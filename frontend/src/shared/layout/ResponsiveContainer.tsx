/**
 * Advanced Responsive Container System
 * 
 * Enhanced container components with fluid sizing, adaptive padding,
 * and intelligent content organization based on screen size and content type
 */

import React, { forwardRef } from 'react'
import { Box, BoxProps, Container, ContainerProps, useTheme } from '@mui/material'
import { styled } from '@mui/material/styles'
import { useResponsive, useContainerQuery, useDynamicViewportHeight } from '../hooks'
import { type BreakpointKey } from '../theme'

// Enhanced container with responsive behavior
const StyledContainer = styled(Container, {
  shouldForwardProp: (prop) => !['fluidPadding', 'adaptiveMaxWidth', 'contentType'].includes(prop as string),
})<{
  fluidPadding?: boolean
  adaptiveMaxWidth?: boolean
  contentType?: 'text' | 'media' | 'dashboard' | 'form'
}>(({ fluidPadding, adaptiveMaxWidth, contentType }) => ({
  // Fluid padding that scales with viewport
  ...(fluidPadding && {
    paddingLeft: 'clamp(16px, 4vw, 32px)',
    paddingRight: 'clamp(16px, 4vw, 32px)',
  }),
  
  // Adaptive max-width based on content type
  ...(adaptiveMaxWidth && {
    ...(contentType === 'text' && {
      maxWidth: '65ch', // Optimal reading line length
    }),
    ...(contentType === 'form' && {
      maxWidth: '480px',
    }),
    ...(contentType === 'dashboard' && {
      maxWidth: '1600px',
    }),
    ...(contentType === 'media' && {
      maxWidth: '100%',
    }),
  }),
}))

// Responsive section container
const SectionContainer = styled(Box)(({ theme }) => ({
  width: '100%',
  marginLeft: 'auto',
  marginRight: 'auto',
  // Responsive vertical spacing
  paddingTop: 'clamp(32px, 8vw, 80px)',
  paddingBottom: 'clamp(32px, 8vw, 80px)',
  
  // Responsive horizontal padding
  paddingLeft: 'clamp(16px, 4vw, 32px)',
  paddingRight: 'clamp(16px, 4vw, 32px)',
  
  [theme.breakpoints.up('tablet')]: {
    paddingTop: 'clamp(48px, 10vw, 120px)',
    paddingBottom: 'clamp(48px, 10vw, 120px)',
  },
}))

// Card container with adaptive sizing
const CardContainer = styled(Box)(({ theme }) => ({
  backgroundColor: theme.palette.background.paper,
  borderRadius: theme.shape.borderRadius,
  boxShadow: theme.shadows[1],
  padding: 'clamp(16px, 4vw, 24px)',
  transition: 'all 0.3s ease-in-out',
  
  '&:hover': {
    boxShadow: theme.shadows[4],
    transform: 'translateY(-2px)',
  },
  
  // Responsive padding
  [theme.breakpoints.up('tablet')]: {
    padding: 'clamp(20px, 5vw, 32px)',
  },
}))

// Full viewport container
const ViewportContainer = styled(Box)(() => ({
  width: '100vw',
  // Use CSS custom property for dynamic viewport height with fallback
  minHeight: 'calc(var(--vh, 1vh) * 100)',
  display: 'flex',
  flexDirection: 'column',
}))

// Props interfaces
interface ResponsiveContainerProps extends Omit<ContainerProps, 'maxWidth'> {
  maxWidth?: BreakpointKey | number | false
  fluidPadding?: boolean
  adaptiveMaxWidth?: boolean
  contentType?: 'text' | 'media' | 'dashboard' | 'form'
  centerContent?: boolean
}

interface FlexContainerProps extends BoxProps {
  direction?: 'row' | 'column' | Partial<Record<BreakpointKey, 'row' | 'column'>>
  align?: 'start' | 'center' | 'end' | 'stretch' | 'baseline'
  justify?: 'start' | 'center' | 'end' | 'space-between' | 'space-around' | 'space-evenly'
  gap?: number | Partial<Record<BreakpointKey, number>>
  wrap?: boolean
}

interface GridContainerProps extends BoxProps {
  columns?: number | Partial<Record<BreakpointKey, number>>
  gap?: number | Partial<Record<BreakpointKey, number>>
  autoFit?: boolean
  minItemWidth?: number
  maxItemWidth?: number
}

