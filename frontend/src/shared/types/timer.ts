// Timer and productivity related types
import {User} from './auth'

export interface TimerSettings {
  focusLength: number // in minutes
  shortBreakLength: number // in minutes
  longBreakLength: number // in minutes
  longBreakInterval: number // after how many focus sessions
  autoStartBreaks: boolean
  autoStartFocus: boolean
  soundEnabled: boolean
  notificationsEnabled: boolean
  selectedSounds: {
    focusStart: string
    focusEnd: string
    breakStart: string
    breakEnd: string
  }
}

export interface TimerState {
  currentPhase: 'focus' | 'short-break' | 'long-break' | 'idle'
  timeRemaining: number // in seconds
  isRunning: boolean
  isPaused: boolean
  currentCycle: number
  totalCycles: number
  sessionStartTime?: string
  currentHiveId?: string
}

export interface SessionStats {
  id: string
  userId: string
  user: User
  hiveId?: string
  date: string
  focusTime: number // in minutes
  breakTime: number // in minutes
  completedCycles: number
  targetCycles: number
  productivity: {
    rating: number // 1-5
    notes?: string
  }
  distractions: number
  goals: SessionGoal[]
  achievements: string[] // achievement IDs
}

export interface SessionGoal {
  id: string
  description: string
  isCompleted: boolean
  completedAt?: string
  priority: 'low' | 'medium' | 'high'
}

export interface ProductivityChart {
  labels: string[]
  datasets: Array<{
    label: string
    data: number[]
    backgroundColor?: string
    borderColor?: string
    fill?: boolean
  }>
}

export interface WeeklyStats {
  week: string // ISO week string
  totalFocusTime: number
  averageSessionLength: number
  completedSessions: number
  targetSessions: number
  mostProductiveDay: string
  dailyStats: DailyStats[]
}

export interface DailyStats {
  date: string
  focusTime: number
  sessions: number
  productivity: number
  goals: {
    completed: number
    total: number
  }
}

export interface TimerContextType {
  timerState: TimerState
  timerSettings: TimerSettings
  currentSession: SessionStats | null
  startTimer: (phase?: TimerState['currentPhase'], hiveId?: string) => void
  pauseTimer: () => void
  resumeTimer: () => void
  stopTimer: () => void
  skipPhase: () => void
  updateSettings: (settings: Partial<TimerSettings>) => void
  addGoal: (description: string, priority: SessionGoal['priority']) => void
  completeGoal: (goalId: string) => void
  removeGoal: (goalId: string) => void
  recordDistraction: () => void
  endSession: (productivity: SessionStats['productivity']) => void
}

export interface FocusTimerProps {
  hiveId?: string
  onSessionStart?: (session: SessionStats) => void
  onSessionEnd?: (session: SessionStats) => void
  showSettings?: boolean
  compact?: boolean
}

export interface SessionStatsProps {
  userId?: string
  period: 'today' | 'week' | 'month' | 'all'
  showCharts?: boolean
  showGoals?: boolean
}

export interface ProductivityChartProps {
  data: ProductivityChart
  type: 'line' | 'bar' | 'doughnut'
  height?: number
  showLegend?: boolean
  responsive?: boolean
}