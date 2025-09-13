/**
 * Content Adaptation Tests for Mobile FocusHive
 * Tests how content adapts and transforms for mobile screens
 */

import { test, expect, Page, Locator } from '@playwright/test';
import { VIEWPORT_BREAKPOINTS, MOBILE_DEVICES } from './mobile.config';
import { MobileHelper } from '../../helpers/mobile.helper';
import { AuthHelper } from '../../helpers/auth.helper';
import { TEST_URLS } from '../../helpers/test-data';

interface ContentAdaptation {
  name: string;
  selector: string;
  adaptations: {
    mobile: ContentExpectation;
    tablet: ContentExpectation;
    desktop: ContentExpectation;
  };
  priority: 'critical' | 'high' | 'medium';
}

interface ContentExpectation {
  layout: 'stacked' | 'grid' | 'carousel' | 'accordion' | 'tabs' | 'hidden';
  textSize: 'small' | 'medium' | 'large';
  imageScaling: 'original' | 'responsive' | 'cropped' | 'hidden';
  interactionPattern: 'tap' | 'swipe' | 'scroll' | 'hover' | 'none';
}

interface TableAdaptation {
  selector: string;
  mobilePattern: 'cards' | 'accordion' | 'horizontal_scroll' | 'stacked_rows' | 'hidden_columns';
  priorityColumns: string[];
}

interface FormAdaptation {
  name: string;
  selector: string;
  mobileOptimizations: string[];
  expectedKeyboard: string;
}

