/**
 * Hive Query Hooks
 * 
 * Provides React Query hooks for hive operations with:
 * - Optimized caching strategies
 * - Real-time updates
 * - Optimistic mutations
 * - Background refetching
 */

import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/react-query';
import { hiveApiService } from '@services/api';
import { queryKeys, STALE_TIMES, CACHE_TIMES, invalidateQueries } from '@lib/queryClient';
import { transformHiveDTO, type HiveDTO } from './transformers';
import { useAuth } from './useAuthQueries';
import type { 
  Hive 
} from './types';

// Type for paginated responses
interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
}
import type { 
  CreateHiveRequest, 
  UpdateHiveRequest,
  HiveSearchFilters 
} from '@shared/types/hive';

// ============================================================================
// QUERY HOOKS
// ============================================================================

/**
 * Get all hives for current user
 */
export const useHives = (filters?: HiveSearchFilters) => {
  const { user } = useAuth();
  const currentUserId = user?.id || '';

  return useQuery({
    queryKey: queryKeys.hives.list(filters),
    queryFn: async () => {
      const response = await hiveApiService.getHives(filters?.page || 0, filters?.size || 20);
      // Transform the paginated response
      return {
        ...response,
        content: (response as unknown as PaginatedResponse<HiveDTO>).content?.map((dto: HiveDTO) => transformHiveDTO(dto, currentUserId)) || []
      };
    },
    staleTime: STALE_TIMES.SESSION_DATA,
    gcTime: CACHE_TIMES.MEDIUM,
    retry: 2,
    meta: {
      description: 'List of user hives',
      requiresAuth: true,
    },
  });
};

/**
 * Get infinite list of hives with pagination
 */
export const useInfiniteHives = (filters?: HiveSearchFilters) => {
  const { user } = useAuth();
  const currentUserId = user?.id || '';

  return useInfiniteQuery({
    queryKey: [...queryKeys.hives.all, 'infinite', filters],
    queryFn: async ({ pageParam = 0 }) => {
      const response = await hiveApiService.getHives(pageParam, 20);
      // Transform the paginated response
      return {
        ...response,
        content: (response as unknown as PaginatedResponse<HiveDTO>).content?.map((dto: HiveDTO) => transformHiveDTO(dto, currentUserId)) || []
      };
    },
    initialPageParam: 0,
    getNextPageParam: (lastPage, _allPages) => {
      // Check if we have more pages based on PaginatedResponse
      return !lastPage.last ? lastPage.page + 1 : undefined;
    },
    staleTime: STALE_TIMES.SESSION_DATA,
    gcTime: CACHE_TIMES.MEDIUM,
    meta: {
      description: 'Infinite scroll hive list',
      requiresAuth: true,
    },
  });
};

/**
 * Get specific hive by ID
 */
export const useHive = (hiveId: string, enabled = true) => {
  const { user } = useAuth();
  const currentUserId = user?.id || '';

  return useQuery({
    queryKey: queryKeys.hives.detail(hiveId),
    queryFn: async () => {
      const dto = await hiveApiService.getHiveById(parseInt(hiveId, 10));
      return transformHiveDTO(dto as unknown as HiveDTO, currentUserId);
    },
    enabled: enabled && !!hiveId,
    staleTime: STALE_TIMES.STATIC_CONTENT,
    gcTime: CACHE_TIMES.LONG,
    retry: (failureCount, error: unknown) => {
      // Don't retry on 404 errors - need proper type checking
      const axiosError = error as { response?: { status?: number } };
      if (axiosError?.response?.status === 404) return false;
      return failureCount < 2;
    },
    meta: {
      description: 'Individual hive details',
      requiresAuth: true,
    },
  });
};

/**
 * Get hive members
 */
