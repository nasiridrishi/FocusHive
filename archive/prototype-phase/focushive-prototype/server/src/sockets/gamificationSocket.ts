import { Server as SocketServer, Socket } from 'socket.io';
import { gamificationService } from '../services/gamificationService';
import { dataStore } from '../data/store';

interface SocketData {
  userId?: string;
  authenticated: boolean;
  currentRoomId?: string;
}

export const setupGamificationSockets = (io: SocketServer) => {
  io.on('connection', (socket: Socket & { data: SocketData }) => {
    
    // Get user stats
    socket.on('gamification:get-stats', async () => {
      try {
        if (!socket.data.userId || !socket.data.authenticated) {
          socket.emit('gamification:error', { message: 'Not authenticated' });
          return;
        }
        
        const stats = await gamificationService.getUserStats(socket.data.userId);
        socket.emit('gamification:stats', { stats });
      } catch (error) {
        console.error('Error getting user stats:', error);
        socket.emit('gamification:error', { message: 'Failed to get stats' });
      }
    });
    
    // Get user achievements
    socket.on('gamification:get-achievements', async () => {
      try {
        if (!socket.data.userId || !socket.data.authenticated) {
          socket.emit('gamification:error', { message: 'Not authenticated' });
          return;
        }
        
        const achievements = await gamificationService.getUserAchievements(socket.data.userId);
        socket.emit('gamification:achievements', { achievements });
      } catch (error) {
        console.error('Error getting achievements:', error);
        socket.emit('gamification:error', { message: 'Failed to get achievements' });
      }
    });
    
    // Get leaderboard
    socket.on('gamification:get-leaderboard', async ({ type, limit = 10 }) => {
      try {
        // Validate type
        if (!['daily', 'weekly', 'monthly', 'allTime'].includes(type)) {
          socket.emit('gamification:error', { message: 'Invalid leaderboard type' });
          return;
        }
        
        const leaderboard = await gamificationService.getLeaderboard(type, limit);
        socket.emit('gamification:leaderboard', { leaderboard, type });
      } catch (error) {
        console.error('Error getting leaderboard:', error);
        socket.emit('gamification:error', { message: 'Failed to get leaderboard' });
      }
    });
  });
};

// Helper function to notify achievement earned (called from timer completion)
export const notifyAchievementEarned = async (io: SocketServer, userId: string, achievement: any) => {
  // Notify the user
  io.to(userId).emit('gamification:achievement-earned', { achievement });
  
  // Optionally notify the room if in a room
  const user = dataStore.getUser(userId);
  if (user) {
    // Could emit to room here if needed
  }
};

// Helper function to notify stats update
export const notifyStatsUpdate = (io: SocketServer, userId: string, update: any) => {
  io.to(userId).emit('gamification:stats-updated', update);
};