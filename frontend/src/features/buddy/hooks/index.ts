/**
 * Buddy System Hooks
 * Export all buddy-related React hooks for easy importing
 */

// Profile hooks
export {
  useBuddyProfile,
  useOtherBuddyProfile,
  useUpdateBuddyPreferences,
  useUpdateBuddyAvailability,
  useBuddyProfileManagement,
  buddyProfileKeys,
} from './useBuddyProfile';

// Matching hooks
export {
  useSearchBuddies,
  usePendingMatches,
  useBuddyMatch,
  useAcceptMatch,
  useDeclineMatch,
  useCancelMatch,
  useBuddyMatching,
  buddyMatchingKeys,
} from './useBuddyMatching';

// Session hooks
export {
  useCreateSession,
  useActiveSessions,
  useActiveSession,
  useSessionHistory,
  useBuddySession,
  useSessionActions,
  useSessionCheckIn,
  useBuddySessions,
  buddySessionKeys,
} from './useBuddySessions';

// Statistics hooks
export {
  useBuddyStats,
  useTopBuddies,
  useBuddyLeaderboard,
  useBuddyStatistics,
  buddyStatsKeys,
} from './useBuddyStats';

// Invitation hooks
export {
  useReceivedInvitations,
  useSendInvitation,
  useAcceptInvitation,
  useDeclineInvitation,
  useBuddyInvitations,
  buddyInvitationKeys,
} from './useBuddyInvitations';

// Message hooks
export {
  useSessionMessages,
  useSendMessage,
  useAddReaction,
  useBuddyMessages,
  buddyMessageKeys,
} from './useBuddyMessages';

// Re-export the buddy service for direct access if needed
export { buddyService } from '../services/buddyService';

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