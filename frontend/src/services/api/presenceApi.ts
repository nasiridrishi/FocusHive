import axios, { AxiosInstance, AxiosError } from 'axios';
import { apiClient } from './httpInterceptors';
import { API_ENDPOINTS, buildEndpoint } from './index';

/**
 * Presence API Service
 * 
 * Provides real-time presence and activity tracking with:
 * - User presence status management
 * - Hive presence tracking
 * - Focus session monitoring
 * - Batch operations for performance
 */

export interface UserPresence {
  userId: number;
  status: PresenceStatus;
  lastSeen: string;
  currentActivity?: string;
  isInFocusSession: boolean;
  focusSession?: FocusSession;
}

export interface FocusSession {
  id?: number;
  userId: number;
  hiveId?: number;
  startTime: string;
  endTime?: string;
  sessionType: 'POMODORO' | 'FOCUS' | 'BREAK' | 'CUSTOM';
  targetDuration?: number; // in minutes
  actualDuration?: number; // in minutes
  task?: string;
  isActive: boolean;
  metadata?: Record<string, any>;
}

export interface HivePresenceInfo {
  hiveId: number;
  totalMembers: number;
  activeMembers: number;
  membersInFocus: number;
  presences: UserPresence[];
  lastUpdated: string;
}

export interface PresenceUpdate {
  status: PresenceStatus;
  activity?: string;
  hiveId?: number;
  metadata?: Record<string, any>;
}

export interface SessionBroadcast {
  sessionId: number;
  userId: number;
  hiveId?: number;
  type: 'SESSION_STARTED' | 'SESSION_ENDED' | 'SESSION_PAUSED' | 'SESSION_RESUMED';
  session: FocusSession;
  timestamp: string;
}

export interface PresenceBroadcast {
  userId: number;
  hiveId?: number;
  type: 'PRESENCE_UPDATED' | 'USER_JOINED' | 'USER_LEFT';
  presence: UserPresence;
  timestamp: string;
}

export enum PresenceStatus {
  ONLINE = 'ONLINE',
  AWAY = 'AWAY',
  BUSY = 'BUSY',
  OFFLINE = 'OFFLINE',
  IN_FOCUS = 'IN_FOCUS',
  ON_BREAK = 'ON_BREAK'
}

class PresenceApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = apiClient;
  }

  /**
   * Get user presence by ID
   */
  async getUserPresence(userId: number): Promise<UserPresence> {
    try {
      const endpoint = buildEndpoint(API_ENDPOINTS.PRESENCE.USER_BY_ID, { userId });
      const response = await this.api.get<UserPresence>(endpoint);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get user presence');
      throw error;
    }
  }

  /**
   * Get current user's presence
   */
  async getMyPresence(): Promise<UserPresence> {
    try {
      const response = await this.api.get<UserPresence>(API_ENDPOINTS.PRESENCE.ME);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get current user presence');
      throw error;
    }
  }

  /**
   * Update current user's presence
   */
  async updateMyPresence(update: PresenceUpdate): Promise<UserPresence> {
    try {
      const response = await this.api.put<UserPresence>(API_ENDPOINTS.PRESENCE.ME, update);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to update presence');
      throw error;
    }
  }

  /**
   * Get hive presence information
   */
  async getHivePresence(hiveId: number): Promise<HivePresenceInfo> {
    try {
      const endpoint = buildEndpoint(API_ENDPOINTS.PRESENCE.HIVE, { hiveId });
      const response = await this.api.get<HivePresenceInfo>(endpoint);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get hive presence');
      throw error;
    }
  }

  /**
   * Get presence for multiple hives (batch operation)
   */
  async getHivePresenceBatch(hiveIds: number[]): Promise<HivePresenceInfo[]> {
    try {
      const response = await this.api.post<HivePresenceInfo[]>(
        API_ENDPOINTS.PRESENCE.HIVES_BATCH,
        { hiveIds }
      );
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get batch hive presence');
      throw error;
    }
  }

  /**
   * Get current user's focus sessions
   */
  async getMySessions(): Promise<FocusSession[]> {
    try {
      const response = await this.api.get<FocusSession[]>(API_ENDPOINTS.PRESENCE.MY_SESSIONS);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get user sessions');
      throw error;
    }
  }

  /**
   * Get focus sessions for a specific hive
   */
  async getHiveSessions(hiveId: number): Promise<FocusSession[]> {
    try {
      const endpoint = buildEndpoint(API_ENDPOINTS.PRESENCE.HIVE_SESSIONS, { hiveId });
      const response = await this.api.get<FocusSession[]>(endpoint);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get hive sessions');
      throw error;
    }
  }

  /**
   * Start a focus session
   */
  async startFocusSession(session: Omit<FocusSession, 'id' | 'startTime' | 'isActive'>): Promise<FocusSession> {
    try {
      // This would typically go to a timer or session endpoint, but using presence for now
      const sessionData = {
        ...session,
        startTime: new Date().toISOString(),
        isActive: true
      };
      
      const response = await this.api.post<FocusSession>('/api/v1/timer/sessions/start', sessionData);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to start focus session');
      throw error;
    }
  }

  /**
   * End a focus session
   */
  async endFocusSession(sessionId: number): Promise<FocusSession> {
    try {
      const response = await this.api.post<FocusSession>(`/api/v1/timer/sessions/${sessionId}/end`);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to end focus session');
      throw error;
    }
  }

  /**
   * Update presence status
   */
  async updateStatus(status: PresenceStatus, activity?: string, hiveId?: number): Promise<UserPresence> {
    try {
      const update: PresenceUpdate = { status, activity, hiveId };
      return await this.updateMyPresence(update);
    } catch (error) {
      this.handleError(error, 'Failed to update status');
      throw error;
    }
  }

  /**
   * Set user as online
   */
  async setOnline(activity?: string): Promise<UserPresence> {
    return this.updateStatus(PresenceStatus.ONLINE, activity);
  }

  /**
   * Set user as away
   */
  async setAway(): Promise<UserPresence> {
    return this.updateStatus(PresenceStatus.AWAY);
  }

  /**
   * Set user as busy
   */
  async setBusy(activity?: string): Promise<UserPresence> {
    return this.updateStatus(PresenceStatus.BUSY, activity);
  }

  /**
   * Set user as offline
   */
  async setOffline(): Promise<UserPresence> {
    return this.updateStatus(PresenceStatus.OFFLINE);
  }

  /**
   * Join a hive's presence tracking
   */
  async joinHivePresence(hiveId: number): Promise<UserPresence> {
    try {
      const update: PresenceUpdate = { 
        status: PresenceStatus.ONLINE, 
        hiveId,
        activity: 'Joined hive'
      };
      return await this.updateMyPresence(update);
    } catch (error) {
      this.handleError(error, 'Failed to join hive presence');
      throw error;
    }
  }

  /**
   * Leave a hive's presence tracking
   */
  async leaveHivePresence(hiveId: number): Promise<UserPresence> {
    try {
      const update: PresenceUpdate = { 
        status: PresenceStatus.ONLINE,
        activity: 'Left hive'
      };
      return await this.updateMyPresence(update);
    } catch (error) {
      this.handleError(error, 'Failed to leave hive presence');
      throw error;
    }
  }

  /**
   * Get presence statistics for analytics
   */
  async getPresenceStats(hiveId?: number, dateRange?: { start: string; end: string }): Promise<any> {
    try {
      const params = hiveId ? { hiveId, ...dateRange } : dateRange;
      const response = await this.api.get('/api/v1/analytics/presence/stats', { params });
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get presence statistics');
      throw error;
    }
  }

  /**
   * Error handler for consistent error formatting
   */
  private handleError(error: unknown, defaultMessage: string): never {
    if (error instanceof AxiosError) {
      const message = error.response?.data?.message || 
                     error.response?.data?.error || 
                     defaultMessage;
      throw new Error(message);
    }
    throw new Error('Network error occurred');
  }
}

// Export singleton instance
export const presenceApiService = new PresenceApiService();

export default presenceApiService;