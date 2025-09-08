/**
 * Page Object Model for Mobile Responsiveness Testing
 * Provides utilities for mobile-specific interactions and responsive testing across FocusHive pages
 */

import { Page, expect, Locator } from '@playwright/test';
import { SELECTORS, TIMEOUTS, TEST_URLS } from '../helpers/test-data';
import { 
  MobileHelper, 
  DeviceConfig, 
  TouchInteraction, 
  ViewportTestResult,
  MobilePerformanceMetrics 
} from '../helpers/mobile.helper';

export class MobilePage {
  readonly page: Page;
  readonly mobileHelper: MobileHelper;
  
  // Common responsive elements
  readonly mainContent: Locator;
  readonly navigationMenu: Locator;
  readonly mobileMenuToggle: Locator;
  readonly mobileMenuDrawer: Locator;
  readonly loadingIndicator: Locator;
  readonly errorMessage: Locator;
  
  // Header and navigation elements
  readonly header: Locator;
  readonly logo: Locator;
  readonly searchBar: Locator;
  readonly userAvatar: Locator;
  readonly userMenu: Locator;
  readonly notificationBell: Locator;
  
  // Mobile-specific navigation elements
  readonly bottomNavigation: Locator;
  readonly backButton: Locator;
  readonly breadcrumbs: Locator;
  readonly tabBar: Locator;
  
  // Form elements for mobile testing
  readonly forms: Locator;
  readonly textInputs: Locator;
  readonly emailInputs: Locator;
  readonly passwordInputs: Locator;
  readonly submitButtons: Locator;
  readonly formErrors: Locator;
  
  // Content and layout elements
  readonly cards: Locator;
  readonly modals: Locator;
  readonly overlays: Locator;
  readonly tables: Locator;
  readonly images: Locator;
  readonly videos: Locator;
  
  // Interactive elements
  readonly buttons: Locator;
  readonly links: Locator;
  readonly toggleSwitches: Locator;
  readonly dropdowns: Locator;
  readonly sliders: Locator;
  
  // Hive-specific mobile elements
  readonly createHiveButton: Locator;
  readonly hiveCards: Locator;
  readonly joinHiveButton: Locator;
  readonly hiveSettings: Locator;
  
  // Timer elements
  readonly timerDisplay: Locator;
  readonly startTimerButton: Locator;
  readonly pauseTimerButton: Locator;
  readonly stopTimerButton: Locator;
  readonly timerControls: Locator;
  
  // Chat and communication elements
  readonly chatContainer: Locator;
  readonly chatInput: Locator;
  readonly chatMessages: Locator;
  readonly sendMessageButton: Locator;
  readonly emojiPicker: Locator;
  
  // Analytics and dashboard elements
  readonly dashboardCards: Locator;
  readonly chartContainers: Locator;
  readonly dateRangePicker: Locator;
  readonly filterButtons: Locator;
  readonly exportButton: Locator;
  
  // PWA elements
  readonly installPrompt: Locator;
  readonly addToHomescreenBanner: Locator;
  readonly offlineBanner: Locator;
  readonly updateAvailableBanner: Locator;
  
  // Accessibility elements
  readonly skipLinks: Locator;
  readonly focusTraps: Locator;
  readonly ariaLiveRegions: Locator;
  readonly landmarkRegions: Locator;

