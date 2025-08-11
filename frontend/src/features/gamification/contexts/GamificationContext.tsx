import React, { createContext, useContext, useReducer, useCallback, useEffect } from 'react';
import type { 
  GamificationContextValue, 
  GamificationStats, 
  StreakType,
} from '../types/gamification';

// Mock API - In real app, this would be imported from an API service
const mockApi = {
  getGamificationStats: async (): Promise<GamificationStats> => {
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    return {
      points: {
        current: 1250,
        total: 15750,
        todayEarned: 150,
        weekEarned: 420,
      },
      achievements: [
        {
          id: 'first-focus',
          title: 'First Focus',
          description: 'Complete your first focus session',
          icon: 'focus',
          category: 'focus',
          points: 100,
          unlockedAt: new Date('2024-01-15T10:30:00Z'),
          isUnlocked: true,
          rarity: 'common',
        },
        {
          id: 'focus-master',
          title: 'Focus Master',
          description: 'Complete 100 focus sessions',
          icon: 'master',
          category: 'focus',
          points: 1000,
          progress: 45,
          maxProgress: 100,
          isUnlocked: false,
          rarity: 'epic',
        },
      ],
      streaks: [
        {
          id: 'daily-login-1',
          type: 'daily_login',
          current: 7,
          best: 15,
          lastActivity: new Date('2024-01-15T10:30:00Z'),
          isActive: true,
        },
      ],
      level: 12,
      rank: 256,
      totalUsers: 1500,
    };
  },
  
  addPoints: async (amount: number, source: string): Promise<GamificationStats> => {
    await new Promise(resolve => setTimeout(resolve, 500));
    const currentStats = await mockApi.getGamificationStats();
    return {
      ...currentStats,
      points: {
        ...currentStats.points,
        current: currentStats.points.current + amount,
        total: currentStats.points.total + amount,
        todayEarned: currentStats.points.todayEarned + amount,
      },
    };
  },
  
  unlockAchievement: async (achievementId: string): Promise<GamificationStats> => {
    await new Promise(resolve => setTimeout(resolve, 500));
    const currentStats = await mockApi.getGamificationStats();
    return {
      ...currentStats,
      achievements: currentStats.achievements.map(achievement =>
        achievement.id === achievementId
          ? { ...achievement, isUnlocked: true, unlockedAt: new Date() }
          : achievement
      ),
    };
  },
  
  updateStreak: async (streakType: StreakType): Promise<GamificationStats> => {
    await new Promise(resolve => setTimeout(resolve, 500));
    const currentStats = await mockApi.getGamificationStats();
    return {
      ...currentStats,
      streaks: currentStats.streaks.map(streak =>
        streak.type === streakType
          ? { 
              ...streak, 
              current: streak.current + 1,
              best: Math.max(streak.best, streak.current + 1),
              lastActivity: new Date(),
              isActive: true,
            }
          : streak
      ),
    };
  },
};

// Action types
type GamificationAction =
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_STATS'; payload: GamificationStats }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'UPDATE_POINTS'; payload: { amount: number; source: string } }
  | { type: 'UNLOCK_ACHIEVEMENT'; payload: string }
  | { type: 'UPDATE_STREAK'; payload: StreakType };

// State type
interface GamificationState {
  stats: GamificationStats | null;
  loading: boolean;
  error: string | null;
}

// Initial state
const initialState: GamificationState = {
  stats: null,
  loading: false,
  error: null,
};

// Reducer
const gamificationReducer = (
  state: GamificationState,
  action: GamificationAction
): GamificationState => {
  switch (action.type) {
    case 'SET_LOADING':
      return { ...state, loading: action.payload };
    
    case 'SET_STATS':
      return { ...state, stats: action.payload, loading: false, error: null };
    
    case 'SET_ERROR':
      return { ...state, error: action.payload, loading: false };
    
    default:
      return state;
  }
};

// Default context value
const defaultContextValue: GamificationContextValue = {
  stats: null,
  loading: false,
  error: null,
  addPoints: async () => {},
  unlockAchievement: async () => {},
  updateStreak: async () => {},
  refreshStats: async () => {},
};

