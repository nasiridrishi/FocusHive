/**
 * Keyboard Navigation Hook
 *
 * Provides keyboard navigation functionality for lists, menus,
 * tabs, and other navigable components.
 */

import {useCallback, useEffect, useRef, useState} from 'react';
import {getFocusableElements} from '../utils/focusManagement';
import {KEYBOARD} from '../constants/wcag';
import type {
  KeyboardNavigationConfig,
  NavigationDirection,
  UseKeyboardNavigationReturn
} from '../types/accessibility';

export interface UseKeyboardNavigationOptions extends KeyboardNavigationConfig {
  containerRef?: React.RefObject<HTMLElement>;
  enabled?: boolean;
  itemSelector?: string;
  initialIndex?: number;
  autoFocus?: boolean;
}

/**
 * Hook for keyboard navigation in lists and menus
 */
export function useKeyboardNavigation(
    options: UseKeyboardNavigationOptions = {}
): UseKeyboardNavigationReturn {
  const {
    orientation = 'vertical',
    wrap = true,
    loop = true,
    roving = false,
    containerRef,
    enabled = true,
    itemSelector,
    initialIndex = -1,
    autoFocus = false,
    onNavigate,
    onSelect
  } = options;

  const [currentIndex, setCurrentIndex] = useState(initialIndex);
  const internalContainerRef = useRef<HTMLElement | null>(null);
  const elementsRef = useRef<HTMLElement[]>([]);
  const isNavigatingRef = useRef(false);

  // Get container element
  const getContainer = useCallback((): HTMLElement | null => {
    return containerRef?.current || internalContainerRef.current;
  }, [containerRef]);

  // Update elements list
  const updateElements = useCallback(() => {
    const container = getContainer();
    if (!container) return;

    if (itemSelector) {
      elementsRef.current = Array.from(
          container.querySelectorAll<HTMLElement>(itemSelector)
      ).filter(el => {
        // Filter out disabled or hidden elements
        return !el.hasAttribute('disabled') &&
            !el.hasAttribute('aria-disabled') &&
            !el.hidden &&
            el.offsetWidth > 0 &&
            el.offsetHeight > 0;
      });
    } else {
      elementsRef.current = getFocusableElements(container);
    }
  }, [getContainer, itemSelector]);

  // Focus element at given index
  const focusElement = useCallback((index: number) => {
    updateElements();

    if (index < 0 || index >= elementsRef.current.length) return;

    const element = elementsRef.current[index];
    if (element) {
      isNavigatingRef.current = true;
      element.focus();

      // Update roving tabindex if enabled
      if (roving) {
        elementsRef.current.forEach((el, i) => {
          el.setAttribute('tabindex', i === index ? '0' : '-1');
        });
      }

      // Call navigation callback
      if (onNavigate) {
        onNavigate(element, 'focus');
      }

      setTimeout(() => {
        isNavigatingRef.current = false;
      }, 0);
    }
  }, [updateElements, roving, onNavigate]);

  // Navigate to next element
  const focusNext = useCallback(() => {
    updateElements();
    let nextIndex = currentIndex + 1;

    if (nextIndex >= elementsRef.current.length) {
      nextIndex = wrap || loop ? 0 : elementsRef.current.length - 1;
    }

    setCurrentIndex(nextIndex);
    focusElement(nextIndex);
  }, [currentIndex, wrap, loop, focusElement, updateElements]);

  // Navigate to previous element
  const focusPrevious = useCallback(() => {
    updateElements();
    let prevIndex = currentIndex - 1;

    if (prevIndex < 0) {
      prevIndex = wrap || loop ? elementsRef.current.length - 1 : 0;
    }

    setCurrentIndex(prevIndex);
    focusElement(prevIndex);
  }, [currentIndex, wrap, loop, focusElement, updateElements]);

  // Navigate to first element
  const focusFirst = useCallback(() => {
    setCurrentIndex(0);
    focusElement(0);
  }, [focusElement]);

  // Navigate to last element
  const focusLast = useCallback(() => {
    updateElements();
    const lastIndex = elementsRef.current.length - 1;
    setCurrentIndex(lastIndex);
    focusElement(lastIndex);
  }, [focusElement, updateElements]);

  // Navigate in a specific direction
  const navigate = useCallback((direction: NavigationDirection) => {
    switch (direction) {
      case 'up':
        if (orientation === 'vertical' || orientation === 'both') {
          focusPrevious();
        }
        break;
      case 'down':
        if (orientation === 'vertical' || orientation === 'both') {
          focusNext();
        }
        break;
      case 'left':
        if (orientation === 'horizontal' || orientation === 'both') {
          focusPrevious();
        }
        break;
      case 'right':
        if (orientation === 'horizontal' || orientation === 'both') {
          focusNext();
        }
        break;
      case 'first':
        focusFirst();
        break;
      case 'last':
        focusLast();
        break;
    }
  }, [orientation, focusNext, focusPrevious, focusFirst, focusLast]);

  // Handle keyboard events
  const handleKeyDown = useCallback((event: React.KeyboardEvent) => {
    if (!enabled) return;

    const {key, ctrlKey, shiftKey, altKey, metaKey} = event;
    let handled = false;

    // Don't handle if modifier keys are pressed (except shift for selection)
    if ((ctrlKey || altKey || metaKey) && !shiftKey) return;

    switch (key) {
      case KEYBOARD.ARROW_UP:
        if (orientation === 'vertical' || orientation === 'both') {
          navigate('up');
          handled = true;
        }
        break;

      case KEYBOARD.ARROW_DOWN:
        if (orientation === 'vertical' || orientation === 'both') {
          navigate('down');
          handled = true;
        }
        break;

      case KEYBOARD.ARROW_LEFT:
        if (orientation === 'horizontal' || orientation === 'both') {
          navigate('left');
          handled = true;
        }
        break;

      case KEYBOARD.ARROW_RIGHT:
        if (orientation === 'horizontal' || orientation === 'both') {
          navigate('right');
          handled = true;
        }
        break;

      case KEYBOARD.HOME:
        navigate('first');
        handled = true;
        break;

      case KEYBOARD.END:
        navigate('last');
        handled = true;
        break;

      case KEYBOARD.ENTER:
      case KEYBOARD.SPACE:
        updateElements();
        if (currentIndex >= 0 && currentIndex < elementsRef.current.length) {
          const currentElement = elementsRef.current[currentIndex];
          if (currentElement && onSelect) {
            onSelect(currentElement);
            handled = true;
          }
        }
        break;

      case KEYBOARD.TAB:
        // Let Tab work normally, but update current index if roving
        if (roving) {
          setTimeout(() => {
            updateElements();
            const focusedIndex = elementsRef.current.findIndex(
                el => el === document.activeElement
            );
            if (focusedIndex !== -1) {
              setCurrentIndex(focusedIndex);
            }
          }, 0);
        }
        break;
    }

    if (handled) {
      event.preventDefault();
      event.stopPropagation();
    }
  }, [enabled, orientation, navigate, currentIndex, onSelect, roving, updateElements]);

  // Handle focus events to track current index
  const handleFocus = useCallback((event: React.FocusEvent) => {
    if (isNavigatingRef.current) return;

    updateElements();
    const focusedElement = event.target as HTMLElement;
    const focusedIndex = elementsRef.current.indexOf(focusedElement);

    if (focusedIndex !== -1) {
      setCurrentIndex(focusedIndex);

      // Update roving tabindex
      if (roving) {
        elementsRef.current.forEach((el, i) => {
          el.setAttribute('tabindex', i === focusedIndex ? '0' : '-1');
        });
      }
    }
  }, [roving, updateElements]);

  // Initialize roving tabindex
  useEffect(() => {
    if (!roving || !enabled) return;

    updateElements();

    elementsRef.current.forEach((el, i) => {
      el.setAttribute('tabindex', i === currentIndex ? '0' : '-1');
    });
  }, [roving, enabled, currentIndex, updateElements]);

  // Auto focus initial element
  useEffect(() => {
    if (autoFocus && enabled && currentIndex >= 0) {
      focusElement(currentIndex);
    }
  }, [autoFocus, enabled, currentIndex, focusElement]);

  // Update elements when container changes
  useEffect(() => {
    const container = getContainer();
    if (!container) return;

    updateElements();

    // Set up mutation observer to watch for DOM changes
    const observer = new MutationObserver(() => {
      updateElements();
    });

    observer.observe(container, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ['disabled', 'aria-disabled', 'hidden', 'tabindex']
    });

    return () => observer.disconnect();
  }, [getContainer, updateElements]);

  return {
    currentIndex,
    setCurrentIndex: (index: number) => {
      setCurrentIndex(index);
      focusElement(index);
    },
    focusNext,
    focusPrevious,
    focusFirst,
    focusLast,
    handleKeyDown,
    handleFocus,
    navigate,
    elements: elementsRef.current,
    containerRef: containerRef || internalContainerRef
  };
}

