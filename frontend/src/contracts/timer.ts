/**
 * Timer Contracts
 * Core interfaces for timer and time tracking functionality in FocusHive
 *
 * The timer system manages focus sessions, Pomodoro timers, and time tracking
 * across devices and sessions.
 */

/**
 * Timer types
 */
export type TimerType =
  | 'focus'      // Focus/work timer
  | 'break'      // Break timer
  | 'pomodoro'   // Pomodoro technique timer
  | 'countdown'  // Generic countdown timer
  | 'stopwatch'  // Count up timer
  | 'custom';    // User-defined timer

/**
 * Timer status
 */
export type TimerStatus =
  | 'idle'       // Not started
  | 'running'    // Currently running
  | 'paused'     // Temporarily paused
  | 'completed'  // Finished successfully
  | 'cancelled'; // Cancelled before completion

/**
 * Timer preset templates
 */
export type TimerPreset =
  | 'pomodoro_25_5'    // 25 min work, 5 min break
  | 'pomodoro_50_10'   // 50 min work, 10 min break
  | 'deep_work_90'     // 90 min deep work session
  | 'quick_focus_15'   // 15 min quick focus
  | 'long_break_30'    // 30 min long break
  | 'custom';          // Custom configuration

/**
 * Main Timer interface
 */
export interface Timer {
  id: string;
  userId: string;
  hiveId?: string;
  type: TimerType;
  status: TimerStatus;
  name?: string;
  description?: string;
  duration: number;        // Total duration in milliseconds
  elapsed: number;         // Elapsed time in milliseconds
  remaining: number;       // Remaining time in milliseconds
  startedAt?: string;
  pausedAt?: string;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
  config?: TimerConfig;
  linkedTaskId?: string;
  linkedGoalId?: string;
  syncedDevices?: string[];
  tags?: string[];
}

/**
 * Timer configuration
 */
export interface TimerConfig {
  preset?: TimerPreset;
  workDuration?: number;      // in milliseconds
  shortBreakDuration?: number; // in milliseconds
  longBreakDuration?: number;  // in milliseconds
  sessionsBeforeLongBreak?: number;
  autoStartBreaks?: boolean;
  autoStartNextSession?: boolean;
  notifications?: {
    sound?: boolean;
    desktop?: boolean;
    mobile?: boolean;
    vibration?: boolean;
    reminderBeforeEnd?: number; // milliseconds before end
  };
  theme?: {
    color?: string;
    sound?: string;
    animation?: string;
  };
}

/**
 * Timer session (completed timer instance)
 */
export interface TimerSession {
  id: string;
  timerId: string;
  userId: string;
  hiveId?: string;
  type: TimerType;
  startedAt: string;
  endedAt: string;
  plannedDuration: number;  // in milliseconds
  actualDuration: number;   // in milliseconds
  completed: boolean;
  pauseCount: number;
  totalPauseTime: number;   // in milliseconds
  productivity?: {
    focusScore?: number;
    tasksCompleted?: number;
    notes?: string;
  };
  interruptions?: Array<{
    timestamp: string;
    duration: number;
    reason?: string;
  }>;
}

/**
 * Pomodoro session
 */
export interface PomodoroSession {
  id: string;
  userId: string;
  currentCycle: number;
  totalCycles: number;
  workSessions: TimerSession[];
  breakSessions: TimerSession[];
  isWorkPhase: boolean;
  startedAt: string;
  completedAt?: string;
  config: TimerConfig;
  stats: {
    completedCycles: number;
    totalFocusTime: number;
    totalBreakTime: number;
    averageFocusScore?: number;
  };
}

/**
 * Timer statistics
 */
export interface TimerStatistics {
  userId: string;
  period: 'day' | 'week' | 'month' | 'year' | 'all';
  startDate: string;
  endDate: string;
  totalSessions: number;
  completedSessions: number;
  cancelledSessions: number;
  totalTime: number;         // in milliseconds
  focusTime: number;         // in milliseconds
  breakTime: number;         // in milliseconds
  averageSessionLength: number;
  longestSession: number;
  shortestSession: number;
  favoriteTimeOfDay?: string;
  mostProductiveDay?: string;
  pomodorosCompleted?: number;
  streakDays: number;
  currentStreak: number;
  bestStreak: number;
}

/**
 * Timer goal
 */
export interface TimerGoal {
  id: string;
  userId: string;
  type: 'daily' | 'weekly' | 'monthly' | 'custom';
  targetMinutes: number;
  currentMinutes: number;
  startDate: string;
  endDate: string;
  completed: boolean;
  completedAt?: string;
  recurring: boolean;
  reminderEnabled: boolean;
  reminderTime?: string;
}

/**
 * Create timer request
 */
export interface CreateTimerRequest {
  type: TimerType;
  name?: string;
  description?: string;
  duration: number;
  hiveId?: string;
  config?: TimerConfig;
  linkedTaskId?: string;
  linkedGoalId?: string;
  tags?: string[];
  startImmediately?: boolean;
}

