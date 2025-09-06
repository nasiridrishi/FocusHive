/**
 * Music Context Hooks
 * 
 * Custom hooks for accessing music context
 */

import { useContext } from 'react';
import MusicContext from '../context/MusicContext';
import type { MusicContextType } from '../types';

/**
 * Hook to use music context
 */
export const useMusic = (): MusicContextType => {
  const context = useContext(MusicContext);
  if (context === undefined) {
    throw new Error('useMusic must be used within a MusicProvider');
  }
  return context;
};