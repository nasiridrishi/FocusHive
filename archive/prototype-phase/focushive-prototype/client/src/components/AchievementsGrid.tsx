import React from 'react';
import { useGamification } from '../contexts/GamificationContext';

const ALL_ACHIEVEMENTS = [
  {
    id: 'first_focus',
    name: 'First Focus',
    description: 'Complete your first 25-minute focus session',
    icon: '🎯',
    points: 10,
    category: 'focus'
  },
  {
    id: 'hour_power',
    name: 'Hour Power',
    description: 'Complete 60 minutes of focus in a single day',
    icon: '⚡',
    points: 20,
    category: 'focus'
  },
  {
    id: 'deep_diver',
    name: 'Deep Diver',
    description: 'Complete a 4-hour deep work session',
    icon: '🌊',
    points: 50,
    category: 'focus'
  },
  {
    id: 'centurion',
    name: 'Centurion',
    description: 'Reach 100 total hours of focus time',
    icon: '💯',
    points: 100,
    category: 'focus'
  },
  {
    id: 'consistent',
    name: 'Consistent',
    description: 'Maintain a 7-day focus streak',
    icon: '🔥',
    points: 25,
    category: 'streak'
  },
  {
    id: 'dedicated',
    name: 'Dedicated',
    description: 'Maintain a 30-day focus streak',
    icon: '💪',
    points: 75,
    category: 'streak'
  },
  {
    id: 'unstoppable',
    name: 'Unstoppable',
    description: 'Maintain a 100-day focus streak',
    icon: '🚀',
    points: 200,
    category: 'streak'
  },
  {
    id: 'team_player',
    name: 'Team Player',
    description: 'Join 5 different focus rooms',
    icon: '👥',
    points: 15,
    category: 'social'
  },
  {
    id: 'popular_space',
    name: 'Popular Space',
    description: 'Have 5+ people in your room at once',
    icon: '🌟',
    points: 30,
    category: 'social'
  },
  {
    id: 'buddy_up',
    name: 'Buddy Up',
    description: 'Complete a session with a focus buddy',
    icon: '🤝',
    points: 20,
    category: 'social'
  }
];

export const AchievementsGrid: React.FC = () => {
  const { achievements } = useGamification();
  const earnedIds = new Set(achievements.map(a => a.id));

  const categories = [
    { id: 'focus', name: 'Focus', colorClass: 'text-indigo-600 dark:text-indigo-400 border-indigo-400 dark:border-indigo-500 bg-indigo-50 dark:bg-indigo-900/20' },
    { id: 'streak', name: 'Streak', colorClass: 'text-orange-600 dark:text-orange-400 border-orange-400 dark:border-orange-500 bg-orange-50 dark:bg-orange-900/20' },
    { id: 'social', name: 'Social', colorClass: 'text-green-600 dark:text-green-400 border-green-400 dark:border-green-500 bg-green-50 dark:bg-green-900/20' }
  ];

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg p-6">
      <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-6">Achievements</h2>

      {categories.map(category => {
        const categoryAchievements = ALL_ACHIEVEMENTS.filter(a => a.category === category.id);
        const earnedCount = categoryAchievements.filter(a => earnedIds.has(a.id)).length;

        return (
          <div key={category.id} className="mb-8">
            <div className="flex items-center justify-between mb-4">
              <h3 className={`text-lg font-semibold ${category.colorClass.split(' ')[0]} ${category.colorClass.split(' ')[1]}`}>
                {category.name} Achievements
              </h3>
              <span className="text-sm text-gray-500 dark:text-gray-400">
                {earnedCount} / {categoryAchievements.length}
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {categoryAchievements.map(achievement => {
                const isEarned = earnedIds.has(achievement.id);

                return (
                  <div
                    key={achievement.id}
                    className={`p-4 rounded-lg border-2 transition-all ${
                      isEarned
                        ? category.colorClass
                        : 'border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900/50 opacity-60'
                    }`}
                  >
                    <div className="flex items-start space-x-3">
                      <div className={`text-3xl ${isEarned ? '' : 'grayscale'}`}>
                        {achievement.icon}
                      </div>
                      <div className="flex-1">
                        <h4 className={`font-semibold ${isEarned ? 'text-gray-800 dark:text-gray-200' : 'text-gray-500 dark:text-gray-400'}`}>
                          {achievement.name}
                        </h4>
                        <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                          {achievement.description}
                        </p>
                        <p className={`text-sm font-medium mt-2 ${
                          isEarned ? category.colorClass.split(' ')[0] + ' ' + category.colorClass.split(' ')[1] : 'text-gray-400 dark:text-gray-500'
                        }`}>
                          {isEarned ? '✓' : ''} {achievement.points} points
                        </p>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        );
      })}

      <div className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
        Total: {achievements.length} / {ALL_ACHIEVEMENTS.length} achievements earned
      </div>
    </div>
  );
};