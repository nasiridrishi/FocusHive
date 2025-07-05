/**
 * Enhanced Dummy Data Generator for FocusHive
 * Creates rooms with active participants and simulates real-time activities
 */

import { Room, User, ParticipantStatus } from '@focushive/shared';
import { roomService } from '../services/roomService';
import { socketService } from '../services/socketService';
import { dataStore } from '../data/store';
import bcrypt from 'bcryptjs';
import { v4 as uuidv4 } from 'uuid';

interface DummyRoomConfig {
  name: string;
  focusType: 'deep-work' | 'study' | 'creative' | 'general' | 'writing' | 'coding';
  tags: string[];
  participantCount: number;
  tasks: string[];
  description: string;
}

const DUMMY_ROOMS: DummyRoomConfig[] = [
  { 
    name: '🎯 Deep Work Zone', 
    focusType: 'deep-work', 
    tags: ['productivity', 'coding', 'focus'],
    participantCount: 8,
    tasks: ['Complete API integration', 'Review pull requests', 'Write unit tests', 'Debug authentication', 'Optimize database queries', 'Refactor components'],
    description: 'Silent deep work sessions for maximum productivity'
  },
  { 
    name: '📚 Study Hall', 
    focusType: 'study', 
    tags: ['exam-prep', 'learning', 'students'],
    participantCount: 6,
    tasks: ['Chapter 5 review', 'Practice problems', 'Flashcard review', 'Mock exam prep', 'Essay outline', 'Research notes'],
    description: 'Focused study sessions for students'
  },
  { 
    name: '🎨 Creative Space', 
    focusType: 'creative', 
    tags: ['design', 'writing', 'art'],
    participantCount: 4,
    tasks: ['Logo design concepts', 'Color palette selection', 'Client presentation', 'Portfolio update', 'Blog post draft', 'UI mockups'],
    description: 'Creative work and design sprints'
  },
  { 
    name: '☀️ Morning Focus', 
    focusType: 'general', 
    tags: ['early-bird', 'productivity'],
    participantCount: 5,
    tasks: ['Email inbox zero', 'Daily planning', 'Team standup prep', 'Documentation', 'Code review', 'Task prioritization'],
    description: 'Early morning productivity sessions'
  },
  { 
    name: '🌙 Night Owls', 
    focusType: 'general', 
    tags: ['late-night', 'coding'],
    participantCount: 7,
    tasks: ['Feature implementation', 'Code refactoring', 'Database work', 'Deploy to staging', 'Bug fixes', 'Integration testing'],
    description: 'Late night coding and focus sessions'
  },
  { 
    name: '📝 Thesis Writing', 
    focusType: 'study', 
    tags: ['academic', 'research'],
    participantCount: 3,
    tasks: ['Literature review', 'Data analysis', 'Chapter 3 draft', 'Citations formatting', 'Methodology section', 'Results interpretation'],
    description: 'Dedicated space for thesis and research work'
  },
  { 
    name: '💻 Web Dev Hub', 
    focusType: 'deep-work', 
    tags: ['javascript', 'react', 'coding'],
    participantCount: 9,
    tasks: ['React components', 'API endpoints', 'CSS styling', 'Performance optimization', 'Unit tests', 'Documentation'],
    description: 'Web development focused sessions'
  },
  { 
    name: '🧮 Data Science Lab', 
    focusType: 'study', 
    tags: ['python', 'machine-learning'],
    participantCount: 5,
    tasks: ['Data preprocessing', 'Model training', 'Feature engineering', 'Results visualization', 'Jupyter notebooks', 'Algorithm optimization'],
    description: 'Data science and ML work sessions'
  }
];

