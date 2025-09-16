/**
 * Authentication Contracts
 * Core interfaces for authentication and authorization in FocusHive
 *
 * This module defines all authentication-related types used across the application.
 * All interfaces are designed to integrate with the Identity Service at port 8081.
 */

/**
 * Account status types
 */
export type AccountStatus = 'active' | 'inactive' | 'suspended' | 'pending' | 'deleted';

/**
 * Persona types for different work/study contexts
 */
export type PersonaType = 'work' | 'study' | 'personal' | 'custom';

/**
 * User profile information
 */
export interface UserProfile {
  bio?: string;
  avatarUrl?: string;
  timezone?: string;
  language?: string;
  phoneNumber?: string;
  location?: string;
  website?: string;
  socialLinks?: {
    linkedin?: string;
    github?: string;
    twitter?: string;
  };
}

/**
 * User preferences and settings
 */
export interface UserPreferences {
  theme?: 'light' | 'dark' | 'system';
  language?: string;
  timezone?: string;
  notifications?: {
    email?: boolean;
    push?: boolean;
    sms?: boolean;
    inApp?: boolean;
  };
  privacy?: {
    profileVisibility?: 'public' | 'private' | 'friends';
    showOnlineStatus?: boolean;
    allowMessages?: boolean;
  };
  accessibility?: {
    highContrast?: boolean;
    fontSize?: 'small' | 'medium' | 'large';
    reducedMotion?: boolean;
  };
}

/**
 * Persona - Different identities/contexts for the same user
 */
export interface Persona {
  id: string;
  type: PersonaType;
  name: string;
  description?: string;
  isActive: boolean;
  isDefault?: boolean;
  createdAt: string;
  updatedAt: string;
  settings?: {
    theme?: string;
    notifications?: boolean;
    privacy?: string;
    [key: string]: any;
  };
  preferences?: UserPreferences;
}

/**
 * Main User interface
 */
export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  username?: string;
  createdAt: string;
  updatedAt: string;
  emailVerified: boolean;
  phoneVerified?: boolean;
  accountStatus: AccountStatus;
  lastLoginAt?: string;
  persona?: Persona;
  personas?: Persona[];
  profile?: UserProfile;
  preferences?: UserPreferences;
  roles?: string[];
  permissions?: string[];
}

/**
 * Authentication tokens
 */
export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number; // in milliseconds
  tokenType: string; // Usually 'Bearer'
  scope: string;
  issuedAt?: number; // timestamp
}

/**
 * Login request payload
 */
export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
  personaId?: string; // Optional: login with specific persona
  deviceInfo?: {
    deviceId?: string;
    deviceType?: string;
    userAgent?: string;
    ipAddress?: string;
  };
}

/**
 * Registration request payload
 */
export interface RegisterRequest {
  email: string;
  password: string;
  confirmPassword: string;
  firstName: string;
  lastName: string;
  username?: string;
  phoneNumber?: string;
  acceptTerms: boolean;
  marketingConsent?: boolean;
  timezone?: string;
  language?: string;
  referralCode?: string;
}

/**
 * Authentication response
 */
export interface AuthResponse {
  user: User;
  tokens: AuthTokens;
  sessionId?: string;
  deviceId?: string;
  requiresTwoFactor?: boolean;
  requiresEmailVerification?: boolean;
}

/**
 * Refresh token request
 */
export interface RefreshTokenRequest {
  refreshToken: string;
  grantType: 'refresh_token';
}

/**
 * Refresh token response
 */
export interface RefreshTokenResponse extends AuthTokens {
  // Extends AuthTokens, same structure
}

/**
 * Auth error codes
 */
export type AuthErrorCode =
  | 'INVALID_CREDENTIALS'
  | 'TOKEN_EXPIRED'
  | 'TOKEN_INVALID'
  | 'REFRESH_TOKEN_EXPIRED'
  | 'ACCOUNT_LOCKED'
  | 'ACCOUNT_DISABLED'
  | 'EMAIL_NOT_VERIFIED'
  | 'INVALID_PERSONA'
  | 'SESSION_EXPIRED'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'RATE_LIMIT_EXCEEDED'
  | 'TWO_FACTOR_REQUIRED'
  | 'TWO_FACTOR_FAILED'
  | 'PASSWORD_RESET_REQUIRED'
  | 'LOGIN_FAILED'
  | 'REGISTRATION_FAILED'
  | 'LOGOUT_FAILED'
  | 'TOKEN_REFRESH_FAILED'
  | 'PROFILE_UPDATE_FAILED'
  | 'PREFERENCES_UPDATE_FAILED'
  | 'PERSONA_SWITCH_FAILED'
  | 'VALIDATION_ERROR'
  | 'NETWORK_ERROR'
  | 'USER_NOT_FOUND'
  | 'EMAIL_IN_USE'
  | 'WEAK_PASSWORD'
  | 'UNKNOWN_ERROR';

