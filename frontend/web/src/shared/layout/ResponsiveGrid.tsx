/**
 * Advanced Responsive Grid System
 * 
 * Enhanced grid component with CSS Grid support, container queries,
 * and intelligent auto-sizing based on content and screen size
 */

import React, { forwardRef } from 'react'
import { Box, BoxProps } from '@mui/material'
import { styled } from '@mui/material/styles'
import { useContainerQuery, useResponsive } from '../hooks'
import { type BreakpointKey, type ContainerBreakpointKey } from '../theme'

// Base grid container with CSS Grid support
const GridContainer = styled(Box, {
  shouldForwardProp: (prop) => 
    !['columns', 'gap', 'autoFit', 'minItemWidth', 'maxItemWidth', 'aspectRatio'].includes(prop as string),
})<{
  columns?: number | Partial<Record<BreakpointKey, number>>
  gap?: number | Partial<Record<BreakpointKey, number>>
  autoFit?: boolean
  minItemWidth?: number
  maxItemWidth?: number
  aspectRatio?: string
}>(({ theme, columns = 1, gap = 2, autoFit, minItemWidth, maxItemWidth, aspectRatio }) => {
  const getResponsiveValue = (value: number | Partial<Record<BreakpointKey, number>>, defaultValue: number) => {
    if (typeof value === 'number') return value
    
    // Create responsive CSS for the value
    const breakpointKeys = Object.keys(theme.breakpoints.values) as BreakpointKey[]
    let css = {}
    
    breakpointKeys.forEach((breakpoint) => {
      if (value[breakpoint] !== undefined) {
        css = {
          ...css,
          [theme.breakpoints.up(breakpoint)]: {
            '--grid-value': value[breakpoint],
          },
        }
      }
    })
    
    return css
  }
  
  const baseColumns = typeof columns === 'number' ? columns : columns.mobile || 1
  const baseGap = typeof gap === 'number' ? gap : gap.mobile || 2
  
  return {
    display: 'grid',
    gap: theme.spacing(baseGap),
    width: '100%',
    
    // Auto-fit columns based on minimum item width
    ...(autoFit && minItemWidth && {
      gridTemplateColumns: `repeat(auto-fit, minmax(${minItemWidth}px, ${maxItemWidth ? `${maxItemWidth}px` : '1fr'}))`,
    }),
    
    // Fixed column layout
    ...(!autoFit && {
      gridTemplateColumns: `repeat(${baseColumns}, 1fr)`,
    }),
    
    // Aspect ratio for grid items
    ...(aspectRatio && {
      '& > *': {
        aspectRatio,
      },
    }),
    
    // Responsive columns
    ...(typeof columns === 'object' && getResponsiveValue(columns, baseColumns)),
    
    // Responsive gap
    ...(typeof gap === 'object' && {
      ...getResponsiveValue(gap, baseGap),
      gap: 'var(--grid-gap, ' + theme.spacing(baseGap) + ')',
    }),
  }
})

// Grid item component
const GridItem = styled(Box, {
  shouldForwardProp: (prop) => 
    !['span', 'spanRow', 'order', 'align', 'justify'].includes(prop as string),
})<{
  span?: number | Partial<Record<BreakpointKey, number>>
  spanRow?: number | Partial<Record<BreakpointKey, number>>
  order?: number | Partial<Record<BreakpointKey, number>>
  align?: 'start' | 'center' | 'end' | 'stretch'
  justify?: 'start' | 'center' | 'end' | 'stretch'
}>(({ theme, span, spanRow, order, align, justify }) => ({
  // Column span
  ...(typeof span === 'number' && {
    gridColumn: `span ${span}`,
  }),
  
  // Row span
  ...(typeof spanRow === 'number' && {
    gridRow: `span ${spanRow}`,
  }),
  
  // Order
  ...(typeof order === 'number' && {
    order,
  }),
  
  // Alignment
  ...(align && {
    alignSelf: align,
  }),
  
  ...(justify && {
    justifySelf: justify,
  }),
  
  // Responsive span, order, etc. would be implemented here
  // using similar pattern as GridContainer
}))

// Props interfaces
interface ResponsiveGridProps extends Omit<BoxProps, 'columns'> {
  // Grid configuration
  columns?: number | Partial<Record<BreakpointKey, number>>
  gap?: number | Partial<Record<BreakpointKey, number>>
  
  // Auto-sizing options
  autoFit?: boolean
  minItemWidth?: number
  maxItemWidth?: number
  
  // Visual options
  aspectRatio?: string
  
  // Container queries
  useContainerQueries?: boolean
  
  children: React.ReactNode
}

interface GridItemProps extends BoxProps {
  span?: number | Partial<Record<BreakpointKey, number>>
  spanRow?: number | Partial<Record<BreakpointKey, number>>
  order?: number | Partial<Record<BreakpointKey, number>>
  align?: 'start' | 'center' | 'end' | 'stretch'
  justify?: 'start' | 'center' | 'end' | 'stretch'
}

