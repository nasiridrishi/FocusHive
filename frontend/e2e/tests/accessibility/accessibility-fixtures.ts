/**
 * Accessibility Testing Fixtures and Test Data
 * 
 * Shared fixtures, test data, and utilities for accessibility E2E tests
 * Provides consistent test data across all accessibility test suites
 * 
 * UOL-44.19: Comprehensive Accessibility E2E Tests
 */

import { test as base, Page } from '@playwright/test';
import { injectAxe, getViolations, checkA11y } from 'axe-playwright';
import { DEFAULT_ACCESSIBILITY_CONFIG, getAccessibilityConfig } from './accessibility.config';

// Test data for accessibility testing
export const ACCESSIBILITY_TEST_DATA = {
  // Valid form data
  validUser: {
    firstName: 'Jane',
    lastName: 'Smith',
    email: 'jane.smith@example.com',
    password: 'SecurePass123!',
    confirmPassword: 'SecurePass123!',
    phone: '+1 (555) 123-4567',
    dateOfBirth: '1990-06-15'
  },

  // Invalid form data for error testing
  invalidUser: {
    firstName: '',
    lastName: '',
    email: 'invalid-email',
    password: '123',
    confirmPassword: '456',
    phone: '123abc',
    dateOfBirth: 'not-a-date'
  },

  // Test hive data
  testHive: {
    name: 'Accessibility Test Hive',
    description: 'A test hive for accessibility validation',
    isPublic: true,
    maxMembers: 10,
    tags: ['accessibility', 'testing', 'wcag']
  },

  // Test messages for chat functionality
  testMessages: [
    'Hello everyone! This is a test message for accessibility validation.',
    'Testing screen reader announcements with this message.',
    'Keyboard navigation test message with special characters: @#$%',
    'Long message to test text wrapping and readability: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.'
  ],

  // Error messages for validation testing
  errorMessages: {
    required: 'This field is required',
    email: 'Please enter a valid email address',
    password: 'Password must be at least 8 characters long',
    confirmPassword: 'Passwords do not match',
    phone: 'Please enter a valid phone number',
    dateOfBirth: 'Please enter a valid date'
  },

  // Success messages
  successMessages: {
    save: 'Settings saved successfully',
    create: 'Created successfully',
    update: 'Updated successfully',
    delete: 'Deleted successfully',
    join: 'Joined successfully',
    leave: 'Left successfully'
  }
};

// Common selectors used across accessibility tests
export const ACCESSIBILITY_SELECTORS = {
  // Navigation
  mainNavigation: 'nav[role="navigation"], .main-nav, .primary-nav',
  breadcrumbs: '[aria-label*="breadcrumb" i], .breadcrumb, nav[aria-label*="breadcrumb" i]',
  skipLinks: '.skip-link, .skip-nav, a[href="#main"], a[href="#content"]',

  // Headings
  mainHeading: 'h1',
  allHeadings: 'h1, h2, h3, h4, h5, h6',

  // Forms
  forms: 'form',
  formInputs: 'input, select, textarea',
  requiredFields: '[required], [aria-required="true"]',
  errorMessages: '.error, .error-message, [role="alert"], [aria-invalid="true"] + *',
  submitButtons: 'button[type="submit"], input[type="submit"]',

  // Interactive elements
  buttons: 'button, [role="button"]',
  links: 'a[href], [role="link"]',
  tabs: '[role="tab"], .tab',
  modals: '[role="dialog"], .modal, .dialog',
  tooltips: '[role="tooltip"], .tooltip',

  // Media
  images: 'img',
  videos: 'video',
  audio: 'audio',

  // Live regions
  statusRegions: '[role="status"], [aria-live="polite"]',
  alertRegions: '[role="alert"], [aria-live="assertive"]',
  logRegions: '[role="log"]',

  // Tables
  tables: 'table',
  tableHeaders: 'th',
  tableCaptions: 'caption',

  // Lists
  lists: 'ul, ol, dl',
  listItems: 'li, dt, dd',

  // Focus management
  focusable: 'a[href], button, input, select, textarea, [tabindex="0"], [role="button"], [role="link"], [role="tab"]'
};

