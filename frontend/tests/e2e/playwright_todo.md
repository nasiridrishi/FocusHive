# FocusHive Comprehensive Playwright MCP E2E Testing Todo List

## 🚨 CRITICAL MANDATE: PLAYWRIGHT MCP ONLY 🚨

**ALL TESTS MUST USE PLAYWRIGHT MCP BROWSER CONTROL TOOLS**

### ⚠️ MANDATORY REQUIREMENTS ⚠️
1. **NO TRADITIONAL PLAYWRIGHT** - Do NOT use @playwright/test directly
2. **ONLY MCP TOOLS** - Use `mcp__playwright__browser_*` tools exclusively
3. **VISIBLE BROWSER** - All tests run in visible browser (not headless)
4. **INTERACTIVE TESTING** - Real browser interactions via MCP protocol
5. **NO TEST RUNNERS** - Use MCP browser control, not test frameworks

### MCP Tool Reference
```
REQUIRED MCP TOOLS FOR ALL TESTS:
- mcp__playwright__browser_navigate - Navigate to URLs
- mcp__playwright__browser_fill_form - Fill multiple form fields
- mcp__playwright__browser_type - Type into input fields
- mcp__playwright__browser_click - Click elements
- mcp__playwright__browser_snapshot - Capture page state
- mcp__playwright__browser_evaluate - Run JavaScript in browser
- mcp__playwright__browser_wait_for - Wait for conditions
- mcp__playwright__browser_take_screenshot - Capture evidence
- mcp__playwright__browser_console_messages - Check console
- mcp__playwright__browser_network_requests - Monitor network
```

## Testing Strategy - MCP Approach

### Methodology
- **Framework**: Playwright MCP (Model Context Protocol) - NOT traditional Playwright
- **Execution**: Direct browser control via MCP tools
- **Evidence**: Screenshots and snapshots for every test
- **Validation**: Browser evaluate for assertions
- **Coverage Goal**: 100% critical paths using MCP browser automation

### MCP Test Pattern
Every test MUST follow this pattern:
1. `mcp__playwright__browser_navigate` to test URL
2. `mcp__playwright__browser_snapshot` to verify page loaded
3. `mcp__playwright__browser_fill_form` or `browser_type` for inputs
4. `mcp__playwright__browser_click` for actions
5. `mcp__playwright__browser_wait_for` for state changes
6. `mcp__playwright__browser_evaluate` for validation
7. `mcp__playwright__browser_take_screenshot` for evidence

---

## 🔴 Priority 1: Core Authentication & Access Control (Week 1)
**ALL TESTS USE PLAYWRIGHT MCP - NO EXCEPTIONS**

### User Registration [MCP]
- [ ] **REG-001**: [MCP] Navigate to /register, use browser_fill_form for all fields, browser_click submit, validate with browser_evaluate
- [ ] **REG-002**: [MCP] Use browser_type invalid email, browser_snapshot for error, browser_evaluate for validation message
- [ ] **REG-003**: [MCP] Use browser_type weak password, browser_wait_for error display, browser_take_screenshot
- [ ] **REG-004**: [MCP] Use browser_fill_form with duplicate username, browser_network_requests to verify API call
- [ ] **REG-005**: [MCP] Use browser_click on terms checkbox, browser_evaluate to check state
- [ ] **REG-006**: [MCP] After registration, use browser_network_requests to verify email API call
- [ ] **REG-007**: [MCP] Register user via MCP, attempt duplicate with browser_fill_form, verify error
- [ ] **REG-008**: [MCP] Use browser_type with SQL injection payload, browser_evaluate for security
- [ ] **REG-009**: [MCP] Use browser_type with XSS payload, browser_console_messages to verify no execution
- [ ] **REG-010**: [MCP] Rapid submissions with browser_click, browser_network_requests for rate limit

