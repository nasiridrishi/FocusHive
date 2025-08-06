/**
 * Adaptive Navigation System
 * 
 * Intelligent navigation that adapts to screen size, device type, and user context
 * Includes bottom navigation for mobile, sidebar for desktop, and smart transitions
 */

import React, { useState, useEffect, useMemo } from 'react'
import {
  Box,
  BottomNavigation,
  BottomNavigationAction,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  AppBar,
  Toolbar,
  IconButton,
  Typography,
  Badge,
  Fab,
  Collapse,
  Divider,
  Avatar,
  Chip,
  useTheme,
  alpha,
} from '@mui/material'
import {
  Home as HomeIcon,
  Dashboard as DashboardIcon,
  Group as GroupIcon,
  Chat as ChatIcon,
  Timer as TimerIcon,
  Settings as SettingsIcon,
  Menu as MenuIcon,
  Close as CloseIcon,
  Notifications as NotificationsIcon,
  Search as SearchIcon,
  Add as AddIcon,
  ExpandLess,
  ExpandMore,
  Person as PersonIcon,
} from '@mui/icons-material'
import { useNavigate, useLocation } from 'react-router-dom'
import { styled } from '@mui/material/styles'
import { useResponsive, useScrollDirection, useTouchGestures } from '../hooks'

// Types for navigation items
interface NavigationItem {
  id: string
  label: string
  icon: React.ReactNode
  path: string
  badge?: number
  children?: NavigationItem[]
  requiresAuth?: boolean
  mobileOnly?: boolean
  desktopOnly?: boolean
}

interface AdaptiveNavigationProps {
  items: NavigationItem[]
  currentUser?: {
    name: string
    avatar?: string
    email: string
  }
  onItemClick?: (item: NavigationItem) => void
  onNotificationClick?: () => void
  notificationCount?: number
  isConnected?: boolean
}

// Styled components
const StyledBottomNavigation = styled(BottomNavigation)(({ theme }) => ({
  position: 'fixed',
  bottom: 0,
  left: 0,
  right: 0,
  zIndex: theme.zIndex.appBar,
  borderTop: `1px solid ${theme.palette.divider}`,
  backdropFilter: 'blur(10px)',
  backgroundColor: alpha(theme.palette.background.paper, 0.9),
  '& .MuiBottomNavigationAction-root': {
    minWidth: 'auto',
    paddingTop: theme.spacing(1),
    '&.Mui-selected': {
      color: theme.palette.primary.main,
      '& .MuiBottomNavigationAction-label': {
        fontSize: '0.75rem',
        fontWeight: 600,
      },
    },
  },
}))

const SidebarDrawer = styled(Drawer)(({ theme }) => ({
  '& .MuiDrawer-paper': {
    width: 280,
    backgroundColor: theme.palette.background.default,
    borderRight: `1px solid ${theme.palette.divider}`,
    display: 'flex',
    flexDirection: 'column',
  },
}))

const SidebarHeader = styled(Box)(({ theme }) => ({
  padding: theme.spacing(2),
  borderBottom: `1px solid ${theme.palette.divider}`,
  display: 'flex',
  alignItems: 'center',
  gap: theme.spacing(2),
}))

const FloatingFab = styled(Fab)(({ theme }) => ({
  position: 'fixed',
  bottom: theme.spacing(10), // Above bottom navigation
  right: theme.spacing(2),
  zIndex: theme.zIndex.fab,
  transition: 'transform 0.3s ease-in-out',
  '&.hidden': {
    transform: 'translateY(100px)',
  },
}))

// Mobile Bottom Navigation Component
const MobileBottomNav: React.FC<{
  items: NavigationItem[]
  currentPath: string
  onItemClick: (item: NavigationItem) => void
}> = ({ items, currentPath, onItemClick }) => {
  const scrollDirection = useScrollDirection()
  
  // Filter items for mobile display (max 5 items)
  const mobileItems = useMemo(() => {
    return items
      .filter(item => !item.desktopOnly)
      .slice(0, 5) // Limit to 5 items for better UX
  }, [items])
  
  const currentValue = mobileItems.findIndex(item => item.path === currentPath)
  
  return (
    <StyledBottomNavigation
      value={currentValue >= 0 ? currentValue : false}
      sx={{
        transform: scrollDirection === 'down' ? 'translateY(100%)' : 'translateY(0)',
        transition: 'transform 0.3s ease-in-out',
      }}
    >
      {mobileItems.map((item) => (
        <BottomNavigationAction
          key={item.id}
          label={item.label}
          icon={
            item.badge ? (
              <Badge badgeContent={item.badge} color="error">
                {item.icon}
              </Badge>
            ) : (
              item.icon
            )
          }
          onClick={() => onItemClick(item)}
        />
      ))}
    </StyledBottomNavigation>
  )
}

