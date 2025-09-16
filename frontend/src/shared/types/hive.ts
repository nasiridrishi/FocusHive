// Hive related types
import {User} from './auth'

export interface Hive {
  id: string
  name: string
  description: string
  ownerId: string
  owner: User
  maxMembers: number
  isPublic: boolean
  tags: string[]
  settings: HiveSettings
  currentMembers: number
  memberCount: number // Alias for currentMembers
  isOwner: boolean
  isMember: boolean
  createdAt: string
  updatedAt: string
}

export interface HiveSettings {
  allowChat: boolean
  allowVoice: boolean
  requireApproval: boolean
  focusMode: 'pomodoro' | 'continuous' | 'flexible'
  defaultSessionLength: number // in minutes
  maxSessionLength: number // in minutes
}

export interface HiveMember {
  id: string
  userId: string
  user: User
  hiveId: string
  role: 'owner' | 'moderator' | 'member'
  joinedAt: string
  isActive: boolean
  permissions: HiveMemberPermissions
}

export interface HiveMemberPermissions {
  canInviteMembers: boolean
  canModerateChat: boolean
  canManageSettings: boolean
  canStartTimers: boolean
}

// Response type for API operations
export type HiveResponse = Hive

export interface CreateHiveRequest {
  name: string
  description: string
  maxMembers: number
  isPublic: boolean
  tags: string[]
  settings: HiveSettings
}

export interface UpdateHiveRequest {
  name?: string
  description?: string
  maxMembers?: number
  isPublic?: boolean
  tags?: string[]
  settings?: Partial<HiveSettings>
}

export interface JoinHiveRequest {
  hiveId: string
  message?: string // for approval-required hives
}

export interface HiveInvitation {
  id: string
  hiveId: string
  hive: Hive
  inviterId: string
  inviter: User
  inviteeId: string
  invitee: User
  status: 'pending' | 'accepted' | 'rejected' | 'expired'
  message?: string
  createdAt: string
  expiresAt: string
}

export interface HiveStats {
  totalFocusTime: number // in minutes
  averageSessionLength: number
  completedSessions: number
  totalMembers: number
  activeMembers: number
  popularTimes: Array<{
    hour: number
    count: number
  }>
}

export interface HiveSearchFilters {
  query?: string
  tags?: string[]
  isPublic?: boolean
  maxMembers?: number
  hasSpots?: boolean
  sortBy?: 'name' | 'members' | 'activity' | 'created'
  sortOrder?: 'asc' | 'desc'
  page?: number
  size?: number
}