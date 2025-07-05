import { ChatMessage } from '@focushive/shared';
import { chatStore } from '../data/chatStore';
import { dataStore } from '../data/store';
import { timerService } from './timerService';

export class ChatService {
  private readonly MAX_MESSAGE_LENGTH = 500;
  
  sendMessage(roomId: string, userId: string, message: string): ChatMessage {
    // Validate room exists
    const room = dataStore.getRoom(roomId);
    if (!room) {
      throw new Error('Room not found');
    }

    // Check if user is in room
    const participant = dataStore.getParticipant(roomId, userId);
    if (!participant) {
      throw new Error('User not in room');
    }

    // Check if chat is enabled (only during breaks)
    if (!this.isChatEnabled(roomId)) {
      throw new Error('Chat is only available during breaks');
    }

    // Validate message
    if (!this.validateMessage(message)) {
      if (message.trim().length === 0) {
        throw new Error('Message cannot be empty');
      }
      throw new Error(`Message exceeds ${this.MAX_MESSAGE_LENGTH} character limit`);
    }

    // Check rate limit
    if (!chatStore.checkRateLimit(userId)) {
      throw new Error('Rate limit exceeded. Please wait before sending another message');
    }

    // Add message
    const chatMessage = chatStore.addMessage({
      roomId,
      userId,
      username: participant.username,
      message: message.trim(),
      type: 'user'
    });

    return chatMessage;
  }

  getMessageHistory(roomId: string, limit: number = 100): ChatMessage[] {
    const room = dataStore.getRoom(roomId);
    if (!room) {
      throw new Error('Room not found');
    }

    return chatStore.getMessages(roomId, limit);
  }

  isChatEnabled(roomId: string): boolean {
    const timer = timerService.getTimer(roomId);
    if (!timer) {
      return false;
    }

    // Chat is enabled during break phases
    return timer.phase === 'shortBreak' || timer.phase === 'longBreak';
  }

  validateMessage(message: string): boolean {
    const trimmed = message.trim();
    return trimmed.length > 0 && trimmed.length <= this.MAX_MESSAGE_LENGTH;
  }

  clearRoomMessages(roomId: string): void {
    const room = dataStore.getRoom(roomId);
    if (!room) {
      throw new Error('Room not found');
    }

    chatStore.clearMessages(roomId);
  }

  sendSystemMessage(roomId: string, message: string): ChatMessage {
    return chatStore.addMessage({
      roomId,
      userId: 'system',
      username: 'System',
      message,
      type: 'system'
    });
  }

  getUnreadCount(roomId: string, userId: string): number {
    return chatStore.getUnreadCount(roomId, userId);
  }

  markMessagesAsRead(roomId: string, userId: string): void {
    chatStore.markAsRead(roomId, userId);
  }
}

export const chatService = new ChatService();