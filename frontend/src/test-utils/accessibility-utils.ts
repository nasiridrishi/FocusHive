import {configureAxe, toHaveNoViolations} from 'jest-axe';
import {RenderResult} from '@testing-library/react';

// Extend expect with jest-axe matchers
expect.extend(toHaveNoViolations);

// Configure axe with custom rules
export const axe = configureAxe({
  rules: {
    // Enable stricter color contrast checking
    'color-contrast': {enabled: true},
    // Enable ARIA usage checks
    'aria-roles': {enabled: true},
    'aria-valid-attr': {enabled: true},
    'aria-required-attr': {enabled: true},
    // Enable semantic markup checks
    'landmark-one-main': {enabled: true},
    'page-has-heading-one': {enabled: true},
    // Enable form accessibility checks
    'label': {enabled: true},
    'form-field-multiple-labels': {enabled: true},
  }
});

/**
 * Test component for accessibility violations
 */
export const testA11y = async (container: Element, axeOptions?: object) => {
  const results = await axe(container, axeOptions);
  expect(results).toHaveNoViolations();
  return results;
};

/**
 * Test component accessibility with custom configuration
 */
export const testA11yWithConfig = async (
    container: Element,
    config: {
      rules?: Record<string, { enabled: boolean }>;
      tags?: string[];
      exclude?: string[];
      include?: string[];
    }
) => {
  const customAxe = configureAxe({
    rules: config.rules || {}
  });

  let axeOptions = {};
  if (config.exclude || config.include) {
    axeOptions = {
      exclude: config.exclude,
      include: config.include,
    };
  }

  const results = await customAxe(container, axeOptions);
  expect(results).toHaveNoViolations();
  return results;
};

/**
 * Test keyboard navigation for a component
 */
export const testKeyboardNavigation = async (
    renderResult: RenderResult,
    expectedFocusableElements: string[]
) => {
  const {container} = renderResult;
  const {default: userEvent} = await import('@testing-library/user-event');
  const user = userEvent.setup();

  // Get all focusable elements
  const focusableElements = container.querySelectorAll(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
  );

  expect(focusableElements).toHaveLength(expectedFocusableElements.length);

  // Test tab navigation
  for (let i = 0; i < focusableElements.length; i++) {
    await user.tab();
    expect(focusableElements[i]).toHaveFocus();
  }

  // Test shift+tab navigation
  for (let i = focusableElements.length - 2; i >= 0; i--) {
    await user.tab({shift: true});
    expect(focusableElements[i]).toHaveFocus();
  }
};

/**
 * Test focus management for modals and dialogs
 */
export const testFocusTrap = async (
    renderResult: RenderResult,
    triggerElement: Element,
    modalSelector: string
) => {
  const {container} = renderResult;
  const {default: userEvent} = await import('@testing-library/user-event');
  const user = userEvent.setup();

  // Focus should be trapped within the modal
  const modal = container.querySelector(modalSelector);
  expect(modal).toBeInTheDocument();

  const focusableInModal = modal?.querySelectorAll(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
  ) || [];

  if (focusableInModal.length > 0) {
    // First focusable element should be focused
    expect(focusableInModal[0]).toHaveFocus();

    // Tab to last element
    for (let i = 1; i < focusableInModal.length; i++) {
      await user.tab();
      expect(focusableInModal[i]).toHaveFocus();
    }

    // Tab from last element should go to first
    await user.tab();
    expect(focusableInModal[0]).toHaveFocus();

    // Shift+tab from first element should go to last
    await user.tab({shift: true});
    expect(focusableInModal[focusableInModal.length - 1]).toHaveFocus();
  }
};

/**
 * Test ARIA live regions for dynamic content announcements
 */
export const testLiveRegion = async (
    container: Element,
    expectedText: string,
    liveRegionSelector = '[aria-live]'
) => {
  const liveRegion = container.querySelector(liveRegionSelector);
  expect(liveRegion).toBeInTheDocument();
  expect(liveRegion).toHaveTextContent(expectedText);
  expect(liveRegion).toHaveAttribute('aria-live');
};

