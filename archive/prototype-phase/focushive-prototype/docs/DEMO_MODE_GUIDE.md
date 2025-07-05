# Demo Mode Guide

## Overview
Demo mode populates FocusHive with realistic dummy data to showcase the platform's features, perfect for screenshots and demonstrations.

## 🚀 Quick Start

### Enable Demo Mode
```bash
./demo-mode.sh --demo
```

Or set the environment variable:
```bash
cd server
DEMO_MODE=true npm run dev
```

## 📊 What Demo Mode Includes

### 16 Diverse Users
- **8 Students**: Medical, CS, Engineering, Math, Psychology, Physics, Literature, Business
- **8 Remote Workers**: Software Dev, UX Designer, Content Writer, Data Analyst, PM, Graphic Designer, Consultant, Project Manager

### 12 Active Rooms
**Study Rooms:**
- 📚 CS Finals Study Group
- 🏥 Med School Study Hive  
- 📐 Engineering Problem Sets
- 📝 Essay Writing Sprint
- 🧮 Calculus III Marathon
- 🔬 Chemistry Lab Reports

**Work Rooms:**
- 💻 Deep Work Zone
- 🎨 Design Sprint
- 📊 Data Analysis Cave
- ✍️ Writers' Room
- 🚀 Startup Hustle
- 🌐 Remote Team Sync

### Forum Activity
- 8 realistic posts seeking study partners or work accountability
- Mix of study-related (MCAT prep, algorithms) and work-related (deep work, freelance) posts
- Posts from the last 3 days with varying engagement

### Gamification Data
- Users with realistic points (500-5500)
- Streak data (0-50 days)
- Random achievements assigned
- Populated leaderboard

### Simulated Activity
- Users joining/leaving rooms every 30 seconds
- Chat messages every 45 seconds
- Status changes every 20 seconds

## 📸 Perfect for Screenshots

### Dashboard View
Shows mixed work/study rooms with participants

### Active Hive View
Multiple users with different statuses:
- 🟢 Focusing/Studying
- 🟡 In Break
- 🟣 In Discussion
- 🟠 Stressed (pulsing indicator)

### Forum View
Recent posts from both students and workers

### Leaderboard
Realistic point distribution and streaks

### Chat
Break-time messages showing community interaction

## 🧹 Cleanup

Demo data is automatically cleaned when the server restarts without DEMO_MODE=true.

To manually clean:
```javascript
// In server console
cleanupDummyData()
```

## 💡 Tips for Screenshots

1. **Wait 30 seconds** after starting for full population
2. **Join different rooms** to see various user types
3. **Check forum** for diverse post types
4. **Open chat** during breaks for activity
5. **View leaderboard** to show gamification

## 🔧 Customization

Edit `/server/src/scripts/dummyDataGenerator.ts` to:
- Add more users
- Create different room types
- Modify activity patterns
- Change message content

## ⚠️ Important Notes

- Demo mode is for development/screenshots only
- Do NOT use in production
- Data is not persistent between restarts
- Dummy users cannot actually log in (password: demo123)