package com.focushive.forum.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ForumPost entity.
 * Following TDD approach - write tests first, then implement the entity.
 */
@DisplayName("ForumPost Entity Tests")
class ForumPostTest {

    private ForumPost forumPost;
    private String validUserId;
    private String validHiveId;
    private String validTitle;
    private String validContent;
    private List<String> validTags;

    @BeforeEach
    void setUp() {
        validUserId = "user-123";
        validHiveId = "hive-456";
        validTitle = "Test Forum Post";
        validContent = "This is a test forum post content";
        validTags = List.of("discussion", "help", "testing");
    }

    @Test
    @DisplayName("Should create ForumPost with valid data")
    void shouldCreateForumPostWithValidData() {
        // Given & When
        forumPost = ForumPost.builder()
                .userId(validUserId)
                .hiveId(validHiveId)
                .title(validTitle)
                .content(validContent)
                .tags(validTags)
                .build();

        // Then
        assertThat(forumPost).isNotNull();
        assertThat(forumPost.getUserId()).isEqualTo(validUserId);
        assertThat(forumPost.getHiveId()).isEqualTo(validHiveId);
        assertThat(forumPost.getTitle()).isEqualTo(validTitle);
        assertThat(forumPost.getContent()).isEqualTo(validContent);
        assertThat(forumPost.getTags()).isEqualTo(validTags);
        assertThat(forumPost.getUpvotes()).isEqualTo(0);
        assertThat(forumPost.getDownvotes()).isEqualTo(0);
        assertThat(forumPost.isLocked()).isFalse();
        assertThat(forumPost.isPinned()).isFalse();
    }

    @Test
    @DisplayName("Should set and get ID correctly")
    void shouldSetAndGetIdCorrectly() {
        // Given
        String expectedId = "post-789";
        forumPost = createValidForumPost();

        // When
        forumPost.setId(expectedId);

        // Then
        assertThat(forumPost.getId()).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("Should track creation time")
    void shouldTrackCreationTime() {
        // Given
        LocalDateTime beforeCreation = LocalDateTime.now();

        // When
        forumPost = createValidForumPost();
        forumPost.onCreate(); // Simulate JPA @PrePersist

        LocalDateTime afterCreation = LocalDateTime.now();

        // Then
        assertThat(forumPost.getCreatedAt()).isNotNull();
        assertThat(forumPost.getCreatedAt()).isBetween(beforeCreation, afterCreation);
    }

    @Test
    @DisplayName("Should track last update time")
    void shouldTrackLastUpdateTime() {
        // Given
        forumPost = createValidForumPost();
        forumPost.onCreate();
        LocalDateTime beforeUpdate = LocalDateTime.now();

        // When
        forumPost.onUpdate(); // Simulate JPA @PreUpdate

        LocalDateTime afterUpdate = LocalDateTime.now();

        // Then
        assertThat(forumPost.getUpdatedAt()).isNotNull();
        assertThat(forumPost.getUpdatedAt()).isBetween(beforeUpdate, afterUpdate);
    }

    @Test
    @DisplayName("Should handle upvotes correctly")
    void shouldHandleUpvotesCorrectly() {
        // Given
        forumPost = createValidForumPost();

        // When
        forumPost.setUpvotes(5);

        // Then
        assertThat(forumPost.getUpvotes()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should handle downvotes correctly")
    void shouldHandleDownvotesCorrectly() {
        // Given
        forumPost = createValidForumPost();

        // When
        forumPost.setDownvotes(3);

        // Then
        assertThat(forumPost.getDownvotes()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should calculate net score correctly")
    void shouldCalculateNetScoreCorrectly() {
        // Given
        forumPost = createValidForumPost();
        forumPost.setUpvotes(10);
        forumPost.setDownvotes(3);

        // When
        int netScore = forumPost.getNetScore();

        // Then
        assertThat(netScore).isEqualTo(7);
    }

    @Test
    @DisplayName("Should handle locking correctly")
    void shouldHandleLockingCorrectly() {
        // Given
        forumPost = createValidForumPost();

        // When
        forumPost.setLocked(true);

        // Then
        assertThat(forumPost.isLocked()).isTrue();
    }

    @Test
    @DisplayName("Should handle pinning correctly")
    void shouldHandlePinningCorrectly() {
        // Given
        forumPost = createValidForumPost();

        // When
        forumPost.setPinned(true);

        // Then
        assertThat(forumPost.isPinned()).isTrue();
    }

    @Test
    @DisplayName("Should handle empty tags list")
    void shouldHandleEmptyTagsList() {
        // Given & When
        forumPost = ForumPost.builder()
                .userId(validUserId)
                .hiveId(validHiveId)
                .title(validTitle)
                .content(validContent)
                .tags(new ArrayList<>())
                .build();

        // Then
        assertThat(forumPost.getTags()).isNotNull();
        assertThat(forumPost.getTags()).isEmpty();
    }

    @Test
    @DisplayName("Should handle null tags gracefully")
    void shouldHandleNullTagsGracefully() {
        // Given & When
        forumPost = ForumPost.builder()
                .userId(validUserId)
                .hiveId(validHiveId)
                .title(validTitle)
                .content(validContent)
                .tags(null)
                .build();

        // Then
        assertThat(forumPost.getTags()).isNull();
    }

    @Test
    @DisplayName("Should maintain immutable tags list")
    void shouldMaintainImmutableTagsList() {
        // Given
        List<String> originalTags = new ArrayList<>(validTags);
        forumPost = createValidForumPost();

        // When
        List<String> retrievedTags = forumPost.getTags();

        // Then
        assertThat(retrievedTags).isEqualTo(originalTags);
        // Verify original list remains unchanged
        assertThat(validTags).isEqualTo(originalTags);
    }

    private ForumPost createValidForumPost() {
        return ForumPost.builder()
                .userId(validUserId)
                .hiveId(validHiveId)
                .title(validTitle)
                .content(validContent)
                .tags(validTags)
                .build();
    }
}