import React, { createContext, useReducer, useCallback, useEffect } from 'react';
import { 
  AnalyticsContextValue, 
  AnalyticsDashboardData, 
  AnalyticsFilter, 
  ExportOptions,
  AnalyticsTimeRange,
  ChartDataPoint,
  ProductivityMetrics,
  TaskCompletionData,
  HiveActivityData,
  MemberEngagementData,
  GoalProgressData
} from '../types';
import { startOfWeek, endOfWeek, subDays, format } from 'date-fns';

interface AnalyticsState {
  data: AnalyticsDashboardData | null;
  filter: AnalyticsFilter;
  loading: boolean;
  error: string | null;
}

type AnalyticsAction =
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'SET_DATA'; payload: AnalyticsDashboardData }
  | { type: 'SET_FILTER'; payload: Partial<AnalyticsFilter> }
  | { type: 'RESET_FILTER' };

const initialFilter: AnalyticsFilter = {
  timeRange: {
    start: startOfWeek(new Date(), { weekStartsOn: 1 }),
    end: endOfWeek(new Date(), { weekStartsOn: 1 }),
    period: 'week'
  },
  viewType: 'individual',
  selectedHives: [],
  selectedMembers: [],
  metrics: ['focus-time', 'sessions', 'goals']
};

const initialState: AnalyticsState = {
  data: null,
  filter: initialFilter,
  loading: false,
  error: null
};

const analyticsReducer = (state: AnalyticsState, action: AnalyticsAction): AnalyticsState => {
  switch (action.type) {
    case 'SET_LOADING':
      return { ...state, loading: action.payload };
    case 'SET_ERROR':
      return { ...state, error: action.payload, loading: false };
    case 'SET_DATA':
      return { ...state, data: action.payload, loading: false, error: null };
    case 'SET_FILTER':
      return { 
        ...state, 
        filter: { ...state.filter, ...action.payload },
        error: null
      };
    case 'RESET_FILTER':
      return { ...state, filter: initialFilter };
    default:
      return state;
  }
};

// Mock data generation functions
const generateMockProductivityData = (timeRange: AnalyticsTimeRange): ProductivityMetrics => {
  const daysDiff = Math.ceil((timeRange.end.getTime() - timeRange.start.getTime()) / (1000 * 60 * 60 * 24));
  const completedSessions = Math.floor(daysDiff * 1.5 + Math.random() * 10);
  const totalSessions = Math.floor(completedSessions / 0.8);
  
  return {
    totalFocusTime: completedSessions * 25 + Math.floor(Math.random() * 100),
    averageSessionLength: 25 + Math.floor(Math.random() * 15),
    completedSessions,
    totalSessions,
    completionRate: completedSessions / totalSessions,
    streak: {
      current: Math.floor(Math.random() * 14) + 1,
      best: Math.floor(Math.random() * 30) + 5,
      type: 'daily'
    },
    productivity: {
      average: 3.5 + Math.random() * 1.5,
      trend: Math.random() > 0.5 ? 'increasing' : 'decreasing'
    }
  };
};

const generateMockTaskCompletionData = (): TaskCompletionData => {
  const completed = Math.floor(Math.random() * 30) + 10;
  const total = Math.floor(completed / 0.75) + Math.floor(Math.random() * 10);
  
  return {
    completed,
    total,
    rate: completed / total,
    trend: (Math.random() - 0.5) * 20,
    byPriority: {
      high: { completed: Math.floor(completed * 0.3), total: Math.floor(total * 0.3) },
      medium: { completed: Math.floor(completed * 0.5), total: Math.floor(total * 0.5) },
      low: { completed: Math.floor(completed * 0.2), total: Math.floor(total * 0.2) }
    },
    byCategory: [
      { category: 'Development', completed: Math.floor(completed * 0.4), total: Math.floor(total * 0.4), rate: 0.8 },
      { category: 'Design', completed: Math.floor(completed * 0.3), total: Math.floor(total * 0.3), rate: 0.75 },
      { category: 'Research', completed: Math.floor(completed * 0.2), total: Math.floor(total * 0.2), rate: 0.7 },
      { category: 'Planning', completed: Math.floor(completed * 0.1), total: Math.floor(total * 0.1), rate: 0.9 }
    ]
  };
};

const generateMockChartData = (timeRange: AnalyticsTimeRange): ChartDataPoint[] => {
  const data: ChartDataPoint[] = [];
  const daysDiff = Math.ceil((timeRange.end.getTime() - timeRange.start.getTime()) / (1000 * 60 * 60 * 24));
  
  for (let i = 0; i < Math.min(daysDiff, 30); i++) {
    const date = subDays(timeRange.end, daysDiff - i - 1);
    data.push({
      x: format(date, 'yyyy-MM-dd'),
      y: Math.floor(Math.random() * 180) + 30, // 30-210 minutes
      label: format(date, 'MMM d')
    });
  }
  
  return data;
};

const generateMockHiveActivity = (timeRange: AnalyticsTimeRange): HiveActivityData[] => {
  const data: HiveActivityData[] = [];
  const daysDiff = Math.ceil((timeRange.end.getTime() - timeRange.start.getTime()) / (1000 * 60 * 60 * 24));
  
  for (let i = 0; i < Math.min(daysDiff, 100); i++) {
    const date = subDays(timeRange.end, daysDiff - i - 1);
    const value = Math.floor(Math.random() * 5); // 0-4 activity level
    data.push({
      date: format(date, 'yyyy-MM-dd'),
      value,
      focusTime: value * 30 + Math.floor(Math.random() * 60),
      sessions: value * 2 + Math.floor(Math.random() * 3),
      members: value + Math.floor(Math.random() * 5)
    });
  }
  
  return data;
};

