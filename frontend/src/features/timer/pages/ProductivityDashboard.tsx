import React, { useState, useEffect } from 'react'
import {
  Container,
  Box,
  Typography,
  Fab,
  Drawer,
  IconButton,
  AppBar,
  Toolbar,
  Stack,
  Card,
  CardContent,
  CardHeader,
  Chip,
  Avatar,
  Alert,
  Collapse,
  useMediaQuery,
} from '@mui/material'
import {
  Timer,
  Analytics,
  Settings,
  Close,
  Menu,
  Fullscreen,
  FullscreenExit,
  Notifications,
  Share,
  Download,
  TrendingUp,
  EmojiEvents,
  Today,
} from '@mui/icons-material'
import { useTheme } from '@mui/material/styles'
import { useTimer } from '../contexts/TimerContext'
import { usePresence } from '../../../shared/contexts/PresenceContext'
import FocusTimer from '../components/FocusTimer'
import SessionStats from '../components/SessionStats'
import ProductivityChart from '../components/ProductivityChart'

interface QuickStatsCardProps {
  title: string
  value: string | number
  change?: string
  changeType?: 'positive' | 'negative' | 'neutral'
  icon: React.ReactNode
  color?: 'primary' | 'secondary' | 'success' | 'warning' | 'error'
}

const QuickStatsCard: React.FC<QuickStatsCardProps> = ({
  title,
  value,
  change,
  changeType = 'neutral',
  icon,
  color = 'primary'
}) => {
  const theme = useTheme()
  
  const getChangeColor = () => {
    switch (changeType) {
      case 'positive': return theme.palette.success.main
      case 'negative': return theme.palette.error.main
      default: return theme.palette.text.secondary
    }
  }

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Stack direction="row" spacing={2} alignItems="center">
          <Avatar sx={{ bgcolor: theme.palette[color].main }}>
            {icon}
          </Avatar>
          <Box sx={{ flex: 1 }}>
            <Typography variant="body2" color="text.secondary">
              {title}
            </Typography>
            <Typography variant="h5" fontWeight="bold">
              {value}
            </Typography>
            {change && (
              <Typography 
                variant="caption" 
                sx={{ color: getChangeColor() }}
              >
                {change}
              </Typography>
            )}
          </Box>
        </Stack>
      </CardContent>
    </Card>
  )
}

const AchievementsBanner: React.FC = () => {
  const [showAchievements, setShowAchievements] = useState(false)
  const { currentSession, timerState } = useTimer()
  
  // Mock achievements data
  const recentAchievements = [
    { id: 1, title: 'Focus Master', description: 'Completed 5 focus sessions today', icon: <Timer /> },
    { id: 2, title: 'Goal Crusher', description: 'Achieved 100% goal completion', icon: <EmojiEvents /> },
    { id: 3, title: 'Streak Keeper', description: '7-day focus streak', icon: <TrendingUp /> },
  ]

  useEffect(() => {
    // Show achievements banner when session milestones are reached
    if (currentSession && timerState.currentCycle > 0 && timerState.currentCycle % 4 === 0) {
      setShowAchievements(true)
      // Auto-hide after 5 seconds
      const timer = setTimeout(() => setShowAchievements(false), 5000)
      return () => clearTimeout(timer)
    }
  }, [currentSession, timerState.currentCycle])

  if (recentAchievements.length === 0) return null

  return (
    <Collapse in={showAchievements}>
      <Alert
        severity="success"
        icon={<EmojiEvents />}
        action={
          <IconButton
            size="small"
            onClick={() => setShowAchievements(false)}
          >
            <Close />
          </IconButton>
        }
        sx={{ mb: 2 }}
      >
        <Typography variant="subtitle2" gutterBottom>
          New Achievement Unlocked!
        </Typography>
        <Stack direction="row" spacing={1}>
          {recentAchievements.slice(0, 2).map((achievement) => (
            <Chip
              key={achievement.id}
              label={achievement.title}
              size="small"
              color="success"
              variant="outlined"
            />
          ))}
        </Stack>
      </Alert>
    </Collapse>
  )
}

