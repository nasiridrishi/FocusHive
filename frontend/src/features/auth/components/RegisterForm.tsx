import React, { useState } from 'react'
import {
  Box,
  TextField,
  Button,
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
  PersonOutline
} from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { RegisterRequest, FormErrors } from '@shared/types'

interface RegisterFormProps {
  onSubmit: (userData: RegisterRequest) => Promise<void>
  isLoading?: boolean
  error?: string | null
}

export default function RegisterForm({ onSubmit, isLoading = false, error }: RegisterFormProps) {
  const navigate = useNavigate()
  const [formData, setFormData] = useState<RegisterRequest>({
    email: '',
    password: '',
    username: '',
    firstName: '',
    lastName: ''
  })
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [acceptTerms, setAcceptTerms] = useState(false)
  const [formErrors, setFormErrors] = useState<FormErrors>({})

  const validateForm = (): boolean => {
    const errors: FormErrors = {}

    if (!formData.firstName.trim()) {
      errors.firstName = ['First name is required']
    }

    if (!formData.lastName.trim()) {
      errors.lastName = ['Last name is required']
    }

    if (!formData.username.trim()) {
      errors.username = ['Username is required']
    } else if (formData.username.length < 3) {
      errors.username = ['Username must be at least 3 characters']
    } else if (!/^[a-zA-Z0-9_]+$/.test(formData.username)) {
      errors.username = ['Username can only contain letters, numbers, and underscores']
    }

    if (!formData.email) {
      errors.email = ['Email is required']
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      errors.email = ['Please enter a valid email address']
    }

    if (!formData.password) {
      errors.password = ['Password is required']
    } else if (formData.password.length < 8) {
      errors.password = ['Password must be at least 8 characters']
    } else if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(formData.password)) {
      errors.password = ['Password must contain at least one uppercase letter, one lowercase letter, and one number']
    }

    if (!confirmPassword) {
      errors.confirmPassword = ['Please confirm your password']
    } else if (formData.password !== confirmPassword) {
      errors.confirmPassword = ['Passwords do not match']
    }

    if (!acceptTerms) {
      errors.terms = ['You must accept the terms and conditions']
    }

    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleInputChange = (field: keyof RegisterRequest | 'confirmPassword') => (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const value = event.target.value

    if (field === 'confirmPassword') {
      setConfirmPassword(value)
    } else {
      setFormData(prev => ({
        ...prev,
        [field]: value
      }))
    }
    
    // Clear field error when user starts typing
    if (formErrors[field]) {
      setFormErrors(prev => ({
        ...prev,
        [field]: []
      }))
    }
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    
    if (!validateForm()) {
      return
    }

    try {
      await onSubmit(formData)
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
      <Box component="form" onSubmit={handleSubmit} noValidate>
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
          <TextField
            fullWidth
            id="firstName"
            label="First Name"
            value={formData.firstName}
            onChange={handleInputChange('firstName')}
            error={Boolean(formErrors.firstName?.length)}
            helperText={formErrors.firstName?.[0]}
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
          <TextField
            fullWidth
            id="lastName"
            label="Last Name"
            value={formData.lastName}
            onChange={handleInputChange('lastName')}
            error={Boolean(formErrors.lastName?.length)}
            helperText={formErrors.lastName?.[0]}
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
        </Box>

        <TextField
          fullWidth
          id="username"
          label="Username"
          value={formData.username}
          onChange={handleInputChange('username')}
          error={Boolean(formErrors.username?.length)}
          helperText={formErrors.username?.[0]}
          disabled={isLoading}
          sx={{ mt: 2 }}
          autoComplete="username"
        />

        <TextField
          fullWidth
          id="email"
          label="Email Address"
          type="email"
          value={formData.email}
          onChange={handleInputChange('email')}
          error={Boolean(formErrors.email?.length)}
          helperText={formErrors.email?.[0]}
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

        <TextField
          fullWidth
          id="password"
          label="Password"
          type={showPassword ? 'text' : 'password'}
          value={formData.password}
          onChange={handleInputChange('password')}
          error={Boolean(formErrors.password?.length)}
          helperText={formErrors.password?.[0]}
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
          sx={{ mt: 2 }}
          autoComplete="new-password"
        />

        <TextField
          fullWidth
          id="confirmPassword"
          label="Confirm Password"
          type={showConfirmPassword ? 'text' : 'password'}
          value={confirmPassword}
          onChange={handleInputChange('confirmPassword')}
          error={Boolean(formErrors.confirmPassword?.length)}
          helperText={formErrors.confirmPassword?.[0]}
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

        <FormControlLabel
          control={
            <Checkbox
              checked={acceptTerms}
              onChange={(e) => setAcceptTerms(e.target.checked)}
              disabled={isLoading}
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

        {formErrors.terms && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {formErrors.terms[0]}
          </Alert>
        )}

        <Button
          type="submit"
          fullWidth
          variant="contained"
          size="large"
          disabled={isLoading}
          sx={{
            py: 1.5,
            mb: 2,
            fontSize: '1rem',
            fontWeight: 600
          }}
        >
          {isLoading ? 'Creating Account...' : 'Create Account'}
        </Button>

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