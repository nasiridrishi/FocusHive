import type {
  BuddyProfile,
  BuddyMatch,
  BuddySession,
  BuddyMatchRequest,
  CreateSessionRequest,
  UpdateSessionRequest,
  SessionCheckInRequest,
  RateSessionRequest,
  BuddySearchFilters,
  BuddyStats,
  BuddyInvitation,
  BuddyPreferences,
  BuddyMessage,
  BuddySearchResponse,
  BuddyAvailability,
  SessionRating,
  BuddyCheckIn,
  BuddyWebSocketEvent,
  SessionType,
} from '@/contracts/buddy';

/**
 * Buddy System Service
 * Manages accountability partners, matching, and buddy sessions
 */
export class BuddyService {
  private readonly apiUrl = 'http://localhost:8087/api/v1/buddy';
  private cache = new Map<string, { data: any; timestamp: number }>();
  private readonly CACHE_TTL = 30000; // 30 seconds
  private webSocketService: any;
  private subscriptions: Map<string, () => void> = new Map();

  constructor(webSocketService?: any) {
    this.webSocketService = webSocketService;
  }

  // Profile Management
  async getMyProfile(): Promise<BuddyProfile> {
    const cacheKey = 'my-profile';
    const cached = this.getFromCache(cacheKey);
    if (cached) return cached;

    const response = await fetch(`${this.apiUrl}/profile/me`, {
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch profile: ${response.status} ${response.statusText}`);
    }

    const data = await response.json();
    this.setCache(cacheKey, data);
    return data;
  }

  async getBuddyProfile(userId: number): Promise<BuddyProfile> {
    const cacheKey = `profile-${userId}`;
    const cached = this.getFromCache(cacheKey);
    if (cached) return cached;

    const response = await fetch(`${this.apiUrl}/profiles/${userId}`, {
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch buddy profile: ${response.status} ${response.statusText}`);
    }

    const data = await response.json();
    this.setCache(cacheKey, data);
    return data;
  }

  async updatePreferences(preferences: BuddyPreferences): Promise<BuddyPreferences> {
    const response = await fetch(`${this.apiUrl}/profile/preferences`, {
      method: 'PUT',
      headers: this.getHeaders(),
      body: JSON.stringify(preferences),
    });

    if (!response.ok) {
      throw new Error(`Failed to update preferences: ${response.status} ${response.statusText}`);
    }

    // Clear profile cache as preferences have changed
    this.clearCache('my-profile');

    return response.json();
  }

  async updateAvailability(availability: BuddyAvailability): Promise<BuddyAvailability> {
    const response = await fetch(`${this.apiUrl}/profile/availability`, {
      method: 'PUT',
      headers: this.getHeaders(),
      body: JSON.stringify(availability),
    });

    if (!response.ok) {
      throw new Error(`Failed to update availability: ${response.status} ${response.statusText}`);
    }

    // Clear profile cache
    this.clearCache('my-profile');

    return response.json();
  }

  // Matching System
  async requestMatch(request: BuddyMatchRequest): Promise<BuddyMatch> {
    // Validate request
    if (request.userId <= 0 || request.duration <= 0) {
      throw new Error('Invalid match request parameters');
    }

    const response = await fetch(`${this.apiUrl}/matches/request`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      throw new Error(`Failed to request match: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async findBuddies(filters: BuddySearchFilters): Promise<BuddySearchResponse> {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        if (Array.isArray(value)) {
          params.append(key, value.join(','));
        } else {
          params.append(key, String(value));
        }
      }
    });

    const response = await fetch(`${this.apiUrl}/buddies/search?${params}`, {
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to find buddies: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async acceptMatch(matchId: string): Promise<BuddyMatch> {
    const response = await fetch(`${this.apiUrl}/matches/${matchId}/accept`, {
      method: 'POST',
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to accept match: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async declineMatch(matchId: string): Promise<BuddyMatch> {
    const response = await fetch(`${this.apiUrl}/matches/${matchId}/decline`, {
      method: 'POST',
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to decline match: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async getPendingMatches(): Promise<BuddyMatch[]> {
    const response = await fetch(`${this.apiUrl}/matches/pending`, {
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to get pending matches: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async cancelMatch(matchId: string): Promise<void> {
    const response = await fetch(`${this.apiUrl}/matches/${matchId}/cancel`, {
      method: 'POST',
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to cancel match: ${response.status} ${response.statusText}`);
    }
  }

  // Session Management
  async createSession(request: CreateSessionRequest): Promise<BuddySession> {
    const response = await fetch(`${this.apiUrl}/sessions`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      throw new Error(`Failed to create session: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async startSession(sessionId: string): Promise<BuddySession> {
    const response = await fetch(`${this.apiUrl}/sessions/${sessionId}/start`, {
      method: 'POST',
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to start session: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async pauseSession(sessionId: string): Promise<BuddySession> {
    const response = await fetch(`${this.apiUrl}/sessions/${sessionId}/pause`, {
      method: 'POST',
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to pause session: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async resumeSession(sessionId: string): Promise<BuddySession> {
    const response = await fetch(`${this.apiUrl}/sessions/${sessionId}/resume`, {
      method: 'POST',
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to resume session: ${response.status} ${response.statusText}`);
    }

    const data = await response.json();
    // Set pausedAt to null on resume
    data.pausedAt = null;
    return data;
  }

