import { Room } from '@focushive/shared';
import { dataStore } from '../data/store';
import bcrypt from 'bcryptjs';
import { v4 as uuidv4 } from 'uuid';

interface CreateRoomData {
  name: string;
  description?: string;
  type: 'public' | 'private';
  password?: string;
  maxParticipants?: number;
  tags?: string[];
  focusType?: 'deepWork' | 'study' | 'creative' | 'meeting' | 'other';
}

interface RoomResponse {
  success: boolean;
  room: Room;
  message?: string;
}

export class RoomService {
  async createRoom(userId: string, data: CreateRoomData): Promise<Room> {
    // Validate room name
    if (!data.name || data.name.length < 3 || data.name.length > 50) {
      throw new Error('Room name must be between 3 and 50 characters');
    }

    // Check if room name already exists
    const allRooms = dataStore.getAllRooms();
    
    if (allRooms.some(room => room.name === data.name)) {
      throw new Error('Room name already exists');
    }

    // Hash password if private room
    let hashedPassword: string | undefined;
    if (data.type === 'private' && data.password) {
      hashedPassword = await bcrypt.hash(data.password, 10);
    }

    const room: Room = {
      id: uuidv4(),
      name: data.name,
      description: data.description || '',
      type: data.type,
      isPublic: data.type === 'public',
      password: hashedPassword,
      maxParticipants: data.maxParticipants || 10,
      ownerId: userId,
      participants: [userId],
      tags: data.tags || [],
      focusType: data.focusType || 'deepWork',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    // Create room and add owner as participant
    const createdRoom = dataStore.createRoom(room);
    dataStore.addParticipant(room.id, userId);

    return createdRoom;
  }

  async joinRoom(userId: string, roomId: string, password?: string): Promise<RoomResponse> {
    const room = dataStore.getRoom(roomId);
    if (!room) {
      throw new Error('Room not found');
    }

    // Check if user is already in room
    if (room.participants.includes(userId)) {
      throw new Error('User already in room');
    }

    // Check if room is full
    if (room.participants.length >= room.maxParticipants) {
      throw new Error('Room is full');
    }

    // Check password for private rooms
    if (room.type === 'private') {
      if (!password) {
        throw new Error('Password required for private room');
      }
      
      const isValidPassword = await bcrypt.compare(password, room.password || '');
      if (!isValidPassword) {
        throw new Error('Invalid password');
      }
    }

    // Leave current room if user is in one
    await this.leaveCurrentRoom(userId);

    // Add user to room
    dataStore.addParticipant(roomId, userId);
    const updatedRoom = dataStore.getRoom(roomId)!;

    return {
      success: true,
      room: updatedRoom
    };
  }

  async leaveRoom(userId: string, roomId: string): Promise<RoomResponse> {
    const room = dataStore.getRoom(roomId);
    if (!room) {
      throw new Error('Room not found');
    }

    // Check if user is the owner
    if (room.ownerId === userId) {
      throw new Error('Room owner cannot leave the room. Delete the room instead.');
    }

    // Remove user from room
    dataStore.removeParticipant(roomId, userId);
    const updatedRoom = dataStore.getRoom(roomId)!;

    return {
      success: true,
      room: updatedRoom
    };
  }

  async updateRoom(userId: string, roomId: string, updates: Partial<Room>): Promise<Room> {
    const room = dataStore.getRoom(roomId);
    if (!room) {
      throw new Error('Room not found');
    }

    // Check if user is the owner
    if (room.ownerId !== userId) {
      throw new Error('Only room owner can update room');
    }

    // Don't allow changing certain fields
    delete updates.id;
    delete updates.ownerId;
    delete updates.participants;
    delete updates.createdAt;

    const updatedRoom = dataStore.updateRoom(roomId, updates);
    if (!updatedRoom) {
      throw new Error('Failed to update room');
    }

    return updatedRoom;
  }

  async deleteRoom(userId: string, roomId: string): Promise<{ success: boolean }> {
    const room = dataStore.getRoom(roomId);
    if (!room) {
      throw new Error('Room not found');
    }

    // Check if user is the owner
    if (room.ownerId !== userId) {
      throw new Error('Only room owner can delete room');
    }

    // Delete room (this also removes all participants)
    dataStore.deleteRoom(roomId);

    return { success: true };
  }

  async getPublicRooms(): Promise<Room[]> {
    const rooms = dataStore.getPublicRooms();
    console.log(`[RoomService] Found ${rooms.length} public rooms`);
    return rooms;
  }

  async getRoom(roomId: string): Promise<Room | undefined> {
    return dataStore.getRoom(roomId);
  }

  async getUserRooms(userId: string): Promise<Room[]> {
    const allRooms = dataStore.getAllRooms();
    return allRooms.filter(room => 
      room.ownerId === userId || room.participants.includes(userId)
    );
  }

  private async leaveCurrentRoom(userId: string): Promise<void> {
    // Find if user is in any room
    const userRooms = await this.getUserRooms(userId);
    const currentRoom = userRooms.find(room => 
      room.participants.includes(userId) && room.ownerId !== userId
    );

    if (currentRoom) {
      dataStore.removeParticipant(currentRoom.id, userId);
    }
  }
}

export const roomService = new RoomService();