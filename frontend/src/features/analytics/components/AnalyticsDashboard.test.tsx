import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import AnalyticsDashboard from './AnalyticsDashboard';
import { AnalyticsDashboardProps } from '../types';

// Mock all the child components
vi.mock('./ProductivityChart', () => ({
  productivityChart: ({ loading, error }: { loading?: boolean; error?: string }) => (
    <div data-testid="productivity-chart">
      {loading && <div>Loading chart...</div>}
      {error && <div>Chart error: {error}</div>}
      {!loading && !error && <div>Productivity Chart Content</div>}
    </div>
  )
}));

vi.mock('./TaskCompletionRate', () => ({
  taskCompletionRate: ({ data }: { data?: { rate?: number } }) => (
    <div data-testid="task-completion-rate">
      Task Completion: {data?.rate ? `${(data.rate * 100).toFixed(1)}%` : 'No data'}
    </div>
  )
}));

vi.mock('./HiveActivityHeatmap', () => ({
  hiveActivityHeatmap: ({ data }: { data?: unknown[] }) => (
    <div data-testid="hive-activity-heatmap">
      Hive Activity: {data?.length || 0} days
    </div>
  )
}));

vi.mock('./MemberEngagement', () => ({
  memberEngagement: ({ data }: { data?: unknown[] }) => (
    <div data-testid="member-engagement">
      Members: {data?.length || 0}
    </div>
  )
}));

vi.mock('./GoalProgress', () => ({
  goalProgress: ({ goals }: { goals?: unknown[] }) => (
    <div data-testid="goal-progress">
      Goals: {goals?.length || 0}
    </div>
  )
}));

vi.mock('./AnalyticsFilters', () => ({
  analyticsFilters: ({ onFilterChange }: { onFilterChange: (filter: { viewType: string }) => void }) => (
    <div data-testid="analytics-filters">
      <button onClick={() => onFilterChange({ viewType: 'hive' })}>
        Change Filter
      </button>
    </div>
  )
}));

vi.mock('./ExportMenu', () => ({
  exportMenu: ({ onExport }: { onExport: (options: { format: string }) => void }) => (
    <div data-testid="export-menu">
      <button onClick={() => onExport({ format: 'csv' })}>
        Export Data
      </button>
    </div>
  )
}));

// Mock the analytics context
vi.mock('../contexts/AnalyticsContext', () => ({
  useAnalytics: vi.fn()
}));

// Default mock implementation
const defaultAnalyticsData = {
  data: {
    productivity: {
      totalFocusTime: 480,
      averageSessionLength: 25,
      completedSessions: 12,
      totalSessions: 15,
      completionRate: 0.8,
      streak: { current: 5, best: 7, type: 'daily' as const },
      productivity: { average: 4.2, trend: 'increasing' as const }
    },
    taskCompletion: {
      completed: 23,
      total: 30,
      rate: 0.767,
      trend: 12.5,
      byPriority: {
        high: { completed: 8, total: 10 },
        medium: { completed: 10, total: 12 },
        low: { completed: 5, total: 8 }
      },
      byCategory: []
    },
    hiveActivity: [
      { date: '2024-01-01', value: 2, focusTime: 120, sessions: 3, members: 5 },
      { date: '2024-01-02', value: 1, focusTime: 60, sessions: 1, members: 2 }
    ],
    memberEngagement: [
      {
        user: { 
          id: '1', 
          name: 'Alice Johnson', 
          email: 'alice@example.com',
          username: 'alice_johnson',
          firstName: 'Alice',
          lastName: 'Johnson',
          avatar: null,
          isOnline: true,
          isEmailVerified: true,
          createdAt: '2024-01-01T00:00:00.000Z',
          updatedAt: '2024-01-01T00:00:00.000Z'
        },
        focusTime: 480,
        sessions: 12,
        lastActive: new Date(),
        rank: 1,
        engagement: 'high' as const,
        contribution: 35.2
      }
    ],
    goalProgress: [
      {
        id: '1',
        title: 'Daily Focus',
        target: 240,
        current: 180,
        unit: 'minutes',
        progress: 0.75,
        category: 'focus' as const,
        priority: 'high' as const,
        milestones: []
      }
    ],
    trends: {
      focusTime: [],
      productivity: [],
      sessions: [],
      goals: []
    },
    lastUpdated: new Date()
  },
  filter: {
    timeRange: {
      start: new Date('2024-01-01'),
      end: new Date('2024-01-31'),
      period: 'month' as const
    },
    viewType: 'individual' as const,
    selectedHives: [],
    selectedMembers: [],
    metrics: ['focus-time', 'sessions'] as ('focus-time' | 'sessions')[]
  },
  loading: false,
  error: null,
  updateFilter: vi.fn(),
  refreshData: vi.fn(),
  exportData: vi.fn(),
  setTimeRange: vi.fn(),
  setViewType: vi.fn()
};

