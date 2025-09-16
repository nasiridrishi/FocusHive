/**
 * Data Validation and Edge Case E2E Tests for FocusHive
 * Comprehensive testing of input validation, boundary conditions, and security edge cases
 *
 * Test Coverage:
 * - Invalid input formats and data types
 * - Boundary value testing (min/max limits)
 * - SQL injection and XSS prevention
 * - File upload validation and security
 * - Malformed JSON and XML handling
 * - Unicode and special character handling
 * - Empty, null, and undefined value handling
 * - CSRF and security token validation
 */

import {BrowserContext, expect, Page, test} from '@playwright/test';
import {ErrorHandlingHelper} from '../../helpers/error-handling.helper';
import {AuthHelper} from '../../helpers/auth-helpers';
import {TEST_USERS, TIMEOUTS, validateTestEnvironment} from '../../helpers/test-data';

test.describe('Data Validation and Edge Cases', () => {
  let page: Page;
  let context: BrowserContext;
  let errorHelper: ErrorHandlingHelper;
  let authHelper: AuthHelper;

  test.beforeEach(async ({page: testPage, context: testContext}) => {
    page = testPage;
    context = testContext;
    errorHelper = new ErrorHandlingHelper(page, context);
    authHelper = new AuthHelper(page);

    validateTestEnvironment();

    // Set up validation monitoring
    await errorHelper.setupValidationMonitoring();
  });

  test.afterEach(async () => {
    await errorHelper.cleanup();
  });

  test.describe('Input Format Validation', () => {
    test('should validate email format with comprehensive patterns', async () => {
      await page.goto('/register');

      const invalidEmails = [
        'invalid-email',
        '@domain.com',
        'user@',
        'user@.com',
        'user..name@domain.com',
        'user@domain',
        'user@domain..com',
        'user name@domain.com',
        'user@domain,com',
        'user@domain.com.',
        '.user@domain.com',
        'user@domain.c',
        'user@domain.co.uk.extra',
        'user+tag@domain@extra.com'
      ];

      for (const email of invalidEmails) {
        await page.fill('[data-testid="email-input"]', email);
        await page.click('[data-testid="register-btn"]');

        // Should show specific email validation error
        await expect(page.locator('[data-testid="email-validation-error"]')).toBeVisible();
        await expect(page.locator('[data-testid="email-validation-error"]')).toContainText(/invalid.*email|email.*format/i);

        // Form should not submit
        await expect(page.url()).toContain('/register');

        // Clear for next test
        await page.fill('[data-testid="email-input"]', '');
      }

      // Test valid email formats
      const validEmails = [
        'user@domain.com',
        'user.name@domain.com',
        'user+tag@domain.com',
        'user123@domain123.com',
        'user@subdomain.domain.com',
        'user@domain-name.com',
        'user@domain.co.uk'
      ];

      for (const email of validEmails) {
        await page.fill('[data-testid="email-input"]', email);

        // Should not show email validation error
        const errorLocator = page.locator('[data-testid="email-validation-error"]');
        if (await errorLocator.isVisible()) {
          await expect(errorLocator).not.toBeVisible();
        }
      }
    });

    test('should validate password strength with security requirements', async () => {
      await page.goto('/register');

      const weakPasswords = [
        '123',
        'password',
        '12345678',
        'abcdefgh',
        'ABCDEFGH',
        'Password',
        '12345678a',
        'aaaaaaaaA1!',
        'short1A!'
      ];

      for (const password of weakPasswords) {
        await page.fill('[data-testid="password-input"]', password);
        await page.click('[data-testid="register-btn"]');

        // Should show password strength error
        await expect(page.locator('[data-testid="password-strength-error"]')).toBeVisible();

        // Should show specific strength requirements
        const strengthRequirements = page.locator('[data-testid="password-requirements"]');
        await expect(strengthRequirements).toBeVisible();

        // Verify individual requirement checks
        if (password.length < 8) {
          await expect(page.locator('[data-testid="length-requirement"]')).toHaveClass(/failed|invalid/);
        }
        if (!/[a-z]/.test(password)) {
          await expect(page.locator('[data-testid="lowercase-requirement"]')).toHaveClass(/failed|invalid/);
        }
        if (!/[A-Z]/.test(password)) {
          await expect(page.locator('[data-testid="uppercase-requirement"]')).toHaveClass(/failed|invalid/);
        }
        if (!/\d/.test(password)) {
          await expect(page.locator('[data-testid="number-requirement"]')).toHaveClass(/failed|invalid/);
        }
        if (!/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
          await expect(page.locator('[data-testid="special-requirement"]')).toHaveClass(/failed|invalid/);
        }

        await page.fill('[data-testid="password-input"]', '');
      }

      // Test strong password
      await page.fill('[data-testid="password-input"]', 'StrongPass123!@#');

      // Should show all requirements as satisfied
      await expect(page.locator('[data-testid="length-requirement"]')).toHaveClass(/passed|valid/);
      await expect(page.locator('[data-testid="lowercase-requirement"]')).toHaveClass(/passed|valid/);
      await expect(page.locator('[data-testid="uppercase-requirement"]')).toHaveClass(/passed|valid/);
      await expect(page.locator('[data-testid="number-requirement"]')).toHaveClass(/passed|valid/);
      await expect(page.locator('[data-testid="special-requirement"]')).toHaveClass(/passed|valid/);
    });

    test('should validate phone number formats internationally', async () => {
      await page.goto('/profile/edit');
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

      const invalidPhones = [
        '123',
        'abc-def-ghij',
        '1234567890123456',
        '+',
        '+1-',
        '++1234567890',
        '123-45-6789',
        '(555) 123-45678'
      ];

      for (const phone of invalidPhones) {
        await page.fill('[data-testid="phone-input"]', phone);
        await page.click('[data-testid="save-profile"]');

        await expect(page.locator('[data-testid="phone-validation-error"]')).toBeVisible();
        await page.fill('[data-testid="phone-input"]', '');
      }

      // Test valid international phone formats
      const validPhones = [
        '+1-555-123-4567',
        '+44-20-7946-0958',
        '+81-3-1234-5678',
        '(555) 123-4567',
        '555.123.4567',
        '5551234567'
      ];

      for (const phone of validPhones) {
        await page.fill('[data-testid="phone-input"]', phone);

        const errorLocator = page.locator('[data-testid="phone-validation-error"]');
        if (await errorLocator.isVisible()) {
          await expect(errorLocator).not.toBeVisible();
        }
      }
    });

    test('should validate date formats and ranges', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/create');

      // Test invalid date formats
      const invalidDates = [
        '13/25/2024',
        '02/30/2024',
        '2024-13-01',
        '2024-02-30',
        'invalid-date',
        '32/01/2024',
        '01/32/2024',
        '2024/13/01'
      ];

      for (const date of invalidDates) {
        await page.fill('[data-testid="session-date"]', date);
        await page.click('[data-testid="create-hive-btn"]');

        await expect(page.locator('[data-testid="date-validation-error"]')).toBeVisible();
        await page.fill('[data-testid="session-date"]', '');
      }

      // Test past date validation
      const pastDate = '01/01/2020';
      await page.fill('[data-testid="session-date"]', pastDate);
      await page.click('[data-testid="create-hive-btn"]');

      await expect(page.locator('[data-testid="past-date-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="past-date-error"]')).toContainText(/past.*date|date.*past/i);

      // Test far future date validation
      const farFutureDate = '01/01/2050';
      await page.fill('[data-testid="session-date"]', farFutureDate);
      await page.click('[data-testid="create-hive-btn"]');

      await expect(page.locator('[data-testid="far-future-error"]')).toBeVisible();
    });
  });

  test.describe('Boundary Value Testing', () => {
    test('should enforce string length limits with clear feedback', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/create');

      // Test minimum length validation
      await page.fill('[data-testid="hive-name"]', 'AB');
      await page.click('[data-testid="create-hive-btn"]');

      await expect(page.locator('[data-testid="name-too-short"]')).toBeVisible();
      await expect(page.locator('[data-testid="name-too-short"]')).toContainText(/minimum.*3.*characters/i);

      // Test maximum length validation
      const longName = 'A'.repeat(101);
      await page.fill('[data-testid="hive-name"]', longName);

      // Should prevent input beyond limit or show warning
      const actualValue = await page.inputValue('[data-testid="hive-name"]');
      expect(actualValue.length).toBeLessThanOrEqual(100);

      // If truncated, should show character count
      if (actualValue.length === 100) {
        await expect(page.locator('[data-testid="char-count"]')).toContainText('100/100');
        await expect(page.locator('[data-testid="char-limit-warning"]')).toBeVisible();
      }

      // Test description limits
      const longDescription = 'A'.repeat(1001);
      await page.fill('[data-testid="hive-description"]', longDescription);

      await page.click('[data-testid="create-hive-btn"]');

      await expect(page.locator('[data-testid="description-too-long"]')).toBeVisible();
      await expect(page.locator('[data-testid="description-char-count"]')).toBeVisible();
    });

    test('should validate numeric range limits', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/create');

      // Test minimum participant validation
      await page.fill('[data-testid="max-participants"]', '1');
      await page.click('[data-testid="create-hive-btn"]');

      await expect(page.locator('[data-testid="participants-too-few"]')).toBeVisible();
      await expect(page.locator('[data-testid="participants-too-few"]')).toContainText(/minimum.*2.*participants/i);

      // Test maximum participant validation
      await page.fill('[data-testid="max-participants"]', '101');
      await page.click('[data-testid="create-hive-btn"]');

      await expect(page.locator('[data-testid="participants-too-many"]')).toBeVisible();
      await expect(page.locator('[data-testid="participants-too-many"]')).toContainText(/maximum.*100.*participants/i);

      // Test decimal values in integer fields
      await page.fill('[data-testid="max-participants"]', '5.5');

      // Should either round or show validation error
      const value = await page.inputValue('[data-testid="max-participants"]');
      expect(value).toMatch(/^[1-9]\d*$/); // Integer only

      // Test negative values
      await page.fill('[data-testid="max-participants"]', '-5');
      await page.click('[data-testid="create-hive-btn"]');

      await expect(page.locator('[data-testid="participants-negative"]')).toBeVisible();
    });

    test('should validate time duration limits', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/timer');

      // Test minimum duration
      await page.fill('[data-testid="timer-minutes"]', '0');
      await page.click('[data-testid="start-timer"]');

      await expect(page.locator('[data-testid="duration-too-short"]')).toBeVisible();
      await expect(page.locator('[data-testid="duration-too-short"]')).toContainText(/minimum.*1.*minute/i);

      // Test maximum duration
      await page.fill('[data-testid="timer-minutes"]', '1441'); // > 24 hours
      await page.click('[data-testid="start-timer"]');

      await expect(page.locator('[data-testid="duration-too-long"]')).toBeVisible();
      await expect(page.locator('[data-testid="duration-too-long"]')).toContainText(/maximum.*24.*hours/i);

      // Test invalid time formats
      const invalidTimes = ['abc', '1.5.3', '25:30', '12:60', '-10'];

      for (const time of invalidTimes) {
        await page.fill('[data-testid="timer-minutes"]', time);
        await page.click('[data-testid="start-timer"]');

        await expect(page.locator('[data-testid="time-format-error"]')).toBeVisible();
        await page.fill('[data-testid="timer-minutes"]', '');
      }
    });

    test('should handle file size and type limits', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/profile/edit');

      // Mock file upload with oversized file
      await page.setInputFiles('[data-testid="avatar-upload"]', {
        name: 'large-image.jpg',
        mimeType: 'image/jpeg',
        buffer: Buffer.alloc(10 * 1024 * 1024) // 10MB file
      });

      await expect(page.locator('[data-testid="file-too-large"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});
      await expect(page.locator('[data-testid="file-too-large"]')).toContainText(/file.*too.*large|maximum.*size/i);

      // Test invalid file types
      await page.setInputFiles('[data-testid="avatar-upload"]', {
        name: 'document.pdf',
        mimeType: 'application/pdf',
        buffer: Buffer.alloc(1024)
      });

      await expect(page.locator('[data-testid="invalid-file-type"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});
      await expect(page.locator('[data-testid="invalid-file-type"]')).toContainText(/invalid.*file.*type|supported.*formats/i);

      // Test valid file
      await page.setInputFiles('[data-testid="avatar-upload"]', {
        name: 'profile.jpg',
        mimeType: 'image/jpeg',
        buffer: Buffer.alloc(50 * 1024) // 50KB
      });

      await expect(page.locator('[data-testid="upload-success"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});
    });
  });

  test.describe('Security Validation', () => {
    test('should prevent SQL injection attempts', async () => {
      await page.goto('/login');

      const sqlInjectionPayloads = [
        "'; DROP TABLE users; --",
        "' OR '1'='1",
        "'; UPDATE users SET password='hacked' WHERE '1'='1'; --",
        "' UNION SELECT * FROM users WHERE '1'='1",
        "admin'--",
        "admin' /*",
        "' OR 1=1 --",
        "1' OR '1'='1' UNION SELECT 1,2,3,4,5 --"
      ];

      for (const payload of sqlInjectionPayloads) {
        await page.fill('[data-testid="username"]', payload);
        await page.fill('[data-testid="password"]', 'password');
        await page.click('[data-testid="login-btn"]');

        // Should not cause SQL errors or succeed
        await expect(page.locator('[data-testid="login-failed"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

        // Should not show SQL error details
        const errorText = await page.locator('[data-testid="error-message"]').textContent() || '';
        expect(errorText.toLowerCase()).not.toContain('sql');
        expect(errorText.toLowerCase()).not.toContain('database');
        expect(errorText.toLowerCase()).not.toContain('syntax');

        await page.fill('[data-testid="username"]', '');
      }
    });

    test('should prevent XSS attacks with proper encoding', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/profile/edit');

      const xssPayloads = [
        '<script>alert("XSS")</script>',
        '<img src="x" onerror="alert(1)">',
        'javascript:alert(document.cookie)',
        '<svg onload="alert(1)">',
        '"><script>alert("XSS")</script>',
        '<iframe src="javascript:alert(1)">',
        '<body onload="alert(1)">',
        '<div onclick="alert(1)">Click me</div>'
      ];

      for (const payload of xssPayloads) {
        await page.fill('[data-testid="display-name"]', payload);
        await page.click('[data-testid="save-profile"]');

        // Wait for save to complete
        await expect(page.locator('[data-testid="profile-saved"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

        // Navigate to profile view to check display
        await page.goto('/profile');

        // Should display encoded/escaped content, not execute script
        const displayName = page.locator('[data-testid="user-display-name"]');
        const displayText = await displayName.textContent() || '';

        // Should contain the payload as text, not execute it
        expect(displayText).toContain(payload.replace(/[<>]/g, '')); // Basic check

        // Should not execute JavaScript
        const dialogPromise = page.waitForEvent('dialog', {timeout: 1000}).catch(() => null);
        const dialog = await dialogPromise;
        expect(dialog).toBeNull(); // No alert dialog should appear

        // Reset for next test
        await page.goto('/profile/edit');
      }
    });

    test('should validate CSRF tokens and prevent attacks', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/create');

      // Remove or modify CSRF token
      await page.evaluate(() => {
        const csrfToken = document.querySelector('input[name="csrf_token"]') as HTMLInputElement;
        if (csrfToken) {
          csrfToken.value = 'invalid-token';
        }
      });

      await page.fill('[data-testid="hive-name"]', 'CSRF Test Hive');
      await page.click('[data-testid="create-hive-btn"]');

      // Should reject the request
      await expect(page.locator('[data-testid="csrf-error"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});
      await expect(page.locator('[data-testid="csrf-error"]')).toContainText(/security.*token|csrf.*invalid/i);

      // Should not create the hive
      await page.goto('/dashboard');
      const hiveExists = page.locator('[data-testid="hive-card"]:has-text("CSRF Test Hive")');
      await expect(hiveExists).not.toBeVisible();
    });

    test('should sanitize file uploads and prevent malicious content', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/profile/edit');

      // Test executable file upload
      await page.setInputFiles('[data-testid="avatar-upload"]', {
        name: 'malicious.exe',
        mimeType: 'application/octet-stream',
        buffer: Buffer.from('MZ\x90\x00') // PE header
      });

      await expect(page.locator('[data-testid="dangerous-file-blocked"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Test script disguised as image
      await page.setInputFiles('[data-testid="avatar-upload"]', {
        name: 'script.jpg',
        mimeType: 'image/jpeg',
        buffer: Buffer.from('<script>alert("XSS")</script>')
      });

      await expect(page.locator('[data-testid="malicious-content-detected"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

      // Test file with suspicious extension
      await page.setInputFiles('[data-testid="avatar-upload"]', {
        name: 'image.php.jpg',
        mimeType: 'image/jpeg',
        buffer: Buffer.alloc(1024)
      });

      await expect(page.locator('[data-testid="suspicious-filename"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});
    });
  });

  test.describe('Special Character and Unicode Handling', () => {
    test('should handle unicode characters correctly', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/create');

      const unicodeStrings = [
        '测试中文字符',
        'ñáéíóú àèìòù',
        'Тест кириллицы',
        '🚀 🎯 📊 💡',
        'αβγδε ελληνικά',
        'العربية اختبار',
        'हिंदी परीक्षण',
        '日本語テスト',
        '한국어 테스트'
      ];

      for (const unicodeString of unicodeStrings) {
        await page.fill('[data-testid="hive-name"]', unicodeString);
        await page.fill('[data-testid="hive-description"]', `Description with ${unicodeString}`);
        await page.click('[data-testid="create-hive-btn"]');

        // Should handle unicode correctly
        await expect(page.locator('[data-testid="hive-created"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});

        // Verify on dashboard
        await page.goto('/dashboard');
        await expect(page.locator(`[data-testid="hive-card"]:has-text("${unicodeString}")`)).toBeVisible();

        // Clean up for next test
        await page.click(`[data-testid="delete-hive"]:near([data-testid="hive-card"]:has-text("${unicodeString}"))`);
        await page.click('[data-testid="confirm-delete"]');

        await page.goto('/hive/create');
      }
    });

    test('should handle special characters and escape sequences', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      const specialCharacters = [
        'Message with "quotes" and \'apostrophes\'',
        'Backslashes \\ and forward slashes /',
        'Newlines\nand\ttabs',
        'HTML entities &lt;&gt;&amp;',
        'JSON special chars {}[],:',
        'Regex chars .*+?^$|[]{}()',
        'SQL chars %;--/*',
        'URL chars &?=#+%'
      ];

      for (const message of specialCharacters) {
        await page.fill('[data-testid="chat-input"]', message);
        await page.press('[data-testid="chat-input"]', 'Enter');

        // Should display correctly without breaking UI
        await expect(page.locator('[data-testid="chat-message"]').last()).toContainText(message);

        // Should not break chat functionality
        await expect(page.locator('[data-testid="chat-input"]')).toBeVisible();
        await expect(page.locator('[data-testid="chat-input"]')).toHaveValue('');
      }
    });

    test('should handle extremely long single words and URLs', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/123');

      // Test extremely long word
      const longWord = 'A'.repeat(1000);
      await page.fill('[data-testid="chat-input"]', `This is a very long word: ${longWord}`);
      await page.press('[data-testid="chat-input"]', 'Enter');

      // Should handle gracefully without breaking layout
      const lastMessage = page.locator('[data-testid="chat-message"]').last();
      await expect(lastMessage).toBeVisible();

      // Should apply word-wrap or truncation
      const messageWidth = await lastMessage.evaluate(el => el.scrollWidth);
      const containerWidth = await page.locator('[data-testid="chat-container"]').evaluate(el => el.clientWidth);
      expect(messageWidth).toBeLessThanOrEqual(containerWidth + 50); // Allow some margin

      // Test extremely long URL
      const longUrl = `https://example.com/${'path/'.repeat(200)}file.html`;
      await page.fill('[data-testid="chat-input"]', `Check this URL: ${longUrl}`);
      await page.press('[data-testid="chat-input"]', 'Enter');

      // Should handle URL gracefully
      const urlMessage = page.locator('[data-testid="chat-message"]').last();
      await expect(urlMessage).toBeVisible();

      // URL should be clickable but not break layout
      const linkElement = urlMessage.locator('a');
      if (await linkElement.count() > 0) {
        await expect(linkElement).toHaveAttribute('href', longUrl);
      }
    });
  });

  test.describe('Null, Empty, and Undefined Handling', () => {
    test('should handle empty form submissions gracefully', async () => {
      await page.goto('/register');

      // Submit empty form
      await page.click('[data-testid="register-btn"]');

      // Should show validation errors for required fields
      await expect(page.locator('[data-testid="username-required"]')).toBeVisible();
      await expect(page.locator('[data-testid="email-required"]')).toBeVisible();
      await expect(page.locator('[data-testid="password-required"]')).toBeVisible();

      // Should not proceed to next step
      await expect(page.url()).toContain('/register');

      // Should highlight required fields
      await expect(page.locator('[data-testid="username-input"]')).toHaveClass(/required|error/);
      await expect(page.locator('[data-testid="email-input"]')).toHaveClass(/required|error/);
      await expect(page.locator('[data-testid="password-input"]')).toHaveClass(/required|error/);
    });

    test('should handle whitespace-only input validation', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);
      await page.goto('/hive/create');

      // Test whitespace-only input
      const whitespaceInputs = [
        '   ',
        '\t\t\t',
        '\n\n\n',
        '   \t  \n  ',
        '\u00A0\u00A0\u00A0' // Non-breaking spaces
      ];

      for (const whitespace of whitespaceInputs) {
        await page.fill('[data-testid="hive-name"]', whitespace);
        await page.click('[data-testid="create-hive-btn"]');

        // Should treat as empty and show required field error
        await expect(page.locator('[data-testid="name-required"]')).toBeVisible();

        await page.fill('[data-testid="hive-name"]', '');
      }
    });

    test('should handle null and undefined API responses', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

      // Mock API response with null values
      await context.route('**/api/hives', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            hives: [
              {
                id: '1',
                name: 'Normal Hive',
                description: 'Regular description',
                participants: 5
              },
              {
                id: '2',
                name: null,
                description: undefined,
                participants: null
              },
              {
                id: '3',
                name: '',
                description: '',
                participants: 0
              }
            ]
          })
        });
      });

      await page.goto('/dashboard');

      // Should handle null/undefined values gracefully
      await expect(page.locator('[data-testid="hive-list"]')).toBeVisible();

      // Should show appropriate fallback text for null/undefined values
      const hiveWithNulls = page.locator('[data-testid="hive-card"]').nth(1);
      await expect(hiveWithNulls).toContainText(/untitled|no title|unnamed/i);
      await expect(hiveWithNulls).toContainText(/no description|empty/i);

      // Should handle zero values correctly
      const hiveWithZeros = page.locator('[data-testid="hive-card"]').nth(2);
      await expect(hiveWithZeros).toContainText('0 participants');
    });

    test('should handle missing required API fields', async () => {
      await authHelper.login(TEST_USERS.VALID_USER.username, TEST_USERS.VALID_USER.password);

      // Mock API response with missing required fields
      await context.route('**/api/user/profile', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            // Missing required fields like id, username, email
            displayName: 'Test User',
            avatar: 'avatar.jpg'
          })
        });
      });

      await page.goto('/profile');

      // Should handle missing data gracefully
      await expect(page.locator('[data-testid="profile-error"]')).toBeVisible({timeout: TIMEOUTS.MEDIUM});
      await expect(page.locator('[data-testid="profile-error"]')).toContainText(/incomplete.*data|missing.*information/i);

      // Should offer data recovery options
      await expect(page.locator('[data-testid="reload-profile"]')).toBeVisible();
      await expect(page.locator('[data-testid="contact-support"]')).toBeVisible();
    });
  });
});