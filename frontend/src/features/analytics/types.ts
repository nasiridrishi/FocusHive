export interface AnalyticsFilter {
  timeRange: {
    start: Date;
    end: Date;
    period: 'day' | 'week' | 'month' | 'quarter' | 'year' | 'custom';
  };
  viewType: 'individual' | 'team' | 'hive' | 'comparison';
  selectedHives: string[];
  selectedMembers: string[];
  metrics: string[];
}

export interface ExportOptions {
  format: 'csv' | 'json' | 'pdf' | 'png';
  dateRange: {
    start: Date;
    end: Date;
    period: 'day' | 'week' | 'month' | 'quarter' | 'year' | 'custom';
  };
  includeCharts: boolean;
  includeRawData: boolean;
  sections: ('productivity' | 'goals' | 'hive-activity' | 'member-engagement')[];
}

export interface ExportMenuProps {
  onExport: (options: ExportOptions) => Promise<void>;
  loading?: boolean;
  disabled?: boolean;
  currentFilter: AnalyticsFilter;
}

// Time Range Interface
export interface AnalyticsTimeRange {
  start: Date;
  end: Date;
  period: 'day' | 'week' | 'month' | 'quarter' | 'year' | 'custom';
}

// Chart Data Point Interface
export interface ChartDataPoint {
  x: string;
  y: number;
  label?: string;
}

// User Interface
export interface User {
  id: string;
  email: string;
  username: string;
  firstName: string;
  lastName: string;
  name: string;
  avatar?: string;
  isEmailVerified: boolean;
  createdAt: string;
  updatedAt: string;
}

// Productivity Metrics Interface
export interface ProductivityMetrics {
  totalFocusTime: number;
  averageSessionLength: number;
  completedSessions: number;
  totalSessions: number;
  completionRate: number;
  streak: {
    current: number;
    best: number;
    type: 'daily' | 'weekly';
  };
  productivity: {
    average: number;
    trend: 'increasing' | 'decreasing' | 'stable';
  };
}

// Task Completion Data Interface
export interface TaskCompletionData {
  completed: number;
  total: number;
  rate: number;
  trend: number;
  byPriority: {
    high: { completed: number; total: number };
    medium: { completed: number; total: number };
    low: { completed: number; total: number };
  };
  byCategory: {
    category: string;
    completed: number;
    total: number;
    rate: number;
  }[];
}

// Hive Activity Data Interface
export interface HiveActivityData {
  date: string;
  value: number;
  focusTime: number;
  sessions: number;
  members: number;
}

// Member Engagement Data Interface
export interface MemberEngagementData {
  user: User;
  focusTime: number;
  sessions: number;
  lastActive: Date;
  rank: number;
  engagement: 'high' | 'medium' | 'low';
  contribution: number;
}

// Goal Progress Data Interface
export interface GoalProgressData {
  id: string;
  title: string;
  description?: string;
  target: number;
  current: number;
  unit: string;
  category: 'focus' | 'productivity' | 'collaboration' | 'wellness' | string;
  priority: 'high' | 'medium' | 'low';
  deadline?: Date;
  progress: number;
  milestones: {
    value: number;
    label: string;
    achieved: boolean;
    achievedAt?: Date;
  }[];
}

// Analytics Dashboard Data Interface
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

// Analytics Context Value Interface
export interface AnalyticsContextValue {
  data: AnalyticsDashboardData | null;
  filter: AnalyticsFilter;
  loading: boolean;
  error: string | null;
  updateFilter: (filterUpdate: Partial<AnalyticsFilter>) => void;
  refreshData: () => Promise<void>;
  exportData: (options: ExportOptions) => Promise<void>;
  setTimeRange: (timeRange: AnalyticsTimeRange) => void;
  setViewType: (viewType: AnalyticsFilter['viewType']) => void;
}

// Hive Interface
export interface Hive {
  id: string;
  name: string;
  description?: string;
  memberCount?: number;
  isActive?: boolean;
}

// Member Interface  
export interface Member {
  id: string;
  name: string;
  displayName?: string;
  avatar?: string;
  isActive?: boolean;
}

// Component Props Interfaces
export interface AnalyticsDashboardProps {
  className?: string;
  userId?: string;
  hiveId?: string;
  initialFilter?: Partial<AnalyticsFilter>;
  compactMode?: boolean;
}

export interface AnalyticsFiltersProps {
  filter: AnalyticsFilter;
  onFilterChange: (filter: Partial<AnalyticsFilter>) => void;
  loading?: boolean;
  compact?: boolean;
  availableHives?: Hive[];
  availableMembers?: Member[];
}

export interface TaskCompletionRateProps {
  data: TaskCompletionData;
  loading?: boolean;
  showTrend?: boolean;
  showBreakdown?: boolean;
  variant?: string;
}

export interface ProductivityChartProps {
  data: ChartDataPoint[];
  loading?: boolean;
  title?: string;
  height?: number;
  timeRange?: AnalyticsTimeRange;
  config?: {
    title?: string;
    height?: number;
    showLegend?: boolean;
    animated?: boolean;
    type?: string;
    showGrid?: boolean;
    responsive?: boolean;
    xAxisLabel?: string;
  };
  error?: string;
}

export interface MemberEngagementProps {
  data: MemberEngagementData[];
  loading?: boolean;
  showRankings?: boolean;
  variant?: string;
  onMemberSelect?: (memberId: string) => void;
  selectedMember?: string;
  maxMembers?: number;
  sortBy?: string;
  showRank?: boolean;
  currentUserId?: string;
}

export interface HiveActivityHeatmapProps {
  data: HiveActivityData[];
  loading?: boolean;
  showTooltip?: boolean;
  variant?: string;
  timeRange?: AnalyticsTimeRange;
  year?: number;
  cellSize?: number;
  onDateClick?: (date: string, activityData: HiveActivityData) => void;
}

export interface GoalProgressProps {
  data?: GoalProgressData[];
  loading?: boolean;
  showMilestones?: boolean;
  variant?: string;
  onGoalSelect?: (goalId: string) => void;
  goals?: GoalProgressData[];
  layout?: string;
  onGoalClick?: (goal: GoalProgressData) => void;
}