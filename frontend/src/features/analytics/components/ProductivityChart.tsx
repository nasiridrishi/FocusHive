import React from 'react';
import {
  Alert,
  Box,
  Card,
  CardContent,
  CardHeader,
  Chip,
  CircularProgress,
  Typography,
} from '@mui/material';
import {LineChart} from '@mui/x-charts/LineChart';
import {Timer, TrendingDown, TrendingUp} from '@mui/icons-material';
import {ChartDataPoint, ProductivityChartProps} from '../types';
import {format} from 'date-fns';

const formatTimeRange = (timeRange: ProductivityChartProps['timeRange']): string => {
  const {start, end, period} = timeRange;

  switch (period) {
    case 'day':
      return format(start, 'MMM d, yyyy');
    case 'week':
      return `${format(start, 'MMM d')} - ${format(end, 'MMM d, yyyy')}`;
    case 'month':
      return format(start, 'MMMM yyyy');
    case 'quarter':
      return `Q${Math.ceil((start.getMonth() + 1) / 3)} ${start.getFullYear()}`;
    case 'year':
      return format(start, 'yyyy');
    default:
      return `${format(start, 'MMM d')} - ${format(end, 'MMM d, yyyy')}`;
  }
};

const calculateStats = (data: ChartDataPoint[]): Record<string, unknown> => {
  if (data.length === 0) {
    return {
      total: 0,
      average: 0,
      bestDay: null,
      trend: null
    };
  }

  const total = data.reduce((sum, point) => sum + point.y, 0);
  const average = total / data.length;
  const bestDay = data.reduce((best, current) =>
      current.y > best.y ? current : best
  );

  // Calculate trend (simple linear regression slope)
  const n = data.length;
  if (n < 2) return {total, average, bestDay, trend: null};

  const xValues = data.map((_, index) => index);
  const yValues = data.map(point => point.y);

  const sumX = xValues.reduce((a, b) => a + b, 0);
  const sumY = yValues.reduce((a, b) => a + b, 0);
  const sumXY = xValues.reduce((sum, x, i) => sum + x * yValues[i], 0);
  const sumXX = xValues.reduce((sum, x) => sum + x * x, 0);

  const slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);

  return {
    total,
    average,
    bestDay,
    trend: slope
  };
};

export const ProductivityChart: React.FC<ProductivityChartProps> = ({
                                                                      data,
                                                                      config = {},
                                                                      timeRange,
                                                                      loading = false,
                                                                      error = null
                                                                    }) => {
  if (loading) {
    return (
        <Card>
          <CardContent>
            <Box display="flex" flexDirection="column" alignItems="center" py={4}>
              <CircularProgress size={40}/>
              <Typography variant="body2" color="text.secondary" sx={{mt: 2}}>
                Loading productivity data...
              </Typography>
            </Box>
          </CardContent>
        </Card>
    );
  }

  if (error) {
    return (
        <Card>
          <CardContent>
            <Alert severity="error">
              <Typography variant="h6">Error loading productivity data</Typography>
              <Typography variant="body2">{error}</Typography>
            </Alert>
          </CardContent>
        </Card>
    );
  }

  if (data.length === 0) {
    return (
        <Card>
          <CardContent>
            <Box textAlign="center" py={4}>
              <Timer sx={{fontSize: 48, color: 'text.secondary', mb: 2}}/>
              <Typography variant="h6" color="text.secondary">
                No productivity data available
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Start tracking your focus sessions to see productivity trends.
              </Typography>
            </Box>
          </CardContent>
        </Card>
    );
  }

  const stats = calculateStats(data);
  const chartTitle = `Productivity - ${formatTimeRange(timeRange)}`;

  // Prepare chart data
  const chartData = data.map(point => ({
    x: typeof point.x === 'string' && point.x.includes('-') ?
        new Date(point.x).getTime() :
        typeof point.x === 'number' ? point.x : new Date(point.x).getTime(),
    y: point.y,
    label: point.label || format(new Date(point.x), 'MMM d')
  }));

  const chartConfig = {
    height: config.height || 300,
    title: config.title || chartTitle,
    showLegend: config.showLegend !== false,
    showGrid: config.showGrid !== false,
    animated: config.animated !== false,
    responsive: config.responsive !== false,
    ...config
  };

  const xAxisData = chartData.map(point => point.label);
  const seriesData = chartData.map(point => point.y);

  return (
      <Card>
        <CardHeader
            title={
              <Box display="flex" alignItems="center" justifyContent="space-between">
                <Typography variant="h6">{chartConfig.title}</Typography>
                {stats.trend !== null && (
                    <Chip
                        icon={(stats.trend as number) > 0 ? <TrendingUp/> : <TrendingDown/>}
                        label={(stats.trend as number) > 0 ? 'Improving' : 'Declining'}
                        color={(stats.trend as number) > 0 ? 'success' : 'warning'}
                        size="small"
                    />
                )}
              </Box>
            }
        />
        <CardContent>
          {/* Summary Statistics */}
          <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: 'repeat(3, 1fr)',
                gap: 2,
                mb: 3
              }}
          >
            <Box textAlign="center">
              <Typography variant="h6" color="primary">
                {stats.total as number} min
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Total Focus Time
              </Typography>
            </Box>
            <Box textAlign="center">
              <Typography variant="h6" color="primary">
                {(stats.average as number).toFixed(1)} min
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Average Session
              </Typography>
            </Box>
            <Box textAlign="center">
              <Typography variant="h6" color="primary">
                {(stats.bestDay as {label: string})?.label || 'N/A'}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Best Day
              </Typography>
            </Box>
          </Box>

          {/* Chart */}
          <Box data-testid="chart-container" sx={{width: '100%', height: chartConfig.height}}>
            <LineChart
                data-testid="line-chart"
                xAxis={[{
                  scaleType: 'point',
                  data: xAxisData,
                  label: config.xAxisLabel || 'Time Period'
                }]}
                series={[{
                  data: seriesData,
                  label: 'Focus Time (minutes)',
                  color: '#1976d2',
                  curve: 'linear'
                }]}
                height={chartConfig.height}
                margin={{left: 60, right: 20, top: 20, bottom: 60}}
                grid={{horizontal: chartConfig.showGrid, vertical: chartConfig.showGrid}}
                slotProps={{
                  legend: {
                    hidden: !chartConfig.showLegend
                  }
                }}
            />
          </Box>
        </CardContent>
      </Card>
  );
};