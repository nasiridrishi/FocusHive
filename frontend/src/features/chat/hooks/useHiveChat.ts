import { useState, useEffect, useCallback } from 'react';

interface ChatMessage {
  id: string;
  userId: string;
  username: string;
  content: string;
  timestamp: string;
  edited?: boolean;
  reactions?: Array<{
    emoji: string;
    userIds: string[];
  }>;
}

interface ChatState {
  messages: ChatMessage[];
  typingUsers: string[];
  unreadCount: number;
}

export const useHiveChat = (hiveId: string) => {
  const [state, setState] = useState<ChatState>({
    messages: [],
    typingUsers: [],
    unreadCount: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    // Mock implementation - replace with actual WebSocket/API connection
    const fetchMessages = async () => {
      try {
        setLoading(true);
        // Simulate API delay
        await new Promise(resolve => setTimeout(resolve, 500));

        // Mock data
        setState({
          messages: [
            {
              id: '1',
              userId: '1',
              username: 'Alice',
              content: 'Hey everyone! Ready to focus?',
              timestamp: new Date(Date.now() - 30 * 60000).toISOString(),
            },
            {
              id: '2',
              userId: '2',
              username: 'Bob',
              content: 'Absolutely! Let\'s do this.',
              timestamp: new Date(Date.now() - 25 * 60000).toISOString(),
              reactions: [
                { emoji: '👍', userIds: ['1', '3'] },
                { emoji: '🔥', userIds: ['1'] },
              ],
            },
            {
              id: '3',
              userId: '3',
              username: 'Charlie',
              content: 'Starting my pomodoro now!',
              timestamp: new Date(Date.now() - 20 * 60000).toISOString(),
            },
          ],
          typingUsers: [],
          unreadCount: 0,
        });
      } catch (err) {
        setError(err as Error);
      } finally {
        setLoading(false);
      }
    };

    if (hiveId) {
      fetchMessages();
    }
  }, [hiveId]);

  const sendMessage = useCallback(async (content: string) => {
    const newMessage: ChatMessage = {
      id: String(Date.now()),
      userId: 'current-user',
      username: 'You',
      content,
      timestamp: new Date().toISOString(),
    };

    setState(prev => ({
      ...prev,
      messages: [...prev.messages, newMessage],
    }));

    // In real implementation, send to server via WebSocket
    return newMessage;
  }, []);

  const markAsRead = useCallback(() => {
    setState(prev => ({
      ...prev,
      unreadCount: 0,
    }));
  }, []);

  const setTyping = useCallback((isTyping: boolean) => {
    // In real implementation, send typing status via WebSocket
    console.log('Typing status:', isTyping);
  }, []);

  return {
    messages: state.messages,
    typingUsers: state.typingUsers,
    unreadCount: state.unreadCount,
    loading,
    error,
    sendMessage,
    markAsRead,
    setTyping,
  };
};