export const useHiveMembers = (hiveId: string, enabled = true) => {
  return useQuery({
    queryKey: queryKeys.hives.members(hiveId),
    queryFn: async () => {
      const members = await hiveApiService.getHiveMembers(parseInt(hiveId, 10));
      // For now, return members as-is since they might already be transformed by the API service
      // TODO: Create User transformer if needed
      return members;
    },
    enabled: enabled && !!hiveId,
    staleTime: STALE_TIMES.SESSION_DATA,
    gcTime: CACHE_TIMES.MEDIUM,
    refetchInterval: 30000, // Refresh every 30 seconds for active member tracking
    meta: {
      description: 'Hive member list with presence',
      requiresAuth: true,
    },
  });
};

/**
 * Search hives with debounced query
 */
export const useSearchHives = (searchQuery: string, enabled = true) => {
  const { user } = useAuth();
  const currentUserId = user?.id || '';

  return useQuery({
    queryKey: queryKeys.hives.search(searchQuery),
    queryFn: async () => {
      const results = await hiveApiService.searchHives({ query: searchQuery });
      // Transform the search results if they contain hive DTOs
      return Array.isArray(results) ? results.map((dto: unknown) => transformHiveDTO(dto as unknown as HiveDTO, currentUserId)) : [];
    },
    enabled: enabled && searchQuery.length >= 2, // Only search with 2+ characters
    staleTime: STALE_TIMES.SESSION_DATA,
    gcTime: CACHE_TIMES.SHORT, // Search results don't need long cache
    meta: {
      description: 'Hive search results',
      requiresAuth: true,
    },
  });
};

/**
 * Get recommended hives for user
 */
export const useRecommendedHives = (enabled = true) => {
  const { user } = useAuth();
  const currentUserId = user?.id || '';

  return useQuery({
    queryKey: [...queryKeys.hives.all, 'recommended'],
    queryFn: async () => {
      const results = await hiveApiService.searchHives({ query: '' }); // Use search with empty query as recommendation
      // Transform the recommended hives
      return Array.isArray(results) ? results.map((dto: unknown) => transformHiveDTO(dto as unknown as HiveDTO, currentUserId)) : [];
    },
    enabled,
    staleTime: STALE_TIMES.STATIC_CONTENT,
    gcTime: CACHE_TIMES.LONG,
    meta: {
      description: 'Personalized hive recommendations',
      requiresAuth: true,
    },
  });
};

// ============================================================================
// MUTATION HOOKS
// ============================================================================

/**
 * Create new hive mutation
 */
export const useCreateHive = () => {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const currentUserId = user?.id || '';

  return useMutation({
    mutationFn: async (hiveData: CreateHiveRequest) => {
      const dto = await hiveApiService.createHive(hiveData);
      return transformHiveDTO(dto as unknown as HiveDTO, currentUserId);
    },
    mutationKey: ['hives', 'create'],

    onMutate: async (newHive) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({ queryKey: queryKeys.hives.all });

      // Snapshot previous value
      const previousHives = queryClient.getQueryData(queryKeys.hives.list());

      // Optimistically add new hive to the list
      if (previousHives && Array.isArray(previousHives)) {
        const optimisticHive: Partial<Hive> = {
          id: 'temp-' + Date.now(),
          name: newHive.name,
          description: newHive.description,
          isPublic: newHive.isPublic,
          memberCount: 1,
          isOwner: true,
          isMember: true,
          createdAt: new Date().toISOString(),
          ...newHive,
        };
        
        queryClient.setQueryData(
          queryKeys.hives.list(), 
          [optimisticHive, ...previousHives]
        );
      }

      return { previousHives };
    },

    onSuccess: (newHive) => {
      // Remove optimistic update and add real hive
      queryClient.setQueryData(
        queryKeys.hives.list(),
        (old: Hive[] | undefined) => {
          if (!old) return [newHive];
          return [newHive, ...old.filter(h => !h.id.toString().startsWith('temp-'))];
        }
      );

      // Cache the new hive details
      queryClient.setQueryData(queryKeys.hives.detail(newHive.id.toString()), newHive);

      // Track hive creation
      if (typeof window !== 'undefined' && 'gtag' in window) {
        // @ts-expect-error - gtag is loaded by Google Analytics script
        window.gtag('event', 'create_hive', {
          hive_type: !(newHive as { isPrivate?: boolean }).isPrivate ? 'public' : 'private',
        });
      }
    },

    onError: (error, newHive, context) => {
      // Error handled by error boundary and toast notifications
      
      // Rollback optimistic update
      if (context?.previousHives) {
        queryClient.setQueryData(queryKeys.hives.list(), context.previousHives);
      }
    },

    onSettled: () => {
      // Always refetch to ensure consistency
      queryClient.invalidateQueries({ queryKey: queryKeys.hives.all });
    },

    meta: {
      description: 'Create new hive',
      requiresAuth: true,
    },
  });
};

