import React from 'react';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Box,
  Card,
  CardContent,
  CardHeader,
  Chip,
  CircularProgress,
  LinearProgress,
  List,
  ListItem,
  ListItemText,
  Stack,
  Typography
} from '@mui/material';
import {
  Assignment,
  CheckCircle,
  ExpandMore,
  Flag,
  RadioButtonUnchecked,
  TrendingDown,
  TrendingUp
} from '@mui/icons-material';
import {TaskCompletionRateProps} from '../types';

const getCompletionColor = (rate: number): 'success' | 'warning' | 'error' => {
  if (rate >= 0.75) return 'success';
  if (rate >= 0.5) return 'warning';
  return 'error';
};

const getPriorityColor = (priority: 'high' | 'medium' | 'low'): string => {
  switch (priority) {
    case 'high':
      return '#f44336';
    case 'medium':
      return '#ff9800';
    case 'low':
      return '#4caf50';
    default:
      return '#9e9e9e';
  }
};

export const TaskCompletionRate: React.FC<TaskCompletionRateProps> = ({
                                                                        data,
                                                                        showTrend = true,
                                                                        showBreakdown = false,
                                                                        variant = 'card'
                                                                      }) => {
  const completionPercentage = (data.rate * 100).toFixed(1);
  const completionColor = getCompletionColor(data.rate);
  const trendIsPositive = data.trend > 0;

  if (variant === 'widget') {
    return (
        <Box data-testid="task-completion-widget" sx={{p: 2}}>
          <Box display="flex" alignItems="center" justifyContent="space-between" mb={1}>
            <Typography variant="h6">Task Completion</Typography>
            {showTrend && (
                <Chip
                    icon={trendIsPositive ? <TrendingUp/> : <TrendingDown/>}
                    label={`${trendIsPositive ? '+' : ''}${data.trend.toFixed(1)}%`}
                    color={trendIsPositive ? 'success' : 'error'}
                    size="small"
                    aria-label={trendIsPositive ? 'trending up' : 'trending down'}
                />
            )}
          </Box>
          <Box display="flex" alignItems="center" gap={2}>
            <CircularProgress
                variant="determinate"
                value={Math.min(data.rate * 100, 100)}
                size={60}
                thickness={4}
                color={completionColor}
                aria-valuenow={parseFloat(completionPercentage)}
            />
            <Box>
              <Typography variant="h5" color={`${completionColor}.main`}>
                {completionPercentage}%
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {data.completed} of {data.total} tasks completed
              </Typography>
            </Box>
          </Box>
        </Box>
    );
  }

  const renderBreakdown = (): React.ReactElement | null => {
    if (!showBreakdown && variant !== 'detailed') return null;

    return (
        <Box sx={{mt: 2}}>
          <Accordion>
            <AccordionSummary expandIcon={<ExpandMore/>}>
              <Typography variant="subtitle2">By Priority</Typography>
            </AccordionSummary>
            <AccordionDetails>
              <Stack spacing={2}>
                {Object.entries(data.byPriority).map(([priority, stats]) => {
                  const priorityRate = stats.total > 0 ? (stats.completed / stats.total) * 100 : 0;
                  return (
                      <Box key={priority}>
                        <Box display="flex" justifyContent="space-between" alignItems="center"
                             mb={1}>
                          <Box display="flex" alignItems="center" gap={1}>
                            <Flag sx={{
                              color: getPriorityColor(priority as 'high' | 'medium' | 'low'),
                              fontSize: 16
                            }}/>
                            <Typography variant="body2" sx={{textTransform: 'capitalize'}}>
                              {priority} Priority
                            </Typography>
                          </Box>
                          <Typography variant="body2" fontWeight="bold">
                            {stats.completed}/{stats.total}
                          </Typography>
                        </Box>
                        <LinearProgress
                            variant="determinate"
                            value={priorityRate}
                            color={getCompletionColor(priorityRate / 100)}
                            sx={{height: 6, borderRadius: 3}}
                        />
                      </Box>
                  );
                })}
              </Stack>
            </AccordionDetails>
          </Accordion>

          <Accordion>
            <AccordionSummary expandIcon={<ExpandMore/>}>
              <Typography variant="subtitle2">By Category</Typography>
            </AccordionSummary>
            <AccordionDetails>
              <Stack spacing={2}>
                {data.byCategory.map((category) => (
                    <Box key={category.category}>
                      <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                        <Typography variant="body2">{category.category}</Typography>
                        <Typography variant="body2" fontWeight="bold">
                          {(category.rate * 100).toFixed(1)}%
                        </Typography>
                      </Box>
                      <LinearProgress
                          variant="determinate"
                          value={category.rate * 100}
                          color={getCompletionColor(category.rate)}
                          sx={{height: 6, borderRadius: 3}}
                      />
                    </Box>
                ))}
              </Stack>
            </AccordionDetails>
          </Accordion>
        </Box>
    );
  };

  return (
      <Card role="region">
        <CardHeader
            title={
              <Box display="flex" alignItems="center" gap={1}>
                <Assignment color="primary"/>
                <Typography variant="h6">Task Completion Rate</Typography>
              </Box>
            }
            action={
                showTrend && (
                    <Chip
                        icon={trendIsPositive ? <TrendingUp/> : <TrendingDown/>}
                        label={`${trendIsPositive ? '+' : ''}${data.trend.toFixed(1)}%`}
                        color={trendIsPositive ? 'success' : 'error'}
                        size="small"
                        aria-label={trendIsPositive ? 'trending up' : 'trending down'}
                    />
                )
            }
        />
        <CardContent>
          <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: {xs: '1fr', sm: '1fr 1fr'},
                gap: 3,
                alignItems: 'center'
              }}
          >
            <Box display="flex" flexDirection="column" alignItems="center">
              <Box position="relative" display="inline-flex" mb={2}>
                <CircularProgress
                    variant="determinate"
                    value={Math.min(data.rate * 100, 100)}
                    size={120}
                    thickness={4}
                    color={completionColor}
                    aria-valuenow={parseFloat(completionPercentage)}
                />
                <Box
                    position="absolute"
                    top={0}
                    left={0}
                    bottom={0}
                    right={0}
                    display="flex"
                    alignItems="center"
                    justifyContent="center"
                >
                  <Typography variant="h4" component="div" color={`${completionColor}.main`}>
                    {completionPercentage}%
                  </Typography>
                </Box>
              </Box>
              <Typography variant="body1" color="text.secondary" textAlign="center">
                {data.completed} of {data.total} tasks completed
              </Typography>
            </Box>

            <Box>
              <Typography variant="h6" gutterBottom>
                Quick Stats
              </Typography>
              <List dense>
                <ListItem>
                  <CheckCircle color="success" sx={{mr: 1}}/>
                  <ListItemText
                      primary="Completed Tasks"
                      secondary={`${data.completed} tasks`}
                  />
                </ListItem>
                <ListItem>
                  <RadioButtonUnchecked color="action" sx={{mr: 1}}/>
                  <ListItemText
                      primary="Remaining Tasks"
                      secondary={`${data.total - data.completed} tasks`}
                  />
                </ListItem>
                {showTrend && (
                    <ListItem>
                      {trendIsPositive ? (
                          <TrendingUp color="success" sx={{mr: 1}}/>
                      ) : (
                          <TrendingDown color="error" sx={{mr: 1}}/>
                      )}
                      <ListItemText
                          primary="Trend"
                          secondary={`${trendIsPositive ? '+' : ''}${data.trend.toFixed(1)}% from last period`}
                      />
                    </ListItem>
                )}
              </List>
            </Box>
          </Box>

          {renderBreakdown()}
        </CardContent>
      </Card>
  );
};