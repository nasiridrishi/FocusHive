# Mobile Responsiveness E2E Testing Suite

Comprehensive end-to-end testing suite for mobile responsiveness, touch interactions, and mobile-specific features in the FocusHive application.

## Overview

This testing suite covers all critical aspects of mobile user experience including:

- **Device Viewport Testing** - Testing across various mobile devices and screen sizes
- **Touch Interactions** - Tap, swipe, pinch, long press, and multi-touch gestures  
- **Responsive Layout** - Grid systems, component stacking, and layout adaptation
- **Mobile-Specific Features** - Virtual keyboard, orientation changes, safe area insets
- **Performance on Mobile** - Load times, scroll performance, bundle optimization
- **Form Usability** - Mobile keyboards, input types, validation visibility
- **Content Adaptation** - Text scaling, media responsiveness, content prioritization
- **Mobile Navigation** - Hamburger menus, bottom navigation, accessibility
- **Media Queries** - Breakpoint behavior, print styles, high DPI support
- **PWA Features** - Add to home screen, offline mode, app-like functionality

## Test Structure

```
frontend/e2e/tests/mobile/
├── README.md                          # This documentation
├── mobile-responsiveness.spec.ts      # Main test suite (40+ tests)
├── ../../pages/MobilePage.ts         # Page Object Model
└── ../../helpers/mobile.helper.ts    # Mobile testing utilities
```

## Device Configurations

The test suite includes device emulation for:

### Mobile Phones
- **iPhone SE** (375×667) - Smallest iOS device
- **iPhone 12** (390×844) - Standard iPhone with notch
- **iPhone 14 Pro** (393×852) - Latest iPhone with Dynamic Island
- **Samsung Galaxy S21** (360×800) - Popular Android device
- **Google Pixel 5** (393×851) - Stock Android experience

### Tablets
- **iPad** (768×1024) - Standard tablet size
- **iPad Pro** (1024×1366) - Large tablet format
- **Galaxy Tab S8** (800×1280) - Android tablet

### Desktop Breakpoints
- **Desktop Small** (1280×720) - Small desktop/laptop
- **Desktop Medium** (1440×900) - Standard desktop
- **Desktop Large** (1920×1080) - Full HD display
- **Ultrawide** (2560×1440) - Wide monitor support

## Running the Tests

### Prerequisites

Ensure you have the following installed:
- Node.js 18+ 
- Playwright browsers installed (`npx playwright install`)
- FocusHive backend services running
- Environment variables configured

### Command Line Options

```bash
# Run all mobile tests
npm run test:e2e -- tests/mobile/

# Run on specific device
npm run test:e2e -- tests/mobile/ --project=iPhone

# Run with headed browser (visible)
npm run test:e2e -- tests/mobile/ --headed

# Run specific test group
npm run test:e2e -- tests/mobile/ --grep "Device Viewport"

# Run with debug mode
npm run test:e2e -- tests/mobile/ --debug

# Generate test report
npm run test:e2e -- tests/mobile/ --reporter=html
```

### Configuration

Mobile-specific Playwright configuration in `playwright.config.ts`:

```typescript
// Device-specific projects
{
  name: 'iPhone 12',
  use: { ...devices['iPhone 12'] }
},
{
  name: 'Galaxy S21', 
  use: { ...devices['Galaxy S21'] }
},
{
  name: 'iPad',
  use: { ...devices['iPad'] }
}
```

## Test Categories

### 1. Device Viewport Testing (6 tests)
- iPhone SE (320px-375px width)
- iPhone 12/14 Pro (390px-393px width) 
- Samsung Galaxy S21 (360px width)
- iPad tablet (768px width)
- Orientation change handling
- Ultra-wide display support (2560px+)

**Key Validations:**
- No horizontal scrolling
- Content fits viewport
- Safe area inset support
- Proper content centering on wide screens

### 2. Touch Interactions (5 tests)
- Touch target size validation (44×44px minimum)
- Touch response time (<100ms)
- Swipe gesture support
- Long press interactions
- Pinch-to-zoom functionality

**Key Validations:**
- Touch targets meet accessibility guidelines
- Gestures work smoothly
- Multi-touch support
- Touch feedback implementation

### 3. Responsive Layout Testing (5 tests)
- Vertical element stacking on mobile
- Navigation menu transformation
- Horizontal scroll prevention
- Table responsiveness
- Modal/overlay adaptation

