import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type {
  BuddyProfile,
  BuddyPreferences,
  BuddyAvailability,
} from '@/contracts/buddy';
import { buddyService } from '../services/buddyService';

// Query keys factory
export const buddyProfileKeys = {
  all: ['buddy', 'profile'] as const,
  myProfile: () => [...buddyProfileKeys.all, 'me'] as const,
  profile: (userId: number) => [...buddyProfileKeys.all, userId] as const,
  preferences: () => [...buddyProfileKeys.all, 'preferences'] as const,
  availability: () => [...buddyProfileKeys.all, 'availability'] as const,
};

/**
 * Hook to get current user's buddy profile
 */
export function useBuddyProfile() {
  return useQuery({
    queryKey: buddyProfileKeys.myProfile(),
    queryFn: () => buddyService.getMyProfile(),
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
  });
}

/**
 * Hook to get another user's buddy profile
 */
export function useOtherBuddyProfile(userId: number) {
  return useQuery({
    queryKey: buddyProfileKeys.profile(userId),
    queryFn: () => buddyService.getBuddyProfile(userId),
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
    enabled: userId > 0,
  });
}

/**
 * Hook to update buddy preferences
 */
export function useUpdateBuddyPreferences() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (preferences: BuddyPreferences) =>
      buddyService.updatePreferences(preferences),
    onSuccess: (updatedPreferences) => {
      // Update the profile cache with new preferences
      queryClient.setQueryData<BuddyProfile>(
        buddyProfileKeys.myProfile(),
        (old) => {
          if (!old) return old;
          return { ...old, preferences: updatedPreferences };
        }
      );

      // Also update the preferences cache if it exists
      queryClient.setQueryData(buddyProfileKeys.preferences(), updatedPreferences);
    },
    onError: (error) => {
      console.error('Failed to update buddy preferences:', error);
    },
  });
}

/**
 * Hook to update buddy availability
 */
export function useUpdateBuddyAvailability() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (availability: BuddyAvailability) =>
      buddyService.updateAvailability(availability),
    onSuccess: (updatedAvailability) => {
      // Update the profile cache with new availability
      queryClient.setQueryData<BuddyProfile>(
        buddyProfileKeys.myProfile(),
        (old) => {
          if (!old) return old;
          return { ...old, availability: updatedAvailability };
        }
      );

      // Also update the availability cache if it exists
      queryClient.setQueryData(buddyProfileKeys.availability(), updatedAvailability);
    },
    onError: (error) => {
      console.error('Failed to update buddy availability:', error);
    },
  });
}

/**
 * Combined hook that provides all profile functionality
 */
export function useBuddyProfileManagement() {
  const profile = useBuddyProfile();
  const updatePreferences = useUpdateBuddyPreferences();
  const updateAvailability = useUpdateBuddyAvailability();

  return {
    // Data
    profile: profile.data,
    isLoading: profile.isLoading,
    error: profile.error,

    // Actions
    updatePreferences: updatePreferences.mutate,
    updateAvailability: updateAvailability.mutate,

    // Loading states
    isUpdatingPreferences: updatePreferences.isPending,
    isUpdatingAvailability: updateAvailability.isPending,

    // Helper to refresh profile
    refetchProfile: profile.refetch,
  };
}