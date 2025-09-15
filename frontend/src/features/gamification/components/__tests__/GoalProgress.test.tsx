import React from 'react'
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { screen, within, act } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/test-utils'
import { GoalProgress } from '../GoalProgress'

// Mock data for comprehensive testing
const mockDailyGoal = {
  id: 'goal-1',
  title: 'Complete 5 Focus Sessions',
  description: 'Maintain daily focus with 5 Pomodoro sessions',
  target: 5,
  current: 3,
  unit: 'sessions',
  category: 'focus' as const,
  priority: 'high' as const,
  deadline: new Date(Date.now() + 24 * 60 * 60 * 1000), // Tomorrow
  progress: 0.6, // 60%
  type: 'daily' as const,
  pointsReward: 100,
  streakBonus: 50,
  milestones: [
    { value: 1, label: 'First Session', achieved: true, achievedAt: new Date(), pointsReward: 10 },
    { value: 3, label: 'Halfway There', achieved: true, achievedAt: new Date(), pointsReward: 30 },
    { value: 5, label: 'Daily Goal Complete', achieved: false, pointsReward: 60 }
  ]
}

const mockWeeklyGoal = {
  id: 'goal-2',
  title: 'Weekly Study Marathon',
  description: 'Study for 40 hours this week',
  target: 40,
  current: 28,
  unit: 'hours',
  category: 'productivity' as const,
  priority: 'medium' as const,
  deadline: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000), // Next week
  progress: 0.7, // 70%
  type: 'weekly' as const,
  pointsReward: 500,
  streakBonus: 100,
  milestones: [
    { value: 10, label: 'Strong Start', achieved: true, achievedAt: new Date(), pointsReward: 50 },
    { value: 20, label: 'Halfway Point', achieved: true, achievedAt: new Date(), pointsReward: 100 },
    { value: 30, label: 'Almost There', achieved: false, pointsReward: 150 },
    { value: 40, label: 'Week Complete', achieved: false, pointsReward: 200 }
  ]
}

const mockMonthlyGoal = {
  id: 'goal-3',
  title: 'Monthly Reading Challenge',
  description: 'Read 4 technical books this month',
  target: 4,
  current: 1,
  unit: 'books',
  category: 'learning' as const,
  priority: 'low' as const,
  deadline: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000), // Next month
  progress: 0.25, // 25%
  type: 'monthly' as const,
  pointsReward: 1000,
  streakBonus: 200,
  milestones: [
    { value: 1, label: 'First Book', achieved: true, achievedAt: new Date(), pointsReward: 100 },
    { value: 2, label: 'Halfway There', achieved: false, pointsReward: 250 },
    { value: 3, label: 'Almost Done', achieved: false, pointsReward: 350 },
    { value: 4, label: 'Challenge Complete', achieved: false, pointsReward: 500 }
  ]
}

const mockCompletedGoal = {
  id: 'goal-4',
  title: 'Join 3 Collaborative Hives',
  description: 'Expand your network by joining collaborative hives',
  target: 3,
  current: 3,
  unit: 'hives',
  category: 'collaboration' as const,
  priority: 'medium' as const,
  deadline: new Date(Date.now() - 24 * 60 * 60 * 1000), // Yesterday
  progress: 1.0, // 100%
  type: 'custom' as const,
  pointsReward: 300,
  streakBonus: 0,
  completedAt: new Date(),
  milestones: [
    { value: 1, label: 'First Hive', achieved: true, achievedAt: new Date(), pointsReward: 50 },
    { value: 2, label: 'Second Hive', achieved: true, achievedAt: new Date(), pointsReward: 100 },
    { value: 3, label: 'Goal Complete', achieved: true, achievedAt: new Date(), pointsReward: 150 }
  ]
}

