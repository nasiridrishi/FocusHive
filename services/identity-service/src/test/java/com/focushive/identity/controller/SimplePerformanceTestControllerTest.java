package com.focushive.identity.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.focushive.identity.config.TestSecurityConfig;
import com.focushive.identity.config.TestConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Minimal test for PerformanceTestController using WebMvcTest.
 */
@WebMvcTest(controllers = PerformanceTestController.class)
@Import({TestSecurityConfig.class, TestConfig.class})
@ActiveProfiles({"test", "web-mvc-test"})
class SimplePerformanceTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void debug_ShouldReturnResponse() throws Exception {
        // First just test that the endpoint responds with 200
        mockMvc.perform(get("/api/v1/performance-test/debug"))
                .andExpect(status().isOk());
    }
    
    @Test
    void basic_ShouldReturnResponse() throws Exception {
        // First just test that the endpoint responds with 200
        mockMvc.perform(get("/api/v1/performance-test/basic"))
                .andExpect(status().isOk());
    }
}