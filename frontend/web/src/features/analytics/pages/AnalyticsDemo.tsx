import React from 'react';
import { Box, Container, Typography, Button, Grid, Paper } from '@mui/material';
import { Analytics, ArrowBack } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { AnalyticsDashboard } from '../components/AnalyticsDashboard';
import { AnalyticsProvider } from '../contexts/AnalyticsContext';

export const AnalyticsDemo: React.FC = () => {
  const navigate = useNavigate();

  const handleGoBack = () => {
    navigate(-1);
  };

  return (
    <AnalyticsProvider>
      <Container maxWidth="xl" sx={{ py: 4 }}>
        {/* Header */}
        <Box mb={4}>
          <Button
            startIcon={<ArrowBack />}
            onClick={handleGoBack}
            sx={{ mb: 2 }}
          >
            Back to Dashboard
          </Button>
          
          <Box display="flex" alignItems="center" gap={2} mb={2}>
            <Analytics color="primary" sx={{ fontSize: 40 }} />
            <Typography variant="h3" component="h1" fontWeight="bold">
              Analytics Dashboard Demo
            </Typography>
          </Box>
          
          <Typography variant="h6" color="text.secondary" paragraph>
            Comprehensive productivity analytics and insights for FocusHive
          </Typography>
        </Box>

        {/* Demo Information */}
        <Grid container spacing={3} sx={{ mb: 4 }}>
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 3, height: '100%' }}>
              <Typography variant="h6" gutterBottom color="primary">
                📊 Real-time Analytics
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Track your productivity metrics, focus time, session completion rates, 
                and goal progress with interactive charts and visualizations.
              </Typography>
            </Paper>
          </Grid>
          
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 3, height: '100%' }}>
              <Typography variant="h6" gutterBottom color="primary">
                🔥 Activity Heatmaps
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Visualize your daily activity patterns with GitHub-style heatmaps. 
                Identify your most productive days and maintain consistency.
              </Typography>
            </Paper>
          </Grid>
          
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 3, height: '100%' }}>
              <Typography variant="h6" gutterBottom color="primary">
                👥 Team Insights
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Analyze hive member engagement, collaboration patterns, and 
                comparative productivity metrics for better team coordination.
              </Typography>
            </Paper>
          </Grid>
        </Grid>

        {/* Main Dashboard */}
        <Paper sx={{ p: 3 }}>
          <AnalyticsDashboard
            userId="demo-user"
            compactMode={false}
          />
        </Paper>

        {/* Feature Highlights */}
        <Box mt={6}>
          <Typography variant="h5" gutterBottom fontWeight="bold">
            Key Features
          </Typography>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Box>
                <Typography variant="h6" gutterBottom>
                  ✨ Interactive Charts
                </Typography>
                <Typography variant="body2" color="text.secondary" paragraph>
                  Built with MUI X Charts for smooth interactions, tooltips, and responsive design.
                  Supports line charts, bar charts, heatmaps, and progress indicators.
                </Typography>
              </Box>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Box>
                <Typography variant="h6" gutterBottom>
                  🎯 Goal Tracking
                </Typography>
                <Typography variant="body2" color="text.secondary" paragraph>
                  Set and track personal and team goals with milestone progress, 
                  deadline management, and priority-based organization.
                </Typography>
              </Box>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Box>
                <Typography variant="h6" gutterBottom>
                  📈 Trend Analysis
                </Typography>
                <Typography variant="body2" color="text.secondary" paragraph>
                  Identify productivity patterns, track improvement over time, 
                  and get insights into your work habits and focus efficiency.
                </Typography>
              </Box>
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Box>
                <Typography variant="h6" gutterBottom>
                  📤 Data Export
                </Typography>
                <Typography variant="body2" color="text.secondary" paragraph>
                  Export your analytics data in multiple formats (CSV, JSON, PDF, PNG) 
                  with customizable date ranges and content sections.
                </Typography>
              </Box>
            </Grid>
          </Grid>
        </Box>

        {/* Technical Information */}
        <Box mt={6}>
          <Typography variant="h5" gutterBottom fontWeight="bold">
            Technical Implementation
          </Typography>
          <Paper sx={{ p: 3 }}>
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <Typography variant="h6" gutterBottom>
                  🛠️ Technology Stack
                </Typography>
                <ul>
                  <li>React 18 with TypeScript</li>
                  <li>Material UI (MUI) for components</li>
                  <li>MUI X Charts for data visualization</li>
                  <li>Context API for state management</li>
                  <li>Date-fns for date manipulation</li>
                  <li>Comprehensive test coverage with Vitest</li>
                </ul>
              </Grid>
              
              <Grid item xs={12} md={6}>
                <Typography variant="h6" gutterBottom>
                  🎨 Design Features
                </Typography>
                <ul>
                  <li>Responsive design for all screen sizes</li>
                  <li>Dark/light theme support</li>
                  <li>Accessible components with ARIA labels</li>
                  <li>Smooth animations and transitions</li>
                  <li>Consistent Material Design principles</li>
                  <li>Mobile-first approach</li>
                </ul>
              </Grid>
            </Grid>
          </Paper>
        </Box>
      </Container>
    </AnalyticsProvider>
  );
};