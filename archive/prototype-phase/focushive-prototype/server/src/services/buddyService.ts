import { v4 as uuidv4 } from 'uuid';
import { buddyStore } from '../data/buddyStore';
import { dataStore } from '../data/store';
import type {
  BuddyRequest,
  Buddyship,
  PotentialBuddy,
  BuddyStatus,
  BuddyRequestInfo,
  User
} from '@focushive/shared';

export class BuddyService {
  async findPotentialBuddies(userId: string): Promise<PotentialBuddy[]> {
    const currentUser = dataStore.getUser(userId);
    if (!currentUser) throw new Error('User not found');

    // Get all users looking for buddies
    const allUsers = dataStore.getUsers();
    console.log(`Total users in system: ${allUsers.length}`);
    console.log(`Current user looking for buddy: ${currentUser.lookingForBuddy}`);
    
    const potentialBuddies: PotentialBuddy[] = [];

    for (const user of allUsers) {
      console.log(`Checking user: ${user.username}, lookingForBuddy: ${user.lookingForBuddy}`);
      
      // Skip self and users not looking for buddies
      if (user.id === userId || !user.lookingForBuddy) {
        console.log(`  Skipping: ${user.id === userId ? 'self' : 'not looking for buddy'}`);
        continue;
      }

      // Skip if already buddies
      if (buddyStore.hasBuddyship(userId, user.id)) {
        console.log(`  Skipping: already buddies`);
        continue;
      }

      // Skip if the user already has a buddy
      const userBuddyship = buddyStore.getUserBuddyship(user.id);
      if (userBuddyship && userBuddyship.status === 'active') {
        console.log(`  Skipping: user already has a buddy`);
        continue;
      }

      // Skip if there's a pending request between them
      const existingRequest = this.findExistingRequest(userId, user.id);
      if (existingRequest && existingRequest.status === 'pending') {
        console.log(`  Skipping: pending request exists`);
        continue;
      }

      // Calculate compatibility score
      const compatibilityScore = this.calculateCompatibility(currentUser, user);

      potentialBuddies.push({
        userId: user.id,
        username: user.username,
        avatar: user.avatar,
        totalFocusTime: user.totalFocusTime,
        currentStreak: user.currentStreak,
        compatibilityScore
      });
    }

    // Sort by compatibility score descending
    return potentialBuddies.sort((a, b) => b.compatibilityScore - a.compatibilityScore);
  }

  private calculateCompatibility(user1: User, user2: User): number {
    // Simple compatibility based on similar focus time
    const focusTimeDiff = Math.abs(user1.totalFocusTime - user2.totalFocusTime);
    const maxFocusTime = Math.max(user1.totalFocusTime, user2.totalFocusTime, 1);
    const focusTimeScore = 100 * (1 - focusTimeDiff / maxFocusTime);

    // Streak similarity
    const streakDiff = Math.abs(user1.currentStreak - user2.currentStreak);
    const maxStreak = Math.max(user1.currentStreak, user2.currentStreak, 1);
    const streakScore = 100 * (1 - streakDiff / maxStreak);

    // Average the scores
    return Math.round((focusTimeScore + streakScore) / 2);
  }

  private findExistingRequest(user1Id: string, user2Id: string): BuddyRequest | undefined {
    const request1 = buddyStore.findRequest(user1Id, user2Id);
    const request2 = buddyStore.findRequest(user2Id, user1Id);
    return request1 || request2;
  }

  async sendBuddyRequest(fromUserId: string, toUserId: string, message: string): Promise<BuddyRequest> {
    // Validate
    if (fromUserId === toUserId) {
      throw new Error('Cannot send buddy request to yourself');
    }

    // Check if already buddies
    if (buddyStore.hasBuddyship(fromUserId, toUserId)) {
      throw new Error('Already buddies with this user');
    }

    // Check if target user already has a buddy
    const targetBuddy = buddyStore.getUserBuddyship(toUserId);
    if (targetBuddy && targetBuddy.status === 'active') {
      throw new Error('User already has a buddy');
    }

    // Check if sender already has a buddy
    const senderBuddy = buddyStore.getUserBuddyship(fromUserId);
    if (senderBuddy && senderBuddy.status === 'active') {
      throw new Error('You already have a buddy');
    }

    // Check for existing pending request
    const existingRequest = this.findExistingRequest(fromUserId, toUserId);
    if (existingRequest && existingRequest.status === 'pending') {
      throw new Error('Buddy request already exists');
    }

    // Create new request
    const request: BuddyRequest = {
      id: uuidv4(),
      fromUserId,
      toUserId,
      message,
      status: 'pending',
      createdAt: new Date().toISOString()
    };

    return buddyStore.createRequest(request);
  }

