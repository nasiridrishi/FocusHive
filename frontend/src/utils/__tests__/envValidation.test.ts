/**
 * Environment Validation Tests for FocusHive Frontend
 * 
 * Comprehensive test suite for environment variable validation logic
 * including edge cases, error handling, and validation messages.
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { z } from 'zod';
import { validateEnvironment, isDevelopment, isProduction, getEnvironment } from '../envValidation';

// Mock import.meta.env
const mockImportMeta = {
  env: {} as Record<string, string | undefined>,
  DEV: false,
  PROD: false,
};

// Store original import.meta
const originalImportMeta = globalThis.import.meta;

describe('Environment Validation', () => {
  beforeEach(() => {
    // Mock import.meta for testing
    globalThis.import.meta = mockImportMeta;
    
    // Clear console methods
    vi.spyOn(console, 'info').mockImplementation(() => {});
    vi.spyOn(console, 'error').mockImplementation(() => {});
    
    // Reset environment
    mockImportMeta.env = {};
    mockImportMeta.DEV = false;
    mockImportMeta.PROD = false;
  });

  afterEach(() => {
    // Restore original import.meta
    globalThis.import.meta = originalImportMeta;
    
    // Restore console methods
    vi.restoreAllMocks();
  });

  describe('validateEnvironment', () => {
    it('should validate successfully with all required variables', () => {
      // Arrange
      mockImportMeta.env = {
        VITE_API_URL: 'https://api.focushive.com',
        VITE_WS_URL: 'wss://api.focushive.com',
        VITE_AUTH_DOMAIN: 'auth.focushive.com',
        VITE_SPOTIFY_CLIENT_ID: 'abc123def456',
        VITE_ENV: 'production',
      };

      // Act
      const result = validateEnvironment();

      // Assert
      expect(result).toEqual({
        VITE_API_URL: 'https://api.focushive.com',
        VITE_WS_URL: 'wss://api.focushive.com',
        VITE_AUTH_DOMAIN: 'auth.focushive.com',
        VITE_SPOTIFY_CLIENT_ID: 'abc123def456',
        VITE_ENV: 'production',
        VITE_API_TIMEOUT: 30000,
        VITE_MAX_RETRIES: 3,
        VITE_DEBUG: false,
        VITE_WEBSOCKET_RECONNECT_ATTEMPTS: 10,
        VITE_WEBSOCKET_RECONNECT_DELAY: 1000,
        VITE_WEBSOCKET_HEARTBEAT_INTERVAL: 30000,
      });
    });

    it('should apply default values for optional variables', () => {
      // Arrange
      mockImportMeta.env = {
        VITE_API_URL: 'http://localhost:8080',
        VITE_WS_URL: 'ws://localhost:8080',
        VITE_AUTH_DOMAIN: 'localhost:8081',
        VITE_SPOTIFY_CLIENT_ID: 'test123',
        VITE_ENV: 'development',
        VITE_API_TIMEOUT: '5000',
        VITE_MAX_RETRIES: '1',
        VITE_DEBUG: 'true',
      };

      // Act
      const result = validateEnvironment();

      // Assert
      expect(result.VITE_API_TIMEOUT).toBe(5000);
      expect(result.VITE_MAX_RETRIES).toBe(1);
      expect(result.VITE_DEBUG).toBe(true);
      expect(result.VITE_WEBSOCKET_RECONNECT_ATTEMPTS).toBe(10); // Default
    });

    it('should throw error for missing required VITE_API_URL', () => {
      // Arrange
      mockImportMeta.env = {
        // VITE_API_URL missing
        VITE_WS_URL: 'ws://localhost:8080',
        VITE_AUTH_DOMAIN: 'localhost:8081',
        VITE_SPOTIFY_CLIENT_ID: 'test123',
        VITE_ENV: 'development',
      };

      // Act & Assert
      expect(() => validateEnvironment()).toThrow(/Environment Validation Failed/);
      expect(() => validateEnvironment()).toThrow(/VITE_API_URL/);
    });

    it('should throw error for missing required VITE_WS_URL', () => {
      // Arrange
      mockImportMeta.env = {
        VITE_API_URL: 'http://localhost:8080',
        // VITE_WS_URL missing
        VITE_AUTH_DOMAIN: 'localhost:8081',
        VITE_SPOTIFY_CLIENT_ID: 'test123',
        VITE_ENV: 'development',
      };

      // Act & Assert
      expect(() => validateEnvironment()).toThrow(/Environment Validation Failed/);
      expect(() => validateEnvironment()).toThrow(/VITE_WS_URL/);
    });

    it('should throw error for invalid VITE_API_URL format', () => {
      // Arrange
      mockImportMeta.env = {
        VITE_API_URL: 'not-a-valid-url',
        VITE_WS_URL: 'ws://localhost:8080',
        VITE_AUTH_DOMAIN: 'localhost:8081',
        VITE_SPOTIFY_CLIENT_ID: 'test123',
        VITE_ENV: 'development',
      };

      // Act & Assert
      expect(() => validateEnvironment()).toThrow(/Environment Validation Failed/);
      expect(() => validateEnvironment()).toThrow(/Must be a valid URL/);
    });

    it('should throw error for invalid VITE_WS_URL format', () => {
      // Arrange
      mockImportMeta.env = {
        VITE_API_URL: 'http://localhost:8080',
        VITE_WS_URL: 'http://localhost:8080', // Should be ws:// or wss://
        VITE_AUTH_DOMAIN: 'localhost:8081',
        VITE_SPOTIFY_CLIENT_ID: 'test123',
        VITE_ENV: 'development',
      };

      // Act & Assert
      expect(() => validateEnvironment()).toThrow(/Environment Validation Failed/);
      expect(() => validateEnvironment()).toThrow(/WebSocket URL must start with ws:\/\/ or wss:\/\//);
    });

    it('should throw error for invalid VITE_ENV value', () => {
      // Arrange
      mockImportMeta.env = {
        VITE_API_URL: 'http://localhost:8080',
        VITE_WS_URL: 'ws://localhost:8080',
        VITE_AUTH_DOMAIN: 'localhost:8081',
        VITE_SPOTIFY_CLIENT_ID: 'test123',
        VITE_ENV: 'invalid-env',
      };

      // Act & Assert
      expect(() => validateEnvironment()).toThrow(/Environment Validation Failed/);
      expect(() => validateEnvironment()).toThrow(/Environment must be development, staging, or production/);
    });

    it('should throw error for invalid numeric values', () => {
      // Arrange
      mockImportMeta.env = {
        VITE_API_URL: 'http://localhost:8080',
        VITE_WS_URL: 'ws://localhost:8080',
        VITE_AUTH_DOMAIN: 'localhost:8081',
        VITE_SPOTIFY_CLIENT_ID: 'test123',
        VITE_ENV: 'development',
        VITE_API_TIMEOUT: '500', // Too low (minimum 1000)
      };

      // Act & Assert
      expect(() => validateEnvironment()).toThrow(/Environment Validation Failed/);
      expect(() => validateEnvironment()).toThrow(/API timeout must be at least 1000ms/);
    });

    it('should validate optional URLs when provided', () => {
      // Arrange
      mockImportMeta.env = {
        VITE_API_URL: 'http://localhost:8080',
        VITE_WS_URL: 'ws://localhost:8080',
        VITE_AUTH_DOMAIN: 'localhost:8081',
        VITE_SPOTIFY_CLIENT_ID: 'test123',
        VITE_ENV: 'development',
        VITE_MUSIC_SERVICE_URL: 'https://music.focushive.com',
        VITE_SPOTIFY_REDIRECT_URI: 'http://localhost:3000/music/spotify/callback',
        VITE_ERROR_LOGGING_ENDPOINT: 'https://sentry.io/api',
      };

      // Act
      const result = validateEnvironment();

      // Assert
      expect(result.VITE_MUSIC_SERVICE_URL).toBe('https://music.focushive.com');
      expect(result.VITE_SPOTIFY_REDIRECT_URI).toBe('http://localhost:3000/music/spotify/callback');
      expect(result.VITE_ERROR_LOGGING_ENDPOINT).toBe('https://sentry.io/api');
    });

    it('should throw error for invalid optional URL formats', () => {
      // Arrange
      mockImportMeta.env = {
        VITE_API_URL: 'http://localhost:8080',
        VITE_WS_URL: 'ws://localhost:8080',
        VITE_AUTH_DOMAIN: 'localhost:8081',
        VITE_SPOTIFY_CLIENT_ID: 'test123',
        VITE_ENV: 'development',
        VITE_MUSIC_SERVICE_URL: 'not-a-url',
      };

      // Act & Assert
      expect(() => validateEnvironment()).toThrow(/Environment Validation Failed/);
      expect(() => validateEnvironment()).toThrow(/Must be a valid URL/);
    });

    it('should validate WebSocket configuration ranges', () => {
      // Arrange
      mockImportMeta.env = {
        VITE_API_URL: 'http://localhost:8080',
        VITE_WS_URL: 'ws://localhost:8080',
        VITE_AUTH_DOMAIN: 'localhost:8081',
        VITE_SPOTIFY_CLIENT_ID: 'test123',
        VITE_ENV: 'development',
        VITE_WEBSOCKET_RECONNECT_ATTEMPTS: '100', // Too high (max 50)
      };

      // Act & Assert
      expect(() => validateEnvironment()).toThrow(/Environment Validation Failed/);
      expect(() => validateEnvironment()).toThrow(/Reconnect attempts cannot exceed 50/);
    });

    it('should log configuration summary on successful validation', () => {
      // Arrange
      const consoleSpy = vi.spyOn(console, 'info').mockImplementation(() => {});
      mockImportMeta.env = {
        VITE_API_URL: 'http://localhost:8080',
        VITE_WS_URL: 'ws://localhost:8080',
        VITE_AUTH_DOMAIN: 'localhost:8081',
        VITE_SPOTIFY_CLIENT_ID: 'test123',
        VITE_ENV: 'development',
      };

      // Act
      validateEnvironment();

      // Assert
      expect(consoleSpy).toHaveBeenCalledWith('✅ Environment validation successful');
      expect(consoleSpy).toHaveBeenCalledWith('📋 Environment Configuration:', expect.any(Object));
    });

    it('should provide detailed error message with setup instructions', () => {
      // Arrange
      mockImportMeta.env = {}; // No environment variables

      // Act & Assert
      expect(() => validateEnvironment()).toThrow(/Environment Validation Failed/);
      expect(() => validateEnvironment()).toThrow(/Copy \.env\.example to \.env/);
      expect(() => validateEnvironment()).toThrow(/Update the required variables/);
    });
  });

  describe('Utility Functions', () => {
    describe('isDevelopment', () => {
      it('should return true when DEV is true', () => {
        // Arrange
        mockImportMeta.DEV = true;

        // Act & Assert
        expect(isDevelopment()).toBe(true);
      });

      it('should return false when DEV is false', () => {
        // Arrange
        mockImportMeta.DEV = false;

        // Act & Assert
        expect(isDevelopment()).toBe(false);
      });
    });

    describe('isProduction', () => {
      it('should return true when PROD is true', () => {
        // Arrange
        mockImportMeta.PROD = true;

        // Act & Assert
        expect(isProduction()).toBe(true);
      });

      it('should return false when PROD is false', () => {
        // Arrange
        mockImportMeta.PROD = false;

        // Act & Assert
        expect(isProduction()).toBe(false);
      });
    });

    describe('getEnvironment', () => {
      it('should return VITE_ENV value when set', () => {
        // Arrange
        mockImportMeta.env.VITE_ENV = 'staging';

        // Act & Assert
        expect(getEnvironment()).toBe('staging');
      });

      it('should return "development" as default when VITE_ENV not set', () => {
        // Arrange
        mockImportMeta.env.VITE_ENV = undefined;

        // Act & Assert
        expect(getEnvironment()).toBe('development');
      });
    });
  });

  describe('Error Handling', () => {
    it('should handle non-Zod errors gracefully', () => {
      // Arrange - Mock Zod to throw a non-Zod error
      const originalParse = z.object({}).parse;
      vi.spyOn(z.object({}), 'parse').mockImplementation(() => {
        throw new Error('Unexpected error');
      });

      mockImportMeta.env = {
        VITE_API_URL: 'http://localhost:8080',
        VITE_WS_URL: 'ws://localhost:8080',
        VITE_AUTH_DOMAIN: 'localhost:8081',
        VITE_SPOTIFY_CLIENT_ID: 'test123',
        VITE_ENV: 'development',
      };

      // Act & Assert
      expect(() => validateEnvironment()).toThrow('Unexpected error');

      // Cleanup
      vi.restoreAllMocks();
    });

    it('should categorize validation errors correctly', () => {
      // Arrange
      mockImportMeta.env = {
        VITE_API_URL: 'invalid-url',
        // Missing VITE_WS_URL
        VITE_AUTH_DOMAIN: 'localhost:8081',
        VITE_SPOTIFY_CLIENT_ID: 'test123',
        VITE_ENV: 'invalid-env',
      };

      // Act & Assert
      expect(() => validateEnvironment()).toThrow(/Missing Required Environment Variables/);
      expect(() => validateEnvironment()).toThrow(/Invalid Environment Variables/);
      expect(() => validateEnvironment()).toThrow(/VITE_WS_URL/);
      expect(() => validateEnvironment()).toThrow(/VITE_API_URL.*Must be a valid URL/);
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty string values as missing', () => {
      // Arrange
      mockImportMeta.env = {
        VITE_API_URL: '',
        VITE_WS_URL: 'ws://localhost:8080',
        VITE_AUTH_DOMAIN: 'localhost:8081',
        VITE_SPOTIFY_CLIENT_ID: 'test123',
        VITE_ENV: 'development',
      };

      // Act & Assert
      expect(() => validateEnvironment()).toThrow(/Environment Validation Failed/);
    });

    it('should handle boundary values correctly', () => {
      // Arrange
      mockImportMeta.env = {
        VITE_API_URL: 'http://localhost:8080',
        VITE_WS_URL: 'ws://localhost:8080',
        VITE_AUTH_DOMAIN: 'localhost:8081',
        VITE_SPOTIFY_CLIENT_ID: 'test123',
        VITE_ENV: 'development',
        VITE_API_TIMEOUT: '1000', // Minimum allowed
        VITE_MAX_RETRIES: '0', // Minimum allowed
        VITE_WEBSOCKET_RECONNECT_ATTEMPTS: '1', // Minimum allowed
        VITE_WEBSOCKET_RECONNECT_DELAY: '100', // Minimum allowed
        VITE_WEBSOCKET_HEARTBEAT_INTERVAL: '5000', // Minimum allowed
      };

      // Act
      const result = validateEnvironment();

      // Assert
      expect(result.VITE_API_TIMEOUT).toBe(1000);
      expect(result.VITE_MAX_RETRIES).toBe(0);
      expect(result.VITE_WEBSOCKET_RECONNECT_ATTEMPTS).toBe(1);
      expect(result.VITE_WEBSOCKET_RECONNECT_DELAY).toBe(100);
      expect(result.VITE_WEBSOCKET_HEARTBEAT_INTERVAL).toBe(5000);
    });

    it('should validate maximum boundary values', () => {
      // Arrange
      mockImportMeta.env = {
        VITE_API_URL: 'http://localhost:8080',
        VITE_WS_URL: 'ws://localhost:8080',
        VITE_AUTH_DOMAIN: 'localhost:8081',
        VITE_SPOTIFY_CLIENT_ID: 'test123',
        VITE_ENV: 'development',
        VITE_API_TIMEOUT: '120000', // Maximum allowed
        VITE_MAX_RETRIES: '10', // Maximum allowed
        VITE_WEBSOCKET_RECONNECT_ATTEMPTS: '50', // Maximum allowed
        VITE_WEBSOCKET_RECONNECT_DELAY: '30000', // Maximum allowed
        VITE_WEBSOCKET_HEARTBEAT_INTERVAL: '300000', // Maximum allowed
      };

      // Act
      const result = validateEnvironment();

      // Assert
      expect(result.VITE_API_TIMEOUT).toBe(120000);
      expect(result.VITE_MAX_RETRIES).toBe(10);
      expect(result.VITE_WEBSOCKET_RECONNECT_ATTEMPTS).toBe(50);
      expect(result.VITE_WEBSOCKET_RECONNECT_DELAY).toBe(30000);
      expect(result.VITE_WEBSOCKET_HEARTBEAT_INTERVAL).toBe(300000);
    });
  });
});