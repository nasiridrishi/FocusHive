import React, { useState, useMemo, useCallback, useRef, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Avatar,
  IconButton,
  Button,
  TextField,
  Collapse,
  Chip,
  Menu,
  MenuItem,
  Divider,
  Stack,
  Select,
  MenuItem as SelectMenuItem,
  FormControl,
  InputLabel,
  Alert,
  CircularProgress,
  Skeleton,
  Tooltip,
  Badge,
  useTheme,
  useMediaQuery,
  Paper,
  ListItemIcon,
  ListItemText,
  Fade,
  Grow,
} from '@mui/material';
import {
  ThumbUp as ThumbUpIcon,
  ThumbUpOutlined as ThumbUpOutlinedIcon,
  ThumbDown as ThumbDownIcon,
  ThumbDownOutlined as ThumbDownOutlinedIcon,
  Reply as ReplyIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  MoreVert as MoreVertIcon,
  ExpandMore as ExpandMoreIcon,
  ExpandLess as ExpandLessIcon,
  Flag as FlagIcon,
  PushPin as PushPinIcon,
  Lock as LockIcon,
  Visibility as VisibilityIcon,
  Schedule as ScheduleIcon,
  TrendingUp as TrendingUpIcon,
  Forum as ForumIcon,
  CheckCircle as CheckCircleIcon,
  Star as StarIcon,
  EmojiEvents as TrophyIcon,
} from '@mui/icons-material';
import { formatDistanceToNow } from 'date-fns';
import type { ForumPost as ForumThreadType, ForumReply } from '@features/forum/types';

// Types
interface ForumThreadProps {
  thread: ForumThreadType;
  currentUserId?: string;
  onReply?: (parentId: string | null, content: string) => Promise<void>;
  onVote?: (targetId: string, targetType: 'thread' | 'reply', vote: 'up' | 'down') => Promise<void>;
  onEdit?: (targetId: string, targetType: 'thread' | 'reply', content: string) => Promise<void>;
  onDelete?: (targetId: string, targetType: 'thread' | 'reply') => Promise<void>;
  onReport?: (targetId: string, targetType: 'thread' | 'reply', reason: string) => Promise<void>;
  maxDepth?: number;
  sortBy?: 'newest' | 'top' | 'controversial';
  isLoading?: boolean;
  error?: string | null;
  showReplies?: boolean;
  autoFocusReply?: boolean;
}

interface ReplyItemProps {
  reply: ForumReply;
  currentUserId?: string;
  onReply?: (parentId: string, content: string) => Promise<void>;
  onVote?: (replyId: string, vote: 'up' | 'down') => Promise<void>;
  onEdit?: (replyId: string, content: string) => Promise<void>;
  onDelete?: (replyId: string) => Promise<void>;
  onReport?: (replyId: string, reason: string) => Promise<void>;
  depth: number;
  maxDepth: number;
  parentAuthor?: string;
}

// Utility functions
const calculateControversialScore = (likes: number, dislikes: number): number => {
  const total = likes + dislikes;
  if (total === 0) return 0;
  const balance = Math.min(likes, dislikes) / Math.max(likes, dislikes, 1);
  return total * balance;
};

