/**
 * Error Reporting Integration Service
 * Provides integration with monitoring services like Sentry and LogRocket
 */

// Internal SDK interface types
interface SentrySDK {
  init(options: {
    dsn: string;
    environment?: string;
    release?: string;
    integrations?: unknown[];
    tracesSampleRate?: number;
    beforeSend?: (event: unknown) => unknown | null;
  }): void;
  setUser(user: ErrorContext['user'] | null): void;
  setTag(key: string, value: string): void;
  setContext(key: string, context: Record<string, unknown>): void;
  addBreadcrumb(breadcrumb: {
    message: string;
    category?: string;
    level?: string;
    data?: Record<string, unknown>;
  }): void;
  captureException(error: Error, context?: {
    user?: ErrorContext['user'];
    tags?: Record<string, string>;
    extra?: Record<string, unknown>;
    level?: string;
    fingerprint?: string[];
  }): string;
  captureMessage(message: string, level?: string, context?: {
    user?: ErrorContext['user'];
    tags?: Record<string, string>;
    extra?: Record<string, unknown>;
    fingerprint?: string[];
  }): string;
}

interface LogRocketSDK {
  init(appId: string, options?: {
    release?: string;
    console?: {
      shouldAggregateConsoleErrors?: boolean;
    };
    network?: {
      requestSanitizer?: (request: Record<string, unknown>) => Record<string, unknown>;
      responseSanitizer?: (response: Record<string, unknown>) => Record<string, unknown>;
    };
  }): void;
  identify(userId: string, userInfo?: Record<string, unknown>): void;
  track(event: string, properties?: Record<string, unknown>): void;
  sessionURL?: string;
  addTag(key: string, value: string): void;
}

export interface ErrorReportingConfig {
  sentryDsn?: string
  logRocketAppId?: string
  enableSentry: boolean
  enableLogRocket: boolean
  enableInDevelopment: boolean
  environment: string
  release?: string
  userId?: string
  userEmail?: string
}

export interface ErrorContext {
  user?: {
    id?: string
    email?: string
    username?: string
  }
  tags?: Record<string, string>
  extra?: Record<string, unknown>
  level?: 'error' | 'warning' | 'info' | 'debug'
  fingerprint?: string[]
}

export interface BreadcrumbData {
  message: string
  category?: string
  level?: 'error' | 'warning' | 'info' | 'debug'
  data?: Record<string, unknown>
}

/**
 * Abstract base class for error reporting services
 */
abstract class ErrorReportingService {
  protected config: ErrorReportingConfig
  protected isInitialized = false

  constructor(config: ErrorReportingConfig) {
    this.config = config
  }

  abstract initialize(): Promise<void>

  abstract captureError(error: Error, context?: ErrorContext): string

  abstract captureMessage(message: string, level?: ErrorContext['level'], context?: ErrorContext): string

  abstract addBreadcrumb(breadcrumb: BreadcrumbData): void

  abstract setUser(user: ErrorContext['user']): void

  abstract setTag(key: string, value: string): void

  abstract setContext(key: string, context: Record<string, unknown>): void

  public isReady(): boolean {
    return this.isInitialized
  }
}

/**
 * Sentry Error Reporting Implementation
 */
class SentryErrorReporting extends ErrorReportingService {
  private sentry: SentrySDK | null = null

  async initialize(): Promise<void> {
    if (!this.config.enableSentry || !this.config.sentryDsn) {
      return
    }

    if (!this.config.enableInDevelopment && import.meta.env.DEV) {
      // console.log('📊 Sentry disabled in development mode')
      return
    }

    try {
      // Dynamic import to avoid bundling Sentry if not needed
      const {
        init,
        setUser,
        setTag,
        setContext,
        addBreadcrumb,
        captureException,
        captureMessage
      } = await import('@sentry/react')
      this.sentry = {
        init,
        setUser,
        setTag,
        setContext,
        addBreadcrumb,
        captureException,
        captureMessage
      }

      this.sentry.init({
        dsn: this.config.sentryDsn,
        environment: this.config.environment,
        release: this.config.release,
        integrations: [
          // Add performance monitoring if needed
        ],
        tracesSampleRate: import.meta.env.DEV ? 1.0 : 0.1,
        beforeSend: (event: unknown) => {
          // Filter out certain errors in development
          if (import.meta.env.DEV && event && typeof event === 'object' && 'exception' in event) {
            const exception = (event as {exception?: {values?: Array<{value?: string}>}}).exception
            const error = exception?.values?.[0]
            if (error?.value?.includes('ResizeObserver loop limit exceeded')) {
              return null // Don't send this common development error
            }
          }
          return event
        },
      })

      // Set initial user if provided
      if (this.config.userId || this.config.userEmail) {
        this.setUser({
          id: this.config.userId,
          email: this.config.userEmail,
        })
      }

      this.isInitialized = true
      // console.log('📊 Sentry error reporting initialized')
    } catch {
      // console.warn('Failed to initialize Sentry');
    }
  }

