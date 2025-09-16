import {useContext} from 'react';
import {AuthActionsContext, AuthStateContext} from '../contexts/authContexts';
import type {AuthContextType, AuthState} from '@shared/types/auth';

// Custom hooks for consuming context with error checking
export function useAuthState(): AuthState {
  const context = useContext(AuthStateContext);
  if (context === null) {
    throw new Error('useAuthState must be used within an AuthProvider');
  }
  return context;
}

export function useAuthActions(): Omit<AuthContextType, 'authState'> {
  const context = useContext(AuthActionsContext);
  if (context === null) {
    throw new Error('useAuthActions must be used within an AuthProvider');
  }
  return context;
}

// Combined auth hook for convenience
export function useAuth(): AuthContextType {
  const authState = useAuthState();
  const authActions = useAuthActions();
  return {authState, ...authActions};
}