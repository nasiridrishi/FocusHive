/**
 * Buddy helper functions for E2E tests
 * Provides utilities for testing buddy system features, mocking data, and performance testing
 */

import {expect, Locator, Page} from '@playwright/test';

// Mock buddy data types based on the actual buddy types
export interface MockBuddyData {
  activeBuddies: Array<{
    id: number;
    username: string;
    avatar?: string;
    status: 'PENDING' | 'ACTIVE' | 'REJECTED' | 'TERMINATED' | 'COMPLETED';
    startDate: string;
    totalGoals: number;
    completedGoals: number;
    totalSessions: number;
    totalCheckins: number;
    partnerUsername: string;
    partnerAvatar?: string;
    isOnline?: boolean;
    currentActivity?: string;
  }>;
  pendingRequests: Array<{
    id: number;
    fromUsername: string;
    fromUserId: number;
    message: string;
    createdAt: string;
    goals?: string;
    expectations?: string;
  }>;
  sentRequests: Array<{
    id: number;
    toUsername: string;
    toUserId: number;
    status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
    message: string;
    createdAt: string;
  }>;
  potentialMatches: Array<{
    userId: number;
    username: string;
    avatar?: string;
    bio?: string;
    matchScore: number;
    compatibilityBreakdown: {
      timezoneMatch: number;
      focusAreaMatch: number;
      communicationStyleMatch: number;
      availabilityMatch: number;
    };
    commonFocusAreas: string[];
    timezoneOverlapHours: number;
    activeBuddyCount: number;
    completedGoalsCount: number;
    averageSessionRating?: number;
  }>;
  upcomingSessions: Array<{
    id: number;
    relationshipId: number;
    partnerUsername: string;
    sessionDate: string;
    plannedDurationMinutes: number;
    agenda?: string;
    status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  }>;
  goals: Array<{
    id: number;
    relationshipId: number;
    title: string;
    description?: string;
    status: 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
    dueDate?: string;
    progressPercentage: number;
    isJointGoal: boolean;
  }>;
  checkins: Array<{
    id: number;
    relationshipId: number;
    checkinTime: string;
    moodRating: number;
    progressRating: number;
    message: string;
    initiatedByUsername: string;
  }>;
  challenges: Array<{
    id: number;
    title: string;
    type: string;
    description: string;
    status: 'ACTIVE' | 'COMPLETED' | 'EXPIRED';
    participants: Array<{
      username: string;
      score: number;
      progress: number;
    }>;
    endDate: string;
  }>;
  studyGroups: Array<{
    id: number;
    name: string;
    description: string;
    subject: string;
    memberCount: number;
    maxMembers: number;
    meetingSchedule: string;
    isOwner: boolean;
  }>;
  stats: {
    totalBuddyRelationships: number;
    activeBuddies: number;
    completedRelationships: number;
    totalGoals: number;
    completedGoals: number;
    goalCompletionRate: number;
    totalSessions: number;
    completedSessions: number;
    sessionCompletionRate: number;
    averageSessionRating: number;
    totalCheckins: number;
    averageMoodRating: number;
    checkinStreak: number;
    buddyRating: number;
    buddyLevel: string;
  };
  notifications: Array<{
    id: number;
    type: 'NEW_BUDDY_REQUEST' | 'GOAL_REMINDER' | 'SESSION_REMINDER' | 'CHECKIN_REQUEST' | 'CHALLENGE_INVITE';
    title: string;
    message: string;
    fromUsername?: string;
    isRead: boolean;
    createdAt: string;
    actionUrl?: string;
  }>;
  lastUpdated: string;
}

export class BuddyHelper {
  constructor(private page: Page) {
  }

