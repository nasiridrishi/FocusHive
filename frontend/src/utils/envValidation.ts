/**
 * Environment Variable Validation for FocusHive Frontend
 *
 * This module provides comprehensive validation of VITE_* environment variables
 * using Zod schema validation. It ensures all required variables are present
 * at startup and provides clear error messages for missing/invalid configuration.
 *
 * Features:
 * - TypeScript-first validation with Zod
 * - Clear error messages for missing variables
 * - Default values for optional variables
 * - URL format validation
 * - Development/production environment detection
 * - Comprehensive logging of validated configuration
 */

import {z} from 'zod';

// URL validation schema that accepts both HTTP and HTTPS
const urlSchema = z
.string()
.url('Must be a valid URL')
.refine(
    (url) => url.startsWith('http://') || url.startsWith('https://'),
    'URL must start with http:// or https://'
);

// WebSocket URL validation schema
const wsUrlSchema = z
.string()
.refine(
    (url) => url.startsWith('ws://') || url.startsWith('wss://'),
    'WebSocket URL must start with ws:// or wss://'
);

// Environment validation schema
const environmentSchema = z.object({
  // Required Variables
  VITE_API_URL: urlSchema.describe('Backend API base URL'),
  VITE_WS_URL: wsUrlSchema.describe('WebSocket connection URL'),
  VITE_AUTH_DOMAIN: z.string().min(1, 'Auth domain is required').describe('Authentication service domain'),
  VITE_SPOTIFY_CLIENT_ID: z.string().min(1, 'Spotify Client ID is required').describe('Spotify Web API Client ID'),
  VITE_ENV: z.enum(['development', 'staging', 'production'], {
    message: 'Environment must be development, staging, or production'
  }).describe('Application environment'),

  // Optional Variables with Defaults
  VITE_API_TIMEOUT: z.coerce
  .number()
  .min(1000, 'API timeout must be at least 1000ms')
  .max(120000, 'API timeout must not exceed 120000ms')
  .default(30000)
  .describe('API request timeout in milliseconds'),

  VITE_MAX_RETRIES: z.coerce
  .number()
  .min(0, 'Max retries cannot be negative')
  .max(10, 'Max retries cannot exceed 10')
  .default(3)
  .describe('Maximum number of API retry attempts'),

  VITE_DEBUG: z.coerce
  .boolean()
  .default(false)
  .describe('Enable debug logging'),

  // WebSocket Configuration (Optional)
  VITE_WEBSOCKET_RECONNECT_ATTEMPTS: z.coerce
  .number()
  .min(1, 'Reconnect attempts must be at least 1')
  .max(50, 'Reconnect attempts cannot exceed 50')
  .default(10)
  .describe('WebSocket reconnection attempts'),

  VITE_WEBSOCKET_RECONNECT_DELAY: z.coerce
  .number()
  .min(100, 'Reconnect delay must be at least 100ms')
  .max(30000, 'Reconnect delay cannot exceed 30 seconds')
  .default(1000)
  .describe('WebSocket reconnection delay in milliseconds'),

  VITE_WEBSOCKET_HEARTBEAT_INTERVAL: z.coerce
  .number()
  .min(5000, 'Heartbeat interval must be at least 5 seconds')
  .max(300000, 'Heartbeat interval cannot exceed 5 minutes')
  .default(30000)
  .describe('WebSocket heartbeat interval in milliseconds'),

  // Music Service Configuration (Optional)
  VITE_MUSIC_SERVICE_URL: urlSchema.optional().describe('Music service URL'),
  VITE_MUSIC_API_BASE_URL: urlSchema.optional().describe('Music API base URL'),
  VITE_SPOTIFY_REDIRECT_URI: urlSchema.optional().describe('Spotify OAuth redirect URI'),

  // Error Logging Configuration (Optional)
  VITE_ERROR_LOGGING_ENDPOINT: urlSchema.optional().describe('Remote error logging endpoint'),
  VITE_ERROR_LOGGING_API_KEY: z.string().optional().describe('Error logging service API key'),
});

// Export the type for use throughout the application
export type ValidatedEnvironment = z.infer<typeof environmentSchema>;

/**
 * Validates environment variables and returns a typed configuration object
 *
 * @throws {Error} If required environment variables are missing or invalid
 * @returns {ValidatedEnvironment} Validated and typed environment configuration
 */
