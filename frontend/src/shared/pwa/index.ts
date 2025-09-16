/**
 * PWA (Progressive Web App) Module
 *
 * Provides comprehensive PWA functionality for FocusHive including:
 * - Service Worker registration and management
 * - Install prompts and app installation
 * - Update notifications and handling
 * - Offline functionality and caching
 * - Background sync capabilities
 * - Push notification foundation
 *
 * Architecture:
 * - Hooks for state management (useServiceWorkerRegistration, usePWAInstall)
 * - Components for UI (PWAInstallPrompt, PWAUpdateNotification)
 * - Provider for context management (PWAProvider)
 * - Types for TypeScript support
 *
 * Usage:
 * 1. Wrap your app with PWAProvider
 * 2. Use PWAUpdateNotification for automatic update handling
 * 3. Use PWAInstallPrompt for install prompts
 * 4. Use usePWA hook to access PWA state anywhere
 */

// Hooks
export {useServiceWorkerRegistration} from './hooks/useServiceWorkerRegistration';
export {usePWAInstall} from './hooks/usePWAInstall';

// Components
export {PWAProvider, usePWA, withPWA} from './components/PWAProvider';
export {PWAInstallPrompt} from './components/PWAInstallPrompt';
export {PWAUpdateNotification} from './components/PWAUpdateNotification';

// Types
export type {
  ServiceWorkerRegistrationState,
  UseServiceWorkerRegistrationOptions,
  UseServiceWorkerRegistrationReturn,
} from './hooks/useServiceWorkerRegistration';

export type {
  UsePWAInstallReturn,
} from './hooks/usePWAInstall';

export type {
  PWAContextValue,
  PWAProviderProps,
} from './components/PWAProvider';

export type {
  PWAInstallPromptProps,
} from './components/PWAInstallPrompt';

export type {
  PWAUpdateNotificationProps,
} from './components/PWAUpdateNotification';

// Re-export types from types file
export type {
  BeforeInstallPromptEvent,
  PWAInstallState,
  ServiceWorkerUpdateState,
  PWANotificationOptions,
  SyncEvent,
  PushEventData,
  WorkboxWindow,
  CacheStrategy,
  RuntimeCacheRule,
  PWAManifest,
  PWAIcon,
  PWAScreenshot,
  VitePWAConfig,
} from '../../types/pwa';