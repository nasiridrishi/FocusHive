/**
 * TanStack Query v5 Example
 * 
 * Demonstrates the comprehensive caching implementation with:
 * - Real-world usage patterns
 * - Performance optimizations
 * - Offline persistence
 * - Error handling
 */

import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Alert,
  Grid,
  Chip,
  List,
  ListItem,
  ListItemText,
  CircularProgress,
  Switch,
  FormControlLabel,
} from '@mui/material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { queryClient, queryKeys, CACHE_TIMES, STALE_TIMES } from '../lib/queryClient';

// Mock API functions for demonstration
const mockApi = {
  getUser: async (): Promise<{ id: string; name: string; email: string }> => {
    await new Promise(resolve => setTimeout(resolve, 1000));
    return {
      id: '1',
      name: 'John Doe',
      email: 'john@example.com',
    };
  },
  
  getHives: async (): Promise<Array<{ id: string; name: string; members: number }>> => {
    await new Promise(resolve => setTimeout(resolve, 800));
    return [
      { id: '1', name: 'Study Group', members: 5 },
      { id: '2', name: 'Work Focus', members: 12 },
      { id: '3', name: 'Reading Circle', members: 8 },
    ];
  },
  
  updateUser: async (data: { name: string }): Promise<{ id: string; name: string; email: string }> => {
    await new Promise(resolve => setTimeout(resolve, 500));
    return {
      id: '1',
      name: data.name,
      email: 'john@example.com',
    };
  },
};

// Custom hooks using our caching strategy
const useUser = () => {
  return useQuery({
    queryKey: ['user'],
    queryFn: mockApi.getUser,
    staleTime: STALE_TIMES.USER_DATA,
    gcTime: CACHE_TIMES.SHORT,
    retry: 2,
  });
};

const useHives = () => {
  return useQuery({
    queryKey: ['hives'],
    queryFn: mockApi.getHives,
    staleTime: STALE_TIMES.SESSION_DATA,
    gcTime: CACHE_TIMES.MEDIUM,
    retry: 1,
  });
};

const useUpdateUser = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: mockApi.updateUser,
    onMutate: async (newUserData) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: ['user'] });
      
      // Snapshot the previous value
      const previousUser = queryClient.getQueryData(['user']);
      
      // Optimistically update
      queryClient.setQueryData(['user'], (old: any) => ({
        ...old,
        name: newUserData.name,
      }));
      
      return { previousUser };
    },
    onError: (err, newUser, context) => {
      // If the mutation fails, use the context returned from onMutate to roll back
      queryClient.setQueryData(['user'], context?.previousUser);
    },
    onSettled: () => {
      // Always refetch after error or success
      queryClient.invalidateQueries({ queryKey: ['user'] });
    },
  });
};

