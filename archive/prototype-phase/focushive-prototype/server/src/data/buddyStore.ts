import { BuddyRequest, Buddyship } from '@focushive/shared';

interface BuddyStore {
  requests: Map<string, BuddyRequest>;
  buddyships: Map<string, Buddyship>;
  userRequests: Map<string, Set<string>>; // userId -> requestIds
  userBuddyships: Map<string, string>; // userId -> buddyshipId
}

class BuddyDataStore {
  private store: BuddyStore = {
    requests: new Map(),
    buddyships: new Map(),
    userRequests: new Map(),
    userBuddyships: new Map()
  };

  reset(): void {
    this.store.requests.clear();
    this.store.buddyships.clear();
    this.store.userRequests.clear();
    this.store.userBuddyships.clear();
  }

  // Request operations
  createRequest(request: BuddyRequest): BuddyRequest {
    this.store.requests.set(request.id, request);
    
    // Add to user's sent requests
    if (!this.store.userRequests.has(request.fromUserId)) {
      this.store.userRequests.set(request.fromUserId, new Set());
    }
    this.store.userRequests.get(request.fromUserId)!.add(request.id);
    
    // Add to user's received requests
    if (!this.store.userRequests.has(request.toUserId)) {
      this.store.userRequests.set(request.toUserId, new Set());
    }
    this.store.userRequests.get(request.toUserId)!.add(request.id);
    
    return request;
  }

  getRequest(requestId: string): BuddyRequest | undefined {
    return this.store.requests.get(requestId);
  }

  findRequest(fromUserId: string, toUserId: string): BuddyRequest | undefined {
    for (const request of this.store.requests.values()) {
      if (request.fromUserId === fromUserId && request.toUserId === toUserId) {
        return request;
      }
    }
    return undefined;
  }

  updateRequest(requestId: string, updates: Partial<BuddyRequest>): BuddyRequest | undefined {
    const request = this.store.requests.get(requestId);
    if (!request) return undefined;
    
    const updated = {
      ...request,
      ...updates,
      updatedAt: new Date().toISOString()
    };
    
    this.store.requests.set(requestId, updated);
    return updated;
  }

  getUserRequests(userId: string): BuddyRequest[] {
    const requestIds = this.store.userRequests.get(userId);
    if (!requestIds) return [];
    
    const requests: BuddyRequest[] = [];
    for (const requestId of requestIds) {
      const request = this.store.requests.get(requestId);
      if (request) {
        requests.push(request);
      }
    }
    
    return requests;
  }

  // Buddyship operations
  createBuddyship(buddyship: Buddyship): Buddyship {
    this.store.buddyships.set(buddyship.id, buddyship);
    
    // Map both users to this buddyship
    this.store.userBuddyships.set(buddyship.user1Id, buddyship.id);
    this.store.userBuddyships.set(buddyship.user2Id, buddyship.id);
    
    return buddyship;
  }

  getBuddyship(buddyshipId: string): Buddyship | undefined {
    return this.store.buddyships.get(buddyshipId);
  }

  getUserBuddyship(userId: string): Buddyship | undefined {
    const buddyshipId = this.store.userBuddyships.get(userId);
    if (!buddyshipId) return undefined;
    
    return this.store.buddyships.get(buddyshipId);
  }

  updateBuddyship(buddyshipId: string, updates: Partial<Buddyship>): Buddyship | undefined {
    const buddyship = this.store.buddyships.get(buddyshipId);
    if (!buddyship) return undefined;
    
    const updated = {
      ...buddyship,
      ...updates
    };
    
    this.store.buddyships.set(buddyshipId, updated);
    
    // If ending buddyship, remove user mappings
    if (updates.status === 'ended') {
      this.store.userBuddyships.delete(buddyship.user1Id);
      this.store.userBuddyships.delete(buddyship.user2Id);
    }
    
    return updated;
  }

  hasBuddyship(user1Id: string, user2Id: string): boolean {
    const buddyship1 = this.getUserBuddyship(user1Id);
    const buddyship2 = this.getUserBuddyship(user2Id);
    
    if (buddyship1 && buddyship1.status === 'active') {
      return buddyship1.user1Id === user2Id || buddyship1.user2Id === user2Id;
    }
    
    if (buddyship2 && buddyship2.status === 'active') {
      return buddyship2.user1Id === user1Id || buddyship2.user2Id === user1Id;
    }
    
    return false;
  }
}

export const buddyStore = new BuddyDataStore();