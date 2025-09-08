import { useState } from 'react'
import {
  Box,
  TextField,
  Typography,
  Alert,
  Paper,
  Link,
  InputAdornment,
  IconButton
} from '@mui/material'
import {
  Visibility,
  VisibilityOff,
  Email,
  Lock,
  Login as LoginIcon
} from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { useForm, Controller } from 'react-hook-form'
import { yupResolver } from '@hookform/resolvers/yup'
import { LoginRequest } from '@shared/types'
import { LoadingButton } from '@shared/components/loading'
import { loginSchema } from '@shared/validation/schemas'
import { useTranslation } from '@shared/components/i18n'
import ValidationSummary from '@shared/components/ValidationSummary'

interface LoginFormProps {
  onSubmit: (credentials: LoginRequest) => Promise<void>
  isLoading?: boolean
  error?: string | null
}

export default function LoginForm({ onSubmit, isLoading = false, error }: LoginFormProps) {
  const navigate = useNavigate()
  const { t } = useTranslation('auth')
  const [showPassword, setShowPassword] = useState(false)
  
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitted }
  } = useForm<LoginRequest>({
    resolver: yupResolver(loginSchema),
    mode: 'onSubmit',
    reValidateMode: 'onChange',
    defaultValues: {
      email: '',
      password: ''
    }
  })

  const onFormSubmit = async (data: LoginRequest) => {
    try {
      await onSubmit(data)
    } catch (err) {
      // Error handling is done by parent component
      console.error('Login form submission error:', err);
    }
  }

  const handleShowPassword = () => {
    setShowPassword(prev => !prev)
  }

  return (
    <Paper
      elevation={3}
      sx={{
        p: 4,
        maxWidth: 400,
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
            mb: 3
          }}
        >
          {t('login.title')}
        </Typography>

        <Typography
          variant="body2"
          color="text.secondary"
          sx={{
            textAlign: 'center',
            mb: 3
          }}
        >
          {t('login.subtitle')}
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Controller
          name="email"
          control={control}
          render={({ field, fieldState }) => (
            <TextField
              {...field}
              fullWidth
              id="email"
              label={t('login.email')}
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
              sx={{ mb: 2 }}
              autoComplete="email"
              autoFocus
            />
          )}
        />

        <Controller
          name="password"
          control={control}
          render={({ field, fieldState }) => (
            <TextField
              {...field}
              fullWidth
              id="password"
              label={t('login.password')}
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
                      aria-label={t('login.togglePasswordVisibility')}
                    >
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
              sx={{ mb: 3 }}
              autoComplete="current-password"
            />
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
          loadingText={t('login.signingIn')}
          startIcon={<LoginIcon />}
          sx={{
            py: 1.5,
            mb: 2,
            fontSize: '1rem',
            fontWeight: 600
          }}
        >
          {t('login.signIn')}
        </LoadingButton>

        <Box sx={{ textAlign: 'center', mt: 2 }}>
          <Link
            component="button"
            type="button"
            variant="body2"
            onClick={() => navigate('/forgot-password')}
            sx={{ mr: 2 }}
          >
            {t('login.forgotPassword')}
          </Link>
        </Box>

        <Box sx={{ textAlign: 'center', mt: 2 }}>
          <Typography variant="body2" color="text.secondary">
            {t('login.noAccount')}{' '}
            <Link
              component="button"
              type="button"
              onClick={() => navigate('/register')}
              sx={{ fontWeight: 600 }}
            >
              {t('login.signUp')}
            </Link>
          </Typography>
        </Box>
      </Box>
    </Paper>
  )
}