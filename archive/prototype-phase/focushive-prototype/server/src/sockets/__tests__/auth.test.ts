import { Server as SocketServer } from 'socket.io';
import { Socket, io } from 'socket.io-client';
import jwt from 'jsonwebtoken';
import { 
  createTestSocketServer, 
  createAuthenticatedSocket, 
  waitForSocketEvent,
  connectAndWait,
  cleanupSockets,
  cleanupSocketServer,
  TestSocketSetup
} from '../../__tests__/utils/socketTestHelpers';
import { createTestUser } from '../../__tests__/utils/testHelpers';
import { setupSocketAuth } from '../auth';

describe('Socket Authentication', () => {
  let setup: TestSocketSetup;
  let ioServer: SocketServer;
  let serverUrl: string;

  beforeEach(async () => {
    setup = await createTestSocketServer();
    ioServer = setup.io;
    serverUrl = setup.serverUrl;
    
    // Setup authentication middleware
    setupSocketAuth(ioServer);
  });

  afterEach(async () => {
    await cleanupSocketServer(setup);
  });

  describe('Connection and Authentication', () => {
    it('should authenticate with valid JWT token', async () => {
      const { user, token } = await createTestUser();
      const socket = await createAuthenticatedSocket(serverUrl, token);
      
      await connectAndWait(socket); // This now waits for auth:success
      
      // Verify socket is authenticated
      expect(socket.connected).toBe(true);

      cleanupSockets(socket);
    });

    it('should reject invalid token', async () => {
      const socket = await createAuthenticatedSocket(serverUrl, 'invalid-token');
      
      const errorPromise = waitForSocketEvent(socket, 'auth:error');
      const disconnectPromise = waitForSocketEvent(socket, 'disconnect');
      
      socket.connect();
      
      const error = await errorPromise;
      expect(error.message).toContain('Invalid token');
      
      await disconnectPromise;
      expect(socket.connected).toBe(false);
    });

    it('should reject expired token', async () => {
      // Create an expired token
      const expiredToken = jwt.sign(
        { userId: 'test-user', email: 'test@example.com' },
        process.env.JWT_SECRET || 'test-secret',
        { expiresIn: '-1h' } // Expired 1 hour ago
      );

      const socket = await createAuthenticatedSocket(serverUrl, expiredToken);
      
      const errorPromise = waitForSocketEvent(socket, 'auth:error');
      socket.connect();
      
      const error = await errorPromise;
      expect(error.message).toContain('Token expired');
      
      cleanupSockets(socket);
    });

    it('should disconnect unauthenticated sockets after timeout', async () => {
      // Connect without token - use raw socket
      const socket = io(serverUrl, {
        autoConnect: false,
        transports: ['websocket'],
        reconnection: false
      });
      
      await connectAndWait(socket, false); // Don't wait for auth
      
      // Should disconnect within 5 seconds
      const disconnectPromise = waitForSocketEvent(socket, 'disconnect', 6000);
      const reason = await disconnectPromise;
      
      expect(socket.connected).toBe(false);
      
      cleanupSockets(socket);
    }, 10000); // 10 second timeout for this test

    it('should store userId in socket data after auth', async () => {
      const { user, token } = await createTestUser();
      
      // Track server-side socket - set up listener BEFORE connection
      let serverSocket: any;
      const serverSocketPromise = new Promise((resolve) => {
        ioServer.once('connection', (s) => {
          serverSocket = s;
          resolve(s);
        });
      });
      
      const socket = await createAuthenticatedSocket(serverUrl, token);
      await connectAndWait(socket); // This now waits for auth:success
      await serverSocketPromise; // Wait for server socket

      // Check server-side socket data
      expect(serverSocket.data.userId).toBe(user.id);
      expect(serverSocket.data.authenticated).toBe(true);

      cleanupSockets(socket);
    });

    it('should handle reconnection with same token', async () => {
      const { user, token } = await createTestUser();
      const socket = await createAuthenticatedSocket(serverUrl, token);
      
      // First connection
      await connectAndWait(socket); // This now waits for auth:success
      expect(socket.connected).toBe(true);
      
      // Disconnect
      socket.disconnect();
      await new Promise(resolve => setTimeout(resolve, 100));
      
      // Reconnect - socket.io-client will use the same auth
      const authPromise = waitForSocketEvent(socket, 'auth:success', 2000);
      socket.connect();
      await waitForSocketEvent(socket, 'connect');
      
      // Should get auth:success after reconnection
      const secondAuth = await authPromise;
      expect(secondAuth.userId).toBe(user.id);
      
      cleanupSockets(socket);
    });
  });

  describe('Auth Event Handling', () => {
    it('should emit auth:token event for re-authentication', async () => {
      const { user, token } = await createTestUser();
      
      // Create socket without auth in handshake
      const socket = io(serverUrl, {
        autoConnect: false,
        transports: ['websocket']
      });
      
      await connectAndWait(socket, false); // Don't wait for auth since we have no token
      
      // Send auth token after connection
      const authPromise = waitForSocketEvent(socket, 'auth:success');
      socket.emit('auth:token', { token });
      
      const authData = await authPromise;
      expect(authData.userId).toBe(user.id);
      
      cleanupSockets(socket);
    });

    it('should handle missing token gracefully', async () => {
      const socket = io(serverUrl, {
        autoConnect: false,
        transports: ['websocket']
      });
      
      await connectAndWait(socket, false); // Don't wait for auth since we have no token
      
      const errorPromise = waitForSocketEvent(socket, 'auth:error');
      socket.emit('auth:token', {}); // No token in payload
      
      const error = await errorPromise;
      expect(error.message).toContain('No token provided');
      
      cleanupSockets(socket);
    });
  });
});