  constructor(page: Page) {
    this.page = page;
    this.mobileHelper = new MobileHelper(page);
    
    // Common elements
    this.mainContent = page.locator('main, .main-content, [role="main"], [data-testid="main-content"]');
    this.navigationMenu = page.locator('nav, .navigation, [role="navigation"]');
    this.mobileMenuToggle = page.locator('.hamburger, .mobile-menu-toggle, [aria-label="Menu"], [data-testid="mobile-menu-toggle"]');
    this.mobileMenuDrawer = page.locator('.mobile-menu, .nav-drawer, .sidebar-mobile, [data-testid="mobile-menu"]');
    this.loadingIndicator = page.locator('[data-testid="loading"], .loading, .spinner, .MuiCircularProgress-root');
    this.errorMessage = page.locator('[role="alert"], .error, .error-message, .MuiAlert-standardError');
    
    // Header and navigation
    this.header = page.locator('header, .header, [role="banner"]');
    this.logo = page.locator('.logo, [data-testid="logo"], .brand');
    this.searchBar = page.locator('input[type="search"], [placeholder*="search" i], [data-testid="search"]');
    this.userAvatar = page.locator('.user-avatar, .profile-picture, [data-testid="user-avatar"]');
    this.userMenu = page.locator('.user-menu, .profile-dropdown, [data-testid="user-menu"]');
    this.notificationBell = page.locator('.notification-bell, [data-testid="notifications"], .notifications-button');
    
    // Mobile navigation
    this.bottomNavigation = page.locator('.bottom-nav, .tab-bar, [data-testid="bottom-navigation"]');
    this.backButton = page.locator('.back-button, [aria-label="Back"], [data-testid="back-button"]');
    this.breadcrumbs = page.locator('.breadcrumbs, [aria-label="Breadcrumb"], [data-testid="breadcrumbs"]');
    this.tabBar = page.locator('.tab-bar, .tabs, [role="tablist"]');
    
    // Form elements
    this.forms = page.locator('form');
    this.textInputs = page.locator('input[type="text"], input[type="search"], textarea');
    this.emailInputs = page.locator('input[type="email"]');
    this.passwordInputs = page.locator('input[type="password"]');
    this.submitButtons = page.locator('button[type="submit"], input[type="submit"]');
    this.formErrors = page.locator('.form-error, .field-error, .invalid-feedback, [role="alert"]');
    
    // Content elements
    this.cards = page.locator('.card, .card-container, [data-testid*="card"]');
    this.modals = page.locator('.modal, .dialog, [role="dialog"], [data-testid*="modal"]');
    this.overlays = page.locator('.overlay, .backdrop, .modal-backdrop');
    this.tables = page.locator('table, .table, [role="table"]');
    this.images = page.locator('img');
    this.videos = page.locator('video');
    
    // Interactive elements
    this.buttons = page.locator('button, [role="button"]');
    this.links = page.locator('a');
    this.toggleSwitches = page.locator('input[type="checkbox"].toggle, .toggle-switch, [role="switch"]');
    this.dropdowns = page.locator('select, .dropdown, [role="combobox"]');
    this.sliders = page.locator('input[type="range"], .slider, [role="slider"]');
    
    // Hive-specific elements
    this.createHiveButton = page.locator('[data-testid="create-hive-button"], .create-hive, .new-hive-button');
    this.hiveCards = page.locator('[data-testid*="hive-card"], .hive-card, .hive-item');
    this.joinHiveButton = page.locator('[data-testid="join-hive-button"], .join-hive, .hive-join');
    this.hiveSettings = page.locator('[data-testid="hive-settings"], .hive-settings, .settings-button');
    
    // Timer elements
    this.timerDisplay = page.locator('[data-testid="timer-display"], .timer-display, .current-time');
    this.startTimerButton = page.locator('[data-testid="start-timer-button"], .start-timer, .timer-start');
    this.pauseTimerButton = page.locator('[data-testid="pause-timer-button"], .pause-timer, .timer-pause');
    this.stopTimerButton = page.locator('[data-testid="stop-timer-button"], .stop-timer, .timer-stop');
    this.timerControls = page.locator('[data-testid="timer-controls"], .timer-controls, .timer-buttons');
    
    // Chat elements
    this.chatContainer = page.locator('[data-testid="chat-container"], .chat-container, .messages');
    this.chatInput = page.locator('[data-testid="chat-input"], .chat-input, input[placeholder*="message" i]');
    this.chatMessages = page.locator('[data-testid="chat-messages"], .chat-messages, .message-list');
    this.sendMessageButton = page.locator('[data-testid="send-message"], .send-button, .message-send');
    this.emojiPicker = page.locator('[data-testid="emoji-picker"], .emoji-picker, .emoji-button');
    
    // Analytics elements
    this.dashboardCards = page.locator('[data-testid*="dashboard-card"], .dashboard-card, .metric-card');
    this.chartContainers = page.locator('[data-testid*="chart"], .chart, canvas, svg');
    this.dateRangePicker = page.locator('[data-testid="date-range"], .date-picker, input[type="date"]');
    this.filterButtons = page.locator('[data-testid*="filter"], .filter-button, .filter-chip');
    this.exportButton = page.locator('[data-testid="export"], .export-button, .download-button');
    
    // PWA elements
    this.installPrompt = page.locator('[data-testid="pwa-install"], .install-prompt, .add-to-homescreen');
    this.addToHomescreenBanner = page.locator('.a2hs-banner, .install-banner');
    this.offlineBanner = page.locator('[data-testid="offline-banner"], .offline-notice');
    this.updateAvailableBanner = page.locator('[data-testid="update-available"], .update-banner');
    
    // Accessibility elements
    this.skipLinks = page.locator('.skip-link, [data-testid="skip-link"]');
    this.focusTraps = page.locator('[data-focus-trap]');
    this.ariaLiveRegions = page.locator('[aria-live]');
    this.landmarkRegions = page.locator('[role="main"], [role="navigation"], [role="banner"], [role="contentinfo"]');
  }

