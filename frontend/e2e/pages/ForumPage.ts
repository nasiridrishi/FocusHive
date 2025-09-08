/**
 * Page Object Model for Forum System
 * Provides methods to interact with forum features in E2E tests
 */

import { Page, Locator, expect } from '@playwright/test';
import { TIMEOUTS } from '../helpers/test-data';

export class ForumPage {
  readonly page: Page;

  // Main navigation and layout
  readonly forumContainer: Locator;
  readonly navigationTabs: Locator;
  readonly homeTab: Locator;
  readonly categoriesTab: Locator;
  readonly recentTab: Locator;
  readonly popularTab: Locator;
  readonly searchTab: Locator;

  // Header elements
  readonly forumTitle: Locator;
  readonly createPostButton: Locator;
  readonly searchInput: Locator;
  readonly searchButton: Locator;
  readonly notificationBadge: Locator;
  readonly userProfileMenu: Locator;

  // Loading and error states
  readonly loadingSpinner: Locator;
  readonly errorAlert: Locator;
  readonly retryButton: Locator;
  readonly emptyStateMessage: Locator;

  // Forum statistics
  readonly statsContainer: Locator;
  readonly totalPostsCount: Locator;
  readonly totalUsersCount: Locator;
  readonly todayPostsCount: Locator;
  readonly totalCategoriesCount: Locator;

  // Category list
  readonly categoryList: Locator;
  readonly categoryCards: Locator;
  readonly categoryTitle: Locator;
  readonly categoryDescription: Locator;
  readonly categoryPostCount: Locator;
  readonly categoryTopicCount: Locator;
  readonly categoryLastActivity: Locator;

  // Post list
  readonly postList: Locator;
  readonly postCards: Locator;
  readonly postTitle: Locator;
  readonly postAuthor: Locator;
  readonly postContent: Locator;
  readonly postCategory: Locator;
  readonly postTags: Locator;
  readonly postMetrics: Locator;
  readonly postRepliesCount: Locator;
  readonly postViewsCount: Locator;
  readonly postLikesCount: Locator;
  readonly postTimestamp: Locator;
  readonly pinnedPostBadge: Locator;
  readonly lockedPostBadge: Locator;

  // Post creation form
  readonly createPostDialog: Locator;
  readonly postTitleInput: Locator;
  readonly postContentEditor: Locator;
  readonly postCategorySelect: Locator;
  readonly postTagsInput: Locator;
  readonly attachmentUpload: Locator;
  readonly attachmentList: Locator;
  readonly postPreviewButton: Locator;
  readonly postPublishButton: Locator;
  readonly postSaveDraftButton: Locator;
  readonly postCancelButton: Locator;

  // Rich text editor
  readonly editorToolbar: Locator;
  readonly boldButton: Locator;
  readonly italicButton: Locator;
  readonly underlineButton: Locator;
  readonly linkButton: Locator;
  readonly codeButton: Locator;
  readonly imageButton: Locator;
  readonly listButton: Locator;
  readonly quoteButton: Locator;
  readonly emojiButton: Locator;
  readonly markdownToggle: Locator;

  // Post view page
  readonly postViewContainer: Locator;
  readonly postViewTitle: Locator;
  readonly postViewContent: Locator;
  readonly postViewAuthor: Locator;
  readonly postViewMetadata: Locator;
  readonly postViewAttachments: Locator;
  readonly postViewActions: Locator;
  readonly likePostButton: Locator;
  readonly sharePostButton: Locator;
  readonly reportPostButton: Locator;
  readonly editPostButton: Locator;
  readonly deletePostButton: Locator;

  // Reply system
  readonly replySection: Locator;
  readonly replyList: Locator;
  readonly replyItems: Locator;
  readonly replyAuthor: Locator;
  readonly replyContent: Locator;
  readonly replyTimestamp: Locator;
  readonly replyActions: Locator;
  readonly replyLikeButton: Locator;
  readonly replyQuoteButton: Locator;
  readonly replyEditButton: Locator;
  readonly replyDeleteButton: Locator;
  readonly nestedReplies: Locator;

