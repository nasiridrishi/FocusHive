import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import type { BuddyMessage, BuddyWebSocketEvent } from '@/contracts/buddy';
import { buddyService } from '../services/buddyService';

// Query keys factory
export const buddyMessageKeys = {
  all: ['buddy', 'messages'] as const,
  sessionMessages: (sessionId: string) => [...buddyMessageKeys.all, 'session', sessionId] as const,
  message: (messageId: string) => [...buddyMessageKeys.all, messageId] as const,
};

/**
 * Hook to get messages for a buddy session
 */
export function useSessionMessages(sessionId: string) {
  const queryClient = useQueryClient();
  const [isSubscribed, setIsSubscribed] = useState(false);

  const query = useQuery({
    queryKey: buddyMessageKeys.sessionMessages(sessionId),
    queryFn: () => buddyService.getSessionMessages(sessionId),
    staleTime: 30000, // 30 seconds
    gcTime: 2 * 60 * 1000, // 2 minutes
    enabled: !!sessionId,
  });

  // Subscribe to real-time message updates
  useEffect(() => {
    if (sessionId && !isSubscribed) {
      const unsubscribe = buddyService.subscribeToSessionUpdates(
        sessionId,
        (event: BuddyWebSocketEvent) => {
          if (event.type === 'message_received') {
            // Invalidate messages to fetch the new message
            queryClient.invalidateQueries({
              queryKey: buddyMessageKeys.sessionMessages(sessionId)
            });
          }
        }
      );
      setIsSubscribed(true);

      return () => {
        unsubscribe();
        setIsSubscribed(false);
      };
    }
  }, [sessionId, queryClient, isSubscribed]);

  return query;
}

/**
 * Hook to send a message during a buddy session
 */
export function useSendMessage() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      sessionId,
      message
    }: {
      sessionId: string;
      message: {
        content: string;
        type: 'text' | 'emoji' | 'encouragement' | 'task_update';
      };
    }) => buddyService.sendMessage(sessionId, message),
    onMutate: async (variables) => {
      // Cancel any outgoing refetches for this session's messages
      await queryClient.cancelQueries({
        queryKey: buddyMessageKeys.sessionMessages(variables.sessionId)
      });

      // Snapshot the previous value
      const previousMessages = queryClient.getQueryData<BuddyMessage[]>(
        buddyMessageKeys.sessionMessages(variables.sessionId)
      );

      // Optimistically update to the new message
      const optimisticMessage: BuddyMessage = {
        id: `temp-${Date.now()}`,
        sessionId: variables.sessionId,
        senderId: 0, // Will be set by server
        content: variables.message.content,
        type: variables.message.type,
        createdAt: new Date().toISOString(),
        reactions: [],
      };

      queryClient.setQueryData<BuddyMessage[]>(
        buddyMessageKeys.sessionMessages(variables.sessionId),
        (old) => {
          if (!old) return [optimisticMessage];
          return [...old, optimisticMessage];
        }
      );

      // Return a context object with the snapshotted value
      return { previousMessages };
    },
    onError: (err, variables, context) => {
      // If the mutation fails, use the context returned from onMutate to roll back
      queryClient.setQueryData(
        buddyMessageKeys.sessionMessages(variables.sessionId),
        context?.previousMessages
      );
      console.error('Failed to send message:', err);
    },
    onSettled: (data, error, variables) => {
      // Always refetch after error or success
      queryClient.invalidateQueries({
        queryKey: buddyMessageKeys.sessionMessages(variables.sessionId)
      });
    },
  });
}

/**
 * Hook to add a reaction to a message
 */
