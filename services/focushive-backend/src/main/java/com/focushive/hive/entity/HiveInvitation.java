package com.focushive.hive.entity;

import com.focushive.common.entity.BaseEntity;
import com.focushive.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hive_invitations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"hive_id", "invited_user_id", "status"}))
public class HiveInvitation extends BaseEntity {

    @NotNull(message = "Hive is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hive_id", nullable = false)
    private Hive hive;

    @NotNull(message = "Invited user is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_user_id", nullable = false)
    private User invitedUser;

    @NotNull(message = "Inviter is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_user_id", nullable = false)
    private User invitedBy;

    @NotNull(message = "Invitation code is required")
    @Column(name = "invitation_code", unique = true, nullable = false, length = 100)
    private String invitationCode;

    @NotNull(message = "Expiration date is required")
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    // Constructor
    public HiveInvitation() {
        // Default expiration: 7 days from now
        this.expiresAt = LocalDateTime.now().plusDays(7);
        this.invitationCode = UUID.randomUUID().toString();
    }

    // Business logic methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return status == InvitationStatus.PENDING && !isExpired();
    }

    // Getters and setters
    public Hive getHive() {
        return hive;
    }

    public void setHive(Hive hive) {
        this.hive = hive;
    }

    public User getInvitedUser() {
        return invitedUser;
    }

    public void setInvitedUser(User invitedUser) {
        this.invitedUser = invitedUser;
    }

    public User getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(User invitedBy) {
        this.invitedBy = invitedBy;
    }

    public String getInvitationCode() {
        return invitationCode;
    }

    public void setInvitationCode(String invitationCode) {
        this.invitationCode = invitationCode;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public enum InvitationStatus {
        PENDING, ACCEPTED, REJECTED, EXPIRED, REVOKED
    }
}