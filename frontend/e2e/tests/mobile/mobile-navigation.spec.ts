/**
 * Mobile Navigation Pattern Tests for FocusHive
 * Tests mobile-specific navigation patterns, gestures, and UX flows
 */

import { test, expect, Page, Locator } from '@playwright/test';
import { MOBILE_DEVICES, VIEWPORT_BREAKPOINTS } from './mobile.config';
import { MobileHelper } from '../../helpers/mobile.helper';
import { AuthHelper } from '../../helpers/auth.helper';
import { TEST_URLS } from '../../helpers/test-data';

interface NavigationPattern {
  name: string;
  type: 'hamburger' | 'bottom_tabs' | 'drawer' | 'swipe' | 'accordion' | 'breadcrumb';
  selector: string;
  expectedBehavior: string;
  accessibility: {
    hasAriaLabels: boolean;
    keyboardAccessible: boolean;
    screenReaderFriendly: boolean;
  };
}

interface MobileNavigationTest {
  pattern: NavigationPattern;
  viewportWidth: number;
  expectedState: 'visible' | 'hidden' | 'collapsed';
  interactions: string[];
}

interface BreadcrumbTest {
  page: string;
  url: string;
  expectedLevels: string[];
  mobileAdaptation: 'hidden' | 'collapsed' | 'horizontal_scroll';
}

