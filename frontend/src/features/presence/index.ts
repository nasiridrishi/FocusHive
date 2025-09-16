// Export presence service and hooks
export { presenceService } from './services/presenceService';
export type { PresenceService } from './services/presenceService';

// Export hooks
export {
  useUserPresence,
  useHivePresence,
  useActivity,
  useBulkPresence,
  usePresenceSearch,
  usePresenceHistory,
  usePresenceStatistics,
  useCollaboration,
  useAutoPresence,
  useContextPresence,
} from './hooks/usePresence';

// Re-export types from contracts
export type {
  UserPresence,
  HivePresence,
  PresenceStatus,
  ActivityType,
  UserActivity,
  DeviceInfo,
  PresenceUpdate,
  PresenceHeartbeat,
  SetPresenceRequest,
  BulkPresenceRequest,
  BulkPresenceResponse,
  PresenceHistoryEntry,
  PresenceStatistics,
  PresenceWebSocketEvent,
  PresenceSearchParams,
  PresenceNotification,
  PresenceConfig,
  CollaborationSession,
  UserStatusHistory,
} from '@/contracts/presence';