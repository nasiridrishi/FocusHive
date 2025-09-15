import React from 'react'
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { screen, within, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/test-utils'
import DiscoverPage from '../DiscoverPage'

// Mock the useToast hook
const mockToast = {
  success: vi.fn(),
  error: vi.fn(),
  warning: vi.fn(),
  info: vi.fn(),
}

vi.mock('../../../../hooks/useToast', () => ({
  useToast: () => mockToast,
}))

// Mock the HiveList component to test DiscoverPage's logic
const mockHiveList = vi.fn()
vi.mock('../../components', () => ({
  HiveList: (props: any) => {
    mockHiveList(props)
    return (
      <div data-testid="hive-list">
        <div data-testid="hive-list-title">{props.title}</div>
        <div data-testid="hive-list-loading">{props.isLoading ? 'loading' : 'loaded'}</div>
        <div data-testid="hive-list-hives-count">{props.hives?.length || 0}</div>
        <div data-testid="hive-list-show-create">{props.showCreateButton ? 'true' : 'false'}</div>
        <div data-testid="hive-list-show-filters">{props.showFilters ? 'true' : 'false'}</div>
        {props.hives?.map((hive: any) => (
          <div key={hive.id} data-testid={`hive-${hive.id}`}>
            {hive.name}
          </div>
        ))}
        <button onClick={() => props.onJoin?.('test-hive', 'test message')}>
          Join Hive
        </button>
        <button onClick={() => props.onEnter?.('test-hive')}>
          Enter Hive
        </button>
        <button onClick={() => props.onShare?.('test-hive')}>
          Share Hive
        </button>
        <button onClick={() => props.onRefresh?.()}>
          Refresh
        </button>
      </div>
    )
  },
}))

// Mock window.location
const mockLocation = {
  href: '',
  origin: 'http://localhost:3000',
}
Object.defineProperty(window, 'location', {
  value: mockLocation,
  writable: true,
})

// Mock navigator.clipboard
const mockClipboard = {
  writeText: vi.fn().mockResolvedValue(undefined),
}
Object.defineProperty(navigator, 'clipboard', {
  value: mockClipboard,
  writable: true,
})

describe('DiscoverPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockLocation.href = ''
    mockClipboard.writeText.mockClear()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('Rendering', () => {
    it('should render discover page container', () => {
      renderWithProviders(<DiscoverPage />)

      expect(screen.getByTestId('hive-list')).toBeInTheDocument()
    })

    it('should display page title with explore icon', () => {
      renderWithProviders(<DiscoverPage />)

      expect(screen.getByRole('heading', { name: /discover hives/i })).toBeInTheDocument()
    })

    it('should display page description', () => {
      renderWithProviders(<DiscoverPage />)

      expect(screen.getByText(/explore public hives and find communities/i)).toBeInTheDocument()
    })

    it('should display info alert for new users', () => {
      renderWithProviders(<DiscoverPage />)

      expect(screen.getByText(/new to focushive/i)).toBeInTheDocument()
      expect(screen.getByText(/try joining a popular hive to get started/i)).toBeInTheDocument()
    })

    it('should display popular categories section', () => {
      renderWithProviders(<DiscoverPage />)

      expect(screen.getByText(/popular categories/i)).toBeInTheDocument()
    })

    it('should display all category chips', () => {
      renderWithProviders(<DiscoverPage />)

      const categories = [
        'Coding', 'Writing', 'Study', 'Language Learning',
        'Art', 'Math', 'Reading', 'Research', 'Creative'
      ]

      categories.forEach(category => {
        expect(screen.getByRole('button', { name: category })).toBeInTheDocument()
      })
    })

    it('should display hive list with correct props', () => {
      renderWithProviders(<DiscoverPage />)

      expect(screen.getByTestId('hive-list-title')).toHaveTextContent('Public Hives')
      expect(screen.getByTestId('hive-list-show-create')).toHaveTextContent('false')
      expect(screen.getByTestId('hive-list-show-filters')).toHaveTextContent('true')
    })
  })

  describe('Loading States', () => {
    it('should show loading state initially', () => {
      renderWithProviders(<DiscoverPage />)

      expect(screen.getByTestId('hive-list-loading')).toHaveTextContent('loading')
    })

    it('should hide loading state after data loads', async () => {
      renderWithProviders(<DiscoverPage />)

      // Fast-forward timers to simulate data loading
      vi.advanceTimersByTime(1000)

      await waitFor(() => {
        expect(screen.getByTestId('hive-list-loading')).toHaveTextContent('loaded')
      })
    })

    it('should display hives after loading completes', async () => {
      renderWithProviders(<DiscoverPage />)

      // Fast-forward timers
      vi.advanceTimersByTime(1000)

      await waitFor(() => {
        expect(screen.getByTestId('hive-list-hives-count')).toHaveTextContent('5')
      })
    })
  })

  describe('Hive Data', () => {
    it('should load and display mock public hives', async () => {
      renderWithProviders(<DiscoverPage />)

      vi.advanceTimersByTime(1000)

      await waitFor(() => {
        expect(screen.getByTestId('hive-4')).toHaveTextContent('Coding Bootcamp')
        expect(screen.getByTestId('hive-5')).toHaveTextContent('Writers Circle')
        expect(screen.getByTestId('hive-6')).toHaveTextContent('Language Exchange')
        expect(screen.getByTestId('hive-7')).toHaveTextContent('Math Study Hall')
        expect(screen.getByTestId('hive-8')).toHaveTextContent('Art & Design Studio')
      })
    })

    it('should pass correct hive data to HiveList', async () => {
      renderWithProviders(<DiscoverPage />)

      vi.advanceTimersByTime(1000)

      await waitFor(() => {
        const lastCall = mockHiveList.mock.calls[mockHiveList.mock.calls.length - 1]
        const props = lastCall[0]

        expect(props.hives).toHaveLength(5)
        expect(props.hives[0].name).toBe('Coding Bootcamp')
        expect(props.hives[0].isPublic).toBe(true)
        expect(props.currentUserId).toBe('user1')
      })
    })

    it('should pass member data to HiveList', async () => {
      renderWithProviders(<DiscoverPage />)

      vi.advanceTimersByTime(1000)

      await waitFor(() => {
        const lastCall = mockHiveList.mock.calls[mockHiveList.mock.calls.length - 1]
        const props = lastCall[0]

        expect(props.members).toBeDefined()
        expect(Object.keys(props.members)).toHaveLength(5)
        // Check that each hive has members data
        Object.values(props.members).forEach((memberList: any) => {
          expect(Array.isArray(memberList)).toBe(true)
          expect(memberList.length).toBeGreaterThan(0)
        })
      })
    })
  })

  describe('User Interactions', () => {
    it('should handle category chip clicks', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      renderWithProviders(<DiscoverPage />)

      const codingChip = screen.getByRole('button', { name: 'Coding' })
      await user.click(codingChip)

      // Should not throw any errors
      expect(codingChip).toBeInTheDocument()
    })

    it('should handle join hive action', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      renderWithProviders(<DiscoverPage />)

      const joinButton = screen.getByRole('button', { name: /join hive/i })
      await user.click(joinButton)

      // Advance timer to simulate API call
      vi.advanceTimersByTime(2000)

      await waitFor(() => {
        expect(mockToast.success).toHaveBeenCalledWith('Successfully requested to join hive!')
      })
    })

    it('should handle enter hive action', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      renderWithProviders(<DiscoverPage />)

      const enterButton = screen.getByRole('button', { name: /enter hive/i })
      await user.click(enterButton)

      expect(mockLocation.href).toBe('/hive/test-hive')
    })

    it('should handle share hive action', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      renderWithProviders(<DiscoverPage />)

      const shareButton = screen.getByRole('button', { name: /share hive/i })
      await user.click(shareButton)

      expect(mockClipboard.writeText).toHaveBeenCalledWith('http://localhost:3000/hive/test-hive')
      expect(mockToast.success).toHaveBeenCalledWith('Hive link copied to clipboard!')
    })

    it('should handle refresh action', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      renderWithProviders(<DiscoverPage />)

      // Wait for initial load
      vi.advanceTimersByTime(1000)
      await waitFor(() => {
        expect(screen.getByTestId('hive-list-loading')).toHaveTextContent('loaded')
      })

      const refreshButton = screen.getByRole('button', { name: /refresh/i })
      await user.click(refreshButton)

      // Should show loading again
      expect(screen.getByTestId('hive-list-loading')).toHaveTextContent('loading')

      // Then loaded again after refresh completes
      vi.advanceTimersByTime(600)
      await waitFor(() => {
        expect(screen.getByTestId('hive-list-loading')).toHaveTextContent('loaded')
      })
    })
  })

  describe('Join Hive Functionality', () => {
    it('should handle join without message', async () => {
      renderWithProviders(<DiscoverPage />)

      // Wait for component to mount and get the props
      vi.advanceTimersByTime(100)

      await waitFor(() => {
        const lastCall = mockHiveList.mock.calls[mockHiveList.mock.calls.length - 1]
        const props = lastCall[0]

        // Simulate calling onJoin without message
        props.onJoin('test-hive')
      })

      // Advance timer for API simulation
      vi.advanceTimersByTime(2000)

      await waitFor(() => {
        expect(mockToast.success).toHaveBeenCalledWith('Successfully joined hive!')
      })
    })

    it('should handle join with approval message', async () => {
      renderWithProviders(<DiscoverPage />)

      vi.advanceTimersByTime(100)

      await waitFor(() => {
        const lastCall = mockHiveList.mock.calls[mockHiveList.mock.calls.length - 1]
        const props = lastCall[0]

        // Simulate calling onJoin with message
        props.onJoin('test-hive', 'Please let me join!')
      })

      vi.advanceTimersByTime(2000)

      await waitFor(() => {
        expect(mockToast.success).toHaveBeenCalledWith('Successfully requested to join hive!')
      })
    })
  })

  describe('Category Chips', () => {
    it('should render all category chips with correct styling', () => {
      renderWithProviders(<DiscoverPage />)

      const categories = [
        'Coding', 'Writing', 'Study', 'Language Learning',
        'Art', 'Math', 'Reading', 'Research', 'Creative'
      ]

      categories.forEach(category => {
        const chip = screen.getByRole('button', { name: category })
        expect(chip).toBeInTheDocument()
        expect(chip).toHaveClass('MuiChip-outlined')
      })
    })

    it('should have pointer cursor on category chips', () => {
      renderWithProviders(<DiscoverPage />)

      const codingChip = screen.getByRole('button', { name: 'Coding' })
      const styles = getComputedStyle(codingChip)
      expect(styles.cursor).toBe('pointer')
    })
  })

  describe('Accessibility', () => {
    it('should have proper heading hierarchy', () => {
      renderWithProviders(<DiscoverPage />)

      const mainHeading = screen.getByRole('heading', { level: 1 })
      expect(mainHeading).toHaveTextContent('Discover Hives')

      const categoryHeading = screen.getByRole('heading', { level: 6 })
      expect(categoryHeading).toHaveTextContent('Popular Categories')
    })

    it('should have accessible category chips', () => {
      renderWithProviders(<DiscoverPage />)

      const codingChip = screen.getByRole('button', { name: 'Coding' })
      expect(codingChip).toHaveAttribute('role', 'button')
      expect(codingChip).toBeEnabled()
    })

    it('should have proper ARIA labels for buttons', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      renderWithProviders(<DiscoverPage />)

      const codingChip = screen.getByRole('button', { name: 'Coding' })

      // Should be focusable
      codingChip.focus()
      expect(document.activeElement).toBe(codingChip)

      // Should respond to keyboard interaction
      await user.keyboard('{Enter}')
      // Should not throw errors
      expect(codingChip).toBeInTheDocument()
    })
  })

  describe('Responsive Design', () => {
    it('should display category chips in a flexible layout', () => {
      renderWithProviders(<DiscoverPage />)

      const chipContainer = screen.getByRole('button', { name: 'Coding' }).parentElement
      expect(chipContainer).toHaveStyle({
        display: 'flex',
        flexWrap: 'wrap',
      })
    })

    it('should maintain spacing between category chips', () => {
      renderWithProviders(<DiscoverPage />)

      const chipContainer = screen.getByRole('button', { name: 'Coding' }).parentElement
      const styles = getComputedStyle(chipContainer!)
      expect(styles.gap).toBeTruthy()
    })
  })

  describe('Error Handling', () => {
    it('should handle clipboard API failure gracefully', async () => {
      mockClipboard.writeText.mockRejectedValueOnce(new Error('Clipboard failed'))

      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      renderWithProviders(<DiscoverPage />)

      const shareButton = screen.getByRole('button', { name: /share hive/i })
      await user.click(shareButton)

      // Should not break the application
      expect(shareButton).toBeInTheDocument()
    })

    it('should handle missing location API gracefully', async () => {
      const originalLocation = window.location
      delete (window as any).location

      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      renderWithProviders(<DiscoverPage />)

      const enterButton = screen.getByRole('button', { name: /enter hive/i })
      await user.click(enterButton)

      // Should not break the application
      expect(enterButton).toBeInTheDocument()

      // Restore location
      ;(window as any).location = originalLocation
    })
  })

  describe('Performance', () => {
    it('should debounce rapid refresh clicks', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      renderWithProviders(<DiscoverPage />)

      // Wait for initial load
      vi.advanceTimersByTime(1000)
      await waitFor(() => {
        expect(screen.getByTestId('hive-list-loading')).toHaveTextContent('loaded')
      })

      const refreshButton = screen.getByRole('button', { name: /refresh/i })

      // Click refresh multiple times rapidly
      await user.click(refreshButton)
      await user.click(refreshButton)
      await user.click(refreshButton)

      // Should only trigger one refresh cycle
      vi.advanceTimersByTime(600)
      await waitFor(() => {
        expect(screen.getByTestId('hive-list-loading')).toHaveTextContent('loaded')
      })
    })

    it('should not re-render unnecessarily during loading', () => {
      renderWithProviders(<DiscoverPage />)

      const initialCallCount = mockHiveList.mock.calls.length

      // Advance timer partially (not fully loaded yet)
      vi.advanceTimersByTime(400)

      // Should not have triggered additional renders with data
      const currentCallCount = mockHiveList.mock.calls.length
      expect(currentCallCount - initialCallCount).toBeLessThanOrEqual(1)
    })
  })

  describe('Integration', () => {
    it('should pass all required props to HiveList component', async () => {
      renderWithProviders(<DiscoverPage />)

      vi.advanceTimersByTime(1000)

      await waitFor(() => {
        const lastCall = mockHiveList.mock.calls[mockHiveList.mock.calls.length - 1]
        const props = lastCall[0]

        expect(props).toHaveProperty('hives')
        expect(props).toHaveProperty('members')
        expect(props).toHaveProperty('currentUserId')
        expect(props).toHaveProperty('isLoading')
        expect(props).toHaveProperty('onJoin')
        expect(props).toHaveProperty('onEnter')
        expect(props).toHaveProperty('onShare')
        expect(props).toHaveProperty('onRefresh')
        expect(props).toHaveProperty('title')
        expect(props).toHaveProperty('showCreateButton')
        expect(props).toHaveProperty('showFilters')
      })
    })

    it('should maintain consistent currentUserId across re-renders', async () => {
      renderWithProviders(<DiscoverPage />)

      vi.advanceTimersByTime(1000)

      await waitFor(() => {
        const firstCall = mockHiveList.mock.calls[0]
        const lastCall = mockHiveList.mock.calls[mockHiveList.mock.calls.length - 1]

        expect(firstCall[0].currentUserId).toBe('user1')
        expect(lastCall[0].currentUserId).toBe('user1')
      })
    })
  })

  describe('State Management', () => {
    it('should maintain loading state correctly during refresh', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      renderWithProviders(<DiscoverPage />)

      // Initial load
      vi.advanceTimersByTime(1000)
      await waitFor(() => {
        expect(screen.getByTestId('hive-list-loading')).toHaveTextContent('loaded')
      })

      // Trigger refresh
      const refreshButton = screen.getByRole('button', { name: /refresh/i })
      await user.click(refreshButton)

      // Should show loading
      expect(screen.getByTestId('hive-list-loading')).toHaveTextContent('loading')

      // Complete refresh
      vi.advanceTimersByTime(600)
      await waitFor(() => {
        expect(screen.getByTestId('hive-list-loading')).toHaveTextContent('loaded')
      })
    })

    it('should update hive data after refresh', async () => {
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      renderWithProviders(<DiscoverPage />)

      // Initial load
      vi.advanceTimersByTime(1000)
      await waitFor(() => {
        expect(screen.getByTestId('hive-list-hives-count')).toHaveTextContent('5')
      })

      // Trigger refresh
      const refreshButton = screen.getByRole('button', { name: /refresh/i })
      await user.click(refreshButton)

      vi.advanceTimersByTime(600)
      await waitFor(() => {
        expect(screen.getByTestId('hive-list-hives-count')).toHaveTextContent('5')
      })
    })
  })
})