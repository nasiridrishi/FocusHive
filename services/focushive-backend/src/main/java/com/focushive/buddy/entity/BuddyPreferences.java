package com.focushive.buddy.entity;

import com.focushive.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "buddy_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddyPreferences {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(name = "preferred_timezone", length = 50)
    private String preferredTimezone;
    
    @Convert(converter = WorkHoursMapConverter.class)
    @Column(name = "preferred_work_hours", columnDefinition = "TEXT")
    private Map<String, WorkHours> preferredWorkHours;
    
    @Convert(converter = com.focushive.common.converter.StringArrayConverter.class)
    @Column(name = "focus_areas", columnDefinition = "TEXT")
    private String[] focusAreas;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "communication_style", length = 50)
    private CommunicationStyle communicationStyle;
    
    @Column(name = "matching_enabled", nullable = false)
    @Builder.Default
    private Boolean matchingEnabled = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum CommunicationStyle {
        FREQUENT, MODERATE, MINIMAL
    }
    
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkHours {
        private Integer startHour;
        private Integer endHour;
        
        public boolean overlaps(WorkHours other) {
            if (other == null) return false;
            return !(endHour <= other.startHour || startHour >= other.endHour);
        }
        
        public int overlapHours(WorkHours other) {
            if (!overlaps(other)) return 0;
            int overlapStart = Math.max(startHour, other.startHour);
            int overlapEnd = Math.min(endHour, other.endHour);
            return overlapEnd - overlapStart;
        }
    }
    
    public boolean hasFocusAreaOverlap(String[] otherFocusAreas) {
        if (focusAreas == null || otherFocusAreas == null) return false;
        return java.util.Arrays.stream(focusAreas)
                .anyMatch(area -> java.util.Arrays.asList(otherFocusAreas).contains(area));
    }
    
    public long countFocusAreaOverlap(String[] otherFocusAreas) {
        if (focusAreas == null || otherFocusAreas == null) return 0;
        return java.util.Arrays.stream(focusAreas)
                .filter(area -> java.util.Arrays.asList(otherFocusAreas).contains(area))
                .count();
    }
}