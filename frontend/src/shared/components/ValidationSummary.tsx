import { Alert, Box } from '@mui/material'
import { FieldErrors } from 'react-hook-form'

interface ValidationSummaryProps {
  errors: FieldErrors
  fieldLabels?: Record<string, string>
}

export default function ValidationSummary({ errors, fieldLabels = {} }: ValidationSummaryProps) {
  const errorFields = Object.keys(errors)
  
  // Separate acceptTerms from other field errors
  const hasTermsError = errorFields.includes('acceptTerms')
  const fieldErrors = errorFields.filter(field => field !== 'acceptTerms')
  const fieldErrorCount = fieldErrors.length
  
  if (errorFields.length === 0) return null
  
  // Default field labels if not provided
  const defaultLabels: Record<string, string> = {
    email: 'Email',
    password: 'Password',
    confirmPassword: 'Confirm Password',
    firstName: 'First Name',
    lastName: 'Last Name',
    username: 'Username',
    ...fieldLabels
  }
  
  let message = ''
  
  // First check if there are field errors (not including terms)
  if (fieldErrorCount > 0) {
    if (fieldErrorCount >= 3) {
      // For 3+ errors, show generic message
      message = 'Please fill in all highlighted fields'
    } else if (fieldErrorCount === 2) {
      // For 2 errors, list both fields
      const fieldNames = fieldErrors.map(field => defaultLabels[field] || field)
      message = `${fieldNames[0]} and ${fieldNames[1]} are required`
    } else {
      // For 1 error, show specific field
      const fieldName = defaultLabels[fieldErrors[0]] || fieldErrors[0]
      message = `${fieldName} is required`
    }
  } else if (hasTermsError) {
    // Only show terms error if all other fields are filled
    message = 'You must accept the terms and conditions'
  }
  
  return (
    <Box sx={{ mb: 2 }}>
      <Alert severity="error" variant="outlined">
        {message}
      </Alert>
    </Box>
  )
}