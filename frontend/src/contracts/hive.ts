/**
 * Hive Contracts
 * Core interfaces for hive management in FocusHive
 *
 * A "Hive" is a virtual co-working space where users can work together
 * while maintaining individual focus on their tasks.
 */

/**
 * Hive types for different purposes
 */
export type HiveType =
  | 'study'       // Academic study sessions
  | 'work'        // Professional work sessions
  | 'creative'    // Creative projects (art, writing, etc.)
  | 'fitness'     // Exercise and wellness
  | 'meditation'  // Mindfulness and meditation
  | 'social'      // Social gatherings
  | 'custom';     // User-defined type

/**
 * Hive status
 */
export type HiveStatus =
  | 'active'      // Currently active and accepting members
  | 'inactive'    // Temporarily inactive
  | 'archived'    // Archived but viewable
  | 'deleted'     // Soft deleted
  | 'suspended';  // Suspended by admin

/**
 * Member roles in a hive
 */
export type HiveMemberRole =
  | 'owner'       // Created the hive, full permissions
  | 'admin'       // Administrative permissions
  | 'moderator'   // Can moderate chat and members
  | 'member'      // Regular member
  | 'guest';      // Limited access guest

/**
 * Member status in a hive
 */
export type HiveMemberStatus =
  | 'active'      // Currently active in the hive
  | 'inactive'    // Temporarily inactive
  | 'banned'      // Banned from the hive
  | 'muted'       // Muted in chat
  | 'left';       // Left the hive

/**
 * Hive invite status
 */
export type HiveInviteStatus =
  | 'pending'     // Waiting for response
  | 'accepted'    // Invite accepted
  | 'rejected'    // Invite rejected
  | 'expired'     // Invite expired
  | 'cancelled';  // Invite cancelled by sender

/**
 * Hive activity types for tracking
 */
export type HiveActivityType =
  | 'member_joined'
  | 'member_left'
  | 'session_started'
  | 'session_ended'
  | 'message_sent'
  | 'goal_created'
  | 'goal_completed'
  | 'settings_updated'
  | 'member_promoted'
  | 'member_demoted';

/**
 * Hive session status
 */
export type HiveSessionStatus =
  | 'scheduled'   // Scheduled for future
  | 'active'      // Currently running
  | 'paused'      // Temporarily paused
  | 'completed'   // Successfully completed
  | 'cancelled';  // Cancelled

/**
 * Notification settings for a hive
 */
export interface HiveNotificationSettings {
  sessionStart?: boolean;
  sessionEnd?: boolean;
  memberJoined?: boolean;
  memberLeft?: boolean;
  goalCompleted?: boolean;
  dailyReminder?: boolean;
  weeklyReport?: boolean;
  [key: string]: boolean | undefined;
}

/**
 * Hive settings and preferences
 */
export interface HiveSettings {
  allowChat: boolean;
  allowVideo: boolean;
  allowScreenShare: boolean;
  autoMute: boolean;
  sessionDuration: number; // in milliseconds
  breakDuration: number;   // in milliseconds
  maxSessionsPerDay?: number;
  minMembers?: number;
  maxConcurrentSpeakers?: number;
  theme?: string;
  language?: string;
  timezone?: string;
  notificationSettings?: HiveNotificationSettings;
  privacySettings?: {
    showMemberList?: boolean;
    showStatistics?: boolean;
    allowGuestView?: boolean;
  };
  moderationSettings?: {
    requireApprovalToSpeak?: boolean;
    requireApprovalToShare?: boolean;
    autoKickInactive?: boolean;
    inactiveTimeout?: number;
  };
}

/**
 * Hive statistics
 */
export interface HiveStatistics {
  totalSessions: number;
  totalHours: number;
  averageSessionDuration: number; // in milliseconds
  activeMembers: number;
  completionRate: number; // percentage
  weeklyActive?: number;
  monthlyActive?: number;
  topContributors?: string[]; // user IDs
  popularTimes?: Record<string, number>;
  averageRating?: number;
}

/**
 * Hive goal
 */
export interface HiveGoal {
  id: string;
  hiveId: string;
  title: string;
  description?: string;
  createdBy: string;
  createdAt: string;
  targetDate?: string;
  completed: boolean;
  completedAt?: string;
  completedBy?: string;
  priority?: 'low' | 'medium' | 'high';
  assignedTo?: string[];
  tags?: string[];
  progress?: number; // percentage
}

/**
 * Hive tag for categorization
 */
export interface HiveTag {
  id: string;
  name: string;
  color?: string;
  icon?: string;
  usageCount?: number;
}

/**
 * Hive permission
 */
export interface HivePermission {
  id: string;
  name: string;
  description?: string;
  roles: string[];
}

/**
 * Main Hive interface
 */
export interface Hive {
  id: string;
  name: string;
  description: string;
  type: HiveType;
  status: HiveStatus;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  memberCount: number;
  maxMembers: number;
  isPrivate: boolean;
  requiresApproval: boolean;
  imageUrl?: string;
  bannerUrl?: string;
  tags?: string[];
  category?: string;
  settings?: HiveSettings;
  goals?: HiveGoal[];
  statistics?: HiveStatistics;
  featuredUntil?: string;
  metadata?: Record<string, any>;
}

/**
 * Hive member
 */
export interface HiveMember {
  id: string;
  userId: string;
  hiveId: string;
  role: HiveMemberRole;
  status: HiveMemberStatus;
  joinedAt: string;
  lastActiveAt: string;
  nickname?: string;
  bio?: string;
  statistics?: Record<string, any>;
  permissions?: string[];
  settings?: {
    notifications?: boolean;
    autoJoinSessions?: boolean;
  };
}

/**
 * Hive invite
 */
