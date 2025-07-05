/**
 * Enhanced room socket handler that includes dummy participants
 */

import { Server as SocketServer } from 'socket.io';
import { presenceService } from '../services/presenceService';
import { dataStore } from '../data/store';
import type { ParticipantStatus } from '@focushive/shared';

export function enhanceRoomSocketsForDummy(io: SocketServer): void {
  // Override the room:participants event to include dummy users
  const originalEmit = io.to.bind(io);
  
  io.to = function(room: string) {
    const emitter = originalEmit(room);
    const originalEmitMethod = emitter.emit.bind(emitter);
    
    emitter.emit = function(event: string, ...args: any[]) {
      // Intercept room:participants event
      if (event === 'room:participants' && args[0]?.participants) {
        const roomData = dataStore.getRoom(room);
        if (roomData) {
          // Get all participants from presence service
          const allParticipants = presenceService.getRoomParticipants(room);
          
          // Ensure all room participants have presence
          roomData.participants.forEach(userId => {
            if (!allParticipants.find(p => p.userId === userId)) {
              // Add missing participant to presence
              const user = dataStore.getUser(userId);
              if (user && userId.startsWith('dummy-')) {
                presenceService.addPresence(room, userId);
                const status = Math.random() > 0.7 ? 'break' : Math.random() > 0.3 ? 'focusing' : 'idle';
                presenceService.updatePresence(room, userId, {
                  status,
                  currentTask: status === 'focusing' ? 'Working on tasks' : ''
                });
              }
            }
          });
          
          // Get updated participants
          const updatedParticipants = presenceService.getRoomParticipants(room);
          args[0].participants = updatedParticipants;
        }
      }
      
      return originalEmitMethod(event, ...args);
    };
    
    return emitter;
  };
}

/**
 * Ensure dummy participants are visible when someone joins a room
 */
export function ensureDummyParticipantsVisible(roomId: string): ParticipantStatus[] {
  const room = dataStore.getRoom(roomId);
  if (!room) return [];
  
  // Get current presence data
  const currentPresence = presenceService.getRoomParticipants(roomId);
  const presentUserIds = new Set(currentPresence.map(p => p.userId));
  
  // Add presence for any missing dummy participants
  room.participants.forEach(userId => {
    if (!presentUserIds.has(userId) && userId.startsWith('dummy-')) {
      const user = dataStore.getUser(userId);
      if (user) {
        presenceService.addPresence(roomId, userId);
        const status = Math.random() > 0.7 ? 'break' : Math.random() > 0.3 ? 'focusing' : 'idle';
        const tasks = [
          'Working on project',
          'Reviewing materials',
          'Writing documentation',
          'Solving problems',
          'Reading articles',
          'Taking notes'
        ];
        presenceService.updatePresence(roomId, userId, {
          status,
          currentTask: status === 'focusing' ? tasks[Math.floor(Math.random() * tasks.length)] : ''
        });
      }
    }
  });
  
  return presenceService.getRoomParticipants(roomId);
}