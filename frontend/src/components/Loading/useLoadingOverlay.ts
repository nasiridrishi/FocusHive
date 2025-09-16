/**
 * Hook for managing loading overlay state
 */

import React from 'react';

export const useLoadingOverlay = (): {
  isOpen: boolean;
  message: string;
  progress: number;
  showOverlay: (loadingMessage?: string) => void;
  hideOverlay: () => void;
  updateProgress: (value: number) => void;
  updateMessage: (newMessage: string) => void;
} => {
  const [isOpen, setIsOpen] = React.useState(false);
  const [message, setMessage] = React.useState<string>('');
  const [progress, setProgress] = React.useState<number>(0);

  const showOverlay = React.useCallback((loadingMessage?: string) => {
    setMessage(loadingMessage || '');
    setIsOpen(true);
  }, []);

  const hideOverlay = React.useCallback(() => {
    setIsOpen(false);
    setProgress(0);
  }, []);

  const updateProgress = React.useCallback((value: number) => {
    setProgress(Math.max(0, Math.min(100, value)));
  }, []);

  const updateMessage = React.useCallback((newMessage: string) => {
    setMessage(newMessage);
  }, []);

  return {
    isOpen,
    message,
    progress,
    showOverlay,
    hideOverlay,
    updateProgress,
    updateMessage,
  };
};

export default useLoadingOverlay;