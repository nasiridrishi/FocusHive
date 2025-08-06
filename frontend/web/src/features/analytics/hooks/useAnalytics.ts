import { useContext } from 'react';
import { AnalyticsContext, AnalyticsContextValue } from '../contexts/AnalyticsContext';

export const useAnalytics = (): AnalyticsContextValue => {
  const context = useContext(AnalyticsContext);
  if (context === undefined) {
    throw new Error('useAnalytics must be used within an AnalyticsProvider');
  }
  return context;
};