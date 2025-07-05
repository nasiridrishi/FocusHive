import React, { useState, useEffect } from 'react';
import { useSocket } from '../contexts/SocketContext';
import type { TimerState } from '@focushive/shared';

interface IntegratedTimerProps {
  roomId: string;
  variant?: 'horizontal' | 'compact';
}

export const IntegratedTimer: React.FC<IntegratedTimerProps> = ({ roomId, variant = 'horizontal' }) => {
  const { socket } = useSocket();
  const [timerState, setTimerState] = useState<TimerState | null>(null);

  // Listen for timer updates
  useEffect(() => {
    if (!socket) return;

    const handleTimerState = (state: TimerState) => {
      setTimerState(state);
    };

    socket.on('timer:state', handleTimerState);
    socket.on('timer:tick', handleTimerState);

    // Request initial state
    socket.emit('timer:get-state', { roomId });

    return () => {
      socket.off('timer:state', handleTimerState);
      socket.off('timer:tick', handleTimerState);
    };
  }, [socket, roomId]);

  const formatTime = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const getPhaseLabel = (phase: string): string => {
    switch (phase) {
      case 'work': return 'Focus Time';
      case 'shortBreak': return 'Short Break';
      case 'longBreak': return 'Long Break';
      default: return 'Timer';
    }
  };

  const getPhaseColor = (phase: string): string => {
    switch (phase) {
      case 'work': return 'text-green-600 dark:text-green-400';
      case 'shortBreak': return 'text-yellow-600 dark:text-yellow-400';
      case 'longBreak': return 'text-blue-600 dark:text-blue-400';
      default: return 'text-gray-600 dark:text-gray-400';
    }
  };

  const handleStart = () => {
    socket?.emit('timer:start', { roomId });
  };

  const handlePause = () => {
    socket?.emit('timer:pause', { roomId });
  };

  const handleResume = () => {
    socket?.emit('timer:resume', { roomId });
  };

  const handleReset = () => {
    socket?.emit('timer:reset', { roomId });
  };

  if (!timerState) {
    return (
      <div className={`bg-white dark:bg-gray-800 rounded-lg shadow-lg p-4 ${variant === 'horizontal' ? 'flex items-center justify-between' : ''}`}>
        <div className="text-gray-500 dark:text-gray-400">Loading timer...</div>
      </div>
    );
  }

  if (variant === 'compact') {
    return (
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg p-4">
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Pomodoro</h3>
          <span className={`text-xs font-medium ${getPhaseColor(timerState.phase)}`}>
            {getPhaseLabel(timerState.phase)}
          </span>
        </div>
        <div className="text-3xl font-bold text-gray-900 dark:text-white text-center mb-3">
          {formatTime(timerState.remaining)}
        </div>
        <div className="flex gap-1">
          {timerState.status === 'idle' && (
            <button
              onClick={handleStart}
              className="flex-1 px-3 py-1.5 text-sm bg-green-600 text-white rounded hover:bg-green-700 transition-colors"
            >
              Start
            </button>
          )}
          {timerState.status === 'running' && (
            <button
              onClick={handlePause}
              className="flex-1 px-3 py-1.5 text-sm bg-yellow-600 text-white rounded hover:bg-yellow-700 transition-colors"
            >
              Pause
            </button>
          )}
          {timerState.status === 'paused' && (
            <button
              onClick={handleResume}
              className="flex-1 px-3 py-1.5 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors"
            >
              Resume
            </button>
          )}
          <button
            onClick={handleReset}
            className="flex-1 px-3 py-1.5 text-sm bg-gray-600 text-white rounded hover:bg-gray-700 transition-colors"
          >
            Reset
          </button>
        </div>
      </div>
    );
  }

  // Horizontal variant (default)
  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg p-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-6">
          <div>
            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1">Pomodoro Timer</h3>
            <div className="flex items-baseline gap-2">
              <span className="text-4xl font-bold text-gray-900 dark:text-white">
                {formatTime(timerState.remaining)}
              </span>
              <span className={`text-sm font-medium ${getPhaseColor(timerState.phase)}`}>
                {getPhaseLabel(timerState.phase)}
              </span>
            </div>
          </div>
          
          {/* Progress bar */}
          <div className="w-48 h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
            <div 
              className={`h-full transition-all duration-1000 ${
                timerState.phase === 'work' ? 'bg-green-500' : 
                timerState.phase === 'shortBreak' ? 'bg-yellow-500' : 
                'bg-blue-500'
              }`}
              style={{ 
                width: `${timerState.duration ? ((timerState.duration - timerState.remaining) / timerState.duration) * 100 : 0}%` 
              }}
            />
          </div>
        </div>

        <div className="flex items-center gap-2">
          {timerState.status === 'idle' && (
            <button
              onClick={handleStart}
              className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition-colors flex items-center gap-2"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Start
            </button>
          )}
          {timerState.status === 'running' && (
            <button
              onClick={handlePause}
              className="px-4 py-2 bg-yellow-600 text-white rounded-md hover:bg-yellow-700 transition-colors flex items-center gap-2"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Pause
            </button>
          )}
          {timerState.status === 'paused' && (
            <button
              onClick={handleResume}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors flex items-center gap-2"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Resume
            </button>
          )}
          <button
            onClick={handleReset}
            className="px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700 transition-colors flex items-center gap-2"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Reset
          </button>
        </div>
      </div>
    </div>
  );
};