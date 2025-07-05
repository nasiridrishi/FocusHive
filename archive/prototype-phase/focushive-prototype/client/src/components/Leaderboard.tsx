import React from 'react';
import { useGamification } from '../contexts/GamificationContext';
import { useAuth } from '../contexts/AuthContext';

export const Leaderboard: React.FC = () => {
  const { leaderboard, leaderboardType, refreshLeaderboard } = useGamification();
  const { user } = useAuth();

  const tabs = [
    { id: 'daily' as const, label: 'Daily' },
    { id: 'weekly' as const, label: 'Weekly' },
    { id: 'monthly' as const, label: 'Monthly' },
    { id: 'allTime' as const, label: 'All Time' }
  ];

  const getRankStyle = (rank: number) => {
    switch (rank) {
      case 1:
        return 'bg-gradient-to-r from-yellow-400 to-yellow-500 text-white';
      case 2:
        return 'bg-gradient-to-r from-gray-300 to-gray-400 text-white';
      case 3:
        return 'bg-gradient-to-r from-orange-400 to-orange-500 text-white';
      default:
        return 'bg-white dark:bg-gray-700';
    }
  };

  const getRankIcon = (rank: number) => {
    switch (rank) {
      case 1:
        return '🥇';
      case 2:
        return '🥈';
      case 3:
        return '🥉';
      default:
        return `#${rank}`;
    }
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg p-6">
      <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-4">Leaderboard</h2>

      {/* Tabs */}
      <div className="flex space-x-1 mb-6 bg-gray-100 dark:bg-gray-700 rounded-lg p-1">
        {tabs.map(tab => (
          <button
            key={tab.id}
            onClick={() => refreshLeaderboard(tab.id)}
            className={`flex-1 py-2 px-4 rounded-md font-medium transition-colors ${
              leaderboardType === tab.id
                ? 'bg-white dark:bg-gray-600 text-indigo-600 dark:text-indigo-400 shadow-sm'
                : 'text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Leaderboard entries */}
      <div className="space-y-2">
        {leaderboard.length === 0 ? (
          <p className="text-center text-gray-500 dark:text-gray-400 py-8">No data available yet</p>
        ) : (
          leaderboard.map((entry) => (
            <div
              key={entry.userId}
              className={`flex items-center space-x-4 p-4 rounded-lg ${getRankStyle(entry.rank)} ${
                entry.userId === user?.id ? 'ring-2 ring-indigo-500 dark:ring-indigo-400' : ''
              }`}
            >
              {/* Rank */}
              <div className="w-12 text-center font-bold text-lg">
                {getRankIcon(entry.rank)}
              </div>

              {/* Avatar */}
              <div className="w-10 h-10 rounded-full bg-gray-300 dark:bg-gray-600 flex items-center justify-center">
                {entry.avatar ? (
                  <img src={entry.avatar} alt={entry.username} className="w-full h-full rounded-full" />
                ) : (
                  <span className="text-gray-600 dark:text-gray-300 font-semibold">
                    {entry.username.charAt(0).toUpperCase()}
                  </span>
                )}
              </div>

              {/* User info */}
              <div className="flex-1">
                <p className={`font-semibold ${entry.rank <= 3 ? '' : 'text-gray-800 dark:text-gray-200'}`}>
                  {entry.username}
                  {entry.userId === user?.id && ' (You)'}
                </p>
                <div className={`flex items-center space-x-4 text-sm ${entry.rank <= 3 ? '' : 'text-gray-600 dark:text-gray-400'}`}>
                  <span>⭐ {entry.points} pts</span>
                  <span>⏱️ {entry.focusTime} min</span>
                  {entry.streak > 0 && <span>🔥 {entry.streak} days</span>}
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};