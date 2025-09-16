import React from 'react';
import {
  Box,
  Container,
  Typography,
  Paper,
  CircularProgress,
  Alert,
  Button,
  Stack,
  Card,
  CardContent,
  Chip,
  Divider
} from '@mui/material';
import {
  PersonOutline,
  Add,
  Settings,
  CheckCircle
} from '@mui/icons-material';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { PersonaSwitcher } from '../components/PersonaSwitcher';
import { getCurrentPersona, getPersonas } from '../services/identityService';
import { useAuth } from '../../auth/hooks/useAuth';

const PersonaSettingsPage: React.FC = () => {
  const { authState } = useAuth();
  const { user } = authState;

  const { data: currentPersona, isLoading: isLoadingCurrent, error: currentError } = useQuery({
    queryKey: ['currentPersona'],
    queryFn: getCurrentPersona,
    enabled: !!user
  });

  const { data: personas = [], isLoading: isLoadingPersonas } = useQuery({
    queryKey: ['personas'],
    queryFn: getPersonas,
    enabled: !!user
  });

  const isLoading = isLoadingCurrent || isLoadingPersonas;

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (currentError) {
    return (
      <Box sx={{ py: 2 }}>
        <Alert severity="error">Error loading persona settings</Alert>
      </Box>
    );
  }

  return (
    <Container maxWidth={'lg' as any} component="main">
      <Box sx={{ py: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Identity & Personas
        </Typography>
        <Typography variant="body1" color="text.secondary" gutterBottom>
          Switch between different personas for work, study, or personal contexts
        </Typography>

        <Stack spacing={3} sx={{ mt: 4 }}>
          {/* Current Persona Section */}
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Current Persona
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
              <PersonOutline fontSize="large" color="primary" />
              <Box>
                <Typography variant="h5">
                  {currentPersona?.name || 'Default'}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {currentPersona?.type || 'personal'}
                </Typography>
              </Box>
              {currentPersona?.isActive && (
                <Chip
                  icon={<CheckCircle />}
                  label="Active"
                  color="success"
                  size="small"
                />
              )}
            </Box>

            {/* Persona Switcher */}
            <Box sx={{ mt: 2 }}>
              <PersonaSwitcher />
            </Box>
          </Paper>

          {/* Quick Actions */}
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Quick Actions
            </Typography>
            <Stack direction="row" spacing={2}>
              <Button
                variant="outlined"
                startIcon={<Add />}
                component={Link}
                to="/settings/personas/manage"
              >
                Create New Persona
              </Button>
              <Button
                variant="outlined"
                startIcon={<Settings />}
                disabled={!currentPersona}
              >
                Set Default Persona
              </Button>
            </Stack>
          </Paper>

          {/* Persona Statistics */}
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Persona Statistics
            </Typography>
            <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 2 }}>
              <Card variant="outlined">
                <CardContent>
                  <Typography color="text.secondary" gutterBottom>
                    Total Personas
                  </Typography>
                  <Typography variant="h4">
                    {personas.length}
                  </Typography>
                </CardContent>
              </Card>
              <Card variant="outlined">
                <CardContent>
                  <Typography color="text.secondary" gutterBottom>
                    Active Persona
                  </Typography>
                  <Typography variant="h5">
                    {currentPersona?.name || 'None'}
                  </Typography>
                </CardContent>
              </Card>
            </Box>
          </Paper>

          {/* Navigation */}
          <Divider />
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="body2" color="text.secondary">
              Manage your personas to switch between different contexts seamlessly
            </Typography>
            <Button
              component={Link}
              to="/settings/personas/manage"
              variant="contained"
            >
              Manage Personas
            </Button>
          </Box>
        </Stack>
      </Box>
    </Container>
  );
};

export default PersonaSettingsPage;
export { PersonaSettingsPage };