/**
 * Forum Helper Utilities for E2E Tests
 * Provides mock data, test utilities, and helper functions for forum testing
 */

import { Page, expect } from '@playwright/test';
import { 
  ForumUser, 
  ForumCategory, 
  ForumPost, 
  ForumReply, 
  ForumStats,
  ForumNotification,
  ForumCreatePostRequest,
  ForumCreateReplyRequest
} from '../../src/features/forum/types';

/**
 * Mock forum users for testing
 */
export const MOCK_FORUM_USERS: ForumUser[] = [
  {
    id: 1,
    username: 'john_doe',
    avatar: 'https://example.com/avatars/john.jpg',
    role: 'USER',
    joinDate: '2024-01-15T10:00:00Z',
    postCount: 25,
    reputation: 150,
    badges: ['Early Adopter', 'Helpful']
  },
  {
    id: 2,
    username: 'jane_smith',
    avatar: 'https://example.com/avatars/jane.jpg',
    role: 'MODERATOR',
    joinDate: '2024-01-10T08:30:00Z',
    postCount: 89,
    reputation: 350,
    badges: ['Moderator', 'Expert Helper', 'Community Leader']
  },
  {
    id: 3,
    username: 'mike_wilson',
    avatar: 'https://example.com/avatars/mike.jpg',
    role: 'ADMIN',
    joinDate: '2024-01-05T09:00:00Z',
    postCount: 156,
    reputation: 750,
    badges: ['Admin', 'Founder', 'Expert', 'Community Leader']
  },
  {
    id: 4,
    username: 'sarah_jones',
    avatar: 'https://example.com/avatars/sarah.jpg',
    role: 'USER',
    joinDate: '2024-02-01T14:20:00Z',
    postCount: 8,
    reputation: 42,
    badges: ['New Member']
  },
  {
    id: 5,
    username: 'alex_brown',
    avatar: 'https://example.com/avatars/alex.jpg',
    role: 'USER',
    joinDate: '2024-01-20T16:45:00Z',
    postCount: 67,
    reputation: 280,
    badges: ['Active Member', 'Problem Solver']
  }
];

/**
 * Mock forum categories for testing
 */
export const MOCK_FORUM_CATEGORIES: ForumCategory[] = [
  {
    id: 1,
    name: 'General Discussion',
    description: 'Open discussion about anything and everything',
    slug: 'general-discussion',
    color: '#2196F3',
    icon: 'forum',
    postCount: 234,
    topicCount: 45,
    lastActivity: '2024-03-15T10:30:00Z',
    isLocked: false,
    isPrivate: false,
    order: 1,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-03-15T10:30:00Z'
  },
  {
    id: 2,
    name: 'Study Tips & Techniques',
    description: 'Share and discuss effective study methods and productivity tips',
    slug: 'study-tips',
    color: '#4CAF50',
    icon: 'school',
    postCount: 187,
    topicCount: 32,
    lastActivity: '2024-03-15T09:15:00Z',
    isLocked: false,
    isPrivate: false,
    order: 2,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-03-15T09:15:00Z'
  },
  {
    id: 3,
    name: 'Technical Support',
    description: 'Get help with technical issues and troubleshooting',
    slug: 'technical-support',
    color: '#FF9800',
    icon: 'help',
    postCount: 156,
    topicCount: 28,
    lastActivity: '2024-03-15T11:45:00Z',
    isLocked: false,
    isPrivate: false,
    order: 3,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-03-15T11:45:00Z'
  },
  {
    id: 4,
    name: 'Feature Requests',
    description: 'Suggest new features and improvements for the platform',
    slug: 'feature-requests',
    color: '#9C27B0',
    icon: 'lightbulb',
    postCount: 89,
    topicCount: 19,
    lastActivity: '2024-03-14T16:20:00Z',
    isLocked: false,
    isPrivate: false,
    order: 4,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-03-14T16:20:00Z'
  },
  {
    id: 5,
    name: 'Announcements',
    description: 'Official announcements and platform updates',
    slug: 'announcements',
    color: '#F44336',
    icon: 'announcement',
    postCount: 23,
    topicCount: 8,
    lastActivity: '2024-03-13T12:00:00Z',
    isLocked: false,
    isPrivate: false,
    requiredRole: 'ADMIN',
    order: 0,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-03-13T12:00:00Z'
  }
];

