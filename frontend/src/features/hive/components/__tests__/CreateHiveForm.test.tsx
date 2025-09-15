import React from 'react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/test-utils'
import CreateHiveForm from '../CreateHiveForm'

describe('CreateHiveForm', () => {
  const defaultProps = {
    open: true,
    onClose: vi.fn(),
    onSubmit: vi.fn(),
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render form with all required fields', () => {
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      expect(screen.getByTestId('create-hive-form')).toBeInTheDocument()
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/description/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/type/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/category/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/max members/i)).toBeInTheDocument()
    })

    it('should render form title', () => {
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      expect(screen.getByRole('heading', { name: /create new hive/i })).toBeInTheDocument()
    })

    it('should render submit and cancel buttons', () => {
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      expect(screen.getByRole('button', { name: /create hive/i })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument()
    })

    it('should display helper text for fields', () => {
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      expect(screen.getByText(/3-50 characters/i)).toBeInTheDocument()
      expect(screen.getByText(/10-500 characters/i)).toBeInTheDocument()
      expect(screen.getByText(/between 2 and 50 members/i)).toBeInTheDocument()
    })
  })

  describe('Form Validation', () => {
    it('should show error when name is too short', async () => {
      const user = userEvent.setup()
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      const nameInput = screen.getByLabelText(/name/i)
      await user.type(nameInput, 'ab')
      await user.tab() // Trigger blur

      expect(await screen.findByText(/name must be at least 3 characters/i)).toBeInTheDocument()
    })

    it('should show error when name is too long', async () => {
      const user = userEvent.setup()
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      const nameInput = screen.getByLabelText(/name/i)
      await user.type(nameInput, 'a'.repeat(51))
      await user.tab()

      expect(await screen.findByText(/name must be at most 50 characters/i)).toBeInTheDocument()
    })

    it('should show error when description is too short', async () => {
      const user = userEvent.setup()
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      const descInput = screen.getByLabelText(/description/i)
      await user.type(descInput, 'short')
      await user.tab()

      expect(await screen.findByText(/description must be at least 10 characters/i)).toBeInTheDocument()
    })

    it('should show error when max members is below minimum', async () => {
      const user = userEvent.setup()
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      const maxMembersInput = screen.getByLabelText(/max members/i)
      await user.clear(maxMembersInput)
      await user.type(maxMembersInput, '1')
      await user.tab()

      expect(await screen.findByText(/minimum is 2 members/i)).toBeInTheDocument()
    })

    it('should show error when max members exceeds maximum', async () => {
      const user = userEvent.setup()
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      const maxMembersInput = screen.getByLabelText(/max members/i)
      await user.clear(maxMembersInput)
      await user.type(maxMembersInput, '51')
      await user.tab()

      expect(await screen.findByText(/maximum is 50 members/i)).toBeInTheDocument()
    })

    it('should disable submit button when form is invalid', async () => {
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      const submitButton = screen.getByRole('button', { name: /create hive/i })
      expect(submitButton).toBeDisabled()
    })

    it('should enable submit button when form is valid', async () => {
      const user = userEvent.setup()
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      // Fill in valid values
      await user.type(screen.getByLabelText(/name/i), 'Study Group')
      await user.type(screen.getByLabelText(/description/i), 'A focused study group for exam preparation')
      
      const submitButton = screen.getByRole('button', { name: /create hive/i })
      await waitFor(() => {
        expect(submitButton).not.toBeDisabled()
      })
    })
  })

  describe('Type Selection', () => {
    it('should have PUBLIC, PRIVATE, and INVITE_ONLY options', async () => {
      const user = userEvent.setup()
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      const typeSelect = screen.getByLabelText(/type/i)
      await user.click(typeSelect)

      expect(screen.getByRole('option', { name: /public/i })).toBeInTheDocument()
      expect(screen.getByRole('option', { name: /private/i })).toBeInTheDocument()
      expect(screen.getByRole('option', { name: /invite only/i })).toBeInTheDocument()
    })

    it('should select PUBLIC by default', () => {
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      const typeSelect = screen.getByLabelText(/type/i) as HTMLSelectElement
      expect(typeSelect.value).toBe('PUBLIC')
    })

    it('should update value when different type is selected', async () => {
      const user = userEvent.setup()
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      const typeSelect = screen.getByLabelText(/type/i)
      await user.click(typeSelect)
      await user.click(screen.getByRole('option', { name: /private/i }))

      expect((typeSelect as HTMLSelectElement).value).toBe('PRIVATE')
    })
  })

  describe('Category Selection', () => {
    it('should display category options', async () => {
      const user = userEvent.setup()
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      const categorySelect = screen.getByLabelText(/category/i)
      await user.click(categorySelect)

      // Check for some expected categories
      expect(screen.getByRole('option', { name: /study/i })).toBeInTheDocument()
      expect(screen.getByRole('option', { name: /work/i })).toBeInTheDocument()
      expect(screen.getByRole('option', { name: /coding/i })).toBeInTheDocument()
      expect(screen.getByRole('option', { name: /creative/i })).toBeInTheDocument()
    })

    it('should update value when category is selected', async () => {
      const user = userEvent.setup()
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      const categorySelect = screen.getByLabelText(/category/i)
      await user.click(categorySelect)
      await user.click(screen.getByRole('option', { name: /coding/i }))

      expect((categorySelect as HTMLSelectElement).value).toBe('CODING')
    })
  })

  describe('Form Submission', () => {
    it('should call onSubmit with form data when submitted', async () => {
      const user = userEvent.setup()
      const onSubmit = vi.fn()
      renderWithProviders(<CreateHiveForm {...defaultProps} onSubmit={onSubmit} />)

      // Fill in the form
      await user.type(screen.getByLabelText(/name/i), 'Test Hive')
      await user.type(screen.getByLabelText(/description/i), 'This is a test hive for focused work')
      
      const typeSelect = screen.getByLabelText(/type/i)
      await user.click(typeSelect)
      await user.click(screen.getByRole('option', { name: /private/i }))

      const categorySelect = screen.getByLabelText(/category/i)
      await user.click(categorySelect)
      await user.click(screen.getByRole('option', { name: /coding/i }))

      const maxMembersInput = screen.getByLabelText(/max members/i)
      await user.clear(maxMembersInput)
      await user.type(maxMembersInput, '15')

      // Submit the form
      const submitButton = screen.getByRole('button', { name: /create hive/i })
      await waitFor(() => expect(submitButton).not.toBeDisabled())
      await user.click(submitButton)

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith({
          name: 'Test Hive',
          description: 'This is a test hive for focused work',
          type: 'PRIVATE',
          category: 'CODING',
          maxMembers: 15,
        })
      })
    })

    it('should reset form after successful submission', async () => {
      const user = userEvent.setup()
      const onSubmit = vi.fn().mockResolvedValue({ success: true })
      renderWithProviders(<CreateHiveForm {...defaultProps} onSubmit={onSubmit} />)

      // Fill and submit
      await user.type(screen.getByLabelText(/name/i), 'Test Hive')
      await user.type(screen.getByLabelText(/description/i), 'This is a test hive for focused work')
      
      const submitButton = screen.getByRole('button', { name: /create hive/i })
      await waitFor(() => expect(submitButton).not.toBeDisabled())
      await user.click(submitButton)

      await waitFor(() => {
        expect(screen.getByLabelText(/name/i)).toHaveValue('')
        expect(screen.getByLabelText(/description/i)).toHaveValue('')
      })
    })

    it('should show loading state during submission', async () => {
      const user = userEvent.setup()
      const onSubmit = vi.fn(() => new Promise(resolve => setTimeout(resolve, 100)))
      renderWithProviders(<CreateHiveForm {...defaultProps} onSubmit={onSubmit} />)

      // Fill required fields
      await user.type(screen.getByLabelText(/name/i), 'Test Hive')
      await user.type(screen.getByLabelText(/description/i), 'This is a test hive for focused work')

      const submitButton = screen.getByRole('button', { name: /create hive/i })
      await waitFor(() => expect(submitButton).not.toBeDisabled())
      await user.click(submitButton)

      // Check for loading state
      expect(screen.getByRole('progressbar')).toBeInTheDocument()
      expect(submitButton).toBeDisabled()
    })

    it('should show error message on submission failure', async () => {
      const user = userEvent.setup()
      const onSubmit = vi.fn().mockRejectedValue(new Error('Failed to create hive'))
      renderWithProviders(<CreateHiveForm {...defaultProps} onSubmit={onSubmit} />)

      // Fill and submit
      await user.type(screen.getByLabelText(/name/i), 'Test Hive')
      await user.type(screen.getByLabelText(/description/i), 'This is a test hive for focused work')

      const submitButton = screen.getByRole('button', { name: /create hive/i })
      await waitFor(() => expect(submitButton).not.toBeDisabled())
      await user.click(submitButton)

      expect(await screen.findByText(/failed to create hive/i)).toBeInTheDocument()
    })
  })

  describe('Form Cancellation', () => {
    it('should call onClose when cancel button is clicked', async () => {
      const user = userEvent.setup()
      const onClose = vi.fn()
      renderWithProviders(<CreateHiveForm {...defaultProps} onClose={onClose} />)

      const cancelButton = screen.getByRole('button', { name: /cancel/i })
      await user.click(cancelButton)

      expect(onClose).toHaveBeenCalled()
    })

    it('should clear form when cancelled', async () => {
      const user = userEvent.setup()
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      // Add some data
      await user.type(screen.getByLabelText(/name/i), 'Test')
      await user.type(screen.getByLabelText(/description/i), 'Test description')

      // Cancel
      const cancelButton = screen.getByRole('button', { name: /cancel/i })
      await user.click(cancelButton)

      expect(screen.getByLabelText(/name/i)).toHaveValue('')
      expect(screen.getByLabelText(/description/i)).toHaveValue('')
    })
  })

  describe('Accessibility', () => {
    it('should have proper form structure', () => {
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      const form = screen.getByTestId('create-hive-form')
      expect(form).toHaveAttribute('role', 'form')
      expect(form).toHaveAttribute('aria-label', 'Create new hive form')
    })

    it('should have proper labels for all inputs', () => {
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      expect(screen.getByLabelText(/name/i)).toHaveAttribute('id')
      expect(screen.getByLabelText(/description/i)).toHaveAttribute('id')
      expect(screen.getByLabelText(/type/i)).toHaveAttribute('id')
      expect(screen.getByLabelText(/category/i)).toHaveAttribute('id')
      expect(screen.getByLabelText(/max members/i)).toHaveAttribute('id')
    })

    it('should announce errors to screen readers', async () => {
      const user = userEvent.setup()
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      const nameInput = screen.getByLabelText(/name/i)
      await user.type(nameInput, 'a')
      await user.tab()

      const error = await screen.findByText(/name must be at least 3 characters/i)
      expect(error).toHaveAttribute('role', 'alert')
    })

    it('should support keyboard navigation', async () => {
      const user = userEvent.setup()
      renderWithProviders(<CreateHiveForm {...defaultProps} />)

      // Tab through form fields
      await user.tab() // Name
      expect(screen.getByLabelText(/name/i)).toHaveFocus()

      await user.tab() // Description
      expect(screen.getByLabelText(/description/i)).toHaveFocus()

      await user.tab() // Type
      expect(screen.getByLabelText(/type/i)).toHaveFocus()

      await user.tab() // Category
      expect(screen.getByLabelText(/category/i)).toHaveFocus()

      await user.tab() // Max members
      expect(screen.getByLabelText(/max members/i)).toHaveFocus()
    })
  })

  describe('Edge Cases', () => {
    it('should handle special characters in name', async () => {
      const user = userEvent.setup()
      const onSubmit = vi.fn()
      renderWithProviders(<CreateHiveForm {...defaultProps} onSubmit={onSubmit} />)

      await user.type(screen.getByLabelText(/name/i), 'Test & Special @ Hive!')
      await user.type(screen.getByLabelText(/description/i), 'This hive has special characters in the name')

      const submitButton = screen.getByRole('button', { name: /create hive/i })
      await waitFor(() => expect(submitButton).not.toBeDisabled())
      await user.click(submitButton)

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith(
          expect.objectContaining({
            name: 'Test & Special @ Hive!',
          })
        )
      })
    })

    it('should trim whitespace from inputs', async () => {
      const user = userEvent.setup()
      const onSubmit = vi.fn()
      renderWithProviders(<CreateHiveForm {...defaultProps} onSubmit={onSubmit} />)

      await user.type(screen.getByLabelText(/name/i), '  Test Hive  ')
      await user.type(screen.getByLabelText(/description/i), '  Test description with spaces  ')

      const submitButton = screen.getByRole('button', { name: /create hive/i })
      await waitFor(() => expect(submitButton).not.toBeDisabled())
      await user.click(submitButton)

      await waitFor(() => {
        expect(onSubmit).toHaveBeenCalledWith(
          expect.objectContaining({
            name: 'Test Hive',
            description: 'Test description with spaces',
          })
        )
      })
    })
  })
})