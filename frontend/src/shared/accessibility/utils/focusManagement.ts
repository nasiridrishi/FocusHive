/**
 * Focus Management Utilities for Accessibility
 * 
 * Provides utilities for managing focus, keyboard navigation,
 * and focus trapping according to accessibility best practices.
 */

/**
 * Focusable element selector for all interactive elements
 */
export const FOCUSABLE_ELEMENTS = [
  'a[href]',
  'area[href]',
  'input:not([disabled]):not([type="hidden"]):not([aria-hidden])',
  'select:not([disabled]):not([aria-hidden])',
  'textarea:not([disabled]):not([aria-hidden])',
  'button:not([disabled]):not([aria-hidden])',
  'iframe',
  'object',
  'embed',
  '[contenteditable]',
  '[tabindex]:not([tabindex^="-"])',
  'audio[controls]',
  'video[controls]',
  'summary',
  '[role="button"]:not([aria-disabled="true"])',
  '[role="link"]:not([aria-disabled="true"])',
  '[role="menuitem"]:not([aria-disabled="true"])',
  '[role="option"]:not([aria-disabled="true"])',
  '[role="switch"]:not([aria-disabled="true"])',
  '[role="tab"]:not([aria-disabled="true"])',
  '[role="textbox"]:not([aria-disabled="true"])',
].join(', ');

/**
 * Get all focusable elements within a container
 */
export function getFocusableElements(container: HTMLElement): HTMLElement[] {
  return Array.from(container.querySelectorAll(FOCUSABLE_ELEMENTS))
    .filter((el): el is HTMLElement => {
      const element = el as HTMLElement;
      
      // Check if element is visible
      if (element.offsetWidth === 0 && element.offsetHeight === 0) return false;
      if (window.getComputedStyle(element).visibility === 'hidden') return false;
      if (window.getComputedStyle(element).display === 'none') return false;
      
      // Check if element is within a hidden parent
      let parent = element.parentElement;
      while (parent) {
        const style = window.getComputedStyle(parent);
        if (style.display === 'none' || style.visibility === 'hidden') return false;
        parent = parent.parentElement;
      }
      
      return true;
    });
}

/**
 * Get the first focusable element in a container
 */
export function getFirstFocusableElement(container: HTMLElement): HTMLElement | null {
  const focusableElements = getFocusableElements(container);
  return focusableElements.length > 0 ? focusableElements[0] : null;
}

/**
 * Get the last focusable element in a container
 */
export function getLastFocusableElement(container: HTMLElement): HTMLElement | null {
  const focusableElements = getFocusableElements(container);
  return focusableElements.length > 0 ? focusableElements[focusableElements.length - 1] : null;
}

/**
 * Create a focus trap for modal dialogs and other overlay components
 */
export interface FocusTrapOptions {
  initialFocusElement?: HTMLElement | null;
  restoreFocusElement?: HTMLElement | null;
  onEscape?: () => void;
  allowOutsideClick?: boolean;
}

export class FocusTrap {
  private container: HTMLElement;
  private options: FocusTrapOptions;
  private previousActiveElement: Element | null = null;
  private isActive = false;
  
  constructor(container: HTMLElement, options: FocusTrapOptions = {}) {
    this.container = container;
    this.options = options;
  }
  
  activate() {
    if (this.isActive) return;
    
    this.isActive = true;
    this.previousActiveElement = document.activeElement;
    
    // Set initial focus
    const initialFocus = this.options.initialFocusElement || getFirstFocusableElement(this.container);
    if (initialFocus) {
      initialFocus.focus();
    }
    
    // Add event listeners
    document.addEventListener('keydown', this.handleKeyDown);
    document.addEventListener('focusin', this.handleFocusIn);
    
    if (this.options.allowOutsideClick) {
      document.addEventListener('click', this.handleOutsideClick);
    }
  }
  
