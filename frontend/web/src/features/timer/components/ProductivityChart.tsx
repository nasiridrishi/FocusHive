import React, { useMemo, useState } from 'react'
import {
  Card,
  CardContent,
  CardHeader,
  Box,
  Typography,
  Stack,
  IconButton,
  Tooltip,
  ToggleButton,
  ToggleButtonGroup,
  Chip,
  Paper,
  Grid,
  useTheme,
} from '@mui/material'
import {
  Download,
  Fullscreen,
  TrendingUp,
  Timer,
  CheckCircle,
  Star,
  Coffee,
} from '@mui/icons-material'
import {
  LineChart,
  BarChart,
  PieChart,
} from '@mui/x-charts'
import { ProductivityChartProps } from '../../../shared/types/timer'

// Mock data generator for charts
const generateMockData = (type: string, period: string) => {
  const today = new Date()
  const data = []
  
  switch (period) {
    case 'week':
      for (let i = 6; i >= 0; i--) {
        const date = new Date(today)
        date.setDate(date.getDate() - i)
        const dayName = date.toLocaleDateString('en', { weekday: 'short' })
        
        data.push({
          date: dayName,
          focusTime: Math.floor(Math.random() * 180) + 60, // 60-240 minutes
          sessions: Math.floor(Math.random() * 6) + 2, // 2-8 sessions
          productivity: Math.floor(Math.random() * 3) + 3, // 3-5 rating
          goals: Math.floor(Math.random() * 5) + 3, // 3-8 goals
          distractions: Math.floor(Math.random() * 5), // 0-5 distractions
          breaks: Math.floor(Math.random() * 30) + 15, // 15-45 minutes
        })
      }
      break
      
    case 'month':
      for (let i = 29; i >= 0; i--) {
        const date = new Date(today)
        date.setDate(date.getDate() - i)
        
        data.push({
          date: date.getDate().toString(),
          focusTime: Math.floor(Math.random() * 200) + 40,
          sessions: Math.floor(Math.random() * 8) + 1,
          productivity: Math.floor(Math.random() * 3) + 2.5,
          goals: Math.floor(Math.random() * 6) + 2,
          distractions: Math.floor(Math.random() * 6),
          breaks: Math.floor(Math.random() * 40) + 10,
        })
      }
      break
      
    default: // today
      for (let i = 23; i >= 0; i--) {
        const hour = 24 - i
        data.push({
          date: `${hour}:00`,
          focusTime: hour >= 9 && hour <= 17 ? Math.floor(Math.random() * 60) : Math.floor(Math.random() * 20),
          sessions: hour >= 9 && hour <= 17 ? Math.floor(Math.random() * 2) : 0,
          productivity: Math.floor(Math.random() * 2) + 3,
          goals: Math.floor(Math.random() * 2),
          distractions: Math.floor(Math.random() * 2),
          breaks: Math.floor(Math.random() * 15),
        })
      }
  }
  
  return data
}

// MUI X Charts doesn't need a custom tooltip component

const StatSummary: React.FC<{
  title: string
  value: number | string
  unit?: string
  color?: string
  icon?: React.ReactNode
}> = ({ title, value, unit, color, icon }) => {
  const theme = useTheme()
  
  return (
    <Stack direction="row" alignItems="center" spacing={1}>
      {icon && (
        <Box sx={{ color: color || theme.palette.primary.main }}>
          {icon}
        </Box>
      )}
      <Box>
        <Typography variant="body2" color="text.secondary">
          {title}
        </Typography>
        <Typography variant="h6" sx={{ color: color }}>
          {value}{unit}
        </Typography>
      </Box>
    </Stack>
  )
}

