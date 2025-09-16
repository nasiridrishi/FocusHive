import axios, {AxiosInstance} from 'axios'
import {
  ForumAttachment,
  ForumCategory,
  ForumCreatePostRequest,
  ForumCreateReplyRequest,
  ForumNotification,
  ForumPost,
  ForumReply,
  ForumSearchResult,
  ForumStats,
  ForumUser
} from '../types'

class ForumApiService {
  private api: AxiosInstance

  constructor() {
    const apiBaseUrl = import.meta.env.VITE_apiBaseUrl || 'http://localhost:8080';
    this.api = axios.create({
      baseURL: `${apiBaseUrl}/api/forum`,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    })

    // Request interceptor to add auth token
    this.api.interceptors.request.use(
        (config) => {
          const token = localStorage.getItem('authToken')
          if (token) {
            config.headers.Authorization = `Bearer ${token}`
          }
          return config
        },
        (error) => Promise.reject(error)
    )

    // Response interceptor for error handling
    this.api.interceptors.response.use(
        (response) => response,
        (error) => {
          // API errors are handled at component level
          throw error
        }
    )
  }

  // Categories
  async getCategories(): Promise<ForumCategory[]> {
    const response = await this.api.get('/categories')
    return response.data
  }

  async getCategory(categoryId: number): Promise<ForumCategory> {
    const response = await this.api.get(`/categories/${categoryId}`)
    return response.data
  }

  async getCategoryBySlug(slug: string): Promise<ForumCategory> {
    const response = await this.api.get(`/categories/slug/${slug}`)
    return response.data
  }

  // Posts
  async getPosts(
      categoryId?: number,
      page = 1,
      limit = 20,
      sortBy: 'recent' | 'popular' | 'oldest' = 'recent'
  ): Promise<{
    posts: ForumPost[]
    totalPages: number
    totalPosts: number
    currentPage: number
  }> {
    const params = new URLSearchParams({
      page: page.toString(),
      limit: limit.toString(),
      sortBy
    })

    if (categoryId) {
      params.append('categoryId', categoryId.toString())
    }

    const response = await this.api.get(`/posts?${params}`)
    return response.data
  }

  async getPost(postId: number): Promise<ForumPost> {
    const response = await this.api.get(`/posts/${postId}`)
    return response.data
  }

  async getPostBySlug(slug: string): Promise<ForumPost> {
    const response = await this.api.get(`/posts/slug/${slug}`)
    return response.data
  }