  // Reply creation form
  readonly replyForm: Locator;
  readonly replyEditor: Locator;
  readonly replySubmitButton: Locator;
  readonly replyCancelButton: Locator;
  readonly replyAttachmentUpload: Locator;
  readonly replyToIndicator: Locator;

  // Search functionality
  readonly searchResults: Locator;
  readonly searchFilters: Locator;
  readonly searchCategoryFilter: Locator;
  readonly searchDateFilter: Locator;
  readonly searchAuthorFilter: Locator;
  readonly searchSortSelect: Locator;
  readonly searchResultsCount: Locator;
  readonly searchSuggestions: Locator;

  // User profiles and reputation
  readonly userProfileCard: Locator;
  readonly userAvatar: Locator;
  readonly userName: Locator;
  readonly userRole: Locator;
  readonly userReputation: Locator;
  readonly userBadges: Locator;
  readonly userPostCount: Locator;
  readonly userJoinDate: Locator;

  // Voting and rating system
  readonly upvoteButton: Locator;
  readonly downvoteButton: Locator;
  readonly voteCount: Locator;
  readonly bestAnswerButton: Locator;
  readonly bestAnswerBadge: Locator;

  // Moderation tools
  readonly moderationMenu: Locator;
  readonly hideContentButton: Locator;
  readonly lockThreadButton: Locator;
  readonly pinThreadButton: Locator;
  readonly moveThreadButton: Locator;
  readonly deleteContentButton: Locator;
  readonly banUserButton: Locator;
  readonly moderationReasonDialog: Locator;
  readonly moderationReasonInput: Locator;
  readonly confirmModerationButton: Locator;

  // Notifications
  readonly notificationPanel: Locator;
  readonly notificationList: Locator;
  readonly notificationItems: Locator;
  readonly markAllReadButton: Locator;
  readonly clearNotificationsButton: Locator;

  // Tags and categories
  readonly tagCloud: Locator;
  readonly popularTags: Locator;
  readonly tagFilter: Locator;
  readonly categoryNavigation: Locator;
  readonly subcategoryList: Locator;
  readonly breadcrumbNavigation: Locator;

  // Pagination
  readonly pagination: Locator;
  readonly previousPageButton: Locator;
  readonly nextPageButton: Locator;
  readonly pageNumbers: Locator;
  readonly currentPageIndicator: Locator;
  readonly resultsPerPageSelect: Locator;

  // Mobile responsive elements
  readonly mobileMenuToggle: Locator;
  readonly mobileSidebar: Locator;
  readonly mobileSearchToggle: Locator;
  readonly mobileCreatePostFab: Locator;

