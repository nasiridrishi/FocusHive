import React from 'react'
import { Box, Container } from '@mui/material'
import { useNavigate, useLocation } from 'react-router-dom'
import LoginForm from '../components/LoginForm'
import { LoginRequest } from '@shared/types'
import { useAuth } from '../hooks/useAuth'

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { authState, login, clearError } = useAuth()

  // Clear any existing error when component mounts
  React.useEffect(() => {
    if (authState.error) {
      clearError()
    }
  }, [authState.error, clearError])

  const handleLogin = async (credentials: LoginRequest) => {
    try {
      await login(credentials)
      
      // Navigate to the intended destination or dashboard
      const from = location.state?.from?.pathname || '/dashboard'
      navigate(from, { replace: true })
      
    } catch (error) {
      // Error is already handled by the auth context
      // The error will be displayed via authState.error
      console.error('Login failed:', error)
    }
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        py: 4
      }}
    >
      <Container maxWidth="tablet">
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center'
          }}
        >
          <LoginForm
            onSubmit={handleLogin}
            isLoading={authState.isLoading}
            error={authState.error}
          />
        </Box>
      </Container>
    </Box>
  )
}