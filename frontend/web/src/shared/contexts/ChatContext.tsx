import React, { createContext, useContext, useEffect, useState, useCallback, useRef } from 'react'
import { useWebSocket } from './WebSocketContext'
import { 
  ChatContextType, 
  ChatState, 
  ChatMessage, 
  SendMessageRequest, 
  TypingIndicator,
  MessageReaction 
} from '../types/chat'

const ChatContext = createContext<ChatContextType | undefined>(undefined)

interface ChatProviderProps {
  children: React.ReactNode
  userId: string
}

export const ChatProvider: React.FC<ChatProviderProps> = ({
  children,
  userId,
}) => {
  const { isConnected, emit, on } = useWebSocket()
  
  const [chatState, setChatState] = useState<ChatState>({
    messages: {},
    typingUsers: {},
    isLoading: false,
    error: null,
    hasMoreMessages: {},
  })

  // Refs for debouncing and optimistic updates
  const typingTimeoutRef = useRef<Record<string, number>>({})
  const optimisticMessagesRef = useRef<Record<string, ChatMessage[]>>({})
  const messageQueueRef = useRef<SendMessageRequest[]>([])

  // Event handlers - define first to avoid dependency issues
  const handleMessageReceived = useCallback((message: ChatMessage) => {
    // Remove any optimistic message with the same content
    const optimisticMessages = optimisticMessagesRef.current[message.hiveId] || []
    const wasOptimistic = optimisticMessages.some(
      opt => opt.content === message.content && opt.authorId === message.authorId
    )

    setChatState(prev => {
      const existingMessages = prev.messages[message.hiveId] || []
      
      // If this was an optimistic message, replace it; otherwise, add it
      let updatedMessages
      if (wasOptimistic && message.authorId === userId) {
        // Replace optimistic message
        updatedMessages = existingMessages.map(msg => 
          msg.content === message.content && msg.authorId === userId && msg.id.startsWith('temp_')
            ? message
            : msg
        )
      } else {
        // Add new message if it doesn't already exist
        const messageExists = existingMessages.some(msg => msg.id === message.id)
        if (!messageExists) {
          updatedMessages = [...existingMessages, message]
        } else {
          updatedMessages = existingMessages
        }
      }

      return {
        ...prev,
        messages: {
          ...prev.messages,
          [message.hiveId]: updatedMessages,
        },
      }
    })

    // Clean up optimistic messages
    if (wasOptimistic) {
      optimisticMessagesRef.current[message.hiveId] = optimisticMessages.filter(
        opt => !(opt.content === message.content && opt.authorId === message.authorId)
      )
    }
  }, [userId])

  const handleMessageUpdated = useCallback((message: ChatMessage) => {
    setChatState(prev => ({
      ...prev,
      messages: {
        ...prev.messages,
        [message.hiveId]: (prev.messages[message.hiveId] || []).map(msg =>
          msg.id === message.id ? message : msg
        ),
      },
    }))
  }, [])

  const handleMessageDeleted = useCallback((data: { messageId: string, hiveId: string }) => {
    setChatState(prev => ({
      ...prev,
      messages: {
        ...prev.messages,
        [data.hiveId]: (prev.messages[data.hiveId] || []).filter(msg => msg.id !== data.messageId),
      },
    }))
  }, [])

  const handleReactionAdded = useCallback((reaction: MessageReaction & { hiveId: string }) => {
    setChatState(prev => ({
      ...prev,
      messages: {
        ...prev.messages,
        [reaction.hiveId]: (prev.messages[reaction.hiveId] || []).map(msg =>
          msg.id === reaction.messageId
            ? {
                ...msg,
                reactions: [...msg.reactions.filter(r => 
                  !(r.userId === reaction.userId && r.emoji === reaction.emoji)
                ), reaction],
              }
            : msg
        ),
      },
    }))
  }, [])

  const handleReactionRemoved = useCallback((data: { 
    messageId: string, 
    userId: string, 
    emoji: string,
    hiveId: string 
  }) => {
    setChatState(prev => ({
      ...prev,
      messages: {
        ...prev.messages,
        [data.hiveId]: (prev.messages[data.hiveId] || []).map(msg =>
          msg.id === data.messageId
            ? {
                ...msg,
                reactions: msg.reactions.filter(r => 
                  !(r.userId === data.userId && r.emoji === data.emoji)
                ),
              }
            : msg
        ),
      },
    }))
  }, [])

  const handleTypingStart = useCallback((data: TypingIndicator) => {
    setChatState(prev => {
      const existingTyping = prev.typingUsers[data.hiveId] || []
      const alreadyTyping = existingTyping.some(t => t.userId === data.userId)
      
      if (alreadyTyping) return prev

      return {
        ...prev,
        typingUsers: {
          ...prev.typingUsers,
          [data.hiveId]: [...existingTyping, { ...data, isTyping: true }],
        },
      }
    })
  }, [])

  const handleTypingStop = useCallback((data: { userId: string, hiveId: string }) => {
    setChatState(prev => ({
      ...prev,
      typingUsers: {
        ...prev.typingUsers,
        [data.hiveId]: (prev.typingUsers[data.hiveId] || []).filter(t => t.userId !== data.userId),
      },
    }))
  }, [])

  const handleChatHistory = useCallback((data: { 
    hiveId: string, 
    messages: ChatMessage[], 
    hasMore: boolean 
  }) => {
    setChatState(prev => ({
      ...prev,
      messages: {
        ...prev.messages,
        [data.hiveId]: [
          ...data.messages,
          ...(prev.messages[data.hiveId] || []),
        ],
      },
      hasMoreMessages: {
        ...prev.hasMoreMessages,
        [data.hiveId]: data.hasMore,
      },
      isLoading: false,
    }))
  }, [])

  const handleChatError = useCallback((error: { message: string }) => {
    setChatState(prev => ({
      ...prev,
      error: error.message,
      isLoading: false,
    }))
  }, [])

  // Set up WebSocket event listeners
  useEffect(() => {
    if (!isConnected) return

    const unsubscribeFunctions = [
      on('chat:message', (data: unknown) => handleMessageReceived(data as ChatMessage)),
      on('chat:message_updated', (data: unknown) => handleMessageUpdated(data as ChatMessage)),
      on('chat:message_deleted', (data: unknown) => handleMessageDeleted(data as { messageId: string; hiveId: string })),
      on('chat:reaction_added', (data: unknown) => handleReactionAdded(data as MessageReaction & { hiveId: string })),
      on('chat:reaction_removed', (data: unknown) => handleReactionRemoved(data as { messageId: string; userId: string; emoji: string; hiveId: string })),
      on('chat:typing_start', (data: unknown) => handleTypingStart(data as TypingIndicator)),
      on('chat:typing_stop', (data: unknown) => handleTypingStop(data as { userId: string; hiveId: string })),
      on('chat:history', (data: unknown) => handleChatHistory(data as { hiveId: string; messages: ChatMessage[]; hasMore: boolean })),
      on('chat:error', (data: unknown) => handleChatError(data as { message: string })),
    ]

    // Process queued messages when reconnected
    if (messageQueueRef.current.length > 0) {
      messageQueueRef.current.forEach(message => {
        emit('chat:send_message', message)
      })
      messageQueueRef.current = []
    }

    return () => {
      unsubscribeFunctions.forEach(unsubscribe => unsubscribe())
    }
  }, [isConnected, on, emit, handleChatError, handleChatHistory, handleMessageDeleted, handleMessageReceived, handleMessageUpdated, handleReactionAdded, handleReactionRemoved, handleTypingStart, handleTypingStop])

  // Cleanup typing timeouts on unmount
  useEffect(() => {
    return () => {
      const currentTimeouts = typingTimeoutRef.current
      Object.values(currentTimeouts).forEach(timeout => {
        clearTimeout(timeout)
      })
    }
  }, [])

  const sendMessage = useCallback(async (request: SendMessageRequest) => {
    if (!userId) {
      throw new Error('User ID is required to send messages')
    }

    // Create optimistic message
    const optimisticMessage: ChatMessage = {
      id: `temp_${Date.now()}_${Math.random()}`,
      content: request.content,
      authorId: userId,
      author: { id: userId, username: 'You', email: '', firstName: '', lastName: '', isEmailVerified: false, createdAt: '', updatedAt: '' }, // Will be populated by backend
      hiveId: request.hiveId,
      type: request.type || 'text',
      metadata: request.metadata,
      replyTo: request.replyTo,
      reactions: [],
      isEdited: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }

    // Add optimistic message to local state
    setChatState(prev => ({
      ...prev,
      messages: {
        ...prev.messages,
        [request.hiveId]: [
          ...(prev.messages[request.hiveId] || []),
          optimisticMessage,
        ],
      },
    }))

    // Store optimistic message for rollback if needed
    optimisticMessagesRef.current[request.hiveId] = [
      ...(optimisticMessagesRef.current[request.hiveId] || []),
      optimisticMessage,
    ]

    try {
      if (isConnected) {
        emit('chat:send_message', { ...request, userId })
      } else {
        // Queue message for when connection is restored
        messageQueueRef.current.push(request)
        throw new Error('Not connected. Message queued for retry.')
      }
    } catch (error) {
      // Remove optimistic message on error
      setChatState(prev => ({
        ...prev,
        messages: {
          ...prev.messages,
          [request.hiveId]: (prev.messages[request.hiveId] || [])
            .filter(msg => msg.id !== optimisticMessage.id),
        },
        error: error instanceof Error ? error.message : 'Failed to send message',
      }))
      throw error
    }
  }, [userId, isConnected, emit])

  const editMessage = useCallback(async (messageId: string, content: string) => {
    if (!isConnected) {
      throw new Error('Not connected')
    }

    try {
      emit('chat:edit_message', { messageId, content, userId })
    } catch (error) {
      setChatState(prev => ({
        ...prev,
        error: error instanceof Error ? error.message : 'Failed to edit message',
      }))
      throw error
    }
  }, [isConnected, emit, userId])

  const deleteMessage = useCallback(async (messageId: string) => {
    if (!isConnected) {
      throw new Error('Not connected')
    }

    try {
      emit('chat:delete_message', { messageId, userId })
    } catch (error) {
      setChatState(prev => ({
        ...prev,
        error: error instanceof Error ? error.message : 'Failed to delete message',
      }))
      throw error
    }
  }, [isConnected, emit, userId])

  const addReaction = useCallback(async (messageId: string, emoji: string) => {
    if (!isConnected) {
      throw new Error('Not connected')
    }

    try {
      emit('chat:add_reaction', { messageId, emoji, userId })
    } catch (error) {
      setChatState(prev => ({
        ...prev,
        error: error instanceof Error ? error.message : 'Failed to add reaction',
      }))
      throw error
    }
  }, [isConnected, emit, userId])

  const removeReaction = useCallback(async (messageId: string, emoji: string) => {
    if (!isConnected) {
      throw new Error('Not connected')
    }

    try {
      emit('chat:remove_reaction', { messageId, emoji, userId })
    } catch (error) {
      setChatState(prev => ({
        ...prev,
        error: error instanceof Error ? error.message : 'Failed to remove reaction',
      }))
      throw error
    }
  }, [isConnected, emit, userId])

  const loadMoreMessages = useCallback(async (hiveId: string) => {
    if (!isConnected || chatState.isLoading) {
      return
    }

    setChatState(prev => ({ ...prev, isLoading: true }))

    try {
      const existingMessages = chatState.messages[hiveId] || []
      const oldestMessage = existingMessages[0]
      
      emit('chat:load_history', {
        hiveId,
        before: oldestMessage?.createdAt,
        limit: 50,
      })
    } catch (error) {
      setChatState(prev => ({
        ...prev,
        isLoading: false,
        error: error instanceof Error ? error.message : 'Failed to load messages',
      }))
    }
  }, [isConnected, chatState.isLoading, chatState.messages, emit])

  const setTyping = useCallback((hiveId: string, isTyping: boolean) => {
    if (!isConnected || !userId) return

    // Clear existing timeout for this hive
    if (typingTimeoutRef.current[hiveId]) {
      clearTimeout(typingTimeoutRef.current[hiveId])
      delete typingTimeoutRef.current[hiveId]
    }

    if (isTyping) {
      emit('chat:typing_start', { hiveId, userId })
      
      // Auto-stop typing after 3 seconds
      typingTimeoutRef.current[hiveId] = setTimeout(() => {
        emit('chat:typing_stop', { hiveId, userId })
        delete typingTimeoutRef.current[hiveId]
      }, 3000)
    } else {
      emit('chat:typing_stop', { hiveId, userId })
    }
  }, [isConnected, userId, emit])

  const markMessagesAsRead = useCallback((hiveId: string) => {
    if (!isConnected || !userId) return

    emit('chat:mark_read', { hiveId, userId })
  }, [isConnected, userId, emit])


  const value: ChatContextType = {
    chatState,
    sendMessage,
    editMessage,
    deleteMessage,
    addReaction,
    removeReaction,
    loadMoreMessages,
    setTyping,
    markMessagesAsRead,
  }

  return (
    <ChatContext.Provider value={value}>
      {children}
    </ChatContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useChat = (): ChatContextType => {
  const context = useContext(ChatContext)
  if (context === undefined) {
    throw new Error('useChat must be used within a ChatProvider')
  }
  return context
}

export default ChatContext