  constructor(page: Page) {
    this.page = page;

    // Main navigation and layout
    this.forumContainer = page.locator('[data-testid="forum-container"]');
    this.navigationTabs = page.locator('[data-testid="forum-navigation-tabs"]');
    this.homeTab = page.locator('[data-testid="forum-home-tab"]');
    this.categoriesTab = page.locator('[data-testid="forum-categories-tab"]');
    this.recentTab = page.locator('[data-testid="forum-recent-tab"]');
    this.popularTab = page.locator('[data-testid="forum-popular-tab"]');
    this.searchTab = page.locator('[data-testid="forum-search-tab"]');

    // Header elements
    this.forumTitle = page.locator('[data-testid="forum-title"]');
    this.createPostButton = page.locator('[data-testid="create-post-button"]');
    this.searchInput = page.locator('[data-testid="forum-search-input"]');
    this.searchButton = page.locator('[data-testid="forum-search-button"]');
    this.notificationBadge = page.locator('[data-testid="notification-badge"]');
    this.userProfileMenu = page.locator('[data-testid="user-profile-menu"]');

    // Loading and error states
    this.loadingSpinner = page.locator('[data-testid="loading-spinner"]');
    this.errorAlert = page.locator('[data-testid="error-alert"]');
    this.retryButton = page.locator('[data-testid="retry-button"]');
    this.emptyStateMessage = page.locator('[data-testid="empty-state-message"]');

    // Forum statistics
    this.statsContainer = page.locator('[data-testid="forum-stats"]');
    this.totalPostsCount = page.locator('[data-testid="total-posts-count"]');
    this.totalUsersCount = page.locator('[data-testid="total-users-count"]');
    this.todayPostsCount = page.locator('[data-testid="today-posts-count"]');
    this.totalCategoriesCount = page.locator('[data-testid="total-categories-count"]');

    // Category list
    this.categoryList = page.locator('[data-testid="category-list"]');
    this.categoryCards = page.locator('[data-testid="category-card"]');
    this.categoryTitle = page.locator('[data-testid="category-title"]');
    this.categoryDescription = page.locator('[data-testid="category-description"]');
    this.categoryPostCount = page.locator('[data-testid="category-post-count"]');
    this.categoryTopicCount = page.locator('[data-testid="category-topic-count"]');
    this.categoryLastActivity = page.locator('[data-testid="category-last-activity"]');

    // Post list
    this.postList = page.locator('[data-testid="post-list"]');
    this.postCards = page.locator('[data-testid="post-card"]');
    this.postTitle = page.locator('[data-testid="post-title"]');
    this.postAuthor = page.locator('[data-testid="post-author"]');
    this.postContent = page.locator('[data-testid="post-content"]');
    this.postCategory = page.locator('[data-testid="post-category"]');
    this.postTags = page.locator('[data-testid="post-tags"]');
    this.postMetrics = page.locator('[data-testid="post-metrics"]');
    this.postRepliesCount = page.locator('[data-testid="post-replies-count"]');
    this.postViewsCount = page.locator('[data-testid="post-views-count"]');
    this.postLikesCount = page.locator('[data-testid="post-likes-count"]');
    this.postTimestamp = page.locator('[data-testid="post-timestamp"]');
    this.pinnedPostBadge = page.locator('[data-testid="pinned-post-badge"]');
    this.lockedPostBadge = page.locator('[data-testid="locked-post-badge"]');

    // Post creation form
    this.createPostDialog = page.locator('[data-testid="create-post-dialog"]');
    this.postTitleInput = page.locator('[data-testid="post-title-input"]');
    this.postContentEditor = page.locator('[data-testid="post-content-editor"]');
    this.postCategorySelect = page.locator('[data-testid="post-category-select"]');
    this.postTagsInput = page.locator('[data-testid="post-tags-input"]');
    this.attachmentUpload = page.locator('[data-testid="attachment-upload"]');
    this.attachmentList = page.locator('[data-testid="attachment-list"]');
    this.postPreviewButton = page.locator('[data-testid="post-preview-button"]');
    this.postPublishButton = page.locator('[data-testid="post-publish-button"]');
    this.postSaveDraftButton = page.locator('[data-testid="post-save-draft-button"]');
    this.postCancelButton = page.locator('[data-testid="post-cancel-button"]');

    // Rich text editor
    this.editorToolbar = page.locator('[data-testid="editor-toolbar"]');
    this.boldButton = page.locator('[data-testid="editor-bold-button"]');
    this.italicButton = page.locator('[data-testid="editor-italic-button"]');
    this.underlineButton = page.locator('[data-testid="editor-underline-button"]');
    this.linkButton = page.locator('[data-testid="editor-link-button"]');
    this.codeButton = page.locator('[data-testid="editor-code-button"]');
    this.imageButton = page.locator('[data-testid="editor-image-button"]');
    this.listButton = page.locator('[data-testid="editor-list-button"]');
    this.quoteButton = page.locator('[data-testid="editor-quote-button"]');
    this.emojiButton = page.locator('[data-testid="editor-emoji-button"]');
    this.markdownToggle = page.locator('[data-testid="editor-markdown-toggle"]');

    // Post view page
    this.postViewContainer = page.locator('[data-testid="post-view-container"]');
    this.postViewTitle = page.locator('[data-testid="post-view-title"]');
    this.postViewContent = page.locator('[data-testid="post-view-content"]');
    this.postViewAuthor = page.locator('[data-testid="post-view-author"]');
    this.postViewMetadata = page.locator('[data-testid="post-view-metadata"]');
    this.postViewAttachments = page.locator('[data-testid="post-view-attachments"]');
    this.postViewActions = page.locator('[data-testid="post-view-actions"]');
    this.likePostButton = page.locator('[data-testid="like-post-button"]');
    this.sharePostButton = page.locator('[data-testid="share-post-button"]');
    this.reportPostButton = page.locator('[data-testid="report-post-button"]');
    this.editPostButton = page.locator('[data-testid="edit-post-button"]');
    this.deletePostButton = page.locator('[data-testid="delete-post-button"]');

    // Reply system
    this.replySection = page.locator('[data-testid="reply-section"]');
    this.replyList = page.locator('[data-testid="reply-list"]');
    this.replyItems = page.locator('[data-testid="reply-item"]');
    this.replyAuthor = page.locator('[data-testid="reply-author"]');
    this.replyContent = page.locator('[data-testid="reply-content"]');
    this.replyTimestamp = page.locator('[data-testid="reply-timestamp"]');
    this.replyActions = page.locator('[data-testid="reply-actions"]');
    this.replyLikeButton = page.locator('[data-testid="reply-like-button"]');
    this.replyQuoteButton = page.locator('[data-testid="reply-quote-button"]');
    this.replyEditButton = page.locator('[data-testid="reply-edit-button"]');
    this.replyDeleteButton = page.locator('[data-testid="reply-delete-button"]');
    this.nestedReplies = page.locator('[data-testid="nested-replies"]');

    // Reply creation form
    this.replyForm = page.locator('[data-testid="reply-form"]');
    this.replyEditor = page.locator('[data-testid="reply-editor"]');
    this.replySubmitButton = page.locator('[data-testid="reply-submit-button"]');
    this.replyCancelButton = page.locator('[data-testid="reply-cancel-button"]');
    this.replyAttachmentUpload = page.locator('[data-testid="reply-attachment-upload"]');
    this.replyToIndicator = page.locator('[data-testid="reply-to-indicator"]');

    // Search functionality
    this.searchResults = page.locator('[data-testid="search-results"]');
    this.searchFilters = page.locator('[data-testid="search-filters"]');
    this.searchCategoryFilter = page.locator('[data-testid="search-category-filter"]');
    this.searchDateFilter = page.locator('[data-testid="search-date-filter"]');
    this.searchAuthorFilter = page.locator('[data-testid="search-author-filter"]');
    this.searchSortSelect = page.locator('[data-testid="search-sort-select"]');
    this.searchResultsCount = page.locator('[data-testid="search-results-count"]');
    this.searchSuggestions = page.locator('[data-testid="search-suggestions"]');

    // User profiles and reputation
    this.userProfileCard = page.locator('[data-testid="user-profile-card"]');
    this.userAvatar = page.locator('[data-testid="user-avatar"]');
    this.userName = page.locator('[data-testid="user-name"]');
    this.userRole = page.locator('[data-testid="user-role"]');
    this.userReputation = page.locator('[data-testid="user-reputation"]');
    this.userBadges = page.locator('[data-testid="user-badges"]');
    this.userPostCount = page.locator('[data-testid="user-post-count"]');
    this.userJoinDate = page.locator('[data-testid="user-join-date"]');

    // Voting and rating system
    this.upvoteButton = page.locator('[data-testid="upvote-button"]');
    this.downvoteButton = page.locator('[data-testid="downvote-button"]');
    this.voteCount = page.locator('[data-testid="vote-count"]');
    this.bestAnswerButton = page.locator('[data-testid="best-answer-button"]');
    this.bestAnswerBadge = page.locator('[data-testid="best-answer-badge"]');

    // Moderation tools
    this.moderationMenu = page.locator('[data-testid="moderation-menu"]');
    this.hideContentButton = page.locator('[data-testid="hide-content-button"]');
    this.lockThreadButton = page.locator('[data-testid="lock-thread-button"]');
    this.pinThreadButton = page.locator('[data-testid="pin-thread-button"]');
    this.moveThreadButton = page.locator('[data-testid="move-thread-button"]');
    this.deleteContentButton = page.locator('[data-testid="delete-content-button"]');
    this.banUserButton = page.locator('[data-testid="ban-user-button"]');
    this.moderationReasonDialog = page.locator('[data-testid="moderation-reason-dialog"]');
    this.moderationReasonInput = page.locator('[data-testid="moderation-reason-input"]');
    this.confirmModerationButton = page.locator('[data-testid="confirm-moderation-button"]');

    // Notifications
    this.notificationPanel = page.locator('[data-testid="notification-panel"]');
    this.notificationList = page.locator('[data-testid="notification-list"]');
    this.notificationItems = page.locator('[data-testid="notification-item"]');
    this.markAllReadButton = page.locator('[data-testid="mark-all-read-button"]');
    this.clearNotificationsButton = page.locator('[data-testid="clear-notifications-button"]');

    // Tags and categories
    this.tagCloud = page.locator('[data-testid="tag-cloud"]');
    this.popularTags = page.locator('[data-testid="popular-tags"]');
    this.tagFilter = page.locator('[data-testid="tag-filter"]');
    this.categoryNavigation = page.locator('[data-testid="category-navigation"]');
    this.subcategoryList = page.locator('[data-testid="subcategory-list"]');
    this.breadcrumbNavigation = page.locator('[data-testid="breadcrumb-navigation"]');

    // Pagination
    this.pagination = page.locator('[data-testid="pagination"]');
    this.previousPageButton = page.locator('[data-testid="previous-page-button"]');
    this.nextPageButton = page.locator('[data-testid="next-page-button"]');
    this.pageNumbers = page.locator('[data-testid="page-numbers"]');
    this.currentPageIndicator = page.locator('[data-testid="current-page-indicator"]');
    this.resultsPerPageSelect = page.locator('[data-testid="results-per-page-select"]');

    // Mobile responsive elements
    this.mobileMenuToggle = page.locator('[data-testid="mobile-menu-toggle"]');
    this.mobileSidebar = page.locator('[data-testid="mobile-sidebar"]');
    this.mobileSearchToggle = page.locator('[data-testid="mobile-search-toggle"]');
    this.mobileCreatePostFab = page.locator('[data-testid="mobile-create-post-fab"]');
  }