### User Login [MCP]
- [x] **LOGIN-001**: [MCP] browser_navigate to /login, browser_fill_form with valid creds, browser_evaluate for JWT tokens
- [x] **LOGIN-002**: [MCP] browser_type invalid email, browser_snapshot for validation, browser_evaluate error state
- [x] **LOGIN-003**: [MCP] browser_fill_form wrong password, browser_wait_for error, browser_network_requests for 401
- [x] **LOGIN-004**: [MCP] browser_type non-existent user, browser_evaluate security response
- [x] **LOGIN-005**: [MCP] Loop browser_fill_form 5 times, browser_network_requests to verify lockout
- [x] **LOGIN-006**: [MCP] browser_click remember checkbox, browser_evaluate localStorage/sessionStorage
- [ ] **LOGIN-007**: [MCP] browser_click OAuth Google button, handle redirect with browser_navigate
- [ ] **LOGIN-008**: [MCP] browser_click OAuth GitHub button, browser_snapshot OAuth flow
- [x] **LOGIN-009**: [MCP] Login with MCP, browser_evaluate tokens, reload with browser_navigate, verify persistence
- [x] **LOGIN-010**: [MCP] Use browser_tabs to create multiple sessions, verify concurrent login handling

### Session Management [MCP]
- [ ] **SESSION-001**: [MCP] Login via browser_fill_form, browser_evaluate JWT in sessionStorage
- [ ] **SESSION-002**: [MCP] browser_wait_for token expiry time, browser_network_requests for refresh
- [ ] **SESSION-003**: [MCP] browser_click logout, browser_evaluate token removal
- [ ] **SESSION-004**: [MCP] browser_wait_for idle timeout, browser_snapshot session expired state
- [ ] **SESSION-005**: [MCP] Use browser_tabs for multi-device simulation
- [ ] **SESSION-006**: [MCP] Change password flow with browser_fill_form, verify forced logout
- [ ] **SESSION-007**: [MCP] Manipulate token with browser_evaluate, verify security
- [ ] **SESSION-008**: [MCP] browser_click logout, browser_network_requests for revocation
- [ ] **SESSION-009**: [MCP] browser_evaluate secure storage practices
- [ ] **SESSION-010**: [MCP] browser_network_requests to verify CSRF tokens

### Password Recovery [MCP]
- [ ] **RECOVER-001**: [MCP] browser_navigate to /forgot-password, browser_type email, verify request
- [ ] **RECOVER-002**: [MCP] browser_navigate with reset token, browser_evaluate validation
- [ ] **RECOVER-003**: [MCP] browser_fill_form new password fields, browser_click submit
- [ ] **RECOVER-004**: [MCP] browser_wait_for 1 hour, attempt token use, verify expiration
- [ ] **RECOVER-005**: [MCP] Use reset link twice with browser_navigate, verify one-time use
- [ ] **RECOVER-006**: [MCP] Complete reset with browser_fill_form, browser_network_requests for email
- [ ] **RECOVER-007**: [MCP] Rapid reset requests with browser_click, verify rate limiting
- [ ] **RECOVER-008**: [MCP] browser_type invalid email, browser_evaluate error handling
- [ ] **RECOVER-009**: [MCP] browser_navigate with expired token, browser_snapshot error page
- [ ] **RECOVER-010**: [MCP] browser_fill_form with old password, verify history validation

---

## 🟠 Priority 2: Hive Management & Core Features (Week 2)
**MANDATORY: ALL TESTS USE PLAYWRIGHT MCP BROWSER CONTROL**

### Hive Creation [MCP]
- [ ] **HIVE-CREATE-001**: [MCP] browser_navigate to /hives/create, browser_fill_form all fields, browser_click create
- [ ] **HIVE-CREATE-002**: [MCP] browser_click private toggle, browser_type access code, verify creation
- [ ] **HIVE-CREATE-003**: [MCP] browser_type duplicate name, browser_wait_for validation error
- [ ] **HIVE-CREATE-004**: [MCP] browser_type description, browser_evaluate character count
- [ ] **HIVE-CREATE-005**: [MCP] browser_type member limit, browser_evaluate validation
- [ ] **HIVE-CREATE-006**: [MCP] browser_click tags, browser_evaluate tag limit enforcement
- [ ] **HIVE-CREATE-007**: [MCP] browser_select_option focus mode dropdown
- [ ] **HIVE-CREATE-008**: [MCP] browser_drag slider for session length
- [ ] **HIVE-CREATE-009**: [MCP] browser_select_option category dropdown
- [ ] **HIVE-CREATE-010**: [MCP] Rapid creation with browser_click, verify rate limit

