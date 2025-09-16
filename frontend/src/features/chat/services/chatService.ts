import { webSocketService } from '@/services/websocket/WebSocketService';
import { authService } from '@/services/auth/authService';
import type {
  ChatMessage,
  SendMessageRequest,
  UpdateMessageRequest,
  ChatTypingIndicator,
  ChatHistory,
  ChatSearchParams,
  ChatReadReceipt,
  MessageReaction,
  ChatWebSocketEvent,
  BatchMarkAsReadRequest,
} from '@/contracts/chat';
import type { PaginatedResponse } from '@/contracts/common';
import type { IMessage } from '@stomp/stompjs';

/**
 * ChatService - Business logic layer for chat functionality
 *
 * This service provides:
 * - Message sending and receiving
 * - Real-time updates via WebSocket
 * - Message history with pagination
 * - Edit/delete operations
 * - Reactions and mentions
 * - Typing indicators
 * - Read receipts
 * - Message caching
 * - Optimistic updates
 */
export class ChatService {
  private apiUrl = 'http://localhost:8080/api/v1/chat';
  private messageCache: Map<number, { message: ChatMessage; timestamp: number }> = new Map();
  private historyCache: Map<number, { history: ChatHistory; timestamp: number }> = new Map();
  private optimisticMessages: Map<string, ChatMessage> = new Map();
  private cacheTimeout = 5 * 60 * 1000; // 5 minutes
  private subscriptions: Map<string, () => void> = new Map();
  private typingTimers: Map<number, NodeJS.Timeout> = new Map();

  constructor() {}

