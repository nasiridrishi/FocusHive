import React, { useState, useMemo } from 'react'
import {
  Card,
  CardContent,
  CardHeader,
  Box,
  Typography,
  LinearProgress,
  Stack,
  Chip,
  IconButton,
  Avatar,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Divider,
  Paper,
  ToggleButton,
  ToggleButtonGroup,
} from '@mui/material'
import {
  Timer,
  Coffee,
  CheckCircle,
  RadioButtonUnchecked,
  TrendingUp,
  TrendingDown,
  Remove,
  People,
  Star,
  EmojiEvents,
  AccessTime,
  Today,
  CalendarMonth,
  AllInclusive,
} from '@mui/icons-material'
import { useTheme } from '@mui/material/styles'
import { useTimer } from '../contexts/TimerContext'
import { SessionStatsProps, DailyStats } from '../../../shared/types/timer'

// Mock data for demonstration
const generateMockDailyStats = (): DailyStats[] => {
  const stats: DailyStats[] = []
  const today = new Date()
  
  for (let i = 6; i >= 0; i--) {
    const date = new Date(today)
    date.setDate(date.getDate() - i)
    
    stats.push({
      date: date.toISOString().split('T')[0],
      focusTime: Math.floor(Math.random() * 240) + 60, // 60-300 minutes
      sessions: Math.floor(Math.random() * 8) + 1, // 1-8 sessions
      productivity: Math.floor(Math.random() * 5) + 1, // 1-5 rating
      goals: {
        completed: Math.floor(Math.random() * 5),
        total: Math.floor(Math.random() * 3) + 5, // 5-8 total goals
      }
    })
  }
  
  return stats
}

const StatCard: React.FC<{
  title: string
  value: string | number
  subtitle?: string
  icon: React.ReactNode
  color?: 'primary' | 'secondary' | 'success' | 'warning' | 'error'
  trend?: 'up' | 'down' | 'neutral'
  trendValue?: string
}> = ({ title, value, subtitle, icon, color = 'primary', trend, trendValue }) => {
  const theme = useTheme()
  
  const getTrendIcon = () => {
    switch (trend) {
      case 'up': return <TrendingUp fontSize="small" color="success" />
      case 'down': return <TrendingDown fontSize="small" color="error" />
      default: return <Remove fontSize="small" color="disabled" />
    }
  }

  return (
    <Paper sx={{ p: 2, height: '100%' }}>
      <Stack spacing={1}>
        <Stack direction="row" alignItems="center" spacing={1}>
          <Avatar 
            sx={{ 
              bgcolor: theme.palette[color].main, 
              width: 40, 
              height: 40 
            }}
          >
            {icon}
          </Avatar>
          <Box sx={{ flex: 1 }}>
            <Typography variant="body2" color="text.secondary">
              {title}
            </Typography>
            <Typography variant="h6" fontWeight="bold">
              {value}
            </Typography>
          </Box>
        </Stack>
        
        {(subtitle || trend) && (
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            {subtitle && (
              <Typography variant="caption" color="text.secondary">
                {subtitle}
              </Typography>
            )}
            {trend && trendValue && (
              <Stack direction="row" alignItems="center" spacing={0.5}>
                {getTrendIcon()}
                <Typography 
                  variant="caption" 
                  color={trend === 'up' ? 'success.main' : trend === 'down' ? 'error.main' : 'text.secondary'}
                >
                  {trendValue}
                </Typography>
              </Stack>
            )}
          </Stack>
        )}
      </Stack>
    </Paper>
  )
}

const GoalsList: React.FC<{
  goals: Array<{
    id: string
    description: string
    isCompleted: boolean
    priority: 'low' | 'medium' | 'high'
  }>
  onToggleGoal?: (goalId: string) => void
}> = ({ goals, onToggleGoal }) => {
  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'high': return 'error'
      case 'medium': return 'warning'
      case 'low': return 'success'
      default: return 'default'
    }
  }

  if (goals.length === 0) {
    return (
      <Typography variant="body2" color="text.secondary" textAlign="center" py={2}>
        No goals set for this session
      </Typography>
    )
  }

  return (
    <List dense>
      {goals.map((goal, index) => (
        <React.Fragment key={goal.id}>
          <ListItem
            sx={{ px: 0 }}
            secondaryAction={
              <Chip
                label={goal.priority}
                size="small"
                color={getPriorityColor(goal.priority) as 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning'}
                variant="outlined"
              />
            }
          >
            <ListItemIcon>
              <IconButton
                edge="start"
                onClick={() => onToggleGoal?.(goal.id)}
                size="small"
              >
                {goal.isCompleted ? (
                  <CheckCircle color="success" />
                ) : (
                  <RadioButtonUnchecked color="action" />
                )}
              </IconButton>
            </ListItemIcon>
            <ListItemText
              primary={goal.description}
              sx={{
                textDecoration: goal.isCompleted ? 'line-through' : 'none',
                opacity: goal.isCompleted ? 0.6 : 1,
              }}
            />
          </ListItem>
          {index < goals.length - 1 && <Divider />}
        </React.Fragment>
      ))}
    </List>
  )
}

