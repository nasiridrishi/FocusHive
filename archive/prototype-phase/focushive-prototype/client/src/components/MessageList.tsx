import React from 'react';
import type { ChatMessage } from '@focushive/shared';
// import './MessageList.css'; // Using Tailwind classes instead

interface MessageListProps {
  messages: ChatMessage[];
  currentUserId: string;
}

const MessageList: React.FC<MessageListProps> = ({ messages, currentUserId }) => {
  const formatTime = (timestamp: string) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', { 
      hour: 'numeric', 
      minute: '2-digit' 
    });
  };

  return (
    <div className="flex flex-col gap-3">
      {messages.map((message) => (
        <div
          key={message.id}
          className={`animate-fadeIn ${
            message.type === 'user' ? 'max-w-[70%]' : 'w-full'
          } ${
            message.userId === currentUserId ? 'self-end' : 'self-start'
          }`}
        >
          {message.type === 'system' ? (
            <div className="w-full text-center py-2 px-3 text-sm text-gray-500 dark:text-gray-400 italic flex items-center justify-center gap-2">
              <div className="flex-1 h-px bg-gray-200 dark:bg-gray-700"></div>
              <span className="px-3">{message.message}</span>
              <span className="text-xs">{formatTime(message.timestamp)}</span>
              <div className="flex-1 h-px bg-gray-200 dark:bg-gray-700"></div>
            </div>
          ) : (
            <>
              <div className="flex items-baseline gap-2 mb-1">
                <span className={`font-semibold text-sm ${
                  message.userId === currentUserId 
                    ? 'text-indigo-600 dark:text-indigo-400' 
                    : 'text-gray-900 dark:text-gray-100'
                }`}>{message.username}</span>
                <span className="text-xs text-gray-500 dark:text-gray-400">{formatTime(message.timestamp)}</span>
              </div>
              <div className={`px-3 py-2 rounded-2xl text-sm leading-relaxed break-words ${
                message.userId === currentUserId
                  ? 'bg-indigo-600 dark:bg-indigo-500 text-white'
                  : 'bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-gray-100'
              }`}>{message.message}</div>
            </>
          )}
        </div>
      ))}
    </div>
  );
};

export default MessageList;