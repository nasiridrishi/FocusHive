import React from 'react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/test-utils'
import HiveCard from '../HiveCard'

const mockHive = {
  id: '1',
  name: 'Study Hive',
  description: 'A focused study group for CS students preparing for exams',
  ownerId: 'user123',
  owner: {
    id: 'user123',
    email: 'owner@example.com',
    username: 'studyowner',
    firstName: 'John',
    lastName: 'Doe',
    name: 'John Doe',
    isEmailVerified: true,
    createdAt: '2025-01-01T09:00:00Z',
    updatedAt: '2025-01-01T09:00:00Z',
  },
  tags: ['computer-science', 'study-group', 'exam-prep'],
  settings: {
    privacyLevel: 'PUBLIC' as const,
    category: 'STUDY' as const,
    focusMode: 'POMODORO' as const,
    sessionDuration: 25,
    breakDuration: 5,
    maxParticipants: 10,
    autoStartBreaks: true,
    muteNotifications: false,
    virtualBackgrounds: true,
    screenSharing: true,
    chatEnabled: true,
    videoEnabled: true,
    waitingRoom: false,
    recordSessions: false,
    language: 'en',
    timeZone: 'UTC',
    allowGuestAccess: false,
    requireApproval: false,
    customSettings: {},
    allowChat: true,
    allowVoice: true,
    defaultSessionLength: 25,
    maxSessionLength: 60,
  },
  currentMembers: 0,
  maxMembers: 10,
  isPublic: true,
  isOwner: false,
  isMember: false,
  status: 'ACTIVE' as const,
  members: [],
  createdAt: '2025-01-01T10:00:00Z',
  updatedAt: '2025-01-01T10:00:00Z',
  statistics: {
    totalSessions: 0,
    totalFocusTime: 0,
    averageRating: 0,
    weeklyActiveUsers: 0,
  },
  nextSession: null,
  imageUrl: '/hive-images/study.jpg',
}

