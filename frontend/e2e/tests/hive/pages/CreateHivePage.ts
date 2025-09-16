/**
 * Create Hive Page Object Model
 * Handles hive creation form interactions and validations
 */

import {expect, Locator, Page} from '@playwright/test';
import {CreateHiveRequest, HiveSettings} from '../../../src/services/api/hiveApi';
import {FORM_VALIDATION_CASES, TestHive} from '../hive-fixtures';

export class CreateHivePage {
  readonly page: Page;

  // Form elements
  readonly createHiveForm: Locator;
  readonly nameInput: Locator;
  readonly descriptionInput: Locator;
  readonly slugInput: Locator;
  readonly maxMembersInput: Locator;
  readonly privateCheckbox: Locator;
  readonly tagsInput: Locator;
  readonly tagsList: Locator;

  // Settings section
  readonly settingsSection: Locator;
  readonly allowChatCheckbox: Locator;
  readonly allowMusicCheckbox: Locator;
  readonly requireApprovalCheckbox: Locator;

  // Work hours settings
  readonly workHoursSection: Locator;
  readonly startTimeInput: Locator;
  readonly endTimeInput: Locator;
  readonly timezoneSelect: Locator;

  // Image upload
  readonly imageUploadSection: Locator;
  readonly imageInput: Locator;
  readonly imagePreview: Locator;
  readonly removeImageButton: Locator;

  // Form actions
  readonly createButton: Locator;
  readonly cancelButton: Locator;
  readonly resetButton: Locator;

  // Validation messages
  readonly validationErrors: Locator;
  readonly successMessage: Locator;

  // Preview section
  readonly previewSection: Locator;
  readonly previewCard: Locator;

  constructor(page: Page) {
    this.page = page;

    // Form elements
    this.createHiveForm = page.locator('[data-testid="create-hive-form"]');
    this.nameInput = page.locator('[data-testid="hive-name-input"]');
    this.descriptionInput = page.locator('[data-testid="hive-description-input"]');
    this.slugInput = page.locator('[data-testid="hive-slug-input"]');
    this.maxMembersInput = page.locator('[data-testid="hive-max-members-input"]');
    this.privateCheckbox = page.locator('[data-testid="hive-private-checkbox"]');
    this.tagsInput = page.locator('[data-testid="hive-tags-input"]');
    this.tagsList = page.locator('[data-testid="hive-tags-list"]');

    // Settings section
    this.settingsSection = page.locator('[data-testid="hive-settings-section"]');
    this.allowChatCheckbox = page.locator('[data-testid="setting-allowChat"]');
    this.allowMusicCheckbox = page.locator('[data-testid="setting-allowMusic"]');
    this.requireApprovalCheckbox = page.locator('[data-testid="setting-requireApproval"]');

    // Work hours settings
    this.workHoursSection = page.locator('[data-testid="work-hours-section"]');
    this.startTimeInput = page.locator('[data-testid="start-time-input"]');
    this.endTimeInput = page.locator('[data-testid="end-time-input"]');
    this.timezoneSelect = page.locator('[data-testid="timezone-select"]');

    // Image upload
    this.imageUploadSection = page.locator('[data-testid="image-upload-section"]');
    this.imageInput = page.locator('[data-testid="hive-image-input"]');
    this.imagePreview = page.locator('[data-testid="image-preview"]');
    this.removeImageButton = page.locator('[data-testid="remove-image-button"]');

    // Form actions
    this.createButton = page.locator('[data-testid="create-hive-button"]');
    this.cancelButton = page.locator('[data-testid="cancel-create-hive"]');
    this.resetButton = page.locator('[data-testid="reset-form-button"]');

    // Validation messages
    this.validationErrors = page.locator('[data-testid="validation-error"]');
    this.successMessage = page.locator('[data-testid="hive-created-success"]');

    // Preview section
    this.previewSection = page.locator('[data-testid="hive-preview-section"]');
    this.previewCard = page.locator('[data-testid="hive-preview-card"]');
  }

  /**
   * Navigate to create hive page
   */
  async goto(): Promise<void> {
    await this.page.goto('/hives/create');
    await expect(this.createHiveForm).toBeVisible();
  }

