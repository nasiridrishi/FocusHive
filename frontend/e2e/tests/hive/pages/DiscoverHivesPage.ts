/**
 * Discover Hives Page Object Model
 * Handles hive browsing, searching, and filtering functionality
 */

import { Page, Locator, expect } from '@playwright/test';
import { HiveSearchParams } from '../../../src/services/api/hiveApi';
import { TestHive } from '../hive-fixtures';

export class DiscoverHivesPage {
  readonly page: Page;

  // Main page elements
  readonly discoveryPage: Locator;
  readonly pageTitle: Locator;
  readonly createHiveButton: Locator;

  // Search functionality
  readonly searchInput: Locator;
  readonly searchButton: Locator;
  readonly clearSearchButton: Locator;

  // Filter controls
  readonly filtersSection: Locator;
  readonly privacyFilter: Locator;
  readonly availableSlotsFilter: Locator;
  readonly tagsFilter: Locator;
  readonly memberCountFilter: Locator;
  readonly sortBySelect: Locator;
  readonly sortOrderSelect: Locator;
  readonly resetFiltersButton: Locator;

  // Results section
  readonly searchResults: Locator;
  readonly hiveCards: Locator;
  readonly noResultsMessage: Locator;
  readonly loadingIndicator: Locator;

  // Pagination
  readonly paginationSection: Locator;
  readonly previousPageButton: Locator;
  readonly nextPageButton: Locator;
  readonly pageNumbers: Locator;
  readonly resultsPerPageSelect: Locator;

  // View options
  readonly viewToggle: Locator;
  readonly gridViewButton: Locator;
  readonly listViewButton: Locator;

  constructor(page: Page) {
    this.page = page;

    // Main page elements
    this.discoveryPage = page.locator('[data-testid="hive-discovery"]');
    this.pageTitle = page.locator('[data-testid="discovery-title"]');
    this.createHiveButton = page.locator('[data-testid="create-new-hive-button"]');

    // Search functionality
    this.searchInput = page.locator('[data-testid="hive-search-input"]');
    this.searchButton = page.locator('[data-testid="search-button"]');
    this.clearSearchButton = page.locator('[data-testid="clear-search-button"]');

    // Filter controls
    this.filtersSection = page.locator('[data-testid="filters-section"]');
    this.privacyFilter = page.locator('[data-testid="filter-privacy"]');
    this.availableSlotsFilter = page.locator('[data-testid="filter-available-slots"]');
    this.tagsFilter = page.locator('[data-testid="filter-tags"]');
    this.memberCountFilter = page.locator('[data-testid="filter-member-count"]');
    this.sortBySelect = page.locator('[data-testid="sort-by-select"]');
    this.sortOrderSelect = page.locator('[data-testid="sort-order-select"]');
    this.resetFiltersButton = page.locator('[data-testid="reset-filters-button"]');

    // Results section
    this.searchResults = page.locator('[data-testid="hive-search-results"]');
    this.hiveCards = page.locator('[data-testid="hive-card"]');
    this.noResultsMessage = page.locator('[data-testid="no-results-message"]');
    this.loadingIndicator = page.locator('[data-testid="loading-indicator"]');

    // Pagination
    this.paginationSection = page.locator('[data-testid="pagination-section"]');
    this.previousPageButton = page.locator('[data-testid="previous-page"]');
    this.nextPageButton = page.locator('[data-testid="next-page"]');
    this.pageNumbers = page.locator('[data-testid="page-number"]');
    this.resultsPerPageSelect = page.locator('[data-testid="results-per-page"]');

    // View options
    this.viewToggle = page.locator('[data-testid="view-toggle"]');
    this.gridViewButton = page.locator('[data-testid="grid-view-button"]');
    this.listViewButton = page.locator('[data-testid="list-view-button"]');
  }

  /**
   * Navigate to discover hives page
   */
  async goto(): Promise<void> {
    await this.page.goto('/hives/discover');
    await expect(this.discoveryPage).toBeVisible();
  }

