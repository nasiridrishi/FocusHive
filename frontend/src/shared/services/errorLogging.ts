/**
 * Error Logging Service for React Error Boundaries
 * Provides centralized error reporting and logging with development/production modes
 */

export interface ErrorInfo {
  componentStack: string
  errorBoundary?: string
}

export interface LoggedError {
  id: string
  message: string
  stack: string
  timestamp: Date
  url: string
  userAgent: string
  userId?: string
  sessionId: string
  componentStack?: string
  errorBoundary?: string
  severity: 'low' | 'medium' | 'high' | 'critical'
  context?: Record<string, unknown>
  retryCount?: number
  synced?: boolean
}

export interface ErrorLoggerConfig {
  maxLogs: number
  enableConsoleLogging: boolean
  enableRemoteLogging: boolean
  remoteEndpoint?: string
  apiKey?: string
  enableOfflineStorage: boolean
  maxRetries: number
  retryDelay: number
  batchSize: number
  flushInterval: number
  enableMonitoringIntegration: boolean
}

class ErrorLoggingService {
  private config: ErrorLoggerConfig
  private errors: LoggedError[] = []
  private sessionId: string
  private offlineQueue: LoggedError[] = []
  private isOnline: boolean = navigator.onLine
  private flushTimer: NodeJS.Timeout | null = null

  constructor(config: Partial<ErrorLoggerConfig> = {}) {
    this.config = {
      maxLogs: 100,
      enableConsoleLogging: true,
      enableRemoteLogging: false,
      enableOfflineStorage: true,
      maxRetries: 3,
      retryDelay: 1000,
      batchSize: 10,
      flushInterval: 30000, // 30 seconds
      enableMonitoringIntegration: true,
      ...config,
    }
    this.sessionId = this.generateSessionId()
    this.initializeOfflineSupport()
  }

  /**
   * Log an error with full context information
   */
  public logError(
      error: Error,
      errorInfo?: ErrorInfo,
      context: Record<string, unknown> = {},
      severity: LoggedError['severity'] = 'medium'
  ): void {
    const loggedError: LoggedError = {
      id: this.generateErrorId(),
      message: error.message,
      stack: error.stack || 'No stack trace available',
      timestamp: new Date(),
      url: window.location.href,
      userAgent: navigator.userAgent,
      sessionId: this.sessionId,
      componentStack: errorInfo?.componentStack,
      errorBoundary: errorInfo?.errorBoundary,
      severity,
      context,
      retryCount: 0,
      synced: false,
    }

    // Store error locally
    this.storeError(loggedError)

    // Console logging for development
    if (this.config.enableConsoleLogging) {
      this.logToConsole(loggedError)
    }

    // Send to monitoring services (Sentry, LogRocket)
    if (this.config.enableMonitoringIntegration) {
      this.logToMonitoringServices(error, errorInfo, context, severity)
    }

    // Remote logging for production
    if (this.config.enableRemoteLogging) {
      if (this.isOnline) {
        this.logToRemoteWithRetry(loggedError).catch(_remoteError => {
          // console.warn('Failed to log error remotely, adding to offline queue:', _remoteError)
          this.addToOfflineQueue(loggedError)
        })
      } else {
        this.addToOfflineQueue(loggedError)
      }
    }
  }

  /**
   * Log async errors that don't bubble up to error boundaries
   */
  public logAsyncError(
      error: Error,
      context: Record<string, unknown> = {},
      severity: LoggedError['severity'] = 'medium'
  ): void {
    this.logError(error, undefined, {...context, source: 'async'}, severity)
  }

  /**
   * Log network/API errors
   */
  public logNetworkError(
      error: Error,
      endpoint: string,
      method: string,
      status?: number,
      context: Record<string, unknown> = {}
  ): void {
    this.logError(
        error,
        undefined,
        {
          ...context,
          source: 'network',
          endpoint,
          method,
          status,
        },
        'high'
    )
  }

  /**
   * Get recent error logs
   */
  public getErrorLogs(limit?: number): LoggedError[] {
    return limit ? this.errors.slice(-limit) : [...this.errors]
  }

  /**
   * Clear all stored errors
   */
  public clearLogs(): void {
    this.errors = []
  }

  /**
   * Get offline queue status
   */
  public getOfflineQueueStatus(): {
    queueSize: number
    isOnline: boolean
    nextSyncAttempt: Date | null
  } {
    return {
      queueSize: this.offlineQueue.length,
      isOnline: this.isOnline,
      nextSyncAttempt: this.offlineQueue.length > 0 && this.isOnline
          ? new Date(Date.now() + this.config.flushInterval)
          : null,
    }
  }

  /**
   * Manually trigger offline queue sync
   */
  public async syncOfflineErrors(): Promise<void> {
    if (!this.isOnline) {
      throw new Error('Cannot sync while offline')
    }
    await this.flushOfflineQueue()
  }

  /**
   * Clear offline queue (for testing or manual cleanup)
   */
  public clearOfflineQueue(): void {
    this.offlineQueue = []
    this.saveOfflineQueue()
  }

