import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { GoalProgress } from './GoalProgress';
import { GoalProgressProps, GoalProgressData } from '../types';

const mockGoals: GoalProgressData[] = [
  {
    id: '1',
    title: 'Daily Focus Goal',
    description: 'Complete 4 hours of focused work daily',
    target: 240,
    current: 180,
    unit: 'minutes',
    progress: 0.75,
    deadline: new Date('2024-01-31T23:59:59Z'),
    category: 'focus',
    priority: 'high',
    milestones: [
      { value: 60, label: '1 hour', achieved: true, achievedAt: new Date('2024-01-01T09:00:00Z') },
      { value: 120, label: '2 hours', achieved: true, achievedAt: new Date('2024-01-01T11:00:00Z') },
      { value: 180, label: '3 hours', achieved: true, achievedAt: new Date('2024-01-01T14:00:00Z') },
      { value: 240, label: '4 hours', achieved: false }
    ]
  },
  {
    id: '2',
    title: 'Weekly Sessions',
    description: 'Complete 20 focus sessions this week',
    target: 20,
    current: 12,
    unit: 'sessions',
    progress: 0.6,
    category: 'productivity',
    priority: 'medium',
    milestones: [
      { value: 5, label: 'First 5', achieved: true, achievedAt: new Date('2024-01-01T10:00:00Z') },
      { value: 10, label: 'Halfway', achieved: true, achievedAt: new Date('2024-01-02T15:00:00Z') },
      { value: 15, label: 'Almost there', achieved: false },
      { value: 20, label: 'Complete', achieved: false }
    ]
  },
  {
    id: '3',
    title: 'Collaboration Hours',
    description: 'Spend 10 hours in hive sessions with teammates',
    target: 600,
    current: 720,
    unit: 'minutes',
    progress: 1.2, // Over 100%
    category: 'collaboration',
    priority: 'low',
    milestones: [
      { value: 150, label: '2.5 hours', achieved: true, achievedAt: new Date('2024-01-01T12:00:00Z') },
      { value: 300, label: '5 hours', achieved: true, achievedAt: new Date('2024-01-02T16:00:00Z') },
      { value: 450, label: '7.5 hours', achieved: true, achievedAt: new Date('2024-01-03T14:00:00Z') },
      { value: 600, label: '10 hours', achieved: true, achievedAt: new Date('2024-01-04T11:00:00Z') }
    ]
  },
  {
    id: '4',
    title: 'Wellness Breaks',
    description: 'Take meaningful breaks every day',
    target: 7,
    current: 3,
    unit: 'days',
    progress: 0.43,
    deadline: new Date('2024-01-07T23:59:59Z'),
    category: 'wellness',
    priority: 'medium',
    milestones: [
      { value: 2, label: 'Getting started', achieved: true, achievedAt: new Date('2024-01-02T18:00:00Z') },
      { value: 4, label: 'Halfway', achieved: false },
      { value: 7, label: 'Full week', achieved: false }
    ]
  }
];

const defaultProps: GoalProgressProps = {
  goals: mockGoals
};

