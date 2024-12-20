import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Box,
  Card,
  CardContent,
  CardActions,
  Typography,
  LinearProgress,
  CircularProgress,
  Chip,
  Stack,
  Skeleton,
  Tooltip,
  IconButton,
  Collapse,
  Fade,
  useTheme,
  useMediaQuery,
  Breakpoint,
} from '@mui/material';
import Grid from '../../../components/ui/Grid';
import {
  CheckCircle as CheckIcon,
  RadioButtonUnchecked as ProgressIcon,
  ExpandMore as ExpandMoreIcon,
  ExpandLess as ExpandLessIcon,
  Schedule as ScheduleIcon,
  Star as StarIcon,
  Celebration as CelebrationIcon,
  Warning as WarningIcon,
  Today as TodayIcon,
  DateRange as WeekIcon,
  CalendarMonth as MonthIcon,
  Assignment as CustomIcon,
} from '@mui/icons-material';
import { styled, keyframes } from '@mui/material/styles';
import LoadingSpinner from '../../../shared/ui/LoadingSpinner';

// Enhanced Goal types based on test requirements
export interface GoalMilestone {
  value: number;
  label: string;
  achieved: boolean;
  achievedAt?: Date;
  pointsReward: number;
}

export interface Goal {
  id: string;
  title: string;
  description: string;
  target: number;
  current: number;
  unit: string;
  category: 'focus' | 'productivity' | 'collaboration' | 'learning' | 'wellness';
  priority: 'low' | 'medium' | 'high';
  deadline: Date;
  progress: number; // 0-1
  type: 'daily' | 'weekly' | 'monthly' | 'custom';
  pointsReward: number;
  streakBonus: number;
  completedAt?: Date;
  milestones: GoalMilestone[];
}

export interface GoalProgressProps {
  goals: Goal[];
  onGoalClick?: (goal: Goal) => void;
  onMilestoneClick?: (milestone: GoalMilestone, goal: Goal) => void;
  showMilestones?: boolean;
  showAnimation?: boolean;
  layout?: 'grid' | 'list';
  loading?: boolean;
  compact?: boolean;
  highContrast?: boolean;
}

// Animation keyframes
const celebrationAnimation = keyframes`
  0% { transform: scale(1) rotate(0deg); }
  25% { transform: scale(1.1) rotate(5deg); }
  50% { transform: scale(1.2) rotate(-5deg); }
  75% { transform: scale(1.1) rotate(5deg); }
  100% { transform: scale(1) rotate(0deg); }
`;

const confettiAnimation = keyframes`
  0% { transform: translateY(0) rotate(0deg); opacity: 1; }
  100% { transform: translateY(-100px) rotate(360deg); opacity: 0; }
`;

const milestoneAchievementAnimation = keyframes`
  0% { transform: scale(1); }
  50% { transform: scale(1.3); }
  100% { transform: scale(1); }
`;

const progressUpdateAnimation = keyframes`
  0% { transform: scaleX(0); }
  100% { transform: scaleX(1); }
`;

// Styled components
const StyledCard = styled(Card, {
  shouldForwardProp: (prop) => !['goalState', 'compact', 'highContrast'].includes(prop as string),
})<{ goalState: string; compact?: boolean; highContrast?: boolean }>(({ theme, goalState, compact, highContrast }) => ({
  cursor: 'pointer',
  transition: 'all 0.3s ease',
  border: highContrast ? '2px solid' : '1px solid transparent',

  '&:hover': {
    transform: compact ? 'none' : 'translateY(-2px)',
    boxShadow: theme.shadows[8],
  },

  '&:focus-visible': {
    outline: `3px solid ${theme.palette.primary.main}`,
    outlineOffset: '2px',
  },

  '&.goal-not-started': {
    borderLeftColor: theme.palette.grey[400],
    borderLeftWidth: '4px',
  },

  '&.goal-in-progress': {
    borderLeftColor: theme.palette.primary.main,
    borderLeftWidth: '4px',
  },

  '&.goal-completed': {
    borderLeftColor: theme.palette.success.main,
    borderLeftWidth: '4px',
    animation: goalState === 'completed' ? `${celebrationAnimation} 2s ease-in-out` : 'none',
  },

  '&.goal-overdue': {
    borderLeftColor: theme.palette.error.main,
    borderLeftWidth: '4px',
  },

  '&.hovered': {
    backgroundColor: theme.palette.action.hover,
  },

  '&.focus-visible': {
    outline: `2px solid ${theme.palette.primary.main}`,
  },
}));

