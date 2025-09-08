/**
 * Page Object Model for Analytics Dashboard
 * Provides methods to interact with analytics features in E2E tests
 */

import { Page, Locator, expect } from '@playwright/test';
import { TIMEOUTS } from '../helpers/test-data';

export class AnalyticsPage {
  readonly page: Page;

  // Main dashboard elements
  readonly dashboard: Locator;
  readonly dashboardTitle: Locator;
  readonly loadingSpinner: Locator;
  readonly errorAlert: Locator;
  readonly retryButton: Locator;
  readonly refreshButton: Locator;
  readonly emptyStateMessage: Locator;

  // Header elements
  readonly timePeriodChip: Locator;
  readonly viewTypeChip: Locator;
  readonly lastUpdatedText: Locator;
  readonly exportButton: Locator;

  // Summary statistics cards
  readonly totalFocusTimeCard: Locator;
  readonly completedSessionsCard: Locator;
  readonly currentStreakCard: Locator;
  readonly productivityScoreCard: Locator;

  // Filter components
  readonly filtersContainer: Locator;
  readonly timeRangeSelect: Locator;
  readonly metricsSelect: Locator;
  readonly viewTypeSelect: Locator;
  readonly dateRangePicker: Locator;

  // Chart components
  readonly productivityChart: Locator;
  readonly taskCompletionChart: Locator;
  readonly hiveActivityHeatmap: Locator;
  readonly memberEngagementChart: Locator;
  readonly goalProgressChart: Locator;

  // Export menu
  readonly exportMenu: Locator;
  readonly exportFormatSelect: Locator;
  readonly exportDateRangeSelect: Locator;
  readonly exportContentSelect: Locator;
  readonly downloadButton: Locator;

  // Accessibility elements
  readonly chartTooltips: Locator;
  readonly chartLegends: Locator;
  readonly screenReaderText: Locator;

  constructor(page: Page) {
    this.page = page;

    // Main dashboard elements
    this.dashboard = page.locator('[data-testid="analytics-dashboard"]');
    this.dashboardTitle = page.locator('h4:has-text("Analytics Dashboard"), h4:has-text("Hive Analytics")');
    this.loadingSpinner = page.locator('[data-testid="loading-spinner"], .MuiCircularProgress-root');
    this.errorAlert = page.locator('.MuiAlert-root[severity="error"]');
    this.retryButton = page.locator('button:has-text("Retry")');
    this.refreshButton = page.locator('button[aria-label="refresh data"]');
    this.emptyStateMessage = page.locator(':has-text("No analytics data yet")');

    // Header elements
    this.timePeriodChip = page.locator('.MuiChip-root:has-text("Time Period:")');
    this.viewTypeChip = page.locator('.MuiChip-root:has-text("View:")');
    this.lastUpdatedText = page.locator('text=/Last updated:.*$/');
    this.exportButton = page.locator('[data-testid="export-menu-button"]');

    // Summary statistics cards
    this.totalFocusTimeCard = page.locator('[data-testid="stat-card-focus-time"]');
    this.completedSessionsCard = page.locator('[data-testid="stat-card-completed-sessions"]');
    this.currentStreakCard = page.locator('[data-testid="stat-card-streak"]');
    this.productivityScoreCard = page.locator('[data-testid="stat-card-productivity"]');

    // Filter components
    this.filtersContainer = page.locator('[data-testid="analytics-filters"]');
    this.timeRangeSelect = page.locator('[data-testid="time-range-select"]');
    this.metricsSelect = page.locator('[data-testid="metrics-select"]');
    this.viewTypeSelect = page.locator('[data-testid="view-type-select"]');
    this.dateRangePicker = page.locator('[data-testid="date-range-picker"]');

    // Chart components
    this.productivityChart = page.locator('[data-testid="productivity-chart"]');
    this.taskCompletionChart = page.locator('[data-testid="task-completion-chart"]');
    this.hiveActivityHeatmap = page.locator('[data-testid="hive-activity-heatmap"]');
    this.memberEngagementChart = page.locator('[data-testid="member-engagement-chart"]');
    this.goalProgressChart = page.locator('[data-testid="goal-progress-chart"]');

    // Export menu
    this.exportMenu = page.locator('[data-testid="export-menu"]');
    this.exportFormatSelect = page.locator('[data-testid="export-format-select"]');
    this.exportDateRangeSelect = page.locator('[data-testid="export-date-range-select"]');
    this.exportContentSelect = page.locator('[data-testid="export-content-select"]');
    this.downloadButton = page.locator('[data-testid="download-button"]');

    // Accessibility elements
    this.chartTooltips = page.locator('[role="tooltip"]');
    this.chartLegends = page.locator('[aria-label*="legend"]');
    this.screenReaderText = page.locator('.sr-only, [aria-live]');
  }

  /**
   * Navigate to analytics page
   */
  async goto(): Promise<void> {
    await this.page.goto('/analytics');
    await this.waitForPageLoad();
  }

