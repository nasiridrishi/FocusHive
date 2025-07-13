package com.focushive.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // Use JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * Redis Key Patterns:
     * 
     * User Presence:
     * - presence:user:{userId} - Hash containing status, lastSeen, currentHive, activity
     * - hive:{hiveId}:members - Set of active user IDs in a hive
     * - user:{userId}:hives - Set of hive IDs user is currently in
     * 
     * Sessions:
     * - session:user:{userId} - Hash containing active session data
     * - session:{sessionId} - Hash containing session details
     * 
     * Caching:
     * - cache:user:{userId} - Cached user profile data
     * - cache:hive:{hiveId} - Cached hive data
     * - cache:summary:{userId}:{date} - Cached daily summary
     * - cache:leaderboard:{hiveId}:{period} - Sorted set for leaderboard
     * 
     * Rate Limiting:
     * - rate:api:{userId}:{endpoint} - Counter for API rate limiting
     * - rate:session:{userId} - Counter for session creation limit
     * 
     * Real-time Features:
     * - notifications:{userId} - List of pending notifications
     * - ws:connections:{userId} - Set of WebSocket connection IDs
     * - typing:{hiveId}:{userId} - Flag for typing indicator
     * - online:users - Set of all online user IDs
     * 
     * Temporary Data:
     * - temp:invite:{code} - Invitation details
     * - temp:verification:{token} - Email verification data
     * - temp:reset:{token} - Password reset data
     */
    
    public static class RedisKeys {
        // Presence
        public static String userPresence(String userId) {
            return "presence:user:" + userId;
        }
        
        public static String hiveMembers(String hiveId) {
            return "hive:" + hiveId + ":members";
        }
        
        public static String userHives(String userId) {
            return "user:" + userId + ":hives";
        }
        
        // Sessions
        public static String userSession(String userId) {
            return "session:user:" + userId;
        }
        
        public static String sessionData(String sessionId) {
            return "session:" + sessionId;
        }
        
        // Caching
        public static String userCache(String userId) {
            return "cache:user:" + userId;
        }
        
        public static String hiveCache(String hiveId) {
            return "cache:hive:" + hiveId;
        }
        
        public static String dailySummaryCache(String userId, String date) {
            return "cache:summary:" + userId + ":" + date;
        }
        
        public static String leaderboardCache(String hiveId, String period) {
            return "cache:leaderboard:" + hiveId + ":" + period;
        }
        
        // Rate Limiting
        public static String apiRateLimit(String userId, String endpoint) {
            return "rate:api:" + userId + ":" + endpoint;
        }
        
        public static String sessionRateLimit(String userId) {
            return "rate:session:" + userId;
        }
        
        // Real-time
        public static String userNotifications(String userId) {
            return "notifications:" + userId;
        }
        
        public static String wsConnections(String userId) {
            return "ws:connections:" + userId;
        }
        
        public static String typingIndicator(String hiveId, String userId) {
            return "typing:" + hiveId + ":" + userId;
        }
        
        public static String onlineUsers() {
            return "online:users";
        }
        
        // Temporary
        public static String inviteCode(String code) {
            return "temp:invite:" + code;
        }
        
        public static String verificationToken(String token) {
            return "temp:verification:" + token;
        }
        
        public static String resetToken(String token) {
            return "temp:reset:" + token;
        }
    }
}