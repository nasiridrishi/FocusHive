import React, {useEffect, useState} from 'react'
import {
  Alert,
  Avatar,
  Badge,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Divider,
  Grid,
  InputAdornment,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Paper,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material'

// Grid component type workaround
import {
  Add as AddIcon,
  Article as ArticleIcon,
  Category as CategoryIcon,
  Forum as ForumIcon,
  People as PeopleIcon,
  Reply as ReplyIcon,
  Schedule as RecentIcon,
  Search as SearchIcon,
  Star as StarIcon,
  TrendingUp as TrendingIcon,
  Visibility as ViewIcon,
  Whatshot as PopularIcon
} from '@mui/icons-material'
import {useNavigate} from 'react-router-dom'
import {forumApi} from '../services/forumApi'
import {ForumCategory, ForumPost, ForumStats, ForumUser} from '../types'
import ForumCategoryList from './ForumCategoryList'

interface TabPanelProps {
  children?: React.ReactNode
  index: number
  value: number
}

function TabPanel(props: TabPanelProps): React.ReactElement {
  const {children, value, index, ...other} = props
  return (
      <div
          role="tabpanel"
          hidden={value !== index}
          id={`forum-tabpanel-${index}`}
          aria-labelledby={`forum-tab-${index}`}
          {...other}
      >
        {value === index && <Box sx={{py: 3}}>{children}</Box>}
      </div>
  )
}

const ForumHome: React.FC = () => {
  const navigate = useNavigate()
  const [tabValue, setTabValue] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState('')

  // Data states
  const [categories, setCategories] = useState<ForumCategory[]>([])
  const [trendingPosts, setTrendingPosts] = useState<ForumPost[]>([])
  const [recentPosts, setRecentPosts] = useState<ForumPost[]>([])
  const [popularPosts, setPopularPosts] = useState<ForumPost[]>([])
  const [forumStats, setForumStats] = useState<ForumStats | null>(null)
  const [topContributors, setTopContributors] = useState<ForumUser[]>([])

  useEffect(() => {
    loadForumData()
  }, [])

  const loadForumData = async () => {
    try {
      setLoading(true)
      const [
        categoriesData,
        trendingData,
        recentData,
        popularData,
        statsData,
        usersData
      ] = await Promise.all([
        forumApi.getCategories(),
        forumApi.getTrendingPosts(5),
        forumApi.getPosts(undefined, 1, 5, 'recent'),
        forumApi.getPopularPosts('week', 5),
        forumApi.getForumStats(),
        forumApi.getForumUsers(1, 5)
      ])

      setCategories(categoriesData)
      setTrendingPosts(trendingData)
      setRecentPosts(recentData.posts)
      setPopularPosts(popularData)
      setForumStats(statsData)
      setTopContributors(usersData.users)
    } catch {
      // console.error('Error loading forum data');
      setError('Failed to load forum data')
    } finally {
      setLoading(false)
    }
  }

  const handleTabChange = (event: React.SyntheticEvent, newValue: number): void => {
    setTabValue(newValue)
  }

  const handleSearch = (): void => {
    if (searchQuery.trim()) {
      navigate(`/forum/search?q=${encodeURIComponent(searchQuery.trim())}`)
    }
  }

  const handleCreatePost = (): void => {
    navigate('/forum/create')
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

  if (loading) {
    return (
        <Container sx={{py: 4}}>
          <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
            <CircularProgress/>
          </Box>
        </Container>
    )
  }

  return (
      <Container sx={{py: 4}}>
        {/* Header */}
        <Box sx={{mb: 4}}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
            <Box>
              <Typography variant="h3" gutterBottom
                          sx={{display: 'flex', alignItems: 'center', gap: 2}}>
                <ForumIcon fontSize="large" color="primary"/>
                Community Forum
              </Typography>
              <Typography variant="subtitle1" color="textSecondary">
                Connect, discuss, and share knowledge with the FocusHive community
              </Typography>
            </Box>
            <Button
                variant="contained"
                startIcon={<AddIcon/>}
                onClick={handleCreatePost}
                size="large"
            >
              Create Post
            </Button>
          </Box>

          {/* Search Bar */}
          <Paper sx={{p: 2}}>
            <TextField
                fullWidth
                placeholder="Search posts, topics, and discussions..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                InputProps={{
                  startAdornment: (
                      <InputAdornment position="start">
                        <SearchIcon/>
                      </InputAdornment>
                  ),
                  endAdornment: searchQuery && (
                      <InputAdornment position="end">
                        <Button onClick={handleSearch}>Search</Button>
                      </InputAdornment>
                  )
                }}
            />
          </Paper>
        </Box>

        {error && (
            <Alert severity="error" sx={{mb: 3}} onClose={() => setError(null)}>
              {error}
            </Alert>
        )}

        {/* Stats Overview */}
        {forumStats && (
            <Grid container spacing={3} sx={{mb: 4}}>
              <Grid item>
                <Card>
                  <CardContent sx={{textAlign: 'center'}}>
                    <ArticleIcon color="primary" sx={{fontSize: 40, mb: 1}}/>
                    <Typography variant="h4" color="primary">
                      {formatNumber(forumStats.totalPosts)}
                    </Typography>
                    <Typography color="textSecondary">Posts</Typography>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item>
                <Card>
                  <CardContent sx={{textAlign: 'center'}}>
                    <ReplyIcon color="primary" sx={{fontSize: 40, mb: 1}}/>
                    <Typography variant="h4" color="primary">
                      {formatNumber(forumStats.totalReplies)}
                    </Typography>
                    <Typography color="textSecondary">Replies</Typography>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item>
                <Card>
                  <CardContent sx={{textAlign: 'center'}}>
                    <PeopleIcon color="primary" sx={{fontSize: 40, mb: 1}}/>
                    <Typography variant="h4" color="primary">
                      {formatNumber(forumStats.totalUsers)}
                    </Typography>
                    <Typography color="textSecondary">Members</Typography>
                  </CardContent>
                </Card>
              </Grid>

              <Grid item>
                <Card>
                  <CardContent sx={{textAlign: 'center'}}>
                    <ViewIcon color="primary" sx={{fontSize: 40, mb: 1}}/>
                    <Typography variant="h4" color="primary">
                      {formatNumber(forumStats.totalViews)}
                    </Typography>
                    <Typography color="textSecondary">Views</Typography>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>
        )}

        <Grid container spacing={4}>
          {/* Main Content */}
          <Grid item>
            {/* Categories Overview */}
            <Paper sx={{mb: 4}}>
              <Box sx={{p: 3}}>
                <Typography variant="h5" gutterBottom
                            sx={{display: 'flex', alignItems: 'center', gap: 1}}>
                  <CategoryIcon/>
                  Categories
                </Typography>
                <ForumCategoryList
                    categories={categories.slice(0, 6)}
                    showAll={false}
                />
                <Box sx={{textAlign: 'center', mt: 2}}>
                  <Button
                      variant="outlined"
                      onClick={() => navigate('/forum/categories')}
                  >
                    View All Categories
                  </Button>
                </Box>
              </Box>
            </Paper>

            {/* Posts Tabs */}
            <Paper>
              <Tabs
                  value={tabValue}
                  onChange={handleTabChange}
                  indicatorColor="primary"
                  textColor="primary"
                  variant="fullWidth"
              >
                <Tab
                    icon={<TrendingIcon/>}
                    label="Trending"
                    iconPosition="start"
                />
                <Tab
                    icon={<RecentIcon/>}
                    label="Recent"
                    iconPosition="start"
                />
                <Tab
                    icon={<PopularIcon/>}
                    label="Popular"
                    iconPosition="start"
                />
              </Tabs>

              <TabPanel value={tabValue} index={0}>
                {/* Trending Posts */}
                <List>
                  {trendingPosts.map((post, index) => (
                      <React.Fragment key={post.id}>
                        <ListItem
                            button
                            onClick={() => navigate(`/forum/posts/${post.slug}`)}
                            sx={{py: 2}}
                        >
                          <ListItemAvatar>
                            <Avatar src={post.author.avatar}>
                              {post.author.username[0].toUpperCase()}
                            </Avatar>
                          </ListItemAvatar>
                          <ListItemText
                              primary={
                                <Box>
                                  <Typography variant="h6" gutterBottom>
                                    {post.title}
                                    {post.isPinned && (
                                        <Chip
                                            size="small"
                                            label="Pinned"
                                            color="warning"
                                            sx={{ml: 1}}
                                        />
                                    )}
                                  </Typography>
                                  <Box display="flex" alignItems="center" gap={2} flexWrap="wrap">
                                    <Chip
                                        size="small"
                                        label={post.category?.name}
                                        variant="outlined"
                                    />
                                    <Typography variant="caption" color="textSecondary">
                                      by {post.author.username}
                                    </Typography>
                                    <Typography variant="caption" color="textSecondary">
                                      {formatTimeAgo(post.createdAt)}
                                    </Typography>
                                    <Box display="flex" alignItems="center" gap={2}>
                                      <Box display="flex" alignItems="center" gap={0.5}>
                                        <ReplyIcon fontSize="small" color="action"/>
                                        <Typography variant="caption">
                                          {post.replyCount}
                                        </Typography>
                                      </Box>
                                      <Box display="flex" alignItems="center" gap={0.5}>
                                        <ViewIcon fontSize="small" color="action"/>
                                        <Typography variant="caption">
                                          {formatNumber(post.viewCount)}
                                        </Typography>
                                      </Box>
                                      <Box display="flex" alignItems="center" gap={0.5}>
                                        <StarIcon fontSize="small" color="action"/>
                                        <Typography variant="caption">
                                          {post.likeCount}
                                        </Typography>
                                      </Box>
                                    </Box>
                                  </Box>
                                </Box>
                              }
                          />
                        </ListItem>
                        {index < trendingPosts.length - 1 && <Divider/>}
                      </React.Fragment>
                  ))}
                </List>
                {trendingPosts.length === 0 && (
                    <Box textAlign="center" py={4}>
                      <Typography color="textSecondary">
                        No trending posts found
                      </Typography>
                    </Box>
                )}
              </TabPanel>

              <TabPanel value={tabValue} index={1}>
                {/* Recent Posts */}
                <List>
                  {recentPosts.map((post, index) => (
                      <React.Fragment key={post.id}>
                        <ListItem
                            button
                            onClick={() => navigate(`/forum/posts/${post.slug}`)}
                            sx={{py: 2}}
                        >
                          <ListItemAvatar>
                            <Avatar src={post.author.avatar}>
                              {post.author.username[0].toUpperCase()}
                            </Avatar>
                          </ListItemAvatar>
                          <ListItemText
                              primary={
                                <Box>
                                  <Typography variant="h6" gutterBottom>
                                    {post.title}
                                    {post.isPinned && (
                                        <Chip
                                            size="small"
                                            label="Pinned"
                                            color="warning"
                                            sx={{ml: 1}}
                                        />
                                    )}
                                  </Typography>
                                  <Box display="flex" alignItems="center" gap={2} flexWrap="wrap">
                                    <Chip
                                        size="small"
                                        label={post.category?.name}
                                        variant="outlined"
                                    />
                                    <Typography variant="caption" color="textSecondary">
                                      by {post.author.username}
                                    </Typography>
                                    <Typography variant="caption" color="textSecondary">
                                      {formatTimeAgo(post.createdAt)}
                                    </Typography>
                                    <Box display="flex" alignItems="center" gap={2}>
                                      <Box display="flex" alignItems="center" gap={0.5}>
                                        <ReplyIcon fontSize="small" color="action"/>
                                        <Typography variant="caption">
                                          {post.replyCount}
                                        </Typography>
                                      </Box>
                                      <Box display="flex" alignItems="center" gap={0.5}>
                                        <ViewIcon fontSize="small" color="action"/>
                                        <Typography variant="caption">
                                          {formatNumber(post.viewCount)}
                                        </Typography>
                                      </Box>
                                    </Box>
                                  </Box>
                                </Box>
                              }
                          />
                        </ListItem>
                        {index < recentPosts.length - 1 && <Divider/>}
                      </React.Fragment>
                  ))}
                </List>
              </TabPanel>

              <TabPanel value={tabValue} index={2}>
                {/* Popular Posts */}
                <List>
                  {popularPosts.map((post, index) => (
                      <React.Fragment key={post.id}>
                        <ListItem
                            button
                            onClick={() => navigate(`/forum/posts/${post.slug}`)}
                            sx={{py: 2}}
                        >
                          <ListItemAvatar>
                            <Avatar src={post.author.avatar}>
                              {post.author.username[0].toUpperCase()}
                            </Avatar>
                          </ListItemAvatar>
                          <ListItemText
                              primary={
                                <Box>
                                  <Typography variant="h6" gutterBottom>
                                    {post.title}
                                    {post.isPinned && (
                                        <Chip
                                            size="small"
                                            label="Pinned"
                                            color="warning"
                                            sx={{ml: 1}}
                                        />
                                    )}
                                  </Typography>
                                  <Box display="flex" alignItems="center" gap={2} flexWrap="wrap">
                                    <Chip
                                        size="small"
                                        label={post.category?.name}
                                        variant="outlined"
                                    />
                                    <Typography variant="caption" color="textSecondary">
                                      by {post.author.username}
                                    </Typography>
                                    <Typography variant="caption" color="textSecondary">
                                      {formatTimeAgo(post.createdAt)}
                                    </Typography>
                                    <Box display="flex" alignItems="center" gap={2}>
                                      <Box display="flex" alignItems="center" gap={0.5}>
                                        <StarIcon fontSize="small" color="warning"/>
                                        <Typography variant="caption">
                                          {post.likeCount}
                                        </Typography>
                                      </Box>
                                      <Box display="flex" alignItems="center" gap={0.5}>
                                        <ViewIcon fontSize="small" color="action"/>
                                        <Typography variant="caption">
                                          {formatNumber(post.viewCount)}
                                        </Typography>
                                      </Box>
                                    </Box>
                                  </Box>
                                </Box>
                              }
                          />
                        </ListItem>
                        {index < popularPosts.length - 1 && <Divider/>}
                      </React.Fragment>
                  ))}
                </List>
              </TabPanel>
            </Paper>
          </Grid>

          {/* Sidebar */}
          <Grid item>
            {/* Top Contributors */}
            <Paper sx={{mb: 3}}>
              <Box sx={{p: 3}}>
                <Typography variant="h6" gutterBottom
                            sx={{display: 'flex', alignItems: 'center', gap: 1}}>
                  <StarIcon color="warning"/>
                  Top Contributors
                </Typography>
                <List dense>
                  {topContributors.map((user, index) => (
                      <ListItem key={user.id} sx={{px: 0}}>
                        <ListItemAvatar>
                          <Badge
                              badgeContent={index + 1}
                              color="primary"
                              anchorOrigin={{
                                vertical: 'bottom',
                                horizontal: 'right',
                              }}
                          >
                            <Avatar src={user.avatar} sx={{width: 32, height: 32}}>
                              {user.username[0].toUpperCase()}
                            </Avatar>
                          </Badge>
                        </ListItemAvatar>
                        <ListItemText
                            primary={user.username}
                            secondary={`${user.postCount} posts • ${user.reputation} rep`}
                        />
                      </ListItem>
                  ))}
                </List>
              </Box>
            </Paper>

            {/* Forum Stats */}
            {forumStats && (
                <Paper>
                  <Box sx={{p: 3}}>
                    <Typography variant="h6" gutterBottom>
                      Forum Activity
                    </Typography>
                    <Box sx={{display: 'flex', flexDirection: 'column', gap: 2}}>
                      <Box display="flex" justifyContent="space-between">
                        <Typography color="textSecondary">Today's Posts:</Typography>
                        <Typography variant="body2">{forumStats.todayPosts}</Typography>
                      </Box>
                      <Box display="flex" justifyContent="space-between">
                        <Typography color="textSecondary">Today's Replies:</Typography>
                        <Typography variant="body2">{forumStats.todayReplies}</Typography>
                      </Box>
                      <Box display="flex" justifyContent="space-between">
                        <Typography color="textSecondary">Avg Replies/Post:</Typography>
                        <Typography variant="body2">
                          {forumStats.averageRepliesPerPost.toFixed(1)}
                        </Typography>
                      </Box>
                      {forumStats.newestMember && (
                          <Box>
                            <Typography color="textSecondary" variant="body2" gutterBottom>
                              Newest Member:
                            </Typography>
                            <Box display="flex" alignItems="center" gap={1}>
                              <Avatar src={forumStats.newestMember.avatar}
                                      sx={{width: 32, height: 32}}>
                                {forumStats.newestMember.username[0].toUpperCase()}
                              </Avatar>
                              <Typography variant="body2">
                                {forumStats.newestMember.username}
                              </Typography>
                            </Box>
                          </Box>
                      )}
                    </Box>
                  </Box>
                </Paper>
            )}
          </Grid>
        </Grid>
      </Container>
  )
}

export default ForumHome