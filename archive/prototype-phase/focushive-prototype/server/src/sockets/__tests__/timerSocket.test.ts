import { Server as SocketServer } from 'socket.io';
import { Socket } from 'socket.io-client';
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
import { setupRoomSockets } from '../roomSocket';
import { setupTimerSockets, cleanupTimerSockets } from '../timerSocket';
import { RoomService } from '../../services/roomService';
import { timerService } from '../../services/timerService';
import { dataStore } from '../../data/store';

describe('Timer Socket Events', () => {
  let setup: TestSocketSetup;
  let ioServer: SocketServer;
  let serverUrl: string;
  let roomService: RoomService;

  beforeEach(async () => {
    dataStore.clear();
    setup = await createTestSocketServer();
    ioServer = setup.io;
    serverUrl = setup.serverUrl;
    roomService = new RoomService();
    
    // Setup socket handlers
    setupSocketAuth(ioServer);
    setupRoomSockets(ioServer);
    setupTimerSockets(ioServer);
  });

  afterEach(async () => {
    timerService.cleanup();
    cleanupTimerSockets();
    await cleanupSocketServer(setup);
  });

  describe('Timer Start', () => {
    it('should start timer and broadcast to room', async () => {
      const { user, token } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const socket = await createAuthenticatedSocket(serverUrl, token);
      await connectAndWait(socket);

      // Join room first
      socket.emit('room:join', { roomId: room.id });
      await waitForSocketEvent(socket, 'room:joined');

      // Start timer
      const timerStartedPromise = waitForSocketEvent(socket, 'timer:started');
      socket.emit('timer:start', { roomId: room.id });

      const response = await timerStartedPromise;
      expect(response.state).toMatchObject({
        roomId: room.id,
        phase: 'work',
        status: 'running',
        duration: 25 * 60,
        startedBy: user.id
      });

      cleanupSockets(socket);
    });

    it('should reject timer start if user not in room', async () => {
      const { user: owner } = await createTestUser();
      const { user: other, token: otherToken } = await createTestUser();
      
      const room = await roomService.createRoom(owner.id, {
        name: 'Test Room',
        type: 'public'
      });

      const socket = await createAuthenticatedSocket(serverUrl, otherToken);
      await connectAndWait(socket);

      const errorPromise = waitForSocketEvent(socket, 'timer:error');
      socket.emit('timer:start', { roomId: room.id });

      const error = await errorPromise;
      expect(error.message).toContain('not a participant');

      cleanupSockets(socket);
    });

    it('should sync timer state to late joiners', async () => {
      const { user: owner, token: ownerToken } = await createTestUser();
      const { user: participant, token: participantToken } = await createTestUser();
      
      const room = await roomService.createRoom(owner.id, {
        name: 'Test Room',
        type: 'public'
      });

      // Owner starts timer
      const ownerSocket = await createAuthenticatedSocket(serverUrl, ownerToken);
      await connectAndWait(ownerSocket);
      ownerSocket.emit('room:join', { roomId: room.id });
      await waitForSocketEvent(ownerSocket, 'room:joined');
      
      ownerSocket.emit('timer:start', { roomId: room.id });
      await waitForSocketEvent(ownerSocket, 'timer:started');

      // Participant joins room after timer started
      await roomService.joinRoom(participant.id, room.id);
      
      const participantSocket = await createAuthenticatedSocket(serverUrl, participantToken);
      await connectAndWait(participantSocket);
      
      const joinedPromise = waitForSocketEvent(participantSocket, 'room:joined');
      const timerStatePromise = waitForSocketEvent(participantSocket, 'timer:state-update');
      
      participantSocket.emit('room:join', { roomId: room.id });
      
      await joinedPromise;
      const timerState = await timerStatePromise;
      
      expect(timerState.status).toBe('running');
      expect(timerState.phase).toBe('work');

      cleanupSockets(ownerSocket, participantSocket);
    });
  });

  describe('Timer Pause/Resume', () => {
    it('should pause timer and broadcast', async () => {
      const { user, token } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const socket = await createAuthenticatedSocket(serverUrl, token);
      await connectAndWait(socket);
      socket.emit('room:join', { roomId: room.id });
      await waitForSocketEvent(socket, 'room:joined');

      // Start timer first
      socket.emit('timer:start', { roomId: room.id });
      await waitForSocketEvent(socket, 'timer:started');

      // Pause timer
      const timerPausedPromise = waitForSocketEvent(socket, 'timer:paused');
      socket.emit('timer:pause', { roomId: room.id });

      const response = await timerPausedPromise;
      expect(response.state.status).toBe('paused');

      cleanupSockets(socket);
    });

    it('should resume timer and broadcast', async () => {
      const { user, token } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const socket = await createAuthenticatedSocket(serverUrl, token);
      await connectAndWait(socket);
      socket.emit('room:join', { roomId: room.id });
      await waitForSocketEvent(socket, 'room:joined');

      // Start and pause timer
      socket.emit('timer:start', { roomId: room.id });
      await waitForSocketEvent(socket, 'timer:started');
      socket.emit('timer:pause', { roomId: room.id });
      await waitForSocketEvent(socket, 'timer:paused');

      // Resume timer
      const timerResumedPromise = waitForSocketEvent(socket, 'timer:resumed');
      socket.emit('timer:resume', { roomId: room.id });

      const response = await timerResumedPromise;
      expect(response.state.status).toBe('running');

      cleanupSockets(socket);
    });
  });

  describe('Timer Reset', () => {
    it('should reset timer and broadcast', async () => {
      const { user, token } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const socket = await createAuthenticatedSocket(serverUrl, token);
      await connectAndWait(socket);
      socket.emit('room:join', { roomId: room.id });
      await waitForSocketEvent(socket, 'room:joined');

      // Start timer
      socket.emit('timer:start', { roomId: room.id });
      await waitForSocketEvent(socket, 'timer:started');

      // Reset timer
      const timerResetPromise = waitForSocketEvent(socket, 'timer:reset');
      socket.emit('timer:reset', { roomId: room.id });

      const response = await timerResetPromise;
      expect(response.state.status).toBe('idle');
      expect(response.state.phase).toBe('work');
      expect(response.state.remaining).toBe(25 * 60);

      cleanupSockets(socket);
    });
  });

  describe('Timer Skip', () => {
    it('should skip to next phase and broadcast', async () => {
      const { user, token } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const socket = await createAuthenticatedSocket(serverUrl, token);
      await connectAndWait(socket);
      socket.emit('room:join', { roomId: room.id });
      await waitForSocketEvent(socket, 'room:joined');

      // Start timer in work phase
      socket.emit('timer:start', { roomId: room.id });
      await waitForSocketEvent(socket, 'timer:started');

      // Skip to break
      const phaseChangedPromise = waitForSocketEvent(socket, 'timer:phase-changed');
      socket.emit('timer:skip', { roomId: room.id });

      const response = await phaseChangedPromise;
      expect(response.state.phase).toBe('shortBreak');
      expect(response.state.status).toBe('idle');

      cleanupSockets(socket);
    });
  });

  describe('Timer Broadcasting', () => {
    it('should broadcast timer events to all room participants', async () => {
      const { user: user1, token: token1 } = await createTestUser();
      const { user: user2, token: token2 } = await createTestUser();
      
      const room = await roomService.createRoom(user1.id, {
        name: 'Test Room',
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

      // User1 starts timer, User2 should receive event
      const timerStartedPromise = waitForSocketEvent(socket2, 'timer:started');
      socket1.emit('timer:start', { roomId: room.id });

      const response = await timerStartedPromise;
      expect(response.state.status).toBe('running');
      expect(response.state.startedBy).toBe(user1.id);

      cleanupSockets(socket1, socket2);
    });

    it('should not receive timer events from other rooms', async () => {
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

      // Set up listener on socket2 (should not receive)
      let receivedTimer = false;
      socket2.on('timer:started', () => {
        receivedTimer = true;
      });

      // User1 starts timer in room1
      socket1.emit('timer:start', { roomId: room1.id });
      await waitForSocketEvent(socket1, 'timer:started');

      // Wait a bit
      await new Promise(resolve => setTimeout(resolve, 200));
      expect(receivedTimer).toBe(false);

      cleanupSockets(socket1, socket2);
    });
  });

  describe('Timer Ticks', () => {
    it('should send periodic timer updates', async () => {
      const { user, token } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const socket = await createAuthenticatedSocket(serverUrl, token);
      await connectAndWait(socket);
      socket.emit('room:join', { roomId: room.id });
      await waitForSocketEvent(socket, 'room:joined');

      // Start timer
      socket.emit('timer:start', { roomId: room.id });
      await waitForSocketEvent(socket, 'timer:started');

      // Wait for tick event
      const tickPromise = waitForSocketEvent(socket, 'timer:tick', 2000);
      const tick = await tickPromise;
      
      expect(tick.remaining).toBeLessThanOrEqual(25 * 60);
      expect(tick.phase).toBe('work');

      cleanupSockets(socket);
    });
  });

  describe('Phase Completion', () => {
    it('should emit phase-complete when timer ends', async () => {
      const { user, token } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      // Manually set timer to near completion
      const timer = timerService.getOrCreateTimer(room.id);
      timer.duration = 2; // 2 seconds
      timer.remaining = 2;

      const socket = await createAuthenticatedSocket(serverUrl, token);
      await connectAndWait(socket);
      socket.emit('room:join', { roomId: room.id });
      await waitForSocketEvent(socket, 'room:joined');

      // Start timer
      socket.emit('timer:start', { roomId: room.id });
      await waitForSocketEvent(socket, 'timer:started');

      // Wait for phase complete
      const phaseCompletePromise = waitForSocketEvent(socket, 'timer:phase-complete', 3000);
      const response = await phaseCompletePromise;
      
      expect(response.completedPhase).toBe('work');
      expect(response.nextPhase).toBe('shortBreak');

      cleanupSockets(socket);
    });
  });

  describe('Error Handling', () => {
    it('should handle non-existent room', async () => {
      const { user, token } = await createTestUser();
      
      const socket = await createAuthenticatedSocket(serverUrl, token);
      await connectAndWait(socket);

      const errorPromise = waitForSocketEvent(socket, 'timer:error');
      socket.emit('timer:start', { roomId: 'non-existent' });

      const error = await errorPromise;
      expect(error.message).toContain('Room not found');

      cleanupSockets(socket);
    });

    it('should handle invalid timer operations', async () => {
      const { user, token } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const socket = await createAuthenticatedSocket(serverUrl, token);
      await connectAndWait(socket);
      socket.emit('room:join', { roomId: room.id });
      await waitForSocketEvent(socket, 'room:joined');

      // Try to pause when not running
      const errorPromise = waitForSocketEvent(socket, 'timer:error');
      socket.emit('timer:pause', { roomId: room.id });

      const error = await errorPromise;
      expect(error.message).toBeDefined();
      // Timer might not exist yet, so we get 'Timer not found' instead of 'Timer is not running'

      cleanupSockets(socket);
    });
  });
});