import { Server, Socket } from 'socket.io';
import { ForumService } from '../services/forumService';
import { logger } from '../utils/logger';

const forumService = new ForumService();

export function setupForumHandlers(io: Server, socket: Socket) {
  // Join forum namespace
  socket.on('forum:join', () => {
    socket.join('forum');
    console.log(`Socket ${socket.id} joined forum`);
  });

  // Leave forum namespace
  socket.on('forum:leave', () => {
    socket.leave('forum');
    console.log(`Socket ${socket.id} left forum`);
  });

  // Get posts
  socket.on('forum:get-posts', async (filters) => {
    try {
      const posts = await forumService.getPosts(filters);
      socket.emit('forum:posts', { posts });
    } catch (error: any) {
      logger.error('Error getting posts:', error);
      socket.emit('forum:error', { message: error.message });
    }
  });

  // Get specific post
  socket.on('forum:get-post', async ({ postId }) => {
    try {
      const post = await forumService.getPost(postId);
      if (post) {
        socket.emit('forum:post-updated', { post });
      }
    } catch (error: any) {
      logger.error('Error getting post:', error);
      socket.emit('forum:error', { message: error.message });
    }
  });

  // Create post
  socket.on('forum:create-post', async (postData) => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('forum:error', { message: 'User not found' });
        return;
      }

      const post = await forumService.createPost(user.id, postData);
      
      // Emit to all forum users
      io.to('forum').emit('forum:new-post', { post });
      
      socket.emit('forum:post-created', { post });
    } catch (error: any) {
      logger.error('Error creating post:', error);
      socket.emit('forum:error', { message: error.message });
    }
  });

  // Respond to post
  socket.on('forum:respond-to-post', async ({ postId }) => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('forum:error', { message: 'User not found' });
        return;
      }

      const updatedPost = await forumService.respondToPost(postId, user.id);
      
      // Notify all forum users about the update
      io.to('forum').emit('forum:post-updated', { post: updatedPost });
      
      socket.emit('forum:response-sent', { post: updatedPost });
    } catch (error: any) {
      logger.error('Error responding to post:', error);
      socket.emit('forum:error', { message: error.message });
    }
  });

  // Update post status
  socket.on('forum:update-post-status', async ({ postId, status }) => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('forum:error', { message: 'User not found' });
        return;
      }

      const updatedPost = await forumService.updatePostStatus(postId, user.id, status);
      
      // Notify all forum users
      io.to('forum').emit('forum:post-updated', { post: updatedPost });
      
      socket.emit('forum:status-updated', { post: updatedPost });
    } catch (error: any) {
      logger.error('Error updating post status:', error);
      socket.emit('forum:error', { message: error.message });
    }
  });

  // Global Chat operations (renamed to avoid conflict with room chat)
  socket.on('global-chat:get-messages', async () => {
    try {
      const data = await forumService.getChatMessages();
      socket.emit('global-chat:messages', data);
    } catch (error: any) {
      logger.error('Error getting chat messages:', error);
      socket.emit('global-chat:error', { message: error.message });
    }
  });

  socket.on('global-chat:send-message', async ({ message }) => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('global-chat:error', { message: 'User not found' });
        return;
      }

      const chatMessage = await forumService.sendChatMessage(user.id, message);
      
      // Emit to all connected users
      io.emit('global-chat:new-message', { message: chatMessage });
    } catch (error: any) {
      logger.error('Error sending chat message:', error);
      socket.emit('global-chat:error', { message: error.message });
    }
  });

  socket.on('global-chat:report-message', async ({ messageId }) => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('global-chat:error', { message: 'User not found' });
        return;
      }

      await forumService.reportChatMessage(messageId, user.id);
      
      // Refresh messages for everyone if message was hidden
      const data = await forumService.getChatMessages();
      io.emit('global-chat:messages', data);
    } catch (error: any) {
      logger.error('Error reporting message:', error);
      socket.emit('global-chat:error', { message: error.message });
    }
  });

  // Get user connections
  socket.on('forum:get-connections', async () => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('forum:error', { message: 'User not found' });
        return;
      }

      const connections = await forumService.getUserConnections(user.id);
      socket.emit('forum:connections', { connections });
    } catch (error: any) {
      logger.error('Error getting connections:', error);
      socket.emit('forum:error', { message: error.message });
    }
  });

  // Connect to post (create buddy request)
  socket.on('forum:connect-to-post', async ({ postId, message }) => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('forum:error', { message: 'User not found' });
        return;
      }

      const post = await forumService.getPost(postId);
      if (!post) {
        socket.emit('forum:error', { message: 'Post not found' });
        return;
      }

      const connection = await forumService.createBuddyConnection(postId, user.id, post.userId);
      
      // Update the requester's connections
      socket.emit('forum:connection-update', { connection });
      
      // Notify recipient
      const recipientSockets = await io.fetchSockets();
      for (const recipientSocket of recipientSockets) {
        if (recipientSocket.data.user?.id === post.userId) {
          recipientSocket.emit('forum:new-connection-request', { connection });
          break;
        }
      }
    } catch (error: any) {
      logger.error('Error connecting to post:', error);
      socket.emit('forum:error', { message: error.message });
    }
  });

  // Connection operations (legacy)
  socket.on('forum:create-connection', async ({ postId, recipientId }) => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('forum:error', { message: 'User not found' });
        return;
      }

      const connection = await forumService.createBuddyConnection(postId, user.id, recipientId);
      
      socket.emit('forum:connection-created', { connection });
      
      // Notify recipient
      const recipientSockets = await io.fetchSockets();
      for (const recipientSocket of recipientSockets) {
        if (recipientSocket.data.user?.id === recipientId) {
          recipientSocket.emit('forum:connection-request', { connection });
          break;
        }
      }
    } catch (error: any) {
      logger.error('Error creating connection:', error);
      socket.emit('forum:error', { message: error.message });
    }
  });

  // Accept connection
  socket.on('forum:accept-connection', async ({ connectionId }) => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('forum:error', { message: 'User not found' });
        return;
      }

      const connection = await forumService.updateConnection(connectionId, user.id, 'accepted');
      
      // Notify both users
      const otherUserId = connection.requesterId === user.id ? connection.requestedUserId : connection.requesterId;
      const otherSockets = await io.fetchSockets();
      for (const otherSocket of otherSockets) {
        if (otherSocket.data.user?.id === otherUserId) {
          otherSocket.emit('forum:connection-update', { connection });
          break;
        }
      }
      
      socket.emit('forum:connection-update', { connection });
    } catch (error: any) {
      logger.error('Error accepting connection:', error);
      socket.emit('forum:error', { message: error.message });
    }
  });

  // Decline connection
  socket.on('forum:decline-connection', async ({ connectionId }) => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('forum:error', { message: 'User not found' });
        return;
      }

      await forumService.updateConnection(connectionId, user.id, 'declined');
      
      // Refresh connections for the user
      const connections = await forumService.getUserConnections(user.id);
      socket.emit('forum:connections', { connections });
    } catch (error: any) {
      logger.error('Error declining connection:', error);
      socket.emit('forum:error', { message: error.message });
    }
  });

  // Update connection (legacy)
  socket.on('forum:update-connection', async ({ connectionId, status }) => {
    try {
      const user = socket.data.user;
      if (!user) {
        socket.emit('forum:error', { message: 'User not found' });
        return;
      }

      const connection = await forumService.updateConnection(connectionId, user.id, status);
      
      socket.emit('forum:connection-updated', { connection });
      
      // Notify requester
      const requesterSockets = await io.fetchSockets();
      for (const requesterSocket of requesterSockets) {
        if (requesterSocket.data.user?.id === connection.requesterId) {
          requesterSocket.emit('forum:connection-updated', { connection });
          break;
        }
      }
    } catch (error: any) {
      logger.error('Error updating connection:', error);
      socket.emit('forum:error', { message: error.message });
    }
  });

  // Get forum stats
  socket.on('forum:get-stats', async () => {
    try {
      const stats = await forumService.getForumStats();
      socket.emit('forum:stats', stats);
    } catch (error: any) {
      logger.error('Error getting forum stats:', error);
      socket.emit('forum:error', { message: error.message });
    }
  });
}