// Main ResponsiveContainer component
export const ResponsiveContainer = forwardRef<HTMLDivElement, ResponsiveContainerProps>(
  ({ 
    maxWidth = 'desktop',
    fluidPadding = false,
    adaptiveMaxWidth = false,
    contentType = 'dashboard',
    centerContent = true,
    children,
    ...props 
  }, ref) => {
    const theme = useTheme()
    
    // Convert BreakpointKey to pixel value
    const getMaxWidth = () => {
      if (maxWidth === false) return false
      if (typeof maxWidth === 'number') return maxWidth
      return theme.breakpoints.values[maxWidth]
    }
    
    return (
      <StyledContainer
        ref={ref}
        maxWidth={false} // We handle maxWidth manually
        fluidPadding={fluidPadding}
        adaptiveMaxWidth={adaptiveMaxWidth}
        contentType={contentType}
        style={{
          ...(maxWidth !== false && { maxWidth: typeof getMaxWidth() === 'number' ? `${getMaxWidth()}px` : String(getMaxWidth()) }),
          ...(centerContent && { margin: '0 auto' }),
        }}
        {...props}
      >
        {children}
      </StyledContainer>
    )
  }
)

ResponsiveContainer.displayName = 'ResponsiveContainer'

// Flex container with responsive direction and spacing
export const FlexContainer = forwardRef<HTMLDivElement, FlexContainerProps>(
  ({ 
    direction = 'row',
    align = 'stretch',
    justify = 'start',
    gap = 2,
    wrap = false,
    children,
    ...props 
  }, ref) => {
    const { currentBreakpoint } = useResponsive()
    const theme = useTheme()
    
    const activeDirection = typeof direction === 'string' ? direction : direction[currentBreakpoint] || 'row'
    const activeGap = typeof gap === 'number' ? gap : gap[currentBreakpoint] || 2
    
    return (
      <Box
        ref={ref}
        sx={{
          display: 'flex',
          flexDirection: activeDirection,
          alignItems: align === 'start' ? 'flex-start' : align === 'end' ? 'flex-end' : align,
          justifyContent: justify === 'start' ? 'flex-start' : justify === 'end' ? 'flex-end' : justify,
          gap: theme.spacing(activeGap),
          flexWrap: wrap ? 'wrap' : 'nowrap',
        }}
        {...props}
      >
        {children}
      </Box>
    )
  }
)

FlexContainer.displayName = 'FlexContainer'

// CSS Grid container with responsive configuration
export const GridContainer = forwardRef<HTMLDivElement, GridContainerProps>(
  ({ 
    columns = { mobile: 1, tablet: 2, desktop: 3 },
    gap = { mobile: 2, tablet: 3, desktop: 4 },
    autoFit = false,
    minItemWidth = 250,
    maxItemWidth,
    children,
    ...props 
  }, ref) => {
    const { currentBreakpoint } = useResponsive()
    const theme = useTheme()
    
    const activeColumns = typeof columns === 'number' ? columns : columns[currentBreakpoint] || 1
    const activeGap = typeof gap === 'number' ? gap : gap[currentBreakpoint] || 2
    
    return (
      <Box
        ref={ref}
        sx={{
          display: 'grid',
          gap: theme.spacing(activeGap),
          // Auto-fit columns based on minimum item width
          ...(autoFit && {
            gridTemplateColumns: `repeat(auto-fit, minmax(${minItemWidth}px, ${maxItemWidth ? `${maxItemWidth}px` : '1fr'}))`,
          }),
          // Fixed column layout
          ...(!autoFit && {
            gridTemplateColumns: `repeat(${activeColumns}, 1fr)`,
          }),
        }}
        {...props}
      >
        {children}
      </Box>
    )
  }
)

GridContainer.displayName = 'GridContainer'

