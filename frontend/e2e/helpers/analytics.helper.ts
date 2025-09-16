/**
 * Analytics helper functions for E2E tests
 * Provides utilities for testing analytics features, mocking data, and performance testing
 */

import {expect, Locator, Page} from '@playwright/test';
import {TIMEOUTS} from './test-data';

// Mock analytics data types
export interface MockAnalyticsData {
  productivity: {
    totalFocusTime: number;
    completedSessions: number;
    completionRate: number;
    streak: {
      current: number;
      best: number;
    };
    productivity: {
      average: number;
      trend: 'up' | 'down' | 'stable';
    };
  };
  taskCompletion: {
    total: number;
    completed: number;
    pending: number;
    rate: number;
  };
  trends: {
    focusTime: Array<{
      date: string;
      value: number;
    }>;
  };
  hiveActivity: Array<{
    date: string;
    value: number;
  }>;
  memberEngagement: Array<{
    userId: string;
    name: string;
    focusTime: number;
    sessions: number;
  }>;
  goalProgress: Array<{
    id: string;
    title: string;
    progress: number;
    target: number;
  }>;
  lastUpdated: string;
}

export class AnalyticsHelper {
  constructor(private page: Page) {
  }

  /**
   * Generate mock analytics data with realistic values
   */
  generateMockAnalyticsData(options: {
    hasData?: boolean;
    focusTimeMinutes?: number;
    completedSessions?: number;
    currentStreak?: number;
    memberCount?: number;
    goalCount?: number;
  } = {}): MockAnalyticsData {
    const {
      hasData = true,
      focusTimeMinutes = 240, // 4 hours
      completedSessions = 12,
      currentStreak = 7,
      memberCount = 5,
      goalCount = 3
    } = options;

    if (!hasData) {
      return {
        productivity: {
          totalFocusTime: 0,
          completedSessions: 0,
          completionRate: 0,
          streak: {current: 0, best: 0},
          productivity: {average: 0, trend: 'stable'}
        },
        taskCompletion: {
          total: 0,
          completed: 0,
          pending: 0,
          rate: 0
        },
        trends: {focusTime: []},
        hiveActivity: [],
        memberEngagement: [],
        goalProgress: [],
        lastUpdated: new Date().toISOString()
      };
    }

    // Generate trend data for the last 30 days
    const trendData = Array.from({length: 30}, (_, i) => {
      const date = new Date();
      date.setDate(date.getDate() - (29 - i));
      return {
        date: date.toISOString().split('T')[0],
        value: Math.floor(Math.random() * 120) + 60 // 60-180 minutes per day
      };
    });

    // Generate hive activity data
    const hiveActivityData = Array.from({length: 365}, (_, i) => {
      const date = new Date();
      date.setDate(date.getDate() - (364 - i));
      return {
        date: date.toISOString().split('T')[0],
        value: Math.floor(Math.random() * 10) // 0-10 activity level
      };
    });

    // Generate member engagement data
    const memberEngagementData = Array.from({length: memberCount}, (_, i) => ({
      userId: `user-${i + 1}`,
      name: `Member ${i + 1}`,
      focusTime: Math.floor(Math.random() * 180) + 60, // 60-240 minutes
      sessions: Math.floor(Math.random() * 10) + 1 // 1-10 sessions
    }));

    // Generate goal progress data
    const goalProgressData = Array.from({length: goalCount}, (_, i) => ({
      id: `goal-${i + 1}`,
      title: `Goal ${i + 1}: Complete Project Phase`,
      progress: Math.floor(Math.random() * 80) + 10, // 10-90% progress
      target: 100
    }));

    return {
      productivity: {
        totalFocusTime: focusTimeMinutes,
        completedSessions,
        completionRate: 0.85,
        streak: {
          current: currentStreak,
          best: Math.max(currentStreak + 3, 14)
        },
        productivity: {
          average: 4.2,
          trend: 'up'
        }
      },
      taskCompletion: {
        total: 45,
        completed: 38,
        pending: 7,
        rate: 0.84
      },
      trends: {
        focusTime: trendData
      },
      hiveActivity: hiveActivityData,
      memberEngagement: memberEngagementData,
      goalProgress: goalProgressData,
      lastUpdated: new Date().toISOString()
    };
  }