### Hive Discovery [MCP]
- [ ] **HIVE-DISCOVER-001**: [MCP] browser_navigate to /hives, browser_snapshot list display
- [ ] **HIVE-DISCOVER-002**: [MCP] browser_type in search field, browser_wait_for results
- [ ] **HIVE-DISCOVER-003**: [MCP] browser_click category filter, browser_evaluate filtered results
- [ ] **HIVE-DISCOVER-004**: [MCP] browser_click active members filter, verify filtering
- [ ] **HIVE-DISCOVER-005**: [MCP] browser_select_option focus mode filter
- [ ] **HIVE-DISCOVER-006**: [MCP] browser_click sort by popularity
- [ ] **HIVE-DISCOVER-007**: [MCP] browser_click sort by date
- [ ] **HIVE-DISCOVER-008**: [MCP] browser_click pagination, browser_evaluate 20 items
- [ ] **HIVE-DISCOVER-009**: [MCP] browser_hover hive card, browser_click details
- [ ] **HIVE-DISCOVER-010**: [MCP] browser_wait_for real-time member count updates

### Joining Hives [MCP]
- [ ] **HIVE-JOIN-001**: [MCP] browser_click join button on public hive
- [ ] **HIVE-JOIN-002**: [MCP] browser_type access code, browser_click join private
- [ ] **HIVE-JOIN-003**: [MCP] browser_click request access, browser_wait_for approval
- [ ] **HIVE-JOIN-004**: [MCP] Fill hive to max, browser_click join, verify rejection
- [ ] **HIVE-JOIN-005**: [MCP] Join hive, browser_click join again, verify prevention
- [ ] **HIVE-JOIN-006**: [MCP] browser_click join, browser_network_requests for notification
- [ ] **HIVE-JOIN-007**: [MCP] browser_type wrong code, browser_evaluate error
- [ ] **HIVE-JOIN-008**: [MCP] Banned user attempts join with browser_click
- [ ] **HIVE-JOIN-009**: [MCP] browser_navigate to history, verify join tracking
- [ ] **HIVE-JOIN-010**: [MCP] browser_tabs for concurrent joins, verify handling

### Hive Management [MCP]
- [ ] **HIVE-MANAGE-001**: [MCP] browser_click edit, browser_fill_form updates, save
- [ ] **HIVE-MANAGE-002**: [MCP] browser_click delete, browser_handle_dialog confirm
- [ ] **HIVE-MANAGE-003**: [MCP] browser_select_option new owner, browser_click transfer
- [ ] **HIVE-MANAGE-004**: [MCP] browser_click member menu, browser_click kick
- [ ] **HIVE-MANAGE-005**: [MCP] browser_click ban user, browser_evaluate ban list
- [ ] **HIVE-MANAGE-006**: [MCP] browser_click promote to mod
- [ ] **HIVE-MANAGE-007**: [MCP] browser_click privacy toggle, verify changes
- [ ] **HIVE-MANAGE-008**: [MCP] browser_click archive inactive hive
- [ ] **HIVE-MANAGE-009**: [MCP] browser_click generate new code button
- [ ] **HIVE-MANAGE-010**: [MCP] browser_click export analytics

---

## 🟡 Priority 3: Real-Time Features & Collaboration (Week 3)
**CRITICAL: USE MCP BROWSER TOOLS FOR ALL REAL-TIME TESTING**

### Focus Sessions [MCP]
- [ ] **SESSION-001**: [MCP] browser_click start Pomodoro, browser_wait_for timer
- [ ] **SESSION-002**: [MCP] browser_wait_for break, browser_evaluate 5 min timer
- [ ] **SESSION-003**: [MCP] browser_wait_for long break, verify 15 min
- [ ] **SESSION-004**: [MCP] browser_click pause, browser_click resume
- [ ] **SESSION-005**: [MCP] browser_click end session early
- [ ] **SESSION-006**: [MCP] browser_tabs for multi-user, verify sync
- [ ] **SESSION-007**: [MCP] browser_wait_for completion, check notification
- [ ] **SESSION-008**: [MCP] browser_wait_for auto-break transition
- [ ] **SESSION-009**: [MCP] Complete session, browser_navigate to history
- [ ] **SESSION-010**: [MCP] browser_evaluate streak tracking data

