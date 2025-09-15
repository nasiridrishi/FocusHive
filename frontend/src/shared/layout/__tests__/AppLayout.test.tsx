import React from 'react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../test-utils/test-utils'
import AppLayout from '../AppLayout'

// Mock useNavigate
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    Outlet: () => <div data-testid="router-outlet">Outlet Content</div>,
  }
})

// Mock useAuth hook
vi.mock('../../../hooks/useAuth', () => ({
  useAuth: vi.fn(() => ({
    user: {
      id: 'user123',
      name: 'John Doe',
      email: 'john@example.com',
    },
    isAuthenticated: true,
    logout: vi.fn(),
  })),
}))

describe('AppLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render the main layout structure', () => {
      renderWithProviders(<AppLayout />)

      expect(screen.getByTestId('app-layout')).toBeInTheDocument()
      expect(screen.getByTestId('app-header')).toBeInTheDocument()
      expect(screen.getByTestId('app-sidebar')).toBeInTheDocument()
      expect(screen.getByTestId('main-content')).toBeInTheDocument()
      expect(screen.getByTestId('router-outlet')).toBeInTheDocument()
    })

    it('should render header with logo and navigation', () => {
      renderWithProviders(<AppLayout />)

      const header = screen.getByTestId('app-header')
      expect(within(header).getByTestId('app-logo')).toBeInTheDocument()
      expect(within(header).getByTestId('nav-menu')).toBeInTheDocument()
      expect(within(header).getByTestId('user-menu')).toBeInTheDocument()
    })

    it('should render sidebar with navigation links', () => {
      renderWithProviders(<AppLayout />)

      const sidebar = screen.getByTestId('app-sidebar')
      expect(within(sidebar).getByText('Dashboard')).toBeInTheDocument()
      expect(within(sidebar).getByText('Discover')).toBeInTheDocument()
      expect(within(sidebar).getByText('My Hives')).toBeInTheDocument()
      expect(within(sidebar).getByText('Analytics')).toBeInTheDocument()
      expect(within(sidebar).getByText('Settings')).toBeInTheDocument()
    })

    it('should render footer with copyright', () => {
      renderWithProviders(<AppLayout />)

      const footer = screen.getByTestId('app-footer')
      expect(footer).toBeInTheDocument()
      expect(within(footer).getByText(/© 2025 FocusHive/i)).toBeInTheDocument()
    })

    it('should render children content in main area', () => {
      renderWithProviders(
        <AppLayout>
          <div data-testid="child-content">Child Content</div>
        </AppLayout>
      )

      expect(screen.getByTestId('child-content')).toBeInTheDocument()
      expect(screen.getByTestId('router-outlet')).toBeInTheDocument()
    })
  })

  describe('Sidebar Toggle', () => {
    it('should toggle sidebar visibility on mobile', async () => {
      const user = userEvent.setup()
      renderWithProviders(<AppLayout />)

      const toggleButton = screen.getByTestId('sidebar-toggle')
      const sidebar = screen.getByTestId('app-sidebar')

      expect(sidebar).toHaveAttribute('aria-expanded', 'true')

      await user.click(toggleButton)
      expect(sidebar).toHaveAttribute('aria-expanded', 'false')

      await user.click(toggleButton)
      expect(sidebar).toHaveAttribute('aria-expanded', 'true')
    })

    it('should close sidebar when overlay is clicked on mobile', async () => {
      const user = userEvent.setup()
      renderWithProviders(<AppLayout />)

      const sidebar = screen.getByTestId('app-sidebar')
      expect(sidebar).toHaveAttribute('aria-expanded', 'true')

      const overlay = screen.getByTestId('sidebar-overlay')
      await user.click(overlay)

      expect(sidebar).toHaveAttribute('aria-expanded', 'false')
    })
  })

  describe('Navigation', () => {
    it('should highlight active navigation item', () => {
      renderWithProviders(<AppLayout />)

      // Assuming we're on dashboard route
      const dashboardLink = screen.getByTestId('nav-link-dashboard')
      expect(dashboardLink).toHaveClass('active')
    })

    it('should navigate when sidebar links are clicked', async () => {
      const user = userEvent.setup()
      renderWithProviders(<AppLayout />)

      const discoverLink = screen.getByText('Discover')
      await user.click(discoverLink)

      expect(mockNavigate).toHaveBeenCalledWith('/discover')
    })

    it('should show user menu dropdown when clicked', async () => {
      const user = userEvent.setup()
      renderWithProviders(<AppLayout />)

      const userMenuButton = screen.getByTestId('user-menu-button')
      await user.click(userMenuButton)

      expect(screen.getByTestId('user-menu-dropdown')).toBeInTheDocument()
      expect(screen.getByText('Profile')).toBeInTheDocument()
      expect(screen.getByText('Settings')).toBeInTheDocument()
      expect(screen.getByText('Logout')).toBeInTheDocument()
    })

    it('should handle logout when clicked', async () => {
      const user = userEvent.setup()
      const { useAuth } = await import('../../../hooks/useAuth')
      const mockLogout = vi.fn()
      vi.mocked(useAuth).mockReturnValue({
        user: { id: 'user123', name: 'John Doe', email: 'john@example.com' },
        isAuthenticated: true,
        logout: mockLogout,
      } as any)

      renderWithProviders(<AppLayout />)

      const userMenuButton = screen.getByTestId('user-menu-button')
      await user.click(userMenuButton)

      const logoutButton = screen.getByText('Logout')
      await user.click(logoutButton)

      expect(mockLogout).toHaveBeenCalled()
    })
  })

  describe('Responsive Behavior', () => {
    it('should show mobile menu button on small screens', () => {
      // Mock mobile viewport
      global.innerWidth = 375
      renderWithProviders(<AppLayout />)

      expect(screen.getByTestId('mobile-menu-button')).toBeInTheDocument()
    })

    it('should hide sidebar by default on mobile', () => {
      global.innerWidth = 375
      renderWithProviders(<AppLayout />)

      const sidebar = screen.getByTestId('app-sidebar')
      expect(sidebar).toHaveAttribute('aria-expanded', 'false')
    })

    it('should show sidebar by default on desktop', () => {
      global.innerWidth = 1920
      renderWithProviders(<AppLayout />)

      const sidebar = screen.getByTestId('app-sidebar')
      expect(sidebar).toHaveAttribute('aria-expanded', 'true')
    })
  })

  describe('Notifications', () => {
    it('should show notification badge when there are unread notifications', () => {
      renderWithProviders(<AppLayout />)

      const notificationBadge = screen.getByTestId('notification-badge')
      expect(notificationBadge).toBeInTheDocument()
      expect(notificationBadge).toHaveTextContent('3') // Default value in component
    })

    it('should open notifications panel when bell icon is clicked', async () => {
      const user = userEvent.setup()
      renderWithProviders(<AppLayout />)

      const notificationButton = screen.getByTestId('notification-button')
      await user.click(notificationButton)

      expect(screen.getByTestId('notifications-panel')).toBeInTheDocument()
    })
  })

  describe('Theme Toggle', () => {
    it('should have theme toggle button', () => {
      renderWithProviders(<AppLayout />)

      expect(screen.getByTestId('theme-toggle')).toBeInTheDocument()
    })

    it('should toggle between light and dark theme', async () => {
      const user = userEvent.setup()
      renderWithProviders(<AppLayout />)

      const themeToggle = screen.getByTestId('theme-toggle')

      // Initially light theme
      expect(themeToggle).toHaveAttribute('aria-label', 'Switch to dark theme')

      await user.click(themeToggle)
      expect(themeToggle).toHaveAttribute('aria-label', 'Switch to light theme')

      await user.click(themeToggle)
      expect(themeToggle).toHaveAttribute('aria-label', 'Switch to dark theme')
    })
  })

  describe('Accessibility', () => {
    it('should have proper ARIA landmarks', () => {
      renderWithProviders(<AppLayout />)

      expect(screen.getByRole('banner')).toBeInTheDocument() // header
      expect(screen.getByRole('navigation')).toBeInTheDocument() // nav
      expect(screen.getByRole('main')).toBeInTheDocument() // main content
      expect(screen.getByRole('contentinfo')).toBeInTheDocument() // footer
    })

    it('should have proper heading hierarchy', () => {
      renderWithProviders(<AppLayout />)

      const headings = screen.getAllByRole('heading')
      expect(headings.length).toBeGreaterThan(0)
    })

    it('should support keyboard navigation', async () => {
      const user = userEvent.setup()
      renderWithProviders(<AppLayout />)

      // Tab through interactive elements
      await user.tab()
      expect(document.activeElement).not.toBe(document.body)

      // Check that sidebar can be toggled with Enter key
      const toggleButton = screen.getByTestId('sidebar-toggle')
      toggleButton.focus()
      await user.keyboard('{Enter}')

      const sidebar = screen.getByTestId('app-sidebar')
      expect(sidebar).toHaveAttribute('aria-expanded', 'false')
    })

    it('should announce sidebar state changes to screen readers', () => {
      renderWithProviders(<AppLayout />)

      const sidebar = screen.getByTestId('app-sidebar')
      expect(sidebar).toHaveAttribute('aria-label', 'Main navigation')
    })
  })

  describe('Loading States', () => {
    it('should show loading indicator when content is loading', () => {
      renderWithProviders(<AppLayout isLoading={true} />)

      expect(screen.getByTestId('layout-loading')).toBeInTheDocument()
    })
  })

  describe('Error States', () => {
    it('should show error boundary fallback on error', () => {
      const testError = new Error('Test error')
      renderWithProviders(<AppLayout error={testError} />)

      expect(screen.getByTestId('error-fallback')).toBeInTheDocument()
    })
  })
})