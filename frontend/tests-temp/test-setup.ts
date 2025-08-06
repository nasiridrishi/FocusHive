import '@testing-library/jest-dom';
import { vi } from 'vitest';
import { setupPWATestEnvironment, cleanupPWATestEnvironment } from './test-utils/pwa-test-utils';

// Make vi available globally for test utilities
global.vi = vi;

// Set up PWA test environment before each test
beforeEach(() => {
  setupPWATestEnvironment();
});

// Clean up after each test
afterEach(() => {
  cleanupPWATestEnvironment();
});