/**
 * Socket Service - Provides programmatic access to socket functionality
 * Used by dummy data generator to simulate socket events
 */

import { Server } from 'socket.io';
import { ParticipantStatus } from '@focushive/shared';

class SocketService {
  private static instance: SocketService;
  public io: Server | null = null;
  private roomParticipants: Map<string, Map<string, ParticipantStatus>> = new Map();

  private constructor() {}

  static getInstance(): SocketService {
    if (!SocketService.instance) {
      SocketService.instance = new SocketService();
    }
    return SocketService.instance;
  }

  setIo(io: Server): void {
    this.io = io;
  }

  // Simulate a user joining a room
  handleRoomJoin(data: { roomId: string }, userId: string): void {
    if (!this.roomParticipants.has(data.roomId)) {
      this.roomParticipants.set(data.roomId, new Map());
    }

    const participants = this.roomParticipants.get(data.roomId)!;
    participants.set(userId, {
      userId,
      status: 'idle',
      currentTask: '',
      joinedAt: Date.now(),
      sessionFocusTime: 0
    });

    // Emit to room if io is available
    if (this.io) {
      this.io.to(data.roomId).emit('room:user-joined', {
        user: { id: userId },
        participant: participants.get(userId)
      });
    }
  }

  // Simulate presence update
  handlePresenceUpdate(data: { status: ParticipantStatus['status']; currentTask?: string }, userId: string): void {
    // Find which room the user is in
    let userRoom: string | null = null;
    let participantStatus: ParticipantStatus | null = null;

    this.roomParticipants.forEach((participants, roomId) => {
      if (participants.has(userId)) {
        userRoom = roomId;
        participantStatus = participants.get(userId)!;
        participantStatus.status = data.status;
        if (data.currentTask !== undefined) {
          participantStatus.currentTask = data.currentTask;
        }
      }
    });

    // Emit update if user is in a room
    if (userRoom && participantStatus && this.io) {
      this.io.to(userRoom).emit('presence:updated', {
        userId,
        status: participantStatus
      });
    }
  }

  // Get participants in a room
  getRoomParticipants(roomId: string): ParticipantStatus[] {
    const participants = this.roomParticipants.get(roomId);
    return participants ? Array.from(participants.values()) : [];
  }

  // Clear all data (for cleanup)
  clear(): void {
    this.roomParticipants.clear();
  }
}

export const socketService = SocketService.getInstance();