import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderHook } from '@testing-library/react';
import { GamificationProvider, useGamification } from './GamificationContext';
import type { GamificationStats } from '../types/gamification';

const mockStats: GamificationStats = {
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

const TestComponent = () => {
  const {
    stats,
    loading,
    error,
    addPoints,
    unlockAchievement,
    updateStreak,
    refreshStats,
  } = useGamification();

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <div data-testid="points">{stats?.points.current}</div>
      <div data-testid="level">{stats?.level}</div>
      <div data-testid="rank">{stats?.rank}</div>
      <button 
        onClick={() => addPoints(100, 'test')}
        data-testid="add-points"
      >
        Add Points
      </button>
      <button 
        onClick={() => unlockAchievement('test-achievement')}
        data-testid="unlock-achievement"
      >
        Unlock Achievement
      </button>
      <button 
        onClick={() => updateStreak('daily_login')}
        data-testid="update-streak"
      >
        Update Streak
      </button>
      <button 
        onClick={refreshStats}
        data-testid="refresh-stats"
      >
        Refresh Stats
      </button>
    </div>
  );
};

const renderWithProvider = (component: React.ReactElement) => {
  return render(
    <GamificationProvider>
      {component}
    </GamificationProvider>
  );
};

describe('GamificationContext', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Provider Initialization', () => {
    it('loads stats on mount', async () => {
      renderWithProvider(<TestComponent />);
      
      expect(screen.getByText('Loading...')).toBeInTheDocument();
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      }, { timeout: 15000 });
    });

    it('provides default context values when no provider is present', () => {
      const { result } = renderHook(() => useGamification());
      
      expect(result.current.stats).toBeNull();
      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBeNull();
    });
  });

  describe('Stats Display', () => {
    it('displays all stats correctly', async () => {
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
        expect(screen.getByTestId('level')).toHaveTextContent('12');
        expect(screen.getByTestId('rank')).toHaveTextContent('256');
      }, { timeout: 15000 });
    });
  });

  describe('Points Management', () => {
    it('adds points successfully', async () => {
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      }, { timeout: 15000 });
      
      const addPointsButton = screen.getByTestId('add-points');
      await act(async () => {
        await userEvent.click(addPointsButton);
      });
      
      // The mock API will add 100 points to current value
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1350');
      }, { timeout: 15000 });
    });
  });

  describe('Achievement Management', () => {
    it('unlocks achievement successfully', async () => {
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      }, { timeout: 15000 });
      
      const unlockButton = screen.getByTestId('unlock-achievement');
      await act(async () => {
        await userEvent.click(unlockButton);
      });
      
      // The action should complete without error
      await waitFor(() => {
        expect(screen.getByTestId('points')).toBeInTheDocument();
      }, { timeout: 15000 });
    });
  });

  describe('Streak Management', () => {
    it('updates streak successfully', async () => {
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      }, { timeout: 15000 });
      
      const updateStreakButton = screen.getByTestId('update-streak');
      await act(async () => {
        await userEvent.click(updateStreakButton);
      });
      
      // The action should complete without error
      await waitFor(() => {
        expect(screen.getByTestId('points')).toBeInTheDocument();
      }, { timeout: 15000 });
    });
  });

  describe('Data Refresh', () => {
    it('refreshes stats successfully', async () => {
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      }, { timeout: 15000 });
      
      const refreshButton = screen.getByTestId('refresh-stats');
      await act(async () => {
        await userEvent.click(refreshButton);
      });
      
      // Should still show stats after refresh
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      }, { timeout: 15000 });
    });
  });
});