  /**
   * Navigate to forum homepage
   */
  async navigateToForum(): Promise<void> {
    await this.page.goto('/forum');
    await this.waitForLoad();
  }

  /**
   * Wait for forum page to load
   */
  async waitForLoad(): Promise<void> {
    await this.forumContainer.waitFor({ timeout: TIMEOUTS.PAGE_LOAD });
  }

  /**
   * Navigate to specific category
   */
  async navigateToCategory(categoryId: number | string): Promise<void> {
    await this.page.goto(`/forum/categories/${categoryId}`);
    await this.waitForLoad();
  }

  /**
   * Navigate to specific post
   */
  async navigateToPost(postId: number | string): Promise<void> {
    await this.page.goto(`/forum/posts/${postId}`);
    await this.waitForLoad();
  }

  /**
   * Search for forum content
   */
  async searchContent(query: string): Promise<void> {
    await this.searchInput.fill(query);
    await this.searchButton.click();
    await this.searchResults.waitFor({ timeout: TIMEOUTS.SEARCH });
  }

  /**
   * Create a new post
   */
  async createPost(postData: {
    title: string;
    content: string;
    categoryId?: string;
    tags?: string[];
    attachments?: string[];
  }): Promise<void> {
    await this.createPostButton.click();
    await this.createPostDialog.waitFor();

    await this.postTitleInput.fill(postData.title);
    await this.fillRichTextEditor(this.postContentEditor, postData.content);

    if (postData.categoryId) {
      await this.postCategorySelect.click();
      await this.page.locator(`[data-value="${postData.categoryId}"]`).click();
    }

    if (postData.tags && postData.tags.length > 0) {
      await this.postTagsInput.fill(postData.tags.join(','));
    }

    if (postData.attachments && postData.attachments.length > 0) {
      for (const attachment of postData.attachments) {
        await this.attachmentUpload.setInputFiles(attachment);
      }
    }

    await this.postPublishButton.click();
    await this.createPostDialog.waitFor({ state: 'hidden' });
  }

