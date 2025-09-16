// Mock for virtual:pwa-register/react module
import {useState} from 'react';

export function useRegisterSW() {
  const [updateAvailable] = useState(false);
  const [registration] = useState<ServiceWorkerRegistration | undefined>(undefined);

  return {
    updateAvailable,
    registration,
    updateServiceWorker: () => Promise.resolve(),
  };
}