/**
 * Update hive mutation
 */
export const useUpdateHive = () => {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const currentUserId = user?.id || '';

  return useMutation({
    mutationFn: async ({ hiveId, updates }: { hiveId: string; updates: UpdateHiveRequest }) => {
      const dto = await hiveApiService.updateHive(parseInt(hiveId, 10), updates);
      return transformHiveDTO(dto as unknown as HiveDTO, currentUserId);
    },
    mutationKey: ['hives', 'update'],

    onMutate: async ({ hiveId, updates }) => {
      // Cancel outgoing refetches for this hive
      await queryClient.cancelQueries({ queryKey: queryKeys.hives.detail(hiveId) });

      // Snapshot previous value
      const previousHive = queryClient.getQueryData<Hive>(queryKeys.hives.detail(hiveId));

      // Optimistically update hive data
      if (previousHive) {
        const optimisticHive = { ...previousHive, ...updates };
        queryClient.setQueryData(queryKeys.hives.detail(hiveId), optimisticHive);

        // Also update in the list if it exists
        queryClient.setQueryData(
          queryKeys.hives.list(),
          (old: Hive[] | undefined) => {
            if (!old) return old;
            return old.map(hive => 
              hive.id === hiveId ? optimisticHive : hive
            );
          }
        );
      }

      return { previousHive, hiveId };
    },

    onSuccess: (updatedHive, { hiveId }) => {
      // Update with server response
      queryClient.setQueryData(queryKeys.hives.detail(hiveId), updatedHive);
      
      // Update in list as well
      queryClient.setQueryData(
        queryKeys.hives.list(),
        (old: Hive[] | undefined) => {
          if (!old) return old;
          return old.map(hive => 
            hive.id === hiveId ? updatedHive : hive
          );
        }
      );
    },

    onError: (error, { hiveId }, context) => {
      // Error handled by error boundary and toast notifications
      
      // Rollback optimistic update
      if (context?.previousHive) {
        queryClient.setQueryData(
          queryKeys.hives.detail(hiveId), 
          context.previousHive
        );
      }
    },

    onSettled: (data, error, { hiveId }) => {
      // Always refetch to ensure consistency
      queryClient.invalidateQueries({ queryKey: queryKeys.hives.detail(hiveId) });
    },

    meta: {
      description: 'Update hive details',
      requiresAuth: true,
    },
  });
};

/**
 * Delete hive mutation
 */
