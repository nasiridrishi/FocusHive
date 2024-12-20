import React from 'react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { screen, within, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/test-utils'
import ChatWindow from '../ChatWindow'
import { ConnectionState } from '../../../../shared/contexts/WebSocketContext'
import { ChatMessage } from '../../../../shared/types/chat'

// Mock the child components
vi.mock('../MessageList', () => ({
  default: ({ messages, onLoadMore, onEditMessage, onDeleteMessage, onReplyMessage, onReaction, onRemoveReaction, currentUserId, typingUsers }: any) => (
    <div data-testid="message-list">
      <div data-testid="messages-count">{messages.length} messages</div>
      {typingUsers?.length > 0 && (
        <div data-testid="typing-users">{typingUsers.length} users typing</div>
      )}
      <button onClick={onLoadMore}>Load More</button>
      <button onClick={() => onEditMessage('msg1', 'edited')}>Edit</button>
      <button onClick={() => onDeleteMessage('msg1')}>Delete</button>
      <button onClick={() => onReplyMessage({ id: 'msg1', content: 'Reply to this' })}>Reply</button>
      <button onClick={() => onReaction('msg1', '👍')}>React</button>
      <button onClick={() => onRemoveReaction('msg1', '👍')}>Remove React</button>
      <div data-testid="current-user">{currentUserId}</div>
    </div>
  )
}))

vi.mock('../MessageInput', () => ({
  default: ({ onSendMessage, onTypingStart, onTypingStop, disabled, replyTo, onCancelReply }: any) => (
    <div data-testid="message-input">
      <input 
        data-testid="message-input-field"
        onFocus={onTypingStart}
        onBlur={onTypingStop}
        disabled={disabled}
      />
      <button 
        data-testid="send-button"
        onClick={() => onSendMessage('Test message')}
        disabled={disabled}
      >
        Send
      </button>
      {replyTo && (
        <div data-testid="reply-to">
          Replying to: {replyTo.content}
          <button onClick={onCancelReply}>Cancel</button>
        </div>
      )}
    </div>
  )
}))

vi.mock('../TypingIndicator', () => ({
  default: ({ typingUsers }: any) => (
    <div data-testid="typing-indicator">
      {typingUsers?.map((user: any) => user.user.name).join(', ')} typing...
    </div>
  )
}))

// Mock contexts
const mockWebSocketContext = {
  connectionState: ConnectionState.CONNECTED,
  isConnected: true,
}

const mockChatContext = {
  chatState: {
    messages: {},
    typingUsers: {},
    hasMoreMessages: {},
    isLoading: false,
    error: null,
    unreadCounts: {},
    lastReadMessages: {},
  },
  sendMessage: vi.fn(),
  editMessage: vi.fn(),
  deleteMessage: vi.fn(),
  addReaction: vi.fn(),
  removeReaction: vi.fn(),
  loadMoreMessages: vi.fn(),
  setTyping: vi.fn(),
  markMessagesAsRead: vi.fn(),
}

vi.mock('../../../../shared/contexts/WebSocketContext', () => ({
  ConnectionState: {
    CONNECTED: 'CONNECTED',
    CONNECTING: 'CONNECTING',
    RECONNECTING: 'RECONNECTING',
    ERROR: 'ERROR',
    DISCONNECTED: 'DISCONNECTED',
  },
  useWebSocket: () => mockWebSocketContext,
}))

vi.mock('../../../../shared/contexts/ChatContext', () => ({
  useChat: () => mockChatContext,
}))

