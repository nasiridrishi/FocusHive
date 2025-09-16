import { useQuery, useMutation, useQueryClient, UseQueryResult } from '@tanstack/react-query';
import { useCallback, useEffect, useState, useRef } from 'react';
import { chatService } from '../services/chatService';
import type {
  ChatMessage,
  SendMessageRequest,
  UpdateMessageRequest,
  ChatTypingIndicator,
  ChatHistory,
  ChatSearchParams,
} from '@/contracts/chat';
import type { PaginatedResponse } from '@/contracts/common';

// Query keys for cache management
const QUERY_KEYS = {
  all: ['chat'] as const,
  messages: (hiveId: number) => [...QUERY_KEYS.all, 'messages', hiveId] as const,
  history: (hiveId: number) => [...QUERY_KEYS.all, 'history', hiveId] as const,
  search: (params: ChatSearchParams) => [...QUERY_KEYS.all, 'search', params] as const,
  receipts: (messageId: number) => [...QUERY_KEYS.all, 'receipts', messageId] as const,
};

/**
 * Hook to manage chat messages in a hive
 */
export function useHiveChat(hiveId: number | undefined) {
  const queryClient = useQueryClient();
  const [typingUsers, setTypingUsers] = useState<Map<number, ChatTypingIndicator>>(new Map());
  const [optimisticMessages, setOptimisticMessages] = useState<ChatMessage[]>([]);
  const unsubscribeRefs = useRef<(() => void)[]>([]);
  const typingTimeoutRef = useRef<NodeJS.Timeout>();

  // Fetch message history
  const historyQuery = useQuery({
    queryKey: QUERY_KEYS.history(hiveId!),
    queryFn: () => chatService.getMessageHistory(hiveId!),
    enabled: !!hiveId,
    staleTime: 1000 * 60 * 2, // 2 minutes
    gcTime: 1000 * 60 * 5,
  });

  // Send message mutation
  const sendMessageMutation = useMutation({
    mutationFn: (request: Omit<SendMessageRequest, 'hiveId'>) =>
      chatService.sendMessage({ ...request, hiveId: hiveId! }),
    onMutate: async (request) => {
      // Optimistic update
      const tempMessage: ChatMessage = {
        id: -Date.now(),
        hiveId: hiveId!,
        userId: -1,
        username: 'You',
        content: request.content,
        type: request.type || 'TEXT',
        status: 'SENDING',
        reactions: [],
        attachments: [],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      setOptimisticMessages(prev => [...prev, tempMessage]);
      return { tempMessage };
    },
    onSuccess: (data, variables, context) => {
      // Remove optimistic message and add real one
      setOptimisticMessages(prev => prev.filter(m => m.id !== context?.tempMessage.id));

      // Update cache
      queryClient.setQueryData<ChatHistory>(
        QUERY_KEYS.history(hiveId!),
        (old) => {
          if (!old) return old;
          return {
            ...old,
            messages: [...old.messages, data],
          };
        }
      );
    },
    onError: (error, variables, context) => {
      // Mark optimistic message as failed
      setOptimisticMessages(prev =>
        prev.map(m =>
          m.id === context?.tempMessage.id
            ? { ...m, status: 'FAILED' as const }
            : m
        )
      );
    },
  });

  // Update message mutation
  const updateMessageMutation = useMutation({
    mutationFn: ({ messageId, ...request }: UpdateMessageRequest & { messageId: number }) =>
      chatService.updateMessage(messageId, request),
    onSuccess: (data) => {
      // Update in cache
      queryClient.setQueryData<ChatHistory>(
        QUERY_KEYS.history(hiveId!),
        (old) => {
          if (!old) return old;
          return {
            ...old,
            messages: old.messages.map(m => m.id === data.id ? data : m),
          };
        }
      );
    },
  });

  // Delete message mutation
  const deleteMessageMutation = useMutation({
    mutationFn: ({ messageId, soft = true }: { messageId: number; soft?: boolean }) =>
      chatService.deleteMessage(messageId, { soft }),
    onSuccess: (data, variables) => {
      if (variables.soft && data) {
        // Update message in cache (soft delete)
        queryClient.setQueryData<ChatHistory>(
          QUERY_KEYS.history(hiveId!),
          (old) => {
            if (!old) return old;
            return {
              ...old,
              messages: old.messages.map(m => m.id === variables.messageId ? data : m),
            };
          }
        );
      } else {
        // Remove from cache (hard delete)
        queryClient.setQueryData<ChatHistory>(
          QUERY_KEYS.history(hiveId!),
          (old) => {
            if (!old) return old;
            return {
              ...old,
              messages: old.messages.filter(m => m.id !== variables.messageId),
            };
          }
        );
      }
    },
  });

  // Add reaction mutation
  const addReactionMutation = useMutation({
    mutationFn: ({ messageId, emoji }: { messageId: number; emoji: string }) =>
      chatService.addReaction(messageId, emoji),
    onSuccess: (_, variables) => {
      // Optimistically update the message in cache
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.history(hiveId!) });
    },
  });

  // Remove reaction mutation
  const removeReactionMutation = useMutation({
    mutationFn: ({ messageId, emoji }: { messageId: number; emoji: string }) =>
      chatService.removeReaction(messageId, emoji),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.history(hiveId!) });
    },
  });

  // Send typing indicator
  const sendTypingIndicator = useCallback((isTyping: boolean) => {
    if (!hiveId) return;

    chatService.sendTypingIndicator(hiveId, isTyping);

    // Clear existing timeout
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    // Auto-stop typing after 3 seconds
    if (isTyping) {
      typingTimeoutRef.current = setTimeout(() => {
        chatService.sendTypingIndicator(hiveId, false);
      }, 3000);
    }
  }, [hiveId]);

  // Load more messages (pagination)
  const loadMoreMessages = useCallback(async (beforeMessageId: number) => {
    if (!hiveId) return;

    const moreHistory = await chatService.getMessageHistory(hiveId, {
      beforeMessageId,
      limit: 50,
    });

    // Merge with existing history
    queryClient.setQueryData<ChatHistory>(
      QUERY_KEYS.history(hiveId),
      (old) => {
        if (!old) return moreHistory;
        return {
          ...old,
          messages: [...moreHistory.messages, ...old.messages],
          hasMore: moreHistory.hasMore,
          oldestMessageId: moreHistory.oldestMessageId,
        };
      }
    );

    return moreHistory;
  }, [hiveId, queryClient]);

  // Subscribe to real-time updates
  useEffect(() => {
    if (!hiveId) return;

    // Clear previous subscriptions
    unsubscribeRefs.current.forEach(unsub => unsub());
    unsubscribeRefs.current = [];

    // Subscribe to new messages
    const unsubMessages = chatService.subscribeToMessages(hiveId, (message) => {
      queryClient.setQueryData<ChatHistory>(
        QUERY_KEYS.history(hiveId),
        (old) => {
          if (!old) return old;
          // Check if message already exists
          if (old.messages.some(m => m.id === message.id)) {
            return old;
          }
          return {
            ...old,
            messages: [...old.messages, message],
          };
        }
      );
    });

    // Subscribe to message edits
    const unsubEdits = chatService.subscribeToMessageEdits(hiveId, (message) => {
      queryClient.setQueryData<ChatHistory>(
        QUERY_KEYS.history(hiveId),
        (old) => {
          if (!old) return old;
          return {
            ...old,
            messages: old.messages.map(m => m.id === message.id ? message : m),
          };
        }
      );
    });

    // Subscribe to message deletions
    const unsubDeletions = chatService.subscribeToMessageDeletions(hiveId, ({ messageId }) => {
      queryClient.setQueryData<ChatHistory>(
        QUERY_KEYS.history(hiveId),
        (old) => {
          if (!old) return old;
          return {
            ...old,
            messages: old.messages.filter(m => m.id !== messageId),
          };
        }
      );
    });

    // Subscribe to typing indicators
    const unsubTyping = chatService.subscribeToTypingIndicators(hiveId, (indicator) => {
      setTypingUsers(prev => {
        const updated = new Map(prev);
        if (indicator.isTyping) {
          updated.set(indicator.userId, indicator);
          // Auto-remove after 5 seconds
          setTimeout(() => {
            setTypingUsers(p => {
              const u = new Map(p);
              u.delete(indicator.userId);
              return u;
            });
          }, 5000);
        } else {
          updated.delete(indicator.userId);
        }
        return updated;
      });
    });

    unsubscribeRefs.current.push(unsubMessages, unsubEdits, unsubDeletions, unsubTyping);

    return () => {
      unsubscribeRefs.current.forEach(unsub => unsub());
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
    };
  }, [hiveId, queryClient]);

  // Combine real messages with optimistic ones
  const allMessages = [
    ...(historyQuery.data?.messages || []),
    ...optimisticMessages,
  ].sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());

  return {
    messages: allMessages,
    history: historyQuery.data,
    isLoading: historyQuery.isLoading,
    error: historyQuery.error,
    hasMore: historyQuery.data?.hasMore || false,
    typingUsers: Array.from(typingUsers.values()),

    // Actions
    sendMessage: sendMessageMutation.mutate,
    updateMessage: updateMessageMutation.mutate,
    deleteMessage: deleteMessageMutation.mutate,
    addReaction: addReactionMutation.mutate,
    removeReaction: removeReactionMutation.mutate,
    sendTypingIndicator,
    loadMoreMessages,

    // Mutation states
    isSending: sendMessageMutation.isPending,
    isUpdating: updateMessageMutation.isPending,
    isDeleting: deleteMessageMutation.isPending,
  };
}

