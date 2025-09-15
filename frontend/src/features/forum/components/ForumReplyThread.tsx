import React, {useState} from 'react'
import {
  Alert,
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Collapse,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  Menu,
  MenuItem,
  TextField,
  Typography
} from '@mui/material'
import {
  Delete as DeleteIcon,
  Edit as EditIcon,
  ExpandLess as ExpandLessIcon,
  ExpandMore as ExpandMoreIcon,
  MoreVert as MoreVertIcon,
  Reply as ReplyIcon,
  Report as ReportIcon,
  Schedule as ScheduleIcon,
  ThumbDown as DislikeIcon,
  ThumbUp as LikeIcon
} from '@mui/icons-material'
import {forumApi} from '../services/forumApi'
import {ForumCreateReplyRequest, ForumReply} from '../types'
import {sanitizeHtml, htmlToPlainText} from '@shared/utils/sanitizeHtml'

interface ForumReplyThreadProps {
  replies: ForumReply[]
  postId: number | string
  onReplyUpdate?: () => void
  maxDepth?: number
  currentDepth?: number
}

interface ReplyItemProps {
  reply: ForumReply
  postId: number | string
  onReplyUpdate?: () => void
  maxDepth: number
  currentDepth: number
}

const ReplyItem: React.FC<ReplyItemProps> = ({
                                               reply,
                                               postId,
                                               onReplyUpdate,
                                               maxDepth,
                                               currentDepth
                                             }) => {
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null)
  const [replyDialogOpen, setReplyDialogOpen] = useState(false)
  const [editDialogOpen, setEditDialogOpen] = useState(false)
  const [replyContent, setReplyContent] = useState('')
  const [editContent, setEditContent] = useState(reply.content)
  const [submitting, setSubmitting] = useState(false)
  const [childrenExpanded, setChildrenExpanded] = useState(true)
  const [userHasLiked, setUserHasLiked] = useState(false)
  const [userHasDisliked, setUserHasDisliked] = useState(false)
  const [localLikeCount, setLocalLikeCount] = useState(reply.likeCount)
  const [localDislikeCount, setLocalDislikeCount] = useState(reply.dislikeCount)

  const handleMenuClick = (event: React.MouseEvent<HTMLButtonElement>): void => {
    setMenuAnchor(event.currentTarget)
  }

  const handleMenuClose = (): void => {
    setMenuAnchor(null)
  }

  const handleLikeReply = async () => {
    try {
      if (userHasLiked) {
        await forumApi.unlikeReply(Number(reply.id))
        setUserHasLiked(false)
        setLocalLikeCount(prev => prev - 1)
      } else {
        await forumApi.likeReply(Number(reply.id))
        setUserHasLiked(true)
        if (userHasDisliked) {
          setUserHasDisliked(false)
          setLocalLikeCount(prev => prev + 1)
          setLocalDislikeCount(prev => prev - 1)
        } else {
          setLocalLikeCount(prev => prev + 1)
        }
      }
    } catch {
      // console.error('Error with reply reaction');
    }
  }

  const handleSubmitReply = async () => {
    if (!replyContent.trim()) return

    setSubmitting(true)
    try {
      const newReplyData: ForumCreateReplyRequest = {
        content: replyContent.trim(),
        postId: postId,
        parentReplyId: reply.id
      }

      await forumApi.createReply(newReplyData)
      setReplyContent('')
      setReplyDialogOpen(false)

      if (onReplyUpdate) {
        onReplyUpdate()
      }
    } catch {
      // console.error('Error submitting reply');
    } finally {
      setSubmitting(false)
    }
  }

  const handleEditReply = async () => {
    if (!editContent.trim()) return

    setSubmitting(true)
    try {
      await forumApi.updateReply(Number(reply.id), editContent.trim())
      setEditDialogOpen(false)

      if (onReplyUpdate) {
        onReplyUpdate()
      }
    } catch {
      // console.error('Error editing reply');
    } finally {
      setSubmitting(false)
    }
  }

  const formatTimeAgo = (dateString: string): string => {
    const date = new Date(dateString)
    const now = new Date()
    const diffInHours = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60))

    if (diffInHours < 1) return 'Just now'
    if (diffInHours < 24) return `${diffInHours}h ago`
    if (diffInHours < 168) return `${Math.floor(diffInHours / 24)}d ago`
    return date.toLocaleDateString()
  }

  const formatNumber = (num: number): string => {
    if (num >= 1000000) return `${(num / 1000000).toFixed(1)}M`
    if (num >= 1000) return `${(num / 1000).toFixed(1)}K`
    return num.toString()
  }

  const canReply = currentDepth < maxDepth
  const hasChildren = reply.childReplies && reply.childReplies.length > 0

  return (
      <Box sx={{mb: 2}}>
        <Card
            variant={currentDepth > 0 ? "outlined" : "elevation"}
            sx={{
              ml: currentDepth * 3,
              position: 'relative',
              '&::before': currentDepth > 0 ? {
                content: '""',
                position: 'absolute',
                left: -12,
                top: 0,
                bottom: 0,
                width: 2,
                backgroundColor: 'divider'
              } : {}
            }}
        >
          {reply.isHidden ? (
              <CardContent>
                <Alert severity="warning">
                  This reply has been hidden by moderators
                </Alert>
              </CardContent>
          ) : (
              <>
                <CardContent>
                  {/* Reply Header */}
                  <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={2}>
                    <Box display="flex" alignItems="center" gap={2}>
                      <Avatar src={reply.author.avatar} sx={{width: 32, height: 32}}>
                        {reply.author.username[0].toUpperCase()}
                      </Avatar>
                      <Box>
                        <Typography variant="subtitle2"
                                    sx={{display: 'flex', alignItems: 'center', gap: 1}}>
                          {reply.author.username}
                          {reply.author.role === 'MODERATOR' && (
                              <Chip size="small" label="MOD" color="primary"/>
                          )}
                          {reply.author.role === 'ADMIN' && (
                              <Chip size="small" label="ADMIN" color="error"/>
                          )}
                          {reply.isModeratorReply && (
                              <Chip size="small" label="Official" color="warning"/>
                          )}
                        </Typography>
                        <Box display="flex" alignItems="center" gap={1}>
                          <ScheduleIcon fontSize="small" color="action"/>
                          <Typography variant="caption" color="textSecondary">
                            {formatTimeAgo(reply.createdAt)}
                          </Typography>
                          {reply.editedAt && (
                              <Typography variant="caption" color="textSecondary">
                                (edited {formatTimeAgo(reply.editedAt)})
                              </Typography>
                          )}
                        </Box>
                      </Box>
                    </Box>

                    <IconButton size="small" onClick={handleMenuClick}>
                      <MoreVertIcon fontSize="small"/>
                    </IconButton>
                  </Box>

                  {/* Reply Content */}
                  <Typography
                      variant="body2"
                      sx={{
                        whiteSpace: 'pre-wrap',
                        lineHeight: 1.6,
                        mb: 2
                      }}
                      dangerouslySetInnerHTML={{__html: sanitizeHtml(reply.content)}}
                  />

                  {/* Reply Actions */}
                  <Box display="flex" alignItems="center" gap={1}>
                    <Button
                        size="small"
                        startIcon={<LikeIcon/>}
                        variant={userHasLiked ? 'contained' : 'text'}
                        onClick={handleLikeReply}
                    >
                      {formatNumber(localLikeCount)}
                    </Button>

                    <Button
                        size="small"
                        startIcon={<DislikeIcon/>}
                        variant={userHasDisliked ? 'contained' : 'text'}
                        color="error"
                    >
                      {formatNumber(localDislikeCount)}
                    </Button>

                    {canReply && (
                        <Button
                            size="small"
                            startIcon={<ReplyIcon/>}
                            variant="text"
                            onClick={() => setReplyDialogOpen(true)}
                        >
                          Reply
                        </Button>
                    )}

                    {hasChildren && (
                        <Button
                            size="small"
                            startIcon={childrenExpanded ? <ExpandLessIcon/> : <ExpandMoreIcon/>}
                            variant="text"
                            onClick={() => setChildrenExpanded(!childrenExpanded)}
                        >
                          {childrenExpanded ? 'Hide' : 'Show'} {reply.childReplies?.length} replies
                        </Button>
                    )}
                  </Box>
                </CardContent>
              </>
          )}
        </Card>

        {/* Child Replies */}
        {hasChildren && (
            <Collapse in={childrenExpanded}>
              <Box sx={{mt: 1}}>
                <ForumReplyThread
                    replies={reply.childReplies || []}
                    postId={postId}
                    onReplyUpdate={onReplyUpdate}
                    maxDepth={maxDepth}
                    currentDepth={currentDepth + 1}
                />
              </Box>
            </Collapse>
        )}

        {/* Context Menu */}
        <Menu
            anchorEl={menuAnchor}
            open={Boolean(menuAnchor)}
            onClose={handleMenuClose}
        >
          <MenuItem onClick={() => {
            setEditDialogOpen(true);
            handleMenuClose()
          }}>
            <EditIcon sx={{mr: 1}} fontSize="small"/>
            Edit Reply
          </MenuItem>
          <MenuItem onClick={handleMenuClose}>
            <ReportIcon sx={{mr: 1}} fontSize="small"/>
            Report Reply
          </MenuItem>
          <Divider/>
          <MenuItem onClick={handleMenuClose} sx={{color: 'error.main'}}>
            <DeleteIcon sx={{mr: 1}} fontSize="small"/>
            Delete Reply
          </MenuItem>
        </Menu>

        {/* Reply Dialog */}
        <Dialog open={replyDialogOpen} onClose={() => setReplyDialogOpen(false)} fullWidth>
          <DialogTitle>Reply to {reply.author.username}</DialogTitle>
          <DialogContent>
            <Box sx={{mb: 2, p: 2, backgroundColor: 'grey.50', borderRadius: 1}}>
              <Typography variant="caption" color="textSecondary" gutterBottom display="block">
                Replying to:
              </Typography>
              <Typography variant="body2" sx={{fontStyle: 'italic'}}>
                {(() => {
                  const plainText = htmlToPlainText(reply.content);
                  return plainText.length > 200
                    ? `${plainText.substring(0, 200)}...`
                    : plainText;
                })()
                }
              </Typography>
            </Box>
            <TextField
                multiline
                rows={4}
                fullWidth
                placeholder="Write your reply..."
                value={replyContent}
                onChange={(e) => setReplyContent(e.target.value)}
                sx={{mt: 2}}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setReplyDialogOpen(false)}>
              Cancel
            </Button>
            <Button
                onClick={handleSubmitReply}
                variant="contained"
                disabled={!replyContent.trim() || submitting}
                startIcon={submitting ? <CircularProgress size={16}/> : <ReplyIcon/>}
            >
              {submitting ? 'Posting...' : 'Post Reply'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Edit Dialog */}
        <Dialog open={editDialogOpen} onClose={() => setEditDialogOpen(false)} fullWidth>
          <DialogTitle>Edit Reply</DialogTitle>
          <DialogContent>
            <TextField
                multiline
                rows={6}
                fullWidth
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                sx={{mt: 2}}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setEditDialogOpen(false)}>
              Cancel
            </Button>
            <Button
                onClick={handleEditReply}
                variant="contained"
                disabled={!editContent.trim() || submitting}
                startIcon={submitting ? <CircularProgress size={16}/> : <EditIcon/>}
            >
              {submitting ? 'Saving...' : 'Save Changes'}
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
  )
}

const ForumReplyThread: React.FC<ForumReplyThreadProps> = ({
                                                             replies,
                                                             postId,
                                                             onReplyUpdate,
                                                             maxDepth = 5,
                                                             currentDepth = 0
                                                           }) => {
  if (!replies || replies.length === 0) {
    return null
  }

  return (
      <Box>
        {replies.map((reply) => (
            <ReplyItem
                key={reply.id}
                reply={reply}
                postId={postId}
                onReplyUpdate={onReplyUpdate}
                maxDepth={maxDepth}
                currentDepth={currentDepth}
            />
        ))}
      </Box>
  )
}

export default ForumReplyThread