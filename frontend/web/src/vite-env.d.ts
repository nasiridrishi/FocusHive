/// <reference types="vite/client" />

// PWA Virtual Modules from vite-plugin-pwa
declare module 'virtual:pwa-register' {
  export interface RegisterSWOptions {
    immediate?: boolean;
    onNeedRefresh?: () => void;
    onOfflineReady?: () => void;
    onRegistered?: (registration: ServiceWorkerRegistration | undefined) => void;
    onRegisteredSW?: (swUrl: string, registration: ServiceWorkerRegistration | undefined) => void;
    onRegisterError?: (error: any) => void;
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
    onRegisterError?: (error: any) => void;
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
    messageSW(data: any): Promise<any>;
  }
}