  /**
   * Create a reply to a post
   */
  async replyToPost(content: string, parentReplyId?: string): Promise<void> {
    if (parentReplyId) {
      await this.page.locator(`[data-testid="reply-to-${parentReplyId}"]`).click();
    }

    await this.replyEditor.waitFor();
    await this.fillRichTextEditor(this.replyEditor, content);
    await this.replySubmitButton.click();

    // Wait for reply to be posted
    await this.page.waitForResponse(response => 
      response.url().includes('/api/forum/replies') && response.status() === 201
    );
  }

  /**
   * Like a post
   */
  async likePost(postId?: string): Promise<void> {
    const likeButton = postId 
      ? this.page.locator(`[data-testid="like-post-${postId}"]`)
      : this.likePostButton;
    
    await likeButton.click();
    await this.page.waitForResponse(response => 
      response.url().includes('/like') && response.status() === 200
    );
  }

  /**
   * Vote on content (upvote/downvote)
   */
  async vote(type: 'up' | 'down', contentId?: string): Promise<void> {
    const voteButton = contentId
      ? this.page.locator(`[data-testid="${type}vote-${contentId}"]`)
      : type === 'up' ? this.upvoteButton : this.downvoteButton;
    
    await voteButton.click();
    await this.page.waitForResponse(response => 
      response.url().includes('/vote') && response.status() === 200
    );
  }

