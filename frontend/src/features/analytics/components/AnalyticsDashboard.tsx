import React, { useEffect } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  IconButton,
  Chip,
  CircularProgress,
  Alert,
  Button,
  useMediaQuery
} from '@mui/material';
import {
  Dashboard,
  Refresh,
  TrendingUp,
  Timer,
  EmojiEvents,
  Analytics
} from '@mui/icons-material';
// Use lazy-loaded chart components for better bundle splitting
import { 
  LazyProductivityChart as ProductivityChart,
  LazyTaskCompletionRate as TaskCompletionRate,
  LazyHiveActivityHeatmap as HiveActivityHeatmap,
  LazyMemberEngagement as MemberEngagement,
  LazyGoalProgress as GoalProgress
} from '@shared/components/lazy-features';
import { AnalyticsFilters } from './AnalyticsFilters';
import { ExportMenu } from './ExportMenu';
import { useAnalytics } from '../contexts/AnalyticsContext';
import { AnalyticsDashboardProps } from '../types';
import { format } from 'date-fns';

const StatCard: React.FC<{
  title: string;
  value: string;
  icon: React.ReactNode;
  color?: string;
  subtitle?: string;
}> = ({ title, value, icon, color = 'primary', subtitle }) => (
  <Card sx={{ height: '100%' }}>
    <CardContent>
      <Box display="flex" alignItems="center" justifyContent="space-between">
        <Box>
          <Typography variant="h4" color={`${color}.main`} fontWeight="bold">
            {value}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {title}
          </Typography>
          {subtitle && (
            <Typography variant="caption" color="text.secondary">
              {subtitle}
            </Typography>
          )}
        </Box>
        <Box color={`${color}.main`}>
          {icon}
        </Box>
      </Box>
    </CardContent>
  </Card>
);

