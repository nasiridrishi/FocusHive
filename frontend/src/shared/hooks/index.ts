/**
 * Responsive Hooks Exports
 *
 * Central export point for all responsive and utility hooks
 */

// Main responsive hooks
export {
  useResponsive,
  useBreakpoint,
  useBreakpointBetween,
  useDeviceType,
  useResponsiveSpacing,
  useResponsiveTypography,
  useOrientation,
  useScrollDirection,
  useReducedMotion,
} from './useResponsive'

// Container query hooks
export {
  useContainerQuery,
  useContainerAspectRatio,
  useContainerColumns,
  useContainerGrid,
  useContainerTextScale,
} from './useContainerQuery'

// Utility hooks
export {
  useViewportVisibility,
  useLazyImage,
  useAdaptiveLoading,
  useSmartLoading,
  useResponsiveGrid,
  useTouchGestures,
  useResponsiveModal,
  useDynamicViewportHeight,
} from './useResponsiveUtils'

// Async state management hooks
export {
  useAsync,
  useAsyncData,
  useAsyncSubmit,
  type AsyncState,
  type UseAsyncOptions,
  type UseAsyncReturn
} from './useAsync'

// Error handling hooks
export {
  useAsyncError,
  withAsyncErrorHandling,
  type AsyncErrorOptions
} from './useAsyncError'

// API hooks with loading states - TODO: implement when API services are ready
// export {
//   useApi,
//   useAuthApi,
//   useHiveApi,
//   usePresenceApi,
//   useTimerApi,
//   useAnalyticsApi
// } from './useApiHooks'