  async endSession(sessionId: string): Promise<BuddySession> {
    const response = await fetch(`${this.apiUrl}/sessions/${sessionId}/end`, {
      method: 'POST',
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to end session: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async checkIn(sessionId: string, checkIn: SessionCheckInRequest): Promise<BuddyCheckIn> {
    const response = await fetch(`${this.apiUrl}/sessions/${sessionId}/checkin`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify(checkIn),
    });

    if (!response.ok) {
      throw new Error(`Failed to check in: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async getActiveSessions(): Promise<BuddySession[]> {
    const response = await fetch(`${this.apiUrl}/sessions/active`, {
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to get active sessions: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async getSessionHistory(): Promise<BuddySession[]> {
    const response = await fetch(`${this.apiUrl}/sessions/history`, {
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to get session history: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async rateSession(sessionId: string, rating: RateSessionRequest): Promise<SessionRating> {
    const response = await fetch(`${this.apiUrl}/sessions/${sessionId}/rate`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify(rating),
    });

    if (!response.ok) {
      throw new Error(`Failed to rate session: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  // Statistics
  async getMyStats(): Promise<BuddyStats> {
    const response = await fetch(`${this.apiUrl}/stats/me`, {
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to get stats: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async getTopBuddies(): Promise<Array<{
    userId: number;
    username: string;
    sessionCount: number;
    averageRating: number;
  }>> {
    const response = await fetch(`${this.apiUrl}/stats/top-buddies`, {
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to get top buddies: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async getLeaderboard(period: 'daily' | 'weekly' | 'monthly'): Promise<BuddyStats[]> {
    const response = await fetch(`${this.apiUrl}/leaderboard/${period}`, {
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to get leaderboard: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  // Invitations
  async sendInvitation(invitation: {
    toUserId: number;
    sessionType: SessionType;
    message?: string;
    scheduledFor?: string;
  }): Promise<BuddyInvitation> {
    const response = await fetch(`${this.apiUrl}/invitations`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify(invitation),
    });

    if (!response.ok) {
      throw new Error(`Failed to send invitation: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async getReceivedInvitations(): Promise<BuddyInvitation[]> {
    const response = await fetch(`${this.apiUrl}/invitations/received`, {
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to get invitations: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async acceptInvitation(invitationId: string): Promise<BuddyInvitation> {
    const response = await fetch(`${this.apiUrl}/invitations/${invitationId}/accept`, {
      method: 'POST',
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to accept invitation: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async declineInvitation(invitationId: string): Promise<BuddyInvitation> {
    const response = await fetch(`${this.apiUrl}/invitations/${invitationId}/decline`, {
      method: 'POST',
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to decline invitation: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  // Messages
  async sendMessage(sessionId: string, message: {
    content: string;
    type: 'text' | 'emoji' | 'encouragement' | 'task_update';
  }): Promise<BuddyMessage> {
    const response = await fetch(`${this.apiUrl}/sessions/${sessionId}/messages`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify(message),
    });

    if (!response.ok) {
      throw new Error(`Failed to send message: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async getSessionMessages(sessionId: string): Promise<BuddyMessage[]> {
    const response = await fetch(`${this.apiUrl}/sessions/${sessionId}/messages`, {
      headers: this.getHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to get messages: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  async addMessageReaction(messageId: string, emoji: string): Promise<BuddyMessage> {
    const response = await fetch(`${this.apiUrl}/messages/${messageId}/reactions`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify({ emoji }),
    });

    if (!response.ok) {
      throw new Error(`Failed to add reaction: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  // WebSocket subscriptions
  subscribeToMatchUpdates(callback: (event: BuddyWebSocketEvent) => void): () => void {
    if (!this.webSocketService) {
      console.warn('WebSocket not configured, match updates unavailable');
      return () => {};
    }

    const channel = '/topic/buddy/matches';
    const unsubscribe = this.webSocketService.subscribe?.(channel, (event: any) => {
      callback(event);
    }) || (() => {});

    this.subscriptions.set(`match-updates`, unsubscribe);
    return unsubscribe;
  }

  subscribeToSessionUpdates(sessionId: string, callback: (event: BuddyWebSocketEvent) => void): () => void {
    if (!this.webSocketService) {
      console.warn('WebSocket not configured, session updates unavailable');
      return () => {};
    }

    const channel = `/topic/buddy/sessions/${sessionId}`;
    const unsubscribe = this.webSocketService.subscribe?.(channel, (event: any) => {
      callback(event);
    }) || (() => {});

    this.subscriptions.set(`session-${sessionId}`, unsubscribe);
    return unsubscribe;
  }

  subscribeToNotifications(callback: (notification: any) => void): () => void {
    if (!this.webSocketService) {
      console.warn('WebSocket not configured, notifications unavailable');
      return () => {};
    }

    const channel = '/user/queue/buddy/notifications';
    const unsubscribe = this.webSocketService.subscribe?.(channel, (notification: any) => {
      callback(notification);
    }) || (() => {});

    this.subscriptions.set('notifications', unsubscribe);
    return unsubscribe;
  }

  // Utility methods
  private getHeaders(): HeadersInit {
    return {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${this.getAuthToken()}`,
    };
  }

  private getAuthToken(): string {
    // In production, get from auth service
    return localStorage.getItem('authToken') || 'test-token';
  }

  private getFromCache(key: string): any {
    const cached = this.cache.get(key);
    if (cached && Date.now() - cached.timestamp < this.CACHE_TTL) {
      return cached.data;
    }
    return null;
  }

  private setCache(key: string, data: any): void {
    this.cache.set(key, { data, timestamp: Date.now() });
  }

  private clearCache(pattern?: string): void {
    if (pattern) {
      Array.from(this.cache.keys())
        .filter(key => key.includes(pattern))
        .forEach(key => this.cache.delete(key));
    } else {
      this.cache.clear();
    }
  }

  cleanup(): void {
    this.cache.clear();
    this.subscriptions.forEach(unsubscribe => unsubscribe());
    this.subscriptions.clear();
  }
}

// Export singleton instance
export const buddyService = new BuddyService();