const AnalyticsDashboard: React.FC<AnalyticsDashboardProps> = ({
  userId,
  hiveId,
  initialFilter,
  compactMode = false
}) => {
  const isMobile = useMediaQuery('(max-width:960px)');
  const {
    data,
    filter,
    loading,
    error,
    updateFilter,
    refreshData,
    exportData,
    setViewType
  } = useAnalytics();

  // Apply initial filter if provided
  useEffect(() => {
    if (initialFilter) {
      updateFilter(initialFilter);
    }
  }, [initialFilter, updateFilter]);

  // Set view type based on context
  useEffect(() => {
    if (hiveId && filter.viewType === 'individual') {
      setViewType('hive');
    }
  }, [hiveId, filter.viewType, setViewType]);

  const handleRefresh = () => {
    refreshData();
  };

  const handleRetry = () => {
    refreshData();
  };

  const formatFocusTime = (minutes: number): string => {
    const hours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60;
    return `${hours}h ${remainingMinutes}m`;
  };

  if (loading && !data) {
    return (
      <Box
        data-testid="analytics-dashboard"
        className={compactMode ? 'dashboard-compact' : 'dashboard-responsive'}
        display="flex"
        flexDirection="column"
        alignItems="center"
        justifyContent="center"
        minHeight="400px"
      >
        <CircularProgress size={48} />
        <Typography variant="h6" sx={{ mt: 2 }}>
          Loading analytics data...
        </Typography>
      </Box>
    );
  }

  if (error && !data) {
    return (
      <Box
        data-testid="analytics-dashboard"
        className={compactMode ? 'dashboard-compact' : 'dashboard-responsive'}
      >
        <Alert 
          severity="error" 
          action={
            <Button color="inherit" size="small" onClick={handleRetry}>
              Retry
            </Button>
          }
        >
          <Typography variant="h6">Error loading analytics data</Typography>
          <Typography variant="body2">{error}</Typography>
        </Alert>
      </Box>
    );
  }

  if (!data || (
    data.productivity.totalFocusTime === 0 &&
    data.taskCompletion.total === 0 &&
    data.hiveActivity.length === 0 &&
    data.memberEngagement.length === 0 &&
    data.goalProgress.length === 0
  )) {
    return (
      <Box
        data-testid="analytics-dashboard"
        className={compactMode ? 'dashboard-compact' : 'dashboard-responsive'}
        textAlign="center"
        py={8}
      >
        <Analytics sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
        <Typography variant="h5" color="text.secondary" gutterBottom>
          No analytics data yet
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Start using FocusHive to see your productivity analytics!
        </Typography>
      </Box>
    );
  }

  const dashboardTitle = hiveId ? 'Hive Analytics' : 'Analytics Dashboard';
  const currentPeriodLabel = format(filter.timeRange.start, 'MMM d') + 
    (filter.timeRange.period !== 'day' ? ` - ${format(filter.timeRange.end, 'MMM d, yyyy')}` : ', yyyy');

  return (
    <Box
      data-testid="analytics-dashboard"
      className={compactMode ? 'dashboard-compact dashboard-responsive' : 'dashboard-responsive'}
    >
      {/* Header */}
      <Box display="flex" alignItems="center" justifyContent="space-between" mb={3}>
        <Box>
          <Typography variant="h4" fontWeight="bold" gutterBottom>
            {dashboardTitle}
          </Typography>
          <Box display="flex" alignItems="center" gap={1} flexWrap="wrap">
            <Chip
              label={`Time Period: ${currentPeriodLabel}`}
              variant="outlined"
              size="small"
            />
            <Chip
              label={`View: ${filter.viewType}`}
              variant="outlined"
              size="small"
              sx={{ textTransform: 'capitalize' }}
            />
            {data.lastUpdated && (
              <Typography variant="caption" color="text.secondary">
                Last updated: {format(data.lastUpdated, 'MMM d, yyyy h:mm a')}
              </Typography>
            )}
          </Box>
        </Box>
        
        <Box display="flex" alignItems="center" gap={1}>
          <IconButton 
            onClick={handleRefresh} 
            disabled={loading}
            aria-label="refresh data"
          >
            <Refresh />
          </IconButton>
          <ExportMenu 
            onExport={exportData}
            loading={loading}
            currentFilter={filter}
          />
        </Box>
      </Box>

      {/* Summary Statistics */}
      <Box sx={{ 
        display: 'grid',
        gridTemplateColumns: { xs: 'repeat(2, 1fr)', sm: 'repeat(4, 1fr)' },
        gap: 2,
        mb: 3 
      }}>
        <StatCard
          title="Total Focus Time"
          value={formatFocusTime(data.productivity.totalFocusTime)}
          icon={<Timer sx={{ fontSize: 32 }} />}
          color="primary"
          subtitle={`${data.productivity.completedSessions} sessions`}
        />
        <StatCard
          title="Completed Sessions"
          value={data.productivity.completedSessions.toString()}
          icon={<TrendingUp sx={{ fontSize: 32 }} />}
          color="success"
          subtitle={`${Math.round(data.productivity.completionRate * 100)}% completion rate`}
        />
        <StatCard
          title="Current Streak"
          value={`${data.productivity.streak.current} days`}
          icon={<EmojiEvents sx={{ fontSize: 32 }} />}
          color="warning"
          subtitle={`Best: ${data.productivity.streak.best} days`}
        />
        <StatCard
          title="Productivity Score"
          value={`${data.productivity.productivity.average.toFixed(1)}/5`}
          icon={<Dashboard sx={{ fontSize: 32 }} />}
          color="info"
          subtitle={`Trend: ${data.productivity.productivity.trend}`}
        />
      </Box>

      <Box sx={{ 
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', lg: '1fr 2fr' },
        gap: 3
      }}>
        {/* Filters */}
        <Box>
          <AnalyticsFilters
            filter={filter}
            onFilterChange={updateFilter}
            compact={isMobile || compactMode}
          />
        </Box>

        {/* Main Content */}
        <Box>
          <Box sx={{
            display: 'flex',
            flexDirection: 'column',
            gap: 3
          }}>
            {/* Productivity Chart */}
            {filter.metrics.includes('focus-time') && (
              <ProductivityChart
                data={data.trends.focusTime}
                timeRange={filter.timeRange}
                loading={loading}
                error={error}
              />
            )}

            {/* Task Completion and Goal Progress */}
            <Box sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
              gap: 3
            }}>
              <TaskCompletionRate
                data={data.taskCompletion}
                showTrend={true}
                showBreakdown={!compactMode}
                variant={compactMode ? 'widget' : 'card'}
              />
              <GoalProgress
                goals={data.goalProgress}
                layout={compactMode ? 'list' : 'grid'}
                showMilestones={!compactMode}
              />
            </Box>

            {/* Hive Activity Heatmap */}
            {(hiveId || filter.viewType === 'hive') && (
              <HiveActivityHeatmap
                data={data.hiveActivity}
                showTooltip={true}
                cellSize={isMobile ? 8 : 12}
              />
            )}

            {/* Member Engagement */}
            {(hiveId || filter.viewType !== 'individual') && data.memberEngagement.length > 0 && (
              <MemberEngagement
                data={data.memberEngagement}
                maxMembers={compactMode ? 5 : undefined}
                sortBy="focusTime"
                showRank={true}
                currentUserId={userId}
              />
            )}
          </Box>
        </Box>
      </Box>
    </Box>
  );
};

export default AnalyticsDashboard;