import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Room } from '@focushive/shared';
import { roomService } from '../services/roomService';
import { useAuth } from '../contexts/AuthContext';

interface RoomCardProps {
  room: Room;
  onJoin?: () => void;
  onUpdate?: () => void;
}

export const RoomCard: React.FC<RoomCardProps> = ({ room, onJoin, onUpdate }) => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [isJoining, setIsJoining] = useState(false);
  const [password, setPassword] = useState('');
  const [showPasswordInput, setShowPasswordInput] = useState(false);
  const [error, setError] = useState('');

  const isOwner = user?.id === room.ownerId;
  const isParticipant = room.participants.includes(user?.id || '');

  const handleJoin = async () => {
    if (room.type === 'private' && !showPasswordInput) {
      setShowPasswordInput(true);
      return;
    }

    setIsJoining(true);
    setError('');

    try {
      await roomService.joinRoom(room.id, room.type === 'private' ? password : undefined);
      onJoin?.();
      setShowPasswordInput(false);
      setPassword('');
      // Navigate to room page
      navigate(`/room/${room.id}`);
    } catch (error: any) {
      setError(error.response?.data?.error || 'Failed to join room');
    } finally {
      setIsJoining(false);
    }
  };

  const handleLeave = async () => {
    try {
      await roomService.leaveRoom(room.id);
      onUpdate?.();
    } catch (error: any) {
      setError(error.response?.data?.error || 'Failed to leave room');
    }
  };

  const focusTypeColors = {
    deepWork: 'bg-purple-100 dark:bg-purple-900/30 text-purple-800 dark:text-purple-300',
    study: 'bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300',
    creative: 'bg-pink-100 dark:bg-pink-900/30 text-pink-800 dark:text-pink-300',
    meeting: 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300',
    other: 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300',
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md dark:shadow-gray-900/50 p-6 hover:shadow-lg dark:hover:shadow-gray-900/70 transition-shadow">
      <div className="flex justify-between items-start mb-4">
        <div>
          <h3 className="text-xl font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            {room.name}
            {room.type === 'private' && (
              <svg className="w-4 h-4 text-gray-500 dark:text-gray-400" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
              </svg>
            )}
          </h3>
          {room.description && (
            <p className="text-gray-600 dark:text-gray-400 mt-1">{room.description}</p>
          )}
        </div>
        <span className={`px-2 py-1 rounded-full text-xs font-medium ${focusTypeColors[room.focusType]}`}>
          {room.focusType}
        </span>
      </div>

      <div className="flex items-center justify-between text-sm text-gray-500 dark:text-gray-400 mb-4">
        <span className="flex items-center gap-1">
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path d="M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z" />
          </svg>
          {room.participants.length}/{room.maxParticipants}
        </span>
        {room.tags.length > 0 && (
          <div className="flex gap-1">
            {room.tags.slice(0, 3).map((tag, index) => (
              <span key={index} className="bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 px-2 py-1 rounded text-xs">
                {tag}
              </span>
            ))}
          </div>
        )}
      </div>

      {error && (
        <div className="mb-4 text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 p-2 rounded">
          {error}
        </div>
      )}

      {showPasswordInput && (
        <div className="mb-4">
          <input
            type="password"
            placeholder="Enter room password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
            onKeyDown={(e) => e.key === 'Enter' && handleJoin()}
          />
        </div>
      )}

      <div className="flex gap-2">
        {!isParticipant ? (
          <button
            onClick={handleJoin}
            disabled={isJoining}
            className="flex-1 bg-indigo-600 dark:bg-indigo-500 text-white px-4 py-2 rounded-md hover:bg-indigo-700 dark:hover:bg-indigo-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isJoining ? 'Joining...' : 'Join Room'}
          </button>
        ) : isOwner ? (
          <button
            onClick={() => navigate(`/room/${room.id}`)}
            className="flex-1 bg-indigo-600 dark:bg-indigo-500 text-white px-4 py-2 rounded-md hover:bg-indigo-700 dark:hover:bg-indigo-600 transition-colors"
          >
            Enter Room
          </button>
        ) : (
          <div className="flex gap-2">
            <button
              onClick={() => navigate(`/room/${room.id}`)}
              className="flex-1 bg-indigo-600 dark:bg-indigo-500 text-white px-4 py-2 rounded-md hover:bg-indigo-700 dark:hover:bg-indigo-600 transition-colors"
            >
              Enter Room
            </button>
            <button
              onClick={handleLeave}
              className="bg-red-600 dark:bg-red-500 text-white px-4 py-2 rounded-md hover:bg-red-700 dark:hover:bg-red-600 transition-colors"
            >
              Leave
            </button>
          </div>
        )}
      </div>
    </div>
  );
};