import {AxiosError, AxiosInstance} from 'axios';
import {apiClient} from './httpInterceptors';
import {API_ENDPOINTS, buildEndpoint} from './index';

/**
 * Analytics API Service
 *
 * Provides comprehensive analytics and reporting with:
 * - Session tracking
 * - User statistics
 * - Hive leaderboards
 * - Performance metrics
 */

export interface SessionRequest {
  sessionType: 'FOCUS' | 'BREAK' | 'POMODORO' | 'CUSTOM';
  startTime: string;
  expectedDuration?: number;
  task?: string;
  hiveId?: number;
  metadata?: Record<string, unknown>;
}

export interface SessionResponse {
  sessionId: number;
  userId: number;
  sessionType: string;
  startTime: string;
  endTime?: string;
  duration?: number;
  task?: string;
  hiveId?: number;
  completionRate?: number;
  metadata?: Record<string, unknown>;
  createdAt: string;
}

export interface UserStats {
  userId: number;
  username: string;
  totalFocusTime: number; // minutes
  totalSessions: number;
  averageSessionLength: number;
  completionRate: number;
  streakDays: number;
  longestStreak: number;
  focusScore: number; // 0-100
  rank?: number;
  period: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'ALL_TIME';
  periodStart: string;
  periodEnd: string;
  sessionBreakdown: {
    pomodoro: number;
    focus: number;
    break: number;
    custom: number;
  };
  productivityTrend: {
    date: string;
    focusTime: number;
    sessions: number;
  }[];
  topTasks: {
    task: string;
    timeSpent: number;
    sessions: number;
  }[];
}

export interface LeaderboardEntry {
  rank: number;
  userId: number;
  username: string;
  displayName?: string;
  avatar?: string;
  focusTime: number;
  sessions: number;
  focusScore: number;
  streakDays: number;
  change: number; // position change from previous period
}

export interface HiveLeaderboard {
  hiveId: number;
  hiveName: string;
  period: 'DAILY' | 'WEEKLY' | 'MONTHLY';
  periodStart: string;
  periodEnd: string;
  totalMembers: number;
  activeMembers: number;
  entries: LeaderboardEntry[];
  hiveStats: {
    totalFocusTime: number;
    totalSessions: number;
    averageSessionLength: number;
    mostActiveHour: number;
    topPerformers: LeaderboardEntry[];
  };
}

export interface GlobalLeaderboard {
  period: 'DAILY' | 'WEEKLY' | 'MONTHLY';
  periodStart: string;
  periodEnd: string;
  totalUsers: number;
  entries: LeaderboardEntry[];
}

export interface AnalyticsExport {
  userId: number;
  format: 'JSON' | 'CSV' | 'PDF';
  dataTypes: ('SESSIONS' | 'STATS' | 'ACHIEVEMENTS' | 'HIVE_ACTIVITY')[];
  dateRange: {
    start: string;
    end: string;
  };
  exportUrl?: string;
  status: 'REQUESTED' | 'PROCESSING' | 'READY' | 'EXPIRED';
  createdAt: string;
  expiresAt?: string;
}

class AnalyticsApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = apiClient;
  }

  /**
   * Start tracking a new session
   */
  async startSession(request: SessionRequest): Promise<SessionResponse> {
    try {
const response = await this.api.post<SessionResponse>(API_ENDPOINTS.ANALYTICS.START_SESSION, request);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to start session tracking');
      throw error;
    }
  }

  /**
   * End session tracking
   */
  async endSession(sessionId: number, endTime?: string, actualDuration?: number): Promise<SessionResponse> {
    try {
      const requestBody = {
        endTime: endTime || new Date().toISOString(),
        actualDuration
      };
const response = await this.api.post<SessionResponse>(API_ENDPOINTS.ANALYTICS.END_SESSION, requestBody);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to end session tracking');
      throw error;
    }
  }

  /**
   * Start session tracking with detailed request
   */
  async startSessionDetailed(sessionId: number, request: SessionRequest): Promise<SessionResponse> {
    try {
      const endpoint = buildEndpoint(API_ENDPOINTS.ANALYTICS.START_SESSION, {sessionId});
      const response = await this.api.post<SessionResponse>(endpoint, request);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to start detailed session tracking');
      throw error;
    }
  }

  /**
   * End session tracking with detailed request
   */
  async endSessionDetailed(sessionId: number, endTime?: string): Promise<SessionResponse> {
    try {
      const endpoint = buildEndpoint(API_ENDPOINTS.ANALYTICS.END_SESSION, {sessionId});
      const requestBody = {endTime: endTime || new Date().toISOString()};
      const response = await this.api.post<SessionResponse>(endpoint, requestBody);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to end detailed session tracking');
      throw error;
    }
  }

  /**
   * Get user statistics
   */
  async getUserStats(
      userId?: number,
      period: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'ALL_TIME' = 'WEEKLY'
  ): Promise<UserStats> {
    try {
      if (userId) {
        const endpoint = buildEndpoint(API_ENDPOINTS.ANALYTICS.USER_STATS, {userId});
        const response = await this.api.get<UserStats>(endpoint, {params: {period}});
        return response.data;
      } else {
        // Get current user's stats
        const response = await this.api.get<UserStats>(API_ENDPOINTS.ANALYTICS.USER_STATS_ME, {
          params: {period}
        });
        return response.data;
      }
    } catch (error) {
      this.handleError(error, 'Failed to get user statistics');
      throw error;
    }
  }

  /**
   * Get current user's statistics
   */
  async getMyStats(period: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'ALL_TIME' = 'WEEKLY'): Promise<UserStats> {
    return this.getUserStats(undefined, period);
  }

  /**
   * Get hive leaderboard
   */
  async getHiveLeaderboard(
      hiveId: number,
      period: 'DAILY' | 'WEEKLY' | 'MONTHLY' = 'WEEKLY',
      limit = 50
  ): Promise<HiveLeaderboard> {
    try {
      const endpoint = buildEndpoint(API_ENDPOINTS.ANALYTICS.HIVE_LEADERBOARD, {hiveId});
      const response = await this.api.get<HiveLeaderboard>(endpoint, {
        params: {period, limit}
      });
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get hive leaderboard');
      throw error;
    }
  }

  /**
   * Get global leaderboard
   */
  async getGlobalLeaderboard(
      period: 'DAILY' | 'WEEKLY' | 'MONTHLY' = 'WEEKLY',
      limit = 100
  ): Promise<GlobalLeaderboard> {
    try {
      const response = await this.api.get<GlobalLeaderboard>(API_ENDPOINTS.ANALYTICS.LEADERBOARD, {
        params: {period, limit}
      });
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get global leaderboard');
      throw error;
    }
  }

  /**
   * Get productivity trends
   */
  async getProductivityTrends(
      userId?: number,
      dateRange?: { start: string; end: string },
      granularity: 'HOUR' | 'DAY' | 'WEEK' | 'MONTH' = 'DAY'
  ): Promise<{
    trends: { date: string; focusTime: number; sessions: number; }[];
    summary: { totalTime: number; averageDaily: number; bestDay: string; };
  }> {
    try {
      const params: Record<string, unknown> = {granularity};
      if (userId) params.userId = userId;
      if (dateRange) {
        params.startDate = dateRange.start;
        params.endDate = dateRange.end;
      }

      const endpoint = userId
          ? buildEndpoint(API_ENDPOINTS.ANALYTICS.USER_STATS, {userId})
          : API_ENDPOINTS.ANALYTICS.USER_STATS_ME;

      const response = await this.api.get(`${endpoint}/trends`, {params});
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get productivity trends');
      throw error;
    }
  }

  /**
   * Get session analytics
   */
  async getSessionAnalytics(
      dateRange?: { start: string; end: string },
      hiveId?: number
  ): Promise<{
    totalSessions: number;
    completedSessions: number;
    averageDuration: number;
    completionRate: number;
    sessionsByType: Record<string, number>;
    peakHours: { hour: number; sessions: number; }[];
  }> {
    try {
      const params: Record<string, unknown> = {};
      if (dateRange) {
        params.startDate = dateRange.start;
        params.endDate = dateRange.end;
      }
      if (hiveId) params.hiveId = hiveId;

      const response = await this.api.get(`${API_ENDPOINTS.ANALYTICS.BASE}/sessions/analytics`, {params});
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get session analytics');
      throw error;
    }
  }

  /**
   * Request data export
   */
  async requestDataExport(
      format: 'JSON' | 'CSV' | 'PDF' = 'JSON',
      dataTypes: ('SESSIONS' | 'STATS' | 'ACHIEVEMENTS' | 'HIVE_ACTIVITY')[] = ['SESSIONS', 'STATS'],
      dateRange?: { start: string; end: string }
  ): Promise<AnalyticsExport> {
    try {
      const request = {
        format,
        dataTypes,
        dateRange: dateRange || {
          start: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(), // 30 days ago
          end: new Date().toISOString()
        }
      };

      const response = await this.api.post<AnalyticsExport>(`${API_ENDPOINTS.ANALYTICS.BASE}/export`, request);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to request data export');
      throw error;
    }
  }

  /**
   * Get export status
   */
  async getExportStatus(exportId: number): Promise<AnalyticsExport> {
    try {
      const response = await this.api.get<AnalyticsExport>(`${API_ENDPOINTS.ANALYTICS.BASE}/export/${exportId}`);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get export status');
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
export const analyticsApiService = new AnalyticsApiService();

export default analyticsApiService;