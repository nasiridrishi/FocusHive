/**
 * Dummy Data Generator for FocusHive Demo
 * This module generates realistic dummy users and activity for screenshots and demos
 * Can be easily removed for production by not calling initializeDummyData()
 */

import { dataStore } from '../data/store';
import { gamificationStore } from '../data/gamificationStore';
import { chatStore } from '../data/chatStore';
import { forumStore } from '../data/forumStore';
import { User, Room, ForumPost, ChatMessage } from '@focushive/shared';
import bcrypt from 'bcryptjs';

// Realistic names for diverse users
const DUMMY_USERS = [
  // Students
  { name: 'Emma Chen', email: 'emma.chen@university.edu', role: 'student', field: 'Computer Science' },
  { name: 'James Wilson', email: 'j.wilson@college.edu', role: 'student', field: 'Medicine' },
  { name: 'Priya Patel', email: 'priya.p@uni.edu', role: 'student', field: 'Engineering' },
  { name: 'Lucas Brown', email: 'lucas.b@school.edu', role: 'student', field: 'Mathematics' },
  { name: 'Sofia Rodriguez', email: 'sofia.r@university.edu', role: 'student', field: 'Psychology' },
  { name: 'Ahmed Hassan', email: 'ahmed.h@college.edu', role: 'student', field: 'Physics' },
  { name: 'Maria Garcia', email: 'maria.g@uni.edu', role: 'student', field: 'Literature' },
  { name: 'David Kim', email: 'david.k@school.edu', role: 'student', field: 'Business' },
  
  // Remote Workers
  { name: 'Sarah Johnson', email: 'sarah@techcorp.com', role: 'worker', field: 'Software Developer' },
  { name: 'Michael Zhang', email: 'mzhang@design.co', role: 'worker', field: 'UX Designer' },
  { name: 'Alex Thompson', email: 'alex.t@marketing.io', role: 'worker', field: 'Content Writer' },
  { name: 'Olivia Davis', email: 'olivia@fintech.com', role: 'worker', field: 'Data Analyst' },
  { name: 'Ryan Mitchell', email: 'ryan.m@startup.io', role: 'worker', field: 'Product Manager' },
  { name: 'Nina Kowalski', email: 'nina@freelance.com', role: 'worker', field: 'Graphic Designer' },
  { name: 'Carlos Mendez', email: 'carlos@consult.co', role: 'worker', field: 'Consultant' },
  { name: 'Lisa Anderson', email: 'lisa.a@remote.work', role: 'worker', field: 'Project Manager' }
];

// Realistic room names
const DUMMY_ROOMS = [
  // Study rooms
  { name: '📚 CS Finals Study Group', description: 'Preparing for Computer Science final exams', type: 'study' },
  { name: '🏥 Med School Study Hive', description: 'MCAT prep and anatomy review', type: 'study' },
  { name: '📐 Engineering Problem Sets', description: 'Working through thermodynamics problems together', type: 'study' },
  { name: '📝 Essay Writing Sprint', description: 'Literature essays due this week!', type: 'study' },
  { name: '🧮 Calculus III Marathon', description: 'Integration techniques and series', type: 'study' },
  { name: '🔬 Chemistry Lab Reports', description: 'Organic chemistry lab write-ups', type: 'study' },
  
  // Work rooms
  { name: '💻 Deep Work Zone', description: 'No meetings, just focused coding', type: 'work' },
  { name: '🎨 Design Sprint', description: 'UI/UX designers collaborating on projects', type: 'work' },
  { name: '📊 Data Analysis Cave', description: 'Crunching numbers in peace', type: 'work' },
  { name: '✍️ Writers\' Room', description: 'Content creators and copywriters unite', type: 'work' },
  { name: '🚀 Startup Hustle', description: 'Building the next big thing', type: 'work' },
  { name: '🌐 Remote Team Sync', description: 'Distributed team working together', type: 'work' }
];

