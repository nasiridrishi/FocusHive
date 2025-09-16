/**
 * Accessibility Testing Configuration
 *
 * Centralized configuration for accessibility testing including:
 * - WCAG 2.1 AA compliance rules
 * - Screen reader testing settings
 * - Performance thresholds
 * - Browser-specific settings
 * - Test environment configuration
 *
 * UOL-44.19: Comprehensive Accessibility E2E Tests
 */

export interface AccessibilityConfig {
  wcag: WCAGConfig;
  screenReaders: ScreenReaderConfig;
  performance: PerformanceConfig;
  browsers: BrowserConfig;
  testing: TestingConfig;
  thresholds: ThresholdConfig;
}

export interface WCAGConfig {
  level: 'A' | 'AA' | 'AAA';
  version: '2.0' | '2.1' | '2.2';
  tags: string[];
  rules: {
    enabled: string[];
    disabled: string[];
    custom: Record<string, unknown>;
  };
}

export interface ScreenReaderConfig {
  primary: string;
  supported: string[];
  testPatterns: {
    navigation: boolean;
    announcements: boolean;
    landmarks: boolean;
    forms: boolean;
  };
}

export interface PerformanceConfig {
  maxNavigationTime: number;
  maxFocusTime: number;
  maxContrastCheckTime: number;
  maxTestDuration: number;
}

export interface BrowserConfig {
  chromium: BrowserSettings;
  firefox: BrowserSettings;
  webkit: BrowserSettings;
  mobile: MobileSettings;
}

export interface BrowserSettings {
  enabled: boolean;
  specificTests: string[];
  excludedTests: string[];
}

export interface MobileSettings extends BrowserSettings {
  devices: string[];
  orientations: ('portrait' | 'landscape')[];
  touchTargets: {
    minimumSize: number;
    spacing: number;
  };
}

export interface TestingConfig {
  retries: number;
  timeout: number;
  screenshots: boolean;
  videos: boolean;
  reports: {
    html: boolean;
    json: boolean;
    junit: boolean;
  };
  parallel: boolean;
  workers: number;
}

export interface ThresholdConfig {
  colorContrast: {
    normalText: number;
    largeText: number;
    uiComponents: number;
    focusIndicators: number;
  };
  timing: {
    keyboardNavigation: number;
    focusVisible: number;
    liveRegionAnnouncement: number;
  };
  size: {
    touchTargets: number;
    focusArea: number;
    clickableArea: number;
  };
  compliance: {
    minimumPassRate: number;
    criticalViolationsAllowed: number;
    warningsThreshold: number;
  };
}

/**
 * Default accessibility testing configuration for WCAG 2.1 AA compliance
 */
export const DEFAULT_ACCESSIBILITY_CONFIG: AccessibilityConfig = {
  wcag: {
    level: 'AA',
    version: '2.1',
    tags: ['wcag2a', 'wcag2aa', 'wcag21aa', 'best-practice'],
    rules: {
      enabled: [
        // Perceivable
        'image-alt',
        'input-image-alt',
        'area-alt',
        'server-side-image-map',
        'object-alt',
        'video-caption',
        'audio-caption',
        'color-contrast',
        'color-contrast-enhanced',
        'use-landmarks',

        // Operable
        'keyboard',
        'focus-order-semantics',
        'tabindex',
        'bypass',
        'page-has-heading-one',
        'heading-order',
        'link-name',
        'link-in-text-block',
        'focus-order-semantics',

        // Understandable
        'valid-lang',
        'label',
        'form-field-multiple-labels',
        'label-title-only',
        'label-content-name-mismatch',

        // Robust
        'duplicate-id',
        'duplicate-id-active',
        'duplicate-id-aria',
        'aria-valid-attr',
        'aria-valid-attr-value',
        'aria-required-attr',
        'aria-allowed-attr',
        'aria-required-children',
        'aria-required-parent',
        'aria-roles',
        'button-name',
        'input-button-name'
      ],
      disabled: [
        // Disable rules that might be too strict for MVP
        'landmark-contentinfo-is-top-level',
        'landmark-main-is-top-level',
        'landmark-no-duplicate-banner',
        'landmark-no-duplicate-contentinfo'
      ],
      custom: {
        'color-contrast': {
          options: {
            noScroll: true,
            fontSize: '14pt',
            fontWeight: 'normal'
          }
        }
      }
    }
  },

  screenReaders: {
    primary: 'NVDA',
    supported: ['NVDA', 'JAWS', 'VoiceOver', 'TalkBack', 'Orca'],
    testPatterns: {
      navigation: true,
      announcements: true,
      landmarks: true,
      forms: true
    }
  },

  performance: {
    maxNavigationTime: 5000, // 5 seconds
    maxFocusTime: 200, // 200ms
    maxContrastCheckTime: 1000, // 1 second
    maxTestDuration: 300000 // 5 minutes per test
  },

  browsers: {
    chromium: {
      enabled: true,
      specificTests: [],
      excludedTests: []
    },
    firefox: {
      enabled: true,
      specificTests: [],
      excludedTests: []
    },
    webkit: {
      enabled: true,
      specificTests: [],
      excludedTests: ['some-webkit-incompatible-test']
    },
    mobile: {
      enabled: true,
      devices: ['iPhone 12', 'Pixel 5', 'iPad'],
      orientations: ['portrait', 'landscape'],
      specificTests: ['mobile-accessibility', 'touch-targets'],
      excludedTests: [],
      touchTargets: {
        minimumSize: 44, // 44x44px minimum
        spacing: 8 // 8px minimum spacing
      }
    }
  },

  testing: {
    retries: 2,
    timeout: 60000, // 60 seconds per test
    screenshots: true,
    videos: false, // Disabled for faster execution
    reports: {
      html: true,
      json: true,
      junit: true
    },
    parallel: true,
    workers: 3
  },

  thresholds: {
    colorContrast: {
      normalText: 4.5, // WCAG AA
      largeText: 3.0, // WCAG AA
      uiComponents: 3.0, // WCAG AA
      focusIndicators: 3.0 // WCAG AA
    },
    timing: {
      keyboardNavigation: 100, // 100ms between key presses
      focusVisible: 50, // 50ms for focus to become visible
      liveRegionAnnouncement: 500 // 500ms for live region updates
    },
    size: {
      touchTargets: 44, // 44x44px minimum
      focusArea: 24, // 24x24px minimum focus area
      clickableArea: 44 // 44x44px minimum clickable area
    },
    compliance: {
      minimumPassRate: 95, // 95% of tests must pass
      criticalViolationsAllowed: 0, // No critical violations allowed
      warningsThreshold: 10 // Maximum 10 warnings
    }
  }
};

