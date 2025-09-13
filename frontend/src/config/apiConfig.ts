/**
 * API Configuration for FocusHive Microservices
 * 
 * Centralized configuration for all microservice endpoints with:
 * - Environment-based URL configuration
 * - Service-specific endpoint definitions
 * - WebSocket configuration
 * - Default fallback URLs for development
 */

// Service base URLs with environment variables and fallbacks
export const SERVICE_URLS = {
  // Core FocusHive Backend Service (Port 8080)
  BACKEND: import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080',
  
  // Identity Service - OAuth2 & Persona Management (Port 8081)
  IDENTITY: import.meta.env.VITE_IDENTITY_URL || 'http://localhost:8081',
  
  // Music Service - Spotify Integration (Port 8082)
  MUSIC: import.meta.env.VITE_MUSIC_URL || 'http://localhost:8082',
  
  // Notification Service - Multi-channel Notifications (Port 8083)
  NOTIFICATION: import.meta.env.VITE_NOTIFICATION_URL || 'http://localhost:8083',
  
  // Chat Service - Real-time Messaging (Port 8084)
  CHAT: import.meta.env.VITE_CHAT_URL || 'http://localhost:8084',
  
  // Analytics Service - Productivity Tracking (Port 8085)
  ANALYTICS: import.meta.env.VITE_ANALYTICS_URL || 'http://localhost:8085',
  
  // Forum Service - Community Discussions (Port 8086)
  FORUM: import.meta.env.VITE_FORUM_URL || 'http://localhost:8086',
  
  // Buddy Service - Accountability Partners (Port 8087)
  BUDDY: import.meta.env.VITE_BUDDY_URL || 'http://localhost:8087'
} as const;

// WebSocket endpoints configuration
export const WS_ENDPOINTS = {
  // Main WebSocket connection for core features
  MAIN: import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws',
  
  // Chat-specific WebSocket connection
  CHAT: import.meta.env.VITE_CHAT_WS_URL || 'ws://localhost:8084/ws',
  
  // Music service WebSocket for collaborative playlists
  MUSIC: import.meta.env.VITE_MUSIC_WS_URL || 'ws://localhost:8082/ws',
  
  // Analytics WebSocket for real-time metrics
  ANALYTICS: import.meta.env.VITE_ANALYTICS_WS_URL || 'ws://localhost:8085/ws',
  
  // Forum WebSocket for real-time updates
  FORUM: import.meta.env.VITE_FORUM_WS_URL || 'ws://localhost:8086/ws',
  
  // Buddy WebSocket for accountability features
  BUDDY: import.meta.env.VITE_BUDDY_WS_URL || 'ws://localhost:8087/ws'
} as const;

