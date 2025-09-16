/**
 * CSS Compatibility Tests
 * Tests CSS features and styling across different browsers with visual regression
 */

import {expect, test} from '@playwright/test';
import {testCSSSupport} from './browser-helpers';

test.describe('CSS Layout Features', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should support CSS Flexbox', async ({page}) => {
    const flexboxSupport = await testCSSSupport(page, 'display', 'flex');
    expect(flexboxSupport).toBe(true);

    // Test practical flexbox usage
    await page.setContent(`
      <div style="display: flex; justify-content: space-between; align-items: center; width: 300px; height: 100px; border: 1px solid black;">
        <div style="background: red; width: 50px; height: 50px;"></div>
        <div style="background: green; width: 50px; height: 50px;"></div>
        <div style="background: blue; width: 50px; height: 50px;"></div>
      </div>
    `);

    const container = page.locator('div').first();
    const boundingBox = await container.boundingBox();
    expect(boundingBox?.width).toBeCloseTo(300, 1);
    expect(boundingBox?.height).toBeCloseTo(100, 1);

    // Take screenshot for visual regression
    await expect(container).toHaveScreenshot('flexbox-layout.png');
  });

  test('should support CSS Grid', async ({page}) => {
    const gridSupport = await testCSSSupport(page, 'display', 'grid');
    expect(gridSupport).toBe(true);

    // Test practical grid usage
    await page.setContent(`
      <div style="display: grid; grid-template-columns: 1fr 1fr; grid-gap: 10px; width: 300px; height: 200px; border: 1px solid black;">
        <div style="background: red; grid-column: 1; grid-row: 1;"></div>
        <div style="background: green; grid-column: 2; grid-row: 1;"></div>
        <div style="background: blue; grid-column: 1 / span 2; grid-row: 2;"></div>
      </div>
    `);

    const container = page.locator('div').first();
    const boundingBox = await container.boundingBox();
    expect(boundingBox?.width).toBeCloseTo(300, 1);

    // Take screenshot for visual regression
    await expect(container).toHaveScreenshot('grid-layout.png');
  });

  test('should support CSS Custom Properties (Variables)', async ({page}) => {
    const customPropsSupport = await testCSSSupport(page, 'color', 'var(--test-color)');
    expect(customPropsSupport).toBe(true);

    // Test practical custom properties usage
    await page.setContent(`
      <style>
        :root {
          --primary-color: #007bff;
          --secondary-color: #6c757d;
          --border-radius: 8px;
          --font-size: 16px;
        }
        .test-element {
          color: var(--primary-color);
          background-color: var(--secondary-color);
          border-radius: var(--border-radius);
          font-size: var(--font-size);
          padding: 20px;
          width: 200px;
        }
      </style>
      <div class="test-element">Custom Properties Test</div>
    `);

    const element = page.locator('.test-element');
    const computedStyle = await element.evaluate((el) => {
      const style = window.getComputedStyle(el);
      return {
        color: style.color,
        backgroundColor: style.backgroundColor,
        borderRadius: style.borderRadius,
        fontSize: style.fontSize
      };
    });

    expect(computedStyle.borderRadius).toBe('8px');
    expect(computedStyle.fontSize).toBe('16px');

    // Take screenshot for visual regression
    await expect(element).toHaveScreenshot('css-custom-properties.png');
  });

  test('should support CSS Sticky positioning', async ({page}) => {
    const stickySupport = await testCSSSupport(page, 'position', 'sticky');
    expect(stickySupport).toBe(true);

    // Test sticky positioning behavior
    await page.setContent(`
      <div style="height: 200px; overflow: auto; border: 1px solid black;">
        <div style="height: 100px; background: lightgray;">Content before sticky</div>
        <div id="sticky" style="position: sticky; top: 0; background: yellow; height: 50px; border: 1px solid red;">
          Sticky Element
        </div>
        <div style="height: 300px; background: lightblue;">Content after sticky (scroll to test)</div>
      </div>
    `);

    const stickyElement = page.locator('#sticky');
    await expect(stickyElement).toBeVisible();

    // Scroll the container to test sticky behavior
    await page.locator('div').first().evaluate((el) => {
      el.scrollTop = 50;
    });

    await page.waitForTimeout(100);
    await expect(stickyElement).toBeVisible();
  });
});

