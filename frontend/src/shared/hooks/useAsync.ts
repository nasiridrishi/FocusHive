import { useState, useCallback, useRef, useEffect } from 'react'
import { useAsyncError } from './useAsyncError'

/**
 * State for async operations
 */
export interface AsyncState<T> {
  data: T | null
  error: string | null
  isLoading: boolean
  isIdle: boolean
  isSuccess: boolean
  isError: boolean
}

/**
 * Options for useAsync hook
 */
export interface UseAsyncOptions<T> {
  /** Initial data */
  initialData?: T | null
  /** Execute immediately on mount */
  immediate?: boolean
  /** Reset data on new execution */
  resetOnExecute?: boolean
  /** Error handling options */
  errorOptions?: {
    /** Show error in UI or log only */
    logOnly?: boolean
    /** Error severity */
    severity?: 'low' | 'medium' | 'high' | 'critical'
    /** Additional context for error logging */
    context?: Record<string, unknown>
  }
  /** Success callback */
  onSuccess?: (data: T) => void
  /** Error callback */
  onError?: (error: string) => void
  /** Loading state change callback */
  onLoadingChange?: (isLoading: boolean) => void
}

/**
 * Return type for useAsync hook
 */
export interface UseAsyncReturn<T> extends AsyncState<T> {
  /** Execute the async function */
  execute: (...args: any[]) => Promise<T | null>
  /** Reset state to initial */
  reset: () => void
  /** Set data manually */
  setData: (data: T | null) => void
  /** Set error manually */
  setError: (error: string | null) => void
  /** Set loading manually */
  setLoading: (loading: boolean) => void
  /** Cancel pending request */
  cancel: () => void
}

/**
 * Hook for managing async operations with loading, error, and data states
 * 
 * Features:
 * - Automatic loading state management
 * - Error handling with useAsyncError integration
 * - Request cancellation
 * - Success/error callbacks
 * - Manual state setters
 * - TypeScript support
 * 
 * @example
 * ```tsx
 * const { data, isLoading, error, execute } = useAsync(
 *   async (userId: string) => {
 *     const response = await api.getUser(userId)
 *     return response.data
 *   },
 *   {
 *     onSuccess: (user) => console.log('User loaded:', user),
 *     onError: (error) => console.error('Failed to load user:', error)
 *   }
 * )
 * 
 * // Execute the function
 * useEffect(() => {
 *   execute('user123')
 * }, [])
 * ```
 */
