/**
 * Buddy System Hooks
 * Export all buddy-related React hooks for easy importing
 */

// Matching hooks - only export what exists
export {
  useBuddyMatching,
  useSearchBuddies,
  usePendingMatches,
  useBuddyMatch,
  useAcceptMatch,
  useDeclineMatch,
  useCancelMatch,
  buddyMatchingKeys,
} from './useBuddyMatching';

// Other hooks - export only main hooks for now
export { useBuddyProfile } from './useBuddyProfile';
export { useBuddySessions } from './useBuddySessions';
export { useBuddyStats } from './useBuddyStats';
export { useBuddyInvitations } from './useBuddyInvitations';
export { useBuddyMessages } from './useBuddyMessages';

/**
 * Combined query keys for all buddy features
 * Useful for invalidating all buddy-related queries
 */
export const allBuddyKeys = {
  all: ['buddy'] as const,
  profile: ['buddy', 'profile'] as const,
  matching: ['buddy', 'matching'] as const,
  sessions: ['buddy', 'sessions'] as const,
  stats: ['buddy', 'stats'] as const,
  invitations: ['buddy', 'invitations'] as const,
  messages: ['buddy', 'messages'] as const,
};

/**
 * Utility type for all buddy query keys
 */
export type BuddyQueryKeys = typeof allBuddyKeys;