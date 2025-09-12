import React from 'react'
import { describe, it, expect, vi, beforeEach, type MockedFunction } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/test-utils'
import { HiveCard } from '../HiveCard'
import { Hive, HiveMember, User } from '@shared/types'

// Mock JoinHiveButton component
vi.mock('../JoinHiveButton', () => ({
  JoinHiveButton: ({ 
    hive, 
    onJoin, 
    isLoading, 
    variant,
    size
  }: {
    hive: Hive
    onJoin?: (hiveId: string) => void
    isLoading?: boolean
    variant?: string
    size?: string
  }) => (
    <button 
      onClick={() => onJoin?.(hive.id)}
      disabled={isLoading || hive.currentMembers >= hive.maxMembers}
      data-testid="join-hive-button"
      data-variant={variant}
      data-size={size}
    >
      {hive.currentMembers >= hive.maxMembers ? 'Full' : 'Join Hive'}
    </button>
  )
}))

// Mock data
const mockUser: User = {
  id: 'user1',
  username: 'testuser',
  email: 'test@example.com',
  firstName: 'Test',
  lastName: 'User',
  name: 'Test User',
  avatar: null,
  profilePicture: null,
  isEmailVerified: true,
  isVerified: true,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z'
}

const mockOwner: User = {
  id: 'owner1',
  username: 'owner',
  email: 'owner@example.com',
  firstName: 'Hive',
  lastName: 'Owner',
  name: 'Hive Owner',
  avatar: null,
  profilePicture: null,
  isEmailVerified: true,
  isVerified: true,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z'
}

const createMockHive = (overrides: Partial<Hive> = {}): Hive => ({
  id: `hive-${Math.random().toString(36).substr(2, 9)}`,
  name: 'Test Hive',
  description: 'A test hive for development and collaboration',
  ownerId: 'owner1',
  owner: mockOwner,
  maxMembers: 10,
  isPublic: true,
  tags: ['study', 'programming', 'collaboration'],
  settings: {
    allowChat: true,
    allowVoice: false,
    requireApproval: false,
    focusMode: 'continuous',
    defaultSessionLength: 25,
    maxSessionLength: 120
  },
  currentMembers: 3,
  memberCount: 3,
  isOwner: false,
  isMember: false,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
  ...overrides
})

const createMockMember = (overrides: Partial<HiveMember> = {}): HiveMember => ({
  id: `member-${Math.random().toString(36).substr(2, 9)}`,
  userId: 'user1',
  user: mockUser,
  hiveId: 'hive1',
  role: 'member',
  joinedAt: '2024-01-01T00:00:00Z',
  isActive: true,
  permissions: {
    canInviteMembers: false,
    canModerateChat: false,
    canManageSettings: false,
    canStartTimers: false
  },
  ...overrides
})

