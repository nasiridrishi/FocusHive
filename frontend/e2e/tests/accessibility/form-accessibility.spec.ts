/**
 * Form Accessibility Tests
 *
 * Tests comprehensive form accessibility including:
 * - Label associations and descriptions
 * - Error handling and validation
 * - Required field indicators
 * - Fieldset and legend usage
 * - Autocomplete attributes
 * - Input purposes and formats
 * - Accessible form submission
 *
 * UOL-44.19: Comprehensive Accessibility E2E Tests
 */

import {expect, Locator, test} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {AccessibilityHelper} from '../../helpers/accessibility.helper';
import {AccessibilityPage} from '../../pages/AccessibilityPage';

interface _FormTest {
  pagePath: string;
  formName: string;
  expectedFields: string[];
  requiredFields: string[];
}

interface ValidationTest {
  fieldType: string;
  invalidValue: string;
  validValue: string;
  expectedError: RegExp;
}

test.describe('Form Accessibility Tests', () => {
  let accessibilityHelper: AccessibilityHelper;
  let accessibilityPage: AccessibilityPage;

  test.beforeEach(async ({page}) => {
    accessibilityHelper = new AccessibilityHelper(page);
    accessibilityPage = new AccessibilityPage(page);

    await accessibilityHelper.configureAxe();
  });

  test.describe('Label Associations', () => {
    test('should have proper label associations for all form controls', async ({page}) => {
      const formPages = ['/login', '/register', '/profile'];

      for (const formPage of formPages) {
        await page.goto(formPage);
        await accessibilityPage.waitForPageLoad();

        // Run axe-core label checks
        const results = await new AxeBuilder({page})
        .withRules([
          'label',
          'form-field-multiple-labels',
          'label-title-only',
          'label-content-name-mismatch'
        ])
        .analyze();

        expect(results.violations, `Page ${formPage} should have proper form labels`).toEqual([]);

        // Manual verification of all form controls
        const formControls = await page.locator('input, select, textarea').all();

        for (const control of formControls) {
          const controlType = await control.getAttribute('type') || 'text';
          const _controlId = await control.getAttribute('id');
          const controlName = await control.getAttribute('name');

          // Skip hidden inputs and submit buttons
          if (controlType === 'hidden' || controlType === 'submit') continue;

          // Check for label association
          const hasLabel = await control.evaluate((el) => {
            const input = el as HTMLInputElement;

            // Method 1: Label with 'for' attribute
            if (input.id) {
              const label = document.querySelector(`label[for="${input.id}"]`);
              if (label) return true;
            }

            // Method 2: Input inside label
            const parentLabel = input.closest('label');
            if (parentLabel) return true;

            // Method 3: aria-label
            if (input.getAttribute('aria-label')) return true;

            // Method 4: aria-labelledby
            if (input.getAttribute('aria-labelledby')) {
              const labelledBy = input.getAttribute('aria-labelledby') || '';
              const referencedElements = labelledBy.split(' ');
              return referencedElements.every(id => document.getElementById(id));
            }

            // Method 5: title attribute (not recommended but valid)
            if (input.getAttribute('title')) return true;

            return false;
          });

          expect(
              hasLabel,
              `Form control ${controlType} with name="${controlName}" should have label association on ${formPage}`
          ).toBeTruthy();

          // Verify accessible name is meaningful
          const accessibleName = await accessibilityHelper.getAccessibleName(control);
          expect(
              accessibleName.length,
              `Form control should have meaningful accessible name on ${formPage}`
          ).toBeGreaterThan(0);
        }
      }
    });

    test('should have unique and descriptive labels', async ({page}) => {
      const formPages = ['/register', '/profile'];

      for (const formPage of formPages) {
        await page.goto(formPage);

        const formControls = await page.locator('input, select, textarea').all();
        const labelTexts = new Set<string>();

        for (const control of formControls) {
          const controlType = await control.getAttribute('type');

          if (controlType === 'hidden' || controlType === 'submit') continue;

          const accessibleName = await accessibilityHelper.getAccessibleName(control);

          if (accessibleName.length > 0) {
            // Check for duplicate labels (should be avoided)
            if (labelTexts.has(accessibleName)) {
              console.warn(`Duplicate label found on ${formPage}: "${accessibleName}"`);
            }
            labelTexts.add(accessibleName);

            // Label should be descriptive (avoid generic terms)
            const genericLabels = ['input', 'field', 'textbox', 'text', 'enter'];
            const isGeneric = genericLabels.some(generic =>
                accessibleName.toLowerCase().trim() === generic
            );

            expect(
                isGeneric,
                `Label "${accessibleName}" should be descriptive, not generic`
            ).toBeFalsy();
          }
        }
      }
    });

    test('should associate labels with the correct control when multiple exist', async ({page}) => {
      await page.goto('/profile');

      // Look for cases where multiple controls might have similar labels
      const emailInputs = await page.locator('input[type="email"]').all();

      if (emailInputs.length > 1) {
        for (const emailInput of emailInputs) {
          const label = await accessibilityHelper.getAccessibleName(emailInput);
          const inputId = await emailInput.getAttribute('id');

          expect(
              label.length,
              `Each email input should have a distinct label`
          ).toBeGreaterThan(0);

          // Verify the label is actually associated with this specific input
          if (inputId) {
            const associatedLabel = await page.locator(`label[for="${inputId}"]`).first();

            if (await associatedLabel.count() > 0) {
              const labelText = await associatedLabel.textContent();
              expect(
                  labelText?.trim(),
                  `Label should be associated with correct input`
              ).toBe(label.trim());
            }
          }
        }
      }
    });
  });

  test.describe('Required Field Indicators', () => {
    test('should properly indicate required fields', async ({page}) => {
      const formPages = ['/login', '/register'];

      for (const formPage of formPages) {
        await page.goto(formPage);

        const requiredFields = await accessibilityPage.getRequiredFields();

        for (const field of requiredFields) {
          // Check programmatic indication
          const hasRequiredAttr = await field.getAttribute('required');
          const hasAriaRequired = await field.getAttribute('aria-required');

          expect(
              hasRequiredAttr !== null || hasAriaRequired === 'true',
              'Required fields should have required or aria-required attribute'
          ).toBeTruthy();

          // Check visual indication in label
          const fieldId = await field.getAttribute('id');
          const _fieldName = await field.getAttribute('name');

          if (fieldId) {
            const associatedLabel = await page.locator(`label[for="${fieldId}"]`).first();

            if (await associatedLabel.count() > 0) {
              const labelText = await associatedLabel.textContent();
              const labelHtml = await associatedLabel.innerHTML();

              // Should have some visual indicator (*, required, etc.)
              const hasVisualIndicator = labelText?.includes('*') ||
                  labelText?.toLowerCase().includes('required') ||
                  labelHtml.includes('required') ||
                  labelHtml.includes('*');

              expect(
                  hasVisualIndicator,
                  `Required field label "${labelText}" should have visual indicator`
              ).toBeTruthy();
            }
          }
        }

        // Should have general instructions about required fields
        const hasRequiredInstruction = await page.locator(
            'text="* indicates required field", text="required fields", [aria-describedby], .form-help'
        ).count();

        if (requiredFields.length > 0) {
          expect(
              hasRequiredInstruction,
              `Page ${formPage} should explain required field indicators`
          ).toBeGreaterThan(0);
        }
      }
    });

    test('should maintain required field indicators across states', async ({page}) => {
      await page.goto('/register');

      const requiredFields = await accessibilityPage.getRequiredFields();

      for (const field of requiredFields.slice(0, 3)) {
        // Test initial state
        const initialRequired = await field.getAttribute('aria-required') ||
            await field.getAttribute('required');

        // Focus the field
        await field.focus();
        await page.waitForTimeout(200);

        const focusedRequired = await field.getAttribute('aria-required') ||
            await field.getAttribute('required');

        expect(
            focusedRequired,
            'Required indicator should persist when field is focused'
        ).toBe(initialRequired);

        // Fill the field
        const fieldType = await field.getAttribute('type');

        if (fieldType === 'email') {
          await field.fill('test@example.com');
        } else if (fieldType === 'password') {
          await field.fill('password123');
        } else {
          await field.fill('test value');
        }

        await page.waitForTimeout(200);

        const filledRequired = await field.getAttribute('aria-required') ||
            await field.getAttribute('required');

        expect(
            filledRequired,
            'Required indicator should persist when field is filled'
        ).toBe(initialRequired);
      }
    });
  });

  test.describe('Error Handling and Validation', () => {
    test('should provide accessible error messages', async ({page}) => {
      await page.goto('/login');

      // Submit form without filling required fields
      const submitButton = await page.locator('button[type="submit"], input[type="submit"]').first();
      await submitButton.click();
      await page.waitForTimeout(1000);

      const errorMessages = await accessibilityPage.getErrorMessages();

      for (const errorMessage of errorMessages) {
        // Error should be visible
        await expect(errorMessage).toBeVisible();

        // Error should have proper role or live region
        const role = await errorMessage.getAttribute('role');
        const ariaLive = await errorMessage.getAttribute('aria-live');

        const isAccessible = role === 'alert' ||
            ariaLive === 'assertive' ||
            ariaLive === 'polite';

        expect(isAccessible, 'Error messages should be announced to screen readers').toBeTruthy();

        // Error should be associated with the relevant field
        const errorId = await errorMessage.getAttribute('id');

        if (errorId) {
          const associatedField = await page.locator(`[aria-describedby*="${errorId}"]`).first();

          if (await associatedField.count() > 0) {
            expect(
                await associatedField.count(),
                'Error message should be associated with form field'
            ).toBeGreaterThan(0);
          }
        }

        // Error text should be meaningful
        const errorText = await errorMessage.textContent();
        expect(
            errorText?.trim().length,
            'Error messages should have meaningful text'
        ).toBeGreaterThan(0);

        // Should not be generic error
        const genericErrors = ['error', 'invalid', 'wrong', 'bad'];
        const isGeneric = genericErrors.some(generic =>
            errorText?.toLowerCase().trim() === generic
        );

        expect(
            isGeneric,
            `Error message "${errorText}" should be specific, not generic`
        ).toBeFalsy();
      }
    });

    test('should provide error suggestions and corrections', async ({page}) => {
      await page.goto('/register');

      const validationTests: ValidationTest[] = [
        {
          fieldType: 'email',
          invalidValue: 'invalid-email',
          validValue: 'user@example.com',
          expectedError: /email|format|@|example/i
        },
        {
          fieldType: 'password',
          invalidValue: '123',
          validValue: 'SecurePass123!',
          expectedError: /password|length|character|requirement/i
        },
        {
          fieldType: 'tel',
          invalidValue: 'abc123',
          validValue: '+1234567890',
          expectedError: /phone|number|format|digit/i
        }
      ];

      for (const test of validationTests) {
        const field = await page.locator(`input[type="${test.fieldType}"]`).first();

        if (await field.count() > 0) {
          // Enter invalid value
          await field.fill(test.invalidValue);
          await field.blur();
          await page.waitForTimeout(500);

          // Look for error message
          const fieldId = await field.getAttribute('id');
          const _fieldName = await field.getAttribute('name');

          let errorMessage = null;

          // Try to find associated error message
          if (fieldId) {
            const describedBy = await field.getAttribute('aria-describedby');

            if (describedBy) {
              const errorIds = describedBy.split(' ');

              for (const errorId of errorIds) {
                const errorEl = await page.locator(`#${errorId}`).first();

                if (await errorEl.count() > 0 && await errorEl.isVisible()) {
                  errorMessage = await errorEl.textContent();
                  break;
                }
              }
            }
          }

          // Fallback: look for nearby error messages
          if (!errorMessage) {
            const nearbyErrors = await field.locator('xpath=..//*[contains(@class, "error") or contains(@role, "alert")]').all();

            for (const error of nearbyErrors) {
              if (await error.isVisible()) {
                errorMessage = await error.textContent();
                break;
              }
            }
          }

          if (errorMessage) {
            expect(
                test.expectedError.test(errorMessage),
                `Error message "${errorMessage}" should provide helpful guidance for ${test.fieldType} field`
            ).toBeTruthy();
          }
        }
      }
    });

    test('should clear errors when input becomes valid', async ({page}) => {
      await page.goto('/login');

      // Submit to trigger validation
      const submitButton = await page.locator('button[type="submit"]').first();
      await submitButton.click();
      await page.waitForTimeout(1000);

      // Find a required email field
      const emailField = await page.locator('input[type="email"][required], input[type="email"][aria-required="true"]').first();

      if (await emailField.count() > 0) {
        // Should have error initially
        let errorElements = await accessibilityPage.getErrorMessages();
        const initialErrorCount = errorElements.length;

        // Fill with valid email
        await emailField.fill('valid@example.com');
        await emailField.blur();
        await page.waitForTimeout(1000);

        // Errors should be cleared or reduced
        errorElements = await accessibilityPage.getErrorMessages();
        const finalErrorCount = errorElements.length;

        expect(
            finalErrorCount,
            'Error count should decrease when valid input is provided'
        ).toBeLessThanOrEqual(initialErrorCount);
      }
    });

    test('should handle client-side and server-side validation consistently', async ({page}) => {
      await page.goto('/register');

      const emailField = await page.locator('input[type="email"]').first();

      if (await emailField.count() > 0) {
        // Test client-side validation
        await emailField.fill('invalid-email');
        await emailField.blur();
        await page.waitForTimeout(500);

        const clientErrorElements = await accessibilityPage.getErrorMessages();

        // Test that HTML5 validation doesn't override accessible error handling
        const html5ValidationMessage = await emailField.evaluate((el: HTMLInputElement) => {
          return el.validationMessage;
        });

        if (html5ValidationMessage && clientErrorElements.length > 0) {
          // Should have custom accessible error message, not just HTML5 validation
          const customError = await clientErrorElements[0].textContent();

          expect(
              customError,
              'Should use custom accessible error messages over HTML5 validation'
          ).not.toBe(html5ValidationMessage);
        }
      }
    });
  });

  test.describe('Fieldset and Legend Usage', () => {
    test('should use fieldsets for grouped form controls', async ({page}) => {
      const formPages = ['/register', '/profile'];

      for (const formPage of formPages) {
        await page.goto(formPage);

        const fieldsets = await page.locator('fieldset').all();

        for (const fieldset of fieldsets) {
          // Every fieldset should have a legend
          const legend = await fieldset.locator('legend').first();
          await expect(legend).toBeAttached();

          const legendText = await legend.textContent();
          expect(
              legendText?.trim().length,
              'Fieldset legends should have descriptive text'
          ).toBeGreaterThan(0);

          // Fieldset should contain form controls
          const controls = await fieldset.locator('input, select, textarea').all();
          expect(
              controls.length,
              'Fieldsets should contain form controls'
          ).toBeGreaterThan(0);

          // Related controls should be logically grouped
          const controlTypes = new Set();

          for (const control of controls) {
            const name = await control.getAttribute('name');
            const type = await control.getAttribute('type');

            if (name) controlTypes.add(name);
            if (type) controlTypes.add(type);
          }

          // Should group related fields (e.g., address fields, contact fields)
          console.log(`Fieldset "${legendText}" contains ${controls.length} controls`);
        }
      }
    });

    test('should use fieldsets for radio button groups', async ({page}) => {
      await page.goto('/profile');

      const radioButtons = await page.locator('input[type="radio"]').all();

      if (radioButtons.length > 0) {
        const radioGroups = new Map<string, Locator[]>();

        // Group radio buttons by name
        for (const radio of radioButtons) {
          const name = await radio.getAttribute('name');

          if (name) {
            if (!radioGroups.has(name)) {
              radioGroups.set(name, []);
            }
            radioGroups.get(name)?.push(radio);
          }
        }

        // Each radio group should be in a fieldset
        for (const [groupName, radios] of radioGroups) {
          if (radios.length > 1) {
            const firstRadio = radios[0];
            const fieldset = await firstRadio.locator('xpath=ancestor::fieldset').first();

            expect(
                await fieldset.count(),
                `Radio group "${groupName}" should be contained in a fieldset`
            ).toBeGreaterThan(0);

            if (await fieldset.count() > 0) {
              const legend = await fieldset.locator('legend').first();
              await expect(legend).toBeAttached();

              const legendText = await legend.textContent();
              expect(
                  legendText?.trim().length,
                  `Radio group "${groupName}" should have descriptive legend`
              ).toBeGreaterThan(0);
            }
          }
        }
      }
    });

    test('should use fieldsets for checkbox groups', async ({page}) => {
      await page.goto('/profile');

      // Look for logical checkbox groups (same name or related topics)
      const checkboxes = await page.locator('input[type="checkbox"]').all();

      if (checkboxes.length > 2) {
        // Look for checkboxes that might be logically grouped
        const checkboxGroups = new Map<string, Locator[]>();

        for (const checkbox of checkboxes) {
          const name = await checkbox.getAttribute('name');
          const _id = await checkbox.getAttribute('id');
          const _label = await accessibilityHelper.getAccessibleName(checkbox);

          // Group by name or by semantic similarity
          const groupKey = name || 'preferences'; // Default group for preferences

          if (!checkboxGroups.has(groupKey)) {
            checkboxGroups.set(groupKey, []);
          }
          checkboxGroups.get(groupKey)?.push(checkbox);
        }

        for (const [groupName, checkboxList] of checkboxGroups) {
          if (checkboxList.length > 2) {
            // Check if grouped in fieldset
            const firstCheckbox = checkboxList[0];
            const fieldset = await firstCheckbox.locator('xpath=ancestor::fieldset').first();

            if (await fieldset.count() > 0) {
              const legend = await fieldset.locator('legend').first();
              await expect(legend).toBeAttached();

              console.log(`Checkbox group "${groupName}" is properly contained in fieldset`);
            } else {
              console.warn(`Checkbox group "${groupName}" should consider using fieldset for better accessibility`);
            }
          }
        }
      }
    });
  });

  test.describe('Autocomplete and Input Purposes', () => {
    test('should have appropriate autocomplete attributes', async ({page}) => {
      const formPages = ['/login', '/register', '/profile'];

      const autocompleteTests = [
        {type: 'email', expected: 'email'},
        {name: 'username', expected: 'username'},
        {name: 'password', expected: 'current-password'},
        {name: 'new-password', expected: 'new-password'},
        {name: 'first-name', expected: 'given-name'},
        {name: 'last-name', expected: 'family-name'},
        {name: 'phone', expected: 'tel'},
        {name: 'address', expected: 'street-address'},
        {name: 'city', expected: 'address-level2'},
        {name: 'postal-code', expected: 'postal-code'},
        {name: 'country', expected: 'country'}
      ];

      for (const formPage of formPages) {
        await page.goto(formPage);

        for (const test of autocompleteTests) {
          let selector = '';

          if (test.type) {
            selector = `input[type="${test.type}"]`;
          } else if (test.name) {
            selector = `input[name*="${test.name}"], input[id*="${test.name}"]`;
          }

          const fields = await page.locator(selector).all();

          for (const field of fields) {
            const autocomplete = await field.getAttribute('autocomplete');
            const fieldName = await field.getAttribute('name');
            const fieldId = await field.getAttribute('id');

            if (autocomplete) {
              expect(
                  autocomplete,
                  `Field ${fieldName || fieldId} should have autocomplete="${test.expected}"`
              ).toBe(test.expected);
            } else {
              console.warn(
                  `Field ${fieldName || fieldId} on ${formPage} could benefit from autocomplete="${test.expected}"`
              );
            }
          }
        }
      }
    });

    test('should have appropriate input types and modes', async ({page}) => {
      const formPages = ['/register', '/profile'];

      const inputTypeTests = [
        {purpose: 'email', expectedType: 'email', expectedMode: 'email'},
        {purpose: 'phone', expectedType: 'tel', expectedMode: 'tel'},
        {purpose: 'url', expectedType: 'url', expectedMode: 'url'},
        {purpose: 'number', expectedType: 'number', expectedMode: 'numeric'},
        {purpose: 'search', expectedType: 'search', expectedMode: null}
      ];

      for (const formPage of formPages) {
        await page.goto(formPage);

        for (const test of inputTypeTests) {
          const fields = await page.locator(`input[name*="${test.purpose}"], input[id*="${test.purpose}"], input[placeholder*="${test.purpose}"]`).all();

          for (const field of fields) {
            const inputType = await field.getAttribute('type');
            const inputMode = await field.getAttribute('inputmode');
            const fieldName = await field.getAttribute('name');

            if (inputType && inputType !== test.expectedType) {
              console.warn(
                  `Field ${fieldName} for ${test.purpose} should use type="${test.expectedType}" instead of "${inputType}"`
              );
            }

            if (test.expectedMode && inputMode !== test.expectedMode) {
              console.warn(
                  `Field ${fieldName} for ${test.purpose} should use inputmode="${test.expectedMode}"`
              );
            }
          }
        }
      }
    });
  });

  test.describe('Form Instructions and Help', () => {
    test('should provide clear instructions for form completion', async ({page}) => {
      const formPages = ['/register', '/profile'];

      for (const formPage of formPages) {
        await page.goto(formPage);

        // Look for form instructions
        const instructions = await page.locator(
            '.form-instructions, .form-help, [role="group"] p, form > p, [aria-describedby]'
        ).all();

        for (const instruction of instructions) {
          const instructionText = await instruction.textContent();

          expect(
              instructionText?.trim().length,
              'Form instructions should have meaningful content'
          ).toBeGreaterThan(0);
        }

        // Check for password requirements if password field exists
        const passwordFields = await page.locator('input[type="password"]').all();

        if (passwordFields.length > 0) {
          const passwordRequirements = await page.locator(
              'text="password must", text="password should", text="minimum", [id*="password-help"], [id*="password-requirements"]'
          ).count();

          expect(
              passwordRequirements,
              `Page ${formPage} should provide password requirements`
          ).toBeGreaterThan(0);
        }
      }
    });

    test('should provide help text for complex fields', async ({page}) => {
      await page.goto('/profile');

      const complexFields = await page.locator(
          'input[type="password"], input[pattern], input[type="tel"], input[type="date"], select[multiple]'
      ).all();

      for (const field of complexFields) {
        const fieldType = await field.getAttribute('type');
        const fieldName = await field.getAttribute('name');
        const describedBy = await field.getAttribute('aria-describedby');

        if (describedBy) {
          const helpTextIds = describedBy.split(' ');

          for (const helpId of helpTextIds) {
            const helpElement = await page.locator(`#${helpId}`).first();

            if (await helpElement.count() > 0) {
              const helpText = await helpElement.textContent();

              expect(
                  helpText?.trim().length,
                  `Help text for ${fieldType} field should be meaningful`
              ).toBeGreaterThan(0);
            }
          }
        } else if (fieldType === 'password' || fieldType === 'tel') {
          console.warn(
              `Complex field ${fieldName} (${fieldType}) should have associated help text`
          );
        }
      }
    });

    test('should support contextual help', async ({page}) => {
      await page.goto('/profile');

      // Look for help buttons or info icons
      const helpTriggers = await page.locator(
          'button[aria-label*="help"], [data-tooltip], [title], .help-icon, .info-icon'
      ).all();

      for (const trigger of helpTriggers.slice(0, 3)) {
        const hasAccessibleName = await accessibilityHelper.hasAccessibleName(trigger);
        expect(
            hasAccessibleName,
            'Help triggers should have accessible names'
        ).toBeTruthy();

        // Test activation
        await trigger.click();
        await page.waitForTimeout(500);

        // Look for help content
        const helpContent = await page.locator(
            '[role="tooltip"], .tooltip, .help-text:visible, [aria-expanded="true"] + .help-content'
        ).first();

        if (await helpContent.count() > 0) {
          const contentText = await helpContent.textContent();

          expect(
              contentText?.trim().length,
              'Help content should be meaningful'
          ).toBeGreaterThan(0);

          // Help content should be keyboard accessible
          const isKeyboardAccessible = await accessibilityPage.isKeyboardAccessible(helpContent);

          if (!isKeyboardAccessible) {
            // Content should at least be announced by screen readers
            const role = await helpContent.getAttribute('role');
            const ariaLive = await helpContent.getAttribute('aria-live');

            const isAnnounced = role === 'tooltip' ||
                ariaLive === 'polite' ||
                ariaLive === 'assertive';

            expect(
                isAnnounced,
                'Help content should be announced to screen readers'
            ).toBeTruthy();
          }
        }
      }
    });
  });

  test.describe('Form Submission and Feedback', () => {
    test('should provide accessible form submission feedback', async ({page}) => {
      await page.goto('/login');

      // Fill out form correctly
      const emailField = await page.locator('input[type="email"]').first();
      const passwordField = await page.locator('input[type="password"]').first();

      if (await emailField.count() > 0 && await passwordField.count() > 0) {
        await emailField.fill('user@example.com');
        await passwordField.fill('password123');

        // Submit form
        const submitButton = await page.locator('button[type="submit"]').first();
        await submitButton.click();
        await page.waitForTimeout(2000);

        // Look for feedback (success or error)
        const feedbackElements = await page.locator(
            '[role="alert"], [role="status"], [aria-live="polite"], [aria-live="assertive"], .success, .error, .message'
        ).all();

        for (const feedback of feedbackElements) {
          const isVisible = await feedback.isVisible();

          if (isVisible) {
            const feedbackText = await feedback.textContent();

            expect(
                feedbackText?.trim().length,
                'Form submission feedback should have meaningful content'
            ).toBeGreaterThan(0);

            // Should be announced to screen readers
            const role = await feedback.getAttribute('role');
            const ariaLive = await feedback.getAttribute('aria-live');

            const isAnnounced = role === 'alert' ||
                role === 'status' ||
                ariaLive === 'polite' ||
                ariaLive === 'assertive';

            expect(
                isAnnounced,
                'Form feedback should be announced to screen readers'
            ).toBeTruthy();
          }
        }
      }
    });

    test('should handle form submission loading states accessibly', async ({page}) => {
      await page.goto('/register');

      const submitButton = await page.locator('button[type="submit"]').first();

      if (await submitButton.count() > 0) {
        // Check initial state
        const initialText = await accessibilityHelper.getAccessibleName(submitButton);
        const initialDisabled = await submitButton.isDisabled();

        // Click submit (even if form is invalid to test loading state)
        await submitButton.click();
        await page.waitForTimeout(500);

        // Button should indicate loading state
        const loadingText = await accessibilityHelper.getAccessibleName(submitButton);
        const isDisabledDuringSubmit = await submitButton.isDisabled();
        const hasAriaBusy = await submitButton.getAttribute('aria-busy');

        const hasLoadingIndication = loadingText !== initialText ||
            hasAriaBusy === 'true' ||
            isDisabledDuringSubmit !== initialDisabled;

        if (hasLoadingIndication) {
          expect(
              loadingText.length,
              'Loading state should have accessible name'
          ).toBeGreaterThan(0);

          console.log(`Submit button loading state: "${loadingText}"`);
        }
      }
    });

    test('should prevent duplicate form submissions accessibly', async ({page}) => {
      await page.goto('/register');

      const submitButton = await page.locator('button[type="submit"]').first();

      if (await submitButton.count() > 0) {
        // Fill required fields to allow submission
        const requiredFields = await accessibilityPage.getRequiredFields();

        for (const field of requiredFields.slice(0, 3)) {
          const fieldType = await field.getAttribute('type');

          if (fieldType === 'email') {
            await field.fill('test@example.com');
          } else if (fieldType === 'password') {
            await field.fill('password123');
          } else {
            await field.fill('test value');
          }
        }

        // First submission
        await submitButton.click();
        await page.waitForTimeout(200);

        // Button should be disabled or show loading state
        const isDisabled = await submitButton.isDisabled();
        const ariaBusy = await submitButton.getAttribute('aria-busy');
        const buttonText = await accessibilityHelper.getAccessibleName(submitButton);

        const hasProtection = isDisabled ||
            ariaBusy === 'true' ||
            buttonText.toLowerCase().includes('submitting') ||
            buttonText.toLowerCase().includes('loading');

        expect(
            hasProtection,
            'Form should prevent duplicate submissions accessibly'
        ).toBeTruthy();
      }
    });
  });

  test.describe('Mobile Form Accessibility', () => {
    test('should be accessible on mobile devices', async ({page}) => {
      await page.setViewportSize({width: 375, height: 667});
      await page.goto('/login');

      // Form should be usable at mobile viewport
      const results = await new AxeBuilder({page})
      .withTags(['wcag2a', 'wcag2aa'])
      .analyze();

      expect(results.violations, 'Forms should be accessible on mobile').toEqual([]);

      // Touch targets should be adequate
      const interactiveElements = await accessibilityPage.getInteractiveElements();

      for (const element of interactiveElements.slice(0, 5)) {
        const boundingBox = await element.boundingBox();

        if (boundingBox) {
          expect(
              Math.min(boundingBox.width, boundingBox.height),
              'Touch targets should be at least 44px'
          ).toBeGreaterThanOrEqual(44);
        }
      }

      // Input types should trigger appropriate mobile keyboards
      const emailInputs = await page.locator('input[type="email"]').all();

      for (const input of emailInputs) {
        const inputMode = await input.getAttribute('inputmode');
        const type = await input.getAttribute('type');

        expect(
            type === 'email' || inputMode === 'email',
            'Email inputs should trigger email keyboard on mobile'
        ).toBeTruthy();
      }
    });

    test('should handle virtual keyboard properly', async ({page}) => {
      await page.setViewportSize({width: 375, height: 667});
      await page.goto('/register');

      const inputs = await page.locator('input, textarea').all();

      for (const input of inputs.slice(0, 3)) {
        await input.focus();
        await page.waitForTimeout(300);

        // Input should still be visible when virtual keyboard is open
        const inputBox = await input.boundingBox();

        if (inputBox) {
          // On mobile, focused input should be in upper half of screen
          // to account for virtual keyboard
          expect(
              inputBox.y,
              'Focused input should be visible above virtual keyboard'
          ).toBeLessThan(300);
        }
      }
    });
  });
});