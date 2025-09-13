/**
 * Mobile Testing Configuration for FocusHive E2E Tests
 * Comprehensive device matrix and mobile testing settings
 */

import { defineConfig, devices, PlaywrightTestConfig } from '@playwright/test';

export interface MobileDeviceConfig {
  name: string;
  device: string;
  viewport: {
    width: number;
    height: number;
  };
  userAgent: string;
  deviceScaleFactor: number;
  isMobile: boolean;
  hasTouch: boolean;
  platform: 'iOS' | 'Android' | 'iPadOS';
  category: 'phone' | 'tablet' | 'foldable';
  orientation?: 'portrait' | 'landscape';
  safeArea?: {
    top: number;
    right: number;
    bottom: number;
    left: number;
  };
}

export interface ViewportBreakpoint {
  name: string;
  width: number;
  height?: number;
  description: string;
  testScenarios: string[];
}

export interface TouchTargetSpec {
  minSize: number;
  recommendedSize: number;
  spacing: number;
  platform: 'iOS' | 'Android' | 'universal';
}

export interface PerformanceBudget {
  loadTime: number; // milliseconds
  fcp: number; // First Contentful Paint
  lcp: number; // Largest Contentful Paint
  fid: number; // First Input Delay
  cls: number; // Cumulative Layout Shift
  memoryUsage: number; // MB
  bundleSize: number; // KB
}

/**
 * Latest Mobile Device Matrix (2024)
 */
export const MOBILE_DEVICES: Record<string, MobileDeviceConfig> = {
  // iPhone Models
  IPHONE_SE_3RD: {
    name: 'iPhone SE (3rd gen)',
    device: 'iPhone SE',
    viewport: { width: 375, height: 667 },
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1',
    deviceScaleFactor: 2,
    isMobile: true,
    hasTouch: true,
    platform: 'iOS',
    category: 'phone',
    orientation: 'portrait'
  },

  IPHONE_14: {
    name: 'iPhone 14',
    device: 'iPhone 14',
    viewport: { width: 390, height: 844 },
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1',
    deviceScaleFactor: 3,
    isMobile: true,
    hasTouch: true,
    platform: 'iOS',
    category: 'phone',
    orientation: 'portrait',
    safeArea: { top: 47, right: 0, bottom: 34, left: 0 }
  },

  IPHONE_14_PRO: {
    name: 'iPhone 14 Pro',
    device: 'iPhone 14 Pro',
    viewport: { width: 393, height: 852 },
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1',
    deviceScaleFactor: 3,
    isMobile: true,
    hasTouch: true,
    platform: 'iOS',
    category: 'phone',
    orientation: 'portrait',
    safeArea: { top: 59, right: 0, bottom: 34, left: 0 }
  },

  IPHONE_15_PRO: {
    name: 'iPhone 15 Pro',
    device: 'iPhone 15 Pro',
    viewport: { width: 393, height: 852 },
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Mobile/15E148 Safari/604.1',
    deviceScaleFactor: 3,
    isMobile: true,
    hasTouch: true,
    platform: 'iOS',
    category: 'phone',
    orientation: 'portrait',
    safeArea: { top: 59, right: 0, bottom: 34, left: 0 }
  },

  IPHONE_15_PRO_MAX: {
    name: 'iPhone 15 Pro Max',
    device: 'iPhone 15 Pro Max',
    viewport: { width: 430, height: 932 },
    userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Mobile/15E148 Safari/604.1',
    deviceScaleFactor: 3,
    isMobile: true,
    hasTouch: true,
    platform: 'iOS',
    category: 'phone',
    orientation: 'portrait',
    safeArea: { top: 59, right: 0, bottom: 34, left: 0 }
  },

  // Android Phones
  PIXEL_5: {
    name: 'Google Pixel 5',
    device: 'Pixel 5',
    viewport: { width: 393, height: 851 },
    userAgent: 'Mozilla/5.0 (Linux; Android 14; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
    deviceScaleFactor: 2.75,
    isMobile: true,
    hasTouch: true,
    platform: 'Android',
    category: 'phone',
    orientation: 'portrait'
  },

  PIXEL_7: {
    name: 'Google Pixel 7',
    device: 'Pixel 7',
    viewport: { width: 412, height: 915 },
    userAgent: 'Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
    deviceScaleFactor: 2.625,
    isMobile: true,
    hasTouch: true,
    platform: 'Android',
    category: 'phone',
    orientation: 'portrait'
  },

  GALAXY_S21: {
    name: 'Samsung Galaxy S21',
    device: 'Galaxy S21',
    viewport: { width: 360, height: 800 },
    userAgent: 'Mozilla/5.0 (Linux; Android 14; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
    deviceScaleFactor: 3,
    isMobile: true,
    hasTouch: true,
    platform: 'Android',
    category: 'phone',
    orientation: 'portrait'
  },

  GALAXY_S23_ULTRA: {
    name: 'Samsung Galaxy S23 Ultra',
    device: 'Galaxy S23 Ultra',
    viewport: { width: 384, height: 854 },
    userAgent: 'Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
    deviceScaleFactor: 3.5,
    isMobile: true,
    hasTouch: true,
    platform: 'Android',
    category: 'phone',
    orientation: 'portrait'
  },

  ONEPLUS_9: {
    name: 'OnePlus 9',
    device: 'OnePlus 9',
    viewport: { width: 412, height: 915 },
    userAgent: 'Mozilla/5.0 (Linux; Android 14; LE2113) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
    deviceScaleFactor: 2.625,
    isMobile: true,
    hasTouch: true,
    platform: 'Android',
    category: 'phone',
    orientation: 'portrait'
  },

  // Tablets
  IPAD_MINI: {
    name: 'iPad Mini (6th gen)',
    device: 'iPad Mini',
    viewport: { width: 744, height: 1133 },
    userAgent: 'Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1',
    deviceScaleFactor: 2,
    isMobile: true,
    hasTouch: true,
    platform: 'iPadOS',
    category: 'tablet',
    orientation: 'portrait'
  },

  IPAD_AIR: {
    name: 'iPad Air (5th gen)',
    device: 'iPad Air',
    viewport: { width: 820, height: 1180 },
    userAgent: 'Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1',
    deviceScaleFactor: 2,
    isMobile: true,
    hasTouch: true,
    platform: 'iPadOS',
    category: 'tablet',
    orientation: 'portrait'
  },

  IPAD_PRO_11: {
    name: 'iPad Pro 11" (4th gen)',
    device: 'iPad Pro 11',
    viewport: { width: 834, height: 1194 },
    userAgent: 'Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1',
    deviceScaleFactor: 2,
    isMobile: true,
    hasTouch: true,
    platform: 'iPadOS',
    category: 'tablet',
    orientation: 'portrait'
  },

  GALAXY_TAB_S8: {
    name: 'Samsung Galaxy Tab S8',
    device: 'Galaxy Tab S8',
    viewport: { width: 753, height: 1037 },
    userAgent: 'Mozilla/5.0 (Linux; Android 14; SM-X706B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    deviceScaleFactor: 2.4,
    isMobile: true,
    hasTouch: true,
    platform: 'Android',
    category: 'tablet',
    orientation: 'portrait'
  },

  // Foldable Devices
  GALAXY_FOLD_4: {
    name: 'Samsung Galaxy Z Fold 4 (Inner)',
    device: 'Galaxy Z Fold 4',
    viewport: { width: 344, height: 748 },
    userAgent: 'Mozilla/5.0 (Linux; Android 14; SM-F936B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
    deviceScaleFactor: 3.5,
    isMobile: true,
    hasTouch: true,
    platform: 'Android',
    category: 'foldable',
    orientation: 'portrait'
  },

  GALAXY_FOLD_4_OUTER: {
    name: 'Samsung Galaxy Z Fold 4 (Cover)',
    device: 'Galaxy Z Fold 4 Cover',
    viewport: { width: 344, height: 748 },
    userAgent: 'Mozilla/5.0 (Linux; Android 14; SM-F936B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
    deviceScaleFactor: 3.5,
    isMobile: true,
    hasTouch: true,
    platform: 'Android',
    category: 'foldable',
    orientation: 'portrait'
  },

  GALAXY_FLIP_4: {
    name: 'Samsung Galaxy Z Flip 4',
    device: 'Galaxy Z Flip 4',
    viewport: { width: 360, height: 748 },
    userAgent: 'Mozilla/5.0 (Linux; Android 14; SM-F721B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
    deviceScaleFactor: 3,
    isMobile: true,
    hasTouch: true,
    platform: 'Android',
    category: 'foldable',
    orientation: 'portrait'
  }
};

