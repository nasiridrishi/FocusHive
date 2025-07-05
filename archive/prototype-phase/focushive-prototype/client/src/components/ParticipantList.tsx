import React from 'react';
import type { ParticipantStatus } from '@focushive/shared';
import { useAuth } from '../contexts/AuthContext';

interface ParticipantListProps {
  participants: ParticipantStatus[];
  className?: string;
}

export const ParticipantList: React.FC<ParticipantListProps> = ({ participants, className = '' }) => {
  const { user } = useAuth();

  const getStatusColor = (status: ParticipantStatus['status']) => {
    switch (status) {
      case 'focusing':
        return 'bg-green-500';
      case 'break':
        return 'bg-yellow-500';
      case 'away':
        return 'bg-orange-500';
      case 'idle':
      default:
        return 'bg-gray-400';
    }
  };

  const getStatusText = (status: ParticipantStatus['status']) => {
    switch (status) {
      case 'focusing':
        return 'Focusing';
      case 'break':
        return 'On Break';
      case 'away':
        return 'Away';
      case 'idle':
      default:
        return 'Idle';
    }
  };

  const formatTime = (minutes: number): string => {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours > 0) {
      return `${hours}h ${mins}m`;
    }
    return `${mins}m`;
  };

  return (
    <div className={`bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-900/50 p-4 ${className}`}>
      <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
        Participants ({participants.length})
      </h3>
      
      <div className="space-y-3">
        {participants.map((participant) => {
          const isMe = participant.userId === user?.id;
          
          return (
            <div
              key={participant.userId}
              className={`flex items-center p-3 rounded-lg ${
                isMe ? 'bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700' : 'bg-gray-50 dark:bg-gray-700'
              }`}
            >
              <div className="relative">
                <div className="w-12 h-12 rounded-full bg-gray-300 dark:bg-gray-600 flex items-center justify-center text-gray-600 dark:text-gray-300 font-semibold">
                  {participant.userId.substring(0, 2).toUpperCase()}
                </div>
                <div
                  className={`absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-white dark:border-gray-800 ${getStatusColor(
                    participant.status
                  )}`}
                />
              </div>
              
              <div className="ml-3 flex-1">
                <div className="flex items-center">
                  <span className="font-medium text-gray-900 dark:text-gray-100">
                    {isMe ? 'You' : `User ${participant.userId.slice(-4)}`}
                  </span>
                  {participant.isCreator && (
                    <span className="ml-2 px-2 py-1 text-xs bg-purple-100 dark:bg-purple-900/30 text-purple-800 dark:text-purple-300 rounded-full">
                      Owner
                    </span>
                  )}
                </div>
                
                <div className="flex items-center mt-1 text-sm text-gray-600 dark:text-gray-400">
                  <span className={`font-medium ${
                    participant.status === 'focusing' ? 'text-green-600 dark:text-green-400' : ''
                  }`}>
                    {getStatusText(participant.status)}
                  </span>
                  
                  {participant.currentTask && (
                    <>
                      <span className="mx-2">•</span>
                      <span className="italic truncate">{participant.currentTask}</span>
                    </>
                  )}
                  
                  {participant.sessionFocusTime > 0 && (
                    <>
                      <span className="mx-2">•</span>
                      <span>{formatTime(participant.sessionFocusTime)}</span>
                    </>
                  )}
                </div>
              </div>
              
              {participant.isMuted && (
                <div className="ml-2">
                  <svg className="w-5 h-5 text-gray-400 dark:text-gray-500" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M9.383 3.076A1 1 0 0110 4v12a1 1 0 01-1.707.707L4.586 13H2a1 1 0 01-1-1V8a1 1 0 011-1h2.586l3.707-3.707a1 1 0 011.09-.217zM12.293 7.293a1 1 0 011.414 0L15 8.586l1.293-1.293a1 1 0 111.414 1.414L16.414 10l1.293 1.293a1 1 0 01-1.414 1.414L15 11.414l-1.293 1.293a1 1 0 01-1.414-1.414L13.586 10l-1.293-1.293a1 1 0 010-1.414z" clipRule="evenodd" />
                  </svg>
                </div>
              )}
            </div>
          );
        })}
      </div>
      
      {participants.length === 0 && (
        <p className="text-center text-gray-500 dark:text-gray-400 py-8">
          No participants yet
        </p>
      )}
    </div>
  );
};