  /**
   * Search for hives by query
   */
  async searchHives(query: string): Promise<void> {
    await this.searchInput.fill(query);
    await this.searchButton.click();
    await this.waitForResults();
  }

  /**
   * Search by pressing Enter
   */
  async searchHivesWithEnter(query: string): Promise<void> {
    await this.searchInput.fill(query);
    await this.searchInput.press('Enter');
    await this.waitForResults();
  }

  /**
   * Clear current search
   */
  async clearSearch(): Promise<void> {
    await this.clearSearchButton.click();
    await this.waitForResults();
    await expect(this.searchInput).toHaveValue('');
  }

  /**
   * Apply privacy filter
   */
  async filterByPrivacy(privacy: 'public' | 'private' | 'all'): Promise<void> {
    await this.privacyFilter.click();
    
    if (privacy !== 'all') {
      await this.page.click(`[data-testid="privacy-option-${privacy}"]`);
    } else {
      await this.page.click('[data-testid="privacy-option-all"]');
    }
    
    await this.waitForResults();
  }

  /**
   * Filter by available slots
   */
  async filterByAvailableSlots(hasSlots: boolean): Promise<void> {
    const isChecked = await this.availableSlotsFilter.isChecked();
    if (isChecked !== hasSlots) {
      await this.availableSlotsFilter.click();
    }
    await this.waitForResults();
  }

  /**
   * Filter by tags
   */
  async filterByTags(tags: string[]): Promise<void> {
    await this.tagsFilter.click();
    
    // First clear existing selections
    const clearAllTags = this.page.locator('[data-testid="clear-all-tags"]');
    if (await clearAllTags.isVisible()) {
      await clearAllTags.click();
    }
    
    // Select specified tags
    for (const tag of tags) {
      await this.page.click(`[data-testid="tag-filter-${tag}"]`);
    }
    
    // Close tag filter dropdown
    await this.tagsFilter.click();
    await this.waitForResults();
  }

  /**
   * Filter by member count range
   */
  async filterByMemberCount(min: number, max: number): Promise<void> {
    await this.memberCountFilter.click();
    await this.page.fill('[data-testid="min-members-input"]', min.toString());
    await this.page.fill('[data-testid="max-members-input"]', max.toString());
    await this.page.click('[data-testid="apply-member-filter"]');
    await this.waitForResults();
  }

  /**
   * Sort results
   */
  async sortResults(sortBy: 'name' | 'members' | 'activity' | 'created', order: 'asc' | 'desc' = 'asc'): Promise<void> {
    await this.sortBySelect.selectOption(sortBy);
    await this.sortOrderSelect.selectOption(order);
    await this.waitForResults();
  }

  /**
   * Reset all filters
   */
  async resetFilters(): Promise<void> {
    await this.resetFiltersButton.click();
    await this.waitForResults();
    
    // Verify filters are reset
    await expect(this.searchInput).toHaveValue('');
    await expect(this.availableSlotsFilter).not.toBeChecked();
  }

  /**
   * Switch to grid view
   */
  async switchToGridView(): Promise<void> {
    await this.gridViewButton.click();
    await expect(this.searchResults).toHaveClass(/grid-view/);
  }

  /**
   * Switch to list view
   */
  async switchToListView(): Promise<void> {
    await this.listViewButton.click();
    await expect(this.searchResults).toHaveClass(/list-view/);
  }