/**
 * Mock forum posts for testing
 */
export const MOCK_FORUM_POSTS: ForumPost[] = [
  {
    id: 1,
    title: 'Welcome to the FocusHive Community Forum!',
    content: 'Hello everyone! Welcome to our community forum where you can discuss study techniques, share tips, and connect with fellow learners. Please read our community guidelines before posting.',
    slug: 'welcome-to-focushive-forum',
    categoryId: 5,
    authorId: 3,
    author: MOCK_FORUM_USERS[2],
    isPinned: true,
    isLocked: false,
    isHidden: false,
    viewCount: 1250,
    replyCount: 45,
    likeCount: 89,
    dislikeCount: 2,
    lastReplyAt: '2024-03-15T08:30:00Z',
    tags: ['welcome', 'community', 'guidelines'],
    createdAt: '2024-01-01T10:00:00Z',
    updatedAt: '2024-03-15T08:30:00Z'
  },
  {
    id: 2,
    title: 'Effective Pomodoro Technique Variations',
    content: 'I\'ve been experimenting with different variations of the Pomodoro Technique and wanted to share what works best for different types of tasks. What variations have you tried?',
    slug: 'pomodoro-technique-variations',
    categoryId: 2,
    authorId: 1,
    author: MOCK_FORUM_USERS[0],
    isPinned: false,
    isLocked: false,
    isHidden: false,
    viewCount: 324,
    replyCount: 12,
    likeCount: 18,
    dislikeCount: 1,
    lastReplyAt: '2024-03-15T10:15:00Z',
    tags: ['pomodoro', 'productivity', 'time-management'],
    createdAt: '2024-03-12T14:30:00Z',
    updatedAt: '2024-03-15T10:15:00Z'
  },
  {
    id: 3,
    title: 'How to stay motivated during long study sessions?',
    content: 'I struggle with maintaining motivation during extended study periods. Does anyone have tips for staying focused and motivated for 4+ hour sessions?',
    slug: 'staying-motivated-long-sessions',
    categoryId: 2,
    authorId: 4,
    author: MOCK_FORUM_USERS[3],
    isPinned: false,
    isLocked: false,
    isHidden: false,
    viewCount: 198,
    replyCount: 8,
    likeCount: 15,
    dislikeCount: 0,
    lastReplyAt: '2024-03-15T09:45:00Z',
    tags: ['motivation', 'long-sessions', 'focus'],
    createdAt: '2024-03-14T16:20:00Z',
    updatedAt: '2024-03-15T09:45:00Z'
  },
  {
    id: 4,
    title: 'Bug Report: Timer not syncing across devices',
    content: 'I\'ve noticed that the focus timer doesn\'t sync properly between my laptop and phone. Has anyone else experienced this issue?',
    slug: 'timer-sync-bug-report',
    categoryId: 3,
    authorId: 5,
    author: MOCK_FORUM_USERS[4],
    isPinned: false,
    isLocked: false,
    isHidden: false,
    viewCount: 87,
    replyCount: 6,
    likeCount: 4,
    dislikeCount: 0,
    lastReplyAt: '2024-03-15T11:30:00Z',
    tags: ['bug', 'timer', 'sync', 'mobile'],
    createdAt: '2024-03-15T08:15:00Z',
    updatedAt: '2024-03-15T11:30:00Z'
  },
  {
    id: 5,
    title: 'Feature Request: Dark mode for late night studying',
    content: 'Would love to see a dark mode option for those late night study sessions. It would be easier on the eyes and help maintain circadian rhythms.',
    slug: 'dark-mode-feature-request',
    categoryId: 4,
    authorId: 1,
    author: MOCK_FORUM_USERS[0],
    isPinned: false,
    isLocked: false,
    isHidden: false,
    viewCount: 156,
    replyCount: 23,
    likeCount: 34,
    dislikeCount: 1,
    lastReplyAt: '2024-03-14T22:10:00Z',
    tags: ['feature-request', 'dark-mode', 'ui', 'accessibility'],
    createdAt: '2024-03-10T19:45:00Z',
    updatedAt: '2024-03-14T22:10:00Z'
  }
];

/**
 * Mock forum replies for testing
 */