  /**
   * Navigate to a page and wait for mobile layout to load
   */
  async navigateToPage(url: string): Promise<void> {
    await this.page.goto(url);
    await this.waitForMobilePageLoad();
  }

  /**
   * Wait for mobile-specific page load indicators
   */
  async waitForMobilePageLoad(): Promise<void> {
    // Wait for network idle
    await this.page.waitForLoadState('networkidle');
    
    // Wait for main content
    await expect(this.mainContent).toBeVisible({ timeout: TIMEOUTS.LONG });
    
    // Ensure loading indicators are gone
    const loadingVisible = await this.loadingIndicator.isVisible().catch(() => false);
    if (loadingVisible) {
      await expect(this.loadingIndicator).not.toBeVisible({ timeout: TIMEOUTS.LONG });
    }
    
    // Wait a bit for responsive layout to settle
    await this.page.waitForTimeout(500);
  }

  /**
   * Test mobile navigation menu functionality
   */
  async testMobileNavigation(): Promise<{
    hamburgerMenuVisible: boolean;
    menuToggles: boolean;
    menuItemsAccessible: boolean;
    closeOnItemClick: boolean;
    closeOnOutsideClick: boolean;
  }> {
    const results = await this.mobileHelper.verifyMobileMenu();
    
    // Additional test for outside click behavior
    let closeOnOutsideClick = false;
    
    if (results.hamburgerMenuVisible && results.menuToggles) {
      // Open menu
      await this.mobileMenuToggle.click();
      await this.page.waitForTimeout(300);
      
      // Click outside menu
      await this.mainContent.click({ position: { x: 10, y: 10 } });
      await this.page.waitForTimeout(300);
      
      // Check if menu closed
      closeOnOutsideClick = !(await this.mobileMenuDrawer.isVisible().catch(() => true));
    }
    
    return {
      ...results,
      closeOnOutsideClick
    };
  }

