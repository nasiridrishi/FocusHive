import React from 'react'
import {renderHook} from '@testing-library/react'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {describe, expect, it, vi} from 'vitest'
import {useHiveDetails} from '../useHiveQueries'
import * as authModule from '../useAuthQueries'
import type {User} from '@shared/types/auth'

// Mock the useAuth hook
vi.mock('../useAuthQueries')

// Mock the API hooks to directly return data
vi.mock('../useHiveQueries', async () => {
  const actual = await vi.importActual('../useHiveQueries')
  return {
    ...actual,
    useHive: vi.fn(),
    useHiveMembers: vi.fn()
  }
})

import {useHive, useHiveMembers} from '../useHiveQueries'

describe('useHiveDetails Authorization Logic', () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {retry: false}
    }
  })

  const wrapper = ({children}: {children: React.ReactNode}) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  )

  describe('isOwner logic', () => {
    it('should return true when user ID matches hive owner ID', () => {
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

      vi.mocked(authModule.useAuth).mockReturnValue({
        user: mockUser,
        isAuthenticated: true,
        isLoading: false,
        isUserLoading: false,
        isAuthStatusLoading: false,
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
        refetchAuthStatus: vi.fn()
      })

      vi.mocked(useHive).mockReturnValue({
        data: {
          id: '456',
          name: 'Test Hive',
          ownerId: 'user-123' // Same as user ID
        },
        isLoading: false,
        error: null
      } as any)

      vi.mocked(useHiveMembers).mockReturnValue({
        data: [],
        isLoading: false,
        error: null
      } as any)

      const {result} = renderHook(
        () => useHiveDetails('456'),
        {wrapper}
      )

      expect(result.current.isOwner).toBe(true)
      expect(result.current.isMember).toBe(true) // Owner is also a member
    })

    it('should return false when user ID does not match hive owner ID', () => {
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

      vi.mocked(authModule.useAuth).mockReturnValue({
        user: mockUser,
        isAuthenticated: true,
        isLoading: false,
        isUserLoading: false,
        isAuthStatusLoading: false,
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
        refetchAuthStatus: vi.fn()
      })

      vi.mocked(useHive).mockReturnValue({
        data: {
          id: '456',
          name: 'Test Hive',
          ownerId: 'different-user-456' // Different from user ID
        },
        isLoading: false,
        error: null
      } as any)

      vi.mocked(useHiveMembers).mockReturnValue({
        data: [],
        isLoading: false,
        error: null
      } as any)

      const {result} = renderHook(
        () => useHiveDetails('456'),
        {wrapper}
      )

      expect(result.current.isOwner).toBe(false)
      expect(result.current.isMember).toBe(false)
    })

    it('should return false when user is not authenticated', () => {
      vi.mocked(authModule.useAuth).mockReturnValue({
        user: null as any,
        isAuthenticated: false,
        isLoading: false,
        isUserLoading: false,
        isAuthStatusLoading: false,
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
        refetchAuthStatus: vi.fn()
      })

      vi.mocked(useHive).mockReturnValue({
        data: {
          id: '456',
          name: 'Test Hive',
          ownerId: 'user-123'
        },
        isLoading: false,
        error: null
      } as any)

      vi.mocked(useHiveMembers).mockReturnValue({
        data: [],
        isLoading: false,
        error: null
      } as any)

      const {result} = renderHook(
        () => useHiveDetails('456'),
        {wrapper}
      )

      expect(result.current.isOwner).toBe(false)
      expect(result.current.isMember).toBe(false)
    })
  })

  describe('isMember logic', () => {
    it('should return true when user is in members list', () => {
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

      vi.mocked(authModule.useAuth).mockReturnValue({
        user: mockUser,
        isAuthenticated: true,
        isLoading: false,
        isUserLoading: false,
        isAuthStatusLoading: false,
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
        refetchAuthStatus: vi.fn()
      })

      vi.mocked(useHive).mockReturnValue({
        data: {
          id: '456',
          name: 'Test Hive',
          ownerId: 'different-user'
        },
        isLoading: false,
        error: null
      } as any)

      vi.mocked(useHiveMembers).mockReturnValue({
        data: [
          {id: 'user-123', username: 'testuser'},
          {id: 'user-456', username: 'other'}
        ],
        isLoading: false,
        error: null
      } as any)

      const {result} = renderHook(
        () => useHiveDetails('456'),
        {wrapper}
      )

      expect(result.current.isOwner).toBe(false)
      expect(result.current.isMember).toBe(true)
    })

    it('should return false when user is not in members list', () => {
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

      vi.mocked(authModule.useAuth).mockReturnValue({
        user: mockUser,
        isAuthenticated: true,
        isLoading: false,
        isUserLoading: false,
        isAuthStatusLoading: false,
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
        refetchAuthStatus: vi.fn()
      })

      vi.mocked(useHive).mockReturnValue({
        data: {
          id: '456',
          name: 'Test Hive',
          ownerId: 'different-user'
        },
        isLoading: false,
        error: null
      } as any)

      vi.mocked(useHiveMembers).mockReturnValue({
        data: [
          {id: 'user-456', username: 'other'},
          {id: 'user-789', username: 'another'}
        ],
        isLoading: false,
        error: null
      } as any)

      const {result} = renderHook(
        () => useHiveDetails('456'),
        {wrapper}
      )

      expect(result.current.isOwner).toBe(false)
      expect(result.current.isMember).toBe(false)
    })

    it('should consider owner as member even if not in members list', () => {
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

      vi.mocked(authModule.useAuth).mockReturnValue({
        user: mockUser,
        isAuthenticated: true,
        isLoading: false,
        isUserLoading: false,
        isAuthStatusLoading: false,
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
        refetchAuthStatus: vi.fn()
      })

      vi.mocked(useHive).mockReturnValue({
        data: {
          id: '456',
          name: 'Test Hive',
          ownerId: 'user-123' // User is owner
        },
        isLoading: false,
        error: null
      } as any)

      vi.mocked(useHiveMembers).mockReturnValue({
        data: [], // Empty members list
        isLoading: false,
        error: null
      } as any)

      const {result} = renderHook(
        () => useHiveDetails('456'),
        {wrapper}
      )

      expect(result.current.isOwner).toBe(true)
      expect(result.current.isMember).toBe(true) // Owner should be considered a member
    })
  })
})