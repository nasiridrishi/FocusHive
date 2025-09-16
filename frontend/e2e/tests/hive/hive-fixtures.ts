/**
 * Hive Test Fixtures and Mock Data
 * Provides consistent test data for hive workflow E2E tests
 */

import {CreateHiveRequest, Hive, HiveMember, HiveSettings} from '../../../src/services/api/hiveApi';

export interface TestHive extends Partial<Hive> {
  id: number;
  name: string;
  slug: string;
  isPrivate: boolean;
  maxMembers: number;
  memberCount: number;
}

// Test Users for Multi-User Scenarios
export const HIVE_TEST_USERS = {
  OWNER: {
    id: 1,
    username: 'hive_owner',
    email: 'owner@focushive.test',
    password: 'SecurePassword123!',
    displayName: 'Hive Owner',
    avatar: 'https://via.placeholder.com/150/blue/white?text=HO'
  },
  MEMBER_1: {
    id: 2,
    username: 'active_member',
    email: 'member1@focushive.test',
    password: 'SecurePassword123!',
    displayName: 'Active Member',
    avatar: 'https://via.placeholder.com/150/green/white?text=AM'
  },
  MEMBER_2: {
    id: 3,
    username: 'casual_member',
    email: 'member2@focushive.test',
    password: 'SecurePassword123!',
    displayName: 'Casual Member',
    avatar: 'https://via.placeholder.com/150/orange/white?text=CM'
  },
  MODERATOR: {
    id: 4,
    username: 'hive_moderator',
    email: 'moderator@focushive.test',
    password: 'SecurePassword123!',
    displayName: 'Hive Moderator',
    avatar: 'https://via.placeholder.com/150/purple/white?text=HM'
  },
  NON_MEMBER: {
    id: 5,
    username: 'non_member',
    email: 'nonmember@focushive.test',
    password: 'SecurePassword123!',
    displayName: 'Non Member',
    avatar: 'https://via.placeholder.com/150/red/white?text=NM'
  }
} as const;

// Default Hive Settings
export const DEFAULT_HIVE_SETTINGS: HiveSettings = {
  allowChat: true,
  allowMusic: true,
  requireApproval: false,
  workHours: {
    start: '09:00',
    end: '17:00',
    timezone: 'UTC'
  }
};

// Test Hive Templates
export const HIVE_TEMPLATES = {
  PUBLIC_STUDY_HIVE: {
    name: 'Public Study Group',
    description: 'A public hive for collaborative studying',
    slug: 'public-study-group',
    isPrivate: false,
    maxMembers: 10,
    memberCount: 3,
    tags: ['study', 'academic', 'collaborative'],
    settings: {
      ...DEFAULT_HIVE_SETTINGS,
      requireApproval: false
    }
  } as TestHive,

  PRIVATE_WORK_HIVE: {
    name: 'Private Work Team',
    description: 'Private hive for team productivity',
    slug: 'private-work-team',
    isPrivate: true,
    maxMembers: 5,
    memberCount: 2,
    tags: ['work', 'team', 'productivity'],
    settings: {
      ...DEFAULT_HIVE_SETTINGS,
      requireApproval: true,
      allowMusic: false
    }
  } as TestHive,

  LARGE_COMMUNITY_HIVE: {
    name: 'Large Community Hive',
    description: 'Large community for general productivity',
    slug: 'large-community-hive',
    isPrivate: false,
    maxMembers: 100,
    memberCount: 25,
    tags: ['community', 'productivity', 'general'],
    settings: {
      ...DEFAULT_HIVE_SETTINGS,
      requireApproval: false
    }
  } as TestHive,

  APPROVAL_REQUIRED_HIVE: {
    name: 'Exclusive Focus Group',
    description: 'Exclusive hive requiring approval',
    slug: 'exclusive-focus-group',
    isPrivate: false,
    maxMembers: 8,
    memberCount: 5,
    tags: ['exclusive', 'focus', 'curated'],
    settings: {
      ...DEFAULT_HIVE_SETTINGS,
      requireApproval: true
    }
  } as TestHive,

  FULL_CAPACITY_HIVE: {
    name: 'Full Capacity Hive',
    description: 'Hive at maximum capacity',
    slug: 'full-capacity-hive',
    isPrivate: false,
    maxMembers: 3,
    memberCount: 3,
    tags: ['full', 'waitlist'],
    settings: DEFAULT_HIVE_SETTINGS
  } as TestHive
} as const;

