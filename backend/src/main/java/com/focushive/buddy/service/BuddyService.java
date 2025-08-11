package com.focushive.buddy.service;

import com.focushive.buddy.dto.*;
import com.focushive.buddy.entity.BuddyRelationship;
import com.focushive.buddy.entity.BuddyPreferences;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BuddyService {
    
    // Buddy Relationship Management
    BuddyRelationshipDTO sendBuddyRequest(Long fromUserId, Long toUserId, BuddyRequestDTO request);
    BuddyRelationshipDTO acceptBuddyRequest(Long relationshipId, Long userId);
    BuddyRelationshipDTO rejectBuddyRequest(Long relationshipId, Long userId);
    BuddyRelationshipDTO terminateBuddyRelationship(Long relationshipId, Long userId, String reason);
    List<BuddyRelationshipDTO> getActiveBuddies(Long userId);
    List<BuddyRelationshipDTO> getPendingRequests(Long userId);
    List<BuddyRelationshipDTO> getSentRequests(Long userId);
    BuddyRelationshipDTO getBuddyRelationship(Long relationshipId);
    
    // Buddy Matching
    List<BuddyMatchDTO> findPotentialMatches(Long userId);
    BuddyMatchScoreDTO calculateMatchScore(Long userId1, Long userId2);
    
    // Buddy Preferences
    BuddyPreferencesDTO getUserPreferences(Long userId);
    BuddyPreferencesDTO updateUserPreferences(Long userId, BuddyPreferencesDTO preferences);
    
    // Buddy Goals
    BuddyGoalDTO createGoal(Long relationshipId, BuddyGoalDTO goal);
    BuddyGoalDTO updateGoal(Long goalId, BuddyGoalDTO goal);
    BuddyGoalDTO markGoalComplete(Long goalId, Long userId);
    List<BuddyGoalDTO> getRelationshipGoals(Long relationshipId);
    List<BuddyGoalDTO> getActiveGoals(Long relationshipId);
    
    // Buddy Check-ins
    BuddyCheckinDTO createCheckin(Long relationshipId, Long initiatorId, BuddyCheckinDTO checkin);
    List<BuddyCheckinDTO> getRelationshipCheckins(Long relationshipId, Pageable pageable);
    BuddyCheckinStatsDTO getCheckinStats(Long relationshipId);
    
    // Buddy Sessions
    BuddySessionDTO scheduleSession(Long relationshipId, BuddySessionDTO session);
    BuddySessionDTO updateSession(Long sessionId, BuddySessionDTO session);
    BuddySessionDTO startSession(Long sessionId, Long userId);
    BuddySessionDTO endSession(Long sessionId, Long userId);
    BuddySessionDTO cancelSession(Long sessionId, Long userId, String reason);
    BuddySessionDTO rateSession(Long sessionId, Long userId, Integer rating, String feedback);
    List<BuddySessionDTO> getUpcomingSessions(Long userId);
    List<BuddySessionDTO> getRelationshipSessions(Long relationshipId, Pageable pageable);
    
    // Statistics and Analytics
    BuddyRelationshipStatsDTO getRelationshipStats(Long relationshipId);
    UserBuddyStatsDTO getUserBuddyStats(Long userId);
    
    // Notifications
    void notifyBuddyRequest(Long fromUserId, Long toUserId);
    void notifyBuddyAcceptance(Long relationshipId);
    void notifyUpcomingSession(Long sessionId);
    void notifyGoalDeadline(Long goalId);
}