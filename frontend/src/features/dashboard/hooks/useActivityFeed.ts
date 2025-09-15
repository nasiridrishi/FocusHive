// Hook for fetching activity feed
export const useActivityFeed = () => {
  return {
    activities: [],
    isLoading: false,
    error: null,
  }
}
