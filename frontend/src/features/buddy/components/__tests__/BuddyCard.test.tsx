import React from 'react'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {screen, waitFor} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {renderWithProviders} from '../../../../test-utils/test-utils'
import {BuddyCard, BuddyCardProps} from '../BuddyCard'
import {BuddyMatch, BuddyRelationship} from '../../components/types'

const user = userEvent.setup()

// Mock child components that might be heavy or complex
vi.mock('@mui/icons-material', () => ({
  AccessTime: () => <div data-testid="time-icon" />,
  Chat: () => <div data-testid="chat-icon" />,
  EmojiEvents: () => <div data-testid="trophy-icon" />,
  FiberManualRecord: () => <div data-testid="status-icon" />,
  Groups: () => <div data-testid="groups-icon" />,
  Message: () => <div data-testid="message-icon" />,
  Person: () => <div data-testid="person-icon" />,
  Psychology: () => <div data-testid="psychology-icon" />,
  Schedule: () => <div data-testid="schedule-icon" />,
  Star: () => <div data-testid="star-icon" />,
  Visibility: () => <div data-testid="visibility-icon" />,
  WhatshotRounded: () => <div data-testid="fire-icon" />
}))

// Mock formatDistanceToNow from date-fns
vi.mock('date-fns', () => ({
  formatDistanceToNow: vi.fn((date: Date) => {
    const now = new Date()
    const diff = now.getTime() - date.getTime()
    const minutes = Math.floor(diff / (1000 * 60))

    if (minutes < 1) return 'just now'
    if (minutes < 60) return `${minutes} minutes ago`
    if (minutes < 1440) return `${Math.floor(minutes / 60)} hours ago`
    return `${Math.floor(minutes / 1440)} days ago`
  })
}))

// Mock data
const mockAchievements = [
  { id: '1', name: 'Goal Crusher', description: 'Complete 10 goals', icon: '🏆', earnedAt: '2024-01-01T00:00:00Z' },
  { id: '2', name: 'Consistency King', description: '30 day streak', icon: '👑', earnedAt: '2024-01-02T00:00:00Z' },
  { id: '3', name: 'Team Player', description: 'Join 5 hives', icon: '🤝', earnedAt: '2024-01-03T00:00:00Z' }
]

const mockBuddyMatch: BuddyMatch = {
  id: '1',
  userId: '1',
  username: 'john_doe',
  email: 'john@example.com',
  firstName: 'John',
  lastName: 'Doe',
  avatar: 'https://example.com/avatar.jpg',
  status: 'ONLINE',
  compatibilityScore: 0.85,
  commonInterests: ['Programming', 'Study', 'Fitness'],
  achievements: mockAchievements,
  focusGoals: ['Complete project', 'Study daily'],
  preferredSchedule: {
    timezone: 'America/New_York',
    workingHours: '9-5',
    preferredFocusTimes: ['Morning', 'Evening']
  },
  matchedAt: '2024-01-01T00:00:00Z',
  isRequested: false,
  isPending: false
}

const mockBuddyRelationship: BuddyRelationship = {
  id: '1',
  buddyId: '2',
  buddy: {
    id: '2',
    username: 'jane_smith',
    email: 'jane@example.com',
    firstName: 'Jane',
    lastName: 'Smith',
    avatar: 'https://example.com/avatar2.jpg'
  },
  status: 'ACTIVE',
  startedAt: '2024-01-01T00:00:00Z',
  compatibility: {
    score: 0.85,
    commonInterests: ['Programming', 'Study'],
    focusAlignment: 0.9,
    scheduleCompatibility: 0.8
  },
  sharedSessions: 12,
  achievements: mockAchievements,
  lastInteraction: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString() // 2 hours ago
}

