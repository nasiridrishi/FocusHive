import React, {useState} from 'react'
import {
  Badge,
  Box,
  Button,
  Chip,
  Collapse,
  Divider,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
} from '@mui/material'
import {
  Add as AddIcon,
  Analytics as AnalyticsIcon,
  Dashboard as DashboardIcon,
  ExpandLess,
  ExpandMore,
  Groups as GroupsIcon,
  Help as HelpIcon,
  Lock as LockIcon,
  PeopleAlt as PeopleAltIcon,
  Public as PublicIcon,
  Settings as SettingsIcon,
  Timer as TimerIcon,
} from '@mui/icons-material'
import {useLocation, useNavigate} from 'react-router-dom'

// Mock data - will be replaced with real data from API/context
const mockHives = [
  {
    id: '1',
    name: 'Study Group',
    isPublic: true,
    currentMembers: 12,
    maxMembers: 20,
    isActive: true,
    onlineMembers: 5,
  },
  {
    id: '2',
    name: 'Work Project',
    isPublic: false,
    currentMembers: 8,
    maxMembers: 15,
    isActive: true,
    onlineMembers: 3,
  },
  {
    id: '3',
    name: 'Reading Club',
    isPublic: true,
    currentMembers: 25,
    maxMembers: 30,
    isActive: false,
    onlineMembers: 0,
  },
]

const mockRecentActivity = [
  {user: 'Alice', action: 'joined Study Group', time: '2 min ago'},
  {user: 'Bob', action: 'completed 25 min session', time: '15 min ago'},
  {user: 'Carol', action: 'started focus session', time: '30 min ago'},
]

