import { Server as SocketServer } from 'socket.io';
import { Socket, io } from 'socket.io-client';
import { 
  createTestSocketServer, 
  createAuthenticatedSocket, 
  waitForSocketEvent,
  connectAndWait,
  cleanupSockets,
  cleanupSocketServer,
  TestSocketSetup
} from '../../__tests__/utils/socketTestHelpers';
import { createTestUser, createTestRoom } from '../../__tests__/utils/testHelpers';
import { setupSocketAuth } from '../auth';
import { setupRoomSockets } from '../roomSocket';
import { dataStore } from '../../data/store';
import { RoomService } from '../../services/roomService';
import { presenceService } from '../../services/presenceService';

describe('Room Socket Events', () => {
  let setup: TestSocketSetup;
  let ioServer: SocketServer;
  let serverUrl: string;
  let roomService: RoomService;

  beforeEach(async () => {
    setup = await createTestSocketServer();
    ioServer = setup.io;
    serverUrl = setup.serverUrl;
    roomService = new RoomService();
    
    // Setup socket handlers
    setupSocketAuth(ioServer);
    setupRoomSockets(ioServer);
  });

  afterEach(async () => {
    await cleanupSocketServer(setup);
  });

  describe('Room Join/Leave', () => {
    it('should join room if user is participant', async () => {
      const { user, token } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const socket = await createAuthenticatedSocket(serverUrl, token);
      await connectAndWait(socket); // This now waits for auth:success

      // Join room
      const joinPromise = waitForSocketEvent(socket, 'room:joined');
      socket.emit('room:join', { roomId: room.id });

      const joinData = await joinPromise;
      expect(joinData.room.id).toBe(room.id);
      expect(joinData.participants).toBeDefined();
      expect(joinData.participants.length).toBeGreaterThan(0);

      cleanupSockets(socket);
    });

    it('should reject join if user not in room', async () => {
      const { user: owner } = await createTestUser();
      const { user: otherUser, token: otherToken } = await createTestUser();
      
      const room = await roomService.createRoom(owner.id, {
        name: 'Private Room',
        type: 'private',
        password: 'secret'
      });

      const socket = await createAuthenticatedSocket(serverUrl, otherToken);
      await connectAndWait(socket);

      // Try to join room without being a participant
      const errorPromise = waitForSocketEvent(socket, 'room:error');
      socket.emit('room:join', { roomId: room.id });

      const error = await errorPromise;
      expect(error.message).toContain('not a participant');

      cleanupSockets(socket);
    });

    it('should broadcast user-joined to other participants', async () => {
      const { user: owner, token: ownerToken } = await createTestUser();
      const { user: participant, token: participantToken } = await createTestUser();
      
      const room = await roomService.createRoom(owner.id, {
        name: 'Broadcast Test Room',
        type: 'public'
      });

      // Owner connects and joins room
      const ownerSocket = await createAuthenticatedSocket(serverUrl, ownerToken);
      await connectAndWait(ownerSocket);
      ownerSocket.emit('room:join', { roomId: room.id });
      await waitForSocketEvent(ownerSocket, 'room:joined');

      // Participant joins the room
      await roomService.joinRoom(participant.id, room.id);

      // Participant connects
      const participantSocket = await createAuthenticatedSocket(serverUrl, participantToken);
      await connectAndWait(participantSocket);

      // Owner should receive user-joined event
      const userJoinedPromise = waitForSocketEvent(ownerSocket, 'room:user-joined');
      participantSocket.emit('room:join', { roomId: room.id });

      const joinedData = await userJoinedPromise;
      expect(joinedData.user.id).toBe(participant.id);
      expect(joinedData.participant.userId).toBe(participant.id);

      cleanupSockets(ownerSocket, participantSocket);
    });

    it('should leave room and broadcast user-left', async () => {
      const { user: owner, token: ownerToken } = await createTestUser();
      const { user: participant, token: participantToken } = await createTestUser();
      
      const room = await roomService.createRoom(owner.id, {
        name: 'Leave Test Room',
        type: 'public'
      });

      // Both users join room
      await roomService.joinRoom(participant.id, room.id);

      // Connect both sockets
      const ownerSocket = await createAuthenticatedSocket(serverUrl, ownerToken);
      const participantSocket = await createAuthenticatedSocket(serverUrl, participantToken);
      
      await connectAndWait(ownerSocket);
      await connectAndWait(participantSocket);

      // Join rooms
      ownerSocket.emit('room:join', { roomId: room.id });
      participantSocket.emit('room:join', { roomId: room.id });
      await waitForSocketEvent(ownerSocket, 'room:joined');
      await waitForSocketEvent(participantSocket, 'room:joined');

      // Participant leaves, owner should be notified
      const userLeftPromise = waitForSocketEvent(ownerSocket, 'room:user-left');
      participantSocket.emit('room:leave', { roomId: room.id });

      const leftData = await userLeftPromise;
      expect(leftData.userId).toBe(participant.id);

      cleanupSockets(ownerSocket, participantSocket);
    });

    it('should handle disconnect by removing from all rooms', async () => {
      const { user, token } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Disconnect Test Room',
        type: 'public'
      });

      // Track server socket
      let serverSocket: any;
      ioServer.once('connection', (s) => {
        serverSocket = s;
      });

      const socket = await createAuthenticatedSocket(serverUrl, token);
      await connectAndWait(socket); // This now waits for auth:success

      // Join room
      socket.emit('room:join', { roomId: room.id });
      await waitForSocketEvent(socket, 'room:joined');

      // Check socket has room data
      expect(serverSocket.data.currentRoomId).toBe(room.id);

      // Disconnect
      socket.disconnect();
      await new Promise(resolve => setTimeout(resolve, 100));

      // Check presence was removed
      const participants = presenceService.getRoomParticipants(room.id);
      expect(participants.find(p => p.userId === user.id)).toBeUndefined();
    });
  });

  describe('Presence Updates', () => {
    it('should update presence and broadcast to room', async () => {
      const { user: user1, token: token1 } = await createTestUser();
      const { user: user2, token: token2 } = await createTestUser();
      
      const room = await roomService.createRoom(user1.id, {
        name: 'Presence Test Room',
        type: 'public'
      });
      await roomService.joinRoom(user2.id, room.id);

      // Connect both users
      const socket1 = await createAuthenticatedSocket(serverUrl, token1);
      const socket2 = await createAuthenticatedSocket(serverUrl, token2);
      
      await connectAndWait(socket1);
      await connectAndWait(socket2);

      // Join room
      socket1.emit('room:join', { roomId: room.id });
      socket2.emit('room:join', { roomId: room.id });
      await waitForSocketEvent(socket1, 'room:joined');
      await waitForSocketEvent(socket2, 'room:joined');

      // User1 updates presence, User2 should receive it
      const presencePromise = waitForSocketEvent(socket2, 'presence:updated');
      socket1.emit('presence:update', { 
        status: 'focusing',
        currentTask: 'Writing tests'
      });

      const presenceData = await presencePromise;
      expect(presenceData.userId).toBe(user1.id);
      expect(presenceData.status.status).toBe('focusing');
      expect(presenceData.status.currentTask).toBe('Writing tests');

      cleanupSockets(socket1, socket2);
    });

    it('should not receive events from other rooms', async () => {
      const { user: user1, token: token1 } = await createTestUser();
      const { user: user2, token: token2 } = await createTestUser();
      
      const room1 = await roomService.createRoom(user1.id, {
        name: 'Room 1',
        type: 'public'
      });
      const room2 = await roomService.createRoom(user2.id, {
        name: 'Room 2',
        type: 'public'
      });

      // Connect users
      const socket1 = await createAuthenticatedSocket(serverUrl, token1);
      const socket2 = await createAuthenticatedSocket(serverUrl, token2);
      
      await connectAndWait(socket1);
      await connectAndWait(socket2);

      // Join different rooms
      socket1.emit('room:join', { roomId: room1.id });
      socket2.emit('room:join', { roomId: room2.id });
      await waitForSocketEvent(socket1, 'room:joined');
      await waitForSocketEvent(socket2, 'room:joined');

      // Set up listener for presence update (should not fire)
      let receivedUpdate = false;
      socket2.on('presence:updated', () => {
        receivedUpdate = true;
      });

      // User1 updates presence in room1
      socket1.emit('presence:update', { status: 'focusing' });

      // Wait a bit to ensure no event is received
      await new Promise(resolve => setTimeout(resolve, 200));
      expect(receivedUpdate).toBe(false);

      cleanupSockets(socket1, socket2);
    });

    it('should sync participants list on join', async () => {
      const { user: owner, token: ownerToken } = await createTestUser();
      const { user: participant, token: participantToken } = await createTestUser();
      
      const room = await roomService.createRoom(owner.id, {
        name: 'Sync Test Room',
        type: 'public'
      });
      await roomService.joinRoom(participant.id, room.id);

      // Owner connects first
      const ownerSocket = await createAuthenticatedSocket(serverUrl, ownerToken);
      await connectAndWait(ownerSocket);
      ownerSocket.emit('room:join', { roomId: room.id });
      await waitForSocketEvent(ownerSocket, 'room:joined');

      // Update owner's presence
      ownerSocket.emit('presence:update', { 
        status: 'focusing',
        currentTask: 'Planning'
      });

      // Participant connects and should receive current participants
      const participantSocket = await createAuthenticatedSocket(serverUrl, participantToken);
      await connectAndWait(participantSocket);
      
      const joinedPromise = waitForSocketEvent(participantSocket, 'room:joined');
      participantSocket.emit('room:join', { roomId: room.id });

      const joinedData = await joinedPromise;
      expect(joinedData.participants).toBeDefined();
      expect(joinedData.participants.length).toBe(2);
      
      const ownerPresence = joinedData.participants.find((p: any) => p.userId === owner.id);
      expect(ownerPresence).toBeDefined();
      expect(ownerPresence.status).toBe('focusing');
      expect(ownerPresence.currentTask).toBe('Planning');

      cleanupSockets(ownerSocket, participantSocket);
    });
  });
});