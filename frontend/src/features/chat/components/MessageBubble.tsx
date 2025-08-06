import React, { useState } from 'react'
import {
  Box,
  Typography,
  Paper,
  IconButton,
  Tooltip,
  Menu,
  MenuItem,
  Chip,
  Link,
  styled,
  alpha,
  useTheme,
} from '@mui/material'
import {
  MoreVert as MoreVertIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Reply as ReplyIcon,
  EmojiEmotions as EmojiIcon,
  Check as CheckIcon,
} from '@mui/icons-material'
import { ChatMessage, MessageReaction } from '../../../shared/types/chat'
import { PresenceAvatar } from '../../presence/components/PresenceIndicator'

const MessageContainer = styled(Box)<{ isOwn: boolean }>(({ theme, isOwn }) => ({
  display: 'flex',
  flexDirection: isOwn ? 'row-reverse' : 'row',
  alignItems: 'flex-start',
  gap: theme.spacing(1),
  marginBottom: theme.spacing(1),
  padding: theme.spacing(0, 1),
}))

const MessageBubbleStyled = styled(Paper)<{ isOwn: boolean; isSystem?: boolean }>(
  ({ theme, isOwn, isSystem }) => ({
    maxWidth: '70%',
    minWidth: '60px',
    padding: theme.spacing(1, 1.5),
    borderRadius: isOwn 
      ? '18px 18px 4px 18px'
      : '18px 18px 18px 4px',
    backgroundColor: isSystem
      ? alpha(theme.palette.info.main, 0.1)
      : isOwn
      ? theme.palette.primary.main
      : theme.palette.background.paper,
    color: isSystem
      ? theme.palette.info.main
      : isOwn
      ? theme.palette.primary.contrastText
      : theme.palette.text.primary,
    border: `1px solid ${
      isSystem 
        ? alpha(theme.palette.info.main, 0.3)
        : isOwn 
        ? 'transparent'
        : alpha(theme.palette.divider, 0.2)
    }`,
    position: 'relative',
    transition: 'all 0.2s ease-in-out',
    '&:hover': {
      transform: 'translateY(-1px)',
      boxShadow: theme.shadows[2],
    },
  })
)

const MessageContent = styled(Box)({
  wordWrap: 'break-word',
  whiteSpace: 'pre-wrap',
})

const MessageTime = styled(Typography)<{ isOwn: boolean }>(({ theme, isOwn }) => ({
  fontSize: '0.7rem',
  color: isOwn ? alpha(theme.palette.primary.contrastText, 0.7) : theme.palette.text.secondary,
  marginTop: theme.spacing(0.5),
  textAlign: isOwn ? 'right' : 'left',
}))

const ReactionContainer = styled(Box)(({ theme }) => ({
  display: 'flex',
  flexWrap: 'wrap',
  gap: theme.spacing(0.5),
  marginTop: theme.spacing(0.5),
}))

const ReactionChip = styled(Chip)<{ isUserReaction: boolean }>(({ theme, isUserReaction }) => ({
  height: 24,
  fontSize: '0.7rem',
  cursor: 'pointer',
  backgroundColor: isUserReaction 
    ? alpha(theme.palette.primary.main, 0.2)
    : alpha(theme.palette.action.hover, 0.1),
  border: `1px solid ${isUserReaction 
    ? alpha(theme.palette.primary.main, 0.5)
    : alpha(theme.palette.divider, 0.2)}`,
  '&:hover': {
    backgroundColor: isUserReaction 
      ? alpha(theme.palette.primary.main, 0.3)
      : alpha(theme.palette.action.hover, 0.2),
  },
}))

const EditedIndicator = styled('span')(({ theme }) => ({
  fontSize: '0.65rem',
  fontStyle: 'italic',
  color: theme.palette.text.secondary,
  marginLeft: theme.spacing(0.5),
}))

const ReplyIndicator = styled(Box)(({ theme }) => ({
  borderLeft: `3px solid ${theme.palette.primary.main}`,
  paddingLeft: theme.spacing(1),
  marginBottom: theme.spacing(1),
  backgroundColor: alpha(theme.palette.action.hover, 0.05),
  borderRadius: theme.shape.borderRadius,
  padding: theme.spacing(0.5, 1),
}))

interface MessageBubbleProps {
  message: ChatMessage
  isOwn: boolean
  showAvatar?: boolean
  showTimestamp?: boolean
  canEdit?: boolean
  canDelete?: boolean
  onEdit?: (messageId: string, content: string) => void
  onDelete?: (messageId: string) => void
  onReply?: (message: ChatMessage) => void
  onReaction?: (messageId: string, emoji: string) => void
  onRemoveReaction?: (messageId: string, emoji: string) => void
  currentUserId: string
  replyToMessage?: ChatMessage | null
}

