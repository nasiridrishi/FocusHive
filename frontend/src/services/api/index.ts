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
import { apiClient, createAuthenticatedAxiosInstance, createServiceAxiosInstance } from './httpInterceptors';
export { 
  apiClient, 
  createAuthenticatedAxiosInstance, 
  createServiceAxiosInstance,
  type StandardizedError 
} from './httpInterceptors';

// Authentication API
import authApiService, { tokenStorage } from './authApi';
export { default as authApiService, tokenStorage } from './authApi';

// Hive API
import hiveApiService from './hiveApi';
export { default as hiveApiService } from './hiveApi';

// Presence API  
import presenceApiService from './presenceApi';
export { default as presenceApiService } from './presenceApi';

// Timer API
import timerApiService from './timerApi';
export { default as timerApiService } from './timerApi';

// Analytics API
import analyticsApiService from './analyticsApi';
export { default as analyticsApiService } from './analyticsApi';

// API base configuration - uses validated environment variables
export const API_CONFIG = {
  get baseURL() {
    try {
      const { getApiConfig } = require('../config/environmentConfig');
      return getApiConfig().baseUrl;
    } catch {
      // Fallback for cases where validation hasn't run yet
      return import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    }
  },
  timeout: 10000,
  retries: 3,
  retryDelay: 1000
} as const;

