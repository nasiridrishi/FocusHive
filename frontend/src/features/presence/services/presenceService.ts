import { webSocketService } from '@/services/websocket/WebSocketService';
import { authService } from '@/services/auth/authService';
import type {
  UserPresence,
  SetPresenceRequest,
  PresenceStatus,
  HivePresence,
  PresenceHeartbeat,
  UserActivity,
  PresenceStatistics,
  CollaborationSession,
  BulkPresenceRequest,
  BulkPresenceResponse,
  PresenceUpdate,
  PresenceHistoryEntry,
  UserStatusHistory,
  PresenceSearchParams,
  PresenceConfig,
} from '@/contracts/presence';
import type { IMessage } from '@stomp/stompjs';
import type { PaginatedResponse } from '@/contracts/common';

/**
 * PresenceService - Business logic layer for presence and activity tracking
 *
 * This service provides:
 * - User presence status management (online, away, busy, offline)
 * - Activity tracking (working, studying, break, etc.)
 * - Real-time presence updates via WebSocket
 * - Automatic heartbeat mechanism
 * - Hive presence aggregation
 * - Auto-away detection
 * - Collaboration sessions
 * - Presence statistics and history
 * - Caching for performance
 */
export class PresenceService {
  private apiUrl = 'http://localhost:8080/api/v1/presence';
  private presenceCache: Map<number, { presence: UserPresence; timestamp: number }> = new Map();
  private hivePresenceCache: Map<number, { presence: HivePresence; timestamp: number }> = new Map();
  private cacheTimeout = 1 * 60 * 1000; // 1 minute for presence (shorter than other caches)
  private subscriptions: Map<string, () => void> = new Map();

  // Heartbeat management
  private heartbeatInterval: NodeJS.Timeout | null = null;
  private heartbeatIntervalMs = 30000; // 30 seconds
  private activeHives: Set<number> = new Set();
  private currentStatus: PresenceStatus = 'ONLINE';
  private currentActivity: UserActivity | undefined;

  // Auto-away detection
  private autoAwayTimer: NodeJS.Timeout | null = null;
  private autoAwayThreshold = 5 * 60 * 1000; // 5 minutes
  private lastActivityTime = Date.now();
  private autoAwayEnabled = false;

  constructor() {
    // Start heartbeat automatically when service is created
    if (authService.isAuthenticated()) {
      this.startHeartbeat();
    }
  }

  /**
   * Set user presence status
   */
  async setPresence(request: SetPresenceRequest): Promise<UserPresence> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(this.apiUrl, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error(`Failed to set presence: ${response.status} ${response.statusText}`);
      }

      const presence = await response.json();

      // Update local state
      this.currentStatus = presence.status;
      this.currentActivity = presence.activity;

      // Cache the presence
      this.cacheUserPresence(presence);

      // Notify via WebSocket
      this.notifyPresenceUpdate(presence);

      // Reset auto-away timer if setting online
      if (presence.status === 'ONLINE' && this.autoAwayEnabled) {
        this.resetAutoAwayTimer();
      }

