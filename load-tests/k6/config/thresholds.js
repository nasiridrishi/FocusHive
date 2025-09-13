/**
 * Performance Thresholds Configuration for k6 Load Tests
 * 
 * Defines performance thresholds for different test scenarios
 * and service level objectives (SLOs) for FocusHive services
 */

export const API_THRESHOLDS = {
  // HTTP response time thresholds
  http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95th percentile < 500ms, 99th < 1000ms
  
  // Success rate thresholds
  http_req_failed: ['rate<0.01'], // Error rate should be less than 1%
  
  // Specific endpoint thresholds
  endpoints: {
    auth: {
      http_req_duration: ['p(95)<200', 'p(99)<500'],
      http_req_failed: ['rate<0.005'] // 0.5% error rate for critical auth
    },
    hives: {
      http_req_duration: ['p(95)<300', 'p(99)<800'],
      http_req_failed: ['rate<0.01']
    },
    analytics: {
      http_req_duration: ['p(95)<1000', 'p(99)<2000'], // Analytics can be slower
      http_req_failed: ['rate<0.02']
    },
    notifications: {
      http_req_duration: ['p(95)<100', 'p(99)<200'], // Fast notifications
      http_req_failed: ['rate<0.005']
    }
  }
};

export const WEBSOCKET_THRESHOLDS = {
  // WebSocket connection thresholds
  ws_connection_time: ['p(95)<1000'], // Connection time < 1s
  ws_message_latency: ['p(95)<100', 'p(99)<200'], // Message latency
  ws_connection_failed: ['rate<0.01'], // Connection failure rate
  
  // Message throughput
  ws_messages_per_second: ['rate>50'], // At least 50 messages/sec capability
  
  // Service-specific WebSocket thresholds
  services: {
    presence: {
      ws_message_latency: ['p(95)<50', 'p(99)<100'], // Real-time presence
      ws_connection_failed: ['rate<0.005']
    },
    chat: {
      ws_message_latency: ['p(95)<100', 'p(99)<200'],
      ws_connection_failed: ['rate<0.01']
    },
    notifications: {
      ws_message_latency: ['p(95)<50', 'p(99)<100'],
      ws_connection_failed: ['rate<0.005']
    }
  }
};

export const LOAD_TEST_SCENARIOS = {
  // Smoke test - minimal load
  smoke: {
    executor: 'constant-vus',
    vus: 1,
    duration: '1m',
    thresholds: {
      ...API_THRESHOLDS,
      checks: ['rate>0.99'] // 99% check success rate
    }
  },

  // Load test - normal expected load
  load: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '2m', target: 10 },  // Ramp up
      { duration: '5m', target: 10 },  // Stay at 10 users
      { duration: '2m', target: 20 },  // Ramp to 20 users
      { duration: '5m', target: 20 },  // Stay at 20 users
      { duration: '2m', target: 0 }    // Ramp down
    ],
    thresholds: API_THRESHOLDS
  },

  // Stress test - higher than normal load
  stress: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '2m', target: 20 },  // Ramp up
      { duration: '5m', target: 20 },  // Stay at 20
      { duration: '2m', target: 50 },  // Ramp to 50
      { duration: '5m', target: 50 },  // Stay at 50
      { duration: '2m', target: 100 }, // Ramp to 100
      { duration: '5m', target: 100 }, // Stay at 100
      { duration: '5m', target: 0 }    // Ramp down
    ],
    thresholds: {
      http_req_duration: ['p(95)<1000', 'p(99)<2000'], // More relaxed for stress
      http_req_failed: ['rate<0.05'], // 5% error rate acceptable under stress
      checks: ['rate>0.95']
    }
  },

  // Spike test - sudden load increase
  spike: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '1m', target: 10 },   // Normal load
      { duration: '30s', target: 100 }, // Spike!
      { duration: '1m', target: 100 },  // Stay at spike
      { duration: '30s', target: 10 },  // Back to normal
      { duration: '1m', target: 0 }     // Ramp down
    ],
    thresholds: {
      http_req_duration: ['p(95)<2000'], // Very relaxed during spike
      http_req_failed: ['rate<0.1'],     // 10% error rate during spike
      checks: ['rate>0.90']
    }
  },

  // Soak test - extended duration
  soak: {
    executor: 'constant-vus',
    vus: 20,
    duration: '30m', // Extended duration
    thresholds: {
      ...API_THRESHOLDS,
      // Memory leak detection through response time degradation
      http_req_duration: ['p(95)<500', 'p(99)<1000'],
      // Stricter error rate for soak test
      http_req_failed: ['rate<0.005']
    }
  },

  // Breakpoint test - find the breaking point
  breakpoint: {
    executor: 'ramping-arrival-rate',
    startRate: 1,
    timeUnit: '1s',
    preAllocatedVUs: 500,
    maxVUs: 1000,
    stages: [
      { duration: '2m', target: 10 },   // Start with 10 req/s
      { duration: '5m', target: 50 },   // Ramp to 50 req/s
      { duration: '5m', target: 100 },  // Ramp to 100 req/s
      { duration: '5m', target: 200 },  // Ramp to 200 req/s
      { duration: '5m', target: 300 },  // Ramp to 300 req/s
      { duration: '5m', target: 400 },  // Find breaking point
    ],
    thresholds: {
      http_req_duration: ['p(95)<1000'],
      http_req_failed: ['rate<0.1'],
      // Stop test if error rate exceeds 20%
      'http_req_failed{expected_response:true}': ['rate<0.2']
    }
  }
};

