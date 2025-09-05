package com.focushive.forum.service.impl;

import com.focushive.forum.dto.*;
import com.focushive.forum.entity.*;
import com.focushive.forum.entity.ForumSubscription.NotificationType;
import com.focushive.forum.repository.*;
import com.focushive.forum.service.ForumService;
import com.focushive.user.entity.User;
import com.focushive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.features.forum.enabled", havingValue = "true", matchIfMissing = false)
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ForumServiceImpl implements ForumService {
    
    private final ForumCategoryRepository categoryRepository;
    private final ForumPostRepository postRepository;
    private final ForumReplyRepository replyRepository;
    private final ForumVoteRepository voteRepository;
    private final ForumSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    
    // Category Management
    
    @Override
    public ForumCategoryDTO createCategory(ForumCategoryDTO dto) {
        log.info("Creating forum category: {}", dto.getName());
        
        ForumCategory category = ForumCategory.builder()
            .name(dto.getName())
            .description(dto.getDescription())
            .icon(dto.getIcon())
            .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
            .isActive(true)
            .build();
        
        if (dto.getParentId() != null) {
            ForumCategory parent = categoryRepository.findById(dto.getParentId())
                .orElseThrow(() -> new EntityNotFoundException("Parent category not found"));
            category.setParent(parent);
        }
        
        category = categoryRepository.save(category);
        return mapToCategoryDTO(category);
    }
    
    @Override
    public ForumCategoryDTO updateCategory(String categoryId, ForumCategoryDTO dto) {
        log.info("Updating forum category: {}", categoryId);
        
        ForumCategory category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setIcon(dto.getIcon());
        category.setSortOrder(dto.getSortOrder());
        category.setIsActive(dto.getIsActive());
        
        category = categoryRepository.save(category);
        return mapToCategoryDTO(category);
    }
    
    @Override
    public void deleteCategory(String categoryId) {
        log.info("Deleting forum category: {}", categoryId);
        
        ForumCategory category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        
        // Soft delete by deactivating
        category.setIsActive(false);
        categoryRepository.save(category);
    }
    
    @Override
    public ForumCategoryDTO getCategory(String categoryId) {
        ForumCategory category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        return mapToCategoryDTO(category);
    }
    
    @Override
    public ForumCategoryDTO getCategoryBySlug(String slug) {
        ForumCategory category = categoryRepository.findBySlug(slug)
            .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        return mapToCategoryDTO(category);
    }
    
    @Override
    public List<ForumCategoryDTO> getAllCategories() {
        List<ForumCategory> categories = categoryRepository.findByIsActiveOrderBySortOrderAsc(true);
        return categories.stream().map(this::mapToCategoryDTO).collect(Collectors.toList());
    }
    
    @Override
    public List<ForumCategoryDTO> getGlobalCategories() {
        List<ForumCategory> categories = categoryRepository.findGlobalCategories();
        return categories.stream().map(this::mapToCategoryDTO).collect(Collectors.toList());
    }
    
    @Override
    public List<ForumCategoryDTO> getHiveCategories(String hiveId) {
        List<ForumCategory> categories = categoryRepository.findCategoriesForHive(hiveId);
        return categories.stream().map(this::mapToCategoryDTO).collect(Collectors.toList());
    }
    
    @Override
    public List<ForumCategoryDTO> getRootCategories() {
        List<ForumCategory> categories = categoryRepository.findRootCategoriesWithChildren();
        return categories.stream().map(this::mapToCategoryDTO).collect(Collectors.toList());
    }
    
    @Override
    public List<ForumCategoryDTO> getChildCategories(String parentId) {
        List<ForumCategory> categories = categoryRepository.findByParentIdOrderBySortOrderAsc(parentId);
        return categories.stream().map(this::mapToCategoryDTO).collect(Collectors.toList());
    }
    
    // Post Management
    
    @Override
    public ForumPostDTO createPost(String userId, ForumPostDTO dto) {
        log.info("Creating forum post by user {}: {}", userId, dto.getTitle());
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        ForumCategory category = categoryRepository.findById(dto.getCategoryId())
            .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        
        ForumPost post = ForumPost.builder()
            .category(category)
            .user(user)
            .title(dto.getTitle())
            .content(dto.getContent())
            .contentHtml(dto.getContentHtml())
            .tags(dto.getTags() != null ? dto.getTags().toArray(new String[0]) : null)
            .viewCount(0)
            .replyCount(0)
            .voteScore(0)
            .isPinned(false)
            .isLocked(false)
            .isDeleted(false)
            .build();
        
        post = postRepository.save(post);
        
        // Auto-subscribe creator to their post
        subscribeToPost(post.getId().toString(), userId, ForumSubscriptionDTO.builder()
            .notificationType(NotificationType.ALL)
            .emailNotifications(true)
            .inAppNotifications(true)
            .build());
        
        // Send notifications
        notifyNewPost(post.getId().toString());
        
        return mapToPostDTO(post, userId);
    }
    
    @Override
    public ForumPostDTO updatePost(String postId, String userId, ForumPostDTO dto) {
        log.info("Updating forum post {} by user {}", postId, userId);
        
        ForumPost post = postRepository.findById(postId)
            .orElseThrow(() -> new EntityNotFoundException("Post not found"));
        
        if (!post.canEdit(userId)) {
            throw new IllegalStateException("User cannot edit this post");
        }
        
        User editor = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setContentHtml(dto.getContentHtml());
        post.setTags(dto.getTags() != null ? dto.getTags().toArray(new String[0]) : null);
        post.setEditedAt(LocalDateTime.now());
        post.setEditedBy(editor);
        
        post = postRepository.save(post);
        return mapToPostDTO(post, userId);
    }
    
    @Override
    public void deletePost(String postId, String userId) {
        log.info("Deleting forum post {} by user {}", postId, userId);
        
        ForumPost post = postRepository.findById(postId)
            .orElseThrow(() -> new EntityNotFoundException("Post not found"));
        
        if (!post.getUser().getId().equals(userId)) {
            throw new IllegalStateException("User cannot delete this post");
        }
        
        post.setIsDeleted(true);
        postRepository.save(post);
    }
    
    @Override
    public ForumPostDTO getPost(String postId) {
        ForumPost post = postRepository.findById(postId)
            .orElseThrow(() -> new EntityNotFoundException("Post not found"));
        
        if (post.getIsDeleted()) {
            throw new EntityNotFoundException("Post not found");
        }
        
        return mapToPostDTO(post, null);
    }
    
    @Override
    public ForumPostDTO getPostBySlug(String categoryId, String slug) {
        ForumPost post = postRepository.findByCategoryIdAndSlug(categoryId, slug)
            .orElseThrow(() -> new EntityNotFoundException("Post not found"));
        return mapToPostDTO(post, null);
    }
    
    @Override
    public Page<ForumPostDTO> getPostsByCategory(String categoryId, Pageable pageable) {
        Page<ForumPost> posts = postRepository.findByCategoryId(categoryId, pageable);
        return posts.map(post -> mapToPostDTO(post, null));
    }
    
    @Override
    public Page<ForumPostDTO> getPostsByUser(String userId, Pageable pageable) {
        Page<ForumPost> posts = postRepository.findByUserId(userId, pageable);
        return posts.map(post -> mapToPostDTO(post, userId));
    }
    
    @Override
    public Page<ForumPostDTO> getPostsByHive(String hiveId, Pageable pageable) {
        Page<ForumPost> posts = postRepository.findByHiveId(hiveId, pageable);
        return posts.map(post -> mapToPostDTO(post, null));
    }
    
    @Override
    public Page<ForumPostDTO> searchPosts(String searchTerm, Pageable pageable) {
        Page<ForumPost> posts = postRepository.searchPosts(searchTerm, pageable);
        return posts.map(post -> mapToPostDTO(post, null));
    }
    
    @Override
    public Page<ForumPostDTO> getPostsByTag(String tag, Pageable pageable) {
        Page<ForumPost> posts = postRepository.findByTag(tag, pageable);
        return posts.map(post -> mapToPostDTO(post, null));
    }
    
    @Override
    public List<ForumPostDTO> getTrendingPosts(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        List<ForumPost> posts = postRepository.findTopPostsSince(since, PageRequest.of(0, limit));
        return posts.stream().map(post -> mapToPostDTO(post, null)).collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void incrementViewCount(String postId) {
        postRepository.incrementViewCount(postId);
    }
    
    @Override
    public void togglePinPost(String postId, String userId) {
        ForumPost post = postRepository.findById(postId)
            .orElseThrow(() -> new EntityNotFoundException("Post not found"));
        
        // TODO: Check admin/moderator permissions
        
        post.setIsPinned(!post.getIsPinned());
        postRepository.save(post);
    }
    
    @Override
    public void toggleLockPost(String postId, String userId) {
        ForumPost post = postRepository.findById(postId)
            .orElseThrow(() -> new EntityNotFoundException("Post not found"));
        
        // TODO: Check admin/moderator permissions or post owner
        
        post.setIsLocked(!post.getIsLocked());
        postRepository.save(post);
    }
    
    // Reply Management
    
    @Override
    public ForumReplyDTO createReply(String postId, String userId, ForumReplyDTO dto) {
        log.info("Creating reply to post {} by user {}", postId, userId);
        
        ForumPost post = postRepository.findById(postId)
            .orElseThrow(() -> new EntityNotFoundException("Post not found"));
        
        if (!post.canReply()) {
            throw new IllegalStateException("Cannot reply to this post");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        ForumReply reply = ForumReply.builder()
            .post(post)
            .user(user)
            .content(dto.getContent())
            .contentHtml(dto.getContentHtml())
            .voteScore(0)
            .isAccepted(false)
            .isDeleted(false)
            .build();
        
        if (dto.getParentReplyId() != null) {
            ForumReply parent = replyRepository.findById(dto.getParentReplyId())
                .orElseThrow(() -> new EntityNotFoundException("Parent reply not found"));
            reply.setParentReply(parent);
        }
        
        reply = replyRepository.save(reply);
        
        // Update post reply count
        post.incrementReplyCount();
        postRepository.save(post);
        
        // Send notifications
        notifyNewReply(reply.getId().toString());
        
        return mapToReplyDTO(reply, userId);
    }
    
    @Override
    public ForumReplyDTO updateReply(String replyId, String userId, ForumReplyDTO dto) {
        log.info("Updating reply {} by user {}", replyId, userId);
        
        ForumReply reply = replyRepository.findById(replyId)
            .orElseThrow(() -> new EntityNotFoundException("Reply not found"));
        
        if (!reply.canEdit(userId)) {
            throw new IllegalStateException("User cannot edit this reply");
        }
        
        User editor = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        reply.setContent(dto.getContent());
        reply.setContentHtml(dto.getContentHtml());
        reply.setEditedAt(LocalDateTime.now());
        reply.setEditedBy(editor);
        
        reply = replyRepository.save(reply);
        return mapToReplyDTO(reply, userId);
    }
    
    @Override
    public void deleteReply(String replyId, String userId) {
        log.info("Deleting reply {} by user {}", replyId, userId);
        
        ForumReply reply = replyRepository.findById(replyId)
            .orElseThrow(() -> new EntityNotFoundException("Reply not found"));
        
        if (!reply.getUser().getId().equals(userId)) {
            throw new IllegalStateException("User cannot delete this reply");
        }
        
        reply.setIsDeleted(true);
        replyRepository.save(reply);
        
        // Update post reply count
        ForumPost post = reply.getPost();
        post.decrementReplyCount();
        postRepository.save(post);
    }
    
    @Override
    public ForumReplyDTO getReply(String replyId) {
        ForumReply reply = replyRepository.findById(replyId)
            .orElseThrow(() -> new EntityNotFoundException("Reply not found"));
        
        if (reply.getIsDeleted()) {
            throw new EntityNotFoundException("Reply not found");
        }
        
        return mapToReplyDTO(reply, null);
    }
    
    @Override
    public List<ForumReplyDTO> getPostReplies(String postId) {
        List<ForumReply> replies = replyRepository.findByPostId(postId);
        return buildReplyTree(replies, null);
    }
    
    @Override
    public List<ForumReplyDTO> getTopLevelReplies(String postId) {
        List<ForumReply> replies = replyRepository.findTopLevelRepliesByPostId(postId);
        return replies.stream()
            .map(reply -> mapToReplyDTO(reply, null))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ForumReplyDTO> getChildReplies(String parentReplyId) {
        List<ForumReply> replies = replyRepository.findChildReplies(parentReplyId);
        return replies.stream()
            .map(reply -> mapToReplyDTO(reply, null))
            .collect(Collectors.toList());
    }
    
    @Override
    public Page<ForumReplyDTO> getUserReplies(String userId, Pageable pageable) {
        Page<ForumReply> replies = replyRepository.findByUserId(userId, pageable);
        return replies.map(reply -> mapToReplyDTO(reply, userId));
    }
    
    @Override
    public void acceptReply(String replyId, String userId) {
        ForumReply reply = replyRepository.findById(replyId)
            .orElseThrow(() -> new EntityNotFoundException("Reply not found"));
        
        ForumPost post = reply.getPost();
        if (!post.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Only post author can accept answers");
        }
        
        reply.markAsAccepted();
        replyRepository.save(reply);
        
        // Notify the reply author
        notifyReplyAccepted(replyId);
    }
    
    @Override
    public void unacceptReply(String replyId, String userId) {
        ForumReply reply = replyRepository.findById(replyId)
            .orElseThrow(() -> new EntityNotFoundException("Reply not found"));
        
        ForumPost post = reply.getPost();
        if (!post.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Only post author can unaccept answers");
        }
        
        reply.setIsAccepted(false);
        replyRepository.save(reply);
    }
    
    // Voting
    
    @Override
    public ForumVoteDTO voteOnPost(String postId, String userId, Integer voteType) {
        log.info("User {} voting {} on post {}", userId, voteType, postId);
        
        ForumPost post = postRepository.findById(postId)
            .orElseThrow(() -> new EntityNotFoundException("Post not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        // Check if user already voted
        Optional<ForumVote> existingVote = voteRepository.findByUserIdAndPostId(userId, postId);
        
        ForumVote vote;
        int scoreChange = voteType;
        
        if (existingVote.isPresent()) {
            vote = existingVote.get();
            scoreChange = voteType - vote.getVoteType(); // Calculate the change
            vote.setVoteType(voteType);
        } else {
            vote = ForumVote.builder()
                .user(user)
                .post(post)
                .voteType(voteType)
                .build();
        }
        
        vote = voteRepository.save(vote);
        
        // Update post score
        post.updateVoteScore(scoreChange);
        postRepository.save(post);
        
        return mapToVoteDTO(vote);
    }
    
    @Override
    public ForumVoteDTO voteOnReply(String replyId, String userId, Integer voteType) {
        log.info("User {} voting {} on reply {}", userId, voteType, replyId);
        
        ForumReply reply = replyRepository.findById(replyId)
            .orElseThrow(() -> new EntityNotFoundException("Reply not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        // Check if user already voted
        Optional<ForumVote> existingVote = voteRepository.findByUserIdAndReplyId(userId, replyId);
        
        ForumVote vote;
        int scoreChange = voteType;
        
        if (existingVote.isPresent()) {
            vote = existingVote.get();
            scoreChange = voteType - vote.getVoteType();
            vote.setVoteType(voteType);
        } else {
            vote = ForumVote.builder()
                .user(user)
                .reply(reply)
                .voteType(voteType)
                .build();
        }
        
        vote = voteRepository.save(vote);
        
        // Update reply score
        reply.updateVoteScore(scoreChange);
        replyRepository.save(reply);
        
        return mapToVoteDTO(vote);
    }
    
    @Override
    public void removeVote(String postId, String replyId, String userId) {
        if (postId != null) {
            Optional<ForumVote> vote = voteRepository.findByUserIdAndPostId(userId, postId);
            if (vote.isPresent()) {
                ForumPost post = vote.get().getPost();
                post.updateVoteScore(-vote.get().getVoteType());
                postRepository.save(post);
                voteRepository.delete(vote.get());
            }
        } else if (replyId != null) {
            Optional<ForumVote> vote = voteRepository.findByUserIdAndReplyId(userId, replyId);
            if (vote.isPresent()) {
                ForumReply reply = vote.get().getReply();
                reply.updateVoteScore(-vote.get().getVoteType());
                replyRepository.save(reply);
                voteRepository.delete(vote.get());
            }
        }
    }
    
    @Override
    public Integer getUserVoteOnPost(String postId, String userId) {
        Optional<ForumVote> vote = voteRepository.findByUserIdAndPostId(userId, postId);
        return vote.map(ForumVote::getVoteType).orElse(0);
    }
    
    @Override
    public Integer getUserVoteOnReply(String replyId, String userId) {
        Optional<ForumVote> vote = voteRepository.findByUserIdAndReplyId(userId, replyId);
        return vote.map(ForumVote::getVoteType).orElse(0);
    }
    
    // Subscriptions
    
    @Override
    public ForumSubscriptionDTO subscribeToPost(String postId, String userId, ForumSubscriptionDTO dto) {
        ForumPost post = postRepository.findById(postId)
            .orElseThrow(() -> new EntityNotFoundException("Post not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        Optional<ForumSubscription> existing = subscriptionRepository.findByUserIdAndPostId(userId, postId);
        
        ForumSubscription subscription;
        if (existing.isPresent()) {
            subscription = existing.get();
            subscription.setNotificationType(dto.getNotificationType());
            subscription.setEmailNotifications(dto.getEmailNotifications());
            subscription.setInAppNotifications(dto.getInAppNotifications());
        } else {
            subscription = ForumSubscription.builder()
                .user(user)
                .post(post)
                .notificationType(dto.getNotificationType())
                .emailNotifications(dto.getEmailNotifications() != null ? dto.getEmailNotifications() : true)
                .inAppNotifications(dto.getInAppNotifications() != null ? dto.getInAppNotifications() : true)
                .isMuted(false)
                .build();
        }
        
        subscription = subscriptionRepository.save(subscription);
        return mapToSubscriptionDTO(subscription);
    }
    
    @Override
    public ForumSubscriptionDTO subscribeToCategory(String categoryId, String userId, ForumSubscriptionDTO dto) {
        ForumCategory category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        Optional<ForumSubscription> existing = subscriptionRepository.findByUserIdAndCategoryId(userId, categoryId);
        
        ForumSubscription subscription;
        if (existing.isPresent()) {
            subscription = existing.get();
            subscription.setNotificationType(dto.getNotificationType());
            subscription.setEmailNotifications(dto.getEmailNotifications());
            subscription.setInAppNotifications(dto.getInAppNotifications());
        } else {
            subscription = ForumSubscription.builder()
                .user(user)
                .category(category)
                .notificationType(dto.getNotificationType())
                .emailNotifications(dto.getEmailNotifications() != null ? dto.getEmailNotifications() : true)
                .inAppNotifications(dto.getInAppNotifications() != null ? dto.getInAppNotifications() : true)
                .isMuted(false)
                .build();
        }
        
        subscription = subscriptionRepository.save(subscription);
        return mapToSubscriptionDTO(subscription);
    }
    
    @Override
    public void unsubscribeFromPost(String postId, String userId) {
        Optional<ForumSubscription> subscription = subscriptionRepository.findByUserIdAndPostId(userId, postId);
        subscription.ifPresent(subscriptionRepository::delete);
    }
    
    @Override
    public void unsubscribeFromCategory(String categoryId, String userId) {
        Optional<ForumSubscription> subscription = subscriptionRepository.findByUserIdAndCategoryId(userId, categoryId);
        subscription.ifPresent(subscriptionRepository::delete);
    }
    
    @Override
    public ForumSubscriptionDTO updateSubscription(String subscriptionId, ForumSubscriptionDTO dto) {
        ForumSubscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
        
        subscription.setNotificationType(dto.getNotificationType());
        subscription.setEmailNotifications(dto.getEmailNotifications());
        subscription.setInAppNotifications(dto.getInAppNotifications());
        
        subscription = subscriptionRepository.save(subscription);
        return mapToSubscriptionDTO(subscription);
    }
    
    @Override
    public List<ForumSubscriptionDTO> getUserSubscriptions(String userId) {
        List<ForumSubscription> subscriptions = subscriptionRepository.findByUserId(userId);
        return subscriptions.stream()
            .map(this::mapToSubscriptionDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public void muteSubscription(String subscriptionId, Integer hours) {
        ForumSubscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
        
        subscription.muteTemporarily(hours);
        subscriptionRepository.save(subscription);
    }
    
    @Override
    public void unmuteSubscription(String subscriptionId) {
        ForumSubscription subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
        
        subscription.unmute();
        subscriptionRepository.save(subscription);
    }
    
    // Statistics
    
    @Override
    public Long getPostCount(String categoryId) {
        return categoryRepository.countPostsInCategory(categoryId);
    }
    
    @Override
    public Long getUserPostCount(String userId, int days) {
        LocalDateTime since = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        return postRepository.countUserPostsSince(userId, since);
    }
    
    @Override
    public Long getUserReplyCount(String userId, int days) {
        LocalDateTime since = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        return replyRepository.countUserRepliesSince(userId, since);
    }
    
    @Override
    public Long getUserAcceptedReplyCount(String userId) {
        return replyRepository.countAcceptedRepliesByUser(userId);
    }
    
    @Override
    public List<String> getPopularTags(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        List<Object[]> results = postRepository.findPopularTags(since, PageRequest.of(0, limit));
        return results.stream()
            .map(result -> (String) result[0])
            .collect(Collectors.toList());
    }
    
    // Notifications
    
    @Override
    public void notifyNewPost(String postId) {
        // TODO: Implement WebSocket notification
        log.info("Notifying subscribers of new post: {}", postId);
    }
    
    @Override
    public void notifyNewReply(String replyId) {
        // TODO: Implement WebSocket notification
        log.info("Notifying subscribers of new reply: {}", replyId);
    }
    
    @Override
    public void notifyReplyAccepted(String replyId) {
        // TODO: Implement WebSocket notification
        log.info("Notifying reply author of acceptance: {}", replyId);
    }
    
    @Override
    public void notifyMention(String userId, String postId, String replyId) {
        // TODO: Implement WebSocket notification
        log.info("Notifying user {} of mention", userId);
    }
    
    // Helper methods
    
    private ForumCategoryDTO mapToCategoryDTO(ForumCategory category) {
        ForumCategoryDTO dto = ForumCategoryDTO.builder()
            .id(category.getId().toString())
            .name(category.getName())
            .description(category.getDescription())
            .slug(category.getSlug())
            .parentId(category.getParent() != null ? category.getParent().getId().toString() : null)
            .hiveId(category.getHive() != null ? category.getHive().getId().toString() : null)
            .icon(category.getIcon())
            .sortOrder(category.getSortOrder())
            .isActive(category.getIsActive())
            .postCount(category.getPostCount())
            .createdAt(category.getCreatedAt())
            .updatedAt(category.getUpdatedAt())
            .build();
        
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            dto.setChildren(category.getChildren().stream()
                .map(this::mapToCategoryDTO)
                .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    private ForumPostDTO mapToPostDTO(ForumPost post, String currentUserId) {
        ForumPostDTO dto = ForumPostDTO.builder()
            .id(post.getId().toString())
            .categoryId(post.getCategory().getId().toString())
            .categoryName(post.getCategory().getName())
            .userId(post.getUser().getId())
            .username(post.getUser().getUsername())
            .title(post.getTitle())
            .content(post.getContent())
            .contentHtml(post.getContentHtml())
            .slug(post.getSlug())
            .tags(post.getTags() != null ? Arrays.asList(post.getTags()) : null)
            .viewCount(post.getViewCount())
            .replyCount(post.getReplyCount())
            .voteScore(post.getVoteScore())
            .isPinned(post.getIsPinned())
            .isLocked(post.getIsLocked())
            .isDeleted(post.getIsDeleted())
            .isEdited(post.isEdited())
            .editedAt(post.getEditedAt())
            .createdAt(post.getCreatedAt())
            .updatedAt(post.getUpdatedAt())
            .build();
        
        if (post.getEditedBy() != null) {
            dto.setEditedByUsername(post.getEditedBy().getUsername());
        }
        
        if (currentUserId != null) {
            dto.setUserVote(getUserVoteOnPost(post.getId().toString(), currentUserId));
            dto.setCanEdit(post.canEdit(currentUserId));
            dto.setCanReply(post.canReply());
        }
        
        // Check if post has accepted answer
        List<ForumReply> acceptedReplies = replyRepository.findAcceptedRepliesByPostId(post.getId());
        dto.setHasAcceptedAnswer(!acceptedReplies.isEmpty());
        
        return dto;
    }
    
    private ForumReplyDTO mapToReplyDTO(ForumReply reply, String currentUserId) {
        ForumReplyDTO dto = ForumReplyDTO.builder()
            .id(reply.getId().toString())
            .postId(reply.getPost().getId().toString())
            .parentReplyId(reply.getParentReply() != null ? reply.getParentReply().getId().toString() : null)
            .userId(reply.getUser().getId())
            .username(reply.getUser().getUsername())
            .content(reply.getContent())
            .contentHtml(reply.getContentHtml())
            .voteScore(reply.getVoteScore())
            .isAccepted(reply.getIsAccepted())
            .isDeleted(reply.getIsDeleted())
            .isEdited(reply.isEdited())
            .editedAt(reply.getEditedAt())
            .createdAt(reply.getCreatedAt())
            .updatedAt(reply.getUpdatedAt())
            .depth(reply.getDepth())
            .build();
        
        if (reply.getEditedBy() != null) {
            dto.setEditedByUsername(reply.getEditedBy().getUsername());
        }
        
        if (currentUserId != null) {
            dto.setUserVote(getUserVoteOnReply(reply.getId().toString(), currentUserId));
            dto.setCanEdit(reply.canEdit(currentUserId));
            dto.setCanAccept(reply.isTopLevel() && reply.getPost().getUser().getId().equals(currentUserId));
        }
        
        return dto;
    }
    
    private ForumVoteDTO mapToVoteDTO(ForumVote vote) {
        return ForumVoteDTO.builder()
            .id(vote.getId().toString())
            .userId(vote.getUser().getId())
            .username(vote.getUser().getUsername())
            .postId(vote.getPost() != null ? vote.getPost().getId().toString() : null)
            .replyId(vote.getReply() != null ? vote.getReply().getId().toString() : null)
            .voteType(vote.getVoteType())
            .createdAt(vote.getCreatedAt())
            .build();
    }
    
    private ForumSubscriptionDTO mapToSubscriptionDTO(ForumSubscription subscription) {
        ForumSubscriptionDTO dto = ForumSubscriptionDTO.builder()
            .id(subscription.getId().toString())
            .userId(subscription.getUser().getId())
            .notificationType(subscription.getNotificationType())
            .emailNotifications(subscription.getEmailNotifications())
            .inAppNotifications(subscription.getInAppNotifications())
            .isMuted(subscription.getIsMuted())
            .mutedUntil(subscription.getMutedUntil())
            .createdAt(subscription.getCreatedAt())
            .updatedAt(subscription.getUpdatedAt())
            .build();
        
        if (subscription.getPost() != null) {
            dto.setPostId(subscription.getPost().getId().toString());
            dto.setPostTitle(subscription.getPost().getTitle());
        }
        
        if (subscription.getCategory() != null) {
            dto.setCategoryId(subscription.getCategory().getId().toString());
            dto.setCategoryName(subscription.getCategory().getName());
        }
        
        return dto;
    }
    
    private List<ForumReplyDTO> buildReplyTree(List<ForumReply> replies, String currentUserId) {
        Map<String, ForumReplyDTO> replyMap = new HashMap<>();
        List<ForumReplyDTO> rootReplies = new ArrayList<>();
        
        // First pass: create all DTOs
        for (ForumReply reply : replies) {
            ForumReplyDTO dto = mapToReplyDTO(reply, currentUserId);
            replyMap.put(reply.getId().toString(), dto);
        }
        
        // Second pass: build tree structure
        for (ForumReply reply : replies) {
            ForumReplyDTO dto = replyMap.get(reply.getId().toString());
            if (reply.getParentReply() == null) {
                rootReplies.add(dto);
            } else {
                ForumReplyDTO parent = replyMap.get(reply.getParentReply().getId().toString());
                if (parent != null) {
                    if (parent.getChildReplies() == null) {
                        parent.setChildReplies(new ArrayList<>());
                    }
                    parent.getChildReplies().add(dto);
                }
            }
        }
        
        return rootReplies;
    }
}