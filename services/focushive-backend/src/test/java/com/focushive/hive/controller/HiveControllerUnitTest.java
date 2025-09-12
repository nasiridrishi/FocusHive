package com.focushive.hive.controller;

import com.focushive.hive.dto.CreateHiveRequest;
import com.focushive.hive.dto.HiveResponse;
import com.focushive.hive.dto.UpdateHiveRequest;
import com.focushive.hive.entity.Hive;
import com.focushive.hive.service.HiveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;

/**
 * Unit test for HiveController using Mockito to test CRUD operations
 * without requiring full Spring context.
 */
@ExtendWith(MockitoExtension.class)
class HiveControllerUnitTest {

    @Mock
    private HiveService hiveService;

    @InjectMocks
    private HiveController hiveController;

    @BeforeEach
    void setUp() {
        // Setup is minimal since we're testing controller directly
    }

    @Test
    @DisplayName("CREATE: Should create hive and return 201 with hive response")
    void createHive_ShouldReturnCreatedHive_WhenValidRequest() throws Exception {
        // Arrange
        CreateHiveRequest request = new CreateHiveRequest();
        request.setName("Test Hive");
        request.setDescription("A test hive for unit testing");
        request.setMaxMembers(10);
        request.setIsPublic(true);
        request.setType(Hive.HiveType.GENERAL);

        HiveResponse expectedResponse = new HiveResponse();
        expectedResponse.setId("hive-123");
        expectedResponse.setName("Test Hive");
        expectedResponse.setDescription("A test hive for unit testing");
        expectedResponse.setOwnerId("test-user");
        expectedResponse.setMaxMembers(10);
        expectedResponse.setCurrentMembers(1);
        expectedResponse.setIsPublic(true);
        expectedResponse.setIsActive(true);
        expectedResponse.setType(Hive.HiveType.GENERAL);
        expectedResponse.setCreatedAt(LocalDateTime.now());
        expectedResponse.setUpdatedAt(LocalDateTime.now());

        // Mock the service call
        when(hiveService.createHive(any(CreateHiveRequest.class), eq("test-user")))
            .thenReturn(expectedResponse);

        // Create mock UserDetails
        UserDetails userDetails = User.builder()
            .username("test-user")
            .password("password")
            .authorities(Collections.emptyList())
            .build();

        // Act - Test the controller method directly to avoid security annotations
        ResponseEntity<HiveResponse> response = hiveController.createHive(request, userDetails);
        
        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("hive-123", response.getBody().getId());
        assertEquals("Test Hive", response.getBody().getName());
        assertEquals("A test hive for unit testing", response.getBody().getDescription());
        assertEquals("test-user", response.getBody().getOwnerId());
        assertEquals(10, response.getBody().getMaxMembers());
        assertEquals(1, response.getBody().getCurrentMembers());
        assertEquals(true, response.getBody().getIsPublic());
        assertEquals(true, response.getBody().getIsActive());
        assertEquals(Hive.HiveType.GENERAL, response.getBody().getType());
    }

