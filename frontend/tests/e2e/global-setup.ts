import { chromium, FullConfig } from '@playwright/test';
import { TestDataManager } from './helpers/test-data-manager';

/**
 * Global setup runs before all tests
 * Sets up test environment, creates test users, etc.
 */
async function globalSetup(config: FullConfig) {
  console.log('🚀 Starting E2E test setup...');
  
  // Initialize test data manager
  const testDataManager = new TestDataManager();
  
  try {
    // Wait for the application to be ready
    const browser = await chromium.launch();
    const context = await browser.newContext();
    const page = await context.newPage();
    
    // Check if the app is ready
    const baseURL = config.projects[0]?.use?.baseURL || 'http://localhost:5173';
    console.log(`⏳ Waiting for application at ${baseURL}...`);
    
    await page.goto(baseURL, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('body', { timeout: 30000 });
    
    console.log('✅ Application is ready');
    
    // Setup test environment
    await testDataManager.setupTestEnvironment();
    
    console.log('✅ Test data setup complete');
    
    await browser.close();
    
    console.log('🎉 Global setup completed successfully');
  } catch (error) {
    console.error('❌ Global setup failed:', error);
    throw error;
  }
}

export default globalSetup;