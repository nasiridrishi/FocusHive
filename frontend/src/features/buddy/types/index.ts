// Buddy System Types

export interface BuddyRelationship {
  id: number
  user1Id: number
  user1Username: string
  user1Avatar?: string
  user2Id: number
  user2Username: string
  user2Avatar?: string
  status: 'PENDING' | 'ACTIVE' | 'REJECTED' | 'TERMINATED' | 'COMPLETED'
  startDate?: string
  endDate?: string
  terminationReason?: string
  totalGoals?: number
  completedGoals?: number
  totalSessions?: number
  totalCheckins?: number
  partnerId?: number
  partnerUsername?: string
  partnerAvatar?: string
  isInitiator?: boolean
  createdAt: string
  updatedAt: string
}

export interface BuddyRequest {
  toUserId: number
  message?: string
  proposedEndDate?: string
  goals?: string
  expectations?: string
}

export interface BuddyMatch {
  userId: number
  username: string
  avatar?: string
  bio?: string
  matchScore: number
  commonFocusAreas: string[]
  timezoneOverlapHours: number
  communicationStyle: string
  matchReasons: Record<string, any>
  activeBuddyCount: number
  completedGoalsCount: number
  averageSessionRating?: number
}

export interface BuddyPreferences {
  id?: number
  userId?: number
  preferredTimezone?: string
  preferredWorkHours?: Record<string, WorkHours>
  focusAreas?: string[]
  communicationStyle?: 'FREQUENT' | 'MODERATE' | 'MINIMAL'
  matchingEnabled: boolean
}

export interface WorkHours {
  startHour: number
  endHour: number
}

export interface BuddyGoal {
  id?: number
  relationshipId: number
  title: string
  description?: string
  status: 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
  dueDate?: string
  completedAt?: string
  completedBy?: number
  completedByUsername?: string
  metrics?: Record<string, any>
  progressPercentage?: number
  createdAt?: string
  updatedAt?: string
}

export interface BuddyCheckin {
  id?: number
  relationshipId: number
  initiatedById?: number
  initiatedByUsername?: string
  checkinTime?: string
  moodRating?: number
  progressRating?: number
  message?: string
  currentFocus?: string
  challenges?: string
  wins?: string
  createdAt?: string
}

export interface BuddySession {
  id?: number
  relationshipId: number
  sessionDate: string
  plannedDurationMinutes: number
  actualDurationMinutes?: number
  agenda?: string
  notes?: string
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW'
  user1Joined?: string
  user1Left?: string
  user2Joined?: string
  user2Left?: string
  user1Rating?: number
  user1Feedback?: string
  user2Rating?: number
  user2Feedback?: string
  cancelledAt?: string
  cancelledBy?: number
  cancellationReason?: string
  averageRating?: number
  createdAt?: string
}

export interface BuddyStats {
  relationshipId?: number
  totalBuddyRelationships?: number
  activeBuddies?: number
  completedRelationships?: number
  totalGoals?: number
  completedGoals?: number
  goalCompletionRate?: number
  totalSessions?: number
  completedSessions?: number
  sessionCompletionRate?: number
  totalSessionMinutes?: number
  averageSessionDuration?: number
  averageSessionRating?: number
  totalCheckins?: number
  averageMoodRating?: number
  averageProgressRating?: number
  buddyRating?: number
  buddyLevel?: string
}

// Forum System Types

export interface ForumUser {
  id: number
  username: string
  avatar?: string
  role: 'USER' | 'MODERATOR' | 'ADMIN'
  joinDate: string
  postCount: number
  reputation: number
  badges?: string[]
}

export interface ForumCategory {
  id: number
  name: string
  description: string
  slug: string
  color?: string
  icon?: string
  postCount: number
  topicCount: number
  lastActivity?: string
  lastPost?: ForumPost
  parentCategoryId?: number
  subcategories?: ForumCategory[]
  moderators?: ForumUser[]
  isLocked: boolean
  isPrivate: boolean
  requiredRole?: 'USER' | 'MODERATOR' | 'ADMIN'
  order: number
  createdAt: string
  updatedAt: string
}

export interface ForumPost {
  id: number
  title: string
  content: string
  slug: string
  categoryId: number
  category?: ForumCategory
  authorId: number
  author: ForumUser
  isPinned: boolean
  isLocked: boolean
  isHidden: boolean
  viewCount: number
  replyCount: number
  likeCount: number
  dislikeCount: number
  lastReplyAt?: string
  lastReply?: ForumReply
  tags?: string[]
  attachments?: ForumAttachment[]
  createdAt: string
  updatedAt: string
}

export interface ForumReply {
  id: number
  content: string
  postId: number
  post?: ForumPost
  authorId: number
  author: ForumUser
  parentReplyId?: number
  parentReply?: ForumReply
  childReplies?: ForumReply[]
  isHidden: boolean
  isModeratorReply: boolean
  likeCount: number
  dislikeCount: number
  attachments?: ForumAttachment[]
  editedAt?: string
  editedBy?: ForumUser
  createdAt: string
  updatedAt: string
}

export interface ForumAttachment {
  id: number
  filename: string
  originalName: string
  mimeType: string
  size: number
  url: string
  thumbnailUrl?: string
  postId?: number
  replyId?: number
  uploadedById: number
  uploadedBy: ForumUser
  createdAt: string
}

export interface ForumNotification {
  id: number
  userId: number
  type: 'NEW_REPLY' | 'POST_LIKED' | 'REPLY_LIKED' | 'MENTION' | 'MODERATOR_ACTION'
  title: string
  message: string
  relatedPostId?: number
  relatedReplyId?: number
  relatedUserId?: number
  isRead: boolean
  actionUrl?: string
  createdAt: string
}

export interface ForumStats {
  totalPosts: number
  totalReplies: number
  totalUsers: number
  totalCategories: number
  todayPosts: number
  todayReplies: number
  mostActiveCategory?: ForumCategory
  newestMember?: ForumUser
  totalViews: number
  averageRepliesPerPost: number
}

export interface ForumSearchResult {
  posts: ForumPost[]
  replies: ForumReply[]
  users: ForumUser[]
  totalResults: number
  query: string
  filters?: {
    categoryId?: number
    authorId?: number
    dateRange?: {
      start: string
      end: string
    }
    sortBy?: 'relevance' | 'date' | 'replies' | 'views'
    tags?: string[]
  }
}

export interface ForumCreatePostRequest {
  title: string
  content: string
  categoryId: number
  tags?: string[]
  isPinned?: boolean
  isLocked?: boolean
  attachments?: File[]
}

export interface ForumCreateReplyRequest {
  content: string
  postId: number
  parentReplyId?: number
  attachments?: File[]
}

export interface ForumModeration {
  id: number
  actionType: 'HIDE_POST' | 'LOCK_POST' | 'PIN_POST' | 'MOVE_POST' | 'DELETE_POST' | 'HIDE_REPLY' | 'DELETE_REPLY' | 'BAN_USER' | 'WARN_USER'
  targetType: 'POST' | 'REPLY' | 'USER'
  targetId: number
  moderatorId: number
  moderator: ForumUser
  reason: string
  notes?: string
  isActive: boolean
  expiresAt?: string
  createdAt: string
}