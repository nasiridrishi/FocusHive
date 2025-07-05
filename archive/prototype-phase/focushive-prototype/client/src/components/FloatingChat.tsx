import React, { useState, useEffect } from 'react';
import { useSocket } from '../contexts/SocketContext';
import { useAuth } from '../contexts/AuthContext';
import type { ChatMessage } from '@focushive/shared';

interface FloatingChatProps {
  roomId: string;
}

export const FloatingChat: React.FC<FloatingChatProps> = ({ roomId }) => {
  const { socket } = useSocket();
  const { user } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [newMessage, setNewMessage] = useState('');
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (!socket) return;

    // Get message history
    socket.emit('chat:get-history', { roomId });

    // Listen for messages
    const handleHistory = ({ messages: history }: { messages: ChatMessage[] }) => {
      setMessages(history);
    };

    const handleNewMessage = ({ message }: { message: ChatMessage }) => {
      setMessages(prev => [...prev, message]);
      // Increment unread count if chat is closed
      if (!isOpen && message.userId !== user?.id) {
        setUnreadCount(prev => prev + 1);
      }
    };

    socket.on('chat:history', handleHistory);
    socket.on('chat:message', handleNewMessage);

    return () => {
      socket.off('chat:history', handleHistory);
      socket.off('chat:message', handleNewMessage);
    };
  }, [socket, roomId, isOpen, user?.id]);

  // Reset unread count when opening chat
  useEffect(() => {
    if (isOpen) {
      setUnreadCount(0);
    }
  }, [isOpen]);

  const sendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!socket || !newMessage.trim()) return;
    
    socket.emit('chat:send-message', { roomId, message: newMessage });
    setNewMessage('');
  };

  return (
    <>
      {/* Chat Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="fixed bottom-6 right-6 w-14 h-14 bg-indigo-600 dark:bg-indigo-500 text-white rounded-full shadow-lg hover:bg-indigo-700 dark:hover:bg-indigo-600 transition-all transform hover:scale-105 flex items-center justify-center z-40"
      >
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white text-xs rounded-full flex items-center justify-center">
            {unreadCount}
          </span>
        )}
        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
        </svg>
      </button>

      {/* Chat Window */}
      {isOpen && (
        <div className="fixed bottom-24 right-6 w-96 h-[500px] bg-white dark:bg-gray-800 rounded-lg shadow-2xl flex flex-col z-50 animate-fadeIn">
          {/* Header */}
          <div className="flex items-center justify-between p-4 border-b dark:border-gray-700">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Room Chat</h3>
            <button
              onClick={() => setIsOpen(false)}
              className="p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors"
            >
              <svg className="w-5 h-5 text-gray-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto p-4 space-y-2">
            {messages.length === 0 ? (
              <p className="text-gray-500 text-center py-8">No messages yet. Say hello!</p>
            ) : (
              messages.map((msg) => (
                <div key={msg.id} className={`${
                  msg.userId === user?.id ? 'ml-auto' : 'mr-auto'
                } max-w-[80%]`}>
                  <div className={`p-3 rounded-lg ${
                    msg.userId === user?.id 
                      ? 'bg-indigo-100 dark:bg-indigo-900/30 text-right' 
                      : 'bg-gray-100 dark:bg-gray-700'
                  }`}>
                    <p className="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                      {msg.username}
                    </p>
                    <p className="text-sm text-gray-800 dark:text-gray-200">{msg.message}</p>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Input */}
          <form onSubmit={sendMessage} className="p-4 border-t dark:border-gray-700">
            <div className="flex gap-2">
              <input
                type="text"
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                placeholder="Type a message..."
                className="flex-1 px-3 py-2 bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-white rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm"
              />
              <button
                type="submit"
                className="px-4 py-2 bg-indigo-600 dark:bg-indigo-500 text-white rounded-md hover:bg-indigo-700 dark:hover:bg-indigo-600 transition-colors"
              >
                Send
              </button>
            </div>
          </form>
        </div>
      )}
    </>
  );
};