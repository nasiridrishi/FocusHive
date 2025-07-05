// Socket Events
export const SOCKET_EVENTS = {
  // Connection events
  CONNECT: 'connect',
  DISCONNECT: 'disconnect',
  AUTH: 'user:authenticate',
  AUTHENTICATED: 'user:authenticated',
  UNAUTHORIZED: 'user:unauthorized',

  // Room events
  JOIN_ROOM: 'room:join',
  LEAVE_ROOM: 'room:leave',
  ROOM_JOINED: 'room:joined',
  USER_JOINED: 'room:user-joined',
  USER_LEFT: 'room:user-left',
  PARTICIPANT_UPDATE: 'participant:update',
  PARTICIPANT_UPDATED: 'room:participant-updated',

  // Timer events
  TIMER_START: 'timer:start',
  TIMER_PAUSE: 'timer:pause',
  TIMER_SKIP: 'timer:skip',
  TIMER_TICK: 'timer:tick',
  TIMER_PHASE_CHANGE: 'timer:phase-change',
  TIMER_STARTED: 'timer:started',
  TIMER_PAUSED: 'timer:paused',

  // Chat events
  CHAT_MESSAGE: 'chat:message',
  CHAT_NEW_MESSAGE: 'chat:new-message',
  CHAT_ENABLED: 'chat:enabled',

  // Buddy events
  BUDDY_FIND: 'buddy:find',
  BUDDY_CANCEL: 'buddy:cancel',
  BUDDY_MATCHED: 'buddy:matched',

  // Stats events
  STATS_UPDATED: 'stats:updated',

  // Error events
  ERROR: 'error'
} as const;

// Default Settings
export const DEFAULT_SETTINGS = {
  POMODORO: {
    FOCUS_DURATION: 25, // minutes
    SHORT_BREAK_DURATION: 5, // minutes
    LONG_BREAK_DURATION: 15, // minutes
    SESSIONS_UNTIL_LONG_BREAK: 4
  },
  ROOM: {
    MAX_PARTICIPANTS: 12,
    DEFAULT_TYPE: 'Mixed' as const
  },
  POINTS: {
    PER_MINUTE: 1,
    STREAK_BONUS: 10,
    COMPLETION_BONUS: 5
  }
} as const;

// API Endpoints
export const API_ENDPOINTS = {
  AUTH: {
    REGISTER: '/api/auth/register',
    LOGIN: '/api/auth/login',
    LOGOUT: '/api/auth/logout',
    ME: '/api/auth/me'
  },
  USERS: {
    GET: '/api/users/:id',
    UPDATE: '/api/users/:id',
    ONLINE: '/api/users/online',
    BUDDIES: '/api/users/buddies'
  },
  ROOMS: {
    LIST: '/api/rooms',
    CREATE: '/api/rooms',
    GET: '/api/rooms/:id',
    DELETE: '/api/rooms/:id',
    PARTICIPANTS: '/api/rooms/:id/participants'
  },
  STATS: {
    LEADERBOARD: '/api/stats/leaderboard',
    USER: '/api/stats/user/:id',
    SESSION: '/api/stats/session'
  }
} as const;