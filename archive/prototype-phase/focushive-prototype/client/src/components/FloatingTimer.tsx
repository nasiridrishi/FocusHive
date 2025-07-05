import React, { useState, useRef, useEffect } from 'react';
import { useSocket } from '../contexts/SocketContext';
import type { TimerState } from '@focushive/shared';

type Position = 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right' | 'left' | 'right';

interface FloatingTimerProps {
  roomId: string;
}

export const FloatingTimer: React.FC<FloatingTimerProps> = ({ roomId }) => {
  const { socket } = useSocket();
  const [position, setPosition] = useState<Position>('bottom-right');
  const [isDragging, setIsDragging] = useState(false);
  const [isMinimized, setIsMinimized] = useState(false);
  const [offset, setOffset] = useState({ x: 20, y: 20 });
  const [timerState, setTimerState] = useState<TimerState | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const dragStartPos = useRef({ x: 0, y: 0 });

  const positionClasses: Record<Position, string> = {
    'top-left': 'top-0 left-0',
    'top-right': 'top-0 right-0',
    'bottom-left': 'bottom-0 left-0',
    'bottom-right': 'bottom-0 right-0',
    'left': 'top-1/2 left-0 -translate-y-1/2',
    'right': 'top-1/2 right-0 -translate-y-1/2'
  };

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

  const handleMouseDown = (e: React.MouseEvent) => {
    setIsDragging(true);
    dragStartPos.current = { x: e.clientX, y: e.clientY };
  };

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!isDragging || !containerRef.current) return;

      const deltaX = e.clientX - dragStartPos.current.x;
      const deltaY = e.clientY - dragStartPos.current.y;
      
      // Determine which corner/side is closest
      const rect = containerRef.current.getBoundingClientRect();
      const windowWidth = window.innerWidth;
      const windowHeight = window.innerHeight;
      
      const distances = {
        'top-left': Math.sqrt(rect.left ** 2 + rect.top ** 2),
        'top-right': Math.sqrt((windowWidth - rect.right) ** 2 + rect.top ** 2),
        'bottom-left': Math.sqrt(rect.left ** 2 + (windowHeight - rect.bottom) ** 2),
        'bottom-right': Math.sqrt((windowWidth - rect.right) ** 2 + (windowHeight - rect.bottom) ** 2),
        'left': rect.left,
        'right': windowWidth - rect.right
      };

      const closestPosition = Object.entries(distances).reduce((a, b) => 
        a[1] < b[1] ? a : b
      )[0] as Position;

      setPosition(closestPosition);
      dragStartPos.current = { x: e.clientX, y: e.clientY };
    };

    const handleMouseUp = () => {
      setIsDragging(false);
    };

    if (isDragging) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };
  }, [isDragging]);

  return (
    <div
      ref={containerRef}
      className={`fixed z-50 ${positionClasses[position]} transition-all duration-300 ${
        isDragging ? 'cursor-grabbing' : ''
      }`}
      style={{
        margin: `${offset.y}px ${offset.x}px`,
        width: isMinimized ? 'auto' : '400px'
      }}
    >
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-2xl border border-gray-200 dark:border-gray-700">
        {/* Timer Header */}
        <div
          className="flex items-center justify-between p-2 bg-gray-50 dark:bg-gray-700 rounded-t-lg cursor-grab"
          onMouseDown={handleMouseDown}
        >
          <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Pomodoro Timer</h3>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setIsMinimized(!isMinimized)}
              className="p-1 hover:bg-gray-200 dark:hover:bg-gray-600 rounded transition-colors"
            >
              <svg className="w-4 h-4 text-gray-600 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                {isMinimized ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8V4m0 0h4M4 4l5 5m11-5h-4m4 0v4m0-4l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 12H4" />
                )}
              </svg>
            </button>
          </div>
        </div>

        {/* Timer Content */}
        {!isMinimized && (
          <div className="transform scale-90 origin-top">
            {timerState ? (
              <div className="p-6 text-center">
                <div className="text-5xl font-bold text-gray-900 dark:text-white mb-2">
                  {formatTime(timerState.remaining)}
                </div>
                <div className="text-sm text-gray-600 dark:text-gray-400 mb-4">
                  {getPhaseLabel(timerState.phase)}
                </div>
                <div className="flex justify-center gap-2">
                  {timerState.status === 'idle' && (
                    <button
                      onClick={() => socket?.emit('timer:start', { roomId })}
                      className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition-colors"
                    >
                      Start
                    </button>
                  )}
                  {timerState.status === 'running' && (
                    <button
                      onClick={() => socket?.emit('timer:pause', { roomId })}
                      className="px-4 py-2 bg-yellow-600 text-white rounded-md hover:bg-yellow-700 transition-colors"
                    >
                      Pause
                    </button>
                  )}
                  {timerState.status === 'paused' && (
                    <button
                      onClick={() => socket?.emit('timer:resume', { roomId })}
                      className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
                    >
                      Resume
                    </button>
                  )}
                  <button
                    onClick={() => socket?.emit('timer:reset', { roomId })}
                    className="px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700 transition-colors"
                  >
                    Reset
                  </button>
                </div>
              </div>
            ) : (
              <div className="p-6 text-center">
                <div className="text-gray-500">Loading timer...</div>
              </div>
            )}
          </div>
        )}

        {/* Minimized View */}
        {isMinimized && timerState && (
          <div className="p-3 text-center">
            <div className="text-2xl font-bold text-gray-900 dark:text-white">
              {formatTime(timerState.remaining)}
            </div>
            <div className="text-xs text-gray-600 dark:text-gray-400 mt-1">
              {getPhaseLabel(timerState.phase)}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};