test.describe('CSS Visual Features', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/');
  });

  test('should support CSS Transforms', async ({page}) => {
    const transformSupport = await testCSSSupport(page, 'transform', 'rotate(45deg)');
    expect(transformSupport).toBe(true);

    // Test various transforms
    await page.setContent(`
      <div style="display: flex; gap: 20px; padding: 50px;">
        <div style="width: 50px; height: 50px; background: red; transform: rotate(45deg);"></div>
        <div style="width: 50px; height: 50px; background: green; transform: scale(1.5);"></div>
        <div style="width: 50px; height: 50px; background: blue; transform: translate(20px, 10px);"></div>
        <div style="width: 50px; height: 50px; background: orange; transform: skew(10deg, 5deg);"></div>
      </div>
    `);

    const container = page.locator('div').first();
    await expect(container).toHaveScreenshot('css-transforms.png');
  });

  test('should support CSS Animations', async ({page}) => {
    const animationSupport = await testCSSSupport(page, 'animation', 'test 1s ease-in-out');
    expect(animationSupport).toBe(true);

    // Test CSS animation
    await page.setContent(`
      <style>
        @keyframes fadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        .animated {
          width: 100px;
          height: 100px;
          background: red;
          animation: fadeIn 0.5s ease-in-out;
        }
      </style>
      <div class="animated"></div>
    `);

    const animatedElement = page.locator('.animated');
    await expect(animatedElement).toBeVisible();

    // Wait for animation to complete
    await page.waitForTimeout(600);
    await expect(animatedElement).toHaveScreenshot('css-animation.png');
  });

  test('should support CSS Transitions', async ({page}) => {
    const transitionSupport = await testCSSSupport(page, 'transition', 'all 0.3s ease');
    expect(transitionSupport).toBe(true);

    // Test CSS transitions
    await page.setContent(`
      <style>
        .transition-test {
          width: 100px;
          height: 100px;
          background: blue;
          transition: all 0.3s ease;
          cursor: pointer;
        }
        .transition-test:hover {
          width: 150px;
          height: 150px;
          background: red;
          border-radius: 50%;
        }
      </style>
      <div class="transition-test"></div>
    `);

    const element = page.locator('.transition-test');

    // Take initial screenshot
    await expect(element).toHaveScreenshot('css-transition-before.png');

    // Trigger hover to start transition
    await element.hover();
    await page.waitForTimeout(400); // Wait for transition to complete

    // Take final screenshot
    await expect(element).toHaveScreenshot('css-transition-after.png');
  });

  test('should support CSS Filters', async ({page}) => {
    const filterSupport = await testCSSSupport(page, 'filter', 'blur(5px)');
    expect(filterSupport).toBe(true);

    // Test various CSS filters
    await page.setContent(`
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; padding: 20px;">
        <div style="width: 100px; height: 100px; background: url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjMDA3YmZmIi8+PC9zdmc+'); filter: blur(2px);">Blur</div>
        <div style="width: 100px; height: 100px; background: url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjMjhhNzQ1Ii8+PC9zdmc+'); filter: brightness(1.5);">Brightness</div>
        <div style="width: 100px; height: 100px; background: url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZmZjMTA3Ii8+PC9zdmc+'); filter: contrast(2);">Contrast</div>
        <div style="width: 100px; height: 100px; background: url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZGM5NTQ1Ii8+PC9zdmc+'); filter: grayscale(100%);">Grayscale</div>
        <div style="width: 100px; height: 100px; background: url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjNmY0MmMxIi8+PC9zdmc+'); filter: hue-rotate(90deg);">Hue Rotate</div>
        <div style="width: 100px; height: 100px; background: url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZTc0YzNjIi8+PC9zdmc+'); filter: drop-shadow(5px 5px 10px rgba(0,0,0,0.5));">Drop Shadow</div>
      </div>
    `);

    const container = page.locator('div').first();
    await expect(container).toHaveScreenshot('css-filters.png');
  });

  test('should support Backdrop Filter', async ({page, browserName}) => {
    const backdropFilterSupport = await testCSSSupport(page, 'backdrop-filter', 'blur(10px)');

    // Safari and some browsers support backdrop-filter
    if (backdropFilterSupport) {
      await page.setContent(`
        <div style="position: relative; width: 300px; height: 200px; background: linear-gradient(45deg, red, blue, green);">
          <div style="position: absolute; top: 50px; left: 50px; width: 200px; height: 100px; 
                      backdrop-filter: blur(10px); background: rgba(255,255,255,0.2); 
                      border: 1px solid rgba(255,255,255,0.3); border-radius: 10px;
                      display: flex; align-items: center; justify-content: center;">
            Backdrop Filter
          </div>
        </div>
      `);

      const container = page.locator('div').first();
      await expect(container).toHaveScreenshot('css-backdrop-filter.png');
    } else {
      console.log(`Backdrop filter not supported in ${browserName}`);
    }
  });
});

