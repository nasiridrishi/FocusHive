/**
 * Comprehensive Responsive Layout Tests for FocusHive
 * Tests layout adaptation patterns and responsive design components
 */

import { test, expect, Page, Locator } from '@playwright/test';
import { VIEWPORT_BREAKPOINTS, MOBILE_DEVICES } from './mobile.config';
import { MobileHelper } from '../../helpers/mobile.helper';
import { AuthHelper } from '../../helpers/auth.helper';
import { TEST_URLS } from '../../helpers/test-data';

interface LayoutTest {
  name: string;
  selector: string;
  breakpoints: {
    mobile: LayoutExpectation;
    tablet: LayoutExpectation;
    desktop: LayoutExpectation;
  };
  priority: 'critical' | 'high' | 'medium';
}

interface LayoutExpectation {
  visible: boolean;
  position?: 'static' | 'fixed' | 'absolute' | 'relative' | 'sticky';
  display?: string;
  gridColumns?: number;
  flexDirection?: 'row' | 'column';
  width?: string;
  height?: string;
  customValidation?: (element: Locator) => Promise<boolean>;
}

interface GridLayoutTest {
  name: string;
  selector: string;
  expectedColumns: {
    mobile: number;
    tablet: number;
    desktop: number;
  };
  itemSelector: string;
  gap?: number;
}

interface FlexLayoutTest {
  name: string;
  selector: string;
  expectedDirection: {
    mobile: 'row' | 'column';
    tablet: 'row' | 'column';  
    desktop: 'row' | 'column';
  };
  wrap?: boolean;
  alignment?: string;
}