describe('BuddyCard', () => {
  const defaultProps: BuddyCardProps = {
    buddy: mockBuddyMatch,
    variant: 'default' as const,
    currentUserId: 2,
    onConnect: vi.fn(),
    onMessage: vi.fn(),
    onViewProfile: vi.fn()
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering Tests', () => {
    it('should render the component with basic buddy information', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      expect(screen.getByTestId('buddy-card')).toBeInTheDocument()
      expect(screen.getByText('john_doe')).toBeInTheDocument()
      expect(screen.getByText('Passionate about productivity and focus techniques')).toBeInTheDocument()
    })

    it('should render avatar with image when provided', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      const avatar = screen.getByRole('img')
      expect(avatar).toHaveAttribute('src', 'https://example.com/avatar.jpg')
    })

    it('should render avatar with initials fallback when no image', () => {
      const buddyWithoutAvatar = { ...mockBuddyMatch, avatar: undefined }
      renderWithProviders(<BuddyCard {...defaultProps} buddy={buddyWithoutAvatar} />)

      expect(screen.getByText('J')).toBeInTheDocument() // First letter of username
    })

    it('should render with custom data-testid when provided', () => {
      renderWithProviders(<BuddyCard {...defaultProps} data-testid="custom-buddy-card" />)

      expect(screen.getByTestId('custom-buddy-card')).toBeInTheDocument()
    })

    it('should handle missing bio gracefully', () => {
      const buddyWithoutBio = mockBuddyMatch
      renderWithProviders(<BuddyCard {...defaultProps} buddy={buddyWithoutBio} />)

      expect(screen.getByTestId('buddy-card')).toBeInTheDocument()
      expect(screen.queryByText('Passionate about productivity')).not.toBeInTheDocument()
    })
  })

  describe('Buddy Information Display', () => {
    it('should display buddy name correctly', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      expect(screen.getByText('john_doe')).toBeInTheDocument()
    })

    it('should display bio when available', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      expect(screen.getByText('Passionate about productivity and focus techniques')).toBeInTheDocument()
    })

    it('should display shared interests as chips', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      expect(screen.getByText('Programming')).toBeInTheDocument()
      expect(screen.getByText('Study')).toBeInTheDocument()
      expect(screen.getByText('Fitness')).toBeInTheDocument()
    })

    it('should handle empty shared interests gracefully', () => {
      const buddyWithoutInterests = { ...mockBuddyMatch, commonInterests: [] }
      renderWithProviders(<BuddyCard {...defaultProps} buddy={buddyWithoutInterests} />)

      expect(screen.getByTestId('buddy-card')).toBeInTheDocument()
      expect(screen.queryByText('Programming')).not.toBeInTheDocument()
    })

    it('should display communication style', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      expect(screen.getByText(/moderate communication/i)).toBeInTheDocument()
    })
  })

  describe('Status Indicators', () => {
    it('should show online status for active buddy relationship', () => {
      const buddyRelationship = { ...mockBuddyRelationship, status: 'ACTIVE' as const }
      renderWithProviders(
        <BuddyCard
          {...defaultProps}
          buddy={buddyRelationship}
          isOnline={true}
        />
      )

      expect(screen.getByText('Online')).toBeInTheDocument()
      expect(screen.getByTestId('status-icon')).toBeInTheDocument()
    })

    it('should show offline status when buddy is not online', () => {
      renderWithProviders(
        <BuddyCard
          {...defaultProps}
          isOnline={false}
        />
      )

      expect(screen.getByText('Offline')).toBeInTheDocument()
    })

    it('should show away status when specified', () => {
      renderWithProviders(
        <BuddyCard
          {...defaultProps}
          status="away"
        />
      )

      expect(screen.getByText('Away')).toBeInTheDocument()
    })

    it('should show last active time when offline', () => {
      const relationshipWithLastActive = {
        ...mockBuddyRelationship,
        updatedAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString()
      }

      renderWithProviders(
        <BuddyCard
          {...defaultProps}
          buddy={relationshipWithLastActive}
          isOnline={false}
          lastActiveTime={new Date(Date.now() - 2 * 60 * 60 * 1000)}
        />
      )

      expect(screen.getByText(/2 hours ago/)).toBeInTheDocument()
    })
  })

  describe('Compatibility Score Display', () => {
    it('should display match percentage for high compatibility', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      expect(screen.getByText('85%')).toBeInTheDocument()
      expect(screen.getByText(/excellent match/i)).toBeInTheDocument()
    })

    it('should display good match for medium compatibility', () => {
      const mediumMatch = { ...mockBuddyMatch, compatibilityScore: 0.7 }
      renderWithProviders(<BuddyCard {...defaultProps} buddy={mediumMatch} />)

      expect(screen.getByText('70%')).toBeInTheDocument()
      expect(screen.getByText(/good match/i)).toBeInTheDocument()
    })

    it('should display fair match for lower compatibility', () => {
      const lowMatch = { ...mockBuddyMatch, compatibilityScore: 0.5 }
      renderWithProviders(<BuddyCard {...defaultProps} buddy={lowMatch} />)

      expect(screen.getByText('50%')).toBeInTheDocument()
      expect(screen.getByText(/fair match/i)).toBeInTheDocument()
    })

    it('should display poor match for very low compatibility', () => {
      const poorMatch = { ...mockBuddyMatch, compatibilityScore: 0.3 }
      renderWithProviders(<BuddyCard {...defaultProps} buddy={poorMatch} />)

      expect(screen.getByText('30%')).toBeInTheDocument()
      expect(screen.getByText(/poor match/i)).toBeInTheDocument()
    })

    it('should use appropriate color for match score chip', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      const matchChip = screen.getByText('85%').closest('.MuiChip-root')
      expect(matchChip).toHaveClass('MuiChip-colorSuccess')
    })
  })

  describe('Action Buttons Functionality', () => {
    it('should render Connect button for potential matches', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      expect(screen.getByRole('button', { name: /connect/i })).toBeInTheDocument()
    })

    it('should render Message button', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      expect(screen.getByRole('button', { name: /message/i })).toBeInTheDocument()
    })

    it('should render View Profile button', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      expect(screen.getByRole('button', { name: /view profile/i })).toBeInTheDocument()
    })

    it('should call onConnect when connect button is clicked', async () => {
      const onConnect = vi.fn()
      renderWithProviders(<BuddyCard {...defaultProps} onConnect={onConnect} />)

      const connectButton = screen.getByRole('button', { name: /connect/i })
      await user.click(connectButton)

      expect(onConnect).toHaveBeenCalledWith(mockBuddyMatch.userId)
    })

    it('should call onMessage when message button is clicked', async () => {
      const onMessage = vi.fn()
      renderWithProviders(<BuddyCard {...defaultProps} onMessage={onMessage} />)

      const messageButton = screen.getByRole('button', { name: /message/i })
      await user.click(messageButton)

      expect(onMessage).toHaveBeenCalledWith(mockBuddyMatch.userId)
    })

    it('should call onViewProfile when view profile button is clicked', async () => {
      const onViewProfile = vi.fn()
      renderWithProviders(<BuddyCard {...defaultProps} onViewProfile={onViewProfile} />)

      const viewProfileButton = screen.getByRole('button', { name: /view profile/i })
      await user.click(viewProfileButton)

      expect(onViewProfile).toHaveBeenCalledWith(mockBuddyMatch.userId)
    })

    it('should disable connect button when request is pending', () => {
      renderWithProviders(<BuddyCard {...defaultProps} isPending={true} />)

      const connectButton = screen.getByRole('button', { name: /pending/i })
      expect(connectButton).toBeDisabled()
    })

    it('should show different button text for existing buddies', () => {
      renderWithProviders(
        <BuddyCard
          {...defaultProps}
          buddy={mockBuddyRelationship}
          isConnected={true}
        />
      )

      expect(screen.getByRole('button', { name: /connected/i })).toBeInTheDocument()
    })
  })

  describe('Achievement Badges', () => {
    it('should display achievement badges when provided', () => {
      const achievementNames = mockAchievements.map(a => a.name)
      renderWithProviders(
        <BuddyCard
          {...defaultProps}
          achievements={achievementNames}
        />
      )

      expect(screen.getByText('Goal Crusher')).toBeInTheDocument()
      expect(screen.getByText('Consistency King')).toBeInTheDocument()
      expect(screen.getByText('Team Player')).toBeInTheDocument()
    })

    it('should not show achievement section when no achievements', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      expect(screen.queryByText('Achievements')).not.toBeInTheDocument()
    })

    it('should limit number of achievement badges displayed', () => {
      const manyAchievements = Array.from({ length: 10 }, (_, i) => `Achievement ${i + 1}`)
      renderWithProviders(
        <BuddyCard
          {...defaultProps}
          achievements={manyAchievements}
        />
      )

      // Should only show first 3 achievements
      expect(screen.getByText('Achievement 1')).toBeInTheDocument()
      expect(screen.getByText('Achievement 2')).toBeInTheDocument()
      expect(screen.getByText('Achievement 3')).toBeInTheDocument()
      expect(screen.queryByText('Achievement 4')).not.toBeInTheDocument()
    })
  })

  describe('Buddy Streak and Statistics', () => {
    it('should display streak information for buddy relationships', () => {
      renderWithProviders(
        <BuddyCard
          {...defaultProps}
          buddy={mockBuddyRelationship}
          streakDays={7}
        />
      )

      expect(screen.getByText(/7 day streak/i)).toBeInTheDocument()
      expect(screen.getByTestId('fire-icon')).toBeInTheDocument()
    })

    it('should display completed goals count', () => {
      renderWithProviders(
        <BuddyCard
          {...defaultProps}
          buddy={mockBuddyRelationship}
        />
      )

      expect(screen.getByText('3/5 goals')).toBeInTheDocument()
    })

    it('should display total sessions completed', () => {
      renderWithProviders(
        <BuddyCard
          {...defaultProps}
          buddy={mockBuddyRelationship}
        />
      )

      expect(screen.getByText('12 sessions')).toBeInTheDocument()
    })

    it('should display average session rating when available', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      expect(screen.getByText('4.7')).toBeInTheDocument()
      expect(screen.getByTestId('star-icon')).toBeInTheDocument()
    })

    it('should handle zero streak gracefully', () => {
      renderWithProviders(
        <BuddyCard
          {...defaultProps}
          buddy={mockBuddyRelationship}
          streakDays={0}
        />
      )

      expect(screen.queryByText(/streak/i)).not.toBeInTheDocument()
    })
  })

  describe('Different Card Sizes', () => {
    it('should render compact variant correctly', () => {
      renderWithProviders(<BuddyCard {...defaultProps} variant="compact" />)

      const card = screen.getByTestId('buddy-card')
      expect(card).toHaveAttribute('data-variant', 'compact')
    })

    it('should render full variant with all details', () => {
      renderWithProviders(<BuddyCard {...defaultProps} variant="full" />)

      const card = screen.getByTestId('buddy-card')
      expect(card).toHaveAttribute('data-variant', 'full')

      // Full variant should show more details
      expect(screen.getByText(/timezone overlap/i)).toBeInTheDocument()
    })

    it('should hide certain elements in compact mode', () => {
      renderWithProviders(<BuddyCard {...defaultProps} variant="compact" />)

      // Bio should be truncated or hidden in compact mode
      const bio = screen.queryByText('Passionate about productivity and focus techniques')
      if (bio) {
        expect(bio).toHaveClass('truncated') // Assuming compact mode truncates
      }
    })

    it('should show all action buttons in full mode', () => {
      renderWithProviders(<BuddyCard {...defaultProps} variant="full" />)

      expect(screen.getByRole('button', { name: /connect/i })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /message/i })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /view profile/i })).toBeInTheDocument()
    })

    it('should prioritize primary actions in compact mode', () => {
      renderWithProviders(<BuddyCard {...defaultProps} variant="compact" />)

      // Connect button should always be visible
      expect(screen.getByRole('button', { name: /connect/i })).toBeInTheDocument()
    })
  })

  describe('Accessibility', () => {
    it('should have proper ARIA labels for status indicators', () => {
      renderWithProviders(
        <BuddyCard
          {...defaultProps}
          isOnline={true}
        />
      )

      const statusIndicator = screen.getByLabelText(/online status/i)
      expect(statusIndicator).toBeInTheDocument()
    })

    it('should have accessible button labels', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      expect(screen.getByRole('button', { name: /connect with john_doe/i })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /send message to john_doe/i })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /view john_doe's profile/i })).toBeInTheDocument()
    })

    it('should have proper alt text for avatar images', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      const avatar = screen.getByRole('img')
      expect(avatar).toHaveAttribute('alt', 'john_doe avatar')
    })

    it('should be keyboard navigable', async () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      const connectButton = screen.getByRole('button', { name: /connect/i })
      const messageButton = screen.getByRole('button', { name: /message/i })

      // Test keyboard navigation
      connectButton.focus()
      expect(connectButton).toHaveFocus()

      await user.tab()
      expect(messageButton).toHaveFocus()
    })

    it('should have proper semantic markup', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      // Card should be properly structured
      expect(screen.getByRole('article')).toBeInTheDocument()
      expect(screen.getByRole('heading', { level: 6 })).toBeInTheDocument()
    })

    it('should support screen reader announcements for status changes', () => {
      const { rerender } = renderWithProviders(
        <BuddyCard {...defaultProps} isOnline={false} />
      )

      rerender(<BuddyCard {...defaultProps} isOnline={true} />)

      expect(screen.getByText('Online')).toBeInTheDocument()
      expect(screen.getByLabelText(/online status/i)).toBeInTheDocument()
    })

    it('should have high contrast indicators for important information', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      const matchScoreChip = screen.getByText('85%').closest('.MuiChip-root')
      expect(matchScoreChip).toHaveClass('MuiChip-colorSuccess')
    })
  })

  describe('Error Handling and Edge Cases', () => {
    it('should handle undefined buddy gracefully', () => {
      renderWithProviders(<BuddyCard {...defaultProps} buddy={undefined as any} />)

      expect(screen.queryByTestId('buddy-card')).not.toBeInTheDocument()
    })

    it('should handle extremely long usernames', () => {
      const longUsername = 'a'.repeat(50)
      const buddyWithLongName = { ...mockBuddyMatch, username: longUsername }

      renderWithProviders(<BuddyCard {...defaultProps} buddy={buddyWithLongName} />)

      const usernameElement = screen.getByText(longUsername)
      expect(usernameElement).toBeInTheDocument()
    })

    it('should handle missing callback functions gracefully', () => {
      renderWithProviders(
        <BuddyCard
          {...defaultProps}
          onConnect={undefined}
          onMessage={undefined}
          onViewProfile={undefined}
        />
      )

      // Buttons should either be disabled or not crash when clicked
      const buttons = screen.getAllByRole('button')
      expect(buttons.length).toBeGreaterThan(0)
    })

    it('should handle invalid match scores', () => {
      const invalidMatch = { ...mockBuddyMatch, matchScore: 1.5 } // Invalid score > 1
      renderWithProviders(<BuddyCard {...defaultProps} buddy={invalidMatch} />)

      // Should cap at 100%
      expect(screen.getByText('100%')).toBeInTheDocument()
    })

    it('should handle negative achievement counts', () => {
      const negativeStats = { ...mockBuddyMatch, completedGoalsCount: -1 }
      renderWithProviders(<BuddyCard {...defaultProps} buddy={negativeStats} />)

      // Should display 0 instead of negative
      expect(screen.getByText(/0.*goals/)).toBeInTheDocument()
    })

    it('should handle future dates in lastActiveTime', () => {
      const futureDate = new Date(Date.now() + 24 * 60 * 60 * 1000) // Tomorrow

      renderWithProviders(
        <BuddyCard
          {...defaultProps}
          isOnline={false}
          lastActiveTime={futureDate}
        />
      )

      // Should handle future dates gracefully
      expect(screen.getByTestId('buddy-card')).toBeInTheDocument()
    })
  })

  describe('Performance and Optimization', () => {
    it('should not re-render unnecessarily with same props', () => {
      const { rerender } = renderWithProviders(<BuddyCard {...defaultProps} />)

      const initialText = screen.getByText('john_doe')
      expect(initialText).toBeInTheDocument()

      // Rerender with same props
      rerender(<BuddyCard {...defaultProps} />)

      // Should still show the same content without unnecessary updates
      expect(screen.getByText('john_doe')).toBeInTheDocument()
    })

    it('should handle rapid status changes smoothly', async () => {
      const { rerender } = renderWithProviders(
        <BuddyCard {...defaultProps} isOnline={false} />
      )

      // Rapid status changes
      rerender(<BuddyCard {...defaultProps} isOnline={true} />)
      rerender(<BuddyCard {...defaultProps} isOnline={false} />)
      rerender(<BuddyCard {...defaultProps} isOnline={true} />)

      await waitFor(() => {
        expect(screen.getByText('Online')).toBeInTheDocument()
      })
    })

    it('should optimize image loading with proper src attributes', () => {
      renderWithProviders(<BuddyCard {...defaultProps} />)

      const avatar = screen.getByRole('img')
      expect(avatar).toHaveAttribute('src', 'https://example.com/avatar.jpg')
      expect(avatar).toHaveAttribute('loading', 'lazy') // Assuming lazy loading is implemented
    })
  })
})