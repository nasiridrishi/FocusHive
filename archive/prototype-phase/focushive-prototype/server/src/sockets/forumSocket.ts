import { Server } from 'socket.io';
import { setupForumHandlers } from './forumHandlers';

export function setupForumSockets(io: Server) {
  io.on('connection', (socket) => {
    setupForumHandlers(io, socket);
  });
}