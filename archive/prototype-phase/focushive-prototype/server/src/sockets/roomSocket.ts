import { Server as SocketServer, Socket } from 'socket.io';
import { dataStore } from '../data/store';
import { RoomService } from '../services/roomService';
import { presenceService } from '../services/presenceService';
import { timerService } from '../services/timerService';
import { sendSystemMessage } from './chatSocket';
import type { ParticipantStatus, User } from '@focushive/shared';
import { ensureDummyParticipantsVisible } from './roomSocketEnhanced';

interface AuthenticatedSocket extends Socket {
  data: {
    userId?: string;
    authenticated: boolean;
    currentRoomId?: string;
    [key: string]: any;
  };
}

export const setupRoomSockets = (io: SocketServer) => {
  const roomService = new RoomService();

  // Don't create a new connection handler - auth.ts already does that
  // Instead, we'll add our handlers to existing connections
  io.on('connection', (socket: AuthenticatedSocket) => {
    // Skip if handlers already set (to avoid duplicates)
    if ((socket as any)._roomHandlersSet) return;
    (socket as any)._roomHandlersSet = true;
    
    // Only set up room handlers for authenticated sockets
    socket.on('room:join', async ({ roomId }: { roomId: string }) => {
      if (!socket.data.authenticated || !socket.data.userId) {
        socket.emit('room:error', { message: 'Not authenticated' });
        return;
      }

      try {
        const userId = socket.data.userId;
        const room = await roomService.getRoom(roomId);
        
        if (!room) {
          socket.emit('room:error', { message: 'Room not found' });
          return;
        }

        // Check if user is participant
        if (!room.participants.includes(userId)) {
          socket.emit('room:error', { message: 'You are not a participant of this room' });
          return;
        }

        // Leave current room if in one
        if (socket.data.currentRoomId) {
          socket.leave(socket.data.currentRoomId);
          presenceService.removePresence(socket.data.currentRoomId, userId);
        }

        // Join new room
        socket.join(roomId);
        socket.data.currentRoomId = roomId;

        // Add presence
        const participant = presenceService.addPresence(roomId, userId);
        
        // Get user info
        const user = dataStore.getUser(userId);
        if (!user) {
          socket.emit('room:error', { message: 'User not found' });
          return;
        }

        // Notify others in room
        socket.to(roomId).emit('room:user-joined', {
          user: sanitizeUser(user),
          participant
        });

        // Get all participants with presence (including dummy users)
        const participants = ensureDummyParticipantsVisible(roomId);
        
        console.log(`[Room Join] User ${user.username} joining room ${room.name}`);
        console.log(`[Room Join] Room has ${room.participants.length} participants in DB`);
        console.log(`[Room Join] Presence service shows ${participants.length} participants`);
        console.log(`[Room Join] Participants:`, participants.map(p => ({ userId: p.userId, status: p.status })));

        // Send room info to joining user
        socket.emit('room:joined', {
          room,
          participants
        });

        // Send timer state (always send it so client knows the phase)
        const timerState = timerService.getTimerState(roomId);
        if (timerState) {
          socket.emit('timer:state-update', timerState);
          
          // Also send chat state
          const chatEnabled = timerState.phase === 'shortBreak' || timerState.phase === 'longBreak';
          socket.emit('chat:state-changed', { enabled: chatEnabled, phase: timerState.phase });
        }

        // Send updated participant list to everyone
        io.to(roomId).emit('room:participants', { participants });
        
        // Send system message about user joining (only for real users)
        if (!userId.startsWith('dummy-')) {
          sendSystemMessage(io, roomId, `${user.username} has joined the room`);
        }
      } catch (error: any) {
        socket.emit('room:error', { message: error.message || 'Failed to join room' });
      }
    });

    socket.on('room:leave', async ({ roomId }: { roomId: string }) => {
      if (!socket.data.authenticated || !socket.data.userId) {
        return;
      }

      const userId = socket.data.userId;

      // Leave socket room
      socket.leave(roomId);
      
      // Remove presence
      presenceService.removePresence(roomId, userId);
      
      // Clear current room
      if (socket.data.currentRoomId === roomId) {
        socket.data.currentRoomId = undefined;
      }

      // Notify others
      socket.to(roomId).emit('room:user-left', { userId });

      // Get user info for system message
      const user = dataStore.getUser(userId);
      
      // Send updated participant list
      const participants = presenceService.getRoomParticipants(roomId);
      io.to(roomId).emit('room:participants', { participants });
      
      // Send system message about user leaving
      if (user) {
        sendSystemMessage(io, roomId, `${user.username} has left the room`);
      }

      socket.emit('room:left', { roomId });
    });

    socket.on('presence:update', async ({ status, currentTask }: { status: string; currentTask?: string }) => {
      if (!socket.data.authenticated || !socket.data.userId || !socket.data.currentRoomId) {
        return;
      }

      const userId = socket.data.userId;
      const roomId = socket.data.currentRoomId;

      // Update presence
      const updatedStatus = presenceService.updatePresence(roomId, userId, {
        status: status as any,
        currentTask
      });

      // Broadcast to room (including sender)
      io.to(roomId).emit('presence:updated', {
        userId,
        status: updatedStatus
      });
    });

    // Handle disconnect
    socket.on('disconnect', () => {
      if (socket.data.authenticated && socket.data.userId && socket.data.currentRoomId) {
        const userId = socket.data.userId;
        const roomId = socket.data.currentRoomId;

        // Remove presence
        presenceService.removePresence(roomId, userId);
        
        // Remove from room participants if not the owner
        const room = dataStore.getRoom(roomId);
        if (room && room.ownerId !== userId) {
          dataStore.removeParticipant(roomId, userId);
        }

        // Notify others
        socket.to(roomId).emit('room:user-left', { userId });

        // Send updated participant list
        const participants = presenceService.getRoomParticipants(roomId);
        io.to(roomId).emit('room:participants', { participants });
      }
    });
  });
};

// Helper to remove sensitive data from user object
const sanitizeUser = (user: User): Partial<User> => {
  const { password, ...userWithoutPassword } = user;
  return userWithoutPassword;
};