import React from 'react';
import { useGamification } from '../contexts/GamificationContext';

export const PointsDisplay: React.FC = () => {
  const { stats } = useGamification();

  if (!stats) return null;

  return (
    <div className="flex items-center space-x-6 p-4 bg-white dark:bg-gray-800 rounded-lg">
      {/* Points */}
      <div className="flex items-center space-x-2">
        <span className="text-2xl">⭐</span>
        <div>
          <p className="text-sm text-gray-500 dark:text-gray-400">Points</p>
          <p className="text-xl font-bold text-indigo-600 dark:text-indigo-400">{stats.totalPoints}</p>
        </div>
      </div>

      {/* Streak */}
      <div className="flex items-center space-x-2">
        <span className="text-2xl">🔥</span>
        <div>
          <p className="text-sm text-gray-500 dark:text-gray-400">Streak</p>
          <p className="text-xl font-bold text-orange-600 dark:text-orange-400">{stats.currentStreak} days</p>
        </div>
      </div>

      {/* Today's Focus */}
      <div className="flex items-center space-x-2">
        <span className="text-2xl">⏱️</span>
        <div>
          <p className="text-sm text-gray-500 dark:text-gray-400">Today</p>
          <p className="text-xl font-bold text-green-600 dark:text-green-400">{stats.todayFocusTime} min</p>
        </div>
      </div>

      {/* Rank */}
      {stats.rank > 0 && (
        <div className="flex items-center space-x-2">
          <span className="text-2xl">🏆</span>
          <div>
            <p className="text-sm text-gray-500 dark:text-gray-400">Rank</p>
            <p className="text-xl font-bold text-purple-600 dark:text-purple-400">#{stats.rank}</p>
          </div>
        </div>
      )}
    </div>
  );
};