/**
 * Responsive Breakpoints for Testing
 */
export const VIEWPORT_BREAKPOINTS: Record<string, ViewportBreakpoint> = {
  MOBILE_XS: {
    name: 'Mobile XS',
    width: 320,
    description: 'Extra small mobile devices (iPhone SE 1st gen)',
    testScenarios: ['text readability', 'touch targets', 'content stacking']
  },
  
  MOBILE_SM: {
    name: 'Mobile Small',
    width: 360,
    description: 'Small mobile devices (Galaxy S21)',
    testScenarios: ['navigation collapse', 'form usability', 'image scaling']
  },
  
  MOBILE_MD: {
    name: 'Mobile Medium',
    width: 390,
    description: 'Medium mobile devices (iPhone 14)',
    testScenarios: ['content adaptation', 'modal responsiveness', 'grid to stack']
  },
  
  MOBILE_LG: {
    name: 'Mobile Large',
    width: 414,
    description: 'Large mobile devices (iPhone 14 Plus)',
    testScenarios: ['landscape layouts', 'extended content', 'side-by-side views']
  },
  
  TABLET_SM: {
    name: 'Tablet Small',
    width: 768,
    description: 'Small tablets (iPad Mini)',
    testScenarios: ['sidebar layouts', 'desktop features', 'hover states']
  },
  
  TABLET_MD: {
    name: 'Tablet Medium',
    width: 834,
    description: 'Medium tablets (iPad Pro 11")',
    testScenarios: ['multi-column layouts', 'desktop navigation', 'card grids']
  },
  
  TABLET_LG: {
    name: 'Tablet Large',
    width: 1024,
    description: 'Large tablets (iPad Pro 12.9")',
    testScenarios: ['desktop equivalence', 'complex layouts', 'split views']
  }
};

