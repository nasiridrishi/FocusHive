/**
 * Analytics system types for FocusHive
 * These types define the structure for productivity metrics, charts, and dashboard data
 */

import {User} from '../../../shared/types/auth';

// Unused imports removed - these types are not used in this file

export interface AnalyticsTimeRange {
  start: Date;
  end: Date;
  period: 'day' | 'week' | 'month' | 'quarter' | 'year' | 'custom';
}

export interface ProductivityMetrics {
  totalFocusTime: number; // in minutes
  averageSessionLength: number; // in minutes
  completedSessions: number;
  totalSessions: number;
  completionRate: number; // 0-1
  streak: {
    current: number;
    best: number;
    type: 'daily' | 'weekly';
  };
  productivity: {
    average: number; // 1-5 rating
    trend: 'increasing' | 'decreasing' | 'stable';
  };
}

export interface TaskCompletionData {
  completed: number;
  total: number;
  rate: number; // 0-1
  trend: number; // percentage change from previous period
  byPriority: {
    high: { completed: number; total: number };
    medium: { completed: number; total: number };
    low: { completed: number; total: number };
  };
  byCategory: Array<{
    category: string;
    completed: number;
    total: number;
    rate: number;
  }>;
}

export interface HiveActivityData {
  date: string; // ISO date string
  value: number; // activity intensity 0-4
  focusTime: number; // minutes
  sessions: number;
  members: number; // active members on this day
}

export interface MemberEngagementData {
  user: User;
  focusTime: number; // total minutes in period
  sessions: number;
  lastActive: Date;
  rank: number;
  engagement: 'high' | 'medium' | 'low'; // based on activity
  contribution: number; // percentage of total hive activity
}

export interface GoalProgressData {
  id: string;
  title: string;
  description?: string;
  target: number;
  current: number;
  unit: string; // 'minutes', 'sessions', 'days', etc.
  progress: number; // 0-1
  deadline?: Date;
  category: 'focus' | 'productivity' | 'collaboration' | 'wellness';
  priority: 'low' | 'medium' | 'high';
  milestones: Array<{
    value: number;
    label: string;
    achieved: boolean;
    achievedAt?: Date;
  }>;
}

export interface ChartDataPoint {
  x: string | number | Date;
  y: number;
  label?: string;
  category?: string;
}

export interface ChartConfig {
  type: 'line' | 'bar' | 'area' | 'pie' | 'heatmap' | 'scatter';
  title?: string;
  xAxisLabel?: string;
  yAxisLabel?: string;
  showLegend?: boolean;
  showGrid?: boolean;
  animated?: boolean;
  height?: number;
  colors?: string[];
  responsive?: boolean;
}

export interface ExportOptions {
  format: 'csv' | 'json' | 'pdf' | 'png';
  dateRange: AnalyticsTimeRange;
  includeCharts: boolean;
  includeRawData: boolean;
  sections: Array<'productivity' | 'goals' | 'hive-activity' | 'member-engagement'>;
}

export interface AnalyticsFilter {
  timeRange: AnalyticsTimeRange;
  viewType: 'individual' | 'hive' | 'comparison';
  selectedHives?: string[];
  selectedMembers?: string[];
  metrics: Array<'focus-time' | 'sessions' | 'goals' | 'engagement' | 'productivity'>;
}

export interface AnalyticsDashboardData {
  productivity: ProductivityMetrics;
  taskCompletion: TaskCompletionData;
  hiveActivity: HiveActivityData[];
  memberEngagement: MemberEngagementData[];
  goalProgress: GoalProgressData[];
  trends: {
    focusTime: ChartDataPoint[];
    productivity: ChartDataPoint[];
    sessions: ChartDataPoint[];
    goals: ChartDataPoint[];
  };
  lastUpdated: Date;
}

export interface AnalyticsContextValue {
  data: AnalyticsDashboardData | null;
  filter: AnalyticsFilter;
  loading: boolean;
  error: string | null;

  // Actions
  updateFilter: (filter: Partial<AnalyticsFilter>) => void;
  refreshData: () => Promise<void>;
  exportData: (options: ExportOptions) => Promise<void>;
  setTimeRange: (range: AnalyticsTimeRange) => void;
  setViewType: (type: AnalyticsFilter['viewType']) => void;
}

// Component Props Interfaces

export interface TaskCompletionRateProps {
  data: TaskCompletionData;
  showTrend?: boolean;
  showBreakdown?: boolean;
  variant?: 'card' | 'widget' | 'detailed';
}

export interface HiveActivityHeatmapProps {
  data: HiveActivityData[];
  year?: number;
  showTooltip?: boolean;
  cellSize?: number;
  onDateClick?: (date: string, data: HiveActivityData) => void;
}

export interface MemberEngagementProps {
  data: MemberEngagementData[];
  maxMembers?: number;
  sortBy?: 'focusTime' | 'sessions' | 'engagement';
  showRank?: boolean;
  currentUserId?: string;
}

export interface GoalProgressProps {
  goals: GoalProgressData[];
  layout?: 'grid' | 'list';
  showMilestones?: boolean;
  onGoalClick?: (goal: GoalProgressData) => void;
}

export interface AnalyticsFiltersProps {
  filter: AnalyticsFilter;
  onFilterChange: (filter: Partial<AnalyticsFilter>) => void;
  availableHives?: Array<{ id: string; name: string }>;
  availableMembers?: Array<{ id: string; name: string }>;
  compact?: boolean;
}

export interface ExportMenuProps {
  onExport: (options: ExportOptions) => Promise<void>;
  loading?: boolean;
  disabled?: boolean;
  currentFilter: AnalyticsFilter;
}

export interface AnalyticsDashboardProps {
  userId?: string;
  hiveId?: string;
  initialFilter?: Partial<AnalyticsFilter>;
  compactMode?: boolean;
}