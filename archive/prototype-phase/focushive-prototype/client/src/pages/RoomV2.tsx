import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useSocket } from '../contexts/SocketContext';
import { useAuth } from '../contexts/AuthContext';
import { roomService } from '../services/roomService';
import { FloatingTimer } from '../components/FloatingTimer';
import Navigation from '../components/Navigation';
import type { Room as RoomType, ParticipantStatus, ChatMessage } from '@focushive/shared';

// Participant Card Component
const ParticipantCard: React.FC<{ 
  participant: ParticipantStatus; 
  isCurrentUser: boolean;
  onStatusChange?: (status: ParticipantStatus['status']) => void;
}> = ({ participant, isCurrentUser, onStatusChange }) => {
  const [showStatusMenu, setShowStatusMenu] = useState(false);

  const statusColors = {
    focusing: 'bg-green-500',
    break: 'bg-yellow-500',
    away: 'bg-orange-500',
    idle: 'bg-gray-400'
  };

  const statusOptions = [
    { value: 'focusing' as const, label: 'Focusing', icon: '🎯' },
    { value: 'break' as const, label: 'On Break', icon: '☕' },
    { value: 'away' as const, label: 'Away', icon: '🚶' },
    { value: 'idle' as const, label: 'Idle', icon: '💤' }
  ];

  return (
    <div className="relative">
      <div 
        className={`bg-white dark:bg-gray-900 rounded-lg p-4 h-full flex flex-col items-center justify-center cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors ${
          isCurrentUser ? 'ring-2 ring-indigo-500' : ''
        }`}
        onClick={() => isCurrentUser && setShowStatusMenu(!showStatusMenu)}
      >
        <div className="relative mb-3">
          <div className="w-16 h-16 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center text-gray-700 dark:text-gray-300 font-semibold text-xl">
            {participant.userId.substring(0, 2).toUpperCase()}
          </div>
          <div className={`absolute bottom-0 right-0 w-4 h-4 rounded-full border-2 border-white dark:border-gray-900 ${statusColors[participant.status]}`} />
        </div>
        <p className="text-gray-900 dark:text-white font-medium text-sm">
          {isCurrentUser ? 'You' : `User ${participant.userId.slice(-4)}`}
        </p>
        <p className="text-gray-600 dark:text-gray-400 text-xs mt-1">
          {participant.status === 'focusing' ? 'Focusing' : 
           participant.status === 'break' ? 'On Break' :
           participant.status === 'away' ? 'Away' : 'Idle'}
        </p>
      </div>

      {/* Status Menu */}
      {isCurrentUser && showStatusMenu && (
        <div className="absolute top-full left-0 mt-2 bg-white dark:bg-gray-800 rounded-lg shadow-lg p-2 z-10 min-w-[150px]">
          {statusOptions.map((option) => (
            <button
              key={option.value}
              onClick={(e) => {
                e.stopPropagation();
                onStatusChange?.(option.value);
                setShowStatusMenu(false);
              }}
              className="w-full flex items-center gap-2 px-3 py-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors"
            >
              <span>{option.icon}</span>
              <span className="text-sm text-gray-700 dark:text-gray-300">{option.label}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
};

// Task Management Component
const TaskManagement: React.FC = () => {
  const [tasks, setTasks] = useState<string[]>(['Complete project report', 'Review code', 'Team meeting prep']);
  const [newTask, setNewTask] = useState('');

  const addTask = (e: React.FormEvent) => {
    e.preventDefault();
    if (newTask.trim()) {
      setTasks([...tasks, newTask]);
      setNewTask('');
    }
  };

  return (
    <div className="bg-white dark:bg-gray-900 rounded-lg p-6 h-full shadow-lg dark:shadow-gray-900/50">
      <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Task Management</h2>
      
      <form onSubmit={addTask} className="mb-4">
        <div className="flex gap-2">
          <input
            type="text"
            value={newTask}
            onChange={(e) => setNewTask(e.target.value)}
            placeholder="Add a new task..."
            className="flex-1 px-3 py-2 bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-white rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 border border-gray-300 dark:border-gray-700"
          />
          <button
            type="submit"
            className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 transition-colors"
          >
            Add
          </button>
        </div>
      </form>

      <div className="space-y-2 overflow-y-auto max-h-[300px]">
        {tasks.map((task, index) => (
          <div key={index} className="flex items-center gap-3 p-3 bg-gray-100 dark:bg-gray-800 rounded-md">
            <input type="checkbox" className="w-4 h-4 text-indigo-600 rounded" />
            <span className="text-gray-700 dark:text-gray-300 flex-1">{task}</span>
          </div>
        ))}
      </div>
    </div>
  );
};

// Chat Component
const ChatPanel: React.FC<{ roomId: string }> = ({ roomId }) => {
  const { socket } = useSocket();
  const { user } = useAuth();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [newMessage, setNewMessage] = useState('');

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
    };

    socket.on('chat:history', handleHistory);
    socket.on('chat:message', handleNewMessage);

    return () => {
      socket.off('chat:history', handleHistory);
      socket.off('chat:message', handleNewMessage);
    };
  }, [socket, roomId]);

  const sendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!socket || !newMessage.trim()) return;
    
    socket.emit('chat:send-message', { roomId, message: newMessage });
    setNewMessage('');
  };

  return (
    <div className="bg-white dark:bg-gray-900 rounded-lg p-6 h-full flex flex-col shadow-lg dark:shadow-gray-900/50">
      <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Chat</h2>
      
      <div className="flex-1 overflow-y-auto mb-4 space-y-2">
          {messages.length === 0 ? (
          <p className="text-gray-500 text-center py-8">No messages yet</p>
        ) : (
          messages.map((msg) => (
            <div key={msg.id} className={`p-3 rounded-md ${
              msg.userId === user?.id ? 'bg-indigo-100 dark:bg-indigo-900/30 ml-4' : 'bg-gray-100 dark:bg-gray-800 mr-4'
            }`}>
              <p className="text-sm font-medium text-gray-700 dark:text-gray-300">{msg.username}</p>
              <p className="text-gray-600 dark:text-gray-400 text-sm">{msg.message}</p>
            </div>
          ))
        )}
      </div>

      <form onSubmit={sendMessage} className="flex gap-2">
        <input
          type="text"
          value={newMessage}
          onChange={(e) => setNewMessage(e.target.value)}
          placeholder="Type a message..."
          className="flex-1 px-3 py-2 bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-white rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 border border-gray-300 dark:border-gray-700"
        />
        <button
          type="submit"
          className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 transition-colors"
        >
          Send
        </button>
      </form>
    </div>
  );
};

// Room Info Component
const RoomInfo: React.FC<{ room: RoomType | null }> = ({ room }) => {
  if (!room) return null;

  return (
    <div className="bg-white dark:bg-gray-900 rounded-lg p-6 h-full shadow-lg dark:shadow-gray-900/50">
      <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Room Info</h2>
      
      <div className="space-y-3 text-sm">
        <div className="flex justify-between">
          <span className="text-gray-600 dark:text-gray-400">Name:</span>
          <span className="text-gray-900 dark:text-white font-medium">{room.name}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-600 dark:text-gray-400">Type:</span>
          <span className="text-gray-900 dark:text-white">{room.type === 'private' ? '🔒 Private' : '🌐 Public'}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-gray-600 dark:text-gray-400">Focus Type:</span>
          <span className="text-gray-900 dark:text-white capitalize">{room.focusType || 'General'}</span>
        </div>
        {room.tags && room.tags.length > 0 && (
          <div>
            <p className="text-gray-600 dark:text-gray-400 mb-2">Tags:</p>
            <div className="flex flex-wrap gap-1">
              {room.tags.map((tag, index) => (
                <span key={index} className="bg-gray-200 dark:bg-gray-800 text-gray-700 dark:text-gray-300 px-2 py-1 rounded-full text-xs">
                  {tag}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

// Main Room Component
export const RoomV2: React.FC = () => {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { connected, currentRoom, participants, joinRoom, leaveRoom, updatePresence } = useSocket();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [roomDetails, setRoomDetails] = useState<RoomType | null>(null);
  const [hasJoined, setHasJoined] = useState(false);

  // Load room and join
  useEffect(() => {
    const loadAndJoinRoom = async () => {
      if (!roomId) {
        navigate('/dashboard');
        return;
      }

      try {
        setLoading(true);
        const room = await roomService.getRoom(roomId);
        setRoomDetails(room);
        
        if (connected && !hasJoined) {
          await joinRoom(roomId);
          setHasJoined(true);
        }
        
        setLoading(false);
      } catch (err) {
        setError('Failed to join room');
        setLoading(false);
      }
    };

    loadAndJoinRoom();
  }, [roomId, connected, hasJoined, navigate, joinRoom]);

  const handleLeaveRoom = async () => {
    try {
      await leaveRoom();
      setHasJoined(false);
      navigate('/dashboard');
    } catch (error) {
      console.error('Failed to leave room:', error);
    }
  };

  const handleStatusChange = (status: ParticipantStatus['status']) => {
    updatePresence(status, '');
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 dark:border-indigo-400"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex items-center justify-center">
        <div className="text-center">
          <p className="text-red-600 dark:text-red-500 mb-4">{error}</p>
          <button
            onClick={() => navigate('/dashboard')}
            className="px-4 py-2 bg-red-600 dark:bg-red-700 text-white rounded-md hover:bg-red-700 dark:hover:bg-red-600"
          >
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  const room = currentRoom || roomDetails;

  // Create placeholder participants for empty slots (excluding current user)
  const maxParticipants = 8;
  const otherParticipants = participants.filter(p => p.userId !== user?.id);
  const participantSlots = [...otherParticipants];
  while (participantSlots.length < maxParticipants) {
    participantSlots.push(null as any);
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <Navigation />
      
      {/* Main Grid Layout */}
      <div className="h-[calc(100vh-64px)] p-4">
        <div className="grid grid-cols-12 gap-4 h-full">
          {/* Top Row */}
          <div className="col-span-3 row-span-1">
            <div className="bg-white dark:bg-gray-900 rounded-lg p-6 h-full shadow-lg dark:shadow-gray-900/50">
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Participants</h2>
              <div className="space-y-2">
                {participants.map((participant) => {
                  const isCurrentUser = participant.userId === user?.id;
                  return (
                    <div
                      key={participant.userId}
                      className={`flex items-center gap-3 p-2 rounded-md transition-colors cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-800 ${
                        isCurrentUser ? 'bg-indigo-100 dark:bg-indigo-900/30' : ''
                      }`}
                      onClick={() => {
                        if (isCurrentUser) {
                          // Toggle through statuses
                          const statuses: ParticipantStatus['status'][] = ['focusing', 'break', 'away', 'idle'];
                          const currentIndex = statuses.indexOf(participant.status);
                          const nextStatus = statuses[(currentIndex + 1) % statuses.length];
                          handleStatusChange(nextStatus);
                        }
                      }}
                    >
                      <div className={`w-2 h-2 rounded-full ${
                        participant.status === 'focusing' ? 'bg-green-500' :
                        participant.status === 'break' ? 'bg-yellow-500' :
                        participant.status === 'away' ? 'bg-orange-500' :
                        'bg-gray-400'
                      }`} />
                      <span className="text-gray-700 dark:text-gray-300 text-sm">
                        {isCurrentUser ? 'You (click to change status)' : `User ${participant.userId.slice(-4)}`}
                      </span>
                    </div>
                  );
                })}
              </div>
              
              <button
                onClick={handleLeaveRoom}
                className="mt-4 w-full px-4 py-2 bg-red-600 dark:bg-red-700 text-white rounded-md hover:bg-red-700 dark:hover:bg-red-600 transition-colors"
              >
                Leave Room
              </button>
            </div>
          </div>

          <div className="col-span-6 row-span-1">
            <TaskManagement />
          </div>

          <div className="col-span-3 row-span-1">
            <ChatPanel roomId={roomId!} />
          </div>

          {/* Bottom Row - Participant Grid */}
          <div className="col-span-10 row-span-1">
            <div className="grid grid-cols-4 gap-3 h-full">
              {participantSlots.slice(0, 8).map((participant, index) => (
                <div key={participant?.userId || `slot-${index}`} className="h-full">
                  {participant && participant.userId !== user?.id ? (
                    <ParticipantCard
                      participant={participant}
                      isCurrentUser={false}
                      onStatusChange={handleStatusChange}
                    />
                  ) : (
                    <div className="bg-gray-100 dark:bg-gray-900 rounded-lg p-4 h-full flex items-center justify-center border-2 border-dashed border-gray-300 dark:border-gray-800">
                      <p className="text-gray-500 dark:text-gray-600 text-sm">Empty Slot</p>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          <div className="col-span-2 row-span-1">
            <RoomInfo room={room} />
          </div>
        </div>
      </div>

      {/* Floating Timer */}
      <FloatingTimer roomId={roomId!} />
    </div>
  );
};