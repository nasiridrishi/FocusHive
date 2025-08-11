import { useState, useEffect, useCallback } from 'react';
import { registerSW } from 'virtual:pwa-register';
import type { RegisterSWOptions } from 'virtual:pwa-register';

export interface ServiceWorkerRegistrationState {
  isRegistered: boolean;
  isRegistering: boolean;
  registration: ServiceWorkerRegistration | null;
  error: Error | null;
  needsRefresh: boolean;
  offlineReady: boolean;
}

export interface UseServiceWorkerRegistrationOptions extends RegisterSWOptions {
  // Additional options specific to our hook
}

export interface UseServiceWorkerRegistrationReturn extends ServiceWorkerRegistrationState {
  updateServiceWorker: (reloadPage?: boolean) => Promise<void>;
}

/**
 * Custom hook for managing Service Worker registration with PWA capabilities
 * Provides state management for service worker lifecycle and update handling
 */
export const useServiceWorkerRegistration = (
  options: UseServiceWorkerRegistrationOptions = {}
): UseServiceWorkerRegistrationReturn => {
  const [state, setState] = useState<ServiceWorkerRegistrationState>({
    isRegistered: false,
    isRegistering: false,
    registration: null,
    error: null,
    needsRefresh: false,
    offlineReady: false,
  });

  const [updateSW, setUpdateSW] = useState<((reloadPage?: boolean) => Promise<void>) | null>(null);

  const updateServiceWorker = useCallback(async (reloadPage?: boolean) => {
    if (updateSW) {
      try {
        await updateSW(reloadPage);
      } catch (error) {
        setState(prev => ({ ...prev, error: error as Error }));
      }
    }
  }, [updateSW]);

  useEffect(() => {
    // Skip registration if service worker is not supported
    if (!('serviceWorker' in navigator)) {
      setState(prev => ({ 
        ...prev, 
        error: new Error('Service Worker not supported') 
      }));
      return;
    }

    setState(prev => ({ ...prev, isRegistering: true }));

    try {
      const updateSWFunction = registerSW({
        immediate: options.immediate ?? true,
        onRegistered: (registration) => {
          setState(prev => ({
            ...prev,
            isRegistered: true,
            isRegistering: false,
            registration: registration || null,
            error: null,
          }));
          options.onRegistered?.(registration);
        },
        onRegisteredSW: (swUrl, registration) => {
          setState(prev => ({
            ...prev,
            isRegistered: true,
            isRegistering: false,
            registration: registration || null,
            error: null,
          }));
          options.onRegisteredSW?.(swUrl, registration);
        },
        onNeedRefresh: () => {
          setState(prev => ({
            ...prev,
            needsRefresh: true,
          }));
          options.onNeedRefresh?.();
        },
        onOfflineReady: () => {
          setState(prev => ({
            ...prev,
            offlineReady: true,
          }));
          options.onOfflineReady?.();
        },
        onRegisterError: (error) => {
          setState(prev => ({
            ...prev,
            isRegistered: false,
            isRegistering: false,
            error: error instanceof Error ? error : new Error(String(error)),
          }));
          options.onRegisterError?.(error);
        },
      });

      setUpdateSW(() => updateSWFunction);
    } catch (error) {
      setState(prev => ({
        ...prev,
        isRegistered: false,
        isRegistering: false,
        error: error instanceof Error ? error : new Error(String(error)),
      }));
    }
  }, [options]);

  return {
    ...state,
    updateServiceWorker,
  };
};