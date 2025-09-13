/**
 * Form and Input Compatibility Tests
 * Tests form elements, input types, validation, and interaction across browsers
 */

import { test, expect, Page } from '@playwright/test';
import { getBrowserInfo } from './browser-helpers';

test.describe('HTML5 Input Types', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should support modern input types', async ({ page }) => {
    await page.setContent(`
      <form id="input-types-form">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; padding: 20px;">
          <input type="email" id="email" placeholder="email@example.com">
          <input type="tel" id="tel" placeholder="+1234567890">
          <input type="url" id="url" placeholder="https://example.com">
          <input type="date" id="date">
          <input type="time" id="time">
          <input type="datetime-local" id="datetime-local">
          <input type="number" id="number" min="0" max="100" step="1">
          <input type="range" id="range" min="0" max="100" value="50">
          <input type="color" id="color" value="#ff0000">
          <input type="search" id="search" placeholder="Search...">
        </div>
      </form>
    `);

    // Test input type support by checking if they fall back to text type
    const inputTypeSupport = await page.evaluate(() => {
      const inputs = {
        email: document.getElementById('email') as HTMLInputElement,
        tel: document.getElementById('tel') as HTMLInputElement,
        url: document.getElementById('url') as HTMLInputElement,
        date: document.getElementById('date') as HTMLInputElement,
        time: document.getElementById('time') as HTMLInputElement,
        datetimeLocal: document.getElementById('datetime-local') as HTMLInputElement,
        number: document.getElementById('number') as HTMLInputElement,
        range: document.getElementById('range') as HTMLInputElement,
        color: document.getElementById('color') as HTMLInputElement,
        search: document.getElementById('search') as HTMLInputElement
      };

      const support: { [key: string]: boolean } = {};
      
      Object.entries(inputs).forEach(([key, input]) => {
        if (input) {
          // If the browser doesn't support the input type, it falls back to 'text'
          support[key] = input.type !== 'text' || key === 'search'; // search might be 'text' but still supported
        }
      });

      return support;
    });

    // Modern browsers should support most of these
    expect(inputTypeSupport.email).toBe(true);
    expect(inputTypeSupport.number).toBe(true);
    expect(inputTypeSupport.range).toBe(true);

    console.log('Input Type Support:', inputTypeSupport);

    // Take screenshot of different input types
    await expect(page.locator('#input-types-form')).toHaveScreenshot('html5-input-types.png');
  });

  test('should handle input validation', async ({ page }) => {
    await page.setContent(`
      <form id="validation-form">
        <div style="padding: 20px;">
          <input type="email" id="email-validation" required placeholder="Required email">
          <input type="number" id="number-validation" min="1" max="10" step="1" placeholder="Number 1-10">
          <input type="text" id="text-validation" pattern="[A-Za-z]{3,}" placeholder="Letters only, min 3">
          <input type="text" id="minmax-validation" minlength="5" maxlength="15" placeholder="5-15 characters">
          <button type="submit">Submit</button>
        </div>
      </form>
    `);

    // Test HTML5 validation
    const emailInput = page.locator('#email-validation');
    const numberInput = page.locator('#number-validation');
    const patternInput = page.locator('#text-validation');
    const minmaxInput = page.locator('#minmax-validation');
    const submitButton = page.locator('button[type="submit"]');

    // Test required validation
    await submitButton.click();
    
    const emailValidity = await emailInput.evaluate((input: HTMLInputElement) => ({
      valid: input.validity.valid,
      valueMissing: input.validity.valueMissing,
      validationMessage: input.validationMessage
    }));

    expect(emailValidity.valid).toBe(false);
    expect(emailValidity.valueMissing).toBe(true);

    // Test email format validation
    await emailInput.fill('invalid-email');
    const emailFormatValidity = await emailInput.evaluate((input: HTMLInputElement) => ({
      valid: input.validity.valid,
      typeMismatch: input.validity.typeMismatch
    }));

    expect(emailFormatValidity.valid).toBe(false);
    expect(emailFormatValidity.typeMismatch).toBe(true);

    // Test valid email
    await emailInput.fill('test@example.com');
    const validEmailValidity = await emailInput.evaluate((input: HTMLInputElement) => input.validity.valid);
    expect(validEmailValidity).toBe(true);

    // Test number range validation
    await numberInput.fill('15'); // Outside max range
    const numberRangeValidity = await numberInput.evaluate((input: HTMLInputElement) => ({
      valid: input.validity.valid,
      rangeOverflow: input.validity.rangeOverflow
    }));

    expect(numberRangeValidity.valid).toBe(false);
    expect(numberRangeValidity.rangeOverflow).toBe(true);

    // Test pattern validation
    await patternInput.fill('123');
    const patternValidity = await patternInput.evaluate((input: HTMLInputElement) => ({
      valid: input.validity.valid,
      patternMismatch: input.validity.patternMismatch
    }));

    expect(patternValidity.valid).toBe(false);
    expect(patternValidity.patternMismatch).toBe(true);

    console.log('HTML5 Validation Tests Passed');
  });

  test('should support custom validity messages', async ({ page }) => {
    await page.setContent(`
      <form id="custom-validity-form">
        <input type="email" id="custom-email" required>
        <script>
          const emailInput = document.getElementById('custom-email');
          emailInput.addEventListener('invalid', function() {
            if (this.validity.valueMissing) {
              this.setCustomValidity('Please enter your email address');
            } else if (this.validity.typeMismatch) {
              this.setCustomValidity('Please enter a valid email format');
            } else {
              this.setCustomValidity('');
            }
          });
          
          emailInput.addEventListener('input', function() {
            this.setCustomValidity('');
          });
        </script>
      </form>
    `);

    const emailInput = page.locator('#custom-email');
    
    // Trigger validation
    await emailInput.focus();
    await emailInput.blur();

    const customMessage = await emailInput.evaluate((input: HTMLInputElement) => {
      input.reportValidity();
      return input.validationMessage;
    });

    expect(customMessage).toContain('Please enter your email address');
  });
});

