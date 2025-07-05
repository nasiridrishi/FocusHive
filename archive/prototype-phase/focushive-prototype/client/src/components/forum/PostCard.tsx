import React from 'react';
import type { ForumPost } from '@focushive/shared';

interface PostCardProps {
  post: ForumPost;
  onConnect: () => void;
  onViewDetails: () => void;
}

export const PostCard: React.FC<PostCardProps> = ({ post, onConnect, onViewDetails }) => {
  const formatSchedule = () => {
    const days = post.schedule.days.map(d => d.charAt(0).toUpperCase() + d.slice(1, 3)).join(', ');
    const times = post.schedule.timeSlots.map(slot => `${slot.start}-${slot.end}`).join(', ');
    return `${days} | ${times} ${post.schedule.timezone}`;
  };

  const getTypeColor = () => {
    switch (post.type) {
      case 'study': return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200';
      case 'work': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200';
      case 'accountability': return 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200';
      case 'group': return 'bg-orange-100 text-orange-800 dark:bg-orange-900 dark:text-orange-200';
      default: return 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200';
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

  const formatTimeAgo = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    
    if (diffHours < 1) return 'Just now';
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffHours < 48) return 'Yesterday';
    return `${Math.floor(diffHours / 24)}d ago`;
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6 hover:shadow-md dark:hover:shadow-gray-900/50 transition-shadow cursor-pointer">
      <div onClick={onViewDetails}>
        {/* Header */}
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center space-x-3">
            <img
              src={post.userAvatar || `https://ui-avatars.com/api/?name=${post.username}`}
              alt={post.username}
              className="w-10 h-10 rounded-full"
            />
            <div>
              <h4 className="font-medium text-gray-900 dark:text-white">{post.username}</h4>
              <p className="text-sm text-gray-500 dark:text-gray-400">{formatTimeAgo(post.createdAt)}</p>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            <span className={`px-2 py-1 rounded-full text-xs font-medium ${getTypeColor()}`}>
              {post.type}
            </span>
            {post.status === 'matched' && (
              <span className="px-2 py-1 bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200 rounded-full text-xs font-medium">
                Matched
              </span>
            )}
          </div>
        </div>

        {/* Title */}
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">{post.title}</h3>

        {/* Description */}
        <p className="text-gray-600 dark:text-gray-300 mb-4 line-clamp-2">{post.description}</p>

        {/* Tags */}
        <div className="flex flex-wrap gap-2 mb-4">
          {post.tags.map((tag, index) => (
            <span key={index} className="px-2 py-1 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-md text-xs">
              {tag}
            </span>
          ))}
        </div>

        {/* Schedule */}
        <div className="flex items-center space-x-4 text-sm text-gray-600 dark:text-gray-400 mb-4">
          <div className="flex items-center">
            <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {formatSchedule()}
          </div>
          <div className="flex items-center">
            <span className="mr-1">{getCommitmentIcon()}</span>
            {post.commitmentLevel}
          </div>
        </div>

        {/* Working Style */}
        <div className="flex items-center space-x-3 text-xs text-gray-500 dark:text-gray-400 mb-4">
          <span className="flex items-center">
            {post.workingStyle.videoPreference === 'on' ? '🎥' : post.workingStyle.videoPreference === 'off' ? '🔇' : '🎥?'}
            Video {post.workingStyle.videoPreference}
          </span>
          <span>•</span>
          <span>{post.workingStyle.communicationStyle} communication</span>
          <span>•</span>
          <span>{post.workingStyle.breakPreference} breaks</span>
        </div>
      </div>

      {/* Actions */}
      <div className="flex items-center justify-between pt-4 border-t border-gray-100 dark:border-gray-700">
        <div className="flex items-center text-sm text-gray-500 dark:text-gray-400">
          <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
          {post.responses.length} responses
        </div>
        <button
          onClick={(e) => {
            e.stopPropagation();
            onConnect();
          }}
          disabled={post.status === 'matched'}
          className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 disabled:bg-gray-300 dark:disabled:bg-gray-600 disabled:cursor-not-allowed text-sm font-medium transition-colors"
        >
          {post.status === 'matched' ? 'Matched' : 'Connect'}
        </button>
      </div>
    </div>
  );
};