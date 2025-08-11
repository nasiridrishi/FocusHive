import React from 'react'
import { Box, Container } from '@mui/material'
import { useNavigate, useLocation } from 'react-router-dom'
import LoginForm from '../components/LoginForm'
import { LoginRequest } from '@shared/types'

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  
  // TODO: Replace with actual auth context/hook
  const [isLoading, setIsLoading] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)

  const handleLogin = async (credentials: LoginRequest) => {
    setIsLoading(true)
    setError(null)

    try {
      // TODO: Implement actual login API call
      // Will use credentials.email and credentials.password when API is implemented
      void credentials; // Temporary to satisfy linter
      
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      // For now, just navigate to dashboard
      // In real implementation, this would be handled by auth context
      const from = location.state?.from?.pathname || '/dashboard'
      navigate(from, { replace: true })
      
    } catch (err) {
      setError('Invalid email or password. Please try again.')
    } finally {
      setIsLoading(false)
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
            isLoading={isLoading}
            error={error}
          />
        </Box>
      </Container>
    </Box>
  )
}