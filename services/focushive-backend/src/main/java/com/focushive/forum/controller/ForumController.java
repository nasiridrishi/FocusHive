package com.focushive.forum.controller;

import com.focushive.forum.dto.*;
import com.focushive.forum.service.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Forum functionality.
 * Provides endpoints for managing forum categories, posts, replies, and voting.
 */
@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Forum", description = "Forum management API for community discussions")
public class ForumController {

    private final ForumService forumService;

    // CATEGORY ENDPOINTS

    /**
     * Get all forum categories.
     */
    @GetMapping("/categories")
    @Operation(summary = "Get all categories", description = "Retrieve all forum categories")
    public ResponseEntity<List<ForumCategoryDTO>> getAllCategories() {
        log.debug("Getting all forum categories");
        List<ForumCategoryDTO> categories = forumService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Get category by ID.
     */
    @GetMapping("/categories/{categoryId}")
    @Operation(summary = "Get category by ID", description = "Retrieve a specific forum category")
    public ResponseEntity<ForumCategoryDTO> getCategory(
            @PathVariable String categoryId) {
        log.debug("Getting forum category: {}", categoryId);
        ForumCategoryDTO category = forumService.getCategory(categoryId);
        return ResponseEntity.ok(category);
    }

    /**
     * Create new category.
     */
    @PostMapping("/categories")
    @Operation(summary = "Create category", description = "Create a new forum category")
    public ResponseEntity<ForumCategoryDTO> createCategory(
            @RequestBody @Valid ForumCategoryDTO category) {
        log.debug("Creating forum category: {}", category.getName());
        ForumCategoryDTO created = forumService.createCategory(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get categories for a specific hive.
     */
    @GetMapping("/hives/{hiveId}/categories")
    @Operation(summary = "Get hive categories", description = "Get all categories for a specific hive")
    public ResponseEntity<List<ForumCategoryDTO>> getHiveCategories(
            @PathVariable String hiveId) {
        log.debug("Getting forum categories for hive: {}", hiveId);
        List<ForumCategoryDTO> categories = forumService.getHiveCategories(hiveId);
        return ResponseEntity.ok(categories);
    }

    // POST ENDPOINTS

    /**
     * Get posts by category.
     */
    @GetMapping("/categories/{categoryId}/posts")
    @Operation(summary = "Get posts by category", description = "Get all posts in a specific category")
    public ResponseEntity<Page<ForumPostDTO>> getPostsByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.debug("Getting posts for category: {} (page: {}, size: {})", categoryId, page, size);

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ForumPostDTO> posts = forumService.getPostsByCategory(categoryId, pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get posts by hive.
     */
    @GetMapping("/hives/{hiveId}/posts")
    @Operation(summary = "Get posts by hive", description = "Get all posts in a specific hive")
    public ResponseEntity<Page<ForumPostDTO>> getPostsByHive(
            @PathVariable String hiveId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.debug("Getting posts for hive: {} (page: {}, size: {})", hiveId, page, size);

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ForumPostDTO> posts = forumService.getPostsByHive(hiveId, pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get post by ID.
     */
    @GetMapping("/posts/{postId}")
    @Operation(summary = "Get post by ID", description = "Retrieve a specific forum post")
    public ResponseEntity<ForumPostDTO> getPost(@PathVariable String postId) {
        log.debug("Getting forum post: {}", postId);
        ForumPostDTO post = forumService.getPost(postId);

        // Increment view count
        forumService.incrementViewCount(postId);

        return ResponseEntity.ok(post);
    }

    /**
     * Create new post.
     */
    @PostMapping("/posts")
    @Operation(summary = "Create post", description = "Create a new forum post")
    public ResponseEntity<ForumPostDTO> createPost(
            @RequestBody @Valid ForumPostDTO post,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Creating forum post: {} by user: {}", post.getTitle(), userDetails.getUsername());
        ForumPostDTO created = forumService.createPost(userDetails.getUsername(), post);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update existing post.
     */
    @PutMapping("/posts/{postId}")
    @Operation(summary = "Update post", description = "Update an existing forum post")
    public ResponseEntity<ForumPostDTO> updatePost(
            @PathVariable String postId,
            @RequestBody @Valid ForumPostDTO post,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Updating forum post: {} by user: {}", postId, userDetails.getUsername());
        ForumPostDTO updated = forumService.updatePost(postId, userDetails.getUsername(), post);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete post.
     */
    @DeleteMapping("/posts/{postId}")
    @Operation(summary = "Delete post", description = "Delete a forum post")
    public ResponseEntity<Void> deletePost(
            @PathVariable String postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Deleting forum post: {} by user: {}", postId, userDetails.getUsername());
        forumService.deletePost(postId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Pin/unpin post.
     */
    @PostMapping("/posts/{postId}/pin")
    @Operation(summary = "Toggle post pin", description = "Pin or unpin a forum post")
    public ResponseEntity<Void> togglePinPost(
            @PathVariable String postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Toggling pin for post: {} by user: {}", postId, userDetails.getUsername());
        forumService.togglePinPost(postId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * Lock/unlock post.
     */
    @PostMapping("/posts/{postId}/lock")
    @Operation(summary = "Toggle post lock", description = "Lock or unlock a forum post")
    public ResponseEntity<Void> toggleLockPost(
            @PathVariable String postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Toggling lock for post: {} by user: {}", postId, userDetails.getUsername());
        forumService.toggleLockPost(postId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    // REPLY ENDPOINTS

    /**
     * Get replies for a post.
     */
    @GetMapping("/posts/{postId}/replies")
    @Operation(summary = "Get post replies", description = "Get all replies for a specific post")
    public ResponseEntity<List<ForumReplyDTO>> getPostReplies(@PathVariable String postId) {
        log.debug("Getting replies for post: {}", postId);
        List<ForumReplyDTO> replies = forumService.getPostReplies(postId);
        return ResponseEntity.ok(replies);
    }

    /**
     * Create new reply.
     */
    @PostMapping("/posts/{postId}/replies")
    @Operation(summary = "Create reply", description = "Create a new reply to a forum post")
    public ResponseEntity<ForumReplyDTO> createReply(
            @PathVariable String postId,
            @RequestBody @Valid ForumReplyDTO reply,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Creating reply for post: {} by user: {}", postId, userDetails.getUsername());
        ForumReplyDTO created = forumService.createReply(postId, userDetails.getUsername(), reply);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update existing reply.
     */
    @PutMapping("/replies/{replyId}")
    @Operation(summary = "Update reply", description = "Update an existing forum reply")
    public ResponseEntity<ForumReplyDTO> updateReply(
            @PathVariable String replyId,
            @RequestBody @Valid ForumReplyDTO reply,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Updating reply: {} by user: {}", replyId, userDetails.getUsername());
        ForumReplyDTO updated = forumService.updateReply(replyId, userDetails.getUsername(), reply);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete reply.
     */
    @DeleteMapping("/replies/{replyId}")
    @Operation(summary = "Delete reply", description = "Delete a forum reply")
    public ResponseEntity<Void> deleteReply(
            @PathVariable String replyId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Deleting reply: {} by user: {}", replyId, userDetails.getUsername());
        forumService.deleteReply(replyId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // VOTING ENDPOINTS

    /**
     * Vote on a post.
     */
    @PostMapping("/posts/{postId}/vote")
    @Operation(summary = "Vote on post", description = "Upvote (1) or downvote (-1) a forum post")
    public ResponseEntity<ForumVoteDTO> voteOnPost(
            @PathVariable String postId,
            @RequestParam @Parameter(description = "Vote type: 1 for upvote, -1 for downvote") Integer voteType,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("User {} voting {} on post: {}", userDetails.getUsername(), voteType, postId);
        ForumVoteDTO vote = forumService.voteOnPost(postId, userDetails.getUsername(), voteType);
        return ResponseEntity.ok(vote);
    }

    /**
     * Vote on a reply.
     */
    @PostMapping("/replies/{replyId}/vote")
    @Operation(summary = "Vote on reply", description = "Upvote (1) or downvote (-1) a forum reply")
    public ResponseEntity<ForumVoteDTO> voteOnReply(
            @PathVariable String replyId,
            @RequestParam @Parameter(description = "Vote type: 1 for upvote, -1 for downvote") Integer voteType,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("User {} voting {} on reply: {}", userDetails.getUsername(), voteType, replyId);
        ForumVoteDTO vote = forumService.voteOnReply(replyId, userDetails.getUsername(), voteType);
        return ResponseEntity.ok(vote);
    }

    /**
     * Remove vote from post or reply.
     */
    @DeleteMapping("/posts/{postId}/vote")
    @Operation(summary = "Remove post vote", description = "Remove user's vote from a post")
    public ResponseEntity<Void> removePostVote(
            @PathVariable String postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("User {} removing vote from post: {}", userDetails.getUsername(), postId);
        forumService.removeVote(postId, null, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/replies/{replyId}/vote")
    @Operation(summary = "Remove reply vote", description = "Remove user's vote from a reply")
    public ResponseEntity<Void> removeReplyVote(
            @PathVariable String replyId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("User {} removing vote from reply: {}", userDetails.getUsername(), replyId);
        forumService.removeVote(null, replyId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // SEARCH ENDPOINTS

    /**
     * Search posts.
     */
    @GetMapping("/search")
    @Operation(summary = "Search posts", description = "Search forum posts by keyword")
    public ResponseEntity<Page<ForumPostDTO>> searchPosts(
            @RequestParam("q") String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.debug("Searching posts for: '{}' (page: {}, size: {})", searchTerm, page, size);

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ForumPostDTO> posts = forumService.searchPosts(searchTerm, pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get posts by tag.
     */
    @GetMapping("/posts/tag/{tag}")
    @Operation(summary = "Get posts by tag", description = "Get all posts with a specific tag")
    public ResponseEntity<Page<ForumPostDTO>> getPostsByTag(
            @PathVariable String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.debug("Getting posts with tag: '{}' (page: {}, size: {})", tag, page, size);

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ForumPostDTO> posts = forumService.getPostsByTag(tag, pageable);
        return ResponseEntity.ok(posts);
    }

    // STATISTICS ENDPOINTS

    /**
     * Get trending posts.
     */
    @GetMapping("/trending")
    @Operation(summary = "Get trending posts", description = "Get currently trending forum posts")
    public ResponseEntity<List<ForumPostDTO>> getTrendingPosts(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit) {

        log.debug("Getting trending posts for last {} days (limit: {})", days, limit);
        List<ForumPostDTO> posts = forumService.getTrendingPosts(days, limit);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get popular tags.
     */
    @GetMapping("/tags/popular")
    @Operation(summary = "Get popular tags", description = "Get most popular forum tags")
    public ResponseEntity<List<String>> getPopularTags(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "20") int limit) {

        log.debug("Getting popular tags for last {} days (limit: {})", days, limit);
        List<String> tags = forumService.getPopularTags(days, limit);
        return ResponseEntity.ok(tags);
    }
}