/**
 * Higher-order component for conditional loading
 */

import React from 'react';
import { LoadingSpinner, LoadingSpinnerProps } from './LoadingSpinner';

export interface WithLoadingProps {
  isLoading: boolean;
  loadingProps?: Partial<LoadingSpinnerProps>;
  fallback?: React.ReactNode;
}

export const withLoading = <P extends object>(
    Component: React.ComponentType<P>
): React.FC<P & WithLoadingProps> => {
  return ({isLoading, loadingProps, fallback, ...props}) => {
    if (isLoading) {
      return fallback || <LoadingSpinner variant="centered" {...loadingProps} />;
    }
    return <Component {...(props as P)} />;
  };
};

export default withLoading;