  /**
   * Get unsynced errors count by severity
   */
  public getUnsyncedErrorsBySeverity(): Record<LoggedError['severity'], number> {
    return this.offlineQueue.reduce((acc, error) => {
      acc[error.severity] = (acc[error.severity] || 0) + 1
      return acc
    }, {} as Record<LoggedError['severity'], number>)
  }

  /**
   * Get error statistics
   */
  public getErrorStats(): {
    total: number
    bySeverity: Record<LoggedError['severity'], number>
    byHour: Record<string, number>
  } {
    const bySeverity = this.errors.reduce(
        (acc, error) => {
          acc[error.severity] = (acc[error.severity] || 0) + 1
          return acc
        },
        {} as Record<LoggedError['severity'], number>
    )

    const byHour = this.errors.reduce((acc, error) => {
      const hour = error.timestamp.toISOString().slice(0, 13)
      acc[hour] = (acc[hour] || 0) + 1
      return acc
    }, {} as Record<string, number>)

    return {
      total: this.errors.length,
      bySeverity,
      byHour,
    }
  }

  /**
   * Initialize offline support and event listeners
   */
  private initializeOfflineSupport(): void {
    // Load offline queue from localStorage
    this.loadOfflineQueue()

    // Listen for online/offline events
    window.addEventListener('online', this.handleOnline.bind(this))
    window.addEventListener('offline', this.handleOffline.bind(this))

    // Start periodic flush timer
    this.startFlushTimer()

    // Attempt to sync on initialization if online
    if (this.isOnline) {
      this.flushOfflineQueue()
    }
  }

  private handleOnline(): void {
    this.isOnline = true
    // console.log('📶 Network connection restored - syncing offline errors')
    this.flushOfflineQueue()
  }

  private handleOffline(): void {
    this.isOnline = false
    // console.log('📵 Network connection lost - errors will be queued for sync')
  }

  private startFlushTimer(): void {
    if (this.flushTimer) {
      clearInterval(this.flushTimer)
    }

    this.flushTimer = setInterval(() => {
      if (this.isOnline && this.offlineQueue.length > 0) {
        this.flushOfflineQueue()
      }
    }, this.config.flushInterval)
  }

  private loadOfflineQueue(): void {
    if (!this.config.enableOfflineStorage) return

    try {
      const stored = localStorage.getItem('focushive_offline_errors')
      if (stored) {
        const parsed = JSON.parse(stored) as LoggedError[]
        // Convert timestamp strings back to Date objects
        this.offlineQueue = parsed.map(error => ({
          ...error,
          timestamp: new Date(error.timestamp),
          synced: false,
          retryCount: error.retryCount || 0,
        }))
        // console.log(`📋 Loaded ${this.offlineQueue.length} offline errors for sync`)
      }
    } catch {
      // console.warn('Failed to load offline error queue')
      this.offlineQueue = []
    }
  }

  private saveOfflineQueue(): void {
    if (!this.config.enableOfflineStorage) return

    try {
      localStorage.setItem('focushive_offline_errors', JSON.stringify(this.offlineQueue))
    } catch {
      // console.warn('Failed to save offline error queue')
    }
  }

  private async flushOfflineQueue(): Promise<void> {
    if (!this.isOnline || this.offlineQueue.length === 0) return

    // console.log(`🔄 Attempting to sync ${this.offlineQueue.length} offline errors`)

    // Process in batches
    const batch = this.offlineQueue.splice(0, this.config.batchSize)

    for (const error of batch) {
      try {
        await this.logToRemoteWithRetry(error)
        // Mark as synced (will be removed from queue)
      } catch {
        // Re-add to queue if retry limit not exceeded
        if ((error.retryCount || 0) < this.config.maxRetries) {
          error.retryCount = (error.retryCount || 0) + 1
          this.offlineQueue.push(error)
        } else {
          // console.warn(`❌ Failed to sync error ${error.id} after ${this.config.maxRetries} retries`)
        }
      }
    }

    // Save updated queue
    this.saveOfflineQueue()

    // Continue with remaining errors if any
    if (this.offlineQueue.length > 0) {
      setTimeout(() => this.flushOfflineQueue(), this.config.retryDelay)
    }
  }

  private storeError(error: LoggedError): void {
    this.errors.push(error)

    // Maintain max logs limit
    if (this.errors.length > this.config.maxLogs) {
      this.errors.shift()
    }

    // Store in localStorage for persistence across sessions
    try {
      localStorage.setItem('focushive_error_logs', JSON.stringify(this.getErrorLogs(10)))
    } catch {
      // console.warn('Failed to store error logs in localStorage')
    }
  }