const AnimatedProgress = styled(LinearProgress, {
  shouldForwardProp: (prop) => prop !== 'animated',
})<{ animated?: boolean }>(({ animated }) => ({
  '&.animated-progress .MuiLinearProgress-bar': {
    animation: animated ? `${progressUpdateAnimation} 1s ease-out` : 'none',
  },

  '&.progress-not-started .MuiLinearProgress-bar': {
    backgroundColor: '#e0e0e0',
  },

  '&.progress-in-progress .MuiLinearProgress-bar': {
    backgroundColor: '#1976d2',
  },

  '&.progress-completed .MuiLinearProgress-bar': {
    backgroundColor: '#4caf50',
  },

  '&.progress-overdue .MuiLinearProgress-bar': {
    backgroundColor: '#f44336',
  },

  '&.high-contrast-progress': {
    height: '8px',
    '& .MuiLinearProgress-bar': {
      border: '1px solid #000',
    },
  },
}));

const MilestoneContainer = styled(Box)(({ theme }) => ({
  display: 'flex',
  flexWrap: 'wrap',
  gap: theme.spacing(1),
  marginTop: theme.spacing(1),
}));

const MilestoneChip = styled(Chip, {
  shouldForwardProp: (prop) => prop !== 'achieved',
})<{ achieved: boolean }>(({ theme, achieved }) => ({
  cursor: 'pointer',
  transition: 'all 0.2s ease',

  '&.milestone-achieved': {
    backgroundColor: theme.palette.success.light,
    color: theme.palette.success.contrastText,
    animation: `${milestoneAchievementAnimation} 0.3s ease`,
  },

  '&.milestone-unachieved': {
    backgroundColor: theme.palette.grey[200],
    color: theme.palette.text.secondary,
  },

  '&:hover': {
    transform: 'scale(1.05)',
  },
}));

const ConfettiContainer = styled(Box)({
  position: 'absolute',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  pointerEvents: 'none',
  overflow: 'hidden',

  '& .confetti-piece': {
    position: 'absolute',
    width: '10px',
    height: '10px',
    backgroundColor: '#ffeb3b',
    animation: `${confettiAnimation} 3s ease-out forwards`,
  },
});

const StatusIndicator = styled(Box, {
  shouldForwardProp: (prop) => prop !== 'status',
})<{ status: string }>(({ theme, status }) => {
  const getColor = () => {
    switch (status) {
      case 'completed': return theme.palette.success.main;
      case 'in-progress': return theme.palette.primary.main;
      case 'overdue': return theme.palette.error.main;
      default: return theme.palette.grey[400];
    }
  };

  return {
    width: '12px',
    height: '12px',
    borderRadius: '50%',
    backgroundColor: getColor(),
    display: 'inline-block',
    marginRight: theme.spacing(1),
  };
});

