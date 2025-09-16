import type {
  FocusSession,
  DailyAnalytics,
  WeeklyAnalytics,
  MonthlyAnalytics,
  ProductivityGoal,
  Leaderboard,
  AnalyticsInsight,
  ChartConfig,
  HeatmapData,
  ProductivityPattern,
  Streak,
  BurnoutRisk,
  GetAnalyticsRequest,
  GetAnalyticsResponse,
  GetLeaderboardRequest,
  ExportAnalyticsRequest,
  ExportAnalyticsResponse,
  AnalyticsWebSocketEvent,
  AnalyticsPreferences,
  MetricType,
  AggregationPeriod,
  ProductivityMetric,
  ComparativeAnalytics,
} from '@/contracts/analytics';

export class AnalyticsService {
  private apiUrl = 'http://localhost:8080/api/v1/analytics';
  private cacheTimeout = 5 * 60 * 1000; // 5 minutes
  private cache: Map<string, { data: any; timestamp: number }> = new Map();
  private sessionTimers: Map<number, NodeJS.Timeout> = new Map();
  private currentSession: FocusSession | null = null;

  // Helper method to get auth headers
  private getAuthHeaders(): HeadersInit {
    const token = localStorage.getItem('authToken');
    return {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : '',
    };
  }

  // Helper method to check cache
  private getCachedData<T>(key: string): T | null {
    const cached = this.cache.get(key);
    if (cached && Date.now() - cached.timestamp < this.cacheTimeout) {
      return cached.data as T;
    }
    return null;
  }

  // Helper method to set cache
  private setCachedData(key: string, data: any): void {
    this.cache.set(key, { data, timestamp: Date.now() });
  }

  /**
   * Focus Session Management
   */
  async startFocusSession(params: {
    type: 'POMODORO' | 'DEEP_WORK' | 'REGULAR' | 'BREAK';
    taskName?: string;
    hiveId?: number;
    plannedDuration?: number;
  }): Promise<FocusSession> {
    const response = await fetch(`${this.apiUrl}/sessions`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: JSON.stringify(params),
    });

    if (!response.ok) {
      throw new Error(`Failed to start focus session: ${response.statusText}`);
    }

    const session = await response.json();
    this.currentSession = session;

    // Set a timer for auto-complete if duration is specified
    if (params.plannedDuration) {
      const timer = setTimeout(() => {
        this.endFocusSession(session.id, true).catch(console.error);
      }, params.plannedDuration * 60 * 1000);
      this.sessionTimers.set(session.id, timer);
    }

