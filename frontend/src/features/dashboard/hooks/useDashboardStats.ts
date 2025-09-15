// Hook for fetching dashboard statistics
export const useDashboardStats = () => {
  // This would normally fetch from API
  return {
    stats: {
      todaysFocus: 0,
      weeklyStreak: 0,
      hivesJoined: 0,
      productivityScore: 0,
    },
    isLoading: false,
    error: null,
  }
}
