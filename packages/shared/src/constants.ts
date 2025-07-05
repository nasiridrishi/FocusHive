// Timer constants
export const DEFAULT_WORK_DURATION = 25 * 60; // 25 minutes in seconds
export const DEFAULT_SHORT_BREAK = 5 * 60; // 5 minutes
export const DEFAULT_LONG_BREAK = 15 * 60; // 15 minutes

// Socket events
export const SOCKET_EVENTS = {
  // Authentication
  AUTH_SUCCESS: 'auth:success',
  AUTH_ERROR: 'auth:error',

  // Presence
  PRESENCE_UPDATE: 'presence:update',
  PRESENCE_USER_JOINED: 'presence:user-joined',
  PRESENCE_USER_LEFT: 'presence:user-left',

  // Hive
  HIVE_CREATED: 'hive:created',
  HIVE_UPDATED: 'hive:updated',
  HIVE_MEMBER_ADDED: 'hive:member-added',
  HIVE_MEMBER_REMOVED: 'hive:member-removed',

  // Timer
  TIMER_START: 'timer:start',
  TIMER_PAUSE: 'timer:pause',
  TIMER_COMPLETE: 'timer:complete',
  TIMER_SYNC: 'timer:sync',
} as const;