const DUMMY_USERS = [
  // Students
  { name: 'Emma Chen', field: 'Computer Science', type: 'student' },
  { name: 'James Wilson', field: 'Medicine', type: 'student' },
  { name: 'Priya Patel', field: 'Engineering', type: 'student' },
  { name: 'Lucas Brown', field: 'Mathematics', type: 'student' },
  { name: 'Sofia Rodriguez', field: 'Psychology', type: 'student' },
  { name: 'Ahmed Hassan', field: 'Physics', type: 'student' },
  { name: 'Maria Garcia', field: 'Literature', type: 'student' },
  { name: 'David Kim', field: 'Business', type: 'student' },
  
  // Remote Workers
  { name: 'Sarah Johnson', field: 'Software Developer', type: 'worker' },
  { name: 'Michael Zhang', field: 'UX Designer', type: 'worker' },
  { name: 'Alex Thompson', field: 'Content Writer', type: 'worker' },
  { name: 'Olivia Davis', field: 'Data Analyst', type: 'worker' },
  { name: 'Ryan Mitchell', field: 'Product Manager', type: 'worker' },
  { name: 'Nina Kowalski', field: 'Graphic Designer', type: 'worker' },
  { name: 'Carlos Mendez', field: 'Consultant', type: 'worker' },
  { name: 'Lisa Anderson', field: 'Project Manager', type: 'worker' }
];

export class EnhancedDummyDataGenerator {
  private static instance: EnhancedDummyDataGenerator;
  private dummyUsers: Map<string, User> = new Map();
  private dummyRooms: Map<string, Room> = new Map();
  private intervalIds: NodeJS.Timeout[] = [];

  private constructor() {}

  static getInstance(): EnhancedDummyDataGenerator {
    if (!EnhancedDummyDataGenerator.instance) {
      EnhancedDummyDataGenerator.instance = new EnhancedDummyDataGenerator();
    }
    return EnhancedDummyDataGenerator.instance;
  }

  async initialize(): Promise<void> {
    console.log('🎭 Initializing enhanced dummy data...');
    
    // Create dummy users
    await this.createDummyUsers();
    
    // Create dummy rooms with participants
    await this.createDummyRooms();
    
    // Start activity simulation
    this.startActivitySimulation();
    
    console.log('✅ Enhanced dummy data initialized');
  }

