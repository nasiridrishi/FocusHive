import React from 'react'
import {
  SwipeableDrawer,
  Box,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  Divider,
  Avatar,
  IconButton,
  useTheme,
} from '@mui/material'
import {
  Dashboard as DashboardIcon,
  Groups as GroupsIcon,
  Public as PublicIcon,
  Analytics as AnalyticsIcon,
  Timer as TimerIcon,
  Settings as SettingsIcon,
  Help as HelpIcon,
  Close as CloseIcon,
  AccountCircle as AccountCircleIcon,
} from '@mui/icons-material'
import { useNavigate, useLocation } from 'react-router-dom'

interface NavigationDrawerProps {
  open: boolean
  onClose: () => void
  width: number
}

// Check if running on iOS for optimizations
const iOS = typeof navigator !== 'undefined' && /iPad|iPhone|iPod/.test(navigator.userAgent)

export const NavigationDrawer: React.FC<NavigationDrawerProps> = ({
  open,
  onClose,
  width,
}) => {
  const theme = useTheme()
  const navigate = useNavigate()
  const location = useLocation()
  
  // Mock user data (will come from auth context later)
  const currentUser = {
    name: 'John Doe',
    email: 'john.doe@example.com',
    avatar: null,
  }

  const handleNavigation = (path: string) => {
    navigate(path)
    onClose()
  }

  const isActiveRoute = (path: string) => {
    return location.pathname === path
  }

  const navigationItems = [
    { path: '/dashboard', label: 'Dashboard', icon: DashboardIcon },
    { path: '/hives', label: 'My Hives', icon: GroupsIcon },
    { path: '/discover', label: 'Discover Hives', icon: PublicIcon },
    { path: '/analytics', label: 'Analytics', icon: AnalyticsIcon },
    { path: '/timer', label: 'Focus Timer', icon: TimerIcon },
  ]

  const bottomNavigationItems = [
    { path: '/settings', label: 'Settings', icon: SettingsIcon },
    { path: '/help', label: 'Help & Support', icon: HelpIcon },
  ]

  const drawerContent = (
    <Box
      sx={{
        width: width,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        bgcolor: 'background.default',
      }}
      role="presentation"
    >
      {/* Header with User Info and Close Button */}
      <Box
        sx={{
          p: 2,
          display: 'flex',
          alignItems: 'center',
          bgcolor: 'primary.main',
          color: 'primary.contrastText',
        }}
      >
        <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', gap: 2 }}>
          {currentUser.avatar ? (
            <Avatar
              src={currentUser.avatar}
              alt={currentUser.name}
              sx={{ width: 40, height: 40 }}
            />
          ) : (
            <Avatar sx={{ width: 40, height: 40, bgcolor: 'primary.dark' }}>
              <AccountCircleIcon />
            </Avatar>
          )}
          <Box>
            <Typography variant="subtitle1" fontWeight={600}>
              {currentUser.name}
            </Typography>
            <Typography variant="body2" sx={{ opacity: 0.8 }}>
              {currentUser.email}
            </Typography>
          </Box>
        </Box>
        <IconButton
          onClick={onClose}
          sx={{
            color: 'primary.contrastText',
            '&:hover': {
              bgcolor: 'primary.dark',
            },
          }}
        >
          <CloseIcon />
        </IconButton>
      </Box>

      {/* Main Navigation */}
      <List sx={{ px: 1, py: 2, flex: 1 }}>
        {navigationItems.map((item) => {
          const Icon = item.icon
          const isActive = isActiveRoute(item.path)
          
          return (
            <ListItem key={item.path} disablePadding sx={{ mb: 0.5 }}>
              <ListItemButton
                selected={isActive}
                onClick={() => handleNavigation(item.path)}
                sx={{
                  borderRadius: 1,
                  py: 1.5,
                  '&.Mui-selected': {
                    bgcolor: 'primary.main',
                    color: 'primary.contrastText',
                    '&:hover': {
                      bgcolor: 'primary.dark',
                    },
                    '& .MuiListItemIcon-root': {
                      color: 'primary.contrastText',
                    },
                  },
                }}
              >
                <ListItemIcon>
                  <Icon />
                </ListItemIcon>
                <ListItemText 
                  primary={item.label}
                  primaryTypographyProps={{
                    variant: 'body1',
                    fontWeight: isActive ? 600 : 400,
                  }}
                />
              </ListItemButton>
            </ListItem>
          )
        })}
      </List>

      {/* Bottom Navigation */}
      <Box sx={{ mt: 'auto' }}>
        <Divider sx={{ mx: 2, mb: 1 }} />
        <List sx={{ px: 1, pb: 2 }}>
          {bottomNavigationItems.map((item) => {
            const Icon = item.icon
            const isActive = isActiveRoute(item.path)
            
            return (
              <ListItem key={item.path} disablePadding sx={{ mb: 0.5 }}>
                <ListItemButton
                  selected={isActive}
                  onClick={() => handleNavigation(item.path)}
                  sx={{
                    borderRadius: 1,
                    py: 1.5,
                    '&.Mui-selected': {
                      bgcolor: 'primary.main',
                      color: 'primary.contrastText',
                      '&:hover': {
                        bgcolor: 'primary.dark',
                      },
                      '& .MuiListItemIcon-root': {
                        color: 'primary.contrastText',
                      },
                    },
                  }}
                >
                  <ListItemIcon>
                    <Icon />
                  </ListItemIcon>
                  <ListItemText 
                    primary={item.label}
                    primaryTypographyProps={{
                      variant: 'body1',
                      fontWeight: isActive ? 600 : 400,
                    }}
                  />
                </ListItemButton>
              </ListItem>
            )
          })}
        </List>
      </Box>

      {/* App Version Footer */}
      <Box
        sx={{
          p: 2,
          textAlign: 'center',
          borderTop: `1px solid ${theme.palette.divider}`,
        }}
      >
        <Typography variant="caption" color="text.secondary">
          FocusHive v1.0.0
        </Typography>
      </Box>
    </Box>
  )

  return (
    <SwipeableDrawer
      anchor="left"
      open={open}
      onClose={onClose}
      onOpen={() => {}} // Required for SwipeableDrawer but we don't use swipe to open
      disableBackdropTransition={!iOS}
      disableDiscovery={iOS}
      ModalProps={{
        keepMounted: true, // Better open performance on mobile
      }}
      PaperProps={{
        sx: {
          width: width,
          bgcolor: 'background.default',
        },
      }}
    >
      {drawerContent}
    </SwipeableDrawer>
  )
}

export default NavigationDrawer