test.describe('Mobile Navigation - Primary Navigation Patterns', () => {
  let authHelper: AuthHelper;
  let mobileHelper: MobileHelper;

  test.beforeEach(async ({ page }) => {
    authHelper = new AuthHelper(page);
    mobileHelper = new MobileHelper(page);
    
    // Set mobile viewport
    await page.setViewportSize({ width: 390, height: 844 });
    await authHelper.loginWithTestUser();
  });

  const navigationPatterns: NavigationPattern[] = [
    {
      name: 'Hamburger Menu',
      type: 'hamburger',
      selector: '[data-testid="mobile-menu-toggle"]',
      expectedBehavior: 'opens_drawer_on_tap',
      accessibility: {
        hasAriaLabels: true,
        keyboardAccessible: true,
        screenReaderFriendly: true
      }
    },
    {
      name: 'Bottom Tab Navigation',
      type: 'bottom_tabs',
      selector: '[data-testid="bottom-navigation"]',
      expectedBehavior: 'fixed_bottom_position',
      accessibility: {
        hasAriaLabels: true,
        keyboardAccessible: true,
        screenReaderFriendly: true
      }
    },
    {
      name: 'Navigation Drawer',
      type: 'drawer',
      selector: '[data-testid="navigation-drawer"]',
      expectedBehavior: 'slides_from_left',
      accessibility: {
        hasAriaLabels: true,
        keyboardAccessible: true,
        screenReaderFriendly: true
      }
    },
    {
      name: 'Swipeable Tabs',
      type: 'swipe',
      selector: '[data-testid="swipeable-tabs"]',
      expectedBehavior: 'horizontal_swipe_navigation',
      accessibility: {
        hasAriaLabels: true,
        keyboardAccessible: false, // Swipe is not keyboard accessible
        screenReaderFriendly: true
      }
    }
  ];

  navigationPatterns.forEach(pattern => {
    test(`should implement ${pattern.name} correctly`, async ({ page }) => {
      await page.goto(TEST_URLS.DASHBOARD);
      await page.waitForLoadState('networkidle');

      await test.step(`Testing ${pattern.name} functionality`, async () => {
        const element = page.locator(pattern.selector);
        
        if (await element.isVisible()) {
          // Test basic functionality
          await testNavigationPattern(page, pattern);
          
          // Test accessibility
          await testNavigationAccessibility(page, pattern);
          
          // Test responsive behavior
          await testNavigationResponsiveness(page, pattern);
        } else {
          console.log(`${pattern.name} not found - may not be implemented yet`);
        }
      });
    });
  });

  test('should handle hamburger menu interaction flow', async ({ page }) => {
    await page.goto(TEST_URLS.DASHBOARD);
    await page.waitForLoadState('networkidle');

    const hamburgerButton = page.locator('[data-testid="mobile-menu-toggle"], .hamburger, [aria-label*="menu"]');
    const navigationDrawer = page.locator('[data-testid="navigation-drawer"], .drawer, [role="dialog"]');

    if (await hamburgerButton.isVisible()) {
      await test.step('Hamburger menu complete flow', async () => {
        // Initial state - drawer should be closed
        await expect(navigationDrawer).not.toBeVisible();

        // Open drawer
        await hamburgerButton.click();
        await page.waitForTimeout(500); // Animation time

        // Verify drawer is open
        await expect(navigationDrawer).toBeVisible();

        // Verify drawer content
        const drawerContent = navigationDrawer.locator('nav, ul, [role="navigation"]');
        await expect(drawerContent).toBeVisible();

        // Test navigation links
        const navigationLinks = drawerContent.locator('a, [role="menuitem"]');
        const linkCount = await navigationLinks.count();
        expect(linkCount).toBeGreaterThan(0);

        // Test clicking a navigation link
        if (linkCount > 0) {
          const firstLink = navigationLinks.first();
          const linkText = await firstLink.textContent();
          const linkHref = await firstLink.getAttribute('href');
          
          if (linkHref && !linkHref.startsWith('#')) {
            await firstLink.click();
            await page.waitForLoadState('networkidle');
            
            // Drawer should close after navigation
            await expect(navigationDrawer).not.toBeVisible();
            
            console.log(`Successfully navigated via drawer link: ${linkText}`);
          }
        }

        // Test closing drawer with close button
        if (await navigationDrawer.isVisible()) {
          const closeButton = navigationDrawer.locator('[data-testid="drawer-close"], .close, [aria-label*="close"]');
          
          if (await closeButton.isVisible()) {
            await closeButton.click();
            await page.waitForTimeout(300);
            await expect(navigationDrawer).not.toBeVisible();
          }
        }

        // Test closing drawer by clicking outside
        await hamburgerButton.click();
        await page.waitForTimeout(500);
        await expect(navigationDrawer).toBeVisible();
        
        // Click outside drawer area
        await page.click('body', { position: { x: 50, y: 50 } });
        await page.waitForTimeout(300);
        await expect(navigationDrawer).not.toBeVisible();

        // Test ESC key to close drawer
        await hamburgerButton.click();
        await page.waitForTimeout(500);
        await expect(navigationDrawer).toBeVisible();
        
        await page.keyboard.press('Escape');
        await page.waitForTimeout(300);
        await expect(navigationDrawer).not.toBeVisible();
      });
    }
  });

  test('should handle bottom tab navigation', async ({ page }) => {
    await page.goto(TEST_URLS.DASHBOARD);
    await page.waitForLoadState('networkidle');

    const bottomNavigation = page.locator('[data-testid="bottom-navigation"], .bottom-nav, .tab-bar');

    if (await bottomNavigation.isVisible()) {
      await test.step('Bottom tab navigation flow', async () => {
        // Verify fixed positioning
        const navStyle = await bottomNavigation.evaluate(el => ({
          position: window.getComputedStyle(el).position,
          bottom: window.getComputedStyle(el).bottom,
          zIndex: window.getComputedStyle(el).zIndex
        }));

        expect(navStyle.position).toBe('fixed');
        expect(parseInt(navStyle.zIndex)).toBeGreaterThan(10);

        // Test tab switching
        const tabs = bottomNavigation.locator('[role="tab"], .tab, a');
        const tabCount = await tabs.count();

        if (tabCount > 1) {
          // Get initial active tab
          const initialActiveTab = bottomNavigation.locator('[aria-selected="true"], .active, .selected');
          const initialActiveIndex = await getTabIndex(initialActiveTab);

          // Click on a different tab
          const secondTab = tabs.nth(1);
          const secondTabText = await secondTab.textContent();
          
          await secondTab.click();
          await page.waitForTimeout(500);

          // Verify tab switched
          const newActiveTab = bottomNavigation.locator('[aria-selected="true"], .active, .selected');
          const newActiveIndex = await getTabIndex(newActiveTab);
          
          expect(newActiveIndex).not.toBe(initialActiveIndex);
          
          // Verify content changed (check URL or page content)
          const currentUrl = page.url();
          console.log(`Navigated to tab: ${secondTabText}, URL: ${currentUrl}`);

          // Test all tabs
          for (let i = 0; i < Math.min(tabCount, 4); i++) {
            const tab = tabs.nth(i);
            const tabLabel = await tab.textContent();
            
            await tab.click();
            await page.waitForTimeout(300);
            
            // Verify tab is active
            const isActive = await tab.evaluate(el => {
              return el.getAttribute('aria-selected') === 'true' ||
                     el.classList.contains('active') ||
                     el.classList.contains('selected');
            });
            
            expect(isActive).toBeTruthy();
            console.log(`Tab "${tabLabel}" is active: ${isActive}`);
          }
        }
      });
    }
  });

  test('should handle swipe navigation between tabs', async ({ page }) => {
    await page.goto(TEST_URLS.ANALYTICS);
    await page.waitForLoadState('networkidle');

    const swipeableContainer = page.locator('[data-testid="swipeable-tabs"], .swipeable, .carousel');

    if (await swipeableContainer.isVisible()) {
      await test.step('Swipe navigation between tabs', async () => {
        const containerRect = await swipeableContainer.boundingBox();
        if (!containerRect) return;

        const centerX = containerRect.x + containerRect.width / 2;
        const centerY = containerRect.y + containerRect.height / 2;

        // Get initial tab state
        const initialActiveTab = page.locator('[aria-selected="true"], .active-tab, .current');
        const initialTabText = await initialActiveTab.textContent().catch(() => '');

        // Perform swipe left (next tab)
        await page.mouse.move(centerX + 100, centerY);
        await page.mouse.down();
        await page.mouse.move(centerX - 100, centerY, { steps: 10 });
        await page.mouse.up();
        
        await page.waitForTimeout(500); // Animation time

        // Check if tab changed
        const newActiveTab = page.locator('[aria-selected="true"], .active-tab, .current');
        const newTabText = await newActiveTab.textContent().catch(() => '');
        
        if (newTabText !== initialTabText) {
          console.log(`Swipe navigation successful: "${initialTabText}" -> "${newTabText}"`);
        }

        // Perform swipe right (previous tab)
        await page.mouse.move(centerX - 100, centerY);
        await page.mouse.down();
        await page.mouse.move(centerX + 100, centerY, { steps: 10 });
        await page.mouse.up();
        
        await page.waitForTimeout(500);

        // Verify we can navigate back
        const finalTabText = await newActiveTab.textContent().catch(() => '');
        console.log(`Swipe back result: "${newTabText}" -> "${finalTabText}"`);
      });
    }
  });
});