describe('HiveCard', () => {
  const defaultProps = {
    hive: mockHive,
    onJoin: vi.fn(),
    onLeave: vi.fn(),
    onView: vi.fn(),
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render hive card container', () => {
      renderWithProviders(<HiveCard {...defaultProps} />)

      expect(screen.getByTestId('hive-card')).toBeInTheDocument()
    })

    it('should display hive name', () => {
      renderWithProviders(<HiveCard {...defaultProps} />)

      expect(screen.getByText('Study Hive')).toBeInTheDocument()
    })

    it('should display hive description', () => {
      renderWithProviders(<HiveCard {...defaultProps} />)

      expect(screen.getByText('A focused study group for CS students preparing for exams')).toBeInTheDocument()
    })

    it('should display member count', () => {
      renderWithProviders(<HiveCard {...defaultProps} />)

      expect(screen.getByTestId('member-count')).toHaveTextContent('0/10')
    })

    it('should display hive type badge', () => {
      renderWithProviders(<HiveCard {...defaultProps} />)

      expect(screen.getByTestId('privacy-badge')).toHaveTextContent('PUBLIC')
    })

    it('should display hive category', () => {
      renderWithProviders(<HiveCard {...defaultProps} />)

      expect(screen.getByTestId('category-chip')).toHaveTextContent('STUDY')
    })

    it('should display tags', () => {
      renderWithProviders(<HiveCard {...defaultProps} />)

      const tagsContainer = screen.getByTestId('hive-tags')
      expect(within(tagsContainer).getByText('computer-science')).toBeInTheDocument()
      expect(within(tagsContainer).getByText('study-group')).toBeInTheDocument()
      expect(within(tagsContainer).getByText('exam-prep')).toBeInTheDocument()
    })

    it('should display creator name', () => {
      renderWithProviders(<HiveCard {...defaultProps} />)

      expect(screen.getByTestId('owner-info')).toBeInTheDocument()
    })

    it('should display hive image when provided', () => {
      renderWithProviders(<HiveCard {...defaultProps} />)

      const image = screen.getByRole('img', { name: /study hive/i })
      expect(image).toHaveAttribute('src', '/hive-images/study.jpg')
    })

    it('should display default image when no image provided', () => {
      const hiveWithoutImage = { ...mockHive, imageUrl: undefined }
      renderWithProviders(<HiveCard {...defaultProps} hive={hiveWithoutImage} />)

      expect(screen.getByTestId('default-hive-image')).toBeInTheDocument()
    })
  })

  describe('Member Status', () => {
    it('should show join button when user is not a member', () => {
      renderWithProviders(<HiveCard {...defaultProps} />)

      expect(screen.getByRole('button', { name: /join/i })).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: /leave/i })).not.toBeInTheDocument()
    })

    it('should show leave button when user is a member', () => {
      const memberHive = {
        ...mockHive,
        isMember: true,
        members: [{ userId: 'current-user', joinedAt: new Date().toISOString() }]
      }
      renderWithProviders(<HiveCard {...defaultProps} hive={memberHive} currentUserId="current-user" />)

      expect(screen.getByRole('button', { name: /leave/i })).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: /join/i })).not.toBeInTheDocument()
    })

    it('should show member badge when user is a member', () => {
      const memberHive = {
        ...mockHive,
        isMember: true,
        members: [{ userId: 'current-user', joinedAt: new Date().toISOString() }]
      }
      renderWithProviders(<HiveCard {...defaultProps} hive={memberHive} currentUserId="current-user" />)

      expect(screen.getByTestId('member-badge')).toBeInTheDocument()
    })

    it('should disable join button when hive is full', () => {
      const fullHive = {
        ...mockHive,
        currentMembers: 10,
        members: Array(10).fill(null).map((_, i) => ({ userId: `user${i}`, joinedAt: new Date().toISOString() }))
      }
      renderWithProviders(<HiveCard {...defaultProps} hive={fullHive} />)

      const joinButton = screen.getByRole('button', { name: /full/i })
      expect(joinButton).toBeDisabled()
    })
  })

  describe('Hive Types', () => {
    it('should display private hive indicator', () => {
      const privateHive = {
        ...mockHive,
        isPublic: false,
        settings: { ...mockHive.settings, privacyLevel: 'PRIVATE' as const }
      }
      renderWithProviders(<HiveCard {...defaultProps} hive={privateHive} />)

      expect(screen.getByTestId('privacy-badge')).toHaveTextContent('PRIVATE')
      expect(screen.getByTestId('privacy-icon')).toBeInTheDocument()
    })

    it('should display invite-only hive indicator', () => {
      const inviteHive = {
        ...mockHive,
        isPublic: false,
        settings: { ...mockHive.settings, privacyLevel: 'INVITE_ONLY' as const }
      }
      renderWithProviders(<HiveCard {...defaultProps} hive={inviteHive} />)

      expect(screen.getByTestId('privacy-badge')).toHaveTextContent('INVITE_ONLY')
      expect(screen.getByTestId('privacy-icon')).toBeInTheDocument()
    })
  })

  describe('User Interactions', () => {
    it('should call onJoin when join button is clicked', async () => {
      const user = userEvent.setup()
      const onJoin = vi.fn()
      renderWithProviders(<HiveCard {...defaultProps} onJoin={onJoin} />)

      const joinButton = screen.getByRole('button', { name: /join/i })
      await user.click(joinButton)

      expect(onJoin).toHaveBeenCalledWith('1')
    })

    it('should call onLeave when leave button is clicked', async () => {
      const user = userEvent.setup()
      const onLeave = vi.fn()
      const memberHive = {
        ...mockHive,
        isMember: true,
        members: [{ userId: 'current-user', joinedAt: new Date().toISOString() }]
      }
      renderWithProviders(
        <HiveCard {...defaultProps} hive={memberHive} onLeave={onLeave} currentUserId="current-user" />
      )

      const leaveButton = screen.getByRole('button', { name: /leave/i })
      await user.click(leaveButton)

      expect(onLeave).toHaveBeenCalledWith('1')
    })

    it('should call onView when card is clicked', async () => {
      const user = userEvent.setup()
      const onView = vi.fn()
      renderWithProviders(<HiveCard {...defaultProps} onView={onView} />)

      const card = screen.getByTestId('hive-card')
      await user.click(card)

      expect(onView).toHaveBeenCalledWith('1')
    })

    it('should not call onView when action buttons are clicked', async () => {
      const user = userEvent.setup()
      const onView = vi.fn()
      const onJoin = vi.fn()
      renderWithProviders(
        <HiveCard {...defaultProps} onView={onView} onJoin={onJoin} />
      )

      const joinButton = screen.getByRole('button', { name: /join/i })
      await user.click(joinButton)

      expect(onJoin).toHaveBeenCalled()
      expect(onView).not.toHaveBeenCalled()
    })
  })

  describe('Status Indicators', () => {
    it('should show active indicator for active hives', () => {
      renderWithProviders(<HiveCard {...defaultProps} />)

      expect(screen.getByTestId('active-indicator')).toBeInTheDocument()
    })

    it('should show inactive indicator for inactive hives', () => {
      const inactiveHive = { ...mockHive, status: 'INACTIVE' as const }
      renderWithProviders(<HiveCard {...defaultProps} hive={inactiveHive} />)

      expect(screen.getByTestId('inactive-indicator')).toBeInTheDocument()
    })

    it('should show member progress bar', () => {
      renderWithProviders(<HiveCard {...defaultProps} />)

      const progressBar = screen.getByRole('progressbar', { hidden: true })
      expect(progressBar).toHaveAttribute('aria-valuenow', '0') // 0/10 = 0%
    })
  })

  describe('Hover Effects', () => {
    it('should show view details on hover', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveCard {...defaultProps} />)

      const card = screen.getByTestId('hive-card')
      await user.hover(card)

      expect(screen.getByText(/view details/i)).toBeInTheDocument()
    })
  })

  describe('Loading State', () => {
    it('should show skeleton when loading', () => {
      renderWithProviders(<HiveCard {...defaultProps} isLoading />)

      expect(screen.getByTestId('hive-card-skeleton')).toBeInTheDocument()
    })
  })

  describe('Accessibility', () => {
    it('should have accessible card structure', () => {
      renderWithProviders(<HiveCard {...defaultProps} />)

      const card = screen.getByTestId('hive-card')
      expect(card).toHaveAttribute('role', 'article')
    })

    it('should have accessible member count', () => {
      renderWithProviders(<HiveCard {...defaultProps} />)

      const memberCount = screen.getByTestId('member-count')
      expect(memberCount).toHaveAttribute('aria-label', '0 out of 10 members')
    })

    it('should have keyboard navigation support', async () => {
      const user = userEvent.setup()
      const onView = vi.fn()
      renderWithProviders(<HiveCard {...defaultProps} onView={onView} />)

      const card = screen.getByTestId('hive-card')
      card.focus()

      await user.keyboard('{Enter}')
      expect(onView).toHaveBeenCalled()
    })
  })

  describe('Responsive Design', () => {
    it('should hide tags on small screens', () => {
      renderWithProviders(<HiveCard {...defaultProps} compact />)

      expect(screen.queryByTestId('hive-tags')).not.toBeInTheDocument()
    })

    it('should show compact layout when specified', () => {
      renderWithProviders(<HiveCard {...defaultProps} compact />)

      const card = screen.getByTestId('hive-card')
      expect(card).toHaveClass('hive-card--compact')
    })
  })
})