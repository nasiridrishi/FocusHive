import { http, HttpResponse } from 'msw';
import type { User, LoginResponse, RegisterResponse, PasswordResetResponse } from '@shared/types/auth';

// Extended user type for testing that includes optional displayName
type TestUser = User & {
  displayName?: string;
};

// Mock users database for different test scenarios
const mockUsers = new Map<string, User>();

// Mock session storage to track tokens in tests
const mockTokenStorage = new Map<string, { token: string; user: User }>();

// Default test user
const defaultMockUser: User = {
  id: '1',
  username: 'testuser',
  email: 'testuser@example.com',
  firstName: 'Test',
  lastName: 'User',
  name: 'Test User',
  avatar: null,
  profilePicture: null,
  isEmailVerified: true,
  isVerified: true,
  createdAt: new Date('2024-01-01').toISOString(),
  updatedAt: new Date('2024-01-01').toISOString()
};

// Initialize with default user
mockUsers.set('testuser@example.com', defaultMockUser);
mockUsers.set('test@example.com', { ...defaultMockUser, email: 'test@example.com' });

// Mock tokens
const generateMockTokens = (user: User) => {
  const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJ0ZXN0dXNlckBleGFtcGxlLmNvbSIsImlhdCI6MTcwNjc4NDAwMCwiZXhwIjoxNzM4MzIwMDAwfQ.mock-access-token';
  const refreshToken = 'mock-refresh-token-12345';
  
  // Store token association for /me endpoint
  mockTokenStorage.set(token, { token, user });
  
  return { token, refreshToken };
};

// Helper to get user from auth header
const getUserFromAuthHeader = (authHeader: string | null): User | null => {
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return null;
  }
  
  const token = authHeader.replace('Bearer ', '');
  const session = mockTokenStorage.get(token);
  return session?.user || defaultMockUser; // Fallback for tests
};

// Mock hive data for testing
const mockHives = [
  {
    id: '1',
    name: 'Study Marathon',
    description: 'A focused environment for intensive study sessions',
    ownerId: '1',
    owner: {
      id: '1',
      username: 'studymaster',
      email: 'study@example.com',
      firstName: 'Study',
      lastName: 'Master',
      name: 'Study Master',
      avatar: null,
      profilePicture: null,
      isEmailVerified: true,
      isVerified: true,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    },
    maxMembers: 20,
    isPublic: true,
    tags: ['study', 'academic', 'focus'],
    settings: {
      allowChat: true,
      allowVoice: false,
      requireApproval: false,
      focusMode: 'pomodoro',
      defaultSessionLength: 25,
      maxSessionLength: 120
    },
    currentMembers: 8,
    memberCount: 8,
    isOwner: false,
    isMember: false,
    createdAt: new Date('2024-01-01').toISOString(),
    updatedAt: new Date('2024-01-01').toISOString()
  },
  {
    id: '2',
    name: 'Programming Hub',
    description: 'Code together, debug together, learn together',
    ownerId: '2',
    owner: {
      id: '2',
      username: 'codemaster',
      email: 'code@example.com',
      firstName: 'Code',
      lastName: 'Master',
      name: 'Code Master',
      avatar: null,
      profilePicture: null,
      isEmailVerified: true,
      isVerified: true,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    },
    maxMembers: 15,
    isPublic: true,
    tags: ['programming', 'coding', 'development'],
    settings: {
      allowChat: true,
      allowVoice: true,
      requireApproval: false,
      focusMode: 'continuous',
      defaultSessionLength: 50,
      maxSessionLength: 180
    },
    currentMembers: 12,
    memberCount: 12,
    isOwner: false,
    isMember: true,
    createdAt: new Date('2024-01-02').toISOString(),
    updatedAt: new Date('2024-01-02').toISOString()
  },
  {
    id: '3',
    name: 'Private Writing Circle',
    description: 'A quiet space for writers and content creators',
    ownerId: '1',
    owner: {
      id: '1',
      username: 'studymaster',
      email: 'study@example.com',
      firstName: 'Study',
      lastName: 'Master',
      name: 'Study Master',
      avatar: null,
      profilePicture: null,
      isEmailVerified: true,
      isVerified: true,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    },
    maxMembers: 8,
    isPublic: false,
    tags: ['writing', 'creative', 'quiet'],
    settings: {
      allowChat: false,
      allowVoice: false,
      requireApproval: true,
      focusMode: 'flexible',
      defaultSessionLength: 60,
      maxSessionLength: 240
    },
    currentMembers: 5,
    memberCount: 5,
    isOwner: true,
    isMember: true,
    createdAt: new Date('2024-01-03').toISOString(),
    updatedAt: new Date('2024-01-03').toISOString()
  },
  {
    id: '4',
    name: 'Full Capacity Hive',
    description: 'This hive is at full capacity',
    ownerId: '2',
    owner: {
      id: '2',
      username: 'codemaster',
      email: 'code@example.com',
      firstName: 'Code',
      lastName: 'Master',
      name: 'Code Master',
      avatar: null,
      profilePicture: null,
      isEmailVerified: true,
      isVerified: true,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    },
    maxMembers: 5,
    isPublic: true,
    tags: ['full', 'busy'],
    settings: {
      allowChat: true,
      allowVoice: false,
      requireApproval: false,
      focusMode: 'pomodoro',
      defaultSessionLength: 25,
      maxSessionLength: 60
    },
    currentMembers: 5,
    memberCount: 5,
    isOwner: false,
    isMember: false,
    createdAt: new Date('2024-01-04').toISOString(),
    updatedAt: new Date('2024-01-04').toISOString()
  }
];

