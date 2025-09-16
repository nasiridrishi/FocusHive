/**
 * Authentication Query Hooks
 *
 * Provides React Query hooks for authentication operations with:
 * - Optimized caching strategies
 * - Optimistic updates
 * - Error handling
 * - Type safety
 */

import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {authApiService} from '@services/api';
import {CACHE_TIMES, queryKeys, STALE_TIMES} from '@lib/queryClient';
import type {
  ChangePasswordRequest,
  LoginRequest,
  LoginResponse,
  PasswordResetRequest,
  RegisterRequest,
  RegisterResponse,
  User,
} from '@shared/types/auth';

// ============================================================================
// QUERY HOOKS
// ============================================================================

/**
 * Get current authenticated user
 */
export const useCurrentUser = () => {
  return useQuery({
    queryKey: queryKeys.auth.user(),
    queryFn: () => authApiService.getCurrentUser(),
    staleTime: STALE_TIMES.USER_DATA,
    gcTime: CACHE_TIMES.SHORT,
    retry: (failureCount, _error: unknown) => {
      // Don't retry on 401/403 _errors (user not authenticated)
      const hasResponse = _error && typeof _error === 'object' && 'response' in _error;
      const status = hasResponse ? (_error as {
        response: { status?: number }
      }).response?.status : undefined;
      if (status === 401 || status === 403) {
        return false;
      }
      return failureCount < 2;
    },
    meta: {
      description: 'Current user profile data',
      requiresAuth: true,
    },
  });
};

/**
 * Check authentication status
 */
export const useAuthStatus = () => {
  return useQuery({
    queryKey: [...queryKeys.auth.all, 'status'],
    queryFn: () => authApiService.validateAuth(),
    staleTime: STALE_TIMES.SESSION_DATA,
    gcTime: CACHE_TIMES.SHORT,
    retry: false, // Don't retry auth validation
    refetchOnWindowFocus: true, // Check auth when window gains focus
    refetchOnMount: true,
    meta: {
      description: 'Authentication status check',
    },
  });
};

/**
 * Get user permissions (placeholder for future implementation)
 */
export const useUserPermissions = (enabled = true) => {
  return useQuery({
    queryKey: queryKeys.auth.permissions(),
    queryFn: async () => {
      // Placeholder - implement when backend provides permissions endpoint
      return [];
    },
    enabled,
    staleTime: STALE_TIMES.STATIC_CONTENT,
    gcTime: CACHE_TIMES.MEDIUM,
    meta: {
      description: 'User permissions and roles',
      requiresAuth: true,
    },
  });
};

// ============================================================================
// MUTATION HOOKS
// ============================================================================

/**
 * Login mutation with optimistic updates
 */
export const useLogin = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (credentials: LoginRequest) => authApiService.login(credentials),
    mutationKey: ['auth', 'login'],

    onMutate: async (credentials) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({queryKey: queryKeys.auth.all});

      // Optimistically set loading state
      queryClient.setQueryData([...queryKeys.auth.all, 'loginStatus'], 'loading');

      return {credentials};
    },

    onSuccess: (data: LoginResponse, _variables, _context) => {
      // Update user data in cache
      queryClient.setQueryData(queryKeys.auth.user(), data.user);

      // Mark authentication as valid
      queryClient.setQueryData([...queryKeys.auth.all, 'status'], true);

      // Prefetch initial user data
      queryClient.prefetchQuery({
        queryKey: [...queryKeys.notifications.all, 'count'],
        staleTime: STALE_TIMES.SESSION_DATA,
      });

      // Track successful login
      if (typeof window !== 'undefined' && 'gtag' in window) {
        // @ts-expect-error - gtag is loaded by Google Analytics script
        window.gtag('event', 'login', {
          method: 'email',
        });
      }
    },

    onError: (_error, _variables, _context) => {
      // Error handled by _error boundary and toast notifications

      // Clear any optimistic updates
      queryClient.removeQueries({queryKey: [...queryKeys.auth.all, 'loginStatus']});
      queryClient.setQueryData([...queryKeys.auth.all, 'status'], false);
    },

    onSettled: () => {
      // Always clean up loading states
      queryClient.removeQueries({queryKey: [...queryKeys.auth.all, 'loginStatus']});
    },

    meta: {
      description: 'User login operation',
    },
  });
};

/**
 * Registration mutation
 */
export const useRegister = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (userData: RegisterRequest) => authApiService.register(userData),
    mutationKey: ['auth', 'register'],

    onSuccess: (data: RegisterResponse) => {
      // Update user data in cache
      queryClient.setQueryData(queryKeys.auth.user(), data.user);
      queryClient.setQueryData([...queryKeys.auth.all, 'status'], true);

      // Track successful registration
      if (typeof window !== 'undefined' && 'gtag' in window) {
        // @ts-expect-error - gtag is loaded by Google Analytics script
        window.gtag('event', 'sign_up', {
          method: 'email',
        });
      }
    },

    onError: (_error) => {
      // Error handled by _error boundary and toast notifications
      queryClient.setQueryData([...queryKeys.auth.all, 'status'], false);
    },

    meta: {
      description: 'User registration operation',
    },
  });
};

