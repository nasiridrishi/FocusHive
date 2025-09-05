import React from 'react'
import {
  Box,
  Card,
  CardContent,
  Typography,
  
  Chip,
  Avatar,
  List,
  ListItem,
  ListItemText,
  ListItemAvatar,
  ListItemSecondaryAction,
  IconButton,
  Paper,
  Grid,
} from '@mui/material'

// Grid component type workaround
import {
  ArrowForward as ArrowForwardIcon,
  Lock as LockIcon,
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon,
  Article as ArticleIcon,
  Reply as ReplyIcon,
  Schedule as ScheduleIcon,
  Category as CategoryIcon
} from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { ForumCategory } from '../types'

interface ForumCategoryListProps {
  categories: ForumCategory[]
  showAll?: boolean
  compact?: boolean
}

const ForumCategoryList: React.FC<ForumCategoryListProps> = ({
  categories,
  showAll: _showAll = true,
  compact = false
}) => {
  const navigate = useNavigate()

  const handleCategoryClick = (category: ForumCategory) => {
    navigate(`/forum/categories/${category.slug}`)
  }

  const formatNumber = (num: number): string => {
    if (num >= 1000000) return `${(num / 1000000).toFixed(1)}M`
    if (num >= 1000) return `${(num / 1000).toFixed(1)}K`
    return num.toString()
  }

  const formatTimeAgo = (dateString?: string): string => {
    if (!dateString) return 'No activity'
    
    const date = new Date(dateString)
    const now = new Date()
    const diffInHours = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60))
    
    if (diffInHours < 1) return 'Just now'
    if (diffInHours < 24) return `${diffInHours}h ago`
    if (diffInHours < 168) return `${Math.floor(diffInHours / 24)}d ago`
    return date.toLocaleDateString()
  }

  const getCategoryColor = (color?: string): string => {
    return color || '#1976d2'
  }

  const getCategoryIcon = (icon?: string, color?: string) => {
    // For now, we'll use a default icon, but this could be extended to support custom icons
    return <CategoryIcon sx={{ color: getCategoryColor(color) }} />
  }

  const getAccessIcon = (category: ForumCategory) => {
    if (category.isLocked) {
      return <LockIcon color="error" fontSize="small" />
    }
    if (category.isPrivate) {
      return <VisibilityOffIcon color="warning" fontSize="small" />
    }
    return <VisibilityIcon color="success" fontSize="small" />
  }

  if (compact) {
    return (
      <List>
        {categories.map((category) => (
          <ListItem
            key={category.id}
            button
            onClick={() => handleCategoryClick(category)}
            disabled={category.isPrivate || category.isLocked}
          >
            <ListItemAvatar>
              <Avatar sx={{ bgcolor: getCategoryColor(category.color) }}>
                {getCategoryIcon(category.icon, category.color)}
              </Avatar>
            </ListItemAvatar>
            <ListItemText
              primary={
                <Box display="flex" alignItems="center" gap={1}>
                  <Typography variant="subtitle1">{category.name}</Typography>
                  {getAccessIcon(category)}
                </Box>
              }
              secondary={
                <Box>
                  <Typography variant="body2" color="textSecondary" gutterBottom>
                    {category.description}
                  </Typography>
                  <Box display="flex" alignItems="center" gap={2}>
                    <Box display="flex" alignItems="center" gap={0.5}>
                      <ArticleIcon fontSize="small" color="action" />
                      <Typography variant="caption">
                        {formatNumber(category.postCount)}
                      </Typography>
                    </Box>
                    <Box display="flex" alignItems="center" gap={0.5}>
                      <ReplyIcon fontSize="small" color="action" />
                      <Typography variant="caption">
                        {formatNumber(category.topicCount)}
                      </Typography>
                    </Box>
                  </Box>
                </Box>
              }
            />
            <ListItemSecondaryAction>
              <IconButton edge="end" onClick={() => handleCategoryClick(category)}>
                <ArrowForwardIcon />
              </IconButton>
            </ListItemSecondaryAction>
          </ListItem>
        ))}
      </List>
    )
  }

  return (
    <Grid container spacing={3}>
      {categories.map((category) => (
        <Grid item key={category.id}>
          <Card 
            sx={{ 
              cursor: category.isPrivate || category.isLocked ? 'not-allowed' : 'pointer',
              opacity: category.isPrivate || category.isLocked ? 0.7 : 1,
              '&:hover': {
                boxShadow: category.isPrivate || category.isLocked ? 'none' : 4,
              }
            }}
            onClick={() => !category.isPrivate && !category.isLocked && handleCategoryClick(category)}
          >
            <CardContent>
              <Box display="flex" alignItems="flex-start" gap={2} mb={2}>
                <Avatar 
                  sx={{ 
                    bgcolor: getCategoryColor(category.color),
                    width: 48,
                    height: 48 
                  }}
                >
                  {getCategoryIcon(category.icon, category.color)}
                </Avatar>
                <Box flexGrow={1}>
                  <Box display="flex" alignItems="center" gap={1} mb={1}>
                    <Typography variant="h6" component="h3">
                      {category.name}
                    </Typography>
                    {getAccessIcon(category)}
                  </Box>
                  <Typography variant="body2" color="textSecondary" paragraph>
                    {category.description}
                  </Typography>
                </Box>
              </Box>

              {/* Category Stats */}
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Box display="flex" gap={3}>
                  <Box display="flex" alignItems="center" gap={0.5}>
                    <ArticleIcon fontSize="small" color="action" />
                    <Typography variant="body2" color="textSecondary">
                      {formatNumber(category.postCount)} posts
                    </Typography>
                  </Box>
                  <Box display="flex" alignItems="center" gap={0.5}>
                    <ReplyIcon fontSize="small" color="action" />
                    <Typography variant="body2" color="textSecondary">
                      {formatNumber(category.topicCount)} topics
                    </Typography>
                  </Box>
                </Box>
                
                {category.lastActivity && (
                  <Box display="flex" alignItems="center" gap={0.5}>
                    <ScheduleIcon fontSize="small" color="action" />
                    <Typography variant="caption" color="textSecondary">
                      {formatTimeAgo(category.lastActivity)}
                    </Typography>
                  </Box>
                )}
              </Box>

              {/* Last Post Info */}
              {category.lastPost && (
                <Paper variant="outlined" sx={{ p: 2 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    Latest Post
                  </Typography>
                  <Box display="flex" alignItems="center" gap={1}>
                    <Avatar 
                      src={category.lastPost.author.avatar}
                      sx={{ width: 24, height: 24 }}
                    >
                      {category.lastPost.author.username[0].toUpperCase()}
                    </Avatar>
                    <Typography variant="body2" color="textSecondary">
                      <strong>{category.lastPost.title}</strong>
                      {' '}by {category.lastPost.author.username}
                    </Typography>
                  </Box>
                </Paper>
              )}

              {/* Moderators */}
              {category.moderators && category.moderators.length > 0 && (
                <Box mt={2}>
                  <Typography variant="caption" color="textSecondary" gutterBottom display="block">
                    Moderators:
                  </Typography>
                  <Box display="flex" gap={1} flexWrap="wrap">
                    {category.moderators.slice(0, 3).map((moderator) => (
                      <Chip
                        key={moderator.id}
                        size="small"
                        avatar={
                          <Avatar src={moderator.avatar}>
                            {moderator.username[0].toUpperCase()}
                          </Avatar>
                        }
                        label={moderator.username}
                        variant="outlined"
                      />
                    ))}
                    {category.moderators.length > 3 && (
                      <Chip
                        size="small"
                        label={`+${category.moderators.length - 3} more`}
                        variant="outlined"
                      />
                    )}
                  </Box>
                </Box>
              )}

              {/* Subcategories */}
              {category.subcategories && category.subcategories.length > 0 && (
                <Box mt={2}>
                  <Typography variant="caption" color="textSecondary" gutterBottom display="block">
                    Subcategories:
                  </Typography>
                  <Box display="flex" gap={1} flexWrap="wrap">
                    {category.subcategories.slice(0, 4).map((subcategory) => (
                      <Chip
                        key={subcategory.id}
                        size="small"
                        label={subcategory.name}
                        clickable
                        onClick={(e) => {
                          e.stopPropagation()
                          if (!subcategory.isPrivate && !subcategory.isLocked) {
                            handleCategoryClick(subcategory)
                          }
                        }}
                        disabled={subcategory.isPrivate || subcategory.isLocked}
                      />
                    ))}
                    {category.subcategories.length > 4 && (
                      <Chip
                        size="small"
                        label={`+${category.subcategories.length - 4} more`}
                        variant="outlined"
                      />
                    )}
                  </Box>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
      ))}
    </Grid>
  )
}

export default ForumCategoryList