  /**
   * Test touch interactions on interactive elements
   */
  async testTouchInteractions(): Promise<{
    buttonsResponsive: boolean;
    linksResponsive: boolean;
    formsResponsive: boolean;
    averageResponseTime: number;
  }> {
    const responseTimes: number[] = [];
    let buttonsResponsive = true;
    let linksResponsive = true;
    let formsResponsive = true;
    
    // Test button responsiveness
    const buttonCount = await this.buttons.count();
    if (buttonCount > 0) {
      for (let i = 0; i < Math.min(buttonCount, 5); i++) {
        const button = this.buttons.nth(i);
        if (await button.isVisible() && await button.isEnabled()) {
          const startTime = Date.now();
          try {
            await button.tap();
            const responseTime = Date.now() - startTime;
            responseTimes.push(responseTime);
            
            if (responseTime > 300) { // Touch should respond within 300ms
              buttonsResponsive = false;
            }
          } catch (error) {
            buttonsResponsive = false;
          }
        }
      }
    }
    
    // Test link responsiveness
    const linkCount = await this.links.count();
    if (linkCount > 0) {
      for (let i = 0; i < Math.min(linkCount, 3); i++) {
        const link = this.links.nth(i);
        if (await link.isVisible()) {
          const startTime = Date.now();
          try {
            await link.tap();
            const responseTime = Date.now() - startTime;
            responseTimes.push(responseTime);
            
            if (responseTime > 300) {
              linksResponsive = false;
            }
          } catch (error) {
            linksResponsive = false;
          }
        }
      }
    }
    
    // Test form input responsiveness
    const inputCount = await this.textInputs.count();
    if (inputCount > 0) {
      const input = this.textInputs.first();
      if (await input.isVisible()) {
        const startTime = Date.now();
        try {
          await input.tap();
          await input.fill('test');
          const responseTime = Date.now() - startTime;
          responseTimes.push(responseTime);
          
          if (responseTime > 500) { // Form interactions can take slightly longer
            formsResponsive = false;
          }
        } catch (error) {
          formsResponsive = false;
        }
      }
    }
    
    const averageResponseTime = responseTimes.length > 0 
      ? responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length 
      : 0;
    
    return {
      buttonsResponsive,
      linksResponsive,
      formsResponsive,
      averageResponseTime
    };
  }

  /**
   * Test viewport scaling and zoom behavior
   */
  async testViewportScaling(): Promise<{
    zoomResponsive: boolean;
    contentScales: boolean;
    textReadable: boolean;
    horizontalScrollPrevented: boolean;
  }> {
    const initialViewport = await this.page.viewportSize();
    
    // Test zoom responsiveness by changing viewport scale
    await this.page.setViewportSize({ 
      width: initialViewport?.width || 375, 
      height: initialViewport?.height || 667 
    });
    await this.page.waitForTimeout(500);
    
    // Simulate zoom by reducing viewport and increasing device scale factor
    const client = await this.page.context().newCDPSession(this.page);
    await client.send('Emulation.setDeviceMetricsOverride', {
      width: (initialViewport?.width || 375) / 2,
      height: (initialViewport?.height || 667) / 2,
      deviceScaleFactor: 2,
      mobile: true
    });
    
    await this.page.waitForTimeout(1000);
    
    // Check if content scales properly
    const contentScales = await this.page.evaluate(() => {
      const body = document.body;
      const rect = body.getBoundingClientRect();
      return rect.width > 0 && rect.height > 0;
    });
    
    // Check text readability (minimum font size)
    const textReadable = await this.page.evaluate(() => {
      const textElements = document.querySelectorAll('p, span, div, h1, h2, h3, h4, h5, h6');
      let minFontSize = Infinity;
      
      for (const element of textElements) {
        const styles = window.getComputedStyle(element);
        const fontSize = parseFloat(styles.fontSize);
        if (fontSize > 0 && fontSize < minFontSize) {
          minFontSize = fontSize;
        }
      }
      
      return minFontSize >= 16; // Minimum readable font size
    });
    
    // Check for horizontal scroll
    const horizontalScrollPrevented = await this.page.evaluate(() => {
      return document.documentElement.scrollWidth <= window.innerWidth;
    });
    
    // Reset viewport
    await client.send('Emulation.clearDeviceMetricsOverride');
    if (initialViewport) {
      await this.page.setViewportSize(initialViewport);
    }
    
    return {
      zoomResponsive: contentScales,
      contentScales,
      textReadable,
      horizontalScrollPrevented
    };
  }

