/**
 * Grid Component Wrapper
 * 
 * This wrapper re-exports MUI's Unstable_Grid2 as the default Grid component
 * to maintain the Grid2 API while fixing TypeScript compilation errors.
 * 
 * The codebase was migrated from stable Grid v1 to use Grid2 syntax,
 * but the import was left as stable Grid, causing type mismatches.
 * 
 * Grid2 API differences:
 * - No need for `container` and `item` props (auto-detected)
 * - Uses `xs`, `sm`, `md` etc. directly as props
 * - Better responsive behavior and simpler API
 */

import Grid2, { Grid2Props } from '@mui/material/Unstable_Grid2'
import type { ReactNode } from 'react'

// Explicitly define size props to fix TypeScript issues
interface SizeProps {
  xs?: number | 'auto'
  sm?: number | 'auto'
  md?: number | 'auto'
  lg?: number | 'auto'
  xl?: number | 'auto'
}

interface BaseProps extends Omit<Grid2Props, 'component'>, SizeProps {
  children?: ReactNode
  component?: Grid2Props['component']
  'data-testid'?: string
  key?: string | number
  container?: boolean
  spacing?: number | string
  size?: number | 'auto'
  offset?: number | 'auto'
}

const Grid = (props: BaseProps) => {
  return <Grid2 {...props} />
}

// Export our wrapped version as default
export default Grid

// Also export the original Grid2 as a named export in case it's needed directly
export { default as Grid2 } from '@mui/material/Unstable_Grid2'

// Re-export the Grid2Props type for proper typing
export type { Grid2Props as GridProps } from '@mui/material/Unstable_Grid2'