  deactivate() {
    if (!this.isActive) return;
    
    this.isActive = false;
    
    // Remove event listeners
    document.removeEventListener('keydown', this.handleKeyDown);
    document.removeEventListener('focusin', this.handleFocusIn);
    document.removeEventListener('click', this.handleOutsideClick);
    
    // Restore focus
    const restoreElement = this.options.restoreFocusElement || this.previousActiveElement;
    if (restoreElement && 'focus' in restoreElement) {
      (restoreElement as HTMLElement).focus();
    }
  }
  
  private handleKeyDown = (event: KeyboardEvent) => {
    if (!this.isActive) return;
    
    if (event.key === 'Escape' && this.options.onEscape) {
      event.preventDefault();
      this.options.onEscape();
      return;
    }
    
    if (event.key === 'Tab') {
      const focusableElements = getFocusableElements(this.container);
      
      if (focusableElements.length === 0) {
        event.preventDefault();
        return;
      }
      
      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];
      
      if (event.shiftKey) {
        // Shift + Tab (backwards)
        if (document.activeElement === firstElement) {
          event.preventDefault();
          lastElement.focus();
        }
      } else {
        // Tab (forwards)
        if (document.activeElement === lastElement) {
          event.preventDefault();
          firstElement.focus();
        }
      }
    }
  }
  
  private handleFocusIn = (event: FocusEvent) => {
    if (!this.isActive) return;
    
    const target = event.target as HTMLElement;
    if (!this.container.contains(target)) {
      // Focus escaped, bring it back
      const firstFocusable = getFirstFocusableElement(this.container);
      if (firstFocusable) {
        firstFocusable.focus();
      }
    }
  }
  
  private handleOutsideClick = (event: MouseEvent) => {
    if (!this.isActive) return;
    
    const target = event.target as HTMLElement;
    if (!this.container.contains(target) && this.options.onEscape) {
      this.options.onEscape();
    }
  }
}

/**
 * Save focus position for later restoration
 */
export class FocusManager {
  private focusStack: Element[] = [];
  
  push(element?: Element | null) {
    const elementToSave = element || document.activeElement;
    if (elementToSave) {
      this.focusStack.push(elementToSave);
    }
  }
  
  pop(): boolean {
    const element = this.focusStack.pop();
    if (element && 'focus' in element && typeof element.focus === 'function') {
      try {
        (element as HTMLElement).focus();
        return true;
      } catch (error) {
        console.warn('Failed to restore focus:', error);
        return false;
      }
    }
    return false;
  }
  
  clear() {
    this.focusStack = [];
  }
  
  peek(): Element | null {
    return this.focusStack.length > 0 ? this.focusStack[this.focusStack.length - 1] : null;
  }
}

/**
 * Global focus manager instance
 */
export const globalFocusManager = new FocusManager();

/**
 * Keyboard navigation utilities
 */
export interface KeyboardNavigationConfig {
  horizontal?: boolean;
  vertical?: boolean;
  wrap?: boolean;
  loop?: boolean;
  onSelect?: (element: HTMLElement) => void;
}

export class KeyboardNavigator {
  private container: HTMLElement;
  private config: KeyboardNavigationConfig;
  private currentIndex = -1;
  private elements: HTMLElement[] = [];
  
  constructor(container: HTMLElement, config: KeyboardNavigationConfig = {}) {
    this.container = container;
    this.config = {
      horizontal: true,
      vertical: true,
      wrap: true,
      loop: true,
      ...config
    };
    
    this.updateElements();
    this.bindEvents();
  }
  
  private updateElements() {
    this.elements = getFocusableElements(this.container);
    this.currentIndex = this.elements.findIndex(el => el === document.activeElement);
  }
  
  private bindEvents() {
    this.container.addEventListener('keydown', this.handleKeyDown);
    this.container.addEventListener('focusin', this.handleFocusIn);
  }
  
  unbind() {
    this.container.removeEventListener('keydown', this.handleKeyDown);
    this.container.removeEventListener('focusin', this.handleFocusIn);
  }
  
