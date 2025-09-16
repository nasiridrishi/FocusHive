/**
 * Authentication test fixtures and test data
 * Comprehensive test data for all authentication scenarios
 */

export interface TestUser {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface OAuthProvider {
  name: string;
  mockUrl: string;
  clientId: string;
  scopes: string[];
}

/**
 * Valid test users for different scenarios
 */
export const AUTH_TEST_USERS = {
  // Primary test user
  VALID_USER: {
    username: 'auth_test_user',
    email: 'auth.test@focushive.com',
    password: 'SecurePass123!',
    firstName: 'Auth',
    lastName: 'TestUser',
  },

  // Secondary test user for multi-user scenarios
  VALID_USER_2: {
    username: 'auth_test_user_2',
    email: 'auth.test2@focushive.com',
    password: 'SecurePass456!',
    firstName: 'Auth2',
    lastName: 'TestUser2',
  },

  // Admin user for privilege testing
  ADMIN_USER: {
    username: 'auth_admin_user',
    email: 'admin.test@focushive.com',
    password: 'AdminPass123!',
    firstName: 'Admin',
    lastName: 'TestUser',
  },

  // New user for registration testing
  NEW_USER: {
    username: 'auth_new_user',
    email: 'new.test@focushive.com',
    password: 'NewUserPass123!',
    firstName: 'New',
    lastName: 'TestUser',
  },

  // User with existing account for OAuth linking
  OAUTH_LINK_USER: {
    username: 'oauth_link_user',
    email: 'oauth.link@focushive.com',
    password: 'OAuthLink123!',
    firstName: 'OAuth',
    lastName: 'LinkUser',
  },
} as const satisfies Record<string, TestUser>;

/**
 * Invalid credentials for negative testing
 */
export const INVALID_CREDENTIALS = {
  NON_EXISTENT_USER: {
    username: 'nonexistent@example.com',
    password: 'password123',
  },

  WRONG_PASSWORD: {
    username: AUTH_TEST_USERS.VALID_USER.username,
    password: 'wrongpassword',
  },

  MALFORMED_EMAIL: {
    username: 'not-an-email',
    password: 'password123',
  },

  WEAK_PASSWORD: {
    username: 'weakpass@example.com',
    password: '123',
  },

  EMPTY_CREDENTIALS: {
    username: '',
    password: '',
  },

  SQL_INJECTION: {
    username: "'; DROP TABLE users; --",
    password: "' OR '1'='1",
  },

  XSS_ATTEMPT: {
    username: '<script>alert("xss")</script>',
    password: '<img src=x onerror=alert(1)>',
  },
} as const;

/**
 * Registration form validation test cases
 */
export const REGISTRATION_VALIDATION_CASES = {
  MISSING_USERNAME: {
    username: '',
    email: 'test@example.com',
    password: 'Password123!',
    firstName: 'Test',
    lastName: 'User',
    expectedError: 'Username is required',
  },

  SHORT_USERNAME: {
    username: 'ab',
    email: 'test@example.com',
    password: 'Password123!',
    firstName: 'Test',
    lastName: 'User',
    expectedError: 'Username must be at least 3 characters',
  },

  INVALID_EMAIL: {
    username: 'testuser',
    email: 'not-an-email',
    password: 'Password123!',
    firstName: 'Test',
    lastName: 'User',
    expectedError: 'Please enter a valid email address',
  },

  WEAK_PASSWORD: {
    username: 'testuser',
    email: 'test@example.com',
    password: 'weak',
    firstName: 'Test',
    lastName: 'User',
    expectedError: 'Password must be at least 8 characters',
  },

  PASSWORD_NO_UPPERCASE: {
    username: 'testuser',
    email: 'test@example.com',
    password: 'lowercase123!',
    firstName: 'Test',
    lastName: 'User',
    expectedError: 'Password must contain uppercase, lowercase, number and special character',
  },

  PASSWORD_NO_SPECIAL: {
    username: 'testuser',
    email: 'test@example.com',
    password: 'Password123',
    firstName: 'Test',
    lastName: 'User',
    expectedError: 'Password must contain uppercase, lowercase, number and special character',
  },

  MISSING_FIRST_NAME: {
    username: 'testuser',
    email: 'test@example.com',
    password: 'Password123!',
    firstName: '',
    lastName: 'User',
    expectedError: 'First name is required',
  },

  MISSING_LAST_NAME: {
    username: 'testuser',
    email: 'test@example.com',
    password: 'Password123!',
    firstName: 'Test',
    lastName: '',
    expectedError: 'Last name is required',
  },
} as const;

/**
 * OAuth provider configurations for testing
 */
export const OAUTH_PROVIDERS: Record<string, OAuthProvider> = {
  GOOGLE: {
    name: 'Google',
    mockUrl: '/auth/oauth2/google',
    clientId: 'test-google-client-id',
    scopes: ['openid', 'profile', 'email'],
  },

  GITHUB: {
    name: 'GitHub',
    mockUrl: '/auth/oauth2/github',
    clientId: 'test-github-client-id',
    scopes: ['user:email', 'read:user'],
  },

  MICROSOFT: {
    name: 'Microsoft',
    mockUrl: '/auth/oauth2/microsoft',
    clientId: 'test-microsoft-client-id',
    scopes: ['openid', 'profile', 'email'],
  },
} as const;

/**
 * Mock OAuth user profiles for testing
 */
export const MOCK_OAUTH_PROFILES = {
  GOOGLE: {
    id: 'google-user-123',
    email: 'oauth.google@example.com',
    name: 'Google Test User',
    given_name: 'Google',
    family_name: 'User',
    picture: 'https://example.com/avatar.jpg',
    verified_email: true,
  },

  GITHUB: {
    id: 456,
    login: 'github-test-user',
    email: 'oauth.github@example.com',
    name: 'GitHub Test User',
    avatar_url: 'https://example.com/github-avatar.jpg',
  },

  MICROSOFT: {
    id: 'microsoft-user-789',
    userPrincipalName: 'oauth.microsoft@example.com',
    displayName: 'Microsoft Test User',
    givenName: 'Microsoft',
    surname: 'User',
  },
} as const;

/**
 * Password reset test scenarios
 */
export const PASSWORD_RESET_SCENARIOS = {
  VALID_EMAIL: {
    email: AUTH_TEST_USERS.VALID_USER.email,
    newPassword: 'NewSecurePass123!',
  },

  NON_EXISTENT_EMAIL: {
    email: 'nonexistent@example.com',
    newPassword: 'NewPassword123!',
  },

  INVALID_EMAIL: {
    email: 'not-an-email',
    newPassword: 'NewPassword123!',
  },

  WEAK_NEW_PASSWORD: {
    email: AUTH_TEST_USERS.VALID_USER.email,
    newPassword: 'weak',
  },
} as const;

/**
 * Session management test scenarios
 */
export const SESSION_SCENARIOS = {
  REMEMBER_ME_ENABLED: {
    username: AUTH_TEST_USERS.VALID_USER.username,
    password: AUTH_TEST_USERS.VALID_USER.password,
    rememberMe: true,
    expectedTokenLocation: 'localStorage',
  },

  REMEMBER_ME_DISABLED: {
    username: AUTH_TEST_USERS.VALID_USER.username,
    password: AUTH_TEST_USERS.VALID_USER.password,
    rememberMe: false,
    expectedTokenLocation: 'sessionStorage',
  },

  CONCURRENT_SESSIONS: {
    users: [AUTH_TEST_USERS.VALID_USER, AUTH_TEST_USERS.VALID_USER_2],
    maxSessions: 3,
  },

  SESSION_TIMEOUT: {
    username: AUTH_TEST_USERS.VALID_USER.username,
    password: AUTH_TEST_USERS.VALID_USER.password,
    timeoutMinutes: 30,
  },
} as const;

/**
 * Account lockout scenarios
 */
export const LOCKOUT_SCENARIOS = {
  MAX_FAILED_ATTEMPTS: 5,
  LOCKOUT_DURATION_MINUTES: 15,
  PROGRESSIVE_DELAY: [1, 2, 4, 8, 15], // seconds
} as const;

/**
 * Email verification scenarios
 */
export const EMAIL_VERIFICATION = {
  VALID_TOKEN: 'valid-verification-token-123',
  EXPIRED_TOKEN: 'expired-verification-token-456',
  INVALID_TOKEN: 'invalid-verification-token-789',
  MALFORMED_TOKEN: 'malformed-token',
  TOKEN_EXPIRY_HOURS: 24,
} as const;

/**
 * MailHog API configuration for email testing
 */
export const MAILHOG_CONFIG = {
  API_BASE_URL: process.env.E2E_MAILHOG_API_URL || 'http://localhost:8025',
  WEB_URL: process.env.E2E_MAILHOG_WEB_URL || 'http://localhost:8025',
  DELETE_ENDPOINT: '/api/v1/messages',
  MESSAGES_ENDPOINT: '/api/v2/messages',
  SEARCH_ENDPOINT: '/api/v2/search',
} as const;

/**
 * Performance thresholds for authentication flows
 */
export const AUTH_PERFORMANCE_THRESHOLDS = {
  LOGIN_TIME_MS: 2000,
  REGISTRATION_TIME_MS: 3000,
  PASSWORD_RESET_TIME_MS: 2500,
  TOKEN_REFRESH_TIME_MS: 1000,
  LOGOUT_TIME_MS: 1000,
  OAUTH_FLOW_TIME_MS: 5000,
} as const;

/**
 * Mobile viewport configurations for responsive testing
 */
export const MOBILE_VIEWPORTS = {
  IPHONE_SE: {width: 375, height: 667},
  IPHONE_12: {width: 390, height: 844},
  IPHONE_12_PRO_MAX: {width: 428, height: 926},
  SAMSUNG_S21: {width: 360, height: 800},
  IPAD: {width: 768, height: 1024},
  IPAD_PRO: {width: 1024, height: 1366},
} as const;

/**
 * Accessibility test scenarios
 */
export const ACCESSIBILITY_SCENARIOS = {
  KEYBOARD_NAVIGATION: {
    tabSequence: [
      'username-input',
      'password-input',
      'remember-me-checkbox',
      'login-button',
      'register-link',
      'forgot-password-link',
    ],
  },

  SCREEN_READER: {
    requiredAriaLabels: [
      'Username or Email',
      'Password',
      'Sign In',
      'Remember me',
      'Create new account',
      'Forgot password?',
    ],
  },

  HIGH_CONTRAST: {
    elements: ['form', 'input', 'button', 'link', 'error-message'],
  },
} as const;

/**
 * Generate unique test user with timestamp
 */
export function generateUniqueAuthUser(baseUser: TestUser): TestUser {
  const timestamp = Date.now();
  const randomId = Math.random().toString(36).substring(2, 8);

  return {
    ...baseUser,
    username: `${baseUser.username}_${timestamp}_${randomId}`,
    email: `${timestamp}_${randomId}_${baseUser.email}`,
  };
}

/**
 * Generate test data for bulk operations
 */
export function generateTestUsers(count: number): TestUser[] {
  return Array.from({length: count}, (_, index) =>
      generateUniqueAuthUser({
        username: `bulk_test_user_${index}`,
        email: `bulk.test.${index}@focushive.com`,
        password: 'BulkTestPass123!',
        firstName: 'Bulk',
        lastName: `User${index}`,
      })
  );
}

/**
 * JWT token mock data for testing
 */
export const MOCK_JWT_TOKENS = {
  VALID_ACCESS_TOKEN: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c',
  VALID_REFRESH_TOKEN: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwidHlwZSI6InJlZnJlc2giLCJpYXQiOjE1MTYyMzkwMjJ9.L8i6g3PluJJUeccX2atkUjCiCmQIXNxMh-T8kKSRIpc',
  EXPIRED_ACCESS_TOKEN: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjN9.invalid-signature',
  INVALID_TOKEN: 'invalid.jwt.token',
  MALFORMED_TOKEN: 'not-a-jwt-token-at-all',
} as const;