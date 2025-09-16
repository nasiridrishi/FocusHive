import React from 'react'
import {renderHook, waitFor} from '@testing-library/react'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {describe, expect, it, vi, beforeEach} from 'vitest'
import {useHiveDetails} from '../useHiveQueries'
import * as authModule from '../useAuthQueries'
import type {User} from '@shared/types/auth'
import {server} from '@test-utils/msw-server'
import {http, HttpResponse} from 'msw'
import {hiveApiService} from '@/services/api/hiveApi'

vi.mock('@/services/api/hiveApi')

describe('useHiveQueries Authorization Tests', () => {
  let queryClient: QueryClient

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
          staleTime: 0,
          gcTime: 0
        },
        mutations: {retry: false}
      }
    })
    vi.clearAllMocks()
  })

  const wrapper = ({children}: {children: React.ReactNode}) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  )

  describe('isOwner check', () => {
    it('should return true when current user is the hive owner', async () => {
      const mockUser: User = {
        id: 'user-123',
        email: 'test@example.com',
        username: 'testuser',
        firstName: 'Test',
        lastName: 'User',
        name: 'Test User',
        isEmailVerified: true,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      }

      // Mock the useAuth hook to return our test user
      vi.spyOn(authModule, 'useAuth').mockReturnValue({
        user: mockUser,
        isAuthenticated: true,
        isLoading: false,
        error: null,
        userError: null,
        authStatusError: null as any,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        isLoggingIn: false,
        isLoggingOut: false,
        isRegistering: false,
        loginError: null,
        logoutError: null,
        registerError: null,
        refetchUser: vi.fn(),
        refetchAuthStatus: vi.fn(),
        isUserLoading: false,
        isAuthStatusLoading: false
      })

      const mockHive = {
        id: 456,
        name: 'Test Hive',
        description: 'A test hive',
        ownerId: 'user-123', // Same as current user
        memberCount: 5,
        maxMembers: 10,
        isPrivate: false,
        createdAt: '2024-01-01T00:00:00Z'
      }

      // Setup MSW handlers for this test
      server.use(
        http.get('http://localhost:8080/api/v1/hives/:id', () => {
          return HttpResponse.json(mockHive)
        }),
        http.get('http://localhost:8080/api/v1/hives/:id/members', () => {
          return HttpResponse.json([])
        })
      )

      const {result} = renderHook(
        () => useHiveDetails('456'),
        {wrapper}
      )

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false)
      })

      // Verify authorization check works
      expect(result.current.isOwner).toBe(true)
    })

    it('should return false when current user is not the hive owner', async () => {
      const mockUser: User = {
        id: 'user-123',
        email: 'test@example.com',
        username: 'testuser',
        firstName: 'Test',
        lastName: 'User',
        name: 'Test User',
        isEmailVerified: true,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      }

      vi.spyOn(authModule, 'useAuth').mockReturnValue({
        user: mockUser,
        isAuthenticated: true,
        isLoading: false,
        error: null,
        userError: null,
        authStatusError: null as any,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        isLoggingIn: false,
        isLoggingOut: false,
        isRegistering: false,
        loginError: null,
        logoutError: null,
        registerError: null,
        refetchUser: vi.fn(),
        refetchAuthStatus: vi.fn(),
        isUserLoading: false,
        isAuthStatusLoading: false
      })

      const mockHive = {
        id: 'hive-456',
        name: 'Test Hive',
        slug: 'test-hive',
        description: 'A test hive',
        ownerId: 'different-user-789', // Different from current user
        memberCount: 5,
        maxMembers: 10,
        tags: [],
        settings: {},
        isPrivate: false,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      } as any

      vi.mocked(hiveApiService.getHiveById).mockResolvedValue(mockHive)
      vi.mocked(hiveApiService.getHiveMembers).mockResolvedValue([])

      const {result} = renderHook(
        () => useHiveDetails('hive-456'),
        {wrapper}
      )

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false)
      })

      expect(result.current.isOwner).toBe(false)
    })

    it('should return false when user is not authenticated', async () => {
      // Mock unauthenticated state
      vi.spyOn(authModule, 'useAuth').mockReturnValue({
        user: null,
        isAuthenticated: false,
        isLoading: false,
        error: null,
        userError: null,
        authStatusError: null as any,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        isLoggingIn: false,
        isLoggingOut: false,
        isRegistering: false,
        loginError: null,
        logoutError: null,
        registerError: null,
        refetchUser: vi.fn(),
        refetchAuthStatus: vi.fn(),
        isUserLoading: false,
        isAuthStatusLoading: false
      })

      const mockHive = {
        id: 'hive-456',
        name: 'Test Hive',
        slug: 'test-hive',
        description: 'A test hive',
        ownerId: 'user-123',
        memberCount: 5,
        maxMembers: 10,
        tags: [],
        settings: {},
        isPrivate: false,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      } as any

      vi.mocked(hiveApiService.getHiveById).mockResolvedValue(mockHive)
      vi.mocked(hiveApiService.getHiveMembers).mockResolvedValue([])

      const {result} = renderHook(
        () => useHiveDetails('hive-456'),
        {wrapper}
      )

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false)
      })

      expect(result.current.isOwner).toBe(false)
    })
  })

  describe('isMember check', () => {
    it('should return true when current user is in members list (FAILING TEST)', async () => {
      // This test SHOULD FAIL initially to demonstrate the missing functionality
      // After implementation, isMember should be true when current user is in the members list

      const mockUser: User = {
        id: 'user-123',
        email: 'test@example.com',
        username: 'testuser',
        firstName: 'Test',
        lastName: 'User',
        name: 'Test User',
        isEmailVerified: true,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      }

      vi.spyOn(authModule, 'useAuth').mockReturnValue({
        user: mockUser,
        isAuthenticated: true,
        isLoading: false,
        error: null,
        userError: null,
        authStatusError: null as any,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        isLoggingIn: false,
        isLoggingOut: false,
        isRegistering: false,
        loginError: null,
        logoutError: null,
        registerError: null,
        refetchUser: vi.fn(),
        refetchAuthStatus: vi.fn(),
        isUserLoading: false,
        isAuthStatusLoading: false
      })

      const mockHive = {
        id: 'hive-456',
        name: 'Test Hive',
        slug: 'test-hive',
        description: 'A test hive',
        ownerId: 'different-user',
        memberCount: 3,
        maxMembers: 10,
        tags: [],
        settings: {},
        isPrivate: false,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      } as any

      const mockMembers = [
        {
          id: 1,
          userId: 123,
          hiveId: 456,
          role: 'MEMBER' as const,
          joinedAt: '2024-01-01T00:00:00Z',
          user: {
            id: 123,
            username: 'testuser',
            displayName: 'Test User',
            avatar: null
          }
        },
        {
          id: 2,
          userId: 456,
          hiveId: 456,
          role: 'MEMBER' as const,
          joinedAt: '2024-01-01T00:00:00Z',
          user: {
            id: 456,
            username: 'otheruser',
            displayName: 'Other User',
            avatar: null
          }
        }
      ]

      vi.mocked(hiveApiService.getHiveById).mockResolvedValue(mockHive)
      vi.mocked(hiveApiService.getHiveMembers).mockResolvedValue(mockMembers)

      const {result} = renderHook(
        () => useHiveDetails('hive-456'),
        {wrapper}
      )

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false)
      })

      // This assertion should FAIL initially because isMember is hardcoded to false
      // After fix, it should pass
      expect(result.current.isMember).toBe(true)
    })

    it('should return false when current user is not in members list', async () => {
      const mockUser: User = {
        id: 'user-123',
        email: 'test@example.com',
        username: 'testuser',
        firstName: 'Test',
        lastName: 'User',
        name: 'Test User',
        isEmailVerified: true,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      }

      vi.spyOn(authModule, 'useAuth').mockReturnValue({
        user: mockUser,
        isAuthenticated: true,
        isLoading: false,
        error: null,
        userError: null,
        authStatusError: null as any,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        isLoggingIn: false,
        isLoggingOut: false,
        isRegistering: false,
        loginError: null,
        logoutError: null,
        registerError: null,
        refetchUser: vi.fn(),
        refetchAuthStatus: vi.fn(),
        isUserLoading: false,
        isAuthStatusLoading: false
      })

      const mockHive = {
        id: 'hive-456',
        name: 'Test Hive',
        slug: 'test-hive',
        description: 'A test hive',
        ownerId: 'different-user',
        memberCount: 2,
        maxMembers: 10,
        tags: [],
        settings: {},
        isPrivate: false,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      } as any

      const mockMembers = [
        {
          id: 1,
          userId: 456,
          hiveId: 456,
          role: 'MEMBER' as const,
          joinedAt: '2024-01-01T00:00:00Z',
          user: {
            id: 456,
            username: 'otheruser',
            displayName: 'Other User',
            avatar: null
          }
        },
        {
          id: 2,
          userId: 789,
          hiveId: 456,
          role: 'MEMBER' as const,
          joinedAt: '2024-01-01T00:00:00Z',
          user: {
            id: 789,
            username: 'anotheruser',
            displayName: 'Another User',
            avatar: null
          }
        }
      ]

      vi.mocked(hiveApiService.getHiveById).mockResolvedValue(mockHive)
      vi.mocked(hiveApiService.getHiveMembers).mockResolvedValue(mockMembers)

      const {result} = renderHook(
        () => useHiveDetails('hive-456'),
        {wrapper}
      )

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false)
      })

      expect(result.current.isMember).toBe(false)
    })

    it('should return false when members list is empty', async () => {
      const mockUser: User = {
        id: 'user-123',
        email: 'test@example.com',
        username: 'testuser',
        firstName: 'Test',
        lastName: 'User',
        name: 'Test User',
        isEmailVerified: true,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      }

      vi.spyOn(authModule, 'useAuth').mockReturnValue({
        user: mockUser,
        isAuthenticated: true,
        isLoading: false,
        error: null,
        userError: null,
        authStatusError: null as any,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        isLoggingIn: false,
        isLoggingOut: false,
        isRegistering: false,
        loginError: null,
        logoutError: null,
        registerError: null,
        refetchUser: vi.fn(),
        refetchAuthStatus: vi.fn(),
        isUserLoading: false,
        isAuthStatusLoading: false
      })

      const mockHive = {
        id: 'hive-456',
        name: 'Test Hive',
        slug: 'test-hive',
        description: 'A test hive',
        ownerId: 'different-user',
        memberCount: 0,
        maxMembers: 10,
        tags: [],
        settings: {},
        isPrivate: false,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      } as any

      vi.mocked(hiveApiService.getHiveById).mockResolvedValue(mockHive)
      vi.mocked(hiveApiService.getHiveMembers).mockResolvedValue([])

      const {result} = renderHook(
        () => useHiveDetails('hive-456'),
        {wrapper}
      )

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false)
      })

      expect(result.current.isMember).toBe(false)
    })

    it('should return true when owner is also considered a member', async () => {
      // Owner should also be considered a member
      const mockUser: User = {
        id: 'user-123',
        email: 'test@example.com',
        username: 'testuser',
        firstName: 'Test',
        lastName: 'User',
        name: 'Test User',
        isEmailVerified: true,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      }

      vi.spyOn(authModule, 'useAuth').mockReturnValue({
        user: mockUser,
        isAuthenticated: true,
        isLoading: false,
        error: null,
        userError: null,
        authStatusError: null as any,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        isLoggingIn: false,
        isLoggingOut: false,
        isRegistering: false,
        loginError: null,
        logoutError: null,
        registerError: null,
        refetchUser: vi.fn(),
        refetchAuthStatus: vi.fn(),
        isUserLoading: false,
        isAuthStatusLoading: false
      })

      const mockHive = {
        id: 'hive-456',
        name: 'Test Hive',
        slug: 'test-hive',
        description: 'A test hive',
        ownerId: 'user-123', // Current user is the owner
        memberCount: 1,
        maxMembers: 10,
        tags: [],
        settings: {},
        isPrivate: false,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      } as any

      vi.mocked(hiveApiService.getHiveById).mockResolvedValue(mockHive)
      vi.mocked(hiveApiService.getHiveMembers).mockResolvedValue([])

      const {result} = renderHook(
        () => useHiveDetails('hive-456'),
        {wrapper}
      )

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false)
      })

      // Owner should also be considered a member
      expect(result.current.isOwner).toBe(true)
      expect(result.current.isMember).toBe(true) // This will fail initially
    })
  })
})