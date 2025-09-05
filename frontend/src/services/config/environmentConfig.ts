/**
 * Environment Configuration Service
 * 
 * Centralized service for accessing validated environment configuration.
 * This service should be used instead of accessing import.meta.env directly.
 * 
 * Usage:
 * import { getApiConfig, getWebSocketConfig } from '@/services/config/environmentConfig'
 * 
 * const apiConfig = getApiConfig()
 * const wsConfig = getWebSocketConfig()
 */

import { env } from '../validation/envValidation';
import type { ValidatedEnv } from '../validation/envValidation';

/**
 * Get the validated environment configuration
 */
export function getEnvironment(): ValidatedEnv {
  return env();
}

/**
 * Get API configuration
 */
export function getApiConfig() {
  const environment = env();
  
  return {
    baseUrl: environment.VITE_API_BASE_URL,
    musicApiBaseUrl: environment.VITE_MUSIC_API_BASE_URL || environment.VITE_API_BASE_URL,
    timeout: 30000, // 30 seconds default
  };
}

/**
 * Get WebSocket configuration
 */
export function getWebSocketConfig() {
  const environment = env();
  
  return {
    url: environment.VITE_WEBSOCKET_URL,
    reconnectAttempts: environment.VITE_WEBSOCKET_RECONNECT_ATTEMPTS,
    reconnectDelay: environment.VITE_WEBSOCKET_RECONNECT_DELAY,
    heartbeatInterval: environment.VITE_WEBSOCKET_HEARTBEAT_INTERVAL,
  };
}

/**
 * Get Music Service configuration
 */
export function getMusicConfig() {
  const environment = env();
  
  return {
    serviceUrl: environment.VITE_MUSIC_SERVICE_URL || 'http://localhost:8084',
    apiBaseUrl: environment.VITE_MUSIC_API_BASE_URL || environment.VITE_API_BASE_URL,
  };
}

/**
 * Get Spotify configuration
 */
export function getSpotifyConfig() {
  const environment = env();
  
  return {
    clientId: environment.VITE_SPOTIFY_CLIENT_ID,
    redirectUri: environment.VITE_SPOTIFY_REDIRECT_URI || `${window.location.origin}/music/spotify/callback`,
    isEnabled: !!environment.VITE_SPOTIFY_CLIENT_ID,
  };
}

/**
 * Get Error Logging configuration
 */
export function getErrorLoggingConfig() {
  const environment = env();
  
  return {
    endpoint: environment.VITE_ERROR_LOGGING_ENDPOINT,
    apiKey: environment.VITE_ERROR_LOGGING_API_KEY,
    enabled: environment.PROD && !!environment.VITE_ERROR_LOGGING_ENDPOINT,
    enableRemoteLogging: environment.PROD,
  };
}

/**
 * Get environment mode information
 */
export function getEnvironmentMode() {
  const environment = env();
  
  return {
    mode: environment.MODE,
    isDevelopment: environment.DEV,
    isProduction: environment.PROD,
    isSSR: environment.SSR,
    baseUrl: environment.BASE_URL,
  };
}

/**
 * Check if we're in development mode
 */
export function isDevelopment(): boolean {
  return env().DEV;
}

/**
 * Check if we're in production mode
 */
export function isProduction(): boolean {
  return env().PROD;
}

/**
 * Check if a feature is enabled based on environment
 */
export function isFeatureEnabled(feature: string): boolean {
  const environment = env();
  
  switch (feature) {
    case 'spotify':
      return !!environment.VITE_SPOTIFY_CLIENT_ID;
    case 'errorLogging':
      return environment.PROD && !!environment.VITE_ERROR_LOGGING_ENDPOINT;
    case 'musicService':
      return !!environment.VITE_MUSIC_SERVICE_URL || !!environment.VITE_MUSIC_API_BASE_URL;
    default:
      return false;
  }
}

// Export default configuration getters
export default {
  getEnvironment,
  getApiConfig,
  getWebSocketConfig,
  getMusicConfig,
  getSpotifyConfig,
  getErrorLoggingConfig,
  getEnvironmentMode,
  isDevelopment,
  isProduction,
  isFeatureEnabled,
};