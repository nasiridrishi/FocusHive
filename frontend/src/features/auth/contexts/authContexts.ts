import { createContext } from 'react';
import type { AuthState, AuthContextType } from '@shared/types/auth';

// Create contexts for state and actions (performance optimization)
export const AuthStateContext = createContext<AuthState | null>(null);
export const AuthActionsContext = createContext<Omit<AuthContextType, 'authState'> | null>(null);