import { chromium } from 'playwright';

async function captureDashboard() {
  console.log('Starting browser to capture dashboard...\n');

  const browser = await chromium.launch({
    headless: true
  });

  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    // Navigate to login
    await page.goto('http://localhost:5173/login');
    await page.waitForSelector('h1:has-text("Sign In")');

    // Login
    await page.fill('input[type="email"]', 'testuser2025@example.com');
    await page.fill('input[type="password"]', 'SecurePass#2025!');
    await page.click('button:has-text("Sign In")');

    // Wait for navigation
    await page.waitForURL('**/dashboard', { timeout: 5000 });
    await page.waitForTimeout(2000);

    // Take screenshot
    await page.screenshot({ 
      path: 'tests/e2e/screenshots/dashboard-fixed-user-display.png',
      fullPage: true 
    });
    
    console.log('✅ Screenshot saved: dashboard-fixed-user-display.png');

    // Check what's displayed in the header
    const userDisplay = await page.textContent('.MuiAvatar-root + *');
    console.log('📧 User display in header:', userDisplay);

  } catch (error) {
    console.error('Error:', error.message);
  } finally {
    await browser.close();
  }
}

captureDashboard();
