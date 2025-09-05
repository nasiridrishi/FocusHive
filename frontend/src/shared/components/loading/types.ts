import { CircularProgressProps, SkeletonProps, ButtonProps, BackdropProps } from '@mui/material'

/**
 * Loading component type definitions
 */

export interface LoadingSpinnerProps extends Omit<CircularProgressProps, 'children'> {
  /** Optional text to display below spinner */
  text?: string
  /** Wrapper styling options */
  centered?: boolean
  /** Inline vs block display */
  inline?: boolean
}

export interface LoadingBackdropProps extends Omit<BackdropProps, 'children'> {
  /** Loading text to display */
  text?: string
  /** Show progress value */
  progress?: number
  /** Backdrop variant */
  variant?: 'simple' | 'detailed'
}

export interface LoadingSkeletonProps extends SkeletonProps {
  /** Number of skeleton lines */
  lines?: number
  /** Show avatar skeleton */
  avatar?: boolean
  /** Show action buttons skeleton */
  actions?: boolean
}

export interface LoadingButtonProps extends Omit<ButtonProps, 'loading'> {
  /** Loading state */
  loading?: boolean
  /** Text to show during loading */
  loadingText?: string
  /** Position of loading indicator */
  loadingPosition?: 'start' | 'center' | 'end'
}

export interface ContentSkeletonProps {
  /** Type of content to skeleton */
  type: 'card' | 'list' | 'form' | 'table' | 'chat' | 'hive'
  /** Number of items to show */
  count?: number
  /** Show loading animation */
  animation?: 'pulse' | 'wave' | false
}

export interface TableSkeletonProps {
  /** Number of rows to show */
  rows?: number
  /** Number of columns to show */
  columns?: number
  /** Show header skeleton */
  showHeader?: boolean
  /** Show actions column */
  showActions?: boolean
  /** Animation type */
  animation?: 'pulse' | 'wave' | false
}

export interface LazyLoadingFallbackProps {
  /**
   * Type of loading fallback to display
   * - spinner: Simple circular progress indicator
   * - skeleton: Content-shaped loading placeholder
   * - page: Full page loading with branded message
   * - feature: Feature-specific loading state
   */
  variant?: 'spinner' | 'skeleton' | 'page' | 'feature'
  
  /**
   * Custom loading message
   */
  message?: string
  
  /**
   * Minimum height for the loading area
   */
  minHeight?: string | number
  
  /**
   * Show loading message alongside indicator
   */
  showMessage?: boolean
  
  /**
   * Feature name for contextual loading (used with 'feature' variant)
   */
  featureName?: string
}