// Goal Progress Component
export const GoalProgress: React.FC<GoalProgressProps> = ({
  goals,
  onGoalClick,
  onMilestoneClick,
  showMilestones = true,
  showAnimation = true,
  layout = 'grid',
  loading = false,
  compact = false,
  highContrast = false,
}) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm' as Breakpoint));
  const isTablet = useMediaQuery(theme.breakpoints.down('md' as Breakpoint));
  const [expandedGoals, setExpandedGoals] = useState<Set<string>>(new Set());
  const [hoveredGoal, setHoveredGoal] = useState<string | null>(null);
  const [showCelebration, setShowCelebration] = useState<Set<string>>(new Set());

  // Auto-expand goals in non-compact mode
  useEffect(() => {
    if (!compact) {
      setExpandedGoals(new Set(goals.map(goal => goal.id)));
    }
  }, [goals, compact]);

  // Trigger celebration animation for completed goals
  useEffect(() => {
    const completedGoals = goals.filter(goal => goal.progress >= 1.0);
    if (showAnimation && completedGoals.length > 0) {
      const newCelebrations = new Set<string>(completedGoals.map(goal => goal.id));
      setShowCelebration(newCelebrations);

      // Clear celebrations after animation
      const timer = setTimeout(() => {
        setShowCelebration(new Set());
      }, 3000);

      return () => clearTimeout(timer);
    }
  }, [goals, showAnimation]);

  const getGoalState = useCallback((goal: Goal): string => {
    if (goal.progress >= 1.0) return 'completed';
    if (goal.deadline < new Date() && goal.progress < 1.0) return 'overdue';
    if (goal.progress > 0) return 'in-progress';
    return 'not-started';
  }, []);

  const getStatusText = useCallback((state: string): string => {
    switch (state) {
      case 'completed': return 'Completed';
      case 'in-progress': return 'In Progress';
      case 'overdue': return 'Overdue';
      default: return 'Not Started';
    }
  }, []);

  const getProgressClass = useCallback((state: string): string => {
    return `progress-${state.replace('-', '-')}`;
  }, []);

  const getGoalTypeIcon = useCallback((type: Goal['type']) => {
    switch (type) {
      case 'daily': return <TodayIcon fontSize="small" />;
      case 'weekly': return <WeekIcon fontSize="small" />;
      case 'monthly': return <MonthIcon fontSize="small" />;
      default: return <CustomIcon fontSize="small" />;
    }
  }, []);

  const getPriorityClass = useCallback((priority: Goal['priority']): string => {
    return `priority-${priority}`;
  }, []);

  const getCategoryClass = useCallback((category: Goal['category']): string => {
    return `category-${category}`;
  }, []);

  const formatTimeRemaining = useCallback((deadline: Date): string => {
    const now = new Date();
    const diff = deadline.getTime() - now.getTime();
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const days = Math.floor(hours / 24);

    if (days > 0) return `${days} day${days !== 1 ? 's' : ''} remaining`;
    if (hours > 0) return `${hours} hour${hours !== 1 ? 's' : ''} remaining`;
    return 'Due soon';
  }, []);

  const handleGoalClick = useCallback((goal: Goal) => {
    onGoalClick?.(goal);
  }, [onGoalClick]);

  const handleMilestoneClick = useCallback((milestone: GoalMilestone, goal: Goal) => {
    onMilestoneClick?.(milestone, goal);
  }, [onMilestoneClick]);

  const handleExpandToggle = useCallback((goalId: string) => {
    setExpandedGoals(prev => {
      const newSet = new Set(prev);
      if (newSet.has(goalId)) {
        newSet.delete(goalId);
      } else {
        newSet.add(goalId);
      }
      return newSet;
    });
  }, []);

  const handleGoalHover = useCallback((goalId: string | null) => {
    setHoveredGoal(goalId);
  }, []);

  const renderGoalCard = useCallback((goal: Goal) => {
    const state = getGoalState(goal);
    const isExpanded = expandedGoals.has(goal.id);
    const isHovered = hoveredGoal === goal.id;
    const progressPercent = Math.min(Math.max(goal.progress * 100, 0), 100);
    const shouldShowCelebration = showCelebration.has(goal.id);

    return (
      <StyledCard
        key={goal.id}
        data-testid={`goal-card-${goal.id}`}
        goalState={state}
        compact={compact}
        highContrast={highContrast}
        className={`goal-${state} ${isHovered ? 'hovered' : ''}`}
        tabIndex={0}
        onClick={() => handleGoalClick(goal)}
        onMouseEnter={() => handleGoalHover(goal.id)}
        onMouseLeave={() => handleGoalHover(null)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            handleGoalClick(goal);
          }
        }}
        sx={{
          position: 'relative',
          height: compact ? 'auto' : undefined,
          ...(isMobile && { marginBottom: 2 }),
        }}
      >
        {/* Confetti Effect */}
        {shouldShowCelebration && (
          <ConfettiContainer data-testid="confetti-effect">
            {Array.from({ length: 20 }, (_, i) => (
              <Box
                key={i}
                className="confetti-piece"
                sx={{
                  left: `${Math.random() * 100}%`,
                  animationDelay: `${Math.random() * 2}s`,
                  backgroundColor: ['#ffeb3b', '#4caf50', '#2196f3', '#ff9800'][i % 4],
                }}
              />
            ))}
          </ConfettiContainer>
        )}

        <CardContent sx={{ pb: compact ? 1 : 2 }}>
          {/* Header */}
          <Box display="flex" alignItems="center" justifyContent="space-between" mb={1}>
            <Box display="flex" alignItems="center" gap={1}>
              <Box data-testid={`goal-type-${goal.type}-${goal.id}`}>
                {getGoalTypeIcon(goal.type)}
              </Box>
              <StatusIndicator
                data-testid={`status-indicator-${goal.id}`}
                status={state}
                className={`status-${state}`}
              />
              <Typography variant="h6" component="h3" sx={{ flexGrow: 1 }}>
                {goal.title}
              </Typography>
            </Box>

            <Box display="flex" alignItems="center" gap={1}>
              <Chip
                data-testid={`priority-indicator-${goal.id}`}
                label={goal.priority}
                size="small"
                className={getPriorityClass(goal.priority)}
                color={goal.priority === 'high' ? 'error' : goal.priority === 'medium' ? 'warning' : 'default'}
              />

              {!compact && (
                <IconButton
                  data-testid={`expand-button-${goal.id}`}
                  size="small"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleExpandToggle(goal.id);
                  }}
                >
                  {isExpanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                </IconButton>
              )}
            </Box>
          </Box>

          {/* Status and Category */}
          <Box display="flex" alignItems="center" justifyContent="space-between" mb={2}>
            <Typography
              data-testid={`status-text-${goal.id}`}
              variant="body2"
              color="text.secondary"
            >
              {getStatusText(state)}
            </Typography>

            <Chip
              data-testid={`category-badge-${goal.id}`}
              label={goal.category}
              size="small"
              variant="outlined"
              className={getCategoryClass(goal.category)}
            />
          </Box>

          {/* Description */}
          <Typography variant="body2" color="text.secondary" mb={2}>
            {goal.description}
          </Typography>

          {/* Progress */}
          <Box mb={2}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
              <Typography variant="body2">
                {goal.current} / {goal.target} {goal.unit}
              </Typography>
              <Typography
                data-testid={`progress-text-${goal.id}`}
                variant="body2"
                fontWeight="bold"
              >
                {Math.round(progressPercent)}%
              </Typography>
            </Box>

            <Tooltip
              title={`${goal.current} of ${goal.target} ${goal.unit} completed (${Math.round(progressPercent)}%)`}
              data-testid="progress-tooltip"
            >
              <AnimatedProgress
                data-testid={`progress-bar-${goal.id}`}
                variant="determinate"
                value={progressPercent}
                animated={showAnimation}
                className={`${showAnimation ? 'animated-progress' : ''} ${getProgressClass(state)} ${highContrast ? 'high-contrast-progress' : ''}`}
                role="progressbar"
                aria-label={`Goal progress: ${Math.round(progressPercent)}% complete`}
                aria-valuenow={Math.round(progressPercent)}
                aria-valuemin={0}
                aria-valuemax={100}
                sx={{ height: highContrast ? 8 : 4 }}
              />
            </Tooltip>

            {showAnimation && (
              <Box data-testid={`progress-animation-${goal.id}`} />
            )}
          </Box>

          {/* Rewards */}
          <Box display="flex" alignItems="center" gap={2} mb={2}>
            <Typography
              data-testid={`points-reward-${goal.id}`}
              variant="body2"
              color="primary"
            >
              {goal.pointsReward} points
            </Typography>

            {goal.streakBonus > 0 && (
              <Typography
                data-testid={`streak-bonus-${goal.id}`}
                variant="body2"
                color="secondary"
              >
                +{goal.streakBonus} streak bonus
              </Typography>
            )}
          </Box>

          {/* Deadline */}
          <Box display="flex" alignItems="center" gap={1} mb={showMilestones ? 2 : 0}>
            <ScheduleIcon fontSize="small" color="action" />
            {goal.type === 'daily' && (
              <Typography
                data-testid={`deadline-indicator-${goal.id}`}
                variant="body2"
                color="text.secondary"
              >
                Due today
              </Typography>
            )}
            {goal.type === 'weekly' && (
              <Box data-testid={`week-progress-${goal.id}`}>
                <Typography variant="body2" color="text.secondary">
                  {formatTimeRemaining(goal.deadline)}
                </Typography>
              </Box>
            )}
            {goal.type === 'monthly' && (
              <Box data-testid={`month-progress-${goal.id}`}>
                <Typography variant="body2" color="text.secondary">
                  {formatTimeRemaining(goal.deadline)}
                </Typography>
              </Box>
            )}
            {goal.type === 'custom' && (
              <Box data-testid={`custom-deadline-${goal.id}`}>
                <Typography variant="body2" color="text.secondary">
                  {formatTimeRemaining(goal.deadline)}
                </Typography>
              </Box>
            )}

            {state !== 'overdue' && (
              <Typography
                data-testid={`time-remaining-${goal.id}`}
                variant="body2"
                color="text.secondary"
              >
                {formatTimeRemaining(goal.deadline)}
              </Typography>
            )}
          </Box>

          {/* Completion Celebration */}
          {state === 'completed' && (
            <Box
              data-testid={`completion-celebration-${goal.id}`}
              display="flex"
              alignItems="center"
              gap={1}
              mb={2}
            >
              <CelebrationIcon color="success" />
              <Typography variant="body2" color="success.main">
                Goal completed! 🎉
              </Typography>

              {shouldShowCelebration && (
                <Box data-testid={`celebration-animation-${goal.id}`} />
              )}
            </Box>
          )}

          {/* Overdue Warning */}
          {state === 'overdue' && (
            <Box
              data-testid={`overdue-warning-${goal.id}`}
              display="flex"
              alignItems="center"
              gap={1}
              mb={2}
            >
              <WarningIcon color="error" />
              <Typography variant="body2" color="error.main">
                Goal is overdue
              </Typography>
            </Box>
          )}

          {/* Goal Details (Expandable) */}
          <Collapse in={isExpanded}>
            <Box data-testid={`goal-details-${goal.id}`} className={isExpanded ? 'expanded' : 'collapsed'}>
              {showMilestones && goal.milestones.length > 0 && (
                <Box>
                  <Typography variant="subtitle2" mb={1}>
                    Milestones
                  </Typography>
                  <MilestoneContainer
                    data-testid={`milestones-${goal.id}`}
                    role="list"
                    aria-label="Goal milestones"
                  >
                    {goal.milestones.map((milestone, index) => (
                      <MilestoneChip
                        key={index}
                        data-testid={`milestone-${milestone.achieved ? 'achieved' : 'unachieved'}-${index}-${goal.id}`}
                        achieved={milestone.achieved}
                        className={`milestone-${milestone.achieved ? 'achieved' : 'unachieved'}`}
                        label={
                          <Box display="flex" alignItems="center" gap={0.5}>
                            {milestone.achieved ? (
                              <CheckIcon data-testid="check-icon" fontSize="small" />
                            ) : (
                              <ProgressIcon data-testid="progress-icon" fontSize="small" />
                            )}
                            <span>{milestone.label}</span>
                            <span style={{ fontSize: '0.75em' }}>({milestone.pointsReward} pts)</span>
                          </Box>
                        }
                        onClick={(e) => {
                          e.stopPropagation();
                          handleMilestoneClick(milestone, goal);
                        }}
                        role="listitem"
                        aria-label={`Milestone: ${milestone.label}, ${milestone.achieved ? 'completed' : 'not completed'}`}
                      />
                    ))}
                  </MilestoneContainer>

                  {showAnimation && (
                    <Box data-testid="milestone-achievement-animation" />
                  )}
                </Box>
              )}
            </Box>
          </Collapse>
        </CardContent>

        {/* Hover Actions */}
        {isHovered && (
          <Fade in={isHovered}>
            <Box
              data-testid={`hover-actions-${goal.id}`}
              position="absolute"
              top={8}
              right={8}
              zIndex={1}
            >
              <StarIcon color="primary" fontSize="small" />
            </Box>
          </Fade>
        )}

        {/* Screen Reader Status */}
        <Box
          data-testid={`sr-status-${goal.id}`}
          className="sr-only"
          aria-live="polite"
        >
          {state === 'completed' &&
            `Goal completed: ${goal.title}, ${Math.round(progressPercent)}% complete, earned ${goal.pointsReward} points`
          }
        </Box>
      </StyledCard>
    );
  }, [
    expandedGoals, hoveredGoal, showCelebration, compact, highContrast, showMilestones, showAnimation,
    getGoalState, getStatusText, getProgressClass, getGoalTypeIcon, getPriorityClass, getCategoryClass,
    formatTimeRemaining, handleGoalClick, handleMilestoneClick, handleExpandToggle, handleGoalHover, isMobile
  ]);

  const renderSkeletonLoading = () => (
    <Box data-testid="goal-progress-loading">
      {Array.from({ length: 3 }, (_, index) => (
        <Card key={index} data-testid="goal-card-skeleton" sx={{ mb: 2 }}>
          <CardContent>
            <Skeleton variant="text" width="60%" height={32} />
            <Skeleton variant="text" width="80%" height={20} sx={{ mt: 1 }} />
            <Skeleton variant="rectangular" width="100%" height={8} sx={{ mt: 2 }} />
            <Box display="flex" gap={1} mt={2}>
              <Skeleton variant="circular" width={24} height={24} />
              <Skeleton variant="circular" width={24} height={24} />
              <Skeleton variant="circular" width={24} height={24} />
            </Box>
          </CardContent>
        </Card>
      ))}
    </Box>
  );

  const renderEmptyState = () => (
    <Box
      data-testid="no-goals-state"
      display="flex"
      flexDirection="column"
      alignItems="center"
      justifyContent="center"
      py={8}
      textAlign="center"
    >
      <Typography variant="h6" color="text.secondary" mb={1}>
        No goals set yet
      </Typography>
      <Typography variant="body2" color="text.secondary">
        Set your first goal to start earning points!
      </Typography>
    </Box>
  );

  const getGridColumns = () => {
    if (isMobile) return 1;
    if (isTablet) return 2;
    return layout === 'list' ? 1 : 3;
  };

  if (loading) {
    return renderSkeletonLoading();
  }

  if (!goals || goals.length === 0) {
    return renderEmptyState();
  }

  return (
    <Box
      data-testid="goal-progress-container"
      className={`${compact ? 'compact-mode' : ''} ${isMobile ? 'mobile-layout' : ''} ${highContrast ? 'high-contrast-mode' : ''}`}
    >
      <Grid
        container
        spacing={2}
        data-testid="goals-container"
        className={layout === 'grid' ? 'goals-grid-layout' : 'goals-list-layout'}
        sx={{
          gridTemplateColumns: `repeat(${getGridColumns()}, 1fr)`,
        }}
      >
        {goals.map((goal) => (
          <Grid xs={12} sm={layout === 'list' ? 12 : 6} md={layout === 'list' ? 12 : 4} key={goal.id}>
            {renderGoalCard(goal)}
          </Grid>
        ))}
      </Grid>
    </Box>
  );
};

export default GoalProgress;