const generateMockMemberEngagement = (): MemberEngagementData[] => {
  const members = [
    { id: '1', name: 'Alice Johnson', avatar: undefined },
    { id: '2', name: 'Bob Smith', avatar: undefined },
    { id: '3', name: 'Carol Davis', avatar: undefined },
    { id: '4', name: 'David Wilson', avatar: undefined },
    { id: '5', name: 'Eve Brown', avatar: undefined }
  ];
  
  return members.map((member, index) => {
    const focusTime = Math.floor(Math.random() * 400) + 100;
    const sessions = Math.floor(focusTime / 30) + Math.floor(Math.random() * 5);
    
    return {
      user: member,
      focusTime,
      sessions,
      lastActive: subDays(new Date(), Math.floor(Math.random() * 7)),
      rank: index + 1,
      engagement: focusTime > 300 ? 'high' : focusTime > 150 ? 'medium' : 'low',
      contribution: (focusTime / 1500) * 100 // Assuming total of 1500 minutes
    };
  });
};

const generateMockGoalProgress = (): GoalProgressData[] => {
  const goals = [
    {
      id: '1',
      title: 'Daily Focus Goal',
      description: 'Complete 4 hours of focused work daily',
      target: 240,
      unit: 'minutes',
      category: 'focus' as const,
      priority: 'high' as const,
      deadline: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)
    },
    {
      id: '2',
      title: 'Weekly Sessions',
      description: 'Complete 20 focus sessions this week',
      target: 20,
      unit: 'sessions',
      category: 'productivity' as const,
      priority: 'medium' as const
    },
    {
      id: '3',
      title: 'Team Collaboration',
      description: 'Participate in hive sessions with teammates',
      target: 600,
      unit: 'minutes',
      category: 'collaboration' as const,
      priority: 'low' as const
    }
  ];
  
  return goals.map(goal => {
    const current = Math.floor(Math.random() * goal.target * 1.2);
    const progress = Math.min(current / goal.target, 1.2);
    
    return {
      ...goal,
      current,
      progress,
      milestones: [
        { value: goal.target * 0.25, label: '25%', achieved: current >= goal.target * 0.25 },
        { value: goal.target * 0.5, label: '50%', achieved: current >= goal.target * 0.5 },
        { value: goal.target * 0.75, label: '75%', achieved: current >= goal.target * 0.75 },
        { value: goal.target, label: 'Complete', achieved: current >= goal.target }
      ].map(milestone => ({
        ...milestone,
        achievedAt: milestone.achieved ? subDays(new Date(), Math.floor(Math.random() * 7)) : undefined
      }))
    };
  });
};

const generateMockData = (filter: AnalyticsFilter): AnalyticsDashboardData => {
  const productivity = generateMockProductivityData(filter.timeRange);
  
  return {
    productivity,
    taskCompletion: generateMockTaskCompletionData(),
    hiveActivity: generateMockHiveActivity(filter.timeRange),
    memberEngagement: generateMockMemberEngagement(),
    goalProgress: generateMockGoalProgress(),
    trends: {
      focusTime: generateMockChartData(filter.timeRange),
      productivity: generateMockChartData(filter.timeRange),
      sessions: generateMockChartData(filter.timeRange),
      goals: generateMockChartData(filter.timeRange)
    },
    lastUpdated: new Date()
  };
};

const AnalyticsContext = createContext<AnalyticsContextValue | undefined>(undefined);

export const AnalyticsProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [state, dispatch] = useReducer(analyticsReducer, initialState);

  const fetchData = useCallback(async (filter: AnalyticsFilter) => {
    dispatch({ type: 'SET_LOADING', payload: true });
    
    try {
      // Simulate API call delay
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      const mockData = generateMockData(filter);
      dispatch({ type: 'SET_DATA', payload: mockData });
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: 'Failed to fetch analytics data' });
    }
  }, []);

  const updateFilter = useCallback((filterUpdate: Partial<AnalyticsFilter>) => {
    dispatch({ type: 'SET_FILTER', payload: filterUpdate });
  }, []);

  const refreshData = useCallback(async () => {
    await fetchData(state.filter);
  }, [fetchData, state.filter]);

  const exportData = useCallback(async (options: ExportOptions) => {
    // Simulate export process
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    // In a real implementation, this would generate and download the file
    console.log('Exporting data with options:', options);
    
    // Create a mock download
    const data = JSON.stringify(state.data, null, 2);
    const blob = new Blob([data], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `analytics-${format(new Date(), 'yyyy-MM-dd')}.${options.format}`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }, [state.data]);

  const setTimeRange = useCallback((timeRange: AnalyticsTimeRange) => {
    updateFilter({ timeRange });
  }, [updateFilter]);

  const setViewType = useCallback((viewType: AnalyticsFilter['viewType']) => {
    updateFilter({ viewType });
  }, [updateFilter]);

  // Fetch initial data
  useEffect(() => {
    fetchData(state.filter);
  }, [fetchData, state.filter]);

  const contextValue: AnalyticsContextValue = {
    data: state.data,
    filter: state.filter,
    loading: state.loading,
    error: state.error,
    updateFilter,
    refreshData,
    exportData,
    setTimeRange,
    setViewType
  };

  return (
    <AnalyticsContext.Provider value={contextValue}>
      {children}
    </AnalyticsContext.Provider>
  );
};

// Custom hook to use Analytics context
export const useAnalytics = (): AnalyticsContextValue => {
  const context = React.useContext(AnalyticsContext);
  if (!context) {
    throw new Error('useAnalytics must be used within an AnalyticsProvider');
  }
  return context;
};