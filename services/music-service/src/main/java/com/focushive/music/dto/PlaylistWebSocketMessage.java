package com.focushive.music.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * DTO for WebSocket messages related to collaborative playlist operations.
 * Handles various playlist actions like track addition, removal, reordering,
 * and participant management.
 */
public class PlaylistWebSocketMessage {

    @JsonProperty("action")
    private String action;

    @JsonProperty("playlistId")
    private UUID playlistId;

    @JsonProperty("spotifyTrackId")
    private String spotifyTrackId;

    @JsonProperty("trackId")
    private UUID trackId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("newPosition")
    private Integer newPosition;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private Long timestamp;

    // Constructors
    public PlaylistWebSocketMessage() {}

    public PlaylistWebSocketMessage(String action, UUID playlistId, String userId) {
        this.action = action;
        this.playlistId = playlistId;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public UUID getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(UUID playlistId) {
        this.playlistId = playlistId;
    }

    public String getSpotifyTrackId() {
        return spotifyTrackId;
    }

    public void setSpotifyTrackId(String spotifyTrackId) {
        this.spotifyTrackId = spotifyTrackId;
    }

    public UUID getTrackId() {
        return trackId;
    }

    public void setTrackId(UUID trackId) {
        this.trackId = trackId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getNewPosition() {
        return newPosition;
    }

    public void setNewPosition(Integer newPosition) {
        this.newPosition = newPosition;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "PlaylistWebSocketMessage{" +
                "action='" + action + '\'' +
                ", playlistId=" + playlistId +
                ", spotifyTrackId='" + spotifyTrackId + '\'' +
                ", trackId=" + trackId +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", newPosition=" + newPosition +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}