  /**
   * Test form interactions on mobile
   */
  async testMobileFormInteractions(): Promise<{
    virtualKeyboardHandled: boolean;
    inputTypesOptimized: boolean;
    formValidationVisible: boolean;
    submitButtonAccessible: boolean;
  }> {
    const formResults = await this.mobileHelper.testMobileFormUsability();
    
    // Test virtual keyboard handling
    const keyboardResult = await this.mobileHelper.testVirtualKeyboard();
    
    // Test form validation visibility
    let formValidationVisible = true;
    const formCount = await this.forms.count();
    
    if (formCount > 0) {
      const form = this.forms.first();
      const requiredInput = form.locator('input[required], input[data-required="true"]').first();
      
      if (await requiredInput.isVisible()) {
        // Try to submit form without filling required field
        const submitButton = form.locator('button[type="submit"]').first();
        if (await submitButton.isVisible()) {
          await submitButton.tap();
          await this.page.waitForTimeout(500);
          
          // Check if validation error is visible
          const errorVisible = await this.formErrors.isVisible().catch(() => false);
          formValidationVisible = errorVisible;
        }
      }
    }
    
    // Test submit button accessibility
    const submitButtonCount = await this.submitButtons.count();
    let submitButtonAccessible = false;
    
    if (submitButtonCount > 0) {
      const submitButton = this.submitButtons.first();
      const buttonBox = await submitButton.boundingBox();
      submitButtonAccessible = !!buttonBox && 
        buttonBox.width >= 44 && buttonBox.height >= 44; // iOS minimum touch target
    }
    
    return {
      virtualKeyboardHandled: keyboardResult.keyboardTriggered && !keyboardResult.layoutShifted,
      inputTypesOptimized: formResults.inputTypesOptimized,
      formValidationVisible,
      submitButtonAccessible
    };
  }

  /**
   * Test card layout responsiveness
   */
  async testCardLayout(): Promise<{
    cardsStackProperly: boolean;
    cardContentVisible: boolean;
    cardInteractive: boolean;
    cardsResponsive: boolean;
  }> {
    const cardCount = await this.cards.count();
    
    if (cardCount === 0) {
      return {
        cardsStackProperly: true,
        cardContentVisible: true,
        cardInteractive: true,
        cardsResponsive: true
      };
    }
    
    // Test card stacking (should be single column on mobile)
    const cardsStackProperly = await this.page.evaluate(() => {
      const cards = Array.from(document.querySelectorAll('.card, .card-container, [data-testid*="card"]'));
      if (cards.length < 2) return true;
      
      const firstCardRect = cards[0].getBoundingClientRect();
      const secondCardRect = cards[1].getBoundingClientRect();
      
      // Cards should stack vertically (second card below first)
      return secondCardRect.top >= firstCardRect.bottom - 10; // Allow 10px tolerance
    });
    
    // Test card content visibility
    let cardContentVisible = true;
    for (let i = 0; i < Math.min(cardCount, 3); i++) {
      const card = this.cards.nth(i);
      const cardText = await card.textContent();
      if (!cardText || cardText.trim().length === 0) {
        cardContentVisible = false;
        break;
      }
    }
    
    // Test card interactivity
    let cardInteractive = true;
    const firstCard = this.cards.first();
    if (await firstCard.isVisible()) {
      try {
        await firstCard.tap();
        await this.page.waitForTimeout(200);
      } catch (error) {
        cardInteractive = false;
      }
    }
    
    // Test card responsiveness across viewports
    const currentViewport = await this.page.viewportSize();
    let cardsResponsive = true;
    
    if (currentViewport) {
      // Test at mobile viewport
      await this.page.setViewportSize({ width: 375, height: 667 });
      await this.page.waitForTimeout(500);
      
      const mobileCardVisible = await this.cards.first().isVisible().catch(() => false);
      
      // Test at tablet viewport
      await this.page.setViewportSize({ width: 768, height: 1024 });
      await this.page.waitForTimeout(500);
      
      const tabletCardVisible = await this.cards.first().isVisible().catch(() => false);
      
      cardsResponsive = mobileCardVisible && tabletCardVisible;
      
      // Restore original viewport
      await this.page.setViewportSize(currentViewport);
    }
    
    return {
      cardsStackProperly,
      cardContentVisible,
      cardInteractive,
      cardsResponsive
    };
  }

