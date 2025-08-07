package com.focushive.music.service;

import com.focushive.music.client.HiveServiceClient;
import com.focushive.music.exception.MusicServiceException;
import com.focushive.music.model.Playlist;
import com.focushive.music.model.PlaylistCollaborator;
import com.focushive.music.model.PlaylistTrack;
import com.focushive.music.repository.PlaylistRepository;
import com.focushive.music.event.MusicEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing collaborative playlists within hives.
 * 
 * Handles creation, management, and real-time synchronization of
 * collaborative playlists that allow multiple hive members to
 * contribute and vote on tracks.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CollaborativePlaylistService {

    private final PlaylistRepository playlistRepository;
    private final HiveServiceClient hiveServiceClient;
    private final MusicEventPublisher musicEventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Initializes a collaborative playlist for a hive session.
     * 
     * @param hiveId The hive ID
     * @param sessionId The session ID
     * @param userId The user initiating the session
     * @return Created playlist
     */
    public Playlist initializeHiveSessionPlaylist(UUID hiveId, UUID sessionId, UUID userId) {
        log.info("Initializing hive session playlist for hiveId: {}, sessionId: {}, userId: {}", 
            hiveId, sessionId, userId);
        
        try {
            // Check if playlist already exists for this session
            Optional<Playlist> existingPlaylist = playlistRepository.findByHiveIdAndSessionId(hiveId, sessionId);
            if (existingPlaylist.isPresent()) {
                return existingPlaylist.get();
            }
            
            // Verify user is member of hive
            if (!isUserMemberOfHive(hiveId, userId)) {
                throw new MusicServiceException.UnauthorizedOperationException(
                    "User is not a member of the hive");
            }
            
            // Create collaborative playlist
            Playlist playlist = new Playlist();
            playlist.setName("Hive Session - " + LocalDateTime.now().toString());
            playlist.setDescription("Collaborative playlist for hive session");
            playlist.setHiveId(hiveId);
            playlist.setSessionId(sessionId);
            playlist.setCreatedBy(userId);
            playlist.setCollaborative(true);
            playlist.setPublic(false);
            playlist.setCreatedAt(LocalDateTime.now());
            playlist.setUpdatedAt(LocalDateTime.now());
            
            playlist = playlistRepository.save(playlist);
            
            // Add creator as first collaborator
            addCollaborator(playlist.getId(), userId, "owner");
            
            // Notify hive members
            notifyHivePlaylistCreated(hiveId, playlist);
            
            log.info("Created hive session playlist with ID: {}", playlist.getId());
            return playlist;
            
        } catch (Exception e) {
            log.error("Error initializing hive session playlist", e);
            throw new MusicServiceException.CollaborativeFeatureException(
                "playlist initialization", e.getMessage());
        }
    }

    /**
     * Updates hive session playlist based on new preferences.
     * 
     * @param hiveId The hive ID
     * @param sessionId The session ID
     * @param userId The user making updates
     * @param newMusicPreferences Updated music preferences
     */
    public void updateHiveSessionPlaylist(UUID hiveId, UUID sessionId, UUID userId, 
                                        Map<String, Object> newMusicPreferences) {
        log.info("Updating hive session playlist for hiveId: {}, sessionId: {}", hiveId, sessionId);
        
        try {
            Playlist playlist = playlistRepository.findByHiveIdAndSessionId(hiveId, sessionId)
                .orElseThrow(() -> new MusicServiceException.ResourceNotFoundException(
                    "Collaborative playlist", hiveId + ":" + sessionId));
            
            // Update playlist metadata with new preferences
            Map<String, Object> metadata = playlist.getMetadata();
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            metadata.put("lastUpdatedPreferences", newMusicPreferences);
            metadata.put("lastUpdatedBy", userId);
            metadata.put("lastUpdatedAt", System.currentTimeMillis());
            
            playlist.setMetadata(metadata);
            playlist.setUpdatedAt(LocalDateTime.now());
            playlistRepository.save(playlist);
            
            // Notify hive members of update
            notifyHivePlaylistUpdated(hiveId, playlist, userId);
            
        } catch (Exception e) {
            log.error("Error updating hive session playlist", e);
        }
    }

    /**
     * Adds a member to collaborative playlists when they join a hive.
     * 
     * @param hiveId The hive ID
     * @param userId The user joining
     * @param userMusicPreferences User's music preferences
     */
    public void addMemberToHivePlaylists(UUID hiveId, UUID userId, Map<String, Object> userMusicPreferences) {
        log.info("Adding member to hive playlists for hiveId: {}, userId: {}", hiveId, userId);
        
        try {
            // Get active collaborative playlists for the hive
            List<Playlist> activePlaylists = playlistRepository.findActiveCollaborativePlaylistsByHiveId(hiveId);
            
            for (Playlist playlist : activePlaylists) {
                // Add as collaborator if not already added
                if (!hasCollaboratorAccess(playlist.getId(), userId)) {
                    addCollaborator(playlist.getId(), userId, "contributor");
                }
            }
            
            log.info("Added user {} to {} hive playlists", userId, activePlaylists.size());
            
        } catch (Exception e) {
            log.error("Error adding member to hive playlists", e);
        }
    }

    /**
     * Removes a member from collaborative playlists when they leave a hive.
     * 
     * @param hiveId The hive ID
     * @param userId The user leaving
     */
    public void removeMemberFromHivePlaylists(UUID hiveId, UUID userId) {
        log.info("Removing member from hive playlists for hiveId: {}, userId: {}", hiveId, userId);
        
        try {
            // Get collaborative playlists for the hive
            List<Playlist> hivePlaylists = playlistRepository.findByHiveIdAndCollaborativeTrue(hiveId);
            
            for (Playlist playlist : hivePlaylists) {
                // Remove collaborator access
                removeCollaborator(playlist.getId(), userId);
            }
            
            log.info("Removed user {} from {} hive playlists", userId, hivePlaylists.size());
            
        } catch (Exception e) {
            log.error("Error removing member from hive playlists", e);
        }
    }

    /**
     * Adds a track to a collaborative playlist with voting.
     * 
     * @param playlistId The playlist ID
     * @param trackId The track ID
     * @param addedBy The user adding the track
     * @return Added playlist track
     */
    public PlaylistTrack addTrackToCollaborativePlaylist(UUID playlistId, String trackId, UUID addedBy) {
        log.info("Adding track {} to collaborative playlist {} by user {}", trackId, playlistId, addedBy);
        
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new MusicServiceException.ResourceNotFoundException("Playlist", playlistId.toString()));
        
        if (!playlist.isCollaborative()) {
            throw new MusicServiceException.BusinessRuleViolationException(
                "Cannot add track to non-collaborative playlist");
        }
        
        if (!hasCollaboratorAccess(playlistId, addedBy)) {
            throw new MusicServiceException.UnauthorizedOperationException(
                "User does not have access to this playlist");
        }
        
        // Check if track already exists in playlist
        if (playlist.getTracks().stream().anyMatch(track -> track.getTrackId().equals(trackId))) {
            throw new MusicServiceException.BusinessRuleViolationException(
                "Track already exists in playlist");
        }
        
        // Create playlist track with initial vote
        PlaylistTrack playlistTrack = new PlaylistTrack();
        playlistTrack.setPlaylist(playlist);
        playlistTrack.setTrackId(trackId);
        playlistTrack.setAddedBy(addedBy);
        playlistTrack.setPosition(playlist.getTracks().size());
        playlistTrack.setVoteScore(1); // Initial vote from the person who added it
        playlistTrack.setCreatedAt(LocalDateTime.now());
        
        // Initialize voting data
        Map<String, Object> votingData = new HashMap<>();
        votingData.put("upvotes", List.of(addedBy.toString()));
        votingData.put("downvotes", List.of());
        playlistTrack.setVotingData(votingData);
        
        playlist.getTracks().add(playlistTrack);
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);
        
        // Notify hive members
        notifyHiveTrackAdded(playlist.getHiveId(), playlist, playlistTrack, addedBy);
        
        return playlistTrack;
    }

    /**
     * Votes on a track in a collaborative playlist.
     * 
     * @param playlistId The playlist ID
     * @param trackId The track ID
     * @param userId The user voting
     * @param voteType Vote type ("upvote", "downvote", "remove_vote")
     * @return Updated playlist track
     */
    public PlaylistTrack voteOnTrack(UUID playlistId, String trackId, UUID userId, String voteType) {
        log.info("User {} voting {} on track {} in playlist {}", userId, voteType, trackId, playlistId);
        
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new MusicServiceException.ResourceNotFoundException("Playlist", playlistId.toString()));
        
        if (!hasCollaboratorAccess(playlistId, userId)) {
            throw new MusicServiceException.UnauthorizedOperationException(
                "User does not have access to this playlist");
        }
        
        PlaylistTrack track = playlist.getTracks().stream()
            .filter(t -> t.getTrackId().equals(trackId))
            .findFirst()
            .orElseThrow(() -> new MusicServiceException.ResourceNotFoundException("Track", trackId));
        
        // Update voting data
        Map<String, Object> votingData = track.getVotingData();
        if (votingData == null) {
            votingData = new HashMap<>();
            votingData.put("upvotes", new ArrayList<String>());
            votingData.put("downvotes", new ArrayList<String>());
        }
        
        @SuppressWarnings("unchecked")
        List<String> upvotes = (List<String>) votingData.get("upvotes");
        @SuppressWarnings("unchecked")
        List<String> downvotes = (List<String>) votingData.get("downvotes");
        
        String userIdString = userId.toString();
        
        // Remove previous votes
        upvotes.remove(userIdString);
        downvotes.remove(userIdString);
        
        // Add new vote
        switch (voteType.toLowerCase()) {
            case "upvote" -> upvotes.add(userIdString);
            case "downvote" -> downvotes.add(userIdString);
            // "remove_vote" just removes previous votes, no new vote added
        }
        
        // Update vote score
        track.setVoteScore(upvotes.size() - downvotes.size());
        track.setVotingData(votingData);
        
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);
        
        // Notify hive members
        notifyHiveTrackVoted(playlist.getHiveId(), playlist, track, userId, voteType);
        
        return track;
    }

    /**
     * Gets collaborative playlists for a hive.
     * 
     * @param hiveId The hive ID
     * @return List of collaborative playlists
     */
    @Transactional(readOnly = true)
    public List<Playlist> getHiveCollaborativePlaylists(UUID hiveId) {
        return playlistRepository.findByHiveIdAndCollaborativeTrue(hiveId);
    }

    /**
     * Adds a collaborator to a playlist.
     */
    private void addCollaborator(UUID playlistId, UUID userId, String role) {
        Playlist playlist = playlistRepository.findById(playlistId).orElse(null);
        if (playlist != null) {
            PlaylistCollaborator collaborator = new PlaylistCollaborator();
            collaborator.setPlaylist(playlist);
            collaborator.setUserId(userId);
            collaborator.setRole(role);
            collaborator.setAddedAt(LocalDateTime.now());
            
            playlist.getCollaborators().add(collaborator);
            playlistRepository.save(playlist);
        }
    }

    /**
     * Removes a collaborator from a playlist.
     */
    private void removeCollaborator(UUID playlistId, UUID userId) {
        Playlist playlist = playlistRepository.findById(playlistId).orElse(null);
        if (playlist != null) {
            playlist.getCollaborators().removeIf(c -> c.getUserId().equals(userId));
            playlistRepository.save(playlist);
        }
    }

    /**
     * Checks if user has collaborator access to a playlist.
     */
    private boolean hasCollaboratorAccess(UUID playlistId, UUID userId) {
        return playlistRepository.findById(playlistId)
            .map(playlist -> playlist.getCollaborators().stream()
                .anyMatch(c -> c.getUserId().equals(userId)))
            .orElse(false);
    }

    /**
     * Checks if user is member of hive.
     */
    private boolean isUserMemberOfHive(UUID hiveId, UUID userId) {
        try {
            var response = hiveServiceClient.getMembershipStatus(hiveId, userId, null);
            return response.getStatusCode().is2xxSuccessful() && 
                   response.getBody() != null && 
                   response.getBody().isMember();
        } catch (Exception e) {
            log.warn("Error checking hive membership for userId: {}, hiveId: {}", userId, hiveId, e);
            return false;
        }
    }

    /**
     * Notifies hive members of playlist creation.
     */
    private void notifyHivePlaylistCreated(UUID hiveId, Playlist playlist) {
        messagingTemplate.convertAndSend(
            "/topic/hive/" + hiveId + "/music/playlist/created",
            Map.of(
                "playlistId", playlist.getId(),
                "playlistName", playlist.getName(),
                "createdBy", playlist.getCreatedBy(),
                "isCollaborative", playlist.isCollaborative(),
                "timestamp", System.currentTimeMillis()
            )
        );
    }

    /**
     * Notifies hive members of playlist update.
     */
    private void notifyHivePlaylistUpdated(UUID hiveId, Playlist playlist, UUID updatedBy) {
        messagingTemplate.convertAndSend(
            "/topic/hive/" + hiveId + "/music/playlist/updated",
            Map.of(
                "playlistId", playlist.getId(),
                "updatedBy", updatedBy,
                "timestamp", System.currentTimeMillis()
            )
        );
    }

    /**
     * Notifies hive members of track addition.
     */
    private void notifyHiveTrackAdded(UUID hiveId, Playlist playlist, PlaylistTrack track, UUID addedBy) {
        messagingTemplate.convertAndSend(
            "/topic/hive/" + hiveId + "/music/track/added",
            Map.of(
                "playlistId", playlist.getId(),
                "trackId", track.getTrackId(),
                "addedBy", addedBy,
                "position", track.getPosition(),
                "voteScore", track.getVoteScore(),
                "timestamp", System.currentTimeMillis()
            )
        );
    }

    /**
     * Notifies hive members of track voting.
     */
    private void notifyHiveTrackVoted(UUID hiveId, Playlist playlist, PlaylistTrack track, 
                                    UUID votedBy, String voteType) {
        messagingTemplate.convertAndSend(
            "/topic/hive/" + hiveId + "/music/track/voted",
            Map.of(
                "playlistId", playlist.getId(),
                "trackId", track.getTrackId(),
                "votedBy", votedBy,
                "voteType", voteType,
                "newVoteScore", track.getVoteScore(),
                "timestamp", System.currentTimeMillis()
            )
        );
    }
}