test.describe('Responsive Layout - Navigation Patterns', () => {
  let authHelper: AuthHelper;
  let mobileHelper: MobileHelper;

  test.beforeEach(async ({ page }) => {
    authHelper = new AuthHelper(page);
    mobileHelper = new MobileHelper(page);
    await authHelper.loginWithTestUser();
  });

  const navigationTests: LayoutTest[] = [
    {
      name: 'Main Navigation',
      selector: '[data-testid="main-navigation"]',
      breakpoints: {
        mobile: { 
          visible: false, // Hidden on mobile, replaced by hamburger menu
          display: 'none'
        },
        tablet: { 
          visible: true,
          position: 'fixed',
          display: 'flex'
        },
        desktop: { 
          visible: true,
          position: 'static',
          display: 'flex'
        }
      },
      priority: 'critical'
    },
    {
      name: 'Mobile Menu Toggle',
      selector: '[data-testid="mobile-menu-toggle"]',
      breakpoints: {
        mobile: { 
          visible: true,
          position: 'fixed'
        },
        tablet: { 
          visible: false,
          display: 'none'
        },
        desktop: { 
          visible: false,
          display: 'none'
        }
      },
      priority: 'critical'
    },
    {
      name: 'Bottom Navigation',
      selector: '[data-testid="bottom-navigation"]',
      breakpoints: {
        mobile: { 
          visible: true,
          position: 'fixed'
        },
        tablet: { 
          visible: false,
          display: 'none'
        },
        desktop: { 
          visible: false,
          display: 'none'
        }
      },
      priority: 'high'
    },
    {
      name: 'Breadcrumbs',
      selector: '[data-testid="breadcrumbs"]',
      breakpoints: {
        mobile: { 
          visible: false,
          display: 'none'
        },
        tablet: { 
          visible: true,
          display: 'block'
        },
        desktop: { 
          visible: true,
          display: 'block'
        }
      },
      priority: 'medium'
    }
  ];

  navigationTests.forEach(test => {
    test(`should adapt ${test.name} correctly across breakpoints`, async ({ page }) => {
      await page.goto(TEST_URLS.DASHBOARD);
      await page.waitForLoadState('networkidle');

      // Test mobile layout
      await validateLayoutAtBreakpoint(page, test, 'mobile', 390);
      
      // Test tablet layout  
      await validateLayoutAtBreakpoint(page, test, 'tablet', 768);
      
      // Test desktop layout
      await validateLayoutAtBreakpoint(page, test, 'desktop', 1024);
    });
  });

  test('should handle navigation drawer functionality', async ({ page }) => {
    await page.goto(TEST_URLS.DASHBOARD);
    
    // Set mobile viewport
    await page.setViewportSize({ width: 390, height: 844 });
    await page.waitForTimeout(300);

    await test.step('Mobile menu toggle should open/close drawer', async () => {
      const menuToggle = page.locator('[data-testid="mobile-menu-toggle"]');
      const navigationDrawer = page.locator('[data-testid="navigation-drawer"]');

      // Ensure menu toggle is visible on mobile
      await expect(menuToggle).toBeVisible();

      // Open drawer
      await menuToggle.click();
      await page.waitForTimeout(500); // Animation time

      // Verify drawer is open
      await expect(navigationDrawer).toBeVisible();
      
      // Verify drawer has correct mobile styling
      const drawerStyle = await navigationDrawer.evaluate(el => ({
        position: window.getComputedStyle(el).position,
        zIndex: window.getComputedStyle(el).zIndex,
        width: window.getComputedStyle(el).width
      }));

      expect(drawerStyle.position).toBe('fixed');
      expect(parseInt(drawerStyle.zIndex)).toBeGreaterThan(1000);

      // Close drawer by clicking outside or close button
      const closeButton = navigationDrawer.locator('[data-testid="drawer-close"]');
      if (await closeButton.isVisible()) {
        await closeButton.click();
      } else {
        // Click outside drawer to close
        await page.click('body', { position: { x: 50, y: 50 } });
      }

      await page.waitForTimeout(500);
      
      // Verify drawer is closed
      await expect(navigationDrawer).not.toBeVisible();
    });
  });

  test('should handle tab navigation responsiveness', async ({ page }) => {
    await page.goto(TEST_URLS.ANALYTICS);
    await page.waitForLoadState('networkidle');

    const tabTests = [
      { width: 390, name: 'Mobile', expectedBehavior: 'scrollable_tabs' },
      { width: 768, name: 'Tablet', expectedBehavior: 'full_width_tabs' },
      { width: 1024, name: 'Desktop', expectedBehavior: 'full_width_tabs' }
    ];

    for (const tabTest of tabTests) {
      await test.step(`Tab navigation on ${tabTest.name}`, async () => {
        await page.setViewportSize({ width: tabTest.width, height: 800 });
        await page.waitForTimeout(300);

        const tabContainer = page.locator('[data-testid="tab-container"], .tabs, [role="tablist"]');
        
        if (await tabContainer.isVisible()) {
          const tabStyle = await tabContainer.evaluate(el => ({
            overflowX: window.getComputedStyle(el).overflowX,
            flexWrap: window.getComputedStyle(el).flexWrap,
            justifyContent: window.getComputedStyle(el).justifyContent
          }));

          if (tabTest.expectedBehavior === 'scrollable_tabs') {
            // Mobile should allow horizontal scrolling
            expect(['scroll', 'auto']).toContain(tabStyle.overflowX);
          } else {
            // Tablet/desktop should fit all tabs
            expect(tabStyle.overflowX).toBe('visible');
          }
        }
      });
    }
  });
});

