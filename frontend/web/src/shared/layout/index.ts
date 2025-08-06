/**
 * Enhanced Layout System Exports
 * 
 * Central export point for all responsive layout components and utilities
 */

// Legacy layout components (for backward compatibility)
export { default as AppLayout } from './AppLayout'
export { default as Header } from './Header'
export { default as Sidebar } from './Sidebar'
export { default as NavigationDrawer } from './NavigationDrawer'

// New responsive layout system
export { ResponsiveLayout, PageLayout } from './ResponsiveLayout'
export { AdaptiveNavigation } from './AdaptiveNavigation'

// Grid and container components
export {
  ResponsiveGrid,
  GridItem,
  MasonryGrid,
  ResponsiveStack,
  ResponsiveContainer as GridResponsiveContainer,
  GridContainer,
  StyledGridItem,
} from './ResponsiveGrid'

// Container components
export {
  ResponsiveContainer,
  FlexContainer,
  GridContainer as ContainerGridContainer,
  Section,
  ResponsiveCard,
  FullViewportContainer,
  ContainerQueryBox,
  StyledContainer,
  SectionContainer,
  CardContainer,
  ViewportContainer,
} from './ResponsiveContainer'

