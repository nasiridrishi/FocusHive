/**
 * Environment Provider Component
 * 
 * Validates environment variables at application startup and provides
 * validated environment configuration throughout the app.
 * 
 * Shows an error page if critical environment variables are missing,
 * and provides a React context for accessing validated environment variables.
 */

import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import {
  validateEnvironment,
  checkEnvironmentSetup,
  type ValidatedEnv,
  type EnvValidationError
} from '../services/validation/envValidation';
import EnvironmentError from '../components/EnvironmentError';
import { CircularProgress, Box, Typography } from '@mui/material';

// Environment Context
interface EnvironmentContextValue {
  env: ValidatedEnv;
  isValid: boolean;
  errors: EnvValidationError[];
  isLoading: boolean;
  revalidate: () => void;
}

const EnvironmentContext = createContext<EnvironmentContextValue | null>(null);

// Provider Props
interface EnvironmentProviderProps {
  children: ReactNode;
}

// Loading Component
const EnvironmentLoading: React.FC = () => (
  <Box
    display="flex"
    flexDirection="column"
    alignItems="center"
    justifyContent="center"
    minHeight="100vh"
    gap={2}
  >
    <CircularProgress size={48} />
    <Typography variant="h6" color="text.secondary">
      Validating Configuration...
    </Typography>
  </Box>
);

/**
 * Environment Provider Component
 * 
 * Validates environment variables and provides access to validated config.
 * Renders an error page if validation fails.
 */
export const EnvironmentProvider: React.FC<EnvironmentProviderProps> = ({
  children
}) => {
  const [contextValue, setContextValue] = useState<EnvironmentContextValue>({
    env: {} as ValidatedEnv,
    isValid: false,
    errors: [],
    isLoading: true,
    revalidate: () => {}
  });

  const validateEnvironmentConfig = React.useCallback(() => {
    setContextValue(prev => ({ ...prev, isLoading: true }));

    try {
      const result = validateEnvironment();
      
      // Run development environment checks
      if (import.meta.env.DEV) {
        checkEnvironmentSetup();
      }

      setContextValue({
        env: result.env,
        isValid: result.isValid,
        errors: result.errors,
        isLoading: false,
        revalidate: validateEnvironmentConfig
      });

      // Log validation results in development
      if (import.meta.env.DEV) {
        if (result.isValid) {
          console.log('✅ Environment validation successful');
        } else {
          console.error('❌ Environment validation failed:', result.errors);
        }
      }
    } catch (error) {
      console.error('Environment validation error:', error);
      
      // Create a fallback error state
      setContextValue({
        env: {} as ValidatedEnv,
        isValid: false,
        errors: [{
          variable: 'ENVIRONMENT',
          message: error instanceof Error ? error.message : 'Unknown validation error',
          severity: 'error'
        }],
        isLoading: false,
        revalidate: validateEnvironmentConfig
      });
    }
  }, []);

  // Validate environment on mount
  useEffect(() => {
    validateEnvironmentConfig();
  }, [validateEnvironmentConfig]);

  // Show loading while validating
  if (contextValue.isLoading) {
    return <EnvironmentLoading />;
  }

  // Show error page if validation fails
  if (!contextValue.isValid) {
    return (
      <EnvironmentError 
        errors={contextValue.errors}
        onRetry={contextValue.revalidate}
      />
    );
  }

  // Provide validated environment to children
  return (
    <EnvironmentContext.Provider value={contextValue}>
      {children}
    </EnvironmentContext.Provider>
  );
};

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

export default EnvironmentProvider;