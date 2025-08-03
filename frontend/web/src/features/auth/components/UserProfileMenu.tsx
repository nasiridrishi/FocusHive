import React, { useState } from 'react'
import {
  Menu,
  MenuItem,
  IconButton,
  Avatar,
  Typography,
  Divider,
  ListItemIcon,
  ListItemText,
  Box,
  Badge
} from '@mui/material'
import {
  AccountCircle,
  Settings,
  Dashboard,
  Logout,
  Person,
  Notifications,
  Help,
  DarkMode,
  LightMode
} from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { User, PresenceStatus } from '@shared/types'

interface UserProfileMenuProps {
  user: User
  presenceStatus?: PresenceStatus
  onLogout: () => void
  onThemeToggle?: () => void
  isDarkMode?: boolean
  notificationCount?: number
}

const getPresenceColor = (status: PresenceStatus = 'offline') => {
  switch (status) {
    case 'online':
      return '#4caf50'
    case 'focusing':
      return '#2196f3'
    case 'break':
      return '#ff9800'
    case 'away':
      return '#ffc107'
    case 'offline':
    default:
      return '#9e9e9e'
  }
}

const getPresenceText = (status: PresenceStatus = 'offline') => {
  switch (status) {
    case 'online':
      return 'Online'
    case 'focusing':
      return 'Focusing'
    case 'break':
      return 'On Break'
    case 'away':
      return 'Away'
    case 'offline':
    default:
      return 'Offline'
  }
}

export default function UserProfileMenu({
  user,
  presenceStatus = 'offline',
  onLogout,
  onThemeToggle,
  isDarkMode = false,
  notificationCount = 0
}: UserProfileMenuProps) {
  const navigate = useNavigate()
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
  const open = Boolean(anchorEl)

  const handleClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget)
  }

  const handleClose = () => {
    setAnchorEl(null)
  }

  const handleMenuItemClick = (action: () => void) => {
    handleClose()
    action()
  }

  const getUserInitials = (user: User) => {
    return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase()
  }

  const getAvatarSrc = (user: User) => {
    return user.avatar || undefined
  }

  return (
    <>
      <IconButton
        onClick={handleClick}
        size="small"
        sx={{ ml: 2 }}
        aria-controls={open ? 'profile-menu' : undefined}
        aria-haspopup="true"
        aria-expanded={open ? 'true' : undefined}
        aria-label="User profile menu"
      >
        <Badge
          overlap="circular"
          anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
          badgeContent={
            <Box
              sx={{
                width: 12,
                height: 12,
                borderRadius: '50%',
                backgroundColor: getPresenceColor(presenceStatus),
                border: '2px solid white'
              }}
            />
          }
        >
          <Avatar
            src={getAvatarSrc(user)}
            sx={{
              width: 40,
              height: 40,
              fontSize: '1rem',
              fontWeight: 'bold'
            }}
          >
            {getUserInitials(user)}
          </Avatar>
        </Badge>
      </IconButton>

      <Menu
        anchorEl={anchorEl}
        id="profile-menu"
        open={open}
        onClose={handleClose}
        onClick={handleClose}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
        sx={{
          '& .MuiPaper-root': {
            minWidth: 280,
            mt: 1.5,
            '& .MuiMenuItem-root': {
              borderRadius: 1,
              mx: 1,
              my: 0.5
            }
          }
        }}
      >
        {/* User Info Header */}
        <Box sx={{ px: 2, py: 1.5 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            {user.firstName} {user.lastName}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            @{user.username}
          </Typography>
          <Typography
            variant="caption"
            sx={{
              color: getPresenceColor(presenceStatus),
              fontWeight: 500,
              display: 'flex',
              alignItems: 'center',
              gap: 0.5,
              mt: 0.5
            }}
          >
            <Box
              sx={{
                width: 6,
                height: 6,
                borderRadius: '50%',
                backgroundColor: getPresenceColor(presenceStatus)
              }}
            />
            {getPresenceText(presenceStatus)}
          </Typography>
        </Box>

        <Divider />

        {/* Navigation Items */}
        <MenuItem onClick={() => handleMenuItemClick(() => navigate('/dashboard'))}>
          <ListItemIcon>
            <Dashboard fontSize="small" />
          </ListItemIcon>
          <ListItemText>Dashboard</ListItemText>
        </MenuItem>

        <MenuItem onClick={() => handleMenuItemClick(() => navigate('/profile'))}>
          <ListItemIcon>
            <Person fontSize="small" />
          </ListItemIcon>
          <ListItemText>My Profile</ListItemText>
        </MenuItem>

        <MenuItem onClick={() => handleMenuItemClick(() => navigate('/notifications'))}>
          <ListItemIcon>
            <Badge badgeContent={notificationCount} color="error">
              <Notifications fontSize="small" />
            </Badge>
          </ListItemIcon>
          <ListItemText>Notifications</ListItemText>
        </MenuItem>

        <MenuItem onClick={() => handleMenuItemClick(() => navigate('/settings'))}>
          <ListItemIcon>
            <Settings fontSize="small" />
          </ListItemIcon>
          <ListItemText>Settings</ListItemText>
        </MenuItem>

        {onThemeToggle && (
          <MenuItem onClick={() => handleMenuItemClick(onThemeToggle)}>
            <ListItemIcon>
              {isDarkMode ? <LightMode fontSize="small" /> : <DarkMode fontSize="small" />}
            </ListItemIcon>
            <ListItemText>
              {isDarkMode ? 'Light Mode' : 'Dark Mode'}
            </ListItemText>
          </MenuItem>
        )}

        <MenuItem onClick={() => handleMenuItemClick(() => navigate('/help'))}>
          <ListItemIcon>
            <Help fontSize="small" />
          </ListItemIcon>
          <ListItemText>Help & Support</ListItemText>
        </MenuItem>

        <Divider />

        {/* Logout */}
        <MenuItem
          onClick={() => handleMenuItemClick(onLogout)}
          sx={{
            color: 'error.main',
            '&:hover': {
              backgroundColor: 'error.lighter'
            }
          }}
        >
          <ListItemIcon>
            <Logout fontSize="small" sx={{ color: 'error.main' }} />
          </ListItemIcon>
          <ListItemText>Sign Out</ListItemText>
        </MenuItem>
      </Menu>
    </>
  )
}