/**
 * Test Constants and Fixtures
 * Centralized constants for E2E tests
 */

export const TEST_URLS = {
  HOME: '/',
  LOGIN: '/login',
  REGISTER: '/register',
  DASHBOARD: '/dashboard',
  PROFILE: '/profile',
  SETTINGS: '/settings',
  APP: '/app',
} as const;

export const TEST_SELECTORS = {
  // Auth selectors
  EMAIL_INPUT: 'input[name="email"], input[type="email"]',
  PASSWORD_INPUT: 'input[name="password"], input[type="password"]',
  LOGIN_BUTTON: 'button[type="submit"]:has-text("Sign In"), button:has-text("Login")',
  REGISTER_BUTTON: 'button[type="submit"]:has-text("Create"), button[type="submit"]:has-text("Register")',
  
  // Form selectors
  ERROR_MESSAGE: '[role="alert"], .error, .error-message, .MuiAlert-message',
  SUCCESS_MESSAGE: '.success, .success-message, .MuiAlert-message',
  
  // Navigation selectors
  GET_STARTED_BUTTON: 'button:has-text("Get Started"), a:has-text("Get Started")',
  SIGNUP_LINK: 'a[href="/register"], button:has-text("Sign up")',
  LOGIN_LINK: 'a[href="/login"], button:has-text("Sign In")',
  
  // Common elements
  MAIN_HEADING: 'h1',
  MAIN_CONTENT: 'main, .main-content, [role="main"]',
  NAVIGATION: 'nav, header nav, .navigation',
  
} as const;

export const TEST_TIMEOUTS = {
  SHORT: 2000,
  MEDIUM: 5000,
  LONG: 10000,
  PAGE_LOAD: 15000,
} as const;

export const TEST_VIEWPORTS = {
  MOBILE: { width: 375, height: 667 },
  TABLET: { width: 768, height: 1024 },
  DESKTOP: { width: 1280, height: 720 },
  DESKTOP_LARGE: { width: 1920, height: 1080 },
} as const;

export const TEST_USER_DEFAULTS = {
  FIRST_NAME: 'Test',
  LAST_NAME: 'User',
  PASSWORD: 'TestPassword123!',
  DOMAIN: 'focushive.test',
} as const;

export const TEST_VALIDATION_MESSAGES = {
  REQUIRED_FIELD: 'This field is required',
  INVALID_EMAIL: 'Please enter a valid email',
  PASSWORD_TOO_SHORT: 'Password must be at least',
  PASSWORDS_DONT_MATCH: 'Passwords do not match',
  INVALID_CREDENTIALS: 'Invalid credentials',
  EMAIL_ALREADY_EXISTS: 'Email already exists',
  USERNAME_ALREADY_EXISTS: 'Username already exists',
} as const;