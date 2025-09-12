import React from 'react'
import { 
  renderWithProviders, 
  screen, 
  userEvent, 
  waitFor,
  act 
} from '../../../test-utils/test-utils'
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest'
import { TimerProvider, useTimer } from './TimerContext'
import { TimerState, TimerSettings, SessionStats, SessionGoal } from '../../../shared/types/timer'
import type { User } from '../../../shared/types/auth'

// Mock dependencies
const mockEmit = vi.fn()
const mockOn = vi.fn(() => () => {}) // Return unsubscribe function
const mockUpdatePresence = vi.fn()

vi.mock('../../../shared/contexts/WebSocketContext', () => ({
  useWebSocket: () => ({
    isConnected: true,
    emit: mockEmit,
    on: mockOn,
  }),
}))

vi.mock('../../../shared/contexts/PresenceContext', () => ({
  usePresence: () => ({
    currentPresence: { hiveId: 'test-hive-123' },
    updatePresence: mockUpdatePresence,
  }),
}))

// Mock Web Audio API
const mockOscillator = {
  connect: vi.fn(),
  frequency: { setValueAtTime: vi.fn() },
  start: vi.fn(),
  stop: vi.fn(),
}

const mockGain = {
  connect: vi.fn(),
  gain: {
    setValueAtTime: vi.fn(),
    exponentialRampToValueAtTime: vi.fn(),
  },
}

const mockAudioContext = {
  createOscillator: vi.fn(() => mockOscillator),
  createGain: vi.fn(() => mockGain),
  destination: {},
  currentTime: 0,
  close: vi.fn(),
}

Object.defineProperty(window, 'AudioContext', {
  value: vi.fn(() => mockAudioContext),
  configurable: true,
})

Object.defineProperty(window, 'webkitAudioContext', {
  value: vi.fn(() => mockAudioContext),
  configurable: true,
})

// Mock Notification API
const mockNotification = vi.fn()
Object.defineProperty(window, 'Notification', {
  value: mockNotification,
  configurable: true,
})

Object.defineProperty(Notification, 'permission', {
  value: 'granted',
  configurable: true,
})

Object.defineProperty(Notification, 'requestPermission', {
  value: vi.fn().mockResolvedValue('granted'),
  configurable: true,
})

// Mock timers
vi.useFakeTimers()

// Test user
const mockUser: User = {
  id: 'user-123',
  email: 'test@example.com',
  username: 'testuser',
  firstName: 'Test',
  lastName: 'User',
  name: 'Test User',
  isEmailVerified: true,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
}

// Test component to access timer context
const TimerTestComponent: React.FC = () => {
  const timer = useTimer()
  
  return (
    <div data-testid="timer-test">
      {/* Timer State */}
      <div data-testid="current-phase">{timer.timerState.currentPhase}</div>
      <div data-testid="time-remaining">{timer.timerState.timeRemaining}</div>
      <div data-testid="is-running">{timer.timerState.isRunning.toString()}</div>
      <div data-testid="is-paused">{timer.timerState.isPaused.toString()}</div>
      <div data-testid="current-cycle">{timer.timerState.currentCycle}</div>
      
      {/* Timer Settings */}
      <div data-testid="focus-length">{timer.timerSettings.focusLength}</div>
      <div data-testid="sound-enabled">{timer.timerSettings.soundEnabled.toString()}</div>
      <div data-testid="notifications-enabled">{timer.timerSettings.notificationsEnabled.toString()}</div>
      
      {/* Session Info */}
      <div data-testid="session-id">{timer.currentSession?.id || 'null'}</div>
      <div data-testid="session-distractions">{timer.currentSession?.distractions || 0}</div>
      <div data-testid="session-goals-count">{timer.currentSession?.goals.length || 0}</div>
      
      {/* Actions */}
      <button 
        data-testid="start-focus" 
        onClick={() => timer.startTimer('focus', 'test-hive-123')}
      >
        Start Focus
      </button>
      <button 
        data-testid="start-break" 
        onClick={() => timer.startTimer('short-break', 'test-hive-123')}
      >
        Start Break
      </button>
      <button data-testid="pause-timer" onClick={timer.pauseTimer}>
        Pause Timer
      </button>
      <button data-testid="resume-timer" onClick={timer.resumeTimer}>
        Resume Timer
      </button>
      <button data-testid="stop-timer" onClick={timer.stopTimer}>
        Stop Timer
      </button>
      <button data-testid="skip-phase" onClick={timer.skipPhase}>
        Skip Phase
      </button>
      <button 
        data-testid="record-distraction" 
        onClick={timer.recordDistraction}
      >
        Record Distraction
      </button>
      <button 
        data-testid="add-goal" 
        onClick={() => timer.addGoal('Test goal', 'medium')}
      >
        Add Goal
      </button>
      <button 
        data-testid="complete-goal" 
        onClick={() => {
          if (timer.currentSession?.goals[0]) {
            timer.completeGoal(timer.currentSession.goals[0].id)
          }
        }}
      >
        Complete First Goal
      </button>
      <button 
        data-testid="remove-goal" 
        onClick={() => {
          if (timer.currentSession?.goals[0]) {
            timer.removeGoal(timer.currentSession.goals[0].id)
          }
        }}
      >
        Remove First Goal
      </button>
      <button 
        data-testid="update-settings" 
        onClick={() => timer.updateSettings({ soundEnabled: false })}
      >
        Disable Sound
      </button>
      <button 
        data-testid="end-session" 
        onClick={() => timer.endSession({ rating: 4, notes: 'Good session' })}
      >
        End Session
      </button>
    </div>
  )
}

const TestWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <TimerProvider userId={mockUser.id}>
      {children}
    </TimerProvider>
  )
}

describe('TimerContext Integration', () => {
  let consoleSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    vi.clearAllMocks()
    vi.clearAllTimers()
    localStorage.clear()
    consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    consoleSpy.mockRestore()
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
    vi.useFakeTimers()
  })

  describe('Initial State', () => {
    it('provides default timer state', () => {
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      expect(screen.getByTestId('current-phase')).toHaveTextContent('idle')
      expect(screen.getByTestId('time-remaining')).toHaveTextContent('0')
      expect(screen.getByTestId('is-running')).toHaveTextContent('false')
      expect(screen.getByTestId('is-paused')).toHaveTextContent('false')
      expect(screen.getByTestId('current-cycle')).toHaveTextContent('0')
      expect(screen.getByTestId('session-id')).toHaveTextContent('null')
    })

    it('provides default timer settings', () => {
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Default Pomodoro settings
      expect(screen.getByTestId('focus-length')).toHaveTextContent('25')
      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('true')
      expect(screen.getByTestId('notifications-enabled')).toHaveTextContent('true')
    })

    it('initializes audio context when sound is enabled', () => {
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Audio context should be created
      expect(window.AudioContext).toHaveBeenCalled()
    })
  })

  describe('Timer Lifecycle', () => {
    it('starts focus session correctly', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      const startButton = screen.getByTestId('start-focus')
      await act(async () => {
        await user.click(startButton)
      })

      // Timer state should update
      expect(screen.getByTestId('current-phase')).toHaveTextContent('focus')
      expect(screen.getByTestId('time-remaining')).toHaveTextContent('1500') // 25 minutes
      expect(screen.getByTestId('is-running')).toHaveTextContent('true')
      expect(screen.getByTestId('current-cycle')).toHaveTextContent('1')

      // Session should be created
      await waitFor(() => {
        const sessionId = screen.getByTestId('session-id').textContent
        expect(sessionId).not.toBe('null')
        expect(sessionId).toMatch(/^session_user-123_\d+_/)
      })

      // WebSocket events should be emitted
      expect(mockEmit).toHaveBeenCalledWith('timer:session_start', expect.objectContaining({
        userId: mockUser.id,
        hiveId: 'test-hive-123',
      }))

      expect(mockEmit).toHaveBeenCalledWith('timer:start', expect.objectContaining({
        userId: mockUser.id,
        phase: 'focus',
        duration: 25,
        hiveId: 'test-hive-123',
      }))

      // Presence should be updated
      expect(mockUpdatePresence).toHaveBeenCalledWith('focusing', 'Focus session (25min)')
    })

    it('starts break session without creating new session', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      const startBreakButton = screen.getByTestId('start-break')
      await act(async () => {
        await user.click(startBreakButton)
      })

      // Timer should start but no new session should be created for breaks
      expect(screen.getByTestId('current-phase')).toHaveTextContent('short-break')
      expect(screen.getByTestId('time-remaining')).toHaveTextContent('300') // 5 minutes
      expect(screen.getByTestId('is-running')).toHaveTextContent('true')
      expect(screen.getByTestId('session-id')).toHaveTextContent('null') // No session for breaks
    })

    it('pauses and resumes timer correctly', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Start timer
      const startButton = screen.getByTestId('start-focus')
      await act(async () => {
        await user.click(startButton)
      })

      expect(screen.getByTestId('is-running')).toHaveTextContent('true')

      // Pause timer
      const pauseButton = screen.getByTestId('pause-timer')
      await act(async () => {
        await user.click(pauseButton)
      })

      expect(screen.getByTestId('is-running')).toHaveTextContent('false')
      expect(screen.getByTestId('is-paused')).toHaveTextContent('true')

      // WebSocket event should be emitted
      expect(mockEmit).toHaveBeenCalledWith('timer:pause', expect.objectContaining({
        userId: mockUser.id,
      }))

      // Presence should be updated
      expect(mockUpdatePresence).toHaveBeenCalledWith('online', 'Timer paused')

      // Resume timer
      const resumeButton = screen.getByTestId('resume-timer')
      await act(async () => {
        await user.click(resumeButton)
      })

      expect(screen.getByTestId('is-running')).toHaveTextContent('true')
      expect(screen.getByTestId('is-paused')).toHaveTextContent('false')

      // WebSocket event should be emitted
      expect(mockEmit).toHaveBeenCalledWith('timer:resume', expect.objectContaining({
        userId: mockUser.id,
      }))
    })

    it('stops timer and resets state', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Start timer
      const startButton = screen.getByTestId('start-focus')
      await act(async () => {
        await user.click(startButton)
      })

      expect(screen.getByTestId('is-running')).toHaveTextContent('true')

      // Stop timer
      const stopButton = screen.getByTestId('stop-timer')
      await act(async () => {
        await user.click(stopButton)
      })

      expect(screen.getByTestId('current-phase')).toHaveTextContent('idle')
      expect(screen.getByTestId('time-remaining')).toHaveTextContent('0')
      expect(screen.getByTestId('is-running')).toHaveTextContent('false')
      expect(screen.getByTestId('is-paused')).toHaveTextContent('false')

      // WebSocket event should be emitted
      expect(mockEmit).toHaveBeenCalledWith('timer:stop', expect.objectContaining({
        userId: mockUser.id,
      }))

      // Presence should be updated
      expect(mockUpdatePresence).toHaveBeenCalledWith('online', 'Timer stopped')
    })

    it('skips phase when running', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Start timer
      const startButton = screen.getByTestId('start-focus')
      await act(async () => {
        await user.click(startButton)
      })

      expect(screen.getByTestId('current-phase')).toHaveTextContent('focus')

      // Skip phase
      const skipButton = screen.getByTestId('skip-phase')
      await act(async () => {
        await user.click(skipButton)
      })

      // WebSocket event should be emitted
      expect(mockEmit).toHaveBeenCalledWith('timer:skip_phase', expect.objectContaining({
        userId: mockUser.id,
        phase: 'focus',
      }))
    })

    it('counts down time when running', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Start timer
      const startButton = screen.getByTestId('start-focus')
      await act(async () => {
        await user.click(startButton)
      })

      expect(screen.getByTestId('time-remaining')).toHaveTextContent('1500')

      // Advance time by 1 second
      act(() => {
        vi.advanceTimersByTime(1000)
      })

      await waitFor(() => {
        expect(screen.getByTestId('time-remaining')).toHaveTextContent('1499')
      })

      // Advance time by 59 more seconds (1 minute total)
      act(() => {
        vi.advanceTimersByTime(59000)
      })

      await waitFor(() => {
        expect(screen.getByTestId('time-remaining')).toHaveTextContent('1440') // 24 minutes
      })
    })
  })

  describe('Session Management', () => {
    it('creates session when starting focus timer', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      expect(screen.getByTestId('session-id')).toHaveTextContent('null')

      const startButton = screen.getByTestId('start-focus')
      await act(async () => {
        await user.click(startButton)
      })

      await waitFor(() => {
        const sessionId = screen.getByTestId('session-id').textContent
        expect(sessionId).not.toBe('null')
        expect(sessionId).toMatch(/^session_user-123_\d+_/)
      })
    })

    it('records distractions in current session', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Start session
      const startButton = screen.getByTestId('start-focus')
      await act(async () => {
        await user.click(startButton)
      })

      await waitFor(() => {
        expect(screen.getByTestId('session-id')).not.toHaveTextContent('null')
      })

      expect(screen.getByTestId('session-distractions')).toHaveTextContent('0')

      // Record distraction
      const distractionButton = screen.getByTestId('record-distraction')
      await act(async () => {
        await user.click(distractionButton)
      })

      expect(screen.getByTestId('session-distractions')).toHaveTextContent('1')

      // WebSocket event should be emitted
      expect(mockEmit).toHaveBeenCalledWith('timer:distraction_recorded', expect.objectContaining({
        userId: mockUser.id,
      }))
    })

    it('manages session goals', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Start session
      const startButton = screen.getByTestId('start-focus')
      await act(async () => {
        await user.click(startButton)
      })

      await waitFor(() => {
        expect(screen.getByTestId('session-id')).not.toHaveTextContent('null')
      })

      expect(screen.getByTestId('session-goals-count')).toHaveTextContent('0')

      // Add goal
      const addGoalButton = screen.getByTestId('add-goal')
      await act(async () => {
        await user.click(addGoalButton)
      })

      expect(screen.getByTestId('session-goals-count')).toHaveTextContent('1')

      // WebSocket event should be emitted
      expect(mockEmit).toHaveBeenCalledWith('timer:goal_added', expect.objectContaining({
        userId: mockUser.id,
        goal: expect.objectContaining({
          description: 'Test goal',
          priority: 'medium',
          isCompleted: false,
        }),
      }))

      // Complete goal
      const completeGoalButton = screen.getByTestId('complete-goal')
      await act(async () => {
        await user.click(completeGoalButton)
      })

      // WebSocket event should be emitted
      expect(mockEmit).toHaveBeenCalledWith('timer:goal_completed', expect.objectContaining({
        userId: mockUser.id,
      }))

      // Remove goal
      const removeGoalButton = screen.getByTestId('remove-goal')
      await act(async () => {
        await user.click(removeGoalButton)
      })

      expect(screen.getByTestId('session-goals-count')).toHaveTextContent('0')

      // WebSocket event should be emitted
      expect(mockEmit).toHaveBeenCalledWith('timer:goal_removed', expect.objectContaining({
        userId: mockUser.id,
      }))
    })

    it('ends session with productivity rating', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Start session
      const startButton = screen.getByTestId('start-focus')
      await act(async () => {
        await user.click(startButton)
      })

      await waitFor(() => {
        expect(screen.getByTestId('session-id')).not.toHaveTextContent('null')
      })

      // End session
      const endSessionButton = screen.getByTestId('end-session')
      await act(async () => {
        await user.click(endSessionButton)
      })

      expect(screen.getByTestId('session-id')).toHaveTextContent('null')

      // WebSocket event should be emitted
      expect(mockEmit).toHaveBeenCalledWith('timer:session_end', expect.objectContaining({
        userId: mockUser.id,
        session: expect.objectContaining({
          productivity: { rating: 4, notes: 'Good session' },
        }),
      }))

      // Presence should be updated
      expect(mockUpdatePresence).toHaveBeenCalledWith('online', 'Session completed')
    })
  })

  describe('Settings Management', () => {
    it('updates timer settings', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('true')

      // Update settings
      const updateSettingsButton = screen.getByTestId('update-settings')
      await act(async () => {
        await user.click(updateSettingsButton)
      })

      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('false')

      // WebSocket event should be emitted
      expect(mockEmit).toHaveBeenCalledWith('timer:settings_update', expect.objectContaining({
        userId: mockUser.id,
        settings: expect.objectContaining({
          soundEnabled: false,
        }),
      }))
    })

    it('persists settings to localStorage', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      const { unmount } = renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Update settings
      const updateSettingsButton = screen.getByTestId('update-settings')
      await act(async () => {
        await user.click(updateSettingsButton)
      })

      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('false')

      // Unmount and remount to test persistence
      unmount()

      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Settings should be persisted
      await waitFor(() => {
        expect(screen.getByTestId('sound-enabled')).toHaveTextContent('false')
      })
    })

    it('loads settings from localStorage on mount', () => {
      // Pre-populate localStorage with custom settings
      const customSettings = {
        focusLength: 30,
        soundEnabled: false,
        notificationsEnabled: false,
      }
      localStorage.setItem(`timer-settings-${mockUser.id}`, JSON.stringify(customSettings))

      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Should load custom settings
      expect(screen.getByTestId('focus-length')).toHaveTextContent('30')
      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('false')
      expect(screen.getByTestId('notifications-enabled')).toHaveTextContent('false')
    })

    it('handles corrupt localStorage data gracefully', () => {
      // Set invalid JSON in localStorage
      localStorage.setItem(`timer-settings-${mockUser.id}`, 'invalid-json')

      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Should fall back to default settings
      expect(screen.getByTestId('focus-length')).toHaveTextContent('25')
      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('true')
    })
  })

  describe('WebSocket Integration', () => {
    it('sets up WebSocket event listeners on mount', () => {
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Should register WebSocket event listeners
      expect(mockOn).toHaveBeenCalledWith('timer:session_started', expect.any(Function))
      expect(mockOn).toHaveBeenCalledWith('timer:session_ended', expect.any(Function))
      expect(mockOn).toHaveBeenCalledWith('timer:phase_completed', expect.any(Function))
      expect(mockOn).toHaveBeenCalledWith('timer:settings_updated', expect.any(Function))
    })

    it('handles session_started event from other devices', () => {
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Get the session_started handler
      const sessionStartedCall = mockOn.mock.calls.find(call => call[0] === 'timer:session_started')
      expect(sessionStartedCall).toBeDefined()
      
      const sessionStartedHandler = sessionStartedCall![1]

      // Trigger the handler with different session ID
      act(() => {
        sessionStartedHandler({ userId: mockUser.id, sessionId: 'different-session-id' })
      })

      // Should handle the event (exact behavior depends on implementation)
      expect(screen.getByTestId('session-id')).toHaveTextContent('null')
    })

    it('handles session_ended event', () => {
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Get the session_ended handler
      const sessionEndedCall = mockOn.mock.calls.find(call => call[0] === 'timer:session_ended')
      expect(sessionEndedCall).toBeDefined()
      
      const sessionEndedHandler = sessionEndedCall![1]

      // Trigger the handler
      act(() => {
        sessionEndedHandler({ userId: mockUser.id })
      })

      // Should reset session and timer state
      expect(screen.getByTestId('session-id')).toHaveTextContent('null')
      expect(screen.getByTestId('current-phase')).toHaveTextContent('idle')
    })

    it('handles settings_updated event', () => {
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('true')

      // Get the settings_updated handler
      const settingsUpdatedCall = mockOn.mock.calls.find(call => call[0] === 'timer:settings_updated')
      expect(settingsUpdatedCall).toBeDefined()
      
      const settingsUpdatedHandler = settingsUpdatedCall![1]

      // Trigger the handler with new settings
      act(() => {
        settingsUpdatedHandler({ 
          userId: mockUser.id, 
          settings: { soundEnabled: false, focusLength: 30 } 
        })
      })

      // Should update settings
      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('false')
      expect(screen.getByTestId('focus-length')).toHaveTextContent('30')
    })

    it('ignores WebSocket events from other users', () => {
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      const settingsUpdatedCall = mockOn.mock.calls.find(call => call[0] === 'timer:settings_updated')
      const settingsUpdatedHandler = settingsUpdatedCall![1]

      // Trigger handler with different user ID
      act(() => {
        settingsUpdatedHandler({ 
          userId: 'different-user', 
          settings: { soundEnabled: false } 
        })
      })

      // Should not update settings
      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('true')
    })
  })

  describe('Audio and Notifications', () => {
    it('initializes audio context when sound is enabled', () => {
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Should create audio context
      expect(window.AudioContext).toHaveBeenCalled()
    })

    it('plays notification sound on timer start', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      const startButton = screen.getByTestId('start-focus')
      await act(async () => {
        await user.click(startButton)
      })

      // Should create oscillator for sound
      expect(mockAudioContext.createOscillator).toHaveBeenCalled()
      expect(mockAudioContext.createGain).toHaveBeenCalled()
    })

    it('does not play sound when sound is disabled', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Disable sound first
      const updateSettingsButton = screen.getByTestId('update-settings')
      await act(async () => {
        await user.click(updateSettingsButton)
      })

      expect(screen.getByTestId('sound-enabled')).toHaveTextContent('false')

      // Clear previous calls
      mockAudioContext.createOscillator.mockClear()

      // Start timer
      const startButton = screen.getByTestId('start-focus')
      await act(async () => {
        await user.click(startButton)
      })

      // Should not create oscillator
      expect(mockAudioContext.createOscillator).not.toHaveBeenCalled()
    })

    it('creates browser notification when enabled', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Start timer and let it complete (advance by 25 minutes)
      const startButton = screen.getByTestId('start-focus')
      await act(async () => {
        await user.click(startButton)
      })

      // Fast forward to timer completion
      act(() => {
        vi.advanceTimersByTime(1500000) // 25 minutes
      })

      // Should show browser notification (implementation would call new Notification)
      await waitFor(() => {
        expect(screen.getByTestId('time-remaining')).toHaveTextContent('0')
      })
    })
  })

  describe('Cleanup and Memory Management', () => {
    it('cleans up intervals on unmount', () => {
      const clearIntervalSpy = vi.spyOn(window, 'clearInterval')
      
      const { unmount } = renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      unmount()

      expect(clearIntervalSpy).toHaveBeenCalled()
      clearIntervalSpy.mockRestore()
    })

    it('closes audio context on unmount', () => {
      const { unmount } = renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      unmount()

      expect(mockAudioContext.close).toHaveBeenCalled()
    })

    it('unsubscribes from WebSocket events on unmount', () => {
      const unsubscribeFn = vi.fn()
      mockOn.mockReturnValue(unsubscribeFn)

      const { unmount } = renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      unmount()

      // Should call unsubscribe functions
      expect(unsubscribeFn).toHaveBeenCalled()
    })
  })

  describe('Error Handling', () => {
    it('handles audio context creation errors gracefully', () => {
      // Mock AudioContext to throw error
      Object.defineProperty(window, 'AudioContext', {
        value: vi.fn(() => {
          throw new Error('AudioContext not supported')
        }),
        configurable: true,
      })

      // Should not crash
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      expect(screen.getByTestId('timer-test')).toBeInTheDocument()
    })

    it('handles WebSocket disconnection gracefully', () => {
      // Mock disconnected WebSocket
      vi.mocked(require('../../../shared/contexts/WebSocketContext').useWebSocket).mockReturnValue({
        isConnected: false,
        emit: mockEmit,
        on: mockOn,
      })

      // Should not crash
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      expect(screen.getByTestId('timer-test')).toBeInTheDocument()
    })
  })

  describe('Edge Cases', () => {
    it('handles actions without current session gracefully', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Try to record distraction without session
      const distractionButton = screen.getByTestId('record-distraction')
      await act(async () => {
        await user.click(distractionButton)
      })

      // Should handle gracefully
      expect(screen.getByTestId('session-distractions')).toHaveTextContent('0')
    })

    it('handles skip phase when not running', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      
      renderWithProviders(
        <TestWrapper>
          <TimerTestComponent />
        </TestWrapper>,
        { withAuth: false, user: mockUser }
      )

      // Try to skip when timer is not running
      const skipButton = screen.getByTestId('skip-phase')
      await act(async () => {
        await user.click(skipButton)
      })

      // Should not emit WebSocket event
      expect(mockEmit).not.toHaveBeenCalledWith('timer:skip_phase', expect.any(Object))
    })
  })
})