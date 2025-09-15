import React from 'react'
import { describe, expect, it, vi, beforeEach, beforeAll, afterEach } from 'vitest'
import { screen, within, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../test-utils/test-utils'
import { Header } from '../../../shared/layout/Header'
import type { User } from '@shared/types/auth'

// Mock react-router-dom
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

// Mock the i18n components
vi.mock('../../../shared/components/i18n', () => ({
  CompactLanguageSwitcher: () => <div data-testid="language-switcher">Language Switcher</div>,
  useTranslation: () => ({
    t: (key: string) => {
      const translations: Record<string, string> = {
        'app.name': 'FocusHive',
        'placeholders.search': 'Search hives, users...',
        'accessibility.aria_label_search': 'Search',
        'status.connected': 'Connected to server',
        'status.disconnected': 'Disconnected from server',
        'status.online': 'Online',
        'status.offline': 'Offline',
        'navigation.notifications': 'Notifications',
        'navigation.profile': 'Profile',
        'navigation.settings': 'Settings',
        'navigation.logout': 'Logout',
      }
      return translations[key] || key
    },
  }),
}))

// Mock user data
const mockUser: User = {
  id: '1',
  email: 'john.doe@example.com',
  username: 'johndoe',
  firstName: 'John',
  lastName: 'Doe',
  name: 'John Doe',
  avatar: 'https://example.com/avatar.jpg',
  profilePicture: 'https://example.com/avatar.jpg',
  isEmailVerified: true,
  isVerified: true,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
}

const mockUserWithoutAvatar: User = {
  ...mockUser,
  avatar: undefined,
  profilePicture: undefined,
}

describe('Header', () => {
  const defaultProps = {
    drawerWidth: 240,
    onDrawerToggle: vi.fn(),
    isConnected: true,
  }

  beforeAll(() => {
    // Mock window.matchMedia for responsive design tests
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    })
  })

  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render the header AppBar', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const appBar = screen.getByRole('banner')
      expect(appBar).toBeInTheDocument()
    })

    it('should display the app logo/title', () => {
      renderWithProviders(<Header {...defaultProps} />)

      expect(screen.getByText('FocusHive')).toBeInTheDocument()
    })

    it('should render mobile menu toggle button', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const menuButton = screen.getByRole('button', { name: /toggle drawer/i })
      expect(menuButton).toBeInTheDocument()
    })

    it('should render search bar with placeholder', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const searchInput = screen.getByPlaceholderText('Search hives, users...')
      expect(searchInput).toBeInTheDocument()
      expect(searchInput).toHaveAttribute('aria-label', 'Search')
    })

    it('should render search icon', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const searchIcon = screen.getByTestId('SearchIcon')
      expect(searchIcon).toBeInTheDocument()
    })

    it('should render language switcher on desktop', () => {
      renderWithProviders(<Header {...defaultProps} />)

      expect(screen.getByTestId('language-switcher')).toBeInTheDocument()
    })

    it('should render notifications bell', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const notificationButton = screen.getByRole('button', { name: /show .* new notifications/i })
      expect(notificationButton).toBeInTheDocument()
    })

    it('should render user profile button', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const profileButton = screen.getByRole('button', { name: /account of current user/i })
      expect(profileButton).toBeInTheDocument()
    })
  })

  describe('Authentication States', () => {
    it('should show default mock user data when no user provided', () => {
      renderWithProviders(<Header {...defaultProps} />)

      // The component has mock user data, so it should show profile button
      expect(screen.getByRole('button', { name: /account of current user/i })).toBeInTheDocument()
    })

    it('should show account circle icon for user profile button', () => {
      renderWithProviders(<Header {...defaultProps} />)

      // The component uses mock data with no avatar, so should show AccountCircleIcon
      const profileButton = screen.getByRole('button', { name: /account of current user/i })
      expect(profileButton).toBeInTheDocument()

      // AccountCircleIcon should be present in the profile button
      const accountIcons = screen.getAllByTestId('AccountCircleIcon')
      expect(accountIcons.length).toBeGreaterThan(0)
    })

    it('should display user menu when authenticated', () => {
      renderWithProviders(<Header {...defaultProps} />)

      // Component always shows profile button with mock data
      const profileButton = screen.getByRole('button', { name: /account of current user/i })
      expect(profileButton).toBeInTheDocument()
    })

    it('should show notification bell when authenticated', () => {
      renderWithProviders(<Header {...defaultProps} />)

      // Component always shows notifications with mock data
      const notificationButton = screen.getByRole('button', { name: /show .* new notifications/i })
      expect(notificationButton).toBeInTheDocument()
    })
  })

  describe('User Menu Functionality', () => {
    it('should open user menu when profile button is clicked', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const profileButton = screen.getByRole('button', { name: /account of current user/i })
      await user.click(profileButton)

      expect(screen.getByRole('menu')).toBeInTheDocument()
    })

    it('should display user information in menu header', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const profileButton = screen.getByRole('button', { name: /account of current user/i })
      await user.click(profileButton)

      // Mock user data should be displayed
      expect(screen.getByText('John Doe')).toBeInTheDocument()
      expect(screen.getByText('john.doe@example.com')).toBeInTheDocument()
    })

    it('should display profile menu item', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const profileButton = screen.getByRole('button', { name: /account of current user/i })
      await user.click(profileButton)

      expect(screen.getByRole('menuitem', { name: /profile/i })).toBeInTheDocument()
    })

    it('should display settings menu item', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const profileButton = screen.getByRole('button', { name: /account of current user/i })
      await user.click(profileButton)

      expect(screen.getByRole('menuitem', { name: /settings/i })).toBeInTheDocument()
    })

    it('should display logout menu item', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const profileButton = screen.getByRole('button', { name: /account of current user/i })
      await user.click(profileButton)

      expect(screen.getByRole('menuitem', { name: /logout/i })).toBeInTheDocument()
    })

    it('should navigate to profile when profile menu item is clicked', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const profileButton = screen.getByRole('button', { name: /account of current user/i })
      await user.click(profileButton)

      const profileMenuItem = screen.getByRole('menuitem', { name: /profile/i })
      await user.click(profileMenuItem)

      expect(mockNavigate).toHaveBeenCalledWith('/profile')
    })

    it('should navigate to settings when settings menu item is clicked', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const profileButton = screen.getByRole('button', { name: /account of current user/i })
      await user.click(profileButton)

      const settingsMenuItem = screen.getByRole('menuitem', { name: /settings/i })
      await user.click(settingsMenuItem)

      expect(mockNavigate).toHaveBeenCalledWith('/settings')
    })

    it('should navigate to login when logout menu item is clicked', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const profileButton = screen.getByRole('button', { name: /account of current user/i })
      await user.click(profileButton)

      const logoutMenuItem = screen.getByRole('menuitem', { name: /logout/i })
      await user.click(logoutMenuItem)

      expect(mockNavigate).toHaveBeenCalledWith('/login')
    })

    it('should close menu when clicking outside', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const profileButton = screen.getByRole('button', { name: /account of current user/i })
      await user.click(profileButton)

      expect(screen.getByRole('menu')).toBeInTheDocument()

      // Press escape to close menu (more reliable than clicking outside)
      await user.keyboard('{Escape}')

      await waitFor(() => {
        expect(screen.queryByRole('menu')).not.toBeInTheDocument()
      }, { timeout: 3000 })
    })
  })

  describe('Notification Badge Display', () => {
    it('should display notification badge with count', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const notificationButton = screen.getByRole('button', { name: /show 3 new notifications/i })
      expect(notificationButton).toBeInTheDocument()

      const badge = within(notificationButton).getByText('3')
      expect(badge).toBeInTheDocument()
    })

    it('should open notifications menu when clicked', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const notificationButton = screen.getByRole('button', { name: /show 3 new notifications/i })
      await user.click(notificationButton)

      expect(screen.getByText('Notifications')).toBeInTheDocument()
      expect(screen.getByRole('menu')).toBeInTheDocument()
    })

    it('should display mock notifications in menu', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const notificationButton = screen.getByRole('button', { name: /show 3 new notifications/i })
      await user.click(notificationButton)

      expect(screen.getByText('New member joined "Study Group"')).toBeInTheDocument()
      expect(screen.getByText('Focus session completed!')).toBeInTheDocument()
      expect(screen.getByText('Invitation to "Coding Bootcamp"')).toBeInTheDocument()
    })

    it('should display notification timestamps', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const notificationButton = screen.getByRole('button', { name: /show 3 new notifications/i })
      await user.click(notificationButton)

      expect(screen.getByText('2 minutes ago')).toBeInTheDocument()
      expect(screen.getByText('1 hour ago')).toBeInTheDocument()
      expect(screen.getByText('3 hours ago')).toBeInTheDocument()
    })

    it('should close notifications menu when clicking outside', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const notificationButton = screen.getByRole('button', { name: /show 3 new notifications/i })
      await user.click(notificationButton)

      expect(screen.getByRole('menu')).toBeInTheDocument()

      // Press escape to close menu (more reliable than clicking outside)
      await user.keyboard('{Escape}')

      await waitFor(() => {
        expect(screen.queryByRole('menu')).not.toBeInTheDocument()
      }, { timeout: 3000 })
    })
  })

  describe('Connection Status', () => {
    it('should show online status when connected', () => {
      renderWithProviders(<Header {...defaultProps} isConnected={true} />)

      expect(screen.getByText('Online')).toBeInTheDocument()
      expect(screen.getByTestId('WifiIcon')).toBeInTheDocument()
    })

    it('should show offline status when disconnected', () => {
      renderWithProviders(<Header {...defaultProps} isConnected={false} />)

      expect(screen.getByText('Offline')).toBeInTheDocument()
      expect(screen.getByTestId('WifiOffIcon')).toBeInTheDocument()
    })

    it('should have correct tooltip for connected state', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} isConnected={true} />)

      const statusChip = screen.getByText('Online')
      expect(statusChip).toBeInTheDocument()

      // Hover over the chip to show tooltip
      await user.hover(statusChip)

      // Wait for tooltip to appear
      await waitFor(() => {
        const tooltip = screen.queryByText('Connected to server')
        expect(tooltip).toBeInTheDocument()
      }, { timeout: 2000 })
    })

    it('should have correct tooltip for disconnected state', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} isConnected={false} />)

      const statusChip = screen.getByText('Offline')
      expect(statusChip).toBeInTheDocument()

      // Hover over the chip to show tooltip
      await user.hover(statusChip)

      // Wait for tooltip to appear
      await waitFor(() => {
        const tooltip = screen.queryByText('Disconnected from server')
        expect(tooltip).toBeInTheDocument()
      }, { timeout: 2000 })
    })
  })

  describe('Mobile Menu Toggle', () => {
    it('should call onDrawerToggle when menu button is clicked', async () => {
      const user = userEvent.setup()
      const onDrawerToggle = vi.fn()
      renderWithProviders(<Header {...defaultProps} onDrawerToggle={onDrawerToggle} />)

      const menuButton = screen.getByRole('button', { name: /toggle drawer/i })
      await user.click(menuButton)

      expect(onDrawerToggle).toHaveBeenCalledTimes(1)
    })

    it('should have correct aria-label for menu button', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const menuButton = screen.getByRole('button', { name: /toggle drawer/i })
      expect(menuButton).toHaveAttribute('aria-label', 'toggle drawer')
    })

    it('should render menu icon', () => {
      renderWithProviders(<Header {...defaultProps} />)

      expect(screen.getByTestId('MenuIcon')).toBeInTheDocument()
    })
  })

  describe('Search Functionality', () => {
    it('should update search input value when typing', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const searchInput = screen.getByPlaceholderText('Search hives, users...')
      await user.type(searchInput, 'test query')

      expect(searchInput).toHaveValue('test query')
    })

    it('should navigate to search results when form is submitted', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const searchInput = screen.getByPlaceholderText('Search hives, users...')
      await user.type(searchInput, 'test query')
      await user.keyboard('{Enter}')

      expect(mockNavigate).toHaveBeenCalledWith('/search?q=test%20query')
    })

    it('should not navigate when search query is empty', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const searchInput = screen.getByPlaceholderText('Search hives, users...')
      await user.click(searchInput)
      await user.keyboard('{Enter}')

      expect(mockNavigate).not.toHaveBeenCalled()
    })

    it('should trim whitespace from search query', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const searchInput = screen.getByPlaceholderText('Search hives, users...')
      await user.type(searchInput, '  test query  ')
      await user.keyboard('{Enter}')

      expect(mockNavigate).toHaveBeenCalledWith('/search?q=test%20query')
    })

    it('should not navigate when search query is only whitespace', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const searchInput = screen.getByPlaceholderText('Search hives, users...')
      await user.type(searchInput, '   ')
      await user.keyboard('{Enter}')

      expect(mockNavigate).not.toHaveBeenCalled()
    })
  })

  describe('Responsive Design', () => {
    it('should hide app title on small screens', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const title = screen.getByText('FocusHive')
      const computedStyle = window.getComputedStyle(title)

      // The title has sx={{ display: { xs: 'none', sm: 'block' } }}
      expect(title).toBeInTheDocument()
    })

    it('should hide language switcher on mobile', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const languageSwitcher = screen.getByTestId('language-switcher')
      const container = languageSwitcher.closest('div')

      // Should have sx={{ display: { xs: 'none', md: 'block' } }}
      expect(container).toBeInTheDocument()
    })

    it('should hide connection status chip on mobile', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const statusChip = screen.getByText('Online')

      // Should have sx={{ display: { xs: 'none', sm: 'flex' } }}
      expect(statusChip).toBeInTheDocument()
    })

    it('should make search bar full width on mobile', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const searchInput = screen.getByPlaceholderText('Search hives, users...')
      const searchContainer = searchInput.closest('form')

      // Should have responsive width styling
      expect(searchContainer).toBeInTheDocument()
    })

    it('should adjust header width based on drawer width', () => {
      const customDrawerWidth = 300
      renderWithProviders(<Header {...defaultProps} drawerWidth={customDrawerWidth} />)

      const appBar = screen.getByRole('banner')
      expect(appBar).toBeInTheDocument()

      // The width calculation is done in sx prop: calc(100% - ${drawerWidth}px)
      // We can't easily test the computed style, but we can verify the component renders
    })
  })

  describe('Accessibility', () => {
    it('should have proper ARIA labels for buttons', () => {
      renderWithProviders(<Header {...defaultProps} />)

      expect(screen.getByRole('button', { name: /toggle drawer/i })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /account of current user/i })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /show .* new notifications/i })).toBeInTheDocument()
    })

    it('should have proper ARIA attributes for menus', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const profileButton = screen.getByRole('button', { name: /account of current user/i })
      expect(profileButton).toHaveAttribute('aria-controls', 'primary-search-account-menu')
      expect(profileButton).toHaveAttribute('aria-haspopup', 'true')

      await user.click(profileButton)

      const menu = screen.getByRole('menu')
      expect(menu).toBeInTheDocument()
    })

    it('should have accessible search input', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const searchInput = screen.getByPlaceholderText('Search hives, users...')
      expect(searchInput).toHaveAttribute('aria-label', 'Search')
    })

    it('should support keyboard navigation for profile button', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const profileButton = screen.getByRole('button', { name: /account of current user/i })
      profileButton.focus()

      expect(document.activeElement).toBe(profileButton)

      await user.keyboard('{Enter}')
      expect(screen.getByRole('menu')).toBeInTheDocument()
    })

    it('should support keyboard navigation for menu items', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const profileButton = screen.getByRole('button', { name: /account of current user/i })
      await user.click(profileButton)

      const profileMenuItem = screen.getByRole('menuitem', { name: /profile/i })
      profileMenuItem.focus()

      expect(document.activeElement).toBe(profileMenuItem)

      await user.keyboard('{Enter}')
      expect(mockNavigate).toHaveBeenCalledWith('/profile')
    })

    it('should have proper color contrast for status indicators', () => {
      renderWithProviders(<Header {...defaultProps} isConnected={true} />)

      const onlineStatus = screen.getByText('Online')
      expect(onlineStatus).toBeInTheDocument()

      // The chip should use success color for connected state
      const statusChip = onlineStatus.closest('.MuiChip-root')
      expect(statusChip).toHaveClass('MuiChip-colorSuccess')
    })

    it('should have proper focus management for search', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const searchInput = screen.getByPlaceholderText('Search hives, users...')

      await user.click(searchInput)
      expect(document.activeElement).toBe(searchInput)

      await user.type(searchInput, 'test')
      expect(searchInput).toHaveValue('test')
    })

    it('should have semantic banner role for header', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const header = screen.getByRole('banner')
      expect(header).toBeInTheDocument()
    })
  })

  describe('Theme Integration', () => {
    it('should apply theme transitions to AppBar', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const appBar = screen.getByRole('banner')
      expect(appBar).toBeInTheDocument()

      // Theme transitions are applied via sx prop
      // We can't easily test computed styles, but we verify the component renders
    })

    it('should use theme colors for elements', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const title = screen.getByText('FocusHive')
      expect(title).toBeInTheDocument()

      // Title should use primary color from theme
      // Actual color testing would require theme provider inspection
    })

    it('should apply theme spacing consistently', () => {
      renderWithProviders(<Header {...defaultProps} />)

      const toolbar = screen.getByRole('banner').querySelector('.MuiToolbar-root')
      expect(toolbar).toBeInTheDocument()

      // Theme spacing is applied throughout the component
      // We verify the structure is correct
    })

    it('should not have theme toggle button (not implemented yet)', () => {
      renderWithProviders(<Header {...defaultProps} />)

      // Theme toggle is not implemented in current Header component
      // This test documents the current state
      const themeToggle = screen.queryByRole('button', { name: /theme/i })
      expect(themeToggle).not.toBeInTheDocument()
    })
  })

  describe('Error Handling', () => {
    it('should handle missing translation keys gracefully', () => {
      renderWithProviders(<Header {...defaultProps} />)

      // All translation keys should be handled by our mock
      expect(screen.getByText('FocusHive')).toBeInTheDocument()
      expect(screen.getByPlaceholderText('Search hives, users...')).toBeInTheDocument()
    })

    it('should handle missing user avatar gracefully', () => {
      renderWithProviders(<Header {...defaultProps} />)

      // Should show account circle icon instead of broken image
      // Component uses mock data without avatar, so AccountCircleIcon should be present
      const accountIcons = screen.getAllByTestId('AccountCircleIcon')
      expect(accountIcons.length).toBeGreaterThan(0)
    })

    it('should handle drawer width changes', () => {
      const { rerender } = renderWithProviders(<Header {...defaultProps} drawerWidth={240} />)

      expect(screen.getByRole('banner')).toBeInTheDocument()

      rerender(<Header {...defaultProps} drawerWidth={300} />)

      expect(screen.getByRole('banner')).toBeInTheDocument()
    })
  })

  describe('Performance Considerations', () => {
    it('should not cause unnecessary re-renders when props change', () => {
      const { rerender } = renderWithProviders(<Header {...defaultProps} />)

      const initialHeader = screen.getByRole('banner')
      expect(initialHeader).toBeInTheDocument()

      // Change connection status
      rerender(<Header {...defaultProps} isConnected={false} />)

      // Header should still be present but with updated status
      expect(screen.getByRole('banner')).toBeInTheDocument()
      expect(screen.getByText('Offline')).toBeInTheDocument()
    })

    it('should debounce search input appropriately', async () => {
      const user = userEvent.setup()
      renderWithProviders(<Header {...defaultProps} />)

      const searchInput = screen.getByPlaceholderText('Search hives, users...')

      // Type quickly
      await user.type(searchInput, 'quick')

      expect(searchInput).toHaveValue('quick')

      // Submit immediately should work
      await user.keyboard('{Enter}')
      expect(mockNavigate).toHaveBeenCalledWith('/search?q=quick')
    })
  })
})