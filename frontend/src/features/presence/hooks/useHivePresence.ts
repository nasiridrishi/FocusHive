import { useState, useEffect } from 'react';

interface UserPresence {
  userId: string;
  username: string;
  status: 'ONLINE' | 'AWAY' | 'BUSY' | 'IN_FOCUS' | 'OFFLINE';
  avatar?: string;
  focusTimeRemaining?: number;
  lastActivity?: string;
}

export const useHivePresence = (hiveId: string) => {
  const [members, setMembers] = useState<UserPresence[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    // Mock implementation - replace with actual WebSocket/API connection
    const fetchPresence = async () => {
      try {
        setLoading(true);
        // Simulate API delay
        await new Promise(resolve => setTimeout(resolve, 300));

        // Mock data
        setMembers([
          {
            userId: '1',
            username: 'Alice',
            status: 'IN_FOCUS',
            avatar: '/avatar1.png',
            focusTimeRemaining: 1500, // seconds
          },
          {
            userId: '2',
            username: 'Bob',
            status: 'ONLINE',
            avatar: '/avatar2.png',
          },
          {
            userId: '3',
            username: 'Charlie',
            status: 'AWAY',
            avatar: '/avatar3.png',
            lastActivity: new Date(Date.now() - 15 * 60000).toISOString(),
          },
        ]);
      } catch (err) {
        setError(err as Error);
      } finally {
        setLoading(false);
      }
    };

    if (hiveId) {
      fetchPresence();
    }
  }, [hiveId]);

  return { members, loading, error };
};