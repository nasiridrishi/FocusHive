import React, { useState } from 'react'
import {
  Card,
  CardContent,
  CardHeader,
  Typography,
  Box,
  Grid,
  Chip,
  IconButton,
  Collapse,
  Divider,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  LinearProgress,
  styled,
  alpha,
  useTheme,
} from '@mui/material'
import {
  ExpandMore as ExpandMoreIcon,
  Groups as GroupsIcon,
  Visibility as FocusingIcon,
  Coffee as BreakIcon,
  TrendingUp as TrendingUpIcon,
  Schedule as ScheduleIcon,
  Person as PersonIcon,
} from '@mui/icons-material'
import { ActiveUsersList } from './ActiveUsersList'
import { UserStatusBadge } from './UserStatusBadge'
import { HivePresenceInfo, ActivityEvent, UserPresence } from '../../../shared/types/presence'

const StyledCard = styled(Card)(({ theme }) => ({
  background: `linear-gradient(135deg, 
    ${alpha(theme.palette.primary.main, 0.05)} 0%, 
    ${alpha(theme.palette.background.paper, 0.95)} 100%)`,
  border: `1px solid ${alpha(theme.palette.primary.main, 0.1)}`,
  transition: 'all 0.3s ease-in-out',
  '&:hover': {
    transform: 'translateY(-2px)',
    boxShadow: theme.shadows[4],
  },
}))

const MetricCard = styled(Box)(({ theme }) => ({
  textAlign: 'center',
  padding: theme.spacing(2),
  borderRadius: theme.shape.borderRadius,
  background: alpha(theme.palette.background.paper, 0.8),
  border: `1px solid ${alpha(theme.palette.divider, 0.1)}`,
}))

const ExpandMoreStyled = styled(IconButton)<{ expand: boolean }>(({ theme, expand }) => ({
  transform: !expand ? 'rotate(0deg)' : 'rotate(180deg)',
  marginLeft: 'auto',
  transition: theme.transitions.create('transform', {
    duration: theme.transitions.duration.shortest,
  }),
}))

interface HivePresencePanelProps {
  hiveId: string
  presenceInfo: HivePresenceInfo
  recentActivity?: ActivityEvent[]
  showActivity?: boolean
  compact?: boolean
  onUserClick?: (user: UserPresence) => void
  className?: string
}

