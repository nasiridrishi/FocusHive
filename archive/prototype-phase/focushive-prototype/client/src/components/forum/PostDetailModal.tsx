import React, { useState } from 'react';
import type { ForumPost, BuddyConnection } from '@focushive/shared';
import { useAuth } from '../../contexts/AuthContext';
import { useSocket } from '../../contexts/SocketContext';

interface PostDetailModalProps {
  post: ForumPost;
  isOpen: boolean;
  onClose: () => void;
  onConnect: () => void;
}

export const PostDetailModal: React.FC<PostDetailModalProps> = ({ 
  post, 
  isOpen, 
  onClose, 
  onConnect 
}) => {
  const { user } = useAuth();
  const { socket } = useSocket();
  const [showConnectConfirm, setShowConnectConfirm] = useState(false);
  const [connectMessage, setConnectMessage] = useState('');

  if (!isOpen) return null;

  const isOwnPost = post.userId === user?.id;
  const hasResponded = post.responses.includes(user?.id || '');

  const formatSchedule = () => {
    const days = post.schedule.days
      .map(d => d.charAt(0).toUpperCase() + d.slice(1))
      .join(', ');
    const times = post.schedule.timeSlots
      .map(slot => `${slot.start}-${slot.end}`)
      .join(', ');
    return `${days} | ${times} ${post.schedule.timezone}`;
  };

  const getTypeIcon = () => {
    switch (post.type) {
      case 'study': return '📚';
      case 'work': return '💻';
      case 'accountability': return '🎯';
      case 'group': return '👥';
      default: return '📌';
    }
  };

  const getCommitmentIcon = () => {
    switch (post.commitmentLevel) {
      case 'daily': return '📅';
      case 'weekly': return '📆';
      case 'one-time': return '⏰';
      case 'flexible': return '🔄';
      default: return '📌';
    }
  };

  const handleConnect = () => {
    if (!showConnectConfirm) {
      setShowConnectConfirm(true);
      return;
    }

    // Send connect request with optional message
    if (socket) {
      socket.emit('forum:connect-to-post', {
        postId: post.id,
        message: connectMessage
      });
    }
    
    onConnect();
    setShowConnectConfirm(false);
    setConnectMessage('');
  };

  return (
    <div className="fixed inset-0 bg-gray-500 dark:bg-gray-900 bg-opacity-75 dark:bg-opacity-75 flex items-center justify-center p-4 z-50">
      <div className="bg-white dark:bg-gray-800 rounded-lg max-w-3xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="sticky top-0 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 p-6">
          <div className="flex justify-between items-start">
            <div className="flex items-center space-x-3">
              <span className="text-2xl">{getTypeIcon()}</span>
              <div>
                <h2 className="text-xl font-semibold text-gray-900 dark:text-white">{post.title}</h2>
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  Posted by {post.username} • {new Date(post.createdAt).toLocaleDateString()}
                </p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-500 dark:text-gray-500 dark:hover:text-gray-300"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="p-6 space-y-6">
          {/* Status badges */}
          <div className="flex items-center space-x-3">
            <span className={`px-3 py-1 rounded-full text-sm font-medium ${
              post.status === 'active' 
                ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200' 
                : post.status === 'matched'
                ? 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200'
                : 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200'
            }`}>
              {post.status}
            </span>
            <span className="text-sm text-gray-500 dark:text-gray-400">
              {post.responses.length} response{post.responses.length !== 1 ? 's' : ''}
            </span>
          </div>

          {/* Description */}
          <div>
            <h3 className="font-medium text-gray-900 dark:text-white mb-2">About this opportunity</h3>
            <p className="text-gray-600 dark:text-gray-300 whitespace-pre-wrap">{post.description}</p>
          </div>

          {/* Tags */}
          <div>
            <h3 className="font-medium text-gray-900 dark:text-white mb-2">Topics</h3>
            <div className="flex flex-wrap gap-2">
              {post.tags.map((tag, index) => (
                <span
                  key={index}
                  className="px-3 py-1 bg-indigo-100 dark:bg-indigo-900 text-indigo-700 dark:text-indigo-300 rounded-full text-sm"
                >
                  {tag}
                </span>
              ))}
            </div>
          </div>

          {/* Schedule */}
          <div className="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
            <h3 className="font-medium text-gray-900 dark:text-white mb-3">Schedule & Commitment</h3>
            <div className="space-y-2">
              <div className="flex items-center text-sm">
                <svg className="w-5 h-5 mr-2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span className="text-gray-600 dark:text-gray-300">{formatSchedule()}</span>
              </div>
              <div className="flex items-center text-sm">
                <span className="mr-2">{getCommitmentIcon()}</span>
                <span className="text-gray-600 dark:text-gray-300 capitalize">{post.commitmentLevel} commitment</span>
              </div>
            </div>
          </div>

          {/* Working Style */}
          <div className="bg-blue-50 dark:bg-blue-900/30 rounded-lg p-4">
            <h3 className="font-medium text-gray-900 dark:text-white mb-3">Working Style Preferences</h3>
            <div className="grid grid-cols-3 gap-4">
              <div className="text-center">
                <div className="text-2xl mb-1">
                  {post.workingStyle.videoPreference === 'on' ? '🎥' : 
                   post.workingStyle.videoPreference === 'off' ? '🔇' : '🎥?'}
                </div>
                <div className="text-sm text-gray-600 dark:text-gray-300">
                  Video {post.workingStyle.videoPreference}
                </div>
              </div>
              <div className="text-center">
                <div className="text-2xl mb-1">
                  {post.workingStyle.communicationStyle === 'minimal' ? '🤐' :
                   post.workingStyle.communicationStyle === 'moderate' ? '💬' : '🗣️'}
                </div>
                <div className="text-sm text-gray-600 dark:text-gray-300 capitalize">
                  {post.workingStyle.communicationStyle} chat
                </div>
              </div>
              <div className="text-center">
                <div className="text-2xl mb-1">
                  {post.workingStyle.breakPreference === 'synchronized' ? '🤝' : '🚶'}
                </div>
                <div className="text-sm text-gray-600 dark:text-gray-300 capitalize">
                  {post.workingStyle.breakPreference} breaks
                </div>
              </div>
            </div>
          </div>

          {/* Connect confirmation */}
          {showConnectConfirm && !isOwnPost && !hasResponded && (
            <div className="bg-indigo-50 dark:bg-indigo-900/30 rounded-lg p-4 space-y-3">
              <p className="text-sm text-indigo-800 dark:text-indigo-200">
                Send a connection request to {post.username}?
              </p>
              <textarea
                value={connectMessage}
                onChange={(e) => setConnectMessage(e.target.value)}
                placeholder="Add a message (optional)"
                className="w-full px-3 py-2 border border-indigo-200 dark:border-indigo-700 dark:bg-gray-700 dark:text-white rounded-md text-sm"
                rows={3}
              />
              <div className="flex space-x-2">
                <button
                  onClick={handleConnect}
                  className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 text-sm"
                >
                  Send Request
                </button>
                <button
                  onClick={() => setShowConnectConfirm(false)}
                  className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-md hover:bg-gray-50 dark:hover:bg-gray-700 text-sm"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="sticky bottom-0 bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 p-6">
          <div className="flex justify-between items-center">
            <div className="text-sm text-gray-500 dark:text-gray-400">
              {post.expiresAt && (
                <span>Expires {new Date(post.expiresAt).toLocaleDateString()}</span>
              )}
            </div>
            <div className="flex space-x-3">
              {isOwnPost ? (
                <>
                  <button className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-md hover:bg-gray-50 dark:hover:bg-gray-700">
                    Edit Post
                  </button>
                  <button className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 dark:bg-red-500 dark:hover:bg-red-600">
                    Close Post
                  </button>
                </>
              ) : hasResponded ? (
                <button
                  disabled
                  className="px-6 py-2 bg-gray-300 dark:bg-gray-600 text-gray-500 dark:text-gray-400 rounded-md cursor-not-allowed"
                >
                  Request Sent
                </button>
              ) : post.status === 'matched' ? (
                <button
                  disabled
                  className="px-6 py-2 bg-gray-300 dark:bg-gray-600 text-gray-500 dark:text-gray-400 rounded-md cursor-not-allowed"
                >
                  Already Matched
                </button>
              ) : (
                <button
                  onClick={() => !showConnectConfirm && handleConnect()}
                  className="px-6 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600"
                >
                  Connect
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};