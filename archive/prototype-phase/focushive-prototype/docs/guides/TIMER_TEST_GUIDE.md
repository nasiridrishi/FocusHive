# Timer Functionality Test Guide

## Prerequisites
- Server running on http://localhost:3000
- Client running on http://localhost:5173

## Test Steps

### 1. Setup Test Users
1. Open http://localhost:5173 in Browser Window 1
2. Register a new user:
   - Email: user1@test.com
   - Username: User1
   - Password: password123
3. Open http://localhost:5173 in Browser Window 2 (or incognito)
4. Register another user:
   - Email: user2@test.com
   - Username: User2
   - Password: password123

### 2. Create and Join Room
1. In Window 1 (User1):
   - Click "Create Room"
   - Room Name: "Timer Test Room"
   - Type: Public
   - Focus Type: Deep Work
   - Create the room
   - Click "Enter Room"

2. In Window 2 (User2):
   - You should see "Timer Test Room" in available rooms
   - Click "Join Room"
   - Click "Enter Room"

### 3. Test Timer Synchronization
1. **Start Timer** (Window 1):
   - Click "Start Timer" button
   - Verify timer starts counting down from 25:00
   - Check that running animation (pulsing dots) appears
   - Verify Window 2 shows the same timer state

2. **Pause Timer** (Window 2):
   - Click "Pause" button
   - Verify timer stops in both windows
   - Check that running animation disappears

3. **Resume Timer** (Either window):
   - Click "Resume" button
   - Verify timer continues from where it paused
   - Check synchronization in both windows

4. **Skip Phase** (Either window):
   - Click "Skip Phase" button
   - Confirm the dialog
   - Verify timer switches to "Short Break" (5:00)
   - Check phase indicator shows "Short Break" in green

5. **Reset Timer** (Either window):
   - Start the timer first
   - Click "Reset" button
   - Confirm the dialog
   - Verify timer returns to 25:00 and "idle" state

### 4. Test Late Joiner Sync
1. Start timer in existing room
2. Open Window 3 and login as user3@test.com
3. Join the same room
4. Verify new user sees current timer state

### 5. Test Phase Progression
1. In the room, look for session indicators (dots at bottom)
2. Complete a work session (or skip)
3. Verify it goes to Short Break (5:00)
4. Complete 3 more work sessions
5. Verify 4th completion goes to Long Break (15:00)
6. Check session dots reset after long break

### 6. Test Browser Notifications
1. When timer completes a phase, you should get a browser notification
2. If not, check browser permissions for notifications

## Expected Behaviors

### Timer States
- **Idle**: Shows time, "Start Timer" button available
- **Running**: Countdown active, shows "Pause" button
- **Paused**: Time frozen, shows "Resume" button

### Phase Colors
- **Work (Focus Time)**: Indigo badge
- **Short Break**: Green badge
- **Long Break**: Blue badge

### Controls Available
- During any active state: Reset, Skip Phase
- Only room participants can control timer
- All participants see synchronized updates

## Things to Verify
- [ ] Timer counts down accurately (1 second intervals)
- [ ] All users see the same timer state
- [ ] Phase transitions work correctly
- [ ] Session counting works (dots at bottom)
- [ ] Late joiners see current state
- [ ] Browser notifications work on phase complete
- [ ] Timer continues even if users leave/rejoin
- [ ] Controls are responsive and immediate

## Known Limitations (Prototype)
- Timer state is lost on server restart
- No persistence between sessions
- No custom duration settings
- No sound notifications (only browser notifications)