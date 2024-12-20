import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/react-query';
import type {
  ForumPost,
  ForumReply,
  ForumCategory,
  ForumTag,
  UserForumProfile,
  ForumPreferences,
  PostDraft,
  CreatePostRequest,
  UpdatePostRequest,
  CreateReplyRequest,
  UpdateReplyRequest,
  ReactionType,
  ReactionRequest,
  FlagContentRequest,
  ForumSearchParams,
  ForumPostsResponse,
  ForumRepliesResponse,
  ForumStats,
  ForumWebSocketEvent
} from '@/contracts/forum';
import { forumService } from '../services/forumService';
import { useWebSocket } from '@/hooks/useWebSocket';
import { useEffect } from 'react';

// Query keys factory
export const forumKeys = {
  all: ['forum'] as const,
  posts: () => [...forumKeys.all, 'posts'] as const,
  post: (id: number) => [...forumKeys.posts(), id] as const,
  userPosts: (userId: number) => [...forumKeys.posts(), 'user', userId] as const,
  replies: (postId: number) => [...forumKeys.posts(), postId, 'replies'] as const,
  categories: () => [...forumKeys.all, 'categories'] as const,
  tags: () => [...forumKeys.all, 'tags'] as const,
  search: (params: ForumSearchParams) => [...forumKeys.posts(), 'search', params] as const,
  userProfile: (userId: number) => [...forumKeys.all, 'profile', userId] as const,
  userPreferences: (userId: number) => [...forumKeys.all, 'preferences', userId] as const,
  drafts: () => [...forumKeys.all, 'drafts'] as const,
  draft: (id: number) => [...forumKeys.drafts(), id] as const,
  moderationQueue: () => [...forumKeys.all, 'moderation'] as const,
};

// Hook to get all posts
export function usePosts(params?: ForumSearchParams) {
  return useQuery({
    queryKey: [...forumKeys.posts(), params],
    queryFn: () => forumService.getPosts(params),
    staleTime: 30000, // 30 seconds
    gcTime: 5 * 60 * 1000, // 5 minutes
  });
}

// Hook to get infinite scroll posts
export function useInfinitePosts(params?: Omit<ForumSearchParams, 'page'>) {
  return useInfiniteQuery({
    queryKey: [...forumKeys.posts(), 'infinite', params],
    queryFn: ({ pageParam = 0 }) => forumService.getPosts({ ...params, page: pageParam }),
    getNextPageParam: (lastPage) => {
      const { pagination } = lastPage;
      if (pagination.hasMore) {
        return pagination.page + 1;
      }
      return undefined;
    },
    initialPageParam: 0,
    staleTime: 30000,
  });
}

// Hook to get a single post
export function usePost(postId: number) {
  return useQuery({
    queryKey: forumKeys.post(postId),
    queryFn: () => forumService.getPost(postId),
    staleTime: 30000,
    enabled: postId > 0,
  });
}

// Hook to create a post
export function useCreatePost() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (request: CreatePostRequest) => forumService.createPost(request),
    onSuccess: (newPost) => {
      // Invalidate posts list
      queryClient.invalidateQueries({ queryKey: forumKeys.posts() });
      // Add the new post to cache
      queryClient.setQueryData(forumKeys.post(Number(newPost.id)), newPost);
      // Clear drafts if it was from a draft
      queryClient.invalidateQueries({ queryKey: forumKeys.drafts() });
    },
  });
}

// Hook to update a post
export function useUpdatePost() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: UpdatePostRequest }) => 
      forumService.updatePost(id, request),
    onSuccess: (updatedPost) => {
      // Update the specific post in cache
      queryClient.setQueryData(forumKeys.post(Number(updatedPost.id)), updatedPost);
      // Invalidate posts list
      queryClient.invalidateQueries({ queryKey: forumKeys.posts() });
    },
  });
}

// Hook to delete a post
export function useDeletePost() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (postId: number) => forumService.deletePost(postId),
    onSuccess: (_, postId) => {
      // Remove from cache
      queryClient.removeQueries({ queryKey: forumKeys.post(postId) });
      // Invalidate posts list
      queryClient.invalidateQueries({ queryKey: forumKeys.posts() });
    },
  });
}

// Hook to get replies for a post
export function useReplies(postId: number, page = 0, pageSize = 20) {
  return useQuery({
    queryKey: [...forumKeys.replies(postId), page, pageSize],
    queryFn: () => forumService.getPostReplies(postId, page, pageSize),
    staleTime: 30000,
    enabled: postId > 0,
  });
}

// Hook to create a reply
export function useCreateReply() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateReplyRequest) =>
      forumService.createReply(request),
    onSuccess: (_, variables) => {
      // Invalidate replies for the post
      queryClient.invalidateQueries({ queryKey: forumKeys.replies(Number(variables.postId)) });
      // Update reply count in the post
      queryClient.setQueryData<ForumPost>(forumKeys.post(Number(variables.postId)), (old) => {
        if (!old) return old;
        return { ...old, replyCount: old.replyCount + 1 };
      });
    },
  });
}

// Hook to update a reply
export function useUpdateReply() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ replyId, request }: { replyId: number; request: UpdateReplyRequest }) => 
      forumService.updateReply(replyId, request),
    onSuccess: (updatedReply) => {
      // Invalidate replies for the post
      queryClient.invalidateQueries({ queryKey: forumKeys.replies(Number(updatedReply.postId)) });
    },
  });
}

// Hook to delete a reply
export function useDeleteReply() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (replyId: number) =>
      forumService.deleteReply(replyId),
    onMutate: async (replyId) => {
      // Store postId for use in onSuccess
      return { replyId };
    },
    onSuccess: () => {
      // Invalidate all replies queries as we don't know the postId here
      queryClient.invalidateQueries({ queryKey: forumKeys.posts() });
    },
  });
}

