import { ChatService } from '../../services/chatService';
import { chatStore } from '../../data/chatStore';
import { dataStore } from '../../data/store';
import { timerService } from '../../services/timerService';
import { TimerPhase, ChatMessage } from '@focushive/shared';

// Mock dependencies
jest.mock('../../data/chatStore');
jest.mock('../../data/store');
jest.mock('../../services/timerService');

describe('ChatService', () => {
  let service: ChatService;
  const mockRoom = {
    id: 'room1',
    name: 'Test Room',
    participants: ['user1', 'user2'],
    ownerId: 'user1'
  };

  beforeEach(() => {
    service = new ChatService();
    jest.clearAllMocks();
    
    // Default mocks
    (dataStore.getRoom as jest.Mock).mockReturnValue(mockRoom);
    (dataStore.getParticipant as jest.Mock).mockImplementation((roomId, userId) => ({
      userId,
      roomId,
      username: `User${userId.slice(-1)}`
    }));
  });

  describe('sendMessage', () => {
    it('should send a message successfully during break', () => {
      const mockTimer = {
        phase: 'shortBreak' as TimerPhase,
        status: 'running'
      };
      (timerService.getTimer as jest.Mock).mockReturnValue(mockTimer);
      (chatStore.checkRateLimit as jest.Mock).mockReturnValue(true);
      (chatStore.addMessage as jest.Mock).mockReturnValue({
        id: 'msg1',
        roomId: 'room1',
        userId: 'user1',
        username: 'User1',
        message: 'Hello!',
        timestamp: Date.now(),
        type: 'user'
      });

      const message = service.sendMessage('room1', 'user1', 'Hello!');

      expect(message).toMatchObject({
        roomId: 'room1',
        userId: 'user1',
        username: 'User1',
        message: 'Hello!',
        type: 'user'
      });
      expect(chatStore.addMessage).toHaveBeenCalled();
    });

    it('should throw error during work phase', () => {
      const mockTimer = {
        phase: 'work' as TimerPhase,
        status: 'running'
      };
      (timerService.getTimer as jest.Mock).mockReturnValue(mockTimer);

      expect(() => {
        service.sendMessage('room1', 'user1', 'Hello!');
      }).toThrow('Chat is only available during breaks');
    });

    it('should throw error if user not in room', () => {
      (dataStore.getParticipant as jest.Mock).mockReturnValue(null);

      expect(() => {
        service.sendMessage('room1', 'user3', 'Hello!');
      }).toThrow('User not in room');
    });

    it('should throw error if message is empty', () => {
      const mockTimer = {
        phase: 'shortBreak' as TimerPhase,
        status: 'running'
      };
      (timerService.getTimer as jest.Mock).mockReturnValue(mockTimer);

      expect(() => {
        service.sendMessage('room1', 'user1', '   ');
      }).toThrow('Message cannot be empty');
    });

    it('should throw error if message exceeds character limit', () => {
      const mockTimer = {
        phase: 'shortBreak' as TimerPhase,
        status: 'running'
      };
      (timerService.getTimer as jest.Mock).mockReturnValue(mockTimer);

      const longMessage = 'a'.repeat(501);
      expect(() => {
        service.sendMessage('room1', 'user1', longMessage);
      }).toThrow('Message exceeds 500 character limit');
    });

    it('should throw error if rate limit exceeded', () => {
      const mockTimer = {
        phase: 'shortBreak' as TimerPhase,
        status: 'running'
      };
      (timerService.getTimer as jest.Mock).mockReturnValue(mockTimer);
      (chatStore.checkRateLimit as jest.Mock).mockReturnValue(false);

      expect(() => {
        service.sendMessage('room1', 'user1', 'Hello!');
      }).toThrow('Rate limit exceeded. Please wait before sending another message');
    });

    it('should allow chat when timer is idle in break phase', () => {
      const mockTimer = {
        phase: 'shortBreak' as TimerPhase,
        status: 'idle'
      };
      (timerService.getTimer as jest.Mock).mockReturnValue(mockTimer);
      (chatStore.checkRateLimit as jest.Mock).mockReturnValue(true);
      (chatStore.addMessage as jest.Mock).mockReturnValue({
        id: 'msg1',
        roomId: 'room1',
        userId: 'user1',
        username: 'User1',
        message: 'Hello!',
        timestamp: Date.now(),
        type: 'user'
      });

      const message = service.sendMessage('room1', 'user1', 'Hello!');
      expect(message).toBeDefined();
    });
  });

  describe('getMessageHistory', () => {
    it('should return message history', () => {
      const mockMessages = [
        {
          id: 'msg1',
          roomId: 'room1',
          userId: 'user1',
          username: 'User1',
          message: 'Hello!',
          timestamp: Date.now() - 1000,
          type: 'user' as const
        },
        {
          id: 'msg2',
          roomId: 'room1',
          userId: 'user2',
          username: 'User2',
          message: 'Hi there!',
          timestamp: Date.now(),
          type: 'user' as const
        }
      ];
      (chatStore.getMessages as jest.Mock).mockReturnValue(mockMessages);

      const history = service.getMessageHistory('room1', 50);

      expect(history).toEqual(mockMessages);
      expect(chatStore.getMessages).toHaveBeenCalledWith('room1', 50);
    });

    it('should throw error if room does not exist', () => {
      (dataStore.getRoom as jest.Mock).mockReturnValue(null);

      expect(() => {
        service.getMessageHistory('room999', 50);
      }).toThrow('Room not found');
    });

    it('should use default limit of 100', () => {
      (chatStore.getMessages as jest.Mock).mockReturnValue([]);

      service.getMessageHistory('room1');

      expect(chatStore.getMessages).toHaveBeenCalledWith('room1', 100);
    });
  });

  describe('isChatEnabled', () => {
    it('should return true during short break', () => {
      const mockTimer = {
        phase: 'shortBreak' as TimerPhase,
        status: 'running'
      };
      (timerService.getTimer as jest.Mock).mockReturnValue(mockTimer);

      expect(service.isChatEnabled('room1')).toBe(true);
    });

    it('should return true during long break', () => {
      const mockTimer = {
        phase: 'longBreak' as TimerPhase,
        status: 'running'
      };
      (timerService.getTimer as jest.Mock).mockReturnValue(mockTimer);

      expect(service.isChatEnabled('room1')).toBe(true);
    });

    it('should return false during work phase', () => {
      const mockTimer = {
        phase: 'work' as TimerPhase,
        status: 'running'
      };
      (timerService.getTimer as jest.Mock).mockReturnValue(mockTimer);

      expect(service.isChatEnabled('room1')).toBe(false);
    });

    it('should return false if no timer exists', () => {
      (timerService.getTimer as jest.Mock).mockReturnValue(null);

      expect(service.isChatEnabled('room1')).toBe(false);
    });
  });

  describe('validateMessage', () => {
    it('should validate normal message', () => {
      expect(service.validateMessage('Hello world!')).toBe(true);
    });

    it('should reject empty message', () => {
      expect(service.validateMessage('')).toBe(false);
      expect(service.validateMessage('   ')).toBe(false);
    });

    it('should reject message over 500 characters', () => {
      const longMessage = 'a'.repeat(501);
      expect(service.validateMessage(longMessage)).toBe(false);
    });

    it('should accept message exactly 500 characters', () => {
      const maxMessage = 'a'.repeat(500);
      expect(service.validateMessage(maxMessage)).toBe(true);
    });
  });

  describe('clearRoomMessages', () => {
    it('should clear all room messages', () => {
      service.clearRoomMessages('room1');

      expect(chatStore.clearMessages).toHaveBeenCalledWith('room1');
    });

    it('should throw error if room does not exist', () => {
      (dataStore.getRoom as jest.Mock).mockReturnValue(null);

      expect(() => {
        service.clearRoomMessages('room999');
      }).toThrow('Room not found');
    });
  });

  describe('sendSystemMessage', () => {
    it('should send system message', () => {
      (chatStore.addMessage as jest.Mock).mockReturnValue({
        id: 'sys1',
        roomId: 'room1',
        userId: 'system',
        username: 'System',
        message: 'User1 joined the room',
        timestamp: Date.now(),
        type: 'system'
      });

      const message = service.sendSystemMessage('room1', 'User1 joined the room');

      expect(message).toMatchObject({
        userId: 'system',
        username: 'System',
        message: 'User1 joined the room',
        type: 'system'
      });
    });
  });

  describe('getUnreadCount', () => {
    it('should return unread message count', () => {
      (chatStore.getUnreadCount as jest.Mock).mockReturnValue(5);

      const count = service.getUnreadCount('room1', 'user1');

      expect(count).toBe(5);
      expect(chatStore.getUnreadCount).toHaveBeenCalledWith('room1', 'user1');
    });
  });

  describe('markMessagesAsRead', () => {
    it('should mark messages as read', () => {
      service.markMessagesAsRead('room1', 'user1');

      expect(chatStore.markAsRead).toHaveBeenCalledWith('room1', 'user1');
    });
  });
});