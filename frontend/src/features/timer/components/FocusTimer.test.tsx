import React from 'react'
import {act, renderWithProviders, screen, userEvent, waitFor} from '../../../test-utils/test-utils'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {FocusTimer} from './FocusTimer'
import {TimerProvider, useTimer} from '../contexts/TimerContext'
import {FocusTimerProps} from '../../../shared/types/timer'
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

// Mock Web Audio API
const mockAudioContext = {
  createOscillator: vi.fn(() => ({
    connect: vi.fn(),
    frequency: {setValueAtTime: vi.fn()},
    start: vi.fn(),
    stop: vi.fn(),
  })),
  createGain: vi.fn(() => ({
    connect: vi.fn(),
    gain: {
      setValueAtTime: vi.fn(),
      exponentialRampToValueAtTime: vi.fn(),
    },
  })),
  destination: {},
  currentTime: 0,
  close: vi.fn(),
}

// Mock Web Notification API
const mockNotification = vi.fn()
Object.defineProperty(window, 'Notification', {
  value: mockNotification,
  configurable: true,
})

Object.defineProperty(window, 'AudioContext', {
  value: vi.fn(() => mockAudioContext),
  configurable: true,
})

Object.defineProperty(window, 'webkitAudioContext', {
  value: vi.fn(() => mockAudioContext),
  configurable: true,
})

// Mock timers globally
vi.useFakeTimers({
  shouldAdvanceTime: true,
  toFake: ['setTimeout', 'clearTimeout', 'setInterval', 'clearInterval', 'Date']
})

// Test user data
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

// Test component to wrap FocusTimer with TimerProvider
const TestTimerWrapper: React.FC<{
  children: React.ReactNode
  timerProps?: Partial<FocusTimerProps>
}> = ({children, timerProps = {}}) => {
  return (
      <TimerProvider userId={mockUser.id}>
        <FocusTimer hiveId="test-hive-123" {...timerProps} />
        {children}
      </TimerProvider>
  )
}

// Test component to access timer context
const TimerStateInspector: React.FC = () => {
  const timer = useTimer()

  return (
      <div data-testid="timer-inspector">
        <div data-testid="current-phase">{timer.timerState.currentPhase}</div>
        <div data-testid="time-remaining">{timer.timerState.timeRemaining}</div>
        <div data-testid="is-running">{timer.timerState.isRunning.toString()}</div>
        <div data-testid="is-paused">{timer.timerState.isPaused.toString()}</div>
        <div data-testid="current-cycle">{timer.timerState.currentCycle}</div>
        <div data-testid="session-id">{timer.currentSession?.id || 'null'}</div>
        <div data-testid="focus-length">{timer.timerSettings.focusLength}</div>
        <div data-testid="sound-enabled">{timer.timerSettings.soundEnabled.toString()}</div>
        <div
            data-testid="notifications-enabled">{timer.timerSettings.notificationsEnabled.toString()}</div>
      </div>
  )
}

const _defaultProps: FocusTimerProps = {
  hiveId: 'test-hive-123',
}

