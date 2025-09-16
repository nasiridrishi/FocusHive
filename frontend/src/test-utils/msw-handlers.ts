import {http, HttpResponse} from 'msw';
import type {
  LoginResponse,
  PasswordResetResponse,
  RegisterResponse,
  User
} from '@shared/types/auth';

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
mockUsers.set('test@example.com', {...defaultMockUser, email: 'test@example.com'});

// Mock tokens
const generateMockTokens = (user: User): { token: string; refreshToken: string } => {
  const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJ0ZXN0dXNlckBleGFtcGxlLmNvbSIsImlhdCI6MTcwNjc4NDAwMCwiZXhwIjoxNzM4MzIwMDAwfQ.mock-access-token';
  const refreshToken = 'mock-refresh-token-12345';

  // Store token association for /me endpoint
  mockTokenStorage.set(token, {token, user});

  return {token, refreshToken};
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
  http.post('http://localhost:8081/api/v1/auth/login', async ({request}) => {
    const body = await request.json() as { email: string; password: string };
    const {email, password} = body;

    // Handle invalid credentials
    if (email === 'wronguser@example.com' || password === 'wrongpassword') {
      return HttpResponse.json(
          {message: 'Invalid credentials'},
          {status: 401}
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
  http.post('http://localhost:8081/api/v1/auth/register', async ({request}) => {
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
          {message: 'Email already exists'},
          {status: 400}
      );
    }

    if (!body.email || !body.username || !body.firstName || !body.lastName) {
      return HttpResponse.json(
          {message: 'Invalid registration data'},
          {status: 400}
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
    return HttpResponse.json({message: 'Logged out successfully'});
  }),

  // GET CURRENT USER (/me)
  http.get('http://localhost:8081/api/v1/auth/me', ({request}) => {
    const authHeader = request.headers.get('authorization');
    const user = getUserFromAuthHeader(authHeader);

    if (!user) {
      return HttpResponse.json(
          {message: 'Unauthorized'},
          {status: 401}
      );
    }

    return HttpResponse.json({
      user
    });
  }),

  // UPDATE PROFILE
  http.put('http://localhost:8081/api/v1/auth/profile', async ({request}) => {
    const authHeader = request.headers.get('authorization');

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(
          {message: 'Unauthorized'},
          {status: 401}
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
  http.put('http://localhost:8081/api/v1/auth/change-password', async ({request}) => {
    const authHeader = request.headers.get('authorization');

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(
          {message: 'Unauthorized'},
          {status: 401}
      );
    }

    const body = await request.json() as { currentPassword: string; newPassword: string };

    if (body.currentPassword === 'wrongpassword') {
      return HttpResponse.json(
          {message: 'Current password is incorrect'},
          {status: 400}
      );
    }

    return HttpResponse.json({message: 'Password changed successfully'});
  }),

  // FORGOT PASSWORD
  http.post('http://localhost:8081/api/v1/auth/forgot-password', async ({request}) => {
    const body = await request.json() as { email: string };

    if (!body.email) {
      return HttpResponse.json(
          {message: 'Email is required'},
          {status: 400}
      );
    }

    const response: PasswordResetResponse = {
      message: 'If an account with that email exists, we have sent you a password reset email.'
    };

    return HttpResponse.json(response);
  }),

  // REFRESH TOKEN
  http.post('http://localhost:8081/api/v1/auth/refresh', async ({request}) => {
    const body = await request.json() as { refreshToken: string };

    if (!body.refreshToken) {
      return HttpResponse.json(
          {message: 'Refresh token is required'},
          {status: 400}
      );
    }

    if (body.refreshToken === 'invalid-refresh-token') {
      return HttpResponse.json(
          {message: 'Invalid refresh token'},
          {status: 401}
      );
    }

    const tokens = generateMockTokens(defaultMockUser);
    return HttpResponse.json(tokens);
  }),

  // ====================================
  // LEGACY AUTH ENDPOINTS (for backward compatibility and test overrides)
  // ====================================

  http.post('/api/auth/login', async ({request}) => {
    const body = await request.json() as { email: string; password: string };
    const {email} = body;

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

  http.post('/api/auth/register', async ({request}) => {
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

  // Get hive by ID
  http.get('http://localhost:8080/api/v1/hives/:id', ({params}) => {
    const hiveId = params.id as string;
    const hive = mockHives.find(h => h.id === hiveId.toString());

    if (!hive) {
      return HttpResponse.json(
        { message: 'Hive not found' },
        { status: 404 }
      );
    }

    return HttpResponse.json(hive);
  }),

  // Get hive members
  http.get('http://localhost:8080/api/v1/hives/:id/members', ({params}) => {
    const hiveId = params.id as string;
    const members = mockMembers[hiveId] || [];

    // Return just the user objects for the test
    return HttpResponse.json(members.map(m => m.user));
  }),

  http.get('/api/hives', ({request}) => {
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

  http.get('/api/hives/:id/members', ({params}) => {
    const hiveId = params.id as string;
    const members = mockMembers[hiveId] || [];
    return HttpResponse.json(members);
  }),

  http.post('/api/hives', async ({request}) => {
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
    return HttpResponse.json(newHive, {status: 201});
  }),

  http.post('/api/hives/:id/join', ({params}) => {
    const _hiveId = params.id as string;
    return HttpResponse.json({message: 'Successfully joined hive'});
  }),

  http.delete('/api/hives/:id/leave', ({params}) => {
    const _hiveId = params.id as string;
    return HttpResponse.json({message: 'Successfully left hive'});
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
      headers: {'Content-Type': 'application/json'}
    });
  }),

  // Empty response
  http.post('http://localhost:8081/api/v1/auth/empty', () => {
    return new Response('', {status: 200});
  }),

  // ====================================
  // FORUM ENDPOINTS
  // ====================================

  // Get all posts
  http.get('http://localhost:8080/api/v1/forum/posts', () => {
    return HttpResponse.json({
      content: [
        {
          id: 1,
          userId: 1,
          title: 'Test Post',
          content: 'Test content',
          postType: 'discussion',
          status: 'published',
          categoryId: 1,
          tags: ['test'],
          views: 10,
          upvotes: 5,
          downvotes: 1,
          replyCount: 3,
          isLocked: false,
          isPinned: false,
          isFeatured: false,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }
      ],
      totalPages: 1,
      totalElements: 1,
      number: 0,
      size: 20,
    });
  }),

  // Create post
  http.post('http://localhost:8080/api/v1/forum/posts', async ({request}) => {
    const body = await request.json() as any;
    return HttpResponse.json({
      id: 1,
      ...body,
      userId: 1,
      status: 'published',
      views: 0,
      upvotes: 0,
      downvotes: 0,
      replyCount: 0,
      isLocked: false,
      isPinned: false,
      isFeatured: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    });
  }),

  // Get post by ID
  http.get('http://localhost:8080/api/v1/forum/posts/:id', ({params}) => {
    const id = Number(params.id);
    return HttpResponse.json({
      id,
      userId: 1,
      title: 'Test Post',
      content: 'Test content',
      postType: 'discussion',
      status: 'published',
      categoryId: 1,
      tags: ['test'],
      views: 10,
      upvotes: 5,
      downvotes: 1,
      replyCount: 3,
      isLocked: false,
      isPinned: false,
      isFeatured: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    });
  }),

  // Update post
  http.put('http://localhost:8080/api/v1/forum/posts/:id', async ({params, request}) => {
    const id = Number(params.id);
    const body = await request.json() as any;
    return HttpResponse.json({
      id,
      ...body,
      updatedAt: new Date().toISOString(),
    });
  }),

  // Delete post
  http.delete('http://localhost:8080/api/v1/forum/posts/:id', () => {
    return new Response(null, { status: 204 });
  }),

  // Get replies
  http.get('http://localhost:8080/api/v1/forum/posts/:postId/replies', ({params}) => {
    const postId = Number(params.postId);
    return HttpResponse.json({
      content: [
        {
          id: 1,
          postId,
          userId: 2,
          parentReplyId: null,
          content: 'Test reply',
          upvotes: 2,
          downvotes: 0,
          isAccepted: false,
          isDeleted: false,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }
      ],
      totalPages: 1,
      totalElements: 1,
      number: 0,
      size: 20,
    });
  }),

  // Create reply
  http.post('http://localhost:8080/api/v1/forum/posts/:postId/replies', async ({params, request}) => {
    const postId = Number(params.postId);
    const body = await request.json() as any;
    return HttpResponse.json({
      id: 1,
      postId,
      userId: 1,
      ...body,
      upvotes: 0,
      downvotes: 0,
      isAccepted: false,
      isDeleted: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    });
  }),

  // Add reaction
  http.post('http://localhost:8080/api/v1/forum/posts/:id/reactions', async ({params, request}) => {
    const id = Number(params.id);
    const body = await request.json() as any;
    return HttpResponse.json({
      postId: id,
      ...body,
      userId: 1,
      createdAt: new Date().toISOString(),
    });
  }),

  // Remove reaction
  http.delete('http://localhost:8080/api/v1/forum/posts/:id/reactions/:reactionType', () => {
    return new Response(null, { status: 204 });
  }),

  // Search posts
  http.get('http://localhost:8080/api/v1/forum/search', () => {
    return HttpResponse.json({
      content: [
        {
          id: 1,
          userId: 1,
          title: 'Search Result',
          content: 'Matching content',
          postType: 'discussion',
          status: 'published',
          categoryId: 1,
          tags: ['search'],
          views: 5,
          upvotes: 2,
          downvotes: 0,
          replyCount: 1,
          isLocked: false,
          isPinned: false,
          isFeatured: false,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }
      ],
      totalPages: 1,
      totalElements: 1,
      number: 0,
      size: 20,
    });
  }),

  // Get user posts
  http.get('http://localhost:8080/api/v1/forum/users/:userId/posts', ({params}) => {
    const userId = Number(params.userId);
    return HttpResponse.json({
      content: [
        {
          id: 1,
          userId,
          title: 'User Post',
          content: 'User content',
          postType: 'discussion',
          status: 'published',
          categoryId: 1,
          tags: ['user'],
          views: 3,
          upvotes: 1,
          downvotes: 0,
          replyCount: 0,
          isLocked: false,
          isPinned: false,
          isFeatured: false,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }
      ],
      totalPages: 1,
      totalElements: 1,
      number: 0,
      size: 20,
    });
  }),

  // Get categories
  http.get('http://localhost:8080/api/v1/forum/categories', () => {
    return HttpResponse.json([
      {
        id: 1,
        name: 'General',
        slug: 'general',
        description: 'General discussions',
        parentId: null,
        postCount: 10,
        displayOrder: 1,
        isActive: true,
      }
    ]);
  }),

  // Get tags
  http.get('http://localhost:8080/api/v1/forum/tags', () => {
    return HttpResponse.json({
      content: [
        {
          id: 1,
          name: 'javascript',
          slug: 'javascript',
          postCount: 5,
        }
      ],
      totalPages: 1,
      totalElements: 1,
      number: 0,
      size: 20,
    });
  }),

  // Get user profile
  http.get('http://localhost:8080/api/v1/forum/users/:userId', ({params}) => {
    const userId = Number(params.userId);
    return HttpResponse.json({
      userId,
      reputation: 100,
      postCount: 10,
      replyCount: 20,
      helpfulAnswers: 5,
      badges: [],
      joinedDate: new Date().toISOString(),
    });
  }),

  // Update user preferences
  http.put('http://localhost:8080/api/v1/forum/users/:userId/preferences', async ({request}) => {
    const body = await request.json() as any;
    return HttpResponse.json({
      ...body,
      updatedAt: new Date().toISOString(),
    });
  }),

  // Report post
  http.post('http://localhost:8080/api/v1/forum/posts/:id/report', async ({request}) => {
    const body = await request.json() as any;
    return HttpResponse.json({
      ...body,
      reportedAt: new Date().toISOString(),
    });
  }),

  // Get moderation queue
  http.get('http://localhost:8080/api/v1/forum/moderation/queue', () => {
    return HttpResponse.json({
      content: [],
      totalPages: 0,
      totalElements: 0,
      number: 0,
      size: 20,
    });
  }),

  // Approve post
  http.post('http://localhost:8080/api/v1/forum/moderation/posts/:id/approve', () => {
    return HttpResponse.json({
      approved: true,
      approvedAt: new Date().toISOString(),
    });
  }),

  // Lock post
  http.post('http://localhost:8080/api/v1/forum/posts/:id/lock', () => {
    return HttpResponse.json({
      isLocked: true,
      lockedAt: new Date().toISOString(),
    });
  }),

  // Pin post
  http.post('http://localhost:8080/api/v1/forum/posts/:id/pin', () => {
    return HttpResponse.json({
      isPinned: true,
      pinnedAt: new Date().toISOString(),
    });
  }),

  // Get drafts
  http.get('http://localhost:8080/api/v1/forum/drafts', () => {
    return HttpResponse.json({
      content: [],
      totalPages: 0,
      totalElements: 0,
      number: 0,
      size: 20,
    });
  }),

  // Save draft
  http.post('http://localhost:8080/api/v1/forum/drafts', async ({request}) => {
    const body = await request.json() as any;
    return HttpResponse.json({
      id: 1,
      ...body,
      status: 'draft',
      createdAt: new Date().toISOString(),
    });
  }),

  // Update draft
  http.put('http://localhost:8080/api/v1/forum/drafts/:id', async ({params, request}) => {
    const id = Number(params.id);
    const body = await request.json() as any;
    return HttpResponse.json({
      id,
      ...body,
      updatedAt: new Date().toISOString(),
    });
  }),

  // Delete draft
  http.delete('http://localhost:8080/api/v1/forum/drafts/:id', () => {
    return new Response(null, { status: 204 });
  }),

  // ====================================
  // BUDDY SERVICE ENDPOINTS
  // ====================================

  // Get current user buddy profile
  http.get('http://localhost:8087/api/v1/buddy/profile/me', () => {
    return HttpResponse.json({
      userId: 1,
      username: 'testuser',
      avatarUrl: null,
      bio: 'Test user bio',
      interests: ['coding', 'reading'],
      goals: ['productivity', 'learning'],
      preferredSessionTypes: ['focus', 'study'],
      timezone: 'UTC',
      languages: ['en'],
      rating: 4.5,
      totalSessions: 10,
      completionRate: 90,
      isVerified: true,
      availability: {
        monday: [{ startTime: '09:00', endTime: '12:00', sessionTypes: ['work'] }],
        tuesday: [],
        wednesday: [],
        thursday: [],
        friday: [],
        saturday: [],
        sunday: [],
      },
      preferences: {
        autoAccept: true,
        matchingPreference: 'goals',
        notifyOnMatch: true,
        sessionReminders: false,
      },
      createdAt: new Date().toISOString(),
      lastActiveAt: new Date().toISOString(),
    });
  }),

  // Get buddy profile by ID
  http.get('http://localhost:8087/api/v1/buddy/profiles/:userId', ({ params }) => {
    const userId = Number(params.userId);
    return HttpResponse.json({
      userId,
      username: `user${userId}`,
      rating: 4.2,
      totalSessions: 5,
      completionRate: 85,
      interests: ['programming'],
      goals: ['focus'],
      preferredSessionTypes: ['work'],
      timezone: 'UTC',
      languages: ['en'],
      isVerified: false,
      createdAt: new Date().toISOString(),
      lastActiveAt: new Date().toISOString(),
    });
  }),

  // Update preferences
  http.put('http://localhost:8087/api/v1/buddy/profile/preferences', async ({ request }) => {
    const body = await request.json() as any;
    return HttpResponse.json(body);
  }),

  // Update availability
  http.put('http://localhost:8087/api/v1/buddy/profile/availability', async ({ request }) => {
    const body = await request.json() as any;
    return HttpResponse.json(body);
  }),

  // Request match
  http.post('http://localhost:8087/api/v1/buddy/matches/request', async ({ request }) => {
    const body = await request.json() as any;
    return HttpResponse.json({
      id: 'match-' + Date.now(),
      requesterId: body.userId,
      partnerId: 2,
      status: 'pending',
      sessionType: body.sessionType,
      sessionGoal: body.sessionGoal,
      duration: body.duration,
      matchedAt: new Date().toISOString(),
      expiresAt: new Date(Date.now() + 3600000).toISOString(),
      requester: {
        userId: body.userId,
        username: 'requester',
      },
      partner: {
        userId: 2,
        username: 'partner',
      },
    });
  }),

  // Search buddies
  http.get('http://localhost:8087/api/v1/buddy/buddies/search', () => {
    return HttpResponse.json({
      buddies: [
        {
          userId: 2,
          username: 'buddy1',
          rating: 4.7,
          interests: ['programming', 'math'],
          isVerified: true,
        },
      ],
      pagination: {
        page: 0,
        pageSize: 20,
        total: 1,
        hasMore: false,
      },
    });
  }),

  // Accept match
  http.post('http://localhost:8087/api/v1/buddy/matches/:matchId/accept', ({ params }) => {
    return HttpResponse.json({
      id: params.matchId as string,
      status: 'accepted',
    });
  }),

  // Decline match
  http.post('http://localhost:8087/api/v1/buddy/matches/:matchId/decline', ({ params }) => {
    return HttpResponse.json({
      id: params.matchId as string,
      status: 'declined',
    });
  }),

  // Get pending matches
  http.get('http://localhost:8087/api/v1/buddy/matches/pending', () => {
    return HttpResponse.json([
      {
        id: 'match-pending-1',
        status: 'pending',
        sessionType: 'study',
        expiresAt: new Date(Date.now() + 3600000).toISOString(),
      },
    ]);
  }),

  // Cancel match
  http.post('http://localhost:8087/api/v1/buddy/matches/:matchId/cancel', () => {
    return new Response(null, { status: 204 });
  }),

  // Create session
  http.post('http://localhost:8087/api/v1/buddy/sessions', async ({ request }) => {
    const body = await request.json() as any;
    return HttpResponse.json({
      id: 'session-' + Date.now(),
      matchId: body.matchId,
      status: 'scheduled',
      sessionType: body.sessionType,
      goal: body.goal,
      plannedDuration: body.duration,
      participants: [],
      checkIns: [],
    });
  }),

  // Start session
  http.post('http://localhost:8087/api/v1/buddy/sessions/:sessionId/start', ({ params }) => {
    return HttpResponse.json({
      id: params.sessionId,
      status: 'active',
      startedAt: new Date().toISOString(),
    });
  }),

  // Pause session
  http.post('http://localhost:8087/api/v1/buddy/sessions/:sessionId/pause', ({ params }) => {
    return HttpResponse.json({
      id: params.sessionId,
      status: 'paused',
      pausedAt: new Date().toISOString(),
    });
  }),

  // Resume session
  http.post('http://localhost:8087/api/v1/buddy/sessions/:sessionId/resume', ({ params }) => {
    return HttpResponse.json({
      id: params.sessionId,
      status: 'active',
      pausedAt: null,
    });
  }),

  // End session
  http.post('http://localhost:8087/api/v1/buddy/sessions/:sessionId/end', ({ params }) => {
    return HttpResponse.json({
      id: params.sessionId,
      status: 'completed',
      endedAt: new Date().toISOString(),
      actualDuration: 60,
    });
  }),

  // Check in
  http.post('http://localhost:8087/api/v1/buddy/sessions/:sessionId/checkin', async ({ request }) => {
    const body = await request.json() as any;
    return HttpResponse.json({
      id: 'checkin-' + Date.now(),
      ...body,
      timestamp: new Date().toISOString(),
    });
  }),

  // Get active sessions
  http.get('http://localhost:8087/api/v1/buddy/sessions/active', () => {
    return HttpResponse.json([
      {
        id: 'session-active-1',
        status: 'active',
      },
      {
        id: 'session-paused-1',
        status: 'paused',
      },
    ]);
  }),

  // Get session history
  http.get('http://localhost:8087/api/v1/buddy/sessions/history', () => {
    return HttpResponse.json([
      {
        id: 'session-completed-1',
        status: 'completed',
      },
      {
        id: 'session-completed-2',
        status: 'completed',
      },
    ]);
  }),

  // Rate session
  http.post('http://localhost:8087/api/v1/buddy/sessions/:sessionId/rate', async ({ params, request }) => {
    const body = await request.json() as any;
    return HttpResponse.json({
      sessionId: params.sessionId,
      ...body,
      raterId: 1,
      partnerId: 2,
      createdAt: new Date().toISOString(),
    });
  }),

  // Get stats
  http.get('http://localhost:8087/api/v1/buddy/stats/me', () => {
    return HttpResponse.json({
      userId: 1,
      totalSessions: 10,
      totalHours: 20,
      averageSessionLength: 120,
      completionRate: 90,
      averageRating: 4.5,
      totalBuddies: 5,
      repeatBuddies: 2,
      currentStreak: 3,
      longestStreak: 7,
      favoriteSessionType: 'focus',
      mostProductiveTime: '14:00',
      badges: [],
      recentSessions: [],
      topBuddies: [],
    });
  }),

  // Get top buddies
  http.get('http://localhost:8087/api/v1/buddy/stats/top-buddies', () => {
    return HttpResponse.json([
      {
        userId: 2,
        username: 'topbuddy1',
        sessionCount: 5,
        averageRating: 4.8,
      },
    ]);
  }),

  // Get leaderboard
  http.get('http://localhost:8087/api/v1/buddy/leaderboard/:period', () => {
    return HttpResponse.json([
      {
        userId: 1,
        totalHours: 50,
        totalSessions: 25,
      },
      {
        userId: 2,
        totalHours: 45,
        totalSessions: 20,
      },
    ]);
  }),

  // Send invitation
  http.post('http://localhost:8087/api/v1/buddy/invitations', async ({ request }) => {
    const body = await request.json() as any;
    return HttpResponse.json({
      id: 'inv-' + Date.now(),
      fromUserId: 1,
      toUserId: body.toUserId,
      sessionType: body.sessionType,
      message: body.message,
      status: 'pending',
      expiresAt: new Date(Date.now() + 86400000).toISOString(),
      createdAt: new Date().toISOString(),
    });
  }),

  // Get received invitations
  http.get('http://localhost:8087/api/v1/buddy/invitations/received', () => {
    return HttpResponse.json([
      {
        id: 'inv-received-1',
        status: 'pending',
        expiresAt: new Date(Date.now() + 3600000).toISOString(),
      },
    ]);
  }),

  // Accept invitation
  http.post('http://localhost:8087/api/v1/buddy/invitations/:invId/accept', ({ params }) => {
    return HttpResponse.json({
      id: params.invId,
      status: 'accepted',
    });
  }),

  // Decline invitation
  http.post('http://localhost:8087/api/v1/buddy/invitations/:invId/decline', ({ params }) => {
    return HttpResponse.json({
      id: params.invId,
      status: 'declined',
    });
  }),

  // Send message
  http.post('http://localhost:8087/api/v1/buddy/sessions/:sessionId/messages', async ({ params, request }) => {
    const body = await request.json() as any;
    return HttpResponse.json({
      id: 'msg-' + Date.now(),
      sessionId: params.sessionId as string,
      senderId: 1,
      content: body.content,
      type: body.type,
      createdAt: new Date().toISOString(),
    });
  }),

  // Get session messages
  http.get('http://localhost:8087/api/v1/buddy/sessions/:sessionId/messages', ({ params }) => {
    return HttpResponse.json([
      {
        id: 'msg-1',
        sessionId: params.sessionId,
        senderId: 1,
        content: 'Test message',
        type: 'text',
        createdAt: new Date().toISOString(),
      },
    ]);
  }),

  // Add message reaction
  http.post('http://localhost:8087/api/v1/buddy/messages/:messageId/reactions', async ({ request }) => {
    const body = await request.json() as any;
    return HttpResponse.json({
      id: 'msg-with-reaction',
      reactions: [
        {
          userId: 1,
          emoji: body.emoji,
        },
      ],
    });
  })
];
