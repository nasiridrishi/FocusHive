# Analytics E2E Tests

Comprehensive end-to-end tests for the Analytics and Productivity Tracking features (UOL-309) in FocusHive.

## Overview

These tests cover all aspects of the analytics system as specified in the Linear task UOL-309:

1. **Data Collection & Tracking** - Focus session times, break monitoring, activity patterns, productivity scores
2. **Analytics Dashboard** - Real-time metrics, historical data visualization, date filtering
3. **Reporting Features** - Daily/weekly/monthly reports, comparative analytics, trend analysis
4. **Data Visualization** - Interactive charts, real-time updates, mobile-responsive displays
5. **Performance Metrics** - Focus vs break ratios, productivity streaks, peak performance times
6. **Export Functionality** - CSV, JSON, PDF, PNG export formats
7. **Responsive Design** - Mobile, tablet, desktop layouts
8. **Accessibility** - WCAG compliance, keyboard navigation, screen readers

## Test Structure

### Files

- **`analytics.spec.ts`** - Main test suite with comprehensive scenarios
- **`../pages/AnalyticsPage.ts`** - Page Object Model for analytics interactions
- **`../helpers/analytics.helper.ts`** - Helper functions for mocking and utilities

### Test Categories

1. **Dashboard Loading and Basic Functionality**
   - Page loading performance (must load within 3 seconds)
   - Loading states and error handling
   - Empty state handling
   - Network failure recovery

2. **Data Collection & Tracking**
   - Productivity metrics accuracy
   - Focus session time tracking
   - Streak calculations
   - Productivity score computation

3. **Data Visualization Components**
   - Chart rendering and interactions
   - Hive activity heatmaps
   - Member engagement displays
   - Goal progress indicators
   - Tooltip functionality

4. **Filtering and Time Range Selection**
   - Time range filters (day, week, month, year, custom)
   - Data updates when filters change
   - Custom date range selection

5. **Export Functionality**
   - CSV, JSON, PDF, PNG export formats
   - Download handling
   - Export error recovery

6. **Real-time Updates and Performance**
   - Data refresh functionality
   - Performance with large datasets
   - Concurrent update handling

7. **Responsive Design and Accessibility**
   - Mobile, tablet, desktop layouts
   - Accessibility standards compliance
   - Keyboard navigation
   - Color contrast and visual indicators

8. **Integration and Error Handling**
   - Authentication integration
   - Malformed API response handling
   - Partial data handling
   - Network recovery

## Key Features Tested

### Performance Requirements
- Dashboard loads within 3 seconds
- API responses under 2 seconds
- Chart rendering under 1.5 seconds
- First Contentful Paint under 1.5 seconds
- Largest Contentful Paint under 2.5 seconds

### Data Mocking
The tests use comprehensive mock data generation:
- Realistic productivity metrics
- 30-day trend data
- 365-day heatmap data
- Configurable member counts and goals
- Empty state and error condition mocking

### Accessibility Testing
- ARIA labels and descriptions
- Keyboard navigation support
- Screen reader compatibility
- High contrast compliance
- Focus management

## Running the Tests

### Prerequisites
- Frontend application running on `http://localhost:5173`
- Analytics demo route available at `/analytics/demo`

### Run All Analytics Tests
```bash
npx playwright test e2e/tests/analytics/analytics.spec.ts
```

### Run Specific Test Groups
```bash
# Dashboard loading tests only
npx playwright test e2e/tests/analytics/analytics.spec.ts --grep "Dashboard Loading"

# Data visualization tests only
npx playwright test e2e/tests/analytics/analytics.spec.ts --grep "Data Visualization"

# Performance tests only
npx playwright test e2e/tests/analytics/analytics.spec.ts --grep "Performance"

# Accessibility tests only
npx playwright test e2e/tests/analytics/analytics.spec.ts --grep "Accessibility"
```

