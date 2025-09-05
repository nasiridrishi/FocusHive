import { http, HttpResponse } from 'msw';

// Simple mock data without type issues
const mockUser = {
  id: '1',
  username: 'testuser',
  email: 'test@example.com',
  avatar: null,
  createdAt: new Date('2024-01-01').toISOString()
};

const mockHives = [
  {
    id: '1',
    name: 'Test Hive',
    description: 'A test hive for development',
    ownerId: '1',
    isPublic: true,
    memberCount: 5,
    createdAt: new Date('2024-01-01').toISOString(),
    settings: {
      allowChat: true,
      requireApproval: false
    }
  }
];

export const handlers = [
  // Simple auth endpoints
  http.post('/api/auth/login', () => {
    return HttpResponse.json({ user: mockUser, token: 'mock-token' });
  }),

  http.post('/api/auth/register', () => {
    return HttpResponse.json({ user: mockUser, token: 'mock-token' });
  }),

  // Simple hive endpoints
  http.get('/api/hives', () => {
    return HttpResponse.json(mockHives);
  }),

  http.post('/api/hives', () => {
    return HttpResponse.json(mockHives[0], { status: 201 });
  })
];