/**
 * High contrast mode configuration
 */
export const HIGH_CONTRAST_CONFIG = {
  ...DEFAULT_ACCESSIBILITY_CONFIG,
  wcag: {
    ...DEFAULT_ACCESSIBILITY_CONFIG.wcag,
    level: 'AAA' as const,
    tags: ['wcag2a', 'wcag2aa', 'wcag2aaa', 'wcag21aa', 'wcag21aaa']
  },
  thresholds: {
    ...DEFAULT_ACCESSIBILITY_CONFIG.thresholds,
    colorContrast: {
      normalText: 7.0, // WCAG AAA
      largeText: 4.5, // WCAG AAA
      uiComponents: 4.5, // Enhanced for high contrast
      focusIndicators: 4.5 // Enhanced for high contrast
    }
  }
};

/**
 * Mobile-first accessibility configuration
 */
export const MOBILE_ACCESSIBILITY_CONFIG = {
  ...DEFAULT_ACCESSIBILITY_CONFIG,
  browsers: {
    ...DEFAULT_ACCESSIBILITY_CONFIG.browsers,
    mobile: {
      ...DEFAULT_ACCESSIBILITY_CONFIG.browsers.mobile,
      touchTargets: {
        minimumSize: 48, // Larger for better mobile experience
        spacing: 12 // More spacing for mobile
      }
    }
  },
  thresholds: {
    ...DEFAULT_ACCESSIBILITY_CONFIG.thresholds,
    size: {
      touchTargets: 48,
      focusArea: 44,
      clickableArea: 48
    }
  }
};

/**
 * Development environment configuration (more lenient)
 */
export const DEV_ACCESSIBILITY_CONFIG = {
  ...DEFAULT_ACCESSIBILITY_CONFIG,
  testing: {
    ...DEFAULT_ACCESSIBILITY_CONFIG.testing,
    retries: 1,
    timeout: 30000,
    screenshots: false,
    videos: false
  },
  thresholds: {
    ...DEFAULT_ACCESSIBILITY_CONFIG.thresholds,
    compliance: {
      minimumPassRate: 85, // More lenient for development
      criticalViolationsAllowed: 2, // Allow some critical issues in dev
      warningsThreshold: 20 // More warnings allowed in dev
    }
  }
};

/**
 * CI/CD environment configuration (strict)
 */
export const CI_ACCESSIBILITY_CONFIG = {
  ...DEFAULT_ACCESSIBILITY_CONFIG,
  testing: {
    ...DEFAULT_ACCESSIBILITY_CONFIG.testing,
    retries: 3,
    timeout: 90000,
    screenshots: true,
    videos: true,
    parallel: false, // Sequential execution for stability
    workers: 1
  },
  thresholds: {
    ...DEFAULT_ACCESSIBILITY_CONFIG.thresholds,
    compliance: {
      minimumPassRate: 98, // Very strict for CI
      criticalViolationsAllowed: 0, // No critical violations in CI
      warningsThreshold: 5 // Very few warnings allowed
    }
  }
};

