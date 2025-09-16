package com.focushive.forum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.forum.domain.Forum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ForumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    public void shouldCreateForum() throws Exception {
        // given
        Forum forum = new Forum();
        forum.setName("Test Forum");
        forum.setDescription("Test Description");
        forum.setCreatedAt(Instant.now());

        // when
        mockMvc.perform(post("/api/v1/forums")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forum)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    public void shouldGetAllForums() throws Exception {
        // when
        mockMvc.perform(get("/api/v1/forums"))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