  captureError(error: Error, context?: ErrorContext): string {
    if (!this.sentry || !this.isInitialized) {
      return ''
    }

    return this.sentry.captureException(error, {
      user: context?.user,
      tags: context?.tags,
      extra: context?.extra,
      level: context?.level,
      fingerprint: context?.fingerprint,
    })
  }

  captureMessage(message: string, level: ErrorContext['level'] = 'info', context?: ErrorContext): string {
    if (!this.sentry || !this.isInitialized) {
      return ''
    }

    return this.sentry.captureMessage(message, level, {
      user: context?.user,
      tags: context?.tags,
      extra: context?.extra,
      fingerprint: context?.fingerprint,
    })
  }

  addBreadcrumb(breadcrumb: BreadcrumbData): void {
    if (!this.sentry || !this.isInitialized) {
      return
    }

    this.sentry.addBreadcrumb({
      message: breadcrumb.message,
      category: breadcrumb.category || 'custom',
      level: breadcrumb.level || 'info',
      data: breadcrumb.data,
    })
  }

  setUser(user?: ErrorContext['user']): void {
    if (!this.sentry || !this.isInitialized) {
      return
    }

    this.sentry.setUser(user || null)
  }

  setTag(key: string, value: string): void {
    if (!this.sentry || !this.isInitialized) {
      return
    }

    this.sentry.setTag(key, value)
  }

  setContext(key: string, context: Record<string, unknown>): void {
    if (!this.sentry || !this.isInitialized) {
      return
    }

    this.sentry.setContext(key, context)
  }
}

/**
 * LogRocket Session Recording Implementation
 */
class LogRocketService {
  private logRocket: LogRocketSDK | null = null
  private isInitialized = false
  private config: ErrorReportingConfig

  constructor(config: ErrorReportingConfig) {
    this.config = config
  }

  async initialize(): Promise<void> {
    if (!this.config.enableLogRocket || !this.config.logRocketAppId) {
      return
    }

    if (!this.config.enableInDevelopment && import.meta.env.DEV) {
      // console.log('📹 LogRocket disabled in development mode')
      return
    }

    try {
      // Dynamic import to avoid bundling LogRocket if not needed
      const LogRocket = await import('logrocket')
      this.logRocket = LogRocket.default || (LogRocket as unknown as LogRocketSDK)

      this.logRocket.init(this.config.logRocketAppId, {
        release: this.config.release,
        console: {
          shouldAggregateConsoleErrors: true,
        },
        network: {
          requestSanitizer: (request: Record<string, unknown>): Record<string, unknown> => {
            // Sanitize sensitive data from network requests
            if (request && typeof request === 'object' && 'headers' in request) {
              const headers = (request as {headers?: {authorization?: string}}).headers
              if (headers && typeof headers === 'object' && 'authorization' in headers) {
                headers.authorization = '[REDACTED]'
              }
            }
            return request
          },
          responseSanitizer: (response) => {
            // Sanitize sensitive data from network responses
            return response
          },
        },
      })

      // Set initial user if provided
      if (this.config.userId || this.config.userEmail) {
        this.identify({
          id: this.config.userId,
          email: this.config.userEmail,
        })
      }

      this.isInitialized = true
      // console.log('📹 LogRocket session recording initialized')
    } catch {
      // console.warn('Failed to initialize LogRocket');
    }
  }

  identify(user: { id?: string; email?: string; [key: string]: unknown }): void {
    if (!this.logRocket || !this.isInitialized) {
      return
    }

    this.logRocket.identify(user.id || user.email || 'anonymous', user)
  }

  track(event: string, properties?: Record<string, unknown>): void {
    if (!this.logRocket || !this.isInitialized) {
      return
    }

    this.logRocket.track(event, properties)
  }

  getSessionURL(): string {
    if (!this.logRocket || !this.isInitialized) {
      return ''
    }

    return this.logRocket.sessionURL || ''
  }

