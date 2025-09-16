import React, {useState} from 'react'
import {
  alpha,
  AppBar,
  Avatar,
  Badge,
  Box,
  Chip,
  IconButton,
  InputBase,
  Menu,
  MenuItem,
  Toolbar,
  Tooltip,
  Typography,
  useTheme,
} from '@mui/material'
import {
  AccountCircle as AccountCircleIcon,
  Logout as LogoutIcon,
  Menu as MenuIcon,
  Notifications as NotificationsIcon,
  Search as SearchIcon,
  Settings as SettingsIcon,
  Wifi as WifiIcon,
  WifiOff as WifiOffIcon,
} from '@mui/icons-material'
import {useNavigate} from 'react-router-dom'
import {CompactLanguageSwitcher, useTranslation} from '../components/i18n'

interface HeaderProps {
  drawerWidth: number
  onDrawerToggle: () => void
  isConnected: boolean
}

export const Header: React.FC<HeaderProps> = ({
                                                drawerWidth,
                                                onDrawerToggle,
                                                isConnected,
                                              }) => {
  const theme = useTheme()
  const navigate = useNavigate()
  const {t} = useTranslation('common')

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

  const handleProfileMenuOpen = (event: React.MouseEvent<HTMLElement>): void => {
    setAnchorEl(event.currentTarget)
  }

  const handleNotificationsOpen = (event: React.MouseEvent<HTMLElement>): void => {
    setNotificationsAnchorEl(event.currentTarget)
  }

  const handleMenuClose = (): void => {
    setAnchorEl(null)
    setNotificationsAnchorEl(null)
  }

  const handleLogout = (): void => {
    handleMenuClose()
    // TODO: Implement logout logic
    navigate('/login')
  }

  const handleSettings = (): void => {
    handleMenuClose()
    navigate('/settings')
  }

  const handleProfile = (): void => {
    handleMenuClose()
    navigate('/profile')
  }

  const handleSearchSubmit = (event: React.FormEvent): void => {
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
            width: {md: `calc(100% - ${drawerWidth}px)`},
            ml: {md: `${drawerWidth}px`},
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
              sx={{mr: 2}}
          >
            <MenuIcon/>
          </IconButton>

          {/* Logo/Title */}
          <Typography
              variant="h6"
              noWrap
              component="div"
              sx={{
                fontWeight: 600,
                color: 'primary.main',
                display: {xs: 'none', sm: 'block'},
              }}
          >
            {t('app.name')}
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
                marginLeft: {xs: 1, sm: 3},
                marginRight: 2,
                width: {xs: '100%', sm: 'auto'},
                maxWidth: {xs: 'none', sm: 400},
                flexGrow: {xs: 1, sm: 0},
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
              <SearchIcon color="action"/>
            </Box>
            <InputBase
                placeholder={t('placeholders.search')}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                inputProps={{'aria-label': t('accessibility.aria_label_search')}}
                sx={{
                  color: 'inherit',
                  width: '100%',
                  '& .MuiInputBase-input': {
                    padding: theme.spacing(1, 1, 1, 0),
                    paddingLeft: `calc(1em + ${theme.spacing(4)})`,
                    transition: theme.transitions.create('width'),
                    width: {xs: '100%', sm: '20ch', md: '30ch'},
                  },
                }}
            />
          </Box>

          {/* Spacer */}
          <Box sx={{flexGrow: 1}}/>

          {/* Right Side Actions */}
          <Box sx={{display: 'flex', alignItems: 'center', gap: 1}}>
            {/* Language Switcher */}
            <Box sx={{display: {xs: 'none', md: 'block'}}}>
              <CompactLanguageSwitcher/>
            </Box>

            {/* Connection Status */}
            <Tooltip title={isConnected ? t('status.connected') : t('status.disconnected')}>
              <Chip
                  icon={isConnected ? <WifiIcon/> : <WifiOffIcon/>}
                  label={isConnected ? t('status.online') : t('status.offline')}
                  color={isConnected ? 'success' : 'error'}
                  variant="outlined"
                  size="small"
                  sx={{
                    display: {xs: 'none', sm: 'flex'},
                    '& .MuiChip-icon': {
                      fontSize: '16px',
                    },
                  }}
              />
            </Tooltip>

            {/* Notifications */}
            <Tooltip title={t('navigation.notifications') || 'Notifications'}>
              <IconButton
                  color="inherit"
                  onClick={handleNotificationsOpen}
                  aria-label={`show ${notificationCount} new notifications`}
              >
                <Badge badgeContent={notificationCount} color="error">
                  <NotificationsIcon/>
                </Badge>
              </IconButton>
            </Tooltip>

            {/* User Profile */}
            <Tooltip title={t('navigation.profile')}>
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
                        sx={{width: 32, height: 32}}
                    />
                ) : (
                    <AccountCircleIcon/>
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
            <Box sx={{px: 2, py: 1, borderBottom: `1px solid ${theme.palette.divider}`}}>
              <Typography variant="subtitle2" noWrap>
                {currentUser.name}
              </Typography>
              <Typography variant="body2" color="text.secondary" noWrap>
                {currentUser.email}
              </Typography>
            </Box>
            <MenuItem onClick={handleProfile}>
              <AccountCircleIcon sx={{mr: 1}}/>
              {t('navigation.profile')}
            </MenuItem>
            <MenuItem onClick={handleSettings}>
              <SettingsIcon sx={{mr: 1}}/>
              {t('navigation.settings')}
            </MenuItem>
            <MenuItem onClick={handleLogout}>
              <LogoutIcon sx={{mr: 1}}/>
              {t('navigation.logout')}
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
            <Box sx={{px: 2, py: 1, borderBottom: `1px solid ${theme.palette.divider}`}}>
              <Typography
                  variant="h6">{t('navigation.notifications') || 'Notifications'}</Typography>
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