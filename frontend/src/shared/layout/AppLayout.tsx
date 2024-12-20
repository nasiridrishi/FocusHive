import React, { useEffect, useState } from 'react'
import {
  Box,
  Drawer,
  useMediaQuery,
  useTheme,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Avatar,
  Menu,
  Breakpoint,
  MenuItem,
  Badge,
  Divider,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  ListItemButton,
  Button,
  CircularProgress,
  Alert,
} from '@mui/material'
import {
  Menu as MenuIcon,
  Notifications as NotificationsIcon,
  Dashboard as DashboardIcon,
  Explore as ExploreIcon,
  Timer as TimerIcon,
  Analytics as AnalyticsIcon,
  Forum as ForumIcon,
  Settings as SettingsIcon,
  AccountCircle,
  Brightness4,
  Brightness7,
  Logout,
  Close as CloseIcon,
} from '@mui/icons-material'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '@features/auth'
import { LogoLink } from '../components/Logo'

const DRAWER_WIDTH = 240

interface AppLayoutProps {
  children?: React.ReactNode
  isLoading?: boolean
  error?: Error | null
}

const navigationItems = [
  { id: 'dashboard', label: 'Dashboard', path: '/dashboard', icon: <DashboardIcon /> },
  { id: 'discover', label: 'Discover', path: '/discover', icon: <ExploreIcon /> },
  { id: 'timer', label: 'Focus Timer', path: '/timer', icon: <TimerIcon /> },
  { id: 'analytics', label: 'Analytics', path: '/analytics', icon: <AnalyticsIcon /> },
  { id: 'forum', label: 'Forum', path: '/forum', icon: <ForumIcon /> },
  { id: 'settings', label: 'Settings', path: '/settings', icon: <SettingsIcon /> },
]

