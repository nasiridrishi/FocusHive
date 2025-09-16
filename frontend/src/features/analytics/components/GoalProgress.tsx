import React, {useState} from 'react';
import {
  Box,
  Card,
  CardContent,
  CardHeader,
  Chip,
  Collapse,
  FormControl,
  IconButton,
  InputLabel,
  LinearProgress,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  MenuItem,
  Select,
  Typography
} from '@mui/material';
import {
  CheckCircle,
  EmojiEvents,
  ExpandLess,
  ExpandMore,
  Flag,
  RadioButtonUnchecked,
  Schedule
} from '@mui/icons-material';
import {GoalProgressData, GoalProgressProps} from '../types';
import {format, formatDistanceToNow, isPast} from 'date-fns';

const getPriorityColor = (priority: GoalProgressData['priority']): 'error' | 'warning' | 'success' | 'default' => {
  switch (priority) {
    case 'high':
      return 'error';
    case 'medium':
      return 'warning';
    case 'low':
      return 'success';
    default:
      return 'default';
  }
};

const getCategoryColor = (category: GoalProgressData['category']): string => {
  switch (category) {
    case 'focus':
      return '#1976d2';
    case 'productivity':
      return '#388e3c';
    case 'collaboration':
      return '#f57c00';
    case 'wellness':
      return '#7b1fa2';
    default:
      return '#616161';
  }
};

const getProgressColor = (progress: number): 'success' | 'warning' | 'error' | 'primary' => {
  if (progress >= 1) return 'success';
  if (progress >= 0.75) return 'primary';
  if (progress >= 0.5) return 'warning';
  return 'error';
};

const sortGoals = (goals: GoalProgressData[], sortBy: string): GoalProgressData[] => {
  const sorted = [...goals];

  switch (sortBy) {
    case 'progress':
      return sorted.sort((a, b) => b.progress - a.progress);
    case 'deadline':
      return sorted.sort((a, b) => {
        if (!a.deadline && !b.deadline) return 0;
        if (!a.deadline) return 1;
        if (!b.deadline) return -1;
        return a.deadline.getTime() - b.deadline.getTime();
      });
    case 'priority': {
      const priorityOrder = {high: 3, medium: 2, low: 1};
      return sorted.sort((a, b) => priorityOrder[b.priority] - priorityOrder[a.priority]);
    }
    case 'category':
      return sorted.sort((a, b) => a.category.localeCompare(b.category));
    default:
      return sorted;
  }
};

