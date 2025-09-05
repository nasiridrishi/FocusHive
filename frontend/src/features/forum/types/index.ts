// Re-export forum types from buddy types (since they're in the same file for now)
export type {
  ForumUser,
  ForumCategory,
  ForumPost,
  ForumReply,
  ForumAttachment,
  ForumNotification,
  ForumStats,
  ForumSearchResult,
  ForumCreatePostRequest,
  ForumCreateReplyRequest,
  ForumModeration
} from '../../buddy/types'