test.describe('Form Controls and Interactions', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should support advanced form controls', async ({ page }) => {
    await page.setContent(`
      <form id="advanced-controls">
        <div style="padding: 20px;">
          <fieldset>
            <legend>Advanced Form Controls</legend>
            
            <div style="margin: 10px 0;">
              <label>File Upload:</label>
              <input type="file" id="file-input" accept=".txt,.json,image/*" multiple>
            </div>
            
            <div style="margin: 10px 0;">
              <label>Progress Bar:</label>
              <progress id="progress" value="50" max="100">50%</progress>
            </div>
            
            <div style="margin: 10px 0;">
              <label>Meter:</label>
              <meter id="meter" value="75" min="0" max="100" optimum="80">75%</meter>
            </div>
            
            <div style="margin: 10px 0;">
              <label>Datalist with autocomplete:</label>
              <input list="browsers" id="datalist-input" placeholder="Choose or type...">
              <datalist id="browsers">
                <option value="Chrome">
                <option value="Firefox">
                <option value="Safari">
                <option value="Edge">
              </datalist>
            </div>
            
            <div style="margin: 10px 0;">
              <label>Output element:</label>
              <input type="range" id="range-input" min="0" max="100" value="50" oninput="document.getElementById('output').value = this.value">
              <output id="output">50</output>
            </div>
          </fieldset>
        </div>
      </form>
    `);

    // Test that advanced controls are visible and functional
    const progressBar = page.locator('#progress');
    const meterElement = page.locator('#meter');
    const datalistInput = page.locator('#datalist-input');
    const fileInput = page.locator('#file-input');
    const rangeInput = page.locator('#range-input');
    const output = page.locator('#output');

    await expect(progressBar).toBeVisible();
    await expect(meterElement).toBeVisible();
    await expect(datalistInput).toBeVisible();
    await expect(fileInput).toBeVisible();

    // Test datalist functionality
    await datalistInput.click();
    await datalistInput.type('Chr');
    // Some browsers might show autocomplete suggestions

    // Test range input and output synchronization
    await rangeInput.fill('75');
    const outputValue = await output.textContent();
    expect(outputValue).toBe('75');

    // Take screenshot of advanced controls
    await expect(page.locator('#advanced-controls')).toHaveScreenshot('advanced-form-controls.png');
  });

  test('should support form submission methods', async ({ page }) => {
    await page.setContent(`
      <form id="submission-form" method="post" action="/test-submit">
        <input type="text" name="username" value="testuser">
        <input type="email" name="email" value="test@example.com">
        <button type="submit" id="submit-btn">Submit</button>
        <button type="button" id="ajax-submit">AJAX Submit</button>
        <input type="reset" id="reset-btn" value="Reset">
      </form>
      
      <script>
        document.getElementById('ajax-submit').addEventListener('click', function(e) {
          e.preventDefault();
          const formData = new FormData(document.getElementById('submission-form'));
          
          // Test FormData API
          const data = {};
          for (let [key, value] of formData.entries()) {
            data[key] = value;
          }
          
          window.formSubmissionResult = {
            method: 'ajax',
            data: data
          };
        });
        
        document.getElementById('submission-form').addEventListener('submit', function(e) {
          e.preventDefault();
          window.formSubmissionResult = {
            method: 'native',
            formData: Object.fromEntries(new FormData(this))
          };
        });
      </script>
    `);

    const submitButton = page.locator('#submit-btn');
    const ajaxButton = page.locator('#ajax-submit');
    const resetButton = page.locator('#reset-btn');

    // Test native form submission
    await submitButton.click();
    
    const nativeSubmissionResult = await page.evaluate(() => 
      (window as unknown as { formSubmissionResult?: { method: string; formData: { [key: string]: string } } }).formSubmissionResult
    );

    expect(nativeSubmissionResult?.method).toBe('native');
    expect(nativeSubmissionResult?.formData.username).toBe('testuser');
    expect(nativeSubmissionResult?.formData.email).toBe('test@example.com');

    // Test AJAX submission with FormData
    await ajaxButton.click();
    
    const ajaxSubmissionResult = await page.evaluate(() => 
      (window as unknown as { formSubmissionResult?: { method: string; data: { [key: string]: string } } }).formSubmissionResult
    );

    expect(ajaxSubmissionResult?.method).toBe('ajax');
    expect(ajaxSubmissionResult?.data.username).toBe('testuser');
    expect(ajaxSubmissionResult?.data.email).toBe('test@example.com');

    console.log('Form Submission Tests Passed');
  });

  test('should handle form autofill and autocomplete', async ({ page }) => {
    await page.setContent(`
      <form id="autofill-form">
        <div style="padding: 20px;">
          <input type="text" name="name" autocomplete="name" placeholder="Full Name">
          <input type="email" name="email" autocomplete="email" placeholder="Email">
          <input type="tel" name="phone" autocomplete="tel" placeholder="Phone">
          <input type="password" name="password" autocomplete="current-password" placeholder="Password">
          <input type="text" name="organization" autocomplete="organization" placeholder="Company">
          <input type="text" name="address" autocomplete="street-address" placeholder="Address">
          <input type="text" name="city" autocomplete="address-level2" placeholder="City">
          <input type="text" name="postal" autocomplete="postal-code" placeholder="Postal Code">
          <input type="text" name="country" autocomplete="country" placeholder="Country">
        </div>
      </form>
    `);

    // Test that autocomplete attributes are properly set
    const autocompleteAttributes = await page.evaluate(() => {
      const form = document.getElementById('autofill-form');
      const inputs = form?.querySelectorAll('input[autocomplete]');
      const attributes: { [key: string]: string } = {};
      
      inputs?.forEach((input) => {
        const inputElement = input as HTMLInputElement;
        attributes[inputElement.name] = inputElement.getAttribute('autocomplete') || '';
      });
      
      return attributes;
    });

    expect(autocompleteAttributes.name).toBe('name');
    expect(autocompleteAttributes.email).toBe('email');
    expect(autocompleteAttributes.phone).toBe('tel');
    expect(autocompleteAttributes.password).toBe('current-password');

    // Take screenshot of autofill form
    await expect(page.locator('#autofill-form')).toHaveScreenshot('autofill-form.png');
  });
});