  /**
   * Get all hive cards on current page
   */
  async getHiveCards(): Promise<Array<{
    id: number;
    name: string;
    description: string;
    memberCount: number;
    maxMembers: number;
    isPrivate: boolean;
    tags: string[];
    hasAvailableSlots: boolean;
  }>> {
    const cards = await this.hiveCards.all();
    const hiveData = [];

    for (const card of cards) {
      const id = parseInt(await card.getAttribute('data-hive-id') || '0');
      const name = await card.locator('[data-testid="hive-name"]').textContent() || '';
      const description = await card.locator('[data-testid="hive-description"]').textContent() || '';
      
      const memberText = await card.locator('[data-testid="member-count"]').textContent() || '0/0';
      const [currentMembers, maxMembers] = memberText.split('/').map(num => parseInt(num.trim()));
      
      const privacyIcon = card.locator('[data-testid="privacy-icon"]');
      const isPrivate = await privacyIcon.getAttribute('title') === 'Private Hive';
      
      const tagElements = await card.locator('[data-testid="hive-tag"]').all();
      const tags = [];
      for (const tagElement of tagElements) {
        const tagText = await tagElement.textContent();
        if (tagText) tags.push(tagText.trim());
      }
      
      const hasAvailableSlots = currentMembers < maxMembers;

      hiveData.push({
        id,
        name,
        description,
        memberCount: currentMembers,
        maxMembers,
        isPrivate,
        tags,
        hasAvailableSlots
      });
    }

    return hiveData;
  }

  /**
   * Click on a specific hive card
   */
  async clickHiveCard(hiveId: number): Promise<void> {
    await this.page.click(`[data-testid="hive-card"][data-hive-id="${hiveId}"]`);
    
    // Should navigate to hive page
    await expect(this.page.locator('[data-testid="hive-workspace"]')).toBeVisible();
  }

  /**
   * Join hive from discovery page
   */
  async joinHiveFromCard(hiveId: number): Promise<void> {
    const card = this.page.locator(`[data-testid="hive-card"][data-hive-id="${hiveId}"]`);
    const joinButton = card.locator('[data-testid="quick-join-button"]');
    
    await joinButton.click();
    
    // Handle join confirmation or approval modal
    const confirmJoin = this.page.locator('[data-testid="confirm-join"]');
    const approvalModal = this.page.locator('[data-testid="join-approval-modal"]');
    
    if (await confirmJoin.isVisible()) {
      await confirmJoin.click();
    } else if (await approvalModal.isVisible()) {
      await this.page.fill('[data-testid="join-message-input"]', 'I would like to join this hive');
      await this.page.click('[data-testid="submit-join-request"]');
    }
  }

  /**
   * Get search results count
   */
  async getResultsCount(): Promise<number> {
    const resultsText = await this.page.locator('[data-testid="results-count"]').textContent() || '0';
    return parseInt(resultsText.replace(/\D/g, ''));
  }

  /**
   * Navigate to specific page
   */
  async goToPage(pageNumber: number): Promise<void> {
    await this.page.click(`[data-testid="page-number"]:has-text("${pageNumber}")`);
    await this.waitForResults();
  }

  /**
   * Navigate to next page
   */
  async goToNextPage(): Promise<void> {
    await this.nextPageButton.click();
    await this.waitForResults();
  }

  /**
   * Navigate to previous page
   */
  async goToPreviousPage(): Promise<void> {
    await this.previousPageButton.click();
    await this.waitForResults();
  }

  /**
   * Change results per page
   */
  async changeResultsPerPage(count: number): Promise<void> {
    await this.resultsPerPageSelect.selectOption(count.toString());
    await this.waitForResults();
  }

  /**
   * Get current pagination info
   */
  async getPaginationInfo(): Promise<{
    currentPage: number;
    totalPages: number;
    resultsPerPage: number;
    totalResults: number;
  }> {
    const currentPageElement = this.page.locator('[data-testid="current-page"]');
    const currentPage = parseInt(await currentPageElement.textContent() || '1');
    
    const totalPagesElement = this.page.locator('[data-testid="total-pages"]');
    const totalPages = parseInt(await totalPagesElement.textContent() || '1');
    
    const resultsPerPageValue = await this.resultsPerPageSelect.inputValue();
    const resultsPerPage = parseInt(resultsPerPageValue || '20');
    
    const totalResults = await this.getResultsCount();

    return {
      currentPage,
      totalPages,
      resultsPerPage,
      totalResults
    };
  }

