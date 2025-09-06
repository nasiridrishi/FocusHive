import { useState } from 'react'
import {
  Box,
  TextField,
  Typography,
  Alert,
  Paper,
  Link,
  InputAdornment,
  IconButton,
  FormControlLabel,
  Checkbox
} from '@mui/material'
import {
  Visibility,
  VisibilityOff,
  Email,
  Lock,
  Person,
  PersonOutline,
  PersonAdd
} from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { useForm, Controller } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { RegisterRequest } from '@shared/types'
import { LoadingButton } from '@shared/components/loading'
import { registerSchema } from '@shared/validation/schemas'
import PasswordStrengthIndicator from '@shared/components/PasswordStrengthIndicator'
import ValidationSummary from '@shared/components/ValidationSummary'

interface RegisterFormProps {
  onSubmit: (userData: RegisterRequest) => Promise<void>
  isLoading?: boolean
  error?: string | null
}

type RegisterFormData = RegisterRequest & {
  confirmPassword: string
  acceptTerms: boolean
}

export default function RegisterForm({ onSubmit, isLoading = false, error }: RegisterFormProps) {
  const navigate = useNavigate()
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)

  const {
    control,
    handleSubmit,
    watch,
    formState: { errors, isSubmitted }
  } = useForm<RegisterFormData>({
    resolver: yupResolver(registerSchema),
    mode: 'onSubmit',
    reValidateMode: 'onChange',
    defaultValues: {
      firstName: '',
      lastName: '',
      username: '',
      email: '',
      password: '',
      confirmPassword: '',
      acceptTerms: false
    }
  })

  const password = watch('password')

  const onFormSubmit = async (data: RegisterFormData) => {
    try {
      // Remove confirmPassword and acceptTerms before submitting
      const { confirmPassword, acceptTerms, ...submitData } = data
      await onSubmit(submitData)
    } catch (err) {
      // Registration error handled by parent component
    }
  }

  const handleShowPassword = () => setShowPassword(prev => !prev)
  const handleShowConfirmPassword = () => setShowConfirmPassword(prev => !prev)

  return (
    <Paper
      elevation={3}
      sx={{
        p: 4,
        maxWidth: 500,
        width: '100%',
        borderRadius: 2
      }}
    >
      <Box component="form" onSubmit={handleSubmit(onFormSubmit)} noValidate>
        <Typography
          variant="h4"
          component="h1"
          gutterBottom
          sx={{
            textAlign: 'center',
            fontWeight: 'bold',
            color: 'primary.main',
            mb: 2
          }}
        >
          Join FocusHive
        </Typography>

        <Typography
          variant="body2"
          color="text.secondary"
          sx={{
            textAlign: 'center',
            mb: 3
          }}
        >
          Create your account to start focusing together
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
            gap: 2
          }}
        >
          <Controller
            name="firstName"
            control={control}
            render={({ field, fieldState }) => (
              <TextField
                {...field}
                fullWidth
                id="firstName"
                label="First Name"
                error={isSubmitted && !!fieldState.error}
                disabled={isLoading}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Person color="action" />
                    </InputAdornment>
                  ),
                }}
                autoComplete="given-name"
                autoFocus
              />
            )}
          />
          
          <Controller
            name="lastName"
            control={control}
            render={({ field, fieldState }) => (
              <TextField
                {...field}
                fullWidth
                id="lastName"
                label="Last Name"
                error={isSubmitted && !!fieldState.error}
                disabled={isLoading}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <PersonOutline color="action" />
                    </InputAdornment>
                  ),
                }}
                autoComplete="family-name"
              />
            )}
          />
        </Box>

        <Controller
          name="username"
          control={control}
          render={({ field, fieldState }) => (
            <TextField
              {...field}
              fullWidth
              id="username"
              label="Username"
              error={isSubmitted && !!fieldState.error}
              disabled={isLoading}
              sx={{ mt: 2 }}
              autoComplete="username"
            />
          )}
        />

        <Controller
          name="email"
          control={control}
          render={({ field, fieldState }) => (
            <TextField
              {...field}
              fullWidth
              id="email"
              label="Email Address"
              type="email"
              error={isSubmitted && !!fieldState.error}
              disabled={isLoading}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Email color="action" />
                  </InputAdornment>
                ),
              }}
              sx={{ mt: 2 }}
              autoComplete="email"
            />
          )}
        />

        <Controller
          name="password"
          control={control}
          render={({ field, fieldState }) => (
            <Box sx={{ mt: 2 }}>
              <TextField
                {...field}
                fullWidth
                id="password"
                label="Password"
                type={showPassword ? 'text' : 'password'}
                error={isSubmitted && !!fieldState.error}
                disabled={isLoading}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Lock color="action" />
                    </InputAdornment>
                  ),
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        onClick={handleShowPassword}
                        edge="end"
                        aria-label="toggle password visibility"
                      >
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
                autoComplete="new-password"
              />
              <PasswordStrengthIndicator 
                password={password || ''} 
                show={!!password}
              />
            </Box>
          )}
        />

        <Controller
          name="confirmPassword"
          control={control}
          render={({ field, fieldState }) => (
            <TextField
              {...field}
              fullWidth
              id="confirmPassword"
              label="Confirm Password"
              type={showConfirmPassword ? 'text' : 'password'}
              error={isSubmitted && !!fieldState.error}
              disabled={isLoading}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Lock color="action" />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={handleShowConfirmPassword}
                      edge="end"
                      aria-label="toggle confirm password visibility"
                    >
                      {showConfirmPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
              sx={{ mt: 2 }}
              autoComplete="new-password"
            />
          )}
        />

        <Controller
          name="acceptTerms"
          control={control}
          render={({ field, fieldState }) => (
            <Box>
              <FormControlLabel
                control={
                  <Checkbox
                    {...field}
                    checked={field.value}
                    disabled={isLoading}
                    sx={{
                      color: isSubmitted && fieldState.error ? 'error.main' : undefined,
                      '&.Mui-checked': {
                        color: isSubmitted && fieldState.error ? 'error.main' : 'primary.main'
                      }
                    }}
                  />
                }
                label={
                  <Typography variant="body2">
                    I agree to the{' '}
                    <Link href="/terms" target="_blank" rel="noopener">
                      Terms of Service
                    </Link>{' '}
                    and{' '}
                    <Link href="/privacy" target="_blank" rel="noopener">
                      Privacy Policy
                    </Link>
                  </Typography>
                }
                sx={{ mt: 2, mb: 1 }}
              />
            </Box>
          )}
        />

        {isSubmitted && Object.keys(errors).length > 0 && (
          <ValidationSummary errors={errors} />
        )}

        <LoadingButton
          type="submit"
          fullWidth
          variant="contained"
          size="large"
          loading={isLoading}
          loadingText="Creating Account..."
          startIcon={<PersonAdd />}
          sx={{
            py: 1.5,
            mb: 2,
            fontSize: '1rem',
            fontWeight: 600
          }}
        >
          Create Account
        </LoadingButton>

        <Box sx={{ textAlign: 'center', mt: 2 }}>
          <Typography variant="body2" color="text.secondary">
            Already have an account?{' '}
            <Link
              component="button"
              type="button"
              onClick={() => navigate('/login')}
              sx={{ fontWeight: 600 }}
            >
              Sign In
            </Link>
          </Typography>
        </Box>
      </Box>
    </Paper>
  )
}