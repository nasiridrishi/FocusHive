// Analytics-related types and interfaces

export type MetricType = 
  | 'FOCUS_TIME'
  | 'TASK_COMPLETION'
  | 'BREAK_TIME'
  | 'HIVE_PARTICIPATION'
  | 'STREAK'
  | 'PRODUCTIVITY_SCORE';

export type AggregationPeriod = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY' | 'CUSTOM';
export type ComparisonType = 'PREVIOUS_PERIOD' | 'SAME_PERIOD_LAST_YEAR' | 'AVERAGE';
export type ChartType = 'LINE' | 'BAR' | 'PIE' | 'DONUT' | 'AREA' | 'HEATMAP';

// Core Analytics Data
export interface FocusSession {
  id: number;
  userId: number;
  hiveId?: number;
  startTime: string;
  endTime?: string;
  duration: number; // in minutes
  type: 'POMODORO' | 'DEEP_WORK' | 'REGULAR' | 'BREAK';
  taskName?: string;
  completed: boolean;
  interruptions: number;
  productivityScore?: number;
}

export interface ProductivityMetric {
  metricType: MetricType;
  value: number;
  unit: string;
  timestamp: string;
  metadata?: Record<string, any>;
}

export interface DailyAnalytics {
  userId: number;
  date: string;
  totalFocusMinutes: number;
  totalBreakMinutes: number;
  sessionsCompleted: number;
  tasksCompleted: number;
  productivityScore: number; // 0-100
  peakProductivityHour: number; // 0-23
  interruptions: number;
  hivesVisited: number[];
  longestFocusSession: number; // in minutes
  averageSessionLength: number; // in minutes
  goals: {
    daily: number;
    achieved: number;
  };
}

export interface WeeklyAnalytics {
  userId: number;
  weekStartDate: string;
  weekEndDate: string;
  totalFocusMinutes: number;
  dailyAverage: number;
  productivityTrend: 'UP' | 'DOWN' | 'STABLE';
  percentageChange: number;
  bestDay: string;
  worstDay: string;
  consistencyScore: number; // 0-100
  dailyBreakdown: DailyAnalytics[];
  weeklyGoals: {
    target: number;
    achieved: number;
    percentage: number;
  };
}

export interface MonthlyAnalytics {
  userId: number;
  month: number;
  year: number;
  totalFocusMinutes: number;
  dailyAverage: number;
  weeklyAverage: number;
  productivityScore: number;
  topHives: {
    hiveId: number;
    hiveName: string;
    minutesSpent: number;
  }[];
  topTasks: {
    taskName: string;
    minutesSpent: number;
    completionRate: number;
  }[];
  monthlyGoals: {
    target: number;
    achieved: number;
    percentage: number;
  };
  insights: AnalyticsInsight[];
}

// Analytics Insights and Recommendations
export interface AnalyticsInsight {
  id: string;
  type: 'ACHIEVEMENT' | 'RECOMMENDATION' | 'WARNING' | 'PATTERN';
  title: string;
  description: string;
  importance: 'HIGH' | 'MEDIUM' | 'LOW';
  actionable: boolean;
  action?: {
    label: string;
    type: string;
    payload?: any;
  };
  createdAt: string;
}

// Comparative Analytics
export interface ComparativeAnalytics {
  userId: number;
  period: AggregationPeriod;
  currentPeriod: {
    startDate: string;
    endDate: string;
    metrics: ProductivityMetric[];
  };
  previousPeriod: {
    startDate: string;
    endDate: string;
    metrics: ProductivityMetric[];
  };
  comparison: {
    type: ComparisonType;
    percentageChange: number;
    trend: 'IMPROVING' | 'DECLINING' | 'STABLE';
  };
}

// Leaderboards and Rankings
export interface LeaderboardEntry {
  rank: number;
  userId: number;
  username: string;
  avatar?: string;
  score: number;
  metric: MetricType;
  change: number; // position change from previous period
  isCurrentUser: boolean;
}

export interface Leaderboard {
  id: string;
  type: 'GLOBAL' | 'HIVE' | 'FRIENDS';
  period: AggregationPeriod;
  metric: MetricType;
  entries: LeaderboardEntry[];
  lastUpdated: string;
  totalParticipants: number;
}

// Charts and Visualizations
export interface ChartData {
  labels: string[];
  datasets: {
    label: string;
    data: number[];
    backgroundColor?: string | string[];
    borderColor?: string;
    fill?: boolean;
  }[];
}

