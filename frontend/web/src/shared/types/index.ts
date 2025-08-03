// Re-export all types for convenient importing
export * from './auth'
export * from './hive'
export * from './presence'
export * from './chat'
export * from './timer'
export * from './common'

// Grouped exports for specific domains
export type {
  User,
  AuthState,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  AuthContextType
} from './auth'

export type {
  Hive,
  HiveMember,
  HiveSettings,
  CreateHiveRequest,
  UpdateHiveRequest,
  JoinHiveRequest,
  HiveInvitation,
  HiveStats
} from './hive'

export type {
  PresenceStatus,
  UserPresence,
  PresenceUpdate,
  HivePresenceInfo,
  FocusSession,
  SessionBreak,
  PresenceContextType
} from './presence'

export type {
  ChatMessage,
  MessageReaction,
  SendMessageRequest,
  TypingIndicator,
  ChatState,
  ChatContextType,
  MessageInputProps,
  MessageListProps
} from './chat'

export type {
  TimerSettings,
  TimerState,
  SessionStats,
  SessionGoal,
  ProductivityChart,
  WeeklyStats,
  DailyStats,
  TimerContextType,
  FocusTimerProps,
  SessionStatsProps,
  ProductivityChartProps
} from './timer'

export type {
  ApiResponse,
  ApiError,
  PaginatedResponse,
  QueryOptions,
  LoadingState,
  FormErrors,
  FormField,
  ModalProps,
  ConfirmDialogProps,
  NotificationData,
  UserPreferences,
  Theme
} from './common'