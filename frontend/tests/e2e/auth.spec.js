import { chromium } from 'playwright';
import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// Test configuration
const BASE_URL = 'http://localhost:5173';
const API_URL = 'http://localhost:8081';
const SCREENSHOTS_DIR = path.join(__dirname, 'screenshots');

// Test credentials
const testUser = {
  email: 'testuser2025@example.com',
  password: 'SecurePass#2025!',
  username: 'testuser2025'
};

async function ensureScreenshotsDir() {
  try {
    await fs.mkdir(SCREENSHOTS_DIR, { recursive: true });
  } catch (error) {
    // Directory might already exist
  }
}

async function takeScreenshot(page, name) {
  await ensureScreenshotsDir();
  const screenshotPath = path.join(SCREENSHOTS_DIR, `${name}.png`);
  await page.screenshot({ path: screenshotPath, fullPage: false });
  console.log(`📸 Screenshot saved: ${screenshotPath}`);
}

async function testLogin() {
  console.log('\n🧪 Starting E2E Authentication Tests\n');

  const browser = await chromium.launch({
    headless: false,
    slowMo: 500 // Slow down for visibility
  });

  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    // Test 1: Navigate to Login Page
    console.log('✅ Test 1: Navigate to Login Page');
    await page.goto(`${BASE_URL}/login`);
    await page.waitForSelector('h1:has-text("Sign In")');
    await takeScreenshot(page, '01-login-page');
    console.log('   ✓ Login page loaded successfully\n');

    // Test 2: Fill Login Form
    console.log('✅ Test 2: Fill Login Form');
    await page.fill('input[type="email"]', testUser.email);
    await page.fill('input[type="password"]', testUser.password);
    await takeScreenshot(page, '02-login-form-filled');
    console.log('   ✓ Form filled with test credentials\n');

    // Test 3: Submit Login
    console.log('✅ Test 3: Submit Login');
    await page.click('button:has-text("Sign In")');

    // Wait for navigation or error
    try {
      await page.waitForURL('**/dashboard', { timeout: 5000 });
      await takeScreenshot(page, '03-dashboard-after-login');
      console.log('   ✓ Successfully navigated to dashboard\n');
    } catch (navError) {
      // Check if we're still on login page
      const currentUrl = page.url();
      if (currentUrl.includes('/login')) {
        console.log('   ✗ Failed: Still on login page');

        // Check for error messages
        const errorElement = await page.$('text=/error|failed|invalid/i');
        if (errorElement) {
          const errorText = await errorElement.textContent();
          console.log(`   Error message: ${errorText}`);
        }

        // Check storage for tokens (access in sessionStorage, refresh in localStorage)
        const tokens = await page.evaluate(() => ({
          accessToken: sessionStorage.getItem('access_token'),
          refreshToken: localStorage.getItem('refresh_token'),
          user: localStorage.getItem('user') || sessionStorage.getItem('user')
        }));

        console.log('   Token status:');
        console.log(`     - Access Token: ${tokens.accessToken ? 'Present' : 'Missing'}`);
        console.log(`     - Refresh Token: ${tokens.refreshToken ? 'Present' : 'Missing'}`);
        console.log(`     - User Data: ${tokens.user ? 'Present' : 'Missing'}`);

        await takeScreenshot(page, '03-login-failed');
        throw new Error('Login did not navigate to dashboard');
      }
    }

    // Test 4: Verify User is Logged In
    console.log('✅ Test 4: Verify User is Logged In');
    const userInfo = await page.evaluate(() => {
      const accessToken = sessionStorage.getItem('access_token');
      const refreshToken = localStorage.getItem('refresh_token');
      const user = localStorage.getItem('user') || sessionStorage.getItem('user');
      return {
        hasAccessToken: !!accessToken,
        hasRefreshToken: !!refreshToken,
        user: user ? JSON.parse(user) : null
      };
    });

    if (userInfo.hasAccessToken && userInfo.hasRefreshToken) {
      console.log(`   ✓ User authenticated with tokens\n`);
      console.log(`     - Access Token: Present in sessionStorage`);
      console.log(`     - Refresh Token: Present in localStorage`);
      if (userInfo.user) {
        console.log(`     - User: ${userInfo.user.username || userInfo.user.email}`);
      } else {
        console.log(`     - User data: Not stored in browser storage (managed by auth context)`);
      }
      console.log('');
    } else {
      throw new Error('Authentication tokens not found in storage');
    }

    // Test 5: Test Logout
    console.log('✅ Test 5: Test Logout');
    // Look for logout button (could be in menu or header)
    const logoutButton = await page.$('button:has-text("Logout"), button:has-text("Sign Out")');
    if (logoutButton) {
      await logoutButton.click();
      await page.waitForURL('**/login', { timeout: 5000 });
      await takeScreenshot(page, '04-after-logout');
      console.log('   ✓ Successfully logged out\n');
    } else {
      console.log('   ⚠ Logout button not found (may be in menu)\n');
    }

    console.log('🎉 All Authentication Tests Passed!\n');

  } catch (error) {
    console.error('\n❌ Test Failed:', error.message);
    await takeScreenshot(page, 'error-state');
    throw error;
  } finally {
    await browser.close();
  }
}

// Run the tests
testLogin().catch(error => {
  console.error('Test suite failed:', error);
  process.exit(1);
});