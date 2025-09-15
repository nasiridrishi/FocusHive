import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';

interface HiveDetails {
  id: string;
  name: string;
  description: string;
  settings: {
    privacyLevel: 'PUBLIC' | 'PRIVATE' | 'INVITE_ONLY';
    category: string;
    focusMode: string;
    maxParticipants: number;
  };
  creatorId: string;
  members: Array<{
    userId: string;
    joinedAt: string;
    role: string;
  }>;
  currentMembers: number;
  maxMembers: number;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
}

export const useHiveDetails = () => {
  const { id } = useParams<{ id: string }>();
  const [hive, setHive] = useState<HiveDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    // Mock implementation - replace with actual API call
    const fetchHiveDetails = async () => {
      try {
        setLoading(true);
        // Simulate API delay
        await new Promise(resolve => setTimeout(resolve, 500));

        // Mock data
        setHive({
          id: id || '1',
          name: 'Focus Hive',
          description: 'A productive workspace for focused work',
          settings: {
            privacyLevel: 'PUBLIC',
            category: 'WORK',
            focusMode: 'POMODORO',
            maxParticipants: 10,
          },
          creatorId: 'user-1',
          members: [],
          currentMembers: 5,
          maxMembers: 10,
          status: 'ACTIVE',
          createdAt: new Date().toISOString(),
        });
      } catch (err) {
        setError(err as Error);
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      fetchHiveDetails();
    }
  }, [id]);

  return { hive, loading, error };
};