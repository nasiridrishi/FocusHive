import { Server as SocketServer, Socket } from 'socket.io';
import { timerService } from '../services/timerService';
import { TimerState } from '@focushive/shared';
import { gamificationService } from '../services/gamificationService';
import { dataStore } from '../data/store';
import { notifyAchievementEarned, notifyStatsUpdate } from './gamificationSocket';
import { notifyChatStateChange } from './chatSocket';

interface SocketData {
  userId?: string;
  authenticated: boolean;
  currentRoomId?: string;
}

interface TimerRequest {
  roomId: string;
}

// Track timer intervals for each room
const timerIntervals = new Map<string, NodeJS.Timeout>();

export const setupTimerSockets = (io: SocketServer) => {
  io.on('connection', (socket: Socket & { data: SocketData }) => {
    // Timer Start
    socket.on('timer:start', async ({ roomId }: TimerRequest) => {
      try {
        if (!socket.data.userId || !socket.data.authenticated) {
          socket.emit('timer:error', { message: 'Not authenticated' });
          return;
        }

        const state = timerService.startTimer(roomId, socket.data.userId);
        
        // Broadcast to all room participants
        io.to(roomId).emit('timer:started', { state });
        
        // Start sending timer ticks
        startTimerTicks(io, roomId);
      } catch (error: any) {
        socket.emit('timer:error', { message: error.message });
      }
    });

    // Timer Pause
    socket.on('timer:pause', async ({ roomId }: TimerRequest) => {
      try {
        if (!socket.data.userId || !socket.data.authenticated) {
          socket.emit('timer:error', { message: 'Not authenticated' });
          return;
        }

        const state = timerService.pauseTimer(roomId, socket.data.userId);
        
        // Stop timer ticks
        stopTimerTicks(roomId);
        
        // Broadcast to all room participants
        io.to(roomId).emit('timer:paused', { state });
      } catch (error: any) {
        socket.emit('timer:error', { message: error.message });
      }
    });

    // Timer Resume
    socket.on('timer:resume', async ({ roomId }: TimerRequest) => {
      try {
        if (!socket.data.userId || !socket.data.authenticated) {
          socket.emit('timer:error', { message: 'Not authenticated' });
          return;
        }

        const state = timerService.resumeTimer(roomId, socket.data.userId);
        
        // Restart timer ticks
        startTimerTicks(io, roomId);
        
        // Broadcast to all room participants
        io.to(roomId).emit('timer:resumed', { state });
      } catch (error: any) {
        socket.emit('timer:error', { message: error.message });
      }
    });

    // Timer Reset
    socket.on('timer:reset', async ({ roomId }: TimerRequest) => {
      try {
        if (!socket.data.userId || !socket.data.authenticated) {
          socket.emit('timer:error', { message: 'Not authenticated' });
          return;
        }

        const state = timerService.resetTimer(roomId, socket.data.userId);
        
        // Stop timer ticks
        stopTimerTicks(roomId);
        
        // Broadcast to all room participants
        io.to(roomId).emit('timer:reset', { state });
      } catch (error: any) {
        socket.emit('timer:error', { message: error.message });
      }
    });

    // Timer Skip to Next Phase
    socket.on('timer:skip', async ({ roomId }: TimerRequest) => {
      try {
        if (!socket.data.userId || !socket.data.authenticated) {
          socket.emit('timer:error', { message: 'Not authenticated' });
          return;
        }

        const state = timerService.skipToNextPhase(roomId, socket.data.userId);
        
        // Stop timer ticks since timer resets to idle
        stopTimerTicks(roomId);
        
        // Broadcast phase change
        io.to(roomId).emit('timer:phase-changed', { state });
        
        // Notify chat state change
        const chatEnabled = state.phase === 'shortBreak' || state.phase === 'longBreak';
        notifyChatStateChange(io, roomId, chatEnabled, state.phase);
      } catch (error: any) {
        socket.emit('timer:error', { message: error.message });
      }
    });

    // Get Timer State
    socket.on('timer:get-state', async ({ roomId }: TimerRequest) => {
      try {
        const timer = timerService.getTimer(roomId);
        if (timer) {
          const remaining = timerService.getRemainingTime(roomId);
          socket.emit('timer:state', {
            ...timer,
            remaining,
            phase: timer.phase,
            status: timer.status,
            duration: timer.duration
          });
        } else {
          // Send default state if no timer exists
          socket.emit('timer:state', {
            phase: 'work',
            status: 'idle',
            remaining: 1500, // 25 minutes
            duration: 1500,
            startedAt: null,
            pausedAt: null
          });
        }
      } catch (error: any) {
        socket.emit('timer:error', { message: error.message });
      }
    });
  });
};

