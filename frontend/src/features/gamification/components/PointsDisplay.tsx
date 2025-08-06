import React from 'react';
import { Box, Typography, Card, useTheme, useMediaQuery } from '@mui/material';
import { motion, AnimatePresence } from 'framer-motion';
import { Star, Today, DateRange } from '@mui/icons-material';
import type { PointsDisplayProps } from '../types/gamification';
import { formatPoints } from '../utils/gamificationUtils';

const PointsDisplay: React.FC<PointsDisplayProps> = ({
  points,
  showToday = false,
  showWeek = false,
  size = 'medium',
  animated = true,
}) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('tablet'));
  const isTablet = useMediaQuery(theme.breakpoints.down('desktop'));

  const getSizeStyles = () => {
    switch (size) {
      case 'small':
        return {
          fontSize: '1.5rem',
          padding: '8px 16px',
          minHeight: '80px',
        };
      case 'large':
        return {
          fontSize: '2.5rem',
          padding: '24px 32px',
          minHeight: '160px',
        };
      default:
        return {
          fontSize: '2rem',
          padding: '16px 24px',
          minHeight: '120px',
        };
    }
  };

  const sizeStyles = getSizeStyles();

  const containerVariants = {
    initial: { scale: 0.9, opacity: 0 },
    animate: { 
      scale: 1, 
      opacity: 1,
    },
    exit: { scale: 0.9, opacity: 0 }
  };

  const containerTransition = { duration: 0.3, ease: 'easeOut' as const };

  const numberVariants = {
    initial: { y: 20, opacity: 0 },
    animate: { 
      y: 0, 
      opacity: 1,
      transition: { duration: 0.4, delay: 0.1 }
    }
  };

  const getLayoutClass = () => {
    if (isMobile) return 'mobile-layout';
    if (isTablet) return 'tablet-layout';
    return 'desktop-layout';
  };

  return (
    <Card
      component={animated ? motion.div : 'div'}
      variants={animated ? containerVariants : undefined}
      initial={animated ? 'initial' : undefined}
      animate={animated ? 'animate' : undefined}
      exit={animated ? 'exit' : undefined}
      transition={animated ? containerTransition : undefined}
      data-testid="points-display"
      data-animated={animated.toString()}
      role="region"
      aria-label="Points display"
      className={getLayoutClass()}
      sx={{
        ...sizeStyles,
        background: `linear-gradient(135deg, ${theme.palette.primary.main}20, ${theme.palette.secondary.main}20)`,
        border: `2px solid ${theme.palette.primary.main}30`,
        borderRadius: 3,
        display: 'flex',
        flexDirection: isMobile ? 'column' : 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 2,
        position: 'relative',
        overflow: 'hidden',
        '&::before': {
          content: '""',
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          height: '3px',
          background: `linear-gradient(90deg, ${theme.palette.primary.main}, ${theme.palette.secondary.main})`,
        },
        '&.mobile-layout': {
          padding: '12px 16px',
          minHeight: 'auto',
        },
        '&.tablet-layout': {
          padding: '16px 20px',
        },
      }}
    >
      {/* Main Points Section */}
      <Box 
        sx={{ 
          display: 'flex', 
          alignItems: 'center', 
          gap: 2,
          flex: 1,
        }}
      >
        <Box
          sx={{
            width: 48,
            height: 48,
            borderRadius: '50%',
            background: `linear-gradient(135deg, ${theme.palette.primary.main}, ${theme.palette.secondary.main})`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'white',
          }}
        >
          <Star />
        </Box>
        
        <Box>
          <Typography
            component={animated ? motion.span : 'span'}
            variants={animated ? numberVariants : undefined}
            initial={animated ? 'initial' : undefined}
            animate={animated ? 'animate' : undefined}
            variant="h3"
            aria-label={`Current points: ${formatPoints(points.current)}`}
            sx={{
              fontSize: sizeStyles.fontSize,
              fontWeight: 'bold',
              color: theme.palette.primary.main,
              lineHeight: 1,
            }}
          >
            {formatPoints(points.current)}
          </Typography>
          <Typography 
            variant="body2" 
            color="text.secondary"
            sx={{ marginTop: 0.5 }}
          >
            points
          </Typography>
        </Box>
      </Box>

      {/* Total Points */}
      <Box sx={{ textAlign: isMobile ? 'center' : 'right' }}>
        <Typography 
          variant="caption" 
          color="text.secondary"
          display="block"
        >
          Total
        </Typography>
        <Typography 
          variant="h6"
          color="text.primary"
          aria-label={`Total points: ${formatPoints(points.total)}`}
          sx={{ fontWeight: 'medium' }}
        >
          {formatPoints(points.total)}
        </Typography>
      </Box>

      {/* Today Points */}
      {showToday && (
        <AnimatePresence>
          <Box
            component={animated ? motion.div : 'div'}
            initial={animated ? { opacity: 0, x: 20 } : undefined}
            animate={animated ? { opacity: 1, x: 0 } : undefined}
            exit={animated ? { opacity: 0, x: 20 } : undefined}
            sx={{ 
              display: 'flex', 
              alignItems: 'center', 
              gap: 1,
              borderLeft: isMobile ? 'none' : `2px solid ${theme.palette.primary.main}30`,
              borderTop: isMobile ? `2px solid ${theme.palette.primary.main}30` : 'none',
              paddingLeft: isMobile ? 0 : 2,
              paddingTop: isMobile ? 2 : 0,
            }}
          >
            <Today color="primary" fontSize="small" />
            <Box>
              <Typography 
                variant="body2" 
                color="text.secondary"
              >
                Today
              </Typography>
              <Typography 
                variant="h6"
                color="primary"
                aria-label={`Points earned today: ${points.todayEarned}`}
                sx={{ fontWeight: 'medium' }}
              >
                {formatPoints(points.todayEarned)}
              </Typography>
            </Box>
          </Box>
        </AnimatePresence>
      )}

      {/* Week Points */}
      {showWeek && (
        <AnimatePresence>
          <Box
            component={animated ? motion.div : 'div'}
            initial={animated ? { opacity: 0, x: 20 } : undefined}
            animate={animated ? { opacity: 1, x: 0 } : undefined}
            exit={animated ? { opacity: 0, x: 20 } : undefined}
            sx={{ 
              display: 'flex', 
              alignItems: 'center', 
              gap: 1,
              borderLeft: isMobile ? 'none' : `2px solid ${theme.palette.secondary.main}30`,
              borderTop: isMobile ? `2px solid ${theme.palette.secondary.main}30` : 'none',
              paddingLeft: isMobile ? 0 : 2,
              paddingTop: isMobile ? 2 : 0,
            }}
          >
            <DateRange color="secondary" fontSize="small" />
            <Box>
              <Typography 
                variant="body2" 
                color="text.secondary"
              >
                This Week
              </Typography>
              <Typography 
                variant="h6"
                color="secondary"
                aria-label={`Points earned this week: ${points.weekEarned}`}
                sx={{ fontWeight: 'medium' }}
              >
                {formatPoints(points.weekEarned)}
              </Typography>
            </Box>
          </Box>
        </AnimatePresence>
      )}
    </Card>
  );
};

export default React.memo(PointsDisplay);