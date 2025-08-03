import React, { useEffect, useRef, useState, useCallback, useMemo } from 'react'
import {
  Box,
  CircularProgress,
  Typography,
  Button,
  Divider,
  styled,
  alpha,
  useTheme,
} from '@mui/material'
import { ExpandLess as ExpandLessIcon } from '@mui/icons-material'
import MessageBubble from './MessageBubble'
import { ChatMessage } from '../../../shared/types/chat'

const MessageListContainer = styled(Box)(({ theme }) => ({
  display: 'flex',
  flexDirection: 'column',
  height: '100%',
  overflow: 'hidden',
  position: 'relative',
}))

const ScrollContainer = styled(Box)(({ theme }) => ({
  flex: 1,
  overflowY: 'auto',
  overflowX: 'hidden',
  scrollBehavior: 'smooth',
  padding: theme.spacing(1),
  '&::-webkit-scrollbar': {
    width: 6,
  },
  '&::-webkit-scrollbar-track': {
    background: alpha(theme.palette.action.hover, 0.1),
    borderRadius: 3,
  },
  '&::-webkit-scrollbar-thumb': {
    background: alpha(theme.palette.action.active, 0.3),
    borderRadius: 3,
    '&:hover': {
      background: alpha(theme.palette.action.active, 0.5),
    },
  },
}))

const LoadMoreButton = styled(Button)(({ theme }) => ({
  margin: theme.spacing(1),
  borderRadius: theme.shape.borderRadius * 2,
}))

const DateDivider = styled(Box)(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  margin: theme.spacing(2, 0),
  '&::before, &::after': {
    content: '""',
    flex: 1,
    height: 1,
    backgroundColor: theme.palette.divider,
  },
}))

const DateLabel = styled(Typography)(({ theme }) => ({
  padding: theme.spacing(0, 2),
  backgroundColor: theme.palette.background.paper,
  color: theme.palette.text.secondary,
  fontSize: '0.75rem',
  fontWeight: 500,
}))

const ScrollToBottomButton = styled(Button)(({ theme }) => ({
  position: 'absolute',
  bottom: theme.spacing(2),
  right: theme.spacing(2),
  minWidth: 'auto',
  width: 48,
  height: 48,
  borderRadius: '50%',
  backgroundColor: theme.palette.primary.main,
  color: theme.palette.primary.contrastText,
  boxShadow: theme.shadows[4],
  zIndex: 10,
  '&:hover': {
    backgroundColor: theme.palette.primary.dark,
    transform: 'scale(1.05)',
  },
}))

const TypingIndicatorContainer = styled(Box)(({ theme }) => ({
  padding: theme.spacing(1, 2),
  color: theme.palette.text.secondary,
  fontStyle: 'italic',
  fontSize: '0.875rem',
}))

interface MessageListProps {
  hiveId: string
  messages: ChatMessage[]
  onLoadMore?: () => void
  hasMore?: boolean
  isLoading?: boolean
  currentUserId: string
  onEditMessage?: (messageId: string, content: string) => void
  onDeleteMessage?: (messageId: string) => void
  onReplyMessage?: (message: ChatMessage) => void
  onReaction?: (messageId: string, emoji: string) => void
  onRemoveReaction?: (messageId: string, emoji: string) => void
  typingUsers?: Array<{ userId: string; user: { name: string } }>
  autoScroll?: boolean
  className?: string
}

