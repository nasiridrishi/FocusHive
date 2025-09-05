import React, { useCallback } from 'react'
import { useErrorBoundary } from 'react-error-boundary'
import { errorLogger } from '@shared/services/errorLogging'

export interface AsyncErrorOptions {
  context?: Record<string, unknown>
  severity?: 'low' | 'medium' | 'high' | 'critical'
  shouldThrow?: boolean
  logOnly?: boolean
}

/**
 * Custom hook for handling async errors that don't bubble up to React Error Boundaries
 * 
 * React Error Boundaries only catch errors during:
 * - Rendering
 * - Lifecycle methods  
 * - Constructors
 * 
 * They DON'T catch errors in:
 * - Event handlers
 * - Async code (setTimeout, fetch, promises)
 * - Server-side rendering
 * - Errors thrown in the error boundary itself
 * 
 * This hook bridges that gap by providing a way to report async errors
 * to the nearest error boundary while also logging them appropriately.
 */
export const useAsyncError = () => {
  const { showBoundary } = useErrorBoundary()

  /**
   * Capture and handle an async error
   */
  const captureError = useCallback(
    (error: Error | unknown, options: AsyncErrorOptions = {}) => {
      const {
        context = {},
        severity = 'medium',
        shouldThrow = false,
        logOnly = false,
      } = options

      // Ensure we have an Error object
      const errorObj = error instanceof Error 
        ? error 
        : new Error(String(error))

      // Always log the error
      errorLogger.logAsyncError(errorObj, context, severity)

      if (logOnly) {
        // Only log, don't throw to boundary
        console.error('Async error (log only):', errorObj)
        return
      }

      if (shouldThrow) {
        // Throw to nearest error boundary
        showBoundary(errorObj)
      } else {
        // Log but don't crash the UI
        console.error('Async error caught:', errorObj)
      }
    },
    [showBoundary]
  )

  /**
   * Wrap a promise to automatically catch and handle errors
   */
  const wrapPromise = useCallback(
    <T>(
      promise: Promise<T>,
      options: AsyncErrorOptions = {}
    ): Promise<T | null> => {
      return promise.catch((error) => {
        captureError(error, {
          ...options,
          context: { ...options.context, source: 'promise' },
        })
        return null
      })
    },
    [captureError]
  )

  /**
   * Wrap an async function to automatically catch and handle errors
   */
  const wrapAsyncFunction = useCallback(
    <T extends (...args: any[]) => Promise<any>>(
      asyncFn: T,
      options: AsyncErrorOptions = {}
    ): T => {
      return ((...args: Parameters<T>) => {
        return wrapPromise(asyncFn(...args), {
          ...options,
          context: { ...options.context, source: 'async_function' },
        })
      }) as T
    },
    [wrapPromise]
  )

  /**
   * Wrap an event handler to catch and handle errors
   */
  const wrapEventHandler = useCallback(
    <T extends (...args: any[]) => any>(
      handler: T,
      options: AsyncErrorOptions = {}
    ): T => {
      return ((...args: Parameters<T>) => {
        try {
          const result = handler(...args)
          
          // If the handler returns a promise, wrap it
          if (result instanceof Promise) {
            return wrapPromise(result, {
              ...options,
              context: { ...options.context, source: 'event_handler' },
            })
          }
          
          return result
        } catch (error) {
          captureError(error, {
            ...options,
            context: { ...options.context, source: 'event_handler' },
          })
          return null
        }
      }) as T
    },
    [captureError, wrapPromise]
  )

  /**
   * Safe API call wrapper
   */
  const safeApiCall = useCallback(
    async <T>(
      apiCall: () => Promise<T>,
      options: AsyncErrorOptions = {}
    ): Promise<T | null> => {
      try {
        return await apiCall()
      } catch (error) {
        captureError(error, {
          severity: 'high',
          ...options,
          context: { 
            ...options.context, 
            source: 'api_call',
            timestamp: new Date().toISOString(),
          },
        })
        return null
      }
    },
    [captureError]
  )

  /**
   * Safe WebSocket error handler
   */
  const handleWebSocketError = useCallback(
    (_wsEvent: Event, wsUrl?: string, options: AsyncErrorOptions = {}) => {
      captureError(new Error(`WebSocket error: ${wsUrl || 'unknown'}`), {
        severity: 'high',
        ...options,
        context: {
          ...options.context,
          source: 'websocket',
          wsUrl,
          timestamp: new Date().toISOString(),
        },
      })
    },
    [captureError]
  )

  return {
    captureError,
    wrapPromise,
    wrapAsyncFunction,
    wrapEventHandler,
    safeApiCall,
    handleWebSocketError,
  }
}

/**
 * HOC for wrapping components with async error handling
 */
export const withAsyncErrorHandling = <P extends object>(
  Component: React.ComponentType<P>
) => {
  const WrappedComponent = (props: P) => {
    const asyncError = useAsyncError()
    
    // Inject the async error handler into props
    const enhancedProps = {
      ...props,
      asyncError,
    } as P & { asyncError: ReturnType<typeof useAsyncError> }

    return React.createElement(Component, enhancedProps)
  }

  WrappedComponent.displayName = `withAsyncErrorHandling(${Component.displayName || Component.name})`
  
  return WrappedComponent
}

export default useAsyncError