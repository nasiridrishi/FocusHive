import { BuddyService } from '../../services/buddyService';
import { buddyStore } from '../../data/buddyStore';
import { dataStore } from '../../data/store';

describe('Buddy System Integration Tests', () => {
  let buddyService: BuddyService;
  
  beforeEach(() => {
    buddyService = new BuddyService();
    buddyStore.reset();
    dataStore.clear();
  });
  
  it('should handle complete buddy journey', async () => {
    // Create test users
    const alice = dataStore.createUser({
      id: 'alice-' + Date.now(),
      email: 'alice@example.com',
      username: 'Alice',
      password: 'hashed',
      avatar: 'alice.jpg',
      totalFocusTime: 150,
      currentStreak: 7,
      longestStreak: 10,
      points: 750,
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
    
    const bob = dataStore.createUser({
      id: 'bob-' + Date.now(),
      email: 'bob@example.com',
      username: 'Bob',
      password: 'hashed',
      avatar: 'bob.jpg',
      totalFocusTime: 140,
      currentStreak: 5,
      longestStreak: 8,
      points: 650,
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
    
    const charlie = dataStore.createUser({
      id: 'charlie-' + Date.now(),
      email: 'charlie@example.com',
      username: 'Charlie',
      password: 'hashed',
      avatar: 'charlie.jpg',
      totalFocusTime: 500,
      currentStreak: 20,
      longestStreak: 30,
      points: 2500,
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
    
    // Step 1: Alice finds potential buddies
    const potentials = await buddyService.findPotentialBuddies(alice.id);
    expect(potentials).toHaveLength(2);
    
    // Bob should have higher compatibility (similar stats)
    expect(potentials[0].userId).toBe(bob.id);
    expect(potentials[0].compatibilityScore).toBeGreaterThan(potentials[1].compatibilityScore);
    
    // Step 2: Alice sends request to Bob
    const request = await buddyService.sendBuddyRequest(
      alice.id, 
      bob.id, 
      'Hi Bob! Our focus stats are similar. Want to be buddies?'
    );
    expect(request.status).toBe('pending');
    
    // Step 3: Bob checks his requests
    const bobRequests = await buddyService.getBuddyRequests(bob.id);
    expect(bobRequests.received).toHaveLength(1);
    expect(bobRequests.received[0].fromUserId).toBe(alice.id);
    expect(bobRequests.sent).toHaveLength(0);
    
    // Step 4: Bob accepts the request
    const acceptResult = await buddyService.acceptBuddyRequest(alice.id, bob.id);
    expect(acceptResult.success).toBe(true);
    expect(acceptResult.buddyship?.status).toBe('active');
    
    // Step 5: Both users have each other as buddy
    const aliceBuddy = await buddyService.getCurrentBuddy(alice.id);
    const bobBuddy = await buddyService.getCurrentBuddy(bob.id);
    
    expect(aliceBuddy?.buddyId).toBe(bob.id);
    expect(bobBuddy?.buddyId).toBe(alice.id);
    
    // Step 6: They set shared goals
    await buddyService.updateSharedGoals(alice.id, [
      'Complete 5 focus sessions daily',
      'Maintain a 7-day streak',
      'Study for 2 hours each day'
    ]);
    
    // Both should see the shared goals
    const aliceUpdatedBuddy = await buddyService.getCurrentBuddy(alice.id);
    const bobUpdatedBuddy = await buddyService.getCurrentBuddy(bob.id);
    
    expect(aliceUpdatedBuddy?.sharedGoals).toHaveLength(3);
    expect(bobUpdatedBuddy?.sharedGoals).toEqual(aliceUpdatedBuddy?.sharedGoals);
    
    // Step 7: Charlie can't request to be buddy with Alice (she's taken)
    const alicePotentials = await buddyService.findPotentialBuddies(alice.id);
    expect(alicePotentials.find(p => p.userId === bob.id)).toBeUndefined();
    
    // Step 8: After some time, they end their buddyship
    const endResult = await buddyService.endBuddyship(alice.id);
    expect(endResult.success).toBe(true);
    
    // Neither should have a buddy now
    const aliceAfterEnd = await buddyService.getCurrentBuddy(alice.id);
    const bobAfterEnd = await buddyService.getCurrentBuddy(bob.id);
    
    expect(aliceAfterEnd).toBeNull();
    expect(bobAfterEnd).toBeNull();
    
    // They can now find each other as potential buddies again
    const aliceNewPotentials = await buddyService.findPotentialBuddies(alice.id);
    expect(aliceNewPotentials.find(p => p.userId === bob.id)).toBeDefined();
  });
  
  it('should handle declined requests properly', async () => {
    const user1 = dataStore.createUser({
      id: 'user1-' + Date.now(),
      email: 'user1@example.com',
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
    
    const user2 = dataStore.createUser({
      id: 'user2-' + Date.now(),
      email: 'user2@example.com',
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
    
    // Send request
    await buddyService.sendBuddyRequest(user1.id, user2.id, 'Be my buddy?');
    
    // Decline request
    const declineResult = await buddyService.declineBuddyRequest(user1.id, user2.id);
    expect(declineResult.success).toBe(true);
    
    // Request should no longer appear in pending lists
    const user1Requests = await buddyService.getBuddyRequests(user1.id);
    const user2Requests = await buddyService.getBuddyRequests(user2.id);
    
    expect(user1Requests.sent).toHaveLength(0);
    expect(user2Requests.received).toHaveLength(0);
    
    // They can still send new requests to each other
    const newRequest = await buddyService.sendBuddyRequest(
      user2.id, 
      user1.id, 
      'Sorry about before. Want to try again?'
    );
    expect(newRequest.status).toBe('pending');
  });
  
  it('should enforce buddy exclusivity', async () => {
    const user1 = dataStore.createUser({
      id: 'exclusive1-' + Date.now(),
      email: 'exclusive1@example.com',
      username: 'ExclusiveUser1',
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
    
    const user2 = dataStore.createUser({
      id: 'exclusive2-' + Date.now(),
      email: 'exclusive2@example.com',
      username: 'ExclusiveUser2',
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
    
    const user3 = dataStore.createUser({
      id: 'exclusive3-' + Date.now(),
      email: 'exclusive3@example.com',
      username: 'ExclusiveUser3',
      password: 'hashed',
      avatar: 'avatar3.jpg',
      totalFocusTime: 130,
      currentStreak: 4,
      longestStreak: 8,
      points: 480,
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
    
    // User1 and User2 become buddies
    await buddyService.sendBuddyRequest(user1.id, user2.id, 'Be my buddy?');
    await buddyService.acceptBuddyRequest(user1.id, user2.id);
    
    // User3 tries to send request to User1 (who already has a buddy)
    await expect(buddyService.sendBuddyRequest(user3.id, user1.id, 'Want to be buddies?'))
      .rejects.toThrow('User already has a buddy');
    
    // User1 should not appear in User3's potential buddies
    const user3Potentials = await buddyService.findPotentialBuddies(user3.id);
    expect(user3Potentials.find(p => p.userId === user1.id)).toBeUndefined();
    expect(user3Potentials.find(p => p.userId === user2.id)).toBeUndefined();
  });
});