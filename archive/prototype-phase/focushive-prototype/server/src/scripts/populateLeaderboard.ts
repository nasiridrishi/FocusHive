/**
 * Populates leaderboard with dummy users
 */

import { dataStore } from '../data/store';
import { Session } from '@focushive/shared';
import { v4 as uuidv4 } from 'uuid';

export function populateLeaderboard(): void {
  console.log('📊 Populating leaderboard with dummy user sessions...');
  
  // Get all dummy users
  const dummyUsers = dataStore.getAllUsers().filter(user => 
    user.id.startsWith('dummy-') || user.id.startsWith('dummy_')
  );
  
  if (dummyUsers.length === 0) {
    console.log('⚠️ No dummy users found for leaderboard');
    return;
  }
  
  const now = Date.now();
  const dayMs = 24 * 60 * 60 * 1000;
  const weekMs = 7 * dayMs;
  
  // Create sessions for each dummy user
  dummyUsers.forEach((user, index) => {
    // Different activity levels for variety
    const activityLevel = index % 3; // 0 = low, 1 = medium, 2 = high
    
    // Sessions for the past week
    let sessionsToCreate = 0;
    let sessionDurations: number[] = [];
    
    switch (activityLevel) {
      case 0: // Low activity
        sessionsToCreate = 3 + Math.floor(Math.random() * 3); // 3-5 sessions
        sessionDurations = [15, 20, 25, 30]; // Shorter sessions
        break;
      case 1: // Medium activity
        sessionsToCreate = 6 + Math.floor(Math.random() * 4); // 6-9 sessions
        sessionDurations = [25, 30, 45, 50]; // Standard sessions
        break;
      case 2: // High activity
        sessionsToCreate = 10 + Math.floor(Math.random() * 5); // 10-14 sessions
        sessionDurations = [45, 50, 60, 90]; // Longer sessions
        break;
    }
    
    let totalFocusTime = 0;
    
    for (let i = 0; i < sessionsToCreate; i++) {
      // Spread sessions across the week
      const daysAgo = Math.floor(Math.random() * 7);
      const hoursAgo = Math.floor(Math.random() * 24);
      const startTime = now - (daysAgo * dayMs) - (hoursAgo * 60 * 60 * 1000);
      
      // Random duration from the user's typical durations
      const duration = sessionDurations[Math.floor(Math.random() * sessionDurations.length)];
      const endTime = startTime + (duration * 60 * 1000);
      
      const session: Session = {
        id: uuidv4(),
        userId: user.id,
        roomId: 'dummy-room-' + Math.floor(Math.random() * 8),
        startTime,
        endTime,
        duration, // in minutes
        type: 'focus',
        completed: true,
        pointsEarned: Math.floor(duration / 5) * 5 // 5 points per 5 minutes
      };
      
      dataStore.createSession(session);
      totalFocusTime += duration;
    }
    
    // Update user's total focus time and points
    const currentUser = dataStore.getUser(user.id);
    if (currentUser) {
      const existingTime = currentUser.totalFocusTime || 0;
      const existingPoints = currentUser.points || 0;
      
      dataStore.updateUser(user.id, {
        totalFocusTime: existingTime + totalFocusTime,
        points: existingPoints + (sessionsToCreate * 10), // Bonus points
        currentStreak: Math.floor(Math.random() * 20) + 1, // 1-20 day streak
        longestStreak: Math.floor(Math.random() * 30) + 10, // 10-40 days
        lastActiveDate: new Date(now - (Math.random() * dayMs)).toISOString()
      });
    }
  });
  
  // Add some sessions for today specifically
  const todayUsers = dummyUsers.slice(0, Math.floor(dummyUsers.length * 0.7)); // 70% active today
  todayUsers.forEach(user => {
    const sessionsToday = 1 + Math.floor(Math.random() * 3); // 1-3 sessions today
    
    for (let i = 0; i < sessionsToday; i++) {
      const hoursAgo = Math.floor(Math.random() * 12); // Within last 12 hours
      const startTime = now - (hoursAgo * 60 * 60 * 1000);
      const duration = 25 + Math.floor(Math.random() * 35); // 25-60 minutes
      
      const session: Session = {
        id: uuidv4(),
        userId: user.id,
        roomId: 'dummy-room-' + Math.floor(Math.random() * 8),
        startTime,
        endTime: startTime + (duration * 60 * 1000),
        duration,
        type: 'focus',
        completed: true,
        pointsEarned: Math.floor(duration / 5) * 5
      };
      
      dataStore.createSession(session);
    }
  });
  
  console.log('✅ Leaderboard populated with dummy user sessions');
  
  // Log sample leaderboard
  const dailyLeaderboard = dataStore.getLeaderboard('daily', 5);
  console.log('📈 Daily Leaderboard Sample:');
  dailyLeaderboard.forEach((entry, index) => {
    console.log(`  ${index + 1}. ${entry.username}: ${entry.focusTime} minutes`);
  });
}