// Section container for page sections
export const Section: React.FC<{
  as?: React.ElementType
  background?: 'default' | 'paper' | 'primary' | 'secondary'
  spacing?: 'small' | 'medium' | 'large'
  children: React.ReactNode
} & BoxProps> = ({ 
  as = 'section',
  background = 'default',
  spacing = 'medium',
  children,
  ...props 
}) => {
  const theme = useTheme()
  
  const spacingMap = {
    small: { py: { mobile: 4, tablet: 6, desktop: 8 } },
    medium: { py: { mobile: 6, tablet: 8, desktop: 12 } },
    large: { py: { mobile: 8, tablet: 12, desktop: 16 } },
  }
  
  const backgroundMap = {
    default: theme.palette.background.default,
    paper: theme.palette.background.paper,
    primary: theme.palette.primary.main,
    secondary: theme.palette.secondary.main,
  }
  
  return (
    <Box
      component={as}
      sx={{
        width: '100%',
        backgroundColor: backgroundMap[background],
        ...(background === 'primary' || background === 'secondary' ? {
          color: theme.palette.getContrastText(backgroundMap[background]),
        } : {}),
        ...spacingMap[spacing],
      }}
      {...props}
    >
      <ResponsiveContainer>
        {children}
      </ResponsiveContainer>
    </Box>
  )
}

// Card component with responsive behavior
export const ResponsiveCard: React.FC<{
  variant?: 'elevated' | 'outlined' | 'filled'
  padding?: 'small' | 'medium' | 'large'
  clickable?: boolean
  children: React.ReactNode
} & BoxProps> = ({ 
  variant = 'elevated',
  padding = 'medium',
  clickable = false,
  children,
  ...props 
}) => {
  const theme = useTheme()
  
  const paddingMap = {
    small: { mobile: 2, tablet: 3, desktop: 3 },
    medium: { mobile: 3, tablet: 4, desktop: 4 },
    large: { mobile: 4, tablet: 5, desktop: 6 },
  }
  
  const variantStyles = {
    elevated: {
      backgroundColor: theme.palette.background.paper,
      boxShadow: theme.shadows[1],
      '&:hover': clickable ? {
        boxShadow: theme.shadows[4],
        transform: 'translateY(-2px)',
      } : {},
    },
    outlined: {
      backgroundColor: theme.palette.background.paper,
      border: `1px solid ${theme.palette.divider}`,
      '&:hover': clickable ? {
        borderColor: theme.palette.primary.main,
        boxShadow: `0 0 0 1px ${theme.palette.primary.main}`,
      } : {},
    },
    filled: {
      backgroundColor: theme.palette.action.hover,
      '&:hover': clickable ? {
        backgroundColor: theme.palette.action.selected,
      } : {},
    },
  }
  
  return (
    <CardContainer
      sx={{
        ...variantStyles[variant],
        padding: (theme) => {
          const paddingSize = typeof padding === 'string' && ['small', 'medium', 'large'].includes(padding) 
            ? paddingMap[padding as keyof typeof paddingMap]
            : paddingMap.medium;
          return {
            xs: theme.spacing(paddingSize.mobile),
            sm: theme.spacing(paddingSize.tablet),
            md: theme.spacing(paddingSize.desktop),
          };
        },
        cursor: clickable ? 'pointer' : 'default',
        transition: 'all 0.3s ease-in-out',
      }}
      {...props}
    >
      {children}
    </CardContainer>
  )
}

// Full viewport container with proper height handling
export const FullViewportContainer: React.FC<{
  children: React.ReactNode
} & BoxProps> = ({ children, ...props }) => {
  const { height: dynamicHeight } = useDynamicViewportHeight()
  
  return (
    <ViewportContainer
      sx={{
        minHeight: `${dynamicHeight}px`,
        '--vh': `${dynamicHeight / 100}px`,
      }}
      {...props}
    >
      {children}
    </ViewportContainer>
  )
}

// Container with query-based responsiveness
export const ContainerQueryBox: React.FC<{
  children: React.ReactNode
} & BoxProps> = ({ children, ...props }) => {
  const { containerRef, currentBreakpoint } = useContainerQuery()
  
  return (
    <Box
      ref={containerRef}
      sx={{
        containerType: 'inline-size',
        width: '100%',
        // Apply styles based on container queries
        '@container (min-width: 320px)': {
          padding: 1,
        },
        '@container (min-width: 480px)': {
          padding: 2,
        },
        '@container (min-width: 640px)': {
          padding: 3,
        },
      }}
      {...props}
    >
      <Box
        sx={{
          // Example of container-responsive styling
          fontSize: currentBreakpoint === 'xs' ? '0.875rem' : 
                   currentBreakpoint === 'sm' ? '1rem' : '1.125rem',
        }}
      >
        {children}
      </Box>
    </Box>
  )
}

// Export styled components for advanced usage
export { StyledContainer, SectionContainer, CardContainer, ViewportContainer }