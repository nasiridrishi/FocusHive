// Hook for fetching recent hives
export const useRecentHives = () => {
  return {
    hives: [],
    isLoading: false,
    error: null,
  }
}