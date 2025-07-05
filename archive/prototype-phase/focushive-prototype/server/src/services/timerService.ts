import { TimerState, TimerPhase, TimerStatus, TimerConfig } from '@focushive/shared';
import { dataStore } from '../data/store';

export class TimerService {
  private static instance: TimerService;
  private timers: Map<string, TimerState> = new Map();
  private intervals: Map<string, NodeJS.Timeout> = new Map();
  
  private config: TimerConfig = {
    workDuration: 25 * 60, // 25 minutes in seconds
    shortBreakDuration: 5 * 60, // 5 minutes
    longBreakDuration: 15 * 60, // 15 minutes
    sessionsUntilLongBreak: 4
  };

  constructor() {
    if (TimerService.instance) {
      return TimerService.instance;
    }
    TimerService.instance = this;
  }

  getOrCreateTimer(roomId: string): TimerState {
    if (!this.timers.has(roomId)) {
      const timer: TimerState = {
        roomId,
        phase: 'work',
        status: 'idle',
        duration: this.config.workDuration,
        remaining: this.config.workDuration,
        pausedDuration: 0,
        sessionCount: 0
      };
      this.timers.set(roomId, timer);
    }
    return this.timers.get(roomId)!;
  }

  getTimer(roomId: string): TimerState | undefined {
    return this.timers.get(roomId);
  }

  startTimer(roomId: string, userId: string): TimerState {
    this.validateUserInRoom(roomId, userId);
    
    const timer = this.getOrCreateTimer(roomId);
    
    if (timer.status === 'running') {
      return timer;
    }

    // Store previous status before changing
    const wasPaused = timer.status === 'paused';
    
    timer.status = 'running';
    timer.startedBy = userId;
    timer.startedAt = Date.now();
    
    // If not resuming from pause, reset remaining time
    if (!wasPaused) {
      timer.remaining = timer.duration;
    }

    return timer;
  }

  pauseTimer(roomId: string, userId: string): TimerState {
    this.validateUserInRoom(roomId, userId);
    
    const timer = this.getTimer(roomId);
    if (!timer) {
      throw new Error('Timer not found');
    }

    if (timer.status !== 'running') {
      throw new Error('Timer is not running');
    }

    timer.status = 'paused';
    timer.pausedAt = Date.now();
    
    // Calculate and update remaining time
    const elapsed = this.getElapsedTime(timer);
    timer.remaining = Math.max(0, timer.duration - elapsed);

    return timer;
  }

  resumeTimer(roomId: string, userId: string): TimerState {
    this.validateUserInRoom(roomId, userId);
    
    const timer = this.getTimer(roomId);
    if (!timer) {
      throw new Error('Timer not found');
    }

    if (timer.status !== 'paused') {
      throw new Error('Timer is not paused');
    }

    // Calculate paused duration
    if (timer.pausedAt) {
      timer.pausedDuration += Date.now() - timer.pausedAt;
    }

    timer.status = 'running';
    timer.pausedAt = undefined;

    return timer;
  }

  resetTimer(roomId: string, userId: string): TimerState {
    this.validateUserInRoom(roomId, userId);
    
    const timer = this.getOrCreateTimer(roomId);
    
    // Clear any running interval
    this.clearInterval(roomId);

    // Reset to initial state
    timer.status = 'idle';
    timer.phase = 'work';
    timer.duration = this.config.workDuration;
    timer.remaining = this.config.workDuration;
    timer.sessionCount = 0;
    timer.startedAt = undefined;
    timer.pausedAt = undefined;
    timer.pausedDuration = 0;
    timer.startedBy = undefined;

    return timer;
  }

  nextPhase(roomId: string, userId: string): TimerState {
    this.validateUserInRoom(roomId, userId);
    
    const timer = this.getOrCreateTimer(roomId);
    
    // Determine next phase
    if (timer.phase === 'work') {
      // Increment session count after completing work
      timer.sessionCount++;
      
      // Check if it's time for a long break
      if (timer.sessionCount >= this.config.sessionsUntilLongBreak) {
        timer.phase = 'longBreak';
        timer.duration = this.config.longBreakDuration;
        timer.sessionCount = 0; // Reset after long break
      } else {
        timer.phase = 'shortBreak';
        timer.duration = this.config.shortBreakDuration;
      }
    } else {
      // After any break, go back to work
      timer.phase = 'work';
      timer.duration = this.config.workDuration;
    }

    // Reset timer state for new phase
    timer.status = 'idle';
    timer.remaining = timer.duration;
    timer.startedAt = undefined;
    timer.pausedAt = undefined;
    timer.pausedDuration = 0;

    return timer;
  }

  skipToNextPhase(roomId: string, userId: string): TimerState {
    this.validateUserInRoom(roomId, userId);
    
    // Clear any running interval
    this.clearInterval(roomId);
    
    return this.nextPhase(roomId, userId);
  }

  getRemainingTime(roomId: string): number {
    const timer = this.getTimer(roomId);
    if (!timer) {
      return 0;
    }

    if (timer.status === 'idle') {
      return timer.remaining;
    }

    if (timer.status === 'paused') {
      return timer.remaining;
    }

    // Calculate remaining time for running timer
    const elapsed = this.getElapsedTime(timer);
    return Math.max(0, timer.duration - elapsed);
  }

  getTimerState(roomId: string): TimerState | null {
    const timer = this.getTimer(roomId);
    if (!timer) {
      return null;
    }

    // Update remaining time for running timers
    if (timer.status === 'running') {
      timer.remaining = this.getRemainingTime(roomId);
    }

    return { ...timer };
  }

  removeTimer(roomId: string): void {
    this.clearInterval(roomId);
    this.timers.delete(roomId);
  }

  cleanup(): void {
    // Clear all intervals
    this.intervals.forEach((interval) => clearInterval(interval));
    this.intervals.clear();
  }

  // Test helper method to set phase directly
  setPhaseForTesting(roomId: string, phase: TimerPhase): void {
    const timer = this.getOrCreateTimer(roomId);
    timer.phase = phase;
    // Set appropriate duration based on phase
    if (phase === 'work') {
      timer.duration = this.config.workDuration;
    } else if (phase === 'shortBreak') {
      timer.duration = this.config.shortBreakDuration;
    } else if (phase === 'longBreak') {
      timer.duration = this.config.longBreakDuration;
    }
    timer.remaining = timer.duration;
  }

  private validateUserInRoom(roomId: string, userId: string): void {
    const room = dataStore.getRoom(roomId);
    if (!room) {
      throw new Error('Room not found');
    }

    if (!room.participants.includes(userId)) {
      throw new Error('User is not a participant of this room');
    }
  }

  private getElapsedTime(timer: TimerState): number {
    if (!timer.startedAt) {
      return 0;
    }

    const now = Date.now();
    const totalElapsed = now - timer.startedAt;
    const actualRunTime = totalElapsed - timer.pausedDuration;
    
    return Math.floor(actualRunTime / 1000); // Convert to seconds
  }

  private clearInterval(roomId: string): void {
    const interval = this.intervals.get(roomId);
    if (interval) {
      clearInterval(interval);
      this.intervals.delete(roomId);
    }
  }
}

// Export singleton instance
export const timerService = new TimerService();