const HivePresencePanel: React.FC<HivePresencePanelProps> = ({
  hiveId,
  presenceInfo,
  recentActivity = [],
  showActivity = true,
  compact = false,
  onUserClick,
  className,
}) => {
  const theme = useTheme()
  const [expanded, setExpanded] = useState(!compact)

  const { activeUsers, totalOnline, totalFocusing, totalOnBreak } = presenceInfo

  // Calculate productivity metrics
  const totalActive = totalOnline + totalFocusing
  const focusRate = totalActive > 0 ? (totalFocusing / totalActive) * 100 : 0
  const breakRate = totalActive > 0 ? (totalOnBreak / totalActive) * 100 : 0

  // Sort users by status priority (focusing > online > break > away > offline)
  const sortedUsers = [...activeUsers].sort((a, b) => {
    const statusPriority = {
      focusing: 4,
      online: 3,
      break: 2,
      away: 1,
      offline: 0,
    }
    return (statusPriority[b.status] || 0) - (statusPriority[a.status] || 0)
  })

  const formatActivityTime = (timestamp: string) => {
    const date = new Date(timestamp)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffMinutes = Math.floor(diffMs / (1000 * 60))
    
    if (diffMinutes < 1) return 'Just now'
    if (diffMinutes < 60) return `${diffMinutes}m ago`
    
    const diffHours = Math.floor(diffMinutes / 60)
    if (diffHours < 24) return `${diffHours}h ago`
    
    return date.toLocaleDateString()
  }

  const getActivityIcon = (type: ActivityEvent['type']) => {
    switch (type) {
      case 'joined_hive':
        return <PersonIcon fontSize="small" color="success" />
      case 'left_hive':
        return <PersonIcon fontSize="small" color="error" />
      case 'started_session':
        return <FocusingIcon fontSize="small" color="primary" />
      case 'completed_session':
        return <TrendingUpIcon fontSize="small" color="primary" />
      case 'took_break':
        return <BreakIcon fontSize="small" color="warning" />
      case 'resumed_session':
        return <FocusingIcon fontSize="small" color="primary" />
      default:
        return <ScheduleIcon fontSize="small" />
    }
  }

  const getActivityMessage = (event: ActivityEvent) => {
    switch (event.type) {
      case 'joined_hive':
        return `${event.user.name} joined the hive`
      case 'left_hive':
        return `${event.user.name} left the hive`
      case 'started_session':
        return `${event.user.name} started a focus session`
      case 'completed_session':
        return `${event.user.name} completed a focus session`
      case 'took_break':
        return `${event.user.name} took a break`
      case 'resumed_session':
        return `${event.user.name} resumed focusing`
      default:
        return `${event.user.name} ${event.type.replace('_', ' ')}`
    }
  }

  const renderMetrics = () => (
    <Grid container spacing={2} sx={{ mb: 2 }}>
      <Grid item xs={3}>
        <MetricCard>
          <Typography variant="h6" color="primary">
            {totalActive}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Active
          </Typography>
        </MetricCard>
      </Grid>
      <Grid item xs={3}>
        <MetricCard>
          <Typography variant="h6" color="success.main">
            {totalFocusing}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Focusing
          </Typography>
        </MetricCard>
      </Grid>
      <Grid item xs={3}>
        <MetricCard>
          <Typography variant="h6" color="warning.main">
            {totalOnBreak}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            On Break
          </Typography>
        </MetricCard>
      </Grid>
      <Grid item xs={3}>
        <MetricCard>
          <Typography variant="h6" color="text.secondary">
            {Math.round(focusRate)}%
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Focus Rate
          </Typography>
        </MetricCard>
      </Grid>
    </Grid>
  )

  const renderFocusProgress = () => {
    if (totalActive === 0) return null

    return (
      <Box sx={{ mb: 2 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
          <Typography variant="body2" color="text.secondary">
            Hive Focus Activity
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {Math.round(focusRate)}%
          </Typography>
        </Box>
        <LinearProgress
          variant="determinate"
          value={focusRate}
          sx={{
            height: 8,
            borderRadius: 4,
            backgroundColor: alpha(theme.palette.grey[300], 0.3),
            '& .MuiLinearProgress-bar': {
              borderRadius: 4,
              background: `linear-gradient(45deg, ${theme.palette.primary.main}, ${theme.palette.success.main})`,
            },
          }}
        />
      </Box>
    )
  }

  const renderRecentActivity = () => {
    if (!showActivity || recentActivity.length === 0) return null

    return (
      <>
        <Divider sx={{ my: 2 }} />
        <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>
          Recent Activity
        </Typography>
        <List dense>
          {recentActivity.slice(0, 5).map((event) => (
            <ListItem key={event.id} sx={{ px: 0, py: 0.5 }}>
              <ListItemIcon sx={{ minWidth: 36 }}>
                {getActivityIcon(event.type)}
              </ListItemIcon>
              <ListItemText
                primary={getActivityMessage(event)}
                secondary={formatActivityTime(event.timestamp)}
                primaryTypographyProps={{ variant: 'body2' }}
                secondaryTypographyProps={{ variant: 'caption' }}
              />
            </ListItem>
          ))}
        </List>
      </>
    )
  }

  if (compact) {
    return (
      <Box className={className} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <GroupsIcon fontSize="small" color="action" />
        <Typography variant="body2" color="text.secondary">
          {totalActive} active
        </Typography>
        {totalFocusing > 0 && (
          <Chip
            label={`${totalFocusing} focusing`}
            size="small"
            color="primary"
            variant="outlined"
          />
        )}
      </Box>
    )
  }

  return (
    <StyledCard className={className}>
      <CardHeader
        title={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <GroupsIcon color="primary" />
            <Typography variant="h6">
              Hive Presence
            </Typography>
            {totalFocusing > 0 && (
              <Chip
                label={`${totalFocusing} focusing`}
                size="small"
                color="primary"
                variant="filled"
              />
            )}
          </Box>
        }
        action={
          <ExpandMoreStyled
            expand={expanded}
            onClick={() => setExpanded(!expanded)}
            aria-expanded={expanded}
            aria-label="show more"
          >
            <ExpandMoreIcon />
          </ExpandMoreStyled>
        }
        sx={{ pb: 1 }}
      />
      
      <CardContent sx={{ pt: 0 }}>
        {!expanded && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Typography variant="body2" color="text.secondary">
              {totalActive} users active
            </Typography>
            {renderFocusProgress()}
          </Box>
        )}
        
        <Collapse in={expanded} timeout="auto" unmountOnExit>
          {renderMetrics()}
          {renderFocusProgress()}
          
          <ActiveUsersList
            users={sortedUsers}
            maxVisible={8}
            showStatusSummary={false}
            onUserClick={onUserClick}
            title=""
          />
          
          {renderRecentActivity()}
        </Collapse>
      </CardContent>
    </StyledCard>
  )
}

export default React.memo(HivePresencePanel)