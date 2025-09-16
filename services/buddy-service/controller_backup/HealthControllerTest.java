package com.focushive.buddy.controller;

import com.focushive.buddy.config.TestSecurityConfig;
import com.focushive.buddy.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = HealthController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
})
@Import(TestSecurityConfig.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("buddy-service"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void healthEndpointShouldIndicateBuddyComponentsStatus() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.database").exists())
                .andExpect(jsonPath("$.components.matching").exists());
    }
}