import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useSocket } from '../contexts/SocketContext';
import { useToast } from '../contexts/ToastContext';
import { GlobalChat } from '../components/forum/GlobalChat';
import { PostCard } from '../components/forum/PostCard';
import { CreatePostModal } from '../components/forum/CreatePostModal';
import { PostDetailModal } from '../components/forum/PostDetailModal';
import { MyConnections } from '../components/forum/MyConnections';
import Navigation from '../components/Navigation';
import type { ForumPost, ForumFilters, CreatePostFormData, BuddyConnection } from '@focushive/shared';

export const Forums: React.FC = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { socket } = useSocket();
  const { showToast } = useToast();
  const [posts, setPosts] = useState<ForumPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [activeTab, setActiveTab] = useState<'find' | 'groups' | 'professional' | 'connections'>('find');
  const [filters, setFilters] = useState<ForumFilters>({
    sortBy: 'recent',
    filterBy: 'both'
  });
  const [stats, setStats] = useState({
    activeSeekers: 0,
    successfulMatches: 0,
    totalFocusHours: 0
  });
  const [selectedPost, setSelectedPost] = useState<ForumPost | null>(null);
  const [connections, setConnections] = useState<BuddyConnection[]>([]);

  useEffect(() => {
    if (!socket) return;

    // Join forum namespace
    socket.emit('forum:join');

    // Get initial data
    loadPosts();
    loadStats();
    loadConnections();

    // Socket event listeners
    const handlePosts = ({ posts }: { posts: ForumPost[] }) => {
      setPosts(posts);
      setLoading(false);
    };

    const handleNewPost = ({ post }: { post: ForumPost }) => {
      setPosts(prev => [post, ...prev]);
    };

    const handlePostUpdate = ({ post }: { post: ForumPost }) => {
      setPosts(prev => prev.map(p => p.id === post.id ? post : p));
    };

    const handleStats = (newStats: typeof stats) => {
      setStats(newStats);
    };

    const handleConnections = ({ connections }: { connections: BuddyConnection[] }) => {
      setConnections(connections);
    };

    const handleConnectionUpdate = ({ connection }: { connection: BuddyConnection }) => {
      setConnections(prev => {
        const index = prev.findIndex(c => c.id === connection.id);
        if (index >= 0) {
          return [...prev.slice(0, index), connection, ...prev.slice(index + 1)];
        }
        return [...prev, connection];
      });
    };

    socket.on('forum:posts', handlePosts);
    socket.on('forum:new-post', handleNewPost);
    socket.on('forum:post-updated', handlePostUpdate);
    socket.on('forum:stats', handleStats);
    socket.on('forum:connections', handleConnections);
    socket.on('forum:connection-update', handleConnectionUpdate);

    return () => {
      socket.emit('forum:leave');
      socket.off('forum:posts', handlePosts);
      socket.off('forum:new-post', handleNewPost);
      socket.off('forum:post-updated', handlePostUpdate);
      socket.off('forum:stats', handleStats);
      socket.off('forum:connections', handleConnections);
      socket.off('forum:connection-update', handleConnectionUpdate);
    };
  }, [socket]);

  const loadPosts = () => {
    if (!socket) return;
    socket.emit('forum:get-posts', filters);
  };

  const loadStats = () => {
    if (!socket) return;
    socket.emit('forum:get-stats');
  };

  const loadConnections = () => {
    if (!socket) return;
    socket.emit('forum:get-connections');
  };

  useEffect(() => {
    loadPosts();
  }, [filters]);

  const handleCreatePost = (data: CreatePostFormData) => {
    if (!socket) return;
    socket.emit('forum:create-post', data);
  };

  const handleConnect = (post: ForumPost) => {
    if (!socket) return;
    
    if (post.userId === user?.id) {
      showToast('warning', "You can't connect to your own post!");
      return;
    }

    socket.emit('forum:respond-to-post', { postId: post.id });
    showToast('success', 'Connection request sent!');
    
    // Reload connections if on connections tab
    if (activeTab === 'connections') {
      loadConnections();
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const getFilteredPosts = () => {
    let filtered = posts;

    if (activeTab === 'groups') {
      filtered = posts.filter(p => p.type === 'group');
    } else if (activeTab === 'professional') {
      filtered = posts.filter(p => p.type === 'work');
    }

    return filtered;
  };

  const handleStartSession = (connectionId: string) => {
    // Navigate to room with buddy
    const connection = connections.find(c => c.id === connectionId);
    if (connection) {
      // Create or join a room with the buddy
      navigate(`/room/buddy-${connectionId}`);
    }
  };

  const handleViewPostFromConnection = (postId: string) => {
    const post = posts.find(p => p.id === postId);
    if (post) {
      setSelectedPost(post);
    } else {
      // Load the specific post
      if (socket) {
        socket.emit('forum:get-post', { postId });
      }
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Navigation />

      {/* Global Chat */}
      <GlobalChat />

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Navigation Tabs */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex space-x-1 bg-gray-100 dark:bg-gray-800 rounded-lg p-1">
            <button
              onClick={() => setActiveTab('find')}
              className={`px-4 py-2 rounded-md font-medium transition-colors ${
                activeTab === 'find'
                  ? 'bg-white dark:bg-gray-700 text-indigo-600 dark:text-indigo-400 shadow-sm'
                  : 'text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'
              }`}
            >
              Find a Buddy
            </button>
            <button
              onClick={() => setActiveTab('groups')}
              className={`px-4 py-2 rounded-md font-medium transition-colors ${
                activeTab === 'groups'
                  ? 'bg-white dark:bg-gray-700 text-indigo-600 dark:text-indigo-400 shadow-sm'
                  : 'text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'
              }`}
            >
              Study Groups
            </button>
            <button
              onClick={() => setActiveTab('professional')}
              className={`px-4 py-2 rounded-md font-medium transition-colors ${
                activeTab === 'professional'
                  ? 'bg-white dark:bg-gray-700 text-indigo-600 dark:text-indigo-400 shadow-sm'
                  : 'text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'
              }`}
            >
              Professional Co-working
            </button>
            <button
              onClick={() => setActiveTab('connections')}
              className={`px-4 py-2 rounded-md font-medium transition-colors ${
                activeTab === 'connections'
                  ? 'bg-white dark:bg-gray-700 text-indigo-600 dark:text-indigo-400 shadow-sm'
                  : 'text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'
              }`}
            >
              My Connections
            </button>
          </div>

          <div className="flex items-center space-x-4">
            <div className="text-sm text-gray-600 dark:text-gray-400">
              <span className="font-medium">{stats.activeSeekers}</span> active seekers today
            </div>
            <button
              onClick={() => setShowCreateModal(true)}
              className="px-4 py-2 bg-indigo-600 dark:bg-indigo-500 text-white rounded-md hover:bg-indigo-700 dark:hover:bg-indigo-600 font-medium transition-colors"
            >
              Looking for a Buddy?
            </button>
          </div>
        </div>

        {/* Filters */}
        <div className="bg-white dark:bg-gray-800 p-4 rounded-lg shadow-sm mb-6">
          <div className="flex items-center space-x-6">
            <div className="flex items-center space-x-2">
              <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Sort by:</label>
              <select
                value={filters.sortBy}
                onChange={(e) => setFilters({ ...filters, sortBy: e.target.value as any })}
                className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded-md text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                <option value="recent">Recent</option>
                <option value="responses">Most Responses</option>
                <option value="starting-soon">Starting Soon</option>
              </select>
            </div>

            {activeTab === 'find' && (
              <div className="flex items-center space-x-2">
                <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Filter by:</label>
                <select
                  value={filters.filterBy}
                  onChange={(e) => setFilters({ ...filters, filterBy: e.target.value as any })}
                  className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded-md text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                >
                  <option value="both">All Types</option>
                  <option value="study">Study Only</option>
                  <option value="work">Work Only</option>
                </select>
              </div>
            )}
          </div>
        </div>

        {/* Posts Grid or Connections */}
        {activeTab === 'connections' ? (
          <MyConnections
            connections={connections}
            onViewPost={handleViewPostFromConnection}
            onStartSession={handleStartSession}
          />
        ) : loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 dark:border-indigo-400"></div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {getFilteredPosts().length === 0 ? (
              <div className="col-span-full text-center py-12">
                <p className="text-gray-500 dark:text-gray-400 mb-4">No posts found. Be the first to create one!</p>
                <button
                  onClick={() => setShowCreateModal(true)}
                  className="px-4 py-2 bg-indigo-600 dark:bg-indigo-500 text-white rounded-md hover:bg-indigo-700 dark:hover:bg-indigo-600 transition-colors"
                >
                  Create Post
                </button>
              </div>
            ) : (
              getFilteredPosts().map((post) => (
                <PostCard
                  key={post.id}
                  post={post}
                  onConnect={() => handleConnect(post)}
                  onViewDetails={() => setSelectedPost(post)}
                />
              ))
            )}
          </div>
        )}

        {/* Stats Footer */}
        <div className="mt-12 bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Community Stats</h3>
          <div className="grid grid-cols-3 gap-6">
            <div className="text-center">
              <div className="text-3xl font-bold text-indigo-600 dark:text-indigo-400">{stats.activeSeekers}</div>
              <div className="text-sm text-gray-600 dark:text-gray-400">Active Seekers</div>
            </div>
            <div className="text-center">
              <div className="text-3xl font-bold text-green-600 dark:text-green-400">{stats.successfulMatches}</div>
              <div className="text-sm text-gray-600 dark:text-gray-400">Successful Matches</div>
            </div>
            <div className="text-center">
              <div className="text-3xl font-bold text-purple-600 dark:text-purple-400">{stats.totalFocusHours.toLocaleString()}</div>
              <div className="text-sm text-gray-600 dark:text-gray-400">Total Focus Hours</div>
            </div>
          </div>
        </div>
      </main>

      {/* Create Post Modal */}
      <CreatePostModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSubmit={handleCreatePost}
      />

      {/* Post Detail Modal */}
      {selectedPost && (
        <PostDetailModal
          post={selectedPost}
          isOpen={!!selectedPost}
          onClose={() => setSelectedPost(null)}
          onConnect={() => handleConnect(selectedPost)}
        />
      )}
    </div>
  );
};