import React, { useState, useEffect } from 'react'
import {
  Box,
  Paper,
  Typography,
  Grid,
  Card,
  CardContent,
  Button,
  Tab,
  Tabs,
  Avatar,
  Chip,
  IconButton,
  List,
  ListItem,
  ListItemText,
  ListItemAvatar,
  ListItemSecondaryAction,
  CircularProgress,
  Alert,
  Badge,
  Divider
} from '@mui/material'
import {
  People as PeopleIcon,
  PersonAdd as PersonAddIcon,
  CheckCircle as CheckCircleIcon,
  Schedule as ScheduleIcon,
  TrendingUp as TrendingUpIcon,
  Mood as MoodIcon,
  EmojiEvents as TrophyIcon,
  Settings as SettingsIcon,
  Notifications as NotificationsIcon
} from '@mui/icons-material'
import { buddyApi } from '../services/buddyApi'
import { BuddyRelationship, BuddySession, BuddyGoal, BuddyStats } from '../types'
import BuddyRequestDialog from './BuddyRequestDialog'
import BuddyGoalsList from './BuddyGoalsList'
import BuddySessionCard from './BuddySessionCard'
import BuddyCheckinDialog from './BuddyCheckinDialog'
import BuddyMatchingCard from './BuddyMatchingCard'

