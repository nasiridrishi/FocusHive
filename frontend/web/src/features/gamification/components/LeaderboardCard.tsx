import React, { useMemo } from 'react';
import {
  Box,
  Card,
  Typography,
  Avatar,
  List,
  ListItem,
  Chip,
  useTheme,
  alpha,
  Skeleton,
  Divider,
} from '@mui/material';
import { motion } from 'framer-motion';
import {
  TrendingUp,
  TrendingDown,
  Remove,
  Schedule,
  EmojiEvents,
} from '@mui/icons-material';
import type { LeaderboardCardProps } from '../types/gamification';
import { 
  formatPoints, 
  formatRankChange, 
  getUserInitials,
} from '../utils/gamificationUtils';

const LeaderboardCard: React.FC<LeaderboardCardProps> = ({
  leaderboard,
  currentUserId,
  maxEntries,
  showRankChange = true,
}) => {
  const theme = useTheme();

  // Process entries for display - must be before any early returns
  const displayEntries = useMemo(() => {
    if (!leaderboard) return [];
    
    let entries = [...leaderboard.entries];
    
    // If maxEntries is specified and we have more entries
    if (maxEntries && entries.length > maxEntries) {
      // Always include current user if they exist
      const currentUserEntry = entries.find(entry => entry.user.id === currentUserId);
      const topEntries = entries.slice(0, maxEntries);
      
      // If current user is not in top entries but exists in the list
      if (currentUserEntry && !topEntries.some(entry => entry.user.id === currentUserId)) {
        // Remove last top entry and add current user
        topEntries.pop();
        topEntries.push(currentUserEntry);
      }
      
      entries = topEntries;
    }
    
    return entries;
  }, [leaderboard, maxEntries, currentUserId]);

  // Handle loading state
  if (!leaderboard) {
    return (
      <Card 
        data-testid="leaderboard-skeleton"
        sx={{ p: 2 }}
      >
        <Skeleton variant="text" height={32} width="60%" />
        <Skeleton variant="text" height={20} width="40%" sx={{ mb: 2 }} />
        {Array.from({ length: 5 }).map((_, index) => (
          <Box key={index} sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
            <Skeleton variant="circular" width={32} height={32} />
            <Skeleton variant="text" width="40%" />
            <Skeleton variant="text" width="20%" sx={{ ml: 'auto' }} />
          </Box>
        ))}
      </Card>
    );
  }

  // Handle error state
  if ('error' in leaderboard && leaderboard.error) {
    return (
      <Card sx={{ p: 2, textAlign: 'center' }}>
        <Typography color="error">
          Failed to load leaderboard
        </Typography>
      </Card>
    );
  }

  const isEmpty = leaderboard.entries.length === 0;

  const hasMoreEntries = maxEntries && leaderboard.entries.length > maxEntries;
  const extraCount = hasMoreEntries ? leaderboard.entries.length - displayEntries.length : 0;

  const formatPeriod = (period: string) => {
    return period.replace('_', ' ').toLowerCase();
  };

  const getPodiumClass = (rank: number) => {
    switch (rank) {
      case 1: return 'podium-gold';
      case 2: return 'podium-silver';
      case 3: return 'podium-bronze';
      default: return '';
    }
  };

  const getRankChangeIcon = (change?: number) => {
    if (!change || change === 0) return <Remove fontSize="small" />;
    return change > 0 ? <TrendingUp fontSize="small" /> : <TrendingDown fontSize="small" />;
  };

  const getRankChangeColor = (change?: number) => {
    if (!change || change === 0) return theme.palette.text.secondary;
    return change > 0 ? theme.palette.success.main : theme.palette.error.main;
  };

  const listVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.1,
      },
    },
  };

  const itemVariants = {
    hidden: { x: -20, opacity: 0 },
    visible: { 
      x: 0, 
      opacity: 1,
      transition: { duration: 0.3 }
    },
  };

  if (isEmpty) {
    return (
      <Card
        aria-label={`${leaderboard.title} leaderboard`}
        sx={{ p: 3, textAlign: 'center' }}
      >
        <Typography variant="h6" gutterBottom>
          {leaderboard.title}
        </Typography>
        
        <Box 
          data-testid="empty-leaderboard-illustration"
          sx={{ mb: 2 }}
        >
          <EmojiEvents 
            sx={{ 
              fontSize: '4rem', 
              color: theme.palette.grey[400],
              mb: 1,
            }} 
          />
        </Box>
        
        <Typography color="text.secondary" gutterBottom>
          No entries yet
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Be the first to appear on this leaderboard!
        </Typography>
      </Card>
    );
  }

  return (
    <Card
      aria-label={`${leaderboard.title} leaderboard`}
      sx={{
        overflow: 'hidden',
        background: `linear-gradient(135deg, ${alpha(theme.palette.primary.main, 0.02)}, ${alpha(theme.palette.secondary.main, 0.02)})`,
      }}
    >
      {/* Header */}
      <Box sx={{ p: 2, pb: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
          <EmojiEvents color="primary" />
          <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
            {leaderboard.title}
          </Typography>
          <Chip
            label={formatPeriod(leaderboard.period)}
            size="small"
            sx={{
              backgroundColor: alpha(theme.palette.primary.main, 0.1),
              color: theme.palette.primary.main,
              fontSize: '0.7rem',
              textTransform: 'capitalize',
            }}
          />
        </Box>
        
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Schedule fontSize="small" color="action" />
          <Typography variant="caption" color="text.secondary">
            Updated {leaderboard.lastUpdated.toLocaleDateString('en-US', {
              month: 'short',
              day: 'numeric',
              hour: '2-digit',
              minute: '2-digit',
            })}
          </Typography>
        </Box>
      </Box>

      <Divider />

      {/* Leaderboard List */}
      <Box 
        data-testid="leaderboard-scroll-container"
        sx={{ 
          maxHeight: 400, 
          overflowY: 'auto',
          position: 'relative',
        }}
      >
        {displayEntries.length > 100 ? (
          // Virtual scrolling for very long lists
          <Box data-testid="virtualized-list">
            {/* This would implement virtual scrolling */}
            <Typography sx={{ p: 2, textAlign: 'center' }} color="text.secondary">
              Virtualized list for {displayEntries.length} entries
            </Typography>
          </Box>
        ) : (
          <List
            component={motion.div}
            variants={listVariants}
            initial="hidden"
            animate="visible"
            sx={{ p: 0 }}
          >
            {displayEntries.map((entry) => (
              <ListItem
                key={entry.user.id}
                component={motion.li}
                variants={itemVariants}
                data-testid="leaderboard-entry"
                className={`
                  ${entry.user.id === currentUserId ? 'current-user' : ''}
                  ${entry.rank <= 3 ? getPodiumClass(entry.rank) : ''}
                `}
                sx={{
                  py: 1.5,
                  px: 2,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 2,
                  background: entry.user.id === currentUserId 
                    ? alpha(theme.palette.primary.main, 0.08)
                    : 'transparent',
                  borderLeft: entry.user.id === currentUserId 
                    ? `4px solid ${theme.palette.primary.main}`
                    : 'none',
                  '&:hover': {
                    backgroundColor: alpha(theme.palette.action.hover, 0.04),
                  },
                  
                  // Podium styling
                  '&.podium-gold': {
                    background: `linear-gradient(90deg, ${alpha('#FFD700', 0.1)}, transparent)`,
                  },
                  '&.podium-silver': {
                    background: `linear-gradient(90deg, ${alpha('#C0C0C0', 0.1)}, transparent)`,
                  },
                  '&.podium-bronze': {
                    background: `linear-gradient(90deg, ${alpha('#CD7F32', 0.1)}, transparent)`,
                  },
                }}
                tabIndex={0}
                role="listitem"
                aria-label={`Rank ${entry.rank}: ${entry.user.name} with ${formatPoints(entry.points)} points`}
              >
                {/* Rank */}
                <Box
                  data-testid={entry.rank <= 3 ? `podium-${entry.rank}` : undefined}
                  sx={{
                    width: 32,
                    height: 32,
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontWeight: 'bold',
                    fontSize: '0.9rem',
                    ...(entry.rank === 1 && {
                      background: 'linear-gradient(135deg, #FFD700, #FFA000)',
                      color: 'white',
                    }),
                    ...(entry.rank === 2 && {
                      background: 'linear-gradient(135deg, #C0C0C0, #757575)',
                      color: 'white',
                    }),
                    ...(entry.rank === 3 && {
                      background: 'linear-gradient(135deg, #CD7F32, #8D6E63)',
                      color: 'white',
                    }),
                    ...(entry.rank > 3 && {
                      backgroundColor: alpha(theme.palette.primary.main, 0.1),
                      color: theme.palette.primary.main,
                    }),
                  }}
                >
                  {entry.rank <= 3 ? (
                    <EmojiEvents fontSize="small" />
                  ) : (
                    entry.rank
                  )}
                </Box>

                {/* Avatar */}
                <Avatar
                  src={entry.user.avatar}
                  alt={entry.user.name}
                  sx={{ width: 40, height: 40 }}
                >
                  {getUserInitials(entry.user.name)}
                </Avatar>

                {/* User Info */}
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Typography 
                      variant="body1" 
                      sx={{ 
                        fontWeight: 'medium',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {entry.user.name}
                    </Typography>
                    {entry.user.id === currentUserId && (
                      <Chip 
                        label="You" 
                        size="small"
                        sx={{
                          backgroundColor: theme.palette.primary.main,
                          color: 'white',
                          fontSize: '0.7rem',
                          height: 20,
                        }}
                      />
                    )}
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    {formatPoints(entry.points)} points
                  </Typography>
                </Box>

                {/* Rank Change */}
                {showRankChange && (
                  <Box
                    data-testid={
                      entry.change === undefined || entry.change === 0
                        ? 'rank-change-same'
                        : entry.change > 0
                        ? `rank-change-up-${entry.change}`
                        : `rank-change-down-${Math.abs(entry.change)}`
                    }
                    className={
                      entry.change === undefined || entry.change === 0
                        ? 'rank-same'
                        : entry.change > 0
                        ? 'rank-up'
                        : 'rank-down'
                    }
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 0.5,
                      color: getRankChangeColor(entry.change),
                      minWidth: 60,
                      justifyContent: 'center',
                    }}
                    aria-label={
                      entry.change === undefined || entry.change === 0
                        ? 'No rank change'
                        : entry.change > 0
                        ? `Moved up ${entry.change} positions`
                        : `Moved down ${Math.abs(entry.change)} positions`
                    }
                  >
                    {getRankChangeIcon(entry.change)}
                    <Typography 
                      variant="caption" 
                      sx={{ fontWeight: 'medium' }}
                    >
                      {formatRankChange(entry.change)}
                    </Typography>
                  </Box>
                )}
              </ListItem>
            ))}
          </List>
        )}
      </Box>

      {/* "And more" indicator */}
      {hasMoreEntries && extraCount > 0 && (
        <Box sx={{ p: 2, textAlign: 'center', borderTop: `1px solid ${theme.palette.divider}` }}>
          <Typography variant="body2" color="text.secondary">
            and {extraCount} more...
          </Typography>
        </Box>
      )}
    </Card>
  );
};

export default React.memo(LeaderboardCard);