export function useAsync<T, Args extends unknown[] = unknown[]>(
  asyncFunction: (...args: Args) => Promise<T>,
  options: UseAsyncOptions<T> = {}
): UseAsyncReturn<T> {
  const {
    initialData = null,
    immediate = false,
    resetOnExecute = true,
    errorOptions = {},
    onSuccess,
    onError,
    onLoadingChange
  } = options

  const { captureError } = useAsyncError()
  const abortControllerRef = useRef<AbortController | null>(null)
  const immediateExecutedRef = useRef(false)

  // Main state
  const [state, setState] = useState<AsyncState<T>>({
    data: initialData,
    error: null,
    isLoading: false,
    isIdle: true,
    isSuccess: false,
    isError: false
  })

  // Update derived states when main state changes
  const updateState = useCallback((updates: Partial<AsyncState<T>>) => {
    setState(prevState => {
      const newState = { ...prevState, ...updates }
      
      // Update derived states
      newState.isIdle = !newState.isLoading && !newState.error && !newState.data
      newState.isSuccess = !newState.isLoading && !newState.error && !!newState.data
      newState.isError = !newState.isLoading && !!newState.error
      
      return newState
    })
  }, [])

  // Notify loading change
  useEffect(() => {
    onLoadingChange?.(state.isLoading)
  }, [state.isLoading, onLoadingChange])

  // Cancel pending request
  const cancel = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort()
      abortControllerRef.current = null
    }
  }, [])

  // Execute the async function
  const execute = useCallback(
    async (...args: Args): Promise<T | null> => {
      // Cancel any pending request
      cancel()

      // Create new abort controller
      abortControllerRef.current = new AbortController()
      const { signal } = abortControllerRef.current

      try {
        // Reset data if requested
        if (resetOnExecute) {
          updateState({
            data: null,
            error: null,
            isLoading: true
          })
        } else {
          updateState({
            error: null,
            isLoading: true
          })
        }

        // Execute the async function
        const result = await asyncFunction(...args)

        // Check if request was cancelled
        if (signal.aborted) {
          return null
        }

        // Success
        updateState({
          data: result,
          error: null,
          isLoading: false
        })

        onSuccess?.(result)
        return result

      } catch (error) {
        // Check if request was cancelled
        if (signal.aborted) {
          return null
        }

        const errorMessage = error instanceof Error ? error.message : String(error)

        // Update state
        updateState({
          error: errorMessage,
          isLoading: false
        })

        // Handle error with useAsyncError
        captureError(error, {
          severity: 'medium',
          logOnly: true,
          ...errorOptions,
          context: {
            function: asyncFunction.name,
            args: args.length > 0 ? args : undefined,
            ...errorOptions.context
          }
        })

        onError?.(errorMessage)
        return null

      } finally {
        // Clear abort controller if it's the current one
        if (abortControllerRef.current?.signal === signal) {
          abortControllerRef.current = null
        }
      }
    },
    [asyncFunction, resetOnExecute, updateState, cancel, captureError, errorOptions, onSuccess, onError]
  )

  // Manual state setters
  const setData = useCallback((data: T | null) => {
    updateState({ data })
  }, [updateState])

  const setError = useCallback((error: string | null) => {
    updateState({ error })
  }, [updateState])

  const setLoading = useCallback((isLoading: boolean) => {
    updateState({ isLoading })
  }, [updateState])

  // Reset state
  const reset = useCallback(() => {
    cancel()
    setState({
      data: initialData,
      error: null,
      isLoading: false,
      isIdle: true,
      isSuccess: false,
      isError: false
    })
  }, [cancel, initialData])

  // Execute immediately if requested
  useEffect(() => {
    if (immediate && !immediateExecutedRef.current) {
      immediateExecutedRef.current = true
      execute(...([] as unknown as Args))
    }
  }, [immediate, execute])

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      cancel()
    }
  }, [cancel])

  return {
    // State
    ...state,
    // Actions
    execute,
    reset,
    setData,
    setError,
    setLoading,
    cancel
  }
}

/**
 * Simplified version of useAsync for basic API calls
 * 
 * @example
 * ```tsx
 * const { data, loading, error, refetch } = useAsyncData(
 *   () => api.getUsers()
 * )
 * ```
 */
export function useAsyncData<T>(
  asyncFunction: () => Promise<T>,
  immediate = true
) {
  const { data, isLoading, error, execute, reset } = useAsync(asyncFunction, {
    immediate
  })

  return {
    data,
    loading: isLoading,
    error,
    refetch: execute,
    reset
  }
}

/**
 * Hook for form submissions with loading and error states
 * 
 * @example
 * ```tsx
 * const { loading, error, submit } = useAsyncSubmit(
 *   async (formData) => {
 *     await api.createUser(formData)
 *   },
 *   {
 *     onSuccess: () => navigate('/users'),
 *     onError: (error) => toast.error(error)
 *   }
 * )
 * ```
 */
export function useAsyncSubmit<Args extends unknown[] = unknown[]>(
  submitFunction: (...args: Args) => Promise<unknown>,
  options: Omit<UseAsyncOptions<unknown>, 'immediate' | 'initialData'> = {}
) {
  const { isLoading, error, execute, reset } = useAsync(submitFunction, {
    immediate: false,
    resetOnExecute: true,
    ...options
  })

  return {
    loading: isLoading,
    error,
    submit: execute,
    reset
  }
}

export default useAsync