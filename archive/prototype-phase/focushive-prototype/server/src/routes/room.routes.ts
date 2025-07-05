import { Router, Request, Response } from 'express';
import { authenticate as authMiddleware } from '../middleware/auth';
import { RoomService } from '../services/roomService';
import { Room } from '@focushive/shared';

const router = Router();
const roomService = new RoomService();

// Get all public rooms (no auth required)
router.get('/', async (req: Request, res: Response) => {
  try {
    const rooms = await roomService.getPublicRooms();
    // Remove passwords from response
    const sanitizedRooms = rooms.map(room => {
      const { password, ...roomWithoutPassword } = room;
      return roomWithoutPassword;
    });
    res.json({ rooms: sanitizedRooms });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch rooms' });
  }
});

// Get user's rooms (auth required)
router.get('/my-rooms', authMiddleware, async (req: Request, res: Response) => {
  try {
    const userId = (req as any).user.id;
    const rooms = await roomService.getUserRooms(userId);
    // Remove passwords from response
    const sanitizedRooms = rooms.map(room => {
      const { password, ...roomWithoutPassword } = room;
      return roomWithoutPassword;
    });
    res.json({ rooms: sanitizedRooms });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch user rooms' });
  }
});

// Get room details (no auth required for public rooms)
router.get('/:id', async (req: Request, res: Response) => {
  try {
    const room = await roomService.getRoom(req.params.id);
    if (!room) {
      return res.status(404).json({ error: 'Room not found' });
    }
    // Remove password from response
    const { password, ...roomWithoutPassword } = room;
    res.json({ room: roomWithoutPassword });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch room' });
  }
});

// Create a new room (auth required)
router.post('/', authMiddleware, async (req: Request, res: Response) => {
  try {
    const userId = (req as any).user.id;
    const { name, description, type, password, maxParticipants, tags, focusType } = req.body;

    // Validate required fields
    if (!name || !type) {
      return res.status(400).json({ error: 'Name and type are required' });
    }

    const room = await roomService.createRoom(userId, {
      name,
      description,
      type,
      password,
      maxParticipants,
      tags,
      focusType
    });

    // Remove password from response
    const { password: pwd, ...roomWithoutPassword } = room;
    res.status(201).json({ room: roomWithoutPassword });
  } catch (error: any) {
    if (error.message.includes('Room name')) {
      res.status(400).json({ error: error.message });
    } else {
      res.status(500).json({ error: 'Failed to create room' });
    }
  }
});

// Join a room (auth required)
router.post('/:id/join', authMiddleware, async (req: Request, res: Response) => {
  try {
    const userId = (req as any).user.id;
    const roomId = req.params.id;
    const { password } = req.body;

    const result = await roomService.joinRoom(userId, roomId, password);
    
    // Remove password from response
    const { password: pwd, ...roomWithoutPassword } = result.room;
    res.json({ room: roomWithoutPassword, message: 'Successfully joined room' });
  } catch (error: any) {
    if (error.message.includes('not found')) {
      res.status(404).json({ error: error.message });
    } else if (error.message.includes('Password') || error.message.includes('password') || error.message.includes('full') || error.message.includes('already')) {
      res.status(400).json({ error: error.message });
    } else {
      res.status(500).json({ error: 'Failed to join room' });
    }
  }
});

// Leave a room (auth required)
router.post('/:id/leave', authMiddleware, async (req: Request, res: Response) => {
  try {
    const userId = (req as any).user.id;
    const roomId = req.params.id;

    const result = await roomService.leaveRoom(userId, roomId);
    
    // Remove password from response
    const { password: pwd, ...roomWithoutPassword } = result.room;
    res.json({ room: roomWithoutPassword, message: 'Successfully left room' });
  } catch (error: any) {
    if (error.message.includes('not found')) {
      res.status(404).json({ error: error.message });
    } else if (error.message.includes('owner')) {
      res.status(400).json({ error: error.message });
    } else {
      res.status(500).json({ error: 'Failed to leave room' });
    }
  }
});

// Update a room (auth required, owner only)
router.put('/:id', authMiddleware, async (req: Request, res: Response) => {
  try {
    const userId = (req as any).user.id;
    const roomId = req.params.id;
    const updates = req.body;

    const room = await roomService.updateRoom(userId, roomId, updates);
    
    // Remove password from response
    const { password: pwd, ...roomWithoutPassword } = room;
    res.json({ room: roomWithoutPassword });
  } catch (error: any) {
    if (error.message.includes('not found')) {
      res.status(404).json({ error: error.message });
    } else if (error.message.includes('owner')) {
      res.status(403).json({ error: error.message });
    } else {
      res.status(500).json({ error: 'Failed to update room' });
    }
  }
});

// Delete a room (auth required, owner only)
router.delete('/:id', authMiddleware, async (req: Request, res: Response) => {
  try {
    const userId = (req as any).user.id;
    const roomId = req.params.id;

    const result = await roomService.deleteRoom(userId, roomId);
    res.json({ message: 'Room deleted successfully', success: result.success });
  } catch (error: any) {
    if (error.message.includes('not found')) {
      res.status(404).json({ error: error.message });
    } else if (error.message.includes('owner')) {
      res.status(403).json({ error: error.message });
    } else {
      res.status(500).json({ error: 'Failed to delete room' });
    }
  }
});

export { router as roomRoutes };