  /**
   * Fill basic hive information
   */
  async fillBasicInfo(hiveData: Partial<CreateHiveRequest>): Promise<void> {
    if (hiveData.name) {
      await this.nameInput.fill(hiveData.name);
    }

    if (hiveData.description) {
      await this.descriptionInput.fill(hiveData.description);
    }

    if (hiveData.slug) {
      await this.slugInput.fill(hiveData.slug);
    }

    if (hiveData.maxMembers) {
      await this.maxMembersInput.fill(hiveData.maxMembers.toString());
    }

    if (hiveData.isPrivate !== undefined) {
      const isChecked = await this.privateCheckbox.isChecked();
      if (isChecked !== hiveData.isPrivate) {
        await this.privateCheckbox.click();
      }
    }
  }

  /**
   * Add tags to the hive
   */
  async addTags(tags: string[]): Promise<void> {
    for (const tag of tags) {
      await this.tagsInput.fill(tag);
      await this.tagsInput.press('Enter');

      // Verify tag was added
      await expect(
          this.tagsList.locator(`[data-testid="tag-${tag}"]`)
      ).toBeVisible();
    }
  }

  /**
   * Remove a tag
   */
  async removeTag(tag: string): Promise<void> {
    await this.tagsList.locator(`[data-testid="remove-tag-${tag}"]`).click();

    // Verify tag was removed
    await expect(
        this.tagsList.locator(`[data-testid="tag-${tag}"]`)
    ).not.toBeVisible();
  }

  /**
   * Configure hive settings
   */
  async configureSettings(settings: Partial<HiveSettings>): Promise<void> {
    if (settings.allowChat !== undefined) {
      await this.toggleSetting('allowChat', settings.allowChat);
    }

    if (settings.allowMusic !== undefined) {
      await this.toggleSetting('allowMusic', settings.allowMusic);
    }

    if (settings.requireApproval !== undefined) {
      await this.toggleSetting('requireApproval', settings.requireApproval);
    }

    // Configure work hours if provided
    if (settings.workHours) {
      await this.configureWorkHours(settings.workHours);
    }
  }

  /**
   * Configure work hours
   */
  async configureWorkHours(workHours: {
    start: string;
    end: string;
    timezone: string;
  }): Promise<void> {
    await this.startTimeInput.fill(workHours.start);
    await this.endTimeInput.fill(workHours.end);
    await this.timezoneSelect.selectOption(workHours.timezone);
  }

  /**
   * Upload hive image
   */
  async uploadImage(imagePath: string): Promise<void> {
    await this.imageInput.setInputFiles(imagePath);

    // Wait for image preview to appear
    await expect(this.imagePreview).toBeVisible();
  }

  /**
   * Remove uploaded image
   */
  async removeImage(): Promise<void> {
    await this.removeImageButton.click();
    await expect(this.imagePreview).not.toBeVisible();
  }

  /**
   * Create the hive
   */
  async createHive(): Promise<TestHive> {
    await this.createButton.click();

    // Wait for success message or redirect
    await expect(
        this.successMessage.or(this.page.locator('[data-testid="hive-workspace"]'))
    ).toBeVisible();

    // Extract hive information from success state or URL
    let hiveId: number;
    if (await this.successMessage.isVisible()) {
      const successText = await this.successMessage.textContent() || '';
      const idMatch = successText.match(/ID: (\d+)/);
      hiveId = idMatch ? parseInt(idMatch[1]) : Date.now();
    } else {
      // Extract from URL if redirected to hive page
      const url = this.page.url();
      hiveId = parseInt(url.split('/hives/')[1]) || Date.now();
    }

    // Get form values to return as TestHive
    const name = await this.nameInput.inputValue();
    const description = await this.descriptionInput.inputValue();
    const isPrivate = await this.privateCheckbox.isChecked();
    const maxMembers = parseInt(await this.maxMembersInput.inputValue()) || 10;

    return {
      id: hiveId,
      name,
      description,
      slug: name.toLowerCase().replace(/\s+/g, '-'),
      isPrivate,
      maxMembers,
      memberCount: 1, // Creator is automatically a member
      tags: await this.getSelectedTags()
    } as TestHive;
  }

  /**
   * Cancel hive creation
   */
  async cancelCreation(): Promise<void> {
    await this.cancelButton.click();

    // Should navigate away from create page
    await expect(this.createHiveForm).not.toBeVisible();
  }

