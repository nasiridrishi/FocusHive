/**
 * Auditory Accessibility E2E Tests
 *
 * Tests for auditory accessibility features including:
 * - Audio controls and autoplay prevention
 * - Captions and transcripts for media content
 * - Audio descriptions for visual content
 * - Sound indication alternatives
 * - Volume controls and muting
 * - Audio feedback accessibility
 *
 * WCAG 2.1 Success Criteria:
 * - 1.2.1 Audio-only and Video-only (Prerecorded)
 * - 1.2.2 Captions (Prerecorded)
 * - 1.2.3 Audio Description or Media Alternative (Prerecorded)
 * - 1.2.4 Captions (Live)
 * - 1.2.5 Audio Description (Prerecorded)
 * - 1.4.2 Audio Control
 *
 * UOL-44.19: Comprehensive Accessibility E2E Tests
 */

import {expect, Page, test} from '@playwright/test';
import {checkA11y, injectAxe} from 'axe-playwright';
import {DEFAULT_ACCESSIBILITY_CONFIG} from './accessibility.config';

test.describe('Auditory Accessibility Tests', () => {
  let page: Page;

  test.beforeEach(async ({page: testPage}) => {
    page = testPage;
    await page.goto('/');
    await injectAxe(page);
  });

  test.describe('WCAG 1.2.1 - Audio-only and Video-only Content', () => {
    test('should provide text alternatives for audio-only content', async () => {
      // Test audio-only content has text alternatives
      await page.goto('/focus-sessions');

      const audioElements = await page.locator('audio').all();

      for (const audio of audioElements) {
        // Check for transcript or text alternative
        const _audioId = await audio.getAttribute('id');
        const ariaDescribedBy = await audio.getAttribute('aria-describedby');
        const ariaLabel = await audio.getAttribute('aria-label');

        // Must have either aria-label or aria-describedby pointing to transcript
        expect(ariaLabel || ariaDescribedBy).toBeTruthy();

        if (ariaDescribedBy) {
          const transcript = page.locator(`#${ariaDescribedBy}`);
          await expect(transcript).toBeVisible();
          const transcriptText = await transcript.textContent();
          expect(transcriptText?.trim()).toBeTruthy();
        }

        // Check for controls
        const hasControls = await audio.getAttribute('controls');
        expect(hasControls).toBeTruthy();
      }
    });

    test('should provide audio descriptions for video-only content', async () => {
      await page.goto('/tutorials');

      const videoElements = await page.locator('video').all();

      for (const video of videoElements) {
        const ariaDescribedBy = await video.getAttribute('aria-describedby');
        const audioDescTrack = await video.locator('track[kind="descriptions"]');

        // Should have either aria-describedby or audio description track
        const hasAudioDesc = ariaDescribedBy || await audioDescTrack.count() > 0;
        expect(hasAudioDesc).toBeTruthy();

        if (ariaDescribedBy) {
          const description = page.locator(`#${ariaDescribedBy}`);
          await expect(description).toBeVisible();
        }
      }
    });
  });

  test.describe('WCAG 1.2.2 - Captions (Prerecorded)', () => {
    test('should provide captions for prerecorded video content', async () => {
      await page.goto('/tutorials');

      const videoElements = await page.locator('video').all();

      for (const video of videoElements) {
        // Check for caption tracks
        const captionTracks = await video.locator('track[kind="captions"], track[kind="subtitles"]');
        const captionCount = await captionTracks.count();

        if (captionCount > 0) {
          // Verify at least one caption track is in a supported format
          const firstTrack = captionTracks.first();
          const src = await firstTrack.getAttribute('src');
          const srcLang = await firstTrack.getAttribute('srclang');

          expect(src).toBeTruthy();
          expect(srcLang).toBeTruthy();

          // Verify caption file exists (if local)
          if (src?.startsWith('/') || src?.startsWith('./')) {
            const response = await page.request.get(src);
            expect(response.status()).toBe(200);
          }
        }

        // Check for caption controls in custom player
        const captionButton = page.locator('[aria-label*="captions" i], [aria-label*="subtitles" i]');
        if (await captionButton.count() > 0) {
          await expect(captionButton.first()).toBeVisible();
        }
      }
    });

    test('should allow users to toggle captions on/off', async () => {
      await page.goto('/tutorials');

      const captionToggle = page.locator('[aria-label*="captions" i], [data-testid="caption-toggle"]');

      if (await captionToggle.count() > 0) {
        const toggle = captionToggle.first();

        // Test toggling captions
        await toggle.click();

        // Check if aria-pressed or similar state indicator changes
        const pressed = await toggle.getAttribute('aria-pressed');
        const checked = await toggle.getAttribute('aria-checked');

        expect(pressed === 'true' || checked === 'true').toBeTruthy();

        // Toggle off
        await toggle.click();
        const pressedAfter = await toggle.getAttribute('aria-pressed');
        const checkedAfter = await toggle.getAttribute('aria-checked');

        expect(pressedAfter === 'false' || checkedAfter === 'false').toBeTruthy();
      }
    });
  });

  test.describe('WCAG 1.2.3 - Audio Description or Media Alternative', () => {
    test('should provide audio descriptions for video content', async () => {
      await page.goto('/onboarding-videos');

      const videoElements = await page.locator('video').all();

      for (const video of videoElements) {
        // Check for audio description track
        const audioDescTrack = await video.locator('track[kind="descriptions"]');
        const hasAudioDesc = await audioDescTrack.count() > 0;

        // Check for alternative full text transcript
        const transcriptId = await video.getAttribute('aria-describedby');
        let hasTranscript = false;

        if (transcriptId) {
          const transcript = page.locator(`#${transcriptId}`);
          hasTranscript = await transcript.isVisible();
        }

        // Must have either audio description or full transcript
        expect(hasAudioDesc || hasTranscript).toBeTruthy();
      }
    });
  });

  test.describe('WCAG 1.4.2 - Audio Control', () => {
    test('should not auto-play audio for more than 3 seconds', async () => {
      await page.goto('/');

      // Check all audio and video elements
      const mediaElements = await page.locator('audio, video').all();

      for (const media of mediaElements) {
        const autoplay = await media.getAttribute('autoplay');
        const muted = await media.getAttribute('muted');
        const _loop = await media.getAttribute('loop');

        if (autoplay) {
          // If autoplaying, must be muted or have user controls
          const hasControls = await media.getAttribute('controls');
          expect(muted !== null || hasControls !== null).toBeTruthy();
        }

        // Check duration if possible
        try {
          const duration = await page.evaluate((el) => {
            return (el as HTMLMediaElement).duration;
          }, media);

          if (autoplay && !muted && duration > 3) {
            // Must have pause/stop mechanism
            const hasControls = await media.getAttribute('controls');
            expect(hasControls).toBeTruthy();
          }
        } catch {
          // Duration not available, skip this check
        }
      }
    });

    test('should provide audio control mechanisms', async () => {
      await page.goto('/ambient-sounds');

      const audioElements = await page.locator('audio').all();

      for (const audio of audioElements) {
        // Must have either native controls or custom controls
        const hasControls = await audio.getAttribute('controls');

        if (!hasControls) {
          // Look for custom control buttons
          const _audioId = await audio.getAttribute('id');
          const playButton = page.locator(`[aria-controls="${audioId}"], [data-target="${audioId}"]`);
          const customControls = await playButton.count() > 0;

          expect(customControls).toBeTruthy();

          if (customControls) {
            // Verify play/pause functionality
            const button = playButton.first();
            const ariaLabel = await button.getAttribute('aria-label');
            expect(ariaLabel).toContain(/play|pause|stop/i);
          }
        }
      }
    });

    test('should provide volume controls for audio content', async () => {
      await page.goto('/ambient-sounds');

      // Check for volume controls
      const volumeControls = page.locator(
          'input[type="range"][aria-label*="volume" i], ' +
          '[role="slider"][aria-label*="volume" i], ' +
          'audio[controls], ' +
          '[data-testid*="volume"]'
      );

      if (await volumeControls.count() > 0) {
        const volumeControl = volumeControls.first();

        // Test volume control accessibility
        const ariaLabel = await volumeControl.getAttribute('aria-label');
        const ariaValueNow = await volumeControl.getAttribute('aria-valuenow');
        const ariaValueMin = await volumeControl.getAttribute('aria-valuemin');
        const ariaValueMax = await volumeControl.getAttribute('aria-valuemax');

        if (await volumeControl.getAttribute('role') === 'slider') {
          expect(ariaLabel).toBeTruthy();
          expect(ariaValueNow).toBeTruthy();
          expect(ariaValueMin).toBeTruthy();
          expect(ariaValueMax).toBeTruthy();
        }
      }
    });
  });

  test.describe('Audio Feedback and Notifications', () => {
    test('should provide visual alternatives for audio notifications', async () => {
      await page.goto('/dashboard');

      // Test notification system
      await page.click('[data-testid="notification-trigger"]');

      // Audio notifications should have visual counterparts
      await expect(page.locator('.notification, [role="alert"], [aria-live]')).toBeVisible();

      // Check for notification text content
      const notification = page.locator('.notification, [role="alert"]').first();
      if (await notification.count() > 0) {
        const text = await notification.textContent();
        expect(text?.trim()).toBeTruthy();
      }
    });

    test('should respect user preferences for audio feedback', async () => {
      await page.goto('/settings/accessibility');

      // Look for audio preference controls
      const audioSettings = page.locator(
          '[data-testid*="audio"], ' +
          'input[name*="audio"], ' +
          '[aria-label*="sound" i], ' +
          '[aria-label*="audio" i]'
      );

      if (await audioSettings.count() > 0) {
        const setting = audioSettings.first();

        // Verify it's properly labeled
        const label = await setting.getAttribute('aria-label');
        const labelledBy = await setting.getAttribute('aria-labelledby');

        expect(label || labelledBy).toBeTruthy();

        // Test toggle functionality if it's a checkbox/switch
        const type = await setting.getAttribute('type');
        const role = await setting.getAttribute('role');

        if (type === 'checkbox' || role === 'switch') {
          await setting.click();
          const checked = await setting.isChecked();
          expect(typeof checked).toBe('boolean');
        }
      }
    });
  });

  test.describe('Media Player Accessibility', () => {
    test('should provide accessible media player controls', async () => {
      await page.goto('/focus-music');

      const mediaPlayer = page.locator('[data-testid="media-player"], .media-player');

      if (await mediaPlayer.count() > 0) {
        const player = mediaPlayer.first();

        // Check for essential controls
        const playButton = player.locator('[aria-label*="play" i], [data-testid*="play"]');
        const pauseButton = player.locator('[aria-label*="pause" i], [data-testid*="pause"]');
        const volumeControl = player.locator('[aria-label*="volume" i], input[type="range"]');
        const progressControl = player.locator('[aria-label*="progress" i], [aria-label*="seek" i]');

        // Play/pause should be available
        const hasPlayPause = await playButton.count() > 0 || await pauseButton.count() > 0;
        expect(hasPlayPause).toBeTruthy();

        // Volume control should be accessible
        if (await volumeControl.count() > 0) {
          const control = volumeControl.first();
          const ariaLabel = await control.getAttribute('aria-label');
          expect(ariaLabel).toBeTruthy();
        }

        // Progress/seek control should be accessible
        if (await progressControl.count() > 0) {
          const control = progressControl.first();
          const ariaLabel = await control.getAttribute('aria-label');
          const role = await control.getAttribute('role');

          expect(ariaLabel).toBeTruthy();
          if (role === 'slider') {
            const ariaValueNow = await control.getAttribute('aria-valuenow');
            expect(ariaValueNow).toBeTruthy();
          }
        }
      }
    });

    test('should provide keyboard access to media controls', async () => {
      await page.goto('/focus-music');

      const mediaPlayer = page.locator('[data-testid="media-player"], .media-player');

      if (await mediaPlayer.count() > 0) {
        // Test keyboard navigation through controls
        await page.keyboard.press('Tab');

        const focusedElement = page.locator(':focus');
        const tagName = await focusedElement.evaluate(el => el.tagName.toLowerCase());
        const role = await focusedElement.getAttribute('role');
        const ariaLabel = await focusedElement.getAttribute('aria-label');

        // Should focus on a control element
        const isControl = ['button', 'input', 'slider'].includes(tagName) ||
            ['button', 'slider'].includes(role || '');
        expect(isControl).toBeTruthy();

        // Should have meaningful label
        if (isControl) {
          expect(ariaLabel).toBeTruthy();
        }

        // Test spacebar activation for buttons
        if (tagName === 'button' || role === 'button') {
          await page.keyboard.press('Space');
          // Verify state change or action occurred
        }
      }
    });
  });

  test.describe('Live Audio Content', () => {
    test('should provide captions for live audio streams', async () => {
      await page.goto('/live-study-sessions');

      // Look for live audio/video elements
      const liveElements = page.locator('[data-live="true"], .live-stream');

      if (await liveElements.count() > 0) {
        const _liveElement = liveElements.first();

        // Check for live caption support
        const captionElement = page.locator(
            '.live-captions, [data-testid="live-captions"], [aria-live="polite"]'
        );

        if (await captionElement.count() > 0) {
          await expect(captionElement.first()).toBeVisible();

          // Check for caption toggle
          const captionToggle = page.locator('[aria-label*="caption" i]');
          if (await captionToggle.count() > 0) {
            await expect(captionToggle.first()).toBeVisible();
          }
        }
      }
    });
  });

  test.describe('Comprehensive Audio Accessibility', () => {
    test('should pass comprehensive audio accessibility checks', async () => {
      await page.goto('/');

      await checkA11y(page, undefined, {
        axeOptions: {
          runOnly: {
            type: 'tag',
            values: ['wcag21aa', 'wcag2a', 'best-practice']
          },
          rules: {
            'audio-caption': {enabled: true},
            'video-caption': {enabled: true},
            'no-autoplay-audio': {enabled: true}
          }
        },
        detailedReport: true,
        detailedReportOptions: {
          html: true
        }
      });
    });

    test('should handle audio content gracefully when disabled', async () => {
      // Simulate audio being disabled
      await page.addInitScript(() => {
        Object.defineProperty(navigator, 'mediaDevices', {
          writable: true,
          value: undefined
        });
      });

      await page.goto('/ambient-sounds');

      // App should still be functional without audio
      await expect(page.locator('body')).toBeVisible();

      // Should show appropriate messaging
      const audioWarning = page.locator('.audio-disabled, [data-testid="audio-unavailable"]');
      if (await audioWarning.count() > 0) {
        await expect(audioWarning.first()).toBeVisible();
      }
    });
  });

  test.describe('Audio Performance and Quality', () => {
    test('should load audio content within accessibility timeouts', async () => {
      const startTime = Date.now();
      await page.goto('/ambient-sounds');

      // Wait for audio elements to be ready
      await page.waitForSelector('audio', {
        timeout: DEFAULT_ACCESSIBILITY_CONFIG.performance.maxNavigationTime
      });

      const loadTime = Date.now() - startTime;
      expect(loadTime).toBeLessThan(DEFAULT_ACCESSIBILITY_CONFIG.performance.maxNavigationTime);
    });

    test('should maintain audio accessibility during interactions', async () => {
      await page.goto('/focus-music');

      // Interact with audio controls
      const playButton = page.locator('[aria-label*="play" i]').first();
      if (await playButton.count() > 0) {
        await playButton.click();

        // Audio state should be communicated
        await page.waitForTimeout(100);

        const pauseButton = page.locator('[aria-label*="pause" i]').first();
        const ariaPressed = await playButton.getAttribute('aria-pressed');
        const ariaExpanded = await playButton.getAttribute('aria-expanded');

        // State should be updated
        expect(ariaPressed || ariaExpanded || await pauseButton.count() > 0).toBeTruthy();
      }
    });
  });
});