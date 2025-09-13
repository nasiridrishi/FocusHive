/**
 * K6 Load Testing Helper Utilities
 * 
 * Common utilities for FocusHive load testing scenarios
 */

import { check, sleep } from 'k6';
import { Rate, Counter, Trend, Gauge } from 'k6/metrics';
import { randomString, randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
export const authSuccessRate = new Rate('auth_success_rate');
export const hiveOperationsRate = new Rate('hive_operations_success_rate');
export const wsConnectionTime = new Trend('ws_connection_time');
export const wsMessageLatency = new Trend('ws_message_latency');
export const apiCallsCounter = new Counter('api_calls_total');
export const concurrentUsers = new Gauge('concurrent_users');

// Authentication helper
export class AuthHelper {
  constructor(http, baseUrl) {
    this.http = http;
    this.baseUrl = baseUrl;
    this.tokens = new Map();
  }

  /**
   * Authenticate user and return JWT token
   */
  login(email, password) {
    const loginPayload = {
      email: email,
      password: password
    };

    const response = this.http.post(
      `${this.baseUrl}/api/auth/login`,
      JSON.stringify(loginPayload),
      {
        headers: {
          'Content-Type': 'application/json',
        },
        tags: { endpoint: 'auth_login' }
      }
    );

    const success = check(response, {
      'login status is 200': (r) => r.status === 200,
      'login response has token': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.access_token !== undefined;
        } catch {
          return false;
        }
      },
      'login response time < 2s': (r) => r.timings.duration < 2000,
    });

    authSuccessRate.add(success);
    apiCallsCounter.add(1);

    if (success && response.status === 200) {
      const body = JSON.parse(response.body);
      this.tokens.set(email, body.access_token);
      return body.access_token;
    }

    return null;
  }

  /**
   * Get cached token or login if needed
   */
  getToken(email, password) {
    if (this.tokens.has(email)) {
      return this.tokens.get(email);
    }
    return this.login(email, password);
  }

  /**
   * Get authorization headers
   */
  getAuthHeaders(token) {
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    };
  }

  /**
   * Refresh token
   */
  refreshToken(refreshToken) {
    const response = this.http.post(
      `${this.baseUrl}/api/auth/refresh`,
      JSON.stringify({ refresh_token: refreshToken }),
      {
        headers: { 'Content-Type': 'application/json' },
        tags: { endpoint: 'auth_refresh' }
      }
    );

    const success = check(response, {
      'refresh status is 200': (r) => r.status === 200,
      'refresh has new token': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.access_token !== undefined;
        } catch {
          return false;
        }
      }
    });

    authSuccessRate.add(success);
    return success ? JSON.parse(response.body).access_token : null;
  }

  /**
   * Logout user
   */
  logout(token) {
    const response = this.http.post(
      `${this.baseUrl}/api/auth/logout`,
      null,
      {
        headers: this.getAuthHeaders(token),
        tags: { endpoint: 'auth_logout' }
      }
    );

    const success = check(response, {
      'logout status is 200 or 204': (r) => r.status === 200 || r.status === 204,
    });

    authSuccessRate.add(success);
    return success;
  }
}

// Hive operations helper
export class HiveHelper {
  constructor(http, baseUrl, authHelper) {
    this.http = http;
    this.baseUrl = baseUrl;
    this.authHelper = authHelper;
  }

