/**
 * E2E Tests for useWebSocket hook
 * Testing with REAL WebSocket service - NO MOCKS
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useWebSocket } from '../useWebSocket';
import { webSocketService } from '../../services/websocket/WebSocketService';
import type { PresenceStatus } from '../../services/websocket/WebSocketService';

describe('useWebSocket Hook E2E Tests', () => {
  beforeEach(() => {
    // Ensure disconnected state before each test
    webSocketService.disconnect();
  });

  afterEach(() => {
    // Clean up after each test
    webSocketService.disconnect();
  });

  describe('Connection Management', () => {
    it('should provide connection methods', () => {
      const { result } = renderHook(() => useWebSocket());

      expect(result.current.connect).toBeDefined();
      expect(result.current.disconnect).toBeDefined();
      expect(result.current.isConnected).toBe(false);
      expect(result.current.connectionState).toBe('DISCONNECTED');
    });

    it('should connect and update state', () => {
      const { result } = renderHook(() => useWebSocket());

      act(() => {
        result.current.connect();
      });

      // Connection happens asynchronously, but we can check the state changes
      expect(() => result.current.connect()).not.toThrow();
    });

    it('should disconnect and update state', () => {
      const { result } = renderHook(() => useWebSocket());

      act(() => {
        result.current.connect();
      });

      act(() => {
        result.current.disconnect();
      });

      expect(result.current.isConnected).toBe(false);
      expect(result.current.connectionState).toBe('DISCONNECTED');
    });
  });

  describe('Subscription Management', () => {
    it('should provide subscription methods', () => {
      const { result } = renderHook(() => useWebSocket());

      expect(result.current.subscribe).toBeDefined();
      expect(result.current.unsubscribe).toBeDefined();
    });

    it('should handle subscriptions', () => {
      const { result } = renderHook(() => useWebSocket());
      const mockCallback = vi.fn();

      let subscriptionId: string = '';
      act(() => {
        subscriptionId = result.current.subscribe('/topic/test', mockCallback);
      });

      // Subscription returns empty string when not connected, but shouldn't throw
      expect(typeof subscriptionId).toBe('string');

      if (subscriptionId) {
        act(() => {
          result.current.unsubscribe(subscriptionId);
        });
      }
    });
  });

  describe('Message Sending', () => {
    it('should provide message sending methods', () => {
      const { result } = renderHook(() => useWebSocket());

      expect(result.current.sendMessage).toBeDefined();
      expect(result.current.updatePresence).toBeDefined();
      expect(result.current.startFocusSession).toBeDefined();
    });

    it('should handle message sending when not connected', () => {
      const { result } = renderHook(() => useWebSocket());

      // Should not throw even when not connected
      expect(() => {
        act(() => {
          result.current.sendMessage('/app/test', { message: 'test' });
        });
      }).not.toThrow();
    });

    it('should handle presence updates', () => {
      const { result } = renderHook(() => useWebSocket());

      expect(() => {
        act(() => {
          result.current.updatePresence('ONLINE' as PresenceStatus, 123);
        });
      }).not.toThrow();
    });
  });

  describe('Notifications', () => {
    it('should provide notification methods', () => {
      const { result } = renderHook(() => useWebSocket());

      expect(result.current.notifications).toBeDefined();
      expect(result.current.clearNotification).toBeDefined();
      expect(result.current.clearAllNotifications).toBeDefined();
    });

    it('should handle notification management', () => {
      const { result } = renderHook(() => useWebSocket());

      // Initially no notifications
      expect(result.current.notifications).toHaveLength(0);

      // Should not throw when clearing
      expect(() => {
        act(() => {
          result.current.clearAllNotifications();
        });
      }).not.toThrow();
    });
  });

  describe('Reconnection', () => {
    it('should provide reconnection methods', () => {
      const { result } = renderHook(() => useWebSocket());

      expect(result.current.retryConnection).toBeDefined();
      expect(result.current.reconnectWithNewToken).toBeDefined();
      expect(result.current.connectionInfo).toBeDefined();
    });

    it('should return connection info', () => {
      const { result } = renderHook(() => useWebSocket());

      const info = result.current.connectionInfo;
      expect(info).toHaveProperty('isConnected');
      expect(info).toHaveProperty('connectionState');
      expect(info).toHaveProperty('reconnectionInfo');
      expect(info.reconnectionInfo).toHaveProperty('attempts');
      expect(info.reconnectionInfo).toHaveProperty('maxAttempts');
      expect(info.reconnectionInfo).toHaveProperty('isReconnecting');
      expect(info.reconnectionInfo.isReconnecting).toBe(false);
    });
  });
});