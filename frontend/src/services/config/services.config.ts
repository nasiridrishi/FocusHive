/**
 * Service Configuration
 *
 * Centralized configuration for all FocusHive microservices.
 * Aligned with SERVICE_INTEGRATION_GUIDE.md
 */

interface ServiceEndpoints {
  IDENTITY: string;
  BACKEND: string;
  NOTIFICATION: string;
  BUDDY: string;
  FRONTEND: string;
  // Legacy services (being consolidated into backend)
  CHAT?: string;
  ANALYTICS?: string;
  FORUM?: string;
  MUSIC?: string;
}

interface WebSocketEndpoints {
  BACKEND: string;
  STOMP: string;
  // Service-specific WebSocket connections
  CHAT?: string;
  ANALYTICS?: string;
  FORUM?: string;
  BUDDY?: string;
}

/**
 * Get service URLs based on environment
 */
export function getServiceUrls(): ServiceEndpoints {
  const isDev = import.meta.env.DEV;

  if (isDev) {
    return {
      IDENTITY: import.meta.env.VITE_IDENTITY_URL || 'http://localhost:8081',
      BACKEND: import.meta.env.VITE_BACKEND_URL || 'https://identity.focushive.app',
      NOTIFICATION: import.meta.env.VITE_NOTIFICATION_URL || 'http://localhost:8083',
      BUDDY: import.meta.env.VITE_BUDDY_URL || 'http://localhost:8087',
      FRONTEND: import.meta.env.VITE_FRONTEND_URL || 'http://localhost:5173',
      // Legacy - these are now part of backend
      CHAT: import.meta.env.VITE_CHAT_URL || 'https://identity.focushive.app',
      ANALYTICS: import.meta.env.VITE_ANALYTICS_URL || 'https://identity.focushive.app',
      FORUM: import.meta.env.VITE_FORUM_URL || 'https://identity.focushive.app',
      MUSIC: import.meta.env.VITE_MUSIC_URL || 'http://localhost:8082',
    };
  }

  // Production URLs
  return {
    IDENTITY: 'https://identity.focushive.app',
    BACKEND: 'https://backend.focushive.app',
    NOTIFICATION: 'https://notification.focushive.app',
    BUDDY: 'https://buddy.focushive.app',
    FRONTEND: 'https://app.focushive.app',
  };
}

/**
 * Get WebSocket URLs based on environment
 */
export function getWebSocketUrls(): WebSocketEndpoints {
  const isDev = import.meta.env.DEV;

  if (isDev) {
    return {
      BACKEND: import.meta.env.VITE_WS_URL || 'wss://identity.focushive.app/ws',
      STOMP: import.meta.env.VITE_WS_URL || 'wss://identity.focushive.app/ws',
      // Service-specific (optional)
      CHAT: import.meta.env.VITE_CHAT_WS_URL || 'wss://identity.focushive.app/ws',
      ANALYTICS: import.meta.env.VITE_ANALYTICS_WS_URL || 'wss://identity.focushive.app/ws',
      FORUM: import.meta.env.VITE_FORUM_WS_URL || 'wss://identity.focushive.app/ws',
      BUDDY: import.meta.env.VITE_BUDDY_WS_URL || 'ws://localhost:8087/ws',
    };
  }

  // Production WebSocket URLs
  return {
    BACKEND: 'wss://backend.focushive.app/ws',
    STOMP: 'wss://backend.focushive.app/ws',
  };
}

/**
 * Service health check endpoints
 */
export function getHealthCheckUrls() {
  const services = getServiceUrls();

  return {
    identity: `${services.IDENTITY}/actuator/health`,
    backend: `${services.BACKEND}/actuator/health`,
    notification: `${services.NOTIFICATION}/actuator/health`,
    buddy: `${services.BUDDY}/actuator/health`,
  };
}

/**
 * API endpoints configuration
 */
