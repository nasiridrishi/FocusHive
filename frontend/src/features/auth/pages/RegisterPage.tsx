import React from 'react'
import { Box, Container } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import RegisterForm from '../components/RegisterForm'
import { RegisterRequest } from '@shared/types'

export default function RegisterPage() {
  const navigate = useNavigate()
  
  // TODO: Replace with actual auth context/hook
  const [isLoading, setIsLoading] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)

  const handleRegister = async (userData: RegisterRequest) => {
    setIsLoading(true)
    setError(null)

    try {
      // TODO: Implement actual registration API call
      
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      // For now, just navigate to dashboard
      // In real implementation, this would be handled by auth context
      navigate('/dashboard', { replace: true })
      
    } catch (err) {
      setError('Registration failed. Please try again.')
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
          <RegisterForm
            onSubmit={handleRegister}
            isLoading={isLoading}
            error={error}
          />
        </Box>
      </Container>
    </Box>
  )
}