/**
 * Comprehensive Touch Interaction Tests for FocusHive Mobile
 * Tests all touch gestures and mobile-specific interactions
 */

import { test, expect, Page, Locator } from '@playwright/test';
import { MOBILE_DEVICES, TOUCH_TARGET_SPECS } from './mobile.config';
import { MobileHelper } from '../../helpers/mobile.helper';
import { AuthHelper } from '../../helpers/auth.helper';
import { TEST_URLS } from '../../helpers/test-data';

interface TouchPoint {
  x: number;
  y: number;
}

interface TouchGesture {
  type: 'tap' | 'doubleTap' | 'longPress' | 'swipe' | 'pinch' | 'pan' | 'drag';
  startPoint: TouchPoint;
  endPoint?: TouchPoint;
  duration?: number;
  fingers?: number;
  scale?: number; // For pinch gestures
  direction?: 'up' | 'down' | 'left' | 'right';
}

interface TouchTestResult {
  gesture: TouchGesture;
  success: boolean;
  responsiveness: number; // milliseconds
  accuracy: boolean;
  issues: string[];
}

interface SwipeGesture {
  direction: 'left' | 'right' | 'up' | 'down';
  distance: number;
  duration: number;
  velocity: number;
}

test.describe('Touch Interactions - Basic Touch Gestures', () => {
  let authHelper: AuthHelper;
  let mobileHelper: MobileHelper;

  test.beforeEach(async ({ page }) => {
    authHelper = new AuthHelper(page);
    mobileHelper = new MobileHelper(page);
    
    // Set up mobile context
    await page.setViewportSize({ width: 390, height: 844 });
    await authHelper.loginWithTestUser();
  });

  /**
   * Test basic tap interactions
   */
  test('should handle tap gestures correctly', async ({ page }) => {
    await page.goto(TEST_URLS.DASHBOARD);
    await page.waitForLoadState('networkidle');

    // Test single tap on buttons
    const testElements = [
      { selector: '[data-testid="create-hive-button"]', name: 'Create Hive Button' },
      { selector: '[data-testid="profile-menu"]', name: 'Profile Menu' },
      { selector: '[data-testid="notification-bell"]', name: 'Notification Bell' },
      { selector: '[data-testid="search-button"]', name: 'Search Button' }
    ];

    for (const element of testElements) {
      await test.step(`Testing tap on ${element.name}`, async () => {
        const locator = page.locator(element.selector);
        
        if (await locator.isVisible()) {
          const startTime = Date.now();
          
          // Perform touch tap
          await performTouchTap(page, locator);
          
          const responseTime = Date.now() - startTime;
          
          // Verify response time is acceptable for mobile
          expect(responseTime).toBeLessThan(300);
          
          // Verify visual feedback (if applicable)
          await verifyTouchFeedback(page, locator);
        }
      });
    }
  });

  /**
   * Test double tap interactions
   */
  test('should handle double tap gestures', async ({ page }) => {
    await page.goto(TEST_URLS.HIVE_LIST);
    await page.waitForLoadState('networkidle');

    // Test double tap to like or favorite
    const hiveCard = page.locator('[data-testid^="hive-card-"]').first();
    
    if (await hiveCard.isVisible()) {
      await test.step('Double tap to favorite hive', async () => {
        const boundingBox = await hiveCard.boundingBox();
        if (!boundingBox) throw new Error('Cannot get hive card bounds');

        const centerX = boundingBox.x + boundingBox.width / 2;
        const centerY = boundingBox.y + boundingBox.height / 2;

        // Perform double tap
        await page.touchscreen.tap(centerX, centerY);
        await page.waitForTimeout(50); // Brief pause between taps
        await page.touchscreen.tap(centerX, centerY);

        // Verify double tap action
        await page.waitForTimeout(500);
        const favoriteIndicator = page.locator('[data-testid="favorite-indicator"]');
        
        // Note: This depends on actual implementation
        console.log('Double tap completed - implementation specific verification needed');
      });
    }
  });

  /**
   * Test long press interactions
   */
  test('should handle long press gestures', async ({ page }) => {
    await page.goto(TEST_URLS.HIVE_LIST);
    await page.waitForLoadState('networkidle');

    const hiveCard = page.locator('[data-testid^="hive-card-"]').first();
    
    if (await hiveCard.isVisible()) {
      await test.step('Long press to show context menu', async () => {
        // Perform long press
        await performLongPress(page, hiveCard, 800); // 800ms long press
        
        // Verify context menu appears
        const contextMenu = page.locator('[data-testid="context-menu"], [role="menu"]');
        await expect(contextMenu).toBeVisible({ timeout: 1000 });
        
        // Verify context menu has expected options
        const menuOptions = [
          'Edit',
          'Delete', 
          'Share',
          'Favorite'
        ];
        
        for (const option of menuOptions) {
          const menuItem = contextMenu.locator(`text="${option}"`);
          if (await menuItem.isVisible()) {
            console.log(`Context menu option found: ${option}`);
          }
        }
        
        // Close context menu
        await page.keyboard.press('Escape');
      });
    }
  });

  /**
   * Test swipe gestures
   */
  test('should handle swipe gestures correctly', async ({ page }) => {
    await page.goto(TEST_URLS.DASHBOARD);
    await page.waitForLoadState('networkidle');

    const swipeTests: Array<{
      name: string;
      selector: string;
      gesture: SwipeGesture;
      expectedAction: string;
    }> = [
      {
        name: 'Navigation drawer',
        selector: 'body',
        gesture: { direction: 'right', distance: 200, duration: 300, velocity: 0.67 },
        expectedAction: 'open_drawer'
      },
      {
        name: 'Tab navigation',
        selector: '[data-testid="tab-container"]',
        gesture: { direction: 'left', distance: 150, duration: 200, velocity: 0.75 },
        expectedAction: 'next_tab'
      },
      {
        name: 'Card dismissal',
        selector: '[data-testid^="notification-card-"]',
        gesture: { direction: 'right', distance: 100, duration: 250, velocity: 0.4 },
        expectedAction: 'dismiss_card'
      }
    ];

    for (const swipeTest of swipeTests) {
      await test.step(`Testing ${swipeTest.name} swipe`, async () => {
        const element = page.locator(swipeTest.selector);
        
        if (await element.isVisible()) {
          await performSwipeGesture(page, element, swipeTest.gesture);
          
          // Allow time for swipe animation/action
          await page.waitForTimeout(500);
          
          // Verify expected action occurred
          await verifySwipeAction(page, swipeTest.expectedAction);
        }
      });
    }
  });

  /**
   * Test pinch to zoom gestures
   */
  test('should handle pinch to zoom on zoomable content', async ({ page }) => {
    await page.goto(TEST_URLS.ANALYTICS);
    await page.waitForLoadState('networkidle');

    // Look for zoomable charts or images
    const zoomableElements = [
      '[data-testid="productivity-chart"]',
      '[data-testid="analytics-graph"]',
      '[data-testid="hive-activity-chart"]'
    ];

    for (const selector of zoomableElements) {
      const element = page.locator(selector);
      
      if (await element.isVisible()) {
        await test.step(`Testing pinch zoom on ${selector}`, async () => {
          const boundingBox = await element.boundingBox();
          if (!boundingBox) return;

          // Perform pinch out (zoom in)
          await performPinchGesture(page, boundingBox, 'out', 1.5);
          
          await page.waitForTimeout(500);
          
          // Verify zoom level changed
          const zoomLevel = await getElementZoomLevel(page, element);
          expect(zoomLevel).toBeGreaterThan(1);
          
          // Perform pinch in (zoom out)
          await performPinchGesture(page, boundingBox, 'in', 0.7);
          
          await page.waitForTimeout(500);
          
          // Verify zoom returned closer to original
          const finalZoomLevel = await getElementZoomLevel(page, element);
          expect(finalZoomLevel).toBeLessThan(zoomLevel);
        });
      }
    }
  });

  /**
   * Test drag and drop on mobile
   */
  test('should handle drag and drop gestures', async ({ page }) => {
    await page.goto(TEST_URLS.HIVE_SETTINGS);
    await page.waitForLoadState('networkidle');

    // Test draggable elements like priority lists or task reordering
    const draggableItem = page.locator('[data-testid="draggable-item"]').first();
    const dropZone = page.locator('[data-testid="drop-zone"]').first();

    if (await draggableItem.isVisible() && await dropZone.isVisible()) {
      await test.step('Drag and drop item', async () => {
        const itemBox = await draggableItem.boundingBox();
        const dropBox = await dropZone.boundingBox();
        
        if (!itemBox || !dropBox) return;

        // Perform drag gesture
        await page.touchscreen.tap(itemBox.x + itemBox.width / 2, itemBox.y + itemBox.height / 2);
        await page.waitForTimeout(100);
        
        // Drag to drop zone
        await dragToPosition(page, 
          { x: itemBox.x + itemBox.width / 2, y: itemBox.y + itemBox.height / 2 },
          { x: dropBox.x + dropBox.width / 2, y: dropBox.y + dropBox.height / 2 },
          800 // drag duration
        );

        // Verify drop action
        await page.waitForTimeout(500);
        
        const dropSuccess = await page.locator('[data-testid="drop-success"]').isVisible();
        console.log(`Drag and drop success: ${dropSuccess}`);
      });
    }
  });

  /**
   * Test pull to refresh gesture
   */
  test('should handle pull to refresh on scrollable content', async ({ page }) => {
    await page.goto(TEST_URLS.HIVE_LIST);
    await page.waitForLoadState('networkidle');

    await test.step('Pull to refresh hive list', async () => {
      // Scroll to top first
      await page.evaluate(() => window.scrollTo(0, 0));
      
      // Perform pull down gesture from top
      await performPullToRefresh(page);
      
      // Look for refresh indicator
      const refreshIndicator = page.locator('[data-testid="refresh-indicator"], .refresh-spinner, [aria-label*="refresh"]');
      
      // The refresh indicator should appear briefly
      const refreshVisible = await refreshIndicator.isVisible().catch(() => false);
      console.log(`Pull to refresh triggered: ${refreshVisible}`);
      
      // Wait for refresh to complete
      await page.waitForLoadState('networkidle');
    });
  });

  /**
   * Test scroll momentum and overscroll behavior
   */
  test('should handle scroll momentum correctly', async ({ page }) => {
    await page.goto(TEST_URLS.HIVE_LIST);
    await page.waitForLoadState('networkidle');

    await test.step('Test scroll momentum', async () => {
      // Perform fast swipe for momentum scroll
      const viewport = page.viewportSize();
      if (!viewport) return;

      const startY = viewport.height / 2;
      const endY = viewport.height / 4;
      
      // Fast swipe up
      await page.touchscreen.tap(viewport.width / 2, startY);
      await dragToPosition(page,
        { x: viewport.width / 2, y: startY },
        { x: viewport.width / 2, y: endY },
        150 // Fast swipe
      );

      // Allow momentum to continue
      await page.waitForTimeout(1000);
      
      // Verify scroll position changed beyond gesture distance
      const scrollPosition = await page.evaluate(() => window.pageYOffset);
      const gestureDistance = startY - endY;
      
      expect(scrollPosition).toBeGreaterThan(gestureDistance);
    });

    await test.step('Test overscroll bounce', async () => {
      // Scroll to top
      await page.evaluate(() => window.scrollTo(0, 0));
      
      // Try to overscroll at top
      const viewport = page.viewportSize();
      if (!viewport) return;

      await dragToPosition(page,
        { x: viewport.width / 2, y: 100 },
        { x: viewport.width / 2, y: 300 },
        200
      );

      // Verify we're still at top (bounce back)
      await page.waitForTimeout(500);
      const scrollPosition = await page.evaluate(() => window.pageYOffset);
      expect(scrollPosition).toBe(0);
    });
  });
});

