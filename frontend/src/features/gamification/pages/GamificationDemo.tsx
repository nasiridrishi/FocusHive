import React from 'react';
import {
  Container,
  Typography,
  Box,
  Button,
  Card,
  CardContent,
  Divider,
} from '@mui/material';
import {
  SportsEsports as GamingIcon,
  Assessment as ChartIcon,
  EmojiEvents as TrophyIcon,
  LocalFireDepartment as FireIcon,
  SportsEsports as InteractiveIcon,
} from '@mui/icons-material';
import {
  PointsDisplay,
  AchievementBadge,
  StreakCounter,
  LeaderboardCard,
} from '../components';
import { GamificationProvider, useGamification } from '../contexts';
import type { Achievement, Streak, Leaderboard } from '../types';

// Mock data for demo
const mockAchievements: Achievement[] = [
  {
    id: 'first-focus',
    title: 'First Focus',
    description: 'Complete your first focus session',
    icon: 'focus',
    category: 'focus',
    points: 100,
    unlockedAt: new Date('2024-01-15T10:30:00Z'),
    isUnlocked: true,
    rarity: 'common',
  },
  {
    id: 'focus-master',
    title: 'Focus Master',
    description: 'Complete 100 focus sessions',
    icon: 'master',
    category: 'focus',
    points: 1000,
    progress: 45,
    maxProgress: 100,
    isUnlocked: false,
    rarity: 'epic',
  },
  {
    id: 'collaborator',
    title: 'Team Player',
    description: 'Join 5 different hives',
    icon: 'team',
    category: 'collaboration',
    points: 200,
    progress: 3,
    maxProgress: 5,
    isUnlocked: false,
    rarity: 'uncommon',
  },
  {
    id: 'legend',
    title: 'FocusHive Legend',
    description: 'Achieve the impossible',
    icon: 'legend',
    category: 'special',
    points: 10000,
    isUnlocked: false,
    rarity: 'legendary',
  },
];

const mockStreaks: Streak[] = [
  {
    id: 'daily-login-1',
    type: 'daily_login',
    current: 7,
    best: 15,
    lastActivity: new Date('2024-01-15T10:30:00Z'),
    isActive: true,
  },
  {
    id: 'focus-session-1',
    type: 'focus_session',
    current: 0,
    best: 25,
    lastActivity: new Date('2024-01-10T10:30:00Z'),
    isActive: false,
  },
  {
    id: 'hive-participation-1',
    type: 'hive_participation',
    current: 3,
    best: 10,
    lastActivity: new Date(),
    isActive: true,
  },
];

const mockLeaderboard: Leaderboard = {
  id: 'weekly-points',
  title: 'Weekly Points',
  period: 'weekly',
  lastUpdated: new Date('2024-01-15T10:30:00Z'),
  entries: [
    { 
      user: { id: '1', name: 'Alice Johnson', avatar: '/avatars/alice.jpg' }, 
      points: 2500, 
      rank: 1, 
      change: 2 
    },
    { 
      user: { id: '2', name: 'Bob Smith', avatar: '/avatars/bob.jpg' }, 
      points: 2200, 
      rank: 2, 
      change: -1 
    },
    { 
      user: { id: '3', name: 'Carol Brown' }, 
      points: 1800, 
      rank: 3, 
      change: 0 
    },
    { 
      user: { id: '4', name: 'David Wilson' }, 
      points: 1500, 
      rank: 4, 
      change: 1 
    },
    { 
      user: { id: '5', name: 'Eve Davis' }, 
      points: 1200, 
      rank: 5, 
      change: -2 
    },
  ],
};

