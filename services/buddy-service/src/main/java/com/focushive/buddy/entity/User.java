package com.focushive.buddy.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Simplified User entity for buddy matching purposes.
 * This represents basic user information needed for compatibility matching.
 * Note: This is a lightweight representation - full user data comes from Identity Service.
 */
@Entity
@Table(name = "buddy_users") // Avoid conflicts with identity service users table
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "id", nullable = false)
    private String id;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Size(max = 50)
    @Column(name = "timezone", length = 50)
    private String timezone;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "interests", columnDefinition = "text[]")
    private List<String> interests;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "preferred_focus_times", columnDefinition = "text[]")
    private List<String> preferredFocusTimes;

    @Size(max = 20)
    @Column(name = "experience_level", length = 20)
    private String experienceLevel;

    @Size(max = 50)
    @Column(name = "communication_style", length = 50)
    private String communicationStyle;

    @Size(max = 50)
    @Column(name = "personality_type", length = 50)
    private String personalityType;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}