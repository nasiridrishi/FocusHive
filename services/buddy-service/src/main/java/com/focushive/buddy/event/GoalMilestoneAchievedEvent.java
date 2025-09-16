package com.focushive.buddy.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Event fired when a goal milestone is achieved.
 *
 * Triggers:
 * - Celebration notifications to user and partner
 * - Achievement badge awarding
 * - Progress analytics updates
 * - Social sharing opportunities
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GoalMilestoneAchievedEvent extends ApplicationEvent {

    private final UUID milestoneId;
    private final UUID goalId;
    private final UUID userId;
    private final UUID partnershipId;
    private final String milestoneTitle;
    private final Integer progressPercentage;
    private final ZonedDateTime achievedAt;
    private final String celebrationMessage;

    public GoalMilestoneAchievedEvent(Object source, UUID milestoneId, UUID goalId, UUID userId,
                                     UUID partnershipId, String milestoneTitle, Integer progressPercentage) {
        super(source);
        this.milestoneId = milestoneId;
        this.goalId = goalId;
        this.userId = userId;
        this.partnershipId = partnershipId;
        this.milestoneTitle = milestoneTitle;
        this.progressPercentage = progressPercentage;
        this.achievedAt = ZonedDateTime.now();
        this.celebrationMessage = generateCelebrationMessage(milestoneTitle, progressPercentage);
    }

    private String generateCelebrationMessage(String title, Integer progress) {
        return String.format("Congratulations! You've achieved milestone '%s' (%d%% complete)!", title, progress);
    }
}