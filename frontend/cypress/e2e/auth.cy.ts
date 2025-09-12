describe('Authentication Flow', () => {
  beforeEach(() => {
    // Clear any existing session
    cy.clearCookies()
    cy.clearLocalStorage()
    cy.clearAllSessionStorage()
  })

  describe('Login Page', () => {
    beforeEach(() => {
      cy.visit('/auth/login')
    })

    it('should display login form', () => {
      cy.get('[data-testid="login-form"]').should('be.visible')
      cy.get('[data-testid="email-input"]').should('be.visible')
      cy.get('[data-testid="password-input"]').should('be.visible')
      cy.get('[data-testid="login-button"]').should('be.visible')
    })

    it('should show validation errors for empty form', () => {
      cy.get('[data-testid="login-button"]').click()
      
      // Should show validation errors
      cy.get('[data-testid="email-error"]').should('be.visible')
      cy.get('[data-testid="password-error"]').should('be.visible')
    })

    it('should show validation errors for invalid email', () => {
      cy.get('[data-testid="email-input"]').type('invalid-email')
      cy.get('[data-testid="password-input"]').type('password123')
      cy.get('[data-testid="login-button"]').click()
      
      cy.get('[data-testid="email-error"]').should('be.visible')
        .and('contain', 'email')
    })

    it('should handle login failure', () => {
      // Mock failed login response
      cy.mockApiCall('POST', '**/auth/login', {
        statusCode: 401,
        body: { message: 'Invalid credentials' }
      })

      cy.get('[data-testid="email-input"]').type('user@example.com')
      cy.get('[data-testid="password-input"]').type('wrongpassword')
      cy.get('[data-testid="login-button"]').click()

      // Should show error message
      cy.get('[data-testid="login-error"]').should('be.visible')
        .and('contain', 'Invalid credentials')
    })

    it('should login successfully with valid credentials', () => {
      cy.fixture('users').then((users) => {
        // Mock successful login response
        cy.mockApiCall('POST', '**/auth/login', {
          statusCode: 200,
          body: {
            token: 'mock-jwt-token',
            user: users.validUser
          }
        })

        cy.get('[data-testid="email-input"]').type(users.validUser.email)
        cy.get('[data-testid="password-input"]').type(users.validUser.password)
        cy.get('[data-testid="login-button"]').click()

        // Should redirect to dashboard or home page
        cy.url().should('not.include', '/auth/login')
        
        // Should store auth token
        cy.window().its('localStorage.token').should('exist')
      })
    })

    it('should toggle password visibility', () => {
      cy.get('[data-testid="password-input"]').should('have.attr', 'type', 'password')
      
      cy.get('[data-testid="toggle-password-visibility"]').click()
      cy.get('[data-testid="password-input"]').should('have.attr', 'type', 'text')
      
      cy.get('[data-testid="toggle-password-visibility"]').click()
      cy.get('[data-testid="password-input"]').should('have.attr', 'type', 'password')
    })

    it('should navigate to signup page', () => {
      cy.get('[data-testid="signup-link"]').click()
      cy.url().should('include', '/auth/signup')
    })
  })

  describe('Signup Page', () => {
    beforeEach(() => {
      cy.visit('/auth/signup')
    })

    it('should display signup form', () => {
      cy.get('[data-testid="signup-form"]').should('be.visible')
      cy.get('[data-testid="name-input"]').should('be.visible')
      cy.get('[data-testid="email-input"]').should('be.visible')
      cy.get('[data-testid="password-input"]').should('be.visible')
      cy.get('[data-testid="confirm-password-input"]').should('be.visible')
      cy.get('[data-testid="signup-button"]').should('be.visible')
    })

    it('should validate password confirmation', () => {
      cy.get('[data-testid="password-input"]').type('password123')
      cy.get('[data-testid="confirm-password-input"]').type('differentpassword')
      cy.get('[data-testid="signup-button"]').click()
      
      cy.get('[data-testid="confirm-password-error"]').should('be.visible')
        .and('contain', 'match')
    })
  })

  describe('Logout', () => {
    beforeEach(() => {
      // Setup authenticated state
      cy.fixture('users').then((users) => {
        cy.setupAuthState(users.validUser)
        cy.visit('/dashboard')
      })
    })

    it('should logout successfully', () => {
      cy.logout()
      
      // Should redirect to login page
      cy.url().should('include', '/auth/login')
      
      // Should clear auth data
      cy.window().then((win) => {
        expect(win.localStorage.getItem('token')).to.be.null
        expect(win.localStorage.getItem('user')).to.be.null
      })
    })
  })

  describe('Protected Routes', () => {
    it('should redirect unauthenticated users to login', () => {
      cy.visit('/dashboard')
      cy.url().should('include', '/auth/login')
    })

    it('should allow authenticated users to access protected routes', () => {
      cy.fixture('users').then((users) => {
        cy.setupAuthState(users.validUser)
        cy.visit('/dashboard')
        cy.url().should('include', '/dashboard')
      })
    })
  })

  describe('OAuth2 Integration', () => {
    it('should display OAuth login options', () => {
      cy.visit('/auth/login')
      
      // Check for OAuth buttons (if implemented)
      cy.get('[data-testid="oauth-google"]').should('be.visible')
      cy.get('[data-testid="oauth-github"]').should('be.visible')
    })

    it('should handle OAuth callback', () => {
      // Mock OAuth callback with token
      cy.visit('/auth/callback?code=mock-auth-code&state=mock-state')
      
      // Should process the callback and redirect
      cy.url().should('not.include', '/auth/callback')
    })
  })
})