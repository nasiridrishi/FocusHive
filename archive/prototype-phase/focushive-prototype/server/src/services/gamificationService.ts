import { dataStore } from '../data/store';
import { gamificationStore } from '../data/gamificationStore';

export interface AchievementTrigger {
  type: 'session_complete' | 'streak_updated' | 'room_joined' | 'buddy_matched';
  userId: string;
  duration?: number;
  streak?: number;
  roomId?: string;
}

export interface Achievement {
  id: string;
  name: string;
  description: string;
  icon: string;
  points: number;
  category: 'focus' | 'streak' | 'social';
}

export interface StreakInfo {
  current: number;
  longest: number;
  isActive: boolean;
  nextMilestone: number;
}

export interface SessionCompleteResult {
  pointsEarned: number;
  newStreak: number;
  achievements: Achievement[];
}

const ACHIEVEMENTS: Record<string, Achievement> = {
  first_focus: {
    id: 'first_focus',
    name: 'First Focus',
    description: 'Complete your first 25-minute focus session',
    icon: '🎯',
    points: 10,
    category: 'focus'
  },
  hour_power: {
    id: 'hour_power',
    name: 'Hour Power',
    description: 'Complete 60 minutes of focus in a single day',
    icon: '⚡',
    points: 20,
    category: 'focus'
  },
  deep_diver: {
    id: 'deep_diver',
    name: 'Deep Diver',
    description: 'Complete a 4-hour deep work session',
    icon: '🌊',
    points: 50,
    category: 'focus'
  },
  centurion: {
    id: 'centurion',
    name: 'Centurion',
    description: 'Reach 100 total hours of focus time',
    icon: '💯',
    points: 100,
    category: 'focus'
  },
  consistent: {
    id: 'consistent',
    name: 'Consistent',
    description: 'Maintain a 7-day focus streak',
    icon: '🔥',
    points: 25,
    category: 'streak'
  },
  dedicated: {
    id: 'dedicated',
    name: 'Dedicated',
    description: 'Maintain a 30-day focus streak',
    icon: '💪',
    points: 75,
    category: 'streak'
  },
  unstoppable: {
    id: 'unstoppable',
    name: 'Unstoppable',
    description: 'Maintain a 100-day focus streak',
    icon: '🚀',
    points: 200,
    category: 'streak'
  },
  team_player: {
    id: 'team_player',
    name: 'Team Player',
    description: 'Join 5 different focus rooms',
    icon: '👥',
    points: 15,
    category: 'social'
  },
  popular_space: {
    id: 'popular_space',
    name: 'Popular Space',
    description: 'Have 5+ people in your room at once',
    icon: '🌟',
    points: 30,
    category: 'social'
  },
  buddy_up: {
    id: 'buddy_up',
    name: 'Buddy Up',
    description: 'Complete a session with a focus buddy',
    icon: '🤝',
    points: 20,
    category: 'social'
  }
};

export class GamificationService {
  calculateSessionPoints(duration: number, streak: number): number {
    let points = duration; // Base: 1 point per minute
    
    // Duration bonus for longer sessions
    if (duration >= 90) {
      points *= 1.2; // 20% bonus
    } else if (duration >= 50) {
      points *= 1.1; // 10% bonus
    }
    
    // Streak multiplier
    if (streak >= 30) {
      points *= 2; // 2x for 30+ days
    } else if (streak >= 7) {
      points *= 1.5; // 1.5x for 7+ days
    } else if (streak >= 3) {
      points *= 1.2; // 1.2x for 3+ days
    } else if (streak >= 1) {
      points *= 1.1; // 1.1x for any streak
    }
    
    return Math.round(points);
  }

  async awardPoints(userId: string, points: number, reason: string): Promise<void> {
    const user = dataStore.getUser(userId);
    if (!user) throw new Error('User not found');
    
    const newPoints = (user.points || 0) + points;
    const newFocusTime = (user.totalFocusTime || 0) + points; // Assuming 1 point = 1 minute
    
    dataStore.updateUser(userId, {
      points: newPoints,
      totalFocusTime: newFocusTime
    });
    
    // Update daily stats
    await gamificationStore.updateDailyStats(userId, {
      points,
      focusTime: points, // Using points as focusTime proxy for now
      sessionsCompleted: 1
    });
  }

  async updateStreak(userId: string): Promise<number> {
    const user = dataStore.getUser(userId);
    if (!user) throw new Error('User not found');
    
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    const lastActive = user.lastActiveDate ? new Date(user.lastActiveDate) : null;
    if (lastActive) {
      lastActive.setHours(0, 0, 0, 0);
    }
    
    // If already active today, return current streak
    if (lastActive && lastActive.getTime() === today.getTime()) {
      return user.currentStreak || 0;
    }
    
    let newStreak = 1;
    
    if (lastActive) {
      const daysSinceActive = Math.floor((today.getTime() - lastActive.getTime()) / (1000 * 60 * 60 * 24));
      
      if (daysSinceActive === 1) {
        // Continuing streak from yesterday
        newStreak = (user.currentStreak || 0) + 1;
      } else if (daysSinceActive > 1) {
        // Streak broken, start new one
        newStreak = 1;
      }
    }
    
    const updateData: any = {
      currentStreak: newStreak,
      lastActiveDate: new Date().toISOString()
    };
    
    // Only update longestStreak if it's actually longer
    if (newStreak > (user.longestStreak || 0)) {
      updateData.longestStreak = newStreak;
    }
    
    dataStore.updateUser(userId, updateData);
    
    return newStreak;
  }

