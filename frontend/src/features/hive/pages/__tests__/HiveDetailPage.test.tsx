import React from 'react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/test-utils'
import HiveDetailPage from '../HiveDetailPage'

// Mock the useParams hook
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useParams: () => ({ hiveId: 'hive-123' }),
    useNavigate: () => vi.fn(),
  }
})

// Mock hooks
vi.mock('../../../hive/hooks/useHiveDetails', () => ({
  useHiveDetails: vi.fn(() => ({
    hive: {
      id: 'hive-123',
      name: 'Study Together',
      description: 'A focused study group for exam preparation',
      type: 'PUBLIC',
      category: 'STUDY',
      currentMembers: 5,
      maxMembers: 10,
      createdBy: 'user-456',
      createdAt: new Date('2025-01-01'),
      isActive: true,
      tags: ['study', 'focus', 'exams'],
    },
    isLoading: false,
    error: null,
  })),
}))

vi.mock('../../../presence/hooks/useHivePresence', () => ({
  useHivePresence: vi.fn(() => ({
    members: [
      {
        id: 'user-1',
        name: 'John Doe',
        avatar: '/avatar1.jpg',
        status: 'ONLINE',
        focusTime: 45,
      },
      {
        id: 'user-2',
        name: 'Jane Smith',
        avatar: '/avatar2.jpg',
        status: 'IN_FOCUS',
        focusTime: 25,
      },
      {
        id: 'user-3',
        name: 'Bob Wilson',
        status: 'AWAY',
      },
    ],
    isLoading: false,
  })),
}))

vi.mock('../../../timer/hooks/useTimer', () => ({
  useTimer: vi.fn(() => ({
    time: 1500, // 25 minutes in seconds
    isRunning: false,
    start: vi.fn(),
    pause: vi.fn(),
    reset: vi.fn(),
  })),
}))

vi.mock('../../../chat/hooks/useHiveChat', () => ({
  useHiveChat: vi.fn(() => ({
    messages: [
      {
        id: 'msg-1',
        userId: 'user-1',
        userName: 'John Doe',
        content: 'Let\'s focus for the next 25 minutes!',
        timestamp: new Date(),
      },
    ],
    sendMessage: vi.fn(),
    isConnected: true,
  })),
}))