// Timer Configuration Templates
export const TIMER_CONFIGURATIONS = {
  POMODORO_25_5: {
    type: 'pomodoro',
    workDuration: 25,
    shortBreakDuration: 5,
    longBreakDuration: 15,
    sessionsUntilLongBreak: 4,
    autoStartBreaks: true,
    autoStartSessions: false
  },
  POMODORO_50_10: {
    type: 'pomodoro',
    workDuration: 50,
    shortBreakDuration: 10,
    longBreakDuration: 30,
    sessionsUntilLongBreak: 3,
    autoStartBreaks: false,
    autoStartSessions: false
  },
  CONTINUOUS_60: {
    type: 'continuous',
    duration: 60,
    reminderInterval: 15,
    allowPause: true
  },
  FLEXIBLE: {
    type: 'flexible',
    minDuration: 15,
    maxDuration: 180,
    suggestedDuration: 45,
    allowCustom: true
  }
} as const;

// Presence Status Templates
export const PRESENCE_STATUSES = {
  ACTIVE: {
    status: 'active',
    activity: 'focusing',
    lastSeen: new Date().toISOString(),
    currentSession: {
      id: 'session_1',
      startedAt: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
      timerType: 'pomodoro',
      remainingTime: 600 // 10 minutes
    }
  },
  BREAK: {
    status: 'break',
    activity: 'on break',
    lastSeen: new Date().toISOString(),
    currentSession: {
      id: 'session_1',
      startedAt: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
      timerType: 'break',
      remainingTime: 180 // 3 minutes
    }
  },
  AWAY: {
    status: 'away',
    activity: 'away',
    lastSeen: new Date(Date.now() - 10 * 60 * 1000).toISOString(),
    currentSession: null
  },
  OFFLINE: {
    status: 'offline',
    activity: 'offline',
    lastSeen: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    currentSession: null
  }
} as const;

// Analytics Mock Data
export const ANALYTICS_MOCK_DATA = {
  PERSONAL_STATS: {
    totalFocusTime: 1440, // 24 hours in minutes
    averageSessionLength: 35,
    completedSessions: 42,
    currentStreak: 7,
    longestStreak: 14,
    weeklyGoalProgress: 0.75,
    productivityScore: 85,
    weeklyData: [
      {day: 'Mon', focusTime: 180, sessions: 5},
      {day: 'Tue', focusTime: 210, sessions: 6},
      {day: 'Wed', focusTime: 165, sessions: 4},
      {day: 'Thu', focusTime: 195, sessions: 5},
      {day: 'Fri', focusTime: 240, sessions: 7},
      {day: 'Sat', focusTime: 120, sessions: 3},
      {day: 'Sun', focusTime: 150, sessions: 4}
    ]
  },
  HIVE_STATS: {
    totalMembers: 8,
    activeMembers: 5,
    avgSessionsPerMember: 3.2,
    totalHiveFocusTime: 5280, // 88 hours
    popularTimes: [
      {hour: 9, count: 12},
      {hour: 10, count: 18},
      {hour: 11, count: 15},
      {hour: 14, count: 20},
      {hour: 15, count: 22},
      {hour: 16, count: 16}
    ],
    weeklyActivity: [85, 92, 78, 88, 95, 42, 38] // Activity percentage by day
  }
} as const;

