# Accessibility E2E Testing Suite

This comprehensive accessibility testing suite ensures WCAG 2.1 AA compliance across all FocusHive application features. The suite includes automated testing with axe-core integration and manual testing guidelines for assistive technologies.

## Overview

The accessibility test suite validates compliance with:
- **WCAG 2.1 Level A** - Basic accessibility requirements
- **WCAG 2.1 Level AA** - Standard accessibility requirements (target compliance level)
- **Best Practices** - Additional recommendations for improved accessibility

## Test Structure

### 1. Main Test File
- **`accessibility.spec.ts`**: Comprehensive test suite with 45+ scenarios covering all WCAG 2.1 AA requirements

### 2. Page Object Model
- **`AccessibilityPage.ts`**: Page object model providing accessibility testing methods and element selectors

### 3. Helper Utilities
- **`accessibility.helper.ts`**: Accessibility testing utilities with axe-core integration and WCAG validation functions

## Test Categories

### 1. Keyboard Navigation (10 Tests)
- **Tab Order**: Verifies logical tab order and focus management
- **Skip Links**: Tests skip navigation functionality 
- **Keyboard Shortcuts**: Validates keyboard shortcuts and hotkeys
- **Focus Trapping**: Tests modal and dialog focus management
- **Arrow Navigation**: Verifies menu and widget navigation

**Key Requirements:**
- All interactive elements must be keyboard accessible
- Focus indicators must be clearly visible
- Tab order must be logical and match visual layout
- No keyboard traps except in modals/dialogs
- Skip links must be functional and properly positioned

### 2. Screen Reader Compatibility (5 Tests)
- **ARIA Labels**: Validates proper labeling and descriptions
- **Heading Structure**: Tests heading hierarchy (h1-h6)
- **Live Regions**: Verifies dynamic content announcements
- **Landmark Navigation**: Tests page structure landmarks
- **Alternative Text**: Validates image alt text and descriptions

**Key Requirements:**
- All interactive elements must have accessible names
- Heading hierarchy must be logical (no skipped levels)
- Dynamic content changes must be announced
- Page must have proper landmark structure
- Images must have meaningful alt text

### 3. Color and Contrast (4 Tests)
- **Contrast Ratios**: WCAG AA compliance (4.5:1 normal, 3:1 large text)
- **Color Independence**: Information not conveyed by color alone
- **High Contrast Mode**: Support for high contrast displays
- **Focus Indicators**: Visible focus indicators with adequate contrast

**Key Requirements:**
- Text contrast ratio ≥ 4.5:1 (normal text) or ≥ 3:1 (large text)
- Interactive element contrast ≥ 3:1
- Focus indicators must be clearly visible
- Information must not rely solely on color

### 4. Form Accessibility (4 Tests)
- **Label Associations**: Proper form field labeling
- **Error Messages**: Clear, accessible error communication
- **Required Fields**: Proper indication of required inputs
- **Fieldset Usage**: Logical grouping of related controls

**Key Requirements:**
- All form fields must have associated labels
- Error messages must be announced and associated with fields
- Required fields must be clearly indicated
- Related form controls must be grouped with fieldsets

### 5. Interactive Elements (5 Tests)
- **Button vs Link Semantics**: Proper use of buttons and links
- **Touch Target Sizes**: Minimum 44x44px clickable areas
- **Hover/Focus States**: Proper visual feedback
- **Loading States**: Accessible loading indicators
- **Disabled States**: Clear disabled state communication

**Key Requirements:**
- Buttons for actions, links for navigation
- Touch targets ≥ 44x44px on all devices
- Clear visual feedback for all states
- Loading states must be announced to screen readers
- Disabled states must be properly communicated

### 6. Media Accessibility (2 Tests)
- **Video Controls**: Keyboard accessible media controls
- **Captions/Transcripts**: Text alternatives for audio content

**Key Requirements:**
- All media controls must be keyboard accessible
- Videos with audio must have captions or transcripts
- Auto-playing content must be controllable

### 7. Content Structure (4 Tests)
- **Semantic HTML**: Proper use of HTML elements
- **Table Structure**: Accessible data tables
- **List Structure**: Proper list markup
- **Language/Titles**: Page language and title attributes

**Key Requirements:**
- Content must use semantic HTML elements
- Tables must have proper headers and scope attributes
- Lists must use proper list markup
- Page must have lang attribute and descriptive title

