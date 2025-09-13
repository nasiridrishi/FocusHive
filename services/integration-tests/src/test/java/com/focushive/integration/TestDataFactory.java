package com.focushive.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Factory class for creating test data objects used across cross-service integration tests.
 * 
 * Provides:
 * - Realistic test data that matches production patterns
 * - Consistent data formats across all services
 * - Builder patterns for flexible test scenarios
 * - JSON representations for API testing
 */
public class TestDataFactory {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // User test data
    public static class Users {
        
        public static Map<String, Object> createTestUser(String email, String name) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "email", email,
                "name", name,
                "password", "TestPassword123!",
                "personas", List.of("work", "study"),
                "createdAt", LocalDateTime.now().format(ISO_FORMATTER),
                "isActive", true,
                "emailVerified", true
            );
        }

        public static Map<String, Object> createUserWithPersonas(String email, String name, List<String> personas) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "email", email,
                "name", name,
                "password", "TestPassword123!",
                "personas", personas,
                "createdAt", LocalDateTime.now().format(ISO_FORMATTER),
                "isActive", true,
                "emailVerified", true,
                "preferences", Map.of(
                    "notifications", Map.of(
                        "email", true,
                        "push", true,
                        "inApp", true
                    ),
                    "privacy", Map.of(
                        "showActivity", true,
                        "shareProgress", false
                    )
                )
            );
        }

        public static String createUserJson(String email, String name) {
            try {
                return objectMapper.writeValueAsString(createTestUser(email, name));
            } catch (Exception e) {
                throw new RuntimeException("Failed to create user JSON", e);
            }
        }
    }

    // Hive test data
    public static class Hives {
        
        public static Map<String, Object> createTestHive(String name, String creatorId) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "name", name,
                "description", "Test hive for " + name,
                "category", "study",
                "maxMembers", 10,
                "currentMembers", 1,
                "isPrivate", false,
                "createdBy", creatorId,
                "createdAt", LocalDateTime.now().format(ISO_FORMATTER),
                "settings", Map.of(
                    "allowMusic", true,
                    "requireCamera", false,
                    "sessionDuration", 25,
                    "breakDuration", 5
                )
            );
        }

        public static Map<String, Object> createPrivateHive(String name, String creatorId, List<String> invitedUsers) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "name", name,
                "description", "Private test hive",
                "category", "work",
                "maxMembers", 5,
                "currentMembers", 1,
                "isPrivate", true,
                "createdBy", creatorId,
                "invitedUsers", invitedUsers,
                "createdAt", LocalDateTime.now().format(ISO_FORMATTER),
                "settings", Map.of(
                    "allowMusic", false,
                    "requireCamera", true,
                    "sessionDuration", 50,
                    "breakDuration", 10
                )
            );
        }

        public static String createHiveJson(String name, String creatorId) {
            try {
                return objectMapper.writeValueAsString(createTestHive(name, creatorId));
            } catch (Exception e) {
                throw new RuntimeException("Failed to create hive JSON", e);
            }
        }
    }

    // Timer session test data
    public static class TimerSessions {
        
        public static Map<String, Object> createTimerSession(String userId, String hiveId, int durationMinutes) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "userId", userId,
                "hiveId", hiveId,
                "type", "FOCUS",
                "plannedDuration", durationMinutes,
                "startTime", LocalDateTime.now().format(ISO_FORMATTER),
                "status", "ACTIVE",
                "settings", Map.of(
                    "allowInterruptions", false,
                    "trackProductivity", true,
                    "enableBreakReminders", true
                )
            );
        }

        public static Map<String, Object> createCompletedSession(String userId, String hiveId, int actualMinutes, int productivityScore) {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusMinutes(actualMinutes);
            
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "userId", userId,
                "hiveId", hiveId,
                "type", "FOCUS",
                "plannedDuration", actualMinutes,
                "actualDuration", actualMinutes,
                "startTime", startTime.format(ISO_FORMATTER),
                "endTime", endTime.format(ISO_FORMATTER),
                "status", "COMPLETED",
                "productivityScore", productivityScore,
                "interruptions", 0,
                "achievements", List.of("FOCUS_MASTER", "TIME_KEEPER")
            );
        }

        public static String createTimerSessionJson(String userId, String hiveId, int durationMinutes) {
            try {
                return objectMapper.writeValueAsString(createTimerSession(userId, hiveId, durationMinutes));
            } catch (Exception e) {
                throw new RuntimeException("Failed to create timer session JSON", e);
            }
        }
    }

    // Analytics events test data
    public static class AnalyticsEvents {
        
        public static Map<String, Object> createUserJoinEvent(String userId, String hiveId) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "eventType", "USER_JOINED_HIVE",
                "userId", userId,
                "hiveId", hiveId,
                "timestamp", LocalDateTime.now().format(ISO_FORMATTER),
                "metadata", Map.of(
                    "joinMethod", "invitation",
                    "userPersona", "work"
                )
            );
        }

        public static Map<String, Object> createSessionStartEvent(String userId, String hiveId, String sessionId) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "eventType", "TIMER_SESSION_STARTED",
                "userId", userId,
                "hiveId", hiveId,
                "sessionId", sessionId,
                "timestamp", LocalDateTime.now().format(ISO_FORMATTER),
                "metadata", Map.of(
                    "sessionType", "FOCUS",
                    "plannedDuration", 25,
                    "hiveActiveMembers", 3
                )
            );
        }

        public static Map<String, Object> createSessionCompleteEvent(String userId, String hiveId, String sessionId, int actualMinutes, int productivityScore) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "eventType", "TIMER_SESSION_COMPLETED",
                "userId", userId,
                "hiveId", hiveId,
                "sessionId", sessionId,
                "timestamp", LocalDateTime.now().format(ISO_FORMATTER),
                "metadata", Map.of(
                    "actualDuration", actualMinutes,
                    "productivityScore", productivityScore,
                    "achievements", List.of("FOCUS_MASTER"),
                    "interruptions", 0
                )
            );
        }

        public static Map<String, Object> createAchievementEvent(String userId, String achievementType, Map<String, Object> criteria) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "eventType", "ACHIEVEMENT_EARNED",
                "userId", userId,
                "timestamp", LocalDateTime.now().format(ISO_FORMATTER),
                "metadata", Map.of(
                    "achievementType", achievementType,
                    "criteria", criteria,
                    "pointsEarned", 100,
                    "level", "BRONZE"
                )
            );
        }
    }

    // Notification test data
    public static class Notifications {
        
        public static Map<String, Object> createHiveJoinNotification(String recipientId, String senderName, String hiveName) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "type", "HIVE_NEW_MEMBER",
                "recipientId", recipientId,
                "title", "New member joined your hive",
                "message", senderName + " joined " + hiveName,
                "channels", List.of("IN_APP", "EMAIL"),
                "priority", "MEDIUM",
                "timestamp", LocalDateTime.now().format(ISO_FORMATTER),
                "metadata", Map.of(
                    "hiveName", hiveName,
                    "senderName", senderName,
                    "actionUrl", "/hives/" + UUID.randomUUID().toString()
                )
            );
        }

        public static Map<String, Object> createTimerStartNotification(String recipientId, String userName, String hiveName) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "type", "TIMER_SESSION_STARTED",
                "recipientId", recipientId,
                "title", "Focus session started",
                "message", userName + " started a focus session in " + hiveName,
                "channels", List.of("IN_APP", "PUSH"),
                "priority", "LOW",
                "timestamp", LocalDateTime.now().format(ISO_FORMATTER),
                "metadata", Map.of(
                    "hiveName", hiveName,
                    "userName", userName,
                    "sessionDuration", 25
                )
            );
        }

        public static Map<String, Object> createAchievementNotification(String recipientId, String achievementType) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "type", "ACHIEVEMENT_EARNED",
                "recipientId", recipientId,
                "title", "Achievement unlocked!",
                "message", "You earned the " + achievementType + " achievement",
                "channels", List.of("IN_APP", "PUSH", "EMAIL"),
                "priority", "HIGH",
                "timestamp", LocalDateTime.now().format(ISO_FORMATTER),
                "metadata", Map.of(
                    "achievementType", achievementType,
                    "pointsEarned", 100,
                    "celebrationGif", "achievement_celebration.gif"
                )
            );
        }
    }

    // Buddy system test data
    public static class BuddyData {
        
        public static Map<String, Object> createBuddyPair(String user1Id, String user2Id) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "user1Id", user1Id,
                "user2Id", user2Id,
                "status", "ACTIVE",
                "matchedAt", LocalDateTime.now().format(ISO_FORMATTER),
                "compatibility", Map.of(
                    "workingHours", 0.85,
                    "focusStyle", 0.92,
                    "goals", 0.78
                ),
                "preferences", Map.of(
                    "checkInFrequency", "DAILY",
                    "sharedGoals", true,
                    "competitiveMode", false
                )
            );
        }

        public static Map<String, Object> createBuddySession(String buddyPairId, String user1Id, String user2Id, int durationMinutes) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "buddyPairId", buddyPairId,
                "participants", List.of(user1Id, user2Id),
                "type", "JOINT_FOCUS",
                "plannedDuration", durationMinutes,
                "startTime", LocalDateTime.now().format(ISO_FORMATTER),
                "status", "ACTIVE",
                "goals", List.of(
                    Map.of("userId", user1Id, "goal", "Complete project proposal"),
                    Map.of("userId", user2Id, "goal", "Study for exam")
                )
            );
        }

        public static Map<String, Object> createAccountabilityCheckIn(String buddyPairId, String fromUserId, String toUserId) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "buddyPairId", buddyPairId,
                "fromUserId", fromUserId,
                "toUserId", toUserId,
                "type", "PROGRESS_CHECK",
                "message", "How's your progress going today?",
                "timestamp", LocalDateTime.now().format(ISO_FORMATTER),
                "requiresResponse", true,
                "deadline", LocalDateTime.now().plusHours(24).format(ISO_FORMATTER)
            );
        }
    }

    // Productivity metrics test data
    public static class ProductivityMetrics {
        
        public static Map<String, Object> createDailyMetrics(String userId, LocalDateTime date, double focusHours, int sessionsCompleted) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "userId", userId,
                "date", date.toLocalDate().toString(),
                "focusHours", focusHours,
                "sessionsCompleted", sessionsCompleted,
                "productivityScore", calculateProductivityScore(focusHours, sessionsCompleted),
                "achievements", List.of("DAILY_ACHIEVER"),
                "breakdown", Map.of(
                    "deepWork", focusHours * 0.7,
                    "collaboration", focusHours * 0.2,
                    "learning", focusHours * 0.1
                )
            );
        }

        public static Map<String, Object> createWeeklyMetrics(String userId, int weekNumber, double averageFocusHours, int totalSessions) {
            return Map.of(
                "id", UUID.randomUUID().toString(),
                "userId", userId,
                "weekNumber", weekNumber,
                "year", 2024,
                "averageFocusHours", averageFocusHours,
                "totalSessions", totalSessions,
                "weeklyScore", calculateWeeklyScore(averageFocusHours, totalSessions),
                "trends", Map.of(
                    "focusHoursTrend", "+12%",
                    "sessionCompletionRate", "85%",
                    "consistencyScore", 0.78
                )
            );
        }

        private static int calculateProductivityScore(double focusHours, int sessionsCompleted) {
            return Math.min(100, (int) ((focusHours * 10) + (sessionsCompleted * 5)));
        }

        private static int calculateWeeklyScore(double averageFocusHours, int totalSessions) {
            return Math.min(100, (int) ((averageFocusHours * 8) + (totalSessions * 2)));
        }
    }

    // Utility methods for JSON conversion
    public static String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert data to JSON", e);
        }
    }

    public static ObjectNode createJsonNode() {
        return objectMapper.createObjectNode();
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
}