export function validateEnvironment(): ValidatedEnvironment {
  try {
    // Extract environment variables with VITE_ prefix
    const rawEnv = {
      VITE_API_URL: import.meta.env.VITE_API_URL,
      VITE_WS_URL: import.meta.env.VITE_WS_URL,
      VITE_AUTH_DOMAIN: import.meta.env.VITE_AUTH_DOMAIN,
      VITE_SPOTIFY_CLIENT_ID: import.meta.env.VITE_SPOTIFY_CLIENT_ID,
      VITE_ENV: import.meta.env.VITE_ENV,
      VITE_API_TIMEOUT: import.meta.env.VITE_API_TIMEOUT,
      VITE_MAX_RETRIES: import.meta.env.VITE_MAX_RETRIES,
      VITE_DEBUG: import.meta.env.VITE_DEBUG,
      VITE_WEBSOCKET_RECONNECT_ATTEMPTS: import.meta.env.VITE_WEBSOCKET_RECONNECT_ATTEMPTS,
      VITE_WEBSOCKET_RECONNECT_DELAY: import.meta.env.VITE_WEBSOCKET_RECONNECT_DELAY,
      VITE_WEBSOCKET_HEARTBEAT_INTERVAL: import.meta.env.VITE_WEBSOCKET_HEARTBEAT_INTERVAL,
      VITE_MUSIC_SERVICE_URL: import.meta.env.VITE_MUSIC_SERVICE_URL,
      VITE_MUSIC_API_BASE_URL: import.meta.env.VITE_MUSIC_API_BASE_URL,
      VITE_SPOTIFY_REDIRECT_URI: import.meta.env.VITE_SPOTIFY_REDIRECT_URI,
      VITE_ERROR_LOGGING_ENDPOINT: import.meta.env.VITE_ERROR_LOGGING_ENDPOINT,
      VITE_ERROR_LOGGING_API_KEY: import.meta.env.VITE_ERROR_LOGGING_API_KEY,
    };

    // Validate the environment variables
    const validatedEnv = environmentSchema.parse(rawEnv);

    // Log successful validation (without sensitive data)
    // Debug statement removed
    // Debug statement removed
    return validatedEnv;
  } catch (error) {
    if (error instanceof z.ZodError) {
      // Create a detailed error message for missing/invalid environment variables
      const missingVars: string[] = [];
      const invalidVars: string[] = [];

      error.issues.forEach((issue) => {
        const varName = issue.path.join('.');
        if (issue.code === 'invalid_type' && 'received' in issue && issue.received === 'undefined') {
          missingVars.push(varName);
        } else {
          invalidVars.push(`${varName}: ${issue.message}`);
        }
      });

      let errorMessage = '❌ Environment Validation Failed!\n\n';

      if (missingVars.length > 0) {
        errorMessage += '🚨 Missing Required Environment Variables:\n';
        missingVars.forEach(varName => {
          errorMessage += `  - ${varName}\n`;
        });
        errorMessage += '\n';
      }

      if (invalidVars.length > 0) {
        errorMessage += '⚠️  Invalid Environment Variables:\n';
        invalidVars.forEach(error => {
          errorMessage += `  - ${error}\n`;
        });
        errorMessage += '\n';
      }

      errorMessage += '📖 Setup Instructions:\n';
      errorMessage += '1. Copy .env.example to .env in the frontend directory\n';
      errorMessage += '2. Update the required variables with your configuration\n';
      errorMessage += '3. Ensure all URLs are properly formatted\n';
      errorMessage += '4. Restart the development server\n\n';
      errorMessage += '📚 See .env.example for detailed configuration examples';

      // Debug statement removed
      throw new Error(errorMessage);
    }

    // Re-throw other errors
    throw error;
  }
}

/**
 * Returns true if the application is running in development mode
 */
export function isDevelopment(): boolean {
  return import.meta.env.DEV;
}

/**
 * Returns true if the application is running in production mode
 */
export function isProduction(): boolean {
  return import.meta.env.PROD;
}

/**
 * Returns the current environment name
 */
export function getEnvironment(): string {
  return import.meta.env.VITE_ENV || 'development';
}

/**
 * Safely gets an environment variable with a fallback value
 */
export function getEnvVar(key: string, fallback?: string): string | undefined {
  return import.meta.env[key] || fallback;
}