  /**
   * Create a new hive
   */
  createHive(token, hiveData) {
    const payload = {
      name: hiveData.name || `Load Test Hive ${randomString(8)}`,
      description: hiveData.description || 'Created by load test',
      category: hiveData.category || randomItem(['work', 'study', 'personal']),
      isPublic: hiveData.isPublic !== undefined ? hiveData.isPublic : true,
      maxMembers: hiveData.maxMembers || randomIntBetween(10, 100)
    };

    const response = this.http.post(
      `${this.baseUrl}/api/hives`,
      JSON.stringify(payload),
      {
        headers: this.authHelper.getAuthHeaders(token),
        tags: { endpoint: 'hives_create' }
      }
    );

    const success = check(response, {
      'create hive status is 201': (r) => r.status === 201,
      'create hive response has id': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.id !== undefined;
        } catch {
          return false;
        }
      },
      'create hive response time < 1s': (r) => r.timings.duration < 1000,
    });

    hiveOperationsRate.add(success);
    apiCallsCounter.add(1);

    return success ? JSON.parse(response.body) : null;
  }

  /**
   * Get list of hives
   */
  getHives(token, params = {}) {
    let url = `${this.baseUrl}/api/hives`;
    const queryParams = new URLSearchParams(params);
    if (queryParams.toString()) {
      url += `?${queryParams.toString()}`;
    }

    const response = this.http.get(url, {
      headers: this.authHelper.getAuthHeaders(token),
      tags: { endpoint: 'hives_list' }
    });

    const success = check(response, {
      'get hives status is 200': (r) => r.status === 200,
      'get hives response is array': (r) => {
        try {
          const body = JSON.parse(r.body);
          return Array.isArray(body.content || body);
        } catch {
          return false;
        }
      },
      'get hives response time < 500ms': (r) => r.timings.duration < 500,
    });

    hiveOperationsRate.add(success);
    apiCallsCounter.add(1);

    return success ? JSON.parse(response.body) : null;
  }

  /**
   * Get specific hive
   */
  getHive(token, hiveId) {
    const response = this.http.get(
      `${this.baseUrl}/api/hives/${hiveId}`,
      {
        headers: this.authHelper.getAuthHeaders(token),
        tags: { endpoint: 'hives_get' }
      }
    );

    const success = check(response, {
      'get hive status is 200': (r) => r.status === 200,
      'get hive response has id': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.id === hiveId;
        } catch {
          return false;
        }
      },
      'get hive response time < 300ms': (r) => r.timings.duration < 300,
    });

    hiveOperationsRate.add(success);
    apiCallsCounter.add(1);

    return success ? JSON.parse(response.body) : null;
  }

  /**
   * Join a hive
   */
  joinHive(token, hiveId) {
    const response = this.http.post(
      `${this.baseUrl}/api/hives/${hiveId}/join`,
      null,
      {
        headers: this.authHelper.getAuthHeaders(token),
        tags: { endpoint: 'hives_join' }
      }
    );

    const success = check(response, {
      'join hive status is 200': (r) => r.status === 200,
      'join hive response time < 500ms': (r) => r.timings.duration < 500,
    });

    hiveOperationsRate.add(success);
    apiCallsCounter.add(1);

    return success;
  }

  /**
   * Leave a hive
   */
  leaveHive(token, hiveId) {
    const response = this.http.post(
      `${this.baseUrl}/api/hives/${hiveId}/leave`,
      null,
      {
        headers: this.authHelper.getAuthHeaders(token),
        tags: { endpoint: 'hives_leave' }
      }
    );

    const success = check(response, {
      'leave hive status is 200': (r) => r.status === 200,
      'leave hive response time < 500ms': (r) => r.timings.duration < 500,
    });

    hiveOperationsRate.add(success);
    return success;
  }

  /**
   * Update hive
   */
  updateHive(token, hiveId, updates) {
    const response = this.http.put(
      `${this.baseUrl}/api/hives/${hiveId}`,
      JSON.stringify(updates),
      {
        headers: this.authHelper.getAuthHeaders(token),
        tags: { endpoint: 'hives_update' }
      }
    );

    const success = check(response, {
      'update hive status is 200': (r) => r.status === 200,
      'update hive response time < 1s': (r) => r.timings.duration < 1000,
    });

    hiveOperationsRate.add(success);
    apiCallsCounter.add(1);

    return success ? JSON.parse(response.body) : null;
  }

  /**
   * Delete hive
   */
  deleteHive(token, hiveId) {
    const response = this.http.del(
      `${this.baseUrl}/api/hives/${hiveId}`,
      null,
      {
        headers: this.authHelper.getAuthHeaders(token),
        tags: { endpoint: 'hives_delete' }
      }
    );

    const success = check(response, {
      'delete hive status is 200 or 204': (r) => r.status === 200 || r.status === 204,
      'delete hive response time < 500ms': (r) => r.timings.duration < 500,
    });

    hiveOperationsRate.add(success);
    return success;
  }
}

// Presence helper
export class PresenceHelper {
  constructor(http, baseUrl, authHelper) {
    this.http = http;
    this.baseUrl = baseUrl;
    this.authHelper = authHelper;
  }

  /**
   * Update user presence
   */
  updatePresence(token, presenceData) {
    const payload = {
      status: presenceData.status || 'online',
      activity: presenceData.activity || 'working',
      hiveId: presenceData.hiveId,
      timestamp: new Date().toISOString()
    };

    const response = this.http.post(
      `${this.baseUrl}/api/presence`,
      JSON.stringify(payload),
      {
        headers: this.authHelper.getAuthHeaders(token),
        tags: { endpoint: 'presence_update' }
      }
    );

    const success = check(response, {
      'update presence status is 200': (r) => r.status === 200,
      'update presence response time < 200ms': (r) => r.timings.duration < 200,
    });

    apiCallsCounter.add(1);
    return success;
  }

