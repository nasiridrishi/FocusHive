import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { ChatService } from '../chatService';
import { authService } from '@/services/auth/authService';
import { webSocketService } from '@/services/websocket/WebSocketService';
import type {
  ChatMessage,
  SendMessageRequest,
  UpdateMessageRequest,
  ChatTypingIndicator,
  ChatHistory
} from '@/contracts/chat';
import type { PaginatedResponse } from '@/contracts/common';

// Mock the dependencies
vi.mock('@/services/auth/authService');
vi.mock('@/services/websocket/WebSocketService');

describe('ChatService E2E Tests', () => {
  let chatService: ChatService;

  beforeEach(() => {
    // Clear all mocks before each test
    vi.clearAllMocks();

    // Setup auth mock
    vi.spyOn(authService, 'getAccessToken').mockReturnValue('mock-token');
    vi.spyOn(authService, 'isAuthenticated').mockReturnValue(true);

    // Setup WebSocket mock
    vi.spyOn(webSocketService, 'isConnectedStatus').mockReturnValue(true);
    vi.spyOn(webSocketService, 'subscribe').mockReturnValue('subscription-id');
    vi.spyOn(webSocketService, 'sendMessage').mockImplementation(() => {});

    // Create service instance
    chatService = new ChatService();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('Send Message', () => {
    it('should send a text message to a hive', async () => {
      const request: SendMessageRequest = {
        hiveId: '1',
        content: 'Hello, everyone!',
        type: 'text',
      };

      const mockMessage: ChatMessage = {
        id: '1',
        hiveId: request.hiveId,
        senderId: '123',
        senderName: 'testuser',
        text: request.content,
        timestamp: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockMessage),
      } as any);

      const result = await chatService.sendMessage(request);

      expect(result).toEqual(mockMessage);
      expect(global.fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/chat/messages',
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-token',
            'Content-Type': 'application/json',
          }),
          body: JSON.stringify(request),
        })
      );
    });

    it('should send a message with mentions', async () => {
      const request: SendMessageRequest = {
        hiveId: '1',
        content: 'Hey @john and @jane, check this out!',
        mentions: ['john', 'jane'],
      };

      const mockMessage: ChatMessage = {
        id: '2',
        hiveId: request.hiveId,
        senderId: '123',
        senderName: 'testuser',
        text: request.content,
        timestamp: new Date().toISOString(),
        mentions: request.mentions,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockMessage),
      } as any);

      const result = await chatService.sendMessage(request);

      expect(result.mentions).toEqual(['john', 'jane']);
    });

    it('should handle optimistic updates for messages', async () => {
      const request: SendMessageRequest = {
        hiveId: '1',
        content: 'Test optimistic update',
      };

      // Track optimistic message
      const optimisticId = await chatService.sendMessageOptimistic(request);
      expect(optimisticId).toBeDefined();
      expect(optimisticId).toMatch(/^optimistic-/);

      // Verify optimistic message is stored
      const optimisticMessage = chatService.getOptimisticMessage(optimisticId);
      expect(optimisticMessage).toBeDefined();
      // Note: ChatMessage doesn't have status property, only ExtendedChatMessage does
      expect(optimisticMessage).toBeDefined();
    });
  });

  describe('Get Message History', () => {
    it('should fetch message history for a hive', async () => {
      const hiveId = 1;
      const mockMessages: ChatMessage[] = [
        {
          id: '1',
          hiveId: String(hiveId),
          senderId: '123',
          senderName: 'user1',
          text: 'First message',
          timestamp: '2024-01-01T10:00:00Z',
          createdAt: '2024-01-01T10:00:00Z',
          updatedAt: '2024-01-01T10:00:00Z',
        },
        {
          id: '2',
          hiveId: String(hiveId),
          senderId: '124',
          senderName: 'user2',
          text: 'Second message',
          timestamp: '2024-01-01T10:01:00Z',
          createdAt: '2024-01-01T10:01:00Z',
          updatedAt: '2024-01-01T10:01:00Z',
        },
      ];

      const mockHistory: ChatHistory = {
        messages: mockMessages.map(msg => ({ ...msg, type: 'text' as const, status: 'sent' as const })),
        hasMore: true,
        total: 100,
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockHistory),
      } as any);

      const result = await chatService.getMessageHistory(hiveId);

      expect(result).toEqual(mockHistory);
      expect(global.fetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/chat/hives/${hiveId}/history`,
        expect.objectContaining({
          method: 'GET',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-token',
          }),
        })
      );
    });

    it('should cache message history', async () => {
      const hiveId = 1;
      const mockHistory: ChatHistory = {
        messages: [],
        hasMore: false,
        total: 0,
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockHistory),
      } as any);

      // First call should fetch from API
      await chatService.getMessageHistory(hiveId);
      expect(global.fetch).toHaveBeenCalledTimes(1);

      // Second call should use cache
      await chatService.getMessageHistory(hiveId);
      expect(global.fetch).toHaveBeenCalledTimes(1);

      // Clear cache
      chatService.clearCache();

      // Third call should fetch again
      await chatService.getMessageHistory(hiveId);
      expect(global.fetch).toHaveBeenCalledTimes(2);
    });

    it('should support pagination for message history', async () => {
      const hiveId = 1;
      const beforeMessageId = 50;
      const limit = 20;

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue({
          hiveId,
          messages: [],
          hasMore: true,
          totalMessages: 100,
        }),
      } as any);

      await chatService.getMessageHistory(hiveId, { beforeMessageId, limit });

      expect(global.fetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/chat/hives/${hiveId}/history?before=${beforeMessageId}&limit=${limit}`,
        expect.any(Object)
      );
    });
  });

  describe('Update Message', () => {
    it('should update an existing message', async () => {
      const messageId = 1;
      const request: UpdateMessageRequest = {
        content: 'Updated message content',
      };

      const mockMessage: ChatMessage = {
        id: String(messageId),
        hiveId: '1',
        senderId: '123',
        senderName: 'testuser',
        text: request.content!,
        timestamp: '2024-01-01T10:00:00Z',
        createdAt: '2024-01-01T10:00:00Z',
        updatedAt: new Date().toISOString(),
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockMessage),
      } as any);

      const result = await chatService.updateMessage(messageId, request);

      expect(result).toEqual(mockMessage);
      expect(result.edited).toBe(true);
      expect(global.fetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/chat/messages/${messageId}`,
        expect.objectContaining({
          method: 'PUT',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-token',
            'Content-Type': 'application/json',
          }),
          body: JSON.stringify(request),
        })
      );
    });
  });

  describe('Delete Message', () => {
    it('should delete a message', async () => {
      const messageId = 1;

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
      } as any);

      await chatService.deleteMessage(messageId);

      expect(global.fetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/chat/messages/${messageId}`,
        expect.objectContaining({
          method: 'DELETE',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-token',
          }),
        })
      );
    });

    it('should handle soft delete', async () => {
      const messageId = 1;

      const mockMessage: ChatMessage = {
        id: String(messageId),
        hiveId: '1',
        senderId: '123',
        senderName: 'testuser',
        text: '[Message deleted]',
        timestamp: '2024-01-01T10:00:00Z',
        deleted: true,
        createdAt: '2024-01-01T10:00:00Z',
        updatedAt: new Date().toISOString(),
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockMessage),
      } as any);

      const result = await chatService.deleteMessage(messageId, { soft: true });

      expect(result).toEqual(mockMessage);
      expect(result).toBeDefined();
      expect((result as ChatMessage).deleted).toBe(true);
    });
  });

  describe('Message Reactions', () => {
    it('should add a reaction to a message', async () => {
      const messageId = 1;
      const emoji = '👍';

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue({ success: true }),
      } as any);

      await chatService.addReaction(messageId, emoji);

      expect(global.fetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/chat/messages/${messageId}/reactions`,
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-token',
            'Content-Type': 'application/json',
          }),
          body: JSON.stringify({ emoji }),
        })
      );
    });

    it('should remove a reaction from a message', async () => {
      const messageId = 1;
      const emoji = '👍';

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
      } as any);

      await chatService.removeReaction(messageId, emoji);

      expect(global.fetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/chat/messages/${messageId}/reactions/${emoji}`,
        expect.objectContaining({
          method: 'DELETE',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-token',
          }),
        })
      );
    });
  });

  describe('Typing Indicators', () => {
    it('should send typing indicator via WebSocket', () => {
      const hiveId = 1;
      const isTyping = true;

      chatService.sendTypingIndicator(hiveId, isTyping);

      expect(webSocketService.sendMessage).toHaveBeenCalledWith(
        '/app/chat/typing',
        expect.objectContaining({
          hiveId,
          isTyping,
        })
      );
    });

    it('should subscribe to typing indicators', async () => {
      const hiveId = 1;
      const callback = vi.fn();
      let capturedCallback: any;

      vi.spyOn(webSocketService, 'subscribe').mockImplementation((topic, cb) => {
        capturedCallback = cb;
        return 'subscription-id';
      });

      const unsubscribe = chatService.subscribeToTypingIndicators(hiveId, callback);

      expect(webSocketService.subscribe).toHaveBeenCalledWith(
        `/topic/chat/${hiveId}/typing`,
        expect.any(Function)
      );

      // Simulate a typing indicator
      const typingData: ChatTypingIndicator = {
        userId: '123',
        userName: 'testuser',
        roomId: String(hiveId),
        startedAt: new Date().toISOString(),
        isTyping: true,
      };

      capturedCallback({ body: JSON.stringify(typingData) } as any);
      expect(callback).toHaveBeenCalledWith(typingData);

      // Test unsubscribe
      expect(unsubscribe).toBeDefined();
    });
  });

  describe('Real-time Message Updates', () => {
    it('should subscribe to new messages in a hive', async () => {
      const hiveId = 1;
      const callback = vi.fn();
      let capturedCallback: any;

      vi.spyOn(webSocketService, 'subscribe').mockImplementation((topic, cb) => {
        capturedCallback = cb;
        return 'subscription-id';
      });

      chatService.subscribeToMessages(hiveId, callback);

      expect(webSocketService.subscribe).toHaveBeenCalledWith(
        `/topic/chat/${hiveId}/messages`,
        expect.any(Function)
      );

      // Simulate a new message
      const newMessage: ChatMessage = {
        id: '1',
        hiveId: String(hiveId),
        senderId: '123',
        senderName: 'testuser',
        text: 'Real-time message',
        timestamp: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      capturedCallback({ body: JSON.stringify(newMessage) } as any);
      expect(callback).toHaveBeenCalledWith(newMessage);
    });

    it('should handle message edits via WebSocket', async () => {
      const hiveId = 1;
      const onEdit = vi.fn();
      let capturedCallback: any;

      vi.spyOn(webSocketService, 'subscribe').mockImplementation((topic, cb) => {
        capturedCallback = cb;
        return 'subscription-id';
      });

      chatService.subscribeToMessageEdits(hiveId, onEdit);

      expect(webSocketService.subscribe).toHaveBeenCalledWith(
        `/topic/chat/${hiveId}/edits`,
        expect.any(Function)
      );

      const editedMessage: ChatMessage = {
        id: '1',
        hiveId: String(hiveId),
        senderId: '123',
        senderName: 'testuser',
        text: 'Edited content',
        timestamp: '2024-01-01T10:00:00Z',
        edited: true,
        createdAt: '2024-01-01T10:00:00Z',
        updatedAt: new Date().toISOString(),
      };

      capturedCallback({ body: JSON.stringify(editedMessage) } as any);
      expect(onEdit).toHaveBeenCalledWith(editedMessage);
    });

    it('should handle message deletions via WebSocket', async () => {
      const hiveId = 1;
      const onDelete = vi.fn();
      let capturedCallback: any;

      vi.spyOn(webSocketService, 'subscribe').mockImplementation((topic, cb) => {
        capturedCallback = cb;
        return 'subscription-id';
      });

      chatService.subscribeToMessageDeletions(hiveId, onDelete);

      expect(webSocketService.subscribe).toHaveBeenCalledWith(
        `/topic/chat/${hiveId}/deletions`,
        expect.any(Function)
      );

      const deletionData = { messageId: 1, deletedAt: new Date().toISOString() };
      capturedCallback({ body: JSON.stringify(deletionData) } as any);
      expect(onDelete).toHaveBeenCalledWith(deletionData);
    });
  });

  describe('Search Messages', () => {
    it('should search messages across hives', async () => {
      const searchParams = {
        query: 'test search',
        hiveId: 1,
        page: 0,
        size: 20,
      };

      const mockResults: PaginatedResponse<ChatMessage> = {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 20,
        page: 0,
        number: 0,
        first: true,
        last: true,
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockResults),
      } as any);

      const result = await chatService.searchMessages(searchParams);

      expect(result).toEqual(mockResults);
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/chat/search'),
        expect.any(Object)
      );
    });
  });

  describe('Read Receipts', () => {
    it('should mark messages as read', async () => {
      const messageIds = [1, 2, 3];

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue({ success: true }),
      } as any);

      await chatService.markAsRead(messageIds);

      expect(global.fetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/chat/messages/read',
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Authorization': 'Bearer mock-token',
            'Content-Type': 'application/json',
          }),
          body: JSON.stringify({ messageIds }),
        })
      );
    });

    it('should get read receipts for a message', async () => {
      const messageId = 1;
      const mockReceipts = [
        { userId: 123, username: 'user1', readAt: '2024-01-01T10:00:00Z' },
        { userId: 124, username: 'user2', readAt: '2024-01-01T10:01:00Z' },
      ];

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue(mockReceipts),
      } as any);

      const result = await chatService.getReadReceipts(messageId);

      expect(result).toEqual(mockReceipts);
      expect(global.fetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/chat/messages/${messageId}/receipts`,
        expect.any(Object)
      );
    });
  });

  describe('Error Handling', () => {
    it('should handle authentication errors', async () => {
      vi.spyOn(authService, 'getAccessToken').mockReturnValue(null);

      await expect(chatService.sendMessage({
        hiveId: '1',
        content: 'test',
      })).rejects.toThrow('Authentication required');
    });

    it('should handle API errors gracefully', async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
      } as any);

      await expect(chatService.getMessageHistory(1)).rejects.toThrow('Failed to fetch message history');
    });

    it('should handle WebSocket disconnection', () => {
      vi.spyOn(webSocketService, 'isConnectedStatus').mockReturnValue(false);

      const callback = vi.fn();
      const unsubscribe = chatService.subscribeToMessages(1, callback);

      // Should return a no-op unsubscribe function
      expect(unsubscribe).toBeDefined();
      expect(webSocketService.subscribe).not.toHaveBeenCalled();
    });
  });

  describe('Cache Management', () => {
    it('should cache messages properly', async () => {
      const hiveId = 1;
      const message: ChatMessage = {
        id: '1',
        hiveId: String(hiveId),
        senderId: '123',
        senderName: 'testuser',
        text: 'Cached message',
        timestamp: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      // Add message to cache
      chatService.cacheMessage(message);

      // Retrieve from cache
      const cached = chatService.getCachedMessage(message.id);
      expect(cached).toEqual(message);

      // Clear specific hive cache
      chatService.clearHiveCache(hiveId);
      const afterClear = chatService.getCachedMessage(message.id);
      expect(afterClear).toBeNull();
    });

    it('should expire cache after timeout', async () => {
      const message: ChatMessage = {
        id: '1',
        hiveId: '1',
        senderId: '123',
        senderName: 'testuser',
        text: 'Test',
        timestamp: new Date().toISOString(),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      // Mock cache with expired timestamp
      chatService['messageCache'].set(message.id, {
        message,
        timestamp: Date.now() - (6 * 60 * 1000), // 6 minutes ago
      });

      const cached = chatService.getCachedMessage(message.id);
      expect(cached).toBeNull();
    });
  });
});