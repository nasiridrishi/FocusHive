import React, {useEffect, useState} from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  Card,
  CardContent,
  Chip,
  LinearProgress,
  Paper,
  Stack,
  Typography,
  Alert,
  Skeleton,
} from '@mui/material'
import {
  People as PeopleIcon,
  Rocket as RocketIcon,
  Star as StarIcon,
  Timer as TimerIcon,
  TrendingUp as TrendingUpIcon,
} from '@mui/icons-material'
import {HiveList} from '../components'
import {Hive, HiveMember} from '@shared/types'
import { useAuth } from '../../../features/auth/hooks/useAuth'

// Mock data - this would come from API calls in a real app
const mockHives: Hive[] = [
  {
    id: '1',
    name: 'Study Group',
    description: 'A collaborative space for focused studying and academic work. Perfect for students and lifelong learners.',
    ownerId: 'user1',
    owner: {
      id: 'user1',
      email: 'alice@example.com',
      username: 'alice_j',
      firstName: 'Alice',
      lastName: 'Johnson',
      name: 'Alice Johnson',
      profilePicture: undefined,
      isEmailVerified: true,
      isVerified: true,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
    },
    maxMembers: 20,
    isPublic: true,
    tags: ['Study', 'Academic', 'Learning'],
    settings: {
      allowChat: true,
      allowVoice: false,
      requireApproval: false,
      focusMode: 'POMODORO',
      defaultSessionLength: 25,
      maxSessionLength: 120,
      privacyLevel: 'PUBLIC',
      category: 'STUDY',
      maxParticipants: 20
    },
    currentMembers: 12,
    createdAt: '2024-01-15T10:00:00Z',
    updatedAt: '2024-01-20T15:30:00Z',
    memberCount: 15,
    isOwner: true,
    isMember: true,
  },
  {
    id: '2',
    name: 'Work Project',
    description: 'Deep focus sessions for important work projects. A private space for our team to maintain accountability.',
    ownerId: 'user2',
    owner: {
      id: 'user2',
      email: 'bob@example.com',
      username: 'bob_s',
      firstName: 'Bob',
      lastName: 'Smith',
      name: 'Bob Smith',
      profilePicture: undefined,
      isEmailVerified: true,
      isVerified: true,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
    },
    maxMembers: 15,
    isPublic: false,
    tags: ['Work', 'Project', 'Team'],
    settings: {
      allowChat: true,
      allowVoice: false,
      requireApproval: true,
      focusMode: 'TIMEBLOCK',
      defaultSessionLength: 60,
      maxSessionLength: 180,
      privacyLevel: 'PRIVATE',
      category: 'WORK',
      maxParticipants: 10
    },
    currentMembers: 8,
    createdAt: '2024-01-10T14:00:00Z',
    updatedAt: '2024-01-22T09:15:00Z',
    memberCount: 25,
    isOwner: false,
    isMember: true,
  },
  {
    id: '3',
    name: 'Reading Club',
    description: 'Join fellow book lovers for quiet reading sessions. Share your progress and discover new books.',
    ownerId: 'user3',
    owner: {
      id: 'user3',
      email: 'carol@example.com',
      username: 'carol_w',
      firstName: 'Carol',
      lastName: 'Williams',
      name: 'Carol Williams',
      profilePicture: undefined,
      isEmailVerified: true,
      isVerified: true,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
    },
    maxMembers: 30,
    isPublic: true,
    tags: ['Reading', 'Books', 'Literature'],
    settings: {
      allowChat: true,
      allowVoice: false,
      requireApproval: false,
      focusMode: 'FREEFORM',
      defaultSessionLength: 45,
      maxSessionLength: 240,
      privacyLevel: 'PUBLIC',
      category: 'SOCIAL',
      maxParticipants: 30
    },
    currentMembers: 25,
    createdAt: '2024-01-05T16:00:00Z',
    updatedAt: '2024-01-21T11:45:00Z',
    memberCount: 8,
    isOwner: false,
    isMember: true,
  },
]

