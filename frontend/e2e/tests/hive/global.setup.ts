/**
 * Global Setup for Hive Workflow E2E Tests
 * Handles test environment preparation, database seeding, and authentication setup
 */

import { chromium, FullConfig, Page } from '@playwright/test';
import { HIVE_TEST_USERS } from './hive-fixtures';

async function globalSetup(config: FullConfig) {
  console.log('🚀 Setting up Hive Workflow E2E Test Environment...');
  
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();
  
  try {
    // Step 1: Wait for services to be ready
    console.log('⏳ Waiting for backend services...');
    
    const services = [
      { name: 'FocusHive Backend', url: process.env.E2E_API_BASE_URL || 'http://localhost:8080' },
      { name: 'Identity Service', url: process.env.E2E_IDENTITY_API_URL || 'http://localhost:8081' },
      { name: 'Frontend', url: config.projects[0]?.use?.baseURL || 'http://127.0.0.1:5173' }
    ];
    
    for (const service of services) {
      await waitForService(page, service.name, service.url);
    }
    
    // Step 2: Create test users if they don't exist
    console.log('👥 Setting up test users...');
    
    for (const [userKey, userData] of Object.entries(HIVE_TEST_USERS)) {
      try {
        await createTestUser(page, userData);
        console.log(`✅ Test user created/verified: ${userData.username}`);
      } catch (error) {
        console.warn(`⚠️  Warning creating user ${userData.username}:`, error);
      }
    }
    
    // Step 3: Clean up any existing test data
    console.log('🧹 Cleaning up previous test data...');
    await cleanupTestData(page);
    
    // Step 4: Store authentication states
    console.log('🔐 Pre-authenticating test users...');
    for (const [userKey, userData] of Object.entries(HIVE_TEST_USERS)) {
      await authenticateUser(page, userData, `auth-${userKey.toLowerCase()}.json`);
    }
    
    console.log('✅ Global setup completed successfully!');
    
  } catch (error) {
    console.error('❌ Global setup failed:', error);
    throw error;
  } finally {
    await context.close();
    await browser.close();
  }
}

/**
 * Wait for a service to be ready
 */
async function waitForService(page: Page, name: string, url: string, maxAttempts: number = 30): Promise<void> {
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      const response = await page.request.get(`${url}/health`).catch(() => 
        page.request.get(`${url}/actuator/health`).catch(() =>
          page.request.get(url)
        )
      );
      
      if (response.ok()) {
        console.log(`✅ ${name} is ready`);
        return;
      }
    } catch (error) {
      // Service not ready yet
    }
    
    if (attempt === maxAttempts) {
      throw new Error(`${name} failed to start after ${maxAttempts} attempts`);
    }
    
    console.log(`⏳ Waiting for ${name}... (attempt ${attempt}/${maxAttempts})`);
    await new Promise(resolve => setTimeout(resolve, 2000));
  }
}

/**
 * Create a test user via API
 */
async function createTestUser(page: Page, userData: typeof HIVE_TEST_USERS[keyof typeof HIVE_TEST_USERS]): Promise<void> {
  const identityServiceUrl = process.env.E2E_IDENTITY_API_URL || 'http://localhost:8081';
  
  // Try to register the user
  const registerData = {
    username: userData.username,
    email: userData.email,
    password: userData.password,
    displayName: userData.displayName,
    firstName: userData.displayName.split(' ')[0],
    lastName: userData.displayName.split(' ')[1] || ''
  };
  
  try {
    const response = await page.request.post(`${identityServiceUrl}/api/auth/register`, {
      data: registerData,
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    // User might already exist, which is fine
    if (response.status() === 409) {
      console.log(`🔄 User ${userData.username} already exists`);
      return;
    }
    
    if (!response.ok()) {
      throw new Error(`Registration failed with status ${response.status()}`);
    }
    
  } catch (error) {
    // If registration fails, try to verify user exists by attempting login
    try {
      const loginResponse = await page.request.post(`${identityServiceUrl}/api/auth/login`, {
        data: {
          username: userData.username,
          password: userData.password
        }
      });
      
      if (loginResponse.ok()) {
        console.log(`🔄 User ${userData.username} already exists and can login`);
        return;
      }
    } catch (loginError) {
      // Both registration and login failed
      throw new Error(`Failed to create or verify user ${userData.username}: ${error}`);
    }
  }
}

/**
 * Authenticate a user and store the session
 */
async function authenticateUser(
  page: Page, 
  userData: typeof HIVE_TEST_USERS[keyof typeof HIVE_TEST_USERS],
  fileName: string
): Promise<void> {
  const identityServiceUrl = process.env.E2E_IDENTITY_API_URL || 'http://localhost:8081';
  const frontendUrl = process.env.E2E_BASE_URL || 'http://127.0.0.1:5173';
  
  // Navigate to login page
  await page.goto(`${frontendUrl}/login`);
  
  // Perform login
  await page.fill('[data-testid="username-input"]', userData.username);
  await page.fill('[data-testid="password-input"]', userData.password);
  await page.click('[data-testid="login-button"]');
  
  // Wait for successful login (redirect to dashboard)
  try {
    await page.waitForURL('**/dashboard', { timeout: 10000 });
    
    // Store authentication state
    await page.context().storageState({ path: fileName });
    console.log(`✅ Authenticated ${userData.username}`);
    
  } catch (error) {
    console.warn(`⚠️  Authentication failed for ${userData.username}:`, error);
    
    // Try alternative login method or handle specific login flow
    const currentUrl = page.url();
    if (currentUrl.includes('/login')) {
      // Check for error messages
      const errorMessage = await page.locator('[data-testid="login-error"]').textContent().catch(() => null);
      console.log(`Login error for ${userData.username}:`, errorMessage);
    }
  }
}

/**
 * Clean up any existing test data
 */
async function cleanupTestData(page: Page): Promise<void> {
  const apiBaseUrl = process.env.E2E_API_BASE_URL || 'http://localhost:8080';
  
  try {
    // Clean up test hives (those with test patterns in the name)
    const cleanupResponse = await page.request.post(`${apiBaseUrl}/api/test/cleanup`, {
      data: {
        cleanupType: 'hives',
        pattern: 'Test|E2E|temp|staging'
      },
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    if (cleanupResponse.ok()) {
      console.log('✅ Test data cleanup completed');
    } else {
      console.log('⚠️  Test cleanup endpoint not available (this is normal in production)');
    }
    
  } catch (error) {
    console.log('⚠️  Test cleanup not available:', error.message);
  }
}

export default globalSetup;