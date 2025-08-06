import React, { useEffect } from 'react';
import {
  Box,
  Grid,
  Typography,
  Card,
  CardContent,
  IconButton,
  Chip,
  CircularProgress,
  Alert,
  Button,
  useTheme,
  useMediaQuery
} from '@mui/material';

// Type assertion for Grid props to handle MUI type issues
const GridItem = Grid as any;
import {
  Dashboard,
  Refresh,
  TrendingUp,
  Timer,
  EmojiEvents,
  Analytics
} from '@mui/icons-material';
import { ProductivityChart } from './ProductivityChart';
import { TaskCompletionRate } from './TaskCompletionRate';
import { HiveActivityHeatmap } from './HiveActivityHeatmap';
import { MemberEngagement } from './MemberEngagement';
import { GoalProgress } from './GoalProgress';
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

export const AnalyticsDashboard: React.FC<AnalyticsDashboardProps> = ({
  userId,
  hiveId,
  initialFilter,
  compactMode = false
}) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md' as any));
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
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <GridItem item xs={6} sm={3} key="total-focus-time" {...({} as any)}>
          <StatCard
            title="Total Focus Time"
            value={formatFocusTime(data.productivity.totalFocusTime)}
            icon={<Timer sx={{ fontSize: 32 }} />}
            color="primary"
            subtitle={`${data.productivity.completedSessions} sessions`}
          />
        </GridItem>
        <GridItem item xs={6} sm={3}>
          <StatCard
            title="Completed Sessions"
            value={data.productivity.completedSessions.toString()}
            icon={<TrendingUp sx={{ fontSize: 32 }} />}
            color="success"
            subtitle={`${Math.round(data.productivity.completionRate * 100)}% completion rate`}
          />
        </GridItem>
        <GridItem item xs={6} sm={3}>
          <StatCard
            title="Current Streak"
            value={`${data.productivity.streak.current} days`}
            icon={<EmojiEvents sx={{ fontSize: 32 }} />}
            color="warning"
            subtitle={`Best: ${data.productivity.streak.best} days`}
          />
        </GridItem>
        <GridItem item xs={6} sm={3}>
          <StatCard
            title="Productivity Score"
            value={`${data.productivity.productivity.average.toFixed(1)}/5`}
            icon={<Dashboard sx={{ fontSize: 32 }} />}
            color="info"
            subtitle={`Trend: ${data.productivity.productivity.trend}`}
          />
        </GridItem>
      </Grid>

      <Grid container spacing={3}>
        {/* Filters */}
        <GridItem item xs={12} lg={3}>
          <AnalyticsFilters
            filter={filter}
            onFilterChange={updateFilter}
            compact={isMobile || compactMode}
          />
        </GridItem>

        {/* Main Content */}
        <GridItem item xs={12} lg={9}>
          <Grid container spacing={3}>
            {/* Productivity Chart */}
            {filter.metrics.includes('focus-time') && (
              <GridItem item xs={12}>
                <ProductivityChart
                  data={data.trends.focusTime}
                  timeRange={filter.timeRange}
                  loading={loading}
                  error={error}
                />
              </GridItem>
            )}

            {/* Task Completion and Goal Progress */}
            <GridItem item xs={12} md={6}>
              <TaskCompletionRate
                data={data.taskCompletion}
                showTrend={true}
                showBreakdown={!compactMode}
                variant={compactMode ? 'widget' : 'card'}
              />
            </GridItem>
            <GridItem item xs={12} md={6}>
              <GoalProgress
                goals={data.goalProgress}
                layout={compactMode ? 'list' : 'grid'}
                showMilestones={!compactMode}
              />
            </GridItem>

            {/* Hive Activity Heatmap */}
            {(hiveId || filter.viewType === 'hive') && (
              <GridItem item xs={12}>
                <HiveActivityHeatmap
                  data={data.hiveActivity}
                  showTooltip={true}
                  cellSize={isMobile ? 8 : 12}
                />
              </GridItem>
            )}

            {/* Member Engagement */}
            {(hiveId || filter.viewType !== 'individual') && data.memberEngagement.length > 0 && (
              <GridItem item xs={12}>
                <MemberEngagement
                  data={data.memberEngagement}
                  maxMembers={compactMode ? 5 : undefined}
                  sortBy="focusTime"
                  showRank={true}
                  currentUserId={userId}
                />
              </GridItem>
            )}
          </Grid>
        </GridItem>
      </Grid>
    </Box>
  );
};