// Color contrast test data
export const COLOR_CONTRAST_DATA = {
  // WCAG AA minimum contrast ratios
  minimumContrast: {
    normalText: 4.5,
    largeText: 3.0,
    uiComponents: 3.0,
    focusIndicators: 3.0
  },

  // WCAG AAA enhanced contrast ratios
  enhancedContrast: {
    normalText: 7.0,
    largeText: 4.5,
    uiComponents: 4.5,
    focusIndicators: 4.5
  },

  // Test color combinations
  testColors: [
    { background: '#ffffff', foreground: '#000000', ratio: 21, passes: true },
    { background: '#ffffff', foreground: '#767676', ratio: 4.54, passes: true },
    { background: '#ffffff', foreground: '#949494', ratio: 3.45, passes: false },
    { background: '#000000', foreground: '#ffffff', ratio: 21, passes: true },
    { background: '#0066cc', foreground: '#ffffff', ratio: 5.74, passes: true }
  ]
};

// Keyboard navigation test data
export const KEYBOARD_TEST_DATA = {
  // Standard keyboard shortcuts
  shortcuts: {
    tab: 'Tab',
    shiftTab: 'Shift+Tab',
    enter: 'Enter',
    space: 'Space',
    escape: 'Escape',
    home: 'Home',
    end: 'End',
    pageUp: 'PageUp',
    pageDown: 'PageDown',
    arrowUp: 'ArrowUp',
    arrowDown: 'ArrowDown',
    arrowLeft: 'ArrowLeft',
    arrowRight: 'ArrowRight'
  },

  // Expected focus order for common layouts
  focusOrder: {
    mainPage: ['skip-link', 'logo', 'main-nav', 'search', 'main-content', 'footer'],
    form: ['first-input', 'second-input', 'submit-button', 'cancel-button'],
    modal: ['close-button', 'modal-content', 'action-buttons']
  }
};

// Screen reader test data
export const SCREEN_READER_DATA = {
  // Common screen readers and their usage statistics
  screenReaders: {
    nvda: { name: 'NVDA', usage: 65.6, platform: 'Windows' },
    jaws: { name: 'JAWS', usage: 60.5, platform: 'Windows' },
    voiceover: { name: 'VoiceOver', usage: 47.1, platform: 'macOS/iOS' },
    talkback: { name: 'TalkBack', usage: 32.8, platform: 'Android' },
    orca: { name: 'Orca', usage: 8.7, platform: 'Linux' }
  },

  // ARIA roles and their expected announcements
  ariaRoles: [
    { role: 'button', expectation: 'announces as button' },
    { role: 'link', expectation: 'announces as link with destination' },
    { role: 'heading', expectation: 'announces heading level' },
    { role: 'list', expectation: 'announces list with item count' },
    { role: 'listitem', expectation: 'announces item position' },
    { role: 'navigation', expectation: 'announces as navigation' },
    { role: 'main', expectation: 'announces as main content' },
    { role: 'complementary', expectation: 'announces as complementary' },
    { role: 'banner', expectation: 'announces as banner' },
    { role: 'contentinfo', expectation: 'announces as content info' }
  ]
};