const ProductivityDashboard: React.FC = () => {
  const theme = useTheme()
  const isMobile = useMediaQuery('(max-width: 960px)')
  const { currentSession, timerState } = useTimer()
  const { currentPresence } = usePresence()
  
  const [sidebarOpen, setSidebarOpen] = useState(!isMobile)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [selectedView, setSelectedView] = useState<'overview' | 'timer' | 'analytics'>('overview')

  // Mock data for quick stats
  const quickStats = [
    {
      title: 'Today\'s Focus',
      value: '4h 25m',
      change: '+15% from yesterday',
      changeType: 'positive' as const,
      icon: <Timer />,
      color: 'primary' as const,
    },
    {
      title: 'Sessions Completed',
      value: 12,
      change: '+3 from yesterday',
      changeType: 'positive' as const,
      icon: <EmojiEvents />,
      color: 'success' as const,
    },
    {
      title: 'Current Streak',
      value: '7 days',
      change: 'Personal best!',
      changeType: 'positive' as const,
      icon: <TrendingUp />,
      color: 'warning' as const,
    },
    {
      title: 'Productivity Score',
      value: '4.2/5',
      change: '+0.3 this week',
      changeType: 'positive' as const,
      icon: <Analytics />,
      color: 'secondary' as const,
    },
  ]

  const toggleSidebar = () => {
    setSidebarOpen(!sidebarOpen)
  }

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen()
      setIsFullscreen(true)
    } else {
      document.exitFullscreen()
      setIsFullscreen(false)
    }
  }

  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement)
    }

    document.addEventListener('fullscreenchange', handleFullscreenChange)
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange)
  }, [])

  const renderMainContent = () => {
    switch (selectedView) {
      case 'timer':
        return (
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '2fr 1fr' }, gap: 3 }}>
            <Box>
              <FocusTimer 
                hiveId={currentPresence?.hiveId}
                showSettings={true}
                compact={false}
              />
            </Box>
            <Box>
              <SessionStats
                period="today"
                showCharts={false}
                showGoals={true}
              />
            </Box>
          </Box>
        )
      
      case 'analytics':
        return (
          <Box sx={{ display: 'grid', gridTemplateColumns: '1fr', gap: 3 }}>
            <Box>
              <ProductivityChart
                data={{ labels: [], datasets: [] }}
                type="line"
                height={400}
                showLegend={true}
                responsive={true}
              />
            </Box>
            <Box>
              <SessionStats
                period="week"
                showCharts={true}
                showGoals={false}
              />
            </Box>
          </Box>
        )
      
      default: // overview
        return (
          <Box sx={{ display: 'grid', gridTemplateColumns: '1fr', gap: 3 }}>
            {/* Quick Stats */}
            <Box>
              <Typography variant="h6" gutterBottom>
                Today's Overview
              </Typography>
              <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(4, 1fr)' }, gap: 2 }}>
                {quickStats.map((stat, index) => (
                  <Box key={index}>
                    <QuickStatsCard {...stat} />
                  </Box>
                ))}
              </Box>
            </Box>

            {/* Timer and Current Session */}
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' }, gap: 3 }}>
              <Box>
                <FocusTimer 
                  hiveId={currentPresence?.hiveId}
                  showSettings={false}
                  compact={true}
                />
              </Box>

              {/* Session Stats */}
              <Box>
                <SessionStats
                  period="today"
                  showCharts={false}
                  showGoals={true}
                />
              </Box>
            </Box>

            {/* Charts */}
            <Box>
              <ProductivityChart
                data={{ labels: [], datasets: [] }}
                type="line"
                height={300}
                showLegend={true}
                responsive={true}
              />
            </Box>
          </Box>
        )
    }
  }

  const sidebarContent = (
    <Box sx={{ width: 280, p: 2 }}>
      <Stack spacing={2}>
        <Typography variant="h6" gutterBottom>
          Productivity Dashboard
        </Typography>
        
        {/* View Selector */}
        <Stack spacing={1}>
          <Typography variant="subtitle2" color="text.secondary">
            Views
          </Typography>
          {[
            { key: 'overview', label: 'Overview', icon: <Today /> },
            { key: 'timer', label: 'Timer', icon: <Timer /> },
            { key: 'analytics', label: 'Analytics', icon: <Analytics /> },
          ].map((view) => (
            <Card
              key={view.key}
              sx={{
                cursor: 'pointer',
                border: selectedView === view.key ? 2 : 1,
                borderColor: selectedView === view.key ? 'primary.main' : 'divider',
                bgcolor: selectedView === view.key ? 'primary.50' : 'background.paper',
              }}
              onClick={() => setSelectedView(view.key as 'overview' | 'timer' | 'analytics')}
            >
              <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
                <Stack direction="row" spacing={2} alignItems="center">
                  {view.icon}
                  <Typography variant="body2">
                    {view.label}
                  </Typography>
                </Stack>
              </CardContent>
            </Card>
          ))}
        </Stack>

        {/* Current Session Info */}
        {currentSession && (
          <Card>
            <CardHeader
              title="Active Session"
              titleTypographyProps={{ variant: 'subtitle2' }}
              avatar={
                <Avatar sx={{ bgcolor: 'primary.main', width: 32, height: 32 }}>
                  <Timer fontSize="small" />
                </Avatar>
              }
            />
            <CardContent sx={{ pt: 0 }}>
              <Stack spacing={1}>
                <Chip
                  label={`Cycle ${timerState.currentCycle}/${currentSession.targetCycles}`}
                  size="small"
                  color="primary"
                />
                <Chip
                  label={`${currentSession.goals.filter(g => g.isCompleted).length}/${currentSession.goals.length} goals`}
                  size="small"
                  color="success"
                  variant="outlined"
                />
                <Chip
                  label={`${currentSession.distractions} distractions`}
                  size="small"
                  color="warning"
                  variant="outlined"
                />
              </Stack>
            </CardContent>
          </Card>
        )}

        {/* Quick Actions */}
        <Stack spacing={1}>
          <Typography variant="subtitle2" color="text.secondary">
            Quick Actions
          </Typography>
          <Stack spacing={1}>
            <Chip
              icon={<Download />}
              label="Export Data"
              clickable
              variant="outlined"
              size="small"
            />
            <Chip
              icon={<Share />}
              label="Share Progress"
              clickable
              variant="outlined"
              size="small"
            />
            <Chip
              icon={<Settings />}
              label="Timer Settings"
              clickable
              variant="outlined"
              size="small"
            />
          </Stack>
        </Stack>
      </Stack>
    </Box>
  )

  return (
    <Box sx={{ display: 'flex', height: '100vh' }}>
      {/* AppBar */}
      <AppBar 
        position="fixed" 
        sx={{ 
          zIndex: theme.zIndex.drawer + 1,
          bgcolor: 'background.paper',
          color: 'text.primary',
          boxShadow: 1,
        }}
      >
        <Toolbar>
          <IconButton
            edge="start"
            onClick={toggleSidebar}
            sx={{ mr: 2 }}
          >
            <Menu />
          </IconButton>
          
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            Productivity Dashboard
          </Typography>

          <Stack direction="row" spacing={1}>
            <IconButton onClick={toggleFullscreen}>
              {isFullscreen ? <FullscreenExit /> : <Fullscreen />}
            </IconButton>
            <IconButton>
              <Notifications />
            </IconButton>
          </Stack>
        </Toolbar>
      </AppBar>

      {/* Sidebar */}
      <Drawer
        variant={isMobile ? 'temporary' : 'persistent'}
        open={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        sx={{
          '& .MuiDrawer-paper': {
            width: 280,
            mt: isMobile ? 0 : 8,
            height: isMobile ? '100%' : 'calc(100% - 64px)',
          },
        }}
      >
        {sidebarContent}
      </Drawer>

      {/* Main Content */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          mt: 8,
          ml: !isMobile && sidebarOpen ? 0 : 0,
          transition: theme.transitions.create(['margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
          }),
        }}
      >
        <Container maxWidth={false} sx={{ py: 3 }}>
          <AchievementsBanner />
          {renderMainContent()}
        </Container>
      </Box>

      {/* Floating Timer for Mobile */}
      {isMobile && !currentSession && (
        <Fab
          color="primary"
          sx={{
            position: 'fixed',
            bottom: 16,
            right: 16,
          }}
          onClick={() => setSelectedView('timer')}
        >
          <Timer />
        </Fab>
      )}
    </Box>
  )
}

export default ProductivityDashboard