export const Sidebar: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()

  const [hivesExpanded, setHivesExpanded] = useState(true)
  const [activityExpanded, setActivityExpanded] = useState(false)

  const handleNavigation = (path: string): void => {
    navigate(path)
  }

  const handleHiveClick = (hiveId: string): void => {
    navigate(`/hive/${hiveId}`)
  }

  const handleCreateHive = (): void => {
    navigate('/hive/create')
  }

  const isActiveRoute = (path: string): boolean => {
    return location.pathname === path
  }

  const navigationItems = [
    {path: '/dashboard', label: 'Dashboard', icon: DashboardIcon},
    {path: '/hives', label: 'My Hives', icon: GroupsIcon},
    {path: '/discover', label: 'Discover', icon: PublicIcon},
    {path: '/analytics', label: 'Analytics', icon: AnalyticsIcon},
    {path: '/timer', label: 'Focus Timer', icon: TimerIcon},
  ]

  const bottomNavigationItems = [
    {path: '/settings', label: 'Settings', icon: SettingsIcon},
    {path: '/help', label: 'Help & Support', icon: HelpIcon},
  ]

  return (
      <Box
          sx={{
            height: '100%',
            display: 'flex',
            flexDirection: 'column',
            bgcolor: 'background.default',
          }}
      >
        {/* Main Navigation */}
        <List sx={{px: 1, py: 2}}>
          {navigationItems.map((item) => {
            const Icon = item.icon
            const isActive = isActiveRoute(item.path)

            return (
                <ListItem key={item.path} disablePadding sx={{mb: 0.5}}>
                  <ListItemButton
                      selected={isActive}
                      onClick={() => handleNavigation(item.path)}
                      sx={{
                        borderRadius: 1,
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
                      <Icon/>
                    </ListItemIcon>
                    <ListItemText
                        primary={item.label}
                        primaryTypographyProps={{
                          variant: 'body2',
                          fontWeight: isActive ? 600 : 400,
                        }}
                    />
                  </ListItemButton>
                </ListItem>
            )
          })}
        </List>

        <Divider sx={{mx: 2}}/>

        {/* My Hives Section */}
        <Box sx={{px: 1, py: 1}}>
          <ListItemButton
              onClick={() => setHivesExpanded(!hivesExpanded)}
              sx={{borderRadius: 1, mb: 1}}
          >
            <ListItemIcon>
              <GroupsIcon/>
            </ListItemIcon>
            <ListItemText
                primary="My Hives"
                primaryTypographyProps={{
                  variant: 'body2',
                  fontWeight: 600,
                }}
            />
            <IconButton size="small">
              {hivesExpanded ? <ExpandLess/> : <ExpandMore/>}
            </IconButton>
          </ListItemButton>

          <Collapse in={hivesExpanded} timeout="auto" unmountOnExit>
            <List disablePadding sx={{pl: 1}}>
              {mockHives.map((hive) => (
                  <ListItem key={hive.id} disablePadding sx={{mb: 0.5}}>
                    <ListItemButton
                        onClick={() => handleHiveClick(hive.id)}
                        sx={{
                          borderRadius: 1,
                          py: 0.5,
                        }}
                    >
                      <ListItemIcon sx={{minWidth: 32}}>
                        <Badge
                            badgeContent={hive.onlineMembers}
                            color={hive.isActive ? 'success' : 'default'}
                            variant="dot"
                            invisible={hive.onlineMembers === 0}
                        >
                          {hive.isPublic ? (
                              <PublicIcon sx={{fontSize: 20}}/>
                          ) : (
                              <LockIcon sx={{fontSize: 20}}/>
                          )}
                        </Badge>
                      </ListItemIcon>
                      <Box sx={{flex: 1, minWidth: 0}}>
                        <Typography
                            variant="body2"
                            sx={{
                              fontWeight: 500,
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              whiteSpace: 'nowrap',
                            }}
                        >
                          {hive.name}
                        </Typography>
                        <Box sx={{display: 'flex', alignItems: 'center', gap: 1}}>
                          <Chip
                              size="small"
                              label={`${hive.currentMembers}/${hive.maxMembers}`}
                              variant="outlined"
                              sx={{
                                fontSize: '0.7rem',
                                height: 18,
                              }}
                          />
                          {hive.onlineMembers > 0 && (
                              <Chip
                                  size="small"
                                  label={`${hive.onlineMembers} online`}
                                  color="success"
                                  variant="filled"
                                  sx={{
                                    fontSize: '0.7rem',
                                    height: 18,
                                  }}
                              />
                          )}
                        </Box>
                      </Box>
                    </ListItemButton>
                  </ListItem>
              ))}

              {/* Create New Hive Button */}
              <ListItem disablePadding sx={{mt: 1}}>
                <Button
                    variant="outlined"
                    size="small"
                    startIcon={<AddIcon/>}
                    onClick={handleCreateHive}
                    fullWidth
                    sx={{
                      borderRadius: 1,
                      py: 1,
                      borderStyle: 'dashed',
                      color: 'text.secondary',
                      borderColor: 'divider',
                      '&:hover': {
                        borderColor: 'primary.main',
                        color: 'primary.main',
                      },
                    }}
                >
                  Create Hive
                </Button>
              </ListItem>
            </List>
          </Collapse>
        </Box>

        <Divider sx={{mx: 2}}/>

        {/* Recent Activity Section */}
        <Box sx={{px: 1, py: 1, flexGrow: 1}}>
          <ListItemButton
              onClick={() => setActivityExpanded(!activityExpanded)}
              sx={{borderRadius: 1, mb: 1}}
          >
            <ListItemIcon>
              <PeopleAltIcon/>
            </ListItemIcon>
            <ListItemText
                primary="Recent Activity"
                primaryTypographyProps={{
                  variant: 'body2',
                  fontWeight: 600,
                }}
            />
            <IconButton size="small">
              {activityExpanded ? <ExpandLess/> : <ExpandMore/>}
            </IconButton>
          </ListItemButton>

          <Collapse in={activityExpanded} timeout="auto" unmountOnExit>
            <List disablePadding sx={{pl: 1}}>
              {mockRecentActivity.map((activity, index) => (
                  <ListItem key={index} disablePadding sx={{mb: 1}}>
                    <Box sx={{width: '100%', px: 1}}>
                      <Typography variant="caption" color="text.primary">
                        <strong>{activity.user}</strong> {activity.action}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" display="block">
                        {activity.time}
                      </Typography>
                    </Box>
                  </ListItem>
              ))}
            </List>
          </Collapse>
        </Box>

        {/* Bottom Navigation */}
        <Box sx={{mt: 'auto'}}>
          <Divider sx={{mx: 2, mb: 1}}/>
          <List sx={{px: 1, pb: 2}}>
            {bottomNavigationItems.map((item) => {
              const Icon = item.icon
              const isActive = isActiveRoute(item.path)

              return (
                  <ListItem key={item.path} disablePadding sx={{mb: 0.5}}>
                    <ListItemButton
                        selected={isActive}
                        onClick={() => handleNavigation(item.path)}
                        sx={{
                          borderRadius: 1,
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
                        <Icon/>
                      </ListItemIcon>
                      <ListItemText
                          primary={item.label}
                          primaryTypographyProps={{
                            variant: 'body2',
                            fontWeight: isActive ? 600 : 400,
                          }}
                      />
                    </ListItemButton>
                  </ListItem>
              )
            })}
          </List>
        </Box>
      </Box>
  )
}

export default Sidebar