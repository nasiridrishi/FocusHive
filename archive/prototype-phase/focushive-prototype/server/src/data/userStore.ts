import { User } from '@focushive/shared';

interface LocalUser extends Omit<User, 'password'> {
  passwordHash: string;
}

class UserStore {
  private users: Map<string, LocalUser> = new Map();
  private emailIndex: Map<string, string> = new Map();

  async create(userData: Omit<LocalUser, 'id' | 'createdAt' | 'updatedAt'>): Promise<LocalUser> {
    const id = Math.random().toString(36).substr(2, 9);
    const now = new Date().toISOString();
    
    const user: LocalUser = {
      ...userData,
      id,
      createdAt: now,
      updatedAt: now
    };
    
    this.users.set(id, user);
    this.emailIndex.set(user.email.toLowerCase(), id);
    
    return user;
  }

  async findById(id: string): Promise<LocalUser | null> {
    return this.users.get(id) || null;
  }

  async findByEmail(email: string): Promise<LocalUser | null> {
    const userId = this.emailIndex.get(email.toLowerCase());
    if (!userId) return null;
    return this.users.get(userId) || null;
  }

  async update(id: string, updates: Partial<LocalUser>): Promise<void> {
    const user = this.users.get(id);
    if (!user) throw new Error('User not found');
    
    const updatedUser = {
      ...user,
      ...updates,
      id: user.id, // Prevent id change
      createdAt: user.createdAt, // Prevent createdAt change
      updatedAt: new Date().toISOString()
    };
    
    this.users.set(id, updatedUser);
    
    // Update email index if email changed
    if (updates.email && updates.email !== user.email) {
      this.emailIndex.delete(user.email.toLowerCase());
      this.emailIndex.set(updates.email.toLowerCase(), id);
    }
  }

  async delete(id: string): Promise<void> {
    const user = this.users.get(id);
    if (user) {
      this.users.delete(id);
      this.emailIndex.delete(user.email.toLowerCase());
    }
  }

  async getAll(): Promise<LocalUser[]> {
    return Array.from(this.users.values());
  }

  reset(): void {
    this.users.clear();
    this.emailIndex.clear();
  }
}

export const userStore = new UserStore();