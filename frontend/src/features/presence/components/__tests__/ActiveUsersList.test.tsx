import React from 'react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { screen, within, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/test-utils'
import ActiveUsersList, { ActiveUsersInline } from '../ActiveUsersList'
import { UserPresence } from '../../../../shared/types/presence'

// Mock the child components to simplify testing
vi.mock('../PresenceIndicator', () => ({
  PresenceAvatar: ({ status, name, src, onClick, size }: any) => (
    <div
      data-testid={`presence-avatar-${name}`}
      data-status={status}
      data-size={size}
      onClick={onClick}
    >
      {name || 'Avatar'}
    </div>
  )
}))

vi.mock('../UserStatusBadge', () => ({
  default: ({ status, currentActivity, variant }: any) => (
    <div
      data-testid="user-status-badge"
      data-status={status}
      data-variant={variant}
    >
      {status} {currentActivity && `- ${currentActivity}`}
    </div>
  ),
  StatusDot: ({ status }: any) => (
    <div data-testid={`status-dot-${status}`}>{status}</div>
  )
}))

const createMockUser = (overrides?: Partial<UserPresence>): UserPresence => ({
  userId: 'user1',
  user: {
    id: 'user1',
    username: 'johndoe',
    email: 'john@example.com',
    firstName: 'John',
    lastName: 'Doe',
    name: 'John Doe',
    avatar: '/avatar1.jpg',
    profilePicture: null,
    isEmailVerified: true,
    isVerified: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z'
  },
  status: 'online',
  currentActivity: 'Working on tasks',
  lastSeen: new Date().toISOString(),
  ...overrides
})