  /**
   * Get hive presence
   */
  getHivePresence(token, hiveId) {
    const response = this.http.get(
      `${this.baseUrl}/api/presence/hive/${hiveId}`,
      {
        headers: this.authHelper.getAuthHeaders(token),
        tags: { endpoint: 'presence_get' }
      }
    );

    const success = check(response, {
      'get presence status is 200': (r) => r.status === 200,
      'get presence response time < 300ms': (r) => r.timings.duration < 300,
    });

    apiCallsCounter.add(1);
    return success ? JSON.parse(response.body) : null;
  }
}

// Analytics helper
export class AnalyticsHelper {
  constructor(http, baseUrl, authHelper) {
    this.http = http;
    this.baseUrl = baseUrl;
    this.authHelper = authHelper;
  }

  /**
   * Get dashboard analytics
   */
  getDashboard(token) {
    const response = this.http.get(
      `${this.baseUrl}/api/analytics/dashboard`,
      {
        headers: this.authHelper.getAuthHeaders(token),
        tags: { endpoint: 'analytics_dashboard' }
      }
    );

    const success = check(response, {
      'analytics dashboard status is 200': (r) => r.status === 200,
      'analytics dashboard response time < 1s': (r) => r.timings.duration < 1000,
    });

    apiCallsCounter.add(1);
    return success ? JSON.parse(response.body) : null;
  }

  /**
   * Get productivity metrics
   */
  getProductivity(token, params = {}) {
    let url = `${this.baseUrl}/api/analytics/productivity`;
    const queryParams = new URLSearchParams(params);
    if (queryParams.toString()) {
      url += `?${queryParams.toString()}`;
    }

    const response = this.http.get(url, {
      headers: this.authHelper.getAuthHeaders(token),
      tags: { endpoint: 'analytics_productivity' }
    });

    const success = check(response, {
      'productivity analytics status is 200': (r) => r.status === 200,
      'productivity analytics response time < 2s': (r) => r.timings.duration < 2000,
    });

    apiCallsCounter.add(1);
    return success ? JSON.parse(response.body) : null;
  }
}

// WebSocket helper
export class WebSocketHelper {
  constructor(ws, baseUrl) {
    this.ws = ws;
    this.baseUrl = baseUrl;
    this.connections = new Map();
  }

  /**
   * Connect to WebSocket
   */
  connect(path, token, params = {}) {
    const wsUrl = `${this.baseUrl.replace('http', 'ws')}${path}`;
    const start = Date.now();

    try {
      const socket = this.ws.connect(wsUrl, params, (socket) => {
        const connectionTime = Date.now() - start;
        wsConnectionTime.add(connectionTime);

        socket.on('open', () => {
          // Send authentication
          socket.send(JSON.stringify({
            type: 'auth',
            token: token
          }));
        });

        socket.on('message', (data) => {
          try {
            const message = JSON.parse(data);
            if (message.timestamp) {
              const latency = Date.now() - message.timestamp;
              wsMessageLatency.add(latency);
            }
          } catch (e) {
            // Handle non-JSON messages
          }
        });

        socket.on('error', (e) => {
          console.error('WebSocket error:', e);
        });
      });

      this.connections.set(path, socket);
      return socket;
    } catch (error) {
      console.error(`Failed to connect to WebSocket ${wsUrl}:`, error);
      return null;
    }
  }

  /**
   * Send message to WebSocket
   */
  sendMessage(path, message) {
    const socket = this.connections.get(path);
    if (socket && socket.readyState === 1) { // OPEN
      const messageWithTimestamp = {
        ...message,
        timestamp: Date.now()
      };
      socket.send(JSON.stringify(messageWithTimestamp));
      return true;
    }
    return false;
  }

  /**
   * Close connection
   */
  closeConnection(path) {
    const socket = this.connections.get(path);
    if (socket) {
      socket.close();
      this.connections.delete(path);
      return true;
    }
    return false;
  }

  /**
   * Close all connections
   */
  closeAll() {
    for (const [path, socket] of this.connections) {
      socket.close();
    }
    this.connections.clear();
  }
}

