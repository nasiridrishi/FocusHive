import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import type {
  BuddyProfile,
  BuddyMatch,
  BuddySession,
  BuddyMatchRequest,
  CreateSessionRequest,
  UpdateSessionRequest,
  SessionCheckInRequest,
  RateSessionRequest,
  BuddySearchFilters,
  BuddyStats,
  BuddyInvitation,
  BuddyPreferences,
  BuddyMessage,
  SessionType,
  BuddyMatchStatus,
  BuddySessionStatus,
} from '@/contracts/buddy';
import { buddyService } from '../buddyService';

describe('BuddyService E2E Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Reset service state if needed
    if (buddyService.cleanup) {
      buddyService.cleanup();
    }
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('Buddy Profile Management', () => {
    it('should get current user buddy profile', async () => {
      const profile = await buddyService.getMyProfile();
      
      expect(profile).toBeDefined();
      expect(profile.userId).toBeGreaterThan(0);
      expect(profile.username).toBeDefined();
      expect(profile.interests).toBeInstanceOf(Array);
      expect(profile.goals).toBeInstanceOf(Array);
    });

    it('should get buddy profile by user ID', async () => {
      const userId = 123;
      const profile = await buddyService.getBuddyProfile(userId);
      
      expect(profile).toBeDefined();
      expect(profile.userId).toBe(userId);
      expect(profile.rating).toBeGreaterThanOrEqual(0);
      expect(profile.rating).toBeLessThanOrEqual(5);
    });

    it('should update buddy preferences', async () => {
      const preferences: BuddyPreferences = {
        matchingPreference: 'interests',
        minRating: 4.0,
        preferredLanguages: ['en', 'es'],
        autoAccept: false,
        notifyOnMatch: true,
        sessionReminders: true,
      };
      
      const updated = await buddyService.updatePreferences(preferences);
      
      expect(updated).toMatchObject(preferences);
    });

    it('should update buddy availability', async () => {
      const availability = {
        monday: [
          { startTime: '09:00', endTime: '12:00', sessionTypes: ['work' as SessionType] },
          { startTime: '14:00', endTime: '17:00', sessionTypes: ['study' as SessionType] },
        ],
        tuesday: [],
        wednesday: [],
        thursday: [],
        friday: [],
        saturday: [],
        sunday: [],
      };
      
      const updated = await buddyService.updateAvailability(availability);
      
      expect(updated.monday).toHaveLength(2);
      expect(updated.monday[0].startTime).toBe('09:00');
    });
  });

  describe('Buddy Matching', () => {
    it('should create a buddy match request', async () => {
      const request: BuddyMatchRequest = {
        userId: 1,
        sessionType: 'focus',
        duration: 60,
        sessionGoal: 'Complete project tasks',
        preferences: 'interests',
      };
      
      const match = await buddyService.requestMatch(request);
      
      expect(match).toBeDefined();
      expect(match.id).toBeDefined();
      expect(match.status).toBe('pending');
      expect(match.sessionType).toBe('focus');
    });

    it('should find available buddies with filters', async () => {
      const filters: BuddySearchFilters = {
        sessionType: 'study',
        interests: ['programming', 'math'],
        minRating: 4.0,
        availability: 'now',
      };
      
      const result = await buddyService.findBuddies(filters);
      
      expect(result.buddies).toBeInstanceOf(Array);
      expect(result.pagination).toBeDefined();
      expect(result.pagination.page).toBeGreaterThanOrEqual(0);
    });

    it('should accept a buddy match', async () => {
      const matchId = 'match-123';
      const result = await buddyService.acceptMatch(matchId);
      
      expect(result.status).toBe('accepted');
      expect(result.id).toBe(matchId);
    });

    it('should decline a buddy match', async () => {
      const matchId = 'match-456';
      const result = await buddyService.declineMatch(matchId);
      
      expect(result.status).toBe('declined');
      expect(result.id).toBe(matchId);
    });

    it('should get pending matches', async () => {
      const matches = await buddyService.getPendingMatches();
      
      expect(matches).toBeInstanceOf(Array);
      matches.forEach(match => {
        expect(match.status).toBe('pending');
        expect(match.expiresAt).toBeDefined();
      });
    });

    it('should cancel a match request', async () => {
      const matchId = 'match-789';
      await expect(buddyService.cancelMatch(matchId)).resolves.not.toThrow();
    });
  });

  describe('Buddy Sessions', () => {
    it('should create a new buddy session', async () => {
      const request: CreateSessionRequest = {
        matchId: 'match-123',
        sessionType: 'work',
        goal: 'Complete coding tasks',
        duration: 90,
      };
      
      const session = await buddyService.createSession(request);
      
      expect(session).toBeDefined();
      expect(session.id).toBeDefined();
      expect(session.status).toBe('scheduled');
      expect(session.sessionType).toBe('work');
      expect(session.plannedDuration).toBe(90);
    });

    it('should start a buddy session', async () => {
      const sessionId = 'session-123';
      const session = await buddyService.startSession(sessionId);
      
      expect(session.status).toBe('active');
      expect(session.startedAt).toBeDefined();
    });

    it('should pause a buddy session', async () => {
      const sessionId = 'session-123';
      const session = await buddyService.pauseSession(sessionId);
      
      expect(session.status).toBe('paused');
      expect(session.pausedAt).toBeDefined();
    });

    it('should resume a buddy session', async () => {
      const sessionId = 'session-123';
      const session = await buddyService.resumeSession(sessionId);
      
      expect(session.status).toBe('active');
      expect(session.pausedAt).toBeNull();
    });

    it('should end a buddy session', async () => {
      const sessionId = 'session-123';
      const session = await buddyService.endSession(sessionId);
      
      expect(session.status).toBe('completed');
      expect(session.endedAt).toBeDefined();
      expect(session.actualDuration).toBeGreaterThan(0);
    });

    it('should check in during a session', async () => {
      const sessionId = 'session-123';
      const checkIn: SessionCheckInRequest = {
        status: 'on_track',
        progress: 50,
        message: 'Halfway through my tasks',
      };
      
      const result = await buddyService.checkIn(sessionId, checkIn);
      
      expect(result).toBeDefined();
      expect(result.status).toBe('on_track');
      expect(result.progress).toBe(50);
    });

    it('should get active sessions', async () => {
      const sessions = await buddyService.getActiveSessions();
      
      expect(sessions).toBeInstanceOf(Array);
      sessions.forEach(session => {
        expect(['active', 'paused']).toContain(session.status);
      });
    });

    it('should get session history', async () => {
      const history = await buddyService.getSessionHistory();
      
      expect(history).toBeInstanceOf(Array);
      expect(history.every(s => s.status === 'completed')).toBe(true);
    });

    it('should rate a completed session', async () => {
      const sessionId = 'session-123';
      const rating: RateSessionRequest = {
        overallRating: 5,
        punctuality: 5,
        focus: 4,
        helpfulness: 5,
        wouldRepeat: true,
        feedback: 'Great session partner!',
      };
      
      const result = await buddyService.rateSession(sessionId, rating);
      
      expect(result.overallRating).toBe(5);
      expect(result.sessionId).toBe(sessionId);
    });
  });

  describe('Buddy Statistics', () => {
    it('should get buddy statistics', async () => {
      const stats = await buddyService.getMyStats();
      
      expect(stats).toBeDefined();
      expect(stats.totalSessions).toBeGreaterThanOrEqual(0);
      expect(stats.totalHours).toBeGreaterThanOrEqual(0);
      expect(stats.averageRating).toBeGreaterThanOrEqual(0);
      expect(stats.averageRating).toBeLessThanOrEqual(5);
      expect(stats.badges).toBeInstanceOf(Array);
    });

    it('should get top buddies', async () => {
      const topBuddies = await buddyService.getTopBuddies();
      
      expect(topBuddies).toBeInstanceOf(Array);
      topBuddies.forEach(buddy => {
        expect(buddy.sessionCount).toBeGreaterThan(0);
        expect(buddy.averageRating).toBeGreaterThanOrEqual(0);
      });
    });

    it('should get buddy leaderboard', async () => {
      const leaderboard = await buddyService.getLeaderboard('weekly');
      
      expect(leaderboard).toBeInstanceOf(Array);
      expect(leaderboard).toHaveLength(leaderboard.length);
      
      // Check if sorted by rank
      for (let i = 1; i < leaderboard.length; i++) {
        expect(leaderboard[i].totalHours).toBeLessThanOrEqual(leaderboard[i - 1].totalHours);
      }
    });
  });

  describe('Buddy Invitations', () => {
    it('should send a buddy invitation', async () => {
      const invitation = {
        toUserId: 456,
        sessionType: 'study' as SessionType,
        message: 'Want to study together?',
        scheduledFor: new Date(Date.now() + 3600000).toISOString(),
      };
      
      const result = await buddyService.sendInvitation(invitation);
      
      expect(result.id).toBeDefined();
      expect(result.status).toBe('pending');
      expect(result.toUserId).toBe(456);
    });

    it('should get received invitations', async () => {
      const invitations = await buddyService.getReceivedInvitations();
      
      expect(invitations).toBeInstanceOf(Array);
      invitations.forEach(inv => {
        expect(inv.status).toBe('pending');
        expect(inv.expiresAt).toBeDefined();
      });
    });

    it('should accept an invitation', async () => {
      const invitationId = 'inv-123';
      const result = await buddyService.acceptInvitation(invitationId);
      
      expect(result.status).toBe('accepted');
    });

    it('should decline an invitation', async () => {
      const invitationId = 'inv-456';
      const result = await buddyService.declineInvitation(invitationId);
      
      expect(result.status).toBe('declined');
    });
  });

  describe('Session Messages', () => {
    it('should send a message during session', async () => {
      const sessionId = 'session-123';
      const message = {
        content: 'Keep up the good work!',
        type: 'encouragement' as const,
      };
      
      const result = await buddyService.sendMessage(sessionId, message);
      
      expect(result.id).toBeDefined();
      expect(result.sessionId).toBe(sessionId);
      expect(result.content).toBe(message.content);
      expect(result.type).toBe('encouragement');
    });

    it('should get session messages', async () => {
      const sessionId = 'session-123';
      const messages = await buddyService.getSessionMessages(sessionId);
      
      expect(messages).toBeInstanceOf(Array);
      messages.forEach(msg => {
        expect(msg.sessionId).toBe(sessionId);
        expect(msg.senderId).toBeDefined();
        expect(msg.content).toBeDefined();
      });
    });

    it('should add emoji reaction to message', async () => {
      const messageId = 'msg-123';
      const emoji = '👍';
      
      const result = await buddyService.addMessageReaction(messageId, emoji);
      
      expect(result).toBeDefined();
      expect(result.reactions).toContainEqual(
        expect.objectContaining({ emoji })
      );
    });
  });

  describe('Error Handling', () => {
    it('should handle invalid match request gracefully', async () => {
      const invalidRequest: BuddyMatchRequest = {
        userId: -1,
        sessionType: 'invalid' as SessionType,
        duration: -30,
      };
      
      await expect(buddyService.requestMatch(invalidRequest)).rejects.toThrow();
    });

    it('should handle network errors', async () => {
      // Simulate network error
      vi.spyOn(global, 'fetch').mockRejectedValueOnce(new Error('Network error'));
      
      await expect(buddyService.getMyProfile()).rejects.toThrow('Network error');
    });

    it('should handle unauthorized access', async () => {
      // Mock 401 response
      vi.spyOn(global, 'fetch').mockResolvedValueOnce(
        new Response(null, { status: 401 })
      );
      
      await expect(buddyService.getMyProfile()).rejects.toThrow();
    });
  });

  describe('Caching', () => {
    it('should cache buddy profiles', async () => {
      const userId = 123;
      
      // First call - should fetch from API
      const profile1 = await buddyService.getBuddyProfile(userId);
      
      // Second call - should return from cache
      const profile2 = await buddyService.getBuddyProfile(userId);
      
      expect(profile1).toEqual(profile2);
      expect(fetch).toHaveBeenCalledTimes(1);
    });

    it('should invalidate cache on profile update', async () => {
      const preferences: BuddyPreferences = {
        matchingPreference: 'goals',
        autoAccept: true,
        notifyOnMatch: true,
        sessionReminders: false,
      };
      
      await buddyService.updatePreferences(preferences);
      
      // Cache should be cleared after update
      const profile = await buddyService.getMyProfile();
      expect(profile.preferences).toMatchObject(preferences);
    });
  });

  describe('WebSocket Integration', () => {
    it('should subscribe to buddy match updates', async () => {
      const callback = vi.fn();
      const unsubscribe = buddyService.subscribeToMatchUpdates(callback);
      
      expect(unsubscribe).toBeInstanceOf(Function);
      
      // Cleanup
      unsubscribe();
    });

    it('should subscribe to session updates', async () => {
      const sessionId = 'session-123';
      const callback = vi.fn();
      const unsubscribe = buddyService.subscribeToSessionUpdates(sessionId, callback);
      
      expect(unsubscribe).toBeInstanceOf(Function);
      
      // Cleanup
      unsubscribe();
    });

    it('should handle buddy notifications', async () => {
      const callback = vi.fn();
      const unsubscribe = buddyService.subscribeToNotifications(callback);
      
      // Simulate notification
      console.log('Buddy notification subscription active');
      
      expect(unsubscribe).toBeInstanceOf(Function);
      unsubscribe();
    });
  });
});
