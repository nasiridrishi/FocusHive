import { renderHook } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';

// Mock the virtual:pwa-register module
const mockUpdateSW = vi.fn().mockResolvedValue(undefined);
const mockRegisterSW = vi.fn().mockReturnValue(mockUpdateSW);

vi.mock('virtual:pwa-register', () => ({
  registerSW: mockRegisterSW
}));

describe('useServiceWorkerRegistration', () => {
  beforeEach(() => {
    // Mock navigator.serviceWorker
    Object.defineProperty(global.navigator, 'serviceWorker', {
      value: {
        register: vi.fn(),
        ready: Promise.resolve({
          unregister: vi.fn().mockResolvedValue(true),
        }),
      },
      configurable: true,
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should import without errors', async () => {
    const { useServiceWorkerRegistration } = await import('../useServiceWorkerRegistration');
    expect(useServiceWorkerRegistration).toBeDefined();
  });

  it('should handle browsers that support service workers', async () => {
    const { useServiceWorkerRegistration } = await import('../useServiceWorkerRegistration');
    
    const { result } = renderHook(() => useServiceWorkerRegistration());

    expect(result.current.isRegistering).toBe(true);
    expect(result.current.error).toBe(null);
    expect(mockRegisterSW).toHaveBeenCalled();
  });

  it('should handle browsers that do not support service workers', async () => {
    // Mock navigator without serviceWorker support
    const originalServiceWorker = global.navigator.serviceWorker;
    // @ts-expect-error - Intentionally testing without serviceWorker
    delete global.navigator.serviceWorker;

    const { useServiceWorkerRegistration } = await import('../useServiceWorkerRegistration');
    const { result } = renderHook(() => useServiceWorkerRegistration());

    expect(result.current.isRegistering).toBe(false);
    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.error?.message).toBe('Service Worker not supported');

    // Restore navigator
    global.navigator.serviceWorker = originalServiceWorker;
  });

  it('should have correct initial state', async () => {
    const { useServiceWorkerRegistration } = await import('../useServiceWorkerRegistration');
    const { result } = renderHook(() => useServiceWorkerRegistration());

    expect(result.current.isRegistered).toBe(false);
    expect(result.current.registration).toBe(null);
    expect(result.current.needsRefresh).toBe(false);
    expect(result.current.offlineReady).toBe(false);
    expect(typeof result.current.updateServiceWorker).toBe('function');
  });

  it('should provide updateServiceWorker function', async () => {
    const { useServiceWorkerRegistration } = await import('../useServiceWorkerRegistration');
    const { result } = renderHook(() => useServiceWorkerRegistration());

    await result.current.updateServiceWorker();
    
    expect(mockUpdateSW).toHaveBeenCalled();
  });
});