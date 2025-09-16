// Enhanced token manager mock with event emitter capabilities
import { vi } from 'vitest';

type TokenEvents = {
  tokenRefreshNeeded: void;
  authFailure: void;
  tokenUpdated: { token: string; refreshToken: string };
};

interface TokenManagerMock {
  saveTokens: (accessToken: string, refreshToken: string) => boolean;
  getAccessToken: () => string | null;
  getRefreshToken: () => string | null;
  clearTokens: () => boolean;
  isTokenExpired: (token: string) => boolean;
  parseJWT: (token: string) => any;
  validateToken: (token: string) => boolean;
  hasValidTokens: () => boolean;
  getTokenExpirationInfo: (token: string) => any;
  getUserFromToken: () => any;
  supportsHttpOnlyCookies: () => boolean;
  on: (event: keyof TokenEvents, handler: () => void) => TokenManagerMock;
  off: (event: keyof TokenEvents, handler: () => void) => TokenManagerMock;
  init: () => Promise<TokenManagerMock>;
  destroy: () => Promise<boolean>;
  refreshToken: () => Promise<any>;
  triggerEvent: (eventName: keyof TokenEvents, payload?: unknown) => void;
  setTokenExpiry: (isExpired: boolean) => void;
  setHasValidTokens: (isValid: boolean) => void;
  reset: () => void;
}

// Simple event emitter implementation
class SimpleEventEmitter {
  private events = new Map<string, Set<(...args: any[]) => void>>();
  
  on(event: string, handler: (...args: unknown[]) => void): void {
    if (!this.events.has(event)) {
      this.events.set(event, new Set());
    }
    this.events.get(event)?.add(handler);
  }
  
  off(event: string, handler: (...args: unknown[]) => void): void {
    const handlers = this.events.get(event);
    if (handlers) {
      handlers.delete(handler);
    }
  }
  
  emit(event: string, ...args: unknown[]): void {
    const handlers = this.events.get(event);
    if (handlers) {
      handlers.forEach(handler => handler(...args));
    }
  }
  
  clear(): void {
    this.events.clear();
  }
}

export const createTokenManagerMock = (): TokenManagerMock => {
  // Create an event emitter for the token manager
  const emitter = new SimpleEventEmitter();
  
  // Mock token storage
  let mockAccessToken: string | null = null;
  let mockRefreshToken: string | null = null;
  
  // User data mock
  const mockUser = {
    id: '1',
    email: 'testuser@example.com',
    username: 'testuser',
    firstName: 'Test',
    lastName: 'User',
    name: 'Test User',
    avatar: null,
    profilePicture: null,
    isEmailVerified: true,
    isVerified: true,
    createdAt: '2023-01-01T00:00:00Z',
    updatedAt: '2023-01-01T00:00:00Z',
  };

  const tokenManagerMock = {
    // Core token methods
    saveTokens: vi.fn((accessToken: string, refreshToken: string) => {
      mockAccessToken = accessToken;
      mockRefreshToken = refreshToken;
      emitter.emit('tokenUpdated', { token: accessToken, refreshToken });
      return true;
    }),
    
    getAccessToken: vi.fn(() => mockAccessToken),
    
    getRefreshToken: vi.fn(() => mockRefreshToken),
    
    clearTokens: vi.fn(() => {
      mockAccessToken = null;
      mockRefreshToken = null;
      return true;
    }),
    
    // Token validation methods
    isTokenExpired: vi.fn((_token: string) => {
      // By default tokens are not expired
      return false; 
    }),
    
    parseJWT: vi.fn((_token: string) => {
      // Return a parsed token with standard claims
      return {
        sub: '1',
        email: 'testuser@example.com',
        name: 'Test User',
        exp: Math.floor(Date.now() / 1000) + 3600, // 1 hour from now
        iat: Math.floor(Date.now() / 1000) - 60,   // 1 minute ago
      };
    }),
    
    validateToken: vi.fn((_token: string) => {
      return !tokenManagerMock.isTokenExpired(_token);
    }),
    
    hasValidTokens: vi.fn(() => {
      return !!mockAccessToken && !!mockRefreshToken && 
        !tokenManagerMock.isTokenExpired(mockAccessToken);
    }),
    
    getTokenExpirationInfo: vi.fn((_token: string) => {
      return {
        expiresAt: Date.now() + 3600000, // 1 hour from now
        issuedAt: Date.now() - 60000,    // 1 minute ago
        isExpired: false,
        timeRemaining: 3600,  // seconds
      };
    }),
    
    getUserFromToken: vi.fn(() => mockUser),
    
    // Storage type detection
    supportsHttpOnlyCookies: vi.fn().mockReturnValue(false),
    
    // Event handling
    on: vi.fn((event: keyof TokenEvents, handler: () => void) => {
      emitter.on(event, handler);
      return tokenManagerMock; // For chaining
    }),
    
    off: vi.fn((event: keyof TokenEvents, handler: () => void) => {
      emitter.off(event, handler);
      return tokenManagerMock; // For chaining
    }),
    
    // Lifecycle methods
    init: vi.fn(() => Promise.resolve(tokenManagerMock)),
    
    destroy: vi.fn(() => {
      emitter.clear();
      return Promise.resolve(true);
    }),
    
    // Refresh handling
    refreshToken: vi.fn(() => Promise.resolve({
      token: 'new-access-token',
      refreshToken: 'new-refresh-token',
      user: mockUser
    })),
    
    // Additional utilities for testing
    triggerEvent: (eventName: keyof TokenEvents, payload?: unknown) => {
      emitter.emit(eventName, payload);
    },
    
    setTokenExpiry: (isExpired: boolean) => {
      tokenManagerMock.isTokenExpired.mockImplementation(() => isExpired);
    },
    
    setHasValidTokens: (isValid: boolean) => {
      tokenManagerMock.hasValidTokens.mockImplementation(() => isValid);
    },
    
    reset: () => {
      mockAccessToken = null;
      mockRefreshToken = null;
      emitter.clear();
      vi.resetAllMocks();
    }
  };
  
  return tokenManagerMock;
};

export const tokenManagerMock = createTokenManagerMock();

export default tokenManagerMock;