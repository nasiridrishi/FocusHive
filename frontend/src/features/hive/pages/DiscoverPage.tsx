import React, { useState, useEffect } from 'react'
import {
  Box,
  Typography,
  Alert,
  Chip,
  Stack,
} from '@mui/material'
import {
  Explore as ExploreIcon,
} from '@mui/icons-material'
import { HiveList } from '../components'
import { Hive, HiveMember } from '@shared/types'

// Mock data for public hives
const mockPublicHives: Hive[] = [
  {
    id: '4',
    name: 'Coding Bootcamp',
    description: 'Learn to code together! Join fellow developers for coding sessions, pair programming, and project work.',
    ownerId: 'user4',
    owner: {
      id: 'user4',
      email: 'dave@example.com',
      username: 'dave_b',
      firstName: 'Dave',
      lastName: 'Brown',
      name: 'Dave Brown',
      profilePicture: undefined,
      isEmailVerified: true,
      isVerified: true,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
    },
    maxMembers: 25,
    isPublic: true,
    tags: ['Coding', 'Programming', 'Learning', 'Bootcamp'],
    settings: {
      allowChat: true,
      allowVoice: false,
      requireApproval: false,
      focusMode: 'continuous',
      defaultSessionLength: 90,
      maxSessionLength: 180,
    },
    currentMembers: 18,
    createdAt: '2024-01-12T08:00:00Z',
    updatedAt: '2024-01-23T14:20:00Z',
  },
  {
    id: '5',
    name: 'Writers Circle',
    description: 'A supportive community for writers of all levels. Join us for writing sprints, feedback sessions, and creative inspiration.',
    ownerId: 'user5',
    owner: {
      id: 'user5',
      email: 'emma@example.com',
      username: 'emma_d',
      firstName: 'Emma',
      lastName: 'Davis',
      name: 'Emma Davis',
      profilePicture: undefined,
      isEmailVerified: true,
      isVerified: true,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
    },
    maxMembers: 20,
    isPublic: true,
    tags: ['Writing', 'Creative', 'Literature', 'Fiction'],
    settings: {
      allowChat: true,
      allowVoice: false,
      requireApproval: true,
      focusMode: 'pomodoro',
      defaultSessionLength: 25,
      maxSessionLength: 120,
    },
    currentMembers: 14,
    createdAt: '2024-01-08T12:00:00Z',
    updatedAt: '2024-01-22T16:45:00Z',
  },
  {
    id: '6',
    name: 'Language Exchange',
    description: 'Practice languages with native speakers and fellow learners. Improve your conversation skills in a supportive environment.',
    ownerId: 'user6',
    owner: {
      id: 'user6',
      email: 'frank@example.com',
      username: 'frank_m',
      firstName: 'Frank',
      lastName: 'Miller',
      name: 'Frank Miller',
      profilePicture: undefined,
      isEmailVerified: true,
      isVerified: true,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
    },
    maxMembers: 50,
    isPublic: true,
    tags: ['Language Learning', 'Conversation', 'Culture', 'Exchange'],
    settings: {
      allowChat: true,
      allowVoice: true,
      requireApproval: false,
      focusMode: 'flexible',
      defaultSessionLength: 60,
      maxSessionLength: 240,
    },
    currentMembers: 32,
    createdAt: '2024-01-06T10:00:00Z',
    updatedAt: '2024-01-21T13:30:00Z',
  },
  {
    id: '7',
    name: 'Math Study Hall',
    description: 'Tackle challenging math problems together. From basic algebra to advanced calculus, everyone is welcome!',
    ownerId: 'user7',
    owner: {
      id: 'user7',
      email: 'grace@example.com',
      username: 'grace_w',
      firstName: 'Grace',
      lastName: 'Wilson',
      name: 'Grace Wilson',
      profilePicture: undefined,
      isEmailVerified: true,
      isVerified: true,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
    },
    maxMembers: 30,
    isPublic: true,
    tags: ['Math', 'Study', 'Problem Solving', 'Education'],
    settings: {
      allowChat: true,
      allowVoice: false,
      requireApproval: false,
      focusMode: 'continuous',
      defaultSessionLength: 45,
      maxSessionLength: 150,
    },
    currentMembers: 22,
    createdAt: '2024-01-14T14:00:00Z',
    updatedAt: '2024-01-23T10:15:00Z',
  },
  {
    id: '8',
    name: 'Art & Design Studio',
    description: 'Create art in a focused, inspiring environment. Share your work, get feedback, and stay motivated with fellow artists.',
    ownerId: 'user8',
    owner: {
      id: 'user8',
      email: 'henry@example.com',
      username: 'henry_t',
      firstName: 'Henry',
      lastName: 'Taylor',
      name: 'Henry Taylor',
      profilePicture: undefined,
      isEmailVerified: true,
      isVerified: true,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
    },
    maxMembers: 15,
    isPublic: true,
    tags: ['Art', 'Design', 'Creative', 'Visual'],
    settings: {
      allowChat: true,
      allowVoice: false,
      requireApproval: true,
      focusMode: 'flexible',
      defaultSessionLength: 90,
      maxSessionLength: 300,
    },
    currentMembers: 11,
    createdAt: '2024-01-09T16:00:00Z',
    updatedAt: '2024-01-20T12:40:00Z',
  },
]

