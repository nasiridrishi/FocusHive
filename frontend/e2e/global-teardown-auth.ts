/**
 * Global Teardown for Authentication E2E Tests
 * Cleans up test environment and artifacts after authentication testing
 */

import {chromium, FullConfig} from '@playwright/test';

async function globalTeardown(_config: FullConfig): Promise<void> {
  console.log('🧹 Cleaning up authentication E2E test environment...');

  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    // Clear MailHog test emails if available
    if (process.env.E2E_MAILHOG_API_URL) {
      try {
        await page.request.delete(`${process.env.E2E_MAILHOG_API_URL}/api/v1/messages`);
        console.log('📧 Cleared all test emails from MailHog');
      } catch {
        console.warn('⚠️  Could not clear MailHog emails (service may not be running)');
      }
    }

    // Clean up test users if database is accessible
    console.log('👥 Cleaning up test users...');

    const testUsernames = [
      'e2e_test_user',
      'e2e_test_user_2',
      'auth_test_user',
      'auth_test_user_2',
      'oauth_link_user',
    ];

    for (const username of testUsernames) {
      try {
        // Try to delete test user (if endpoint exists)
        const deleteResponse = await page.request.delete(
            `${process.env.E2E_IDENTITY_API_URL}/api/v1/admin/users/${username}`,
            {
              failOnStatusCode: false,
            }
        );

        if (deleteResponse.ok()) {
          console.log(`✅ Cleaned up test user: ${username}`);
        } else {
          console.log(`ℹ️  Test user ${username} not found or could not be deleted`);
        }
      } catch {
        console.log(`ℹ️  Could not clean up test user ${username}`);
      }
    }

    // Clear any test sessions/tokens if admin endpoint exists
    try {
      await page.request.post(`${process.env.E2E_API_BASE_URL}/api/v1/admin/clear-test-sessions`, {
        failOnStatusCode: false,
      });
      console.log('🔐 Cleared test authentication sessions');
    } catch {
      console.log('ℹ️  Could not clear test sessions (admin endpoint may not exist)');
    }

    // Clean up OAuth test data if any
    console.log('🔧 Cleaning up OAuth test artifacts...');

    // Clear any OAuth state/cache (implementation specific)
    try {
      await page.request.delete(`${process.env.E2E_IDENTITY_API_URL}/api/v1/oauth/test-cache`, {
        failOnStatusCode: false,
      });
      console.log('✅ Cleared OAuth test cache');
    } catch {
      console.log('ℹ️  OAuth test cache cleanup not available');
    }

    // Generate test summary
    console.log('📊 Authentication test summary:');
    console.log(`   Frontend URL: ${process.env.E2E_BASE_URL}`);
    console.log(`   Identity Service: ${process.env.E2E_IDENTITY_API_URL}`);
    console.log(`   Backend API: ${process.env.E2E_API_BASE_URL}`);
    console.log(`   MailHog: ${process.env.E2E_MAILHOG_API_URL}`);

    // Clear browser storage one final time
    await page.evaluate(() => {
      try {
        localStorage.clear();
        sessionStorage.clear();

        // Clear cookies
        document.cookie.split(";").forEach(c => {
          const eqPos = c.indexOf("=");
          const name = eqPos > -1 ? c.substr(0, eqPos) : c;
          document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/;domain=localhost";
          document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/;domain=127.0.0.1";
          document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/";
        });
      } catch (error) {
        console.warn('Could not clear browser storage:', error);
      }
    });

    console.log('✅ Authentication E2E test environment cleanup complete');

  } catch (error) {
    console.error('❌ Error during authentication test cleanup:');
    console.error(error);
    // Don't throw error to avoid failing the entire test suite
  } finally {
    await context.close();
    await browser.close();
  }
}

export default globalTeardown;