test.describe('Keyboard and Accessibility', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should support keyboard navigation', async ({ page }) => {
    await page.setContent(`
      <form id="keyboard-nav-form">
        <div style="padding: 20px;">
          <input type="text" id="input1" placeholder="Input 1">
          <select id="select1">
            <option value="option1">Option 1</option>
            <option value="option2">Option 2</option>
            <option value="option3">Option 3</option>
          </select>
          <textarea id="textarea1" placeholder="Textarea 1"></textarea>
          <button type="button" id="button1">Button 1</button>
          <input type="checkbox" id="checkbox1">
          <label for="checkbox1">Checkbox</label>
          <input type="radio" id="radio1" name="radio-group" value="radio1">
          <label for="radio1">Radio 1</label>
          <input type="radio" id="radio2" name="radio-group" value="radio2">
          <label for="radio2">Radio 2</label>
          <button type="submit" id="submit-btn">Submit</button>
        </div>
      </form>
    `);

    // Test Tab navigation
    await page.keyboard.press('Tab');
    let focused = await page.evaluate(() => document.activeElement?.id);
    expect(focused).toBe('input1');

    await page.keyboard.press('Tab');
    focused = await page.evaluate(() => document.activeElement?.id);
    expect(focused).toBe('select1');

    await page.keyboard.press('Tab');
    focused = await page.evaluate(() => document.activeElement?.id);
    expect(focused).toBe('textarea1');

    // Test Shift+Tab (reverse navigation)
    await page.keyboard.press('Shift+Tab');
    focused = await page.evaluate(() => document.activeElement?.id);
    expect(focused).toBe('select1');

    // Test Enter key on buttons
    const button1 = page.locator('#button1');
    await button1.focus();
    
    let buttonClicked = false;
    await button1.evaluate((btn) => {
      btn.addEventListener('click', () => {
        (window as unknown as { buttonClicked: boolean }).buttonClicked = true;
      });
    });
    
    await page.keyboard.press('Enter');
    buttonClicked = await page.evaluate(() => (window as unknown as { buttonClicked?: boolean }).buttonClicked || false);
    expect(buttonClicked).toBe(true);

    // Test Space key on checkbox
    const checkbox = page.locator('#checkbox1');
    await checkbox.focus();
    await page.keyboard.press('Space');
    
    const checkboxChecked = await checkbox.isChecked();
    expect(checkboxChecked).toBe(true);

    console.log('Keyboard Navigation Tests Passed');
  });

  test('should support ARIA attributes and screen readers', async ({ page }) => {
    await page.setContent(`
      <form id="aria-form">
        <div style="padding: 20px;">
          <div role="group" aria-labelledby="personal-info">
            <h2 id="personal-info">Personal Information</h2>
            
            <label for="name-input">Name *</label>
            <input type="text" id="name-input" required aria-required="true" aria-describedby="name-help">
            <div id="name-help" role="status" aria-live="polite">Enter your full name</div>
            
            <label for="email-input">Email *</label>
            <input type="email" id="email-input" required aria-required="true" aria-invalid="false" aria-describedby="email-error">
            <div id="email-error" role="alert" aria-live="assertive" style="color: red; display: none;">Please enter a valid email</div>
            
            <fieldset>
              <legend>Preferred Contact Method</legend>
              <input type="radio" id="contact-email" name="contact" value="email" aria-describedby="contact-help">
              <label for="contact-email">Email</label>
              
              <input type="radio" id="contact-phone" name="contact" value="phone" aria-describedby="contact-help">
              <label for="contact-phone">Phone</label>
              
              <div id="contact-help">Select your preferred method of contact</div>
            </fieldset>
            
            <button type="submit" aria-describedby="submit-help">Submit Form</button>
            <div id="submit-help">Click to submit the form data</div>
          </div>
        </div>
        
        <script>
          const emailInput = document.getElementById('email-input');
          const emailError = document.getElementById('email-error');
          
          emailInput.addEventListener('blur', function() {
            if (this.validity.typeMismatch) {
              this.setAttribute('aria-invalid', 'true');
              emailError.style.display = 'block';
              emailError.textContent = 'Please enter a valid email address';
            } else {
              this.setAttribute('aria-invalid', 'false');
              emailError.style.display = 'none';
            }
          });
        </script>
      </form>
    `);

    // Test ARIA attributes presence
    const ariaAttributes = await page.evaluate(() => {
      const nameInput = document.getElementById('name-input') as HTMLInputElement;
      const emailInput = document.getElementById('email-input') as HTMLInputElement;
      const group = document.querySelector('[role="group"]');
      const alert = document.querySelector('[role="alert"]');
      const status = document.querySelector('[role="status"]');
      
      return {
        nameRequired: nameInput?.getAttribute('aria-required'),
        nameDescribedBy: nameInput?.getAttribute('aria-describedby'),
        emailInvalid: emailInput?.getAttribute('aria-invalid'),
        groupRole: group?.getAttribute('role'),
        groupLabelledBy: group?.getAttribute('aria-labelledby'),
        alertRole: alert?.getAttribute('role'),
        statusRole: status?.getAttribute('role'),
        statusLive: status?.getAttribute('aria-live')
      };
    });

    expect(ariaAttributes.nameRequired).toBe('true');
    expect(ariaAttributes.nameDescribedBy).toBe('name-help');
    expect(ariaAttributes.emailInvalid).toBe('false');
    expect(ariaAttributes.groupRole).toBe('group');
    expect(ariaAttributes.alertRole).toBe('alert');
    expect(ariaAttributes.statusRole).toBe('status');
    expect(ariaAttributes.statusLive).toBe('polite');

    // Test dynamic ARIA changes
    const emailInput = page.locator('#email-input');
    await emailInput.fill('invalid-email');
    await emailInput.blur();

    const ariaInvalid = await emailInput.getAttribute('aria-invalid');
    expect(ariaInvalid).toBe('true');

    const errorVisible = await page.locator('#email-error').isVisible();
    expect(errorVisible).toBe(true);

    console.log('ARIA Attributes Tests Passed');
  });

  test('should support focus management', async ({ page }) => {
    await page.setContent(`
      <div id="focus-management">
        <button id="open-modal">Open Modal</button>
        
        <div id="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title" style="display: none; position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%); background: white; border: 2px solid black; padding: 20px; z-index: 1000;">
          <h2 id="modal-title">Modal Dialog</h2>
          <p>This is a modal dialog for testing focus management.</p>
          <input type="text" id="modal-input" placeholder="Modal input">
          <button id="modal-ok">OK</button>
          <button id="modal-cancel">Cancel</button>
        </div>
        
        <div id="modal-overlay" style="display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 999;"></div>
        
        <script>
          const openModalBtn = document.getElementById('open-modal');
          const modal = document.getElementById('modal');
          const modalOverlay = document.getElementById('modal-overlay');
          const modalOk = document.getElementById('modal-ok');
          const modalCancel = document.getElementById('modal-cancel');
          const modalInput = document.getElementById('modal-input');
          
          let lastFocused;
          
          openModalBtn.addEventListener('click', function() {
            lastFocused = document.activeElement;
            modal.style.display = 'block';
            modalOverlay.style.display = 'block';
            modalInput.focus();
            
            // Trap focus within modal
            const focusableElements = modal.querySelectorAll('input, button');
            const firstFocusable = focusableElements[0];
            const lastFocusable = focusableElements[focusableElements.length - 1];
            
            modal.addEventListener('keydown', function(e) {
              if (e.key === 'Tab') {
                if (e.shiftKey) {
                  if (document.activeElement === firstFocusable) {
                    e.preventDefault();
                    lastFocusable.focus();
                  }
                } else {
                  if (document.activeElement === lastFocusable) {
                    e.preventDefault();
                    firstFocusable.focus();
                  }
                }
              }
              
              if (e.key === 'Escape') {
                closeModal();
              }
            });
          });
          
          function closeModal() {
            modal.style.display = 'none';
            modalOverlay.style.display = 'none';
            if (lastFocused) {
              lastFocused.focus();
            }
          }
          
          modalOk.addEventListener('click', closeModal);
          modalCancel.addEventListener('click', closeModal);
          modalOverlay.addEventListener('click', closeModal);
        </script>
      </div>
    `);

    // Test modal opening and focus management
    const openModalButton = page.locator('#open-modal');
    const modal = page.locator('#modal');
    const modalInput = page.locator('#modal-input');
    const modalCancel = page.locator('#modal-cancel');

    // Open modal
    await openModalButton.click();
    await expect(modal).toBeVisible();

    // Check that focus moved to modal input
    const focusedElement = await page.evaluate(() => document.activeElement?.id);
    expect(focusedElement).toBe('modal-input');

    // Test focus trapping with Tab
    await page.keyboard.press('Tab');
    const focusedAfterTab = await page.evaluate(() => document.activeElement?.id);
    expect(focusedAfterTab).toBe('modal-ok');

    // Test Escape key to close modal
    await page.keyboard.press('Escape');
    await expect(modal).toBeHidden();

    // Check that focus returned to original element
    const focusedAfterClose = await page.evaluate(() => document.activeElement?.id);
    expect(focusedAfterClose).toBe('open-modal');

    console.log('Focus Management Tests Passed');
  });
});