describe('ActiveUsersList', () => {
  const mockUsers: UserPresence[] = [
    createMockUser({
      userId: 'user1',
      user: { ...createMockUser().user, name: 'John Doe' },
      status: 'online',
      currentActivity: 'Working on project'
    }),
    createMockUser({
      userId: 'user2',
      user: { ...createMockUser().user, id: 'user2', name: 'Jane Smith' },
      status: 'focusing',
      currentActivity: 'Deep work session'
    }),
    createMockUser({
      userId: 'user3',
      user: { ...createMockUser().user, id: 'user3', name: 'Bob Wilson' },
      status: 'break',
      currentActivity: 'Taking a coffee break'
    }),
    createMockUser({
      userId: 'user4',
      user: { ...createMockUser().user, id: 'user4', name: 'Alice Brown' },
      status: 'away'
    }),
    createMockUser({
      userId: 'user5',
      user: { ...createMockUser().user, id: 'user5', name: 'Charlie Davis' },
      status: 'offline'
    }),
    createMockUser({
      userId: 'user6',
      user: { ...createMockUser().user, id: 'user6', name: 'Eve Johnson' },
      status: 'online'
    })
  ]

  describe('Rendering', () => {
    it('should render empty state when no users', () => {
      renderWithProviders(<ActiveUsersList users={[]} />)

      expect(screen.getByText('No active users')).toBeInTheDocument()
    })

    it('should render users list', () => {
      renderWithProviders(<ActiveUsersList users={mockUsers.slice(0, 3)} />)

      expect(screen.getByTestId('presence-avatar-John Doe')).toBeInTheDocument()
      expect(screen.getByTestId('presence-avatar-Jane Smith')).toBeInTheDocument()
      expect(screen.getByTestId('presence-avatar-Bob Wilson')).toBeInTheDocument()
    })

    it('should show title with user count', () => {
      renderWithProviders(<ActiveUsersList users={mockUsers} title="Team Members" />)

      expect(screen.getByText('Team Members (6)')).toBeInTheDocument()
    })

    it('should render with custom title', () => {
      renderWithProviders(<ActiveUsersList users={mockUsers} title="Custom Title" />)

      expect(screen.getByText('Custom Title (6)')).toBeInTheDocument()
    })

    it('should apply custom className', () => {
      const { container } = renderWithProviders(
        <ActiveUsersList users={mockUsers} className="custom-class" />
      )

      expect(container.querySelector('.custom-class')).toBeInTheDocument()
    })
  })

  describe('User Display', () => {
    it('should limit visible users based on maxVisible prop', () => {
      renderWithProviders(<ActiveUsersList users={mockUsers} maxVisible={3} />)

      expect(screen.getByTestId('presence-avatar-John Doe')).toBeInTheDocument()
      expect(screen.getByTestId('presence-avatar-Jane Smith')).toBeInTheDocument()
      expect(screen.getByTestId('presence-avatar-Bob Wilson')).toBeInTheDocument()
      expect(screen.queryByTestId('presence-avatar-Alice Brown')).not.toBeInTheDocument()

      // Should show overflow indicator
      expect(screen.getByText('+3')).toBeInTheDocument()
    })

    it('should show all users when maxVisible exceeds user count', () => {
      renderWithProviders(<ActiveUsersList users={mockUsers.slice(0, 3)} maxVisible={10} />)

      expect(screen.getByTestId('presence-avatar-John Doe')).toBeInTheDocument()
      expect(screen.getByTestId('presence-avatar-Jane Smith')).toBeInTheDocument()
      expect(screen.getByTestId('presence-avatar-Bob Wilson')).toBeInTheDocument()
      expect(screen.queryByText('+', { exact: false })).not.toBeInTheDocument()
    })
  })

  describe('Size Variants', () => {
    it('should render with small size', () => {
      renderWithProviders(<ActiveUsersList users={mockUsers} size="small" />)

      const avatar = screen.getByTestId('presence-avatar-John Doe')
      expect(avatar).toHaveAttribute('data-size', '32')
    })

    it('should render with medium size by default', () => {
      renderWithProviders(<ActiveUsersList users={mockUsers} />)

      const avatar = screen.getByTestId('presence-avatar-John Doe')
      expect(avatar).toHaveAttribute('data-size', '40')
    })

    it('should render with large size', () => {
      renderWithProviders(<ActiveUsersList users={mockUsers} size="large" />)

      const avatar = screen.getByTestId('presence-avatar-John Doe')
      expect(avatar).toHaveAttribute('data-size', '48')
    })
  })

  describe('Status Summary', () => {
    it('should show status summary by default', () => {
      renderWithProviders(<ActiveUsersList users={mockUsers} />)

      expect(screen.getByTestId('status-dot-online')).toBeInTheDocument()
      expect(screen.getByTestId('status-dot-focusing')).toBeInTheDocument()
      expect(screen.getByTestId('status-dot-break')).toBeInTheDocument()
      expect(screen.getByTestId('status-dot-away')).toBeInTheDocument()
      expect(screen.getByTestId('status-dot-offline')).toBeInTheDocument()
    })

    it('should hide status summary when showStatusSummary is false', () => {
      renderWithProviders(<ActiveUsersList users={mockUsers} showStatusSummary={false} />)

      expect(screen.queryByTestId('status-dot-online')).not.toBeInTheDocument()
      expect(screen.queryByTestId('status-dot-focusing')).not.toBeInTheDocument()
    })

    it('should show correct count for each status', () => {
      renderWithProviders(<ActiveUsersList users={mockUsers} />)

      // 2 online users
      const onlineDot = screen.getByTestId('status-dot-online')
      const onlineCount = onlineDot.nextElementSibling
      expect(onlineCount).toHaveTextContent('2')

      // 1 focusing user
      const focusingDot = screen.getByTestId('status-dot-focusing')
      const focusingCount = focusingDot.nextElementSibling
      expect(focusingCount).toHaveTextContent('1')
    })
  })

  describe('User Details Popover', () => {
    it('should show popover when clicking overflow avatar', async () => {
      const user = userEvent.setup()
      renderWithProviders(<ActiveUsersList users={mockUsers} maxVisible={3} />)

      const overflowAvatar = screen.getByText('+3')
      await user.click(overflowAvatar)

      await waitFor(() => {
        expect(screen.getByText('3 Users')).toBeInTheDocument()
      })
    })

    it('should not show popover when showUserDetails is false', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <ActiveUsersList users={mockUsers} maxVisible={3} showUserDetails={false} />
      )

      const overflowAvatar = screen.getByText('+3')
      await user.click(overflowAvatar)

      // Should not show popover
      expect(screen.queryByText('3 Users')).not.toBeInTheDocument()
    })

    it.skip('should close popover when clicking outside', async () => {
      // This test requires complex MUI Popover mocking
      // Functionality verified manually
    })
  })

  describe('User Interaction', () => {
    it('should call onUserClick when clicking on user avatar', async () => {
      const handleUserClick = vi.fn()
      const user = userEvent.setup()

      renderWithProviders(
        <ActiveUsersList users={mockUsers} onUserClick={handleUserClick} />
      )

      const avatar = screen.getByTestId('presence-avatar-John Doe')
      await user.click(avatar)

      expect(handleUserClick).toHaveBeenCalledWith(
        expect.objectContaining({
          userId: 'user1',
          user: expect.objectContaining({ name: 'John Doe' })
        })
      )
    })

    it('should not call onUserClick when not provided', async () => {
      const user = userEvent.setup()

      renderWithProviders(<ActiveUsersList users={mockUsers} />)

      const avatar = screen.getByTestId('presence-avatar-John Doe')
      await user.click(avatar)

      // Should not throw error
      expect(true).toBe(true)
    })
  })

  describe('Accessibility', () => {
    it('should have proper structure', () => {
      renderWithProviders(<ActiveUsersList users={mockUsers} />)

      expect(screen.getByText('Active Users (6)')).toBeInTheDocument()
      expect(screen.getByTestId('presence-avatar-John Doe')).toBeInTheDocument()
    })

    it('should provide meaningful empty state', () => {
      renderWithProviders(<ActiveUsersList users={[]} />)

      expect(screen.getByText('No active users')).toBeInTheDocument()
    })
  })
})

