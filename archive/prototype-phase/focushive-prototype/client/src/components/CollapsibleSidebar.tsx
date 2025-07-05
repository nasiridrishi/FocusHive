import React, { useState, memo } from 'react';
import type { Room, ParticipantStatus } from '@focushive/shared';
import { useAuth } from '../contexts/AuthContext';

interface CollapsibleSidebarProps {
  participants: ParticipantStatus[];
  room: Room | null;
  onStatusChange: (status: ParticipantStatus['status']) => void;
  onLeaveRoom: () => void;
}

const CollapsibleSidebarComponent: React.FC<CollapsibleSidebarProps> = ({
  participants,
  room,
  onStatusChange,
  onLeaveRoom
}) => {
  const { user } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [showStatusMenu, setShowStatusMenu] = useState(false);

  const statusColors = {
    focusing: 'bg-green-500',
    break: 'bg-yellow-500',
    away: 'bg-orange-500',
    idle: 'bg-gray-400'
  };

  // Get current user's status
  const currentUserStatus = participants.find(p => p.userId === user?.id)?.status;

  const handleStatusSelect = (status: ParticipantStatus['status']) => {
    console.log('[CollapsibleSidebar] Changing status to:', status);
    setShowStatusMenu(false);
    // Small delay to ensure menu closes before status update
    setTimeout(() => {
      onStatusChange(status);
    }, 100);
  };

  return (
    <>
      {/* Toggle Button - Moves with sidebar */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className={`fixed top-20 z-40 p-2 bg-white dark:bg-gray-800 rounded-lg shadow-lg hover:shadow-xl transition-all duration-300 ${
          isOpen ? 'left-[288px]' : 'left-4'
        }`}
        aria-label="Toggle sidebar"
      >
        <svg className="w-5 h-5 text-gray-600 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      </button>

      {/* Sidebar */}
      <div className={`fixed top-0 left-0 h-full w-80 bg-white dark:bg-gray-900 shadow-2xl transform transition-transform duration-300 z-30 ${
        isOpen ? 'translate-x-0' : '-translate-x-full'
      }`}>
        <div className="h-full flex flex-col p-6 pt-20">
          {/* Participants Section */}
          <div className="flex-1 flex flex-col">
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Participants ({participants.length})</h2>
            <div className="space-y-2 flex-1 overflow-y-auto">
              {participants.map((participant) => {
                const isCurrentUser = participant.userId === user?.id;
                return (
                  <div
                    key={participant.userId}
                    className={`flex items-center gap-3 p-3 rounded-lg transition-colors ${
                      isCurrentUser 
                        ? 'bg-indigo-100 dark:bg-indigo-900/30' 
                        : 'bg-gray-50 dark:bg-gray-800'
                    }`}
                  >
                    <div className={`w-3 h-3 rounded-full ${statusColors[participant.status]}`} />
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-900 dark:text-white">
                        {isCurrentUser ? 'You' : `User ${participant.userId.slice(-4)}`}
                      </p>
                      <p className="text-xs text-gray-600 dark:text-gray-400 capitalize">
                        {participant.status}
                        {participant.currentTask && ` • ${participant.currentTask}`}
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Room Info Section - Now at bottom */}
          {room && (
            <div className="mb-4">
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Room Details</h2>
              <div className="space-y-3 bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600 dark:text-gray-400">Name:</span>
                  <span className="text-gray-900 dark:text-white font-medium">{room.name}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600 dark:text-gray-400">Type:</span>
                  <span className="text-gray-900 dark:text-white">{room.type === 'private' ? '🔒 Private' : '🌐 Public'}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600 dark:text-gray-400">Focus:</span>
                  <span className="text-gray-900 dark:text-white capitalize">{room.focusType || 'General'}</span>
                </div>
                {room.tags && room.tags.length > 0 && (
                  <div className="pt-2">
                    <p className="text-sm text-gray-600 dark:text-gray-400 mb-2">Tags:</p>
                    <div className="flex flex-wrap gap-1">
                      {room.tags.map((tag, index) => (
                        <span key={index} className="bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 px-2 py-1 rounded-full text-xs">
                          {tag}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex gap-2">
            {/* Status Change Button - 1/3 width (moved to left) */}
            <div className="flex-[1] relative">
              <button
                onClick={() => setShowStatusMenu(!showStatusMenu)}
                className="w-full h-full px-3 py-3 bg-indigo-600 dark:bg-indigo-700 text-white rounded-lg hover:bg-indigo-700 dark:hover:bg-indigo-600 transition-colors flex items-center justify-center gap-2"
                title="Change Status"
              >
                <div className={`w-3 h-3 rounded-full ${statusColors[currentUserStatus || 'idle']}`} />
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              </button>
              
              {/* Status Dropdown Menu */}
              {showStatusMenu && (
                <div 
                  className="absolute bottom-full left-0 mb-2 bg-white dark:bg-gray-800 rounded-lg shadow-xl border border-gray-200 dark:border-gray-700 overflow-hidden min-w-[150px]"
                  onClick={(e) => e.stopPropagation()}
                >
                  <div className="py-1">
                    <button
                      onClick={() => handleStatusSelect('focusing')}
                      className={`w-full px-4 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-3 transition-colors ${
                        currentUserStatus === 'focusing' ? 'bg-gray-100 dark:bg-gray-700' : ''
                      }`}
                    >
                      <div className="w-3 h-3 rounded-full bg-green-500" />
                      <span className="text-sm font-medium text-gray-900 dark:text-white">Focusing</span>
                      {currentUserStatus === 'focusing' && (
                        <svg className="w-4 h-4 ml-auto text-green-600" fill="currentColor" viewBox="0 0 20 20">
                          <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                        </svg>
                      )}
                    </button>
                    <button
                      onClick={() => handleStatusSelect('break')}
                      className={`w-full px-4 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-3 transition-colors ${
                        currentUserStatus === 'break' ? 'bg-gray-100 dark:bg-gray-700' : ''
                      }`}
                    >
                      <div className="w-3 h-3 rounded-full bg-yellow-500" />
                      <span className="text-sm font-medium text-gray-900 dark:text-white">Break</span>
                      {currentUserStatus === 'break' && (
                        <svg className="w-4 h-4 ml-auto text-yellow-600" fill="currentColor" viewBox="0 0 20 20">
                          <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                        </svg>
                      )}
                    </button>
                    <button
                      onClick={() => handleStatusSelect('away')}
                      className={`w-full px-4 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-3 transition-colors ${
                        currentUserStatus === 'away' ? 'bg-gray-100 dark:bg-gray-700' : ''
                      }`}
                    >
                      <div className="w-3 h-3 rounded-full bg-orange-500" />
                      <span className="text-sm font-medium text-gray-900 dark:text-white">Away</span>
                      {currentUserStatus === 'away' && (
                        <svg className="w-4 h-4 ml-auto text-orange-600" fill="currentColor" viewBox="0 0 20 20">
                          <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                        </svg>
                      )}
                    </button>
                    <button
                      onClick={() => handleStatusSelect('idle')}
                      className={`w-full px-4 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-3 transition-colors ${
                        currentUserStatus === 'idle' ? 'bg-gray-100 dark:bg-gray-700' : ''
                      }`}
                    >
                      <div className="w-3 h-3 rounded-full bg-gray-400" />
                      <span className="text-sm font-medium text-gray-900 dark:text-white">Idle</span>
                      {currentUserStatus === 'idle' && (
                        <svg className="w-4 h-4 ml-auto text-gray-600" fill="currentColor" viewBox="0 0 20 20">
                          <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                        </svg>
                      )}
                    </button>
                  </div>
                </div>
              )}
            </div>
            
            {/* Leave Room Button - 2/3 width (moved to right) */}
            <button
              onClick={onLeaveRoom}
              className="flex-[2] px-4 py-3 bg-red-600 dark:bg-red-700 text-white rounded-lg hover:bg-red-700 dark:hover:bg-red-600 transition-colors font-medium"
            >
              Leave Room
            </button>
          </div>
        </div>
      </div>

      {/* Overlay */}
      {isOpen && (
        <div 
          className="fixed inset-0 bg-black bg-opacity-50 z-20" 
          onClick={() => {
            setIsOpen(false);
            setShowStatusMenu(false);
          }}
        />
      )}
    </>
  );
};

// Export without memo for now - the issue might be elsewhere
export const CollapsibleSidebar = CollapsibleSidebarComponent;