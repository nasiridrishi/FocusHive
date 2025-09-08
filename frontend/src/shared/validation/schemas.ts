import * as yup from 'yup'

// Password strength regex patterns
const passwordStrengthRegex = {
  uppercase: /(?=.*[A-Z])/,
  lowercase: /(?=.*[a-z])/,
  number: /(?=.*\d)/,
  special: /(?=.*[@$!%*?&#\-_+=<>.,;:'"()[\]{}|\\/~`^])/,
  minLength: /.{8,}/
}

// Email validation schema
const emailSchema = yup
  .string()
  .required('Email is required')
  .email('Please enter a valid email address')
  .max(255, 'Email must be less than 255 characters')

// Password validation schema with strength requirements
const passwordSchema = yup
  .string()
  .required('Password is required')
  .min(8, 'Password must be at least 8 characters')
  .max(128, 'Password must be less than 128 characters')
  .matches(passwordStrengthRegex.uppercase, 'Password must contain at least one uppercase letter')
  .matches(passwordStrengthRegex.lowercase, 'Password must contain at least one lowercase letter')
  .matches(passwordStrengthRegex.number, 'Password must contain at least one number')
  .matches(passwordStrengthRegex.special, 'Password must contain at least one special character')

// Username validation schema
const usernameSchema = yup
  .string()
  .required('Username is required')
  .min(3, 'Username must be at least 3 characters')
  .max(30, 'Username must be less than 30 characters')
  .matches(/^[a-zA-Z0-9_-]+$/, 'Username can only contain letters, numbers, underscores, and hyphens')

// Name validation schema
const nameSchema = yup
  .string()
  .required('This field is required')
  .min(2, 'Must be at least 2 characters')
  .max(50, 'Must be less than 50 characters')
  .matches(/^[a-zA-Z\s'-]+$/, 'Can only contain letters, spaces, apostrophes, and hyphens')

// Login form validation schema
export const loginSchema = yup.object({
  email: emailSchema,
  password: yup
    .string()
    .required('Password is required')
    .min(6, 'Password must be at least 6 characters')
})

// Registration form validation schema
export const registerSchema = yup.object({
  firstName: nameSchema.label('First name'),
  lastName: nameSchema.label('Last name'),
  username: usernameSchema,
  email: emailSchema,
  password: passwordSchema,
  confirmPassword: yup
    .string()
    .required('Please confirm your password')
    .oneOf([yup.ref('password')], 'Passwords must match'),
  acceptTerms: yup
    .boolean()
    .oneOf([true], 'You must accept the terms and conditions')
})

// Create Hive form validation schema
export const createHiveSchema = yup.object({
  name: yup
    .string()
    .required('Hive name is required')
    .min(3, 'Hive name must be at least 3 characters')
    .max(50, 'Hive name must be less than 50 characters')
    .matches(/^[a-zA-Z0-9\s\-_.,!?()]+$/, 'Hive name contains invalid characters'),
  description: yup
    .string()
    .required('Description is required')
    .min(10, 'Description must be at least 10 characters')
    .max(500, 'Description must be less than 500 characters'),
  maxMembers: yup
    .number()
    .required('Maximum members is required')
    .min(2, 'Must allow at least 2 members')
    .max(100, 'Cannot exceed 100 members'),
  isPublic: yup.boolean().required(),
  tags: yup
    .array()
    .of(yup.string().max(20, 'Tag must be less than 20 characters'))
    .max(10, 'Cannot have more than 10 tags'),
  settings: yup.object({
    allowChat: yup.boolean().required(),
    allowVoice: yup.boolean().required(),
    requireApproval: yup.boolean().required(),
    focusMode: yup
      .string()
      .required('Focus mode is required')
      .oneOf(['pomodoro', 'continuous', 'flexible'], 'Invalid focus mode'),
    defaultSessionLength: yup
      .number()
      .required('Default session length is required')
      .min(5, 'Session must be at least 5 minutes')
      .max(120, 'Session cannot exceed 120 minutes'),
    maxSessionLength: yup
      .number()
      .required('Maximum session length is required')
      .min(30, 'Maximum session must be at least 30 minutes')
      .max(480, 'Maximum session cannot exceed 8 hours')
      .test(
        'greater-than-default',
        'Maximum session must be greater than default session',
        function (value) {
          const { defaultSessionLength } = this.parent
          return !value || !defaultSessionLength || value >= defaultSessionLength
        }
      )
  })
})

// Password strength checker utility
export const checkPasswordStrength = (password: string): {
  score: number
  feedback: string[]
  strength: 'weak' | 'fair' | 'good' | 'strong'
} => {
  const feedback: string[] = []
  let score = 0

  if (password.length >= 8) score += 1
  else feedback.push('Use at least 8 characters')

  if (passwordStrengthRegex.lowercase.test(password)) score += 1
  else feedback.push('Add lowercase letters')

  if (passwordStrengthRegex.uppercase.test(password)) score += 1
  else feedback.push('Add uppercase letters')

  if (passwordStrengthRegex.number.test(password)) score += 1
  else feedback.push('Add numbers')

  if (passwordStrengthRegex.special.test(password)) score += 1
  else feedback.push('Add special characters (@$!%*?&)')

  // Length bonus
  if (password.length >= 12) score += 1

  let strength: 'weak' | 'fair' | 'good' | 'strong'
  if (score <= 2) strength = 'weak'
  else if (score <= 3) strength = 'fair'
  else if (score <= 4) strength = 'good'
  else strength = 'strong'

  return { score, feedback, strength }
}

// Export individual schemas for reuse
export { emailSchema, passwordSchema, usernameSchema, nameSchema }