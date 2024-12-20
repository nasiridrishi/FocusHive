import React from 'react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/test-utils'
import ProductivityChart from '../ProductivityChart'

// Mock recharts to avoid rendering issues in tests
vi.mock('recharts', () => ({
  LineChart: vi.fn(({ children, ...props }) => (
    <div data-testid="line-chart" {...props}>{children}</div>
  )),
  Line: vi.fn((props) => <div data-testid="chart-line" {...props} />),
  XAxis: vi.fn((props) => <div data-testid="chart-x-axis" {...props} />),
  YAxis: vi.fn((props) => <div data-testid="chart-y-axis" {...props} />),
  CartesianGrid: vi.fn((props) => <div data-testid="chart-grid" {...props} />),
  Tooltip: vi.fn((props) => <div data-testid="chart-tooltip" {...props} />),
  Legend: vi.fn((props) => <div data-testid="chart-legend" {...props} />),
  ResponsiveContainer: vi.fn(({ children, ...props }) => (
    <div data-testid="responsive-container" {...props}>{children}</div>
  )),
  AreaChart: vi.fn(({ children, ...props }) => (
    <div data-testid="area-chart" {...props}>{children}</div>
  )),
  Area: vi.fn((props) => <div data-testid="chart-area" {...props} />),
  BarChart: vi.fn(({ children, ...props }) => (
    <div data-testid="bar-chart" {...props}>{children}</div>
  )),
  Bar: vi.fn((props) => <div data-testid="chart-bar" {...props} />),
}))

const mockData = {
  daily: [
    { date: '2025-01-01', focusTime: 120, breakTime: 15, sessions: 3 },
    { date: '2025-01-02', focusTime: 150, breakTime: 20, sessions: 4 },
    { date: '2025-01-03', focusTime: 90, breakTime: 10, sessions: 2 },
    { date: '2025-01-04', focusTime: 180, breakTime: 25, sessions: 5 },
    { date: '2025-01-05', focusTime: 210, breakTime: 30, sessions: 6 },
    { date: '2025-01-06', focusTime: 165, breakTime: 22, sessions: 4 },
    { date: '2025-01-07', focusTime: 195, breakTime: 28, sessions: 5 },
  ],
  weekly: [
    { week: 'Week 1', focusTime: 720, breakTime: 90, sessions: 18 },
    { week: 'Week 2', focusTime: 850, breakTime: 110, sessions: 22 },
    { week: 'Week 3', focusTime: 680, breakTime: 85, sessions: 17 },
    { week: 'Week 4', focusTime: 920, breakTime: 120, sessions: 24 },
  ],
  monthly: [
    { month: 'Jan', focusTime: 3200, breakTime: 400, sessions: 80 },
    { month: 'Feb', focusTime: 2800, breakTime: 350, sessions: 70 },
    { month: 'Mar', focusTime: 3500, breakTime: 450, sessions: 88 },
  ],
}