### 8. Motion and Animation (2 Tests)
- **Reduced Motion**: Respects user motion preferences
- **Auto-play Controls**: Controls for auto-playing content

**Key Requirements:**
- Animations must respect prefers-reduced-motion
- Auto-playing content must be controllable
- No seizure-inducing animations

### 9. Responsive Accessibility (3 Tests)
- **200% Zoom**: Content reflows at 200% zoom
- **320px Width**: No horizontal scrolling at 320px width
- **Touch Targets**: Adequate touch target sizes on mobile

**Key Requirements:**
- Content must be readable at 200% zoom without horizontal scrolling
- Layout must reflow properly at small viewport sizes
- Touch targets must be ≥ 44x44px on mobile devices

### 10. Assistive Technology Support (3 Tests)
- **Screen Reader Simulation**: Basic screen reader navigation
- **NVDA/JAWS Support**: Windows screen reader compatibility
- **Voice Control**: Support for voice navigation software

**Key Requirements:**
- Content must be navigable with screen readers
- Interactive elements must have accessible names for voice control
- ARIA attributes must be properly implemented

## Running the Tests

### Prerequisites
```bash
# Ensure axe-core is installed
npm install @axe-core/playwright --save-dev

# Ensure playwright is configured
npm install @playwright/test --save-dev
```

### Execute Test Suite
```bash
# Run all accessibility tests
npm run test:e2e -- --grep "Accessibility"

# Run specific test categories
npm run test:e2e -- --grep "Keyboard Navigation"
npm run test:e2e -- --grep "Screen Reader"
npm run test:e2e -- --grep "Color and Contrast"

# Run with specific browsers
npm run test:e2e -- --project=chromium --grep "Accessibility"
npm run test:e2e -- --project=firefox --grep "Accessibility"
npm run test:e2e -- --project=webkit --grep "Accessibility"

# Generate accessibility report
npm run test:e2e -- --grep "Accessibility Reporting"
```

### Test Configuration
```typescript
// playwright.config.ts additions for accessibility testing
use: {
  // Enable accessibility testing features
  ignoreHTTPSErrors: true,
  
  // Accessibility-specific settings
  reducedMotion: 'reduce', // Test with reduced motion
  forcedColors: 'active',  // Test with high contrast
  
  // Viewport settings for responsive testing
  viewport: { width: 1280, height: 720 }
}
```

## WCAG 2.1 AA Compliance Checklist

### Level A Requirements ✓
- [x] **1.1.1** Non-text Content - Alt text for images
- [x] **1.3.1** Info and Relationships - Semantic structure
- [x] **1.3.2** Meaningful Sequence - Logical reading order
- [x] **1.4.1** Use of Color - Information not conveyed by color alone
- [x] **2.1.1** Keyboard - All functionality via keyboard
- [x] **2.1.2** No Keyboard Trap - Users can navigate away
- [x] **2.4.1** Bypass Blocks - Skip navigation links
- [x] **2.4.2** Page Titled - Descriptive page titles
- [x] **3.1.1** Language of Page - Page language specified
- [x] **4.1.1** Parsing - Valid HTML markup
- [x] **4.1.2** Name, Role, Value - Accessible names for UI components

### Level AA Requirements ✓
- [x] **1.4.3** Contrast (Minimum) - 4.5:1 contrast ratio
- [x] **1.4.4** Resize Text - Text can be resized to 200%
- [x] **1.4.5** Images of Text - Use text instead of images of text
- [x] **2.4.3** Focus Order - Logical focus order
- [x] **2.4.4** Link Purpose (In Context) - Descriptive link text
- [x] **2.4.5** Multiple Ways - Multiple navigation methods
- [x] **2.4.6** Headings and Labels - Descriptive headings and labels
- [x] **2.4.7** Focus Visible - Visible focus indicator
- [x] **3.1.2** Language of Parts - Language changes identified
- [x] **3.2.1** On Focus - No context changes on focus
- [x] **3.2.2** On Input - No context changes on input
- [x] **3.3.1** Error Identification - Errors are identified
- [x] **3.3.2** Labels or Instructions - Clear labels and instructions
- [x] **4.1.3** Status Messages - Status changes are announced

