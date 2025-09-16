import React from 'react'
import {act, renderWithProviders, screen, userEvent, waitFor} from '../../../test-utils/test-utils'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {Menu} from '@mui/material'
import {TimerProvider, useTimer} from '../contexts/TimerContext'
import {TimerSettings} from '../../../shared/types/timer'
import type {User} from '../../../shared/types/auth'

// Mock WebSocket and Presence contexts
vi.mock('../../../shared/contexts/WebSocketContext', () => ({
  useWebSocket: () => ({
    isConnected: true,
    emit: vi.fn(),
    on: vi.fn(() => () => {
    }),
  }),
}))

vi.mock('../../../shared/contexts/PresenceContext', () => ({
  usePresence: () => ({
    currentPresence: {hiveId: 'test-hive-123'},
    updatePresence: vi.fn(),
  }),
}))

// Mock user
const mockUser: User = {
  id: 'user-123',
  email: 'testuser@example.com',
  username: 'testuser',
  firstName: 'Test',
  lastName: 'User',
  name: 'Test User',
  isEmailVerified: true,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
}

// TimerSettingsMenu component extracted for testing
interface TimerSettingsMenuProps {
  anchorEl: HTMLElement | null
  open: boolean
  onClose: () => void
}

const TimerSettingsMenu: React.FC<TimerSettingsMenuProps> = ({anchorEl, open, onClose}) => {
  const {timerSettings, updateSettings} = useTimer()

  const handleToggleSound = (): void => {
    updateSettings({soundEnabled: !timerSettings.soundEnabled})
  }

  const handleToggleNotifications = (): void => {
    updateSettings({notificationsEnabled: !timerSettings.notificationsEnabled})
  }

  return (
      <Menu anchorEl={anchorEl} open={open} onClose={onClose} data-testid="timer-settings-menu">
        <div
            role="menuitem"
            onClick={handleToggleSound}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                handleToggleSound();
              }
            }}
            tabIndex={0}
            data-testid="sound-toggle"
            style={{padding: '8px 16px', cursor: 'pointer'}}
        >
          {timerSettings.soundEnabled ? 'Disable Sound' : 'Enable Sound'}
        </div>
        <div
            role="menuitem"
            onClick={handleToggleNotifications}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                handleToggleNotifications();
              }
            }}
            tabIndex={0}
            data-testid="notifications-toggle"
            style={{padding: '8px 16px', cursor: 'pointer'}}
        >
          {timerSettings.notificationsEnabled ? 'Disable Notifications' : 'Enable Notifications'}
        </div>
        <div style={{borderTop: '1px solid #e0e0e0', margin: '8px 0'}}/>
        <div
            role="menuitem"
            onClick={onClose}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                onClose();
              }
            }}
            tabIndex={0}
            data-testid="advanced-settings"
            style={{padding: '8px 16px', cursor: 'pointer'}}
        >
          Advanced Settings
        </div>
      </Menu>
  )
}

// Test wrapper component
const TestWrapper: React.FC<{
  children: React.ReactNode
  initialSettings?: Partial<TimerSettings>
}> = ({children, initialSettings: _initialSettings}) => {
  return (
      <TimerProvider userId={mockUser.id}>
        {children}
      </TimerProvider>
  )
}

// Test component to control menu state and inspect timer settings
const MenuController: React.FC<{
  initialSettings?: Partial<TimerSettings>
}> = ({initialSettings: _initialSettings}) => {
  const [anchorEl, setAnchorEl] = React.useState<HTMLElement | null>(null)
  const {timerSettings} = useTimer()

  const handleClick = (event: React.MouseEvent<HTMLElement>): void => {
    setAnchorEl(event.currentTarget)
  }

  const handleClose = (): void => {
    setAnchorEl(null)
  }

  return (
      <div>
        <button
            data-testid="menu-trigger"
            onClick={handleClick}
        >
          Open Settings
        </button>
        <TimerSettingsMenu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleClose}
        />
        <div data-testid="settings-inspector">
          <div data-testid="sound-enabled">{timerSettings.soundEnabled.toString()}</div>
          <div
              data-testid="notifications-enabled">{timerSettings.notificationsEnabled.toString()}</div>
          <div data-testid="focus-length">{timerSettings.focusLength}</div>
          <div data-testid="short-break-length">{timerSettings.shortBreakLength}</div>
          <div data-testid="long-break-length">{timerSettings.longBreakLength}</div>
          <div data-testid="auto-start-breaks">{timerSettings.autoStartBreaks.toString()}</div>
          <div data-testid="auto-start-focus">{timerSettings.autoStartFocus.toString()}</div>
        </div>
      </div>
  )
}