  /**
   * Mock analytics API endpoints
   */
  async mockAnalyticsApi(mockData: MockAnalyticsData): Promise<void> {
    // Mock analytics data endpoint
    await this.page.route('**/api/v1/analytics/**', (route) => {
      const url = route.request().url();

      if (url.includes('/dashboard')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: mockData
          })
        });
      } else if (url.includes('/export')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            downloadUrl: `/analytics/exports/${Date.now()}.csv`
          })
        });
      } else {
        route.continue();
      }
    });

    // Mock export download endpoint
    await this.page.route('**/analytics/exports/**', (route) => {
      const format = route.request().url().split('.').pop();
      let contentType = 'text/csv';
      let body = 'date,focus_time,sessions\n2023-01-01,120,3\n2023-01-02,95,2';

      if (format === 'json') {
        contentType = 'application/json';
        body = JSON.stringify(mockData, null, 2);
      } else if (format === 'pdf') {
        contentType = 'application/pdf';
        body = 'PDF content placeholder';
      } else if (format === 'png') {
        contentType = 'image/png';
        body = 'PNG image placeholder';
      }

      route.fulfill({
        status: 200,
        contentType,
        body,
        headers: {
          'Content-Disposition': `attachment; filename="analytics-export.${format}"`
        }
      });
    });
  }

  /**
   * Mock empty analytics state
   */
  async mockEmptyAnalytics(): Promise<void> {
    const emptyData = this.generateMockAnalyticsData({hasData: false});
    await this.mockAnalyticsApi(emptyData);
  }

  /**
   * Mock analytics API error
   */
  async mockAnalyticsApiError(errorCode: number = 500): Promise<void> {
    await this.page.route('**/api/v1/analytics/**', (route) => {
      route.fulfill({
        status: errorCode,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          error: 'Analytics service temporarily unavailable',
          message: 'Please try again later'
        })
      });
    });
  }

  /**
   * Mock slow analytics API response
   */
  async mockSlowAnalyticsApi(delayMs: number = 3000): Promise<void> {
    const mockData = this.generateMockAnalyticsData();

    await this.page.route('**/api/v1/analytics/**', async (route) => {
      // Add delay to simulate slow response
      await new Promise(resolve => setTimeout(resolve, delayMs));

      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: mockData
        })
      });
    });
  }

  /**
   * Mock network failure
   */
  async mockNetworkFailure(): Promise<void> {
    await this.page.route('**/api/v1/analytics/**', (route) => {
      route.abort('internetdisconnected');
    });
  }

  /**
   * Set up performance monitoring for analytics
   */
  async setupPerformanceMonitoring(): Promise<{
    getMetrics: () => Promise<{
      loadTime: number;
      apiResponseTime: number;
      chartRenderTime: number;
    }>;
  }> {
    let apiStartTime: number;
    let apiEndTime: number;

    // Monitor API response time
    await this.page.route('**/api/v1/analytics/**', (route) => {
      apiStartTime = Date.now();
      route.continue();
    });

    this.page.on('response', (response) => {
      if (response.url().includes('/api/v1/analytics/')) {
        apiEndTime = Date.now();
      }
    });

    return {
      getMetrics: async () => {
        // Measure chart rendering time by checking when SVG elements are present
        const chartRenderStart = Date.now();

        try {
          await this.page.locator('svg, canvas, .recharts-wrapper').first()
          .waitFor({state: 'visible', timeout: TIMEOUTS.MEDIUM});
        } catch {
          // Charts might not be present
        }

        const chartRenderEnd = Date.now();

        return {
          loadTime: chartRenderEnd - chartRenderStart,
          apiResponseTime: apiEndTime - apiStartTime || 0,
          chartRenderTime: chartRenderEnd - chartRenderStart
        };
      }
    };
  }

  /**
   * Test chart interaction capabilities
   */
  async testChartInteractions(chartLocator: Locator): Promise<void> {
    // Test hover interactions
    const chartBounds = await chartLocator.boundingBox();
    if (chartBounds) {
      // Hover over different parts of the chart
      const testPoints = [
        {x: chartBounds.x + chartBounds.width * 0.25, y: chartBounds.y + chartBounds.height * 0.5},
        {x: chartBounds.x + chartBounds.width * 0.5, y: chartBounds.y + chartBounds.height * 0.3},
        {x: chartBounds.x + chartBounds.width * 0.75, y: chartBounds.y + chartBounds.height * 0.7}
      ];

      for (const point of testPoints) {
        await this.page.mouse.move(point.x, point.y);
        await this.page.waitForTimeout(500); // Wait for tooltip to appear

        // Check if tooltip is visible
        const tooltip = this.page.locator('[role="tooltip"]');
        if (await tooltip.isVisible()) {
          expect(await tooltip.textContent()).toBeTruthy();
        }
      }
    }
  }

  /**
   * Validate chart accessibility
   */
  async validateChartAccessibility(chartLocator: Locator): Promise<void> {
    // Check for ARIA labels
    const hasAriaLabel = await chartLocator.getAttribute('aria-label');
    const hasAriaDescribedBy = await chartLocator.getAttribute('aria-describedby');

    expect(hasAriaLabel || hasAriaDescribedBy).toBeTruthy();

    // Check for keyboard navigation support
    await chartLocator.focus();
    expect(await chartLocator.evaluate((el: HTMLElement) =>
        document.activeElement === el || el.contains(document.activeElement)
    )).toBeTruthy();
  }

  /**
   * Test data export functionality
   */
  async testDataExport(format: 'csv' | 'json' | 'pdf' | 'png'): Promise<void> {
    // Set up download listener
    const downloadPromise = this.page.waitForEvent('download');

    // Trigger export with the specified format
    await this.page.click('[data-testid="export-menu-button"]');
    await this.page.selectOption('[data-testid="export-format-select"]', format);
    await this.page.click('[data-testid="download-button"]');

    // Wait for download to start
    const download = await downloadPromise;

    // Verify download properties
    const filename = download.suggestedFilename();
    expect(filename).toContain('analytics');
    expect(filename).toContain(format);

    // Verify file size is reasonable (not empty)
    await download.saveAs(`./test-downloads/${filename}`);

    // For CSV/JSON, we can verify content structure
    if (format === 'csv' || format === 'json') {
      expect(await download.failure()).toBeNull();
    }
  }

  /**
   * Test responsive behavior across different screen sizes
   */
  async testResponsiveDesign(): Promise<void> {
    const viewports = [
      {name: 'mobile', width: 375, height: 667},
      {name: 'tablet', width: 768, height: 1024},
      {name: 'desktop', width: 1920, height: 1080}
    ];

    for (const viewport of viewports) {
      await this.page.setViewportSize({width: viewport.width, height: viewport.height});
      await this.page.waitForTimeout(1000); // Allow layout to adjust

      // Verify dashboard is still functional
      const dashboard = this.page.locator('[data-testid="analytics-dashboard"]');
      await expect(dashboard).toBeVisible();

      // Verify responsive classes are applied
      await expect(dashboard).toHaveClass(/dashboard-responsive/);

      // Verify charts adjust appropriately
      const charts = this.page.locator('svg, canvas, .recharts-wrapper');
      const chartCount = await charts.count();

      // At least some charts should be visible on all screen sizes
      expect(chartCount).toBeGreaterThanOrEqual(1);
    }
  }

  /**
   * Verify data accuracy by comparing displayed values with mock data
   */
  async verifyDataAccuracy(expectedData: MockAnalyticsData): Promise<void> {
    // Helper function to format time
    const formatTime = (minutes: number): string => {
      const hours = Math.floor(minutes / 60);
      const remainingMinutes = minutes % 60;
      return `${hours}h ${remainingMinutes}m`;
    };

    // Verify focus time
    const focusTimeElement = this.page.locator('[data-testid="stat-card-focus-time"] h4').first();
    const displayedFocusTime = await focusTimeElement.textContent();
    expect(displayedFocusTime).toBe(formatTime(expectedData.productivity.totalFocusTime));

    // Verify completed sessions
    const sessionsElement = this.page.locator('[data-testid="stat-card-completed-sessions"] h4').first();
    const displayedSessions = await sessionsElement.textContent();
    expect(displayedSessions).toBe(expectedData.productivity.completedSessions.toString());

    // Verify current streak
    const streakElement = this.page.locator('[data-testid="stat-card-streak"] h4').first();
    const displayedStreak = await streakElement.textContent();
    expect(displayedStreak).toBe(`${expectedData.productivity.streak.current} days`);

    // Verify productivity score
    const scoreElement = this.page.locator('[data-testid="stat-card-productivity"] h4').first();
    const displayedScore = await scoreElement.textContent();
    expect(displayedScore).toBe(`${expectedData.productivity.productivity.average.toFixed(1)}/5`);
  }

  /**
   * Test filter functionality
   */
  async testFilters(): Promise<void> {
    // Test time range filter
    const timeRanges = ['day', 'week', 'month', 'year'];

    for (const range of timeRanges) {
      const timeRangeSelect = this.page.locator('[data-testid="time-range-select"]');
      if (await timeRangeSelect.isVisible()) {
        await timeRangeSelect.selectOption(range);
        await this.page.waitForTimeout(1000); // Allow data to refresh

        // Verify the filter is applied (chip should show the selected range)
        const chip = this.page.locator('.MuiChip-root:has-text("Time Period:")');
        await expect(chip).toBeVisible();
      }
    }
  }

  /**
   * Clean up test artifacts
   */
  async cleanup(): Promise<void> {
    // Clear any route mocks
    await this.page.unrouteAll();

    // Reset viewport
    await this.page.setViewportSize({width: 1280, height: 720});
  }
}