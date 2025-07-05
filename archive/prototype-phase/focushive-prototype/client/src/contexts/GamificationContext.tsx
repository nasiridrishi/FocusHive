import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useSocket } from './SocketContext';
import { useAuth } from './AuthContext';

interface UserStats {
  totalPoints: number;
  totalFocusTime: number;
  currentStreak: number;
  longestStreak: number;
  todayFocusTime: number;
  todayPoints: number;
  rank: number;
}

interface Achievement {
  id: string;
  name: string;
  description: string;
  icon: string;
  points: number;
  category: 'focus' | 'streak' | 'social';
}

interface LeaderboardEntry {
  userId: string;
  username: string;
  avatar: string;
  focusTime: number;
  points: number;
  streak: number;
  rank: number;
}

interface GamificationContextType {
  stats: UserStats | null;
  achievements: Achievement[];
  leaderboard: LeaderboardEntry[];
  leaderboardType: 'daily' | 'weekly' | 'monthly' | 'allTime';
  refreshStats: () => void;
  refreshAchievements: () => void;
  refreshLeaderboard: (type?: 'daily' | 'weekly' | 'monthly' | 'allTime') => void;
  newAchievement: Achievement | null;
  clearNewAchievement: () => void;
}

const GamificationContext = createContext<GamificationContextType | null>(null);

export const useGamification = () => {
  const context = useContext(GamificationContext);
  if (!context) {
    throw new Error('useGamification must be used within a GamificationProvider');
  }
  return context;
};

export const GamificationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { socket, authenticated } = useSocket();
  const { user } = useAuth();
  const [stats, setStats] = useState<UserStats | null>(null);
  const [achievements, setAchievements] = useState<Achievement[]>([]);
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([]);
  const [leaderboardType, setLeaderboardType] = useState<'daily' | 'weekly' | 'monthly' | 'allTime'>('daily');
  const [newAchievement, setNewAchievement] = useState<Achievement | null>(null);

  const refreshStats = useCallback(() => {
    if (socket && user) {
      socket.emit('gamification:get-stats');
    }
  }, [socket, user]);

  const refreshAchievements = useCallback(() => {
    if (socket && user) {
      socket.emit('gamification:get-achievements');
    }
  }, [socket, user]);

  const refreshLeaderboard = useCallback((type: 'daily' | 'weekly' | 'monthly' | 'allTime' = 'daily') => {
    if (socket) {
      setLeaderboardType(type);
      socket.emit('gamification:get-leaderboard', { type, limit: 10 });
    }
  }, [socket]);

  const clearNewAchievement = useCallback(() => {
    setNewAchievement(null);
  }, []);

  useEffect(() => {
    if (!socket || !user || !authenticated) return;

    // Socket event handlers
    const handleStats = ({ stats }: { stats: UserStats }) => {
      setStats(stats);
    };

    const handleAchievements = ({ achievements }: { achievements: Achievement[] }) => {
      setAchievements(achievements);
    };

    const handleLeaderboard = ({ leaderboard }: { leaderboard: LeaderboardEntry[] }) => {
      setLeaderboard(leaderboard);
    };

    const handleStatsUpdated = (update: any) => {
      // Update local stats optimistically
      if (stats) {
        setStats({
          ...stats,
          totalPoints: update.newTotalPoints || stats.totalPoints,
          currentStreak: update.newStreak || stats.currentStreak,
          todayPoints: stats.todayPoints + (update.pointsEarned || 0),
          todayFocusTime: stats.todayFocusTime + (update.sessionTime || 0)
        });
      }
    };

    const handleAchievementEarned = ({ achievement }: { achievement: Achievement }) => {
      setNewAchievement(achievement);
      setAchievements(prev => [...prev, achievement]);
      
      // Show notification
      if ('Notification' in window && Notification.permission === 'granted') {
        new Notification('Achievement Unlocked!', {
          body: `${achievement.icon} ${achievement.name}: ${achievement.description}`,
          icon: '/favicon.ico'
        });
      }
    };

    const handleError = ({ message }: { message: string }) => {
      console.error('Gamification error:', message);
    };

    // Register listeners
    socket.on('gamification:stats', handleStats);
    socket.on('gamification:achievements', handleAchievements);
    socket.on('gamification:leaderboard', handleLeaderboard);
    socket.on('gamification:stats-updated', handleStatsUpdated);
    socket.on('gamification:achievement-earned', handleAchievementEarned);
    socket.on('gamification:error', handleError);

    // Initial data fetch
    refreshStats();
    refreshAchievements();
    refreshLeaderboard('daily');

    // Cleanup
    return () => {
      socket.off('gamification:stats', handleStats);
      socket.off('gamification:achievements', handleAchievements);
      socket.off('gamification:leaderboard', handleLeaderboard);
      socket.off('gamification:stats-updated', handleStatsUpdated);
      socket.off('gamification:achievement-earned', handleAchievementEarned);
      socket.off('gamification:error', handleError);
    };
  }, [socket, user, authenticated, refreshStats, refreshAchievements, refreshLeaderboard, stats]);

  return (
    <GamificationContext.Provider value={{
      stats,
      achievements,
      leaderboard,
      leaderboardType,
      refreshStats,
      refreshAchievements,
      refreshLeaderboard,
      newAchievement,
      clearNewAchievement
    }}>
      {children}
    </GamificationContext.Provider>
  );
};