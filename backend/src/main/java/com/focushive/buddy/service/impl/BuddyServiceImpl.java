package com.focushive.buddy.service.impl;

import com.focushive.buddy.dto.*;
import com.focushive.buddy.entity.*;
import com.focushive.buddy.entity.BuddyRelationship.BuddyStatus;
import com.focushive.buddy.entity.BuddyGoal.GoalStatus;
import com.focushive.buddy.entity.BuddySession.SessionStatus;
import com.focushive.buddy.entity.BuddyPreferences.CommunicationStyle;
import com.focushive.buddy.repository.*;
import com.focushive.buddy.service.BuddyService;
import com.focushive.user.entity.User;
import com.focushive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BuddyServiceImpl implements BuddyService {
    
    private final BuddyRelationshipRepository relationshipRepository;
    private final BuddyGoalRepository goalRepository;
    private final BuddyCheckinRepository checkinRepository;
    private final BuddyPreferencesRepository preferencesRepository;
    private final BuddySessionRepository sessionRepository;
    private final UserRepository userRepository;
    
    // Constants for matching algorithm
    private static final double FOCUS_AREA_WEIGHT = 0.35;
    private static final double TIMEZONE_WEIGHT = 0.25;
    private static final double COMMUNICATION_WEIGHT = 0.20;
    private static final double AVAILABILITY_WEIGHT = 0.20;
    
    @Override
    public BuddyRelationshipDTO sendBuddyRequest(String fromUserId, String toUserId, BuddyRequestDTO request) {
        log.info("Sending buddy request from user {} to user {}", fromUserId, toUserId);
        
        // Check if relationship already exists
        Optional<BuddyRelationship> existing = relationshipRepository.findByUserIds(
            fromUserId, toUserId, Arrays.asList(BuddyStatus.PENDING, BuddyStatus.ACTIVE)
        );
        
        if (existing.isPresent()) {
            throw new IllegalStateException("Buddy relationship already exists or is pending");
        }
        
        User fromUser = userRepository.findById(fromUserId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + fromUserId));
        User toUser = userRepository.findById(toUserId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + toUserId));
        
        BuddyRelationship relationship = BuddyRelationship.builder()
            .user1(fromUser)
            .user2(toUser)
            .status(BuddyStatus.PENDING)
            .build();
        
        relationship = relationshipRepository.save(relationship);
        
        // Send notification
        notifyBuddyRequest(fromUserId, toUserId);
        
        return mapToRelationshipDTO(relationship, fromUserId);
    }
    
    @Override
    public BuddyRelationshipDTO acceptBuddyRequest(Long relationshipId, String userId) {
        log.info("Accepting buddy request {} by user {}", relationshipId, userId);
        
        BuddyRelationship relationship = relationshipRepository.findById(relationshipId)
            .orElseThrow(() -> new EntityNotFoundException("Relationship not found: " + relationshipId));
        
        // Verify the user is the recipient
        if (!relationship.isRecipient(userId)) {
            throw new IllegalStateException("Only the recipient can accept the buddy request");
        }
        
        if (relationship.getStatus() != BuddyStatus.PENDING) {
            throw new IllegalStateException("Can only accept pending requests");
        }
        
        relationship.setStatus(BuddyStatus.ACTIVE);
        relationship.setStartDate(LocalDateTime.now());
        relationship = relationshipRepository.save(relationship);
        
        // Send notification
        notifyBuddyAcceptance(relationshipId);
        
        return mapToRelationshipDTO(relationship, userId);
    }
    
    @Override
    public BuddyRelationshipDTO rejectBuddyRequest(Long relationshipId, String userId) {
        log.info("Rejecting buddy request {} by user {}", relationshipId, userId);
        
        BuddyRelationship relationship = relationshipRepository.findById(relationshipId)
            .orElseThrow(() -> new EntityNotFoundException("Relationship not found: " + relationshipId));
        
        // Verify the user is the recipient
        if (!relationship.isRecipient(userId)) {
            throw new IllegalStateException("Only the recipient can reject the buddy request");
        }
        
        if (relationship.getStatus() != BuddyStatus.PENDING) {
            throw new IllegalStateException("Can only reject pending requests");
        }
        
        relationship.setStatus(BuddyStatus.BLOCKED);
        relationship = relationshipRepository.save(relationship);
        
        return mapToRelationshipDTO(relationship, userId);
    }
    
    @Override
    public BuddyRelationshipDTO terminateBuddyRelationship(Long relationshipId, String userId, String reason) {
        log.info("Terminating buddy relationship {} by user {}", relationshipId, userId);
        
        BuddyRelationship relationship = relationshipRepository.findById(relationshipId)
            .orElseThrow(() -> new EntityNotFoundException("Relationship not found: " + relationshipId));
        
        // Verify the user is part of the relationship
        if (!relationship.involvesUser(userId)) {
            throw new IllegalStateException("User is not part of this relationship");
        }
        
        if (relationship.getStatus() != BuddyStatus.ACTIVE) {
            throw new IllegalStateException("Can only terminate active relationships");
        }
        
        relationship.setStatus(BuddyStatus.ENDED);
        relationship.setEndDate(LocalDateTime.now());
        relationship.setTerminationReason(reason);
        relationship = relationshipRepository.save(relationship);
        
        return mapToRelationshipDTO(relationship, userId);
    }
    
    @Override
    public List<BuddyRelationshipDTO> getActiveBuddies(String userId) {
        List<BuddyRelationship> relationships = relationshipRepository.findActiveBuddiesForUser(userId);
        return relationships.stream()
            .map(r -> mapToRelationshipDTO(r, userId))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<BuddyRelationshipDTO> getPendingRequests(String userId) {
        List<BuddyRelationship> relationships = relationshipRepository.findPendingRequestsForUser(userId);
        return relationships.stream()
            .map(r -> mapToRelationshipDTO(r, userId))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<BuddyRelationshipDTO> getSentRequests(String userId) {
        List<BuddyRelationship> relationships = relationshipRepository.findSentRequestsByUser(userId);
        return relationships.stream()
            .map(r -> mapToRelationshipDTO(r, userId))
            .collect(Collectors.toList());
    }
    
    @Override
    public BuddyRelationshipDTO getBuddyRelationship(Long relationshipId) {
        BuddyRelationship relationship = relationshipRepository.findById(relationshipId)
            .orElseThrow(() -> new EntityNotFoundException("Relationship not found: " + relationshipId));
        return mapToRelationshipDTO(relationship, null);
    }
    
    @Override
    public List<BuddyMatchDTO> findPotentialMatches(String userId) {
        log.info("Finding potential matches for user {}", userId);
        
        BuddyPreferences userPrefs = preferencesRepository.findByUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("User preferences not found"));
        
        if (!userPrefs.getMatchingEnabled()) {
            return Collections.emptyList();
        }
        
        List<BuddyPreferences> candidates = preferencesRepository.findAvailableForMatching(userId);
        
        List<BuddyMatchDTO> matches = new ArrayList<>();
        for (BuddyPreferences candidatePrefs : candidates) {
            // Check if users are already buddies
            if (relationshipRepository.areUsersBuddies(userId, candidatePrefs.getUser().getId())) {
                continue;
            }
            
            BuddyMatchScoreDTO score = calculateMatchScore(userId, candidatePrefs.getUser().getId());
            if (score.getTotalScore() >= 0.5) { // Minimum threshold
                matches.add(createMatchDTO(candidatePrefs.getUser(), score));
            }
        }
        
        // Sort by match score descending
        matches.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
        
        // Return top 10 matches
        return matches.stream().limit(10).collect(Collectors.toList());
    }
    
    @Override
    public BuddyMatchScoreDTO calculateMatchScore(String userId1, String userId2) {
        BuddyPreferences prefs1 = preferencesRepository.findByUserId(userId1)
            .orElseThrow(() -> new EntityNotFoundException("User 1 preferences not found"));
        BuddyPreferences prefs2 = preferencesRepository.findByUserId(userId2)
            .orElseThrow(() -> new EntityNotFoundException("User 2 preferences not found"));
        
        double focusAreaScore = calculateFocusAreaScore(prefs1, prefs2);
        double timezoneScore = calculateTimezoneScore(prefs1, prefs2);
        double communicationScore = calculateCommunicationScore(prefs1, prefs2);
        double availabilityScore = calculateAvailabilityScore(prefs1, prefs2);
        
        double totalScore = (focusAreaScore * FOCUS_AREA_WEIGHT) +
                           (timezoneScore * TIMEZONE_WEIGHT) +
                           (communicationScore * COMMUNICATION_WEIGHT) +
                           (availabilityScore * AVAILABILITY_WEIGHT);
        
        Map<String, Object> breakdown = new HashMap<>();
        breakdown.put("focusAreas", focusAreaScore);
        breakdown.put("timezone", timezoneScore);
        breakdown.put("communication", communicationScore);
        breakdown.put("availability", availabilityScore);
        
        String recommendation = totalScore >= 0.8 ? "Excellent Match" :
                               totalScore >= 0.6 ? "Good Match" :
                               totalScore >= 0.4 ? "Fair Match" : "Poor Match";
        
        return BuddyMatchScoreDTO.builder()
            .user1Id(userId1)
            .user2Id(userId2)
            .totalScore(totalScore)
            .focusAreaScore(focusAreaScore)
            .timezoneScore(timezoneScore)
            .communicationScore(communicationScore)
            .availabilityScore(availabilityScore)
            .scoreBreakdown(breakdown)
            .recommendation(recommendation)
            .build();
    }
    
    @Override
    public BuddyPreferencesDTO getUserPreferences(String userId) {
        BuddyPreferences preferences = preferencesRepository.findByUserId(userId)
            .orElse(createDefaultPreferences(userId));
        return mapToPreferencesDTO(preferences);
    }
    
    @Override
    public BuddyPreferencesDTO updateUserPreferences(String userId, BuddyPreferencesDTO dto) {
        BuddyPreferences preferences = preferencesRepository.findByUserId(userId)
            .orElse(createDefaultPreferences(userId));
        
        preferences.setPreferredTimezone(dto.getPreferredTimezone());
        preferences.setPreferredWorkHours(dto.getPreferredWorkHours());
        preferences.setFocusAreas(dto.getFocusAreas());
        preferences.setCommunicationStyle(dto.getCommunicationStyle());
        preferences.setMatchingEnabled(dto.getMatchingEnabled());
        
        preferences = preferencesRepository.save(preferences);
        return mapToPreferencesDTO(preferences);
    }
    
    @Override
    public BuddyGoalDTO createGoal(Long relationshipId, BuddyGoalDTO dto) {
        BuddyRelationship relationship = relationshipRepository.findById(relationshipId)
            .orElseThrow(() -> new EntityNotFoundException("Relationship not found"));
        
        BuddyGoal goal = BuddyGoal.builder()
            .relationship(relationship)
            .title(dto.getTitle())
            .description(dto.getDescription())
            .status(GoalStatus.ACTIVE)
            .dueDate(dto.getDueDate())
            .metrics(dto.getMetrics())
            .build();
        
        goal = goalRepository.save(goal);
        return mapToGoalDTO(goal);
    }
    
    @Override
    public BuddyGoalDTO updateGoal(Long goalId, BuddyGoalDTO dto) {
        BuddyGoal goal = goalRepository.findById(goalId)
            .orElseThrow(() -> new EntityNotFoundException("Goal not found"));
        
        goal.setTitle(dto.getTitle());
        goal.setDescription(dto.getDescription());
        goal.setDueDate(dto.getDueDate());
        goal.setMetrics(dto.getMetrics());
        goal.setProgressPercentage(dto.getProgressPercentage());
        
        goal = goalRepository.save(goal);
        return mapToGoalDTO(goal);
    }
    
    @Override
    public BuddyGoalDTO markGoalComplete(Long goalId, String userId) {
        BuddyGoal goal = goalRepository.findById(goalId)
            .orElseThrow(() -> new EntityNotFoundException("Goal not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        goal.setStatus(GoalStatus.COMPLETED);
        goal.setCompletedAt(LocalDateTime.now());
        goal.setCompletedBy(user);
        goal.setProgressPercentage(100);
        
        goal = goalRepository.save(goal);
        return mapToGoalDTO(goal);
    }
    
    @Override
    public List<BuddyGoalDTO> getRelationshipGoals(Long relationshipId) {
        List<BuddyGoal> goals = goalRepository.findByRelationshipIdOrderByCreatedAtDesc(relationshipId);
        return goals.stream().map(this::mapToGoalDTO).collect(Collectors.toList());
    }
    
    @Override
    public List<BuddyGoalDTO> getActiveGoals(String userId) {
        List<BuddyGoal> goals = goalRepository.findActiveGoalsForUser(userId);
        return goals.stream().map(this::mapToGoalDTO).collect(Collectors.toList());
    }
    
    @Override
    public List<BuddyGoalDTO> getActiveGoals() {
        List<BuddyGoal> goals = goalRepository.findAll()
            .stream()
            .filter(g -> g.getStatus() == BuddyGoal.GoalStatus.ACTIVE)
            .collect(Collectors.toList());
        return goals.stream().map(this::mapToGoalDTO).collect(Collectors.toList());
    }
    
    @Override
    public BuddyCheckinDTO createCheckin(Long relationshipId, String initiatorId, BuddyCheckinDTO dto) {
        BuddyRelationship relationship = relationshipRepository.findById(relationshipId)
            .orElseThrow(() -> new EntityNotFoundException("Relationship not found"));
        
        User initiator = userRepository.findById(initiatorId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        BuddyCheckin checkin = BuddyCheckin.builder()
            .relationship(relationship)
            .initiatedBy(initiator)
            .checkinTime(LocalDateTime.now())
            .moodRating(dto.getMoodRating())
            .progressRating(dto.getProgressRating())
            .message(dto.getMessage())
            .currentFocus(dto.getCurrentFocus())
            .challenges(dto.getChallenges())
            .wins(dto.getWins())
            .build();
        
        checkin = checkinRepository.save(checkin);
        return mapToCheckinDTO(checkin);
    }
    
    @Override
    public List<BuddyCheckinDTO> getRelationshipCheckins(Long relationshipId, Pageable pageable) {
        List<BuddyCheckin> checkins = checkinRepository.findByRelationshipIdOrderByCheckinTimeDesc(relationshipId);
        return checkins.stream().map(this::mapToCheckinDTO).collect(Collectors.toList());
    }
    
    @Override
    public BuddyCheckinStatsDTO getCheckinStats(Long relationshipId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        LocalDateTime thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        
        Long totalCheckins = checkinRepository.countRecentCheckinsForRelationship(
            relationshipId, LocalDateTime.of(2020, 1, 1, 0, 0)
        );
        Long checkinsLast7Days = checkinRepository.countRecentCheckinsForRelationship(
            relationshipId, sevenDaysAgo
        );
        Long checkinsLast30Days = checkinRepository.countRecentCheckinsForRelationship(
            relationshipId, thirtyDaysAgo
        );
        
        Double avgMood = checkinRepository.getAverageMoodForRelationship(
            relationshipId, thirtyDaysAgo
        );
        
        return BuddyCheckinStatsDTO.builder()
            .relationshipId(relationshipId)
            .totalCheckins(totalCheckins)
            .checkinsLast7Days(checkinsLast7Days)
            .checkinsLast30Days(checkinsLast30Days)
            .averageMoodRating(avgMood)
            .build();
    }
    
    @Override
    public BuddySessionDTO scheduleSession(Long relationshipId, BuddySessionDTO dto) {
        BuddyRelationship relationship = relationshipRepository.findById(relationshipId)
            .orElseThrow(() -> new EntityNotFoundException("Relationship not found"));
        
        BuddySession session = BuddySession.builder()
            .relationship(relationship)
            .sessionDate(dto.getSessionDate())
            .plannedDurationMinutes(dto.getPlannedDurationMinutes())
            .agenda(dto.getAgenda())
            .status(SessionStatus.SCHEDULED)
            .build();
        
        session = sessionRepository.save(session);
        
        // Schedule notification reminder
        notifyUpcomingSession(session.getId());
        
        return mapToSessionDTO(session);
    }
    
    @Override
    public BuddySessionDTO updateSession(Long sessionId, BuddySessionDTO dto) {
        BuddySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        
        session.setSessionDate(dto.getSessionDate());
        session.setPlannedDurationMinutes(dto.getPlannedDurationMinutes());
        session.setAgenda(dto.getAgenda());
        session.setNotes(dto.getNotes());
        
        session = sessionRepository.save(session);
        return mapToSessionDTO(session);
    }
    
    @Override
    public BuddySessionDTO startSession(Long sessionId, String userId) {
        BuddySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        
        session.userJoined(userId);
        session = sessionRepository.save(session);
        return mapToSessionDTO(session);
    }
    
    @Override
    public BuddySessionDTO endSession(Long sessionId, String userId) {
        BuddySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        
        session.userLeft(userId);
        session = sessionRepository.save(session);
        return mapToSessionDTO(session);
    }
    
    @Override
    public BuddySessionDTO cancelSession(Long sessionId, String userId, String reason) {
        BuddySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        
        session.cancelSession(userId, reason);
        session = sessionRepository.save(session);
        return mapToSessionDTO(session);
    }
    
    @Override
    public BuddySessionDTO rateSession(Long sessionId, String userId, Integer rating, String feedback) {
        BuddySession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        
        session.addRating(userId, rating, feedback);
        session = sessionRepository.save(session);
        return mapToSessionDTO(session);
    }
    
    @Override
    public List<BuddySessionDTO> getUpcomingSessions(String userId) {
        List<BuddySession> sessions = sessionRepository.findUpcomingSessionsForUser(userId, LocalDateTime.now());
        return sessions.stream().map(this::mapToSessionDTO).collect(Collectors.toList());
    }
    
    @Override
    public List<BuddySessionDTO> getUpcomingSessions() {
        List<BuddySession> sessions = sessionRepository.findAll()
            .stream()
            .filter(s -> s.getSessionDate().isAfter(LocalDateTime.now()) && 
                        s.getStatus() == BuddySession.SessionStatus.SCHEDULED)
            .collect(Collectors.toList());
        return sessions.stream().map(this::mapToSessionDTO).collect(Collectors.toList());
    }
    
    @Override
    public List<BuddySessionDTO> getRelationshipSessions(Long relationshipId, Pageable pageable) {
        List<BuddySession> sessions = sessionRepository.findByRelationshipIdOrderBySessionDateDesc(relationshipId);
        return sessions.stream().map(this::mapToSessionDTO).collect(Collectors.toList());
    }
    
    @Override
    public BuddyRelationshipStatsDTO getRelationshipStats(Long relationshipId) {
        BuddyRelationship relationship = relationshipRepository.findById(relationshipId)
            .orElseThrow(() -> new EntityNotFoundException("Relationship not found"));
        
        Long totalGoals = goalRepository.countByRelationshipAndStatus(relationshipId, null);
        Long completedGoals = goalRepository.countByRelationshipAndStatus(relationshipId, GoalStatus.COMPLETED);
        
        Long totalSessions = sessionRepository.countByRelationshipAndStatus(relationshipId, null);
        Long completedSessions = sessionRepository.countByRelationshipAndStatus(relationshipId, SessionStatus.COMPLETED);
        
        Double avgSessionDuration = sessionRepository.getAverageSessionDurationForRelationship(relationshipId);
        Double avgSessionRating = sessionRepository.getAverageRatingForRelationship(relationshipId);
        
        Long totalCheckins = checkinRepository.countRecentCheckinsForRelationship(
            relationshipId, LocalDateTime.of(2020, 1, 1, 0, 0)
        );
        
        return BuddyRelationshipStatsDTO.builder()
            .relationshipId(relationshipId)
            .startDate(relationship.getStartDate())
            .totalGoals(totalGoals)
            .completedGoals(completedGoals)
            .totalSessions(totalSessions)
            .completedSessions(completedSessions)
            .averageSessionDuration(avgSessionDuration)
            .averageSessionRating(avgSessionRating)
            .totalCheckins(totalCheckins)
            .build();
    }
    
    @Override
    public UserBuddyStatsDTO getUserBuddyStats(String userId) {
        Long activeBuddies = relationshipRepository.countActiveBuddiesForUser(userId);
        List<BuddyGoal> userGoals = goalRepository.findActiveGoalsForUser(userId);
        
        return UserBuddyStatsDTO.builder()
            .userId(userId)
            .activeBuddies(activeBuddies)
            .totalGoalsCreated((long) userGoals.size())
            .build();
    }
    
    @Override
    public void notifyBuddyRequest(String fromUserId, String toUserId) {
        // TODO: Implement WebSocket notification
        log.info("Notifying user {} of buddy request from user {}", toUserId, fromUserId);
    }
    
    @Override
    public void notifyBuddyAcceptance(Long relationshipId) {
        // TODO: Implement WebSocket notification
        log.info("Notifying buddy acceptance for relationship {}", relationshipId);
    }
    
    @Override
    public void notifyUpcomingSession(Long sessionId) {
        // TODO: Implement notification scheduling
        log.info("Scheduling notification for upcoming session {}", sessionId);
    }
    
    @Override
    public void notifyGoalDeadline(Long goalId) {
        // TODO: Implement notification scheduling
        log.info("Scheduling notification for goal deadline {}", goalId);
    }
    
    @Override
    public List<BuddyRelationshipDTO> getActiveRelationships() {
        List<BuddyRelationship> relationships = relationshipRepository.findAll()
            .stream()
            .filter(r -> r.getStatus() == BuddyRelationship.BuddyStatus.ACTIVE)
            .collect(Collectors.toList());
        return relationships.stream()
            .map(r -> mapToRelationshipDTO(r, null))
            .collect(Collectors.toList());
    }
    
    // Helper methods
    
    private BuddyRelationshipDTO mapToRelationshipDTO(BuddyRelationship relationship, String currentUserId) {
        BuddyRelationshipDTO dto = BuddyRelationshipDTO.builder()
            .id(relationship.getId())
            .user1Id(relationship.getUser1().getId())
            .user1Username(relationship.getUser1().getUsername())
            .user2Id(relationship.getUser2().getId())
            .user2Username(relationship.getUser2().getUsername())
            .status(relationship.getStatus())
            .startDate(relationship.getStartDate())
            .endDate(relationship.getEndDate())
            .terminationReason(relationship.getTerminationReason())
            .createdAt(relationship.getCreatedAt())
            .updatedAt(relationship.getUpdatedAt())
            .build();
        
        if (currentUserId != null) {
            String partnerId = relationship.getPartnerId(currentUserId);
            if (partnerId != null) {
                User partner = relationship.getUser1().getId().equals(partnerId) ? 
                    relationship.getUser1() : relationship.getUser2();
                dto.setPartnerId(partnerId);
                dto.setPartnerUsername(partner.getUsername());
            }
            dto.setInitiator(relationship.getUser1().getId().equals(currentUserId));
        }
        
        return dto;
    }
    
    private BuddyMatchDTO createMatchDTO(User user, BuddyMatchScoreDTO score) {
        return BuddyMatchDTO.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .matchScore(score.getTotalScore())
            .matchReasons(score.getScoreBreakdown())
            .build();
    }
    
    private double calculateFocusAreaScore(BuddyPreferences prefs1, BuddyPreferences prefs2) {
        if (prefs1.getFocusAreas() == null || prefs2.getFocusAreas() == null) {
            return 0.0;
        }
        
        long overlap = prefs1.countFocusAreaOverlap(prefs2.getFocusAreas());
        int total = Math.max(prefs1.getFocusAreas().size(), prefs2.getFocusAreas().size());
        
        return total > 0 ? (double) overlap / total : 0.0;
    }
    
    private double calculateTimezoneScore(BuddyPreferences prefs1, BuddyPreferences prefs2) {
        if (prefs1.getPreferredTimezone() == null || prefs2.getPreferredTimezone() == null) {
            return 0.5; // Neutral score if timezone not specified
        }
        
        // Simplified - same timezone gets full score
        return prefs1.getPreferredTimezone().equals(prefs2.getPreferredTimezone()) ? 1.0 : 0.3;
    }
    
    private double calculateCommunicationScore(BuddyPreferences prefs1, BuddyPreferences prefs2) {
        if (prefs1.getCommunicationStyle() == null || prefs2.getCommunicationStyle() == null) {
            return 0.5;
        }
        
        if (prefs1.getCommunicationStyle() == prefs2.getCommunicationStyle()) {
            return 1.0;
        }
        
        // Adjacent styles are somewhat compatible
        CommunicationStyle style1 = prefs1.getCommunicationStyle();
        CommunicationStyle style2 = prefs2.getCommunicationStyle();
        
        if ((style1 == CommunicationStyle.FREQUENT && style2 == CommunicationStyle.MODERATE) ||
            (style1 == CommunicationStyle.MODERATE && style2 == CommunicationStyle.FREQUENT) ||
            (style1 == CommunicationStyle.MODERATE && style2 == CommunicationStyle.MINIMAL) ||
            (style1 == CommunicationStyle.MINIMAL && style2 == CommunicationStyle.MODERATE)) {
            return 0.7;
        }
        
        return 0.3;
    }
    
    private double calculateAvailabilityScore(BuddyPreferences prefs1, BuddyPreferences prefs2) {
        if (prefs1.getPreferredWorkHours() == null || prefs2.getPreferredWorkHours() == null) {
            return 0.5;
        }
        
        int totalOverlap = 0;
        int dayCount = 0;
        
        for (Map.Entry<String, BuddyPreferences.WorkHours> entry : prefs1.getPreferredWorkHours().entrySet()) {
            BuddyPreferences.WorkHours hours2 = prefs2.getPreferredWorkHours().get(entry.getKey());
            if (hours2 != null) {
                totalOverlap += entry.getValue().overlapHours(hours2);
                dayCount++;
            }
        }
        
        if (dayCount == 0) return 0.0;
        
        // Average overlap hours per day, normalized (assuming max 8 hours overlap is excellent)
        double avgOverlap = (double) totalOverlap / dayCount;
        return Math.min(avgOverlap / 8.0, 1.0);
    }
    
    private BuddyPreferences createDefaultPreferences(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        return BuddyPreferences.builder()
            .user(user)
            .matchingEnabled(true)
            .communicationStyle(CommunicationStyle.MODERATE)
            .build();
    }
    
    private BuddyPreferencesDTO mapToPreferencesDTO(BuddyPreferences preferences) {
        return BuddyPreferencesDTO.builder()
            .id(preferences.getId())
            .userId(preferences.getUser().getId())
            .preferredTimezone(preferences.getPreferredTimezone())
            .preferredWorkHours(preferences.getPreferredWorkHours())
            .focusAreas(preferences.getFocusAreas())
            .communicationStyle(preferences.getCommunicationStyle())
            .matchingEnabled(preferences.getMatchingEnabled())
            .build();
    }
    
    private BuddyGoalDTO mapToGoalDTO(BuddyGoal goal) {
        BuddyGoalDTO dto = BuddyGoalDTO.builder()
            .id(goal.getId())
            .relationshipId(goal.getRelationship().getId())
            .title(goal.getTitle())
            .description(goal.getDescription())
            .status(goal.getStatus())
            .dueDate(goal.getDueDate())
            .completedAt(goal.getCompletedAt())
            .metrics(goal.getMetrics())
            .progressPercentage(goal.getProgressPercentage())
            .createdAt(goal.getCreatedAt())
            .updatedAt(goal.getUpdatedAt())
            .build();
        
        if (goal.getCompletedBy() != null) {
            dto.setCompletedBy(goal.getCompletedBy().getId());
            dto.setCompletedByUsername(goal.getCompletedBy().getUsername());
        }
        
        return dto;
    }
    
    private BuddyCheckinDTO mapToCheckinDTO(BuddyCheckin checkin) {
        return BuddyCheckinDTO.builder()
            .id(checkin.getId())
            .relationshipId(checkin.getRelationship().getId())
            .initiatedById(checkin.getInitiatedBy().getId())
            .initiatedByUsername(checkin.getInitiatedBy().getUsername())
            .checkinTime(checkin.getCheckinTime())
            .moodRating(checkin.getMoodRating())
            .progressRating(checkin.getProgressRating())
            .message(checkin.getMessage())
            .currentFocus(checkin.getCurrentFocus())
            .challenges(checkin.getChallenges())
            .wins(checkin.getWins())
            .createdAt(checkin.getCreatedAt())
            .build();
    }
    
    private BuddySessionDTO mapToSessionDTO(BuddySession session) {
        return BuddySessionDTO.builder()
            .id(session.getId())
            .relationshipId(session.getRelationship().getId())
            .sessionDate(session.getSessionDate())
            .plannedDurationMinutes(session.getPlannedDurationMinutes())
            .actualDurationMinutes(session.getActualDurationMinutes())
            .agenda(session.getAgenda())
            .notes(session.getNotes())
            .status(session.getStatus())
            .user1Joined(session.getUser1Joined())
            .user1Left(session.getUser1Left())
            .user2Joined(session.getUser2Joined())
            .user2Left(session.getUser2Left())
            .user1Rating(session.getUser1Rating())
            .user1Feedback(session.getUser1Feedback())
            .user2Rating(session.getUser2Rating())
            .user2Feedback(session.getUser2Feedback())
            .cancelledAt(session.getCancelledAt())
            .cancelledBy(session.getCancelledBy())
            .cancellationReason(session.getCancellationReason())
            .averageRating(session.getAverageRating())
            .createdAt(session.getCreatedAt())
            .build();
    }
}