  /**
   * Send a message to a hive
   */
  async sendMessage(request: SendMessageRequest): Promise<ChatMessage> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/messages`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error(`Failed to send message: ${response.status} ${response.statusText}`);
      }

      const message = await response.json();

      // Cache the new message
      this.cacheMessage(message);

      // Notify via WebSocket
      this.notifyNewMessage(message);

      return message;
    } catch (error) {
      console.error('Error sending message:', error);
      throw error;
    }
  }

  /**
   * Send a message with optimistic update
   */
  async sendMessageOptimistic(request: SendMessageRequest): Promise<string> {
    const optimisticId = `optimistic-${Date.now()}-${Math.random()}`;

    // Create optimistic message
    const optimisticMessage: ChatMessage = {
      id: -1,
      hiveId: request.hiveId,
      userId: authService.getCurrentUser()?.id || -1,
      username: authService.getCurrentUser()?.username || 'You',
      avatar: authService.getCurrentUser()?.avatar,
      content: request.content,
      type: request.type || 'TEXT',
      status: 'SENDING',
      parentMessageId: request.parentMessageId,
      mentions: request.mentions,
      reactions: [],
      attachments: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    // Store optimistic message
    this.optimisticMessages.set(optimisticId, optimisticMessage);

    // Send actual message in background
    this.sendMessage(request)
      .then(message => {
        // Replace optimistic with real message
        this.optimisticMessages.delete(optimisticId);
        this.cacheMessage(message);
      })
      .catch(error => {
        // Mark optimistic message as failed
        const failed = this.optimisticMessages.get(optimisticId);
        if (failed) {
          failed.status = 'FAILED';
        }
        console.error('Failed to send message:', error);
      });

    return optimisticId;
  }

  /**
   * Get optimistic message by ID
   */
  getOptimisticMessage(optimisticId: string): ChatMessage | null {
    return this.optimisticMessages.get(optimisticId) || null;
  }

  /**
   * Get message history for a hive
   */
  async getMessageHistory(
    hiveId: number,
    options?: { beforeMessageId?: number; limit?: number }
  ): Promise<ChatHistory> {
    // Check cache first
    const cached = this.getCachedHistory(hiveId);
    if (cached && !options) {
      return cached;
    }

    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      let url = `${this.apiUrl}/hives/${hiveId}/history`;
      if (options) {
        const params = new URLSearchParams();
        if (options.beforeMessageId) params.append('before', options.beforeMessageId.toString());
        if (options.limit) params.append('limit', options.limit.toString());
        url += `?${params}`;
      }

      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch message history: ${response.status} ${response.statusText}`);
      }

      const history = await response.json();

      // Cache history (only if no options)
      if (!options) {
        this.cacheHistory(hiveId, history);
      }

      // Cache individual messages
      history.messages?.forEach((msg: ChatMessage) => this.cacheMessage(msg));

      return history;
    } catch (error) {
      console.error('Error fetching message history:', error);
      throw error;
    }
  }

  /**
   * Update an existing message
   */
  async updateMessage(messageId: number, request: UpdateMessageRequest): Promise<ChatMessage> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/messages/${messageId}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error(`Failed to update message: ${response.status} ${response.statusText}`);
      }

      const message = await response.json();

      // Update cache
      this.cacheMessage(message);

      // Notify via WebSocket
      this.notifyMessageEdit(message);

      return message;
    } catch (error) {
      console.error('Error updating message:', error);
      throw error;
    }
  }

  /**
   * Delete a message
   */
  async deleteMessage(messageId: number, options?: { soft?: boolean }): Promise<ChatMessage | void> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const url = options?.soft
        ? `${this.apiUrl}/messages/${messageId}?soft=true`
        : `${this.apiUrl}/messages/${messageId}`;

      const response = await fetch(url, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to delete message: ${response.status} ${response.statusText}`);
      }

      if (options?.soft) {
        const message = await response.json();
        this.cacheMessage(message);
        this.notifyMessageDeletion(message.hiveId, messageId);
        return message;
      } else {
        // Remove from cache
        this.messageCache.delete(messageId);
        return;
      }
    } catch (error) {
      console.error('Error deleting message:', error);
      throw error;
    }
  }

  /**
   * Add a reaction to a message
   */
  async addReaction(messageId: number, emoji: string): Promise<void> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/messages/${messageId}/reactions`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ emoji }),
      });

      if (!response.ok) {
        throw new Error(`Failed to add reaction: ${response.status} ${response.statusText}`);
      }

      // Update cached message if exists
      const cached = this.getCachedMessage(messageId);
      if (cached) {
        const user = authService.getCurrentUser();
        if (user && !cached.reactions) {
          cached.reactions = [];
        }
        cached.reactions?.push({
          emoji,
          userId: user?.id || -1,
          username: user?.username || 'Unknown',
          createdAt: new Date().toISOString(),
        });
        this.cacheMessage(cached);
      }
    } catch (error) {
      console.error('Error adding reaction:', error);
      throw error;
    }
  }

  /**
   * Remove a reaction from a message
   */
  async removeReaction(messageId: number, emoji: string): Promise<void> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/messages/${messageId}/reactions/${emoji}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to remove reaction: ${response.status} ${response.statusText}`);
      }

      // Update cached message if exists
      const cached = this.getCachedMessage(messageId);
      if (cached && cached.reactions) {
        const userId = authService.getCurrentUser()?.id;
        cached.reactions = cached.reactions.filter(r =>
          !(r.emoji === emoji && r.userId === userId)
        );
        this.cacheMessage(cached);
      }
    } catch (error) {
      console.error('Error removing reaction:', error);
      throw error;
    }
  }

  /**
   * Send typing indicator
   */
  sendTypingIndicator(hiveId: number, isTyping: boolean): void {
    const destination = '/app/chat/typing';
    const payload: ChatTypingIndicator = {
      hiveId,
      userId: authService.getCurrentUser()?.id || -1,
      username: authService.getCurrentUser()?.username || 'Unknown',
      isTyping,
      timestamp: new Date().toISOString(),
    };

    webSocketService.sendMessage(destination, payload);

    // Clear any existing typing timer for this hive
    const existingTimer = this.typingTimers.get(hiveId);
    if (existingTimer) {
      clearTimeout(existingTimer);
    }

    // Auto-stop typing after 5 seconds
    if (isTyping) {
      const timer = setTimeout(() => {
        this.sendTypingIndicator(hiveId, false);
      }, 5000);
      this.typingTimers.set(hiveId, timer);
    }
  }

  /**
   * Subscribe to typing indicators
   */
  subscribeToTypingIndicators(hiveId: number, callback: (indicator: ChatTypingIndicator) => void): () => void {
    if (!webSocketService.isConnectedStatus()) {
      return () => {};
    }

    const topic = `/topic/chat/${hiveId}/typing`;

    const subscriptionId = webSocketService.subscribe(topic, (message: IMessage) => {
      try {
        const indicator = JSON.parse(message.body) as ChatTypingIndicator;
        callback(indicator);
      } catch (error) {
        console.error('Failed to parse typing indicator:', error);
      }
    });

    const subscriptionKey = `typing-${hiveId}`;
    const unsubscribe = () => {
      webSocketService.unsubscribe(subscriptionId);
      this.subscriptions.delete(subscriptionKey);
    };

    this.subscriptions.set(subscriptionKey, unsubscribe);
    return unsubscribe;
  }

  /**
   * Subscribe to new messages in a hive
   */
  subscribeToMessages(hiveId: number, callback: (message: ChatMessage) => void): () => void {
    if (!webSocketService.isConnectedStatus()) {
      return () => {};
    }

    const topic = `/topic/chat/${hiveId}/messages`;

    const subscriptionId = webSocketService.subscribe(topic, (message: IMessage) => {
      try {
        const chatMessage = JSON.parse(message.body) as ChatMessage;
        this.cacheMessage(chatMessage);
        callback(chatMessage);
      } catch (error) {
        console.error('Failed to parse chat message:', error);
      }
    });

    const subscriptionKey = `messages-${hiveId}`;
    const unsubscribe = () => {
      webSocketService.unsubscribe(subscriptionId);
      this.subscriptions.delete(subscriptionKey);
    };

    this.subscriptions.set(subscriptionKey, unsubscribe);
    return unsubscribe;
  }

  /**
   * Subscribe to message edits
   */
  subscribeToMessageEdits(hiveId: number, callback: (message: ChatMessage) => void): () => void {
    if (!webSocketService.isConnectedStatus()) {
      return () => {};
    }

    const topic = `/topic/chat/${hiveId}/edits`;

    const subscriptionId = webSocketService.subscribe(topic, (message: IMessage) => {
      try {
        const editedMessage = JSON.parse(message.body) as ChatMessage;
        this.cacheMessage(editedMessage);
        callback(editedMessage);
      } catch (error) {
        console.error('Failed to parse message edit:', error);
      }
    });

    const subscriptionKey = `edits-${hiveId}`;
    const unsubscribe = () => {
      webSocketService.unsubscribe(subscriptionId);
      this.subscriptions.delete(subscriptionKey);
    };

    this.subscriptions.set(subscriptionKey, unsubscribe);
    return unsubscribe;
  }

  /**
   * Subscribe to message deletions
   */
  subscribeToMessageDeletions(hiveId: number, callback: (data: { messageId: number; deletedAt: string }) => void): () => void {
    if (!webSocketService.isConnectedStatus()) {
      return () => {};
    }

    const topic = `/topic/chat/${hiveId}/deletions`;

    const subscriptionId = webSocketService.subscribe(topic, (message: IMessage) => {
      try {
        const deletionData = JSON.parse(message.body);
        // Remove from cache
        if (deletionData.messageId) {
          this.messageCache.delete(deletionData.messageId);
        }
        callback(deletionData);
      } catch (error) {
        console.error('Failed to parse message deletion:', error);
      }
    });

    const subscriptionKey = `deletions-${hiveId}`;
    const unsubscribe = () => {
      webSocketService.unsubscribe(subscriptionId);
      this.subscriptions.delete(subscriptionKey);
    };

    this.subscriptions.set(subscriptionKey, unsubscribe);
    return unsubscribe;
  }

  /**
   * Search messages
   */
  async searchMessages(params: ChatSearchParams): Promise<PaginatedResponse<ChatMessage>> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const queryParams = new URLSearchParams();
      if (params.query) queryParams.append('query', params.query);
      if (params.hiveId) queryParams.append('hiveId', params.hiveId.toString());
      if (params.userId) queryParams.append('userId', params.userId.toString());
      if (params.type) queryParams.append('type', params.type);
      if (params.startDate) queryParams.append('startDate', params.startDate);
      if (params.endDate) queryParams.append('endDate', params.endDate);
      if (params.page !== undefined) queryParams.append('page', params.page.toString());
      if (params.size !== undefined) queryParams.append('size', params.size.toString());

      const response = await fetch(`${this.apiUrl}/search?${queryParams}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to search messages: ${response.status} ${response.statusText}`);
      }

      const results = await response.json();

      // Cache found messages
      results.content?.forEach((msg: ChatMessage) => this.cacheMessage(msg));

      return results;
    } catch (error) {
      console.error('Error searching messages:', error);
      throw error;
    }
  }

  /**
   * Mark messages as read
   */
  async markAsRead(messageIds: number[]): Promise<void> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/messages/read`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ messageIds }),
      });

      if (!response.ok) {
        throw new Error(`Failed to mark messages as read: ${response.status} ${response.statusText}`);
      }

      // Update cached messages
      messageIds.forEach(id => {
        const cached = this.getCachedMessage(id);
        if (cached) {
          cached.status = 'READ';
          this.cacheMessage(cached);
        }
      });
    } catch (error) {
      console.error('Error marking messages as read:', error);
      throw error;
    }
  }

  /**
   * Get read receipts for a message
   */
  async getReadReceipts(messageId: number): Promise<ChatReadReceipt[]> {
    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('Authentication required');
    }

    try {
      const response = await fetch(`${this.apiUrl}/messages/${messageId}/receipts`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch read receipts: ${response.status} ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error fetching read receipts:', error);
      throw error;
    }
  }

  // Cache management methods

  cacheMessage(message: ChatMessage): void {
    this.messageCache.set(message.id, {
      message,
      timestamp: Date.now(),
    });
  }

  getCachedMessage(messageId: number): ChatMessage | null {
    const cached = this.messageCache.get(messageId);
    if (!cached) return null;

    // Check if cache is expired
    if (Date.now() - cached.timestamp > this.cacheTimeout) {
      this.messageCache.delete(messageId);
      return null;
    }

    return cached.message;
  }

  private cacheHistory(hiveId: number, history: ChatHistory): void {
    this.historyCache.set(hiveId, {
      history,
      timestamp: Date.now(),
    });
  }

  private getCachedHistory(hiveId: number): ChatHistory | null {
    const cached = this.historyCache.get(hiveId);
    if (!cached) return null;

    // Check if cache is expired
    if (Date.now() - cached.timestamp > this.cacheTimeout) {
      this.historyCache.delete(hiveId);
      return null;
    }

    return cached.history;
  }

  clearCache(): void {
    this.messageCache.clear();
    this.historyCache.clear();
    this.optimisticMessages.clear();
  }

  clearHiveCache(hiveId: number): void {
    // Clear history cache
    this.historyCache.delete(hiveId);

    // Clear message cache for this hive
    for (const [messageId, cached] of this.messageCache.entries()) {
      if (cached.message.hiveId === hiveId) {
        this.messageCache.delete(messageId);
      }
    }
  }

  // Private helper methods

  private notifyNewMessage(message: ChatMessage): void {
    const destination = '/app/chat/message';
    webSocketService.sendMessage(destination, {
      type: 'MESSAGE',
      hiveId: message.hiveId,
      message,
      timestamp: new Date().toISOString(),
    });
  }

  private notifyMessageEdit(message: ChatMessage): void {
    const destination = '/app/chat/edit';
    webSocketService.sendMessage(destination, {
      type: 'EDIT',
      hiveId: message.hiveId,
      message,
      timestamp: new Date().toISOString(),
    });
  }

  private notifyMessageDeletion(hiveId: number, messageId: number): void {
    const destination = '/app/chat/delete';
    webSocketService.sendMessage(destination, {
      type: 'DELETE',
      hiveId,
      messageId,
      deletedAt: new Date().toISOString(),
    });
  }

  /**
   * Cleanup method to be called when service is destroyed
   */
  cleanup(): void {
    // Clear all subscriptions
    this.subscriptions.forEach(unsub => unsub());
    this.subscriptions.clear();

    // Clear all typing timers
    this.typingTimers.forEach(timer => clearTimeout(timer));
    this.typingTimers.clear();

    // Clear cache
    this.clearCache();
  }
}

// Export singleton instance
export const chatService = new ChatService();