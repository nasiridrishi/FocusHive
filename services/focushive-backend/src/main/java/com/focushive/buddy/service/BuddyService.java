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
    BuddyRelationshipDTO acceptBuddyRequest(String relationshipId, String userId);
    BuddyRelationshipDTO rejectBuddyRequest(String relationshipId, String userId);
    BuddyRelationshipDTO terminateBuddyRelationship(String relationshipId, String userId, String reason);
    List<BuddyRelationshipDTO> getActiveBuddies(String userId);
    List<BuddyRelationshipDTO> getPendingRequests(String userId);
    List<BuddyRelationshipDTO> getSentRequests(String userId);
    BuddyRelationshipDTO getBuddyRelationship(String relationshipId);
    
    // Buddy Matching
    List<BuddyMatchDTO> findPotentialMatches(String userId);
    BuddyMatchScoreDTO calculateMatchScore(String userId1, String userId2);
    
    // Buddy Preferences
    BuddyPreferencesDTO getUserPreferences(String userId);
    BuddyPreferencesDTO updateUserPreferences(String userId, BuddyPreferencesDTO preferences);
    
    // Buddy Goals
    BuddyGoalDTO createGoal(String relationshipId, BuddyGoalDTO goal);
    BuddyGoalDTO updateGoal(String goalId, BuddyGoalDTO goal);
    BuddyGoalDTO markGoalComplete(String goalId, String userId);
    List<BuddyGoalDTO> getRelationshipGoals(String relationshipId);
    List<BuddyGoalDTO> getActiveGoals(String userId);
    List<BuddyGoalDTO> getActiveGoals(); // For WebSocket usage - gets active goals for all users
    
    // Buddy Check-ins
    BuddyCheckinDTO createCheckin(String relationshipId, String initiatorId, BuddyCheckinDTO checkin);
    List<BuddyCheckinDTO> getRelationshipCheckins(String relationshipId, Pageable pageable);
    BuddyCheckinStatsDTO getCheckinStats(String relationshipId);
    
    // Buddy Sessions
    BuddySessionDTO scheduleSession(String relationshipId, BuddySessionDTO session);
    BuddySessionDTO updateSession(String sessionId, BuddySessionDTO session);
    BuddySessionDTO startSession(String sessionId, String userId);
    BuddySessionDTO endSession(String sessionId, String userId);
    BuddySessionDTO cancelSession(String sessionId, String userId, String reason);
    BuddySessionDTO rateSession(String sessionId, String userId, Integer rating, String feedback);
    List<BuddySessionDTO> getUpcomingSessions(String userId);
    List<BuddySessionDTO> getUpcomingSessions(); // For WebSocket usage - gets upcoming sessions for all users
    List<BuddySessionDTO> getRelationshipSessions(String relationshipId, Pageable pageable);
    
    // Statistics and Analytics
    BuddyRelationshipStatsDTO getRelationshipStats(String relationshipId);
    UserBuddyStatsDTO getUserBuddyStats(String userId);
    
    // Notifications
    void notifyBuddyRequest(String fromUserId, String toUserId);
    void notifyBuddyAcceptance(String relationshipId);
    void notifyUpcomingSession(String sessionId);
    void notifyGoalDeadline(String goalId);
    
    // Additional methods needed by WebSocket handlers
    List<BuddyRelationshipDTO> getActiveRelationships();
}