test.describe('CSS Responsive Features', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/');
  });

  test('should support CSS Media Queries', async ({page}) => {
    await page.setContent(`
      <style>
        .responsive-box {
          width: 200px;
          height: 100px;
          background: blue;
          color: white;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        
        @media (max-width: 768px) {
          .responsive-box {
            background: red;
            width: 150px;
          }
        }
        
        @media (max-width: 480px) {
          .responsive-box {
            background: green;
            width: 100px;
          }
        }
        
        @media (prefers-color-scheme: dark) {
          .responsive-box {
            border: 2px solid white;
          }
        }
      </style>
      <div class="responsive-box">Responsive</div>
    `);

    const element = page.locator('.responsive-box');

    // Test desktop view
    await page.setViewportSize({width: 1024, height: 768});
    await expect(element).toHaveScreenshot('responsive-desktop.png');

    // Test tablet view
    await page.setViewportSize({width: 768, height: 1024});
    await page.waitForTimeout(100);
    await expect(element).toHaveScreenshot('responsive-tablet.png');

    // Test mobile view
    await page.setViewportSize({width: 375, height: 667});
    await page.waitForTimeout(100);
    await expect(element).toHaveScreenshot('responsive-mobile.png');
  });

  test('should support Container Queries', async ({page}) => {
    const containerQuerySupport = await testCSSSupport(page, 'container-type', 'inline-size');

    if (containerQuerySupport) {
      await page.setContent(`
        <style>
          .container {
            container-type: inline-size;
            width: 300px;
            border: 1px solid black;
            resize: horizontal;
            overflow: auto;
          }
          
          .item {
            background: blue;
            color: white;
            padding: 20px;
            text-align: center;
          }
          
          @container (max-width: 200px) {
            .item {
              background: red;
              font-size: 14px;
            }
          }
        </style>
        <div class="container">
          <div class="item">Container Query Item</div>
        </div>
      `);

      const container = page.locator('.container');
      await expect(container).toHaveScreenshot('container-queries.png');
    } else {
      console.log('Container queries not supported in this browser');
    }
  });

  test('should support CSS Aspect Ratio', async ({page}) => {
    const aspectRatioSupport = await testCSSSupport(page, 'aspect-ratio', '16/9');

    if (aspectRatioSupport) {
      await page.setContent(`
        <style>
          .aspect-ratio-box {
            width: 300px;
            aspect-ratio: 16/9;
            background: linear-gradient(45deg, purple, pink);
            border: 2px solid black;
          }
        </style>
        <div class="aspect-ratio-box"></div>
      `);

      const element = page.locator('.aspect-ratio-box');
      const boundingBox = await element.boundingBox();

      if (boundingBox) {
        const expectedHeight = (boundingBox.width * 9) / 16;
        expect(boundingBox.height).toBeCloseTo(expectedHeight, 1);
      }

      await expect(element).toHaveScreenshot('css-aspect-ratio.png');
    } else {
      console.log('CSS aspect-ratio not supported in this browser');
    }
  });
});

test.describe('CSS Typography Features', () => {
  test.beforeEach(async ({page}) => {
    await page.goto('/');
  });

  test('should support Web Fonts loading', async ({page}) => {
    await page.setContent(`
      <style>
        @import url('https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;700&display=swap');
        
        .font-test {
          font-family: 'Roboto', sans-serif;
          font-size: 24px;
          padding: 20px;
        }
        
        .font-weight-light { font-weight: 300; }
        .font-weight-normal { font-weight: 400; }
        .font-weight-bold { font-weight: 700; }
      </style>
      <div class="font-test font-weight-light">Light Text</div>
      <div class="font-test font-weight-normal">Normal Text</div>
      <div class="font-test font-weight-bold">Bold Text</div>
    `);

    // Wait for fonts to load
    await page.waitForTimeout(2000);

    const container = page.locator('body');
    await expect(container).toHaveScreenshot('web-fonts.png');
  });

  test('should support CSS Text Features', async ({page}) => {
    await page.setContent(`
      <style>
        .text-features {
          width: 300px;
          padding: 20px;
          border: 1px solid black;
          margin: 10px 0;
        }
        
        .text-overflow {
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        
        .text-shadow {
          text-shadow: 2px 2px 4px rgba(0,0,0,0.5);
          font-size: 24px;
          color: white;
          background: darkblue;
        }
        
        .text-stroke {
          -webkit-text-stroke: 2px black;
          color: yellow;
          font-size: 24px;
          font-weight: bold;
        }
        
        .text-gradient {
          background: linear-gradient(45deg, red, blue);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          font-size: 24px;
          font-weight: bold;
        }
      </style>
      <div class="text-features text-overflow">This is a very long text that should be truncated with ellipsis</div>
      <div class="text-features text-shadow">Text with Shadow</div>
      <div class="text-features text-stroke">Text with Stroke</div>
      <div class="text-features text-gradient">Gradient Text</div>
    `);

    const container = page.locator('body');
    await expect(container).toHaveScreenshot('css-text-features.png');
  });
});

