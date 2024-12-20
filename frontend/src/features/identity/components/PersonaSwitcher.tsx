import React, { useState } from 'react';
import {
  Button,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  CircularProgress,
  Alert,
  Divider,
  Typography,
  Box,
  Chip
} from '@mui/material';
import {
  Person,
  Work,
  School,
  SportsEsports,
  Brush,
  ExpandMore,
  Check,
  Add
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getPersonas, switchPersona } from '../services/identityService';
import type { Persona, PersonaType } from '../types';

interface PersonaSwitcherProps {
  currentPersona?: Persona | null;
  onSwitch?: (persona: Persona) => void;
  onCreateNew?: () => void;
  variant?: 'default' | 'compact';
}

const getPersonaIcon = (type: PersonaType) => {
  switch (type) {
    case 'work':
      return <Work data-testid="work-icon" />;
    case 'study':
      return <School data-testid="study-icon" />;
    case 'personal':
      return <Person />;
    case 'gaming':
      return <SportsEsports />;
    case 'creative':
      return <Brush />;
    default:
      return <Person />;
  }
};

export const PersonaSwitcher: React.FC<PersonaSwitcherProps> = ({ 
  currentPersona: propPersona, 
  onSwitch,
  onCreateNew,
  variant = 'default'
}) => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const queryClient = useQueryClient();
  const open = Boolean(anchorEl);
  
  // Debug: Check if onCreateNew callback is passed
  console.log('PersonaSwitcher - onCreateNew prop:', onCreateNew ? 'exists' : 'missing');

  const { data: personas = [], isLoading, error } = useQuery({
    queryKey: ['personas'],
    queryFn: getPersonas,
    enabled: open,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  const switchMutation = useMutation({
    mutationFn: switchPersona,
    onSuccess: (persona) => {
      queryClient.invalidateQueries({ queryKey: ['personas'] });
      if (onSwitch) {
        onSwitch(persona);
      }
      handleClose();
    },
  });

  const currentPersona = propPersona || personas.find(p => p.isActive);

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleSwitchPersona = (persona: Persona) => {
    if (persona.id !== currentPersona?.id) {
      switchMutation.mutate(persona.id);
    }
  };

  return (
    <>
      <Button
        aria-label="Switch persona"
        aria-controls={open ? 'persona-menu' : undefined}
        aria-haspopup="true"
        aria-expanded={open ? 'true' : undefined}
        onClick={handleClick}
        startIcon={currentPersona ? getPersonaIcon(currentPersona.type) : <Person />}
        endIcon={<ExpandMore />}
        variant={variant === 'compact' ? 'text' : 'outlined'}
        size={variant === 'compact' ? 'small' : 'medium'}
        sx={{ 
          minWidth: variant === 'compact' ? 'auto' : 150,
          textTransform: 'none',
          ...(variant === 'compact' && {
            color: 'text.secondary',
            fontSize: '0.875rem',
            '&:hover': {
              backgroundColor: 'action.hover'
            }
          })
        }}
      >
        {variant === 'compact' ? (currentPersona?.name || 'Select') : (currentPersona?.name || 'Select Persona')}
      </Button>

      <Menu
        id="persona-menu"
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        role="menu"
        PaperProps={{
          elevation: 3,
          sx: { minWidth: 250, mt: 1 }
        }}
      >
        {[
          <Box key="header" sx={{ px: 2, py: 1 }}>
            <Typography variant="caption" color="text.secondary">
              SWITCH PERSONA
            </Typography>
          </Box>,
          <Divider key="header-divider" />,
          
          ...(isLoading ? [
            <Box key="loading" sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
              <CircularProgress size={24} role="progressbar" />
            </Box>
          ] : []),

          ...(error ? [
            <MenuItem key="error" disabled>
              <Alert severity="error" sx={{ width: '100%' }}>
                Error loading personas
              </Alert>
            </MenuItem>
          ] : []),

          ...(!isLoading && !error ? personas.map((persona) => (
            <MenuItem
              key={persona.id}
              onClick={() => handleSwitchPersona(persona)}
              role="menuitem"
              aria-current={persona.isActive ? 'true' : undefined}
              selected={persona.isActive}
            >
              <ListItemIcon>
                {getPersonaIcon(persona.type)}
              </ListItemIcon>
              <ListItemText
                primary={persona.name}
                secondary={`${persona.isDefault ? 'Default • ' : ''}${persona.type}`}
                secondaryTypographyProps={{
                  variant: 'caption',
                  color: 'text.secondary'
                }}
              />
              {persona.isActive && (
                <ListItemIcon>
                  <Check color="primary" />
                </ListItemIcon>
              )}
            </MenuItem>
          )) : []),

          ...(!isLoading && !error && personas.length === 0 ? [
            <MenuItem key="empty" disabled>
              <Typography variant="body2" color="text.secondary">
                No personas available
              </Typography>
            </MenuItem>
          ] : []),
          
          ...(onCreateNew ? [
            <Divider key="divider" />,
            <Box key="create-button" sx={{ p: 1 }}>
              <Button
                fullWidth
                variant="outlined"
                startIcon={<Add />}
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  console.log('Create New Persona button clicked');
                  handleClose();
                  if (onCreateNew) {
                    console.log('Calling onCreateNew callback');
                    setTimeout(() => onCreateNew(), 100);
                  } else {
                    console.log('onCreateNew callback is not defined');
                  }
                }}
                sx={{
                  textTransform: 'none',
                  justifyContent: 'flex-start',
                  color: 'primary.main',
                  borderStyle: 'dashed'
                }}
              >
                Create New Persona
              </Button>
            </Box>
          ] : [])
        ]}
      </Menu>
    </>
  );
};