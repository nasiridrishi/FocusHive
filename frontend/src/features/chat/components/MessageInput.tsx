import React, { useState, useRef, useCallback, useEffect } from 'react'
import {
  Box,
  TextField,
  IconButton,
  Paper,
  Popover,
  Typography,
  styled,
  alpha,
  useTheme,
  InputAdornment,
} from '@mui/material'
import {
  Send as SendIcon,
  EmojiEmotions as EmojiIcon,
  AttachFile as AttachFileIcon,
  Close as CloseIcon,
  Reply as ReplyIcon,
} from '@mui/icons-material'
import { ChatMessage, MessageInputProps as BaseMessageInputProps } from '../../../shared/types/chat'

const MessageInputContainer = styled(Paper)(({ theme }) => ({
  padding: theme.spacing(1),
  borderRadius: theme.shape.borderRadius * 2,
  border: `1px solid ${alpha(theme.palette.divider, 0.2)}`,
  background: alpha(theme.palette.background.paper, 0.9),
  backdropFilter: 'blur(10px)',
}))

const EmojiGrid = styled(Box)(({ theme }) => ({
  display: 'grid',
  gridTemplateColumns: 'repeat(8, 1fr)',
  gap: theme.spacing(0.5),
  padding: theme.spacing(1),
  maxWidth: 300,
  maxHeight: 200,
  overflow: 'auto',
}))

const EmojiButton = styled(IconButton)(({ theme }) => ({
  width: 32,
  height: 32,
  fontSize: '1.2rem',
  '&:hover': {
    backgroundColor: alpha(theme.palette.primary.main, 0.1),
  },
}))

const ReplyBanner = styled(Box)(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  gap: theme.spacing(1),
  padding: theme.spacing(1, 1.5),
  backgroundColor: alpha(theme.palette.primary.main, 0.1),
  borderRadius: theme.shape.borderRadius,
  marginBottom: theme.spacing(1),
}))

const StyledTextField = styled(TextField)(({ theme }) => ({
  '& .MuiOutlinedInput-root': {
    borderRadius: theme.shape.borderRadius * 1.5,
    '& fieldset': {
      border: 'none',
    },
    '&:hover fieldset': {
      border: 'none',
    },
    '&.Mui-focused fieldset': {
      border: `2px solid ${theme.palette.primary.main}`,
    },
  },
}))

// Common emojis for quick access
const QUICK_EMOJIS = [
  '😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣',
  '😊', '😇', '🙂', '🙃', '😉', '😌', '😍', '🥰',
  '😘', '😗', '😙', '😚', '😋', '😛', '😝', '😜',
  '🤪', '🤨', '🧐', '🤓', '😎', '🤩', '🥳', '😏',
  '😒', '😞', '😔', '😟', '😕', '🙁', '☹️', '😣',
  '😖', '😫', '😩', '🥺', '😢', '😭', '😤', '😠',
  '😡', '🤬', '🤯', '😳', '🥵', '🥶', '😱', '😨',
  '😰', '😥', '😓', '🤗', '🤔', '🤭', '🤫', '🤥',
  '👍', '👎', '👌', '✌️', '🤞', '🤟', '🤘', '🤙',
  '💪', '🙏', '✨', '🎉', '🎊', '💯', '🔥', '⚡',
  '💖', '💝', '💗', '💓', '💕', '💘', '💞', '💯',
]

interface MessageInputProps extends Omit<BaseMessageInputProps, 'onSendMessage'> {
  onSendMessage: (content: string, type?: 'text' | 'emoji') => void
  onTypingStart?: () => void
  onTypingStop?: () => void
  replyTo?: ChatMessage | null
  onCancelReply?: () => void
  className?: string
}

