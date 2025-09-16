/**
 * Timer Service
 * Comprehensive timer management with Pomodoro support, goals, and templates
 */

import type {
  TimerSession,
  TimerStatus,
  TimerType,
  TimerStats,
  TimerHistoryEntry,
  TimerTemplate,
  TimerGoal,
  CreateTimerRequest,
  UpdateTimerRequest,
  TimerStartResponse,
  TimerHistoryResponse,
  TimerTemplatesResponse,
  TimerWebSocketEvent,
  TimerCommand,
  TimerSyncData,
  SessionStatus,
  SessionType
} from '@/contracts/timer';

/**
 * Production-ready Timer Service with comprehensive timer management
 */
export class TimerService {
  private apiUrl = 'http://localhost:8080/api/v1/timer';
  private webSocketService: any;
  private cache = new Map<string, { data: any; timestamp: number }>();
  private readonly CACHE_TTL = 30000; // 30 seconds
  private subscriptions: Map<string, () => void> = new Map();

  constructor() {
    // WebSocket service will be injected via tests
  }

  /**
   * Start a new timer session
   */
  async startTimer(request: CreateTimerRequest): Promise<TimerStartResponse> {
    try {
      const response = await fetch(`${this.apiUrl}/sessions/start`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify(request)
      });

      if (!response.ok) {
        throw new Error(`Failed to start timer: ${response.status} ${response.statusText}`);
      }

      const result = await response.json();

      // Cache the current session
      this.setCacheEntry('current_session', result.session);

      // Send WebSocket notification if connected
      if (this.webSocketService?.isConnected?.()) {
        this.webSocketService.sendMessage('/app/timer/start', {
          sessionId: result.session.id
        });
      }

      return result;
    } catch (error) {
      console.error('Error starting timer:', error);
      throw error;
    }
  }

  /**
   * Pause an active timer
   */
  async pauseTimer(sessionId: number): Promise<TimerSession> {
    try {
      const response = await fetch(`${this.apiUrl}/sessions/${sessionId}/pause`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to pause timer: ${response.status} ${response.statusText}`);
      }

      const session = await response.json();
      this.updateCachedSession(session);

      if (this.webSocketService?.isConnected?.()) {
        this.webSocketService.sendMessage('/app/timer/pause', { sessionId });
      }

      return session;
    } catch (error) {
      console.error('Error pausing timer:', error);
      throw error;
    }
  }

  /**
   * Resume a paused timer
   */
  async resumeTimer(sessionId: number): Promise<TimerSession> {
    try {
      const response = await fetch(`${this.apiUrl}/sessions/${sessionId}/resume`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to resume timer: ${response.status} ${response.statusText}`);
      }

      const session = await response.json();
      this.updateCachedSession(session);

      if (this.webSocketService?.isConnected?.()) {
        this.webSocketService.sendMessage('/app/timer/resume', { sessionId });
      }

      return session;
    } catch (error) {
      console.error('Error resuming timer:', error);
      throw error;
    }
  }

  /**
   * Stop and complete a timer
   */
  async stopTimer(sessionId: number): Promise<TimerSession> {
    try {
      const response = await fetch(`${this.apiUrl}/sessions/${sessionId}/stop`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to stop timer: ${response.status} ${response.statusText}`);
      }

      const session = await response.json();

      // Clear current session cache if this was the active one
      const cached = this.getCacheEntry('current_session');
      if (cached?.id === sessionId) {
        this.cache.delete('current_session');
      }

      if (this.webSocketService?.isConnected?.()) {
        this.webSocketService.sendMessage('/app/timer/stop', { sessionId });
      }

      return session;
    } catch (error) {
      console.error('Error stopping timer:', error);
      throw error;
    }
  }

  /**
   * Cancel a timer session
   */
  async cancelTimer(sessionId: number): Promise<TimerSession> {
    try {
      const response = await fetch(`${this.apiUrl}/sessions/${sessionId}/cancel`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to cancel timer: ${response.status} ${response.statusText}`);
      }

      const session = await response.json();

      // Clear current session cache
      const cached = this.getCacheEntry('current_session');
      if (cached?.id === sessionId) {
        this.cache.delete('current_session');
      }

      if (this.webSocketService?.isConnected?.()) {
        this.webSocketService.sendMessage('/app/timer/cancel', { sessionId });
      }

      return session;
    } catch (error) {
      console.error('Error cancelling timer:', error);
      throw error;
    }
  }

  /**
   * Get current active timer
   */
  async getCurrentTimer(): Promise<TimerSession | null> {
    // Check cache first
    const cached = this.getCacheEntry('current_session');
    if (cached) {
      return cached;
    }

    try {
      const response = await fetch(`${this.apiUrl}/sessions/current`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (response.status === 404) {
        return null;
      }

      if (!response.ok) {
        throw new Error(`Failed to get current timer: ${response.status} ${response.statusText}`);
      }

      const session = await response.json();
      this.setCacheEntry('current_session', session);
      return session;
    } catch (error) {
      console.error('Error getting current timer:', error);
      throw error;
    }
  }

  /**
   * Get timer history with pagination
   */
  async getTimerHistory(params: { page: number; pageSize: number }): Promise<TimerHistoryResponse> {
    const queryParams = new URLSearchParams({
      page: params.page.toString(),
      size: params.pageSize.toString()
    });

    try {
      const response = await fetch(`${this.apiUrl}/sessions/history?${queryParams}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch timer history: ${response.status} ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error fetching timer history:', error);
      throw error;
    }
  }

  /**
   * Get timer statistics
   */
  async getTimerStatistics(): Promise<TimerStats> {
    // Check cache
    const cached = this.getCacheEntry('timer_stats');
    if (cached) {
      return cached;
    }

    try {
      const response = await fetch(`${this.apiUrl}/statistics`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch timer statistics: ${response.status} ${response.statusText}`);
      }

      const stats = await response.json();
      this.setCacheEntry('timer_stats', stats);
      return stats;
    } catch (error) {
      console.error('Error fetching timer statistics:', error);
      throw error;
    }
  }

  /**
   * Get timer templates
   */
  async getTimerTemplates(): Promise<TimerTemplatesResponse> {
    // Check cache
    const cached = this.getCacheEntry('timer_templates');
    if (cached) {
      return cached;
    }

    try {
      const response = await fetch(`${this.apiUrl}/templates`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch timer templates: ${response.status} ${response.statusText}`);
      }

      const templates = await response.json();
      this.setCacheEntry('timer_templates', templates);
      return templates;
    } catch (error) {
      console.error('Error fetching timer templates:', error);
      throw error;
    }
  }

  /**
   * Create a custom timer template
   */
  async createTimerTemplate(template: Partial<TimerTemplate>): Promise<TimerTemplate> {
    try {
      const response = await fetch(`${this.apiUrl}/templates`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify(template)
      });

      if (!response.ok) {
        throw new Error(`Failed to create timer template: ${response.status} ${response.statusText}`);
      }

      const newTemplate = await response.json();

      // Invalidate templates cache
      this.cache.delete('timer_templates');

      return newTemplate;
    } catch (error) {
      console.error('Error creating timer template:', error);
      throw error;
    }
  }

  /**
   * Delete a custom timer template
   */
  async deleteTimerTemplate(templateId: string): Promise<void> {
    try {
      const response = await fetch(`${this.apiUrl}/templates/${templateId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to delete timer template: ${response.status} ${response.statusText}`);
      }

      // Invalidate templates cache
      this.cache.delete('timer_templates');
    } catch (error) {
      console.error('Error deleting timer template:', error);
      throw error;
    }
  }

  /**
   * Get timer goals
   */
  async getTimerGoals(): Promise<TimerGoal[]> {
    // Check cache
    const cached = this.getCacheEntry('timer_goals');
    if (cached) {
      return cached;
    }

    try {
      const response = await fetch(`${this.apiUrl}/goals`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch timer goals: ${response.status} ${response.statusText}`);
      }

      const goals = await response.json();
      this.setCacheEntry('timer_goals', goals);
      return goals;
    } catch (error) {
      console.error('Error fetching timer goals:', error);
      throw error;
    }
  }

  /**
   * Create a timer goal
   */
  async createTimerGoal(goal: Partial<TimerGoal>): Promise<TimerGoal> {
    try {
      const response = await fetch(`${this.apiUrl}/goals`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify(goal)
      });

      if (!response.ok) {
        throw new Error(`Failed to create timer goal: ${response.status} ${response.statusText}`);
      }

      const newGoal = await response.json();

      // Invalidate goals cache
      this.cache.delete('timer_goals');

      return newGoal;
    } catch (error) {
      console.error('Error creating timer goal:', error);
      throw error;
    }
  }

  /**
   * Update goal progress
   */
  async updateGoalProgress(goalId: string, progressMinutes: number): Promise<TimerGoal> {
    try {
      const response = await fetch(`${this.apiUrl}/goals/${goalId}/progress`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify({ progressMinutes })
      });

      if (!response.ok) {
        throw new Error(`Failed to update goal progress: ${response.status} ${response.statusText}`);
      }

      const updatedGoal = await response.json();

      // Invalidate goals cache
      this.cache.delete('timer_goals');

      return updatedGoal;
    } catch (error) {
      console.error('Error updating goal progress:', error);
      throw error;
    }
  }

  /**
   * Subscribe to timer updates via WebSocket
   */
  subscribeToTimerUpdates(sessionId: number, callback: (event: TimerWebSocketEvent) => void): () => void {
    if (!this.webSocketService?.isConnected?.()) {
      console.warn('WebSocket not connected, timer updates unavailable');
      return () => {};
    }

    const channel = `/topic/timer/${sessionId}`;
    const unsubscribe = this.webSocketService.subscribe(channel, (message: any) => {
      try {
        const event = typeof message === 'string' ? JSON.parse(message) : message;
        callback(event);
      } catch (error) {
        console.error('Error parsing timer update:', error);
      }
    });

    // Store unsubscribe function
    this.subscriptions.set(channel, unsubscribe);

    return () => {
      if (this.subscriptions.has(channel)) {
        const unsub = this.subscriptions.get(channel);
        unsub?.();
        this.subscriptions.delete(channel);
      }
    };
  }

  /**
   * Send timer command via WebSocket
   */
  sendTimerCommand(command: TimerCommand): void {
    if (!this.webSocketService?.isConnected?.()) {
      throw new Error('WebSocket is not connected');
    }

    this.webSocketService.sendMessage('/app/timer/command', command);
  }

  /**
   * Sync timer state across devices
   */
  syncTimerState(syncData: TimerSyncData): void {
    if (!this.webSocketService?.isConnected?.()) {
      throw new Error('WebSocket is not connected');
    }

    this.webSocketService.sendMessage('/app/timer/sync', syncData);
  }

  /**
   * Start a Pomodoro session
   */
  async startPomodoro(): Promise<TimerSession> {
    const request: CreateTimerRequest = {
      hiveId: 0, // Default hive
      duration: 25 * 60 * 1000, // 25 minutes
      type: 'pomodoro',
      title: 'Pomodoro Session'
    };

    const response = await this.startTimer(request);
    return response.session;
  }

  /**
   * Start a break session
   */
  async startBreak(type: 'short' | 'long'): Promise<TimerSession> {
    const duration = type === 'short' ? 5 * 60 * 1000 : 15 * 60 * 1000;
    const request: CreateTimerRequest = {
      hiveId: 0,
      duration,
      type: type === 'short' ? 'short-break' : 'long-break',
      title: type === 'short' ? 'Short Break' : 'Long Break'
    };

    const response = await this.startTimer(request);
    return response.session;
  }

  /**
   * Get Pomodoro count
   */
  getPomodoroCount(): number {
    const count = localStorage.getItem('pomodoro_count');
    return count ? parseInt(count, 10) : 0;
  }

  /**
   * Persist timer state to localStorage
   */
  persistTimerState(session: TimerSession): void {
    localStorage.setItem('timer_current_session', JSON.stringify(session));
  }

  /**
   * Restore timer state from localStorage
   */
  restoreTimerState(): TimerSession | null {
    const stored = localStorage.getItem('timer_current_session');
    if (!stored) return null;

    try {
      return JSON.parse(stored);
    } catch {
      return null;
    }
  }

  /**
   * Clear all cache
   */
  clearCache(): void {
    this.cache.clear();
  }

  /**
   * Get single timer template
   */
  async getTimerTemplate(templateId: number | string): Promise<TimerTemplate> {
    const cached = this.getCacheEntry(`template_${templateId}`);
    if (cached) return cached;

    try {
      const response = await fetch(`${this.apiUrl}/templates/${templateId}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch template: ${response.status} ${response.statusText}`);
      }

      const template = await response.json();
      this.setCacheEntry(`template_${templateId}`, template);
      return template;
    } catch (error) {
      console.error('Error fetching template:', error);
      throw error;
    }
  }

  /**
   * Update a timer template
   */
  async updateTimerTemplate(
    templateId: number | string,
    updates: any // Using any to avoid import issues
  ): Promise<TimerTemplate> {
    try {
      const response = await fetch(`${this.apiUrl}/templates/${templateId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify(updates)
      });

      if (!response.ok) {
        throw new Error(`Failed to update template: ${response.status} ${response.statusText}`);
      }

      const template = await response.json();
      // Invalidate caches
      this.cache.delete(`template_${templateId}`);
      this.cache.delete('timer_templates');
      return template;
    } catch (error) {
      console.error('Error updating template:', error);
      throw error;
    }
  }

  /**
   * Start timer from template
   */
  async startTimerFromTemplate(
    templateId: number | string,
    hiveId?: number | null
  ): Promise<TimerSession> {
    try {
      const response = await fetch(`${this.apiUrl}/templates/${templateId}/start`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify({ hiveId })
      });

      if (!response.ok) {
        throw new Error(`Failed to start timer from template: ${response.status} ${response.statusText}`);
      }

      const result = await response.json();
      const session = result.session || result;

      // Update cache
      this.setCacheEntry('current_session', session);
      this.cache.delete('timer_history');

      // Notify via WebSocket (if available)
      if (this.webSocketService) {
        this.sendTimerCommand({ action: 'start', sessionId: session.id });
      }

      return session;
    } catch (error) {
      console.error('Error starting timer from template:', error);
      throw error;
    }
  }

  /**
   * Get single timer goal
   */
  async getTimerGoal(goalId: number | string): Promise<TimerGoal> {
    const cached = this.getCacheEntry(`goal_${goalId}`);
    if (cached) return cached;

    try {
      const response = await fetch(`${this.apiUrl}/goals/${goalId}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch goal: ${response.status} ${response.statusText}`);
      }

      const goal = await response.json();
      this.setCacheEntry(`goal_${goalId}`, goal);
      return goal;
    } catch (error) {
      console.error('Error fetching goal:', error);
      throw error;
    }
  }

  /**
   * Update a timer goal
   */
  async updateTimerGoal(
    goalId: number | string,
    updates: any // Using any to avoid import issues
  ): Promise<TimerGoal> {
    try {
      const response = await fetch(`${this.apiUrl}/goals/${goalId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify(updates)
      });

      if (!response.ok) {
        throw new Error(`Failed to update goal: ${response.status} ${response.statusText}`);
      }

      const goal = await response.json();
      // Invalidate caches
      this.cache.delete(`goal_${goalId}`);
      this.cache.delete('timer_goals');
      return goal;
    } catch (error) {
      console.error('Error updating goal:', error);
      throw error;
    }
  }

  /**
   * Delete a timer goal
   */
  async deleteTimerGoal(goalId: number | string): Promise<void> {
    try {
      const response = await fetch(`${this.apiUrl}/goals/${goalId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to delete goal: ${response.status} ${response.statusText}`);
      }

      // Invalidate caches
      this.cache.delete(`goal_${goalId}`);
      this.cache.delete('timer_goals');
    } catch (error) {
      console.error('Error deleting goal:', error);
      throw error;
    }
  }

  /**
   * Get goal progress
   */
  async getGoalProgress(goalId: number | string): Promise<any> {
    try {
      const response = await fetch(`${this.apiUrl}/goals/${goalId}/progress`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch goal progress: ${response.status} ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error fetching goal progress:', error);
      throw error;
    }
  }

  /**
   * Clean up resources
   */
  cleanup(): void {
    // Unsubscribe from all WebSocket channels
    this.subscriptions.forEach(unsubscribe => unsubscribe());
    this.subscriptions.clear();

    // Clear cache
    this.clearCache();

    // Throw error on subsequent calls
    this.getCurrentTimer = () => Promise.reject(new Error('Service cleaned up'));
  }

  /**
   * Get auth token (mock for testing)
   */
  private getAuthToken(): string {
    // In production, this would get the real auth token
    // For testing, we return a mock token
    return 'mock-token';
  }

  /**
   * Cache management helpers
   */
  private setCacheEntry(key: string, data: any): void {
    this.cache.set(key, {
      data,
      timestamp: Date.now()
    });
  }

  private getCacheEntry(key: string): any | null {
    const entry = this.cache.get(key);
    if (!entry) return null;

    const age = Date.now() - entry.timestamp;
    if (age > this.CACHE_TTL) {
      this.cache.delete(key);
      return null;
    }

    return entry.data;
  }

  private updateCachedSession(session: TimerSession): void {
    const cached = this.getCacheEntry('current_session');
    if (cached?.id === session.id) {
      this.setCacheEntry('current_session', session);
    }
  }
}

// Export singleton instance
export const timerService = new TimerService();