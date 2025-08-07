package com.focushive.music.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Entity representing an active music listening session.
 * 
 * Tracks the current state of a user's music playback, including:
 * - Current playlist and track position
 * - Playback settings (volume, shuffle, repeat)
 * - Session timing and analytics
 * - Collaborative/synchronized session information
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "music_sessions", schema = "music", indexes = {
    @Index(name = "idx_music_sessions_user_id", columnList = "userId"),
    @Index(name = "idx_music_sessions_hive_id", columnList = "hiveId"),
    @Index(name = "idx_music_sessions_active", columnList = "sessionEndTime")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MusicSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User ID of the session owner.
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * Hive ID if this is a collaborative session, null for personal sessions.
     */
    @Column(name = "hive_id")
    private String hiveId;

    /**
     * Type of music session.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    private SessionType sessionType;

    /**
     * The playlist currently being played in this session.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_playlist_id")
    private Playlist currentPlaylist;

    /**
     * Current position/index in the playlist (0-based).
     */
    @Column(name = "current_track_position")
    @Builder.Default
    private Integer currentTrackPosition = 0;

    /**
     * Whether music is currently playing or paused.
     */
    @Column(name = "is_playing")
    @Builder.Default
    private Boolean isPlaying = false;

    /**
     * Current volume level (0-100).
     */
    @Column(name = "volume")
    @Builder.Default
    private Integer volume = 50;

    /**
     * Whether shuffle mode is enabled.
     */
    @Column(name = "shuffle_enabled")
    @Builder.Default
    private Boolean shuffleEnabled = false;

    /**
     * Repeat mode setting.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_mode")
    @Builder.Default
    private RepeatMode repeatMode = RepeatMode.OFF;

    /**
     * Timestamp when the session was started.
     */
    @CreatedDate
    @Column(name = "session_start_time", nullable = false, updatable = false)
    private LocalDateTime sessionStartTime;

    /**
     * Timestamp of the last activity in this session.
     */
    @Column(name = "last_activity_time")
    private LocalDateTime lastActivityTime;

    /**
     * Timestamp when the session ended (null for active sessions).
     */
    @Column(name = "session_end_time")
    private LocalDateTime sessionEndTime;

    /**
     * Total listening time in this session (milliseconds).
     */
    @Column(name = "total_listening_time_ms")
    @Builder.Default
    private Long totalListeningTimeMs = 0L;

    /**
     * Enumeration of session types.
     */
    public enum SessionType {
        /**
         * Individual user session.
         */
        PERSONAL,
        
        /**
         * Multiple users can contribute but each controls their own playback.
         */
        COLLABORATIVE,
        
        /**
         * All participants hear the same track at the same time.
         */
        SYNCHRONIZED
    }

    /**
     * Enumeration of repeat modes.
     */
    public enum RepeatMode {
        /**
         * No repeat.
         */
        OFF,
        
        /**
         * Repeat current track.
         */
        TRACK,
        
        /**
         * Repeat entire playlist.
         */
        PLAYLIST
    }

    /**
     * Checks if this session is currently active.
     * 
     * @return true if the session has not ended
     */
    public boolean isActive() {
        return sessionEndTime == null;
    }

    /**
     * Checks if this is a personal (non-collaborative) session.
     * 
     * @return true if this is a personal session
     */
    public boolean isPersonal() {
        return sessionType == SessionType.PERSONAL;
    }

    /**
     * Checks if this is a collaborative or synchronized session.
     * 
     * @return true if this is a group session
     */
    public boolean isGroupSession() {
        return sessionType == SessionType.COLLABORATIVE || 
               sessionType == SessionType.SYNCHRONIZED;
    }

    /**
     * Updates the last activity time to now.
     */
    public void updateActivity() {
        this.lastActivityTime = LocalDateTime.now();
    }

    /**
     * Ends the session and calculates final listening time.
     */
    public void endSession() {
        this.sessionEndTime = LocalDateTime.now();
        this.isPlaying = false;
        updateActivity();
        
        // Calculate total session duration if not already tracked
        if (totalListeningTimeMs == 0 && sessionStartTime != null) {
            Duration sessionDuration = Duration.between(sessionStartTime, sessionEndTime);
            this.totalListeningTimeMs = sessionDuration.toMillis();
        }
    }

    /**
     * Advances to the next track in the playlist.
     */
    public void nextTrack() {
        if (currentPlaylist != null && currentTrackPosition != null) {
            int playlistSize = currentPlaylist.getTotalTracks();
            
            if (repeatMode == RepeatMode.PLAYLIST && currentTrackPosition >= playlistSize - 1) {
                // Loop back to start if repeat playlist is enabled
                currentTrackPosition = 0;
            } else if (currentTrackPosition < playlistSize - 1) {
                currentTrackPosition++;
            }
        }
        updateActivity();
    }

    /**
     * Goes back to the previous track in the playlist.
     */
    public void previousTrack() {
        if (currentTrackPosition != null && currentTrackPosition > 0) {
            currentTrackPosition--;
        }
        updateActivity();
    }

    /**
     * Toggles play/pause state.
     */
    public void togglePlayback() {
        this.isPlaying = !this.isPlaying;
        updateActivity();
    }

    /**
     * Sets the volume level with validation.
     * 
     * @param volume Volume level (0-100)
     */
    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
        updateActivity();
    }

    /**
     * Gets the session duration in a human-readable format.
     * 
     * @return Duration as a string (e.g., "2h 30m")
     */
    public String getFormattedDuration() {
        LocalDateTime endTime = sessionEndTime != null ? sessionEndTime : LocalDateTime.now();
        Duration duration = Duration.between(sessionStartTime, endTime);
        
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
}