describe('TimerSettingsMenu Component', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  describe('Rendering', () => {
    it('renders without crashing when closed', () => {
      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      expect(screen.getByTestId('menu-trigger')).toBeInTheDocument()
      expect(screen.queryByTestId('timer-settings-menu')).not.toBeInTheDocument()
    })

    it('renders menu items when open', async () => {
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      const trigger = screen.getByTestId('menu-trigger')
      await act(async () => {
        await user.click(trigger)
      })

      expect(screen.getByTestId('timer-settings-menu')).toBeInTheDocument()
      expect(screen.getByTestId('sound-toggle')).toBeInTheDocument()
      expect(screen.getByTestId('notifications-toggle')).toBeInTheDocument()
      expect(screen.getByTestId('advanced-settings')).toBeInTheDocument()
    })

    it('displays correct initial sound setting text', async () => {
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      const trigger = screen.getByTestId('menu-trigger')
      await act(async () => {
        await user.click(trigger)
      })

      // Sound is enabled by default, so should show "Disable Sound"
      expect(screen.getByText('Disable Sound')).toBeInTheDocument()
    })

    it('displays correct initial notification setting text', async () => {
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      const trigger = screen.getByTestId('menu-trigger')
      await act(async () => {
        await user.click(trigger)
      })

      // Notifications are enabled by default, so should show "Disable Notifications"
      expect(screen.getByText('Disable Notifications')).toBeInTheDocument()
    })
  })

  describe('Sound Setting Toggle', () => {
    it('toggles sound setting when clicked', async () => {
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Initial state - sound enabled
      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('true')

      // Open menu
      const trigger = screen.getByTestId('menu-trigger')
      await act(async () => {
        await user.click(trigger)
      })

      // Click sound toggle
      const soundToggle = screen.getByTestId('sound-toggle')
      await act(async () => {
        await user.click(soundToggle)
      })

      // Sound should be disabled
      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('false')
    })

    it('updates toggle text when sound setting changes', async () => {
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Open menu
      const trigger = screen.getByTestId('menu-trigger')
      await act(async () => {
        await user.click(trigger)
      })

      expect(screen.getByText('Disable Sound')).toBeInTheDocument()

      // Click sound toggle
      const soundToggle = screen.getByTestId('sound-toggle')
      await act(async () => {
        await user.click(soundToggle)
      })

      // Reopen menu to see updated text
      await act(async () => {
        await user.click(trigger)
      })

      expect(screen.getByText('Enable Sound')).toBeInTheDocument()
    })

    it('can toggle sound setting multiple times', async () => {
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('true')

      // Toggle off
      const trigger = screen.getByTestId('menu-trigger')
      await act(async () => {
        await user.click(trigger)
      })

      let soundToggle = screen.getByTestId('sound-toggle')
      await act(async () => {
        await user.click(soundToggle)
      })

      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('false')

      // Toggle back on
      await act(async () => {
        await user.click(trigger)
      })

      soundToggle = screen.getByTestId('sound-toggle')
      await act(async () => {
        await user.click(soundToggle)
      })

      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('true')
    })
  })

  describe('Notification Setting Toggle', () => {
    it('toggles notification setting when clicked', async () => {
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Initial state - notifications enabled
      expect(screen.getByTestId('notifications-enabled')).toHaveTextContent('true')

      // Open menu
      const trigger = screen.getByTestId('menu-trigger')
      await act(async () => {
        await user.click(trigger)
      })

      // Click notifications toggle
      const notificationsToggle = screen.getByTestId('notifications-toggle')
      await act(async () => {
        await user.click(notificationsToggle)
      })

      // Notifications should be disabled
      expect(screen.getByTestId('notifications-enabled')).toHaveTextContent('false')
    })

    it('updates toggle text when notification setting changes', async () => {
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Open menu
      const trigger = screen.getByTestId('menu-trigger')
      await act(async () => {
        await user.click(trigger)
      })

      expect(screen.getByText('Disable Notifications')).toBeInTheDocument()

      // Click notifications toggle
      const notificationsToggle = screen.getByTestId('notifications-toggle')
      await act(async () => {
        await user.click(notificationsToggle)
      })

      // Reopen menu to see updated text
      await act(async () => {
        await user.click(trigger)
      })

      expect(screen.getByText('Enable Notifications')).toBeInTheDocument()
    })
  })

  describe('Menu Interaction', () => {
    it('closes menu when advanced settings is clicked', async () => {
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Open menu
      const trigger = screen.getByTestId('menu-trigger')
      await act(async () => {
        await user.click(trigger)
      })

      expect(screen.getByTestId('timer-settings-menu')).toBeInTheDocument()

      // Click advanced settings
      const advancedSettings = screen.getByTestId('advanced-settings')
      await act(async () => {
        await user.click(advancedSettings)
      })

      // Menu should close
      await waitFor(() => {
        expect(screen.queryByTestId('timer-settings-menu')).not.toBeInTheDocument()
      })
    })

    it('can be opened and closed multiple times', async () => {
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      const trigger = screen.getByTestId('menu-trigger')

      // Open menu
      await act(async () => {
        await user.click(trigger)
      })
      expect(screen.getByTestId('timer-settings-menu')).toBeInTheDocument()

      // Close menu
      const advancedSettings = screen.getByTestId('advanced-settings')
      await act(async () => {
        await user.click(advancedSettings)
      })

      await waitFor(() => {
        expect(screen.queryByTestId('timer-settings-menu')).not.toBeInTheDocument()
      })

      // Open menu again
      await act(async () => {
        await user.click(trigger)
      })
      expect(screen.getByTestId('timer-settings-menu')).toBeInTheDocument()
    })
  })

  describe('Settings Persistence', () => {
    it('persists sound setting changes', async () => {
      const user = userEvent.setup()

      const {unmount} = renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Change sound setting
      const trigger = screen.getByTestId('menu-trigger')
      await act(async () => {
        await user.click(trigger)
      })

      const soundToggle = screen.getByTestId('sound-toggle')
      await act(async () => {
        await user.click(soundToggle)
      })

      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('false')

      // Unmount and remount
      unmount()

      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Setting should be persisted
      await waitFor(() => {
        expect(screen.getByTestId('sound-enabled')).toHaveTextContent('false')
      })
    })

    it('persists notification setting changes', async () => {
      const user = userEvent.setup()

      const {unmount} = renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Change notification setting
      const trigger = screen.getByTestId('menu-trigger')
      await act(async () => {
        await user.click(trigger)
      })

      const notificationsToggle = screen.getByTestId('notifications-toggle')
      await act(async () => {
        await user.click(notificationsToggle)
      })

      expect(screen.getByTestId('notifications-enabled')).toHaveTextContent('false')

      // Unmount and remount
      unmount()

      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Setting should be persisted
      await waitFor(() => {
        expect(screen.getByTestId('notifications-enabled')).toHaveTextContent('false')
      })
    })
  })

  describe('Accessibility', () => {
    it('has proper ARIA roles for menu items', async () => {
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      const trigger = screen.getByTestId('menu-trigger')
      await act(async () => {
        await user.click(trigger)
      })

      const soundToggle = screen.getByTestId('sound-toggle')
      const notificationsToggle = screen.getByTestId('notifications-toggle')
      const advancedSettings = screen.getByTestId('advanced-settings')

      expect(soundToggle).toHaveAttribute('role', 'menuitem')
      expect(notificationsToggle).toHaveAttribute('role', 'menuitem')
      expect(advancedSettings).toHaveAttribute('role', 'menuitem')
    })

    it('supports keyboard navigation', async () => {
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      const trigger = screen.getByTestId('menu-trigger')
      await act(async () => {
        await user.click(trigger)
      })

      const soundToggle = screen.getByTestId('sound-toggle')

      // Focus the menu item
      soundToggle.focus()
      expect(soundToggle).toHaveFocus()

      // Activate with Enter key
      await act(async () => {
        await user.keyboard('{Enter}')
      })

      // Setting should toggle
      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('false')
    })
  })

  describe('Edge Cases', () => {
    it('handles rapid clicking on toggles', async () => {
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      const trigger = screen.getByTestId('menu-trigger')
      await act(async () => {
        await user.click(trigger)
      })

      const soundToggle = screen.getByTestId('sound-toggle')

      // Rapid clicks
      await act(async () => {
        await user.click(soundToggle)
        await user.click(soundToggle)
        await user.click(soundToggle)
      })

      // Should handle gracefully and show final state
      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('false')
    })

    it('handles menu closing during interaction', async () => {
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      const trigger = screen.getByTestId('menu-trigger')
      await act(async () => {
        await user.click(trigger)
      })

      expect(screen.getByTestId('timer-settings-menu')).toBeInTheDocument()

      // Click outside to close menu (simulate with advanced settings)
      const advancedSettings = screen.getByTestId('advanced-settings')
      await act(async () => {
        await user.click(advancedSettings)
      })

      await waitFor(() => {
        expect(screen.queryByTestId('timer-settings-menu')).not.toBeInTheDocument()
      })
    })
  })

  describe('Default Settings Verification', () => {
    it('shows correct default timer settings', () => {
      renderWithProviders(
          <TestWrapper>
            <MenuController/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Verify default Pomodoro settings
      expect(screen.getByTestId('focus-length')).toHaveTextContent('25')
      expect(screen.getByTestId('short-break-length')).toHaveTextContent('5')
      expect(screen.getByTestId('long-break-length')).toHaveTextContent('15')
      expect(screen.getByTestId('auto-start-breaks')).toHaveTextContent('false')
      expect(screen.getByTestId('auto-start-focus')).toHaveTextContent('false')
      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('true')
      expect(screen.getByTestId('notifications-enabled')).toHaveTextContent('true')
    })
  })
})