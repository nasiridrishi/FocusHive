/**
 * Environment Variable Validation Service
 * 
 * Validates all required and optional environment variables at application startup.
 * Provides type-safe access to environment variables with validation.
 * 
 * Based on Vite environment variable best practices:
 * - All client-side env vars must be prefixed with VITE_
 * - Use import.meta.env to access variables
 * - Provide fallbacks for optional variables
 */

// Environment variable type definitions
export interface ValidatedEnv {
  // Core API Configuration (Required)
  VITE_API_BASE_URL: string;
  VITE_WEBSOCKET_URL: string;

  // WebSocket Configuration (Optional with defaults)
  VITE_WEBSOCKET_RECONNECT_ATTEMPTS: number;
  VITE_WEBSOCKET_RECONNECT_DELAY: number;
  VITE_WEBSOCKET_HEARTBEAT_INTERVAL: number;

  // Music Service Configuration (Optional)
  VITE_MUSIC_API_BASE_URL?: string;
  VITE_MUSIC_SERVICE_URL?: string;
  
  // Spotify Integration (Optional)
  VITE_SPOTIFY_CLIENT_ID?: string;
  VITE_SPOTIFY_REDIRECT_URI?: string;

  // Error Logging (Optional)
  VITE_ERROR_LOGGING_ENDPOINT?: string;
  VITE_ERROR_LOGGING_API_KEY?: string;

  // Built-in Vite variables (always available)
  MODE: string;
  DEV: boolean;
  PROD: boolean;
  SSR: boolean;
  BASE_URL: string;
}

// Validation error type
export interface EnvValidationError {
  variable: string;
  message: string;
  severity: 'error' | 'warning';
}

// Environment validation configuration
interface EnvValidationConfig {
  variable: string;
  required: boolean;
  type: 'string' | 'number' | 'boolean' | 'url';
  defaultValue?: string | number | boolean;
  validator?: (value: unknown) => boolean;
  description: string;
}

// Environment variable validation rules
const ENV_VALIDATION_RULES: EnvValidationConfig[] = [
  {
    variable: 'VITE_API_BASE_URL',
    required: true,
    type: 'url',
    description: 'Base URL for the FocusHive backend API',
    validator: (url: string) => {
      try {
        new URL(url);
        return url.startsWith('http://') || url.startsWith('https://');
      } catch {
        return false;
      }
    }
  },
  {
    variable: 'VITE_WEBSOCKET_URL',
    required: true,
    type: 'url',
    description: 'WebSocket URL for real-time communication',
    validator: (url: string) => {
      try {
        const wsUrl = new URL(url);
        return wsUrl.protocol === 'ws:' || wsUrl.protocol === 'wss:';
      } catch {
        return false;
      }
    }
  },
  {
    variable: 'VITE_WEBSOCKET_RECONNECT_ATTEMPTS',
    required: false,
    type: 'number',
    defaultValue: 10,
    description: 'Number of WebSocket reconnection attempts',
    validator: (value: number) => value > 0 && value <= 100
  },
  {
    variable: 'VITE_WEBSOCKET_RECONNECT_DELAY',
    required: false,
    type: 'number',
    defaultValue: 1000,
    description: 'WebSocket reconnection delay in milliseconds',
    validator: (value: number) => value >= 100 && value <= 60000
  },
  {
    variable: 'VITE_WEBSOCKET_HEARTBEAT_INTERVAL',
    required: false,
    type: 'number',
    defaultValue: 30000,
    description: 'WebSocket heartbeat interval in milliseconds',
    validator: (value: number) => value >= 5000 && value <= 300000
  },
  {
    variable: 'VITE_MUSIC_API_BASE_URL',
    required: false,
    type: 'url',
    description: 'Base URL for music service API (optional)',
    validator: (url: string) => {
      if (!url) return true; // Optional
      try {
        new URL(url);
        return url.startsWith('http://') || url.startsWith('https://');
      } catch {
        return false;
      }
    }
  },
  {
    variable: 'VITE_MUSIC_SERVICE_URL',
    required: false,
    type: 'url',
    defaultValue: 'http://localhost:8084',
    description: 'Music service URL for Spotify integration',
    validator: (url: string) => {
      try {
        new URL(url);
        return url.startsWith('http://') || url.startsWith('https://');
      } catch {
        return false;
      }
    }
  },
  {
    variable: 'VITE_SPOTIFY_CLIENT_ID',
    required: false,
    type: 'string',
    description: 'Spotify Client ID for music integration (optional)',
    validator: (value: string) => !value || (value.length > 0 && /^[a-zA-Z0-9]+$/.test(value))
  },
  {
    variable: 'VITE_SPOTIFY_REDIRECT_URI',
    required: false,
    type: 'url',
    description: 'Spotify OAuth redirect URI (optional)',
    validator: (url: string) => {
      if (!url) return true; // Will use default
      try {
        new URL(url);
        return true;
      } catch {
        return false;
      }
    }
  },
  {
    variable: 'VITE_ERROR_LOGGING_ENDPOINT',
    required: false,
    type: 'url',
    description: 'Remote error logging endpoint (optional)',
    validator: (url: string) => {
      if (!url) return true; // Optional
      try {
        new URL(url);
        return url.startsWith('http://') || url.startsWith('https://');
      } catch {
        return false;
      }
    }
  },
  {
    variable: 'VITE_ERROR_LOGGING_API_KEY',
    required: false,
    type: 'string',
    description: 'API key for remote error logging (optional)',
    validator: (value: string) => !value || value.length >= 8
  }
];

