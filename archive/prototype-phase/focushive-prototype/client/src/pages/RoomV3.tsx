import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useSocket } from '../contexts/SocketContext';
import { useAuth } from '../contexts/AuthContext';
import { roomService } from '../services/roomService';
import { IntegratedTimer } from '../components/IntegratedTimer';
import { FloatingChat } from '../components/FloatingChat';
import { CollapsibleSidebar } from '../components/CollapsibleSidebar';
import Navigation from '../components/Navigation';
import type { Room as RoomType, ParticipantStatus } from '@focushive/shared';

// Video-style Participant Card
const ParticipantCard: React.FC<{ 
  participant: ParticipantStatus;
  isCurrentUser?: boolean;
}> = ({ participant, isCurrentUser }) => {
  const statusColors = {
    focusing: 'border-green-500',
    break: 'border-yellow-500',
    away: 'border-orange-500',
    idle: 'border-gray-400'
  };

  const statusDotColors = {
    focusing: 'bg-green-500',
    break: 'bg-yellow-500',
    away: 'bg-orange-500',
    idle: 'bg-gray-400'
  };

  // Generate a consistent color for the user avatar background
  const avatarColors = [
    'from-blue-400 to-blue-600',
    'from-purple-400 to-purple-600',
    'from-pink-400 to-pink-600',
    'from-indigo-400 to-indigo-600',
    'from-teal-400 to-teal-600',
    'from-green-400 to-green-600'
  ];
  const colorIndex = participant.userId.charCodeAt(0) % avatarColors.length;

  return (
    <div className="relative group transition-all duration-300 hover:scale-[1.02]">
      <div className={`relative bg-gray-900 dark:bg-gray-800 rounded-lg overflow-hidden border-2 ${statusColors[participant.status]} shadow-xl hover:shadow-2xl transition-all aspect-[4/3]`}>
        {/* Simulated Screen/Camera View */}
        <div className="absolute inset-0 bg-gradient-to-br from-gray-800 to-gray-900 dark:from-gray-700 dark:to-gray-800">
          {/* Placeholder for screen share */}
          <div className="flex items-center justify-center h-full opacity-20">
            <svg className="w-24 h-24 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
          </div>
        </div>

        {/* Profile Picture - Top Left */}
        <div className="absolute top-3 left-3 flex items-center gap-2">
          <div className={`w-12 h-12 rounded-full bg-gradient-to-br ${avatarColors[colorIndex]} flex items-center justify-center shadow-lg border-2 border-white/20`}>
            <span className="text-white font-bold text-lg">
              {participant.userId.substring(0, 2).toUpperCase()}
            </span>
          </div>
          <div className={`w-3 h-3 rounded-full ${statusDotColors[participant.status]} shadow-lg`} />
        </div>

        {/* Username - Top Right */}
        <div className="absolute top-3 right-3">
          <p className="text-white font-medium text-sm bg-black/40 px-2 py-1 rounded backdrop-blur-sm">
            {isCurrentUser ? 'You' : `User ${participant.userId.slice(-4)}`}
          </p>
        </div>
        
        {/* "You" indicator for current user */}
        {isCurrentUser && (
          <div className="absolute top-3 left-1/2 transform -translate-x-1/2">
            <div className="bg-indigo-600 text-white text-xs px-3 py-1 rounded-full font-medium shadow-lg">
              Your Screen
            </div>
          </div>
        )}

        {/* Current Task - Bottom */}
        {participant.currentTask && (
          <div className="absolute bottom-0 left-0 right-0 p-3 bg-gradient-to-t from-black/80 to-transparent">
            <p className="text-white text-sm font-medium text-center">
              {participant.currentTask}
            </p>
          </div>
        )}

        {/* Mic/Camera Icons - Bottom Right */}
        <div className="absolute bottom-3 right-3 flex gap-2">
          <div className="bg-black/40 p-1.5 rounded backdrop-blur-sm">
            <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
            </svg>
          </div>
          <div className="bg-black/40 p-1.5 rounded backdrop-blur-sm">
            <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
            </svg>
          </div>
        </div>
      </div>
    </div>
  );
};

