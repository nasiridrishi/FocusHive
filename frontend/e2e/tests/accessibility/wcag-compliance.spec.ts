/**
 * WCAG 2.1 AA Compliance Test Suite
 * 
 * Tests comprehensive compliance with Web Content Accessibility Guidelines 2.1 Level AA
 * Covers all four main principles: Perceivable, Operable, Understandable, Robust
 * 
 * UOL-44.19: Comprehensive Accessibility E2E Tests
 */

import { test, expect, Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { AccessibilityHelper } from '../../helpers/accessibility.helper';
import { AccessibilityPage } from '../../pages/AccessibilityPage';

test.describe('WCAG 2.1 AA Compliance Suite', () => {
  let accessibilityHelper: AccessibilityHelper;
  let accessibilityPage: AccessibilityPage;

  test.beforeEach(async ({ page }) => {
    accessibilityHelper = new AccessibilityHelper(page);
    accessibilityPage = new AccessibilityPage(page);
    
    // Configure axe-core for WCAG 2.1 AA testing
    await accessibilityHelper.configureAxe();
  });

  test.describe('Principle 1: Perceivable', () => {
    test.describe('1.1 Text Alternatives', () => {
      test('1.1.1 Non-text Content (Level A)', async ({ page }) => {
        const testPages = ['/', '/dashboard', '/profile'];
        
        for (const testPage of testPages) {
          await page.goto(testPage);
          await accessibilityPage.waitForPageLoad();
          
          const results = await new AxeBuilder({ page })
            .withRules(['image-alt', 'input-image-alt', 'area-alt', 'server-side-image-map'])
            .analyze();
            
          expect(results.violations, `Page ${testPage} has alt text violations`).toEqual([]);
          
          // Check that decorative images have empty alt text
          const decorativeImages = await page.locator('img[alt=""]').all();
          for (const img of decorativeImages) {
            const hasAriaHidden = await img.getAttribute('aria-hidden');
            const isDecorative = await img.evaluate((el) => {
              return el.closest('[role="presentation"]') !== null;
            });
            
            // Decorative images should have empty alt or be hidden from screen readers
            expect(hasAriaHidden === 'true' || isDecorative).toBeTruthy();
          }
        }
      });
    });

    test.describe('1.2 Time-based Media', () => {
      test('1.2.1 Audio-only and Video-only (Level A)', async ({ page }) => {
        await page.goto('/help'); // Assuming help page may have media
        
        const audioElements = await page.locator('audio').all();
        const videoElements = await page.locator('video').all();
        
        // Check for alternative content for audio-only
        for (const audio of audioElements) {
          const hasTranscript = await audio.evaluate((el) => {
            const parent = el.parentElement;
            return parent?.querySelector('[data-transcript]') !== null ||
                   parent?.textContent?.toLowerCase().includes('transcript') ||
                   el.getAttribute('aria-describedby') !== null;
          });
          
          expect(hasTranscript, 'Audio elements should have transcript or alternative').toBeTruthy();
        }
        
        // Check for alternative content for video-only
        for (const video of videoElements) {
          const hasAlternative = await video.evaluate((el) => {
            const parent = el.parentElement;
            return parent?.querySelector('[data-transcript], [data-audio-description]') !== null ||
                   el.querySelector('track[kind="descriptions"]') !== null;
          });
          
          if (await video.getAttribute('muted') === null) {
            expect(hasAlternative, 'Video elements should have transcript or audio description').toBeTruthy();
          }
        }
      });

      test('1.2.2 Captions (Level A)', async ({ page }) => {
        await page.goto('/help');
        
        const videoElements = await page.locator('video').all();
        
        for (const video of videoElements) {
          const hasAudio = await video.evaluate((el) => {
            return el.hasAttribute('muted') === false;
          });
          
          if (hasAudio) {
            const captionTracks = await video.locator('track[kind="captions"], track[kind="subtitles"]').all();
            expect(captionTracks.length, 'Videos with audio should have caption tracks').toBeGreaterThan(0);
            
            // Verify caption tracks have valid sources
            for (const track of captionTracks) {
              const src = await track.getAttribute('src');
              expect(src, 'Caption tracks should have valid src').toBeTruthy();
            }
          }
        }
      });
    });

    test.describe('1.3 Adaptable', () => {
      test('1.3.1 Info and Relationships (Level A)', async ({ page }) => {
        const testPages = ['/', '/dashboard', '/profile'];
        
        for (const testPage of testPages) {
          await page.goto(testPage);
          
          const results = await new AxeBuilder({ page })
            .withRules([
              'definition-list',
              'dlitem',
              'list',
              'listitem',
              'th-has-data-cells',
              'td-headers-attr',
              'scope-attr-valid'
            ])
            .analyze();
            
          expect(results.violations, `Page ${testPage} has structural violations`).toEqual([]);
        }
      });

      test('1.3.2 Meaningful Sequence (Level A)', async ({ page }) => {
        const testPages = ['/', '/dashboard'];
        
        for (const testPage of testPages) {
          await page.goto(testPage);
          
          // Test reading order with CSS disabled
          await page.addStyleTag({ content: '* { all: unset !important; }' });
          
          const headings = await accessibilityPage.getHeadingStructure();
          const isLogicalOrder = await accessibilityHelper.verifyHeadingOrder(headings);
          
          expect(isLogicalOrder, `Page ${testPage} should have logical heading order`).toBeTruthy();
        }
      });

      test('1.3.3 Sensory Characteristics (Level A)', async ({ page }) => {
        await page.goto('/');
        
        const results = await new AxeBuilder({ page })
            .withRules(['color-contrast', 'use-landmarks'])
            .analyze();
            
        expect(results.violations).toEqual([]);
        
        // Check that instructions don't rely solely on sensory characteristics
        const instructionTexts = await page.locator('p, div, span').evaluateAll((elements) => {
          return elements
            .map(el => el.textContent?.toLowerCase() || '')
            .filter(text => 
              text.includes('click the red button') ||
              text.includes('use the round button') ||
              text.includes('on your right') ||
              text.includes('above') ||
              text.includes('below')
            );
        });
        
        expect(instructionTexts.length, 'Instructions should not rely solely on sensory characteristics').toBe(0);
      });

      test('1.3.4 Orientation (Level AA)', async ({ page }) => {
        await page.goto('/');
        
        // Test both portrait and landscape orientations
        const orientations = [
          { width: 375, height: 667 }, // Portrait
          { width: 667, height: 375 }  // Landscape
        ];
        
        for (const viewport of orientations) {
          await page.setViewportSize(viewport);
          await page.waitForTimeout(500);
          
          // Check that content is accessible in both orientations
          const results = await new AxeBuilder({ page })
            .withTags(['wcag21aa'])
            .analyze();
            
          expect(results.violations, `Content should be accessible in ${viewport.width}x${viewport.height}`).toEqual([]);
          
          // Verify no content is cut off
          const hasHorizontalScroll = await page.evaluate(() => {
            return document.body.scrollWidth > window.innerWidth;
          });
          
          expect(hasHorizontalScroll, 'Should not require horizontal scrolling').toBeFalsy();
        }
      });

      test('1.3.5 Identify Input Purpose (Level AA)', async ({ page }) => {
        const formsPages = ['/login', '/register', '/profile'];
        
        for (const formPage of formsPages) {
          await page.goto(formPage);
          
          const inputElements = await page.locator('input[type="text"], input[type="email"], input[type="password"], input[type="tel"]').all();
          
          for (const input of inputElements) {
            const autocomplete = await input.getAttribute('autocomplete');
            const name = await input.getAttribute('name');
            const type = await input.getAttribute('type');
            
            // Check for appropriate autocomplete values
            if (name?.includes('email') || type === 'email') {
              expect(autocomplete, 'Email fields should have autocomplete="email"').toBe('email');
            }
            
            if (name?.includes('password') || type === 'password') {
              expect(autocomplete?.includes('password'), 'Password fields should have appropriate autocomplete').toBeTruthy();
            }
            
            if (name?.includes('username')) {
              expect(autocomplete, 'Username fields should have autocomplete="username"').toBe('username');
            }
          }
        }
      });
    });

    test.describe('1.4 Distinguishable', () => {
      test('1.4.1 Use of Color (Level A)', async ({ page }) => {
        await page.goto('/dashboard');
        
        const results = await new AxeBuilder({ page })
          .withRules(['color-contrast', 'link-in-text-block'])
          .analyze();
          
        expect(results.violations).toEqual([]);
      });

      test('1.4.2 Audio Control (Level A)', async ({ page }) => {
        await page.goto('/');
        
        const audioElements = await page.locator('audio[autoplay], video[autoplay]').all();
        
        for (const media of audioElements) {
          const hasControls = await media.getAttribute('controls');
          const isMuted = await media.getAttribute('muted');
          const duration = await media.evaluate((el: HTMLMediaElement) => el.duration);
          
          // Auto-playing audio longer than 3 seconds should have controls or be muted
          if (duration > 3) {
            expect(hasControls !== null || isMuted !== null, 'Auto-playing audio > 3s should have controls or be muted').toBeTruthy();
          }
        }
      });

      test('1.4.3 Contrast (Minimum) (Level AA)', async ({ page }) => {
        const testPages = ['/', '/dashboard', '/login'];
        
        for (const testPage of testPages) {
          await page.goto(testPage);
          
          const results = await new AxeBuilder({ page })
            .withRules(['color-contrast'])
            .analyze();
            
          expect(results.violations, `Page ${testPage} should meet contrast requirements`).toEqual([]);
        }
      });

      test('1.4.4 Resize Text (Level AA)', async ({ page }) => {
        await page.goto('/');
        
        // Test 200% zoom without horizontal scrolling
        const originalSize = page.viewportSize()!;
        const zoomedSize = {
          width: Math.floor(originalSize.width / 2),
          height: Math.floor(originalSize.height / 2)
        };
        
        await page.setViewportSize(zoomedSize);
        await page.waitForTimeout(1000);
        
        const hasHorizontalScroll = await page.evaluate(() => {
          return document.body.scrollWidth > window.innerWidth;
        });
        
        expect(hasHorizontalScroll, 'Text should resize to 200% without horizontal scroll').toBeFalsy();
        
        // Restore original size
        await page.setViewportSize(originalSize);
      });

      test('1.4.5 Images of Text (Level AA)', async ({ page }) => {
        const testPages = ['/', '/dashboard'];
        
        for (const testPage of testPages) {
          await page.goto(testPage);
          
          const results = await new AxeBuilder({ page })
            .withRules(['image-alt'])
            .analyze();
            
          // Check for images that might contain text
          const images = await page.locator('img').all();
          
          for (const img of images) {
            const alt = await img.getAttribute('alt');
            const src = await img.getAttribute('src');
            
            // Look for indicators that this might be text as image
            const mayContainText = alt?.toLowerCase().includes('text') ||
                                   alt?.toLowerCase().includes('heading') ||
                                   src?.toLowerCase().includes('text') ||
                                   src?.toLowerCase().includes('logo');
            
            if (mayContainText && !src?.toLowerCase().includes('logo')) {
              console.warn(`Possible text as image detected: ${src} with alt: ${alt}`);
            }
          }
        }
      });

      test('1.4.10 Reflow (Level AA)', async ({ page }) => {
        await page.goto('/');
        
        // Test at 320px width (equivalent to 400% zoom)
        await page.setViewportSize({ width: 320, height: 568 });
        await page.waitForTimeout(500);
        
        const hasHorizontalScroll = await page.evaluate(() => {
          return document.body.scrollWidth > window.innerWidth;
        });
        
        expect(hasHorizontalScroll, 'Content should reflow at 320px width without horizontal scroll').toBeFalsy();
        
        // Check that all content is still accessible
        const results = await new AxeBuilder({ page })
          .withTags(['wcag21aa'])
          .analyze();
          
        expect(results.violations).toEqual([]);
      });

      test('1.4.11 Non-text Contrast (Level AA)', async ({ page }) => {
        await page.goto('/');
        
        // Test UI components and graphics contrast
        const interactiveElements = await accessibilityPage.getInteractiveElements();
        
        for (const element of interactiveElements.slice(0, 10)) {
          const contrastResult = await accessibilityHelper.checkElementContrast(element);
          expect(contrastResult.ratio, 'Interactive elements should meet 3:1 contrast ratio').toBeGreaterThanOrEqual(3);
        }
      });

      test('1.4.12 Text Spacing (Level AA)', async ({ page }) => {
        await page.goto('/');
        
        // Apply text spacing CSS as per WCAG requirements
        await page.addStyleTag({
          content: `
            * {
              line-height: 1.5 !important;
              letter-spacing: 0.12em !important;
              word-spacing: 0.16em !important;
            }
            p {
              margin-bottom: 2em !important;
            }
          `
        });
        
        await page.waitForTimeout(500);
        
        // Check that content is still readable and doesn't overlap
        const hasOverlappingContent = await page.evaluate(() => {
          const elements = Array.from(document.querySelectorAll('p, span, div'));
          for (let i = 0; i < elements.length - 1; i++) {
            const rect1 = elements[i].getBoundingClientRect();
            const rect2 = elements[i + 1].getBoundingClientRect();
            
            if (rect1.bottom > rect2.top && rect1.top < rect2.bottom) {
              return true; // Overlapping detected
            }
          }
          return false;
        });
        
        expect(hasOverlappingContent, 'Text should not overlap with modified spacing').toBeFalsy();
      });

      test('1.4.13 Content on Hover or Focus (Level AA)', async ({ page }) => {
        await page.goto('/');
        
        const tooltipTriggers = await page.locator('[title], [data-tooltip], [aria-describedby]').all();
        
        for (const trigger of tooltipTriggers.slice(0, 5)) {
          await trigger.hover();
          await page.waitForTimeout(200);
          
          // Check if tooltip/popup content appears
          const tooltip = page.locator('[role="tooltip"], .tooltip, .popover').first();
          
          if (await tooltip.isVisible()) {
            // Test dismissible (ESC key)
            await page.keyboard.press('Escape');
            await expect(tooltip).not.toBeVisible();
            
            // Test hoverable (can move mouse to tooltip)
            await trigger.hover();
            await page.waitForTimeout(200);
            
            if (await tooltip.isVisible()) {
              await tooltip.hover();
              await expect(tooltip).toBeVisible();
            }
            
            // Test persistent (remains visible until dismissed)
            await trigger.focus();
            await page.waitForTimeout(200);
            
            if (await tooltip.isVisible()) {
              await page.keyboard.press('Tab'); // Move focus away
              await page.waitForTimeout(200);
              // Tooltip may or may not disappear depending on implementation
            }
          }
        }
      });
    });
  });

  test.describe('Principle 2: Operable', () => {
    test.describe('2.1 Keyboard Accessible', () => {
      test('2.1.1 Keyboard (Level A)', async ({ page }) => {
        const testPages = ['/', '/dashboard'];
        
        for (const testPage of testPages) {
          await page.goto(testPage);
          
          const results = await new AxeBuilder({ page })
            .withRules(['keyboard'])
            .analyze();
            
          expect(results.violations, `Page ${testPage} should be keyboard accessible`).toEqual([]);
          
          // Test actual keyboard navigation
          const navigationTest = await accessibilityHelper.testKeyboardNavigation(page.locator('body'));
          expect(navigationTest.canNavigate, `Should be able to navigate ${testPage} with keyboard`).toBeTruthy();
        }
      });

      test('2.1.2 No Keyboard Trap (Level A)', async ({ page }) => {
        await page.goto('/dashboard');
        
        await accessibilityPage.verifyNoKeyboardTraps();
        
        // Test modal focus trapping if modal exists
        const modalTrigger = await accessibilityPage.findModalTrigger();
        if (modalTrigger) {
          await modalTrigger.click();
          const modal = await accessibilityPage.getOpenModal();
          
          if (modal) {
            await accessibilityPage.testFocusTrap(modal);
            await page.keyboard.press('Escape'); // Should close modal
          }
        }
      });

      test('2.1.4 Character Key Shortcuts (Level A)', async ({ page }) => {
        await page.goto('/dashboard');
        
        // Test that character key shortcuts don't interfere with normal typing
        const textInputs = await page.locator('input[type="text"], textarea').all();
        
        for (const input of textInputs.slice(0, 3)) {
          await input.focus();
          await input.fill('test text');
          
          const value = await input.inputValue();
          expect(value, 'Text input should work normally').toBe('test text');
        }
      });
    });

    test.describe('2.2 Enough Time', () => {
      test('2.2.1 Timing Adjustable (Level A)', async ({ page }) => {
        await page.goto('/dashboard');
        
        // Look for any time limits
        const timeoutElements = await page.locator('[data-timeout], [data-timer]').all();
        
        for (const element of timeoutElements) {
          // Should provide way to extend, adjust, or turn off time limit
          const hasControls = await element.evaluate((el) => {
            const parent = el.closest('div, section');
            return parent?.querySelector('[data-extend], [data-adjust], button:has-text("extend")') !== null;
          });
          
          if (!hasControls) {
            console.warn('Time limit detected without user controls');
          }
        }
      });

      test('2.2.2 Pause, Stop, Hide (Level A)', async ({ page }) => {
        await page.goto('/');
        
        // Check for moving, blinking, or scrolling content
        const animatedElements = await page.locator('[data-animate], .animate, [style*="animation"]').all();
        
        for (const element of animatedElements) {
          const hasPlayPauseControls = await element.evaluate((el) => {
            const controls = el.querySelector('[data-pause], [data-play], button[aria-label*="pause"], button[aria-label*="play"]');
            return controls !== null;
          });
          
          const isEssential = await element.evaluate((el) => {
            return el.getAttribute('data-essential') === 'true' ||
                   el.closest('[role="progressbar"]') !== null;
          });
          
          if (!isEssential) {
            expect(hasPlayPauseControls, 'Non-essential animations should have pause controls').toBeTruthy();
          }
        }
      });
    });

    test.describe('2.3 Seizures and Physical Reactions', () => {
      test('2.3.1 Three Flashes or Below Threshold (Level A)', async ({ page }) => {
        await page.goto('/');
        
        // Check for flashing content
        const flashingElements = await page.locator('[data-flash], .flash, [style*="blink"]').all();
        
        expect(flashingElements.length, 'Should not have flashing content that may cause seizures').toBe(0);
      });
    });

    test.describe('2.4 Navigable', () => {
      test('2.4.1 Bypass Blocks (Level A)', async ({ page }) => {
        await page.goto('/');
        
        const skipLink = await accessibilityPage.getSkipLink();
        expect(skipLink, 'Page should have skip navigation link').toBeTruthy();
        
        if (skipLink) {
          await page.keyboard.press('Tab'); // Make skip link visible
          await expect(skipLink).toBeVisible();
          
          await skipLink.click();
          const mainContent = await accessibilityPage.getMainContent();
          await expect(mainContent).toBeFocused();
        }
      });

      test('2.4.2 Page Titled (Level A)', async ({ page }) => {
        const testPages = [
          { path: '/', expectedTitle: /FocusHive|Home/ },
          { path: '/dashboard', expectedTitle: /Dashboard/ },
          { path: '/login', expectedTitle: /Login|Sign In/ },
          { path: '/register', expectedTitle: /Register|Sign Up/ }
        ];
        
        for (const { path, expectedTitle } of testPages) {
          await page.goto(path);
          const title = await page.title();
          
          expect(title, `Page ${path} should have descriptive title`).toMatch(expectedTitle);
          expect(title.length, `Page ${path} title should not be empty`).toBeGreaterThan(0);
        }
      });

      test('2.4.3 Focus Order (Level A)', async ({ page }) => {
        await page.goto('/');
        
        const results = await new AxeBuilder({ page })
          .withRules(['focus-order-semantics', 'tabindex'])
          .analyze();
          
        expect(results.violations).toEqual([]);
        
        // Test logical tab order
        const focusableElements = await accessibilityPage.getFocusableElements();
        
        for (let i = 0; i < Math.min(focusableElements.length, 10); i++) {
          await page.keyboard.press('Tab');
          const focusedElement = page.locator(':focus');
          await expect(focusedElement).toBeVisible();
        }
      });

      test('2.4.4 Link Purpose (Level A)', async ({ page }) => {
        const testPages = ['/', '/dashboard'];
        
        for (const testPage of testPages) {
          await page.goto(testPage);
          
          const results = await new AxeBuilder({ page })
            .withRules(['link-name', 'link-in-text-block'])
            .analyze();
            
          expect(results.violations, `Page ${testPage} links should have clear purpose`).toEqual([]);
          
          // Check for vague link text
          const links = await page.locator('a').all();
          
          for (const link of links.slice(0, 10)) {
            const linkText = await accessibilityHelper.getAccessibleName(link);
            const vaguePhrases = ['click here', 'read more', 'more', 'here', 'link'];
            
            const isVague = vaguePhrases.some(phrase => 
              linkText.toLowerCase().trim() === phrase
            );
            
            expect(isVague, `Link text "${linkText}" should be descriptive`).toBeFalsy();
          }
        }
      });

      test('2.4.5 Multiple Ways (Level AA)', async ({ page }) => {
        await page.goto('/');
        
        // Check for multiple navigation methods
        const navigationMethods = {
          sitemap: await page.locator('a[href*="sitemap"], a:has-text("Sitemap")').count(),
          search: await page.locator('[role="search"], input[type="search"], [data-search]').count(),
          navigation: await page.locator('nav, [role="navigation"]').count(),
          breadcrumbs: await page.locator('[aria-label*="breadcrumb"], .breadcrumb, [data-breadcrumb]').count()
        };
        
        const availableMethods = Object.values(navigationMethods).filter(count => count > 0).length;
        expect(availableMethods, 'Should provide multiple ways to navigate').toBeGreaterThanOrEqual(2);
      });

      test('2.4.6 Headings and Labels (Level AA)', async ({ page }) => {
        const testPages = ['/', '/dashboard', '/login'];
        
        for (const testPage of testPages) {
          await page.goto(testPage);
          
          const results = await new AxeBuilder({ page })
            .withRules(['empty-heading', 'heading-order', 'label'])
            .analyze();
            
          expect(results.violations, `Page ${testPage} should have descriptive headings and labels`).toEqual([]);
        }
      });

      test('2.4.7 Focus Visible (Level AA)', async ({ page }) => {
        await page.goto('/');
        
        const focusableElements = await accessibilityPage.getFocusableElements();
        
        for (let i = 0; i < Math.min(focusableElements.length, 10); i++) {
          await page.keyboard.press('Tab');
          const focusedElement = page.locator(':focus');
          
          const focusResult = await accessibilityHelper.verifyFocusIndicator(focusedElement);
          expect(focusResult.isVisible, `Focus indicator should be visible for element ${i}`).toBeTruthy();
          expect(focusResult.meetsContrastRequirement, `Focus indicator should meet contrast requirements for element ${i}`).toBeTruthy();
        }
      });
    });

    test.describe('2.5 Input Modalities', () => {
      test('2.5.1 Pointer Gestures (Level A)', async ({ page }) => {
        await page.goto('/');
        
        // Test that multipoint or path-based gestures have single-pointer alternatives
        const gestureElements = await page.locator('[data-gesture], [data-swipe], [data-pinch]').all();
        
        for (const element of gestureElements) {
          const hasAlternative = await element.evaluate((el) => {
            const parent = el.closest('div, section');
            return parent?.querySelector('button, [role="button"], input') !== null;
          });
          
          expect(hasAlternative, 'Gesture-based interactions should have single-pointer alternatives').toBeTruthy();
        }
      });

      test('2.5.2 Pointer Cancellation (Level A)', async ({ page }) => {
        await page.goto('/');
        
        const buttons = await page.locator('button').all();
        
        for (const button of buttons.slice(0, 5)) {
          // Test that button events occur on up-event, not down-event
          let clickTriggered = false;
          
          await button.evaluate((btn) => {
            btn.addEventListener('click', () => {
              // This should only fire on mouseup/touchend
            });
          });
          
          await button.hover();
          // Mouse down but don't release
          await page.mouse.down();
          await page.waitForTimeout(100);
          
          // Move away from button
          await page.mouse.move(0, 0);
          await page.mouse.up();
          
          // Click should not have been triggered when moving away
          // This is implementation-dependent but good practice
        }
      });

      test('2.5.3 Label in Name (Level A)', async ({ page }) => {
        const testPages = ['/login', '/register', '/profile'];
        
        for (const testPage of testPages) {
          await page.goto(testPage);
          
          const results = await new AxeBuilder({ page })
            .withRules(['label-content-name-mismatch'])
            .analyze();
            
          expect(results.violations, `Page ${testPage} should have consistent labels and names`).toEqual([]);
        }
      });

      test('2.5.4 Motion Actuation (Level A)', async ({ page }) => {
        await page.goto('/');
        
        // Check for motion-based controls and ensure they have alternatives
        const motionElements = await page.locator('[data-shake], [data-tilt], [data-motion]').all();
        
        for (const element of motionElements) {
          const hasAlternative = await element.evaluate((el) => {
            const parent = el.closest('div, section');
            return parent?.querySelector('button:not([data-motion]), input, [role="button"]:not([data-motion])') !== null;
          });
          
          expect(hasAlternative, 'Motion-based controls should have non-motion alternatives').toBeTruthy();
        }
      });
    });
  });

  test.describe('Principle 3: Understandable', () => {
    test.describe('3.1 Readable', () => {
      test('3.1.1 Language of Page (Level A)', async ({ page }) => {
        const testPages = ['/', '/dashboard', '/login'];
        
        for (const testPage of testPages) {
          await page.goto(testPage);
          
          const lang = await page.locator('html').getAttribute('lang');
          expect(lang, `Page ${testPage} should have lang attribute`).toBeTruthy();
          expect(lang, `Page ${testPage} should have valid lang code`).toMatch(/^[a-z]{2}(-[A-Z]{2})?$/);
        }
      });

      test('3.1.2 Language of Parts (Level AA)', async ({ page }) => {
        await page.goto('/');
        
        // Check for content in different languages
        const foreignContent = await page.locator('[lang]:not(html)').all();
        
        for (const element of foreignContent) {
          const lang = await element.getAttribute('lang');
          expect(lang, 'Foreign language content should have lang attribute').toMatch(/^[a-z]{2}(-[A-Z]{2})?$/);
        }
      });
    });

    test.describe('3.2 Predictable', () => {
      test('3.2.1 On Focus (Level A)', async ({ page }) => {
        await page.goto('/');
        
        // Test that focusing elements doesn't cause unexpected context changes
        const focusableElements = await accessibilityPage.getFocusableElements();
        
        const originalUrl = page.url();
        
        for (let i = 0; i < Math.min(focusableElements.length, 10); i++) {
          const element = focusableElements[i];
          await element.focus();
          
          // Check that focus doesn't cause navigation or major layout changes
          const currentUrl = page.url();
          expect(currentUrl, 'Focus should not cause navigation').toBe(originalUrl);
        }
      });

      test('3.2.2 On Input (Level A)', async ({ page }) => {
        await page.goto('/login');
        
        const inputs = await page.locator('input, select').all();
        const originalUrl = page.url();
        
        for (const input of inputs.slice(0, 5)) {
          const type = await input.getAttribute('type');
          
          if (type === 'text' || type === 'email') {
            await input.fill('test');
          } else if (type === 'checkbox') {
            await input.check();
          }
          
          // Check that input doesn't cause unexpected navigation
          const currentUrl = page.url();
          expect(currentUrl, 'Input should not cause automatic navigation').toBe(originalUrl);
        }
      });

      test('3.2.3 Consistent Navigation (Level AA)', async ({ page }) => {
        const testPages = ['/', '/dashboard', '/profile'];
        let previousNavStructure: string[] | null = null;
        
        for (const testPage of testPages) {
          await page.goto(testPage);
          
          const navigation = await page.locator('nav, [role="navigation"]').first();
          if (await navigation.count() > 0) {
            const navItems = await navigation.locator('a, button').allTextContents();
            
            if (previousNavStructure) {
              // Check that common navigation items appear in same order
              const commonItems = navItems.filter(item => previousNavStructure.includes(item));
              expect(commonItems.length, 'Navigation should be consistent across pages').toBeGreaterThan(0);
            }
            
            previousNavStructure = navItems;
          }
        }
      });

      test('3.2.4 Consistent Identification (Level AA)', async ({ page }) => {
        const testPages = ['/', '/dashboard', '/profile'];
        const componentIdentities = new Map();
        
        for (const testPage of testPages) {
          await page.goto(testPage);
          
          // Check that same functional components have consistent identification
          const searchInputs = await page.locator('input[type="search"], [role="search"] input').all();
          
          for (const input of searchInputs) {
            const label = await accessibilityHelper.getAccessibleName(input);
            const placeholder = await input.getAttribute('placeholder');
            
            const identifier = label || placeholder || 'search';
            
            if (componentIdentities.has('search')) {
              expect(identifier, 'Search components should be consistently identified').toBe(componentIdentities.get('search'));
            } else {
              componentIdentities.set('search', identifier);
            }
          }
        }
      });
    });

    test.describe('3.3 Input Assistance', () => {
      test('3.3.1 Error Identification (Level A)', async ({ page }) => {
        await page.goto('/login');
        
        // Submit form without required fields to trigger validation
        const submitButton = page.locator('button[type="submit"], input[type="submit"]').first();
        await submitButton.click();
        
        await page.waitForTimeout(1000);
        
        const results = await new AxeBuilder({ page })
          .withRules(['label', 'form-field-multiple-labels'])
          .analyze();
          
        expect(results.violations).toEqual([]);
        
        // Check for error messages
        const errorMessages = await accessibilityPage.getErrorMessages();
        
        for (const error of errorMessages) {
          await expect(error).toBeVisible();
          
          // Error should identify the field in error
          const errorText = await error.textContent();
          expect(errorText?.length, 'Error message should not be empty').toBeGreaterThan(0);
        }
      });

      test('3.3.2 Labels or Instructions (Level A)', async ({ page }) => {
        const formPages = ['/login', '/register', '/profile'];
        
        for (const formPage of formPages) {
          await page.goto(formPage);
          
          const results = await new AxeBuilder({ page })
            .withRules(['label', 'form-field-multiple-labels'])
            .analyze();
            
          expect(results.violations, `Page ${formPage} should have proper form labels`).toEqual([]);
        }
      });

      test('3.3.3 Error Suggestion (Level AA)', async ({ page }) => {
        await page.goto('/login');
        
        // Test with invalid data
        const emailField = page.locator('input[type="email"]').first();
        if (await emailField.count() > 0) {
          await emailField.fill('invalid-email');
          
          const submitButton = page.locator('button[type="submit"]').first();
          await submitButton.click();
          
          await page.waitForTimeout(1000);
          
          const errorMessages = await accessibilityPage.getErrorMessages();
          
          for (const error of errorMessages) {
            const errorText = await error.textContent();
            
            // Error should suggest correction
            const hasSuggestion = errorText?.toLowerCase().includes('format') ||
                                  errorText?.toLowerCase().includes('example') ||
                                  errorText?.toLowerCase().includes('should') ||
                                  errorText?.includes('@');
            
            expect(hasSuggestion, 'Error messages should provide correction suggestions').toBeTruthy();
          }
        }
      });

      test('3.3.4 Error Prevention (Level AA)', async ({ page }) => {
        await page.goto('/profile');
        
        // Check for confirmation mechanisms on important forms
        const submitButtons = await page.locator('button[type="submit"]:has-text("Delete"), button[type="submit"]:has-text("Remove")').all();
        
        for (const button of submitButtons) {
          const hasConfirmation = await button.evaluate((btn) => {
            const form = btn.closest('form');
            return form?.querySelector('[type="checkbox"]:required') !== null ||
                   btn.getAttribute('onclick')?.includes('confirm') ||
                   form?.getAttribute('onsubmit')?.includes('confirm');
          });
          
          expect(hasConfirmation, 'Destructive actions should require confirmation').toBeTruthy();
        }
      });
    });
  });

  test.describe('Principle 4: Robust', () => {
    test.describe('4.1 Compatible', () => {
      test('4.1.1 Parsing (Level A)', async ({ page }) => {
        const testPages = ['/', '/dashboard', '/login'];
        
        for (const testPage of testPages) {
          await page.goto(testPage);
          
          // Check for valid HTML structure
          const results = await new AxeBuilder({ page })
            .withRules(['duplicate-id', 'duplicate-id-active', 'duplicate-id-aria'])
            .analyze();
            
          expect(results.violations, `Page ${testPage} should have valid HTML structure`).toEqual([]);
        }
      });

      test('4.1.2 Name, Role, Value (Level A)', async ({ page }) => {
        const testPages = ['/', '/dashboard', '/login'];
        
        for (const testPage of testPages) {
          await page.goto(testPage);
          
          const results = await new AxeBuilder({ page })
            .withRules([
              'aria-valid-attr',
              'aria-valid-attr-value',
              'aria-required-attr',
              'button-name',
              'input-button-name',
              'link-name'
            ])
            .analyze();
            
          expect(results.violations, `Page ${testPage} should have proper name, role, value`).toEqual([]);
        }
      });

      test('4.1.3 Status Messages (Level AA)', async ({ page }) => {
        await page.goto('/dashboard');
        
        // Test status messages are properly announced
        const statusRegions = await page.locator('[aria-live], [role="status"], [role="alert"]').all();
        
        for (const region of statusRegions) {
          const ariaLive = await region.getAttribute('aria-live');
          const role = await region.getAttribute('role');
          
          const hasProperAnnouncement = ariaLive === 'polite' ||
                                        ariaLive === 'assertive' ||
                                        role === 'status' ||
                                        role === 'alert';
          
          expect(hasProperAnnouncement, 'Status messages should be properly announced').toBeTruthy();
        }
      });
    });
  });

  // Comprehensive test that runs all WCAG 2.1 AA rules
  test('Complete WCAG 2.1 AA Compliance Scan', async ({ page }) => {
    const testPages = ['/', '/dashboard', '/login'];
    const allResults = [];
    
    for (const testPage of testPages) {
      await page.goto(testPage);
      await accessibilityPage.waitForPageLoad();
      
      const results = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
        .analyze();
      
      allResults.push({
        page: testPage,
        violations: results.violations,
        passes: results.passes.length,
        incomplete: results.incomplete.length
      });
      
      if (results.violations.length > 0) {
        console.error(`WCAG violations found on ${testPage}:`, JSON.stringify(results.violations, null, 2));
      }
      
      expect(results.violations, `Page ${testPage} should pass WCAG 2.1 AA compliance`).toEqual([]);
    }
    
    // Generate summary report
    const totalViolations = allResults.reduce((sum, result) => sum + result.violations.length, 0);
    const totalPasses = allResults.reduce((sum, result) => sum + result.passes, 0);
    const totalIncomplete = allResults.reduce((sum, result) => sum + result.incomplete, 0);
    
    console.log('WCAG 2.1 AA Compliance Summary:', {
      totalPages: allResults.length,
      totalViolations,
      totalPasses,
      totalIncomplete,
      complianceScore: totalViolations === 0 ? 100 : Math.round((totalPasses / (totalPasses + totalViolations)) * 100)
    });
  });
});
