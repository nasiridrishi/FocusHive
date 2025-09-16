package com.focushive.integration.fallback;

import com.focushive.integration.client.BuddyServiceClient;
import com.focushive.integration.dto.BuddyDtos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Fallback implementation for Buddy Service.
 * Provides graceful degradation when the service is unavailable.
 */
@Slf4j
@Component
public class BuddyServiceFallback implements BuddyServiceClient {

    @Override
    public List<BuddyDtos.PotentialMatchDto> getPotentialMatches(String userId) {
        log.warn("Buddy service unavailable. Cannot retrieve matches for user: {}", userId);
        return Collections.emptyList();
    }

    @Override
    public BuddyDtos.BuddyMatchResponse createMatch(BuddyDtos.BuddyMatchRequest request) {
        log.warn("Buddy service unavailable. Cannot create match: {}", request);
        BuddyDtos.BuddyMatchResponse response = new BuddyDtos.BuddyMatchResponse();
        response.setSuccess(false);
        response.setMessage("Buddy service temporarily unavailable. Please try again later.");
        return response;
    }

    @Override
    public List<BuddyDtos.BuddyPartnershipDto> getUserPartnerships(String userId) {
        log.warn("Buddy service unavailable. Cannot retrieve partnerships for user: {}", userId);
        return Collections.emptyList();
    }

    @Override
    public BuddyDtos.CheckInResponse createCheckIn(String partnershipId, BuddyDtos.CheckInRequest request) {
        log.warn("Buddy service unavailable. Cannot create check-in for partnership: {}", partnershipId);
        BuddyDtos.CheckInResponse response = new BuddyDtos.CheckInResponse();
        response.setSuccess(false);
        response.setMessage("Check-in service temporarily unavailable");
        return response;
    }

    @Override
    public List<BuddyDtos.CheckInResponse> getCheckIns(String partnershipId) {
        log.warn("Buddy service unavailable. Cannot retrieve check-ins for partnership: {}", partnershipId);
        return Collections.emptyList();
    }

    @Override
    public BuddyDtos.BuddyGoalResponse createGoal(BuddyDtos.BuddyGoalRequest request) {
        log.warn("Buddy service unavailable. Cannot create goal: {}", request);
        BuddyDtos.BuddyGoalResponse response = new BuddyDtos.BuddyGoalResponse();
        response.setSuccess(false);
        response.setMessage("Goal creation service temporarily unavailable");
        return response;
    }

    @Override
    public List<BuddyDtos.BuddyGoalResponse> getUserGoals(String userId) {
        log.warn("Buddy service unavailable. Cannot retrieve goals for user: {}", userId);
        return Collections.emptyList();
    }

    @Override
    public void updateGoalProgress(String goalId, BuddyDtos.GoalProgressRequest request) {
        log.warn("Buddy service unavailable. Cannot update progress for goal: {}", goalId);
    }

    @Override
    public void endPartnership(String partnershipId) {
        log.warn("Buddy service unavailable. Cannot end partnership: {}", partnershipId);
    }

    @Override
    public String healthCheck() {
        return "{\"status\":\"DOWN\",\"message\":\"Buddy service unreachable\"}";
    }
}