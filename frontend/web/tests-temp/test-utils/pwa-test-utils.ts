/**
 * PWA Testing Utilities
 * Provides mocks and helpers for testing Progressive Web App features
 */

// Mock Service Worker registration
export const mockServiceWorkerRegistration = {
  installing: null,
  waiting: null,
  active: null,
  scope: '/',
  update: vi.fn().mockResolvedValue(undefined),
  unregister: vi.fn().mockResolvedValue(true),
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
};

// Mock Service Worker
export const mockServiceWorker = {
  scriptURL: '/sw.js',
  state: 'activated' as ServiceWorkerState,
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
  postMessage: vi.fn(),
};

// Mock Navigator with Service Worker support
export const createMockNavigator = (options: {
  serviceWorkerSupported?: boolean;
  online?: boolean;
} = {}) => {
  const { serviceWorkerSupported = true, online = true } = options;

  const mockNavigator = {
    serviceWorker: serviceWorkerSupported ? {
      register: vi.fn().mockResolvedValue(mockServiceWorkerRegistration),
      ready: Promise.resolve(mockServiceWorkerRegistration),
      controller: mockServiceWorker,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      getRegistration: vi.fn().mockResolvedValue(mockServiceWorkerRegistration),
      getRegistrations: vi.fn().mockResolvedValue([mockServiceWorkerRegistration]),
    } : undefined,
    onLine: online,
    connection: {
      effectiveType: '4g',
      downlink: 10,
    },
  };

  return mockNavigator;
};

// Mock beforeinstallprompt event
export const createMockBeforeInstallPromptEvent = () => {
  const mockEvent = {
    preventDefault: vi.fn(),
    prompt: vi.fn().mockResolvedValue({ outcome: 'accepted' }),
    userChoice: Promise.resolve({ outcome: 'accepted' as const }),
  };

  return mockEvent;
};

// Mock Push Subscription
export const mockPushSubscription = {
  endpoint: 'https://example.com/push/endpoint',
  keys: {
    p256dh: 'mock-p256dh-key',
    auth: 'mock-auth-key',
  },
  getKey: vi.fn().mockReturnValue(new ArrayBuffer(0)),
  toJSON: vi.fn().mockReturnValue({
    endpoint: 'https://example.com/push/endpoint',
    keys: {
      p256dh: 'mock-p256dh-key',
      auth: 'mock-auth-key',
    },
  }),
  unsubscribe: vi.fn().mockResolvedValue(true),
};

// Mock Push Manager
export const mockPushManager = {
  subscribe: vi.fn().mockResolvedValue(mockPushSubscription),
  getSubscription: vi.fn().mockResolvedValue(mockPushSubscription),
  permissionState: vi.fn().mockResolvedValue('granted' as PushPermissionState),
};

// Setup PWA test environment
export const setupPWATestEnvironment = () => {
  const mockNavigator = createMockNavigator();
  
  // Mock global navigator
  Object.defineProperty(window, 'navigator', {
    value: mockNavigator,
    writable: true,
  });

  // Mock window.location
  Object.defineProperty(window, 'location', {
    value: {
      protocol: 'https:',
      hostname: 'localhost',
      port: '5173',
      origin: 'https://localhost:5173',
    },
    writable: true,
  });

  // Mock caches API
  const mockCache = {
    put: vi.fn().mockResolvedValue(undefined),
    add: vi.fn().mockResolvedValue(undefined),
    addAll: vi.fn().mockResolvedValue(undefined),
    match: vi.fn().mockResolvedValue(undefined),
    matchAll: vi.fn().mockResolvedValue([]),
    delete: vi.fn().mockResolvedValue(true),
    keys: vi.fn().mockResolvedValue([]),
  };

  Object.defineProperty(window, 'caches', {
    value: {
      open: vi.fn().mockResolvedValue(mockCache),
      has: vi.fn().mockResolvedValue(true),
      delete: vi.fn().mockResolvedValue(true),
      keys: vi.fn().mockResolvedValue(['cache-v1']),
      match: vi.fn().mockResolvedValue(undefined),
    },
    writable: true,
  });

  // Mock Notification API
  Object.defineProperty(window, 'Notification', {
    value: {
      permission: 'default',
      requestPermission: vi.fn().mockResolvedValue('granted'),
    },
    writable: true,
  });

  return {
    mockNavigator,
    mockCache,
    mockServiceWorkerRegistration,
    mockServiceWorker,
  };
};

// Cleanup PWA test environment
export const cleanupPWATestEnvironment = () => {
  vi.clearAllMocks();
};

// Test utilities for PWA state assertions
export const waitForServiceWorkerReady = async (timeout = 5000): Promise<void> => {
  return new Promise((resolve) => {
    setTimeout(resolve, Math.min(timeout, 100)); // Simulate async SW registration
  });
};

export const triggerServiceWorkerUpdate = () => {
  const event = new CustomEvent('controllerchange');
  window.navigator.serviceWorker?.dispatchEvent?.(event);
};

export const simulateOfflineMode = () => {
  Object.defineProperty(window.navigator, 'onLine', { value: false, writable: true });
  window.dispatchEvent(new Event('offline'));
};

export const simulateOnlineMode = () => {
  Object.defineProperty(window.navigator, 'onLine', { value: true, writable: true });
  window.dispatchEvent(new Event('online'));
};