/**
 * Update timer request
 */
export interface UpdateTimerRequest {
  name?: string;
  description?: string;
  duration?: number;
  config?: Partial<TimerConfig>;
  tags?: string[];
}

/**
 * Timer action request
 */
export interface TimerActionRequest {
  action: 'start' | 'pause' | 'resume' | 'stop' | 'reset' | 'skip';
  reason?: string;
  syncToDevices?: boolean;
}

/**
 * Timer sync event
 */
export interface TimerSyncEvent {
  timerId: string;
  userId: string;
  deviceId: string;
  action: string;
  timestamp: string;
  state: {
    status: TimerStatus;
    elapsed: number;
    remaining: number;
  };
}

/**
 * Timer notification
 */
export interface TimerNotification {
  id: string;
  timerId: string;
  type: 'start' | 'pause' | 'resume' | 'warning' | 'complete' | 'break';
  title: string;
  message: string;
  timestamp: string;
  actions?: Array<{
    id: string;
    label: string;
    action: string;
  }>;
}

/**
 * Timer preset configuration
 */
export interface TimerPresetConfig {
  id: string;
  name: string;
  description?: string;
  type: TimerType;
  preset: TimerPreset;
  config: TimerConfig;
  isDefault: boolean;
  isPublic: boolean;
  createdBy?: string;
  usageCount?: number;
  rating?: number;
}

/**
 * Timer analytics
 */
export interface TimerAnalytics {
  userId: string;
  date: string;
  hourlyDistribution: Record<string, number>; // hour -> minutes
  typeDistribution: Record<TimerType, number>;
  completionRate: number;
  averageFocusTime: number;
  breakCompliance: number; // percentage of breaks taken
  productivityTrend: 'increasing' | 'stable' | 'decreasing';
  recommendations?: string[];
}

/**
 * Timer context state
 */
export interface TimerContextState {
  activeTimer: Timer | null;
  timers: Timer[];
  currentPomodoro: PomodoroSession | null;
  timerSessions: TimerSession[];
  timerGoals: TimerGoal[];
  statistics: TimerStatistics | null;
  presets: TimerPresetConfig[];
  isRunning: boolean;
  isSyncing: boolean;
  error: Error | null;
}

/**
 * Timer context methods
 */
export interface TimerContextMethods {
  createTimer: (request: CreateTimerRequest) => Promise<Timer>;
  updateTimer: (timerId: string, request: UpdateTimerRequest) => Promise<Timer>;
  deleteTimer: (timerId: string) => Promise<void>;
  startTimer: (timerId: string) => Promise<void>;
  pauseTimer: (timerId: string) => Promise<void>;
  resumeTimer: (timerId: string) => Promise<void>;
  stopTimer: (timerId: string) => Promise<void>;
  resetTimer: (timerId: string) => Promise<void>;
  skipTimer: (timerId: string) => Promise<void>;
  performTimerAction: (timerId: string, request: TimerActionRequest) => Promise<void>;
  startPomodoro: (config?: TimerConfig) => Promise<PomodoroSession>;
  getTimer: (timerId: string) => Promise<Timer>;
  getTimers: () => Promise<Timer[]>;
  getTimerSessions: (timerId?: string) => Promise<TimerSession[]>;
  getTimerStatistics: (period: 'day' | 'week' | 'month') => Promise<TimerStatistics>;
  getTimerAnalytics: (date: string) => Promise<TimerAnalytics>;
  createTimerGoal: (goal: Omit<TimerGoal, 'id' | 'currentMinutes' | 'completed'>) => Promise<TimerGoal>;
  updateTimerGoal: (goalId: string, updates: Partial<TimerGoal>) => Promise<TimerGoal>;
  deleteTimerGoal: (goalId: string) => Promise<void>;
  getTimerGoals: () => Promise<TimerGoal[]>;
  syncTimer: (timerId: string, deviceId: string) => Promise<void>;
  unsyncTimer: (timerId: string, deviceId: string) => Promise<void>;
  createPreset: (preset: Omit<TimerPresetConfig, 'id' | 'usageCount' | 'rating'>) => Promise<TimerPresetConfig>;
  getPresets: () => Promise<TimerPresetConfig[]>;
  tick: () => void; // Update timer state every second
  clearError: () => void;
}

/**
 * Complete Timer context type
 */
export interface TimerContextType extends TimerContextState, TimerContextMethods {}

/**
 * Timer utility functions for calculations
 */
export interface TimerUtils {
  formatTime: (milliseconds: number) => string;
  calculateProgress: (elapsed: number, total: number) => number;
  getNextBreakType: (completedSessions: number, config: TimerConfig) => 'short' | 'long';
  calculateProductivityScore: (session: TimerSession) => number;
  estimateCompletionTime: (remaining: number) => Date;
  generateTimerSound: (type: 'start' | 'pause' | 'complete' | 'warning') => void;
}