import React, {useEffect, useRef, useState} from 'react'
import {
  Alert,
  Autocomplete,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  FormControlLabel,
  FormGroup,
  FormLabel,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Step,
  StepLabel,
  Stepper,
  Switch,
  TextField,
  Typography,
} from '@mui/material'
import {
  Add as AddIcon,
  Group as GroupIcon,
  Lock as LockIcon,
  Public as PublicIcon,
  Settings as SettingsIcon,
} from '@mui/icons-material'
import {Controller, Resolver, SubmitHandler, useForm} from 'react-hook-form'
import {yupResolver} from '@hookform/resolvers/yup'
import {CreateHiveRequest} from '@shared/types'
import {LoadingButton} from '@shared/components/loading'
import {createHiveSchema} from '@shared/validation/schemas'

// Form data type that matches what the form actually provides
// We need to match the yup schema structure exactly
type CreateHiveFormData = {
  name: string
  description: string
  maxMembers: number
  isPublic: boolean
  tags: string[]
  settings: {
    privacyLevel: 'PUBLIC' | 'PRIVATE' | 'INVITE_ONLY'
    category: 'STUDY' | 'WORK' | 'SOCIAL' | 'CODING'
    maxParticipants: number
    allowChat: boolean
    allowVoice: boolean
    requireApproval: boolean
    focusMode: 'POMODORO' | 'TIMEBLOCK' | 'FREEFORM'
    defaultSessionLength: number
    maxSessionLength: number
  }
}

interface CreateHiveFormProps {
  open: boolean
  onClose: () => void
  onSubmit: (hive: CreateHiveRequest) => void
  isLoading?: boolean
  error?: string | null
}

const steps = ['Basic Info', 'Settings', 'Review']

const focusModeOptions = [
  {
    value: 'POMODORO',
    label: 'Pomodoro (25/5 min cycles)',
    description: 'Structured work sessions with breaks'
  },
  {value: 'TIMEBLOCK', label: 'Time Block', description: 'Uninterrupted focus sessions'},
  {value: 'FREEFORM', label: 'Free Form', description: 'Members choose their own timing'},
]

const commonTags = [
  'Study', 'Work', 'Coding', 'Writing', 'Reading', 'Research',
  'Art', 'Design', 'Language Learning', 'Fitness', 'Music',
  'Science', 'Math', 'Programming', 'Business', 'Creative'
]