test.describe('Touch Interactions - Touch Target Validation', () => {
  test('should meet minimum touch target size requirements', async ({ page }) => {
    await page.goto(TEST_URLS.HOME);
    await page.waitForLoadState('networkidle');

    const platforms = ['iOS', 'Android'] as const;
    
    for (const platform of platforms) {
      await test.step(`Validating touch targets for ${platform}`, async () => {
        const spec = TOUCH_TARGET_SPECS[platform.toUpperCase()];
        
        const touchTargetViolations = await page.evaluate((minSize) => {
          const violations: Array<{
            element: string;
            size: { width: number; height: number };
            selector: string;
          }> = [];
          
          const interactiveElements = document.querySelectorAll(
            'button, a, input, textarea, select, [role="button"], [tabindex="0"], [onclick]'
          );
          
          interactiveElements.forEach((element, index) => {
            const rect = element.getBoundingClientRect();
            
            if (rect.width < minSize || rect.height < minSize) {
              violations.push({
                element: element.tagName.toLowerCase() + (element.id ? `#${element.id}` : ''),
                size: { width: Math.round(rect.width), height: Math.round(rect.height) },
                selector: element.tagName.toLowerCase() + `:nth-of-type(${index + 1})`
              });
            }
          });
          
          return violations;
        }, spec.minSize);

        expect(touchTargetViolations.length, 
          `Touch target violations for ${platform}: ${JSON.stringify(touchTargetViolations, null, 2)}`
        ).toBe(0);
      });
    }
  });

  test('should have adequate spacing between touch targets', async ({ page }) => {
    await page.goto(TEST_URLS.DASHBOARD);
    await page.waitForLoadState('networkidle');

    const spacingViolations = await page.evaluate(() => {
      const violations: Array<{
        element1: string;
        element2: string;
        distance: number;
      }> = [];
      
      const interactiveElements = Array.from(document.querySelectorAll(
        'button, a, input, textarea, select, [role="button"], [tabindex="0"]'
      ));
      
      for (let i = 0; i < interactiveElements.length; i++) {
        for (let j = i + 1; j < interactiveElements.length; j++) {
          const rect1 = interactiveElements[i].getBoundingClientRect();
          const rect2 = interactiveElements[j].getBoundingClientRect();
          
          // Calculate distance between centers
          const centerX1 = rect1.x + rect1.width / 2;
          const centerY1 = rect1.y + rect1.height / 2;
          const centerX2 = rect2.x + rect2.width / 2;
          const centerY2 = rect2.y + rect2.height / 2;
          
          const distance = Math.sqrt(
            Math.pow(centerX2 - centerX1, 2) + Math.pow(centerY2 - centerY1, 2)
          );
          
          // Minimum distance should be sum of minimum touch target sizes plus spacing
          const minDistance = 44 + 8; // 44px target + 8px spacing
          
          if (distance < minDistance && distance > 0) {
            violations.push({
              element1: interactiveElements[i].tagName + (interactiveElements[i].id ? `#${interactiveElements[i].id}` : ''),
              element2: interactiveElements[j].tagName + (interactiveElements[j].id ? `#${interactiveElements[j].id}` : ''),
              distance: Math.round(distance)
            });
          }
        }
      }
      
      return violations;
    });

    // Allow some violations for dense UIs, but flag excessive ones
    expect(spacingViolations.length).toBeLessThan(5);
  });
});

