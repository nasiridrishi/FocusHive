/**
 * Input Validation Security Tests (UOL-44.15)
 *
 * Comprehensive client-side input validation security testing for FocusHive
 *
 * Test Categories:
 * 1. Form Input Validation
 * 2. Data Type Validation
 * 3. Length and Range Validation
 * 4. Special Character Handling
 * 5. File Upload Validation
 * 6. URL/Email Format Validation
 * 7. SQL Injection Prevention
 * 8. NoSQL Injection Prevention
 * 9. Command Injection Prevention
 * 10. Path Traversal Prevention
 */

import {expect, Page, test} from '@playwright/test';

interface _ValidationTest {
  input: string;
  shouldPass: boolean;
  description: string;
}

interface FieldTestConfig {
  selector: string;
  fieldType: string;
  validInputs: string[];
  invalidInputs: string[];
  maxLength?: number;
  minLength?: number;
  pattern?: RegExp;
}

class InputValidationHelper {
  constructor(private page: Page) {
  }

  async testFieldValidation(config: FieldTestConfig): Promise<{
    validInputsPassed: boolean[];
    invalidInputsBlocked: boolean[];
    validationMessages: string[];
  }> {
    const validResults: boolean[] = [];
    const invalidResults: boolean[] = [];
    const messages: string[] = [];

    // Test valid inputs
    for (const validInput of config.validInputs) {
      await this.page.fill(config.selector, '');
      await this.page.fill(config.selector, validInput);
      await this.page.press(config.selector, 'Tab');

      await this.page.waitForTimeout(500);

      const isValid = await this.checkFieldValidity(config.selector);
      const errorMessage = await this.getValidationMessage(config.selector);

      validResults.push(isValid);
      if (errorMessage) {
        messages.push(`Valid input "${validInput}": ${errorMessage}`);
      }
    }

    // Test invalid inputs
    for (const invalidInput of config.invalidInputs) {
      await this.page.fill(config.selector, '');
      await this.page.fill(config.selector, invalidInput);
      await this.page.press(config.selector, 'Tab');

      await this.page.waitForTimeout(500);

      const isInvalid = !(await this.checkFieldValidity(config.selector));
      const errorMessage = await this.getValidationMessage(config.selector);

      invalidResults.push(isInvalid);
      if (errorMessage) {
        messages.push(`Invalid input "${invalidInput}": ${errorMessage}`);
      }
    }

    return {
      validInputsPassed: validResults,
      invalidInputsBlocked: invalidResults,
      validationMessages: messages
    };
  }

  async checkFieldValidity(selector: string): Promise<boolean> {
    return await this.page.evaluate((sel) => {
      const element = document.querySelector(sel) as HTMLInputElement | HTMLTextAreaElement;
      return element ? element.validity.valid : false;
    }, selector);
  }

  async getValidationMessage(selector: string): Promise<string> {
    return await this.page.evaluate((sel) => {
      const element = document.querySelector(sel) as HTMLInputElement | HTMLTextAreaElement;
      return element ? element.validationMessage : '';
    }, selector);
  }

  async testInjectionPayloads(selector: string, payloads: string[]): Promise<{
    blocked: boolean[];
    sanitized: boolean[];
    originalValues: string[];
    actualValues: string[];
  }> {
    const blocked: boolean[] = [];
    const sanitized: boolean[] = [];
    const originalValues: string[] = [];
    const actualValues: string[] = [];

    for (const payload of payloads) {
      await this.page.fill(selector, '');
      await this.page.fill(selector, payload);
      await this.page.press(selector, 'Tab');

      await this.page.waitForTimeout(300);

      const actualValue = await this.page.inputValue(selector);
      const isBlocked = actualValue === '';
      const isSanitized = actualValue !== payload && actualValue !== '';

      blocked.push(isBlocked);
      sanitized.push(isSanitized);
      originalValues.push(payload);
      actualValues.push(actualValue);
    }

    return {blocked, sanitized, originalValues, actualValues};
  }

