package com.focushive.timer.scheduler;

import com.focushive.timer.dto.TimerStateDto;
import com.focushive.timer.entity.HiveTimer;
import com.focushive.timer.repository.HiveTimerRepository;
import com.focushive.timer.service.TimerService;
import com.focushive.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled tasks for updating active timers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimerScheduler {
    
    private final HiveTimerRepository hiveTimerRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    
    /**
     * Update all active hive timers every second.
     * Broadcasts updates to connected clients.
     */
    @Scheduled(fixedDelay = 1000) // Run every second
    @Transactional
    public void updateActiveTimers() {
        List<HiveTimer> activeTimers = hiveTimerRepository.findAll().stream()
                .filter(HiveTimer::getIsRunning)
                .toList();
        
        for (HiveTimer timer : activeTimers) {
            // Decrement remaining seconds
            int remainingSeconds = timer.getRemainingSeconds() - 1;
            
            if (remainingSeconds <= 0) {
                // Timer completed
                timer.setRemainingSeconds(0);
                timer.setIsRunning(false);
                timer.setCompletedAt(LocalDateTime.now());
                
                // Broadcast completion
                TimerStateDto stateDto = convertToTimerStateDto(timer);
                messagingTemplate.convertAndSend(
                    "/topic/hive/" + timer.getHiveId() + "/timer/complete", 
                    stateDto
                );
                
                log.info("Timer {} for hive {} completed", timer.getId(), timer.getHiveId());
            } else {
                // Update remaining time
                timer.setRemainingSeconds(remainingSeconds);
                
                // Broadcast update every 5 seconds to reduce traffic
                if (remainingSeconds % 5 == 0) {
                    TimerStateDto stateDto = convertToTimerStateDto(timer);
                    messagingTemplate.convertAndSend(
                        "/topic/hive/" + timer.getHiveId() + "/timer", 
                        stateDto
                    );
                }
            }
            
            hiveTimerRepository.save(timer);
        }
    }
    
    /**
     * Clean up old completed timers daily.
     */
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    @Transactional
    public void cleanupOldTimers() {
        log.info("Starting daily timer cleanup");
        
        // Delete timers completed more than 7 days ago
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        List<HiveTimer> oldTimers = hiveTimerRepository.findAll().stream()
                .filter(timer -> !timer.getIsRunning() && 
                        timer.getCompletedAt() != null && 
                        timer.getCompletedAt().isBefore(cutoffDate))
                .toList();
        
        if (!oldTimers.isEmpty()) {
            hiveTimerRepository.deleteAll(oldTimers);
            log.info("Deleted {} old completed timers", oldTimers.size());
        }
    }
    
    private TimerStateDto convertToTimerStateDto(HiveTimer timer) {
        var user = userService.getUserById(timer.getStartedBy());
        return TimerStateDto.builder()
                .timerId(timer.getId())
                .hiveId(timer.getHiveId())
                .timerType(timer.getTimerType())
                .durationMinutes(timer.getDurationMinutes())
                .remainingSeconds(timer.getRemainingSeconds())
                .isRunning(timer.getIsRunning())
                .startedBy(timer.getStartedBy())
                .startedByUsername(user.getUsername())
                .startedAt(timer.getStartedAt())
                .pausedAt(timer.getPausedAt())
                .build();
    }
}