import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type {
  BuddyMatch,
  BuddyMatchRequest,
  BuddySearchFilters,
  BuddySearchResponse,
  BuddyProfile,
} from '@/contracts/buddy';
import { buddyService } from '../services/buddyService';

// Query keys factory
export const buddyMatchingKeys = {
  all: ['buddy', 'matching'] as const,
  matches: () => [...buddyMatchingKeys.all, 'matches'] as const,
  pendingMatches: () => [...buddyMatchingKeys.matches(), 'pending'] as const,
  match: (matchId: string) => [...buddyMatchingKeys.matches(), matchId] as const,
  search: (filters: BuddySearchFilters) => [...buddyMatchingKeys.all, 'search', filters] as const,
};

/**
 * Hook to search for available buddies
 */
export function useSearchBuddies(filters: BuddySearchFilters, enabled = true) {
  return useQuery({
    queryKey: buddyMatchingKeys.search(filters),
    queryFn: () => buddyService.findBuddies(filters),
    staleTime: 60000, // 1 minute
    gcTime: 5 * 60 * 1000, // 5 minutes
    enabled: enabled && Object.keys(filters).length > 0,
  });
}

/**
 * Hook to get pending matches
 */
export function usePendingMatches() {
  return useQuery({
    queryKey: buddyMatchingKeys.pendingMatches(),
    queryFn: () => buddyService.getPendingMatches(),
    staleTime: 30000, // 30 seconds
    gcTime: 2 * 60 * 1000, // 2 minutes
    refetchInterval: 60000, // Refetch every minute for real-time updates
  });
}

/**
 * Hook to request a buddy match
 */
export function useBuddyMatch() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: BuddyMatchRequest) =>
      buddyService.requestMatch(request),
    onSuccess: (newMatch) => {
      // Add the new match to pending matches
      queryClient.setQueryData<BuddyMatch[]>(
        buddyMatchingKeys.pendingMatches(),
        (old) => {
          if (!old) return [newMatch];
          return [newMatch, ...old];
        }
      );

      // Cache the individual match
      queryClient.setQueryData(buddyMatchingKeys.match(newMatch.id), newMatch);
    },
    onError: (error) => {
      console.error('Failed to request buddy match:', error);
    },
  });
}

/**
 * Hook to accept a match
 */
export function useAcceptMatch() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (matchId: string) => buddyService.acceptMatch(matchId),
    onSuccess: (acceptedMatch) => {
      // Update the match in cache
      queryClient.setQueryData(buddyMatchingKeys.match(acceptedMatch.id), acceptedMatch);

      // Update pending matches - remove from pending since it's now accepted
      queryClient.setQueryData<BuddyMatch[]>(
        buddyMatchingKeys.pendingMatches(),
        (old) => {
          if (!old) return old;
          return old.map(match =>
            match.id === acceptedMatch.id ? acceptedMatch : match
          );
        }
      );
    },
    onError: (error) => {
      console.error('Failed to accept match:', error);
    },
  });
}

/**
 * Hook to decline a match
 */
export function useDeclineMatch() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (matchId: string) => buddyService.declineMatch(matchId),
    onSuccess: (declinedMatch) => {
      // Update the match in cache
      queryClient.setQueryData(buddyMatchingKeys.match(declinedMatch.id), declinedMatch);

      // Remove from pending matches
      queryClient.setQueryData<BuddyMatch[]>(
        buddyMatchingKeys.pendingMatches(),
        (old) => {
          if (!old) return old;
          return old.filter(match => match.id !== declinedMatch.id);
        }
      );
    },
    onError: (error) => {
      console.error('Failed to decline match:', error);
    },
  });
}

/**
 * Hook to cancel a match
 */
export function useCancelMatch() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (matchId: string) => buddyService.cancelMatch(matchId),
    onSuccess: (_, matchId) => {
      // Remove from pending matches
      queryClient.setQueryData<BuddyMatch[]>(
        buddyMatchingKeys.pendingMatches(),
        (old) => {
          if (!old) return old;
          return old.filter(match => match.id !== matchId);
        }
      );

      // Remove from individual match cache
      queryClient.removeQueries({ queryKey: buddyMatchingKeys.match(matchId) });
    },
    onError: (error) => {
      console.error('Failed to cancel match:', error);
    },
  });
}

/**
 * Combined hook for buddy matching functionality
 */
export function useBuddyMatching() {
  const pendingMatches = usePendingMatches();
  const requestMatch = useBuddyMatch();
  const acceptMatch = useAcceptMatch();
  const declineMatch = useDeclineMatch();
  const cancelMatch = useCancelMatch();

  return {
    // Data
    pendingMatches: pendingMatches.data || [],
    isLoadingMatches: pendingMatches.isLoading,
    matchesError: pendingMatches.error,

    // Actions
    requestMatch: requestMatch.mutate,
    acceptMatch: acceptMatch.mutate,
    declineMatch: declineMatch.mutate,
    cancelMatch: cancelMatch.mutate,

    // Loading states
    isRequestingMatch: requestMatch.isPending,
    isAcceptingMatch: acceptMatch.isPending,
    isDecliningMatch: declineMatch.isPending,
    isCancelingMatch: cancelMatch.isPending,

    // Helpers
    refetchMatches: pendingMatches.refetch,
    hasMatches: (pendingMatches.data?.length || 0) > 0,
  };
}