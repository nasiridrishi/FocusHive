/**
 * Presence Contracts
 * Core interfaces for user presence and activity tracking in FocusHive
 *
 * The presence system tracks user online status, activity, and focus states
 * across hives and sessions.
 */

import { PresenceStatus, PresenceUpdate } from './websocket';

// Re-export WebSocket types
export type { PresenceStatus, PresenceUpdate } from './websocket';

/**
 * User activity types
 */
export type UserActivityType =
  | 'typing'
  | 'reading'
  | 'idle'
  | 'focused'
  | 'in_break'
  | 'screensharing'
  | 'video_on'
  | 'audio_on'
  | 'presenting';

/**
 * Focus level indicators
 */
export type FocusLevel =
  | 'deep_focus'    // Fully concentrated, do not disturb
  | 'focused'       // Regular focus mode
  | 'available'     // Available for interaction
  | 'break'         // On a break
  | 'away';         // Away from desk

/**
 * Device types for presence
 */
export type DeviceType =
  | 'desktop'
  | 'laptop'
  | 'mobile'
  | 'tablet'
  | 'web';

/**
 * User presence information
 */
export interface UserPresence {
  userId: string;
  username?: string;  // Add missing property
  avatar?: string;    // Add missing property
  status: PresenceStatus;
  focusLevel: FocusLevel;
  lastSeen: string;
  lastActivity: string;
  currentActivity?: UserActivityType;
  currentHiveId?: string;
  currentSessionId?: string;
  device: {
    type: DeviceType;
    id: string;
    name?: string;
    browser?: string;
    os?: string;
  };
  deviceInfo?: {  // Add alias for device
    type?: DeviceType;
    id?: string;
    name?: string;
    browser?: string;
    os?: string;
  };
  location?: {
    timezone: string;
    country?: string;
    city?: string;
  };
  customStatus?: {
    emoji?: string;
    message?: string;
    expiresAt?: string;
  };
  metadata?: Record<string, any>;  // Add missing property
}

/**
 * Presence update request
 */
export interface UpdatePresenceRequest {
  status?: PresenceStatus;
  focusLevel?: FocusLevel;
  activity?: UserActivityType;
  customStatus?: {
    emoji?: string;
    message?: string;
    duration?: number; // in milliseconds
  };
}

/**
 * Hive presence information
 */
export interface HivePresence {
  hiveId: string;
  onlineCount: number;
  totalMembers: number;
  activeUsers: UserPresence[];
  recentlyActive: UserPresence[];
  focusedCount: number;
  inBreakCount: number;
  averageFocusTime?: number;
  peakActivityTime?: string;
}

/**
 * Presence subscription request
 */
export interface PresenceSubscriptionRequest {
  type: 'user' | 'hive' | 'friends';
  targetIds: string[];
  includeActivity?: boolean;
  includeLocation?: boolean;
}

/**
 * Presence event
 */
export interface PresenceEvent {
  id: string;
  userId: string;
  type: 'status_changed' | 'activity_changed' | 'joined_hive' | 'left_hive' | 'focus_started' | 'focus_ended';
  timestamp: string;
  previousValue?: any;
  currentValue: any;
  metadata?: Record<string, any>;
}

/**
 * Presence heartbeat
 */
export interface PresenceHeartbeat {
  userId: string;
  timestamp: number;
  status: PresenceStatus;
  activity?: UserActivityType;
  sessionId?: string;
  metrics?: {
    keystrokes?: number;
    mouseMovements?: number;
    activeWindows?: number;
  };
}

/**
 * Focus session
 */
export interface FocusSession {
  id: string;
  userId: string;
  hiveId?: string;
  startedAt: string;
  endedAt?: string;
  plannedDuration: number; // in milliseconds
  actualDuration?: number; // in milliseconds
  focusLevel: FocusLevel;
  breaks: Array<{
    startedAt: string;
    endedAt?: string;
    duration: number;
    type: 'short' | 'long' | 'emergency';
  }>;
  productivity?: {
    score: number;
    tasksCompleted?: number;
    distractions?: number;
  };
  mood?: {
    before?: string;
    after?: string;
  };
}

/**
 * Presence analytics
 */
export interface PresenceAnalytics {
  userId: string;
  period: 'day' | 'week' | 'month';
  startDate: string;
  endDate: string;
  totalOnlineTime: number; // in milliseconds
  totalFocusTime: number;  // in milliseconds
  totalBreakTime: number;  // in milliseconds
  averageSessionLength: number;
  focusSessions: number;
  completedSessions: number;
  abandonedSessions: number;
  peakHours: Record<string, number>;
  mostActiveHives: Array<{
    hiveId: string;
    hiveName: string;
    timeSpent: number;
  }>;
  focusScore: number;
  streakDays: number;
}

