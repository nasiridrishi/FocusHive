package com.focushive.analytics.service;

import com.focushive.analytics.dto.SessionRequest;
import com.focushive.analytics.dto.SessionResponse;
import com.focushive.analytics.dto.UserStats;
import com.focushive.analytics.entity.DailySummary;
import com.focushive.analytics.repository.DailySummaryRepository;
import com.focushive.analytics.controller.AnalyticsController.EndSessionRequest;
import com.focushive.analytics.controller.AnalyticsController.LeaderboardEntry;
import com.focushive.analytics.controller.AnalyticsController.TimePeriod;
import com.focushive.common.exception.ResourceNotFoundException;
import com.focushive.common.exception.BadRequestException;
import com.focushive.timer.entity.FocusSession;
import com.focushive.timer.repository.FocusSessionRepository;
import com.focushive.user.entity.User;
import com.focushive.user.repository.UserRepository;
import com.focushive.hive.entity.HiveMember;
import com.focushive.hive.repository.HiveMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {
    
    private final FocusSessionRepository focusSessionRepository;
    private final DailySummaryRepository dailySummaryRepository;
    private final UserRepository userRepository;
    private final HiveMemberRepository hiveMemberRepository;
    
    @Transactional
    public SessionResponse startSession(SessionRequest request, String userId) {
        log.info("Starting session for user: {} with duration: {} minutes", userId, request.getTargetDurationMinutes());
        
        // Check if user already has an active session
        Optional<FocusSession> activeSession = focusSessionRepository.findByUserIdAndCompletedFalse(userId);
        if (activeSession.isPresent()) {
            throw new BadRequestException("User already has an active session. Please end the current session first.");
        }
        
        // Validate hive membership if hiveId is provided
        if (request.getHiveId() != null && !request.getHiveId().isEmpty()) {
            boolean isMember = hiveMemberRepository.existsByHiveIdAndUserId(request.getHiveId(), userId);
            if (!isMember) {
                throw new BadRequestException("User is not a member of the specified hive");
            }
        }
        
        // Create new focus session
        FocusSession session = FocusSession.builder()
            .userId(userId)
            .hiveId(request.getHiveId())
            .sessionType(request.getType())
            .durationMinutes(request.getTargetDurationMinutes())
            .startTime(LocalDateTime.now())
            .completed(false)
            .interruptions(0)
            .notes(request.getNotes())
            .build();
        
        FocusSession savedSession = focusSessionRepository.save(session);
        log.info("Session started successfully with ID: {}", savedSession.getId());
        
        return new SessionResponse(savedSession);
    }
    
    @Transactional
    public SessionResponse endSession(String sessionId, EndSessionRequest request, String userId) {
        log.info("Ending session: {} for user: {}", sessionId, userId);
        
        // Find the session and verify ownership
        FocusSession session = focusSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        
        if (!session.getUserId().equals(userId)) {
            throw new BadRequestException("User can only end their own sessions");
        }
        
        if (session.getCompleted()) {
            throw new BadRequestException("Session is already completed");
        }
        
        // Update session with completion data
        LocalDateTime endTime = LocalDateTime.now();
        session.setEndTime(endTime);
        session.setCompleted(request.completed() != null ? request.completed() : true);
        session.setActualDurationMinutes(request.actualDurationMinutes() != null ? 
            request.actualDurationMinutes() : 
            (int) ChronoUnit.MINUTES.between(session.getStartTime(), endTime));
        
        // Update interruptions if provided (maps to distractionsLogged in the request)
        if (request.distractionsLogged() != null) {
            session.setInterruptions(request.distractionsLogged());
        }
        
        // Update notes if provided
        if (request.notes() != null && !request.notes().trim().isEmpty()) {
            session.setNotes(request.notes());
        }
        
        FocusSession savedSession = focusSessionRepository.save(session);
        log.info("Session ended successfully. Duration: {} minutes", savedSession.getActualDurationMinutes());
        
        // Update daily summary
        updateDailySummary(savedSession);
        
        return new SessionResponse(savedSession);
    }
    
    public UserStats getUserStats(String userId, LocalDate startDate, LocalDate endDate) {
        log.info("Getting user stats for user: {} from {} to {}", userId, startDate, endDate);
        
        // Verify user exists
        userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Set default date range if not provided (last 30 days)
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date");
        }
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        
        // Get sessions in date range
        List<FocusSession> sessions = focusSessionRepository.findByUserIdAndDateRange(
            userId, startDateTime, endDateTime);
        
        // Calculate statistics
        UserStats stats = new UserStats();
        stats.setUserId(userId);
        stats.setStartDate(startDate);
        stats.setEndDate(endDate);
        
        int totalSessions = sessions.size();
        int completedSessions = (int) sessions.stream().filter(FocusSession::getCompleted).count();
        
        stats.setTotalSessions(totalSessions);
        stats.setCompletedSessions(completedSessions);
        stats.setCompletionRate(totalSessions > 0 ? (double) completedSessions / totalSessions * 100 : 0.0);
        
        // Calculate total minutes from completed sessions
        int totalMinutes = sessions.stream()
            .filter(FocusSession::getCompleted)
            .mapToInt(s -> s.getActualDurationMinutes() != null ? s.getActualDurationMinutes() : 0)
            .sum();
        stats.setTotalMinutes(totalMinutes);
        
        // Calculate average session length
        double averageLength = completedSessions > 0 ? (double) totalMinutes / completedSessions : 0.0;
        stats.setAverageSessionLength(averageLength);
        
        // Calculate streaks using daily summaries
        List<DailySummary> dailySummaries = dailySummaryRepository.findByUserIdAndDateRange(
            userId, startDate, endDate);
        
        int[] streaks = calculateStreaks(dailySummaries);
        stats.setCurrentStreak(streaks[0]);
        stats.setLongestStreak(streaks[1]);
        
        // Group sessions by type
        Map<String, Integer> sessionsByType = sessions.stream()
            .filter(FocusSession::getCompleted)
            .collect(Collectors.groupingBy(
                s -> s.getSessionType().toString(),
                Collectors.summingInt(s -> 1)
            ));
        stats.setSessionsByType(sessionsByType);
        
        // Group minutes by hive
        Map<String, Integer> minutesByHive = sessions.stream()
            .filter(FocusSession::getCompleted)
            .filter(s -> s.getHiveId() != null)
            .collect(Collectors.groupingBy(
                FocusSession::getHiveId,
                Collectors.summingInt(s -> s.getActualDurationMinutes() != null ? s.getActualDurationMinutes() : 0)
            ));
        stats.setMinutesByHive(minutesByHive);
        
        log.info("Calculated stats for user {}: {} total sessions, {} completed, {}% completion rate", 
            userId, totalSessions, completedSessions, stats.getCompletionRate());
        
        return stats;
    }
    
    public List<LeaderboardEntry> getHiveLeaderboard(String hiveId, TimePeriod period) {
        log.info("Getting leaderboard for hive: {} with period: {}", hiveId, period);
        
        // Calculate date range based on period
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = switch (period) {
            case DAY -> endDate;
            case WEEK -> endDate.minusWeeks(1);
            case MONTH -> endDate.minusMonths(1);
            case ALL_TIME -> LocalDate.of(2020, 1, 1); // Far past date
        };
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        
        // Get all hive members using pagination to get all results
        List<HiveMember> members = hiveMemberRepository.findByHiveId(hiveId, Pageable.unpaged()).getContent();
        if (members.isEmpty()) {
            log.warn("No members found for hive: {}", hiveId);
            return new ArrayList<>();
        }
        
        // Calculate stats for each member
        List<LeaderboardEntry> entries = new ArrayList<>();
        
        for (HiveMember member : members) {
            User user = member.getUser();
            if (user == null) {
                continue;
            }
            String userId = user.getId();
            
            // Get sessions for this user in the hive during the period
            List<FocusSession> userSessions = focusSessionRepository.findByUserIdAndDateRange(
                userId, startDateTime, endDateTime)
                .stream()
                .filter(session -> hiveId.equals(session.getHiveId()))
                .filter(FocusSession::getCompleted)
                .toList();
            
            if (!userSessions.isEmpty()) {
                int totalMinutes = userSessions.stream()
                    .mapToInt(s -> s.getActualDurationMinutes() != null ? s.getActualDurationMinutes() : 0)
                    .sum();
                
                int sessionsCompleted = userSessions.size();
                
                // Calculate completion rate (sessions completed vs total sessions attempted)
                List<FocusSession> allUserSessions = focusSessionRepository.findByUserIdAndDateRange(
                    userId, startDateTime, endDateTime)
                    .stream()
                    .filter(session -> hiveId.equals(session.getHiveId()))
                    .toList();
                
                double completionRate = allUserSessions.isEmpty() ? 0.0 : 
                    (double) sessionsCompleted / allUserSessions.size() * 100;
                
                LeaderboardEntry entry = new LeaderboardEntry(
                    userId,
                    user.getDisplayName(),
                    totalMinutes,
                    sessionsCompleted,
                    completionRate,
                    0 // rank will be set after sorting
                );
                entries.add(entry);
            }
        }
        
        // Sort by total minutes descending, then by completion rate
        entries.sort((a, b) -> {
            int minutesComparison = b.totalMinutes().compareTo(a.totalMinutes());
            if (minutesComparison != 0) {
                return minutesComparison;
            }
            return b.completionRate().compareTo(a.completionRate());
        });
        
        // Assign ranks
        List<LeaderboardEntry> rankedEntries = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry original = entries.get(i);
            LeaderboardEntry ranked = new LeaderboardEntry(
                original.userId(),
                original.username(),
                original.totalMinutes(),
                original.sessionsCompleted(),
                original.completionRate(),
                i + 1
            );
            rankedEntries.add(ranked);
        }
        
        log.info("Generated leaderboard for hive {} with {} entries", hiveId, rankedEntries.size());
        return rankedEntries;
    }
    
    private void updateDailySummary(FocusSession session) {
        LocalDate sessionDate = session.getStartTime().toLocalDate();
        String userId = session.getUserId();
        
        DailySummary summary = dailySummaryRepository.findByUserIdAndDate(userId, sessionDate)
            .orElse(new DailySummary());
        
        if (summary.getId() == null) {
            // New summary
            summary.setUserId(userId);
            summary.setDate(sessionDate);
        }
        
        // Update session counts
        summary.setSessionsCount(summary.getSessionsCount() + 1);
        if (session.getCompleted()) {
            summary.setCompletedSessions(summary.getCompletedSessions() + 1);
        }
        
        // Update minutes based on session type and completion
        if (session.getCompleted() && session.getActualDurationMinutes() != null) {
            int duration = session.getActualDurationMinutes();
            summary.setTotalMinutes(summary.getTotalMinutes() + duration);
            
            if (session.getSessionType() == FocusSession.SessionType.WORK || 
                session.getSessionType() == FocusSession.SessionType.STUDY) {
                summary.setFocusMinutes(summary.getFocusMinutes() + duration);
            } else if (session.getSessionType() == FocusSession.SessionType.BREAK) {
                summary.setBreakMinutes(summary.getBreakMinutes() + duration);
            }
        }
        
        // Update interruptions/distractions
        if (session.getInterruptions() != null) {
            summary.setDistractionsCount(summary.getDistractionsCount() + session.getInterruptions());
        }
        
        // Recalculate average session length
        if (summary.getCompletedSessions() > 0) {
            summary.setAverageSessionLength(summary.getTotalMinutes() / summary.getCompletedSessions());
        }
        
        // Calculate productivity score (simple algorithm)
        if (summary.getSessionsCount() > 0) {
            double completionRate = (double) summary.getCompletedSessions() / summary.getSessionsCount();
            int productivityScore = (int) (completionRate * 100);
            // Factor in distractions (reduce score if too many distractions)
            if (summary.getDistractionsCount() > summary.getCompletedSessions() * 2) {
                productivityScore = Math.max(0, productivityScore - 10);
            }
            summary.setProductivityScore(Math.min(100, productivityScore));
        }
        
        dailySummaryRepository.save(summary);
        // Removed debug log to reduce verbosity and avoid logging user data frequently
    }
    
    private int[] calculateStreaks(List<DailySummary> summaries) {
        if (summaries.isEmpty()) {
            return new int[]{0, 0};
        }
        
        // Sort by date descending
        summaries.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        
        int currentStreak = 0;
        int longestStreak = 0;
        int tempStreak = 0;
        
        LocalDate expectedDate = LocalDate.now();
        boolean streakActive = true;
        
        for (DailySummary summary : summaries) {
            // Only count days with actual focus time
            if (summary.getFocusMinutes() > 0) {
                if (summary.getDate().equals(expectedDate)) {
                    tempStreak++;
                    if (streakActive) {
                        currentStreak = tempStreak;
                    }
                    expectedDate = expectedDate.minusDays(1);
                } else if (summary.getDate().isBefore(expectedDate)) {
                    // Gap in streak
                    longestStreak = Math.max(longestStreak, tempStreak);
                    tempStreak = 1;
                    expectedDate = summary.getDate().minusDays(1);
                    streakActive = false;
                }
            }
        }
        
        longestStreak = Math.max(longestStreak, tempStreak);
        
        return new int[]{currentStreak, longestStreak};
    }
}