export const API_ENDPOINTS = {
  // Authentication (Identity Service)
  AUTH: {
    REGISTER: '/api/v1/auth/register',
    LOGIN: '/api/v1/auth/login',
    LOGOUT: '/api/v1/auth/logout',
    REFRESH: '/api/v1/auth/refresh',
    VERIFY_EMAIL: '/api/v1/auth/verify-email',
    RESET_PASSWORD: '/api/v1/auth/reset-password',
    CHANGE_PASSWORD: '/api/v1/auth/change-password',
    ME: '/api/v1/auth/me',
  },

  // Persona Management (Identity Service)
  PERSONAS: {
    LIST: '/api/v1/personas',
    CREATE: '/api/v1/personas',
    GET: '/api/v1/personas/:id',
    UPDATE: '/api/v1/personas/:id',
    DELETE: '/api/v1/personas/:id',
    ACTIVATE: '/api/v1/personas/:id/activate',
  },

  // Hive Management (Backend Service)
  HIVES: {
    LIST: '/api/v1/hives',
    CREATE: '/api/v1/hives',
    GET: '/api/v1/hives/:id',
    UPDATE: '/api/v1/hives/:id',
    DELETE: '/api/v1/hives/:id',
    JOIN: '/api/v1/hives/:id/join',
    LEAVE: '/api/v1/hives/:id/leave',
    MEMBERS: '/api/v1/hives/:id/members',
    INVITE: '/api/v1/hives/:id/invite',
  },

  // Presence (Backend Service)
  PRESENCE: {
    GET_HIVE: '/api/v1/presence/hive/:hiveId',
    UPDATE: '/api/v1/presence/update',
    HEARTBEAT: '/api/v1/presence/heartbeat',
    SET_STATUS: '/api/v1/presence/status',
  },

  // Timer Sessions (Backend Service)
  TIMER: {
    START: '/api/v1/timer/start',
    END: '/api/v1/timer/sessions/:sessionId/end',
    PAUSE: '/api/v1/timer/sessions/:sessionId/pause',
    RESUME: '/api/v1/timer/sessions/:sessionId/resume',
    GET_SESSION: '/api/v1/timer/sessions/:sessionId',
    ACTIVE_SESSIONS: '/api/v1/timer/hives/:hiveId/sessions',
    USER_SESSIONS: '/api/v1/timer/users/:userId/sessions',
  },

  // Chat (Backend Service - Chat Module)
  CHAT: {
    SEND: '/api/chat/send',
    HISTORY: '/api/chat/hive/:hiveId/history',
    THREAD: '/api/chat/thread/:messageId',
    DELETE: '/api/chat/message/:messageId',
    EDIT: '/api/chat/message/:messageId',
    REACT: '/api/chat/message/:messageId/react',
  },

  // Analytics (Backend Service - Analytics Module)
  ANALYTICS: {
    USER_SUMMARY: '/api/analytics/users/:userId/summary',
    HIVE_SUMMARY: '/api/analytics/hive/:hiveId/summary',
    PRODUCTIVITY: '/api/analytics/productivity',
    GOALS: '/api/analytics/goals',
    ACHIEVEMENTS: '/api/analytics/achievements',
  },

  // Forum (Backend Service - Forum Module)
  FORUM: {
    THREADS: '/api/forum/threads',
    CREATE_THREAD: '/api/forum/threads',
    GET_THREAD: '/api/forum/threads/:threadId',
    REPLY: '/api/forum/threads/:threadId/reply',
    UPVOTE: '/api/forum/posts/:postId/upvote',
    DOWNVOTE: '/api/forum/posts/:postId/downvote',
  },

  // Notifications (Notification Service)
  NOTIFICATIONS: {
    LIST: '/api/v1/notifications',
    READ: '/api/v1/notifications/:id/read',
    READ_ALL: '/api/v1/notifications/read-all',
    PREFERENCES: '/api/v1/preferences',
    UPDATE_PREFERENCES: '/api/v1/preferences',
    SUBSCRIBE_PUSH: '/api/v1/push/subscribe',
    UNSUBSCRIBE_PUSH: '/api/v1/push/unsubscribe',
  },

  // Buddy System (Buddy Service)
  BUDDY: {
    FIND_MATCH: '/api/v1/buddy/match',
    CURRENT: '/api/v1/buddy/current',
    REQUEST: '/api/v1/buddy/request/:userId',
    RESPOND: '/api/v1/buddy/requests/:requestId',
    END_PARTNERSHIP: '/api/v1/buddy/end',
    HISTORY: '/api/v1/buddy/history',
  },
};

/**
 * WebSocket event types
 */
