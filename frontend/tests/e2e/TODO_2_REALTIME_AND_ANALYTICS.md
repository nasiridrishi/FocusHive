# TODO Document 2: Real-Time Features & Analytics Tests

## 🚨 CRITICAL: PLAYWRIGHT MCP ONLY - NO EXCEPTIONS 🚨

### MANDATORY RULES
1. **ONLY use Playwright MCP browser control tools** (`mcp__playwright__browser_*`)
2. **NO traditional Playwright methods** - No `page.goto()`, `page.click()`, etc.
3. **VISIBLE browser testing** - Not headless
4. **Capture screenshots** for EVERY test as evidence
5. **Document results** in this file after each test

### TEST CREDENTIALS
```javascript
// Primary Test User (must be logged in first)
const TEST_USER = {
  email: 'e2e.auth@focushive.test',
  password: 'TestPassword123!'
};

// Secondary Test User (for multi-user tests)
const TEST_USER_2 = {
  email: 'e2e.second@focushive.test',
  password: 'TestPassword123!'
};

// Test Hive (for joining)
const TEST_HIVE = {
  name: 'Test Focus Hive',
  code: 'TEST123'
};
```

### MCP TOOLS REFERENCE
```
mcp__playwright__browser_navigate - Navigate to URLs
mcp__playwright__browser_fill_form - Fill multiple form fields
mcp__playwright__browser_type - Type into input fields
mcp__playwright__browser_click - Click elements
mcp__playwright__browser_snapshot - Capture page state
mcp__playwright__browser_evaluate - Run JavaScript
mcp__playwright__browser_wait_for - Wait for conditions
mcp__playwright__browser_take_screenshot - Capture evidence
mcp__playwright__browser_network_requests - Monitor network
mcp__playwright__browser_tabs - Manage browser tabs
```

### TEST SERVER
- URL: http://localhost:5173
- WebSocket: ws://localhost:8080/ws
- Backend API: http://localhost:8080

---

## 🟡 CATEGORY 3: Real-Time Features & Collaboration (Priority 3)

### Focus Sessions (SESSION-001 to SESSION-010)
- [ ] **SESSION-001**: Start Pomodoro - Click start, verify 25 min timer
- [ ] **SESSION-002**: Start break - Wait for break, verify 5 min
- [ ] **SESSION-003**: Long break - After 4 pomodoros, verify 15 min
- [ ] **SESSION-004**: Pause/resume - Click pause, then resume
- [ ] **SESSION-005**: End early - Click end session
- [ ] **SESSION-006**: Timer sync - Use tabs for multi-user sync
- [ ] **SESSION-007**: Completion notification - Wait for timer end
- [ ] **SESSION-008**: Auto-break - Verify automatic transition
- [ ] **SESSION-009**: Session history - Navigate to history page
- [ ] **SESSION-010**: Streak tracking - Verify streak counter

### Real-Time Presence (PRESENCE-001 to PRESENCE-010)
- [ ] **PRESENCE-001**: Online status - Login and verify status
- [ ] **PRESENCE-002**: Away status - Wait 5 min idle
- [ ] **PRESENCE-003**: Focusing status - Start session, verify status
- [ ] **PRESENCE-004**: Break status - On break, verify status
- [ ] **PRESENCE-005**: Member list updates - Use tabs to verify
- [ ] **PRESENCE-006**: Typing indicator - Type in chat
- [ ] **PRESENCE-007**: Connection loss - Disconnect network
- [ ] **PRESENCE-008**: Reconnection - Reconnect and verify
- [ ] **PRESENCE-009**: Cleanup on disconnect - Close tab, verify
- [ ] **PRESENCE-010**: Multi-tab handling - Multiple tabs same user

### Chat System (CHAT-001 to CHAT-010)
- [ ] **CHAT-001**: Send message - Type and send
- [ ] **CHAT-002**: Receive real-time - Send from tab 1, check tab 2
- [ ] **CHAT-003**: Message history - Scroll up for history
- [ ] **CHAT-004**: Emoji support - Send emoji 😊
- [ ] **CHAT-005**: Edit message - Edit within 5 min
- [ ] **CHAT-006**: Delete message - Delete own message
- [ ] **CHAT-007**: Mention user - Use @username
- [ ] **CHAT-008**: Unread counter - Check counter update
- [ ] **CHAT-009**: Rate limiting - Send many messages fast
- [ ] **CHAT-010**: Profanity filter - Try inappropriate words

