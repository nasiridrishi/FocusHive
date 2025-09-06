import React, { lazy, Suspense, ComponentType } from 'react'
import { SvgIcon, CircularProgress } from '@mui/material'
import { SvgIconProps } from '@mui/material/SvgIcon'
import type { DynamicIconProps } from './dynamicIconUtils'

// Common icons that should be loaded immediately (frequently used)
import HomeIcon from '@mui/icons-material/Home'
import DashboardIcon from '@mui/icons-material/Dashboard'
import PersonIcon from '@mui/icons-material/Person'
import SettingsIcon from '@mui/icons-material/Settings'
import MenuIcon from '@mui/icons-material/Menu'
import CloseIcon from '@mui/icons-material/Close'
import CheckIcon from '@mui/icons-material/Check'
import ErrorIcon from '@mui/icons-material/Error'
import WarningIcon from '@mui/icons-material/Warning'
import InfoIcon from '@mui/icons-material/Info'

// Map of immediately available icons
const immediateIcons = {
  Home: HomeIcon,
  Dashboard: DashboardIcon,
  Person: PersonIcon,
  Settings: SettingsIcon,
  Menu: MenuIcon,
  Close: CloseIcon,
  Check: CheckIcon,
  Error: ErrorIcon,
  Warning: WarningIcon,
  Info: InfoIcon,
} as const

type ImmediateIconName = keyof typeof immediateIcons

// Icon loading cache to prevent duplicate imports
const iconCache = new Map<string, Promise<ComponentType<SvgIconProps>>>()

// Fallback loading component
const IconLoadingFallback = ({ size = 24 }: { size?: number }) => (
  <CircularProgress size={size * 0.8} thickness={5} />
)

// Interface moved to dynamicIconUtils.ts to avoid Fast Refresh warnings

/**
 * Dynamically loads Material-UI icons to reduce bundle size.
 * Commonly used icons are loaded immediately, while others are lazy loaded.
 */
export function DynamicIcon({ 
  name, 
  fallback, 
  showLoading = true,
  ...iconProps 
}: DynamicIconProps) {
  // Check if it's an immediately available icon
  if (name in immediateIcons) {
    const IconComponent = immediateIcons[name as ImmediateIconName]
    return <IconComponent {...iconProps} />
  }

  // Create lazy icon component
  const LazyIcon = lazy(() => {
    // Check cache first
    const iconName = `${name}Icon`
    
    if (!iconCache.has(iconName)) {
      const iconPromise = import('@mui/icons-material')
        .then(iconModule => {
          const IconComponent = (iconModule as unknown)[iconName] as React.ComponentType<SvgIconProps>
          if (IconComponent) {
            return { default: IconComponent }
          }
          throw new Error(`Icon ${iconName} not found`)
        })
        .catch(() => {
          // Return a fallback icon if import fails
          return { default: fallback || ErrorIcon }
        })
      
      iconCache.set(iconName, iconPromise as unknown as Promise<ComponentType<SvgIconProps>>)
    }
    
    return iconCache.get(iconName) as unknown as Promise<{ default: ComponentType<unknown> }>
  })

  return (
    <Suspense fallback={
      showLoading ? (
        <IconLoadingFallback size={iconProps.fontSize === 'small' ? 20 : 24} />
      ) : (
        fallback ? React.createElement(fallback, iconProps) : <SvgIcon {...iconProps} />
      )
    }>
      <LazyIcon {...iconProps} />
    </Suspense>
  )
}

// Function moved to dynamicIconHooks.tsx to avoid Fast Refresh warnings

// Hook moved to dynamicIconHooks.tsx to avoid Fast Refresh warnings

export default DynamicIcon