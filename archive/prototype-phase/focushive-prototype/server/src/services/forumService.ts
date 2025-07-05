import { v4 as uuidv4 } from 'uuid';
import { forumStore } from '../data/forumStore';
import { dataStore } from '../data/store';
import type {
  ForumPost,
  GlobalChatMessage,
  BuddyConnection,
  ForumFilters,
  User
} from '@focushive/shared';

export class ForumService {
  constructor() {
    // Load sample data on initialization
    if (forumStore.getActivePosts().length === 0) {
      forumStore.loadSampleData();
    }
  }

  // Post operations
  async createPost(userId: string, postData: Partial<ForumPost>): Promise<ForumPost> {
    const user = dataStore.getUser(userId);
    if (!user) throw new Error('User not found');

    const post: ForumPost = {
      id: uuidv4(),
      userId,
      username: user.username,
      userAvatar: user.avatar || '',
      status: 'active',
      responses: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      ...postData
    } as ForumPost;

    return forumStore.createPost(post);
  }

  async getPosts(filters?: ForumFilters): Promise<ForumPost[]> {
    let posts = forumStore.getActivePosts();

    // Apply filters
    if (filters) {
      // Filter by type
      if (filters.filterBy && filters.filterBy !== 'both') {
        posts = posts.filter(post => post.type === filters.filterBy);
      }

      // Filter by tags
      if (filters.tags && filters.tags.length > 0) {
        posts = posts.filter(post => 
          filters.tags!.some(tag => post.tags.includes(tag))
        );
      }

      // Filter by timezone (simplified - in real app would calculate time overlap)
      if (filters.timezone) {
        posts = posts.filter(post => post.schedule.timezone === filters.timezone);
      }

      // Sort
      switch (filters.sortBy) {
        case 'responses':
          posts.sort((a, b) => b.responses.length - a.responses.length);
          break;
        case 'starting-soon':
          // For demo, just randomize
          posts.sort(() => Math.random() - 0.5);
          break;
        case 'recent':
        default:
          posts.sort((a, b) => 
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
          );
      }
    }

    return posts;
  }

  async getPost(postId: string): Promise<ForumPost | null> {
    return forumStore.getPost(postId) || null;
  }

  async respondToPost(postId: string, userId: string): Promise<ForumPost> {
    const post = forumStore.getPost(postId);
    if (!post) throw new Error('Post not found');
    
    if (post.userId === userId) {
      throw new Error('Cannot respond to your own post');
    }

    const updated = forumStore.addResponse(postId, userId);
    if (!updated) throw new Error('Failed to add response');

    return updated;
  }

  async updatePostStatus(postId: string, userId: string, status: ForumPost['status']): Promise<ForumPost> {
    const post = forumStore.getPost(postId);
    if (!post) throw new Error('Post not found');
    
    if (post.userId !== userId) {
      throw new Error('Only post owner can update status');
    }

    const updated = forumStore.updatePost(postId, { status });
    if (!updated) throw new Error('Failed to update post');

    return updated;
  }

  // Chat operations
  async sendChatMessage(userId: string, message: string): Promise<GlobalChatMessage> {
    const user = dataStore.getUser(userId);
    if (!user) throw new Error('User not found');

    // Basic validation
    if (!message.trim()) throw new Error('Message cannot be empty');
    if (message.length > 200) throw new Error('Message too long (max 200 chars)');

    // Rate limiting would go here in production

    const chatMessage: GlobalChatMessage = {
      id: uuidv4(),
      userId,
      username: user.username,
      userAvatar: user.avatar || '',
      message: message.trim(),
      timestamp: new Date().toISOString(),
      isDeleted: false,
      reportedBy: []
    };

    return forumStore.addChatMessage(chatMessage);
  }

  async getChatMessages(limit: number = 50): Promise<{
    messages: GlobalChatMessage[];
    onlineCount: number;
  }> {
    return {
      messages: forumStore.getChatMessages(limit),
      onlineCount: forumStore.getOnlineUsersCount()
    };
  }

  async reportChatMessage(messageId: string, reporterId: string): Promise<void> {
    forumStore.reportChatMessage(messageId, reporterId);
  }

  // Connection operations
  async createBuddyConnection(postId: string, requesterId: string, recipientId: string): Promise<BuddyConnection> {
    const post = forumStore.getPost(postId);
    if (!post) throw new Error('Post not found');

    if (post.userId !== recipientId) {
      throw new Error('Invalid recipient');
    }

    // Calculate compatibility score (simplified)
    const requester = dataStore.getUser(requesterId);
    const recipient = dataStore.getUser(recipientId);
    
    if (!requester || !recipient) throw new Error('User not found');

    const compatibilityScore = this.calculateCompatibility(requester, recipient, post);

    const connection: BuddyConnection = {
      id: uuidv4(),
      requesterId,
      requestedUserId: recipientId,
      postId,
      status: 'pending',
      compatibilityScore,
      sharedTags: post.tags,
      schedule: post.schedule,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      stats: {
        sessionsCompleted: 0,
        totalFocusTime: 0,
        streak: 0
      }
    };

    return forumStore.createConnection(connection);
  }

  private calculateCompatibility(user1: User, user2: User, post: ForumPost): number {
    // Simple compatibility score - in real app would be more sophisticated
    return Math.floor(Math.random() * 30) + 70; // 70-100 score
  }

  async getUserConnections(userId: string): Promise<BuddyConnection[]> {
    const connections = forumStore.getUserConnections(userId);
    
    // Enrich with user data
    return connections.map(conn => {
      const requesterUser = dataStore.getUser(conn.requesterId);
      const requestedUser = dataStore.getUser(conn.requestedUserId);
      
      return {
        ...conn,
        requesterUser: requesterUser ? {
          id: requesterUser.id,
          username: requesterUser.username,
          avatar: requesterUser.avatar
        } : undefined,
        requestedUser: requestedUser ? {
          id: requestedUser.id,
          username: requestedUser.username,
          avatar: requestedUser.avatar
        } : undefined
      };
    });
  }

  async updateConnection(connectionId: string, userId: string, status: BuddyConnection['status']): Promise<BuddyConnection> {
    const connection = forumStore.getConnection(connectionId);
    if (!connection) throw new Error('Connection not found');

    if (connection.requestedUserId !== userId) {
      throw new Error('Only recipient can update connection status');
    }

    const updated = forumStore.updateConnection(connectionId, { status });
    if (!updated) throw new Error('Failed to update connection');

    // If accepted, update the post status
    if (status === 'accepted') {
      await this.updatePostStatus(connection.postId, connection.requestedUserId, 'matched');
    }

    return updated;
  }

  // Stats
  async getForumStats(): Promise<{
    activeSeekers: number;
    successfulMatches: number;
    totalFocusHours: number;
  }> {
    const posts = forumStore.getActivePosts();
    const connections = forumStore.getAllConnections();
    
    return {
      activeSeekers: posts.length,
      successfulMatches: connections.filter(c => c.status === 'accepted').length,
      totalFocusHours: Math.floor(Math.random() * 1000) + 500 // Mock data
    };
  }
}