// Forum posts content
const FORUM_POSTS = [
  // Study-related posts
  {
    title: 'Looking for study buddy for Organic Chemistry',
    content: 'Hey everyone! I\'m struggling with Organic Chemistry II and looking for someone to study with. I\'m usually available evenings EST. We could work through practice problems together!',
    tags: ['study-buddy', 'chemistry', 'science'],
    type: 'student'
  },
  {
    title: 'Starting a daily MCAT study group',
    content: 'Planning to take the MCAT in 3 months. Want to create a consistent study group that meets every morning at 8 AM EST. Looking for serious, committed people!',
    tags: ['mcat', 'medical-school', 'study-group'],
    type: 'student'
  },
  {
    title: 'CS algorithms study session tonight',
    content: 'Working through LeetCode problems for interview prep. Join me tonight at 7 PM PST if you want to practice together. Focus on trees and graphs.',
    tags: ['algorithms', 'leetcode', 'computer-science'],
    type: 'student'
  },
  {
    title: 'Finals week survival hive 🎯',
    content: 'Who else is drowning in finals? Let\'s create a supportive study environment. Pomodoro sessions with short chat breaks to keep each other sane!',
    tags: ['finals', 'study-group', 'motivation'],
    type: 'student'
  },
  
  // Work-related posts
  {
    title: 'Early morning deep work sessions',
    content: 'I\'m most productive from 5-8 AM. Looking for other early birds who want to maintain accountability during deep work sessions. No meetings, just focus!',
    tags: ['deep-work', 'productivity', 'morning'],
    type: 'worker'
  },
  {
    title: 'Freelance designers co-working space',
    content: 'Creating a virtual co-working space for freelance designers. Share wins, get feedback, and beat the isolation of working alone!',
    tags: ['design', 'freelance', 'co-working'],
    type: 'worker'
  },
  {
    title: 'Developer focus sessions - no bugs allowed! 🐛',
    content: 'Join our developer-focused hive for uninterrupted coding sessions. We do 90-minute sprints with 15-minute breaks. Currently working on React/Node projects.',
    tags: ['coding', 'developer', 'react'],
    type: 'worker'
  },
  {
    title: 'Writers\' accountability group',
    content: 'For content writers, copywriters, and authors. Daily word count check-ins and focused writing sprints. Let\'s hit those deadlines together!',
    tags: ['writing', 'content', 'accountability'],
    type: 'worker'
  }
];

// Chat messages for break time
const CHAT_MESSAGES = [
  // Student chats
  'Just finished chapter 5! Anyone else finding the material harder than expected?',
  'Taking a quick break. My brain is fried from all these equations 🤯',
  'Coffee break! ☕ How\'s everyone\'s study session going?',
  'Finally understood that concept! Sometimes it just clicks after the 10th time 😅',
  'Anyone want to do a quick review of yesterday\'s material?',
  'Pomodoro break! Stretching is so important. Don\'t forget to move!',
  
  // Worker chats
  'Just pushed my code! Feels good to complete that feature 🎉',
  'Break time! Anyone else struggling with Zoom fatigue today?',
  'Completed 3 tasks this morning. Productivity is through the roof here!',
  'Love working alongside you all. Makes remote work less lonely 💙',
  'Quick break to refill coffee. This debugging session is intense!',
  'Shipped the design! Thanks for the motivation, hive mates!'
];

export class DummyDataGenerator {
  private static instance: DummyDataGenerator;
  private dummyUserIds: string[] = [];
  private intervalIds: NodeJS.Timeout[] = [];

  private constructor() {}

  static getInstance(): DummyDataGenerator {
    if (!DummyDataGenerator.instance) {
      DummyDataGenerator.instance = new DummyDataGenerator();
    }
    return DummyDataGenerator.instance;
  }

  /**
   * Initialize dummy data - call this once on server start for demo mode
   */
  async initializeDummyData(): Promise<void> {
    console.log('🎭 Initializing dummy data for demo...');
    
    // Create dummy users
    await this.createDummyUsers();
    
    // Create dummy rooms
    await this.createDummyRooms();
    
    // Create forum posts
    await this.createForumPosts();
    
    // Initialize gamification data
    await this.initializeGamificationData();
    
    // Start simulated activities
    this.startSimulatedActivities();
    
    console.log('✅ Dummy data initialized successfully');
  }

