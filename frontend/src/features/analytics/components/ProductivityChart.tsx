import React, { useState, useEffect, useCallback, useRef } from 'react'
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  ButtonGroup,
  IconButton,
  Menu,
  MenuItem,
  Stack,
  Skeleton,
  Alert,
  useTheme,
  useMediaQuery,
  CircularProgress,
  Chip,
  Divider,
  Breakpoint,
} from '@mui/material'
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts/lib'
import {
  Download as DownloadIcon,
  Refresh as RefreshIcon,
  ShowChart as LineChartIcon,
  BarChart as BarChartIcon,
  KeyboardArrowDown as ArrowDownIcon,
} from '@mui/icons-material'

export interface ProductivityData {
  date?: string
  week?: string
  month?: string
  focusTime: number
  breakTime: number
  sessions: number
}

export interface ProductivityChartProps {
  data: ProductivityData[]
  view: 'daily' | 'weekly' | 'monthly'
  onViewChange: (view: 'daily' | 'weekly' | 'monthly') => void
  type?: 'line' | 'area' | 'bar'
  isLoading?: boolean
  error?: string | null
  onRetry?: () => void
  onExport?: (data: ProductivityData[]) => void
  colors?: {
    focusTime?: string
    breakTime?: string
  }
  theme?: 'light' | 'dark'
}