    @Test
    @DisplayName("READ: Should get hive and return 200 with hive response")
    void getHive_ShouldReturnHiveResponse_WhenValidId() {
        // Arrange
        String hiveId = "hive-123";
        
        HiveResponse expectedResponse = new HiveResponse();
        expectedResponse.setId(hiveId);
        expectedResponse.setName("Test Hive");
        expectedResponse.setDescription("A test hive for unit testing");
        expectedResponse.setOwnerId("test-user");
        expectedResponse.setMaxMembers(10);
        expectedResponse.setCurrentMembers(3);
        expectedResponse.setIsPublic(true);
        expectedResponse.setIsActive(true);
        expectedResponse.setType(Hive.HiveType.GENERAL);
        expectedResponse.setCreatedAt(LocalDateTime.now());
        expectedResponse.setUpdatedAt(LocalDateTime.now());

        // Mock the service call
        when(hiveService.getHive(eq(hiveId), eq("test-user")))
            .thenReturn(expectedResponse);

        // Create mock UserDetails
        UserDetails userDetails = User.builder()
            .username("test-user")
            .password("password")
            .authorities(Collections.emptyList())
            .build();

        // Act - Test the controller method directly
        ResponseEntity<HiveResponse> response = hiveController.getHive(hiveId, userDetails);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(hiveId, response.getBody().getId());
        assertEquals("Test Hive", response.getBody().getName());
        assertEquals("A test hive for unit testing", response.getBody().getDescription());
        assertEquals("test-user", response.getBody().getOwnerId());
        assertEquals(10, response.getBody().getMaxMembers());
        assertEquals(3, response.getBody().getCurrentMembers());
        assertEquals(true, response.getBody().getIsPublic());
        assertEquals(true, response.getBody().getIsActive());
        assertEquals(Hive.HiveType.GENERAL, response.getBody().getType());
    }

    @Test
    @DisplayName("UPDATE: Should update hive and return 200 with updated hive response")
    void updateHive_ShouldReturnUpdatedHive_WhenValidRequest() {
        // Arrange
        String hiveId = "hive-123";
        
        UpdateHiveRequest request = new UpdateHiveRequest();
        request.setName("Updated Test Hive");
        request.setDescription("An updated test hive description");
        request.setMaxMembers(20);
        request.setIsPublic(false);
        request.setType(Hive.HiveType.STUDY);
        
        HiveResponse expectedResponse = new HiveResponse();
        expectedResponse.setId(hiveId);
        expectedResponse.setName("Updated Test Hive");
        expectedResponse.setDescription("An updated test hive description");
        expectedResponse.setOwnerId("test-user");
        expectedResponse.setMaxMembers(20);
        expectedResponse.setCurrentMembers(5);
        expectedResponse.setIsPublic(false);
        expectedResponse.setIsActive(true);
        expectedResponse.setType(Hive.HiveType.STUDY);
        expectedResponse.setCreatedAt(LocalDateTime.now().minusDays(1));
        expectedResponse.setUpdatedAt(LocalDateTime.now());

        // Mock the service call
        when(hiveService.updateHive(eq(hiveId), any(UpdateHiveRequest.class), eq("test-user")))
            .thenReturn(expectedResponse);

        // Create mock UserDetails
        UserDetails userDetails = User.builder()
            .username("test-user")
            .password("password")
            .authorities(Collections.emptyList())
            .build();

        // Act - Test the controller method directly
        ResponseEntity<HiveResponse> response = hiveController.updateHive(hiveId, request, userDetails);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(hiveId, response.getBody().getId());
        assertEquals("Updated Test Hive", response.getBody().getName());
        assertEquals("An updated test hive description", response.getBody().getDescription());
        assertEquals("test-user", response.getBody().getOwnerId());
        assertEquals(20, response.getBody().getMaxMembers());
        assertEquals(5, response.getBody().getCurrentMembers());
        assertEquals(false, response.getBody().getIsPublic());
        assertEquals(true, response.getBody().getIsActive());
        assertEquals(Hive.HiveType.STUDY, response.getBody().getType());
    }

    @Test
    @DisplayName("DELETE: Should delete hive and return 204 No Content")
    void deleteHive_ShouldReturnNoContent_WhenValidId() {
        // Arrange
        String hiveId = "hive-123";
        
        // Mock the service call - void method
        doNothing().when(hiveService).deleteHive(eq(hiveId), eq("test-user"));

        // Create mock UserDetails
        UserDetails userDetails = User.builder()
            .username("test-user")
            .password("password")
            .authorities(Collections.emptyList())
            .build();

        // Act - Test the controller method directly
        ResponseEntity<Void> response = hiveController.deleteHive(hiveId, userDetails);
        
        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        
        // Verify service was called with correct parameters
        verify(hiveService).deleteHive(eq(hiveId), eq("test-user"));
    }
}