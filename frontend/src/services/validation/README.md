# Environment Variable Validation

This module provides comprehensive environment variable validation for the FocusHive frontend
application.

## Overview

The environment validation system ensures that all required configuration is present and valid
before the application starts. It provides:

- **Type-safe environment configuration** with TypeScript definitions
- **Runtime validation** with detailed error messages
- **User-friendly error pages** when configuration is missing
- **Development vs production validation** with appropriate warnings
- **Centralized configuration access** throughout the application

## Features

### ✅ Required Variables

- `VITE_API_BASE_URL` - Backend API URL
- `VITE_WEBSOCKET_URL` - WebSocket server URL

### ⚙️ Optional Variables (with defaults)

- `VITE_WEBSOCKET_RECONNECT_ATTEMPTS` (default: 10)
- `VITE_WEBSOCKET_RECONNECT_DELAY` (default: 1000)
- `VITE_WEBSOCKET_HEARTBEAT_INTERVAL` (default: 30000)
- `VITE_MUSIC_SERVICE_URL` (default: http://localhost:8084)
- `VITE_MUSIC_API_BASE_URL` (optional)
- `VITE_SPOTIFY_CLIENT_ID` (optional)
- `VITE_SPOTIFY_REDIRECT_URI` (optional)
- `VITE_ERROR_LOGGING_ENDPOINT` (optional)
- `VITE_ERROR_LOGGING_API_KEY` (optional)

## Usage

### Basic Usage (Recommended)

The environment validation is automatically integrated into the React app via `EnvironmentProvider`:

```tsx
import { EnvironmentProvider } from './providers/EnvironmentProvider';

function App() {
  return (
    <EnvironmentProvider>
      {/* Your app components */}
    </EnvironmentProvider>
  );
}
```

### Using Environment Variables in Components

Use the provided hooks to access validated configuration:

```tsx
import { useEnvironment, useApiConfig, useWebSocketConfig } from './providers/EnvironmentProvider';

function MyComponent() {
  // Get full environment
  const env = useEnvironment();
  
  // Get specific configuration groups
  const apiConfig = useApiConfig();
  const wsConfig = useWebSocketConfig();
  const spotifyConfig = useSpotifyConfig();
  
  return (
    <div>
      <p>API Base URL: {apiConfig.apiBaseUrl}</p>
      <p>WebSocket URL: {wsConfig.url}</p>
      {spotifyConfig.isEnabled && (
        <p>Spotify Enabled: {spotifyConfig.clientId}</p>
      )}
    </div>
  );
}
```

### Direct Validation Usage

For advanced use cases, you can use the validation functions directly:

```tsx
import { validateEnvironment, getValidatedEnv } from './services/validation';

// Validate environment and get results
const result = validateEnvironment();
console.log('Is valid:', result.isValid);
console.log('Errors:', result.errors);

// Get validated environment (throws if invalid)
const env = getValidatedEnv();
console.log('API URL:', env.VITE_API_BASE_URL);
```

### Centralized Configuration Service

Use the centralized configuration service in your API clients:

```tsx
import { getApiConfig, getWebSocketConfig } from './services/config/environmentConfig';

// API client setup
const apiConfig = getApiConfig();
const client = axios.create({
  baseURL: apiConfig.baseUrl,
  timeout: apiConfig.timeout
});

// WebSocket client setup
const wsConfig = getWebSocketConfig();
const wsClient = new WebSocketClient(wsConfig.url, {
  reconnectAttempts: wsConfig.reconnectAttempts,
  reconnectDelay: wsConfig.reconnectDelay
});
```

## Configuration

### Environment Files

Create a `.env` file in your project root:

```bash
# Required Configuration
VITE_API_BASE_URL=http://localhost:8080
VITE_WEBSOCKET_URL=ws://localhost:8080

# Optional Configuration
VITE_WEBSOCKET_RECONNECT_ATTEMPTS=10
VITE_WEBSOCKET_RECONNECT_DELAY=1000
VITE_WEBSOCKET_HEARTBEAT_INTERVAL=30000

# Spotify Integration (Optional)
VITE_SPOTIFY_CLIENT_ID=your_client_id_here
VITE_SPOTIFY_REDIRECT_URI=http://localhost:3000/music/spotify/callback

# Error Logging (Optional)
VITE_ERROR_LOGGING_ENDPOINT=https://your-logging-service.com/api/errors
VITE_ERROR_LOGGING_API_KEY=your_api_key
```

### Environment-Specific Configuration

#### Development

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_WEBSOCKET_URL=ws://localhost:8080
```

#### Staging

```bash
VITE_API_BASE_URL=https://staging-api.focushive.com
VITE_WEBSOCKET_URL=wss://staging-api.focushive.com
```

#### Production

```bash
VITE_API_BASE_URL=https://api.focushive.com
VITE_WEBSOCKET_URL=wss://api.focushive.com
```

## Validation Rules

### URL Validation

- **API URLs**: Must be valid HTTP/HTTPS URLs
- **WebSocket URLs**: Must be valid WS/WSS URLs
- **Production**: HTTPS and WSS recommended (warnings shown for HTTP/WS)

### Number Validation

- **Reconnect Attempts**: 1-100 (integer)
- **Reconnect Delay**: 100-60000 ms (integer)
- **Heartbeat Interval**: 5000-300000 ms (integer)

### String Validation

- **Spotify Client ID**: Alphanumeric characters only (if provided)
- **API Keys**: Minimum 8 characters (if provided)

## Error Handling

### User-Friendly Error Page

When validation fails, users see a comprehensive error page with:

- Clear error descriptions
- Setup instructions
- Example configuration
- Copy-to-clipboard functionality
- Environment-specific guidance

### Development Features

In development mode, the system provides:

- Console logging of validation results
- Detailed environment inspection
- Performance-friendly caching
- Hot reload support

### Production Features

In production mode, the system provides:

- Security warnings for insecure configurations
- Performance optimization
- Error reporting integration
- Graceful degradation

## API Reference

### Core Functions

#### `validateEnvironment()`

```typescript
function validateEnvironment(): {
  env: ValidatedEnv;
  errors: EnvValidationError[];
  isValid: boolean;
}
```

#### `getValidatedEnv()`

```typescript
function getValidatedEnv(): ValidatedEnv
```

Throws error if validation fails.

#### `validateAndWarnEnvironment()`

```typescript
function validateAndWarnEnvironment(): ValidatedEnv
```

Logs warnings to console, throws for errors.

### React Hooks

#### `useEnvironment()`

```typescript
function useEnvironment(): ValidatedEnv
```

#### `useApiConfig()`

```typescript
function useApiConfig(): {
  apiBaseUrl: string;
  websocketUrl: string;
  musicApiBaseUrl: string;
  musicServiceUrl: string;
}
```

#### `useWebSocketConfig()`

```typescript
function useWebSocketConfig(): {
  url: string;
  reconnectAttempts: number;
  reconnectDelay: number;
  heartbeatInterval: number;
}
```

### Configuration Services

#### `getApiConfig()`

```typescript
function getApiConfig(): {
  baseUrl: string;
  musicApiBaseUrl: string;
  timeout: number;
}
```

#### `isFeatureEnabled(feature: string)`

```typescript
function isFeatureEnabled(feature: 'spotify' | 'errorLogging' | 'musicService'): boolean
```

## Testing

The validation system includes comprehensive tests covering:

- Required field validation
- URL format validation
- Number range validation
- Production vs development scenarios
- Error message accuracy
- Default value application

Run tests with:

```bash
npm test src/services/validation/envValidation.test.ts
```

## Best Practices

### Environment Variables

1. **Always prefix with `VITE_`** for client-side access
2. **Use descriptive names** that clearly indicate purpose
3. **Provide sensible defaults** for optional variables
4. **Document all variables** in `.env.example`

### Error Handling

1. **Fail fast** - validate at application startup
2. **Provide clear messages** - help developers fix issues quickly
3. **Differentiate severity** - errors vs warnings
4. **Guide users** - show examples and instructions

### Security

1. **Never commit real values** to version control
2. **Use HTTPS/WSS in production**
3. **Validate all inputs** - don't trust environment values
4. **Sanitize sensitive data** in logs and error messages

### Performance

1. **Validate once** - cache results after initial validation
2. **Lazy load config** - only validate when needed
3. **Minimize bundle impact** - tree-shake unused validators
4. **Use development checks** - skip expensive validation in production

## Migration Guide

### From Direct `import.meta.env` Usage

**Before:**

```tsx
const apiUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
```

**After:**

```tsx
import { useApiConfig } from './providers/EnvironmentProvider';

function MyComponent() {
  const { apiBaseUrl } = useApiConfig();
  // apiBaseUrl is guaranteed to be valid
}
```

### From Manual Validation

**Before:**

```tsx
if (!import.meta.env.VITE_API_BASE_URL) {
  throw new Error('API URL is required');
}
```

**After:**

```tsx
// Validation happens automatically in EnvironmentProvider
// Just use the validated config
const env = useEnvironment();
```

This environment validation system ensures robust, type-safe, and user-friendly configuration
management for the FocusHive frontend application.