import React, {useCallback, useEffect, useMemo, useState} from 'react'
import {
  alpha,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Collapse,
  Fade,
  Grid,
  IconButton,
  Slider,
  TextField,
  Tooltip,
  Typography,
  useTheme,
} from '@mui/material'
import {
  Add,
  Battery30,
  Battery60,
  BatteryChargingFull,
  Clear,
  Coffee,
  EmojiObjects as Target,
  ExpandLess,
  ExpandMore,
  FitnessCenter,
  Groups,
  Lightbulb,
  LocalFireDepartment,
  Palette,
  Refresh,
  SentimentDissatisfied,
  SentimentSatisfied,
  Spa,
  Star,
  Tune,
  Work,
} from '@mui/icons-material'
import {MoodOption, MoodSelectorProps, MoodState, TaskType} from '../../types/music'

// Icon mapping helper
const geticonComponent = (iconName: string): React.ComponentType<{sx: {fontSize: string}}> => {
  const iconMap = {
    'SentimentSatisfied': SentimentSatisfied,
    'LocalFireDepartment': LocalFireDepartment,
    'Target': Target,
    'Palette': Palette,
    'SentimentDissatisfied': SentimentDissatisfied,
    'Star': Star,
  };
  const iconComponent = iconMap[iconName as keyof typeof iconMap];
  return iconComponent || SentimentSatisfied;
};

// Predefined mood options
const moodOptions: MoodOption[] = [
  {
    id: 'happy',
    name: 'Happy',
    icon: 'SentimentSatisfied',
    color: '#FFD54F',
    energy: 75,
    description: 'Upbeat and positive vibes',
  },
  {
    id: 'calm',
    name: 'Calm',
    icon: '😌',
    color: '#81C784',
    energy: 30,
    description: 'Peaceful and serene',
  },
  {
    id: 'energetic',
    name: 'Energetic',
    icon: 'LocalFireDepartment',
    color: '#FF7043',
    energy: 90,
    description: 'High energy and motivation',
  },
  {
    id: 'focused',
    name: 'Focused',
    icon: 'Target',
    color: '#42A5F5',
    energy: 60,
    description: 'Deep concentration mode',
  },
  {
    id: 'creative',
    name: 'Creative',
    icon: 'Palette',
    color: '#BA68C8',
    energy: 70,
    description: 'Inspiration and imagination',
  },
  {
    id: 'melancholic',
    name: 'Melancholic',
    icon: 'SentimentDissatisfied',
    color: '#78909C',
    energy: 25,
    description: 'Reflective and contemplative',
  },
  {
    id: 'adventurous',
    name: 'Adventurous',
    icon: 'Star',
    color: '#FFA726',
    energy: 85,
    description: 'Ready for exploration',
  },
  {
    id: 'romantic',
    name: 'Romantic',
    icon: '💕',
    color: '#F48FB1',
    energy: 45,
    description: 'Love and affection',
  },
]

const taskTypeOptions: {
  value: TaskType;
  label: string;
  icon: React.ReactElement;
  color: string
}[] = [
  {
    value: 'focus',
    label: 'Deep Work',
    icon: <Work/>,
    color: '#1976D2',
  },
  {
    value: 'creative',
    label: 'Creative Work',
    icon: <Lightbulb/>,
    color: '#7B1FA2',
  },
  {
    value: 'break',
    label: 'Break Time',
    icon: <Coffee/>,
    color: '#388E3C',
  },
  {
    value: 'exercise',
    label: 'Exercise',
    icon: <FitnessCenter/>,
    color: '#F57C00',
  },
  {
    value: 'relax',
    label: 'Relaxation',
    icon: <Spa/>,
    color: '#5E35B1',
  },
  {
    value: 'social',
    label: 'Social Time',
    icon: <Groups/>,
    color: '#D32F2F',
  },
]

