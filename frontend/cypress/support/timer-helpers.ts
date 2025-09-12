/**
 * Timer-specific test utilities and helpers for Cypress E2E tests
 */

// Timer state constants
export const TIMER_PHASES = {
  IDLE: 'idle',
  FOCUS: 'focus',
  SHORT_BREAK: 'short-break',
  LONG_BREAK: 'long-break'
} as const

export const DEFAULT_DURATIONS = {
  FOCUS: 25 * 60, // 25 minutes in seconds
  SHORT_BREAK: 5 * 60, // 5 minutes in seconds
  LONG_BREAK: 15 * 60 // 15 minutes in seconds
} as const

// Timer test utilities
export class TimerTestUtils {
  /**
   * Format seconds to MM:SS display format
   */
  static formatTime(seconds: number): string {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }

  /**
   * Generate timer state for testing
   */
  static generateTimerState(overrides: Partial<{
    currentPhase: string
    timeRemaining: number
    isRunning: boolean
    isPaused: boolean
    currentCycle: number
    totalCycles: number
  }> = {}) {
    return {
      currentPhase: 'idle',
      timeRemaining: DEFAULT_DURATIONS.FOCUS,
      isRunning: false,
      isPaused: false,
      currentCycle: 0,
      totalCycles: 4,
      sessionStartTime: new Date().toISOString(),
      ...overrides
    }
  }

  /**
   * Generate session data for testing
   */
  static generateSessionData(overrides: Partial<{
    id: string
    userId: string
    hiveId: string
    targetCycles: number
    completedCycles: number
    distractions: number
    goals: Array<{
      id: string
      description: string
      isCompleted: boolean
      priority: string
    }>
  }> = {}) {
    return {
      id: `session-${Date.now()}`,
      userId: 'test-user-123',
      hiveId: null,
      targetCycles: 4,
      completedCycles: 0,
      distractions: 0,
      goals: [],
      productivity: { rating: 0, notes: '' },
      startTime: new Date().toISOString(),
      focusTime: 0,
      breakTime: 0,
      achievements: [],
      ...overrides
    }
  }

  /**
   * Generate member activity data for hive testing
   */
  static generateMemberActivity(count: number = 5) {
    const statuses = ['focusing', 'on-break', 'idle']
    const phases = ['focus', 'short-break', 'long-break', 'idle']
    
    return Array.from({ length: count }, (_, i) => ({
      userId: `user-${i}`,
      userName: `Test User ${i}`,
      status: statuses[i % statuses.length],
      phase: phases[i % phases.length],
      timeRemaining: Math.floor(Math.random() * 1500),
      currentCycle: Math.floor(Math.random() * 4) + 1,
      isOnline: Math.random() > 0.2 // 80% online
    }))
  }

  /**
   * Generate WebSocket messages for timer synchronization
   */
  static generateWebSocketMessage(type: string, data: Record<string, unknown> = {}) {
    const messages = {
      TIMER_SYNC: {
        type: 'TIMER_SYNC',
        timerState: this.generateTimerState(),
        timestamp: Date.now(),
        ...data
      },
      PRESENCE_UPDATE: {
        type: 'PRESENCE_UPDATE',
        userId: 'test-user-123',
        status: 'focusing',
        timestamp: Date.now(),
        ...data
      },
      MEMBER_TIMER_UPDATE: {
        type: 'MEMBER_TIMER_UPDATE',
        userId: 'other-user',
        phase: 'focus',
        timeRemaining: 1200,
        ...data
      },
      SESSION_UPDATE: {
        type: 'SESSION_UPDATE',
        sessionId: 'session-123',
        update: {
          distractions: 1,
          completedGoals: 2
        },
        ...data
      }
    }

    return messages[type as keyof typeof messages] || { type, ...data }
  }
}

// Custom Cypress commands for timer testing
declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Assert timer display shows expected time
       * @example cy.assertTimerDisplay(1500) // 25:00
       */
      assertTimerDisplay(expectedSeconds: number): Chainable<void>
      
      /**
       * Assert timer phase matches expected phase
       * @example cy.assertTimerPhase('focus')
       */
      assertTimerPhase(expectedPhase: string): Chainable<void>
      
      /**
       * Simulate timer countdown for testing
       * @example cy.simulateTimerCountdown(60, 1) // Count down 60 seconds in 1 second steps
       */
      simulateTimerCountdown(totalSeconds: number, stepSize?: number): Chainable<void>
      
      /**
       * Fast forward timer to specific time for testing
       * @example cy.fastForwardTimer(300) // Fast forward to 5:00 remaining
       */
      fastForwardTimer(timeRemaining: number): Chainable<void>
      
      /**
       * Test timer with specific scenario
       * @example cy.testTimerScenario('complete-focus-cycle')
       */
      testTimerScenario(scenarioName: string): Chainable<void>
    }
  }
}

