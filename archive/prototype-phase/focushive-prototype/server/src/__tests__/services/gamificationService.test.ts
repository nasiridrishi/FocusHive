import { GamificationService } from '../../services/gamificationService';
import { dataStore } from '../../data/store';
import { gamificationStore } from '../../data/gamificationStore';

describe('GamificationService', () => {
  let service: GamificationService;
  let mockUser: any;

  beforeEach(() => {
    service = new GamificationService();
    jest.clearAllMocks();
    
    // Mock user
    mockUser = {
      id: 'user1',
      email: 'test@example.com',
      username: 'testuser',
      passwordHash: 'hashed',
      avatar: 'avatar.jpg',
      totalFocusTime: 0,
      currentStreak: 0,
      longestStreak: 0,
      points: 0,
      lastActiveDate: new Date().toISOString(),
      lookingForBuddy: false,
      preferences: {
        darkMode: false,
        soundEnabled: true,
        defaultPomodoro: { focusDuration: 25, breakDuration: 5 }
      },
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    
    jest.spyOn(dataStore, 'getUser').mockReturnValue(mockUser);
    jest.spyOn(dataStore, 'updateUser').mockReturnValue(mockUser);
  });

  describe('Points System', () => {
    describe('calculateSessionPoints', () => {
      it('should calculate basic points (1 per minute)', () => {
        const points = service.calculateSessionPoints(25, 0);
        expect(points).toBe(25);
      });

      it('should apply streak multiplier', () => {
        // 7-day streak = 1.5x multiplier
        const points = service.calculateSessionPoints(25, 7);
        expect(points).toBe(38); // 25 * 1.5 = 37.5, rounded up
      });

      it('should apply higher multiplier for longer streaks', () => {
        // 30-day streak = 2x multiplier
        const points = service.calculateSessionPoints(25, 30);
        expect(points).toBe(50);
      });

      it('should give bonus for longer sessions', () => {
        // 50-minute session gets 10% bonus  
        const points = service.calculateSessionPoints(50, 0);
        expect(points).toBe(55); // 50 * 1.1 = 55
      });

      it('should combine streak and duration bonuses', () => {
        // 50-minute session with 7-day streak
        const points = service.calculateSessionPoints(50, 7);
        expect(points).toBe(83); // 50 * 1.1 * 1.5 = 82.5, rounded up
      });
    });

    describe('awardPoints', () => {
      it('should award points to user', async () => {
        await service.awardPoints('user1', 25, 'Completed focus session');
        
        expect(dataStore.updateUser).toHaveBeenCalledWith('user1', {
          points: 25,
          totalFocusTime: 25
        });
      });

      it('should add to existing points', async () => {
        mockUser.points = 100;
        await service.awardPoints('user1', 25, 'Completed focus session');
        
        expect(dataStore.updateUser).toHaveBeenCalledWith('user1', {
          points: 125,
          totalFocusTime: 25
        });
      });

      it('should track daily stats', async () => {
        const updateDailyStats = jest.spyOn(gamificationStore, 'updateDailyStats')
          .mockResolvedValue();
        
        await service.awardPoints('user1', 25, 'Completed focus session');
        
        expect(updateDailyStats).toHaveBeenCalledWith('user1', {
          points: 25,
          focusTime: 25,
          sessionsCompleted: 1
        });
      });
    });
  });

  describe('Streak System', () => {
    describe('updateStreak', () => {
      it('should start new streak', async () => {
        mockUser.currentStreak = 0;
        mockUser.lastActiveDate = new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString();
        
        const newStreak = await service.updateStreak('user1');
        
        expect(newStreak).toBe(1);
        expect(dataStore.updateUser).toHaveBeenCalledWith('user1', {
          currentStreak: 1,
          longestStreak: 1,
          lastActiveDate: expect.any(String)
        });
      });

      it('should continue streak from yesterday', async () => {
        mockUser.currentStreak = 5;
        mockUser.longestStreak = 10;
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);
        mockUser.lastActiveDate = yesterday.toISOString();
        
        const newStreak = await service.updateStreak('user1');
        
        expect(newStreak).toBe(6);
        expect(dataStore.updateUser).toHaveBeenCalledWith('user1', {
          currentStreak: 6,
          lastActiveDate: expect.any(String)
        });
      });

      it('should update longest streak', async () => {
        mockUser.currentStreak = 10;
        mockUser.longestStreak = 10;
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);
        mockUser.lastActiveDate = yesterday.toISOString();
        
        const newStreak = await service.updateStreak('user1');
        
        expect(newStreak).toBe(11);
        expect(dataStore.updateUser).toHaveBeenCalledWith('user1', {
          currentStreak: 11,
          longestStreak: 11,
          lastActiveDate: expect.any(String)
        });
      });

      it('should not update if already active today', async () => {
        mockUser.currentStreak = 5;
        mockUser.lastActiveDate = new Date().toISOString();
        
        const newStreak = await service.updateStreak('user1');
        
        expect(newStreak).toBe(5);
        expect(dataStore.updateUser).not.toHaveBeenCalled();
      });

      it('should reset streak after 2+ days gap', async () => {
        mockUser.currentStreak = 10;
        mockUser.longestStreak = 10;
        const threeDaysAgo = new Date();
        threeDaysAgo.setDate(threeDaysAgo.getDate() - 3);
        mockUser.lastActiveDate = threeDaysAgo.toISOString();
        
        const newStreak = await service.updateStreak('user1');
        
        expect(newStreak).toBe(1);
        expect(dataStore.updateUser).toHaveBeenCalledWith('user1', {
          currentStreak: 1,
          lastActiveDate: expect.any(String)
        });
      });
    });

    describe('checkStreakStatus', () => {
      it('should return active streak info', async () => {
        mockUser.currentStreak = 7;
        mockUser.longestStreak = 30;
        mockUser.lastActiveDate = new Date().toISOString();
        
        const status = await service.checkStreakStatus('user1');
        
        expect(status).toEqual({
          current: 7,
          longest: 30,
          isActive: true,
          nextMilestone: 10
        });
      });

      it('should show inactive for old streak', async () => {
        mockUser.currentStreak = 7;
        const threeDaysAgo = new Date();
        threeDaysAgo.setDate(threeDaysAgo.getDate() - 3);
        mockUser.lastActiveDate = threeDaysAgo.toISOString();
        
        const status = await service.checkStreakStatus('user1');
        
        expect(status.isActive).toBe(false);
      });

      it('should calculate correct next milestone', async () => {
        mockUser.currentStreak = 25;
        
        const status = await service.checkStreakStatus('user1');
        
        expect(status.nextMilestone).toBe(30);
      });
    });
  });

  describe('Achievement System', () => {
    beforeEach(() => {
      jest.spyOn(gamificationStore, 'getUserAchievements').mockResolvedValue([]);
      jest.spyOn(gamificationStore, 'awardAchievement').mockResolvedValue();
    });

    describe('checkAchievements', () => {
      it('should award first focus achievement', async () => {
        const trigger = {
          type: 'session_complete' as const,
          duration: 25,
          userId: 'user1'
        };
        
        const achievements = await service.checkAchievements('user1', trigger);
        
        expect(achievements).toHaveLength(1);
        expect(achievements[0].id).toBe('first_focus');
      });

      it('should award hour power achievement', async () => {
        jest.spyOn(gamificationStore, 'getDailyStats').mockResolvedValue({
          date: new Date().toISOString().split('T')[0],
          focusTime: 60,
          points: 60,
          sessionsCompleted: 3
        });
        
        const trigger = {
          type: 'session_complete' as const,
          duration: 25,
          userId: 'user1'
        };
        
        const achievements = await service.checkAchievements('user1', trigger);
        
        expect(achievements.some(a => a.id === 'hour_power')).toBe(true);
      });

      it('should award streak achievements', async () => {
        mockUser.currentStreak = 7;
        
        const trigger = {
          type: 'streak_updated' as const,
          streak: 7,
          userId: 'user1'
        };
        
        const achievements = await service.checkAchievements('user1', trigger);
        
        expect(achievements.some(a => a.id === 'consistent')).toBe(true);
      });

      it('should not award already earned achievements', async () => {
        jest.spyOn(gamificationStore, 'getUserAchievements')
          .mockResolvedValue(['first_focus', 'hour_power']);
        jest.spyOn(gamificationStore, 'getDailyStats').mockResolvedValue({
          date: new Date().toISOString().split('T')[0],
          focusTime: 60,
          points: 60,
          sessionsCompleted: 3
        });
        
        const trigger = {
          type: 'session_complete' as const,
          duration: 25,
          userId: 'user1'
        };
        
        const achievements = await service.checkAchievements('user1', trigger);
        
        expect(achievements).toHaveLength(0);
      });
    });

    describe('awardAchievement', () => {
      it('should award achievement and points', async () => {
        await service.awardAchievement('user1', 'first_focus');
        
        expect(gamificationStore.awardAchievement).toHaveBeenCalledWith('user1', 'first_focus');
        expect(dataStore.updateUser).toHaveBeenCalledWith('user1', {
          points: 10 // First focus gives 10 points
        });
      });
    });
  });

  describe('Leaderboard System', () => {
    describe('getLeaderboard', () => {
      it('should get daily leaderboard', async () => {
        const mockLeaderboard = [
          { userId: 'user1', username: 'user1', avatar: '', focusTime: 120, points: 150, streak: 5, rank: 1 },
          { userId: 'user2', username: 'user2', avatar: '', focusTime: 90, points: 100, streak: 3, rank: 2 }
        ];
        
        jest.spyOn(gamificationStore, 'getLeaderboard')
          .mockResolvedValue(mockLeaderboard);
        
        const leaderboard = await service.getLeaderboard('daily', 10);
        
        expect(leaderboard).toEqual(mockLeaderboard);
        expect(gamificationStore.getLeaderboard).toHaveBeenCalledWith('daily', 10);
      });

      it('should handle different time periods', async () => {
        jest.spyOn(gamificationStore, 'getLeaderboard').mockResolvedValue([]);
        
        await service.getLeaderboard('weekly', 10);
        expect(gamificationStore.getLeaderboard).toHaveBeenCalledWith('weekly', 10);
        
        await service.getLeaderboard('monthly', 10);
        expect(gamificationStore.getLeaderboard).toHaveBeenCalledWith('monthly', 10);
      });
    });

    describe('getUserRank', () => {
      it('should get user rank in leaderboard', async () => {
        jest.spyOn(gamificationStore, 'getUserRank').mockResolvedValue(5);
        
        const rank = await service.getUserRank('user1', 'daily');
        
        expect(rank).toBe(5);
        expect(gamificationStore.getUserRank).toHaveBeenCalledWith('user1', 'daily');
      });
    });
  });

  describe('Integration with Timer', () => {
    it('should handle complete focus session', async () => {
      // Set user to have no current streak
      mockUser.currentStreak = 0;
      mockUser.lastActiveDate = null;
      
      jest.spyOn(gamificationStore, 'getUserAchievements').mockResolvedValue([]);
      jest.spyOn(gamificationStore, 'updateDailyStats').mockResolvedValue();
      jest.spyOn(gamificationStore, 'awardAchievement').mockResolvedValue();
      jest.spyOn(gamificationStore, 'getDailyStats').mockResolvedValue(null);
      
      const result = await service.handleSessionComplete('user1', 25, 0);
      
      expect(result.pointsEarned).toBe(28); // 25 * 1.1 (new streak = 1) = 27.5 rounded to 28
      expect(result.newStreak).toBe(1);
      expect(result.achievements).toHaveLength(1);
      expect(result.achievements[0].id).toBe('first_focus');
    });
  });
});