import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { useLoadingState, useSimpleLoadingState } from '../../../hooks/useLoadingState';

describe('useLoadingState', () => {
  it('initializes with no loading operations', () => {
    const { result } = renderHook(() => useLoadingState());
    
    expect(result.current.isLoading).toBe(false);
    expect(result.current.errors).toEqual({});
    expect(result.current.success).toEqual({});
  });

  it('sets and clears loading state for operations', () => {
    const { result } = renderHook(() => useLoadingState());

    act(() => {
      result.current.setLoading('login', true);
    });
    
    expect(result.current.isLoading).toBe(true);
    expect(result.current.isOperationLoading('login')).toBe(true);

    act(() => {
      result.current.setLoading('login', false);
    });
    
    expect(result.current.isLoading).toBe(false);
    expect(result.current.isOperationLoading('login')).toBe(false);
  });

  it('handles multiple concurrent operations', () => {
    const { result } = renderHook(() => useLoadingState());

    act(() => {
      result.current.setLoading('login', true);
      result.current.setLoading('register', true);
    });
    
    expect(result.current.isLoading).toBe(true);
    expect(result.current.isOperationLoading('login')).toBe(true);
    expect(result.current.isOperationLoading('register')).toBe(true);

    act(() => {
      result.current.setLoading('login', false);
    });
    
    expect(result.current.isLoading).toBe(true); // still loading register
    expect(result.current.isOperationLoading('login')).toBe(false);
    expect(result.current.isOperationLoading('register')).toBe(true);

    act(() => {
      result.current.setLoading('register', false);
    });
    
    expect(result.current.isLoading).toBe(false);
  });

  it('handles error states', () => {
    const { result } = renderHook(() => useLoadingState());
    const error = new Error('Test error');

    act(() => {
      result.current.setError('login', error);
    });
    
    expect(result.current.hasError('login')).toBe(true);
    expect(result.current.getError('login')).toBe(error);

    act(() => {
      result.current.setError('login', null);
    });
    
    expect(result.current.hasError('login')).toBe(false);
    expect(result.current.getError('login')).toBe(null);
  });

  it('handles success states with auto-reset', () => {
    vi.useFakeTimers();
    const { result } = renderHook(() => useLoadingState());

    act(() => {
      result.current.setSuccess('login', true);
    });
    
    expect(result.current.hasSuccess('login')).toBe(true);

    // Fast-forward time to trigger auto-reset
    act(() => {
      vi.advanceTimersByTime(3000);
    });
    
    expect(result.current.hasSuccess('login')).toBe(false);

    vi.useRealTimers();
  });

  it('clears all states', () => {
    const { result } = renderHook(() => useLoadingState());

    act(() => {
      result.current.setLoading('login', true);
      result.current.setError('register', new Error('Test'));
      result.current.setSuccess('logout', true);
    });
    
    expect(result.current.isLoading).toBe(true);
    expect(result.current.hasError('register')).toBe(true);
    expect(result.current.hasSuccess('logout')).toBe(true);

    act(() => {
      result.current.clearAllStates();
    });
    
    expect(result.current.isLoading).toBe(false);
    expect(result.current.errors).toEqual({});
    expect(result.current.success).toEqual({});
  });
});

describe('useSimpleLoadingState', () => {
  it('initializes with default state', () => {
    const { result } = renderHook(() => useSimpleLoadingState());
    
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBe(null);
    expect(result.current.success).toBe(false);
  });

  it('handles loading state transitions', () => {
    const { result } = renderHook(() => useSimpleLoadingState());

    act(() => {
      result.current.setLoading(true);
    });
    
    expect(result.current.isLoading).toBe(true);
    expect(result.current.error).toBe(null);
    expect(result.current.success).toBe(false);

    act(() => {
      result.current.setLoading(false);
    });
    
    expect(result.current.isLoading).toBe(false);
  });

  it('handles error states', () => {
    const { result } = renderHook(() => useSimpleLoadingState());
    const error = new Error('Test error');

    act(() => {
      result.current.setLoading(true);
    });

    act(() => {
      result.current.setError(error);
    });
    
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBe(error);
    expect(result.current.success).toBe(false);
  });

  it('handles success state with auto-reset', () => {
    vi.useFakeTimers();
    const { result } = renderHook(() => useSimpleLoadingState());

    act(() => {
      result.current.setLoading(true);
    });

    act(() => {
      result.current.setSuccess();
    });
    
    expect(result.current.isLoading).toBe(false);
    expect(result.current.success).toBe(true);
    expect(result.current.error).toBe(null);

    // Fast-forward time to trigger auto-reset
    act(() => {
      vi.advanceTimersByTime(3000);
    });
    
    expect(result.current.success).toBe(false);

    vi.useRealTimers();
  });

  it('resets all state', () => {
    const { result } = renderHook(() => useSimpleLoadingState());

    act(() => {
      result.current.setLoading(true);
      result.current.setError(new Error('Test'));
    });

    act(() => {
      result.current.reset();
    });
    
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBe(null);
    expect(result.current.success).toBe(false);
  });
});