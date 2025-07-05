import { Router, Request, Response } from 'express';
import { authService } from '../services/authService';
import { authenticate } from '../middleware/auth';
import { dataStore } from '../data/store';

const router = Router();

// Register
router.post('/register', async (req: Request, res: Response) => {
  try {
    const { email, username, password } = req.body;

    // Validation
    if (!email || !username || !password) {
      return res.status(400).json({ error: 'Missing required fields' });
    }

    if (password.length < 6) {
      return res.status(400).json({ error: 'Password must be at least 6 characters' });
    }

    const result = await authService.register(email, username, password);
    res.json(result);
  } catch (error: any) {
    res.status(400).json({ error: error.message });
  }
});

// Login
router.post('/login', async (req: Request, res: Response) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: 'Missing required fields' });
    }

    const result = await authService.login(email, password);
    res.json(result);
  } catch (error: any) {
    res.status(401).json({ error: error.message });
  }
});

// Get current user
router.get('/me', authenticate, async (req: Request, res: Response) => {
  try {
    const user = dataStore.getUser(req.user!.id);
    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }

    // Remove password from response
    const { password, ...userWithoutPassword } = user;
    res.json(userWithoutPassword);
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
});

// Logout (client-side token removal, but we can use this for cleanup)
router.post('/logout', authenticate, async (req: Request, res: Response) => {
  // In a real app, you might want to blacklist the token or clear sessions
  res.json({ message: 'Logged out successfully' });
});

// Update profile
router.put('/profile', authenticate, async (req: Request, res: Response) => {
  try {
    const { lookingForBuddy } = req.body;
    const userId = req.user!.id;
    
    const user = dataStore.getUser(userId);
    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }

    // Update user profile
    const updatedUser = dataStore.updateUser(userId, {
      lookingForBuddy: lookingForBuddy !== undefined ? lookingForBuddy : user.lookingForBuddy
    });

    if (!updatedUser) {
      return res.status(500).json({ error: 'Failed to update profile' });
    }

    // Return updated user without password
    const { password, ...userWithoutPassword } = updatedUser;
    res.json(userWithoutPassword);
  } catch (error) {
    console.error('Update profile error:', error);
    res.status(500).json({ error: 'Server error' });
  }
});

export default router;