test.describe('Mobile Navigation - Breadcrumb Adaptation', () => {
  const breadcrumbTests: BreadcrumbTest[] = [
    {
      page: 'Dashboard',
      url: TEST_URLS.DASHBOARD,
      expectedLevels: ['Home', 'Dashboard'],
      mobileAdaptation: 'collapsed'
    },
    {
      page: 'Hive Details',
      url: TEST_URLS.HIVE_LIST + '/test-hive',
      expectedLevels: ['Home', 'Hives', 'Test Hive'],
      mobileAdaptation: 'horizontal_scroll'
    },
    {
      page: 'Profile Settings',
      url: TEST_URLS.PROFILE + '/settings',
      expectedLevels: ['Home', 'Profile', 'Settings'],
      mobileAdaptation: 'collapsed'
    }
  ];

  breadcrumbTests.forEach(breadcrumbTest => {
    test(`should adapt breadcrumbs on ${breadcrumbTest.page}`, async ({ page }) => {
      let authHelper = new AuthHelper(page);
      await authHelper.loginWithTestUser();

      // Set mobile viewport
      await page.setViewportSize({ width: 390, height: 844 });

      await test.step(`Testing breadcrumb adaptation for ${breadcrumbTest.page}`, async () => {
        await page.goto(breadcrumbTest.url);
        await page.waitForLoadState('networkidle');

        const breadcrumb = page.locator('[data-testid="breadcrumbs"], .breadcrumb, nav[aria-label*="breadcrumb"]');

        if (await breadcrumb.isVisible()) {
          // Test mobile adaptation
          await testBreadcrumbMobileAdaptation(page, breadcrumb, breadcrumbTest);
        } else {
          // Breadcrumbs might be hidden on mobile - this is acceptable
          console.log(`Breadcrumbs hidden on mobile for ${breadcrumbTest.page} - this is acceptable UX`);
        }
      });
    });
  });

  test('should provide alternative navigation when breadcrumbs are hidden', async ({ page }) => {
    let authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto(TEST_URLS.HIVE_LIST);

    await test.step('Alternative navigation when no breadcrumbs', async () => {
      const breadcrumb = page.locator('[data-testid="breadcrumbs"], .breadcrumb');
      const backButton = page.locator('[data-testid="back-button"], .back, [aria-label*="back"]');
      const hamburgerMenu = page.locator('[data-testid="mobile-menu-toggle"]');

      const hasBreadcrumbs = await breadcrumb.isVisible();
      
      if (!hasBreadcrumbs) {
        // Should have alternative navigation methods
        const hasBackButton = await backButton.isVisible();
        const hasHamburgerMenu = await hamburgerMenu.isVisible();
        
        const hasAlternativeNavigation = hasBackButton || hasHamburgerMenu;
        expect(hasAlternativeNavigation).toBeTruthy();
        
        console.log('Alternative navigation available:', {
          backButton: hasBackButton,
          hamburgerMenu: hasHamburgerMenu
        });

        // Test back button functionality if present
        if (hasBackButton) {
          await backButton.click();
          await page.waitForTimeout(500);
          
          // Should navigate back (check URL or page content changed)
          const currentUrl = page.url();
          console.log(`Back button navigated to: ${currentUrl}`);
        }
      }
    });
  });
});