// API endpoint patterns by service
export const API_ENDPOINTS = {
  // Identity Service (Port 8081) - Authentication & Personas
  AUTH: {
    BASE: '/api/v1/auth',
    LOGIN: '/api/v1/auth/login',
    REGISTER: '/api/v1/auth/register',
    REFRESH: '/api/v1/auth/refresh',
    LOGOUT: '/api/v1/auth/logout',
    ME: '/api/v1/auth/me',
    PROFILE: '/api/v1/auth/profile',
    CHANGE_PASSWORD: '/api/v1/auth/change-password',
    FORGOT_PASSWORD: '/api/v1/auth/forgot-password'
  },
  
  PERSONAS: {
    BASE: '/api/v1/personas',
    BY_ID: '/api/v1/personas/:id',
    SWITCH: '/api/v1/personas/:id/switch',
    CREATE: '/api/v1/personas',
    UPDATE: '/api/v1/personas/:id',
    DELETE: '/api/v1/personas/:id'
  },
  
  PRIVACY: {
    BASE: '/api/v1/privacy',
    SETTINGS: '/api/v1/privacy/settings',
    CONSENT: '/api/v1/privacy/consent',
    DATA_EXPORT: '/api/v1/privacy/export',
    DATA_DELETE: '/api/v1/privacy/delete'
  },

  // FocusHive Backend Service (Port 8080) - Core Features
  HIVES: {
    BASE: '/api/v1/hives',
    BY_ID: '/api/v1/hives/:id',
    BY_SLUG: '/api/v1/hives/by-slug/:slug',
    JOIN: '/api/v1/hives/:id/join',
    LEAVE: '/api/v1/hives/:id/leave',
    MEMBERS: '/api/v1/hives/:id/members',
    SEARCH: '/api/v1/hives/search'
  },

  PRESENCE: {
    BASE: '/api/v1/presence',
    USER_BY_ID: '/api/v1/presence/users/:userId',
    ME: '/api/v1/presence/me',
    HIVE: '/api/v1/presence/hives/:hiveId',
    HIVES_BATCH: '/api/v1/presence/hives/batch',
    MY_SESSIONS: '/api/v1/presence/sessions/me',
    HIVE_SESSIONS: '/api/v1/presence/hives/:hiveId/sessions'
  },

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

  // Music Service (Port 8082) - Spotify Integration
  MUSIC: {
    BASE: '/api/v1/music',
    SPOTIFY_AUTH: '/api/v1/spotify/auth',
    SPOTIFY_CALLBACK: '/api/v1/spotify/callback',
    SPOTIFY_CONNECT: '/api/v1/spotify/connect',
    SPOTIFY_DISCONNECT: '/api/v1/spotify/disconnect',
    PLAYLISTS: '/api/v1/music/playlists',
    PLAYLIST_BY_ID: '/api/v1/music/playlists/:id',
    COLLABORATIVE: '/api/v1/music/collaborative',
    RECOMMENDATIONS: '/api/v1/music/recommendations',
    CURRENT_TRACK: '/api/v1/music/current',
    PLAY: '/api/v1/music/play',
    PAUSE: '/api/v1/music/pause',
    SKIP: '/api/v1/music/skip'
  },

  // Notification Service (Port 8083) - Multi-channel Notifications
  NOTIFICATIONS: {
    BASE: '/api/v1/notifications',
    UNREAD: '/api/v1/notifications/unread',
    UNREAD_COUNT: '/api/v1/notifications/unread/count',
    BY_ID: '/api/v1/notifications/:id',
    BY_TYPE: '/api/v1/notifications/type/:type',
    MARK_READ: '/api/v1/notifications/:id/read',
    MARK_ALL_READ: '/api/v1/notifications/read-all',
    DELETE: '/api/v1/notifications/:id',
    SETTINGS: '/api/v1/notifications/settings',
    PREFERENCES: '/api/v1/notifications/preferences'
  },

  // Chat Service (Port 8084) - Real-time Messaging
  CHAT: {
    BASE: '/api/v1/chat',
    HIVE_MESSAGES: '/api/v1/chat/hives/:hiveId/messages',
    HIVE_RECENT: '/api/v1/chat/hives/:hiveId/messages/recent',
    HIVE_AFTER: '/api/v1/chat/hives/:hiveId/messages/after',
    HIVE_ANNOUNCE: '/api/v1/chat/hives/:hiveId/announce',
    DIRECT_MESSAGES: '/api/v1/chat/direct',
    DIRECT_THREAD: '/api/v1/chat/direct/:userId',
    SEND_MESSAGE: '/api/v1/chat/messages',
    MESSAGE_BY_ID: '/api/v1/chat/messages/:id',
    DELETE_MESSAGE: '/api/v1/chat/messages/:id',
    EDIT_MESSAGE: '/api/v1/chat/messages/:id'
  },

  // Analytics Service (Port 8085) - Productivity Tracking
  ANALYTICS: {
    BASE: '/api/v1/analytics',
    START_SESSION: '/api/v1/analytics/sessions/start',
    END_SESSION: '/api/v1/analytics/sessions/:sessionId/end',
    USER_STATS: '/api/v1/analytics/users/:userId/stats',
    HIVE_LEADERBOARD: '/api/v1/analytics/hives/:hiveId/leaderboard',
    PERSONAL_DASHBOARD: '/api/v1/analytics/dashboard',
    PRODUCTIVITY_TRENDS: '/api/v1/analytics/trends',
    FOCUS_PATTERNS: '/api/v1/analytics/focus-patterns',
    WEEKLY_REPORT: '/api/v1/analytics/reports/weekly',
    MONTHLY_REPORT: '/api/v1/analytics/reports/monthly',
    EXPORT_DATA: '/api/v1/analytics/export'
  },

  // Forum Service (Port 8086) - Community Discussions
  FORUM: {
    BASE: '/api/v1/forum',
    POSTS: '/api/v1/forum/posts',
    POST_BY_ID: '/api/v1/forum/posts/:id',
    CATEGORIES: '/api/v1/forum/categories',
    CATEGORY_BY_ID: '/api/v1/forum/categories/:id',
    REPLIES: '/api/v1/forum/posts/:postId/replies',
    REPLY_BY_ID: '/api/v1/forum/replies/:id',
    VOTE_POST: '/api/v1/forum/posts/:id/vote',
    VOTE_REPLY: '/api/v1/forum/replies/:id/vote',
    ACCEPT_REPLY: '/api/v1/forum/replies/:id/accept',
    SEARCH: '/api/v1/forum/search',
    TRENDING: '/api/v1/forum/trending',
    MY_POSTS: '/api/v1/forum/my-posts'
  },

  // Buddy Service (Port 8087) - Accountability Partners
  BUDDY: {
    BASE: '/api/v1/buddy',
    REQUEST: '/api/v1/buddy/request',
    ACCEPT_REQUEST: '/api/v1/buddy/request/:relationshipId/accept',
    REJECT_REQUEST: '/api/v1/buddy/request/:relationshipId/reject',
    TERMINATE: '/api/v1/buddy/relationship/:relationshipId',
    ACTIVE_RELATIONSHIPS: '/api/v1/buddy/relationships/active',
    PENDING_REQUESTS: '/api/v1/buddy/requests/pending',
    SENT_REQUESTS: '/api/v1/buddy/requests/sent',
    RELATIONSHIP: '/api/v1/buddy/relationship/:relationshipId',
    MATCHES: '/api/v1/buddy/matches',
    MATCH_SCORE: '/api/v1/buddy/match-score/:userId',
    PREFERENCES: '/api/v1/buddy/preferences',
    GOALS: '/api/v1/buddy/relationship/:relationshipId/goals',
    GOAL_BY_ID: '/api/v1/buddy/goals/:goalId',
    COMPLETE_GOAL: '/api/v1/buddy/goals/:goalId/complete',
    ACTIVE_GOALS: '/api/v1/buddy/relationship/:relationshipId/goals/active',
    CHECKIN: '/api/v1/buddy/relationship/:relationshipId/checkin',
    CHECKINS: '/api/v1/buddy/relationship/:relationshipId/checkins',
    CHECKIN_STATS: '/api/v1/buddy/relationship/:relationshipId/checkins/stats',
    SESSIONS: '/api/v1/buddy/relationship/:relationshipId/sessions',
    SESSION_BY_ID: '/api/v1/buddy/sessions/:sessionId',
    START_SESSION: '/api/v1/buddy/sessions/:sessionId/start',
    END_SESSION: '/api/v1/buddy/sessions/:sessionId/end',
    CANCEL_SESSION: '/api/v1/buddy/sessions/:sessionId/cancel',
    RATE_SESSION: '/api/v1/buddy/sessions/:sessionId/rate',
    UPCOMING_SESSIONS: '/api/v1/buddy/sessions/upcoming',
    RELATIONSHIP_STATS: '/api/v1/buddy/relationship/:relationshipId/stats',
    USER_STATS: '/api/v1/buddy/stats'
  }
} as const;

