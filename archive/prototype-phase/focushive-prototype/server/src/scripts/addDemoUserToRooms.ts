/**
 * Adds demo user to a dummy room for easy testing
 */

import { dataStore } from '../data/store';

export function addDemoUserToRooms(): void {
  const demoUserId = 'demo-user-001';
  const demoUser = dataStore.getUser(demoUserId);
  
  if (!demoUser) {
    console.log('⚠️ Demo user not found');
    return;
  }
  
  // Find a dummy room with space
  const dummyRooms = dataStore.getAllRooms().filter(room => 
    room.id.startsWith('dummy-room-') && 
    room.participants.length < room.maxParticipants
  );
  
  if (dummyRooms.length === 0) {
    console.log('⚠️ No dummy rooms available');
    return;
  }
  
  // Add demo user to first available room (Deep Work Zone)
  const targetRoom = dummyRooms.find(r => r.name.includes('Deep Work')) || dummyRooms[0];
  
  if (!targetRoom.participants.includes(demoUserId)) {
    targetRoom.participants.push(demoUserId);
    dataStore.updateRoom(targetRoom.id, { participants: targetRoom.participants });
    console.log(`✅ Added demo user to room: ${targetRoom.name}`);
  }
}