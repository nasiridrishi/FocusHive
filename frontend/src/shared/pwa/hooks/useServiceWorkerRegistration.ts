import { useState, useEffect, useCallback, useMemo } from 'react';
// Temporarily disabled PWA imports due to build issues
// import { registerSW } from 'virtual:pwa-register';
// import type { RegisterSWOptions } from 'virtual:pwa-register';

// Stub for RegisterSWOptions
interface RegisterSWOptions {
  immediate?: boolean;
  onNeedRefresh?: () => void;
  onOfflineReady?: () => void;
  onRegistered?: (registration: ServiceWorkerRegistration | undefined) => void;
  onRegisterError?: (error: unknown) => void;
}

// Stub registerSW function
const registerSW = (_options?: RegisterSWOptions) => {
  console.warn('PWA registration is currently disabled');
  return () => Promise.resolve();
}

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

  // Memoize callback functions to prevent infinite re-renders
  const onRegisteredCallback = useCallback((registration?: ServiceWorkerRegistration) => {
    setState(prev => ({
      ...prev,
      isRegistered: true,
      isRegistering: false,
      registration: registration || null,
      error: null,
    }));
    options.onRegistered?.(registration);
  }, [options.onRegistered]);

  const onRegisteredSWCallback = useCallback((swUrl: string, registration?: ServiceWorkerRegistration) => {
    setState(prev => ({
      ...prev,
      isRegistered: true,
      isRegistering: false,
      registration: registration || null,
      error: null,
    }));
    options.onRegistered?.(registration);
  }, [options.onRegistered]);

  const onNeedRefreshCallback = useCallback(() => {
    setState(prev => ({
      ...prev,
      needsRefresh: true,
    }));
    options.onNeedRefresh?.();
  }, [options.onNeedRefresh]);

  const onOfflineReadyCallback = useCallback(() => {
    setState(prev => ({
      ...prev,
      offlineReady: true,
    }));
    options.onOfflineReady?.();
  }, [options.onOfflineReady]);

  const onRegisterErrorCallback = useCallback((error: unknown) => {
    setState(prev => ({
      ...prev,
      isRegistered: false,
      isRegistering: false,
      error: error instanceof Error ? error : new Error(String(error)),
    }));
    options.onRegisterError?.(error);
  }, [options.onRegisterError]);

  // Memoize the register options to prevent infinite re-renders
  const registerOptions = useMemo(() => ({
    immediate: options.immediate ?? true,
    onRegistered: onRegisteredCallback,
    onRegisteredSW: onRegisteredSWCallback,
    onNeedRefresh: onNeedRefreshCallback,
    onOfflineReady: onOfflineReadyCallback,
    onRegisterError: onRegisterErrorCallback,
  }), [
    options.immediate,
    onRegisteredCallback,
    onRegisteredSWCallback,
    onNeedRefreshCallback,
    onOfflineReadyCallback,
    onRegisterErrorCallback,
  ]);

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
      const updateSWFunction = registerSW(registerOptions);
      setUpdateSW(() => updateSWFunction);
    } catch (error) {
      setState(prev => ({
        ...prev,
        isRegistered: false,
        isRegistering: false,
        error: error instanceof Error ? error : new Error(String(error)),
      }));
    }
  }, [registerOptions]);

  return {
    ...state,
    updateServiceWorker,
  };
};