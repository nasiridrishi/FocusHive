import React, { useMemo } from 'react';
import {
  Card,
  CardContent,
  CardHeader,
  Typography,
  Box,
  useTheme,
  alpha,
  Theme,
  Tooltip
} from '@mui/material';
import { CalendarMonth } from '@mui/icons-material';
import { HiveActivityHeatmapProps, HiveActivityData } from '../types';
import { format, startOfYear, endOfYear, eachDayOfInterval, getDay, getWeek, parseISO } from 'date-fns';

const DAYS_OF_WEEK = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

const getActivityLevel = (value: number): number => {
  if (value === 0) return 0;
  if (value <= 1) return 1;
  if (value <= 2) return 2;
  if (value <= 3) return 3;
  return 4;
};

const getActivityColor = (level: number, theme: Theme): string => {
  const baseColor = theme.palette.primary.main;
  switch (level) {
    case 0: return theme.palette.grey[100];
    case 1: return alpha(baseColor, 0.2);
    case 2: return alpha(baseColor, 0.4);
    case 3: return alpha(baseColor, 0.6);
    case 4: return alpha(baseColor, 0.8);
    default: return theme.palette.grey[100];
  }
};

export const HiveActivityHeatmap: React.FC<HiveActivityHeatmapProps> = ({
  data,
  year = new Date().getFullYear(),
  showTooltip = true,
  cellSize = 12,
  onDateClick
}) => {
  const theme = useTheme();

  // Create a map of date -> activity data for quick lookup
  const activityMap = useMemo(() => {
    const map = new Map<string, HiveActivityData>();
    data.forEach(item => {
      map.set(item.date, item);
    });
    return map;
  }, [data]);

  // Generate all days of the year
  const yearStart = startOfYear(new Date(year, 0, 1));
  const yearEnd = endOfYear(new Date(year, 11, 31));
  const allDays = eachDayOfInterval({ start: yearStart, end: yearEnd });

  // Group days by weeks
  const weeks = useMemo(() => {
    const weekMap = new Map<number, Date[]>();
    allDays.forEach(day => {
      const weekNumber = getWeek(day);
      if (!weekMap.has(weekNumber)) {
        weekMap.set(weekNumber, []);
      }
      weekMap.get(weekNumber)?.push(day);
    });
    return Array.from(weekMap.entries()).sort(([a], [b]) => a - b);
  }, [allDays]);

  // Calculate statistics
  const stats = useMemo(() => {
    const activeDays = data.filter(item => item.value > 0);
    const totalActivity = data.reduce((sum, item) => sum + item.value, 0);
    const mostActiveDay = data.reduce((max, current) =>
      current.value > max.value ? current : max,
      { date: '', value: 0, focusTime: 0, sessions: 0, members: 0 }
    );

    return {
      activeDays: activeDays.length,
      averageActivity: data.length > 0 ? Math.round(totalActivity / data.length * 10) / 10 : 0,
      mostActiveDay: mostActiveDay.value > 0 ? format(parseISO(mostActiveDay.date), 'MMM d') : 'None'
    };
  }, [data]);

  const renderCell = (day: Date): JSX.Element => {
    const dateString = format(day, 'yyyy-MM-dd');
    const activityData = activityMap.get(dateString);
    const level = activityData ? getActivityLevel(activityData.value) : 0;
    
    const cell = (
      <Box
        key={dateString}
        data-testid={`heatmap-cell-${dateString}`}
        sx={{
          width: cellSize,
          height: cellSize,
          backgroundColor: getActivityColor(level, theme),
          borderRadius: 1,
          cursor: onDateClick ? 'pointer' : 'default',
          border: '1px solid',
          borderColor: theme.palette.divider,
          transition: 'background-color 0.2s',
          '&:hover': {
            boxShadow: showTooltip ? 2 : undefined,
          },
        }}
        onClick={onDateClick && activityData ? () => onDateClick(dateString, activityData) : undefined}
        onKeyDown={onDateClick && activityData ? (e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            onDateClick(dateString, activityData);
          }
        } : undefined}
        aria-label={`Activity on ${dateString}: ${activityData?.value ?? 0}`}
        tabIndex={0}
        role="button"
      />
    );
    
    if (!showTooltip || !activityData) {
      return cell;
    }

    return (
      <Tooltip
        key={dateString}
        title={
          <Box>
            <Typography variant="body2" fontWeight="bold">
              {format(day, 'MMMM d, yyyy')}
            </Typography>
            <Typography variant="body2">
              {activityData.focusTime} minutes
            </Typography>
            <Typography variant="body2">
              {activityData.sessions} sessions
            </Typography>
            <Typography variant="body2">
              {activityData.members} members
            </Typography>
          </Box>
        }
      >
        {cell}
      </Tooltip>
    );
  };

  const renderMonthLabels = (): React.ReactNode => {
    const monthPositions: { month: string; position: number }[] = [];
    let currentMonth = -1;

    weeks.forEach(([weekNum, days]) => {
      const firstDay = days[0];
      if (firstDay) {
        const month = firstDay.getMonth();
        if (month !== currentMonth) {
          monthPositions.push({
            month: MONTHS[month],
            position: weekNum * (cellSize + 2)
          });
          currentMonth = month;
        }
      }
    });

    return (
      <Box display="flex" mb={1} sx={{ paddingLeft: `${cellSize + 10}px` }}>
        {monthPositions.map(({ month, position }) => (
          <Typography
            key={month}
            variant="caption"
            sx={{
              position: 'absolute',
              left: position,
              fontSize: '10px',
              color: 'text.secondary'
            }}
          >
            {month}
          </Typography>
        ))}
      </Box>
    );
  };

  const renderWeekdayLabels = (): React.ReactNode => {
    const visibleDays = [1, 3, 5]; // Mon, Wed, Fri
    
    return (
      <Box display="flex" flexDirection="column" mr={1}>
        {DAYS_OF_WEEK.map((day, index) => (
          <Box
            key={day}
            sx={{
              height: cellSize,
              display: 'flex',
              alignItems: 'center',
              mb: '2px'
            }}
          >
            {visibleDays.includes(index) && (
              <Typography
                variant="caption"
                sx={{
                  fontSize: '10px',
                  color: 'text.secondary',
                  width: cellSize + 8
                }}
              >
                {day}
              </Typography>
            )}
          </Box>
        ))}
      </Box>
    );
  };

  const renderLegend = (): React.ReactNode => {
    return (
      <Box display="flex" alignItems="center" justifyContent="center" mt={2} gap={1}>
        <Typography variant="caption" color="text.secondary">
          Less
        </Typography>
        {[0, 1, 2, 3, 4].map(level => (
          <Box
            key={level}
            data-testid={`legend-level-${level}`}
            sx={{
              width: 10,
              height: 10,
              backgroundColor: getActivityColor(level, theme),
              border: `1px solid ${theme.palette.grey[300]}`,
              borderRadius: 1
            }}
          />
        ))}
        <Typography variant="caption" color="text.secondary">
          More
        </Typography>
      </Box>
    );
  };

  return (
    <Card>
      <CardHeader
        title={
          <Box display="flex" alignItems="center" gap={1}>
            <CalendarMonth color="primary" />
            <Typography variant="h6">
              Hive Activity Heatmap - {year}
            </Typography>
          </Box>
        }
      />
      <CardContent>
        {/* Statistics */}
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
              {stats.activeDays}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Total Active Days
            </Typography>
          </Box>
          <Box textAlign="center">
            <Typography variant="h6" color="primary">
              {stats.averageActivity}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Average Activity
            </Typography>
          </Box>
          <Box textAlign="center">
            <Typography variant="h6" color="primary">
              {stats.mostActiveDay}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Most Active Day
            </Typography>
          </Box>
        </Box>

        {/* Month labels */}
        {renderMonthLabels()}

        {/* Heatmap grid */}
        <Box
          display="flex"
          sx={{
            overflowX: 'auto',
            '--cell-size': `${cellSize}px`
          }}
        >
          {renderWeekdayLabels()}

          <Box
            data-testid="heatmap-grid"
            display="flex"
            gap="2px"
            sx={{
              '--cell-size': `${cellSize}px`
            }}
          >
            {weeks.map(([weekNum, days]) => (
              <Box
                key={weekNum}
                data-testid={`week-column-${weekNum}`}
                display="flex"
                flexDirection="column"
                gap="2px"
              >
                {DAYS_OF_WEEK.map((_, dayIndex) => {
                  const day = days.find(d => getDay(d) === dayIndex);
                  return day ? renderCell(day) : (
                    <Box
                      key={`empty-${weekNum}-${dayIndex}`}
                      sx={{ width: cellSize, height: cellSize }}
                    />
                  );
                })}
              </Box>
            ))}
          </Box>
        </Box>

        {/* Legend */}
        {renderLegend()}
      </CardContent>
    </Card>
  );
};