const mockMembers: Record<string, HiveMember[]> = {
  '1': [
    {
      id: 'member1',
      userId: 'user1',
      user: {
        id: 'user1',
        email: 'alice@example.com',
        username: 'alice_j',
        firstName: 'Alice',
        lastName: 'Johnson',
        name: 'Alice Johnson',
        profilePicture: undefined,
        isEmailVerified: true,
        isVerified: true,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      },
      hiveId: '1',
      role: 'owner',
      joinedAt: '2024-01-15T10:00:00Z',
      isActive: true,
      permissions: {
        canInviteMembers: true,
        canModerateChat: true,
        canManageSettings: true,
        canStartTimers: true,
      },
    },
    // Add more mock members as needed
  ],
  '2': [
    {
      id: 'member2',
      userId: 'user2',
      user: {
        id: 'user2',
        email: 'bob@example.com',
        username: 'bob_s',
        firstName: 'Bob',
        lastName: 'Smith',
        name: 'Bob Smith',
        profilePicture: undefined,
        isEmailVerified: true,
        isVerified: true,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      },
      hiveId: '2',
      role: 'owner',
      joinedAt: '2024-01-10T14:00:00Z',
      isActive: true,
      permissions: {
        canInviteMembers: true,
        canModerateChat: true,
        canManageSettings: true,
        canStartTimers: true,
      },
    },
  ],
  '3': [
    {
      id: 'member3',
      userId: 'user3',
      user: {
        id: 'user3',
        email: 'carol@example.com',
        username: 'carol_w',
        firstName: 'Carol',
        lastName: 'Williams',
        name: 'Carol Williams',
        profilePicture: undefined,
        isEmailVerified: true,
        isVerified: true,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      },
      hiveId: '3',
      role: 'owner',
      joinedAt: '2024-01-05T16:00:00Z',
      isActive: false,
      permissions: {
        canInviteMembers: true,
        canModerateChat: true,
        canManageSettings: true,
        canStartTimers: true,
      },
    },
  ],
}

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate()
  const [hives, setHives] = useState<Hive[]>([])
  const [members, setMembers] = useState<Record<string, HiveMember[]>>({})
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [stats, setStats] = useState<{
    focusTime: number;
    weeklyGoal: number;
    completedSessions: number;
    streakDays: number;
  }>({ focusTime: 0, weeklyGoal: 200, completedSessions: 0, streakDays: 0 })

  const { authState } = useAuth()
  const currentUserId = authState.user?.id || 'user1'

  // Initialize and immediately try to load real data
  useEffect(() => {
    // Start with loading state
    setIsLoading(true)

    // Try to fetch real data immediately
    fetchRealData().then(() => {
      console.log('Initial data fetch complete')
    }).catch(err => {
      console.error('Initial data fetch failed:', err)
      // Show error state, not mock data
      setHives([])
      setMembers({})
      setStats({ focusTime: 0, weeklyGoal: 0, completedSessions: 0, streakDays: 0 })
      setError('Unable to connect to backend services. Please check your connection.')
    }).finally(() => {
      setIsLoading(false)
    })
  }, [])

  const fetchRealData = async () => {
    try {
      // Import the API services directly (not through index to avoid circular deps)
      const { default: hiveApiService } = await import('../../../services/api/hiveApi')
      const { default: analyticsApiService } = await import('../../../services/api/analyticsApi')

      // Check if services are available
      if (!hiveApiService || !analyticsApiService) {
        console.log('API services not yet available, using mock data')
        return
      }

      console.log('Fetching real data from API...')
      setError(null) // Clear any previous errors

      // Fetch hives
      const hivesResponse = await hiveApiService.getHives(0, 20)
      console.log('Hives API response:', hivesResponse)

      const hivesData = hivesResponse.content || []

      if (hivesData.length > 0) {
        // Convert API response to match our Hive type
        const formattedHives = hivesData.map((hive: any) => ({
          ...hive,
          id: String(hive.id),
          currentMembers: hive.memberCount || 0,
          isOwner: hive.ownerId === currentUserId,
          isMember: true,
          owner: {
            id: String(hive.ownerId),
            email: 'user@example.com',
            username: `user${hive.ownerId}`,
            name: `User ${hive.ownerId}`,
            profilePicture: undefined,
            isEmailVerified: true,
            isVerified: true,
            createdAt: hive.createdAt,
            updatedAt: hive.updatedAt,
          }
        }))

        setHives(formattedHives)
        console.log('Loaded', formattedHives.length, 'hives from API')
      } else {
        console.log('No hives found in API, showing empty state')
        setHives([]) // Show empty state, not mock data
      }

      // Fetch user statistics
      try {
        const userStats = await analyticsApiService.getMyStats('WEEKLY')
        setStats({
          focusTime: Math.round(userStats.totalFocusTime / 60),
          weeklyGoal: 200,
          completedSessions: userStats.totalSessions,
          streakDays: userStats.streakDays
        })
      } catch (err) {
        console.log('Could not fetch user stats, using defaults')
      }

    } catch (err) {
      console.log('Using mock data - API not available:', err)
    }
  }

  // Define handleRefresh first since it's used by other handlers
  const handleRefresh = async (): Promise<void> => {
    setIsLoading(true)
    setError(null)

    try {
      await fetchRealData()
    } catch (error) {
      console.error('Failed to refresh data', error)
      setError('Failed to refresh data. Using cached data.')
    } finally {
      setIsLoading(false)
    }
  }

  const handleJoinHive = async (hiveId: string, message?: string) => {
    void message; // Message parameter for future use
    try {
      const { default: hiveApiService } = await import('../../../services/api/hiveApi')
      await hiveApiService.joinHive(Number(hiveId))
      // Refresh hives after joining
      handleRefresh()
    } catch (error) {
      console.error('Failed to join hive', error)
      setError('Failed to join hive. Please try again.')
    }
  }

  const handleLeaveHive = async (hiveId: string) => {
    try {
      const { default: hiveApiService } = await import('../../../services/api/hiveApi')
      await hiveApiService.leaveHive(Number(hiveId))
      // Refresh hives after leaving
      handleRefresh()
    } catch (error) {
      console.error('Failed to leave hive', error)
      setError('Failed to leave hive. Please try again.')
    }
  }

  const handleEnterHive = (hiveId: string): void => {
    // Navigate to hive page
    window.location.href = `/hive/${hiveId}`
  }

  const handleHiveSettings = (hiveId: string): void => {
    // Navigate to settings page
    window.location.href = `/hive/${hiveId}/settings`
  }

  const handleShareHive = (hiveId: string): void => {
    // Will use hiveId when share dialog is implemented
    void hiveId;
    // Open share dialog
  }

  const handleCreateHive = async (hiveData: any): Promise<void> => {
    try {
      const { default: hiveApiService } = await import('../../../services/api/hiveApi')
      await hiveApiService.createHive(hiveData)
      // Refresh hives after creating
      handleRefresh()
    } catch (error) {
      console.error('Failed to create hive', error)
      setError('Failed to create hive. Please try again.')
    }
  }

  // Calculate dashboard stats
  const joinedHives = hives.filter(hive => hive.isMember)
  const totalFocusTime = stats.focusTime
  const weeklyGoal = stats.weeklyGoal
  const completedSessions = stats.completedSessions
  const streakDays = stats.streakDays

  return (
      <Box>
        {/* Error Alert */}
        {error && (
          <Alert severity="warning" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        {/* Welcome Section */}
        <Box sx={{mb: 4}}>
          <Typography variant="h4" component="h1" fontWeight={600} gutterBottom>
            Welcome back{authState.user?.name ? `, ${authState.user.name}` : authState.user?.displayName ? `, ${authState.user.displayName}` : ''}!
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Ready to focus and be productive? Check out your hives and start a new session.
          </Typography>
        </Box>

        {/* Stats Cards */}
        <Box sx={{
          display: 'grid',
          gridTemplateColumns: {xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(4, 1fr)'},
          gap: 3,
          mb: 4
        }}>
          <Card>
            <CardContent>
              <Box sx={{display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
                <Box>
                  <Typography color="text.secondary" gutterBottom variant="h6">
                    Focus Time
                  </Typography>
                  <Typography variant="h4" component="div">
                    {isLoading ? <Skeleton width={60} /> : `${totalFocusTime}h`}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    This week
                  </Typography>
                </Box>
                <TimerIcon sx={{fontSize: 40, color: 'primary.main'}}/>
              </Box>
              <Box sx={{mt: 2}}>
                <LinearProgress
                    variant="determinate"
                    value={(totalFocusTime / weeklyGoal) * 100}
                    sx={{height: 6, borderRadius: 3}}
                />
                <Typography variant="caption" color="text.secondary" sx={{mt: 1, display: 'block'}}>
                  {Math.round((totalFocusTime / weeklyGoal) * 100)}% of weekly goal
                </Typography>
              </Box>
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Box sx={{display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
                <Box>
                  <Typography color="text.secondary" gutterBottom variant="h6">
                    My Hives
                  </Typography>
                  <Typography variant="h4" component="div">
                    {isLoading ? <Skeleton width={40} /> : joinedHives.length}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Active memberships
                  </Typography>
                </Box>
                <PeopleIcon sx={{fontSize: 40, color: 'success.main'}}/>
              </Box>
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Box sx={{display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
                <Box>
                  <Typography color="text.secondary" gutterBottom variant="h6">
                    Sessions
                  </Typography>
                  <Typography variant="h4" component="div">
                    {isLoading ? <Skeleton width={40} /> : completedSessions}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    This week
                  </Typography>
                </Box>
                <StarIcon sx={{fontSize: 40, color: 'warning.main'}}/>
              </Box>
            </CardContent>
          </Card>

          <Card>
            <CardContent>
              <Box sx={{display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
                <Box>
                  <Typography color="text.secondary" gutterBottom variant="h6">
                    Streak
                  </Typography>
                  <Typography variant="h4" component="div">
                    {isLoading ? <Skeleton width={40} /> : streakDays}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Days active
                  </Typography>
                </Box>
                <TrendingUpIcon sx={{fontSize: 40, color: 'error.main'}}/>
              </Box>
            </CardContent>
          </Card>
        </Box>

        {/* Quick Actions */}
        <Paper sx={{p: 3, mb: 4, bgcolor: 'primary.main', color: 'primary.contrastText'}}>
          <Typography variant="h6" gutterBottom
                      sx={{display: 'flex', alignItems: 'center', gap: 1}}>
            <RocketIcon/>
            Quick Start
          </Typography>
          <Box
            sx={{
              display: 'flex',
              flexDirection: 'row',
              gap: 2,
              alignItems: 'center',
              justifyContent: 'center',
              flexWrap: 'wrap'
            }}
          >
            <Chip
                label="Start 25min Pomodoro"
                onClick={() => {
                  // Start a Pomodoro session with default settings
                  navigate('/timer', { 
                    state: { 
                      mode: 'pomodoro',
                      duration: 25,
                      autoStart: true 
                    } 
                  })
                }}
                sx={{
                  bgcolor: 'primary.contrastText',
                  color: 'primary.main',
                  fontSize: { xs: '0.875rem', sm: '1rem' },
                  fontWeight: 500,
                  py: { xs: 1.5, sm: 2 },
                  px: { xs: 2, sm: 3 },
                  height: 'auto',
                  flex: '1 1 auto',
                  minWidth: 0,
                  '& .MuiChip-label': {
                    padding: { xs: '8px 12px', sm: '12px 16px' },
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis'
                  },
                  '&:hover': {
                    bgcolor: 'grey.100',
                    transform: 'translateY(-2px)',
                    transition: 'all 0.2s ease'
                  }
                }}
            />
            <Chip
                label="Join Active Hive"
                onClick={() => {
                  // Navigate to the first active hive or discover page
                  const activeHive = hives.find(h => h.isMember && h.currentMembers > 0)
                  if (activeHive) {
                    navigate(`/hives/${activeHive.id}`)
                  } else {
                    navigate('/discover')
                  }
                }}
                sx={{
                  bgcolor: 'primary.contrastText',
                  color: 'primary.main',
                  fontSize: { xs: '0.875rem', sm: '1rem' },
                  fontWeight: 500,
                  py: { xs: 1.5, sm: 2 },
                  px: { xs: 2, sm: 3 },
                  height: 'auto',
                  flex: '1 1 auto',
                  minWidth: 0,
                  '& .MuiChip-label': {
                    padding: { xs: '8px 12px', sm: '12px 16px' },
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis'
                  },
                  '&:hover': {
                    bgcolor: 'grey.100',
                    transform: 'translateY(-2px)',
                    transition: 'all 0.2s ease'
                  }
                }}
            />
            <Chip
                label="Browse Discover"
                onClick={() => {
                  navigate('/discover')
                }}
                sx={{
                  bgcolor: 'primary.contrastText',
                  color: 'primary.main',
                  fontSize: { xs: '0.875rem', sm: '1rem' },
                  fontWeight: 500,
                  py: { xs: 1.5, sm: 2 },
                  px: { xs: 2, sm: 3 },
                  height: 'auto',
                  flex: '1 1 auto',
                  minWidth: 0,
                  '& .MuiChip-label': {
                    padding: { xs: '8px 12px', sm: '12px 16px' },
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis'
                  },
                  '&:hover': {
                    bgcolor: 'grey.100',
                    transform: 'translateY(-2px)',
                    transition: 'all 0.2s ease'
                  }
                }}
            />
          </Box>
        </Paper>

        {/* Hives List */}
        <HiveList
            hives={hives}
            members={members}
            currentUserId={currentUserId}
            isLoading={isLoading}
            onJoin={handleJoinHive}
            onLeave={handleLeaveHive}
            onEnter={handleEnterHive}
            onSettings={handleHiveSettings}
            onShare={handleShareHive}
            onRefresh={handleRefresh}
            onCreateHive={handleCreateHive}
            title="My Hives"
            showCreateButton={true}
            showFilters={true}
        />
      </Box>
  )
}

export default DashboardPage