import React from 'react';
import { renderHook } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { 
  useMyPresence,
  useUserPresence,
  useHivePresence,
  useBatchHivePresence,
  useMyFocusSessions,
  useHiveFocusSessions,
  usePresenceStats,
  useUpdatePresence,
  useStartFocusSession,
  useEndFocusSession,
  useUpdateFocusSession
} from '../usePresenceQueries';
import { vi } from 'vitest';
import { ReactNode } from 'react';

// Mock the API service
vi.mock('@services/api/presenceApi', () => {
  const mockApiService = {
    getMyPresence: vi.fn().mockResolvedValue({}),
    getUserPresence: vi.fn().mockResolvedValue({}),
    getHivePresence: vi.fn().mockResolvedValue({ presences: [] }),
    getHivePresenceBatch: vi.fn().mockResolvedValue([]),
    getMySessions: vi.fn().mockResolvedValue([]),
    getHiveSessions: vi.fn().mockResolvedValue([]),
    getPresenceStats: vi.fn().mockResolvedValue({}),
    updateMyPresence: vi.fn().mockResolvedValue({}),
    startFocusSession: vi.fn().mockResolvedValue({}),
    endFocusSession: vi.fn().mockResolvedValue({}),
  };
  
  return {
    default: mockApiService,
    presenceApiService: mockApiService,
    FocusSession: class {},
    PresenceUpdate: class {},
  };
});

// Mock the auth API service
vi.mock('@services/api', () => ({
  authApiService: {
    getCurrentUser: vi.fn().mockResolvedValue({ id: 'test-user', email: 'test@example.com' }),
    validateAuth: vi.fn().mockResolvedValue(true),
  },
}));

// Mock the auth hook
vi.mock('./useAuthQueries', () => ({
  useAuth: vi.fn(() => ({ user: { id: 'test-user' } })),
}));

// Mock query client utilities
vi.mock('@lib/queryClient', () => ({
  CACHE_TIMES: { SHORT: 5000, MEDIUM: 10000, LONG: 20000 },
  STALE_TIMES: { SESSION_DATA: 1000, STATIC_CONTENT: 5000, REALTIME: 0 },
  queryKeys: {
    auth: {
      all: ['auth'],
      user: () => ['auth', 'user'],
      profile: () => ['auth', 'profile'],
      permissions: () => ['auth', 'permissions'],
    },
    presence: {
      all: ['presence'],
      user: (id: string) => ['presence', 'user', id],
      hive: (id: string) => ['presence', 'hive', id],
      sessions: (hiveId?: string) => ['presence', 'sessions', hiveId].filter(Boolean),
    },
    notifications: {
      all: ['notifications'],
      list: (filters?: unknown) => ['notifications', 'list', filters],
      unread: () => ['notifications', 'unread'],
      count: () => ['notifications', 'count'],
    },
  },
}));

// Mock transformers
vi.mock('./transformers', () => ({
  transformPresenceDTO: vi.fn((dto, userId) => ({ ...dto, transformedBy: userId })),
}));

// Mock types
vi.mock('@shared/types/presence', () => ({
  FocusSession: {},
  PresenceUpdate: {},
}));

vi.mock('./types', () => ({
  UserPresence: {},
}));

// Test wrapper with QueryClient
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  const Wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
  return Wrapper;
};

describe('usePresenceQueries return types', () => {
  describe('Query hooks should return UseQueryResult', () => {
    it('should return UseQueryResult from useMyPresence', () => {
      const { result } = renderHook(() => useMyPresence(), { wrapper: createWrapper() });
      
      // Should have React Query properties, not Promise properties
      expect(result.current).toHaveProperty('data');
      expect(result.current).toHaveProperty('isLoading');
      expect(result.current).toHaveProperty('error');
      expect(result.current).toHaveProperty('refetch');
      expect(result.current).toHaveProperty('isSuccess');
      expect(result.current).toHaveProperty('isError');
      
      // Should NOT have Promise properties
      expect(result.current).not.toHaveProperty('then');
      expect(result.current).not.toHaveProperty('catch');
      expect(result.current).not.toHaveProperty('finally');
    });

    it('should return UseQueryResult from useUserPresence', () => {
      const { result } = renderHook(() => useUserPresence('123'), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('data');
      expect(result.current).toHaveProperty('isLoading');
      expect(result.current).toHaveProperty('error');
      expect(result.current).toHaveProperty('refetch');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
      expect(result.current).not.toHaveProperty('catch');
    });

    it('should return UseQueryResult from useHivePresence', () => {
      const { result } = renderHook(() => useHivePresence('123'), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('data');
      expect(result.current).toHaveProperty('isLoading');
      expect(result.current).toHaveProperty('error');
      expect(result.current).toHaveProperty('refetch');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
    });

    it('should return UseQueryResult from useBatchHivePresence', () => {
      const { result } = renderHook(() => useBatchHivePresence(['123', '456']), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('data');
      expect(result.current).toHaveProperty('isLoading');
      expect(result.current).toHaveProperty('error');
      expect(result.current).toHaveProperty('refetch');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
    });

    it('should return UseQueryResult from useMyFocusSessions', () => {
      const { result } = renderHook(() => useMyFocusSessions(), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('data');
      expect(result.current).toHaveProperty('isLoading');
      expect(result.current).toHaveProperty('error');
      expect(result.current).toHaveProperty('refetch');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
    });

    it('should return UseQueryResult from useHiveFocusSessions', () => {
      const { result } = renderHook(() => useHiveFocusSessions('123'), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('data');
      expect(result.current).toHaveProperty('isLoading');
      expect(result.current).toHaveProperty('error');
      expect(result.current).toHaveProperty('refetch');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
    });

    it('should return UseQueryResult from usePresenceStats', () => {
      const { result } = renderHook(() => usePresenceStats(), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('data');
      expect(result.current).toHaveProperty('isLoading');
      expect(result.current).toHaveProperty('error');
      expect(result.current).toHaveProperty('refetch');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
    });
  });

  describe('Mutation hooks should return UseMutationResult', () => {
    it('should return UseMutationResult from useUpdatePresence', () => {
      const { result } = renderHook(() => useUpdatePresence(), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('mutate');
      expect(result.current).toHaveProperty('mutateAsync');
      expect(result.current).toHaveProperty('isPending');
      expect(result.current).toHaveProperty('isSuccess');
      expect(result.current).toHaveProperty('isError');
      expect(result.current).toHaveProperty('error');
      expect(result.current).toHaveProperty('data');
      expect(result.current).toHaveProperty('reset');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
      expect(result.current).not.toHaveProperty('catch');
    });

    it('should return UseMutationResult from useStartFocusSession', () => {
      const { result } = renderHook(() => useStartFocusSession(), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('mutate');
      expect(result.current).toHaveProperty('isPending');
      expect(result.current).toHaveProperty('isSuccess');
      expect(result.current).toHaveProperty('error');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
    });

    it('should return UseMutationResult from useEndFocusSession', () => {
      const { result } = renderHook(() => useEndFocusSession(), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('mutate');
      expect(result.current).toHaveProperty('isPending');
      expect(result.current).toHaveProperty('isSuccess');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
    });

    it('should return UseMutationResult from useUpdateFocusSession', () => {
      const { result } = renderHook(() => useUpdateFocusSession(), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('mutate');
      expect(result.current).toHaveProperty('isPending');
      expect(result.current).toHaveProperty('isSuccess');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
    });
  });
});