    return session;
  }

  async endFocusSession(sessionId: number, completed: boolean): Promise<FocusSession> {
    // Clear any auto-complete timer
    const timer = this.sessionTimers.get(sessionId);
    if (timer) {
      clearTimeout(timer);
      this.sessionTimers.delete(sessionId);
    }

    const response = await fetch(`${this.apiUrl}/sessions/${sessionId}/end`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: JSON.stringify({ completed }),
    });

    if (!response.ok) {
      throw new Error(`Failed to end focus session: ${response.statusText}`);
    }

    const session = await response.json();
    
    if (this.currentSession?.id === sessionId) {
      this.currentSession = null;
    }

    // Invalidate related caches
    this.invalidateCaches(['daily', 'weekly', 'monthly']);

    return session;
  }

  async pauseFocusSession(sessionId: number): Promise<FocusSession> {
    const response = await fetch(`${this.apiUrl}/sessions/${sessionId}/pause`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to pause session: ${response.statusText}`);
    }

    return response.json();
  }

  async resumeFocusSession(sessionId: number): Promise<FocusSession> {
    const response = await fetch(`${this.apiUrl}/sessions/${sessionId}/resume`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to resume session: ${response.statusText}`);
    }

    return response.json();
  }

  async trackInterruption(sessionId: number, reason?: string): Promise<void> {
    const response = await fetch(`${this.apiUrl}/sessions/${sessionId}/interrupt`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: JSON.stringify({ reason }),
    });

    if (!response.ok) {
      throw new Error(`Failed to track interruption: ${response.statusText}`);
    }
  }

  /**
   * Analytics Aggregation
   */
  async getDailyAnalytics(userId: number, date?: string): Promise<DailyAnalytics> {
    const targetDate = date || new Date().toISOString().split('T')[0];
    const cacheKey = `daily-${userId}-${targetDate}`;
    
    const cached = this.getCachedData<DailyAnalytics>(cacheKey);
    if (cached) return cached;

    const response = await fetch(
      `${this.apiUrl}/daily?userId=${userId}&date=${targetDate}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch daily analytics: ${response.statusText || response.status}`);
    }

    const data = await response.json();
    const analytics = data.analytics || data;
    
    this.setCachedData(cacheKey, analytics);
    return analytics;
  }

  async getWeeklyAnalytics(
    userId: number,
    weekStart?: string
  ): Promise<WeeklyAnalytics> {
    const startDate = weekStart || this.getWeekStart(new Date());
    const cacheKey = `weekly-${userId}-${startDate}`;
    
    const cached = this.getCachedData<WeeklyAnalytics>(cacheKey);
    if (cached) return cached;

    const response = await fetch(
      `${this.apiUrl}/weekly?userId=${userId}&weekStart=${startDate}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch weekly analytics: ${response.statusText}`);
    }

    const data = await response.json();
    const analytics = data.analytics || data;
    
    this.setCachedData(cacheKey, analytics);
    return analytics;
  }

  async getMonthlyAnalytics(
    userId: number,
    month?: number,
    year?: number
  ): Promise<MonthlyAnalytics> {
    const now = new Date();
    const targetMonth = month ?? now.getMonth() + 1;
    const targetYear = year ?? now.getFullYear();
    const cacheKey = `monthly-${userId}-${targetYear}-${targetMonth}`;
    
    const cached = this.getCachedData<MonthlyAnalytics>(cacheKey);
    if (cached) return cached;

    const response = await fetch(
      `${this.apiUrl}/monthly?userId=${userId}&month=${targetMonth}&year=${targetYear}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch monthly analytics: ${response.statusText}`);
    }

    const data = await response.json();
    const analytics = data.analytics || data;
    
    this.setCachedData(cacheKey, analytics);
    return analytics;
  }

  async getCustomAnalytics(request: GetAnalyticsRequest): Promise<GetAnalyticsResponse> {
    const response = await fetch(`${this.apiUrl}/custom`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch custom analytics: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Productivity Goals
   */
  async createGoal(params: {
    type: 'DAILY' | 'WEEKLY' | 'MONTHLY';
    metric: MetricType;
    target: number;
  }): Promise<ProductivityGoal> {
    const response = await fetch(`${this.apiUrl}/goals`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: JSON.stringify(params),
    });

    if (!response.ok) {
      throw new Error(`Failed to create goal: ${response.statusText}`);
    }

    return response.json();
  }

  async updateGoalProgress(
    goalId: number,
    progress: number
  ): Promise<ProductivityGoal> {
    const response = await fetch(`${this.apiUrl}/goals/${goalId}/progress`, {
      method: 'PUT',
      headers: this.getAuthHeaders(),
      body: JSON.stringify({ progress }),
    });

    if (!response.ok) {
      throw new Error(`Failed to update goal progress: ${response.statusText}`);
    }

    return response.json();
  }

  async getActiveGoals(userId: number): Promise<ProductivityGoal[]> {
    const cacheKey = `goals-${userId}`;
    const cached = this.getCachedData<ProductivityGoal[]>(cacheKey);
    if (cached) return cached;

    const response = await fetch(
      `${this.apiUrl}/goals?userId=${userId}&active=true`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch goals: ${response.statusText}`);
    }

    const goals = await response.json();
    this.setCachedData(cacheKey, goals);
    return goals;
  }

  async deleteGoal(goalId: number): Promise<void> {
    const response = await fetch(`${this.apiUrl}/goals/${goalId}`, {
      method: 'DELETE',
      headers: this.getAuthHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to delete goal: ${response.statusText}`);
    }

    this.invalidateCaches(['goals']);
  }

  /**
   * Leaderboards
   */
  async getLeaderboard(request: GetLeaderboardRequest): Promise<Leaderboard> {
    const cacheKey = `leaderboard-${request.type}-${request.metric}-${request.period}`;
    const cached = this.getCachedData<Leaderboard>(cacheKey);
    if (cached) return cached;

    const response = await fetch(`${this.apiUrl}/leaderboard`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      throw new Error(`Failed to fetch leaderboard: ${response.statusText}`);
    }

    const leaderboard = await response.json();
    this.setCachedData(cacheKey, leaderboard);
    return leaderboard;
  }

  async getUserRank(
    userId: number,
    metric: MetricType,
    period: AggregationPeriod
  ): Promise<number> {
    const response = await fetch(
      `${this.apiUrl}/rank?userId=${userId}&metric=${metric}&period=${period}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch user rank: ${response.statusText}`);
    }

    const data = await response.json();
    return data.rank;
  }

  /**
   * Insights and Patterns
   */
  async getInsights(userId: number): Promise<AnalyticsInsight[]> {
    const cacheKey = `insights-${userId}`;
    const cached = this.getCachedData<AnalyticsInsight[]>(cacheKey);
    if (cached) return cached;

    const response = await fetch(
      `${this.apiUrl}/insights?userId=${userId}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch insights: ${response.statusText}`);
    }

    const insights = await response.json();
    this.setCachedData(cacheKey, insights);
    return insights;
  }

  async dismissInsight(insightId: string): Promise<void> {
    const response = await fetch(`${this.apiUrl}/insights/${insightId}/dismiss`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Failed to dismiss insight: ${response.statusText}`);
    }

    this.invalidateCaches(['insights']);
  }

  async detectPatterns(userId: number): Promise<ProductivityPattern[]> {
    const response = await fetch(
      `${this.apiUrl}/patterns?userId=${userId}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to detect patterns: ${response.statusText}`);
    }

    return response.json();
  }

  async assessBurnoutRisk(userId: number): Promise<BurnoutRisk> {
    const response = await fetch(
      `${this.apiUrl}/burnout?userId=${userId}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to assess burnout risk: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Streak Tracking
   */
  async getStreaks(userId: number): Promise<Streak[]> {
    const cacheKey = `streaks-${userId}`;
    const cached = this.getCachedData<Streak[]>(cacheKey);
    if (cached) return cached;

    const response = await fetch(
      `${this.apiUrl}/streaks?userId=${userId}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch streaks: ${response.statusText}`);
    }

    const streaks = await response.json();
    this.setCachedData(cacheKey, streaks);
    return streaks;
  }

  async updateStreak(
    userId: number,
    type: 'DAILY_LOGIN' | 'FOCUS_GOAL' | 'TASK_COMPLETION' | 'HIVE_PARTICIPATION'
  ): Promise<void> {
    const response = await fetch(`${this.apiUrl}/streaks`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: JSON.stringify({ userId, type }),
    });

    if (!response.ok) {
      throw new Error(`Failed to update streak: ${response.statusText}`);
    }

    this.invalidateCaches(['streaks']);
  }

  /**
   * Data Visualization
   */
  async getChartData(
    chartType: string,
    period: AggregationPeriod,
    userId?: number
  ): Promise<ChartConfig> {
    const params = new URLSearchParams({
      type: chartType,
      period,
      ...(userId && { userId: userId.toString() }),
    });

    const response = await fetch(
      `${this.apiUrl}/charts?${params}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch chart data: ${response.statusText}`);
    }

    return response.json();
  }

  async getActivityHeatmap(
    userId: number,
    year: number
  ): Promise<HeatmapData> {
    const cacheKey = `heatmap-${userId}-${year}`;
    const cached = this.getCachedData<HeatmapData>(cacheKey);
    if (cached) return cached;

    const response = await fetch(
      `${this.apiUrl}/heatmap?userId=${userId}&year=${year}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch heatmap data: ${response.statusText}`);
    }

    const heatmap = await response.json();
    this.setCachedData(cacheKey, heatmap);
    return heatmap;
  }

  /**
   * Export and Sharing
   */
  async exportAnalytics(
    request: ExportAnalyticsRequest
  ): Promise<ExportAnalyticsResponse> {
    const response = await fetch(`${this.apiUrl}/export`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      throw new Error(`Failed to export analytics: ${response.statusText}`);
    }

    return response.json();
  }

  async shareAnalytics(
    userId: number,
    period: AggregationPeriod,
    shareWith: 'HIVE' | 'PUBLIC'
  ): Promise<{ shareUrl: string }> {
    const response = await fetch(`${this.apiUrl}/share`, {
      method: 'POST',
      headers: this.getAuthHeaders(),
      body: JSON.stringify({ userId, period, shareWith }),
    });

    if (!response.ok) {
      throw new Error(`Failed to share analytics: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Preferences
   */
  async getPreferences(userId: number): Promise<AnalyticsPreferences> {
    const response = await fetch(
      `${this.apiUrl}/preferences?userId=${userId}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to fetch preferences: ${response.statusText}`);
    }

    return response.json();
  }

  async updatePreferences(
    userId: number,
    preferences: Partial<AnalyticsPreferences>
  ): Promise<AnalyticsPreferences> {
    const response = await fetch(`${this.apiUrl}/preferences`, {
      method: 'PUT',
      headers: this.getAuthHeaders(),
      body: JSON.stringify({ userId, ...preferences }),
    });

    if (!response.ok) {
      throw new Error(`Failed to update preferences: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Real-time Updates (via WebSocket)
   */
  subscribeToAnalyticsUpdates(
    userId: number,
    callback: (event: AnalyticsWebSocketEvent) => void
  ): () => void {
    // This would integrate with the WebSocket service
    // For now, return a no-op unsubscribe function
    const wsService = (window as any).webSocketService;
    if (wsService?.subscribe) {
      return wsService.subscribe(
        `/user/${userId}/analytics`,
        callback
      );
    }
    return () => {};
  }

  /**
   * Utility Methods
   */
  private getWeekStart(date: Date): string {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    d.setDate(diff);
    return d.toISOString().split('T')[0];
  }

  clearCache(): void {
    this.cache.clear();
  }

  private invalidateCaches(types: string[]): void {
    for (const [key] of this.cache.entries()) {
      if (types.some(type => key.startsWith(type))) {
        this.cache.delete(key);
      }
    }
  }

  /**
   * Quick Stats Methods
   */
  async getTodayStats(userId: number): Promise<{
    focusMinutes: number;
    sessionsCompleted: number;
    currentStreak: number;
    productivityScore: number;
  }> {
    const daily = await this.getDailyAnalytics(userId);
    const streaks = await this.getStreaks(userId);
    
    return {
      focusMinutes: daily.totalFocusMinutes,
      sessionsCompleted: daily.sessionsCompleted,
      currentStreak: streaks.find(s => s.type === 'DAILY_LOGIN')?.currentStreak || 0,
      productivityScore: daily.productivityScore,
    };
  }

  async getWeeklyComparison(userId: number): Promise<{
    thisWeek: number;
    lastWeek: number;
    percentageChange: number;
    trend: 'UP' | 'DOWN' | 'STABLE';
  }> {
    const thisWeek = await this.getWeeklyAnalytics(userId);
    const lastWeekStart = new Date(thisWeek.weekStartDate);
    lastWeekStart.setDate(lastWeekStart.getDate() - 7);
    const lastWeek = await this.getWeeklyAnalytics(
      userId,
      lastWeekStart.toISOString().split('T')[0]
    );

    const change = thisWeek.totalFocusMinutes - lastWeek.totalFocusMinutes;
    const percentageChange = lastWeek.totalFocusMinutes 
      ? (change / lastWeek.totalFocusMinutes) * 100 
      : 0;

    return {
      thisWeek: thisWeek.totalFocusMinutes,
      lastWeek: lastWeek.totalFocusMinutes,
      percentageChange,
      trend: percentageChange > 5 ? 'UP' : percentageChange < -5 ? 'DOWN' : 'STABLE',
    };
  }

  // Cleanup method
  cleanup(): void {
    // Clear all timers
    for (const timer of this.sessionTimers.values()) {
      clearTimeout(timer);
    }
    this.sessionTimers.clear();
    
    // Clear cache
    this.cache.clear();
    
    // Reset current session
    this.currentSession = null;
  }
}

// Export singleton instance
export const analyticsService = new AnalyticsService();