test.describe('Browser-Specific Form Behavior', () => {
  test('should handle Safari form quirks', async ({ page, browserName }) => {
    test.skip(browserName !== 'webkit', 'Safari-specific test');

    await page.setContent(`
      <form id="safari-form">
        <input type="date" id="safari-date">
        <input type="time" id="safari-time">
        <input type="file" id="safari-file" accept="image/*">
      </form>
    `);

    const dateInput = page.locator('#safari-date');
    const timeInput = page.locator('#safari-time');
    const fileInput = page.locator('#safari-file');

    // Safari has specific behavior for date/time inputs
    await expect(dateInput).toBeVisible();
    await expect(timeInput).toBeVisible();
    await expect(fileInput).toBeVisible();

    console.log('Safari form tests completed');
  });

  test('should handle Chrome form enhancements', async ({ page, browserName }) => {
    test.skip(browserName !== 'chromium', 'Chrome-specific test');

    const chromeFormFeatures = await page.evaluate(() => {
      return {
        paymentRequest: 'PaymentRequest' in window,
        webAuthn: 'credentials' in navigator && 'create' in (navigator.credentials as CredentialsContainer),
        formAssociated: 'ElementInternals' in window
      };
    });

    console.log('Chrome Form Features:', chromeFormFeatures);
  });

  test('should handle Firefox form behavior', async ({ page, browserName }) => {
    test.skip(browserName !== 'firefox', 'Firefox-specific test');

    await page.setContent(`
      <form id="firefox-form">
        <input type="number" id="firefox-number" step="0.01">
        <input type="search" id="firefox-search">
      </form>
    `);

    const numberInput = page.locator('#firefox-number');
    const searchInput = page.locator('#firefox-search');

    await expect(numberInput).toBeVisible();
    await expect(searchInput).toBeVisible();

    console.log('Firefox form tests completed');
  });
});