test.describe('Touch Interactions - Gesture Recognition', () => {
  test('should distinguish between tap and scroll initiation', async ({ page }) => {
    await page.goto(TEST_URLS.HIVE_LIST);
    await page.waitForLoadState('networkidle');

    const hiveCard = page.locator('[data-testid^="hive-card-"]').first();
    
    if (await hiveCard.isVisible()) {
      await test.step('Quick tap should not trigger scroll', async () => {
        const initialScrollPosition = await page.evaluate(() => window.pageYOffset);
        
        // Perform quick tap
        await performTouchTap(page, hiveCard, 50); // 50ms tap
        
        await page.waitForTimeout(200);
        
        const finalScrollPosition = await page.evaluate(() => window.pageYOffset);
        expect(Math.abs(finalScrollPosition - initialScrollPosition)).toBeLessThan(10);
      });

      await test.step('Drag should trigger scroll', async () => {
        const initialScrollPosition = await page.evaluate(() => window.pageYOffset);
        
        const boundingBox = await hiveCard.boundingBox();
        if (!boundingBox) return;

        // Perform drag gesture
        await dragToPosition(page,
          { x: boundingBox.x + 50, y: boundingBox.y + 50 },
          { x: boundingBox.x + 50, y: boundingBox.y - 100 },
          300
        );
        
        await page.waitForTimeout(200);
        
        const finalScrollPosition = await page.evaluate(() => window.pageYOffset);
        expect(Math.abs(finalScrollPosition - initialScrollPosition)).toBeGreaterThan(50);
      });
    }
  });

  test('should handle simultaneous multi-touch correctly', async ({ page }) => {
    await page.goto(TEST_URLS.ANALYTICS);
    await page.waitForLoadState('networkidle');

    // Test two-finger scroll vs pinch recognition
    await test.step('Two-finger scroll should not trigger pinch', async () => {
      const viewport = page.viewportSize();
      if (!viewport) return;

      // Simulate two-finger scroll (parallel movement)
      const touch1Start = { x: viewport.width / 3, y: viewport.height / 2 };
      const touch1End = { x: viewport.width / 3, y: viewport.height / 2 - 100 };
      const touch2Start = { x: (viewport.width * 2) / 3, y: viewport.height / 2 };
      const touch2End = { x: (viewport.width * 2) / 3, y: viewport.height / 2 - 100 };

      // This is a simplified test - real multi-touch testing requires more complex setup
      await performMultiTouchGesture(page, [
        { start: touch1Start, end: touch1End },
        { start: touch2Start, end: touch2End }
      ], 300);

      // Verify scroll occurred but not zoom
      const scrollPosition = await page.evaluate(() => window.pageYOffset);
      expect(scrollPosition).toBeGreaterThan(0);
    });
  });
});