/**
 * Test form accessibility
 */
export const testFormA11y = async (renderResult: RenderResult) => {
  const {container} = renderResult;

  // All form inputs should have labels
  const inputs = container.querySelectorAll('input, select, textarea');
  inputs.forEach((input) => {
    const hasLabel =
        input.hasAttribute('aria-label') ||
        input.hasAttribute('aria-labelledby') ||
        container.querySelector(`label[for="${input.id}"]`) ||
        input.closest('label');

    expect(hasLabel).toBeTruthy();
  });

  // Required fields should be marked appropriately
  const requiredInputs = container.querySelectorAll('[required]');
  requiredInputs.forEach((input) => {
    expect(
        input.hasAttribute('aria-required') ||
        input.hasAttribute('required')
    ).toBeTruthy();
  });

  // Error messages should be associated with inputs
  const errorMessages = container.querySelectorAll('[role="alert"], .error, .MuiFormHelperText-error');
  errorMessages.forEach((error) => {
    const associatedInputId = error.getAttribute('id');
    if (associatedInputId) {
      const associatedInput = container.querySelector(`[aria-describedby*="${associatedInputId}"]`);
      expect(associatedInput).toBeInTheDocument();
    }
  });
};

/**
 * Test color contrast (requires axe-core)
 */
export const testColorContrast = async (container: Element) => {
  const results = await axe(container, {
    rules: {
      'color-contrast': {enabled: true},
      'color-contrast-enhanced': {enabled: true},
    },
  });

  expect(results).toHaveNoViolations();
  return results;
};

/**
 * Test heading structure
 */
export const testHeadingStructure = (container: Element): void => {
  const headings = Array.from(container.querySelectorAll('h1, h2, h3, h4, h5, h6'));

  if (headings.length > 0) {
    // Should start with h1 or have only one h1
    const h1Elements = headings.filter(h => h.tagName === 'H1');
    expect(h1Elements.length).toBeLessThanOrEqual(1);

    // Heading levels should not skip (e.g., h1 -> h3 without h2)
    const headingLevels = headings.map(h => parseInt(h.tagName.charAt(1)));
    for (let i = 1; i < headingLevels.length; i++) {
      const current = headingLevels[i];
      const previous = headingLevels[i - 1];

      // Allow same level, next level, or going back to any previous level
      expect(current - previous).toBeLessThanOrEqual(1);
    }
  }
};

/**
 * Test landmark roles
 */
export const testLandmarks = (container: Element): void => {
  const _landmarks = container.querySelectorAll(
      'main, nav, header, footer, aside, section[aria-label], [role="main"], [role="navigation"], [role="banner"], [role="contentinfo"], [role="complementary"]'
  );

  // Should have at least a main landmark
  const mainLandmarks = container.querySelectorAll('main, [role="main"]');
  expect(mainLandmarks.length).toBeGreaterThanOrEqual(1);
};

/**
 * Helper to create accessibility test suite
 */
export const createA11yTestSuite = (
    componentName: string,
    renderComponent: () => RenderResult,
    options: {
      skipKeyboardNavigation?: boolean;
      skipColorContrast?: boolean;
      skipHeadingStructure?: boolean;
      skipLandmarks?: boolean;
      customAxeRules?: Record<string, { enabled: boolean }>;
    } = {}
) => {
  describe(`${componentName} Accessibility`, () => {
    it('should have no accessibility violations', async () => {
      const {container} = renderComponent();
      await testA11y(container, {
        rules: options.customAxeRules,
      });
    });

    if (!options.skipColorContrast) {
      it('should meet color contrast requirements', async () => {
        const {container} = renderComponent();
        await testColorContrast(container);
      });
    }

    if (!options.skipHeadingStructure) {
      it('should have proper heading structure', () => {
        const {container} = renderComponent();
        testHeadingStructure(container);
      });
    }

    if (!options.skipLandmarks) {
      it('should have proper landmark structure', () => {
        const {container} = renderComponent();
        testLandmarks(container);
      });
    }
  });
};