# Performance Fix for 100% CPU Usage

## Problem Identified
The application was causing 100% CPU usage due to a scheduled task in `TimerScheduler.java` that was running every second and performing heavy database operations.

## Root Causes
1. **TimerScheduler.updateActiveTimers()** - Running every 1 second with:
   - `findAll()` fetching ALL timers from database
   - Processing and saving each timer
   - Making user service calls

2. **Additional scheduled tasks**:
   - PresenceTrackingService: 2 tasks every minute
   - WebSocketEventHandler: 3 tasks (1/minute, 2/hour)

## Fixes Applied

### 1. Reduced Timer Update Frequency
- Changed from 1 second to 5 seconds interval
- Updated decrement logic to match new interval

### 2. Optimized Database Queries
- Changed from `findAll().stream().filter()` to `findByIsRunningTrue()`
- Added early return when no active timers

### 3. Quick Disable Option
To temporarily disable ALL scheduled tasks for testing:

```java
// In FocusHiveApplication.java, comment out this line:
@EnableScheduling  // <- Comment this out to disable all scheduled tasks
```

## Testing the Fix

1. **Rebuild the application**:
```bash
./gradlew clean build
```

2. **Run with monitoring**:
```bash
./gradlew bootRun
```

3. **Monitor CPU usage**:
```bash
# Linux/Mac
top -p $(pgrep -f focushive)

# Or use htop
htop
```

## Further Optimizations Recommended

1. **Implement caching** for user lookups in TimerScheduler
2. **Use event-driven updates** instead of polling for timers
3. **Batch database updates** instead of individual saves
4. **Consider using Spring Cache** for frequently accessed data
5. **Add configuration property** to control scheduling:
   ```yaml
   scheduling:
     enabled: true  # Can be set to false to disable
     timer-update-interval: 5000  # Configurable interval
   ```

## Emergency Disable

If CPU issues persist, you can completely disable scheduling:

1. Comment out `@EnableScheduling` in `FocusHiveApplication.java`
2. Or set environment variable: `SPRING_TASK_SCHEDULING_ENABLED=false`
3. Or add to application.yml:
   ```yaml
   spring:
     task:
       scheduling:
         enabled: false
   ```

## Monitoring Recommendations

Add these endpoints to monitor scheduled tasks:
- `/actuator/scheduledtasks` - View all scheduled tasks
- `/actuator/metrics/scheduler.active` - Active scheduled tasks count
- `/actuator/health` - Overall application health