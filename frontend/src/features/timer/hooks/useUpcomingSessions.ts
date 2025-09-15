// Hook for fetching upcoming sessions
export const useUpcomingSessions = () => {
  return {
    sessions: [],
    isLoading: false,
    error: null,
  }
}