import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  TextField,
  Autocomplete,
  Grid,
  Chip,
  FormControl,
  FormLabel,
  RadioGroup,
  FormControlLabel,
  Radio,
  Select,
  MenuItem,
  InputLabel,
  Checkbox,
  FormGroup,
  Alert,
  Paper,
  IconButton,
  CircularProgress,
  useTheme,
  useMediaQuery,
  Card,
  CardContent,
  Avatar,
  Stack,
  Divider,
  Slider,
} from '@mui/material';
import {
  Close as CloseIcon,
  LocationOn,
  School,
  Work,
  Code,
  Brush,
  MusicNote,
  FitnessCenter,
  MenuBook,
  Language,
  Psychology,
  Business,
  HealthAndSafety,
  SportsEsports,
  Refresh as RefreshIcon,
} from '@mui/icons-material';

interface TimeSlot {
  day: string;
  hour: number;
}

interface BuddyPreferences {
  timezone: string;
  availability: TimeSlot[];
  focusAreas: string[];
  goals: string[];
  preferredCommunication: string[];
  sessionFrequency: string;
  accountabilityLevel: string;
  studyHours: string[];
  languages: string[];
  commitmentHours: number;
  bio: string;
  experienceLevel: string;
  learningStyle: string;
  meetingFrequency: string;
}

interface BuddyMatch {
  id: string;
  username: string;
  avatar?: string;
  timezone: string;
  matchPercentage: number;
  commonFocusAreas: string[];
  commonAvailability: TimeSlot[];
  bio?: string;
  rating?: number;
  sessionsCompleted?: number;
}

export interface BuddyMatchingDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit?: (preferences: BuddyPreferences) => void;
  onMatch?: (match: BuddyMatch) => void;
  loading?: boolean;
  error?: string | null;
  matches?: BuddyMatch[];
}

const timeZones = [
  'UTC-12:00', 'UTC-11:00', 'UTC-10:00', 'UTC-09:00', 'UTC-08:00',
  'UTC-07:00', 'UTC-06:00', 'UTC-05:00', 'UTC-04:00', 'UTC-03:00',
  'UTC-02:00', 'UTC-01:00', 'UTC+00:00', 'UTC+01:00', 'UTC+02:00',
  'UTC+03:00', 'UTC+04:00', 'UTC+05:00', 'UTC+06:00', 'UTC+07:00',
  'UTC+08:00', 'UTC+09:00', 'UTC+10:00', 'UTC+11:00', 'UTC+12:00',
];

const STUDY_HOURS = [
  { value: 'morning', label: 'Morning (6AM - 12PM)' },
  { value: 'afternoon', label: 'Afternoon (12PM - 6PM)' },
  { value: 'evening', label: 'Evening (6PM - 12AM)' },
  { value: 'night', label: 'Night (12AM - 6AM)' },
];

const focusAreaIcons: Record<string, React.ReactElement> = {
  'Study': <School />,
  'Work': <Work />,
  'Coding': <Code />,
  'Design': <Brush />,
  'Music': <MusicNote />,
  'Fitness': <FitnessCenter />,
  'Reading': <MenuBook />,
  'Language Learning': <Language />,
  'Research': <Psychology />,
  'Business': <Business />,
  'Health': <HealthAndSafety />,
  'Gaming': <SportsEsports />,
};

const focusAreas = Object.keys(focusAreaIcons);
const DAYS_OF_WEEK = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

