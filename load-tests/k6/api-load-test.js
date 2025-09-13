/**
 * k6 API Load Test
 * 
 * Tests REST API performance under various load conditions
 * Focuses on authentication, hive management, and core API endpoints
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { AuthenticationHelper, HiveHelper, PresenceHelper, AnalyticsHelper } from './utils/helpers.js';
import { API_THRESHOLDS, LOAD_TEST_SCENARIOS, SERVICE_ENDPOINTS } from './config/thresholds.js';

// Custom metrics
export const authenticationRate = new Rate('authentication_success');
export const apiErrorRate = new Rate('api_errors');
export const responseTimeTrend = new Trend('api_response_time');
export const requestCounter = new Counter('api_requests_total');

// Test configuration
export const options = {
  scenarios: {
    // Smoke test - basic functionality
    smoke: {
      ...LOAD_TEST_SCENARIOS.smoke,
      exec: 'smokeTest',
      tags: { test_type: 'smoke' }
    },
    
    // Load test - normal expected load
    normal_load: {
      ...LOAD_TEST_SCENARIOS.load,
      exec: 'loadTest',
      startTime: '5s',
      tags: { test_type: 'load' }
    },
    
    // Stress test - high load
    stress_load: {
      ...LOAD_TEST_SCENARIOS.stress,
      exec: 'stressTest',
      startTime: '30s',
      tags: { test_type: 'stress' }
    }
  },
  
  thresholds: {
    ...API_THRESHOLDS,
    'authentication_success': ['rate>0.95'],
    'api_errors': ['rate<0.01'],
    'api_response_time': ['p(95)<500'],
    'http_req_duration{group:::Authentication}': API_THRESHOLDS.endpoints.auth.http_req_duration,
    'http_req_duration{group:::Hive Management}': API_THRESHOLDS.endpoints.hives.http_req_duration,
    'http_req_duration{group:::Analytics}': API_THRESHOLDS.endpoints.analytics.http_req_duration
  }
};

// Test data
const testUsers = [
  { email: 'loadtest1@focushive.com', password: 'LoadTest123!' },
  { email: 'loadtest2@focushive.com', password: 'LoadTest123!' },
  { email: 'loadtest3@focushive.com', password: 'LoadTest123!' }
];

let authHelper;
let hiveHelper;
let presenceHelper;
let analyticsHelper;

export function setup() {
  console.log('Setting up API Load Test');
  
  // Initialize helpers
  authHelper = new AuthenticationHelper(SERVICE_ENDPOINTS.base_url);
  hiveHelper = new HiveHelper(SERVICE_ENDPOINTS.base_url);
  presenceHelper = new PresenceHelper(SERVICE_ENDPOINTS.base_url);
  analyticsHelper = new AnalyticsHelper(SERVICE_ENDPOINTS.base_url);
  
  // Pre-authenticate admin user for setup
  const adminAuth = authHelper.login('admin@focushive.com', 'AdminPassword123!');
  
  if (!adminAuth.success) {
    console.error('Failed to authenticate admin user for setup');
    return { setupFailed: true };
  }
  
  console.log('API Load Test setup completed');
  return { 
    setupFailed: false,
    adminToken: adminAuth.token,
    baseUrl: SERVICE_ENDPOINTS.base_url
  };
}

export function smokeTest(data) {
  if (data.setupFailed) {
    console.error('Skipping smoke test due to setup failure');
    return;
  }
  
  group('Smoke Test - Basic API Functionality', () => {
    const userIndex = (__VU - 1) % testUsers.length;
    const testUser = testUsers[userIndex];
    
    // Test authentication
    group('Authentication', () => {
      const loginResult = authHelper.login(testUser.email, testUser.password);
      
      authenticationRate.add(loginResult.success);
      requestCounter.add(1);
      
      if (loginResult.success) {
        responseTimeTrend.add(loginResult.responseTime);
        
        // Test token validation
        const meResult = authHelper.validateToken(loginResult.token);
        check(meResult, {
          'user profile retrieved': (r) => r.success,
          'user email matches': (r) => r.user && r.user.email === testUser.email
        });
      } else {
        apiErrorRate.add(1);
        console.warn(`Authentication failed for user: ${testUser.email}`);
      }
    });
    
    sleep(1);
  });
}

export function loadTest(data) {
  if (data.setupFailed) {
    console.error('Skipping load test due to setup failure');
    return;
  }
  
  const userIndex = (__VU - 1) % testUsers.length;
  const testUser = testUsers[userIndex];
  
  group('Load Test - Normal Usage Patterns', () => {
    let authToken = null;
    
    // Authentication flow
    group('Authentication', () => {
      const loginResult = authHelper.login(testUser.email, testUser.password);
      
      authenticationRate.add(loginResult.success);
      requestCounter.add(1);
      
      if (loginResult.success) {
        authToken = loginResult.token;
        responseTimeTrend.add(loginResult.responseTime);
      } else {
        apiErrorRate.add(1);
        return;
      }
    });
    
    if (!authToken) return;
    
    // Hive management operations
    group('Hive Management', () => {
      // List hives
      const listHivesResult = hiveHelper.listHives(authToken);
      check(listHivesResult, {
        'hives list retrieved': (r) => r.success,
        'hives response time acceptable': (r) => r.responseTime < 300
      });
      
      requestCounter.add(1);
      if (listHivesResult.success) {
        responseTimeTrend.add(listHivesResult.responseTime);
      } else {
        apiErrorRate.add(1);
      }
      
      // Create a test hive
      const hiveData = {
        name: `LoadTest Hive ${__VU}-${Date.now()}`,
        description: 'Automated load test hive',
        category: 'work',
        isPublic: true,
        maxMembers: 10
      };
      
      const createHiveResult = hiveHelper.createHive(authToken, hiveData);
      check(createHiveResult, {
        'hive created successfully': (r) => r.success,
        'hive creation time acceptable': (r) => r.responseTime < 500
      });
      
      requestCounter.add(1);
      if (createHiveResult.success) {
        responseTimeTrend.add(createHiveResult.responseTime);
        
        // Join the created hive
        const joinResult = hiveHelper.joinHive(authToken, createHiveResult.hiveId);
        check(joinResult, {
          'hive joined successfully': (r) => r.success,
          'join response time acceptable': (r) => r.responseTime < 200
        });
        
        requestCounter.add(1);
        if (joinResult.success) {
          responseTimeTrend.add(joinResult.responseTime);
        } else {
          apiErrorRate.add(1);
        }
      } else {
        apiErrorRate.add(1);
      }
    });
    
    // Presence operations
    group('Presence Management', () => {
      const presenceUpdate = {
        status: 'online',
        activity: 'coding',
        hiveId: null // Will be set from available hives
      };
      
      const updateResult = presenceHelper.updatePresence(authToken, presenceUpdate);
      check(updateResult, {
        'presence updated': (r) => r.success,
        'presence update time acceptable': (r) => r.responseTime < 100
      });
      
      requestCounter.add(1);
      if (updateResult.success) {
        responseTimeTrend.add(updateResult.responseTime);
      } else {
        apiErrorRate.add(1);
      }
    });
    
    // Analytics operations
    group('Analytics', () => {
      const dashboardResult = analyticsHelper.getDashboard(authToken);
      check(dashboardResult, {
        'dashboard data retrieved': (r) => r.success,
        'dashboard response time acceptable': (r) => r.responseTime < 1000
      });
      
      requestCounter.add(1);
      if (dashboardResult.success) {
        responseTimeTrend.add(dashboardResult.responseTime);
      } else {
        apiErrorRate.add(1);
      }
    });
    
    sleep(Math.random() * 2 + 1); // Random sleep 1-3 seconds
  });
}

export function stressTest(data) {
  if (data.setupFailed) {
    console.error('Skipping stress test due to setup failure');
    return;
  }
  
  const userIndex = (__VU - 1) % testUsers.length;
  const testUser = testUsers[userIndex];
  
  group('Stress Test - High Load Conditions', () => {
    let authToken = null;
    
    // Rapid authentication
    group('High-Frequency Authentication', () => {
      const loginResult = authHelper.login(testUser.email, testUser.password);
      
      authenticationRate.add(loginResult.success);
      requestCounter.add(1);
      
      if (loginResult.success) {
        authToken = loginResult.token;
        responseTimeTrend.add(loginResult.responseTime);
      } else {
        apiErrorRate.add(1);
      }
    });
    
    if (!authToken) return;
    
    // Rapid API calls
    group('High-Frequency API Operations', () => {
      for (let i = 0; i < 5; i++) {
        // Rapid hive list requests
        const listResult = hiveHelper.listHives(authToken);
        requestCounter.add(1);
        
        if (listResult.success) {
          responseTimeTrend.add(listResult.responseTime);
        } else {
          apiErrorRate.add(1);
        }
        
        // Rapid presence updates
        const presenceResult = presenceHelper.updatePresence(authToken, {
          status: i % 2 === 0 ? 'online' : 'busy',
          activity: `stress-test-${i}`
        });
        requestCounter.add(1);
        
        if (presenceResult.success) {
          responseTimeTrend.add(presenceResult.responseTime);
        } else {
          apiErrorRate.add(1);
        }
        
        // Minimal sleep between rapid requests
        sleep(0.1);
      }
    });
    
    sleep(0.5); // Shorter sleep for stress conditions
  });
}

export function teardown(data) {
  if (data.setupFailed) {
    console.log('Teardown skipped due to setup failure');
    return;
  }
  
  console.log('API Load Test completed');
  console.log(`Total API requests: ${requestCounter.value}`);
  console.log(`Average response time: ${responseTimeTrend.avg}ms`);
  console.log(`Authentication success rate: ${(authenticationRate.rate * 100).toFixed(2)}%`);
  console.log(`API error rate: ${(apiErrorRate.rate * 100).toFixed(2)}%`);
}