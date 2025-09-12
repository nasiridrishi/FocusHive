// ***********************************************************
// This file is processed and loaded automatically before your component tests.
// ***********************************************************

import './commands'
import '@testing-library/cypress/add-commands'

// Import global styles or setup that should be available for component testing
import '../../src/index.css'


// Example: Component testing setup for Material UI theme provider
// You can wrap components with providers here
// import { ThemeProvider } from '@mui/material/styles'
// import { theme } from '../../src/theme'
// 
// Cypress.Commands.overwrite('mount', (originalFn, component, options = {}) => {
//   const wrapped = (
//     <ThemeProvider theme={theme}>
//       {component}
//     </ThemeProvider>
//   )
//   return originalFn(wrapped, options)
// })