import { User, Room } from '@focushive/shared';
import { dataStore } from '../../data/store';
import { authService } from '../../services/authService';

export const createTestUser = async (overrides: Partial<User> = {}): Promise<{ user: User; token: string }> => {
  const defaultUser = {
    email: `test${Date.now()}@example.com`,
    username: `testuser${Date.now()}`,
    password: 'password123'
  };
  
  const { user, token } = await authService.register(
    overrides.email || defaultUser.email,
    overrides.username || defaultUser.username,
    defaultUser.password
  );
  
  return { user, token };
};

export const createTestRoom = (ownerId: string, overrides: Partial<Room> = {}): Room => {
  const room: Room = {
    id: `room-${Date.now()}`,
    name: overrides.name || 'Test Room',
    description: overrides.description || 'A test room',
    type: overrides.type || 'public',
    maxParticipants: overrides.maxParticipants || 10,
    ownerId,
    isPublic: overrides.type !== 'private',
    tags: overrides.tags || [],
    participants: [ownerId],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    focusType: overrides.focusType || 'deepWork',
    password: overrides.password
  };
  
  return dataStore.createRoom(room);
};

export const authenticatedRequest = (request: any, token: string) => {
  return request.set('Authorization', `Bearer ${token}`);
};