### Real-Time Presence [MCP]
- [ ] **PRESENCE-001**: [MCP] Login, browser_evaluate online status indicator
- [ ] **PRESENCE-002**: [MCP] browser_wait_for 5 min idle, verify away status
- [ ] **PRESENCE-003**: [MCP] Start session, browser_evaluate focusing status
- [ ] **PRESENCE-004**: [MCP] browser_click break, verify break status
- [ ] **PRESENCE-005**: [MCP] browser_tabs for second user, verify list updates
- [ ] **PRESENCE-006**: [MCP] browser_type in chat, verify typing indicator
- [ ] **PRESENCE-007**: [MCP] Disconnect network, browser_evaluate handling
- [ ] **PRESENCE-008**: [MCP] Reconnect, browser_evaluate state recovery
- [ ] **PRESENCE-009**: [MCP] browser_close, verify cleanup
- [ ] **PRESENCE-010**: [MCP] browser_tabs multiple, verify presence

### Chat System [MCP]
- [ ] **CHAT-001**: [MCP] browser_type message, browser_press_key Enter
- [ ] **CHAT-002**: [MCP] Send from tab1, browser_wait_for in tab2
- [ ] **CHAT-003**: [MCP] browser_evaluate message history loading
- [ ] **CHAT-004**: [MCP] browser_type emoji, verify rendering
- [ ] **CHAT-005**: [MCP] browser_hover message, browser_click edit
- [ ] **CHAT-006**: [MCP] browser_hover message, browser_click delete
- [ ] **CHAT-007**: [MCP] browser_type @username, verify mention
- [ ] **CHAT-008**: [MPC] browser_evaluate unread counter
- [ ] **CHAT-009**: [MCP] Rapid messages with browser_type, verify rate limit
- [ ] **CHAT-010**: [MCP] browser_type profanity, verify filter

### WebSocket Connection [MCP]
- [ ] **WS-001**: [MCP] browser_network_requests, verify WS upgrade
- [ ] **WS-002**: [MCP] browser_wait_for heartbeat messages
- [ ] **WS-003**: [MCP] Disconnect, browser_wait_for reconnection
- [ ] **WS-004**: [MCP] browser_evaluate connection state
- [ ] **WS-005**: [MCP] Disconnect, send messages, reconnect, verify queue
- [ ] **WS-006**: [MCP] Send binary data, browser_network_requests
- [ ] **WS-007**: [MCP] browser_network_requests for HTTP→WS upgrade
- [ ] **WS-008**: [MCP] Subscribe multiple topics, verify handling
- [ ] **WS-009**: [MCP] browser_click logout, verify WS cleanup
- [ ] **WS-010**: [MCP] browser_tabs multiple, verify load balancing

---

## 🟢 Priority 4: Analytics & Gamification (Week 4)
**ENFORCE: PLAYWRIGHT MCP FOR ALL UI INTERACTIONS**

### Analytics Dashboard [MCP]
- [ ] **ANALYTICS-001**: [MCP] browser_navigate to /analytics, browser_evaluate chart data
- [ ] **ANALYTICS-002**: [MCP] browser_click week view, browser_snapshot trends
- [ ] **ANALYTICS-003**: [MCP] browser_select_option month, browser_evaluate report
- [ ] **ANALYTICS-004**: [MCP] browser_evaluate streak display element
- [ ] **ANALYTICS-005**: [MCP] browser_hover chart, browser_evaluate tooltip
- [ ] **ANALYTICS-006**: [MCP] browser_click hive filter, verify stats update
- [ ] **ANALYTICS-007**: [MCP] browser_evaluate comparison metrics
- [ ] **ANALYTICS-008**: [MCP] browser_click export CSV button
- [ ] **ANALYTICS-009**: [MCP] browser_fill_form goals, browser_evaluate tracking
- [ ] **ANALYTICS-010**: [MCP] browser_evaluate AI insights panel

