import { Server as SocketServer, Socket } from 'socket.io';
import { chatService } from '../services/chatService';

interface SocketData {
  userId?: string;
  authenticated: boolean;
  currentRoomId?: string;
}

interface SendMessageRequest {
  roomId: string;
  message: string;
}

interface GetHistoryRequest {
  roomId: string;
  limit?: number;
}

interface MarkReadRequest {
  roomId: string;
}

export const setupChatSockets = (io: SocketServer) => {
  io.on('connection', (socket: Socket & { data: SocketData }) => {
    
    // Send message
    socket.on('chat:send-message', async (data: SendMessageRequest) => {
      try {
        if (!socket.data.userId || !socket.data.authenticated) {
          socket.emit('chat:error', { message: 'Not authenticated' });
          return;
        }

        if (!data.roomId || !data.message) {
          socket.emit('chat:error', { message: 'Room ID and message are required' });
          return;
        }

        // Send the message
        const message = chatService.sendMessage(
          data.roomId,
          socket.data.userId,
          data.message
        );

        // Broadcast to all room participants
        io.to(data.roomId).emit('chat:message', { message });
      } catch (error: any) {
        socket.emit('chat:error', { message: error.message });
      }
    });

    // Get message history
    socket.on('chat:get-history', async (data: GetHistoryRequest) => {
      try {
        if (!socket.data.userId || !socket.data.authenticated) {
          socket.emit('chat:error', { message: 'Not authenticated' });
          return;
        }

        if (!data.roomId) {
          socket.emit('chat:error', { message: 'Room ID is required' });
          return;
        }

        const messages = chatService.getMessageHistory(
          data.roomId,
          data.limit || 100
        );

        socket.emit('chat:history', { messages });
      } catch (error: any) {
        socket.emit('chat:error', { message: error.message });
      }
    });

    // Mark messages as read
    socket.on('chat:mark-read', async (data: MarkReadRequest) => {
      try {
        if (!socket.data.userId || !socket.data.authenticated) {
          socket.emit('chat:error', { message: 'Not authenticated' });
          return;
        }

        if (!data.roomId) {
          socket.emit('chat:error', { message: 'Room ID is required' });
          return;
        }

        chatService.markMessagesAsRead(data.roomId, socket.data.userId);
        socket.emit('chat:read-confirmed', { roomId: data.roomId });
      } catch (error: any) {
        socket.emit('chat:error', { message: error.message });
      }
    });
  });
};

// Helper function to send system message to room
export const sendSystemMessage = (io: SocketServer, roomId: string, message: string) => {
  try {
    const systemMessage = chatService.sendSystemMessage(roomId, message);
    io.to(roomId).emit('chat:message', { message: systemMessage });
  } catch (error) {
    console.error('Error sending system message:', error);
  }
};

// Helper function to notify chat state change
export const notifyChatStateChange = (io: SocketServer, roomId: string, enabled: boolean, phase: string) => {
  io.to(roomId).emit('chat:state-changed', { enabled, phase });
};