// Start sending timer ticks for a room
function startTimerTicks(io: SocketServer, roomId: string) {
  // Clear any existing interval
  stopTimerTicks(roomId);
  
  // Send immediate tick
  sendTimerTick(io, roomId);
  
  // Set up interval for periodic ticks (every second)
  const interval = setInterval(() => {
    const timer = timerService.getTimer(roomId);
    if (!timer || timer.status !== 'running') {
      stopTimerTicks(roomId);
      return;
    }
    
    sendTimerTick(io, roomId);
    
    // Check if timer completed
    const remaining = timerService.getRemainingTime(roomId);
    if (remaining <= 0) {
      handlePhaseComplete(io, roomId);
    }
  }, 1000);
  
  timerIntervals.set(roomId, interval);
}

// Stop sending timer ticks for a room
function stopTimerTicks(roomId: string) {
  const interval = timerIntervals.get(roomId);
  if (interval) {
    clearInterval(interval);
    timerIntervals.delete(roomId);
  }
}

// Send a timer tick update
function sendTimerTick(io: SocketServer, roomId: string) {
  const timer = timerService.getTimer(roomId);
  if (!timer) return;
  
  const remaining = timerService.getRemainingTime(roomId);
  io.to(roomId).emit('timer:tick', {
    ...timer,
    remaining,
    phase: timer.phase,
    status: timer.status,
    duration: timer.duration
  });
}

// Handle phase completion
async function handlePhaseComplete(io: SocketServer, roomId: string) {
  const timer = timerService.getTimer(roomId);
  if (!timer) return;
  
  const completedPhase = timer.phase;
  
  // Stop ticks
  stopTimerTicks(roomId);
  
  // Auto-advance to next phase (but keep it idle)
  try {
    // Get any user from the room to perform the phase transition
    const room = require('../data/store').dataStore.getRoom(roomId);
    if (room && room.participants.length > 0) {
      const userId = room.participants[0];
      timerService.nextPhase(roomId, userId);
      
      const newTimer = timerService.getTimer(roomId);
      
      // Award points and check achievements if it was a work phase
      if (completedPhase === 'work' && timer.duration) {
        const duration = Math.floor(timer.duration / 60); // Convert to minutes
        
        // Process gamification for all participants
        for (const participantId of room.participants) {
          try {
            const user = dataStore.getUser(participantId);
            if (user) {
              const result = await gamificationService.handleSessionComplete(
                participantId, 
                duration, 
                user.currentStreak || 0
              );
              
              // Notify stats update
              notifyStatsUpdate(io, participantId, {
                pointsEarned: result.pointsEarned,
                newTotalPoints: (user.points || 0) + result.pointsEarned,
                newStreak: result.newStreak,
                sessionTime: duration
              });
              
              // Notify achievements
              for (const achievement of result.achievements) {
                await notifyAchievementEarned(io, participantId, achievement);
              }
            }
          } catch (error) {
            console.error(`Error processing gamification for user ${participantId}:`, error);
          }
        }
      }
      
      io.to(roomId).emit('timer:phase-complete', {
        completedPhase,
        nextPhase: newTimer?.phase,
        state: newTimer
      });
      
      // Notify chat state change
      if (newTimer) {
        const chatEnabled = newTimer.phase === 'shortBreak' || newTimer.phase === 'longBreak';
        notifyChatStateChange(io, roomId, chatEnabled, newTimer.phase);
      }
    }
  } catch (error) {
    console.error('Error handling phase complete:', error);
  }
}

// Clean up intervals on server shutdown
export function cleanupTimerSockets() {
  timerIntervals.forEach(interval => clearInterval(interval));
  timerIntervals.clear();
}