const MessageBubble: React.FC<MessageBubbleProps> = ({
  message,
  isOwn,
  showAvatar = true,
  showTimestamp = true,
  canEdit = true,
  canDelete = true,
  onEdit,
  onDelete,
  onReply,
  onReaction,
  onRemoveReaction,
  currentUserId,
  replyToMessage,
}) => {
  const theme = useTheme()
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
  const [isEditing, setIsEditing] = useState(false)
  const [editContent, setEditContent] = useState(message.content)

  const isSystem = message.type === 'system'
  const isTemp = message.id.startsWith('temp_')

  const formatTime = (timestamp: string) => {
    const date = new Date(timestamp)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffMinutes = Math.floor(diffMs / (1000 * 60))
    
    if (diffMinutes < 1) return 'now'
    if (diffMinutes < 60) return `${diffMinutes}m`
    
    const diffHours = Math.floor(diffMinutes / 60)
    if (diffHours < 24) return `${diffHours}h`
    
    // Same day
    if (date.toDateString() === now.toDateString()) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }
    
    // Different day
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' })
  }

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget)
  }

  const handleMenuClose = () => {
    setAnchorEl(null)
  }

  const handleEdit = () => {
    setIsEditing(true)
    handleMenuClose()
  }

  const handleSaveEdit = () => {
    if (editContent.trim() && editContent !== message.content && onEdit) {
      onEdit(message.id, editContent.trim())
    }
    setIsEditing(false)
    setEditContent(message.content)
  }

  const handleCancelEdit = () => {
    setIsEditing(false)
    setEditContent(message.content)
  }

  const handleDelete = () => {
    if (onDelete) {
      onDelete(message.id)
    }
    handleMenuClose()
  }

  const handleReply = () => {
    if (onReply) {
      onReply(message)
    }
    handleMenuClose()
  }

  const handleReactionClick = (emoji: string) => {
    const userReaction = message.reactions.find(
      r => r.userId === currentUserId && r.emoji === emoji
    )
    
    if (userReaction && onRemoveReaction) {
      onRemoveReaction(message.id, emoji)
    } else if (onReaction) {
      onReaction(message.id, emoji)
    }
  }

  const groupReactions = (reactions: MessageReaction[]) => {
    const grouped = reactions.reduce((acc, reaction) => {
      const key = reaction.emoji
      if (!acc[key]) {
        acc[key] = {
          emoji: reaction.emoji,
          count: 0,
          users: [],
          hasCurrentUser: false,
        }
      }
      acc[key].count++
      acc[key].users.push(reaction.user.name)
      if (reaction.userId === currentUserId) {
        acc[key].hasCurrentUser = true
      }
      return acc
    }, {} as Record<string, { emoji: string; count: number; users: string[]; hasCurrentUser: boolean }>)

    return Object.values(grouped)
  }

  const renderReplyTo = () => {
    if (!message.replyTo || !replyToMessage) return null

    return (
      <ReplyIndicator>
        <Typography variant="caption" color="text.secondary">
          Replying to {replyToMessage.author.name}
        </Typography>
        <Typography variant="body2" sx={{ mt: 0.5, opacity: 0.8 }}>
          {replyToMessage.content.length > 50 
            ? `${replyToMessage.content.substring(0, 50)}...`
            : replyToMessage.content
          }
        </Typography>
      </ReplyIndicator>
    )
  }

  const renderContent = () => {
    if (isEditing) {
      return (
        <Box>
          <textarea
            value={editContent}
            onChange={(e) => setEditContent(e.target.value)}
            style={{
              width: '100%',
              minHeight: '60px',
              border: 'none',
              outline: 'none',
              resize: 'none',
              background: 'transparent',
              color: 'inherit',
              fontFamily: 'inherit',
              fontSize: 'inherit',
            }}
            autoFocus
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault()
                handleSaveEdit()
              } else if (e.key === 'Escape') {
                handleCancelEdit()
              }
            }}
          />
          <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
            <IconButton size="small" onClick={handleSaveEdit} color="primary">
              <CheckIcon fontSize="small" />
            </IconButton>
            <IconButton size="small" onClick={handleCancelEdit}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Box>
        </Box>
      )
    }

    // Handle different message types
    switch (message.type) {
      case 'system':
        return (
          <Typography variant="body2" sx={{ fontStyle: 'italic' }}>
            {message.content}
          </Typography>
        )
      
      case 'image':
        return (
          <Box>
            {message.metadata?.imageUrl && (
              <img
                src={message.metadata.imageUrl}
                alt="Shared image"
                style={{
                  maxWidth: '100%',
                  maxHeight: '300px',
                  borderRadius: theme.shape.borderRadius,
                  marginBottom: message.content ? theme.spacing(1) : 0,
                }}
              />
            )}
            {message.content && (
              <Typography variant="body2">{message.content}</Typography>
            )}
          </Box>
        )
      
      case 'file':
        return (
          <Box>
            <Link
              href={message.metadata?.imageUrl}
              target="_blank"
              rel="noopener noreferrer"
              sx={{ display: 'block', mb: message.content ? 1 : 0 }}
            >
              📎 {message.metadata?.fileName || 'File'}
              {message.metadata?.fileSize && (
                <Typography variant="caption" sx={{ ml: 1 }}>
                  ({(message.metadata.fileSize / 1024).toFixed(1)} KB)
                </Typography>
              )}
            </Link>
            {message.content && (
              <Typography variant="body2">{message.content}</Typography>
            )}
          </Box>
        )
      
      default:
        return (
          <MessageContent>
            <Typography variant="body2">
              {message.content}
            </Typography>
          </MessageContent>
        )
    }
  }

  const renderReactions = () => {
    if (message.reactions.length === 0) return null

    const groupedReactions = groupReactions(message.reactions)

    return (
      <ReactionContainer>
        {groupedReactions.map((reaction) => (
          <Tooltip
            key={reaction.emoji}
            title={reaction.users.join(', ')}
            placement="top"
          >
            <ReactionChip
              label={`${reaction.emoji} ${reaction.count}`}
              size="small"
              isUserReaction={reaction.hasCurrentUser}
              onClick={() => handleReactionClick(reaction.emoji)}
            />
          </Tooltip>
        ))}
      </ReactionContainer>
    )
  }

  if (isSystem) {
    return (
      <Box sx={{ textAlign: 'center', my: 1 }}>
        <MessageBubbleStyled isOwn={false} isSystem elevation={0}>
          {renderContent()}
          {showTimestamp && (
            <MessageTime isOwn={false}>
              {formatTime(message.createdAt)}
            </MessageTime>
          )}
        </MessageBubbleStyled>
      </Box>
    )
  }

  return (
    <MessageContainer isOwn={isOwn}>
      {showAvatar && !isOwn && (
        <PresenceAvatar
          status="online" // You might want to get actual presence status
          src={message.author.avatar}
          name={message.author.name}
          size={32}
          showAnimation={false}
        />
      )}
      
      <Box sx={{ flexGrow: 1, minWidth: 0 }}>
        {!isOwn && (
          <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
            {message.author.name}
          </Typography>
        )}
        
        <Box sx={{ position: 'relative' }}>
          {renderReplyTo()}
          
          <MessageBubbleStyled 
            isOwn={isOwn}
            elevation={1}
            onMouseEnter={() => {
              // Show action buttons on hover
            }}
          >
            {renderContent()}
            
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                {showTimestamp && (
                  <MessageTime isOwn={isOwn}>
                    {formatTime(message.createdAt)}
                    {message.isEdited && (
                      <EditedIndicator>
                        (edited)
                      </EditedIndicator>
                    )}
                    {isTemp && (
                      <EditedIndicator>
                        (sending...)
                      </EditedIndicator>
                    )}
                  </MessageTime>
                )}
              </Box>
              
              {!isSystem && !isTemp && (
                <IconButton
                  size="small"
                  onClick={handleMenuOpen}
                  sx={{ 
                    opacity: 0.7,
                    '&:hover': { opacity: 1 },
                  }}
                >
                  <MoreVertIcon fontSize="small" />
                </IconButton>
              )}
            </Box>
          </MessageBubbleStyled>
          
          {renderReactions()}
        </Box>
      </Box>

      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
      >
        <MenuItem onClick={handleReply}>
          <ReplyIcon fontSize="small" sx={{ mr: 1 }} />
          Reply
        </MenuItem>
        <MenuItem onClick={() => handleReactionClick('👍')}>
          <EmojiIcon fontSize="small" sx={{ mr: 1 }} />
          React
        </MenuItem>
        {isOwn && canEdit && (
          <MenuItem onClick={handleEdit}>
            <EditIcon fontSize="small" sx={{ mr: 1 }} />
            Edit
          </MenuItem>
        )}
        {isOwn && canDelete && (
          <MenuItem onClick={handleDelete} sx={{ color: 'error.main' }}>
            <DeleteIcon fontSize="small" sx={{ mr: 1 }} />
            Delete
          </MenuItem>
        )}
      </Menu>
    </MessageContainer>
  )
}

export default React.memo(MessageBubble)