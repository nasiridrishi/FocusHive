import { ChatMessage } from '@focushive/shared';

interface RoomMessages {
  messages: ChatMessage[];
  lastReadTimestamp: Map<string, string>; // userId -> ISO timestamp
}

interface UserRateLimit {
  messageCount: number;
  windowStart: number;
}

class ChatStore {
  private rooms: Map<string, RoomMessages> = new Map();
  private rateLimits: Map<string, UserRateLimit> = new Map();
  private messageIdCounter = 0;
  
  private readonly MAX_MESSAGES_PER_ROOM = 1000;
  private readonly RATE_LIMIT_WINDOW = 60000; // 1 minute
  private readonly RATE_LIMIT_MAX_MESSAGES = 10;

  addMessage(data: {
    roomId: string;
    userId: string;
    username: string;
    message: string;
    type: 'user' | 'system';
  }): ChatMessage {
    const room = this.getOrCreateRoom(data.roomId);
    
    const message: ChatMessage = {
      id: `msg_${++this.messageIdCounter}`,
      roomId: data.roomId,
      userId: data.userId,
      username: data.username,
      message: data.message,
      timestamp: new Date().toISOString(),
      type: data.type
    };

    room.messages.push(message);

    // Trim old messages if exceeds limit
    if (room.messages.length > this.MAX_MESSAGES_PER_ROOM) {
      room.messages = room.messages.slice(-this.MAX_MESSAGES_PER_ROOM);
    }

    // Update rate limit for user messages
    if (data.type === 'user') {
      this.updateRateLimit(data.userId);
    }

    return message;
  }

  getMessages(roomId: string, limit: number): ChatMessage[] {
    const room = this.rooms.get(roomId);
    if (!room) {
      return [];
    }

    const messages = room.messages;
    return messages.slice(-limit);
  }

  clearMessages(roomId: string): void {
    const room = this.rooms.get(roomId);
    if (room) {
      room.messages = [];
    }
  }

  checkRateLimit(userId: string): boolean {
    const now = Date.now();
    const userLimit = this.rateLimits.get(userId);

    if (!userLimit) {
      return true;
    }

    // Check if window has expired
    if (now - userLimit.windowStart > this.RATE_LIMIT_WINDOW) {
      this.rateLimits.delete(userId);
      return true;
    }

    return userLimit.messageCount < this.RATE_LIMIT_MAX_MESSAGES;
  }

  getUnreadCount(roomId: string, userId: string): number {
    const room = this.rooms.get(roomId);
    if (!room) {
      return 0;
    }

    const lastRead = room.lastReadTimestamp.get(userId) || '1970-01-01T00:00:00.000Z';
    return room.messages.filter(msg => 
      msg.timestamp > lastRead && msg.userId !== userId
    ).length;
  }

  markAsRead(roomId: string, userId: string): void {
    const room = this.getOrCreateRoom(roomId);
    room.lastReadTimestamp.set(userId, new Date().toISOString());
  }

  private getOrCreateRoom(roomId: string): RoomMessages {
    if (!this.rooms.has(roomId)) {
      this.rooms.set(roomId, {
        messages: [],
        lastReadTimestamp: new Map()
      });
    }
    return this.rooms.get(roomId)!;
  }

  private updateRateLimit(userId: string): void {
    const now = Date.now();
    const userLimit = this.rateLimits.get(userId);

    if (!userLimit || now - userLimit.windowStart > this.RATE_LIMIT_WINDOW) {
      // Start new window
      this.rateLimits.set(userId, {
        messageCount: 1,
        windowStart: now
      });
    } else {
      // Increment count in current window
      userLimit.messageCount++;
    }
  }

  // For testing
  reset(): void {
    this.rooms.clear();
    this.rateLimits.clear();
    this.messageIdCounter = 0;
  }
}

export const chatStore = new ChatStore();