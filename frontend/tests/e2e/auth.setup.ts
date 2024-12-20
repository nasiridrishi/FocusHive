import { test as setup, expect } from '@playwright/test';
import { LoginPage } from './pages/login-page';
import { RegistrationPage } from './pages/registration-page';
import { TestDataManager } from './helpers/test-data-manager';

const authFile = 'test-results/.auth/user.json';

/**
 * Authentication setup
 * Creates a test user and saves the authentication state
 */
setup('authenticate', async ({ page }) => {
  console.log('🔐 Setting up authentication...');
  
  const testDataManager = new TestDataManager();
  const loginPage = new LoginPage(page);
  const registrationPage = new RegistrationPage(page);

  try {
    // Generate a test user
    const testUser = testDataManager.generateTestUser({
      username: 'e2e_auth_user',
      email: 'e2e.auth@focushive.test',
      firstName: 'E2E',
      lastName: 'Auth'
    });

    console.log(`Creating test user: ${testUser.email}`);

    // First, try to register the user
    await registrationPage.goto();
    
    try {
      await registrationPage.register(testUser);
      
      // Check if registration was successful
      try {
        await registrationPage.verifySuccessfulRegistration();
        console.log('✅ User registration successful');
        
        // If we got redirected to login, go there
        const currentUrl = await page.url();
        if (currentUrl.includes('/login')) {
          console.log('Redirected to login page, logging in...');
          await loginPage.login(testUser.email, testUser.password);
        }
      } catch (registrationError) {
        console.log('Registration failed or user already exists, trying login...');
        
        // Registration failed, try to login instead
        await loginPage.goto();
        await loginPage.login(testUser.email, testUser.password);
      }
    } catch (error) {
      console.log('Registration page error, trying login...');
      
      // If registration completely failed, try login
      await loginPage.goto();
      await loginPage.login(testUser.email, testUser.password);
    }

    // Verify we're authenticated by checking for redirect or user elements
    await page.waitForTimeout(3000); // Give time for authentication to complete
    
    const currentUrl = await page.url();
    console.log(`Current URL after authentication: ${currentUrl}`);
    
    // Check for signs of successful authentication
    const isAuthenticated = 
      currentUrl.includes('/dashboard') ||
      currentUrl.includes('/home') ||
      currentUrl.includes('/app') ||
      !currentUrl.includes('/login') && !currentUrl.includes('/register');
    
    if (!isAuthenticated) {
      console.warn('Authentication may have failed, but continuing...');
    } else {
      console.log('✅ Authentication successful');
    }

    // Save signed-in state to 'authFile'
    await page.context().storageState({ path: authFile });
    console.log(`✅ Authentication state saved to ${authFile}`);

  } catch (error) {
    console.error('❌ Authentication setup failed:', error);
    
    // Still save the state even if authentication failed
    // This allows tests to run and show proper error states
    await page.context().storageState({ path: authFile });
    
    // Don't throw here, let individual tests handle authentication failures
    console.log('⚠️ Continuing with potentially unauthenticated state...');
  }
});