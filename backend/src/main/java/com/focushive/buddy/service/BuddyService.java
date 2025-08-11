package com.focushive.buddy.service;

import com.focushive.buddy.dto.*;
import com.focushive.buddy.entity.BuddyRelationship;
import com.focushive.buddy.entity.BuddyPreferences;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BuddyService {
    
    // Buddy Relationship Management
    BuddyRelationshipDTO sendBuddyRequest(String fromUserId, String toUserId, BuddyRequestDTO request);
    BuddyRelationshipDTO acceptBuddyRequest(Long relationshipId, String userId);
    BuddyRelationshipDTO rejectBuddyRequest(Long relationshipId, String userId);
    BuddyRelationshipDTO terminateBuddyRelationship(Long relationshipId, String userId, String reason);
    List<BuddyRelationshipDTO> getActiveBuddies(String userId);
    List<BuddyRelationshipDTO> getPendingRequests(String userId);
    List<BuddyRelationshipDTO> getSentRequests(String userId);
    BuddyRelationshipDTO getBuddyRelationship(Long relationshipId);
    
    // Buddy Matching
    List<BuddyMatchDTO> findPotentialMatches(String userId);
    BuddyMatchScoreDTO calculateMatchScore(String userId1, String userId2);
    
    // Buddy Preferences
    BuddyPreferencesDTO getUserPreferences(String userId);
    BuddyPreferencesDTO updateUserPreferences(String userId, BuddyPreferencesDTO preferences);
    
    // Buddy Goals
    BuddyGoalDTO createGoal(Long relationshipId, BuddyGoalDTO goal);
    BuddyGoalDTO updateGoal(Long goalId, BuddyGoalDTO goal);
    BuddyGoalDTO markGoalComplete(Long goalId, String userId);
    List<BuddyGoalDTO> getRelationshipGoals(Long relationshipId);
    List<BuddyGoalDTO> getActiveGoals(String userId);
    List<BuddyGoalDTO> getActiveGoals(); // For WebSocket usage - gets active goals for all users
    
    // Buddy Check-ins
    BuddyCheckinDTO createCheckin(Long relationshipId, String initiatorId, BuddyCheckinDTO checkin);
    List<BuddyCheckinDTO> getRelationshipCheckins(Long relationshipId, Pageable pageable);
    BuddyCheckinStatsDTO getCheckinStats(Long relationshipId);
    
    // Buddy Sessions
    BuddySessionDTO scheduleSession(Long relationshipId, BuddySessionDTO session);
    BuddySessionDTO updateSession(Long sessionId, BuddySessionDTO session);
    BuddySessionDTO startSession(Long sessionId, String userId);
    BuddySessionDTO endSession(Long sessionId, String userId);
    BuddySessionDTO cancelSession(Long sessionId, String userId, String reason);
    BuddySessionDTO rateSession(Long sessionId, String userId, Integer rating, String feedback);
    List<BuddySessionDTO> getUpcomingSessions(String userId);
    List<BuddySessionDTO> getUpcomingSessions(); // For WebSocket usage - gets upcoming sessions for all users
    List<BuddySessionDTO> getRelationshipSessions(Long relationshipId, Pageable pageable);
    
    // Statistics and Analytics
    BuddyRelationshipStatsDTO getRelationshipStats(Long relationshipId);
    UserBuddyStatsDTO getUserBuddyStats(String userId);
    
    // Notifications
    void notifyBuddyRequest(String fromUserId, String toUserId);
    void notifyBuddyAcceptance(Long relationshipId);
    void notifyUpcomingSession(Long sessionId);
    void notifyGoalDeadline(Long goalId);
    
    // Additional methods needed by WebSocket handlers
    List<BuddyRelationshipDTO> getActiveRelationships();
}