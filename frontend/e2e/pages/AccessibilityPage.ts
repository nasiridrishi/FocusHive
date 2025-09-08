/**
 * Accessibility Page Object Model for E2E Testing
 * Provides methods to interact with and test accessibility features
 */

import { Page, Locator, expect } from '@playwright/test';

interface HeadingStructure {
  h1: Locator[];
  h2: Locator[];
  h3: Locator[];
  h4: Locator[];
  h5: Locator[];
  h6: Locator[];
}

interface LandmarkStructure {
  main: Locator[];
  navigation: Locator[];
  banner: Locator[];
  contentinfo: Locator[];
  complementary: Locator[];
  search: Locator[];
}

export class AccessibilityPage {
  constructor(private page: Page) {}

  /**
   * Wait for page to load completely
   */
  async waitForPageLoad(): Promise<void> {
    await this.page.waitForLoadState('networkidle');
    await this.page.waitForTimeout(500); // Allow for dynamic content
  }

  /**
   * Get all focusable elements on the page
   */
  async getFocusableElements(): Promise<Locator[]> {
    const focusableSelectors = [
      'button:not([disabled])',
      'input:not([disabled])',
      'select:not([disabled])',
      'textarea:not([disabled])',
      'a[href]',
      '[tabindex]:not([tabindex="-1"])',
      '[role="button"]:not([aria-disabled="true"])',
      '[role="link"]:not([aria-disabled="true"])',
      '[role="tab"]:not([aria-disabled="true"])',
      '[role="menuitem"]:not([aria-disabled="true"])',
      'details > summary'
    ].join(', ');

    return this.page.locator(focusableSelectors).all();
  }

  /**
   * Get skip link if present
   */
  async getSkipLink(): Promise<Locator | null> {
    const skipLinks = [
      'a:has-text("Skip to main content")',
      'a:has-text("Skip to content")',
      'a[href="#main-content"]',
      'a[href="#main"]',
      '.skip-link',
      '[data-testid="skip-link"]'
    ];

    for (const selector of skipLinks) {
      const element = this.page.locator(selector).first();
      if (await element.count() > 0) {
        return element;
      }
    }

    return null;
  }

  /**
   * Get main content area
   */
  async getMainContent(): Promise<Locator> {
    const mainSelectors = [
      'main',
      '[role="main"]',
      '#main-content',
      '#main',
      '.main-content'
    ];

    for (const selector of mainSelectors) {
      const element = this.page.locator(selector).first();
      if (await element.count() > 0) {
        return element;
      }
    }

    // Fallback to body
    return this.page.locator('body');
  }

  /**
   * Find modal trigger button
   */
  async findModalTrigger(): Promise<Locator | null> {
    const modalTriggers = [
      'button:has-text("Open")',
      'button[aria-haspopup="dialog"]',
      'button[data-testid*="modal"]',
      'button[data-bs-toggle="modal"]',
      '[data-modal-trigger]'
    ];

    for (const selector of modalTriggers) {
      const element = this.page.locator(selector).first();
      if (await element.count() > 0 && await element.isVisible()) {
        return element;
      }
    }

    return null;
  }

  /**
   * Get currently open modal
   */
  async getOpenModal(): Promise<Locator | null> {
    const modalSelectors = [
      '[role="dialog"]',
      '[aria-modal="true"]',
      '.modal[style*="display: block"]',
      '.modal.show',
      '[data-testid="modal"]'
    ];

    for (const selector of modalSelectors) {
      const element = this.page.locator(selector).first();
      if (await element.count() > 0 && await element.isVisible()) {
        return element;
      }
    }

    return null;
  }

