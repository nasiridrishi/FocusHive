import React from 'react';
import {alpha, Box, Card, Chip, Tooltip, Typography, useTheme,} from '@mui/material';
import {motion} from 'framer-motion';
import {
  BrokenImage,
  EmojiEvents,
  FitnessCenter,
  Group,
  LocalFireDepartment,
  Login,
  TrendingUp,
} from '@mui/icons-material';
import type {StreakCounterProps} from '../types/gamification';
import {
  calculateDaysActive,
  calculateTimeUntilStreakBreaks,
  formatDuration,
  formatStreakType,
} from '../utils/gamificationUtils';

const StreakCounter: React.FC<StreakCounterProps> = ({
                                                       streak,
                                                       variant = 'default',
                                                       showBest = true,
                                                     }) => {
  const theme = useTheme();
  const isActive = streak.isActive;
  const isNewRecord = streak.current === streak.best && streak.current > 0;
  const daysActive = streak.lastActivity ? calculateDaysActive(streak.lastActivity) : 0;
  const hoursUntilBreak = calculateTimeUntilStreakBreaks(streak);

  // Check if user prefers reduced motion
  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const getStreakIcon = (): React.ReactElement => {
    const iconProps = {fontSize: 'inherit' as const};

    switch (streak.type) {
      case 'daily_login':
        return <Login {...iconProps} />;
      case 'focus_session':
        return <FitnessCenter {...iconProps} />;
      case 'goal_completion':
        return <EmojiEvents {...iconProps} />;
      case 'hive_participation':
        return <Group {...iconProps} />;
      default:
        return <TrendingUp {...iconProps} />;
    }
  };

  const getVariantStyles = (): {padding: number; minHeight: number; flexDirection: string; gap: number} => {
    switch (variant) {
      case 'compact':
        return {
          padding: 1,
          minHeight: 60,
          flexDirection: 'row' as const,
          gap: 1,
        };
      case 'detailed':
        return {
          padding: 3,
          minHeight: 160,
          flexDirection: 'column' as const,
          gap: 2,
        };
      default:
        return {
          padding: 2,
          minHeight: 120,
          flexDirection: 'column' as const,
          gap: 1.5,
        };
    }
  };

  const variantStyles = getVariantStyles();

  const cardVariants = {
    initial: {scale: 0.9, opacity: 0},
    animate: {
      scale: 1,
      opacity: 1,
    },
    newRecord: {
      scale: [1, 1.05, 1],
      boxShadow: [
        '0 4px 20px rgba(255, 193, 7, 0.3)',
        '0 8px 30px rgba(255, 193, 7, 0.5)',
        '0 4px 20px rgba(255, 193, 7, 0.3)',
      ],
    }
  };

  const animateTransition = {duration: 0.3, ease: 'easeOut' as const};
  const newRecordTransition = {
    duration: 2,
    repeat: Infinity,
    repeatDelay: 3,
  };

  const fireVariants = {
    burning: {
      scale: [1, 1.1, 1],
      rotate: [-2, 2, -2],
      transition: {
        duration: 0.5,
        repeat: Infinity,
        repeatType: 'reverse' as const,
      }
    },
    extinguished: {
      scale: 0.8,
      opacity: 0.5,
    }
  };

  const formatLastActivity = (): string => {
    if (!streak.lastActivity) return 'never';
    if (daysActive === 0) return 'today';
    if (daysActive === 1) return 'yesterday';
    return `${daysActive} days ago`;
  };

  return (
      <Card
          component={motion.div}
          variants={cardVariants}
          initial="initial"
          animate={isNewRecord && !prefersReducedMotion ? 'newRecord' : 'animate'}
          transition={isNewRecord && !prefersReducedMotion ? newRecordTransition : animateTransition}
          data-testid="streak-counter"
          className={`
        ${isActive ? 'streak-active active' : 'streak-inactive inactive'}
        variant-${variant}
        ${isNewRecord ? 'new-record' : ''}
        ${prefersReducedMotion ? 'reduced-motion' : ''}
      `}
          role="region"
          aria-label="Streak counter"
          sx={{
            ...variantStyles,
            background: isActive
                ? `linear-gradient(135deg, ${alpha(theme.palette.success.main, 0.1)}, ${alpha(theme.palette.warning.main, 0.1)})`
                : `linear-gradient(135deg, ${alpha(theme.palette.grey[500], 0.05)}, ${alpha(theme.palette.grey[600], 0.1)})`,
            border: `2px solid ${
                isActive
                    ? isNewRecord
                        ? theme.palette.warning.main
                        : theme.palette.success.main
                    : theme.palette.grey[400]
            }`,
            borderRadius: 3,
            display: 'flex',
            alignItems: variant === 'compact' ? 'center' : 'flex-start',
            position: 'relative',
            overflow: 'hidden',

            '&.new-record::before': {
              content: '"🎉"',
              position: 'absolute',
              top: 8,
              right: 8,
              fontSize: '1.2em',
              animation: !prefersReducedMotion ? 'bounce 1s infinite' : 'none',
            },

            '@keyframes bounce': {
              '0%, 20%, 50%, 80%, 100%': {transform: 'translateY(0)'},
              '40%': {transform: 'translateY(-10px)'},
              '60%': {transform: 'translateY(-5px)'},
            },
          }}
      >
        {/* Streak Icon and Status */}
        <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: variant === 'compact' ? 1 : 1.5,
              mb: variant === 'compact' ? 0 : 1,
            }}
        >
          <Box
              data-testid="streak-icon"
              data-streak-type={streak.type}
              sx={{
                width: variant === 'compact' ? 32 : 40,
                height: variant === 'compact' ? 32 : 40,
                borderRadius: '50%',
                background: isActive
                    ? `linear-gradient(135deg, ${theme.palette.primary.main}, ${theme.palette.secondary.main})`
                    : theme.palette.grey[400],
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'white',
                fontSize: variant === 'compact' ? '1.2rem' : '1.5rem',
              }}
          >
            {getStreakIcon()}
          </Box>

          <Box sx={{flex: 1}}>
            <Typography
                variant={variant === 'compact' ? 'body2' : 'body1'}
                sx={{
                  fontWeight: 'medium',
                  color: theme.palette.text.primary,
                }}
            >
              {formatStreakType(streak.type)}
            </Typography>

            {variant !== 'compact' && (
                <Typography
                    variant="caption"
                    sx={{
                      color: isActive ? theme.palette.success.main : theme.palette.error.main,
                      fontWeight: 'medium',
                    }}
                >
                  {isActive ? 'Streak is active' : 'Streak is broken'}
                  <span className="sr-only">
                {isActive ? 'ACTIVE' : 'BROKEN'}
              </span>
                </Typography>
            )}
          </Box>

          {/* Status Icon */}
          <Box sx={{display: 'flex', alignItems: 'center'}}>
            {isActive ? (
                <motion.div
                    variants={fireVariants}
                    animate={!prefersReducedMotion ? 'burning' : undefined}
                    data-testid="fire-icon"
                >
                  <LocalFireDepartment
                      sx={{
                        color: theme.palette.warning.main,
                        fontSize: '1.5rem',
                      }}
                  />
                </motion.div>
            ) : (
                <BrokenImage
                    data-testid="broken-chain-icon"
                    sx={{
                      color: theme.palette.grey[500],
                      fontSize: '1.5rem',
                    }}
                />
            )}
          </Box>
        </Box>

        {/* Current Streak */}
        <Box
            sx={{
              display: 'flex',
              alignItems: variant === 'compact' ? 'center' : 'flex-start',
              justifyContent: variant === 'compact' ? 'flex-end' : 'space-between',
              gap: 2,
              flex: 1,
            }}
        >
          <Box sx={{textAlign: variant === 'compact' ? 'right' : 'left'}}>
            <Typography
                variant="caption"
                color="text.secondary"
                display="block"
            >
              Current Streak
            </Typography>
            <Typography
                variant={variant === 'compact' ? 'h6' : 'h4'}
                sx={{
                  fontWeight: 'bold',
                  color: isActive ? theme.palette.primary.main : theme.palette.text.secondary,
                  lineHeight: 1,
                }}
                aria-label={`Current streak: ${Math.max(0, streak.current)} days`}
            >
              {Math.max(0, streak.current)}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {Math.max(0, streak.current) === 1 ? 'day' : 'days'}
            </Typography>
          </Box>

          {/* Best Streak */}
          {showBest && (
              <Box sx={{textAlign: variant === 'compact' ? 'right' : 'left'}}>
                <Typography
                    variant="caption"
                    color="text.secondary"
                    display="block"
                >
                  Best
                </Typography>
                <Box
                    className={`best-streak ${isNewRecord ? 'is-current-best' : ''}`}
                    sx={{
                      display: 'flex',
                      alignItems: 'baseline',
                      gap: 0.5,
                    }}
                >
                  <Typography
                      variant={variant === 'compact' ? 'body1' : 'h5'}
                      sx={{
                        fontWeight: 'medium',
                        color: isNewRecord ? theme.palette.warning.main : theme.palette.text.primary,
                      }}
                      aria-label={`Best streak: ${streak.best} days`}
                  >
                    {streak.best.toLocaleString()}
                  </Typography>
                  {isNewRecord && (
                      <Chip
                          label="NEW!"
                          size="small"
                          sx={{
                            backgroundColor: theme.palette.warning.main,
                            color: 'white',
                            fontSize: '0.6rem',
                            height: 16,
                            fontWeight: 'bold',
                          }}
                      />
                  )}
                </Box>
              </Box>
          )}
        </Box>

        {/* Detailed Information */}
        {variant === 'detailed' && (
            <Box
                sx={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  pt: 1,
                  borderTop: `1px solid ${theme.palette.divider}`,
                }}
            >
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Last Activity
                </Typography>
                <Typography
                    variant="body2"
                    sx={{fontWeight: 'medium'}}
                >
                  {formatLastActivity()}
                </Typography>
              </Box>

              {isActive && hoursUntilBreak > 0 && (
                  <Tooltip title="Time remaining to maintain streak">
                    <Box sx={{textAlign: 'right'}}>
                      <Typography variant="caption" color="text.secondary">
                        Time Left
                      </Typography>
                      <Typography
                          variant="body2"
                          sx={{
                            fontWeight: 'medium',
                            color: hoursUntilBreak <= 6
                                ? theme.palette.warning.main
                                : theme.palette.success.main,
                          }}
                      >
                        {formatDuration(hoursUntilBreak)}
                      </Typography>
                    </Box>
                  </Tooltip>
              )}

              {variant === 'detailed' && (
                  <Box sx={{textAlign: 'right'}}>
                    <Typography variant="caption" color="text.secondary">
                      Streak Started
                    </Typography>
                    <Typography
                        variant="body2"
                        sx={{fontWeight: 'medium'}}
                    >
                      {streak.current > 0 ? `${streak.current} days ago` : 'Not started'}
                    </Typography>
                  </Box>
              )}
            </Box>
        )}
      </Card>
  );
};

export default React.memo(StreakCounter);