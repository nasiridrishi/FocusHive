import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  IconButton,
  Chip,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Stack,
  Tooltip
} from '@mui/material';
import {
  Add,
  Edit,
  Delete,
  Person,
  Work,
  School,
  SportsEsports,
  Brush
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getPersonas,
  createPersona,
  updatePersona,
  deletePersona
} from '../services/identityService';
import type { Persona, PersonaType, PersonaCreateRequest } from '../types';

interface PersonaDialogProps {
  open: boolean;
  onClose: () => void;
  persona?: Persona | null;
  onSave: (data: PersonaCreateRequest | Partial<PersonaCreateRequest>) => void;
}

const getPersonaIcon = (type: PersonaType) => {
  switch (type) {
    case 'work':
      return <Work />;
    case 'study':
      return <School />;
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

const PersonaDialog: React.FC<PersonaDialogProps> = ({
  open,
  onClose,
  persona,
  onSave
}) => {
  const [name, setName] = useState('');
  const [type, setType] = useState<PersonaType>('personal');
  const isEdit = !!persona;

  React.useEffect(() => {
    if (persona) {
      setName(persona.name);
      setType(persona.type);
    } else {
      setName('');
      setType('personal');
    }
  }, [persona]);

  const handleSave = () => {
    if (isEdit) {
      onSave({ name });
    } else {
      onSave({
        name,
        type,
        isDefault: false,
        settings: {}
      });
    }
    handleClose();
  };

  const handleClose = () => {
    setName('');
    setType('personal');
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth={'sm' as any} fullWidth>
      <DialogTitle>
        {isEdit ? 'Edit Persona' : 'Create New Persona'}
      </DialogTitle>
      <DialogContent>
        <Stack spacing={3} sx={{ mt: 1 }}>
          <TextField
            label="Persona Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            fullWidth
            required
            autoFocus
          />
          {!isEdit && (
            <FormControl fullWidth>
              <InputLabel id="persona-type-label">Persona Type</InputLabel>
              <Select
                labelId="persona-type-label"
                id="persona-type-select"
                value={type}
                label="Persona Type"
                onChange={(e) => setType(e.target.value as PersonaType)}
              >
                <MenuItem value="personal">Personal</MenuItem>
                <MenuItem value="work">Work</MenuItem>
                <MenuItem value="study">Study</MenuItem>
                <MenuItem value="gaming">Gaming</MenuItem>
                <MenuItem value="creative">Creative</MenuItem>
              </Select>
            </FormControl>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>Cancel</Button>
        <Button
          onClick={handleSave}
          variant="contained"
          disabled={!name.trim()}
        >
          {isEdit ? 'Save' : 'Create'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

interface DeleteConfirmDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  personaName: string;
}

const DeleteConfirmDialog: React.FC<DeleteConfirmDialogProps> = ({
  open,
  onClose,
  onConfirm,
  personaName
}) => {
  return (
    <Dialog open={open} onClose={onClose} maxWidth={'sm' as any}>
      <DialogTitle>Delete Persona</DialogTitle>
      <DialogContent>
        <Typography>
          Are you sure you want to delete the persona "{personaName}"?
          This action cannot be undone.
        </Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={onConfirm} color="error" variant="contained">
          Confirm
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export const PersonaManagement: React.FC = () => {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedPersona, setSelectedPersona] = useState<Persona | null>(null);
  const [personaToDelete, setPersonaToDelete] = useState<Persona | null>(null);
  const queryClient = useQueryClient();

  const { data: personas = [], isLoading, error } = useQuery({
    queryKey: ['personas'],
    queryFn: getPersonas,
  });

  const createMutation = useMutation({
    mutationFn: createPersona,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['personas'] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<PersonaCreateRequest> }) =>
      updatePersona(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['personas'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deletePersona,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['personas'] });
    },
  });

  const handleAdd = () => {
    setSelectedPersona(null);
    setDialogOpen(true);
  };

  const handleEdit = (persona: Persona) => {
    setSelectedPersona(persona);
    setDialogOpen(true);
  };

  const handleDelete = (persona: Persona) => {
    setPersonaToDelete(persona);
    setDeleteDialogOpen(true);
  };

  const handleSave = (data: PersonaCreateRequest | Partial<PersonaCreateRequest>) => {
    if (selectedPersona) {
      updateMutation.mutate({ id: selectedPersona.id, data });
    } else {
      createMutation.mutate(data as PersonaCreateRequest);
    }
    setDialogOpen(false);
  };

  const handleConfirmDelete = () => {
    if (personaToDelete) {
      deleteMutation.mutate(personaToDelete.id);
    }
    setDeleteDialogOpen(false);
    setPersonaToDelete(null);
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress role="progressbar" />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ py: 2 }}>
        <Alert severity="error">Error loading personas</Alert>
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4" component="h1">
          Manage Personas
        </Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={handleAdd}
        >
          Add Persona
        </Button>
      </Box>

      <Box sx={{
        display: 'grid',
        gridTemplateColumns: {
          xs: '1fr',
          sm: 'repeat(2, 1fr)',
          md: 'repeat(3, 1fr)'
        },
        gap: 3
      }}>
        {personas.map((persona) => (
          <Card key={persona.id} data-testid="persona-card">
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                {getPersonaIcon(persona.type)}
                <Typography variant="h6" sx={{ ml: 1 }}>
                  {persona.name}
                </Typography>
              </Box>

              <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
                <Chip
                  label={persona.type}
                  size="small"
                  color="primary"
                  variant="outlined"
                />
                {persona.isActive && (
                  <Chip
                    label="Active"
                    size="small"
                    color="success"
                  />
                )}
                {persona.isDefault && (
                  <Chip
                    label="Default"
                    size="small"
                    color="info"
                  />
                )}
              </Stack>

              <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Tooltip title="Edit persona">
                  <IconButton
                    aria-label="Edit persona"
                    onClick={() => handleEdit(persona)}
                    size="small"
                  >
                    <Edit />
                  </IconButton>
                </Tooltip>
                <Tooltip
                  title={
                    persona.isActive
                      ? 'Cannot delete active persona'
                      : persona.isDefault
                      ? 'Cannot delete default persona'
                      : 'Delete persona'
                  }
                >
                  <span>
                    <IconButton
                      aria-label="Delete persona"
                      onClick={() => handleDelete(persona)}
                      disabled={persona.isActive || persona.isDefault}
                      size="small"
                      color="error"
                    >
                      <Delete />
                    </IconButton>
                  </span>
                </Tooltip>
              </Box>
            </CardContent>
          </Card>
        ))}
      </Box>

      <PersonaDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        persona={selectedPersona}
        onSave={handleSave}
      />

      {personaToDelete && (
        <DeleteConfirmDialog
          open={deleteDialogOpen}
          onClose={() => setDeleteDialogOpen(false)}
          onConfirm={handleConfirmDelete}
          personaName={personaToDelete.name}
        />
      )}
    </Box>
  );
};