  async acceptBuddyRequest(fromUserId: string, toUserId: string): Promise<{ success: boolean; buddyship?: Buddyship }> {
    const request = buddyStore.findRequest(fromUserId, toUserId);
    if (!request || request.status !== 'pending') {
      throw new Error('Buddy request not found');
    }

    // Update request status
    buddyStore.updateRequest(request.id, { status: 'accepted' });

    // Create buddyship
    const buddyship: Buddyship = {
      id: uuidv4(),
      user1Id: fromUserId,
      user2Id: toUserId,
      status: 'active',
      startedAt: new Date().toISOString()
    };

    const created = buddyStore.createBuddyship(buddyship);

    return { success: true, buddyship: created };
  }

  async declineBuddyRequest(fromUserId: string, toUserId: string): Promise<{ success: boolean }> {
    const request = buddyStore.findRequest(fromUserId, toUserId);
    if (!request || request.status !== 'pending') {
      throw new Error('Buddy request not found');
    }

    buddyStore.updateRequest(request.id, { status: 'declined' });

    return { success: true };
  }

  async getCurrentBuddy(userId: string): Promise<BuddyStatus | null> {
    const buddyship = buddyStore.getUserBuddyship(userId);
    if (!buddyship || buddyship.status !== 'active') {
      return null;
    }

    // Get the other user's ID
    const buddyId = buddyship.user1Id === userId ? buddyship.user2Id : buddyship.user1Id;
    const buddy = dataStore.getUser(buddyId);
    if (!buddy) return null;

    return {
      buddyId: buddy.id,
      username: buddy.username,
      avatar: buddy.avatar,
      status: buddyship.status,
      sharedGoals: buddyship.sharedGoals,
      startedAt: buddyship.startedAt
    };
  }

  async endBuddyship(userId: string): Promise<{ success: boolean }> {
    const buddyship = buddyStore.getUserBuddyship(userId);
    if (!buddyship || buddyship.status !== 'active') {
      throw new Error('No active buddyship found');
    }

    buddyStore.updateBuddyship(buddyship.id, {
      status: 'ended',
      endedAt: new Date().toISOString()
    });

    return { success: true };
  }

  async getBuddyRequests(userId: string): Promise<{ sent: BuddyRequestInfo[]; received: BuddyRequestInfo[] }> {
    const allRequests = buddyStore.getUserRequests(userId);
    const sent: BuddyRequestInfo[] = [];
    const received: BuddyRequestInfo[] = [];

    for (const request of allRequests) {
      // Only include pending requests
      if (request.status !== 'pending') continue;

      if (request.fromUserId === userId) {
        // Sent request
        const toUser = dataStore.getUser(request.toUserId);
        if (toUser) {
          sent.push({
            ...request,
            username: toUser.username,
            avatar: toUser.avatar,
            totalFocusTime: toUser.totalFocusTime,
            currentStreak: toUser.currentStreak
          });
        }
      } else {
        // Received request
        const fromUser = dataStore.getUser(request.fromUserId);
        if (fromUser) {
          received.push({
            ...request,
            username: fromUser.username,
            avatar: fromUser.avatar,
            totalFocusTime: fromUser.totalFocusTime,
            currentStreak: fromUser.currentStreak
          });
        }
      }
    }

    return { sent, received };
  }

  async updateSharedGoals(userId: string, goals: string[]): Promise<{ success: boolean }> {
    const buddyship = buddyStore.getUserBuddyship(userId);
    if (!buddyship || buddyship.status !== 'active') {
      throw new Error('No active buddyship found');
    }

    buddyStore.updateBuddyship(buddyship.id, { sharedGoals: goals });

    return { success: true };
  }
}