  async checkStreakStatus(userId: string): Promise<StreakInfo> {
    const user = dataStore.getUser(userId);
    if (!user) throw new Error('User not found');
    
    const current = user.currentStreak || 0;
    const longest = user.longestStreak || 0;
    
    // Check if streak is still active
    let isActive = false;
    if (user.lastActiveDate) {
      const lastActive = new Date(user.lastActiveDate);
      const today = new Date();
      lastActive.setHours(0, 0, 0, 0);
      today.setHours(0, 0, 0, 0);
      
      const daysSince = Math.floor((today.getTime() - lastActive.getTime()) / (1000 * 60 * 60 * 24));
      isActive = daysSince <= 1;
    }
    
    // Calculate next milestone
    const milestones = [3, 7, 10, 30, 50, 100];
    const nextMilestone = milestones.find(m => m > current) || current + 10;
    
    return { current, longest, isActive, nextMilestone };
  }

  async checkAchievements(userId: string, trigger: AchievementTrigger): Promise<Achievement[]> {
    const earnedAchievements = await gamificationStore.getUserAchievements(userId);
    const newAchievements: Achievement[] = [];
    
    switch (trigger.type) {
      case 'session_complete':
        // First Focus
        if (!earnedAchievements.includes('first_focus') && trigger.duration && trigger.duration >= 25) {
          newAchievements.push(ACHIEVEMENTS.first_focus);
        }
        
        // Hour Power
        if (!earnedAchievements.includes('hour_power')) {
          const dailyStats = await gamificationStore.getDailyStats(userId, new Date());
          if (dailyStats && dailyStats.focusTime >= 60) {
            newAchievements.push(ACHIEVEMENTS.hour_power);
          }
        }
        
        // Centurion
        if (!earnedAchievements.includes('centurion')) {
          const user = dataStore.getUser(userId);
          if (user && user.totalFocusTime >= 6000) { // 100 hours = 6000 minutes
            newAchievements.push(ACHIEVEMENTS.centurion);
          }
        }
        break;
        
      case 'streak_updated':
        if (!trigger.streak) break;
        
        // Streak achievements
        if (!earnedAchievements.includes('consistent') && trigger.streak >= 7) {
          newAchievements.push(ACHIEVEMENTS.consistent);
        }
        if (!earnedAchievements.includes('dedicated') && trigger.streak >= 30) {
          newAchievements.push(ACHIEVEMENTS.dedicated);
        }
        if (!earnedAchievements.includes('unstoppable') && trigger.streak >= 100) {
          newAchievements.push(ACHIEVEMENTS.unstoppable);
        }
        break;
    }
    
    // Award achievements
    for (const achievement of newAchievements) {
      await this.awardAchievement(userId, achievement.id);
    }
    
    return newAchievements;
  }

  async awardAchievement(userId: string, achievementId: string): Promise<void> {
    const achievement = ACHIEVEMENTS[achievementId];
    if (!achievement) throw new Error('Achievement not found');
    
    await gamificationStore.awardAchievement(userId, achievementId);
    
    // Award points for achievement
    const user = dataStore.getUser(userId);
    if (user) {
      dataStore.updateUser(userId, {
        points: (user.points || 0) + achievement.points
      });
    }
  }

  async getUserAchievements(userId: string): Promise<Achievement[]> {
    const achievementIds = await gamificationStore.getUserAchievements(userId);
    return achievementIds.map(id => ACHIEVEMENTS[id]).filter(Boolean);
  }

  async getLeaderboard(type: 'daily' | 'weekly' | 'monthly' | 'allTime', limit: number): Promise<any[]> {
    // Use dataStore's getLeaderboard which has all user data
    const period = type === 'daily' ? 'daily' : type === 'weekly' ? 'weekly' : 'all';
    return dataStore.getLeaderboard(period, limit);
  }

  async getUserRank(userId: string, type: string): Promise<number> {
    return gamificationStore.getUserRank(userId, type);
  }

  async getUserStats(userId: string): Promise<any> {
    const user = dataStore.getUser(userId);
    if (!user) throw new Error('User not found');
    
    const today = new Date();
    const dailyStats = await gamificationStore.getDailyStats(userId, today);
    const rank = await gamificationStore.getUserRank(userId, 'daily');
    
    return {
      totalPoints: user.points || 0,
      totalFocusTime: user.totalFocusTime || 0,
      currentStreak: user.currentStreak || 0,
      longestStreak: user.longestStreak || 0,
      todayFocusTime: dailyStats?.focusTime || 0,
      todayPoints: dailyStats?.points || 0,
      rank
    };
  }

  async handleSessionComplete(userId: string, duration: number, currentStreak: number): Promise<SessionCompleteResult> {
    // Update streak
    const newStreak = await this.updateStreak(userId);
    
    // Calculate and award points
    const pointsEarned = this.calculateSessionPoints(duration, newStreak);
    await this.awardPoints(userId, pointsEarned, 'Completed focus session');
    
    // Check for achievements
    const achievements = await this.checkAchievements(userId, {
      type: 'session_complete',
      userId,
      duration
    });
    
    // Check streak achievements
    if (newStreak > currentStreak) {
      const streakAchievements = await this.checkAchievements(userId, {
        type: 'streak_updated',
        userId,
        streak: newStreak
      });
      achievements.push(...streakAchievements);
    }
    
    return {
      pointsEarned,
      newStreak,
      achievements
    };
  }
}

export const gamificationService = new GamificationService();