export interface ForumPost {
  id: string;
  userId: string;
  username: string;
  userAvatar: string;
  type: 'study' | 'work' | 'accountability' | 'group';
  title: string;
  description: string;
  tags: string[];
  schedule: {
    days: string[]; // ['monday', 'wednesday', 'friday']
    timeSlots: { start: string; end: string }[];
    timezone: string;
  };
  commitmentLevel: 'one-time' | 'weekly' | 'daily' | 'flexible';
  workingStyle: {
    videoPreference: 'on' | 'off' | 'optional';
    communicationStyle: 'minimal' | 'moderate' | 'chatty';
    breakPreference: 'synchronized' | 'independent';
  };
  status: 'active' | 'matched' | 'closed';
  responses: string[]; // user IDs who responded
  createdAt: string;
  updatedAt: string;
  expiresAt?: string;
}

export interface GlobalChatMessage {
  id: string;
  userId: string;
  username: string;
  userAvatar: string;
  message: string;
  timestamp: string;
  isDeleted: boolean;
  reportedBy: string[]; // user IDs who reported
}

export interface BuddyConnection {
  id: string;
  requesterId: string; // userId who sent the request
  requestedUserId: string; // userId who received the request
  postId: string;
  status: 'pending' | 'accepted' | 'declined';
  compatibilityScore: number;
  sharedTags: string[];
  privateRoomId?: string;
  schedule: {
    days: string[];
    timeSlots: { start: string; end: string }[];
    timezone: string;
  };
  createdAt: string;
  updatedAt: string;
  stats: {
    sessionsCompleted: number;
    totalFocusTime: number; // in minutes
    streak: number;
  };
  // Optional user data (populated when fetching)
  requesterUser?: {
    id: string;
    username: string;
    avatar?: string;
  };
  requestedUser?: {
    id: string;
    username: string;
    avatar?: string;
  };
}

export interface ForumFilters {
  sortBy: 'recent' | 'responses' | 'starting-soon';
  filterBy: 'study' | 'work' | 'both';
  timezone?: string;
  tags?: string[];
}

export interface CreatePostFormData {
  type: ForumPost['type'];
  title: string;
  description: string;
  tags: string[];
  schedule: ForumPost['schedule'];
  commitmentLevel: ForumPost['commitmentLevel'];
  workingStyle: ForumPost['workingStyle'];
}