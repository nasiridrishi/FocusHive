import { BuddyService } from '../../services/buddyService';
import { dataStore } from '../../data/store';
import { buddyStore } from '../../data/buddyStore';
import { User } from '@focushive/shared';

describe('BuddyService', () => {
  let buddyService: BuddyService;
  let user1: User;
  let user2: User;
  let user3: User;

  beforeEach(() => {
    buddyService = new BuddyService();
    dataStore.clear();
    buddyStore.reset();

    // Create test users
    user1 = dataStore.createUser({
      id: 'user1',
      email: 'user1@test.com',
      username: 'User1',
      password: 'hashed',
      avatar: 'avatar1.jpg',
      totalFocusTime: 100,
      currentStreak: 5,
      longestStreak: 10,
      points: 500,
      lastActiveDate: new Date().toISOString(),
      lookingForBuddy: true,
      preferences: {
        darkMode: false,
        soundEnabled: true,
        defaultPomodoro: { focusDuration: 25, breakDuration: 5 }
      },
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    });

    user2 = dataStore.createUser({
      id: 'user2',
      email: 'user2@test.com',
      username: 'User2',
      password: 'hashed',
      avatar: 'avatar2.jpg',
      totalFocusTime: 120,
      currentStreak: 3,
      longestStreak: 7,
      points: 450,
      lastActiveDate: new Date().toISOString(),
      lookingForBuddy: true,
      preferences: {
        darkMode: false,
        soundEnabled: true,
        defaultPomodoro: { focusDuration: 25, breakDuration: 5 }
      },
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    });

    user3 = dataStore.createUser({
      id: 'user3',
      email: 'user3@test.com',
      username: 'User3',
      password: 'hashed',
      avatar: 'avatar3.jpg',
      totalFocusTime: 50,
      currentStreak: 1,
      longestStreak: 2,
      points: 100,
      lastActiveDate: new Date().toISOString(),
      lookingForBuddy: false, // Not looking for buddy
      preferences: {
        darkMode: false,
        soundEnabled: true,
        defaultPomodoro: { focusDuration: 25, breakDuration: 5 }
      },
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    });
  });

  describe('findPotentialBuddies', () => {
    it('should find users looking for buddies', async () => {
      const potentials = await buddyService.findPotentialBuddies(user1.id);
      
      expect(potentials).toHaveLength(1);
      expect(potentials[0].userId).toBe(user2.id);
      expect(potentials[0].username).toBe(user2.username);
    });

    it('should not include users not looking for buddies', async () => {
      const potentials = await buddyService.findPotentialBuddies(user1.id);
      
      const userIds = potentials.map(p => p.userId);
      expect(userIds).not.toContain(user3.id);
    });

    it('should not include self', async () => {
      const potentials = await buddyService.findPotentialBuddies(user1.id);
      
      const userIds = potentials.map(p => p.userId);
      expect(userIds).not.toContain(user1.id);
    });

    it('should calculate compatibility score based on focus time', async () => {
      const potentials = await buddyService.findPotentialBuddies(user1.id);
      
      expect(potentials[0].compatibilityScore).toBeGreaterThan(0);
      expect(potentials[0].compatibilityScore).toBeLessThanOrEqual(100);
    });

    it('should sort by compatibility score', async () => {
      // Create another user with very different stats
      const user4 = dataStore.createUser({
        id: 'user4',
        email: 'user4@test.com',
        username: 'User4',
        password: 'hashed',
        avatar: 'avatar4.jpg',
        totalFocusTime: 500, // Very different from user1
        currentStreak: 20,
        longestStreak: 30,
        points: 2000,
        lastActiveDate: new Date().toISOString(),
        lookingForBuddy: true,
        preferences: {
          darkMode: false,
          soundEnabled: true,
          defaultPomodoro: { focusDuration: 25, breakDuration: 5 }
        },
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      });

      const potentials = await buddyService.findPotentialBuddies(user1.id);
      
      expect(potentials).toHaveLength(2);
      // User2 should have higher compatibility (closer stats to user1)
      expect(potentials[0].userId).toBe(user2.id);
      expect(potentials[1].userId).toBe(user4.id);
    });
  });

  describe('sendBuddyRequest', () => {
    it('should create a buddy request', async () => {
      const request = await buddyService.sendBuddyRequest(user1.id, user2.id, 'Let\'s focus together!');
      
      expect(request).toMatchObject({
        fromUserId: user1.id,
        toUserId: user2.id,
        message: 'Let\'s focus together!',
        status: 'pending'
      });
      expect(request.id).toBeDefined();
      expect(request.createdAt).toBeDefined();
    });

    it('should not allow duplicate pending requests', async () => {
      await buddyService.sendBuddyRequest(user1.id, user2.id, 'First request');
      
      await expect(buddyService.sendBuddyRequest(user1.id, user2.id, 'Second request'))
        .rejects.toThrow('Buddy request already exists');
    });

    it('should not allow request to self', async () => {
      await expect(buddyService.sendBuddyRequest(user1.id, user1.id, 'To myself'))
        .rejects.toThrow('Cannot send buddy request to yourself');
    });

    it('should not allow request if already buddies', async () => {
      // First create a buddy relationship
      await buddyService.sendBuddyRequest(user1.id, user2.id, 'Be my buddy');
      await buddyService.acceptBuddyRequest(user1.id, user2.id);
      
      // Try to send another request
      await expect(buddyService.sendBuddyRequest(user1.id, user2.id, 'Another request'))
        .rejects.toThrow('Already buddies with this user');
    });
  });

  describe('acceptBuddyRequest', () => {
    beforeEach(async () => {
      await buddyService.sendBuddyRequest(user1.id, user2.id, 'Be my buddy');
    });

    it('should accept buddy request and create buddy relationship', async () => {
      const result = await buddyService.acceptBuddyRequest(user1.id, user2.id);
      
      expect(result.success).toBe(true);
      expect(result.buddyship).toMatchObject({
        user1Id: user1.id,
        user2Id: user2.id,
        status: 'active'
      });
    });

    it('should mark request as accepted', async () => {
      await buddyService.acceptBuddyRequest(user1.id, user2.id);
      
      // getBuddyRequests only returns pending requests, so it should be empty after accepting
      const requests = await buddyService.getBuddyRequests(user2.id);
      const request = requests.received.find(r => r.fromUserId === user1.id);
      expect(request).toBeUndefined();
    });

    it('should not accept non-existent request', async () => {
      await expect(buddyService.acceptBuddyRequest(user3.id, user1.id))
        .rejects.toThrow('Buddy request not found');
    });
  });

  describe('declineBuddyRequest', () => {
    beforeEach(async () => {
      await buddyService.sendBuddyRequest(user1.id, user2.id, 'Be my buddy');
    });

    it('should decline buddy request', async () => {
      const result = await buddyService.declineBuddyRequest(user1.id, user2.id);
      
      expect(result.success).toBe(true);
    });

    it('should mark request as declined', async () => {
      await buddyService.declineBuddyRequest(user1.id, user2.id);
      
      // getBuddyRequests only returns pending requests, so it should be empty after declining
      const requests = await buddyService.getBuddyRequests(user2.id);
      const request = requests.received.find(r => r.fromUserId === user1.id);
      expect(request).toBeUndefined();
    });
  });

  describe('getCurrentBuddy', () => {
    it('should return current buddy', async () => {
      await buddyService.sendBuddyRequest(user1.id, user2.id, 'Be my buddy');
      await buddyService.acceptBuddyRequest(user1.id, user2.id);
      
      const buddy = await buddyService.getCurrentBuddy(user1.id);
      
      expect(buddy).toMatchObject({
        buddyId: user2.id,
        username: user2.username,
        avatar: user2.avatar,
        status: 'active'
      });
    });

    it('should return null if no buddy', async () => {
      const buddy = await buddyService.getCurrentBuddy(user1.id);
      
      expect(buddy).toBeNull();
    });
  });

  describe('endBuddyship', () => {
    beforeEach(async () => {
      await buddyService.sendBuddyRequest(user1.id, user2.id, 'Be my buddy');
      await buddyService.acceptBuddyRequest(user1.id, user2.id);
    });

    it('should end buddyship', async () => {
      const result = await buddyService.endBuddyship(user1.id);
      
      expect(result.success).toBe(true);
    });

    it('should mark buddyship as ended', async () => {
      await buddyService.endBuddyship(user1.id);
      
      const buddy1 = await buddyService.getCurrentBuddy(user1.id);
      const buddy2 = await buddyService.getCurrentBuddy(user2.id);
      
      expect(buddy1).toBeNull();
      expect(buddy2).toBeNull();
    });
  });

  describe('getBuddyRequests', () => {
    it('should return sent and received requests', async () => {
      await buddyService.sendBuddyRequest(user1.id, user2.id, 'From user1');
      await buddyService.sendBuddyRequest(user3.id, user1.id, 'From user3');
      
      const requests = await buddyService.getBuddyRequests(user1.id);
      
      expect(requests.sent).toHaveLength(1);
      expect(requests.sent[0].toUserId).toBe(user2.id);
      
      expect(requests.received).toHaveLength(1);
      expect(requests.received[0].fromUserId).toBe(user3.id);
    });

    it('should only return pending requests', async () => {
      await buddyService.sendBuddyRequest(user1.id, user2.id, 'Request 1');
      await buddyService.sendBuddyRequest(user3.id, user1.id, 'Request 2');
      
      // Accept one request
      await buddyService.acceptBuddyRequest(user1.id, user2.id);
      
      const requests = await buddyService.getBuddyRequests(user1.id);
      
      expect(requests.sent).toHaveLength(0); // Accepted request not shown
      expect(requests.received).toHaveLength(1); // Still pending
    });
  });
});