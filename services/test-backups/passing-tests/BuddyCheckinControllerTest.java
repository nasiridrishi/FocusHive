package com.focushive.buddy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.buddy.constant.CheckInType;
import com.focushive.buddy.constant.MoodType;
import com.focushive.buddy.dto.CheckinRequestDto;
import com.focushive.buddy.dto.CheckinResponseDto;
import com.focushive.buddy.service.BuddyCheckinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Simple, focused tests for BuddyCheckinController.
 * Covers core user-level functionality without complex security setup.
 */
@WebMvcTest(value = BuddyCheckinController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
}, excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
    type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE, 
    value = {com.focushive.buddy.security.SimpleJwtAuthenticationFilter.class}
))
@Import(com.focushive.buddy.config.TestSecurityConfig.class)
@DisplayName("BuddyCheckinController - User Level Tests")
class BuddyCheckinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BuddyCheckinService buddyCheckinService;

    private UUID testUserId;
    private UUID testPartnershipId;
    private UUID testCheckinId;
    private CheckinRequestDto validCheckinRequest;
    private CheckinResponseDto checkinResponse;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPartnershipId = UUID.randomUUID();
        testCheckinId = UUID.randomUUID();

        validCheckinRequest = CheckinRequestDto.builder()
                .partnershipId(testPartnershipId)
                .checkinType(CheckInType.DAILY)
                .mood(MoodType.MOTIVATED)
                .productivityRating(8)
                .focusHours(6)
                .content("Had a productive day working on the project")
                .notes("Completed all planned tasks")
                .build();

        checkinResponse = CheckinResponseDto.builder()
                .id(testCheckinId)
                .userId(testUserId)
                .partnershipId(testPartnershipId)
                .checkinType(CheckInType.DAILY)
                .mood(MoodType.MOTIVATED)
                .productivityRating(8)
                .content("Had a productive day working on the project")
                .createdAt(LocalDateTime.now())
                .summary("Daily check-in completed successfully")
                .build();
    }

    @Test
    @DisplayName("User can create a daily check-in successfully")
    void createDailyCheckin_Success() throws Exception {
        // Given
        when(buddyCheckinService.createDailyCheckin(eq(testUserId), any(CheckinRequestDto.class)))
                .thenReturn(checkinResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/buddy/checkins/daily")
                .header("X-User-ID", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCheckinRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testCheckinId.toString()))
                .andExpect(jsonPath("$.data.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.data.mood").value("MOTIVATED"))
                .andExpect(jsonPath("$.message").value("Daily check-in created successfully"));
    }

    @Test
    @DisplayName("User gets validation error for invalid productivity rating")
    void createDailyCheckin_InvalidProductivityRating() throws Exception {
        // Given
        CheckinRequestDto invalidRequest = validCheckinRequest.toBuilder()
                .productivityRating(15) // Invalid: > 10
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/buddy/checkins/daily")
                .header("X-User-ID", testUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Productivity rating must not exceed 10"));
    }

    @Test
    @DisplayName("User gets error when User ID header is missing")
    void createDailyCheckin_MissingUserHeader() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/buddy/checkins/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCheckinRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User ID header is required"));
    }

    @Test
    @DisplayName("User can retrieve their check-in details")
    void getCheckinDetails_Success() throws Exception {
        // Given
        when(buddyCheckinService.getDailyCheckin(testUserId, testCheckinId))
                .thenReturn(checkinResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/buddy/checkins/{id}", testCheckinId)
                .header("X-User-ID", testUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testCheckinId.toString()))
                .andExpect(jsonPath("$.data.mood").value("MOTIVATED"))
                .andExpect(jsonPath("$.message").value("Check-in details retrieved"));
    }

    @Test
    @DisplayName("User gets error for invalid UUID format")
    void getCheckinDetails_InvalidUUID() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/buddy/checkins/{id}", "invalid-uuid")
                .header("X-User-ID", testUserId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid check-in ID format"));
    }
}