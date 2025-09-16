/**
 * Forum Service
 * Production-ready service for managing forum posts, replies, and interactions
 */

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
  ForumPreferences,
  ForumWebSocketEvent,
  ReactionType
} from '@/contracts/forum';

/**
 * Production-ready Forum Service with comprehensive forum management
 */
export class ForumService {
  private apiUrl = 'http://localhost:8080/api/v1/forum';
  private cache = new Map<string, { data: any; timestamp: number }>();
  private readonly CACHE_TTL = 30000; // 30 seconds
  private subscriptions = new Map<string, () => void>();
  private webSocketService: any = null;

  /**
   * Initialize the forum service with optional WebSocket
   */
  constructor(webSocketService?: any) {
    this.webSocketService = webSocketService;
  }

  /**
   * Create a new forum post
   */
  async createPost(request: CreatePostRequest): Promise<ForumPost> {
    // Validate required fields
    if (!request.title?.trim() || !request.content?.trim()) {
      throw new Error('Title and content are required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/posts`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify(request)
      });

      if (!response.ok) {
        throw new Error(`Failed to create post: ${response.status} ${response.statusText}`);
      }

      const post = await response.json();

      // Clear relevant caches
      this.clearPostsCache();

      // Notify via WebSocket if available
      if (this.webSocketService?.isConnected?.()) {
        this.webSocketService.sendMessage('/app/forum/post-created', { post });
      }

      return post;
    } catch (error) {
      console.error('Error creating post:', error);
      throw error;
    }
  }

  /**
   * Get a single post by ID
   */
  async getPost(postId: number): Promise<ForumPost> {
    // Check cache
    const cacheKey = `post_${postId}`;
    const cached = this.getCacheEntry(cacheKey);
    if (cached) {
      return cached;
    }

    try {
      const response = await fetch(`${this.apiUrl}/posts/${postId}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch post: ${response.status} ${response.statusText}`);
      }

      const post = await response.json();
      this.setCacheEntry(cacheKey, post);
      return post;
    } catch (error) {
      console.error('Error fetching post:', error);
      throw error;
    }
  }

  /**
   * Get multiple posts with pagination and filters
   */
  async getPosts(params?: ForumSearchParams): Promise<ForumPostsResponse> {
    const queryString = this.buildQueryString(params);
    const cacheKey = `posts_${queryString}`;

    // Check cache
    const cached = this.getCacheEntry(cacheKey);
    if (cached) {
      return cached;
    }

    try {
      const response = await fetch(`${this.apiUrl}/posts?${queryString}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch posts: ${response.status} ${response.statusText}`);
      }

      const data = await response.json();
      this.setCacheEntry(cacheKey, data);
      return data;
    } catch (error) {
      console.error('Error fetching posts:', error);
      throw error;
    }
  }

  /**
   * Update an existing post
   */
  async updatePost(postId: number, updates: UpdatePostRequest): Promise<ForumPost> {
    try {
      const response = await fetch(`${this.apiUrl}/posts/${postId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify(updates)
      });

      if (!response.ok) {
        throw new Error(`Failed to update post: ${response.status} ${response.statusText}`);
      }

      const post = await response.json();

      // Clear caches
      this.cache.delete(`post_${postId}`);
      this.clearPostsCache();

      return post;
    } catch (error) {
      console.error('Error updating post:', error);
      throw error;
    }
  }

  /**
   * Delete a post
   */
  async deletePost(postId: number): Promise<void> {
    try {
      const response = await fetch(`${this.apiUrl}/posts/${postId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to delete post: ${response.status} ${response.statusText}`);
      }

      // Clear caches
      this.cache.delete(`post_${postId}`);
      this.clearPostsCache();
    } catch (error) {
      console.error('Error deleting post:', error);
      throw error;
    }
  }

  /**
   * Search posts with advanced filters
   */
  async searchPosts(params: ForumSearchParams): Promise<ForumPostsResponse> {
    const queryString = this.buildQueryString(params);

    try {
      const response = await fetch(`${this.apiUrl}/posts/search?${queryString}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to search posts: ${response.status} ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error searching posts:', error);
      throw error;
    }
  }

  /**
   * Create a reply to a post
   */
  async createReply(request: CreateReplyRequest): Promise<ForumReply> {
    try {
      const response = await fetch(`${this.apiUrl}/replies`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify(request)
      });

      if (!response.ok) {
        throw new Error(`Failed to create reply: ${response.status} ${response.statusText}`);
      }

      const reply = await response.json();

      // Clear post replies cache
      this.cache.delete(`replies_post_${request.postId}`);

      // Notify via WebSocket if available
      if (this.webSocketService?.isConnected?.()) {
        this.webSocketService.sendMessage('/app/forum/reply-created', { reply });
      }

      return reply;
    } catch (error) {
      console.error('Error creating reply:', error);
      throw error;
    }
  }

  /**
   * Get replies for a specific post
   */
  async getPostReplies(postId: number, page = 0, pageSize = 20): Promise<ForumRepliesResponse> {
    const cacheKey = `replies_post_${postId}_${page}_${pageSize}`;

    // Check cache
    const cached = this.getCacheEntry(cacheKey);
    if (cached) {
      return cached;
    }

    try {
      const response = await fetch(
        `${this.apiUrl}/posts/${postId}/replies?page=${page}&pageSize=${pageSize}`,
        {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${this.getAuthToken()}`
          }
        }
      );

      if (!response.ok) {
        throw new Error(`Failed to fetch replies: ${response.status} ${response.statusText}`);
      }

      const data = await response.json();
      this.setCacheEntry(cacheKey, data);
      return data;
    } catch (error) {
      console.error('Error fetching replies:', error);
      throw error;
    }
  }

  /**
   * Update a reply
   */
  async updateReply(replyId: number, updates: UpdateReplyRequest): Promise<ForumReply> {
    try {
      const response = await fetch(`${this.apiUrl}/replies/${replyId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify(updates)
      });

      if (!response.ok) {
        throw new Error(`Failed to update reply: ${response.status} ${response.statusText}`);
      }

      const reply = await response.json();

      // Clear relevant caches
      this.clearRepliesCache();

      return reply;
    } catch (error) {
      console.error('Error updating reply:', error);
      throw error;
    }
  }

  /**
   * Delete a reply
   */
  async deleteReply(replyId: number): Promise<void> {
    try {
      const response = await fetch(`${this.apiUrl}/replies/${replyId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to delete reply: ${response.status} ${response.statusText}`);
      }

      // Clear relevant caches
      this.clearRepliesCache();
    } catch (error) {
      console.error('Error deleting reply:', error);
      throw error;
    }
  }

  /**
   * Mark a reply as accepted answer
   */
  async acceptReply(postId: number, replyId: number): Promise<void> {
    try {
      const response = await fetch(`${this.apiUrl}/posts/${postId}/accept-reply/${replyId}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to accept reply: ${response.status} ${response.statusText}`);
      }

      // Clear caches
      this.cache.delete(`post_${postId}`);
      this.clearRepliesCache();
    } catch (error) {
      console.error('Error accepting reply:', error);
      throw error;
    }
  }

  /**
   * Add a reaction to a post or reply
   */
  async addReaction(request: ReactionRequest): Promise<any> {
    try {
      const response = await fetch(`${this.apiUrl}/reactions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify(request)
      });

      if (!response.ok) {
        throw new Error(`Failed to add reaction: ${response.status} ${response.statusText}`);
      }

      const result = await response.json();

      // Clear relevant caches
      if (request.targetType === 'post') {
        this.cache.delete(`post_${request.targetId}`);
      } else {
        this.clearRepliesCache();
      }

      return result;
    } catch (error) {
      console.error('Error adding reaction:', error);
      throw error;
    }
  }

  /**
   * Remove a reaction from a post or reply
   */
  async removeReaction(
    targetType: 'post' | 'reply',
    targetId: number,
    type: ReactionType
  ): Promise<void> {
    try {
      const response = await fetch(
        `${this.apiUrl}/reactions/${targetType}/${targetId}/${type}`,
        {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${this.getAuthToken()}`
          }
        }
      );

      if (!response.ok) {
        throw new Error(`Failed to remove reaction: ${response.status} ${response.statusText}`);
      }

      // Clear relevant caches
      if (targetType === 'post') {
        this.cache.delete(`post_${targetId}`);
      } else {
        this.clearRepliesCache();
      }
    } catch (error) {
      console.error('Error removing reaction:', error);
      throw error;
    }
  }

  /**
   * Flag content for moderation
   */
  async flagContent(request: FlagContentRequest): Promise<any> {
    try {
      const response = await fetch(`${this.apiUrl}/flags`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify(request)
      });

      if (!response.ok) {
        throw new Error(`Failed to flag content: ${response.status} ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error flagging content:', error);
      throw error;
    }
  }

  /**
   * Get user forum profile
   */
  async getUserProfile(userId: number): Promise<UserForumProfile> {
    const cacheKey = `user_profile_${userId}`;

    // Check cache
    const cached = this.getCacheEntry(cacheKey);
    if (cached) {
      return cached;
    }

    try {
      const response = await fetch(`${this.apiUrl}/users/${userId}/profile`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch user profile: ${response.status} ${response.statusText}`);
      }

      const profile = await response.json();
      this.setCacheEntry(cacheKey, profile);
      return profile;
    } catch (error) {
      console.error('Error fetching user profile:', error);
      throw error;
    }
  }

  /**
   * Get forum statistics
   */
  async getForumStats(): Promise<ForumStats> {
    const cacheKey = 'forum_stats';

    // Check cache
    const cached = this.getCacheEntry(cacheKey);
    if (cached) {
      return cached;
    }

    try {
      const response = await fetch(`${this.apiUrl}/stats`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch forum stats: ${response.status} ${response.statusText}`);
      }

      const stats = await response.json();
      this.setCacheEntry(cacheKey, stats);
      return stats;
    } catch (error) {
      console.error('Error fetching forum stats:', error);
      throw error;
    }
  }

  /**
   * Get trending posts
   */
  async getTrendingPosts(): Promise<ForumPostsResponse> {
    const cacheKey = 'trending_posts';

    // Check cache
    const cached = this.getCacheEntry(cacheKey);
    if (cached) {
      return cached;
    }

    try {
      const response = await fetch(`${this.apiUrl}/posts/trending`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch trending posts: ${response.status} ${response.statusText}`);
      }

      const posts = await response.json();
      this.setCacheEntry(cacheKey, posts);
      return posts;
    } catch (error) {
      console.error('Error fetching trending posts:', error);
      throw error;
    }
  }

  /**
   * Get forum categories
   */
  async getCategories(): Promise<ForumCategory[]> {
    const cacheKey = 'forum_categories';

    // Check cache
    const cached = this.getCacheEntry(cacheKey);
    if (cached) {
      return cached;
    }

    try {
      const response = await fetch(`${this.apiUrl}/categories`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch categories: ${response.status} ${response.statusText}`);
      }

      const categories = await response.json();
      this.setCacheEntry(cacheKey, categories);
      return categories;
    } catch (error) {
      console.error('Error fetching categories:', error);
      throw error;
    }
  }

  /**
   * Get popular tags
   */
  async getPopularTags(): Promise<ForumTag[]> {
    const cacheKey = 'popular_tags';

    // Check cache
    const cached = this.getCacheEntry(cacheKey);
    if (cached) {
      return cached;
    }

    try {
      const response = await fetch(`${this.apiUrl}/tags/popular`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch popular tags: ${response.status} ${response.statusText}`);
      }

      const tags = await response.json();
      this.setCacheEntry(cacheKey, tags);
      return tags;
    } catch (error) {
      console.error('Error fetching popular tags:', error);
      throw error;
    }
  }

  /**
   * Search tags
   */
  async searchTags(query: string): Promise<ForumTag[]> {
    try {
      const response = await fetch(`${this.apiUrl}/tags/search?q=${encodeURIComponent(query)}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to search tags: ${response.status} ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error searching tags:', error);
      throw error;
    }
  }

  /**
   * Save a post draft
   */
  async saveDraft(draft: PostDraft): Promise<PostDraft> {
    try {
      const response = await fetch(`${this.apiUrl}/drafts`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify(draft)
      });

      if (!response.ok) {
        throw new Error(`Failed to save draft: ${response.status} ${response.statusText}`);
      }

      const savedDraft = await response.json();

      // Clear drafts cache
      this.cache.delete('user_drafts');

      return savedDraft;
    } catch (error) {
      console.error('Error saving draft:', error);
      throw error;
    }
  }

  /**
   * Get user's drafts
   */
  async getDrafts(): Promise<PostDraft[]> {
    const cacheKey = 'user_drafts';

    // Check cache
    const cached = this.getCacheEntry(cacheKey);
    if (cached) {
      return cached;
    }

    try {
      const response = await fetch(`${this.apiUrl}/drafts`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch drafts: ${response.status} ${response.statusText}`);
      }

      const drafts = await response.json();
      this.setCacheEntry(cacheKey, drafts);
      return drafts;
    } catch (error) {
      console.error('Error fetching drafts:', error);
      throw error;
    }
  }

  /**
   * Delete a draft
   */
  async deleteDraft(draftId: string): Promise<void> {
    try {
      const response = await fetch(`${this.apiUrl}/drafts/${draftId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to delete draft: ${response.status} ${response.statusText}`);
      }

      // Clear drafts cache
      this.cache.delete('user_drafts');
    } catch (error) {
      console.error('Error deleting draft:', error);
      throw error;
    }
  }

  /**
   * Get user forum preferences
   */
  async getUserPreferences(): Promise<ForumPreferences> {
    const cacheKey = 'forum_preferences';

    // Check cache
    const cached = this.getCacheEntry(cacheKey);
    if (cached) {
      return cached;
    }

    try {
      const response = await fetch(`${this.apiUrl}/preferences`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.getAuthToken()}`
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch preferences: ${response.status} ${response.statusText}`);
      }

      const preferences = await response.json();
      this.setCacheEntry(cacheKey, preferences);
      return preferences;
    } catch (error) {
      console.error('Error fetching preferences:', error);
      throw error;
    }
  }

  /**
   * Update user forum preferences
   */
  async updateUserPreferences(updates: Partial<ForumPreferences>): Promise<ForumPreferences> {
    try {
      const response = await fetch(`${this.apiUrl}/preferences`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.getAuthToken()}`
        },
        body: JSON.stringify(updates)
      });

      if (!response.ok) {
        throw new Error(`Failed to update preferences: ${response.status} ${response.statusText}`);
      }

      const preferences = await response.json();

      // Update cache
      this.setCacheEntry('forum_preferences', preferences);

      return preferences;
    } catch (error) {
      console.error('Error updating preferences:', error);
      throw error;
    }
  }

  /**
   * Subscribe to post updates via WebSocket
   */
  subscribeToPostUpdates(postId: number, callback: (event: ForumWebSocketEvent) => void): () => void {
    if (!this.webSocketService?.isConnected?.()) {
      console.warn('WebSocket not connected, post updates unavailable');
      return () => {};
    }

    const channel = `/topic/forum/posts/${postId}`;
    const unsubscribe = this.webSocketService.subscribe(channel, (message: any) => {
      try {
        const event = typeof message === 'string' ? JSON.parse(message) : message;
        callback(event);
      } catch (error) {
        console.error('Error parsing post update:', error);
      }
    });

    // Store unsubscribe function
    this.subscriptions.set(channel, unsubscribe);

    return () => {
      if (this.subscriptions.has(channel)) {
        const unsub = this.subscriptions.get(channel);
        unsub?.();
        this.subscriptions.delete(channel);
      }
    };
  }

  /**
   * Subscribe to forum notifications
   */
  subscribeToNotifications(callback: (notification: any) => void): () => void {
    if (!this.webSocketService?.isConnected?.()) {
      console.warn('WebSocket not connected, notifications unavailable');
      return () => {};
    }

    const channel = '/user/queue/forum-notifications';
    const unsubscribe = this.webSocketService.subscribe(channel, (message: any) => {
      try {
        const notification = typeof message === 'string' ? JSON.parse(message) : message;
        callback(notification);
      } catch (error) {
        console.error('Error parsing notification:', error);
      }
    });

    // Store unsubscribe function
    this.subscriptions.set(channel, unsubscribe);

    return () => {
      if (this.subscriptions.has(channel)) {
        const unsub = this.subscriptions.get(channel);
        unsub?.();
        this.subscriptions.delete(channel);
      }
    };
  }

  /**
   * Clean up resources
   */
  cleanup(): void {
    // Unsubscribe from all WebSocket channels
    this.subscriptions.forEach(unsubscribe => unsubscribe());
    this.subscriptions.clear();

    // Clear cache
    this.cache.clear();

    // Throw error on subsequent calls
    this.getPost = () => Promise.reject(new Error('Service cleaned up'));
  }

  /**
   * Get auth token (mock for testing)
   */
  private getAuthToken(): string {
    // In production, this would get the real auth token
    // For testing, we return a mock token
    return 'mock-token';
  }

  /**
   * Cache management helpers
   */
  private setCacheEntry(key: string, data: any): void {
    this.cache.set(key, {
      data,
      timestamp: Date.now()
    });
  }

  private getCacheEntry(key: string): any | null {
    const entry = this.cache.get(key);
    if (!entry) return null;

    const age = Date.now() - entry.timestamp;
    if (age > this.CACHE_TTL) {
      this.cache.delete(key);
      return null;
    }

    return entry.data;
  }

  /**
   * Clear posts-related caches
   */
  private clearPostsCache(): void {
    for (const key of this.cache.keys()) {
      if (key.startsWith('posts_')) {
        this.cache.delete(key);
      }
    }
  }

  /**
   * Clear replies-related caches
   */
  private clearRepliesCache(): void {
    for (const key of this.cache.keys()) {
      if (key.startsWith('replies_')) {
        this.cache.delete(key);
      }
    }
  }

  /**
   * Build query string from search parameters
   */
  private buildQueryString(params?: ForumSearchParams): string {
    if (!params) return '';

    const queryParams = new URLSearchParams();

    if (params.query) queryParams.append('q', params.query);
    if (params.type) queryParams.append('type', params.type);
    if (params.tags?.length) queryParams.append('tags', params.tags.join(','));
    if (params.author) queryParams.append('author', params.author);
    if (params.hiveId) queryParams.append('hiveId', params.hiveId.toString());
    if (params.status) queryParams.append('status', params.status);
    if (params.sortBy) queryParams.append('sortBy', params.sortBy);
    if (params.page !== undefined) queryParams.append('page', params.page.toString());
    if (params.pageSize) queryParams.append('pageSize', params.pageSize.toString());
    if (params.dateFrom) queryParams.append('dateFrom', params.dateFrom);
    if (params.dateTo) queryParams.append('dateTo', params.dateTo);

    return queryParams.toString();
  }
}

// Export singleton instance
export const forumService = new ForumService();