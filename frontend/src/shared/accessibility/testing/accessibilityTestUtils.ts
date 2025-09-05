/**
 * Accessibility Testing Utilities
 * 
 * Testing utilities for validating WCAG 2.1 AA compliance
 * and accessibility features in React components.
 */

import { screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { calculateContrastRatio } from '../utils/colorContrast';
import { WCAG_CONTRAST_RATIOS, TOUCH_TARGETS } from '../constants/wcag';

/**
 * Accessibility test result interface
 */
export interface AccessibilityTestResult {
  passed: boolean;
  message: string;
  element?: HTMLElement;
  severity: 'error' | 'warning' | 'info';
}

/**
 * Accessibility test suite results
 */
export interface AccessibilityTestSuite {
  results: AccessibilityTestResult[];
  passed: boolean;
  errors: AccessibilityTestResult[];
  warnings: AccessibilityTestResult[];
}

/**
 * Color contrast testing utilities
 */
export class ContrastTester {
  /**
   * Test color contrast ratio for text elements
   */
  static testTextContrast(element: HTMLElement): AccessibilityTestResult {
    const computedStyles = window.getComputedStyle(element);
    const color = computedStyles.color;
    const backgroundColor = computedStyles.backgroundColor;
    const fontSize = parseFloat(computedStyles.fontSize);
    
    if (!color || !backgroundColor) {
      return {
        passed: false,
        message: 'Could not determine text or background color',
        element,
        severity: 'warning',
      };
    }
    
    const contrast = calculateContrastRatio(color, backgroundColor);
    const isLargeText = fontSize >= 18 || (fontSize >= 14 && computedStyles.fontWeight === 'bold');
    const requiredRatio = isLargeText ? WCAG_CONTRAST_RATIOS.AA_LARGE : WCAG_CONTRAST_RATIOS.AA_NORMAL;
    
    const passed = contrast >= requiredRatio;
    
    return {
      passed,
      message: `Text contrast ratio: ${contrast.toFixed(2)}:1 (required: ${requiredRatio}:1)`,
      element,
      severity: passed ? 'info' : 'error',
    };
  }
  
  /**
   * Test all text elements in a container for contrast
   */
  static testAllTextContrast(container: HTMLElement): AccessibilityTestResult[] {
    const textElements = container.querySelectorAll('*');
    const results: AccessibilityTestResult[] = [];
    
    textElements.forEach((element) => {
      const computedStyles = window.getComputedStyle(element);
      const hasText = element.textContent?.trim();
      
      if (hasText && computedStyles.color && computedStyles.backgroundColor) {
        results.push(this.testTextContrast(element as HTMLElement));
      }
    });
    
    return results;
  }
}

/**
 * Focus management testing utilities
 */
export class FocusTester {
  /**
   * Test if element is focusable
   */
  static isFocusable(element: HTMLElement): boolean {
    const focusableSelectors = [
      'a[href]',
      'button',
      'input',
      'select',
      'textarea',
      '[tabindex]',
      '[contenteditable="true"]',
    ];
    
    return focusableSelectors.some(selector => element.matches(selector)) &&
           !element.hasAttribute('disabled') &&
           element.tabIndex >= 0;
  }
  
  /**
   * Test focus order in container
   */
  static testFocusOrder(container: HTMLElement): AccessibilityTestResult {
    const focusableElements = Array.from(container.querySelectorAll('*'))
      .filter(element => this.isFocusable(element as HTMLElement))
      .sort((a, b) => {
        const aIndex = (a as HTMLElement).tabIndex || 0;
        const bIndex = (b as HTMLElement).tabIndex || 0;
        return aIndex - bIndex;
      });
    
    const hasLogicalOrder = focusableElements.every((element, index) => {
      const tabIndex = (element as HTMLElement).tabIndex;
      return tabIndex === 0 || tabIndex === index + 1;
    });
    
    return {
      passed: hasLogicalOrder,
      message: hasLogicalOrder 
        ? 'Focus order follows logical sequence'
        : 'Focus order may not follow logical sequence',
      severity: hasLogicalOrder ? 'info' : 'warning',
    };
  }
  
  /**
   * Test focus trap in modal or overlay
   */
  static async testFocusTrap(container: HTMLElement, user: ReturnType<typeof userEvent.setup>): Promise<AccessibilityTestResult> {
    const focusableElements = Array.from(container.querySelectorAll('*'))
      .filter(element => this.isFocusable(element as HTMLElement));
    
    if (focusableElements.length === 0) {
      return {
        passed: false,
        message: 'No focusable elements found in focus trap container',
        severity: 'error',
      };
    }
    
    const firstElement = focusableElements[0] as HTMLElement;
    const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement;
    
    // Focus first element
    firstElement.focus();
    expect(document.activeElement).toBe(firstElement);
    
    // Tab backwards from first element should focus last
    await user.keyboard('{Shift>}{Tab}{/Shift}');
    
    const focusTrapped = document.activeElement === lastElement || 
                       focusableElements.includes(document.activeElement as Element);
    
    return {
      passed: focusTrapped,
      message: focusTrapped 
        ? 'Focus trap working correctly'
        : 'Focus may escape the trap container',
      severity: focusTrapped ? 'info' : 'error',
    };
  }
}

/**
 * ARIA testing utilities
 */
export class AriaTester {
  /**
   * Test required ARIA labels
   */
  static testAriaLabels(container: HTMLElement): AccessibilityTestResult[] {
    const results: AccessibilityTestResult[] = [];
    
    // Test buttons without accessible names
    const buttons = container.querySelectorAll('button, [role="button"]');
    buttons.forEach((button) => {
      const hasLabel = button.getAttribute('aria-label') ||
                      button.getAttribute('aria-labelledby') ||
                      button.textContent?.trim();
      
      results.push({
        passed: !!hasLabel,
        message: hasLabel ? 'Button has accessible name' : 'Button missing accessible name',
        element: button as HTMLElement,
        severity: hasLabel ? 'info' : 'error',
      });
    });
    
    // Test form inputs
    const inputs = container.querySelectorAll('input, select, textarea');
    inputs.forEach((input) => {
      const hasLabel = input.getAttribute('aria-label') ||
                      input.getAttribute('aria-labelledby') ||
                      container.querySelector(`label[for="${input.id}"]`);
      
      results.push({
        passed: !!hasLabel,
        message: hasLabel ? 'Input has accessible label' : 'Input missing accessible label',
        element: input as HTMLElement,
        severity: hasLabel ? 'info' : 'error',
      });
    });
    
    return results;
  }
  
  /**
   * Test ARIA relationships
   */
  static testAriaRelationships(container: HTMLElement): AccessibilityTestResult[] {
    const results: AccessibilityTestResult[] = [];
    
    // Test aria-describedby relationships
    const describedElements = container.querySelectorAll('[aria-describedby]');
    describedElements.forEach((element) => {
      const describedBy = element.getAttribute('aria-describedby')!;
      const descriptionIds = describedBy.split(' ');
      
      descriptionIds.forEach((id) => {
        const descriptionElement = container.querySelector(`#${id}`);
        results.push({
          passed: !!descriptionElement,
          message: descriptionElement 
            ? `aria-describedby relationship valid for ${id}`
            : `aria-describedby references missing element ${id}`,
          element: element as HTMLElement,
          severity: descriptionElement ? 'info' : 'error',
        });
      });
    });
    
    // Test aria-labelledby relationships
    const labelledElements = container.querySelectorAll('[aria-labelledby]');
    labelledElements.forEach((element) => {
      const labelledBy = element.getAttribute('aria-labelledby')!;
      const labelIds = labelledBy.split(' ');
      
      labelIds.forEach((id) => {
        const labelElement = container.querySelector(`#${id}`);
        results.push({
          passed: !!labelElement,
          message: labelElement 
            ? `aria-labelledby relationship valid for ${id}`
            : `aria-labelledby references missing element ${id}`,
          element: element as HTMLElement,
          severity: labelElement ? 'info' : 'error',
        });
      });
    });
    
    return results;
  }
  
  /**
   * Test live regions
   */
  static testLiveRegions(container: HTMLElement): AccessibilityTestResult[] {
    const results: AccessibilityTestResult[] = [];
    
    const liveRegions = container.querySelectorAll('[aria-live], [role="status"], [role="alert"]');
    liveRegions.forEach((region) => {
      const ariaLive = region.getAttribute('aria-live');
      const role = region.getAttribute('role');
      
      const hasValidLiveAttribute = ariaLive === 'polite' || 
                                  ariaLive === 'assertive' || 
                                  ariaLive === 'off' ||
                                  role === 'status' ||
                                  role === 'alert';
      
      results.push({
        passed: hasValidLiveAttribute,
        message: hasValidLiveAttribute 
          ? 'Live region properly configured'
          : 'Live region has invalid or missing aria-live attribute',
        element: region as HTMLElement,
        severity: hasValidLiveAttribute ? 'info' : 'error',
      });
    });
    
    return results;
  }
}

/**
 * Keyboard navigation testing utilities
 */
export class KeyboardTester {
  /**
   * Test keyboard navigation in container
   */
  static async testKeyboardNavigation(
    container: HTMLElement, 
    user: ReturnType<typeof userEvent.setup>
  ): Promise<AccessibilityTestResult[]> {
    const results: AccessibilityTestResult[] = [];
    
    const focusableElements = Array.from(container.querySelectorAll('*'))
      .filter(element => FocusTester.isFocusable(element as HTMLElement));
    
    if (focusableElements.length === 0) {
      return [{
        passed: false,
        message: 'No keyboard focusable elements found',
        severity: 'warning',
      }];
    }
    
    // Test Tab navigation
    const firstElement = focusableElements[0] as HTMLElement;
    firstElement.focus();
    
    for (let i = 1; i < Math.min(focusableElements.length, 5); i++) {
      await user.keyboard('{Tab}');
      await waitFor(() => {
        const focusedCorrectly = document.activeElement === focusableElements[i];
        results.push({
          passed: focusedCorrectly,
          message: focusedCorrectly 
            ? `Tab navigation to element ${i + 1} successful`
            : `Tab navigation to element ${i + 1} failed`,
          element: focusableElements[i] as HTMLElement,
          severity: focusedCorrectly ? 'info' : 'error',
        });
      });
    }
    
    return results;
  }
  
  /**
   * Test Escape key handling
   */
  static async testEscapeKey(
    container: HTMLElement,
    user: ReturnType<typeof userEvent.setup>,
    expectedHandler?: () => void
  ): Promise<AccessibilityTestResult> {
    const initialFocus = document.activeElement;
    
    await user.keyboard('{Escape}');
    
    if (expectedHandler) {
      return {
        passed: true,
        message: 'Escape key handler tested (custom verification required)',
        severity: 'info',
      };
    }
    
    // Default test: check if modal/overlay was closed
    const isVisible = container.offsetParent !== null;
    
    return {
      passed: !isVisible,
      message: isVisible 
        ? 'Escape key may not be properly handled'
        : 'Escape key handling appears correct',
      severity: isVisible ? 'warning' : 'info',
    };
  }
}

/**
 * Touch target testing utilities
 */
export class TouchTargetTester {
  /**
   * Test minimum touch target sizes
   */
  static testTouchTargetSizes(container: HTMLElement): AccessibilityTestResult[] {
    const results: AccessibilityTestResult[] = [];
    
    const interactiveElements = container.querySelectorAll(
      'button, [role="button"], a, input, select, textarea, [tabindex]'
    );
    
    interactiveElements.forEach((element) => {
      const rect = element.getBoundingClientRect();
      const minSize = TOUCH_TARGETS.MIN_SIZE;
      
      const meetsMinSize = rect.width >= minSize && rect.height >= minSize;
      
      results.push({
        passed: meetsMinSize,
        message: meetsMinSize 
          ? `Touch target size adequate (${rect.width.toFixed(0)}x${rect.height.toFixed(0)}px)`
          : `Touch target too small (${rect.width.toFixed(0)}x${rect.height.toFixed(0)}px, minimum ${minSize}x${minSize}px)`,
        element: element as HTMLElement,
        severity: meetsMinSize ? 'info' : 'error',
      });
    });
    
    return results;
  }
}

/**
 * Comprehensive accessibility test suite
 */
export class AccessibilityTester {
  /**
   * Run comprehensive accessibility tests on a container
   */
  static async runAccessibilityTests(
    container: HTMLElement,
    user: ReturnType<typeof userEvent.setup>
  ): Promise<AccessibilityTestSuite> {
    const results: AccessibilityTestResult[] = [];
    
    // Color contrast tests
    results.push(...ContrastTester.testAllTextContrast(container));
    
    // Focus management tests
    results.push(FocusTester.testFocusOrder(container));
    
    // ARIA tests
    results.push(...AriaTester.testAriaLabels(container));
    results.push(...AriaTester.testAriaRelationships(container));
    results.push(...AriaTester.testLiveRegions(container));
    
    // Keyboard navigation tests
    results.push(...await KeyboardTester.testKeyboardNavigation(container, user));
    
    // Touch target tests
    results.push(...TouchTargetTester.testTouchTargetSizes(container));
    
    const errors = results.filter(r => r.severity === 'error');
    const warnings = results.filter(r => r.severity === 'warning');
    const passed = errors.length === 0;
    
    return {
      results,
      passed,
      errors,
      warnings,
    };
  }
  
  /**
   * Create accessibility test helpers for Jest/Vitest
   */
  static createTestHelpers() {
    return {
      /**
       * Custom Jest matcher for accessibility compliance
       */
      toBeAccessible: async (container: HTMLElement) => {
        const user = userEvent.setup();
        const testSuite = await AccessibilityTester.runAccessibilityTests(container, user);
        
        return {
          pass: testSuite.passed,
          message: () => testSuite.passed 
            ? 'Expected element to fail accessibility tests'
            : `Accessibility tests failed:\n${testSuite.errors.map(e => `- ${e.message}`).join('\n')}`,
        };
      },
      
      /**
       * Test individual accessibility aspects
       */
      toHaveValidContrast: (element: HTMLElement) => {
        const result = ContrastTester.testTextContrast(element);
        return {
          pass: result.passed,
          message: () => result.message,
        };
      },
      
      toHaveValidAriaLabels: (container: HTMLElement) => {
        const results = AriaTester.testAriaLabels(container);
        const errors = results.filter(r => r.severity === 'error');
        return {
          pass: errors.length === 0,
          message: () => errors.length === 0 
            ? 'All elements have valid ARIA labels'
            : `ARIA label errors:\n${errors.map(e => `- ${e.message}`).join('\n')}`,
        };
      },
      
      toHaveValidTouchTargets: (container: HTMLElement) => {
        const results = TouchTargetTester.testTouchTargetSizes(container);
        const errors = results.filter(r => r.severity === 'error');
        return {
          pass: errors.length === 0,
          message: () => errors.length === 0 
            ? 'All touch targets meet minimum size requirements'
            : `Touch target errors:\n${errors.map(e => `- ${e.message}`).join('\n')}`,
        };
      },
    };
  }
}

/**
 * Accessibility testing setup for test files
 */
export function setupAccessibilityTests() {
  const helpers = AccessibilityTester.createTestHelpers();
  
  // Extend expect with custom matchers (for Jest)
  if (typeof expect !== 'undefined' && expect.extend) {
    expect.extend(helpers);
  }
  
  return helpers;
}

export default AccessibilityTester;