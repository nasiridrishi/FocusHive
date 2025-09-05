import { renderHook, act } from '@testing-library/react'
import { useAsyncError } from './useAsyncError'
import { errorLogger } from '@shared/services/errorLogging'
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest'

// Mock the error logger
vi.mock('@shared/services/errorLogging', () => ({
  errorLogger: {
    logAsyncError: vi.fn(),
  },
}))

// Mock react-error-boundary
vi.mock('react-error-boundary', () => ({
  useErrorBoundary: () => ({
    showBoundary: vi.fn(),
  }),
}))

describe('useAsyncError', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Suppress console.error during tests
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('captureError', () => {
    it('logs async errors with default options', () => {
      const { result } = renderHook(() => useAsyncError())
      const testError = new Error('Test async error')

      act(() => {
        result.current.captureError(testError)
      })

      expect(errorLogger.logAsyncError).toHaveBeenCalledWith(
        testError,
        {},
        'medium'
      )
    })

    it('logs async errors with custom context and severity', () => {
      const { result } = renderHook(() => useAsyncError())
      const testError = new Error('Critical error')
      const context = { userId: '123', action: 'save' }

      act(() => {
        result.current.captureError(testError, {
          context,
          severity: 'critical',
        })
      })

      expect(errorLogger.logAsyncError).toHaveBeenCalledWith(
        testError,
        context,
        'critical'
      )
    })

    it('converts non-Error objects to Error objects', () => {
      const { result } = renderHook(() => useAsyncError())

      act(() => {
        result.current.captureError('String error')
      })

      expect(errorLogger.logAsyncError).toHaveBeenCalledWith(
        expect.any(Error),
        {},
        'medium'
      )

      const loggedError = (errorLogger.logAsyncError as ReturnType<typeof vi.fn>).mock.calls[0][0]
      expect(loggedError.message).toBe('String error')
    })

    it('only logs when logOnly option is true', () => {
      const { result } = renderHook(() => useAsyncError())
      const testError = new Error('Log only error')

      act(() => {
        result.current.captureError(testError, { logOnly: true })
      })

      expect(errorLogger.logAsyncError).toHaveBeenCalledWith(
        testError,
        {},
        'medium'
      )
      expect(console.error).toHaveBeenCalledWith('Async error (log only):', testError)
    })
  })

  describe('wrapPromise', () => {
    it('returns resolved value for successful promises', async () => {
      const { result } = renderHook(() => useAsyncError())
      const successPromise = Promise.resolve('success')

      const wrappedPromise = result.current.wrapPromise(successPromise)
      const value = await wrappedPromise

      expect(value).toBe('success')
    })

    it('catches and logs rejected promises', async () => {
      const { result } = renderHook(() => useAsyncError())
      const testError = new Error('Promise rejection')
      const failurePromise = Promise.reject(testError)

      const wrappedPromise = result.current.wrapPromise(failurePromise, {
        context: { source: 'api' },
        severity: 'high',
      })
      const value = await wrappedPromise

      expect(value).toBeNull()
      expect(errorLogger.logAsyncError).toHaveBeenCalledWith(
        testError,
        expect.objectContaining({ source: 'promise' }),
        'high'
      )
    })
  })

  describe('wrapAsyncFunction', () => {
    it('wraps async functions to catch errors', async () => {
      const { result } = renderHook(() => useAsyncError())
      const asyncFunction = async (input: string) => {
        if (input === 'error') {
          throw new Error('Function error')
        }
        return `Result: ${input}`
      }

      const wrappedFunction = result.current.wrapAsyncFunction(asyncFunction, {
        context: { type: 'user_action' },
      })

      // Test successful call
      const successResult = await wrappedFunction('success')
      expect(successResult).toBe('Result: success')

      // Test error call
      const errorResult = await wrappedFunction('error')
      expect(errorResult).toBeNull()
      expect(errorLogger.logAsyncError).toHaveBeenCalledWith(
        expect.any(Error),
        { source: 'async_function', type: 'user_action' },
        'medium'
      )
    })
  })

  describe('wrapEventHandler', () => {
    it('wraps synchronous event handlers', () => {
      const { result } = renderHook(() => useAsyncError())
      const eventHandler = (data: string) => {
        if (data === 'error') {
          throw new Error('Handler error')
        }
        return `Handled: ${data}`
      }

      const wrappedHandler = result.current.wrapEventHandler(eventHandler)

      // Test successful handling
      const successResult = wrappedHandler('success')
      expect(successResult).toBe('Handled: success')

      // Test error handling
      const errorResult = wrappedHandler('error')
      expect(errorResult).toBeNull()
      expect(errorLogger.logAsyncError).toHaveBeenCalledWith(
        expect.any(Error),
        { source: 'event_handler' },
        'medium'
      )
    })

    it('wraps asynchronous event handlers', async () => {
      const { result } = renderHook(() => useAsyncError())
      const asyncEventHandler = async (data: string) => {
        if (data === 'error') {
          throw new Error('Async handler error')
        }
        return `Async handled: ${data}`
      }

      const wrappedHandler = result.current.wrapEventHandler(asyncEventHandler)

      // Test successful async handling
      const successResult = await wrappedHandler('success')
      expect(successResult).toBe('Async handled: success')

      // Test async error handling
      const errorResult = await wrappedHandler('error')
      expect(errorResult).toBeNull()
      expect(errorLogger.logAsyncError).toHaveBeenCalled()
    })
  })

  describe('safeApiCall', () => {
    it('executes API calls successfully', async () => {
      const { result } = renderHook(() => useAsyncError())
      const apiCall = async () => ({ data: 'success' })

      const response = await result.current.safeApiCall(apiCall)

      expect(response).toEqual({ data: 'success' })
    })

    it('catches and logs API call errors', async () => {
      const { result } = renderHook(() => useAsyncError())
      const failingApiCall = async () => {
        throw new Error('API call failed')
      }

      const response = await result.current.safeApiCall(failingApiCall, {
        context: { endpoint: '/api/users' },
      })

      expect(response).toBeNull()
      expect(errorLogger.logAsyncError).toHaveBeenCalledWith(
        expect.any(Error),
        expect.objectContaining({
          source: 'api_call',
          endpoint: '/api/users',
          timestamp: expect.any(String),
        }),
        'high'
      )
    })
  })

  describe('handleWebSocketError', () => {
    it('logs WebSocket errors with URL and context', () => {
      const { result } = renderHook(() => useAsyncError())
      const wsEvent = new Event('error')
      const wsUrl = 'wss://example.com/ws'

      act(() => {
        result.current.handleWebSocketError(wsEvent, wsUrl, {
          context: { reconnectAttempt: 3 },
        })
      })

      expect(errorLogger.logAsyncError).toHaveBeenCalledWith(
        expect.any(Error),
        expect.objectContaining({
          source: 'websocket',
          wsUrl,
          reconnectAttempt: 3,
          timestamp: expect.any(String),
        }),
        'high'
      )
    })

    it('handles WebSocket errors without URL', () => {
      const { result } = renderHook(() => useAsyncError())
      const wsEvent = new Event('error')

      act(() => {
        result.current.handleWebSocketError(wsEvent)
      })

      expect(errorLogger.logAsyncError).toHaveBeenCalledWith(
        expect.any(Error),
        expect.objectContaining({
          source: 'websocket',
          wsUrl: undefined,
        }),
        'high'
      )

      const loggedError = (errorLogger.logAsyncError as ReturnType<typeof vi.fn>).mock.calls[0][0]
      expect(loggedError.message).toContain('unknown')
    })
  })
})