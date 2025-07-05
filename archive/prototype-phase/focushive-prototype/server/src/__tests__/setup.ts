// Test setup file
import { dataStore } from '../data/store';
import { presenceService } from '../services/presenceService';

// Set test environment variables
process.env.JWT_SECRET = 'test-secret-key';
process.env.NODE_ENV = 'test';

// Clear data before each test
beforeEach(() => {
  dataStore.clear();
  presenceService.clear();
});

// Suppress console logs during tests
global.console = {
  ...console,
  log: jest.fn(),
  error: jest.fn(),
  warn: jest.fn(),
};