// Task Management Widget
const TaskWidget: React.FC = () => {
  const [tasks, setTasks] = useState<{ id: string; text: string; completed: boolean }[]>([
    { id: '1', text: 'Complete project report', completed: false },
    { id: '2', text: 'Review code', completed: false },
    { id: '3', text: 'Team meeting prep', completed: false }
  ]);
  const [newTask, setNewTask] = useState('');
  const [isExpanded, setIsExpanded] = useState(false);

  const addTask = (e: React.FormEvent) => {
    e.preventDefault();
    if (newTask.trim()) {
      setTasks([...tasks, { id: Date.now().toString(), text: newTask, completed: false }]);
      setNewTask('');
    }
  };

  const toggleTask = (id: string) => {
    setTasks(tasks.map(task => 
      task.id === id ? { ...task, completed: !task.completed } : task
    ));
  };

  return (
    <div className="fixed top-20 right-4 z-30">
      <div className={`bg-white dark:bg-gray-800 rounded-lg shadow-xl transition-all duration-300 ${
        isExpanded ? 'w-96' : 'w-auto'
      }`}>
        <button
          onClick={() => setIsExpanded(!isExpanded)}
          className="p-4 flex items-center gap-2 text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white transition-colors"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
          </svg>
          <span className="font-medium">Tasks ({tasks.filter(t => !t.completed).length})</span>
          <svg className={`w-4 h-4 transition-transform ${isExpanded ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>

        {isExpanded && (
          <div className="p-4 pt-0 border-t dark:border-gray-700">
            <form onSubmit={addTask} className="mb-3">
              <div className="flex gap-2">
                <input
                  type="text"
                  value={newTask}
                  onChange={(e) => setNewTask(e.target.value)}
                  placeholder="Add a new task..."
                  className="flex-1 px-3 py-2 bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-white rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm"
                />
                <button
                  type="submit"
                  className="px-3 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 transition-colors"
                >
                  Add
                </button>
              </div>
            </form>

            <div className="space-y-2 max-h-64 overflow-y-auto">
              {tasks.map((task) => (
                <div key={task.id} className="flex items-center gap-3 p-2 hover:bg-gray-50 dark:hover:bg-gray-700 rounded transition-colors">
                  <input
                    type="checkbox"
                    checked={task.completed}
                    onChange={() => toggleTask(task.id)}
                    className="w-4 h-4 text-indigo-600 rounded"
                  />
                  <span className={`text-sm text-gray-700 dark:text-gray-300 flex-1 ${
                    task.completed ? 'line-through opacity-50' : ''
                  }`}>
                    {task.text}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

// Main Room Component
export const RoomV3: React.FC = () => {
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
          console.log('[RoomV3] Joining room:', roomId);
          await joinRoom(roomId);
          setHasJoined(true);
          console.log('[RoomV3] Joined room, participants:', participants);
        }
        
        setLoading(false);
      } catch (err) {
        setError('Failed to join room');
        setLoading(false);
      }
    };

    loadAndJoinRoom();
  }, [roomId, connected, hasJoined, navigate, joinRoom]);

  const handleLeaveRoom = useCallback(async () => {
    try {
      await leaveRoom();
      setHasJoined(false);
      navigate('/dashboard');
    } catch (error) {
      console.error('Failed to leave room:', error);
    }
  }, [leaveRoom, navigate]);

  const handleStatusChange = useCallback((status: ParticipantStatus['status']) => {
    console.log('[RoomV3] Updating presence to:', status);
    updatePresence(status, '');
  }, [updatePresence]);

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
  
  // Include all participants (including current user)
  const allParticipants = participants;
  
  console.log('[RoomV3] Total participants:', participants.length);
  console.log('[RoomV3] Participants data:', participants);

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-950 dark:to-gray-900">
      <Navigation />
      
      {/* Collapsible Sidebar */}
      <CollapsibleSidebar
        participants={participants}
        room={room}
        onStatusChange={handleStatusChange}
        onLeaveRoom={handleLeaveRoom}
      />

      {/* Task Widget */}
      <TaskWidget />

      {/* Room Title and Timer - Fixed at top */}
      <div className="fixed top-20 left-0 right-0 z-20 px-8">
        <div className="max-w-4xl mx-auto space-y-4">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white text-center">
            {room?.name || 'Focus Room'}
          </h1>
          <IntegratedTimer roomId={roomId!} variant="horizontal" />
        </div>
      </div>

      {/* Main Content - Clean Participant Grid */}
      <div className="h-[calc(100vh-64px)] flex items-center justify-center p-8 pt-40">
        <div className="max-w-7xl mx-auto w-full">

          {/* Participant Grid - Larger cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 max-w-7xl mx-auto">
            {allParticipants.length === 0 ? (
              <div className="col-span-full text-center py-16">
                <p className="text-gray-500 dark:text-gray-400 text-lg">
                  Joining room...
                </p>
              </div>
            ) : (
              allParticipants.map((participant) => (
                <ParticipantCard
                  key={participant.userId}
                  participant={participant}
                  isCurrentUser={participant.userId === user?.id}
                />
              ))
            )}
          </div>
        </div>
      </div>

      {/* Floating Components */}
      <FloatingChat roomId={roomId!} />
    </div>
  );
};