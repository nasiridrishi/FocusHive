import React from 'react'
import {
  Box,
  Typography,
  Paper,
  Card,
  CardContent,
  Button,
  Chip,
  Avatar,
  IconButton,
  Skeleton
} from '@mui/material'
import Grid from '../../../components/ui/Grid'
import {
  TrendingUp,
  Timer,
  Group,
  EmojiEvents,
  Schedule,
  Person,
  Star,
  Refresh
} from '@mui/icons-material'
import { useAuth } from '@/hooks/useAuth'
import { useDashboardStats } from '../hooks/useDashboardStats'
import { useActivityFeed } from '../hooks/useActivityFeed'
import { useRecentHives } from '../../hive/hooks/useRecentHives'
import { useUpcomingSessions } from '../../timer/hooks/useUpcomingSessions'

const DashboardPage: React.FC = () => {
  const { user } = useAuth()
  const { stats, isLoading: statsLoading, error: statsError } = useDashboardStats()
  const { activities, isLoading: activitiesLoading } = useActivityFeed()
  const { hives, isLoading: hivesLoading } = useRecentHives()
  const { sessions, isLoading: sessionsLoading } = useUpcomingSessions()

  const formatTime = (minutes: number) => {
    if (minutes < 60) return `${minutes} min`
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`
  }

  const formatDate = (date: Date) => {
    return new Intl.DateTimeFormat('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date)
  }

  const formatRelativeTime = (timestamp: string) => {
    const date = new Date(timestamp)
    const now = new Date()
    const diff = now.getTime() - date.getTime()
    const hours = Math.floor(diff / (1000 * 60 * 60))

    if (hours < 1) {
      const minutes = Math.floor(diff / (1000 * 60))
      return `${minutes} minutes ago`
    }
    if (hours < 24) {
      return `${hours} hours ago`
    }
    const days = Math.floor(hours / 24)
    return `${days} days ago`
  }

  return (
    <Box component="main" role="main" data-testid="dashboard-page" sx={{ p: 3 }}>
      {/* Welcome Section */}
      <Box data-testid="welcome-section" sx={{ mb: 4 }}>
        <Typography variant="h1" sx={{ fontSize: '2rem' }} gutterBottom>
          Welcome back, {user ? `${user.firstName} ${user.lastName}`.trim() || user.username || 'User' : 'User'}
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Typography variant="body1" color="text.secondary">
            {(user && 'persona' in user && (user as any).persona as string) || 'Professional'}
          </Typography>
          <Typography data-testid="current-date" variant="body2" color="text.secondary">
            {formatDate(new Date())}
          </Typography>
        </Box>
      </Box>

      {/* Stats Section */}
      <Box data-testid="stats-section" sx={{ mb: 4 }}>
        <Grid container spacing={3}>
          {/* Today's Focus */}
          <Grid xs={12} sm={6} md={3}>
            <Card data-testid="stats-card">
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                  <Timer data-testid="stats-icon" color="primary" />
                </Box>
                <Typography data-testid="stats-title" variant="subtitle2" color="text.secondary">
                  Today's Focus
                </Typography>
                <Typography data-testid="stats-value" variant="h4">
                  {formatTime(stats?.todaysFocus || 0)}
                </Typography>
                <Box data-testid="stats-color" sx={{ display: 'none' }}>primary</Box>
              </CardContent>
            </Card>
          </Grid>

          {/* Weekly Streak */}
          <Grid xs={12} sm={6} md={3}>
            <Card data-testid="stats-card">
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                  <TrendingUp data-testid="stats-icon" color="success" />
                </Box>
                <Typography data-testid="stats-title" variant="subtitle2" color="text.secondary">
                  Weekly Streak
                </Typography>
                <Typography data-testid="stats-value" variant="h4">
                  {stats?.weeklyStreak || 0} days
                </Typography>
                <Box data-testid="stats-color" sx={{ display: 'none' }}>success</Box>
              </CardContent>
            </Card>
          </Grid>

          {/* Hives Joined */}
          <Grid xs={12} sm={6} md={3}>
            <Card data-testid="stats-card">
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                  <Group data-testid="stats-icon" color="info" />
                </Box>
                <Typography data-testid="stats-title" variant="subtitle2" color="text.secondary">
                  Hives Joined
                </Typography>
                <Typography data-testid="stats-value" variant="h4">
                  {stats?.hivesJoined || 0}
                </Typography>
                <Box data-testid="stats-color" sx={{ display: 'none' }}>info</Box>
              </CardContent>
            </Card>
          </Grid>

          {/* Productivity Score */}
          <Grid xs={12} sm={6} md={3}>
            <Card data-testid="stats-card">
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                  <EmojiEvents data-testid="stats-icon" color="warning" />
                </Box>
                <Typography data-testid="stats-title" variant="subtitle2" color="text.secondary">
                  Productivity Score
                </Typography>
                <Typography data-testid="stats-value" variant="h4">
                  {stats?.productivityScore || 0}%
                </Typography>
                <Box data-testid="stats-color" sx={{ display: 'none' }}>warning</Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Box>

      {/* Recent Hives Section */}
      <Box data-testid="recent-hives-section" sx={{ mb: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h2" sx={{ fontSize: '1.5rem' }}>Recent Hives</Typography>
          <Button data-testid="view-all-hives" variant="text">
            View All
          </Button>
        </Box>
        <Grid container spacing={2}>
          {hivesLoading ? (
            <>
              <Grid xs={12} md={6}>
                <Skeleton data-testid="skeleton-loader" variant="rectangular" height={100} />
              </Grid>
              <Grid xs={12} md={6}>
                <Skeleton data-testid="skeleton-loader" variant="rectangular" height={100} />
              </Grid>
            </>
          ) : (
            hives?.map((hive) => (
              <Grid xs={12} md={6} key={hive.id}>
                <Card data-testid="hive-card" sx={{ cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }}>
                  <CardContent>
                    <Typography variant="h6">{hive.name}</Typography>
                    <Typography variant="body2" color="text.secondary">
                      {hive.memberCount} members
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Last active: {formatRelativeTime(hive.lastActive)}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))
          )}
        </Grid>
      </Box>

      {/* Upcoming Sessions Section */}
      <Box data-testid="upcoming-sessions-section" sx={{ mb: 4 }}>
        <Typography variant="h2" sx={{ fontSize: '1.5rem' }} gutterBottom>Upcoming Sessions</Typography>
        <Grid container spacing={2}>
          {sessionsLoading ? (
            <>
              <Grid xs={12} md={6}>
                <Skeleton data-testid="skeleton-loader" variant="rectangular" height={100} />
              </Grid>
              <Grid xs={12} md={6}>
                <Skeleton data-testid="skeleton-loader" variant="rectangular" height={100} />
              </Grid>
            </>
          ) : (
            sessions?.map((session) => (
              <Grid xs={12} md={6} key={session.id}>
                <Card data-testid="session-card">
                  <CardContent sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Box>
                      <Typography variant="h6">{session.title}</Typography>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
                        <Schedule fontSize="small" />
                        <Typography variant="body2">{session.duration} minutes</Typography>
                      </Box>
                      <Typography variant="caption" color="text.secondary">
                        {new Date(session.startTime).toLocaleTimeString()}
                      </Typography>
                    </Box>
                    <Button data-testid="join-session-button" variant="contained" size="small">
                      Join
                    </Button>
                  </CardContent>
                </Card>
              </Grid>
            ))
          )}
        </Grid>
      </Box>

      {/* Activity Feed Section */}
      <Box data-testid="activity-feed-section">
        <Typography variant="h2" sx={{ fontSize: '1.5rem' }} gutterBottom>Recent Activity</Typography>
        {activitiesLoading ? (
          <Box>
            <Skeleton data-testid="skeleton-loader" variant="text" height={60} />
            <Skeleton data-testid="skeleton-loader" variant="text" height={60} />
            <Skeleton data-testid="skeleton-loader" variant="text" height={60} />
          </Box>
        ) : statsError ? (
          <Typography color="error">Error loading dashboard data</Typography>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {activities?.map((activity) => (
              <Paper
                key={activity.id}
                data-testid="activity-item"
                sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 2 }}
              >
                <Box data-testid="activity-icon">
                  {activity.icon === 'timer' && <Timer />}
                  {activity.icon === 'group' && <Group />}
                  {activity.icon === 'trophy' && <EmojiEvents />}
                </Box>
                <Box sx={{ flex: 1 }}>
                  <Typography variant="body1">{activity.title}</Typography>
                  <Typography data-testid="activity-timestamp" variant="caption" color="text.secondary">
                    {formatRelativeTime(activity.timestamp)}
                  </Typography>
                </Box>
              </Paper>
            ))}
          </Box>
        )}
      </Box>

      {/* Quick Actions */}
      <Box sx={{ position: 'fixed', bottom: 20, right: 20, display: 'none' }}>
        <IconButton data-testid="refresh-dashboard" color="primary">
          <Refresh />
        </IconButton>
      </Box>
    </Box>
  )
}

export default DashboardPage