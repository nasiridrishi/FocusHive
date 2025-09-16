/**
 * Higher-order component for conditional skeleton loading
 */

import React from 'react';
import { SkeletonLoader, SkeletonLoaderProps } from './SkeletonLoader';

export interface WithSkeletonProps {
  isLoading: boolean;
  skeletonProps?: Partial<SkeletonLoaderProps>;
  fallback?: React.ReactNode;
}

export const withSkeleton = <P extends object>(
    Component: React.ComponentType<P>
): React.FC<P & WithSkeletonProps> => {
  return ({isLoading, skeletonProps, fallback, ...props}) => {
    if (isLoading) {
      return fallback || <SkeletonLoader {...skeletonProps} />;
    }
    return <Component {...(props as P)} />;
  };
};

export default withSkeleton;