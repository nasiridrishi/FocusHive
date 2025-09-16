/**
 * Page Object Model for Performance Testing
 * Provides utilities for performance measurement across all FocusHive pages
 */

import {expect, Locator, Page} from '@playwright/test';
import {TEST_URLS, TIMEOUTS} from '../helpers/test-data';
import {
  MemoryUsage,
  PerformanceHelper,
  PerformanceMetrics,
  WebSocketMetrics
} from '../helpers/performance.helper';

export class PerformancePage {
  readonly page: Page;
  readonly performanceHelper: PerformanceHelper;

  // Common page elements for performance testing
  readonly loadingIndicator: Locator;
  readonly errorMessage: Locator;
  readonly mainContent: Locator;
  readonly navigationMenu: Locator;

  // Hive-related elements
  readonly createHiveButton: Locator;
  readonly hiveNameInput: Locator;
  readonly hiveDescriptionInput: Locator;
  readonly createHiveSubmit: Locator;
  readonly startTimerButton: Locator;
  readonly stopTimerButton: Locator;
  readonly presenceIndicators: Locator;

  // Analytics elements
  readonly timeRangeSelect: Locator;
  readonly exportDataButton: Locator;
  readonly analyticsCharts: Locator;
  readonly productivityMetrics: Locator;

  // WebSocket testing elements
  readonly chatInput: Locator;
  readonly chatMessages: Locator;
  readonly statusIndicator: Locator;
  readonly connectionStatus: Locator;

  constructor(page: Page) {
    this.page = page;
    this.performanceHelper = new PerformanceHelper(page);

    // Common elements
    this.loadingIndicator = page.locator('[data-testid="loading"], .loading, .spinner');
    this.errorMessage = page.locator('[data-testid="error"], .error, .alert-error');
    this.mainContent = page.locator('main, .main-content, [data-testid="main-content"]');
    this.navigationMenu = page.locator('nav, .navigation, [data-testid="navigation"]');

    // Hive elements
    this.createHiveButton = page.locator('[data-testid="create-hive-button"]');
    this.hiveNameInput = page.locator('[data-testid="hive-name-input"]');
    this.hiveDescriptionInput = page.locator('[data-testid="hive-description-input"]');
    this.createHiveSubmit = page.locator('[data-testid="create-hive-submit"]');
    this.startTimerButton = page.locator('[data-testid="start-timer-button"]');
    this.stopTimerButton = page.locator('[data-testid="stop-timer-button"]');
    this.presenceIndicators = page.locator('[data-testid="presence-indicator"], .presence-indicator');

    // Analytics elements
    this.timeRangeSelect = page.locator('[data-testid="time-range-select"]');
    this.exportDataButton = page.locator('[data-testid="export-data-button"]');
    this.analyticsCharts = page.locator('[data-testid="analytics-chart"], .chart, canvas');
    this.productivityMetrics = page.locator('[data-testid="productivity-metrics"]');

    // WebSocket elements
    this.chatInput = page.locator('[data-testid="chat-input"], input[placeholder*="message"]');
    this.chatMessages = page.locator('[data-testid="chat-messages"], .messages, .chat-container');
    this.statusIndicator = page.locator('[data-testid="status-indicator"]');
    this.connectionStatus = page.locator('[data-testid="connection-status"], .connection-status');
  }

  /**
   * Navigate to a specific URL and measure performance
   */
  async navigateAndMeasure(url: string): Promise<PerformanceMetrics> {
    return await this.performanceHelper.collectPerformanceMetrics(url);
  }

  /**
   * Navigate to login page and measure performance
   */
  async gotoLogin(): Promise<PerformanceMetrics> {
    return await this.navigateAndMeasure(TEST_URLS.LOGIN);
  }

  /**
   * Navigate to dashboard and measure performance
   */
  async gotoDashboard(): Promise<PerformanceMetrics> {
    return await this.navigateAndMeasure(TEST_URLS.DASHBOARD);
  }

  /**
   * Navigate to hive page and measure performance
   */
  async gotoHive(): Promise<PerformanceMetrics> {
    return await this.navigateAndMeasure('/hive');
  }

  /**
   * Navigate to analytics page and measure performance
   */
  async gotoAnalytics(): Promise<PerformanceMetrics> {
    return await this.navigateAndMeasure('/analytics');
  }

  /**
   * Navigate to profile page and measure performance
   */
  async gotoProfile(): Promise<PerformanceMetrics> {
    return await this.navigateAndMeasure('/profile');
  }

  /**
   * Navigate to forum page and measure performance
   */
  async gotoForum(): Promise<PerformanceMetrics> {
    return await this.navigateAndMeasure('/forum');
  }

