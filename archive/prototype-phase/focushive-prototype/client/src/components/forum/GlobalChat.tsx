import React, { useState, useEffect, useRef } from 'react';
import { useSocket } from '../../contexts/SocketContext';
import { useAuth } from '../../contexts/AuthContext';
import { useToast } from '../../contexts/ToastContext';
import type { GlobalChatMessage } from '@focushive/shared';

export const GlobalChat: React.FC = () => {
  const { socket } = useSocket();
  const { user } = useAuth();
  const { showToast } = useToast();
  const [messages, setMessages] = useState<GlobalChatMessage[]>([]);
  const [onlineCount, setOnlineCount] = useState(0);
  const [newMessage, setNewMessage] = useState('');
  const [isExpanded, setIsExpanded] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isExpanded]);

  useEffect(() => {
    if (!socket) return;

    // Get initial messages
    socket.emit('global-chat:get-messages');

    // Listen for messages and updates
    const handleMessages = (data: { messages: GlobalChatMessage[]; onlineCount: number }) => {
      setMessages(data.messages);
      setOnlineCount(data.onlineCount);
    };

    const handleNewMessage = ({ message }: { message: GlobalChatMessage }) => {
      setMessages(prev => [...prev, message]);
      // Play notification sound if not from current user
      if (message.userId !== user?.id) {
        // Could add notification sound here
      }
    };

    const handleError = ({ message }: { message: string }) => {
      setError(message);
      setTimeout(() => setError(null), 3000);
    };

    socket.on('global-chat:messages', handleMessages);
    socket.on('global-chat:new-message', handleNewMessage);
    socket.on('global-chat:error', handleError);

    return () => {
      socket.off('global-chat:messages', handleMessages);
      socket.off('global-chat:new-message', handleNewMessage);
      socket.off('global-chat:error', handleError);
    };
  }, [socket, user?.id]);

  const sendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!socket || !newMessage.trim()) return;

    socket.emit('global-chat:send-message', { message: newMessage });
    setNewMessage('');
  };

  const reportMessage = (messageId: string) => {
    if (!socket) return;
    socket.emit('global-chat:report-message', { messageId });
    showToast('info', 'Message reported. Thank you for helping keep the community safe.');
  };

  const getRecentMessages = () => {
    return messages.slice(-3);
  };

  const formatTimestamp = (timestamp: string) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 1) return 'just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffMins < 1440) return `${Math.floor(diffMins / 60)}h ago`;
    return date.toLocaleDateString();
  };

  // Collapsed view
  if (!isExpanded) {
    return (
      <div className="bg-gray-50 dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 py-2 px-4 overflow-hidden">
        <div className="max-w-7xl mx-auto flex items-center justify-between gap-2">
          <div className="flex items-center gap-3 min-w-0 flex-1">
            <span className="text-sm text-gray-500 dark:text-gray-400 whitespace-nowrap flex-shrink-0">
              {onlineCount} online
            </span>
            <div className="min-w-0 flex-1 overflow-hidden">
              <div className="flex items-center gap-3 overflow-x-auto scrollbar-hide">
                {getRecentMessages().map((msg) => (
                  <div key={msg.id} className="flex items-center gap-1 text-sm whitespace-nowrap flex-shrink-0">
                    <span className="font-medium text-gray-700 dark:text-gray-300">{msg.username}:</span>
                    <span className="text-gray-600 dark:text-gray-400 max-w-[200px] truncate inline-block">{msg.message}</span>
                    <span className="text-xs text-gray-400 dark:text-gray-500">{formatTimestamp(msg.timestamp)}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
          <button
            onClick={() => setIsExpanded(true)}
            className="flex-shrink-0 px-3 py-1 text-sm bg-indigo-600 text-white rounded-md hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 transition-colors whitespace-nowrap"
          >
            Expand chat
          </button>
        </div>
      </div>
    );
  }

  // Expanded view
  return (
    <div className="bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-700 shadow-lg transition-all duration-300">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-2 border-b border-gray-200 dark:border-gray-700">
          <h3 className="font-semibold text-gray-900 dark:text-white">Global Chat</h3>
          <div className="flex items-center space-x-4">
            <span className="text-sm text-gray-500 dark:text-gray-400">{onlineCount} users online</span>
            <button
              onClick={() => setIsExpanded(false)}
              className="text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </button>
          </div>
        </div>

        {/* Messages */}
        <div className="h-80 overflow-y-auto p-4 space-y-3 dark:bg-gray-800">
          {messages.length === 0 ? (
            <p className="text-center text-gray-500 dark:text-gray-400 py-8">No messages yet. Say hello!</p>
          ) : (
            messages.map((msg) => (
              <div
                key={msg.id}
                className={`flex items-start space-x-3 ${
                  msg.userId === user?.id ? 'flex-row-reverse space-x-reverse' : ''
                }`}
              >
                <img
                  src={msg.userAvatar || `https://ui-avatars.com/api/?name=${msg.username}`}
                  alt={msg.username}
                  className="w-8 h-8 rounded-full"
                />
                <div className={`flex-1 ${msg.userId === user?.id ? 'text-right' : ''}`}>
                  <div className="flex items-center space-x-2">
                    <span className="font-medium text-sm text-gray-900 dark:text-gray-100">{msg.username}</span>
                    <span className="text-xs text-gray-400 dark:text-gray-500">{formatTimestamp(msg.timestamp)}</span>
                    {msg.userId !== user?.id && (
                      <button
                        onClick={() => reportMessage(msg.id)}
                        className="text-xs text-gray-400 dark:text-gray-500 hover:text-red-600 dark:hover:text-red-400 opacity-0 hover:opacity-100 transition-opacity"
                      >
                        Report
                      </button>
                    )}
                  </div>
                  <p className={`mt-1 text-sm ${
                    msg.userId === user?.id
                      ? 'bg-indigo-100 dark:bg-indigo-900 text-indigo-900 dark:text-indigo-100 inline-block rounded-lg px-3 py-1'
                      : 'text-gray-700 dark:text-gray-300'
                  }`}>
                    {msg.message}
                  </p>
                </div>
              </div>
            ))
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Input */}
        <form onSubmit={sendMessage} className="p-4 border-t border-gray-200 dark:border-gray-700">
          {error && (
            <div className="mb-2 text-sm text-red-600 dark:text-red-400">{error}</div>
          )}
          <div className="flex space-x-2">
            <input
              ref={inputRef}
              type="text"
              value={newMessage}
              onChange={(e) => setNewMessage(e.target.value)}
              placeholder="Say hello to the community..."
              maxLength={200}
              className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-800 dark:text-white rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400"
            />
            <button
              type="submit"
              disabled={!newMessage.trim()}
              className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Send
            </button>
          </div>
          <div className="mt-1 text-xs text-gray-400 dark:text-gray-500 text-right">
            {newMessage.length}/200
          </div>
        </form>
      </div>
    </div>
  );
};