  /**
   * Fill rich text editor with content
   */
  async fillRichTextEditor(editor: Locator, content: string): Promise<void> {
    await editor.click();
    await editor.fill(content);
  }

  /**
   * Apply rich text formatting
   */
  async applyFormatting(type: 'bold' | 'italic' | 'underline' | 'code' | 'link' | 'quote'): Promise<void> {
    const buttonMap = {
      bold: this.boldButton,
      italic: this.italicButton,
      underline: this.underlineButton,
      code: this.codeButton,
      link: this.linkButton,
      quote: this.quoteButton
    };

    await buttonMap[type].click();
  }

  /**
   * Upload attachment
   */
  async uploadAttachment(filePath: string): Promise<void> {
    await this.attachmentUpload.setInputFiles(filePath);
    await this.attachmentList.waitFor();
  }

  /**
   * Filter content by category
   */
  async filterByCategory(categoryId: string): Promise<void> {
    await this.searchCategoryFilter.click();
    await this.page.locator(`[data-value="${categoryId}"]`).click();
  }

  /**
   * Sort content
   */
  async sortContent(sortBy: 'recent' | 'popular' | 'oldest' | 'replies' | 'views'): Promise<void> {
    await this.searchSortSelect.click();
    await this.page.locator(`[data-value="${sortBy}"]`).click();
  }

  /**
   * Moderate content (hide, lock, pin, delete)
   */
  async moderateContent(
    action: 'hide' | 'lock' | 'pin' | 'move' | 'delete',
    reason?: string,
    targetCategoryId?: string
  ): Promise<void> {
    await this.moderationMenu.click();
    
    const actionButtons = {
      hide: this.hideContentButton,
      lock: this.lockThreadButton,
      pin: this.pinThreadButton,
      move: this.moveThreadButton,
      delete: this.deleteContentButton
    };

    await actionButtons[action].click();

    if (reason || targetCategoryId) {
      await this.moderationReasonDialog.waitFor();
      
      if (reason) {
        await this.moderationReasonInput.fill(reason);
      }
      
      if (targetCategoryId && action === 'move') {
        await this.page.locator('[data-testid="target-category-select"]').click();
        await this.page.locator(`[data-value="${targetCategoryId}"]`).click();
      }
      
      await this.confirmModerationButton.click();
    }

    await this.moderationReasonDialog.waitFor({ state: 'hidden' });
  }

  /**
   * Check notification count
   */
  async getNotificationCount(): Promise<number> {
    const badge = await this.notificationBadge.textContent();
    return badge ? parseInt(badge) : 0;
  }