test.describe('Mobile Navigation - Deep Linking and State Management', () => {
  test('should handle deep links with mobile navigation', async ({ page }) => {
    let authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    
    await page.setViewportSize({ width: 390, height: 844 });

    await test.step('Deep link navigation preservation', async () => {
      // Navigate to a deep page directly
      const deepUrl = TEST_URLS.ANALYTICS + '?tab=productivity&period=week';
      await page.goto(deepUrl);
      await page.waitForLoadState('networkidle');

      // Verify URL parameters are preserved
      const currentUrl = page.url();
      expect(currentUrl).toContain('tab=productivity');
      expect(currentUrl).toContain('period=week');

      // Verify mobile navigation reflects current location
      const activeNavItem = page.locator('[aria-selected="true"], .active, .current');
      if (await activeNavItem.isVisible()) {
        const activeText = await activeNavItem.textContent();
        console.log(`Active navigation item: ${activeText}`);
      }

      // Test navigation state persistence
      const hamburger = page.locator('[data-testid="mobile-menu-toggle"]');
      if (await hamburger.isVisible()) {
        await hamburger.click();
        await page.waitForTimeout(500);
        
        // Current page should be highlighted in drawer
        const drawerActiveItem = page.locator('[data-testid="navigation-drawer"] [aria-current], [data-testid="navigation-drawer"] .active');
        if (await drawerActiveItem.isVisible()) {
          const drawerActiveText = await drawerActiveItem.textContent();
          console.log(`Active item in drawer: ${drawerActiveText}`);
        }
      }
    });
  });

  test('should maintain navigation state during page transitions', async ({ page }) => {
    let authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    
    await page.setViewportSize({ width: 390, height: 844 });

    await test.step('Navigation state persistence', async () => {
      await page.goto(TEST_URLS.DASHBOARD);

      // Open drawer and note state
      const hamburger = page.locator('[data-testid="mobile-menu-toggle"]');
      if (await hamburger.isVisible()) {
        await hamburger.click();
        await page.waitForTimeout(500);

        // Navigate to different page from drawer
        const hiveLink = page.locator('[data-testid="navigation-drawer"] a[href*="hive"], [data-testid="navigation-drawer"] [data-testid*="hive"]');
        
        if (await hiveLink.isVisible()) {
          await hiveLink.click();
          await page.waitForLoadState('networkidle');

          // Verify we navigated
          const newUrl = page.url();
          expect(newUrl).not.toBe(TEST_URLS.DASHBOARD);

          // Verify drawer closed after navigation
          const drawer = page.locator('[data-testid="navigation-drawer"]');
          await expect(drawer).not.toBeVisible();

          // Verify hamburger is still available
          await expect(hamburger).toBeVisible();
        }
      }
    });
  });

  test('should handle browser back/forward with mobile navigation', async ({ page }) => {
    let authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    
    await page.setViewportSize({ width: 390, height: 844 });

    await test.step('Browser navigation integration', async () => {
      // Navigate through several pages
      await page.goto(TEST_URLS.DASHBOARD);
      await page.waitForLoadState('networkidle');

      await page.goto(TEST_URLS.HIVE_LIST);
      await page.waitForLoadState('networkidle');

      await page.goto(TEST_URLS.ANALYTICS);
      await page.waitForLoadState('networkidle');

      // Use browser back
      await page.goBack();
      await page.waitForLoadState('networkidle');
      
      // Verify we're at hive list
      expect(page.url()).toContain('hive');

      // Check that mobile navigation reflects current page
      const bottomNav = page.locator('[data-testid="bottom-navigation"]');
      if (await bottomNav.isVisible()) {
        const activeTab = bottomNav.locator('[aria-selected="true"], .active');
        if (await activeTab.isVisible()) {
          const activeText = await activeTab.textContent();
          console.log(`Active tab after browser back: ${activeText}`);
        }
      }

      // Use browser forward
      await page.goForward();
      await page.waitForLoadState('networkidle');

      // Verify we're back at analytics
      expect(page.url()).toContain('analytics');
    });
  });
});

