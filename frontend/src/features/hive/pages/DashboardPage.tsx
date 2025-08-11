import React, { useState, useEffect } from 'react'
import {
  Box,
  Typography,
  Card,
  CardContent,
  Chip,
  Paper,
  Stack,
  LinearProgress,
} from '@mui/material'
import {
  TrendingUp as TrendingUpIcon,
  Timer as TimerIcon,
  People as PeopleIcon,
  Star as StarIcon,
} from '@mui/icons-material'
import { HiveList } from '../components'
import { Hive, HiveMember } from '@shared/types'

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
      focusMode: 'pomodoro',
      defaultSessionLength: 25,
      maxSessionLength: 120,
    },
    currentMembers: 12,
    createdAt: '2024-01-15T10:00:00Z',
    updatedAt: '2024-01-20T15:30:00Z',
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
      focusMode: 'continuous',
      defaultSessionLength: 60,
      maxSessionLength: 180,
    },
    currentMembers: 8,
    createdAt: '2024-01-10T14:00:00Z',
    updatedAt: '2024-01-22T09:15:00Z',
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
      focusMode: 'flexible',
      defaultSessionLength: 45,
      maxSessionLength: 240,
    },
    currentMembers: 25,
    createdAt: '2024-01-05T16:00:00Z',
    updatedAt: '2024-01-21T11:45:00Z',
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
  const [hives, setHives] = useState<Hive[]>([])
  const [members, setMembers] = useState<Record<string, HiveMember[]>>({})
  const [isLoading, setIsLoading] = useState(true)

  // Mock current user ID
  const currentUserId = 'user1'

  // Simulate API call
  useEffect(() => {
    setTimeout(() => {
      setHives(mockHives)
      setMembers(mockMembers)
      setIsLoading(false)
    }, 1000)
  }, [])

  const handleJoinHive = async (hiveId: string, message?: string) => {
    // Will use hiveId and message when API is implemented
    void hiveId;
    void message;
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000))
  }

  const handleLeaveHive = async (hiveId: string) => {
    // Will use hiveId when API is implemented
    void hiveId;
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000))
  }

  const handleEnterHive = (hiveId: string) => {
    // Navigate to hive page
    window.location.href = `/hive/${hiveId}`
  }

  const handleHiveSettings = (hiveId: string) => {
    // Navigate to settings page
    window.location.href = `/hive/${hiveId}/settings`
  }

  const handleShareHive = (hiveId: string) => {
    // Will use hiveId when share dialog is implemented
    void hiveId;
    // Open share dialog
  }

  const handleCreateHive = (hiveData: object) => {
    // Will use hiveData when API is implemented
    void hiveData;
    // Simulate API call and update state
  }

  const handleRefresh = () => {
    setIsLoading(true)
    setTimeout(() => {
      setHives(mockHives)
      setMembers(mockMembers)
      setIsLoading(false)
    }, 500)
  }

  // Calculate dashboard stats
  const joinedHives = hives.filter(hive => 
    members[hive.id]?.some(member => member.userId === currentUserId)
  )
  const totalFocusTime = 145 // Mock data - would come from API
  const weeklyGoal = 200
  const completedSessions = 12

  return (
    <Box>
      {/* Welcome Section */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" component="h1" fontWeight={600} gutterBottom>
          Welcome back! 👋
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Ready to focus and be productive? Check out your hives and start a new session.
        </Typography>
      </Box>

      {/* Stats Cards */}
      <Box sx={{ 
        display: 'grid', 
        gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', md: 'repeat(4, 1fr)' },
        gap: 3,
        mb: 4 
      }}>
        <Card>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <Box>
                <Typography color="text.secondary" gutterBottom variant="h6">
                  Focus Time
                </Typography>
                <Typography variant="h4" component="div">
                  {totalFocusTime}h
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  This week
                </Typography>
              </Box>
              <TimerIcon sx={{ fontSize: 40, color: 'primary.main' }} />
            </Box>
            <Box sx={{ mt: 2 }}>
              <LinearProgress 
                variant="determinate" 
                value={(totalFocusTime / weeklyGoal) * 100}
                sx={{ height: 6, borderRadius: 3 }}
              />
              <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                {Math.round((totalFocusTime / weeklyGoal) * 100)}% of weekly goal
              </Typography>
            </Box>
          </CardContent>
        </Card>

        <Card>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <Box>
                <Typography color="text.secondary" gutterBottom variant="h6">
                  My Hives
                </Typography>
                <Typography variant="h4" component="div">
                  {joinedHives.length}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Active memberships
                </Typography>
              </Box>
              <PeopleIcon sx={{ fontSize: 40, color: 'success.main' }} />
            </Box>
          </CardContent>
        </Card>

        <Card>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <Box>
                <Typography color="text.secondary" gutterBottom variant="h6">
                  Sessions
                </Typography>
                <Typography variant="h4" component="div">
                  {completedSessions}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  This week
                </Typography>
              </Box>
              <StarIcon sx={{ fontSize: 40, color: 'warning.main' }} />
            </Box>
          </CardContent>
        </Card>

        <Card>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <Box>
                <Typography color="text.secondary" gutterBottom variant="h6">
                  Streak
                </Typography>
                <Typography variant="h4" component="div">
                  7
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Days active
                </Typography>
              </Box>
              <TrendingUpIcon sx={{ fontSize: 40, color: 'error.main' }} />
            </Box>
          </CardContent>
        </Card>
      </Box>

      {/* Quick Actions */}
      <Paper sx={{ p: 3, mb: 4, bgcolor: 'primary.main', color: 'primary.contrastText' }}>
        <Typography variant="h6" gutterBottom>
          🚀 Quick Start
        </Typography>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <Chip
            label="Start 25min Pomodoro"
            onClick={() => {}}
            sx={{ 
              bgcolor: 'primary.contrastText', 
              color: 'primary.main',
              '&:hover': { bgcolor: 'grey.100' }
            }}
          />
          <Chip
            label="Join Active Hive"
            onClick={() => {}}
            sx={{ 
              bgcolor: 'primary.contrastText', 
              color: 'primary.main',
              '&:hover': { bgcolor: 'grey.100' }
            }}
          />
          <Chip
            label="Browse Discover"
            onClick={() => {}}
            sx={{ 
              bgcolor: 'primary.contrastText', 
              color: 'primary.main',
              '&:hover': { bgcolor: 'grey.100' }
            }}
          />
        </Stack>
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