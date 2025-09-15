/**
 * Chat Contracts
 * Core interfaces for chat and messaging functionality in FocusHive
 *
 * The chat system provides real-time messaging within hives, including
 * direct messages, group chats, and threaded conversations.
 */

import { ChatMessage } from './websocket';

// Re-export WebSocket types
export type { ChatMessage } from './websocket';

// Create aliases for commonly expected types
export type ChatTypingIndicator = TypingIndicator;
export type ChatHistory = {
  messages: ExtendedChatMessage[];
  roomId?: string;
  total?: number;
  hasMore?: boolean;
};
export type ChatSearchParams = SearchMessagesRequest;
export type ChatReadReceipt = {
  messageId: string;
  userId: string;
  readAt: string;
};
export type ChatNotification = {
  id: string;
  type: 'message' | 'mention' | 'reaction';
  roomId: string;
  messageId?: string;
  fromUserId: string;
  toUserId: string;
  content: string;
  timestamp: string;
  isRead: boolean;
};
export type ChatWebSocketEvent = {
  type: 'message' | 'typing' | 'reaction' | 'read_receipt' | 'user_joined' | 'user_left';
  roomId: string;
  data: any;
  timestamp: string;
};
export type BatchMarkAsReadRequest = {
  messageIds: string[];
  readAt?: string;
};

/**
 * Message types
 */
export type MessageType =
  | 'text'          // Plain text message
  | 'image'         // Image attachment
  | 'file'          // File attachment
  | 'audio'         // Audio message
  | 'video'         // Video message
  | 'code'          // Code snippet
  | 'system'        // System message
  | 'announcement'  // Announcement
  | 'poll'          // Poll/survey
  | 'task';         // Task assignment

/**
 * Message status
 */
export type MessageStatus =
  | 'sending'       // Being sent
  | 'sent'          // Successfully sent
  | 'delivered'     // Delivered to recipients
  | 'read'          // Read by recipients
  | 'failed'        // Failed to send
  | 'deleted';      // Deleted message

/**
 * Chat types
 */
export type ChatType =
  | 'hive'          // Hive group chat
  | 'direct'        // Direct message
  | 'thread'        // Thread reply
  | 'announcement'  // Announcement channel
  | 'support';      // Support chat

/**
 * Reaction types
 */
export interface MessageReaction {
  emoji: string;
  userId: string;
  userName: string;
  timestamp: string;
}

/**
 * Message attachment
 */
export interface MessageAttachment {
  id: string;
  name: string;
  type: string;       // MIME type
  size: number;       // in bytes
  url: string;
  thumbnailUrl?: string;
  metadata?: {
    width?: number;
    height?: number;
    duration?: number;  // for audio/video
    pages?: number;     // for documents
  };
}

/**
 * Message mention
 */
export interface MessageMention {
  userId: string;
  userName: string;
  startIndex: number; // Position in text
  endIndex: number;   // Position in text
}

/**
 * Extended chat message with additional properties
 */
export interface ExtendedChatMessage extends ChatMessage {
  type: MessageType;
  status: MessageStatus;
  readBy?: Array<{
    userId: string;
    readAt: string;
  }>;
  deliveredTo?: Array<{
    userId: string;
    deliveredAt: string;
  }>;
  editHistory?: Array<{
    text: string;
    editedAt: string;
    editedBy: string;
  }>;
  metadata?: Record<string, any>;
}

/**
 * Chat thread
 */
export interface ChatThread {
  id: string;
  parentMessageId: string;
  hiveId: string;
  startedBy: string;
  startedAt: string;
  lastReplyAt: string;
  replyCount: number;
  participants: string[];
  messages: ExtendedChatMessage[];
  isLocked?: boolean;
  isPinned?: boolean;
}

/**
 * Chat room/channel
 */