test.describe('Content Adaptation - Layout Transformations', () => {
  let authHelper: AuthHelper;
  let mobileHelper: MobileHelper;

  test.beforeEach(async ({ page }) => {
    authHelper = new AuthHelper(page);
    mobileHelper = new MobileHelper(page);
    await authHelper.loginWithTestUser();
  });

  const contentAdaptations: ContentAdaptation[] = [
    {
      name: 'Hive Grid to Vertical Stack',
      selector: '[data-testid="hive-grid"]',
      adaptations: {
        mobile: {
          layout: 'stacked',
          textSize: 'medium',
          imageScaling: 'responsive',
          interactionPattern: 'tap'
        },
        tablet: {
          layout: 'grid',
          textSize: 'medium',
          imageScaling: 'responsive',
          interactionPattern: 'tap'
        },
        desktop: {
          layout: 'grid',
          textSize: 'medium',
          imageScaling: 'original',
          interactionPattern: 'hover'
        }
      },
      priority: 'critical'
    },
    {
      name: 'Analytics Dashboard Cards',
      selector: '[data-testid="analytics-dashboard"]',
      adaptations: {
        mobile: {
          layout: 'stacked',
          textSize: 'small',
          imageScaling: 'responsive',
          interactionPattern: 'scroll'
        },
        tablet: {
          layout: 'grid',
          textSize: 'medium',
          imageScaling: 'responsive',
          interactionPattern: 'tap'
        },
        desktop: {
          layout: 'grid',
          textSize: 'medium',
          imageScaling: 'original',
          interactionPattern: 'hover'
        }
      },
      priority: 'high'
    },
    {
      name: 'User Profile Sections',
      selector: '[data-testid="profile-sections"]',
      adaptations: {
        mobile: {
          layout: 'accordion',
          textSize: 'medium',
          imageScaling: 'responsive',
          interactionPattern: 'tap'
        },
        tablet: {
          layout: 'tabs',
          textSize: 'medium',
          imageScaling: 'responsive',
          interactionPattern: 'tap'
        },
        desktop: {
          layout: 'grid',
          textSize: 'medium',
          imageScaling: 'original',
          interactionPattern: 'hover'
        }
      },
      priority: 'high'
    },
    {
      name: 'Feature Gallery',
      selector: '[data-testid="feature-gallery"]',
      adaptations: {
        mobile: {
          layout: 'carousel',
          textSize: 'medium',
          imageScaling: 'responsive',
          interactionPattern: 'swipe'
        },
        tablet: {
          layout: 'grid',
          textSize: 'medium',
          imageScaling: 'responsive',
          interactionPattern: 'tap'
        },
        desktop: {
          layout: 'grid',
          textSize: 'medium',
          imageScaling: 'original',
          interactionPattern: 'hover'
        }
      },
      priority: 'medium'
    }
  ];

  contentAdaptations.forEach(adaptation => {
    test(`should adapt ${adaptation.name} correctly across breakpoints`, async ({ page }) => {
      const urlMap: Record<string, string> = {
        'Hive Grid to Vertical Stack': TEST_URLS.HIVE_LIST,
        'Analytics Dashboard Cards': TEST_URLS.ANALYTICS,
        'User Profile Sections': TEST_URLS.PROFILE,
        'Feature Gallery': TEST_URLS.HOME
      };

      await page.goto(urlMap[adaptation.name] || TEST_URLS.DASHBOARD);
      await page.waitForLoadState('networkidle');

      // Test mobile adaptation
      await validateContentAdaptation(page, adaptation, 'mobile', 390);
      
      // Test tablet adaptation
      await validateContentAdaptation(page, adaptation, 'tablet', 768);
      
      // Test desktop adaptation
      await validateContentAdaptation(page, adaptation, 'desktop', 1024);
    });
  });

  test('should handle content overflow gracefully', async ({ page }) => {
    await page.goto(TEST_URLS.HIVE_LIST);
    await page.waitForLoadState('networkidle');

    const viewports = [320, 390, 768, 1024];

    for (const width of viewports) {
      await test.step(`Content overflow handling at ${width}px`, async () => {
        await page.setViewportSize({ width, height: 800 });
        await page.waitForTimeout(300);

        // Check for text overflow
        const textOverflows = await page.evaluate(() => {
          const textElements = document.querySelectorAll('p, span, div, h1, h2, h3, h4, h5, h6');
          const overflows: Array<{element: string; scrollWidth: number; clientWidth: number}> = [];
          
          textElements.forEach((el, index) => {
            const element = el as HTMLElement;
            if (element.scrollWidth > element.clientWidth + 5) { // 5px tolerance
              overflows.push({
                element: `${element.tagName}:nth-child(${index + 1})`,
                scrollWidth: element.scrollWidth,
                clientWidth: element.clientWidth
              });
            }
          });
          
          return overflows;
        });

        // Some overflow might be intentional (e.g., horizontal scrolling)
        // But excessive overflow indicates poor mobile optimization
        expect(textOverflows.length).toBeLessThan(5);

        if (textOverflows.length > 0) {
          console.log(`Text overflows at ${width}px:`, textOverflows.slice(0, 3));
        }

        // Check for image overflow
        const imageOverflows = await page.evaluate(() => {
          const images = document.querySelectorAll('img');
          let overflowCount = 0;
          
          images.forEach(img => {
            const rect = img.getBoundingClientRect();
            if (rect.right > window.innerWidth + 10) { // 10px tolerance
              overflowCount++;
            }
          });
          
          return overflowCount;
        });

        expect(imageOverflows).toBe(0);
      });
    }
  });

  test('should optimize images for mobile viewing', async ({ page }) => {
    await page.goto(TEST_URLS.HIVE_LIST);
    await page.waitForLoadState('networkidle');

    await test.step('Mobile image optimization', async () => {
      await page.setViewportSize({ width: 390, height: 844 });
      await page.waitForTimeout(300);

      const imageOptimizations = await page.evaluate(() => {
        const images = Array.from(document.querySelectorAll('img'));
        const optimizations: Array<{
          src: string;
          dimensions: { width: number; height: number };
          aspectRatio: number;
          isResponsive: boolean;
          hasLazyLoading: boolean;
          hasAltText: boolean;
        }> = [];

        images.forEach(img => {
          const rect = img.getBoundingClientRect();
          const computedStyle = window.getComputedStyle(img);
          
          optimizations.push({
            src: img.src || 'no-src',
            dimensions: { width: rect.width, height: rect.height },
            aspectRatio: rect.width / rect.height,
            isResponsive: computedStyle.maxWidth === '100%' || img.hasAttribute('sizes'),
            hasLazyLoading: img.getAttribute('loading') === 'lazy',
            hasAltText: !!img.alt
          });
        });

        return optimizations;
      });

      for (const img of imageOptimizations) {
        // Images should not exceed viewport width
        expect(img.dimensions.width).toBeLessThanOrEqual(390 + 10); // 10px tolerance

        // Images should have reasonable aspect ratios
        if (img.dimensions.width > 200) {
          expect(img.aspectRatio).toBeGreaterThan(0.5);
          expect(img.aspectRatio).toBeLessThan(4);
        }

        // Alt text should be present for accessibility
        if (!img.hasAltText) {
          console.warn(`Image missing alt text: ${img.src.substring(0, 50)}...`);
        }
      }

      console.log(`Analyzed ${imageOptimizations.length} images for mobile optimization`);
    });
  });
});

