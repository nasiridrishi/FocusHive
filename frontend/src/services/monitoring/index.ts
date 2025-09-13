export {
  initializeErrorReporting,
  getErrorReporting,
  captureError,
  captureMessage,
  addBreadcrumb,
  setUser,
  setTag,
  setContext,
  trackEvent,
  defaultErrorReportingConfig,
} from './errorReporting'

export type {
  ErrorReportingConfig,
  ErrorContext,
  BreadcrumbData,
} from './errorReporting'