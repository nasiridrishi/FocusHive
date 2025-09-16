/**
 * E2E Test Configuration for Identity Service
 *
 * Comprehensive testing configuration for Profile and Persona Management
 * Supports CM3035 Advanced Web Design template requirements
 *
 * @fileoverview Identity service E2E test configuration
 * @version 1.0.0
 */

/**
 * API endpoints for Identity Service
 */
export const IDENTITY_API = {
  BASE_URL: process.env.E2E_IDENTITY_API_URL || 'http://localhost:8081',
  ENDPOINTS: {
    // Authentication
    LOGIN: '/api/v1/auth/login',
    LOGOUT: '/api/v1/auth/logout',
    REFRESH: '/api/v1/auth/refresh',
    REGISTER: '/api/v1/auth/register',

    // Personas
    PERSONAS: '/api/v1/personas',
    PERSONA_BY_ID: (id: string) => `/api/v1/personas/${id}`,
    PERSONA_SWITCH: (id: string) => `/api/v1/personas/${id}/switch`,
    PERSONA_DEFAULT: (id: string) => `/api/v1/personas/${id}/default`,
    ACTIVE_PERSONA: '/api/v1/personas/active',
    PERSONA_TEMPLATES: '/api/v1/personas/templates',

    // OAuth2 Provider
    OAUTH2_AUTHORIZE: '/api/v1/oauth2/authorize',
    OAUTH2_TOKEN: '/api/v1/oauth2/token',
    OAUTH2_INTROSPECT: '/api/v1/oauth2/introspect',
    OAUTH2_REVOKE: '/api/v1/oauth2/revoke',
    OAUTH2_USERINFO: '/api/v1/oauth2/userinfo',
    OAUTH2_CLIENTS: '/api/v1/oauth2/clients',

    // Privacy & Data
    PRIVACY_PREFERENCES: '/api/v1/privacy/preferences',
    DATA_EXPORT: '/api/v1/privacy/data-export',
    DATA_IMPORT: '/api/v1/privacy/data-import',
    CONSENT: '/api/v1/privacy/consent',

    // Server metadata
    WELL_KNOWN: '/.well-known/oauth-authorization-server'
  }
} as const;

/**
 * Frontend routes for Identity features
 */
export const IDENTITY_ROUTES = {
  // Authentication
  LOGIN: '/auth/login',
  REGISTER: '/auth/register',
  PASSWORD_RESET: '/auth/password-reset',

  // Profile & Personas
  PROFILE: '/profile',
  PERSONAS: '/profile/personas',
  PERSONA_CREATE: '/profile/personas/create',
  PERSONA_EDIT: (id: string) => `/profile/personas/${id}/edit`,

  // Privacy Settings
  PRIVACY: '/profile/privacy',
  DATA_MANAGEMENT: '/profile/data',
  CONSENT_MANAGEMENT: '/profile/consent',

  // OAuth2 Management
  OAUTH2_APPS: '/profile/oauth2-apps',
  OAUTH2_AUTHORIZE_UI: '/oauth2/authorize',

  // Settings
  ACCOUNT_SETTINGS: '/profile/settings',
  SECURITY_SETTINGS: '/profile/security'
} as const;

/**
 * Test user accounts with different persona configurations
 */
export const TEST_USERS = {
  // Standard user with work/personal personas
  MULTI_PERSONA_USER: {
    email: 'test.personas@focushive.test',
    password: 'TestPassword123!',
    displayName: 'Multi Persona User',
    personas: ['work', 'personal', 'study']
  },

  // Privacy-focused user
  PRIVACY_USER: {
    email: 'privacy.user@focushive.test',
    password: 'PrivacyFirst123!',
    displayName: 'Privacy Focused User',
    personas: ['private']
  },

  // OAuth2 client developer
  OAUTH_DEV_USER: {
    email: 'oauth.dev@focushive.test',
    password: 'OAuthDev123!',
    displayName: 'OAuth2 Developer',
    personas: ['developer']
  },

  // Enterprise user with complex requirements
  ENTERPRISE_USER: {
    email: 'enterprise.user@focushive.test',
    password: 'Enterprise123!',
    displayName: 'Enterprise User',
    personas: ['work', 'personal', 'admin', 'guest']
  }
} as const;

/**
 * Persona templates for testing
 */
export const PERSONA_TEMPLATES = {
  WORK: {
    name: 'Work',
    type: 'PROFESSIONAL',
    displayName: 'Professional Me',
    bio: 'Focused on productivity and collaboration',
    privacyLevel: 'COLLEAGUES',
    notificationPreferences: {
      email: true,
      push: true,
      desktop: true,
      marketing: false
    }
  },
  PERSONAL: {
    name: 'Personal',
    type: 'PERSONAL',
    displayName: 'Personal Me',
    bio: 'Relaxed and creative',
    privacyLevel: 'FRIENDS',
    notificationPreferences: {
      email: true,
      push: false,
      desktop: false,
      marketing: true
    }
  },
  STUDY: {
    name: 'Study',
    type: 'ACADEMIC',
    displayName: 'Student Me',
    bio: 'Learning and growing',
    privacyLevel: 'STUDY_GROUPS',
    notificationPreferences: {
      email: true,
      push: true,
      desktop: true,
      marketing: false
    }
  },
  GAMING: {
    name: 'Gaming',
    type: 'SOCIAL',
    displayName: 'Gamer Me',
    bio: 'Ready to play and compete',
    privacyLevel: 'PUBLIC',
    notificationPreferences: {
      email: false,
      push: true,
      desktop: true,
      marketing: true
    }
  }
} as const;