### Gamification System [MCP]
- [ ] **GAME-001**: [MCP] Complete session, browser_evaluate XP animation
- [ ] **GAME-002**: [MCP] browser_wait_for achievement unlock popup
- [ ] **GAME-003**: [MCP] browser_evaluate level progress bar
- [ ] **GAME-004**: [MCP] browser_navigate to challenges, verify daily
- [ ] **GAME-005**: [MCP] browser_click weekly challenges tab
- [ ] **GAME-006**: [MCP] browser_navigate to leaderboard
- [ ] **GAME-007**: [MCP] browser_click badges, browser_evaluate collection
- [ ] **GAME-008**: [MCP] browser_evaluate streak bonus display
- [ ] **GAME-009**: [MCP] Join team, browser_evaluate team challenges
- [ ] **GAME-010**: [MCP] browser_click redeem rewards

### Buddy System [MCP]
- [ ] **BUDDY-001**: [MCP] browser_click request buddy button
- [ ] **BUDDY-002**: [MCP] browser_click accept request
- [ ] **BUDDY-003**: [MCP] browser_click decline request
- [ ] **BUDDY-004**: [MCP] browser_evaluate matching algorithm results
- [ ] **BUDDY-005**: [MCP] browser_fill_form shared goals
- [ ] **BUDDY-006**: [MCP] browser_evaluate progress sharing UI
- [ ] **BUDDY-007**: [MCP] browser_wait_for reminder notification
- [ ] **BUDDY-008**: [MCP] browser_type buddy chat message
- [ ] **BUDDY-009**: [MCP] browser_click end partnership
- [ ] **BUDDY-010**: [MCP] browser_evaluate buddy metrics

---

## 🔵 Priority 5: User Experience & Accessibility (Week 5)
**REQUIRED: MCP BROWSER TOOLS FOR ALL ACCESSIBILITY TESTING**

### User Profile [MCP]
- [ ] **PROFILE-001**: [MCP] browser_type new display name, save
- [ ] **PROFILE-002**: [MCP] browser_file_upload avatar image
- [ ] **PROFILE-003**: [MCP] browser_type bio text, verify save
- [ ] **PROFILE-004**: [MCP] browser_select_option timezone
- [ ] **PROFILE-005**: [MCP] browser_select_option language
- [ ] **PROFILE-006**: [MCP] browser_click theme toggle
- [ ] **PROFILE-007**: [MCP] browser_click notification preferences
- [ ] **PROFILE-008**: [MCP] browser_click privacy settings
- [ ] **PROFILE-009**: [MCP] browser_click delete account
- [ ] **PROFILE-010**: [MCP] browser_click export data

### Notifications [MCP]
- [ ] **NOTIF-001**: [MCP] Trigger event, browser_evaluate in-app notification
- [ ] **NOTIF-002**: [MCP] browser_evaluate email sent indicator
- [ ] **NOTIF-003**: [MCP] browser_evaluate PWA push notification
- [ ] **NOTIF-004**: [MCP] browser_fill_form notification preferences
- [ ] **NOTIF-005**: [MCP] browser_click DND toggle
- [ ] **NOTIF-006**: [MCP] browser_navigate to notification history
- [ ] **NOTIF-007**: [MCP] browser_click mark as read
- [ ] **NOTIF-008**: [MCP] browser_click select all, browser_click bulk action
- [ ] **NOTIF-009**: [MCP] browser_click sound settings
- [ ] **NOTIF-010**: [MCP] browser_evaluate desktop notification

### Accessibility [MCP]
- [ ] **A11Y-001**: [MCP] browser_press_key Tab through page
- [ ] **A11Y-002**: [MCP] browser_evaluate screen reader elements
- [ ] **A11Y-003**: [MCP] browser_evaluate ARIA labels
- [ ] **A11Y-004**: [MCP] browser_press_key Tab, verify focus indicators
- [ ] **A11Y-005**: [MCP] browser_evaluate color contrast ratios
- [ ] **A11Y-006**: [MCP] browser_evaluate with zoom 200%
- [ ] **A11Y-007**: [MCP] browser_evaluate reduced motion CSS
- [ ] **A11Y-008**: [MCP] browser_evaluate alt text presence
- [ ] **A11Y-009**: [MCP] browser_evaluate form labels
- [ ] **A11Y-010**: [MCP] Trigger error, browser_evaluate announcement

