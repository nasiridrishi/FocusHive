# Visual Assets Creation Guide

This guide provides instructions for creating all visual assets needed for the FocusHive draft report.

## Screenshots Required

### 1. Application Screenshots

#### Login Screen (Figure 4.2)
- Show the clean login interface
- Include both email/password fields
- Show "Remember Me" and "Forgot Password" options
- Capture with proper styling and branding

#### Main Dashboard (Figure 4.3)
- Display active hives list
- Show user's current presence status
- Include navigation menu
- Capture during active use with realistic data

#### Hive Interface (Figure 4.4)
- Show active members with presence indicators
- Display chat panel on the right
- Include timer widget at the top
- Show at least 3-4 active users with different statuses

#### Real-time Chat (Figure 4.5)
- Capture active conversation
- Show message timestamps
- Include system messages (user joined/left)
- Display message edit/delete options

#### Timer Widget States (Figure 4.6)
- Create composite image showing:
  - Idle state
  - Active timer countdown
  - Break time
  - Completed session

#### User Settings (Figure 4.7)
- Profile information form
- Pomodoro timer preferences
- Notification settings
- Privacy controls

### 2. Performance Visualizations

#### Response Time Distribution (Figure 5.4)
```python
import matplotlib.pyplot as plt
import numpy as np

# Sample data for response time distribution
response_times = np.random.lognormal(2.5, 0.5, 10000)
response_times = response_times[response_times < 100]  # Cap at 100ms

plt.figure(figsize=(10, 6))
plt.hist(response_times, bins=50, color='#4DABF7', edgecolor='black', alpha=0.7)
plt.axvline(np.percentile(response_times, 95), color='red', linestyle='dashed', linewidth=2, label='95th Percentile')
plt.xlabel('Response Time (ms)')
plt.ylabel('Frequency')
plt.title('WebSocket Message Response Time Distribution')
plt.legend()
plt.grid(True, alpha=0.3)
```

#### Concurrent Users vs Response Time (Figure 5.5)
```python
# Data for scalability graph
users = [100, 500, 1000, 2000, 3000, 4000, 5000]
avg_response = [12, 15, 18, 25, 35, 52, 78]
p95_response = [23, 28, 31, 42, 58, 89, 142]

plt.figure(figsize=(10, 6))
plt.plot(users, avg_response, 'b-o', label='Average Response Time', linewidth=2, markersize=8)
plt.plot(users, p95_response, 'r--s', label='95th Percentile', linewidth=2, markersize=8)
plt.xlabel('Concurrent Users')
plt.ylabel('Response Time (ms)')
plt.title('System Scalability: Response Time vs Concurrent Users')
plt.legend()
plt.grid(True, alpha=0.3)
plt.xlim(0, 5500)
plt.ylim(0, 160)
```

#### Memory Usage Over Time (Figure 5.6)
```python
# Memory usage during 1-hour load test
time_minutes = np.arange(0, 61, 1)
memory_usage = 1024 + np.cumsum(np.random.normal(2, 5, 61))
memory_usage = np.maximum(memory_usage, 1024)  # Ensure no negative values

plt.figure(figsize=(10, 6))
plt.plot(time_minutes, memory_usage, 'g-', linewidth=2)
plt.fill_between(time_minutes, memory_usage, 1024, alpha=0.3, color='green')
plt.xlabel('Time (minutes)')
plt.ylabel('Memory Usage (MB)')
plt.title('JVM Memory Usage During Load Test (1000 Concurrent Users)')
plt.grid(True, alpha=0.3)
plt.xlim(0, 60)
```

#### Database Query Performance (Figure 5.7)
```python
# Query performance data
queries = ['User Presence\nLookup', 'Hive Member\nQuery', 'Message\nHistory', 'Focus Session\nStats']
avg_times = [2.3, 1.8, 4.5, 8.2]
p95_times = [4.1, 3.2, 7.8, 12.4]

x = np.arange(len(queries))
width = 0.35

fig, ax = plt.subplots(figsize=(10, 6))
bars1 = ax.bar(x - width/2, avg_times, width, label='Average', color='#4DABF7')
bars2 = ax.bar(x + width/2, p95_times, width, label='95th Percentile', color='#FF6B6B')

ax.set_xlabel('Query Type')
ax.set_ylabel('Response Time (ms)')
ax.set_title('Database Query Performance by Type')
ax.set_xticks(x)
ax.set_xticklabels(queries)
ax.legend()
ax.grid(True, alpha=0.3, axis='y')
```