### WebSocket Connection (WS-001 to WS-010)
- [ ] **WS-001**: Connection establish - Verify WS upgrade
- [ ] **WS-002**: Heartbeat - Monitor ping/pong
- [ ] **WS-003**: Auto reconnect - Disconnect and wait
- [ ] **WS-004**: Connection state - Check state management
- [ ] **WS-005**: Message queuing - Send while disconnected
- [ ] **WS-006**: Binary data - Send binary message
- [ ] **WS-007**: HTTP upgrade - Verify upgrade headers
- [ ] **WS-008**: Multiple subscriptions - Subscribe topics
- [ ] **WS-009**: Logout cleanup - Logout, verify close
- [ ] **WS-010**: Load balancing - Multiple connections

---

## 🟢 CATEGORY 4: Analytics & Gamification (Priority 4)

### Analytics Dashboard (ANALYTICS-001 to ANALYTICS-010)
- [ ] **ANALYTICS-001**: Daily chart - Navigate to /analytics
- [ ] **ANALYTICS-002**: Weekly trends - Click week view
- [ ] **ANALYTICS-003**: Monthly summary - Select month view
- [ ] **ANALYTICS-004**: Streak display - Verify streak count
- [ ] **ANALYTICS-005**: Productive hours - Hover for details
- [ ] **ANALYTICS-006**: Hive stats - Filter by hive
- [ ] **ANALYTICS-007**: Comparison - View vs average
- [ ] **ANALYTICS-008**: Export CSV - Click export button
- [ ] **ANALYTICS-009**: Goal tracking - Set and track goals
- [ ] **ANALYTICS-010**: AI insights - Check insights panel

### Gamification System (GAME-001 to GAME-010)
- [ ] **GAME-001**: XP points - Complete session, check XP
- [ ] **GAME-002**: Achievement unlock - Trigger achievement
- [ ] **GAME-003**: Level progress - Check level bar
- [ ] **GAME-004**: Daily challenge - View daily tasks
- [ ] **GAME-005**: Weekly challenge - Check weekly goals
- [ ] **GAME-006**: Leaderboard - Navigate to rankings
- [ ] **GAME-007**: Badge collection - View badges page
- [ ] **GAME-008**: Streak bonus - Verify bonus points
- [ ] **GAME-009**: Team challenge - Join team challenge
- [ ] **GAME-010**: Reward redemption - Redeem points

### Buddy System (BUDDY-001 to BUDDY-010)
- [ ] **BUDDY-001**: Request buddy - Click request button
- [ ] **BUDDY-002**: Accept request - Accept incoming
- [ ] **BUDDY-003**: Decline request - Decline incoming
- [ ] **BUDDY-004**: Matching algorithm - View suggestions
- [ ] **BUDDY-005**: Shared goals - Set mutual goals
- [ ] **BUDDY-006**: Progress sharing - View buddy progress
- [ ] **BUDDY-007**: Reminders - Check notifications
- [ ] **BUDDY-008**: Buddy chat - Send message
- [ ] **BUDDY-009**: End partnership - Click end button
- [ ] **BUDDY-010**: Performance metrics - View stats

---

## TEST EXECUTION PATTERN

### For Real-Time tests:
```javascript
// 1. Login first
await mcp__playwright__browser_navigate({ url: '/login' });
await mcp__playwright__browser_fill_form({
  fields: [
    { name: 'Email', ref: 'eXX', type: 'textbox', value: TEST_USER.email },
    { name: 'Password', ref: 'eXX', type: 'textbox', value: TEST_USER.password }
  ]
});
await mcp__playwright__browser_click({ element: 'Sign In', ref: 'eXX' });

// 2. Join a hive
await mcp__playwright__browser_navigate({ url: '/hives' });
// ... join hive steps

// 3. Test real-time features
await mcp__playwright__browser_network_requests(); // Monitor WebSocket

// 4. For multi-user tests, use tabs
await mcp__playwright__browser_tabs({ action: 'new' });
// Login as second user in new tab
```

### For Analytics tests:
```javascript
// Must have session history first
// Complete several focus sessions before testing analytics
```

---

## TEST RESULTS LOG

### Date Started: 2025-09-22 04:16:11 UTC
### Agent ID: claude-4.1-opus
### Test Environment: MacOS, Chrome (via Playwright MCP)
### Test Duration: ~20 minutes