  private handleKeyDown = (event: KeyboardEvent) => {
    const { key } = event;
    
    // Update elements list
    this.updateElements();
    
    let handled = false;
    
    if (this.config.horizontal && (key === 'ArrowLeft' || key === 'ArrowRight')) {
      const direction = key === 'ArrowRight' ? 1 : -1;
      this.navigate(direction);
      handled = true;
    }
    
    if (this.config.vertical && (key === 'ArrowUp' || key === 'ArrowDown')) {
      const direction = key === 'ArrowDown' ? 1 : -1;
      this.navigate(direction);
      handled = true;
    }
    
    if (key === 'Home') {
      this.navigateToFirst();
      handled = true;
    }
    
    if (key === 'End') {
      this.navigateToLast();
      handled = true;
    }
    
    if ((key === 'Enter' || key === ' ') && this.config.onSelect) {
      const currentElement = this.elements[this.currentIndex];
      if (currentElement) {
        this.config.onSelect(currentElement);
        handled = true;
      }
    }
    
    if (handled) {
      event.preventDefault();
      event.stopPropagation();
    }
  }
  
  private handleFocusIn = () => {
    this.updateElements();
  }
  
  private navigate(direction: number) {
    if (this.elements.length === 0) return;
    
    let newIndex = this.currentIndex + direction;
    
    if (this.config.wrap || this.config.loop) {
      if (newIndex < 0) {
        newIndex = this.elements.length - 1;
      } else if (newIndex >= this.elements.length) {
        newIndex = 0;
      }
    } else {
      newIndex = Math.max(0, Math.min(newIndex, this.elements.length - 1));
    }
    
    this.focusElement(newIndex);
  }
  
  private navigateToFirst() {
    if (this.elements.length > 0) {
      this.focusElement(0);
    }
  }
  
  private navigateToLast() {
    if (this.elements.length > 0) {
      this.focusElement(this.elements.length - 1);
    }
  }
  
  private focusElement(index: number) {
    if (index >= 0 && index < this.elements.length) {
      this.currentIndex = index;
      this.elements[index].focus();
    }
  }
}

/**
 * Check if an element is currently focused
 */
export function isFocused(element: HTMLElement): boolean {
  return document.activeElement === element;
}

/**
 * Check if focus is currently within a container
 */
export function isFocusWithin(container: HTMLElement): boolean {
  return container.contains(document.activeElement);
}

/**
 * Move focus to the next focusable element
 */
export function focusNext(currentElement?: HTMLElement): boolean {
  const current = currentElement || document.activeElement as HTMLElement;
  if (!current) return false;
  
  const root = document.body;
  const focusableElements = getFocusableElements(root);
  const currentIndex = focusableElements.indexOf(current);
  
  if (currentIndex >= 0 && currentIndex < focusableElements.length - 1) {
    focusableElements[currentIndex + 1].focus();
    return true;
  }
  
  return false;
}

/**
 * Move focus to the previous focusable element
 */
export function focusPrevious(currentElement?: HTMLElement): boolean {
  const current = currentElement || document.activeElement as HTMLElement;
  if (!current) return false;
  
  const root = document.body;
  const focusableElements = getFocusableElements(root);
  const currentIndex = focusableElements.indexOf(current);
  
  if (currentIndex > 0) {
    focusableElements[currentIndex - 1].focus();
    return true;
  }
  
  return false;
}

/**
 * Focus utilities for specific use cases
 */
export const focusUtils = {
  // Save current focus
  saveFocus: () => globalFocusManager.push(),
  
  // Restore saved focus
  restoreFocus: () => globalFocusManager.pop(),
  
  // Focus the first element in a container
  focusFirst: (container: HTMLElement) => {
    const first = getFirstFocusableElement(container);
    if (first) first.focus();
    return !!first;
  },
  
  // Focus the last element in a container
  focusLast: (container: HTMLElement) => {
    const last = getLastFocusableElement(container);
    if (last) last.focus();
    return !!last;
  },
  
  // Check if element can receive focus
  isFocusable: (element: HTMLElement): boolean => {
    return getFocusableElements(document.body).includes(element);
  },
  
  // Focus element with error handling
  safeFocus: (element: HTMLElement | null): boolean => {
    if (!element) return false;
    
    try {
      element.focus();
      return document.activeElement === element;
    } catch (error) {
      console.warn('Failed to focus element:', error);
      return false;
    }
  }
};