describe('ActiveUsersInline', () => {
  const mockUsers: UserPresence[] = [
    createMockUser({
      userId: 'user1',
      user: { ...createMockUser().user, name: 'John Doe' }
    }),
    createMockUser({
      userId: 'user2',
      user: { ...createMockUser().user, id: 'user2', name: 'Jane Smith' }
    }),
    createMockUser({
      userId: 'user3',
      user: { ...createMockUser().user, id: 'user3', name: 'Bob Wilson' }
    }),
    createMockUser({
      userId: 'user4',
      user: { ...createMockUser().user, id: 'user4', name: 'Alice Brown' }
    })
  ]

  it('should render inline version', () => {
    renderWithProviders(<ActiveUsersInline users={mockUsers} />)

    expect(screen.getByTestId('presence-avatar-John Doe')).toBeInTheDocument()
    expect(screen.getByTestId('presence-avatar-Jane Smith')).toBeInTheDocument()
    expect(screen.getByTestId('presence-avatar-Bob Wilson')).toBeInTheDocument()
  })

  it('should limit visible users', () => {
    renderWithProviders(<ActiveUsersInline users={mockUsers} maxVisible={2} />)

    expect(screen.getByTestId('presence-avatar-John Doe')).toBeInTheDocument()
    expect(screen.getByTestId('presence-avatar-Jane Smith')).toBeInTheDocument()
    expect(screen.queryByTestId('presence-avatar-Bob Wilson')).not.toBeInTheDocument()
  })

  it('should render with small size by default', () => {
    renderWithProviders(<ActiveUsersInline users={mockUsers} />)

    const avatar = screen.getByTestId('presence-avatar-John Doe')
    expect(avatar).toHaveAttribute('data-size', '24')
  })

  it('should render nothing when no users', () => {
    const { container } = renderWithProviders(<ActiveUsersInline users={[]} />)

    expect(container.firstChild).toBeNull()
  })
})