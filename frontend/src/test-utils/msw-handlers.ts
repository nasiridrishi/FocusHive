import { http, HttpResponse } from 'msw';
import type { 
  LoginRequest, 
  LoginResponse, 
  RegisterRequest, 
  User 
} from '@shared/types/auth';
import type { HiveResponse, CreateHiveRequest } from '@shared/types/hive';
import type { GamificationStats } from '@features/gamification/types/gamification';

// Mock data
const mockUser: User = {
  id: '1',
  username: 'testuser',
  email: 'test@example.com',
  displayName: 'Test User',
  avatar: null,
  createdAt: new Date('2024-01-01').toISOString(),
  lastLoginAt: new Date('2024-01-15').toISOString()
};

const mockGamificationStats: GamificationStats = {
  points: {
    current: 1250,
    total: 15750,
    todayEarned: 150,
    weekEarned: 420,
  },
  achievements: [
    {
      id: 'first-focus',
      title: 'First Focus',
      description: 'Complete your first focus session',
      icon: 'focus',
      category: 'focus',
      points: 100,
      unlockedAt: new Date('2024-01-15T10:30:00Z'),
      isUnlocked: true,
      rarity: 'common',
    },
  ],
  streaks: [
    {
      id: 'daily-login-1',
      type: 'daily_login',
      current: 7,
      best: 15,
      lastActivity: new Date('2024-01-15T10:30:00Z'),
      isActive: true,
    },
  ],
  level: 12,
  rank: 256,
  totalUsers: 1500,
};

const mockHives: HiveResponse[] = [
  {
    id: '1',
    name: 'Study Group',
    description: 'A focused study environment',
    type: 'study',
    isPublic: true,
    maxMembers: 10,
    currentMembers: 5,
    createdAt: new Date('2024-01-01').toISOString(),
    updatedAt: new Date('2024-01-15').toISOString(),
    ownerId: '1',
    tags: ['study', 'productivity'],
    settings: {
      allowChat: true,
      requireApproval: false,
      mutingRules: {
        autoMute: false,
        schedules: []
      }
    }
  }
];

export const handlers = [
  // Authentication endpoints
  http.post('/api/auth/login', async ({ request }) => {
    const loginData = await request.json() as LoginRequest;
    
    if (loginData.email === 'testuser@example.com' && loginData.password === 'password') {
      const response: LoginResponse = {
        user: mockUser,
        token: 'mock-jwt-token',
        refreshToken: 'mock-refresh-token'
      };
      return HttpResponse.json(response);
    }
    
    return HttpResponse.json(
      { message: 'Invalid credentials' }, 
      { status: 401 }
    );
  }),

  http.post('/api/auth/register', async ({ request }) => {
    const registerData = await request.json() as RegisterRequest;
    
    if (registerData.username && registerData.email && registerData.password) {
      const response: LoginResponse = {
        user: {
          ...mockUser,
          username: registerData.username,
          email: registerData.email,
          name: `${registerData.firstName} ${registerData.lastName}`
        },
        token: 'mock-jwt-token',
        refreshToken: 'mock-refresh-token'
      };
      return HttpResponse.json(response, { status: 201 });
    }
    
    return HttpResponse.json(
      { message: 'Invalid registration data' }, 
      { status: 400 }
    );
  }),

  http.post('/api/auth/refresh', () => {
    return HttpResponse.json({
      accessToken: 'new-mock-jwt-token',
      refreshToken: 'new-mock-refresh-token'
    });
  }),

  http.post('/api/auth/logout', () => {
    return HttpResponse.json({ success: true });
  }),

  http.get('/api/auth/me', ({ request }) => {
    const authHeader = request.headers.get('authorization');
    if (authHeader?.startsWith('Bearer ')) {
      return HttpResponse.json(mockUser);
    }
    return HttpResponse.json(
      { message: 'Unauthorized' }, 
      { status: 401 }
    );
  }),

  // Hive endpoints
  http.get('/api/hives', () => {
    return HttpResponse.json(mockHives);
  }),

  http.post('/api/hives', async ({ request }) => {
    const hiveData = await request.json() as CreateHiveRequest;
    
    const newHive: HiveResponse = {
      id: String(mockHives.length + 1),
      ...hiveData,
      currentMembers: 1,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      ownerId: mockUser.id,
      settings: {
        allowChat: true,
        requireApproval: false,
        mutingRules: {
          autoMute: false,
          schedules: []
        }
      }
    };
    
    mockHives.push(newHive);
    return HttpResponse.json(newHive, { status: 201 });
  }),

  http.get('/api/hives/:id', ({ params }) => {
    const hive = mockHives.find(h => h.id === params.id);
    if (!hive) {
      return HttpResponse.json(
        { message: 'Hive not found' }, 
        { status: 404 }
      );
    }
    return HttpResponse.json(hive);
  }),

  // Gamification endpoints
  http.get('/api/gamification/stats', () => {
    return HttpResponse.json(mockGamificationStats);
  }),

  http.post('/api/gamification/points', async ({ request }) => {
    const { points, reason } = await request.json() as { points: number; reason: string };
    
    // Mock adding points
    const updatedStats = {
      ...mockGamificationStats,
      points: {
        ...mockGamificationStats.points,
        current: mockGamificationStats.points.current + points,
        total: mockGamificationStats.points.total + points,
        todayEarned: mockGamificationStats.points.todayEarned + points
      }
    };
    
    return HttpResponse.json(updatedStats);
  }),

  http.post('/api/gamification/achievements/:id/unlock', ({ params }) => {
    return HttpResponse.json({ 
      success: true, 
      achievementId: params.id 
    });
  }),

  http.post('/api/gamification/streaks/:type', ({ params }) => {
    return HttpResponse.json({ 
      success: true, 
      streakType: params.type 
    });
  }),

  // Presence endpoints
  http.get('/api/presence/hive/:hiveId', ({ params }) => {
    return HttpResponse.json([
      {
        userId: mockUser.id,
        username: mockUser.username,
        displayName: mockUser.displayName,
        avatar: mockUser.avatar,
        status: 'focusing',
        joinedAt: new Date(),
        lastSeen: new Date()
      }
    ]);
  }),

  // Timer/Session endpoints
  http.post('/api/sessions', async ({ request }) => {
    const sessionData = await request.json() as {
      hiveId: string;
      type: string;
      duration: number;
    };
    
    return HttpResponse.json({
      id: 'session-1',
      ...sessionData,
      userId: mockUser.id,
      startedAt: new Date(),
      status: 'active'
    }, { status: 201 });
  }),

  http.put('/api/sessions/:id/end', ({ params }) => {
    return HttpResponse.json({
      id: params.id,
      endedAt: new Date(),
      status: 'completed',
      duration: 1500 // 25 minutes in seconds
    });
  }),

  // Chat endpoints
  http.get('/api/hives/:hiveId/messages', ({ params }) => {
    return HttpResponse.json([
      {
        id: 'msg-1',
        content: 'Hello everyone!',
        userId: mockUser.id,
        username: mockUser.username,
        timestamp: new Date(),
        hiveId: params.hiveId
      }
    ]);
  }),

  http.post('/api/hives/:hiveId/messages', async ({ request, params }) => {
    const { content } = await request.json() as { content: string };
    
    return HttpResponse.json({
      id: 'msg-' + Date.now(),
      content,
      userId: mockUser.id,
      username: mockUser.username,
      timestamp: new Date(),
      hiveId: params.hiveId
    }, { status: 201 });
  })
];