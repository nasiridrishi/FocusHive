package com.focushive.buddy.service;

import com.focushive.buddy.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing buddy goals and milestones.
 * Handles goal creation, progress tracking, milestone management,
 * achievements, analytics, and goal synchronization between partners.
 */
public interface BuddyGoalService {

    // =========================
    // Goal Creation & Management
    // =========================

    /**
     * Creates a new individual goal for a user.
     *
     * @param goalDto the goal creation data
     * @return created goal response
     */
    GoalResponseDto createIndividualGoal(GoalCreationDto goalDto);

    /**
     * Creates a new shared goal between buddy partners.
     *
     * @param goalDto the goal creation data
     * @return created goal response
     */
    GoalResponseDto createSharedGoal(GoalCreationDto goalDto);

    /**
     * Updates an existing goal.
     *
     * @param goalId the goal ID to update
     * @param goalDto the updated goal data
     * @param userId the user making the update
     * @return updated goal response
     */
    GoalResponseDto updateGoal(UUID goalId, GoalCreationDto goalDto, UUID userId);

    /**
     * Deletes a goal (soft delete with archiving).
     *
     * @param goalId the goal ID to delete
     * @param userId the user requesting deletion
     */
    void deleteGoal(UUID goalId, UUID userId);

    /**
     * Creates a goal from a template.
     *
     * @param templateId the template ID to use
     * @param customization any customizations to apply
     * @param userId the user creating the goal
     * @return created goal response
     */
    GoalResponseDto cloneGoalFromTemplate(UUID templateId, GoalCreationDto customization, UUID userId);

    /**
     * Validates goal parameters for business rules.
     *
     * @param goalDto the goal data to validate
     * @param userId the user creating/updating the goal
     * @return validation result with any errors
     */
    ValidationResultDto validateGoalParameters(GoalCreationDto goalDto, UUID userId);

    /**
     * Enforces goal limits per user/partnership.
     *
     * @param userId the user ID
     * @param partnershipId the partnership ID (optional)
     * @return true if under limits, false otherwise
     */
    Boolean enforceGoalLimits(UUID userId, UUID partnershipId);

    /**
     * Handles goal duplication detection and prevention.
     *
     * @param goalDto the goal to check for duplicates
     * @param userId the user creating the goal
     * @return list of potential duplicates
     */
    List<GoalResponseDto> handleGoalDuplication(GoalCreationDto goalDto, UUID userId);

    // =========================
    // Milestone Management
    // =========================

    /**
     * Adds a milestone to a goal.
     *
     * @param goalId the goal ID
     * @param milestoneDto the milestone data
     * @param userId the user adding the milestone
     * @return created milestone
     */
    MilestoneDto addMilestone(UUID goalId, MilestoneDto milestoneDto, UUID userId);

    /**
     * Updates an existing milestone.
     *
     * @param milestoneId the milestone ID
     * @param milestoneDto the updated milestone data
     * @param userId the user making the update
     * @return updated milestone
     */
    MilestoneDto updateMilestone(UUID milestoneId, MilestoneDto milestoneDto, UUID userId);

    /**
     * Completes a milestone.
     *
     * @param milestoneId the milestone ID
     * @param userId the user completing the milestone
     * @param completionNotes optional completion notes
     * @return updated milestone
     */
    MilestoneDto completeMilestone(UUID milestoneId, UUID userId, String completionNotes);

    /**
     * Reorders milestones within a goal.
     *
     * @param goalId the goal ID
     * @param milestoneIds ordered list of milestone IDs
     * @param userId the user making the reorder
     * @return updated list of milestones
     */
    List<MilestoneDto> reorderMilestones(UUID goalId, List<UUID> milestoneIds, UUID userId);

    /**
     * Validates milestone progress and dependencies.
     *
     * @param milestoneId the milestone ID
     * @param userId the user requesting validation
     * @return validation result
     */
    ValidationResultDto validateMilestoneProgress(UUID milestoneId, UUID userId);

    /**
     * Calculates milestone completion percentage for a goal.
     *
     * @param goalId the goal ID
     * @return completion percentage
     */
    Integer calculateMilestoneCompletion(UUID goalId);

    // =========================
    // Progress Tracking
    // =========================

    /**
     * Updates goal progress.
     *
     * @param progressDto the progress update data
     * @return updated goal response
     */
    GoalResponseDto updateProgress(ProgressUpdateDto progressDto);

    /**
     * Calculates overall progress for a goal.
     *
     * @param goalId the goal ID
     * @return overall progress percentage
     */
    Integer calculateOverallProgress(UUID goalId);

    /**
     * Records daily progress tracking.
     *
     * @param goalId the goal ID
     * @param progressPercentage the progress percentage
     * @param userId the user updating progress
     * @param notes optional progress notes
     */
    void trackDailyProgress(UUID goalId, Integer progressPercentage, UUID userId, String notes);

