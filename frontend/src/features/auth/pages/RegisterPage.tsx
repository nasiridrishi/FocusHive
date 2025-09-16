import React from 'react'
import {Box, Container} from '@mui/material'
import {useNavigate} from 'react-router-dom'
import RegisterForm from '../components/RegisterForm'
import {RegisterRequest} from '@shared/types'
import {useAuth} from '../hooks/useAuth'

export default function RegisterPage(): JSX.Element {
  const navigate = useNavigate()
  const {authState, register, clearError} = useAuth()

  // Clear any existing error when component mounts
  React.useEffect(() => {
    if (authState.error) {
      clearError()
    }
  }, [authState.error, clearError])

  const handleRegister = async (userData: RegisterRequest) => {
    try {
      await register(userData)

      // Navigate to dashboard after successful registration
      navigate('/dashboard', {replace: true})

    } catch {
      // Register error logged to error service
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
            <RegisterForm
                onSubmit={handleRegister}
                isLoading={authState.isLoading}
                error={authState.error}
            />
          </Box>
        </Container>
      </Box>
  )
}