# Screenshot Guide for FocusHive Preliminary Report

## Prerequisites
1. Make sure the demo mode is running with dummy data:
   ```bash
   ./demo-mode.sh
   ```
2. Login with demo user: demo@focushive.com / demo123

## Screenshots to Capture

### 1. Dashboard View (for Section 3.3)
- Navigate to Dashboard
- Ensure multiple dummy rooms are visible with participants
- Show both study and work-focused rooms
- Capture showing the room grid with participant counts
- Save as: `screenshot-1-dashboard.png`

### 2. Focus Room with Presence Indicators (for Section 3.5)
- Join a populated dummy room (preferably with 4-6 participants)
- Make sure different status indicators are visible (focusing, studying, on break)
- Show the participant cards with their status colors
- Save as: `screenshot-2-presence-indicators.png`

### 3. Adaptive UI Comparison (for Section 4.2)
- Take two screenshots:
  a. Normal room view with all UI elements visible
  b. Minimalist/focus mode with collapsed sidebar and hidden chat
- You may need to use the toggle buttons to show both states
- Save as: `screenshot-3a-normal-ui.png` and `screenshot-3b-focus-ui.png`

### 4. Synchronized Pomodoro Timer (for Section 4.2)
- Join a room where a Pomodoro session is active
- Show the floating timer with session progress
- If possible, show multiple participant cards indicating they're in the same session
- Save as: `screenshot-4-pomodoro-timer.png`

### 5. Leaderboard and Gamification (for Section 4.3)
- Navigate to the Leaderboard page
- Show the daily/weekly view with dummy users
- Make sure points, focus time, and streaks are visible
- Also capture any achievement badges if visible
- Save as: `screenshot-5-leaderboard.png`

### 6. Forum/Community Features (for Section 4.3)
- Navigate to Forums
- Show posts from dummy users
- Include both study-related and work-related discussions
- If possible, show the global chat panel as well
- Save as: `screenshot-6-forum-community.png`

## Tips for Better Screenshots

1. **Use consistent window size**: 1280x800 or 1440x900 for uniformity
2. **Theme**: Use light mode for better print quality in the report
3. **Clear content**: Ensure dummy user names and content are appropriate
4. **Remove personal info**: Hide any real browser bookmarks or tabs
5. **High quality**: Save as PNG for better quality

## Adding to Report

After capturing, add to the report at the specified locations with:

```markdown
![Figure X: Description](./screenshots/screenshot-name.png)
*Figure X: Detailed caption as specified in the guide*
```

## Optional Enhancement Screenshots

If space permits, consider also capturing:
- Buddy matching interface
- Achievement unlock notification
- Dark mode comparison
- Mobile responsive view