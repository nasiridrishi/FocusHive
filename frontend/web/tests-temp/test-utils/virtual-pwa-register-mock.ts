/**
 * Mock implementation of virtual:pwa-register for testing
 */
import { vi } from 'vitest';

export interface RegisterSWOptions {
  immediate?: boolean;
  onNeedRefresh?: () => void;
  onOfflineReady?: () => void;
  onRegistered?: (registration: ServiceWorkerRegistration | undefined) => void;
  onRegisteredSW?: (swUrl: string, registration: ServiceWorkerRegistration | undefined) => void;
  onRegisterError?: (error: any) => void;
}

export const registerSW = vi.fn((options?: RegisterSWOptions) => {
  // Return a mock update function
  return vi.fn().mockResolvedValue(undefined);
});