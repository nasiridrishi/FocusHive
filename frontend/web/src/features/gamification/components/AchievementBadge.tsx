import React from 'react';
import {
  Box,
  Card,
  Typography,
  Chip,
  LinearProgress,
  useTheme,
  alpha,
} from '@mui/material';
import { motion } from 'framer-motion';
import {
  Star,
  Lock,
  KeyboardArrowRight,
} from '@mui/icons-material';
import type { AchievementBadgeProps } from '../types/gamification';
import {
  formatPoints,
  getRarityColor,
  getCategoryIcon,
  getAchievementProgress,
  isAchievementUnlocked,
} from '../utils/gamificationUtils';

const AchievementBadge: React.FC<AchievementBadgeProps> = ({
  achievement,
  size = 'medium',
  showProgress = true,
  onClick,
}) => {
  const theme = useTheme();
  const isUnlocked = isAchievementUnlocked(achievement);
  const progress = getAchievementProgress(achievement);
  const rarityColor = getRarityColor(achievement.rarity);
  const categoryIcon = getCategoryIcon(achievement.category);
  const isClickable = Boolean(onClick);
  
  // Check if recently unlocked (within last 24 hours)
  const isRecentlyUnlocked = isUnlocked && achievement.unlockedAt && 
    (Date.now() - achievement.unlockedAt.getTime()) < 24 * 60 * 60 * 1000;

  const getSizeStyles = () => {
    switch (size) {
      case 'small':
        return {
          width: 200,
          height: 120,
          iconSize: 32,
          titleVariant: 'body2' as const,
          descVariant: 'caption' as const,
        };
      case 'large':
        return {
          width: 320,
          height: 200,
          iconSize: 64,
          titleVariant: 'h6' as const,
          descVariant: 'body2' as const,
        };
      default:
        return {
          width: 250,
          height: 160,
          iconSize: 48,
          titleVariant: 'body1' as const,
          descVariant: 'body2' as const,
        };
    }
  };

  const sizeStyles = getSizeStyles();

  const cardVariants = {
    initial: { scale: 0.9, opacity: 0, rotateY: -180 },
    animate: { 
      scale: 1, 
      opacity: 1, 
      rotateY: 0,
      transition: { 
        duration: 0.6, 
        ease: 'easeOut',
        rotateY: { duration: 0.8 }
      }
    },
    hover: {
      scale: isClickable ? 1.05 : 1,
      y: isClickable ? -4 : 0,
      transition: { duration: 0.2 }
    },
    tap: { scale: 0.98 }
  };

  const iconVariants = {
    locked: { 
      scale: 0.8, 
      opacity: 0.5,
      filter: 'grayscale(100%)'
    },
    unlocked: { 
      scale: 1, 
      opacity: 1,
      filter: 'grayscale(0%)',
      transition: { duration: 0.3 }
    },
    recentlyUnlocked: {
      scale: [1, 1.2, 1],
      rotate: [0, 360],
      transition: { 
        duration: 1,
        repeat: Infinity,
        repeatDelay: 3
      }
    }
  };

  const handleClick = () => {
    if (onClick) {
      onClick();
    }
  };

  const handleKeyDown = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter' && onClick) {
      onClick();
    }
  };

  return (
    <Card
      component={motion.div}
      variants={cardVariants}
      initial="initial"
      animate="animate"
      whileHover="hover"
      whileTap={isClickable ? "tap" : undefined}
      data-testid="achievement-badge"
      className={`
        ${isUnlocked ? 'unlocked' : 'locked'}
        ${`size-${size}`}
        ${`rarity-${achievement.rarity}`}
        ${achievement.rarity === 'legendary' ? 'legendary-glow' : ''}
        ${isRecentlyUnlocked ? 'recently-unlocked' : ''}
        ${isClickable ? 'hover-effect' : ''}
      `}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      tabIndex={isClickable ? 0 : -1}
      role={isClickable ? 'button' : undefined}
      aria-label={`Achievement: ${achievement.title}`}
      sx={{
        width: sizeStyles.width,
        height: sizeStyles.height,
        cursor: isClickable ? 'pointer' : 'default',
        position: 'relative',
        background: isUnlocked 
          ? `linear-gradient(135deg, ${alpha(rarityColor, 0.1)}, ${alpha(rarityColor, 0.2)})`
          : `linear-gradient(135deg, ${alpha(theme.palette.grey[500], 0.1)}, ${alpha(theme.palette.grey[600], 0.2)})`,
        border: `2px solid ${isUnlocked ? rarityColor : theme.palette.grey[400]}`,
        borderRadius: 3,
        padding: 2,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        opacity: isUnlocked ? 1 : 0.7,
        overflow: 'hidden',
        
        // Rarity-specific styling
        '&.rarity-legendary': {
          boxShadow: `0 0 20px ${alpha(rarityColor, 0.5)}`,
          '&.legendary-glow::before': {
            content: '""',
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: `linear-gradient(45deg, transparent 30%, ${alpha(rarityColor, 0.1)} 50%, transparent 70%)`,
            animation: 'shimmer 3s infinite',
          },
        },
        
        '&.recently-unlocked': {
          '&::after': {
            content: '"✨"',
            position: 'absolute',
            top: 8,
            right: 8,
            fontSize: '1.2em',
            animation: 'sparkle 2s infinite',
          },
        },
        
        '@keyframes shimmer': {
          '0%': { transform: 'translateX(-100%)' },
          '100%': { transform: 'translateX(100%)' },
        },
        
        '@keyframes sparkle': {
          '0%, 100%': { opacity: 1, transform: 'scale(1)' },
          '50%': { opacity: 0.5, transform: 'scale(1.2)' },
        },
      }}
    >
      {/* Rarity Indicator */}
      <Box sx={{ position: 'absolute', top: 8, left: 8 }}>
        <Chip
          label={achievement.rarity.toUpperCase()}
          size="small"
          sx={{
            backgroundColor: rarityColor,
            color: 'white',
            fontSize: '0.7rem',
            height: 20,
            fontWeight: 'bold',
          }}
        />
      </Box>

      {/* Lock Status */}
      {!isUnlocked && (
        <Box sx={{ position: 'absolute', top: 8, right: 8 }}>
          <Lock 
            sx={{ 
              color: theme.palette.grey[500],
              fontSize: '1rem',
            }} 
          />
          <Typography 
            variant="caption" 
            sx={{ 
              position: 'absolute',
              top: 20,
              right: -8,
              color: theme.palette.grey[500],
              fontSize: '0.6rem',
            }}
          >
            LOCKED
          </Typography>
        </Box>
      )}

      {/* Achievement Icon */}
      <Box 
        sx={{ 
          display: 'flex', 
          alignItems: 'center', 
          gap: 1,
          mb: 1,
        }}
      >
        <Box
          component={motion.div}
          variants={iconVariants}
          animate={isRecentlyUnlocked ? 'recentlyUnlocked' : (isUnlocked ? 'unlocked' : 'locked')}
          sx={{
            width: sizeStyles.iconSize,
            height: sizeStyles.iconSize,
            borderRadius: '50%',
            background: isUnlocked 
              ? `linear-gradient(135deg, ${rarityColor}, ${alpha(rarityColor, 0.7)})`
              : theme.palette.grey[400],
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'white',
            fontSize: sizeStyles.iconSize * 0.6,
          }}
        >
          <img
            src={`/icons/achievements/${achievement.icon}.svg`}
            alt={achievement.title}
            data-icon={achievement.icon}
            style={{ 
              width: '70%', 
              height: '70%',
              filter: isUnlocked ? 'none' : 'grayscale(100%)',
            }}
            onError={(e) => {
              // Fallback to emoji if SVG fails to load
              const target = e.target as HTMLImageElement;
              target.style.display = 'none';
              const parent = target.parentElement;
              if (parent) {
                parent.textContent = categoryIcon;
              }
            }}
          />
        </Box>

        {/* Category Badge */}
        <Chip
          label={achievement.category.toUpperCase()}
          size="small"
          className={`category-${achievement.category}`}
          sx={{
            backgroundColor: alpha(theme.palette.primary.main, 0.1),
            color: theme.palette.primary.main,
            fontSize: '0.6rem',
            height: 18,
          }}
        />
      </Box>

      {/* Title and Description */}
      <Box sx={{ flex: 1, mb: 1 }}>
        <Typography
          variant={sizeStyles.titleVariant}
          sx={{
            fontWeight: 'bold',
            color: isUnlocked ? theme.palette.text.primary : theme.palette.text.secondary,
            mb: 0.5,
            lineHeight: 1.2,
          }}
        >
          {achievement.title}
        </Typography>
        
        {achievement.description && (
          <Typography
            variant={sizeStyles.descVariant}
            sx={{
              color: theme.palette.text.secondary,
              opacity: isUnlocked ? 1 : 0.7,
              lineHeight: 1.3,
              display: '-webkit-box',
              WebkitLineClamp: size === 'small' ? 2 : 3,
              WebkitBoxOrient: 'vertical',
              overflow: 'hidden',
            }}
          >
            {achievement.description}
          </Typography>
        )}
      </Box>

      {/* Progress Bar */}
      {showProgress && achievement.progress !== undefined && achievement.maxProgress && !isUnlocked && (
        <Box sx={{ mb: 1 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
            <Typography variant="caption" color="text.secondary">
              Progress
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {achievement.progress}/{achievement.maxProgress}
            </Typography>
          </Box>
          <LinearProgress
            variant="determinate"
            value={progress}
            role="progressbar"
            aria-valuenow={achievement.progress}
            aria-valuemax={achievement.maxProgress}
            sx={{
              height: 6,
              borderRadius: 3,
              backgroundColor: alpha(theme.palette.grey[500], 0.2),
              '& .MuiLinearProgress-bar': {
                backgroundColor: rarityColor,
                borderRadius: 3,
              },
            }}
          />
        </Box>
      )}

      {/* Points and Unlock Date */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
          <Star sx={{ color: theme.palette.warning.main, fontSize: '1rem' }} />
          <Typography 
            variant="caption" 
            sx={{ fontWeight: 'bold' }}
            aria-label={`${achievement.points} points`}
          >
            {formatPoints(achievement.points)}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            points
          </Typography>
        </Box>
        
        {isUnlocked && achievement.unlockedAt && (
          <Typography 
            variant="caption" 
            color="text.secondary"
            sx={{ fontSize: '0.7rem' }}
          >
            Unlocked {achievement.unlockedAt.toLocaleDateString('en-US', { 
              month: 'short', 
              day: 'numeric',
              year: 'numeric'
            })}
          </Typography>
        )}
        
        {isClickable && (
          <KeyboardArrowRight 
            sx={{ 
              color: theme.palette.text.secondary,
              fontSize: '1.2rem',
            }} 
          />
        )}
      </Box>
    </Card>
  );
};

export default React.memo(AchievementBadge);