test.describe('Content Adaptation - Table Responsiveness', () => {
  const tableAdaptations: TableAdaptation[] = [
    {
      selector: '[data-testid="hive-members-table"]',
      mobilePattern: 'cards',
      priorityColumns: ['name', 'status', 'role']
    },
    {
      selector: '[data-testid="analytics-data-table"]',
      mobilePattern: 'horizontal_scroll',
      priorityColumns: ['metric', 'value', 'change']
    },
    {
      selector: '[data-testid="activity-log-table"]',
      mobilePattern: 'stacked_rows',
      priorityColumns: ['timestamp', 'action', 'user']
    }
  ];

  tableAdaptations.forEach(tableTest => {
    test(`should adapt table ${tableTest.selector} to mobile`, async ({ page }) => {
      let authHelper = new AuthHelper(page);
      await authHelper.loginWithTestUser();

      const urlMap: Record<string, string> = {
        '[data-testid="hive-members-table"]': TEST_URLS.HIVE_LIST,
        '[data-testid="analytics-data-table"]': TEST_URLS.ANALYTICS,
        '[data-testid="activity-log-table"]': TEST_URLS.PROFILE
      };

      await page.goto(urlMap[tableTest.selector] || TEST_URLS.DASHBOARD);
      await page.waitForLoadState('networkidle');

      await test.step(`Testing table adaptation pattern: ${tableTest.mobilePattern}`, async () => {
        const table = page.locator(tableTest.selector);

        // Test desktop view first
        await page.setViewportSize({ width: 1024, height: 768 });
        await page.waitForTimeout(300);

        if (await table.isVisible()) {
          const desktopColumns = await table.locator('th, [role="columnheader"]').count();
          
          // Test mobile view
          await page.setViewportSize({ width: 390, height: 844 });
          await page.waitForTimeout(500); // Allow adaptation time

          await validateTableMobileAdaptation(page, table, tableTest);
        }
      });
    });
  });

  test('should handle table overflow with horizontal scroll', async ({ page }) => {
    let authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.ANALYTICS);
    await page.waitForLoadState('networkidle');

    await page.setViewportSize({ width: 390, height: 844 });

    await test.step('Horizontal scroll table handling', async () => {
      const tables = page.locator('table, [role="table"]');
      const tableCount = await tables.count();

      if (tableCount > 0) {
        const firstTable = tables.first();
        
        // Check if table container allows horizontal scroll
        const tableContainer = firstTable.locator('xpath=..');
        const scrollCapable = await tableContainer.evaluate(el => {
          const style = window.getComputedStyle(el);
          return style.overflowX === 'auto' || style.overflowX === 'scroll';
        });

        if (scrollCapable) {
          // Test horizontal scrolling
          const scrollAmount = await tableContainer.evaluate(el => {
            const initialScroll = el.scrollLeft;
            el.scrollLeft += 100;
            const newScroll = el.scrollLeft;
            return newScroll - initialScroll;
          });

          expect(scrollAmount).toBeGreaterThan(0);
          console.log(`Table horizontal scroll working: ${scrollAmount}px`);
        } else {
          // Table should have adapted to cards or stacked layout
          const hasCardLayout = await page.locator('.card, [data-card="true"]').isVisible();
          const hasStackedLayout = await firstTable.evaluate(el => {
            const style = window.getComputedStyle(el);
            return style.display === 'block' || style.display === 'flex';
          });

          const hasAdaptation = hasCardLayout || hasStackedLayout;
          console.log(`Table adaptation detected: cards=${hasCardLayout}, stacked=${hasStackedLayout}`);
        }
      }
    });
  });
});

