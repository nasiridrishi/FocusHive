package com.focushive.hive.dto;

import com.focushive.hive.entity.HiveInvitation;

import java.time.LocalDateTime;

/**
 * Response DTO for HiveInvitation information
 */
public class InvitationResponse {

    private String id;
    private String hiveId;
    private String hiveName;
    private String invitedUserId;
    private String invitedEmail;
    private String invitedUsername;
    private String invitedByUserId;
    private String invitedByUsername;
    private String invitationCode;
    private LocalDateTime expiresAt;
    private HiveInvitation.InvitationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    private String message;
    private boolean isExpired;
    private boolean isActive;

    // Default constructor
    public InvitationResponse() {}

    // Constructor from entity
    public InvitationResponse(HiveInvitation invitation) {
        this.id = invitation.getId();
        this.hiveId = invitation.getHive().getId();
        this.hiveName = invitation.getHive().getName();
        this.invitedUserId = invitation.getInvitedUser().getId();
        this.invitedEmail = invitation.getInvitedUser().getEmail();
        this.invitedUsername = invitation.getInvitedUser().getUsername();
        this.invitedByUserId = invitation.getInvitedBy().getId();
        this.invitedByUsername = invitation.getInvitedBy().getUsername();
        this.invitationCode = invitation.getInvitationCode();
        this.expiresAt = invitation.getExpiresAt();
        this.status = invitation.getStatus();
        this.createdAt = invitation.getCreatedAt();
        this.respondedAt = invitation.getRespondedAt();
        this.message = invitation.getMessage();
        this.isExpired = invitation.isExpired();
        this.isActive = invitation.isActive();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHiveId() {
        return hiveId;
    }

    public void setHiveId(String hiveId) {
        this.hiveId = hiveId;
    }

    public String getHiveName() {
        return hiveName;
    }

    public void setHiveName(String hiveName) {
        this.hiveName = hiveName;
    }

    public String getInvitedUserId() {
        return invitedUserId;
    }

    public void setInvitedUserId(String invitedUserId) {
        this.invitedUserId = invitedUserId;
    }

    public String getInvitedEmail() {
        return invitedEmail;
    }

    public void setInvitedEmail(String invitedEmail) {
        this.invitedEmail = invitedEmail;
    }

    public String getInvitedUsername() {
        return invitedUsername;
    }

    public void setInvitedUsername(String invitedUsername) {
        this.invitedUsername = invitedUsername;
    }

    public String getInvitedByUserId() {
        return invitedByUserId;
    }

    public void setInvitedByUserId(String invitedByUserId) {
        this.invitedByUserId = invitedByUserId;
    }

    public String getInvitedByUsername() {
        return invitedByUsername;
    }

    public void setInvitedByUsername(String invitedByUsername) {
        this.invitedByUsername = invitedByUsername;
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

    public HiveInvitation.InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(HiveInvitation.InvitationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public boolean isExpired() {
        return isExpired;
    }

    public void setExpired(boolean expired) {
        isExpired = expired;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}