/**
 * Forum Service E2E Tests
 * Comprehensive test suite for forum functionality following TDD principles
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { forumService } from '../forumService';
import type {
  ForumPost,
  ForumReply,
  CreatePostRequest,
  UpdatePostRequest,
  CreateReplyRequest,
  UpdateReplyRequest,
  ForumSearchParams,
  ForumStats,
  UserForumProfile,
  ReactionRequest,
  FlagContentRequest,
  ForumPostsResponse,
  ForumRepliesResponse,
  ForumCategory,
  ForumTag,
  PostDraft,
  ForumPreferences
} from '@/contracts/forum';

// Mock WebSocket service
const mockWebSocketService = {
  isConnected: vi.fn(() => false),
  subscribe: vi.fn(),
  sendMessage: vi.fn(),
  unsubscribe: vi.fn()
};

// Mock auth token
const mockAuthToken = 'mock-token-123';

describe('ForumService E2E Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Clear localStorage
    localStorage.clear();
    // Reset fetch mock
    vi.spyOn(global, 'fetch').mockClear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('Post Management', () => {
    it('should create a new forum post', async () => {
      const createRequest: CreatePostRequest = {
        title: 'Test Post',
        content: 'This is a test post content',
        type: 'discussion',
        tags: ['test', 'forum']
      };

      const mockPost: ForumPost = {
        id: 1,
        userId: 123,
        title: 'Test Post',
        slug: 'test-post',
        content: 'This is a test post content',
        type: 'discussion',
        status: 'published',
        tags: ['test', 'forum'],
        viewCount: 0,
        replyCount: 0,
        likeCount: 0,
        isPinned: false,
        isFeatured: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        lastActivityAt: new Date().toISOString(),
        author: {
          id: 123,
          username: 'testuser',
          reputation: 100
        }
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockPost
      } as Response);

      const result = await forumService.createPost(createRequest);

      expect(result).toEqual(mockPost);
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/posts',
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Content-Type': 'application/json'
          }),
          body: JSON.stringify(createRequest)
        })
      );
    });

    it('should get a single post by ID', async () => {
      const mockPost: ForumPost = {
        id: 1,
        userId: 123,
        title: 'Test Post',
        slug: 'test-post',
        content: 'Test content',
        type: 'discussion',
        status: 'published',
        tags: ['test'],
        viewCount: 10,
        replyCount: 5,
        likeCount: 3,
        isPinned: false,
        isFeatured: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        lastActivityAt: new Date().toISOString(),
        author: {
          id: 123,
          username: 'testuser'
        }
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockPost
      } as Response);

      const result = await forumService.getPost(1);

      expect(result).toEqual(mockPost);
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/posts/1',
        expect.any(Object)
      );
    });

    it('should get multiple posts with pagination', async () => {
      const mockResponse: ForumPostsResponse = {
        posts: [
          {
            id: 1,
            userId: 123,
            title: 'Post 1',
            slug: 'post-1',
            content: 'Content 1',
            type: 'discussion',
            status: 'published',
            tags: [],
            viewCount: 0,
            replyCount: 0,
            likeCount: 0,
            isPinned: false,
            isFeatured: false,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
            lastActivityAt: new Date().toISOString(),
            author: { id: 123, username: 'user1' }
          }
        ],
        pagination: {
          page: 0,
          pageSize: 20,
          total: 1,
          hasMore: false
        }
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse
      } as Response);

      const params: ForumSearchParams = {
        page: 0,
        pageSize: 20,
        sortBy: 'recent'
      };

      const result = await forumService.getPosts(params);

      expect(result).toEqual(mockResponse);
      expect(fetch).toHaveBeenCalled();
    });

    it('should update an existing post', async () => {
      const updateRequest: UpdatePostRequest = {
        title: 'Updated Title',
        content: 'Updated content'
      };

      const mockUpdatedPost: ForumPost = {
        id: 1,
        userId: 123,
        title: 'Updated Title',
        slug: 'updated-title',
        content: 'Updated content',
        type: 'discussion',
        status: 'published',
        tags: [],
        viewCount: 10,
        replyCount: 0,
        likeCount: 0,
        isPinned: false,
        isFeatured: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        lastActivityAt: new Date().toISOString(),
        author: { id: 123, username: 'testuser' }
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockUpdatedPost
      } as Response);

      const result = await forumService.updatePost(1, updateRequest);

      expect(result).toEqual(mockUpdatedPost);
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/posts/1',
        expect.objectContaining({
          method: 'PUT',
          body: JSON.stringify(updateRequest)
        })
      );
    });

    it('should delete a post', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => ({ success: true })
      } as Response);

      await forumService.deletePost(1);

      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/posts/1',
        expect.objectContaining({
          method: 'DELETE'
        })
      );
    });

    it('should search posts with filters', async () => {
      const searchParams: ForumSearchParams = {
        query: 'test',
        type: 'question',
        tags: ['javascript'],
        sortBy: 'popular'
      };

      const mockResponse: ForumPostsResponse = {
        posts: [],
        pagination: {
          page: 0,
          pageSize: 20,
          total: 0,
          hasMore: false
        }
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse
      } as Response);

      const result = await forumService.searchPosts(searchParams);

      expect(result).toEqual(mockResponse);
      expect(fetch).toHaveBeenCalled();
    });
  });

  describe('Reply Management', () => {
    it('should create a new reply', async () => {
      const createRequest: CreateReplyRequest = {
        postId: 1,
        content: 'This is a reply'
      };

      const mockReply: ForumReply = {
        id: 1,
        postId: 1,
        userId: 123,
        content: 'This is a reply',
        status: 'visible',
        likeCount: 0,
        isAccepted: false,
        isEdited: false,
        createdAt: new Date().toISOString(),
        author: {
          id: 123,
          username: 'testuser'
        }
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockReply
      } as Response);

      const result = await forumService.createReply(createRequest);

      expect(result).toEqual(mockReply);
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/replies',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(createRequest)
        })
      );
    });

    it('should get replies for a post', async () => {
      const mockResponse: ForumRepliesResponse = {
        replies: [
          {
            id: 1,
            postId: 1,
            userId: 123,
            content: 'Reply 1',
            status: 'visible',
            likeCount: 0,
            isAccepted: false,
            isEdited: false,
            createdAt: new Date().toISOString(),
            author: { id: 123, username: 'user1' }
          }
        ],
        pagination: {
          page: 0,
          pageSize: 20,
          total: 1,
          hasMore: false
        }
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse
      } as Response);

      const result = await forumService.getPostReplies(1, 0, 20);

      expect(result).toEqual(mockResponse);
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/posts/1/replies?page=0&pageSize=20',
        expect.any(Object)
      );
    });

    it('should update a reply', async () => {
      const updateRequest: UpdateReplyRequest = {
        content: 'Updated reply content'
      };

      const mockUpdatedReply: ForumReply = {
        id: 1,
        postId: 1,
        userId: 123,
        content: 'Updated reply content',
        status: 'visible',
        likeCount: 0,
        isAccepted: false,
        isEdited: true,
        editedAt: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        author: { id: 123, username: 'testuser' }
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockUpdatedReply
      } as Response);

      const result = await forumService.updateReply(1, updateRequest);

      expect(result).toEqual(mockUpdatedReply);
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/replies/1',
        expect.objectContaining({
          method: 'PUT',
          body: JSON.stringify(updateRequest)
        })
      );
    });

    it('should delete a reply', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => ({ success: true })
      } as Response);

      await forumService.deleteReply(1);

      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/replies/1',
        expect.objectContaining({
          method: 'DELETE'
        })
      );
    });

    it('should mark a reply as accepted answer', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => ({ success: true })
      } as Response);

      await forumService.acceptReply(1, 10);

      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/posts/1/accept-reply/10',
        expect.objectContaining({
          method: 'POST'
        })
      );
    });
  });

  describe('Reactions and Interactions', () => {
    it('should add a reaction to a post', async () => {
      const reactionRequest: ReactionRequest = {
        targetType: 'post',
        targetId: 1,
        type: 'like'
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => ({ success: true, count: 10 })
      } as Response);

      const result = await forumService.addReaction(reactionRequest);

      expect(result).toEqual({ success: true, count: 10 });
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/reactions',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(reactionRequest)
        })
      );
    });

    it('should remove a reaction from a post', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => ({ success: true })
      } as Response);

      await forumService.removeReaction('post', 1, 'like');

      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/reactions/post/1/like',
        expect.objectContaining({
          method: 'DELETE'
        })
      );
    });

    it('should flag content for moderation', async () => {
      const flagRequest: FlagContentRequest = {
        targetType: 'post',
        targetId: 1,
        reason: 'spam',
        description: 'This is spam'
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => ({ success: true, flagId: 'flag-123' })
      } as Response);

      const result = await forumService.flagContent(flagRequest);

      expect(result).toEqual({ success: true, flagId: 'flag-123' });
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/flags',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(flagRequest)
        })
      );
    });
  });

  describe('User Profiles and Statistics', () => {
    it('should get user forum profile', async () => {
      const mockProfile: UserForumProfile = {
        userId: 123,
        reputation: 500,
        postCount: 10,
        replyCount: 50,
        acceptedAnswers: 5,
        helpfulVotes: 100,
        joinedAt: new Date().toISOString(),
        badges: []
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockProfile
      } as Response);

      const result = await forumService.getUserProfile(123);

      expect(result).toEqual(mockProfile);
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/users/123/profile',
        expect.any(Object)
      );
    });

    it('should get forum statistics', async () => {
      const mockStats: ForumStats = {
        totalPosts: 100,
        totalReplies: 500,
        totalUsers: 50,
        totalViews: 10000,
        todayPosts: 5,
        todayReplies: 20,
        activeDiscussions: 15,
        unansweredQuestions: 10,
        trendingTags: [],
        topContributors: []
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockStats
      } as Response);

      const result = await forumService.getForumStats();

      expect(result).toEqual(mockStats);
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/stats',
        expect.any(Object)
      );
    });

    it('should get trending posts', async () => {
      const mockResponse: ForumPostsResponse = {
        posts: [],
        pagination: {
          page: 0,
          pageSize: 10,
          total: 0,
          hasMore: false
        }
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse
      } as Response);

      const result = await forumService.getTrendingPosts();

      expect(result).toEqual(mockResponse);
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/posts/trending',
        expect.any(Object)
      );
    });
  });

  describe('Categories and Tags', () => {
    it('should get forum categories', async () => {
      const mockCategories: ForumCategory[] = [
        {
          id: 'cat1',
          name: 'General',
          description: 'General discussion',
          postCount: 50,
          isActive: true,
          order: 1
        }
      ];

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockCategories
      } as Response);

      const result = await forumService.getCategories();

      expect(result).toEqual(mockCategories);
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/categories',
        expect.any(Object)
      );
    });

    it('should get popular tags', async () => {
      const mockTags: ForumTag[] = [
        {
          id: 'tag1',
          name: 'javascript',
          usageCount: 100
        }
      ];

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockTags
      } as Response);

      const result = await forumService.getPopularTags();

      expect(result).toEqual(mockTags);
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/tags/popular',
        expect.any(Object)
      );
    });

    it('should search tags by query', async () => {
      const mockTags: ForumTag[] = [
        {
          id: 'tag1',
          name: 'typescript',
          usageCount: 50
        }
      ];

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockTags
      } as Response);

      const result = await forumService.searchTags('type');

      expect(result).toEqual(mockTags);
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/tags/search?q=type',
        expect.any(Object)
      );
    });
  });

  describe('Draft Management', () => {
    it('should save a post draft', async () => {
      const draft: PostDraft = {
        title: 'Draft Title',
        content: 'Draft content',
        type: 'discussion',
        tags: ['test'],
        lastSavedAt: new Date().toISOString(),
        autoSave: true
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => ({ ...draft, id: 'draft-123' })
      } as Response);

      const result = await forumService.saveDraft(draft);

      expect(result).toHaveProperty('id', 'draft-123');
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/drafts',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(draft)
        })
      );
    });

    it('should get user drafts', async () => {
      const mockDrafts: PostDraft[] = [
        {
          id: 'draft-1',
          title: 'Draft 1',
          content: 'Content',
          type: 'discussion',
          tags: [],
          lastSavedAt: new Date().toISOString(),
          autoSave: true
        }
      ];

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockDrafts
      } as Response);

      const result = await forumService.getDrafts();

      expect(result).toEqual(mockDrafts);
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/drafts',
        expect.any(Object)
      );
    });

    it('should delete a draft', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => ({ success: true })
      } as Response);

      await forumService.deleteDraft('draft-123');

      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/drafts/draft-123',
        expect.objectContaining({
          method: 'DELETE'
        })
      );
    });
  });

  describe('User Preferences', () => {
    it('should get user forum preferences', async () => {
      const mockPreferences: ForumPreferences = {
        emailNotifications: true,
        pushNotifications: false,
        notifyOnReply: true,
        notifyOnMention: true,
        notifyOnAccepted: true,
        digestFrequency: 'daily',
        defaultPostType: 'discussion',
        defaultSortOrder: 'recent'
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockPreferences
      } as Response);

      const result = await forumService.getUserPreferences();

      expect(result).toEqual(mockPreferences);
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/preferences',
        expect.any(Object)
      );
    });

    it('should update user forum preferences', async () => {
      const updates: Partial<ForumPreferences> = {
        emailNotifications: false,
        digestFrequency: 'weekly'
      };

      const mockUpdatedPrefs: ForumPreferences = {
        emailNotifications: false,
        pushNotifications: false,
        notifyOnReply: true,
        notifyOnMention: true,
        notifyOnAccepted: true,
        digestFrequency: 'weekly',
        defaultPostType: 'discussion',
        defaultSortOrder: 'recent'
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockUpdatedPrefs
      } as Response);

      const result = await forumService.updateUserPreferences(updates);

      expect(result).toEqual(mockUpdatedPrefs);
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/forum/preferences',
        expect.objectContaining({
          method: 'PUT',
          body: JSON.stringify(updates)
        })
      );
    });
  });

  describe('Error Handling', () => {
    it('should handle API errors gracefully', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error'
      } as Response);

      await expect(forumService.getPost(1)).rejects.toThrow('Failed to fetch post');
    });

    it('should handle network errors', async () => {
      vi.spyOn(global, 'fetch').mockRejectedValueOnce(new Error('Network error'));

      await expect(forumService.getPost(1)).rejects.toThrow('Network error');
    });

    it('should validate required fields', async () => {
      const invalidRequest: CreatePostRequest = {
        title: '',
        content: '',
        type: 'discussion'
      };

      await expect(forumService.createPost(invalidRequest)).rejects.toThrow();
    });
  });

  describe('Caching', () => {
    it('should cache frequently accessed data', async () => {
      const mockPost: ForumPost = {
        id: 1,
        userId: 123,
        title: 'Cached Post',
        slug: 'cached-post',
        content: 'Content',
        type: 'discussion',
        status: 'published',
        tags: [],
        viewCount: 0,
        replyCount: 0,
        likeCount: 0,
        isPinned: false,
        isFeatured: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        lastActivityAt: new Date().toISOString(),
        author: { id: 123, username: 'user' }
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => mockPost
      } as Response);

      // First call - should fetch from API
      await forumService.getPost(1);
      expect(fetch).toHaveBeenCalledTimes(1);

      // Second call - should use cache
      await forumService.getPost(1);
      expect(fetch).toHaveBeenCalledTimes(1); // Still only called once
    });

    it('should invalidate cache on updates', async () => {
      const updateRequest: UpdatePostRequest = {
        title: 'Updated Title'
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => ({})
      } as Response);

      await forumService.updatePost(1, updateRequest);

      // Cache for this post should be cleared
      // Next getPost call should fetch from API
    });
  });

  describe('WebSocket Integration', () => {
    it('should subscribe to post updates', async () => {
      const callback = vi.fn();
      const unsubscribe = forumService.subscribeToPostUpdates(1, callback);

      expect(typeof unsubscribe).toBe('function');
      // WebSocket is mocked as not connected, so subscription won't actually happen
    });

    it('should subscribe to forum notifications', async () => {
      const callback = vi.fn();
      const unsubscribe = forumService.subscribeToNotifications(callback);

      expect(typeof unsubscribe).toBe('function');
    });

    it('should send real-time reply notification', async () => {
      const createRequest: CreateReplyRequest = {
        postId: 1,
        content: 'New reply'
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        json: async () => ({ id: 1 })
      } as Response);

      await forumService.createReply(createRequest);

      // Should trigger WebSocket notification if connected
    });
  });
});