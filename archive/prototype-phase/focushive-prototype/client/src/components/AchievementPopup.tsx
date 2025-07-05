import React, { useEffect } from 'react';
import { useGamification } from '../contexts/GamificationContext';

export const AchievementPopup: React.FC = () => {
  const { newAchievement, clearNewAchievement } = useGamification();

  useEffect(() => {
    if (newAchievement) {
      const timer = setTimeout(() => {
        clearNewAchievement();
      }, 5000); // Auto-hide after 5 seconds

      return () => clearTimeout(timer);
    }
  }, [newAchievement, clearNewAchievement]);

  if (!newAchievement) return null;

  return (
    <div className="fixed top-4 right-4 z-50 animate-slide-in">
      <div className="bg-white rounded-lg shadow-2xl p-6 max-w-sm border-2 border-yellow-400">
        <div className="flex items-center space-x-4">
          <div className="text-5xl animate-bounce">{newAchievement.icon}</div>
          <div className="flex-1">
            <h3 className="text-xl font-bold text-gray-800">Achievement Unlocked!</h3>
            <p className="text-lg font-semibold text-indigo-600 mt-1">{newAchievement.name}</p>
            <p className="text-sm text-gray-600 mt-1">{newAchievement.description}</p>
            <p className="text-sm font-medium text-green-600 mt-2">+{newAchievement.points} points</p>
          </div>
          <button
            onClick={clearNewAchievement}
            className="text-gray-400 hover:text-gray-600"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  );
};