// WebSocket Message Templates
export const WEBSOCKET_MESSAGES = {
  PRESENCE_UPDATE: {
    type: 'PRESENCE_UPDATE',
    payload: {
      userId: 1,
      hiveId: 1,
      status: 'active',
      timestamp: new Date().toISOString()
    }
  },
  TIMER_SYNC: {
    type: 'TIMER_SYNC',
    payload: {
      hiveId: 1,
      sessionId: 'session_1',
      remainingTime: 1500,
      isActive: true,
      participants: [1, 2, 3]
    }
  },
  MEMBER_JOINED: {
    type: 'MEMBER_JOINED',
    payload: {
      hiveId: 1,
      member: {
        id: 2,
        userId: 2,
        username: 'new_member',
        displayName: 'New Member'
      }
    }
  },
  MEMBER_LEFT: {
    type: 'MEMBER_LEFT',
    payload: {
      hiveId: 1,
      memberId: 2
    }
  }
} as const;

// Form Validation Test Cases
export const FORM_VALIDATION_CASES = {
  INVALID_HIVE_DATA: [
    {
      name: '',
      description: 'Valid description',
      expectedError: 'Hive name is required'
    },
    {
      name: 'AB',
      description: 'Valid description',
      expectedError: 'Hive name must be at least 3 characters'
    },
    {
      name: 'A'.repeat(101),
      description: 'Valid description',
      expectedError: 'Hive name must be 100 characters or less'
    },
    {
      name: 'Valid Name',
      description: '',
      maxMembers: 0,
      expectedError: 'Maximum members must be at least 2'
    },
    {
      name: 'Valid Name',
      description: 'Valid description',
      maxMembers: 1001,
      expectedError: 'Maximum members cannot exceed 1000'
    }
  ]
} as const;

// Performance Test Scenarios
export const PERFORMANCE_SCENARIOS = {
  CONCURRENT_USERS: {
    LOW_LOAD: 5,
    MEDIUM_LOAD: 15,
    HIGH_LOAD: 50
  },
  TIMER_SYNC_LATENCY: {
    ACCEPTABLE_MS: 100,
    WARNING_MS: 500,
    CRITICAL_MS: 1000
  },
  PRESENCE_UPDATE_LATENCY: {
    ACCEPTABLE_MS: 50,
    WARNING_MS: 200,
    CRITICAL_MS: 500
  }
} as const;

// Helper function to generate unique test data
export function generateUniqueHiveData(template: string = 'PUBLIC_STUDY_HIVE'): CreateHiveRequest {
  const timestamp = Date.now();
  const baseTemplate = HIVE_TEMPLATES[template as keyof typeof HIVE_TEMPLATES];

  return {
    name: `${baseTemplate.name} ${timestamp}`,
    description: `${baseTemplate.description} (Test ${timestamp})`,
    slug: `${baseTemplate.slug}-${timestamp}`,
    isPrivate: baseTemplate.isPrivate,
    maxMembers: baseTemplate.maxMembers,
    tags: baseTemplate.tags,
    settings: baseTemplate.settings
  };
}

// Helper function to create test member data
export function generateTestMember(userId: number, role: 'OWNER' | 'ADMIN' | 'MODERATOR' | 'MEMBER' = 'MEMBER'): HiveMember {
  const user = Object.values(HIVE_TEST_USERS).find(u => u.id === userId) || HIVE_TEST_USERS.MEMBER_1;

  return {
    id: userId * 10, // Unique member ID
    userId,
    hiveId: 1,
    role,
    joinedAt: new Date().toISOString(),
    user: {
      id: userId,
      username: user.username,
      displayName: user.displayName,
      avatar: user.avatar
    }
  };
}

export default {
  HIVE_TEST_USERS,
  HIVE_TEMPLATES,
  TIMER_CONFIGURATIONS,
  PRESENCE_STATUSES,
  ANALYTICS_MOCK_DATA,
  WEBSOCKET_MESSAGES,
  FORM_VALIDATION_CASES,
  PERFORMANCE_SCENARIOS,
  generateUniqueHiveData,
  generateTestMember
};