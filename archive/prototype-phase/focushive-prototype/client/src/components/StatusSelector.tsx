import React, { useState } from 'react';
import type { ParticipantStatus } from '@focushive/shared';
import { useSocket } from '../contexts/SocketContext';

export const StatusSelector: React.FC = () => {
  const { updatePresence } = useSocket();
  const [status, setStatus] = useState<ParticipantStatus['status']>('idle');
  const [currentTask, setCurrentTask] = useState('');
  const [showTaskInput, setShowTaskInput] = useState(false);

  const handleStatusChange = (newStatus: ParticipantStatus['status']) => {
    setStatus(newStatus);
    updatePresence(newStatus, currentTask);
    
    // Show task input when focusing
    if (newStatus === 'focusing') {
      setShowTaskInput(true);
    }
  };

  const handleTaskUpdate = () => {
    updatePresence(status, currentTask);
    setShowTaskInput(false);
  };

  const statusOptions: { value: ParticipantStatus['status']; label: string; color: string; icon: string }[] = [
    { 
      value: 'focusing', 
      label: 'Focusing', 
      color: 'text-green-600 dark:text-green-400 bg-green-100 dark:bg-green-900/30 hover:bg-green-200 dark:hover:bg-green-900/50',
      icon: '🎯'
    },
    { 
      value: 'break', 
      label: 'On Break', 
      color: 'text-yellow-600 dark:text-yellow-400 bg-yellow-100 dark:bg-yellow-900/30 hover:bg-yellow-200 dark:hover:bg-yellow-900/50',
      icon: '☕'
    },
    { 
      value: 'away', 
      label: 'Away', 
      color: 'text-orange-600 dark:text-orange-400 bg-orange-100 dark:bg-orange-900/30 hover:bg-orange-200 dark:hover:bg-orange-900/50',
      icon: '🚶'
    },
    { 
      value: 'idle', 
      label: 'Idle', 
      color: 'text-gray-600 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600',
      icon: '💤'
    }
  ];

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-900/50 p-4">
      <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-3">Your Status</h3>
      
      <div className="grid grid-cols-2 gap-2 mb-4">
        {statusOptions.map((option) => (
          <button
            key={option.value}
            onClick={() => handleStatusChange(option.value)}
            className={`p-3 rounded-lg transition-colors ${
              status === option.value
                ? option.color + ' ring-2 ring-offset-2 ring-current'
                : 'text-gray-600 dark:text-gray-400 bg-gray-50 dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600'
            }`}
          >
            <span className="text-2xl mb-1 block">{option.icon}</span>
            <span className="text-sm font-medium">{option.label}</span>
          </button>
        ))}
      </div>

      {(showTaskInput || currentTask) && (
        <div className="mt-4 p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            What are you working on?
          </label>
          <div className="flex gap-2">
            <input
              type="text"
              value={currentTask}
              onChange={(e) => setCurrentTask(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleTaskUpdate()}
              placeholder="e.g., Writing report, Coding feature..."
              className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400 text-sm bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100"
              maxLength={100}
            />
            <button
              onClick={handleTaskUpdate}
              className="px-3 py-2 bg-indigo-600 dark:bg-indigo-500 text-white rounded-md hover:bg-indigo-700 dark:hover:bg-indigo-600 transition-colors text-sm"
            >
              Update
            </button>
          </div>
          {currentTask && (
            <p className="mt-2 text-xs text-gray-600 dark:text-gray-400">
              Currently: {currentTask}
            </p>
          )}
        </div>
      )}
    </div>
  );
};