// Load test utilities
export class LoadTestUtils {
  /**
   * Generate random user data
   */
  static generateUser() {
    const firstName = randomItem(['John', 'Jane', 'Mike', 'Sarah', 'David', 'Lisa', 'Chris', 'Emma']);
    const lastName = randomItem(['Smith', 'Johnson', 'Brown', 'Davis', 'Miller', 'Wilson', 'Moore', 'Taylor']);
    
    return {
      firstName,
      lastName,
      email: `${firstName.toLowerCase()}.${lastName.toLowerCase()}+${randomString(5)}@loadtest.com`,
      password: 'LoadTest123!',
      preferences: {
        notifications: true,
        theme: randomItem(['light', 'dark', 'auto']),
        language: 'en'
      }
    };
  }

  /**
   * Generate random hive data
   */
  static generateHive() {
    const categories = ['work', 'study', 'personal', 'team', 'project'];
    const adjectives = ['Focused', 'Productive', 'Creative', 'Collaborative', 'Efficient'];
    const nouns = ['Hive', 'Space', 'Room', 'Zone', 'Hub', 'Lab', 'Studio'];
    
    return {
      name: `${randomItem(adjectives)} ${randomItem(nouns)} ${randomString(4)}`,
      description: `A ${randomItem(['productive', 'collaborative', 'focused'])} space for ${randomItem(['work', 'study', 'projects'])}`,
      category: randomItem(categories),
      isPublic: Math.random() > 0.3, // 70% public
      maxMembers: randomIntBetween(10, 100),
      settings: {
        allowChat: true,
        requireApproval: Math.random() > 0.5,
        focusMode: randomItem(['pomodoro', 'deep-work', 'flexible'])
      }
    };
  }

  /**
   * Wait with jitter
   */
  static sleepWithJitter(baseSeconds, jitterPercent = 0.1) {
    const jitter = baseSeconds * jitterPercent * (Math.random() - 0.5) * 2;
    const sleepTime = Math.max(0.1, baseSeconds + jitter);
    sleep(sleepTime);
  }

  /**
   * Simulate realistic user behavior
   */
  static simulateUserBehavior() {
    // Simulate thinking time between actions
    this.sleepWithJitter(randomIntBetween(1, 3));
  }

  /**
   * Get random item weighted by probability
   */
  static weightedRandomItem(items, weights) {
    if (items.length !== weights.length) {
      throw new Error('Items and weights arrays must have same length');
    }

    const totalWeight = weights.reduce((sum, weight) => sum + weight, 0);
    let random = Math.random() * totalWeight;

    for (let i = 0; i < items.length; i++) {
      random -= weights[i];
      if (random <= 0) {
        return items[i];
      }
    }

    return items[items.length - 1]; // Fallback
  }

  /**
   * Record custom metric
   */
  static recordMetric(name, value, tags = {}) {
    if (!global.customMetrics) {
      global.customMetrics = {};
    }
    
    if (!global.customMetrics[name]) {
      global.customMetrics[name] = new Trend(name);
    }
    
    global.customMetrics[name].add(value, tags);
  }

  /**
   * Update concurrent users gauge
   */
  static updateConcurrentUsers(count) {
    concurrentUsers.add(count);
  }

  /**
   * Log test progress
   */
  static logProgress(message, data = {}) {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] ${message}`, JSON.stringify(data, null, 2));
  }
}

// Error handling utilities
export function handleError(error, context = '') {
  console.error(`Error in ${context}:`, error);
  
  // Record error metric
  if (!global.errorCounter) {
    global.errorCounter = new Counter('errors_total');
  }
  global.errorCounter.add(1, { context });
}

// Test data generators
export const TestDataGenerator = {
  user: LoadTestUtils.generateUser,
  hive: LoadTestUtils.generateHive,
  
  presenceUpdate() {
    return {
      status: randomItem(['online', 'away', 'busy']),
      activity: randomItem(['coding', 'studying', 'meeting', 'break', 'planning']),
      timestamp: new Date().toISOString()
    };
  },
  
  notification() {
    return {
      type: randomItem(['hive_invite', 'session_start', 'achievement', 'reminder']),
      title: `Test Notification ${randomString(6)}`,
      message: `This is a test notification generated at ${new Date().toISOString()}`,
      priority: randomItem(['low', 'normal', 'high'])
    };
  },
  
  chatMessage() {
    const messages = [
      'Hello everyone!',
      'How is everyone doing today?',
      'Great work session!',
      'Taking a quick break',
      'Back to work!',
      'Anyone want to pair program?',
      'Good luck with your tasks!',
      'See you all tomorrow!'
    ];
    
    return {
      content: randomItem(messages),
      timestamp: new Date().toISOString()
    };
  }
};