/**
 * Presence settings
 */
export interface PresenceSettings {
  shareStatus: boolean;
  shareActivity: boolean;
  shareLocation: boolean;
  shareFocusLevel: boolean;
  autoAway: boolean;
  autoAwayTimeout: number; // in milliseconds
  focusModeSettings: {
    blockNotifications: boolean;
    autoReplyMessage?: string;
    allowedContacts?: string[];
  };
  privacyMode: 'public' | 'friends' | 'private';
  deviceSync: boolean;
}

/**
 * Bulk presence response
 */
export interface BulkPresenceResponse {
  presences: Record<string, UserPresence>;
  lastUpdated: string;
  nextUpdateIn?: number; // milliseconds until next update
}

/**
 * Presence history
 */
export interface PresenceHistory {
  userId: string;
  date: string;
  events: PresenceEvent[];
  summary: {
    totalOnlineTime: number;
    statusChanges: number;
    hivesVisited: string[];
    peakActivity: string;
  };
}

/**
 * Presence context state
 */
export interface PresenceContextState {
  myPresence: UserPresence | null;
  othersPresence: Record<string, UserPresence>;
  hivePresence: HivePresence | null;
  currentFocusSession: FocusSession | null;
  presenceSettings: PresenceSettings | null;
  isTracking: boolean;
  lastHeartbeat: number | null;
  error: Error | null;
}

/**
 * Presence context methods
 */
export interface PresenceContextMethods {
  updatePresence: (request: UpdatePresenceRequest) => Promise<void>;
  setStatus: (status: PresenceStatus) => Promise<void>;
  setFocusLevel: (level: FocusLevel) => Promise<void>;
  setActivity: (activity: UserActivityType) => Promise<void>;
  setCustomStatus: (emoji: string, message: string, duration?: number) => Promise<void>;
  startFocusSession: (duration: number, hiveId?: string) => Promise<FocusSession>;
  endFocusSession: () => Promise<void>;
  pauseFocusSession: () => Promise<void>;
  resumeFocusSession: () => Promise<void>;
  subscribeToPresence: (request: PresenceSubscriptionRequest) => Promise<void>;
  unsubscribeFromPresence: (targetIds: string[]) => Promise<void>;
  getPresence: (userId: string) => Promise<UserPresence>;
  getBulkPresence: (userIds: string[]) => Promise<BulkPresenceResponse>;
  getHivePresence: (hiveId: string) => Promise<HivePresence>;
  getPresenceHistory: (userId: string, date: string) => Promise<PresenceHistory>;
  getPresenceAnalytics: (period: 'day' | 'week' | 'month') => Promise<PresenceAnalytics>;
  updatePresenceSettings: (settings: Partial<PresenceSettings>) => Promise<void>;
  sendHeartbeat: () => void;
  clearError: () => void;
}

/**
 * Complete Presence context type
 */
export interface PresenceContextType extends PresenceContextState, PresenceContextMethods {}

// Aliases and additional types for compatibility with hooks
export type PresenceSearchParams = {
  userId?: string;
  hiveId?: string;
  status?: PresenceStatus;
  startDate?: string;
  endDate?: string;
};
export type PresenceHistoryEntry = PresenceEvent;
export type PresenceStatistics = PresenceAnalytics;
export type PresenceUpdateRequest = UpdatePresenceRequest; // Renamed to avoid websocket conflict
export type CollaborationSession = FocusSession;
export type UserStatusHistory = PresenceHistory;
export type PresenceConfig = PresenceSettings;
export type BulkPresenceRequest = {
  userIds: string[];
  includeActivity?: boolean;
};
export type ActivityType = UserActivityType; // Alias for hooks
export type UserActivity = UserActivityType; // Another alias
export type SetPresenceRequest = UpdatePresenceRequest; // Legacy alias
export type DeviceInfo = UserPresence['device'];
export type PresenceNotification = {
  id: string;
  userId: string;
  type: 'status_changed' | 'focus_started' | 'focus_ended';
  message: string;
  timestamp: string;
};
export type PresenceWebSocketEvent = {
  type: 'presence_update' | 'focus_session' | 'heartbeat';
  userId: string;
  data: any;
  timestamp: string;
};