  /**
   * Navigate to buddy system page and measure performance
   */
  async gotoBuddy(): Promise<PerformanceMetrics> {
    return await this.navigateAndMeasure('/buddy');
  }

  /**
   * Navigate to gamification page and measure performance
   */
  async gotoGamification(): Promise<PerformanceMetrics> {
    return await this.navigateAndMeasure('/gamification');
  }

  /**
   * Wait for page to be fully loaded and interactive
   */
  async waitForPageLoad(): Promise<void> {
    // Wait for network idle
    await this.page.waitForLoadState('networkidle');

    // Wait for main content to be visible
    await expect(this.mainContent).toBeVisible({timeout: TIMEOUTS.LONG});

    // Ensure no loading indicators are visible
    const loadingVisible = await this.loadingIndicator.isVisible().catch(() => false);
    if (loadingVisible) {
      await expect(this.loadingIndicator).not.toBeVisible({timeout: TIMEOUTS.LONG});
    }
  }

  /**
   * Wait for specific element to be visible with timeout
   */
  async waitForElement(locator: Locator, timeout: number = TIMEOUTS.MEDIUM): Promise<void> {
    await expect(locator).toBeVisible({timeout});
  }

  /**
   * Simulate heavy interaction on current page
   */
  async simulateHeavyInteraction(): Promise<void> {
    const startTime = Date.now();

    try {
      // Scroll to bottom of page
      await this.page.evaluate(() => {
        window.scrollTo(0, document.body.scrollHeight);
      });

      await this.page.waitForTimeout(500);

      // Scroll to top
      await this.page.evaluate(() => {
        window.scrollTo(0, 0);
      });

      await this.page.waitForTimeout(500);

      // Click on navigation elements if available
      const navItems = await this.navigationMenu.locator('a, button').count();
      if (navItems > 0) {
        for (let i = 0; i < Math.min(navItems, 3); i++) {
          const navItem = this.navigationMenu.locator('a, button').nth(i);
          if (await navItem.isVisible()) {
            await navItem.hover();
            await this.page.waitForTimeout(200);
          }
        }
      }

      // Interact with main content elements
      const interactiveElements = await this.mainContent.locator('button, input, select, [role="button"]').count();
      if (interactiveElements > 0) {
        for (let i = 0; i < Math.min(interactiveElements, 5); i++) {
          const element = this.mainContent.locator('button, input, select, [role="button"]').nth(i);
          if (await element.isVisible() && await element.isEnabled()) {
            await element.hover();
            await this.page.waitForTimeout(100);
          }
        }
      }

    } catch (error) {
      console.warn('Heavy interaction simulation failed:', error);
    }

    const duration = Date.now() - startTime;
    console.log(`Heavy interaction simulation completed in ${duration}ms`);
  }

  /**
   * Create a hive with performance monitoring
   */
  async createHiveWithMetrics(hiveName: string, description: string): Promise<{
    metrics: PerformanceMetrics;
    success: boolean
  }> {
    const _startTime = Date.now();
    let success = false;

    try {
      // Click create hive button
      await this.createHiveButton.click();
      await this.waitForElement(this.hiveNameInput);

      // Fill hive details
      await this.hiveNameInput.fill(hiveName);
      await this.hiveDescriptionInput.fill(description);

      // Submit form
      await this.createHiveSubmit.click();

      // Wait for creation to complete
      await this.waitForPageLoad();

      success = true;
    } catch (error) {
      console.warn('Hive creation failed:', error);
      success = false;
    }

    // Measure performance after operation
    const metrics = await this.performanceHelper.collectPerformanceMetrics(this.page.url());

    return {metrics, success};
  }

  /**
   * Start timer with performance monitoring
   */
  async startTimerWithMetrics(): Promise<{ metrics: PerformanceMetrics; success: boolean }> {
    let success = false;

    try {
      await this.startTimerButton.click();
      await this.page.waitForTimeout(1000); // Wait for timer to start
      success = true;
    } catch (error) {
      console.warn('Timer start failed:', error);
      success = false;
    }

    const metrics = await this.performanceHelper.collectPerformanceMetrics(this.page.url());
    return {metrics, success};
  }

  /**
   * Test WebSocket connection performance
   */
  async testWebSocketConnection(): Promise<WebSocketMetrics> {
    // Try to connect to WebSocket endpoint
    const wsUrl = 'ws://localhost:8080/ws';
    return await this.performanceHelper.testWebSocketPerformance(wsUrl, 50);
  }

