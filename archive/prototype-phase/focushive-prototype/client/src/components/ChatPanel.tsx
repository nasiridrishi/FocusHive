import React, { useState, useEffect, useRef } from 'react';
import type { ChatMessage } from '@focushive/shared';
import { useSocket } from '../contexts/SocketContext';
import { useAuth } from '../contexts/AuthContext';
import MessageList from './MessageList';
import MessageInput from './MessageInput';
// import './ChatPanel.css'; // Using Tailwind classes instead

interface ChatPanelProps {
  roomId: string;
  enabled: boolean;
  phase?: string;
}

const ChatPanel: React.FC<ChatPanelProps> = ({ roomId, enabled, phase }) => {
  const { socket } = useSocket();
  const { user } = useAuth();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Scroll to bottom when new messages arrive
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Fetch message history on mount
  useEffect(() => {
    if (!socket || !roomId) return;

    const fetchHistory = () => {
      if (socket.connected) {
        socket.emit('chat:get-history', { roomId });
      }
    };

    // Fetch immediately if already connected
    fetchHistory();

    // Also fetch when socket connects
    socket.on('connect', fetchHistory);

    // Listen for message history
    const handleHistory = ({ messages: history }: { messages: ChatMessage[] }) => {
      setMessages(history);
      setError(null);
    };

    // Listen for new messages
    const handleMessage = ({ message }: { message: ChatMessage }) => {
      setMessages(prev => [...prev, message]);
      
      // Increment unread count if message is from another user
      if (message.userId !== user?.id && message.type === 'user') {
        setUnreadCount(prev => prev + 1);
      }
    };

    // Listen for chat state changes
    const handleStateChange = ({ enabled: isEnabled, phase: currentPhase }: { enabled: boolean; phase: string }) => {
      if (!isEnabled) {
        setError('Chat is only available during breaks');
      } else {
        setError(null);
      }
    };

    // Listen for errors
    const handleError = ({ message }: { message: string }) => {
      setError(message);
    };

    socket.on('chat:history', handleHistory);
    socket.on('chat:message', handleMessage);
    socket.on('chat:state-changed', handleStateChange);
    socket.on('chat:error', handleError);

    return () => {
      socket.off('connect', fetchHistory);
      socket.off('chat:history', handleHistory);
      socket.off('chat:message', handleMessage);
      socket.off('chat:state-changed', handleStateChange);
      socket.off('chat:error', handleError);
    };
  }, [socket, roomId, user?.id]);

  // Mark messages as read when panel is focused
  useEffect(() => {
    if (!socket || !roomId || unreadCount === 0) return;

    const handleFocus = () => {
      if (socket && socket.connected) {
        socket.emit('chat:mark-read', { roomId });
        setUnreadCount(0);
      }
    };

    window.addEventListener('focus', handleFocus);
    return () => window.removeEventListener('focus', handleFocus);
  }, [socket, roomId, unreadCount]);

  const sendMessage = (message: string) => {
    if (!socket || !socket.connected || !message.trim()) return;

    socket.emit('chat:send-message', { roomId, message });
  };

  return (
    <div className="flex flex-col h-full bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-900/50 overflow-hidden">
      <div className="p-4 bg-gray-50 dark:bg-gray-700 border-b border-gray-200 dark:border-gray-600 flex items-center justify-between">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Chat</h3>
        {unreadCount > 0 && (
          <span className="bg-indigo-600 dark:bg-indigo-500 text-white px-2 py-0.5 rounded-full text-xs font-bold">{unreadCount}</span>
        )}
        {!enabled && (
          <span className="text-sm text-gray-500 dark:text-gray-400 italic">
            {phase === 'work' ? 'Available during breaks' : 'Chat disabled'}
          </span>
        )}
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        <MessageList messages={messages} currentUserId={user?.id || ''} />
        <div ref={messagesEndRef} />
      </div>

      {error && (
        <div className="px-4 py-2 bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 text-sm border-t border-red-200 dark:border-red-800">
          {error}
        </div>
      )}

      <MessageInput 
        onSendMessage={sendMessage} 
        disabled={!enabled}
        placeholder={enabled ? 'Type a message...' : 'Chat available during breaks'}
      />
    </div>
  );
};

export default ChatPanel;