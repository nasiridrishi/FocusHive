/**
 * TypeScript Environment Variable Declarations for FocusHive Frontend
 *
 * This file provides TypeScript type definitions for all VITE_* environment
 * variables used in the FocusHive application. It ensures type safety when
 * accessing environment variables through import.meta.env.
 *
 * All variables are declared as optional (string | undefined) to match
 * the runtime behavior where environment variables may not be set.
 */

/// <reference types="vite/client" />

interface ImportMetaEnv {
  // ========================================
  // REQUIRED ENVIRONMENT VARIABLES
  // ========================================

  /**
   * Backend API base URL
   * @example "http://localhost:8080" | "https://api.focushive.com"
   */
  readonly VITE_API_URL: string | undefined;

  /**
   * WebSocket connection URL for real-time communication
   * @example "ws://localhost:8080" | "wss://api.focushive.com"
   */
  readonly VITE_WS_URL: string | undefined;

  /**
   * Authentication service domain
   * @example "localhost:8081" | "auth.focushive.com"
   */
  readonly VITE_AUTH_DOMAIN: string | undefined;

  /**
   * Spotify Web API Client ID for music integration
   * @example "abc123def456ghi789"
   */
  readonly VITE_SPOTIFY_CLIENT_ID: string | undefined;

  /**
   * Application environment
   * @example "development" | "staging" | "production"
   */
  readonly VITE_ENV: string | undefined;

  // ========================================
  // OPTIONAL CONFIGURATION VARIABLES
  // ========================================

  /**
   * API request timeout in milliseconds
   * @default 30000
   * @example "5000" | "60000"
   */
  readonly VITE_API_TIMEOUT: string | undefined;

  /**
   * Maximum number of API retry attempts
   * @default 3
   * @example "1" | "5"
   */
  readonly VITE_MAX_RETRIES: string | undefined;

  /**
   * Enable debug logging
   * @default false
   * @example "true" | "false"
   */
  readonly VITE_DEBUG: string | undefined;

  // ========================================
  // WEBSOCKET CONFIGURATION
  // ========================================

  /**
   * WebSocket reconnection attempts
   * @default 10
   * @example "5" | "20"
   */
  readonly VITE_WEBSOCKET_RECONNECT_ATTEMPTS: string | undefined;

  /**
   * WebSocket reconnection delay in milliseconds
   * @default 1000
   * @example "500" | "2000"
   */
  readonly VITE_WEBSOCKET_RECONNECT_DELAY: string | undefined;

  /**
   * WebSocket heartbeat interval in milliseconds
   * @default 30000
   * @example "15000" | "60000"
   */
  readonly VITE_WEBSOCKET_HEARTBEAT_INTERVAL: string | undefined;

  // ========================================
  // MUSIC SERVICE CONFIGURATION
  // ========================================

  /**
   * Music service URL
   * @example "http://localhost:8084" | "https://music.focushive.com"
   */
  readonly VITE_MUSIC_SERVICE_URL: string | undefined;

  /**
   * Music API base URL (fallback to main API if not provided)
   * @example "http://localhost:8080" | "https://api.focushive.com"
   */
  readonly VITE_MUSIC_API_BASE_URL: string | undefined;

  /**
   * Spotify OAuth redirect URI
   * @example "http://localhost:3000/music/spotify/callback"
   */
  readonly VITE_SPOTIFY_REDIRECT_URI: string | undefined;

  // ========================================
  // ERROR LOGGING CONFIGURATION
  // ========================================

  /**
   * Remote error logging service endpoint
   * @example "https://api.bugsnag.com" | "https://sentry.io/api"
   */
  readonly VITE_ERROR_LOGGING_ENDPOINT: string | undefined;

  /**
   * API key for error logging service
   * @example "abc123-def456-ghi789"
   */
  readonly VITE_ERROR_LOGGING_API_KEY: string | undefined;

  // ========================================
  // VITE BUILT-IN VARIABLES
  // ========================================

  /**
   * Vite mode (from --mode option or NODE_ENV)
   * @example "development" | "production"
   */
  readonly MODE: string;

  /**
   * Whether the app is running in development
   * @example true | false
   */
  readonly DEV: boolean;

  /**
   * Whether the app is running in production
   * @example true | false
   */
  readonly PROD: boolean;

  /**
   * Whether the app is being server-side rendered
   * @example true | false
   */
  readonly SSR: boolean;

  /**
   * Base URL for the application
   * @example "/" | "/app/"
   */
  readonly BASE_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

// ========================================
// UTILITY TYPES
// ========================================

/**
 * Type for validated environment configuration
 * This type represents the structure after validation with defaults applied
 */
export interface ValidatedEnvironmentConfig {
  // Required variables (guaranteed to be present after validation)
  VITE_API_URL: string;
  VITE_WS_URL: string;
  VITE_AUTH_DOMAIN: string;
  VITE_SPOTIFY_CLIENT_ID: string;
  VITE_ENV: 'development' | 'staging' | 'production';

  // Optional variables with defaults (guaranteed to have values after validation)
  VITE_API_TIMEOUT: number;
  VITE_MAX_RETRIES: number;
  VITE_DEBUG: boolean;
  VITE_WEBSOCKET_RECONNECT_ATTEMPTS: number;
  VITE_WEBSOCKET_RECONNECT_DELAY: number;
  VITE_WEBSOCKET_HEARTBEAT_INTERVAL: number;

  // Optional variables (may be undefined)
  VITE_MUSIC_SERVICE_URL?: string;
  VITE_MUSIC_API_BASE_URL?: string;
  VITE_SPOTIFY_REDIRECT_URI?: string;
  VITE_ERROR_LOGGING_ENDPOINT?: string;
  VITE_ERROR_LOGGING_API_KEY?: string;
}

/**
 * Environment variable validation status
 */
export type EnvironmentValidationStatus =
    | { status: 'valid'; config: ValidatedEnvironmentConfig }
    | { status: 'invalid'; errors: string[] };

/**
 * Environment validation error details
 */
export interface EnvironmentValidationError {
  variable: string;
  message: string;
  received?: unknown;
  expected?: string;
}

/**
 * Environment configuration health check result
 */
export interface EnvironmentHealthCheck {
  valid: boolean;
  environment: ValidatedEnvironmentConfig['VITE_ENV'];
  apiUrlReachable: boolean;
  wsUrlReachable: boolean;
  spotifyConfigured: boolean;
  musicServiceConfigured: boolean;
  errorLoggingConfigured: boolean;
  timestamp: string;
}

// ========================================
// GLOBAL WINDOW INTERFACE EXTENSION
// ========================================

/**
 * Extends the global Window interface to include FocusHive-specific properties
 */
declare global {
  interface Window {
    /**
     * Validated environment configuration available globally after startup validation
     */
    __FOCUSHIVE_ENV__?: ValidatedEnvironmentConfig;
  }
}