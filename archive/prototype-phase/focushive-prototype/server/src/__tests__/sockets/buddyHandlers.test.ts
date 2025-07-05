import { Server as SocketIOServer } from 'socket.io';
import { Socket as ClientSocket } from 'socket.io-client';
import { setupBuddyHandlers } from '../../sockets/buddyHandlers';
import { setupSocketAuth } from '../../sockets/auth';
import { dataStore } from '../../data/store';
import { buddyStore } from '../../data/buddyStore';
import { User } from '@focushive/shared';
import { 
  createTestSocketServer, 
  createAuthenticatedSocket, 
  connectAndWait,
  cleanupSockets,
  cleanupSocketServer,
  TestSocketSetup
} from '../utils/socketTestHelpers';
import { createTestUser } from '../utils/testHelpers';

describe('Buddy Socket Handlers', () => {
  let setup: TestSocketSetup;
  let io: SocketIOServer;
  let serverUrl: string;
  let clientSocket: ClientSocket;
  let user1: User;
  let user2: User;
  let user3: User;
  let token1: string;
  let token2: string;
  let token3: string;

  beforeEach(async () => {
    setup = await createTestSocketServer();
    io = setup.io;
    serverUrl = setup.serverUrl;
    
    // Setup authentication middleware
    setupSocketAuth(io);
    
    // Setup buddy handlers
    io.on('connection', (socket) => {
      setupBuddyHandlers(io, socket);
    });
    
    dataStore.clear();
    buddyStore.reset();
    
    // Create test users with proper tokens
    const testUser1 = await createTestUser({
      email: 'user1@test.com',
      password: 'password123',
      username: 'User1'
    });
    user1 = testUser1.user;
    token1 = testUser1.token;
    
    user1 = dataStore.updateUser(user1.id, {
      avatar: 'avatar1.jpg',
      totalFocusTime: 100,
      currentStreak: 5,
      longestStreak: 10,
      points: 500,
      lastActiveDate: new Date().toISOString(),
      lookingForBuddy: true
    })!;

    const testUser2 = await createTestUser({
      email: 'user2@test.com',
      password: 'password123',
      username: 'User2'
    });
    user2 = testUser2.user;
    token2 = testUser2.token;
    
    user2 = dataStore.updateUser(user2.id, {
      avatar: 'avatar2.jpg',
      totalFocusTime: 120,
      currentStreak: 3,
      longestStreak: 7,
      points: 450,
      lastActiveDate: new Date().toISOString(),
      lookingForBuddy: true
    })!;

    const testUser3 = await createTestUser({
      email: 'user3@test.com',
      password: 'password123',
      username: 'User3'
    });
    user3 = testUser3.user;
    token3 = testUser3.token;
    
    user3 = dataStore.updateUser(user3.id, {
      avatar: 'avatar3.jpg',
      totalFocusTime: 50,
      currentStreak: 1,
      longestStreak: 2,
      points: 100,
      lastActiveDate: new Date().toISOString(),
      lookingForBuddy: false
    })!;

    // Connect and authenticate main test socket
    clientSocket = await createAuthenticatedSocket(serverUrl, token1);
    await connectAndWait(clientSocket);
  });

  afterEach(async () => {
    cleanupSockets(clientSocket);
    await cleanupSocketServer(setup);
  });

  describe('buddy:find-potential', () => {
    it('should return potential buddies', (done) => {
      clientSocket.on('buddy:potential-buddies', ({ buddies }) => {
        expect(buddies).toHaveLength(1);
        expect(buddies[0].userId).toBe(user2.id);
        expect(buddies[0].compatibilityScore).toBeGreaterThan(0);
        done();
      });

      clientSocket.emit('buddy:find-potential');
    });

    it('should handle errors', async () => {
      // Remove user from socket data to simulate error
      const sockets = await io.fetchSockets();
      if (sockets.length > 0) {
        sockets[0].data.user = null;
      }

      return new Promise<void>((resolve) => {
        clientSocket.on('buddy:error', ({ message }) => {
          expect(message).toBe('User not found');
          resolve();
        });

        clientSocket.emit('buddy:find-potential');
      });
    });
  });

  describe('buddy:send-request', () => {
    it('should send buddy request', (done) => {
      clientSocket.on('buddy:request-sent', ({ request }) => {
        expect(request.fromUserId).toBe(user1.id);
        expect(request.toUserId).toBe(user2.id);
        expect(request.message).toBe('Let\'s focus together!');
        expect(request.status).toBe('pending');
        done();
      });

      clientSocket.emit('buddy:send-request', {
        toUserId: user2.id,
        message: 'Let\'s focus together!'
      });
    });

    it('should notify recipient', async () => {
      // Connect user2
      const user2Socket = await createAuthenticatedSocket(serverUrl, token2);
      await connectAndWait(user2Socket);

      return new Promise<void>((resolve) => {
        user2Socket.on('buddy:request-received', ({ request, from }) => {
          expect(request.fromUserId).toBe(user1.id);
          expect(from.username).toBe(user1.username);
          cleanupSockets(user2Socket);
          resolve();
        });

        clientSocket.emit('buddy:send-request', {
          toUserId: user2.id,
          message: 'Let\'s focus together!'
        });
      });
    });
  });

  describe('buddy:accept-request', () => {
    beforeEach((done) => {
      // Send a request first
      clientSocket.once('buddy:request-sent', () => {
        done();
      });

      clientSocket.emit('buddy:send-request', {
        toUserId: user2.id,
        message: 'Be my buddy!'
      });
    });

    it('should accept buddy request', async () => {
      // Connect user2 to accept the request
      const user2Socket = await createAuthenticatedSocket(serverUrl, token2);
      await connectAndWait(user2Socket);

      return new Promise<void>((resolve) => {
        user2Socket.on('buddy:request-accepted', ({ buddyship }) => {
          expect(buddyship.user1Id).toBe(user1.id);
          expect(buddyship.user2Id).toBe(user2.id);
          expect(buddyship.status).toBe('active');
          cleanupSockets(user2Socket);
          resolve();
        });

        user2Socket.emit('buddy:accept-request', {
          fromUserId: user1.id
        });
      });
    });

    it('should notify both users', async () => {
      const user2Socket = await createAuthenticatedSocket(serverUrl, token2);
      await connectAndWait(user2Socket);

      let notificationsReceived = 0;

      return new Promise<void>((resolve) => {
        clientSocket.on('buddy:matched', ({ buddy, buddyship }) => {
          expect(buddy.userId).toBe(user2.id);
          expect(buddyship.status).toBe('active');
          notificationsReceived++;
          if (notificationsReceived === 2) {
            cleanupSockets(user2Socket);
            resolve();
          }
        });

        user2Socket.on('buddy:matched', ({ buddy, buddyship }) => {
          expect(buddy.userId).toBe(user1.id);
          expect(buddyship.status).toBe('active');
          notificationsReceived++;
          if (notificationsReceived === 2) {
            cleanupSockets(user2Socket);
            resolve();
          }
        });

        user2Socket.emit('buddy:accept-request', {
          fromUserId: user1.id
        });
      });
    });
  });

  describe('buddy:decline-request', () => {
    beforeEach((done) => {
      clientSocket.once('buddy:request-sent', () => {
        done();
      });

      clientSocket.emit('buddy:send-request', {
        toUserId: user2.id,
        message: 'Be my buddy!'
      });
    });

    it('should decline buddy request', async () => {
      const user2Socket = await createAuthenticatedSocket(serverUrl, token2);
      await connectAndWait(user2Socket);

      return new Promise<void>((resolve) => {
        user2Socket.on('buddy:request-declined', ({ success }) => {
          expect(success).toBe(true);
          cleanupSockets(user2Socket);
          resolve();
        });

        user2Socket.emit('buddy:decline-request', {
          fromUserId: user1.id
        });
      });
    });
  });

  describe('buddy:get-current', () => {
    it('should return null when no buddy', (done) => {
      clientSocket.on('buddy:current', ({ buddy }) => {
        expect(buddy).toBeNull();
        done();
      });

      clientSocket.emit('buddy:get-current');
    });

    it('should return current buddy', async () => {
      // First establish buddy relationship
      const user2Socket = await createAuthenticatedSocket(serverUrl, token2);
      await connectAndWait(user2Socket);

      // Send request
      await new Promise<void>((resolve) => {
        clientSocket.once('buddy:request-sent', () => {
          resolve();
        });

        clientSocket.emit('buddy:send-request', {
          toUserId: user2.id,
          message: 'Be my buddy!'
        });
      });

      // Accept request
      await new Promise<void>((resolve) => {
        user2Socket.once('buddy:request-accepted', () => {
          resolve();
        });

        user2Socket.emit('buddy:accept-request', {
          fromUserId: user1.id
        });
      });

      // Get current buddy
      return new Promise<void>((resolve) => {
        clientSocket.once('buddy:current', ({ buddy }) => {
          expect(buddy).not.toBeNull();
          expect(buddy.buddyId).toBe(user2.id);
          expect(buddy.username).toBe(user2.username);
          cleanupSockets(user2Socket);
          resolve();
        });

        clientSocket.emit('buddy:get-current');
      });
    });
  });

  describe('buddy:end-buddyship', () => {
    it('should end buddyship', async () => {
      // Setup buddy relationship first
      const user2Socket = await createAuthenticatedSocket(serverUrl, token2);
      await connectAndWait(user2Socket);

      // Send and accept request
      await new Promise<void>((resolve) => {
        clientSocket.once('buddy:request-sent', () => {
          resolve();
        });

        clientSocket.emit('buddy:send-request', {
          toUserId: user2.id,
          message: 'Be my buddy!'
        });
      });

      await new Promise<void>((resolve) => {
        user2Socket.once('buddy:request-accepted', () => {
          resolve();
        });

        user2Socket.emit('buddy:accept-request', {
          fromUserId: user1.id
        });
      });

      // End buddyship
      return new Promise<void>((resolve) => {
        clientSocket.once('buddy:ended', ({ success }) => {
          expect(success).toBe(true);
          cleanupSockets(user2Socket);
          resolve();
        });

        clientSocket.emit('buddy:end-buddyship');
      });
    });
  });

  describe('buddy:get-requests', () => {
    it('should return buddy requests', async () => {
      // User3 sends request to user1
      const user3Socket = await createAuthenticatedSocket(serverUrl, token3);
      await connectAndWait(user3Socket);

      // Send request
      await new Promise<void>((resolve) => {
        user3Socket.once('buddy:request-sent', () => {
          resolve();
        });

        user3Socket.emit('buddy:send-request', {
          toUserId: user1.id,
          message: 'Be my buddy!'
        });
      });

      // User1 gets their requests
      return new Promise<void>((resolve) => {
        clientSocket.once('buddy:requests', ({ sent, received }) => {
          expect(sent).toHaveLength(0);
          expect(received).toHaveLength(1);
          expect(received[0].fromUserId).toBe(user3.id);
          cleanupSockets(user3Socket);
          resolve();
        });

        clientSocket.emit('buddy:get-requests');
      });
    });
  });
});