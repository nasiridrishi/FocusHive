import { ForumPost, GlobalChatMessage, BuddyConnection } from '@focushive/shared';

interface ForumStore {
  posts: Map<string, ForumPost>;
  chatMessages: GlobalChatMessage[];
  connections: Map<string, BuddyConnection>;
  userPosts: Map<string, Set<string>>; // userId -> postIds
  userConnections: Map<string, Set<string>>; // userId -> connectionIds
}

class ForumDataStore {
  private store: ForumStore = {
    posts: new Map(),
    chatMessages: [],
    connections: new Map(),
    userPosts: new Map(),
    userConnections: new Map()
  };

  private maxChatMessages = 100;
  private chatMessageLimit = 200; // character limit

  reset(): void {
    this.store.posts.clear();
    this.store.chatMessages = [];
    this.store.connections.clear();
    this.store.userPosts.clear();
    this.store.userConnections.clear();
  }

  // Post operations
  createPost(post: ForumPost): ForumPost {
    this.store.posts.set(post.id, post);
    
    // Add to user's posts
    if (!this.store.userPosts.has(post.userId)) {
      this.store.userPosts.set(post.userId, new Set());
    }
    this.store.userPosts.get(post.userId)!.add(post.id);
    
    return post;
  }

  getPost(postId: string): ForumPost | undefined {
    return this.store.posts.get(postId);
  }

  getAllPosts(): ForumPost[] {
    return Array.from(this.store.posts.values())
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  }

  getActivePosts(): ForumPost[] {
    return this.getAllPosts().filter(post => post.status === 'active');
  }

  getUserPosts(userId: string): ForumPost[] {
    const postIds = this.store.userPosts.get(userId);
    if (!postIds) return [];
    
    const posts: ForumPost[] = [];
    for (const postId of postIds) {
      const post = this.store.posts.get(postId);
      if (post) posts.push(post);
    }
    
    return posts;
  }

  updatePost(postId: string, updates: Partial<ForumPost>): ForumPost | undefined {
    const post = this.store.posts.get(postId);
    if (!post) return undefined;
    
    const updated = {
      ...post,
      ...updates,
      updatedAt: new Date().toISOString()
    };
    
    this.store.posts.set(postId, updated);
    return updated;
  }

  addResponse(postId: string, userId: string): ForumPost | undefined {
    const post = this.store.posts.get(postId);
    if (!post) return undefined;
    
    if (!post.responses.includes(userId)) {
      post.responses.push(userId);
      post.updatedAt = new Date().toISOString();
      this.store.posts.set(postId, post);
    }
    
    return post;
  }

  // Chat operations
  addChatMessage(message: GlobalChatMessage): GlobalChatMessage {
    // Enforce character limit
    if (message.message.length > this.chatMessageLimit) {
      message.message = message.message.substring(0, this.chatMessageLimit);
    }
    
    this.store.chatMessages.push(message);
    
    // Keep only last N messages
    if (this.store.chatMessages.length > this.maxChatMessages) {
      this.store.chatMessages = this.store.chatMessages.slice(-this.maxChatMessages);
    }
    
    return message;
  }

  getChatMessages(limit: number = 50): GlobalChatMessage[] {
    const messages = this.store.chatMessages.filter(msg => !msg.isDeleted);
    return messages.slice(-limit);
  }

  reportChatMessage(messageId: string, reporterId: string): void {
    const message = this.store.chatMessages.find(msg => msg.id === messageId);
    if (message && !message.reportedBy.includes(reporterId)) {
      message.reportedBy.push(reporterId);
      
      // Auto-hide if 3+ reports
      if (message.reportedBy.length >= 3) {
        message.isDeleted = true;
      }
    }
  }

  getOnlineUsersCount(): number {
    // This would be tracked by socket connections in real implementation
    // For now, return a mock number
    return Math.floor(Math.random() * 20) + 5;
  }

  // Connection operations
  createConnection(connection: BuddyConnection): BuddyConnection {
    this.store.connections.set(connection.id, connection);
    
    // Add to both users' connections
    [connection.requesterId, connection.requestedUserId].forEach(userId => {
      if (!this.store.userConnections.has(userId)) {
        this.store.userConnections.set(userId, new Set());
      }
      this.store.userConnections.get(userId)!.add(connection.id);
    });
    
    return connection;
  }

  getConnection(connectionId: string): BuddyConnection | undefined {
    return this.store.connections.get(connectionId);
  }

  getUserConnections(userId: string): BuddyConnection[] {
    const connectionIds = this.store.userConnections.get(userId);
    if (!connectionIds) return [];
    
    const connections: BuddyConnection[] = [];
    for (const connectionId of connectionIds) {
      const connection = this.store.connections.get(connectionId);
      if (connection) connections.push(connection);
    }
    
    return connections;
  }

  getAllConnections(): BuddyConnection[] {
    return Array.from(this.store.connections.values());
  }

