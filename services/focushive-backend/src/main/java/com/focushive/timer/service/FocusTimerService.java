package com.focushive.timer.service;

import com.focushive.timer.dto.*;
import com.focushive.timer.entity.FocusSession;
import com.focushive.timer.entity.TimerTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service interface for managing focus timer sessions.
 */
public interface FocusTimerService {

    /**
     * Start a new focus timer session.
     */
    FocusSessionResponse startTimer(StartTimerRequest request);

    /**
     * Pause an active timer session.
     */
    FocusSessionResponse pauseTimer(String sessionId, String userId);

    /**
     * Resume a paused timer session.
     */
    FocusSessionResponse resumeTimer(String sessionId, String userId);

    /**
     * Complete a timer session.
     */
    FocusSessionResponse completeTimer(String sessionId, String userId, Integer productivityScore);

    /**
     * Cancel a timer session.
     */
    FocusSessionResponse cancelTimer(String sessionId, String userId, String reason);

    /**
     * Get active session for a user.
     */
    FocusSessionResponse getActiveSession(String userId);

    /**
     * Get session by ID.
     */
    FocusSessionResponse getSession(String sessionId, String userId);

    /**
     * Get user's session history.
     */
    Page<FocusSessionResponse> getUserSessions(String userId, Pageable pageable);

    /**
     * Get sessions for a hive.
     */
    List<FocusSessionResponse> getHiveSessions(String hiveId, FocusSession.SessionStatus status);

    /**
     * Update session metrics.
     */
    FocusSessionResponse updateSessionMetrics(String sessionId, String userId, UpdateSessionMetricsRequest request);

    /**
     * Add note to session.
     */
    FocusSessionResponse addSessionNote(String sessionId, String userId, String note);

    /**
     * Mark task as completed in session.
     */
    FocusSessionResponse markTaskCompleted(String sessionId, String userId, String taskId);

    /**
     * Get user's timer statistics.
     */
    TimerStatisticsResponse getUserStatistics(String userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get hive timer statistics.
     */
    HiveTimerStatisticsResponse getHiveStatistics(String hiveId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Create timer template.
     */
    TimerTemplate createTemplate(CreateTemplateRequest request);

    /**
     * Update timer template.
     */
    TimerTemplate updateTemplate(String templateId, UpdateTemplateRequest request);

    /**
     * Delete timer template.
     */
    void deleteTemplate(String templateId, String userId);

    /**
     * Get user's templates.
     */
    List<TimerTemplate> getUserTemplates(String userId);

    /**
     * Get system templates.
     */
    List<TimerTemplate> getSystemTemplates();

    /**
     * Get public templates.
     */
    List<TimerTemplate> getPublicTemplates();

    /**
     * Set user's default template.
     */
    TimerTemplate setDefaultTemplate(String templateId, String userId);

    /**
     * Handle expired sessions.
     */
    void handleExpiredSessions();

    /**
     * Send session reminders.
     */
    void sendSessionReminders();

    /**
     * Sync session across devices.
     */
    FocusSessionResponse syncSession(String sessionId, String deviceToken);

    /**
     * Get sessions needing sync.
     */
    List<FocusSessionResponse> getSessionsForSync(String userId, LocalDateTime lastSyncTime);

    /**
     * Export user's timer data.
     */
    TimerDataExportResponse exportUserData(String userId, ExportFormat format);

    /**
     * Import timer data.
     */
    TimerDataImportResponse importUserData(String userId, TimerDataImportRequest request);

    /**
     * Validate session access.
     */
    boolean validateSessionAccess(String sessionId, String userId);

    /**
     * Calculate productivity insights.
     */
    ProductivityInsightsResponse getProductivityInsights(String userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get recommended templates based on user behavior.
     */
    List<TimerTemplate> getRecommendedTemplates(String userId);

    /**
     * Handle session state transitions.
     */
    void handleSessionStateTransition(String sessionId, FocusSession.SessionStatus newStatus);

    /**
     * Broadcast timer updates via WebSocket.
     */
    void broadcastTimerUpdate(String sessionId, TimerUpdateType updateType);

    /**
     * Clean up old sessions.
     */
    void cleanupOldSessions(int daysToKeep);

    /**
     * Get session by sync token.
     */
    FocusSessionResponse getSessionBySyncToken(String syncToken);

    /**
     * Generate sync token for session.
     */
    String generateSyncToken(String sessionId);

    /**
     * Validate and refresh sync token.
     */
    String refreshSyncToken(String oldToken);

    /**
     * Get most productive hour for user.
     */
    Integer getMostProductiveHour(String userId);

    /**
     * Get session count by type.
     */
    Map<FocusSession.SessionType, Long> getSessionCountByType(String userId);

    /**
     * Trigger achievement check.
     */
    void checkAndAwardAchievements(String userId, String sessionId);

    /**
     * Calculate streak for user.
     */
    Integer calculateUserStreak(String userId);

    /**
     * Update session with break notification.
     */
    void notifyBreakTime(String sessionId);

    /**
     * Handle Pomodoro cycle completion.
     */
    void handlePomodoroCycle(String sessionId, int cycleNumber);

    /**
     * Get session recommendations.
     */
    SessionRecommendationResponse getSessionRecommendations(String userId);

    /**
     * Validate timer constraints.
     */
    void validateTimerConstraints(StartTimerRequest request);

    /**
     * Update session tags.
     */
    FocusSessionResponse updateSessionTags(String sessionId, String userId, List<String> tags);

    /**
     * Search sessions.
     */
    Page<FocusSessionResponse> searchSessions(SessionSearchRequest request, Pageable pageable);

    /**
     * Get trending focus topics.
     */
    List<String> getTrendingFocusTopics();

    /**
     * Share session summary.
     */
    void shareSessionSummary(String sessionId, String userId, ShareRequest request);

    /**
     * Get session comparisons.
     */
    SessionComparisonResponse compareSessionPerformance(String userId, List<String> sessionIds);

    /**
     * Archive old sessions.
     */
    void archiveSessions(String userId, LocalDateTime beforeDate);

    /**
     * Restore archived sessions.
     */
    void restoreArchivedSessions(String userId, List<String> sessionIds);

    /**
     * Get AI-powered session insights.
     */
    AISessionInsightsResponse getAIInsights(String sessionId);

    /**
     * Handle emergency session stop.
     */
    void emergencyStopAllSessions(String userId);

    /**
     * Validate template access.
     */
    boolean validateTemplateAccess(String templateId, String userId);

    /**
     * Clone template.
     */
    TimerTemplate cloneTemplate(String templateId, String userId);

    /**
     * Get template usage statistics.
     */
    TemplateUsageStatistics getTemplateStatistics(String templateId);

    /**
     * Rate template.
     */
    void rateTemplate(String templateId, String userId, int rating);

    /**
     * Get template recommendations based on context.
     */
    List<TimerTemplate> getContextualTemplateRecommendations(String userId, String context);
}