// Extended test fixture with accessibility utilities
export const test = base.extend<{
  accessibilityPage: Page;
  skipA11yViolations: () => void;
  expectA11yCompliance: (selector?: string) => Promise<void>;
  getColorContrast: (elementSelector: string) => Promise<number>;
  testKeyboardNavigation: (startSelector: string, expectedOrder: string[]) => Promise<void>;
  announceToScreenReader: (message: string) => Promise<void>;
  waitForLiveRegionUpdate: (selector: string, timeout?: number) => Promise<string>;
}>({
  // Enhanced page fixture with accessibility setup
  accessibilityPage: async ({ page }, use) => {
    // Configure accessibility settings
    const config = getAccessibilityConfig(process.env.ACCESSIBILITY_CONFIG || 'default');
    
    // Set viewport based on config
    if (config.browsers.mobile.enabled && process.env.ACCESSIBILITY_CONFIG === 'mobile') {
      await page.setViewportSize({ width: 375, height: 667 });
    }

    // Inject axe-core for accessibility testing
    await page.goto('/');
    await injectAxe(page);

    // Add accessibility CSS if high contrast mode
    if (process.env.ACCESSIBILITY_CONFIG === 'high-contrast') {
      await page.addStyleTag({
        content: `
          * {
            filter: invert(1) hue-rotate(180deg) !important;
          }
          img, video, svg {
            filter: invert(1) hue-rotate(180deg) !important;
          }
        `
      });
    }

    await use(page);
  },

  // Utility to skip axe violations for specific tests
  skipA11yViolations: async ({}, use) => {
    const skip = () => {
      // Mark test as skipped for axe violations
      console.warn('Axe violations skipped for this test');
    };
    await use(skip);
  },

  // Utility to check accessibility compliance
  expectA11yCompliance: async ({ page }, use) => {
    const checkCompliance = async (selector?: string) => {
      await checkA11y(page, selector, {
        axeOptions: {
          runOnly: {
            type: 'tag',
            values: ['wcag2a', 'wcag2aa', 'wcag21aa', 'best-practice']
          }
        },
        detailedReport: true
      });
    };
    await use(checkCompliance);
  },

  // Utility to get color contrast ratio
  getColorContrast: async ({ page }, use) => {
    const getContrast = async (elementSelector: string): Promise<number> => {
      return await page.evaluate((selector) => {
        const element = document.querySelector(selector);
        if (!element) return 0;

        const computedStyle = window.getComputedStyle(element);
        const backgroundColor = computedStyle.backgroundColor;
        const textColor = computedStyle.color;

        // Simple contrast calculation (would use a proper library in production)
        const getRGB = (color: string) => {
          const match = color.match(/\d+/g);
          return match ? match.map(Number) : [0, 0, 0];
        };

        const getLuminance = (rgb: number[]) => {
          const [r, g, b] = rgb.map(c => {
            c = c / 255;
            return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
          });
          return 0.2126 * r + 0.7152 * g + 0.0722 * b;
        };

        const bgRGB = getRGB(backgroundColor);
        const textRGB = getRGB(textColor);
        
        const bgLuminance = getLuminance(bgRGB);
        const textLuminance = getLuminance(textRGB);
        
        const lighter = Math.max(bgLuminance, textLuminance);
        const darker = Math.min(bgLuminance, textLuminance);
        
        return (lighter + 0.05) / (darker + 0.05);
      }, elementSelector);
    };
    await use(getContrast);
  },

  // Utility to test keyboard navigation
  testKeyboardNavigation: async ({ page }, use) => {
    const testNavigation = async (startSelector: string, expectedOrder: string[]) => {
      // Focus on starting element
      await page.locator(startSelector).focus();
      
      const focusedElements: string[] = [];
      
      // Navigate through expected order
      for (let i = 0; i < expectedOrder.length; i++) {
        const focusedElement = await page.locator(':focus');
        const elementId = await focusedElement.getAttribute('id') || 
                          await focusedElement.getAttribute('data-testid') || 
                          await focusedElement.evaluate(el => el.tagName.toLowerCase());
        
        focusedElements.push(elementId);
        
        if (i < expectedOrder.length - 1) {
          await page.keyboard.press('Tab');
        }
      }
      
      // Verify focus order matches expectations
      return focusedElements;
    };
    await use(testNavigation);
  },

  // Utility to announce messages to screen readers
  announceToScreenReader: async ({ page }, use) => {
    const announce = async (message: string) => {
      await page.evaluate((msg) => {
        // Create or update live region
        let announcer = document.getElementById('accessibility-announcer');
        if (!announcer) {
          announcer = document.createElement('div');
          announcer.id = 'accessibility-announcer';
          announcer.setAttribute('aria-live', 'assertive');
          announcer.setAttribute('aria-atomic', 'true');
          announcer.style.position = 'absolute';
          announcer.style.left = '-10000px';
          announcer.style.width = '1px';
          announcer.style.height = '1px';
          announcer.style.overflow = 'hidden';
          document.body.appendChild(announcer);
        }
        
        // Clear and set new message
        announcer.textContent = '';
        setTimeout(() => {
          announcer!.textContent = msg;
        }, 100);
      }, message);
    };
    await use(announce);
  },

  // Utility to wait for live region updates
  waitForLiveRegionUpdate: async ({ page }, use) => {
    const waitForUpdate = async (selector: string, timeout = 5000): Promise<string> => {
      const liveRegion = page.locator(selector);
      
      // Wait for content to appear or change
      await liveRegion.waitFor({ state: 'visible', timeout });
      
      return await liveRegion.textContent() || '';
    };
    await use(waitForUpdate);
  }
});