const sortReplies = (replies: ForumReply[], sortBy: string): ForumReply[] => {
  const sorted = [...replies];

  switch (sortBy) {
    case 'top':
      return sorted.sort((a, b) => (b.likeCount - b.dislikeCount) - (a.likeCount - a.dislikeCount));
    case 'controversial':
      return sorted.sort((a, b) =>
        calculateControversialScore(b.likeCount, b.dislikeCount) -
        calculateControversialScore(a.likeCount, a.dislikeCount)
      );
    case 'newest':
    default:
      return sorted.sort((a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
  }
};

// Reply Item Component
const ReplyItem: React.FC<ReplyItemProps> = React.memo(({
  reply,
  currentUserId,
  onReply,
  onVote,
  onEdit,
  onDelete,
  onReport,
  depth,
  maxDepth,
  parentAuthor
}) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('tablet'));
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [showReplyForm, setShowReplyForm] = useState(false);
  const [showEditForm, setShowEditForm] = useState(false);
  const [replyContent, setReplyContent] = useState('');
  const [editContent, setEditContent] = useState(reply.content);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [isVoting, setIsVoting] = useState(false);
  const [optimisticVote, setOptimisticVote] = useState<'up' | 'down' | null>(null);
  const replyInputRef = useRef<HTMLInputElement>(null);
  const editInputRef = useRef<HTMLInputElement>(null);

  const isAuthor = currentUserId === reply.authorId;
  const hasChildren = reply.childReplies && reply.childReplies.length > 0;
  const canReply = depth < maxDepth;
  const isDeepNested = depth >= maxDepth;

  useEffect(() => {
    if (showReplyForm && replyInputRef.current) {
      replyInputRef.current.focus();
    }
  }, [showReplyForm]);

  useEffect(() => {
    if (showEditForm && editInputRef.current) {
      editInputRef.current.focus();
    }
  }, [showEditForm]);

  const handleVote = async (vote: 'up' | 'down') => {
    if (!onVote || isVoting) return;

    setIsVoting(true);
    setOptimisticVote(vote);

    try {
      await onVote(String(reply.id), vote);
    } catch (error) {
      setOptimisticVote(null);
      console.error('Vote failed:', error);
    } finally {
      setIsVoting(false);
    }
  };

  const handleReplySubmit = async () => {
    if (!onReply || !replyContent.trim()) return;

    try {
      await onReply(String(reply.id), replyContent);
      setReplyContent('');
      setShowReplyForm(false);
    } catch (error) {
      console.error('Reply failed:', error);
    }
  };

  const handleEditSubmit = async () => {
    if (!onEdit || !editContent.trim()) return;

    try {
      await onEdit(String(reply.id), editContent);
      setShowEditForm(false);
    } catch (error) {
      console.error('Edit failed:', error);
    }
  };

  const handleDelete = async () => {
    if (!onDelete || !window.confirm('Are you sure you want to delete this reply?')) return;

    try {
      await onDelete(String(reply.id));
    } catch (error) {
      console.error('Delete failed:', error);
    }
    setAnchorEl(null);
  };

  const handleReport = async () => {
    if (!onReport) return;

    const reason = window.prompt('Please provide a reason for reporting this reply:');
    if (!reason) return;

    try {
      await onReport(String(reply.id), reason);
      alert('Reply reported successfully');
    } catch (error) {
      console.error('Report failed:', error);
    }
    setAnchorEl(null);
  };

  const getVoteCount = () => {
    let likes = reply.likeCount;
    let dislikes = reply.dislikeCount;

    if (optimisticVote === 'up') likes++;
    if (optimisticVote === 'down') dislikes++;

    return likes - dislikes;
  };

  return (
    <Box
      data-testid={`reply-${reply.id}`}
      className={depth > 0 ? 'nested-reply' : ''}
      sx={{
        ml: depth > 0 ? (isMobile ? 2 : 4) : 0,
        mt: 2,
        borderLeft: depth > 0 ? `2px solid ${theme.palette.divider}` : 'none',
        pl: depth > 0 ? 2 : 0,
        opacity: reply.isHidden ? 0.5 : 1,
      }}
    >
      <Paper
        elevation={0}
        sx={{
          p: 2,
          backgroundColor: depth % 2 === 0 ? 'background.paper' : 'action.hover',
          '&:hover': {
            backgroundColor: 'action.selected',
          },
        }}
      >
        {/* Reply Header */}
        <Stack direction="row" spacing={2} alignItems="flex-start">
          <Avatar
            src={reply.author.avatar}
            alt={reply.author.username}
            sx={{ width: 32, height: 32 }}
          >
            {reply.author?.username?.[0]?.toUpperCase() || 'U'}
          </Avatar>

          <Box flex={1}>
            <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
              <Typography variant="subtitle2" component="span">
                {reply.author?.username || 'Anonymous'}
              </Typography>

              {reply.author?.role === 'MODERATOR' && (
                <Chip
                  size="small"
                  label="Moderator"
                  color="primary"
                  icon={<StarIcon />}
                  sx={{ height: 20 }}
                  data-testid="moderator-badge"
                />
              )}

              {reply.author?.reputation && reply.author.reputation > 100 && (
                <Chip
                  size="small"
                  label={`${reply.author.reputation} rep`}
                  icon={<TrophyIcon />}
                  sx={{ height: 20 }}
                />
              )}

              {parentAuthor && (
                <Typography variant="caption" color="text.secondary">
                  replying to @{parentAuthor}
                </Typography>
              )}

              <Typography variant="caption" color="text.secondary">
                • {(() => {
                  try {
                    const date = new Date(reply.createdAt);
                    if (isNaN(date.getTime())) return reply.createdAt;
                    return formatDistanceToNow(date, { addSuffix: true });
                  } catch {
                    return reply.createdAt;
                  }
                })()}
              </Typography>

              {reply.updatedAt !== reply.createdAt && (
                <Typography variant="caption" color="text.secondary">
                  (edited)
                </Typography>
              )}
            </Stack>

            {/* Reply Content */}
            <Collapse in={!isCollapsed}>
              {showEditForm ? (
                <Box sx={{ mt: 1 }}>
                  <TextField
                    ref={editInputRef}
                    fullWidth
                    multiline
                    rows={3}
                    value={editContent}
                    onChange={(e) => setEditContent(e.target.value)}
                    placeholder="Edit your reply..."
                    variant="outlined"
                    size="small"
                  />
                  <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
                    <Button
                      size="small"
                      variant="contained"
                      onClick={handleEditSubmit}
                      disabled={!editContent.trim()}
                    >
                      Save
                    </Button>
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => {
                        setShowEditForm(false);
                        setEditContent(reply.content);
                      }}
                    >
                      Cancel
                    </Button>
                  </Stack>
                </Box>
              ) : (
                <Typography variant="body2" sx={{ mt: 1, whiteSpace: 'pre-wrap' }}>
                  {reply.content}
                </Typography>
              )}
            </Collapse>

            {/* Reply Actions */}
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 1 }}>
              <Stack direction="row" spacing={0.5} alignItems="center">
                <IconButton
                  size="small"
                  onClick={() => handleVote('up')}
                  disabled={isVoting}
                  color={optimisticVote === 'up' ? 'primary' : 'default'}
                  aria-label="upvote"
                >
                  {optimisticVote === 'up' ? <ThumbUpIcon fontSize="small" /> : <ThumbUpOutlinedIcon fontSize="small" />}
                </IconButton>

                <Typography variant="caption" sx={{ minWidth: 20, textAlign: 'center' }}>
                  {getVoteCount()}
                </Typography>

                <IconButton
                  size="small"
                  onClick={() => handleVote('down')}
                  disabled={isVoting}
                  color={optimisticVote === 'down' ? 'error' : 'default'}
                  aria-label="downvote"
                >
                  {optimisticVote === 'down' ? <ThumbDownIcon fontSize="small" /> : <ThumbDownOutlinedIcon fontSize="small" />}
                </IconButton>
              </Stack>

              {canReply && (
                <Button
                  size="small"
                  startIcon={<ReplyIcon />}
                  onClick={() => setShowReplyForm(!showReplyForm)}
                >
                  Reply
                </Button>
              )}

              {isAuthor && !showEditForm && (
                <Button
                  size="small"
                  startIcon={<EditIcon />}
                  onClick={() => setShowEditForm(true)}
                >
                  Edit
                </Button>
              )}

              {hasChildren && (
                <Button
                  size="small"
                  startIcon={isCollapsed ? <ExpandMoreIcon /> : <ExpandLessIcon />}
                  onClick={() => setIsCollapsed(!isCollapsed)}
                >
                  {isCollapsed ? `Show ${reply.childReplies!.length} replies` : 'Hide replies'}
                </Button>
              )}

              <IconButton
                size="small"
                onClick={(e) => setAnchorEl(e.currentTarget)}
                aria-label="more options"
              >
                <MoreVertIcon fontSize="small" />
              </IconButton>
            </Stack>

            {/* Reply Form */}
            <Collapse in={showReplyForm}>
              <Box sx={{ mt: 2 }}>
                <TextField
                  ref={replyInputRef}
                  fullWidth
                  multiline
                  rows={3}
                  value={replyContent}
                  onChange={(e) => setReplyContent(e.target.value)}
                  placeholder="Write your reply..."
                  variant="outlined"
                  size="small"
                />
                <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
                  <Button
                    size="small"
                    variant="contained"
                    onClick={handleReplySubmit}
                    disabled={!replyContent.trim()}
                  >
                    Post Reply
                  </Button>
                  <Button
                    size="small"
                    variant="outlined"
                    onClick={() => {
                      setShowReplyForm(false);
                      setReplyContent('');
                    }}
                  >
                    Cancel
                  </Button>
                </Stack>
              </Box>
            </Collapse>

            {/* Deep Nesting Message */}
            {isDeepNested && hasChildren && (
              <Alert severity="info" sx={{ mt: 2 }}>
                <Typography variant="caption">
                  Continue this thread → Replies are nested too deep to display here
                </Typography>
              </Alert>
            )}

            {/* Child Replies */}
            {!isCollapsed && hasChildren && !isDeepNested && (
              <Box sx={{ mt: 2 }}>
                {reply.childReplies!.map((childReply) => (
                  <ReplyItem
                    key={childReply.id}
                    reply={childReply}
                    currentUserId={currentUserId}
                    onReply={onReply}
                    onVote={onVote}
                    onEdit={onEdit}
                    onDelete={onDelete}
                    onReport={onReport}
                    depth={depth + 1}
                    maxDepth={maxDepth}
                    parentAuthor={reply.author.username}
                  />
                ))}
              </Box>
            )}
          </Box>
        </Stack>
      </Paper>

      {/* Context Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
      >
        {isAuthor && (
          <MenuItem onClick={handleDelete}>
            <ListItemIcon>
              <DeleteIcon fontSize="small" />
            </ListItemIcon>
            <ListItemText>Delete</ListItemText>
          </MenuItem>
        )}
        <MenuItem onClick={handleReport}>
          <ListItemIcon>
            <FlagIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Report</ListItemText>
        </MenuItem>
      </Menu>
    </Box>
  );
});

ReplyItem.displayName = 'ReplyItem';

// Main ForumThread Component
export const ForumThread: React.FC<ForumThreadProps> = ({
  thread,
  currentUserId,
  onReply,
  onVote,
  onEdit,
  onDelete,
  onReport,
  maxDepth = 5,
  sortBy = 'newest',
  isLoading = false,
  error = null,
  showReplies = true,
  autoFocusReply = false
}) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('tablet'));
  const [localSortBy, setLocalSortBy] = useState(sortBy);
  const [showReplyForm, setShowReplyForm] = useState(false);
  const [replyContent, setReplyContent] = useState('');
  const [editMode, setEditMode] = useState(false);
  const [editContent, setEditContent] = useState(thread.content);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [isVoting, setIsVoting] = useState(false);
  const [optimisticVote, setOptimisticVote] = useState<'up' | 'down' | null>(null);
  const [repliesCollapsed, setRepliesCollapsed] = useState(false);
  const replyInputRef = useRef<HTMLInputElement>(null);
  const editInputRef = useRef<HTMLInputElement>(null);

  const isAuthor = currentUserId === thread.authorId;
  const isModerator = thread.author?.role === 'MODERATOR';

  useEffect(() => {
    if (autoFocusReply && showReplyForm && replyInputRef.current) {
      replyInputRef.current.focus();
    }
  }, [showReplyForm, autoFocusReply]);

  useEffect(() => {
    if (editMode && editInputRef.current) {
      editInputRef.current.focus();
    }
  }, [editMode]);

  const sortedReplies = useMemo(() => {
    // ForumPost type doesn't have replies, it's likely in a separate field
    const replies = (thread as any).replies || [];
    if (!replies || replies.length === 0) return [];
    return sortReplies(replies, localSortBy);
  }, [(thread as any).replies, localSortBy]);

  const handleVote = useCallback(async (vote: 'up' | 'down') => {
    if (!onVote || isVoting) return;

    setIsVoting(true);
    setOptimisticVote(vote);

    try {
      await onVote(String(thread.id), 'thread', vote);
    } catch (error) {
      setOptimisticVote(null);
      console.error('Vote failed:', error);
    } finally {
      setIsVoting(false);
    }
  }, [onVote, thread.id, isVoting]);

  const handleReplySubmit = useCallback(async () => {
    if (!onReply || !replyContent.trim()) return;

    try {
      await onReply(null, replyContent);
      setReplyContent('');
      setShowReplyForm(false);
    } catch (error) {
      console.error('Reply failed:', error);
    }
  }, [onReply, replyContent]);

  const handleEditSubmit = useCallback(async () => {
    if (!onEdit || !editContent.trim()) return;

    try {
      await onEdit(String(thread.id), 'thread', editContent);
      setEditMode(false);
    } catch (error) {
      console.error('Edit failed:', error);
    }
  }, [onEdit, thread.id, editContent]);

  const handleDelete = useCallback(async () => {
    if (!onDelete || !window.confirm('Are you sure you want to delete this thread?')) return;

    try {
      await onDelete(String(thread.id), 'thread');
    } catch (error) {
      console.error('Delete failed:', error);
    }
    setAnchorEl(null);
  }, [onDelete, thread.id]);

  const handleReport = useCallback(async () => {
    if (!onReport) return;

    const reason = window.prompt('Please provide a reason for reporting this thread:');
    if (!reason) return;

    try {
      await onReport(String(thread.id), 'thread', reason);
      alert('Thread reported successfully');
    } catch (error) {
      console.error('Report failed:', error);
    }
    setAnchorEl(null);
  }, [onReport, thread.id]);

  const getVoteCount = () => {
    let likes = thread.likeCount;
    let dislikes = thread.dislikeCount;

    if (optimisticVote === 'up') likes++;
    if (optimisticVote === 'down') dislikes++;

    return likes - dislikes;
  };

  // Loading state
  if (isLoading) {
    return (
      <Card data-testid="forum-thread-skeleton">
        <CardContent>
          <Skeleton variant="text" width="80%" height={40} />
          <Stack direction="row" spacing={2} sx={{ mt: 2, mb: 2 }}>
            <Skeleton variant="circular" width={40} height={40} />
            <Box flex={1}>
              <Skeleton variant="text" width="30%" />
              <Skeleton variant="text" width="100%" />
              <Skeleton variant="text" width="100%" />
            </Box>
          </Stack>
          {[1, 2, 3].map((i) => (
            <Box key={i} sx={{ ml: 4, mt: 2 }}>
              <Skeleton variant="rectangular" height={80} />
            </Box>
          ))}
        </CardContent>
      </Card>
    );
  }

  // Error state
  if (error) {
    return (
      <Alert severity="error" data-testid="forum-thread-error">
        <Typography>{error}</Typography>
      </Alert>
    );
  }

  return (
    <Card data-testid="forum-thread">
      <CardContent>
        {/* Thread Header */}
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
          <Box flex={1}>
            <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
              {thread.isPinned && (
                <Tooltip title="Pinned">
                  <Box data-testid="pinned-indicator" sx={{ display: 'inline-flex' }}>
                    <PushPinIcon color="primary" />
                  </Box>
                </Tooltip>
              )}
              {thread.isLocked && (
                <Tooltip title="Locked">
                  <Box data-testid="locked-indicator" sx={{ display: 'inline-flex' }}>
                    <LockIcon color="action" />
                  </Box>
                </Tooltip>
              )}
              <Typography variant="h5" component="h1" gutterBottom>
                {thread.title}
              </Typography>
            </Stack>

            {/* Tags */}
            {thread.tags && thread.tags.length > 0 && (
              <Stack direction="row" spacing={1} sx={{ mb: 2 }} flexWrap="wrap">
                {thread.tags.map((tag) => (
                  <Chip
                    key={tag}
                    label={tag}
                    size="small"
                    variant="outlined"
                    sx={{ mb: 0.5 }}
                  />
                ))}
              </Stack>
            )}

            {/* Category */}
            {thread.category && (
              <Chip
                label={thread.category.name}
                size="small"
                sx={{
                  backgroundColor: thread.category.color,
                  color: theme.palette.getContrastText(thread.category.color),
                  mb: 2
                }}
              />
            )}
          </Box>

          {/* Options Menu */}
          <IconButton
            onClick={(e) => setAnchorEl(e.currentTarget)}
            aria-label="thread options"
          >
            <MoreVertIcon />
          </IconButton>
        </Stack>

        {/* Author Info */}
        <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }}>
          <Avatar
            src={thread.author.avatar}
            alt={thread.author.username}
            sx={{ width: 48, height: 48 }}
          >
            {thread.author?.username?.[0]?.toUpperCase() || 'U'}
          </Avatar>
          <Box>
            <Stack direction="row" spacing={1} alignItems="center">
              <Typography variant="subtitle1">
                {thread.author?.username || 'Anonymous'}
              </Typography>
              {isModerator && (
                <Chip
                  size="small"
                  label="Moderator"
                  color="primary"
                  icon={<StarIcon />}
                  data-testid="moderator-badge"
                />
              )}
              {thread.author?.reputation && thread.author.reputation > 100 && (
                <Chip
                  size="small"
                  label={`${thread.author.reputation} rep`}
                  icon={<TrophyIcon />}
                />
              )}
            </Stack>
            <Stack direction="row" spacing={2} alignItems="center" data-testid="thread-metadata">
              <Typography variant="caption" color="text.secondary">
                {(() => {
                  try {
                    const date = new Date(thread.createdAt);
                    if (isNaN(date.getTime())) return thread.createdAt;
                    return formatDistanceToNow(date, { addSuffix: true });
                  } catch {
                    return thread.createdAt;
                  }
                })()}
              </Typography>
              {thread.updatedAt !== thread.createdAt && (
                <Typography variant="caption" color="text.secondary">
                  (edited)
                </Typography>
              )}
              <Stack direction="row" spacing={0.5} alignItems="center">
                <VisibilityIcon fontSize="small" color="action" />
                <Typography variant="caption" color="text.secondary">
                  {thread.viewCount} views
                </Typography>
              </Stack>
            </Stack>
          </Box>
        </Stack>

        {/* Thread Content */}
        <Divider sx={{ mb: 2 }} />

        {editMode ? (
          <Box sx={{ mb: 2 }}>
            <TextField
              ref={editInputRef}
              fullWidth
              multiline
              rows={6}
              value={editContent}
              onChange={(e) => setEditContent(e.target.value)}
              variant="outlined"
            />
            <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
              <Button variant="contained" onClick={handleEditSubmit}>
                Save Changes
              </Button>
              <Button
                variant="outlined"
                onClick={() => {
                  setEditMode(false);
                  setEditContent(thread.content);
                }}
              >
                Cancel
              </Button>
            </Stack>
          </Box>
        ) : (
          <Typography variant="body1" sx={{ mb: 3, whiteSpace: 'pre-wrap' }}>
            {thread.content}
          </Typography>
        )}

        {/* Thread Actions */}
        <Stack
          direction={isMobile ? 'column' : 'row'}
          justifyContent="space-between"
          alignItems={isMobile ? 'stretch' : 'center'}
          spacing={2}
        >
          <Stack direction="row" spacing={2} alignItems="center">
            {/* Vote Controls */}
            <Stack direction="row" spacing={1} alignItems="center">
              <IconButton
                onClick={() => handleVote('up')}
                disabled={isVoting}
                color={optimisticVote === 'up' ? 'primary' : 'default'}
                aria-label="upvote thread"
              >
                {optimisticVote === 'up' ? <ThumbUpIcon /> : <ThumbUpOutlinedIcon />}
              </IconButton>

              <Typography variant="body2" sx={{ minWidth: 30, textAlign: 'center' }}>
                {getVoteCount()}
              </Typography>

              <IconButton
                onClick={() => handleVote('down')}
                disabled={isVoting}
                color={optimisticVote === 'down' ? 'error' : 'default'}
                aria-label="downvote thread"
              >
                {optimisticVote === 'down' ? <ThumbDownIcon /> : <ThumbDownOutlinedIcon />}
              </IconButton>
            </Stack>

            {/* Action Buttons */}
            {!thread.isLocked && (
              <Button
                startIcon={<ReplyIcon />}
                onClick={() => setShowReplyForm(!showReplyForm)}
                variant="outlined"
                size="small"
              >
                Reply
              </Button>
            )}

            {isAuthor && !editMode && (
              <Button
                startIcon={<EditIcon />}
                onClick={() => setEditMode(true)}
                variant="outlined"
                size="small"
              >
                Edit
              </Button>
            )}

            {thread.replyCount > 0 && (
              <Button
                startIcon={repliesCollapsed ? <ExpandMoreIcon /> : <ExpandLessIcon />}
                onClick={() => setRepliesCollapsed(!repliesCollapsed)}
                size="small"
              >
                {repliesCollapsed
                  ? `Show ${thread.replyCount} replies`
                  : `Hide replies`}
              </Button>
            )}
          </Stack>

          {/* Reply Stats */}
          <Stack direction="row" spacing={2} alignItems="center">
            <Stack direction="row" spacing={0.5} alignItems="center">
              <ForumIcon fontSize="small" color="action" />
              <Typography variant="caption" color="text.secondary">
                {thread.replyCount} replies
              </Typography>
            </Stack>
            {thread.lastReplyAt && (
              <Typography variant="caption" color="text.secondary">
                Last reply {(() => {
                  try {
                    const date = new Date(thread.lastReplyAt!);
                    if (isNaN(date.getTime())) return thread.lastReplyAt;
                    return formatDistanceToNow(date, { addSuffix: true });
                  } catch {
                    return thread.lastReplyAt;
                  }
                })()}
              </Typography>
            )}
          </Stack>
        </Stack>

        {/* Reply Form */}
        <Collapse in={showReplyForm}>
          <Box sx={{ mt: 3 }}>
            <Typography variant="subtitle2" gutterBottom>
              Write your reply
            </Typography>
            <TextField
              ref={replyInputRef}
              fullWidth
              multiline
              rows={4}
              value={replyContent}
              onChange={(e) => setReplyContent(e.target.value)}
              placeholder="Share your thoughts..."
              variant="outlined"
            />
            <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
              <Button
                variant="contained"
                onClick={handleReplySubmit}
                disabled={!replyContent.trim()}
              >
                Post Reply
              </Button>
              <Button
                variant="outlined"
                onClick={() => {
                  setShowReplyForm(false);
                  setReplyContent('');
                }}
              >
                Cancel
              </Button>
            </Stack>
          </Box>
        </Collapse>

        {/* Replies Section */}
        {showReplies && !repliesCollapsed && (thread as any).replies && (thread as any).replies.length > 0 && (
          <Box sx={{ mt: 3 }}>
            <Divider sx={{ mb: 2 }} />

            {/* Sort Controls */}
            <Stack
              direction="row"
              justifyContent="space-between"
              alignItems="center"
              sx={{ mb: 2 }}
            >
              <Typography variant="h6">
                Replies ({thread.replyCount})
              </Typography>

              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel>Sort by</InputLabel>
                <Select
                  value={localSortBy}
                  onChange={(e) => setLocalSortBy(e.target.value as 'newest' | 'top' | 'controversial')}
                  label="Sort by"
                  data-testid="sort-select"
                >
                  <SelectMenuItem value="newest">
                    <Stack direction="row" spacing={1} alignItems="center">
                      <ScheduleIcon fontSize="small" />
                      <span>Newest</span>
                    </Stack>
                  </SelectMenuItem>
                  <SelectMenuItem value="top">
                    <Stack direction="row" spacing={1} alignItems="center">
                      <TrendingUpIcon fontSize="small" />
                      <span>Top</span>
                    </Stack>
                  </SelectMenuItem>
                  <SelectMenuItem value="controversial">
                    <Stack direction="row" spacing={1} alignItems="center">
                      <ForumIcon fontSize="small" />
                      <span>Controversial</span>
                    </Stack>
                  </SelectMenuItem>
                </Select>
              </FormControl>
            </Stack>

            {/* Replies List */}
            <Box>
              {sortedReplies.map((reply) => (
                <ReplyItem
                  key={reply.id}
                  reply={reply}
                  currentUserId={currentUserId}
                  onReply={onReply ? (parentId, content) => onReply(parentId, content) : undefined}
                  onVote={onVote ? (replyId, vote) => onVote(replyId, 'reply', vote) : undefined}
                  onEdit={onEdit ? (replyId, content) => onEdit(replyId, 'reply', content) : undefined}
                  onDelete={onDelete ? (replyId) => onDelete(replyId, 'reply') : undefined}
                  onReport={onReport ? (replyId, reason) => onReport(replyId, 'reply', reason) : undefined}
                  depth={0}
                  maxDepth={maxDepth}
                />
              ))}
            </Box>
          </Box>
        )}

        {/* No Replies Message */}
        {showReplies && !repliesCollapsed && (!(thread as any).replies || (thread as any).replies.length === 0) && (
          <Box sx={{ mt: 3, textAlign: 'center', py: 4 }}>
            <ForumIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 2 }} />
            <Typography variant="body1" color="text.secondary">
              No replies yet. Be the first to reply!
            </Typography>
          </Box>
        )}
      </CardContent>

      {/* Context Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
      >
        {isAuthor && (
          <MenuItem onClick={handleDelete}>
            <ListItemIcon>
              <DeleteIcon fontSize="small" />
            </ListItemIcon>
            <ListItemText>Delete Thread</ListItemText>
          </MenuItem>
        )}
        <MenuItem onClick={handleReport}>
          <ListItemIcon>
            <FlagIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Report Thread</ListItemText>
        </MenuItem>
      </Menu>
    </Card>
  );
};