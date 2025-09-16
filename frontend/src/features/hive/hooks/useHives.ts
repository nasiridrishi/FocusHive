import { useQuery, useMutation, useQueryClient, UseQueryResult } from '@tanstack/react-query';
import { useCallback, useEffect } from 'react';
import { getHiveService, HiveService } from '../services/hiveService';
import type {
  Hive,
  CreateHiveRequest,
  UpdateHiveRequest,
  HiveSearchParams,
  HiveMember
} from '@/contracts/hive';
import type { PaginatedResponse } from '@/contracts/common';

// Query keys for cache management
const QUERY_KEYS = {
  all: ['hives'] as const,
  lists: () => [...QUERY_KEYS.all, 'list'] as const,
  list: (params: any) => [...QUERY_KEYS.lists(), params] as const,
  details: () => [...QUERY_KEYS.all, 'detail'] as const,
  detail: (id: number) => [...QUERY_KEYS.details(), id] as const,
  members: (id: number) => [...QUERY_KEYS.all, 'members', id] as const,
  search: (params: HiveSearchParams) => [...QUERY_KEYS.all, 'search', params] as const,
};

// Get singleton hive service instance
const hiveService = getHiveService();

/**
 * Hook to fetch all hives with pagination
 */
export function useHives(page = 0, size = 20): UseQueryResult<PaginatedResponse<Hive>, Error> {
  return useQuery({
    queryKey: QUERY_KEYS.list({ page, size }),
    queryFn: () => hiveService.getHives(page, size),
    staleTime: 1000 * 60 * 5, // 5 minutes
    gcTime: 1000 * 60 * 10, // 10 minutes
  });
}

/**
 * Hook to fetch a specific hive by ID
 */
export function useHive(hiveId: number | undefined): UseQueryResult<Hive, Error> {
  return useQuery({
    queryKey: QUERY_KEYS.detail(hiveId!),
    queryFn: () => hiveService.getHive(hiveId!),
    enabled: !!hiveId,
    staleTime: 1000 * 60 * 5,
    gcTime: 1000 * 60 * 10,
  });
}

/**
 * Hook to fetch hive members
 */
export function useHiveMembers(hiveId: number | undefined): UseQueryResult<PaginatedResponse<HiveMember>, Error> {
  return useQuery({
    queryKey: QUERY_KEYS.members(hiveId!),
    queryFn: () => hiveService.getHiveMembers(hiveId!),
    enabled: !!hiveId,
    staleTime: 1000 * 60 * 2, // 2 minutes (members change more frequently)
    gcTime: 1000 * 60 * 5,
  });
}

/**
 * Hook to search hives
 */
export function useSearchHives(params: HiveSearchParams): UseQueryResult<PaginatedResponse<Hive>, Error> {
  return useQuery({
    queryKey: QUERY_KEYS.search(params),
    queryFn: () => hiveService.searchHives(params),
    enabled: !!(params.query || params.tags?.length),
    staleTime: 1000 * 60 * 2,
    gcTime: 1000 * 60 * 5,
  });
}

/**
 * Hook to create a new hive
 */
export function useCreateHive() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateHiveRequest) => hiveService.createHive(request),
    onSuccess: (newHive) => {
      // Invalidate and refetch hives list
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.lists() });

      // Add the new hive to the cache
      queryClient.setQueryData(QUERY_KEYS.detail(newHive.id), newHive);

      // Optimistically update lists if they exist in cache
      queryClient.setQueriesData<PaginatedResponse<Hive>>(
        { queryKey: QUERY_KEYS.lists() },
        (old) => {
          if (!old) return old;
          return {
            ...old,
            content: [newHive, ...old.content],
            totalElements: old.totalElements + 1,
          };
        }
      );
    },
    onError: (error) => {
      console.error('Failed to create hive:', error);
    },
  });
}

/**
 * Hook to update a hive
 */
export function useUpdateHive(hiveId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: UpdateHiveRequest) => hiveService.updateHive(hiveId, request),
    onMutate: async (request) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({ queryKey: QUERY_KEYS.detail(hiveId) });

      // Snapshot the previous value
      const previousHive = queryClient.getQueryData<Hive>(QUERY_KEYS.detail(hiveId));

      // Optimistically update to the new value
      if (previousHive) {
        queryClient.setQueryData(QUERY_KEYS.detail(hiveId), {
          ...previousHive,
          ...request,
        });
      }

      return { previousHive };
    },
    onError: (err, request, context) => {
      // If the mutation fails, use the context returned from onMutate to roll back
      if (context?.previousHive) {
        queryClient.setQueryData(QUERY_KEYS.detail(hiveId), context.previousHive);
      }
    },
    onSettled: () => {
      // Always refetch after error or success
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.detail(hiveId) });
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.lists() });
    },
  });
}

/**
 * Hook to delete a hive
 */