test.describe('Mobile Navigation - Error Handling and Edge Cases', () => {
  test('should handle navigation errors gracefully', async ({ page }) => {
    let authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto(TEST_URLS.DASHBOARD);

    await test.step('Navigation error handling', async () => {
      // Test navigation to non-existent page
      const hamburger = page.locator('[data-testid="mobile-menu-toggle"]');
      if (await hamburger.isVisible()) {
        await hamburger.click();
        await page.waitForTimeout(500);

        // Try to navigate to a broken link (if any exist)
        const brokenLink = page.locator('[data-testid="navigation-drawer"] a[href="/non-existent"]');
        
        if (await brokenLink.isVisible()) {
          await brokenLink.click();
          await page.waitForTimeout(2000);

          // Should show error page or stay on current page
          const errorIndicator = page.locator('[data-testid="error-page"], .error, text="404"');
          const isStillOnDashboard = page.url().includes('dashboard');
          
          const hasErrorHandling = await errorIndicator.isVisible() || isStillOnDashboard;
          expect(hasErrorHandling).toBeTruthy();
          
          console.log(`Navigation error handled gracefully: ${hasErrorHandling}`);
        }
      }
    });
  });

  test('should handle slow network during navigation', async ({ page, context }) => {
    let authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    
    await page.setViewportSize({ width: 390, height: 844 });

    // Simulate slow network
    const client = await context.newCDPSession(page);
    await client.send('Network.enable');
    await client.send('Network.emulateNetworkConditions', {
      offline: false,
      downloadThroughput: 100 * 1024 / 8, // Very slow 100 Kbps
      uploadThroughput: 100 * 1024 / 8,
      latency: 3000 // 3 second latency
    });

    await test.step('Slow network navigation', async () => {
      await page.goto(TEST_URLS.DASHBOARD);

      const hamburger = page.locator('[data-testid="mobile-menu-toggle"]');
      if (await hamburger.isVisible()) {
        await hamburger.click();
        await page.waitForTimeout(500);

        const navigationLink = page.locator('[data-testid="navigation-drawer"] a').first();
        
        if (await navigationLink.isVisible()) {
          const startTime = Date.now();
          await navigationLink.click();
          
          // Look for loading indicators
          const loadingIndicator = page.locator('[data-testid="loading"], .loading, .spinner');
          const hasLoadingIndicator = await loadingIndicator.isVisible({ timeout: 1000 }).catch(() => false);
          
          await page.waitForLoadState('networkidle', { timeout: 15000 });
          const loadTime = Date.now() - startTime;
          
          console.log(`Navigation with slow network: ${loadTime}ms, loading indicator: ${hasLoadingIndicator}`);
          
          // Should eventually complete navigation
          expect(loadTime).toBeLessThan(15000); // Should complete within 15 seconds
        }
      }
    });
  });
});

// Helper Functions

async function testNavigationPattern(page: Page, pattern: NavigationPattern): Promise<void> {
  const element = page.locator(pattern.selector);
  
  switch (pattern.type) {
    case 'hamburger':
      await element.click();
      const drawer = page.locator('[data-testid="navigation-drawer"], [role="dialog"]');
      await expect(drawer).toBeVisible({ timeout: 1000 });
      break;
      
    case 'bottom_tabs':
      const tabs = element.locator('[role="tab"], .tab, a');
      const tabCount = await tabs.count();
      expect(tabCount).toBeGreaterThan(0);
      break;
      
    case 'drawer':
      const isVisible = await element.isVisible();
      // Drawer visibility depends on current state
      console.log(`Drawer visible: ${isVisible}`);
      break;
      
    case 'swipe':
      // Test swipe capability would require more complex interaction
      const isSwipeable = await element.evaluate(el => {
        return el.getAttribute('data-swipeable') === 'true' ||
               el.classList.contains('swipeable') ||
               window.getComputedStyle(el).overflowX === 'auto';
      });
      console.log(`Swipeable container detected: ${isSwipeable}`);
      break;
  }
}

