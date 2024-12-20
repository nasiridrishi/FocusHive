import { chromium } from 'playwright';

async function captureQuickStart() {
  console.log('Capturing Quick Start section...\n');

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

    // Take screenshot of Quick Start section
    const quickStartSection = await page.locator('text=Quick Start').locator('..').locator('..');
    await quickStartSection.screenshot({ 
      path: 'tests/e2e/screenshots/quick-start-fixed.png'
    });
    
    console.log('✅ Screenshot saved: quick-start-fixed.png');

  } catch (error) {
    console.error('Error:', error.message);
  } finally {
    await browser.close();
  }
}

captureQuickStart();
