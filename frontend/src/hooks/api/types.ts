/**
 * Extended types for the frontend that include computed properties
 * These types extend the base types from shared/types with computed properties added by transformers
 */

import type { 
  UserPresence as BaseUserPresence, 
  PresenceStatus 
} from '../../shared/types/presence';
import type { 
  Hive as BaseHive
} from '../../shared/types/hive';
import type { 
  User as BaseUser 
} from '../../shared/types/auth';

// Extended UserPresence with computed properties
export interface UserPresence extends BaseUserPresence {
  // Computed properties added by transformPresenceDTO
  isActive: boolean;
  isOnline: boolean;
  isFocusing: boolean;
  isCurrentUser: boolean;
  lastSeenFormatted: string;
  statusDisplayText: string;
  activityDisplayText: string;
}

// Define our own membership status type
export type HiveMembershipStatus = 'owner' | 'member' | 'not_member' | 'pending' | 'banned';

// Extended Hive with computed properties
export interface Hive extends BaseHive {
  // Computed properties added by transformHiveDTO
  isOwner: boolean;
  isMember: boolean;
  isFull: boolean;
  hasSpots: boolean;
  spotsRemaining: number;
  membershipStatus: HiveMembershipStatus;
  displayName: string;
  shortDescription: string;
}

// Extended User with computed properties
export interface User extends BaseUser {
  // Computed properties added by transformUserDTO
  initials: string;
  displayNameOrUsername: string;
  isOnline?: boolean;
  lastSeenFormatted?: string;
  profileUrl?: string;
}

// Re-export other types for convenience
export type { PresenceStatus };