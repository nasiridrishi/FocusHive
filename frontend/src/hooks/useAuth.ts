/**
 * Re-export useAuth hook with compatibility layer for existing tests
 * This provides a unified interface that works with both the new auth architecture
 * and existing test expectations
 */

import { useAuthContext } from '../contexts/AuthContext';

// Use the main AuthContext which has the expected interface
export const useAuth = useAuthContext;

// Also export the features auth hook with a different name if needed
export { useAuth as useFeatureAuth } from '../features/auth/hooks/useAuth';