// Implement custom timer commands
Cypress.Commands.add('assertTimerDisplay', (expectedSeconds: number) => {
  const expectedDisplay = TimerTestUtils.formatTime(expectedSeconds)
  cy.get('[data-cy=timer-display]').should('contain', expectedDisplay)
})

Cypress.Commands.add('assertTimerPhase', (expectedPhase: string) => {
  const phaseDisplay = expectedPhase.replace('-', ' ')
  const capitalizedPhase = phaseDisplay.split(' ')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
  
  cy.get('[data-cy=phase-chip]').should('contain', capitalizedPhase)
})

Cypress.Commands.add('simulateTimerCountdown', (totalSeconds: number, stepSize: number = 1) => {
  for (let remaining = totalSeconds; remaining > 0; remaining -= stepSize) {
    cy.mockTimerWebSocket({
      type: 'TIMER_SYNC',
      timerState: TimerTestUtils.generateTimerState({
        currentPhase: 'focus',
        timeRemaining: remaining,
        isRunning: true
      })
    })
    
    // Small delay to simulate real timer behavior
    if (remaining % 10 === 0) { // Only wait every 10th update to speed up tests
      cy.wait(10)
    }
  }
})

Cypress.Commands.add('fastForwardTimer', (timeRemaining: number) => {
  cy.mockTimerWebSocket({
    type: 'TIMER_SYNC',
    timerState: TimerTestUtils.generateTimerState({
      currentPhase: 'focus',
      timeRemaining,
      isRunning: true
    })
  })
  
  cy.assertTimerDisplay(timeRemaining)
})

Cypress.Commands.add('testTimerScenario', (scenarioName: string) => {
  const scenarios = {
    'complete-focus-cycle': () => {
      // Start focus session
      cy.startTimerSession('focus')
      cy.assertTimerPhase('focus')
      
      // Fast forward to end of focus period
      cy.fastForwardTimer(5)
      cy.simulateTimerCountdown(5)
      
      // Should transition to break
      cy.mockTimerWebSocket({
        type: 'PHASE_CHANGE',
        newPhase: 'short-break',
        timeRemaining: DEFAULT_DURATIONS.SHORT_BREAK
      })
      
      cy.assertTimerPhase('short-break')
    },
    
    'pause-resume-session': () => {
      cy.startTimerSession('focus')
      cy.fastForwardTimer(1200) // 20:00
      
      // Pause timer
      cy.get('[data-cy=main-action-button]').click()
      cy.get('[data-cy=main-action-button]').should('have.attr', 'title', 'Resume Timer')
      
      // Wait and ensure time doesn't change
      cy.wait(1000)
      cy.assertTimerDisplay(1200)
      
      // Resume timer
      cy.get('[data-cy=main-action-button]').click()
      cy.get('[data-cy=main-action-button]').should('have.attr', 'title', 'Pause Timer')
    },
    
    'handle-distractions': () => {
      cy.startTimerSession('focus')
      
      // Record multiple distractions
      for (let i = 1; i <= 3; i++) {
        cy.mockApiCall('POST', '**/api/timer/sessions/*/distractions', {
          statusCode: 200,
          body: { distractions: i }
        })
        
        cy.get('[data-cy=distractions-chip]').click()
        cy.get('[data-cy=distractions-chip]').should('contain', `Distractions: ${i}`)
      }
    },
    
    'manage-session-goals': () => {
      cy.startTimerSession('focus')
      cy.get('[data-cy=goals-chip]').click()
      
      // Add multiple goals
      const goals = [
        { description: 'Complete reading assignment', priority: 'high' },
        { description: 'Review notes', priority: 'medium' },
        { description: 'Organize materials', priority: 'low' }
      ]
      
      goals.forEach((goal, index) => {
        cy.get('[data-cy=add-goal-chip]').click()
        cy.get('[data-cy=goal-input]').type(goal.description)
        cy.get('[data-cy=goal-priority-select]').click()
        cy.get(`[data-cy=goal-priority-${goal.priority}]`).click()
        cy.get('[data-cy=save-goal-button]').click()
        
        // Verify goal was added
        cy.get('[data-cy=goals-badge]').should('contain', (index + 1).toString())
      })
      
      // Complete first goal
      cy.get('[data-cy=goal-chip]').first().within(() => {
        cy.get('[data-cy=complete-goal-button]').click()
      })
      
      cy.get('[data-cy=goal-chip]').first()
        .should('have.class', 'MuiChip-colorSuccess')
    }
  }
  
  const scenario = scenarios[scenarioName as keyof typeof scenarios]
  if (scenario) {
    scenario()
  } else {
    throw new Error(`Unknown timer scenario: ${scenarioName}`)
  }
})

export { TimerTestUtils }