/**
 * Hook for tab-like navigation
 */
export function useTabNavigation(
    containerRef?: React.RefObject<HTMLElement>,
    enabled: boolean = true
) {
  return useKeyboardNavigation({
    containerRef,
    enabled,
    orientation: 'horizontal',
    roving: true,
    itemSelector: '[role="tab"]',
    wrap: true
  });
}

/**
 * Hook for menu navigation
 */
export function useMenuNavigation(
    containerRef?: React.RefObject<HTMLElement>,
    enabled: boolean = true
) {
  return useKeyboardNavigation({
    containerRef,
    enabled,
    orientation: 'vertical',
    roving: false,
    itemSelector: '[role="menuitem"], [role="menuitemcheckbox"], [role="menuitemradio"]',
    wrap: true,
    loop: true
  });
}

/**
 * Hook for grid navigation
 */
export function useGridNavigation(
    containerRef?: React.RefObject<HTMLElement>,
    enabled: boolean = true
) {
  return useKeyboardNavigation({
    containerRef,
    enabled,
    orientation: 'both',
    roving: true,
    itemSelector: '[role="gridcell"]',
    wrap: false,
    loop: false
  });
}

/**
 * Hook for listbox navigation
 */
export function useListboxNavigation(
    containerRef?: React.RefObject<HTMLElement>,
    enabled: boolean = true
) {
  return useKeyboardNavigation({
    containerRef,
    enabled,
    orientation: 'vertical',
    roving: false,
    itemSelector: '[role="option"]',
    wrap: true,
    loop: true
  });
}