export const CreateHiveForm: React.FC<CreateHiveFormProps> = ({
                                                                open,
                                                                onClose,
                                                                onSubmit,
                                                                isLoading = false,
                                                                error = null,
                                                              }) => {
  const [activeStep, setActiveStep] = useState(0)
  const nameInputRef = useRef<HTMLInputElement>(null)

  const {
    control,
    handleSubmit,
    watch,
    trigger,
    formState: {isValid, errors},
    reset
  } = useForm<CreateHiveFormData>({
    resolver: yupResolver(createHiveSchema) as Resolver<CreateHiveFormData>,
    mode: 'onChange',
    reValidateMode: 'onChange',
    defaultValues: {
      name: '',
      description: '',
      maxMembers: 10,
      isPublic: true,
      tags: [],
      settings: {
        privacyLevel: 'PUBLIC',
        category: 'STUDY',
        maxParticipants: 10,
        allowChat: true,
        allowVoice: false,
        requireApproval: false,
        focusMode: 'FREEFORM',
        defaultSessionLength: 25,
        maxSessionLength: 120,
      },
    },
  })

  const watchedValues = watch()

  // Debug logging
  console.log('Form state:', { isValid, errors, watchedValues })

  const handleNext = async () => {
    // Validate current step before proceeding
    let fieldsToValidate: (keyof CreateHiveFormData)[] = []

    switch (activeStep) {
      case 0:
        fieldsToValidate = ['name', 'description', 'maxMembers', 'isPublic', 'tags']
        break
      case 1:
        fieldsToValidate = ['settings']
        break
    }

    const isStepValid = await trigger(fieldsToValidate)
    if (isStepValid) {
      setActiveStep((prevActiveStep) => prevActiveStep + 1)
    }
  }

  const handleBack = (): void => {
    setActiveStep((prevActiveStep) => prevActiveStep - 1)
  }

  const onFormSubmit: SubmitHandler<CreateHiveFormData> = (data: CreateHiveFormData) => {
    // Convert form data to API request format
    const apiData: CreateHiveRequest = {
      name: data.name,
      description: data.description,
      maxMembers: data.maxMembers,
      isPublic: data.isPublic,
      tags: data.tags || [],
      settings: {
        privacyLevel: data.settings.privacyLevel,
        category: data.settings.category,
        maxParticipants: data.settings.maxParticipants,
        allowChat: data.settings.allowChat,
        allowVoice: data.settings.allowVoice,
        requireApproval: data.settings.requireApproval,
        focusMode: data.settings.focusMode,
        defaultSessionLength: data.settings.defaultSessionLength,
        maxSessionLength: data.settings.maxSessionLength,
      },
    }
    onSubmit(apiData)
  }

  const handleClose = (): void => {
    if (!isLoading) {
      onClose()
      // Reset form after dialog closes
      setTimeout(() => {
        setActiveStep(0)
        reset()
      }, 300)
    }
  }

  const isStepValid = (step: number): boolean => {
    switch (step) {
      case 0:
        return watchedValues.name?.trim().length >= 3 && watchedValues.description?.trim().length >= 10
      case 1:
        return true
      case 2:
        return true // Always allow final step submission
      default:
        return false
    }
  }

  useEffect(() => {
    // Focus the name input when dialog opens and we're on the first step
    if (open && activeStep === 0 && nameInputRef.current) {
      // Use setTimeout to ensure the dialog is fully rendered
      const timer = setTimeout(() => {
        nameInputRef.current?.focus()
      }, 100)
      return () => clearTimeout(timer)
    }
  }, [open, activeStep])

  const renderStepContent = (step: number): React.ReactElement | null => {
    switch (step) {
      case 0:
        return (
            <Box sx={{display: 'flex', flexDirection: 'column', gap: 3}}>
              <Controller
                  name="name"
                  control={control}
                  render={({field, fieldState}) => (
                      <TextField
                          {...field}
                          inputRef={nameInputRef}
                          label="Hive Name"
                          placeholder="e.g., Study Group, Writing Circle, Code Jam"
                          fullWidth
                          required
                          error={!!fieldState.error}
                          helperText={fieldState.error?.message || `${field.value?.length || 0}/50 characters`}
                          inputProps={{maxLength: 50}}
                      />
                  )}
              />

              <Controller
                  name="description"
                  control={control}
                  render={({field, fieldState}) => (
                      <TextField
                          {...field}
                          label="Description"
                          placeholder="Describe the purpose and goals of your hive..."
                          fullWidth
                          required
                          multiline
                          rows={4}
                          error={!!fieldState.error}
                          helperText={fieldState.error?.message || `${field.value?.length || 0}/500 characters`}
                          inputProps={{maxLength: 500}}
                      />
                  )}
              />

              <Controller
                  name="maxMembers"
                  control={control}
                  render={({field, fieldState}) => (
                      <FormControl fullWidth error={!!fieldState.error}>
                        <InputLabel>Maximum Members</InputLabel>
                        <Select
                            {...field}
                            label="Maximum Members"
                        >
                          {[5, 10, 15, 20, 25, 30, 50].map(num => (
                              <MenuItem key={num} value={num}>{num} members</MenuItem>
                          ))}
                        </Select>
                        {fieldState.error && (
                            <Typography variant="caption" color="error" sx={{ml: 2, mt: 0.5}}>
                              {fieldState.error.message}
                            </Typography>
                        )}
                      </FormControl>
                  )}
              />

              <Controller
                  name="isPublic"
                  control={control}
                  render={({field}) => (
                      <FormControlLabel
                          control={
                            <Switch
                                {...field}
                                checked={field.value}
                                color="primary"
                            />
                          }
                          label={
                            <Box sx={{display: 'flex', alignItems: 'center', gap: 1}}>
                              {field.value ? <PublicIcon/> : <LockIcon/>}
                              {field.value ? 'Public Hive' : 'Private Hive'}
                            </Box>
                          }
                      />
                  )}
              />

              <Typography variant="body2" color="text.secondary">
                {watchedValues.isPublic
                    ? 'Anyone can discover and join this hive'
                    : 'Only invited members can join this hive'
                }
              </Typography>

              <Box>
                <Typography variant="subtitle2" gutterBottom>
                  Tags (help others discover your hive)
                </Typography>
                <Controller
                    name="tags"
                    control={control}
                    render={({field, fieldState}) => (
                        <Autocomplete
                            {...field}
                            multiple
                            freeSolo
                            options={commonTags}
                            value={field.value || []}
                            onChange={(_, newValue) => field.onChange(newValue)}
                            renderTags={(value, getTagProps) =>
                                value.map((option, index) => (
                                    <Chip variant="outlined"
                                          label={option} {...getTagProps({index})} />
                                ))
                            }
                            renderInput={(params) => (
                                <TextField
                                    {...params}
                                    placeholder="Add tags..."
                                    error={!!fieldState.error}
                                    helperText={fieldState.error?.message || "Press Enter to add custom tags"}
                                />
                            )}
                        />
                    )}
                />
              </Box>
            </Box>
        )

      case 1:
        return (
            <Box sx={{display: 'flex', flexDirection: 'column', gap: 3}}>
              <Typography variant="h6" sx={{display: 'flex', alignItems: 'center', gap: 1}}>
                <SettingsIcon/>
                Hive Settings
              </Typography>

              <FormControl component="fieldset">
                <FormLabel component="legend">Communication</FormLabel>
                <FormGroup>
                  <Controller
                      name="settings.allowChat"
                      control={control}
                      render={({field}) => (
                          <FormControlLabel
                              control={
                                <Switch
                                    {...field}
                                    checked={field.value}
                                />
                              }
                              label="Allow chat messages"
                          />
                      )}
                  />
                  <Controller
                      name="settings.allowVoice"
                      control={control}
                      render={({field}) => (
                          <FormControlLabel
                              control={
                                <Switch
                                    {...field}
                                    checked={field.value}
                                />
                              }
                              label="Allow voice calls (coming soon)"
                              disabled
                          />
                      )}
                  />
                </FormGroup>
              </FormControl>

              <Controller
                  name="settings.requireApproval"
                  control={control}
                  render={({field}) => (
                      <FormControlLabel
                          control={
                            <Switch
                                {...field}
                                checked={field.value}
                            />
                          }
                          label="Require approval to join"
                      />
                  )}
              />

              <Controller
                  name="settings.focusMode"
                  control={control}
                  render={({field, fieldState}) => (
                      <FormControl fullWidth error={!!fieldState.error}>
                        <InputLabel>Focus Mode</InputLabel>
                        <Select
                            {...field}
                            label="Focus Mode"
                        >
                          {focusModeOptions.map(option => (
                              <MenuItem key={option.value} value={option.value}>
                                <Box>
                                  <Typography variant="body1">{option.label}</Typography>
                                  <Typography variant="caption" color="text.secondary">
                                    {option.description}
                                  </Typography>
                                </Box>
                              </MenuItem>
                          ))}
                        </Select>
                        {fieldState.error && (
                            <Typography variant="caption" color="error" sx={{ml: 2, mt: 0.5}}>
                              {fieldState.error.message}
                            </Typography>
                        )}
                      </FormControl>
                  )}
              />

              <Box sx={{display: 'flex', gap: 2}}>
                <Controller
                    name="settings.defaultSessionLength"
                    control={control}
                    render={({field, fieldState}) => (
                        <TextField
                            {...field}
                            label="Default Session (minutes)"
                            type="number"
                            inputProps={{min: 5, max: 120, step: 5}}
                            sx={{flex: 1}}
                            error={!!fieldState.error}
                            helperText={fieldState.error?.message}
                        />
                    )}
                />
                <Controller
                    name="settings.maxSessionLength"
                    control={control}
                    render={({field, fieldState}) => (
                        <TextField
                            {...field}
                            label="Max Session (minutes)"
                            type="number"
                            inputProps={{min: 30, max: 480, step: 15}}
                            sx={{flex: 1}}
                            error={!!fieldState.error}
                            helperText={fieldState.error?.message}
                        />
                    )}
                />
              </Box>
            </Box>
        )

      case 2:
        return (
            <Box sx={{display: 'flex', flexDirection: 'column', gap: 3}}>
              <Typography variant="h6">Review Your Hive</Typography>

              <Paper variant="outlined" sx={{p: 3}}>
                <Box sx={{display: 'flex', alignItems: 'center', gap: 2, mb: 2}}>
                  <GroupIcon color="primary"/>
                  <Typography variant="h6">{watchedValues.name}</Typography>
                  <Chip
                      size="small"
                      icon={watchedValues.isPublic ? <PublicIcon/> : <LockIcon/>}
                      label={watchedValues.isPublic ? 'Public' : 'Private'}
                      color={watchedValues.isPublic ? 'success' : 'warning'}
                  />
                </Box>

                <Typography variant="body1" color="text.secondary" paragraph>
                  {watchedValues.description}
                </Typography>

                <Box sx={{display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2}}>
                  {watchedValues.tags?.map(tag => (
                      <Chip key={tag} size="small" label={tag} variant="outlined"/>
                  ))}
                </Box>

                <Divider sx={{my: 2}}/>

                <Typography variant="subtitle2" gutterBottom>Settings</Typography>
                <Box sx={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1}}>
                  <Typography variant="body2">Max Members: {watchedValues.maxMembers}</Typography>
                  <Typography variant="body2">Focus
                    Mode: {watchedValues.settings?.focusMode}</Typography>
                  <Typography variant="body2">
                    Chat: {watchedValues.settings?.allowChat ? 'Enabled' : 'Disabled'}
                  </Typography>
                  <Typography variant="body2">
                    Approval: {watchedValues.settings?.requireApproval ? 'Required' : 'Not Required'}
                  </Typography>
                  <Typography variant="body2">
                    Default Session: {watchedValues.settings?.defaultSessionLength}m
                  </Typography>
                  <Typography variant="body2">
                    Max Session: {watchedValues.settings?.maxSessionLength}m
                  </Typography>
                </Box>
              </Paper>

              {error && (
                  <Alert severity="error">
                    {error}
                  </Alert>
              )}
            </Box>
        )

      default:
        return null
    }
  }

  return (
      <Dialog
          open={open}
          onClose={handleClose}
          maxWidth="desktop"
          fullWidth
          PaperProps={{
            sx: {borderRadius: 2, minHeight: 600}
          }}
      >
        <DialogTitle>
          <Box sx={{display: 'flex', alignItems: 'center', gap: 1}}>
            <AddIcon/>
            Create New Hive
          </Box>
        </DialogTitle>

        <DialogContent>
          <Stepper activeStep={activeStep} sx={{mb: 4}}>
            {steps.map((label) => (
                <Step key={label}>
                  <StepLabel>{label}</StepLabel>
                </Step>
            ))}
          </Stepper>

          {renderStepContent(activeStep)}
        </DialogContent>

        <DialogActions sx={{px: 3, pb: 3}}>
          <Button onClick={handleClose} disabled={isLoading}>
            Cancel
          </Button>

          <Box sx={{display: 'flex', gap: 1}}>
            {activeStep > 0 && (
                <Button onClick={handleBack} disabled={isLoading}>
                  Back
                </Button>
            )}

            {activeStep < steps.length - 1 ? (
                <Button
                    variant="contained"
                    onClick={handleNext}
                    disabled={!isStepValid(activeStep)}
                >
                  Next
                </Button>
            ) : (
                <LoadingButton
                    variant="contained"
                    onClick={handleSubmit(onFormSubmit)}
                    disabled={isLoading || !watchedValues.name?.trim() || watchedValues.name?.trim().length < 3 || !watchedValues.description?.trim() || watchedValues.description?.trim().length < 10}
                    loading={isLoading}
                    loadingText="Creating..."
                    startIcon={<AddIcon/>}
                >
                  Create Hive
                </LoadingButton>
            )}
          </Box>
        </DialogActions>
      </Dialog>
  )
}

export default CreateHiveForm