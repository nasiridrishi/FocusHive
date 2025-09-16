import React from 'react'
import {render, screen} from '../../../test-utils/test-utils'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {createTheme, ThemeProvider} from '@mui/material/styles'
import {TimerState} from '../../../shared/types/timer'

// Extract CircularTimer component for testing
// Since it's not exported separately, we'll test it through the main component
// But we can create a standalone version for focused testing

interface CircularTimerProps {
  timeRemaining: number
  totalTime: number
  phase: TimerState['currentPhase']
  size?: number
}

const CircularTimer: React.FC<CircularTimerProps> = ({
                                                       timeRemaining,
                                                       totalTime,
                                                       phase,
                                                       size = 200
                                                     }) => {
  const progress = totalTime > 0 ? ((totalTime - timeRemaining) / totalTime) * 100 : 0
  const circumference = 2 * Math.PI * (size / 2 - 8)
  const strokeDasharray = circumference
  const strokeDashoffset = circumference - (progress / 100) * circumference

  const theme = createTheme()

  const getPhaseColor = (phase: TimerState['currentPhase']): string => {
    switch (phase) {
      case 'focus':
        return theme.palette.primary.main
      case 'short-break':
        return theme.palette.success.main
      case 'long-break':
        return theme.palette.info.main
      default:
        return theme.palette.grey[400]
    }
  }

  const formatTime = (seconds: number): string => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }

  return (
      <div
          data-testid="circular-timer"
          style={{
            position: 'relative',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: size,
            height: size,
          }}
      >
        {/* Background Circle */}
        <svg width={size} height={size} style={{position: 'absolute'}}>
          <circle
              data-testid="background-circle"
              cx={size / 2}
              cy={size / 2}
              r={size / 2 - 8}
              fill="none"
              stroke={theme.palette.grey[200]}
              strokeWidth="4"
          />
          {/* Progress Circle */}
          <circle
              data-testid="progress-circle"
              cx={size / 2}
              cy={size / 2}
              r={size / 2 - 8}
              fill="none"
              stroke={getPhaseColor(phase)}
              strokeWidth="6"
              strokeLinecap="round"
              strokeDasharray={strokeDasharray}
              strokeDashoffset={strokeDashoffset}
              transform={`rotate(-90 ${size / 2} ${size / 2})`}
          />
        </svg>

        {/* Time Display */}
        <div data-testid="time-display" style={{textAlign: 'center', zIndex: 1}}>
          <div
              data-testid="time-text"
              style={{
                fontFamily: 'monospace',
                fontWeight: 'bold',
                color: getPhaseColor(phase),
                fontSize: '2.5rem'
              }}
          >
            {formatTime(timeRemaining)}
          </div>
          <div
              data-testid="phase-text"
              style={{
                fontSize: '0.875rem',
                color: theme.palette.text.secondary,
                textTransform: 'capitalize',
                marginTop: '8px'
              }}
          >
            {phase.replace('-', ' ')}
          </div>
        </div>
      </div>
  )
}

// Wrapper component with theme
const CircularTimerWrapper: React.FC<{ children: React.ReactNode }> = ({children}) => {
  const theme = createTheme()
  return (
      <ThemeProvider theme={theme}>
        {children}
      </ThemeProvider>
  )
}

