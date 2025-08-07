package com.focushive.music.event;

import com.focushive.music.client.AnalyticsServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Publisher for music-related events.
 * 
 * Handles publishing events to internal event system, WebSocket clients,
 * and external services for analytics and monitoring.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MusicEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final AnalyticsServiceClient analyticsServiceClient;

    /**
     * Publishes music session started event.
     * 
     * @param userId User ID
     * @param sessionId Session ID
     * @param hiveId Hive ID (optional)
     * @param musicEnabled Whether music is enabled
     */
    @Async
    public void publishMusicSessionStarted(UUID userId, UUID sessionId, UUID hiveId, boolean musicEnabled) {
        log.debug("Publishing music session started event for sessionId: {}", sessionId);
        
        try {
            // Create and publish internal event
            MusicSessionStartedEvent event = new MusicSessionStartedEvent(
                userId, sessionId, hiveId, musicEnabled, LocalDateTime.now()
            );
            applicationEventPublisher.publishEvent(event);
            
            // Send WebSocket message to user
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/music/session/started",
                Map.of(
                    "sessionId", sessionId,
                    "hiveId", hiveId,
                    "musicEnabled", musicEnabled,
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            // Send WebSocket message to hive if applicable
            if (hiveId != null) {
                messagingTemplate.convertAndSend(
                    "/topic/hive/" + hiveId + "/music/session/started",
                    Map.of(
                        "userId", userId,
                        "sessionId", sessionId,
                        "musicEnabled", musicEnabled,
                        "timestamp", System.currentTimeMillis()
                    )
                );
            }
            
            // Send to analytics service
            recordAnalyticsEvent("music_session_started", userId, sessionId, hiveId, Map.of(
                "musicEnabled", musicEnabled
            ));
            
        } catch (Exception e) {
            log.error("Error publishing music session started event", e);
        }
    }

    /**
     * Publishes music session ended event.
     * 
     * @param userId User ID
     * @param sessionId Session ID
     * @param hiveId Hive ID (optional)
     * @param duration Session duration
     * @param metrics Session metrics
     */
    @Async
    public void publishMusicSessionEnded(UUID userId, UUID sessionId, UUID hiveId, 
                                        long duration, Map<String, Object> metrics) {
        log.debug("Publishing music session ended event for sessionId: {}", sessionId);
        
        try {
            // Create and publish internal event
            MusicSessionEndedEvent event = new MusicSessionEndedEvent(
                userId, sessionId, hiveId, duration, metrics, LocalDateTime.now()
            );
            applicationEventPublisher.publishEvent(event);
            
            // Send WebSocket message to user
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/music/session/ended",
                Map.of(
                    "sessionId", sessionId,
                    "hiveId", hiveId,
                    "duration", duration,
                    "metrics", metrics,
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            // Send WebSocket message to hive if applicable
            if (hiveId != null) {
                messagingTemplate.convertAndSend(
                    "/topic/hive/" + hiveId + "/music/session/ended",
                    Map.of(
                        "userId", userId,
                        "sessionId", sessionId,
                        "duration", duration,
                        "timestamp", System.currentTimeMillis()
                    )
                );
            }
            
            // Send to analytics service
            recordAnalyticsEvent("music_session_ended", userId, sessionId, hiveId, Map.of(
                "duration", duration,
                "metrics", metrics
            ));
            
        } catch (Exception e) {
            log.error("Error publishing music session ended event", e);
        }
    }

    /**
     * Publishes track played event.
     * 
     * @param userId User ID
     * @param sessionId Session ID
     * @param hiveId Hive ID (optional)
     * @param trackId Track ID
     * @param artistId Artist ID
     * @param playlistId Playlist ID (optional)
     */
    @Async
    public void publishTrackPlayed(UUID userId, UUID sessionId, UUID hiveId, 
                                  String trackId, String artistId, UUID playlistId) {
        log.debug("Publishing track played event for trackId: {}", trackId);
        
        try {
            // Send WebSocket message to user
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/music/track/played",
                Map.of(
                    "sessionId", sessionId,
                    "trackId", trackId,
                    "artistId", artistId,
                    "playlistId", playlistId,
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            // Send WebSocket message to hive if applicable
            if (hiveId != null) {
                messagingTemplate.convertAndSend(
                    "/topic/hive/" + hiveId + "/music/track/played",
                    Map.of(
                        "userId", userId,
                        "sessionId", sessionId,
                        "trackId", trackId,
                        "artistId", artistId,
                        "timestamp", System.currentTimeMillis()
                    )
                );
            }
            
            // Send to analytics service
            recordAnalyticsEvent("track_played", userId, sessionId, hiveId, Map.of(
                "trackId", trackId,
                "artistId", artistId,
                "playlistId", playlistId
            ));
            
        } catch (Exception e) {
            log.error("Error publishing track played event", e);
        }
    }

    /**
     * Publishes playlist created event.
     * 
     * @param userId User ID
     * @param playlistId Playlist ID
     * @param hiveId Hive ID (optional for collaborative playlists)
     * @param playlistName Playlist name
     * @param isCollaborative Whether playlist is collaborative
     */
    @Async
    public void publishPlaylistCreated(UUID userId, UUID playlistId, UUID hiveId, 
                                     String playlistName, boolean isCollaborative) {
        log.debug("Publishing playlist created event for playlistId: {}", playlistId);
        
        try {
            // Send WebSocket message to user
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/music/playlist/created",
                Map.of(
                    "playlistId", playlistId,
                    "name", playlistName,
                    "isCollaborative", isCollaborative,
                    "hiveId", hiveId,
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            // Send WebSocket message to hive if collaborative
            if (hiveId != null && isCollaborative) {
                messagingTemplate.convertAndSend(
                    "/topic/hive/" + hiveId + "/music/playlist/created",
                    Map.of(
                        "userId", userId,
                        "playlistId", playlistId,
                        "name", playlistName,
                        "timestamp", System.currentTimeMillis()
                    )
                );
            }
            
            // Send to analytics service
            recordAnalyticsEvent("playlist_created", userId, null, hiveId, Map.of(
                "playlistId", playlistId,
                "playlistName", playlistName,
                "isCollaborative", isCollaborative
            ));
            
        } catch (Exception e) {
            log.error("Error publishing playlist created event", e);
        }
    }

    /**
     * Publishes hive music member joined event.
     * 
     * @param hiveId Hive ID
     * @param userId User ID
     * @param musicPreferences User's music preferences
     */
    @Async
    public void publishHiveMusicMemberJoined(UUID hiveId, UUID userId, Map<String, Object> musicPreferences) {
        log.debug("Publishing hive music member joined event for hiveId: {}, userId: {}", hiveId, userId);
        
        try {
            // Send WebSocket message to hive
            messagingTemplate.convertAndSend(
                "/topic/hive/" + hiveId + "/music/member/joined",
                Map.of(
                    "userId", userId,
                    "musicPreferences", musicPreferences,
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            // Send to analytics service
            recordAnalyticsEvent("hive_music_member_joined", userId, null, hiveId, Map.of(
                "musicPreferences", musicPreferences
            ));
            
        } catch (Exception e) {
            log.error("Error publishing hive music member joined event", e);
        }
    }

    /**
     * Publishes hive music member left event.
     * 
     * @param hiveId Hive ID
     * @param userId User ID
     */
    @Async
    public void publishHiveMusicMemberLeft(UUID hiveId, UUID userId) {
        log.debug("Publishing hive music member left event for hiveId: {}, userId: {}", hiveId, userId);
        
        try {
            // Send WebSocket message to hive
            messagingTemplate.convertAndSend(
                "/topic/hive/" + hiveId + "/music/member/left",
                Map.of(
                    "userId", userId,
                    "timestamp", System.currentTimeMillis()
                )
            );
            
            // Send to analytics service
            recordAnalyticsEvent("hive_music_member_left", userId, null, hiveId, Map.of());
            
        } catch (Exception e) {
            log.error("Error publishing hive music member left event", e);
        }
    }

    /**
     * Records an analytics event asynchronously.
     * 
     * @param eventType Event type
     * @param userId User ID
     * @param sessionId Session ID (optional)
     * @param hiveId Hive ID (optional)
     * @param properties Event properties
     */
    private void recordAnalyticsEvent(String eventType, UUID userId, UUID sessionId, 
                                    UUID hiveId, Map<String, Object> properties) {
        try {
            AnalyticsServiceClient.MusicEventRequest event = new AnalyticsServiceClient.MusicEventRequest(
                userId,
                sessionId,
                hiveId,
                eventType,
                (String) properties.get("trackId"),
                (String) properties.get("artistId"),
                properties.get("playlistId") != null ? properties.get("playlistId").toString() : null,
                System.currentTimeMillis(),
                properties,
                Map.of("source", "music-service")
            );
            
            analyticsServiceClient.recordMusicEvent(event, null);
            
        } catch (Exception e) {
            log.warn("Failed to record analytics event: {}", eventType, e);
        }
    }
}