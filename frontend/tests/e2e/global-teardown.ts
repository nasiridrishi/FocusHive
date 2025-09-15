import { FullConfig } from '@playwright/test';
import { TestDataManager } from './helpers/test-data-manager';

/**
 * Global teardown runs after all tests complete
 * Cleans up test data, closes connections, etc.
 */
async function globalTeardown(config: FullConfig) {
  console.log('🧹 Starting E2E test cleanup...');
  
  const testDataManager = new TestDataManager();
  
  try {
    // Cleanup test data
    await testDataManager.cleanupTestEnvironment();
    
    console.log('✅ Test data cleanup complete');
    console.log('🎉 Global teardown completed successfully');
  } catch (error) {
    console.error('❌ Global teardown failed:', error);
    // Don't throw here, let tests complete
  }
}

export default globalTeardown;