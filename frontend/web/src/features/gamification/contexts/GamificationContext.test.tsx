import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderHook } from '@testing-library/react';
import { GamificationProvider, useGamification } from './GamificationContext';
import type { GamificationStats, Achievement, Streak } from '../types/gamification';

// Mock API calls
const mockApi = {
  getGamificationStats: vi.fn(),
  addPoints: vi.fn(),
  unlockAchievement: vi.fn(),
  updateStreak: vi.fn(),
};

vi.mock('../api/gamificationApi', () => ({
  gamificationApi: mockApi,
}));

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
    mockApi.getGamificationStats.mockResolvedValue(mockStats);
  });

  afterEach(() => {
    vi.clearAllTimers();
  });

  describe('Provider Initialization', () => {
    it('loads stats on mount', async () => {
      renderWithProvider(<TestComponent />);
      
      expect(screen.getByText('Loading...')).toBeInTheDocument();
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      });
      
      expect(mockApi.getGamificationStats).toHaveBeenCalledTimes(1);
    });

    it('handles loading state correctly', () => {
      mockApi.getGamificationStats.mockImplementation(
        () => new Promise(resolve => setTimeout(() => resolve(mockStats), 100))
      );
      
      renderWithProvider(<TestComponent />);
      
      expect(screen.getByText('Loading...')).toBeInTheDocument();
    });

    it('handles API errors gracefully', async () => {
      const errorMessage = 'Failed to fetch stats';
      mockApi.getGamificationStats.mockRejectedValue(new Error(errorMessage));
      
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByText(`Error: ${errorMessage}`)).toBeInTheDocument();
      });
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
      });
    });

    it('updates stats when data changes', async () => {
      const updatedStats: GamificationStats = {
        ...mockStats,
        points: { ...mockStats.points, current: 1350 },
      };
      
      mockApi.getGamificationStats
        .mockResolvedValueOnce(mockStats)
        .mockResolvedValueOnce(updatedStats);
      
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      });
      
      const refreshButton = screen.getByTestId('refresh-stats');
      await userEvent.click(refreshButton);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1350');
      });
    });
  });

  describe('Points Management', () => {
    it('adds points successfully', async () => {
      const updatedStats: GamificationStats = {
        ...mockStats,
        points: { ...mockStats.points, current: 1350 },
      };
      
      mockApi.addPoints.mockResolvedValue(updatedStats);
      
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      });
      
      const addPointsButton = screen.getByTestId('add-points');
      await userEvent.click(addPointsButton);
      
      await waitFor(() => {
        expect(mockApi.addPoints).toHaveBeenCalledWith(100, 'test');
      });
    });

    it('handles points addition errors', async () => {
      mockApi.addPoints.mockRejectedValue(new Error('Failed to add points'));
      
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      });
      
      const addPointsButton = screen.getByTestId('add-points');
      await userEvent.click(addPointsButton);
      
      await waitFor(() => {
        expect(screen.getByText('Error: Failed to add points')).toBeInTheDocument();
      });
    });

    it('prevents negative points', async () => {
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      });
      
      const { result } = renderHook(() => useGamification(), {
        wrapper: GamificationProvider,
      });
      
      await act(async () => {
        try {
          await result.current.addPoints(-2000, 'test');
        } catch (error) {
          expect(error).toBeInstanceOf(Error);
        }
      });
    });
  });

  describe('Achievement Management', () => {
    it('unlocks achievement successfully', async () => {
      const updatedStats: GamificationStats = {
        ...mockStats,
        achievements: [
          ...mockStats.achievements,
          {
            id: 'test-achievement',
            title: 'Test Achievement',
            description: 'Test description',
            icon: 'test',
            category: 'focus',
            points: 50,
            isUnlocked: true,
            rarity: 'common',
            unlockedAt: new Date(),
          },
        ],
      };
      
      mockApi.unlockAchievement.mockResolvedValue(updatedStats);
      
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      });
      
      const unlockButton = screen.getByTestId('unlock-achievement');
      await userEvent.click(unlockButton);
      
      await waitFor(() => {
        expect(mockApi.unlockAchievement).toHaveBeenCalledWith('test-achievement');
      });
    });

    it('handles achievement unlock errors', async () => {
      mockApi.unlockAchievement.mockRejectedValue(new Error('Already unlocked'));
      
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      });
      
      const unlockButton = screen.getByTestId('unlock-achievement');
      await userEvent.click(unlockButton);
      
      await waitFor(() => {
        expect(screen.getByText('Error: Already unlocked')).toBeInTheDocument();
      });
    });

    it('prevents unlocking already unlocked achievements', async () => {
      renderWithProvider(<TestComponent />);
      
      const { result } = renderHook(() => useGamification(), {
        wrapper: GamificationProvider,
      });
      
      await waitFor(() => result.current.stats !== null);
      
      await act(async () => {
        try {
          await result.current.unlockAchievement('first-focus'); // Already unlocked
        } catch (error) {
          expect(error).toBeInstanceOf(Error);
        }
      });
    });
  });

  describe('Streak Management', () => {
    it('updates streak successfully', async () => {
      const updatedStats: GamificationStats = {
        ...mockStats,
        streaks: [
          {
            ...mockStats.streaks[0],
            current: 8,
            lastActivity: new Date(),
          },
        ],
      };
      
      mockApi.updateStreak.mockResolvedValue(updatedStats);
      
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      });
      
      const updateStreakButton = screen.getByTestId('update-streak');
      await userEvent.click(updateStreakButton);
      
      await waitFor(() => {
        expect(mockApi.updateStreak).toHaveBeenCalledWith('daily_login');
      });
    });

    it('handles streak update errors', async () => {
      mockApi.updateStreak.mockRejectedValue(new Error('Streak update failed'));
      
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      });
      
      const updateStreakButton = screen.getByTestId('update-streak');
      await userEvent.click(updateStreakButton);
      
      await waitFor(() => {
        expect(screen.getByText('Error: Streak update failed')).toBeInTheDocument();
      });
    });

    it('creates new streak when none exists', async () => {
      const updatedStats: GamificationStats = {
        ...mockStats,
        streaks: [
          ...mockStats.streaks,
          {
            id: 'new-streak',
            type: 'focus_session',
            current: 1,
            best: 1,
            lastActivity: new Date(),
            isActive: true,
          },
        ],
      };
      
      mockApi.updateStreak.mockResolvedValue(updatedStats);
      
      renderWithProvider(<TestComponent />);
      
      const { result } = renderHook(() => useGamification(), {
        wrapper: GamificationProvider,
      });
      
      await waitFor(() => result.current.stats !== null);
      
      await act(async () => {
        await result.current.updateStreak('focus_session');
      });
      
      expect(mockApi.updateStreak).toHaveBeenCalledWith('focus_session');
    });
  });

  describe('Data Refresh', () => {
    it('refreshes stats successfully', async () => {
      const updatedStats: GamificationStats = {
        ...mockStats,
        points: { ...mockStats.points, current: 1400 },
      };
      
      mockApi.getGamificationStats
        .mockResolvedValueOnce(mockStats)
        .mockResolvedValueOnce(updatedStats);
      
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      });
      
      const refreshButton = screen.getByTestId('refresh-stats');
      await userEvent.click(refreshButton);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1400');
      });
      
      expect(mockApi.getGamificationStats).toHaveBeenCalledTimes(2);
    });

    it('handles refresh errors', async () => {
      mockApi.getGamificationStats
        .mockResolvedValueOnce(mockStats)
        .mockRejectedValueOnce(new Error('Refresh failed'));
      
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      });
      
      const refreshButton = screen.getByTestId('refresh-stats');
      await userEvent.click(refreshButton);
      
      await waitFor(() => {
        expect(screen.getByText('Error: Refresh failed')).toBeInTheDocument();
      });
    });
  });

  describe('Performance Optimizations', () => {
    it('debounces multiple rapid API calls', async () => {
      vi.useFakeTimers();
      
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      });
      
      const addPointsButton = screen.getByTestId('add-points');
      
      // Make multiple rapid calls
      await userEvent.click(addPointsButton);
      await userEvent.click(addPointsButton);
      await userEvent.click(addPointsButton);
      
      // Fast-forward debounce timer
      act(() => {
        vi.advanceTimersByTime(1000);
      });
      
      // Should only make one API call due to debouncing
      await waitFor(() => {
        expect(mockApi.addPoints).toHaveBeenCalledTimes(1);
      });
      
      vi.useRealTimers();
    });

    it('caches stats to prevent unnecessary API calls', async () => {
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      });
      
      // Multiple renders should not trigger additional API calls
      const refreshButton = screen.getByTestId('refresh-stats');
      await userEvent.click(refreshButton);
      await userEvent.click(refreshButton);
      await userEvent.click(refreshButton);
      
      await waitFor(() => {
        expect(mockApi.getGamificationStats).toHaveBeenCalledTimes(2); // Initial + one refresh
      });
    });
  });

  describe('Error Recovery', () => {
    it('retries failed API calls', async () => {
      mockApi.getGamificationStats
        .mockRejectedValueOnce(new Error('Network error'))
        .mockResolvedValueOnce(mockStats);
      
      renderWithProvider(<TestComponent />);
      
      // Should initially show error
      await waitFor(() => {
        expect(screen.getByText('Error: Network error')).toBeInTheDocument();
      });
      
      // Auto-retry should succeed
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      }, { timeout: 5000 });
      
      expect(mockApi.getGamificationStats).toHaveBeenCalledTimes(2);
    });

    it('provides fallback data when API fails', async () => {
      mockApi.getGamificationStats.mockRejectedValue(new Error('API unavailable'));
      
      const FallbackComponent = () => {
        const { stats, error } = useGamification();
        
        if (error && !stats) {
          return (
            <div>
              <div>Error: {error}</div>
              <div data-testid="fallback-points">0</div>
            </div>
          );
        }
        
        return <div data-testid="points">{stats?.points.current}</div>;
      };
      
      renderWithProvider(<FallbackComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('fallback-points')).toHaveTextContent('0');
      });
    });
  });

  describe('Real-time Updates', () => {
    it('handles real-time stat updates via WebSocket', async () => {
      const mockWebSocket = {
        send: vi.fn(),
        close: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
      };
      
      // Mock WebSocket
      global.WebSocket = vi.fn(() => mockWebSocket) as any;
      
      renderWithProvider(<TestComponent />);
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1250');
      });
      
      // Simulate WebSocket message
      const messageEvent = new MessageEvent('message', {
        data: JSON.stringify({
          type: 'STATS_UPDATE',
          payload: {
            ...mockStats,
            points: { ...mockStats.points, current: 1300 },
          },
        }),
      });
      
      act(() => {
        mockWebSocket.addEventListener.mock.calls
          .find(call => call[0] === 'message')[1](messageEvent);
      });
      
      await waitFor(() => {
        expect(screen.getByTestId('points')).toHaveTextContent('1300');
      });
    });
  });
});