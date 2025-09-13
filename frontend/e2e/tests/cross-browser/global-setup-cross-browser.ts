/**
 * Global Setup for Cross-Browser Testing
 * Initializes test environment and starts services
 */

import { chromium, firefox, webkit, devices, FullConfig } from '@playwright/test';
import { exec } from 'child_process';
import { promisify } from 'util';
import * as fs from 'fs/promises';
import * as path from 'path';

const execAsync = promisify(exec);

async function globalSetup(config: FullConfig) {
  console.log('🚀 Starting Cross-Browser Test Environment Setup...');

  // Create necessary directories
  const dirs = [
    'test-results',
    'cross-browser-report',
    'screenshots',
    'videos'
  ];

  for (const dir of dirs) {
    await fs.mkdir(dir, { recursive: true }).catch(() => {});
  }

  // Start the development server
  console.log('📡 Starting development server...');
  try {
    // Check if server is already running
    const response = await fetch('http://localhost:5173').catch(() => null);
    
    if (!response || !response.ok) {
      console.log('Starting Vite dev server...');
      const serverProcess = exec('npm run dev', { cwd: process.cwd() });
      
      // Wait for server to be ready
      let attempts = 0;
      const maxAttempts = 30;
      
      while (attempts < maxAttempts) {
        try {
          const response = await fetch('http://localhost:5173');
          if (response.ok) {
            console.log('✅ Development server started successfully');
            break;
          }
        } catch {
          // Server not ready yet
        }
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        attempts++;
      }
      
      if (attempts >= maxAttempts) {
        throw new Error('Failed to start development server after 30 seconds');
      }
    } else {
      console.log('✅ Development server already running');
    }
  } catch (error) {
    console.error('❌ Failed to start development server:', error);
    throw error;
  }

  // Initialize browser compatibility matrix
  const compatibilityMatrix = {
    timestamp: new Date().toISOString(),
    testEnvironment: {
      nodeVersion: process.version,
      playwrightVersion: require('@playwright/test/package.json').version,
      platform: process.platform,
      arch: process.arch
    },
    browsers: {
      chrome: { tested: false, version: '', features: {} },
      firefox: { tested: false, version: '', features: {} },
      safari: { tested: false, version: '', features: {} },
      edge: { tested: false, version: '', features: {} }
    },
    testCategories: {
      'browser-compatibility': { total: 0, passed: 0, failed: 0 },
      'javascript-features': { total: 0, passed: 0, failed: 0 },
      'css-compatibility': { total: 0, passed: 0, failed: 0 },
      'websocket-compatibility': { total: 0, passed: 0, failed: 0 },
      'media-compatibility': { total: 0, passed: 0, failed: 0 },
      'pwa-compatibility': { total: 0, passed: 0, failed: 0 },
      'form-compatibility': { total: 0, passed: 0, failed: 0 },
      'browser-specific-features': { total: 0, passed: 0, failed: 0 }
    }
  };

  await fs.writeFile('cross-browser-compatibility-matrix.json', JSON.stringify(compatibilityMatrix, null, 2));

  // Detect available browsers and their versions
  console.log('🔍 Detecting browser versions...');
  
  try {
    // Chrome/Chromium detection
    const chromeBrowser = await chromium.launch({ headless: true });
    const chromeVersion = await chromeBrowser.version();
    await chromeBrowser.close();
    compatibilityMatrix.browsers.chrome.version = chromeVersion;
    console.log(`  Chrome: ${chromeVersion}`);
  } catch (error) {
    console.log('  Chrome: Not available');
  }

  try {
    // Firefox detection
    const firefoxBrowser = await firefox.launch({ headless: true });
    const firefoxVersion = await firefoxBrowser.version();
    await firefoxBrowser.close();
    compatibilityMatrix.browsers.firefox.version = firefoxVersion;
    console.log(`  Firefox: ${firefoxVersion}`);
  } catch (error) {
    console.log('  Firefox: Not available');
  }

  try {
    // Safari/WebKit detection
    const safarieBrowser = await webkit.launch({ headless: true });
    const safariVersion = await safarieBrowser.version();
    await safarieBrowser.close();
    compatibilityMatrix.browsers.safari.version = safariVersion;
    console.log(`  Safari: ${safariVersion}`);
  } catch (error) {
    console.log('  Safari: Not available');
  }

  // Create test configuration summary
  const testConfig = {
    baseURL: config.webServer?.url || 'http://localhost:5173',
    timeout: config.timeout || 60000,
    expect: {
      timeout: config.expect?.timeout || 10000
    },
    projects: config.projects?.map(project => ({
      name: project.name,
      browser: project.use?.browserName || 'unknown',
      viewport: project.use?.viewport,
      device: project.use?.deviceScaleFactor ? 'mobile' : 'desktop'
    })) || [],
    workers: config.workers
  };

  await fs.writeFile('test-config-summary.json', JSON.stringify(testConfig, null, 2));

  // Set up performance monitoring
  console.log('📊 Setting up performance monitoring...');
  
  const performanceConfig = {
    enabled: true,
    thresholds: {
      pageLoad: 5000,     // 5 seconds max page load
      apiResponse: 2000,  // 2 seconds max API response
      renderTime: 1000,   // 1 second max render time
      memoryUsage: 100,   // 100MB max memory usage
      networkRequests: 50 // 50 max network requests per page
    },
    metrics: {
      webVitals: true,
      customMetrics: true,
      resourceTiming: true,
      navigationTiming: true
    }
  };

  await fs.writeFile('performance-config.json', JSON.stringify(performanceConfig, null, 2));

  // Initialize cross-browser feature detection
  console.log('🧪 Initializing feature detection matrix...');
  
  const featureMatrix = {
    html5: {
      canvas: { expected: true, results: {} },
      video: { expected: true, results: {} },
      audio: { expected: true, results: {} },
      localStorage: { expected: true, results: {} },
      sessionStorage: { expected: true, results: {} },
      webWorkers: { expected: true, results: {} },
      applicationCache: { expected: false, results: {} }, // Deprecated
      geolocation: { expected: true, results: {} }
    },
    css3: {
      flexbox: { expected: true, results: {} },
      grid: { expected: true, results: {} },
      transforms: { expected: true, results: {} },
      transitions: { expected: true, results: {} },
      animations: { expected: true, results: {} },
      customProperties: { expected: true, results: {} },
      filters: { expected: true, results: {} }
    },
    javascript: {
      es6Classes: { expected: true, results: {} },
      arrowFunctions: { expected: true, results: {} },
      templateLiterals: { expected: true, results: {} },
      destructuring: { expected: true, results: {} },
      promises: { expected: true, results: {} },
      asyncAwait: { expected: true, results: {} },
      modules: { expected: true, results: {} }
    },
    webapis: {
      fetch: { expected: true, results: {} },
      websocket: { expected: true, results: {} },
      webrtc: { expected: true, results: {} },
      serviceWorker: { expected: true, results: {} },
      pushNotifications: { expected: true, results: {} },
      webShare: { expected: false, results: {} }, // Not universal
      paymentRequest: { expected: false, results: {} }, // Chrome-specific
      webAuthn: { expected: true, results: {} }
    },
    media: {
      webp: { expected: true, results: {} },
      avif: { expected: false, results: {} }, // Not universal yet
      webm: { expected: true, results: {} },
      mp4: { expected: true, results: {} },
      webAudio: { expected: true, results: {} },
      mediaDevices: { expected: true, results: {} }
    }
  };

  await fs.writeFile('feature-detection-matrix.json', JSON.stringify(featureMatrix, null, 2));

  // Create test execution plan
  const executionPlan = {
    phases: [
      {
        name: 'Feature Detection',
        tests: ['browser-compatibility.spec.ts'],
        browsers: ['chrome', 'firefox', 'safari', 'edge'],
        priority: 'high'
      },
      {
        name: 'JavaScript Compatibility',
        tests: ['javascript-features.spec.ts'],
        browsers: ['chrome', 'firefox', 'safari', 'edge'],
        priority: 'high'
      },
      {
        name: 'CSS Compatibility',
        tests: ['css-compatibility.spec.ts'],
        browsers: ['chrome', 'firefox', 'safari', 'edge'],
        priority: 'high'
      },
      {
        name: 'Real-time Features',
        tests: ['websocket-compatibility.spec.ts'],
        browsers: ['chrome', 'firefox', 'safari', 'edge'],
        priority: 'critical'
      },
      {
        name: 'Media & Files',
        tests: ['media-compatibility.spec.ts'],
        browsers: ['chrome', 'firefox', 'safari', 'edge'],
        priority: 'medium'
      },
      {
        name: 'PWA Features',
        tests: ['pwa-compatibility.spec.ts'],
        browsers: ['chrome', 'firefox', 'safari', 'edge'],
        priority: 'medium'
      },
      {
        name: 'Form Interactions',
        tests: ['form-compatibility.spec.ts'],
        browsers: ['chrome', 'firefox', 'safari', 'edge'],
        priority: 'medium'
      },
      {
        name: 'Browser-Specific',
        tests: ['browser-specific-features.spec.ts'],
        browsers: ['chrome', 'firefox', 'safari', 'edge'],
        priority: 'low'
      }
    ],
    estimatedDuration: {
      total: '45-60 minutes',
      perBrowser: '10-15 minutes',
      parallel: '15-20 minutes'
    }
  };

  await fs.writeFile('test-execution-plan.json', JSON.stringify(executionPlan, null, 2));

  // Set up test data and fixtures
  console.log('📁 Setting up test data and fixtures...');
  
  const testData = {
    users: {
      testUser: {
        email: 'test@focushive.com',
        password: 'testPassword123',
        name: 'Test User'
      }
    },
    testContent: {
      sampleText: 'This is sample text for testing',
      longText: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. '.repeat(50),
      specialCharacters: '!@#$%^&*()_+-=[]{}|;:,.<>?',
      unicodeText: '🚀 Unicode test: café, naïve, résumé, 中文, العربية, עברית'
    },
    testFiles: {
      image: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==',
      json: JSON.stringify({ test: 'data', number: 42, array: [1, 2, 3] }),
      text: 'This is a test file content'
    },
    apiEndpoints: {
      health: '/api/health',
      auth: '/api/auth',
      users: '/api/users',
      websocket: 'ws://localhost:8080/ws'
    }
  };

  await fs.writeFile('test-data.json', JSON.stringify(testData, null, 2));

  // Create browser-specific configurations
  const browserConfigs = {
    chrome: {
      launchOptions: {
        args: [
          '--no-sandbox',
          '--disable-setuid-sandbox',
          '--disable-dev-shm-usage',
          '--disable-gpu',
          '--disable-web-security',
          '--disable-features=TranslateUI',
          '--disable-ipc-flooding-protection',
          '--enable-automation',
          '--disable-background-timer-throttling',
          '--disable-backgrounding-occluded-windows',
          '--disable-renderer-backgrounding'
        ]
      },
      viewport: { width: 1280, height: 720 },
      permissions: ['camera', 'microphone', 'notifications'],
      features: ['webgl', 'webgl2', 'webcodecs', 'paymentrequest']
    },
    firefox: {
      launchOptions: {
        firefoxUserPrefs: {
          'dom.webnotifications.enabled': true,
          'media.navigator.permission.disabled': true,
          'dom.serviceWorkers.enabled': true
        }
      },
      viewport: { width: 1280, height: 720 },
      permissions: ['camera', 'microphone', 'notifications'],
      features: ['webgl', 'webrtc', 'serviceworkers']
    },
    safari: {
      launchOptions: {},
      viewport: { width: 1280, height: 720 },
      permissions: ['camera', 'microphone'],
      features: ['webgl', 'serviceworkers'],
      limitations: ['indexeddb-private-browsing', 'websocket-background-tabs']
    },
    edge: {
      launchOptions: {
        args: [
          '--no-sandbox',
          '--disable-setuid-sandbox',
          '--disable-dev-shm-usage'
        ]
      },
      viewport: { width: 1280, height: 720 },
      permissions: ['camera', 'microphone', 'notifications'],
      features: ['webgl', 'webgl2', 'paymentrequest', 'webauthn']
    }
  };

  await fs.writeFile('browser-configs.json', JSON.stringify(browserConfigs, null, 2));

  console.log('✅ Cross-Browser Test Environment Setup Complete');
  console.log(`📊 Test execution plan: ${executionPlan.phases.length} phases`);
  console.log(`🌐 Browsers detected: ${Object.entries(compatibilityMatrix.browsers).filter(([_, browser]) => browser.version).length}`);
  console.log(`⏱️  Estimated duration: ${executionPlan.estimatedDuration.parallel} (parallel execution)`);

  // Store setup completion timestamp
  process.env.CROSS_BROWSER_SETUP_TIME = Date.now().toString();
}

export default globalSetup;