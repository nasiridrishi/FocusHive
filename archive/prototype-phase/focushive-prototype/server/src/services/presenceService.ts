import { ParticipantStatus } from '@focushive/shared';

interface PresenceData extends ParticipantStatus {
  roomId: string;
  isOnline: boolean;
  socketId?: string;
}

export class PresenceService {
  private presence = new Map<string, PresenceData>(); // key: roomId:userId
  private readonly IDLE_TIMEOUT = 5 * 60 * 1000; // 5 minutes

  addPresence(roomId: string, userId: string, socketId?: string): PresenceData {
    const key = `${roomId}:${userId}`;
    const now = Date.now();
    
    const presenceData: PresenceData = {
      userId,
      roomId,
      status: 'idle',
      joinedAt: now,
      lastActivity: now,
      sessionFocusTime: 0,
      isOnline: true,
      socketId
    };

    this.presence.set(key, presenceData);
    
    this.presence.set(key, presenceData);
    return presenceData;
  }

  removePresence(roomId: string, userId: string): void {
    const key = `${roomId}:${userId}`;
    this.presence.delete(key);
  }

  updatePresence(
    roomId: string, 
    userId: string, 
    updates: Partial<PresenceData>
  ): PresenceData | undefined {
    const key = `${roomId}:${userId}`;
    const current = this.presence.get(key);
    
    if (!current) return undefined;

    const updated = {
      ...current,
      ...updates,
      lastActivity: Date.now()
    };

    this.presence.set(key, updated);
    return updated;
  }

  getPresence(roomId: string, userId: string): PresenceData | undefined {
    const key = `${roomId}:${userId}`;
    return this.presence.get(key);
  }

  getRoomParticipants(roomId: string): ParticipantStatus[] {
    const participants: ParticipantStatus[] = [];
    const now = Date.now();

    for (const [key, data] of this.presence.entries()) {
      if (data.roomId === roomId) {
        // Check if idle
        if (data.status !== 'idle' && data.lastActivity && now - data.lastActivity > this.IDLE_TIMEOUT) {
          data.status = 'idle';
        }

        // Return only the ParticipantStatus fields
        participants.push({
          userId: data.userId,
          status: data.status,
          currentTask: data.currentTask,
          joinedAt: data.joinedAt,
          lastActivity: data.lastActivity,
          sessionFocusTime: data.sessionFocusTime
        });
      }
    }

    return participants;
  }

  updateActivity(roomId: string, userId: string): void {
    const key = `${roomId}:${userId}`;
    const current = this.presence.get(key);
    
    if (current) {
      current.lastActivity = Date.now();
    }
  }

  getUserRooms(userId: string): string[] {
    const rooms: string[] = [];
    
    for (const [key, data] of this.presence.entries()) {
      if (data.userId === userId) {
        rooms.push(data.roomId);
      }
    }

    return rooms;
  }

  clearRoomPresence(roomId: string): void {
    for (const [key, data] of this.presence.entries()) {
      if (data.roomId === roomId) {
        this.presence.delete(key);
      }
    }
  }

  // For testing
  clear(): void {
    this.presence.clear();
  }
}

// Export singleton instance
export const presenceService = new PresenceService();