export const BuddyMatchingDialog: React.FC<BuddyMatchingDialogProps> = ({
  open,
  onClose,
  onSubmit,
  onMatch,
  loading = false,
  error = null,
  matches = [],
}) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('tablet'));

  const [preferences, setPreferences] = useState<BuddyPreferences>({
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC+00:00',
    availability: [],
    focusAreas: [],
    goals: [],
    preferredCommunication: ['chat'],
    sessionFrequency: 'weekly',
    accountabilityLevel: 'medium',
    studyHours: [],
    languages: ['English'],
    commitmentHours: 10,
    bio: '',
    experienceLevel: 'intermediate',
    learningStyle: '',
    meetingFrequency: 'weekly',
  });

  const [matchesFound, setMatchesFound] = useState(false);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  const handleSubmit = () => {
    const errors: Record<string, string> = {};

    if (preferences.focusAreas.length === 0) {
      errors.focusAreas = 'At least one focus area is required';
    }
    if (!preferences.timezone) {
      errors.timezone = 'Timezone is required';
    }
    if (preferences.availability.length === 0) {
      errors.availability = 'Select at least one available time slot';
    }

    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      return;
    }

    setValidationErrors({});
    onSubmit?.(preferences);
    setMatchesFound(true);
  };

  const handleReset = () => {
    setPreferences({
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC+00:00',
      availability: [],
      focusAreas: [],
      goals: [],
      preferredCommunication: ['chat'],
      sessionFrequency: 'weekly',
      accountabilityLevel: 'medium',
      studyHours: [],
      languages: ['English'],
      commitmentHours: 10,
      bio: '',
      experienceLevel: 'intermediate',
      learningStyle: '',
      meetingFrequency: 'weekly',
    });
    setValidationErrors({});
    setMatchesFound(false);
  };

  const renderContent = () => {
    if (loading) {
      return (
        <Box data-testid="loading-spinner" sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress />
        </Box>
      );
    }

    if (error) {
      return (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      );
    }

    if (matchesFound && matches.length > 0) {
      return (
        <Box data-testid="suggested-matches">
          <Alert severity="success" sx={{ mb: 2 }}>
            Matches found!
          </Alert>
          <Typography variant="h6" sx={{ mb: 2 }}>Suggested Matches</Typography>
          <Stack spacing={2}>
            {matches.map(match => (
              <Card key={match.id} data-testid={`match-${match.id}`}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <Avatar sx={{ mr: 2 }}>
                      {match.username[0].toUpperCase()}
                    </Avatar>
                    <Box sx={{ flexGrow: 1 }}>
                      <Typography variant="h6">
                        {match.username}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        <LocationOn fontSize="small" sx={{ verticalAlign: 'middle' }} />
                        {match.timezone}
                      </Typography>
                    </Box>
                    <Box sx={{ textAlign: 'center' }}>
                      <Typography variant="h4">
                        {match.matchPercentage}%
                      </Typography>
                      <Typography variant="caption">Match</Typography>
                    </Box>
                  </Box>

                  <Divider sx={{ my: 2 }} />

                  <Typography variant="body2" gutterBottom>
                    <strong>Common Focus Areas:</strong>
                  </Typography>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mb: 2 }}>
                    {match.commonFocusAreas.map(area => (
                      <Chip
                        key={area}
                        label={area}
                        size="small"
                        icon={focusAreaIcons[area]}
                      />
                    ))}
                  </Box>

                  <Typography variant="body2" gutterBottom>
                    <strong>Common Availability:</strong> {match.commonAvailability.length} overlapping slots
                  </Typography>

                  {match.bio && (
                    <Typography variant="body2" sx={{ mt: 1 }}>
                      {match.bio}
                    </Typography>
                  )}

                  <Box sx={{ mt: 2, display: 'flex', gap: 1 }}>
                    <Button
                      variant="contained"
                      fullWidth
                      onClick={() => onMatch?.(match)}
                      data-testid={`connect-buddy-${match.id}`}
                    >
                      Connect
                    </Button>
                  </Box>
                </CardContent>
              </Card>
            ))}
          </Stack>
        </Box>
      );
    }

    return (
      <Box data-testid="preferences-form">
        <Stack spacing={3}>
          {/* Focus Areas Input */}
          <TextField
            data-testid="focus-areas-input"
            label="Focus Areas"
            fullWidth
            value={preferences.focusAreas.join(', ')}
            onChange={(e) => setPreferences(prev => ({
              ...prev,
              focusAreas: e.target.value.split(',').map(s => s.trim()).filter(s => s),
            }))}
            placeholder="React, TypeScript, Node.js"
            error={!!validationErrors.focusAreas}
            helperText={validationErrors.focusAreas || 'Comma-separated list of topics'}
            inputProps={{ 'aria-label': 'Focus Areas' }}
          />

          {/* Time Zone */}
          <Box>
            <Autocomplete
              value={preferences.timezone}
              onChange={(_, value) => setPreferences(prev => ({ ...prev, timezone: value || '' }))}
              options={timeZones}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Timezone"
                  fullWidth
                  error={!!validationErrors.timezone}
                  helperText={validationErrors.timezone}
                />
              )}
              data-testid="timezone-selector"
            />
            <Button
              data-testid="detect-timezone-button"
              size="small"
              onClick={() => {
                const detected = Intl.DateTimeFormat().resolvedOptions().timeZone;
                setPreferences(prev => ({ ...prev, timezone: detected }));
              }}
              sx={{ mt: 1 }}
            >
              Detect My Timezone
            </Button>
          </Box>

          {/* Study Hours */}
          <Box>
            <Typography variant="body2" sx={{ mb: 1 }}>Study Hours</Typography>
            <FormGroup data-testid="study-hours-selector">
              {STUDY_HOURS.map((hour) => (
                <FormControlLabel
                  key={hour.value}
                  control={
                    <Checkbox
                      checked={preferences.studyHours.includes(hour.value)}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setPreferences(prev => ({ ...prev, studyHours: [...prev.studyHours, hour.value] }));
                        } else {
                          setPreferences(prev => ({ ...prev, studyHours: prev.studyHours.filter(h => h !== hour.value) }));
                        }
                      }}
                    />
                  }
                  label={hour.label}
                />
              ))}
            </FormGroup>
          </Box>

          {/* Communication Preferences */}
          <Box>
            <Typography variant="body2" sx={{ mb: 1 }}>Communication Preferences</Typography>
            <FormGroup row>
              {['Video Calls', 'Voice Calls', 'Text Chat', 'Screen Sharing', 'Async Messages'].map(method => (
                <FormControlLabel
                  key={method}
                  control={
                    <Checkbox
                      checked={preferences.preferredCommunication.includes(method)}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setPreferences(prev => ({
                            ...prev,
                            preferredCommunication: [...prev.preferredCommunication, method],
                          }));
                        } else {
                          setPreferences(prev => ({
                            ...prev,
                            preferredCommunication: prev.preferredCommunication.filter(m => m !== method),
                          }));
                        }
                      }}
                    />
                  }
                  label={method}
                />
              ))}
            </FormGroup>
          </Box>

          {/* Availability */}
          <Box>
            <Typography variant="body2" sx={{ mb: 1 }}>Availability</Typography>
            <FormGroup data-testid="availability-schedule">
              {DAYS_OF_WEEK.map((day) => (
                <FormControlLabel
                  key={day}
                  control={
                    <Checkbox
                      checked={preferences.availability.some(slot => slot.day === day)}
                      onChange={(e) => {
                        if (e.target.checked) {
                          // Add a default morning slot for the day
                          setPreferences(prev => ({
                            ...prev,
                            availability: [...prev.availability, { day, hour: 9 }],
                          }));
                        } else {
                          setPreferences(prev => ({
                            ...prev,
                            availability: prev.availability.filter(slot => slot.day !== day),
                          }));
                        }
                      }}
                      inputProps={{ 'aria-label': day }}
                    />
                  }
                  label={day}
                />
              ))}
            </FormGroup>
            {validationErrors.availability && (
              <Typography role="alert" color="error" variant="caption">
                {validationErrors.availability}
              </Typography>
            )}
          </Box>

          {/* Languages */}
          <TextField
            label="Preferred Languages"
            fullWidth
            value={preferences.languages.join(', ')}
            onChange={(e) => setPreferences(prev => ({
              ...prev,
              languages: e.target.value.split(',').map(s => s.trim()).filter(s => s),
            }))}
            placeholder="English, Spanish"
            inputProps={{ 'aria-label': 'Preferred Languages' }}
          />

          {/* Experience Level */}
          <FormControl fullWidth>
            <InputLabel>Experience Level</InputLabel>
            <Select
              value={preferences.experienceLevel}
              onChange={(e) => setPreferences(prev => ({ ...prev, experienceLevel: e.target.value }))}
              label="Experience Level"
              inputProps={{ 'aria-label': 'Experience Level' }}
            >
              <MenuItem value="beginner">Beginner</MenuItem>
              <MenuItem value="intermediate">Intermediate</MenuItem>
              <MenuItem value="advanced">Advanced</MenuItem>
              <MenuItem value="expert">Expert</MenuItem>
            </Select>
          </FormControl>

          {/* Commitment Hours */}
          <Box>
            <Typography variant="body2" sx={{ mb: 1 }}>
              Weekly Commitment: {preferences.commitmentHours} hours
            </Typography>
            <Slider
              data-testid="commitment-slider"
              value={preferences.commitmentHours}
              onChange={(_, value) => setPreferences(prev => ({ ...prev, commitmentHours: value as number }))}
              min={1}
              max={40}
              marks
              step={1}
              valueLabelDisplay="auto"
            />
          </Box>

          {/* About You */}
          <TextField
            label="About You"
            fullWidth
            multiline
            rows={3}
            value={preferences.bio}
            onChange={(e) => setPreferences(prev => ({ ...prev, bio: e.target.value.slice(0, 500) }))}
            helperText={`${preferences.bio.length}/500`}
            inputProps={{ 'aria-label': 'About You' }}
          />

          {/* Advanced Options */}
          <Box data-testid="advanced-options-toggle">
            <Typography variant="h6" gutterBottom>
              Advanced Preferences
            </Typography>
            <Box data-testid="advanced-preferences">
              <Stack spacing={2}>
                <TextField
                  label="Goals"
                  fullWidth
                  multiline
                  rows={2}
                  value={preferences.goals.join('\n')}
                  onChange={(e) => setPreferences(prev => ({
                    ...prev,
                    goals: e.target.value.split('\n').filter(g => g.trim()),
                  }))}
                  inputProps={{ 'aria-label': 'Goals' }}
                />

                <FormControl fullWidth>
                  <InputLabel>Learning Style</InputLabel>
                  <Select
                    value={preferences.learningStyle}
                    onChange={(e) => setPreferences(prev => ({ ...prev, learningStyle: e.target.value }))}
                    label="Learning Style"
                    inputProps={{ 'aria-label': 'Learning Style' }}
                  >
                    <MenuItem value="visual">Visual</MenuItem>
                    <MenuItem value="auditory">Auditory</MenuItem>
                    <MenuItem value="kinesthetic">Kinesthetic</MenuItem>
                    <MenuItem value="reading">Reading/Writing</MenuItem>
                  </Select>
                </FormControl>

                <FormControl fullWidth>
                  <InputLabel>Meeting Frequency</InputLabel>
                  <Select
                    value={preferences.meetingFrequency}
                    onChange={(e) => setPreferences(prev => ({ ...prev, meetingFrequency: e.target.value }))}
                    label="Meeting Frequency"
                    inputProps={{ 'aria-label': 'Meeting Frequency' }}
                  >
                    <MenuItem value="daily">Daily</MenuItem>
                    <MenuItem value="weekly">Weekly</MenuItem>
                    <MenuItem value="biweekly">Bi-weekly</MenuItem>
                    <MenuItem value="monthly">Monthly</MenuItem>
                  </Select>
                </FormControl>

                <Box>
                  <Typography variant="body2" sx={{ mb: 1 }}>Accountability Style</Typography>
                  <RadioGroup
                    value={preferences.accountabilityLevel}
                    onChange={(e) => setPreferences(prev => ({ ...prev, accountabilityLevel: e.target.value }))}
                  >
                    <FormControlLabel value="gentle" control={<Radio />} label="Gentle Encouragement" />
                    <FormControlLabel value="moderate" control={<Radio />} label="Moderate Follow-up" />
                    <FormControlLabel value="strict" control={<Radio />} label="Strict Accountability" />
                  </RadioGroup>
                </Box>
              </Stack>
            </Box>
          </Box>
        </Stack>
      </Box>
    );
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth={'laptop' as const}
      fullWidth
      fullScreen={isMobile}
      data-testid="buddy-matching-dialog"
      aria-labelledby="dialog-title"
    >
      <DialogTitle id="dialog-title" data-testid="dialog-title">
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Typography variant="h5">Find Your Study Buddy</Typography>
          <IconButton
            onClick={onClose}
            size="small"
            aria-label="Close dialog"
            data-testid="close-button"
          >
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>
      <DialogContent>
        {renderContent()}
      </DialogContent>
      <DialogActions>
        {!matchesFound && (
          <>
            <Button
              data-testid="reset-form-button"
              startIcon={<RefreshIcon />}
              onClick={handleReset}
            >
              Reset
            </Button>
            <Box sx={{ flexGrow: 1 }} />
          </>
        )}
        <Button data-testid="cancel-button" onClick={onClose}>
          {matchesFound ? 'Close' : 'Cancel'}
        </Button>
        {!matchesFound && (
          <Button
            data-testid="submit-preferences-button"
            variant="contained"
            onClick={handleSubmit}
            disabled={loading}
          >
            Find Matches
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};