    /**
     * Generates a progress report for a goal.
     *
     * @param goalId the goal ID
     * @param startDate the start date for the report
     * @param endDate the end date for the report
     * @return progress report
     */
    GoalAnalyticsDto generateProgressReport(UUID goalId, LocalDate startDate, LocalDate endDate);

    /**
     * Detects progress stagnation for a goal.
     *
     * @param goalId the goal ID
     * @param daysThreshold threshold for stagnation detection
     * @return true if stagnant, false otherwise
     */
    Boolean detectProgressStagnation(UUID goalId, Integer daysThreshold);

    /**
     * Suggests progress interventions for stagnant goals.
     *
     * @param goalId the goal ID
     * @return list of suggested interventions
     */
    List<String> suggestProgressInterventions(UUID goalId);

    /**
     * Compares progress between partners for shared goals.
     *
     * @param goalId the shared goal ID
     * @return comparison analytics
     */
    GoalAnalyticsDto.CollaborationAnalyticsDto comparePartnerProgress(UUID goalId);

    /**
     * Predicts goal completion date using ML.
     *
     * @param goalId the goal ID
     * @return predicted completion date analytics
     */
    GoalAnalyticsDto.PredictiveAnalyticsDto predictCompletionDate(UUID goalId);

    // =========================
    // Achievement & Celebration
    // =========================

    /**
     * Celebrates goal completion with achievements and notifications.
     *
     * @param goalId the completed goal ID
     * @param userId the user who completed the goal
     * @return list of achievements earned
     */
    List<AchievementDto> celebrateGoalCompletion(UUID goalId, UUID userId);

    /**
     * Awards achievements for goal-related activities.
     *
     * @param userId the user ID
     * @param achievementType the type of achievement
     * @param relatedEntityId the related goal/milestone ID
     * @param metadata additional achievement metadata
     * @return awarded achievement
     */
    AchievementDto awardAchievement(UUID userId, String achievementType, UUID relatedEntityId, Object metadata);

    /**
     * Generates personalized celebration messages.
     *
     * @param achievementDto the achievement to celebrate
     * @return celebration message
     */
    String generateCelebrationMessage(AchievementDto achievementDto);

    /**
     * Shares achievement on social platforms or with partners.
     *
     * @param achievementId the achievement ID
     * @param shareSettings the sharing preferences
     * @param userId the user sharing the achievement
     */
    void shareAchievement(UUID achievementId, AchievementDto.ShareSettings shareSettings, UUID userId);

    /**
     * Tracks achievement history for a user.
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return page of achievements
     */
    Page<AchievementDto> trackAchievementHistory(UUID userId, Pageable pageable);

    // =========================
    // Goal Synchronization
    // =========================

    /**
     * Synchronizes shared goals between partners.
     *
     * @param partnershipId the partnership ID
     * @return list of synchronized goals
     */
    List<GoalResponseDto> syncSharedGoals(UUID partnershipId);

    /**
     * Handles conflicting updates to shared goals.
     *
     * @param goalId the goal ID with conflicts
     * @param conflictResolution the resolution strategy
     * @param userId the user resolving the conflict
     * @return resolved goal
     */
    GoalResponseDto handleConflictingUpdates(UUID goalId, String conflictResolution, UUID userId);

    /**
     * Merges goal progress from both partners.
     *
     * @param goalId the shared goal ID
     * @return merged progress data
     */
    ProgressUpdateDto mergeGoalProgress(UUID goalId);

    /**
     * Notifies partner of goal changes in real-time.
     *
     * @param goalId the goal ID that changed
     * @param changeType the type of change
     * @param userId the user who made the change
     */
    void notifyPartnerOfChanges(UUID goalId, String changeType, UUID userId);

    /**
     * Handles partner goal abandonment scenarios.
     *
     * @param goalId the goal ID being abandoned
     * @param abandoningUserId the user abandoning the goal
     * @param reason the reason for abandonment
     */
    void handlePartnerGoalAbandonment(UUID goalId, UUID abandoningUserId, String reason);

    /**
     * Maintains goal consistency and data integrity.
     *
     * @param goalId the goal ID to check
     * @return consistency check result
     */
    ValidationResultDto maintainGoalConsistency(UUID goalId);

    // =========================
    // Analytics & Insights
    // =========================

    /**
     * Generates comprehensive goal analytics.
     *
     * @param goalId the goal ID
     * @return complete analytics data
     */
    GoalAnalyticsDto generateGoalAnalytics(UUID goalId);

    /**
     * Identifies success patterns in user goals.
     *
     * @param userId the user ID
     * @return identified patterns and insights
     */
    List<GoalAnalyticsDto.InsightDto> identifySuccessPatterns(UUID userId);