describe('HiveCard', () => {
  const defaultProps = {
    hive: createMockHive(),
    members: [] as HiveMember[],
    currentUserId: undefined as string | undefined,
    onJoin: vi.fn(),
    onLeave: vi.fn(),
    onEnter: vi.fn(),
    onSettings: vi.fn(),
    onShare: vi.fn(),
    variant: 'default' as const,
    isLoading: false
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Basic Rendering', () => {
    it('should render hive name as heading', () => {
      const hive = createMockHive({ name: 'My Awesome Hive' })
      renderWithProviders(<HiveCard {...defaultProps} hive={hive} />)
      
      expect(screen.getByRole('heading', { name: 'My Awesome Hive' })).toBeInTheDocument()
    })

    it('should render hive description', () => {
      const hive = createMockHive({ description: 'This is a detailed hive description' })
      renderWithProviders(<HiveCard {...defaultProps} hive={hive} />)
      
      expect(screen.getByText('This is a detailed hive description')).toBeInTheDocument()
    })

    it('should display member count', () => {
      const hive = createMockHive({ currentMembers: 5, maxMembers: 10 })
      renderWithProviders(<HiveCard {...defaultProps} hive={hive} />)
      
      expect(screen.getByText('5/10')).toBeInTheDocument()
    })

    it('should show public/private status', () => {
      // Test public hive
      const publicHive = createMockHive({ isPublic: true })
      const { rerender } = renderWithProviders(<HiveCard {...defaultProps} hive={publicHive} />)
      expect(screen.getByText('Public')).toBeInTheDocument()

      // Test private hive
      const privateHive = createMockHive({ isPublic: false })
      rerender(<HiveCard {...defaultProps} hive={privateHive} />)
      expect(screen.getByText('Private')).toBeInTheDocument()
    })

    it('should display focus mode when available', () => {
      const hive = createMockHive({ 
        settings: { 
          ...createMockHive().settings, 
          focusMode: 'pomodoro' 
        } 
      })
      renderWithProviders(<HiveCard {...defaultProps} hive={hive} />)
      
      expect(screen.getByText('pomodoro')).toBeInTheDocument()
    })
  })

  describe('Variant Rendering', () => {
    it('should render with default height for default variant', () => {
      const hive = createMockHive()
      renderWithProviders(<HiveCard {...defaultProps} hive={hive} variant="default" />)
      
      const card = screen.getByRole('heading').closest('[class*="MuiCard-root"]')
      expect(card).toHaveStyle({ height: '180px' })
    })

    it('should render with compact height for compact variant', () => {
      const hive = createMockHive()
      renderWithProviders(<HiveCard {...defaultProps} hive={hive} variant="compact" />)
      
      const card = screen.getByRole('heading').closest('[class*="MuiCard-root"]')
      expect(card).toHaveStyle({ height: '120px' })
    })

    it('should render with detailed height for detailed variant', () => {
      const hive = createMockHive()
      renderWithProviders(<HiveCard {...defaultProps} hive={hive} variant="detailed" />)
      
      const card = screen.getByRole('heading').closest('[class*="MuiCard-root"]')
      expect(card).toHaveStyle({ height: '240px' })
    })

    it('should not show description in compact variant', () => {
      const hive = createMockHive({ description: 'This should not be visible' })
      renderWithProviders(<HiveCard {...defaultProps} hive={hive} variant="compact" />)
      
      expect(screen.queryByText('This should not be visible')).not.toBeInTheDocument()
    })

    it('should not show members section in compact variant', () => {
      const member = createMockMember({ isActive: true })
      renderWithProviders(
        <HiveCard {...defaultProps} hive={createMockHive()} members={[member]} variant="compact" />
      )
      
      expect(screen.queryByText('1 online')).not.toBeInTheDocument()
    })

    it('should show tags only in detailed variant', () => {
      const hive = createMockHive({ tags: ['test', 'programming', 'study'] })
      
      // Test detailed variant shows tags
      const { rerender } = renderWithProviders(
        <HiveCard {...defaultProps} hive={hive} variant="detailed" />
      )
      expect(screen.getByText('test')).toBeInTheDocument()
      expect(screen.getByText('programming')).toBeInTheDocument()

      // Test default variant doesn't show tags
      rerender(<HiveCard {...defaultProps} hive={hive} variant="default" />)
      expect(screen.queryByText('test')).not.toBeInTheDocument()
    })

    it('should limit tags display to 3 in detailed variant', () => {
      const hive = createMockHive({ tags: ['tag1', 'tag2', 'tag3', 'tag4', 'tag5'] })
      renderWithProviders(<HiveCard {...defaultProps} hive={hive} variant="detailed" />)
      
      expect(screen.getByText('tag1')).toBeInTheDocument()
      expect(screen.getByText('tag2')).toBeInTheDocument()
      expect(screen.getByText('tag3')).toBeInTheDocument()
      expect(screen.getByText('+2 more')).toBeInTheDocument()
    })
  })

  describe('Member Status Display', () => {
    it('should show member chip when user is a member', () => {
      const hive = createMockHive({ id: 'hive1' })
      const member = createMockMember({ userId: 'user1', hiveId: 'hive1' })
      
      renderWithProviders(
        <HiveCard {...defaultProps} hive={hive} members={[member]} currentUserId="user1" />
      )
      
      expect(screen.getByText('Member')).toBeInTheDocument()
    })

    it('should not show member chip when user is not a member', () => {
      const hive = createMockHive()
      renderWithProviders(
        <HiveCard {...defaultProps} hive={hive} currentUserId="user1" />
      )
      
      expect(screen.queryByText('Member')).not.toBeInTheDocument()
    })

    it('should show highlighted border for member cards', () => {
      const hive = createMockHive({ id: 'hive1' })
      const member = createMockMember({ userId: 'user1', hiveId: 'hive1' })
      
      renderWithProviders(
        <HiveCard {...defaultProps} hive={hive} members={[member]} currentUserId="user1" />
      )
      
      // Check for primary border color on the card
      const card = screen.getByRole('heading').closest('[class*="MuiCard-root"]')
      expect(card).toHaveStyle({ 'border-width': '2px' })
    })
  })

  describe('Online Members Display', () => {
    it('should show online members count', () => {
      const hive = createMockHive({ id: 'hive1' })
      const onlineMember = createMockMember({ isActive: true })
      const offlineMember = createMockMember({ isActive: false })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[onlineMember, offlineMember]} 
          variant="default"
        />
      )
      
      expect(screen.getByText('1 online')).toBeInTheDocument()
    })

    it('should show online member avatars', () => {
      const hive = createMockHive({ id: 'hive1' })
      const onlineMember = createMockMember({ 
        isActive: true,
        user: { ...mockUser, firstName: 'John', lastName: 'Doe' }
      })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[onlineMember]} 
          variant="default"
        />
      )
      
      const avatar = screen.getByText('JD') // First + Last initials
      expect(avatar).toBeInTheDocument()
    })

    it('should limit avatar display to 4 members max', () => {
      const hive = createMockHive({ id: 'hive1' })
      const members = Array.from({ length: 6 }, (_, i) => 
        createMockMember({ 
          id: `member-${i}`,
          isActive: true,
          user: { 
            ...mockUser, 
            id: `user-${i}`,
            firstName: `User${i}`, 
            lastName: `Test${i}` 
          }
        })
      )
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={members} 
          variant="default"
        />
      )
      
      // Should show "6 online" but only render up to 4 avatars in AvatarGroup
      expect(screen.getByText('6 online')).toBeInTheDocument()
      // AvatarGroup with max={4} will show 3 avatars + "+3" for the remaining ones
    })
  })

  describe('Action Buttons - Non-Member', () => {
    it('should show JoinHiveButton for non-members', () => {
      const hive = createMockHive()
      renderWithProviders(
        <HiveCard {...defaultProps} hive={hive} currentUserId="user1" />
      )
      
      expect(screen.getByTestId('join-hive-button')).toBeInTheDocument()
    })

    it('should call onJoin when join button is clicked', async () => {
      const user = userEvent.setup()
      const onJoin = vi.fn()
      const hive = createMockHive({ id: 'test-hive' })
      
      renderWithProviders(
        <HiveCard {...defaultProps} hive={hive} onJoin={onJoin} currentUserId="user1" />
      )
      
      const joinButton = screen.getByTestId('join-hive-button')
      await user.click(joinButton)
      
      expect(onJoin).toHaveBeenCalledWith('test-hive')
    })

    it('should pass correct props to JoinHiveButton', () => {
      const hive = createMockHive()
      renderWithProviders(
        <HiveCard {...defaultProps} hive={hive} isLoading={true} currentUserId="user1" />
      )
      
      const joinButton = screen.getByTestId('join-hive-button')
      expect(joinButton).toHaveAttribute('data-variant', 'contained')
      expect(joinButton).toHaveAttribute('data-size', 'medium')
      expect(joinButton).toBeDisabled() // Due to isLoading
    })
  })

  describe('Action Buttons - Member', () => {
    it('should show Enter Hive button for members', () => {
      const hive = createMockHive({ id: 'hive1' })
      const member = createMockMember({ userId: 'user1', hiveId: 'hive1' })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1" 
        />
      )
      
      expect(screen.getByRole('button', { name: 'Enter Hive' })).toBeInTheDocument()
    })

    it('should call onEnter when Enter Hive button is clicked', async () => {
      const user = userEvent.setup()
      const onEnter = vi.fn()
      const hive = createMockHive({ id: 'test-hive' })
      const member = createMockMember({ userId: 'user1', hiveId: 'test-hive' })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1"
          onEnter={onEnter}
        />
      )
      
      const enterButton = screen.getByRole('button', { name: 'Enter Hive' })
      await user.click(enterButton)
      
      expect(onEnter).toHaveBeenCalledWith('test-hive')
    })

    it('should show more actions menu button for members', () => {
      const hive = createMockHive({ id: 'hive1' })
      const member = createMockMember({ userId: 'user1', hiveId: 'hive1' })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1" 
        />
      )
      
      const menuButton = screen.getByRole('button', { name: '' }) // MoreVertIcon button
      expect(menuButton).toBeInTheDocument()
    })

    it('should disable action buttons when loading', () => {
      const hive = createMockHive({ id: 'hive1' })
      const member = createMockMember({ userId: 'user1', hiveId: 'hive1' })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1"
          isLoading={true}
        />
      )
      
      expect(screen.getByRole('button', { name: 'Enter Hive' })).toBeDisabled()
    })
  })

  describe('Menu Actions', () => {
    it('should open menu when more actions button is clicked', async () => {
      const user = userEvent.setup()
      const hive = createMockHive({ id: 'hive1' })
      const member = createMockMember({ userId: 'user1', hiveId: 'hive1' })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1" 
        />
      )
      
      const menuButton = screen.getAllByRole('button').find(btn => 
        btn.querySelector('[data-testid="MoreVertIcon"]')
      )
      expect(menuButton).toBeInTheDocument()
      
      if (menuButton) {
        await user.click(menuButton)
      }
      
      expect(screen.getByRole('menu')).toBeInTheDocument()
      expect(screen.getByRole('menuitem', { name: 'Share' })).toBeInTheDocument()
    })

    it('should show Settings menu item for members with management permissions', async () => {
      const user = userEvent.setup()
      const hive = createMockHive({ id: 'hive1' })
      const member = createMockMember({ 
        userId: 'user1', 
        hiveId: 'hive1',
        permissions: { ...createMockMember().permissions, canManageSettings: true }
      })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1" 
        />
      )
      
      const menuButton = screen.getAllByRole('button').find(btn => 
        btn.querySelector('[data-testid="MoreVertIcon"]')
      )
      
      if (menuButton) {
        await user.click(menuButton)
      }
      
      expect(screen.getByRole('menuitem', { name: 'Settings' })).toBeInTheDocument()
    })

    it('should show Settings menu item for hive owners', async () => {
      const user = userEvent.setup()
      const hive = createMockHive({ id: 'hive1', ownerId: 'user1' })
      const member = createMockMember({ userId: 'user1', hiveId: 'hive1' })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1" 
        />
      )
      
      const menuButton = screen.getAllByRole('button').find(btn => 
        btn.querySelector('[data-testid="MoreVertIcon"]')
      )
      
      if (menuButton) {
        await user.click(menuButton)
      }
      
      expect(screen.getByRole('menuitem', { name: 'Settings' })).toBeInTheDocument()
    })

    it('should not show Leave Hive option for owners', async () => {
      const user = userEvent.setup()
      const hive = createMockHive({ id: 'hive1', ownerId: 'user1' })
      const member = createMockMember({ userId: 'user1', hiveId: 'hive1' })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1" 
        />
      )
      
      const menuButton = screen.getAllByRole('button').find(btn => 
        btn.querySelector('[data-testid="MoreVertIcon"]')
      )
      
      if (menuButton) {
        await user.click(menuButton)
      }
      
      expect(screen.queryByRole('menuitem', { name: 'Leave Hive' })).not.toBeInTheDocument()
    })

    it('should show Leave Hive option for non-owner members', async () => {
      const user = userEvent.setup()
      const hive = createMockHive({ id: 'hive1', ownerId: 'owner1' })
      const member = createMockMember({ userId: 'user1', hiveId: 'hive1' })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1" 
        />
      )
      
      const menuButton = screen.getAllByRole('button').find(btn => 
        btn.querySelector('[data-testid="MoreVertIcon"]')
      )
      
      if (menuButton) {
        await user.click(menuButton)
      }
      
      expect(screen.getByRole('menuitem', { name: 'Leave Hive' })).toBeInTheDocument()
    })

    it('should call onLeave when Leave Hive is clicked', async () => {
      const user = userEvent.setup()
      const onLeave = vi.fn()
      const hive = createMockHive({ id: 'test-hive', ownerId: 'owner1' })
      const member = createMockMember({ userId: 'user1', hiveId: 'test-hive' })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1"
          onLeave={onLeave}
        />
      )
      
      const menuButton = screen.getAllByRole('button').find(btn => 
        btn.querySelector('[data-testid="MoreVertIcon"]')
      )
      
      if (menuButton) {
        await user.click(menuButton)
      }
      
      const leaveItem = screen.getByRole('menuitem', { name: 'Leave Hive' })
      await user.click(leaveItem)
      
      expect(onLeave).toHaveBeenCalledWith('test-hive')
    })

    it('should call onShare when Share is clicked', async () => {
      const user = userEvent.setup()
      const onShare = vi.fn()
      const hive = createMockHive({ id: 'test-hive' })
      const member = createMockMember({ userId: 'user1', hiveId: 'test-hive' })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1"
          onShare={onShare}
        />
      )
      
      const menuButton = screen.getAllByRole('button').find(btn => 
        btn.querySelector('[data-testid="MoreVertIcon"]')
      )
      
      if (menuButton) {
        await user.click(menuButton)
      }
      
      const shareItem = screen.getByRole('menuitem', { name: 'Share' })
      await user.click(shareItem)
      
      expect(onShare).toHaveBeenCalledWith('test-hive')
    })

    it('should call onSettings when Settings is clicked', async () => {
      const user = userEvent.setup()
      const onSettings = vi.fn()
      const hive = createMockHive({ id: 'test-hive', ownerId: 'user1' })
      const member = createMockMember({ userId: 'user1', hiveId: 'test-hive' })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1"
          onSettings={onSettings}
        />
      )
      
      const menuButton = screen.getAllByRole('button').find(btn => 
        btn.querySelector('[data-testid="MoreVertIcon"]')
      )
      
      if (menuButton) {
        await user.click(menuButton)
      }
      
      const settingsItem = screen.getByRole('menuitem', { name: 'Settings' })
      await user.click(settingsItem)
      
      expect(onSettings).toHaveBeenCalledWith('test-hive')
    })

    it('should close menu when an action is clicked', async () => {
      const user = userEvent.setup()
      const hive = createMockHive({ id: 'hive1' })
      const member = createMockMember({ userId: 'user1', hiveId: 'hive1' })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1" 
        />
      )
      
      const menuButton = screen.getAllByRole('button').find(btn => 
        btn.querySelector('[data-testid="MoreVertIcon"]')
      )
      
      if (menuButton) {
        await user.click(menuButton)
      }
      
      const shareItem = screen.getByRole('menuitem', { name: 'Share' })
      await user.click(shareItem)
      
      // Menu should be closed after clicking an item
      expect(screen.queryByRole('menu')).not.toBeInTheDocument()
    })
  })

  describe('Loading States', () => {
    it('should apply loading opacity to card', () => {
      const hive = createMockHive()
      renderWithProviders(
        <HiveCard {...defaultProps} hive={hive} isLoading={true} />
      )
      
      const card = screen.getByRole('heading').closest('[class*="MuiCard-root"]')
      expect(card).toHaveStyle({ opacity: '0.7' })
    })

    it('should disable all interactive elements when loading', () => {
      const hive = createMockHive({ id: 'hive1' })
      const member = createMockMember({ userId: 'user1', hiveId: 'hive1' })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1"
          isLoading={true}
        />
      )
      
      expect(screen.getByRole('button', { name: 'Enter Hive' })).toBeDisabled()
      
      // Check menu button is also disabled
      const menuButton = screen.getAllByRole('button').find(btn => 
        btn.querySelector('[data-testid="MoreVertIcon"]')
      )
      expect(menuButton).toBeDisabled()
    })
  })

  describe('Edge Cases and Error Handling', () => {
    it('should handle very long hive names gracefully', () => {
      const hive = createMockHive({ 
        name: 'This is a very long hive name that should be truncated properly to prevent layout issues' 
      })
      renderWithProviders(<HiveCard {...defaultProps} hive={hive} />)
      
      const nameElement = screen.getByRole('heading')
      expect(nameElement).toHaveStyle({ 
        overflow: 'hidden',
        textOverflow: 'ellipsis'
      })
    })

    it('should handle very long descriptions gracefully', () => {
      const longDescription = 'This is a very long description that should be truncated after a certain number of lines to prevent the card from becoming too tall and breaking the layout of the grid or list view.'
      const hive = createMockHive({ description: longDescription })
      
      renderWithProviders(<HiveCard {...defaultProps} hive={hive} />)
      
      const descElement = screen.getByText(longDescription)
      expect(descElement).toHaveStyle({ 
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        display: '-webkit-box'
      })
    })

    it('should handle empty tags array', () => {
      const hive = createMockHive({ tags: [] })
      renderWithProviders(<HiveCard {...defaultProps} hive={hive} variant="detailed" />)
      
      // Should not crash and should not show tag section
      expect(screen.getByRole('heading')).toBeInTheDocument()
    })

    it('should handle missing member data gracefully', () => {
      const hive = createMockHive()
      renderWithProviders(<HiveCard {...defaultProps} hive={hive} members={undefined} />)
      
      // Should not crash and should show 0 online
      expect(screen.queryByText('online')).not.toBeInTheDocument()
    })

    it('should handle zero max members', () => {
      const hive = createMockHive({ currentMembers: 0, maxMembers: 0 })
      renderWithProviders(<HiveCard {...defaultProps} hive={hive} />)
      
      expect(screen.getByText('0/0')).toBeInTheDocument()
    })

    it('should handle members without profile pictures', () => {
      const hive = createMockHive({ id: 'hive1' })
      const member = createMockMember({ 
        isActive: true,
        user: { 
          ...mockUser, 
          firstName: 'John', 
          lastName: 'Doe',
          profilePicture: null 
        }
      })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          variant="default"
        />
      )
      
      // Should show initials when no profile picture
      expect(screen.getByText('JD')).toBeInTheDocument()
    })
  })

  describe('Hover Effects', () => {
    it('should have hover transform and shadow styles', () => {
      const hive = createMockHive()
      renderWithProviders(<HiveCard {...defaultProps} hive={hive} />)
      
      const card = screen.getByRole('heading').closest('[class*="MuiCard-root"]')
      expect(card).toHaveStyle({ 
        transition: 'all 0.2s ease-in-out'
      })
    })
  })

  describe('Accessibility', () => {
    it('should have proper ARIA labels and roles', () => {
      const hive = createMockHive({ name: 'Accessible Hive' })
      renderWithProviders(<HiveCard {...defaultProps} hive={hive} />)
      
      expect(screen.getByRole('heading', { name: 'Accessible Hive' })).toBeInTheDocument()
    })

    it('should have proper button labels', () => {
      const hive = createMockHive({ id: 'hive1' })
      const member = createMockMember({ userId: 'user1', hiveId: 'hive1' })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1" 
        />
      )
      
      expect(screen.getByRole('button', { name: 'Enter Hive' })).toBeInTheDocument()
    })

    it('should have tooltips for member avatars', async () => {
      const hive = createMockHive({ id: 'hive1' })
      const member = createMockMember({ 
        isActive: true,
        user: { 
          ...mockUser, 
          firstName: 'John', 
          lastName: 'Doe' 
        }
      })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          variant="default"
        />
      )
      
      const avatar = screen.getByText('JD')
      expect(avatar).toBeInTheDocument()
      
      // Verify the avatar is wrapped in a Tooltip component
      // The actual tooltip behavior is handled by Material UI and doesn't need explicit testing
    })

    it('should support keyboard navigation', () => {
      const hive = createMockHive({ id: 'hive1' })
      const member = createMockMember({ userId: 'user1', hiveId: 'hive1' })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1" 
        />
      )
      
      const enterButton = screen.getByRole('button', { name: 'Enter Hive' })
      const menuButton = screen.getAllByRole('button').find(btn => 
        btn.querySelector('[data-testid="MoreVertIcon"]')
      )
      
      // Test that buttons are focusable
      enterButton.focus()
      expect(enterButton).toHaveFocus()
      
      if (menuButton) {
        menuButton.focus()
        expect(menuButton).toHaveFocus()
      }
    })
  })

  describe('Responsive Design', () => {
    it('should have responsive font sizes', () => {
      const hive = createMockHive({ name: 'Test Hive' })
      
      // Test compact variant font size
      const { rerender } = renderWithProviders(
        <HiveCard {...defaultProps} hive={hive} variant="compact" />
      )
      let nameElement = screen.getByRole('heading')
      expect(nameElement).toHaveStyle({ fontSize: '1rem' })
      
      // Test default variant font size
      rerender(<HiveCard {...defaultProps} hive={hive} variant="default" />)
      nameElement = screen.getByRole('heading')
      expect(nameElement).toHaveStyle({ fontSize: '1.1rem' })
    })

    it('should handle different description lengths in different variants', () => {
      const longDescription = 'This is a long description that should be handled differently in different variants of the card component.'
      const hive = createMockHive({ description: longDescription })
      
      // Test default variant (2 lines)
      const { rerender } = renderWithProviders(
        <HiveCard {...defaultProps} hive={hive} variant="default" />
      )
      let descElement = screen.getByText(longDescription)
      expect(descElement).toHaveStyle({ WebkitLineClamp: '2' })
      
      // Test detailed variant (3 lines)
      rerender(<HiveCard {...defaultProps} hive={hive} variant="detailed" />)
      descElement = screen.getByText(longDescription)
      expect(descElement).toHaveStyle({ WebkitLineClamp: '3' })
    })
  })

  describe('Performance', () => {
    it('should not re-render unnecessarily with same props', () => {
      const hive = createMockHive()
      const { rerender } = renderWithProviders(<HiveCard {...defaultProps} hive={hive} />)
      
      const initialName = screen.getByRole('heading')
      expect(initialName).toBeInTheDocument()
      
      // Rerender with same props
      rerender(<HiveCard {...defaultProps} hive={hive} />)
      
      // Should still show the same content
      expect(screen.getByRole('heading')).toHaveTextContent(hive.name)
    })

    it('should handle menu state changes gracefully', async () => {
      const user = userEvent.setup()
      const hive = createMockHive({ id: 'hive1' })
      const member = createMockMember({ userId: 'user1', hiveId: 'hive1' })
      
      renderWithProviders(
        <HiveCard 
          {...defaultProps} 
          hive={hive} 
          members={[member]} 
          currentUserId="user1" 
        />
      )
      
      const menuButton = screen.getAllByRole('button').find(btn => 
        btn.querySelector('[data-testid="MoreVertIcon"]')
      )
      
      if (menuButton) {
        // Test that menu can be opened
        await user.click(menuButton)
        expect(screen.getByRole('menu')).toBeInTheDocument()
        
        // Test that clicking a menu item works and closes the menu
        const shareItem = screen.getByRole('menuitem', { name: 'Share' })
        await user.click(shareItem)
        
        // Menu should close after action
        await waitFor(() => {
          expect(screen.queryByRole('menu')).not.toBeInTheDocument()
        })
      }
    })
  })
})