import { dataStore } from '../data/store';
import { authService } from '../services/authService';

async function createTestUsers() {
  console.log('Creating test users...');
  
  try {
    // Create multiple test users with varying stats
    const testUsers = [
      {
        email: 'alice@test.com',
        username: 'Alice',
        password: 'password123',
        totalFocusTime: 150,
        currentStreak: 7,
        points: 750,
        lookingForBuddy: true
      },
      {
        email: 'bob@test.com',
        username: 'Bob',
        password: 'password123',
        totalFocusTime: 120,
        currentStreak: 5,
        points: 600,
        lookingForBuddy: true
      },
      {
        email: 'charlie@test.com',
        username: 'Charlie',
        password: 'password123',
        totalFocusTime: 200,
        currentStreak: 10,
        points: 1000,
        lookingForBuddy: true
      },
      {
        email: 'diana@test.com',
        username: 'Diana',
        password: 'password123',
        totalFocusTime: 80,
        currentStreak: 3,
        points: 400,
        lookingForBuddy: true
      },
      {
        email: 'eve@test.com',
        username: 'Eve',
        password: 'password123',
        totalFocusTime: 300,
        currentStreak: 15,
        points: 1500,
        lookingForBuddy: false // Not looking for buddy
      }
    ];

    for (const userData of testUsers) {
      try {
        // Register user
        const { user } = await authService.register(
          userData.email,
          userData.username,
          userData.password
        );
        
        // Update their stats
        dataStore.updateUser(user.id, {
          totalFocusTime: userData.totalFocusTime,
          currentStreak: userData.currentStreak,
          points: userData.points,
          lookingForBuddy: userData.lookingForBuddy,
          lastActiveDate: new Date().toISOString()
        });
        
        console.log(`✓ Created user: ${userData.username} (${userData.email})`);
      } catch (error: any) {
        if (error.message.includes('already exists')) {
          console.log(`- User ${userData.email} already exists`);
        } else {
          console.error(`✗ Failed to create ${userData.username}:`, error.message);
        }
      }
    }
    
    console.log('\nTest users created successfully!');
    console.log('You can now log in with any of these users using password: password123');
    
  } catch (error) {
    console.error('Error creating test users:', error);
  }
}

// Run the script
createTestUsers();