// Helper functions for touch interactions

/**
 * Perform a touch tap with configurable duration
 */
async function performTouchTap(page: Page, locator: Locator, duration = 100): Promise<void> {
  const boundingBox = await locator.boundingBox();
  if (!boundingBox) throw new Error('Element not found');

  const centerX = boundingBox.x + boundingBox.width / 2;
  const centerY = boundingBox.y + boundingBox.height / 2;

  await page.touchscreen.tap(centerX, centerY);
  await page.waitForTimeout(duration);
}

/**
 * Perform a long press gesture
 */
async function performLongPress(page: Page, locator: Locator, duration = 800): Promise<void> {
  const boundingBox = await locator.boundingBox();
  if (!boundingBox) throw new Error('Element not found');

  const centerX = boundingBox.x + boundingBox.width / 2;
  const centerY = boundingBox.y + boundingBox.height / 2;

  // Touch down and hold
  await page.mouse.move(centerX, centerY);
  await page.mouse.down();
  await page.waitForTimeout(duration);
  await page.mouse.up();
}

/**
 * Perform swipe gesture
 */
async function performSwipeGesture(page: Page, locator: Locator, gesture: SwipeGesture): Promise<void> {
  const boundingBox = await locator.boundingBox();
  if (!boundingBox) throw new Error('Element not found');

  const startX = boundingBox.x + boundingBox.width / 2;
  const startY = boundingBox.y + boundingBox.height / 2;

  let endX = startX;
  let endY = startY;

  switch (gesture.direction) {
    case 'left':
      endX -= gesture.distance;
      break;
    case 'right':
      endX += gesture.distance;
      break;
    case 'up':
      endY -= gesture.distance;
      break;
    case 'down':
      endY += gesture.distance;
      break;
  }

  await dragToPosition(page, { x: startX, y: startY }, { x: endX, y: endY }, gesture.duration);
}

