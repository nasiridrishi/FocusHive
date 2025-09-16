/**
 * Environment Provider Component
 *
 * Validates environment variables at application startup and provides
 * validated environment configuration throughout the app.
 *
 * Shows an error page if critical environment variables are missing,
 * and provides a React context for accessing validated environment variables.
 */

import React, {ReactNode, useEffect, useState} from 'react';
import {
  checkEnvironmentSetup,
  type ValidatedEnv,
  validateEnvironment
} from '../services/validation/envValidation';
import EnvironmentError from '../components/EnvironmentError';
import {Box, CircularProgress, Typography} from '@mui/material';
import {EnvironmentContext, type EnvironmentContextValue} from './useEnvironment';
import {logger} from '../utils/logger';

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
      <CircularProgress size={48}/>
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
    revalidate: () => {
    }
  });

  const validateEnvironmentConfig = React.useCallback(() => {
    setContextValue(prev => ({...prev, isLoading: true}));

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
          logger.info('✅ Environment validation successful');
        } else {
          logger.error('❌ Environment validation failed:', 'EnvironmentProvider', result.errors);
        }
      }
    } catch (error) {
      logger.error('Environment validation error:', error);

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
    return <EnvironmentLoading/>;
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

// Hooks should be imported directly from './useEnvironment' to avoid Fast Refresh warning

export default EnvironmentProvider;