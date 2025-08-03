// Presence and real-time related types
import { User } from './auth'

export type PresenceStatus = 'online' | 'focusing' | 'break' | 'away' | 'offline'

export interface UserPresence {
  userId: string
  user: User
  status: PresenceStatus
  currentActivity?: string
  sessionStartTime?: string
  lastSeen: string
  hiveId?: string
  deviceInfo?: {
    type: 'web' | 'mobile' | 'desktop'
    browser?: string
    os?: string
  }
}

export interface PresenceUpdate {
  userId: string
  status: PresenceStatus
  activity?: string
  sessionStartTime?: string
  hiveId?: string
  timestamp: string
}

export interface HivePresenceInfo {
  hiveId: string
  activeUsers: UserPresence[]
  totalOnline: number
  totalFocusing: number
  totalOnBreak: number
}

export interface ActivityEvent {
  id: string
  userId: string
  user: User
  type: 'joined_hive' | 'left_hive' | 'started_session' | 'completed_session' | 'took_break' | 'resumed_session'
  data?: Record<string, any>
  timestamp: string
  hiveId?: string
}

export interface FocusSession {
  id: string
  userId: string
  user: User
  hiveId?: string
  startTime: string
  endTime?: string
  duration?: number // in minutes
  isActive: boolean
  type: 'pomodoro' | 'continuous' | 'custom'
  targetDuration?: number // in minutes
  breaks: SessionBreak[]
  productivity?: {
    rating: number // 1-5
    notes?: string
  }
}

export interface SessionBreak {
  id: string
  startTime: string
  endTime?: string
  duration?: number // in minutes
  type: 'short' | 'long' | 'lunch'
  isActive: boolean
}

export interface PresenceContextType {
  currentPresence: UserPresence | null
  hivePresence: Record<string, HivePresenceInfo>
  updatePresence: (status: PresenceStatus, activity?: string) => void
  joinHivePresence: (hiveId: string) => void
  leaveHivePresence: (hiveId: string) => void
  startFocusSession: (hiveId?: string, targetDuration?: number) => void
  endFocusSession: (productivity?: FocusSession['productivity']) => void
  takeBreak: (type: SessionBreak['type']) => void
  resumeFromBreak: () => void
}