const GoalCard: React.FC<{
  goal: GoalProgressData;
  showMilestones?: boolean;
  onGoalClick?: (goal: GoalProgressData) => void;
}> = ({goal, showMilestones = false, onGoalClick}) => {
  const [expanded, setExpanded] = useState(false);
  const isCompleted = goal.progress >= 1;
  const isOverdue = goal.deadline && isPast(goal.deadline) && !isCompleted;
  const progressPercentage = Math.min(goal.progress * 100, 100);

  return (
      <Card
          data-testid={`goal-card-${goal.id}`}
          className={isCompleted ? 'goal-completed' : ''}
          sx={{
            cursor: onGoalClick ? 'pointer' : 'default',
            border: isCompleted ? '2px solid' : '1px solid',
            borderColor: isCompleted ? 'success.main' : 'divider',
            bgcolor: isCompleted ? 'success.50' : 'background.paper',
            '&:hover': onGoalClick ? {
              boxShadow: 2,
              transform: 'translateY(-2px)'
            } : {}
          }}
          onClick={() => onGoalClick?.(goal)}
      >
        <CardContent>
          {/* Header */}
          <Box display="flex" alignItems="flex-start" justifyContent="space-between" mb={2}>
            <Box flex={1}>
              <Box display="flex" alignItems="center" gap={1} mb={1}>
                <Typography variant="h6" sx={{fontWeight: 'bold'}}>
                  {goal.title}
                </Typography>
                {isCompleted && (
                    <Chip
                        icon={<CheckCircle/>}
                        label="Completed!"
                        color="success"
                        size="small"
                    />
                )}
                {isOverdue && (
                    <Chip
                        label="Overdue"
                        color="error"
                        size="small"
                    />
                )}
              </Box>

              <Box display="flex" alignItems="center" gap={1} mb={1}>
                <Chip
                    className={`priority-indicator priority-${goal.priority}`}
                    icon={<Flag/>}
                    label={goal.priority}
                    color={getPriorityColor(goal.priority)}
                    size="small"
                    sx={{textTransform: 'capitalize'}}
                />
                <Chip
                    label={goal.category}
                    size="small"
                    sx={{
                      bgcolor: getCategoryColor(goal.category),
                      color: 'white',
                      textTransform: 'capitalize'
                    }}
                />
              </Box>
            </Box>

            {goal.description && (
                <IconButton
                    aria-label="expand goal details"
                    onClick={(e) => {
                      e.stopPropagation();
                      setExpanded(!expanded);
                    }}
                >
                  {expanded ? <ExpandLess/> : <ExpandMore/>}
                </IconButton>
            )}
          </Box>

          {/* Progress */}
          <Box mb={2}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
              <Typography variant="body2" color="text.secondary">
                Progress
              </Typography>
              <Typography variant="h6" color={getProgressColor(goal.progress) + '.main'}>
                {Math.round(progressPercentage)}%
              </Typography>
            </Box>

            <LinearProgress
                data-testid={`progress-bar-${goal.id}`}
                variant="determinate"
                value={progressPercentage}
                color={getProgressColor(goal.progress)}
                sx={{height: 8, borderRadius: 4}}
                aria-valuenow={Math.round(progressPercentage)}
            />

            <Typography variant="body2" color="text.secondary" sx={{mt: 1}}>
              {goal.current} / {goal.target} {goal.unit}
            </Typography>
          </Box>

          {/* Deadline */}
          {goal.deadline && (
              <Box display="flex" alignItems="center" gap={1} mb={1}>
                <Schedule fontSize="small" color={isOverdue ? 'error' : 'action'}/>
                <Typography
                    variant="body2"
                    color={isOverdue ? 'error.main' : 'text.secondary'}
                >
                  Due: {format(goal.deadline, 'MMM d, yyyy')}
                </Typography>
                {!isCompleted && (
                    <Typography variant="body2" color="text.secondary">
                      ({formatDistanceToNow(goal.deadline, {addSuffix: true})})
                    </Typography>
                )}
              </Box>
          )}

          {/* Description */}
          <Collapse in={expanded}>
            <Typography variant="body2" color="text.secondary" sx={{mt: 1}}>
              {goal.description}
            </Typography>
          </Collapse>

          {/* Milestones */}
          {showMilestones && goal.milestones.length > 0 && (
              <Box sx={{mt: 2}}>
                <Typography variant="subtitle2" gutterBottom>
                  Milestones
                </Typography>
                <List dense>
                  {goal.milestones.map((milestone, index) => (
                      <ListItem key={index} sx={{px: 0}}>
                        <ListItemIcon sx={{minWidth: 32}}>
                          {milestone.achieved ? (
                              <CheckCircle
                                  color="success"
                                  fontSize="small"
                                  aria-label="milestone achieved"
                              />
                          ) : (
                              <RadioButtonUnchecked
                                  color="action"
                                  fontSize="small"
                              />
                          )}
                        </ListItemIcon>
                        <ListItemText
                            primary={milestone.label}
                            secondary={
                              milestone.achieved && milestone.achievedAt
                                  ? `Completed ${format(milestone.achievedAt, 'MMM d')}`
                                  : `${milestone.value} ${goal.unit}`
                            }
                            sx={{
                              textDecoration: milestone.achieved ? 'line-through' : 'none',
                              opacity: milestone.achieved ? 0.7 : 1
                            }}
                        />
                      </ListItem>
                  ))}
                </List>
              </Box>
          )}
        </CardContent>
      </Card>
  );
};

