/**
 * Simulates socket connections for dummy users
 * This allows dummy users to appear in rooms properly
 */

import { io as ioClient, Socket } from 'socket.io-client';
import { dataStore } from '../data/store';
import { presenceService } from '../services/presenceService';
import type { ParticipantStatus, Room } from '@focushive/shared';

interface DummySocket {
  userId: string;
  socket: Socket;
  roomId?: string;
}

export class SocketSimulator {
  private static instance: SocketSimulator;
  private dummySockets: Map<string, DummySocket> = new Map();
  private serverUrl: string = 'http://localhost:3000';
  
  private constructor() {}

  static getInstance(): SocketSimulator {
    if (!SocketSimulator.instance) {
      SocketSimulator.instance = new SocketSimulator();
    }
    return SocketSimulator.instance;
  }

  /**
   * Simulate dummy users joining rooms with proper presence
   */
  async simulateDummyConnections(): Promise<void> {
    console.log('🔌 Simulating socket connections for dummy users...');
    
    // Get all dummy rooms (both old and new format)
    const allRooms = dataStore.getAllRooms();
    const dummyRooms = allRooms.filter(room => 
      room.id.startsWith('dummy-room-') || 
      room.id.startsWith('dummy_room_') ||
      room.name.includes('🎯') || 
      room.name.includes('📚') ||
      room.name.includes('💻') ||
      room.name.includes('🎨')
    );
    const dummyUsers = dataStore.getAllUsers().filter(user => user.id.startsWith('dummy'));
    
    if (dummyRooms.length === 0 || dummyUsers.length === 0) {
      console.log('⚠️ No dummy rooms or users found');
      return;
    }

    console.log(`📊 Found ${dummyRooms.length} dummy rooms and ${dummyUsers.length} dummy users`);

    // For each room, simulate participants joining
    for (const room of dummyRooms) {
      await this.populateRoom(room);
    }
    
    // Log room populations
    dummyRooms.forEach(room => {
      const participants = presenceService.getRoomParticipants(room.id);
      console.log(`📍 Room "${room.name}" has ${participants.length} participants`);
    });
    
    console.log('✅ Socket connections simulated');
  }

  private async populateRoom(room: Room): Promise<void> {
    // Get target participant count from room config
    const targetCount = this.getTargetParticipantCount(room.name);
    
    // Clear existing participants except the owner
    room.participants = [room.ownerId];
    
    // Get available dummy users (both formats)
    const allDummyUsers = dataStore.getAllUsers().filter(user => 
      user.id.startsWith('dummy-') || user.id.startsWith('dummy_')
    );
    const availableUsers = allDummyUsers.filter(user => user.id !== room.ownerId);
    
    // Shuffle available users
    const shuffled = [...availableUsers].sort(() => 0.5 - Math.random());
    
    // Add participants up to target count (minus 1 for owner)
    const participantsToAdd = Math.min(targetCount - 1, shuffled.length);
    
    console.log(`🏠 Populating "${room.name}" with ${participantsToAdd} participants`);
    
    for (let i = 0; i < participantsToAdd; i++) {
      const user = shuffled[i];
      if (user) {
        // Add to room participants
        room.participants.push(user.id);
        
        // Add presence with appropriate status
        const status = this.getRandomStatus();
        const task = status === 'focusing' ? this.getRandomTask(room.focusType) : '';
        
        // Add presence data
        const presence = presenceService.addPresence(room.id, user.id);
        presenceService.updatePresence(room.id, user.id, {
          status,
          currentTask: task
        });
        
        console.log(`  👤 Added ${user.username} (${status}${task ? ': ' + task : ''})`);
      }
    }
    
    // Update room in dataStore
    dataStore.updateRoom(room.id, { participants: room.participants });
  }

  private getTargetParticipantCount(roomName: string): number {
    const roomConfigs: { [key: string]: number } = {
      '🎯 Deep Work Zone': 8,
      '📚 Study Hall': 6,
      '🎨 Creative Space': 4,
      '☀️ Morning Focus': 5,
      '🌙 Night Owls': 7,
      '📝 Thesis Writing': 3,
      '💻 Web Dev Hub': 9,
      '🧮 Data Science Lab': 5
    };
    
    return roomConfigs[roomName] || 4;
  }

  private getRandomStatus(): ParticipantStatus['status'] {
    const statuses: ParticipantStatus['status'][] = ['focusing', 'focusing', 'focusing', 'break', 'idle'];
    return statuses[Math.floor(Math.random() * statuses.length)];
  }

  private getRandomTask(focusType: string): string {
    const tasks: { [key: string]: string[] } = {
      deepWork: [
        'API integration',
        'Code refactoring',
        'System design',
        'Performance optimization',
        'Database queries'
      ],
      study: [
        'Chapter review',
        'Practice problems',
        'Essay writing',
        'Research reading',
        'Note taking'
      ],
      creative: [
        'Design concepts',
        'Content creation',
        'Brainstorming',
        'Sketch ideas',
        'Color theory'
      ],
      other: [
        'Documentation',
        'Planning',
        'Email management',
        'Review tasks',
        'Team sync'
      ]
    };
    
    const taskList = tasks[focusType] || tasks.other;
    return taskList[Math.floor(Math.random() * taskList.length)];
  }

  private isUserInAnyRoom(userId: string): boolean {
    const rooms = dataStore.getAllRooms();
    return rooms.some(room => room.participants.includes(userId));
  }

  /**
   * Start activity simulation
   */
  startActivitySimulation(): void {
    // Update presence statuses periodically
    setInterval(() => {
      this.updateRandomPresences();
    }, 30000); // Every 30 seconds
    
    // Change tasks periodically
    setInterval(() => {
      this.updateRandomTasks();
    }, 120000); // Every 2 minutes
  }

  private updateRandomPresences(): void {
    const rooms = dataStore.getAllRooms().filter(room => room.id.startsWith('dummy-room-'));
    
    rooms.forEach(room => {
      // Randomly update 1-2 participants per room
      const participantsToUpdate = Math.min(2, room.participants.length);
      
      for (let i = 0; i < participantsToUpdate; i++) {
        const participant = room.participants[Math.floor(Math.random() * room.participants.length)];
        if (participant && (participant.startsWith('dummy-') || participant.startsWith('dummy_'))) {
          const newStatus = this.getRandomStatus();
          const task = newStatus === 'focusing' ? this.getRandomTask(room.focusType) : '';
          
          presenceService.updatePresence(room.id, participant, {
            status: newStatus,
            currentTask: task
          });
        }
      }
    });
  }

  private updateRandomTasks(): void {
    const rooms = dataStore.getAllRooms().filter(room => room.id.startsWith('dummy-room-'));
    
    rooms.forEach(room => {
      const focusingParticipants = room.participants.filter(userId => {
        const presence = presenceService.getPresence(room.id, userId);
        return presence && presence.status === 'focusing' && (userId.startsWith('dummy-') || userId.startsWith('dummy_'));
      });
      
      if (focusingParticipants.length > 0) {
        // Update task for one random focusing participant
        const participant = focusingParticipants[Math.floor(Math.random() * focusingParticipants.length)];
        const newTask = this.getRandomTask(room.focusType);
        
        presenceService.updatePresence(room.id, participant, {
          status: 'focusing',
          currentTask: newTask
        });
      }
    });
  }

  cleanup(): void {
    // Close all dummy sockets
    this.dummySockets.forEach(({ socket }) => {
      socket.disconnect();
    });
    this.dummySockets.clear();
  }
}

export const socketSimulator = SocketSimulator.getInstance();