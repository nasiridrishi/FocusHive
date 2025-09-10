/**
 * Enhanced Responsive Layout System
 * 
 * Updated AppLayout component that integrates all the new responsive features
 * and provides a unified layout experience across all screen sizes
 */

import React, { useState, useEffect } from 'react'
import { Box, useTheme } from '@mui/material'
import { Outlet, useLocation } from 'react-router-dom'
import { 
  Home as HomeIcon, 
  Dashboard as DashboardIcon, 
  Business as BusinessIcon, 
  Assignment as AssignmentIcon, 
  Search as SearchIcon, 
  Chat as ChatIcon 
} from '@mui/icons-material'
import { AdaptiveNavigation } from './AdaptiveNavigation'
import { ResponsiveContainer } from './ResponsiveContainer'
import { useResponsive, useScrollDirection, useDynamicViewportHeight } from '../hooks'

interface ResponsiveLayoutProps {
  children?: React.ReactNode
  currentUser?: {
    name: string
    avatar?: string
    email: string
  }
  isConnected?: boolean
  notificationCount?: number
}

export const ResponsiveLayout: React.FC<ResponsiveLayoutProps> = ({
  children,
  currentUser,
  isConnected = true,
  notificationCount = 0,
}) => {
  const theme = useTheme()
  const location = useLocation()
  const { isMobile, isTablet, isDesktop } = useResponsive()
  const scrollDirection = useScrollDirection()
  const { height: viewportHeight } = useDynamicViewportHeight()
  
  // Layout state
  const [sidebarOpen, setSidebarOpen] = useState(!isMobile)
  const [contentPadding, setContentPadding] = useState({
    top: 64, // Default header height
    bottom: isMobile ? 56 : 0, // Bottom navigation on mobile
    left: 0,
    right: 0,
  })
  
  // Update layout when screen size changes
  useEffect(() => {
    if (isMobile) {
      setSidebarOpen(false)
      setContentPadding({
        top: 56, // Mobile header height
        bottom: 56, // Bottom navigation
        left: 0,
        right: 0,
      })
    } else if (isTablet) {
      setSidebarOpen(false)
      setContentPadding({
        top: 64, // Tablet header height
        bottom: 0,
        left: 0,
        right: 0,
      })
    } else {
      setSidebarOpen(true)
      setContentPadding({
        top: 72, // Desktop header height
        bottom: 0,
        left: 280, // Sidebar width
        right: 0,
      })
    }
  }, [isMobile, isTablet])
  
  // Check if we're on authentication pages (no layout needed)
  const isAuthPage = ['/login', '/register'].includes(location.pathname)
  const isLandingPage = location.pathname === '/'
  
  // Navigation items configuration
  const navigationItems = [
    {
      id: 'home',
      label: 'Home',
      icon: <HomeIcon />,
      path: '/',
    },
    {
      id: 'dashboard',
      label: 'Dashboard',
      icon: <DashboardIcon />,
      path: '/dashboard',
      requiresAuth: true,
    },
    {
      id: 'hives',
      label: 'Hives',
      icon: <BusinessIcon />,
      path: '/hives',
      requiresAuth: true,
      children: [
        {
          id: 'my-hives',
          label: 'My Hives',
          icon: <AssignmentIcon />,
          path: '/hives/my',
        },
        {
          id: 'discover',
          label: 'Discover',
          icon: <SearchIcon />,
          path: '/hives/discover',
        },
      ],
    },
    {
      id: 'chat',
      label: 'Chat',
      icon: <ChatIcon />,
      path: '/chat',
      badge: 3,
      requiresAuth: true,
    },
    {
      id: 'timer',
      label: 'Focus',
      icon: <span>⏰</span>,
      path: '/timer',
      requiresAuth: true,
    },
  ]
  
  // Render minimal layout for auth pages
  if (isAuthPage || isLandingPage) {
    return (
      <Box
        id="main-content"
        sx={{
          display: 'flex',
          flexDirection: 'column',
          minHeight: '100vh',
          backgroundColor: 'background.default',
        }}
      >
        {children || <Outlet />}
      </Box>
    )
  }
  
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: '100vh',
        backgroundColor: 'background.default',
        // Set CSS custom property for dynamic viewport height
        '--vh': `${viewportHeight / 100}px`,
      }}
    >
      {/* Adaptive Navigation System */}
      <AdaptiveNavigation
        items={navigationItems}
        currentUser={currentUser}
        isConnected={isConnected}
        notificationCount={notificationCount}
        onNotificationClick={() => {}}
      />
      
      {/* Main Content Area */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          transition: theme.transitions.create(['margin', 'padding'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
          }),
          // Dynamic padding based on layout state with safe areas
          paddingTop: `${contentPadding.top}px`,
          paddingLeft: `max(env(safe-area-inset-left), ${isDesktop && sidebarOpen ? contentPadding.left : 0}px)`,
          paddingRight: `max(env(safe-area-inset-right), ${contentPadding.right}px)`,
          paddingBottom: `max(env(safe-area-inset-bottom), ${contentPadding.bottom}px)`,
          // Ensure content doesn't go below viewport fold
          minHeight: `calc(100vh - ${contentPadding.top + contentPadding.bottom}px)`,
        }}
      >
        {/* Content Container */}
        <ResponsiveContainer
          id="main-content"
          maxWidth="desktopLg"
          fluidPadding={true}
          contentType="dashboard"
          sx={{
            minHeight: '100%',
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          {children || <Outlet />}
        </ResponsiveContainer>
      </Box>
      
      {/* Scroll-to-top button */}
      {scrollDirection === 'up' && (
        <Box
          sx={{
            position: 'fixed',
            bottom: isMobile ? theme.spacing(10) : theme.spacing(4),
            right: theme.spacing(2),
            zIndex: theme.zIndex.fab,
            display: { xs: 'block', md: 'none' }, // Show only on smaller screens
          }}
        >
          {/* Scroll to top functionality would be implemented here */}
        </Box>
      )}
    </Box>
  )
}