test.describe('Responsive Layout - Content Grid Systems', () => {
  const gridTests: GridLayoutTest[] = [
    {
      name: 'Hive Cards Grid',
      selector: '[data-testid="hive-grid"]',
      expectedColumns: {
        mobile: 1,
        tablet: 2, 
        desktop: 3
      },
      itemSelector: '[data-testid^="hive-card-"]',
      gap: 16
    },
    {
      name: 'Analytics Dashboard Grid',
      selector: '[data-testid="analytics-grid"]',
      expectedColumns: {
        mobile: 1,
        tablet: 2,
        desktop: 4
      },
      itemSelector: '[data-testid^="analytics-card-"]',
      gap: 20
    },
    {
      name: 'User Profile Grid',
      selector: '[data-testid="profile-sections"]',
      expectedColumns: {
        mobile: 1,
        tablet: 1,
        desktop: 2
      },
      itemSelector: '[data-testid^="profile-section-"]'
    }
  ];

  gridTests.forEach(gridTest => {
    test(`should adapt ${gridTest.name} grid layout correctly`, async ({ page }) => {
      let authHelper = new AuthHelper(page);
      await authHelper.loginWithTestUser();

      // Navigate to appropriate page based on grid type
      const urlMap = {
        'Hive Cards Grid': TEST_URLS.HIVE_LIST,
        'Analytics Dashboard Grid': TEST_URLS.ANALYTICS,
        'User Profile Grid': TEST_URLS.PROFILE
      };
      
      await page.goto(urlMap[gridTest.name as keyof typeof urlMap] || TEST_URLS.DASHBOARD);
      await page.waitForLoadState('networkidle');

      // Test mobile grid
      await validateGridLayout(page, gridTest, 'mobile', 390);
      
      // Test tablet grid
      await validateGridLayout(page, gridTest, 'tablet', 768);
      
      // Test desktop grid
      await validateGridLayout(page, gridTest, 'desktop', 1024);
    });
  });

  test('should handle grid item overflow gracefully', async ({ page }) => {
    let authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.HIVE_LIST);

    const viewports = [390, 768, 1024];
    
    for (const width of viewports) {
      await test.step(`Grid overflow handling at ${width}px`, async () => {
        await page.setViewportSize({ width, height: 800 });
        await page.waitForTimeout(300);

        // Check for horizontal overflow
        const hasHorizontalScroll = await page.evaluate(() => {
          return document.documentElement.scrollWidth > window.innerWidth;
        });

        expect(hasHorizontalScroll).toBeFalsy();

        // Check grid items don't overflow their containers
        const overflowingItems = await page.evaluate(() => {
          const gridItems = Array.from(document.querySelectorAll('[data-testid^="hive-card-"]'));
          return gridItems.filter(item => {
            const rect = item.getBoundingClientRect();
            return rect.right > window.innerWidth + 5; // 5px tolerance
          }).length;
        });

        expect(overflowingItems).toBe(0);
      });
    }
  });
});

test.describe('Responsive Layout - Flex Containers', () => {
  const flexTests: FlexLayoutTest[] = [
    {
      name: 'Header Actions',
      selector: '[data-testid="header-actions"]',
      expectedDirection: {
        mobile: 'column',
        tablet: 'row',
        desktop: 'row'
      },
      wrap: true
    },
    {
      name: 'Form Buttons',
      selector: '[data-testid="form-actions"]',
      expectedDirection: {
        mobile: 'column',
        tablet: 'row', 
        desktop: 'row'
      },
      alignment: 'center'
    },
    {
      name: 'Card Actions',
      selector: '[data-testid="card-actions"]',
      expectedDirection: {
        mobile: 'row',
        tablet: 'row',
        desktop: 'row'
      },
      wrap: false
    }
  ];

  flexTests.forEach(flexTest => {
    test(`should adapt ${flexTest.name} flex layout correctly`, async ({ page }) => {
      let authHelper = new AuthHelper(page);
      await authHelper.loginWithTestUser();
      
      await page.goto(TEST_URLS.DASHBOARD);
      await page.waitForLoadState('networkidle');

      // Test mobile flex layout
      await validateFlexLayout(page, flexTest, 'mobile', 390);
      
      // Test tablet flex layout  
      await validateFlexLayout(page, flexTest, 'tablet', 768);
      
      // Test desktop flex layout
      await validateFlexLayout(page, flexTest, 'desktop', 1024);
    });
  });
});