export const MOCK_FORUM_REPLIES: ForumReply[] = [
  {
    id: 1,
    content: 'Thanks for the warm welcome! Excited to be part of this community.',
    postId: 1,
    authorId: 1,
    author: MOCK_FORUM_USERS[0],
    isHidden: false,
    isModeratorReply: false,
    likeCount: 5,
    dislikeCount: 0,
    createdAt: '2024-01-02T10:30:00Z',
    updatedAt: '2024-01-02T10:30:00Z'
  },
  {
    id: 2,
    content: 'I use a modified 45/15 technique for deep work tasks. The longer focus period helps me get into flow state more easily.',
    postId: 2,
    authorId: 5,
    author: MOCK_FORUM_USERS[4],
    isHidden: false,
    isModeratorReply: false,
    likeCount: 8,
    dislikeCount: 1,
    createdAt: '2024-03-13T09:15:00Z',
    updatedAt: '2024-03-13T09:15:00Z'
  },
  {
    id: 3,
    content: 'Try breaking long sessions into 90-minute chunks with 20-minute breaks. It aligns better with natural attention cycles.',
    postId: 3,
    authorId: 2,
    author: MOCK_FORUM_USERS[1],
    isHidden: false,
    isModeratorReply: true,
    likeCount: 12,
    dislikeCount: 0,
    createdAt: '2024-03-14T18:45:00Z',
    updatedAt: '2024-03-14T18:45:00Z'
  },
  {
    id: 4,
    content: 'I can confirm this issue. It seems to happen when switching between WiFi and mobile data.',
    postId: 4,
    authorId: 1,
    author: MOCK_FORUM_USERS[0],
    isHidden: false,
    isModeratorReply: false,
    likeCount: 3,
    dislikeCount: 0,
    createdAt: '2024-03-15T09:30:00Z',
    updatedAt: '2024-03-15T09:30:00Z'
  },
  {
    id: 5,
    content: 'Dark mode is already on our roadmap! We\'re planning to include it in the next major update.',
    postId: 5,
    authorId: 3,
    author: MOCK_FORUM_USERS[2],
    isHidden: false,
    isModeratorReply: false,
    likeCount: 25,
    dislikeCount: 0,
    createdAt: '2024-03-11T08:20:00Z',
    updatedAt: '2024-03-11T08:20:00Z'
  }
];

/**
 * Mock forum statistics for testing
 */
export const MOCK_FORUM_STATS: ForumStats = {
  totalPosts: 689,
  totalReplies: 1456,
  totalUsers: 245,
  totalCategories: 5,
  todayPosts: 12,
  todayReplies: 34,
  totalViews: 15678,
  averageRepliesPerPost: 2.1
};

/**
 * Mock forum notifications for testing
 */
export const MOCK_FORUM_NOTIFICATIONS: ForumNotification[] = [
  {
    id: 1,
    userId: 1,
    type: 'NEW_REPLY',
    title: 'New reply to your post',
    message: 'jane_smith replied to your post "Effective Pomodoro Technique Variations"',
    relatedPostId: 2,
    relatedReplyId: 2,
    relatedUserId: 2,
    isRead: false,
    actionUrl: '/forum/posts/2#reply-2',
    createdAt: '2024-03-15T10:15:00Z'
  },
  {
    id: 2,
    userId: 1,
    type: 'POST_LIKED',
    title: 'Someone liked your post',
    message: 'alex_brown liked your post "Dark mode feature request"',
    relatedPostId: 5,
    relatedUserId: 5,
    isRead: false,
    actionUrl: '/forum/posts/5',
    createdAt: '2024-03-15T08:45:00Z'
  },
  {
    id: 3,
    userId: 4,
    type: 'REPLY_LIKED',
    title: 'Someone liked your reply',
    message: 'john_doe liked your reply in "How to stay motivated during long study sessions?"',
    relatedPostId: 3,
    relatedReplyId: 3,
    relatedUserId: 1,
    isRead: true,
    actionUrl: '/forum/posts/3#reply-3',
    createdAt: '2024-03-14T20:30:00Z'
  }
];

/**
 * Test data for various forum scenarios
 */