export const TanStackQueryExample: React.FC = () => {
  const [enableBackgroundRefetch, setEnableBackgroundRefetch] = useState(true);
  const [showAdvanced, setShowAdvanced] = useState(false);
  
  // Use our custom hooks
  const userQuery = useUser();
  const hivesQuery = useHives();
  const updateUserMutation = useUpdateUser();
  const client = useQueryClient();
  
  // Cache statistics
  const getCacheStats = () => {
    const cache = client.getQueryCache();
    return {
      totalQueries: cache.getAll().length,
      staleQueries: cache.getAll().filter(query => query.isStale()).length,
      fetchingQueries: cache.getAll().filter(query => query.state.isFetching).length,
    };
  };
  
  const [cacheStats, setCacheStats] = React.useState(getCacheStats());
  
  React.useEffect(() => {
    const interval = setInterval(() => {
      setCacheStats(getCacheStats());
    }, 1000);
    return () => clearInterval(interval);
  }, [client]);
  
  const handleUpdateUser = () => {
    updateUserMutation.mutate({ name: 'Jane Smith' });
  };
  
  const handleClearCache = () => {
    client.clear();
  };
  
  const handleInvalidateUser = () => {
    client.invalidateQueries({ queryKey: ['user'] });
  };
  
  return (
    <Box sx={{ p: 3, maxWidth: 1200, mx: 'auto' }}>
      <Typography variant="h4" gutterBottom>
        TanStack Query v5 Caching Demo
      </Typography>
      
      <Alert severity="info" sx={{ mb: 3 }}>
        This demonstrates our comprehensive API caching strategy with optimistic updates, 
        background refetching, and offline persistence.
      </Alert>
      
      {/* Cache Statistics */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Cache Statistics
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={4}>
              <Chip 
                label={`Queries: ${cacheStats.totalQueries}`} 
                color="primary" 
                variant="outlined" 
              />
            </Grid>
            <Grid item xs={4}>
              <Chip 
                label={`Stale: ${cacheStats.staleQueries}`} 
                color="warning" 
                variant="outlined" 
              />
            </Grid>
            <Grid item xs={4}>
              <Chip 
                label={`Fetching: ${cacheStats.fetchingQueries}`} 
                color="info" 
                variant="outlined" 
              />
            </Grid>
          </Grid>
          
          <Box sx={{ mt: 2, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            <Button size="small" onClick={() => userQuery.refetch()}>
              Refetch User
            </Button>
            <Button size="small" onClick={() => hivesQuery.refetch()}>
              Refetch Hives
            </Button>
            <Button size="small" onClick={handleInvalidateUser}>
              Invalidate User
            </Button>
            <Button size="small" onClick={handleClearCache} color="warning">
              Clear Cache
            </Button>
            <FormControlLabel
              control={
                <Switch
                  checked={showAdvanced}
                  onChange={(e) => setShowAdvanced(e.target.checked)}
                  size="small"
                />
              }
              label="Show Advanced"
            />
          </Box>
        </CardContent>
      </Card>
      
      <Grid container spacing={3}>
        {/* User Data */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                User Data (Short Cache: 5min)
              </Typography>
              
              {userQuery.isLoading && (
                <Box display="flex" alignItems="center" gap={2}>
                  <CircularProgress size={20} />
                  <Typography>Loading user...</Typography>
                </Box>
              )}
              
              {userQuery.isFetching && !userQuery.isLoading && (
                <Alert severity="info" sx={{ mb: 2 }}>
                  Background refreshing...
                </Alert>
              )}
              
              {userQuery.error && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  Error: {(userQuery.error as Error).message}
                </Alert>
              )}
              
              {userQuery.data && (
                <List dense>
                  <ListItem>
                    <ListItemText primary="Name" secondary={userQuery.data.name} />
                  </ListItem>
                  <ListItem>
                    <ListItemText primary="Email" secondary={userQuery.data.email} />
                  </ListItem>
                </List>
              )}
              
              <Box sx={{ mt: 2 }}>
                <Button
                  variant="outlined"
                  onClick={handleUpdateUser}
                  disabled={updateUserMutation.isPending}
                  sx={{ mr: 1 }}
                >
                  {updateUserMutation.isPending ? 'Updating...' : 'Update User (Optimistic)'}
                </Button>
                
                {updateUserMutation.isSuccess && (
                  <Chip label="Updated!" color="success" size="small" />
                )}
                
                {updateUserMutation.isError && (
                  <Chip label="Failed!" color="error" size="small" />
                )}
              </Box>
              
              {showAdvanced && (
                <Box sx={{ mt: 2, p: 2, bgcolor: 'grey.100', borderRadius: 1 }}>
                  <Typography variant="caption" component="div">
                    Cache Status: {userQuery.isStale ? 'Stale' : 'Fresh'}
                  </Typography>
                  <Typography variant="caption" component="div">
                    Last Updated: {userQuery.dataUpdatedAt ? 
                      new Date(userQuery.dataUpdatedAt).toLocaleTimeString() : 'Never'}
                  </Typography>
                  <Typography variant="caption" component="div">
                    Fetch Status: {userQuery.fetchStatus}
                  </Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
        
        {/* Hives Data */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Hives Data (Medium Cache: 2hr)
              </Typography>
              
              {hivesQuery.isLoading && (
                <Box display="flex" alignItems="center" gap={2}>
                  <CircularProgress size={20} />
                  <Typography>Loading hives...</Typography>
                </Box>
              )}
              
              {hivesQuery.isFetching && !hivesQuery.isLoading && (
                <Alert severity="info" sx={{ mb: 2 }}>
                  Background refreshing...
                </Alert>
              )}
              
              {hivesQuery.error && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  Error: {(hivesQuery.error as Error).message}
                </Alert>
              )}
              
              {hivesQuery.data && (
                <List dense>
                  {hivesQuery.data.map((hive) => (
                    <ListItem key={hive.id}>
                      <ListItemText 
                        primary={hive.name}
                        secondary={`${hive.members} members`}
                      />
                    </ListItem>
                  ))}
                </List>
              )}
              
              {showAdvanced && (
                <Box sx={{ mt: 2, p: 2, bgcolor: 'grey.100', borderRadius: 1 }}>
                  <Typography variant="caption" component="div">
                    Cache Status: {hivesQuery.isStale ? 'Stale' : 'Fresh'}
                  </Typography>
                  <Typography variant="caption" component="div">
                    Last Updated: {hivesQuery.dataUpdatedAt ? 
                      new Date(hivesQuery.dataUpdatedAt).toLocaleTimeString() : 'Never'}
                  </Typography>
                  <Typography variant="caption" component="div">
                    Background Refetch: {enableBackgroundRefetch ? 'Enabled' : 'Disabled'}
                  </Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      
      {/* Advanced Features */}
      {showAdvanced && (
        <Card sx={{ mt: 3 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Advanced Caching Features
            </Typography>
            
            <Grid container spacing={2}>
              <Grid item xs={12} md={4}>
                <Typography variant="subtitle2" gutterBottom>
                  Cache Strategies
                </Typography>
                <List dense>
                  <ListItem>
                    <ListItemText 
                      primary="Short-term (5min)"
                      secondary="User data, sessions"
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText 
                      primary="Medium-term (2hr)"
                      secondary="Hives, static content"
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText 
                      primary="Long-term (24hr)"
                      secondary="Reference data"
                    />
                  </ListItem>
                </List>
              </Grid>
              
              <Grid item xs={12} md={4}>
                <Typography variant="subtitle2" gutterBottom>
                  Optimizations
                </Typography>
                <List dense>
                  <ListItem>
                    <ListItemText 
                      primary="Optimistic Updates"
                      secondary="Instant UI feedback"
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText 
                      primary="Background Refetch"
                      secondary="Fresh data without loading"
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText 
                      primary="Structural Sharing"
                      secondary="Minimize re-renders"
                    />
                  </ListItem>
                </List>
              </Grid>
              
              <Grid item xs={12} md={4}>
                <Typography variant="subtitle2" gutterBottom>
                  Offline Support
                </Typography>
                <List dense>
                  <ListItem>
                    <ListItemText 
                      primary="Local Persistence"
                      secondary="localStorage backup"
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText 
                      primary="Auto-sync"
                      secondary="Sync on reconnect"
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText 
                      primary="Intelligent Retry"
                      secondary="Network-aware retry"
                    />
                  </ListItem>
                </List>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}
    </Box>
  );
};

export default TanStackQueryExample;