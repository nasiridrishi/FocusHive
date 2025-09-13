/**
 * Global Teardown for Hive Workflow E2E Tests
 * Handles cleanup of test data and environment restoration
 */

import { chromium, FullConfig, Page } from '@playwright/test';
import fs from 'fs/promises';
import path from 'path';

// Test result interfaces
interface TestSpec {
  ok: boolean;
  title: string;
}

interface TestSuite {
  title: string;
  specs?: TestSpec[];
}

interface TestResults {
  stats?: {
    total: number;
    passed: number;
    failed: number;
    skipped: number;
    duration: number;
  };
  suites?: TestSuite[];
}

async function globalTeardown(config: FullConfig) {
  console.log('🧹 Starting Hive Workflow E2E Test Cleanup...');
  
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();
  
  try {
    // Step 1: Clean up test data
    console.log('🗑️  Cleaning up test data...');
    await cleanupTestData(page);
    
    // Step 2: Clean up authentication files
    console.log('🔐 Cleaning up authentication files...');
    await cleanupAuthFiles();
    
    // Step 3: Generate test report summary
    console.log('📊 Generating test summary...');
    await generateTestSummary();
    
    console.log('✅ Global teardown completed successfully!');
    
  } catch (error) {
    console.error('❌ Global teardown failed:', error);
    // Don't throw error in teardown to avoid masking test failures
  } finally {
    await context.close();
    await browser.close();
  }
}

/**
 * Clean up test data from the backend
 */
async function cleanupTestData(page: Page): Promise<void> {
  const apiBaseUrl = process.env.E2E_API_BASE_URL || 'http://localhost:8080';
  
  try {
    // Clean up test hives, sessions, and analytics data
    const cleanupOperations = [
      { type: 'hives', pattern: 'Test|E2E|temp|staging|PRESENCE_TEST|ANALYTICS_TEST' },
      { type: 'sessions', pattern: 'test_session|e2e_session' },
      { type: 'analytics', pattern: 'test_user|e2e_user' }
    ];
    
    for (const operation of cleanupOperations) {
      try {
        const response = await page.request.post(`${apiBaseUrl}/api/test/cleanup`, {
          data: operation,
          headers: {
            'Content-Type': 'application/json'
          }
        });
        
        if (response.ok()) {
          const result = await response.json();
          console.log(`✅ Cleaned up ${operation.type}: ${result.deletedCount || 0} items`);
        } else {
          console.log(`⚠️  Cleanup endpoint for ${operation.type} returned ${response.status()}`);
        }
        
      } catch (error) {
        console.log(`⚠️  Could not cleanup ${operation.type}:`, error.message);
      }
    }
    
  } catch (error) {
    console.log('⚠️  Test data cleanup not available:', error.message);
  }
}

/**
 * Clean up authentication state files
 */
async function cleanupAuthFiles(): Promise<void> {
  const authFiles = [
    'auth-owner.json',
    'auth-member_1.json', 
    'auth-member_2.json',
    'auth-moderator.json',
    'auth-non_member.json'
  ];
  
  for (const authFile of authFiles) {
    try {
      await fs.unlink(authFile);
      console.log(`✅ Removed auth file: ${authFile}`);
    } catch (error) {
      // File might not exist, which is fine
      if (error.code !== 'ENOENT') {
        console.log(`⚠️  Could not remove ${authFile}:`, error.message);
      }
    }
  }
}

/**
 * Generate a summary of test results
 */
async function generateTestSummary(): Promise<void> {
  try {
    // Read test results if available
    const resultsFile = 'hive-workflow-results.json';
    
    try {
      const resultsData = await fs.readFile(resultsFile, 'utf-8');
      const results: TestResults = JSON.parse(resultsData);
      
      const summary = {
        timestamp: new Date().toISOString(),
        totalTests: results.stats?.total || 0,
        passed: results.stats?.passed || 0,
        failed: results.stats?.failed || 0,
        skipped: results.stats?.skipped || 0,
        duration: results.stats?.duration || 0,
        suites: results.suites?.map((suite: TestSuite) => ({
          title: suite.title,
          tests: suite.specs?.length || 0,
          passed: suite.specs?.filter((spec: TestSpec) => spec.ok).length || 0,
          failed: suite.specs?.filter((spec: TestSpec) => !spec.ok).length || 0
        })) || []
      };
      
      // Write summary to file
      await fs.writeFile(
        'hive-workflow-summary.json', 
        JSON.stringify(summary, null, 2)
      );
      
      // Log summary to console
      console.log('\n📊 Test Execution Summary:');
      console.log(`Total Tests: ${summary.totalTests}`);
      console.log(`✅ Passed: ${summary.passed}`);
      console.log(`❌ Failed: ${summary.failed}`);
      console.log(`⏭️  Skipped: ${summary.skipped}`);
      console.log(`⏱️  Duration: ${Math.round(summary.duration / 1000)}s`);
      
      if (summary.failed > 0) {
        console.log('\n❌ Failed Test Suites:');
        summary.suites
          .filter(suite => suite.failed > 0)
          .forEach(suite => {
            console.log(`  - ${suite.title}: ${suite.failed}/${suite.tests} failed`);
          });
      }
      
    } catch (error) {
      console.log('⚠️  Could not read test results for summary');
    }
    
  } catch (error) {
    console.log('⚠️  Could not generate test summary:', error.message);
  }
}

export default globalTeardown;