    /**
     * Suggests optimal goals based on user profile and history.
     *
     * @param userId the user ID
     * @param category optional goal category filter
     * @return list of suggested goals
     */
    List<GoalTemplateDto> suggestOptimalGoals(UUID userId, String category);

    /**
     * Calculates goal difficulty score.
     *
     * @param goalDto the goal to score
     * @return difficulty score (1-5)
     */
    Integer calculateGoalDifficulty(GoalCreationDto goalDto);

    /**
     * Compares with community average performance.
     *
     * @param goalId the goal ID
     * @return comparative analytics
     */
    GoalAnalyticsDto.ComparativeAnalyticsDto compareWithCommunityAverage(UUID goalId);

    /**
     * Generates insightful feedback for goal performance.
     *
     * @param goalId the goal ID
     * @return actionable insights and recommendations
     */
    List<GoalAnalyticsDto.RecommendationDto> generateInsightfulFeedback(UUID goalId);

    // =========================
    // Goal Templates & Suggestions
    // =========================

    /**
     * Gets available goal templates.
     *
     * @param category optional category filter
     * @param difficulty optional difficulty filter
     * @param pageable pagination parameters
     * @return page of goal templates
     */
    Page<GoalTemplateDto> getGoalTemplates(String category, Integer difficulty, Pageable pageable);

    /**
     * Suggests goals based on user profile and preferences.
     *
     * @param userId the user ID
     * @param maxSuggestions maximum number of suggestions
     * @return list of personalized goal suggestions
     */
    List<GoalTemplateDto> suggestGoalsBasedOnProfile(UUID userId, Integer maxSuggestions);

    /**
     * Customizes a goal template for a specific user.
     *
     * @param templateId the template ID
     * @param customizations user-specific customizations
     * @param userId the user customizing the template
     * @return customized template
     */
    GoalTemplateDto customizeTemplate(UUID templateId, GoalCreationDto customizations, UUID userId);

    /**
     * Rates a goal template based on user experience.
     *
     * @param templateId the template ID
     * @param rating the rating (1-5 stars)
     * @param feedback optional text feedback
     * @param userId the user providing the rating
     */
    void rateGoalTemplate(UUID templateId, Integer rating, String feedback, UUID userId);

    /**
     * Tracks template effectiveness and success rates.
     *
     * @param templateId the template ID
     * @return effectiveness metrics
     */
    GoalTemplateDto.TemplateStatisticsDto trackTemplateEffectiveness(UUID templateId);

    // =========================
    // Utility & Query Methods
    // =========================

    /**
     * Gets a goal by ID with full details.
     *
     * @param goalId the goal ID
     * @param userId the requesting user ID
     * @return goal details
     */
    GoalResponseDto getGoalById(UUID goalId, UUID userId);

    /**
     * Gets goals for a user with filtering and pagination.
     *
     * @param userId the user ID
     * @param status optional status filter
     * @param pageable pagination parameters
     * @return page of goals
     */
    Page<GoalResponseDto> getGoalsForUser(UUID userId, String status, Pageable pageable);

    /**
     * Gets shared goals for a partnership.
     *
     * @param partnershipId the partnership ID
     * @param pageable pagination parameters
     * @return page of shared goals
     */
    Page<GoalResponseDto> getSharedGoalsForPartnership(UUID partnershipId, Pageable pageable);

    /**
     * Gets milestones for a goal.
     *
     * @param goalId the goal ID
     * @param includeCompleted whether to include completed milestones
     * @return list of milestones
     */
    List<MilestoneDto> getMilestonesForGoal(UUID goalId, Boolean includeCompleted);

    /**
     * Searches goals by criteria.
     *
     * @param searchCriteria the search criteria
     * @param userId the searching user ID
     * @param pageable pagination parameters
     * @return page of matching goals
     */
    Page<GoalResponseDto> searchGoals(GoalSearchCriteria searchCriteria, UUID userId, Pageable pageable);

    /**
     * Finds goals with upcoming deadlines within specified days.
     *
     * @param reminderDays the number of days before deadline to consider
     * @return list of goals with upcoming deadlines
     */
    List<GoalResponseDto> findGoalsWithUpcomingDeadlines(int reminderDays);

    /**
     * DTO for goal search criteria
     */
    class GoalSearchCriteria {
        private String title;
        private String category;
        private String status;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer minProgress;
        private Integer maxProgress;
        private List<String> tags;
    }

    /**
     * DTO for validation results
     */
    class ValidationResultDto {
        public Boolean isValid;
        public List<String> errors;
        public List<String> warnings;
        public String summary;

        public ValidationResultDto() {}

        public ValidationResultDto(Boolean isValid, List<String> errors, List<String> warnings) {
            this.isValid = isValid;
            this.errors = errors;
            this.warnings = warnings;
        }
    }
}