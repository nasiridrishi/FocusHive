import React, {useCallback, useEffect, useState} from 'react'
import {SelectChangeEvent} from "@mui/material/Select";
import {
  Alert,
  Avatar,
  Box,
  Breadcrumbs,
  Button,
  Chip,
  CircularProgress,
  Container,
  Divider,
  FormControl,
  IconButton,
  InputLabel,
  Link,
  List,
  ListItem,
  ListItemAvatar,
  ListItemSecondaryAction,
  ListItemText,
  Menu,
  MenuItem,
  Pagination,
  Paper,
  Select,
  Typography
} from '@mui/material'
import {
  Add as AddIcon,
  Category as CategoryIcon,
  Home as HomeIcon,
  Lock as LockIcon,
  MoreVert as MoreVertIcon,
  PushPin as PinIcon,
  Reply as ReplyIcon,
  Sort as SortIcon,
  ThumbUp as LikeIcon,
  Visibility as ViewIcon
} from '@mui/icons-material'
import {Link as RouterLink, useNavigate, useParams} from 'react-router-dom'
import {forumApi} from '../services/forumApi'
import {ForumCategory, ForumPost} from '../types'

const ForumPostList: React.FC = () => {
  const navigate = useNavigate()
  const {categorySlug} = useParams<{ categorySlug: string }>()

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [posts, setPosts] = useState<ForumPost[]>([])
  const [category, setCategory] = useState<ForumCategory | null>(null)
  const [currentPage, setCurrentPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [sortBy, setSortBy] = useState<'recent' | 'popular' | 'oldest'>('recent')
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null)
  const [selectedPost, setSelectedPost] = useState<ForumPost | null>(null)

  const postsPerPage = 20

  const loadCategoryAndPosts = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)

      // Load category info
      const categoryData = await forumApi.getCategoryBySlug(categorySlug || '')
      setCategory(categoryData)

      // Load posts in this category
      const postsData = await forumApi.getPosts(
          Number(categoryData.id),
          currentPage,
          postsPerPage,
          sortBy
      )

      setPosts(postsData.posts)
      setTotalPages(postsData.totalPages)
    } catch (err) {
      const error = err as { response?: { data?: { message?: string } } }
      setError(error.response?.data?.message || 'Failed to load posts')
    } finally {
      setLoading(false)
    }
  }, [categorySlug, currentPage, sortBy])

  useEffect(() => {
    if (categorySlug) {
      loadCategoryAndPosts()
    }
  }, [categorySlug, currentPage, sortBy, loadCategoryAndPosts])

  const handlePageChange = (event: React.ChangeEvent<unknown>, page: number): void => {
    setCurrentPage(page)
    window.scrollTo(0, 0)
  }

  const handleSortChange = (event: SelectChangeEvent<"popular" | "recent" | "oldest">): void => {
    setSortBy(event.target.value as 'recent' | 'popular' | 'oldest')
    setCurrentPage(1) // Reset to first page when changing sort
  }

  const handlePostClick = (post: ForumPost): void => {
    navigate(`/forum/posts/${post.slug}`)
  }

  const handleCreatePost = (): void => {
    navigate(`/forum/categories/${categorySlug}/create`)
  }

  const handleMenuClick = (event: React.MouseEvent<HTMLButtonElement>, post: ForumPost): void => {
    event.stopPropagation()
    setSelectedPost(post)
    setMenuAnchor(event.currentTarget)
  }

  const handleMenuClose = (): void => {
    setMenuAnchor(null)
    setSelectedPost(null)
  }

  const formatNumber = (num: number): string => {
    if (num >= 1000000) return `${(num / 1000000).toFixed(1)}M`
    if (num >= 1000) return `${(num / 1000).toFixed(1)}K`
    return num.toString()
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

  const getPostStatusIcons = (post: ForumPost): React.ReactElement[] => {
    const icons = []

    if (post.isPinned) {
      icons.push(
          <PinIcon key="pinned" color="warning" fontSize="small"/>
      )
    }

    if (post.isLocked) {
      icons.push(
          <LockIcon key="locked" color="error" fontSize="small"/>
      )
    }

    return icons
  }

  if (loading) {
    return (
        <Container sx={{py: 4}}>
          <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
            <CircularProgress/>
          </Box>
        </Container>
    )
  }

  if (error || !category) {
    return (
        <Container sx={{py: 4}}>
          <Alert severity="error">
            {error || 'Category not found'}
          </Alert>
        </Container>
    )
  }

  return (
      <Container sx={{py: 4}}>
        {/* Breadcrumbs */}
        <Breadcrumbs sx={{mb: 3}}>
          <Link component={RouterLink} to="/forum" color="inherit">
            <Box display="flex" alignItems="center" gap={0.5}>
              <HomeIcon fontSize="small"/>
              Forum
            </Box>
          </Link>
          <Link component={RouterLink} to="/forum/categories" color="inherit">
            <Box display="flex" alignItems="center" gap={0.5}>
              <CategoryIcon fontSize="small"/>
              Categories
            </Box>
          </Link>
          <Typography color="textPrimary">
            {category.name}
          </Typography>
        </Breadcrumbs>

        {/* Category Header */}
        <Paper sx={{p: 3, mb: 3}}>
          <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={2}>
            <Box>
              <Typography variant="h4" gutterBottom
                          sx={{display: 'flex', alignItems: 'center', gap: 1}}>
                {category.name}
                {category.isLocked && <LockIcon color="error"/>}
              </Typography>
              <Typography variant="body1" color="textSecondary" paragraph>
                {category.description}
              </Typography>
            </Box>

            {!category.isLocked && (
                <Button
                    variant="contained"
                    startIcon={<AddIcon/>}
                    onClick={handleCreatePost}
                >
                  New Post
                </Button>
            )}
          </Box>

          {/* Category Stats */}
          <Box display="flex" gap={4} flexWrap="wrap">
            <Box display="flex" alignItems="center" gap={0.5}>
              <Typography variant="body2" color="textSecondary">
                Posts: <strong>{formatNumber(category.postCount)}</strong>
              </Typography>
            </Box>
            <Box display="flex" alignItems="center" gap={0.5}>
              <Typography variant="body2" color="textSecondary">
                Topics: <strong>{formatNumber(category.topicCount)}</strong>
              </Typography>
            </Box>
            {category.lastActivity && (
                <Box display="flex" alignItems="center" gap={0.5}>
                  <Typography variant="body2" color="textSecondary">
                    Last Activity: <strong>{formatTimeAgo(category.lastActivity)}</strong>
                  </Typography>
                </Box>
            )}
          </Box>
        </Paper>

        {/* Controls */}
        <Box display="flex" justifyContent="between" alignItems="center" mb={3}>
          <FormControl size="small" sx={{minWidth: 120}}>
            <InputLabel>Sort By</InputLabel>
            <Select
                value={sortBy}
                onChange={handleSortChange}
                label="Sort By"
                startAdornment={<SortIcon sx={{ml: 1, mr: 0.5}}/>}
            >
              <MenuItem value="recent">Most Recent</MenuItem>
              <MenuItem value="popular">Most Popular</MenuItem>
              <MenuItem value="oldest">Oldest First</MenuItem>
            </Select>
          </FormControl>

          <Typography variant="body2" color="textSecondary">
            {posts.length > 0 && (
                `Showing ${(currentPage - 1) * postsPerPage + 1}-${Math.min(currentPage * postsPerPage, posts.length)} of ${category.postCount} posts`
            )}
          </Typography>
        </Box>

        {error && (
            <Alert severity="error" sx={{mb: 3}} onClose={() => setError(null)}>
              {error}
            </Alert>
        )}

        {/* Posts List */}
        {posts.length === 0 ? (
            <Paper sx={{p: 4, textAlign: 'center'}}>
              <Typography variant="h6" color="textSecondary" gutterBottom>
                No posts yet in this category
              </Typography>
              <Typography variant="body2" color="textSecondary" paragraph>
                Be the first to start a discussion!
              </Typography>
              {!category.isLocked && (
                  <Button
                      variant="contained"
                      startIcon={<AddIcon/>}
                      onClick={handleCreatePost}
                  >
                    Create First Post
                  </Button>
              )}
            </Paper>
        ) : (
            <Paper>
              <List>
                {posts.map((post, index) => (
                    <React.Fragment key={post.id}>
                      <ListItem
                          button
                          onClick={() => handlePostClick(post)}
                          sx={{
                            py: 2,
                            '&:hover': {backgroundColor: 'action.hover'}
                          }}
                      >
                        <ListItemAvatar>
                          <Avatar src={post.author.avatar}>
                            {post.author.username[0].toUpperCase()}
                          </Avatar>
                        </ListItemAvatar>

                        <ListItemText
                            primary={
                              <Box>
                                <Box display="flex" alignItems="center" gap={1} mb={0.5}>
                                  <Typography variant="h6" component="div" sx={{flexGrow: 1}}>
                                    {post.title}
                                  </Typography>
                                  <Box display="flex" gap={0.5}>
                                    {getPostStatusIcons(post)}
                                  </Box>
                                </Box>

                                <Box display="flex" alignItems="center" gap={2} flexWrap="wrap">
                                  <Typography variant="body2" color="textSecondary">
                                    by <strong>{post.author.username}</strong>
                                  </Typography>

                                  <Typography variant="body2" color="textSecondary">
                                    {formatTimeAgo(post.createdAt)}
                                  </Typography>

                                  {/* Post stats */}
                                  <Box display="flex" alignItems="center" gap={2}>
                                    <Box display="flex" alignItems="center" gap={0.5}>
                                      <ReplyIcon fontSize="small" color="action"/>
                                      <Typography variant="body2" color="textSecondary">
                                        {formatNumber(post.replyCount)}
                                      </Typography>
                                    </Box>

                                    <Box display="flex" alignItems="center" gap={0.5}>
                                      <ViewIcon fontSize="small" color="action"/>
                                      <Typography variant="body2" color="textSecondary">
                                        {formatNumber(post.viewCount)}
                                      </Typography>
                                    </Box>

                                    <Box display="flex" alignItems="center" gap={0.5}>
                                      <LikeIcon fontSize="small" color="action"/>
                                      <Typography variant="body2" color="textSecondary">
                                        {formatNumber(post.likeCount)}
                                      </Typography>
                                    </Box>
                                  </Box>
                                </Box>

                                {/* Tags */}
                                {post.tags && post.tags.length > 0 && (
                                    <Box display="flex" gap={0.5} mt={1} flexWrap="wrap">
                                      {post.tags.slice(0, 3).map((tag) => (
                                          <Chip
                                              key={tag}
                                              label={tag}
                                              size="small"
                                              variant="outlined"
                                              sx={{fontSize: '0.7rem'}}
                                          />
                                      ))}
                                      {post.tags.length > 3 && (
                                          <Chip
                                              label={`+${post.tags.length - 3} more`}
                                              size="small"
                                              variant="outlined"
                                              sx={{fontSize: '0.7rem'}}
                                          />
                                      )}
                                    </Box>
                                )}
                              </Box>
                            }
                            secondary={
                                post.lastReply && (
                                    <Box mt={1}>
                                      <Typography variant="caption" color="textSecondary">
                                        Last reply
                                        by <strong>{post.lastReply.author.username}</strong> • {formatTimeAgo(post.lastReply.createdAt)}
                                      </Typography>
                                    </Box>
                                )
                            }
                        />

                        <ListItemSecondaryAction>
                          <IconButton
                              edge="end"
                              onClick={(e) => handleMenuClick(e, post)}
                          >
                            <MoreVertIcon/>
                          </IconButton>
                        </ListItemSecondaryAction>
                      </ListItem>

                      {index < posts.length - 1 && <Divider/>}
                    </React.Fragment>
                ))}
              </List>
            </Paper>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
            <Box display="flex" justifyContent="center" mt={4}>
              <Pagination
                  count={totalPages}
                  page={currentPage}
                  onChange={handlePageChange}
                  color="primary"
                  showFirstButton
                  showLastButton
              />
            </Box>
        )}

        {/* Post Context Menu */}
        <Menu
            anchorEl={menuAnchor}
            open={Boolean(menuAnchor)}
            onClose={handleMenuClose}
        >
          {selectedPost && (
              <MenuItem onClick={() => {
                handlePostClick(selectedPost)
                handleMenuClose()
              }}>
                View Post
              </MenuItem>
          )}
        </Menu>
      </Container>
  )
}

export default ForumPostList