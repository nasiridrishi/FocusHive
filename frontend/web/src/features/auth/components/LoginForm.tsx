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
  IconButton
} from '@mui/material'
import {
  Visibility,
  VisibilityOff,
  Email,
  Lock
} from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { LoginRequest, FormErrors } from '@shared/types'

interface LoginFormProps {
  onSubmit: (credentials: LoginRequest) => Promise<void>
  isLoading?: boolean
  error?: string | null
}

export default function LoginForm({ onSubmit, isLoading = false, error }: LoginFormProps) {
  const navigate = useNavigate()
  const [formData, setFormData] = useState<LoginRequest>({
    email: '',
    password: ''
  })
  const [showPassword, setShowPassword] = useState(false)
  const [formErrors, setFormErrors] = useState<FormErrors>({})

  const validateForm = (): boolean => {
    const errors: FormErrors = {}

    if (!formData.email) {
      errors.email = ['Email is required']
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      errors.email = ['Please enter a valid email address']
    }

    if (!formData.password) {
      errors.password = ['Password is required']
    } else if (formData.password.length < 6) {
      errors.password = ['Password must be at least 6 characters']
    }

    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleInputChange = (field: keyof LoginRequest) => (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    setFormData(prev => ({
      ...prev,
      [field]: event.target.value
    }))
    
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
      // Error handling is done by parent component
      console.error('Login error:', err)
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
      <Box component="form" onSubmit={handleSubmit} noValidate>
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
          Welcome Back
        </Typography>

        <Typography
          variant="body2"
          color="text.secondary"
          sx={{
            textAlign: 'center',
            mb: 3
          }}
        >
          Sign in to continue to FocusHive
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

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
          sx={{ mb: 2 }}
          autoComplete="email"
          autoFocus
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
          sx={{ mb: 3 }}
          autoComplete="current-password"
        />

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
          {isLoading ? 'Signing In...' : 'Sign In'}
        </Button>

        <Box sx={{ textAlign: 'center', mt: 2 }}>
          <Link
            component="button"
            type="button"
            variant="body2"
            onClick={() => navigate('/forgot-password')}
            sx={{ mr: 2 }}
          >
            Forgot Password?
          </Link>
        </Box>

        <Box sx={{ textAlign: 'center', mt: 2 }}>
          <Typography variant="body2" color="text.secondary">
            Don't have an account?{' '}
            <Link
              component="button"
              type="button"
              onClick={() => navigate('/register')}
              sx={{ fontWeight: 600 }}
            >
              Sign Up
            </Link>
          </Typography>
        </Box>
      </Box>
    </Paper>
  )
}