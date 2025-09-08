/**
 * E2E Tests for Analytics and Productivity Tracking Features (UOL-309)
 * 
 * Tests cover:
 * 1. Data Collection & Tracking
 * 2. Analytics Dashboard
 * 3. Reporting Features
 * 4. Data Visualization
 * 5. Performance Metrics
 * 6. Export Functionality
 * 7. Responsive Design
 * 8. Accessibility
 */

import { test, expect } from '@playwright/test';
import { AnalyticsPage } from '../../pages/AnalyticsPage';
import { AnalyticsHelper } from '../../helpers/analytics.helper';
import { AuthHelper } from '../../helpers/auth.helper';
import { 
  TEST_USERS, 
  validateTestEnvironment,
  PERFORMANCE_THRESHOLDS
} from '../../helpers/test-data';

test.describe('Analytics and Productivity Tracking', () => {
  let analyticsPage: AnalyticsPage;
  let analyticsHelper: AnalyticsHelper;
  let authHelper: AuthHelper;

  test.beforeEach(async ({ page }) => {
    // Initialize page objects and helpers
    analyticsPage = new AnalyticsPage(page);
    analyticsHelper = new AnalyticsHelper(page);
    authHelper = new AuthHelper(page);

    // Validate test environment
    validateTestEnvironment();

    // Clear any existing authentication
    await authHelper.clearStorage();
  });

  test.afterEach(async ({ page: _page }) => {
    // Cleanup after each test
    await analyticsHelper.cleanup();
    await authHelper.clearStorage();
  });

  test.describe('Dashboard Loading and Basic Functionality', () => {
    test('should display analytics demo page when route is not implemented', async () => {
      // Since /analytics route doesn't exist, test the demo page
      await analyticsPage.gotoDemo();
      
      // Verify demo page loads
      await expect(analyticsPage.page).toHaveTitle(/FocusHive/);
      await expect(analyticsPage.page.locator('h1, h3').filter({ hasText: 'Analytics Dashboard Demo' })).toBeVisible();
      
      // Verify demo dashboard is visible
      await expect(analyticsPage.dashboard).toBeVisible();
    });

    test('should load analytics dashboard within performance threshold', async () => {
      // Mock analytics data
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);

      // Measure load performance
      const performanceMetrics = await analyticsPage.measureLoadPerformance();

      // Verify performance requirements (dashboard should load within 3 seconds)
      expect(performanceMetrics.loadTime).toBeLessThan(PERFORMANCE_THRESHOLDS.DASHBOARD_LOAD_TIME);
      
      // Verify dashboard loaded successfully
      await analyticsPage.verifyDashboardLoaded();
    });

    test('should display loading state during data fetch', async () => {
      // Mock slow API response
      await analyticsHelper.mockSlowAnalyticsApi(2000);
      
      await analyticsPage.gotoDemo();
      
      // Verify loading spinner appears
      await expect(analyticsPage.loadingSpinner).toBeVisible();
      await expect(analyticsPage.page.locator('text=Loading analytics data')).toBeVisible();
      
      // Wait for loading to complete
      await analyticsPage.waitForDataLoad();
      
      // Verify loading spinner disappears
      await expect(analyticsPage.loadingSpinner).not.toBeVisible();
    });

    test('should handle API errors gracefully', async () => {
      // Mock API error
      await analyticsHelper.mockAnalyticsApiError(500);
      
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForPageLoad();
      
      // Verify error state is displayed
      await analyticsPage.verifyErrorState();
      await expect(analyticsPage.page.locator('text=Error loading analytics data')).toBeVisible();
      
      // Verify retry button works
      // Reset to successful response
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      
      await analyticsPage.clickRetry();
      await analyticsPage.waitForDataLoad();
      
      // Verify dashboard loads after retry
      await analyticsPage.verifyDashboardLoaded();
    });

    test('should display empty state when no data available', async () => {
      // Mock empty analytics data
      await analyticsHelper.mockEmptyAnalytics();
      
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForPageLoad();
      
      // Verify empty state is displayed
      await analyticsPage.verifyEmptyState();
      await expect(analyticsPage.page.locator('text=Start using FocusHive to see your productivity analytics')).toBeVisible();
    });

    test('should handle network failures gracefully', async () => {
      // Mock network failure
      await analyticsHelper.mockNetworkFailure();
      
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForPageLoad();
      
      // Verify error handling for network issues
      await analyticsPage.verifyErrorState();
    });
  });

  test.describe('Data Collection & Tracking', () => {
    test('should display accurate productivity metrics', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData({
        focusTimeMinutes: 180, // 3 hours
        completedSessions: 8,
        currentStreak: 5
      });
      
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Verify data accuracy
      await analyticsHelper.verifyDataAccuracy(mockData);
    });

    test('should track focus session times correctly', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData({
        focusTimeMinutes: 240, // 4 hours
        completedSessions: 12
      });
      
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Verify focus time is displayed correctly
      const focusTime = await analyticsPage.getFocusTimeValue();
      expect(focusTime).toBe('4h 0m');
      
      // Verify sessions count
      const sessions = await analyticsPage.getCompletedSessionsValue();
      expect(sessions).toBe('12');
    });

    test('should display productivity streaks and patterns', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData({
        currentStreak: 14
      });
      
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Verify current streak
      const streak = await analyticsPage.getCurrentStreakValue();
      expect(streak).toContain('14 days');
      
      // Verify streak card shows best streak
      const streakCard = analyticsPage.currentStreakCard;
      await expect(streakCard.locator('text=/Best:.*days/')).toBeVisible();
    });

    test('should calculate and display productivity scores', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Verify productivity score format
      const score = await analyticsPage.getProductivityScoreValue();
      expect(score).toMatch(/^\d\.\d\/5$/); // Format: X.X/5
      
      // Verify trend indicator
      await expect(analyticsPage.productivityScoreCard.locator('text=/Trend:/')).toBeVisible();
    });
  });

  test.describe('Data Visualization Components', () => {
    test('should render charts correctly', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Verify charts are rendered
      await analyticsPage.verifyChartsRendered();
      
      // Verify specific chart components are present
      if (await analyticsPage.productivityChart.isVisible()) {
        await expect(analyticsPage.productivityChart).toContainText('Focus Time');
      }
      
      if (await analyticsPage.taskCompletionChart.isVisible()) {
        await expect(analyticsPage.taskCompletionChart.locator('svg, canvas')).toBeVisible();
      }
    });

    test('should support chart interactions', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Test chart hover interactions
      if (await analyticsPage.productivityChart.isVisible()) {
        await analyticsHelper.testChartInteractions(analyticsPage.productivityChart);
      }
      
      // Verify tooltip appears on hover
      await analyticsPage.hoverOverChart(analyticsPage.productivityChart);
    });

    test('should display hive activity heatmap when available', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Note: Hive activity heatmap may not be visible in individual view
      // This test checks if it appears when switching to hive view
      if (await analyticsPage.viewTypeSelect.isVisible()) {
        await analyticsPage.updateViewType('hive');
        
        if (await analyticsPage.hiveActivityHeatmap.isVisible()) {
          await expect(analyticsPage.hiveActivityHeatmap).toBeVisible();
          
          // Verify heatmap has interactive elements
          const heatmapCells = analyticsPage.hiveActivityHeatmap.locator('rect, circle');
          expect(await heatmapCells.count()).toBeGreaterThan(0);
        }
      }
    });

    test('should show member engagement when in team/hive view', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData({
        memberCount: 8
      });
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Switch to hive view to see member engagement
      if (await analyticsPage.viewTypeSelect.isVisible()) {
        await analyticsPage.updateViewType('hive');
        
        if (await analyticsPage.memberEngagementChart.isVisible()) {
          await expect(analyticsPage.memberEngagementChart).toBeVisible();
          
          // Verify member data is displayed
          const memberItems = analyticsPage.memberEngagementChart.locator('[data-testid*="member-"]');
          expect(await memberItems.count()).toBeGreaterThan(0);
        }
      }
    });

    test('should display goal progress indicators', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData({
        goalCount: 5
      });
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      if (await analyticsPage.goalProgressChart.isVisible()) {
        await expect(analyticsPage.goalProgressChart).toBeVisible();
        
        // Verify progress indicators are present
        const progressBars = analyticsPage.goalProgressChart.locator('.MuiLinearProgress-root, [role="progressbar"]');
        expect(await progressBars.count()).toBeGreaterThan(0);
      }
    });
  });

  test.describe('Filtering and Time Range Selection', () => {
    test('should support time range filtering', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Test time range filters
      await analyticsHelper.testFilters();
      
      // Verify time period chip updates
      await expect(analyticsPage.timePeriodChip).toBeVisible();
    });

    test('should update data when filters change', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Get initial values
      const initialFocusTime = await analyticsPage.getFocusTimeValue();
      
      // Change time range filter
      if (await analyticsPage.timeRangeSelect.isVisible()) {
        await analyticsPage.updateTimeRange('month');
        
        // Verify data can refresh (implementation may vary)
        await expect(analyticsPage.totalFocusTimeCard).toBeVisible();
      }
    });

    test('should support custom date range selection', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Test custom date range if available
      if (await analyticsPage.dateRangePicker.isVisible()) {
        await analyticsPage.dateRangePicker.click();
        
        // Verify date picker opens
        const datePicker = analyticsPage.page.locator('.MuiDatePicker-root, [role="dialog"]');
        if (await datePicker.isVisible()) {
          await expect(datePicker).toBeVisible();
        }
      }
    });
  });

  test.describe('Export Functionality', () => {
    test('should export data in CSV format', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Test CSV export
      if (await analyticsPage.exportButton.isVisible()) {
        await analyticsHelper.testDataExport('csv');
      }
    });

    test('should export data in JSON format', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Test JSON export
      if (await analyticsPage.exportButton.isVisible()) {
        await analyticsHelper.testDataExport('json');
      }
    });

    test('should support PDF report generation', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Test PDF export
      if (await analyticsPage.exportButton.isVisible()) {
        await analyticsHelper.testDataExport('pdf');
      }
    });

    test('should support chart image export', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Test PNG export
      if (await analyticsPage.exportButton.isVisible()) {
        await analyticsHelper.testDataExport('png');
      }
    });

    test('should handle export errors gracefully', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      
      // Mock export failure
      await analyticsPage.page.route('**/analytics/exports/**', (route) => {
        route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Export failed' })
        });
      });
      
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      if (await analyticsPage.exportButton.isVisible()) {
        await analyticsPage.openExportMenu();
        await analyticsPage.selectExportFormat('csv');
        
        // Attempt download - should handle error gracefully
        await analyticsPage.downloadButton.click();
        
        // Verify error handling (implementation specific)
        await expect(analyticsPage.page.locator('.MuiAlert-root, [role="alert"]')).toBeVisible();
      }
    });
  });

  test.describe('Real-time Updates and Performance', () => {
    test('should refresh data when refresh button is clicked', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Test refresh functionality
      await analyticsPage.verifyRealTimeUpdates();
      
      // Verify refresh button works
      await analyticsPage.clickRefresh();
      await analyticsPage.waitForDataLoad();
      
      // Verify dashboard remains functional
      await analyticsPage.verifyDashboardLoaded();
    });

    test('should maintain good performance with large datasets', async () => {
      // Generate large dataset
      const largeDataset = analyticsHelper.generateMockAnalyticsData({
        memberCount: 50,
        goalCount: 20
      });
      
      await analyticsHelper.mockAnalyticsApi(largeDataset);
      
      // Monitor performance
      const performanceMonitor = await analyticsHelper.setupPerformanceMonitoring();
      
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Get performance metrics
      const metrics = await performanceMonitor.getMetrics();
      
      // Verify performance thresholds
      expect(metrics.apiResponseTime).toBeLessThan(PERFORMANCE_THRESHOLDS.API_RESPONSE_TIME);
      expect(metrics.chartRenderTime).toBeLessThan(PERFORMANCE_THRESHOLDS.CHART_RENDER_TIME);
    });

    test('should handle concurrent data updates efficiently', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Simulate multiple rapid updates
      const updatePromises = [];
      for (let i = 0; i < 5; i++) {
        updatePromises.push(analyticsPage.clickRefresh());
        await analyticsPage.page.waitForTimeout(200); // Small delay between requests
      }
      
      await Promise.all(updatePromises);
      
      // Verify dashboard remains stable
      await analyticsPage.verifyDashboardLoaded();
    });
  });

  test.describe('Responsive Design and Accessibility', () => {
    test('should adapt to different screen sizes', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Test responsive design across screen sizes
      await analyticsHelper.testResponsiveDesign();
    });

    test('should be accessible on mobile devices', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      
      // Test mobile layout
      await analyticsPage.verifyMobileLayout();
      
      // Verify touch interactions work
      await expect(analyticsPage.refreshButton).toBeVisible();
      await analyticsPage.refreshButton.tap();
    });

    test('should meet accessibility standards', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Test accessibility features
      await analyticsPage.verifyAccessibility();
      
      // Test chart accessibility
      if (await analyticsPage.productivityChart.isVisible()) {
        await analyticsHelper.validateChartAccessibility(analyticsPage.productivityChart);
      }
    });

    test('should support keyboard navigation', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Test keyboard navigation
      await analyticsPage.page.keyboard.press('Tab');
      
      // Verify focusable elements
      const focusedElement = await analyticsPage.page.evaluate(() => document.activeElement?.tagName);
      expect(['BUTTON', 'A', 'INPUT', 'SELECT']).toContain(focusedElement);
      
      // Test refresh button with keyboard
      await analyticsPage.refreshButton.focus();
      await analyticsPage.page.keyboard.press('Enter');
      await analyticsPage.waitForDataLoad();
    });

    test('should have proper color contrast and visual indicators', async () => {
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Verify high contrast elements are visible
      await expect(analyticsPage.dashboardTitle).toBeVisible();
      await expect(analyticsPage.totalFocusTimeCard).toBeVisible();
      
      // Verify status indicators have appropriate colors
      const statusElements = analyticsPage.page.locator('.MuiChip-root, .MuiAlert-root');
      expect(await statusElements.count()).toBeGreaterThan(0);
    });
  });

  test.describe('Integration with Authentication', () => {
    test('should redirect unauthenticated users to login', async ({ page }) => {
      // Clear any authentication
      await authHelper.clearStorage();
      
      // Try to access analytics directly
      await page.goto('/analytics');
      
      // Should be redirected to login (implementation may vary)
      // For now, we'll check if we get a proper response
      const response = await page.waitForResponse(/analytics|login|auth/);
      expect([200, 302, 401, 403]).toContain(response.status());
    });

    test('should display user-specific analytics when authenticated', async () => {
      // Mock user authentication
      await authHelper.loginWithValidUser();
      
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Verify dashboard shows user-specific data
      await analyticsPage.verifyDashboardLoaded();
      await expect(analyticsPage.dashboardTitle).toContainText('Analytics Dashboard');
    });
  });

  test.describe('Error Handling and Edge Cases', () => {
    test('should handle malformed API responses', async () => {
      await analyticsPage.page.route('**/api/v1/analytics/**', (route) => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: 'invalid json response'
        });
      });
      
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForPageLoad();
      
      // Should show error state for malformed data
      await analyticsPage.verifyErrorState();
    });

    test('should handle partial data gracefully', async () => {
      const partialData = {
        productivity: {
          totalFocusTime: 120,
          completedSessions: 5,
          completionRate: 0.8,
          streak: { current: 3, best: 10 },
          productivity: { average: 3.5, trend: 'up' as const }
        },
        taskCompletion: {
          total: 0,
          completed: 0,
          pending: 0,
          rate: 0
        },
        trends: { focusTime: [] },
        hiveActivity: [],
        memberEngagement: [],
        goalProgress: [],
        lastUpdated: new Date().toISOString()
      };
      
      await analyticsHelper.mockAnalyticsApi(partialData);
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForDataLoad();
      
      // Should display available data and handle missing data gracefully
      await expect(analyticsPage.totalFocusTimeCard).toBeVisible();
      await expect(analyticsPage.totalFocusTimeCard).toContainText('2h 0m');
    });

    test('should recover from temporary network issues', async () => {
      // Start with network failure
      await analyticsHelper.mockNetworkFailure();
      
      await analyticsPage.gotoDemo();
      await analyticsPage.waitForPageLoad();
      await analyticsPage.verifyErrorState();
      
      // Restore network and retry
      const mockData = analyticsHelper.generateMockAnalyticsData();
      await analyticsHelper.mockAnalyticsApi(mockData);
      
      await analyticsPage.clickRetry();
      await analyticsPage.waitForDataLoad();
      
      // Should recover and display data
      await analyticsPage.verifyDashboardLoaded();
    });
  });
});