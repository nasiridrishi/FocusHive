import {useEffect, useRef, useState} from 'react'
import {Alert, Box, IconButton, InputAdornment, Link, TextField, Typography} from '@mui/material'
import {Email, Lock, Login as LoginIcon, Visibility, VisibilityOff} from '@mui/icons-material'
import {useNavigate} from 'react-router-dom'
import {Controller, useForm, SubmitHandler} from 'react-hook-form'
import {yupResolver} from '@hookform/resolvers/yup'
import {LoginRequest} from '@/contracts/auth'
import {LoadingButton} from '@shared/components/loading'
import {FormWithLoading} from '../../../components/Loading'
import {loginSchema} from '@/shared/validation/schemas'
import {useTranslation} from '@shared/components/i18n'
import ValidationSummary from '@shared/components/ValidationSummary'
import {Logo} from '@shared/components/Logo'
import * as yup from 'yup'

interface LoginFormProps {
  onSubmit: (credentials: LoginRequest) => Promise<void>
  isLoading?: boolean
  error?: string | null
  onErrorClear?: () => void
}

// Define form data type from schema
type LoginFormData = yup.InferType<typeof loginSchema>;

export default function LoginForm({onSubmit, isLoading = false, error, onErrorClear}: LoginFormProps) {
  const navigate = useNavigate()
  const {t} = useTranslation('auth')
  const [showPassword, setShowPassword] = useState(false)
  const emailInputRef = useRef<HTMLInputElement>(null)

  const {
    control,
    handleSubmit,
    formState: {errors, isSubmitted}
  } = useForm<LoginFormData>({
    resolver: yupResolver(loginSchema) as any,
    mode: 'onSubmit',
    reValidateMode: 'onChange',
    defaultValues: {
      email: '',
      password: ''
    }
  })

  const onFormSubmit: SubmitHandler<LoginFormData> = async (data) => {
    try {
      // Convert form data to LoginRequest if needed
      const loginData: LoginRequest = {
        email: data.email,
        password: data.password
      };
      await onSubmit(loginData)
    } catch {
      // Error handling is done by parent component
      // console.error('Login form submission error:', err);
    }
  }

  const handleShowPassword = (): void => {
    setShowPassword(prev => !prev)
  }

  useEffect(() => {
    // Focus the email input when component mounts and is not loading
    if (!isLoading && emailInputRef.current) {
      emailInputRef.current.focus()
    }
  }, [isLoading])

  // Clear errors when user starts typing
  const handleFieldChange = (onChange: any) => (event: any) => {
    if (error && onErrorClear) {
      onErrorClear()
    }
    onChange(event)
  }

  return (
      <FormWithLoading
          loading={isLoading}
          loadingMessage={t('login.signingIn')}
          variant="disable"
          disableInteraction={true}
          containerProps={{
            elevation: 3,
            sx: {
              p: 4,
              maxWidth: 400,
              width: '100%',
              borderRadius: 2
            }
          }}
      >
        <Box component="form" onSubmit={handleSubmit(onFormSubmit)} noValidate>
          {/* Logo */}
          <Box sx={{ display: 'flex', justifyContent: 'center', mb: 3 }}>
            <Logo variant="full" height={48} />
          </Box>
          
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
              <Alert severity="error" sx={{mb: 2}}>
                {error}
              </Alert>
          )}

          <Controller
              name="email"
              control={control}
              render={({field, fieldState}) => (
                  <TextField
                      {...field}
                      onChange={handleFieldChange(field.onChange)}
                      fullWidth
                      id="email"
                      label={t('login.email')}
                      type="email"
                      error={isSubmitted && !!fieldState.error}
                      disabled={isLoading}
                      inputRef={emailInputRef}
                      InputProps={{
                        startAdornment: (
                            <InputAdornment position="start">
                              <Email color="action"/>
                            </InputAdornment>
                        ),
                      }}
                      sx={{mb: 2}}
                      autoComplete="email"
                  />
              )}
          />

          <Controller
              name="password"
              control={control}
              render={({field, fieldState}) => (
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
                              <Lock color="action"/>
                            </InputAdornment>
                        ),
                        endAdornment: (
                            <InputAdornment position="end">
                              <IconButton
                                  onClick={handleShowPassword}
                                  edge="end"
                                  aria-label={t('login.togglePasswordVisibility')}
                              >
                                {showPassword ? <VisibilityOff/> : <Visibility/>}
                              </IconButton>
                            </InputAdornment>
                        ),
                      }}
                      sx={{mb: 3}}
                      autoComplete="current-password"
                  />
              )}
          />

          {isSubmitted && Object.keys(errors).length > 0 && (
              <ValidationSummary errors={errors}/>
          )}

          <LoadingButton
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              loading={isLoading}
              loadingText={t('login.signingIn')}
              startIcon={<LoginIcon/>}
              sx={{
                py: 1.5,
                mb: 2,
                fontSize: '1rem',
                fontWeight: 600
              }}
          >
            {t('login.signIn')}
          </LoadingButton>

          <Box sx={{textAlign: 'center', mt: 2}}>
            <Link
                component="button"
                type="button"
                variant="body2"
                onClick={() => navigate('/forgot-password')}
                sx={{mr: 2}}
            >
              {t('login.forgotPassword')}
            </Link>
          </Box>

          <Box sx={{textAlign: 'center', mt: 2}}>
            <Typography variant="body2" color="text.secondary">
              {t('login.noAccount')}{' '}
              <Link
                  component="button"
                  type="button"
                  onClick={() => navigate('/register')}
                  sx={{fontWeight: 600}}
              >
                {t('login.signUp')}
              </Link>
            </Typography>
          </Box>
        </Box>
      </FormWithLoading>
  )
}