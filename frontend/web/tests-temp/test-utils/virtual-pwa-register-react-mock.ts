/**
 * Mock implementation of virtual:pwa-register/react for testing
 */
import { vi } from 'vitest';
import { useState } from 'react';

export interface RegisterSWOptions {
  immediate?: boolean;
  onNeedRefresh?: () => void;
  onOfflineReady?: () => void;
  onRegistered?: (registration: ServiceWorkerRegistration | undefined) => void;
  onRegisteredSW?: (swUrl: string, registration: ServiceWorkerRegistration | undefined) => void;
  onRegisterError?: (error: Error) => void;
}

export const useRegisterSW = vi.fn((options?: RegisterSWOptions) => {
  const [needRefresh, setNeedRefresh] = useState(false);
  const [offlineReady, setOfflineReady] = useState(false);
  
  const updateServiceWorker = vi.fn().mockResolvedValue(undefined);

  // Simulate registering if options provided
  if (options?.onRegistered) {
    setTimeout(() => options.onRegistered?.(new ServiceWorkerRegistration()), 100);
  }

  return {
    needRefresh: [needRefresh, setNeedRefresh],
    offlineReady: [offlineReady, setOfflineReady],
    updateServiceWorker,
  };
});