// Utility component for page layouts with consistent spacing
export const PageLayout: React.FC<{
  title?: string
  subtitle?: string
  actions?: React.ReactNode
  children: React.ReactNode
  maxWidth?: 'mobile' | 'tablet' | 'desktop' | 'desktopLg' | false
  spacing?: 'compact' | 'normal' | 'spacious'
}> = ({
  title,
  subtitle,
  actions,
  children,
  maxWidth = 'desktop',
  spacing = 'normal',
}) => {
  const spacingMap = {
    compact: { mobile: 2, tablet: 3, desktop: 4 },
    normal: { mobile: 3, tablet: 4, desktop: 6 },
    spacious: { mobile: 4, tablet: 6, desktop: 8 },
  }
  
  return (
    <Box
      sx={{
        width: '100%',
        ...(maxWidth && {
          maxWidth: theme => theme.breakpoints.values[maxWidth],
          mx: 'auto',
        }),
        py: (theme) => ({
          xs: theme.spacing(spacingMap[spacing].mobile),
          sm: theme.spacing(spacingMap[spacing].tablet),
          md: theme.spacing(spacingMap[spacing].desktop),
        }),
      }}
    >
      {/* Page Header */}
      {(title || subtitle || actions) && (
        <Box
          sx={{
            display: 'flex',
            flexDirection: { xs: 'column', sm: 'row' },
            justifyContent: 'space-between',
            alignItems: { xs: 'flex-start', sm: 'center' },
            gap: 2,
            mb: 4,
          }}
        >
          <Box>
            {title && (
              <Box
                component="h1"
                sx={{
                  fontSize: { xs: '1.75rem', sm: '2.125rem', md: '2.5rem' },
                  fontWeight: 700,
                  lineHeight: 1.2,
                  margin: 0,
                  marginBottom: subtitle ? 1 : 0,
                  color: 'text.primary',
                }}
              >
                {title}
              </Box>
            )}
            {subtitle && (
              <Box
                component="p"
                sx={{
                  fontSize: { xs: '1rem', sm: '1.125rem' },
                  color: 'text.secondary',
                  margin: 0,
                  lineHeight: 1.5,
                }}
              >
                {subtitle}
              </Box>
            )}
          </Box>
          {actions && (
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
              {actions}
            </Box>
          )}
        </Box>
      )}
      
      {/* Page Content */}
      <Box>
        {children}
      </Box>
    </Box>
  )
}

export default ResponsiveLayout