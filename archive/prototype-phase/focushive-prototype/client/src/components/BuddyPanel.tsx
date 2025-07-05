import React, { useState, useEffect } from 'react';
import { useSocket } from '../contexts/SocketContext';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';
import { NotificationModal } from './ui/NotificationModal';
import type { PotentialBuddy, BuddyStatus, BuddyRequestInfo } from '@focushive/shared';

export const BuddyPanel: React.FC = () => {
  const { socket } = useSocket();
  const { user } = useAuth();
  const { showToast } = useToast();
  const [currentBuddy, setCurrentBuddy] = useState<BuddyStatus | null>(null);
  const [potentialBuddies, setPotentialBuddies] = useState<PotentialBuddy[]>([]);
  const [sentRequests, setSentRequests] = useState<BuddyRequestInfo[]>([]);
  const [receivedRequests, setReceivedRequests] = useState<BuddyRequestInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'buddy' | 'find' | 'requests'>('buddy');
  const [showEndBuddyModal, setShowEndBuddyModal] = useState(false);

  useEffect(() => {
    if (!socket) return;

    // Load initial data
    loadBuddyData();

    // Socket event listeners
    socket.on('buddy:current', ({ buddy }) => {
      setCurrentBuddy(buddy);
      if (buddy) {
        setActiveTab('buddy');
      }
    });

    socket.on('buddy:potential-buddies', ({ buddies }) => {
      setPotentialBuddies(buddies);
      setLoading(false);
    });

    socket.on('buddy:requests', ({ sent, received }) => {
      setSentRequests(sent);
      setReceivedRequests(received);
      setLoading(false);
    });

    socket.on('buddy:request-sent', ({ request }) => {
      setError(null);
      loadBuddyRequests();
    });

    socket.on('buddy:request-received', ({ request, from }) => {
      // Add to received requests
      loadBuddyRequests();
    });

    socket.on('buddy:matched', ({ buddy, buddyship }) => {
      setCurrentBuddy({
        buddyId: buddy.userId,
        username: buddy.username,
        avatar: buddy.avatar,
        status: 'active',
        sharedGoals: buddyship.sharedGoals,
        startedAt: buddyship.startedAt
      });
      setActiveTab('buddy');
      loadBuddyRequests();
    });

    socket.on('buddy:ended', ({ success, endedBy }) => {
      if (success) {
        setCurrentBuddy(null);
        setActiveTab('find');
      }
    });

    socket.on('buddy:error', ({ message }) => {
      setError(message);
      setLoading(false);
    });

    return () => {
      socket.off('buddy:current');
      socket.off('buddy:potential-buddies');
      socket.off('buddy:requests');
      socket.off('buddy:request-sent');
      socket.off('buddy:request-received');
      socket.off('buddy:matched');
      socket.off('buddy:ended');
      socket.off('buddy:error');
    };
  }, [socket]);

  const loadBuddyData = () => {
    if (!socket || !socket.connected) return;
    
    socket.emit('buddy:get-current');
    loadBuddyRequests();
  };

  const loadBuddyRequests = () => {
    if (!socket || !socket.connected) return;
    socket.emit('buddy:get-requests');
  };

  const findPotentialBuddies = () => {
    console.log('Finding potential buddies...', { socket: !!socket, connected: socket?.connected });
    
    if (!socket || !socket.connected) {
      setError('Not connected to server. Please refresh the page.');
      return;
    }
    
    setLoading(true);
    setError(null);
    socket.emit('buddy:find-potential');
  };

  const sendBuddyRequest = (toUserId: string, message: string) => {
    if (!socket || !socket.connected) return;
    
    socket.emit('buddy:send-request', { toUserId, message });
  };

  const acceptRequest = (fromUserId: string) => {
    if (!socket || !socket.connected) return;
    
    socket.emit('buddy:accept-request', { fromUserId });
  };

  const declineRequest = (fromUserId: string) => {
    if (!socket || !socket.connected) return;
    
    socket.emit('buddy:decline-request', { fromUserId });
  };

  const endBuddyship = () => {
    if (!socket || !socket.connected) return;
    setShowEndBuddyModal(true);
  };

  const confirmEndBuddyship = () => {
    if (!socket || !socket.connected) return;
    socket.emit('buddy:end-buddyship');
    setShowEndBuddyModal(false);
    showToast('info', 'Buddyship ended');
  };

  // Render current buddy view
  const renderCurrentBuddy = () => {
    if (!currentBuddy) {
      return (
        <div className="text-center py-8">
          <p className="text-gray-500 dark:text-gray-400 mb-4">You don't have a buddy yet.</p>
          <button
            onClick={() => setActiveTab('find')}
            className="px-4 py-2 bg-indigo-600 dark:bg-indigo-500 text-white rounded-md hover:bg-indigo-700 dark:hover:bg-indigo-600 transition-colors"
          >
            Find a Buddy
          </button>
        </div>
      );
    }

    return (
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <img
              src={currentBuddy.avatar || `https://ui-avatars.com/api/?name=${currentBuddy.username}`}
              alt={currentBuddy.username}
              className="w-12 h-12 rounded-full"
            />
            <div>
              <h3 className="font-semibold text-gray-900 dark:text-white">{currentBuddy.username}</h3>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Buddies since {new Date(currentBuddy.startedAt).toLocaleDateString()}
              </p>
            </div>
          </div>
          <button
            onClick={endBuddyship}
            className="text-red-600 dark:text-red-400 hover:text-red-800 dark:hover:text-red-300 text-sm transition-colors"
          >
            End Buddyship
          </button>
        </div>

        {currentBuddy.sharedGoals && currentBuddy.sharedGoals.length > 0 && (
          <div className="mt-4">
            <h4 className="font-medium mb-2 text-gray-900 dark:text-white">Shared Goals</h4>
            <ul className="space-y-1">
              {currentBuddy.sharedGoals.map((goal, index) => (
                <li key={index} className="flex items-start">
                  <span className="text-green-500 mr-2">✓</span>
                  <span className="text-sm text-gray-700 dark:text-gray-300">{goal}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    );
  };

  // Create test users (dev only)
  const createTestUsers = async () => {
    try {
      const response = await fetch('http://localhost:3000/api/test/create-test-users', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        }
      });
      const data = await response.json();
      console.log('Test users created:', data);
      showToast('success', `Created ${data.users?.length || 0} test users. They all have "password123" as password.`);
    } catch (error) {
      console.error('Failed to create test users:', error);
    }
  };

  // Render find buddies view
  const renderFindBuddies = () => (
    <div className="space-y-4">
      <button
        onClick={findPotentialBuddies}
        disabled={loading}
        className="w-full px-4 py-2 bg-indigo-600 dark:bg-indigo-500 text-white rounded-md hover:bg-indigo-700 dark:hover:bg-indigo-600 disabled:opacity-50 transition-colors"
      >
        {loading ? 'Finding...' : 'Find Compatible Buddies'}
      </button>

      {/* Dev only - create test users */}
      {process.env.NODE_ENV !== 'production' && potentialBuddies.length === 0 && !loading && (
        <button
          onClick={createTestUsers}
          className="w-full px-4 py-2 bg-gray-600 dark:bg-gray-500 text-white rounded-md hover:bg-gray-700 dark:hover:bg-gray-600 text-sm transition-colors"
        >
          Create Test Users (Dev Only)
        </button>
      )}

      {error && (
        <div className="bg-red-100 dark:bg-red-900/20 text-red-700 dark:text-red-400 p-3 rounded-md text-sm">
          {error}
        </div>
      )}

      {potentialBuddies.length > 0 && (
        <div className="space-y-3">
          <h3 className="font-medium text-gray-900 dark:text-white">Potential Buddies</h3>
          {potentialBuddies.map((buddy) => (
            <div key={buddy.userId} className="border border-gray-200 dark:border-gray-700 rounded-lg p-3 bg-white dark:bg-gray-800">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center space-x-3">
                  <img
                    src={buddy.avatar || `https://ui-avatars.com/api/?name=${buddy.username}`}
                    alt={buddy.username}
                    className="w-10 h-10 rounded-full"
                  />
                  <div>
                    <h4 className="font-medium text-gray-900 dark:text-white">{buddy.username}</h4>
                    <p className="text-xs text-gray-500 dark:text-gray-400">
                      {buddy.totalFocusTime} min • {buddy.currentStreak} day streak
                    </p>
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-sm font-medium text-green-600 dark:text-green-400">
                    {buddy.compatibilityScore}% match
                  </div>
                </div>
              </div>
              <button
                onClick={() => {
                  sendBuddyRequest(buddy.userId, 'Hi! Want to be focus buddies?');
                  showToast('success', 'Buddy request sent!');
                }}
                className="w-full text-sm px-3 py-1 bg-indigo-600 dark:bg-indigo-500 text-white rounded hover:bg-indigo-700 dark:hover:bg-indigo-600 transition-colors"
              >
                Send Request
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );

  // Render requests view
  const renderRequests = () => (
    <div className="space-y-4">
      {receivedRequests.length > 0 && (
        <div>
          <h3 className="font-medium mb-2 text-gray-900 dark:text-white">Received Requests</h3>
          <div className="space-y-2">
            {receivedRequests.map((request) => (
              <div key={request.id} className="border border-gray-200 dark:border-gray-700 rounded-lg p-3 bg-white dark:bg-gray-800">
                <div className="flex items-center space-x-3 mb-2">
                  <img
                    src={request.avatar || `https://ui-avatars.com/api/?name=${request.username}`}
                    alt={request.username}
                    className="w-10 h-10 rounded-full"
                  />
                  <div className="flex-1">
                    <h4 className="font-medium text-gray-900 dark:text-white">{request.username}</h4>
                    <p className="text-xs text-gray-500 dark:text-gray-400">
                      {request.totalFocusTime} min • {request.currentStreak} day streak
                    </p>
                  </div>
                </div>
                <p className="text-sm text-gray-600 dark:text-gray-400 mb-2">"{request.message}"</p>
                <div className="flex space-x-2">
                  <button
                    onClick={() => acceptRequest(request.fromUserId)}
                    className="flex-1 text-sm px-3 py-1 bg-green-600 dark:bg-green-500 text-white rounded hover:bg-green-700 dark:hover:bg-green-600 transition-colors"
                  >
                    Accept
                  </button>
                  <button
                    onClick={() => declineRequest(request.fromUserId)}
                    className="flex-1 text-sm px-3 py-1 bg-gray-300 dark:bg-gray-600 text-gray-700 dark:text-gray-200 rounded hover:bg-gray-400 dark:hover:bg-gray-500 transition-colors"
                  >
                    Decline
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {sentRequests.length > 0 && (
        <div>
          <h3 className="font-medium mb-2 text-gray-900 dark:text-white">Sent Requests</h3>
          <div className="space-y-2">
            {sentRequests.map((request) => (
              <div key={request.id} className="border border-gray-200 dark:border-gray-700 rounded-lg p-3 bg-white dark:bg-gray-800">
                <div className="flex items-center space-x-3">
                  <img
                    src={request.avatar || `https://ui-avatars.com/api/?name=${request.username}`}
                    alt={request.username}
                    className="w-10 h-10 rounded-full"
                  />
                  <div>
                    <h4 className="font-medium text-gray-900 dark:text-white">{request.username}</h4>
                    <p className="text-sm text-gray-500 dark:text-gray-400">Pending</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {receivedRequests.length === 0 && sentRequests.length === 0 && (
        <p className="text-center text-gray-500 dark:text-gray-400 py-4">No pending requests</p>
      )}
    </div>
  );

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Focus Buddy</h2>
        {(receivedRequests.length > 0 || sentRequests.length > 0) && !currentBuddy && (
          <span className="bg-red-500 text-white text-xs px-2 py-1 rounded-full">
            {receivedRequests.length + sentRequests.length}
          </span>
        )}
      </div>

      {!currentBuddy && (
        <div className="flex space-x-2 mb-4">
          <button
            onClick={() => setActiveTab('find')}
            className={`flex-1 px-3 py-1 text-sm rounded ${
              activeTab === 'find'
                ? 'bg-indigo-100 dark:bg-indigo-900/50 text-indigo-700 dark:text-indigo-300'
                : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'
            }`}
          >
            Find
          </button>
          <button
            onClick={() => setActiveTab('requests')}
            className={`flex-1 px-3 py-1 text-sm rounded ${
              activeTab === 'requests'
                ? 'bg-indigo-100 dark:bg-indigo-900/50 text-indigo-700 dark:text-indigo-300'
                : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'
            }`}
          >
            Requests
            {(receivedRequests.length > 0 || sentRequests.length > 0) && (
              <span className="ml-1 text-xs">({receivedRequests.length + sentRequests.length})</span>
            )}
          </button>
        </div>
      )}

      <div className="min-h-[200px]">
        {currentBuddy || activeTab === 'buddy' ? renderCurrentBuddy() : null}
        {!currentBuddy && activeTab === 'find' ? renderFindBuddies() : null}
        {!currentBuddy && activeTab === 'requests' ? renderRequests() : null}
      </div>

      <NotificationModal
        isOpen={showEndBuddyModal}
        onClose={() => setShowEndBuddyModal(false)}
        type="warning"
        title="End Buddyship"
        message="Are you sure you want to end your buddyship? This action cannot be undone."
        actions={[
          {
            label: 'End Buddyship',
            onClick: confirmEndBuddyship,
            variant: 'primary'
          },
          {
            label: 'Cancel',
            onClick: () => setShowEndBuddyModal(false),
            variant: 'outline'
          }
        ]}
      />
    </div>
  );
};