/**
 * Validates a single environment variable according to its configuration
 */
function validateEnvironmentVariable(
  config: EnvValidationConfig,
  rawValue: string | undefined
): { value: unknown; error?: EnvValidationError } {
  const { variable, required, type, defaultValue, validator, description } = config;

  // Check if required variable is missing
  if (required && (rawValue === undefined || rawValue === '')) {
    return {
      value: undefined,
      error: {
        variable,
        message: `Required environment variable ${variable} is missing. ${description}`,
        severity: 'error'
      }
    };
  }

  // Use default value if not provided and not required
  if (!rawValue && defaultValue !== undefined) {
    return { value: defaultValue };
  }

  // Return undefined for optional variables without values
  if (!rawValue && !required) {
    return { value: undefined };
  }

  let convertedValue: unknown = rawValue;

  // Type conversion
  try {
    switch (type) {
      case 'number': {
        const numValue = parseInt(rawValue!, 10);
        convertedValue = numValue;
        if (isNaN(numValue)) {
          return {
            value: undefined,
            error: {
              variable,
              message: `Environment variable ${variable} must be a valid number. Got: "${rawValue}"`,
              severity: 'error'
            }
          };
        }
        break;
      }
      case 'boolean':
        convertedValue = rawValue!.toLowerCase() === 'true';
        break;
      case 'url':
      case 'string':
        convertedValue = rawValue!;
        break;
    }
  } catch (error) {
    return {
      value: undefined,
      error: {
        variable,
        message: `Failed to convert environment variable ${variable} to ${type}. Got: "${rawValue}"`,
        severity: 'error'
      }
    };
  }

  // Custom validation
  if (validator && !validator(convertedValue)) {
    return {
      value: convertedValue,
      error: {
        variable,
        message: `Environment variable ${variable} failed validation. ${description}. Got: "${rawValue}"`,
        severity: required ? 'error' : 'warning'
      }
    };
  }

  return { value: convertedValue };
}

/**
 * Validates all environment variables and returns the validated configuration
 */