const DemoContent: React.FC = () => {
  const { stats, loading, addPoints, unlockAchievement, updateStreak } = useGamification();

  const handleAddPoints = async () => {
    try {
      await addPoints(50, 'demo-action');
    } catch (error) {
      console.error('Error:', error);
    }
  };

  const handleUnlockAchievement = async () => {
    try {
      await unlockAchievement('focus-master');
    } catch (error) {
      console.error('Error:', error);
    }
  };

  const handleUpdateStreak = async () => {
    try {
      await updateStreak('daily_login');
    } catch (error) {
      console.error('Error:', error);
    }
  };

  if (loading) {
    return (
      <Box sx={{ textAlign: 'center', p: 4 }}>
        <Typography variant="h6">Loading gamification data...</Typography>
      </Box>
    );
  }

  return (
    <Container maxWidth="desktop" sx={{ py: 4 }}>
      <Typography variant="h3" component="h1" gutterBottom align="center" sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 2 }}>
        <GamingIcon sx={{ fontSize: '2.5rem' }} />
        FocusHive Gamification System
      </Typography>
      
      <Typography variant="body1" align="center" color="text.secondary" paragraph>
        A comprehensive gamification system featuring points, achievements, streaks, and leaderboards
      </Typography>

      <Divider sx={{ my: 4 }} />

      {/* Points Display Section */}
      <Box sx={{ mb: 6 }}>
        <Typography variant="h4" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <ChartIcon />
          Points Display
        </Typography>
        <Typography variant="body2" color="text.secondary" paragraph>
          Dynamic points display with animations and responsive design
        </Typography>
        
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { 
              mobile: '1fr', 
              tablet: '1fr 1fr', 
              desktop: '2fr 1fr 1fr' 
            },
            gap: 3
          }}
        >
          <PointsDisplay 
            points={stats?.points || { current: 1250, total: 15750, todayEarned: 150, weekEarned: 420 }}
            showToday
            showWeek
            size="medium"
          />
          <PointsDisplay 
            points={stats?.points || { current: 850, total: 5200, todayEarned: 75, weekEarned: 200 }}
            size="small"
          />
          <Button 
            variant="contained" 
            onClick={handleAddPoints}
            sx={{ height: '100%', minHeight: 120 }}
            fullWidth
          >
            Add 50 Points
          </Button>
        </Box>
      </Box>

      {/* Achievement Badges Section */}
      <Box sx={{ mb: 6 }}>
        <Typography variant="h4" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <TrophyIcon />
          Achievement Badges
        </Typography>
        <Typography variant="body2" color="text.secondary" paragraph>
          Interactive achievement badges with progress tracking and rarity indicators
        </Typography>
        
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { 
              mobile: '1fr', 
              tablet: '1fr 1fr', 
              desktop: 'repeat(3, 1fr)' 
            },
            gap: 3
          }}
        >
          {mockAchievements.map((achievement) => (
            <AchievementBadge
              key={achievement.id}
              achievement={achievement}
              showProgress
              onClick={() => {}}
            />
          ))}
        </Box>
        
        <Box sx={{ mt: 2, textAlign: 'center' }}>
          <Button 
            variant="outlined" 
            onClick={handleUnlockAchievement}
          >
            Unlock Focus Master
          </Button>
        </Box>
      </Box>

      {/* Streak Counters Section */}
      <Box sx={{ mb: 6 }}>
        <Typography variant="h4" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <FireIcon />
          Streak Counters
        </Typography>
        <Typography variant="body2" color="text.secondary" paragraph>
          Track user consistency with animated streak counters
        </Typography>
        
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { 
              mobile: '1fr', 
              tablet: '1fr', 
              desktop: 'repeat(3, 1fr)' 
            },
            gap: 3
          }}
        >
          {mockStreaks.map((streak) => (
            <StreakCounter 
              key={streak.id}
              streak={streak} 
              variant="default"
              showBest
            />
          ))}
        </Box>
        
        <Box sx={{ mt: 3 }}>
          <Typography variant="h6" gutterBottom>
            Different Variants
          </Typography>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { 
                mobile: '1fr', 
                tablet: '1fr', 
                desktop: '1fr 2fr' 
              },
              gap: 2
            }}
          >
            <StreakCounter 
              streak={mockStreaks[0]} 
              variant="compact"
            />
            <StreakCounter 
              streak={mockStreaks[0]} 
              variant="detailed"
            />
          </Box>
        </Box>
        
        <Box sx={{ mt: 2, textAlign: 'center' }}>
          <Button 
            variant="outlined" 
            onClick={handleUpdateStreak}
          >
            Update Daily Login Streak
          </Button>
        </Box>
      </Box>

      {/* Leaderboard Section */}
      <Box sx={{ mb: 6 }}>
        <Typography variant="h4" gutterBottom>
          🥇 Leaderboard
        </Typography>
        <Typography variant="body2" color="text.secondary" paragraph>
          Competitive leaderboards with rank changes and user highlighting
        </Typography>
        
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { 
              mobile: '1fr', 
              tablet: '1fr', 
              desktop: '1fr 1fr' 
            },
            gap: 3
          }}
        >
          <LeaderboardCard 
            leaderboard={mockLeaderboard}
            currentUserId="3"
            showRankChange
          />
          <LeaderboardCard 
            leaderboard={{
              ...mockLeaderboard,
              title: 'Top 3 Only',
            }}
            maxEntries={3}
            showRankChange={false}
          />
        </Box>
      </Box>

      {/* Interactive Demo Section */}
      <Card sx={{ mb: 4 }}>
        <CardContent>
          <Typography variant="h5" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <InteractiveIcon />
            Interactive Demo
          </Typography>
          <Typography variant="body2" color="text.secondary" paragraph>
            Test the gamification system with live data from the context
          </Typography>
          
          {stats && (
            <Box sx={{ mt: 2 }}>
              <Typography variant="body1" paragraph>
                <strong>Current Level:</strong> {stats.level}
              </Typography>
              <Typography variant="body1" paragraph>
                <strong>Rank:</strong> {stats.rank} of {stats.totalUsers}
              </Typography>
              <Typography variant="body1" paragraph>
                <strong>Achievements Unlocked:</strong> {stats.achievements.filter(a => a.isUnlocked).length} of {stats.achievements.length}
              </Typography>
              <Typography variant="body1" paragraph>
                <strong>Active Streaks:</strong> {stats.streaks.filter(s => s.isActive).length} of {stats.streaks.length}
              </Typography>
            </Box>
          )}
          
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mt: 3 }}>
            <Button variant="contained" onClick={handleAddPoints}>
              Earn Points
            </Button>
            <Button variant="contained" onClick={handleUnlockAchievement}>
              Unlock Achievement
            </Button>
            <Button variant="contained" onClick={handleUpdateStreak}>
              Continue Streak
            </Button>
          </Box>
        </CardContent>
      </Card>

      {/* System Information */}
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            📋 System Information
          </Typography>
          <Typography variant="body2" paragraph>
            This gamification system includes:
          </Typography>
          <ul>
            <li><strong>Points System:</strong> Earn and track points with animated displays</li>
            <li><strong>Achievement System:</strong> Unlock badges with different rarities and progress tracking</li>
            <li><strong>Streak System:</strong> Maintain consistency with visual streak counters</li>
            <li><strong>Leaderboard System:</strong> Compete with others and track rank changes</li>
            <li><strong>Real-time Updates:</strong> Live data synchronization via WebSocket</li>
            <li><strong>Responsive Design:</strong> Works on mobile, tablet, and desktop</li>
            <li><strong>Accessibility:</strong> Full ARIA support and keyboard navigation</li>
            <li><strong>Animations:</strong> Smooth transitions and micro-interactions</li>
          </ul>
        </CardContent>
      </Card>
    </Container>
  );
};

const GamificationDemo: React.FC = () => {
  return (
    <GamificationProvider>
      <DemoContent />
    </GamificationProvider>
  );
};

export default GamificationDemo;