export const AppLayout: React.FC<AppLayoutProps> = ({ children, isLoading = false, error = null }) => {
  const theme = useTheme()
  const location = useLocation()
  const navigate = useNavigate()
  const { authState, logout } = useAuth()
  const user = authState?.user

  const isMobile = useMediaQuery(theme.breakpoints.down('md' as Breakpoint))
  const [mobileOpen, setMobileOpen] = useState(false)
  const [userMenuAnchor, setUserMenuAnchor] = useState<null | HTMLElement>(null)
  const [notificationOpen, setNotificationOpen] = useState(false)
  const [isDarkMode, setIsDarkMode] = useState(false)
  const [unreadNotifications] = useState(3)

  // Auto-close mobile drawer on route change
  useEffect(() => {
    if (isMobile) {
      setMobileOpen(false)
    }
  }, [location.pathname, isMobile])

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen)
  }

  const handleUserMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setUserMenuAnchor(event.currentTarget)
  }

  const handleUserMenuClose = () => {
    setUserMenuAnchor(null)
  }

  const handleLogout = () => {
    logout()
    handleUserMenuClose()
    navigate('/login')
  }

  const handleThemeToggle = () => {
    setIsDarkMode(!isDarkMode)
  }

  const handleNotificationOpen = () => {
    setNotificationOpen(!notificationOpen)
  }

  const handleNavigate = (path: string) => {
    navigate(path)
    if (isMobile) {
      setMobileOpen(false)
    }
  }

  const drawer = (
    <Box>
      <Toolbar />
      <Divider />
      <List>
        {navigationItems.map((item) => {
          const isActive = location.pathname === item.path
          return (
            <ListItem key={item.id} disablePadding>
              <ListItemButton
                data-testid={`nav-link-${item.id}`}
                onClick={() => handleNavigate(item.path)}
                selected={isActive}
                sx={{
                  '&.Mui-selected': {
                    backgroundColor: theme.palette.action.selected,
                  },
                }}
              >
                <ListItemIcon>{item.icon}</ListItemIcon>
                <ListItemText primary={item.label} />
              </ListItemButton>
            </ListItem>
          )
        })}
      </List>
    </Box>
  )

  // Loading state
  if (isLoading) {
    return (
      <Box
        data-testid="layout-loading"
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
        }}
      >
        <CircularProgress />
      </Box>
    )
  }

  // Error state
  if (error) {
    return (
      <Box
        data-testid="error-fallback"
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          p: 3,
        }}
      >
        <Alert severity="error" sx={{ maxWidth: 500 }}>
          <Typography variant="h6">Something went wrong</Typography>
          <Typography>{error.message}</Typography>
        </Alert>
      </Box>
    )
  }

  return (
    <Box data-testid="app-layout" sx={{ display: 'flex' }}>
      {/* Header */}
      <AppBar
        position="fixed"
        data-testid="app-header"
        sx={{
          zIndex: theme.zIndex.drawer + 1,
        }}
        role="banner"
      >
        <Toolbar>
          {/* Mobile menu button */}
          {isMobile && (
            <IconButton
              data-testid="mobile-menu-button"
              color="inherit"
              aria-label="open drawer"
              edge="start"
              onClick={handleDrawerToggle}
              sx={{ mr: 2 }}
            >
              <MenuIcon />
            </IconButton>
          )}

          {/* Desktop sidebar toggle */}
          {!isMobile && (
            <IconButton
              data-testid="sidebar-toggle"
              color="inherit"
              aria-label="toggle sidebar"
              edge="start"
              onClick={handleDrawerToggle}
              sx={{ mr: 2 }}
            >
              <MenuIcon />
            </IconButton>
          )}

          {/* Logo */}
          <Box sx={{ flexGrow: 1 }}>
            <LogoLink
              variant="full"
              height={35}
              onClick={() => navigate('/dashboard')}
            />
          </Box>

          {/* Theme toggle */}
          <IconButton
            data-testid="theme-toggle"
            color="inherit"
            onClick={handleThemeToggle}
            aria-label="toggle theme"
          >
            {isDarkMode ? <Brightness7 /> : <Brightness4 />}
          </IconButton>

          {/* Notifications */}
          <IconButton
            data-testid="notification-button"
            color="inherit"
            onClick={handleNotificationOpen}
            aria-label="show notifications"
          >
            <Badge
              data-testid="notification-badge"
              badgeContent={unreadNotifications}
              color="error"
            >
              <NotificationsIcon />
            </Badge>
          </IconButton>

          {/* User menu */}
          <IconButton
            data-testid="user-menu-button"
            onClick={handleUserMenuOpen}
            color="inherit"
            aria-label="user menu"
          >
            {user?.avatar ? (
              <Avatar src={user.avatar} alt={user.name} />
            ) : (
              <AccountCircle />
            )}
          </IconButton>

          <Menu
            anchorEl={userMenuAnchor}
            open={Boolean(userMenuAnchor)}
            onClose={handleUserMenuClose}
            data-testid="user-menu"
          >
            <MenuItem onClick={() => navigate('/profile')}>
              Profile
            </MenuItem>
            <MenuItem onClick={() => navigate('/settings')}>
              Settings
            </MenuItem>
            <Divider />
            <MenuItem onClick={handleLogout}>
              <ListItemIcon>
                <Logout fontSize="small" />
              </ListItemIcon>
              Logout
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      {/* Sidebar */}
      <Box
        component="nav"
        data-testid="app-sidebar"
        sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}
        aria-label="navigation"
      >
        {/* Mobile drawer */}
        {isMobile ? (
          <Drawer
            variant="temporary"
            open={mobileOpen}
            onClose={handleDrawerToggle}
            ModalProps={{
              keepMounted: true, // Better open performance on mobile
            }}
            sx={{
              display: { xs: 'block', md: 'none' },
              '& .MuiDrawer-paper': {
                boxSizing: 'border-box',
                width: DRAWER_WIDTH,
              },
            }}
          >
            {/* Close button for mobile */}
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', p: 1 }}>
              <IconButton
                data-testid="drawer-overlay"
                onClick={handleDrawerToggle}
              >
                <CloseIcon />
              </IconButton>
            </Box>
            {drawer}
          </Drawer>
        ) : (
          /* Desktop drawer */
          <Drawer
            variant="persistent"
            open={!isMobile}
            sx={{
              display: { xs: 'none', md: 'block' },
              '& .MuiDrawer-paper': {
                boxSizing: 'border-box',
                width: DRAWER_WIDTH,
              },
            }}
          >
            {drawer}
          </Drawer>
        )}
      </Box>

      {/* Main content */}
      <Box
        component="main"
        role="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          mt: '64px', // AppBar height
        }}
      >
        {children || <Outlet data-testid="router-outlet" />}
      </Box>

      {/* Footer */}
      <Box
        component="footer"
        data-testid="app-footer"
        role="contentinfo"
        sx={{
          position: 'fixed',
          bottom: 0,
          left: { md: DRAWER_WIDTH },
          right: 0,
          p: 2,
          bgcolor: 'background.paper',
          borderTop: 1,
          borderColor: 'divider',
          textAlign: 'center',
        }}
      >
        <Typography variant="caption" color="text.secondary">
          © 2025 FocusHive. All rights reserved.
        </Typography>
      </Box>

      {/* Notifications Panel - for demonstration */}
      {notificationOpen && (
        <Box
          data-testid="notifications-panel"
          sx={{
            position: 'absolute',
            top: 64,
            right: 0,
            width: 320,
            bgcolor: 'background.paper',
            boxShadow: 3,
            p: 2,
            zIndex: theme.zIndex.drawer,
          }}
        >
          <Typography variant="h6">Notifications</Typography>
          <Typography variant="body2">You have {unreadNotifications} unread notifications</Typography>
        </Box>
      )}
    </Box>
  )
}

export default AppLayout