  private async createDummyUsers(): Promise<void> {
    const passwordHash = await bcrypt.hash('demo123', 10);
    
    for (const userData of DUMMY_USERS) {
      const username = userData.name.toLowerCase().replace(' ', '.');
      const email = `${username}@${userData.type === 'student' ? 'university.edu' : 'company.com'}`;
      
      const user: User = {
        id: `dummy-${username}`,
        email,
        username: userData.name,
        password: passwordHash,
        avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(userData.name)}&background=random`,
        lookingForBuddy: true,
        totalFocusTime: Math.floor(Math.random() * 3000) + 600, // 600-3600 minutes (10-60 hours)
        currentStreak: Math.floor(Math.random() * 20) + 1, // 1-20 days
        longestStreak: Math.floor(Math.random() * 30) + 10, // 10-40 days
        points: Math.floor(Math.random() * 3000) + 500, // 500-3500 points
        lastActiveDate: new Date().toISOString(),
        preferences: {
          darkMode: Math.random() > 0.5,
          soundEnabled: true,
          defaultPomodoro: {
            focusDuration: 25,
            breakDuration: 5
          }
        },
        createdAt: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString(),
        updatedAt: new Date().toISOString()
      };
      
      const createdUser = dataStore.createUser(user);
      this.dummyUsers.set(createdUser.id, createdUser);
    }
  }

  private async createDummyRooms(): Promise<void> {
    const userIds = Array.from(this.dummyUsers.keys());
    
    for (const roomConfig of DUMMY_ROOMS) {
      // Select random creator
      const creatorId = userIds[Math.floor(Math.random() * userIds.length)];
      
      // Create room directly
      const room: Room = {
        id: `dummy-room-${uuidv4()}`,
        name: roomConfig.name,
        description: roomConfig.description,
        ownerId: creatorId,
        type: 'public',
        isPublic: true,
        focusType: roomConfig.focusType === 'deep-work' ? 'deepWork' : roomConfig.focusType === 'coding' ? 'other' : roomConfig.focusType === 'writing' ? 'creative' : roomConfig.focusType === 'general' ? 'other' : roomConfig.focusType,
        tags: roomConfig.tags,
        maxParticipants: 12,
        participants: [creatorId],
        pomodoroSettings: {
          focusDuration: 25,
          shortBreakDuration: 5,
          longBreakDuration: 15,
          sessionsUntilLongBreak: 4
        },
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
      
      const createdRoom = dataStore.createRoom(room);
      
      this.dummyRooms.set(room.id, room);
      
      // Add participants to room
      await this.populateRoom(room.id, roomConfig.participantCount, roomConfig.tasks);
    }
  }

  private async populateRoom(roomId: string, targetCount: number, tasks: string[]): Promise<void> {
    const room = this.dummyRooms.get(roomId);
    if (!room) return;
    
    const allUserIds = Array.from(this.dummyUsers.keys());
    const availableUsers = allUserIds.filter(id => !room.participants.includes(id));
    
    // Add random participants (excluding creator who's already in)
    const participantsToAdd = Math.min(targetCount - 1, availableUsers.length);
    
    for (let i = 0; i < participantsToAdd; i++) {
      const randomIndex = Math.floor(Math.random() * availableUsers.length);
      const userId = availableUsers[randomIndex];
      availableUsers.splice(randomIndex, 1);
      
      try {
        // Add participant directly
        room.participants.push(userId);
        
        // Set participant status through socket service
        const statuses: ParticipantStatus['status'][] = ['focusing', 'focusing', 'focusing', 'break', 'idle'];
        const status = statuses[Math.floor(Math.random() * statuses.length)];
        const currentTask = status === 'focusing' ? tasks[i % tasks.length] : '';
        
        // Simulate presence update
        socketService.handleRoomJoin({ roomId }, userId);
        socketService.handlePresenceUpdate({ status, currentTask }, userId);
      } catch (error) {
        // Ignore join errors for dummy data
      }
    }
  }

  private startActivitySimulation(): void {
    // Simulate status changes every 30-60 seconds
    const statusInterval = setInterval(() => {
      this.simulateStatusChanges();
    }, 45000);
    
    // Simulate task updates every 2-3 minutes
    const taskInterval = setInterval(() => {
      this.simulateTaskUpdates();
    }, 150000);
    
    this.intervalIds.push(statusInterval, taskInterval);
  }

  private simulateStatusChanges(): void {
    const statuses: ParticipantStatus['status'][] = ['focusing', 'break', 'idle', 'away'];
    
    this.dummyRooms.forEach((room) => {
      // Randomly change 1-2 participants' status
      const participantsToChange = Math.min(2, room.participants.length);
      
      for (let i = 0; i < participantsToChange; i++) {
        const participant = room.participants[Math.floor(Math.random() * room.participants.length)];
        const newStatus = statuses[Math.floor(Math.random() * statuses.length)];
        
        if (socketService.io) {
          socketService.handlePresenceUpdate({ status: newStatus }, participant);
        }
      }
    });
  }

  private simulateTaskUpdates(): void {
    const tasks = [
      'Working on project documentation',
      'Debugging code issues',
      'Reviewing study materials',
      'Writing research paper',
      'Preparing presentation',
      'Data analysis',
      'Reading technical articles',
      'Practice problems'
    ];
    
    this.dummyRooms.forEach((room) => {
      // Update tasks for focusing participants
      const focusingParticipants = room.participants.filter(p => {
        // Check participant status in socket service
        return true; // Simplified for now
      });
      
      if (focusingParticipants.length > 0) {
        const participant = focusingParticipants[Math.floor(Math.random() * focusingParticipants.length)];
        const newTask = tasks[Math.floor(Math.random() * tasks.length)];
        
        if (socketService.io) {
          socketService.handlePresenceUpdate({ status: 'focusing', currentTask: newTask }, participant);
        }
      }
    });
  }

  cleanup(): void {
    console.log('🧹 Cleaning up enhanced dummy data...');
    
    // Clear intervals
    this.intervalIds.forEach(id => clearInterval(id));
    this.intervalIds = [];
    
    // Remove dummy rooms
    this.dummyRooms.forEach((room) => {
      // Clear participants
      room.participants = [];
    });
    
    // Clear maps
    this.dummyUsers.clear();
    this.dummyRooms.clear();
    
    console.log('✅ Cleanup complete');
  }
}

// Export singleton instance
export const enhancedDummyData = EnhancedDummyDataGenerator.getInstance();