export const SERVICE_ENDPOINTS = {
  base_url: __ENV.BASE_URL || 'http://localhost:8080',
  
  // Service URLs
  services: {
    identity: __ENV.IDENTITY_URL || 'http://localhost:8081',
    music: __ENV.MUSIC_URL || 'http://localhost:8082', 
    notification: __ENV.NOTIFICATION_URL || 'http://localhost:8083',
    chat: __ENV.CHAT_URL || 'http://localhost:8084',
    analytics: __ENV.ANALYTICS_URL || 'http://localhost:8085',
    forum: __ENV.FORUM_URL || 'http://localhost:8086',
    buddy: __ENV.BUDDY_URL || 'http://localhost:8087'
  },

  // API endpoints to test
  endpoints: {
    // Authentication endpoints
    auth: {
      login: '/api/auth/login',
      me: '/api/auth/me',
      refresh: '/api/auth/refresh',
      logout: '/api/auth/logout'
    },
    
    // Hive management
    hives: {
      list: '/api/hives',
      create: '/api/hives',
      get: (id) => `/api/hives/${id}`,
      update: (id) => `/api/hives/${id}`,
      delete: (id) => `/api/hives/${id}`,
      join: (id) => `/api/hives/${id}/join`,
      leave: (id) => `/api/hives/${id}/leave`
    },
    
    // User presence
    presence: {
      update: '/api/presence',
      get: (hiveId) => `/api/presence/hive/${hiveId}`,
      history: '/api/presence/history'
    },
    
    // Analytics
    analytics: {
      dashboard: '/api/analytics/dashboard',
      productivity: '/api/analytics/productivity',
      insights: '/api/analytics/insights',
      export: '/api/analytics/export'
    },
    
    // Notifications
    notifications: {
      list: '/api/notifications',
      markRead: (id) => `/api/notifications/${id}/read`,
      settings: '/api/notifications/settings'
    },
    
    // User profiles
    users: {
      profile: '/api/users/profile',
      update: '/api/users/profile',
      preferences: '/api/users/preferences'
    }
  },

  // WebSocket endpoints
  websockets: {
    main: '/ws',
    presence: '/ws/presence',
    chat: (hiveId) => `/ws/chat/${hiveId}`,
    notifications: '/ws/notifications'
  }
};

export const TEST_DATA = {
  // Test user credentials
  users: [
    { email: 'test1@focushive.com', password: 'TestPassword123!' },
    { email: 'test2@focushive.com', password: 'TestPassword123!' },
    { email: 'test3@focushive.com', password: 'TestPassword123!' },
    { email: 'loadtest@focushive.com', password: 'LoadTest123!' }
  ],
  
  // Sample hive data
  hives: [
    {
      name: 'Load Test Hive 1',
      description: 'Hive for load testing purposes',
      category: 'work',
      isPublic: true,
      maxMembers: 50
    },
    {
      name: 'Performance Test Hive',
      description: 'Testing performance and scalability',
      category: 'study', 
      isPublic: true,
      maxMembers: 100
    }
  ],
  
  // Sample presence updates
  presenceStates: ['online', 'away', 'busy', 'offline'],
  
  // Sample activities
  activities: [
    'coding', 'studying', 'meeting', 'break', 'planning',
    'writing', 'reading', 'designing', 'testing', 'reviewing'
  ],
  
  // Sample notification types
  notificationTypes: [
    'hive_invite', 'session_start', 'session_end', 
    'achievement', 'reminder', 'system_alert'
  ]
};

// Environment-specific configurations
export const ENVIRONMENTS = {
  local: {
    base_url: 'http://localhost:8080',
    thresholds: API_THRESHOLDS,
    load_profile: 'light'
  },
  
  staging: {
    base_url: 'https://staging.focushive.com',
    thresholds: {
      ...API_THRESHOLDS,
      http_req_duration: ['p(95)<800', 'p(99)<1500'] // More relaxed for staging
    },
    load_profile: 'moderate'
  },
  
  production: {
    base_url: 'https://focushive.com',
    thresholds: {
      ...API_THRESHOLDS,
      http_req_failed: ['rate<0.005'] // Stricter for production
    },
    load_profile: 'conservative'
  }
};

// Load profiles
export const LOAD_PROFILES = {
  light: {
    max_vus: 50,
    duration_multiplier: 1,
    ramp_time: '1m'
  },
  
  moderate: {
    max_vus: 200,
    duration_multiplier: 1.5,
    ramp_time: '2m'
  },
  
  heavy: {
    max_vus: 500,
    duration_multiplier: 2,
    ramp_time: '3m'
  },
  
  conservative: {
    max_vus: 25,
    duration_multiplier: 0.5,
    ramp_time: '30s'
  }
};