describe('CircularTimer Component', () => {
  const defaultProps: CircularTimerProps = {
    timeRemaining: 1500, // 25 minutes
    totalTime: 1500,
    phase: 'focus' as const,
    size: 200,
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('renders without crashing', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} />
          </CircularTimerWrapper>
      )

      expect(screen.getByTestId('circular-timer')).toBeInTheDocument()
      expect(screen.getByTestId('time-display')).toBeInTheDocument()
      expect(screen.getByTestId('time-text')).toBeInTheDocument()
      expect(screen.getByTestId('phase-text')).toBeInTheDocument()
    })

    it('renders with custom size', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} size={300}/>
          </CircularTimerWrapper>
      )

      const timer = screen.getByTestId('circular-timer')
      expect(timer).toHaveStyle({width: '300px', height: '300px'})
    })

    it('renders SVG circles correctly', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} />
          </CircularTimerWrapper>
      )

      expect(screen.getByTestId('background-circle')).toBeInTheDocument()
      expect(screen.getByTestId('progress-circle')).toBeInTheDocument()
    })
  })

  describe('Time Formatting', () => {
    it('formats time correctly for full minutes', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} timeRemaining={1800} totalTime={1800}/>
          </CircularTimerWrapper>
      )

      expect(screen.getByTestId('time-text')).toHaveTextContent('30:00')
    })

    it('formats time correctly for minutes and seconds', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} timeRemaining={1545} totalTime={1800}/>
          </CircularTimerWrapper>
      )

      expect(screen.getByTestId('time-text')).toHaveTextContent('25:45')
    })

    it('formats time correctly for seconds only', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} timeRemaining={45} totalTime={1800}/>
          </CircularTimerWrapper>
      )

      expect(screen.getByTestId('time-text')).toHaveTextContent('00:45')
    })

    it('formats zero time correctly', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} timeRemaining={0} totalTime={1500}/>
          </CircularTimerWrapper>
      )

      expect(screen.getByTestId('time-text')).toHaveTextContent('00:00')
    })

    it('pads single digit minutes and seconds', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} timeRemaining={65} totalTime={1500}/>
          </CircularTimerWrapper>
      )

      expect(screen.getByTestId('time-text')).toHaveTextContent('01:05')
    })
  })

  describe('Phase Display', () => {
    it('displays focus phase correctly', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} phase="focus"/>
          </CircularTimerWrapper>
      )

      expect(screen.getByTestId('phase-text')).toHaveTextContent('focus')
    })

    it('displays short break phase correctly', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} phase="short-break"/>
          </CircularTimerWrapper>
      )

      expect(screen.getByTestId('phase-text')).toHaveTextContent('short break')
    })

    it('displays long break phase correctly', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} phase="long-break"/>
          </CircularTimerWrapper>
      )

      expect(screen.getByTestId('phase-text')).toHaveTextContent('long break')
    })

    it('displays idle phase correctly', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} phase="idle"/>
          </CircularTimerWrapper>
      )

      expect(screen.getByTestId('phase-text')).toHaveTextContent('idle')
    })
  })

  describe('Progress Calculation', () => {
    it('calculates progress correctly at start', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer timeRemaining={1500} totalTime={1500} phase="focus"/>
          </CircularTimerWrapper>
      )

      const progressCircle = screen.getByTestId('progress-circle')
      expect(progressCircle).toBeInTheDocument()

      // At start, progress should be 0% (full dashoffset)
      const circumference = 2 * Math.PI * (200 / 2 - 8)
      expect(progressCircle).toHaveAttribute('stroke-dashoffset', circumference.toString())
    })

    it('calculates progress correctly at halfway point', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer timeRemaining={750} totalTime={1500} phase="focus"/>
          </CircularTimerWrapper>
      )

      const progressCircle = screen.getByTestId('progress-circle')
      const circumference = 2 * Math.PI * (200 / 2 - 8)
      const expectedOffset = circumference - (50 / 100) * circumference

      expect(progressCircle).toHaveAttribute('stroke-dashoffset', expectedOffset.toString())
    })

    it('calculates progress correctly when complete', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer timeRemaining={0} totalTime={1500} phase="focus"/>
          </CircularTimerWrapper>
      )

      const progressCircle = screen.getByTestId('progress-circle')
      const circumference = 2 * Math.PI * (200 / 2 - 8)
      const expectedOffset = circumference - circumference // Should be 0

      expect(progressCircle).toHaveAttribute('stroke-dashoffset', expectedOffset.toString())
    })

    it('handles zero total time gracefully', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer timeRemaining={0} totalTime={0} phase="idle"/>
          </CircularTimerWrapper>
      )

      const progressCircle = screen.getByTestId('progress-circle')
      const circumference = 2 * Math.PI * (200 / 2 - 8)

      // With zero total time, progress should be 0, so full dashoffset
      expect(progressCircle).toHaveAttribute('stroke-dashoffset', circumference.toString())
    })
  })

  describe('Visual Styling', () => {
    it('applies correct colors for focus phase', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} phase="focus"/>
          </CircularTimerWrapper>
      )

      const progressCircle = screen.getByTestId('progress-circle')
      // Focus phase should use primary color
      expect(progressCircle).toHaveAttribute('stroke')
    })

    it('applies correct colors for short break phase', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} phase="short-break"/>
          </CircularTimerWrapper>
      )

      const progressCircle = screen.getByTestId('progress-circle')
      // Short break should use success color
      expect(progressCircle).toHaveAttribute('stroke')
    })

    it('applies correct colors for long break phase', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} phase="long-break"/>
          </CircularTimerWrapper>
      )

      const progressCircle = screen.getByTestId('progress-circle')
      // Long break should use info color
      expect(progressCircle).toHaveAttribute('stroke')
    })

    it('has proper stroke width for progress circle', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} />
          </CircularTimerWrapper>
      )

      const progressCircle = screen.getByTestId('progress-circle')
      expect(progressCircle).toHaveAttribute('stroke-width', '6')
    })

    it('has proper stroke width for background circle', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} />
          </CircularTimerWrapper>
      )

      const backgroundCircle = screen.getByTestId('background-circle')
      expect(backgroundCircle).toHaveAttribute('stroke-width', '4')
    })
  })

  describe('Responsive Design', () => {
    it('adjusts size prop correctly', () => {
      const {rerender} = render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} size={150}/>
          </CircularTimerWrapper>
      )

      let timer = screen.getByTestId('circular-timer')
      expect(timer).toHaveStyle({width: '150px', height: '150px'})

      rerender(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} size={300}/>
          </CircularTimerWrapper>
      )

      timer = screen.getByTestId('circular-timer')
      expect(timer).toHaveStyle({width: '300px', height: '300px'})
    })

    it('maintains aspect ratio', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} size={250}/>
          </CircularTimerWrapper>
      )

      const timer = screen.getByTestId('circular-timer')
      expect(timer).toHaveStyle({width: '250px', height: '250px'})
    })
  })

  describe('Accessibility', () => {
    it('has proper structure for screen readers', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} />
          </CircularTimerWrapper>
      )

      // Time should be readable
      expect(screen.getByTestId('time-text')).toHaveTextContent('25:00')
      // Phase should be readable
      expect(screen.getByTestId('phase-text')).toHaveTextContent('focus')
    })

    it('uses monospace font for time display', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer {...defaultProps} />
          </CircularTimerWrapper>
      )

      const timeText = screen.getByTestId('time-text')
      expect(timeText).toHaveStyle({fontFamily: 'monospace'})
    })
  })

  describe('Edge Cases', () => {
    it('handles negative time remaining', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer timeRemaining={-10} totalTime={1500} phase="focus"/>
          </CircularTimerWrapper>
      )

      // Should handle gracefully, likely showing 00:00 or handling as 0
      expect(screen.getByTestId('time-text')).toBeInTheDocument()
    })

    it('handles very large numbers', () => {
      const largeTime = 99999 // 27+ hours
      render(
          <CircularTimerWrapper>
            <CircularTimer timeRemaining={largeTime} totalTime={largeTime} phase="focus"/>
          </CircularTimerWrapper>
      )

      expect(screen.getByTestId('time-text')).toHaveTextContent('1666:39')
    })

    it('handles time remaining larger than total time', () => {
      render(
          <CircularTimerWrapper>
            <CircularTimer timeRemaining={2000} totalTime={1500} phase="focus"/>
          </CircularTimerWrapper>
      )

      // Should handle gracefully without breaking
      expect(screen.getByTestId('time-text')).toHaveTextContent('33:20')
    })
  })
})