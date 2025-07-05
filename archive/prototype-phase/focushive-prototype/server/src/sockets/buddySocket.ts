import { Server } from 'socket.io';
import { setupBuddyHandlers } from './buddyHandlers';

export function setupBuddySockets(io: Server) {
  io.on('connection', (socket) => {
    setupBuddyHandlers(io, socket);
  });
}