const mockOverdueGoal = {
  id: 'goal-5',
  title: 'Failed Challenge',
  description: 'This goal was not completed in time',
  target: 10,
  current: 4,
  unit: 'tasks',
  category: 'productivity' as const,
  priority: 'high' as const,
  deadline: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000), // 2 days ago
  progress: 0.4, // 40%
  type: 'custom' as const,
  pointsReward: 200,
  streakBonus: 0,
  milestones: [
    { value: 2, label: 'Quick Start', achieved: true, achievedAt: new Date(), pointsReward: 20 },
    { value: 5, label: 'Halfway', achieved: false, pointsReward: 50 },
    { value: 10, label: 'Complete', achieved: false, pointsReward: 130 }
  ]
}

const mockNotStartedGoal = {
  id: 'goal-6',
  title: 'Weekend Wellness',
  description: 'Take regular breaks and maintain work-life balance',
  target: 8,
  current: 0,
  unit: 'breaks',
  category: 'wellness' as const,
  priority: 'low' as const,
  deadline: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000), // Day after tomorrow
  progress: 0, // 0%
  type: 'custom' as const,
  pointsReward: 150,
  streakBonus: 25,
  milestones: [
    { value: 2, label: 'First Steps', achieved: false, pointsReward: 25 },
    { value: 4, label: 'Halfway', achieved: false, pointsReward: 50 },
    { value: 8, label: 'Wellness Achieved', achieved: false, pointsReward: 75 }
  ]
}

const defaultProps = {
  goals: [mockDailyGoal, mockWeeklyGoal, mockMonthlyGoal],
  onGoalClick: vi.fn(),
  onMilestoneClick: vi.fn(),
  showMilestones: true,
  showAnimation: true,
  layout: 'grid' as const
}

