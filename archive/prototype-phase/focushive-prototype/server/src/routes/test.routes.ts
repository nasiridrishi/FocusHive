import { Router, Request, Response } from 'express';
import { authService } from '../services/authService';
import { dataStore } from '../data/store';

const router = Router();

// Create test users (only in development)
router.post('/create-test-users', async (req: Request, res: Response) => {
  if (process.env.NODE_ENV === 'production') {
    return res.status(403).json({ error: 'Not available in production' });
  }

  try {
    const createdUsers = [];
    
    // Test users with varying stats
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
        const updatedUser = dataStore.updateUser(user.id, {
          totalFocusTime: userData.totalFocusTime,
          currentStreak: userData.currentStreak,
          points: userData.points,
          lookingForBuddy: userData.lookingForBuddy,
          lastActiveDate: new Date().toISOString()
        });
        
        if (updatedUser) {
          createdUsers.push({
            username: updatedUser.username,
            email: updatedUser.email,
            lookingForBuddy: updatedUser.lookingForBuddy
          });
        }
      } catch (error: any) {
        // User might already exist
        console.log(`User ${userData.email} might already exist:`, error.message);
      }
    }
    
    res.json({
      message: 'Test users created',
      users: createdUsers,
      note: 'Login with password: password123'
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

export default router;