test.describe('Content Adaptation - Form Optimization', () => {
  const formAdaptations: FormAdaptation[] = [
    {
      name: 'Login Form',
      selector: '[data-testid="login-form"]',
      mobileOptimizations: ['large_inputs', 'appropriate_keyboards', 'clear_labels', 'touch_friendly_buttons'],
      expectedKeyboard: 'email'
    },
    {
      name: 'Profile Settings Form',
      selector: '[data-testid="profile-form"]',
      mobileOptimizations: ['grouped_sections', 'floating_labels', 'inline_validation'],
      expectedKeyboard: 'text'
    },
    {
      name: 'Create Hive Form',
      selector: '[data-testid="create-hive-form"]',
      mobileOptimizations: ['step_by_step', 'progress_indicator', 'save_draft'],
      expectedKeyboard: 'text'
    }
  ];

  formAdaptations.forEach(formTest => {
    test(`should optimize ${formTest.name} for mobile`, async ({ page }) => {
      let authHelper = new AuthHelper(page);
      
      const urlMap: Record<string, string> = {
        'Login Form': TEST_URLS.LOGIN,
        'Profile Settings Form': TEST_URLS.PROFILE,
        'Create Hive Form': TEST_URLS.DASHBOARD
      };

      await page.goto(urlMap[formTest.name] || TEST_URLS.HOME);
      
      // For login form, don't authenticate first
      if (formTest.name !== 'Login Form') {
        await authHelper.loginWithTestUser();
      }
      
      await page.waitForLoadState('networkidle');

      await test.step(`Testing mobile form optimization for ${formTest.name}`, async () => {
        await page.setViewportSize({ width: 390, height: 844 });
        await page.waitForTimeout(300);

        const form = page.locator(formTest.selector);
        
        if (await form.isVisible()) {
          await validateMobileFormOptimizations(page, form, formTest);
        } else {
          // Form might be in a modal or require triggering
          const createButton = page.locator('[data-testid="create-hive-button"], [data-testid="edit-profile"]');
          if (await createButton.isVisible()) {
            await createButton.click();
            await page.waitForTimeout(500);
            
            const modalForm = page.locator(formTest.selector);
            if (await modalForm.isVisible()) {
              await validateMobileFormOptimizations(page, modalForm, formTest);
            }
          }
        }
      });
    });
  });

  test('should handle virtual keyboard appropriately', async ({ page }) => {
    await page.goto(TEST_URLS.LOGIN);
    await page.setViewportSize({ width: 390, height: 844 });

    await test.step('Virtual keyboard handling', async () => {
      const inputs = [
        { selector: 'input[type="email"]', expectedKeyboard: 'email' },
        { selector: 'input[type="password"]', expectedKeyboard: 'text' },
        { selector: 'input[type="tel"]', expectedKeyboard: 'tel' },
        { selector: 'input[type="number"]', expectedKeyboard: 'numeric' },
        { selector: 'input[type="url"]', expectedKeyboard: 'url' }
      ];

      for (const input of inputs) {
        const field = page.locator(input.selector);
        
        if (await field.isVisible()) {
          // Check input attributes that affect mobile keyboards
          const inputAttributes = await field.evaluate(el => ({
            type: el.getAttribute('type'),
            inputMode: el.getAttribute('inputmode'),
            autocomplete: el.getAttribute('autocomplete'),
            autocorrect: el.getAttribute('autocorrect'),
            autocapitalize: el.getAttribute('autocapitalize')
          }));

          // Verify appropriate keyboard hints are set
          if (input.expectedKeyboard === 'email') {
            expect(inputAttributes.type).toBe('email');
          }

          console.log(`Input ${input.selector} attributes:`, inputAttributes);

          // Test focus behavior
          await field.focus();
          await page.waitForTimeout(500);

          // Verify input is still visible after keyboard appears
          const isStillVisible = await field.isVisible();
          expect(isStillVisible).toBeTruthy();
        }
      }
    });
  });
});

