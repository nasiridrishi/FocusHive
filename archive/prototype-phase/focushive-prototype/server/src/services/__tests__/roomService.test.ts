import { RoomService } from '../roomService';
import { dataStore } from '../../data/store';
import { createTestUser } from '../../__tests__/utils/testHelpers';
import { Room } from '@focushive/shared';

describe('RoomService', () => {
  let roomService: RoomService;
  let testUser: any;
  let testToken: string;

  beforeEach(async () => {
    roomService = new RoomService();
    const result = await createTestUser();
    testUser = result.user;
    testToken = result.token;
  });

  describe('createRoom', () => {
    it('should create a public room', async () => {
      const roomData = {
        name: 'Study Room',
        description: 'A quiet place to study',
        type: 'public' as const,
        maxParticipants: 8,
        tags: ['study', 'quiet']
      };

      const room = await roomService.createRoom(testUser.id, roomData);

      expect(room).toBeDefined();
      expect(room.name).toBe(roomData.name);
      expect(room.description).toBe(roomData.description);
      expect(room.type).toBe('public');
      expect(room.isPublic).toBe(true);
      expect(room.ownerId).toBe(testUser.id);
      expect(room.participants).toContain(testUser.id);
      expect(room.maxParticipants).toBe(8);
      expect(room.tags).toEqual(['study', 'quiet']);
    });

    it('should create a private room with password', async () => {
      const roomData = {
        name: 'Private Study Room',
        description: 'Invite only',
        type: 'private' as const,
        password: 'secret123',
        maxParticipants: 4
      };

      const room = await roomService.createRoom(testUser.id, roomData);

      expect(room).toBeDefined();
      expect(room.type).toBe('private');
      expect(room.isPublic).toBe(false);
      expect(room.password).toBeDefined();
      // Password should be hashed
      expect(room.password).not.toBe('secret123');
    });

    it('should throw error if room name already exists', async () => {
      const roomData = {
        name: 'Unique Room',
        type: 'public' as const
      };

      await roomService.createRoom(testUser.id, roomData);
      
      await expect(
        roomService.createRoom(testUser.id, roomData)
      ).rejects.toThrow('Room name already exists');
    });

    it('should enforce maximum room name length', async () => {
      const roomData = {
        name: 'A'.repeat(51), // 51 characters
        type: 'public' as const
      };

      await expect(
        roomService.createRoom(testUser.id, roomData)
      ).rejects.toThrow('Room name must be between 3 and 50 characters');
    });
  });

  describe('joinRoom', () => {
    let publicRoom: Room;
    let privateRoom: Room;
    let otherUser: any;

    beforeEach(async () => {
      const otherUserResult = await createTestUser();
      otherUser = otherUserResult.user;

      publicRoom = await roomService.createRoom(testUser.id, {
        name: 'Public Room',
        type: 'public',
        maxParticipants: 5
      });

      privateRoom = await roomService.createRoom(testUser.id, {
        name: 'Private Room',
        type: 'private',
        password: 'secret123',
        maxParticipants: 3
      });
    });

    it('should allow joining a public room', async () => {
      const result = await roomService.joinRoom(otherUser.id, publicRoom.id);

      expect(result.success).toBe(true);
      expect(result.room.participants).toContain(otherUser.id);
      expect(result.room.participants.length).toBe(2);
    });

    it('should require password for private room', async () => {
      await expect(
        roomService.joinRoom(otherUser.id, privateRoom.id)
      ).rejects.toThrow('Password required for private room');
    });

    it('should allow joining private room with correct password', async () => {
      const result = await roomService.joinRoom(otherUser.id, privateRoom.id, 'secret123');

      expect(result.success).toBe(true);
      expect(result.room.participants).toContain(otherUser.id);
    });

    it('should reject incorrect password for private room', async () => {
      await expect(
        roomService.joinRoom(otherUser.id, privateRoom.id, 'wrongpassword')
      ).rejects.toThrow('Invalid password');
    });

    it('should enforce max participants limit', async () => {
      // Fill the room to capacity
      const room = await roomService.createRoom(testUser.id, {
        name: 'Small Room',
        type: 'public',
        maxParticipants: 2
      });

      // First user can join
      const user1 = await createTestUser();
      await roomService.joinRoom(user1.user.id, room.id);

      // Second user should be rejected
      const user2 = await createTestUser();
      await expect(
        roomService.joinRoom(user2.user.id, room.id)
      ).rejects.toThrow('Room is full');
    });

    it('should not allow user to join same room twice', async () => {
      await roomService.joinRoom(otherUser.id, publicRoom.id);

      await expect(
        roomService.joinRoom(otherUser.id, publicRoom.id)
      ).rejects.toThrow('User already in room');
    });

    it('should remove user from previous room when joining new room', async () => {
      const room1 = await roomService.createRoom(testUser.id, {
        name: 'Room 1',
        type: 'public'
      });
      const room2 = await roomService.createRoom(testUser.id, {
        name: 'Room 2',
        type: 'public'
      });

      // Join first room
      await roomService.joinRoom(otherUser.id, room1.id);
      expect(room1.participants).toContain(otherUser.id);

      // Join second room
      await roomService.joinRoom(otherUser.id, room2.id);
      
      // Check user is in room 2 and not in room 1
      const updatedRoom1 = dataStore.getRoom(room1.id);
      const updatedRoom2 = dataStore.getRoom(room2.id);
      
      expect(updatedRoom1?.participants).not.toContain(otherUser.id);
      expect(updatedRoom2?.participants).toContain(otherUser.id);
    });
  });

  describe('leaveRoom', () => {
    it('should allow user to leave room', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Leave Test Room',
        type: 'public'
      });

      const otherUser = await createTestUser();
      await roomService.joinRoom(otherUser.user.id, room.id);

      const result = await roomService.leaveRoom(otherUser.user.id, room.id);

      expect(result.success).toBe(true);
      expect(result.room.participants).not.toContain(otherUser.user.id);
    });

    it('should not allow room owner to leave their room', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Owner Room',
        type: 'public'
      });

      await expect(
        roomService.leaveRoom(testUser.id, room.id)
      ).rejects.toThrow('Room owner cannot leave the room. Delete the room instead.');
    });
  });

  describe('updateRoom', () => {
    it('should allow owner to update room', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Original Name',
        type: 'public'
      });

      const updated = await roomService.updateRoom(testUser.id, room.id, {
        name: 'Updated Name',
        description: 'Updated description'
      });

      expect(updated.name).toBe('Updated Name');
      expect(updated.description).toBe('Updated description');
    });

    it('should not allow non-owner to update room', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Owner Room',
        type: 'public'
      });

      const otherUser = await createTestUser();

      await expect(
        roomService.updateRoom(otherUser.user.id, room.id, {
          name: 'Hacked Name'
        })
      ).rejects.toThrow('Only room owner can update room');
    });
  });

  describe('deleteRoom', () => {
    it('should allow owner to delete room', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Delete Me',
        type: 'public'
      });

      const result = await roomService.deleteRoom(testUser.id, room.id);

      expect(result.success).toBe(true);
      expect(dataStore.getRoom(room.id)).toBeUndefined();
    });

    it('should not allow non-owner to delete room', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Protected Room',
        type: 'public'
      });

      const otherUser = await createTestUser();

      await expect(
        roomService.deleteRoom(otherUser.user.id, room.id)
      ).rejects.toThrow('Only room owner can delete room');
    });

    it('should remove all participants when room is deleted', async () => {
      const room = await roomService.createRoom(testUser.id, {
        name: 'Room With Participants',
        type: 'public'
      });

      const user1 = await createTestUser();
      const user2 = await createTestUser();
      
      await roomService.joinRoom(user1.user.id, room.id);
      await roomService.joinRoom(user2.user.id, room.id);

      await roomService.deleteRoom(testUser.id, room.id);

      const participants = dataStore.getRoomParticipants(room.id);
      expect(participants.length).toBe(0);
    });
  });

  describe('getPublicRooms', () => {
    it('should return only public rooms', async () => {
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

      const rooms = await roomService.getPublicRooms();

      expect(rooms.length).toBe(2);
      expect(rooms.every(r => r.isPublic)).toBe(true);
      expect(rooms.map(r => r.name)).toContain('Public Room 1');
      expect(rooms.map(r => r.name)).toContain('Public Room 2');
      expect(rooms.map(r => r.name)).not.toContain('Private Room');
    });
  });

  describe('getUserRooms', () => {
    it('should return rooms where user is owner or participant', async () => {
      const ownedRoom = await roomService.createRoom(testUser.id, {
        name: 'My Room',
        type: 'public'
      });

      const otherUser = await createTestUser();
      const otherRoom = await roomService.createRoom(otherUser.user.id, {
        name: 'Other Room',
        type: 'public'
      });

      // Join other user's room
      await roomService.joinRoom(testUser.id, otherRoom.id);

      const rooms = await roomService.getUserRooms(testUser.id);

      expect(rooms.length).toBe(2);
      expect(rooms.map(r => r.id)).toContain(ownedRoom.id);
      expect(rooms.map(r => r.id)).toContain(otherRoom.id);
    });
  });
});