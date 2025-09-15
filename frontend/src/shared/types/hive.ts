// Hive related types
import {User} from './auth'

export interface HiveBase {
  id: string
  name: string
  description: string
  ownerId: string
  owner: User
tags?: string[]
  settings: HiveSettingsBase
  currentMembers: number
  memberCount?: number // Alias for currentMembers
  status?: 'ACTIVE' | 'INACTIVE'
  createdAt: string
  updatedAt: string
}

export interface HiveSettingsBase {
  focusMode: string
  maxParticipants: number
}

export interface HiveSettings extends HiveSettingsBase {
  allowChat: boolean
  allowVoice: boolean
  requireApproval: boolean
  privacyLevel: 'PUBLIC' | 'PRIVATE' | 'INVITE_ONLY'
  category: 'STUDY' | 'WORK' | 'SOCIAL' | 'CODING'
  focusMode: 'POMODORO' | 'TIMEBLOCK' | 'FREEFORM'
  defaultSessionLength: number // in minutes
  maxSessionLength: number // in minutes
  [key: string]: any // Allow additional custom settings
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
export interface Hive extends HiveBase {
  maxMembers: number
  isPublic: boolean
  settings: HiveSettings
  isOwner: boolean
  isMember: boolean
  tags: string[]
  members?: Array<{ userId: string; joinedAt: string }>
  statistics?: {
    totalSessions: number
    totalFocusTime: number
    averageRating: number
    weeklyActiveUsers: number
  }
  nextSession?: unknown
  imageUrl?: string
}

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