export const SessionStats: React.FC<SessionStatsProps> = ({
  period = 'today',
  showGoals = true,
}) => {
  const { currentSession, timerState, timerSettings } = useTimer()
  const [selectedPeriod, setSelectedPeriod] = useState(period)
  
  // Mock data - in real app, this would come from API
  const dailyStats = useMemo(() => generateMockDailyStats(), [])
  
  // Calculate session progress
  const sessionProgress = useMemo(() => {
    if (!currentSession) return null
    
    const completedGoals = currentSession.goals.filter(g => g.isCompleted).length
    const totalGoals = currentSession.goals.length
    const cycleProgress = (timerState.currentCycle / currentSession.targetCycles) * 100
    
    return {
      goalProgress: totalGoals > 0 ? (completedGoals / totalGoals) * 100 : 0,
      cycleProgress,
      completedGoals,
      totalGoals,
    }
  }, [currentSession, timerState.currentCycle])

  // Calculate period statistics
  const periodStats = useMemo(() => {
    let relevantStats = dailyStats
    
    switch (selectedPeriod) {
      case 'today':
        relevantStats = dailyStats.slice(-1)
        break
      case 'week':
        relevantStats = dailyStats.slice(-7)
        break
      case 'month':
        relevantStats = dailyStats.slice(-30)
        break
      case 'all':
      default:
        relevantStats = dailyStats
        break
    }
    
    const totalFocusTime = relevantStats.reduce((sum, stat) => sum + stat.focusTime, 0)
    const totalSessions = relevantStats.reduce((sum, stat) => sum + stat.sessions, 0)
    const avgProductivity = relevantStats.length > 0 
      ? relevantStats.reduce((sum, stat) => sum + stat.productivity, 0) / relevantStats.length
      : 0
    const totalGoalsCompleted = relevantStats.reduce((sum, stat) => sum + stat.goals.completed, 0)
    const totalGoals = relevantStats.reduce((sum, stat) => sum + stat.goals.total, 0)
    
    return {
      totalFocusTime,
      totalSessions,
      avgProductivity,
      goalCompletionRate: totalGoals > 0 ? (totalGoalsCompleted / totalGoals) * 100 : 0,
      avgSessionLength: totalSessions > 0 ? totalFocusTime / totalSessions : 0,
    }
  }, [dailyStats, selectedPeriod])

  const formatTime = (minutes: number): string => {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    return hours > 0 ? `${hours}h ${mins}m` : `${mins}m`
  }

  const handlePeriodChange = (_: React.MouseEvent<HTMLElement>, newPeriod: string | null) => {
    if (newPeriod !== null) {
      setSelectedPeriod(newPeriod as typeof period)
    }
  }

  return (
    <Box>
      {/* Current Session Stats */}
      {currentSession && (
        <Card sx={{ mb: 3 }}>
          <CardHeader
            title="Current Session"
            subheader={`Started ${new Date(currentSession.date).toLocaleDateString()}`}
            avatar={
              <Avatar sx={{ bgcolor: 'primary.main' }}>
                <Timer />
              </Avatar>
            }
          />
          <CardContent>
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(4, 1fr)' }, gap: 3 }}>
              <Box>
                <StatCard
                  title="Current Cycle"
                  value={`${timerState.currentCycle}/${currentSession.targetCycles}`}
                  subtitle="Pomodoros"
                  icon={<Timer />}
                  color="primary"
                />
              </Box>
              <Box>
                <StatCard
                  title="Phase"
                  value={timerState.currentPhase.replace('-', ' ')}
                  subtitle={timerState.isRunning ? 'Running' : 'Paused'}
                  icon={timerState.currentPhase === 'focus' ? <Timer /> : <Coffee />}
                  color={timerState.currentPhase === 'focus' ? 'primary' : 'success'}
                />
              </Box>
              <Box>
                <StatCard
                  title="Goals Progress"
                  value={`${sessionProgress?.completedGoals || 0}/${sessionProgress?.totalGoals || 0}`}
                  subtitle={`${Math.round(sessionProgress?.goalProgress || 0)}% complete`}
                  icon={<CheckCircle />}
                  color="success"
                />
              </Box>
              <Box>
                <StatCard
                  title="Distractions"
                  value={currentSession.distractions}
                  subtitle="Recorded"
                  icon={<People />}
                  color="warning"
                />
              </Box>
            </Box>

            {/* Progress Bars */}
            <Box sx={{ mt: 3 }}>
              <Typography variant="subtitle2" gutterBottom>
                Session Progress
              </Typography>
              <Stack spacing={2}>
                <Box>
                  <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                    <Typography variant="body2">Cycle Progress</Typography>
                    <Typography variant="body2" color="text.secondary">
                      {Math.round(sessionProgress?.cycleProgress || 0)}%
                    </Typography>
                  </Stack>
                  <LinearProgress
                    variant="determinate"
                    value={sessionProgress?.cycleProgress || 0}
                    sx={{ height: 8, borderRadius: 4 }}
                  />
                </Box>
                <Box>
                  <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                    <Typography variant="body2">Goal Progress</Typography>
                    <Typography variant="body2" color="text.secondary">
                      {Math.round(sessionProgress?.goalProgress || 0)}%
                    </Typography>
                  </Stack>
                  <LinearProgress
                    variant="determinate"
                    value={sessionProgress?.goalProgress || 0}
                    color="success"
                    sx={{ height: 8, borderRadius: 4 }}
                  />
                </Box>
              </Stack>
            </Box>

            {/* Current Session Goals */}
            {showGoals && currentSession.goals.length > 0 && (
              <Box sx={{ mt: 3 }}>
                <Typography variant="subtitle2" gutterBottom>
                  Session Goals
                </Typography>
                <GoalsList goals={currentSession.goals} />
              </Box>
            )}
          </CardContent>
        </Card>
      )}

      {/* Period Statistics */}
      <Card>
        <CardHeader
          title="Productivity Statistics"
          action={
            <ToggleButtonGroup
              value={selectedPeriod}
              exclusive
              onChange={handlePeriodChange}
              size="small"
            >
              <ToggleButton value="today">
                <Today fontSize="small" />
              </ToggleButton>
              <ToggleButton value="week">
                <AccessTime fontSize="small" />
              </ToggleButton>
              <ToggleButton value="month">
                <CalendarMonth fontSize="small" />
              </ToggleButton>
              <ToggleButton value="all">
                <AllInclusive fontSize="small" />
              </ToggleButton>
            </ToggleButtonGroup>
          }
        />
        <CardContent>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(3, 1fr)' }, gap: 3 }}>
            <Box>
              <StatCard
                title="Total Focus Time"
                value={formatTime(periodStats.totalFocusTime)}
                subtitle={`${periodStats.totalSessions} sessions`}
                icon={<Timer />}
                color="primary"
                trend="up"
                trendValue="+15%"
              />
            </Box>
            <Box>
              <StatCard
                title="Avg Session Length"
                value={formatTime(Math.round(periodStats.avgSessionLength))}
                subtitle="Per session"
                icon={<AccessTime />}
                color="secondary"
                trend="up"
                trendValue="+5%"
              />
            </Box>
            <Box>
              <StatCard
                title="Productivity Rating"
                value={`${periodStats.avgProductivity.toFixed(1)}/5`}
                subtitle="Average"
                icon={<Star />}
                color="success"
                trend="neutral"
              />
            </Box>
            <Box>
              <StatCard
                title="Goal Completion"
                value={`${Math.round(periodStats.goalCompletionRate)}%`}
                subtitle="Success rate"
                icon={<EmojiEvents />}
                color="warning"
                trend="up"
                trendValue="+8%"
              />
            </Box>
            <Box>
              <StatCard
                title="Focus Sessions"
                value={periodStats.totalSessions}
                subtitle={`${selectedPeriod === 'today' ? 'Today' : 'Total'}`}
                icon={<CheckCircle />}
                color="primary"
                trend="up"
                trendValue="+3"
              />
            </Box>
            <Box>
              <StatCard
                title="Settings"
                value={`${timerSettings.focusLength}m`}
                subtitle="Focus length"
                icon={<Timer />}
                color="secondary"
              />
            </Box>
          </Box>
        </CardContent>
      </Card>
    </Box>
  )
}

export default SessionStats