test.describe('Browser-Specific CSS Behavior', () => {
  test('should handle vendor prefixes correctly', async ({page, browserName}) => {
    await page.setContent(`
      <style>
        .vendor-prefix-test {
          width: 100px;
          height: 100px;
          background: red;
          
          /* Standard */
          transform: rotate(45deg);
          transition: transform 0.3s ease;
          
          /* Webkit prefixes (Safari, Chrome) */
          -webkit-transform: rotate(45deg);
          -webkit-transition: -webkit-transform 0.3s ease;
          
          /* Mozilla prefix (Firefox) */
          -moz-transform: rotate(45deg);
          -moz-transition: -moz-transform 0.3s ease;
          
          /* Microsoft prefix (Edge legacy) */
          -ms-transform: rotate(45deg);
          -ms-transition: -ms-transform 0.3s ease;
        }
        
        .vendor-prefix-test:hover {
          transform: rotate(90deg);
          -webkit-transform: rotate(90deg);
          -moz-transform: rotate(90deg);
          -ms-transform: rotate(90deg);
        }
      </style>
      <div class="vendor-prefix-test"></div>
    `);

    const element = page.locator('.vendor-prefix-test');
    await expect(element).toHaveScreenshot(`vendor-prefixes-${browserName}.png`);

    // Test hover state
    await element.hover();
    await page.waitForTimeout(400);
    await expect(element).toHaveScreenshot(`vendor-prefixes-hover-${browserName}.png`);
  });

  test('should test Material UI component rendering', async ({page}) => {
    // Navigate to a page that uses Material UI components
    await page.goto('/login'); // Assuming login page uses MUI components

    // Wait for components to render
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    // Test that MUI components render correctly
    const muiButton = page.locator('button').first();
    if (await muiButton.count() > 0) {
      await expect(muiButton).toBeVisible();

      // Take screenshot of MUI components
      await expect(page).toHaveScreenshot('material-ui-components.png', {
        fullPage: true,
        mask: [page.locator('[data-testid="dynamic-content"]')] // Mask dynamic content
      });
    }
  });

  test('should test dark mode support', async ({page}) => {
    // Test system preference for dark mode
    await page.emulateMedia({colorScheme: 'dark'});

    await page.setContent(`
      <style>
        body {
          background: white;
          color: black;
          transition: background-color 0.3s, color 0.3s;
        }
        
        @media (prefers-color-scheme: dark) {
          body {
            background: #1a1a1a;
            color: white;
          }
        }
        
        .theme-test {
          padding: 40px;
          border: 1px solid currentColor;
          margin: 20px;
        }
      </style>
      <div class="theme-test">
        <h1>Dark Mode Test</h1>
        <p>This content should adapt to dark mode preferences.</p>
      </div>
    `);

    await expect(page).toHaveScreenshot('dark-mode-test.png');

    // Test light mode
    await page.emulateMedia({colorScheme: 'light'});
    await page.waitForTimeout(400); // Wait for transition
    await expect(page).toHaveScreenshot('light-mode-test.png');
  });

  test('should test high contrast mode', async ({page}) => {
    // Emulate high contrast preferences
    await page.emulateMedia({forcedColors: 'active'});

    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    // Test that the page is still usable in high contrast mode
    const mainContent = page.locator('main, [role="main"], .main-content').first();
    if (await mainContent.count() > 0) {
      await expect(mainContent).toBeVisible();
      await expect(page).toHaveScreenshot('high-contrast-mode.png', {
        fullPage: true
      });
    }
  });

  test('should test print styles', async ({page}) => {
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle');

    // Emulate print media
    await page.emulateMedia({media: 'print'});
    await page.waitForTimeout(500);

    // Take screenshot of print styles
    await expect(page).toHaveScreenshot('print-styles.png', {
      fullPage: true
    });
  });
});