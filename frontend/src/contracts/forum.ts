/**
 * Forum System Contract Types
 * Defines types for community discussions, posts, replies, and moderation
 */

/**
 * Post types and categories
 */
export type PostType = 'question' | 'discussion' | 'announcement' | 'tip' | 'resource';
export type PostStatus = 'draft' | 'published' | 'archived' | 'deleted' | 'locked';
export type ReplyStatus = 'visible' | 'hidden' | 'deleted';
export type ModerationStatus = 'pending' | 'approved' | 'rejected' | 'flagged';
export type ReactionType = 'like' | 'helpful' | 'insightful' | 'celebrate' | 'love';

/**
 * Forum post entity
 */
export interface ForumPost {
  id: string | number;
  userId: string | number;
  hiveId?: string | number | null;   // Optional association with a hive
  title: string;
  slug: string;                      // URL-friendly identifier
  content: string;
  type: PostType;
  status: PostStatus;
  tags: string[];
  viewCount: number;
  replyCount: number;
  likeCount: number;
  isPinned: boolean;
  isFeatured: boolean;
  isAnswered?: boolean;              // For question type posts
  acceptedReplyId?: string | number | null;   // ID of accepted answer
  createdAt: string;
  updatedAt: string;
  lastActivityAt: string;
  replies?: ForumReply[];            // Add missing replies property
  author: {
    id: string | number;
    username: string;
    avatarUrl?: string;
    reputation?: number;
  };
  stats?: {
    views: number;
    replies: number;
    likes: number;
    shares: number;
  };
}

/**
 * Forum reply/comment entity
 */
export interface ForumReply {
  id: string | number;
  postId: string | number;
  userId: string | number;
  parentId?: string | number | null; // For nested replies
  parentReplyId?: string | number | null; // Alias for parentId
  content: string;
  status: ReplyStatus;
  likeCount: number;
  isAccepted: boolean;           // Marked as accepted answer
  isEdited: boolean;
  editedAt?: string | null;
  createdAt: string;
  author: {
    id: string | number;
    username: string;
    avatarUrl?: string;
    reputation?: number;
  };
  reactions?: {
    type: ReactionType;
    count: number;
    hasReacted: boolean;
  }[];
  children?: ForumReply[];        // Nested replies
}

/**
 * Create post request
 */
export interface CreatePostRequest {
  title: string;
  content: string;
  type: PostType;
  hiveId?: string | number | null;
  categoryId?: string | number; // Add categoryId support
  tags?: string[];
  isDraft?: boolean;
}

/**
 * Update post request
 */
export interface UpdatePostRequest {
  title?: string;
  content?: string;
  type?: PostType;
  tags?: string[];
  status?: PostStatus;
}

/**
 * Create reply request
 */
export interface CreateReplyRequest {
  postId: string | number;
  content: string;
  parentId?: string | number | null;
  parentReplyId?: string | number | null; // Alias for parentId
}

/**
 * Update reply request
 */
export interface UpdateReplyRequest {
  content?: string;
}

/**
 * Forum search parameters
 */
export interface ForumSearchParams {
  query?: string;
  type?: PostType;
  tags?: string[];
  author?: string;
  hiveId?: string | number;
  status?: PostStatus;
  sortBy?: 'recent' | 'popular' | 'trending' | 'unanswered';
  page?: number;
  pageSize?: number;
  dateFrom?: string;
  dateTo?: string;
}

/**
 * Forum statistics
 */
export interface ForumStats {
  totalPosts: number;
  totalReplies: number;
  totalUsers: number;
  totalViews: number;
  todayPosts: number;
  todayReplies: number;
  activeDiscussions: number;
  unansweredQuestions: number;
  trendingTags: {
    tag: string;
    count: number;
    trend: 'up' | 'down' | 'stable';
  }[];
  topContributors: {
    userId: number;
    username: string;
    posts: number;
    replies: number;
    reputation: number;
  }[];
}

/**
 * User reputation and badges
 */
export interface UserForumProfile {
  userId: number;
  reputation: number;
  postCount: number;
  replyCount: number;
  acceptedAnswers: number;
  helpfulVotes: number;
  joinedAt: string;
  badges: ForumBadge[];
  bio?: string;
  specialties?: string[];
}

