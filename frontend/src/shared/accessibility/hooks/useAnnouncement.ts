/**
 * Announcement Hook
 *
 * Provides screen reader announcements through ARIA live regions
 * for dynamic content updates and user feedback.
 */

import {useCallback, useRef, useState} from 'react';
import {liveRegionManager} from '../utils/ariaUtils';
import {SCREEN_READER_DELAYS} from '../constants/wcag';
import type {AnnouncementOptions, UseAnnouncementReturn} from '../types/accessibility';

/**
 * Hook for managing screen reader announcements
 */
export function useAnnouncement(): UseAnnouncementReturn {
  const [lastAnnouncement, setLastAnnouncement] = useState<string | null>(null);
  const timeoutRef = useRef<NodeJS.Timeout>();

  const announce = useCallback((
      message: string,
      options: AnnouncementOptions = {}
  ) => {
    const {
      level = 'polite',
      delay = 0,
      clearAfter = SCREEN_READER_DELAYS.CLEAR_ANNOUNCEMENT,
      interrupt = false
    } = options;

    if (!message.trim()) return;

    // Clear existing timeout
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }

    // Clear previous announcement if interrupting
    if (interrupt) {
      liveRegionManager.clear();
    }

    // Announce with optional delay
    setTimeout(() => {
      setLastAnnouncement(message);

      switch (level) {
        case 'assertive':
          liveRegionManager.announceAssertive(message);
          break;
        case 'status':
          liveRegionManager.announceStatus(message);
          break;
        default:
          liveRegionManager.announcePolite(message);
          break;
      }

      // Clear after specified time
      if (clearAfter > 0) {
        timeoutRef.current = setTimeout(() => {
          setLastAnnouncement(null);
        }, clearAfter);
      }
    }, delay);
  }, []);

  const clear = useCallback(() => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }
    liveRegionManager.clear();
    setLastAnnouncement(null);
  }, []);

  return {
    announce,
    clear,
    lastAnnouncement
  };
}

/**
 * Hook for form-specific announcements
 */
export function useFormAnnouncement(): void {
  const {announce, clear, lastAnnouncement} = useAnnouncement();

  const announceError = useCallback((message: string, fieldName?: string) => {
    const errorMessage = fieldName
        ? `Error in ${fieldName}: ${message}`
        : `Error: ${message}`;

    announce(errorMessage, {level: 'assertive', interrupt: true});
  }, [announce]);

  const announceSuccess = useCallback((message: string) => {
    announce(message, {level: 'polite', delay: SCREEN_READER_DELAYS.SHORT});
  }, [announce]);

  const announceValidation = useCallback((message: string) => {
    announce(message, {
      level: 'polite',
      delay: SCREEN_READER_DELAYS.AFTER_FOCUS_CHANGE
    });
  }, [announce]);

  return {
    announce,
    announceError,
    announceSuccess,
    announceValidation,
    clear,
    lastAnnouncement
  };
}

/**
 * Hook for navigation announcements
 */
export function useNavigationAnnouncement(): void {
  const {announce, clear, lastAnnouncement} = useAnnouncement();

  const announceRouteChange = useCallback((routeName: string) => {
    const message = `Navigated to ${routeName}`;
    announce(message, {
      level: 'polite',
      delay: SCREEN_READER_DELAYS.AFTER_ROUTE_CHANGE
    });
  }, [announce]);

  const announcePageLoad = useCallback((pageTitle: string) => {
    const message = `${pageTitle} page loaded`;
    announce(message, {
      level: 'polite',
      delay: SCREEN_READER_DELAYS.AFTER_PAGE_LOAD
    });
  }, [announce]);

  const announceModalOpen = useCallback((modalTitle: string) => {
    const message = `${modalTitle} dialog opened`;
    announce(message, {
      level: 'polite',
      delay: SCREEN_READER_DELAYS.AFTER_MODAL_OPEN
    });
  }, [announce]);

  const announceModalClose = useCallback((modalTitle: string) => {
    const message = `${modalTitle} dialog closed`;
    announce(message, {level: 'polite'});
  }, [announce]);

  return {
    announce,
    announceRouteChange,
    announcePageLoad,
    announceModalOpen,
    announceModalClose,
    clear,
    lastAnnouncement
  };
}

/**
 * Hook for loading state announcements
 */