/**
 * OAuth2 test clients
 */
export const OAUTH2_TEST_CLIENTS = {
  WEB_APP: {
    clientId: 'test-web-app',
    clientSecret: 'test-web-secret-123',
    name: 'Test Web Application',
    redirectUris: ['http://localhost:3000/callback', 'http://localhost:3001/callback'],
    scopes: ['profile', 'email', 'personas.read', 'personas.write'],
    grantTypes: ['authorization_code', 'refresh_token']
  },
  MOBILE_APP: {
    clientId: 'test-mobile-app',
    clientSecret: null, // Public client
    name: 'Test Mobile Application',
    redirectUris: ['com.focushive.test://callback'],
    scopes: ['profile', 'email', 'personas.read'],
    grantTypes: ['authorization_code', 'refresh_token'],
    pkce: true
  },
  THIRD_PARTY: {
    clientId: 'third-party-app',
    clientSecret: 'third-party-secret-456',
    name: 'Third Party Integration',
    redirectUris: ['https://thirdparty.example.com/callback'],
    scopes: ['profile'],
    grantTypes: ['authorization_code']
  }
} as const;

/**
 * Test timeouts and performance expectations
 */
export const PERFORMANCE_THRESHOLDS = {
  // API response time expectations (ms)
  API_RESPONSE_TIME: {
    FAST: 200,      // Authentication, simple gets
    MEDIUM: 500,    // Persona operations, data queries
    SLOW: 2000,     // Data export, complex operations
    TIMEOUT: 10000  // Maximum allowed response time
  },

  // UI interaction expectations (ms)
  UI_RESPONSE_TIME: {
    NAVIGATION: 1000,
    FORM_VALIDATION: 300,
    DATA_LOAD: 2000,
    PERSONA_SWITCH: 500
  },

  // Concurrent operation limits
  CONCURRENT_LIMITS: {
    SESSIONS: 5,      // Max concurrent persona sessions
    OAUTH_CLIENTS: 10, // Max OAuth2 clients per user
    PERSONAS: 10       // Max personas per user
  }
} as const;

/**
 * Accessibility test configuration
 */
export const ACCESSIBILITY_CONFIG = {
  // WCAG 2.1 AA compliance rules
  WCAG_RULES: {
    level: 'AA',
    version: '2.1',
    tags: ['wcag2a', 'wcag2aa', 'wcag21aa']
  },

  // Keyboard navigation requirements
  KEYBOARD_NAV: {
    tabOrder: true,
    escapeKey: true,
    enterKey: true,
    arrowKeys: true
  },

  // Screen reader requirements
  SCREEN_READER: {
    altText: true,
    ariaLabels: true,
    headingStructure: true,
    skipLinks: true
  }
} as const;

/**
 * Security test configuration
 */
export const SECURITY_CONFIG = {
  // JWT token validation
  JWT: {
    algorithm: 'RS256',
    expirationBuffer: 300, // 5 minutes
    refreshThreshold: 900  // 15 minutes
  },

  // OAuth2 security requirements
  OAUTH2: {
    stateParam: true,
    pkce: true,
    secureRedirect: true,
    scopeValidation: true
  },

  // Privacy protection
  PRIVACY: {
    dataEncryption: true,
    consentTracking: true,
    rightToDelete: true,
    dataPortability: true
  }
} as const;

/**
 * Browser and device configurations
 */
export const BROWSER_CONFIG = {
  DESKTOP_BROWSERS: [
    {name: 'chromium', viewport: {width: 1920, height: 1080}},
    {name: 'firefox', viewport: {width: 1920, height: 1080}},
    {name: 'webkit', viewport: {width: 1920, height: 1080}}
  ],

  MOBILE_DEVICES: [
    {name: 'iPhone 12', viewport: {width: 390, height: 844}},
    {name: 'Pixel 5', viewport: {width: 393, height: 851}},
    {name: 'iPad Air', viewport: {width: 820, height: 1180}}
  ],

  // Responsive breakpoints to test
  BREAKPOINTS: [
    {name: 'mobile', width: 375},
    {name: 'tablet', width: 768},
    {name: 'desktop', width: 1024},
    {name: 'wide', width: 1440}
  ]
} as const;

/**
 * Test data cleanup configuration
 */
export const CLEANUP_CONFIG = {
  // Automatic cleanup after tests
  AUTO_CLEANUP: true,

  // Data retention for debugging
  RETAIN_ON_FAILURE: true,

  // Cleanup timeout
  CLEANUP_TIMEOUT: 30000,

  // Resources to cleanup
  RESOURCES: [
    'test_users',
    'test_personas',
    'test_oauth_clients',
    'test_sessions',
    'test_export_requests'
  ]
} as const;