const createMockMessage = (overrides?: Partial<ChatMessage>): ChatMessage => ({
  id: 'msg1',
  hiveId: 'hive1',
  authorId: 'user1',
  author: {
    id: 'user1',
    email: 'john.doe@example.com',
    username: 'johndoe',
    firstName: 'John',
    lastName: 'Doe',
    name: 'John Doe',
    avatar: '/avatar1.jpg',
    isEmailVerified: true,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  content: 'Test message',
  type: 'text',
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
  isEdited: false,
  reactions: [],
  ...overrides,
})

describe('ChatWindow', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Reset mock contexts to default state
    mockWebSocketContext.connectionState = ConnectionState.CONNECTED
    mockWebSocketContext.isConnected = true
    mockChatContext.chatState = {
      messages: {},
      typingUsers: {},
      hasMoreMessages: {},
      isLoading: false,
      error: null,
      unreadCounts: {},
      lastReadMessages: {},
    }
  })

  describe('Rendering', () => {
    it('should render chat window with header', () => {
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          hiveName="Test Hive"
          currentUserId="user1"
        />
      )

      expect(screen.getByText('Test Hive')).toBeInTheDocument()
      expect(screen.getByTestId('message-list')).toBeInTheDocument()
      expect(screen.getByTestId('message-input')).toBeInTheDocument()
    })

    it('should render minimized state', () => {
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          hiveName="Test Hive"
          currentUserId="user1"
          defaultMinimized={true}
        />
      )

      expect(screen.getByText('Test Hive')).toBeInTheDocument()
      expect(screen.queryByTestId('message-list')).not.toBeInTheDocument()
      expect(screen.queryByTestId('message-input')).not.toBeInTheDocument()
    })

    it('should apply custom className', () => {
      const { container } = renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
          className="custom-chat"
        />
      )

      expect(container.querySelector('.custom-chat')).toBeInTheDocument()
    })

    it('should show default hive name when not provided', () => {
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      expect(screen.getByText('Chat')).toBeInTheDocument()
    })
  })

  describe('Connection Status', () => {
    it('should show connected status', () => {
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      expect(screen.getByText('Connected')).toBeInTheDocument()
    })

    it('should show connecting status', () => {
      mockWebSocketContext.connectionState = ConnectionState.CONNECTING
      mockWebSocketContext.isConnected = false

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      expect(screen.getByText('Connecting...')).toBeInTheDocument()
    })

    it('should show reconnecting status', () => {
      mockWebSocketContext.connectionState = ConnectionState.RECONNECTING
      mockWebSocketContext.isConnected = false

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      expect(screen.getByText('Reconnecting...')).toBeInTheDocument()
    })

    it('should show error status', () => {
      mockWebSocketContext.connectionState = ConnectionState.ERROR
      mockWebSocketContext.isConnected = false

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      expect(screen.getByText('Error')).toBeInTheDocument()
    })

    it('should show disconnected status', () => {
      mockWebSocketContext.connectionState = ConnectionState.DISCONNECTED
      mockWebSocketContext.isConnected = false

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      expect(screen.getByText('Disconnected')).toBeInTheDocument()
    })

    it('should show reconnect button when disconnected', () => {
      mockWebSocketContext.isConnected = false

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      expect(screen.getByTitle('Reconnect')).toBeInTheDocument()
    })

    it('should disable message input when disconnected', () => {
      mockWebSocketContext.isConnected = false

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      const sendButton = screen.getByTestId('send-button')
      expect(sendButton).toBeDisabled()
    })
  })

  describe('Window Controls', () => {
    it('should minimize and expand window', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      // Initially expanded
      expect(screen.getByTestId('message-list')).toBeInTheDocument()

      // Click minimize button
      const minimizeButton = screen.getByTitle('Minimize')
      await user.click(minimizeButton)

      // Should be minimized
      expect(screen.queryByTestId('message-list')).not.toBeInTheDocument()

      // Click header to expand
      const header = screen.getByText('Chat')
      await user.click(header)

      // Should be expanded again
      expect(screen.getByTestId('message-list')).toBeInTheDocument()
    })

    it('should toggle fullscreen mode', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
          allowFullscreen={true}
        />
      )

      const fullscreenButton = screen.getByTitle('Fullscreen')
      await user.click(fullscreenButton)

      // Should show exit fullscreen button
      expect(screen.getByTitle('Exit Fullscreen')).toBeInTheDocument()

      // Click to exit fullscreen
      await user.click(screen.getByTitle('Exit Fullscreen'))

      // Should show fullscreen button again
      expect(screen.getByTitle('Fullscreen')).toBeInTheDocument()
    })

    it('should not show fullscreen button when disabled', () => {
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
          allowFullscreen={false}
        />
      )

      expect(screen.queryByTitle('Fullscreen')).not.toBeInTheDocument()
    })

    it('should call onClose when close button clicked', async () => {
      const handleClose = vi.fn()
      const user = userEvent.setup()

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
          onClose={handleClose}
        />
      )

      const closeButton = screen.getByTitle('Close')
      await user.click(closeButton)

      expect(handleClose).toHaveBeenCalledTimes(1)
    })

    it('should not show close button when onClose not provided', () => {
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      expect(screen.queryByTitle('Close')).not.toBeInTheDocument()
    })
  })

  describe('Messages Display', () => {
    it('should display messages from chat state', () => {
      const messages = [
        createMockMessage({ id: 'msg1', content: 'Message 1' }),
        createMockMessage({ id: 'msg2', content: 'Message 2' }),
      ]

      mockChatContext.chatState.messages['hive1'] = messages

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      expect(screen.getByTestId('messages-count')).toHaveTextContent('2 messages')
    })

    it('should display empty state when no messages', () => {
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      expect(screen.getByTestId('messages-count')).toHaveTextContent('0 messages')
    })

    it('should pass current user ID to message list', () => {
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user123"
        />
      )

      expect(screen.getByTestId('current-user')).toHaveTextContent('user123')
    })
  })

  describe('Typing Indicators', () => {
    it('should display typing users in header', () => {
      const typingUsers = [
        { userId: 'user2', user: { id: 'user2', name: 'Jane' } },
        { userId: 'user3', user: { id: 'user3', name: 'Bob' } },
      ]

      mockChatContext.chatState.typingUsers['hive1'] = typingUsers

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      expect(screen.getByTestId('typing-indicator')).toHaveTextContent('Jane, Bob typing...')
    })

    it('should not show typing indicator when minimized', () => {
      const typingUsers = [
        { userId: 'user2', user: { id: 'user2', name: 'Jane' } },
      ]

      mockChatContext.chatState.typingUsers['hive1'] = typingUsers

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
          defaultMinimized={true}
        />
      )

      expect(screen.queryByTestId('typing-indicator')).not.toBeInTheDocument()
    })

    it('should pass typing users to message list', () => {
      const typingUsers = [
        { userId: 'user2', user: { id: 'user2', name: 'Jane' } },
      ]

      mockChatContext.chatState.typingUsers['hive1'] = typingUsers

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      expect(screen.getByTestId('typing-users')).toHaveTextContent('1 users typing')
    })
  })

  describe('Message Actions', () => {
    it('should handle sending a message', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      const sendButton = screen.getByTestId('send-button')
      await user.click(sendButton)

      expect(mockChatContext.sendMessage).toHaveBeenCalledWith({
        hiveId: 'hive1',
        content: 'Test message',
        type: 'text',
        replyTo: undefined,
      })
    })

    it('should handle editing a message', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      const editButton = screen.getByText('Edit')
      await user.click(editButton)

      expect(mockChatContext.editMessage).toHaveBeenCalledWith('msg1', 'edited')
    })

    it('should handle deleting a message', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      const deleteButton = screen.getByText('Delete')
      await user.click(deleteButton)

      expect(mockChatContext.deleteMessage).toHaveBeenCalledWith('msg1')
    })

    it('should handle adding a reaction', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      const reactButton = screen.getByText('React')
      await user.click(reactButton)

      expect(mockChatContext.addReaction).toHaveBeenCalledWith('msg1', '👍')
    })

    it('should handle removing a reaction', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      const removeReactButton = screen.getByText('Remove React')
      await user.click(removeReactButton)

      expect(mockChatContext.removeReaction).toHaveBeenCalledWith('msg1', '👍')
    })

    it('should handle loading more messages', async () => {
      const user = userEvent.setup()
      mockChatContext.chatState.hasMoreMessages['hive1'] = true

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      const loadMoreButton = screen.getByText('Load More')
      await user.click(loadMoreButton)

      expect(mockChatContext.loadMoreMessages).toHaveBeenCalledWith('hive1')
    })
  })

  describe('Reply Feature', () => {
    it('should handle replying to a message', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      const replyButton = screen.getByText('Reply')
      await user.click(replyButton)

      // Should show reply indicator
      expect(screen.getByTestId('reply-to')).toHaveTextContent('Replying to: Reply to this')

      // Send message with reply
      const sendButton = screen.getByTestId('send-button')
      await user.click(sendButton)

      expect(mockChatContext.sendMessage).toHaveBeenCalledWith({
        hiveId: 'hive1',
        content: 'Test message',
        type: 'text',
        replyTo: 'msg1',
      })
    })

    it('should cancel reply', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      // Start replying
      const replyButton = screen.getByText('Reply')
      await user.click(replyButton)
      expect(screen.getByTestId('reply-to')).toBeInTheDocument()

      // Cancel reply
      const cancelButton = screen.getByText('Cancel')
      await user.click(cancelButton)

      // Reply indicator should be gone
      expect(screen.queryByTestId('reply-to')).not.toBeInTheDocument()
    })

    it('should expand window when replying while minimized', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
          defaultMinimized={true}
        />
      )

      // Initially minimized
      expect(screen.queryByTestId('message-list')).not.toBeInTheDocument()

      // Expand and click reply
      const header = screen.getByText('Chat')
      await user.click(header)
      
      const replyButton = screen.getByText('Reply')
      await user.click(replyButton)

      // Should remain expanded
      expect(screen.getByTestId('message-list')).toBeInTheDocument()
    })
  })

  describe('Typing Status', () => {
    it('should handle typing start', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      const inputField = screen.getByTestId('message-input-field')
      await user.click(inputField)

      expect(mockChatContext.setTyping).toHaveBeenCalledWith('hive1', true)
    })

    it('should handle typing stop', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      const inputField = screen.getByTestId('message-input-field')
      await user.click(inputField)
      await user.tab() // Blur the input

      expect(mockChatContext.setTyping).toHaveBeenCalledWith('hive1', false)
    })
  })

  describe('Read Receipts', () => {
    it('should mark messages as read when window is opened', async () => {
      const messages = [
        createMockMessage({ id: 'msg1' }),
        createMockMessage({ id: 'msg2' }),
      ]
      mockChatContext.chatState.messages['hive1'] = messages

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      await waitFor(() => {
        expect(mockChatContext.markMessagesAsRead).toHaveBeenCalledWith('hive1')
      })
    })

    it('should not mark as read when minimized', () => {
      const messages = [
        createMockMessage({ id: 'msg1' }),
      ]
      mockChatContext.chatState.messages['hive1'] = messages

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
          defaultMinimized={true}
        />
      )

      expect(mockChatContext.markMessagesAsRead).not.toHaveBeenCalled()
    })

    it('should mark as read when expanded from minimized', async () => {
      const user = userEvent.setup()
      const messages = [
        createMockMessage({ id: 'msg1' }),
      ]
      mockChatContext.chatState.messages['hive1'] = messages

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
          defaultMinimized={true}
        />
      )

      // Initially not called
      expect(mockChatContext.markMessagesAsRead).not.toHaveBeenCalled()

      // Expand window
      const header = screen.getByText('Chat')
      await user.click(header)

      await waitFor(() => {
        expect(mockChatContext.markMessagesAsRead).toHaveBeenCalledWith('hive1')
      })
    })
  })

  describe('Error Handling', () => {
    it('should display error from chat state', async () => {
      mockChatContext.chatState.error = 'Connection failed'

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      await waitFor(() => {
        expect(screen.getByText('Connection failed')).toBeInTheDocument()
      })
    })

    it('should handle send message error', async () => {
      const user = userEvent.setup()
      mockChatContext.sendMessage.mockRejectedValueOnce(new Error('Send failed'))

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      const sendButton = screen.getByTestId('send-button')
      await user.click(sendButton)

      await waitFor(() => {
        expect(screen.getByText('Send failed')).toBeInTheDocument()
      })
    })

    it('should handle edit message error', async () => {
      const user = userEvent.setup()
      mockChatContext.editMessage.mockRejectedValueOnce(new Error('Edit failed'))

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      const editButton = screen.getByText('Edit')
      await user.click(editButton)

      await waitFor(() => {
        expect(screen.getByText('Edit failed')).toBeInTheDocument()
      })
    })

    it('should close error alert', async () => {
      const user = userEvent.setup()
      mockChatContext.chatState.error = 'Test error'

      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
        />
      )

      const alert = await screen.findByText('Test error')
      expect(alert).toBeInTheDocument()

      // Find and click the close button in the alert
      const alertContainer = alert.closest('.MuiAlert-root') as HTMLElement
      const closeButton = within(alertContainer).getByRole('button')
      await user.click(closeButton)

      await waitFor(() => {
        expect(screen.queryByText('Test error')).not.toBeInTheDocument()
      })
    })
  })

  describe('Accessibility', () => {
    it('should have proper ARIA labels', () => {
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          hiveName="Test Hive"
          currentUserId="user1"
        />
      )

      expect(screen.getByTitle('Minimize')).toBeInTheDocument()
      expect(screen.getByTitle('Fullscreen')).toBeInTheDocument()
    })

    it('should support keyboard navigation', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <ChatWindow
          hiveId="hive1"
          currentUserId="user1"
          onClose={vi.fn()}
        />
      )

      // Tab through controls
      await user.tab()
      expect(screen.getByTitle('Minimize')).toHaveFocus()

      await user.tab()
      expect(screen.getByTitle('Fullscreen')).toHaveFocus()

      await user.tab()
      expect(screen.getByTitle('Close')).toHaveFocus()
    })
  })
})
