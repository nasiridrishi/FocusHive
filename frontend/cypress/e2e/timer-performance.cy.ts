describe('Timer Performance Tests', () => {
  beforeEach(() => {
    cy.fixture('users').then((users) => {
      cy.setupAuthState(users.validUser)
    })
    
    // Setup timer with default settings
    cy.setupTimerSession('activeSession')
    cy.visit('/timer')
  })

  describe('Timer Rendering Performance', () => {
    it('should render timer components within performance budget', () => {
      // Measure initial load performance
      cy.window().then((win) => {
        const startTime = win.performance.now()
        
        cy.get('[data-cy=timer-container]').should('be.visible').then(() => {
          const endTime = win.performance.now()
          const loadTime = endTime - startTime
          
          // Timer should load within 100ms
          expect(loadTime).to.be.lessThan(100)
        })
      })
    })

    it('should maintain 60fps during timer countdown', () => {
      let frameCount = 0
      let startTime: number
      
      cy.window().then((win) => {
        // Mock requestAnimationFrame to count frames
        const originalRAF = win.requestAnimationFrame
        win.requestAnimationFrame = (callback) => {
          frameCount++
          return originalRAF(callback)
        }
        
        startTime = win.performance.now()
      })

      cy.startTimerSession('focus')
      
      // Let timer run for 2 seconds
      cy.wait(2000)
      
      cy.window().then((win) => {
        const endTime = win.performance.now()
        const duration = (endTime - startTime) / 1000 // Convert to seconds
        const fps = frameCount / duration
        
        // Should maintain at least 50fps (allowing some margin from 60fps)
        expect(fps).to.be.greaterThan(50)
      })
    })

    it('should handle rapid timer state changes efficiently', () => {
      const startTime = Date.now()
      
      // Rapidly start and stop timer multiple times
      for (let i = 0; i < 5; i++) {
        cy.get('[data-cy=main-action-button]').click()
        cy.waitForStableElement('[data-cy=stop-button]')
        cy.get('[data-cy=stop-button]').click()
        cy.waitForStableElement('[data-cy=main-action-button]')
      }
      
      cy.then(() => {
        const endTime = Date.now()
        const totalTime = endTime - startTime
        
        // All operations should complete within 1 second
        expect(totalTime).to.be.lessThan(1000)
      })
    })
  })

  describe('Memory Usage', () => {
    it('should not leak memory during long timer sessions', () => {
      let initialMemory: number
      let finalMemory: number
      
      cy.window().then((win) => {
        // Get initial memory usage
        if ('memory' in win.performance) {
          initialMemory = (win.performance as Performance & {
            memory?: { usedJSHeapSize: number }
          }).memory?.usedJSHeapSize || 0
        }
      })

      // Start timer and simulate running for extended period
      cy.startTimerSession('focus')
      
      // Simulate timer updates for 30 seconds (fast-forwarded)
      for (let i = 0; i < 30; i++) {
        cy.mockTimerWebSocket({
          type: 'TIMER_SYNC',
          timerState: {
            currentPhase: 'focus',
            timeRemaining: 1500 - (i * 50),
            isRunning: true,
            currentCycle: 1
          }
        })
        cy.wait(10) // Small delay between updates
      }
      
      cy.window().then((win) => {
        // Get final memory usage
        if ('memory' in win.performance) {
          finalMemory = (win.performance as Performance & {
            memory?: { usedJSHeapSize: number }
          }).memory?.usedJSHeapSize || 0
          
          // Memory increase should be less than 5MB
          const memoryIncrease = (finalMemory - initialMemory) / (1024 * 1024)
          expect(memoryIncrease).to.be.lessThan(5)
        }
      })
    })
  })

  describe('WebSocket Performance', () => {
    it('should handle high-frequency WebSocket messages efficiently', () => {
      cy.startTimerSession('focus')
      
      const startTime = Date.now()
      const messageCount = 100
      
      // Send rapid WebSocket messages
      for (let i = 0; i < messageCount; i++) {
        cy.mockTimerWebSocket({
          type: 'PRESENCE_UPDATE',
          userId: `user-${i}`,
          status: 'focusing',
          timestamp: Date.now()
        })
      }
      
      // Verify UI remains responsive
      cy.get('[data-cy=main-action-button]').should('be.visible')
      cy.get('[data-cy=timer-display]').should('be.visible')
      
      cy.then(() => {
        const endTime = Date.now()
        const processingTime = endTime - startTime
        
        // All messages should be processed within 500ms
        expect(processingTime).to.be.lessThan(500)
      })
    })

    it('should recover quickly from WebSocket reconnection', () => {
      cy.startTimerSession('focus')
      
      // Simulate WebSocket disconnection
      cy.window().then((win) => {
        ;(win as Window & { mockStompClient?: { connected: boolean } }).mockStompClient = {
          connected: false
        }
      })
      
      const reconnectStartTime = Date.now()
      
      // Verify disconnection indicator appears
      cy.get('[data-cy=connection-status]').should('contain', 'Reconnecting')
      
      // Simulate reconnection
      cy.window().then((win) => {
        ;(win as Window & { mockStompClient?: { connected: boolean } }).mockStompClient = {
          connected: true
        }
      })
      
      // Verify reconnection indicator disappears
      cy.get('[data-cy=connection-status]').should('not.exist')
      
      cy.then(() => {
        const reconnectTime = Date.now() - reconnectStartTime
        
        // Reconnection should be detected within 1 second
        expect(reconnectTime).to.be.lessThan(1000)
      })
    })
  })

  describe('Large Dataset Handling', () => {
    it('should efficiently render many hive members with timer data', () => {
      // Mock large member dataset
      const memberCount = 50
      const members = Array.from({ length: memberCount }, (_, i) => ({
        userId: `user-${i}`,
        userName: `User ${i}`,
        status: i % 3 === 0 ? 'focusing' : i % 3 === 1 ? 'on-break' : 'idle',
        phase: i % 3 === 0 ? 'focus' : i % 3 === 1 ? 'short-break' : 'idle',
        timeRemaining: Math.floor(Math.random() * 1500),
        currentCycle: Math.floor(Math.random() * 4) + 1,
        isOnline: Math.random() > 0.3
      }))
      
      cy.mockApiCall('GET', '**/api/hives/*/members/activity', { body: members })
      
      const startTime = Date.now()
      
      cy.startTimerSession('focus', 'test-hive-456')
      cy.get('[data-cy=member-activity]').should('be.visible')
      
      // Verify all members render within reasonable time
      cy.get('[data-cy=member-status]').should('have.length', memberCount)
      
      cy.then(() => {
        const renderTime = Date.now() - startTime
        
        // Should render all members within 2 seconds
        expect(renderTime).to.be.lessThan(2000)
      })
    })

    it('should handle many session goals efficiently', () => {
      // Create session with many goals
      const goalCount = 25
      const goals = Array.from({ length: goalCount }, (_, i) => ({
        id: `goal-${i}`,
        description: `Goal ${i}: Complete task number ${i}`,
        isCompleted: Math.random() > 0.5,
        priority: i % 3 === 0 ? 'high' : i % 3 === 1 ? 'medium' : 'low',
        createdAt: new Date().toISOString()
      }))
      
      cy.fixture('timer').then((timerData) => {
        const sessionWithManyGoals = {
          ...timerData.activeSession,
          goals
        }
        
        cy.mockApiCall('GET', '**/api/timer/sessions/current', {
          body: sessionWithManyGoals
        })
      })
      
      cy.startTimerSession('focus')
      cy.get('[data-cy=goals-chip]').click()
      
      // Verify goals section renders efficiently
      const startTime = Date.now()
      cy.get('[data-cy=goal-chip]').should('have.length', goalCount)
      
      cy.then(() => {
        const renderTime = Date.now() - startTime
        
        // Should render all goals within 1 second
        expect(renderTime).to.be.lessThan(1000)
      })
    })
  })

  describe('Resource Optimization', () => {
    it('should minimize network requests during timer operation', () => {
      let requestCount = 0
      
      // Track all network requests
      cy.intercept('**', (req) => {
        requestCount++
        req.continue()
      }).as('allRequests')
      
      cy.startTimerSession('focus')
      
      // Record distraction multiple times
      cy.get('[data-cy=distractions-chip]').click()
      cy.get('[data-cy=distractions-chip]').click()
      cy.get('[data-cy=distractions-chip]').click()
      
      // Should batch or minimize requests
      cy.then(() => {
        // Should make reasonable number of requests (not one per action)
        expect(requestCount).to.be.lessThan(10)
      })
    })

    it('should efficiently update timer display without unnecessary re-renders', () => {
      let renderCount = 0
      
      cy.window().then((win) => {
        // Mock to count component re-renders
        const originalSetInterval = win.setInterval
        win.setInterval = (callback, interval) => {
          if (interval === 1000) { // Timer update interval
            renderCount++
          }
          return originalSetInterval(callback, interval)
        }
      })
      
      cy.startTimerSession('focus')
      
      // Let timer run for a few seconds
      cy.wait(3000)
      
      cy.then(() => {
        // Should only update once per second, not more frequently
        expect(renderCount).to.be.lessThan(5)
      })
    })
  })

  describe('Stress Testing', () => {
    it('should handle rapid user interactions without breaking', () => {
      // Rapid start/stop/pause sequence
      for (let i = 0; i < 10; i++) {
        cy.get('[data-cy=main-action-button]').click()
        cy.get('[data-cy=main-action-button]').click() // pause
        cy.get('[data-cy=main-action-button]').click() // resume
        cy.get('[data-cy=stop-button]').click()
      }
      
      // Timer should still be functional
      cy.get('[data-cy=main-action-button]').should('be.visible')
      cy.get('[data-cy=timer-display]').should('contain', ':')
    })

    it('should maintain performance with concurrent timer instances', () => {
      // Simulate multiple timer instances (different browser tabs)
      const instanceCount = 5
      
      for (let i = 0; i < instanceCount; i++) {
        cy.mockTimerWebSocket({
          type: 'TIMER_INSTANCE',
          instanceId: `instance-${i}`,
          timerState: {
            currentPhase: 'focus',
            timeRemaining: 1500 - (i * 100),
            isRunning: true
          }
        })
      }
      
      // Main timer should still function normally
      cy.startTimerSession('focus')
      cy.get('[data-cy=timer-display]').should('be.visible')
      cy.get('[data-cy=main-action-button]').should('have.attr', 'title', 'Pause Timer')
    })
  })
})