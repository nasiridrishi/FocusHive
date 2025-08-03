import { Container, Typography, Button, Box } from '@mui/material'
import { useNavigate } from 'react-router-dom'

export default function HomePage() {
  const navigate = useNavigate()

  const handleGetStarted = () => {
    navigate('/login')
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
      }}
    >
      <Container maxWidth="lg">
        <Box
          sx={{
            textAlign: 'center',
            py: 8,
          }}
        >
          <Typography
            variant="h2"
            component="h1"
            gutterBottom
            sx={{
              fontWeight: 'bold',
              color: 'text.primary',
              mb: 3,
            }}
          >
            Welcome to FocusHive
          </Typography>
          <Typography
            variant="h5"
            component="p"
            color="text.secondary"
            sx={{
              maxWidth: 600,
              mx: 'auto',
              mb: 4,
            }}
          >
            Your digital co-working and co-studying platform
          </Typography>
          <Typography
            variant="body1"
            color="text.secondary"
            sx={{
              maxWidth: 800,
              mx: 'auto',
              mb: 5,
            }}
          >
            Join virtual hives where you can work on individual tasks while being visibly present and accountable to others. 
            Boost your productivity through passive accountability and collaborative focus sessions.
          </Typography>
          <Button
            variant="contained"
            size="large"
            onClick={handleGetStarted}
            sx={{
              py: 2,
              px: 4,
              fontSize: '1.1rem',
              borderRadius: 2,
            }}
          >
            Get Started
          </Button>
        </Box>
      </Container>
    </Box>
  )
}