/**
 * Hook to search chat messages
 */
export function useChatSearch(params: ChatSearchParams) {
  const searchQuery = useQuery({
    queryKey: QUERY_KEYS.search(params),
    queryFn: () => chatService.searchMessages(params),
    enabled: !!params.query,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });

  return {
    results: searchQuery.data?.content || [],
    totalResults: searchQuery.data?.totalElements || 0,
    isSearching: searchQuery.isLoading,
    error: searchQuery.error,
  };
}

/**
 * Hook to mark messages as read
 */
export function useMarkAsRead() {
  const queryClient = useQueryClient();

  const markAsReadMutation = useMutation({
    mutationFn: (messageIds: number[]) => chatService.markAsRead(messageIds),
    onSuccess: () => {
      // Invalidate relevant queries
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.all });
    },
  });

  return {
    markAsRead: markAsReadMutation.mutate,
    isMarking: markAsReadMutation.isPending,
  };
}

/**
 * Hook to get read receipts for a message
 */
export function useReadReceipts(messageId: number | undefined) {
  const receiptsQuery = useQuery({
    queryKey: QUERY_KEYS.receipts(messageId!),
    queryFn: () => chatService.getReadReceipts(messageId!),
    enabled: !!messageId,
    staleTime: 1000 * 60 * 2, // 2 minutes
  });

  return {
    receipts: receiptsQuery.data || [],
    isLoading: receiptsQuery.isLoading,
    error: receiptsQuery.error,
  };
}