// Desktop Sidebar Component
const DesktopSidebar: React.FC<{
  items: NavigationItem[]
  currentPath: string
  onItemClick: (item: NavigationItem) => void
  currentUser?: AdaptiveNavigationProps['currentUser']
  isOpen: boolean
  onClose: () => void
}> = ({ items, currentPath, onItemClick, currentUser, isOpen, onClose }) => {
  const [expandedItems, setExpandedItems] = useState<Set<string>>(new Set())
  
  const toggleExpanded = (itemId: string) => {
    setExpandedItems(prev => {
      const newSet = new Set(prev)
      if (newSet.has(itemId)) {
        newSet.delete(itemId)
      } else {
        newSet.add(itemId)
      }
      return newSet
    })
  }
  
  const renderNavigationItem = (item: NavigationItem, depth = 0) => {
    const isActive = currentPath === item.path
    const isExpanded = expandedItems.has(item.id)
    const hasChildren = item.children && item.children.length > 0
    
    return (
      <Box key={item.id}>
        <ListItem disablePadding>
          <ListItemButton
            selected={isActive}
            onClick={() => {
              if (hasChildren) {
                toggleExpanded(item.id)
              } else {
                onItemClick(item)
              }
            }}
            sx={{
              pl: 2 + (depth * 2),
              borderRadius: 1,
              mx: 1,
              mb: 0.5,
              '&.Mui-selected': {
                backgroundColor: (theme) => alpha(theme.palette.primary.main, 0.12),
                color: (theme) => theme.palette.primary.main,
                '&:hover': {
                  backgroundColor: (theme) => alpha(theme.palette.primary.main, 0.16),
                },
              },
            }}
          >
            <ListItemIcon
              sx={{
                color: 'inherit',
                minWidth: 40,
              }}
            >
              {item.badge ? (
                <Badge badgeContent={item.badge} color="error">
                  {item.icon}
                </Badge>
              ) : (
                item.icon
              )}
            </ListItemIcon>
            <ListItemText 
              primary={item.label}
              primaryTypographyProps={{
                variant: 'body2',
                fontWeight: isActive ? 600 : 400,
              }}
            />
            {hasChildren && (
              isExpanded ? <ExpandLess /> : <ExpandMore />
            )}
          </ListItemButton>
        </ListItem>
        
        {hasChildren && (
          <Collapse in={isExpanded} timeout="auto" unmountOnExit>
            <List component="div" disablePadding>
              {item.children!.map(child => renderNavigationItem(child, depth + 1))}
            </List>
          </Collapse>
        )}
      </Box>
    )
  }
  
  return (
    <SidebarDrawer
      variant="persistent"
      anchor="left"
      open={isOpen}
      onClose={onClose}
    >
      {/* Sidebar Header */}
      <SidebarHeader>
        <Avatar
          src={currentUser?.avatar}
          alt={currentUser?.name}
          sx={{ width: 40, height: 40 }}
        >
          {currentUser?.name?.[0]}
        </Avatar>
        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
          <Typography variant="subtitle2" noWrap>
            {currentUser?.name || 'User'}
          </Typography>
          <Typography variant="caption" color="text.secondary" noWrap>
            {currentUser?.email || 'user@example.com'}
          </Typography>
        </Box>
      </SidebarHeader>
      
      {/* Navigation Items */}
      <Box sx={{ flexGrow: 1, overflowY: 'auto', py: 1 }}>
        <List>
          {items
            .filter(item => !item.mobileOnly)
            .map(item => renderNavigationItem(item))}
        </List>
      </Box>
      
      {/* Sidebar Footer */}
      <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
        <Chip
          size="small"
          label="FocusHive v1.0"
          variant="outlined"
          sx={{ width: '100%' }}
        />
      </Box>
    </SidebarDrawer>
  )
}

// Adaptive Header Component
const AdaptiveHeader: React.FC<{
  onMenuClick: () => void
  onNotificationClick?: () => void
  notificationCount?: number
  isConnected?: boolean
  showMenuButton: boolean
}> = ({ onMenuClick, onNotificationClick, notificationCount, isConnected, showMenuButton }) => {
  const theme = useTheme()
  const scrollDirection = useScrollDirection()
  
  return (
    <AppBar
      position="fixed"
      elevation={1}
      sx={{
        transform: scrollDirection === 'down' ? 'translateY(-100%)' : 'translateY(0)',
        transition: 'transform 0.3s ease-in-out',
        backgroundColor: alpha(theme.palette.background.paper, 0.9),
        backdropFilter: 'blur(10px)',
        color: theme.palette.text.primary,
        borderBottom: `1px solid ${theme.palette.divider}`,
      }}
    >
      <Toolbar>
        {showMenuButton && (
          <IconButton
            edge="start"
            color="inherit"
            aria-label="menu"
            onClick={onMenuClick}
            sx={{ mr: 2 }}
          >
            <MenuIcon />
          </IconButton>
        )}
        
        <Typography variant="h6" component="div" sx={{ flexGrow: 1, fontWeight: 600 }}>
          FocusHive
        </Typography>
        
        {/* Connection Status */}
        <Chip
          size="small"
          label={isConnected ? 'Online' : 'Offline'}
          color={isConnected ? 'success' : 'error'}
          variant="outlined"
          sx={{ mr: 1, display: { xs: 'none', sm: 'flex' } }}
        />
        
        {/* Notifications */}
        <IconButton
          color="inherit"
          onClick={onNotificationClick}
          aria-label="notifications"
        >
          <Badge badgeContent={notificationCount} color="error">
            <NotificationsIcon />
          </Badge>
        </IconButton>
      </Toolbar>
    </AppBar>
  )
}

