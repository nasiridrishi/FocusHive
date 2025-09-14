package com.focushive.music.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.music.dto.PlaylistWebSocketMessage;
import com.focushive.music.entity.Playlist;
import com.focushive.music.entity.PlaylistTrack;
import com.focushive.music.service.PlaylistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket controller for handling real-time collaborative playlist operations.
 * Manages WebSocket messages for playlist track operations and participant notifications.
 */
@Controller
public class PlaylistWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(PlaylistWebSocketController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private PlaylistService playlistService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Handle track addition requests via WebSocket
     */
    @MessageMapping("/playlist/track/add")
    public void handleAddTrack(@Payload String message) {
        try {
            PlaylistWebSocketMessage wsMessage = objectMapper.readValue(message, PlaylistWebSocketMessage.class);
            logger.info("Received ADD_TRACK message for playlist: {}", wsMessage.getPlaylistId());

            // Validate playlist permissions
            Playlist playlist = playlistService.getPlaylist(wsMessage.getPlaylistId());
            if (!hasPermissionToModify(playlist, wsMessage.getUserId())) {
                sendPermissionDeniedMessage(wsMessage.getPlaylistId(), wsMessage.getUserId());
                return;
            }

            // Add track to playlist
            PlaylistTrack addedTrack = playlistService.addTrack(
                wsMessage.getPlaylistId(),
                wsMessage.getSpotifyTrackId()
            );

            // Broadcast track addition to all subscribers
            Map<String, Object> notification = new HashMap<>();
            notification.put("action", "TRACK_ADDED");
            notification.put("playlistId", wsMessage.getPlaylistId());
            notification.put("track", addedTrack);
            notification.put("userId", wsMessage.getUserId());
            notification.put("timestamp", System.currentTimeMillis());

            String topic = "/topic/playlist/" + wsMessage.getPlaylistId();
            messagingTemplate.convertAndSend(topic, objectMapper.writeValueAsString(notification));

        } catch (Exception e) {
            logger.error("Error handling ADD_TRACK message", e);
            sendErrorMessage("Failed to add track");
        }
    }

    /**
     * Handle track removal requests via WebSocket
     */
    @MessageMapping("/playlist/track/remove")
    public void handleRemoveTrack(@Payload String message) {
        try {
            PlaylistWebSocketMessage wsMessage = objectMapper.readValue(message, PlaylistWebSocketMessage.class);
            logger.info("Received REMOVE_TRACK message for playlist: {}", wsMessage.getPlaylistId());

            // Validate playlist permissions
            Playlist playlist = playlistService.getPlaylist(wsMessage.getPlaylistId());
            if (!hasPermissionToModify(playlist, wsMessage.getUserId())) {
                sendPermissionDeniedMessage(wsMessage.getPlaylistId(), wsMessage.getUserId());
                return;
            }

            // Remove track from playlist
            playlistService.removeTrack(wsMessage.getTrackId());

            // Broadcast track removal to all subscribers
            Map<String, Object> notification = new HashMap<>();
            notification.put("action", "TRACK_REMOVED");
            notification.put("playlistId", wsMessage.getPlaylistId());
            notification.put("trackId", wsMessage.getTrackId());
            notification.put("userId", wsMessage.getUserId());
            notification.put("timestamp", System.currentTimeMillis());

            String topic = "/topic/playlist/" + wsMessage.getPlaylistId();
            messagingTemplate.convertAndSend(topic, objectMapper.writeValueAsString(notification));

        } catch (Exception e) {
            logger.error("Error handling REMOVE_TRACK message", e);
            sendErrorMessage("Failed to remove track");
        }
    }

    /**
     * Handle track reordering requests via WebSocket
     */
    @MessageMapping("/playlist/track/reorder")
    public void handleReorderTrack(@Payload String message) {
        try {
            PlaylistWebSocketMessage wsMessage = objectMapper.readValue(message, PlaylistWebSocketMessage.class);
            logger.info("Received REORDER_TRACK message for playlist: {}", wsMessage.getPlaylistId());

            // Validate playlist permissions
            Playlist playlist = playlistService.getPlaylist(wsMessage.getPlaylistId());
            if (!hasPermissionToModify(playlist, wsMessage.getUserId())) {
                sendPermissionDeniedMessage(wsMessage.getPlaylistId(), wsMessage.getUserId());
                return;
            }

            // Reorder track in playlist
            playlistService.reorderTrack(wsMessage.getTrackId(), wsMessage.getNewPosition());

            // Broadcast track reordering to all subscribers
            Map<String, Object> notification = new HashMap<>();
            notification.put("action", "TRACK_REORDERED");
            notification.put("playlistId", wsMessage.getPlaylistId());
            notification.put("trackId", wsMessage.getTrackId());
            notification.put("newPosition", wsMessage.getNewPosition());
            notification.put("userId", wsMessage.getUserId());
            notification.put("timestamp", System.currentTimeMillis());

            String topic = "/topic/playlist/" + wsMessage.getPlaylistId();
            messagingTemplate.convertAndSend(topic, objectMapper.writeValueAsString(notification));

        } catch (Exception e) {
            logger.error("Error handling REORDER_TRACK message", e);
            sendErrorMessage("Failed to reorder track");
        }
    }

    /**
     * Handle participant join notifications
     */
    @MessageMapping("/playlist/join")
    public void handleJoinPlaylist(@Payload String message) {
        try {
            PlaylistWebSocketMessage wsMessage = objectMapper.readValue(message, PlaylistWebSocketMessage.class);
            logger.info("User {} joining playlist: {}", wsMessage.getUserId(), wsMessage.getPlaylistId());

            // Broadcast join notification to all subscribers
            Map<String, Object> notification = new HashMap<>();
            notification.put("action", "USER_JOINED");
            notification.put("playlistId", wsMessage.getPlaylistId());
            notification.put("userId", wsMessage.getUserId());
            notification.put("userName", wsMessage.getUserName());
            notification.put("timestamp", System.currentTimeMillis());

            String topic = "/topic/playlist/" + wsMessage.getPlaylistId();
            messagingTemplate.convertAndSend(topic, objectMapper.writeValueAsString(notification));

        } catch (Exception e) {
            logger.error("Error handling JOIN_PLAYLIST message", e);
            sendErrorMessage("Failed to join playlist");
        }
    }

    /**
     * Handle participant leave notifications
     */
    @MessageMapping("/playlist/leave")
    public void handleLeavePlaylist(@Payload String message) {
        try {
            PlaylistWebSocketMessage wsMessage = objectMapper.readValue(message, PlaylistWebSocketMessage.class);
            logger.info("User {} leaving playlist: {}", wsMessage.getUserId(), wsMessage.getPlaylistId());

            // Broadcast leave notification to all subscribers
            Map<String, Object> notification = new HashMap<>();
            notification.put("action", "USER_LEFT");
            notification.put("playlistId", wsMessage.getPlaylistId());
            notification.put("userId", wsMessage.getUserId());
            notification.put("userName", wsMessage.getUserName());
            notification.put("timestamp", System.currentTimeMillis());

            String topic = "/topic/playlist/" + wsMessage.getPlaylistId();
            messagingTemplate.convertAndSend(topic, objectMapper.writeValueAsString(notification));

        } catch (Exception e) {
            logger.error("Error handling LEAVE_PLAYLIST message", e);
            sendErrorMessage("Failed to leave playlist");
        }
    }

    /**
     * Check if user has permission to modify the playlist
     */
    private boolean hasPermissionToModify(Playlist playlist, String userId) {
        if (playlist == null || userId == null) {
            return false;
        }

        // Owner can always modify
        if (playlist.getUserId().equals(userId)) {
            return true;
        }

        // For collaborative playlists, check if playlist is public or user is authorized
        return playlist.getType() == Playlist.PlaylistType.COLLABORATIVE && Boolean.TRUE.equals(playlist.getIsPublic());
    }

    /**
     * Send permission denied message
     */
    private void sendPermissionDeniedMessage(UUID playlistId, String userId) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("action", "PERMISSION_DENIED");
            notification.put("playlistId", playlistId);
            notification.put("userId", userId);
            notification.put("message", "You don't have permission to modify this playlist");
            notification.put("timestamp", System.currentTimeMillis());

            String topic = "/topic/playlist/" + playlistId;
            messagingTemplate.convertAndSend(topic, objectMapper.writeValueAsString(notification));
        } catch (Exception e) {
            logger.error("Failed to send permission denied message", e);
        }
    }

    /**
     * Send error message
     */
    private void sendErrorMessage(String errorMessage) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("action", "ERROR");
            notification.put("message", errorMessage);
            notification.put("timestamp", System.currentTimeMillis());

            // Send to general error topic
            messagingTemplate.convertAndSend("/topic/errors", objectMapper.writeValueAsString(notification));
        } catch (Exception e) {
            logger.error("Failed to send error message", e);
        }
    }
}