export interface ChatRoom {
  id: string;
  type: ChatType;
  name?: string;
  description?: string;
  hiveId?: string;
  participants: string[];
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  lastMessageAt?: string;
  lastMessage?: ExtendedChatMessage;
  unreadCount?: number;
  settings?: ChatRoomSettings;
  isPinned?: boolean;
  isMuted?: boolean;
  isArchived?: boolean;
}

/**
 * Chat room settings
 */
export interface ChatRoomSettings {
  allowFileUpload?: boolean;
  allowVoiceMessages?: boolean;
  allowVideoMessages?: boolean;
  allowReactions?: boolean;
  allowThreads?: boolean;
  allowEditing?: boolean;
  allowDeleting?: boolean;
  maxMessageLength?: number;
  maxFileSize?: number;       // in bytes
  allowedFileTypes?: string[]; // MIME types
  messageRetention?: number;   // days to keep messages
  slowMode?: {
    enabled: boolean;
    interval: number;         // milliseconds between messages
  };
  moderation?: {
    enabled: boolean;
    keywords?: string[];
    requireApproval?: boolean;
  };
}

/**
 * Chat notification preferences
 */
export interface ChatNotificationPreferences {
  allMessages: boolean;
  mentions: boolean;
  replies: boolean;
  reactions: boolean;
  announcements: boolean;
  directMessages: boolean;
  keywords?: string[];
  mutedRooms?: string[];
  soundEnabled: boolean;
  desktopEnabled: boolean;
  mobileEnabled: boolean;
}

/**
 * Typing indicator
 */
export interface TypingIndicator {
  userId: string;
  userName: string;
  roomId: string;
  startedAt: string;
  isTyping: boolean;
}

/**
 * Message search request
 */
export interface SearchMessagesRequest {
  query?: string;
  roomId?: string;
  userId?: string;
  type?: MessageType;
  startDate?: string;
  endDate?: string;
  hasAttachments?: boolean;
  hasReactions?: boolean;
  isEdited?: boolean;
  limit?: number;
  offset?: number;
  sortBy?: 'relevance' | 'date';
  sortOrder?: 'asc' | 'desc';
}

/**
 * Send message request
 */
export interface SendMessageRequest {
  roomId?: string;
  text?: string;
  content?: string;       // Alias for text
  hiveId?: string;       // Optional hive context
  type?: MessageType;
  attachments?: File[] | MessageAttachment[];
  replyToId?: string;
  parentMessageId?: string; // Alias for replyToId
  mentions?: string[];    // user IDs
  metadata?: Record<string, any>;
}

/**
 * Update message request
 */
export interface UpdateMessageRequest {
  text?: string;
  content?: string;       // Alias for text
  attachments?: MessageAttachment[];
}

/**
 * Create chat room request
 */
export interface CreateChatRoomRequest {
  type: ChatType;
  name?: string;
  description?: string;
  hiveId?: string;
  participants?: string[];
  settings?: Partial<ChatRoomSettings>;
}

/**
 * Chat statistics
 */
export interface ChatStatistics {
  userId?: string;
  roomId?: string;
  period: 'day' | 'week' | 'month' | 'all';
  startDate: string;
  endDate: string;
  messagesSent: number;
  messagesReceived: number;
  averageMessageLength: number;
  activeHours: Record<string, number>; // hour -> message count
  topEmojis: Array<{
    emoji: string;
    count: number;
  }>;
  topParticipants?: Array<{
    userId: string;
    userName: string;
    messageCount: number;
  }>;
  engagement: {
    reactions: number;
    replies: number;
    mentions: number;
  };
}

/**
 * Chat moderation action
 */
export interface ChatModerationAction {
  id: string;
  roomId: string;
  action: 'delete' | 'edit' | 'pin' | 'unpin' | 'lock' | 'unlock';
  targetMessageId?: string;
  targetUserId?: string;
  moderatorId: string;
  reason?: string;
  timestamp: string;
}

/**
 * Chat export format
 */