  /**
   * Reset form to initial state
   */
  async resetForm(): Promise<void> {
    await this.resetButton.click();

    // Verify form is cleared
    await expect(this.nameInput).toHaveValue('');
    await expect(this.descriptionInput).toHaveValue('');
  }

  /**
   * Get validation errors
   */
  async getValidationErrors(): Promise<string[]> {
    const errorElements = await this.validationErrors.all();
    const errors = [];

    for (const element of errorElements) {
      const errorText = await element.textContent();
      if (errorText) {
        errors.push(errorText);
      }
    }

    return errors;
  }

  /**
   * Test form validation with invalid data
   */
  async testValidation(validationCase: typeof FORM_VALIDATION_CASES.INVALID_HIVE_DATA[0]): Promise<void> {
    // Fill form with invalid data
    if (validationCase.name !== undefined) {
      await this.nameInput.fill(validationCase.name);
    }
    if (validationCase.description !== undefined) {
      await this.descriptionInput.fill(validationCase.description);
    }
    if (validationCase.maxMembers !== undefined) {
      await this.maxMembersInput.fill(validationCase.maxMembers.toString());
    }

    // Try to submit
    await this.createButton.click();

    // Verify expected error appears
    await expect(
        this.page.locator(`[data-testid="validation-error"]:has-text("${validationCase.expectedError}")`)
    ).toBeVisible();
  }

  /**
   * Get preview card information
   */
  async getPreviewInfo(): Promise<{
    name: string;
    description: string;
    isPrivate: boolean;
    memberLimit: number;
    tags: string[];
  }> {
    const name = await this.previewCard.locator('[data-testid="preview-name"]').textContent() || '';
    const description = await this.previewCard.locator('[data-testid="preview-description"]').textContent() || '';
    const privacyText = await this.previewCard.locator('[data-testid="preview-privacy"]').textContent() || '';
    const isPrivate = privacyText.includes('Private');
    const memberLimitText = await this.previewCard.locator('[data-testid="preview-member-limit"]').textContent() || '0';
    const memberLimit = parseInt(memberLimitText.replace(/\D/g, ''));
    const tags = await this.getPreviewTags();

    return {
      name,
      description,
      isPrivate,
      memberLimit,
      tags
    };
  }

  /**
   * Verify auto-generated slug
   */
  async verifySlugGeneration(expectedSlug: string): Promise<void> {
    await expect(this.slugInput).toHaveValue(expectedSlug);
  }

  /**
   * Test duplicate hive name
   */
  async testDuplicateName(existingName: string): Promise<void> {
    await this.nameInput.fill(existingName);
    await this.createButton.click();

    await expect(
        this.page.locator('[data-testid="validation-error"]:has-text("already exists")')
    ).toBeVisible();
  }

  /**
   * Get character count for fields with limits
   */
  async getCharacterCounts(): Promise<{
    name: number;
    description: number;
  }> {
    const nameCount = (await this.nameInput.inputValue()).length;
    const descriptionCount = (await this.descriptionInput.inputValue()).length;

    return {name: nameCount, description: descriptionCount};
  }

  /**
   * Toggle a setting checkbox
   */
  private async toggleSetting(setting: string, enabled: boolean): Promise<void> {
    const checkbox = this.page.locator(`[data-testid="setting-${setting}"]`);
    const isChecked = await checkbox.isChecked();

    if (isChecked !== enabled) {
      await checkbox.click();
    }
  }

  /**
   * Get selected tags
   */
  private async getSelectedTags(): Promise<string[]> {
    const tagElements = await this.tagsList.locator('[data-testid^="tag-"]').all();
    const tags = [];

    for (const element of tagElements) {
      const tagText = await element.textContent();
      if (tagText) {
        tags.push(tagText.trim());
      }
    }

    return tags;
  }

  /**
   * Get preview tags
   */
  private async getPreviewTags(): Promise<string[]> {
    const tagElements = await this.previewCard.locator('[data-testid="preview-tag"]').all();
    const tags = [];

    for (const element of tagElements) {
      const tagText = await element.textContent();
      if (tagText) {
        tags.push(tagText.trim());
      }
    }

    return tags;
  }
}

export default CreateHivePage;