import { Server as HttpServer } from 'http';
import { Server as SocketServer } from 'socket.io';
import { io, Socket } from 'socket.io-client';
import express from 'express';
import { authService } from '../../services/authService';
import { createTestUser } from './testHelpers';

export { createTestUser };

export interface TestSocketSetup {
  httpServer: HttpServer;
  io: SocketServer;
  serverUrl: string;
  port: number;
}

export const createTestSocketServer = async (): Promise<TestSocketSetup> => {
  const app = express();
  const httpServer = app.listen(0); // Random port
  const port = (httpServer.address() as any).port;
  
  const io = new SocketServer(httpServer, {
    cors: {
      origin: '*',
      credentials: true
    }
  });

  const serverUrl = `http://localhost:${port}`;
  
  return { httpServer, io, serverUrl, port };
};

export const createAuthenticatedSocket = async (
  serverUrl: string,
  token?: string
): Promise<Socket> => {
  // Create test user and token if not provided
  if (!token) {
    const { token: newToken } = await createTestUser();
    token = newToken;
  }

  const socket = io(serverUrl, {
    auth: { token },
    autoConnect: false,
    transports: ['websocket'],
    reconnection: false // Disable reconnection for tests
  });

  return socket;
};

export const waitForSocketEvent = (
  socket: Socket | SocketServer,
  event: string,
  timeout = 1000
): Promise<any> => {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      reject(new Error(`Timeout waiting for event: ${event}`));
    }, timeout);

    socket.once(event, (data: any) => {
      clearTimeout(timer);
      resolve(data);
    });
  });
};

export const connectAndWait = async (socket: Socket, waitForAuth = true): Promise<void> => {
  // Set up auth:success listener before connecting
  const authPromise = waitForAuth ? waitForSocketEvent(socket, 'auth:success', 2000) : Promise.resolve();
  
  socket.connect();
  await waitForSocketEvent(socket, 'connect');
  
  // Wait for auth if needed
  if (waitForAuth) {
    await authPromise;
  }
};

export const cleanupSockets = (...sockets: Socket[]) => {
  sockets.forEach(socket => {
    if (socket.connected) {
      socket.disconnect();
    }
  });
};

export const cleanupSocketServer = async (setup: TestSocketSetup) => {
  await new Promise<void>((resolve) => {
    setup.io.close(() => {
      setup.httpServer.close(() => {
        resolve();
      });
    });
  });
};