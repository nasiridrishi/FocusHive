/**
 * Buddy System Contract Types
 * Defines types for accountability partners, matching, and buddy sessions
 */

/**
 * Buddy types and status
 */
export type BuddyStatus = 'available' | 'in_session' | 'offline' | 'busy' | 'away';
export type BuddyMatchStatus = 'pending' | 'accepted' | 'declined' | 'expired' | 'completed';
export type BuddySessionStatus = 'scheduled' | 'active' | 'paused' | 'completed' | 'cancelled';
export type MatchingPreference = 'random' | 'interests' | 'goals' | 'schedule' | 'performance';
export type SessionType = 'focus' | 'study' | 'work' | 'creative' | 'exercise' | 'meditation';
export type BuddyRole = 'initiator' | 'partner';

/**
 * Buddy profile
 */
export interface BuddyProfile {
  userId: number;
  username: string;
  avatarUrl?: string;
  bio?: string;
  interests: string[];
  goals: string[];
  preferredSessionTypes: SessionType[];
  timezone: string;
  languages: string[];
  rating: number;
  totalSessions: number;
  completionRate: number;
  isVerified: boolean;
  badges?: BuddyBadge[];
  availability?: BuddyAvailability;
  preferences?: BuddyPreferences;
  createdAt: string;
  lastActiveAt: string;
}

/**
 * Buddy availability schedule
 */
export interface BuddyAvailability {
  monday: TimeSlot[];
  tuesday: TimeSlot[];
  wednesday: TimeSlot[];
  thursday: TimeSlot[];
  friday: TimeSlot[];
  saturday: TimeSlot[];
  sunday: TimeSlot[];
  exceptions?: DateException[];
}

/**
 * Time slot for availability
 */
export interface TimeSlot {
  startTime: string;  // HH:MM format
  endTime: string;    // HH:MM format
  sessionTypes?: SessionType[];
}

/**
 * Date exception for availability
 */
export interface DateException {
  date: string;
  available: boolean;
  slots?: TimeSlot[];
}

/**
 * Buddy matching preferences
 */
export interface BuddyPreferences {
  matchingPreference: MatchingPreference;
  minRating?: number;
  maxDistance?: number; // km for location-based matching
  ageRange?: {
    min: number;
    max: number;
  };
  preferredLanguages?: string[];
  excludedUserIds?: number[];
  autoAccept: boolean;
  notifyOnMatch: boolean;
  sessionReminders: boolean;
}

/**
 * Buddy match request
 */
export interface BuddyMatchRequest {
  userId: number;
  targetUserId?: number; // Optional for specific buddy request
  sessionType: SessionType;
  sessionGoal?: string;
  duration: number; // minutes
  scheduledFor?: string; // ISO date string for future sessions
  message?: string;
  preferences?: MatchingPreference;
}

/**
 * Buddy match result
 */
export interface BuddyMatch {
  id: string;
  requesterId: number;
  partnerId: number;
  status: BuddyMatchStatus;
  sessionType: SessionType;
  sessionGoal?: string;
  duration: number;
  scheduledFor?: string;
  matchedAt: string;
  expiresAt: string;
  requester: BuddyProfile;
  partner: BuddyProfile;
  matchScore?: number;
  matchReasons?: string[];
}

/**
 * Buddy session
 */
export interface BuddySession {
  id: string;
  matchId: string;
  status: BuddySessionStatus;
  sessionType: SessionType;
  goal?: string;
  plannedDuration: number;
  actualDuration?: number;
  startedAt?: string;
  endedAt?: string;
  pausedAt?: string;
  participants: BuddyParticipant[];
  checkIns: BuddyCheckIn[];
  notes?: SessionNote[];
  rating?: SessionRating;
  achievements?: SessionAchievement[];
  nextSessionId?: string;
}

/**
 * Buddy participant in session
 */
export interface BuddyParticipant {
  userId: number;
  username: string;
  role: BuddyRole;
  status: BuddyStatus;
  joinedAt: string;
  leftAt?: string;
  focusScore?: number;
  tasksCompleted?: number;
  breaks?: number;
}

/**
 * Buddy check-in during session
 */
export interface BuddyCheckIn {
  id: string;
  userId: number;
  timestamp: string;
  status: 'on_track' | 'struggling' | 'completed_task' | 'taking_break';
  message?: string;
  progress?: number; // 0-100
}

/**
 * Session note
 */
export interface SessionNote {
  id: string;
  userId: number;
  content: string;
  isShared: boolean;
  createdAt: string;
}

/**
 * Session rating and feedback
 */