  /**
   * Test modal and overlay behavior on mobile
   */
  async testModalBehavior(): Promise<{
    modalResponsive: boolean;
    modalClosable: boolean;
    modalAccessible: boolean;
    overlayPreventsScroll: boolean;
  }> {
    // Try to trigger a modal (look for modal trigger buttons)
    const modalTrigger = this.page.locator('button:has-text("modal"), button:has-text("open"), .modal-trigger').first();
    
    if (!(await modalTrigger.isVisible().catch(() => false))) {
      return {
        modalResponsive: true,
        modalClosable: true,
        modalAccessible: true,
        overlayPreventsScroll: true
      };
    }
    
    // Open modal
    await modalTrigger.tap();
    await this.page.waitForTimeout(500);
    
    const modalVisible = await this.modals.isVisible().catch(() => false);
    
    if (!modalVisible) {
      return {
        modalResponsive: false,
        modalClosable: false,
        modalAccessible: false,
        overlayPreventsScroll: false
      };
    }
    
    // Test modal responsiveness
    const modalBox = await this.modals.first().boundingBox();
    const viewport = await this.page.viewportSize();
    const modalResponsive = !!modalBox && !!viewport && 
      modalBox.width <= viewport.width && modalBox.height <= viewport.height;
    
    // Test modal closability
    const closeButton = this.modals.locator('.close, .modal-close, button[aria-label="Close"]').first();
    let modalClosable = false;
    
    if (await closeButton.isVisible().catch(() => false)) {
      await closeButton.tap();
      await this.page.waitForTimeout(500);
      modalClosable = !(await this.modals.isVisible().catch(() => true));
    }
    
    // Test accessibility (focus trap, ARIA attributes)
    const modalAccessible = await this.page.evaluate(() => {
      const modal = document.querySelector('[role="dialog"], .modal');
      return modal ? 
        modal.hasAttribute('aria-labelledby') || modal.hasAttribute('aria-label') : 
        false;
    });
    
    // Test overlay scroll prevention
    const overlayPreventsScroll = await this.page.evaluate(() => {
      const body = document.body;
      const bodyStyles = window.getComputedStyle(body);
      return bodyStyles.overflow === 'hidden' || body.style.overflow === 'hidden';
    });
    
    return {
      modalResponsive,
      modalClosable,
      modalAccessible,
      overlayPreventsScroll
    };
  }

  /**
   * Test image and media responsiveness
   */
  async testMediaResponsiveness(): Promise<{
    imagesResponsive: boolean;
    imagesOptimized: boolean;
    videosResponsive: boolean;
    mediaAccessible: boolean;
  }> {
    // Test image responsiveness
    const imageCount = await this.images.count();
    let imagesResponsive = true;
    let imagesOptimized = true;
    
    if (imageCount > 0) {
      for (let i = 0; i < Math.min(imageCount, 5); i++) {
        const image = this.images.nth(i);
        const imageBox = await image.boundingBox();
        const viewport = await this.page.viewportSize();
        
        if (imageBox && viewport) {
          // Image should not overflow viewport
          if (imageBox.width > viewport.width) {
            imagesResponsive = false;
          }
          
          // Check for responsive attributes
          const hasResponsiveAttrs = await image.evaluate((img) => {
            return img.hasAttribute('srcset') || 
                   img.hasAttribute('sizes') || 
                   img.style.maxWidth === '100%' ||
                   window.getComputedStyle(img).maxWidth === '100%';
          });
          
          if (!hasResponsiveAttrs) {
            imagesOptimized = false;
          }
        }
      }
    }
    
    // Test video responsiveness
    const videoCount = await this.videos.count();
    let videosResponsive = true;
    
    if (videoCount > 0) {
      for (let i = 0; i < Math.min(videoCount, 3); i++) {
        const video = this.videos.nth(i);
        const videoBox = await video.boundingBox();
        const viewport = await this.page.viewportSize();
        
        if (videoBox && viewport && videoBox.width > viewport.width) {
          videosResponsive = false;
          break;
        }
      }
    }
    
    // Test media accessibility
    const mediaAccessible = await this.page.evaluate(() => {
      const images = Array.from(document.querySelectorAll('img'));
      const videos = Array.from(document.querySelectorAll('video'));
      
      const imagesHaveAlt = images.length === 0 || images.every(img => 
        img.hasAttribute('alt') || img.hasAttribute('aria-label')
      );
      
      const videosHaveLabels = videos.length === 0 || videos.every(video => 
        video.hasAttribute('aria-label') || 
        video.querySelector('track[kind="captions"]') !== null
      );
      
      return imagesHaveAlt && videosHaveLabels;
    });
    
    return {
      imagesResponsive,
      imagesOptimized,
      videosResponsive,
      mediaAccessible
    };
  }

