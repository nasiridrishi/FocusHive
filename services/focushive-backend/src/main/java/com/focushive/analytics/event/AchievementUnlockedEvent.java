package com.focushive.analytics.event;

import com.focushive.analytics.enums.AchievementType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Event published when a user unlocks an achievement.
 * This event triggers notifications and real-time updates.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AchievementUnlockedEvent extends ApplicationEvent {

    private String userId;
    private AchievementType achievementType;
    private LocalDateTime unlockedAt;
    private int currentValue;
    private String context; // Optional context about how it was unlocked

    public AchievementUnlockedEvent(Object source) {
        super(source);
    }

    public AchievementUnlockedEvent(Object source, String userId, AchievementType achievementType) {
        super(source);
        this.userId = userId;
        this.achievementType = achievementType;
        this.unlockedAt = LocalDateTime.now();
    }

    public String getAchievementName() {
        return achievementType != null ? achievementType.getName() : "";
    }

    public String getAchievementDescription() {
        return achievementType != null ? achievementType.getDescription() : "";
    }

    public int getPoints() {
        return achievementType != null ? achievementType.getPoints() : 0;
    }

    public String getCategory() {
        return achievementType != null ? achievementType.getCategory() : "";
    }
}