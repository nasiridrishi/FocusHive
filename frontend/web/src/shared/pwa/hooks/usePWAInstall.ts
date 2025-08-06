import { useState, useEffect, useCallback } from 'react';
import type { BeforeInstallPromptEvent, PWAInstallState } from '../../../types/pwa';

export interface UsePWAInstallReturn extends PWAInstallState {
  isInstalling: boolean;
  promptInstall: () => Promise<void>;
  cancelInstall: () => void;
}

/**
 * Custom hook for managing PWA installation prompts and state
 * Handles beforeinstallprompt events and provides install functionality
 */
export const usePWAInstall = (): UsePWAInstallReturn => {
  const [state, setState] = useState<PWAInstallState & { isInstalling: boolean }>({
    isInstallable: false,
    isInstalled: false,
    isStandalone: false,
    installPrompt: null,
    isInstalling: false,
  });

  // Check if app is running in standalone mode
  const checkStandaloneMode = useCallback(() => {
    // Check iOS Safari standalone mode
    const isIOSStandalone = (window.navigator as any).standalone === true;
    
    // Check display-mode: standalone media query
    const isStandalone = window.matchMedia('(display-mode: standalone)').matches;
    
    return isIOSStandalone || isStandalone;
  }, []);

  const promptInstall = useCallback(async () => {
    const { installPrompt } = state;
    
    if (!installPrompt) {
      return;
    }

    setState(prev => ({ ...prev, isInstalling: true }));

    try {
      // Prevent the default mini-infobar from appearing
      installPrompt.preventDefault();
      
      // Show the install prompt
      await installPrompt.prompt();
      
      // Wait for the user to respond to the prompt
      const result = await installPrompt.userChoice;
      
      // Clear the install prompt regardless of outcome
      setState(prev => ({
        ...prev,
        isInstalling: false,
        isInstallable: false,
        installPrompt: null,
      }));

      if (result.outcome === 'accepted') {
        console.log('User accepted the install prompt');
      } else {
        console.log('User dismissed the install prompt');
      }
    } catch (error) {
      console.error('Error during install prompt:', error);
      setState(prev => ({
        ...prev,
        isInstalling: false,
        isInstallable: false,
        installPrompt: null,
      }));
    }
  }, [state]);

  const cancelInstall = useCallback(() => {
    setState(prev => ({
      ...prev,
      isInstallable: false,
      installPrompt: null,
    }));
  }, []);

  useEffect(() => {
    // Check initial standalone mode
    const isStandalone = checkStandaloneMode();
    
    setState(prev => ({
      ...prev,
      isStandalone,
      isInstalled: isStandalone,
    }));

    // Handle beforeinstallprompt event
    const handleBeforeInstallPrompt = (event: Event) => {
      const beforeInstallPromptEvent = event as BeforeInstallPromptEvent;
      
      // Prevent the mini-infobar from appearing on mobile
      beforeInstallPromptEvent.preventDefault();
      
      // Save the event so it can be triggered later
      setState(prev => ({
        ...prev,
        isInstallable: true,
        installPrompt: beforeInstallPromptEvent,
      }));
    };

    // Handle app installed event
    const handleAppInstalled = () => {
      setState(prev => ({
        ...prev,
        isInstalled: true,
        isInstallable: false,
        installPrompt: null,
      }));
    };

    // Add event listeners
    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    window.addEventListener('appinstalled', handleAppInstalled);

    // Listen for display mode changes
    const mediaQuery = window.matchMedia('(display-mode: standalone)');
    const handleDisplayModeChange = (e: MediaQueryListEvent) => {
      setState(prev => ({
        ...prev,
        isStandalone: e.matches,
        isInstalled: e.matches,
      }));
    };

    if (mediaQuery.addEventListener) {
      mediaQuery.addEventListener('change', handleDisplayModeChange);
    } else {
      // Fallback for older browsers
      mediaQuery.addListener(handleDisplayModeChange);
    }

    // Cleanup
    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
      window.removeEventListener('appinstalled', handleAppInstalled);
      
      if (mediaQuery.removeEventListener) {
        mediaQuery.removeEventListener('change', handleDisplayModeChange);
      } else {
        // Fallback for older browsers
        mediaQuery.removeListener(handleDisplayModeChange);
      }
    };
  }, [checkStandaloneMode]);

  return {
    ...state,
    promptInstall,
    cancelInstall,
  };
};