import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import { createServer } from 'http';
import { Server } from 'socket.io';

// Load environment variables
dotenv.config();

// Import routes
import authRoutes from './routes/auth.routes';
import { roomRoutes } from './routes/room.routes';
import testRoutes from './routes/test.routes';

// Import socket handlers
import { setupSocketAuth } from './sockets/auth';
import { setupRoomSockets } from './sockets/roomSocket';
import { setupTimerSockets } from './sockets/timerSocket';
import { setupGamificationSockets } from './sockets/gamificationSocket';
import { setupChatSockets } from './sockets/chatSocket';
import { setupBuddySockets } from './sockets/buddySocket';
import { setupForumSockets } from './sockets/forumSocket';

// Create Express app
const app = express();
const httpServer = createServer(app);

// Initialize Socket.io
const io = new Server(httpServer, {
  cors: {
    origin: process.env.CLIENT_URL || 'http://localhost:5173',
    credentials: true
  }
});

// Middleware
app.use(cors({
  origin: process.env.CLIENT_URL || 'http://localhost:5173',
  credentials: true
}));
app.use(express.json());

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/rooms', roomRoutes);
app.use('/api/test', testRoutes);

// Debug routes (only in development)
if (process.env.NODE_ENV === 'development') {
  const { debugRoutes } = require('./routes/debug.routes');
  app.use('/api/debug', debugRoutes);
}

// Setup socket handlers
setupSocketAuth(io);
setupRoomSockets(io);
setupTimerSockets(io);
setupGamificationSockets(io);
setupChatSockets(io);
setupBuddySockets(io);
setupForumSockets(io);

// Initialize socket service for programmatic access
const { socketService } = require('./services/socketService');
socketService.setIo(io);

// Enhance room sockets for dummy participants
const { enhanceRoomSocketsForDummy } = require('./sockets/roomSocketEnhanced');
enhanceRoomSocketsForDummy(io);

// Error handling middleware
app.use((err: Error, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.error('Error:', err);
  res.status(500).json({ error: 'Internal server error' });
});

// Start server
const PORT = process.env.PORT || 3000;
httpServer.listen(Number(PORT), '127.0.0.1', async () => {
  console.log(`🚀 FocusHive server running on http://localhost:${PORT}`);
  console.log('Server is listening:', httpServer.listening);
  console.log('Address:', httpServer.address());
  
  // Initialize dummy data if in demo mode
  if (process.env.DEMO_MODE === 'true' || process.env.NODE_ENV === 'development') {
    // Create demo user first
    const { createDemoUser } = await import('./scripts/createDemoUser');
    await createDemoUser();
    
    const { DummyDataGenerator } = await import('./scripts/dummyDataGenerator');
    const dummyDataGenerator = DummyDataGenerator.getInstance();
    await dummyDataGenerator.initializeDummyData();
    console.log('📸 Demo mode enabled - dummy data loaded');
    
    // Also initialize enhanced dummy data for rooms with participants
    const { enhancedDummyData } = await import('./scripts/enhancedDummyData');
    await enhancedDummyData.initialize();
    console.log('🎯 Enhanced dummy data with active rooms loaded');
    
    // Simulate socket connections for dummy users
    const { socketSimulator } = await import('./scripts/simulateSocketConnections');
    await socketSimulator.simulateDummyConnections();
    socketSimulator.startActivitySimulation();
    
    // Add demo user to a room for easy testing
    const { addDemoUserToRooms } = await import('./scripts/addDemoUserToRooms');
    addDemoUserToRooms();
    
    // Populate leaderboard with dummy user sessions
    const { populateLeaderboard } = await import('./scripts/populateLeaderboard');
    populateLeaderboard();
  }
});

// Add error handling
httpServer.on('error', (error) => {
  console.error('Server error:', error);
  process.exit(1);
});