  /**
   * Open notifications panel
   */
  async openNotifications(): Promise<void> {
    await this.notificationBadge.click();
    await this.notificationPanel.waitFor();
  }

  /**
   * Mark all notifications as read
   */
  async markAllNotificationsRead(): Promise<void> {
    await this.openNotifications();
    await this.markAllReadButton.click();
  }

  /**
   * Navigate to next page
   */
  async goToNextPage(): Promise<void> {
    await this.nextPageButton.click();
    await this.waitForLoad();
  }

  /**
   * Navigate to previous page
   */
  async goToPreviousPage(): Promise<void> {
    await this.previousPageButton.click();
    await this.waitForLoad();
  }

  /**
   * Go to specific page number
   */
  async goToPage(pageNumber: number): Promise<void> {
    await this.page.locator(`[data-testid="page-${pageNumber}"]`).click();
    await this.waitForLoad();
  }

  /**
   * Toggle mobile menu
   */
  async toggleMobileMenu(): Promise<void> {
    await this.mobileMenuToggle.click();
    await this.mobileSidebar.waitFor();
  }

  /**
   * Toggle mobile search
   */
  async toggleMobileSearch(): Promise<void> {
    await this.mobileSearchToggle.click();
    await this.searchInput.waitFor();
  }

  /**
   * Verify post is displayed correctly
   */
  async verifyPostDisplay(postData: {
    title: string;
    author: string;
    content?: string;
    category?: string;
    repliesCount?: number;
  }): Promise<void> {
    await expect(this.postTitle.first()).toContainText(postData.title);
    await expect(this.postAuthor.first()).toContainText(postData.author);
    
    if (postData.content) {
      await expect(this.postContent.first()).toContainText(postData.content);
    }
    
    if (postData.category) {
      await expect(this.postCategory.first()).toContainText(postData.category);
    }
    
    if (postData.repliesCount !== undefined) {
      await expect(this.postRepliesCount.first()).toContainText(postData.repliesCount.toString());
    }
  }

  /**
   * Verify forum statistics are displayed
   */
  async verifyForumStats(): Promise<void> {
    await expect(this.statsContainer).toBeVisible();
    await expect(this.totalPostsCount).toBeVisible();
    await expect(this.totalUsersCount).toBeVisible();
    await expect(this.totalCategoriesCount).toBeVisible();
  }

  /**
   * Verify search functionality works
   */
  async verifySearchResults(query: string, expectedResultCount?: number): Promise<void> {
    await expect(this.searchResults).toBeVisible();
    
    if (expectedResultCount !== undefined) {
      const resultsText = await this.searchResultsCount.textContent();
      const actualCount = resultsText ? parseInt(resultsText.match(/\d+/)?.[0] || '0') : 0;
      expect(actualCount).toBe(expectedResultCount);
    }
  }

  /**
   * Verify rich text editor functionality
   */
  async verifyRichTextEditor(): Promise<void> {
    await expect(this.editorToolbar).toBeVisible();
    await expect(this.boldButton).toBeVisible();
    await expect(this.italicButton).toBeVisible();
    await expect(this.linkButton).toBeVisible();
    await expect(this.codeButton).toBeVisible();
  }

  /**
   * Verify moderation tools are available for moderators
   */
  async verifyModerationTools(): Promise<void> {
    await expect(this.moderationMenu).toBeVisible();
    
    await this.moderationMenu.click();
    await expect(this.hideContentButton).toBeVisible();
    await expect(this.lockThreadButton).toBeVisible();
    await expect(this.pinThreadButton).toBeVisible();
    await expect(this.deleteContentButton).toBeVisible();
  }

  /**
   * Verify accessibility features
   */
  async verifyAccessibility(): Promise<void> {
    // Check for proper ARIA labels and keyboard navigation
    await expect(this.createPostButton).toHaveAttribute('aria-label');
    await expect(this.searchInput).toHaveAttribute('aria-label');
    
    // Test keyboard navigation
    await this.page.keyboard.press('Tab');
    await expect(this.createPostButton).toBeFocused();
  }
}