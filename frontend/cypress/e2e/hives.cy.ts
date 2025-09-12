describe('Hive Management', () => {
  beforeEach(() => {
    // Setup authenticated user
    cy.fixture('users').then((users) => {
      cy.setupAuthState(users.validUser)
    })
  })

  describe('Hive Dashboard', () => {
    beforeEach(() => {
      // Mock hives API response
      cy.fixture('hives').then((hivesData) => {
        cy.mockApiCall('GET', '**/api/hives', { body: hivesData.hives })
      })
      
      cy.visit('/dashboard')
    })

    it('should display user hives', () => {
      cy.get('[data-testid="hives-list"]').should('be.visible')
      cy.get('[data-testid="hive-card"]').should('have.length.greaterThan', 0)
    })

    it('should show hive details in cards', () => {
      cy.get('[data-testid="hive-card"]').first().within(() => {
        cy.get('[data-testid="hive-name"]').should('be.visible')
        cy.get('[data-testid="hive-description"]').should('be.visible')
        cy.get('[data-testid="hive-members-count"]').should('be.visible')
        cy.get('[data-testid="hive-status"]').should('be.visible')
      })
    })

    it('should filter hives by status', () => {
      cy.get('[data-testid="filter-active"]').click()
      cy.get('[data-testid="hive-card"]').should('exist')
      
      cy.get('[data-testid="filter-inactive"]').click()
      cy.get('[data-testid="hive-card"]').should('exist')
    })

    it('should search hives by name', () => {
      cy.get('[data-testid="hive-search"]').type('Study')
      cy.get('[data-testid="hive-card"]').should('have.length.at.least', 1)
      
      cy.get('[data-testid="hive-search"]').clear().type('NonexistentHive')
      cy.get('[data-testid="no-hives-found"]').should('be.visible')
    })
  })

  describe('Create New Hive', () => {
    beforeEach(() => {
      cy.visit('/dashboard')
      cy.get('[data-testid="create-hive-button"]').click()
    })

    it('should open create hive modal', () => {
      cy.get('[data-testid="create-hive-modal"]').should('be.visible')
      cy.get('[data-testid="hive-name-input"]').should('be.visible')
      cy.get('[data-testid="hive-description-input"]').should('be.visible')
      cy.get('[data-testid="hive-privacy-toggle"]').should('be.visible')
    })

    it('should validate required fields', () => {
      cy.get('[data-testid="create-hive-submit"]').click()
      
      cy.get('[data-testid="hive-name-error"]').should('be.visible')
        .and('contain', 'Name is required')
    })

    it('should create a new hive successfully', () => {
      const newHive = {
        id: 'hive-new',
        name: 'New Test Hive',
        description: 'Test description',
        createdBy: 'user-1',
        members: ['user-1'],
        isActive: true
      }

      // Mock successful creation
      cy.mockApiCall('POST', '**/api/hives', { 
        statusCode: 201,
        body: newHive 
      })

      // Fill form
      cy.get('[data-testid="hive-name-input"]').type(newHive.name)
      cy.get('[data-testid="hive-description-input"]').type(newHive.description)
      cy.get('[data-testid="create-hive-submit"]').click()

      // Should close modal and show success message
      cy.get('[data-testid="create-hive-modal"]').should('not.exist')
      cy.get('[data-testid="success-notification"]').should('be.visible')
        .and('contain', 'Hive created successfully')
    })

    it('should handle creation errors', () => {
      // Mock error response
      cy.mockApiCall('POST', '**/api/hives', {
        statusCode: 400,
        body: { message: 'Hive name already exists' }
      })

      cy.get('[data-testid="hive-name-input"]').type('Existing Hive')
      cy.get('[data-testid="hive-description-input"]').type('Description')
      cy.get('[data-testid="create-hive-submit"]').click()

      cy.get('[data-testid="error-notification"]').should('be.visible')
        .and('contain', 'Hive name already exists')
    })
  })

  describe('Hive Details Page', () => {
    beforeEach(() => {
      cy.fixture('hives').then((hivesData) => {
        const hive = hivesData.singleHive
        cy.mockApiCall('GET', `**/api/hives/${hive.id}`, { body: hive })
        cy.mockApiCall('GET', `**/api/hives/${hive.id}/members`, { 
          body: [{ id: 'user-1', name: 'Test User', isOnline: true }]
        })
        
        cy.visit(`/hives/${hive.id}`)
      })
    })

    it('should display hive information', () => {
      cy.get('[data-testid="hive-header"]').should('be.visible')
      cy.get('[data-testid="hive-title"]').should('contain', 'Solo Work')
      cy.get('[data-testid="hive-description"]').should('be.visible')
      cy.get('[data-testid="members-section"]').should('be.visible')
    })

    it('should show member presence status', () => {
      cy.get('[data-testid="member-list"]').should('be.visible')
      cy.get('[data-testid="member-item"]').should('have.length.at.least', 1)
      
      cy.get('[data-testid="member-item"]').first().within(() => {
        cy.get('[data-testid="member-name"]').should('be.visible')
        cy.get('[data-testid="member-status"]').should('be.visible')
      })
    })

    it('should allow starting focus session', () => {
      // Mock WebSocket connection
      cy.window().then((win) => {
        // Mock STOMP client
        ;(win as Window & { mockStompClient?: unknown }).mockStompClient = {
          connected: true,
          send: cy.stub(),
          subscribe: cy.stub()
        }
      })

      cy.get('[data-testid="start-session-button"]').click()
      cy.get('[data-testid="session-timer"]').should('be.visible')
      cy.get('[data-testid="session-status"]').should('contain', 'Focus Session Active')
    })

    it('should handle WebSocket connection errors gracefully', () => {
      // Mock WebSocket connection failure
      cy.window().then((win) => {
        ;(win as Window & { mockStompClient?: unknown }).mockStompClient = {
          connected: false,
          send: cy.stub(),
          subscribe: cy.stub()
        }
      })

      cy.get('[data-testid="connection-status"]').should('be.visible')
        .and('contain', 'Reconnecting')
    })
  })

  describe('Real-time Features', () => {
    beforeEach(() => {
      cy.fixture('hives').then((hivesData) => {
        const hive = hivesData.singleHive
        cy.mockApiCall('GET', `**/api/hives/${hive.id}`, { body: hive })
        cy.visit(`/hives/${hive.id}`)
      })
    })

    it('should update member presence in real-time', () => {
      // Mock WebSocket message
      cy.window().then((win) => {
        // Simulate receiving a presence update message
        const mockMessage = {
          type: 'PRESENCE_UPDATE',
          userId: 'user-2',
          status: 'online'
        }
        
        // Trigger WebSocket message handler
        const extendedWin = win as Window & { mockWebSocketHandler?: (message: unknown) => void }
        if (extendedWin.mockWebSocketHandler) {
          extendedWin.mockWebSocketHandler(mockMessage)
        }
      })

      // Should update UI to show new online member
      cy.get('[data-testid="online-members-count"]').should('contain', '2')
    })

    it('should show typing indicators in chat', () => {
      cy.get('[data-testid="chat-input"]').type('Hello everyone')
      
      // Should show typing indicator to other users (mocked)
      cy.window().then((win) => {
        const extendedWin = win as Window & { mockWebSocketHandler?: (message: unknown) => void }
        if (extendedWin.mockWebSocketHandler) {
          extendedWin.mockWebSocketHandler({
            type: 'USER_TYPING',
            userId: 'user-2',
            userName: 'Other User'
          })
        }
      })

      cy.get('[data-testid="typing-indicator"]').should('be.visible')
        .and('contain', 'Other User is typing')
    })
  })

  describe('Hive Settings', () => {
    beforeEach(() => {
      cy.fixture('hives').then((hivesData) => {
        const hive = hivesData.singleHive
        cy.mockApiCall('GET', `**/api/hives/${hive.id}`, { body: hive })
        cy.visit(`/hives/${hive.id}/settings`)
      })
    })

    it('should allow hive owner to modify settings', () => {
      cy.get('[data-testid="hive-settings-form"]').should('be.visible')
      
      cy.get('[data-testid="max-members-input"]').clear().type('15')
      cy.get('[data-testid="allow-music-toggle"]').click()
      
      // Mock successful update
      cy.mockApiCall('PATCH', '**/api/hives/*', {
        statusCode: 200,
        body: { message: 'Settings updated' }
      })
      
      cy.get('[data-testid="save-settings-button"]').click()
      
      cy.get('[data-testid="success-notification"]').should('be.visible')
        .and('contain', 'Settings updated')
    })

    it('should prevent non-owners from accessing settings', () => {
      // Mock user without owner permissions
      cy.setupAuthState({ 
        userId: 'user-2', 
        email: 'otheruser@test.com' 
      })
      
      cy.visit('/hives/hive-single/settings')
      
      cy.get('[data-testid="access-denied"]').should('be.visible')
        .and('contain', 'Access denied')
    })
  })
})