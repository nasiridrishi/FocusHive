/**
 * PWA TypeScript Type Definitions
 * Extends global interfaces for PWA functionality
 */

// Extend Window interface for PWA events
declare global {
  interface Window {
    beforeinstallprompt?: BeforeInstallPromptEvent;
    workbox?: WorkboxWindow;
  }

  interface Navigator {
    standalone?: boolean;
  }
}

// BeforeInstallPrompt Event interface
export interface BeforeInstallPromptEvent extends Event {
  readonly platforms: string[];
  readonly userChoice: Promise<{
    outcome: 'accepted' | 'dismissed';
    platform: string;
  }>;

  prompt(): Promise<void>;
}

// PWA Install State
export interface PWAInstallState {
  isInstallable: boolean;
  isInstalled: boolean;
  isStandalone: boolean;
  installPrompt: BeforeInstallPromptEvent | null;
}

// Service Worker Update State
export interface ServiceWorkerUpdateState {
  isUpdateAvailable: boolean;
  isUpdating: boolean;
  needsRefresh: boolean;
  registration: ServiceWorkerRegistration | null;
}

// Notification Action interface (since it might not be available in all environments)
export interface PWANotificationAction {
  action: string;
  title: string;
  icon?: string;
}

// PWA Notification Options
export interface PWANotificationOptions extends NotificationOptions {
  actions?: PWANotificationAction[];
  badge?: string;
  data?: unknown;
  image?: string;
  renotify?: boolean;
  requireInteraction?: boolean;
  silent?: boolean;
  timestamp?: number;
  vibrate?: number | number[];
}

// Background Sync Event
export interface SyncEvent extends ExtendableEvent {
  readonly tag: string;
  readonly lastChance: boolean;
}

// Push Event Data
export interface PushEventData {
  title: string;
  body: string;
  icon?: string;
  badge?: string;
  image?: string;
  data?: unknown;
  actions?: PWANotificationAction[];
  tag?: string;
  requireInteraction?: boolean;
}

// Workbox Window types
export interface WorkboxWindow {
  register(): Promise<ServiceWorkerRegistration>;

  update(): Promise<ServiceWorkerRegistration>;

  addEventListener(type: string, listener: EventListener): void;

  removeEventListener(type: string, listener: EventListener): void;

  getSW(): Promise<ServiceWorker>;

  messageSW(data: unknown): Promise<unknown>;
}

// Cache Strategy Types
export type CacheStrategy =
    | 'CacheFirst'
    | 'CacheOnly'
    | 'NetworkFirst'
    | 'NetworkOnly'
    | 'StaleWhileRevalidate';

// Runtime Caching Rule
export interface RuntimeCacheRule {
  urlPattern: RegExp | string;
  handler: CacheStrategy;
  options?: {
    cacheName?: string;
    expiration?: {
      maxEntries?: number;
      maxAgeSeconds?: number;
    };
    cacheableResponse?: {
      statuses?: number[];
      headers?: Record<string, string>;
    };
  };
}

// PWA Manifest
export interface PWAManifest {
  name: string;
  short_name: string;
  description: string;
  start_url: string;
  display: 'fullscreen' | 'standalone' | 'minimal-ui' | 'browser';
  background_color: string;
  theme_color: string;
  icons: PWAIcon[];
  categories?: string[];
  screenshots?: PWAScreenshot[];
}

export interface PWAIcon {
  src: string;
  sizes: string;
  type: string;
  purpose?: 'any' | 'maskable' | 'monochrome';
}

export interface PWAScreenshot {
  src: string;
  sizes: string;
  type: string;
  form_factor?: 'narrow' | 'wide';
  label?: string;
}

// PWA Configuration for Vite Plugin
export interface VitePWAConfig {
  registerType?: 'autoUpdate' | 'prompt';
  workbox?: {
    globPatterns?: string[];
    runtimeCaching?: RuntimeCacheRule[];
    navigateFallback?: string;
    navigateFallbackDenylist?: RegExp[];
    cleanupOutdatedCaches?: boolean;
    sourcemap?: boolean;
  };
  manifest?: Partial<PWAManifest>;
  devOptions?: {
    enabled?: boolean;
    type?: 'classic' | 'module';
  };
  strategies?: 'generateSW' | 'injectManifest';
  srcDir?: string;
  filename?: string;
}

export {};