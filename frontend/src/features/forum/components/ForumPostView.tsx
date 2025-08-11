import React, { useState, useEffect } from 'react'
import {
  Box,
  Container,
  Typography,
  Paper,
  Avatar,
  Button,
  Chip,
  Breadcrumbs,
  Link,
  IconButton,
  Menu,
  MenuItem,
  Divider,
  Alert,
  CircularProgress,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions
} from '@mui/material'
import {
  ThumbUp as LikeIcon,
  ThumbDown as DislikeIcon,
  Reply as ReplyIcon,
  Share as ShareIcon,
  Report as ReportIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  MoreVert as MoreVertIcon,
  PushPin as PinIcon,
  Lock as LockIcon,
  Visibility as ViewIcon,
  Home as HomeIcon,
  Category as CategoryIcon,
  Schedule as ScheduleIcon
} from '@mui/icons-material'
import { useParams, useNavigate, Link as RouterLink } from 'react-router-dom'
import { forumApi } from '../services/forumApi'
import { ForumPost, ForumReply } from '../types'
import ForumReplyThread from './ForumReplyThread'

const ForumPostView: React.FC = () => {
  const navigate = useNavigate()
  const { postSlug } = useParams<{ postSlug: string }>()
  
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [post, setPost] = useState<ForumPost | null>(null)
  const [replies, setReplies] = useState<ForumReply[]>([])
  const [repliesPage] = useState(1)
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null)
  const [replyDialogOpen, setReplyDialogOpen] = useState(false)
  const [replyContent, setReplyContent] = useState('')
  const [submittingReply, setSubmittingReply] = useState(false)
  const [userHasLiked, setUserHasLiked] = useState(false)
  const [userHasDisliked, setUserHasDisliked] = useState(false)

  useEffect(() => {
    const fetchData = async () => {
      if (postSlug) {
        try {
          setLoading(true)
          const postData = await forumApi.getPostBySlug(postSlug)
          setPost(postData)
          
          const repliesData = await forumApi.getPostReplies(postSlug, repliesPage)
          setReplies(repliesData)
        } catch (err) {
          const error = err as Error & { response?: { data?: { message?: string } } }
          setError(error.response?.data?.message || 'Failed to load post')
        } finally {
          setLoading(false)
        }
      }
    }
    fetchData()
  }, [postSlug, repliesPage])

  const handleLikePost = async () => {
    if (!post) return
    
    try {
      if (userHasLiked) {
        await forumApi.unlikePost(post.id)
        setUserHasLiked(false)
        setPost(prev => prev ? { ...prev, likeCount: prev.likeCount - 1 } : null)
      } else {
        await forumApi.likePost(post.id)
        setUserHasLiked(true)
        if (userHasDisliked) {
          setUserHasDisliked(false)
          setPost(prev => prev ? { ...prev, likeCount: prev.likeCount + 1, dislikeCount: prev.dislikeCount - 1 } : null)
        } else {
          setPost(prev => prev ? { ...prev, likeCount: prev.likeCount + 1 } : null)
        }
      }
    } catch (err) {
      console.error('Failed to like post:', err)
    }
  }

  const handleReplySubmit = async () => {
    if (!post || !replyContent.trim()) return
    
    setSubmittingReply(true)
    try {
      const newReply = await forumApi.createReply({
        content: replyContent.trim(),
        postId: post.id
      })
      
      setReplies(prev => [...prev, newReply])
      setReplyContent('')
      setReplyDialogOpen(false)
      setPost(prev => prev ? { ...prev, replyCount: prev.replyCount + 1 } : null)
    } catch (err) {
      const error = err as { response?: { data?: { message?: string } } }
      setError(error.response?.data?.message || 'Failed to submit reply')
    } finally {
      setSubmittingReply(false)
    }
  }

  const handleMenuClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setMenuAnchor(event.currentTarget)
  }

  const handleMenuClose = () => {
    setMenuAnchor(null)
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

  const getPostStatusIcons = () => {
    if (!post) return []
    
    const icons = []
    
    if (post.isPinned) {
      icons.push(
        <PinIcon key="pinned" color="warning" />
      )
    }
    
    if (post.isLocked) {
      icons.push(
        <LockIcon key="locked" color="error" />
      )
    }

    return icons
  }

  if (loading) {
    return (
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
          <CircularProgress />
        </Box>
      </Container>
    )
  }

  if (error || !post) {
    return (
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Alert severity="error">
          {error || 'Post not found'}
        </Alert>
      </Container>
    )
  }

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      {/* Breadcrumbs */}
      <Breadcrumbs sx={{ mb: 3 }}>
        <Link component={RouterLink} to="/forum" color="inherit">
          <Box display="flex" alignItems="center" gap={0.5}>
            <HomeIcon fontSize="small" />
            Forum
          </Box>
        </Link>
        <Link component={RouterLink} to="/forum/categories" color="inherit">
          <Box display="flex" alignItems="center" gap={0.5}>
            <CategoryIcon fontSize="small" />
            Categories
          </Box>
        </Link>
        {post.category && (
          <Link component={RouterLink} to={`/forum/categories/${post.category.slug}`} color="inherit">
            {post.category.name}
          </Link>
        )}
        <Typography color="textPrimary">
          {post.title}
        </Typography>
      </Breadcrumbs>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Post Content */}
      <Paper sx={{ mb: 4 }}>
        {/* Post Header */}
        <Box sx={{ p: 3, borderBottom: 1, borderColor: 'divider' }}>
          <Box display="flex" justifyContent="between" alignItems="flex-start" mb={2}>
            <Box flexGrow={1}>
              <Box display="flex" alignItems="center" gap={1} mb={1}>
                <Typography variant="h4" component="h1" sx={{ flexGrow: 1 }}>
                  {post.title}
                </Typography>
                <Box display="flex" gap={0.5}>
                  {getPostStatusIcons()}
                </Box>
              </Box>
              
              <Box display="flex" alignItems="center" gap={2} flexWrap="wrap" mb={2}>
                <Box display="flex" alignItems="center" gap={1}>
                  <Avatar src={post.author.avatar}>
                    {post.author.username[0].toUpperCase()}
                  </Avatar>
                  <Box>
                    <Typography variant="subtitle2">
                      {post.author.username}
                    </Typography>
                    <Typography variant="caption" color="textSecondary">
                      {post.author.role} • {post.author.postCount} posts
                    </Typography>
                  </Box>
                </Box>
                
                <Divider orientation="vertical" flexItem />
                
                <Box display="flex" alignItems="center" gap={0.5}>
                  <ScheduleIcon fontSize="small" color="action" />
                  <Typography variant="body2" color="textSecondary">
                    Posted {formatTimeAgo(post.createdAt)}
                  </Typography>
                </Box>
                
                <Box display="flex" alignItems="center" gap={0.5}>
                  <ViewIcon fontSize="small" color="action" />
                  <Typography variant="body2" color="textSecondary">
                    {formatNumber(post.viewCount)} views
                  </Typography>
                </Box>
              </Box>

              {/* Tags */}
              {post.tags && post.tags.length > 0 && (
                <Box display="flex" gap={0.5} flexWrap="wrap">
                  {post.tags.map((tag) => (
                    <Chip
                      key={tag}
                      label={tag}
                      size="small"
                      variant="outlined"
                      clickable
                      onClick={() => navigate(`/forum/search?tags=${encodeURIComponent(tag)}`)}
                    />
                  ))}
                </Box>
              )}
            </Box>
            
            <IconButton onClick={handleMenuClick}>
              <MoreVertIcon />
            </IconButton>
          </Box>
        </Box>

        {/* Post Body */}
        <Box sx={{ p: 3 }}>
          <Typography 
            variant="body1" 
            sx={{ 
              whiteSpace: 'pre-wrap',
              lineHeight: 1.6,
              '& p': { mb: 2 },
              '& img': { maxWidth: '100%', height: 'auto' }
            }}
            dangerouslySetInnerHTML={{ __html: post.content }}
          />
        </Box>

        {/* Post Actions */}
        <Box sx={{ p: 3, borderTop: 1, borderColor: 'divider' }}>
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Box display="flex" gap={1}>
              <Button
                startIcon={<LikeIcon />}
                variant={userHasLiked ? 'contained' : 'outlined'}
                size="small"
                onClick={handleLikePost}
              >
                {formatNumber(post.likeCount)}
              </Button>
              
              <Button
                startIcon={<DislikeIcon />}
                variant={userHasDisliked ? 'contained' : 'outlined'}
                size="small"
                color="error"
              >
                {formatNumber(post.dislikeCount)}
              </Button>
              
              <Button
                startIcon={<ReplyIcon />}
                variant="outlined"
                size="small"
                onClick={() => setReplyDialogOpen(true)}
                disabled={post.isLocked}
              >
                Reply
              </Button>
              
              <Button
                startIcon={<ShareIcon />}
                variant="outlined"
                size="small"
              >
                Share
              </Button>
            </Box>

            <Typography variant="body2" color="textSecondary">
              {formatNumber(post.replyCount)} replies
            </Typography>
          </Box>
        </Box>
      </Paper>

      {/* Replies Section */}
      <Paper>
        <Box sx={{ p: 3, borderBottom: 1, borderColor: 'divider' }}>
          <Typography variant="h6">
            Replies ({formatNumber(post.replyCount)})
          </Typography>
        </Box>

        {replies.length === 0 ? (
          <Box sx={{ p: 4, textAlign: 'center' }}>
            <Typography color="textSecondary" gutterBottom>
              No replies yet
            </Typography>
            <Typography variant="body2" color="textSecondary" paragraph>
              Be the first to join the discussion!
            </Typography>
            {!post.isLocked && (
              <Button
                variant="contained"
                startIcon={<ReplyIcon />}
                onClick={() => setReplyDialogOpen(true)}
              >
                Add Reply
              </Button>
            )}
          </Box>
        ) : (
          <ForumReplyThread 
            replies={replies}
            postId={post.id}
            onReplyUpdate={loadReplies}
          />
        )}
      </Paper>

      {/* Reply Dialog */}
      <Dialog open={replyDialogOpen} onClose={() => setReplyDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Reply to Post</DialogTitle>
        <DialogContent>
          <TextField
            multiline
            rows={6}
            fullWidth
            placeholder="Write your reply..."
            value={replyContent}
            onChange={(e) => setReplyContent(e.target.value)}
            sx={{ mt: 2 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReplyDialogOpen(false)}>
            Cancel
          </Button>
          <Button
            onClick={handleReplySubmit}
            variant="contained"
            disabled={!replyContent.trim() || submittingReply}
            startIcon={submittingReply ? <CircularProgress size={16} /> : <ReplyIcon />}
          >
            {submittingReply ? 'Posting...' : 'Post Reply'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Post Menu */}
      <Menu
        anchorEl={menuAnchor}
        open={Boolean(menuAnchor)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={handleMenuClose}>
          <ShareIcon sx={{ mr: 1 }} fontSize="small" />
          Share Post
        </MenuItem>
        <MenuItem onClick={handleMenuClose}>
          <ReportIcon sx={{ mr: 1 }} fontSize="small" />
          Report Post
        </MenuItem>
        <Divider />
        <MenuItem onClick={handleMenuClose}>
          <EditIcon sx={{ mr: 1 }} fontSize="small" />
          Edit Post
        </MenuItem>
        <MenuItem onClick={handleMenuClose} sx={{ color: 'error.main' }}>
          <DeleteIcon sx={{ mr: 1 }} fontSize="small" />
          Delete Post
        </MenuItem>
      </Menu>
    </Container>
  )
}

export default ForumPostView