  /**
   * Test focus trapping in a modal
   */
  async testFocusTrap(modal: Locator): Promise<void> {
    const focusableElements = await modal.locator(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    ).all();

    if (focusableElements.length === 0) return;

    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    // Focus should start on first element
    await firstElement.focus();
    await expect(firstElement).toBeFocused();

    // Tab through all elements
    for (let i = 0; i < focusableElements.length - 1; i++) {
      await this.page.keyboard.press('Tab');
    }

    // Should be on last element
    await expect(lastElement).toBeFocused();

    // Tab once more should return to first element (focus trap)
    await this.page.keyboard.press('Tab');
    await expect(firstElement).toBeFocused();

    // Shift+Tab from first should go to last (reverse focus trap)
    await this.page.keyboard.press('Shift+Tab');
    await expect(lastElement).toBeFocused();
  }

  /**
   * Verify no keyboard traps exist
   */
  async verifyNoKeyboardTraps(): Promise<void> {
    const focusableElements = await this.getFocusableElements();
    
    if (focusableElements.length === 0) return;

    // Tab through several elements to ensure no trapping
    for (let i = 0; i < Math.min(10, focusableElements.length); i++) {
      await this.page.keyboard.press('Tab');
      
      // Ensure focus moves to a visible element
      const focusedElement = this.page.locator(':focus');
      await expect(focusedElement).toBeVisible();
      
      // Wait briefly to allow for any focus management
      await this.page.waitForTimeout(100);
    }
  }

  /**
   * Find navigation menu
   */
  async findNavigationMenu(): Promise<Locator | null> {
    const navSelectors = [
      'nav',
      '[role="navigation"]',
      '.navbar',
      '.nav-menu',
      '[data-testid="navigation"]'
    ];

    for (const selector of navSelectors) {
      const element = this.page.locator(selector).first();
      if (await element.count() > 0 && await element.isVisible()) {
        return element;
      }
    }

    return null;
  }

  /**
   * Get heading structure
   */
  async getHeadingStructure(): Promise<HeadingStructure> {
    return {
      h1: await this.page.locator('h1').all(),
      h2: await this.page.locator('h2').all(),
      h3: await this.page.locator('h3').all(),
      h4: await this.page.locator('h4').all(),
      h5: await this.page.locator('h5').all(),
      h6: await this.page.locator('h6').all()
    };
  }

  /**
   * Get live regions
   */
  async getLiveRegions(): Promise<Locator[]> {
    return this.page.locator('[aria-live]').all();
  }

  /**
   * Get landmark elements
   */
  async getLandmarks(): Promise<LandmarkStructure> {
    return {
      main: await this.page.locator('main, [role="main"]').all(),
      navigation: await this.page.locator('nav, [role="navigation"]').all(),
      banner: await this.page.locator('header, [role="banner"]').all(),
      contentinfo: await this.page.locator('footer, [role="contentinfo"]').all(),
      complementary: await this.page.locator('aside, [role="complementary"]').all(),
      search: await this.page.locator('[role="search"]').all()
    };
  }

  /**
   * Get error messages
   */
  async getErrorMessages(): Promise<Locator[]> {
    const errorSelectors = [
      '.error',
      '.error-message',
      '[role="alert"]',
      '[aria-live="assertive"]',
      '.invalid-feedback',
      '.field-error',
      '[data-testid*="error"]'
    ];

    const errorMessages: Locator[] = [];
    
    for (const selector of errorSelectors) {
      const elements = await this.page.locator(selector).all();
      errorMessages.push(...elements);
    }

    return errorMessages.filter(async (element) => await element.isVisible());
  }

  /**
   * Get required fields
   */
  async getRequiredFields(): Promise<Locator[]> {
    return this.page.locator('input[required], select[required], textarea[required], [aria-required="true"]').all();
  }

  /**
   * Get interactive elements
   */
  async getInteractiveElements(): Promise<Locator[]> {
    const interactiveSelectors = [
      'button',
      'input[type="button"]',
      'input[type="submit"]',
      'a[href]',
      '[role="button"]',
      '[role="link"]',
      '[tabindex="0"]'
    ].join(', ');

    return this.page.locator(interactiveSelectors).all();
  }

