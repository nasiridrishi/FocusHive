import { Server as SocketServer, Socket } from 'socket.io';
import { authService } from '../services/authService';
import { dataStore } from '../data/store';

interface SocketData {
  userId?: string;
  authenticated: boolean;
  joinedAt: number;
  currentRoomId?: string;
  authError?: string;
  user?: any; // Add user object
}

interface AuthHandshake {
  auth: {
    token?: string;
  };
}

const AUTH_TIMEOUT = 5000; // 5 seconds to authenticate

export const setupSocketAuth = (io: SocketServer) => {
  // Middleware to check initial auth token
  io.use(async (socket: Socket & { data: SocketData }, next) => {
    const token = (socket.handshake as AuthHandshake).auth?.token;
    
    socket.data = {
      authenticated: false,
      joinedAt: Date.now()
    };

    if (token) {
      try {
        const decoded = authService.verifyToken(token);
        const user = dataStore.getUser(decoded.userId);
        
        if (user) {
          socket.data.userId = user.id;
          socket.data.authenticated = true;
          socket.data.user = user;
        } else {
          socket.data.authError = 'User not found';
        }
      } catch (error: any) {
        socket.data.authError = error.name === 'TokenExpiredError' 
          ? 'Token expired' 
          : 'Invalid token';
      }
    }
    
    // Always allow connection, handle auth after
    next();
  });

  io.on('connection', (socket: Socket & { data: SocketData }) => {
    console.log('Socket connected:', socket.id);

    // Check for auth error from middleware
    if (socket.data.authError) {
      socket.emit('auth:error', { message: socket.data.authError });
      socket.disconnect(true);
      return;
    }

    // If already authenticated from handshake
    if (socket.data.authenticated && socket.data.userId) {
      socket.emit('auth:success', { 
        userId: socket.data.userId,
        authenticated: true 
      });
    } else {
      // Set timeout for authentication
      const authTimeout = setTimeout(() => {
        if (!socket.data.authenticated) {
          socket.disconnect(true);
        }
      }, AUTH_TIMEOUT);

      // Handle auth:token event for post-connection auth
      socket.on('auth:token', async ({ token }: { token?: string }) => {
        if (!token) {
          socket.emit('auth:error', { message: 'No token provided' });
          return;
        }

        try {
          const decoded = authService.verifyToken(token);
          const user = dataStore.getUser(decoded.userId);
          
          if (!user) {
            socket.emit('auth:error', { message: 'User not found' });
            return;
          }

          // Update socket data
          socket.data.userId = user.id;
          socket.data.authenticated = true;
          socket.data.user = user;
          
          // Clear timeout
          clearTimeout(authTimeout);
          
          // Emit success
          socket.emit('auth:success', { 
            userId: user.id,
            authenticated: true 
          });
        } catch (error: any) {
          if (error.name === 'TokenExpiredError') {
            socket.emit('auth:error', { message: 'Token expired' });
          } else {
            socket.emit('auth:error', { message: 'Invalid token' });
          }
        }
      });
    }

    // Handle disconnect
    socket.on('disconnect', () => {
      console.log('Socket disconnected:', socket.id);
      
      // Clean up any room presence
      if (socket.data.currentRoomId && socket.data.userId) {
        // This will be handled by room socket handlers
        socket.to(socket.data.currentRoomId).emit('room:user-left', {
          userId: socket.data.userId
        });
      }
    });
  });
};