test.describe('Responsive Layout - Modal and Dialog Adaptation', () => {
  test('should adapt modals for mobile screens', async ({ page }) => {
    let authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.DASHBOARD);

    // Test mobile modal behavior
    await page.setViewportSize({ width: 390, height: 844 });
    
    // Trigger modal (create hive, settings, etc.)
    const createButton = page.locator('[data-testid="create-hive-button"]');
    if (await createButton.isVisible()) {
      await createButton.click();
      await page.waitForTimeout(500);

      const modal = page.locator('[role="dialog"], .modal, [data-testid="modal"]');
      
      if (await modal.isVisible()) {
        await test.step('Modal should be full-screen on mobile', async () => {
          const modalStyle = await modal.evaluate(el => ({
            width: window.getComputedStyle(el).width,
            height: window.getComputedStyle(el).height,
            position: window.getComputedStyle(el).position,
            top: window.getComputedStyle(el).top,
            left: window.getComputedStyle(el).left
          }));

          // Mobile modals should be full-screen or nearly full-screen
          const viewport = page.viewportSize();
          if (viewport) {
            const modalWidth = parseInt(modalStyle.width);
            const modalHeight = parseInt(modalStyle.height);
            
            expect(modalWidth / viewport.width).toBeGreaterThan(0.9); // 90% or more
            expect(modalStyle.position).toBe('fixed');
          }
        });

        // Close modal for next test
        const closeButton = modal.locator('[data-testid="modal-close"], .close, [aria-label="close"]');
        if (await closeButton.isVisible()) {
          await closeButton.click();
        } else {
          await page.keyboard.press('Escape');
        }
      }
    }

    // Test tablet modal behavior
    await page.setViewportSize({ width: 768, height: 1024 });
    
    if (await createButton.isVisible()) {
      await createButton.click();
      await page.waitForTimeout(500);

      const modal = page.locator('[role="dialog"], .modal, [data-testid="modal"]');
      
      if (await modal.isVisible()) {
        await test.step('Modal should be centered on tablet', async () => {
          const modalStyle = await modal.evaluate(el => ({
            width: window.getComputedStyle(el).width,
            maxWidth: window.getComputedStyle(el).maxWidth,
            left: window.getComputedStyle(el).left,
            transform: window.getComputedStyle(el).transform
          }));

          // Tablet modals should be centered and have reasonable width
          const modalWidth = parseInt(modalStyle.width);
          expect(modalWidth).toBeLessThan(700); // Not full width
          expect(modalWidth).toBeGreaterThan(400); // But not too narrow
        });
      }
    }
  });

  test('should handle drawer vs modal patterns appropriately', async ({ page }) => {
    let authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.PROFILE);

    const settingsButton = page.locator('[data-testid="settings-button"]');
    
    if (await settingsButton.isVisible()) {
      // Mobile should use bottom drawer or full-screen modal
      await page.setViewportSize({ width: 390, height: 844 });
      await settingsButton.click();
      await page.waitForTimeout(500);

      const bottomDrawer = page.locator('[data-testid="bottom-drawer"]');
      const fullScreenModal = page.locator('[data-testid="fullscreen-modal"]');
      const modal = page.locator('[role="dialog"]');

      const hasMobilePattern = 
        await bottomDrawer.isVisible() || 
        await fullScreenModal.isVisible() || 
        await modal.isVisible();

      expect(hasMobilePattern).toBeTruthy();

      // Close mobile pattern
      await page.keyboard.press('Escape');
      await page.waitForTimeout(300);

      // Desktop should use centered modal
      await page.setViewportSize({ width: 1024, height: 768 });
      await settingsButton.click();
      await page.waitForTimeout(500);

      const centeredModal = page.locator('[role="dialog"]');
      if (await centeredModal.isVisible()) {
        const modalStyle = await centeredModal.evaluate(el => {
          const rect = el.getBoundingClientRect();
          const viewportWidth = window.innerWidth;
          return {
            width: rect.width,
            left: rect.left,
            centerX: rect.left + rect.width / 2
          };
        });

        // Should be centered
        const viewportCenter = 1024 / 2;
        const tolerance = 50;
        expect(Math.abs(modalStyle.centerX - viewportCenter)).toBeLessThan(tolerance);
      }
    }
  });
});