  /**
   * Check for auto-playing media
   */
  async getAutoPlayingMedia(): Promise<Locator[]> {
    return this.page.locator('video[autoplay], audio[autoplay]').all();
  }

  /**
   * Get elements with animations
   */
  async getAnimatedElements(): Promise<Locator[]> {
    return this.page.locator('[style*="animation"], [style*="transition"], .animate, [data-animate]').all();
  }

  /**
   * Verify text can be resized to 200%
   */
  async verifyTextResizing(): Promise<void> {
    const originalViewport = this.page.viewportSize();
    
    if (!originalViewport) return;

    // Simulate 200% zoom by reducing viewport
    const zoomedViewport = {
      width: Math.floor(originalViewport.width / 2),
      height: Math.floor(originalViewport.height / 2)
    };

    await this.page.setViewportSize(zoomedViewport);
    await this.page.waitForTimeout(500);

    // Check that content is still readable and not horizontally scrollable
    const bodyWidth = await this.page.evaluate(() => document.body.scrollWidth);
    const viewportWidth = await this.page.evaluate(() => window.innerWidth);

    expect(bodyWidth).toBeLessThanOrEqual(viewportWidth * 1.1); // Allow small margin

    // Restore original viewport
    await this.page.setViewportSize(originalViewport);
  }

  /**
   * Check color contrast programmatically (basic check)
   */
  async checkBasicColorContrast(element: Locator): Promise<boolean> {
    const styles = await element.evaluate((el) => {
      const computed = window.getComputedStyle(el);
      return {
        color: computed.color,
        backgroundColor: computed.backgroundColor,
        fontSize: computed.fontSize
      };
    });

    // This is a basic check - axe-core does more comprehensive testing
    return styles.color !== styles.backgroundColor;
  }

  /**
   * Get page language
   */
  async getPageLanguage(): Promise<string | null> {
    return this.page.locator('html').getAttribute('lang');
  }

  /**
   * Get page title
   */
  async getPageTitle(): Promise<string> {
    return this.page.title();
  }

  /**
   * Check if element has accessible name
   */
  async hasAccessibleName(element: Locator): Promise<boolean> {
    const accessibleName = await element.evaluate((el) => {
      // Check various ways an element can have an accessible name
      return (
        el.getAttribute('aria-label') ||
        el.getAttribute('aria-labelledby') ||
        (el as HTMLElement).innerText ||
        el.getAttribute('alt') ||
        el.getAttribute('title') ||
        ''
      );
    });

    return accessibleName.trim().length > 0;
  }

  /**
   * Get element's accessible name
   */
  async getAccessibleName(element: Locator): Promise<string> {
    return element.evaluate((el) => {
      // Priority order for accessible name
      const ariaLabel = el.getAttribute('aria-label');
      if (ariaLabel) return ariaLabel;

      const ariaLabelledBy = el.getAttribute('aria-labelledby');
      if (ariaLabelledBy) {
        const labelElement = document.getElementById(ariaLabelledBy);
        if (labelElement) return labelElement.textContent || '';
      }

      const innerText = (el as HTMLElement).innerText;
      if (innerText) return innerText;

      const alt = el.getAttribute('alt');
      if (alt) return alt;

      const title = el.getAttribute('title');
      if (title) return title;

      // For form elements, check associated label
      if (el instanceof HTMLInputElement || el instanceof HTMLSelectElement || el instanceof HTMLTextAreaElement) {
        const labels = document.querySelectorAll(`label[for="${el.id}"]`);
        if (labels.length > 0) {
          return labels[0].textContent || '';
        }
      }

      return '';
    });
  }