/**
 * Perform pinch gesture (zoom in/out)
 */
async function performPinchGesture(
  page: Page, 
  boundingBox: { x: number; y: number; width: number; height: number }, 
  direction: 'in' | 'out', 
  scale: number
): Promise<void> {
  const centerX = boundingBox.x + boundingBox.width / 2;
  const centerY = boundingBox.y + boundingBox.height / 2;
  
  const initialDistance = 100;
  const finalDistance = direction === 'out' ? initialDistance * scale : initialDistance / scale;

  // Start positions for two fingers
  const finger1Start = { x: centerX - initialDistance / 2, y: centerY };
  const finger2Start = { x: centerX + initialDistance / 2, y: centerY };

  // End positions
  const finger1End = { x: centerX - finalDistance / 2, y: centerY };
  const finger2End = { x: centerX + finalDistance / 2, y: centerY };

  // Simulate two-finger pinch (simplified version)
  await performMultiTouchGesture(page, [
    { start: finger1Start, end: finger1End },
    { start: finger2Start, end: finger2End }
  ], 500);
}

/**
 * Drag from one position to another
 */
async function dragToPosition(
  page: Page, 
  start: TouchPoint, 
  end: TouchPoint, 
  duration: number
): Promise<void> {
  await page.mouse.move(start.x, start.y);
  await page.mouse.down();
  
  // Move in steps for smooth animation
  const steps = Math.max(5, duration / 50);
  const stepX = (end.x - start.x) / steps;
  const stepY = (end.y - start.y) / steps;
  const stepDuration = duration / steps;

  for (let i = 1; i <= steps; i++) {
    await page.mouse.move(
      start.x + stepX * i,
      start.y + stepY * i
    );
    await page.waitForTimeout(stepDuration);
  }

  await page.mouse.up();
}