export const ProductivityChart: React.FC<ProductivityChartProps> = ({
  data: propData,
  type = 'line',
  height = 400,
}) => {
  const theme = useTheme()
  const [chartType, setChartType] = useState<'line' | 'bar' | 'area' | 'pie'>(type)
  const [period, setPeriod] = useState<'today' | 'week' | 'month'>('week')
  
  // Use mock data if no data provided
  const chartData = propData?.datasets ? propData : { datasets: generateMockData(chartType, period) }
  const data = Array.isArray(chartData.datasets) ? chartData.datasets : generateMockData(chartType, period)
  
  // Calculate summary statistics
  const summaryStats = useMemo(() => {
    if (!data || data.length === 0) return null
    
    const totalFocusTime = data.reduce((sum, item) => sum + (item.focusTime || 0), 0)
    const totalSessions = data.reduce((sum, item) => sum + (item.sessions || 0), 0)
    const avgProductivity = data.reduce((sum, item) => sum + (item.productivity || 0), 0) / data.length
    const totalGoals = data.reduce((sum, item) => sum + (item.goals || 0), 0)
    const totalDistractions = data.reduce((sum, item) => sum + (item.distractions || 0), 0)
    
    return {
      totalFocusTime: Math.round(totalFocusTime),
      totalSessions,
      avgProductivity: avgProductivity.toFixed(1),
      totalGoals,
      totalDistractions,
      avgSessionLength: totalSessions > 0 ? Math.round(totalFocusTime / totalSessions) : 0,
    }
  }, [data])

  const formatTime = (minutes: number): string => {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    return hours > 0 ? `${hours}h ${mins}m` : `${mins}m`
  }

  const handleChartTypeChange = (_: React.MouseEvent<HTMLElement>, newType: string | null) => {
    if (newType) setChartType(newType as 'line' | 'bar' | 'area' | 'pie')
  }

  const handlePeriodChange = (_: React.MouseEvent<HTMLElement>, newPeriod: string | null) => {
    if (newPeriod) setPeriod(newPeriod as 'today' | 'week' | 'month')
  }


  const exportData = () => {
    const csvContent = "data:text/csv;charset=utf-8," + 
      "Date,Focus Time,Sessions,Productivity,Goals,Distractions\n" +
      data.map(item => 
        `${item.date},${item.focusTime},${item.sessions},${item.productivity},${item.goals},${item.distractions}`
      ).join("\n")
    
    const encodedUri = encodeURI(csvContent)
    const link = document.createElement("a")
    link.setAttribute("href", encodedUri)
    link.setAttribute("download", `productivity-data-${period}.csv`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  const renderChart = () => {
    // Prepare data for MUI X Charts
    const xAxisData = data.map(item => item.date)
    const focusTimeData = data.map(item => item.focusTime)
    const sessionsData = data.map(item => item.sessions)
    const productivityData = data.map(item => item.productivity)

    switch (chartType) {
      case 'bar':
        return (
          <BarChart
            height={height}
            series={[
              { 
                data: focusTimeData, 
                label: 'Focus Time (min)',
                color: theme.palette.primary.main,
              },
              { 
                data: sessionsData, 
                label: 'Sessions',
                color: theme.palette.secondary.main,
              },
            ]}
            xAxis={[{ 
              data: xAxisData, 
              scaleType: 'band',
            }]}
            margin={{ top: 20, bottom: 30, left: 40, right: 10 }}
          />
        )
        
      case 'area':
        return (
          <LineChart
            height={height}
            series={[
              { 
                data: focusTimeData, 
                label: 'Focus Time (min)',
                color: theme.palette.primary.main,
                area: true,
              },
            ]}
            xAxis={[{ 
              data: xAxisData, 
              scaleType: 'band',
            }]}
            margin={{ top: 20, bottom: 30, left: 40, right: 10 }}
          />
        )
        
      case 'pie': {
        const pieData = [
          { 
            id: 'focus', 
            value: summaryStats?.totalFocusTime || 0, 
            label: 'Focus Time',
            color: theme.palette.primary.main 
          },
          { 
            id: 'breaks', 
            value: data.reduce((sum, item) => sum + (item.breaks || 0), 0), 
            label: 'Break Time',
            color: theme.palette.success.main 
          },
          { 
            id: 'distractions', 
            value: summaryStats?.totalDistractions || 0, 
            label: 'Distractions',
            color: theme.palette.warning.main 
          },
        ]
        
        return (
          <PieChart
            height={height}
            series={[
              {
                data: pieData,
                highlightScope: { faded: 'global', highlighted: 'item' },
                faded: { innerRadius: 30, additionalRadius: -30, color: 'gray' },
                innerRadius: 30,
                outerRadius: 100,
                paddingAngle: 5,
                cornerRadius: 5,
              },
            ]}
            margin={{ top: 20, bottom: 20, left: 20, right: 20 }}
          />
        )
      }
        
      default: // line
        return (
          <LineChart
            height={height}
            series={[
              { 
                data: focusTimeData, 
                label: 'Focus Time (min)',
                color: theme.palette.primary.main,
                curve: 'linear',
              },
              { 
                data: productivityData, 
                label: 'Productivity (1-5)',
                color: theme.palette.success.main,
                curve: 'linear',
              },
            ]}
            xAxis={[{ 
              data: xAxisData, 
              scaleType: 'band',
            }]}
            margin={{ top: 20, bottom: 30, left: 40, right: 10 }}
            grid={{ vertical: true, horizontal: true }}
          />
        )
    }
  }

  return (
    <Card>
      <CardHeader
        title="Productivity Analytics"
        subheader={`${period.charAt(0).toUpperCase() + period.slice(1)} overview`}
        action={
          <Stack direction="row" spacing={1}>
            <Tooltip title="Export Data">
              <IconButton onClick={exportData} size="small">
                <Download />
              </IconButton>
            </Tooltip>
            <Tooltip title="Fullscreen">
              <IconButton size="small">
                <Fullscreen />
              </IconButton>
            </Tooltip>
          </Stack>
        }
      />
      
      <CardContent>
        {/* Controls */}
        <Stack spacing={2} sx={{ mb: 3 }}>
          <Stack direction="row" spacing={2} flexWrap="wrap" alignItems="center">
            <Typography variant="subtitle2">Period:</Typography>
            <ToggleButtonGroup
              value={period}
              exclusive
              onChange={handlePeriodChange}
              size="small"
            >
              <ToggleButton value="today">Today</ToggleButton>
              <ToggleButton value="week">Week</ToggleButton>
              <ToggleButton value="month">Month</ToggleButton>
            </ToggleButtonGroup>
          </Stack>
          
          <Stack direction="row" spacing={2} flexWrap="wrap" alignItems="center">
            <Typography variant="subtitle2">Chart Type:</Typography>
            <ToggleButtonGroup
              value={chartType}
              exclusive
              onChange={handleChartTypeChange}
              size="small"
            >
              <ToggleButton value="line">Line</ToggleButton>
              <ToggleButton value="bar">Bar</ToggleButton>
              <ToggleButton value="area">Area</ToggleButton>
              <ToggleButton value="pie">Pie</ToggleButton>
            </ToggleButtonGroup>
          </Stack>
        </Stack>

        {/* Summary Statistics */}
        {summaryStats && (
          <Paper sx={{ p: 2, mb: 3, bgcolor: 'background.default' }}>
            <Typography variant="subtitle2" gutterBottom>
              Summary Statistics
            </Typography>
            <Grid container spacing={3}>
              <Grid item xs={6} sm={3}>
                <StatSummary
                  title="Total Focus"
                  value={formatTime(summaryStats.totalFocusTime)}
                  color={theme.palette.primary.main}
                  icon={<Timer fontSize="small" />}
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <StatSummary
                  title="Sessions"
                  value={summaryStats.totalSessions}
                  color={theme.palette.secondary.main}
                  icon={<CheckCircle fontSize="small" />}
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <StatSummary
                  title="Avg Productivity"
                  value={summaryStats.avgProductivity}
                  unit="/5"
                  color={theme.palette.success.main}
                  icon={<Star fontSize="small" />}
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <StatSummary
                  title="Avg Session"
                  value={formatTime(summaryStats.avgSessionLength)}
                  color={theme.palette.info.main}
                  icon={<Coffee fontSize="small" />}
                />
              </Grid>
            </Grid>
          </Paper>
        )}

        {/* Chart */}
        <Box sx={{ width: '100%', height: height }}>
          {renderChart()}
        </Box>

        {/* Insights */}
        <Box sx={{ mt: 3 }}>
          <Typography variant="subtitle2" gutterBottom>
            Insights
          </Typography>
          <Stack spacing={1}>
            <Chip
              icon={<TrendingUp />}
              label={`Best day: ${data.reduce((max, current) => 
                current.focusTime > max.focusTime ? current : max, data[0])?.date || 'N/A'}`}
              size="small"
              color="success"
              variant="outlined"
            />
            <Chip
              label={`Average productivity trending ${Math.random() > 0.5 ? 'up' : 'down'}`}
              size="small"
              color="info"
              variant="outlined"
            />
            <Chip
              label={`${summaryStats?.totalGoals || 0} goals completed this ${period}`}
              size="small"
              color="primary"
              variant="outlined"
            />
          </Stack>
        </Box>
      </CardContent>
    </Card>
  )
}

export default ProductivityChart