/**
 * Authentication error structure
 */
export interface AuthError {
  code: AuthErrorCode;
  message: string;
  timestamp?: string;
  path?: string;
  details?: Record<string, any>;
  statusCode?: number;
  traceId?: string;
  retryAfter?: number;
}

/**
 * Token validation result
 */
export interface TokenValidation {
  valid: boolean;
  expired: boolean;
  userId?: string;
  email?: string;
  scope?: string[];
  personaId?: string;
  issuedAt?: number;
  expiresAt?: number;
  remainingTime?: number;
}

/**
 * Password reset request
 */
export interface PasswordResetRequest {
  email: string;
  redirectUrl?: string;
  language?: string;
}

/**
 * Password reset confirmation
 */
export interface PasswordResetConfirmation {
  token: string;
  newPassword: string;
  confirmPassword: string;
}

/**
 * Email verification request
 */
export interface EmailVerificationRequest {
  token: string;
  email?: string;
}

/**
 * Email verification response
 */
export interface EmailVerificationResponse {
  success: boolean;
  message: string;
  user?: User;
}

/**
 * Two-factor authentication request
 */
export interface TwoFactorAuthRequest {
  userId: string;
  code: string;
  trustDevice?: boolean;
}

/**
 * Two-factor authentication setup
 */
export interface TwoFactorSetup {
  secret: string;
  qrCode: string;
  backupCodes: string[];
}

/**
 * Session information
 */
export interface Session {
  id: string;
  userId: string;
  deviceId?: string;
  ipAddress?: string;
  userAgent?: string;
  createdAt: string;
  lastActivityAt: string;
  expiresAt: string;
  isActive: boolean;
  location?: {
    country?: string;
    city?: string;
    region?: string;
  };
}

/**
 * OAuth provider types
 */
export type OAuthProvider = 'google' | 'github' | 'microsoft' | 'spotify';

/**
 * OAuth login request
 */
export interface OAuthLoginRequest {
  provider: OAuthProvider;
  redirectUrl?: string;
  scope?: string[];
  state?: string;
}

/**
 * OAuth callback response
 */
export interface OAuthCallbackResponse {
  provider: OAuthProvider;
  code: string;
  state?: string;
}

/**
 * User permissions and roles
 */
export interface Permission {
  id: string;
  name: string;
  resource: string;
  action: string;
  description?: string;
}

export interface Role {
  id: string;
  name: string;
  description?: string;
  permissions: Permission[];
  isSystem?: boolean;
}

/**
 * Update profile request
 */
export type UpdateProfileRequest = Partial<UserProfile>;

/**
 * Update preferences request
 */
export type UpdatePreferencesRequest = Partial<UserPreferences>;

/**
 * Auth state for context/hooks
 */
export interface AuthState {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: User | null;
  tokens: AuthTokens | null;
  error: AuthError | null;
  sessionId: string | null;
  lastActivity: number | null;
}

/**
 * Extended auth context state
 */
export interface AuthContextState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: AuthError | null;
  sessionExpiresAt: Date | null;
  lastActivity: Date | null;
}

/**
 * Auth context methods
 */
export interface AuthContextMethods {
  login: (request: LoginRequest) => Promise<AuthResponse>;
  register: (request: RegisterRequest) => Promise<AuthResponse>;
  logout: () => Promise<void>;
  refreshToken: () => Promise<void>;
  checkSession: () => Promise<boolean>;
  updateProfile: (profile: UpdateProfileRequest) => Promise<User>;
  updatePreferences: (preferences: UpdatePreferencesRequest) => Promise<User>;
  switchPersona: (personaId: string) => Promise<void>;
  clearError: () => void;
}

/**
 * Complete Auth context type
 */
export interface AuthContextType extends AuthContextState, AuthContextMethods {}

/**
 * Token storage interface
 */
export interface TokenStorage {
  getAccessToken: () => string | null;
  getRefreshToken: () => string | null;
  setTokens: (tokens: AuthTokens) => void;
  clearTokens: () => void;
  getTokenExpiry: () => number | null;
  isTokenExpired: () => boolean;
}

/**
 * Auth service configuration
 */
export interface AuthConfig {
  baseUrl: string;
  tokenRefreshBuffer: number; // Time before expiry to refresh (ms)
  sessionTimeout: number; // Idle timeout (ms)
  rememberMeDuration: number; // Remember me duration (ms)
  maxLoginAttempts: number;
  lockoutDuration: number; // Account lockout duration (ms)
  passwordMinLength: number;
  passwordRequireSpecialChar: boolean;
  passwordRequireNumber: boolean;
  passwordRequireUpperCase: boolean;
  enableTwoFactor: boolean;
  enableOAuth: boolean;
  oauthProviders: OAuthProvider[];
}