export const WS_EVENTS = {
  // Connection events
  CONNECT: 'connect',
  DISCONNECT: 'disconnect',
  ERROR: 'error',

  // Presence events
  PRESENCE_UPDATE: 'presence.update',
  USER_JOINED: 'presence.joined',
  USER_LEFT: 'presence.left',
  STATUS_CHANGED: 'presence.status',

  // Hive events
  HIVE_UPDATE: 'hive.update',
  HIVE_MEMBER_ADDED: 'hive.member.added',
  HIVE_MEMBER_REMOVED: 'hive.member.removed',

  // Chat events
  MESSAGE_RECEIVED: 'chat.message',
  MESSAGE_EDITED: 'chat.edited',
  MESSAGE_DELETED: 'chat.deleted',
  TYPING_START: 'chat.typing.start',
  TYPING_STOP: 'chat.typing.stop',

  // Timer events
  SESSION_STARTED: 'timer.started',
  SESSION_ENDED: 'timer.ended',
  SESSION_PAUSED: 'timer.paused',
  SESSION_RESUMED: 'timer.resumed',

  // Notification events
  NOTIFICATION_RECEIVED: 'notification.received',

  // Buddy events
  BUDDY_REQUEST: 'buddy.request',
  BUDDY_ACCEPTED: 'buddy.accepted',
  BUDDY_REJECTED: 'buddy.rejected',
  BUDDY_ENDED: 'buddy.ended',
};

/**
 * WebSocket STOMP destinations
 */
export const STOMP_DESTINATIONS = {
  // Subscribe destinations (receive messages)
  SUBSCRIBE: {
    // Personal queues
    USER_NOTIFICATIONS: '/user/queue/notifications',
    USER_MESSAGES: '/user/queue/messages',
    USER_ERRORS: '/user/queue/errors',

    // Hive-specific topics
    HIVE_PRESENCE: '/topic/presence/:hiveId',
    HIVE_UPDATES: '/topic/hive/:hiveId',
    HIVE_CHAT: '/topic/chat/:hiveId',
    HIVE_TIMER: '/topic/timer/:hiveId',
  },

  // Send destinations (send messages)
  SEND: {
    CHAT_MESSAGE: '/app/chat/send',
    PRESENCE_UPDATE: '/app/presence/update',
    TIMER_UPDATE: '/app/timer/update',
    HEARTBEAT: '/app/heartbeat',
  },
};

/**
 * Configuration for request timeouts and retries
 */
export const REQUEST_CONFIG = {
  DEFAULT_TIMEOUT: 30000, // 30 seconds
  UPLOAD_TIMEOUT: 120000, // 2 minutes for file uploads
  RETRY_ATTEMPTS: 3,
  RETRY_DELAY: 1000, // 1 second
  EXPONENTIAL_BACKOFF: true,

  // Service-specific timeouts
  TIMEOUTS: {
    AUTH: 10000, // 10 seconds
    HIVE: 15000, // 15 seconds
    CHAT: 5000, // 5 seconds (real-time)
    ANALYTICS: 30000, // 30 seconds (heavy queries)
    NOTIFICATION: 10000, // 10 seconds
  },
};

/**
 * Rate limiting configuration
 */
export const RATE_LIMITS = {
  PUBLIC_API_REQUESTS_PER_HOUR: 100,
  AUTHENTICATED_API_REQUESTS_PER_HOUR: 1000,
  WEBSOCKET_MESSAGES_PER_MINUTE: 60,
  CHAT_MESSAGES_PER_MINUTE: 30,
  FILE_UPLOADS_PER_DAY: 100,
};

/**
 * Security configuration
 */
export const SECURITY_CONFIG = {
  // Token storage keys
  ACCESS_TOKEN_KEY: 'focushive_access_token',
  REFRESH_TOKEN_KEY: 'focushive_refresh_token',
  USER_ID_KEY: 'focushive_user_id',
  PERSONA_ID_KEY: 'focushive_persona_id',

  // Token refresh
  TOKEN_REFRESH_THRESHOLD: 300000, // Refresh 5 minutes before expiry

  // Content Security Policy
  CSP: {
    'default-src': ["'self'"],
    'connect-src': [
      "'self'",
      'https://*.focushive.app',
      'wss://*.focushive.app',
      'http://localhost:*',
      'ws://localhost:*',
    ],
    'img-src': ["'self'", 'data:', 'https:'],
    'script-src': ["'self'", "'unsafe-inline'"],
    'style-src': ["'self'", "'unsafe-inline'"],
  },
};

export default {
  getServiceUrls,
  getWebSocketUrls,
  getHealthCheckUrls,
  API_ENDPOINTS,
  WS_EVENTS,
  STOMP_DESTINATIONS,
  REQUEST_CONFIG,
  RATE_LIMITS,
  SECURITY_CONFIG,
};