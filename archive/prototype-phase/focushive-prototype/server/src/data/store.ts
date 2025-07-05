import { User, Room, Session, ChatMessage } from '@focushive/shared';

// Local participant type with room information
interface RoomParticipant {
  userId: string;
  roomId: string;
  joinedAt: number;
}

class DataStore {
  private users = new Map<string, User>();
  private rooms = new Map<string, Room>();
  private sessions = new Map<string, Session[]>();
  private participants = new Map<string, RoomParticipant>();
  private messages = new Map<string, ChatMessage[]>();
  private emailIndex = new Map<string, string>(); // email -> userId

  // User methods
  createUser(user: User): User {
    this.users.set(user.id, user);
    this.emailIndex.set(user.email, user.id);
    return user;
  }

  getUser(id: string): User | undefined {
    return this.users.get(id);
  }

  getUserById(id: string): User | undefined {
    return this.users.get(id);
  }

  getUsers(): User[] {
    return Array.from(this.users.values());
  }

  getUserByEmail(email: string): User | undefined {
    const userId = this.emailIndex.get(email);
    return userId ? this.users.get(userId) : undefined;
  }

  updateUser(id: string, updates: Partial<User>): User | undefined {
    const user = this.users.get(id);
    if (!user) return undefined;
    
    const updatedUser = { ...user, ...updates, updatedAt: new Date().toISOString() };
    this.users.set(id, updatedUser);
    
    // Update email index if email changed
    if (updates.email && updates.email !== user.email) {
      this.emailIndex.delete(user.email);
      this.emailIndex.set(updates.email, id);
    }
    
    return updatedUser;
  }

  getAllUsers(): User[] {
    return Array.from(this.users.values());
  }

  deleteUser(id: string): void {
    const user = this.users.get(id);
    if (!user) return;
    
    // Remove from email index
    this.emailIndex.delete(user.email);
    
    // Remove from users map
    this.users.delete(id);
    
    // Clean up sessions
    this.sessions.delete(id);
    
    // Remove from all rooms
    for (const room of this.rooms.values()) {
      room.participants = room.participants.filter(pid => pid !== id);
    }
    
    // Remove participant entries
    for (const [key, participant] of this.participants.entries()) {
      if (participant.userId === id) {
        this.participants.delete(key);
      }
    }
  }

  // Room methods
  createRoom(room: Room): Room {
    this.rooms.set(room.id, room);
    return room;
  }

  getRoom(id: string): Room | undefined {
    return this.rooms.get(id);
  }

  getPublicRooms(): Room[] {
    return Array.from(this.rooms.values()).filter(room => room.isPublic);
  }

  getAllRooms(): Room[] {
    return Array.from(this.rooms.values());
  }

  getActiveRooms(): Room[] {
    return Array.from(this.rooms.values()).filter(room => room.participants.length > 0);
  }

  updateRoom(id: string, updates: Partial<Room>): Room | undefined {
    const room = this.rooms.get(id);
    if (!room) return undefined;
    
    const updatedRoom = { ...room, ...updates, updatedAt: new Date().toISOString() };
    this.rooms.set(id, updatedRoom);
    return updatedRoom;
  }

  deleteRoom(id: string): void {
    this.rooms.delete(id);
    // Clean up related data
    this.messages.delete(id);
    // Remove participants
    for (const [key, participant] of this.participants.entries()) {
      if (participant.roomId === id) {
        this.participants.delete(key);
      }
    }
  }

  // Participant methods
  addParticipant(roomId: string, userId: string): RoomParticipant {
    const key = `${roomId}:${userId}`;
    const participant: RoomParticipant = {
      userId,
      roomId,
      joinedAt: Date.now()
    };
    
    this.participants.set(key, participant);
    
    // Update room participants list
    const room = this.rooms.get(roomId);
    if (room && !room.participants.includes(userId)) {
      room.participants.push(userId);
    }
    
    return participant;
  }

  removeParticipant(roomId: string, userId: string): void {
    const key = `${roomId}:${userId}`;
    this.participants.delete(key);
    
    // Update room participants list
    const room = this.rooms.get(roomId);
    if (room) {
      room.participants = room.participants.filter(id => id !== userId);
    }
  }

  getRoomParticipants(roomId: string): RoomParticipant[] {
    const participants: RoomParticipant[] = [];
    for (const [key, participant] of this.participants.entries()) {
      if (participant.roomId === roomId) {
        participants.push(participant);
      }
    }
    return participants;
  }

  getParticipant(roomId: string, userId: string): RoomParticipant & { username: string } | null {
    const key = `${roomId}:${userId}`;
    const participant = this.participants.get(key);
    if (!participant) return null;
    
    const user = this.users.get(userId);
    if (!user) return null;
    
    return {
      ...participant,
      username: user.username
    };
  }

  // Session methods
  createSession(session: Session): Session {
    const userSessions = this.sessions.get(session.userId) || [];
    userSessions.push(session);
    this.sessions.set(session.userId, userSessions);
    return session;
  }

  getUserSessions(userId: string): Session[] {
    return this.sessions.get(userId) || [];
  }

  // Chat methods
  addMessage(message: ChatMessage): ChatMessage {
    const roomMessages = this.messages.get(message.roomId) || [];
    roomMessages.push(message);
    this.messages.set(message.roomId, roomMessages);
    return message;
  }

  getRoomMessages(roomId: string, limit: number = 50): ChatMessage[] {
    const messages = this.messages.get(roomId) || [];
    return messages.slice(-limit);
  }

  // Stats methods
  getUserStats(userId: string) {
    const user = this.users.get(userId);
    if (!user) return null;
    
    const sessions = this.getUserSessions(userId);
    const today = new Date().toDateString();
    const todaySessions = sessions.filter(s => 
      s.endTime && new Date(s.endTime).toDateString() === today
    );
    
    return {
      totalFocusTime: user.totalFocusTime,
      todayFocusTime: todaySessions.reduce((sum, s) => sum + s.duration, 0),
      currentStreak: user.currentStreak,
      longestStreak: user.longestStreak,
      points: user.points,
      sessionsCompleted: sessions.filter(s => s.completed).length,
      todaySessionsCompleted: todaySessions.filter(s => s.completed).length
    };
  }

  getLeaderboard(period: 'daily' | 'weekly' | 'all' = 'all', limit: number = 10): any[] {
    const users = Array.from(this.users.values());
    const now = Date.now();
    
    return users
      .map(user => {
        let relevantTime = 0;
        const sessions = this.getUserSessions(user.id);
        
        if (period === 'all') {
          relevantTime = user.totalFocusTime;
        } else {
          const cutoff = period === 'daily' 
            ? now - 24 * 60 * 60 * 1000
            : now - 7 * 24 * 60 * 60 * 1000;
          
          relevantTime = sessions
            .filter(s => s.endTime && s.endTime > cutoff)
            .reduce((sum, s) => sum + s.duration, 0);
        }
        
        return {
          userId: user.id,
          username: user.username,
          avatar: user.avatar,
          focusTime: relevantTime,
          points: user.points,
          streak: user.currentStreak
        };
      })
      .sort((a, b) => b.focusTime - a.focusTime)
      .slice(0, limit);
  }

  // Clear all data (for testing)
  clear(): void {
    this.users.clear();
    this.rooms.clear();
    this.sessions.clear();
    this.participants.clear();
    this.messages.clear();
    this.emailIndex.clear();
  }
}

// Export singleton instance
export const dataStore = new DataStore();