// API endpoints - Updated to match backend Spring Boot controllers
export const API_ENDPOINTS = {
  // Auth endpoints - matches DemoController and SimpleAuthController
  AUTH: {
    LOGIN: '/api/demo/login',                    // DemoController
    REGISTER: '/api/v1/auth/register',          // SimpleAuthController  
    REFRESH: '/api/v1/auth/refresh',            // Identity Service Client
    LOGOUT: '/api/v1/auth/logout',             
    ME: '/api/v1/auth/me',                     
    PROFILE: '/api/v1/auth/profile',           
    CHANGE_PASSWORD: '/api/v1/auth/change-password',
    FORGOT_PASSWORD: '/api/v1/auth/forgot-password',
    CHECK: '/api/v1/auth/check'                 // SimpleAuthController
  },
  
  // Hive endpoints - matches HiveController (/api/v1/hives)
  HIVES: {
    BASE: '/api/v1/hives',
    BY_ID: '/api/v1/hives/:id',
    BY_SLUG: '/api/v1/hives/by-slug/:slug',
    JOIN: '/api/v1/hives/:id/join',
    LEAVE: '/api/v1/hives/:id/leave',
    MEMBERS: '/api/v1/hives/:id/members',
    SEARCH: '/api/v1/hives/search'
  },
  
  // Presence endpoints - matches PresenceRestController (/api/v1/presence)
  PRESENCE: {
    BASE: '/api/v1/presence',
    USER_BY_ID: '/api/v1/presence/users/:userId',
    ME: '/api/v1/presence/me',
    HIVE: '/api/v1/presence/hives/:hiveId',
    HIVES_BATCH: '/api/v1/presence/hives/batch',
    MY_SESSIONS: '/api/v1/presence/sessions/me',
    HIVE_SESSIONS: '/api/v1/presence/hives/:hiveId/sessions'
  },
  
  // Timer endpoints - matches TimerController (/api/v1/timer)
  TIMER: {
    BASE: '/api/v1/timer',
    START_SESSION: '/api/v1/timer/sessions/start',
    END_SESSION: '/api/v1/timer/sessions/:sessionId/end',
    PAUSE_SESSION: '/api/v1/timer/sessions/:sessionId/pause',
    CURRENT_SESSION: '/api/v1/timer/sessions/current',
    SESSIONS_HISTORY: '/api/v1/timer/sessions/history',
    STATS_DAILY: '/api/v1/timer/stats/daily',
    STATS_WEEKLY: '/api/v1/timer/stats/weekly',
    STATS_MONTHLY: '/api/v1/timer/stats/monthly',
    STATS_STREAK: '/api/v1/timer/stats/streak',
    POMODORO_SETTINGS: '/api/v1/timer/pomodoro/settings'
  },
  
  // Analytics endpoints - matches AnalyticsController (/api/v1/analytics)
  ANALYTICS: {
    BASE: '/api/v1/analytics',
    START_SESSION: '/api/v1/analytics/sessions/start',
    END_SESSION: '/api/v1/analytics/sessions/:sessionId/end',
    USER_STATS: '/api/v1/analytics/users/:userId/stats',
    HIVE_LEADERBOARD: '/api/v1/analytics/hives/:hiveId/leaderboard',
    SESSION_START: '/api/v1/analytics/session/start',
    SESSION_END: '/api/v1/analytics/session/end',
    USER_STATS_ME: '/api/v1/analytics/user/stats',
    LEADERBOARD: '/api/v1/analytics/leaderboard'
  },
  
  // Chat endpoints - matches ChatRestController (/api/v1/chat)
  CHAT: {
    BASE: '/api/v1/chat',
    HIVE_MESSAGES: '/api/v1/chat/hives/:hiveId/messages',
    HIVE_RECENT: '/api/v1/chat/hives/:hiveId/messages/recent',
    HIVE_AFTER: '/api/v1/chat/hives/:hiveId/messages/after',
    HIVE_ANNOUNCE: '/api/v1/chat/hives/:hiveId/announce'
  },
  
  // Notifications - matches NotificationController (/api/notifications)
  NOTIFICATIONS: {
    BASE: '/api/notifications',
    UNREAD: '/api/notifications/unread',
    UNREAD_COUNT: '/api/notifications/unread/count',
    BY_ID: '/api/notifications/:id',
    BY_TYPE: '/api/notifications/type/:type',
    CLEANUP: '/api/notifications/cleanup'
  },
  
  // Health endpoints - matches HealthController (/api/health)
  HEALTH: {
    BASE: '/api/health',
    DETAILED: '/api/health/detailed'
  },
  
  // Test endpoints - matches SimpleController (/api/test)
  TEST: {
    HEALTH: '/api/test/health',
    PING: '/api/test/ping'
  },
  
  // Forum endpoints (placeholder for future implementation)
  FORUM: {
    BASE: '/api/forum',
    POSTS: '/api/forum/posts',
    CATEGORIES: '/api/forum/categories'
  },
  
  // Music endpoints (placeholder for future implementation)
  MUSIC: {
    BASE: '/api/music',
    RECOMMENDATIONS: '/api/music/recommendations',
    PLAYLISTS: '/api/music/playlists'
  },
  
  // Buddy endpoints - matches BuddyController (/api/buddy)
  BUDDY: {
    BASE: '/api/buddy',
    REQUEST: '/api/buddy/request',
    ACCEPT_REQUEST: '/api/buddy/request/:relationshipId/accept',
    REJECT_REQUEST: '/api/buddy/request/:relationshipId/reject',
    TERMINATE: '/api/buddy/relationship/:relationshipId',
    ACTIVE_RELATIONSHIPS: '/api/buddy/relationships/active',
    PENDING_REQUESTS: '/api/buddy/requests/pending',
    SENT_REQUESTS: '/api/buddy/requests/sent',
    RELATIONSHIP: '/api/buddy/relationship/:relationshipId',
    MATCHES: '/api/buddy/matches',
    MATCH_SCORE: '/api/buddy/match-score/:userId',
    PREFERENCES: '/api/buddy/preferences',
    GOALS: '/api/buddy/relationship/:relationshipId/goals',
    GOAL_BY_ID: '/api/buddy/goals/:goalId',
    COMPLETE_GOAL: '/api/buddy/goals/:goalId/complete',
    ACTIVE_GOALS: '/api/buddy/relationship/:relationshipId/goals/active',
    CHECKIN: '/api/buddy/relationship/:relationshipId/checkin',
    CHECKINS: '/api/buddy/relationship/:relationshipId/checkins',
    CHECKIN_STATS: '/api/buddy/relationship/:relationshipId/checkins/stats',
    SESSIONS: '/api/buddy/relationship/:relationshipId/sessions',
    SESSION_BY_ID: '/api/buddy/sessions/:sessionId',
    START_SESSION: '/api/buddy/sessions/:sessionId/start',
    END_SESSION: '/api/buddy/sessions/:sessionId/end',
    CANCEL_SESSION: '/api/buddy/sessions/:sessionId/cancel',
    RATE_SESSION: '/api/buddy/sessions/:sessionId/rate',
    UPCOMING_SESSIONS: '/api/buddy/sessions/upcoming',
    RELATIONSHIP_STATS: '/api/buddy/relationship/:relationshipId/stats',
    USER_STATS: '/api/buddy/stats'
  }
} as const;

// Utility function to build endpoint URLs with parameters
export function buildEndpoint(endpoint: string, params: Record<string, string | number> = {}): string {
  let url = endpoint;
  
  Object.entries(params).forEach(([key, value]) => {
    url = url.replace(`:${key}`, String(value));
  });
  
  return url;
}

// Export commonly used HTTP status codes
export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  NO_CONTENT: 204,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  CONFLICT: 409,
  VALIDATION_ERROR: 422,
  TOO_MANY_REQUESTS: 429,
  INTERNAL_SERVER_ERROR: 500,
  BAD_GATEWAY: 502,
  SERVICE_UNAVAILABLE: 503,
  GATEWAY_TIMEOUT: 504
} as const;

export default {
  authApiService,
  hiveApiService,
  presenceApiService,
  timerApiService,
  analyticsApiService,
  apiClient,
  API_CONFIG,
  API_ENDPOINTS,
  buildEndpoint,
  HTTP_STATUS
};