/**
 * Forum badge
 */
export interface ForumBadge {
  id: string;
  name: string;
  description: string;
  icon: string;
  tier: 'bronze' | 'silver' | 'gold' | 'platinum';
  earnedAt: string;
}

/**
 * Forum moderation
 */
export interface ModerationAction {
  id: string;
  type: 'flag' | 'hide' | 'delete' | 'lock' | 'pin' | 'feature';
  targetType: 'post' | 'reply';
  targetId: number;
  moderatorId: number;
  reason?: string;
  createdAt: string;
}

/**
 * Flag/Report content
 */
export interface FlagContentRequest {
  targetType: 'post' | 'reply';
  targetId: string | number;
  reason: 'spam' | 'inappropriate' | 'offensive' | 'misinformation' | 'other';
  description?: string;
}

/**
 * Reaction request
 */
export interface ReactionRequest {
  targetType: 'post' | 'reply';
  targetId: string | number;
  type: ReactionType;
}

/**
 * Forum notification
 */
export interface ForumNotification {
  id: string;
  userId: number;
  type: 'reply' | 'mention' | 'accepted' | 'reaction' | 'badge';
  message: string;
  sourceId: number;
  sourceType: 'post' | 'reply';
  isRead: boolean;
  createdAt: string;
}

/**
 * Paginated forum response
 */
export interface ForumPostsResponse {
  posts: ForumPost[];
  pagination: {
    page: number;
    pageSize: number;
    total: number;
    hasMore: boolean;
  };
  filters?: {
    type?: PostType;
    tags?: string[];
    status?: PostStatus;
  };
}

/**
 * Forum replies response
 */
export interface ForumRepliesResponse {
  replies: ForumReply[];
  pagination: {
    page: number;
    pageSize: number;
    total: number;
    hasMore: boolean;
  };
}

/**
 * Forum categories
 */
export interface ForumCategory {
  id: string;
  name: string;
  description: string;
  icon?: string;
  color?: string;
  postCount: number;
  isActive: boolean;
  order: number;
}

/**
 * Forum tag
 */
export interface ForumTag {
  id: string;
  name: string;
  description?: string;
  usageCount: number;
  category?: string;
  color?: string;
}

/**
 * WebSocket events for forum updates
 */
export interface ForumWebSocketEvent {
  type: 'post_created' | 'post_updated' | 'reply_created' | 'reply_updated' | 'reaction_added';
  postId?: number;
  replyId?: number;
  data: any;
  timestamp: string;
}

/**
 * Post draft for auto-saving
 */
export interface PostDraft {
  id?: string;
  title: string;
  content: string;
  type: PostType;
  tags: string[];
  lastSavedAt: string;
  autoSave: boolean;
}

/**
 * Forum preferences
 */
export interface ForumPreferences {
  emailNotifications: boolean;
  pushNotifications: boolean;
  notifyOnReply: boolean;
  notifyOnMention: boolean;
  notifyOnAccepted: boolean;
  digestFrequency: 'daily' | 'weekly' | 'never';
  defaultPostType: PostType;
  defaultSortOrder: 'recent' | 'popular' | 'trending';
}

/**
 * Additional buddy-compatible types for forum operations
 */
export interface ForumCreateReplyRequest {
  content: string;
  postId: string | number;
  parentReplyId?: string | number;
  attachments?: File[];
}

/**
 * Forum user for compatibility with buddy system
 */
export interface ForumUser {
  id: string | number;
  username: string;
  avatar?: string;
  role: 'USER' | 'MODERATOR' | 'ADMIN' | 'member';
  joinDate?: string;
  postCount?: number;
  reputation?: number;
  badges?: string[];
}

/**
 * Extended forum post with replies
 */
export interface ExtendedForumPost extends ForumPost {
  replies: ForumReply[];
  author: ForumUser;
}

/**
 * Extended forum reply with user info
 */
export interface ExtendedForumReply extends ForumReply {
  author: ForumUser;
  parentReplyId?: string | number | null;
}
