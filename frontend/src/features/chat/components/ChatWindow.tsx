import React, {useCallback, useEffect, useState} from 'react'
import {
  Alert,
  alpha,
  Box,
  Chip,
  IconButton,
  Paper,
  Snackbar,
  styled,
  Typography,
} from '@mui/material'
import {
  Close as CloseIcon,
  CloseFullscreen as CloseFullscreenIcon,
  Minimize as MinimizeIcon,
  OpenInFull as OpenInFullIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material'
import MessageList from './MessageList'
import MessageInput from './MessageInput'
import TypingIndicator from './TypingIndicator'
import {useChat} from '../../../shared/contexts/ChatContext'
import {ConnectionState, useWebSocket} from '../../../shared/contexts/WebSocketContext'
import {ChatMessage} from '../../../shared/types/chat'

const ChatContainer = styled(Paper)<{ isFullscreen: boolean; isMinimized: boolean }>(
    ({theme, isFullscreen, isMinimized}) => ({
      display: 'flex',
      flexDirection: 'column',
      height: isFullscreen ? '100vh' : isMinimized ? 'auto' : '600px',
      width: isFullscreen ? '100vw' : '400px',
      maxWidth: isFullscreen ? '100vw' : '400px',
      position: isFullscreen ? 'fixed' : 'relative',
      top: isFullscreen ? 0 : 'auto',
      left: isFullscreen ? 0 : 'auto',
      zIndex: isFullscreen ? 1300 : 'auto',
      borderRadius: isFullscreen ? 0 : theme.shape.borderRadius * 2,
      overflow: 'hidden',
      border: `1px solid ${alpha(theme.palette.divider, 0.2)}`,
      background: `linear-gradient(135deg, 
      ${alpha(theme.palette.background.paper, 0.95)} 0%, 
      ${alpha(theme.palette.background.default, 0.98)} 100%)`,
      backdropFilter: 'blur(10px)',
      transition: 'all 0.3s ease-in-out',
    })
)

const ChatHeader = styled(Box)<{ isMinimized: boolean }>(({theme, isMinimized}) => ({
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  padding: theme.spacing(1, 2),
  backgroundColor: alpha(theme.palette.primary.main, 0.1),
  borderBottom: `1px solid ${alpha(theme.palette.divider, 0.1)}`,
  cursor: isMinimized ? 'pointer' : 'default',
  minHeight: 48,
}))

const HeaderControls = styled(Box)({
  display: 'flex',
  alignItems: 'center',
  gap: 4,
})

const ConnectionIndicator = styled(Chip)<{ connectionState: ConnectionState }>(
    ({theme, connectionState}) => {
      const getColor = (): string => {
        switch (connectionState) {
          case ConnectionState.CONNECTED:
            return theme.palette.success.main
          case ConnectionState.CONNECTING:
          case ConnectionState.RECONNECTING:
            return theme.palette.warning.main
          case ConnectionState.ERROR:
          case ConnectionState.DISCONNECTED:
            return theme.palette.error.main
          default:
            return theme.palette.grey[500]
        }
      }

      return {
        height: 20,
        fontSize: '0.7rem',
        backgroundColor: alpha(getColor(), 0.1),
        color: getColor(),
        border: `1px solid ${alpha(getColor(), 0.3)}`,
        '& .MuiChip-icon': {
          color: getColor(),
          fontSize: '0.8rem',
        },
      }
    }
)

interface ChatWindowProps {
  hiveId: string
  hiveName?: string
  currentUserId: string
  onClose?: () => void
  defaultMinimized?: boolean
  allowFullscreen?: boolean
  className?: string
}

const ChatWindow: React.FC<ChatWindowProps> = ({
                                                 hiveId,
                                                 hiveName = 'Chat',
                                                 currentUserId,
                                                 onClose,
                                                 defaultMinimized = false,
                                                 allowFullscreen = true,
                                                 className,
                                               }) => {
  const {connectionState, isConnected} = useWebSocket()
  const {
    chatState,
    sendMessage,
    editMessage,
    deleteMessage,
    addReaction,
    removeReaction,
    loadMoreMessages,
    setTyping,
    markMessagesAsRead,
  } = useChat()

  const [isMinimized, setIsMinimized] = useState(defaultMinimized)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [replyToMessage, setReplyToMessage] = useState<ChatMessage | null>(null)
  const [error, setError] = useState<string | null>(null)

  const messages = chatState.messages[hiveId] || []
  const typingUsers = chatState.typingUsers[hiveId] || []
  const hasMore = chatState.hasMoreMessages[hiveId] || false

  // Mark messages as read when window is opened and messages change
  useEffect(() => {
    if (!isMinimized && messages.length > 0) {
      markMessagesAsRead(hiveId)
    }
  }, [isMinimized, messages.length, hiveId, markMessagesAsRead])

  // Handle chat errors
  useEffect(() => {
    if (chatState.error) {
      setError(chatState.error)
    }
  }, [chatState.error])

  const handleSendMessage = useCallback(async (content: string) => {
    try {
      await sendMessage({
        hiveId,
        content,
        type: 'text',
        replyTo: replyToMessage?.id,
      })
      setReplyToMessage(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to send message')
    }
  }, [hiveId, sendMessage, replyToMessage])

  const handleEditMessage = useCallback(async (messageId: string, content: string) => {
    try {
      await editMessage(messageId, content)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to edit message')
    }
  }, [editMessage])

  const handleDeleteMessage = useCallback(async (messageId: string) => {
    try {
      await deleteMessage(messageId)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete message')
    }
  }, [deleteMessage])

  const handleReaction = useCallback(async (messageId: string, emoji: string) => {
    try {
      await addReaction(messageId, emoji)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add reaction')
    }
  }, [addReaction])

  const handleRemoveReaction = useCallback(async (messageId: string, emoji: string) => {
    try {
      await removeReaction(messageId, emoji)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to remove reaction')
    }
  }, [removeReaction])

  const handleReplyMessage = useCallback((message: ChatMessage) => {
    setReplyToMessage(message)
    setIsMinimized(false) // Ensure window is open when replying
  }, [])

  const handleTypingStart = useCallback(() => {
    setTyping(hiveId, true)
  }, [hiveId, setTyping])

  const handleTypingStop = useCallback(() => {
    setTyping(hiveId, false)
  }, [hiveId, setTyping])

  const getConnectionLabel = (): string => {
    switch (connectionState) {
      case ConnectionState.CONNECTED:
        return 'Connected'
      case ConnectionState.CONNECTING:
        return 'Connecting...'
      case ConnectionState.RECONNECTING:
        return 'Reconnecting...'
      case ConnectionState.ERROR:
        return 'Error'
      case ConnectionState.DISCONNECTED:
        return 'Disconnected'
      default:
        return 'Unknown'
    }
  }

  const handleHeaderClick = (): void => {
    if (isMinimized) {
      setIsMinimized(false)
    }
  }

  const toggleFullscreen = (): void => {
    setIsFullscreen(!isFullscreen)
    setIsMinimized(false)
  }

  const renderHeader = () => (
      <ChatHeader isMinimized={isMinimized} onClick={handleHeaderClick}>
        <Box sx={{display: 'flex', alignItems: 'center', gap: 1, minWidth: 0}}>
          <Typography variant="h6" sx={{fontSize: '1rem', fontWeight: 600}}>
            {hiveName}
          </Typography>
          <ConnectionIndicator
              connectionState={connectionState}
              label={getConnectionLabel()}
              size="small"
              variant="outlined"
          />
          {typingUsers.length > 0 && !isMinimized && (
              <TypingIndicator
                  typingUsers={typingUsers}
                  variant="compact"
                  showAvatars={false}
              />
          )}
        </Box>

        <HeaderControls>
          {!isConnected && (
              <IconButton
                  size="small"
                  onClick={(e) => {
                    e.stopPropagation()
                    window.location.reload()
                  }}
                  title="Reconnect"
              >
                <RefreshIcon fontSize="small"/>
              </IconButton>
          )}

          <IconButton
              size="small"
              onClick={(e) => {
                e.stopPropagation()
                setIsMinimized(!isMinimized)
              }}
              title={isMinimized ? "Expand" : "Minimize"}
          >
            <MinimizeIcon fontSize="small"/>
          </IconButton>

          {allowFullscreen && (
              <IconButton
                  size="small"
                  onClick={(e) => {
                    e.stopPropagation()
                    toggleFullscreen()
                  }}
                  title={isFullscreen ? "Exit Fullscreen" : "Fullscreen"}
              >
                {isFullscreen ? <CloseFullscreenIcon fontSize="small"/> :
                    <OpenInFullIcon fontSize="small"/>}
              </IconButton>
          )}

          {onClose && (
              <IconButton
                  size="small"
                  onClick={(e) => {
                    e.stopPropagation()
                    onClose()
                  }}
                  title="Close"
              >
                <CloseIcon fontSize="small"/>
              </IconButton>
          )}
        </HeaderControls>
      </ChatHeader>
  )

  return (
      <>
        <ChatContainer
            className={className}
            isFullscreen={isFullscreen}
            isMinimized={isMinimized}
            elevation={isFullscreen ? 0 : 6}
        >
          {renderHeader()}

          {!isMinimized && (
              <>
                <MessageList
                    hiveId={hiveId}
                    messages={messages}
                    onLoadMore={() => loadMoreMessages(hiveId)}
                    hasMore={hasMore}
                    isLoading={chatState.isLoading}
                    currentUserId={currentUserId}
                    onEditMessage={handleEditMessage}
                    onDeleteMessage={handleDeleteMessage}
                    onReplyMessage={handleReplyMessage}
                    onReaction={handleReaction}
                    onRemoveReaction={handleRemoveReaction}
                    typingUsers={typingUsers}
                />

                <MessageInput
                    hiveId={hiveId}
                    onSendMessage={handleSendMessage}
                    onTypingStart={handleTypingStart}
                    onTypingStop={handleTypingStop}
                    disabled={!isConnected}
                    replyTo={replyToMessage}
                    onCancelReply={() => setReplyToMessage(null)}
                />
              </>
          )}
        </ChatContainer>

        <Snackbar
            open={Boolean(error)}
            autoHideDuration={6000}
            onClose={() => setError(null)}
            anchorOrigin={{vertical: 'top', horizontal: 'center'}}
        >
          <Alert
              onClose={() => setError(null)}
              severity="error"
              variant="filled"
          >
            {error}
          </Alert>
        </Snackbar>
      </>
  )
}

export default React.memo(ChatWindow)