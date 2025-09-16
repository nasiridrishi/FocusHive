package com.focushive.forum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.forum.dto.*;
import com.focushive.forum.service.ForumService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ForumController.
 * Following TDD approach - write tests first, then implement controller.
 */
@WebMvcTest(ForumController.class)
@TestPropertySource(properties = "app.features.forum.enabled=true")
@DisplayName("Forum Controller Tests")
class ForumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ForumService forumService;

    @Autowired
    private ObjectMapper objectMapper;

    private ForumPostDTO testPost;
    private ForumReplyDTO testReply;
    private ForumCategoryDTO testCategory;
    private Page<ForumPostDTO> testPostPage;

    @BeforeEach
    void setUp() {
        testCategory = ForumCategoryDTO.builder()
                .id("category-1")
                .name("General Discussion")
                .description("General discussion category")
                .slug("general-discussion")
                .build();

        testPost = ForumPostDTO.builder()
                .id("post-1")
                .categoryId("category-1")
                .userId("user-1")
                .title("Test Forum Post")
                .content("This is a test forum post content")
                .tags(new String[]{"discussion", "test"})
                .viewCount(10)
                .replyCount(5)
                .voteScore(3)
                .createdAt(LocalDateTime.now())
                .build();

        testReply = ForumReplyDTO.builder()
                .id("reply-1")
                .postId("post-1")
                .userId("user-2")
                .content("This is a test reply")
                .voteScore(2)
                .createdAt(LocalDateTime.now())
                .build();

        testPostPage = new PageImpl<>(List.of(testPost));
    }

    // CATEGORY ENDPOINTS TESTS

    @Test
    @WithMockUser
    @DisplayName("Should get all categories")
    void shouldGetAllCategories() throws Exception {
        // Given
        List<ForumCategoryDTO> categories = Arrays.asList(testCategory);
        given(forumService.getAllCategories()).willReturn(categories);

        // When & Then
        mockMvc.perform(get("/api/forum/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("category-1"))
                .andExpect(jsonPath("$[0].name").value("General Discussion"));

        verify(forumService).getAllCategories();
    }

    @Test
    @WithMockUser
    @DisplayName("Should get category by ID")
    void shouldGetCategoryById() throws Exception {
        // Given
        given(forumService.getCategory("category-1")).willReturn(testCategory);

        // When & Then
        mockMvc.perform(get("/api/forum/categories/{categoryId}", "category-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("category-1"))
                .andExpected(jsonPath("$.name").value("General Discussion"));

        verify(forumService).getCategory("category-1");
    }

    @Test
    @WithMockUser
    @DisplayName("Should create new category")
    void shouldCreateNewCategory() throws Exception {
        // Given
        given(forumService.createCategory(any(ForumCategoryDTO.class))).willReturn(testCategory);

        // When & Then
        mockMvc.perform(post("/api/forum/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCategory)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("category-1"))
                .andExpected(jsonPath("$.name").value("General Discussion"));

        verify(forumService).createCategory(any(ForumCategoryDTO.class));
    }

    // POST ENDPOINTS TESTS

    @Test
    @WithMockUser
    @DisplayName("Should get posts by category")
    void shouldGetPostsByCategory() throws Exception {
        // Given
        given(forumService.getPostsByCategory(eq("category-1"), any(Pageable.class)))
                .willReturn(testPostPage);

        // When & Then
        mockMvc.perform(get("/api/forum/categories/{categoryId}/posts", "category-1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("post-1"))
                .andExpected(jsonPath("$.content[0].title").value("Test Forum Post"));

        verify(forumService).getPostsByCategory(eq("category-1"), any(Pageable.class));
    }

    @Test
    @WithMockUser
    @DisplayName("Should get post by ID")
    void shouldGetPostById() throws Exception {
        // Given
        given(forumService.getPost("post-1")).willReturn(testPost);

        // When & Then
        mockMvc.perform(get("/api/forum/posts/{postId}", "post-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("post-1"))
                .andExpected(jsonPath("$.title").value("Test Forum Post"));

        verify(forumService).getPost("post-1");
    }

    @Test
    @WithMockUser(username = "user-1")
    @DisplayName("Should create new post")
    void shouldCreateNewPost() throws Exception {
        // Given
        given(forumService.createPost(eq("user-1"), any(ForumPostDTO.class))).willReturn(testPost);

        // When & Then
        mockMvc.perform(post("/api/forum/posts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testPost)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("post-1"))
                .andExpected(jsonPath("$.title").value("Test Forum Post"));

        verify(forumService).createPost(eq("user-1"), any(ForumPostDTO.class));
    }

    @Test
    @WithMockUser(username = "user-1")
    @DisplayName("Should update existing post")
    void shouldUpdateExistingPost() throws Exception {
        // Given
        given(forumService.updatePost(eq("post-1"), eq("user-1"), any(ForumPostDTO.class)))
                .willReturn(testPost);

        // When & Then
        mockMvc.perform(put("/api/forum/posts/{postId}", "post-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testPost)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("post-1"))
                .andExpected(jsonPath("$.title").value("Test Forum Post"));

        verify(forumService).updatePost(eq("post-1"), eq("user-1"), any(ForumPostDTO.class));
    }

    @Test
    @WithMockUser(username = "user-1")
    @DisplayName("Should delete post")
    void shouldDeletePost() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/forum/posts/{postId}", "post-1")
                        .with(csrf()))
                .andExpected(status().isNoContent());

        verify(forumService).deletePost("post-1", "user-1");
    }

    // REPLY ENDPOINTS TESTS

    @Test
    @WithMockUser
    @DisplayName("Should get post replies")
    void shouldGetPostReplies() throws Exception {
        // Given
        List<ForumReplyDTO> replies = Arrays.asList(testReply);
        given(forumService.getPostReplies("post-1")).willReturn(replies);

        // When & Then
        mockMvc.perform(get("/api/forum/posts/{postId}/replies", "post-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("reply-1"))
                .andExpected(jsonPath("$[0].content").value("This is a test reply"));

        verify(forumService).getPostReplies("post-1");
    }

    @Test
    @WithMockUser(username = "user-2")
    @DisplayName("Should create new reply")
    void shouldCreateNewReply() throws Exception {
        // Given
        given(forumService.createReply(eq("post-1"), eq("user-2"), any(ForumReplyDTO.class)))
                .willReturn(testReply);

        // When & Then
        mockMvc.perform(post("/api/forum/posts/{postId}/replies", "post-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReply)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("reply-1"))
                .andExpected(jsonPath("$.content").value("This is a test reply"));

        verify(forumService).createReply(eq("post-1"), eq("user-2"), any(ForumReplyDTO.class));
    }

    // VOTING ENDPOINTS TESTS

    @Test
    @WithMockUser(username = "user-3")
    @DisplayName("Should vote on post")
    void shouldVoteOnPost() throws Exception {
        // Given
        ForumVoteDTO vote = ForumVoteDTO.builder()
                .id("vote-1")
                .userId("user-3")
                .voteType(1)
                .build();
        given(forumService.voteOnPost("post-1", "user-3", 1)).willReturn(vote);

        // When & Then
        mockMvc.perform(post("/api/forum/posts/{postId}/vote", "post-1")
                        .with(csrf())
                        .param("voteType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("vote-1"))
                .andExpected(jsonPath("$.voteType").value(1));

        verify(forumService).voteOnPost("post-1", "user-3", 1);
    }

    // SEARCH ENDPOINTS TESTS

    @Test
    @WithMockUser
    @DisplayName("Should search posts")
    void shouldSearchPosts() throws Exception {
        // Given
        given(forumService.searchPosts(eq("test"), any(Pageable.class))).willReturn(testPostPage);

        // When & Then
        mockMvc.perform(get("/api/forum/search")
                        .param("q", "test")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("post-1"));

        verify(forumService).searchPosts(eq("test"), any(Pageable.class));
    }
}