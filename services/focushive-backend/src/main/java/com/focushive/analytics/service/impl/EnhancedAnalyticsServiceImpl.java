package com.focushive.analytics.service.impl;

import com.focushive.analytics.dto.*;
import com.focushive.analytics.entity.*;
import com.focushive.analytics.enums.AchievementType;
import com.focushive.analytics.enums.ReportPeriod;
import com.focushive.analytics.repository.*;
import com.focushive.analytics.service.EnhancedAnalyticsService;
import com.focushive.common.exception.BadRequestException;
import com.focushive.common.exception.ResourceNotFoundException;
import com.focushive.hive.repository.HiveRepository;
import com.focushive.timer.entity.FocusSession;
import com.focushive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced Analytics Service implementation providing comprehensive productivity tracking,
 * achievement management, goal setting, and detailed reporting capabilities.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EnhancedAnalyticsServiceImpl implements EnhancedAnalyticsService {

    private final ProductivityMetricRepository productivityMetricRepository;
    private final HiveAnalyticsRepository hiveAnalyticsRepository;
    private final UserStreakRepository userStreakRepository;
    private final AchievementProgressRepository achievementProgressRepository;
    private final DailyGoalRepository dailyGoalRepository;
    private final UserRepository userRepository;
    private final HiveRepository hiveRepository;

    // ==================== PRODUCTIVITY METRICS ====================

    @Override
    public ProductivitySummaryResponse getUserProductivitySummary(String userId) {
        log.info("Getting productivity summary for user: {}", userId);

        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        // Get productivity metrics
        List<ProductivityMetric> metrics = productivityMetricRepository
            .findByUserIdAndDateBetween(userId, startDate, endDate);

        // Calculate summary statistics
        int totalFocusMinutes = metrics.stream()
            .mapToInt(ProductivityMetric::getFocusMinutes)
            .sum();

        int totalCompletedSessions = metrics.stream()
            .mapToInt(ProductivityMetric::getCompletedSessions)
            .sum();

        int totalSessions = metrics.stream()
            .mapToInt(ProductivityMetric::getTotalSessions)
            .sum();

        double completionRate = totalSessions > 0 ? (double) totalCompletedSessions / totalSessions * 100 : 0;

        OptionalDouble avgProductivityScore = metrics.stream()
            .filter(m -> m.getProductivityScore() > 0)
            .mapToInt(ProductivityMetric::getProductivityScore)
            .average();

        int averageProductivityScore = (int) avgProductivityScore.orElse(0);

        // Get streak information
        UserStreak streak = userStreakRepository.findByUserId(userId)
            .orElse(new UserStreak());

        // Get goal information
        long goalsAchieved = dailyGoalRepository.countAchievedGoals(userId, startDate, endDate);
        long totalGoals = dailyGoalRepository.countTotalGoals(userId, startDate, endDate);

        // Build daily breakdown
        List<ProductivitySummaryResponse.DailyMetricDto> dailyBreakdown = metrics.stream()
            .map(m -> new ProductivitySummaryResponse.DailyMetricDto(
                m.getDate(),
                m.getFocusMinutes(),
                m.getCompletedSessions(),
                m.getProductivityScore(),
                m.getGoalsAchieved() > 0
            ))
            .collect(Collectors.toList());

        // Calculate trend
        String trend = calculateTrend(metrics);
        double trendPercentage = calculateTrendPercentage(metrics);

        return new ProductivitySummaryResponse(
            userId,
            startDate,
            endDate,
            totalFocusMinutes,
            totalCompletedSessions,
            totalSessions,
            completionRate,
            averageProductivityScore,
            metrics.stream().mapToInt(ProductivityMetric::getProductivityScore).max().orElse(0),
            metrics.stream().filter(m -> m.getProductivityScore() > 0).mapToInt(ProductivityMetric::getProductivityScore).min().orElse(0),
            streak.getCurrentStreak(),
            streak.getLongestStreak(),
            streak.getLastActiveDate(),
            (int) goalsAchieved,
            (int) totalGoals,
            totalGoals > 0 ? (double) goalsAchieved / totalGoals * 100 : 0,
            0, // New achievements (to be calculated)
            totalCompletedSessions > 0 ? totalFocusMinutes / totalCompletedSessions : 0,
            metrics.stream().mapToInt(ProductivityMetric::getBreakMinutes).sum(),
            metrics.stream().mapToInt(ProductivityMetric::getDistractionsCount).sum(),
            metrics.stream().filter(m -> m.getPeakPerformanceHour() != null)
                .mapToInt(ProductivityMetric::getPeakPerformanceHour).max().orElse(0),
            dailyBreakdown,
            new HashMap<>(), // Comparison data
            trend,
            trendPercentage
        );
    }

    @Override
    public DetailedReportResponse getUserDetailedReport(String userId, ReportPeriod period) {
        log.info("Generating detailed report for user: {}, period: {}", userId, period);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(period.getDays());

        List<ProductivityMetric> metrics = productivityMetricRepository
            .findByUserIdAndDateBetween(userId, startDate, endDate);

        // Build detailed response (simplified for now)
        DetailedReportResponse response = new DetailedReportResponse();
        response.setUserId(userId);
        response.setPeriod(period);
        response.setStartDate(startDate);
        response.setEndDate(endDate);

        // Summary stats
        DetailedReportResponse.SummaryStats summary = new DetailedReportResponse.SummaryStats();
        summary.setTotalFocusMinutes(metrics.stream().mapToInt(ProductivityMetric::getFocusMinutes).sum());
        summary.setTotalSessions(metrics.stream().mapToInt(ProductivityMetric::getTotalSessions).sum());
        summary.setCompletedSessions(metrics.stream().mapToInt(ProductivityMetric::getCompletedSessions).sum());
        summary.setAverageProductivityScore((int) metrics.stream()
            .mapToInt(ProductivityMetric::getProductivityScore).average().orElse(0));
        response.setSummary(summary);

        // Daily metrics
        List<DetailedReportResponse.DailyMetricDetail> dailyMetrics = metrics.stream()
            .map(this::buildDailyMetricDetail)
            .collect(Collectors.toList());
        response.setDailyMetrics(dailyMetrics);

        return response;
    }

    @Override
    public int calculateProductivityScore(int focusMinutes, int completedSessions, int distractions, int streakBonus) {
        int baseScore = focusMinutes * 2; // 2 points per focus minute
        int sessionBonus = completedSessions * 10; // 10 points per completed session
        int distractionPenalty = distractions * 5; // -5 points per distraction

        return Math.max(0, baseScore + sessionBonus - distractionPenalty + streakBonus);
    }

    @Override
    @Transactional
    public void updateProductivityMetrics(FocusSession session) {
        if (session.getStatus() != FocusSession.SessionStatus.COMPLETED) {
            return; // Only track completed sessions
        }

        LocalDate sessionDate = session.getStartedAt().toLocalDate();
        String userId = session.getUserId();

        ProductivityMetric metric = productivityMetricRepository
            .findByUserIdAndDate(userId, sessionDate)
            .orElse(new ProductivityMetric());

        if (metric.getId() == null) {
            metric.setUserId(userId);
            metric.setDate(sessionDate);
        }

        // Update session data
        int sessionMinutes = session.getElapsedMinutes() != null ? session.getElapsedMinutes() : 0;
        int distractions = session.getTabSwitches() != null ? session.getTabSwitches() : 0;

        metric.addSessionData(sessionMinutes, true, distractions);

        // Get streak bonus
        UserStreak streak = userStreakRepository.findByUserId(userId).orElse(new UserStreak());
        metric.setStreakBonus(Math.min(streak.getCurrentStreak() * 2, 50)); // Max 50 bonus points

        // Calculate productivity score
        metric.calculateProductivityScore();

        productivityMetricRepository.save(metric);
        log.debug("Updated productivity metrics for user: {} on date: {}", userId, sessionDate);
    }

    // ==================== STREAK MANAGEMENT ====================

    @Override
    public StreakInfoResponse getStreakInformation(String userId) {
        log.info("Getting streak information for user: {}", userId);

        UserStreak streak = userStreakRepository.findByUserId(userId)
            .orElse(createDefaultStreak(userId));

        StreakInfoResponse response = new StreakInfoResponse();
        response.setUserId(userId);
        response.setCurrentStreak(streak.getCurrentStreak());
        response.setLongestStreak(streak.getLongestStreak());
        response.setLastActiveDate(streak.getLastActiveDate());
        response.setStreakStartDate(streak.getStreakStartDate());
        response.setStreakAtRisk(streak.isStreakAtRisk());
        response.setAvailableFreeze(streak.getAvailableStreakFreezes());
        response.setFreezesUsed(streak.getStreakFreezesUsed());
        response.setCanUseFreeze(streak.canUseStreakFreeze());
        response.setTotalActiveDays(streak.getTotalActiveDays());

        // Calculate next milestone
        int[] milestones = {3, 7, 14, 30, 50, 100, 365};
        for (int milestone : milestones) {
            if (streak.getCurrentStreak() < milestone) {
                response.setNextMilestone(milestone);
                response.setDaysToNextMilestone(milestone - streak.getCurrentStreak());
                break;
            }
        }

        // Determine status
        response.setStreakStatus(determineStreakStatus(streak));

        return response;
    }

    @Override
    @Transactional
    public void updateUserStreak(String userId, boolean activeToday) {
        UserStreak streak = userStreakRepository.findByUserId(userId)
            .orElse(createDefaultStreak(userId));

        streak.updateStreak(activeToday);
        userStreakRepository.save(streak);

        log.debug("Updated streak for user: {}, active today: {}, current streak: {}",
            userId, activeToday, streak.getCurrentStreak());
    }

    // ==================== ACHIEVEMENT SYSTEM ====================

    @Override
    public AchievementProgressResponse getUserAchievements(String userId) {
        log.info("Getting achievements for user: {}", userId);

        List<AchievementProgress> progressList = achievementProgressRepository.findByUserId(userId);

        // Ensure all achievement types are tracked
        ensureAllAchievementsTracked(userId, progressList);

        // Reload after ensuring all achievements
        progressList = achievementProgressRepository.findByUserId(userId);

        int totalAchievements = AchievementType.values().length;
        int unlockedCount = (int) progressList.stream().filter(AchievementProgress::isUnlocked).count();
        int inProgressCount = (int) progressList.stream().filter(AchievementProgress::isInProgress).count();

        Long totalPoints = achievementProgressRepository.calculateTotalPoints(userId);

        List<AchievementProgressResponse.AchievementDetail> achievements = progressList.stream()
            .map(this::buildAchievementDetail)
            .collect(Collectors.toList());

        AchievementProgressResponse response = new AchievementProgressResponse();
        response.setUserId(userId);
        response.setTotalAchievements(totalAchievements);
        response.setUnlockedCount(unlockedCount);
        response.setInProgressCount(inProgressCount);
        response.setTotalPoints(totalPoints != null ? totalPoints.intValue() : 0);
        response.setCompletionPercentage((double) unlockedCount / totalAchievements * 100);
        response.setAchievements(achievements);

        return response;
    }

    @Override
    @Transactional
    public boolean checkAndUnlockAchievement(String userId, AchievementType achievementType, int currentValue) {
        Optional<AchievementProgress> existing = achievementProgressRepository
            .findByUserIdAndAchievementType(userId, achievementType);

        if (existing.isPresent() && existing.get().isUnlocked()) {
            return false; // Already unlocked
        }

        if (currentValue >= achievementType.getTargetValue()) {
            AchievementProgress progress = existing.orElse(new AchievementProgress());
            progress.setUserId(userId);
            progress.setAchievementType(achievementType);
            progress.updateProgress(currentValue);
            progress.unlock();

            achievementProgressRepository.save(progress);
            log.info("Achievement unlocked for user {}: {}", userId, achievementType.getName());
            return true;
        }

        return false;
    }

    @Override
    @Transactional
    public void updateAchievementProgress(String userId, AchievementType achievementType, int newValue) {
        AchievementProgress progress = achievementProgressRepository
            .findByUserIdAndAchievementType(userId, achievementType)
            .orElse(new AchievementProgress());

        if (progress.getId() == null) {
            progress.setUserId(userId);
            progress.setAchievementType(achievementType);
        }

        progress.updateProgress(newValue);
        achievementProgressRepository.save(progress);
    }

    // ==================== GOAL SETTING ====================

    @Override
    @Transactional
    public DailyGoal setDailyGoal(String userId, DailyGoalRequest request) {
        log.info("Setting daily goal for user: {}, target: {} minutes", userId, request.getTargetMinutes());

        if (request.getTargetMinutes() <= 0) {
            throw new BadRequestException("Target minutes must be positive");
        }

        DailyGoal goal = dailyGoalRepository.findByUserIdAndDate(userId, request.getDate())
            .orElse(new DailyGoal());

        goal.setUserId(userId);
        goal.setDate(request.getDate());
        goal.setTargetMinutes(request.getTargetMinutes());
        goal.setDescription(request.getDescription());
        goal.setPriority(request.getPriority());

        // Preserve completed minutes if updating existing goal
        if (goal.getCompletedMinutes() == null) {
            goal.setCompletedMinutes(0);
        }

        return dailyGoalRepository.save(goal);
    }

    @Override
    @Transactional
    public void updateGoalProgress(String userId, int additionalMinutes) {
        if (additionalMinutes <= 0) {
            return;
        }

        Optional<DailyGoal> goalOpt = dailyGoalRepository.findByUserIdAndDate(userId, LocalDate.now());
        if (goalOpt.isPresent()) {
            DailyGoal goal = goalOpt.get();
            goal.updateProgress(additionalMinutes);
            dailyGoalRepository.save(goal);

            log.debug("Updated goal progress for user: {}, added: {} minutes, total: {}",
                userId, additionalMinutes, goal.getCompletedMinutes());
        }
    }

    @Override
    public DailyGoalResponse getDailyGoal(String userId) {
        Optional<DailyGoal> goalOpt = dailyGoalRepository.findTodaysGoal(userId);

        if (goalOpt.isEmpty()) {
            return null; // No goal set for today
        }

        DailyGoal goal = goalOpt.get();

        // Build response (simplified)
        DailyGoalResponse response = new DailyGoalResponse();
        response.setUserId(userId);
        response.setGoalId(goal.getId());
        response.setDate(goal.getDate());
        response.setTargetMinutes(goal.getTargetMinutes());
        response.setCompletedMinutes(goal.getCompletedMinutes());
        response.setAchieved(goal.getAchieved());
        response.setAchievedAt(goal.getAchievedAt());
        response.setDescription(goal.getDescription());
        response.setPriority(goal.getPriority());
        response.setCompletionPercentage(goal.getCompletionPercentage());
        response.setRemainingMinutes(goal.getRemainingMinutes());
        response.setOverachieved(goal.isOverachieved());
        response.setStatus(goal.getStatus());
        response.setProgressColor(goal.getProgressColor());
        response.setContributesToStreak(goal.getStreakContribution());

        return response;
    }

    // ==================== HIVE ANALYTICS ====================

    @Override
    public HiveAnalyticsResponse getHiveAnalytics(String hiveId) {
        log.info("Getting hive analytics for hive: {}", hiveId);

        if (!hiveRepository.existsById(hiveId)) {
            throw new ResourceNotFoundException("Hive not found: " + hiveId);
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        List<HiveAnalytics> analytics = hiveAnalyticsRepository
            .findByHiveIdAndDateBetween(hiveId, startDate, endDate);

        // Calculate summary statistics
        int totalActiveUsers = analytics.stream().mapToInt(HiveAnalytics::getActiveUsers).sum();
        int totalFocusTime = analytics.stream().mapToInt(HiveAnalytics::getTotalFocusTime).sum();
        int totalSessions = analytics.stream().mapToInt(HiveAnalytics::getTotalSessions).sum();
        int completedSessions = analytics.stream().mapToInt(HiveAnalytics::getCompletedSessions).sum();

        double completionRate = totalSessions > 0 ? (double) completedSessions / totalSessions * 100 : 0;
        int averageProductivityScore = (int) analytics.stream()
            .mapToInt(HiveAnalytics::getAverageProductivityScore).average().orElse(0);

        HiveAnalyticsResponse response = new HiveAnalyticsResponse();
        response.setHiveId(hiveId);
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        response.setTotalActiveUsers(totalActiveUsers);
        response.setTotalFocusTime(totalFocusTime);
        response.setTotalSessions(totalSessions);
        response.setCompletedSessions(completedSessions);
        response.setCompletionRate(completionRate);
        response.setAverageProductivityScore(averageProductivityScore);
        response.setPeakConcurrentUsers(analytics.stream()
            .mapToInt(HiveAnalytics::getPeakConcurrentUsers).max().orElse(0));

        return response;
    }

    @Override
    @Transactional
    public void updateHiveAnalytics(String hiveId, String userId, int sessionMinutes, int productivityScore) {
        LocalDate today = LocalDate.now();

        HiveAnalytics analytics = hiveAnalyticsRepository.findByHiveIdAndDate(hiveId, today)
            .orElse(new HiveAnalytics());

        if (analytics.getId() == null) {
            analytics.setHiveId(hiveId);
            analytics.setDate(today);
        }

        analytics.addSessionData(sessionMinutes, true, 0, productivityScore);
        hiveAnalyticsRepository.save(analytics);

        log.debug("Updated hive analytics for hive: {}, session: {} minutes", hiveId, sessionMinutes);
    }

    // ==================== DATA EXPORT ====================

    @Override
    public Map<String, Object> exportUserAnalyticsData(String userId) {
        log.info("Exporting analytics data for user: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        Map<String, Object> exportData = new HashMap<>();
        exportData.put("userId", userId);
        exportData.put("exportDate", LocalDateTime.now());

        // Export all data
        exportData.put("productivityMetrics", productivityMetricRepository.findByUserId(userId));
        exportData.put("streaks", userStreakRepository.findByUserId(userId).orElse(null));
        exportData.put("achievements", achievementProgressRepository.findByUserId(userId));
        exportData.put("goals", dailyGoalRepository.findByUserId(userId));

        return exportData;
    }

    // ==================== EVENT PROCESSING ====================

    @Override
    @Transactional
    public void processFocusSessionCompletion(FocusSession session) {
        log.info("Processing focus session completion for user: {}", session.getUserId());

        // Update productivity metrics
        updateProductivityMetrics(session);

        // Update streak
        updateUserStreak(session.getUserId(), true);

        // Update goal progress
        if (session.getElapsedMinutes() != null) {
            updateGoalProgress(session.getUserId(), session.getElapsedMinutes());
        }

        // Update hive analytics if applicable
        if (session.getHiveId() != null) {
            ProductivityMetric todaysMetric = productivityMetricRepository
                .findByUserIdAndDate(session.getUserId(), LocalDate.now())
                .orElse(new ProductivityMetric());
            updateHiveAnalytics(session.getHiveId(), session.getUserId(),
                session.getElapsedMinutes(), todaysMetric.getProductivityScore());
        }

        // Check for achievement unlocks
        checkSessionAchievements(session);
    }

    // ==================== HELPER METHODS ====================

    private UserStreak createDefaultStreak(String userId) {
        UserStreak streak = new UserStreak();
        streak.setUserId(userId);
        streak.setCurrentStreak(0);
        streak.setLongestStreak(0);
        streak.setLastActiveDate(LocalDate.now().minusDays(1));
        streak.setStreakStartDate(LocalDate.now());
        streak.setTotalActiveDays(0);
        streak.setStreakFreezesUsed(0);
        streak.setAvailableStreakFreezes(2);
        return streak;
    }

    private String determineStreakStatus(UserStreak streak) {
        if (streak.getCurrentStreak() == 0) {
            return "NEW";
        } else if (streak.isStreakAtRisk()) {
            return "AT_RISK";
        } else {
            return "ACTIVE";
        }
    }

    private void ensureAllAchievementsTracked(String userId, List<AchievementProgress> existing) {
        Set<AchievementType> existingTypes = existing.stream()
            .map(AchievementProgress::getAchievementType)
            .collect(Collectors.toSet());

        for (AchievementType type : AchievementType.values()) {
            if (!existingTypes.contains(type)) {
                AchievementProgress progress = new AchievementProgress();
                progress.setUserId(userId);
                progress.setAchievementType(type);
                progress.setProgress(0);
                progress.setCurrentValue(0);
                achievementProgressRepository.save(progress);
            }
        }
    }

    private AchievementProgressResponse.AchievementDetail buildAchievementDetail(AchievementProgress progress) {
        AchievementProgressResponse.AchievementDetail detail = new AchievementProgressResponse.AchievementDetail();
        detail.setType(progress.getAchievementType());
        detail.setName(progress.getAchievementName());
        detail.setDescription(progress.getAchievementDescription());
        detail.setCategory(progress.getCategory());
        detail.setProgress(progress.getProgress());
        detail.setCurrentValue(progress.getCurrentValue());
        detail.setTargetValue(progress.getAchievementType().getTargetValue());
        detail.setUnlocked(progress.isUnlocked());
        detail.setUnlockedAt(progress.getUnlockedAt());
        detail.setFirstProgressAt(progress.getFirstProgressAt());
        detail.setPoints(progress.getPoints());
        detail.setEstimatedDaysToUnlock(progress.getEstimatedDaysToUnlock());

        // Determine difficulty and rarity
        detail.setDifficulty(determineDifficulty(progress.getAchievementType()));
        detail.setRarity(determineRarity(progress.getAchievementType()));
        detail.setProgressStatus(determineProgressStatus(progress));

        return detail;
    }

    private DetailedReportResponse.DailyMetricDetail buildDailyMetricDetail(ProductivityMetric metric) {
        DetailedReportResponse.DailyMetricDetail detail = new DetailedReportResponse.DailyMetricDetail();
        detail.setDate(metric.getDate());
        detail.setDayOfWeek(metric.getDate().getDayOfWeek().name());
        detail.setFocusMinutes(metric.getFocusMinutes());
        detail.setSessions(metric.getTotalSessions());
        detail.setCompletedSessions(metric.getCompletedSessions());
        detail.setProductivityScore(metric.getProductivityScore());
        detail.setDistractions(metric.getDistractionsCount());
        detail.setBreakMinutes(metric.getBreakMinutes());
        detail.setGoalAchieved(metric.getGoalsAchieved() > 0);
        // Additional fields would be populated from related entities
        return detail;
    }

    private String calculateTrend(List<ProductivityMetric> metrics) {
        if (metrics.size() < 7) {
            return "STABLE";
        }

        // Simple trend calculation based on last week vs previous week
        List<ProductivityMetric> sortedMetrics = metrics.stream()
            .sorted(Comparator.comparing(ProductivityMetric::getDate))
            .collect(Collectors.toList());

        int halfPoint = sortedMetrics.size() / 2;
        double firstHalfAvg = sortedMetrics.subList(0, halfPoint).stream()
            .mapToInt(ProductivityMetric::getProductivityScore)
            .average().orElse(0);

        double secondHalfAvg = sortedMetrics.subList(halfPoint, sortedMetrics.size()).stream()
            .mapToInt(ProductivityMetric::getProductivityScore)
            .average().orElse(0);

        if (secondHalfAvg > firstHalfAvg * 1.1) {
            return "IMPROVING";
        } else if (secondHalfAvg < firstHalfAvg * 0.9) {
            return "DECLINING";
        } else {
            return "STABLE";
        }
    }

    private double calculateTrendPercentage(List<ProductivityMetric> metrics) {
        if (metrics.size() < 2) {
            return 0.0;
        }

        List<ProductivityMetric> sortedMetrics = metrics.stream()
            .sorted(Comparator.comparing(ProductivityMetric::getDate))
            .collect(Collectors.toList());

        int halfPoint = sortedMetrics.size() / 2;
        double firstHalfAvg = sortedMetrics.subList(0, halfPoint).stream()
            .mapToInt(ProductivityMetric::getProductivityScore)
            .average().orElse(0);

        double secondHalfAvg = sortedMetrics.subList(halfPoint, sortedMetrics.size()).stream()
            .mapToInt(ProductivityMetric::getProductivityScore)
            .average().orElse(0);

        if (firstHalfAvg == 0) {
            return 0.0;
        }

        return ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100;
    }

    private String determineDifficulty(AchievementType type) {
        return switch (type.getCategory()) {
            case "Getting Started" -> "EASY";
            case "Session Milestones", "Social" -> "MEDIUM";
            case "Consistency", "Performance" -> "HARD";
            case "Endurance", "Special" -> "LEGENDARY";
            default -> "MEDIUM";
        };
    }

    private String determineRarity(AchievementType type) {
        return switch (type.getTargetValue()) {
            case 1, 3, 7, 10 -> "COMMON";
            case 30, 50 -> "UNCOMMON";
            case 90, 100 -> "RARE";
            case 180, 300 -> "EPIC";
            default -> "LEGENDARY";
        };
    }

    private String determineProgressStatus(AchievementProgress progress) {
        if (progress.isUnlocked()) {
            return "UNLOCKED";
        } else if (progress.getProgress() >= 90) {
            return "NEAR_COMPLETION";
        } else if (progress.getProgress() > 0) {
            return "IN_PROGRESS";
        } else {
            return "NOT_STARTED";
        }
    }

    private void checkSessionAchievements(FocusSession session) {
        String userId = session.getUserId();

        // Check for first focus achievement
        checkAndUnlockAchievement(userId, AchievementType.FIRST_FOCUS, 1);

        // Check time-based achievements
        LocalDateTime completedAt = session.getCompletedAt();
        if (completedAt != null) {
            int hour = completedAt.getHour();
            if (hour < 7) {
                checkAndUnlockAchievement(userId, AchievementType.EARLY_BIRD, 1);
            } else if (hour >= 22) {
                checkAndUnlockAchievement(userId, AchievementType.NIGHT_OWL, 1);
            }
        }

        // Check session duration achievements
        if (session.getElapsedMinutes() != null) {
            int minutes = session.getElapsedMinutes();
            if (minutes >= 180) {
                checkAndUnlockAchievement(userId, AchievementType.MARATHON_RUNNER, minutes);
            }
            if (minutes >= 300) {
                checkAndUnlockAchievement(userId, AchievementType.ULTRA_RUNNER, minutes);
            }
            if (minutes >= 480) {
                checkAndUnlockAchievement(userId, AchievementType.ENDURANCE_MASTER, minutes);
            }
        }

        // Additional achievement checks would be implemented here
    }
}