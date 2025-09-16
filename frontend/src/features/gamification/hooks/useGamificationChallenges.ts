import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type { Challenge, UserChallenge } from '@/contracts/gamification';
import { gamificationService } from '../services/gamificationService';

// Query keys factory
export const challengeKeys = {
  all: ['gamification', 'challenges'] as const,
  available: () => [...challengeKeys.all, 'available'] as const,
  userChallenge: (userId: number, challengeId: string) =>
    [...challengeKeys.all, 'user', userId, challengeId] as const,
  progress: (userId: number, challengeId: string) =>
    [...challengeKeys.all, 'progress', userId, challengeId] as const,
  leaderboard: (challengeId: string) => [...challengeKeys.all, 'leaderboard', challengeId] as const,
};

/**
 * Hook to get available challenges
 */
export function useAvailableChallenges() {
  return useQuery({
    queryKey: challengeKeys.available(),
    queryFn: () => gamificationService.getAvailableChallenges(),
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 15 * 60 * 1000, // 15 minutes
  });
}

/**
 * Hook to join a challenge
 */
export function useJoinChallenge() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ userId, challengeId }: { userId: number; challengeId: string }) =>
      gamificationService.joinChallenge(userId, challengeId),
    onSuccess: (participation, variables) => {
      // Invalidate available challenges
      queryClient.invalidateQueries({
        queryKey: challengeKeys.available(),
      });

      // Invalidate user challenge data
      queryClient.invalidateQueries({
        queryKey: challengeKeys.userChallenge(variables.userId, variables.challengeId),
      });
    },
    onError: (error) => {
      console.error('Failed to join challenge:', error);
    },
  });
}

/**
 * Hook to complete a challenge
 */
export function useCompleteChallenge() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ userId, challengeId }: { userId: number; challengeId: string }) =>
      gamificationService.completeChallenge(userId, challengeId),
    onSuccess: (completion, variables) => {
      // Invalidate all challenge-related queries
      queryClient.invalidateQueries({
        queryKey: challengeKeys.all,
      });

      // Invalidate points (challenge rewards)
      queryClient.invalidateQueries({
        queryKey: ['gamification', 'points', 'user', variables.userId],
      });

      // Show notification
      console.log('Challenge completed!', completion);
    },
    onError: (error) => {
      console.error('Failed to complete challenge:', error);
    },
  });
}

/**
 * Hook to get challenge progress
 */
export function useChallengeProgress(userId: number, challengeId: string) {
  return useQuery({
    queryKey: challengeKeys.progress(userId, challengeId),
    queryFn: () => gamificationService.getChallengeProgress(userId, challengeId),
    staleTime: 30000, // 30 seconds
    gcTime: 5 * 60 * 1000, // 5 minutes
    enabled: userId > 0 && !!challengeId,
    refetchInterval: 60000, // Refresh every minute for real-time updates
  });
}

/**
 * Hook to leave a challenge
 */
export function useLeaveChallenge() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ userId, challengeId }: { userId: number; challengeId: string }) =>
      gamificationService.leaveChallenge(userId, challengeId),
    onSuccess: (_, variables) => {
      // Invalidate challenge queries
      queryClient.invalidateQueries({
        queryKey: challengeKeys.all,
      });
    },
    onError: (error) => {
      console.error('Failed to leave challenge:', error);
    },
  });
}

/**
 * Hook to get challenge leaderboard
 */
export function useChallengeLeaderboard(challengeId: string) {
  return useQuery({
    queryKey: challengeKeys.leaderboard(challengeId),
    queryFn: () => gamificationService.getChallengeLeaderboard(challengeId),
    staleTime: 60000, // 1 minute
    gcTime: 5 * 60 * 1000, // 5 minutes
    enabled: !!challengeId,
    refetchInterval: 60000, // Refresh every minute
  });
}

/**
 * Combined hook for challenge functionality
 */
export function useGamificationChallenges(userId: number) {
  const availableChallenges = useAvailableChallenges();
  const joinChallenge = useJoinChallenge();
  const completeChallenge = useCompleteChallenge();
  const leaveChallenge = useLeaveChallenge();

  return {
    // Challenges
    challenges: availableChallenges.data || [],
    isLoadingChallenges: availableChallenges.isLoading,
    challengesError: availableChallenges.error,

    // Actions
    joinChallenge: (challengeId: string) =>
      joinChallenge.mutate({ userId, challengeId }),
    completeChallenge: (challengeId: string) =>
      completeChallenge.mutate({ userId, challengeId }),
    leaveChallenge: (challengeId: string) =>
      leaveChallenge.mutate({ userId, challengeId }),

    // Loading states
    isJoining: joinChallenge.isPending,
    isCompleting: completeChallenge.isPending,
    isLeaving: leaveChallenge.isPending,

    // Helpers
    refetchChallenges: availableChallenges.refetch,
    hasChallenges: (availableChallenges.data?.length || 0) > 0,

    // Get challenge by ID
    getChallengeById: (challengeId: string) =>
      availableChallenges.data?.find(c => c.id === challengeId),

    // Filter challenges
    getChallengesByType: (type: string) =>
      availableChallenges.data?.filter(c => c.type === type) || [],

    getChallengesByDifficulty: (difficulty: string) =>
      availableChallenges.data?.filter(c => c.difficulty === difficulty) || [],

    getActiveChallenges: () =>
      availableChallenges.data?.filter(c => c.status === 'active') || [],

    // Check if user can join
    canJoinChallenge: (challengeId: string) => {
      const challenge = availableChallenges.data?.find(c => c.id === challengeId);
      if (!challenge) return false;
      return challenge.status === 'active' &&
        (!challenge.maxParticipants || challenge.currentParticipants < challenge.maxParticipants);
    },
  };
}