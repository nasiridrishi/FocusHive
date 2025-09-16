// Export analytics service
export { analyticsService } from './services/analyticsService';
export type { AnalyticsService } from './services/analyticsService';

// Export hooks
export {
  useFocusSession,
  useDailyAnalytics,
  useWeeklyAnalytics,
  useMonthlyAnalytics,
  useProductivityGoals,
  useInsights,
  useStreaks,
  useLeaderboard,
  useActivityHeatmap,
  useProductivityPatterns,
  useBurnoutRisk,
  useChartData,
  useExportAnalytics,
  useTodayStats,
  useWeeklyComparison,
  useAnalyticsUpdates,
} from './hooks/useAnalytics';

// Re-export types from contracts
export type {
  FocusSession,
  DailyAnalytics,
  WeeklyAnalytics,
  MonthlyAnalytics,
  ProductivityMetric,
  ProductivityGoal,
  Leaderboard,
  LeaderboardEntry,
  AnalyticsInsight,
  ChartConfig,
  ChartData,
  HeatmapData,
  ProductivityPattern,
  Streak,
  BurnoutRisk,
  ComparativeAnalytics,
  GetAnalyticsRequest,
  GetAnalyticsResponse,
  GetLeaderboardRequest,
  ExportAnalyticsRequest,
  ExportAnalyticsResponse,
  AnalyticsWebSocketEvent,
  AnalyticsPreferences,
  MetricType,
  AggregationPeriod,
  ComparisonType,
  ChartType,
  AnalyticsData,
} from '@/contracts/analytics';