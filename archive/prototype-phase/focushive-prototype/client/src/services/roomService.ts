import type { Room } from '@focushive/shared';
import { api } from './api';

interface CreateRoomData {
  name: string;
  description?: string;
  type: 'public' | 'private';
  password?: string;
  maxParticipants?: number;
  tags?: string[];
  focusType?: 'deepWork' | 'study' | 'creative' | 'meeting' | 'other';
}

interface JoinRoomResponse {
  room: Room;
  message: string;
}

export const roomService = {
  async getPublicRooms(): Promise<Room[]> {
    const response = await api.get<{ rooms: Room[] }>('/api/rooms');
    return response.data.rooms;
  },

  async getUserRooms(): Promise<Room[]> {
    const response = await api.get<{ rooms: Room[] }>('/api/rooms/my-rooms');
    return response.data.rooms;
  },

  async getRoom(id: string): Promise<Room> {
    const response = await api.get<{ room: Room }>(`/api/rooms/${id}`);
    return response.data.room;
  },

  async createRoom(data: CreateRoomData): Promise<Room> {
    const response = await api.post<{ room: Room }>('/api/rooms', data);
    return response.data.room;
  },

  async joinRoom(roomId: string, password?: string): Promise<JoinRoomResponse> {
    const response = await api.post<JoinRoomResponse>(`/api/rooms/${roomId}/join`, { password });
    return response.data;
  },

  async leaveRoom(roomId: string): Promise<void> {
    await api.post(`/api/rooms/${roomId}/leave`);
  },

  async updateRoom(roomId: string, updates: Partial<Room>): Promise<Room> {
    const response = await api.put<{ room: Room }>(`/api/rooms/${roomId}`, updates);
    return response.data.room;
  },

  async deleteRoom(roomId: string): Promise<void> {
    await api.delete(`/api/rooms/${roomId}`);
  },
};