### Run with Different Browsers
```bash
# Chrome
npx playwright test e2e/tests/analytics/analytics.spec.ts --project=chromium

# Firefox
npx playwright test e2e/tests/analytics/analytics.spec.ts --project=firefox

# Safari
npx playwright test e2e/tests/analytics/analytics.spec.ts --project=webkit
```

### Debug Mode
```bash
# Run in headed mode with slow motion
npx playwright test e2e/tests/analytics/analytics.spec.ts --headed --slow-mo=1000

# Debug specific test
npx playwright test e2e/tests/analytics/analytics.spec.ts --grep "should load analytics dashboard" --debug
```

## Current Implementation Status

### ✅ Implemented Tests
- Dashboard loading and error handling
- Performance measurement
- Data visualization checks
- Export functionality testing
- Responsive design validation
- Accessibility compliance
- Mock data generation and API mocking

### ⚠️ Implementation Notes
Since the `/analytics` route is not yet implemented in the application:
- Tests target the `/analytics/demo` route which contains the analytics dashboard
- API endpoints are mocked to provide realistic test scenarios
- Tests are designed to work when the actual analytics route is implemented

### 🔄 When Analytics Route is Implemented
When the actual `/analytics` route is available:
1. Update `analyticsPage.goto()` calls from `/analytics/demo` to `/analytics`
2. Replace API mocks with actual backend integration tests
3. Add authentication flow tests for protected routes
4. Include real-time WebSocket update testing

## Test Data

### Mock Analytics Data Structure
```typescript
{
  productivity: {
    totalFocusTime: number;      // minutes
    completedSessions: number;
    completionRate: number;      // 0-1
    streak: { current: number; best: number };
    productivity: { average: number; trend: 'up'|'down'|'stable' };
  },
  taskCompletion: { total, completed, pending, rate },
  trends: { focusTime: Array<{date, value}> },
  hiveActivity: Array<{date, value}>,
  memberEngagement: Array<{userId, name, focusTime, sessions}>,
  goalProgress: Array<{id, title, progress, target}>
}
```

### Performance Thresholds
- Dashboard Load Time: 3000ms
- API Response Time: 2000ms  
- Chart Render Time: 1500ms
- First Contentful Paint: 1500ms
- Largest Contentful Paint: 2500ms

## Troubleshooting

### Common Issues

1. **Tests fail with route not found**
   - Ensure frontend is running on correct port (5173)
   - Check that analytics demo route is available

2. **Download tests fail**
   - Verify test-downloads directory exists
   - Check file permissions for download location

3. **Performance tests fail**
   - Run tests on consistent hardware
   - Close other applications during testing
   - Check network latency if using remote services

4. **Chart interaction tests fail**
   - Verify chart libraries are loaded
   - Check for JavaScript errors in browser console
   - Ensure SVG/Canvas elements are present

### Debugging Tips

1. **Use browser developer tools**
   ```bash
   npx playwright test --debug
   ```

2. **Take screenshots on failure**
   ```bash
   npx playwright test --screenshot=only-on-failure
   ```

3. **Record test execution**
   ```bash
   npx playwright test --video=retain-on-failure
   ```

4. **Generate HTML report**
   ```bash
   npx playwright test
   npx playwright show-report
   ```

## Contributing

When adding new analytics tests:

1. Follow the existing Page Object Model pattern
2. Add new selectors to `AnalyticsPage.ts`
3. Create helper functions in `analytics.helper.ts` for reusable logic
4. Include both positive and negative test scenarios
5. Ensure accessibility compliance in new features
6. Add performance assertions for new components
7. Test across different screen sizes and browsers

## Related Documentation

- [Project Analytics Requirements](../../docs/analytics-requirements.md)
- [Performance Testing Guidelines](../../docs/performance-testing.md)
- [Accessibility Testing Checklist](../../docs/accessibility-checklist.md)
- [Linear Task UOL-309](https://linear.app/focushive/issue/UOL-309)