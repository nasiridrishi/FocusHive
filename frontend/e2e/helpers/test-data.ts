/**
 * Test data constants and utilities for E2E tests
 */

export const TEST_URLS = {
  HOME: '/',
  LOGIN: '/login',
  REGISTER: '/register',
  DASHBOARD: '/dashboard',
} as const;

export const API_ENDPOINTS = {
  IDENTITY_BASE: process.env.E2E_IDENTITY_API_URL || 'http://localhost:8081',
  BACKEND_BASE: process.env.E2E_API_BASE_URL || 'http://localhost:8080',
  LOGIN: '/api/v1/auth/login',
  REGISTER: '/api/v1/auth/register',
  LOGOUT: '/api/v1/auth/logout',
  ME: '/api/v1/auth/me',
} as const;

export const TEST_USERS = {
  VALID_USER: {
    username: 'e2e_test_user',
    email: 'e2e.test@focushive.com',
    password: 'TestPassword123!',
    firstName: 'E2E',
    lastName: 'TestUser',
  },
  NEW_USER: {
    username: 'e2e_new_user',
    email: 'e2e.new@focushive.com',
    password: 'NewPassword123!',
    firstName: 'New',
    lastName: 'User',
  },
  INVALID_USER: {
    username: 'invalid_user',
    email: 'invalid@example.com',
    password: 'wrongpassword',
  },
} as const;

export const FORM_VALIDATION = {
  REQUIRED_FIELD_ERROR: 'This field is required',
  INVALID_EMAIL_ERROR: 'Please enter a valid email address',
  PASSWORD_MIN_LENGTH: 'Password must be at least 8 characters',
  PASSWORD_REQUIREMENTS: 'Password must contain uppercase, lowercase, number and special character',
  USERNAME_MIN_LENGTH: 'Username must be at least 3 characters',
} as const;

export const SELECTORS = {
  // Login form - updated to match actual form structure
  LOGIN_USERNAME_INPUT: '#email, input[name="email"]',
  LOGIN_PASSWORD_INPUT: '#password, input[name="password"]',
  LOGIN_SUBMIT_BUTTON: 'button[type="submit"]:has-text("Sign In"), button[type="submit"]:has-text("Signing in")',

  // Register form  
  REGISTER_USERNAME_INPUT: 'input[name="username"]',
  REGISTER_EMAIL_INPUT: 'input[name="email"]',
  REGISTER_PASSWORD_INPUT: 'input[name="password"]',
  REGISTER_FIRST_NAME_INPUT: 'input[name="firstName"]',
  REGISTER_LAST_NAME_INPUT: 'input[name="lastName"]',
  REGISTER_SUBMIT_BUTTON: 'button[type="submit"]:has-text("Create Account"), button[type="submit"]:has-text("Register")',

  // Navigation
  LOGIN_LINK: 'a[href="/login"], button:has-text("Sign in"), :text("Sign in here")',
  REGISTER_LINK: 'a[href="/register"], button:has-text("Sign up"), :text("Sign up")',

  // User menu and logout
  USER_MENU: '[data-testid="user-menu"], .user-avatar, [aria-label="Account"]',
  LOGOUT_BUTTON: 'button:has-text("Logout"), [data-testid="logout-button"], button:has-text("Sign out")',

  // Error messages
  ERROR_MESSAGE: '[role="alert"], .error, .MuiFormHelperText-error, .error-message',
  SUCCESS_MESSAGE: '.success, .MuiAlert-standardSuccess, [role="status"]',

  // Loading states
  LOADING_SPINNER: '[data-testid="loading"], .loading, .MuiCircularProgress-root',

} as const;

export const TIMEOUTS = {
  SHORT: 2000,
  MEDIUM: 5000,
  LONG: 10000,
  NETWORK: 15000,
  PAGE_LOAD: 30000,
} as const;

export const PERFORMANCE_THRESHOLDS = {
  DASHBOARD_LOAD_TIME: 3000, // 3 seconds max for dashboard load
  API_RESPONSE_TIME: 2000, // 2 seconds max for API responses
  CHART_RENDER_TIME: 1500, // 1.5 seconds max for chart rendering
  FIRST_CONTENTFUL_PAINT: 1500, // 1.5 seconds for FCP
  LARGEST_CONTENTFUL_PAINT: 2500, // 2.5 seconds for LCP
} as const;

/**
 * Generate unique test data to avoid conflicts between parallel tests
 */
export function generateUniqueUser(baseUser: typeof TEST_USERS.VALID_USER): void {
  const timestamp = Date.now();
  const randomId = Math.random().toString(36).substring(7);

  return {
    ...baseUser,
    username: `${baseUser.username}_${timestamp}_${randomId}`,
    email: `${timestamp}_${randomId}_${baseUser.email}`,
  };
}

/**
 * Test environment validation
 */
export function validateTestEnvironment(): void {
  const identityUrl = process.env.E2E_IDENTITY_API_URL || 'http://localhost:8081';
  const backendUrl = process.env.E2E_API_BASE_URL || 'http://localhost:8080';

  return {
    identityServiceUrl: identityUrl,
    backendServiceUrl: backendUrl,
    frontendUrl: process.env.E2E_BASE_URL || 'http://127.0.0.1:5173',
  };
}