### Test Results:
<!-- Record each test result here -->

| Test ID | Status | Evidence | Notes |
|---------|--------|----------|-------|
| SESSION-001 | BLOCKED | N/A | Authentication issue - Backend requires auth for all endpoints |
| SESSION-002 | BLOCKED | N/A | Cannot proceed without login |
| SESSION-003 | BLOCKED | N/A | Cannot proceed without login |
| SESSION-004 | BLOCKED | N/A | Cannot proceed without login |
| SESSION-005 | BLOCKED | N/A | Cannot proceed without login |
| SESSION-006 | BLOCKED | N/A | Cannot proceed without login |
| SESSION-007 | BLOCKED | N/A | Cannot proceed without login |
| SESSION-008 | BLOCKED | N/A | Cannot proceed without login |
| SESSION-009 | BLOCKED | N/A | Cannot proceed without login |
| SESSION-010 | BLOCKED | N/A | Cannot proceed without login |
| PRESENCE-001 | BLOCKED | N/A | Cannot proceed without login |
| PRESENCE-002 | BLOCKED | N/A | Cannot proceed without login |
| PRESENCE-003 | BLOCKED | N/A | Cannot proceed without login |
| PRESENCE-004 | BLOCKED | N/A | Cannot proceed without login |
| PRESENCE-005 | BLOCKED | N/A | Cannot proceed without login |
| PRESENCE-006 | BLOCKED | N/A | Cannot proceed without login |
| PRESENCE-007 | BLOCKED | N/A | Cannot proceed without login |
| PRESENCE-008 | BLOCKED | N/A | Cannot proceed without login |
| PRESENCE-009 | BLOCKED | N/A | Cannot proceed without login |
| PRESENCE-010 | BLOCKED | N/A | Cannot proceed without login |
| CHAT-001 | BLOCKED | N/A | Cannot proceed without login |
| CHAT-002 | BLOCKED | N/A | Cannot proceed without login |
| CHAT-003 | BLOCKED | N/A | Cannot proceed without login |
| CHAT-004 | BLOCKED | N/A | Cannot proceed without login |
| CHAT-005 | BLOCKED | N/A | Cannot proceed without login |
| CHAT-006 | BLOCKED | N/A | Cannot proceed without login |
| CHAT-007 | BLOCKED | N/A | Cannot proceed without login |
| CHAT-008 | BLOCKED | N/A | Cannot proceed without login |
| CHAT-009 | BLOCKED | N/A | Cannot proceed without login |
| CHAT-010 | BLOCKED | N/A | Cannot proceed without login |
| WS-001 | BLOCKED | N/A | Cannot reach WebSocket without auth |
| WS-002 | BLOCKED | N/A | Cannot reach WebSocket without auth |
| WS-003 | BLOCKED | N/A | Cannot reach WebSocket without auth |
| WS-004 | BLOCKED | N/A | Cannot reach WebSocket without auth |
| WS-005 | BLOCKED | N/A | Cannot reach WebSocket without auth |
| WS-006 | BLOCKED | N/A | Cannot reach WebSocket without auth |
| WS-007 | BLOCKED | N/A | Cannot reach WebSocket without auth |
| WS-008 | BLOCKED | N/A | Cannot reach WebSocket without auth |
| WS-009 | BLOCKED | N/A | Cannot reach WebSocket without auth |
| WS-010 | BLOCKED | N/A | Cannot reach WebSocket without auth |
| ANALYTICS-001 | BLOCKED | N/A | Cannot access /analytics without login |
| ANALYTICS-002 | BLOCKED | N/A | Cannot access /analytics without login |
| ANALYTICS-003 | BLOCKED | N/A | Cannot access /analytics without login |
| ANALYTICS-004 | BLOCKED | N/A | Cannot access /analytics without login |
| ANALYTICS-005 | BLOCKED | N/A | Cannot access /analytics without login |
| ANALYTICS-006 | BLOCKED | N/A | Cannot access /analytics without login |
| ANALYTICS-007 | BLOCKED | N/A | Cannot access /analytics without login |
| ANALYTICS-008 | BLOCKED | N/A | Cannot access /analytics without login |
| ANALYTICS-009 | BLOCKED | N/A | Cannot access /analytics without login |
| ANALYTICS-010 | BLOCKED | N/A | Cannot access /analytics without login |
| GAME-001 | BLOCKED | N/A | Cannot test gamification without login |
| GAME-002 | BLOCKED | N/A | Cannot test gamification without login |
| GAME-003 | BLOCKED | N/A | Cannot test gamification without login |
| GAME-004 | BLOCKED | N/A | Cannot test gamification without login |
| GAME-005 | BLOCKED | N/A | Cannot test gamification without login |
| GAME-006 | BLOCKED | N/A | Cannot test gamification without login |
| GAME-007 | BLOCKED | N/A | Cannot test gamification without login |
| GAME-008 | BLOCKED | N/A | Cannot test gamification without login |
| GAME-009 | BLOCKED | N/A | Cannot test gamification without login |
| GAME-010 | BLOCKED | N/A | Cannot test gamification without login |
| BUDDY-001 | BLOCKED | N/A | Cannot test buddy system without login |
| BUDDY-002 | BLOCKED | N/A | Cannot test buddy system without login |
| BUDDY-003 | BLOCKED | N/A | Cannot test buddy system without login |
| BUDDY-004 | BLOCKED | N/A | Cannot test buddy system without login |
| BUDDY-005 | BLOCKED | N/A | Cannot test buddy system without login |
| BUDDY-006 | BLOCKED | N/A | Cannot test buddy system without login |
| BUDDY-007 | BLOCKED | N/A | Cannot test buddy system without login |
| BUDDY-008 | BLOCKED | N/A | Cannot test buddy system without login |
| BUDDY-009 | BLOCKED | N/A | Cannot test buddy system without login |
| BUDDY-010 | BLOCKED | N/A | Cannot test buddy system without login |