  /**
   * Generate mock buddy data with realistic values
   */
  generateMockBuddyData(options: {
    userId?: string;
    activeBuddyCount?: number;
    includePotentialMatches?: boolean;
    matchCount?: number;
    includeGoals?: boolean;
    includeCheckins?: boolean;
    includeSharedSessions?: boolean;
    includeJointGoals?: boolean;
    includeChallenges?: boolean;
    includeStudyGroups?: boolean;
    includeStreaks?: boolean;
    includeReminders?: boolean;
    includeMessages?: boolean;
    includeEncouragementTools?: boolean;
    includeSharedWorkspace?: boolean;
    includeBuddyNotifications?: boolean;
    includeHelpFeatures?: boolean;
    includeProfiles?: boolean;
    includeRelationshipHistory?: boolean;
    includeSwitchingOptions?: boolean;
    includeFeedbackSystem?: boolean;
    includeConflictResolution?: boolean;
    includeSafetyFeatures?: boolean;
    includeSecureCommunications?: boolean;
    includePrivacyValidation?: boolean;
    includePrivacySettings?: boolean;
    includeRealtimeUpdates?: boolean;
    includeConcurrentInteractions?: boolean;
    includeNotificationIntegration?: boolean;
    includeMaxLimitScenario?: boolean;
    includeSearchResults?: boolean;
    includeMatchReasons?: boolean;
    includeAllFeatures?: boolean;
    hasData?: boolean;
  } = {}): MockBuddyData {
    const {
      userId: _userId = 'test-user@focushive.com',
      activeBuddyCount = 2,
      includePotentialMatches = true,
      matchCount = 5,
      includeGoals = true,
      includeCheckins = true,
      includeSharedSessions = true,
      includeJointGoals = false,
      includeChallenges = false,
      includeStudyGroups = false,
      includeStreaks = false,
      includeReminders: _includeReminders = false,
      includeMessages: _includeMessages = false,
      includeEncouragementTools: _includeEncouragementTools = false,
      includeSharedWorkspace: _includeSharedWorkspace = false,
      includeBuddyNotifications = false,
      includeHelpFeatures: _includeHelpFeatures = false,
      includeProfiles = false,
      includeRelationshipHistory: _includeRelationshipHistory = false,
      includeSwitchingOptions: _includeSwitchingOptions = false,
      includeFeedbackSystem: _includeFeedbackSystem = false,
      includeConflictResolution: _includeConflictResolution = false,
      includeSafetyFeatures: _includeSafetyFeatures = false,
      includeSecureCommunications: _includeSecureCommunications = false,
      includePrivacyValidation: _includePrivacyValidation = false,
      includePrivacySettings: _includePrivacySettings = false,
      includeRealtimeUpdates = false,
      includeConcurrentInteractions: _includeConcurrentInteractions = false,
      includeNotificationIntegration = false,
      includeMaxLimitScenario: _includeMaxLimitScenario = false,
      includeSearchResults: _includeSearchResults = false,
      includeMatchReasons: _includeMatchReasons = false,
      includeAllFeatures: _includeAllFeatures = false,
      hasData = true
    } = options;

    if (!hasData) {
      return {
        activeBuddies: [],
        pendingRequests: [],
        sentRequests: [],
        potentialMatches: [],
        upcomingSessions: [],
        goals: [],
        checkins: [],
        challenges: [],
        studyGroups: [],
        stats: {
          totalBuddyRelationships: 0,
          activeBuddies: 0,
          completedRelationships: 0,
          totalGoals: 0,
          completedGoals: 0,
          goalCompletionRate: 0,
          totalSessions: 0,
          completedSessions: 0,
          sessionCompletionRate: 0,
          averageSessionRating: 0,
          totalCheckins: 0,
          averageMoodRating: 0,
          checkinStreak: 0,
          buddyRating: 0,
          buddyLevel: 'Beginner'
        },
        notifications: [],
        lastUpdated: new Date().toISOString()
      };
    }

    // Generate active buddies
    const activeBuddies = Array.from({length: activeBuddyCount}, (_, i) => ({
      id: i + 1,
      username: `buddy_${i + 1}`,
      avatar: `https://avatars.example.com/buddy_${i + 1}.jpg`,
      status: 'ACTIVE' as const,
      startDate: new Date(Date.now() - Math.random() * 90 * 24 * 60 * 60 * 1000).toISOString(),
      totalGoals: Math.floor(Math.random() * 10) + 3,
      completedGoals: Math.floor(Math.random() * 8) + 1,
      totalSessions: Math.floor(Math.random() * 20) + 5,
      totalCheckins: Math.floor(Math.random() * 50) + 10,
      partnerUsername: `buddy_${i + 1}`,
      partnerAvatar: `https://avatars.example.com/buddy_${i + 1}.jpg`,
      isOnline: includeRealtimeUpdates ? Math.random() > 0.3 : true,
      currentActivity: includeRealtimeUpdates ? ['Available', 'In Focus Session', 'Busy', 'Away'][Math.floor(Math.random() * 4)] : 'Available'
    }));

    // Generate pending buddy requests
    const pendingRequests = Array.from({length: 2}, (_, i) => ({
      id: i + 100,
      fromUsername: `requester_${i + 1}`,
      fromUserId: i + 200,
      message: `Hi! I'd love to be accountability buddies. I'm working on ${['web development', 'data science', 'machine learning', 'mobile apps'][Math.floor(Math.random() * 4)]} projects.`,
      createdAt: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
      goals: includeGoals ? 'Complete certification course and build portfolio project' : undefined,
      expectations: includeGoals ? 'Weekly check-ins and mutual accountability' : undefined
    }));

    // Generate sent requests
    const sentRequests = Array.from({length: 1}, (_, i) => ({
      id: i + 300,
      toUsername: `potential_buddy_${i + 1}`,
      toUserId: i + 400,
      status: 'PENDING' as const,
      message: 'Hi! I saw we have similar focus areas and would love to be accountability partners.',
      createdAt: new Date(Date.now() - Math.random() * 3 * 24 * 60 * 60 * 1000).toISOString()
    }));

    // Generate potential matches
    const potentialMatches = includePotentialMatches ? Array.from({length: matchCount}, (_, i) => {
      const focusAreas = ['Programming', 'Web Development', 'Data Science', 'Mobile Development', 'UI/UX Design'];
      const commonAreas = focusAreas.slice(0, Math.floor(Math.random() * 3) + 1);

      return {
        userId: i + 500,
        username: `potential_${i + 1}`,
        avatar: `https://avatars.example.com/potential_${i + 1}.jpg`,
        bio: includeProfiles ? `Passionate about ${commonAreas.join(', ')}. Looking for an accountability buddy to help stay motivated and focused on my goals.` : undefined,
        matchScore: Math.floor(Math.random() * 30) + 70, // 70-100% match
        compatibilityBreakdown: {
          timezoneMatch: Math.floor(Math.random() * 40) + 60,
          focusAreaMatch: Math.floor(Math.random() * 50) + 50,
          communicationStyleMatch: Math.floor(Math.random() * 30) + 70,
          availabilityMatch: Math.floor(Math.random() * 40) + 60
        },
        commonFocusAreas: commonAreas,
        timezoneOverlapHours: Math.floor(Math.random() * 12) + 4,
        activeBuddyCount: Math.floor(Math.random() * 3),
        completedGoalsCount: Math.floor(Math.random() * 50) + 10,
        averageSessionRating: Math.random() * 2 + 3 // 3.0-5.0 rating
      };
    }) : [];

    // Generate upcoming sessions
    const upcomingSessions = includeSharedSessions ? Array.from({length: 3}, (_, i) => ({
      id: i + 600,
      relationshipId: i + 1, // Link to active buddies
      partnerUsername: `buddy_${(i % activeBuddyCount) + 1}`,
      sessionDate: new Date(Date.now() + (i + 1) * 24 * 60 * 60 * 1000).toISOString(),
      plannedDurationMinutes: [25, 45, 60, 90][Math.floor(Math.random() * 4)],
      agenda: `Focus session ${i + 1}: Work on individual projects with periodic check-ins`,
      status: 'SCHEDULED' as const
    })) : [];

    // Generate goals
    const goals = includeGoals ? Array.from({length: activeBuddyCount * 3}, (_, i) => ({
      id: i + 700,
      relationshipId: Math.floor(i / 3) + 1,
      title: [
        'Complete Module 3 of Online Course',
        'Finish Project Prototype',
        'Read 2 Technical Articles Daily',
        'Practice Coding for 1 Hour Daily',
        'Write Technical Blog Post',
        'Update Portfolio Website',
        'Complete Code Review'
      ][i % 7],
      description: 'Detailed description of the goal and success criteria.',
      status: ['IN_PROGRESS', 'COMPLETED', 'IN_PROGRESS'][i % 3] as 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED',
      dueDate: new Date(Date.now() + Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString(),
      progressPercentage: Math.floor(Math.random() * 100),
      isJointGoal: includeJointGoals && i % 4 === 0
    })) : [];

    // Generate check-ins
    const checkins = includeCheckins ? Array.from({length: activeBuddyCount * 5}, (_, i) => ({
      id: i + 800,
      relationshipId: Math.floor(i / 5) + 1,
      checkinTime: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
      moodRating: Math.floor(Math.random() * 5) + 1,
      progressRating: Math.floor(Math.random() * 5) + 1,
      message: [
        'Great progress today! Completed two major milestones.',
        'Feeling a bit stuck but pushing through.',
        'Excellent day! Everything went according to plan.',
        'Had some challenges but learned a lot.',
        'Amazing breakthrough today!'
      ][i % 5],
      initiatedByUsername: i % 2 === 0 ? 'You' : `buddy_${Math.floor(i / 5) + 1}`
    })) : [];

    // Generate challenges
    const challenges = includeChallenges ? Array.from({length: 2}, (_, i) => ({
      id: i + 900,
      title: ['Weekly Focus Challenge', 'Goal Sprint Challenge'][i],
      type: ['focus-time', 'goal-completion'][i],
      description: i === 0 ? 'Who can maintain the longest focus sessions this week?' : 'Race to complete the most goals this month!',
      status: ['ACTIVE', 'ACTIVE'][i] as const,
      participants: [
        {
          username: 'You',
          score: Math.floor(Math.random() * 100),
          progress: Math.floor(Math.random() * 100)
        },
        {
          username: `buddy_${i + 1}`,
          score: Math.floor(Math.random() * 100),
          progress: Math.floor(Math.random() * 100)
        }
      ],
      endDate: new Date(Date.now() + (7 - i * 2) * 24 * 60 * 60 * 1000).toISOString()
    })) : [];

    // Generate study groups
    const studyGroups = includeStudyGroups ? Array.from({length: 1}, (_, i) => ({
      id: i + 1000,
      name: 'Algorithm Study Group',
      description: 'Weekly algorithm practice and problem-solving sessions',
      subject: 'Computer Science',
      memberCount: 4,
      maxMembers: 6,
      meetingSchedule: 'Saturdays 2-4 PM EST',
      isOwner: i === 0
    })) : [];

    // Generate stats
    const stats = {
      totalBuddyRelationships: activeBuddyCount + 2, // Active + completed
      activeBuddies: activeBuddyCount,
      completedRelationships: 2,
      totalGoals: goals.length,
      completedGoals: goals.filter(g => g.status === 'COMPLETED').length,
      goalCompletionRate: goals.length > 0 ? goals.filter(g => g.status === 'COMPLETED').length / goals.length : 0,
      totalSessions: upcomingSessions.length + 12, // Upcoming + completed
      completedSessions: 12,
      sessionCompletionRate: 0.85,
      averageSessionRating: 4.2,
      totalCheckins: checkins.length,
      averageMoodRating: checkins.length > 0 ? checkins.reduce((sum, c) => sum + c.moodRating, 0) / checkins.length : 0,
      checkinStreak: includeStreaks ? 14 : 5,
      buddyRating: 4.5,
      buddyLevel: includeStreaks ? 'Expert' : 'Intermediate'
    };

    // Generate notifications
    const notifications = (includeBuddyNotifications || includeNotificationIntegration) ? [
      {
        id: 1100,
        type: 'NEW_BUDDY_REQUEST' as const,
        title: 'New Buddy Request',
        message: 'Sarah wants to be your accountability buddy',
        fromUsername: 'sarah_dev',
        isRead: false,
        createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
        actionUrl: '/buddy?tab=requests'
      },
      {
        id: 1101,
        type: 'GOAL_REMINDER' as const,
        title: 'Goal Due Soon',
        message: 'Your goal "Complete Module 3" is due in 2 days',
        isRead: false,
        createdAt: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString(),
        actionUrl: '/buddy?tab=goals'
      },
      {
        id: 1102,
        type: 'SESSION_REMINDER' as const,
        title: 'Upcoming Session',
        message: 'Focus session with Alex starts in 30 minutes',
        fromUsername: 'alex_coder',
        isRead: true,
        createdAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
        actionUrl: '/buddy?tab=sessions'
      }
    ] : [];

    return {
      activeBuddies,
      pendingRequests,
      sentRequests,
      potentialMatches,
      upcomingSessions,
      goals,
      checkins,
      challenges,
      studyGroups,
      stats,
      notifications,
      lastUpdated: new Date().toISOString()
    };
  }

  /**
   * Mock buddy API endpoints
   */
  async mockBuddyApi(mockData: MockBuddyData | unknown): Promise<void> {
    // Mock buddy dashboard endpoint
    await this.page.route('**/api/v1/buddy/dashboard**', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: mockData
        })
      });
    });

    // Mock individual buddy endpoints
    await this.page.route('**/api/v1/buddy/relationships/active**', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: mockData.activeBuddies || []
        })
      });
    });

    await this.page.route('**/api/v1/buddy/requests/received**', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: mockData.pendingRequests || []
        })
      });
    });

    await this.page.route('**/api/v1/buddy/requests/sent**', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: mockData.sentRequests || []
        })
      });
    });

    await this.page.route('**/api/v1/buddy/matches**', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: mockData.potentialMatches || []
        })
      });
    });

    await this.page.route('**/api/v1/buddy/sessions/upcoming**', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: mockData.upcomingSessions || []
        })
      });
    });

    await this.page.route('**/api/v1/buddy/goals**', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: mockData.goals || []
        })
      });
    });

    await this.page.route('**/api/v1/buddy/checkins**', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: mockData.checkins || []
        })
      });
    });

    await this.page.route('**/api/v1/buddy/stats**', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: mockData.stats || {}
        })
      });
    });

    // Mock POST endpoints for creating/updating
    await this.page.route('**/api/v1/buddy/requests', (route) => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            message: 'Buddy request sent successfully'
          })
        });
      } else {
        route.continue();
      }
    });

    await this.page.route('**/api/v1/buddy/goals', (route) => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            message: 'Goal created successfully'
          })
        });
      } else {
        route.continue();
      }
    });

    await this.page.route('**/api/v1/buddy/checkins', (route) => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            message: 'Check-in submitted successfully'
          })
        });
      } else {
        route.continue();
      }
    });

    await this.page.route('**/api/v1/buddy/sessions', (route) => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            message: 'Session scheduled successfully'
          })
        });
      } else {
        route.continue();
      }
    });

    // Mock WebSocket for real-time updates
    if (mockData.includeRealtimeUpdates) {
      await this.page.addInitScript(() => {
        // Mock WebSocket updates
        setTimeout(() => {
          window.dispatchEvent(new CustomEvent('buddy-status-update', {
            detail: {buddyId: 'buddy-1', status: 'in-focus-session'}
          }));
        }, 2000);
      });
    }
  }

  /**
   * Mock empty buddy data
   */
  async mockEmptyBuddyData(): Promise<void> {
    const emptyData = this.generateMockBuddyData({hasData: false});
    await this.mockBuddyApi(emptyData);
  }

  /**
   * Mock slow buddy API response
   */
  async mockSlowBuddyApi(delay: number): Promise<void> {
    await this.page.route('**/api/v1/buddy/**', async (route) => {
      await new Promise(resolve => setTimeout(resolve, delay));
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: this.generateMockBuddyData()
        })
      });
    });
  }

  /**
   * Mock buddy API error
   */
  async mockBuddyApiError(statusCode: number): Promise<void> {
    await this.page.route('**/api/v1/buddy/**', (route) => {
      route.fulfill({
        status: statusCode,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          error: 'Internal server error'
        })
      });
    });
  }

  /**
   * Mock network failure
   */
  async mockNetworkFailure(): Promise<void> {
    await this.page.route('**/api/v1/buddy/**', (route) => {
      route.abort('failed');
    });
  }

  /**
   * Simulate buddy status change for real-time testing
   */
  async simulateStatusChange(buddyId: string, newStatus: string): Promise<void> {
    await this.page.evaluate(
        ({buddyId, newStatus}) => {
          window.dispatchEvent(new CustomEvent('buddy-status-update', {
            detail: {buddyId, status: newStatus}
          }));
        },
        {buddyId, newStatus}
    );
  }

  /**
   * Test responsive design across different screen sizes
   */
  async testResponsiveDesign(): Promise<void> {
    const viewports = [
      {width: 1920, height: 1080, name: 'Desktop Large'},
      {width: 1366, height: 768, name: 'Desktop'},
      {width: 768, height: 1024, name: 'Tablet'},
      {width: 375, height: 667, name: 'Mobile'}
    ];

    for (const viewport of viewports) {
      await this.page.setViewportSize(viewport);
      await this.page.waitForTimeout(500); // Allow time for responsive adjustments

      // Verify essential elements are still visible
      const dashboard = this.page.locator('[data-testid="buddy-dashboard"]');
      await expect(dashboard).toBeVisible();
    }

    // Reset to default desktop size
    await this.page.setViewportSize({width: 1366, height: 768});
  }

  /**
   * Validate buddy system accessibility
   */
  async validateBuddyAccessibility(): Promise<void> {
    // Check for proper ARIA labels on buddy cards
    const buddyCards = this.page.locator('[data-testid="buddy-card"]');
    const cardCount = await buddyCards.count();

    for (let i = 0; i < cardCount; i++) {
      const card = buddyCards.nth(i);
      const ariaLabel = await card.getAttribute('aria-label');
      if (ariaLabel) {
        expect(ariaLabel).toContain('buddy');
      }
    }

    // Check for keyboard focus management
    const interactiveElements = this.page.locator('button, [role="button"], [role="tab"], input');
    const elementsCount = await interactiveElements.count();
    expect(elementsCount).toBeGreaterThan(0);

    // Verify tab navigation works
    await this.page.keyboard.press('Tab');
    const focusedElement = await this.page.evaluate(() => document.activeElement?.tagName);
    expect(['BUTTON', 'INPUT', 'A', 'DIV']).toContain(focusedElement);
  }

  /**
   * Setup performance monitoring
   */
  async setupPerformanceMonitoring(): Promise<{
    getMetrics: () => Promise<{
      apiResponseTime: number;
      renderTime: number;
      interactionTime: number;
    }>;
  }> {
    let apiStartTime: number = 0;
    let apiEndTime: number = 0;

    // Monitor API responses
    this.page.on('request', (request) => {
      if (request.url().includes('/api/v1/buddy/')) {
        apiStartTime = Date.now();
      }
    });

    this.page.on('response', (response) => {
      if (response.url().includes('/api/v1/buddy/')) {
        apiEndTime = Date.now();
      }
    });

    return {
      getMetrics: async () => {
        const renderStartTime = Date.now();

        // Wait for buddy components to render
        await this.page.locator('[data-testid="buddy-dashboard"]').waitFor({state: 'visible'});

        const renderEndTime = Date.now();

        // Test interaction performance
        const interactionStartTime = Date.now();
        const firstBuddy = this.page.locator('[data-testid="buddy-card"]').first();
        if (await firstBuddy.isVisible()) {
          await firstBuddy.hover();
        }
        const interactionEndTime = Date.now();

        return {
          apiResponseTime: apiEndTime - apiStartTime,
          renderTime: renderEndTime - renderStartTime,
          interactionTime: interactionEndTime - interactionStartTime
        };
      }
    };
  }

  /**
   * Cleanup test data and mocks
   */
  async cleanup(): Promise<void> {
    // Remove all route mocks
    await this.page.unrouteAll();

    // Clear any test-specific local storage
    await this.page.evaluate(() => {
      Object.keys(localStorage).forEach(key => {
        if (key.startsWith('buddy-test-') || key.startsWith('test-buddy-')) {
          localStorage.removeItem(key);
        }
      });
    });

    // Clear any test-specific session storage
    await this.page.evaluate(() => {
      Object.keys(sessionStorage).forEach(key => {
        if (key.startsWith('buddy-test-') || key.startsWith('test-buddy-')) {
          sessionStorage.removeItem(key);
        }
      });
    });
  }

  /**
   * Verify data accuracy for displayed buddy information
   */
  async verifyDataAccuracy(expectedData: MockBuddyData): Promise<void> {
    // Verify active buddy count
    const buddyCards = this.page.locator('[data-testid="buddy-card"]');
    const displayedCount = await buddyCards.count();
    expect(displayedCount).toBe(expectedData.activeBuddies.length);

    // Verify buddy information is displayed correctly
    for (let i = 0; i < Math.min(displayedCount, expectedData.activeBuddies.length); i++) {
      const card = buddyCards.nth(i);
      const expectedBuddy = expectedData.activeBuddies[i];

      // Check username is displayed
      const usernameElement = card.locator('[data-testid="buddy-username"]');
      if (await usernameElement.isVisible()) {
        await expect(usernameElement).toContainText(expectedBuddy.username);
      }

      // Check status is displayed
      const statusElement = card.locator('[data-testid="buddy-status"]');
      if (await statusElement.isVisible()) {
        await expect(statusElement).toContainText(expectedBuddy.status);
      }
    }
  }

  /**
   * Test chart interactions (if buddy system has charts)
   */
  async testChartInteractions(chartLocator: Locator): Promise<void> {
    // Test hover interactions
    await chartLocator.hover();

    // Look for tooltips or interactive elements
    const tooltip = this.page.locator('[role="tooltip"]');
    if (await tooltip.isVisible()) {
      await expect(tooltip).toBeVisible();
    }

    // Test click interactions if chart has clickable elements
    const clickableElements = chartLocator.locator('circle, rect, path').first();
    if (await clickableElements.isVisible()) {
      await clickableElements.click();
    }
  }

  /**
   * Validate chart accessibility (for statistics charts)
   */
  async validateChartAccessibility(chartLocator: Locator): Promise<void> {
    // Check for aria-label or aria-describedby
    const ariaLabel = await chartLocator.getAttribute('aria-label');
    const ariaDescribedBy = await chartLocator.getAttribute('aria-describedby');

    expect(ariaLabel || ariaDescribedBy).toBeTruthy();

    // Check for chart legend accessibility
    const legend = chartLocator.locator('[aria-label*="legend"], .chart-legend');
    if (await legend.isVisible()) {
      await expect(legend).toBeVisible();
    }
  }
}