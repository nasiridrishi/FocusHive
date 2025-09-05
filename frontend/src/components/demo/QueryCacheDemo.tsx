/**
 * Query Cache Demo Component
 * 
 * Demonstrates TanStack Query v5 caching features including:
 * - Different cache strategies
 * - Optimistic updates
 * - Real-time polling
 * - Offline persistence
 * - Performance monitoring
 */

import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Grid: GridComponent,
  Chip,
  Switch,
  FormControlLabel,
  Alert,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  Tabs,
  Tab,
  CircularProgress,
  LinearProgress,
} from '@mui/material';

// Type assertion for Grid component to work around TypeScript issues
const GridComponent = Grid as any;
import {
  Refresh,
  ClearAll,
  Speed,
  Storage,
  NetworkCheck,
  Timer,
} from '@mui/icons-material';
import { useAuth, useHives, useMyPresence, queryClient, cacheUtils, queryPerformanceUtils } from '@hooks/api';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`cache-demo-tabpanel-${index}`}
      aria-labelledby={`cache-demo-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

export const QueryCacheDemo: React.FC = () => {
  const [tabValue, setTabValue] = useState(0);
  const [enablePolling, setEnablePolling] = useState(true);
  const [cacheInfo, setCacheInfo] = useState<{
    queryCount?: number;
    mutationCount?: number;
    cacheSize?: number;
  }>({});

  // Demo hooks
  const auth = useAuth();
  const hives = useHives();
  const presence = useMyPresence();

  // Cache monitoring
  React.useEffect(() => {
    const updateCacheInfo = () => {
      setCacheInfo({
        queryCount: queryClient.getQueryCache().getAll().length,
        mutationCount: queryClient.getMutationCache().getAll().length,
        cacheSize: queryPerformanceUtils.getCacheSize(),
      });
    };

    updateCacheInfo();
    const interval = setInterval(updateCacheInfo, 2000);
    return () => clearInterval(interval);
  }, []);

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const handleClearCache = (type: 'all' | 'auth' | 'hives' | 'presence') => {
    switch (type) {
      case 'all':
        queryClient.clear();
        break;
      case 'auth':
        queryClient.removeQueries({ queryKey: ['auth'] });
        break;
      case 'hives':
        queryClient.removeQueries({ queryKey: ['hives'] });
        break;
      case 'presence':
        queryClient.removeQueries({ queryKey: ['presence'] });
        break;
    }
  };

  const handleRefetchAll = () => {
    queryClient.refetchQueries();
  };

  return (
    <Box sx={{ width: '100%' }}>
      <Typography variant="h4" gutterBottom>
        TanStack Query Cache Demo
      </Typography>
      
      <Alert severity="info" sx={{ mb: 3 }}>
        This demo showcases the enhanced caching system with offline persistence, 
        optimistic updates, and performance monitoring.
      </Alert>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Cache Statistics
          </Typography>
          <GridComponent container spacing={2}>
            <GridComponentComponent item xs={12} sm={3}>
              <Box display="flex" alignItems="center" gap={1}>
                <Storage color="primary" />
                <Typography variant="body2">
                  Queries: {cacheInfo.queryCount || 0}
                </Typography>
              </Box>
            </GridComponent>
            <GridComponentComponent item xs={12} sm={3}>
              <Box display="flex" alignItems="center" gap={1}>
                <Speed color="secondary" />
                <Typography variant="body2">
                  Mutations: {cacheInfo.mutationCount || 0}
                </Typography>
              </Box>
            </GridComponent>
            <GridComponentComponent item xs={12} sm={3}>
              <Box display="flex" alignItems="center" gap={1}>
                <NetworkCheck color="success" />
                <Typography variant="body2">
                  Cache Size: ~{Math.round((cacheInfo.cacheSize || 0) / 1024)} KB
                </Typography>
              </Box>
            </GridComponent>
            <GridComponentComponent item xs={12} sm={3}>
              <FormControlLabel
                control={
                  <Switch
                    checked={enablePolling}
                    onChange={(e) => setEnablePolling(e.target.checked)}
                  />
                }
                label="Real-time Polling"
              />
            </GridComponent>
          </GridComponent>
          
          <Box sx={{ mt: 2, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            <Button
              size="small"
              startIcon={<Refresh />}
              onClick={handleRefetchAll}
              variant="outlined"
            >
              Refetch All
            </Button>
            <Button
              size="small"
              startIcon={<ClearAll />}
              onClick={() => handleClearCache('all')}
              variant="outlined"
              color="warning"
            >
              Clear All Cache
            </Button>
            <Button
              size="small"
              onClick={() => cacheUtils.prefetchCommonData()}
              variant="outlined"
              color="success"
            >
              Prefetch Common Data
            </Button>
          </Box>
        </CardContent>
      </Card>

      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={tabValue} onChange={handleTabChange} aria-label="cache demo tabs">
          <Tab label="Authentication" />
          <Tab label="Hives" />
          <Tab label="Presence" />
          <Tab label="Performance" />
        </Tabs>
      </Box>

      <TabPanel value={tabValue} index={0}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Authentication Cache
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              User data cached with short-term strategy (5 min) for security.
              Demonstrates optimistic updates and automatic token refresh.
            </Typography>
            
            <GridComponentComponent container spacing={2}>
              <GridComponentComponentComponent item xs={12} md={6}>
                <Box>
                  <Typography variant="subtitle1" gutterBottom>
                    Current State
                  </Typography>
                  <List dense>
                    <ListItem>
                      <ListItemText 
                        primary="Authentication Status" 
                        secondary={auth.isAuthenticated ? 'Authenticated' : 'Not authenticated'}
                      />
                      <ListItemSecondaryAction>
                        <Chip 
                          label={auth.isAuthenticated ? 'Authenticated' : 'Not Auth'}
                          color={auth.isAuthenticated ? 'success' : 'error'}
                          size="small"
                        />
                      </ListItemSecondaryAction>
                    </ListItem>
                    <ListItem>
                      <ListItemText 
                        primary="User Loading" 
                        secondary={auth.isUserLoading ? 'Loading...' : 'Complete'}
                      />
                      {auth.isUserLoading && <CircularProgress size={20} />}
                    </ListItem>
                    <ListItem>
                      <ListItemText 
                        primary="Cache Status" 
                        secondary="Fresh data from cache"
                      />
                      <ListItemSecondaryAction>
                        <Chip label="Cached" color="info" size="small" />
                      </ListItemSecondaryAction>
                    </ListItem>
                  </List>
                </Box>
              </GridComponent>
              <GridComponentComponentComponent item xs={12} md={6}>
                <Box>
                  <Typography variant="subtitle1" gutterBottom>
                    Actions
                  </Typography>
                  <Box display="flex" flexDirection="column" gap={1}>
                    <Button
                      variant="outlined"
                      onClick={() => auth.refetchUser()}
                      disabled={auth.isUserLoading}
                    >
                      Refetch User Data
                    </Button>
                    <Button
                      variant="outlined"
                      color="warning"
                      onClick={() => handleClearCache('auth')}
                    >
                      Clear Auth Cache
                    </Button>
                    {auth.user && (
                      <Button
                        variant="outlined"
                        color="success"
                        disabled={true}
                      >
                        Optimistic Update Demo
                      </Button>
                    )}
                  </Box>
                </Box>
              </GridComponent>
            </GridComponent>
          </CardContent>
        </Card>
      </TabPanel>

      <TabPanel value={tabValue} index={1}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Hives Cache
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              Hive data cached with medium-term strategy (2 hours).
              Shows background refetching and infinite scroll caching.
            </Typography>
            
            <GridComponentComponent container spacing={2}>
              <GridComponentComponentComponent item xs={12} md={6}>
                <Box>
                  <Typography variant="subtitle1" gutterBottom>
                    Cache Status
                  </Typography>
                  <List dense>
                    <ListItem>
                      <ListItemText 
                        primary="Hives Loaded" 
                        secondary={hives.data?.length || 0}
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemText 
                        primary="Loading State" 
                        secondary={hives.isLoading ? 'Loading...' : 'Complete'}
                      />
                      {hives.isLoading && <CircularProgress size={20} />}
                    </ListItem>
                    <ListItem>
                      <ListItemText 
                        primary="Background Fetching" 
                        secondary={hives.isFetching && !hives.isLoading ? 'Yes' : 'No'}
                      />
                      {hives.isFetching && !hives.isLoading && <CircularProgress size={20} />}
                    </ListItem>
                    <ListItem>
                      <ListItemText 
                        primary="Error State" 
                        secondary={hives.error ? 'Error occurred' : 'No errors'}
                      />
                      {hives.error && <Chip label="Error" color="error" size="small" />}
                    </ListItem>
                  </List>
                </Box>
              </GridComponent>
              <GridComponentComponentComponent item xs={12} md={6}>
                <Box>
                  <Typography variant="subtitle1" gutterBottom>
                    Actions
                  </Typography>
                  <Box display="flex" flexDirection="column" gap={1}>
                    <Button
                      variant="outlined"
                      onClick={() => hives.refetch()}
                      disabled={hives.isLoading}
                    >
                      Refetch Hives
                    </Button>
                    <Button
                      variant="outlined"
                      color="warning"
                      onClick={() => handleClearCache('hives')}
                    >
                      Clear Hives Cache
                    </Button>
                    <Button
                      variant="outlined"
                      color="info"
                      disabled={true}
                    >
                      Infinite Scroll Demo
                    </Button>
                  </Box>
                </Box>
              </GridComponent>
            </GridComponent>
          </CardContent>
        </Card>
      </TabPanel>

      <TabPanel value={tabValue} index={2}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Real-time Presence Cache
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              Presence data with real-time polling (5-10 second intervals).
              Demonstrates high-frequency updates and background synchronization.
            </Typography>
            
            <GridComponentComponent container spacing={2}>
              <GridComponentComponentComponent item xs={12} md={6}>
                <Box>
                  <Typography variant="subtitle1" gutterBottom>
                    Presence Status
                  </Typography>
                  <List dense>
                    <ListItem>
                      <ListItemText 
                        primary="Online Status" 
                        secondary={presence.data?.isOnline ? 'Online' : 'Offline'}
                      />
                      <ListItemSecondaryAction>
                        <Chip 
                          label={presence.data?.isOnline ? 'Online' : 'Offline'}
                          color={presence.data?.isOnline ? 'success' : 'error'}
                          size="small"
                        />
                      </ListItemSecondaryAction>
                    </ListItem>
                    <ListItem>
                      <ListItemText 
                        primary="Current Activity" 
                        secondary={presence.data?.currentActivity || 'Not specified'}
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemText 
                        primary="Last Update" 
                        secondary={presence.data?.lastActivity ? 
                          new Date(presence.data.lastActivity).toLocaleTimeString() : 'Never'
                        }
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemText 
                        primary="Polling Status" 
                        secondary={enablePolling ? 'Active' : 'Disabled'}
                      />
                      <ListItemSecondaryAction>
                        <Timer color={enablePolling ? 'success' : 'disabled'} />
                      </ListItemSecondaryAction>
                    </ListItem>
                  </List>
                </Box>
              </GridComponent>
              <GridComponentComponentComponent item xs={12} md={6}>
                <Box>
                  <Typography variant="subtitle1" gutterBottom>
                    Real-time Controls
                  </Typography>
                  <Box display="flex" flexDirection="column" gap={1}>
                    <Button
                      variant="outlined"
                      onClick={() => presence.refetch()}
                      disabled={presence.isLoading}
                    >
                      Manual Refresh
                    </Button>
                    <Button
                      variant="outlined"
                      color="warning"
                      onClick={() => handleClearCache('presence')}
                    >
                      Clear Presence Cache
                    </Button>
                    <FormControlLabel
                      control={
                        <Switch
                          checked={enablePolling}
                          onChange={(e) => setEnablePolling(e.target.checked)}
                        />
                      }
                      label="Enable Auto-refresh"
                    />
                  </Box>
                </Box>
              </GridComponent>
            </GridComponent>
            
            {presence.isFetching && (
              <Box sx={{ mt: 2 }}>
                <LinearProgress />
                <Typography variant="caption" display="block" sx={{ mt: 1 }}>
                  Fetching latest presence data...
                </Typography>
              </Box>
            )}
          </CardContent>
        </Card>
      </TabPanel>

      <TabPanel value={tabValue} index={3}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Performance Monitoring
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              Monitor query performance, cache efficiency, and memory usage.
            </Typography>
            
            <Alert severity="info" sx={{ mb: 2 }}>
              Performance metrics are collected in development mode for debugging purposes.
            </Alert>
            
            <GridComponentComponent container spacing={2}>
              <GridComponentComponentComponent item xs={12} md={6}>
                <Typography variant="subtitle1" gutterBottom>
                  Cache Metrics
                </Typography>
                <List dense>
                  <ListItem>
                    <ListItemText 
                      primary="Total Queries" 
                      secondary={`${cacheInfo.queryCount || 0} cached queries`}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText 
                      primary="Memory Usage" 
                      secondary={`~${Math.round((cacheInfo.cacheSize || 0) / 1024)} KB`}
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText 
                      primary="Cache Hit Rate" 
                      secondary="Estimated 85% (demo value)"
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemText 
                      primary="Offline Support" 
                      secondary="Enabled with localStorage persistence"
                    />
                    <ListItemSecondaryAction>
                      <Chip label="Active" color="success" size="small" />
                    </ListItemSecondaryAction>
                  </ListItem>
                </List>
              </GridComponent>
              <GridComponentComponentComponent item xs={12} md={6}>
                <Typography variant="subtitle1" gutterBottom>
                  Optimization Tools
                </Typography>
                <Box display="flex" flexDirection="column" gap={1}>
                  <Button
                    variant="outlined"
                    onClick={() => queryPerformanceUtils.optimizeCache()}
                    color="info"
                  >
                    Optimize Cache
                  </Button>
                  <Button
                    variant="outlined"
                    onClick={() => console.log(queryPerformanceUtils.getSlowQueries())}
                    color="warning"
                  >
                    Log Slow Queries
                  </Button>
                  <Button
                    variant="outlined"
                    onClick={() => cacheUtils.syncOfflineData()}
                    color="success"
                  >
                    Sync Offline Data
                  </Button>
                </Box>
              </GridComponent>
            </GridComponent>
          </CardContent>
        </Card>
      </TabPanel>
    </Box>
  );
};

export default QueryCacheDemo;