export function useDeleteHive() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (hiveId: number) => hiveService.deleteHive(hiveId),
    onSuccess: (_, hiveId) => {
      // Remove from cache
      queryClient.removeQueries({ queryKey: QUERY_KEYS.detail(hiveId) });
      queryClient.removeQueries({ queryKey: QUERY_KEYS.members(hiveId) });

      // Invalidate lists
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.lists() });
    },
    onError: (error) => {
      console.error('Failed to delete hive:', error);
    },
  });
}

/**
 * Hook to join a hive
 */
export function useJoinHive() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (hiveId: number) => hiveService.joinHive(hiveId),
    onSuccess: (_, hiveId) => {
      // Invalidate hive details and members
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.detail(hiveId) });
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.members(hiveId) });

      // Optimistically update member count
      queryClient.setQueryData<Hive>(QUERY_KEYS.detail(hiveId), (old) => {
        if (!old) return old;
        return {
          ...old,
          memberCount: old.memberCount + 1,
        };
      });
    },
    onError: (error) => {
      console.error('Failed to join hive:', error);
    },
  });
}

/**
 * Hook to leave a hive
 */
export function useLeaveHive() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (hiveId: number) => hiveService.leaveHive(hiveId),
    onSuccess: (_, hiveId) => {
      // Invalidate hive details and members
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.detail(hiveId) });
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.members(hiveId) });

      // Optimistically update member count
      queryClient.setQueryData<Hive>(QUERY_KEYS.detail(hiveId), (old) => {
        if (!old) return old;
        return {
          ...old,
          memberCount: Math.max(0, old.memberCount - 1),
        };
      });
    },
    onError: (error) => {
      console.error('Failed to leave hive:', error);
    },
  });
}

/**
 * Hook to subscribe to real-time hive updates
 */
export function useHiveSubscription(hiveId: number | undefined, enabled = true) {
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!hiveId || !enabled) return;

    const unsubscribe = hiveService.subscribeToHiveUpdates(hiveId, (updatedHive) => {
      // Update cache with real-time data
      queryClient.setQueryData(QUERY_KEYS.detail(hiveId), updatedHive);

      // Update in lists as well
      queryClient.setQueriesData<PaginatedResponse<Hive>>(
        { queryKey: QUERY_KEYS.lists() },
        (old) => {
          if (!old) return old;
          const index = old.content.findIndex(h => h.id === hiveId);
          if (index === -1) return old;

          const newContent = [...old.content];
          newContent[index] = updatedHive;

          return {
            ...old,
            content: newContent,
          };
        }
      );
    });

    return unsubscribe;
  }, [hiveId, enabled, queryClient]);
}

/**
 * Hook to subscribe to presence updates in a hive
 */
export function useHivePresence(hiveId: number | undefined, enabled = true) {
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!hiveId || !enabled) return;

    const unsubscribe = hiveService.subscribeToPresence(hiveId, (presence) => {
      // You can handle presence updates here
      // For example, update a presence state or trigger UI updates
      queryClient.setQueryData(['hive-presence', hiveId], presence);
    });

    return unsubscribe;
  }, [hiveId, enabled, queryClient]);
}

/**
 * Hook to update presence status
 */
export function useUpdatePresence(hiveId: number | undefined) {
  const updatePresence = useCallback(
    (status: 'active' | 'away' | 'offline') => {
      if (!hiveId) return;
      hiveService.updatePresenceStatus(hiveId, status);
    },
    [hiveId]
  );

  return updatePresence;
}

/**
 * Hook to get multiple hives by IDs (batch operation)
 */
export function useHivesByIds(ids: number[]): UseQueryResult<Hive[], Error> {
  return useQuery({
    queryKey: ['hives', 'batch', ids],
    queryFn: () => hiveService.getHivesByIds(ids),
    enabled: ids.length > 0,
    staleTime: 1000 * 60 * 5,
    gcTime: 1000 * 60 * 10,
  });
}

/**
 * Hook to prefetch hive data
 */
export function usePrefetchHive() {
  const queryClient = useQueryClient();

  return useCallback((hiveId: number) => {
    queryClient.prefetchQuery({
      queryKey: QUERY_KEYS.detail(hiveId),
      queryFn: () => hiveService.getHive(hiveId),
      staleTime: 1000 * 60 * 5,
    });
  }, [queryClient]);
}

/**
 * Hook to invalidate hive cache
 */
export function useInvalidateHives() {
  const queryClient = useQueryClient();

  return {
    invalidateAll: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.all }),
    invalidateList: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.lists() }),
    invalidateDetail: (hiveId: number) =>
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.detail(hiveId) }),
    invalidateMembers: (hiveId: number) =>
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.members(hiveId) }),
  };
}

// Export types for Agent 3 to use
export type {
  Hive,
  CreateHiveRequest,
  UpdateHiveRequest,
  HiveSearchParams,
  PaginatedResponse,
  HiveMember
} from '@/services/api/hiveApi';