test.describe('Responsive Layout - Typography and Spacing', () => {
  test('should scale typography appropriately', async ({ page }) => {
    let authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.HOME);

    const typographyElements = [
      { selector: 'h1', name: 'Main Heading', minMobile: 24, minDesktop: 32 },
      { selector: 'h2', name: 'Section Heading', minMobile: 20, minDesktop: 24 },
      { selector: 'h3', name: 'Sub Heading', minMobile: 18, minDesktop: 20 },
      { selector: 'p', name: 'Body Text', minMobile: 14, minDesktop: 16 },
      { selector: 'button', name: 'Button Text', minMobile: 14, minDesktop: 14 }
    ];

    for (const element of typographyElements) {
      const locator = page.locator(element.selector).first();
      
      if (await locator.isVisible()) {
        await test.step(`${element.name} typography scaling`, async () => {
          // Test mobile font size
          await page.setViewportSize({ width: 390, height: 844 });
          await page.waitForTimeout(200);
          
          const mobileFontSize = await locator.evaluate(el => {
            return parseFloat(window.getComputedStyle(el).fontSize);
          });
          
          expect(mobileFontSize).toBeGreaterThanOrEqual(element.minMobile);

          // Test desktop font size
          await page.setViewportSize({ width: 1024, height: 768 });
          await page.waitForTimeout(200);
          
          const desktopFontSize = await locator.evaluate(el => {
            return parseFloat(window.getComputedStyle(el).fontSize);
          });
          
          expect(desktopFontSize).toBeGreaterThanOrEqual(element.minDesktop);
          
          // Desktop should generally be same or larger than mobile
          expect(desktopFontSize).toBeGreaterThanOrEqual(mobileFontSize - 1); // 1px tolerance
        });
      }
    }
  });

  test('should maintain appropriate spacing at all breakpoints', async ({ page }) => {
    let authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.DASHBOARD);

    const spacingElements = [
      { selector: '[data-testid="main-content"]', property: 'padding' },
      { selector: '[data-testid="card-container"]', property: 'margin' },
      { selector: '[data-testid="section-header"]', property: 'marginBottom' }
    ];

    const viewports = [
      { width: 390, name: 'Mobile' },
      { width: 768, name: 'Tablet' },
      { width: 1024, name: 'Desktop' }
    ];

    for (const viewport of viewports) {
      await test.step(`Spacing validation at ${viewport.name}`, async () => {
        await page.setViewportSize({ width: viewport.width, height: 800 });
        await page.waitForTimeout(300);

        for (const element of spacingElements) {
          const locator = page.locator(element.selector);
          
          if (await locator.isVisible()) {
            const spacing = await locator.evaluate((el, prop) => {
              const computed = window.getComputedStyle(el);
              return parseFloat(computed.getPropertyValue(prop.replace(/([A-Z])/g, '-$1').toLowerCase()));
            }, element.property);

            // Minimum spacing requirements
            const minSpacing = viewport.width < 768 ? 8 : 12;
            expect(spacing).toBeGreaterThanOrEqual(minSpacing);
            
            // Maximum spacing (prevent excessive whitespace)
            const maxSpacing = viewport.width < 768 ? 24 : 48;
            expect(spacing).toBeLessThanOrEqual(maxSpacing);
          }
        }
      });
    }
  });
});

// Helper Functions