// Mock members data for testing
const mockMembers = {
  '1': [
    {
      id: 'member1',
      userId: '1',
      user: defaultMockUser,
      hiveId: '1',
      role: 'owner' as const,
      joinedAt: '2024-01-01T00:00:00Z',
      isActive: true,
      permissions: {
        canInviteMembers: true,
        canModerateChat: true,
        canManageSettings: true,
        canStartTimers: true
      }
    },
    {
      id: 'member2',
      userId: 'user2',
      user: {
        id: 'user2',
        username: 'activeuser',
        email: 'active@example.com',
        firstName: 'Active',
        lastName: 'User',
        name: 'Active User',
        avatar: null,
        profilePicture: null,
        isEmailVerified: true,
        isVerified: true,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      },
      hiveId: '1',
      role: 'member' as const,
      joinedAt: '2024-01-02T00:00:00Z',
      isActive: true,
      permissions: {
        canInviteMembers: false,
        canModerateChat: false,
        canManageSettings: false,
        canStartTimers: false
      }
    }
  ],
  '2': [
    {
      id: 'member3',
      userId: '1',
      user: defaultMockUser,
      hiveId: '2',
      role: 'member' as const,
      joinedAt: '2024-01-02T00:00:00Z',
      isActive: true,
      permissions: {
        canInviteMembers: false,
        canModerateChat: false,
        canManageSettings: false,
        canStartTimers: false
      }
    }
  ]
};

