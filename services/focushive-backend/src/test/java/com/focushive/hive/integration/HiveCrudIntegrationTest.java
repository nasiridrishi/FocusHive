package com.focushive.hive.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.hive.dto.CreateHiveRequest;
import com.focushive.hive.dto.HiveResponse;
import com.focushive.hive.entity.Hive;
import com.focushive.hive.repository.HiveRepository;
import com.focushive.user.entity.User;
import com.focushive.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for Hive CRUD operations using TestContainers with PostgreSQL.
 * Tests the full stack: Controller → Service → Repository → Database
 * 
 * Following TDD approach:
 * 1. Write failing test
 * 2. Make it pass
 * 3. Refactor if needed
 */
@AutoConfigureWebMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class HiveCrudIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HiveRepository hiveRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        hiveRepository.deleteAll();
        userRepository.deleteAll();

        // Create a test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@example.com");
        testUser.setPassword("hashedpassword");
        testUser.setDisplayName("Test User");
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should create a new hive and persist to PostgreSQL database")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void shouldCreateHiveAndPersistToDatabase() throws Exception {
        // Given - Arrange: Create request data
        CreateHiveRequest request = new CreateHiveRequest();
        request.setName("My Test Hive");
        request.setDescription("A test hive for integration testing");
        request.setMaxMembers(15);
        request.setIsPublic(true);
        request.setType(Hive.HiveType.STUDY);

        String requestJson = objectMapper.writeValueAsString(request);

        // When - Act: Create hive via REST API
        String responseJson = mockMvc.perform(post("/api/v1/hives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                // Then - Assert: Verify HTTP response
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("My Test Hive"))
                .andExpect(jsonPath("$.description").value("A test hive for integration testing"))
                .andExpect(jsonPath("$.maxMembers").value(15))
                .andExpect(jsonPath("$.isPublic").value(true))
                .andExpect(jsonPath("$.type").value("STUDY"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.slug").exists())
                .andReturn().getResponse().getContentAsString();

        // Parse response to get created hive ID
        HiveResponse response = objectMapper.readValue(responseJson, HiveResponse.class);

        // Then - Assert: Verify hive is persisted in PostgreSQL database
        Optional<Hive> savedHive = hiveRepository.findById(response.getId());
        assertThat(savedHive).isPresent();
        assertThat(savedHive.get().getName()).isEqualTo("My Test Hive");
        assertThat(savedHive.get().getDescription()).isEqualTo("A test hive for integration testing");
        assertThat(savedHive.get().getMaxMembers()).isEqualTo(15);
        assertThat(savedHive.get().getIsPublic()).isTrue();
        assertThat(savedHive.get().getType()).isEqualTo(Hive.HiveType.STUDY);
        assertThat(savedHive.get().getOwner().getId()).isEqualTo(testUser.getId());
        assertThat(savedHive.get().getSlug()).isNotBlank();
    }

    @Test
    @DisplayName("Should retrieve created hive by ID from PostgreSQL database")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void shouldRetrieveHiveByIdFromDatabase() throws Exception {
        // Given - Arrange: Create a hive first
        CreateHiveRequest createRequest = new CreateHiveRequest();
        createRequest.setName("Retrievable Hive");
        createRequest.setDescription("A hive to test retrieval");
        createRequest.setMaxMembers(20);
        createRequest.setIsPublic(false);
        createRequest.setType(Hive.HiveType.WORK);

        String createRequestJson = objectMapper.writeValueAsString(createRequest);

        // Create the hive
        String createResponseJson = mockMvc.perform(post("/api/v1/hives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        HiveResponse createResponse = objectMapper.readValue(createResponseJson, HiveResponse.class);
        String hiveId = createResponse.getId();

        // When - Act: Retrieve the hive by ID
        mockMvc.perform(get("/api/v1/hives/{id}", hiveId))
                // Then - Assert: Verify the retrieved hive data
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(hiveId))
                .andExpect(jsonPath("$.name").value("Retrievable Hive"))
                .andExpect(jsonPath("$.description").value("A hive to test retrieval"))
                .andExpect(jsonPath("$.maxMembers").value(20))
                .andExpect(jsonPath("$.isPublic").value(false))
                .andExpect(jsonPath("$.type").value("WORK"))
                .andExpect(jsonPath("$.slug").exists());

        // Then - Assert: Verify hive exists in database
        Optional<Hive> retrievedHive = hiveRepository.findById(hiveId);
        assertThat(retrievedHive).isPresent();
        assertThat(retrievedHive.get().getName()).isEqualTo("Retrievable Hive");
    }
}