// Create context
const GamificationContext = createContext<GamificationContextValue>(defaultContextValue);


// Provider component
export const GamificationProvider: React.FC<{ children: React.ReactNode }> = ({ 
  children 
}) => {
  const [state, dispatch] = useReducer(gamificationReducer, initialState);

  // Load stats on mount
  const loadStats = useCallback(async () => {
    dispatch({ type: 'SET_LOADING', payload: true });
    try {
      const stats = await mockApi.getGamificationStats();
      dispatch({ type: 'SET_STATS', payload: stats });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to load stats';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      
      // Auto-retry after 3 seconds
      setTimeout(() => {
        loadStats();
      }, 3000);
    }
  }, []);

  // Debounced API calls to prevent excessive requests
  // const debouncedLoadStats = debounce(loadStats, 1000);

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  // Add points
  const addPoints = useCallback(async (amount: number, source: string) => {
    if (amount < 0 && state.stats && state.stats.points.current + amount < 0) {
      throw new Error('Cannot have negative points');
    }

    try {
      const updatedStats = await mockApi.addPoints(amount, source);
      dispatch({ type: 'SET_STATS', payload: updatedStats });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to add points';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      throw error;
    }
  }, [state.stats]);

  // Unlock achievement
  const unlockAchievement = useCallback(async (achievementId: string) => {
    // Check if already unlocked
    const achievement = state.stats?.achievements.find(a => a.id === achievementId);
    if (achievement?.isUnlocked) {
      throw new Error('Achievement already unlocked');
    }

    try {
      const updatedStats = await mockApi.unlockAchievement(achievementId);
      dispatch({ type: 'SET_STATS', payload: updatedStats });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to unlock achievement';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      throw error;
    }
  }, [state.stats]);

  // Update streak
  const updateStreak = useCallback(async (streakType: StreakType) => {
    try {
      const updatedStats = await mockApi.updateStreak(streakType);
      dispatch({ type: 'SET_STATS', payload: updatedStats });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Streak update failed';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      throw error;
    }
  }, []);

  // Refresh stats
  const refreshStats = useCallback(async () => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true });
      const stats = await mockApi.getGamificationStats();
      dispatch({ type: 'SET_STATS', payload: stats });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Refresh failed';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      throw error;
    }
  }, []);

  // WebSocket connection for real-time updates (mock implementation)
  useEffect(() => {
    // Mock WebSocket connection
    const mockWebSocket = {
      addEventListener: (event: string, handler: (e: MessageEvent) => void) => {
        if (event === 'message') {
          // Simulate periodic updates
          const interval = setInterval(() => {
            const mockEvent = new MessageEvent('message', {
              data: JSON.stringify({
                type: 'STATS_UPDATE',
                payload: state.stats ? {
                  ...state.stats,
                  points: {
                    ...state.stats.points,
                    current: state.stats.points.current + Math.floor(Math.random() * 10),
                  },
                } : null,
              }),
            });
            handler(mockEvent);
          }, 30000); // Update every 30 seconds

          return () => clearInterval(interval);
        }
      },
      close: () => {},
    };

    const cleanup = mockWebSocket.addEventListener('message', (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.type === 'STATS_UPDATE' && data.payload) {
          dispatch({ type: 'SET_STATS', payload: data.payload });
        }
      } catch (error) {
        // Invalid WebSocket message
      }
    });

    return cleanup;
  }, [state.stats]);

  const contextValue: GamificationContextValue = {
    stats: state.stats,
    loading: state.loading,
    error: state.error,
    addPoints,
    unlockAchievement,
    updateStreak,
    refreshStats,
  };

  return (
    <GamificationContext.Provider value={contextValue}>
      {children}
    </GamificationContext.Provider>
  );
};

// Hook to use the context
// eslint-disable-next-line react-refresh/only-export-components
export const useGamification = (): GamificationContextValue => {
  const context = useContext(GamificationContext);
  
  if (!context) {
    // Return default values when no provider is present
    return defaultContextValue;
  }
  
  return context;
};