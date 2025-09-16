import {useCallback, useMemo, useState} from 'react';

export interface LoadingState {
  isLoading: boolean;
  errors: Record<string, Error | null>;
  success: Record<string, boolean>;
}

export interface LoadingStateActions {
  setLoading: (operation: string, loading: boolean) => void;
  setError: (operation: string, error: Error | null) => void;
  setSuccess: (operation: string, success: boolean) => void;
  clearState: (operation: string) => void;
  clearAllStates: () => void;
  isOperationLoading: (operation: string) => boolean;
  hasError: (operation: string) => boolean;
  hasSuccess: (operation: string) => boolean;
  getError: (operation: string) => Error | null;
}

export interface UseLoadingStateReturn extends LoadingState, LoadingStateActions {
}

/**
 * Custom hook for managing multiple concurrent loading operations
 *
 * @example
 * ```tsx
 * const {
 *   isLoading,
 *   setLoading,
 *   setError,
 *   setSuccess,
 *   isOperationLoading,
 *   hasError,
 *   clearState
 * } = useLoadingState();
 *
 * // Start loading
 * setLoading('login', true);
 *
 * // Check specific operation
 * if (isOperationLoading('login')) {
 *   // Show loading UI
 * }
 *
 * // Handle success
 * setSuccess('login', true);
 * setLoading('login', false);
 *
 * // Handle error
 * setError('login', new Error('Login failed'));
 * setLoading('login', false);
 * ```
 */
export const useLoadingState = (): UseLoadingStateReturn => {
  const [loadingOperations, setLoadingOperations] = useState<Set<string>>(
      new Set()
  );
  const [errors, setErrors] = useState<Record<string, Error | null>>({});
  const [success, setSuccessState] = useState<Record<string, boolean>>({});

  // Computed loading state
  const isLoading = useMemo(
      () => loadingOperations.size > 0,
      [loadingOperations]
  );

  // Set loading state for an operation
  const setLoading = useCallback((operation: string, loading: boolean) => {
    setLoadingOperations(prev => {
      const newSet = new Set(prev);
      if (loading) {
        newSet.add(operation);
      } else {
        newSet.delete(operation);
      }
      return newSet;
    });

    // Clear error and success when starting loading
    if (loading) {
      setErrors(prev => ({...prev, [operation]: null}));
      setSuccessState(prev => ({...prev, [operation]: false}));
    }
  }, []);

  // Set error state for an operation
  const setError = useCallback((operation: string, error: Error | null) => {
    setErrors(prev => ({...prev, [operation]: error}));
    if (error) {
      setSuccessState(prev => ({...prev, [operation]: false}));
    }
  }, []);

  // Set success state for an operation
  const setSuccess = useCallback((operation: string, success: boolean) => {
    setSuccessState(prev => ({...prev, [operation]: success}));
    if (success) {
      setErrors(prev => ({...prev, [operation]: null}));

      // Auto-reset success after 3 seconds
      setTimeout(() => {
        setSuccessState(current => ({...current, [operation]: false}));
      }, 3000);
    }
  }, []);

  // Clear all states for an operation
  const clearState = useCallback((operation: string) => {
    setLoadingOperations(prev => {
      const newSet = new Set(prev);
      newSet.delete(operation);
      return newSet;
    });
    setErrors(prev => ({...prev, [operation]: null}));
    setSuccessState(prev => ({...prev, [operation]: false}));
  }, []);

  // Clear all operations
  const clearAllStates = useCallback(() => {
    setLoadingOperations(new Set());
    setErrors({});
    setSuccessState({});
  }, []);

  // Check if specific operation is loading
  const isOperationLoading = useCallback(
      (operation: string) => loadingOperations.has(operation),
      [loadingOperations]
  );

  // Check if operation has error
  const hasError = useCallback(
      (operation: string) => Boolean(errors[operation]),
      [errors]
  );

  // Check if operation has success
  const hasSuccess = useCallback(
      (operation: string) => Boolean(success[operation]),
      [success]
  );

  // Get error for operation
  const getError = useCallback(
      (operation: string) => errors[operation] || null,
      [errors]
  );

  return {
    isLoading,
    errors,
    success,
    setLoading,
    setError,
    setSuccess,
    clearState,
    clearAllStates,
    isOperationLoading,
    hasError,
    hasSuccess,
    getError,
  };
};

/**
 * Hook for simple single operation loading state
 * Useful when you only need to track one operation at a time
 */
export const useSimpleLoadingState = (): {
  isLoading: boolean;
  error: Error | null;
  success: boolean;
  setLoading: (loading: boolean) => void;
  setError: (error: Error | null) => void;
  setSuccess: () => void;
  reset: () => void;
} => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [success, setSuccess] = useState(false);

  const setLoading = useCallback((loading: boolean) => {
    setIsLoading(loading);
    if (loading) {
      setError(null);
      setSuccess(false);
    }
  }, []);

  const handleError = useCallback((err: Error | null) => {
    setError(err);
    setIsLoading(false);
    if (err) {
      setSuccess(false);
    }
  }, []);

  const handleSuccess = useCallback(() => {
    setSuccess(true);
    setError(null);
    setIsLoading(false);

    // Auto-reset success after 3 seconds
    setTimeout(() => {
      setSuccess(false);
    }, 3000);
  }, []);

  const reset = useCallback(() => {
    setIsLoading(false);
    setError(null);
    setSuccess(false);
  }, []);

  return {
    isLoading,
    error,
    success,
    setLoading,
    setError: handleError,
    setSuccess: handleSuccess,
    reset,
  };
};

export default useLoadingState;