const ProductivityChart: React.FC<ProductivityChartProps> = ({
  data,
  view,
  onViewChange,
  type = 'line',
  isLoading = false,
  error = null,
  onRetry,
  onExport,
  colors = {
    focusTime: '#8884d8',
    breakTime: '#82ca9d',
  },
  theme = 'light',
}) => {
  const muiTheme = useTheme()
  const isMobile = useMediaQuery(muiTheme.breakpoints.down('sm' as Breakpoint))
  const [chartType, setChartType] = useState<'line' | 'area' | 'bar'>(type)
  const [chartMenuAnchor, setChartMenuAnchor] = useState<null | HTMLElement>(null)
  const [hasDataUpdate, setHasDataUpdate] = useState(false)
  const liveRegionRef = useRef<HTMLDivElement>(null)

  // Calculate statistics
  const totalFocusTime = data.reduce((sum, item) => sum + item.focusTime, 0)
  const totalSessions = data.reduce((sum, item) => sum + item.sessions, 0)
  const averageSessionTime = totalSessions > 0 ? Math.round(totalFocusTime / totalSessions) : 0

  // Handle data updates for accessibility
  useEffect(() => {
    if (data.length > 0 && liveRegionRef.current) {
      setHasDataUpdate(true)
      liveRegionRef.current.textContent = 'Data updated'
      setTimeout(() => setHasDataUpdate(false), 1000)
    }
  }, [data])

  const handleViewChange = useCallback(
    (newView: 'daily' | 'weekly' | 'monthly') => {
      if (newView !== view) {
        onViewChange(newView)
      }
    },
    [view, onViewChange]
  )

  const handleChartTypeClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setChartMenuAnchor(event.currentTarget)
  }

  const handleChartTypeClose = () => {
    setChartMenuAnchor(null)
  }

  const handleChartTypeSelect = (newType: 'line' | 'area' | 'bar') => {
    setChartType(newType)
    handleChartTypeClose()
  }

  const handleExport = () => {
    if (onExport) {
      onExport(data)
    }
  }

  // Loading state
  if (isLoading) {
    return (
      <Card data-testid="productivity-chart" className={`theme-${theme}`}>
        <CardContent>
          <Box role="progressbar" sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
          <Box data-testid="stats-skeleton" sx={{ mt: 2 }}>
            <Stack direction="row" spacing={2} justifyContent="space-around">
              <Skeleton variant="rectangular" width={100} height={60} />
              <Skeleton variant="rectangular" width={100} height={60} />
              <Skeleton variant="rectangular" width={100} height={60} />
            </Stack>
          </Box>
        </CardContent>
      </Card>
    )
  }

  // Error state
  if (error) {
    return (
      <Card data-testid="productivity-chart" className={`theme-${theme}`}>
        <CardContent>
          <Alert
            severity="error"
            action={
              onRetry && (
                <Button color="inherit" size="small" onClick={onRetry}>
                  Retry
                </Button>
              )
            }
          >
            {error}
          </Alert>
        </CardContent>
      </Card>
    )
  }

  // Empty state
  if (data.length === 0) {
    return (
      <Card data-testid="productivity-chart" className={`theme-${theme}`}>
        <CardContent>
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <Typography variant="h6" color="text.secondary" gutterBottom>
              No productivity data available
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Start a focus session to see your productivity
            </Typography>
          </Box>
        </CardContent>
      </Card>
    )
  }

  // Use matchMedia directly for responsive container height
  const chartHeight = isMobile ? 300 : 400
  const xDataKey = view === 'daily' ? 'date' : view === 'weekly' ? 'week' : 'month'

  const renderChart = () => {
    const commonProps = {
      data,
      margin: { top: 5, right: 30, left: 20, bottom: 5 },
    }

    const commonAxisProps = {
      stroke: muiTheme.palette.text.secondary,
    }

    switch (chartType) {
      case 'area':
        return (
          <AreaChart {...commonProps} data-testid="area-chart">
            <CartesianGrid strokeDasharray="3 3" data-testid="chart-grid" />
            <XAxis dataKey={xDataKey} {...commonAxisProps} data-testid="chart-x-axis" />
            <YAxis {...commonAxisProps} data-testid="chart-y-axis" />
            <Tooltip data-testid="chart-tooltip" />
            <Legend data-testid="chart-legend" />
            <Area
              type="monotone"
              dataKey="focusTime"
              stroke={colors.focusTime}
              fill={colors.focusTime}
              data-testid="chart-area"
            />
            <Area
              type="monotone"
              dataKey="breakTime"
              stroke={colors.breakTime}
              fill={colors.breakTime}
              data-testid="chart-area"
            />
          </AreaChart>
        )
      case 'bar':
        return (
          <BarChart {...commonProps} data-testid="bar-chart">
            <CartesianGrid strokeDasharray="3 3" data-testid="chart-grid" />
            <XAxis dataKey={xDataKey} {...commonAxisProps} data-testid="chart-x-axis" />
            <YAxis {...commonAxisProps} data-testid="chart-y-axis" />
            <Tooltip data-testid="chart-tooltip" />
            <Legend data-testid="chart-legend" />
            <Bar
              dataKey="focusTime"
              fill={colors.focusTime}
              data-testid="chart-bar"
            />
            <Bar
              dataKey="breakTime"
              fill={colors.breakTime}
              data-testid="chart-bar"
            />
          </BarChart>
        )
      default:
        return (
          <LineChart {...commonProps} data-testid="line-chart">
            <CartesianGrid strokeDasharray="3 3" data-testid="chart-grid" />
            <XAxis dataKey={xDataKey} {...commonAxisProps} data-testid="chart-x-axis" />
            <YAxis {...commonAxisProps} data-testid="chart-y-axis" />
            <Tooltip data-testid="chart-tooltip" />
            <Legend data-testid="chart-legend" />
            <Line
              type="monotone"
              dataKey="focusTime"
              stroke={colors.focusTime}
              data-testid="chart-line"
            />
            <Line
              type="monotone"
              dataKey="breakTime"
              stroke={colors.breakTime}
              data-testid="chart-line"
            />
          </LineChart>
        )
    }
  }

  return (
    <Card data-testid="productivity-chart" className={`theme-${theme}`}>
      <CardContent>
        {/* Header */}
        <Box sx={{ mb: 3 }}>
          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            justifyContent="space-between"
            alignItems={{ xs: 'stretch', sm: 'center' }}
            spacing={2}
          >
            <Typography variant="h5" component="h2">
              Productivity Overview
            </Typography>
            <Stack direction="row" spacing={1} alignItems="center">
              <Button
                size="small"
                startIcon={<ArrowDownIcon />}
                onClick={handleChartTypeClick}
                aria-label="Chart type"
              >
                Chart Type
              </Button>
              <Menu
                anchorEl={chartMenuAnchor}
                open={Boolean(chartMenuAnchor)}
                onClose={handleChartTypeClose}
              >
                <MenuItem onClick={() => handleChartTypeSelect('line')}>
                  Line Chart
                </MenuItem>
                <MenuItem onClick={() => handleChartTypeSelect('area')}>
                  Area Chart
                </MenuItem>
                <MenuItem onClick={() => handleChartTypeSelect('bar')}>
                  Bar Chart
                </MenuItem>
              </Menu>
              <Button
                size="small"
                startIcon={<DownloadIcon />}
                onClick={handleExport}
                aria-label="Export data"
              >
                Export Data
              </Button>
            </Stack>
          </Stack>
        </Box>

        {/* View Toggle */}
        <Box sx={{ mb: 3 }}>
          <ButtonGroup
            variant="outlined"
            size="small"
            fullWidth={isMobile}
            orientation={isMobile ? 'vertical' : 'horizontal'}
            role="group"
            aria-label="View toggle"
            sx={{ flexDirection: isMobile ? 'column' : 'row', display: 'flex' }}
            style={{ flexDirection: isMobile ? 'column' : 'row' }}
          >
            <Button
              onClick={() => handleViewChange('daily')}
              variant={view === 'daily' ? 'contained' : 'outlined'}
              aria-pressed={view === 'daily'}
            >
              Daily
            </Button>
            <Button
              onClick={() => handleViewChange('weekly')}
              variant={view === 'weekly' ? 'contained' : 'outlined'}
              aria-pressed={view === 'weekly'}
            >
              Weekly
            </Button>
            <Button
              onClick={() => handleViewChange('monthly')}
              variant={view === 'monthly' ? 'contained' : 'outlined'}
              aria-pressed={view === 'monthly'}
            >
              Monthly
            </Button>
          </ButtonGroup>
        </Box>

        {/* Chart */}
        <Box role="img" aria-label="Productivity chart" sx={{ mb: 3 }}>
          <ResponsiveContainer
            width="100%"
            height={chartHeight}
            data-testid="responsive-container"
            {...{ height: chartHeight }}
          >
            {renderChart()}
          </ResponsiveContainer>
        </Box>

        <Divider sx={{ my: 2 }} />

        {/* Statistics */}
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={2}
          justifyContent="space-around"
          alignItems="center"
        >
          <Box textAlign="center">
            <Typography variant="body2" color="text.secondary">
              Total Focus Time
            </Typography>
            <Typography
              variant="h6"
              data-testid="total-focus-time"
              sx={{ fontWeight: 'bold' }}
            >
              {totalFocusTime}
            </Typography>
          </Box>
          <Box textAlign="center">
            <Typography variant="body2" color="text.secondary">
              Average Session Time
            </Typography>
            <Typography
              variant="h6"
              data-testid="average-session-time"
              sx={{ fontWeight: 'bold' }}
            >
              {averageSessionTime}
            </Typography>
          </Box>
          <Box textAlign="center">
            <Typography variant="body2" color="text.secondary">
              Total Sessions
            </Typography>
            <Typography
              variant="h6"
              data-testid="total-sessions"
              sx={{ fontWeight: 'bold' }}
            >
              {totalSessions}
            </Typography>
          </Box>
        </Stack>

        {/* Accessibility live region */}
        <Box
          ref={liveRegionRef}
          role="status"
          aria-live="polite"
          aria-atomic="true"
          sx={{ position: 'absolute', left: '-10000px', width: '1px', height: '1px', overflow: 'hidden' }}
        />
      </CardContent>
    </Card>
  )
}

export default ProductivityChart
export { ProductivityChart }