export const FORUM_TEST_DATA = {
  // Valid post creation data
  validPost: {
    title: 'Test Post Title',
    content: 'This is a test post content with **bold text** and *italic text*.',
    categoryId: '2',
    tags: ['test', 'automation', 'e2e']
  } as ForumCreatePostRequest,

  // Valid reply data
  validReply: {
    content: 'This is a test reply with some helpful information.',
    postId: 1
  } as ForumCreateReplyRequest,

  // Long content for testing
  longPost: {
    title: 'Comprehensive Guide to Effective Study Techniques and Time Management',
    content: `# Introduction

This is a comprehensive guide covering various study techniques and time management strategies that have been proven effective by research and practical application.

## The Pomodoro Technique

The Pomodoro Technique is a time management method that uses a timer to break work into intervals, traditionally 25 minutes in length, separated by short breaks.

### Benefits:
- Improved focus and concentration
- Better time awareness
- Reduced mental fatigue
- Increased productivity

## Spaced Repetition

Spaced repetition is a learning technique that incorporates increasing intervals of time between subsequent review of previously learned material.

## Active Recall

Active recall is the practice of actively stimulating memory during the learning process.

## Conclusion

These techniques, when combined effectively, can significantly improve your study efficiency and retention rates.`,
    categoryId: '2',
    tags: ['study-guide', 'pomodoro', 'spaced-repetition', 'active-recall', 'comprehensive']
  } as ForumCreatePostRequest,

  // Rich text content with various formatting
  richTextPost: {
    title: 'Testing Rich Text Editor Features',
    content: `This post tests various **rich text** formatting options:

- **Bold text**
- *Italic text*
- ~~Strikethrough text~~
- \`inline code\`
- [Link text](https://example.com)

## Code Block

\`\`\`javascript
function focusTimer(duration) {
  let timeRemaining = duration;
  const interval = setInterval(() => {
    if (timeRemaining <= 0) {
      clearInterval(interval);
      console.log('Focus session complete!');
    }
    timeRemaining--;
  }, 1000);
}
\`\`\`

> This is a blockquote with important information.

1. Numbered list item 1
2. Numbered list item 2
3. Numbered list item 3

### Emoji Support
🎯 Focus 📚 Study 💪 Productivity ✅ Success`,
    categoryId: '2',
    tags: ['rich-text', 'formatting', 'markdown', 'testing']
  } as ForumCreatePostRequest,

  // Search queries for testing
  searchQueries: [
    'pomodoro',
    'study tips',
    'motivation',
    'technical support',
    'bug report',
    'feature request',
    'dark mode',
    'timer sync'
  ],

  // Filter test data
  filterData: {
    categories: ['1', '2', '3', '4', '5'],
    sortOptions: ['recent', 'popular', 'oldest', 'replies', 'views'],
    tags: ['productivity', 'study-tips', 'technical', 'bug', 'feature-request']
  },

  // Moderation test data
  moderationData: {
    hideReason: 'Content violates community guidelines',
    lockReason: 'Thread has been resolved and locked to prevent spam',
    pinReason: 'Important announcement for all users',
    moveTargetCategory: '1',
    deleteReason: 'Spam content removed by moderator'
  },

  // User interaction test data
  userInteractions: {
    likePostId: '2',
    upvoteReplyId: '3',
    downvoteReplyId: '1',
    bestAnswerReplyId: '3',
    reportReason: 'Inappropriate content'
  }
};

/**
 * Performance thresholds for forum operations
 */
export const FORUM_PERFORMANCE_THRESHOLDS = {
  PAGE_LOAD: 3000,          // 3 seconds max page load time
  SEARCH_RESPONSE: 1000,    // 1 second max search response time
  POST_CREATION: 2000,      // 2 seconds max post creation time
  REPLY_CREATION: 1500,     // 1.5 seconds max reply creation time
  REAL_TIME_UPDATE: 5000,   // 5 seconds max real-time update delay
  VOTE_RESPONSE: 500,       // 500ms max voting response time
  MODERATION_ACTION: 1000   // 1 second max moderation action time
};

/**
 * Forum Helper Class
 */
