import React, {useEffect, useState} from 'react'
import {Box, Drawer, useMediaQuery, useTheme,} from '@mui/material'
import {Outlet, useLocation} from 'react-router-dom'
import {Header} from './Header'
import {Sidebar} from './Sidebar'
import {NavigationDrawer} from './NavigationDrawer'

const DRAWER_WIDTH = 280
const MOBILE_DRAWER_WIDTH = 280

interface AppLayoutProps {
  children?: React.ReactNode
}

export const AppLayout: React.FC<AppLayoutProps> = ({children}) => {
  const theme = useTheme()
  const location = useLocation()
  const isMobile = useMediaQuery('(max-width: 960px)')

  // Drawer state management
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false)
  const [desktopDrawerOpen, setDesktopDrawerOpen] = useState(true)

  // WebSocket connection state (will be managed by context later)
  const [isConnected] = useState(false)

  // Auto-close mobile drawer on route change
  useEffect(() => {
    if (isMobile) {
      setMobileDrawerOpen(false)
    }
  }, [location.pathname, isMobile])

  // Auto-open desktop drawer on larger screens
  useEffect(() => {
    if (!isMobile && !desktopDrawerOpen) {
      setDesktopDrawerOpen(true)
    }
  }, [isMobile, desktopDrawerOpen])

  const handleDrawerToggle = (): void => {
    if (isMobile) {
      setMobileDrawerOpen(!mobileDrawerOpen)
    } else {
      setDesktopDrawerOpen(!desktopDrawerOpen)
    }
  }

  const handleMobileDrawerClose = (): void => {
    setMobileDrawerOpen(false)
  }

  // Check if we're on authentication pages (no layout needed)
  const isAuthPage = ['/login', '/register'].includes(location.pathname)
  const isLandingPage = location.pathname === '/'

  if (isAuthPage || isLandingPage) {
    return (
        <Box sx={{display: 'flex', flexDirection: 'column', minHeight: '100vh'}}>
          {children || <Outlet/>}
        </Box>
    )
  }

  return (
      <Box sx={{display: 'flex', minHeight: '100vh'}}>
        {/* Header AppBar */}
        <Header
            drawerWidth={isMobile ? 0 : (desktopDrawerOpen ? DRAWER_WIDTH : 0)}
            onDrawerToggle={handleDrawerToggle}
            isConnected={isConnected}
        />

        {/* Mobile Navigation Drawer */}
        {isMobile && (
            <NavigationDrawer
                open={mobileDrawerOpen}
                onClose={handleMobileDrawerClose}
                width={MOBILE_DRAWER_WIDTH}
            />
        )}

        {/* Desktop Sidebar Drawer */}
        {!isMobile && (
            <Drawer
                variant="persistent"
                anchor="left"
                open={desktopDrawerOpen}
                sx={{
                  width: desktopDrawerOpen ? DRAWER_WIDTH : 0,
                  flexShrink: 0,
                  '& .MuiDrawer-paper': {
                    width: DRAWER_WIDTH,
                    boxSizing: 'border-box',
                    top: '64px', // Height of AppBar
                    height: 'calc(100vh - 64px)',
                    borderRight: `1px solid ${theme.palette.divider}`,
                    bgcolor: 'background.default',
                  },
                }}
            >
              <Sidebar/>
            </Drawer>
        )}

        {/* Main Content Area */}
        <Box
            component="main"
            sx={{
              flexGrow: 1,
              width: {
                md: desktopDrawerOpen
                    ? `calc(100% - ${DRAWER_WIDTH}px)`
                    : '100%',
              },
              ml: {
                md: desktopDrawerOpen ? 0 : 0,
              },
              mt: '64px', // Height of AppBar
              transition: theme.transitions.create(['width', 'margin'], {
                easing: theme.transitions.easing.sharp,
                duration: theme.transitions.duration.leavingScreen,
              }),
              bgcolor: 'background.default',
              minHeight: 'calc(100vh - 64px)',
            }}
        >
          {/* Main content container */}
          <Box
              sx={{
                p: 3,
                height: '100%',
              }}
          >
            {children || <Outlet/>}
          </Box>
        </Box>
      </Box>
  )
}

export default AppLayout