export const useDeleteHive = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (hiveId: string) => hiveApiService.deleteHive(parseInt(hiveId, 10)),
    mutationKey: ['hives', 'delete'],

    onMutate: async (hiveId) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({ queryKey: queryKeys.hives.all });

      // Snapshot previous data
      const previousHives = queryClient.getQueryData(queryKeys.hives.list());
      const previousHive = queryClient.getQueryData(queryKeys.hives.detail(hiveId));

      // Optimistically remove hive from list
      queryClient.setQueryData(
        queryKeys.hives.list(),
        (old: Hive[] | undefined) => {
          if (!old) return old;
          return old.filter(hive => hive.id !== hiveId);
        }
      );

      // Remove hive detail from cache
      queryClient.removeQueries({ queryKey: queryKeys.hives.detail(hiveId) });

      return { previousHives, previousHive, hiveId };
    },

    onError: (error, hiveId, context) => {
      // Error handled by error boundary and toast notifications
      
      // Rollback optimistic updates
      if (context?.previousHives) {
        queryClient.setQueryData(queryKeys.hives.list(), context.previousHives);
      }
      if (context?.previousHive) {
        queryClient.setQueryData(queryKeys.hives.detail(hiveId), context.previousHive);
      }
    },

    onSettled: () => {
      // Always refetch to ensure consistency
      queryClient.invalidateQueries({ queryKey: queryKeys.hives.all });
    },

    meta: {
      description: 'Delete hive',
      requiresAuth: true,
    },
  });
};

/**
 * Join hive mutation
 */
export const useJoinHive = () => {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const currentUserId = user?.id || '';

  return useMutation({
    mutationFn: async (hiveId: string) => {
      const dto = await hiveApiService.joinHive(parseInt(hiveId, 10));
      return transformHiveDTO(dto as unknown as HiveDTO, currentUserId);
    },
    mutationKey: ['hives', 'join'],

    onMutate: async (hiveId) => {
      // Cancel outgoing refetches for this hive
      await queryClient.cancelQueries({ queryKey: queryKeys.hives.detail(hiveId) });

      // Snapshot previous value
      const previousHive = queryClient.getQueryData<Hive>(queryKeys.hives.detail(hiveId));

      // Optimistically update hive membership
      if (previousHive) {
        const optimisticHive = {
          ...previousHive,
          isMember: true,
          memberCount: previousHive.memberCount + 1,
        };
        queryClient.setQueryData(queryKeys.hives.detail(hiveId), optimisticHive);
      }

      return { previousHive, hiveId };
    },

    onSuccess: (hiveData, hiveId) => {
      // Update hive data
      queryClient.setQueryData(queryKeys.hives.detail(hiveId), hiveData);
      
      // Add to user's hive list if not already there
      queryClient.setQueryData(
        queryKeys.hives.list(),
        (old: Hive[] | undefined) => {
          if (!old) return [hiveData];
          const exists = old.find(h => h.id === hiveId);
          return exists ? old.map(h => h.id === hiveId ? hiveData : h) : [hiveData, ...old];
        }
      );

      // Invalidate members list to include new member
      queryClient.invalidateQueries({ queryKey: queryKeys.hives.members(hiveId) });
    },

    onError: (error, hiveId, context) => {
      // Error handled by error boundary and toast notifications
      
      // Rollback optimistic update
      if (context?.previousHive) {
        queryClient.setQueryData(
          queryKeys.hives.detail(hiveId),
          context.previousHive
        );
      }
    },

    meta: {
      description: 'Join hive',
      requiresAuth: true,
    },
  });
};

/**
 * Leave hive mutation
 */
