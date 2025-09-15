import React from 'react'
import {
  Card,
  CardContent,
  CardActions,
  Avatar,
  IconButton,
  Chip,
  Typography,
  Box,
  Stack,
  Skeleton,
  Link,
  Tooltip,
  useTheme,
  alpha
} from '@mui/material'
import {
  ThumbUp,
  ThumbDown,
  Reply,
  Share,
  Edit,
  PushPin,
  Lock,
  CheckCircle,
  Visibility,
  Person
} from '@mui/icons-material'
import { formatDistanceToNow } from 'date-fns'
import { ForumPost as ForumPostType } from '../types'

interface ForumPostProps {
  post: ForumPostType
  currentUserId?: number
  userVote?: 'upvote' | 'downvote'
  isAnswered?: boolean
  isLoading?: boolean
  compact?: boolean
  onVote?: (postId: string | number, voteType: 'upvote' | 'downvote') => void
  onEdit?: (postId: string | number) => void
  onShare?: (postId: string | number) => void
  onView?: (postId: string | number) => void
}

const ForumPost: React.FC<ForumPostProps> = ({
  post,
  currentUserId,
  userVote,
  isAnswered = false,
  isLoading = false,
  compact = false,
  onVote,
  onEdit,
  onShare,
  onView
}) => {
  const theme = useTheme()

  // Loading skeleton
  if (isLoading) {
    return (
      <Card
        data-testid="forum-post-skeleton"
        sx={{ mb: 2, p: 2 }}
      >
        <Stack spacing={2}>
          <Stack direction="row" spacing={2} alignItems="center">
            <Skeleton variant="circular" width={40} height={40} />
            <Box flex={1}>
              <Skeleton variant="text" width="60%" height={24} />
              <Skeleton variant="text" width="40%" height={20} />
            </Box>
          </Stack>
          <Skeleton variant="text" width="80%" height={32} />
          <Skeleton variant="rectangular" height={80} />
          <Stack direction="row" spacing={1}>
            <Skeleton variant="rectangular" width={60} height={32} />
            <Skeleton variant="rectangular" width={60} height={32} />
            <Skeleton variant="rectangular" width={60} height={32} />
          </Stack>
        </Stack>
      </Card>
    )
  }

  // Calculate vote score
  const voteScore = Math.max(0, (post.likeCount || 0) - (post.dislikeCount || 0))

  // Check if post has been edited
  const isEdited = post.updatedAt !== post.createdAt

  // Check if current user is the author
  const isAuthor = currentUserId === post.authorId

  // Format timestamp
  const timeAgo = formatDistanceToNow(new Date(post.createdAt), { addSuffix: true })

  // Handle voting
  const handleVote = (voteType: 'upvote' | 'downvote') => {
    if (post.isLocked || !onVote) return
    onVote(post.id, voteType)
  }

  // Handle content click
  const handleContentClick = (e: React.MouseEvent) => {
    // Don't trigger if clicking on interactive elements
    const target = e.target as HTMLElement
    if (target.closest('button') || target.closest('a')) {
      return
    }
    onView?.(post.id)
  }

  // Handle title click
  const handleTitleClick = (e: React.MouseEvent) => {
    e.preventDefault()
    onView?.(post.id)
  }

  // Handle keyboard navigation
  const handleTitleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      onView?.(post.id)
    }
  }

  const handleVoteKeyDown = (e: React.KeyboardEvent, voteType: 'upvote' | 'downvote') => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      handleVote(voteType)
    }
  }

  return (
    <Card
      data-testid="forum-post"
      role="article"
      className={compact ? 'forum-post--compact' : ''}
      sx={{
        mb: 2,
        transition: 'all 0.2s ease-in-out',
        '&:hover': {
          boxShadow: theme.shadows[4],
          transform: 'translateY(-1px)'
        }
      }}
    >
      <CardContent sx={{ p: 3 }}>
        {/* Post States */}
        {(post.isPinned || post.isLocked || isAnswered) && (
          <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
            {post.isPinned && (
              <Chip
                data-testid="pinned-indicator"
                icon={<PushPin />}
                label="Pinned"
                size="small"
                color="primary"
                variant="outlined"
                aria-label="This post is pinned"
              />
            )}
            {post.isLocked && (
              <Chip
                data-testid="locked-indicator"
                icon={<Lock />}
                label="Locked"
                size="small"
                color="warning"
                variant="outlined"
                aria-label="This post is locked"
              />
            )}
            {isAnswered && (
              <Chip
                data-testid="answered-indicator"
                icon={<CheckCircle />}
                label="Answered"
                size="small"
                color="success"
                variant="outlined"
                aria-label="This post has been answered"
              />
            )}
          </Stack>
        )}

        {/* Author and metadata */}
        <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }}>
          {/* Author Avatar */}
          {post.author ? (
            post.author.avatar ? (
              <Avatar
                src={post.author.avatar}
                alt={`${post.author.username} avatar`}
                sx={{ width: 40, height: 40 }}
              />
            ) : (
              <Avatar
                data-testid="default-avatar"
                sx={{ width: 40, height: 40, bgcolor: theme.palette.primary.main }}
              >
                <Person />
              </Avatar>
            )
          ) : (
            <Avatar
              data-testid="default-avatar"
              sx={{ width: 40, height: 40, bgcolor: theme.palette.grey[400] }}
            >
              <Person />
            </Avatar>
          )}

          {/* Author Info */}
          <Box flex={1}>
            <Stack direction="row" alignItems="center" spacing={1}>
              <Typography
                data-testid="author-username"
                variant="subtitle2"
                fontWeight="bold"
              >
                {post.author?.username || (
                  <span data-testid="unknown-author">Unknown User</span>
                )}
              </Typography>

              {/* Category */}
              {post.category && (
                <Chip
                  data-testid="category-chip"
                  label={post.category.name}
                  size="small"
                  variant="outlined"
                  sx={{
                    bgcolor: alpha(post.category.color || theme.palette.primary.main, 0.1),
                    borderColor: post.category.color || theme.palette.primary.main,
                    color: post.category.color || theme.palette.primary.main
                  }}
                />
              )}
            </Stack>

            <Stack direction="row" alignItems="center" spacing={2}>
              <Typography
                data-testid="post-timestamp"
                variant="caption"
                color="text.secondary"
              >
                {timeAgo}
                {isEdited && (
                  <span data-testid="edit-indicator"> • edited</span>
                )}
              </Typography>

              {/* View count */}
              <Stack direction="row" alignItems="center" spacing={0.5}>
                <Visibility fontSize="small" color="disabled" />
                <Typography
                  data-testid="view-count"
                  variant="caption"
                  color="text.secondary"
                >
                  {post.viewCount}
                </Typography>
              </Stack>
            </Stack>

            {/* Author details (hidden in compact mode) */}
            {!compact && post.author && (
              <Stack direction="row" spacing={2} sx={{ mt: 0.5 }}>
                <Typography
                  data-testid="author-post-count"
                  variant="caption"
                  color="text.secondary"
                >
                  {post.author.postCount} posts
                </Typography>

                {/* Author badges */}
                {post.author.badges && post.author.badges.length > 0 && (
                  <Box data-testid="author-badges">
                    <Stack direction="row" spacing={0.5}>
                      {post.author.badges.map((badge, index) => (
                        <Chip
                          key={index}
                          label={badge}
                          size="small"
                          variant="outlined"
                          sx={{ fontSize: '0.625rem', height: 20 }}
                        />
                      ))}
                    </Stack>
                  </Box>
                )}
              </Stack>
            )}
          </Box>
        </Stack>

        {/* Post Title */}
        <Link
          href="#"
          variant="h6"
          onClick={handleTitleClick}
          onKeyDown={handleTitleKeyDown}
          sx={{
            display: 'block',
            textAlign: 'left',
            textDecoration: 'none',
            color: 'inherit',
            mb: 2,
            '&:hover': {
              color: theme.palette.primary.main,
              textDecoration: 'underline'
            },
            '&:focus': {
              outline: `2px solid ${theme.palette.primary.main}`,
              outlineOffset: 2
            }
          }}
          aria-label={`View post: ${post.title}`}
        >
          {post.title}
        </Link>

        {/* Post Content */}
        <Box
          data-testid="post-content"
          onClick={handleContentClick}
          sx={{
            cursor: 'pointer',
            mb: 2,
            '&:hover': {
              bgcolor: alpha(theme.palette.primary.main, 0.02)
            }
          }}
        >
          <Typography
            variant="body2"
            color="text.secondary"
            dangerouslySetInnerHTML={{
              __html: post.content.replace(/<[^>]*>/g, '').substring(0, 200) +
                     (post.content.length > 200 ? '...' : '')
            }}
          />
        </Box>

        {/* Tags */}
        {post.tags && post.tags.length > 0 && (
          <Box data-testid="post-tags" sx={{ mb: 2 }}>
            <Stack direction="row" spacing={1} flexWrap="wrap">
              {post.tags.map((tag, index) => (
                <Chip
                  key={index}
                  label={tag}
                  size="small"
                  variant="outlined"
                  sx={{ fontSize: '0.75rem' }}
                />
              ))}
            </Stack>
          </Box>
        )}

        {/* Stats and interactions */}
        <Stack direction="row" alignItems="center" spacing={2}>
          {/* Vote controls */}
          <Stack direction="row" alignItems="center" spacing={0.5}>
            <Tooltip title={post.isLocked ? "Voting disabled on locked posts" : "Upvote this post"}>
              <span>
                <IconButton
                  className={userVote === 'upvote' ? 'vote-button--active' : ''}
                  disabled={post.isLocked}
                  onClick={() => handleVote('upvote')}
                  onKeyDown={(e) => handleVoteKeyDown(e, 'upvote')}
                  aria-label="Upvote this post"
                  sx={{
                    color: userVote === 'upvote' ? theme.palette.primary.main : 'inherit',
                    '&.vote-button--active': {
                      bgcolor: alpha(theme.palette.primary.main, 0.1)
                    }
                  }}
                >
                  <ThumbUp fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>

            <Typography
              data-testid="vote-count"
              variant="body2"
              fontWeight="bold"
              aria-label={`Post score: ${voteScore} points`}
              sx={{ minWidth: 24, textAlign: 'center' }}
            >
              {voteScore}
            </Typography>

            <Tooltip title={post.isLocked ? "Voting disabled on locked posts" : "Downvote this post"}>
              <span>
                <IconButton
                  className={userVote === 'downvote' ? 'vote-button--active' : ''}
                  disabled={post.isLocked}
                  onClick={() => handleVote('downvote')}
                  onKeyDown={(e) => handleVoteKeyDown(e, 'downvote')}
                  aria-label="Downvote this post"
                  sx={{
                    color: userVote === 'downvote' ? theme.palette.error.main : 'inherit',
                    '&.vote-button--active': {
                      bgcolor: alpha(theme.palette.error.main, 0.1)
                    }
                  }}
                >
                  <ThumbDown fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>
          </Stack>

          {/* Reply count */}
          <Stack direction="row" alignItems="center" spacing={0.5}>
            <Reply fontSize="small" color="disabled" />
            <Typography
              data-testid="comment-count"
              variant="body2"
              color="text.secondary"
            >
              {post.replyCount} {post.replyCount === 1 ? 'reply' : 'replies'}
            </Typography>
          </Stack>
        </Stack>
      </CardContent>

      {/* Actions */}
      <CardActions sx={{ px: 3, pb: 2 }}>
        <Stack direction="row" spacing={1}>
          {/* Edit button (only for author) */}
          {isAuthor && onEdit && (
            <IconButton
              onClick={() => onEdit(post.id)}
              aria-label="Edit post"
              size="small"
            >
              <Edit fontSize="small" />
            </IconButton>
          )}

          {/* Share button */}
          {onShare && (
            <IconButton
              onClick={() => onShare(post.id)}
              aria-label="Share post"
              size="small"
            >
              <Share fontSize="small" />
            </IconButton>
          )}
        </Stack>
      </CardActions>
    </Card>
  )
}

export default ForumPost