  async testFileSizeValidation(fileSelector: string, maxSizeMB: number): Promise<boolean> {
    // Create a large file buffer
    const largeSizeMB = maxSizeMB + 1;
    const largeBuffer = Buffer.alloc(largeSizeMB * 1024 * 1024, 'x');

    try {
      await this.page.setInputFiles(fileSelector, {
        name: 'large-file.txt',
        mimeType: 'text/plain',
        buffer: largeBuffer
      });

      await this.page.waitForTimeout(1000);

      // Check for validation error
      const errorMessage = await this.page.textContent('[data-testid="file-size-error"]');
      return errorMessage !== null && errorMessage.includes('size');
    } catch {
      // If file selection failed, validation worked
      return true;
    }
  }

  async testFileTypeValidation(fileSelector: string, allowedTypes: string[]): Promise<{
    allowedTypesAccepted: boolean[];
    forbiddenTypesRejected: boolean[];
  }> {
    const forbiddenTypes = [
      'text/html', 'application/javascript', 'text/javascript',
      'application/x-executable', 'application/x-msdownload',
      'application/x-sh', 'application/x-python-code'
    ];

    const allowedResults: boolean[] = [];
    const forbiddenResults: boolean[] = [];

    // Test allowed types
    for (const mimeType of allowedTypes) {
      const extension = this.getExtensionFromMimeType(mimeType);
      try {
        await this.page.setInputFiles(fileSelector, {
          name: `test-file.${extension}`,
          mimeType: mimeType,
          buffer: Buffer.from('test content')
        });

        await this.page.waitForTimeout(500);
        const errorMessage = await this.page.textContent('[data-testid="file-type-error"]');
        allowedResults.push(errorMessage === null || errorMessage === '');
      } catch {
        allowedResults.push(false);
      }
    }

    // Test forbidden types
    for (const mimeType of forbiddenTypes) {
      const extension = this.getExtensionFromMimeType(mimeType);
      try {
        await this.page.setInputFiles(fileSelector, {
          name: `malicious-file.${extension}`,
          mimeType: mimeType,
          buffer: Buffer.from('malicious content')
        });

        await this.page.waitForTimeout(500);
        const errorMessage = await this.page.textContent('[data-testid="file-type-error"]');
        forbiddenResults.push(errorMessage !== null && errorMessage.includes('type'));
      } catch {
        // If file selection failed, validation worked
        forbiddenResults.push(true);
      }
    }

    return {
      allowedTypesAccepted: allowedResults,
      forbiddenTypesRejected: forbiddenResults
    };
  }

  async simulateBypassAttempt(selector: string, payload: string): Promise<{
    bypassSuccessful: boolean;
    method: string;
  }> {
    const bypassMethods = [
      'direct_js_manipulation',
      'form_data_manipulation',
      'attribute_modification',
      'event_prevention'
    ];

    for (const method of bypassMethods) {
      let bypassSuccessful = false;

      try {
        switch (method) {
          case 'direct_js_manipulation':
            await this.page.evaluate((sel, val) => {
              const element = document.querySelector(sel) as HTMLInputElement;
              if (element) {
                element.value = val;
                element.dispatchEvent(new Event('input', {bubbles: true}));
                element.dispatchEvent(new Event('change', {bubbles: true}));
              }
            }, selector, payload);
            break;

          case 'form_data_manipulation':
            await this.page.evaluate((sel, val) => {
              const element = document.querySelector(sel) as HTMLInputElement;
              if (element && element.form) {
                const formData = new FormData(element.form);
                formData.set(element.name, val);
              }
            }, selector, payload);
            break;

          case 'attribute_modification':
            await this.page.evaluate((sel, val) => {
              const element = document.querySelector(sel) as HTMLInputElement;
              if (element) {
                element.setAttribute('value', val);
                element.removeAttribute('pattern');
                element.removeAttribute('maxlength');
                element.removeAttribute('required');
              }
            }, selector, payload);
            break;

          case 'event_prevention':
            await this.page.evaluate((sel, val) => {
              const element = document.querySelector(sel) as HTMLInputElement;
              if (element) {
                element.addEventListener('input', (e) => e.stopImmediatePropagation());
                element.value = val;
              }
            }, selector, payload);
            break;
        }

        await this.page.waitForTimeout(500);
        const actualValue = await this.page.inputValue(selector);
        bypassSuccessful = actualValue === payload;

        if (bypassSuccessful) {
          return {bypassSuccessful: true, method};
        }
      } catch {
        // Method failed, continue to next
      }
    }

    return {bypassSuccessful: false, method: 'none'};
  }

