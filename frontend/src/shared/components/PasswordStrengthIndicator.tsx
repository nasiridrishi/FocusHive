import React from 'react'
import { Box, LinearProgress, Typography, Chip } from '@mui/material'
import {
  Check as CheckIcon,
  Close as CloseIcon,
  Security as SecurityIcon
} from '@mui/icons-material'
import { checkPasswordStrength } from '@shared/validation/schemas'

interface PasswordStrengthIndicatorProps {
  password: string
  show?: boolean
}

const STRENGTH_COLORS = {
  weak: '#f44336',
  fair: '#ff9800',
  good: '#2196f3',
  strong: '#4caf50'
} as const

const STRENGTH_LABELS = {
  weak: 'Weak',
  fair: 'Fair',
  good: 'Good',
  strong: 'Strong'
} as const

export const PasswordStrengthIndicator: React.FC<PasswordStrengthIndicatorProps> = ({
  password,
  show = true
}) => {
  if (!show || !password) {
    return null
  }

  const { score, feedback, strength } = checkPasswordStrength(password)
  const progressValue = (score / 6) * 100 // Max score is 6 (5 criteria + length bonus)

  return (
    <Box sx={{ mt: 1, mb: 1 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
        <SecurityIcon fontSize="small" />
        <Typography variant="caption" sx={{ flexGrow: 1 }}>
          Password Strength
        </Typography>
        <Chip
          size="small"
          label={STRENGTH_LABELS[strength]}
          sx={{
            backgroundColor: STRENGTH_COLORS[strength],
            color: 'white',
            fontSize: '0.7rem',
            height: 20
          }}
        />
      </Box>

      <LinearProgress
        variant="determinate"
        value={progressValue}
        sx={{
          height: 6,
          borderRadius: 3,
          backgroundColor: 'grey.300',
          '& .MuiLinearProgress-bar': {
            backgroundColor: STRENGTH_COLORS[strength],
            borderRadius: 3,
          },
        }}
      />

      {feedback.length > 0 && (
        <Box sx={{ mt: 1 }}>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>
            To improve your password:
          </Typography>
          {feedback.map((item, index) => (
            <Box key={index} sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <CloseIcon sx={{ fontSize: 12, color: 'error.main' }} />
              <Typography variant="caption" color="text.secondary">
                {item}
              </Typography>
            </Box>
          ))}
        </Box>
      )}

      {strength === 'strong' && (
        <Box sx={{ mt: 1, display: 'flex', alignItems: 'center', gap: 0.5 }}>
          <CheckIcon sx={{ fontSize: 12, color: 'success.main' }} />
          <Typography variant="caption" color="success.main">
            Strong password!
          </Typography>
        </Box>
      )}
    </Box>
  )
}

export default PasswordStrengthIndicator