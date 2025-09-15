/**
 * Centralized API Services Export
 *
 * Provides centralized access to all API services with:
 * - Authentication handling
 * - HTTP interceptors
 * - Error standardization
 * - Type safety
 */

// Core API utilities
import {apiClient} from './httpInterceptors';
// Authentication API
import authApiService from './authApi';
// Hive API
import hiveApiService from './hiveApi';
// Presence API
import presenceApiService from './presenceApi';
// Timer API
import timerApiService from './timerApi';
// Analytics API
import analyticsApiService from './analyticsApi';
// New centralized API configuration
import {
  API_ENDPOINTS,
  buildEndpoint,
  getServiceUrl,
  HTTP_STATUS,
  SERVICE_ENDPOINTS,
  TIMEOUT_CONFIG
} from '../../config/apiConfig';

// Legacy environment configuration for backward compatibility
import {getApiConfig} from '../config/environmentConfig';

export {
  apiClient,
  type StandardizedError
} from './httpInterceptors';

export {default as authApiService} from './authApi';

export {default as hiveApiService} from './hiveApi';

export {default as presenceApiService} from './presenceApi';

export {default as timerApiService} from './timerApi';

export {default as analyticsApiService} from './analyticsApi';

// API base configuration - uses validated environment variables
export const API_CONFIG = {
  get baseURL() {
    try {
      return getApiConfig().baseUrl;
    } catch {
      // Fallback for cases where validation hasn't run yet
      return import.meta.env.VITE_API_BASE_URL || 'https://identity.focushive.app';
    }
  },
  timeout: TIMEOUT_CONFIG.DEFAULT,
  retries: 3,
  retryDelay: 1000
} as const;

// Re-export from centralized configuration
export {API_ENDPOINTS, SERVICE_ENDPOINTS, buildEndpoint, getServiceUrl, HTTP_STATUS};

// Re-exported from centralized configuration - see src/config/apiConfig.ts

// Re-exported from centralized configuration - see src/config/apiConfig.ts

export default {
  authApiService,
  hiveApiService,
  presenceApiService,
  timerApiService,
  analyticsApiService,
  apiClient,
  API_CONFIG,
  API_ENDPOINTS,
  SERVICE_ENDPOINTS,
  buildEndpoint,
  getServiceUrl,
  HTTP_STATUS
};