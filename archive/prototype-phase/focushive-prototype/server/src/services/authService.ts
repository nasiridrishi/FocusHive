import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { User, AuthResponse } from '@focushive/shared';
import { dataStore } from '../data/store';

export class AuthService {
  private jwtSecret: string;

  constructor() {
    this.jwtSecret = process.env.JWT_SECRET || 'default-secret-key';
  }

  async register(email: string, username: string, password: string): Promise<AuthResponse> {
    // Check if user exists
    const existingUser = dataStore.getUserByEmail(email);
    if (existingUser) {
      throw new Error('User already exists');
    }

    // Hash password
    const hashedPassword = await bcrypt.hash(password, 10);

    // Create user
    const user: User = {
      id: `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      email,
      username,
      password: hashedPassword,
      avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(username)}&background=random`,
      totalFocusTime: 0,
      currentStreak: 0,
      longestStreak: 0,
      points: 0,
      lastActiveDate: new Date().toISOString(),
      lookingForBuddy: false,
      preferences: {
        darkMode: false,
        soundEnabled: true,
        defaultPomodoro: {
          focusDuration: 25,
          breakDuration: 5
        }
      },
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    dataStore.createUser(user);

    // Generate token
    const token = this.generateToken(user);

    // Remove password from response
    const { password: _, ...userWithoutPassword } = user;

    return { 
      user: userWithoutPassword as User, 
      token 
    };
  }

  async login(email: string, password: string): Promise<AuthResponse> {
    const user = dataStore.getUserByEmail(email);
    if (!user) {
      throw new Error('Invalid credentials');
    }

    const isValidPassword = await bcrypt.compare(password, user.password);
    if (!isValidPassword) {
      throw new Error('Invalid credentials');
    }

    // Update last active date
    dataStore.updateUser(user.id, { lastActiveDate: new Date().toISOString() });

    const token = this.generateToken(user);

    // Remove password from response
    const { password: _, ...userWithoutPassword } = user;

    return { 
      user: userWithoutPassword as User, 
      token 
    };
  }

  verifyToken(token: string): { userId: string; email: string } {
    const decoded = jwt.verify(token, this.jwtSecret) as any;
    return { userId: decoded.userId, email: decoded.email };
  }

  private generateToken(user: User): string {
    return jwt.sign(
      { userId: user.id, email: user.email },
      this.jwtSecret,
      { expiresIn: '7d' }
    );
  }
}

export const authService = new AuthService();