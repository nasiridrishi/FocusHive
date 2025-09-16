import type {User} from '@shared/types/auth';
import {QueryClient} from '@tanstack/react-query';

// Mock user for testing
export const mockUser: User = {
  id: '1',
  username: 'testuser',
  email: 'test@example.com',
  firstName: 'Test',
  lastName: 'User',
  name: 'Test User',
  avatar: undefined,
  isEmailVerified: true,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-15T00:00:00Z'
};

// Create a test query client with disabled retries and logging
export const createTestQueryClient = () => {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
        staleTime: 0,
      },
      mutations: {
        retry: false,
      },
    },
  });
};

// Common test utilities
export const waitForAsync = (ms: number = 0) =>
    new Promise(resolve => setTimeout(resolve, ms));

export const mockConsoleError = () => {
  const originalError = console.error;
  beforeAll(() => {
    console.error = jest.fn();
  });
  afterAll(() => {
    console.error = originalError;
  });
  return console.error as jest.Mock;
};

export const mockConsoleWarn = () => {
  const originalWarn = console.warn;
  beforeAll(() => {
    console.warn = jest.fn();
  });
  afterAll(() => {
    console.warn = originalWarn;
  });
  return console.warn as jest.Mock;
};