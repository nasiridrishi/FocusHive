import React, { createContext, useContext, ReactNode } from 'react';
import { useServiceWorkerRegistration } from '../hooks/useServiceWorkerRegistration';
import { usePWAInstall } from '../hooks/usePWAInstall';
import type { ServiceWorkerRegistrationState } from '../hooks/useServiceWorkerRegistration';
import type { PWAInstallState } from '../../../types/pwa';

export interface PWAContextValue {
  // Service Worker state
  serviceWorker: ServiceWorkerRegistrationState & {
    updateServiceWorker: (reloadPage?: boolean) => Promise<void>;
  };
  
  // Install state
  install: PWAInstallState & {
    isInstalling: boolean;
    promptInstall: () => Promise<void>;
    cancelInstall: () => void;
  };
  
  // Combined state helpers
  isReady: boolean;
  hasUpdates: boolean;
  canInstall: boolean;
}

const PWAContext = createContext<PWAContextValue | undefined>(undefined);

export interface PWAProviderProps {
  children: ReactNode;
  /**
   * Options for service worker registration
   */
  serviceWorkerOptions?: {
    immediate?: boolean;
  };
  /**
   * Whether to enable automatic install prompts
   */
  enableInstallPrompts?: boolean;
}

/**
 * PWA Provider Component
 * 
 * Provides PWA functionality throughout the application via React Context.
 * Manages service worker registration, installation prompts, and update notifications.
 * 
 * Features:
 * - Centralized PWA state management
 * - Automatic service worker registration
 * - Install prompt management
 * - Update notification handling
 * - Progressive enhancement (graceful degradation)
 */
export const PWAProvider: React.FC<PWAProviderProps> = ({
  children,
  serviceWorkerOptions = { immediate: true },
  enableInstallPrompts = true,
}) => {
  const serviceWorkerState = useServiceWorkerRegistration(serviceWorkerOptions);
  const installState = usePWAInstall();

  const contextValue: PWAContextValue = {
    serviceWorker: serviceWorkerState,
    install: installState,
    
    // Combined state helpers
    isReady: serviceWorkerState.isRegistered || serviceWorkerState.offlineReady,
    hasUpdates: serviceWorkerState.needsRefresh,
    canInstall: installState.isInstallable && enableInstallPrompts,
  };

  return (
    <PWAContext.Provider value={contextValue}>
      {children}
    </PWAContext.Provider>
  );
};

/**
 * Hook to access PWA context
 * Must be used within a PWAProvider
 */
// eslint-disable-next-line react-refresh/only-export-components
export const usePWA = (): PWAContextValue => {
  const context = useContext(PWAContext);
  
  if (context === undefined) {
    throw new Error('usePWA must be used within a PWAProvider');
  }
  
  return context;
};

/**
 * Higher-order component to wrap components with PWA context
 */
// eslint-disable-next-line react-refresh/only-export-components
export const withPWA = <P extends object>(
  Component: React.ComponentType<P>
): React.FC<P> => {
  return (props: P) => (
    <PWAProvider>
      <Component {...props} />
    </PWAProvider>
  );
};