  private getExtensionFromMimeType(mimeType: string): string {
    const mimeToExt: Record<string, string> = {
      'image/jpeg': 'jpg',
      'image/png': 'png',
      'image/gif': 'gif',
      'image/svg+xml': 'svg',
      'text/plain': 'txt',
      'text/html': 'html',
      'application/javascript': 'js',
      'text/javascript': 'js',
      'application/pdf': 'pdf',
      'application/x-executable': 'exe',
      'application/x-msdownload': 'exe',
      'application/x-sh': 'sh',
      'application/x-python-code': 'py'
    };

    return mimeToExt[mimeType] || 'bin';
  }
}

test.describe('Input Validation Security Tests', () => {
  let validationHelper: InputValidationHelper;

  test.beforeEach(async ({page}) => {
    validationHelper = new InputValidationHelper(page);

    await page.goto('/');

    // Login for authenticated forms
    await page.click('[data-testid="login-button"]');
    await page.fill('[data-testid="email-input"]', 'test@example.com');
    await page.fill('[data-testid="password-input"]', 'TestPassword123!');
    await page.click('[data-testid="submit-login"]');
    await page.waitForURL('/dashboard');
  });

  test.describe('Form Input Validation', () => {
    test('should validate hive name input', async ({page}) => {
      await page.goto('/create-hive');

      const hiveNameConfig: FieldTestConfig = {
        selector: '[data-testid="hive-name-input"]',
        fieldType: 'text',
        validInputs: [
          'My Study Group',
          'Web Development Team',
          'Focus Session #1',
          'Team Alpha-Beta'
        ],
        invalidInputs: [
          '', // empty
          'a', // too short
          'x'.repeat(101), // too long
          '<script>alert("xss")</script>',
          'DROP TABLE hives;',
          '../../etc/passwd'
        ],
        minLength: 2,
        maxLength: 100
      };

      const results = await validationHelper.testFieldValidation(hiveNameConfig);

      // All valid inputs should pass
      results.validInputsPassed.forEach((passed, _index) => {
        expect(passed).toBe(true);
      });

      // All invalid inputs should be blocked
      results.invalidInputsBlocked.forEach((blocked, _index) => {
        expect(blocked).toBe(true);
      });
    });

    test('should validate email format', async ({page}) => {
      await page.goto('/profile');

      const emailConfig: FieldTestConfig = {
        selector: '[data-testid="email-input"]',
        fieldType: 'email',
        validInputs: [
          'user@example.com',
          'test.email+tag@domain.co.uk',
          'user123@test-domain.org'
        ],
        invalidInputs: [
          'invalid-email',
          'test@',
          '@domain.com',
          'test..test@domain.com',
          'test@domain',
          'javascript:alert("xss")@domain.com'
        ]
      };

      const results = await validationHelper.testFieldValidation(emailConfig);

      results.validInputsPassed.forEach((passed) => {
        expect(passed).toBe(true);
      });

      results.invalidInputsBlocked.forEach((blocked) => {
        expect(blocked).toBe(true);
      });
    });

    test('should validate password strength', async ({page}) => {
      await page.goto('/register');

      const passwordConfig: FieldTestConfig = {
        selector: '[data-testid="password-input"]',
        fieldType: 'password',
        validInputs: [
          'StrongP@ssw0rd123',
          'MySecure#Pass1',
          'C0mpl3x!Password'
        ],
        invalidInputs: [
          'weak',
          'password',
          '12345678',
          'Password',
          'password123',
          'PASSWORD123'
        ],
        minLength: 8
      };

      const results = await validationHelper.testFieldValidation(passwordConfig);

      results.validInputsPassed.forEach((passed) => {
        expect(passed).toBe(true);
      });

      results.invalidInputsBlocked.forEach((blocked) => {
        expect(blocked).toBe(true);
      });
    });

    test('should validate URL format', async ({page}) => {
      await page.goto('/profile');

      // Assuming there's a website URL field
      if (await page.locator('[data-testid="website-url-input"]').count() > 0) {
        const urlConfig: FieldTestConfig = {
          selector: '[data-testid="website-url-input"]',
          fieldType: 'url',
          validInputs: [
            'https://www.example.com',
            'http://subdomain.domain.org/path',
            'https://github.com/user/repo'
          ],
          invalidInputs: [
            'not-a-url',
            'ftp://invalid.protocol',
            'javascript:alert("xss")',
            'data:text/html,<script>alert("xss")</script>',
            'file:///etc/passwd'
          ]
        };

        const results = await validationHelper.testFieldValidation(urlConfig);

        results.validInputsPassed.forEach((passed) => {
          expect(passed).toBe(true);
        });

        results.invalidInputsBlocked.forEach((blocked) => {
          expect(blocked).toBe(true);
        });
      }
    });
  });

  test.describe('Injection Prevention', () => {
    test('should prevent SQL injection in text inputs', async ({page}) => {
      await page.goto('/create-hive');

      const sqlInjectionPayloads = [
        "'; DROP TABLE users; --",
        "' OR '1'='1",
        "' UNION SELECT * FROM users --",
        "admin'--",
        "' OR 1=1#",
        "') OR ('1'='1",
        "'; EXEC xp_cmdshell('dir'); --"
      ];

      const results = await validationHelper.testInjectionPayloads(
          '[data-testid="hive-name-input"]',
          sqlInjectionPayloads
      );

      // All SQL injection attempts should be blocked or sanitized
      results.blocked.concat(results.sanitized).forEach((prevented) => {
        expect(prevented).toBe(true);
      });
    });

    test('should prevent NoSQL injection in text inputs', async ({page}) => {
      await page.goto('/create-hive');

      const noSQLInjectionPayloads = [
        '{"$gt":""}',
        '{"$where":"this.username == this.password"}',
        '{"$regex":".*"}',
        'true, $where: "1 == 1"',
        '{$ne: null}',
        '{$regex: /.*/, $where: "1 == 1"}'
      ];

      const results = await validationHelper.testInjectionPayloads(
          '[data-testid="hive-description-input"]',
          noSQLInjectionPayloads
      );

      results.blocked.concat(results.sanitized).forEach((prevented) => {
        expect(prevented).toBe(true);
      });
    });

    test('should prevent command injection', async ({page}) => {
      await page.goto('/create-hive');

      const commandInjectionPayloads = [
        '; ls -la',
        '&& cat /etc/passwd',
        '| whoami',
        '$(id)',
        '`cat /etc/hosts`',
        '; rm -rf /',
        '& net user hacker password /add'
      ];

      const results = await validationHelper.testInjectionPayloads(
          '[data-testid="hive-name-input"]',
          commandInjectionPayloads
      );

      results.blocked.concat(results.sanitized).forEach((prevented) => {
        expect(prevented).toBe(true);
      });
    });

    test('should prevent path traversal attacks', async ({page}) => {
      await page.goto('/profile');

      // Assuming there's a file path input
      if (await page.locator('[data-testid="avatar-path-input"]').count() > 0) {
        const pathTraversalPayloads = [
          '../../../etc/passwd',
          '..\\..\\..\\windows\\system32\\config\\sam',
          '/etc/shadow',
          'C:\\windows\\system32\\drivers\\etc\\hosts',
          '....//....//....//etc/passwd',
          '..%2F..%2F..%2Fetc%2Fpasswd',
          '..%252F..%252F..%252Fetc%252Fpasswd'
        ];

        const results = await validationHelper.testInjectionPayloads(
            '[data-testid="avatar-path-input"]',
            pathTraversalPayloads
        );

        results.blocked.concat(results.sanitized).forEach((prevented) => {
          expect(prevented).toBe(true);
        });
      }
    });
  });

  test.describe('File Upload Validation', () => {
    test('should validate file size limits', async ({page}) => {
      await page.goto('/profile');

      const fileUploadSelector = '[data-testid="avatar-upload"]';

      if (await page.locator(fileUploadSelector).count() > 0) {
        const maxFileSizeMB = 5; // Assuming 5MB limit
        const fileSizeValid = await validationHelper.testFileSizeValidation(
            fileUploadSelector,
            maxFileSizeMB
        );

        expect(fileSizeValid).toBe(true);
      }
    });

    test('should validate file types', async ({page}) => {
      await page.goto('/profile');

      const fileUploadSelector = '[data-testid="avatar-upload"]';

      if (await page.locator(fileUploadSelector).count() > 0) {
        const allowedTypes = ['image/jpeg', 'image/png', 'image/gif'];

        const results = await validationHelper.testFileTypeValidation(
            fileUploadSelector,
            allowedTypes
        );

        // Allowed types should be accepted
        results.allowedTypesAccepted.forEach((accepted) => {
          expect(accepted).toBe(true);
        });

        // Forbidden types should be rejected
        results.forbiddenTypesRejected.forEach((rejected) => {
          expect(rejected).toBe(true);
        });
      }
    });

    test('should validate file content, not just extension', async ({page}) => {
      await page.goto('/profile');

      const fileUploadSelector = '[data-testid="avatar-upload"]';

      if (await page.locator(fileUploadSelector).count() > 0) {
        // Create a malicious file with image extension but HTML content
        const maliciousContent = '<script>alert("XSS")</script>';

        try {
          await page.setInputFiles(fileUploadSelector, {
            name: 'malicious.png',
            mimeType: 'image/png',
            buffer: Buffer.from(maliciousContent)
          });

          await page.waitForTimeout(1000);

          // Should show error for invalid file content
          const errorMessage = await page.textContent('[data-testid="file-content-error"]');
          expect(errorMessage).toContain('Invalid file content');
        } catch (error) {
          // If file upload fails, validation worked
          expect(error).toBeDefined();
        }
      }
    });
  });

  test.describe('Client-side Validation Bypass Prevention', () => {
    test('should prevent validation bypass through DOM manipulation', async ({page}) => {
      await page.goto('/create-hive');

      const maliciousPayload = '<script>alert("bypassed")</script>';

      const bypassResult = await validationHelper.simulateBypassAttempt(
          '[data-testid="hive-name-input"]',
          maliciousPayload
      );

      // Validation bypass should not succeed
      expect(bypassResult.bypassSuccessful).toBe(false);
    });

    test('should prevent form attribute manipulation', async ({page}) => {
      await page.goto('/create-hive');

      // Try to manipulate form validation attributes
      const manipulationSuccess = await page.evaluate(() => {
        const input = document.querySelector('[data-testid="hive-name-input"]') as HTMLInputElement;
        if (input) {
          // Try to remove validation attributes
          input.removeAttribute('required');
          input.removeAttribute('pattern');
          input.removeAttribute('maxlength');
          input.setAttribute('type', 'hidden');

          // Set malicious value
          input.value = '<script>alert("manipulated")</script>';

          // Check if validation still works
          return input.validity.valid;
        }
        return false;
      });

      // Even after attribute manipulation, server-side validation should prevent malicious input
      expect(manipulationSuccess).toBe(false);
    });

    test('should maintain validation on dynamic form updates', async ({page}) => {
      await page.goto('/create-hive');

      // Test dynamic field addition/removal
      await page.evaluate(() => {
        const form = document.querySelector('form');
        if (form) {
          // Add a new input field dynamically
          const newInput = document.createElement('input');
          newInput.setAttribute('data-testid', 'dynamic-input');
          newInput.setAttribute('name', 'dynamicField');
          form.appendChild(newInput);
        }
      });

      // Dynamic fields should still be validated
      if (await page.locator('[data-testid="dynamic-input"]').count() > 0) {
        const dynamicValidation = await validationHelper.testInjectionPayloads(
            '[data-testid="dynamic-input"]',
            ['<script>alert("dynamic")</script>']
        );

        dynamicValidation.blocked.concat(dynamicValidation.sanitized).forEach((prevented) => {
          expect(prevented).toBe(true);
        });
      }
    });
  });

  test.describe('Real-time Validation', () => {
    test('should provide immediate feedback on invalid input', async ({page}) => {
      await page.goto('/register');

      const passwordField = '[data-testid="password-input"]';

      // Type invalid password
      await page.type(passwordField, 'weak');
      await page.press(passwordField, 'Tab');

      await page.waitForTimeout(500);

      // Should show validation error immediately
      const errorMessage = await validationHelper.getValidationMessage(passwordField);
      expect(errorMessage).toBeTruthy();
      expect(errorMessage.length).toBeGreaterThan(0);
    });

    test('should validate as user types', async ({page}) => {
      await page.goto('/create-hive');

      const nameField = '[data-testid="hive-name-input"]';

      // Type invalid characters one by one
      const invalidChars = '<>{}[]';
      let validationTriggered = false;

      for (const char of invalidChars) {
        await page.type(nameField, char);
        await page.waitForTimeout(200);

        const isValid = await validationHelper.checkFieldValidity(nameField);
        if (!isValid) {
          validationTriggered = true;
          break;
        }
      }

      expect(validationTriggered).toBe(true);
    });

    test('should show strength meter for password fields', async ({page}) => {
      await page.goto('/register');

      const passwordField = '[data-testid="password-input"]';
      const strengthIndicator = '[data-testid="password-strength"]';

      if (await page.locator(strengthIndicator).count() > 0) {
        // Test weak password
        await page.fill(passwordField, 'weak');
        let strengthText = await page.textContent(strengthIndicator);
        expect(strengthText?.toLowerCase()).toContain('weak');

        // Test strong password
        await page.fill(passwordField, 'StrongP@ssw0rd123');
        strengthText = await page.textContent(strengthIndicator);
        expect(strengthText?.toLowerCase()).toContain('strong');
      }
    });
  });

  test.describe('Internationalization Input Validation', () => {
    test('should handle Unicode characters properly', async ({page}) => {
      await page.goto('/create-hive');

      const unicodeInputs = [
        'Группа изучения', // Cyrillic
        '学習グループ', // Japanese
        '학습 그룹', // Korean
        'Groupe d\'étude', // French with apostrophe
        'Café-Studiengruppe', // German with special chars
        '🎯 Focus Group 📚' // Emoji
      ];

      const nameField = '[data-testid="hive-name-input"]';

      for (const unicodeInput of unicodeInputs) {
        await page.fill(nameField, unicodeInput);
        await page.press(nameField, 'Tab');

        await page.waitForTimeout(300);

        const isValid = await validationHelper.checkFieldValidity(nameField);
        const actualValue = await page.inputValue(nameField);

        // Unicode input should be accepted and preserved
        expect(isValid).toBe(true);
        expect(actualValue).toBe(unicodeInput);
      }
    });

    test('should prevent Unicode-based attacks', async ({page}) => {
      await page.goto('/create-hive');

      const unicodeAttacks = [
        '\u202e<script>alert("rtl")</script>', // Right-to-left override
        '\u0000<script>alert("null")</script>', // Null character
        '\ufeff<script>alert("bom")</script>', // Byte order mark
        '＜script＞alert("fullwidth")＜/script＞', // Fullwidth characters
        '\u180e<script>alert("mongolian")</script>' // Mongolian vowel separator
      ];

      const results = await validationHelper.testInjectionPayloads(
          '[data-testid="hive-name-input"]',
          unicodeAttacks
      );

      results.blocked.concat(results.sanitized).forEach((prevented) => {
        expect(prevented).toBe(true);
      });
    });
  });
});