  /**
   * Test PWA features on mobile
   */
  async testPWAFeatures(): Promise<{
    installPromptWorks: boolean;
    offlineFunctionality: boolean;
    serviceWorkerActive: boolean;
    manifestValid: boolean;
  }> {
    const pwaResults = await this.mobileHelper.testPWAInstallPrompt();
    
    // Test offline functionality
    let offlineFunctionality = false;
    try {
      // Go offline
      await this.page.context().setOffline(true);
      await this.page.reload();
      await this.page.waitForTimeout(2000);
      
      // Check if offline banner appears or cached content loads
      const offlineBannerVisible = await this.offlineBanner.isVisible().catch(() => false);
      const contentStillVisible = await this.mainContent.isVisible().catch(() => false);
      
      offlineFunctionality = offlineBannerVisible || contentStillVisible;
      
      // Go back online
      await this.page.context().setOffline(false);
    } catch (error) {
      console.warn('Offline test failed:', error);
    }
    
    // Test service worker
    const serviceWorkerActive = await this.page.evaluate(() => {
      return 'serviceWorker' in navigator && 
             navigator.serviceWorker.controller !== null;
    });
    
    // Test manifest
    const manifestValid = await this.page.evaluate(() => {
      const manifestLink = document.querySelector('link[rel="manifest"]');
      return !!manifestLink;
    });
    
    return {
      installPromptWorks: pwaResults.installPromptTriggered || pwaResults.installButtonVisible,
      offlineFunctionality,
      serviceWorkerActive,
      manifestValid
    };
  }

  /**
   * Collect comprehensive mobile performance metrics
   */
  async collectMobileMetrics(): Promise<MobilePerformanceMetrics> {
    return await this.mobileHelper.collectMobilePerformanceMetrics();
  }

  /**
   * Take screenshot for mobile testing documentation
   */
  async takeScreenshot(name: string): Promise<string> {
    const screenshotPath = `mobile-${name}-${Date.now()}.png`;
    await this.page.screenshot({ path: screenshotPath, fullPage: false });
    return screenshotPath;
  }

  /**
   * Reset to default desktop viewport
   */
  async resetViewport(): Promise<void> {
    await this.mobileHelper.resetViewport();
  }

  /**
   * Emulate specific device
   */
  async emulateDevice(deviceName: string): Promise<void> {
    await this.mobileHelper.emulateDevice(deviceName);
  }

  /**
   * Test at specific viewport breakpoint
   */
  async testAtBreakpoint(width: number, height: number): Promise<ViewportTestResult> {
    const breakpoint = { name: `custom-${width}x${height}`, width, height, description: 'Custom breakpoint' };
    return await this.mobileHelper.testAtBreakpoint(breakpoint);
  }
}

export default MobilePage;