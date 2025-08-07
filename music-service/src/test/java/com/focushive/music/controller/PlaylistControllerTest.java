package com.focushive.music.controller;

import com.focushive.music.dto.PlaylistDTO;
import com.focushive.music.exception.MusicServiceException;
import com.focushive.music.service.PlaylistManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for PlaylistController REST endpoints.
 * 
 * Tests all 14 required endpoints with various scenarios including
 * success cases, error cases, validation, authorization, and edge cases.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@WebMvcTest(PlaylistController.class)
@DisplayName("PlaylistController REST API Tests")
class PlaylistControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private PlaylistManagementService playlistManagementService;

    private PlaylistDTO.CreateRequest createRequest;
    private PlaylistDTO.UpdateRequest updateRequest;
    private PlaylistDTO.Response playlistResponse;
    private PlaylistDTO.SearchResult searchResult;

    @BeforeEach
    void setUp() {
        createRequest = PlaylistDTO.CreateRequest.builder()
            .name("Test Playlist")
            .description("A test playlist")
            .isPublic(false)
            .isCollaborative(false)
            .build();

        updateRequest = PlaylistDTO.UpdateRequest.builder()
            .name("Updated Playlist")
            .description("Updated description")
            .isPublic(true)
            .build();

        playlistResponse = PlaylistDTO.Response.builder()
            .id(1L)
            .name("Test Playlist")
            .description("A test playlist")
            .createdBy("user123")
            .isPublic(false)
            .isCollaborative(false)
            .totalTracks(0)
            .totalDurationMs(0L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .canModify(true)
            .userPermission("OWNER")
            .formattedDuration("0:00")
            .build();

        searchResult = PlaylistDTO.SearchResult.builder()
            .playlists(List.of(playlistResponse))
            .totalResults(1L)
            .currentPage(0)
            .totalPages(1)
            .build();
    }

    @Nested
    @DisplayName("GET /api/music/playlists - List user's playlists")
    class ListUserPlaylists {

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should return user's playlists successfully")
        void shouldReturnUserPlaylistsSuccessfully() throws Exception {
            when(playlistManagementService.getUserPlaylists(eq("user123"), any(Pageable.class)))
                .thenReturn(searchResult);

            mockMvc.perform(get("/api/music/playlists")
                    .param("page", "0")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.playlists").isArray())
                .andExpect(jsonPath("$.playlists[0].id").value(1L))
                .andExpect(jsonPath("$.playlists[0].name").value("Test Playlist"))
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should handle empty playlist list")
        void shouldHandleEmptyPlaylistList() throws Exception {
            PlaylistDTO.SearchResult emptyResult = PlaylistDTO.SearchResult.builder()
                .playlists(List.of())
                .totalResults(0L)
                .currentPage(0)
                .totalPages(0)
                .build();

            when(playlistManagementService.getUserPlaylists(eq("user123"), any(Pageable.class)))
                .thenReturn(emptyResult);

            mockMvc.perform(get("/api/music/playlists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playlists").isEmpty())
                .andExpect(jsonPath("$.totalResults").value(0));
        }

        @Test
        @DisplayName("Should require authentication")
        void shouldRequireAuthentication() throws Exception {
            mockMvc.perform(get("/api/music/playlists"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/music/playlists - Create new playlist")
    class CreateNewPlaylist {

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should create playlist successfully")
        void shouldCreatePlaylistSuccessfully() throws Exception {
            when(playlistManagementService.createPlaylist(any(PlaylistDTO.CreateRequest.class), eq("user123")))
                .thenReturn(playlistResponse);

            mockMvc.perform(post("/api/music/playlists")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test Playlist"))
                .andExpect(jsonPath("$.createdBy").value("user123"));

            verify(playlistManagementService).createPlaylist(any(PlaylistDTO.CreateRequest.class), eq("user123"));
        }

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should validate required fields")
        void shouldValidateRequiredFields() throws Exception {
            PlaylistDTO.CreateRequest invalidRequest = PlaylistDTO.CreateRequest.builder()
                .name("") // Empty name should fail validation
                .build();

            mockMvc.perform(post("/api/music/playlists")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should handle unauthorized hive access")
        void shouldHandleUnauthorizedHiveAccess() throws Exception {
            createRequest.setHiveId("unauthorized-hive");
            
            when(playlistManagementService.createPlaylist(any(PlaylistDTO.CreateRequest.class), eq("user123")))
                .thenThrow(new MusicServiceException.UnauthorizedOperationException("User is not a member of hive"));

            mockMvc.perform(post("/api/music/playlists")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Unauthorized Operation"));
        }

        @Test
        @DisplayName("Should require authentication")
        void shouldRequireAuthentication() throws Exception {
            mockMvc.perform(post("/api/music/playlists")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/music/playlists/{playlistId} - Get playlist details")
    class GetPlaylistDetails {

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should return playlist details successfully")
        void shouldReturnPlaylistDetailsSuccessfully() throws Exception {
            when(playlistManagementService.getPlaylistById(1L, "user123"))
                .thenReturn(playlistResponse);

            mockMvc.perform(get("/api/music/playlists/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test Playlist"))
                .andExpect(jsonPath("$.canModify").value(true));
        }

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should handle playlist not found")
        void shouldHandlePlaylistNotFound() throws Exception {
            when(playlistManagementService.getPlaylistById(999L, "user123"))
                .thenThrow(new MusicServiceException.ResourceNotFoundException("Playlist not found"));

            mockMvc.perform(get("/api/music/playlists/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
        }

        @Test
        @WithMockUser(username = "unauthorized-user")
        @DisplayName("Should handle unauthorized access")
        void shouldHandleUnauthorizedAccess() throws Exception {
            when(playlistManagementService.getPlaylistById(1L, "unauthorized-user"))
                .thenThrow(new MusicServiceException.UnauthorizedOperationException("User not authorized"));

            mockMvc.perform(get("/api/music/playlists/1"))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/music/playlists/{playlistId} - Update playlist metadata")
    class UpdatePlaylistMetadata {

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should update playlist successfully")
        void shouldUpdatePlaylistSuccessfully() throws Exception {
            PlaylistDTO.Response updatedResponse = PlaylistDTO.Response.builder()
                .id(1L)
                .name("Updated Playlist")
                .description("Updated description")
                .isPublic(true)
                .build();

            when(playlistManagementService.updatePlaylist(eq(1L), any(PlaylistDTO.UpdateRequest.class), eq("user123")))
                .thenReturn(updatedResponse);

            mockMvc.perform(put("/api/music/playlists/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Updated Playlist"))
                .andExpect(jsonPath("$.isPublic").value(true));
        }

        @Test
        @WithMockUser(username = "unauthorized-user")
        @DisplayName("Should handle unauthorized update")
        void shouldHandleUnauthorizedUpdate() throws Exception {
            when(playlistManagementService.updatePlaylist(eq(1L), any(PlaylistDTO.UpdateRequest.class), eq("unauthorized-user")))
                .thenThrow(new MusicServiceException.UnauthorizedOperationException("User not authorized"));

            mockMvc.perform(put("/api/music/playlists/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/music/playlists/{playlistId} - Delete playlist")
    class DeletePlaylist {

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should delete playlist successfully")
        void shouldDeletePlaylistSuccessfully() throws Exception {
            doNothing().when(playlistManagementService).deletePlaylist(1L, "user123");

            mockMvc.perform(delete("/api/music/playlists/1")
                    .with(csrf()))
                .andExpect(status().isNoContent());

            verify(playlistManagementService).deletePlaylist(1L, "user123");
        }

        @Test
        @WithMockUser(username = "unauthorized-user")
        @DisplayName("Should handle unauthorized deletion")
        void shouldHandleUnauthorizedDeletion() throws Exception {
            doThrow(new MusicServiceException.UnauthorizedOperationException("User not authorized"))
                .when(playlistManagementService).deletePlaylist(1L, "unauthorized-user");

            mockMvc.perform(delete("/api/music/playlists/1")
                    .with(csrf()))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/music/playlists/{playlistId}/tracks - Add tracks")
    class AddTracks {

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should add track successfully")
        void shouldAddTrackSuccessfully() throws Exception {
            PlaylistDTO.AddTrackRequest addTrackRequest = PlaylistDTO.AddTrackRequest.builder()
                .spotifyTrackId("track123")
                .position(0)
                .build();

            PlaylistDTO.TrackInfo trackInfo = PlaylistDTO.TrackInfo.builder()
                .id(1L)
                .spotifyTrackId("track123")
                .trackName("Test Track")
                .artistName("Test Artist")
                .positionInPlaylist(0)
                .build();

            when(playlistManagementService.addTrackToPlaylist(eq(1L), any(PlaylistDTO.AddTrackRequest.class), eq("user123")))
                .thenReturn(trackInfo);

            mockMvc.perform(post("/api/music/playlists/1/tracks")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addTrackRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.spotifyTrackId").value("track123"))
                .andExpect(jsonPath("$.trackName").value("Test Track"));
        }

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should handle duplicate track")
        void shouldHandleDuplicateTrack() throws Exception {
            PlaylistDTO.AddTrackRequest addTrackRequest = PlaylistDTO.AddTrackRequest.builder()
                .spotifyTrackId("track123")
                .build();

            when(playlistManagementService.addTrackToPlaylist(eq(1L), any(PlaylistDTO.AddTrackRequest.class), eq("user123")))
                .thenThrow(new MusicServiceException.BusinessRuleViolationException("Track already exists"));

            mockMvc.perform(post("/api/music/playlists/1/tracks")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addTrackRequest)))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/music/playlists/{playlistId}/tracks/{trackId} - Remove track")
    class RemoveTrack {

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should remove track successfully")
        void shouldRemoveTrackSuccessfully() throws Exception {
            doNothing().when(playlistManagementService).removeTrackFromPlaylist(1L, 1L, "user123");

            mockMvc.perform(delete("/api/music/playlists/1/tracks/1")
                    .with(csrf()))
                .andExpect(status().isNoContent());

            verify(playlistManagementService).removeTrackFromPlaylist(1L, 1L, "user123");
        }
    }

    @Nested
    @DisplayName("PUT /api/music/playlists/{playlistId}/tracks/reorder - Reorder tracks")
    class ReorderTracks {

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should reorder tracks successfully")
        void shouldReorderTracksSuccessfully() throws Exception {
            PlaylistDTO.TrackReorderRequest reorderRequest = PlaylistDTO.TrackReorderRequest.builder()
                .trackOrders(Map.of(1L, 1, 2L, 0))
                .build();

            doNothing().when(playlistManagementService)
                .reorderPlaylistTracks(eq(1L), any(PlaylistDTO.TrackReorderRequest.class), eq("user123"));

            mockMvc.perform(put("/api/music/playlists/1/tracks/reorder")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reorderRequest)))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/music/playlists/{playlistId}/duplicate - Duplicate playlist")
    class DuplicatePlaylist {

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should duplicate playlist successfully")
        void shouldDuplicatePlaylistSuccessfully() throws Exception {
            PlaylistDTO.DuplicateRequest duplicateRequest = PlaylistDTO.DuplicateRequest.builder()
                .newName("Copy of Test Playlist")
                .includeCollaborators(false)
                .build();

            PlaylistDTO.Response duplicatedResponse = PlaylistDTO.Response.builder()
                .id(2L)
                .name("Copy of Test Playlist")
                .createdBy("user123")
                .build();

            when(playlistManagementService.duplicatePlaylist(eq(1L), any(PlaylistDTO.DuplicateRequest.class), eq("user123")))
                .thenReturn(duplicatedResponse);

            mockMvc.perform(post("/api/music/playlists/1/duplicate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("Copy of Test Playlist"));
        }
    }

    @Nested
    @DisplayName("POST /api/music/playlists/{playlistId}/share - Share with hive")
    class ShareWithHive {

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should share playlist with hive successfully")
        void shouldSharePlaylistWithHiveSuccessfully() throws Exception {
            PlaylistDTO.SharePlaylistRequest shareRequest = PlaylistDTO.SharePlaylistRequest.builder()
                .hiveId("hive456")
                .permissionLevel("VIEWER")
                .message("Check out this playlist!")
                .build();

            doNothing().when(playlistManagementService)
                .sharePlaylistWithHive(eq(1L), any(PlaylistDTO.SharePlaylistRequest.class), eq("user123"));

            mockMvc.perform(post("/api/music/playlists/1/share")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(shareRequest)))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/music/playlists/hive/{hiveId} - Get hive playlists")
    class GetHivePlaylists {

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should return hive playlists successfully")
        void shouldReturnHivePlaylistsSuccessfully() throws Exception {
            when(playlistManagementService.getHivePlaylists(eq("hive456"), eq("user123"), any(Pageable.class)))
                .thenReturn(searchResult);

            mockMvc.perform(get("/api/music/playlists/hive/hive456"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.playlists").isArray())
                .andExpect(jsonPath("$.totalResults").value(1));
        }

        @Test
        @WithMockUser(username = "unauthorized-user")
        @DisplayName("Should handle unauthorized hive access")
        void shouldHandleUnauthorizedHiveAccess() throws Exception {
            when(playlistManagementService.getHivePlaylists(eq("hive456"), eq("unauthorized-user"), any(Pageable.class)))
                .thenThrow(new MusicServiceException.UnauthorizedOperationException("User is not a member of hive"));

            mockMvc.perform(get("/api/music/playlists/hive/hive456"))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/music/playlists/smart - Create smart playlist")
    class CreateSmartPlaylist {

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should create smart playlist successfully")
        void shouldCreateSmartPlaylistSuccessfully() throws Exception {
            PlaylistDTO.SmartPlaylistCriteriaRequest criteriaRequest = 
                PlaylistDTO.SmartPlaylistCriteriaRequest.builder()
                    .name("High Energy Tracks")
                    .energyLevel("HIGH")
                    .maxTracks(50)
                    .build();

            PlaylistDTO.Response smartPlaylistResponse = PlaylistDTO.Response.builder()
                .id(1L)
                .name("High Energy Tracks")
                .createdBy("user123")
                .totalTracks(25)
                .build();

            when(playlistManagementService.createSmartPlaylist(any(PlaylistDTO.SmartPlaylistCriteriaRequest.class), eq("user123")))
                .thenReturn(smartPlaylistResponse);

            mockMvc.perform(post("/api/music/playlists/smart")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(criteriaRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("High Energy Tracks"))
                .andExpect(jsonPath("$.totalTracks").value(25));
        }
    }

    @Nested
    @DisplayName("GET /api/music/playlists/{playlistId}/export - Export playlist")
    class ExportPlaylist {

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should export playlist successfully")
        void shouldExportPlaylistSuccessfully() throws Exception {
            PlaylistDTO.ExportResponse exportResponse = PlaylistDTO.ExportResponse.builder()
                .playlistName("Test Playlist")
                .format("JSON")
                .content("{\"name\":\"Test Playlist\",\"tracks\":[]}")
                .exportedAt(LocalDateTime.now())
                .totalTracks(0)
                .fileSizeBytes(42L)
                .build();

            when(playlistManagementService.exportPlaylist(1L, "user123"))
                .thenReturn(exportResponse);

            mockMvc.perform(get("/api/music/playlists/1/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.playlistName").value("Test Playlist"))
                .andExpect(jsonPath("$.format").value("JSON"))
                .andExpect(jsonPath("$.totalTracks").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/music/playlists/import - Import playlist")
    class ImportPlaylist {

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should import playlist successfully")
        void shouldImportPlaylistSuccessfully() throws Exception {
            PlaylistDTO.ImportRequest importRequest = PlaylistDTO.ImportRequest.builder()
                .source("SPOTIFY")
                .externalPlaylistId("spotify123")
                .importTracks(true)
                .newPlaylistName("Imported Playlist")
                .build();

            PlaylistDTO.Response importedResponse = PlaylistDTO.Response.builder()
                .id(1L)
                .name("Imported Playlist")
                .description("Imported from SPOTIFY")
                .createdBy("user123")
                .build();

            when(playlistManagementService.importPlaylist(any(PlaylistDTO.ImportRequest.class), eq("user123")))
                .thenReturn(importedResponse);

            mockMvc.perform(post("/api/music/playlists/import")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(importRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Imported Playlist"))
                .andExpect(jsonPath("$.description").value("Imported from SPOTIFY"));
        }

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should handle unsupported import source")
        void shouldHandleUnsupportedImportSource() throws Exception {
            PlaylistDTO.ImportRequest importRequest = PlaylistDTO.ImportRequest.builder()
                .source("UNSUPPORTED")
                .externalPlaylistId("unsupported123")
                .build();

            when(playlistManagementService.importPlaylist(any(PlaylistDTO.ImportRequest.class), eq("user123")))
                .thenThrow(new MusicServiceException.UnsupportedOperationException("Import source not supported"));

            mockMvc.perform(post("/api/music/playlists/import")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(importRequest)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    class ErrorHandlingAndEdgeCases {

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should handle malformed JSON")
        void shouldHandleMalformedJson() throws Exception {
            mockMvc.perform(post("/api/music/playlists")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should handle invalid path parameters")
        void shouldHandleInvalidPathParameters() throws Exception {
            mockMvc.perform(get("/api/music/playlists/invalid-id"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should handle service exceptions")
        void shouldHandleServiceExceptions() throws Exception {
            when(playlistManagementService.getPlaylistById(1L, "user123"))
                .thenThrow(new RuntimeException("Unexpected error"));

            mockMvc.perform(get("/api/music/playlists/1"))
                .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Content Type and Header Validation")
    class ContentTypeAndHeaderValidation {

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should require JSON content type for POST requests")
        void shouldRequireJsonContentTypeForPostRequests() throws Exception {
            mockMvc.perform(post("/api/music/playlists")
                    .with(csrf())
                    .contentType(MediaType.TEXT_PLAIN)
                    .content("invalid content"))
                .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @WithMockUser(username = "user123")
        @DisplayName("Should return JSON responses")
        void shouldReturnJsonResponses() throws Exception {
            when(playlistManagementService.getUserPlaylists(eq("user123"), any(Pageable.class)))
                .thenReturn(searchResult);

            mockMvc.perform(get("/api/music/playlists"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}