export interface ChartConfig {
  type: ChartType;
  title: string;
  data: ChartData;
  options?: {
    responsive?: boolean;
    maintainAspectRatio?: boolean;
    scales?: any;
    plugins?: any;
  };
}

// Heatmap Data
export interface HeatmapData {
  userId: number;
  startDate: string;
  endDate: string;
  data: {
    date: string;
    value: number;
    level: 0 | 1 | 2 | 3 | 4; // GitHub-style intensity levels
  }[];
  maxValue: number;
  totalValue: number;
}

// Goals and Targets
export interface ProductivityGoal {
  id: number;
  userId: number;
  type: 'DAILY' | 'WEEKLY' | 'MONTHLY';
  metric: MetricType;
  target: number;
  current: number;
  percentage: number;
  startDate: string;
  endDate: string;
  achieved: boolean;
  streak?: number;
}

// API Request/Response Types
export interface GetAnalyticsRequest {
  userId?: number;
  hiveId?: number;
  period: AggregationPeriod;
  startDate?: string;
  endDate?: string;
  metrics?: MetricType[];
  includeComparison?: boolean;
  includeInsights?: boolean;
}

export interface GetAnalyticsResponse {
  analytics: DailyAnalytics | WeeklyAnalytics | MonthlyAnalytics;
  comparison?: ComparativeAnalytics;
  insights?: AnalyticsInsight[];
  charts?: ChartConfig[];
}

export interface GetLeaderboardRequest {
  type: 'GLOBAL' | 'HIVE' | 'FRIENDS';
  hiveId?: number;
  metric: MetricType;
  period: AggregationPeriod;
  limit?: number;
  offset?: number;
}

export interface ExportAnalyticsRequest {
  userId: number;
  format: 'CSV' | 'JSON' | 'PDF';
  period: AggregationPeriod;
  startDate: string;
  endDate: string;
  includeCharts?: boolean;
}

export interface ExportAnalyticsResponse {
  downloadUrl: string;
  expiresAt: string;
  fileSize: number;
  format: string;
}

// Real-time Analytics Updates
export interface AnalyticsWebSocketEvent {
  type: 'METRIC_UPDATE' | 'GOAL_ACHIEVED' | 'LEADERBOARD_CHANGE' | 'INSIGHT_GENERATED';
  userId: number;
  data: any;
  timestamp: string;
}

// Analytics Preferences
export interface AnalyticsPreferences {
  userId: number;
  defaultView: 'DAILY' | 'WEEKLY' | 'MONTHLY';
  favoriteMetrics: MetricType[];
  emailReports: boolean;
  reportFrequency?: 'DAILY' | 'WEEKLY' | 'MONTHLY';
  shareWithHive: boolean;
  publicProfile: boolean;
  timezone: string;
}

// Productivity Patterns
export interface ProductivityPattern {
  id: string;
  userId: number;
  patternType: 'TIME_OF_DAY' | 'DAY_OF_WEEK' | 'SEASONAL' | 'TASK_TYPE';
  description: string;
  confidence: number; // 0-1
  data: {
    bestTimes?: string[];
    bestDays?: string[];
    optimalSessionLength?: number;
    recommendedBreakInterval?: number;
  };
  discoveredAt: string;
}

// Streak Tracking
export interface Streak {
  id: number;
  userId: number;
  type: 'DAILY_LOGIN' | 'FOCUS_GOAL' | 'TASK_COMPLETION' | 'HIVE_PARTICIPATION';
  currentStreak: number;
  longestStreak: number;
  startDate: string;
  lastActiveDate: string;
  milestones: {
    days: number;
    achieved: boolean;
    achievedAt?: string;
    reward?: string;
  }[];
}

// Burnout Detection
export interface BurnoutRisk {
  userId: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  indicators: {
    factor: string;
    value: number;
    threshold: number;
    status: 'NORMAL' | 'WARNING' | 'CRITICAL';
  }[];
  recommendations: string[];
  lastAssessment: string;
}

// Export all types for convenience
export type AnalyticsData = 
  | FocusSession
  | DailyAnalytics
  | WeeklyAnalytics
  | MonthlyAnalytics
  | ProductivityMetric
  | ComparativeAnalytics
  | Leaderboard
  | ProductivityGoal
  | ProductivityPattern
  | Streak
  | BurnoutRisk;