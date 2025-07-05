import { ChatService } from '../../services/chatService';
import { chatStore } from '../../data/chatStore';
import { dataStore } from '../../data/store';
import { timerService } from '../../services/timerService';
import { roomService } from '../../services/roomService';
import { User, Room, TimerState } from '@focushive/shared';

describe('Chat Integration Tests', () => {
  let chatService: ChatService;
  let user1: User;
  let user2: User;
  let room: Room;
  
  beforeEach(() => {
    // Reset all stores
    dataStore.clear();
    chatStore.reset();
    
    // Initialize services
    chatService = new ChatService();
    
    // Create test users
    user1 = dataStore.createUser({
      id: 'user1',
      email: 'user1@test.com',
      username: 'User1',
      password: 'hashed',
      avatar: 'avatar1.jpg',
      totalFocusTime: 0,
      currentStreak: 0,
      longestStreak: 0,
      points: 0,
      lastActiveDate: new Date().toISOString(),
      lookingForBuddy: false,
      preferences: {
        darkMode: false,
        soundEnabled: true,
        defaultPomodoro: { focusDuration: 25, breakDuration: 5 }
      },
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    });
    
    user2 = dataStore.createUser({
      id: 'user2',
      email: 'user2@test.com',
      username: 'User2',
      password: 'hashed',
      avatar: 'avatar2.jpg',
      totalFocusTime: 0,
      currentStreak: 0,
      longestStreak: 0,
      points: 0,
      lastActiveDate: new Date().toISOString(),
      lookingForBuddy: false,
      preferences: {
        darkMode: false,
        soundEnabled: true,
        defaultPomodoro: { focusDuration: 25, breakDuration: 5 }
      },
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    });
    
    // Create test room synchronously
    room = dataStore.createRoom({
      id: 'room1',
      name: 'Test Room',
      description: 'A room for testing chat',
      type: 'public',
      focusType: 'study',
      maxParticipants: 10,
      ownerId: user1.id,
      isPublic: true,
      participants: [],
      tags: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    });
    
    // Add participants
    dataStore.addParticipant(room.id, user1.id);
    dataStore.addParticipant(room.id, user2.id);
  });
  
  describe('Chat during different timer phases', () => {
    it('should allow chat during short break', () => {
      // Set timer to break phase
      timerService.setPhaseForTesting(room.id, 'shortBreak');
      
      // Send message
      const message = chatService.sendMessage(room.id, user1.id, 'Hello during break!');
      
      expect(message).toMatchObject({
        roomId: room.id,
        userId: user1.id,
        username: 'User1',
        message: 'Hello during break!',
        type: 'user'
      });
      
      // Verify message is stored
      const history = chatService.getMessageHistory(room.id);
      expect(history).toHaveLength(1);
      expect(history[0].message).toBe('Hello during break!');
    });
    
    it('should block chat during work phase', () => {
      // Set timer to work phase
      timerService.setPhaseForTesting(room.id, 'work');
      
      // Try to send message
      expect(() => {
        chatService.sendMessage(room.id, user1.id, 'Hello during work!');
      }).toThrow('Chat is only available during breaks');
    });
    
    it('should re-enable chat when switching to break', () => {
      // Start in work phase
      timerService.setPhaseForTesting(room.id, 'work');
      
      // Verify chat is disabled
      expect(chatService.isChatEnabled(room.id)).toBe(false);
      
      // Move to break
      timerService.setPhaseForTesting(room.id, 'shortBreak');
      
      // Verify chat is enabled
      expect(chatService.isChatEnabled(room.id)).toBe(true);
      
      // Send message successfully
      const message = chatService.sendMessage(room.id, user1.id, 'Break time!');
      expect(message).toBeDefined();
    });
  });
  
  describe('Multi-user chat flow', () => {
    beforeEach(() => {
      // Set timer to break phase
      timerService.setPhaseForTesting(room.id, 'shortBreak');
    });
    
    it('should handle conversation between multiple users', () => {
      // User1 sends message
      const msg1 = chatService.sendMessage(room.id, user1.id, 'Hey everyone!');
      
      // User2 sends message
      const msg2 = chatService.sendMessage(room.id, user2.id, 'Hi User1!');
      
      // User1 replies
      const msg3 = chatService.sendMessage(room.id, user1.id, 'How are you?');
      
      // Get history
      const history = chatService.getMessageHistory(room.id);
      
      expect(history).toHaveLength(3);
      expect(history[0].username).toBe('User1');
      expect(history[1].username).toBe('User2');
      expect(history[2].username).toBe('User1');
    });
    
    it('should track unread messages', () => {
      // User1 sends message
      chatService.sendMessage(room.id, user1.id, 'First message');
      
      // Check unread count for user2
      expect(chatService.getUnreadCount(room.id, user2.id)).toBe(1);
      
      // User1 sends another
      chatService.sendMessage(room.id, user1.id, 'Second message');
      
      expect(chatService.getUnreadCount(room.id, user2.id)).toBe(2);
      
      // User2 marks as read
      chatService.markMessagesAsRead(room.id, user2.id);
      
      expect(chatService.getUnreadCount(room.id, user2.id)).toBe(0);
    });
  });
  
  describe('Rate limiting', () => {
    beforeEach(() => {
      // Set timer to break phase
      timerService.setPhaseForTesting(room.id, 'shortBreak');
    });
    
    it('should enforce rate limits', () => {
      // Send 10 messages (the limit)
      for (let i = 0; i < 10; i++) {
        chatService.sendMessage(room.id, user1.id, `Message ${i + 1}`);
      }
      
      // 11th message should fail
      expect(() => {
        chatService.sendMessage(room.id, user1.id, 'Message 11');
      }).toThrow('Rate limit exceeded');
    });
    
    it('should reset rate limit after time window', () => {
      // Send 10 messages
      for (let i = 0; i < 10; i++) {
        chatService.sendMessage(room.id, user1.id, `Message ${i + 1}`);
      }
      
      // Mock time passing (more than 1 minute)
      jest.spyOn(Date, 'now').mockReturnValue(Date.now() + 61000);
      
      // Should be able to send again
      const message = chatService.sendMessage(room.id, user1.id, 'New message');
      expect(message).toBeDefined();
      
      // Restore Date.now
      jest.restoreAllMocks();
    });
  });
  
  describe('System messages', () => {
    it('should send system messages', () => {
      const systemMsg = chatService.sendSystemMessage(room.id, 'User1 has joined the room');
      
      expect(systemMsg).toMatchObject({
        roomId: room.id,
        userId: 'system',
        username: 'System',
        message: 'User1 has joined the room',
        type: 'system'
      });
      
      const history = chatService.getMessageHistory(room.id);
      expect(history).toHaveLength(1);
      expect(history[0].type).toBe('system');
    });
    
    it('should mix system and user messages', () => {
      // System message
      chatService.sendSystemMessage(room.id, 'Chat started');
      
      // Set to break phase
      timerService.setPhaseForTesting(room.id, 'shortBreak');
      
      // User message
      chatService.sendMessage(room.id, user1.id, 'Hello!');
      
      // Another system message
      chatService.sendSystemMessage(room.id, 'User2 has left');
      
      const history = chatService.getMessageHistory(room.id);
      expect(history).toHaveLength(3);
      expect(history[0].type).toBe('system');
      expect(history[1].type).toBe('user');
      expect(history[2].type).toBe('system');
    });
  });
  
  describe('Message validation', () => {
    beforeEach(() => {
      // Set timer to break phase
      timerService.setPhaseForTesting(room.id, 'shortBreak');
    });
    
    it('should handle edge cases', () => {
      // Max length message
      const maxMsg = 'a'.repeat(500);
      const message = chatService.sendMessage(room.id, user1.id, maxMsg);
      expect(message.message).toHaveLength(500);
      
      // Whitespace trimming
      const trimmedMsg = chatService.sendMessage(room.id, user1.id, '  Hello World  ');
      expect(trimmedMsg.message).toBe('Hello World');
    });
  });
});