const MoodSelector: React.FC<MoodSelectorProps> = ({
                                                     onMoodChange,
                                                     currentMood,
                                                     showEnergySlider = true,
                                                     showTaskTypeSelector = true,
                                                     showCustomTags = true,
                                                   }) => {
  const theme = useTheme()

  const [selectedMoodId, setSelectedMoodId] = useState<string | null>(
      currentMood ? moodOptions.find(m => m.name.toLowerCase() === currentMood.mood.toLowerCase())?.id || null : null
  )
  const [energy, setEnergy] = useState<number>(currentMood?.energy || 50)
  const [taskType, setTaskType] = useState<TaskType>(currentMood?.taskType || 'focus')
  const [customTags, setCustomTags] = useState<string[]>(currentMood?.customTags || [])
  const [newTag, setNewTag] = useState('')
  const [isAdvancedOpen, setIsAdvancedOpen] = useState(false)
  const [hoveredMood, setHoveredMood] = useState<string | null>(null)

  // Update local state when currentMood prop changes
  useEffect(() => {
    if (currentMood) {
      const moodOption = moodOptions.find(m => m.name.toLowerCase() === currentMood.mood.toLowerCase())
      setSelectedMoodId(moodOption?.id || null)
      setEnergy(currentMood.energy)
      setTaskType(currentMood.taskType)
      setCustomTags(currentMood.customTags)
    }
  }, [currentMood])

  // Create mood state and notify parent
  const notifyMoodChange = useCallback((
      moodId: string | null,
      energyLevel: number,
      task: TaskType,
      tags: string[]
  ) => {
    if (!moodId) return

    const selectedMood = moodOptions.find(m => m.id === moodId)
    if (!selectedMood) return

    const moodState: MoodState = {
      mood: selectedMood.name,
      energy: energyLevel,
      taskType: task,
      customTags: tags,
    }

    onMoodChange(moodState)
  }, [onMoodChange])

  // Handlers
  const handleMoodSelect = useCallback((moodId: string) => {
    const mood = moodOptions.find(m => m.id === moodId)
    if (!mood) return

    setSelectedMoodId(moodId)

    // Auto-adjust energy based on mood
    const newEnergy = mood.energy
    setEnergy(newEnergy)

    notifyMoodChange(moodId, newEnergy, taskType, customTags)
  }, [taskType, customTags, notifyMoodChange])

  const handleEnergyChange = useCallback((_: Event, newValue: number | number[]) => {
    const energyLevel = Array.isArray(newValue) ? newValue[0] : newValue
    setEnergy(energyLevel)

    if (selectedMoodId) {
      notifyMoodChange(selectedMoodId, energyLevel, taskType, customTags)
    }
  }, [selectedMoodId, taskType, customTags, notifyMoodChange])

  const handleTaskTypeChange = useCallback((newTaskType: TaskType) => {
    setTaskType(newTaskType)

    if (selectedMoodId) {
      notifyMoodChange(selectedMoodId, energy, newTaskType, customTags)
    }
  }, [selectedMoodId, energy, customTags, notifyMoodChange])

  const handleAddCustomTag = useCallback(() => {
    if (newTag.trim() && !customTags.includes(newTag.trim())) {
      const updatedTags = [...customTags, newTag.trim()]
      setCustomTags(updatedTags)
      setNewTag('')

      if (selectedMoodId) {
        notifyMoodChange(selectedMoodId, energy, taskType, updatedTags)
      }
    }
  }, [newTag, customTags, selectedMoodId, energy, taskType, notifyMoodChange])

  const handleRemoveCustomTag = useCallback((tagToRemove: string) => {
    const updatedTags = customTags.filter(tag => tag !== tagToRemove)
    setCustomTags(updatedTags)

    if (selectedMoodId) {
      notifyMoodChange(selectedMoodId, energy, taskType, updatedTags)
    }
  }, [customTags, selectedMoodId, energy, taskType, notifyMoodChange])

  const handleClearMood = useCallback(() => {
    setSelectedMoodId(null)
    setEnergy(50)
    setTaskType('focus')
    setCustomTags([])
    setNewTag('')
  }, [])

  const handleRandomMood = useCallback(() => {
    const randomMood = moodOptions[Math.floor(Math.random() * moodOptions.length)]
    handleMoodSelect(randomMood.id)
  }, [handleMoodSelect])

  // Get energy icon based on level
  const getEnergyIcon = useCallback((energyLevel: number) => {
    if (energyLevel >= 70) return <BatteryChargingFull/>
    if (energyLevel >= 40) return <Battery60/>
    return <Battery30/>
  }, [])

  // Get energy color
  const getEnergyColor = useCallback((energyLevel: number) => {
    if (energyLevel >= 70) return theme.palette.success.main
    if (energyLevel >= 40) return theme.palette.warning.main
    return theme.palette.error.main
  }, [theme])

  // Smart recommendations based on time of day
  const getSmartRecommendations = useMemo(() => {
    const hour = new Date().getHours()

    if (hour >= 6 && hour < 9) {
      return ['energetic', 'happy']
    } else if (hour >= 9 && hour < 12) {
      return ['focused', 'creative']
    } else if (hour >= 12 && hour < 14) {
      return ['calm', 'happy']
    } else if (hour >= 14 && hour < 17) {
      return ['focused', 'energetic']
    } else if (hour >= 17 && hour < 20) {
      return ['relaxed', 'creative']
    } else {
      return ['calm', 'romantic']
    }
  }, [])

  const selectedMoodOption = selectedMoodId ? moodOptions.find(m => m.id === selectedMoodId) : null

  return (
      <Card>
        <CardContent>
          {/* Header */}
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
            <Typography variant="h6" fontWeight="bold">
              How are you feeling?
            </Typography>
            <Box display="flex" gap={1}>
              <Tooltip title="Random mood">
                <IconButton size="small" onClick={handleRandomMood}>
                  <Refresh/>
                </IconButton>
              </Tooltip>
              <Tooltip title="Clear selection">
                <IconButton size="small" onClick={handleClearMood}>
                  <Clear/>
                </IconButton>
              </Tooltip>
              <Tooltip title={isAdvancedOpen ? 'Hide advanced options' : 'Show advanced options'}>
                <IconButton size="small" onClick={() => setIsAdvancedOpen(!isAdvancedOpen)}>
                  <Tune/>
                </IconButton>
              </Tooltip>
            </Box>
          </Box>

          {/* Current Selection Summary */}
          {selectedMoodOption && (
              <Fade in={!!selectedMoodOption}>
                <Box
                    mb={3}
                    p={2}
                    borderRadius={2}
                    sx={{
                      background: `linear-gradient(135deg, ${alpha(selectedMoodOption.color, 0.1)}, ${alpha(selectedMoodOption.color, 0.05)})`,
                      border: `1px solid ${alpha(selectedMoodOption.color, 0.2)}`,
                    }}
                >
                  <Box display="flex" alignItems="center" gap={2}>
                    <Box
                        sx={{
                          fontSize: '2rem',
                          backgroundColor: alpha(selectedMoodOption.color, 0.2),
                          borderRadius: '50%',
                          width: 64,
                          height: 64,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                    >
                      {selectedMoodOption.icon}
                    </Box>
                    <Box flex={1}>
                      <Typography variant="h6" fontWeight="bold" gutterBottom>
                        {selectedMoodOption.name}
                      </Typography>
                      <Typography variant="body2" color="text.secondary" gutterBottom>
                        {selectedMoodOption.description}
                      </Typography>
                      <Box display="flex" alignItems="center" gap={2}>
                        <Box display="flex" alignItems="center" gap={1}>
                          {getEnergyIcon(energy)}
                          <Typography variant="body2">
                            Energy: {energy}%
                          </Typography>
                        </Box>
                        <Box display="flex" alignItems="center" gap={1}>
                          {taskTypeOptions.find(t => t.value === taskType)?.icon}
                          <Typography variant="body2">
                            {taskTypeOptions.find(t => t.value === taskType)?.label}
                          </Typography>
                        </Box>
                      </Box>
                    </Box>
                  </Box>
                </Box>
              </Fade>
          )}

          {/* Smart Recommendations */}
          <Box mb={3}>
            <Typography variant="subtitle2" gutterBottom color="text.secondary">
              Recommended for now
            </Typography>
            <Box display="flex" gap={1} flexWrap="wrap">
              {moodOptions
              .filter(mood => getSmartRecommendations.includes(mood.id))
              .map((mood) => (
                  <Chip
                      key={`rec-${mood.id}`}
                      label={mood.name}
                      icon={React.createElement(geticonComponent(mood.icon), {sx: {fontSize: '1rem'}})}
                      onClick={() => handleMoodSelect(mood.id)}
                      variant={selectedMoodId === mood.id ? 'filled' : 'outlined'}
                      color={selectedMoodId === mood.id ? 'primary' : 'default'}
                      size="small"
                      sx={{
                        borderColor: selectedMoodId === mood.id ? undefined : alpha(mood.color, 0.5),
                        backgroundColor: selectedMoodId === mood.id ? undefined : alpha(mood.color, 0.1),
                        '&:hover': {
                          backgroundColor: alpha(mood.color, 0.2),
                        },
                      }}
                  />
              ))}
            </Box>
          </Box>

          {/* Mood Grid */}
          <Box mb={3}>
            <Typography variant="subtitle2" gutterBottom color="text.secondary">
              All moods
            </Typography>
            <Grid container spacing={1}>
              {moodOptions.map((mood) => (
                  <Grid item key={mood.id}
                        sx={{width: {xs: '50%', sm: '33.33%', md: '25%'}, p: 0.5}}>
                    <Card
                        sx={{
                          cursor: 'pointer',
                          transition: 'all 0.2s',
                          border: selectedMoodId === mood.id
                              ? `2px solid ${mood.color}`
                              : `2px solid ${alpha(mood.color, 0.2)}`,
                          backgroundColor: selectedMoodId === mood.id
                              ? alpha(mood.color, 0.1)
                              : 'transparent',
                          '&:hover': {
                            transform: 'translateY(-2px)',
                            boxShadow: theme.shadows[4],
                            backgroundColor: alpha(mood.color, 0.15),
                          },
                        }}
                        onClick={() => handleMoodSelect(mood.id)}
                        onMouseEnter={() => setHoveredMood(mood.id)}
                        onMouseLeave={() => setHoveredMood(null)}
                    >
                      <CardContent sx={{p: 2, textAlign: 'center', '&:last-child': {pb: 2}}}>
                        <Box
                            sx={{
                              fontSize: '2rem',
                              mb: 1,
                              transform: hoveredMood === mood.id ? 'scale(1.1)' : 'scale(1)',
                              transition: 'transform 0.2s',
                            }}
                        >
                          {React.createElement(geticonComponent(mood.icon), {sx: {fontSize: '1.5rem'}})}
                        </Box>
                        <Typography variant="body2" fontWeight="medium">
                          {mood.name}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {mood.energy}% energy
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
              ))}
            </Grid>
          </Box>

          {/* Advanced Options */}
          <Collapse in={isAdvancedOpen}>
            <Box>
              {/* Energy Slider */}
              {showEnergySlider && (
                  <Box mb={3}>
                    <Box display="flex" alignItems="center" justifyContent="space-between" mb={1}>
                      <Typography variant="subtitle2">
                        Energy Level
                      </Typography>
                      <Box display="flex" alignItems="center" gap={1}>
                        <Box color={getEnergyColor(energy)}>
                          {getEnergyIcon(energy)}
                        </Box>
                        <Typography variant="body2" fontWeight="bold">
                          {energy}%
                        </Typography>
                      </Box>
                    </Box>
                    <Slider
                        value={energy}
                        onChange={handleEnergyChange}
                        min={0}
                        max={100}
                        step={5}
                        marks={[
                          {value: 0, label: 'Low'},
                          {value: 50, label: 'Medium'},
                          {value: 100, label: 'High'},
                        ]}
                        sx={{
                          color: getEnergyColor(energy),
                          '& .MuiSlider-thumb': {
                            '&:hover, &.Mui-focusVisible': {
                              boxShadow: `0px 0px 0px 8px ${alpha(getEnergyColor(energy), 0.16)}`,
                            },
                          },
                        }}
                    />
                  </Box>
              )}

              {/* Task Type Selector */}
              {showTaskTypeSelector && (
                  <Box mb={3}>
                    <Typography variant="subtitle2" gutterBottom>
                      What are you doing?
                    </Typography>
                    <Box display="flex" gap={1} flexWrap="wrap">
                      {taskTypeOptions.map((option) => (
                          <Chip
                              key={option.value}
                              label={option.label}
                              icon={option.icon}
                              onClick={() => handleTaskTypeChange(option.value)}
                              variant={taskType === option.value ? 'filled' : 'outlined'}
                              color={taskType === option.value ? 'primary' : 'default'}
                              sx={{
                                borderColor: taskType === option.value ? undefined : alpha(option.color, 0.5),
                                '& .MuiChip-icon': {
                                  color: taskType === option.value ? undefined : option.color,
                                },
                              }}
                          />
                      ))}
                    </Box>
                  </Box>
              )}

              {/* Custom Tags */}
              {showCustomTags && (
                  <Box>
                    <Typography variant="subtitle2" gutterBottom>
                      Custom Tags
                    </Typography>
                    <Box display="flex" gap={1} alignItems="center" mb={1}>
                      <TextField
                          size="small"
                          placeholder="Add a tag..."
                          value={newTag}
                          onChange={(e) => setNewTag(e.target.value)}
                          onKeyPress={(e) => {
                            if (e.key === 'Enter') {
                              handleAddCustomTag()
                            }
                          }}
                      />
                      <Button
                          size="small"
                          variant="outlined"
                          startIcon={<Add/>}
                          onClick={handleAddCustomTag}
                          disabled={!newTag.trim() || customTags.includes(newTag.trim())}
                      >
                        Add
                      </Button>
                    </Box>
                    <Box display="flex" gap={1} flexWrap="wrap">
                      {customTags.map((tag) => (
                          <Chip
                              key={tag}
                              label={tag}
                              size="small"
                              onDelete={() => handleRemoveCustomTag(tag)}
                              deleteIcon={<Clear/>}
                          />
                      ))}
                    </Box>
                  </Box>
              )}
            </Box>
          </Collapse>

          {/* Toggle Advanced Options */}
          <Box display="flex" justifyContent="center" mt={2}>
            <Button
                size="small"
                onClick={() => setIsAdvancedOpen(!isAdvancedOpen)}
                endIcon={isAdvancedOpen ? <ExpandLess/> : <ExpandMore/>}
            >
              {isAdvancedOpen ? 'Hide' : 'Show'} Advanced Options
            </Button>
          </Box>
        </CardContent>
      </Card>
  )
}

export default MoodSelector