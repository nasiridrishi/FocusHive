/**
 * Environment Hooks
 *
 * Custom hooks for accessing validated environment variables
 * throughout the application.
 */

import {createContext, useContext} from 'react';
import type {EnvValidationError, ValidatedEnv} from '../services/validation/envValidation';

// Environment Context
export interface EnvironmentContextValue {
  env: ValidatedEnv;
  isValid: boolean;
  errors: EnvValidationError[];
  isLoading: boolean;
  revalidate: () => void;
}

export const EnvironmentContext = createContext<EnvironmentContextValue | null>(null);

/**
 * Hook to access validated environment variables
 */
export const useEnvironment = (): ValidatedEnv => {
  const context = useContext(EnvironmentContext);

  if (!context) {
    throw new Error('useEnvironment must be used within an EnvironmentProvider');
  }

  if (!context.isValid) {
    throw new Error('Environment is not valid. This should not happen.');
  }

  return context.env;
};

/**
 * Hook to access full environment context (for debugging/development)
 */
export const useEnvironmentContext = (): EnvironmentContextValue => {
  const context = useContext(EnvironmentContext);

  if (!context) {
    throw new Error('useEnvironmentContext must be used within an EnvironmentProvider');
  }

  return context;
};

/**
 * Hook to check if specific environment variable is available
 */
export const useEnvironmentVariable = <K extends keyof ValidatedEnv>(
    key: K
): ValidatedEnv[K] => {
  const env = useEnvironment();
  return env[key];
};

/**
 * Hook to check environment mode
 */
export const useEnvironmentMode = () => {
  const env = useEnvironment();

  return {
    mode: env.MODE,
    isDevelopment: env.DEV,
    isProduction: env.PROD,
    isSSR: env.SSR,
    baseUrl: env.BASE_URL
  };
};

/**
 * Hook to get API configuration
 */
export const useApiConfig = () => {
  const env = useEnvironment();

  return {
    apiBaseUrl: env.VITE_API_BASE_URL,
    websocketUrl: env.VITE_WEBSOCKET_URL,
    musicApiBaseUrl: env.VITE_MUSIC_API_BASE_URL || env.VITE_API_BASE_URL,
    musicServiceUrl: env.VITE_MUSIC_SERVICE_URL
  };
};

/**
 * Hook to get WebSocket configuration
 */
export const useWebSocketConfig = () => {
  const env = useEnvironment();

  return {
    url: env.VITE_WEBSOCKET_URL,
    reconnectAttempts: env.VITE_WEBSOCKET_RECONNECT_ATTEMPTS,
    reconnectDelay: env.VITE_WEBSOCKET_RECONNECT_DELAY,
    heartbeatInterval: env.VITE_WEBSOCKET_HEARTBEAT_INTERVAL
  };
};

/**
 * Hook to get Spotify configuration (if available)
 */
export const useSpotifyConfig = () => {
  const env = useEnvironment();

  return {
    clientId: env.VITE_SPOTIFY_CLIENT_ID,
    redirectUri: env.VITE_SPOTIFY_REDIRECT_URI,
    isEnabled: !!env.VITE_SPOTIFY_CLIENT_ID
  };
};

/**
 * Hook to get error logging configuration (if available)
 */
export const useErrorLoggingConfig = () => {
  const env = useEnvironment();

  return {
    endpoint: env.VITE_ERROR_LOGGING_ENDPOINT,
    apiKey: env.VITE_ERROR_LOGGING_API_KEY,
    isEnabled: !!env.VITE_ERROR_LOGGING_ENDPOINT
  };
};