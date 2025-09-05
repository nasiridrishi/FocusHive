package com.focushive.buddy.service.impl;

import com.focushive.buddy.dto.*;
import com.focushive.buddy.service.BuddyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.features.buddy.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class BuddyServiceStub implements BuddyService {

    @Override
    public BuddyRelationshipDTO sendBuddyRequest(String fromUserId, String toUserId, BuddyRequestDTO request) {
        log.warn("Buddy service disabled - sendBuddyRequest called");
        return null;
    }

    @Override
    public BuddyRelationshipDTO acceptBuddyRequest(String relationshipId, String userId) {
        log.warn("Buddy service disabled - acceptBuddyRequest called");
        return null;
    }

    @Override
    public BuddyRelationshipDTO rejectBuddyRequest(String relationshipId, String userId) {
        log.warn("Buddy service disabled - rejectBuddyRequest called");
        return null;
    }

    @Override
    public BuddyRelationshipDTO terminateBuddyRelationship(String relationshipId, String userId, String reason) {
        log.warn("Buddy service disabled - terminateBuddyRelationship called");
        return null;
    }

    @Override
    public List<BuddyRelationshipDTO> getActiveBuddies(String userId) {
        log.warn("Buddy service disabled - getActiveBuddies called");
        return Collections.emptyList();
    }

    @Override
    public List<BuddyRelationshipDTO> getPendingRequests(String userId) {
        log.warn("Buddy service disabled - getPendingRequests called");
        return Collections.emptyList();
    }

    @Override
    public List<BuddyRelationshipDTO> getSentRequests(String userId) {
        log.warn("Buddy service disabled - getSentRequests called");
        return Collections.emptyList();
    }

    @Override
    public BuddyRelationshipDTO getBuddyRelationship(String relationshipId) {
        log.warn("Buddy service disabled - getBuddyRelationship called");
        return null;
    }

    @Override
    public List<BuddyMatchDTO> findPotentialMatches(String userId) {
        log.warn("Buddy service disabled - findPotentialMatches called");
        return Collections.emptyList();
    }

    @Override
    public BuddyMatchScoreDTO calculateMatchScore(String userId1, String userId2) {
        log.warn("Buddy service disabled - calculateMatchScore called");
        return null;
    }

    @Override
    public BuddyPreferencesDTO getUserPreferences(String userId) {
        log.warn("Buddy service disabled - getUserPreferences called");
        return null;
    }

    @Override
    public BuddyPreferencesDTO updateUserPreferences(String userId, BuddyPreferencesDTO dto) {
        log.warn("Buddy service disabled - updateUserPreferences called");
        return null;
    }

    @Override
    public BuddyGoalDTO createGoal(String relationshipId, BuddyGoalDTO dto) {
        log.warn("Buddy service disabled - createGoal called");
        return null;
    }

    @Override
    public BuddyGoalDTO updateGoal(String goalId, BuddyGoalDTO dto) {
        log.warn("Buddy service disabled - updateGoal called");
        return null;
    }

    @Override
    public BuddyGoalDTO markGoalComplete(String goalId, String userId) {
        log.warn("Buddy service disabled - markGoalComplete called");
        return null;
    }

    @Override
    public List<BuddyGoalDTO> getRelationshipGoals(String relationshipId) {
        log.warn("Buddy service disabled - getRelationshipGoals called");
        return Collections.emptyList();
    }

    @Override
    public List<BuddyGoalDTO> getActiveGoals(String userId) {
        log.warn("Buddy service disabled - getActiveGoals called");
        return Collections.emptyList();
    }

    @Override
    public List<BuddyGoalDTO> getActiveGoals() {
        log.warn("Buddy service disabled - getActiveGoals called");
        return Collections.emptyList();
    }

    @Override
    public BuddyCheckinDTO createCheckin(String relationshipId, String initiatorId, BuddyCheckinDTO dto) {
        log.warn("Buddy service disabled - createCheckin called");
        return null;
    }

    @Override
    public List<BuddyCheckinDTO> getRelationshipCheckins(String relationshipId, Pageable pageable) {
        log.warn("Buddy service disabled - getRelationshipCheckins called");
        return Collections.emptyList();
    }

    @Override
    public BuddyCheckinStatsDTO getCheckinStats(String relationshipId) {
        log.warn("Buddy service disabled - getCheckinStats called");
        return null;
    }

    @Override
    public BuddySessionDTO scheduleSession(String relationshipId, BuddySessionDTO dto) {
        log.warn("Buddy service disabled - scheduleSession called");
        return null;
    }

    @Override
    public BuddySessionDTO updateSession(String sessionId, BuddySessionDTO dto) {
        log.warn("Buddy service disabled - updateSession called");
        return null;
    }

    @Override
    public BuddySessionDTO startSession(String sessionId, String userId) {
        log.warn("Buddy service disabled - startSession called");
        return null;
    }

    @Override
    public BuddySessionDTO endSession(String sessionId, String userId) {
        log.warn("Buddy service disabled - endSession called");
        return null;
    }

    @Override
    public BuddySessionDTO cancelSession(String sessionId, String userId, String reason) {
        log.warn("Buddy service disabled - cancelSession called");
        return null;
    }

    @Override
    public BuddySessionDTO rateSession(String sessionId, String userId, Integer rating, String feedback) {
        log.warn("Buddy service disabled - rateSession called");
        return null;
    }

    @Override
    public List<BuddySessionDTO> getUpcomingSessions(String userId) {
        log.warn("Buddy service disabled - getUpcomingSessions called");
        return Collections.emptyList();
    }

    @Override
    public List<BuddySessionDTO> getUpcomingSessions() {
        log.warn("Buddy service disabled - getUpcomingSessions called");
        return Collections.emptyList();
    }

    @Override
    public List<BuddySessionDTO> getRelationshipSessions(String relationshipId, Pageable pageable) {
        log.warn("Buddy service disabled - getRelationshipSessions called");
        return Collections.emptyList();
    }

    @Override
    public BuddyRelationshipStatsDTO getRelationshipStats(String relationshipId) {
        log.warn("Buddy service disabled - getRelationshipStats called");
        return null;
    }

    @Override
    public UserBuddyStatsDTO getUserBuddyStats(String userId) {
        log.warn("Buddy service disabled - getUserBuddyStats called");
        return null;
    }

    @Override
    public void notifyBuddyRequest(String fromUserId, String toUserId) {
        log.warn("Buddy service disabled - notifyBuddyRequest called");
    }

    @Override
    public void notifyBuddyAcceptance(String relationshipId) {
        log.warn("Buddy service disabled - notifyBuddyAcceptance called");
    }

    @Override
    public void notifyUpcomingSession(String sessionId) {
        log.warn("Buddy service disabled - notifyUpcomingSession called");
    }

    @Override
    public void notifyGoalDeadline(String goalId) {
        log.warn("Buddy service disabled - notifyGoalDeadline called");
    }

    @Override
    public List<BuddyRelationshipDTO> getActiveRelationships() {
        log.warn("Buddy service disabled - getActiveRelationships called");
        return Collections.emptyList();
    }
}