export function useAddReaction() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ messageId, emoji }: { messageId: string; emoji: string }) =>
      buddyService.addMessageReaction(messageId, emoji),
    onMutate: async (variables) => {
      // Find which session this message belongs to by checking all cached sessions
      const queryCache = queryClient.getQueryCache();
      let sessionId: string | null = null;

      queryCache.findAll({ queryKey: buddyMessageKeys.all }).forEach((query) => {
        const data = query.state.data as BuddyMessage[] | undefined;
        if (data?.some(msg => msg.id === variables.messageId)) {
          const key = query.queryKey;
          if (key.length >= 4 && key[2] === 'session') {
            sessionId = key[3] as string;
          }
        }
      });

      if (sessionId) {
        // Optimistically add the reaction
        queryClient.setQueryData<BuddyMessage[]>(
          buddyMessageKeys.sessionMessages(sessionId),
          (old) => {
            if (!old) return old;
            return old.map(message => {
              if (message.id === variables.messageId) {
                const existingReaction = message.reactions?.find(
                  r => r.emoji === variables.emoji && r.userId === 0 // Current user
                );

                if (existingReaction) {
                  // Remove reaction if it already exists
                  return {
                    ...message,
                    reactions: message.reactions?.filter(r => r !== existingReaction) || []
                  };
                } else {
                  // Add new reaction
                  return {
                    ...message,
                    reactions: [
                      ...(message.reactions || []),
                      { userId: 0, emoji: variables.emoji }
                    ]
                  };
                }
              }
              return message;
            });
          }
        );
      }

      return { sessionId };
    },
    onSuccess: (updatedMessage, variables, context) => {
      if (context?.sessionId) {
        // Update with the actual response from server
        queryClient.setQueryData<BuddyMessage[]>(
          buddyMessageKeys.sessionMessages(context.sessionId),
          (old) => {
            if (!old) return old;
            return old.map(message =>
              message.id === updatedMessage.id ? updatedMessage : message
            );
          }
        );
      }
    },
    onError: (error, variables, context) => {
      // Revert optimistic update on error
      if (context?.sessionId) {
        queryClient.invalidateQueries({
          queryKey: buddyMessageKeys.sessionMessages(context.sessionId)
        });
      }
      console.error('Failed to add reaction:', error);
    },
  });
}

/**
 * Combined hook for buddy messaging functionality
 */
export function useBuddyMessages(sessionId?: string) {
  const sessionMessages = useSessionMessages(sessionId || '');
  const sendMessage = useSendMessage();
  const addReaction = useAddReaction();

  return {
    // Data
    messages: sessionMessages.data || [],
    isLoadingMessages: sessionMessages.isLoading,
    messagesError: sessionMessages.error,

    // Actions
    sendMessage: (message: {
      content: string;
      type: 'text' | 'emoji' | 'encouragement' | 'task_update';
    }) => {
      if (!sessionId) {
        console.error('Cannot send message: no session ID provided');
        return;
      }
      sendMessage.mutate({ sessionId, message });
    },

    addReaction: addReaction.mutate,

    // Loading states
    isSendingMessage: sendMessage.isPending,
    isAddingReaction: addReaction.isPending,

    // Helpers
    refetchMessages: sessionMessages.refetch,
    hasMessages: (sessionMessages.data?.length || 0) > 0,

    // Message utilities
    getMessagesByType: (type: 'text' | 'emoji' | 'encouragement' | 'task_update') =>
      sessionMessages.data?.filter(msg => msg.type === type) || [],

    getRecentMessages: (count = 10) =>
      sessionMessages.data?.slice(-count) || [],

    // Quick send helpers
    sendTextMessage: (content: string) => {
      if (!sessionId) return;
      sendMessage.mutate({
        sessionId,
        message: { content, type: 'text' }
      });
    },

    sendEncouragement: (content: string) => {
      if (!sessionId) return;
      sendMessage.mutate({
        sessionId,
        message: { content, type: 'encouragement' }
      });
    },

    sendTaskUpdate: (content: string) => {
      if (!sessionId) return;
      sendMessage.mutate({
        sessionId,
        message: { content, type: 'task_update' }
      });
    },

    sendEmoji: (emoji: string) => {
      if (!sessionId) return;
      sendMessage.mutate({
        sessionId,
        message: { content: emoji, type: 'emoji' }
      });
    },
  };
}