import React, { useState, KeyboardEvent } from 'react';
// import './MessageInput.css'; // Using Tailwind classes instead

interface MessageInputProps {
  onSendMessage: (message: string) => void;
  disabled?: boolean;
  placeholder?: string;
}

const MessageInput: React.FC<MessageInputProps> = ({ 
  onSendMessage, 
  disabled = false,
  placeholder = 'Type a message...'
}) => {
  const [message, setMessage] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (message.trim() && !disabled) {
      onSendMessage(message);
      setMessage('');
    }
  };

  const handleKeyPress = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  return (
    <form className="flex gap-2 p-4 bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700" onSubmit={handleSubmit}>
      <input
        type="text"
        className="flex-1 px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-full text-sm bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-gray-100 placeholder-gray-500 dark:placeholder-gray-400 outline-none transition-all focus:border-indigo-500 dark:focus:border-indigo-400 focus:bg-white dark:focus:bg-gray-800 disabled:opacity-60 disabled:cursor-not-allowed"
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        onKeyPress={handleKeyPress}
        placeholder={placeholder}
        disabled={disabled}
        maxLength={500}
      />
      <button 
        type="submit" 
        className="px-5 py-2.5 bg-indigo-600 dark:bg-indigo-500 text-white rounded-full text-sm font-semibold cursor-pointer transition-all outline-none hover:bg-indigo-700 dark:hover:bg-indigo-600 hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:translate-y-0"
        disabled={disabled || !message.trim()}
      >
        Send
      </button>
    </form>
  );
};

export default MessageInput;