export function useLoadingAnnouncement(): void {
  const {announce, clear, lastAnnouncement} = useAnnouncement();
  const loadingTimeoutRef = useRef<NodeJS.Timeout>();

  const announceLoading = useCallback((
      message: string = 'Loading content',
      delay: number = SCREEN_READER_DELAYS.AFTER_PAGE_LOAD
  ) => {
    // Clear any existing loading timeout
    if (loadingTimeoutRef.current) {
      clearTimeout(loadingTimeoutRef.current);
    }

    // Announce loading after a delay to avoid announcing quick loads
    loadingTimeoutRef.current = setTimeout(() => {
      announce(message, {level: 'polite'});
    }, delay);
  }, [announce]);

  const announceLoaded = useCallback((
      message: string = 'Content loaded'
  ) => {
    // Clear loading timeout since content is now loaded
    if (loadingTimeoutRef.current) {
      clearTimeout(loadingTimeoutRef.current);
    }

    announce(message, {level: 'polite'});
  }, [announce]);

  const announceLoadingError = useCallback((
      message: string = 'Error loading content'
  ) => {
    // Clear loading timeout
    if (loadingTimeoutRef.current) {
      clearTimeout(loadingTimeoutRef.current);
    }

    announce(message, {level: 'assertive', interrupt: true});
  }, [announce]);

  const announceProgress = useCallback((
      current: number,
      total: number,
      label: string = 'items'
  ) => {
    const percentage = Math.round((current / total) * 100);
    const message = `Loading progress: ${percentage}% complete, ${current} of ${total} ${label}`;
    announce(message, {level: 'polite'});
  }, [announce]);

  return {
    announce,
    announceLoading,
    announceLoaded,
    announceLoadingError,
    announceProgress,
    clear,
    lastAnnouncement
  };
}

/**
 * Hook for table announcements
 */
export function useTableAnnouncement(): void {
  const {announce} = useAnnouncement();

  const announceSortChange = useCallback((
      column: string,
      direction: 'asc' | 'desc'
  ) => {
    const directionText = direction === 'asc' ? 'ascending' : 'descending';
    announce(`Table sorted by ${column}, ${directionText}`, {level: 'polite'});
  }, [announce]);

  const announceFilterChange = useCallback((
      filterType: string,
      value: string
  ) => {
    announce(`Table filtered by ${filterType}: ${value}`, {level: 'polite'});
  }, [announce]);

  const announceRowCount = useCallback((count: number) => {
    const message = count === 1
        ? '1 row in table'
        : `${count} rows in table`;
    announce(message, {level: 'polite'});
  }, [announce]);

  const announceSelection = useCallback((
      selectedCount: number,
      totalCount: number
  ) => {
    const message = selectedCount === 0
        ? 'No rows selected'
        : selectedCount === 1
            ? '1 row selected'
            : selectedCount === totalCount
                ? `All ${totalCount} rows selected`
                : `${selectedCount} of ${totalCount} rows selected`;

    announce(message, {level: 'polite'});
  }, [announce]);

  return {
    announce,
    announceSortChange,
    announceFilterChange,
    announceRowCount,
    announceSelection
  };
}

/**
 * Hook for status announcements with smart debouncing
 */
export function useStatusAnnouncement(debounceMs: number = 300): void {
  const {announce, clear, lastAnnouncement} = useAnnouncement();
  const debounceTimeoutRef = useRef<NodeJS.Timeout>();
  const pendingMessageRef = useRef<string>('');

  const announceStatus = useCallback((message: string) => {
    pendingMessageRef.current = message;

    if (debounceTimeoutRef.current) {
      clearTimeout(debounceTimeoutRef.current);
    }

    debounceTimeoutRef.current = setTimeout(() => {
      if (pendingMessageRef.current) {
        announce(pendingMessageRef.current, {level: 'status'});
        pendingMessageRef.current = '';
      }
    }, debounceMs);
  }, [announce, debounceMs]);

  const announceImmediate = useCallback((message: string) => {
    if (debounceTimeoutRef.current) {
      clearTimeout(debounceTimeoutRef.current);
    }
    pendingMessageRef.current = '';
    announce(message, {level: 'status'});
  }, [announce]);

  return {
    announce,
    announceStatus,
    announceImmediate,
    clear,
    lastAnnouncement
  };
}

/**
 * Hook for focus announcements
 */
export function useFocusAnnouncement(): void {
  const {announce} = useAnnouncement();

  const announceFocusChange = useCallback((
      elementName: string,
      context?: string
  ) => {
    const message = context
        ? `Focused on ${elementName} in ${context}`
        : `Focused on ${elementName}`;

    announce(message, {
      level: 'polite',
      delay: SCREEN_READER_DELAYS.AFTER_FOCUS_CHANGE
    });
  }, [announce]);

  const announceTabChange = useCallback((
      tabName: string,
      position: number,
      total: number
  ) => {
    const message = `${tabName} tab, ${position} of ${total}`;
    announce(message, {level: 'polite'});
  }, [announce]);

  const announceMenuNavigation = useCallback((
      itemName: string,
      position: number,
      total: number,
      hasSubMenu: boolean = false
  ) => {
    const subMenuText = hasSubMenu ? ', has submenu' : '';
    const message = `${itemName}, ${position} of ${total}${subMenuText}`;
    announce(message, {level: 'polite'});
  }, [announce]);

  return {
    announce,
    announceFocusChange,
    announceTabChange,
    announceMenuNavigation
  };
}