// Export all forum hooks from a single entry point
export * from './useForum';

// Re-export commonly used hooks with shorter names
export {
  usePosts,
  usePost,
  useCreatePost,
  useUpdatePost,
  useDeletePost,
  useReplies,
  useCreateReply,
  useCategories,
  usePopularTags,
  useSearchTags,
  useSearchPosts,
  useForumUpdates,
  useForumNotifications,
  useForumStats,
  useTrendingPosts,
  forumKeys,
  forumService,
} from './useForum';