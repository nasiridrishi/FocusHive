/**
 * Focus Trap Hook
 *
 * Provides focus trapping functionality for modal dialogs,
 * dropdown menus, and other overlay components.
 */

import {useCallback, useEffect, useRef} from 'react';
import {FocusTrap} from '../utils/focusManagement';
import type {FocusTrapConfig, UseFocusTrapReturn} from '../types/accessibility';

export interface UseFocusTrapOptions extends FocusTrapConfig {
  isActive?: boolean;
  containerRef?: React.RefObject<HTMLElement>;
}

/**
 * Hook for managing focus trapping in components
 */
export function useFocusTrap(options: UseFocusTrapOptions = {}): UseFocusTrapReturn {
  const {
    isActive = false,
    containerRef,
    initialFocus,
    restoreFocus,
    onEscape,
    allowOutsideClick = false,
    preventScroll = true
  } = options;

  const internalContainerRef = useRef<HTMLElement | null>(null);
  const focusTrapRef = useRef<FocusTrap | null>(null);
  const isActiveRef = useRef(false);

  // Get the container element
  const getContainer = useCallback((): HTMLElement | null => {
    return containerRef?.current || internalContainerRef.current;
  }, [containerRef]);

  // Get focusable elements in the container
  const getFocusableElements = useCallback((): HTMLElement[] => {
    const container = getContainer();
    if (!container || !focusTrapRef.current) return [];

    // Import getFocusableElements dynamically to avoid circular dependencies
    import('../utils/focusManagement').then(({getFocusableElements}) => {
      return getFocusableElements(container);
    });

    return [];
  }, [getContainer]);

  // Activate the focus trap
  const activate = useCallback(() => {
    if (isActiveRef.current) return;

    const container = getContainer();
    if (!container) {
      // console.warn('useFocusTrap: No container element found');
      return;
    }

    // Create focus trap if it doesn't exist
    if (!focusTrapRef.current) {
      focusTrapRef.current = new FocusTrap(container, {
        initialFocusElement: typeof initialFocus === 'string'
            ? document.querySelector(initialFocus) as HTMLElement
            : initialFocus,
        restoreFocusElement: typeof restoreFocus === 'string'
            ? document.querySelector(restoreFocus) as HTMLElement
            : restoreFocus,
        onEscape,
        allowOutsideClick
      });
    }

    focusTrapRef.current.activate();
    isActiveRef.current = true;

    // Prevent body scroll if requested
    if (preventScroll) {
      document.body.style.overflow = 'hidden';
    }
  }, [getContainer, initialFocus, restoreFocus, onEscape, allowOutsideClick, preventScroll]);

  // Deactivate the focus trap
  const deactivate = useCallback(() => {
    if (!isActiveRef.current) return;

    if (focusTrapRef.current) {
      focusTrapRef.current.deactivate();
    }

    isActiveRef.current = false;

    // Restore body scroll
    if (preventScroll) {
      document.body.style.overflow = '';
    }
  }, [preventScroll]);

  // Effect to handle activation/deactivation based on isActive prop
  useEffect(() => {
    if (isActive) {
      activate();
    } else {
      deactivate();
    }

    // Cleanup on unmount
    return () => {
      deactivate();
      if (focusTrapRef.current) {
        focusTrapRef.current.deactivate();
      }
    };
  }, [isActive, activate, deactivate]);

  // Effect to handle escape key globally (fallback)
  useEffect(() => {
    const handleEscape = (event: KeyboardEvent): void => {
      if (event.key === 'Escape' && isActiveRef.current && onEscape) {
        event.preventDefault();
        event.stopPropagation();
        onEscape();
      }
    };

    if (isActive) {
      document.addEventListener('keydown', handleEscape, true);
      return () => document.removeEventListener('keydown', handleEscape, true);
    }
  }, [isActive, onEscape]);

  return {
    activate,
    deactivate,
    isActive: isActiveRef.current,
    focusableElements: getFocusableElements(),
    containerRef: containerRef || internalContainerRef
  };
}

/**
 * Simplified focus trap hook for basic modal usage
 */
export function useModalFocusTrap(isOpen: boolean, onClose?: () => void): void {
  return useFocusTrap({
    isActive: isOpen,
    onEscape: onClose,
    allowOutsideClick: false,
    preventScroll: true
  });
}

/**
 * Focus trap hook for dropdown menus
 */
export function useDropdownFocusTrap(
    isOpen: boolean,
    onClose?: () => void,
    containerRef?: React.RefObject<HTMLElement>
) {
  return useFocusTrap({
    isActive: isOpen,
    containerRef,
    onEscape: onClose,
    allowOutsideClick: true,
    preventScroll: false
  });
}