  /**
   * Test chat message latency
   */
  async testChatMessageLatency(messageCount: number = 10): Promise<number[]> {
    const latencies: number[] = [];

    if (!await this.chatInput.isVisible()) {
      console.warn('Chat input not available for latency testing');
      return latencies;
    }

    for (let i = 0; i < messageCount; i++) {
      const startTime = Date.now();
      const testMessage = `Performance test message ${i} - ${startTime}`;

      try {
        // Send message
        await this.chatInput.fill(testMessage);
        await this.chatInput.press('Enter');

        // Wait for message to appear in chat
        const messageLocator = this.chatMessages.locator(`text="${testMessage}"`).first();
        await expect(messageLocator).toBeVisible({timeout: 5000});

        const latency = Date.now() - startTime;
        latencies.push(latency);

        // Clear input for next message
        await this.chatInput.clear();
        await this.page.waitForTimeout(100);

      } catch (error) {
        console.warn(`Chat message ${i} failed:`, error);
        latencies.push(-1); // Indicate failure
      }
    }

    return latencies;
  }

  /**
   * Monitor memory usage during operation
   */
  async monitorMemoryDuringOperation(
      operation: () => Promise<void>,
      duration: number = 60000
  ): Promise<MemoryUsage[]> {
    // Start memory monitoring
    const memoryPromise = this.performanceHelper.monitorMemoryUsage(duration);

    // Execute the operation
    await operation();

    // Wait for monitoring to complete
    const memoryData = await memoryPromise;

    return memoryData;
  }

  /**
   * Test analytics dashboard performance
   */
  async testAnalyticsDashboardPerformance(): Promise<{
    loadMetrics: PerformanceMetrics;
    filterChangeTime: number;
    exportTime: number
  }> {
    // Navigate to analytics and measure load performance
    const loadMetrics = await this.gotoAnalytics();

    // Test filter change performance
    const filterStartTime = Date.now();
    try {
      await this.timeRangeSelect.selectOption('last-7-days');
      await this.waitForElement(this.analyticsCharts);
    } catch (error) {
      console.warn('Filter change failed:', error);
    }
    const filterChangeTime = Date.now() - filterStartTime;

    // Test export performance
    const exportStartTime = Date.now();
    try {
      await this.exportDataButton.click();
      await this.page.waitForTimeout(3000); // Wait for export to process
    } catch (error) {
      console.warn('Export failed:', error);
    }
    const exportTime = Date.now() - exportStartTime;

    return {
      loadMetrics,
      filterChangeTime,
      exportTime
    };
  }

  /**
   * Verify page performance meets thresholds
   */
  async verifyPerformanceThresholds(metrics: PerformanceMetrics): Promise<{
    passed: boolean;
    failures: string[];
  }> {
    return this.performanceHelper.validatePerformanceThresholds(metrics);
  }

  /**
   * Check for JavaScript errors on page
   */
  async checkForJavaScriptErrors(): Promise<string[]> {
    const errors: string[] = [];

    this.page.on('console', (msg) => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });

    this.page.on('pageerror', (error) => {
      errors.push(`Page error: ${error.message}`);
    });

    return errors;
  }

  /**
   * Get page resource loading summary
   */
  async getResourceLoadingSummary(): Promise<{
    totalResources: number;
    totalSize: number;
    loadTime: number;
    failedResources: number;
  }> {
    const resources = await this.page.evaluate(() => {
      const entries = performance.getEntriesByType('resource') as PerformanceResourceTiming[];
      let totalSize = 0;
      let maxLoadTime = 0;
      let failedCount = 0;

      entries.forEach(entry => {
        if (entry.transferSize) {
          totalSize += entry.transferSize;
        }

        const loadTime = entry.responseEnd - entry.startTime;
        if (loadTime > maxLoadTime) {
          maxLoadTime = loadTime;
        }

        // Check if resource failed to load (no response)
        if (entry.responseEnd === 0) {
          failedCount++;
        }
      });

      return {
        totalResources: entries.length,
        totalSize,
        loadTime: maxLoadTime,
        failedResources: failedCount
      };
    });

    return resources;
  }

  /**
   * Test page responsiveness under simulated slow network
   */
  async testSlowNetworkPerformance(): Promise<PerformanceMetrics> {
    // Simulate slow 3G network
    const client = await this.page.context().newCDPSession(this.page);
    await client.send('Network.enable');
    await client.send('Network.emulateNetworkConditions', {
      offline: false,
      downloadThroughput: 1.5 * 1024 * 1024 / 8, // 1.5 Mbps
      uploadThroughput: 750 * 1024 / 8, // 750 Kbps  
      latency: 40 // 40ms latency
    });

    // Measure performance under slow network
    const metrics = await this.performanceHelper.collectPerformanceMetrics(this.page.url());

    // Disable network throttling
    await client.send('Network.emulateNetworkConditions', {
      offline: false,
      downloadThroughput: -1,
      uploadThroughput: -1,
      latency: 0
    });

    return metrics;
  }
}

export default PerformancePage;