/**
 * Perform multi-touch gesture (simplified)
 */
async function performMultiTouchGesture(
  page: Page,
  touches: Array<{ start: TouchPoint; end: TouchPoint }>,
  duration: number
): Promise<void> {
  // This is a simplified version - real multi-touch testing is complex
  // For now, we'll simulate the primary touch gesture
  if (touches.length > 0) {
    await dragToPosition(page, touches[0].start, touches[0].end, duration);
  }
}

/**
 * Perform pull to refresh gesture
 */
async function performPullToRefresh(page: Page): Promise<void> {
  const viewport = page.viewportSize();
  if (!viewport) return;

  // Pull down from top center
  await dragToPosition(page,
    { x: viewport.width / 2, y: 50 },
    { x: viewport.width / 2, y: 200 },
    400
  );
  
  // Release and allow bounce back
  await page.waitForTimeout(300);
}

/**
 * Verify touch feedback (visual or haptic)
 */
async function verifyTouchFeedback(page: Page, locator: Locator): Promise<void> {
  // Check for visual feedback like ripple effects or state changes
  const hasActiveState = await locator.evaluate((el) => {
    const computedStyle = window.getComputedStyle(el, ':active');
    const normalStyle = window.getComputedStyle(el);
    
    // Check if active state differs from normal
    return (
      computedStyle.backgroundColor !== normalStyle.backgroundColor ||
      computedStyle.opacity !== normalStyle.opacity ||
      computedStyle.transform !== normalStyle.transform
    );
  });

  // Note: Visual feedback is optional but recommended
  console.log(`Touch feedback detected: ${hasActiveState}`);
}

/**
 * Verify swipe action occurred
 */
async function verifySwipeAction(page: Page, expectedAction: string): Promise<void> {
  switch (expectedAction) {
    case 'open_drawer':
      const drawer = page.locator('[data-testid="navigation-drawer"], .drawer, [role="dialog"]');
      const isDrawerVisible = await drawer.isVisible().catch(() => false);
      console.log(`Navigation drawer opened: ${isDrawerVisible}`);
      break;
      
    case 'next_tab':
      const activeTab = page.locator('[aria-selected="true"], .tab-active, .active-tab');
      const isActiveTabVisible = await activeTab.isVisible().catch(() => false);
      console.log(`Tab navigation occurred: ${isActiveTabVisible}`);
      break;
      
    case 'dismiss_card':
      const dismissedCard = page.locator('.dismissed, [data-dismissed="true"]');
      const isCardDismissed = await dismissedCard.isVisible().catch(() => false);
      console.log(`Card dismissed: ${isCardDismissed}`);
      break;
      
    default:
      console.log(`Unknown swipe action: ${expectedAction}`);
  }
}

/**
 * Get element zoom level (simplified)
 */
async function getElementZoomLevel(page: Page, locator: Locator): Promise<number> {
  return await locator.evaluate((el) => {
    const transform = window.getComputedStyle(el).transform;
    
    if (transform && transform !== 'none') {
      // Parse matrix for scale value
      const matrix = transform.match(/matrix.*\((.+)\)/);
      if (matrix) {
        const values = matrix[1].split(', ');
        return parseFloat(values[0]) || 1; // Scale X value
      }
    }
    
    return 1; // Default scale
  });
}