// Export chat service and hooks
export { chatService } from './services/chatService';
export type { ChatService } from './services/chatService';

// Export hooks
export {
  useHiveChat,
  useChatSearch,
  useMarkAsRead,
  useReadReceipts,
  useOptimisticMessages,
} from './hooks/useChat';

// Re-export types from contracts
export type {
  ChatMessage,
  SendMessageRequest,
  UpdateMessageRequest,
  ChatTypingIndicator,
  ChatHistory,
  ChatSearchParams,
  ChatReadReceipt,
  MessageReaction,
  MessageAttachment,
  MessageType,
  MessageStatus,
  ChatNotification,
  ChatStatistics,
  ChatWebSocketEvent,
} from '@/contracts/chat';