export interface ChatExport {
  roomId: string;
  roomName?: string;
  exportDate: string;
  startDate: string;
  endDate: string;
  messages: ExtendedChatMessage[];
  participants: Array<{
    userId: string;
    userName: string;
    messageCount: number;
  }>;
  format: 'json' | 'csv' | 'txt' | 'html';
}

/**
 * Chat context state
 */
export interface ChatContextState {
  activeRoom: ChatRoom | null;
  rooms: ChatRoom[];
  messages: Record<string, ExtendedChatMessage[]>; // roomId -> messages
  threads: Record<string, ChatThread>; // threadId -> thread
  typingIndicators: TypingIndicator[];
  unreadCounts: Record<string, number>; // roomId -> count
  isLoading: boolean;
  isSending: boolean;
  error: Error | null;
}

/**
 * Chat context methods
 */
export interface ChatContextMethods {
  // Room management
  createRoom: (request: CreateChatRoomRequest) => Promise<ChatRoom>;
  joinRoom: (roomId: string) => Promise<void>;
  leaveRoom: (roomId: string) => Promise<void>;
  getRoom: (roomId: string) => Promise<ChatRoom>;
  getRooms: () => Promise<ChatRoom[]>;
  updateRoomSettings: (roomId: string, settings: Partial<ChatRoomSettings>) => Promise<void>;
  archiveRoom: (roomId: string) => Promise<void>;
  deleteRoom: (roomId: string) => Promise<void>;

  // Message management
  sendMessage: (request: SendMessageRequest) => Promise<ExtendedChatMessage>;
  editMessage: (messageId: string, request: UpdateMessageRequest) => Promise<ExtendedChatMessage>;
  deleteMessage: (messageId: string) => Promise<void>;
  loadMessages: (roomId: string, limit?: number, before?: string) => Promise<ExtendedChatMessage[]>;
  searchMessages: (request: SearchMessagesRequest) => Promise<ExtendedChatMessage[]>;
  markAsRead: (roomId: string, messageIds: string[]) => Promise<void>;

  // Reactions
  addReaction: (messageId: string, emoji: string) => Promise<void>;
  removeReaction: (messageId: string, emoji: string) => Promise<void>;

  // Threading
  startThread: (messageId: string) => Promise<ChatThread>;
  replyToThread: (threadId: string, text: string) => Promise<ExtendedChatMessage>;
  loadThread: (threadId: string) => Promise<ChatThread>;

  // Typing indicators
  startTyping: (roomId: string) => void;
  stopTyping: (roomId: string) => void;

  // Notifications
  updateNotificationPreferences: (preferences: Partial<ChatNotificationPreferences>) => Promise<void>;
  muteRoom: (roomId: string, duration?: number) => Promise<void>;
  unmuteRoom: (roomId: string) => Promise<void>;

  // Utilities
  getChatStatistics: (roomId?: string, period?: 'day' | 'week' | 'month') => Promise<ChatStatistics>;
  exportChat: (roomId: string, startDate: string, endDate: string, format: 'json' | 'csv' | 'txt' | 'html') => Promise<ChatExport>;
  clearError: () => void;
}

/**
 * Complete Chat context type
 */
export interface ChatContextType extends ChatContextState, ChatContextMethods {}

/**
 * Chat utility functions
 */
export interface ChatUtils {
  formatMessage: (message: ExtendedChatMessage) => string;
  parseMentions: (text: string) => MessageMention[];
  extractEmojis: (text: string) => string[];
  sanitizeMessage: (text: string) => string;
  generateMessageId: () => string;
  calculateUnreadCount: (messages: ExtendedChatMessage[], lastReadTimestamp: string) => number;
  groupMessagesByDate: (messages: ExtendedChatMessage[]) => Record<string, ExtendedChatMessage[]>;
  isMessageEditable: (message: ExtendedChatMessage, userId: string, maxEditTime?: number) => boolean;
}