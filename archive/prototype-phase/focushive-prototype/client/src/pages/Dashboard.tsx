import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { RoomList } from '../components/RoomList';
import { CreateRoomModal } from '../components/CreateRoomModal';
import { Leaderboard } from '../components/Leaderboard';
import { AchievementsGrid } from '../components/AchievementsGrid';
import { BuddyPanel } from '../components/BuddyPanel';
import { UserProfile } from '../components/UserProfile';
import Navigation from '../components/Navigation';
import { NotificationModal } from '../components/ui/NotificationModal';
import { roomService } from '../services/roomService';
import type { Room } from '@focushive/shared';

export const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [userRooms, setUserRooms] = useState<Room[]>([]);
  const [loadingUserRooms, setLoadingUserRooms] = useState(false);
  const [activeTab, setActiveTab] = useState<'overview' | 'leaderboard' | 'achievements' | 'buddy' | 'profile'>('overview');
  const [showBuddyPanel, setShowBuddyPanel] = useState(false);
  const [notification, setNotification] = useState<{
    isOpen: boolean;
    type: 'info' | 'warning' | 'error' | 'success';
    title: string;
    message: string;
    actions?: Array<{ label: string; onClick: () => void; variant?: 'primary' | 'secondary' | 'outline' }>;
  }>({ isOpen: false, type: 'info', title: '', message: '' });

  // Listen for profile tab event from UserDropdown
  useEffect(() => {
    const handleShowProfile = () => {
      setActiveTab('profile');
    };
    window.addEventListener('show-profile-tab', handleShowProfile);
    return () => window.removeEventListener('show-profile-tab', handleShowProfile);
  }, []);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const loadUserRooms = async () => {
    try {
      setLoadingUserRooms(true);
      const rooms = await roomService.getUserRooms();
      setUserRooms(rooms);
    } catch (error) {
      console.error('Failed to load user rooms:', error);
    } finally {
      setLoadingUserRooms(false);
    }
  };

  const handleQuickStart = async () => {
    try {
      // Get all public rooms
      const publicRooms = await roomService.getPublicRooms();
      const availableRooms = publicRooms.filter(room => 
        room.type === 'public' && 
        room.participants.length < room.maxParticipants
      );

      if (availableRooms.length === 0) {
        setNotification({
          isOpen: true,
          type: 'info',
          title: 'No Public Rooms Available',
          message: 'There are currently no public rooms available to join. You can create your own room or wait for others to create public rooms.',
          actions: [
            {
              label: 'Create a Room',
              onClick: () => {
                setShowCreateModal(true);
                setNotification({ ...notification, isOpen: false });
              },
              variant: 'primary'
            },
            {
              label: 'Find a Buddy',
              onClick: () => {
                setActiveTab('buddy');
                setNotification({ ...notification, isOpen: false });
              },
              variant: 'secondary'
            }
          ]
        });
        return;
      }

      // Join a random available room
      const randomRoom = availableRooms[Math.floor(Math.random() * availableRooms.length)];
      navigate(`/room/${randomRoom.id}`);
    } catch (error) {
      console.error('Failed to quick start:', error);
      setNotification({
        isOpen: true,
        type: 'error',
        title: 'Quick Start Failed',
        message: 'Unable to join a room at this time. Please try again later.',
        actions: []
      });
    }
  };

  useEffect(() => {
    loadUserRooms();
  }, []);

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Navigation />

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Navigation Tabs */}
        <div className="flex space-x-1 mb-8 bg-gray-100 dark:bg-gray-700 rounded-lg p-1">
          <button
            onClick={() => setActiveTab('overview')}
            className={`flex-1 py-2 px-4 rounded-md font-medium transition-colors ${
              activeTab === 'overview'
                ? 'bg-white dark:bg-gray-800 text-indigo-600 dark:text-indigo-400 shadow-sm'
                : 'text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'
            }`}
          >
            Overview
          </button>
          <button
            onClick={() => setActiveTab('leaderboard')}
            className={`flex-1 py-2 px-4 rounded-md font-medium transition-colors ${
              activeTab === 'leaderboard'
                ? 'bg-white dark:bg-gray-800 text-indigo-600 dark:text-indigo-400 shadow-sm'
                : 'text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'
            }`}
          >
            Leaderboard
          </button>
          <button
            onClick={() => setActiveTab('achievements')}
            className={`flex-1 py-2 px-4 rounded-md font-medium transition-colors ${
              activeTab === 'achievements'
                ? 'bg-white dark:bg-gray-800 text-indigo-600 dark:text-indigo-400 shadow-sm'
                : 'text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'
            }`}
          >
            Achievements
          </button>
          <button
            onClick={() => setActiveTab('buddy')}
            className={`flex-1 py-2 px-4 rounded-md font-medium transition-colors ${
              activeTab === 'buddy'
                ? 'bg-white dark:bg-gray-800 text-indigo-600 dark:text-indigo-400 shadow-sm'
                : 'text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'
            }`}
          >
            Buddy
          </button>
        </div>

        {/* Tab Content */}
        {activeTab === 'overview' && (
          <>
            {/* Quick Actions */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <button 
            onClick={handleQuickStart}
            className="bg-blue-600 dark:bg-blue-500 hover:bg-blue-700 dark:hover:bg-blue-600 text-white p-6 rounded-lg shadow dark:shadow-gray-900/50 transition-colors">
            <h3 className="text-xl font-bold mb-2">Quick Start</h3>
            <p>Join a random public room</p>
          </button>
          
          <button 
            onClick={() => setShowCreateModal(true)}
            className="bg-green-600 dark:bg-green-500 hover:bg-green-700 dark:hover:bg-green-600 text-white p-6 rounded-lg shadow dark:shadow-gray-900/50 transition-colors"
          >
            <h3 className="text-xl font-bold mb-2">Create Room</h3>
            <p>Start your own focus room</p>
          </button>
          
          <button 
            onClick={() => setActiveTab('buddy')}
            className="bg-purple-600 dark:bg-purple-500 hover:bg-purple-700 dark:hover:bg-purple-600 text-white p-6 rounded-lg shadow dark:shadow-gray-900/50 transition-colors"
          >
            <h3 className="text-xl font-bold mb-2">Find Buddy</h3>
            <p>Get matched with a focus partner</p>
          </button>
        </div>

        {/* My Rooms */}
        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow mb-8">
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-4">
            My Rooms
          </h2>
          {loadingUserRooms ? (
            <div className="flex justify-center items-center h-24">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
            </div>
          ) : userRooms.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {userRooms.map(room => (
                <div key={room.id} className="p-4 border border-gray-200 dark:border-gray-700 rounded-lg">
                  <h3 className="font-semibold text-gray-900 dark:text-white">{room.name}</h3>
                  <p className="text-sm text-gray-600 dark:text-gray-400">
                    {room.participants.length} participants
                  </p>
                  <span className={`inline-block px-2 py-1 rounded-full text-xs font-medium mt-2 ${
                    room.ownerId === user?.id 
                      ? 'bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300' 
                      : 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300'
                  }`}>
                    {room.ownerId === user?.id ? 'Owner' : 'Participant'}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-gray-600 dark:text-gray-400">
              You're not in any rooms yet. Join or create one to get started!
            </p>
          )}
        </div>

        {/* All Public Rooms */}
        <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow">
          <RoomList />
        </div>
          </>
        )}

        {activeTab === 'leaderboard' && <Leaderboard />}
        
        {activeTab === 'achievements' && <AchievementsGrid />}
        
        {activeTab === 'buddy' && (
          <div className="max-w-md mx-auto">
            <BuddyPanel />
          </div>
        )}
        
        {activeTab === 'profile' && (
          <div className="max-w-md mx-auto">
            <UserProfile />
          </div>
        )}
      </main>

      <CreateRoomModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onRoomCreated={() => {
          loadUserRooms();
          window.location.reload(); // Refresh to update room list
        }}
      />

      <NotificationModal
        isOpen={notification.isOpen}
        onClose={() => setNotification({ ...notification, isOpen: false })}
        type={notification.type}
        title={notification.title}
        message={notification.message}
        actions={notification.actions}
      />
    </div>
  );
};