### Progressive Web App [MCP]
- [ ] **PWA-001**: [MCP] browser_evaluate service worker registration
- [ ] **PWA-002**: [MCP] Go offline, browser_evaluate offline mode
- [ ] **PWA-003**: [MCP] browser_evaluate install prompt
- [ ] **PWA-004**: [MCP] browser_evaluate manifest icon
- [ ] **PWA-005**: [MCP] browser_evaluate splash screen
- [ ] **PWA-006**: [MCP] browser_evaluate background sync
- [ ] **PWA-007**: [MCP] browser_evaluate cache storage
- [ ] **PWA-008**: [MCP] browser_wait_for update notification
- [ ] **PWA-009**: [MCP] browser_navigate with deep link
- [ ] **PWA-010**: [MCP] browser_evaluate share target API

---

## 🟣 Priority 6: Performance & Security (Week 6)
**MANDATE: USE MCP FOR ALL PERFORMANCE MONITORING**

### Performance Testing [MCP]
- [ ] **PERF-001**: [MCP] browser_navigate, browser_evaluate load time <3s
- [ ] **PERF-002**: [MCP] browser_evaluate time to interactive <5s
- [ ] **PERF-003**: [MCP] browser_network_requests, verify API <200ms
- [ ] **PERF-004**: [MCP] browser_network_requests, verify WS <100ms
- [ ] **PERF-005**: [MCP] browser_evaluate memory usage
- [ ] **PERF-006**: [MCP] browser_evaluate CPU usage
- [ ] **PERF-007**: [MCP] browser_evaluate bundle size <500KB
- [ ] **PERF-008**: [MCP] browser_evaluate image optimization
- [ ] **PERF-009**: [MCP] Scroll page, browser_evaluate lazy loading
- [ ] **PERF-010**: [MCP] browser_navigate routes, verify code splitting

### Security Testing [MCP]
- [ ] **SEC-001**: [MCP] browser_type SQL injection payloads
- [ ] **SEC-002**: [MCP] browser_type XSS payloads
- [ ] **SEC-003**: [MCP] browser_evaluate CSRF token presence
- [ ] **SEC-004**: [MCP] browser_network_requests, verify headers
- [ ] **SEC-005**: [MCP] browser_navigate HTTP, verify HTTPS redirect
- [ ] **SEC-006**: [MCP] browser_evaluate CSP headers
- [ ] **SEC-007**: [MCP] Rapid requests, verify rate limiting
- [ ] **SEC-008**: [MCP] browser_type invalid inputs, verify validation
- [ ] **SEC-009**: [MCP] browser_file_upload malicious file
- [ ] **SEC-010**: [MCP] browser_network_requests without auth

### Load Testing [MCP]
- [ ] **LOAD-001**: [MCP] browser_tabs create 100 concurrent sessions
- [ ] **LOAD-002**: [MCP] browser_tabs create 500 concurrent sessions
- [ ] **LOAD-003**: [MCP] browser_tabs create 1000 concurrent sessions
- [ ] **LOAD-004**: [MCP] browser_wait_for 1 hour sustained load
- [ ] **LOAD-005**: [MCP] Spike test with rapid browser_tabs
- [ ] **LOAD-006**: [MCP] browser_evaluate connection pooling
- [ ] **LOAD-007**: [MCP] browser_tabs with WebSocket connections
- [ ] **LOAD-008**: [MCP] browser_evaluate CDN performance
- [ ] **LOAD-009**: [MCP] browser_evaluate cache hit rates
- [ ] **LOAD-010**: [MCP] browser_evaluate graceful degradation

---

## 🔶 Priority 7: Cross-Platform & Integration (Week 7)
**ESSENTIAL: MCP BROWSER CONTROL FOR CROSS-BROWSER TESTING**

