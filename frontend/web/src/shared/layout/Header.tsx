import React, { useState } from 'react'
import {
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Box,
  InputBase,
  Badge,
  Menu,
  MenuItem,
  Avatar,
  Chip,
  useTheme,
  alpha,
  Tooltip,
} from '@mui/material'
import {
  Menu as MenuIcon,
  Search as SearchIcon,
  Notifications as NotificationsIcon,
  AccountCircle as AccountCircleIcon,
  Wifi as WifiIcon,
  WifiOff as WifiOffIcon,
  Settings as SettingsIcon,
  Logout as LogoutIcon,
} from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'

interface HeaderProps {
  drawerWidth: number
  onDrawerToggle: () => void
  isConnected: boolean
  isMobile: boolean
}

export const Header: React.FC<HeaderProps> = ({
  drawerWidth,
  onDrawerToggle,
  isConnected,
  isMobile,
}) => {
  const theme = useTheme()
  const navigate = useNavigate()
  
  // State for menus and search
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
  const [notificationsAnchorEl, setNotificationsAnchorEl] = useState<null | HTMLElement>(null)
  const [searchQuery, setSearchQuery] = useState('')
  
  // Mock user data (will come from auth context later)
  const currentUser = {
    name: 'John Doe',
    email: 'john.doe@example.com',
    avatar: null,
  }
  
  // Mock notifications count
  const notificationCount = 3

  const handleProfileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget)
  }

  const handleNotificationsOpen = (event: React.MouseEvent<HTMLElement>) => {
    setNotificationsAnchorEl(event.currentTarget)
  }

  const handleMenuClose = () => {
    setAnchorEl(null)
    setNotificationsAnchorEl(null)
  }

  const handleLogout = () => {
    handleMenuClose()
    // TODO: Implement logout logic
    navigate('/login')
  }

  const handleSettings = () => {
    handleMenuClose()
    navigate('/settings')
  }

  const handleProfile = () => {
    handleMenuClose()
    navigate('/profile')
  }

  const handleSearchSubmit = (event: React.FormEvent) => {
    event.preventDefault()
    if (searchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`)
    }
  }

  return (
    <AppBar
      position="fixed"
      elevation={1}
      sx={{
        width: { md: `calc(100% - ${drawerWidth}px)` },
        ml: { md: `${drawerWidth}px` },
        transition: theme.transitions.create(['width', 'margin'], {
          easing: theme.transitions.easing.sharp,
          duration: theme.transitions.duration.leavingScreen,
        }),
        bgcolor: 'background.paper',
        color: 'text.primary',
        borderBottom: `1px solid ${theme.palette.divider}`,
      }}
    >
      <Toolbar>
        {/* Menu Button for Mobile/Collapsing Drawer */}
        <IconButton
          color="inherit"
          aria-label="toggle drawer"
          edge="start"
          onClick={onDrawerToggle}
          sx={{ mr: 2 }}
        >
          <MenuIcon />
        </IconButton>

        {/* Logo/Title */}
        <Typography
          variant="h6"
          noWrap
          component="div"
          sx={{
            fontWeight: 600,
            color: 'primary.main',
            display: { xs: 'none', sm: 'block' },
          }}
        >
          FocusHive
        </Typography>

        {/* Search Bar */}
        <Box
          component="form"
          onSubmit={handleSearchSubmit}
          sx={{
            position: 'relative',
            borderRadius: theme.shape.borderRadius,
            backgroundColor: alpha(theme.palette.common.black, 0.04),
            '&:hover': {
              backgroundColor: alpha(theme.palette.common.black, 0.08),
            },
            marginLeft: { xs: 1, sm: 3 },
            marginRight: 2,
            width: { xs: '100%', sm: 'auto' },
            maxWidth: { xs: 'none', sm: 400 },
            flexGrow: { xs: 1, sm: 0 },
          }}
        >
          <Box
            sx={{
              padding: theme.spacing(0, 2),
              height: '100%',
              position: 'absolute',
              pointerEvents: 'none',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <SearchIcon color="action" />
          </Box>
          <InputBase
            placeholder="Search hives, users..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            inputProps={{ 'aria-label': 'search' }}
            sx={{
              color: 'inherit',
              width: '100%',
              '& .MuiInputBase-input': {
                padding: theme.spacing(1, 1, 1, 0),
                paddingLeft: `calc(1em + ${theme.spacing(4)})`,
                transition: theme.transitions.create('width'),
                width: { xs: '100%', sm: '20ch', md: '30ch' },
              },
            }}
          />
        </Box>

        {/* Spacer */}
        <Box sx={{ flexGrow: 1 }} />

        {/* Right Side Actions */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {/* Connection Status */}
          <Tooltip title={isConnected ? 'Connected' : 'Disconnected'}>
            <Chip
              icon={isConnected ? <WifiIcon /> : <WifiOffIcon />}
              label={isConnected ? 'Online' : 'Offline'}
              color={isConnected ? 'success' : 'error'}
              variant="outlined"
              size="small"
              sx={{
                display: { xs: 'none', sm: 'flex' },
                '& .MuiChip-icon': {
                  fontSize: '16px',
                },
              }}
            />
          </Tooltip>

          {/* Notifications */}
          <Tooltip title="Notifications">
            <IconButton
              color="inherit"
              onClick={handleNotificationsOpen}
              aria-label={`show ${notificationCount} new notifications`}
            >
              <Badge badgeContent={notificationCount} color="error">
                <NotificationsIcon />
              </Badge>
            </IconButton>
          </Tooltip>

          {/* User Profile */}
          <Tooltip title="Account">
            <IconButton
              edge="end"
              aria-label="account of current user"
              aria-controls="primary-search-account-menu"
              aria-haspopup="true"
              onClick={handleProfileMenuOpen}
              color="inherit"
            >
              {currentUser.avatar ? (
                <Avatar
                  src={currentUser.avatar}
                  alt={currentUser.name}
                  sx={{ width: 32, height: 32 }}
                />
              ) : (
                <AccountCircleIcon />
              )}
            </IconButton>
          </Tooltip>
        </Box>

        {/* Profile Menu */}
        <Menu
          anchorEl={anchorEl}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'right',
          }}
          keepMounted
          transformOrigin={{
            vertical: 'top',
            horizontal: 'right',
          }}
          open={Boolean(anchorEl)}
          onClose={handleMenuClose}
          PaperProps={{
            sx: {
              mt: 1,
              minWidth: 200,
            },
          }}
        >
          <Box sx={{ px: 2, py: 1, borderBottom: `1px solid ${theme.palette.divider}` }}>
            <Typography variant="subtitle2" noWrap>
              {currentUser.name}
            </Typography>
            <Typography variant="body2" color="text.secondary" noWrap>
              {currentUser.email}
            </Typography>
          </Box>
          <MenuItem onClick={handleProfile}>
            <AccountCircleIcon sx={{ mr: 1 }} />
            Profile
          </MenuItem>
          <MenuItem onClick={handleSettings}>
            <SettingsIcon sx={{ mr: 1 }} />
            Settings
          </MenuItem>
          <MenuItem onClick={handleLogout}>
            <LogoutIcon sx={{ mr: 1 }} />
            Logout
          </MenuItem>
        </Menu>

        {/* Notifications Menu */}
        <Menu
          anchorEl={notificationsAnchorEl}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'right',
          }}
          keepMounted
          transformOrigin={{
            vertical: 'top',
            horizontal: 'right',
          }}
          open={Boolean(notificationsAnchorEl)}
          onClose={handleMenuClose}
          PaperProps={{
            sx: {
              mt: 1,
              minWidth: 300,
              maxHeight: 400,
            },
          }}
        >
          <Box sx={{ px: 2, py: 1, borderBottom: `1px solid ${theme.palette.divider}` }}>
            <Typography variant="h6">Notifications</Typography>
          </Box>
          {/* Mock notifications */}
          <MenuItem onClick={handleMenuClose}>
            <Box>
              <Typography variant="body2">
                New member joined "Study Group"
              </Typography>
              <Typography variant="caption" color="text.secondary">
                2 minutes ago
              </Typography>
            </Box>
          </MenuItem>
          <MenuItem onClick={handleMenuClose}>
            <Box>
              <Typography variant="body2">
                Focus session completed!
              </Typography>
              <Typography variant="caption" color="text.secondary">
                1 hour ago
              </Typography>
            </Box>
          </MenuItem>
          <MenuItem onClick={handleMenuClose}>
            <Box>
              <Typography variant="body2">
                Invitation to "Coding Bootcamp"
              </Typography>
              <Typography variant="caption" color="text.secondary">
                3 hours ago
              </Typography>
            </Box>
          </MenuItem>
        </Menu>
      </Toolbar>
    </AppBar>
  )
}

export default Header