export function validateEnvironment(): {
  env: ValidatedEnv;
  errors: EnvValidationError[];
  isValid: boolean;
} {
  const errors: EnvValidationError[] = [];
  const validatedEnv: Partial<ValidatedEnv> = {};

  // Validate custom environment variables
  for (const config of ENV_VALIDATION_RULES) {
    const rawValue = import.meta.env[config.variable];
    const result = validateEnvironmentVariable(config, rawValue);

    if (result.error) {
      errors.push(result.error);
    }

    if (result.value !== undefined) {
      (validatedEnv as unknown)[config.variable] = result.value;
    }
  }

  // Add built-in Vite environment variables
  validatedEnv.MODE = import.meta.env.MODE;
  validatedEnv.DEV = import.meta.env.DEV;
  validatedEnv.PROD = import.meta.env.PROD;
  validatedEnv.SSR = import.meta.env.SSR;
  validatedEnv.BASE_URL = import.meta.env.BASE_URL;

  // Environment-specific validation
  if (validatedEnv.PROD) {
    // Production-specific validations
    if (!validatedEnv.VITE_API_BASE_URL?.startsWith('https://')) {
      errors.push({
        variable: 'VITE_API_BASE_URL',
        message: 'Production builds should use HTTPS for API base URL',
        severity: 'warning'
      });
    }

    if (!validatedEnv.VITE_WEBSOCKET_URL?.startsWith('wss://')) {
      errors.push({
        variable: 'VITE_WEBSOCKET_URL',
        message: 'Production builds should use secure WebSocket (wss://) for WebSocket URL',
        severity: 'warning'
      });
    }
  }

  const hasErrors = errors.some(error => error.severity === 'error');

  return {
    env: validatedEnv as ValidatedEnv,
    errors,
    isValid: !hasErrors
  };
}

/**
 * Gets a validated environment configuration
 * Throws an error if validation fails
 */
export function getValidatedEnv(): ValidatedEnv {
  const result = validateEnvironment();
  
  if (!result.isValid) {
    const errorMessages = result.errors
      .filter(error => error.severity === 'error')
      .map(error => error.message)
      .join('\n');
    
    throw new Error(`Environment validation failed:\n${errorMessages}`);
  }

  return result.env;
}

/**
 * Validates environment and logs warnings for non-critical issues
 */
export function validateAndWarnEnvironment(): ValidatedEnv {
  const result = validateEnvironment();
  
  // Log warnings to console
  const warnings = result.errors.filter(error => error.severity === 'warning');
  if (warnings.length > 0) {
    console.warn('Environment validation warnings:');
    warnings.forEach(warning => {
      console.warn(`⚠️  ${warning.message}`);
    });
  }

  // Throw error for critical issues
  if (!result.isValid) {
    const errorMessages = result.errors
      .filter(error => error.severity === 'error')
      .map(error => error.message)
      .join('\n');
    
    throw new Error(`Environment validation failed:\n${errorMessages}`);
  }

  return result.env;
}

/**
 * Development helper to check environment setup
 */
export function checkEnvironmentSetup(): void {
  if (import.meta.env.DEV) {
    console.group('🔧 Environment Configuration');
    
    const result = validateEnvironment();
    
    console.log('Environment Mode:', import.meta.env.MODE);
    console.log('Development Mode:', import.meta.env.DEV);
    console.log('Production Mode:', import.meta.env.PROD);
    
    if (result.errors.length > 0) {
      console.group('⚠️ Environment Issues');
      result.errors.forEach(error => {
        const icon = error.severity === 'error' ? '❌' : '⚠️';
        console.log(`${icon} ${error.variable}: ${error.message}`);
      });
      console.groupEnd();
    } else {
      console.log('✅ All environment variables validated successfully');
    }

    console.log('\nValidated Configuration:');
    console.table(result.env);
    
    console.groupEnd();
  }
}

// Export validated environment instance (lazy-loaded)
let _validatedEnv: ValidatedEnv | null = null;

/**
 * Get the singleton validated environment instance
 */
export function env(): ValidatedEnv {
  if (!_validatedEnv) {
    _validatedEnv = validateAndWarnEnvironment();
  }
  return _validatedEnv;
}

// Default export
export default {
  validateEnvironment,
  getValidatedEnv,
  validateAndWarnEnvironment,
  checkEnvironmentSetup,
  env
};