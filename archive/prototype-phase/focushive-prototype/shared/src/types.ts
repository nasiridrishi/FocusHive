// User Types
export interface User {
  id: string;
  email: string;
  username: string;
  password: string; // Hashed
  avatar: string;
  totalFocusTime: number; // in minutes
  currentStreak: number; // in days
  longestStreak: number; // in days
  points: number;
  lastActiveDate: string; // ISO string
  lookingForBuddy: boolean;
  preferences: UserPreferences;
  createdAt: string;
  updatedAt: string;
}

export interface UserPreferences {
  darkMode: boolean;
  soundEnabled: boolean;
  defaultPomodoro: {
    focusDuration: number;
    breakDuration: number;
  };
}

// Room Types
export interface Room {
  id: string;
  name: string;
  description?: string;
  type: 'public' | 'private';
  focusType: 'deepWork' | 'study' | 'creative' | 'meeting' | 'other';
  isPublic: boolean;
  password?: string;
  maxParticipants: number;
  ownerId: string;
  pomodoroSettings?: PomodoroSettings;
  timerState?: TimerState;
  participants: string[]; // User IDs
  bannedUsers?: string[]; // User IDs
  tags: string[];
  stats?: RoomStats;
  createdAt: string;
  updatedAt: string;
}

export interface PomodoroSettings {
  focusDuration: number; // minutes
  shortBreakDuration: number; // minutes
  longBreakDuration: number; // minutes
  sessionsUntilLongBreak: number;
}

// Timer types
export type TimerPhase = 'work' | 'shortBreak' | 'longBreak';
export type TimerStatus = 'idle' | 'running' | 'paused';

export interface TimerState {
  roomId: string;
  phase: TimerPhase;
  status: TimerStatus;
  duration: number; // Total duration in seconds
  remaining: number; // Remaining seconds
  startedAt?: number; // Timestamp when started
  pausedAt?: number; // Timestamp when paused
  pausedDuration: number; // Total paused time in ms
  sessionCount: number; // Number of completed work sessions
  startedBy?: string; // User who started the timer
}

export interface TimerConfig {
  workDuration: number; // seconds
  shortBreakDuration: number; // seconds
  longBreakDuration: number; // seconds
  sessionsUntilLongBreak: number;
}

export interface RoomStats {
  totalSessions: number;
  totalFocusTime: number; // minutes
  peakParticipants: number;
}

// Participant Types
export interface ParticipantStatus {
  userId: string;
  status: 'focusing' | 'break' | 'away' | 'idle';
  currentTask?: string;
  joinedAt: number;
  lastActivity?: number;
  sessionFocusTime: number;
}

// Session Types
export interface Session {
  id: string;
  userId: string;
  roomId: string;
  startTime: number;
  endTime?: number;
  duration: number; // minutes
  type: 'focus' | 'break';
  completed: boolean;
  pointsEarned: number;
}

// Chat Types
export interface ChatMessage {
  id: string;
  roomId: string;
  userId: string;
  username: string;
  message: string;
  timestamp: string;
  type: 'user' | 'system';
}

// Leaderboard Types
export interface LeaderboardEntry {
  userId: string;
  username: string;
  avatar: string;
  focusTime: number;
  points: number;
  streak: number;
  rank?: number;
}

// API Response Types
export interface AuthResponse {
  user: User;
  token: string;
}

export interface ApiError {
  error: string;
  message?: string;
  statusCode?: number;
}

// Re-export buddy types
export * from './types/buddy';
export * from './types/forum';