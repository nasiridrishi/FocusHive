import React from 'react';
import { SvgIconProps } from '@mui/material';
import { DynamicIcon } from './DynamicIcon';

// Icon cache for preloading
const iconCache = new Map<string, Promise<unknown>>();

// Preload commonly used icons after initial page load
export const preloadCommonIcons = () => {
  const commonIconNames = [
    'Add', 'Delete', 'Edit', 'Save', 'Cancel',
    'Favorite', 'Share', 'Search', 'FilterList',
    'MoreVert', 'MoreHoriz', 'ArrowBack', 'ArrowForward',
    'Send', 'Refresh', 'Download', 'Upload'
  ]

  // Preload after a short delay to not block initial render
  setTimeout(() => {
    commonIconNames.forEach(iconName => {
      import('@mui/icons-material').then(iconModule => {
        const IconComponent = (iconModule as Record<string, unknown>)[`${iconName}Icon`]
        if (IconComponent) {
          iconCache.set(`${iconName}Icon`, Promise.resolve(IconComponent))
        }
      })
    })
  }, 2000)
}

// Hook for dynamic icon usage
export function useDynamicIcon(name: string) {
  return React.useMemo(() => ({ 
    Icon: (props: SvgIconProps) => <DynamicIcon name={name} {...props} />
  }), [name])
}