// Main ResponsiveGrid component
export const ResponsiveGrid = forwardRef<HTMLDivElement, ResponsiveGridProps>(
  ({ 
    columns = { mobile: 1, tablet: 2, desktop: 3 },
    gap = { mobile: 2, tablet: 3, desktop: 4 },
    autoFit = false,
    minItemWidth,
    maxItemWidth,
    aspectRatio,
    useContainerQueries = false,
    children,
    ...props 
  }, ref) => {
    const { currentBreakpoint } = useResponsive()
    const containerQuery = useContainerQuery()
    
    // Use container queries if enabled
    const activeColumns = useContainerQueries 
      ? containerQuery.responsiveValue({
          xs: 1,
          sm: 2,
          md: 3,
          lg: 4,
          xl: 5,
          xxl: 6,
        }, 1)
      : (typeof columns === 'number' ? columns : columns[currentBreakpoint] || 1)
    
    const activeGap = useContainerQueries
      ? containerQuery.responsiveValue({
          xs: 1,
          sm: 2,
          md: 3,
        }, 2)
      : (typeof gap === 'number' ? gap : gap[currentBreakpoint] || 2)
    
    return (
      <GridContainer
        ref={useContainerQueries ? containerQuery.containerRef : ref}
        columns={activeColumns}
        gap={activeGap}
        autoFit={autoFit}
        minItemWidth={minItemWidth}
        maxItemWidth={maxItemWidth}
        aspectRatio={aspectRatio}
        {...props}
      >
        {children}
      </GridContainer>
    )
  }
)

ResponsiveGrid.displayName = 'ResponsiveGrid'

// Grid item component
export const GridItem = forwardRef<HTMLDivElement, GridItemProps>(
  ({ span, spanRow, order, align, justify, children, ...props }, ref) => {
    return (
      <GridItem
        ref={ref}
        span={span}
        spanRow={spanRow}
        order={order}
        align={align}
        justify={justify}
        {...props}
      >
        {children}
      </GridItem>
    )
  }
)

GridItem.displayName = 'GridItem'

// Masonry Grid component for Pinterest-style layouts
export const MasonryGrid: React.FC<{
  columns?: number | Partial<Record<BreakpointKey, number>>
  gap?: number
  children: React.ReactNode
}> = ({ columns = { mobile: 1, tablet: 2, desktop: 3 }, gap = 16, children }) => {
  const { currentBreakpoint } = useResponsive()
  
  const activeColumns = typeof columns === 'number' ? columns : columns[currentBreakpoint] || 1
  
  return (
    <Box
      sx={{
        columnCount: activeColumns,
        columnGap: `${gap}px`,
        '& > *': {
          breakInside: 'avoid',
          marginBottom: `${gap}px`,
          display: 'inline-block',
          width: '100%',
        },
      }}
    >
      {children}
    </Box>
  )
}

// Responsive Stack component (enhanced version of MUI Stack)
export const ResponsiveStack: React.FC<{
  direction?: 'row' | 'column' | Partial<Record<BreakpointKey, 'row' | 'column'>>
  spacing?: number | Partial<Record<BreakpointKey, number>>
  align?: 'start' | 'center' | 'end' | 'stretch'
  justify?: 'start' | 'center' | 'end' | 'space-between' | 'space-around' | 'space-evenly'
  wrap?: boolean
  children: React.ReactNode
} & BoxProps> = ({ 
  direction = 'column',
  spacing = 2,
  align = 'stretch',
  justify = 'start',
  wrap = false,
  children,
  ...props 
}) => {
  const { currentBreakpoint } = useResponsive()
  
  const activeDirection = typeof direction === 'string' ? direction : direction[currentBreakpoint] || 'column'
  const activeSpacing = typeof spacing === 'number' ? spacing : spacing[currentBreakpoint] || 2
  
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: activeDirection,
        alignItems: align === 'start' ? 'flex-start' : align === 'end' ? 'flex-end' : align,
        justifyContent: justify === 'start' ? 'flex-start' : justify === 'end' ? 'flex-end' : justify,
        gap: (theme) => theme.spacing(activeSpacing),
        flexWrap: wrap ? 'wrap' : 'nowrap',
      }}
      {...props}
    >
      {children}
    </Box>
  )
}

// Container component with responsive max-widths
export const ResponsiveContainer: React.FC<{
  maxWidth?: BreakpointKey | false
  padding?: number | Partial<Record<BreakpointKey, number>>
  centerContent?: boolean
  children: React.ReactNode
} & BoxProps> = ({ 
  maxWidth = 'desktop',
  padding = { mobile: 2, tablet: 3, desktop: 4 },
  centerContent = true,
  children,
  ...props 
}) => {
  const { currentBreakpoint } = useResponsive()
  
  const activePadding = typeof padding === 'number' ? padding : padding[currentBreakpoint] || 2
  
  return (
    <Box
      sx={{
        width: '100%',
        ...(maxWidth && {
          maxWidth: (theme) => theme.breakpoints.values[maxWidth],
        }),
        ...(centerContent && {
          marginX: 'auto',
        }),
        paddingX: (theme) => theme.spacing(activePadding),
      }}
      {...props}
    >
      {children}
    </Box>
  )
}

// Export all grid components
export { GridContainer, GridItem as StyledGridItem }