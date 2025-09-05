/**
 * Live Region Hook
 * 
 * Provides a hook for creating and managing ARIA live regions
 * for dynamic content announcements.
 */

import { useEffect, useRef, useState } from 'react';
import type { LiveRegionProps } from '../types/accessibility';

export interface UseLiveRegionOptions {
  level?: 'polite' | 'assertive' | 'off';
  atomic?: boolean;
  relevant?: 'additions' | 'removals' | 'text' | 'all';
  label?: string;
  busy?: boolean;
}

export interface UseLiveRegionReturn {
  liveRegionProps: LiveRegionProps;
  announce: (message: string) => void;
  clear: () => void;
  setBusy: (busy: boolean) => void;
  message: string;
  isBusy: boolean;
}

/**
 * Hook for creating and managing an ARIA live region
 */
export function useLiveRegion(
  options: UseLiveRegionOptions = {}
): UseLiveRegionReturn {
  const {
    level = 'polite',
    atomic = true,
    relevant = 'all',
    label,
    busy: initialBusy = false
  } = options;

  const [message, setMessage] = useState('');
  const [isBusy, setIsBusy] = useState(initialBusy);
  const clearTimeoutRef = useRef<NodeJS.Timeout>();

  const announce = (newMessage: string) => {
    // Clear any pending clear timeout
    if (clearTimeoutRef.current) {
      clearTimeout(clearTimeoutRef.current);
    }

    setMessage(newMessage);

    // Auto-clear message after a delay to allow re-announcements
    clearTimeoutRef.current = setTimeout(() => {
      setMessage('');
    }, 1000);
  };

  const clear = () => {
    if (clearTimeoutRef.current) {
      clearTimeout(clearTimeoutRef.current);
    }
    setMessage('');
  };

  const setBusy = (busy: boolean) => {
    setIsBusy(busy);
  };

  // Cleanup timeout on unmount
  useEffect(() => {
    return () => {
      if (clearTimeoutRef.current) {
        clearTimeout(clearTimeoutRef.current);
      }
    };
  }, []);

  const liveRegionProps: LiveRegionProps = {
    'aria-live': level,
    'aria-atomic': atomic,
    'aria-relevant': relevant,
    ...(label && { 'aria-label': label }),
    ...(isBusy && { 'aria-busy': true })
  };

  return {
    liveRegionProps,
    announce,
    clear,
    setBusy,
    message,
    isBusy
  };
}

/**
 * Hook for status live region (role="status")
 */
export function useStatusLiveRegion(label?: string) {
  const liveRegion = useLiveRegion({
    level: 'polite',
    atomic: true,
    relevant: 'all',
    label
  });

  const statusProps = {
    role: 'status' as const,
    ...liveRegion.liveRegionProps
  };

  return {
    ...liveRegion,
    liveRegionProps: statusProps
  };
}

/**
 * Hook for alert live region (role="alert")
 */
export function useAlertLiveRegion(label?: string) {
  const liveRegion = useLiveRegion({
    level: 'assertive',
    atomic: true,
    relevant: 'all',
    label
  });

  const alertProps = {
    role: 'alert' as const,
    ...liveRegion.liveRegionProps
  };

  return {
    ...liveRegion,
    liveRegionProps: alertProps
  };
}

/**
 * Hook for log live region (role="log")
 */
export function useLogLiveRegion(label?: string) {
  const [messages, setMessages] = useState<string[]>([]);
  const maxMessages = 10;

  const liveRegion = useLiveRegion({
    level: 'polite',
    atomic: false,
    relevant: 'additions',
    label
  });

  const addMessage = (message: string) => {
    setMessages(prev => {
      const newMessages = [...prev, message];
      return newMessages.slice(-maxMessages);
    });
    liveRegion.announce(message);
  };

  const clearMessages = () => {
    setMessages([]);
    liveRegion.clear();
  };

  const logProps = {
    role: 'log' as const,
    ...liveRegion.liveRegionProps
  };

  return {
    ...liveRegion,
    liveRegionProps: logProps,
    messages,
    addMessage,
    clearMessages
  };
}

/**
 * Hook for progress announcements
 */
export function useProgressLiveRegion(label?: string) {
  const [progress, setProgress] = useState(0);
  const [progressText, setProgressText] = useState('');
  const lastAnnouncedRef = useRef<number>(-1);
  
  const liveRegion = useLiveRegion({
    level: 'polite',
    atomic: true,
    relevant: 'text',
    label: label || 'Progress status'
  });

  const updateProgress = (
    value: number, 
    total: number = 100, 
    description?: string
  ) => {
    const percentage = Math.round((value / total) * 100);
    setProgress(percentage);

    // Only announce at certain intervals to avoid spam
    const shouldAnnounce = percentage !== lastAnnouncedRef.current && 
                          (percentage % 10 === 0 || percentage === 100);

    if (shouldAnnounce) {
      const message = description 
        ? `${description}: ${percentage}% complete`
        : `${percentage}% complete`;
      
      setProgressText(message);
      liveRegion.announce(message);
      lastAnnouncedRef.current = percentage;
    }
  };

  const completeProgress = (message: string = 'Task completed') => {
    setProgress(100);
    setProgressText(message);
    liveRegion.announce(message);
    lastAnnouncedRef.current = 100;
  };

  const resetProgress = () => {
    setProgress(0);
    setProgressText('');
    lastAnnouncedRef.current = -1;
    liveRegion.clear();
  };

  return {
    ...liveRegion,
    progress,
    progressText,
    updateProgress,
    completeProgress,
    resetProgress
  };
}

/**
 * Hook for form validation live regions
 */
export function useValidationLiveRegion() {
  const errorRegion = useAlertLiveRegion('Form errors');
  const successRegion = useStatusLiveRegion('Form status');
  
  const announceError = (message: string) => {
    errorRegion.announce(message);
  };

  const announceSuccess = (message: string) => {
    successRegion.announce(message);
  };

  const clearErrors = () => {
    errorRegion.clear();
  };

  const clearSuccess = () => {
    successRegion.clear();
  };

  const clearAll = () => {
    errorRegion.clear();
    successRegion.clear();
  };

  return {
    errorRegionProps: errorRegion.liveRegionProps,
    successRegionProps: successRegion.liveRegionProps,
    errorMessage: errorRegion.message,
    successMessage: successRegion.message,
    announceError,
    announceSuccess,
    clearErrors,
    clearSuccess,
    clearAll,
    setBusy: (busy: boolean) => {
      errorRegion.setBusy(busy);
      successRegion.setBusy(busy);
    }
  };
}

/**
 * Hook for search results live region
 */
export function useSearchLiveRegion() {
  const liveRegion = useStatusLiveRegion('Search results');
  
  const announceResults = (
    resultCount: number, 
    query: string, 
    category?: string
  ) => {
    let message;
    
    if (resultCount === 0) {
      message = `No results found for "${query}"`;
    } else if (resultCount === 1) {
      message = `1 result found for "${query}"`;
    } else {
      message = `${resultCount} results found for "${query}"`;
    }
    
    if (category) {
      message += ` in ${category}`;
    }
    
    liveRegion.announce(message);
  };

  const announceSearching = (query: string) => {
    liveRegion.announce(`Searching for "${query}"`);
  };

  const announceFilterChange = (filterName: string, filterValue: string) => {
    liveRegion.announce(`Filter applied: ${filterName} set to ${filterValue}`);
  };

  return {
    ...liveRegion,
    announceResults,
    announceSearching,
    announceFilterChange
  };
}