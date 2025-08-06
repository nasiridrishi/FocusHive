/**
 * @jest-environment jsdom
 */
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useServiceWorkerRegistration } from './useServiceWorkerRegistration';
import { setupPWATestEnvironment, cleanupPWATestEnvironment, waitForServiceWorkerReady } from '../../../test-utils/pwa-test-utils';

// Mock the virtual PWA register module - this will fail initially since it doesn't exist
vi.mock('virtual:pwa-register', () => ({
  registerSW: vi.fn(),
}));

describe('useServiceWorkerRegistration', () => {
  beforeEach(() => {
    setupPWATestEnvironment();
  });

  afterEach(() => {
    cleanupPWATestEnvironment();
  });

  it('should initialize with default state', () => {
    const { result } = renderHook(() => useServiceWorkerRegistration());

    expect(result.current.isRegistered).toBe(false);
    expect(result.current.isRegistering).toBe(false);
    expect(result.current.registration).toBe(null);
    expect(result.current.error).toBe(null);
    expect(result.current.needsRefresh).toBe(false);
    expect(result.current.offlineReady).toBe(false);
  });

  it('should register service worker on mount when enabled', async () => {
    const mockRegisterSW = vi.fn().mockResolvedValue(() => Promise.resolve());
    vi.mocked(require('virtual:pwa-register').registerSW).mockImplementation(mockRegisterSW);

    const { result } = renderHook(() => useServiceWorkerRegistration({ 
      immediate: true 
    }));

    expect(result.current.isRegistering).toBe(true);
    expect(mockRegisterSW).toHaveBeenCalledWith({
      immediate: true,
      onRegistered: expect.any(Function),
      onNeedRefresh: expect.any(Function),
      onOfflineReady: expect.any(Function),
      onRegisterError: expect.any(Function),
    });
  });

  it('should handle successful service worker registration', async () => {
    const mockRegistration = {
      scope: '/',
      active: { scriptURL: '/sw.js' },
      update: vi.fn().mockResolvedValue(undefined),
    } as unknown as ServiceWorkerRegistration;

    const mockRegisterSW = vi.fn().mockImplementation((options) => {
      // Simulate successful registration
      setTimeout(() => {
        options.onRegistered?.(mockRegistration);
      }, 100);
      return () => Promise.resolve();
    });

    vi.mocked(require('virtual:pwa-register').registerSW).mockImplementation(mockRegisterSW);

    const { result } = renderHook(() => useServiceWorkerRegistration({ 
      immediate: true 
    }));

    // Wait for registration to complete
    await act(async () => {
      await waitForServiceWorkerReady();
    });

    expect(result.current.isRegistered).toBe(true);
    expect(result.current.isRegistering).toBe(false);
    expect(result.current.registration).toBe(mockRegistration);
    expect(result.current.error).toBe(null);
  });

  it('should handle service worker registration error', async () => {
    const mockError = new Error('Service worker registration failed');
    const mockRegisterSW = vi.fn().mockImplementation((options) => {
      // Simulate registration error
      setTimeout(() => {
        options.onRegisterError?.(mockError);
      }, 100);
      return () => Promise.resolve();
    });

    vi.mocked(require('virtual:pwa-register').registerSW).mockImplementation(mockRegisterSW);

    const { result } = renderHook(() => useServiceWorkerRegistration({ 
      immediate: true 
    }));

    // Wait for registration to fail
    await act(async () => {
      await waitForServiceWorkerReady();
    });

    expect(result.current.isRegistered).toBe(false);
    expect(result.current.isRegistering).toBe(false);
    expect(result.current.registration).toBe(null);
    expect(result.current.error).toBe(mockError);
  });

  it('should handle needsRefresh callback', async () => {
    const mockRegisterSW = vi.fn().mockImplementation((options) => {
      // Simulate need refresh event
      setTimeout(() => {
        options.onNeedRefresh?.();
      }, 100);
      return () => Promise.resolve();
    });

    vi.mocked(require('virtual:pwa-register').registerSW).mockImplementation(mockRegisterSW);

    const { result } = renderHook(() => useServiceWorkerRegistration({ 
      immediate: true 
    }));

    // Wait for needsRefresh event
    await act(async () => {
      await waitForServiceWorkerReady();
    });

    expect(result.current.needsRefresh).toBe(true);
  });

  it('should handle offlineReady callback', async () => {
    const mockRegisterSW = vi.fn().mockImplementation((options) => {
      // Simulate offline ready event
      setTimeout(() => {
        options.onOfflineReady?.();
      }, 100);
      return () => Promise.resolve();
    });

    vi.mocked(require('virtual:pwa-register').registerSW).mockImplementation(mockRegisterSW);

    const { result } = renderHook(() => useServiceWorkerRegistration({ 
      immediate: true 
    }));

    // Wait for offlineReady event
    await act(async () => {
      await waitForServiceWorkerReady();
    });

    expect(result.current.offlineReady).toBe(true);
  });

  it('should provide updateServiceWorker function', () => {
    const { result } = renderHook(() => useServiceWorkerRegistration());

    expect(typeof result.current.updateServiceWorker).toBe('function');
  });

  it('should call updateServiceWorker when requested', async () => {
    const mockUpdateSW = vi.fn().mockResolvedValue(undefined);
    const mockRegisterSW = vi.fn().mockReturnValue(mockUpdateSW);

    vi.mocked(require('virtual:pwa-register').registerSW).mockImplementation(mockRegisterSW);

    const { result } = renderHook(() => useServiceWorkerRegistration());

    await act(async () => {
      await result.current.updateServiceWorker();
    });

    expect(mockUpdateSW).toHaveBeenCalled();
  });

  it('should not register service worker when immediate is false', () => {
    const mockRegisterSW = vi.fn();
    vi.mocked(require('virtual:pwa-register').registerSW).mockImplementation(mockRegisterSW);

    renderHook(() => useServiceWorkerRegistration({ 
      immediate: false 
    }));

    expect(mockRegisterSW).toHaveBeenCalledWith({
      immediate: false,
      onRegistered: expect.any(Function),
      onNeedRefresh: expect.any(Function),
      onOfflineReady: expect.any(Function),
      onRegisterError: expect.any(Function),
    });
  });

  it('should handle service worker updates', async () => {
    const mockUpdateSW = vi.fn().mockResolvedValue(undefined);
    const mockRegistration = {
      scope: '/',
      active: { scriptURL: '/sw.js' },
      update: vi.fn().mockResolvedValue(undefined),
    } as unknown as ServiceWorkerRegistration;

    const mockRegisterSW = vi.fn().mockImplementation((options) => {
      setTimeout(() => {
        options.onRegistered?.(mockRegistration);
      }, 100);
      return mockUpdateSW;
    });

    vi.mocked(require('virtual:pwa-register').registerSW).mockImplementation(mockRegisterSW);

    const { result } = renderHook(() => useServiceWorkerRegistration({ 
      immediate: true 
    }));

    // Wait for registration
    await act(async () => {
      await waitForServiceWorkerReady();
    });

    // Test update functionality
    await act(async () => {
      await result.current.updateServiceWorker(true);
    });

    expect(mockUpdateSW).toHaveBeenCalledWith(true);
  });
});