### Cross-Browser Testing [MCP]
- [ ] **BROWSER-001**: [MCP] browser_resize for Chrome testing
- [ ] **BROWSER-002**: [MCP] Configure Firefox, browser_navigate
- [ ] **BROWSER-003**: [MCP] Configure Safari, browser_navigate
- [ ] **BROWSER-004**: [MCP] Configure Edge, browser_navigate
- [ ] **BROWSER-005**: [MCP] browser_resize mobile Chrome
- [ ] **BROWSER-006**: [MCP] browser_resize Safari iOS
- [ ] **BROWSER-007**: [MCP] Test Samsung Internet via MCP
- [ ] **BROWSER-008**: [MCP] Configure Opera via MCP
- [ ] **BROWSER-009**: [MCP] Configure Brave via MCP
- [ ] **BROWSER-010**: [MCP] browser_evaluate version compatibility

### Mobile Responsiveness [MCP]
- [ ] **MOBILE-001**: [MCP] browser_resize 375px (iPhone SE)
- [ ] **MOBILE-002**: [MCP] browser_resize 390px (iPhone 12)
- [ ] **MOBILE-003**: [MCP] browser_resize Samsung Galaxy
- [ ] **MOBILE-004**: [MCP] browser_resize 768px (iPad)
- [ ] **MOBILE-005**: [MCP] browser_resize 1024px (iPad Pro)
- [ ] **MOBILE-006**: [MCP] browser_click, browser_drag for touch
- [ ] **MOBILE-007**: [MCP] browser_drag for gesture support
- [ ] **MOBILE-008**: [MCP] browser_resize various viewports
- [ ] **MOBILE-009**: [MCP] browser_resize for orientation
- [ ] **MOBILE-010**: [MCP] browser_evaluate mobile features

### API Integration [MCP]
- [ ] **API-001**: [MCP] browser_network_requests REST testing
- [ ] **API-002**: [MCP] browser_network_requests GraphQL
- [ ] **API-003**: [MCP] browser_click pagination, verify API
- [ ] **API-004**: [MCP] Apply filters, browser_network_requests
- [ ] **API-005**: [MCP] Apply sort, browser_network_requests
- [ ] **API-006**: [MCP] Trigger error, browser_network_requests
- [ ] **API-007**: [MCP] Rapid requests, verify rate limit
- [ ] **API-008**: [MCP] browser_network_requests API version
- [ ] **API-009**: [MCP] browser_evaluate API documentation
- [ ] **API-010**: [MCP] browser_network_requests third-party

---

## 🟤 Priority 8: Error Handling & Recovery (Week 8)
**FINAL: ALL ERROR SCENARIOS VIA MCP BROWSER CONTROL**

### Error Scenarios [MCP]
- [ ] **ERROR-001**: [MCP] Disconnect network, browser_evaluate handling
- [ ] **ERROR-002**: [MCP] browser_wait_for timeout, verify handling
- [ ] **ERROR-003**: [MCP] browser_navigate to /nonexistent
- [ ] **ERROR-004**: [MCP] Trigger 500, browser_evaluate error page
- [ ] **ERROR-005**: [MCP] browser_type invalid data, verify handling
- [ ] **ERROR-006**: [MCP] Wait for session expiry, browser_evaluate
- [ ] **ERROR-007**: [MCP] Access restricted, browser_evaluate denied
- [ ] **ERROR-008**: [MCP] Request missing resource, verify 404
- [ ] **ERROR-009**: [MCP] browser_type invalid, verify validation
- [ ] **ERROR-010**: [MCP] browser_tabs concurrent updates

### Data Integrity [MCP]
- [ ] **DATA-001**: [MCP] Start transaction, cancel, verify rollback
- [ ] **DATA-002**: [MCP] Submit duplicate, browser_evaluate prevention
- [ ] **DATA-003**: [MCP] Delete parent, verify referential integrity
- [ ] **DATA-004**: [MCP] browser_evaluate data migration
- [ ] **DATA-005**: [MCP] browser_evaluate backup verification
- [ ] **DATA-006**: [MCP] Corrupt data, browser_evaluate recovery
- [ ] **DATA-007**: [MCP] browser_evaluate audit logging
- [ ] **DATA-008**: [MCP] browser_evaluate data consistency
- [ ] **DATA-009**: [MCP] Update data, browser_evaluate cache
- [ ] **DATA-010**: [MCP] browser_evaluate orphaned cleanup