### WCAG 2.1 Additions ✓
- [x] **1.3.4** Orientation - Content works in both orientations
- [x] **1.3.5** Identify Input Purpose - Input purpose identified
- [x] **1.4.10** Reflow - Content reflows at 320px width
- [x] **1.4.11** Non-text Contrast - UI component contrast ≥ 3:1
- [x] **1.4.12** Text Spacing - Text spacing can be adjusted
- [x] **1.4.13** Content on Hover or Focus - Dismissible, hoverable, persistent
- [x] **2.1.4** Character Key Shortcuts - Single key shortcuts can be disabled
- [x] **2.5.1** Pointer Gestures - Multi-point gestures have alternatives
- [x] **2.5.2** Pointer Cancellation - Up-event activation or abort/undo
- [x] **2.5.3** Label in Name - Accessible name includes visible text
- [x] **2.5.4** Motion Actuation - Motion-based input has alternatives
- [x] **4.1.3** Status Messages - Status changes announced to screen readers

## Manual Testing Procedures

### Screen Reader Testing
1. **NVDA (Windows - Free)**
   - Download: https://www.nvaccess.org/
   - Test navigation with Tab, Arrow keys, H (headings), L (links)
   - Verify content is announced correctly
   - Test form interactions and error states

2. **JAWS (Windows - Commercial)**
   - Use Virtual Cursor mode for reading
   - Test Quick Navigation keys (H, L, B, F, T)
   - Verify table navigation and form mode
   - Test complex widgets and ARIA implementations

3. **VoiceOver (macOS/iOS - Built-in)**
   - Enable: System Preferences > Accessibility > VoiceOver
   - Navigate with VO+Arrow keys, VO+Space to activate
   - Test Rotor navigation (VO+U)
   - Test mobile VoiceOver gestures on iOS

4. **TalkBack (Android - Built-in)**
   - Enable: Settings > Accessibility > TalkBack
   - Navigate with swipe gestures
   - Test global gestures and reading controls
   - Verify focus management and announcements

### Voice Control Testing
1. **Voice Access (Android)**
   - Enable voice commands for navigation
   - Test "Click [element name]" commands
   - Verify all interactive elements have speech-friendly names

2. **Voice Control (iOS/macOS)**
   - Enable in Accessibility settings
   - Test "Tap [button name]" commands
   - Verify custom voice command support

3. **Dragon NaturallySpeaking (Windows)**
   - Test voice navigation commands
   - Verify form filling with voice input
   - Test custom vocabulary and commands

### Keyboard Testing
1. **Navigation Testing**
   - Test Tab/Shift+Tab through all interactive elements
   - Verify logical tab order matches visual layout
   - Test arrow key navigation in menus and widgets
   - Verify Escape key closes dialogs and menus

2. **Focus Management**
   - Verify focus indicators are clearly visible
   - Test focus doesn't get lost or trapped
   - Verify focus returns to trigger element after dialog closes
   - Test skip links functionality

3. **Keyboard Shortcuts**
   - Document and test all keyboard shortcuts
   - Verify shortcuts don't conflict with assistive technology
   - Test single-key shortcuts can be disabled when needed

## Automated Testing Tools

### axe-core Integration
```typescript
import AxeBuilder from '@axe-core/playwright';

// Basic accessibility scan
const results = await new AxeBuilder({ page })
  .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
  .analyze();

// Custom rule configuration
const results = await new AxeBuilder({ page })
  .withRules(['color-contrast', 'keyboard-navigation'])
  .exclude('[data-test-ignore-accessibility]')
  .analyze();
```

### Color Contrast Testing
```typescript
// Test specific elements
await axeHelper.checkElementContrast(element);

// Test all text elements
const contrastResults = await new AxeBuilder({ page })
  .withRules(['color-contrast'])
  .analyze();
```

### Custom Accessibility Rules
```typescript
// Add custom rules for FocusHive-specific patterns
axe.configure({
  rules: {
    'custom-focus-management': {
      enabled: true,
      selector: '[data-focus-trap]'
    }
  }
});
```

## Continuous Integration

### GitHub Actions Integration
```yaml
# .github/workflows/accessibility.yml
name: Accessibility Tests
on: [push, pull_request]

jobs:
  accessibility:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - run: npm ci
      - run: npm run test:e2e -- --grep "Accessibility"
      - uses: actions/upload-artifact@v3
        if: failure()
        with:
          name: accessibility-report
          path: test-results/
```