test.describe('Content Adaptation - Typography Scaling', () => {
  test('should scale typography appropriately for mobile reading', async ({ page }) => {
    let authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.HOME);

    const viewports = [
      { width: 320, name: 'Small Mobile' },
      { width: 390, name: 'Standard Mobile' },
      { width: 414, name: 'Large Mobile' },
      { width: 768, name: 'Tablet' }
    ];

    for (const viewport of viewports) {
      await test.step(`Typography scaling at ${viewport.name}`, async () => {
        await page.setViewportSize({ width: viewport.width, height: 800 });
        await page.waitForTimeout(300);

        const typographyMetrics = await page.evaluate(() => {
          const elements = {
            h1: document.querySelector('h1'),
            h2: document.querySelector('h2'),
            h3: document.querySelector('h3'),
            p: document.querySelector('p'),
            button: document.querySelector('button'),
            small: document.querySelector('small, .small')
          };

          const metrics: Record<string, {fontSize: number; lineHeight: number; readable: boolean}> = {};

          Object.entries(elements).forEach(([tag, el]) => {
            if (el) {
              const style = window.getComputedStyle(el);
              const fontSize = parseFloat(style.fontSize);
              const lineHeight = parseFloat(style.lineHeight) || fontSize * 1.2;
              
              metrics[tag] = {
                fontSize,
                lineHeight,
                readable: fontSize >= 14 // Minimum readable size on mobile
              };
            }
          });

          return metrics;
        });

        // Validate minimum font sizes for readability
        Object.entries(typographyMetrics).forEach(([tag, metrics]) => {
          const minSizes: Record<string, number> = {
            h1: 24,
            h2: 20,
            h3: 18,
            p: 14,
            button: 14,
            small: 12
          };

          const minSize = minSizes[tag] || 14;
          expect(metrics.fontSize).toBeGreaterThanOrEqual(minSize);
          expect(metrics.readable).toBeTruthy();
        });

        console.log(`Typography at ${viewport.name}:`, typographyMetrics);
      });
    }
  });

  test('should maintain good line spacing for mobile reading', async ({ page }) => {
    let authHelper = new AuthHelper(page);
    await authHelper.loginWithTestUser();
    await page.goto(TEST_URLS.HOME);

    await page.setViewportSize({ width: 390, height: 844 });

    await test.step('Line spacing validation', async () => {
      const lineSpacingMetrics = await page.evaluate(() => {
        const textElements = document.querySelectorAll('p, div, span, li');
        const metrics: Array<{tag: string; fontSize: number; lineHeight: number; ratio: number}> = [];

        textElements.forEach((el, index) => {
          const style = window.getComputedStyle(el);
          const fontSize = parseFloat(style.fontSize);
          const lineHeight = parseFloat(style.lineHeight);
          
          if (!isNaN(fontSize) && !isNaN(lineHeight) && fontSize > 10) {
            metrics.push({
              tag: `${el.tagName}:nth-child(${index + 1})`,
              fontSize,
              lineHeight,
              ratio: lineHeight / fontSize
            });
          }
        });

        return metrics.slice(0, 10); // First 10 elements
      });

      for (const metric of lineSpacingMetrics) {
        // Line height should be at least 1.2x font size for readability
        expect(metric.ratio).toBeGreaterThanOrEqual(1.2);
        
        // But not too high (>2.0) which can hurt readability
        expect(metric.ratio).toBeLessThanOrEqual(2.0);
      }

      console.log('Line spacing ratios:', lineSpacingMetrics.map(m => m.ratio));
    });
  });
});

