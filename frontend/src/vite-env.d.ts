/// <reference types="vite/client" />

// Environment Variables Type Definitions
interface ImportMetaEnv {
  // Core API Configuration
  readonly VITE_API_BASE_URL: string;
  readonly VITE_WEBSOCKET_URL: string;

  // WebSocket Configuration  
  readonly VITE_WEBSOCKET_RECONNECT_ATTEMPTS: string;
  readonly VITE_WEBSOCKET_RECONNECT_DELAY: string;
  readonly VITE_WEBSOCKET_HEARTBEAT_INTERVAL: string;

  // Music Service Configuration
  readonly VITE_MUSIC_API_BASE_URL?: string;
  readonly VITE_MUSIC_SERVICE_URL?: string;
  
  // Spotify Integration
  readonly VITE_SPOTIFY_CLIENT_ID?: string;
  readonly VITE_SPOTIFY_REDIRECT_URI?: string;

  // Error Logging
  readonly VITE_ERROR_LOGGING_ENDPOINT?: string;
  readonly VITE_ERROR_LOGGING_API_KEY?: string;

  // Built-in Vite variables (already defined by Vite, but added for completeness)
  readonly MODE: string;
  readonly DEV: boolean;
  readonly PROD: boolean;
  readonly SSR: boolean;
  readonly BASE_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

// PWA Virtual Modules from vite-plugin-pwa
declare module 'virtual:pwa-register' {
  export interface RegisterSWOptions {
    immediate?: boolean;
    onNeedRefresh?: () => void;
    onOfflineReady?: () => void;
    onRegistered?: (registration: ServiceWorkerRegistration | undefined) => void;
    onRegisteredSW?: (swUrl: string, registration: ServiceWorkerRegistration | undefined) => void;
    onRegisterError?: (error: Error) => void;
  }

  export function registerSW(options?: RegisterSWOptions): (reloadPage?: boolean) => Promise<void>;
}

declare module 'virtual:pwa-register/react' {
  export interface RegisterSWOptions {
    immediate?: boolean;
    onNeedRefresh?: () => void;  
    onOfflineReady?: () => void;
    onRegistered?: (registration: ServiceWorkerRegistration | undefined) => void;
    onRegisteredSW?: (swUrl: string, registration: ServiceWorkerRegistration | undefined) => void;
    onRegisterError?: (error: Error) => void;
  }

  export function useRegisterSW(options?: RegisterSWOptions): {
    needRefresh: [boolean, (value: boolean) => void];
    offlineReady: [boolean, (value: boolean) => void];
    updateServiceWorker: (reloadPage?: boolean) => Promise<void>;
  };
}

declare module 'virtual:pwa-info' {
  export interface PWAInfo {
    pwaInDevEnvironment: boolean;
    webManifest: {
      linkTag: string;
    };
  }

  export const pwaInfo: PWAInfo | undefined;
}

// Workbox Window
declare module 'workbox-window' {
  export class Workbox {
    constructor(scriptURL: string, options?: {
      scope?: string;
    });
    
    register(): Promise<ServiceWorkerRegistration>;
    update(): Promise<ServiceWorkerRegistration>;
    addEventListener(type: string, listener: EventListener): void;
    removeEventListener(type: string, listener: EventListener): void;
    getSW(): Promise<ServiceWorker>;
    messageSW(data: unknown): Promise<unknown>;
  }
}