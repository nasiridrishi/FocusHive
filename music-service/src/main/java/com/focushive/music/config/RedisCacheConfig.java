package com.focushive.music.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration for the music recommendation system.
 * Provides sophisticated caching strategies with different TTLs for various data types.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "music.cache")
public class RedisCacheConfig {

    /**
     * Cache configuration properties.
     */
    private final CacheProperties cacheProperties = new CacheProperties();

    /**
     * Configures the Redis cache manager with custom TTL settings.
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Configuring Redis cache manager for music recommendations");

        RedisCacheConfiguration defaultConfig = createDefaultCacheConfiguration();
        
        Map<String, RedisCacheConfiguration> cacheConfigurations = createCacheConfigurations();

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();

        log.info("Redis cache manager configured with {} cache types", cacheConfigurations.size());
        return cacheManager;
    }

    /**
     * Customizer for fine-tuning cache manager builder.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> {
            builder
                .cacheDefaults(createDefaultCacheConfiguration())
                .withCacheConfiguration("music:recommendations", 
                    createCacheConfiguration(cacheProperties.getRecommendationTtl()))
                .withCacheConfiguration("music:user:preferences", 
                    createCacheConfiguration(cacheProperties.getUserPreferencesTtl()))
                .withCacheConfiguration("music:analytics", 
                    createCacheConfiguration(cacheProperties.getAnalyticsTtl()))
                .withCacheConfiguration("music:feedback", 
                    createCacheConfiguration(cacheProperties.getFeedbackTtl()))
                .withCacheConfiguration("music:history", 
                    createCacheConfiguration(cacheProperties.getHistoryTtl()))
                .withCacheConfiguration("music:spotify:tracks", 
                    createCacheConfiguration(cacheProperties.getSpotifyDataTtl()))
                .withCacheConfiguration("music:collaborative", 
                    createCacheConfiguration(cacheProperties.getCollaborativeTtl()));

            log.info("Redis cache manager customizer applied");
        };
    }

    /**
     * Redis template for manual cache operations.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use Jackson serializer for values
        ObjectMapper objectMapper = createObjectMapper();
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(objectMapper);
        
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        log.info("RedisTemplate configured for music service");
        return template;
    }

    /**
     * Creates default cache configuration.
     */
    private RedisCacheConfiguration createDefaultCacheConfiguration() {
        ObjectMapper objectMapper = createObjectMapper();
        
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheProperties.getDefaultTtl())
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)))
                .disableCachingNullValues()
                .prefixCacheNameWith("focushive:music:")
                .computePrefixWith(cacheName -> "focushive:music:" + cacheName + ":");
    }

    /**
     * Creates cache configuration with specific TTL.
     */
    private RedisCacheConfiguration createCacheConfiguration(Duration ttl) {
        return createDefaultCacheConfiguration()
                .entryTtl(ttl);
    }

    /**
     * Creates all cache type configurations.
     */
    private Map<String, RedisCacheConfiguration> createCacheConfigurations() {
        Map<String, RedisCacheConfiguration> configurations = new HashMap<>();

        // Recommendation cache - 1 hour TTL
        configurations.put("music:recommendations", 
            createCacheConfiguration(cacheProperties.getRecommendationTtl()));

        // User preferences cache - 6 hours TTL
        configurations.put("music:user:preferences", 
            createCacheConfiguration(cacheProperties.getUserPreferencesTtl()));

        // Analytics cache - 30 minutes TTL
        configurations.put("music:analytics", 
            createCacheConfiguration(cacheProperties.getAnalyticsTtl()));

        // Feedback cache - 2 hours TTL
        configurations.put("music:feedback", 
            createCacheConfiguration(cacheProperties.getFeedbackTtl()));

        // History cache - 4 hours TTL
        configurations.put("music:history", 
            createCacheConfiguration(cacheProperties.getHistoryTtl()));

        // Spotify data cache - 24 hours TTL
        configurations.put("music:spotify:tracks", 
            createCacheConfiguration(cacheProperties.getSpotifyDataTtl()));

        // Collaborative cache - 15 minutes TTL (more dynamic)
        configurations.put("music:collaborative", 
            createCacheConfiguration(cacheProperties.getCollaborativeTtl()));

        // Session cache - 8 hours TTL
        configurations.put("music:sessions", 
            createCacheConfiguration(cacheProperties.getSessionTtl()));

        // Algorithm cache - 12 hours TTL
        configurations.put("music:algorithms", 
            createCacheConfiguration(cacheProperties.getAlgorithmTtl()));

        return configurations;
    }

    /**
     * Creates ObjectMapper for JSON serialization.
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

    /**
     * Cache properties configuration class.
     */
    public static class CacheProperties {
        private Duration defaultTtl = Duration.ofMinutes(30);
        private Duration recommendationTtl = Duration.ofHours(1);
        private Duration userPreferencesTtl = Duration.ofHours(6);
        private Duration analyticsTtl = Duration.ofMinutes(30);
        private Duration feedbackTtl = Duration.ofHours(2);
        private Duration historyTtl = Duration.ofHours(4);
        private Duration spotifyDataTtl = Duration.ofHours(24);
        private Duration collaborativeTtl = Duration.ofMinutes(15);
        private Duration sessionTtl = Duration.ofHours(8);
        private Duration algorithmTtl = Duration.ofHours(12);

        // Getters and setters
        public Duration getDefaultTtl() { return defaultTtl; }
        public void setDefaultTtl(Duration defaultTtl) { this.defaultTtl = defaultTtl; }

        public Duration getRecommendationTtl() { return recommendationTtl; }
        public void setRecommendationTtl(Duration recommendationTtl) { this.recommendationTtl = recommendationTtl; }

        public Duration getUserPreferencesTtl() { return userPreferencesTtl; }
        public void setUserPreferencesTtl(Duration userPreferencesTtl) { this.userPreferencesTtl = userPreferencesTtl; }

        public Duration getAnalyticsTtl() { return analyticsTtl; }
        public void setAnalyticsTtl(Duration analyticsTtl) { this.analyticsTtl = analyticsTtl; }

        public Duration getFeedbackTtl() { return feedbackTtl; }
        public void setFeedbackTtl(Duration feedbackTtl) { this.feedbackTtl = feedbackTtl; }

        public Duration getHistoryTtl() { return historyTtl; }
        public void setHistoryTtl(Duration historyTtl) { this.historyTtl = historyTtl; }

        public Duration getSpotifyDataTtl() { return spotifyDataTtl; }
        public void setSpotifyDataTtl(Duration spotifyDataTtl) { this.spotifyDataTtl = spotifyDataTtl; }

        public Duration getCollaborativeTtl() { return collaborativeTtl; }
        public void setCollaborativeTtl(Duration collaborativeTtl) { this.collaborativeTtl = collaborativeTtl; }

        public Duration getSessionTtl() { return sessionTtl; }
        public void setSessionTtl(Duration sessionTtl) { this.sessionTtl = sessionTtl; }

        public Duration getAlgorithmTtl() { return algorithmTtl; }
        public void setAlgorithmTtl(Duration algorithmTtl) { this.algorithmTtl = algorithmTtl; }
    }

    /**
     * Utility class for generating cache keys.
     */
    public static class CacheKeyGenerator {
        
        private static final String RECOMMENDATION_KEY_PREFIX = "music:recommendations:user:";
        private static final String USER_PREFERENCES_KEY_PREFIX = "music:user:preferences:";
        private static final String ANALYTICS_KEY_PREFIX = "music:analytics:user:";
        private static final String FEEDBACK_KEY_PREFIX = "music:feedback:user:";
        private static final String HISTORY_KEY_PREFIX = "music:history:user:";
        private static final String SPOTIFY_KEY_PREFIX = "music:spotify:track:";
        private static final String COLLABORATIVE_KEY_PREFIX = "music:collaborative:hive:";

        public static String recommendationKey(String userId, String taskType) {
            return RECOMMENDATION_KEY_PREFIX + userId + ":task:" + taskType;
        }

        public static String recommendationKeyWithMood(String userId, String taskType, String mood) {
            return RECOMMENDATION_KEY_PREFIX + userId + ":task:" + taskType + ":mood:" + mood;
        }

        public static String userPreferencesKey(String userId) {
            return USER_PREFERENCES_KEY_PREFIX + userId;
        }

        public static String analyticsKey(String userId, String metric) {
            return ANALYTICS_KEY_PREFIX + userId + ":metric:" + metric;
        }

        public static String feedbackKey(String userId, String trackId) {
            return FEEDBACK_KEY_PREFIX + userId + ":track:" + trackId;
        }

        public static String historyKey(String userId, String period) {
            return HISTORY_KEY_PREFIX + userId + ":period:" + period;
        }

        public static String spotifyTrackKey(String trackId) {
            return SPOTIFY_KEY_PREFIX + trackId;
        }

        public static String collaborativeKey(String hiveId, String context) {
            return COLLABORATIVE_KEY_PREFIX + hiveId + ":context:" + context;
        }

        public static String sessionRecommendationKey(String userId, String sessionId) {
            return RECOMMENDATION_KEY_PREFIX + userId + ":session:" + sessionId;
        }

        public static String timeContextKey(String userId, int hour) {
            return RECOMMENDATION_KEY_PREFIX + userId + ":time:" + hour;
        }

        public static String algorithmPerformanceKey(String userId, String algorithmVersion) {
            return "music:algorithms:performance:user:" + userId + ":version:" + algorithmVersion;
        }
    }

    /**
     * Cache statistics collector.
     */
    @Bean
    public CacheStatsCollector cacheStatsCollector() {
        return new CacheStatsCollector();
    }

    public static class CacheStatsCollector {
        
        private final Map<String, CacheStats> stats = new HashMap<>();

        public void recordHit(String cacheName) {
            stats.computeIfAbsent(cacheName, k -> new CacheStats()).incrementHits();
        }

        public void recordMiss(String cacheName) {
            stats.computeIfAbsent(cacheName, k -> new CacheStats()).incrementMisses();
        }

        public void recordEviction(String cacheName) {
            stats.computeIfAbsent(cacheName, k -> new CacheStats()).incrementEvictions();
        }

        public Map<String, CacheStats> getStats() {
            return new HashMap<>(stats);
        }

        public void resetStats() {
            stats.clear();
        }
    }

    public static class CacheStats {
        private long hits = 0;
        private long misses = 0;
        private long evictions = 0;

        public void incrementHits() { hits++; }
        public void incrementMisses() { misses++; }
        public void incrementEvictions() { evictions++; }

        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getEvictions() { return evictions; }
        public double getHitRatio() { 
            long total = hits + misses;
            return total == 0 ? 0.0 : (double) hits / total;
        }
    }
}