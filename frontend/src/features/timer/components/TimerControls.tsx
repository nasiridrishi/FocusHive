import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  ToggleButton,
  ToggleButtonGroup,
  IconButton,
  Switch,
  FormControlLabel,
  Stack,
  Divider,
  Tooltip,
  Chip,
} from '@mui/material';
import {
  VolumeUp,
  VolumeOff,
  Timer,
  Coffee,
  FreeBreakfast,
  Repeat,
} from '@mui/icons-material';
import { useTheme } from '@mui/material/styles';
import { useTimer } from '../contexts/TimerContext';

interface TimerControlsProps {
  compact?: boolean;
  onDurationChange?: (duration: number) => void;
  onSoundToggle?: () => void;
  onAutoStartToggle?: () => void;
  showBreakSettings?: boolean;
}

export const TimerControls: React.FC<TimerControlsProps> = ({
  compact = false,
  onDurationChange,
  onSoundToggle,
  onAutoStartToggle,
  showBreakSettings = true,
}) => {
  const theme = useTheme();
  const { timerSettings, updateSettings } = useTimer();

  // Local state for duration selection
  const [selectedDuration, setSelectedDuration] = useState(timerSettings.focusLength);

  const handleDurationChange = (
    event: React.MouseEvent<HTMLElement>,
    newDuration: number | null
  ) => {
    if (newDuration !== null) {
      setSelectedDuration(newDuration);
      updateSettings({ focusLength: newDuration });
      if (onDurationChange) {
        onDurationChange(newDuration);
      }
    }
  };

  const handleSoundToggle = () => {
    updateSettings({ soundEnabled: !timerSettings.soundEnabled });
    if (onSoundToggle) {
      onSoundToggle();
    }
  };

  const handleAutoStartToggle = (event: React.ChangeEvent<HTMLInputElement>) => {
    updateSettings({ autoStartBreaks: event.target.checked });
    if (onAutoStartToggle) {
      onAutoStartToggle();
    }
  };

  const containerClass = compact ? 'timer-controls-compact' : 'timer-controls-full';
  const containerStyle = compact
    ? { flexDirection: 'column' as const }
    : { flexDirection: 'row' as const };

  return (
    <Box
      data-testid="timer-controls"
      className={containerClass}
      role="region"
      aria-label="Timer Settings"
      sx={{
        width: '100%',
        maxWidth: compact ? 320 : 600,
      }}
    >
      <Stack
        data-testid="controls-container"
        spacing={2}
        sx={containerStyle}
      >
        {/* Duration Selector */}
        <Box>
          <Typography
            variant="subtitle2"
            gutterBottom
            id="focus-duration-label"
          >
            <Timer fontSize="small" sx={{ verticalAlign: 'middle', mr: 1 }} />
            Focus Duration
          </Typography>
          <ToggleButtonGroup
            value={selectedDuration}
            exclusive
            onChange={handleDurationChange}
            aria-labelledby="focus-duration-label"
            aria-label="Focus duration"
            size={compact ? "small" : "medium"}
          >
            <ToggleButton
              value={15}
              data-testid="duration-15"
              aria-label="15 minutes"
            >
              15 min
            </ToggleButton>
            <ToggleButton
              value={25}
              data-testid="duration-25"
              aria-label="25 minutes"
              aria-pressed={selectedDuration === 25}
            >
              25 min
            </ToggleButton>
            <ToggleButton
              value={45}
              data-testid="duration-45"
              aria-label="45 minutes"
              aria-pressed={selectedDuration === 45}
            >
              45 min
            </ToggleButton>
            <ToggleButton
              value={60}
              data-testid="duration-60"
              aria-label="60 minutes"
            >
              60 min
            </ToggleButton>
          </ToggleButtonGroup>
        </Box>

        {/* Sound Toggle */}
        <Box>
          <Tooltip title={timerSettings.soundEnabled ? 'Disable sound' : 'Enable sound'}>
            <IconButton
              data-testid="sound-toggle"
              aria-label="Toggle sound"
              onClick={handleSoundToggle}
              color={timerSettings.soundEnabled ? 'primary' : 'default'}
              size={compact ? "small" : "medium"}
            >
              {timerSettings.soundEnabled ? (
                <VolumeUp data-testid="VolumeUpIcon" />
              ) : (
                <VolumeOff data-testid="VolumeOffIcon" />
              )}
            </IconButton>
          </Tooltip>
        </Box>

        {/* Auto-Start Breaks Toggle */}
        <Box>
          <FormControlLabel
            control={
              <Switch
                data-testid="auto-start-breaks-toggle"
                checked={timerSettings.autoStartBreaks}
                onChange={handleAutoStartToggle}
                aria-label="Toggle auto-start breaks"
                size={compact ? "small" : "medium"}
              />
            }
            label={
              <Typography variant="body2">
                Auto-start breaks
              </Typography>
            }
            labelPlacement="end"
          />
        </Box>
      </Stack>

      {/* Break Settings */}
      {showBreakSettings && (
        <>
          <Divider sx={{ my: 2 }} />
          <Stack
            direction={compact ? 'column' : 'row'}
            spacing={2}
            alignItems="center"
          >
            <Box>
              <Chip
                icon={<Coffee />}
                label="Short break: 5 min"
                variant="outlined"
                size="small"
                color="success"
              />
            </Box>
            <Box>
              <Chip
                icon={<FreeBreakfast />}
                label="Long break: 15 min"
                variant="outlined"
                size="small"
                color="info"
              />
            </Box>
            <Box>
              <Chip
                icon={<Repeat />}
                label="Long break after: 4 pomodoros"
                variant="outlined"
                size="small"
                color="warning"
              />
            </Box>
          </Stack>
        </>
      )}
    </Box>
  );
};