/**
 * Logout mutation
 */
export const useLogout = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => authApiService.logout(),
    mutationKey: ['auth', 'logout'],

    onMutate: async () => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({queryKey: queryKeys.auth.all});
    },

    onSuccess: () => {
      // Clear all cached data on logout
      queryClient.clear();

      // Explicitly set auth status to false
      queryClient.setQueryData([...queryKeys.auth.all, 'status'], false);
    },

    onError: (_error) => {
      // Error handled by _error boundary and toast notifications
      // Even if logout fails, clear local cache
      queryClient.clear();
      queryClient.setQueryData([...queryKeys.auth.all, 'status'], false);
    },

    meta: {
      description: 'User logout operation',
    },
  });
};

/**
 * Update profile mutation with optimistic updates
 */
export const useUpdateProfile = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (userData: Partial<User>) => authApiService.updateProfile(userData),
    mutationKey: ['auth', 'updateProfile'],

    onMutate: async (newUserData) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({queryKey: queryKeys.auth.user()});

      // Snapshot previous value
      const previousUser = queryClient.getQueryData<User>(queryKeys.auth.user());

      // Optimistically update user data
      if (previousUser) {
        queryClient.setQueryData(queryKeys.auth.user(), {
          ...previousUser,
          ...newUserData,
        });
      }

      return {previousUser};
    },

    onSuccess: (updatedUser) => {
      // Update with server response
      queryClient.setQueryData(queryKeys.auth.user(), updatedUser);
    },

    onError: (_error, newUserData, context) => {
      // Error handled by _error boundary and toast notifications

      // Rollback optimistic update
      if (context?.previousUser) {
        queryClient.setQueryData(queryKeys.auth.user(), context.previousUser);
      }
    },

    onSettled: () => {
      // Always refetch to ensure consistency
      queryClient.invalidateQueries({queryKey: queryKeys.auth.user()});
    },

    meta: {
      description: 'Update user profile',
      requiresAuth: true,
    },
  });
};

/**
 * Change password mutation
 */
export const useChangePassword = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (passwordData: ChangePasswordRequest) => authApiService.changePassword(passwordData),
    mutationKey: ['auth', 'changePassword'],

    onSuccess: () => {
      // Optionally invalidate auth status to re-verify
      queryClient.invalidateQueries({queryKey: [...queryKeys.auth.all, 'status']});
    },

    onError: (_error) => {
      // Error handled by _error boundary and toast notifications
    },

    meta: {
      description: 'Change user password',
      requiresAuth: true,
    },
  });
};

/**
 * Request password reset mutation
 */
export const useRequestPasswordReset = () => {
  return useMutation({
    mutationFn: (resetData: PasswordResetRequest) => authApiService.requestPasswordReset(resetData),
    mutationKey: ['auth', 'requestPasswordReset'],

    onError: (_error) => {
      // Error handled by _error boundary and toast notifications
    },

    meta: {
      description: 'Request password reset email',
    },
  });
};

// ============================================================================
// COMPOUND HOOKS
// ============================================================================

/**
 * Combined authentication hook with all necessary data
 */
export const useAuth = () => {
  const userQuery = useCurrentUser();
  const authStatusQuery = useAuthStatus();

  const loginMutation = useLogin();
  const logoutMutation = useLogout();
  const registerMutation = useRegister();

  return {
    // Data
    user: userQuery.data,
    isAuthenticated: authStatusQuery.data === true,

    // Loading states
    isLoading: userQuery.isLoading || authStatusQuery.isLoading,
    isUserLoading: userQuery.isLoading,
    isAuthStatusLoading: authStatusQuery.isLoading,

    // Error states
    error: userQuery.error || authStatusQuery.error,
    userError: userQuery.error,
    authStatusError: authStatusQuery.error,

    // Mutations
    login: loginMutation.mutate,
    logout: logoutMutation.mutate,
    register: registerMutation.mutate,

    // Mutation states
    isLoggingIn: loginMutation.isPending,
    isLoggingOut: logoutMutation.isPending,
    isRegistering: registerMutation.isPending,

    loginError: loginMutation.error,
    logoutError: logoutMutation.error,
    registerError: registerMutation.error,

    // Utilities
    refetchUser: userQuery.refetch,
    refetchAuthStatus: authStatusQuery.refetch,
  };
};

/**
 * Hook for checking if user has specific permissions
 */
export const usePermissions = () => {
  const permissionsQuery = useUserPermissions();

  const hasPermission = (permission: string): boolean => {
    return permissionsQuery.data?.includes(permission) ?? false;
  };

  const hasAnyPermission = (permissions: string[]): boolean => {
    return permissions.some(permission => hasPermission(permission));
  };

  const hasAllPermissions = (permissions: string[]): boolean => {
    return permissions.every(permission => hasPermission(permission));
  };

  return {
    permissions: permissionsQuery.data ?? [],
    isLoading: permissionsQuery.isLoading,
    error: permissionsQuery.error,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    refetch: permissionsQuery.refetch,
  };
};