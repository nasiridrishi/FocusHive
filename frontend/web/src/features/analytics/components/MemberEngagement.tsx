import React from 'react';
import {
  Card,
  CardContent,
  CardHeader,
  Typography,
  Box,
  Avatar,
  Chip,
  LinearProgress,
  Grid,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Divider,
  Badge
} from '@mui/material';
import { 
  Group, 
  TrendingUp, 
  Timer, 
  PlayCircle,
  Person
} from '@mui/icons-material';
import { BarChart } from '@mui/x-charts/BarChart';
import { MemberEngagementProps, MemberEngagementData } from '../types';
import { format } from 'date-fns';

const getEngagementColor = (engagement: MemberEngagementData['engagement']) => {
  switch (engagement) {
    case 'high': return 'success';
    case 'medium': return 'warning';
    case 'low': return 'error';
    default: return 'default';
  }
};

const formatFocusTime = (minutes: number): string => {
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  return `${hours}h ${remainingMinutes}m`;
};

const getInitials = (name: string): string => {
  return name
    .split(' ')
    .map(part => part.charAt(0))
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const sortMembers = (
  members: MemberEngagementData[], 
  sortBy: MemberEngagementProps['sortBy']
): MemberEngagementData[] => {
  const sorted = [...members];
  
  switch (sortBy) {
    case 'sessions':
      return sorted.sort((a, b) => b.sessions - a.sessions);
    case 'engagement':
      const engagementOrder = { high: 3, medium: 2, low: 1 };
      return sorted.sort((a, b) => 
        engagementOrder[b.engagement] - engagementOrder[a.engagement]
      );
    case 'focusTime':
    default:
      return sorted.sort((a, b) => b.focusTime - a.focusTime);
  }
};

export const MemberEngagement: React.FC<MemberEngagementProps> = ({
  data,
  maxMembers,
  sortBy = 'focusTime',
  showRank = true,
  currentUserId
}) => {
  if (data.length === 0) {
    return (
      <Card>
        <CardHeader
          title={
            <Box display="flex" alignItems="center" gap={1}>
              <Group color="primary" />
              <Typography variant="h6">Member Engagement</Typography>
            </Box>
          }
        />
        <CardContent>
          <Box textAlign="center" py={4}>
            <Person sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="text.secondary">
              No member engagement data available
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Invite members to your hive to see engagement metrics.
            </Typography>
          </Box>
        </CardContent>
      </Card>
    );
  }

  const sortedMembers = sortMembers(data, sortBy);
  const displayMembers = maxMembers ? sortedMembers.slice(0, maxMembers) : sortedMembers;
  const maxFocusTime = Math.max(...displayMembers.map(m => m.focusTime));

  // Prepare chart data
  const chartData = displayMembers.slice(0, 5).map(member => ({
    member: member.user.name.split(' ')[0], // First name only
    focusTime: member.focusTime,
    sessions: member.sessions
  }));

  return (
    <Card>
      <CardHeader
        title={
          <Box display="flex" alignItems="center" gap={1}>
            <Group color="primary" />
            <Typography variant="h6">Member Engagement</Typography>
            <Chip
              label={`${data.length} members`}
              size="small"
              variant="outlined"
            />
          </Box>
        }
      />
      <CardContent>
        {/* Engagement Chart */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom>
            Focus Time Distribution
          </Typography>
          <Box sx={{ height: 200, width: '100%' }}>
            <BarChart
              dataset={chartData}
              xAxis={[{ scaleType: 'band', dataKey: 'member' }]}
              series={[
                {
                  dataKey: 'focusTime',
                  label: 'Focus Time (minutes)',
                  color: '#1976d2'
                }
              ]}
              height={200}
              margin={{ left: 60, right: 20, top: 20, bottom: 40 }}
            />
          </Box>
        </Box>

        {/* Member List */}
        <List>
          {displayMembers.map((member, index) => (
            <React.Fragment key={member.user.id}>
              <ListItem
                data-testid={`member-card-${member.user.id}`}
                className={currentUserId === member.user.id ? 'highlighted-member' : ''}
                sx={{
                  bgcolor: currentUserId === member.user.id ? 'action.selected' : 'transparent',
                  borderRadius: 1,
                  mb: 1
                }}
              >
                <ListItemAvatar>
                  <Badge
                    badgeContent={showRank ? `#${member.rank}` : undefined}
                    color="primary"
                    overlap="circular"
                    anchorOrigin={{ vertical: 'top', horizontal: 'left' }}
                  >
                    <Avatar
                      src={member.user.avatar}
                      alt={member.user.name}
                      sx={{ width: 48, height: 48 }}
                    >
                      {!member.user.avatar && getInitials(member.user.name)}
                    </Avatar>
                  </Badge>
                </ListItemAvatar>

                <ListItemText
                  primary={
                    <Box display="flex" alignItems="center" gap={1}>
                      <Typography variant="subtitle1" fontWeight="bold">
                        {member.user.name}
                      </Typography>
                      <Chip
                        label={member.engagement}
                        size="small"
                        color={getEngagementColor(member.engagement)}
                        sx={{ textTransform: 'capitalize' }}
                      />
                    </Box>
                  }
                  secondary={`Last active: ${format(member.lastActive, 'MMM d, yyyy')}`}
                />

                <Box sx={{ minWidth: 200 }}>
                  <Grid container spacing={2} alignItems="center">
                    <Grid item xs={6}>
                      <Box display="flex" alignItems="center" gap={0.5}>
                        <Timer fontSize="small" color="action" />
                        <Typography variant="body2" fontWeight="bold">
                          {formatFocusTime(member.focusTime)}
                        </Typography>
                      </Box>
                      <Box display="flex" alignItems="center" gap={0.5}>
                        <PlayCircle fontSize="small" color="action" />
                        <Typography variant="body2">
                          {member.sessions} sessions
                        </Typography>
                      </Box>
                    </Grid>
                    <Grid item xs={6}>
                      <Box>
                        <Typography variant="body2" color="primary" fontWeight="bold">
                          {member.contribution.toFixed(1)}%
                        </Typography>
                        <LinearProgress
                          data-testid={`engagement-bar-${member.user.id}`}
                          variant="determinate"
                          value={maxFocusTime > 0 ? (member.focusTime / maxFocusTime) * 100 : 0}
                          color={getEngagementColor(member.engagement)}
                          sx={{ 
                            height: 6, 
                            borderRadius: 3,
                            width: maxFocusTime > 0 ? `${(member.focusTime / maxFocusTime) * 100}%` : '0%'
                          }}
                        />
                      </Box>
                    </Grid>
                  </Grid>
                </Box>
              </ListItem>
              {index < displayMembers.length - 1 && <Divider variant="inset" />}
            </React.Fragment>
          ))}
        </List>

        {/* Summary Stats */}
        {data.length > 0 && (
          <Box sx={{ mt: 3, p: 2, bgcolor: 'grey.50', borderRadius: 1 }}>
            <Grid container spacing={2}>
              <Grid item xs={3}>
                <Box textAlign="center">
                  <Typography variant="h6" color="success.main">
                    {data.filter(m => m.engagement === 'high').length}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    High Engagement
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={3}>
                <Box textAlign="center">
                  <Typography variant="h6" color="warning.main">
                    {data.filter(m => m.engagement === 'medium').length}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Medium Engagement
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={3}>
                <Box textAlign="center">
                  <Typography variant="h6" color="error.main">
                    {data.filter(m => m.engagement === 'low').length}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Low Engagement
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={3}>
                <Box textAlign="center">
                  <Typography variant="h6" color="primary">
                    {Math.round(data.reduce((sum, m) => sum + m.focusTime, 0) / 60)}h
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Total Focus Time
                  </Typography>
                </Box>
              </Grid>
            </Grid>
          </Box>
        )}

        {maxMembers && data.length > maxMembers && (
          <Box textAlign="center" sx={{ mt: 2 }}>
            <Typography variant="body2" color="text.secondary">
              Showing top {maxMembers} of {data.length} members
            </Typography>
          </Box>
        )}
      </CardContent>
    </Card>
  );
};