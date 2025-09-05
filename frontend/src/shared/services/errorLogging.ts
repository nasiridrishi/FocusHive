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
}

export interface ErrorLoggerConfig {
  maxLogs: number
  enableConsoleLogging: boolean
  enableRemoteLogging: boolean
  remoteEndpoint?: string
  apiKey?: string
}

class ErrorLoggingService {
  private config: ErrorLoggerConfig
  private errors: LoggedError[] = []
  private sessionId: string

  constructor(config: Partial<ErrorLoggerConfig> = {}) {
    this.config = {
      maxLogs: 100,
      enableConsoleLogging: true,
      enableRemoteLogging: false,
      ...config,
    }
    this.sessionId = this.generateSessionId()
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
    }

    // Store error locally
    this.storeError(loggedError)

    // Console logging for development
    if (this.config.enableConsoleLogging) {
      this.logToConsole(loggedError)
    }

    // Remote logging for production
    if (this.config.enableRemoteLogging) {
      this.logToRemote(loggedError)
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
    this.logError(error, undefined, { ...context, source: 'async' }, severity)
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

  private storeError(error: LoggedError): void {
    this.errors.push(error)

    // Maintain max logs limit
    if (this.errors.length > this.config.maxLogs) {
      this.errors.shift()
    }

    // Store in localStorage for persistence across sessions
    try {
      localStorage.setItem('focushive_error_logs', JSON.stringify(this.getErrorLogs(10)))
    } catch (e) {
      console.warn('Failed to store error logs in localStorage:', e)
    }
  }

  private logToConsole(error: LoggedError): void {
    const isDevelopment = import.meta.env.DEV
    
    if (isDevelopment) {
      console.group(`🔴 Error [${error.severity.toUpperCase()}] - ${error.id}`)
      console.error('Message:', error.message)
      console.error('Stack:', error.stack)
      if (error.componentStack) {
        console.error('Component Stack:', error.componentStack)
      }
      if (error.context) {
        console.error('Context:', error.context)
      }
      console.error('Full Error:', error)
      console.groupEnd()
    } else {
      // Production: Simplified logging
      console.error(`Error ${error.id}: ${error.message}`)
    }
  }

  private async logToRemote(error: LoggedError): Promise<void> {
    if (!this.config.remoteEndpoint) {
      console.warn('Remote logging enabled but no endpoint configured')
      return
    }

    try {
      const response = await fetch(this.config.remoteEndpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(this.config.apiKey && { 'Authorization': `Bearer ${this.config.apiKey}` }),
        },
        body: JSON.stringify({
          error,
          environment: import.meta.env.MODE,
          timestamp: error.timestamp.toISOString(),
        }),
      })

      if (!response.ok) {
        console.warn('Failed to log error remotely:', response.status, response.statusText)
      }
    } catch (e) {
      console.warn('Failed to send error to remote logging service:', e)
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
  maxLogs: 50,
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
    { ...errorInfo, errorBoundary: boundaryName },
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
    { source: 'unhandledrejection', reason: event.reason },
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