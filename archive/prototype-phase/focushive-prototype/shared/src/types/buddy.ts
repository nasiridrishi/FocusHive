export interface BuddyRequest {
  id: string;
  fromUserId: string;
  toUserId: string;
  message: string;
  status: 'pending' | 'accepted' | 'declined';
  createdAt: string;
  updatedAt?: string;
}

export interface Buddyship {
  id: string;
  user1Id: string;
  user2Id: string;
  status: 'active' | 'ended';
  sharedGoals?: string[];
  startedAt: string;
  endedAt?: string;
}

export interface PotentialBuddy {
  userId: string;
  username: string;
  avatar?: string;
  totalFocusTime: number;
  currentStreak: number;
  compatibilityScore: number;
  commonTags?: string[];
}

export interface BuddyStatus {
  buddyId: string;
  username: string;
  avatar?: string;
  status: 'active' | 'ended';
  sharedGoals?: string[];
  startedAt: string;
}

export interface BuddyRequestInfo extends BuddyRequest {
  username: string;
  avatar?: string;
  totalFocusTime: number;
  currentStreak: number;
}