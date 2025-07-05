interface DailyStat {
  date: string;
  focusTime: number;
  points: number;
  sessionsCompleted: number;
}

interface UserStats {
  userId: string;
  achievements: string[];
  dailyStats: Map<string, DailyStat>;
  roomsJoined: Set<string>;
}

class GamificationStore {
  private userStats: Map<string, UserStats> = new Map();

  private getUserStats(userId: string): UserStats {
    if (!this.userStats.has(userId)) {
      this.userStats.set(userId, {
        userId,
        achievements: [],
        dailyStats: new Map(),
        roomsJoined: new Set()
      });
    }
    return this.userStats.get(userId)!;
  }

  async updateDailyStats(userId: string, update: Partial<DailyStat>): Promise<void> {
    const stats = this.getUserStats(userId);
    const today = new Date().toISOString().split('T')[0];
    
    const dailyStat = stats.dailyStats.get(today) || {
      date: today,
      focusTime: 0,
      points: 0,
      sessionsCompleted: 0
    };

    dailyStat.focusTime += update.focusTime || 0;
    dailyStat.points += update.points || 0;
    dailyStat.sessionsCompleted += update.sessionsCompleted || 0;

    stats.dailyStats.set(today, dailyStat);
  }

  async getDailyStats(userId: string, date: Date): Promise<DailyStat | null> {
    const stats = this.getUserStats(userId);
    const dateStr = date.toISOString().split('T')[0];
    return stats.dailyStats.get(dateStr) || null;
  }

  async getUserAchievements(userId: string): Promise<string[]> {
    const stats = this.getUserStats(userId);
    return [...stats.achievements];
  }

  async awardAchievement(userId: string, achievementId: string): Promise<void> {
    const stats = this.getUserStats(userId);
    if (!stats.achievements.includes(achievementId)) {
      stats.achievements.push(achievementId);
    }
  }

  async trackRoomJoined(userId: string, roomId: string): Promise<void> {
    const stats = this.getUserStats(userId);
    stats.roomsJoined.add(roomId);
  }

  async getRoomsJoinedCount(userId: string): Promise<number> {
    const stats = this.getUserStats(userId);
    return stats.roomsJoined.size;
  }

  async getLeaderboard(type: 'daily' | 'weekly' | 'monthly' | 'allTime', limit: number): Promise<any[]> {
    // In production, this would query from database
    // For prototype, we'll aggregate from in-memory data
    const leaderboard: any[] = [];
    
    // This is a simplified implementation
    // In real app, would aggregate based on time period
    for (const [userId, stats] of this.userStats) {
      let totalPoints = 0;
      let totalFocusTime = 0;
      
      if (type === 'daily') {
        const today = new Date().toISOString().split('T')[0];
        const dailyStat = stats.dailyStats.get(today);
        if (dailyStat) {
          totalPoints = dailyStat.points;
          totalFocusTime = dailyStat.focusTime;
        }
      } else {
        // For other periods, sum all (simplified for prototype)
        for (const stat of stats.dailyStats.values()) {
          totalPoints += stat.points;
          totalFocusTime += stat.focusTime;
        }
      }
      
      if (totalPoints > 0) {
        leaderboard.push({
          userId,
          username: userId, // In real app, would fetch from userStore
          avatar: '',
          focusTime: totalFocusTime,
          points: totalPoints,
          streak: 0, // Would fetch from userStore
          rank: 0
        });
      }
    }
    
    // Sort by points and assign ranks
    leaderboard.sort((a, b) => b.points - a.points);
    leaderboard.forEach((entry, index) => {
      entry.rank = index + 1;
    });
    
    return leaderboard.slice(0, limit);
  }

  async getUserRank(userId: string, type: string): Promise<number> {
    const leaderboard = await this.getLeaderboard(type as any, 1000);
    const entry = leaderboard.find(e => e.userId === userId);
    return entry ? entry.rank : 0;
  }

  reset(): void {
    this.userStats.clear();
  }
}

export const gamificationStore = new GamificationStore();