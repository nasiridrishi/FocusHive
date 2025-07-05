import { Server, Socket } from 'socket.io';
import { BuddyService } from '../services/buddyService';
import { dataStore } from '../data/store';
import { logger } from '../utils/logger';

const buddyService = new BuddyService();

export function setupBuddyHandlers(io: Server, socket: Socket) {
  // Find potential buddies
  socket.on('buddy:find-potential', async () => {
    try {
      console.log('Finding potential buddies for socket:', socket.id);
      const user = socket.data.user;
      if (!user) {
        console.log('No user found in socket data');
        socket.emit('buddy:error', { message: 'User not found' });
        return;
      }

      console.log('Finding buddies for user:', user.id, user.username);
      const buddies = await buddyService.findPotentialBuddies(user.id);
      console.log('Found potential buddies:', buddies.length);
      socket.emit('buddy:potential-buddies', { buddies });
    } catch (error: any) {
      logger.error('Error finding potential buddies:', error);
      socket.emit('buddy:error', { message: error.message });
    }
  });

  // Send buddy request
  socket.on('buddy:send-request', async ({ toUserId, message }) => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('buddy:error', { message: 'User not found' });
        return;
      }

      const request = await buddyService.sendBuddyRequest(user.id, toUserId, message);
      socket.emit('buddy:request-sent', { request });

      // Notify recipient if online
      const recipientSocketId = await findUserSocketId(io, toUserId);
      if (recipientSocketId) {
        const fromUser = dataStore.getUser(user.id);
        io.to(recipientSocketId).emit('buddy:request-received', {
          request,
          from: {
            userId: fromUser?.id,
            username: fromUser?.username,
            avatar: fromUser?.avatar,
            totalFocusTime: fromUser?.totalFocusTime,
            currentStreak: fromUser?.currentStreak
          }
        });
      }
    } catch (error: any) {
      logger.error('Error sending buddy request:', error);
      socket.emit('buddy:error', { message: error.message });
    }
  });

  // Accept buddy request
  socket.on('buddy:accept-request', async ({ fromUserId }) => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('buddy:error', { message: 'User not found' });
        return;
      }

      const result = await buddyService.acceptBuddyRequest(fromUserId, user.id);
      socket.emit('buddy:request-accepted', { buddyship: result.buddyship });

      // Notify both users they are now buddies
      const buddy1 = dataStore.getUser(fromUserId);
      const buddy2 = dataStore.getUser(user.id);

      if (buddy1 && buddy2 && result.buddyship) {
        // Notify user who accepted
        socket.emit('buddy:matched', {
          buddy: {
            userId: buddy1.id,
            username: buddy1.username,
            avatar: buddy1.avatar,
            totalFocusTime: buddy1.totalFocusTime,
            currentStreak: buddy1.currentStreak
          },
          buddyship: result.buddyship
        });

        // Notify user who sent request
        const senderSocketId = await findUserSocketId(io, fromUserId);
        if (senderSocketId) {
          io.to(senderSocketId).emit('buddy:matched', {
            buddy: {
              userId: buddy2.id,
              username: buddy2.username,
              avatar: buddy2.avatar,
              totalFocusTime: buddy2.totalFocusTime,
              currentStreak: buddy2.currentStreak
            },
            buddyship: result.buddyship
          });
        }
      }
    } catch (error: any) {
      logger.error('Error accepting buddy request:', error);
      socket.emit('buddy:error', { message: error.message });
    }
  });

  // Decline buddy request
  socket.on('buddy:decline-request', async ({ fromUserId }) => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('buddy:error', { message: 'User not found' });
        return;
      }

      const result = await buddyService.declineBuddyRequest(fromUserId, user.id);
      socket.emit('buddy:request-declined', { success: result.success });

      // Optionally notify sender that request was declined
      const senderSocketId = await findUserSocketId(io, fromUserId);
      if (senderSocketId) {
        io.to(senderSocketId).emit('buddy:request-update', {
          fromUserId,
          toUserId: user.id,
          status: 'declined'
        });
      }
    } catch (error: any) {
      logger.error('Error declining buddy request:', error);
      socket.emit('buddy:error', { message: error.message });
    }
  });

  // Get current buddy
  socket.on('buddy:get-current', async () => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('buddy:error', { message: 'User not found' });
        return;
      }

      const buddy = await buddyService.getCurrentBuddy(user.id);
      socket.emit('buddy:current', { buddy });
    } catch (error: any) {
      logger.error('Error getting current buddy:', error);
      socket.emit('buddy:error', { message: error.message });
    }
  });

  // End buddyship
  socket.on('buddy:end-buddyship', async () => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('buddy:error', { message: 'User not found' });
        return;
      }

      // Get current buddy before ending
      const currentBuddy = await buddyService.getCurrentBuddy(user.id);
      const result = await buddyService.endBuddyship(user.id);
      
      socket.emit('buddy:ended', { success: result.success });

      // Notify the other buddy
      if (currentBuddy) {
        const buddySocketId = await findUserSocketId(io, currentBuddy.buddyId);
        if (buddySocketId) {
          io.to(buddySocketId).emit('buddy:ended', { 
            success: true,
            endedBy: user.id
          });
        }
      }
    } catch (error: any) {
      logger.error('Error ending buddyship:', error);
      socket.emit('buddy:error', { message: error.message });
    }
  });

  // Get buddy requests
  socket.on('buddy:get-requests', async () => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('buddy:error', { message: 'User not found' });
        return;
      }

      const requests = await buddyService.getBuddyRequests(user.id);
      socket.emit('buddy:requests', requests);
    } catch (error: any) {
      logger.error('Error getting buddy requests:', error);
      socket.emit('buddy:error', { message: error.message });
    }
  });

  // Update shared goals
  socket.on('buddy:update-goals', async ({ goals }) => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('buddy:error', { message: 'User not found' });
        return;
      }

      const result = await buddyService.updateSharedGoals(user.id, goals);
      socket.emit('buddy:goals-updated', { success: result.success, goals });

      // Notify buddy about updated goals
      const currentBuddy = await buddyService.getCurrentBuddy(user.id);
      if (currentBuddy) {
        const buddySocketId = await findUserSocketId(io, currentBuddy.buddyId);
        if (buddySocketId) {
          io.to(buddySocketId).emit('buddy:goals-updated', { 
            success: true,
            goals,
            updatedBy: user.id
          });
        }
      }
    } catch (error: any) {
      logger.error('Error updating shared goals:', error);
      socket.emit('buddy:error', { message: error.message });
    }
  });
}

// Helper function to find user's socket ID
async function findUserSocketId(io: Server, userId: string): Promise<string | null> {
  const sockets = await io.fetchSockets();
  for (const socket of sockets) {
    if (socket.data.user?.id === userId) {
      return socket.id;
    }
  }
  return null;
}