/**
 * Simple Logger Utility
 * 
 * Provides a centralized logging system that can be controlled per environment
 * and easily disabled in production if needed.
 */

type LogLevel = 'debug' | 'info' | 'warn' | 'error'

// Removed LogEntry interface as it's not currently used

class Logger {
  private enabledLevels: Set<LogLevel>
  private isDevelopment: boolean

  constructor() {
    this.isDevelopment = import.meta.env.DEV
    
    // In development, enable all logging levels
    // In production, only enable warn and error by default
    this.enabledLevels = new Set(
      this.isDevelopment 
        ? ['debug', 'info', 'warn', 'error']
        : ['warn', 'error']
    )
  }

  private shouldLog(level: LogLevel): boolean {
    return this.enabledLevels.has(level)
  }

  private formatMessage(level: LogLevel, message: string, context?: string, data?: unknown): string {
    const timestamp = new Date().toISOString()
    const prefix = context ? `[${context}]` : ''
    const dataStr = data ? ` ${JSON.stringify(data)}` : ''
    return `${timestamp} [${level.toUpperCase()}] ${prefix} ${message}${dataStr}`
  }

  private log(level: LogLevel, _message: string, _context?: string, _data?: unknown): void {
    if (!this.shouldLog(level)) return

    // Commented out for production - uncomment if needed for debugging
    // const formattedMessage = this.formatMessage(level, _message, _context, _data)
    
    // Route to appropriate console method
    switch (level) {
      case 'error':
        // console.error(formattedMessage)
        break
      case 'warn':
        // console.warn(formattedMessage)
        break
      case 'info':
        // console.info(formattedMessage)
        break
      case 'debug':
        // console.debug(formattedMessage)
        break
    }
  }

  debug(message: string, context?: string, data?: unknown): void {
    this.log('debug', message, context, data)
  }

  info(message: string, context?: string, data?: unknown): void {
    this.log('info', message, context, data)
  }

  warn(message: string, context?: string, data?: unknown): void {
    this.log('warn', message, context, data)
  }

  error(message: string, context?: string, data?: unknown): void {
    this.log('error', message, context, data)
  }

  // Convenience method for errors with Error objects
  logError(error: Error, context?: string, additionalData?: Record<string, unknown>): void {
    this.error(error.message, context, { 
      stack: error.stack, 
      name: error.name, 
      ...(additionalData || {}) 
    })
  }

  // Method to enable/disable specific log levels
  setLogLevel(levels: LogLevel[]): void {
    this.enabledLevels = new Set(levels)
  }

  // Method to check if development logging should be used
  isDev(): boolean {
    return this.isDevelopment
  }
}

// Export singleton instance
export const logger = new Logger()

// Export individual methods for convenient importing
export const { debug, info, warn, error, logError } = logger

// Export the logger class for testing or advanced usage
export { Logger }
export default logger