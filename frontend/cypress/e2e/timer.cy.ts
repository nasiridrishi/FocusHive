describe('Timer Session Workflow', () => {
  beforeEach(() => {
    // Setup authenticated user
    cy.fixture('users').then((users) => {
      cy.setupAuthState(users.validUser)
    })

    // Mock timer-related API endpoints
    cy.mockApiCall('GET', '**/api/timer/settings', {
      body: {
        focusLength: 25,
        shortBreakLength: 5,
        longBreakLength: 15,
        longBreakInterval: 4,
        autoStartBreaks: false,
        autoStartFocus: false,
        soundEnabled: true,
        notificationsEnabled: true,
        selectedSounds: {
          focusStart: 'bell',
          focusEnd: 'chime',
          breakStart: 'soft-bell',
          breakEnd: 'notification'
        }
      }
    })

    cy.mockApiCall('POST', '**/api/timer/sessions', {
      statusCode: 201,
      body: {
        id: 'session-123',
        userId: 'test-user-123',
        hiveId: 'test-hive-456',
        targetCycles: 4,
        completedCycles: 0,
        distractions: 0,
        goals: [],
        productivity: { rating: 0 },
        startTime: new Date().toISOString()
      }
    })

    cy.visit('/timer')
  })

  describe('Timer Setup and Configuration', () => {
    it('should display default timer settings', () => {
      cy.get('[data-cy=timer-container]').should('be.visible')
      cy.get('[data-cy=circular-timer]').should('be.visible')
      cy.get('[data-cy=timer-display]').should('contain', '25:00')
      cy.get('[data-cy=phase-chip]').should('contain', 'Ready to Focus')
    })

    it('should open settings menu and modify timer durations', () => {
      cy.get('[data-cy=settings-button]').click()
      cy.get('[data-cy=settings-menu]').should('be.visible')
      
      cy.get('[data-cy=advanced-settings-button]').click()
      
      // Mock settings update API
      cy.mockApiCall('PATCH', '**/api/timer/settings', {
        statusCode: 200,
        body: { message: 'Settings updated successfully' }
      })
      
      // Modify focus duration
      cy.get('[data-cy=focus-duration-input]').clear().type('30')
      cy.get('[data-cy=short-break-duration-input]').clear().type('10')
      cy.get('[data-cy=save-settings-button]').click()
      
      cy.get('[data-cy=success-notification]').should('be.visible')
        .and('contain', 'Settings updated successfully')
      
      // Verify timer display updates
      cy.get('[data-cy=timer-display]').should('contain', '30:00')
    })

    it('should toggle sound and notification settings', () => {
      cy.get('[data-cy=settings-button]').click()
      
      // Toggle sound
      cy.get('[data-cy=toggle-sound-button]').click()
      cy.get('[data-cy=sound-icon]').should('have.class', 'VolumeOff')
      
      // Toggle notifications
      cy.get('[data-cy=toggle-notifications-button]').click()
      
      cy.get('[data-cy=settings-menu]').should('not.exist')
    })

    it('should save user preferences persistently', () => {
      cy.get('[data-cy=settings-button]').click()
      cy.get('[data-cy=toggle-sound-button]').click()
      
      // Reload page and verify settings persist
      cy.reload()
      cy.waitForStableElement('[data-cy=timer-container]')
      
      cy.get('[data-cy=settings-button]').click()
      cy.get('[data-cy=sound-icon]').should('have.class', 'VolumeOff')
    })
  })

  describe('Focus Session Workflow', () => {
    it('should start a basic focus session', () => {
      cy.get('[data-cy=main-action-button]').should('be.visible')
        .and('have.attr', 'title', 'Start Focus Session')
      
      cy.get('[data-cy=main-action-button]').click()
      
      // Verify session started
      cy.get('[data-cy=timer-display]').should('not.contain', '25:00')
      cy.get('[data-cy=phase-chip]').should('contain', 'Focus Time')
      cy.get('[data-cy=session-alert]').should('be.visible')
        .and('contain', 'Focus session in progress')
      
      // Verify controls changed
      cy.get('[data-cy=main-action-button]').should('have.attr', 'title', 'Pause Timer')
      cy.get('[data-cy=stop-button]').should('be.visible')
      cy.get('[data-cy=skip-button]').should('be.visible')
    })

    it('should handle pause and resume functionality', () => {
      // Start session
      cy.get('[data-cy=main-action-button]').click()
      cy.waitForStableElement('[data-cy=stop-button]')
      
      // Get initial time
      cy.get('[data-cy=timer-display]').then(($display) => {
        const initialTime = $display.text()
        
        // Pause
        cy.get('[data-cy=main-action-button]').click()
        cy.get('[data-cy=main-action-button]').should('have.attr', 'title', 'Resume Timer')
        
        // Wait and verify time doesn't change
        cy.wait(2000)
        cy.get('[data-cy=timer-display]').should('contain', initialTime)
        
        // Resume
        cy.get('[data-cy=main-action-button]').click()
        cy.get('[data-cy=main-action-button]').should('have.attr', 'title', 'Pause Timer')
      })
    })

    it('should stop session and return to idle state', () => {
      // Start session
      cy.get('[data-cy=main-action-button]').click()
      cy.waitForStableElement('[data-cy=stop-button]')
      
      // Stop session
      cy.get('[data-cy=stop-button]').click()
      
      // Mock session end API
      cy.mockApiCall('POST', '**/api/timer/sessions/*/end', {
        statusCode: 200,
        body: { message: 'Session ended successfully' }
      })
      
      // Verify returned to idle state
      cy.get('[data-cy=timer-display]').should('contain', '25:00')
      cy.get('[data-cy=phase-chip]').should('contain', 'Ready to Focus')
      cy.get('[data-cy=session-alert]').should('not.exist')
      cy.get('[data-cy=stop-button]').should('not.exist')
      cy.get('[data-cy=skip-button]').should('not.exist')
    })

    it('should skip to break phase', () => {
      // Start focus session
      cy.get('[data-cy=main-action-button]').click()
      cy.waitForStableElement('[data-cy=skip-button]')
      
      // Skip to break
      cy.get('[data-cy=skip-button]').click()
      
      // Verify transitioned to break
      cy.get('[data-cy=phase-chip]').should('contain', 'Short Break')
      cy.get('[data-cy=timer-display]').should('contain', '05:00')
      cy.get('[data-cy=circular-timer] circle').should('have.attr', 'stroke')
        .and('not.equal', 'rgb(25, 118, 210)') // Not primary color anymore
    })

    it('should complete a full Pomodoro cycle', () => {
      // Mock timer completion
      cy.window().then((win) => {
        // Override timer to complete quickly for testing
        ;(win as Window & { mockTimerComplete?: () => void }).mockTimerComplete = () => {
          // Simulate timer completion
          cy.get('[data-cy=phase-chip]').should('contain', 'Short Break')
        }
      })

      cy.get('[data-cy=main-action-button]').click()
      
      // Simulate focus completion (in real app, this would be automatic)
      cy.get('[data-cy=skip-button]').click()
      
      // Should be in break phase
      cy.get('[data-cy=phase-chip]').should('contain', 'Short Break')
      cy.get('[data-cy=session-progress]').should('be.visible')
      
      // Complete break
      cy.get('[data-cy=skip-button]').click()
      
      // Should increment cycle counter
      cy.get('[data-cy=cycle-chip]').should('contain', 'Cycle 2/4')
    })
  })

  describe('Session Goals Management', () => {
    beforeEach(() => {
      // Start a focus session first
      cy.get('[data-cy=main-action-button]').click()
      cy.waitForStableElement('[data-cy=goals-section]')
    })

    it('should add session goals', () => {
      cy.get('[data-cy=goals-chip]').click()
      cy.get('[data-cy=goals-section]').should('be.visible')
      
      cy.get('[data-cy=add-goal-chip]').click()
      
      // Mock goal creation modal
      cy.get('[data-cy=goal-input]').should('be.visible')
      cy.get('[data-cy=goal-input]').type('Complete chapter 5 reading')
      cy.get('[data-cy=goal-priority-select]').click()
      cy.get('[data-cy=goal-priority-high]').click()
      cy.get('[data-cy=save-goal-button]').click()
      
      // Verify goal appears
      cy.get('[data-cy=goal-chip]').should('contain', 'Complete chapter 5 reading')
      cy.get('[data-cy=goals-badge]').should('contain', '1')
    })

    it('should complete goals during session', () => {
      // Add a goal first
      cy.get('[data-cy=goals-chip]').click()
      cy.get('[data-cy=add-goal-chip]').click()
      cy.get('[data-cy=goal-input]').type('Review notes')
      cy.get('[data-cy=save-goal-button]').click()
      
      // Complete the goal
      cy.get('[data-cy=goal-chip]').first().within(() => {
        cy.get('[data-cy=complete-goal-button]').click()
      })
      
      // Verify goal marked as completed
      cy.get('[data-cy=goal-chip]').first()
        .should('have.class', 'MuiChip-colorSuccess')
      
      // Mock goal completion API
      cy.mockApiCall('PATCH', '**/api/timer/sessions/*/goals/*', {
        statusCode: 200,
        body: { message: 'Goal completed' }
      })
    })

    it('should remove goals from session', () => {
      // Add a goal first
      cy.get('[data-cy=goals-chip]').click()
      cy.get('[data-cy=add-goal-chip]').click()
      cy.get('[data-cy=goal-input]').type('Goal to remove')
      cy.get('[data-cy=save-goal-button]').click()
      
      // Remove the goal
      cy.get('[data-cy=goal-chip]').first().within(() => {
        cy.get('[data-cy=delete-goal-button]').click()
      })
      
      cy.get('[data-cy=goal-chip]').should('not.exist')
      cy.get('[data-cy=goals-badge]').should('contain', '0')
    })
  })

  describe('Distraction Tracking', () => {
    beforeEach(() => {
      cy.get('[data-cy=main-action-button]').click()
      cy.waitForStableElement('[data-cy=distractions-chip]')
    })

    it('should record distractions during focus session', () => {
      cy.get('[data-cy=distractions-chip]').should('contain', 'Distractions: 0')
      
      // Mock distraction recording
      cy.mockApiCall('POST', '**/api/timer/sessions/*/distractions', {
        statusCode: 200,
        body: { distractions: 1 }
      })
      
      cy.get('[data-cy=distractions-chip]').click()
      
      cy.get('[data-cy=distractions-chip]').should('contain', 'Distractions: 1')
    })

    it('should track multiple distractions in session', () => {
      // Record multiple distractions
      for (let i = 1; i <= 3; i++) {
        cy.mockApiCall('POST', '**/api/timer/sessions/*/distractions', {
          statusCode: 200,
          body: { distractions: i }
        })
        
        cy.get('[data-cy=distractions-chip]').click()
        cy.get('[data-cy=distractions-chip]').should('contain', `Distractions: ${i}`)
      }
    })
  })

  describe('Session Rating and Completion', () => {
    it('should allow rating productivity at session end', () => {
      // Start and stop session
      cy.get('[data-cy=main-action-button]').click()
      cy.waitForStableElement('[data-cy=stop-button]')
      cy.get('[data-cy=stop-button]').click()
      
      // Should show rating modal
      cy.get('[data-cy=session-rating-modal]').should('be.visible')
      cy.get('[data-cy=productivity-rating]').should('be.visible')
      
      // Rate session
      cy.get('[data-cy=rating-star-4]').click()
      cy.get('[data-cy=session-notes-input]').type('Great focus session, completed all goals!')
      
      // Mock session completion
      cy.mockApiCall('POST', '**/api/timer/sessions/*/complete', {
        statusCode: 200,
        body: { message: 'Session completed successfully' }
      })
      
      cy.get('[data-cy=complete-session-button]').click()
      
      cy.get('[data-cy=success-notification]').should('be.visible')
        .and('contain', 'Session completed successfully')
    })

    it('should save session data without rating', () => {
      cy.get('[data-cy=main-action-button]').click()
      cy.waitForStableElement('[data-cy=stop-button]')
      cy.get('[data-cy=stop-button]').click()
      
      // Skip rating
      cy.get('[data-cy=skip-rating-button]').click()
      
      cy.get('[data-cy=session-rating-modal]').should('not.exist')
      cy.get('[data-cy=timer-display]').should('contain', '25:00')
    })
  })

  describe('Real-time Features and Presence', () => {
    beforeEach(() => {
      // Mock WebSocket connection
      cy.window().then((win) => {
        ;(win as Window & { mockStompClient?: unknown }).mockStompClient = {
          connected: true,
          send: cy.stub().as('stompSend'),
          subscribe: cy.stub().as('stompSubscribe')
        }
      })
    })

    it('should update presence when starting timer session', () => {
      cy.get('[data-cy=main-action-button]').click()
      
      // Verify WebSocket message sent for presence update
      cy.get('@stompSend').should('have.been.calledWith', 
        '/app/presence/update',
        Cypress.sinon.match.object,
        Cypress.sinon.match.string
      )
    })

    it('should sync timer across multiple browser tabs', () => {
      cy.get('[data-cy=main-action-button]').click()
      
      // Simulate receiving timer sync message from another tab
      cy.window().then((win) => {
        const mockMessage = {
          type: 'TIMER_SYNC',
          timerState: {
            currentPhase: 'focus',
            timeRemaining: 1440, // 24:00
            isRunning: true,
            isPaused: false,
            currentCycle: 1
          }
        }
        
        const extendedWin = win as Window & { mockWebSocketHandler?: (message: unknown) => void }
        if (extendedWin.mockWebSocketHandler) {
          extendedWin.mockWebSocketHandler(mockMessage)
        }
      })
      
      // Verify timer display updated
      cy.get('[data-cy=timer-display]').should('contain', '24:00')
    })

    it('should display member activity during focus session', () => {
      cy.fixture('hives').then((hivesData) => {
        cy.mockApiCall('GET', '**/api/hives/*/members/activity', {
          body: [
            { userId: 'user-1', status: 'focusing', timeRemaining: 1500 },
            { userId: 'user-2', status: 'on-break', timeRemaining: 300 }
          ]
        })
      })
      
      cy.get('[data-cy=main-action-button]').click()
      cy.get('[data-cy=member-activity]').should('be.visible')
      cy.get('[data-cy=member-status]').should('contain', 'focusing')
    })
  })

  describe('Edge Cases and Error Handling', () => {
    it('should handle browser refresh during active timer', () => {
      cy.get('[data-cy=main-action-button]').click()
      
      // Mock timer state recovery
      cy.window().then((win) => {
        win.localStorage.setItem('timerState', JSON.stringify({
          currentPhase: 'focus',
          timeRemaining: 1200,
          isRunning: true,
          isPaused: false,
          sessionId: 'session-123',
          sessionStartTime: new Date().toISOString()
        }))
      })
      
      cy.reload()
      cy.waitForStableElement('[data-cy=timer-container]')
      
      // Should restore timer state
      cy.get('[data-cy=phase-chip]').should('contain', 'Focus Time')
      cy.get('[data-cy=main-action-button]').should('have.attr', 'title', 'Pause Timer')
      cy.get('[data-cy=stop-button]').should('be.visible')
    })

    it('should handle network disconnect gracefully', () => {
      cy.get('[data-cy=main-action-button]').click()
      
      // Mock network failure
      cy.intercept('POST', '**/api/timer/**', { forceNetworkError: true }).as('networkError')
      
      cy.get('[data-cy=distractions-chip]').click()
      
      // Should show offline indicator
      cy.get('[data-cy=offline-indicator]').should('be.visible')
        .and('contain', 'Working offline')
      
      // Timer should continue locally
      cy.get('[data-cy=timer-display]').should('not.contain', '25:00')
    })

    it('should prevent multiple timer instances', () => {
      cy.get('[data-cy=main-action-button]').click()
      
      // Open new tab and try to start timer
      cy.window().then((win) => {
        // Simulate another tab starting a timer
        win.localStorage.setItem('activeTimerTab', 'other-tab-id')
      })
      
      // Should show warning
      cy.get('[data-cy=multiple-timer-warning]').should('be.visible')
        .and('contain', 'Timer is running in another tab')
      
      cy.get('[data-cy=main-action-button]').should('be.disabled')
    })

    it('should handle WebSocket connection failures', () => {
      // Mock WebSocket connection failure
      cy.window().then((win) => {
        ;(win as Window & { mockStompClient?: unknown }).mockStompClient = {
          connected: false,
          send: cy.stub(),
          subscribe: cy.stub()
        }
      })
      
      cy.get('[data-cy=connection-status]').should('be.visible')
        .and('contain', 'Reconnecting')
      
      cy.get('[data-cy=main-action-button]').click()
      
      // Should still work offline
      cy.get('[data-cy=timer-display]').should('not.contain', '25:00')
      cy.get('[data-cy=offline-indicator]').should('be.visible')
    })

    it('should recover from API errors during session', () => {
      cy.get('[data-cy=main-action-button]').click()
      
      // Mock API error for goal creation
      cy.mockApiCall('POST', '**/api/timer/sessions/*/goals', {
        statusCode: 500,
        body: { error: 'Server error' }
      })
      
      cy.get('[data-cy=goals-chip]').click()
      cy.get('[data-cy=add-goal-chip]').click()
      cy.get('[data-cy=goal-input]').type('Test goal')
      cy.get('[data-cy=save-goal-button]').click()
      
      // Should show error but allow retry
      cy.get('[data-cy=error-notification]').should('be.visible')
        .and('contain', 'Failed to save goal')
      
      cy.get('[data-cy=retry-button]').should('be.visible')
    })
  })

  describe('Accessibility and User Experience', () => {
    it('should be keyboard navigable', () => {
      cy.get('body').tab()
      cy.focused().should('have.attr', 'data-cy', 'main-action-button')
      
      cy.focused().tab()
      cy.focused().should('have.attr', 'data-cy', 'settings-button')
      
      // Test keyboard activation
      cy.focused().type('{enter}')
      cy.get('[data-cy=settings-menu]').should('be.visible')
      
      // Test escape to close
      cy.focused().type('{esc}')
      cy.get('[data-cy=settings-menu]').should('not.exist')
    })

    it('should support screen readers', () => {
      cy.injectAxe()
      cy.checkA11y('[data-cy=timer-container]', {
        rules: {
          'color-contrast': { enabled: true },
          'keyboard-navigation': { enabled: true },
          'aria-labels': { enabled: true }
        }
      })
      
      // Check timer display has proper ARIA labels
      cy.get('[data-cy=timer-display]').should('have.attr', 'aria-live', 'polite')
      cy.get('[data-cy=phase-chip]').should('have.attr', 'role', 'status')
    })

    it('should show focus mode for minimal distractions', () => {
      cy.get('[data-cy=fullscreen-button]').click()
      
      cy.get('[data-cy=timer-container]').should('have.class', 'fullscreen-mode')
      cy.get('[data-cy=circular-timer]').should('have.attr', 'data-size', '300')
      
      // Test exit fullscreen
      cy.get('[data-cy=exit-fullscreen-button]').click()
      cy.get('[data-cy=timer-container]').should('not.have.class', 'fullscreen-mode')
    })

    it('should provide visual feedback for timer state changes', () => {
      // Test color changes for different phases
      cy.get('[data-cy=main-action-button]').click()
      cy.get('[data-cy=circular-timer] circle').should('have.attr', 'stroke')
        .and('contain', 'rgb') // Should have color
      
      cy.get('[data-cy=skip-button]').click()
      cy.get('[data-cy=circular-timer] circle').should('have.attr', 'stroke')
        .and('not.equal', 'rgb(25, 118, 210)') // Different color for break
    })
  })

  describe('Integration with Hive Features', () => {
    beforeEach(() => {
      // Mock hive context
      cy.fixture('hives').then((hivesData) => {
        const hive = hivesData.singleHive
        cy.mockApiCall('GET', `**/api/hives/${hive.id}`, { body: hive })
        cy.visit(`/hives/${hive.id}/timer`)
      })
    })

    it('should start timer within hive context', () => {
      cy.get('[data-cy=hive-timer]').should('be.visible')
      cy.get('[data-cy=main-action-button]').click()
      
      // Should notify hive members
      cy.get('@stompSend').should('have.been.calledWith',
        '/app/hive/timer/start',
        Cypress.sinon.match.object
      )
    })

    it('should sync with other hive members', () => {
      cy.get('[data-cy=main-action-button]').click()
      
      // Mock receiving member timer update
      cy.window().then((win) => {
        const mockMessage = {
          type: 'MEMBER_TIMER_UPDATE',
          userId: 'user-2',
          phase: 'focus',
          timeRemaining: 1400
        }
        
        const extendedWin = win as Window & { mockWebSocketHandler?: (message: unknown) => void }
        if (extendedWin.mockWebSocketHandler) {
          extendedWin.mockWebSocketHandler(mockMessage)
        }
      })
      
      cy.get('[data-cy=member-timers]').should('contain', 'user-2')
      cy.get('[data-cy=member-phase]').should('contain', 'focusing')
    })

    it('should show synchronized break times', () => {
      // Mock hive with synchronized breaks setting
      cy.mockApiCall('GET', '**/api/hives/*/settings', {
        body: { synchronizedBreaks: true }
      })
      
      cy.get('[data-cy=main-action-button]').click()
      cy.get('[data-cy=skip-button]').click()
      
      // Should coordinate break with other members
      cy.get('[data-cy=synchronized-break-info]').should('be.visible')
        .and('contain', 'Synchronized break with hive members')
    })
  })
})