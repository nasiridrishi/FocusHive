import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type {
  BuddyInvitation,
  SessionType,
} from '@/contracts/buddy';
import { buddyService } from '../services/buddyService';

// Query keys factory
export const buddyInvitationKeys = {
  all: ['buddy', 'invitations'] as const,
  received: () => [...buddyInvitationKeys.all, 'received'] as const,
  sent: () => [...buddyInvitationKeys.all, 'sent'] as const,
  invitation: (invitationId: string) => [...buddyInvitationKeys.all, invitationId] as const,
};

/**
 * Hook to get received buddy invitations
 */
export function useReceivedInvitations() {
  return useQuery({
    queryKey: buddyInvitationKeys.received(),
    queryFn: () => buddyService.getReceivedInvitations(),
    staleTime: 30000, // 30 seconds
    gcTime: 2 * 60 * 1000, // 2 minutes
    refetchInterval: 60000, // Refetch every minute for real-time updates
  });
}

/**
 * Hook to send a buddy invitation
 */
export function useSendInvitation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitation: {
      toUserId: number;
      sessionType: SessionType;
      message?: string;
      scheduledFor?: string;
    }) => buddyService.sendInvitation(invitation),
    onSuccess: (sentInvitation) => {
      // Cache the individual invitation
      queryClient.setQueryData(
        buddyInvitationKeys.invitation(sentInvitation.id),
        sentInvitation
      );

      // Add to sent invitations if we're tracking them
      queryClient.setQueryData<BuddyInvitation[]>(
        buddyInvitationKeys.sent(),
        (old) => {
          if (!old) return [sentInvitation];
          return [sentInvitation, ...old];
        }
      );
    },
    onError: (error) => {
      console.error('Failed to send buddy invitation:', error);
    },
  });
}

/**
 * Hook to accept a buddy invitation
 */
export function useAcceptInvitation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: string) =>
      buddyService.acceptInvitation(invitationId),
    onSuccess: (acceptedInvitation) => {
      // Update the invitation in cache
      queryClient.setQueryData(
        buddyInvitationKeys.invitation(acceptedInvitation.id),
        acceptedInvitation
      );

      // Update received invitations - either update or remove based on status
      queryClient.setQueryData<BuddyInvitation[]>(
        buddyInvitationKeys.received(),
        (old) => {
          if (!old) return old;
          return old.map(invitation =>
            invitation.id === acceptedInvitation.id ? acceptedInvitation : invitation
          );
        }
      );

      // Invalidate active sessions as accepting an invitation might create a session
      queryClient.invalidateQueries({ queryKey: ['buddy', 'sessions', 'active'] });
    },
    onError: (error) => {
      console.error('Failed to accept invitation:', error);
    },
  });
}

/**
 * Hook to decline a buddy invitation
 */
export function useDeclineInvitation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: string) =>
      buddyService.declineInvitation(invitationId),
    onSuccess: (declinedInvitation) => {
      // Update the invitation in cache
      queryClient.setQueryData(
        buddyInvitationKeys.invitation(declinedInvitation.id),
        declinedInvitation
      );

      // Remove from received invitations since it's now declined
      queryClient.setQueryData<BuddyInvitation[]>(
        buddyInvitationKeys.received(),
        (old) => {
          if (!old) return old;
          return old.filter(invitation => invitation.id !== declinedInvitation.id);
        }
      );
    },
    onError: (error) => {
      console.error('Failed to decline invitation:', error);
    },
  });
}

/**
 * Combined hook for buddy invitation functionality
 */
export function useBuddyInvitations() {
  const queryClient = useQueryClient();
  const receivedInvitations = useReceivedInvitations();
  const sendInvitation = useSendInvitation();
  const acceptInvitation = useAcceptInvitation();
  const declineInvitation = useDeclineInvitation();

  return {
    // Data
    receivedInvitations: receivedInvitations.data || [],
    isLoadingInvitations: receivedInvitations.isLoading,
    invitationsError: receivedInvitations.error,

    // Actions
    sendInvitation: sendInvitation.mutate,
    acceptInvitation: acceptInvitation.mutate,
    declineInvitation: declineInvitation.mutate,

    // Loading states
    isSendingInvitation: sendInvitation.isPending,
    isAcceptingInvitation: acceptInvitation.isPending,
    isDecliningInvitation: declineInvitation.isPending,

    // Helpers
    refetchInvitations: receivedInvitations.refetch,
    hasInvitations: (receivedInvitations.data?.length || 0) > 0,
    pendingInvitations: receivedInvitations.data?.filter(
      invitation => invitation.status === 'pending'
    ) || [],
    hasPendingInvitations: (receivedInvitations.data?.filter(
      invitation => invitation.status === 'pending'
    ).length || 0) > 0,

    // Quick access to invitation actions with optimistic updates
    quickAccept: (invitationId: string) => {
      // Optimistically update the UI
      queryClient.setQueryData<BuddyInvitation[]>(
        buddyInvitationKeys.received(),
        (old) => {
          if (!old) return old;
          return old.map(invitation =>
            invitation.id === invitationId
              ? { ...invitation, status: 'accepted' as const }
              : invitation
          );
        }
      );

      acceptInvitation.mutate(invitationId);
    },

    quickDecline: (invitationId: string) => {
      // Optimistically remove from UI
      queryClient.setQueryData<BuddyInvitation[]>(
        buddyInvitationKeys.received(),
        (old) => {
          if (!old) return old;
          return old.filter(invitation => invitation.id !== invitationId);
        }
      );

      declineInvitation.mutate(invitationId);
    },
  };
}