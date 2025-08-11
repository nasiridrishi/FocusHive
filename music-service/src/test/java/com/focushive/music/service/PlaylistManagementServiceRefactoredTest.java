package com.focushive.music.service;

import com.focushive.music.dto.PlaylistDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class to verify that the refactored PlaylistManagementService
 * correctly delegates to the specialized service classes.
 * 
 * This test ensures backward compatibility after the Single Responsibility
 * Principle refactoring using the Facade pattern.
 */
@ExtendWith(MockitoExtension.class)
class PlaylistManagementServiceRefactoredTest {

    @Mock
    private PlaylistCrudService playlistCrudService;

    @Mock
    private PlaylistTrackService playlistTrackService;

    @Mock
    private SmartPlaylistService smartPlaylistService;

    @Mock
    private PlaylistSharingService playlistSharingService;

    @InjectMocks
    private PlaylistManagementService playlistManagementService;

    private String userId;
    private Long playlistId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        userId = "test-user-123";
        playlistId = 1L;
        pageable = PageRequest.of(0, 10);
    }

    // ===============================
    // CRUD OPERATIONS DELEGATION TESTS
    // ===============================

    @Test
    void createPlaylist_DelegatesToCrudService() {
        // Arrange
        PlaylistDTO.CreateRequest request = PlaylistDTO.CreateRequest.builder()
                .name("Test Playlist")
                .description("Test Description")
                .build();
        
        PlaylistDTO.Response expectedResponse = PlaylistDTO.Response.builder()
                .id(playlistId)
                .name("Test Playlist")
                .build();
        
        when(playlistCrudService.createPlaylist(request, userId)).thenReturn(expectedResponse);

        // Act
        PlaylistDTO.Response result = playlistManagementService.createPlaylist(request, userId);

        // Assert
        assertNotNull(result);
        verify(playlistCrudService, times(1)).createPlaylist(request, userId);
    }

    @Test
    void getPlaylistById_DelegatesToCrudService() {
        // Arrange
        PlaylistDTO.Response expectedResponse = PlaylistDTO.Response.builder()
                .id(playlistId)
                .name("Test Playlist")
                .build();
        
        when(playlistCrudService.getPlaylistById(playlistId, userId)).thenReturn(expectedResponse);

        // Act
        PlaylistDTO.Response result = playlistManagementService.getPlaylistById(playlistId, userId);

        // Assert
        assertNotNull(result);
        verify(playlistCrudService, times(1)).getPlaylistById(playlistId, userId);
    }

    @Test
    void updatePlaylist_DelegatesToCrudService() {
        // Arrange
        PlaylistDTO.UpdateRequest request = PlaylistDTO.UpdateRequest.builder()
                .name("Updated Playlist")
                .build();
        
        PlaylistDTO.Response expectedResponse = PlaylistDTO.Response.builder()
                .id(playlistId)
                .name("Updated Playlist")
                .build();
        
        when(playlistCrudService.updatePlaylist(playlistId, request, userId)).thenReturn(expectedResponse);

        // Act
        PlaylistDTO.Response result = playlistManagementService.updatePlaylist(playlistId, request, userId);

        // Assert
        assertNotNull(result);
        verify(playlistCrudService, times(1)).updatePlaylist(playlistId, request, userId);
    }

    @Test
    void deletePlaylist_DelegatesToCrudService() {
        // Act
        playlistManagementService.deletePlaylist(playlistId, userId);

        // Assert
        verify(playlistCrudService, times(1)).deletePlaylist(playlistId, userId);
    }

    @Test
    void getUserPlaylists_DelegatesToCrudService() {
        // Arrange
        PlaylistDTO.SearchResult expectedResult = PlaylistDTO.SearchResult.builder()
                .totalResults(1L)
                .build();
        
        when(playlistCrudService.getUserPlaylists(userId, pageable)).thenReturn(expectedResult);

        // Act
        PlaylistDTO.SearchResult result = playlistManagementService.getUserPlaylists(userId, pageable);

        // Assert
        assertNotNull(result);
        verify(playlistCrudService, times(1)).getUserPlaylists(userId, pageable);
    }

    // ===============================
    // TRACK MANAGEMENT DELEGATION TESTS
    // ===============================

    @Test
    void addTrackToPlaylist_DelegatesToTrackService() {
        // Arrange
        PlaylistDTO.AddTrackRequest request = PlaylistDTO.AddTrackRequest.builder()
                .spotifyTrackId("test-track-123")
                .build();
        
        PlaylistDTO.TrackInfo expectedTrack = PlaylistDTO.TrackInfo.builder()
                .id(1L)
                .spotifyTrackId("test-track-123")
                .build();
        
        when(playlistTrackService.addTrackToPlaylist(playlistId, request, userId)).thenReturn(expectedTrack);

        // Act
        PlaylistDTO.TrackInfo result = playlistManagementService.addTrackToPlaylist(playlistId, request, userId);

        // Assert
        assertNotNull(result);
        verify(playlistTrackService, times(1)).addTrackToPlaylist(playlistId, request, userId);
    }

    @Test
    void removeTrackFromPlaylist_DelegatesToTrackService() {
        // Arrange
        Long trackId = 1L;

        // Act
        playlistManagementService.removeTrackFromPlaylist(playlistId, trackId, userId);

        // Assert
        verify(playlistTrackService, times(1)).removeTrackFromPlaylist(playlistId, trackId, userId);
    }

    @Test
    void reorderPlaylistTracks_DelegatesToTrackService() {
        // Arrange
        PlaylistDTO.TrackReorderRequest request = PlaylistDTO.TrackReorderRequest.builder()
                .build();

        // Act
        playlistManagementService.reorderPlaylistTracks(playlistId, request, userId);

        // Assert
        verify(playlistTrackService, times(1)).reorderPlaylistTracks(playlistId, request, userId);
    }

    // ===============================
    // SMART PLAYLIST DELEGATION TESTS
    // ===============================

    @Test
    void createSmartPlaylist_DelegatesToSmartService() {
        // Arrange
        PlaylistDTO.SmartPlaylistCriteriaRequest request = PlaylistDTO.SmartPlaylistCriteriaRequest.builder()
                .name("Smart Playlist")
                .build();
        
        PlaylistDTO.Response expectedResponse = PlaylistDTO.Response.builder()
                .id(playlistId)
                .name("Smart Playlist")
                .build();
        
        when(smartPlaylistService.createSmartPlaylist(request, userId)).thenReturn(expectedResponse);

        // Act
        PlaylistDTO.Response result = playlistManagementService.createSmartPlaylist(request, userId);

        // Assert
        assertNotNull(result);
        verify(smartPlaylistService, times(1)).createSmartPlaylist(request, userId);
    }

    @Test
    void refreshSmartPlaylist_DelegatesToSmartService() {
        // Arrange
        PlaylistDTO.Response expectedResponse = PlaylistDTO.Response.builder()
                .id(playlistId)
                .name("Refreshed Smart Playlist")
                .build();
        
        when(smartPlaylistService.refreshSmartPlaylist(playlistId, userId)).thenReturn(expectedResponse);

        // Act
        PlaylistDTO.Response result = playlistManagementService.refreshSmartPlaylist(playlistId, userId);

        // Assert
        assertNotNull(result);
        verify(smartPlaylistService, times(1)).refreshSmartPlaylist(playlistId, userId);
    }

    // ===============================
    // SHARING DELEGATION TESTS
    // ===============================

    @Test
    void sharePlaylistWithHive_DelegatesToSharingService() {
        // Arrange
        PlaylistDTO.SharePlaylistRequest request = PlaylistDTO.SharePlaylistRequest.builder()
                .hiveId("test-hive")
                .permissionLevel("VIEWER")
                .build();
        
        PlaylistDTO.SharingResponse expectedResponse = PlaylistDTO.SharingResponse.builder()
                .playlistId(playlistId)
                .hiveId("test-hive")
                .build();
        
        when(playlistSharingService.sharePlaylistWithHive(playlistId, request, userId)).thenReturn(expectedResponse);

        // Act
        PlaylistDTO.SharingResponse result = playlistManagementService.sharePlaylistWithHive(playlistId, request, userId);

        // Assert
        assertNotNull(result);
        verify(playlistSharingService, times(1)).sharePlaylistWithHive(playlistId, request, userId);
    }

    @Test
    void addCollaborator_DelegatesToSharingService() {
        // Arrange
        PlaylistDTO.AddCollaboratorRequest request = PlaylistDTO.AddCollaboratorRequest.builder()
                .userId("collaborator-123")
                .permissionLevel("EDITOR")
                .build();
        
        PlaylistDTO.CollaboratorInfo expectedInfo = PlaylistDTO.CollaboratorInfo.builder()
                .userId("collaborator-123")
                .permissionLevel("EDITOR")
                .build();
        
        when(playlistSharingService.addCollaborator(playlistId, request, userId)).thenReturn(expectedInfo);

        // Act
        PlaylistDTO.CollaboratorInfo result = playlistManagementService.addCollaborator(playlistId, request, userId);

        // Assert
        assertNotNull(result);
        verify(playlistSharingService, times(1)).addCollaborator(playlistId, request, userId);
    }

    @Test
    void getHivePlaylists_DelegatesToSharingService() {
        // Arrange
        String hiveId = "test-hive";
        PlaylistDTO.SearchResult expectedResult = PlaylistDTO.SearchResult.builder()
                .totalResults(1L)
                .build();
        
        when(playlistSharingService.getHivePlaylists(hiveId, userId, pageable)).thenReturn(expectedResult);

        // Act
        PlaylistDTO.SearchResult result = playlistManagementService.getHivePlaylists(hiveId, userId, pageable);

        // Assert
        assertNotNull(result);
        verify(playlistSharingService, times(1)).getHivePlaylists(hiveId, userId, pageable);
    }

    @Test
    void exportPlaylist_DelegatesToSharingService() {
        // Arrange
        PlaylistDTO.ExportRequest request = PlaylistDTO.ExportRequest.builder()
                .format("JSON")
                .build();
        
        PlaylistDTO.ExportResponse expectedResponse = PlaylistDTO.ExportResponse.builder()
                .playlistId(playlistId)
                .format("JSON")
                .build();
        
        when(playlistSharingService.exportPlaylist(playlistId, request, userId)).thenReturn(expectedResponse);

        // Act
        PlaylistDTO.ExportResponse result = playlistManagementService.exportPlaylist(playlistId, request, userId);

        // Assert
        assertNotNull(result);
        verify(playlistSharingService, times(1)).exportPlaylist(playlistId, request, userId);
    }

    @Test
    void importPlaylist_DelegatesToSharingService() {
        // Arrange
        PlaylistDTO.ImportRequest request = PlaylistDTO.ImportRequest.builder()
                .source("SPOTIFY")
                .externalPlaylistId("external-123")
                .build();
        
        PlaylistDTO.Response expectedResponse = PlaylistDTO.Response.builder()
                .id(playlistId)
                .name("Imported Playlist")
                .build();
        
        when(playlistSharingService.importPlaylist(request, userId)).thenReturn(expectedResponse);

        // Act
        PlaylistDTO.Response result = playlistManagementService.importPlaylist(request, userId);

        // Assert
        assertNotNull(result);
        verify(playlistSharingService, times(1)).importPlaylist(request, userId);
    }

    @Test
    void duplicatePlaylist_DelegatesToSharingService() {
        // Arrange
        PlaylistDTO.DuplicateRequest request = PlaylistDTO.DuplicateRequest.builder()
                .newName("Duplicated Playlist")
                .build();
        
        PlaylistDTO.Response expectedResponse = PlaylistDTO.Response.builder()
                .id(2L)
                .name("Duplicated Playlist")
                .build();
        
        when(playlistSharingService.duplicatePlaylist(playlistId, request, userId)).thenReturn(expectedResponse);

        // Act
        PlaylistDTO.Response result = playlistManagementService.duplicatePlaylist(playlistId, request, userId);

        // Assert
        assertNotNull(result);
        verify(playlistSharingService, times(1)).duplicatePlaylist(playlistId, request, userId);
    }

    // ===============================
    // PERMISSION DELEGATION TESTS
    // ===============================

    @Test
    void canUserModifyPlaylist_DelegatesToCrudService() {
        // Arrange
        when(playlistCrudService.canUserModifyPlaylist(playlistId, userId)).thenReturn(true);

        // Act
        boolean result = playlistManagementService.canUserModifyPlaylist(playlistId, userId);

        // Assert
        assert result;
        verify(playlistCrudService, times(1)).canUserModifyPlaylist(playlistId, userId);
    }

    @Test
    void getPopularPlaylists_DelegatesToCrudService() {
        // Arrange
        PlaylistDTO.SearchResult expectedResult = PlaylistDTO.SearchResult.builder()
                .totalResults(5L)
                .build();
        
        when(playlistCrudService.getPopularPlaylists(pageable)).thenReturn(expectedResult);

        // Act
        PlaylistDTO.SearchResult result = playlistManagementService.getPopularPlaylists(pageable);

        // Assert
        assertNotNull(result);
        verify(playlistCrudService, times(1)).getPopularPlaylists(pageable);
    }
}