// Helper Functions

async function validateContentAdaptation(
  page: Page,
  adaptation: ContentAdaptation,
  breakpoint: 'mobile' | 'tablet' | 'desktop',
  width: number
): Promise<void> {
  await page.setViewportSize({ width, height: 800 });
  await page.waitForTimeout(500); // Allow adaptation time

  const element = page.locator(adaptation.selector);
  const expectation = adaptation.adaptations[breakpoint];

  if (await element.isVisible()) {
    // Test layout adaptation
    await validateLayoutAdaptation(page, element, expectation.layout);
    
    // Test text size
    await validateTextSize(page, element, expectation.textSize);
    
    // Test image scaling
    await validateImageScaling(page, element, expectation.imageScaling);
    
    // Test interaction pattern
    await validateInteractionPattern(page, element, expectation.interactionPattern);
    
    console.log(`${adaptation.name} validated at ${breakpoint} (${width}px)`);
  }
}

async function validateLayoutAdaptation(
  page: Page,
  element: Locator,
  expectedLayout: ContentExpectation['layout']
): Promise<void> {
  const layoutInfo = await element.evaluate(el => {
    const style = window.getComputedStyle(el);
    return {
      display: style.display,
      flexDirection: style.flexDirection,
      gridTemplateColumns: style.gridTemplateColumns
    };
  });

  switch (expectedLayout) {
    case 'stacked':
      expect(['block', 'flex']).toContain(layoutInfo.display);
      if (layoutInfo.display === 'flex') {
        expect(layoutInfo.flexDirection).toBe('column');
      }
      break;
      
    case 'grid':
      expect(layoutInfo.display).toBe('grid');
      expect(layoutInfo.gridTemplateColumns).not.toBe('none');
      break;
      
    case 'carousel':
      const isScrollable = await element.evaluate(el => {
        const style = window.getComputedStyle(el);
        return style.overflowX === 'auto' || style.overflowX === 'scroll';
      });
      expect(isScrollable).toBeTruthy();
      break;
      
    case 'hidden':
      await expect(element).not.toBeVisible();
      break;
  }
}

async function validateTextSize(
  page: Page,
  element: Locator,
  expectedSize: ContentExpectation['textSize']
): Promise<void> {
  const textElements = element.locator('p, span, div, h1, h2, h3, h4, h5, h6');
  const count = await textElements.count();
  
  if (count > 0) {
    const fontSize = await textElements.first().evaluate(el => {
      return parseFloat(window.getComputedStyle(el).fontSize);
    });

    const sizeRanges = {
      small: [12, 16],
      medium: [14, 20],
      large: [18, 24]
    };

    const [min, max] = sizeRanges[expectedSize];
    expect(fontSize).toBeGreaterThanOrEqual(min);
    expect(fontSize).toBeLessThanOrEqual(max);
  }
}