// Hook to add a reaction
export function useAddReaction() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: ReactionRequest) =>
      forumService.addReaction(request),
    onSuccess: (_, variables) => {
      // Update the post with new reaction if it's a post reaction
      if (variables.targetType === 'post') {
        queryClient.setQueryData<ForumPost>(forumKeys.post(Number(variables.targetId)), (old) => {
          if (!old) return old;
          // Increase like count for like reactions
          if (variables.type === 'like') {
            return { ...old, likeCount: old.likeCount + 1 };
          }
          return old;
        });
      }
      // Invalidate replies if it's a reply reaction
      if (variables.targetType === 'reply') {
        queryClient.invalidateQueries({ queryKey: forumKeys.all });
      }
    },
  });
}

// Hook to accept a reply as answer
export function useAcceptReply() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ postId, replyId }: { postId: number; replyId: number }) =>
      forumService.acceptReply(postId, replyId),
    onSuccess: (_, variables) => {
      // Update the post with accepted reply
      queryClient.setQueryData<ForumPost>(forumKeys.post(variables.postId), (old) => {
        if (!old) return old;
        return { ...old, isAnswered: true, acceptedReplyId: variables.replyId };
      });
      // Invalidate replies to update accepted status
      queryClient.invalidateQueries({ queryKey: forumKeys.replies(variables.postId) });
    },
  });
}

// Hook to search posts
export function useSearchPosts(params: ForumSearchParams, enabled = true) {
  return useQuery({
    queryKey: forumKeys.search(params),
    queryFn: () => forumService.searchPosts(params),
    staleTime: 60000, // 1 minute
    enabled: enabled && !!params.query,
  });
}

// Hook to get trending posts
export function useTrendingPosts() {
  return useQuery({
    queryKey: [...forumKeys.posts(), 'trending'],
    queryFn: () => forumService.getTrendingPosts(),
    staleTime: 30000,
  });
}

// Hook to get categories
export function useCategories() {
  return useQuery({
    queryKey: forumKeys.categories(),
    queryFn: () => forumService.getCategories(),
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
  });
}

// Hook to get popular tags
export function usePopularTags() {
  return useQuery({
    queryKey: forumKeys.tags(),
    queryFn: () => forumService.getPopularTags(),
    staleTime: 5 * 60 * 1000,
  });
}

// Hook to search tags
export function useSearchTags(query: string) {
  return useQuery({
    queryKey: [...forumKeys.tags(), 'search', query],
    queryFn: () => forumService.searchTags(query),
    staleTime: 60000,
    enabled: query.length > 0,
  });
}

// Hook to get user profile
export function useForumUserProfile(userId: number) {
  return useQuery({
    queryKey: forumKeys.userProfile(userId),
    queryFn: () => forumService.getUserProfile(userId),
    staleTime: 60000,
    enabled: userId > 0,
  });
}

// Hook to get forum statistics
export function useForumStats() {
  return useQuery({
    queryKey: [...forumKeys.all, 'stats'],
    queryFn: () => forumService.getForumStats(),
    staleTime: 60000,
  });
}

// Hook to get user preferences
export function useForumUserPreferences() {
  return useQuery({
    queryKey: [...forumKeys.all, 'preferences'],
    queryFn: () => forumService.getUserPreferences(),
    staleTime: 5 * 60 * 1000,
  });
}

// Hook to update user preferences
export function useUpdateForumPreferences() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (preferences: Partial<ForumPreferences>) =>
      forumService.updateUserPreferences(preferences),
    onSuccess: (updatedPreferences) => {
      queryClient.setQueryData([...forumKeys.all, 'preferences'], updatedPreferences);
    },
  });
}

// Hook to flag content
export function useFlagContent() {
  return useMutation({
    mutationFn: (request: FlagContentRequest) =>
      forumService.flagContent(request),
  });
}

// Note: Lock, unlock, pin, unpin operations would be handled via updatePost
// with appropriate status changes

// Hook to get drafts
export function useDrafts() {
  return useQuery({
    queryKey: forumKeys.drafts(),
    queryFn: () => forumService.getDrafts(),
    staleTime: 30000,
  });
}

// Hook to save a draft
export function useSaveDraft() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (draft: PostDraft) => forumService.saveDraft(draft),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: forumKeys.drafts() });
    },
  });
}

// Hook to delete a draft
export function useDeleteDraft() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (draftId: string) => forumService.deleteDraft(draftId),
    onSuccess: (_, draftId) => {
      queryClient.invalidateQueries({ queryKey: forumKeys.drafts() });
    },
  });
}

// Note: Moderation features would be implemented as needed
// These would typically require admin privileges

// Hook to subscribe to real-time forum updates
export function useForumUpdates(postId?: number) {
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!postId) return;

    // Subscribe to post updates using forumService
    const unsubscribe = forumService.subscribeToPostUpdates(postId, (event: ForumWebSocketEvent) => {
      switch (event.type) {
        case 'post_updated':
          queryClient.invalidateQueries({ queryKey: forumKeys.post(postId) });
          break;
        case 'reply_created':
        case 'reply_updated':
          queryClient.invalidateQueries({ queryKey: forumKeys.replies(postId) });
          break;
        case 'reaction_added':
          queryClient.invalidateQueries({ queryKey: forumKeys.post(postId) });
          break;
      }
    });

    return unsubscribe;
  }, [postId, queryClient]);
}

// Hook to subscribe to forum notifications
export function useForumNotifications() {
  useEffect(() => {
    const unsubscribe = forumService.subscribeToNotifications((notification) => {
      console.log('Forum notification:', notification);
      // Handle notification display or state updates
    });

    return unsubscribe;
  }, []);
}

// Export all hooks
export {
  forumService,
};
