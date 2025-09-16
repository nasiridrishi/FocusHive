package com.focushive.integration.client;

import com.focushive.integration.dto.BuddyDtos;
import com.focushive.integration.fallback.BuddyServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign client for Buddy Service communication.
 * Configured with circuit breaker and fallback for resilience.
 */
@FeignClient(
    name = "buddy-service",
    url = "${buddy.service.url:http://localhost:8087}",
    fallback = BuddyServiceFallback.class,
    configuration = IntegrationFeignConfiguration.class
)
public interface BuddyServiceClient {

    @GetMapping("/api/v1/buddy/matches")
    List<BuddyDtos.PotentialMatchDto> getPotentialMatches(@RequestHeader("X-User-ID") String userId);

    @PostMapping("/api/v1/buddy/match")
    BuddyDtos.BuddyMatchResponse createMatch(@RequestBody BuddyDtos.BuddyMatchRequest request);

    @GetMapping("/api/v1/buddy/partnerships/{userId}")
    List<BuddyDtos.BuddyPartnershipDto> getUserPartnerships(@PathVariable String userId);

    @PostMapping("/api/v1/buddy/partnerships/{partnershipId}/checkin")
    BuddyDtos.CheckInResponse createCheckIn(@PathVariable String partnershipId, @RequestBody BuddyDtos.CheckInRequest request);

    @GetMapping("/api/v1/buddy/partnerships/{partnershipId}/checkins")
    List<BuddyDtos.CheckInResponse> getCheckIns(@PathVariable String partnershipId);

    @PostMapping("/api/v1/buddy/goals")
    BuddyDtos.BuddyGoalResponse createGoal(@RequestBody BuddyDtos.BuddyGoalRequest request);

    @GetMapping("/api/v1/buddy/goals/{userId}")
    List<BuddyDtos.BuddyGoalResponse> getUserGoals(@PathVariable String userId);

    @PutMapping("/api/v1/buddy/goals/{goalId}/progress")
    void updateGoalProgress(@PathVariable String goalId, @RequestBody BuddyDtos.GoalProgressRequest request);

    @DeleteMapping("/api/v1/buddy/partnerships/{partnershipId}")
    void endPartnership(@PathVariable String partnershipId);

    @GetMapping("/actuator/health")
    String healthCheck();
}