**Key Validations:**
- Grid systems collapse properly
- Components reflow correctly
- No layout breaking
- Proper component prioritization

### 4. Mobile-Specific Features (4 tests)
- Virtual keyboard handling
- Safe area inset support (notches)
- Pull-to-refresh functionality
- Mobile browser chrome behavior

**Key Validations:**
- Layout doesn't break with virtual keyboard
- Notch areas properly handled
- Viewport meta tag effectiveness
- Status bar styling

### 5. Performance Testing (5 tests)
- 3G network load time testing
- 60fps scroll performance
- Bundle size optimization
- Image lazy loading
- Core Web Vitals optimization

**Key Validations:**
- Load time <3 seconds on 3G
- Smooth scrolling (60fps)
- Bundle size <2MB
- LCP <2.5s, CLS <0.1

### 6. Form Usability (4 tests)
- Mobile keyboard optimization
- Autocomplete/autofill support
- Error message visibility
- Date/time picker usability

**Key Validations:**
- Input types trigger correct keyboards
- Form validation clearly visible
- Touch-friendly form controls
- Proper autocomplete attributes

### 7. Content Adaptation (3 tests)
- Text truncation and overflow
- Media responsiveness
- Content prioritization

**Key Validations:**
- No text overflow issues
- Images scale properly
- Important content visible first
- Media maintains aspect ratios

### 8. PWA Features (3 tests)
- Add to home screen support
- Offline mode handling
- App-like meta tag configuration

**Key Validations:**
- Web app manifest present
- Service worker functionality
- Offline content caching
- App-like appearance

### 9. Accessibility & Dark Mode (3 tests)
- Dark mode support on mobile
- Mobile accessibility features
- Reduced motion preferences

**Key Validations:**
- Sufficient color contrast
- Proper ARIA labels
- Focus management
- Motion preference respect

### 10. Authenticated Features (3 tests)
- Hive creation on mobile
- Timer controls accessibility
- Mobile chat functionality

**Key Validations:**
- Feature usability on small screens
- Touch-friendly interactions
- Proper responsive behavior

## Helper Classes

### MobileHelper

Core mobile testing utilities:

```typescript
const mobileHelper = new MobileHelper(page);

// Device emulation
await mobileHelper.emulateDevice('IPHONE_12');

// Touch interactions
await mobileHelper.performTouchInteraction({
  type: 'swipe',
  startPoint: { x: 300, y: 200 },
  endPoint: { x: 100, y: 200 }
});

// Viewport testing
const result = await mobileHelper.testAtBreakpoint({
  name: 'mobile',
  width: 375,
  height: 667
});

// Performance metrics
const metrics = await mobileHelper.collectMobilePerformanceMetrics();
```

### MobilePage

Page Object Model with mobile-specific locators:

```typescript
const mobilePage = new MobilePage(page);

// Navigation testing
const navResult = await mobilePage.testMobileNavigation();

// Form interaction testing
const formResult = await mobilePage.testMobileFormInteractions();

// Layout testing
const layoutResult = await mobilePage.testCardLayout();

// Media testing
const mediaResult = await mobilePage.testMediaResponsiveness();
```

## Best Practices

### Test Organization
- Group related tests in describe blocks
- Use descriptive test names
- Include viewport size in test descriptions
- Test both portrait and landscape orientations

### Device Selection
- Always test on iPhone SE (smallest screen)
- Include at least one Android device
- Test tablet breakpoints (iPad)
- Include desktop for comparison

### Performance Testing
- Test on simulated slow networks
- Measure real performance metrics
- Check bundle sizes and resource loading
- Validate Core Web Vitals

### Touch Interaction Testing
- Verify minimum touch target sizes
- Test gesture recognition
- Validate touch response times
- Check for proper touch feedback

### Accessibility
- Test with screen readers (when possible)
- Verify keyboard navigation
- Check color contrast ratios
- Validate ARIA labels and roles

## Common Issues and Solutions

### Horizontal Scrolling
```typescript
// Check for elements causing overflow
const overflowingElements = await page.evaluate(() => {
  const elements = Array.from(document.querySelectorAll('*'));
  return elements.filter(el => {
    const rect = el.getBoundingClientRect();
    return rect.right > window.innerWidth;
  });
});
```

### Touch Target Size Issues
```typescript
// Validate touch targets meet minimum size
const smallTargets = await mobileHelper.checkTouchTargetSizes();
expect(smallTargets).toHaveLength(0);
```