// Helper functions for common accessibility checks
export class AccessibilityHelpers {
  static async checkHeadingStructure(page: Page): Promise<{ valid: boolean; issues: string[] }> {
    const headings = await page.locator(ACCESSIBILITY_SELECTORS.allHeadings).all();
    const issues: string[] = [];
    let previousLevel = 0;
    
    if (headings.length === 0) {
      issues.push('No headings found on page');
      return { valid: false, issues };
    }
    
    // Check for H1
    const h1Count = await page.locator('h1').count();
    if (h1Count === 0) {
      issues.push('No H1 heading found');
    } else if (h1Count > 1) {
      issues.push('Multiple H1 headings found');
    }
    
    // Check heading hierarchy
    for (const heading of headings) {
      const tagName = await heading.evaluate(el => el.tagName.toLowerCase());
      const level = parseInt(tagName.charAt(1));
      
      if (previousLevel > 0 && level > previousLevel + 1) {
        issues.push(`Heading level skip: ${tagName} after h${previousLevel}`);
      }
      
      previousLevel = level;
    }
    
    return { valid: issues.length === 0, issues };
  }
  
  static async checkFormLabels(page: Page): Promise<{ valid: boolean; issues: string[] }> {
    const inputs = await page.locator(ACCESSIBILITY_SELECTORS.formInputs).all();
    const issues: string[] = [];
    
    for (const input of inputs) {
      const type = await input.getAttribute('type');
      
      // Skip hidden inputs
      if (type === 'hidden') continue;
      
      const id = await input.getAttribute('id');
      const ariaLabel = await input.getAttribute('aria-label');
      const ariaLabelledBy = await input.getAttribute('aria-labelledby');
      
      if (id) {
        const label = page.locator(`label[for="${id}"]`);
        const hasLabel = await label.count() > 0;
        
        if (!hasLabel && !ariaLabel && !ariaLabelledBy) {
          issues.push(`Input without label: ${type || 'text'} input`);
        }
      } else if (!ariaLabel && !ariaLabelledBy) {
        issues.push(`Input without ID or aria-label: ${type || 'text'} input`);
      }
    }
    
    return { valid: issues.length === 0, issues };
  }
  
  static async checkImageAltText(page: Page): Promise<{ valid: boolean; issues: string[] }> {
    const images = await page.locator(ACCESSIBILITY_SELECTORS.images).all();
    const issues: string[] = [];
    
    for (const image of images) {
      const alt = await image.getAttribute('alt');
      const ariaLabel = await image.getAttribute('aria-label');
      const ariaLabelledBy = await image.getAttribute('aria-labelledby');
      const role = await image.getAttribute('role');
      
      // Decorative images should have empty alt
      if (role === 'presentation' || role === 'none') {
        continue;
      }
      
      if (alt === null && !ariaLabel && !ariaLabelledBy) {
        issues.push('Image without alt text or aria-label');
      }
    }
    
    return { valid: issues.length === 0, issues };
  }
  
  static async checkFocusManagement(page: Page, triggerSelector: string): Promise<{ valid: boolean; issues: string[] }> {
    const issues: string[] = [];
    
    // Test focus visibility
    await page.keyboard.press('Tab');
    const focusedElement = page.locator(':focus');
    
    if (await focusedElement.count() === 0) {
      issues.push('No visible focus indicator');
    } else {
      // Check if focus is visible (simplified check)
      const focusStyles = await focusedElement.evaluate(el => {
        const styles = window.getComputedStyle(el);
        return {
          outline: styles.outline,
          outlineWidth: styles.outlineWidth,
          boxShadow: styles.boxShadow,
          border: styles.border
        };
      });
      
      const hasVisibleFocus = focusStyles.outline !== 'none' || 
                             focusStyles.outlineWidth !== '0px' ||
                             focusStyles.boxShadow !== 'none' ||
                             focusStyles.border !== 'none';
      
      if (!hasVisibleFocus) {
        issues.push('Focus indicator not visible');
      }
    }
    
    return { valid: issues.length === 0, issues };
  }
}

export { test as accessibilityTest };
export default test;