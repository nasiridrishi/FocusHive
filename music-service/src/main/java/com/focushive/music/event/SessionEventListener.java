package com.focushive.music.event;

import com.focushive.music.client.SessionServiceClient;
import com.focushive.music.service.MusicRecommendationService;
import com.focushive.music.service.CollaborativePlaylistService;
import com.focushive.music.event.MusicEvents.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Event listener for session-related events from other services.
 * 
 * Handles session start, end, and update events to trigger music-related
 * actions such as playlist recommendations, collaborative playlist updates,
 * and music analytics tracking.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventListener {

    private final MusicRecommendationService musicRecommendationService;
    private final CollaborativePlaylistService collaborativePlaylistService;
    private final MusicEventPublisher musicEventPublisher;

    /**
     * Handles session start events.
     * 
     * @param event Session start event
     */
    @Async
    @EventListener
    public void handleSessionStartEvent(SessionStartEvent event) {
        log.info("Handling session start event for sessionId: {}, userId: {}, hiveId: {}", 
            event.sessionId(), event.userId(), event.hiveId());
        
        try {
            // Generate music recommendations based on session type and user preferences
            if (event.musicEnabled()) {
                musicRecommendationService.generateSessionRecommendations(
                    event.userId(), 
                    event.sessionId(),
                    event.sessionType(),
                    event.preferences()
                );
                
                // Initialize collaborative playlist for hive session
                if (event.hiveId() != null) {
                    collaborativePlaylistService.initializeHiveSessionPlaylist(
                        event.hiveId(), 
                        event.sessionId(),
                        event.userId()
                    );
                }
            }
            
            // Publish music session started event
            musicEventPublisher.publishMusicSessionStarted(
                event.userId(),
                event.sessionId(),
                event.hiveId(),
                event.musicEnabled()
            );
            
        } catch (Exception e) {
            log.error("Error handling session start event for sessionId: {}", event.sessionId(), e);
        }
    }

    /**
     * Handles session end events.
     * 
     * @param event Session end event
     */
    @Async
    @EventListener
    public void handleSessionEndEvent(SessionEndEvent event) {
        log.info("Handling session end event for sessionId: {}, userId: {}", 
            event.sessionId(), event.userId());
        
        try {
            // Finalize music session data
            if (event.musicWasEnabled()) {
                musicRecommendationService.finalizeSessionMusic(
                    event.sessionId(),
                    event.userId(),
                    event.duration(),
                    event.metrics()
                );
            }
            
            // Publish music session ended event
            musicEventPublisher.publishMusicSessionEnded(
                event.userId(),
                event.sessionId(),
                event.hiveId(),
                event.duration(),
                event.metrics()
            );
            
        } catch (Exception e) {
            log.error("Error handling session end event for sessionId: {}", event.sessionId(), e);
        }
    }

    /**
     * Handles session update events.
     * 
     * @param event Session update event
     */
    @Async
    @EventListener
    public void handleSessionUpdateEvent(SessionUpdateEvent event) {
        log.info("Handling session update event for sessionId: {}, userId: {}", 
            event.sessionId(), event.userId());
        
        try {
            // Update music recommendations if preferences changed
            if (event.musicPreferencesChanged()) {
                musicRecommendationService.updateSessionRecommendations(
                    event.sessionId(),
                    event.userId(),
                    event.newMusicPreferences()
                );
            }
            
            // Update collaborative playlist if hive session
            if (event.hiveId() != null && event.musicEnabled()) {
                collaborativePlaylistService.updateHiveSessionPlaylist(
                    event.hiveId(),
                    event.sessionId(),
                    event.userId(),
                    event.newMusicPreferences()
                );
            }
            
        } catch (Exception e) {
            log.error("Error handling session update event for sessionId: {}", event.sessionId(), e);
        }
    }

    /**
     * Handles hive member join events.
     * 
     * @param event Member join event
     */
    @Async
    @EventListener
    public void handleHiveMemberJoinEvent(HiveMemberJoinEvent event) {
        log.info("Handling hive member join event for userId: {}, hiveId: {}", 
            event.userId(), event.hiveId());
        
        try {
            // Add member to active collaborative playlists
            collaborativePlaylistService.addMemberToHivePlaylists(
                event.hiveId(),
                event.userId(),
                event.userMusicPreferences()
            );
            
            // Publish hive music member joined event
            musicEventPublisher.publishHiveMusicMemberJoined(
                event.hiveId(),
                event.userId(),
                event.userMusicPreferences()
            );
            
        } catch (Exception e) {
            log.error("Error handling hive member join event for userId: {}, hiveId: {}", 
                event.userId(), event.hiveId(), e);
        }
    }

    /**
     * Handles hive member leave events.
     * 
     * @param event Member leave event
     */
    @Async
    @EventListener
    public void handleHiveMemberLeaveEvent(HiveMemberLeaveEvent event) {
        log.info("Handling hive member leave event for userId: {}, hiveId: {}", 
            event.userId(), event.hiveId());
        
        try {
            // Remove member from collaborative playlists
            collaborativePlaylistService.removeMemberFromHivePlaylists(
                event.hiveId(),
                event.userId()
            );
            
            // Publish hive music member left event
            musicEventPublisher.publishHiveMusicMemberLeft(
                event.hiveId(),
                event.userId()
            );
            
        } catch (Exception e) {
            log.error("Error handling hive member leave event for userId: {}, hiveId: {}", 
                event.userId(), event.hiveId(), e);
        }
    }
}