  private async logToMonitoringServices(
      error: Error,
      errorInfo?: ErrorInfo,
      context: Record<string, unknown> = {},
      severity: LoggedError['severity'] = 'medium'
  ): Promise<void> {
    try {
      // Dynamic import to avoid bundling monitoring services if not needed
      const {getErrorReporting} = await import('../../services/monitoring')
      const errorReporting = getErrorReporting()

      if (errorReporting.isReady()) {
        // Add breadcrumb for context
        errorReporting.addBreadcrumb({
          message: 'Error logged via ErrorLoggingService',
          category: 'error-logging',
          level: severity === 'critical' ? 'error' : 'warning',
          data: {
            errorBoundary: errorInfo?.errorBoundary,
            url: window.location.href,
            timestamp: new Date().toISOString(),
          },
        })

        // Capture the error with context
        const _eventId = errorReporting.captureError(error, {
          tags: {
            errorBoundary: errorInfo?.errorBoundary || 'unknown',
            severity,
            source: 'error-logging-service',
          },
          extra: {
            ...context,
            componentStack: errorInfo?.componentStack,
            sessionId: this.sessionId,
            userAgent: navigator.userAgent,
          },
          level: severity === 'critical' ? 'error' : 'warning',
        })

        // console.log(`📊 Error sent to monitoring services: ${eventId}`)
      }
    } catch {
      // Don't throw if monitoring fails - log silently
      // console.warn('Failed to send error to monitoring services')
    }
  }

  private logToConsole(error: LoggedError): void {
    const isDevelopment = import.meta.env.DEV

    if (isDevelopment) {
      // console.group(`🔴 Error [${error.severity.toUpperCase()}] - ${error.id}`)
      // console.error('Message:', error.message)
      // console.error('Stack:', error.stack)
      if (error.componentStack) {
        // console.error('Component Stack:', error.componentStack)
      }
      if (error.context) {
        // console.error('Context:', error.context)
      }
      // console.error('Full Error:', error)
      // console.groupEnd()
    } else {
      // Production: Simplified logging
      // console.error(`Error ${error.id}: ${error.message}`)
    }
  }

  private addToOfflineQueue(error: LoggedError): void {
    if (!this.config.enableOfflineStorage) return

    this.offlineQueue.push(error)
    this.saveOfflineQueue()
    // console.log(`📫 Added error ${error.id} to offline queue (${this.offlineQueue.length} total)`)
  }

  private async logToRemoteWithRetry(error: LoggedError): Promise<void> {
    let lastError: Error = new Error('Unknown error')

    for (let attempt = 0; attempt <= this.config.maxRetries; attempt++) {
      try {
        await this.logToRemote(error)
        error.synced = true
        return // Success
      } catch (err) {
        lastError = err instanceof Error ? err : new Error(String(err))

        if (attempt < this.config.maxRetries) {
          const delay = this.config.retryDelay * Math.pow(2, attempt) // Exponential backoff
          // console.warn(`🔄 Retry ${attempt + 1}/${this.config.maxRetries} for error ${error.id} in ${delay}ms`)
          await new Promise(resolve => setTimeout(resolve, delay))
        }
      }
    }

    // All retries failed
    throw lastError
  }

  private async logToRemote(error: LoggedError): Promise<void> {
    if (!this.config.remoteEndpoint) {
      // console.warn('Remote logging enabled but no endpoint configured')
      return
    }

    try {
      const response = await fetch(this.config.remoteEndpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(this.config.apiKey && {'Authorization': `Bearer ${this.config.apiKey}`}),
        },
        body: JSON.stringify({
          error,
          environment: import.meta.env.MODE,
          timestamp: error.timestamp.toISOString(),
        }),
      })

      if (!response.ok) {
        // console.warn('Failed to log error remotely:', response.status, response.statusText)
      }
    } catch {
      // console.warn('Failed to send error to remote logging service')
    }
  }

  private generateSessionId(): string {
    return `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }

  private generateErrorId(): string {
    return `error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }
}

// Singleton instance for the application
export const errorLogger = new ErrorLoggingService({
  enableConsoleLogging: true,
  enableRemoteLogging: import.meta.env.PROD, // Enable in production
  enableOfflineStorage: true,
  enableMonitoringIntegration: true,
  maxLogs: 50,
  maxRetries: 3,
  retryDelay: 1000,
  batchSize: 10,
  flushInterval: 30000, // 30 seconds
  // remoteEndpoint: import.meta.env.VITE_ERROR_LOGGING_ENDPOINT,
  // apiKey: import.meta.env.VITE_ERROR_LOGGING_API_KEY,
})

/**
 * Convenience function for logging React Error Boundary errors
 */
export const logErrorBoundaryError = (
    error: Error,
    errorInfo: ErrorInfo,
    boundaryName: string,
    context: Record<string, unknown> = {}
): void => {
  errorLogger.logError(
      error,
      {...errorInfo, errorBoundary: boundaryName},
      context,
      'high'
  )
}

/**
 * Global error handler for unhandled promise rejections
 */
window.addEventListener('unhandledrejection', (event) => {
  errorLogger.logAsyncError(
      new Error(`Unhandled Promise Rejection: ${event.reason}`),
      {source: 'unhandledrejection', reason: event.reason},
      'critical'
  )
})

/**
 * Global error handler for uncaught exceptions
 */
window.addEventListener('error', (event) => {
  errorLogger.logAsyncError(
      new Error(event.message),
      {
        source: 'uncaught',
        filename: event.filename,
        lineno: event.lineno,
        colno: event.colno,
      },
      'critical'
  )
})

export default errorLogger