  async createPost(postData: ForumCreatePostRequest): Promise<ForumPost> {
    const formData = new FormData()
    formData.append('title', postData.title)
    formData.append('content', postData.content)
    formData.append('categoryId', postData.categoryId.toString())

    if (postData.tags && postData.tags.length > 0) {
      formData.append('tags', JSON.stringify(postData.tags))
    }

    if (postData.isPinned) {
      formData.append('isPinned', 'true')
    }

    if (postData.isLocked) {
      formData.append('isLocked', 'true')
    }

    if (postData.attachments && postData.attachments.length > 0) {
      postData.attachments.forEach((file) => {
        formData.append(`attachments`, file)
      })
    }

    const response = await this.api.post('/posts', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    return response.data
  }

  async updatePost(postId: number, postData: Partial<ForumCreatePostRequest>): Promise<ForumPost> {
    const response = await this.api.put(`/posts/${postId}`, postData)
    return response.data
  }

  async deletePost(postId: number): Promise<void> {
    await this.api.delete(`/posts/${postId}`)
  }

  async likePost(postId: number): Promise<ForumPost> {
    const response = await this.api.post(`/posts/${postId}/like`)
    return response.data
  }

  async unlikePost(postId: number): Promise<ForumPost> {
    const response = await this.api.delete(`/posts/${postId}/like`)
    return response.data
  }

  async pinPost(postId: number): Promise<ForumPost> {
    const response = await this.api.put(`/posts/${postId}/pin`)
    return response.data
  }

  async unpinPost(postId: number): Promise<ForumPost> {
    const response = await this.api.put(`/posts/${postId}/unpin`)
    return response.data
  }

  async lockPost(postId: number): Promise<ForumPost> {
    const response = await this.api.put(`/posts/${postId}/lock`)
    return response.data
  }

  async unlockPost(postId: number): Promise<ForumPost> {
    const response = await this.api.put(`/posts/${postId}/unlock`)
    return response.data
  }

  // Replies
  async getReplies(
      postId: number,
      page = 1,
      limit = 20,
      sortBy: 'oldest' | 'newest' | 'most_liked' = 'oldest'
  ): Promise<{
    replies: ForumReply[]
    totalPages: number
    totalReplies: number
    currentPage: number
  }> {
    const params = new URLSearchParams({
      page: page.toString(),
      limit: limit.toString(),
      sortBy
    })

    const response = await this.api.get(`/posts/${postId}/replies?${params}`)
    return response.data
  }

  async getReply(replyId: number): Promise<ForumReply> {
    const response = await this.api.get(`/replies/${replyId}`)
    return response.data
  }

  async createReply(replyData: ForumCreateReplyRequest): Promise<ForumReply> {
    const formData = new FormData()
    formData.append('content', replyData.content)
    formData.append('postId', replyData.postId.toString())

    if (replyData.parentReplyId) {
      formData.append('parentReplyId', replyData.parentReplyId.toString())
    }

    if (replyData.attachments && replyData.attachments.length > 0) {
      replyData.attachments.forEach((file) => {
        formData.append(`attachments`, file)
      })
    }

    const response = await this.api.post('/replies', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    return response.data
  }

  async updateReply(replyId: number, content: string): Promise<ForumReply> {
    const response = await this.api.put(`/replies/${replyId}`, {content})
    return response.data
  }

  async deleteReply(replyId: number): Promise<void> {
    await this.api.delete(`/replies/${replyId}`)
  }

  async likeReply(replyId: number): Promise<ForumReply> {
    const response = await this.api.post(`/replies/${replyId}/like`)
    return response.data
  }

  async unlikeReply(replyId: number): Promise<ForumReply> {
    const response = await this.api.delete(`/replies/${replyId}/like`)
    return response.data
  }

  // Search
  async search(
      query: string,
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
  ): Promise<ForumSearchResult> {
    const params = new URLSearchParams({query})

    if (filters) {
      if (filters.categoryId) {
        params.append('categoryId', filters.categoryId.toString())
      }
      if (filters.authorId) {
        params.append('authorId', filters.authorId.toString())
      }
      if (filters.dateRange) {
        params.append('startDate', filters.dateRange.start)
        params.append('endDate', filters.dateRange.end)
      }
      if (filters.sortBy) {
        params.append('sortBy', filters.sortBy)
      }
      if (filters.tags && filters.tags.length > 0) {
        params.append('tags', filters.tags.join(','))
      }
    }

    const response = await this.api.get(`/search?${params}`)
    return response.data
  }

  // Users
  async getForumUsers(page = 1, limit = 20): Promise<{
    users: ForumUser[]
    totalPages: number
    totalUsers: number
    currentPage: number
  }> {
    const params = new URLSearchParams({
      page: page.toString(),
      limit: limit.toString()
    })

    const response = await this.api.get(`/users?${params}`)
    return response.data
  }

  async getForumUser(userId: number): Promise<ForumUser> {
    const response = await this.api.get(`/users/${userId}`)
    return response.data
  }

  async getUserPosts(userId: number, page = 1, limit = 20): Promise<{
    posts: ForumPost[]
    totalPages: number
    totalPosts: number
    currentPage: number
  }> {
    const params = new URLSearchParams({
      page: page.toString(),
      limit: limit.toString()
    })

    const response = await this.api.get(`/users/${userId}/posts?${params}`)
    return response.data
  }

  async getUserReplies(userId: number, page = 1, limit = 20): Promise<{
    replies: ForumReply[]
    totalPages: number
    totalReplies: number
    currentPage: number
  }> {
    const params = new URLSearchParams({
      page: page.toString(),
      limit: limit.toString()
    })

    const response = await this.api.get(`/users/${userId}/replies?${params}`)
    return response.data
  }

  // Statistics
  async getForumStats(): Promise<ForumStats> {
    const response = await this.api.get('/stats')
    return response.data
  }

  async getCategoryStats(categoryId: number): Promise<{
    totalPosts: number
    totalReplies: number
    totalViews: number
    activeUsers: number
    topContributors: ForumUser[]
  }> {
    const response = await this.api.get(`/categories/${categoryId}/stats`)
    return response.data
  }

  // Notifications
  async getNotifications(page = 1, limit = 20): Promise<{
    notifications: ForumNotification[]
    totalPages: number
    totalNotifications: number
    unreadCount: number
    currentPage: number
  }> {
    const params = new URLSearchParams({
      page: page.toString(),
      limit: limit.toString()
    })

    const response = await this.api.get(`/notifications?${params}`)
    return response.data
  }

  async markNotificationAsRead(notificationId: number): Promise<void> {
    await this.api.put(`/notifications/${notificationId}/read`)
  }

  async markAllNotificationsAsRead(): Promise<void> {
    await this.api.put('/notifications/read-all')
  }

  async deleteNotification(notificationId: number): Promise<void> {
    await this.api.delete(`/notifications/${notificationId}`)
  }

  // Moderation (for moderators/admins)
  async hidePost(postId: number, reason: string): Promise<void> {
    await this.api.put(`/moderation/posts/${postId}/hide`, {reason})
  }

  async unhidePost(postId: number): Promise<void> {
    await this.api.put(`/moderation/posts/${postId}/unhide`)
  }

  async hideReply(replyId: number, reason: string): Promise<void> {
    await this.api.put(`/moderation/replies/${replyId}/hide`, {reason})
  }

  async unhideReply(replyId: number): Promise<void> {
    await this.api.put(`/moderation/replies/${replyId}/unhide`)
  }

  async movePost(postId: number, categoryId: number): Promise<ForumPost> {
    const response = await this.api.put(`/moderation/posts/${postId}/move`, {categoryId})
    return response.data
  }

  // Attachments
  async uploadAttachment(file: File, postId?: number, replyId?: number): Promise<ForumAttachment> {
    const formData = new FormData()
    formData.append('file', file)

    if (postId) {
      formData.append('postId', postId.toString())
    }

    if (replyId) {
      formData.append('replyId', replyId.toString())
    }

    const response = await this.api.post('/attachments', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    return response.data
  }

  async deleteAttachment(attachmentId: number): Promise<void> {
    await this.api.delete(`/attachments/${attachmentId}`)
  }

  // Trending and Popular
  async getTrendingPosts(limit = 10): Promise<ForumPost[]> {
    const response = await this.api.get(`/posts/trending?limit=${limit}`)
    return response.data
  }

  async getPopularPosts(timeframe: 'today' | 'week' | 'month' | 'all' = 'week', limit = 10): Promise<ForumPost[]> {
    const response = await this.api.get(`/posts/popular?timeframe=${timeframe}&limit=${limit}`)
    return response.data
  }

  async getRecentActivity(limit = 20): Promise<{
    posts: ForumPost[]
    replies: ForumReply[]
    combinedActivity: Array<ForumPost | ForumReply>
  }> {
    const response = await this.api.get(`/activity/recent?limit=${limit}`)
    return response.data
  }
}

export const forumApi = new ForumApiService()