export const handlers = [
  // ====================================
  // IDENTITY SERVICE AUTH ENDPOINTS
  // ====================================
  
  // OPTIONS requests for CORS preflight
  http.options('http://localhost:8081/api/v1/auth/*', () => {
    return new HttpResponse(null, {
      status: 204,
      headers: {
        'Access-Control-Allow-Origin': 'http://localhost:3000',
        'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type, Authorization',
        'Access-Control-Allow-Credentials': 'true'
      }
    });
  }),

  // LOGIN
  http.post('http://localhost:8081/api/v1/auth/login', async ({ request }) => {
    const body = await request.json() as { email: string; password: string };
    const { email, password } = body;

    // Handle invalid credentials
    if (email === 'wronguser@example.com' || password === 'wrongpassword') {
      return HttpResponse.json(
        { message: 'Invalid credentials' },
        { status: 401 }
      );
    }

    // Find user by email
    const user = mockUsers.get(email) || {
      ...defaultMockUser,
      email,
      id: Math.random().toString(36).substr(2, 9)
    };

    const tokens = generateMockTokens(user);
    const response: LoginResponse = {
      user,
      ...tokens
    };

    return HttpResponse.json(response);
  }),

  // REGISTER  
  http.post('http://localhost:8081/api/v1/auth/register', async ({ request }) => {
    const body = await request.json() as { 
      email: string; 
      username: string; 
      firstName: string; 
      lastName: string; 
      password: string;
    };

    // Handle validation errors
    if (body.email === 'existing@example.com') {
      return HttpResponse.json(
        { message: 'Email already exists' },
        { status: 400 }
      );
    }

    if (!body.email || !body.username || !body.firstName || !body.lastName) {
      return HttpResponse.json(
        { message: 'Invalid registration data' },
        { status: 400 }
      );
    }

    const newUser: User = {
      id: Math.random().toString(36).substr(2, 9),
      email: body.email,
      username: body.username,
      firstName: body.firstName,
      lastName: body.lastName,
      name: `${body.firstName} ${body.lastName}`,
      avatar: null,
      profilePicture: null,
      isEmailVerified: false,
      isVerified: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    // Add displayName property for compatibility
    (newUser as TestUser).displayName = body.username;

    // Store new user for future requests
    mockUsers.set(body.email, newUser);

    const tokens = generateMockTokens(newUser);
    const response: RegisterResponse = {
      user: newUser,
      ...tokens
    };

    return HttpResponse.json(response);
  }),

  // LOGOUT
  http.post('http://localhost:8081/api/v1/auth/logout', () => {
    return HttpResponse.json({ message: 'Logged out successfully' });
  }),

  // GET CURRENT USER (/me)
  http.get('http://localhost:8081/api/v1/auth/me', ({ request }) => {
    const authHeader = request.headers.get('authorization');
    const user = getUserFromAuthHeader(authHeader);
    
    if (!user) {
      return HttpResponse.json(
        { message: 'Unauthorized' },
        { status: 401 }
      );
    }

    return HttpResponse.json({ 
      user 
    });
  }),

  // UPDATE PROFILE
  http.put('http://localhost:8081/api/v1/auth/profile', async ({ request }) => {
    const authHeader = request.headers.get('authorization');
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(
        { message: 'Unauthorized' },
        { status: 401 }
      );
    }

    const updates = await request.json() as Partial<User>;
    const updatedUser: User = {
      ...defaultMockUser,
      ...updates,
      updatedAt: new Date().toISOString()
    };

    return HttpResponse.json({ 
      user: updatedUser 
    });
  }),

  // CHANGE PASSWORD
  http.put('http://localhost:8081/api/v1/auth/change-password', async ({ request }) => {
    const authHeader = request.headers.get('authorization');
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(
        { message: 'Unauthorized' },
        { status: 401 }
      );
    }

    const body = await request.json() as { currentPassword: string; newPassword: string };
    
    if (body.currentPassword === 'wrongpassword') {
      return HttpResponse.json(
        { message: 'Current password is incorrect' },
        { status: 400 }
      );
    }

    return HttpResponse.json({ message: 'Password changed successfully' });
  }),

  // FORGOT PASSWORD
  http.post('http://localhost:8081/api/v1/auth/forgot-password', async ({ request }) => {
    const body = await request.json() as { email: string };

    if (!body.email) {
      return HttpResponse.json(
        { message: 'Email is required' },
        { status: 400 }
      );
    }

    const response: PasswordResetResponse = {
      message: 'If an account with that email exists, we have sent you a password reset email.'
    };

    return HttpResponse.json(response);
  }),

  // REFRESH TOKEN
  http.post('http://localhost:8081/api/v1/auth/refresh', async ({ request }) => {
    const body = await request.json() as { refreshToken: string };

    if (!body.refreshToken) {
      return HttpResponse.json(
        { message: 'Refresh token is required' },
        { status: 400 }
      );
    }

    if (body.refreshToken === 'invalid-refresh-token') {
      return HttpResponse.json(
        { message: 'Invalid refresh token' },
        { status: 401 }
      );
    }

    const tokens = generateMockTokens(defaultMockUser);
    return HttpResponse.json(tokens);
  }),

  // ====================================
  // LEGACY AUTH ENDPOINTS (for backward compatibility and test overrides)
  // ====================================
  
  http.post('/api/auth/login', async ({ request }) => {
    const body = await request.json() as { email: string; password: string };
    const { email } = body;
    
    // Find user by email
    const user = mockUsers.get(email) || {
      ...defaultMockUser,
      email,
      id: Math.random().toString(36).substr(2, 9)
    };

    const tokens = generateMockTokens(user);
    const response: LoginResponse = {
      user,
      ...tokens
    };

    return HttpResponse.json(response);
  }),

  http.post('/api/auth/register', async ({ request }) => {
    const body = await request.json() as { 
      email: string; 
      username: string; 
      firstName: string; 
      lastName: string; 
      password: string;
    };

    const newUser: User = {
      id: Math.random().toString(36).substr(2, 9),
      email: body.email,
      username: body.username,
      firstName: body.firstName,
      lastName: body.lastName,
      name: `${body.firstName} ${body.lastName}`,
      avatar: null,
      profilePicture: null,
      isEmailVerified: false,
      isVerified: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    // For registration tests that expect displayName fallback
    if (!body.firstName && !body.lastName) {
      (newUser as TestUser).displayName = body.username;
    }

    const tokens = generateMockTokens(newUser);
    const response: RegisterResponse = {
      user: newUser,
      ...tokens
    };

    return HttpResponse.json(response);
  }),

  // ====================================
  // BACKEND API ENDPOINTS (Port 8080)
  // ====================================
  
  http.get('/api/hives', ({ request }) => {
    const url = new URL(request.url);
    const searchQuery = url.searchParams.get('search')?.toLowerCase();
    const category = url.searchParams.get('category');
    const tags = url.searchParams.get('tags')?.split(',') || [];
    const sortBy = url.searchParams.get('sortBy') || 'activity';
    
    let filteredHives = [...mockHives];
    
    // Apply search filter
    if (searchQuery) {
      filteredHives = filteredHives.filter(hive => 
        hive.name.toLowerCase().includes(searchQuery) ||
        hive.description.toLowerCase().includes(searchQuery) ||
        hive.tags.some(tag => tag.toLowerCase().includes(searchQuery))
      );
    }
    
    // Apply category filter
    if (category && category !== 'all') {
      const currentUserId = '1'; // Mock current user
      filteredHives = filteredHives.filter(hive => {
        const isMember = mockMembers[hive.id]?.some(m => m.userId === currentUserId);
        
        switch (category) {
          case 'public':
            return hive.isPublic;
          case 'private':
            return !hive.isPublic;
          case 'joined':
            return isMember;
          case 'available':
            return !isMember && hive.currentMembers < hive.maxMembers;
          default:
            return true;
        }
      });
    }
    
    // Apply tags filter
    if (tags.length > 0) {
      filteredHives = filteredHives.filter(hive => 
        tags.some(tag => hive.tags.includes(tag))
      );
    }
    
    // Apply sorting
    filteredHives.sort((a, b) => {
      switch (sortBy) {
        case 'name':
          return a.name.localeCompare(b.name);
        case 'members':
          return b.currentMembers - a.currentMembers;
        case 'created':
          return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
        case 'activity':
        default: {
          // Sort by online members, then by total members
          const aOnline = mockMembers[a.id]?.filter(m => m.isActive).length || 0;
          const bOnline = mockMembers[b.id]?.filter(m => m.isActive).length || 0;
          if (aOnline !== bOnline) return bOnline - aOnline;
          return b.currentMembers - a.currentMembers;
        }
      }
    });
    
    return HttpResponse.json({
      hives: filteredHives,
      total: filteredHives.length,
      page: 1,
      size: 20
    });
  }),

  http.get('/api/hives/:id/members', ({ params }) => {
    const hiveId = params.id as string;
    const members = mockMembers[hiveId] || [];
    return HttpResponse.json(members);
  }),

  http.post('/api/hives', async ({ request }) => {
    const body = await request.json() as {
      name: string;
      description: string;
      maxMembers: number;
      isPublic: boolean;
      tags: string[];
      settings: {
        allowChat: boolean;
        allowVoice: boolean;
        requireApproval: boolean;
        focusMode: 'pomodoro' | 'continuous' | 'flexible';
        defaultSessionLength: number;
        maxSessionLength: number;
      };
    };
    const newHive = {
      id: `hive-${Date.now()}`,
      name: body.name,
      description: body.description,
      ownerId: '1',
      owner: defaultMockUser,
      maxMembers: body.maxMembers,
      isPublic: body.isPublic,
      tags: body.tags,
      settings: body.settings,
      currentMembers: 1,
      memberCount: 1,
      isOwner: true,
      isMember: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    return HttpResponse.json(newHive, { status: 201 });
  }),

  http.post('/api/hives/:id/join', ({ params }) => {
    const hiveId = params.id as string;
    return HttpResponse.json({ message: 'Successfully joined hive' });
  }),

  http.delete('/api/hives/:id/leave', ({ params }) => {
    const hiveId = params.id as string;
    return HttpResponse.json({ message: 'Successfully left hive' });
  }),

  // ====================================
  // ERROR SCENARIOS FOR TESTING
  // ====================================
  
  // Network error simulation
  http.post('http://localhost:8081/api/v1/auth/network-error', () => {
    return HttpResponse.error();
  }),

  // Timeout simulation  
  http.post('http://localhost:8081/api/v1/auth/timeout', async () => {
    await new Promise(resolve => setTimeout(resolve, 10000));
    return HttpResponse.json({});
  }),

  // Malformed JSON response
  http.post('http://localhost:8081/api/v1/auth/malformed', () => {
    return new Response('Invalid JSON{', {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    });
  }),

  // Empty response
  http.post('http://localhost:8081/api/v1/auth/empty', () => {
    return new Response('', { status: 200 });
  })
];
