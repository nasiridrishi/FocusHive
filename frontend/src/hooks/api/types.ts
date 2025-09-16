/**
 * Extended types for the frontend that include computed properties
 * These types extend the base types from shared/types with computed properties added by transformers
 */

import type {PresenceStatus, UserPresence as BaseUserPresence} from '../../shared/types/presence';
import type {Hive as BaseHive} from '../../shared/types/hive';
import type {User as BaseUser} from '../../shared/types/auth';

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
export type {PresenceStatus};

// ============================================================================
// REACT QUERY HOOK TYPES
// ============================================================================

import type { 
  UseQueryResult, 
  UseInfiniteQueryResult, 
  UseMutationResult,
  InfiniteData
} from '@tanstack/react-query';

/**
 * Standard query result for single data fetching
 */
export type QueryResult<TData = unknown, TError = unknown> = UseQueryResult<TData, TError>;

/**
 * Infinite query result for paginated data fetching
 */
export type InfiniteQueryResult<TData = unknown, TError = unknown> = UseInfiniteQueryResult<
  InfiniteData<TData>,
  TError
>;

/**
 * Mutation result for data modification operations
 */
export type MutationResult<TData = unknown, TError = unknown, TVariables = void, TContext = unknown> = 
  UseMutationResult<TData, TError, TVariables, TContext>;

// ============================================================================
// COMPOUND HOOK RESULT TYPES
// ============================================================================

/**
 * Combined result from multiple queries for detailed views
 */
export interface DetailedQueryResult<TMain, TSecondary = unknown> {
  // Main data
  main: TMain | undefined;
  secondary: TSecondary | undefined;

  // Loading states
  isLoading: boolean;
  isMainLoading: boolean;
  isSecondaryLoading: boolean;

  // Error states
  error: Error | null;
  mainError: Error | null;
  secondaryError: Error | null;

  // Utilities
  refetchMain: () => void;
  refetchSecondary: () => void;
  refetchAll: () => void;
}

/**
 * Management operations result combining multiple mutations
 */
export interface ManagementResult<
  TCreate = unknown,
  TUpdate = unknown
> {
  // Mutation functions
  create: (data: TCreate) => void;
  update: (data: TUpdate) => void;
  delete: (id: string | number) => void;

  // Loading states
  isCreating: boolean;
  isUpdating: boolean;
  isDeleting: boolean;

  // Error states
  createError: Error | null;
  updateError: Error | null;
  deleteError: Error | null;

  // Success states
  isCreateSuccess: boolean;
  isUpdateSuccess: boolean;
  isDeleteSuccess: boolean;
}

// ============================================================================
// HOOK-SPECIFIC RESULT TYPES
// ============================================================================

/**
 * Hive queries result types
 */
export type HiveListResult = QueryResult<{
  content: Hive[];
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}>;

export type HiveResult = QueryResult<Hive>;
export type HiveSearchResult = QueryResult<Hive[]>;

/**
 * Hive mutations result types
 */
export type CreateHiveMutation = MutationResult<
  Hive,
  unknown,
  { name: string; description?: string; isPublic: boolean }
>;

export type UpdateHiveMutation = MutationResult<
  Hive,
  unknown,
  { hiveId: string; updates: {
    name?: string;
    description?: string;
    isPrivate?: boolean;
    maxMembers?: number;
    tags?: string[];
    settings?: Partial<{
      allowChat?: boolean;
      allowVoice?: boolean;
      requireApproval?: boolean;
      focusMode?: 'pomodoro' | 'continuous' | 'flexible';
      defaultSessionLength?: number;
      maxSessionLength?: number;
    }>;
  }}
>;

export type DeleteHiveMutation = MutationResult<void, unknown, string>;
export type JoinHiveMutation = MutationResult<Hive, unknown, string>;
export type LeaveHiveMutation = MutationResult<void, unknown, string>;

/**
 * Presence queries result types
 */
export type PresenceResult = QueryResult<UserPresence>;
export type HivePresenceResult = QueryResult<UserPresence[]>;

/**
 * Presence mutations result types
 */
export type UpdatePresenceMutation = MutationResult<
  UserPresence,
  unknown,
  Partial<UserPresence>
>;

// ============================================================================
// TYPE UTILITIES
// ============================================================================

/**
 * Extract data type from a query result
 */
export type ExtractQueryData<T> = T extends QueryResult<infer U, unknown> ? U : never;

/**
 * Extract error type from a query result
 */
export type ExtractQueryError<T> = T extends QueryResult<unknown, infer E> ? E : never;

/**
 * Helper to create consistent compound hook results
 */
export type CreateDetailedResult<TMain, TSecondary = unknown> = DetailedQueryResult<TMain, TSecondary>;

/**
 * Helper to create management hook results
 */
export type CreateManagementResult<TCreate = unknown, TUpdate = unknown> = 
  ManagementResult<TCreate, TUpdate>;
