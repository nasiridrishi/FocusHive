package com.focushive.timer.service;

import com.focushive.timer.dto.*;
import com.focushive.timer.entity.FocusSession;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for timer and productivity tracking functionality.
 */
public interface TimerService {
    
    // Session Management
    FocusSessionDto startSession(String userId, StartSessionRequest request);
    FocusSessionDto endSession(String userId, String sessionId);
    FocusSessionDto pauseSession(String userId, String sessionId);
    FocusSessionDto resumeSession(String userId, String sessionId);
    FocusSessionDto getCurrentSession(String userId);
    List<FocusSessionDto> getSessionHistory(String userId, int page, int size);
    
    // Productivity Stats
    ProductivityStatsDto getDailyStats(String userId, LocalDate date);
    List<ProductivityStatsDto> getWeeklyStats(String userId, LocalDate startDate);
    List<ProductivityStatsDto> getMonthlyStats(String userId, int year, int month);
    Integer getCurrentStreak(String userId);
    
    // Pomodoro Settings
    PomodoroSettingsDto getPomodoroSettings(String userId);
    PomodoroSettingsDto updatePomodoroSettings(String userId, PomodoroSettingsDto settings);
    PomodoroSettingsDto createDefaultSettings(String userId);
    
    // Hive Timer Management
    TimerStateDto startHiveTimer(String hiveId, String userId, TimerStateDto timerRequest);
    TimerStateDto pauseHiveTimer(String hiveId, String userId);
    TimerStateDto resumeHiveTimer(String hiveId, String userId);
    TimerStateDto stopHiveTimer(String hiveId, String userId);
    TimerStateDto getHiveTimerState(String hiveId);
    void updateHiveTimer(String hiveId); // Called by scheduled task
}