async function validateLayoutAtBreakpoint(
  page: Page, 
  layoutTest: LayoutTest, 
  breakpoint: 'mobile' | 'tablet' | 'desktop',
  width: number
): Promise<void> {
  await page.setViewportSize({ width, height: 800 });
  await page.waitForTimeout(300); // Allow CSS transitions

  const element = page.locator(layoutTest.selector);
  const expectation = layoutTest.breakpoints[breakpoint];

  // Visibility check
  if (expectation.visible) {
    await expect(element).toBeVisible();
  } else {
    await expect(element).not.toBeVisible();
  }

  // Style validations (only if element is visible)
  if (expectation.visible && await element.isVisible()) {
    if (expectation.position) {
      const position = await element.evaluate(el => 
        window.getComputedStyle(el).position
      );
      expect(position).toBe(expectation.position);
    }

    if (expectation.display) {
      const display = await element.evaluate(el => 
        window.getComputedStyle(el).display
      );
      expect(display).toBe(expectation.display);
    }

    if (expectation.customValidation) {
      const customValid = await expectation.customValidation(element);
      expect(customValid).toBeTruthy();
    }
  }
}

async function validateGridLayout(
  page: Page,
  gridTest: GridLayoutTest,
  breakpoint: 'mobile' | 'tablet' | 'desktop',
  width: number
): Promise<void> {
  await page.setViewportSize({ width, height: 800 });
  await page.waitForTimeout(300);

  const gridContainer = page.locator(gridTest.selector);
  
  if (await gridContainer.isVisible()) {
    const expectedColumns = gridTest.expectedColumns[breakpoint];
    
    // Check CSS Grid columns
    const actualColumns = await gridContainer.evaluate(el => {
      const style = window.getComputedStyle(el);
      const gridCols = style.gridTemplateColumns;
      
      if (gridCols && gridCols !== 'none') {
        return gridCols.split(' ').length;
      }
      
      // Fallback: check if using flexbox with width percentages
      const display = style.display;
      if (display === 'flex') {
        const children = Array.from(el.children);
        if (children.length === 0) return 0;
        
        const firstChildWidth = window.getComputedStyle(children[0]).width;
        const containerWidth = parseFloat(style.width);
        
        if (firstChildWidth.includes('%')) {
          const percentage = parseFloat(firstChildWidth);
          return Math.round(100 / percentage);
        }
      }
      
      return 1; // Default to single column
    });

    expect(actualColumns).toBe(expectedColumns);

    // Check grid gap if specified
    if (gridTest.gap) {
      const actualGap = await gridContainer.evaluate(el => {
        const style = window.getComputedStyle(el);
        return parseFloat(style.gap || style.gridGap || '0');
      });
      
      expect(actualGap).toBeGreaterThanOrEqual(gridTest.gap - 4); // 4px tolerance
    }
  }
}

async function validateFlexLayout(
  page: Page,
  flexTest: FlexLayoutTest,
  breakpoint: 'mobile' | 'tablet' | 'desktop',
  width: number
): Promise<void> {
  await page.setViewportSize({ width, height: 800 });
  await page.waitForTimeout(300);

  const flexContainer = page.locator(flexTest.selector);
  
  if (await flexContainer.isVisible()) {
    const expectedDirection = flexTest.expectedDirection[breakpoint];
    
    const flexStyles = await flexContainer.evaluate(el => ({
      display: window.getComputedStyle(el).display,
      flexDirection: window.getComputedStyle(el).flexDirection,
      flexWrap: window.getComputedStyle(el).flexWrap,
      justifyContent: window.getComputedStyle(el).justifyContent,
      alignItems: window.getComputedStyle(el).alignItems
    }));

    expect(flexStyles.display).toBe('flex');
    expect(flexStyles.flexDirection).toBe(expectedDirection);

    if (flexTest.wrap !== undefined) {
      const expectedWrap = flexTest.wrap ? 'wrap' : 'nowrap';
      expect(flexStyles.flexWrap).toBe(expectedWrap);
    }

    if (flexTest.alignment) {
      const hasValidAlignment = 
        flexStyles.justifyContent.includes(flexTest.alignment) ||
        flexStyles.alignItems.includes(flexTest.alignment);
      expect(hasValidAlignment).toBeTruthy();
    }
  }
}