export const useLeaveHive = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (hiveId: string) => hiveApiService.leaveHive(parseInt(hiveId, 10)),
    mutationKey: ['hives', 'leave'],

    onMutate: async (hiveId) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: queryKeys.hives.detail(hiveId) });
      await queryClient.cancelQueries({ queryKey: queryKeys.hives.list() });

      // Snapshot previous values
      const previousHive = queryClient.getQueryData<Hive>(queryKeys.hives.detail(hiveId));
      const previousHives = queryClient.getQueryData(queryKeys.hives.list());

      // Optimistically update hive membership
      if (previousHive) {
        const optimisticHive = {
          ...previousHive,
          isMember: false,
          memberCount: Math.max(0, previousHive.memberCount - 1),
        };
        queryClient.setQueryData(queryKeys.hives.detail(hiveId), optimisticHive);
      }

      // Remove from user's hive list
      queryClient.setQueryData(
        queryKeys.hives.list(),
        (old: Hive[] | undefined) => {
          if (!old) return old;
          return old.filter(hive => hive.id !== hiveId);
        }
      );

      return { previousHive, previousHives, hiveId };
    },

    onSuccess: (data, hiveId) => {
      // Ensure hive is removed from lists
      queryClient.setQueryData(
        queryKeys.hives.list(),
        (old: Hive[] | undefined) => {
          if (!old) return old;
          return old.filter(hive => hive.id !== hiveId);
        }
      );

      // Invalidate presence and other related data
      invalidateQueries.hive(hiveId);
    },

    onError: (error, hiveId, context) => {
      // Error handled by error boundary and toast notifications
      
      // Rollback optimistic updates
      if (context?.previousHive) {
        queryClient.setQueryData(queryKeys.hives.detail(hiveId), context.previousHive);
      }
      if (context?.previousHives) {
        queryClient.setQueryData(queryKeys.hives.list(), context.previousHives);
      }
    },

    meta: {
      description: 'Leave hive',
      requiresAuth: true,
    },
  });
};

// ============================================================================
// COMPOUND HOOKS
// ============================================================================

/**
 * Complete hive data hook with all related information
 */
export const useHiveDetails = (hiveId: string, enabled = true) => {
  const hiveQuery = useHive(hiveId, enabled);
  const membersQuery = useHiveMembers(hiveId, enabled && !!hiveQuery.data);

  return {
    // Data
    hive: hiveQuery.data,
    members: membersQuery.data || [],
    
    // Loading states
    isLoading: hiveQuery.isLoading || membersQuery.isLoading,
    isHiveLoading: hiveQuery.isLoading,
    isMembersLoading: membersQuery.isLoading,
    
    // Error states  
    error: hiveQuery.error || membersQuery.error,
    hiveError: hiveQuery.error,
    membersError: membersQuery.error,
    
    // Computed values
    memberCount: hiveQuery.data?.memberCount || 0,
    // Note: isOwner/isMember should be computed on frontend based on current user
    isOwner: false, // TODO: Compare hiveQuery.data?.ownerId with current user ID
    isMember: false, // TODO: Check if current user is in members list
    isPublic: !(hiveQuery.data as { isPrivate?: boolean })?.isPrivate,
    
    // Utilities
    refetchHive: hiveQuery.refetch,
    refetchMembers: membersQuery.refetch,
    refetchAll: () => {
      hiveQuery.refetch();
      membersQuery.refetch();
    },
  };
};

/**
 * Hive management hook with all CRUD operations
 */
export const useHiveManagement = () => {
  const createMutation = useCreateHive();
  const updateMutation = useUpdateHive();
  const deleteMutation = useDeleteHive();
  const joinMutation = useJoinHive();
  const leaveMutation = useLeaveHive();

  return {
    // Mutations
    createHive: createMutation.mutate,
    updateHive: updateMutation.mutate,
    deleteHive: deleteMutation.mutate,
    joinHive: joinMutation.mutate,
    leaveHive: leaveMutation.mutate,
    
    // Loading states
    isCreating: createMutation.isPending,
    isUpdating: updateMutation.isPending,
    isDeleting: deleteMutation.isPending,
    isJoining: joinMutation.isPending,
    isLeaving: leaveMutation.isPending,
    
    // Error states
    createError: createMutation.error,
    updateError: updateMutation.error,
    deleteError: deleteMutation.error,
    joinError: joinMutation.error,
    leaveError: leaveMutation.error,
    
    // Success states
    isCreateSuccess: createMutation.isSuccess,
    isUpdateSuccess: updateMutation.isSuccess,
    isDeleteSuccess: deleteMutation.isSuccess,
    isJoinSuccess: joinMutation.isSuccess,
    isLeaveSuccess: leaveMutation.isSuccess,
  };
};