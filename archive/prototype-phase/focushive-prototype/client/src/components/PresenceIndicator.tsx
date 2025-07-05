import React from 'react';
import type { ParticipantStatus } from '@focushive/shared';

interface PresenceIndicatorProps {
  status: ParticipantStatus['status'];
  size?: 'sm' | 'md' | 'lg';
  showText?: boolean;
  pulse?: boolean;
}

export const PresenceIndicator: React.FC<PresenceIndicatorProps> = ({ 
  status, 
  size = 'md', 
  showText = false,
  pulse = true 
}) => {
  const getStatusColor = () => {
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

  const getStatusText = () => {
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

  const getSizeClasses = () => {
    switch (size) {
      case 'sm':
        return 'w-2 h-2';
      case 'lg':
        return 'w-4 h-4';
      case 'md':
      default:
        return 'w-3 h-3';
    }
  };

  return (
    <div className="flex items-center">
      <div className="relative">
        <div className={`${getSizeClasses()} rounded-full ${getStatusColor()}`} />
        {pulse && status === 'focusing' && (
          <div className={`absolute inset-0 ${getSizeClasses()} rounded-full ${getStatusColor()} animate-ping`} />
        )}
      </div>
      {showText && (
        <span className="ml-2 text-sm text-gray-600">{getStatusText()}</span>
      )}
    </div>
  );
};