export const DiscoverPage: React.FC = () => {
  const [hives, setHives] = useState<Hive[]>([])
  const [members, setMembers] = useState<Record<string, HiveMember[]>>({})
  const [isLoading, setIsLoading] = useState(true)

  // Mock current user ID
  const currentUserId = 'user1'

  // Simulate API call
  useEffect(() => {
    setTimeout(() => {
      setHives(mockPublicHives)
      // Mock some members for each hive
      const mockMembersData: Record<string, HiveMember[]> = {}
      mockPublicHives.forEach(hive => {
        mockMembersData[hive.id] = [
          {
            id: `member-${hive.id}-1`,
            userId: hive.ownerId,
            user: hive.owner,
            hiveId: hive.id,
            role: 'owner',
            joinedAt: hive.createdAt,
            isActive: Math.random() > 0.5,
            permissions: {
              canInviteMembers: true,
              canModerateChat: true,
              canManageSettings: true,
              canStartTimers: true,
            },
          },
        ]
      })
      setMembers(mockMembersData)
      setIsLoading(false)
    }, 800)
  }, [])

  const handleJoinHive = async (hiveId: string, message?: string) => {
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1500))
    
    // Show success message or update UI
    alert(`Successfully ${message ? 'requested to join' : 'joined'} hive!`)
  }

  const handleEnterHive = (hiveId: string) => {
    // Navigate to hive page
    window.location.href = `/hive/${hiveId}`
  }

  const handleShareHive = (hiveId: string) => {
    // Open share dialog or copy link
    navigator.clipboard.writeText(`${window.location.origin}/hive/${hiveId}`)
    alert('Hive link copied to clipboard!')
  }

  const handleRefresh = () => {
    setIsLoading(true)
    setTimeout(() => {
      setHives(mockPublicHives)
      setIsLoading(false)
    }, 500)
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
          <ExploreIcon sx={{ fontSize: 40, color: 'primary.main' }} />
          <Typography variant="h4" component="h1" fontWeight={600}>
            Discover Hives
          </Typography>
        </Box>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
          Explore public hives and find communities that match your interests. 
          Join fellow focused individuals and boost your productivity together.
        </Typography>

        {/* Featured Categories */}
        <Alert severity="info" sx={{ mb: 3 }}>
          <Typography variant="body2">
            <strong>New to FocusHive?</strong> Try joining a popular hive to get started. 
            Look for hives with active members and topics that interest you.
          </Typography>
        </Alert>

        <Box>
          <Typography variant="h6" gutterBottom>
            Popular Categories
          </Typography>
          <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1 }}>
            {[
              'Coding', 'Writing', 'Study', 'Language Learning', 
              'Art', 'Math', 'Reading', 'Research', 'Creative'
            ].map((category) => (
              <Chip
                key={category}
                label={category}
                variant="outlined"
                color="primary"
                onClick={() => {}}
                sx={{ cursor: 'pointer' }}
              />
            ))}
          </Stack>
        </Box>
      </Box>

      {/* Hives List */}
      <HiveList
        hives={hives}
        members={members}
        currentUserId={currentUserId}
        isLoading={isLoading}
        onJoin={handleJoinHive}
        onEnter={handleEnterHive}
        onShare={handleShareHive}
        onRefresh={handleRefresh}
        title="Public Hives"
        showCreateButton={false}
        showFilters={true}
      />
    </Box>
  )
}

export default DiscoverPage