  private async createDummyUsers(): Promise<void> {
    const passwordHash = await bcrypt.hash('demo123', 10);
    
    for (const userData of DUMMY_USERS) {
      const userId = `dummy_${userData.email.split('@')[0]}`;
      
      const user: User = {
        id: userId,
        email: userData.email,
        username: userData.name,
        password: passwordHash, // The type expects 'password' not 'passwordHash'
        avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(userData.name)}&background=random`,
        totalFocusTime: Math.floor(Math.random() * 50000) + 10000, // 10-50k minutes
        currentStreak: Math.floor(Math.random() * 30), // 0-30 day streak
        longestStreak: Math.floor(Math.random() * 45) + 5, // 5-50 days
        points: Math.floor(Math.random() * 5000) + 500, // 500-5500 points
        lastActiveDate: new Date().toISOString(),
        lookingForBuddy: Math.random() > 0.5,
        preferences: {
          darkMode: Math.random() > 0.5,
          soundEnabled: true,
          defaultPomodoro: {
            focusDuration: 25,
            breakDuration: 5
          }
        },
        createdAt: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString(), // Within 30 days
        updatedAt: new Date().toISOString()
      };
      
      dataStore.createUser(user);
      this.dummyUserIds.push(userId);
    }
  }

  private async createDummyRooms(): Promise<void> {
    let roomIndex = 100; // Start from 100 to avoid conflicts
    
    for (const roomData of DUMMY_ROOMS) {
      const room: Room = {
        id: `dummy_room_${roomIndex++}`,
        name: roomData.name,
        description: roomData.description,
        type: 'public',
        focusType: roomData.type === 'study' ? 'study' : 'deepWork',
        isPublic: true,
        maxParticipants: 12,
        ownerId: this.getRandomDummyUserId(),
        participants: this.getRandomParticipants(3, 8), // 3-8 participants per room
        tags: roomData.type === 'study' ? ['study', 'academic'] : ['work', 'professional'],
        pomodoroSettings: {
          focusDuration: 25,
          shortBreakDuration: 5,
          longBreakDuration: 15,
          sessionsUntilLongBreak: 4
        },
        createdAt: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
        updatedAt: new Date().toISOString()
      };
      
      dataStore.createRoom(room);
    }
  }

  private async createForumPosts(): Promise<void> {
    for (const postData of FORUM_POSTS) {
      const authorId = this.getRandomDummyUserId(postData.type);
      const author = dataStore.getUserById(authorId);
      
      if (author) {
        const isStudyPost = postData.type === 'student';
        const post: ForumPost = {
          id: `dummy_post_${Date.now()}_${Math.random()}`,
          userId: author.id,
          username: author.username,
          userAvatar: author.avatar || '',
          title: postData.title,
          type: isStudyPost ? 'study' : 'work',
          description: postData.content,
          tags: postData.tags,
          status: 'active',
          schedule: {
            days: ['monday', 'wednesday', 'friday'],
            timeSlots: [{ start: '18:00', end: '20:00' }],
            timezone: 'EST'
          },
          commitmentLevel: 'weekly',
          workingStyle: {
            videoPreference: 'optional',
            communicationStyle: 'moderate',
            breakPreference: 'synchronized'
          },
          responses: [],
          createdAt: new Date(Date.now() - Math.random() * 3 * 24 * 60 * 60 * 1000).toISOString(),
          updatedAt: new Date().toISOString()
        };
        
        forumStore.createPost(post);
      }
    }
  }

  private async initializeGamificationData(): Promise<void> {
    const achievements = [
      'early-bird', 'night-owl', 'focus-master', 'streak-keeper',
      'team-player', 'helpful-peer', 'consistency-king', 'productivity-guru'
    ];
    
    // Give random achievements to dummy users
    for (const userId of this.dummyUserIds) {
      const numAchievements = Math.floor(Math.random() * 5) + 1; // 1-5 achievements
      const userAchievements = this.getRandomItems(achievements, numAchievements);
      
      for (const achievementId of userAchievements) {
        gamificationStore.awardAchievement(userId, achievementId);
      }
    }
  }

  private startSimulatedActivities(): void {
    // Simulate users joining/leaving rooms
    const roomActivityInterval = setInterval(() => {
      this.simulateRoomActivity();
    }, 30000); // Every 30 seconds
    
    // Simulate chat messages
    const chatInterval = setInterval(() => {
      this.simulateChatMessage();
    }, 45000); // Every 45 seconds
    
    // Simulate status changes
    const statusInterval = setInterval(() => {
      this.simulateStatusChange();
    }, 20000); // Every 20 seconds
    
    this.intervalIds.push(roomActivityInterval, chatInterval, statusInterval);
  }

  private simulateRoomActivity(): void {
    const rooms = dataStore.getAllRooms().filter(r => r.id.startsWith('dummy_'));
    if (rooms.length === 0) return;
    
    const room = rooms[Math.floor(Math.random() * rooms.length)];
    const userId = this.getRandomDummyUserId();
    
    // Randomly join or leave
    if (room.participants.includes(userId)) {
      // Leave room
      room.participants = room.participants.filter(id => id !== userId);
    } else if (room.participants.length < room.maxParticipants) {
      // Join room
      room.participants.push(userId);
    }
  }

  private simulateChatMessage(): void {
    const rooms = dataStore.getActiveRooms().filter(r => r.id.startsWith('dummy_'));
    if (rooms.length === 0) return;
    
    const room = rooms[Math.floor(Math.random() * rooms.length)];
    if (room.participants.length === 0) return;
    
    const userId = room.participants[Math.floor(Math.random() * room.participants.length)];
    const user = dataStore.getUserById(userId);
    if (!user) return;
    
    const message: ChatMessage = {
      id: `msg_${Date.now()}`,
      roomId: room.id,
      userId: user.id,
      username: user.username,
      message: CHAT_MESSAGES[Math.floor(Math.random() * CHAT_MESSAGES.length)],
      timestamp: new Date().toISOString(),
      type: 'user'
    };
    
    chatStore.addMessage(message);
  }

  private simulateStatusChange(): void {
    const statuses = ['focusing', 'studying', 'in-break', 'away'];
    const userId = this.getRandomDummyUserId();
    const status = statuses[Math.floor(Math.random() * statuses.length)];
    
    // This would normally update through socket connections
    // For now, just update the user's last active time
    const user = dataStore.getUserById(userId);
    if (user) {
      user.lastActiveDate = new Date().toISOString();
    }
  }

  // Helper methods
  private getRandomDummyUserId(type?: string): string {
    if (type === 'student') {
      const studentIds = this.dummyUserIds.filter(id => id.includes('university') || id.includes('college') || id.includes('school'));
      return studentIds[Math.floor(Math.random() * studentIds.length)];
    } else if (type === 'worker') {
      const workerIds = this.dummyUserIds.filter(id => !id.includes('university') && !id.includes('college') && !id.includes('school'));
      return workerIds[Math.floor(Math.random() * workerIds.length)];
    }
    return this.dummyUserIds[Math.floor(Math.random() * this.dummyUserIds.length)];
  }

  private getRandomParticipants(min: number, max: number): string[] {
    const count = Math.floor(Math.random() * (max - min + 1)) + min;
    const participants = new Set<string>();
    
    while (participants.size < count && participants.size < this.dummyUserIds.length) {
      participants.add(this.getRandomDummyUserId());
    }
    
    return Array.from(participants);
  }

  private getRandomItems<T>(array: T[], count: number): T[] {
    const shuffled = [...array].sort(() => 0.5 - Math.random());
    return shuffled.slice(0, count);
  }

  /**
   * Clean up dummy data - call this to remove all dummy data
   */
  cleanupDummyData(): void {
    console.log('🧹 Cleaning up dummy data...');
    
    // Stop all intervals
    this.intervalIds.forEach(id => clearInterval(id));
    this.intervalIds = [];
    
    // Remove dummy users
    for (const userId of this.dummyUserIds) {
      dataStore.deleteUser(userId);
    }
    
    // Remove dummy rooms
    const rooms = dataStore.getAllRooms();
    rooms.forEach(room => {
      if (room.id.startsWith('dummy_')) {
        dataStore.deleteRoom(room.id);
      }
    });
    
    // Remove dummy forum posts
    const posts = forumStore.getAllPosts();
    posts.forEach(post => {
      if (post.id.startsWith('dummy_')) {
        forumStore.deletePost(post.id);
      }
    });
    
    this.dummyUserIds = [];
    console.log('✅ Dummy data cleaned up');
  }
}

// Export a function to initialize dummy data
export const initializeDummyData = () => DummyDataGenerator.getInstance().initializeDummyData();
export const cleanupDummyData = () => DummyDataGenerator.getInstance().cleanupDummyData();