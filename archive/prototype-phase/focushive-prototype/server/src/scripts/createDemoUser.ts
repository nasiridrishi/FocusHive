/**
 * Creates a demo user for easy testing
 */

import { User } from '@focushive/shared';
import { dataStore } from '../data/store';
import bcrypt from 'bcryptjs';

export async function createDemoUser(): Promise<void> {
  const demoEmail = 'demo@focushive.com';
  
  // Check if demo user already exists
  const existingUser = dataStore.getUserByEmail(demoEmail);
  if (existingUser) {
    console.log('✅ Demo user already exists');
    console.log('📧 Email: demo@focushive.com');
    console.log('🔑 Password: demo123');
    return;
  }

  // Create demo user
  const passwordHash = await bcrypt.hash('demo123', 10);
  
  const demoUser: User = {
    id: 'demo-user-001',
    email: demoEmail,
    username: 'Demo User',
    password: passwordHash,
    avatar: 'https://ui-avatars.com/api/?name=Demo+User&background=4f46e5&color=fff',
    lookingForBuddy: true,
    totalFocusTime: 1250, // ~20 hours
    currentStreak: 7,
    longestStreak: 14,
    points: 2500,
    lastActiveDate: new Date().toISOString(),
    preferences: {
      darkMode: false,
      soundEnabled: true,
      defaultPomodoro: {
        focusDuration: 25,
        breakDuration: 5
      }
    },
    createdAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(), // 7 days ago
    updatedAt: new Date().toISOString()
  };
  
  dataStore.createUser(demoUser);
  
  console.log('✨ Demo user created successfully!');
  console.log('📧 Email: demo@focushive.com');
  console.log('🔑 Password: demo123');
  console.log('\nYou can also run ./demo-mode.sh to automatically login');
}