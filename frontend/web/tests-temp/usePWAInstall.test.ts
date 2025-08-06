/**
 * @jest-environment jsdom
 */
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { usePWAInstall } from './usePWAInstall';
import { 
  setupPWATestEnvironment, 
  cleanupPWATestEnvironment, 
  createMockBeforeInstallPromptEvent 
} from '../../../test-utils/pwa-test-utils';

describe('usePWAInstall', () => {
  beforeEach(() => {
    setupPWATestEnvironment();
  });

  afterEach(() => {
    cleanupPWATestEnvironment();
  });

  it('should initialize with default state', () => {
    const { result } = renderHook(() => usePWAInstall());

    expect(result.current.isInstallable).toBe(false);
    expect(result.current.isInstalled).toBe(false);
    expect(result.current.isStandalone).toBe(false);
    expect(result.current.installPrompt).toBe(null);
    expect(result.current.isInstalling).toBe(false);
    expect(typeof result.current.promptInstall).toBe('function');
    expect(typeof result.current.cancelInstall).toBe('function');
  });

  it('should detect standalone mode', () => {
    // Mock standalone mode
    Object.defineProperty(window.navigator, 'standalone', { 
      value: true, 
      writable: true 
    });
    Object.defineProperty(window, 'matchMedia', {
      value: vi.fn().mockReturnValue({ matches: true }),
      writable: true
    });

    const { result } = renderHook(() => usePWAInstall());

    expect(result.current.isStandalone).toBe(true);
    expect(result.current.isInstalled).toBe(true);
  });

  it('should handle beforeinstallprompt event', async () => {
    const { result } = renderHook(() => usePWAInstall());

    const mockEvent = createMockBeforeInstallPromptEvent();

    // Simulate beforeinstallprompt event
    await act(async () => {
      window.dispatchEvent(Object.assign(new Event('beforeinstallprompt'), mockEvent));
    });

    expect(result.current.isInstallable).toBe(true);
    expect(result.current.installPrompt).toBeTruthy();
  });

  it('should handle install prompt and accept', async () => {
    const { result } = renderHook(() => usePWAInstall());

    const mockEvent = createMockBeforeInstallPromptEvent();
    mockEvent.userChoice = Promise.resolve({ outcome: 'accepted' });

    // Simulate beforeinstallprompt event
    await act(async () => {
      window.dispatchEvent(Object.assign(new Event('beforeinstallprompt'), mockEvent));
    });

    expect(result.current.isInstallable).toBe(true);

    // Trigger install
    await act(async () => {
      await result.current.promptInstall();
    });

    expect(mockEvent.preventDefault).toHaveBeenCalled();
    expect(mockEvent.prompt).toHaveBeenCalled();
    expect(result.current.isInstalling).toBe(false);
    expect(result.current.isInstallable).toBe(false);
  });

  it('should handle install prompt and dismiss', async () => {
    const { result } = renderHook(() => usePWAInstall());

    const mockEvent = createMockBeforeInstallPromptEvent();
    mockEvent.userChoice = Promise.resolve({ outcome: 'dismissed' });

    // Simulate beforeinstallprompt event
    await act(async () => {
      window.dispatchEvent(Object.assign(new Event('beforeinstallprompt'), mockEvent));
    });

    expect(result.current.isInstallable).toBe(true);

    // Trigger install
    await act(async () => {
      await result.current.promptInstall();
    });

    expect(mockEvent.preventDefault).toHaveBeenCalled();
    expect(mockEvent.prompt).toHaveBeenCalled();
    expect(result.current.isInstalling).toBe(false);
    expect(result.current.isInstallable).toBe(false); // Should still be false after dismiss
  });

  it('should handle appinstalled event', async () => {
    const { result } = renderHook(() => usePWAInstall());

    // Simulate app installed event
    await act(async () => {
      window.dispatchEvent(new Event('appinstalled'));
    });

    expect(result.current.isInstalled).toBe(true);
    expect(result.current.isInstallable).toBe(false);
  });

  it('should cancel install prompt', async () => {
    const { result } = renderHook(() => usePWAInstall());

    const mockEvent = createMockBeforeInstallPromptEvent();

    // Simulate beforeinstallprompt event
    await act(async () => {
      window.dispatchEvent(Object.assign(new Event('beforeinstallprompt'), mockEvent));
    });

    expect(result.current.isInstallable).toBe(true);

    // Cancel install
    act(() => {
      result.current.cancelInstall();
    });

    expect(result.current.isInstallable).toBe(false);
    expect(result.current.installPrompt).toBe(null);
  });

  it('should not be installable when no prompt event', () => {
    const { result } = renderHook(() => usePWAInstall());

    expect(result.current.isInstallable).toBe(false);
    expect(result.current.installPrompt).toBe(null);
  });

  it('should detect iOS Safari standalone mode', () => {
    // Mock iOS Safari standalone mode
    Object.defineProperty(window.navigator, 'standalone', { 
      value: true, 
      writable: true 
    });

    const { result } = renderHook(() => usePWAInstall());

    expect(result.current.isStandalone).toBe(true);
    expect(result.current.isInstalled).toBe(true);
  });

  it('should detect display-mode standalone', () => {
    // Mock display-mode: standalone media query
    Object.defineProperty(window, 'matchMedia', {
      value: vi.fn().mockImplementation((query: string) => ({
        matches: query === '(display-mode: standalone)',
        media: query,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
      })),
      writable: true
    });

    const { result } = renderHook(() => usePWAInstall());

    expect(result.current.isStandalone).toBe(true);
    expect(result.current.isInstalled).toBe(true);
  });

  it('should handle install error', async () => {
    const { result } = renderHook(() => usePWAInstall());

    const mockEvent = createMockBeforeInstallPromptEvent();
    mockEvent.prompt = vi.fn().mockRejectedValue(new Error('Install failed'));

    // Simulate beforeinstallprompt event
    await act(async () => {
      window.dispatchEvent(Object.assign(new Event('beforeinstallprompt'), mockEvent));
    });

    // Try to install - should handle error gracefully
    await act(async () => {
      await result.current.promptInstall();
    });

    expect(result.current.isInstalling).toBe(false);
    expect(result.current.isInstallable).toBe(false);
  });

  it('should not prompt install when not installable', async () => {
    const { result } = renderHook(() => usePWAInstall());

    // Try to prompt install without installPrompt event
    await act(async () => {
      await result.current.promptInstall();
    });

    // Should do nothing when not installable
    expect(result.current.isInstalling).toBe(false);
    expect(result.current.isInstallable).toBe(false);
  });
});