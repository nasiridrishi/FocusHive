describe('FocusHive App - Smoke Tests', () => {
  beforeEach(() => {
    // Visit the app before each test
    cy.visit('/')
  })

  it('should load the application successfully', () => {
    // Check that the app loads without errors
    cy.get('body').should('be.visible')
    
    // Check for the app title or main element
    cy.title().should('not.be.empty')
    
    // Verify no console errors occurred
    cy.window().then((win) => {
      expect(win.console.error).to.not.have.been.called
    })
  })

  it('should display the landing page', () => {
    // Check for key landing page elements
    cy.get('[data-testid="app-root"]').should('exist')
    
    // Check that navigation or main content is present
    cy.get('main, nav, header').should('have.length.greaterThan', 0)
  })

  it('should be responsive', () => {
    // Test different viewport sizes
    const viewports = [
      { width: 320, height: 568 }, // Mobile
      { width: 768, height: 1024 }, // Tablet
      { width: 1920, height: 1080 } // Desktop
    ]

    viewports.forEach((viewport) => {
      cy.viewport(viewport.width, viewport.height)
      cy.get('body').should('be.visible')
      
      // Ensure content doesn't overflow
      cy.get('body').should('not.have.css', 'overflow-x', 'scroll')
    })
  })

  it('should handle network failures gracefully', () => {
    // Mock a network failure
    cy.intercept('GET', '/api/**', { forceNetworkError: true }).as('networkFailure')
    
    // App should still be functional
    cy.visit('/')
    cy.get('body').should('be.visible')
    
    // Should show some kind of error state or offline indicator
    // (This depends on your app's error handling implementation)
  })

  context('Accessibility', () => {
    it('should pass basic accessibility checks', () => {
      cy.checkA11y()
    })

    it('should be keyboard navigable', () => {
      // Test tab navigation
      cy.get('body').trigger('keydown', { key: 'Tab' })
      cy.focused().should('be.visible')
      
      // Continue tabbing to ensure focus management works  
      cy.focused().trigger('keydown', { key: 'Tab' })
      cy.focused().should('exist')
    })
  })

  context('Performance', () => {
    it('should load within acceptable time', () => {
      const startTime = Date.now()
      
      cy.visit('/').then(() => {
        const loadTime = Date.now() - startTime
        expect(loadTime).to.be.lessThan(5000) // 5 seconds max
      })
    })

    it('should not have memory leaks', () => {
      // Basic memory leak detection
      cy.window().then((win) => {
        const initialMemory = (win.performance as unknown as { memory?: { usedJSHeapSize: number } }).memory?.usedJSHeapSize
        
        if (initialMemory) {
          // Perform some actions that might cause leaks
          cy.reload()
          
          cy.window().then((newWin) => {
            const finalMemory = (newWin.performance as unknown as { memory?: { usedJSHeapSize: number } }).memory?.usedJSHeapSize
            
            if (finalMemory && initialMemory) {
              // Allow for some memory increase but flag excessive growth
              const memoryIncrease = finalMemory - initialMemory
              expect(memoryIncrease).to.be.lessThan(50 * 1024 * 1024) // 50MB threshold
            }
          })
        }
      })
    })
  })
})