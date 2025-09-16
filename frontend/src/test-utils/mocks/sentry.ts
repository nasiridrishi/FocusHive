// Mocks for @sentry/react and error monitoring services
import { vi } from 'vitest';

const mockScope = {
  setTag: vi.fn(),
  setTags: vi.fn(),
  setExtra: vi.fn(),
  setExtras: vi.fn(),
  setUser: vi.fn(),
  setFingerprint: vi.fn(),
  setLevel: vi.fn(),
  clearBreadcrumbs: vi.fn(),
};

export const mockSentry = {
  captureException: vi.fn(),
  captureMessage: vi.fn(),
  captureEvent: vi.fn(),
  configureScope: vi.fn((callback) => callback(mockScope)),
  withScope: vi.fn((callback) => callback(mockScope)),
  setTag: vi.fn(),
  setTags: vi.fn(),
  setExtra: vi.fn(),
  setExtras: vi.fn(),
  setUser: vi.fn(),
  init: vi.fn(),
  flush: vi.fn(() => Promise.resolve(true)),
  close: vi.fn(() => Promise.resolve(true)),
  lastEventId: vi.fn(() => ''),
  ErrorBoundary: ({ children }: { children: React.ReactNode }) => children,
  Profiler: ({ children }: { children: React.ReactNode }) => children,
  withProfiler: (Component: React.ComponentType) => Component,
};

// Reset all mocked functions between tests
export const resetSentryMocks = () => {
  Object.values(mockSentry).forEach(mock => {
    if (typeof mock === 'function' && 'mockReset' in mock) {
      mock.mockReset();
    }
  });
  
  Object.values(mockScope).forEach(mock => {
    if (typeof mock === 'function' && 'mockReset' in mock) {
      mock.mockReset();
    }
  });
};

export default mockSentry;