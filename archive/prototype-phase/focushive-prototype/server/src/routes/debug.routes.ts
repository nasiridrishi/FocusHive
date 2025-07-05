/**
 * Debug routes to check room and participant data
 */

import { Router } from 'express';
import { dataStore } from '../data/store';
import { presenceService } from '../services/presenceService';

const router = Router();

// Debug route to check room participants
router.get('/room/:roomId/debug', (req, res) => {
  const roomId = req.params.roomId;
  
  const room = dataStore.getRoom(roomId);
  const presence = presenceService.getRoomParticipants(roomId);
  
  if (!room) {
    return res.status(404).json({ error: 'Room not found' });
  }
  
  // Get user details for each participant
  const participantDetails = room.participants.map(userId => {
    const user = dataStore.getUser(userId);
    const userPresence = presence.find(p => p.userId === userId);
    return {
      userId,
      username: user?.username || 'Unknown',
      isDummy: userId.startsWith('dummy-'),
      hasPresence: !!userPresence,
      presence: userPresence
    };
  });
  
  res.json({
    room: {
      id: room.id,
      name: room.name,
      participantCount: room.participants.length,
      participants: room.participants
    },
    participantDetails,
    presenceCount: presence.length,
    presence
  });
});

// Debug route to list all rooms
router.get('/rooms/debug', (req, res) => {
  const rooms = dataStore.getAllRooms();
  const roomSummary = rooms.map(room => ({
    id: room.id,
    name: room.name,
    isDummy: room.id.startsWith('dummy-'),
    participantCount: room.participants.length,
    participants: room.participants,
    presenceCount: presenceService.getRoomParticipants(room.id).length
  }));
  
  res.json({ totalRooms: rooms.length, rooms: roomSummary });
});

export const debugRoutes = router;