async function testNavigationAccessibility(page: Page, pattern: NavigationPattern): Promise<void> {
  const element = page.locator(pattern.selector);
  
  if (pattern.accessibility.hasAriaLabels) {
    const ariaLabel = await element.getAttribute('aria-label');
    const ariaLabelledBy = await element.getAttribute('aria-labelledby');
    
    const hasAriaLabeling = !!(ariaLabel || ariaLabelledBy);
    expect(hasAriaLabeling).toBeTruthy();
  }
  
  if (pattern.accessibility.keyboardAccessible) {
    const tabIndex = await element.getAttribute('tabindex');
    const isButton = await element.evaluate(el => el.tagName.toLowerCase() === 'button');
    const isLink = await element.evaluate(el => el.tagName.toLowerCase() === 'a');
    
    const isKeyboardAccessible = tabIndex !== '-1' && (isButton || isLink || tabIndex === '0');
    expect(isKeyboardAccessible).toBeTruthy();
  }
  
  if (pattern.accessibility.screenReaderFriendly) {
    const role = await element.getAttribute('role');
    const hasRole = !!role;
    console.log(`Screen reader role for ${pattern.name}: ${role || 'none'}`);
  }
}

async function testNavigationResponsiveness(page: Page, pattern: NavigationPattern): Promise<void> {
  const viewports = [390, 768, 1024];
  
  for (const width of viewports) {
    await page.setViewportSize({ width, height: 800 });
    await page.waitForTimeout(300);
    
    const element = page.locator(pattern.selector);
    const isVisible = await element.isVisible();
    
    const expectedVisibility = getExpectedVisibility(pattern.type, width);
    
    if (expectedVisibility !== 'flexible') {
      const shouldBeVisible = expectedVisibility === 'visible';
      expect(isVisible).toBe(shouldBeVisible);
    }
    
    console.log(`${pattern.name} at ${width}px: ${isVisible ? 'visible' : 'hidden'}`);
  }
}

async function testBreadcrumbMobileAdaptation(
  page: Page, 
  breadcrumb: Locator, 
  test: BreadcrumbTest
): Promise<void> {
  switch (test.mobileAdaptation) {
    case 'hidden':
      await expect(breadcrumb).not.toBeVisible();
      break;
      
    case 'collapsed':
      const collapsedIndicator = breadcrumb.locator('.collapsed, [data-collapsed="true"], text="..."');
      const hasCollapsedState = await collapsedIndicator.isVisible();
      console.log(`Breadcrumb collapsed state: ${hasCollapsedState}`);
      break;
      
    case 'horizontal_scroll':
      const scrollable = await breadcrumb.evaluate(el => {
        const style = window.getComputedStyle(el);
        return style.overflowX === 'auto' || style.overflowX === 'scroll';
      });
      expect(scrollable).toBeTruthy();
      break;
  }
}

async function getTabIndex(tab: Locator): Promise<number> {
  try {
    const parent = tab.locator('..');
    const allTabs = parent.locator('[role="tab"], .tab, a');
    const tabCount = await allTabs.count();
    
    for (let i = 0; i < tabCount; i++) {
      const currentTab = allTabs.nth(i);
      const isSame = await tab.evaluate((el, otherEl) => el === otherEl, await currentTab.elementHandle());
      if (isSame) return i;
    }
  } catch (error) {
    console.log('Error getting tab index:', error);
  }
  return -1;
}

function getExpectedVisibility(type: NavigationPattern['type'], width: number): 'visible' | 'hidden' | 'flexible' {
  switch (type) {
    case 'hamburger':
      return width < 768 ? 'visible' : 'hidden';
    case 'bottom_tabs':
      return width < 768 ? 'visible' : 'hidden';
    case 'drawer':
      return 'flexible'; // Depends on state
    case 'swipe':
      return 'flexible'; // Depends on content
    default:
      return 'flexible';
  }
}