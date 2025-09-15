import React from 'react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/test-utils'
import StatsCard from '../StatsCard'
import { Timer, TrendingUp } from '@mui/icons-material'

describe('StatsCard', () => {
  const defaultProps = {
    title: 'Test Title',
    value: '42',
    icon: <Timer />,
    color: 'primary' as const,
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render stats card with all required elements', () => {
      renderWithProviders(<StatsCard {...defaultProps} />)

      expect(screen.getByTestId('stats-card')).toBeInTheDocument()
      expect(screen.getByText('Test Title')).toBeInTheDocument()
      expect(screen.getByText('42')).toBeInTheDocument()
      expect(screen.getByTestId('TimerIcon')).toBeInTheDocument()
    })

    it('should render with custom color', () => {
      renderWithProviders(<StatsCard {...defaultProps} color="success" />)

      const card = screen.getByTestId('stats-card')
      expect(card).toBeInTheDocument()
      // Color would be applied to icon
    })

    it('should render with unit suffix', () => {
      renderWithProviders(<StatsCard {...defaultProps} value="100" unit="%" />)

      expect(screen.getByText('100%')).toBeInTheDocument()
    })

    it('should render with trend indicator when provided', () => {
      renderWithProviders(
        <StatsCard
          {...defaultProps}
          trend="up"
          trendValue="+15%"
        />
      )

      expect(screen.getByTestId('trend-indicator')).toBeInTheDocument()
      expect(screen.getByText('+15%')).toBeInTheDocument()
    })

    it('should render with subtitle when provided', () => {
      renderWithProviders(
        <StatsCard
          {...defaultProps}
          subtitle="vs last week"
        />
      )

      expect(screen.getByText('vs last week')).toBeInTheDocument()
    })

    it('should render loading skeleton when loading', () => {
      renderWithProviders(
        <StatsCard
          {...defaultProps}
          loading={true}
        />
      )

      expect(screen.getByTestId('stats-card-skeleton')).toBeInTheDocument()
    })

    it('should handle error state', () => {
      renderWithProviders(
        <StatsCard
          {...defaultProps}
          error="Failed to load"
        />
      )

      expect(screen.getByText('--')).toBeInTheDocument()
      expect(screen.getByTestId('error-icon')).toBeInTheDocument()
    })

    it('should render with different icon', () => {
      renderWithProviders(
        <StatsCard
          {...defaultProps}
          icon={<TrendingUp />}
        />
      )

      expect(screen.getByTestId('TrendingUpIcon')).toBeInTheDocument()
    })
  })

  describe('Interaction', () => {
    it('should handle click when onClick provided', async () => {
      const user = userEvent.setup()
      const handleClick = vi.fn()

      renderWithProviders(
        <StatsCard
          {...defaultProps}
          onClick={handleClick}
        />
      )

      const card = screen.getByTestId('stats-card')
      await user.click(card)

      expect(handleClick).toHaveBeenCalledTimes(1)
    })

    it('should show hover effect when clickable', async () => {
      const user = userEvent.setup()
      const handleClick = vi.fn()

      renderWithProviders(
        <StatsCard
          {...defaultProps}
          onClick={handleClick}
        />
      )

      const card = screen.getByTestId('stats-card')
      expect(card).toHaveStyle({ cursor: 'pointer' })
    })

    it('should not be clickable without onClick', () => {
      renderWithProviders(<StatsCard {...defaultProps} />)

      const card = screen.getByTestId('stats-card')
      expect(card).not.toHaveStyle({ cursor: 'pointer' })
    })
  })

  describe('Formatting', () => {
    it('should format large numbers with commas', () => {
      renderWithProviders(
        <StatsCard
          {...defaultProps}
          value="1234567"
          formatValue={true}
        />
      )

      expect(screen.getByText('1,234,567')).toBeInTheDocument()
    })

    it('should handle decimal values', () => {
      renderWithProviders(
        <StatsCard
          {...defaultProps}
          value="3.14"
          unit="avg"
        />
      )

      expect(screen.getByText('3.14 avg')).toBeInTheDocument()
    })

    it('should handle zero value', () => {
      renderWithProviders(
        <StatsCard
          {...defaultProps}
          value="0"
        />
      )

      expect(screen.getByText('0')).toBeInTheDocument()
    })
  })

  describe('Variants', () => {
    it('should render compact variant', () => {
      renderWithProviders(
        <StatsCard
          {...defaultProps}
          variant="compact"
        />
      )

      const card = screen.getByTestId('stats-card')
      expect(card).toHaveClass('MuiCard-compact')
    })

    it('should render expanded variant with description', () => {
      renderWithProviders(
        <StatsCard
          {...defaultProps}
          variant="expanded"
          description="This is a detailed description of the metric"
        />
      )

      expect(screen.getByText('This is a detailed description of the metric')).toBeInTheDocument()
    })
  })

  describe('Accessibility', () => {
    it('should have proper ARIA attributes', () => {
      renderWithProviders(
        <StatsCard
          {...defaultProps}
          ariaLabel="Test statistics card"
        />
      )

      const card = screen.getByTestId('stats-card')
      expect(card).toHaveAttribute('aria-label', 'Test statistics card')
    })

    it('should have semantic HTML structure', () => {
      renderWithProviders(<StatsCard {...defaultProps} />)

      const title = screen.getByText('Test Title')
      const value = screen.getByText('42')

      // Title should be a heading element
      expect(title.tagName).toMatch(/H[1-6]/)

      // Value should be prominent
      expect(value).toBeInTheDocument()
    })

    it('should indicate loading state to screen readers', () => {
      renderWithProviders(
        <StatsCard
          {...defaultProps}
          loading={true}
        />
      )

      const skeleton = screen.getByTestId('stats-card-skeleton')
      expect(skeleton).toHaveAttribute('aria-busy', 'true')
    })
  })

  describe('Responsiveness', () => {
    it('should adapt to container width', () => {
      renderWithProviders(
        <StatsCard
          {...defaultProps}
          responsive={true}
        />
      )

      const card = screen.getByTestId('stats-card')
      expect(card).toHaveStyle({ width: '100%' })
    })
  })
})