import {beforeEach, describe, expect, it, vi} from 'vitest';
import {act, renderHook, waitFor} from '@testing-library/react';
import {useForumWebSocket, useWebSocket} from './useWebSocket';
import type {WebSocketMessage} from '../services/websocket/WebSocketService';
import {PresenceStatus} from '../services/websocket/WebSocketService';

// Mock the base useWebSocket hook
vi.mock('./useWebSocket', async (importOriginal) => {
  const actual = await importOriginal() as Record<string, unknown>;
  return {
    ...actual,
    useWebSocket: vi.fn()
  };
});

// Define types for forum system
interface ForumPost {
  id: number;
  title: string;
  content: string;
  authorId: number;
  authorName: string;
  categoryId: number;
  tags: string[];
  upvotes: number;
  downvotes: number;
  replyCount: number;
  isResolved: boolean;
  createdAt: string;
  updatedAt: string;
}

interface ForumReply {
  id: number;
  postId: number;
  content: string;
  authorId: number;
  authorName: string;
  upvotes: number;
  downvotes: number;
  isAccepted: boolean;
  parentReplyId?: number;
  createdAt: string;
  updatedAt: string;
}

describe('useForumWebSocket', () => {
  const mockUseWebSocket = vi.mocked(useWebSocket);
  const mockWebSocketService = {
    subscribeToForumPost: vi.fn(() => 'forum-sub-id'),
    createForumPost: vi.fn(),
    createForumReply: vi.fn(),
    voteOnPost: vi.fn(),
    voteOnReply: vi.fn(),
    acceptReply: vi.fn(),
    editForumPost: vi.fn(),
    setTypingStatus: vi.fn()
  };

  const mockWebSocketReturn = {
    isConnected: false,
    connectionState: 'DISCONNECTED',
    connectionInfo: {
      isConnected: false,
      connectionState: 'DISCONNECTED',
      reconnectionInfo: {
        attempts: 0,
        maxAttempts: 10,
        isReconnecting: false
      }
    },
    connect: vi.fn(),
    disconnect: vi.fn(),
    retryConnection: vi.fn(),
    reconnectWithNewToken: vi.fn(),
    sendMessage: vi.fn(),
    subscribe: vi.fn(),
    unsubscribe: vi.fn(),
    presenceStatus: PresenceStatus.OFFLINE,
    updatePresence: vi.fn(),
    startFocusSession: vi.fn(),
    notifications: [],
    clearNotification: vi.fn(),
    clearAllNotifications: vi.fn(),
    service: mockWebSocketService as unknown as typeof import('../services/websocket/WebSocketService').default
  };

  let capturedMessageHandler: ((message: WebSocketMessage) => void) | undefined;

  beforeEach(() => {
    vi.clearAllMocks();

    mockUseWebSocket.mockImplementation((_options) => {
      // Capture the message handler
      capturedMessageHandler = _options?.onMessage;

      return mockWebSocketReturn;
    });
  });

  it('should initialize with default state', () => {
    const {result} = renderHook(() => useForumWebSocket());

    expect(result.current.forumMessages).toEqual([]);
    expect(result.current.typingUsers).toEqual(new Map());

    // Should have all base WebSocket properties
    expect(result.current.isConnected).toBe(false);
    expect(result.current.connectionState).toBe('DISCONNECTED');

    // Should have forum-specific methods
    expect(typeof result.current.createPost).toBe('function');
    expect(typeof result.current.createReply).toBe('function');
    expect(typeof result.current.voteOnPost).toBe('function');
    expect(typeof result.current.voteOnReply).toBe('function');
    expect(typeof result.current.acceptReply).toBe('function');
    expect(typeof result.current.editPost).toBe('function');
    expect(typeof result.current.setTyping).toBe('function');
  });

  it('should filter forum messages from general messages', async () => {
    const {result} = renderHook(() => useForumWebSocket());

    // Simulate forum message
    const forumMessage: WebSocketMessage = {
      id: 'msg-1',
      type: 'FORUM_NEW_POST' as WebSocketMessage['type'],
      event: 'forum_new_post',
      payload: {postId: 123, title: 'New Forum Post'},
      timestamp: new Date().toISOString()
    };

    act(() => {
      capturedMessageHandler?.(forumMessage);
    });

    await waitFor(() => {
      expect(result.current.forumMessages).toHaveLength(1);
      expect(result.current.forumMessages[0]).toEqual(forumMessage);
    });

    // Simulate non-forum message
    const generalMessage: WebSocketMessage = {
      id: 'msg-2',
      type: 'NOTIFICATION' as WebSocketMessage['type'],
      event: 'notification',
      payload: {message: 'General notification'},
      timestamp: new Date().toISOString()
    };

    act(() => {
      capturedMessageHandler?.(generalMessage);
    });

    await waitFor(() => {
      // Should still only have the forum message
      expect(result.current.forumMessages).toHaveLength(1);
    });
  });

  it('should handle typing status messages', async () => {
    const {result} = renderHook(() => useForumWebSocket());

    // User starts typing
    const typingMessage: WebSocketMessage = {
      id: 'msg-1',
      type: 'USER_TYPING' as WebSocketMessage['type'],
      event: 'user_typing',
      payload: {userId: 123, username: 'testuser'},
      timestamp: new Date().toISOString()
    };

    act(() => {
      capturedMessageHandler?.(typingMessage);
    });

    await waitFor(() => {
      expect(result.current.typingUsers.has(123)).toBe(true);
      expect(result.current.typingUsers.get(123)).toBe('testuser');
    });

    // User stops typing
    const stoppedTypingMessage: WebSocketMessage = {
      id: 'msg-2',
      type: 'USER_STOPPED_TYPING' as WebSocketMessage['type'],
      event: 'user_stopped_typing',
      payload: {userId: 123, username: 'testuser'},
      timestamp: new Date().toISOString()
    };

    act(() => {
      capturedMessageHandler?.(stoppedTypingMessage);
    });

    await waitFor(() => {
      expect(result.current.typingUsers.has(123)).toBe(false);
    });
  });

  it('should handle multiple users typing simultaneously', async () => {
    const {result} = renderHook(() => useForumWebSocket());

    const users = [
      {userId: 123, username: 'user1'},
      {userId: 456, username: 'user2'},
      {userId: 789, username: 'user3'}
    ];

    // All users start typing
    for (const user of users) {
      const typingMessage: WebSocketMessage = {
        id: `msg-${user.userId}`,
        type: 'USER_TYPING' as WebSocketMessage['type'],
        event: 'user_typing',
        payload: user,
        timestamp: new Date().toISOString()
      };

      act(() => {
        capturedMessageHandler?.(typingMessage);
      });
    }

    await waitFor(() => {
      expect(result.current.typingUsers.size).toBe(3);
      expect(result.current.typingUsers.get(123)).toBe('user1');
      expect(result.current.typingUsers.get(456)).toBe('user2');
      expect(result.current.typingUsers.get(789)).toBe('user3');
    });

    // One user stops typing
    const stoppedTypingMessage: WebSocketMessage = {
      id: 'msg-stop-456',
      type: 'USER_STOPPED_TYPING' as WebSocketMessage['type'],
      event: 'user_stopped_typing',
      payload: {userId: 456, username: 'user2'},
      timestamp: new Date().toISOString()
    };

    act(() => {
      capturedMessageHandler?.(stoppedTypingMessage);
    });

    await waitFor(() => {
      expect(result.current.typingUsers.size).toBe(2);
      expect(result.current.typingUsers.has(456)).toBe(false);
      expect(result.current.typingUsers.has(123)).toBe(true);
      expect(result.current.typingUsers.has(789)).toBe(true);
    });
  });

  it('should subscribe to forum post when connected with postId', async () => {
    const postId = 123;

    // Mock connected state
    mockUseWebSocket.mockImplementation((_options) => {
      capturedMessageHandler = _options?.onMessage;

      return {
        ...mockWebSocketReturn,
        isConnected: true
      };
    });

    const {result} = renderHook(() => useForumWebSocket(postId));

    await waitFor(() => {
      expect(mockWebSocketService.subscribeToForumPost).toHaveBeenCalledWith(postId);
    });

    // Verify unsubscribe on unmount
    const {unmount} = renderHook(() => useForumWebSocket(postId));
    unmount();

    expect(result.current.unsubscribe).toHaveBeenCalledWith('forum-sub-id');
  });

  it('should not subscribe when not connected', () => {
    const postId = 123;

    renderHook(() => useForumWebSocket(postId));

    expect(mockWebSocketService.subscribeToForumPost).not.toHaveBeenCalled();
  });

  it('should not subscribe when no postId provided', async () => {
    // Mock connected state
    mockUseWebSocket.mockImplementation((_options) => ({
      ...mockWebSocketReturn,
      isConnected: true
    }));

    renderHook(() => useForumWebSocket());

    await waitFor(() => {
      expect(mockWebSocketService.subscribeToForumPost).not.toHaveBeenCalled();
    });
  });

  it('should handle forum post creation', () => {
    const {result} = renderHook(() => useForumWebSocket());

    const postData: Partial<ForumPost> = {
      title: 'New Forum Post',
      content: 'This is the content of the post',
      categoryId: 1,
      tags: ['javascript', 'react']
    };

    act(() => {
      result.current.createPost(postData);
    });

    expect(mockWebSocketService.createForumPost).toHaveBeenCalledWith(postData);
  });

  it('should handle forum reply creation', () => {
    const {result} = renderHook(() => useForumWebSocket());

    const replyData: Partial<ForumReply> = {
      postId: 123,
      content: 'This is my reply to the post',
      parentReplyId: 456
    };

    act(() => {
      result.current.createReply(replyData);
    });

    expect(mockWebSocketService.createForumReply).toHaveBeenCalledWith(replyData);
  });

  it('should handle voting on posts and replies', () => {
    const {result} = renderHook(() => useForumWebSocket());

    // Test voting on post
    act(() => {
      result.current.voteOnPost(123, 1);
    });
    expect(mockWebSocketService.voteOnPost).toHaveBeenCalledWith(123, 1);

    // Test voting on reply
    act(() => {
      result.current.voteOnReply(456, -1);
    });
    expect(mockWebSocketService.voteOnReply).toHaveBeenCalledWith(456, -1);
  });

  it('should handle accepting replies', () => {
    const {result} = renderHook(() => useForumWebSocket());

    const replyId = 789;

    act(() => {
      result.current.acceptReply(replyId);
    });

    expect(mockWebSocketService.acceptReply).toHaveBeenCalledWith(replyId);
  });

  it('should handle post editing', () => {
    const {result} = renderHook(() => useForumWebSocket());

    const postId = 123;
    const postData: Partial<ForumPost> = {
      title: 'Updated Title',
      content: 'Updated content',
      tags: ['updated', 'post']
    };

    act(() => {
      result.current.editPost(postId, postData);
    });

    expect(mockWebSocketService.editForumPost).toHaveBeenCalledWith(postId, postData);
  });

  it('should handle typing status management', () => {
    const {result} = renderHook(() => useForumWebSocket());

    const location = 'post-123';

    // Start typing
    act(() => {
      result.current.setTyping(location, true);
    });
    expect(mockWebSocketService.setTypingStatus).toHaveBeenCalledWith(location, true);

    // Stop typing
    act(() => {
      result.current.setTyping(location, false);
    });
    expect(mockWebSocketService.setTypingStatus).toHaveBeenCalledWith(location, false);
  });

  it('should accumulate multiple forum messages', async () => {
    const {result} = renderHook(() => useForumWebSocket());

    const messages = [
      {
        id: 'msg-1',
        type: 'FORUM_NEW_POST' as WebSocketMessage['type'],
        event: 'forum_new_post',
        payload: {postId: 123, title: 'New Post'},
        timestamp: new Date().toISOString()
      },
      {
        id: 'msg-2',
        type: 'FORUM_NEW_REPLY' as WebSocketMessage['type'],
        event: 'forum_new_reply',
        payload: {replyId: 456, postId: 123},
        timestamp: new Date().toISOString()
      },
      {
        id: 'msg-3',
        type: 'FORUM_POST_VOTED' as WebSocketMessage['type'],
        event: 'forum_post_voted',
        payload: {postId: 123, votes: 5},
        timestamp: new Date().toISOString()
      }
    ];

    for (const message of messages) {
      act(() => {
        capturedMessageHandler?.(message);
      });
    }

    await waitFor(() => {
      expect(result.current.forumMessages).toHaveLength(3);
      expect(result.current.forumMessages).toEqual(messages);
    });
  });

  it('should handle subscription errors gracefully', () => {
    const postId = 123;

    // Mock service to return null (error case)
    mockWebSocketService.subscribeToForumPost.mockReturnValueOnce(null);

    // Mock connected state
    mockUseWebSocket.mockImplementation((_options) => ({
      ...mockWebSocketReturn,
      isConnected: true
    }));

    const {unmount} = renderHook(() => useForumWebSocket(postId));

    expect(mockWebSocketService.subscribeToForumPost).toHaveBeenCalledWith(postId);

    // Should not attempt to unsubscribe with null subscription
    unmount();
    expect(mockWebSocketReturn.unsubscribe).not.toHaveBeenCalled();
  });

  it('should resubscribe when postId changes', async () => {
    // Mock connected state
    mockUseWebSocket.mockImplementation((_options) => ({
      ...mockWebSocketReturn,
      isConnected: true
    }));

    const {rerender} = renderHook(
        ({postId}: { postId?: number }) => useForumWebSocket(postId),
        {initialProps: {postId: 123}}
    );

    await waitFor(() => {
      expect(mockWebSocketService.subscribeToForumPost).toHaveBeenCalledWith(123);
    });

    // Change post ID
    rerender({postId: 456});

    await waitFor(() => {
      expect(mockWebSocketReturn.unsubscribe).toHaveBeenCalledWith('forum-sub-id');
      expect(mockWebSocketService.subscribeToForumPost).toHaveBeenCalledWith(456);
    });
  });

  it('should handle connection state changes properly', async () => {
    const postId = 123;
    let isConnected = false;

    mockUseWebSocket.mockImplementation((_options) => ({
      ...mockWebSocketReturn,
      isConnected
    }));

    const {rerender} = renderHook(() => useForumWebSocket(postId));

    // Initially not connected - should not subscribe
    expect(mockWebSocketService.subscribeToForumPost).not.toHaveBeenCalled();

    // Simulate connection
    isConnected = true;
    rerender();

    await waitFor(() => {
      expect(mockWebSocketService.subscribeToForumPost).toHaveBeenCalledWith(postId);
    });
  });

  it('should preserve all base useWebSocket functionality', () => {
    const {result} = renderHook(() => useForumWebSocket());

    // Check that all base properties are available
    expect(result.current.isConnected).toBe(mockWebSocketReturn.isConnected);
    expect(result.current.connectionState).toBe(mockWebSocketReturn.connectionState);
    expect(result.current.connect).toBe(mockWebSocketReturn.connect);
    expect(result.current.disconnect).toBe(mockWebSocketReturn.disconnect);
    expect(result.current.sendMessage).toBe(mockWebSocketReturn.sendMessage);
    expect(result.current.subscribe).toBe(mockWebSocketReturn.subscribe);
    expect(result.current.unsubscribe).toBe(mockWebSocketReturn.unsubscribe);
    expect(result.current.updatePresence).toBe(mockWebSocketReturn.updatePresence);
    expect(result.current.startFocusSession).toBe(mockWebSocketReturn.startFocusSession);
    expect(result.current.notifications).toBe(mockWebSocketReturn.notifications);
    expect(result.current.clearNotification).toBe(mockWebSocketReturn.clearNotification);
    expect(result.current.clearAllNotifications).toBe(mockWebSocketReturn.clearAllNotifications);
    expect(result.current.service).toBe(mockWebSocketReturn.service);
  });

  it('should handle mixed message types correctly', async () => {
    const {result} = renderHook(() => useForumWebSocket());

    // Mix of forum and typing messages
    const messages = [
      {
        id: 'msg-1',
        type: 'FORUM_NEW_POST' as WebSocketMessage['type'],
        event: 'forum_new_post',
        payload: {postId: 123},
        timestamp: new Date().toISOString()
      },
      {
        id: 'msg-2',
        type: 'USER_TYPING' as WebSocketMessage['type'],
        event: 'user_typing',
        payload: {userId: 456, username: 'typinguser'},
        timestamp: new Date().toISOString()
      },
      {
        id: 'msg-3',
        type: 'NOTIFICATION' as WebSocketMessage['type'],
        event: 'notification',
        payload: {message: 'general'},
        timestamp: new Date().toISOString()
      },
      {
        id: 'msg-4',
        type: 'USER_STOPPED_TYPING' as WebSocketMessage['type'],
        event: 'user_stopped_typing',
        payload: {userId: 456, username: 'typinguser'},
        timestamp: new Date().toISOString()
      }
    ];

    for (const message of messages) {
      act(() => {
        capturedMessageHandler?.(message);
      });
    }

    await waitFor(() => {
      // Should have one forum message
      expect(result.current.forumMessages).toHaveLength(1);
      expect(result.current.forumMessages[0].type).toBe('FORUM_NEW_POST');

      // Should have no typing users (started and stopped)
      expect(result.current.typingUsers.size).toBe(0);
    });
  });
});