import { useContext } from 'react';
import { SpotifyContext, type SpotifyContextType } from './SpotifyContext';

export const useSpotify = (): SpotifyContextType => {
  const context = useContext(SpotifyContext);
  if (context === undefined) {
    throw new Error('useSpotify must be used within a SpotifyProvider');
  }
  return context;
};