// Service URL mapping for API clients
export const SERVICE_ENDPOINTS = {
  AUTH: SERVICE_URLS.IDENTITY,
  PERSONAS: SERVICE_URLS.IDENTITY,
  PRIVACY: SERVICE_URLS.IDENTITY,
  HIVES: SERVICE_URLS.BACKEND,
  PRESENCE: SERVICE_URLS.BACKEND,
  TIMER: SERVICE_URLS.BACKEND,
  MUSIC: SERVICE_URLS.MUSIC,
  NOTIFICATIONS: SERVICE_URLS.NOTIFICATION,
  CHAT: SERVICE_URLS.CHAT,
  ANALYTICS: SERVICE_URLS.ANALYTICS,
  FORUM: SERVICE_URLS.FORUM,
  BUDDY: SERVICE_URLS.BUDDY
} as const;

// Utility function to build endpoint URLs with parameters
export function buildEndpoint(endpoint: string, params: Record<string, string | number> = {}): string {
  let url = endpoint;
  
  Object.entries(params).forEach(([key, value]) => {
    url = url.replace(`:${key}`, String(value));
  });
  
  return url;
}

// Utility function to get full URL for a service endpoint
export function getServiceUrl(service: keyof typeof SERVICE_ENDPOINTS, endpoint: string): string {
  const baseUrl = SERVICE_ENDPOINTS[service];
  return `${baseUrl}${endpoint}`;
}

// WebSocket connection configuration
export const WEBSOCKET_CONFIG = {
  // Connection settings
  RECONNECT_ATTEMPTS: parseInt(import.meta.env.VITE_WEBSOCKET_RECONNECT_ATTEMPTS || '10'),
  RECONNECT_DELAY: parseInt(import.meta.env.VITE_WEBSOCKET_RECONNECT_DELAY || '1000'),
  HEARTBEAT_INTERVAL: parseInt(import.meta.env.VITE_WEBSOCKET_HEARTBEAT_INTERVAL || '30000'),
  MAX_RECONNECT_DELAY: 30000, // 30 seconds max
  
  // Service-specific WebSocket URLs
  URLS: WS_ENDPOINTS
} as const;

// HTTP status codes for consistent error handling
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

// Timeout configurations
export const TIMEOUT_CONFIG = {
  DEFAULT: 10000, // 10 seconds
  FILE_UPLOAD: 60000, // 60 seconds for file uploads
  WEBSOCKET: 5000, // 5 seconds for WebSocket connections
  HEALTH_CHECK: 5000 // 5 seconds for health checks
} as const;

// Export default configuration object
export default {
  SERVICE_URLS,
  WS_ENDPOINTS,
  API_ENDPOINTS,
  SERVICE_ENDPOINTS,
  WEBSOCKET_CONFIG,
  HTTP_STATUS,
  TIMEOUT_CONFIG,
  buildEndpoint,
  getServiceUrl
};