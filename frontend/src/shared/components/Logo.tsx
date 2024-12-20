import React from 'react'
import { Box, SxProps, Theme } from '@mui/material'

interface LogoProps {
  /** Width of the logo */
  width?: number | string
  /** Height of the logo */
  height?: number | string
  /** Whether to use the full logo with text or just the icon */
  variant?: 'full' | 'icon'
  /** Additional MUI sx props */
  sx?: SxProps<Theme>
  /** Alt text for accessibility */
  alt?: string
}

/**
 * FocusHive Logo Component
 * 
 * Displays the FocusHive logo in different variants.
 * Uses SVG for better scalability and performance.
 */
export const Logo: React.FC<LogoProps> = ({
  width = 'auto',
  height = 40,
  variant = 'full',
  sx = {},
  alt = 'FocusHive'
}) => {
  // Logo paths - using the actual exported logos
  const logoSrc = variant === 'full' 
    ? '/logo/logo.svg'  // Full logo with text
    : '/logo/icon.svg'  // Just the icon

  const pngFallback = variant === 'full'
    ? '/logo/logo.png'
    : '/logo/icon.png'

  return (
    <Box
      component="img"
      src={logoSrc}
      alt={alt}
      sx={{
        width,
        height,
        objectFit: 'contain',
        display: 'block',
        ...sx
      }}
      onError={(e: React.SyntheticEvent<HTMLImageElement>) => {
        // Fallback to PNG if SVG fails
        const img = e.target as HTMLImageElement
        if (img.src !== pngFallback) {
          img.src = pngFallback
        }
      }}
    />
  )
}

/**
 * Logo with link wrapper for navigation
 */
export const LogoLink: React.FC<LogoProps & { href?: string; onClick?: () => void }> = ({
  href = '/',
  onClick,
  ...logoProps
}) => {
  const handleClick = (e: React.MouseEvent) => {
    if (onClick) {
      e.preventDefault()
      onClick()
    }
  }

  return (
    <Box
      component="a"
      href={href}
      onClick={handleClick}
      sx={{
        display: 'inline-flex',
        textDecoration: 'none',
        cursor: 'pointer',
        '&:hover': {
          opacity: 0.8
        }
      }}
    >
      <Logo {...logoProps} />
    </Box>
  )
}

export default Logo