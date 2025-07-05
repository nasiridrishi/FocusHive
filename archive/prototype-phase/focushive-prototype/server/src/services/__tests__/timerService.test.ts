import { TimerService } from '../timerService';
import { dataStore } from '../../data/store';
import { RoomService } from '../roomService';
import { createTestUser, createTestRoom } from '../../__tests__/utils/testHelpers';

describe('TimerService', () => {
  let timerService: TimerService;
  let roomService: RoomService;

  beforeEach(() => {
    dataStore.clear();
    timerService = new TimerService();
    roomService = new RoomService();
  });

  afterEach(() => {
    // Clean up any running timers
    timerService.cleanup();
  });

  describe('Timer Creation and Initialization', () => {
    it('should create a timer for a room', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const timer = timerService.getOrCreateTimer(room.id);
      
      expect(timer).toBeDefined();
      expect(timer.roomId).toBe(room.id);
      expect(timer.phase).toBe('work');
      expect(timer.status).toBe('idle');
      expect(timer.duration).toBe(25 * 60); // 25 minutes in seconds
      expect(timer.remaining).toBe(25 * 60);
      expect(timer.sessionCount).toBe(0);
      expect(timer.pausedDuration).toBe(0);
    });

    it('should return existing timer for room', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const timer1 = timerService.getOrCreateTimer(room.id);
      const timer2 = timerService.getOrCreateTimer(room.id);
      
      expect(timer1).toBe(timer2); // Same reference
    });

    it('should get timer by room ID', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      timerService.getOrCreateTimer(room.id);
      const timer = timerService.getTimer(room.id);
      
      expect(timer).toBeDefined();
      expect(timer?.roomId).toBe(room.id);
    });

    it('should return undefined for non-existent timer', () => {
      const timer = timerService.getTimer('non-existent-room');
      expect(timer).toBeUndefined();
    });
  });

  describe('Timer Start Operation', () => {
    it('should start timer from idle state', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const timer = timerService.startTimer(room.id, user.id);
      
      expect(timer.status).toBe('running');
      expect(timer.startedBy).toBe(user.id);
      expect(timer.startedAt).toBeDefined();
      expect(timer.startedAt).toBeLessThanOrEqual(Date.now());
    });

    it('should not start timer if already running', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      timerService.startTimer(room.id, user.id);
      const startedAt = timerService.getTimer(room.id)?.startedAt;
      
      // Try to start again
      timerService.startTimer(room.id, user.id);
      const timer = timerService.getTimer(room.id);
      
      expect(timer?.startedAt).toBe(startedAt); // Should not change
    });

    it('should validate user is room participant', async () => {
      const { user: owner } = await createTestUser();
      const { user: nonParticipant } = await createTestUser();
      const room = await roomService.createRoom(owner.id, {
        name: 'Test Room',
        type: 'public'
      });

      expect(() => {
        timerService.startTimer(room.id, nonParticipant.id);
      }).toThrow('User is not a participant of this room');
    });
  });

  describe('Timer Pause/Resume Operations', () => {
    it('should pause running timer', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      timerService.startTimer(room.id, user.id);
      const timer = timerService.pauseTimer(room.id, user.id);
      
      expect(timer.status).toBe('paused');
      expect(timer.pausedAt).toBeDefined();
    });

    it('should not pause timer if not running', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const timer = timerService.getOrCreateTimer(room.id);
      
      expect(() => {
        timerService.pauseTimer(room.id, user.id);
      }).toThrow('Timer is not running');
    });

    it('should resume paused timer', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      timerService.startTimer(room.id, user.id);
      timerService.pauseTimer(room.id, user.id);
      
      // Wait a bit to accumulate paused duration
      await new Promise(resolve => setTimeout(resolve, 100));
      
      const timer = timerService.resumeTimer(room.id, user.id);
      
      expect(timer.status).toBe('running');
      expect(timer.pausedAt).toBeUndefined();
      expect(timer.pausedDuration).toBeGreaterThan(0);
    });

    it('should not resume timer if not paused', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      timerService.startTimer(room.id, user.id);
      
      expect(() => {
        timerService.resumeTimer(room.id, user.id);
      }).toThrow('Timer is not paused');
    });
  });

  describe('Timer Reset Operation', () => {
    it('should reset timer to initial state', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      timerService.startTimer(room.id, user.id);
      const timer = timerService.resetTimer(room.id, user.id);
      
      expect(timer.status).toBe('idle');
      expect(timer.phase).toBe('work');
      expect(timer.remaining).toBe(25 * 60);
      expect(timer.sessionCount).toBe(0);
      expect(timer.startedAt).toBeUndefined();
      expect(timer.pausedAt).toBeUndefined();
      expect(timer.pausedDuration).toBe(0);
    });

    it('should reset timer from any state', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      // Start and pause timer
      timerService.startTimer(room.id, user.id);
      timerService.pauseTimer(room.id, user.id);
      
      const timer = timerService.resetTimer(room.id, user.id);
      
      expect(timer.status).toBe('idle');
    });
  });

  describe('Time Calculation', () => {
    it('should calculate remaining time correctly', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const timer = timerService.startTimer(room.id, user.id);
      const startTime = timer.startedAt!;
      
      // Mock time passing
      const mockNow = startTime + 5000; // 5 seconds later
      jest.spyOn(Date, 'now').mockReturnValue(mockNow);
      
      const remaining = timerService.getRemainingTime(room.id);
      
      expect(remaining).toBe(25 * 60 - 5); // 25 minutes - 5 seconds
      
      jest.restoreAllMocks();
    });

    it('should account for paused duration', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      timerService.startTimer(room.id, user.id);
      timerService.pauseTimer(room.id, user.id);
      
      // Set paused duration
      const timer = timerService.getTimer(room.id)!;
      timer.pausedDuration = 10000; // 10 seconds paused
      
      timerService.resumeTimer(room.id, user.id);
      
      const startTime = timer.startedAt!;
      const mockNow = startTime + 15000; // 15 seconds total elapsed
      jest.spyOn(Date, 'now').mockReturnValue(mockNow);
      
      const remaining = timerService.getRemainingTime(room.id);
      
      // Should only count 5 seconds of actual running time
      expect(remaining).toBe(25 * 60 - 5);
      
      jest.restoreAllMocks();
    });

    it('should return 0 when time is up', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const timer = timerService.startTimer(room.id, user.id);
      const startTime = timer.startedAt!;
      
      // Mock time passing beyond duration
      const mockNow = startTime + (26 * 60 * 1000); // 26 minutes later
      jest.spyOn(Date, 'now').mockReturnValue(mockNow);
      
      const remaining = timerService.getRemainingTime(room.id);
      
      expect(remaining).toBe(0);
      
      jest.restoreAllMocks();
    });
  });

  describe('Phase Transitions', () => {
    it('should transition from work to short break', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const timer = timerService.getOrCreateTimer(room.id);
      timer.sessionCount = 1; // Not time for long break yet
      
      const nextTimer = timerService.nextPhase(room.id, user.id);
      
      expect(nextTimer.phase).toBe('shortBreak');
      expect(nextTimer.duration).toBe(5 * 60); // 5 minutes
      expect(nextTimer.remaining).toBe(5 * 60);
      expect(nextTimer.status).toBe('idle');
    });

    it('should transition to long break after 4 work sessions', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const timer = timerService.getOrCreateTimer(room.id);
      timer.sessionCount = 4; // Time for long break
      
      const nextTimer = timerService.nextPhase(room.id, user.id);
      
      expect(nextTimer.phase).toBe('longBreak');
      expect(nextTimer.duration).toBe(15 * 60); // 15 minutes
      expect(nextTimer.sessionCount).toBe(0); // Reset after long break
    });

    it('should transition from break back to work', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const timer = timerService.getOrCreateTimer(room.id);
      timer.phase = 'shortBreak';
      
      const nextTimer = timerService.nextPhase(room.id, user.id);
      
      expect(nextTimer.phase).toBe('work');
      expect(nextTimer.duration).toBe(25 * 60);
    });

    it('should increment session count after work phase', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const timer = timerService.getOrCreateTimer(room.id);
      timer.phase = 'work';
      timer.sessionCount = 2;
      
      const nextTimer = timerService.nextPhase(room.id, user.id);
      
      expect(nextTimer.sessionCount).toBe(3);
    });
  });

  describe('Skip Functionality', () => {
    it('should skip to next phase', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      timerService.startTimer(room.id, user.id);
      const timer = timerService.skipToNextPhase(room.id, user.id);
      
      expect(timer.phase).toBe('shortBreak');
      expect(timer.status).toBe('idle'); // Reset to idle after skip
    });

    it('should handle skip during different phases', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      const timer = timerService.getOrCreateTimer(room.id);
      
      // Skip through all phases
      timerService.skipToNextPhase(room.id, user.id); // work -> short break
      expect(timer.phase).toBe('shortBreak');
      
      timerService.skipToNextPhase(room.id, user.id); // short break -> work
      expect(timer.phase).toBe('work');
      expect(timer.sessionCount).toBe(1);
    });
  });

  describe('Multiple Room Support', () => {
    it('should manage timers for multiple rooms independently', async () => {
      const { user } = await createTestUser();
      const room1 = await roomService.createRoom(user.id, {
        name: 'Room 1',
        type: 'public'
      });
      const room2 = await roomService.createRoom(user.id, {
        name: 'Room 2',
        type: 'public'
      });

      timerService.startTimer(room1.id, user.id);
      const timer1 = timerService.getTimer(room1.id);
      const timer2 = timerService.getOrCreateTimer(room2.id);
      
      expect(timer1?.status).toBe('running');
      expect(timer2.status).toBe('idle');
    });

    it('should clean up timer when room is deleted', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      timerService.getOrCreateTimer(room.id);
      timerService.removeTimer(room.id);
      
      const timer = timerService.getTimer(room.id);
      expect(timer).toBeUndefined();
    });
  });

  describe('Timer State Serialization', () => {
    it('should serialize timer state for clients', async () => {
      const { user } = await createTestUser();
      const room = await roomService.createRoom(user.id, {
        name: 'Test Room',
        type: 'public'
      });

      timerService.startTimer(room.id, user.id);
      const state = timerService.getTimerState(room.id);
      
      expect(state).not.toBeNull();
      expect(state).toMatchObject({
        roomId: room.id,
        phase: 'work',
        status: 'running',
        duration: 25 * 60,
        sessionCount: 0,
        startedBy: user.id
      });
      expect(state!.remaining).toBeDefined();
      expect(state!.remaining).toBeLessThanOrEqual(25 * 60);
    });

    it('should return null state for non-existent timer', () => {
      const state = timerService.getTimerState('non-existent');
      expect(state).toBeNull();
    });
  });

  describe('Error Handling', () => {
    it('should throw error for non-existent room', () => {
      expect(() => {
        timerService.startTimer('non-existent', 'user-id');
      }).toThrow('Room not found');
    });

    it('should throw error when user not in room', async () => {
      const { user: owner } = await createTestUser();
      const { user: other } = await createTestUser();
      const room = await roomService.createRoom(owner.id, {
        name: 'Test Room',
        type: 'private',
        password: 'secret'
      });

      expect(() => {
        timerService.startTimer(room.id, other.id);
      }).toThrow('User is not a participant of this room');
    });
  });
});