interface TabPanelProps {
  children?: React.ReactNode
  index: number
  value: number
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`buddy-tabpanel-${index}`}
      aria-labelledby={`buddy-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  )
}

const BuddyDashboard: React.FC = () => {
  const [tabValue, setTabValue] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  
  // Data states
  const [activeBuddies, setActiveBuddies] = useState<BuddyRelationship[]>([])
  const [pendingRequests, setPendingRequests] = useState<BuddyRelationship[]>([])
  const [sentRequests, setSentRequests] = useState<BuddyRelationship[]>([])
  const [upcomingSessions, setUpcomingSessions] = useState<BuddySession[]>([])
  const [userStats, setUserStats] = useState<BuddyStats | null>(null)
  
  // Dialog states
  const [requestDialogOpen, setRequestDialogOpen] = useState(false)
  const [checkinDialogOpen, setCheckinDialogOpen] = useState(false)
  const [selectedRelationship, setSelectedRelationship] = useState<BuddyRelationship | null>(null)

  useEffect(() => {
    loadDashboardData()
  }, [])

  const loadDashboardData = async () => {
    try {
      setLoading(true)
      const [buddies, pending, sent, sessions, stats] = await Promise.all([
        buddyApi.getActiveBuddies(),
        buddyApi.getPendingRequests(),
        buddyApi.getSentRequests(),
        buddyApi.getUpcomingSessions(),
        buddyApi.getUserStats()
      ])
      
      setActiveBuddies(buddies)
      setPendingRequests(pending)
      setSentRequests(sent)
      setUpcomingSessions(sessions)
      setUserStats(stats)
    } catch (err) {
      setError('Failed to load buddy dashboard data')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleAcceptRequest = async (relationshipId: number) => {
    try {
      await buddyApi.acceptBuddyRequest(relationshipId)
      await loadDashboardData()
    } catch (err) {
      setError('Failed to accept buddy request')
    }
  }

  const handleRejectRequest = async (relationshipId: number) => {
    try {
      await buddyApi.rejectBuddyRequest(relationshipId)
      await loadDashboardData()
    } catch (err) {
      setError('Failed to reject buddy request')
    }
  }

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue)
  }

  const handleOpenCheckin = (buddy: BuddyRelationship) => {
    setSelectedRelationship(buddy)
    setCheckinDialogOpen(true)
  }

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom sx={{ mb: 3 }}>
        Buddy System
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Stats Overview */}
      {userStats && (
        <Grid container spacing={3} sx={{ mb: 3 }}>
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center">
                  <PeopleIcon color="primary" sx={{ mr: 2 }} />
                  <Box>
                    <Typography color="textSecondary" variant="body2">
                      Active Buddies
                    </Typography>
                    <Typography variant="h5">
                      {userStats.activeBuddies || 0}
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center">
                  <TrophyIcon color="primary" sx={{ mr: 2 }} />
                  <Box>
                    <Typography color="textSecondary" variant="body2">
                      Goals Completed
                    </Typography>
                    <Typography variant="h5">
                      {userStats.completedGoals || 0} / {userStats.totalGoals || 0}
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center">
                  <ScheduleIcon color="primary" sx={{ mr: 2 }} />
                  <Box>
                    <Typography color="textSecondary" variant="body2">
                      Total Sessions
                    </Typography>
                    <Typography variant="h5">
                      {userStats.totalSessions || 0}
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center">
                  <MoodIcon color="primary" sx={{ mr: 2 }} />
                  <Box>
                    <Typography color="textSecondary" variant="body2">
                      Buddy Level
                    </Typography>
                    <Typography variant="h5">
                      {userStats.buddyLevel || 'Beginner'}
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* Main Content Tabs */}
      <Paper sx={{ width: '100%' }}>
        <Tabs
          value={tabValue}
          onChange={handleTabChange}
          indicatorColor="primary"
          textColor="primary"
          variant="scrollable"
          scrollButtons="auto"
        >
          <Tab 
            label={
              <Badge badgeContent={activeBuddies.length} color="primary">
                My Buddies
              </Badge>
            }
          />
          <Tab 
            label={
              <Badge badgeContent={pendingRequests.length} color="error">
                Requests
              </Badge>
            }
          />
          <Tab label="Find Buddies" />
          <Tab 
            label={
              <Badge badgeContent={upcomingSessions.length} color="secondary">
                Sessions
              </Badge>
            }
          />
          <Tab label="Goals" />
        </Tabs>

        <TabPanel value={tabValue} index={0}>
          {/* Active Buddies */}
          {activeBuddies.length === 0 ? (
            <Box textAlign="center" py={4}>
              <Typography variant="body1" color="textSecondary" gutterBottom>
                You don't have any active buddies yet
              </Typography>
              <Button
                variant="contained"
                startIcon={<PersonAddIcon />}
                onClick={() => setTabValue(2)}
                sx={{ mt: 2 }}
              >
                Find a Buddy
              </Button>
            </Box>
          ) : (
            <List>
              {activeBuddies.map((buddy) => (
                <ListItem key={buddy.id} divider>
                  <ListItemAvatar>
                    <Avatar src={buddy.partnerAvatar}>
                      {buddy.partnerUsername?.[0].toUpperCase()}
                    </Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={buddy.partnerUsername}
                    secondary={
                      <Box>
                        <Typography variant="caption" display="block">
                          Active since {new Date(buddy.startDate!).toLocaleDateString()}
                        </Typography>
                        <Box mt={1}>
                          <Chip 
                            size="small" 
                            label={`${buddy.completedGoals || 0} goals`}
                            sx={{ mr: 1 }}
                          />
                          <Chip 
                            size="small" 
                            label={`${buddy.totalSessions || 0} sessions`}
                          />
                        </Box>
                      </Box>
                    }
                  />
                  <ListItemSecondaryAction>
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => handleOpenCheckin(buddy)}
                      sx={{ mr: 1 }}
                    >
                      Check In
                    </Button>
                    <Button
                      size="small"
                      variant="outlined"
                      color="secondary"
                    >
                      View Details
                    </Button>
                  </ListItemSecondaryAction>
                </ListItem>
              ))}
            </List>
          )}
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          {/* Requests */}
          <Typography variant="h6" gutterBottom>
            Pending Requests ({pendingRequests.length})
          </Typography>
          {pendingRequests.length === 0 ? (
            <Typography color="textSecondary">No pending requests</Typography>
          ) : (
            <List>
              {pendingRequests.map((request) => (
                <ListItem key={request.id} divider>
                  <ListItemAvatar>
                    <Avatar src={request.user1Avatar}>
                      {request.user1Username?.[0].toUpperCase()}
                    </Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={request.user1Username}
                    secondary={`Requested ${new Date(request.createdAt).toLocaleDateString()}`}
                  />
                  <ListItemSecondaryAction>
                    <Button
                      size="small"
                      variant="contained"
                      color="primary"
                      onClick={() => handleAcceptRequest(request.id)}
                      sx={{ mr: 1 }}
                    >
                      Accept
                    </Button>
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => handleRejectRequest(request.id)}
                    >
                      Decline
                    </Button>
                  </ListItemSecondaryAction>
                </ListItem>
              ))}
            </List>
          )}
          
          <Divider sx={{ my: 3 }} />
          
          <Typography variant="h6" gutterBottom>
            Sent Requests ({sentRequests.length})
          </Typography>
          {sentRequests.length === 0 ? (
            <Typography color="textSecondary">No sent requests</Typography>
          ) : (
            <List>
              {sentRequests.map((request) => (
                <ListItem key={request.id} divider>
                  <ListItemAvatar>
                    <Avatar src={request.user2Avatar}>
                      {request.user2Username?.[0].toUpperCase()}
                    </Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={request.user2Username}
                    secondary={`Sent ${new Date(request.createdAt).toLocaleDateString()}`}
                  />
                  <ListItemSecondaryAction>
                    <Chip label="Pending" size="small" />
                  </ListItemSecondaryAction>
                </ListItem>
              ))}
            </List>
          )}
        </TabPanel>

        <TabPanel value={tabValue} index={2}>
          {/* Find Buddies */}
          <BuddyMatchingCard onMatchFound={loadDashboardData} />
        </TabPanel>

        <TabPanel value={tabValue} index={3}>
          {/* Sessions */}
          <Typography variant="h6" gutterBottom>
            Upcoming Sessions
          </Typography>
          {upcomingSessions.length === 0 ? (
            <Typography color="textSecondary">No upcoming sessions</Typography>
          ) : (
            <Grid container spacing={2}>
              {upcomingSessions.map((session) => (
                <Grid item xs={12} md={6} key={session.id}>
                  <BuddySessionCard 
                    session={session}
                    onUpdate={loadDashboardData}
                  />
                </Grid>
              ))}
            </Grid>
          )}
        </TabPanel>

        <TabPanel value={tabValue} index={4}>
          {/* Goals */}
          {activeBuddies.length > 0 && (
            <BuddyGoalsList 
              relationshipId={activeBuddies[0].id}
              onUpdate={loadDashboardData}
            />
          )}
        </TabPanel>
      </Paper>

      {/* Dialogs */}
      <BuddyRequestDialog 
        open={requestDialogOpen}
        onClose={() => setRequestDialogOpen(false)}
        onSent={loadDashboardData}
      />
      
      {selectedRelationship && (
        <BuddyCheckinDialog
          open={checkinDialogOpen}
          onClose={() => setCheckinDialogOpen(false)}
          relationship={selectedRelationship}
          onSubmit={loadDashboardData}
        />
      )}
    </Box>
  )
}

export default BuddyDashboard