### Recovery Mechanisms [MCP]
- [ ] **RECOVERY-001**: [MCP] browser_type, verify auto-save
- [ ] **RECOVERY-002**: [MCP] Crash browser, restore, verify session
- [ ] **RECOVERY-003**: [MCP] Disconnect, browser_wait_for retry
- [ ] **RECOVERY-004**: [MCP] Trigger error, browser_evaluate fallback
- [ ] **RECOVERY-005**: [MCP] Go offline, queue actions, reconnect
- [ ] **RECOVERY-006**: [MCP] Create conflict, browser_evaluate resolution
- [ ] **RECOVERY-007**: [MCP] browser_evaluate state sync
- [ ] **RECOVERY-008**: [MCP] Trigger error, browser_evaluate reporting
- [ ] **RECOVERY-009**: [MCP] browser_evaluate user feedback UI
- [ ] **RECOVERY-010**: [MCP] browser_evaluate graceful degradation

---

## MCP Test Execution Plan

### Phase 1: MCP Setup (MANDATORY)
- [ ] Install Playwright MCP server
- [ ] Configure browser control tools
- [ ] Create MCP test utilities
- [ ] Set up screenshot/snapshot storage
- [ ] Document MCP patterns

### Phase 2: MCP Implementation
**ALL TESTS MUST USE MCP BROWSER CONTROL TOOLS**
- [ ] Implement Priority 1 tests with MCP only
- [ ] Implement Priority 2 tests with MCP only
- [ ] Continue through all priorities using MCP
- [ ] NO traditional Playwright test runners

### MCP Evidence Collection
Every test MUST capture:
1. `browser_snapshot` - Initial state
2. `browser_take_screenshot` - Key interactions
3. `browser_network_requests` - API verification
4. `browser_console_messages` - Error checking
5. `browser_snapshot` - Final state

---

## Success Metrics (MCP-Based)

### MCP Coverage Goals
- [ ] 100% tests using Playwright MCP tools
- [ ] 0% traditional Playwright usage
- [ ] 100% visual browser testing
- [ ] Complete screenshot evidence
- [ ] Full interaction logs

### MCP Performance Targets
- [ ] Browser launch <2 seconds
- [ ] Page navigation <3 seconds
- [ ] Screenshot capture <500ms
- [ ] Snapshot generation <1 second
- [ ] Browser cleanup <1 second

---

## CRITICAL REMINDERS

### ✅ ALWAYS USE
- `mcp__playwright__browser_navigate` for navigation
- `mcp__playwright__browser_fill_form` for forms
- `mcp__playwright__browser_click` for interactions
- `mcp__playwright__browser_snapshot` for verification
- `mcp__playwright__browser_evaluate` for assertions

### ❌ NEVER USE
- `@playwright/test` framework
- `page.goto()` traditional navigation
- `page.click()` traditional clicks
- `expect()` Jest/Playwright assertions
- Headless browser mode

---

## MCP Test Pattern Example

```typescript
// CORRECT - Using Playwright MCP
await mcp__playwright__browser_navigate({ url: '/login' });
await mcp__playwright__browser_snapshot({});
await mcp__playwright__browser_fill_form({
  fields: [
    { name: 'Email', ref: '#email', type: 'textbox', value: 'test@example.com' },
    { name: 'Password', ref: '#password', type: 'textbox', value: 'password123' }
  ]
});
await mcp__playwright__browser_click({ element: 'Sign In button', ref: 'button[type="submit"]' });
await mcp__playwright__browser_wait_for({ time: 2 });
await mcp__playwright__browser_evaluate({ function: '() => window.location.pathname' });

// WRONG - Traditional Playwright (FORBIDDEN)
await page.goto('/login');  // ❌ DO NOT USE
await page.fill('#email', 'test@example.com');  // ❌ DO NOT USE
await page.click('button[type="submit"]');  // ❌ DO NOT USE
```

---

**Total Tests**: 280 (ALL using Playwright MCP)
**MCP Requirement**: 100% MANDATORY
**Traditional Playwright**: 0% FORBIDDEN

---

*Last Updated: January 2025*
*Version: 2.0 - PLAYWRIGHT MCP ONLY*
*Mandate: ALL E2E TESTS USE MCP BROWSER CONTROL TOOLS*