  updateConnection(connectionId: string, updates: Partial<BuddyConnection>): BuddyConnection | undefined {
    const connection = this.store.connections.get(connectionId);
    if (!connection) return undefined;
    
    const updated = {
      ...connection,
      ...updates
    };
    
    this.store.connections.set(connectionId, updated);
    return updated;
  }

  deletePost(postId: string): void {
    const post = this.store.posts.get(postId);
    if (!post) return;
    
    // Remove from user's posts
    const userPosts = this.store.userPosts.get(post.userId);
    if (userPosts) {
      userPosts.delete(postId);
    }
    
    // Remove the post
    this.store.posts.delete(postId);
  }

  // Sample data
  loadSampleData(): void {
    const samplePosts: Partial<ForumPost>[] = [
      {
        title: "Medical student seeking MCAT study partner",
        type: 'study',
        description: "Looking for a dedicated study partner for MCAT prep. I'm focusing on biology and chemistry sections.",
        tags: ["MCAT", "Medicine", "Biology", "Chemistry"],
        schedule: {
          days: ['monday', 'wednesday', 'friday'],
          timeSlots: [{ start: '18:00', end: '20:00' }],
          timezone: 'EST'
        },
        commitmentLevel: 'weekly',
        workingStyle: {
          videoPreference: 'optional',
          communicationStyle: 'moderate',
          breakPreference: 'synchronized'
        }
      },
      {
        title: "Full-stack developer looking for morning accountability partner",
        type: 'work',
        description: "Need someone to keep me accountable for early morning coding sessions. Working on React and Node.js projects.",
        tags: ["Programming", "Web Dev", "JavaScript", "React", "Node.js"],
        schedule: {
          days: ['monday', 'tuesday', 'wednesday', 'thursday', 'friday'],
          timeSlots: [{ start: '06:00', end: '08:00' }],
          timezone: 'PST'
        },
        commitmentLevel: 'daily',
        workingStyle: {
          videoPreference: 'off',
          communicationStyle: 'minimal',
          breakPreference: 'independent'
        }
      },
      {
        title: "PhD thesis writing buddy needed - History",
        type: 'accountability',
        description: "Writing my dissertation on medieval history. Looking for someone to share the journey and keep each other motivated.",
        tags: ["PhD", "Thesis", "History", "Academic Writing"],
        schedule: {
          days: ['tuesday', 'thursday', 'saturday'],
          timeSlots: [{ start: '14:00', end: '17:00' }],
          timezone: 'CET'
        },
        commitmentLevel: 'flexible',
        workingStyle: {
          videoPreference: 'optional',
          communicationStyle: 'chatty',
          breakPreference: 'synchronized'
        }
      },
      {
        title: "CPA exam study group forming",
        type: 'group',
        description: "Forming a study group for CPA exam preparation. Planning weekend sessions to cover all sections.",
        tags: ["Accounting", "CPA", "Finance"],
        schedule: {
          days: ['saturday', 'sunday'],
          timeSlots: [{ start: '10:00', end: '14:00' }],
          timezone: 'CST'
        },
        commitmentLevel: 'weekly',
        workingStyle: {
          videoPreference: 'on',
          communicationStyle: 'moderate',
          breakPreference: 'synchronized'
        }
      }
    ];

    // Create sample posts with fake user data
    samplePosts.forEach((postData, index) => {
      const post: ForumPost = {
        id: `sample-post-${index + 1}`,
        userId: `sample-user-${index + 1}`,
        username: ['StudyBee', 'CodeNinja', 'HistoryBuff', 'NumbersCruncher'][index],
        userAvatar: '',
        status: 'active',
        responses: Array(Math.floor(Math.random() * 5)).fill('').map((_, i) => `responder-${i}`),
        createdAt: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
        updatedAt: new Date().toISOString(),
        ...postData
      } as ForumPost;
      
      this.createPost(post);
    });

    // Add sample chat messages
    const sampleMessages = [
      {
        username: "StudyBee",
        message: "Just finished a 2-hour deep work session! 💪",
        timestamp: new Date(Date.now() - 2 * 60 * 1000).toISOString()
      },
      {
        username: "CodeNinja",
        message: "Anyone up for a late-night coding session? Working on React",
        timestamp: new Date(Date.now() - 5 * 60 * 1000).toISOString()
      },
      {
        username: "LawStudent23",
        message: "@StudyBee congrats! What were you working on?",
        timestamp: new Date(Date.now() - 1 * 60 * 1000).toISOString()
      },
      {
        username: "FocusedWriter",
        message: "Starting my dissertation chapter 3, wish me luck! 📝",
        timestamp: new Date(Date.now() - 30 * 1000).toISOString()
      }
    ];

    sampleMessages.forEach((msgData, index) => {
      const message: GlobalChatMessage = {
        id: `sample-msg-${index + 1}`,
        userId: `sample-user-${index + 100}`,
        userAvatar: '',
        isDeleted: false,
        reportedBy: [],
        ...msgData
      };
      
      this.addChatMessage(message);
    });
  }
}

export const forumStore = new ForumDataStore();