// Main Adaptive Navigation Component
export const AdaptiveNavigation: React.FC<AdaptiveNavigationProps> = ({
  items,
  currentUser,
  onItemClick,
  onNotificationClick,
  notificationCount = 0,
  isConnected = true,
}) => {
  const navigate = useNavigate()
  const location = useLocation()
  const { isMobile, isTablet } = useResponsive()
  const [sidebarOpen, setSidebarOpen] = useState(!isMobile)
  const [fabVisible, setFabVisible] = useState(true)
  const scrollDirection = useScrollDirection()
  
  // Auto-close sidebar on mobile/tablet
  useEffect(() => {
    if (isMobile || isTablet) {
      setSidebarOpen(false)
    } else {
      setSidebarOpen(true)
    }
  }, [isMobile, isTablet])
  
  // Hide FAB when scrolling down on mobile
  useEffect(() => {
    if (isMobile) {
      setFabVisible(scrollDirection !== 'down')
    }
  }, [scrollDirection, isMobile])
  
  const handleItemClick = (item: NavigationItem) => {
    if (onItemClick) {
      onItemClick(item)
    }
    navigate(item.path)
    
    // Close sidebar on mobile after navigation
    if (isMobile || isTablet) {
      setSidebarOpen(false)
    }
  }
  
  const handleMenuToggle = () => {
    setSidebarOpen(!sidebarOpen)
  }
  
  // Default navigation items
  const defaultItems: NavigationItem[] = [
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
      icon: <GroupIcon />,
      path: '/hives',
      requiresAuth: true,
      children: [
        {
          id: 'my-hives',
          label: 'My Hives',
          icon: <GroupIcon />,
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
      badge: 3, // Example badge
      requiresAuth: true,
    },
    {
      id: 'timer',
      label: 'Timer',
      icon: <TimerIcon />,
      path: '/timer',
      requiresAuth: true,
    },
    {
      id: 'settings',
      label: 'Settings',
      icon: <SettingsIcon />,
      path: '/settings',
      requiresAuth: true,
      desktopOnly: true, // Show only on desktop
    },
  ]
  
  const navigationItems = items.length > 0 ? items : defaultItems
  
  return (
    <>
      {/* Adaptive Header */}
      <AdaptiveHeader
        onMenuClick={handleMenuToggle}
        onNotificationClick={onNotificationClick}
        notificationCount={notificationCount}
        isConnected={isConnected}
        showMenuButton={isMobile || isTablet || !sidebarOpen}
      />
      
      {/* Desktop Sidebar */}
      {!isMobile && (
        <DesktopSidebar
          items={navigationItems}
          currentPath={location.pathname}
          onItemClick={handleItemClick}
          currentUser={currentUser}
          isOpen={sidebarOpen}
          onClose={() => setSidebarOpen(false)}
        />
      )}
      
      {/* Mobile Bottom Navigation */}
      {isMobile && (
        <MobileBottomNav
          items={navigationItems}
          currentPath={location.pathname}
          onItemClick={handleItemClick}
        />
      )}
      
      {/* Mobile Drawer */}
      {(isMobile || isTablet) && (
        <Drawer
          anchor="left"
          open={sidebarOpen}
          onClose={() => setSidebarOpen(false)}
          ModalProps={{
            keepMounted: true, // Better open performance on mobile
          }}
        >
          <Box sx={{ width: 280 }}>
            <DesktopSidebar
              items={navigationItems}
              currentPath={location.pathname}
              onItemClick={handleItemClick}
              currentUser={currentUser}
              isOpen={true}
              onClose={() => setSidebarOpen(false)}
            />
          </Box>
        </Drawer>
      )}
      
      {/* Floating Action Button for Quick Actions */}
      {isMobile && (
        <FloatingFab
          color="primary"
          aria-label="quick action"
          className={!fabVisible ? 'hidden' : ''}
          onClick={() => navigate('/create')}
        >
          <AddIcon />
        </FloatingFab>
      )}
    </>
  )
}

export default AdaptiveNavigation