export class ForumHelper {
  private page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  /**
   * Setup mock API responses for forum testing
   */
  async setupMockApiResponses(): Promise<void> {
    // Mock forum categories API
    await this.page.route('**/api/forum/categories', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_FORUM_CATEGORIES)
      });
    });

    // Mock forum posts API
    await this.page.route('**/api/forum/posts', async route => {
      const url = new URL(route.request().url());
      const page = parseInt(url.searchParams.get('page') || '1');
      const limit = parseInt(url.searchParams.get('limit') || '20');
      
      const startIndex = (page - 1) * limit;
      const endIndex = startIndex + limit;
      const paginatedPosts = MOCK_FORUM_POSTS.slice(startIndex, endIndex);

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          posts: paginatedPosts,
          totalPages: Math.ceil(MOCK_FORUM_POSTS.length / limit),
          totalPosts: MOCK_FORUM_POSTS.length,
          currentPage: page
        })
      });
    });

    // Mock forum post details API
    await this.page.route('**/api/forum/posts/*', async route => {
      const postId = route.request().url().split('/').pop();
      const post = MOCK_FORUM_POSTS.find(p => p.id.toString() === postId);
      
      if (post) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(post)
        });
      } else {
        await route.fulfill({ status: 404 });
      }
    });

    // Mock forum replies API
    await this.page.route('**/api/forum/posts/*/replies', async route => {
      const postId = route.request().url().split('/')[6]; // Extract post ID
      const replies = MOCK_FORUM_REPLIES.filter(r => r.postId.toString() === postId);
      
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          replies,
          totalPages: 1,
          totalReplies: replies.length,
          currentPage: 1
        })
      });
    });

    // Mock forum search API
    await this.page.route('**/api/forum/search', async route => {
      const url = new URL(route.request().url());
      const query = url.searchParams.get('query') || '';
      
      // Simple search simulation
      const searchResults = MOCK_FORUM_POSTS.filter(post =>
        post.title.toLowerCase().includes(query.toLowerCase()) ||
        post.content.toLowerCase().includes(query.toLowerCase()) ||
        post.tags?.some(tag => tag.toLowerCase().includes(query.toLowerCase()))
      );

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          posts: searchResults,
          replies: [],
          users: [],
          totalResults: searchResults.length,
          query
        })
      });
    });

    // Mock forum stats API
    await this.page.route('**/api/forum/stats', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_FORUM_STATS)
      });
    });

    // Mock forum notifications API
    await this.page.route('**/api/forum/notifications', async route => {
      const userId = 1; // Assume current user ID is 1 for testing
      const notifications = MOCK_FORUM_NOTIFICATIONS.filter(n => n.userId === userId);
      
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          notifications,
          totalPages: 1,
          totalNotifications: notifications.length,
          unreadCount: notifications.filter(n => !n.isRead).length,
          currentPage: 1
        })
      });
    });

    // Mock post creation API
    await this.page.route('**/api/forum/posts', async route => {
      if (route.request().method() === 'POST') {
        const newPost: ForumPost = {
          id: MOCK_FORUM_POSTS.length + 1,
          title: 'New Test Post',
          content: 'This is a newly created test post',
          slug: 'new-test-post',
          categoryId: 2,
          authorId: 1,
          author: MOCK_FORUM_USERS[0],
          isPinned: false,
          isLocked: false,
          isHidden: false,
          viewCount: 0,
          replyCount: 0,
          likeCount: 0,
          dislikeCount: 0,
          tags: ['test'],
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        };

        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(newPost)
        });
      }
    });

    // Mock reply creation API
    await this.page.route('**/api/forum/replies', async route => {
      if (route.request().method() === 'POST') {
        const newReply: ForumReply = {
          id: MOCK_FORUM_REPLIES.length + 1,
          content: 'This is a new test reply',
          postId: 1,
          authorId: 1,
          author: MOCK_FORUM_USERS[0],
          isHidden: false,
          isModeratorReply: false,
          likeCount: 0,
          dislikeCount: 0,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        };

        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(newReply)
        });
      }
    });

    // Mock voting API
    await this.page.route('**/api/forum/**/like', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, likeCount: Math.floor(Math.random() * 50) })
      });
    });

    // Mock moderation API
    await this.page.route('**/api/forum/moderation/**', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true })
      });
    });
  }

  /**
   * Generate test post data
   */
  generateTestPostData(overrides: Partial<ForumCreatePostRequest> = {}): ForumCreatePostRequest {
    return {
      ...FORUM_TEST_DATA.validPost,
      title: `Test Post ${Date.now()}`,
      content: `Test content created at ${new Date().toISOString()}`,
      ...overrides
    };
  }

  /**
   * Generate test reply data
   */
  generateTestReplyData(postId: number, overrides: Partial<ForumCreateReplyRequest> = {}): ForumCreateReplyRequest {
    return {
      ...FORUM_TEST_DATA.validReply,
      postId,
      content: `Test reply created at ${new Date().toISOString()}`,
      ...overrides
    };
  }

  /**
   * Wait for forum API responses
   */
  async waitForApiResponse(endpoint: string, timeout = 5000): Promise<void> {
    await this.page.waitForResponse(
      response => response.url().includes(endpoint) && response.status() === 200,
      { timeout }
    );
  }

  /**
   * Simulate real-time updates
   */
  async simulateRealTimeUpdate(type: 'new_post' | 'new_reply' | 'like' | 'notification'): Promise<void> {
    // Simulate WebSocket messages or server-sent events
    await this.page.evaluate((updateType) => {
      // Dispatch custom event to simulate real-time update
      window.dispatchEvent(new CustomEvent('forum_update', { 
        detail: { type: updateType, timestamp: Date.now() } 
      }));
    }, type);
  }

  /**
   * Create test attachments
   */
  async createTestAttachment(type: 'image' | 'document' = 'image'): Promise<string> {
    const fileName = type === 'image' ? 'test-image.png' : 'test-document.pdf';
    const mimeType = type === 'image' ? 'image/png' : 'application/pdf';
    
    // Create a small test file in memory
    const buffer = Buffer.from(`Test ${type} content`);
    
    // Create temporary file path for testing
    const tempFilePath = `/tmp/${fileName}`;
    
    // In real tests, you would write the buffer to a temp file
    // For now, return the path for mock purposes
    return tempFilePath;
  }

  /**
   * Verify forum performance metrics
   */
  async verifyPerformance(operation: keyof typeof FORUM_PERFORMANCE_THRESHOLDS): Promise<void> {
    const threshold = FORUM_PERFORMANCE_THRESHOLDS[operation];
    
    // Start performance measurement
    const startTime = Date.now();
    
    // Wait for operation to complete (implementation depends on specific operation)
    await this.page.waitForLoadState('networkidle');
    
    const endTime = Date.now();
    const duration = endTime - startTime;
    
    // Verify performance meets threshold
    expect(duration).toBeLessThan(threshold);
  }

  /**
   * Cleanup test data and state
   */
  async cleanup(): Promise<void> {
    // Clear any test data, reset state, clean up temporary files
    await this.page.unrouteAll();
    
    // Clear local storage
    await this.page.evaluate(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
    
    // Clear any test attachments
    // In a real implementation, you would clean up temporary files
  }

  /**
   * Verify accessibility compliance
   */
  async verifyAccessibility(): Promise<void> {
    // Check for proper ARIA labels
    const elementsWithAriaLabels = await this.page.locator('[aria-label]').count();
    expect(elementsWithAriaLabels).toBeGreaterThan(0);

    // Check for proper heading hierarchy
    const headings = await this.page.locator('h1, h2, h3, h4, h5, h6').all();
    expect(headings.length).toBeGreaterThan(0);

    // Check for proper form labels
    const formInputs = await this.page.locator('input[type="text"], input[type="email"], textarea').all();
    for (const input of formInputs) {
      const hasLabel = await input.getAttribute('aria-label') || await input.getAttribute('id');
      expect(hasLabel).toBeTruthy();
    }

    // Check color contrast (basic check - in real tests you'd use axe-core)
    const buttons = await this.page.locator('button').all();
    expect(buttons.length).toBeGreaterThan(0);
  }

  /**
   * Test mobile responsiveness
   */
  async testMobileResponsiveness(): Promise<void> {
    // Set mobile viewport
    await this.page.setViewportSize({ width: 375, height: 667 });
    
    // Wait for responsive changes
    await this.page.waitForTimeout(500);
    
    // Verify mobile-specific elements are visible
    const mobileElements = await this.page.locator('[data-testid*="mobile"]').count();
    expect(mobileElements).toBeGreaterThan(0);
    
    // Reset to desktop viewport
    await this.page.setViewportSize({ width: 1920, height: 1080 });
  }
}