  /**
   * Check if no results message is displayed
   */
  async hasNoResults(): Promise<boolean> {
    return await this.noResultsMessage.isVisible();
  }

  /**
   * Get no results message text
   */
  async getNoResultsMessage(): Promise<string> {
    return await this.noResultsMessage.textContent() || '';
  }

  /**
   * Wait for search results to load
   */
  async waitForResults(): Promise<void> {
    // Wait for loading to disappear
    if (await this.loadingIndicator.isVisible()) {
      await expect(this.loadingIndicator).not.toBeVisible();
    }
    
    // Wait for either results or no results message
    await expect(
      this.searchResults.or(this.noResultsMessage)
    ).toBeVisible();
  }

  /**
   * Perform advanced search with multiple filters
   */
  async advancedSearch(params: {
    query?: string;
    privacy?: 'public' | 'private' | 'all';
    tags?: string[];
    hasAvailableSlots?: boolean;
    memberCountMin?: number;
    memberCountMax?: number;
    sortBy?: 'name' | 'members' | 'activity' | 'created';
    sortOrder?: 'asc' | 'desc';
  }): Promise<void> {
    // Reset filters first
    await this.resetFilters();
    
    // Apply search query
    if (params.query) {
      await this.searchHives(params.query);
    }
    
    // Apply privacy filter
    if (params.privacy) {
      await this.filterByPrivacy(params.privacy);
    }
    
    // Apply tags filter
    if (params.tags && params.tags.length > 0) {
      await this.filterByTags(params.tags);
    }
    
    // Apply available slots filter
    if (params.hasAvailableSlots !== undefined) {
      await this.filterByAvailableSlots(params.hasAvailableSlots);
    }
    
    // Apply member count filter
    if (params.memberCountMin !== undefined || params.memberCountMax !== undefined) {
      const min = params.memberCountMin || 0;
      const max = params.memberCountMax || 1000;
      await this.filterByMemberCount(min, max);
    }
    
    // Apply sorting
    if (params.sortBy) {
      await this.sortResults(params.sortBy, params.sortOrder);
    }
  }

  /**
   * Verify search results match filters
   */
  async verifyResultsMatchFilters(expectedFilters: {
    hasAvailableSlots?: boolean;
    isPrivate?: boolean;
    tags?: string[];
  }): Promise<boolean> {
    const hiveCards = await this.getHiveCards();
    
    for (const hive of hiveCards) {
      // Check available slots filter
      if (expectedFilters.hasAvailableSlots !== undefined) {
        if (hive.hasAvailableSlots !== expectedFilters.hasAvailableSlots) {
          return false;
        }
      }
      
      // Check privacy filter
      if (expectedFilters.isPrivate !== undefined) {
        if (hive.isPrivate !== expectedFilters.isPrivate) {
          return false;
        }
      }
      
      // Check tags filter
      if (expectedFilters.tags && expectedFilters.tags.length > 0) {
        const hasMatchingTag = expectedFilters.tags.some(tag => 
          hive.tags.includes(tag)
        );
        if (!hasMatchingTag) {
          return false;
        }
      }
    }
    
    return true;
  }

  /**
   * Get filter summary
   */
  async getActiveFilters(): Promise<{
    searchQuery: string;
    privacy: string;
    hasAvailableSlots: boolean;
    selectedTags: string[];
    sortBy: string;
    sortOrder: string;
  }> {
    return {
      searchQuery: await this.searchInput.inputValue(),
      privacy: await this.privacyFilter.textContent() || 'all',
      hasAvailableSlots: await this.availableSlotsFilter.isChecked(),
      selectedTags: [], // Would need to implement getting selected tags
      sortBy: await this.sortBySelect.inputValue(),
      sortOrder: await this.sortOrderSelect.inputValue()
    };
  }
}

export default DiscoverHivesPage;