describe('HiveDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render hive detail page with all sections', () => {
      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByTestId('hive-detail-page')).toBeInTheDocument()
      expect(screen.getByTestId('hive-info-header')).toBeInTheDocument()
      expect(screen.getByTestId('member-list')).toBeInTheDocument()
      expect(screen.getByTestId('chat-section')).toBeInTheDocument()
      expect(screen.getByTestId('timer-section')).toBeInTheDocument()
    })

    it('should display hive name and description', () => {
      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByText('Study Together')).toBeInTheDocument()
      expect(screen.getByText('A focused study group for exam preparation')).toBeInTheDocument()
    })

    it('should show hive metadata', () => {
      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByText(/public/i)).toBeInTheDocument()
      expect(screen.getByText(/study/i)).toBeInTheDocument()
      expect(screen.getByText(/5 \/ 10 members/i)).toBeInTheDocument()
    })

    it('should display hive tags', () => {
      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByText('study')).toBeInTheDocument()
      expect(screen.getByText('focus')).toBeInTheDocument()
      expect(screen.getByText('exams')).toBeInTheDocument()
    })
  })

  describe('Member List', () => {
    it('should display all active members', () => {
      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByText('John Doe')).toBeInTheDocument()
      expect(screen.getByText('Jane Smith')).toBeInTheDocument()
      expect(screen.getByText('Bob Wilson')).toBeInTheDocument()
    })

    it('should show member presence status', () => {
      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByTestId('presence-ONLINE')).toBeInTheDocument()
      expect(screen.getByTestId('presence-IN_FOCUS')).toBeInTheDocument()
      expect(screen.getByTestId('presence-AWAY')).toBeInTheDocument()
    })

    it('should display focus time for members in focus', () => {
      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByText(/45 min/i)).toBeInTheDocument()
      expect(screen.getByText(/25 min/i)).toBeInTheDocument()
    })

    it('should show member avatars when available', () => {
      renderWithProviders(<HiveDetailPage />)

      const avatar1 = screen.getByAltText('John Doe')
      expect(avatar1).toHaveAttribute('src', '/avatar1.jpg')

      const avatar2 = screen.getByAltText('Jane Smith')
      expect(avatar2).toHaveAttribute('src', '/avatar2.jpg')
    })
  })

  describe('Timer Section', () => {
    it('should display focus timer', () => {
      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByTestId('focus-timer')).toBeInTheDocument()
      expect(screen.getByText('25:00')).toBeInTheDocument()
    })

    it('should have timer controls', () => {
      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByRole('button', { name: /start/i })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /reset/i })).toBeInTheDocument()
    })

    it('should start timer when start button is clicked', async () => {
      const user = userEvent.setup()
      const { useTimer } = await import('../../../timer/hooks/useTimer')
      const start = vi.fn()
      ;(useTimer as any).mockReturnValue({
        time: 1500,
        isRunning: false,
        start,
        pause: vi.fn(),
        reset: vi.fn(),
      })

      renderWithProviders(<HiveDetailPage />)

      const startButton = screen.getByRole('button', { name: /start/i })
      await user.click(startButton)

      expect(start).toHaveBeenCalled()
    })

    it('should show pause button when timer is running', () => {
      const { useTimer } = require('../../../timer/hooks/useTimer')
      useTimer.mockReturnValue({
        time: 1200,
        isRunning: true,
        start: vi.fn(),
        pause: vi.fn(),
        reset: vi.fn(),
      })

      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByRole('button', { name: /pause/i })).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: /start/i })).not.toBeInTheDocument()
    })
  })

  describe('Chat Section', () => {
    it('should display chat messages', () => {
      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByText("Let's focus for the next 25 minutes!")).toBeInTheDocument()
      expect(screen.getByText('John Doe')).toBeInTheDocument()
    })

    it('should have message input field', () => {
      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByPlaceholderText(/type a message/i)).toBeInTheDocument()
    })

    it('should have send button', () => {
      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByRole('button', { name: /send/i })).toBeInTheDocument()
    })

    it('should send message when send button is clicked', async () => {
      const user = userEvent.setup()
      const { useHiveChat } = await import('../../../chat/hooks/useHiveChat')
      const sendMessage = vi.fn()
      ;(useHiveChat as any).mockReturnValue({
        messages: [],
        sendMessage,
        isConnected: true,
      })

      renderWithProviders(<HiveDetailPage />)

      const input = screen.getByPlaceholderText(/type a message/i)
      await user.type(input, 'Hello everyone!')

      const sendButton = screen.getByRole('button', { name: /send/i })
      await user.click(sendButton)

      expect(sendMessage).toHaveBeenCalledWith('Hello everyone!')
    })

    it('should show connection status', () => {
      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByTestId('chat-connection-status')).toBeInTheDocument()
      expect(screen.getByText(/connected/i)).toBeInTheDocument()
    })
  })

  describe('Actions', () => {
    it('should have join hive button for non-members', () => {
      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByRole('button', { name: /join hive/i })).toBeInTheDocument()
    })

    it('should have leave hive button for members', () => {
      const { useHiveDetails } = require('../../../hive/hooks/useHiveDetails')
      useHiveDetails.mockReturnValue({
        hive: {
          id: 'hive-123',
          name: 'Study Together',
          description: 'A focused study group',
          type: 'PUBLIC',
          category: 'STUDY',
          currentMembers: 5,
          maxMembers: 10,
          isMember: true,
        },
        isLoading: false,
        error: null,
      })

      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByRole('button', { name: /leave hive/i })).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: /join hive/i })).not.toBeInTheDocument()
    })

    it('should disable join button when hive is full', () => {
      const { useHiveDetails } = require('../../../hive/hooks/useHiveDetails')
      useHiveDetails.mockReturnValue({
        hive: {
          id: 'hive-123',
          name: 'Study Together',
          description: 'A focused study group',
          type: 'PUBLIC',
          category: 'STUDY',
          currentMembers: 10,
          maxMembers: 10,
        },
        isLoading: false,
        error: null,
      })

      renderWithProviders(<HiveDetailPage />)

      const joinButton = screen.getByRole('button', { name: /hive full/i })
      expect(joinButton).toBeDisabled()
    })

    it('should have settings button for hive owner', () => {
      const { useHiveDetails } = require('../../../hive/hooks/useHiveDetails')
      useHiveDetails.mockReturnValue({
        hive: {
          id: 'hive-123',
          name: 'Study Together',
          description: 'A focused study group',
          type: 'PUBLIC',
          category: 'STUDY',
          currentMembers: 5,
          maxMembers: 10,
          createdBy: 'current-user',
          isOwner: true,
        },
        isLoading: false,
        error: null,
      })

      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByRole('button', { name: /settings/i })).toBeInTheDocument()
    })
  })

  describe('Loading State', () => {
    it('should show loading spinner when hive is loading', () => {
      const { useHiveDetails } = require('../../../hive/hooks/useHiveDetails')
      useHiveDetails.mockReturnValue({
        hive: null,
        isLoading: true,
        error: null,
      })

      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByRole('progressbar')).toBeInTheDocument()
      expect(screen.queryByTestId('hive-info-header')).not.toBeInTheDocument()
    })
  })

  describe('Error State', () => {
    it('should show error message when hive fails to load', () => {
      const { useHiveDetails } = require('../../../hive/hooks/useHiveDetails')
      useHiveDetails.mockReturnValue({
        hive: null,
        isLoading: false,
        error: new Error('Failed to load hive'),
      })

      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByText(/failed to load hive/i)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument()
    })
  })

  describe('Responsive Layout', () => {
    it('should stack sections vertically on mobile', () => {
      global.innerWidth = 375
      renderWithProviders(<HiveDetailPage />)

      const page = screen.getByTestId('hive-detail-page')
      expect(page).toHaveStyle({ flexDirection: 'column' })
    })

    it('should show sections side by side on desktop', () => {
      global.innerWidth = 1920
      renderWithProviders(<HiveDetailPage />)

      const page = screen.getByTestId('hive-detail-page')
      const styles = window.getComputedStyle(page)
      expect(styles.flexDirection).toContain('row')
    })
  })

  describe('Accessibility', () => {
    it('should have proper heading hierarchy', () => {
      renderWithProviders(<HiveDetailPage />)

      const h1 = screen.getByRole('heading', { level: 1 })
      expect(h1).toHaveTextContent('Study Together')

      const h2s = screen.getAllByRole('heading', { level: 2 })
      expect(h2s.length).toBeGreaterThan(0)
    })

    it('should have proper ARIA labels', () => {
      renderWithProviders(<HiveDetailPage />)

      expect(screen.getByLabelText(/member list/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/chat messages/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/focus timer/i)).toBeInTheDocument()
    })

    it('should announce member status changes', async () => {
      renderWithProviders(<HiveDetailPage />)

      // Check for live region
      const liveRegion = screen.getByRole('status')
      expect(liveRegion).toHaveAttribute('aria-live', 'polite')
    })
  })
})