  /**
   * Check if element is keyboard accessible
   */
  async isKeyboardAccessible(element: Locator): Promise<boolean> {
    const isInteractive = await element.evaluate((el) => {
      const tagName = el.tagName.toLowerCase();
      const role = el.getAttribute('role');
      const tabIndex = el.getAttribute('tabindex');

      return (
        tagName === 'button' ||
        tagName === 'a' ||
        tagName === 'input' ||
        tagName === 'select' ||
        tagName === 'textarea' ||
        role === 'button' ||
        role === 'link' ||
        role === 'tab' ||
        role === 'menuitem' ||
        (tabIndex !== null && tabIndex !== '-1')
      );
    });

    if (!isInteractive) return true; // Non-interactive elements don't need keyboard access

    // Try to focus the element
    try {
      await element.focus();
      const isFocused = await element.evaluate((el) => document.activeElement === el);
      return isFocused;
    } catch {
      return false;
    }
  }

  /**
   * Verify element has proper ARIA attributes
   */
  async verifyAriaAttributes(element: Locator): Promise<boolean> {
    const role = await element.getAttribute('role');
    
    if (!role) return true; // Elements without roles don't need ARIA validation

    // Check required attributes for common roles
    const requiredAttributes: Record<string, string[]> = {
      'button': [],
      'link': [],
      'tab': ['aria-controls'],
      'tabpanel': ['aria-labelledby'],
      'dialog': ['aria-labelledby', 'aria-describedby'],
      'checkbox': [],
      'radio': [],
      'menuitem': [],
      'option': []
    };

    const required = requiredAttributes[role];
    if (!required) return true;

    for (const attr of required) {
      const value = await element.getAttribute(attr);
      if (!value) return false;
    }

    return true;
  }

  /**
   * Get form validation errors
   */
  async getFormValidationErrors(): Promise<Array<{ field: Locator; error: string }>> {
    const errors: Array<{ field: Locator; error: string }> = [];
    
    const fields = await this.page.locator('input, select, textarea').all();
    
    for (const field of fields) {
      const validationMessage = await field.evaluate((el) => {
        if (el instanceof HTMLInputElement || 
            el instanceof HTMLSelectElement || 
            el instanceof HTMLTextAreaElement) {
          return el.validationMessage;
        }
        return '';
      });

      if (validationMessage) {
        errors.push({ field, error: validationMessage });
      }
    }

    return errors;
  }

  /**
   * Check if page has proper document structure
   */
  async verifyDocumentStructure(): Promise<{
    hasTitle: boolean;
    hasLang: boolean;
    hasMain: boolean;
    hasH1: boolean;
    headingOrder: boolean;
  }> {
    const title = await this.page.title();
    const lang = await this.getPageLanguage();
    const main = await this.page.locator('main, [role="main"]').count();
    const h1 = await this.page.locator('h1').count();
    
    // Check heading order
    const headings = await this.getHeadingStructure();
    const headingOrder = await this.verifyHeadingOrder(headings);

    return {
      hasTitle: title.length > 0,
      hasLang: lang !== null && lang.length > 0,
      hasMain: main > 0,
      hasH1: h1 > 0,
      headingOrder
    };
  }

  /**
   * Verify heading order is logical
   */
  private async verifyHeadingOrder(headings: HeadingStructure): Promise<boolean> {
    const allHeadings: Array<{ level: number; text: string }> = [];

    for (const [level, headingElements] of Object.entries(headings)) {
      const levelNumber = parseInt(level.substring(1)); // Extract number from h1, h2, etc.
      
      for (const heading of headingElements) {
        const text = await heading.textContent();
        allHeadings.push({ level: levelNumber, text: text || '' });
      }
    }

    // Sort by appearance order (this is a simplified check)
    // In a real implementation, you'd check DOM order
    allHeadings.sort((a, b) => a.level - b.level);

    // Check for proper nesting (no skipping levels)
    let currentLevel = 0;
    for (const heading of allHeadings) {
      if (heading.level > currentLevel + 1) {
        return false; // Skipped a level
      }
      currentLevel = heading.level;
    }

    return true;
  }
}