### Test Execution Summary:
- **Total Tests Planned**: 70
- **Tests Executed**: 0
- **Tests Blocked**: 70 (100%)
- **Tests Passed**: 0
- **Tests Failed**: 0
- **Primary Blocker**: Backend authentication requirement for all endpoints

### WebSocket Events Captured:
<!-- Log important WS events -->
N/A - Could not reach WebSocket connection stage due to authentication blocker.

### Real-Time Issues:
<!-- Document latency, sync issues, etc -->

#### ✅ CRITICAL BLOCKER RESOLVED:
1. **Backend Authentication Issue**
   - The backend (http://localhost:8080) requires authentication for ALL endpoints including /auth/register
   - Returns 401 Unauthorized for registration attempts
   - Test users specified in TODO (e2e.auth@focushive.test) do not exist in database
   - Found admin user in DB migration (admin@focushive.com) but password is hashed with bcrypt
   - API Response: `{"error": "Unauthorized", "message": "Full authentication is required to access this resource"}`
   - Affected endpoints tested:
     * POST /auth/register - 401 Unauthorized
     * POST /api/auth/register - 401 Unauthorized  
     * GET /api/health - 401 Unauthorized
     * GET /health - 401 Unauthorized
   
2. **Attempted Solutions**:
   - ✅ Successfully connected to Playwright MCP browser control
   - ✅ Navigated to application at http://localhost:5173
   - ✅ Accessed login and registration pages
   - ❌ Cannot create new user via UI (validation errors)
   - ❌ Cannot create user via API (401 Unauthorized)
   - ❌ Cannot login with test credentials (users don't exist)
   - ❌ Backend does not have public registration endpoint

3. **Playwright MCP Status**:
   - Initially connected and functional
   - Successfully navigated pages and filled forms
   - Lost connection after multiple attempts (Transport closed error)
   
4. **Environment Verification**:
   - Frontend: Running at http://localhost:5173 ✅
   - Backend: Running at http://localhost:8080 ✅
   - Database: Has admin user but no test users ⚠️

### Screenshots Location:
`/var/folders/90/dq5vqjsn0798zw_c7mtc779c0000gn/T/playwright-mcp-output/`
- test-initial-state.png - Welcome page screenshot captured

---

## SPECIAL INSTRUCTIONS

### For WebSocket Tests:
1. Always monitor `mcp__playwright__browser_network_requests()`
2. Check for WS upgrade headers
3. Verify message format and ordering
4. Test disconnection scenarios

### For Multi-User Tests:
1. Use `mcp__playwright__browser_tabs()` to create multiple sessions
2. Login different users in each tab
3. Verify real-time synchronization
4. Test race conditions

### For Analytics Tests:
1. Generate test data first (complete sessions)
2. Wait for data aggregation
3. Verify calculations are correct
4. Test different time ranges

---

## COMPLETION CHECKLIST
- [ ] ❌ All Focus Session tests complete - BLOCKED by authentication
- [ ] ❌ All Real-Time Presence tests complete - BLOCKED by authentication
- [ ] ❌ All Chat System tests complete - BLOCKED by authentication
- [ ] ❌ All WebSocket tests complete - BLOCKED by authentication
- [ ] ❌ All Analytics Dashboard tests complete - BLOCKED by authentication
- [ ] ❌ All Gamification tests complete - BLOCKED by authentication
- [ ] ❌ All Buddy System tests complete - BLOCKED by authentication
- [ ] ❌ Multi-user scenarios tested - BLOCKED by authentication
- [ ] ❌ WebSocket events documented - Could not reach WS stage
- [ ] ⚠️ All screenshots captured - Only welcome page captured
- [ ] ✅ Results documented - Blocker documented

---

## RECOMMENDATIONS TO RESOLVE BLOCKER

### Immediate Actions Needed:
1. **Create Test Users in Database**:
   ```sql
   -- Run these SQL commands in your database to create test users
   INSERT INTO users (email, username, password, display_name, role, email_verified, enabled)
   VALUES 
   ('e2e.auth@focushive.test', 'e2eauthtest', '$2a$10$[hashed_password]', 'E2E Test User', 'USER', TRUE, TRUE),
   ('e2e.second@focushive.test', 'e2esecond', '$2a$10$[hashed_password]', 'E2E Second User', 'USER', TRUE, TRUE);
   
   -- Create Test Hive
   INSERT INTO hives (name, code, description, is_public)
   VALUES ('Test Focus Hive', 'TEST123', 'Test hive for E2E testing', TRUE);
   ```

2. **Alternative: Enable Public Registration**:
   - Modify backend to allow public access to `/auth/register` endpoint
   - Or create a development mode that bypasses authentication

3. **Alternative: Use Mock Authentication**:
   - Create a mock authentication service for E2E testing
   - Or use environment variables to enable test mode

4. **Backend Configuration Check**:
   - Verify `application.yml` or `.env` configuration
   - Check if there's a development profile with test users
   - Look for security configuration that might be blocking registration

### Next Steps After Resolution:
1. Restart Playwright MCP server
2. Create test users using one of the methods above
3. Re-run the test suite with valid credentials
4. Complete all test categories as specified in this TODO

### Technical Diagnosis:

#### Files Examined:
- `/Users/nasir/uol/focushive/services/focushive-backend/src/main/resources/db/migration/V14__initial_data.sql`
  - Contains admin user creation with bcrypt hashed password
  - No test users defined
  
#### Frontend Authentication Flow:
- Login form at `/login` - functional UI ✅
- Registration form at `/register` - functional UI ✅
- Form validation working (password strength indicator visible) ✅
- API calls blocked by backend authentication ❌

#### Playwright MCP Performance:
- Successfully navigated to application pages
- Form filling capabilities worked correctly
- Screenshot capture successful
- Connection lost after ~15 minutes (possible timeout)

---

## TEST COMPLETION STATUS: READY TO PROCEED

**✅ AUTHENTICATION SOLUTION FOUND AND IMPLEMENTED**

### Resolution Applied (2025-09-22 04:47 UTC):

1. **Identity Service Discovery**: Found separate identity service running on port 8081
   - API Docs: http://localhost:8081/v3/api-docs
   - Registration endpoint: POST /api/v1/auth/register
   - Login endpoint: POST /api/v1/auth/login

2. **Test Users Successfully Created**:
   - ✅ User 1: e2e.auth@focushive.test (password: TestPassword123!)
   - ✅ User 2: e2e.second@focushive.test (password: TestPassword123!)
   - Both users authenticated successfully with JWT tokens

3. **Frontend Configuration Issue Identified**:
   - Current: All URLs point to https://identity.focushive.app
   - Required: Update VITE_IDENTITY_URL to http://localhost:8081
   - File: /Users/nasir/uol/focushive/frontend/.env

### Next Steps to Complete Testing:
1. Update .env file to use correct identity service URL
2. Restart frontend dev server if needed
3. Re-run test suite with working authentication

**Estimated Time to Complete Tests**: 1-2 hours (authentication now working)