const MessageInput: React.FC<MessageInputProps> = ({
  onSendMessage,
  onTypingStart,
  onTypingStop,
  placeholder = 'Type a message...',
  maxLength = 1000,
  disabled = false,
  showEmojiPicker = true,
  allowFileUpload = false,
  replyTo,
  onCancelReply,
  className,
}) => {
  const theme = useTheme()
  const [message, setMessage] = useState('')
  const [emojiAnchorEl, setEmojiAnchorEl] = useState<HTMLElement | null>(null)
  const [isTyping, setIsTyping] = useState(false)
  
  const inputRef = useRef<HTMLInputElement>(null)
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  // Handle typing indicators
  const handleTypingStart = useCallback(() => {
    if (!isTyping && onTypingStart) {
      setIsTyping(true)
      onTypingStart()
    }

    // Clear existing timeout
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current)
    }

    // Set new timeout to stop typing indicator
    typingTimeoutRef.current = setTimeout(() => {
      if (isTyping && onTypingStop) {
        setIsTyping(false)
        onTypingStop()
      }
    }, 2000)
  }, [isTyping, onTypingStart, onTypingStop])

  const handleTypingStop = useCallback(() => {
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current)
      typingTimeoutRef.current = null
    }
    
    if (isTyping && onTypingStop) {
      setIsTyping(false)
      onTypingStop()
    }
  }, [isTyping, onTypingStop])

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current)
      }
    }
  }, [])

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value
    
    if (value.length <= maxLength) {
      setMessage(value)
      
      if (value.trim()) {
        handleTypingStart()
      } else {
        handleTypingStop()
      }
    }
  }

  const handleKeyPress = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      handleSendMessage()
    }
  }

  const handleSendMessage = () => {
    const trimmedMessage = message.trim()
    
    if (!trimmedMessage || disabled) {
      return
    }

    // Stop typing indicator
    handleTypingStop()

    // Send message
    onSendMessage(trimmedMessage)
    
    // Clear input
    setMessage('')
    
    // Focus back to input
    setTimeout(() => {
      inputRef.current?.focus()
    }, 100)
  }

  const handleEmojiSelect = (emoji: string) => {
    const newMessage = message + emoji
    
    if (newMessage.length <= maxLength) {
      setMessage(newMessage)
      
      // Insert emoji at cursor position if possible
      if (inputRef.current) {
        const start = inputRef.current.selectionStart || 0
        const end = inputRef.current.selectionEnd || 0
        const newValue = message.substring(0, start) + emoji + message.substring(end)
        
        if (newValue.length <= maxLength) {
          setMessage(newValue)
          
          // Set cursor position after emoji
          setTimeout(() => {
            if (inputRef.current) {
              inputRef.current.setSelectionRange(start + emoji.length, start + emoji.length)
            }
          }, 0)
        }
      }
    }
    
    setEmojiAnchorEl(null)
    inputRef.current?.focus()
  }

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (file) {
      // Handle file upload logic here
      // You would typically upload the file and then send a message with the file URL
    }
    
    // Reset file input
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const renderReplyBanner = () => {
    if (!replyTo) return null

    return (
      <ReplyBanner>
        <ReplyIcon fontSize="small" color="primary" />
        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
          <Typography variant="caption" color="primary">
            Replying to {replyTo.author.name}
          </Typography>
          <Typography variant="body2" sx={{ 
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}>
            {replyTo.content}
          </Typography>
        </Box>
        <IconButton size="small" onClick={onCancelReply}>
          <CloseIcon fontSize="small" />
        </IconButton>
      </ReplyBanner>
    )
  }

  const renderEmojiPicker = () => (
    <Popover
      open={Boolean(emojiAnchorEl)}
      anchorEl={emojiAnchorEl}
      onClose={() => setEmojiAnchorEl(null)}
      anchorOrigin={{
        vertical: 'top',
        horizontal: 'center',
      }}
      transformOrigin={{
        vertical: 'bottom',
        horizontal: 'center',
      }}
    >
      <Box sx={{ p: 2 }}>
        <Typography variant="subtitle2" sx={{ mb: 1 }}>
          Quick Emojis
        </Typography>
        <EmojiGrid>
          {QUICK_EMOJIS.map((emoji, index) => (
            <EmojiButton
              key={index}
              onClick={() => handleEmojiSelect(emoji)}
            >
              {emoji}
            </EmojiButton>
          ))}
        </EmojiGrid>
      </Box>
    </Popover>
  )

  return (
    <Box className={className}>
      {renderReplyBanner()}
      
      <MessageInputContainer elevation={2}>
        <Box sx={{ display: 'flex', alignItems: 'flex-end', gap: 1 }}>
          <StyledTextField
            inputRef={inputRef}
            fullWidth
            multiline
            maxRows={4}
            value={message}
            onChange={handleInputChange}
            onKeyPress={handleKeyPress}
            placeholder={placeholder}
            disabled={disabled}
            InputProps={{
              startAdornment: allowFileUpload ? (
                <InputAdornment position="start">
                  <input
                    ref={fileInputRef}
                    type="file"
                    hidden
                    onChange={handleFileUpload}
                    accept="image/*,.pdf,.doc,.docx,.txt"
                  />
                  <IconButton
                    size="small"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={disabled}
                  >
                    <AttachFileIcon fontSize="small" />
                  </IconButton>
                </InputAdornment>
              ) : undefined,
              endAdornment: (
                <InputAdornment position="end">
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    {message.length > 0 && (
                      <Typography 
                        variant="caption" 
                        color={message.length > maxLength * 0.9 ? 'error' : 'text.secondary'}
                        sx={{ fontSize: '0.7rem', minWidth: '3ch' }}
                      >
                        {maxLength - message.length}
                      </Typography>
                    )}
                    
                    {showEmojiPicker && (
                      <IconButton
                        size="small"
                        onClick={(e) => setEmojiAnchorEl(e.currentTarget)}
                        disabled={disabled}
                      >
                        <EmojiIcon fontSize="small" />
                      </IconButton>
                    )}
                    
                    <IconButton
                      onClick={handleSendMessage}
                      disabled={disabled || !message.trim()}
                      color="primary"
                      sx={{
                        backgroundColor: message.trim() ? 'primary.main' : 'transparent',
                        color: message.trim() ? 'primary.contrastText' : 'text.secondary',
                        '&:hover': {
                          backgroundColor: message.trim() ? 'primary.dark' : alpha(theme.palette.action.hover, 0.1),
                        },
                        '&.Mui-disabled': {
                          backgroundColor: 'transparent',
                          color: 'text.disabled',
                        },
                      }}
                    >
                      <SendIcon fontSize="small" />
                    </IconButton>
                  </Box>
                </InputAdornment>
              ),
            }}
            sx={{
              '& .MuiInputBase-root': {
                backgroundColor: 'transparent',
              },
            }}
          />
        </Box>
      </MessageInputContainer>
      
      {renderEmojiPicker()}
    </Box>
  )
}

export default React.memo(MessageInput)