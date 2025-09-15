import React from 'react'
import {
  Card,
  CardContent,
  Typography,
  Box,
  Skeleton,
  useTheme,
  alpha,
} from '@mui/material'
import { TrendingUp, TrendingDown, Error as ErrorIcon } from '@mui/icons-material'

interface StatsCardProps {
  title: string
  value: string
  icon: React.ReactElement
  color?: 'primary' | 'secondary' | 'success' | 'error' | 'warning' | 'info'
  unit?: string
  subtitle?: string
  trend?: 'up' | 'down' | 'neutral'
  trendValue?: string
  loading?: boolean
  error?: string
  onClick?: () => void
  formatValue?: boolean
  variant?: 'default' | 'compact' | 'expanded'
  description?: string
  ariaLabel?: string
  responsive?: boolean
}

const StatsCard: React.FC<StatsCardProps> = ({
  title,
  value,
  icon,
  color = 'primary',
  unit,
  subtitle,
  trend,
  trendValue,
  loading = false,
  error,
  onClick,
  formatValue = false,
  variant = 'default',
  description,
  ariaLabel,
  responsive = false,
}) => {
  const theme = useTheme()

  const formatNumber = (num: string): string => {
    if (!formatValue) return num
    const number = parseFloat(num)
    if (isNaN(number)) return num
    return number.toLocaleString('en-US')
  }

  const getTrendIcon = () => {
    if (trend === 'up') return <TrendingUp fontSize="small" color="success" />
    if (trend === 'down') return <TrendingDown fontSize="small" color="error" />
    return null
  }

  const getCardClassName = () => {
    const classes = ['MuiCard-root']
    if (variant === 'compact') classes.push('MuiCard-compact')
    if (variant === 'expanded') classes.push('MuiCard-expanded')
    return classes.join(' ')
  }

  if (loading) {
    return (
      <Card
        data-testid="stats-card-skeleton"
        aria-busy="true"
        sx={{
          width: responsive ? '100%' : 'auto',
          height: variant === 'compact' ? 120 : variant === 'expanded' ? 200 : 160,
        }}
      >
        <CardContent>
          <Skeleton variant="circular" width={40} height={40} />
          <Skeleton variant="text" sx={{ mt: 2 }} />
          <Skeleton variant="text" width="60%" />
        </CardContent>
      </Card>
    )
  }

  const displayValue = error ? '--' : `${formatNumber(value)}${unit ? (unit === '%' || unit.startsWith(' ') ? unit : ` ${unit}`) : ''}`

  return (
    <Card
      data-testid="stats-card"
      className={getCardClassName()}
      aria-label={ariaLabel}
      onClick={onClick}
      sx={{
        cursor: onClick ? 'pointer' : 'default',
        width: responsive ? '100%' : 'auto',
        height: variant === 'compact' ? 120 : variant === 'expanded' ? 200 : 'auto',
        transition: 'all 0.3s ease',
        '&:hover': onClick ? {
          transform: 'translateY(-2px)',
          boxShadow: theme.shadows[4],
          backgroundColor: alpha(theme.palette[color].main, 0.04),
        } : {},
      }}
    >
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <Box sx={{ flex: 1 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <Box
                sx={{
                  p: 1,
                  borderRadius: 1,
                  backgroundColor: alpha(theme.palette[color].main, 0.1),
                  color: theme.palette[color].main,
                  display: 'flex',
                  alignItems: 'center',
                  mr: 2,
                }}
              >
                {error ? <ErrorIcon data-testid="error-icon" /> : React.cloneElement(icon, { color: 'inherit' })}
              </Box>
              {trend && trendValue && (
                <Box
                  data-testid="trend-indicator"
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 0.5,
                    color: trend === 'up' ? 'success.main' : trend === 'down' ? 'error.main' : 'text.secondary',
                  }}
                >
                  {getTrendIcon()}
                  <Typography variant="caption" fontWeight="medium">
                    {trendValue}
                  </Typography>
                </Box>
              )}
            </Box>

            <Typography
              variant="subtitle2"
              color="text.secondary"
              gutterBottom
              component="h3"
            >
              {title}
            </Typography>

            <Typography
              variant={variant === 'compact' ? 'h5' : 'h4'}
              fontWeight="bold"
              sx={{ mb: subtitle ? 0.5 : 0 }}
            >
              {displayValue}
            </Typography>

            {subtitle && (
              <Typography variant="caption" color="text.secondary">
                {subtitle}
              </Typography>
            )}

            {variant === 'expanded' && description && (
              <Typography
                variant="body2"
                color="text.secondary"
                sx={{ mt: 2 }}
              >
                {description}
              </Typography>
            )}
          </Box>
        </Box>
      </CardContent>
    </Card>
  )
}

export default StatsCard