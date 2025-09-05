import { AxiosInstance, AxiosError } from 'axios';
import { apiClient } from './httpInterceptors';
import { API_ENDPOINTS, buildEndpoint } from './index';

/**
 * Timer API Service
 * 
 * Provides timer and focus session management with:
 * - Session lifecycle management
 * - Pomodoro technique support
 * - Productivity statistics
 * - Focus session history
 */

export interface StartSessionRequest {
  sessionType: 'POMODORO' | 'FOCUS' | 'BREAK' | 'CUSTOM';
  targetDuration: number; // in minutes
  task?: string;
  hiveId?: number;
  pomodoroSettings?: PomodoroSettings;
}

export interface EndSessionRequest {
  sessionId: number;
  actualDuration?: number;
  completionReason: 'COMPLETED' | 'INTERRUPTED' | 'CANCELLED';
  notes?: string;
}

export interface FocusSession {
  id: number;
  userId: number;
  sessionType: 'POMODORO' | 'FOCUS' | 'BREAK' | 'CUSTOM';
  targetDuration: number;
  actualDuration?: number;
  startTime: string;
  endTime?: string;
  task?: string;
  hiveId?: number;
  isActive: boolean;
  completionReason?: string;
  notes?: string;
  pomodoroRound?: number;
  createdAt: string;
  updatedAt: string;
}

export interface PomodoroSettings {
  id?: number;
  userId: number;
  focusDuration: number; // minutes
  shortBreakDuration: number; // minutes
  longBreakDuration: number; // minutes
  roundsUntilLongBreak: number;
  autoStartBreaks: boolean;
  autoStartSessions: boolean;
  soundEnabled: boolean;
  notificationsEnabled: boolean;
  theme?: string;
}

export interface ProductivityStats {
  userId: number;
  period: 'DAILY' | 'WEEKLY' | 'MONTHLY';
  date: string;
  totalFocusTime: number; // minutes
  totalSessions: number;
  completedSessions: number;
  averageSessionLength: number;
  longestStreak: number;
  currentStreak: number;
  focusScore: number; // 0-100
  topCategories: { category: string; time: number }[];
  topTasks: { task: string; time: number }[];
}

export interface StreakStats {
  userId: number;
  currentStreak: number;
  longestStreak: number;
  streakStartDate?: string;
  lastSessionDate?: string;
  streakType: 'DAILY' | 'WEEKLY';
}

class TimerApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = apiClient;
  }

  /**
   * Start a new focus session
   */
  async startSession(request: StartSessionRequest): Promise<FocusSession> {
    try {
      const response = await this.api.post<FocusSession>(API_ENDPOINTS.TIMER.START_SESSION, request);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to start focus session');
      throw error;
    }
  }

  /**
   * End a focus session
   */
  async endSession(sessionId: number, request?: Partial<EndSessionRequest>): Promise<FocusSession> {
    try {
      const endpoint = buildEndpoint(API_ENDPOINTS.TIMER.END_SESSION, { sessionId });
      const response = await this.api.post<FocusSession>(endpoint, request);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to end focus session');
      throw error;
    }
  }

  /**
   * Pause a focus session
   */
  async pauseSession(sessionId: number): Promise<FocusSession> {
    try {
      const endpoint = buildEndpoint(API_ENDPOINTS.TIMER.PAUSE_SESSION, { sessionId });
      const response = await this.api.post<FocusSession>(endpoint);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to pause focus session');
      throw error;
    }
  }

  /**
   * Get current active session
   */
  async getCurrentSession(): Promise<FocusSession | null> {
    try {
      const response = await this.api.get<FocusSession>(API_ENDPOINTS.TIMER.CURRENT_SESSION);
      return response.data;
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 404) {
        return null; // No active session
      }
      this.handleError(error, 'Failed to get current session');
      throw error;
    }
  }

  /**
   * Get session history
   */
  async getSessionHistory(page = 0, size = 20, dateRange?: { start: string; end: string }): Promise<{
    content: FocusSession[];
    totalElements: number;
    totalPages: number;
  }> {
    try {
      const params: unknown = { page, size };
      if (dateRange) {
        params.startDate = dateRange.start;
        params.endDate = dateRange.end;
      }

      const response = await this.api.get(API_ENDPOINTS.TIMER.SESSIONS_HISTORY, { params });
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get session history');
      throw error;
    }
  }

  /**
   * Get daily productivity stats
   */
  async getDailyStats(date?: string): Promise<ProductivityStats> {
    try {
      const params = date ? { date } : {};
      const response = await this.api.get<ProductivityStats>(API_ENDPOINTS.TIMER.STATS_DAILY, { params });
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get daily stats');
      throw error;
    }
  }

  /**
   * Get weekly productivity stats
   */
  async getWeeklyStats(week?: string): Promise<ProductivityStats> {
    try {
      const params = week ? { week } : {};
      const response = await this.api.get<ProductivityStats>(API_ENDPOINTS.TIMER.STATS_WEEKLY, { params });
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get weekly stats');
      throw error;
    }
  }

  /**
   * Get monthly productivity stats
   */
  async getMonthlyStats(month?: string): Promise<ProductivityStats> {
    try {
      const params = month ? { month } : {};
      const response = await this.api.get<ProductivityStats>(API_ENDPOINTS.TIMER.STATS_MONTHLY, { params });
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get monthly stats');
      throw error;
    }
  }

  /**
   * Get streak statistics
   */
  async getStreakStats(): Promise<StreakStats> {
    try {
      const response = await this.api.get<StreakStats>(API_ENDPOINTS.TIMER.STATS_STREAK);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get streak stats');
      throw error;
    }
  }

  /**
   * Get user's Pomodoro settings
   */
  async getPomodoroSettings(): Promise<PomodoroSettings> {
    try {
      const response = await this.api.get<PomodoroSettings>(API_ENDPOINTS.TIMER.POMODORO_SETTINGS);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to get Pomodoro settings');
      throw error;
    }
  }

  /**
   * Update user's Pomodoro settings
   */
  async updatePomodoroSettings(settings: Partial<PomodoroSettings>): Promise<PomodoroSettings> {
    try {
      const response = await this.api.put<PomodoroSettings>(API_ENDPOINTS.TIMER.POMODORO_SETTINGS, settings);
      return response.data;
    } catch (error) {
      this.handleError(error, 'Failed to update Pomodoro settings');
      throw error;
    }
  }

  /**
   * Start a Pomodoro session
   */
  async startPomodoroSession(task?: string, hiveId?: number): Promise<FocusSession> {
    try {
      const settings = await this.getPomodoroSettings();
      const request: StartSessionRequest = {
        sessionType: 'POMODORO',
        targetDuration: settings.focusDuration,
        task,
        hiveId,
        pomodoroSettings: settings
      };
      return await this.startSession(request);
    } catch (error) {
      this.handleError(error, 'Failed to start Pomodoro session');
      throw error;
    }
  }

  /**
   * Start a break session (short break)
   */
  async startBreakSession(isLongBreak = false, hiveId?: number): Promise<FocusSession> {
    try {
      const settings = await this.getPomodoroSettings();
      const duration = isLongBreak ? settings.longBreakDuration : settings.shortBreakDuration;
      
      const request: StartSessionRequest = {
        sessionType: 'BREAK',
        targetDuration: duration,
        task: isLongBreak ? 'Long Break' : 'Short Break',
        hiveId
      };
      return await this.startSession(request);
    } catch (error) {
      this.handleError(error, 'Failed to start break session');
      throw error;
    }
  }

  /**
   * Start a custom focus session
   */
  async startCustomSession(duration: number, task?: string, hiveId?: number): Promise<FocusSession> {
    try {
      const request: StartSessionRequest = {
        sessionType: 'CUSTOM',
        targetDuration: duration,
        task,
        hiveId
      };
      return await this.startSession(request);
    } catch (error) {
      this.handleError(error, 'Failed to start custom session');
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
export const timerApiService = new TimerApiService();

export default timerApiService;