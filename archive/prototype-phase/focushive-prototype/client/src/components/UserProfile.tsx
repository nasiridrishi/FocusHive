import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { authService } from '../services/authService';

export const UserProfile: React.FC = () => {
  const { user, setUser } = useAuth();
  const [editing, setEditing] = useState(false);
  const [lookingForBuddy, setLookingForBuddy] = useState(user?.lookingForBuddy || false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const handleSave = async () => {
    if (!user) return;
    
    setSaving(true);
    setError(null);
    setSuccess(false);
    
    try {
      const updatedUser = await authService.updateProfile({
        lookingForBuddy
      });
      
      setUser(updatedUser);
      setSuccess(true);
      setEditing(false);
      
      // Auto-hide success message
      setTimeout(() => setSuccess(false), 3000);
    } catch (err: any) {
      setError(err.message || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  if (!user) return null;

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Profile Settings</h2>
        {!editing && (
          <button
            onClick={() => setEditing(true)}
            className="text-indigo-600 dark:text-indigo-400 hover:text-indigo-800 dark:hover:text-indigo-300 transition-colors"
          >
            Edit
          </button>
        )}
      </div>

      {error && (
        <div className="mb-4 bg-red-100 dark:bg-red-900/20 text-red-700 dark:text-red-400 p-3 rounded-md text-sm">
          {error}
        </div>
      )}

      {success && (
        <div className="mb-4 bg-green-100 dark:bg-green-900/20 text-green-700 dark:text-green-400 p-3 rounded-md text-sm">
          Profile updated successfully!
        </div>
      )}

      <div className="space-y-4">
        <div>
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Username</label>
          <p className="mt-1 text-gray-900 dark:text-gray-100">{user.username}</p>
        </div>

        <div>
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Email</label>
          <p className="mt-1 text-gray-900 dark:text-gray-100">{user.email}</p>
        </div>

        <div>
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Stats</label>
          <div className="mt-1 grid grid-cols-2 gap-4 text-sm">
            <div>
              <span className="text-gray-500 dark:text-gray-400">Total Focus Time:</span>
              <span className="ml-2 font-medium text-gray-900 dark:text-gray-100">{user.totalFocusTime} min</span>
            </div>
            <div>
              <span className="text-gray-500 dark:text-gray-400">Current Streak:</span>
              <span className="ml-2 font-medium text-gray-900 dark:text-gray-100">{user.currentStreak} days</span>
            </div>
            <div>
              <span className="text-gray-500 dark:text-gray-400">Points:</span>
              <span className="ml-2 font-medium text-gray-900 dark:text-gray-100">{user.points}</span>
            </div>
            <div>
              <span className="text-gray-500 dark:text-gray-400">Longest Streak:</span>
              <span className="ml-2 font-medium text-gray-900 dark:text-gray-100">{user.longestStreak} days</span>
            </div>
          </div>
        </div>

        <div className="border-t border-gray-200 dark:border-gray-700 pt-4">
          <div className="flex items-center justify-between">
            <div>
              <label htmlFor="lookingForBuddy" className="text-sm font-medium text-gray-700 dark:text-gray-300">
                Looking for a Focus Buddy
              </label>
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Allow other users to send you buddy requests
              </p>
            </div>
            {editing ? (
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  id="lookingForBuddy"
                  checked={lookingForBuddy}
                  onChange={(e) => setLookingForBuddy(e.target.checked)}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-gray-200 dark:bg-gray-600 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-indigo-300 dark:peer-focus:ring-indigo-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-indigo-600 dark:peer-checked:bg-indigo-500"></div>
              </label>
            ) : (
              <span className={`px-3 py-1 rounded-full text-xs font-medium ${
                user.lookingForBuddy
                  ? 'bg-green-100 dark:bg-green-900/20 text-green-800 dark:text-green-400'
                  : 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300'
              }`}>
                {user.lookingForBuddy ? 'Yes' : 'No'}
              </span>
            )}
          </div>
        </div>

        {editing && (
          <div className="flex justify-end space-x-3 pt-4">
            <button
              onClick={() => {
                setEditing(false);
                setLookingForBuddy(user.lookingForBuddy);
                setError(null);
              }}
              className="px-4 py-2 text-gray-700 dark:text-gray-300 bg-gray-200 dark:bg-gray-700 rounded-md hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              disabled={saving}
              className="px-4 py-2 bg-indigo-600 dark:bg-indigo-500 text-white rounded-md hover:bg-indigo-700 dark:hover:bg-indigo-600 disabled:opacity-50 transition-colors"
            >
              {saving ? 'Saving...' : 'Save'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};