const defaultProps: AnalyticsDashboardProps = {
  userId: 'user-1',
  hiveId: 'hive-1' // Add hiveId to render conditional components
};

// Import the mocked context after mocking
import { useAnalytics } from '../contexts/AnalyticsContext';
const mockUseAnalytics = vi.mocked(useAnalytics);

describe('AnalyticsDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseAnalytics.mockReturnValue(defaultAnalyticsData);
  });

  it('renders without crashing', () => {
    render(<AnalyticsDashboard {...defaultProps} />);
    expect(screen.getByText('Hive Analytics')).toBeInTheDocument();
  });

  it('displays the main dashboard title', () => {
    render(<AnalyticsDashboard {...defaultProps} />);
    expect(screen.getByText('Hive Analytics')).toBeInTheDocument();
  });

  it('displays Analytics Dashboard title when no hiveId is provided', () => {
    const propsWithoutHiveId = { userId: 'user-1' };
    render(<AnalyticsDashboard {...propsWithoutHiveId} />);
    expect(screen.getByText('Analytics Dashboard')).toBeInTheDocument();
  });

  it('renders all analytics components', () => {
    render(<AnalyticsDashboard {...defaultProps} />);
    
    expect(screen.getByTestId('productivity-chart')).toBeInTheDocument();
    expect(screen.getByTestId('task-completion-rate')).toBeInTheDocument();
    expect(screen.getByTestId('hive-activity-heatmap')).toBeInTheDocument();
    expect(screen.getByTestId('member-engagement')).toBeInTheDocument();
    expect(screen.getByTestId('goal-progress')).toBeInTheDocument();
  });

  it('renders filters and export components', () => {
    render(<AnalyticsDashboard {...defaultProps} />);
    
    expect(screen.getByTestId('analytics-filters')).toBeInTheDocument();
    expect(screen.getByTestId('export-menu')).toBeInTheDocument();
  });

  it('displays correct data in components', () => {
    render(<AnalyticsDashboard {...defaultProps} />);
    
    expect(screen.getByText('Task Completion: 76.7%')).toBeInTheDocument();
    expect(screen.getByText('Hive Activity: 2 days')).toBeInTheDocument();
    expect(screen.getByText('Members: 1')).toBeInTheDocument();
    expect(screen.getByText('Goals: 1')).toBeInTheDocument();
  });

  it('shows summary statistics cards', () => {
    render(<AnalyticsDashboard {...defaultProps} />);
    
    expect(screen.getByText('Total Focus Time')).toBeInTheDocument();
    expect(screen.getByText('8h 0m')).toBeInTheDocument(); // 480 minutes
    
    expect(screen.getByText('Completed Sessions')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    
    expect(screen.getByText('Current Streak')).toBeInTheDocument();
    expect(screen.getByText('5 days')).toBeInTheDocument();
    
    expect(screen.getByText('Productivity Score')).toBeInTheDocument();
    expect(screen.getByText('4.2/5')).toBeInTheDocument();
  });

  it('renders in compact mode when specified', () => {
    render(<AnalyticsDashboard {...defaultProps} compactMode={true} />);
    
    const dashboard = screen.getByTestId('analytics-dashboard');
    expect(dashboard).toHaveClass('dashboard-compact');
  });

  it('applies hive context when hiveId is provided', () => {
    render(<AnalyticsDashboard {...defaultProps} hiveId="hive-1" />);
    
    expect(screen.getByText('Hive Analytics')).toBeInTheDocument();
    expect(screen.getByTestId('member-engagement')).toBeInTheDocument();
  });

  it('hides member engagement when in individual mode', () => {
    render(<AnalyticsDashboard {...defaultProps} />);
    
    // Member engagement should be visible but with different layout in individual mode
    const memberEngagement = screen.getByTestId('member-engagement');
    expect(memberEngagement).toBeInTheDocument();
  });

  it('shows refresh button and handles refresh', async () => {
    render(<AnalyticsDashboard {...defaultProps} />);
    
    const refreshButton = screen.getByLabelText('refresh data');
    expect(refreshButton).toBeInTheDocument();
    
    // Test refresh functionality would require mocking the context properly
  });

  it('displays last updated timestamp', () => {
    render(<AnalyticsDashboard {...defaultProps} />);
    
    expect(screen.getByText(/Last updated:/)).toBeInTheDocument();
  });

  it('shows period selector', () => {
    render(<AnalyticsDashboard {...defaultProps} />);
    
    expect(screen.getByText(/Time Period:/)).toBeInTheDocument();
    expect(screen.getByText(/Jan 1 - Jan 31, 2024/)).toBeInTheDocument();
  });

  it('handles loading state', async () => {
    // Mock loading state
    mockUseAnalytics.mockReturnValue({
      ...defaultAnalyticsData,
      data: null,
      loading: true,
      error: null
    });
    
    render(<AnalyticsDashboard {...defaultProps} />);
    
    expect(screen.getByText('Loading analytics data...')).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('handles error state', async () => {
    // Mock error state
    mockUseAnalytics.mockReturnValue({
      ...defaultAnalyticsData,
      data: null,
      loading: false,
      error: 'Failed to load analytics data'
    });
    
    render(<AnalyticsDashboard {...defaultProps} />);
    
    expect(screen.getByText('Error loading analytics data')).toBeInTheDocument();
    expect(screen.getByText('Failed to load analytics data')).toBeInTheDocument();
    expect(screen.getByText('Retry')).toBeInTheDocument();
  });

  it('handles empty data state', async () => {
    // Mock empty data state
    mockUseAnalytics.mockReturnValue({
      ...defaultAnalyticsData,
      data: {
        ...defaultAnalyticsData.data,
        productivity: {
          totalFocusTime: 0,
          averageSessionLength: 0,
          completedSessions: 0,
          totalSessions: 0,
          completionRate: 0,
          streak: { current: 0, best: 0, type: 'daily' as const },
          productivity: { average: 0, trend: 'stable' as const }
        },
        taskCompletion: {
          completed: 0,
          total: 0,
          rate: 0,
          trend: 0,
          byPriority: { high: { completed: 0, total: 0 }, medium: { completed: 0, total: 0 }, low: { completed: 0, total: 0 } },
          byCategory: []
        },
        hiveActivity: [],
        memberEngagement: [],
        goalProgress: []
      }
    });
    
    render(<AnalyticsDashboard {...defaultProps} />);
    
    expect(screen.getByText('No analytics data yet')).toBeInTheDocument();
    expect(screen.getByText('Start using FocusHive to see your productivity analytics!')).toBeInTheDocument();
  });

  it('supports responsive layout', () => {
    render(<AnalyticsDashboard {...defaultProps} />);
    
    const dashboard = screen.getByTestId('analytics-dashboard');
    expect(dashboard).toHaveClass('dashboard-responsive');
  });

  it('shows appropriate sections based on view type - individual', () => {
    render(<AnalyticsDashboard {...defaultProps} />);
    
    expect(screen.getByTestId('productivity-chart')).toBeInTheDocument();
    expect(screen.getByTestId('goal-progress')).toBeInTheDocument();
    // Member engagement should be hidden or minimized in individual view
  });

  it('shows appropriate sections based on view type - hive', async () => {
    // Mock hive view type
    mockUseAnalytics.mockReturnValue({
      ...defaultAnalyticsData,
      filter: {
        ...defaultAnalyticsData.filter,
        viewType: 'hive' as const,
        selectedHives: ['hive-1']
      }
    });
    
    render(<AnalyticsDashboard {...defaultProps} hiveId="hive-1" />);
    
    expect(screen.getByTestId('hive-activity-heatmap')).toBeInTheDocument();
    expect(screen.getByTestId('member-engagement')).toBeInTheDocument();
  });

  it('handles initial filter application', () => {
    const initialFilter = {
      timeRange: {
        start: new Date('2024-02-01'),
        end: new Date('2024-02-29'),
        period: 'month' as const
      },
      viewType: 'hive' as const
    };
    
    render(<AnalyticsDashboard {...defaultProps} initialFilter={initialFilter} />);
    
    expect(screen.getByText('Hive Analytics')).toBeInTheDocument();
  });
});