/**
 * Accessibility Helper for E2E Testing
 * Provides utilities for accessibility testing with axe-core integration
 * Supports WCAG 2.1 AA compliance validation
 */

import {Locator, Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

interface AxeConfiguration {
  tags: string[];
  rules: Record<string, { enabled: boolean }>;
  disableOtherRules: boolean;
}

interface FocusIndicatorResult {
  isVisible: boolean;
  hasOutline: boolean;
  hasBoxShadow: boolean;
  hasBackgroundChange: boolean;
  hasColorChange: boolean;
  meetsContrastRequirement: boolean;
}

interface ColorInfo {
  r: number;
  g: number;
  b: number;
  a?: number;
}

interface ContrastResult {
  ratio: number;
  meetsAA: boolean;
  meetsAAA: boolean;
  foreground: ColorInfo;
  background: ColorInfo;
}

export class AccessibilityHelper {
  private axeConfig: AxeConfiguration;

  constructor(private page: Page) {
    this.axeConfig = {
      tags: ['wcag2a', 'wcag2aa', 'wcag21aa'],
      rules: {},
      disableOtherRules: false
    };
  }

  /**
   * Configure axe-core for comprehensive WCAG testing
   */
  async configureAxe(): Promise<void> {
    await this.page.addInitScript(() => {
      // Configure axe-core options
      window.axe?.configure({
        rules: {
          // Enable all WCAG 2.1 AA rules
          'color-contrast': {enabled: true},
          'color-contrast-enhanced': {enabled: true},
          'focus-order-semantics': {enabled: true},
          'hidden-content': {enabled: false}, // We want to test hidden content in some cases
          'landmark-one-main': {enabled: true},
          'page-has-heading-one': {enabled: true},
          'region': {enabled: true},
          'skip-link': {enabled: true}
        },
        tags: ['wcag2a', 'wcag2aa', 'wcag21aa', 'best-practice']
      });
    });
  }

  /**
   * Run comprehensive accessibility scan
   */
  async runFullAccessibilityScan(): Promise<{
    violations: Array<{
      id: string;
      description: string;
      impact: string;
      nodes: Array<{
        target: string[];
        html: string;
        failureSummary: string;
      }>;
    }>;
    passes: number;
    inapplicable: number;
    incomplete: Array<{
      id: string;
      description: string;
      nodes: Array<{
        target: string[];
        html: string;
      }>;
    }>;
  }> {
    const results = await new AxeBuilder({page: this.page})
    .withTags(this.axeConfig.tags)
    .analyze();

    return {
      violations: results.violations,
      passes: results.passes.length,
      inapplicable: results.inapplicable.length,
      incomplete: results.incomplete
    };
  }

  /**
   * Run focused accessibility scan on specific element
   */
  async scanElement(element: Locator): Promise<{
    violations: Array<{
      id: string;
      description: string;
      nodes: Array<{
        target: string[];
        html: string;
      }>;
    }>;
    passes: number;
  }> {
    const elementHandle = await element.elementHandle();
    if (!elementHandle) {
      throw new Error('Element not found for accessibility scan');
    }

    const results = await new AxeBuilder({page: this.page})
    .include(elementHandle)
    .withTags(this.axeConfig.tags)
    .analyze();

    return {
      violations: results.violations,
      passes: results.passes.length
    };
  }

  /**
   * Verify focus indicator meets WCAG requirements
   */
  async verifyFocusIndicator(element: Locator): Promise<FocusIndicatorResult> {
    await element.focus();

    const focusStyles = await element.evaluate((el) => {
      const computed = window.getComputedStyle(el);
      const beforeFocus = {
        outline: computed.outline,
        boxShadow: computed.boxShadow,
        backgroundColor: computed.backgroundColor,
        color: computed.color,
        border: computed.border
      };

      return beforeFocus;
    });

    // Simulate focus and get new styles
    const focusedStyles = await element.evaluate((el) => {
      el.focus();
      const computed = window.getComputedStyle(el);
      return {
        outline: computed.outline,
        boxShadow: computed.boxShadow,
        backgroundColor: computed.backgroundColor,
        color: computed.color,
        border: computed.border
      };
    });

    const hasOutline = focusedStyles.outline !== 'none' && focusedStyles.outline !== focusStyles.outline;
    const hasBoxShadow = focusedStyles.boxShadow !== 'none' && focusedStyles.boxShadow !== focusStyles.boxShadow;
    const hasBackgroundChange = focusedStyles.backgroundColor !== focusStyles.backgroundColor;
    const hasColorChange = focusedStyles.color !== focusStyles.color;
    const hasBorderChange = focusedStyles.border !== focusStyles.border;

    const isVisible = hasOutline || hasBoxShadow || hasBackgroundChange || hasColorChange || hasBorderChange;

    // Check contrast of focus indicator
    const contrastResult = await this.checkElementContrast(element);
    const meetsContrastRequirement = contrastResult.meetsAA;

    return {
      isVisible,
      hasOutline,
      hasBoxShadow,
      hasBackgroundChange,
      hasColorChange,
      meetsContrastRequirement
    };
  }

  /**
   * Check color contrast ratio
   */
  async checkElementContrast(element: Locator): Promise<ContrastResult> {
    const colors = await element.evaluate((el) => {
      const computed = window.getComputedStyle(el);
      return {
        color: computed.color,
        backgroundColor: computed.backgroundColor,
        fontSize: computed.fontSize
      };
    });

    const foreground = this.parseColor(colors.color);
    const background = this.parseColor(colors.backgroundColor);

    // If background is transparent, find the actual background
    const actualBackground = background.a === 0 ?
        await this.getActualBackgroundColor(element) : background;

    const ratio = this.calculateContrastRatio(foreground, actualBackground);

    // WCAG AA requirements: 4.5:1 for normal text, 3:1 for large text
    const fontSize = parseFloat(colors.fontSize);
    const isLargeText = fontSize >= 18 || (fontSize >= 14 && await this.isBoldText(element));

    const meetsAA = isLargeText ? ratio >= 3 : ratio >= 4.5;
    const meetsAAA = isLargeText ? ratio >= 4.5 : ratio >= 7;

    return {
      ratio: Math.round(ratio * 100) / 100,
      meetsAA,
      meetsAAA,
      foreground,
      background: actualBackground
    };
  }

  /**
   * Verify heading order is logical
   */
  async verifyHeadingOrder(headings: {
    h1: Locator[];
    h2: Locator[];
    h3: Locator[];
    h4: Locator[];
    h5: Locator[];
    h6: Locator[];
  }): Promise<boolean> {
    // Get all headings with their DOM position
    const allHeadings: Array<{ level: number; element: Locator; text: string }> = [];

    for (const [level, headingElements] of Object.entries(headings)) {
      const levelNumber = parseInt(level.substring(1));

      for (const heading of headingElements) {
        const text = await heading.textContent();
        allHeadings.push({level: levelNumber, element: heading, text: text || ''});
      }
    }

    // Sort by DOM order
    const sortedHeadings = await Promise.all(
        allHeadings.map(async (heading, index) => ({
          ...heading,
          domOrder: await this.getDomOrder(heading.element),
          originalIndex: index
        }))
    );

    sortedHeadings.sort((a, b) => a.domOrder - b.domOrder);

    // Verify logical order (no skipped levels)
    let previousLevel = 0;
    for (const heading of sortedHeadings) {
      if (heading.level > previousLevel + 1 && previousLevel > 0) {
        return false; // Skipped a level
      }
      previousLevel = Math.max(previousLevel, heading.level);
    }

    return true;
  }

  /**
   * Verify landmark labels are appropriate
   */
  async verifyLandmarkLabels(landmarks: {
    main: Locator[];
    navigation: Locator[];
    banner: Locator[];
    contentinfo: Locator[];
    complementary: Locator[];
    search: Locator[];
  }): Promise<boolean> {
    for (const [landmarkType, landmarkElements] of Object.entries(landmarks)) {
      for (const landmark of landmarkElements) {
        const hasLabel = await this.hasAccessibleName(landmark);

        // Multiple landmarks of the same type must have distinct labels
        if (landmarkElements.length > 1 && !hasLabel) {
          console.warn(`Multiple ${landmarkType} landmarks found but some lack accessible names`);
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Get accessible name of an element
   */
  async getAccessibleName(element: Locator): Promise<string> {
    return element.evaluate((el) => {
      // Use the Accessibility API if available (cast to unknown first for type safety)
      const elementWithComputedName = el as unknown as { computedName?: string };
      if ('computedName' in el && elementWithComputedName.computedName) {
        return elementWithComputedName.computedName;
      }

      // Fallback to manual calculation
      const ariaLabel = el.getAttribute('aria-label');
      if (ariaLabel) return ariaLabel;

      const ariaLabelledBy = el.getAttribute('aria-labelledby');
      if (ariaLabelledBy) {
        const referencedElements = ariaLabelledBy
        .split(' ')
        .map(id => document.getElementById(id))
        .filter(Boolean);

        return referencedElements.map(ref => ref?.textContent).join(' ').trim();
      }

      // For form controls, check labels
      if (el instanceof HTMLInputElement || el instanceof HTMLSelectElement || el instanceof HTMLTextAreaElement) {
        const label = document.querySelector(`label[for="${el.id}"]`);
        if (label) return label.textContent || '';

        const parentLabel = el.closest('label');
        if (parentLabel) return parentLabel.textContent?.replace(el.value || '', '').trim() || '';
      }

      // For buttons and links, use text content
      if (el instanceof HTMLButtonElement || el instanceof HTMLAnchorElement) {
        return el.textContent?.trim() || '';
      }

      // Check title attribute
      const title = el.getAttribute('title');
      if (title) return title;

      // Check alt attribute for images
      const alt = el.getAttribute('alt');
      if (alt) return alt;

      return '';
    });
  }

  /**
   * Check if element has accessible name
   */
  async hasAccessibleName(element: Locator): Promise<boolean> {
    const name = await this.getAccessibleName(element);
    return name.trim().length > 0;
  }

  /**
   * Verify ARIA attributes are valid
   */
  async verifyAriaAttributes(element: Locator): Promise<{
    isValid: boolean;
    errors: string[];
  }> {
    const errors: string[] = [];

    const ariaAttributes = await element.evaluate((el) => {
      const attrs: Record<string, string> = {};
      for (const attr of el.attributes) {
        if (attr.name.startsWith('aria-')) {
          attrs[attr.name] = attr.value;
        }
      }
      return attrs;
    });

    // Validate common ARIA attributes
    for (const [attr, value] of Object.entries(ariaAttributes)) {
      switch (attr) {
        case 'aria-expanded':
          if (!['true', 'false'].includes(value)) {
            errors.push(`Invalid aria-expanded value: ${value}`);
          }
          break;
        case 'aria-checked':
          if (!['true', 'false', 'mixed'].includes(value)) {
            errors.push(`Invalid aria-checked value: ${value}`);
          }
          break;
        case 'aria-pressed':
          if (!['true', 'false', 'mixed'].includes(value)) {
            errors.push(`Invalid aria-pressed value: ${value}`);
          }
          break;
        case 'aria-selected':
          if (!['true', 'false'].includes(value)) {
            errors.push(`Invalid aria-selected value: ${value}`);
          }
          break;
        case 'aria-hidden':
          if (!['true', 'false'].includes(value)) {
            errors.push(`Invalid aria-hidden value: ${value}`);
          }
          break;
        case 'aria-live':
          if (!['off', 'polite', 'assertive'].includes(value)) {
            errors.push(`Invalid aria-live value: ${value}`);
          }
          break;
        case 'aria-disabled':
          if (!['true', 'false'].includes(value)) {
            errors.push(`Invalid aria-disabled value: ${value}`);
          }
          break;
        case 'aria-required':
          if (!['true', 'false'].includes(value)) {
            errors.push(`Invalid aria-required value: ${value}`);
          }
          break;
      }
    }

    return {
      isValid: errors.length === 0,
      errors
    };
  }

  /**
   * Test keyboard navigation pattern
   */
  async testKeyboardNavigation(container: Locator): Promise<{
    canNavigate: boolean;
    focusableElements: number;
    navigationErrors: string[];
  }> {
    const errors: string[] = [];

    const focusableElements = await container.locator(
        'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
    ).all();

    let canNavigate = true;

    if (focusableElements.length === 0) {
      return {canNavigate: true, focusableElements: 0, navigationErrors: []};
    }

    // Test Tab navigation
    await focusableElements[0].focus();

    for (let i = 0; i < Math.min(focusableElements.length - 1, 10); i++) {
      await this.page.keyboard.press('Tab');

      const currentFocus = this.page.locator(':focus');
      const isVisible = await currentFocus.isVisible();

      if (!isVisible) {
        errors.push(`Focus not visible after Tab navigation at step ${i + 1}`);
        canNavigate = false;
      }
    }

    // Test Shift+Tab navigation
    for (let i = 0; i < Math.min(focusableElements.length - 1, 5); i++) {
      await this.page.keyboard.press('Shift+Tab');

      const currentFocus = this.page.locator(':focus');
      const isVisible = await currentFocus.isVisible();

      if (!isVisible) {
        errors.push(`Focus not visible after Shift+Tab navigation at step ${i + 1}`);
        canNavigate = false;
      }
    }

    return {
      canNavigate,
      focusableElements: focusableElements.length,
      navigationErrors: errors
    };
  }

  /**
   * Generate accessibility report
   */
  async generateReport(): Promise<{
    summary: {
      totalViolations: number;
      criticalViolations: number;
      totalPasses: number;
      complianceScore: number;
    };
    violations: Array<{
      rule: string;
      impact: string;
      count: number;
      description: string;
    }>;
    recommendations: string[];
  }> {
    const results = await this.runFullAccessibilityScan();

    const criticalViolations = results.violations.filter(v => v.impact === 'critical').length;
    const totalViolations = results.violations.length;

    const complianceScore = totalViolations === 0 ? 100 :
        Math.max(0, Math.round(((results.passes - totalViolations) / results.passes) * 100));

    const violationSummary = results.violations.map(violation => ({
      rule: violation.id,
      impact: violation.impact,
      count: violation.nodes.length,
      description: violation.description
    }));

    const recommendations = this.generateRecommendations(results.violations);

    return {
      summary: {
        totalViolations,
        criticalViolations,
        totalPasses: results.passes,
        complianceScore
      },
      violations: violationSummary,
      recommendations
    };
  }

  /**
   * Private helper methods
   */

  private parseColor(colorString: string): ColorInfo {
    // Handle rgb/rgba format
    const rgbMatch = colorString.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)/);
    if (rgbMatch) {
      return {
        r: parseInt(rgbMatch[1]),
        g: parseInt(rgbMatch[2]),
        b: parseInt(rgbMatch[3]),
        a: rgbMatch[4] ? parseFloat(rgbMatch[4]) : 1
      };
    }

    // Handle hex format
    const hexMatch = colorString.match(/^#([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i);
    if (hexMatch) {
      return {
        r: parseInt(hexMatch[1], 16),
        g: parseInt(hexMatch[2], 16),
        b: parseInt(hexMatch[3], 16),
        a: 1
      };
    }

    // Handle named colors (simplified)
    const namedColors: Record<string, ColorInfo> = {
      'white': {r: 255, g: 255, b: 255, a: 1},
      'black': {r: 0, g: 0, b: 0, a: 1},
      'transparent': {r: 0, g: 0, b: 0, a: 0}
    };

    return namedColors[colorString.toLowerCase()] || {r: 0, g: 0, b: 0, a: 1};
  }

  private calculateContrastRatio(foreground: ColorInfo, background: ColorInfo): number {
    const getLuminance = (color: ColorInfo): number => {
      const {r, g, b} = color;
      const [rs, gs, bs] = [r, g, b].map(c => {
        c = c / 255;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
      });
      return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs;
    };

    const l1 = getLuminance(foreground);
    const l2 = getLuminance(background);

    return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
  }

  private async getActualBackgroundColor(element: Locator): Promise<ColorInfo> {
    // Traverse up the DOM to find the first non-transparent background
    return element.evaluate((el) => {
      let currentElement = el as HTMLElement;

      while (currentElement && currentElement !== document.body) {
        const computed = window.getComputedStyle(currentElement);
        const bgColor = computed.backgroundColor;

        if (bgColor && bgColor !== 'transparent' && bgColor !== 'rgba(0, 0, 0, 0)') {
          const rgbMatch = bgColor.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)/);
          if (rgbMatch) {
            return {
              r: parseInt(rgbMatch[1]),
              g: parseInt(rgbMatch[2]),
              b: parseInt(rgbMatch[3]),
              a: rgbMatch[4] ? parseFloat(rgbMatch[4]) : 1
            };
          }
        }

        currentElement = currentElement.parentElement || currentElement;
      }

      // Default to white background
      return {r: 255, g: 255, b: 255, a: 1};
    });
  }

  private async isBoldText(element: Locator): Promise<boolean> {
    return element.evaluate((el) => {
      const computed = window.getComputedStyle(el);
      const fontWeight = computed.fontWeight;
      return fontWeight === 'bold' || parseInt(fontWeight) >= 700;
    });
  }

  private async getDomOrder(element: Locator): Promise<number> {
    return element.evaluate((el) => {
      const walker = document.createTreeWalker(
          document.body,
          NodeFilter.SHOW_ELEMENT,
          null
      );

      let order = 0;
      let currentNode = walker.nextNode();

      while (currentNode) {
        if (currentNode === el) {
          return order;
        }
        order++;
        currentNode = walker.nextNode();
      }

      return order;
    });
  }

  private generateRecommendations(violations: Array<{
    id: string;
    description: string;
    impact: string
  }>): string[] {
    const recommendations: string[] = [];
    const violationTypes = new Set(violations.map(v => v.id));

    if (violationTypes.has('color-contrast')) {
      recommendations.push('Increase color contrast ratios to meet WCAG AA standards (4.5:1 for normal text, 3:1 for large text)');
    }

    if (violationTypes.has('image-alt')) {
      recommendations.push('Add meaningful alt text to all images or mark decorative images with alt=""');
    }

    if (violationTypes.has('label')) {
      recommendations.push('Ensure all form controls have associated labels using <label> elements or aria-label attributes');
    }

    if (violationTypes.has('button-name')) {
      recommendations.push('Provide accessible names for all buttons using text content, aria-label, or aria-labelledby');
    }

    if (violationTypes.has('heading-order')) {
      recommendations.push('Maintain logical heading hierarchy without skipping levels (h1 → h2 → h3...)');
    }

    if (violationTypes.has('landmark-one-main')) {
      recommendations.push('Ensure page has exactly one main landmark (<main> or role="main")');
    }

    if (violationTypes.has('focus-order-semantics')) {
      recommendations.push('Ensure keyboard focus order matches visual layout and interactive elements are keyboard accessible');
    }

    if (recommendations.length === 0) {
      recommendations.push('Great job! No major accessibility issues detected. Continue following WCAG 2.1 AA guidelines.');
    }

    return recommendations;
  }
}