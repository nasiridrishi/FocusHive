import { useState, useCallback } from 'react';

interface BuddyMatch {
  id: string;
  username: string;
  avatar?: string;
  timezone: string;
  focusAreas: string[];
  studyHours: string;
  matchScore: number;
  bio?: string;
}

interface Preferences {
  focusAreas: string[];
  timezone: string;
  studyHours: string;
  communicationPreferences: string[];
  availability: string[];
  experienceLevel: string;
  languages: string[];
  commitmentHours: number;
  bio: string;
  learningStyle?: string;
  goals?: string;
  accountabilityStyle?: string;
  meetingFrequency?: string;
}

export const useBuddyMatching = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [suggestedMatches, setSuggestedMatches] = useState<BuddyMatch[]>([]);

  const submitPreferences = useCallback(async (preferences: Preferences) => {
    try {
      setLoading(true);
      setError(null);

      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000));

      // Mock suggested matches
      setSuggestedMatches([
        {
          id: 'buddy-1',
          username: 'Alice',
          avatar: '/avatar1.png',
          timezone: 'America/New_York',
          focusAreas: ['Web Development', 'React'],
          studyHours: 'Morning',
          matchScore: 95,
          bio: 'Passionate about frontend development'
        },
        {
          id: 'buddy-2',
          username: 'Bob',
          avatar: '/avatar2.png',
          timezone: 'Europe/London',
          focusAreas: ['Data Science', 'Python'],
          studyHours: 'Evening',
          matchScore: 78,
          bio: 'Working on ML projects'
        }
      ]);

      return true;
    } catch (err) {
      setError(err as Error);
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  const findMatches = useCallback(async (criteria: Partial<Preferences>) => {
    try {
      setLoading(true);
      setError(null);

      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000));

      // Return matches based on criteria
      return suggestedMatches;
    } catch (err) {
      setError(err as Error);
      return [];
    } finally {
      setLoading(false);
    }
  }, [suggestedMatches]);

  const connectWithBuddy = useCallback(async (buddyId: string) => {
    try {
      // Simulate sending connection request
      await new Promise(resolve => setTimeout(resolve, 500));
      console.log('Connecting with buddy:', buddyId);
      return true;
    } catch (err) {
      console.error('Failed to connect:', err);
      return false;
    }
  }, []);

  return {
    submitPreferences,
    findMatches,
    connectWithBuddy,
    loading,
    error,
    suggestedMatches
  };
};

// Additional hook exports to satisfy index.ts imports
export const useSearchBuddies = () => {
  const { findMatches, loading, error, suggestedMatches } = useBuddyMatching();
  return {
    searchBuddies: findMatches,
    results: suggestedMatches,
    loading,
    error
  };
};

export const usePendingMatches = () => {
  const [pendingMatches] = useState([]);
  return {
    pendingMatches,
    loading: false,
    error: null
  };
};

export const useBuddyMatch = () => {
  const { connectWithBuddy } = useBuddyMatching();
  return {
    sendMatchRequest: connectWithBuddy,
    loading: false,
    error: null
  };
};

export const useAcceptMatch = () => {
  return {
    acceptMatch: async (matchId: string) => {
      console.log('Accepting match:', matchId);
      return true;
    },
    loading: false,
    error: null
  };
};

export const useDeclineMatch = () => {
  return {
    declineMatch: async (matchId: string) => {
      console.log('Declining match:', matchId);
      return true;
    },
    loading: false,
    error: null
  };
};

export const useCancelMatch = () => {
  return {
    cancelMatch: async (matchId: string) => {
      console.log('Canceling match:', matchId);
      return true;
    },
    loading: false,
    error: null
  };
};

// Query keys for react-query
export const buddyMatchingKeys = {
  all: ['buddy', 'matching'] as const,
  search: (criteria: any) => ['buddy', 'matching', 'search', criteria] as const,
  pending: ['buddy', 'matching', 'pending'] as const,
};
