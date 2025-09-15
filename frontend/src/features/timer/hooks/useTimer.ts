import { useState, useEffect, useCallback, useRef } from 'react';

interface TimerState {
  time: number;
  isRunning: boolean;
  isPaused: boolean;
  mode: 'FOCUS' | 'BREAK' | 'IDLE';
  sessionCount: number;
}

interface TimerConfig {
  focusDuration: number;
  breakDuration: number;
  longBreakDuration: number;
  sessionsBeforeLongBreak: number;
}

export const useTimer = (config?: Partial<TimerConfig>) => {
  const defaultConfig: TimerConfig = {
    focusDuration: 25 * 60, // 25 minutes in seconds
    breakDuration: 5 * 60,   // 5 minutes in seconds
    longBreakDuration: 15 * 60, // 15 minutes in seconds
    sessionsBeforeLongBreak: 4,
  };

  const timerConfig = { ...defaultConfig, ...config };
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  const [state, setState] = useState<TimerState>({
    time: timerConfig.focusDuration,
    isRunning: false,
    isPaused: false,
    mode: 'IDLE',
    sessionCount: 0,
  });

  const start = useCallback(() => {
    setState(prev => ({
      ...prev,
      isRunning: true,
      isPaused: false,
      mode: prev.mode === 'IDLE' ? 'FOCUS' : prev.mode,
    }));
  }, []);

  const pause = useCallback(() => {
    setState(prev => ({
      ...prev,
      isRunning: false,
      isPaused: true,
    }));
  }, []);

  const reset = useCallback(() => {
    setState({
      time: timerConfig.focusDuration,
      isRunning: false,
      isPaused: false,
      mode: 'IDLE',
      sessionCount: 0,
    });
  }, [timerConfig.focusDuration]);

  const skip = useCallback(() => {
    setState(prev => {
      const isLongBreak = (prev.sessionCount + 1) % timerConfig.sessionsBeforeLongBreak === 0;
      const nextMode = prev.mode === 'FOCUS' ? (isLongBreak ? 'BREAK' : 'BREAK') : 'FOCUS';
      const nextTime =
        nextMode === 'FOCUS' ? timerConfig.focusDuration :
        isLongBreak ? timerConfig.longBreakDuration :
        timerConfig.breakDuration;

      return {
        ...prev,
        mode: nextMode,
        time: nextTime,
        sessionCount: nextMode === 'FOCUS' ? prev.sessionCount + 1 : prev.sessionCount,
      };
    });
  }, [timerConfig]);

  useEffect(() => {
    if (state.isRunning && state.time > 0) {
      intervalRef.current = setInterval(() => {
        setState(prev => ({
          ...prev,
          time: prev.time - 1,
        }));
      }, 1000);
    } else if (state.time === 0) {
      // Timer completed
      skip();
    }

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [state.isRunning, state.time, skip]);

  // Mock timer session (usePomodoro expects this)
  const session = state.isRunning ? {
    id: 'current-session',
    status: state.isRunning ? 'running' as const : 'idle' as const,
    sessionType: state.mode === 'FOCUS' ? 'focus' as const : 
                 state.mode === 'BREAK' ? 'short_break' as const : 'focus' as const,
  } : null;

  // Mock start function that accepts CreateTimerRequest
  const startWithRequest = useCallback(async (request: any) => {
    start();
  }, [start]);

  // Mock cancel function
  const cancel = useCallback(async (sessionId: string) => {
    reset();
  }, [reset]);

  return {
    time: state.time,
    isRunning: state.isRunning,
    isPaused: state.isPaused,
    mode: state.mode,
    sessionCount: state.sessionCount,
    session,
    start: startWithRequest,
    pause,
    reset,
    skip,
    cancel,
  };
};