package com.focushive.timer.service.impl;

import com.focushive.common.exception.BadRequestException;
import com.focushive.common.exception.ForbiddenException;
import com.focushive.common.exception.ResourceNotFoundException;
import com.focushive.hive.repository.HiveMemberRepository;
import com.focushive.timer.dto.*;
import com.focushive.timer.entity.*;
import com.focushive.timer.repository.*;
import com.focushive.timer.service.TimerService;
import com.focushive.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimerServiceImpl implements TimerService {
    
    private final FocusSessionRepository focusSessionRepository;
    private final ProductivityStatsRepository productivityStatsRepository;
    private final PomodoroSettingsRepository pomodoroSettingsRepository;
    private final HiveTimerRepository hiveTimerRepository;
    private final HiveMemberRepository hiveMemberRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Override
    @Transactional
    public FocusSessionDto startSession(String userId, StartSessionRequest request) {
        log.debug("Starting session for user: {} with type: {}", userId, request.getSessionType());
        
        // Check for existing active session
        if (focusSessionRepository.findByUserIdAndCompletedFalse(userId).isPresent()) {
            throw new BadRequestException("You already have an active session. Please end it first.");
        }
        
        // Create new session
        FocusSession session = FocusSession.builder()
                .userId(userId)
                .hiveId(request.getHiveId())
                .sessionType(request.getSessionType())
                .durationMinutes(request.getDurationMinutes())
                .startTime(LocalDateTime.now())
                .notes(request.getNotes())
                .completed(false)
                .interruptions(0)
                .build();
        
        session = focusSessionRepository.save(session);
        
        // Update daily stats
        updateDailyStats(userId, stats -> stats.setSessionsStarted(stats.getSessionsStarted() + 1));
        
        // Broadcast to hive if applicable
        if (request.getHiveId() != null) {
            broadcastSessionUpdate(request.getHiveId(), userId, "started", session);
        }
        
        return convertToDto(session);
    }
    
    @Override
    @Transactional
    public FocusSessionDto endSession(String userId, String sessionId) {
        log.debug("Ending session {} for user: {}", sessionId, userId);
        
        FocusSession session = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        
        if (!session.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only end your own sessions");
        }
        
        if (session.getCompleted()) {
            throw new BadRequestException("Session is already completed");
        }
        
        // Calculate actual duration
        LocalDateTime endTime = LocalDateTime.now();
        long actualMinutes = ChronoUnit.MINUTES.between(session.getStartTime(), endTime);
        
        session.setEndTime(endTime);
        session.setActualDurationMinutes((int) actualMinutes);
        session.setCompleted(true);
        
        session = focusSessionRepository.save(session);
        
        // Update daily stats
        final FocusSession finalSession = session;
        final long finalActualMinutes = actualMinutes;
        updateDailyStats(userId, stats -> {
            stats.setSessionsCompleted(stats.getSessionsCompleted() + 1);
            if (finalSession.getSessionType() == FocusSession.SessionType.WORK || 
                finalSession.getSessionType() == FocusSession.SessionType.STUDY) {
                stats.setTotalFocusMinutes(stats.getTotalFocusMinutes() + (int) finalActualMinutes);
                // Update longest streak if applicable
                if (finalActualMinutes > stats.getLongestStreakMinutes()) {
                    stats.setLongestStreakMinutes((int) finalActualMinutes);
                }
            } else if (finalSession.getSessionType() == FocusSession.SessionType.BREAK) {
                stats.setTotalBreakMinutes(stats.getTotalBreakMinutes() + (int) finalActualMinutes);
            }
        });
        
        // Broadcast to hive if applicable
        if (session.getHiveId() != null) {
            broadcastSessionUpdate(session.getHiveId(), userId, "ended", session);
        }
        
        return convertToDto(session);
    }
    
    @Override
    @Transactional
    public FocusSessionDto pauseSession(String userId, String sessionId) {
        log.debug("Pausing session {} for user: {}", sessionId, userId);
        
        FocusSession session = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        
        if (!session.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only pause your own sessions");
        }
        
        session.setInterruptions(session.getInterruptions() + 1);
        session = focusSessionRepository.save(session);
        
        return convertToDto(session);
    }
    
    @Override
    @Transactional
    public FocusSessionDto resumeSession(String userId, String sessionId) {
        // Similar implementation to pause but for resuming
        return pauseSession(userId, sessionId); // Simplified for now
    }
    
    @Override
    @Transactional(readOnly = true)
    public FocusSessionDto getCurrentSession(String userId) {
        return focusSessionRepository.findByUserIdAndCompletedFalse(userId)
                .map(this::convertToDto)
                .orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<FocusSessionDto> getSessionHistory(String userId, int page, int size) {
        Page<FocusSession> sessions = focusSessionRepository.findByUserIdOrderByStartTimeDesc(
                userId, PageRequest.of(page, size));
        return sessions.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductivityStatsDto getDailyStats(String userId, LocalDate date) {
        return productivityStatsRepository.findByUserIdAndDate(userId, date)
                .map(this::convertToStatsDto)
                .orElseGet(() -> createEmptyStatsDto(userId, date));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductivityStatsDto> getWeeklyStats(String userId, LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(6);
        List<ProductivityStats> stats = productivityStatsRepository.findByUserIdAndDateRange(
                userId, startDate, endDate);
        return stats.stream()
                .map(this::convertToStatsDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductivityStatsDto> getMonthlyStats(String userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        List<ProductivityStats> stats = productivityStatsRepository.findByUserIdAndDateRange(
                userId, startDate, endDate);
        return stats.stream()
                .map(this::convertToStatsDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Integer getCurrentStreak(String userId) {
        Integer streak = productivityStatsRepository.getCurrentStreak(userId);
        return streak != null ? streak : 0;
    }
    
    @Override
    @Transactional(readOnly = true)
    public PomodoroSettingsDto getPomodoroSettings(String userId) {
        return pomodoroSettingsRepository.findByUserId(userId)
                .map(this::convertToSettingsDto)
                .orElseGet(() -> createDefaultSettings(userId));
    }
    
    @Override
    @Transactional
    public PomodoroSettingsDto updatePomodoroSettings(String userId, PomodoroSettingsDto settings) {
        PomodoroSettings entity = pomodoroSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    PomodoroSettings newSettings = new PomodoroSettings();
                    newSettings.setUserId(userId);
                    return newSettings;
                });
        
        entity.setWorkDurationMinutes(settings.getWorkDurationMinutes());
        entity.setShortBreakMinutes(settings.getShortBreakMinutes());
        entity.setLongBreakMinutes(settings.getLongBreakMinutes());
        entity.setSessionsUntilLongBreak(settings.getSessionsUntilLongBreak());
        entity.setAutoStartBreaks(settings.getAutoStartBreaks());
        entity.setAutoStartWork(settings.getAutoStartWork());
        entity.setNotificationEnabled(settings.getNotificationEnabled());
        entity.setSoundEnabled(settings.getSoundEnabled());
        
        entity = pomodoroSettingsRepository.save(entity);
        return convertToSettingsDto(entity);
    }
    
    @Override
    @Transactional
    public PomodoroSettingsDto createDefaultSettings(String userId) {
        if (pomodoroSettingsRepository.existsByUserId(userId)) {
            return getPomodoroSettings(userId);
        }
        
        PomodoroSettings settings = PomodoroSettings.builder()
                .userId(userId)
                .build();
        
        settings = pomodoroSettingsRepository.save(settings);
        return convertToSettingsDto(settings);
    }
    
    @Override
    @Transactional
    public TimerStateDto startHiveTimer(String hiveId, String userId, TimerStateDto timerRequest) {
        log.debug("Starting hive timer for hive: {} by user: {}", hiveId, userId);
        
        // Check user is member of hive
        if (!hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)) {
            throw new ForbiddenException("You must be a member of the hive to control timers");
        }
        
        // Check for existing active timer
        if (hiveTimerRepository.existsByHiveIdAndIsRunningTrue(hiveId)) {
            throw new BadRequestException("Hive already has an active timer");
        }
        
        HiveTimer timer = HiveTimer.builder()
                .hiveId(hiveId)
                .timerType(timerRequest.getTimerType())
                .durationMinutes(timerRequest.getDurationMinutes())
                .remainingSeconds(timerRequest.getDurationMinutes() * 60)
                .isRunning(true)
                .startedBy(userId)
                .startedAt(LocalDateTime.now())
                .build();
        
        timer = hiveTimerRepository.save(timer);
        
        TimerStateDto stateDto = convertToTimerStateDto(timer);
        
        // Broadcast timer state to all hive members
        messagingTemplate.convertAndSend("/topic/hive/" + hiveId + "/timer", stateDto);
        
        return stateDto;
    }
    
    @Override
    @Transactional
    public TimerStateDto pauseHiveTimer(String hiveId, String userId) {
        HiveTimer timer = hiveTimerRepository.findByHiveIdAndIsRunningTrue(hiveId)
                .orElseThrow(() -> new ResourceNotFoundException("No active timer found"));
        
        timer.setIsRunning(false);
        timer.setPausedAt(LocalDateTime.now());
        timer = hiveTimerRepository.save(timer);
        
        TimerStateDto stateDto = convertToTimerStateDto(timer);
        messagingTemplate.convertAndSend("/topic/hive/" + hiveId + "/timer", stateDto);
        
        return stateDto;
    }
    
    @Override
    @Transactional
    public TimerStateDto resumeHiveTimer(String hiveId, String userId) {
        HiveTimer timer = hiveTimerRepository.findByHiveIdOrderByCreatedAtDesc(hiveId).stream()
                .filter(t -> !t.getIsRunning())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No paused timer found"));
        
        timer.setIsRunning(true);
        timer.setPausedAt(null);
        timer = hiveTimerRepository.save(timer);
        
        TimerStateDto stateDto = convertToTimerStateDto(timer);
        messagingTemplate.convertAndSend("/topic/hive/" + hiveId + "/timer", stateDto);
        
        return stateDto;
    }
    
    @Override
    @Transactional
    public TimerStateDto stopHiveTimer(String hiveId, String userId) {
        HiveTimer timer = hiveTimerRepository.findByHiveIdAndIsRunningTrue(hiveId)
                .orElseThrow(() -> new ResourceNotFoundException("No active timer found"));
        
        timer.setIsRunning(false);
        timer.setRemainingSeconds(0);
        timer = hiveTimerRepository.save(timer);
        
        // Clean up old inactive timers
        hiveTimerRepository.deleteInactiveTimers(hiveId);
        
        TimerStateDto stateDto = convertToTimerStateDto(timer);
        messagingTemplate.convertAndSend("/topic/hive/" + hiveId + "/timer/stop", stateDto);
        
        return stateDto;
    }
    
    @Override
    @Transactional(readOnly = true)
    public TimerStateDto getHiveTimerState(String hiveId) {
        return hiveTimerRepository.findByHiveIdAndIsRunningTrue(hiveId)
                .map(this::convertToTimerStateDto)
                .orElse(null);
    }
    
    @Override
    @Transactional
    @Scheduled(fixedRate = 1000) // Every second
    public void updateHiveTimer(String hiveId) {
        // This would be called by a scheduled task to update all active timers
        // For now, simplified implementation
    }
    
    // Helper methods
    
    private void updateDailyStats(String userId, java.util.function.Consumer<ProductivityStats> updater) {
        LocalDate today = LocalDate.now();
        ProductivityStats stats = productivityStatsRepository.findByUserIdAndDate(userId, today)
                .orElseGet(() -> {
                    ProductivityStats newStats = ProductivityStats.builder()
                            .userId(userId)
                            .date(today)
                            .build();
                    return productivityStatsRepository.save(newStats);
                });
        
        updater.accept(stats);
        productivityStatsRepository.save(stats);
    }
    
    private void broadcastSessionUpdate(String hiveId, String userId, String action, FocusSession session) {
        var user = userService.getUserById(userId);
        var update = new SessionUpdate(userId, user.getUsername(), action, 
                session.getSessionType().toString(), session.getDurationMinutes());
        messagingTemplate.convertAndSend("/topic/hive/" + hiveId + "/sessions", update);
    }
    
    private FocusSessionDto convertToDto(FocusSession session) {
        return FocusSessionDto.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .hiveId(session.getHiveId())
                .sessionType(session.getSessionType())
                .durationMinutes(session.getDurationMinutes())
                .actualDurationMinutes(session.getActualDurationMinutes())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .completed(session.getCompleted())
                .interruptions(session.getInterruptions())
                .notes(session.getNotes())
                .createdAt(session.getCreatedAt())
                .build();
    }
    
    private ProductivityStatsDto convertToStatsDto(ProductivityStats stats) {
        int totalMinutes = stats.getTotalFocusMinutes() + stats.getTotalBreakMinutes();
        double focusRatio = totalMinutes > 0 ? 
                (stats.getTotalFocusMinutes() * 100.0 / totalMinutes) : 0;
        double avgSessionLength = stats.getSessionsCompleted() > 0 ? 
                (double) stats.getTotalFocusMinutes() / stats.getSessionsCompleted() : 0;
        
        return ProductivityStatsDto.builder()
                .id(stats.getId())
                .userId(stats.getUserId())
                .date(stats.getDate())
                .totalFocusMinutes(stats.getTotalFocusMinutes())
                .totalBreakMinutes(stats.getTotalBreakMinutes())
                .sessionsCompleted(stats.getSessionsCompleted())
                .sessionsStarted(stats.getSessionsStarted())
                .longestStreakMinutes(stats.getLongestStreakMinutes())
                .dailyGoalMinutes(stats.getDailyGoalMinutes())
                .completionPercentage(stats.getCompletionPercentage())
                .totalMinutes(totalMinutes)
                .focusRatio(focusRatio)
                .averageSessionLength(avgSessionLength)
                .build();
    }
    
    private ProductivityStatsDto createEmptyStatsDto(String userId, LocalDate date) {
        return ProductivityStatsDto.builder()
                .userId(userId)
                .date(date)
                .totalFocusMinutes(0)
                .totalBreakMinutes(0)
                .sessionsCompleted(0)
                .sessionsStarted(0)
                .longestStreakMinutes(0)
                .dailyGoalMinutes(480)
                .completionPercentage(0.0)
                .totalMinutes(0)
                .focusRatio(0.0)
                .averageSessionLength(0.0)
                .build();
    }
    
    private PomodoroSettingsDto convertToSettingsDto(PomodoroSettings settings) {
        return PomodoroSettingsDto.builder()
                .id(settings.getId())
                .userId(settings.getUserId())
                .workDurationMinutes(settings.getWorkDurationMinutes())
                .shortBreakMinutes(settings.getShortBreakMinutes())
                .longBreakMinutes(settings.getLongBreakMinutes())
                .sessionsUntilLongBreak(settings.getSessionsUntilLongBreak())
                .autoStartBreaks(settings.getAutoStartBreaks())
                .autoStartWork(settings.getAutoStartWork())
                .notificationEnabled(settings.getNotificationEnabled())
                .soundEnabled(settings.getSoundEnabled())
                .build();
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
    
    // Inner class for session updates
    private record SessionUpdate(String userId, String username, String action, 
                                String sessionType, Integer duration) {}
}