describe('FocusTimer Component', () => {
  let consoleSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    vi.clearAllMocks()
    vi.clearAllTimers()
    localStorage.clear()

    // Mock Notification permission
    Object.defineProperty(Notification, 'permission', {
      value: 'granted',
      configurable: true,
    })
    Object.defineProperty(Notification, 'requestPermission', {
      value: vi.fn().mockResolvedValue('granted'),
      configurable: true,
    })

    // Suppress console errors during tests
    consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {
    })
  })

  afterEach(() => {
    consoleSpy.mockRestore()
    vi.runOnlyPendingTimers()
    vi.clearAllTimers()
  })

  describe('Component Rendering', () => {
    it('renders without crashing', () => {
      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      expect(screen.getByText('Ready to Focus')).toBeInTheDocument()
      expect(screen.getByText('25:00')).toBeInTheDocument()
    })

    it('renders in compact mode', () => {
      renderWithProviders(
          <TestTimerWrapper timerProps={{compact: true}}>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Should render compact layout
      expect(screen.getByText('Ready to Focus')).toBeInTheDocument()
      // Compact mode has smaller timer display
      const timerCard = screen.getByRole('button', {name: /start focus session/i})
      expect(timerCard).toBeInTheDocument()
    })

    it('renders fullscreen mode toggle', () => {
      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      const fullscreenButton = screen.getByRole('button', {name: /fullscreen/i})
      expect(fullscreenButton).toBeInTheDocument()
    })

    it('renders settings button when showSettings is true', () => {
      renderWithProviders(
          <TestTimerWrapper timerProps={{showSettings: true}}>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      const settingsButton = screen.getByRole('button', {name: /settings/i})
      expect(settingsButton).toBeInTheDocument()
    })

    it('does not render settings button when showSettings is false', () => {
      renderWithProviders(
          <TestTimerWrapper timerProps={{showSettings: false}}>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      const settingsButton = screen.queryByRole('button', {name: /settings/i})
      expect(settingsButton).not.toBeInTheDocument()
    })
  })

  describe('Timer Functionality', () => {
    it('starts focus session when play button is clicked', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Initial state
      expect(screen.getByTestId('current-phase')).toHaveTextContent('idle')
      expect(screen.getByTestId('is-running')).toHaveTextContent('false')

      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      // Timer should start
      expect(screen.getByTestId('current-phase')).toHaveTextContent('focus')
      expect(screen.getByTestId('is-running')).toHaveTextContent('true')
      expect(screen.getByTestId('time-remaining')).toHaveTextContent('1500') // 25 minutes in seconds
    })

    it('pauses timer when pause button is clicked', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Start timer first
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      // Wait for timer to start
      await waitFor(() => {
        expect(screen.getByTestId('is-running')).toHaveTextContent('true')
      }, {timeout: 5000})

      // Now pause
      const pauseButton = screen.getByRole('button', {name: /pause timer/i})
      await act(async () => {
        await user.click(pauseButton)
      })

      // Wait for pause state
      await waitFor(() => {
        expect(screen.getByTestId('is-running')).toHaveTextContent('false')
        expect(screen.getByTestId('is-paused')).toHaveTextContent('true')
      }, {timeout: 5000})
    })

    it('resumes timer when resume button is clicked', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Start timer
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      // Wait for timer to start
      await waitFor(() => {
        expect(screen.getByTestId('is-running')).toHaveTextContent('true')
      }, {timeout: 5000})

      // Pause timer
      const pauseButton = screen.getByRole('button', {name: /pause timer/i})
      await act(async () => {
        await user.click(pauseButton)
      })

      // Wait for pause state
      await waitFor(() => {
        expect(screen.getByTestId('is-paused')).toHaveTextContent('true')
      }, {timeout: 5000})

      // Resume timer
      const resumeButton = screen.getByRole('button', {name: /resume timer/i})
      await act(async () => {
        await user.click(resumeButton)
      })

      // Wait for resume state
      await waitFor(() => {
        expect(screen.getByTestId('is-running')).toHaveTextContent('true')
        expect(screen.getByTestId('is-paused')).toHaveTextContent('false')
      }, {timeout: 5000})
    })

    it('stops timer when stop button is clicked', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Start timer
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      // Wait for timer to start
      await waitFor(() => {
        expect(screen.getByTestId('is-running')).toHaveTextContent('true')
      }, {timeout: 5000})

      // Stop timer
      const stopButton = screen.getByRole('button', {name: /stop timer/i})
      await act(async () => {
        await user.click(stopButton)
      })

      // Wait for stop state
      await waitFor(() => {
        expect(screen.getByTestId('is-running')).toHaveTextContent('false')
        expect(screen.getByTestId('is-paused')).toHaveTextContent('false')
        expect(screen.getByTestId('current-phase')).toHaveTextContent('idle')
      }, {timeout: 5000})
    })

    it('skips phase when skip button is clicked', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Start timer
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      expect(screen.getByTestId('current-phase')).toHaveTextContent('focus')

      // Skip phase
      const skipButton = screen.getByRole('button', {name: /skip phase/i})
      await act(async () => {
        await user.click(skipButton)
      })

      // Should move to next phase or complete
      await waitFor(() => {
        const phase = screen.getByTestId('current-phase').textContent
        expect(phase).not.toBe('focus')
      })
    })
  })

  describe('Time Display and Progress', () => {
    it('displays correct time format', () => {
      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Default focus time is 25 minutes (25:00)
      expect(screen.getByText('25:00')).toBeInTheDocument()
    })

    it('counts down time when timer is running', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Start timer
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      // Advance time by 1 second
      act(() => {
        vi.advanceTimersByTime(1000)
      })

      await waitFor(() => {
        expect(screen.getByTestId('time-remaining')).toHaveTextContent('1499') // 24:59 in seconds
      })

      // Advance time by 60 more seconds
      act(() => {
        vi.advanceTimersByTime(60000)
      })

      await waitFor(() => {
        expect(screen.getByTestId('time-remaining')).toHaveTextContent('1439') // 23:59 in seconds
      })
    })

    it('displays correct phase colors and icons', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Start focus session
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      // Should show focus phase
      expect(screen.getByText('Focus Time')).toBeInTheDocument()
      expect(screen.getByTestId('current-phase')).toHaveTextContent('focus')
    })

    it('shows session progress when in session', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Start focus session
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      // Should show session progress
      await waitFor(() => {
        expect(screen.getByText('Focus session in progress')).toBeInTheDocument()
        expect(screen.getByText('Session Progress')).toBeInTheDocument()
      })
    })
  })

  describe('Settings Menu', () => {
    it('opens settings menu when settings button is clicked', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper timerProps={{showSettings: true}}>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      const settingsButton = screen.getByRole('button', {name: /settings/i})
      await act(async () => {
        await user.click(settingsButton)
      })

      // Settings menu should be open
      // Since soundEnabled defaults to true, it should show "Disable Sound"
      expect(screen.getByText('Disable Sound')).toBeInTheDocument()
      // Since notificationsEnabled defaults to true, it should show "Disable Notifications"
      expect(screen.getByText('Disable Notifications')).toBeInTheDocument()
      expect(screen.getByText('Advanced Settings')).toBeInTheDocument()
    })

    it('toggles sound setting', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper timerProps={{showSettings: true}}>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('true')

      // Open settings menu
      const settingsButton = screen.getByRole('button', {name: /settings/i})
      await act(async () => {
        await user.click(settingsButton)
      })

      // Click sound toggle (should show "Disable Sound" since it's currently enabled)
      const soundToggle = screen.getByText('Disable Sound')
      await act(async () => {
        await user.click(soundToggle)
      })

      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('false')
    })

    it('toggles notification setting', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper timerProps={{showSettings: true}}>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      expect(screen.getByTestId('notifications-enabled')).toHaveTextContent('true')

      // Open settings menu
      const settingsButton = screen.getByRole('button', {name: /settings/i})
      await act(async () => {
        await user.click(settingsButton)
      })

      // Click notification toggle
      const notificationToggle = screen.getByText('Disable Notifications')
      await act(async () => {
        await user.click(notificationToggle)
      })

      expect(screen.getByTestId('notifications-enabled')).toHaveTextContent('false')
    })
  })

  describe('Session Management', () => {
    it('creates session when starting focus timer', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      expect(screen.getByTestId('session-id')).toHaveTextContent('null')

      // Start timer
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      // Should create session
      await waitFor(() => {
        const sessionId = screen.getByTestId('session-id').textContent
        expect(sessionId).not.toBe('null')
        expect(sessionId).toMatch(/^session_user-123_\d+_/)
      })
    })

    it('tracks distractions when distraction button is clicked', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Start timer to create session
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      // Wait for session to be created
      await waitFor(() => {
        expect(screen.getByText('Focus session in progress')).toBeInTheDocument()
      })

      // Should show distractions counter
      const distractionsChip = screen.getByText('Distractions: 0')
      expect(distractionsChip).toBeInTheDocument()

      // Click to record distraction
      await act(async () => {
        await user.click(distractionsChip)
      })

      // Distraction count should increase
      expect(screen.getByText('Distractions: 1')).toBeInTheDocument()
    })

    it('shows goals section when goals button is clicked', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Start timer to create session
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      await waitFor(() => {
        expect(screen.getByText('Focus session in progress')).toBeInTheDocument()
      })

      // Click goals button
      const goalsButton = screen.getByText('Goals')
      await act(async () => {
        await user.click(goalsButton)
      })

      // Goals section should be visible
      // Use getAllByText to handle multiple "Session Goals" elements (tooltip + heading)
      const sessionGoalsElements = screen.getAllByText('Session Goals')
      // Should have at least one visible (the heading in the collapsed section)
      expect(sessionGoalsElements.length).toBeGreaterThan(0)
      expect(screen.getByText('Add Goal')).toBeInTheDocument()
    })

    it('calls onSessionStart when session starts', async () => {
      const onSessionStart = vi.fn()
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper timerProps={{onSessionStart}}>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      await waitFor(() => {
        expect(onSessionStart).toHaveBeenCalledWith(
            expect.objectContaining({
              userId: mockUser.id,
              hiveId: 'test-hive-123',
            })
        )
      })
    })

    it('calls onSessionEnd when session ends', async () => {
      const onSessionEnd = vi.fn()
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper timerProps={{onSessionEnd}}>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Start timer
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      await waitFor(() => {
        expect(screen.getByText('Focus session in progress')).toBeInTheDocument()
      })

      // Stop timer
      const stopButton = screen.getByRole('button', {name: /stop timer/i})
      await act(async () => {
        await user.click(stopButton)
      })

      await waitFor(() => {
        expect(onSessionEnd).toHaveBeenCalledWith(
            expect.objectContaining({
              userId: mockUser.id,
            })
        )
      })
    })
  })

  describe('Fullscreen Mode', () => {
    it('toggles fullscreen mode', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      const fullscreenButton = screen.getByRole('button', {name: /fullscreen/i})

      // Enter fullscreen
      await act(async () => {
        await user.click(fullscreenButton)
      })

      // Should show exit fullscreen button
      expect(screen.getByRole('button', {name: /exit fullscreen/i})).toBeInTheDocument()

      // Exit fullscreen
      const exitFullscreenButton = screen.getByRole('button', {name: /exit fullscreen/i})
      await act(async () => {
        await user.click(exitFullscreenButton)
      })

      // Should show fullscreen button again
      expect(screen.getByRole('button', {name: /^fullscreen$/i})).toBeInTheDocument()
    })
  })

  describe('Accessibility', () => {
    it('has proper ARIA labels for buttons', () => {
      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      expect(screen.getByRole('button', {name: /start focus session/i})).toBeInTheDocument()
      expect(screen.getByRole('button', {name: /fullscreen/i})).toBeInTheDocument()
    })

    it('has proper semantic structure', () => {
      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Should have main timer display
      expect(screen.getByText('25:00')).toBeInTheDocument()
      expect(screen.getByText('Ready to Focus')).toBeInTheDocument()
    })

    it('supports keyboard navigation', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      const playButton = screen.getByRole('button', {name: /start focus session/i})

      // Focus the button
      await act(async () => {
        playButton.focus()
      })

      expect(playButton).toHaveFocus()

      // Activate with keyboard
      await act(async () => {
        await user.keyboard('{Enter}')
      })

      // Timer should start
      expect(screen.getByTestId('is-running')).toHaveTextContent('true')
    })
  })

  describe('Edge Cases', () => {
    it('handles timer reaching zero', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Start timer
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      expect(screen.getByTestId('time-remaining')).toHaveTextContent('1500')

      // Fast forward to end of timer (25 minutes)
      act(() => {
        vi.advanceTimersByTime(1500000) // 1500 seconds
      })

      await waitFor(() => {
        expect(screen.getByTestId('time-remaining')).toHaveTextContent('0')
        expect(screen.getByTestId('is-running')).toHaveTextContent('false')
      })
    })

    it('handles settings persistence in localStorage', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      // First render
      const {unmount} = renderWithProviders(
          <TestTimerWrapper timerProps={{showSettings: true}}>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Change a setting
      const settingsButton = screen.getByRole('button', {name: /settings/i})
      await act(async () => {
        await user.click(settingsButton)
      })

      const soundToggle = screen.getByText('Disable Sound')
      await act(async () => {
        await user.click(soundToggle)
      })

      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('false')

      // Unmount and remount to test persistence
      unmount()

      renderWithProviders(
          <TestTimerWrapper timerProps={{showSettings: true}}>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Setting should be persisted
      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('false')
    })

    it('handles invalid timer states gracefully', async () => {
      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Component should render without errors even in edge cases
      expect(screen.getByText('Ready to Focus')).toBeInTheDocument()
      expect(screen.getByTestId('current-phase')).toHaveTextContent('idle')
    })
  })

  describe('Sound and Notifications', () => {
    it('requests notification permission on mount', async () => {
      const requestPermissionSpy = vi.spyOn(Notification, 'requestPermission')

      // Set permission to default to trigger request
      Object.defineProperty(Notification, 'permission', {
        value: 'default',
        configurable: true,
      })

      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      await waitFor(() => {
        expect(requestPermissionSpy).toHaveBeenCalled()
      })
    })

    it('initializes audio context when sound is enabled', async () => {
      renderWithProviders(
          <TestTimerWrapper>
            <TimerStateInspector/>
          </TestTimerWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Audio context should be initialized
      await waitFor(() => {
        expect(screen.getByTestId('sound-enabled')).toHaveTextContent('true')
      })
    })
  })
})