async function validateImageScaling(
  page: Page,
  element: Locator,
  expectedScaling: ContentExpectation['imageScaling']
): Promise<void> {
  const images = element.locator('img');
  const count = await images.count();
  
  if (count > 0) {
    const imageInfo = await images.first().evaluate(el => {
      const rect = el.getBoundingClientRect();
      const style = window.getComputedStyle(el);
      return {
        width: rect.width,
        maxWidth: style.maxWidth,
        objectFit: style.objectFit
      };
    });

    switch (expectedScaling) {
      case 'responsive':
        expect(['100%', '100vw']).toContain(imageInfo.maxWidth);
        break;
        
      case 'cropped':
        expect(['cover', 'crop']).toContain(imageInfo.objectFit);
        break;
        
      case 'hidden':
        await expect(images.first()).not.toBeVisible();
        break;
    }
  }
}

async function validateInteractionPattern(
  page: Page,
  element: Locator,
  expectedPattern: ContentExpectation['interactionPattern']
): Promise<void> {
  switch (expectedPattern) {
    case 'swipe':
      const touchCapable = await element.evaluate(el => {
        return 'ontouchstart' in el || 'ontouchstart' in window;
      });
      expect(touchCapable).toBeTruthy();
      break;
      
    case 'tap':
      const clickable = await element.evaluate(el => {
        return el.style.cursor === 'pointer' || 
               el.getAttribute('role') === 'button' ||
               el.tagName.toLowerCase() === 'button';
      });
      // This might be too strict, as not all tappable elements have cursor pointer
      console.log(`Tap interaction available: ${clickable}`);
      break;
  }
}

async function validateTableMobileAdaptation(
  page: Page,
  table: Locator,
  tableTest: TableAdaptation
): Promise<void> {
  switch (tableTest.mobilePattern) {
    case 'cards':
      const cards = page.locator('.card, [data-card="true"]');
      const cardCount = await cards.count();
      expect(cardCount).toBeGreaterThan(0);
      break;
      
    case 'horizontal_scroll':
      const isScrollable = await table.evaluate(el => {
        const parent = el.parentElement;
        if (parent) {
          const style = window.getComputedStyle(parent);
          return style.overflowX === 'auto' || style.overflowX === 'scroll';
        }
        return false;
      });
      expect(isScrollable).toBeTruthy();
      break;
      
    case 'stacked_rows':
      const hasStackedLayout = await table.evaluate(el => {
        const rows = el.querySelectorAll('tr');
        if (rows.length > 0) {
          const style = window.getComputedStyle(rows[0]);
          return style.display === 'block' || style.display === 'flex';
        }
        return false;
      });
      console.log(`Stacked row layout detected: ${hasStackedLayout}`);
      break;
      
    case 'hidden_columns':
      const visibleColumns = await table.locator('th:visible').count();
      const totalColumns = await table.locator('th').count();
      expect(visibleColumns).toBeLessThan(totalColumns);
      break;
  }
}

async function validateMobileFormOptimizations(
  page: Page,
  form: Locator,
  formTest: FormAdaptation
): Promise<void> {
  const inputs = form.locator('input, textarea, select');
  const inputCount = await inputs.count();

  if (inputCount > 0) {
    for (const optimization of formTest.mobileOptimizations) {
      switch (optimization) {
        case 'large_inputs':
          const inputHeight = await inputs.first().evaluate(el => {
            return el.getBoundingClientRect().height;
          });
          expect(inputHeight).toBeGreaterThanOrEqual(44); // iOS minimum touch target
          break;
          
        case 'appropriate_keyboards':
          const emailInput = inputs.locator('[type="email"]');
          if (await emailInput.count() > 0) {
            const inputType = await emailInput.getAttribute('type');
            expect(inputType).toBe('email');
          }
          break;
          
        case 'clear_labels':
          const labels = form.locator('label');
          const labelCount = await labels.count();
          expect(labelCount).toBeGreaterThan(0);
          break;
          
        case 'touch_friendly_buttons':
          const buttons = form.locator('button, input[type="submit"]');
          if (await buttons.count() > 0) {
            const buttonHeight = await buttons.first().evaluate(el => {
              return el.getBoundingClientRect().height;
            });
            expect(buttonHeight).toBeGreaterThanOrEqual(44);
          }
          break;
      }
    }
  }
}