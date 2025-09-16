import React from 'react';
import {
  Box,
  Container,
  Typography,
  Button,
  CircularProgress,
  Stack,
  IconButton
} from '@mui/material';
import { ArrowBack } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { PersonaManagement } from '../components/PersonaManagement';
import { getPersonas } from '../services/identityService';
import { useAuth } from '../../auth/hooks/useAuth';

const PersonaManagementPage: React.FC = () => {
  const navigate = useNavigate();
  const { authState } = useAuth();
  const { user } = authState;

  const { isLoading } = useQuery({
    queryKey: ['personas'],
    queryFn: getPersonas,
    enabled: !!user
  });

  const handleBack = () => {
    navigate('/settings/personas');
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Container maxWidth={'lg' as any} component="main">
      <Box sx={{ py: 4 }}>
        {/* Header with back button */}
        <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 3 }}>
          <IconButton onClick={handleBack} edge="start">
            <ArrowBack />
          </IconButton>
          <Box sx={{ flex: 1 }}>
            <Typography variant="h4" component="h1">
              Persona Management
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Manage your different personas for various contexts
            </Typography>
          </Box>
        </Stack>

        {/* Back button as a secondary option */}
        <Button
          variant="text"
          startIcon={<ArrowBack />}
          onClick={handleBack}
          sx={{ mb: 3 }}
        >
          Back to Settings
        </Button>

        {/* Persona Management Component */}
        <PersonaManagement />
      </Box>
    </Container>
  );
};

export default PersonaManagementPage;
export { PersonaManagementPage };