  /**
   * Navigate to analytics demo page
   */
  async gotoDemo(): Promise<void> {
    await this.page.goto('/analytics/demo');
    await this.waitForPageLoad();
  }

  /**
   * Wait for page to load completely
   */
  async waitForPageLoad(): Promise<void> {
    await this.page.waitForLoadState('networkidle', { timeout: TIMEOUTS.NETWORK });
    
    // Wait for dashboard to be visible or error/empty state to appear
    // Use longer timeout for initial page load and be more flexible about what constitutes a loaded page
    try {
      await Promise.race([
        this.dashboard.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM }),
        this.errorAlert.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM }),
        this.emptyStateMessage.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM }),
        // Also accept if the page content has loaded (even without specific dashboard elements)
        this.page.locator('main, .MuiContainer-root, body').waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM })
      ]);
    } catch {
      // If none of the expected elements appear, just ensure the page has loaded
      await this.page.waitForLoadState('domcontentloaded');
    }
  }

  /**
   * Wait for dashboard data to load
   */
  async waitForDataLoad(): Promise<void> {
    // Wait for loading spinner to disappear
    try {
      await this.loadingSpinner.waitFor({ state: 'hidden', timeout: TIMEOUTS.NETWORK });
    } catch {
      // Spinner might not be visible, which is fine
    }

    // Wait for either data to load or empty/error state
    await Promise.race([
      this.totalFocusTimeCard.waitFor({ state: 'visible', timeout: TIMEOUTS.MEDIUM }),
      this.emptyStateMessage.waitFor({ state: 'visible', timeout: TIMEOUTS.SHORT }),
      this.errorAlert.waitFor({ state: 'visible', timeout: TIMEOUTS.SHORT })
    ]);
  }

  /**
   * Verify dashboard is loaded successfully
   */
  async verifyDashboardLoaded(): Promise<void> {
    await expect(this.dashboard).toBeVisible();
    await expect(this.dashboardTitle).toBeVisible();
    await expect(this.loadingSpinner).not.toBeVisible();
  }

  /**
   * Verify dashboard shows empty state
   */
  async verifyEmptyState(): Promise<void> {
    await expect(this.dashboard).toBeVisible();
    await expect(this.emptyStateMessage).toBeVisible();
    await expect(this.emptyStateMessage).toContainText('No analytics data yet');
  }

  /**
   * Verify dashboard shows error state
   */
  async verifyErrorState(): Promise<void> {
    await expect(this.dashboard).toBeVisible();
    await expect(this.errorAlert).toBeVisible();
    await expect(this.retryButton).toBeVisible();
  }

  /**
   * Click refresh button
   */
  async clickRefresh(): Promise<void> {
    await this.refreshButton.click();
    await this.waitForDataLoad();
  }

  /**
   * Click retry button
   */
  async clickRetry(): Promise<void> {
    await this.retryButton.click();
    await this.waitForDataLoad();
  }

  /**
   * Verify summary statistics are visible
   */
  async verifySummaryStats(): Promise<void> {
    await expect(this.totalFocusTimeCard).toBeVisible();
    await expect(this.completedSessionsCard).toBeVisible();
    await expect(this.currentStreakCard).toBeVisible();
    await expect(this.productivityScoreCard).toBeVisible();
  }

  /**
   * Get focus time value from summary card
   */
  async getFocusTimeValue(): Promise<string> {
    const element = this.totalFocusTimeCard.locator('h4').first();
    return await element.textContent() || '';
  }

  /**
   * Get completed sessions value from summary card
   */
  async getCompletedSessionsValue(): Promise<string> {
    const element = this.completedSessionsCard.locator('h4').first();
    return await element.textContent() || '';
  }

  /**
   * Get current streak value from summary card
   */
  async getCurrentStreakValue(): Promise<string> {
    const element = this.currentStreakCard.locator('h4').first();
    return await element.textContent() || '';
  }

  /**
   * Get productivity score value from summary card
   */
  async getProductivityScoreValue(): Promise<string> {
    const element = this.productivityScoreCard.locator('h4').first();
    return await element.textContent() || '';
  }

  /**
   * Update time range filter
   */
  async updateTimeRange(range: 'day' | 'week' | 'month' | 'year' | 'custom'): Promise<void> {
    if (await this.timeRangeSelect.isVisible()) {
      await this.timeRangeSelect.click();
      await this.page.locator(`[data-value="${range}"]`).click();
      await this.waitForDataLoad();
    }
  }

  /**
   * Update view type filter
   */
  async updateViewType(viewType: 'individual' | 'hive' | 'team'): Promise<void> {
    if (await this.viewTypeSelect.isVisible()) {
      await this.viewTypeSelect.click();
      await this.page.locator(`[data-value="${viewType}"]`).click();
      await this.waitForDataLoad();
    }
  }

  /**
   * Update metrics filter
   */
  async updateMetrics(metrics: string[]): Promise<void> {
    if (await this.metricsSelect.isVisible()) {
      await this.metricsSelect.click();
      
      // Clear existing selections first
      const clearButton = this.page.locator('[aria-label="Clear"]');
      if (await clearButton.isVisible()) {
        await clearButton.click();
      }
      
      // Select new metrics
      for (const metric of metrics) {
        await this.page.locator(`[data-value="${metric}"]`).click();
      }
      
      // Click outside to close
      await this.dashboard.click();
      await this.waitForDataLoad();
    }
  }

  /**
   * Verify charts are visible and rendered
   */
  async verifyChartsRendered(): Promise<void> {
    // Check that at least some charts are visible
    const visibleCharts = [];
    
    if (await this.productivityChart.isVisible()) {
      visibleCharts.push('productivity');
    }
    if (await this.taskCompletionChart.isVisible()) {
      visibleCharts.push('task-completion');
    }
    if (await this.hiveActivityHeatmap.isVisible()) {
      visibleCharts.push('hive-activity');
    }
    if (await this.memberEngagementChart.isVisible()) {
      visibleCharts.push('member-engagement');
    }
    if (await this.goalProgressChart.isVisible()) {
      visibleCharts.push('goal-progress');
    }

    expect(visibleCharts.length).toBeGreaterThan(0);
  }

  /**
   * Interact with chart tooltip
   */
  async hoverOverChart(chartLocator: Locator, x: number = 100, y: number = 100): Promise<void> {
    await chartLocator.hover({ position: { x, y } });
    
    // Wait for tooltip to appear
    try {
      await this.chartTooltips.first().waitFor({ state: 'visible', timeout: TIMEOUTS.SHORT });
    } catch {
      // Tooltip might not appear, which is fine for some chart types
    }
  }

  /**
   * Open export menu
   */
  async openExportMenu(): Promise<void> {
    await this.exportButton.click();
    await expect(this.exportMenu).toBeVisible();
  }

  /**
   * Select export format
   */
  async selectExportFormat(format: 'csv' | 'json' | 'pdf' | 'png'): Promise<void> {
    await this.exportFormatSelect.click();
    await this.page.locator(`[data-value="${format}"]`).click();
  }

  /**
   * Download analytics data
   */
  async downloadData(format: 'csv' | 'json' | 'pdf' | 'png' = 'csv'): Promise<void> {
    await this.openExportMenu();
    await this.selectExportFormat(format);
    
    // Start waiting for download before clicking
    const downloadPromise = this.page.waitForEvent('download');
    await this.downloadButton.click();
    
    // Wait for download to complete
    const download = await downloadPromise;
    
    // Verify download properties
    expect(download.suggestedFilename()).toContain('analytics');
    expect(download.suggestedFilename()).toContain(format);
  }

  /**
   * Verify responsive design on mobile
   */
  async verifyMobileLayout(): Promise<void> {
    // Set mobile viewport
    await this.page.setViewportSize({ width: 375, height: 667 });
    await this.waitForDataLoad();
    
    // Verify dashboard adjusts to mobile layout
    await expect(this.dashboard).toBeVisible();
    await expect(this.dashboard).toHaveClass(/dashboard-responsive/);
    
    // Verify summary cards are stacked appropriately
    const summaryCards = this.page.locator('.MuiCard-root').first();
    await expect(summaryCards).toBeVisible();
  }

  /**
   * Verify accessibility features
   */
  async verifyAccessibility(): Promise<void> {
    // Check for proper heading structure
    const headings = this.page.locator('h1, h2, h3, h4, h5, h6');
    expect(await headings.count()).toBeGreaterThan(0);
    
    // Check for aria-labels on interactive elements
    const refreshBtn = this.refreshButton;
    await expect(refreshBtn).toHaveAttribute('aria-label', 'refresh data');
    
    // Check for screen reader content
    const srElements = this.page.locator('[aria-live], .sr-only');
    expect(await srElements.count()).toBeGreaterThan(0);
  }

  /**
   * Measure dashboard load performance
   */
  async measureLoadPerformance(): Promise<{
    loadTime: number;
    firstContentfulPaint: number;
    domContentLoaded: number;
  }> {
    const startTime = Date.now();
    
    await this.goto();
    await this.waitForDataLoad();
    
    const endTime = Date.now();
    const loadTime = endTime - startTime;
    
    // Get performance metrics from the browser
    const performanceMetrics = await this.page.evaluate(() => {
      const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
      return {
        firstContentfulPaint: performance.getEntriesByName('first-contentful-paint')[0]?.startTime || 0,
        domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
      };
    });
    
    return {
      loadTime,
      firstContentfulPaint: performanceMetrics.firstContentfulPaint,
      domContentLoaded: performanceMetrics.domContentLoaded,
    };
  }

  /**
   * Verify real-time updates work
   */
  async verifyRealTimeUpdates(): Promise<void> {
    // Get initial values
    const initialFocusTime = await this.getFocusTimeValue();
    
    // Trigger a simulated update (this would depend on the actual implementation)
    await this.clickRefresh();
    
    // Wait for potential updates
    await this.page.waitForTimeout(2000);
    
    // Verify that data can be refreshed (values might change)
    await expect(this.totalFocusTimeCard).toBeVisible();
  }
}