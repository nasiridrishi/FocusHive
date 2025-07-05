import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useSocket } from '../contexts/SocketContext';
import { roomService } from '../services/roomService';
import { ParticipantList } from '../components/ParticipantList';
import { StatusSelector } from '../components/StatusSelector';
import { PresenceIndicator } from '../components/PresenceIndicator';
import { Timer } from '../components/Timer';
import ChatPanel from '../components/ChatPanel';
import Navigation from '../components/Navigation';
import type { Room as RoomType, TimerState } from '@focushive/shared';

export const Room: React.FC = () => {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const { connected, currentRoom, participants, joinRoom, leaveRoom, socket } = useSocket();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [roomDetails, setRoomDetails] = useState<RoomType | null>(null);
  const [timerState, setTimerState] = useState<TimerState | null>(null);
  const [chatEnabled, setChatEnabled] = useState(false);

  // Listen for timer state changes
  useEffect(() => {
    if (!socket) return;

    const handleTimerUpdate = (state: TimerState) => {
      setTimerState(state);
      // Chat is enabled during breaks
      const enabled = state.phase === 'shortBreak' || state.phase === 'longBreak';
      setChatEnabled(enabled);
    };

    const handleChatStateChange = ({ enabled, phase }: { enabled: boolean; phase: string }) => {
      setChatEnabled(enabled);
    };

    socket.on('timer:state-update', handleTimerUpdate);
    socket.on('chat:state-changed', handleChatStateChange);

    return () => {
      socket.off('timer:state-update', handleTimerUpdate);
      socket.off('chat:state-changed', handleChatStateChange);
    };
  }, [socket]);

  // Load room details
  useEffect(() => {
    if (!roomId) {
      navigate('/dashboard');
      return;
    }

    const loadRoomDetails = async () => {
      try {
        const room = await roomService.getRoom(roomId);
        setRoomDetails(room);
      } catch (error: any) {
        console.error('Failed to load room details:', error);
        setError(error.message || 'Failed to load room details');
        setLoading(false);
      }
    };

    loadRoomDetails();
  }, [roomId, navigate]);

  // Join room via socket when connected
  useEffect(() => {
    if (!roomId || !connected || !socket || !roomDetails) return;
    
    // Don't rejoin if already in this room
    if (currentRoom?.id === roomId) {
      setLoading(false);
      return;
    }

    const socketJoinRoom = async () => {
      try {
        setError('');
        await joinRoom(roomId);
        setLoading(false);
      } catch (error: any) {
        console.error('Failed to join room:', error);
        setError(error.message || 'Failed to join room');
        setLoading(false);
      }
    };

    socketJoinRoom();
  }, [roomId, connected, socket, roomDetails, currentRoom, joinRoom]);

  // Leave room on unmount
  useEffect(() => {
    return () => {
      if (currentRoom?.id === roomId) {
        leaveRoom().catch(console.error);
      }
    };
  }, [currentRoom, roomId, leaveRoom]);

  const handleLeaveRoom = async () => {
    try {
      await leaveRoom();
      navigate('/dashboard');
    } catch (error) {
      console.error('Failed to leave room:', error);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 dark:border-indigo-400 mx-auto"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">Joining room...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center">
          <div className="bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300 p-4 rounded-lg max-w-md">
            <p className="font-semibold">Failed to join room</p>
            <p className="mt-2">{error}</p>
            <button
              onClick={() => navigate('/dashboard')}
              className="mt-4 px-4 py-2 bg-red-600 dark:bg-red-700 text-white rounded-md hover:bg-red-700 dark:hover:bg-red-600"
            >
              Back to Dashboard
            </button>
          </div>
        </div>
      </div>
    );
  }

  const room = currentRoom || roomDetails;

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Navigation />
      
      {/* Room Header */}
      <header className="bg-white dark:bg-gray-800 shadow dark:shadow-gray-900/50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div>
              <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                {room?.name || 'Focus Room'}
              </h1>
              {room?.description && (
                <p className="text-gray-600 dark:text-gray-400 mt-1">{room.description}</p>
              )}
            </div>
            
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                <PresenceIndicator status="focusing" size="sm" />
                <span>{participants.filter(p => p.status === 'focusing').length} focusing</span>
              </div>
              
              <button
                onClick={handleLeaveRoom}
                className="px-4 py-2 bg-red-600 dark:bg-red-700 text-white rounded-md hover:bg-red-700 dark:hover:bg-red-600 transition-colors"
              >
                Leave Room
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Column - Main Area */}
          <div className="lg:col-span-2 space-y-6">
            {/* Timer */}
            <Timer roomId={roomId!} />

            {/* Status Selector */}
            <StatusSelector />
          </div>

          {/* Right Column - Participants & Chat */}
          <div className="space-y-6">
            <ParticipantList participants={participants} />
            
            {/* Chat Panel */}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-900/50" style={{ height: '400px' }}>
              <ChatPanel 
                roomId={roomId!} 
                enabled={chatEnabled}
                phase={timerState?.phase}
              />
            </div>
            
            {/* Room Info */}
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-900/50 p-4">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-3">Room Info</h3>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-600 dark:text-gray-400">Type:</span>
                  <span className="font-medium text-gray-900 dark:text-white">{room?.type === 'private' ? '🔒 Private' : '🌐 Public'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600 dark:text-gray-400">Focus Type:</span>
                  <span className="font-medium capitalize text-gray-900 dark:text-white">{room?.focusType || 'General'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600 dark:text-gray-400">Capacity:</span>
                  <span className="font-medium text-gray-900 dark:text-white">{participants.length}/{room?.maxParticipants || 10}</span>
                </div>
              </div>
              
              {room?.tags && room.tags.length > 0 && (
                <div className="mt-3 pt-3 border-t border-gray-200 dark:border-gray-700">
                  <p className="text-sm text-gray-600 dark:text-gray-400 mb-2">Tags:</p>
                  <div className="flex flex-wrap gap-1">
                    {room.tags.map((tag, index) => (
                      <span key={index} className="bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 px-2 py-1 rounded-full text-xs">
                        {tag}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};