      return presence;
    } catch (error) {
      console.error('Error setting presence:', error);
      throw error;
    }
  }

  /**
   * Set user as offline
   */
  async setOffline(): Promise<UserPresence> {
    return this.setPresence({ status: 'OFFLINE' });
  }

  /**
   * Get presence for a specific user
   */
  async getUserPresence(userId: number): Promise<UserPresence> {
    // Check cache first
    const cached = this.getCachedUserPresence(userId);
    if (cached) {
      return cached;
    }

    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/users/${userId}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch user presence: ${response.status} ${response.statusText}`);
      }

      const presence = await response.json();

      // Cache the presence
      this.cacheUserPresence(presence);

      return presence;
    } catch (error) {
      console.error('Error fetching user presence:', error);
      throw error;
    }
  }

  /**
   * Get presence for all users in a hive
   */
  async getHivePresence(hiveId: number): Promise<HivePresence> {
    // Check cache first
    const cached = this.getCachedHivePresence(hiveId);
    if (cached) {
      return cached;
    }

    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/hives/${hiveId}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch hive presence: ${response.status} ${response.statusText}`);
      }

      const hivePresence = await response.json();

      // Cache the hive presence
      this.cacheHivePresence(hivePresence);

      // Cache individual user presences
      hivePresence.activeUsers?.forEach((user: UserPresence) => {
        this.cacheUserPresence(user);
      });

      return hivePresence;
    } catch (error) {
      console.error('Error fetching hive presence:', error);
      throw error;
    }
  }

  /**
   * Get presence for multiple users
   */
  async getBulkPresence(userIds: number[]): Promise<BulkPresenceResponse> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/bulk`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ userIds }),
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch bulk presence: ${response.status} ${response.statusText}`);
      }

      const bulkResponse = await response.json();

      // Cache individual presences
      bulkResponse.presences?.forEach((presence: UserPresence) => {
        this.cacheUserPresence(presence);
      });

      return bulkResponse;
    } catch (error) {
      console.error('Error fetching bulk presence:', error);
      throw error;
    }
  }

  /**
   * Get presence statistics for a user
   */
  async getStatistics(userId: number): Promise<PresenceStatistics> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/users/${userId}/statistics`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch presence statistics: ${response.status} ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error fetching presence statistics:', error);
      throw error;
    }
  }

  /**
   * Get presence history for a user
   */
  async getPresenceHistory(userId: number, startDate: string, endDate: string): Promise<UserStatusHistory> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const params = new URLSearchParams({ startDate, endDate });
      const response = await fetch(`${this.apiUrl}/users/${userId}/history?${params}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch presence history: ${response.status} ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error fetching presence history:', error);
      throw error;
    }
  }

  /**
   * Create a collaboration session
   */
  async createCollaborationSession(
    hiveId: number,
    config: { type: 'POMODORO' | 'BRAINSTORM' | 'CODE_REVIEW' | 'STUDY_SESSION'; duration?: number }
  ): Promise<CollaborationSession> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/collaboration`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          hiveId,
          sharedActivity: {
            type: config.type,
            duration: config.duration,
            startedAt: new Date().toISOString(),
          },
        }),
      });

      if (!response.ok) {
        throw new Error(`Failed to create collaboration session: ${response.status} ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error creating collaboration session:', error);
      throw error;
    }
  }

  /**
   * Join a collaboration session
   */
  async joinCollaborationSession(sessionId: string): Promise<void> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/collaboration/${sessionId}/join`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to join collaboration session: ${response.status} ${response.statusText}`);
      }
    } catch (error) {
      console.error('Error joining collaboration session:', error);
      throw error;
    }
  }

  /**
   * Leave a collaboration session
   */
  async leaveCollaborationSession(sessionId: string): Promise<void> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/collaboration/${sessionId}/leave`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to leave collaboration session: ${response.status} ${response.statusText}`);
      }
    } catch (error) {
      console.error('Error leaving collaboration session:', error);
      throw error;
    }
  }

  // Heartbeat Management

  /**
   * Start automatic heartbeat
   */
  startHeartbeat(): void {
    // Clear any existing interval
    this.stopHeartbeat();

    // Send initial heartbeat
    this.sendHeartbeat();

    // Set up regular heartbeat
    this.heartbeatInterval = setInterval(() => {
      this.sendHeartbeat();
    }, this.heartbeatIntervalMs);
  }

  /**
   * Stop automatic heartbeat
   */
  stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  /**
   * Send a heartbeat
   */
  sendHeartbeat(): void {
    try {
      const user = authService.getCurrentUser();
      if (!user || !webSocketService.isConnectedStatus()) {
        return;
      }

      const heartbeat: PresenceHeartbeat = {
        userId: user.id,
        hiveIds: Array.from(this.activeHives),
        status: this.currentStatus,
        activity: this.currentActivity,
        timestamp: new Date().toISOString(),
      };

      webSocketService.sendMessage('/app/presence/heartbeat', heartbeat);
    } catch (error) {
      // Silently handle heartbeat errors
      console.debug('Heartbeat failed:', error);
    }
  }

  /**
   * Set active hives for heartbeat
   */
  setActiveHives(hiveIds: number[]): void {
    this.activeHives = new Set(hiveIds);
  }

  /**
   * Add a hive to active list
   */
  addActiveHive(hiveId: number): void {
    this.activeHives.add(hiveId);
  }

  /**
   * Remove a hive from active list
   */
  removeActiveHive(hiveId: number): void {
    this.activeHives.delete(hiveId);
  }

  // Real-time Subscriptions

  /**
   * Subscribe to presence updates for a hive
   */
  subscribeToHivePresence(hiveId: number, callback: (update: PresenceUpdate) => void): () => void {
    if (!webSocketService.isConnectedStatus()) {
      return () => {};
    }

    const topic = `/topic/presence/${hiveId}`;

    const subscriptionId = webSocketService.subscribe(topic, (message: IMessage) => {
      try {
        const update = JSON.parse(message.body) as PresenceUpdate;

        // Update cache if it's for a cached user
        const cached = this.getCachedUserPresence(update.userId);
        if (cached) {
          cached.status = update.status;
          if (update.activity) {
            cached.activity = update.activity;
          }
          cached.lastSeen = update.timestamp;
          this.cacheUserPresence(cached);
        }

        callback(update);
      } catch (error) {
        console.error('Failed to parse presence update:', error);
      }
    });

    const subscriptionKey = `hive-presence-${hiveId}`;
    const unsubscribe = () => {
      webSocketService.unsubscribe(subscriptionId);
      this.subscriptions.delete(subscriptionKey);
    };

    this.subscriptions.set(subscriptionKey, unsubscribe);
    return unsubscribe;
  }

  /**
   * Subscribe to a specific user's presence
   */
  subscribeToUserPresence(userId: number, callback: (update: PresenceUpdate) => void): () => void {
    if (!webSocketService.isConnectedStatus()) {
      return () => {};
    }

    const topic = `/topic/presence/user/${userId}`;

    const subscriptionId = webSocketService.subscribe(topic, (message: IMessage) => {
      try {
        const update = JSON.parse(message.body) as PresenceUpdate;

        // Update cache
        const cached = this.getCachedUserPresence(userId);
        if (cached) {
          cached.status = update.status;
          if (update.activity) {
            cached.activity = update.activity;
          }
          cached.lastSeen = update.timestamp;
          this.cacheUserPresence(cached);
        }

        callback(update);
      } catch (error) {
        console.error('Failed to parse user presence update:', error);
      }
    });

    const subscriptionKey = `user-presence-${userId}`;
    const unsubscribe = () => {
      webSocketService.unsubscribe(subscriptionId);
      this.subscriptions.delete(subscriptionKey);
    };

    this.subscriptions.set(subscriptionKey, unsubscribe);
    return unsubscribe;
  }

  // Auto-away Detection

  /**
   * Enable automatic away detection
   */
  enableAutoAway(threshold?: number): void {
    this.autoAwayEnabled = true;
    if (threshold) {
      this.autoAwayThreshold = threshold;
    }
    this.resetAutoAwayTimer();
  }

  /**
   * Disable automatic away detection
   */
  disableAutoAway(): void {
    this.autoAwayEnabled = false;
    if (this.autoAwayTimer) {
      clearTimeout(this.autoAwayTimer);
      this.autoAwayTimer = null;
    }
  }

  /**
   * Update user activity (resets auto-away timer)
   */
  updateActivity(): void {
    this.lastActivityTime = Date.now();
    if (this.autoAwayEnabled) {
      this.resetAutoAwayTimer();

      // If currently away, set back to online
      if (this.currentStatus === 'AWAY') {
        this.setPresence({ status: 'ONLINE', activity: this.currentActivity });
      }
    }
  }

  /**
   * Reset auto-away timer
   */
  private resetAutoAwayTimer(): void {
    if (this.autoAwayTimer) {
      clearTimeout(this.autoAwayTimer);
    }

    if (this.autoAwayEnabled) {
      this.autoAwayTimer = setTimeout(() => {
        this.handleAutoAway();
      }, this.autoAwayThreshold);
    }
  }

  /**
   * Handle auto-away timeout
   */
  private handleAutoAway(): void {
    if (this.currentStatus === 'ONLINE') {
      this.setPresence({ status: 'AWAY' }).catch(error => {
        console.error('Failed to set auto-away:', error);
      });
    }
  }

  // Cache Management

  cacheUserPresence(presence: UserPresence): void {
    this.presenceCache.set(presence.userId, {
      presence,
      timestamp: Date.now(),
    });
  }

  getCachedUserPresence(userId: number): UserPresence | null {
    const cached = this.presenceCache.get(userId);
    if (!cached) return null;

    // Check if cache is expired
    if (Date.now() - cached.timestamp > this.cacheTimeout) {
      this.presenceCache.delete(userId);
      return null;
    }

    return cached.presence;
  }

  private cacheHivePresence(presence: HivePresence): void {
    this.hivePresenceCache.set(presence.hiveId, {
      presence,
      timestamp: Date.now(),
    });
  }

  private getCachedHivePresence(hiveId: number): HivePresence | null {
    const cached = this.hivePresenceCache.get(hiveId);
    if (!cached) return null;

    // Check if cache is expired
    if (Date.now() - cached.timestamp > this.cacheTimeout) {
      this.hivePresenceCache.delete(hiveId);
      return null;
    }

    return cached.presence;
  }

  clearCache(): void {
    this.presenceCache.clear();
    this.hivePresenceCache.clear();
  }

  // Private helper methods

  private notifyPresenceUpdate(presence: UserPresence): void {
    const destination = '/app/presence/update';
    const update: PresenceUpdate = {
      userId: presence.userId,
      status: presence.status,
      activity: presence.activity,
      timestamp: new Date().toISOString(),
    };

    webSocketService.sendMessage(destination, update);
  }

  /**
   * Cleanup method to be called when service is destroyed
   */
  cleanup(): void {
    // Stop heartbeat
    this.stopHeartbeat();

    // Clear auto-away timer
    this.disableAutoAway();

    // Clear all subscriptions
    this.subscriptions.forEach(unsub => unsub());
    this.subscriptions.clear();

    // Clear cache
    this.clearCache();
  }
}

// Export singleton instance
export const presenceService = new PresenceService();