describe('GoalProgress', () => {
  it('renders without crashing', () => {
    render(<GoalProgress {...defaultProps} />);
    expect(screen.getByText('Goal Progress')).toBeInTheDocument();
  });

  it('displays all goals in grid layout by default', () => {
    render(<GoalProgress {...defaultProps} />);
    
    expect(screen.getByText('Daily Focus Goal')).toBeInTheDocument();
    expect(screen.getByText('Weekly Sessions')).toBeInTheDocument();
    expect(screen.getByText('Collaboration Hours')).toBeInTheDocument();
    expect(screen.getByText('Wellness Breaks')).toBeInTheDocument();
  });

  it('displays goals in list layout when specified', () => {
    render(<GoalProgress {...defaultProps} layout="list" />);
    
    const goalContainer = screen.getByTestId('goals-container');
    expect(goalContainer).toHaveClass('goals-list-layout');
  });

  it('shows progress percentages correctly', () => {
    render(<GoalProgress {...defaultProps} />);
    
    expect(screen.getByText('75%')).toBeInTheDocument(); // Daily Focus Goal
    expect(screen.getByText('60%')).toBeInTheDocument(); // Weekly Sessions
    expect(screen.getByText('120%')).toBeInTheDocument(); // Collaboration Hours (over 100%)
    expect(screen.getByText('43%')).toBeInTheDocument(); // Wellness Breaks
  });

  it('displays current/target values with units', () => {
    render(<GoalProgress {...defaultProps} />);
    
    expect(screen.getByText('180 / 240 minutes')).toBeInTheDocument();
    expect(screen.getByText('12 / 20 sessions')).toBeInTheDocument();
    expect(screen.getByText('720 / 600 minutes')).toBeInTheDocument(); // Over target
    expect(screen.getByText('3 / 7 days')).toBeInTheDocument();
  });

  it('shows priority indicators with correct colors', () => {
    render(<GoalProgress {...defaultProps} />);
    
    const highPriorityGoal = screen.getByTestId('goal-card-1');
    expect(highPriorityGoal.querySelector('.priority-indicator')).toHaveClass('priority-high');
    
    const mediumPriorityGoals = screen.getAllByTestId(/goal-card-[24]/);
    mediumPriorityGoals.forEach(goal => {
      expect(goal.querySelector('.priority-indicator')).toHaveClass('priority-medium');
    });
    
    const lowPriorityGoal = screen.getByTestId('goal-card-3');
    expect(lowPriorityGoal.querySelector('.priority-indicator')).toHaveClass('priority-low');
  });

  it('displays category badges with correct styling', () => {
    render(<GoalProgress {...defaultProps} />);
    
    expect(screen.getByText('Focus')).toBeInTheDocument();
    expect(screen.getByText('Productivity')).toBeInTheDocument();
    expect(screen.getByText('Collaboration')).toBeInTheDocument();
    expect(screen.getByText('Wellness')).toBeInTheDocument();
  });

  it('shows deadlines when present', () => {
    render(<GoalProgress {...defaultProps} />);
    
    expect(screen.getByText('Due: Jan 31, 2024')).toBeInTheDocument();
    expect(screen.getByText('Due: Jan 7, 2024')).toBeInTheDocument();
  });

  it('indicates overdue goals', () => {
    const overdueGoals = mockGoals.map(goal => ({
      ...goal,
      deadline: goal.deadline ? new Date('2023-12-31T23:59:59Z') : undefined // Past date
    }));
    
    render(<GoalProgress goals={overdueGoals} />);
    
    const overdueIndicators = screen.getAllByText('Overdue');
    expect(overdueIndicators.length).toBeGreaterThan(0);
  });

  it('shows completed goals with success styling', () => {
    render(<GoalProgress {...defaultProps} />);
    
    const completedGoal = screen.getByTestId('goal-card-3'); // Collaboration Hours (120%)
    expect(completedGoal).toHaveClass('goal-completed');
    expect(screen.getByText('Completed!')).toBeInTheDocument();
  });

  it('displays milestones when showMilestones is true', () => {
    render(<GoalProgress {...defaultProps} showMilestones={true} />);
    
    expect(screen.getByText('1 hour')).toBeInTheDocument();
    expect(screen.getByText('2 hours')).toBeInTheDocument();
    expect(screen.getByText('3 hours')).toBeInTheDocument();
    expect(screen.getByText('4 hours')).toBeInTheDocument();
  });

  it('hides milestones when showMilestones is false', () => {
    render(<GoalProgress {...defaultProps} showMilestones={false} />);
    
    expect(screen.queryByText('1 hour')).not.toBeInTheDocument();
    expect(screen.queryByText('2 hours')).not.toBeInTheDocument();
  });

  it('indicates achieved milestones with checkmarks', () => {
    render(<GoalProgress {...defaultProps} showMilestones={true} />);
    
    const achievedMilestones = screen.getAllByLabelText('milestone achieved');
    expect(achievedMilestones.length).toBeGreaterThan(0);
  });

  it('calls onGoalClick when a goal is clicked', () => {
    const onGoalClick = vi.fn();
    render(<GoalProgress {...defaultProps} onGoalClick={onGoalClick} />);
    
    const goalCard = screen.getByTestId('goal-card-1');
    fireEvent.click(goalCard);
    
    expect(onGoalClick).toHaveBeenCalledWith(mockGoals[0]);
  });

  it('renders progress bars with correct fill levels', () => {
    render(<GoalProgress {...defaultProps} />);
    
    const progressBar1 = screen.getByTestId('progress-bar-1');
    expect(progressBar1).toHaveAttribute('aria-valuenow', '75');
    
    const progressBar2 = screen.getByTestId('progress-bar-2');
    expect(progressBar2).toHaveAttribute('aria-valuenow', '60');
    
    const progressBar3 = screen.getByTestId('progress-bar-3');
    expect(progressBar3).toHaveAttribute('aria-valuenow', '100'); // Capped at 100 for display
    
    const progressBar4 = screen.getByTestId('progress-bar-4');
    expect(progressBar4).toHaveAttribute('aria-valuenow', '43');
  });

  it('handles empty goals list gracefully', () => {
    render(<GoalProgress goals={[]} />);
    
    expect(screen.getByText('Goal Progress')).toBeInTheDocument();
    expect(screen.getByText('No goals set yet')).toBeInTheDocument();
    expect(screen.getByText('Set your first goal to start tracking progress!')).toBeInTheDocument();
  });

  it('shows goal descriptions when expanded', () => {
    render(<GoalProgress {...defaultProps} />);
    
    const expandButton = screen.getAllByLabelText('expand goal details')[0];
    fireEvent.click(expandButton);
    
    expect(screen.getByText('Complete 4 hours of focused work daily')).toBeInTheDocument();
  });

  it('sorts goals by progress when requested', () => {
    render(<GoalProgress {...defaultProps} />);
    
    const sortButton = screen.getByLabelText('sort by progress');
    fireEvent.click(sortButton);
    
    const goalCards = screen.getAllByTestId(/goal-card-/);
    // First goal should be the completed one (120%)
    expect(goalCards[0]).toHaveAttribute('data-testid', 'goal-card-3');
  });

  it('filters goals by category', () => {
    render(<GoalProgress {...defaultProps} />);
    
    const focusFilterButton = screen.getByText('Focus');
    fireEvent.click(focusFilterButton);
    
    expect(screen.getByText('Daily Focus Goal')).toBeInTheDocument();
    expect(screen.queryByText('Weekly Sessions')).not.toBeInTheDocument();
  });

  it('shows time remaining for goals with deadlines', () => {
    // Mock current date to be before deadline
    const mockDate = new Date('2024-01-15T12:00:00Z');
    vi.setSystemTime(mockDate);
    
    render(<GoalProgress {...defaultProps} />);
    
    expect(screen.getByText(/16 days left/)).toBeInTheDocument(); // Jan 31 - Jan 15
    
    vi.useRealTimers();
  });
});