/**
 * Get accessibility configuration based on environment
 */
export function getAccessibilityConfig(environment?: string): AccessibilityConfig {
  switch (environment?.toLowerCase()) {
    case 'development':
    case 'dev':
      return DEV_ACCESSIBILITY_CONFIG;

    case 'ci':
    case 'continuous-integration':
      return CI_ACCESSIBILITY_CONFIG;

    case 'mobile':
      return MOBILE_ACCESSIBILITY_CONFIG;

    case 'high-contrast':
      return HIGH_CONTRAST_CONFIG;

    case 'production':
    case 'prod':
    default:
      return DEFAULT_ACCESSIBILITY_CONFIG;
  }
}

/**
 * WCAG 2.1 Success Criteria mapping
 */
export const WCAG_SUCCESS_CRITERIA = {
  // Level A
  'non-text-content': '1.1.1',
  'audio-only-video-only': '1.2.1',
  'captions-prerecorded': '1.2.2',
  'audio-description-prerecorded': '1.2.3',
  'info-and-relationships': '1.3.1',
  'meaningful-sequence': '1.3.2',
  'sensory-characteristics': '1.3.3',
  'use-of-color': '1.4.1',
  'audio-control': '1.4.2',
  'keyboard': '2.1.1',
  'no-keyboard-trap': '2.1.2',
  'timing-adjustable': '2.2.1',
  'pause-stop-hide': '2.2.2',
  'three-flashes': '2.3.1',
  'bypass-blocks': '2.4.1',
  'page-titled': '2.4.2',
  'focus-order': '2.4.3',
  'link-purpose': '2.4.4',
  'language-of-page': '3.1.1',
  'on-focus': '3.2.1',
  'on-input': '3.2.2',
  'error-identification': '3.3.1',
  'labels-or-instructions': '3.3.2',
  'parsing': '4.1.1',
  'name-role-value': '4.1.2',

  // Level AA
  'captions-live': '1.2.4',
  'audio-description-prerecorded-2': '1.2.5',
  'orientation': '1.3.4',
  'identify-input-purpose': '1.3.5',
  'contrast-minimum': '1.4.3',
  'resize-text': '1.4.4',
  'images-of-text': '1.4.5',
  'reflow': '1.4.10',
  'non-text-contrast': '1.4.11',
  'text-spacing': '1.4.12',
  'content-on-hover-focus': '1.4.13',
  'character-key-shortcuts': '2.1.4',
  'pointer-gestures': '2.5.1',
  'pointer-cancellation': '2.5.2',
  'label-in-name': '2.5.3',
  'motion-actuation': '2.5.4',
  'language-of-parts': '3.1.2',
  'consistent-navigation': '3.2.3',
  'consistent-identification': '3.2.4',
  'error-suggestion': '3.3.3',
  'error-prevention': '3.3.4',
  'status-messages': '4.1.3'
};

/**
 * Accessibility testing priorities
 */
export const TEST_PRIORITIES = {
  critical: [
    'keyboard-navigation',
    'screen-reader-compatibility',
    'color-contrast',
    'form-accessibility',
    'focus-management'
  ],
  important: [
    'aria-implementation',
    'semantic-structure',
    'error-handling',
    'live-regions',
    'responsive-accessibility'
  ],
  beneficial: [
    'performance-accessibility',
    'animation-preferences',
    'print-accessibility',
    'high-contrast-mode'
  ]
};

/**
 * Test data for accessibility testing
 */
export const ACCESSIBILITY_TEST_DATA = {
  validEmails: [
    'user@example.com',
    'test.email+tag@domain.co.uk',
    'firstname.lastname@company.org'
  ],
  invalidEmails: [
    'invalid-email',
    '@domain.com',
    'user@',
    'user.domain.com'
  ],
  passwords: {
    weak: ['123', 'password', 'abc'],
    strong: ['SecurePass123!', 'MyStr0ng@Password', 'C0mpl3x!P4ssw0rd']
  },
  phoneNumbers: {
    valid: ['+1234567890', '(555) 123-4567', '+44 20 7946 0958'],
    invalid: ['abc123', '123', 'phone-number']
  },
  names: {
    first: ['John', 'Jane', 'Alex', 'Maria'],
    last: ['Smith', 'Johnson', 'Williams', 'Brown']
  },
  addresses: {
    street: ['123 Main St', '456 Oak Avenue', '789 Pine Road'],
    city: ['New York', 'London', 'Tokyo', 'Sydney'],
    postal: ['10001', 'SW1A 1AA', '100-0001', '2000']
  }
};

export default DEFAULT_ACCESSIBILITY_CONFIG;