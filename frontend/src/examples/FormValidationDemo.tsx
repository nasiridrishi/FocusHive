import React, { useState } from 'react'
import {
  Container,
  Paper,
  Typography,
  Box,
  Button,
  Divider,
  Alert,
  Tabs,
  Tab
} from '@mui/material'
import LoginForm from '../features/auth/components/LoginForm'
import RegisterForm from '../features/auth/components/RegisterForm'
import CreateHiveForm from '../features/hive/components/CreateHiveForm'
import { LoginRequest, RegisterRequest, CreateHiveRequest } from '../shared/types'

interface TabPanelProps {
  children?: React.ReactNode
  index: number
  value: number
}

function TabPanel({ children, value, index }: TabPanelProps) {
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`form-tabpanel-${index}`}
      aria-labelledby={`form-tab-${index}`}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  )
}

const FormValidationDemo: React.FC = () => {
  const [activeTab, setActiveTab] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [messageType, setMessageType] = useState<'success' | 'error'>('success')
  const [hiveFormOpen, setHiveFormOpen] = useState(false)

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue)
    setMessage(null)
  }

  const handleLogin = async (credentials: LoginRequest) => {
    setIsLoading(true)
    setMessage(null)
    
    try {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1500))
      
      console.log('Login data:', credentials)
      setMessage(`Login successful! Email: ${credentials.email}`)
      setMessageType('success')
    } catch (error) {
      setMessage('Login failed. Please try again.')
      setMessageType('error')
    } finally {
      setIsLoading(false)
    }
  }

  const handleRegister = async (userData: RegisterRequest) => {
    setIsLoading(true)
    setMessage(null)
    
    try {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 2000))
      
      console.log('Register data:', userData)
      setMessage(`Registration successful! Welcome, ${userData.firstName}!`)
      setMessageType('success')
    } catch (error) {
      setMessage('Registration failed. Please try again.')
      setMessageType('error')
    } finally {
      setIsLoading(false)
    }
  }

  const handleCreateHive = async (hiveData: CreateHiveRequest) => {
    setIsLoading(true)
    setMessage(null)
    
    try {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      console.log('Create hive data:', hiveData)
      setMessage(`Hive "${hiveData.name}" created successfully!`)
      setMessageType('success')
      setHiveFormOpen(false)
    } catch (error) {
      setMessage('Failed to create hive. Please try again.')
      setMessageType('error')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Container maxWidth={false} sx={{ py: 4, maxWidth: 'md' }}>
      <Paper sx={{ p: 4, borderRadius: 2 }}>
        <Typography variant="h4" component="h1" gutterBottom align="center">
          Form Validation Demo
        </Typography>
        
        <Typography variant="body1" color="text.secondary" align="center" sx={{ mb: 4 }}>
          Test the enhanced form validation with react-hook-form, Yup schemas, and Material UI integration.
        </Typography>

        {message && (
          <Alert severity={messageType} sx={{ mb: 3 }}>
            {message}
          </Alert>
        )}

        <Tabs value={activeTab} onChange={handleTabChange} centered>
          <Tab label="Login Form" />
          <Tab label="Register Form" />
          <Tab label="Create Hive Form" />
        </Tabs>

        <TabPanel value={activeTab} index={0}>
          <Box display="flex" justifyContent="center">
            <LoginForm
              onSubmit={handleLogin}
              isLoading={isLoading}
              error={messageType === 'error' ? message : null}
            />
          </Box>
          <Divider sx={{ my: 3 }} />
          <Typography variant="h6" gutterBottom>
            Validation Features:
          </Typography>
          <Typography variant="body2" component="ul" sx={{ pl: 2 }}>
            <li>Email format validation</li>
            <li>Password minimum length (6 characters)</li>
            <li>Real-time validation feedback</li>
            <li>Password visibility toggle</li>
          </Typography>
        </TabPanel>

        <TabPanel value={activeTab} index={1}>
          <Box display="flex" justifyContent="center">
            <RegisterForm
              onSubmit={handleRegister}
              isLoading={isLoading}
              error={messageType === 'error' ? message : null}
            />
          </Box>
          <Divider sx={{ my: 3 }} />
          <Typography variant="h6" gutterBottom>
            Validation Features:
          </Typography>
          <Typography variant="body2" component="ul" sx={{ pl: 2 }}>
            <li>Name validation (2-50 characters, letters only)</li>
            <li>Username validation (3-30 characters, alphanumeric)</li>
            <li>Strong password requirements with strength indicator</li>
            <li>Password confirmation matching</li>
            <li>Terms acceptance requirement</li>
            <li>Real-time validation with field-specific error messages</li>
          </Typography>
        </TabPanel>

        <TabPanel value={activeTab} index={2}>
          <Box textAlign="center">
            <Button
              variant="contained"
              size="large"
              onClick={() => setHiveFormOpen(true)}
              sx={{ mb: 3 }}
            >
              Open Create Hive Form
            </Button>
            
            <CreateHiveForm
              open={hiveFormOpen}
              onClose={() => setHiveFormOpen(false)}
              onSubmit={handleCreateHive}
              isLoading={isLoading}
              error={messageType === 'error' ? message : null}
            />
          </Box>
          
          <Divider sx={{ my: 3 }} />
          <Typography variant="h6" gutterBottom>
            Validation Features:
          </Typography>
          <Typography variant="body2" component="ul" sx={{ pl: 2 }}>
            <li>Multi-step form with step-by-step validation</li>
            <li>Hive name validation (3-50 characters)</li>
            <li>Description validation (10-500 characters)</li>
            <li>Tag validation (max 10 tags, 20 chars each)</li>
            <li>Session length validation with cross-field checks</li>
            <li>Real-time character counts</li>
            <li>Step validation before proceeding</li>
          </Typography>
        </TabPanel>
      </Paper>
    </Container>
  )
}

export default FormValidationDemo