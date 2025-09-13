/**
 * Global Setup for Authentication E2E Tests
 * Prepares test environment and dependencies for authentication testing
 */

import { chromium, FullConfig } from '@playwright/test';

async function globalSetup(config: FullConfig) {
  console.log('🚀 Setting up authentication E2E test environment...');

  // Validate environment variables
  const requiredEnvVars = [
    'E2E_BASE_URL',
    'E2E_IDENTITY_API_URL', 
    'E2E_API_BASE_URL',
  ];

  const missingEnvVars = requiredEnvVars.filter(envVar => !process.env[envVar]);
  if (missingEnvVars.length > 0) {
    console.warn(`⚠️  Missing environment variables: ${missingEnvVars.join(', ')}`);
    console.warn('   Using default values for local development');
  }

  // Set default environment variables
  const defaultEnv = {
    E2E_BASE_URL: 'http://127.0.0.1:5173',
    E2E_IDENTITY_API_URL: 'http://localhost:8081',
    E2E_API_BASE_URL: 'http://localhost:8080',
    E2E_MAILHOG_API_URL: 'http://localhost:8025',
    E2E_MAILHOG_WEB_URL: 'http://localhost:8025',
    E2E_OAUTH_GOOGLE_CLIENT_ID: 'test-google-client-id',
    E2E_OAUTH_GITHUB_CLIENT_ID: 'test-github-client-id',
  };

  for (const [key, value] of Object.entries(defaultEnv)) {
    if (!process.env[key]) {
      process.env[key] = value;
    }
  }

  // Validate services are accessible
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    console.log('🔍 Validating test services...');

    // Check frontend is accessible
    try {
      await page.goto(process.env.E2E_BASE_URL!);
      await page.waitForLoadState('networkidle', { timeout: 30000 });
      console.log('✅ Frontend service is accessible');
    } catch (error) {
      console.error(`❌ Frontend service not accessible at ${process.env.E2E_BASE_URL}`);
      console.error('   Make sure to run: npm run dev');
      throw error;
    }

    // Check identity service is accessible
    try {
      const identityResponse = await page.request.get(`${process.env.E2E_IDENTITY_API_URL}/health`);
      if (identityResponse.ok()) {
        console.log('✅ Identity service is accessible');
      } else {
        console.warn(`⚠️  Identity service returned status: ${identityResponse.status()}`);
      }
    } catch (error) {
      console.warn(`⚠️  Identity service not accessible at ${process.env.E2E_IDENTITY_API_URL}`);
      console.warn('   OAuth tests may be skipped');
    }

    // Check backend API service is accessible
    try {
      const backendResponse = await page.request.get(`${process.env.E2E_API_BASE_URL}/health`);
      if (backendResponse.ok()) {
        console.log('✅ Backend API service is accessible');
      } else {
        console.warn(`⚠️  Backend API service returned status: ${backendResponse.status()}`);
      }
    } catch (error) {
      console.warn(`⚠️  Backend API service not accessible at ${process.env.E2E_API_BASE_URL}`);
      console.warn('   API integration tests may fail');
    }

    // Check MailHog service (optional for email testing)
    try {
      const mailhogResponse = await page.request.get(`${process.env.E2E_MAILHOG_API_URL}/api/v1/messages`);
      if (mailhogResponse.ok()) {
        console.log('✅ MailHog service is accessible for email testing');
        
        // Clear any existing emails
        await page.request.delete(`${process.env.E2E_MAILHOG_API_URL}/api/v1/messages`);
        console.log('📧 Cleared existing test emails');
      } else {
        console.warn(`⚠️  MailHog service returned status: ${mailhogResponse.status()}`);
      }
    } catch (error) {
      console.warn(`⚠️  MailHog service not accessible at ${process.env.E2E_MAILHOG_API_URL}`);
      console.warn('   Email verification tests will be skipped');
      console.warn('   To enable email testing, run: docker run --rm -p 1025:1025 -p 8025:8025 mailhog/mailhog');
    }

    // Prepare test data and cleanup any previous test artifacts
    console.log('🧹 Cleaning up previous test artifacts...');
    
    // Clear browser storage
    await page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });

    // Create test users if database is available
    try {
      const testUsers = [
        {
          username: 'e2e_test_user',
          email: 'e2e.test@focushive.com',
          password: 'TestPassword123!',
          firstName: 'E2E',
          lastName: 'TestUser',
        },
        {
          username: 'e2e_test_user_2',
          email: 'e2e.test2@focushive.com', 
          password: 'TestPassword456!',
          firstName: 'E2E2',
          lastName: 'TestUser2',
        },
      ];

      for (const user of testUsers) {
        try {
          const registerResponse = await page.request.post(`${process.env.E2E_IDENTITY_API_URL}/api/v1/auth/register`, {
            data: user,
          });

          if (registerResponse.ok()) {
            console.log(`✅ Test user ${user.username} prepared`);
          } else {
            // User might already exist, which is fine
            console.log(`ℹ️  Test user ${user.username} already exists or registration failed`);
          }
        } catch (error) {
          console.log(`ℹ️  Could not prepare test user ${user.username}`);
        }
      }
    } catch (error) {
      console.warn('⚠️  Could not prepare test users - tests will use mock data');
    }

    // Validate authentication endpoints
    console.log('🔐 Validating authentication endpoints...');
    
    const authEndpoints = [
      '/api/v1/auth/login',
      '/api/v1/auth/register', 
      '/api/v1/auth/logout',
      '/api/v1/auth/me',
      '/api/v1/auth/forgot-password',
    ];

    for (const endpoint of authEndpoints) {
      try {
        // Make a test request to see if endpoint exists
        const response = await page.request.post(`${process.env.E2E_API_BASE_URL}${endpoint}`, {
          data: { test: true },
          failOnStatusCode: false,
        });

        // Any response (even error) means endpoint exists
        if (response.status() === 404) {
          console.warn(`⚠️  Auth endpoint not found: ${endpoint}`);
        } else {
          console.log(`✅ Auth endpoint available: ${endpoint}`);
        }
      } catch (error) {
        console.warn(`⚠️  Could not validate endpoint: ${endpoint}`);
      }
    }

    // Setup OAuth mock endpoints if providers are not available
    console.log('🔧 Setting up OAuth test environment...');
    
    // These will be handled by individual tests with mocking

    console.log('✅ Authentication E2E test environment setup complete');

  } catch (error) {
    console.error('❌ Failed to setup authentication test environment');
    console.error(error);
    throw error;
  } finally {
    await context.close();
    await browser.close();
  }
}

export default globalSetup;