### Quality Gates
```typescript
// Set accessibility thresholds
const accessibilityThresholds = {
  violations: 0,        // Zero tolerance for violations
  contrastRatio: 4.5,   // WCAG AA minimum
  coveragePercent: 90   // 90% of UI components tested
};
```

## Reporting and Metrics

### Test Reports
The accessibility test suite generates comprehensive reports including:
- **Violation Summary**: Count and severity of accessibility issues
- **Compliance Score**: Percentage of WCAG 2.1 AA requirements met
- **Element Coverage**: Percentage of UI elements tested
- **Trend Analysis**: Accessibility metrics over time

### Key Performance Indicators (KPIs)
- **Zero Critical Violations**: No critical accessibility barriers
- **≥95% Compliance Score**: Near-perfect WCAG 2.1 AA compliance
- **≥4.5:1 Contrast Ratio**: All text meets minimum contrast requirements
- **100% Keyboard Access**: All functionality available via keyboard

### Accessibility Dashboard
Monitor accessibility metrics through:
1. **Automated Test Results**: CI/CD pipeline reports
2. **Manual Test Tracking**: Screen reader and keyboard test results
3. **User Feedback**: Accessibility issues reported by users
4. **Compliance Audits**: Regular third-party accessibility assessments

## Best Practices for Developers

### Development Guidelines
1. **Semantic HTML First**: Use proper HTML elements before adding ARIA
2. **Keyboard Navigation**: Ensure all functionality is keyboard accessible
3. **Focus Management**: Implement proper focus indicators and management
4. **Alternative Text**: Provide meaningful alt text for all images
5. **Color Independence**: Don't rely solely on color to convey information

### Testing During Development
1. **Test Early**: Run accessibility tests during feature development
2. **Manual Validation**: Test with keyboard and screen reader regularly
3. **Automated Scanning**: Use axe-devtools browser extension
4. **User Testing**: Include users with disabilities in testing process

### Common Issues to Avoid
- Missing alt attributes on images
- Poor color contrast ratios
- Inaccessible form labels
- Keyboard navigation traps
- Missing focus indicators
- Non-semantic HTML usage
- Poorly structured headings
- Inaccessible error messages

## Resources and References

### WCAG 2.1 Guidelines
- **Official Specification**: https://www.w3.org/WAI/WCAG21/quickref/
- **Understanding WCAG 2.1**: https://www.w3.org/WAI/WCAG21/Understanding/
- **Techniques for WCAG 2.1**: https://www.w3.org/WAI/WCAG21/Techniques/

### Testing Tools
- **axe-core**: https://github.com/dequelabs/axe-core
- **Playwright**: https://playwright.dev/
- **axe DevTools**: https://www.deque.com/axe/devtools/
- **WAVE**: https://wave.webaim.org/

### Assistive Technologies
- **NVDA Screen Reader**: https://www.nvaccess.org/
- **JAWS Screen Reader**: https://www.freedomscientific.com/products/software/jaws/
- **VoiceOver Guide**: https://support.apple.com/guide/voiceover/
- **TalkBack Guide**: https://support.google.com/accessibility/android/topic/3529932

### Learning Resources
- **WebAIM**: https://webaim.org/
- **A11y Project**: https://www.a11yproject.com/
- **Inclusive Design Patterns**: https://shop.smashingmagazine.com/products/inclusive-design-patterns
- **Accessibility Developer Guide**: https://www.accessibility-developer-guide.com/

## Support and Maintenance

### Regular Testing Schedule
- **Daily**: Automated accessibility tests in CI/CD
- **Weekly**: Manual keyboard and screen reader testing
- **Monthly**: Comprehensive accessibility audit
- **Quarterly**: Third-party accessibility assessment

### Issue Tracking
- **GitHub Issues**: Track accessibility bugs with 'accessibility' label
- **Priority Levels**: Critical, High, Medium, Low based on WCAG impact
- **SLA**: Fix critical issues within 24 hours, high within 1 week

### Team Training
- **Accessibility Awareness**: Regular team training sessions
- **Tool Usage**: Training on accessibility testing tools
- **User Empathy**: Sessions with users who rely on assistive technology
- **Best Practices**: Regular review of accessibility guidelines and techniques

---

**Note**: This accessibility testing suite is designed to ensure FocusHive meets and exceeds WCAG 2.1 AA standards. Regular updates to this documentation and test suite should be made as accessibility standards and tools evolve.