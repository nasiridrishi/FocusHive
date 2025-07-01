// User types
export interface User {
  id: string;
  email: string;
  username: string;
  createdAt: Date;
  updatedAt: Date;
}

// Hive types
export interface Hive {
  id: string;
  name: string;
  description: string;
  ownerId: string;
  memberIds: string[];
  isPublic: boolean;
  createdAt: Date;
  updatedAt: Date;
}

// Presence types
export enum PresenceStatus {
  ONLINE = 'online',
  AWAY = 'away',
  BUSY = 'busy',
  OFFLINE = 'offline'
}

export interface UserPresence {
  userId: string;
  status: PresenceStatus;
  lastActivity: Date;
  currentTask?: string;
}