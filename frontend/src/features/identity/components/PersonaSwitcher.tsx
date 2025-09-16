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
  Check
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getPersonas, switchPersona } from '../services/identityService';
import type { Persona, PersonaType } from '../types';

interface PersonaSwitcherProps {
  currentPersona?: Persona | null;
  onSwitch?: (persona: Persona) => void;
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
  onSwitch 
}) => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const queryClient = useQueryClient();
  const open = Boolean(anchorEl);

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
        variant="outlined"
        sx={{ 
          minWidth: 150,
          textTransform: 'none'
        }}
      >
        {currentPersona?.name || 'Select Persona'}
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
        <Box sx={{ px: 2, py: 1 }}>
          <Typography variant="caption" color="text.secondary">
            SWITCH PERSONA
          </Typography>
        </Box>
        <Divider />

        {isLoading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
            <CircularProgress size={24} role="progressbar" />
          </Box>
        )}

        {error && (
          <MenuItem disabled>
            <Alert severity="error" sx={{ width: '100%' }}>
              Error loading personas
            </Alert>
          </MenuItem>
        )}

        {!isLoading && !error && personas.map((persona) => (
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
        ))}

        {!isLoading && !error && personas.length === 0 && (
          <MenuItem disabled>
            <Typography variant="body2" color="text.secondary">
              No personas available
            </Typography>
          </MenuItem>
        )}
      </Menu>
    </>
  );
};