/**
 * Hook to manage optimistic messages (for advanced usage)
 */
export function useOptimisticMessages(hiveId: number) {
  const [optimisticMessages, setOptimisticMessages] = useState<Map<string, ChatMessage>>(new Map());
  const queryClient = useQueryClient();

  const sendOptimistic = useCallback(async (request: Omit<SendMessageRequest, 'hiveId'>) => {
    const optimisticId = await chatService.sendMessageOptimistic({ ...request, hiveId });
    const optimisticMessage = chatService.getOptimisticMessage(optimisticId);

    if (optimisticMessage) {
      setOptimisticMessages(prev => new Map(prev).set(optimisticId, optimisticMessage));

      // Remove after successful send (timeout as fallback)
      setTimeout(() => {
        setOptimisticMessages(prev => {
          const updated = new Map(prev);
          updated.delete(optimisticId);
          return updated;
        });
      }, 10000);
    }

    return optimisticId;
  }, [hiveId]);

  const retryOptimistic = useCallback(async (optimisticId: string) => {
    const message = optimisticMessages.get(optimisticId);
    if (message) {
      // Retry sending
      const newId = await chatService.sendMessageOptimistic({
        hiveId: message.hiveId,
        content: message.content,
        type: message.type,
      });

      // Remove old, add new
      setOptimisticMessages(prev => {
        const updated = new Map(prev);
        updated.delete(optimisticId);
        const newMessage = chatService.getOptimisticMessage(newId);
        if (newMessage) {
          updated.set(newId, newMessage);
        }
        return updated;
      });
    }
  }, [optimisticMessages]);

  return {
    optimisticMessages: Array.from(optimisticMessages.values()),
    sendOptimistic,
    retryOptimistic,
  };
}