describe('GoalProgress', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('Rendering Tests', () => {
    it('should render goal progress container', () => {
      renderWithProviders(<GoalProgress {...defaultProps} />)

      expect(screen.getByTestId('goal-progress-container')).toBeInTheDocument()
    })

    it('should render all goals when provided', () => {
      renderWithProviders(<GoalProgress {...defaultProps} />)

      expect(screen.getByTestId('goal-card-goal-1')).toBeInTheDocument()
      expect(screen.getByTestId('goal-card-goal-2')).toBeInTheDocument()
      expect(screen.getByTestId('goal-card-goal-3')).toBeInTheDocument()
    })

    it('should display goal titles correctly', () => {
      renderWithProviders(<GoalProgress {...defaultProps} />)

      expect(screen.getByText('Complete 5 Focus Sessions')).toBeInTheDocument()
      expect(screen.getByText('Weekly Study Marathon')).toBeInTheDocument()
      expect(screen.getByText('Monthly Reading Challenge')).toBeInTheDocument()
    })

    it('should display goal descriptions when expanded', () => {
      renderWithProviders(<GoalProgress {...defaultProps} />)

      expect(screen.getByText('Maintain daily focus with 5 Pomodoro sessions')).toBeInTheDocument()
      expect(screen.getByText('Study for 40 hours this week')).toBeInTheDocument()
    })

    it('should render empty state when no goals provided', () => {
      renderWithProviders(<GoalProgress {...defaultProps} goals={[]} />)

      expect(screen.getByTestId('no-goals-state')).toBeInTheDocument()
      expect(screen.getByText('No goals set yet')).toBeInTheDocument()
      expect(screen.getByText('Set your first goal to start earning points!')).toBeInTheDocument()
    })

    it('should render loading state when loading', () => {
      renderWithProviders(<GoalProgress {...defaultProps} loading />)

      expect(screen.getByTestId('goal-progress-loading')).toBeInTheDocument()
      expect(screen.getAllByTestId('goal-card-skeleton')).toHaveLength(3)
    })
  })

  describe('Progress Display Tests', () => {
    it('should display 0% progress correctly', () => {
      renderWithProviders(<GoalProgress {...defaultProps} goals={[mockNotStartedGoal]} />)

      const progressText = screen.getByTestId('progress-text-goal-6')
      expect(progressText).toHaveTextContent('0%')

      const progressBar = screen.getByTestId('progress-bar-goal-6')
      expect(progressBar).toHaveAttribute('aria-valuenow', '0')
    })

    it('should display 50% progress correctly', () => {
      const halfCompleteGoal = { ...mockDailyGoal, progress: 0.5, current: 2.5 }
      renderWithProviders(<GoalProgress {...defaultProps} goals={[halfCompleteGoal]} />)

      const progressText = screen.getByTestId('progress-text-goal-1')
      expect(progressText).toHaveTextContent('50%')

      const progressBar = screen.getByTestId('progress-bar-goal-1')
      expect(progressBar).toHaveAttribute('aria-valuenow', '50')
    })

    it('should display 100% progress correctly', () => {
      renderWithProviders(<GoalProgress {...defaultProps} goals={[mockCompletedGoal]} />)

      const progressText = screen.getByTestId('progress-text-goal-4')
      expect(progressText).toHaveTextContent('100%')

      const progressBar = screen.getByTestId('progress-bar-goal-4')
      expect(progressBar).toHaveAttribute('aria-valuenow', '100')
    })

    it('should display current vs target correctly', () => {
      renderWithProviders(<GoalProgress {...defaultProps} />)

      expect(screen.getByText('3 / 5 sessions')).toBeInTheDocument()
      expect(screen.getByText('28 / 40 hours')).toBeInTheDocument()
      expect(screen.getByText('1 / 4 books')).toBeInTheDocument()
    })

    it('should show points reward information', () => {
      renderWithProviders(<GoalProgress {...defaultProps} />)

      expect(screen.getByTestId('points-reward-goal-1')).toHaveTextContent('100 points')
      expect(screen.getByTestId('points-reward-goal-2')).toHaveTextContent('500 points')
    })

    it('should show streak bonus when applicable', () => {
      renderWithProviders(<GoalProgress {...defaultProps} />)

      expect(screen.getByTestId('streak-bonus-goal-1')).toHaveTextContent('+50 streak bonus')
      expect(screen.getByTestId('streak-bonus-goal-2')).toHaveTextContent('+100 streak bonus')
    })
  })

  describe('Goal State Tests', () => {
    it('should display not started state correctly', () => {
      renderWithProviders(<GoalProgress {...defaultProps} goals={[mockNotStartedGoal]} />)

      const goalCard = screen.getByTestId('goal-card-goal-6')
      expect(goalCard).toHaveClass('goal-not-started')

      expect(screen.getByTestId('status-indicator-goal-6')).toHaveClass('status-not-started')
      expect(screen.getByTestId('status-text-goal-6')).toHaveTextContent('Not Started')
    })

    it('should display in progress state correctly', () => {
      renderWithProviders(<GoalProgress {...defaultProps} goals={[mockDailyGoal]} />)

      const goalCard = screen.getByTestId('goal-card-goal-1')
      expect(goalCard).toHaveClass('goal-in-progress')

      expect(screen.getByTestId('status-indicator-goal-1')).toHaveClass('status-in-progress')
      expect(screen.getByTestId('status-text-goal-1')).toHaveTextContent('In Progress')
    })

    it('should display completed state correctly', () => {
      renderWithProviders(<GoalProgress {...defaultProps} goals={[mockCompletedGoal]} />)

      const goalCard = screen.getByTestId('goal-card-goal-4')
      expect(goalCard).toHaveClass('goal-completed')

      expect(screen.getByTestId('status-indicator-goal-4')).toHaveClass('status-completed')
      expect(screen.getByTestId('status-text-goal-4')).toHaveTextContent('Completed')
      expect(screen.getByTestId('completion-celebration-goal-4')).toBeInTheDocument()
    })

    it('should display overdue state correctly', () => {
      renderWithProviders(<GoalProgress {...defaultProps} goals={[mockOverdueGoal]} />)

      const goalCard = screen.getByTestId('goal-card-goal-5')
      expect(goalCard).toHaveClass('goal-overdue')

      expect(screen.getByTestId('status-indicator-goal-5')).toHaveClass('status-overdue')
      expect(screen.getByTestId('status-text-goal-5')).toHaveTextContent('Overdue')
      expect(screen.getByTestId('overdue-warning-goal-5')).toBeInTheDocument()
    })
  })

  describe('Milestone Display Tests', () => {
    it('should show milestones when showMilestones is true', () => {
      renderWithProviders(<GoalProgress {...defaultProps} showMilestones />)

      expect(screen.getByTestId('milestones-goal-1')).toBeInTheDocument()

      const milestones = within(screen.getByTestId('milestones-goal-1'))
      expect(milestones.getByText('First Session')).toBeInTheDocument()
      expect(milestones.getByText('Halfway There')).toBeInTheDocument()
      expect(milestones.getByText('Daily Goal Complete')).toBeInTheDocument()
    })

    it('should hide milestones when showMilestones is false', () => {
      renderWithProviders(<GoalProgress {...defaultProps} showMilestones={false} />)

      expect(screen.queryByTestId('milestones-goal-1')).not.toBeInTheDocument()
    })

    it('should show achieved milestones with checkmarks', () => {
      renderWithProviders(<GoalProgress {...defaultProps} showMilestones />)

      const milestones = within(screen.getByTestId('milestones-goal-1'))
      const achievedMilestones = milestones.getAllByTestId(/milestone-achieved/)
      expect(achievedMilestones).toHaveLength(2)

      achievedMilestones.forEach(milestone => {
        expect(milestone).toHaveClass('milestone-achieved')
        expect(within(milestone).getByTestId('check-icon')).toBeInTheDocument()
      })
    })

    it('should show unachieved milestones with progress indicators', () => {
      renderWithProviders(<GoalProgress {...defaultProps} showMilestones />)

      const milestones = within(screen.getByTestId('milestones-goal-1'))
      const unachievedMilestone = milestones.getByTestId('milestone-unachieved-2')

      expect(unachievedMilestone).toHaveClass('milestone-unachieved')
      expect(within(unachievedMilestone).getByTestId('progress-icon')).toBeInTheDocument()
    })

    it('should show milestone points rewards', () => {
      renderWithProviders(<GoalProgress {...defaultProps} showMilestones />)

      const milestones = within(screen.getByTestId('milestones-goal-1'))
      expect(milestones.getByText('10 pts')).toBeInTheDocument()
      expect(milestones.getByText('30 pts')).toBeInTheDocument()
      expect(milestones.getByText('60 pts')).toBeInTheDocument()
    })
  })

  describe('Animation Behavior Tests', () => {
    it('should show progress animation when showAnimation is true', async () => {
      renderWithProviders(<GoalProgress {...defaultProps} showAnimation />)

      const progressBar = screen.getByTestId('progress-bar-goal-1')
      expect(progressBar).toHaveClass('animated-progress')

      // Simulate progress update
      const updatedGoal = { ...mockDailyGoal, progress: 0.8, current: 4 }
      renderWithProviders(<GoalProgress {...defaultProps} goals={[updatedGoal]} showAnimation />)

      // Wait for animation
      await act(async () => {
        vi.advanceTimersByTime(1000)
      })

      expect(screen.getByTestId('progress-animation-goal-1')).toBeInTheDocument()
    })

    it('should not animate when showAnimation is false', () => {
      renderWithProviders(<GoalProgress {...defaultProps} showAnimation={false} />)

      const progressBar = screen.getByTestId('progress-bar-goal-1')
      expect(progressBar).not.toHaveClass('animated-progress')
    })

    it('should trigger celebration animation on goal completion', async () => {
      renderWithProviders(<GoalProgress {...defaultProps} goals={[mockCompletedGoal]} showAnimation />)

      await act(async () => {
        vi.advanceTimersByTime(500)
      })

      expect(screen.getByTestId('celebration-animation-goal-4')).toBeInTheDocument()
      expect(screen.getByTestId('confetti-effect')).toBeInTheDocument()
    })

    it('should animate milestone achievement', async () => {
      const goalWithNewMilestone = {
        ...mockDailyGoal,
        milestones: [
          ...mockDailyGoal.milestones.slice(0, 2),
          { ...mockDailyGoal.milestones[2], achieved: true, achievedAt: new Date() }
        ]
      }

      renderWithProviders(<GoalProgress {...defaultProps} goals={[goalWithNewMilestone]} showAnimation />)

      await act(async () => {
        vi.advanceTimersByTime(300)
      })

      expect(screen.getByTestId('milestone-achievement-animation')).toBeInTheDocument()
    })
  })

  describe('Color Coding Tests', () => {
    it('should use correct colors for completion status', () => {
      renderWithProviders(<GoalProgress {...defaultProps} goals={[
        mockNotStartedGoal,
        mockDailyGoal,
        mockCompletedGoal,
        mockOverdueGoal
      ]} />)

      // Not started - gray
      expect(screen.getByTestId('progress-bar-goal-6')).toHaveClass('progress-not-started')

      // In progress - blue/primary
      expect(screen.getByTestId('progress-bar-goal-1')).toHaveClass('progress-in-progress')

      // Completed - green
      expect(screen.getByTestId('progress-bar-goal-4')).toHaveClass('progress-completed')

      // Overdue - red
      expect(screen.getByTestId('progress-bar-goal-5')).toHaveClass('progress-overdue')
    })

    it('should use priority-based color coding', () => {
      renderWithProviders(<GoalProgress {...defaultProps} />)

      // High priority - red accent
      expect(screen.getByTestId('priority-indicator-goal-1')).toHaveClass('priority-high')

      // Medium priority - orange accent
      expect(screen.getByTestId('priority-indicator-goal-2')).toHaveClass('priority-medium')

      // Low priority - green accent
      expect(screen.getByTestId('priority-indicator-goal-3')).toHaveClass('priority-low')
    })

    it('should use category-based color schemes', () => {
      renderWithProviders(<GoalProgress {...defaultProps} />)

      expect(screen.getByTestId('category-badge-goal-1')).toHaveClass('category-focus')
      expect(screen.getByTestId('category-badge-goal-2')).toHaveClass('category-productivity')
      expect(screen.getByTestId('category-badge-goal-3')).toHaveClass('category-learning')
    })

    it('should show progress gradient based on completion percentage', () => {
      const goals = [
        { ...mockDailyGoal, progress: 0.25 },
        { ...mockDailyGoal, progress: 0.5 },
        { ...mockDailyGoal, progress: 0.75 },
        { ...mockDailyGoal, progress: 1.0 }
      ]

      renderWithProviders(<GoalProgress {...defaultProps} goals={goals} />)

      goals.forEach((goal, index) => {
        const progressBar = screen.getByTestId(`progress-bar-goal-${index + 1}`)
        const expectedClass = `progress-${Math.floor(goal.progress * 4) * 25}`
        expect(progressBar).toHaveClass(expectedClass)
      })
    })
  })

  describe('Goal Type Support Tests', () => {
    it('should display daily goals with appropriate indicators', () => {
      renderWithProviders(<GoalProgress {...defaultProps} goals={[mockDailyGoal]} />)

      expect(screen.getByTestId('goal-type-daily-goal-1')).toBeInTheDocument()
      expect(screen.getByTestId('deadline-indicator-goal-1')).toHaveTextContent('Due today')
    })

    it('should display weekly goals with week progress', () => {
      renderWithProviders(<GoalProgress {...defaultProps} goals={[mockWeeklyGoal]} />)

      expect(screen.getByTestId('goal-type-weekly-goal-2')).toBeInTheDocument()
      expect(screen.getByTestId('week-progress-goal-2')).toBeInTheDocument()
    })

    it('should display monthly goals with month progress', () => {
      renderWithProviders(<GoalProgress {...defaultProps} goals={[mockMonthlyGoal]} />)

      expect(screen.getByTestId('goal-type-monthly-goal-3')).toBeInTheDocument()
      expect(screen.getByTestId('month-progress-goal-3')).toBeInTheDocument()
    })

    it('should display custom goals with flexible deadlines', () => {
      renderWithProviders(<GoalProgress {...defaultProps} goals={[mockCompletedGoal]} />)

      expect(screen.getByTestId('goal-type-custom-goal-4')).toBeInTheDocument()
      expect(screen.getByTestId('custom-deadline-goal-4')).toBeInTheDocument()
    })

    it('should show time remaining for active goals', () => {
      renderWithProviders(<GoalProgress {...defaultProps} />)

      expect(screen.getByTestId('time-remaining-goal-1')).toHaveTextContent(/\d+ hours? remaining/)
      expect(screen.getByTestId('time-remaining-goal-2')).toHaveTextContent(/\d+ days? remaining/)
    })
  })

  describe('Accessibility Tests', () => {
    it('should have proper ARIA labels for progress bars', () => {
      renderWithProviders(<GoalProgress {...defaultProps} />)

      const progressBar = screen.getByTestId('progress-bar-goal-1')
      expect(progressBar).toHaveAttribute('role', 'progressbar')
      expect(progressBar).toHaveAttribute('aria-label', 'Goal progress: 60% complete')
      expect(progressBar).toHaveAttribute('aria-valuenow', '60')
      expect(progressBar).toHaveAttribute('aria-valuemin', '0')
      expect(progressBar).toHaveAttribute('aria-valuemax', '100')
    })

    it('should have accessible milestone navigation', () => {
      renderWithProviders(<GoalProgress {...defaultProps} showMilestones />)

      const milestones = screen.getByTestId('milestones-goal-1')
      expect(milestones).toHaveAttribute('role', 'list')
      expect(milestones).toHaveAttribute('aria-label', 'Goal milestones')

      const milestoneItems = within(milestones).getAllByRole('listitem')
      milestoneItems.forEach((item, index) => {
        expect(item).toHaveAttribute('aria-label', expect.stringContaining('Milestone'))
      })
    })

    it('should support keyboard navigation', async () => {
      const user = userEvent.setup()
      const onGoalClick = vi.fn()

      renderWithProviders(<GoalProgress {...defaultProps} onGoalClick={onGoalClick} />)

      const goalCard = screen.getByTestId('goal-card-goal-1')
      expect(goalCard).toHaveAttribute('tabindex', '0')

      goalCard.focus()
      await user.keyboard('{Enter}')

      expect(onGoalClick).toHaveBeenCalledWith(mockDailyGoal)
    })

    it('should have screen reader friendly status announcements', () => {
      renderWithProviders(<GoalProgress {...defaultProps} goals={[mockCompletedGoal]} />)

      expect(screen.getByTestId('sr-status-goal-4')).toHaveTextContent(
        'Goal completed: Join 3 Collaborative Hives, 100% complete, earned 300 points'
      )
    })

    it('should have high contrast mode support', () => {
      renderWithProviders(<GoalProgress {...defaultProps} highContrast />)

      const container = screen.getByTestId('goal-progress-container')
      expect(container).toHaveClass('high-contrast-mode')

      const progressBars = screen.getAllByTestId(/progress-bar/)
      progressBars.forEach(bar => {
        expect(bar).toHaveClass('high-contrast-progress')
      })
    })

    it('should provide focus indicators', async () => {
      const user = userEvent.setup()
      renderWithProviders(<GoalProgress {...defaultProps} />)

      const goalCard = screen.getByTestId('goal-card-goal-1')
      await user.tab()

      expect(goalCard).toHaveFocus()
      expect(goalCard).toHaveClass('focus-visible')
    })
  })

  describe('User Interaction Tests', () => {
    it('should call onGoalClick when goal card is clicked', async () => {
      const user = userEvent.setup()
      const onGoalClick = vi.fn()

      renderWithProviders(<GoalProgress {...defaultProps} onGoalClick={onGoalClick} />)

      const goalCard = screen.getByTestId('goal-card-goal-1')
      await user.click(goalCard)

      expect(onGoalClick).toHaveBeenCalledWith(mockDailyGoal)
    })

    it('should call onMilestoneClick when milestone is clicked', async () => {
      const user = userEvent.setup()
      const onMilestoneClick = vi.fn()

      renderWithProviders(<GoalProgress {...defaultProps} onMilestoneClick={onMilestoneClick} showMilestones />)

      const milestone = screen.getByTestId('milestone-achieved-0-goal-1')
      await user.click(milestone)

      expect(onMilestoneClick).toHaveBeenCalledWith(mockDailyGoal.milestones[0], mockDailyGoal)
    })

    it('should expand/collapse goal details on toggle', async () => {
      const user = userEvent.setup()
      renderWithProviders(<GoalProgress {...defaultProps} />)

      const expandButton = screen.getByTestId('expand-button-goal-1')
      await user.click(expandButton)

      expect(screen.getByTestId('goal-details-goal-1')).toHaveClass('expanded')

      await user.click(expandButton)
      expect(screen.getByTestId('goal-details-goal-1')).toHaveClass('collapsed')
    })

    it('should handle goal card hover effects', async () => {
      const user = userEvent.setup()
      renderWithProviders(<GoalProgress {...defaultProps} />)

      const goalCard = screen.getByTestId('goal-card-goal-1')
      await user.hover(goalCard)

      expect(goalCard).toHaveClass('hovered')
      expect(screen.getByTestId('hover-actions-goal-1')).toBeInTheDocument()
    })

    it('should show tooltip on progress bar hover', async () => {
      const user = userEvent.setup()
      renderWithProviders(<GoalProgress {...defaultProps} />)

      const progressBar = screen.getByTestId('progress-bar-goal-1')
      await user.hover(progressBar)

      expect(screen.getByTestId('progress-tooltip')).toBeInTheDocument()
      expect(screen.getByTestId('progress-tooltip')).toHaveTextContent('3 of 5 sessions completed (60%)')
    })
  })

  describe('Layout and Responsive Design Tests', () => {
    it('should render in grid layout by default', () => {
      renderWithProviders(<GoalProgress {...defaultProps} layout="grid" />)

      const container = screen.getByTestId('goals-container')
      expect(container).toHaveClass('goals-grid-layout')
    })

    it('should render in list layout when specified', () => {
      renderWithProviders(<GoalProgress {...defaultProps} layout="list" />)

      const container = screen.getByTestId('goals-container')
      expect(container).toHaveClass('goals-list-layout')
    })

    it('should adapt to compact mode', () => {
      renderWithProviders(<GoalProgress {...defaultProps} compact />)

      const container = screen.getByTestId('goal-progress-container')
      expect(container).toHaveClass('compact-mode')

      // Milestones should be hidden in compact mode
      expect(screen.queryByTestId('milestones-goal-1')).not.toBeInTheDocument()
    })

    it('should handle mobile responsive design', () => {
      // Mock mobile viewport
      Object.defineProperty(window, 'innerWidth', { value: 375 })

      renderWithProviders(<GoalProgress {...defaultProps} />)

      const container = screen.getByTestId('goal-progress-container')
      expect(container).toHaveClass('mobile-layout')

      // Goals should stack in single column on mobile
      const goalsContainer = screen.getByTestId('goals-container')
      expect(goalsContainer).toHaveStyle('grid-template-columns: 1fr')
    })

    it('should handle tablet responsive design', () => {
      // Mock tablet viewport
      Object.defineProperty(window, 'innerWidth', { value: 768 })

      renderWithProviders(<GoalProgress {...defaultProps} />)

      const goalsContainer = screen.getByTestId('goals-container')
      expect(goalsContainer).toHaveStyle('grid-template-columns: repeat(2, 1fr)')
    })
  })

  describe('Error Handling and Edge Cases', () => {
    it('should handle empty goals array gracefully', () => {
      renderWithProviders(<GoalProgress {...defaultProps} goals={[]} />)

      expect(screen.getByTestId('no-goals-state')).toBeInTheDocument()
      expect(screen.queryByTestId('goals-container')).not.toBeInTheDocument()
    })

    it('should handle goals with missing data gracefully', () => {
      const incompleteGoal = {
        id: 'incomplete-goal',
        title: 'Test Goal',
        description: 'Test description',
        target: 10,
        current: 0,
        unit: 'items',
        category: 'productivity' as const,
        priority: 'medium' as const,
        deadline: new Date(Date.now() + 24 * 60 * 60 * 1000),
        progress: 0,
        type: 'custom' as const,
        pointsReward: 100,
        streakBonus: 0,
        milestones: []
      }

      renderWithProviders(<GoalProgress {...defaultProps} goals={[incompleteGoal]} />)

      const goalCard = screen.getByTestId('goal-card-incomplete-goal')
      expect(goalCard).toBeInTheDocument()
      expect(screen.getByText('Test Goal')).toBeInTheDocument()

      // Should show default values
      expect(screen.getByTestId('progress-bar-incomplete-goal')).toHaveAttribute('aria-valuenow', '0')
    })

    it('should handle invalid progress values', () => {
      const invalidGoal = { ...mockDailyGoal, progress: -0.5 }
      renderWithProviders(<GoalProgress {...defaultProps} goals={[invalidGoal]} />)

      const progressBar = screen.getByTestId('progress-bar-goal-1')
      expect(progressBar).toHaveAttribute('aria-valuenow', '0')
    })

    it('should handle progress values over 100%', () => {
      const overcompletedGoal = { ...mockDailyGoal, progress: 1.5, current: 7.5 }
      renderWithProviders(<GoalProgress {...defaultProps} goals={[overcompletedGoal]} />)

      const progressBar = screen.getByTestId('progress-bar-goal-1')
      expect(progressBar).toHaveAttribute('aria-valuenow', '100')
      expect(screen.getByTestId('progress-text-goal-1')).toHaveTextContent('100%')
    })

    it('should handle network errors during goal updates', async () => {
      const onGoalClick = vi.fn().mockRejectedValue(new Error('Network error'))
      const user = userEvent.setup()

      renderWithProviders(<GoalProgress {...defaultProps} onGoalClick={onGoalClick} />)

      const goalCard = screen.getByTestId('goal-card-goal-1')
      await user.click(goalCard)

      expect(screen.getByTestId('error-message')).toHaveTextContent('Failed to update goal')
    })
  })

  describe('Performance and Optimization Tests', () => {
    it('should memoize goal cards to prevent unnecessary re-renders', () => {
      const { rerender } = renderWithProviders(<GoalProgress {...defaultProps} />)

      const goalCard = screen.getByTestId('goal-card-goal-1')
      const initialHTML = goalCard.innerHTML

      // Re-render with same props
      rerender(<GoalProgress {...defaultProps} />)

      expect(goalCard.innerHTML).toBe(initialHTML)
    })

    it('should handle large numbers of goals efficiently', () => {
      const manyGoals = Array.from({ length: 100 }, (_, i) => ({
        ...mockDailyGoal,
        id: `goal-${i}`,
        title: `Goal ${i}`
      }))

      const startTime = performance.now()
      renderWithProviders(<GoalProgress {...defaultProps} goals={manyGoals} />)
      const endTime = performance.now()

      // Render should complete quickly even with many goals
      expect(endTime - startTime).toBeLessThan(1000)
    })

    it('should virtualize goals list when many goals are present', () => {
      const manyGoals = Array.from({ length: 1000 }, (_, i) => ({
        ...mockDailyGoal,
        id: `goal-${i}`,
        title: `Goal ${i}`
      }))

      renderWithProviders(<GoalProgress {...defaultProps} goals={manyGoals} />)

      // Should only render visible goals
      const renderedGoals = screen.getAllByTestId(/goal-card/)
      expect(renderedGoals.length).toBeLessThan(50) // Virtualization threshold
    })
  })
})