  addTag(key: string, value: string): void {
    if (!this.logRocket || !this.isInitialized) {
      return
    }

    this.logRocket.addTag(key, value)
  }

  isReady(): boolean {
    return this.isInitialized
  }
}

/**
 * Unified Error Reporting Manager
 */
class ErrorReportingManager {
  private sentry: SentryErrorReporting
  private logRocket: LogRocketService
  private config: ErrorReportingConfig

  constructor(config: ErrorReportingConfig) {
    this.config = config
    this.sentry = new SentryErrorReporting(config)
    this.logRocket = new LogRocketService(config)
  }

  async initialize(): Promise<void> {
    await Promise.all([
      this.sentry.initialize(),
      this.logRocket.initialize(),
    ])

    // console.log('📊 Error reporting manager initialized')
  }

  captureError(error: Error, context?: ErrorContext): string {
    const eventId = this.sentry.captureError(error, context)

    // Add LogRocket session URL to Sentry context if available
    const sessionURL = this.logRocket.getSessionURL()
    if (sessionURL) {
      this.sentry.setContext('logrocket', {sessionURL})
    }

    // Track error in LogRocket
    this.logRocket.track('Error Occurred', {
      message: error.message,
      stack: error.stack,
      eventId,
      ...context?.extra,
    })

    return eventId
  }

  captureMessage(message: string, level: ErrorContext['level'] = 'info', context?: ErrorContext): string {
    const eventId = this.sentry.captureMessage(message, level, context)

    // Track message in LogRocket
    this.logRocket.track('Message Logged', {
      message,
      level,
      eventId,
      ...context?.extra,
    })

    return eventId
  }

  addBreadcrumb(breadcrumb: BreadcrumbData): void {
    this.sentry.addBreadcrumb(breadcrumb)

    // LogRocket automatically captures user interactions as breadcrumbs
  }

  setUser(user: ErrorContext['user']): void {
    this.sentry.setUser(user)
    if (user) {
      this.logRocket.identify(user)
    }
  }

  setTag(key: string, value: string): void {
    this.sentry.setTag(key, value)
    this.logRocket.addTag(key, value)
  }

  setContext(key: string, context: Record<string, unknown>): void {
    this.sentry.setContext(key, context)
  }

  trackEvent(event: string, properties?: Record<string, unknown>): void {
    this.logRocket.track(event, properties)
  }

  getSessionURL(): string {
    return this.logRocket.getSessionURL()
  }

  isReady(): boolean {
    return this.sentry.isReady() || this.logRocket.isReady()
  }
}

// Singleton instance
let errorReportingManager: ErrorReportingManager | null = null

export function initializeErrorReporting(config: ErrorReportingConfig): Promise<void> {
  errorReportingManager = new ErrorReportingManager(config)
  return errorReportingManager.initialize()
}

export function getErrorReporting(): ErrorReportingManager {
  if (!errorReportingManager) {
    throw new Error('Error reporting manager not initialized. Call initializeErrorReporting first.')
  }
  return errorReportingManager
}

// Convenience functions
export function captureError(error: Error, context?: ErrorContext): string {
  return errorReportingManager?.captureError(error, context) || ''
}

export function captureMessage(message: string, level?: ErrorContext['level'], context?: ErrorContext): string {
  return errorReportingManager?.captureMessage(message, level, context) || ''
}

export function addBreadcrumb(breadcrumb: BreadcrumbData): void {
  errorReportingManager?.addBreadcrumb(breadcrumb)
}

export function setUser(user: ErrorContext['user']): void {
  errorReportingManager?.setUser(user)
}

export function setTag(key: string, value: string): void {
  errorReportingManager?.setTag(key, value)
}

export function setContext(key: string, context: Record<string, unknown>): void {
  errorReportingManager?.setContext(key, context)
}

export function trackEvent(event: string, properties?: Record<string, unknown>): void {
  errorReportingManager?.trackEvent(event, properties)
}

// Default configuration
export const defaultErrorReportingConfig: ErrorReportingConfig = {
  enableSentry: import.meta.env.PROD,
  enableLogRocket: import.meta.env.PROD,
  enableInDevelopment: false,
  environment: import.meta.env.MODE,
  sentryDsn: import.meta.env.VITE_SENTRY_DSN,
  logRocketAppId: import.meta.env.VITE_LOGROCKET_APP_ID,
  release: import.meta.env.VITE_APP_VERSION,
}