/**
 * Touch Target Specifications
 */
export const TOUCH_TARGET_SPECS: Record<string, TouchTargetSpec> = {
  IOS: {
    minSize: 44,
    recommendedSize: 48,
    spacing: 8,
    platform: 'iOS'
  },
  
  ANDROID: {
    minSize: 48,
    recommendedSize: 56,
    spacing: 8,
    platform: 'Android'
  },
  
  UNIVERSAL: {
    minSize: 44,
    recommendedSize: 48,
    spacing: 8,
    platform: 'universal'
  }
};

/**
 * Performance Budgets for Mobile
 */
export const MOBILE_PERFORMANCE_BUDGETS: Record<string, PerformanceBudget> = {
  MOBILE_3G: {
    loadTime: 3000,
    fcp: 1800,
    lcp: 2500,
    fid: 100,
    cls: 0.1,
    memoryUsage: 100,
    bundleSize: 500
  },
  
  MOBILE_4G: {
    loadTime: 2000,
    fcp: 1200,
    lcp: 1800,
    fid: 100,
    cls: 0.1,
    memoryUsage: 150,
    bundleSize: 750
  },
  
  MOBILE_5G: {
    loadTime: 1500,
    fcp: 900,
    lcp: 1200,
    fid: 50,
    cls: 0.05,
    memoryUsage: 200,
    bundleSize: 1000
  },
  
  TABLET: {
    loadTime: 2000,
    fcp: 1000,
    lcp: 1500,
    fid: 100,
    cls: 0.1,
    memoryUsage: 250,
    bundleSize: 1000
  }
};

/**
 * Mobile-Specific Testing Configuration
 */
export const mobileTestConfig: PlaywrightTestConfig = {
  testDir: './e2e/tests/mobile',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 3 : 1, // More retries for mobile due to flakiness
  workers: process.env.CI ? 2 : 4,
  timeout: 90000, // Longer timeout for mobile
  
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    
    // Mobile-specific settings
    ignoreHTTPSErrors: true,
    waitForLoadState: 'networkidle',
    actionTimeout: 15000, // Longer for mobile interactions
    navigationTimeout: 30000,
    
    // Permissions for mobile features
    permissions: ['geolocation', 'notifications', 'camera', 'microphone'],
    
    // Simulate slower mobile network
    launchOptions: {
      slowMo: 50
    }
  },
  
  projects: [
    // iPhone Models
    {
      name: 'iPhone SE (3rd gen)',
      use: { ...devices['iPhone SE'] }
    },
    {
      name: 'iPhone 14',
      use: { ...devices['iPhone 12'] } // Similar specs
    },
    {
      name: 'iPhone 14 Pro',
      use: { ...devices['iPhone 12 Pro'] } // Similar specs
    },
    {
      name: 'iPhone 15 Pro',
      use: { 
        ...devices['iPhone 12 Pro'], // Base configuration
        viewport: { width: 393, height: 852 }
      }
    },
    
    // Android Models
    {
      name: 'Pixel 5',
      use: { ...devices['Pixel 5'] }
    },
    {
      name: 'Pixel 7',
      use: {
        ...devices['Pixel 5'], // Base configuration
        viewport: { width: 412, height: 915 }
      }
    },
    {
      name: 'Galaxy S21',
      use: { 
        ...devices['Galaxy S8'], // Similar Android device
        viewport: { width: 360, height: 800 }
      }
    },
    {
      name: 'Galaxy S23 Ultra',
      use: {
        ...devices['Galaxy S8'],
        viewport: { width: 384, height: 854 }
      }
    },
    
    // Tablets
    {
      name: 'iPad Mini',
      use: { ...devices['iPad Mini'] }
    },
    {
      name: 'iPad Air',
      use: { 
        ...devices['iPad Mini'],
        viewport: { width: 820, height: 1180 }
      }
    },
    {
      name: 'iPad Pro 11"',
      use: { ...devices['iPad Pro'] }
    },
    {
      name: 'Galaxy Tab S8',
      use: {
        ...devices['Galaxy Tab S4'], // Similar tablet
        viewport: { width: 753, height: 1037 }
      }
    },
    
    // Foldable Devices (Custom configurations)
    {
      name: 'Galaxy Z Fold 4 (Inner)',
      use: {
        userAgent: 'Mozilla/5.0 (Linux; Android 14; SM-F936B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
        viewport: { width: 344, height: 748 },
        deviceScaleFactor: 3.5,
        isMobile: true,
        hasTouch: true
      }
    },
    {
      name: 'Galaxy Z Flip 4',
      use: {
        userAgent: 'Mozilla/5.0 (Linux; Android 14; SM-F721B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
        viewport: { width: 360, height: 748 },
        deviceScaleFactor: 3,
        isMobile: true,
        hasTouch: true
      }
    }
  ]
};