const MessageList: React.FC<MessageListProps> = ({
  hiveId,
  messages,
  onLoadMore,
  hasMore = false,
  isLoading = false,
  currentUserId,
  onEditMessage,
  onDeleteMessage,
  onReplyMessage,
  onReaction,
  onRemoveReaction,
  typingUsers = [],
  autoScroll = true,
  className,
}) => {
  const theme = useTheme()
  const scrollContainerRef = useRef<HTMLDivElement>(null)
  const lastMessageRef = useRef<HTMLDivElement>(null)
  const [showScrollToBottom, setShowScrollToBottom] = useState(false)
  const [userScrolledUp, setUserScrolledUp] = useState(false)

  // Group messages by date for date dividers
  const groupedMessages = useMemo(() => {
    const grouped: Array<{ date: string; messages: ChatMessage[] }> = []
    
    messages.forEach((message) => {
      const messageDate = new Date(message.createdAt).toDateString()
      const lastGroup = grouped[grouped.length - 1]
      
      if (lastGroup && lastGroup.date === messageDate) {
        lastGroup.messages.push(message)
      } else {
        grouped.push({
          date: messageDate,
          messages: [message],
        })
      }
    })
    
    return grouped
  }, [messages])

  // Create a map for quick message lookup (for replies)
  const messageMap = useMemo(() => {
    return messages.reduce((map, message) => {
      map[message.id] = message
      return map
    }, {} as Record<string, ChatMessage>)
  }, [messages])

  const formatDateLabel = (dateString: string) => {
    const date = new Date(dateString)
    const today = new Date()
    const yesterday = new Date(today)
    yesterday.setDate(yesterday.getDate() - 1)
    
    if (date.toDateString() === today.toDateString()) {
      return 'Today'
    } else if (date.toDateString() === yesterday.toDateString()) {
      return 'Yesterday'
    } else {
      return date.toLocaleDateString(undefined, { 
        weekday: 'long', 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric' 
      })
    }
  }

  const scrollToBottom = useCallback((smooth = true) => {
    if (lastMessageRef.current) {
      lastMessageRef.current.scrollIntoView({ 
        behavior: smooth ? 'smooth' : 'auto',
        block: 'end'
      })
    }
  }, [])

  const handleScroll = useCallback(() => {
    const container = scrollContainerRef.current
    if (!container) return

    const { scrollTop, scrollHeight, clientHeight } = container
    const isAtBottom = scrollHeight - scrollTop - clientHeight < 50
    const isNearTop = scrollTop < 100

    setShowScrollToBottom(!isAtBottom && messages.length > 0)
    setUserScrolledUp(!isAtBottom)

    // Load more messages when scrolled to top
    if (isNearTop && hasMore && !isLoading && onLoadMore) {
      const currentScrollHeight = scrollHeight
      onLoadMore()
      
      // Maintain scroll position after loading more messages
      setTimeout(() => {
        if (container) {
          const newScrollHeight = container.scrollHeight
          const scrollDiff = newScrollHeight - currentScrollHeight
          container.scrollTop = scrollTop + scrollDiff
        }
      }, 100)
    }
  }, [hasMore, isLoading, onLoadMore, messages.length])

  // Auto-scroll to bottom when new messages arrive (only if user hasn't scrolled up)
  useEffect(() => {
    if (autoScroll && !userScrolledUp && messages.length > 0) {
      setTimeout(() => {
        scrollToBottom(true)
      }, 100)
    }
  }, [messages.length, autoScroll, userScrolledUp, scrollToBottom])

  // Scroll to bottom on initial load
  useEffect(() => {
    if (messages.length > 0 && !userScrolledUp) {
      setTimeout(() => {
        scrollToBottom(false)
      }, 100)
    }
  }, [hiveId]) // Reset scroll when switching hives

  const shouldShowAvatar = useCallback((message: ChatMessage, index: number, groupMessages: ChatMessage[]) => {
    // Show avatar if it's the last message from this user in a sequence
    const nextMessage = groupMessages[index + 1]
    return !nextMessage || nextMessage.authorId !== message.authorId || nextMessage.type === 'system'
  }, [])

  const renderTypingIndicator = () => {
    if (typingUsers.length === 0) return null

    const typingNames = typingUsers.map(user => user.user.name)
    let typingText = ''

    if (typingNames.length === 1) {
      typingText = `${typingNames[0]} is typing...`
    } else if (typingNames.length === 2) {
      typingText = `${typingNames[0]} and ${typingNames[1]} are typing...`
    } else {
      typingText = `${typingNames.slice(0, -1).join(', ')} and ${typingNames[typingNames.length - 1]} are typing...`
    }

    return (
      <TypingIndicatorContainer>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Box sx={{ display: 'flex', gap: 0.5 }}>
            {[1, 2, 3].map((dot) => (
              <Box
                key={dot}
                sx={{
                  width: 4,
                  height: 4,
                  borderRadius: '50%',
                  backgroundColor: 'text.secondary',
                  animation: 'typingDot 1.4s infinite',
                  animationDelay: `${(dot - 1) * 0.2}s`,
                  '@keyframes typingDot': {
                    '0%, 60%, 100%': { opacity: 0.3 },
                    '30%': { opacity: 1 },
                  },
                }}
              />
            ))}
          </Box>
          <Typography variant="body2">{typingText}</Typography>
        </Box>
      </TypingIndicatorContainer>
    )
  }

  if (messages.length === 0 && !isLoading) {
    return (
      <MessageListContainer className={className}>
        <Box sx={{ 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center', 
          height: '100%',
          color: 'text.secondary'
        }}>
          <Typography variant="body1">
            No messages yet. Start the conversation!
          </Typography>
        </Box>
      </MessageListContainer>
    )
  }

  return (
    <MessageListContainer className={className}>
      <ScrollContainer ref={scrollContainerRef} onScroll={handleScroll}>
        {hasMore && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
            {isLoading ? (
              <CircularProgress size={24} />
            ) : (
              <LoadMoreButton 
                variant="outlined" 
                size="small"
                onClick={onLoadMore}
              >
                Load More Messages
              </LoadMoreButton>
            )}
          </Box>
        )}

        {groupedMessages.map((group, groupIndex) => (
          <Box key={group.date}>
            <DateDivider>
              <DateLabel>{formatDateLabel(group.date)}</DateLabel>
            </DateDivider>

            {group.messages.map((message, messageIndex) => (
              <MessageBubble
                key={message.id}
                message={message}
                isOwn={message.authorId === currentUserId}
                showAvatar={shouldShowAvatar(message, messageIndex, group.messages)}
                showTimestamp={true}
                onEdit={onEditMessage}
                onDelete={onDeleteMessage}
                onReply={onReplyMessage}
                onReaction={onReaction}
                onRemoveReaction={onRemoveReaction}
                currentUserId={currentUserId}
                replyToMessage={message.replyTo ? messageMap[message.replyTo] : null}
              />
            ))}
          </Box>
        ))}

        {renderTypingIndicator()}
        
        <div ref={lastMessageRef} />
      </ScrollContainer>

      {showScrollToBottom && (
        <ScrollToBottomButton
          onClick={() => scrollToBottom(true)}
        >
          <ExpandLessIcon sx={{ transform: 'rotate(180deg)' }} />
        </ScrollToBottomButton>
      )}
    </MessageListContainer>
  )
}

export default React.memo(MessageList)