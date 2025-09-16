import {describe, expect, it} from 'vitest'
import {checkPasswordStrength, createHiveSchema, loginSchema, registerSchema} from './schemas'

describe('Validation Schemas', () => {
  describe('loginSchema', () => {
    it('should validate a valid login', async () => {
      const validLogin = {
        email: 'user@example.com',
        password: 'validpassword123'
      }

      const result = await loginSchema.isValid(validLogin)
      expect(result).toBe(true)
    })

    it('should reject invalid email', async () => {
      const invalidLogin = {
        email: 'invalid-email',
        password: 'validpassword123'
      }

      const result = await loginSchema.isValid(invalidLogin)
      expect(result).toBe(false)
    })

    it('should reject short password', async () => {
      const invalidLogin = {
        email: 'user@example.com',
        password: '123'
      }

      const result = await loginSchema.isValid(invalidLogin)
      expect(result).toBe(false)
    })
  })

  describe('registerSchema', () => {
    it('should validate a valid registration', async () => {
      const validRegister = {
        firstName: 'John',
        lastName: 'Doe',
        username: 'johndoe123',
        email: 'john@example.com',
        password: 'SecurePass123!',
        confirmPassword: 'SecurePass123!',
        acceptTerms: true
      }

      const result = await registerSchema.isValid(validRegister)
      expect(result).toBe(true)
    })

    it('should reject mismatched passwords', async () => {
      const invalidRegister = {
        firstName: 'John',
        lastName: 'Doe',
        username: 'johndoe123',
        email: 'john@example.com',
        password: 'SecurePass123!',
        confirmPassword: 'DifferentPassword123!',
        acceptTerms: true
      }

      const result = await registerSchema.isValid(invalidRegister)
      expect(result).toBe(false)
    })

    it('should reject weak password', async () => {
      const invalidRegister = {
        firstName: 'John',
        lastName: 'Doe',
        username: 'johndoe123',
        email: 'john@example.com',
        password: 'weakpass',
        confirmPassword: 'weakpass',
        acceptTerms: true
      }

      const result = await registerSchema.isValid(invalidRegister)
      expect(result).toBe(false)
    })

    it('should reject invalid username', async () => {
      const invalidRegister = {
        firstName: 'John',
        lastName: 'Doe',
        username: 'jo',  // Too short
        email: 'john@example.com',
        password: 'SecurePass123!',
        confirmPassword: 'SecurePass123!',
        acceptTerms: true
      }

      const result = await registerSchema.isValid(invalidRegister)
      expect(result).toBe(false)
    })

    it('should reject when terms not accepted', async () => {
      const invalidRegister = {
        firstName: 'John',
        lastName: 'Doe',
        username: 'johndoe123',
        email: 'john@example.com',
        password: 'SecurePass123!',
        confirmPassword: 'SecurePass123!',
        acceptTerms: false
      }

      const result = await registerSchema.isValid(invalidRegister)
      expect(result).toBe(false)
    })
  })

  describe('createHiveSchema', () => {
    it('should validate a valid hive', async () => {
      const validHive = {
        name: 'Study Group',
        description: 'A great place to study together with focused learning sessions.',
        maxMembers: 10,
        isPublic: true,
        tags: ['study', 'learning'],
        settings: {
          allowChat: true,
          allowVoice: false,
          requireApproval: false,
          focusMode: 'flexible' as const,
          defaultSessionLength: 25,
          maxSessionLength: 120
        }
      }

      const result = await createHiveSchema.isValid(validHive)
      expect(result).toBe(true)
    })

    it('should reject short hive name', async () => {
      const invalidHive = {
        name: 'Hi',  // Too short
        description: 'A great place to study together with focused learning sessions.',
        maxMembers: 10,
        isPublic: true,
        tags: ['study'],
        settings: {
          allowChat: true,
          allowVoice: false,
          requireApproval: false,
          focusMode: 'flexible' as const,
          defaultSessionLength: 25,
          maxSessionLength: 120
        }
      }

      const result = await createHiveSchema.isValid(invalidHive)
      expect(result).toBe(false)
    })

    it('should reject short description', async () => {
      const invalidHive = {
        name: 'Study Group',
        description: 'Short',  // Too short
        maxMembers: 10,
        isPublic: true,
        tags: ['study'],
        settings: {
          allowChat: true,
          allowVoice: false,
          requireApproval: false,
          focusMode: 'flexible' as const,
          defaultSessionLength: 25,
          maxSessionLength: 120
        }
      }

      const result = await createHiveSchema.isValid(invalidHive)
      expect(result).toBe(false)
    })

    it('should reject when max session is less than default session', async () => {
      const invalidHive = {
        name: 'Study Group',
        description: 'A great place to study together with focused learning sessions.',
        maxMembers: 10,
        isPublic: true,
        tags: ['study'],
        settings: {
          allowChat: true,
          allowVoice: false,
          requireApproval: false,
          focusMode: 'flexible' as const,
          defaultSessionLength: 60,
          maxSessionLength: 30  // Less than default
        }
      }

      const result = await createHiveSchema.isValid(invalidHive)
      expect(result).toBe(false)
    })

    it('should reject too many tags', async () => {
      const invalidHive = {
        name: 'Study Group',
        description: 'A great place to study together with focused learning sessions.',
        maxMembers: 10,
        isPublic: true,
        tags: ['tag1', 'tag2', 'tag3', 'tag4', 'tag5', 'tag6', 'tag7', 'tag8', 'tag9', 'tag10', 'tag11'],  // 11 tags
        settings: {
          allowChat: true,
          allowVoice: false,
          requireApproval: false,
          focusMode: 'flexible' as const,
          defaultSessionLength: 25,
          maxSessionLength: 120
        }
      }

      const result = await createHiveSchema.isValid(invalidHive)
      expect(result).toBe(false)
    })
  })

  describe('checkPasswordStrength', () => {
    it('should rate a weak password', () => {
      const result = checkPasswordStrength('weak')
      expect(result.strength).toBe('weak')
      expect(result.score).toBeLessThan(3)
      expect(result.feedback).toContain('Use at least 8 characters')
    })

    it('should rate a fair password', () => {
      const result = checkPasswordStrength('password123')
      expect(result.strength).toBe('fair')
      expect(result.score).toBe(3)
    })

    it('should rate a good password', () => {
      const result = checkPasswordStrength('Password123')
      expect(result.strength).toBe('good')
      expect(result.score).toBe(4)
    })

    it('should rate a strong password', () => {
      const result = checkPasswordStrength('SecurePassword123!')
      expect(result.strength).toBe('strong')
      expect(result.score).toBeGreaterThan(4)
      expect(result.feedback).toHaveLength(0)
    })

    it('should provide helpful feedback for weak passwords', () => {
      const result = checkPasswordStrength('abc')
      expect(result.feedback).toContain('Use at least 8 characters')
      expect(result.feedback).toContain('Add uppercase letters')
      expect(result.feedback).toContain('Add numbers')
      expect(result.feedback).toContain('Add special characters (@$!%*?&)')
    })
  })
})