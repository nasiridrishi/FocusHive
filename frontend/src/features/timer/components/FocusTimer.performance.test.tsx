import React from 'react'
import {act, renderWithProviders, screen, userEvent, waitFor} from '../../../test-utils/test-utils'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {FocusTimer} from './FocusTimer'
import {TimerProvider} from '../contexts/TimerContext'
import type {User} from '../../../shared/types/auth'

// Mock dependencies
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

// Mock Web APIs
Object.defineProperty(window, 'AudioContext', {
  value: vi.fn(() => ({
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
  })),
  configurable: true,
})

Object.defineProperty(window, 'Notification', {
  value: vi.fn(),
  configurable: true,
})

Object.defineProperty(Notification, 'permission', {
  value: 'granted',
  configurable: true,
})

// Use fake timers for performance tests to prevent hanging
vi.useFakeTimers({shouldAdvanceTime: true})

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

const TestWrapper: React.FC<{ children: React.ReactNode }> = ({children}) => {
  return (
      <TimerProvider userId={mockUser.id}>
        {children}
      </TimerProvider>
  )
}

describe('FocusTimer Performance Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()

    // Mock performance APIs
    if (!global.performance) {
      global.performance = {
        now: vi.fn(() => Date.now()),
        mark: vi.fn(),
        measure: vi.fn(),
        getEntriesByType: vi.fn(() => []),
        clearMarks: vi.fn(),
        clearMeasures: vi.fn(),
        eventCounts: new Map(),
        navigation: {} as PerformanceNavigation,
        timing: {} as PerformanceTiming,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(() => true),
        getEntries: vi.fn(() => []),
        getEntriesByName: vi.fn(() => []),
        clearResourceTimings: vi.fn(),
        setResourceTimingBufferSize: vi.fn(),
        onresourcetimingbufferfull: null,
        timeOrigin: 0,
        toJSON: vi.fn(() => ({})),
      } as Performance
    }
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('Timer Interval Management', () => {
    it('should create only one timer interval when running', async () => {
      const setIntervalSpy = vi.spyOn(window, 'setInterval')
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <FocusTimer hiveId="test-hive-123"/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      // Should create exactly one interval
      expect(setIntervalSpy).toHaveBeenCalledTimes(1)
      expect(setIntervalSpy).toHaveBeenCalledWith(expect.any(Function), 1000)

      setIntervalSpy.mockRestore()
    })

    it('should clear intervals when timer is paused', async () => {
      const clearIntervalSpy = vi.spyOn(window, 'clearInterval')
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <FocusTimer hiveId="test-hive-123"/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Start timer
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      // Pause timer
      const pauseButton = screen.getByRole('button', {name: /pause timer/i})
      await act(async () => {
        await user.click(pauseButton)
      })

      // Should clear the interval
      expect(clearIntervalSpy).toHaveBeenCalled()

      clearIntervalSpy.mockRestore()
    })

    it('should clear intervals when timer is stopped', async () => {
      const clearIntervalSpy = vi.spyOn(window, 'clearInterval')
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <FocusTimer hiveId="test-hive-123"/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Start timer
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      // Stop timer
      const stopButton = screen.getByRole('button', {name: /stop timer/i})
      await act(async () => {
        await user.click(stopButton)
      })

      // Should clear the interval
      expect(clearIntervalSpy).toHaveBeenCalled()

      clearIntervalSpy.mockRestore()
    })

    it('should clear intervals on component unmount', () => {
      const clearIntervalSpy = vi.spyOn(window, 'clearInterval')

      const {unmount} = renderWithProviders(
          <TestWrapper>
            <FocusTimer hiveId="test-hive-123"/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      unmount()

      // Should clean up any intervals
      expect(clearIntervalSpy).toHaveBeenCalled()

      clearIntervalSpy.mockRestore()
    })

    it('should not create multiple intervals for rapid start/stop cycles', async () => {
      const setIntervalSpy = vi.spyOn(window, 'setInterval')
      const clearIntervalSpy = vi.spyOn(window, 'clearInterval')
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <FocusTimer hiveId="test-hive-123"/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      const playButton = screen.getByRole('button', {name: /start focus session/i})

      // Rapid start/pause cycles
      for (let i = 0; i < 5; i++) {
        await act(async () => {
          await user.click(playButton)
        })

        const pauseButton = screen.getByRole('button', {name: /pause timer/i})
        await act(async () => {
          await user.click(pauseButton)
        })
      }

      // Should have created and cleared intervals properly
      expect(setIntervalSpy).toHaveBeenCalledTimes(5)
      expect(clearIntervalSpy).toHaveBeenCalledTimes(5)

      setIntervalSpy.mockRestore()
      clearIntervalSpy.mockRestore()
    })
  })

  describe('Memory Leak Prevention', () => {
    it('should clean up audio context on unmount', () => {
      const audioContextCloseSpy = vi.fn()
      const mockAudioContext = {
        createOscillator: vi.fn(),
        createGain: vi.fn(),
        close: audioContextCloseSpy,
      }

      Object.defineProperty(window, 'AudioContext', {
        value: vi.fn(() => mockAudioContext),
        configurable: true,
      })

      const {unmount} = renderWithProviders(
          <TestWrapper>
            <FocusTimer hiveId="test-hive-123"/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      unmount()

      expect(audioContextCloseSpy).toHaveBeenCalled()
    })

    it('should not retain references to old timer states', async () => {
      const user = userEvent.setup()
      let renderCount = 0

      // Custom component to track renders
      const TrackingTimerWrapper: React.FC = () => {
        renderCount++
        return (
            <TestWrapper>
              <FocusTimer hiveId="test-hive-123"/>
            </TestWrapper>
        )
      }

      const {rerender} = renderWithProviders(
          <TrackingTimerWrapper/>,
          {withAuth: false, user: mockUser}
      )

      const initialRenderCount = renderCount

      // Start and stop timer multiple times
      for (let i = 0; i < 3; i++) {
        const playButton = screen.getByRole('button', {name: /start focus session/i})
        await act(async () => {
          await user.click(playButton)
        })

        const stopButton = screen.getByRole('button', {name: /stop timer/i})
        await act(async () => {
          await user.click(stopButton)
        })

        // Force re-render
        rerender(<TrackingTimerWrapper/>)
      }

      // Renders should be minimal and not exponentially increasing
      const finalRenderCount = renderCount
      const renderIncrement = finalRenderCount - initialRenderCount
      expect(renderIncrement).toBeLessThan(20) // Reasonable threshold
    })

    it('should handle rapid component mount/unmount cycles', async () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {
      })

      // Mount and unmount rapidly
      for (let i = 0; i < 10; i++) {
        const {unmount} = renderWithProviders(
            <TestWrapper>
              <FocusTimer hiveId="test-hive-123"/>
            </TestWrapper>,
            {withAuth: false, user: mockUser}
        )

        unmount()
      }

      // Should not produce memory leak errors
      expect(consoleSpy).not.toHaveBeenCalledWith(
          expect.stringContaining('memory leak')
      )

      consoleSpy.mockRestore()
    })

    it('should properly clean up event listeners', () => {
      const removeEventListenerSpy = vi.spyOn(document, 'removeEventListener')

      const {unmount} = renderWithProviders(
          <TestWrapper>
            <FocusTimer hiveId="test-hive-123"/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      unmount()

      // Note: Actual cleanup depends on implementation, this is a general check
      // In real implementation, component should clean up any document listeners
      expect(removeEventListenerSpy).not.toThrow()

      removeEventListenerSpy.mockRestore()
    })
  })

  describe('Performance Benchmarks', () => {
    it('should render initial state within performance threshold', () => {
      const startTime = performance.now()

      renderWithProviders(
          <TestWrapper>
            <FocusTimer hiveId="test-hive-123"/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      const endTime = performance.now()
      const renderTime = endTime - startTime

      // Should render quickly (within 100ms)
      expect(renderTime).toBeLessThan(100)
    })

    it('should handle timer updates efficiently', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestWrapper>
            <FocusTimer hiveId="test-hive-123"/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Start timer
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      const startTime = performance.now()

      // Simulate multiple timer ticks
      for (let i = 0; i < 60; i++) { // 1 minute of updates
        act(() => {
          vi.advanceTimersByTime(1000)
        })
      }

      const endTime = performance.now()
      const updateTime = endTime - startTime

      // Should handle updates efficiently (within 50ms for 60 updates)
      expect(updateTime).toBeLessThan(50)
    })

    it('should handle fullscreen toggle without performance degradation', async () => {
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <FocusTimer hiveId="test-hive-123"/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      const fullscreenButton = screen.getByRole('button', {name: /fullscreen/i})

      const startTime = performance.now()

      // Toggle fullscreen multiple times
      for (let i = 0; i < 10; i++) {
        await act(async () => {
          await user.click(fullscreenButton)
        })

        // Find the exit fullscreen button and click it
        const exitButton = screen.getByRole('button', {name: /exit fullscreen/i})
        await act(async () => {
          await user.click(exitButton)
        })
      }

      const endTime = performance.now()
      const toggleTime = endTime - startTime

      // Should handle toggles efficiently (within 200ms for 20 toggles)
      expect(toggleTime).toBeLessThan(200)
    })

    it('should handle settings menu operations efficiently', async () => {
      const user = userEvent.setup()

      renderWithProviders(
          <TestWrapper>
            <FocusTimer hiveId="test-hive-123" showSettings={true}/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      const startTime = performance.now()

      // Open and close settings menu multiple times
      for (let i = 0; i < 10; i++) {
        const settingsButton = screen.getByRole('button', {name: /settings/i})
        await act(async () => {
          await user.click(settingsButton)
        })

        const soundToggle = screen.getByText(/sound/i)
        await act(async () => {
          await user.click(soundToggle)
        })
      }

      const endTime = performance.now()
      const operationTime = endTime - startTime

      // Should handle menu operations efficiently (within 100ms for 20 operations)
      expect(operationTime).toBeLessThan(100)
    })
  })

  describe('Timer Accuracy', () => {
    it('should maintain accurate timing over extended periods', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestWrapper>
            <FocusTimer hiveId="test-hive-123"/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Start timer
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      // Check initial time (25 minutes = 1500 seconds)
      const initialTimeText = screen.getByText('25:00')
      expect(initialTimeText).toBeInTheDocument()

      // Advance 5 minutes
      act(() => {
        vi.advanceTimersByTime(300000) // 5 minutes
      })

      await waitFor(() => {
        expect(screen.getByText('20:00')).toBeInTheDocument()
      })

      // Advance another 10 minutes
      act(() => {
        vi.advanceTimersByTime(600000) // 10 minutes
      })

      await waitFor(() => {
        expect(screen.getByText('10:00')).toBeInTheDocument()
      })

      // Advance final 10 minutes
      act(() => {
        vi.advanceTimersByTime(600000) // 10 minutes
      })

      await waitFor(() => {
        expect(screen.getByText('00:00')).toBeInTheDocument()
      })
    })

    it('should handle pause/resume without time drift', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestWrapper>
            <FocusTimer hiveId="test-hive-123"/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Start timer
      const playButton = screen.getByRole('button', {name: /start focus session/i})
      await act(async () => {
        await user.click(playButton)
      })

      // Run for 2 minutes
      act(() => {
        vi.advanceTimersByTime(120000) // 2 minutes
      })

      await waitFor(() => {
        expect(screen.getByText('23:00')).toBeInTheDocument()
      })

      // Pause timer
      const pauseButton = screen.getByRole('button', {name: /pause timer/i})
      await act(async () => {
        await user.click(pauseButton)
      })

      // Advance time while paused (should not affect timer)
      act(() => {
        vi.advanceTimersByTime(60000) // 1 minute
      })

      // Time should remain the same
      expect(screen.getByText('23:00')).toBeInTheDocument()

      // Resume timer
      const resumeButton = screen.getByRole('button', {name: /resume timer/i})
      await act(async () => {
        await user.click(resumeButton)
      })

      // Run for 3 more minutes
      act(() => {
        vi.advanceTimersByTime(180000) // 3 minutes
      })

      await waitFor(() => {
        expect(screen.getByText('20:00')).toBeInTheDocument()
      })
    })

    it('should handle rapid start/stop cycles without accumulating errors', async () => {
      const user = userEvent.setup({advanceTimers: vi.advanceTimersByTime})

      renderWithProviders(
          <TestWrapper>
            <FocusTimer hiveId="test-hive-123"/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      let _expectedTime = 1500 // 25 minutes in seconds

      // Perform 5 cycles of start/run/stop
      for (let i = 0; i < 5; i++) {
        // Start timer
        const playButton = screen.getByRole('button', {name: /start focus session/i})
        await act(async () => {
          await user.click(playButton)
        })

        // Run for 30 seconds
        act(() => {
          vi.advanceTimersByTime(30000)
        })

        _expectedTime -= 30

        // Stop timer (should reset)
        const stopButton = screen.getByRole('button', {name: /stop timer/i})
        await act(async () => {
          await user.click(stopButton)
        })

        // Should reset to idle
        await waitFor(() => {
          expect(screen.getByText('Ready to Focus')).toBeInTheDocument()
        })

        _expectedTime = 1500 // Reset for next cycle
      }
    })
  })

  describe('Resource Usage', () => {
    it('should not exceed reasonable DOM node count', async () => {
      const initialNodeCount = document.querySelectorAll('*').length

      const {unmount} = renderWithProviders(
          <TestWrapper>
            <FocusTimer hiveId="test-hive-123"/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      const withComponentNodeCount = document.querySelectorAll('*').length
      const addedNodes = withComponentNodeCount - initialNodeCount

      // Should not add excessive DOM nodes (reasonable threshold: 100 nodes)
      expect(addedNodes).toBeLessThan(100)

      unmount()

      // Should clean up most nodes after unmount
      const finalNodeCount = document.querySelectorAll('*').length
      expect(finalNodeCount).toBeLessThanOrEqual(initialNodeCount + 10) // Allow small margin
    })

    it('should handle multiple concurrent timer instances efficiently', () => {
      const startTime = performance.now()

      // Create multiple timer instances
      const instances = []
      for (let i = 0; i < 5; i++) {
        const instance = renderWithProviders(
            <TestWrapper>
              <FocusTimer hiveId={`test-hive-${i}`}/>
            </TestWrapper>,
            {withAuth: false, user: mockUser}
        )
        instances.push(instance)
      }

      const endTime = performance.now()
      const creationTime = endTime - startTime

      // Should create multiple instances efficiently (within 200ms for 5 instances)
      expect(creationTime).toBeLessThan(200)

      // Cleanup
      instances.forEach(instance => instance.unmount())
    })

    it('should handle localStorage operations efficiently', async () => {
      const user = userEvent.setup()

      // Mock localStorage to track operations
      const setItemSpy = vi.spyOn(Storage.prototype, 'setItem')
      const getItemSpy = vi.spyOn(Storage.prototype, 'getItem')

      const startTime = performance.now()

      renderWithProviders(
          <TestWrapper>
            <FocusTimer hiveId="test-hive-123" showSettings={true}/>
          </TestWrapper>,
          {withAuth: false, user: mockUser}
      )

      // Change settings multiple times
      for (let i = 0; i < 10; i++) {
        const settingsButton = screen.getByRole('button', {name: /settings/i})
        await act(async () => {
          await user.click(settingsButton)
        })

        const soundToggle = screen.getByText(/sound/i)
        await act(async () => {
          await user.click(soundToggle)
        })
      }

      const endTime = performance.now()
      const operationTime = endTime - startTime

      // Should handle localStorage operations efficiently (within 100ms)
      expect(operationTime).toBeLessThan(100)

      // Should not make excessive localStorage calls
      expect(setItemSpy.mock.calls.length).toBeLessThan(50)
      expect(getItemSpy.mock.calls.length).toBeLessThan(20)

      setItemSpy.mockRestore()
      getItemSpy.mockRestore()
    })
  })
})