export interface HiveInvite {
  id: string;
  hiveId: string;
  invitedBy: string;
  invitedUserId?: string;
  invitedEmail: string;
  status: HiveInviteStatus;
  message?: string;
  role?: HiveMemberRole;
  createdAt: string;
  expiresAt: string;
  acceptedAt?: string;
  rejectedAt?: string;
  token?: string;
}

/**
 * Hive activity log
 */
export interface HiveActivity {
  id: string;
  hiveId: string;
  userId?: string;
  type: HiveActivityType;
  description?: string;
  timestamp: string;
  data?: Record<string, any>;
  ipAddress?: string;
  userAgent?: string;
}

/**
 * Hive session
 */
export interface HiveSession {
  id: string;
  hiveId: string;
  status: HiveSessionStatus;
  title?: string;
  description?: string;
  startedAt: string;
  endedAt?: string;
  scheduledDuration: number; // in milliseconds
  actualDuration: number;    // in milliseconds
  participantCount: number;
  participants: string[];     // user IDs
  host?: string;
  recording?: {
    enabled: boolean;
    url?: string;
    duration?: number;
  };
  goals?: string[];          // goal IDs
  notes?: string;
  rating?: number;
}

/**
 * Create hive request
 */
export interface CreateHiveRequest {
  name: string;
  description: string;
  type: HiveType;
  isPrivate: boolean;
  requiresApproval: boolean;
  maxMembers: number;
  imageUrl?: string;
  bannerUrl?: string;
  tags?: string[];
  settings?: Partial<HiveSettings>;
  goals?: Omit<HiveGoal, 'id' | 'hiveId' | 'createdAt'>[];
  initialMembers?: string[];
}

/**
 * Update hive request
 */
export interface UpdateHiveRequest {
  name?: string;
  description?: string;
  type?: HiveType;
  status?: HiveStatus;
  isPrivate?: boolean;
  requiresApproval?: boolean;
  maxMembers?: number;
  imageUrl?: string;
  bannerUrl?: string;
  tags?: string[];
  settings?: Partial<HiveSettings>;
}

/**
 * Join hive request
 */
export interface JoinHiveRequest {
  hiveId: string;
  message?: string;
  referralCode?: string;
}

/**
 * Leave hive request
 */
export interface LeaveHiveRequest {
  hiveId: string;
  reason?: string;
}

/**
 * Invite to hive request
 */
export interface InviteToHiveRequest {
  hiveId: string;
  emails?: string[];
  userIds?: string[];
  message?: string;
  role?: HiveMemberRole;
  expiresIn?: number; // in milliseconds
}

/**
 * Hive response
 */
export interface HiveResponse {
  hive: Hive;
  userRole?: HiveMemberRole;
  isMember?: boolean;
  canJoin?: boolean;
  pendingInvite?: HiveInvite;
}

/**
 * Hive list response
 */
export interface HiveListResponse {
  hives: Hive[];
  total: number;
  page: number;
  pageSize: number;
  hasMore: boolean;
}

/**
 * Hive member list response
 */
export interface HiveMemberListResponse {
  members: HiveMember[];
  total: number;
  page: number;
  pageSize: number;
  hasMore: boolean;
}

/**
 * Search hives request
 */
export interface SearchHivesRequest {
  query?: string;
  type?: HiveType;
  tags?: string[];
  isPrivate?: boolean;
  minMembers?: number;
  maxMembers?: number;
  sortBy?: 'name' | 'created' | 'members' | 'activity';
  sortOrder?: 'asc' | 'desc';
  page?: number;
  pageSize?: number;
}

/**
 * Hive recommendation
 */
export interface HiveRecommendation {
  hive: Hive;
  score: number;
  reason: string;
  matchedTags?: string[];
  mutualMembers?: string[];
}

/**
 * Hive analytics
 */
export interface HiveAnalytics {
  hiveId: string;
  period: 'day' | 'week' | 'month' | 'year' | 'all';
  startDate: string;
  endDate: string;
  sessions: {
    total: number;
    completed: number;
    cancelled: number;
    averageDuration: number;
  };
  members: {
    total: number;
    active: number;
    new: number;
    left: number;
  };
  engagement: {
    messagesPerSession: number;
    participationRate: number;
    averageRating: number;
  };
  goals: {
    created: number;
    completed: number;
    completionRate: number;
  };
}

/**
 * Hive context state
 */
export interface HiveContextState {
  currentHive: Hive | null;
  myHives: Hive[];
  recommendedHives: HiveRecommendation[];
  isLoading: boolean;
  error: Error | null;
}

/**
 * Hive context methods
 */
export interface HiveContextMethods {
  createHive: (request: CreateHiveRequest) => Promise<Hive>;
  updateHive: (hiveId: string, request: UpdateHiveRequest) => Promise<Hive>;
  deleteHive: (hiveId: string) => Promise<void>;
  joinHive: (request: JoinHiveRequest) => Promise<void>;
  leaveHive: (request: LeaveHiveRequest) => Promise<void>;
  inviteToHive: (request: InviteToHiveRequest) => Promise<HiveInvite[]>;
  getHive: (hiveId: string) => Promise<HiveResponse>;
  searchHives: (request: SearchHivesRequest) => Promise<HiveListResponse>;
  getMyHives: () => Promise<Hive[]>;
  getHiveMembers: (hiveId: string) => Promise<HiveMemberListResponse>;
  updateMemberRole: (hiveId: string, userId: string, role: HiveMemberRole) => Promise<void>;
  kickMember: (hiveId: string, userId: string, reason?: string) => Promise<void>;
  startSession: (hiveId: string) => Promise<HiveSession>;
  endSession: (sessionId: string) => Promise<void>;
  clearError: () => void;
}

/**
 * Complete Hive context type
 */
export interface HiveContextType extends HiveContextState, HiveContextMethods {}