export interface BuddyMatch {
  id: string;
  userId: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  avatar: string;
  status: 'ONLINE' | 'OFFLINE' | 'AWAY' | 'BUSY' | 'IN_FOCUS';
  compatibilityScore: number;
  matchScore?: number; // For compatibility with BuddyCard
  bio?: string;
  communicationStyle?: string;
  commonFocusAreas?: string[];
  completedGoalsCount?: number;
  averageSessionRating?: number;
  timezoneOverlapHours?: number;
  commonInterests: string[];
  achievements: Achievement[];
  focusGoals: string[];
  preferredSchedule: {
    timezone: string;
    workingHours: string;
    preferredFocusTimes: string[];
  };
  matchedAt?: string;
  isRequested?: boolean;
  isPending?: boolean;
  requestSentAt?: string;
}

export interface BuddyRelationship {
  id: string;
  buddyId: string;
  partnerId?: number; // For compatibility with BuddyCard
  partnerUsername?: string;
  partnerAvatar?: string;
  buddy: {
    id: string;
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    avatar: string;
  };
  status: 'ACTIVE' | 'PENDING' | 'BLOCKED' | 'ENDED';
  startedAt: string;
  endedAt?: string;
  updatedAt?: string;
  completedGoals?: number;
  totalGoals?: number;
  totalSessions?: number;
  compatibility: {
    score: number;
    commonInterests: string[];
    focusAlignment: number;
    scheduleCompatibility: number;
  };
  sharedSessions: number;
  achievements: Achievement[];
  lastInteraction?: string;
}

export interface Achievement {
  id: string;
  name: string;
  description: string;
  icon: string;
  earnedAt?: string;
  rarity?: 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY';
  category?: 'FOCUS' | 'SOCIAL' | 'MILESTONE' | 'CHALLENGE';
}