### Virtual Keyboard Handling
```typescript
// Test keyboard doesn't break layout
const keyboardResult = await mobileHelper.testVirtualKeyboard();
expect(keyboardResult.layoutShifted).toBeFalsy();
```

### Performance Issues
```typescript
// Monitor Core Web Vitals
const metrics = await page.evaluate(() => ({
  lcp: performance.getEntriesByType('largest-contentful-paint')[0]?.startTime,
  cls: performance.getEntriesByType('layout-shift').reduce((sum, entry) => 
    sum + (!entry.hadRecentInput ? entry.value : 0), 0)
}));
```

## Debugging

### Screenshots for Different Viewports
```typescript
// Take responsive screenshots
const screenshots = await mobilePage.takeResponsiveScreenshots('homepage');
console.log('Screenshots saved:', screenshots);
```

### Device Emulation Verification
```typescript
// Verify device emulation is working
const deviceInfo = await page.evaluate(() => ({
  userAgent: navigator.userAgent,
  viewport: { width: window.innerWidth, height: window.innerHeight },
  touchSupport: 'ontouchstart' in window
}));
```

### Network Condition Testing
```typescript
// Test with slow network
const client = await page.context().newCDPSession(page);
await client.send('Network.emulateNetworkConditions', {
  offline: false,
  downloadThroughput: 1.5 * 1024 * 1024 / 8, // 1.5 Mbps
  uploadThroughput: 750 * 1024 / 8, // 750 Kbps
  latency: 40
});
```

## Environment Variables

Configure these environment variables for mobile testing:

```bash
# Test environment URLs
E2E_BASE_URL=http://127.0.0.1:5173
E2E_API_BASE_URL=http://localhost:8080
E2E_IDENTITY_API_URL=http://localhost:8081

# Test data
E2E_TEST_USER_EMAIL=e2e.test@focushive.com
E2E_TEST_USER_PASSWORD=TestPassword123!

# Performance thresholds
E2E_MOBILE_LOAD_TIME_THRESHOLD=3000
E2E_TOUCH_RESPONSE_THRESHOLD=100
E2E_BUNDLE_SIZE_THRESHOLD=2097152
```

## Contributing

### Adding New Mobile Tests

1. **Device Coverage**: Ensure new tests cover relevant devices
2. **Touch Interactions**: Test with actual touch events, not just clicks
3. **Performance**: Include performance validations where applicable
4. **Accessibility**: Consider accessibility implications
5. **Error Handling**: Test error states on mobile devices

### Test Naming Convention

```typescript
// Good: Descriptive and includes device context
test('should handle form validation errors on iPhone 12', async ({ browser }) => {

// Bad: Too generic
test('should work on mobile', async ({ page }) => {
```

### Pull Request Checklist

- [ ] Tests cover multiple device sizes
- [ ] Touch interactions properly tested  
- [ ] Performance metrics validated
- [ ] Accessibility considerations included
- [ ] Error scenarios tested
- [ ] Documentation updated
- [ ] Screenshots provided for visual changes

## Troubleshooting

### Tests Failing on CI/CD

1. **Timeout Issues**: Increase timeouts for mobile devices
2. **Device Emulation**: Ensure Playwright browsers are installed
3. **Network Conditions**: CI might have different network characteristics
4. **Viewport Issues**: Verify viewport is set correctly before assertions

### Local Development Issues

1. **Service Dependencies**: Ensure all backend services are running
2. **Environment Variables**: Check all required env vars are set
3. **Browser Installation**: Run `npx playwright install` if needed
4. **Port Conflicts**: Verify frontend is running on expected port

### Performance Test Inconsistencies

1. **System Resources**: Close other applications during testing
2. **Network Variability**: Use network emulation for consistent results
3. **Device Capabilities**: Different test machines may have different performance
4. **Cache Effects**: Clear cache between test runs when needed

## Related Documentation

- [Playwright Mobile Testing Guide](https://playwright.dev/docs/emulation)
- [Core Web Vitals](https://web.dev/vitals/)
- [Mobile UX Guidelines](https://developers.google.com/web/fundamentals/design-and-ux/principles)
- [Touch Target Guidelines](https://developers.google.com/web/fundamentals/accessibility/accessible-styles#multi-device_responsive_design)

---

**Note**: This test suite is part of the UOL-317 mobile responsiveness implementation for FocusHive. For questions or issues, please refer to the project documentation or create an issue in the project repository.