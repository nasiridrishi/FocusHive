package com.focushive.hive.dto;

import com.focushive.hive.entity.HiveMember;

import java.time.LocalDateTime;

/**
 * Response DTO for HiveMember information
 */
public class HiveMemberResponse {

    private String id;
    private String userId;
    private String username;
    private String email;
    private HiveMember.MemberRole role;
    private HiveMember.MemberStatus status;
    private LocalDateTime joinedAt;
    private LocalDateTime lastActiveAt;
    private Integer totalMinutes;
    private Integer consecutiveDays;
    private Boolean isMuted;
    private String invitedByUsername;
    private String invitedByUserId;

    // Default constructor
    public HiveMemberResponse() {}

    // Constructor from entity
    public HiveMemberResponse(HiveMember member) {
        this.id = member.getId();
        this.userId = member.getUser().getId();
        this.username = member.getUser().getUsername();
        this.email = member.getUser().getEmail();
        this.role = member.getRole();
        this.status = member.getStatus();
        this.joinedAt = member.getJoinedAt();
        this.lastActiveAt = member.getLastActiveAt();
        this.totalMinutes = member.getTotalMinutes();
        this.consecutiveDays = member.getConsecutiveDays();
        this.isMuted = member.getIsMuted();

        if (member.getInvitedBy() != null) {
            this.invitedByUsername = member.getInvitedBy().getUsername();
            this.invitedByUserId = member.getInvitedBy().getId();
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public HiveMember.MemberRole getRole() {
        return role;
    }

    public void setRole(HiveMember.MemberRole role) {
        this.role = role;
    }

    public HiveMember.MemberStatus getStatus() {
        return status;
    }

    public void setStatus(HiveMember.MemberStatus status) {
        this.status = status;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public Integer getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(Integer totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    public Integer getConsecutiveDays() {
        return consecutiveDays;
    }

    public void setConsecutiveDays(Integer consecutiveDays) {
        this.consecutiveDays = consecutiveDays;
    }

    public Boolean getIsMuted() {
        return isMuted;
    }

    public void setIsMuted(Boolean isMuted) {
        this.isMuted = isMuted;
    }

    public String getInvitedByUsername() {
        return invitedByUsername;
    }

    public void setInvitedByUsername(String invitedByUsername) {
        this.invitedByUsername = invitedByUsername;
    }

    public String getInvitedByUserId() {
        return invitedByUserId;
    }

    public void setInvitedByUserId(String invitedByUserId) {
        this.invitedByUserId = invitedByUserId;
    }
}