describe('ProductivityChart', () => {
  const defaultProps = {
    data: mockData.daily,
    view: 'daily' as const,
    onViewChange: vi.fn(),
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render productivity chart container', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      expect(screen.getByTestId('productivity-chart')).toBeInTheDocument()
    })

    it('should render chart title', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      expect(screen.getByRole('heading', { name: /productivity overview/i })).toBeInTheDocument()
    })

    it('should render view toggle buttons', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      expect(screen.getByRole('button', { name: /daily/i })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /weekly/i })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /monthly/i })).toBeInTheDocument()
    })

    it('should render chart with responsive container', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      expect(screen.getByTestId('responsive-container')).toBeInTheDocument()
    })

    it('should render chart axes', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      expect(screen.getByTestId('chart-x-axis')).toBeInTheDocument()
      expect(screen.getByTestId('chart-y-axis')).toBeInTheDocument()
    })

    it('should render chart grid', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      expect(screen.getByTestId('chart-grid')).toBeInTheDocument()
    })

    it('should render tooltip component', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      expect(screen.getByTestId('chart-tooltip')).toBeInTheDocument()
    })

    it('should render legend', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      expect(screen.getByTestId('chart-legend')).toBeInTheDocument()
    })
  })

  describe('Chart Types', () => {
    it('should render line chart by default', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      expect(screen.getByTestId('line-chart')).toBeInTheDocument()
      const lines = screen.getAllByTestId('chart-line')
      expect(lines).toHaveLength(2) // focusTime and breakTime
    })

    it('should render area chart when type is area', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} type="area" />)

      expect(screen.getByTestId('area-chart')).toBeInTheDocument()
      const areas = screen.getAllByTestId('chart-area')
      expect(areas).toHaveLength(2) // focusTime and breakTime
    })

    it('should render bar chart when type is bar', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} type="bar" />)

      expect(screen.getByTestId('bar-chart')).toBeInTheDocument()
      const bars = screen.getAllByTestId('chart-bar')
      expect(bars).toHaveLength(2) // focusTime and breakTime
    })
  })

  describe('View Toggle', () => {
    it('should highlight active view button', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} view="weekly" />)

      const weeklyButton = screen.getByRole('button', { name: /weekly/i })
      expect(weeklyButton).toHaveAttribute('aria-pressed', 'true')

      const dailyButton = screen.getByRole('button', { name: /daily/i })
      expect(dailyButton).toHaveAttribute('aria-pressed', 'false')
    })

    it('should call onViewChange when view button is clicked', async () => {
      const user = userEvent.setup()
      const onViewChange = vi.fn()
      renderWithProviders(
        <ProductivityChart {...defaultProps} onViewChange={onViewChange} />
      )

      const weeklyButton = screen.getByRole('button', { name: /weekly/i })
      await user.click(weeklyButton)

      expect(onViewChange).toHaveBeenCalledWith('weekly')
    })

    it('should not call onViewChange when clicking already active view', async () => {
      const user = userEvent.setup()
      const onViewChange = vi.fn()
      renderWithProviders(
        <ProductivityChart {...defaultProps} view="daily" onViewChange={onViewChange} />
      )

      const dailyButton = screen.getByRole('button', { name: /daily/i })
      await user.click(dailyButton)

      expect(onViewChange).not.toHaveBeenCalled()
    })
  })

  describe('Data Display', () => {
    it('should display summary statistics', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      // Check for summary stats
      expect(screen.getByTestId('total-focus-time')).toBeInTheDocument()
      expect(screen.getByTestId('average-session-time')).toBeInTheDocument()
      expect(screen.getByTestId('total-sessions')).toBeInTheDocument()
    })

    it('should calculate and display total focus time', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      const totalFocus = mockData.daily.reduce((sum, day) => sum + day.focusTime, 0)
      const totalElement = screen.getByTestId('total-focus-time')
      expect(totalElement).toHaveTextContent(`${totalFocus}`)
    })

    it('should calculate and display average session time', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      const totalFocus = mockData.daily.reduce((sum, day) => sum + day.focusTime, 0)
      const totalSessions = mockData.daily.reduce((sum, day) => sum + day.sessions, 0)
      const average = Math.round(totalFocus / totalSessions)
      
      const averageElement = screen.getByTestId('average-session-time')
      expect(averageElement).toHaveTextContent(`${average}`)
    })

    it('should display total sessions count', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      const totalSessions = mockData.daily.reduce((sum, day) => sum + day.sessions, 0)
      const sessionsElement = screen.getByTestId('total-sessions')
      expect(sessionsElement).toHaveTextContent(`${totalSessions}`)
    })
  })

  describe('Loading State', () => {
    it('should show loading spinner when loading', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} isLoading />)

      // CircularProgress has its own progressbar role, plus our wrapper
      const progressbars = screen.getAllByRole('progressbar')
      expect(progressbars.length).toBeGreaterThan(0)
      expect(screen.queryByTestId('line-chart')).not.toBeInTheDocument()
    })

    it('should show skeleton loader for statistics when loading', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} isLoading />)

      expect(screen.getByTestId('stats-skeleton')).toBeInTheDocument()
    })
  })

  describe('Empty State', () => {
    it('should show empty state when no data', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} data={[]} />)

      expect(screen.getByText(/no productivity data available/i)).toBeInTheDocument()
      expect(screen.queryByTestId('line-chart')).not.toBeInTheDocument()
    })

    it('should show helpful message in empty state', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} data={[]} />)

      expect(screen.getByText(/start a focus session to see your productivity/i)).toBeInTheDocument()
    })
  })

  describe('Error State', () => {
    it('should show error message when error prop is provided', () => {
      renderWithProviders(
        <ProductivityChart {...defaultProps} error="Failed to load data" />
      )

      expect(screen.getByText(/failed to load data/i)).toBeInTheDocument()
      expect(screen.queryByTestId('line-chart')).not.toBeInTheDocument()
    })

    it('should show retry button on error', () => {
      const onRetry = vi.fn()
      renderWithProviders(
        <ProductivityChart {...defaultProps} error="Failed to load data" onRetry={onRetry} />
      )

      expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument()
    })

    it('should call onRetry when retry button is clicked', async () => {
      const user = userEvent.setup()
      const onRetry = vi.fn()
      renderWithProviders(
        <ProductivityChart {...defaultProps} error="Failed to load data" onRetry={onRetry} />
      )

      const retryButton = screen.getByRole('button', { name: /retry/i })
      await user.click(retryButton)

      expect(onRetry).toHaveBeenCalled()
    })
  })

  describe('Interactive Features', () => {
    it('should support chart type toggle', async () => {
      const user = userEvent.setup()
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      const chartTypeButton = screen.getByRole('button', { name: /chart type/i })
      await user.click(chartTypeButton)

      expect(screen.getByRole('menuitem', { name: /line chart/i })).toBeInTheDocument()
      expect(screen.getByRole('menuitem', { name: /area chart/i })).toBeInTheDocument()
      expect(screen.getByRole('menuitem', { name: /bar chart/i })).toBeInTheDocument()
    })

    it('should support data export', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      expect(screen.getByRole('button', { name: /export data/i })).toBeInTheDocument()
    })

    it('should call onExport when export button is clicked', async () => {
      const user = userEvent.setup()
      const onExport = vi.fn()
      renderWithProviders(
        <ProductivityChart {...defaultProps} onExport={onExport} />
      )

      const exportButton = screen.getByRole('button', { name: /export data/i })
      await user.click(exportButton)

      expect(onExport).toHaveBeenCalledWith(mockData.daily)
    })
  })

  describe('Responsive Design', () => {
    it('should adjust chart height on mobile', () => {
      // Mock useMediaQuery to simulate mobile
      const originalMatchMedia = window.matchMedia
      window.matchMedia = vi.fn().mockImplementation(query => ({
        matches: true, // matches mobile breakpoint
        media: query,
        onchange: null,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }))

      renderWithProviders(<ProductivityChart {...defaultProps} />)

      const container = screen.getByTestId('responsive-container')
      expect(container).toHaveAttribute('height', '300')

      // Restore original
      window.matchMedia = originalMatchMedia
    })

    it('should have full chart height on desktop', () => {
      // Mock useMediaQuery to simulate desktop
      const originalMatchMedia = window.matchMedia
      window.matchMedia = vi.fn().mockImplementation(query => ({
        matches: false, // doesn't match mobile breakpoint
        media: query,
        onchange: null,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }))

      renderWithProviders(<ProductivityChart {...defaultProps} />)

      const container = screen.getByTestId('responsive-container')
      expect(container).toHaveAttribute('height', '400')

      // Restore original
      window.matchMedia = originalMatchMedia
    })

    it('should stack view buttons vertically on mobile', () => {
      // Mock useMediaQuery to simulate mobile
      const originalMatchMedia = window.matchMedia
      window.matchMedia = vi.fn().mockImplementation(query => ({
        matches: true, // matches mobile breakpoint
        media: query,
        onchange: null,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }))

      renderWithProviders(<ProductivityChart {...defaultProps} />)

      const buttonGroup = screen.getByRole('group', { name: /view toggle/i })
      expect(buttonGroup).toHaveStyle({ flexDirection: 'column' })

      // Restore original
      window.matchMedia = originalMatchMedia
    })
  })

  describe('Accessibility', () => {
    it('should have accessible chart description', () => {
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      expect(screen.getByRole('img', { name: /productivity chart/i })).toBeInTheDocument()
    })

    it('should have keyboard navigation for view buttons', async () => {
      const user = userEvent.setup()
      renderWithProviders(<ProductivityChart {...defaultProps} />)

      const dailyButton = screen.getByRole('button', { name: /daily/i })
      dailyButton.focus()
      expect(dailyButton).toHaveFocus()

      await user.keyboard('{Tab}')
      const weeklyButton = screen.getByRole('button', { name: /weekly/i })
      expect(weeklyButton).toHaveFocus()
    })

    it('should announce data updates to screen readers', () => {
      const { rerender } = renderWithProviders(<ProductivityChart {...defaultProps} />)

      const liveRegion = screen.getByRole('status')
      expect(liveRegion).toHaveAttribute('aria-live', 'polite')

      rerender(<ProductivityChart {...defaultProps} data={mockData.weekly} />)
      expect(liveRegion).toHaveTextContent(/data updated/i)
    })
  })

  describe('Custom Styling', () => {
    it('should accept custom colors for chart lines', () => {
      const customColors = {
        focusTime: '#4CAF50',
        breakTime: '#FF9800',
      }
      renderWithProviders(
        <ProductivityChart {...defaultProps} colors={customColors} />
      )

      const lines = screen.getAllByTestId('chart-line')
      expect(lines[0]).toHaveAttribute('stroke', '#4CAF50')
      expect(lines[1]).toHaveAttribute('stroke', '#FF9800')
    })

    it('should apply custom theme', () => {
      renderWithProviders(
        <ProductivityChart {...defaultProps} theme="dark" />
      )

      const chart = screen.getByTestId('productivity-chart')
      expect(chart).toHaveClass('theme-dark')
    })
  })
})