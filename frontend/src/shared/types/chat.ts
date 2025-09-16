// Chat related types
import {User} from './auth'

export interface ChatMessage {
  id: string
  content: string
  authorId: string
  author: User
  hiveId: string
  type: 'text' | 'emoji' | 'system' | 'image' | 'file'
  metadata?: {
    fileName?: string
    fileSize?: number
    mimeType?: string
    imageUrl?: string
    systemEvent?: 'user_joined' | 'user_left' | 'session_started' | 'session_ended'
  }
  replyTo?: string // message ID if this is a reply
  reactions: MessageReaction[]
  isEdited: boolean
  editedAt?: string
  createdAt: string
  updatedAt: string
}

export interface MessageReaction {
  id: string
  messageId: string
  userId: string
  user: User
  emoji: string
  createdAt: string
}

export interface SendMessageRequest {
  hiveId: string
  content: string
  type?: ChatMessage['type']
  replyTo?: string
  metadata?: ChatMessage['metadata']
}

export interface TypingIndicator {
  userId: string
  user: User
  hiveId: string
  isTyping: boolean
  timestamp: string
}

export interface ChatState {
  messages: Record<string, ChatMessage[]> // keyed by hiveId
  typingUsers: Record<string, TypingIndicator[]> // keyed by hiveId
  isLoading: boolean
  error: string | null
  hasMoreMessages: Record<string, boolean> // keyed by hiveId
}

export interface EmojiPickerData {
  id: string
  name: string
  colons: string
  text: string
  emoticons: string[]
  skin: number
  native: string
}

export interface ChatContextType {
  chatState: ChatState
  sendMessage: (request: SendMessageRequest) => Promise<void>
  editMessage: (messageId: string, content: string) => Promise<void>
  deleteMessage: (messageId: string) => Promise<void>
  addReaction: (messageId: string, emoji: string) => Promise<void>
  removeReaction: (messageId: string, emoji: string) => Promise<void>
  loadMoreMessages: (hiveId: string) => Promise<void>
  setTyping: (hiveId: string, isTyping: boolean) => void
  markMessagesAsRead: (hiveId: string) => void
}

export interface MessageInputProps {
  hiveId: string
  onSendMessage: (content: string, type?: ChatMessage['type']) => void
  placeholder?: string
  maxLength?: number
  disabled?: boolean
  showEmojiPicker?: boolean
  allowFileUpload?: boolean
}

export interface MessageListProps {
  hiveId: string
  messages: ChatMessage[]
  onLoadMore?: () => void
  hasMore?: boolean
  isLoading?: boolean
  currentUserId: string
}