export interface SessionRating {
  sessionId: string;
  raterId: number;
  partnerId: number;
  overallRating: number; // 1-5
  punctuality: number; // 1-5
  focus: number; // 1-5
  helpfulness: number; // 1-5
  wouldRepeat: boolean;
  feedback?: string;
  tags?: string[];
  createdAt: string;
}

/**
 * Session achievement
 */
export interface SessionAchievement {
  type: 'focus_streak' | 'task_completion' | 'mutual_goal' | 'perfect_attendance' | 'encouragement';
  name: string;
  description: string;
  icon?: string;
  earnedAt: string;
}

/**
 * Buddy badge
 */
export interface BuddyBadge {
  id: string;
  name: string;
  description: string;
  icon: string;
  tier: 'bronze' | 'silver' | 'gold' | 'platinum';
  requirement: string;
  earnedAt: string;
}

/**
 * Buddy statistics
 */
export interface BuddyStats {
  userId: number;
  totalSessions: number;
  totalHours: number;
  averageSessionLength: number;
  completionRate: number;
  averageRating: number;
  totalBuddies: number;
  repeatBuddies: number;
  currentStreak: number;
  longestStreak: number;
  favoriteSessionType: SessionType;
  mostProductiveTime: string;
  badges: BuddyBadge[];
  recentSessions: BuddySession[];
  topBuddies: {
    userId: number;
    username: string;
    sessionCount: number;
    averageRating: number;
  }[];
}

/**
 * Buddy invitation
 */
export interface BuddyInvitation {
  id: string;
  fromUserId: number;
  toUserId: number;
  sessionType: SessionType;
  message?: string;
  scheduledFor?: string;
  expiresAt: string;
  status: 'pending' | 'accepted' | 'declined' | 'expired';
  createdAt: string;
  respondedAt?: string;
}

/**
 * Buddy search filters
 */
export interface BuddySearchFilters {
  sessionType?: SessionType;
  interests?: string[];
  goals?: string[];
  minRating?: number;
  languages?: string[];
  timezone?: string;
  availability?: 'now' | 'today' | 'this_week';
  isVerified?: boolean;
}

/**
 * Create session request
 */
export interface CreateSessionRequest {
  matchId: string;
  sessionType: SessionType;
  goal?: string;
  duration: number;
  scheduledFor?: string;
}

/**
 * Update session request
 */
export interface UpdateSessionRequest {
  status?: BuddySessionStatus;
  goal?: string;
  notes?: string;
  actualDuration?: number;
}

/**
 * Session check-in request
 */
export interface SessionCheckInRequest {
  status: 'on_track' | 'struggling' | 'completed_task' | 'taking_break';
  message?: string;
  progress?: number;
}

/**
 * Rate session request
 */
export interface RateSessionRequest {
  overallRating: number;
  punctuality?: number;
  focus?: number;
  helpfulness?: number;
  wouldRepeat?: boolean;
  feedback?: string;
  tags?: string[];
}

/**
 * Buddy WebSocket events
 */
export interface BuddyWebSocketEvent {
  type: 'match_found' | 'match_accepted' | 'match_declined' |
        'session_started' | 'session_ended' | 'buddy_checkin' |
        'buddy_left' | 'message_received' | 'invitation_received';
  matchId?: string;
  sessionId?: string;
  userId?: number;
  data?: any;
  timestamp: string;
}

/**
 * Buddy notification
 */
export interface BuddyNotification {
  id: string;
  userId: number;
  type: 'match_request' | 'match_accepted' | 'session_reminder' |
        'session_started' | 'buddy_message' | 'rating_received';
  title: string;
  message: string;
  data?: {
    matchId?: string;
    sessionId?: string;
    buddyId?: number;
  };
  isRead: boolean;
  createdAt: string;
}

/**
 * Buddy message (during session)
 */
export interface BuddyMessage {
  id: string;
  sessionId: string;
  senderId: number;
  content: string;
  type: 'text' | 'emoji' | 'encouragement' | 'task_update';
  createdAt: string;
  editedAt?: string;
  reactions?: {
    userId: number;
    emoji: string;
  }[];
}

/**
 * API Response types
 */
export interface BuddyProfileResponse {
  profile: BuddyProfile;
}

export interface BuddyMatchResponse {
  match: BuddyMatch;
}

export interface BuddySessionResponse {
  session: BuddySession;
}

export interface BuddySearchResponse {
  buddies: BuddyProfile[];
  pagination: {
    page: number;
    pageSize: number;
    total: number;
    hasMore: boolean;
  };
}

export interface BuddyStatsResponse {
  stats: BuddyStats;
}

/**
 * Paginated response wrapper
 */
export interface PaginatedBuddyResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}