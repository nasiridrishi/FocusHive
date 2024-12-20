/**
 * Test Data Manager
 * Handles creation, management, and cleanup of test data
 */

export interface TestUser {
  id?: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  password: string;
}

export interface TestHive {
  id?: string;
  name: string;
  description: string;
  isPublic: boolean;
  tags: string[];
  ownerId?: string;
}

export class TestDataManager {
  private testUsers: TestUser[] = [];
  private testHives: TestHive[] = [];

  /**
   * Generate a unique test user
   */
  generateTestUser(overrides: Partial<TestUser> = {}): TestUser {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substring(2, 8);
    
    return {
      username: `testuser_${timestamp}_${random}`,
      email: `test.${timestamp}.${random}@focushive.test`,
      firstName: 'Test',
      lastName: 'User',
      password: 'TestPassword123!',
      ...overrides
    };
  }

  /**
   * Generate a unique test hive
   */
  generateTestHive(overrides: Partial<TestHive> = {}): TestHive {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substring(2, 8);
    
    return {
      name: `Test Hive ${timestamp}`,
      description: `A test hive created at ${new Date().toISOString()}`,
      isPublic: true,
      tags: ['test', 'automation'],
      ...overrides
    };
  }

  /**
   * Register a test user (keeps track for cleanup)
   */
  registerTestUser(user: TestUser): TestUser {
    this.testUsers.push(user);
    return user;
  }

  /**
   * Register a test hive (keeps track for cleanup)
   */
  registerTestHive(hive: TestHive): TestHive {
    this.testHives.push(hive);
    return hive;
  }

  /**
   * Get all registered test users
   */
  getTestUsers(): TestUser[] {
    return [...this.testUsers];
  }

  /**
   * Get all registered test hives
   */
  getTestHives(): TestHive[] {
    return [...this.testHives];
  }

  /**
   * Setup test environment
   */
  async setupTestEnvironment(): Promise<void> {
    console.log('Setting up test environment...');
    
    // Create default test users if needed
    const defaultUser = this.generateTestUser({
      username: 'e2e_test_user',
      email: 'e2e.test@focushive.test',
      firstName: 'E2E',
      lastName: 'Test'
    });
    
    this.registerTestUser(defaultUser);
    
    console.log(`Created ${this.testUsers.length} test users`);
  }

  /**
   * Cleanup test environment
   */
  async cleanupTestEnvironment(): Promise<void> {
    console.log('Cleaning up test environment...');
    
    // In a real implementation, you would:
    // 1. Delete test users from the database
    // 2. Delete test hives and related data
    // 3. Clean up any uploaded files
    // 4. Reset any modified system state
    
    console.log(`Cleaned up ${this.testUsers.length} test users and ${this.testHives.length} test hives`);
    
    // Clear the arrays
    this.testUsers = [];
    this.testHives = [];
  }

  /**
   * Generate test data for specific scenarios
   */
  getScenarioData(scenario: string): any {
    switch (scenario) {
      case 'new-user-registration':
        return {
          user: this.generateTestUser(),
          expectedResult: 'success'
        };
        
      case 'existing-user-login':
        return {
          user: this.testUsers[0] || this.generateTestUser(),
          expectedResult: 'success'
        };
        
      case 'invalid-login':
        return {
          user: {
            username: 'nonexistent@focushive.test',
            password: 'WrongPassword123!'
          },
          expectedResult: 'error'
        };
        
      case 'public-hive':
        return {
          hive: this.generateTestHive({
            isPublic: true,
            name: 'Public Test Hive'
          })
        };
        
      case 'private-hive':
        return {
          hive: this.generateTestHive({
            isPublic: false,
            name: 'Private Test Hive'
          })
        };
        
      default:
        throw new Error(`Unknown test scenario: ${scenario}`);
    }
  }
}