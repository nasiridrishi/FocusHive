/**
 * Timer feature hooks exports
 * Provides all timer-related React hooks for the application
 */

// Core timer hooks
export {
  useTimer,
} from './useTimer';

// Pomodoro technique hooks
export {
  usePomodoro,
  usePomodoroStats,
} from './usePomodoro';

// Timer template hooks
export {
  useTimerTemplates,
  useTimerTemplate,
  useCreateTimerTemplate,
  useUpdateTimerTemplate,
  useDeleteTimerTemplate,
  useStartTimerFromTemplate,
  useFavoriteTemplates,
  useToggleTemplateFavorite,
  useTemplateCategories,
  useTimerTemplateActions,
} from './useTimerTemplates';

// Timer goal hooks
export {
  useTimerGoals,
  useTimerGoal,
  useCreateTimerGoal,
  useUpdateTimerGoal,
  useDeleteTimerGoal,
  useGoalProgress,
  useGoalAchievements,
  useGoalRecommendations,
  useTimerGoalActions,
} from './useTimerGoals';

// Timer analytics hooks
export {
  useTimerAnalytics,
  useTimerComparison,
  useTimerInsights,
  type AnalyticsPeriod,
} from './useTimerAnalytics';

// Re-export types for convenience
export type {
  TimerSession,
  CreateTimerRequest,
  TimerStats,
  TimerPreferences,
  TimerTemplate,
  CreateTimerTemplateRequest,
  UpdateTimerTemplateRequest,
  TimerGoal,
  CreateTimerGoalRequest,
  UpdateTimerGoalRequest,
  SessionType,
  SessionStatus,
} from '@/contracts/timer';