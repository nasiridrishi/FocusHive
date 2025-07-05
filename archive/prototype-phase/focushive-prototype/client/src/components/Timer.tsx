import React, { useEffect, useState, useCallback } from 'react';
import { useSocket } from '../contexts/SocketContext';
import { NotificationModal } from './ui/NotificationModal';
import type { TimerState } from '@focushive/shared';

// Define TimerPhase locally to avoid import issues
type TimerPhase = 'work' | 'shortBreak' | 'longBreak';

interface TimerProps {
  roomId: string;
}

export const Timer: React.FC<TimerProps> = ({ roomId }) => {
  const { socket } = useSocket();
  const [timerState, setTimerState] = useState<TimerState | null>(null);
  const [remaining, setRemaining] = useState<number>(0);
  const [showResetModal, setShowResetModal] = useState(false);
  const [showSkipModal, setShowSkipModal] = useState(false);

  // Format seconds to MM:SS
  const formatTime = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  // Get phase display info
  const getPhaseInfo = (phase: TimerPhase) => {
    switch (phase) {
      case 'work':
        return { name: 'Focus Time', color: 'text-indigo-600 dark:text-indigo-400', bgColor: 'bg-indigo-100 dark:bg-indigo-900/30' };
      case 'shortBreak':
        return { name: 'Short Break', color: 'text-green-600 dark:text-green-400', bgColor: 'bg-green-100 dark:bg-green-900/30' };
      case 'longBreak':
        return { name: 'Long Break', color: 'text-blue-600 dark:text-blue-400', bgColor: 'bg-blue-100 dark:bg-blue-900/30' };
    }
  };

  // Timer controls
  const handleStart = useCallback(() => {
    socket?.emit('timer:start', { roomId });
  }, [socket, roomId]);

  const handlePause = useCallback(() => {
    socket?.emit('timer:pause', { roomId });
  }, [socket, roomId]);

  const handleResume = useCallback(() => {
    socket?.emit('timer:resume', { roomId });
  }, [socket, roomId]);

  const handleReset = useCallback(() => {
    setShowResetModal(true);
  }, []);

  const confirmReset = useCallback(() => {
    socket?.emit('timer:reset', { roomId });
    setShowResetModal(false);
  }, [socket, roomId]);

  const handleSkip = useCallback(() => {
    setShowSkipModal(true);
  }, []);

  const confirmSkip = useCallback(() => {
    socket?.emit('timer:skip', { roomId });
    setShowSkipModal(false);
  }, [socket, roomId]);

  // Socket event listeners
  useEffect(() => {
    if (!socket) return;

    // Timer state updates
    const handleTimerState = ({ state }: { state: TimerState }) => {
      setTimerState(state);
      setRemaining(state.remaining);
    };

    const handleTimerStarted = ({ state }: { state: TimerState }) => {
      setTimerState(state);
      setRemaining(state.remaining);
    };

    const handleTimerPaused = ({ state }: { state: TimerState }) => {
      setTimerState(state);
      setRemaining(state.remaining);
    };

    const handleTimerResumed = ({ state }: { state: TimerState }) => {
      setTimerState(state);
      setRemaining(state.remaining);
    };

    const handleTimerReset = ({ state }: { state: TimerState }) => {
      setTimerState(state);
      setRemaining(state.remaining);
    };

    const handlePhaseChanged = ({ state }: { state: TimerState }) => {
      setTimerState(state);
      setRemaining(state.remaining);
    };

    const handleTimerTick = ({ remaining: tickRemaining }: { remaining: number }) => {
      setRemaining(tickRemaining);
    };

    const handlePhaseComplete = ({ completedPhase, nextPhase, state }: any) => {
      // Show notification
      const phaseInfo = getPhaseInfo(completedPhase);
      const nextInfo = getPhaseInfo(nextPhase);
      
      if (Notification.permission === 'granted') {
        new Notification(`${phaseInfo.name} Complete!`, {
          body: `Time for ${nextInfo.name.toLowerCase()}.`,
          icon: '/favicon.ico'
        });
      }
      
      setTimerState(state);
      setRemaining(state.remaining);
    };

    const handleTimerError = ({ message }: { message: string }) => {
      console.error('Timer error:', message);
    };

    // Register listeners
    socket.on('timer:state', handleTimerState);
    socket.on('timer:started', handleTimerStarted);
    socket.on('timer:paused', handleTimerPaused);
    socket.on('timer:resumed', handleTimerResumed);
    socket.on('timer:reset', handleTimerReset);
    socket.on('timer:phase-changed', handlePhaseChanged);
    socket.on('timer:tick', handleTimerTick);
    socket.on('timer:phase-complete', handlePhaseComplete);
    socket.on('timer:error', handleTimerError);

    // Cleanup
    return () => {
      socket.off('timer:state', handleTimerState);
      socket.off('timer:started', handleTimerStarted);
      socket.off('timer:paused', handleTimerPaused);
      socket.off('timer:resumed', handleTimerResumed);
      socket.off('timer:reset', handleTimerReset);
      socket.off('timer:phase-changed', handlePhaseChanged);
      socket.off('timer:tick', handleTimerTick);
      socket.off('timer:phase-complete', handlePhaseComplete);
      socket.off('timer:error', handleTimerError);
    };
  }, [socket]);

  // Request notification permission
  useEffect(() => {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
  }, []);

  const phaseInfo = timerState ? getPhaseInfo(timerState.phase) : null;
  const displayTime = timerState ? remaining : 0;

  return (
    <>
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg dark:shadow-gray-900/50 p-8">
      <div className="text-center">
        {/* Phase indicator */}
        {phaseInfo && (
          <div className="mb-6">
            <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${phaseInfo.bgColor} ${phaseInfo.color}`}>
              {phaseInfo.name}
            </span>
            {timerState?.sessionCount !== undefined && timerState.sessionCount > 0 && (
              <span className="ml-2 text-sm text-gray-500 dark:text-gray-400">
                Session {timerState.sessionCount}
              </span>
            )}
          </div>
        )}

        {/* Timer display */}
        <div className="mb-8">
          <div className="text-6xl font-mono font-bold text-gray-800 dark:text-gray-100">
            {formatTime(displayTime)}
          </div>
          {timerState?.status === 'running' && (
            <div className="mt-2 flex justify-center">
              <div className="flex space-x-1">
                <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" style={{ animationDelay: '0.2s' }}></div>
                <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" style={{ animationDelay: '0.4s' }}></div>
              </div>
            </div>
          )}
        </div>

        {/* Control buttons */}
        <div className="flex justify-center space-x-4">
          {(!timerState || timerState.status === 'idle') && (
            <button
              onClick={handleStart}
              className="px-6 py-3 bg-indigo-600 dark:bg-indigo-500 text-white rounded-lg hover:bg-indigo-700 dark:hover:bg-indigo-600 transition-colors font-medium"
            >
              Start Timer
            </button>
          )}

          {timerState?.status === 'running' && (
            <button
              onClick={handlePause}
              className="px-6 py-3 bg-yellow-600 dark:bg-yellow-500 text-white rounded-lg hover:bg-yellow-700 dark:hover:bg-yellow-600 transition-colors font-medium"
            >
              Pause
            </button>
          )}

          {timerState?.status === 'paused' && (
            <button
              onClick={handleResume}
              className="px-6 py-3 bg-green-600 dark:bg-green-500 text-white rounded-lg hover:bg-green-700 dark:hover:bg-green-600 transition-colors font-medium"
            >
              Resume
            </button>
          )}

          {timerState && timerState.status !== 'idle' && (
            <>
              <button
                onClick={handleReset}
                className="px-6 py-3 bg-gray-600 dark:bg-gray-500 text-white rounded-lg hover:bg-gray-700 dark:hover:bg-gray-600 transition-colors font-medium"
              >
                Reset
              </button>
              <button
                onClick={handleSkip}
                className="px-6 py-3 bg-purple-600 dark:bg-purple-500 text-white rounded-lg hover:bg-purple-700 dark:hover:bg-purple-600 transition-colors font-medium"
              >
                Skip Phase
              </button>
            </>
          )}
        </div>

        {/* Phase progress indicators */}
        {timerState && (
          <div className="mt-8 flex justify-center space-x-2">
            {[...Array(4)].map((_, i) => (
              <div
                key={i}
                className={`w-2 h-2 rounded-full ${
                  i < (timerState.sessionCount || 0)
                    ? 'bg-indigo-600 dark:bg-indigo-500'
                    : 'bg-gray-300 dark:bg-gray-600'
                }`}
              />
            ))}
          </div>
        )}
      </div>
    </div>

    <NotificationModal
      isOpen={showResetModal}
      onClose={() => setShowResetModal(false)}
      type="warning"
      title="Reset Timer"
      message="Are you sure you want to reset the timer? This will stop the current session."
      actions={[
        {
          label: 'Reset',
          onClick: confirmReset,
          variant: 'primary'
        },
        {
          label: 'Cancel',
          onClick: () => setShowResetModal(false),
          variant: 'outline'
        }
      ]}
    />

    <NotificationModal
      isOpen={showSkipModal}
      onClose={() => setShowSkipModal(false)}
      type="info"
      title="Skip Phase"
      message="Skip to the next phase? The current phase will end immediately."
      actions={[
        {
          label: 'Skip',
          onClick: confirmSkip,
          variant: 'primary'
        },
        {
          label: 'Cancel',
          onClick: () => setShowSkipModal(false),
          variant: 'outline'
        }
      ]}
    />
    </>
  );
};