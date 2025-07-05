import request from 'supertest';
import express from 'express';
import { roomRoutes } from '../room.routes';
import { createTestUser, authenticatedRequest } from '../../__tests__/utils/testHelpers';
import { dataStore } from '../../data/store';
import { RoomService } from '../../services/roomService';

const app = express();
app.use(express.json());
app.use('/api/rooms', roomRoutes);

describe('Room Routes', () => {
  let testUser: any;
  let testToken: string;
  let roomService: RoomService;

  beforeEach(async () => {
    const result = await createTestUser();
    testUser = result.user;
    testToken = result.token;
    roomService = new RoomService();
  });

  describe('POST /api/rooms', () => {
    it('should create a new room with valid data', async () => {
      const roomData = {
        name: 'API Test Room',
        description: 'Created via API',
        type: 'public',
        maxParticipants: 8,
        tags: ['test', 'api']
      };

      const response = await authenticatedRequest(
        request(app).post('/api/rooms'),
        testToken
      ).send(roomData);

      expect(response.status).toBe(201);
      expect(response.body.room).toBeDefined();
      expect(response.body.room.name).toBe(roomData.name);
      expect(response.body.room.ownerId).toBe(testUser.id);
      expect(response.body.room.participants).toContain(testUser.id);
    });

    it('should require authentication', async () => {
      const response = await request(app)
        .post('/api/rooms')
        .send({
          name: 'Unauthorized Room',
          type: 'public'
        });

      expect(response.status).toBe(401);
    });

    it('should validate required fields', async () => {
      const response = await authenticatedRequest(
        request(app).post('/api/rooms'),
        testToken
      ).send({
        // Missing required fields
        description: 'No name provided'
      });

      expect(response.status).toBe(400);
      expect(response.body.error).toBeDefined();
    });
  });

  describe('GET /api/rooms', () => {
    beforeEach(async () => {
      // Create some test rooms
      await roomService.createRoom(testUser.id, {
        name: 'Public Room 1',
        type: 'public'
      });
      await roomService.createRoom(testUser.id, {
        name: 'Private Room',
        type: 'private',
        password: 'secret'
      });
      await roomService.createRoom(testUser.id, {
        name: 'Public Room 2',
        type: 'public'
      });
    });

    it('should return public rooms without authentication', async () => {
      const response = await request(app).get('/api/rooms');

      expect(response.status).toBe(200);
      expect(response.body.rooms).toBeDefined();
      expect(response.body.rooms.length).toBe(2);
      expect(response.body.rooms.every((r: any) => r.isPublic)).toBe(true);
    });

    it('should not include passwords in response', async () => {
      const response = await request(app).get('/api/rooms');

      expect(response.status).toBe(200);
      response.body.rooms.forEach((room: any) => {
        expect(room.password).toBeUndefined();
      });
    });
  });

  describe('GET /api/rooms/my-rooms', () => {
    it('should return user\'s rooms with authentication', async () => {
      const ownedRoom = await roomService.createRoom(testUser.id, {
        name: 'My Owned Room',
        type: 'public'
      });

      // Create another user and their room
      const otherUser = await createTestUser();
      const otherRoom = await roomService.createRoom(otherUser.user.id, {
        name: 'Other User Room',
        type: 'public'
      });

      // Join the other user's room
      await roomService.joinRoom(testUser.id, otherRoom.id);

      const response = await authenticatedRequest(
        request(app).get('/api/rooms/my-rooms'),
        testToken
      );

      expect(response.status).toBe(200);
      expect(response.body.rooms.length).toBe(2);
      expect(response.body.rooms.map((r: any) => r.id)).toContain(ownedRoom.id);
      expect(response.body.rooms.map((r: any) => r.id)).toContain(otherRoom.id);
    });

    it('should require authentication', async () => {
      const response = await request(app).get('/api/rooms/my-rooms');
      expect(response.status).toBe(401);
    });
  });

  describe('GET /api/rooms/:id', () => {
    it('should return room details', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Detail Test Room',
        description: 'Testing room details',
        type: 'public',
        tags: ['test']
      });

      const response = await request(app).get(`/api/rooms/${room.id}`);

      expect(response.status).toBe(200);
      expect(response.body.room).toBeDefined();
      expect(response.body.room.id).toBe(room.id);
      expect(response.body.room.name).toBe(room.name);
      expect(response.body.room.description).toBe(room.description);
    });

    it('should return 404 for non-existent room', async () => {
      const response = await request(app).get('/api/rooms/non-existent-id');
      expect(response.status).toBe(404);
      expect(response.body.error).toBe('Room not found');
    });

    it('should not include password for private rooms', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Private Detail Room',
        type: 'private',
        password: 'secret'
      });

      const response = await request(app).get(`/api/rooms/${room.id}`);

      expect(response.status).toBe(200);
      expect(response.body.room.password).toBeUndefined();
    });
  });

  describe('POST /api/rooms/:id/join', () => {
    it('should allow joining a public room', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Join Test Room',
        type: 'public',
        maxParticipants: 5
      });

      const otherUser = await createTestUser();

      const response = await authenticatedRequest(
        request(app).post(`/api/rooms/${room.id}/join`),
        otherUser.token
      );

      expect(response.status).toBe(200);
      expect(response.body.room.participants).toContain(otherUser.user.id);
    });

    it('should require password for private rooms', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Private Join Room',
        type: 'private',
        password: 'secret123'
      });

      const otherUser = await createTestUser();

      // Without password
      const response1 = await authenticatedRequest(
        request(app).post(`/api/rooms/${room.id}/join`),
        otherUser.token
      );
      expect(response1.status).toBe(400);

      // With wrong password
      const response2 = await authenticatedRequest(
        request(app).post(`/api/rooms/${room.id}/join`),
        otherUser.token
      ).send({ password: 'wrong' });
      expect(response2.status).toBe(400);

      // With correct password
      const response3 = await authenticatedRequest(
        request(app).post(`/api/rooms/${room.id}/join`),
        otherUser.token
      ).send({ password: 'secret123' });
      expect(response3.status).toBe(200);
    });

    it('should require authentication', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Auth Test Room',
        type: 'public'
      });

      const response = await request(app).post(`/api/rooms/${room.id}/join`);
      expect(response.status).toBe(401);
    });
  });

  describe('POST /api/rooms/:id/leave', () => {
    it('should allow leaving a room', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Leave Test Room',
        type: 'public'
      });

      const otherUser = await createTestUser();
      await roomService.joinRoom(otherUser.user.id, room.id);

      const response = await authenticatedRequest(
        request(app).post(`/api/rooms/${room.id}/leave`),
        otherUser.token
      );

      expect(response.status).toBe(200);
      expect(response.body.room.participants).not.toContain(otherUser.user.id);
    });
  });

  describe('PUT /api/rooms/:id', () => {
    it('should allow owner to update room', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Original Name',
        type: 'public'
      });

      const updates = {
        name: 'Updated Name',
        description: 'Updated description',
        maxParticipants: 15
      };

      const response = await authenticatedRequest(
        request(app).put(`/api/rooms/${room.id}`),
        testToken
      ).send(updates);

      expect(response.status).toBe(200);
      expect(response.body.room.name).toBe(updates.name);
      expect(response.body.room.description).toBe(updates.description);
      expect(response.body.room.maxParticipants).toBe(updates.maxParticipants);
    });

    it('should not allow non-owner to update', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Owner Only Room',
        type: 'public'
      });

      const otherUser = await createTestUser();

      const response = await authenticatedRequest(
        request(app).put(`/api/rooms/${room.id}`),
        otherUser.token
      ).send({ name: 'Hacked' });

      expect(response.status).toBe(403);
    });
  });

  describe('DELETE /api/rooms/:id', () => {
    it('should allow owner to delete room', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Delete Me',
        type: 'public'
      });

      const response = await authenticatedRequest(
        request(app).delete(`/api/rooms/${room.id}`),
        testToken
      );

      expect(response.status).toBe(200);
      expect(dataStore.getRoom(room.id)).toBeUndefined();
    });

    it('should not allow non-owner to delete', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Protected Room',
        type: 'public'
      });

      const otherUser = await createTestUser();

      const response = await authenticatedRequest(
        request(app).delete(`/api/rooms/${room.id}`),
        otherUser.token
      );

      expect(response.status).toBe(403);
    });
  });
});