export const GoalProgress: React.FC<GoalProgressProps> = ({
                                                            goals,
                                                            layout = 'grid',
                                                            showMilestones = false,
                                                            onGoalClick
                                                          }) => {
  const [sortBy, setSortBy] = useState('progress');
  const [filterCategory, setFilterCategory] = useState<string>('all');

  if (goals.length === 0) {
    return (
        <Card>
          <CardHeader
              title={
                <Box display="flex" alignItems="center" gap={1}>
                  <EmojiEvents color="primary"/>
                  <Typography variant="h6">Goal Progress</Typography>
                </Box>
              }
          />
          <CardContent>
            <Box textAlign="center" py={4}>
              <EmojiEvents sx={{fontSize: 48, color: 'text.secondary', mb: 2}}/>
              <Typography variant="h6" color="text.secondary">
                No goals set yet
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Set your first goal to start tracking progress!
              </Typography>
            </Box>
          </CardContent>
        </Card>
    );
  }

  // Filter goals by category
  const filteredGoals = filterCategory === 'all'
      ? goals
      : goals.filter(goal => goal.category === filterCategory);

  // Sort goals
  const sortedGoals = sortGoals(filteredGoals, sortBy);

  // Get unique categories for filter
  const categories = Array.from(new Set(goals.map(goal => goal.category)));

  // Calculate summary stats
  const completedGoals = goals.filter(goal => goal.progress >= 1).length;
  const averageProgress = goals.length > 0
      ? goals.reduce((sum, goal) => sum + goal.progress, 0) / goals.length
      : 0;

  return (
      <Card>
        <CardHeader
            title={
              <Box display="flex" alignItems="center" gap={1}>
                <EmojiEvents color="primary"/>
                <Typography variant="h6">Goal Progress</Typography>
                <Chip
                    label={`${completedGoals}/${goals.length} completed`}
                    color={completedGoals === goals.length ? 'success' : 'primary'}
                    size="small"
                />
              </Box>
            }
            action={
              <Box display="flex" gap={1}>
                <FormControl size="small" sx={{minWidth: 120}}>
                  <InputLabel>Sort by</InputLabel>
                  <Select
                      value={sortBy}
                      onChange={(e) => setSortBy(e.target.value)}
                      label="Sort by"
                  >
                    <MenuItem value="progress">Progress</MenuItem>
                    <MenuItem value="deadline">Deadline</MenuItem>
                    <MenuItem value="priority">Priority</MenuItem>
                    <MenuItem value="category">Category</MenuItem>
                  </Select>
                </FormControl>

                <FormControl size="small" sx={{minWidth: 120}}>
                  <InputLabel>Category</InputLabel>
                  <Select
                      value={filterCategory}
                      onChange={(e) => setFilterCategory(e.target.value)}
                      label="Category"
                  >
                    <MenuItem value="all">All</MenuItem>
                    {categories.map(category => (
                        <MenuItem key={category} value={category}>
                          {category.charAt(0).toUpperCase() + category.slice(1)}
                        </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Box>
            }
        />
        <CardContent>
          {/* Summary Stats */}
          <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: {
                  xs: 'repeat(2, 1fr)',
                  sm: 'repeat(4, 1fr)'
                },
                gap: 2,
                mb: 3
              }}
          >
            <Box textAlign="center" p={2} bgcolor="success.50" borderRadius={1}>
              <Typography variant="h5" color="success.main" fontWeight="bold">
                {completedGoals}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Completed
              </Typography>
            </Box>
            <Box textAlign="center" p={2} bgcolor="primary.50" borderRadius={1}>
              <Typography variant="h5" color="primary.main" fontWeight="bold">
                {goals.length - completedGoals}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                In Progress
              </Typography>
            </Box>
            <Box textAlign="center" p={2} bgcolor="warning.50" borderRadius={1}>
              <Typography variant="h5" color="warning.main" fontWeight="bold">
                {Math.round(averageProgress * 100)}%
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Avg Progress
              </Typography>
            </Box>
            <Box textAlign="center" p={2} bgcolor="info.50" borderRadius={1}>
              <Typography variant="h5" color="info.main" fontWeight="bold">
                {goals.filter(g => g.deadline && isPast(g.deadline) && g.progress < 1).length}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Overdue
              </Typography>
            </Box>
          </Box>

          {/* Goals Grid/List */}
          <Box
              data-testid="goals-container"
              className={layout === 'list' ? 'goals-list-layout' : 'goals-grid-layout'}
              sx={{
                display: 'grid',
                gridTemplateColumns: layout === 'grid' ? {
                  xs: '1fr',
                  sm: 'repeat(2, 1fr)',
                  md: 'repeat(3, 1fr)'
                } : '1fr',
                gap: 2
              }}
          >
            {sortedGoals.map((goal) => (
                <GoalCard
                    key={goal.id}
                    goal={goal}
                    showMilestones={showMilestones}
                    onGoalClick={onGoalClick}
                />
            ))}
          </Box>
        </CardContent>
      </Card>
  );
};