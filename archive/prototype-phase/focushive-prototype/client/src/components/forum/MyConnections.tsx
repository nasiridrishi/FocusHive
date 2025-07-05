import React, { useState, useEffect } from 'react';
import type { BuddyConnection, ForumPost } from '@focushive/shared';
import { useAuth } from '../../contexts/AuthContext';
import { useSocket } from '../../contexts/SocketContext';

interface MyConnectionsProps {
  connections: BuddyConnection[];
  onViewPost: (postId: string) => void;
  onStartSession: (connectionId: string) => void;
}

export const MyConnections: React.FC<MyConnectionsProps> = ({ 
  connections, 
  onViewPost,
  onStartSession 
}) => {
  const { user } = useAuth();
  const { socket } = useSocket();
  const [activeTab, setActiveTab] = useState<'active' | 'pending'>('active');

  const activeConnections = connections.filter(c => c.status === 'accepted');
  const pendingConnections = connections.filter(c => 
    c.status === 'pending' && c.requestedUserId === user?.id
  );
  const sentRequests = connections.filter(c => 
    c.status === 'pending' && c.requesterId === user?.id
  );

  const formatNextSession = (schedule: BuddyConnection['schedule']) => {
    const now = new Date();
    const today = now.toLocaleDateString('en-US', { weekday: 'long' }).toLowerCase();
    const currentTime = now.toTimeString().slice(0, 5);

    // Find next scheduled day
    const todayIndex = schedule.days.indexOf(today);
    if (todayIndex !== -1) {
      const slot = schedule.timeSlots[0];
      if (currentTime < slot.start) {
        return `Today at ${slot.start}`;
      }
    }

    // Find next day
    const days = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday'];
    const currentDayIndex = days.indexOf(today);
    
    for (let i = 1; i <= 7; i++) {
      const nextDayIndex = (currentDayIndex + i) % 7;
      const nextDay = days[nextDayIndex];
      
      if (schedule.days.includes(nextDay)) {
        const slot = schedule.timeSlots[0];
        return `${nextDay.charAt(0).toUpperCase() + nextDay.slice(1)} at ${slot.start}`;
      }
    }

    return 'No upcoming session';
  };

  const handleAcceptConnection = (connectionId: string) => {
    if (!socket) return;
    socket.emit('forum:accept-connection', { connectionId });
  };

  const handleDeclineConnection = (connectionId: string) => {
    if (!socket) return;
    socket.emit('forum:decline-connection', { connectionId });
  };

  const ConnectionCard: React.FC<{ connection: BuddyConnection }> = ({ connection }) => {
    const isRequester = connection.requesterId === user?.id;
    const partnerName = isRequester ? connection.requestedUser?.username : connection.requesterUser?.username;
    const partnerId = isRequester ? connection.requestedUserId : connection.requesterId;

    return (
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center space-x-3">
            <img
              src={`https://ui-avatars.com/api/?name=${partnerName || 'User'}`}
              alt={partnerName}
              className="w-12 h-12 rounded-full"
            />
            <div>
              <h4 className="font-medium text-gray-900 dark:text-white">{partnerName || 'Unknown User'}</h4>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Compatibility: {connection.compatibilityScore}%
              </p>
            </div>
          </div>
          {connection.status === 'accepted' && (
            <span className="px-2 py-1 bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200 rounded-full text-xs font-medium">
              Active
            </span>
          )}
        </div>

        {/* Shared Tags */}
        <div className="flex flex-wrap gap-2 mb-4">
          {connection.sharedTags.slice(0, 3).map((tag, index) => (
            <span key={index} className="px-2 py-1 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-md text-xs">
              {tag}
            </span>
          ))}
          {connection.sharedTags.length > 3 && (
            <span className="px-2 py-1 text-gray-500 dark:text-gray-400 text-xs">
              +{connection.sharedTags.length - 3} more
            </span>
          )}
        </div>

        {/* Schedule */}
        <div className="text-sm text-gray-600 dark:text-gray-400 mb-4">
          <div className="flex items-center mb-1">
            <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Next session: {formatNextSession(connection.schedule)}
          </div>
          <div className="text-xs text-gray-500 dark:text-gray-400">
            {connection.schedule.days.map(d => d.slice(0, 3).toUpperCase()).join(', ')} | 
            {connection.schedule.timeSlots[0].start}-{connection.schedule.timeSlots[0].end} {connection.schedule.timezone}
          </div>
        </div>

        {/* Stats */}
        {connection.status === 'accepted' && (
          <div className="grid grid-cols-3 gap-2 mb-4 text-center">
            <div className="bg-gray-50 dark:bg-gray-700 rounded p-2">
              <div className="text-lg font-semibold text-gray-900 dark:text-white">
                {connection.stats.sessionsCompleted}
              </div>
              <div className="text-xs text-gray-500 dark:text-gray-400">Sessions</div>
            </div>
            <div className="bg-gray-50 dark:bg-gray-700 rounded p-2">
              <div className="text-lg font-semibold text-gray-900 dark:text-white">
                {Math.floor(connection.stats.totalFocusTime / 60)}h
              </div>
              <div className="text-xs text-gray-500 dark:text-gray-400">Focus Time</div>
            </div>
            <div className="bg-gray-50 dark:bg-gray-700 rounded p-2">
              <div className="text-lg font-semibold text-gray-900 dark:text-white">
                {connection.stats.streak}
              </div>
              <div className="text-xs text-gray-500 dark:text-gray-400">Day Streak</div>
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="flex space-x-2">
          {connection.status === 'accepted' ? (
            <>
              <button
                onClick={() => onStartSession(connection.id)}
                className="flex-1 px-3 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 text-sm"
              >
                Start Session
              </button>
              <button
                onClick={() => onViewPost(connection.postId)}
                className="px-3 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-md hover:bg-gray-50 dark:hover:bg-gray-700 text-sm"
              >
                View Post
              </button>
            </>
          ) : connection.status === 'pending' && !isRequester ? (
            <>
              <button
                onClick={() => handleAcceptConnection(connection.id)}
                className="flex-1 px-3 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 dark:bg-green-500 dark:hover:bg-green-600 text-sm"
              >
                Accept
              </button>
              <button
                onClick={() => handleDeclineConnection(connection.id)}
                className="flex-1 px-3 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 dark:bg-red-500 dark:hover:bg-red-600 text-sm"
              >
                Decline
              </button>
            </>
          ) : (
            <div className="flex-1 text-center text-sm text-gray-500 dark:text-gray-400 py-2">
              Waiting for response...
            </div>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="space-y-6">
      {/* Tabs */}
      <div className="flex space-x-1 bg-gray-100 dark:bg-gray-700 rounded-lg p-1 max-w-sm">
        <button
          onClick={() => setActiveTab('active')}
          className={`flex-1 px-4 py-2 rounded-md font-medium transition-colors ${
            activeTab === 'active'
              ? 'bg-white dark:bg-gray-800 text-indigo-600 dark:text-indigo-400 shadow-sm'
              : 'text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'
          }`}
        >
          Active ({activeConnections.length})
        </button>
        <button
          onClick={() => setActiveTab('pending')}
          className={`flex-1 px-4 py-2 rounded-md font-medium transition-colors ${
            activeTab === 'pending'
              ? 'bg-white dark:bg-gray-800 text-indigo-600 dark:text-indigo-400 shadow-sm'
              : 'text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'
          }`}
        >
          Pending ({pendingConnections.length + sentRequests.length})
        </button>
      </div>

      {/* Content */}
      {activeTab === 'active' ? (
        <div>
          {activeConnections.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-gray-500 dark:text-gray-400 mb-4">No active connections yet</p>
              <button
                onClick={() => window.location.href = '/forums'}
                className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600"
              >
                Find a Buddy
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {activeConnections.map((connection) => (
                <ConnectionCard key={connection.id} connection={connection} />
              ))}
            </div>
          )}
        </div>
      ) : (
        <div>
          {pendingConnections.length === 0 && sentRequests.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-gray-500 dark:text-gray-400">No pending connections</p>
            </div>
          ) : (
            <div className="space-y-4">
              {pendingConnections.length > 0 && (
                <div>
                  <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                    Received Requests ({pendingConnections.length})
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {pendingConnections.map((connection) => (
                      <ConnectionCard key={connection.id} connection={connection} />
                    ))}
                  </div>
                </div>
              )}
              
              {sentRequests.length > 0 && (
                <div className="mt-8">
                  <h3 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                    Sent Requests ({sentRequests.length})
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {sentRequests.map((connection) => (
                      <ConnectionCard key={connection.id} connection={connection} />
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};