### 3. Architecture Diagrams Export

For each Mermaid diagram in `diagrams.md`:

1. **High Resolution Export**:
   - Use https://mermaid.live/
   - Set background to white
   - Export as PNG at 2x resolution
   - Export as SVG for vector quality

2. **Consistent Styling**:
   - Use the same color scheme across all diagrams
   - Ensure readable font sizes (minimum 12pt)
   - Add subtle shadows for depth
   - Keep consistent spacing and alignment

3. **File Naming Convention**:
   ```
   figure-3-1-system-architecture.png
   figure-3-2-database-erd.png
   figure-3-3-websocket-auth-flow.png
   figure-3-4-presence-update-flow.png
   figure-4-1-component-architecture.png
   figure-5-1-performance-metrics.png
   figure-5-2-test-coverage.png
   figure-5-3-state-machine.png
   ```

## Creating Professional Screenshots

### Browser Setup
1. Use Chrome/Firefox in incognito mode
2. Set zoom to 100%
3. Hide bookmarks and extensions
4. Use a clean desktop background

### Data Preparation
1. Create realistic test data:
   - User names: "Alex Chen", "Sarah Miller", "James Wilson"
   - Hive names: "Deep Focus Zone", "Study Buddies", "Project Sprint"
   - Status messages: "Working on report", "Taking a break", "In flow state"

2. Ensure variety in:
   - User statuses (online, away, busy, offline)
   - Message types and lengths
   - Timer states and durations

### Screenshot Tools
- **macOS**: Cmd+Shift+4 for selection
- **Windows**: Win+Shift+S for snipping tool
- **Linux**: Flameshot or Spectacle
- **Browser**: Full Page Screen Capture extension

### Post-Processing
1. Crop to remove unnecessary whitespace
2. Add subtle drop shadow if needed
3. Ensure consistent dimensions where applicable
4. Compress PNG files without quality loss
5. Add red arrows/boxes to highlight key features

## Performance Data Collection

### Load Testing Setup
```bash
# Using Apache JMeter for load testing
# WebSocket load test configuration
- Thread Group: 1000 users
- Ramp-up Period: 60 seconds
- Loop Count: Infinite
- Duration: 3600 seconds

# Samplers:
1. WebSocket Open Connection
2. WebSocket Send Message (presence update)
3. WebSocket Send Message (heartbeat)
4. WebSocket Read Message
5. WebSocket Close
```

### Metrics to Capture
1. **Response Times**:
   - Connection establishment time
   - Message round-trip time
   - Subscription confirmation time

2. **Throughput**:
   - Messages per second
   - Successful connections per second
   - Data transfer rate (KB/s)

3. **Resource Usage**:
   - CPU utilization
   - Memory usage (heap and non-heap)
   - Thread count
   - Database connection pool usage

4. **Error Rates**:
   - Connection failures
   - Message delivery failures
   - Timeout occurrences

## Visual Consistency Guidelines

### Color Palette
- Primary: #4DABF7 (Blue)
- Success: #51CF66 (Green)
- Warning: #FFE066 (Yellow)
- Error: #FF6B6B (Red)
- Neutral: #495057 (Gray)

### Typography
- Headers: Sans-serif, bold
- Body: Sans-serif, regular
- Code: Monospace font
- Minimum size: 12pt for readability

### Spacing and Layout
- Consistent margins (20px minimum)
- Proper alignment of elements
- White space for clarity
- Group related information

## Deliverables Checklist

- [ ] All Mermaid diagrams exported as PNG and SVG
- [ ] Application screenshots (7 required)
- [ ] Performance graphs (4 required)
- [ ] Test coverage visualization
- [ ] State machine diagram
- [ ] All images properly named and organized
- [ ] Images optimized for file size
- [ ] Alternative text descriptions prepared
- [ ] Figure captions written
- [ ] Cross-references verified in report text