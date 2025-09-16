import React from 'react';
import { renderHook } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { 
  useHives, 
  useHive, 
  useInfiniteHives, 
  useHiveMembers, 
  useSearchHives, 
  useRecommendedHives,
  useCreateHive,
  useUpdateHive,
  useDeleteHive,
  useJoinHive,
  useLeaveHive
} from '../useHiveQueries';
import { vi } from 'vitest';
import { ReactNode } from 'react';

// Mock the API service
vi.mock('@services/api', () => ({
  hiveApiService: {
    getHives: vi.fn().mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, first: true, last: true }),
    getHiveById: vi.fn().mockResolvedValue({}),
    getHiveMembers: vi.fn().mockResolvedValue([]),
    searchHives: vi.fn().mockResolvedValue([]),
    createHive: vi.fn().mockResolvedValue({}),
    updateHive: vi.fn().mockResolvedValue({}),
    deleteHive: vi.fn().mockResolvedValue(undefined),
    joinHive: vi.fn().mockResolvedValue({}),
    leaveHive: vi.fn().mockResolvedValue(undefined),
  },
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
    hives: {
      all: ['hives'],
      list: (filters?: unknown) => ['hives', 'list', filters],
      detail: (id: string) => ['hives', 'detail', id],
      members: (id: string) => ['hives', 'members', id],
      search: (query: string) => ['hives', 'search', query],
    },
    notifications: {
      all: ['notifications'],
      list: (filters?: unknown) => ['notifications', 'list', filters],
      unread: () => ['notifications', 'unread'],
      count: () => ['notifications', 'count'],
    },
  },
  invalidateQueries: {
    hive: vi.fn(),
  },
}));

// Mock transformers
vi.mock('./transformers', () => ({
  transformHiveDTO: vi.fn((dto, userId) => ({ ...dto, transformedBy: userId })),
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

describe('useHiveQueries return types', () => {
  describe('Query hooks should return UseQueryResult', () => {
    it('should return UseQueryResult from useHives', () => {
      const { result } = renderHook(() => useHives(), { wrapper: createWrapper() });
      
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

    it('should return UseQueryResult from useHive', () => {
      const { result } = renderHook(() => useHive('123'), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('data');
      expect(result.current).toHaveProperty('isLoading');
      expect(result.current).toHaveProperty('error');
      expect(result.current).toHaveProperty('refetch');
      expect(result.current).toHaveProperty('isSuccess');
      expect(result.current).toHaveProperty('isError');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
      expect(result.current).not.toHaveProperty('catch');
      expect(result.current).not.toHaveProperty('finally');
    });

    it('should return UseInfiniteQueryResult from useInfiniteHives', () => {
      const { result } = renderHook(() => useInfiniteHives(), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('data');
      expect(result.current).toHaveProperty('isLoading');
      expect(result.current).toHaveProperty('error');
      expect(result.current).toHaveProperty('refetch');
      expect(result.current).toHaveProperty('fetchNextPage');
      expect(result.current).toHaveProperty('hasNextPage');
      expect(result.current).toHaveProperty('isFetchingNextPage');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
      expect(result.current).not.toHaveProperty('catch');
    });

    it('should return UseQueryResult from useHiveMembers', () => {
      const { result } = renderHook(() => useHiveMembers('123'), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('data');
      expect(result.current).toHaveProperty('isLoading');
      expect(result.current).toHaveProperty('error');
      expect(result.current).toHaveProperty('refetch');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
    });

    it('should return UseQueryResult from useSearchHives', () => {
      const { result } = renderHook(() => useSearchHives('test'), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('data');
      expect(result.current).toHaveProperty('isLoading');
      expect(result.current).toHaveProperty('error');
      expect(result.current).toHaveProperty('refetch');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
    });

    it('should return UseQueryResult from useRecommendedHives', () => {
      const { result } = renderHook(() => useRecommendedHives(), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('data');
      expect(result.current).toHaveProperty('isLoading');
      expect(result.current).toHaveProperty('error');
      expect(result.current).toHaveProperty('refetch');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
    });
  });

  describe('Mutation hooks should return UseMutationResult', () => {
    it('should return UseMutationResult from useCreateHive', () => {
      const { result } = renderHook(() => useCreateHive(), { wrapper: createWrapper() });
      
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

    it('should return UseMutationResult from useUpdateHive', () => {
      const { result } = renderHook(() => useUpdateHive(), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('mutate');
      expect(result.current).toHaveProperty('isPending');
      expect(result.current).toHaveProperty('isSuccess');
      expect(result.current).toHaveProperty('error');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
    });

    it('should return UseMutationResult from useDeleteHive', () => {
      const { result } = renderHook(() => useDeleteHive(), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('mutate');
      expect(result.current).toHaveProperty('isPending');
      expect(result.current).toHaveProperty('isSuccess');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
    });

    it('should return UseMutationResult from useJoinHive', () => {
      const { result } = renderHook(() => useJoinHive(), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('mutate');
      expect(result.current).toHaveProperty('isPending');
      expect(result.current).toHaveProperty('isSuccess');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
    });

    it('should return UseMutationResult from useLeaveHive', () => {
      const { result } = renderHook(() => useLeaveHive(), { wrapper: createWrapper() });
      
      expect(result.current).toHaveProperty('mutate');
      expect(result.current).toHaveProperty('isPending');
      expect(result.current).toHaveProperty('isSuccess');
      
      // Should NOT be a Promise
      expect(result.current).not.toHaveProperty('then');
    });
  });
});