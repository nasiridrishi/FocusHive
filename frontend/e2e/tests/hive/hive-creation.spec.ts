/**
 * Hive Creation E2E Tests
 * Tests the complete hive creation workflow with various configurations
 * Following TDD approach with comprehensive test coverage
 */

import { test, expect, Page } from '@playwright/test';
import { CreateHivePage } from './pages/CreateHivePage';
import { HivePage } from './pages/HivePage';
import { EnhancedAuthHelper } from '../../helpers/auth-helpers';
import { 
  HIVE_TEST_USERS, 
  HIVE_TEMPLATES, 
  FORM_VALIDATION_CASES,
  generateUniqueHiveData,
  type TestHive 
} from './hive-fixtures';

test.describe('Hive Creation Workflow', () => {
  let page: Page;
  let createHivePage: CreateHivePage;
  let hivePage: HivePage;
  let authHelper: EnhancedAuthHelper;

  test.beforeEach(async ({ browser }) => {
    const context = await browser.newContext();
    page = await context.newPage();
    
    createHivePage = new CreateHivePage(page);
    hivePage = new HivePage(page);
    authHelper = new EnhancedAuthHelper(page);

    // Login as hive owner
    await authHelper.loginUser(
      HIVE_TEST_USERS.OWNER.email, 
      HIVE_TEST_USERS.OWNER.password
    );
  });

  test.afterEach(async () => {
    await page.close();
  });

  test.describe('Basic Hive Creation', () => {
    test('should create a public hive with minimal configuration', async () => {
      // Arrange
      const hiveData = generateUniqueHiveData('PUBLIC_STUDY_HIVE');
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo({
        name: hiveData.name,
        description: hiveData.description,
        maxMembers: 10,
        isPrivate: false
      });
      
      const createdHive = await createHivePage.createHive();
      
      // Assert
      expect(createdHive.name).toBe(hiveData.name);
      expect(createdHive.isPrivate).toBe(false);
      expect(createdHive.maxMembers).toBe(10);
      
      // Verify redirection to hive workspace
      await expect(page.locator('[data-testid="hive-workspace"]')).toBeVisible();
      
      // Verify hive appears in navigation
      const hiveInfo = await hivePage.getHiveInfo();
      expect(hiveInfo.title).toBe(hiveData.name);
      expect(hiveInfo.isJoined).toBe(true); // Creator automatically joins
    });

    test('should create a private hive with approval required', async () => {
      // Arrange
      const hiveData = generateUniqueHiveData('PRIVATE_WORK_HIVE');
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo({
        name: hiveData.name,
        description: hiveData.description,
        maxMembers: 5,
        isPrivate: true
      });
      
      await createHivePage.configureSettings({
        requireApproval: true,
        allowChat: true,
        allowMusic: false
      });
      
      const createdHive = await createHivePage.createHive();
      
      // Assert
      expect(createdHive.isPrivate).toBe(true);
      expect(createdHive.maxMembers).toBe(5);
      
      // Verify settings were applied
      await hivePage.goto(createdHive.id);
      await hivePage.openHiveSettings();
      
      const requireApprovalCheckbox = page.locator('[data-testid="setting-requireApproval"]');
      const allowMusicCheckbox = page.locator('[data-testid="setting-allowMusic"]');
      
      await expect(requireApprovalCheckbox).toBeChecked();
      await expect(allowMusicCheckbox).not.toBeChecked();
    });

    test('should create hive with tags and work hours configuration', async () => {
      // Arrange
      const hiveData = generateUniqueHiveData('PUBLIC_STUDY_HIVE');
      const tags = ['study', 'productivity', 'university'];
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo({
        name: hiveData.name,
        description: hiveData.description,
        maxMembers: 20
      });
      
      await createHivePage.addTags(tags);
      await createHivePage.configureSettings({
        workHours: {
          start: '09:00',
          end: '17:00',
          timezone: 'UTC'
        }
      });
      
      const createdHive = await createHivePage.createHive();
      
      // Assert
      expect(createdHive.tags).toEqual(expect.arrayContaining(tags));
      
      // Verify tags appear in hive workspace
      await hivePage.goto(createdHive.id);
      for (const tag of tags) {
        await expect(
          page.locator(`[data-testid="hive-tag"]:has-text("${tag}")`)
        ).toBeVisible();
      }
    });
  });

  test.describe('Hive Image Upload', () => {
    test('should upload and preview hive image', async () => {
      // Arrange
      const hiveData = generateUniqueHiveData();
      const imagePath = './test-fixtures/test-hive-image.jpg';
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(hiveData);
      await createHivePage.uploadImage(imagePath);
      
      // Assert - Image preview should be visible
      await expect(createHivePage.imagePreview).toBeVisible();
      
      // Create hive and verify image persists
      const createdHive = await createHivePage.createHive();
      await hivePage.goto(createdHive.id);
      
      await expect(page.locator('[data-testid="hive-avatar"]')).toBeVisible();
    });

    test('should remove uploaded image', async () => {
      // Arrange
      const hiveData = generateUniqueHiveData();
      const imagePath = './test-fixtures/test-hive-image.jpg';
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(hiveData);
      await createHivePage.uploadImage(imagePath);
      
      // Verify image is uploaded
      await expect(createHivePage.imagePreview).toBeVisible();
      
      // Remove image
      await createHivePage.removeImage();
      
      // Assert - Image preview should be hidden
      await expect(createHivePage.imagePreview).not.toBeVisible();
    });

    test('should handle invalid image file formats', async () => {
      // Arrange
      const hiveData = generateUniqueHiveData();
      const invalidImagePath = './test-fixtures/invalid-file.txt';
      
      // Act & Assert
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(hiveData);
      
      // Attempt to upload invalid file
      await expect(async () => {
        await createHivePage.uploadImage(invalidImagePath);
      }).rejects.toThrow();
      
      // Verify error message appears
      await expect(
        page.locator('[data-testid="image-upload-error"]')
      ).toBeVisible();
    });
  });

  test.describe('Form Validation', () => {
    test('should show validation errors for empty required fields', async () => {
      // Act
      await createHivePage.goto();
      await createHivePage.createHive(); // Try to submit empty form
      
      // Assert
      const validationErrors = await createHivePage.getValidationErrors();
      expect(validationErrors).toContain('Hive name is required');
    });

    test('should validate hive name length constraints', async () => {
      // Test minimum length
      await createHivePage.goto();
      await createHivePage.testValidation({
        name: 'AB',
        description: 'Valid description',
        expectedError: 'Hive name must be at least 3 characters'
      });
      
      // Test maximum length
      await createHivePage.resetForm();
      await createHivePage.testValidation({
        name: 'A'.repeat(101),
        description: 'Valid description',
        expectedError: 'Hive name must be 100 characters or less'
      });
    });

    test('should validate member count constraints', async () => {
      // Test minimum members
      await createHivePage.goto();
      await createHivePage.testValidation({
        name: 'Valid Name',
        description: 'Valid description',
        maxMembers: 1,
        expectedError: 'Maximum members must be at least 2'
      });
      
      // Test maximum members
      await createHivePage.resetForm();
      await createHivePage.testValidation({
        name: 'Valid Name',
        description: 'Valid description',
        maxMembers: 1001,
        expectedError: 'Maximum members cannot exceed 1000'
      });
    });

    test('should validate duplicate hive names', async () => {
      // First create a hive
      const firstHiveData = generateUniqueHiveData();
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(firstHiveData);
      await createHivePage.createHive();
      
      // Try to create another hive with the same name
      await createHivePage.goto();
      await createHivePage.testDuplicateName(firstHiveData.name);
    });

    test('should show character count for limited fields', async () => {
      // Arrange
      const longName = 'A'.repeat(50);
      const longDescription = 'B'.repeat(200);
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo({
        name: longName,
        description: longDescription
      });
      
      // Assert
      const charCounts = await createHivePage.getCharacterCounts();
      expect(charCounts.name).toBe(50);
      expect(charCounts.description).toBe(200);
      
      // Verify character count indicators are visible
      await expect(page.locator('[data-testid="name-char-count"]')).toContainText('50/100');
      await expect(page.locator('[data-testid="description-char-count"]')).toContainText('200/500');
    });
  });

  test.describe('Privacy Settings', () => {
    test('should toggle between public and private correctly', async () => {
      // Arrange
      const hiveData = generateUniqueHiveData();
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo({
        name: hiveData.name,
        description: hiveData.description,
        isPrivate: false
      });
      
      // Initially public
      let previewInfo = await createHivePage.getPreviewInfo();
      expect(previewInfo.isPrivate).toBe(false);
      
      // Toggle to private
      await createHivePage.fillBasicInfo({ isPrivate: true });
      previewInfo = await createHivePage.getPreviewInfo();
      expect(previewInfo.isPrivate).toBe(true);
      
      // Toggle back to public
      await createHivePage.fillBasicInfo({ isPrivate: false });
      previewInfo = await createHivePage.getPreviewInfo();
      expect(previewInfo.isPrivate).toBe(false);
    });

    test('should show approval setting only for appropriate privacy levels', async () => {
      // Arrange
      const hiveData = generateUniqueHiveData();
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(hiveData);
      
      // Public hive - approval setting should be available
      await createHivePage.fillBasicInfo({ isPrivate: false });
      await expect(createHivePage.requireApprovalCheckbox).toBeVisible();
      
      // Private hive - approval setting should still be available
      await createHivePage.fillBasicInfo({ isPrivate: true });
      await expect(createHivePage.requireApprovalCheckbox).toBeVisible();
    });
  });

  test.describe('Live Preview', () => {
    test('should update preview as form is filled', async () => {
      // Arrange
      const hiveData = generateUniqueHiveData();
      const tags = ['test', 'preview'];
      
      // Act
      await createHivePage.goto();
      
      // Fill form step by step and check preview updates
      await createHivePage.fillBasicInfo({ name: hiveData.name });
      let previewInfo = await createHivePage.getPreviewInfo();
      expect(previewInfo.name).toBe(hiveData.name);
      
      await createHivePage.fillBasicInfo({ description: hiveData.description });
      previewInfo = await createHivePage.getPreviewInfo();
      expect(previewInfo.description).toBe(hiveData.description);
      
      await createHivePage.fillBasicInfo({ maxMembers: 15 });
      previewInfo = await createHivePage.getPreviewInfo();
      expect(previewInfo.memberLimit).toBe(15);
      
      await createHivePage.addTags(tags);
      previewInfo = await createHivePage.getPreviewInfo();
      expect(previewInfo.tags).toEqual(expect.arrayContaining(tags));
    });
  });

  test.describe('Auto-generated Slug', () => {
    test('should generate slug from hive name', async () => {
      // Arrange
      const hiveName = 'My Awesome Study Hive';
      const expectedSlug = 'my-awesome-study-hive';
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo({ name: hiveName });
      
      // Assert
      await createHivePage.verifySlugGeneration(expectedSlug);
    });

    test('should handle special characters in slug generation', async () => {
      // Arrange
      const hiveName = 'C++ Programming & Development!';
      const expectedSlug = 'cpp-programming-development';
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo({ name: hiveName });
      
      // Assert
      await createHivePage.verifySlugGeneration(expectedSlug);
    });

    test('should allow manual slug editing', async () => {
      // Arrange
      const hiveName = 'Test Hive';
      const customSlug = 'custom-test-slug';
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo({ name: hiveName });
      await createHivePage.slugInput.fill(customSlug);
      
      // Assert
      await expect(createHivePage.slugInput).toHaveValue(customSlug);
    });
  });

  test.describe('Tag Management', () => {
    test('should add and remove tags dynamically', async () => {
      // Arrange
      const hiveData = generateUniqueHiveData();
      const tags = ['study', 'work', 'productivity'];
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(hiveData);
      
      // Add tags
      await createHivePage.addTags(tags);
      
      // Verify all tags are added
      for (const tag of tags) {
        await expect(
          createHivePage.tagsList.locator(`[data-testid="tag-${tag}"]`)
        ).toBeVisible();
      }
      
      // Remove one tag
      await createHivePage.removeTag('work');
      await expect(
        createHivePage.tagsList.locator(`[data-testid="tag-work"]`)
      ).not.toBeVisible();
      
      // Verify other tags remain
      await expect(
        createHivePage.tagsList.locator(`[data-testid="tag-study"]`)
      ).toBeVisible();
      await expect(
        createHivePage.tagsList.locator(`[data-testid="tag-productivity"]`)
      ).toBeVisible();
    });

    test('should prevent duplicate tags', async () => {
      // Arrange
      const hiveData = generateUniqueHiveData();
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(hiveData);
      
      // Add tag twice
      await createHivePage.addTags(['study']);
      await createHivePage.addTags(['study']); // Duplicate
      
      // Assert - Should only have one instance
      const tagElements = await createHivePage.tagsList.locator('[data-testid="tag-study"]').count();
      expect(tagElements).toBe(1);
    });

    test('should limit number of tags', async () => {
      // Arrange
      const hiveData = generateUniqueHiveData();
      const manyTags = Array.from({ length: 15 }, (_, i) => `tag${i + 1}`);
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(hiveData);
      
      // Try to add too many tags
      await createHivePage.addTags(manyTags);
      
      // Assert - Should show error for exceeding tag limit
      await expect(
        page.locator('[data-testid="tag-limit-error"]')
      ).toBeVisible();
      
      // Should only show maximum allowed tags (e.g., 10)
      const visibleTags = await createHivePage.tagsList.locator('[data-testid^="tag-"]').count();
      expect(visibleTags).toBeLessThanOrEqual(10);
    });
  });

  test.describe('Form Reset and Cancel', () => {
    test('should reset form to initial state', async () => {
      // Arrange
      const hiveData = generateUniqueHiveData();
      const tags = ['test', 'reset'];
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(hiveData);
      await createHivePage.addTags(tags);
      await createHivePage.configureSettings({
        allowChat: false,
        requireApproval: true
      });
      
      // Reset form
      await createHivePage.resetForm();
      
      // Assert - All fields should be cleared
      await expect(createHivePage.nameInput).toHaveValue('');
      await expect(createHivePage.descriptionInput).toHaveValue('');
      await expect(createHivePage.maxMembersInput).toHaveValue('10'); // Default value
      await expect(createHivePage.privateCheckbox).not.toBeChecked();
      
      // Tags should be cleared
      const remainingTags = await createHivePage.tagsList.locator('[data-testid^="tag-"]').count();
      expect(remainingTags).toBe(0);
    });

    test('should cancel creation and navigate away', async () => {
      // Arrange
      const hiveData = generateUniqueHiveData();
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(hiveData);
      await createHivePage.cancelCreation();
      
      // Assert - Should navigate to dashboard or hives list
      const currentUrl = page.url();
      expect(currentUrl).not.toContain('/hives/create');
      expect(currentUrl).toMatch(/\/(dashboard|hives)/);
    });

    test('should show confirmation dialog when canceling with unsaved changes', async () => {
      // Arrange
      const hiveData = generateUniqueHiveData();
      
      // Act
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(hiveData);
      await createHivePage.cancelButton.click();
      
      // Assert - Confirmation dialog should appear
      const confirmDialog = page.locator('[data-testid="confirm-cancel-dialog"]');
      await expect(confirmDialog).toBeVisible();
      
      // Cancel the cancellation (stay on form)
      await page.click('[data-testid="stay-on-form"]');
      await expect(createHivePage.createHiveForm).toBeVisible();
      
      // Confirm cancellation (leave form)
      await createHivePage.cancelButton.click();
      await page.click('[data-testid="confirm-cancel"]');
      await expect(createHivePage.createHiveForm).not.toBeVisible();
    });
  });

  test.describe('Accessibility', () => {
    test('should support keyboard navigation', async () => {
      // Arrange
      await createHivePage.goto();
      
      // Act - Navigate form using Tab key
      await page.keyboard.press('Tab'); // Name input
      await expect(createHivePage.nameInput).toBeFocused();
      
      await page.keyboard.press('Tab'); // Description input
      await expect(createHivePage.descriptionInput).toBeFocused();
      
      await page.keyboard.press('Tab'); // Max members input
      await expect(createHivePage.maxMembersInput).toBeFocused();
      
      // Should be able to toggle checkbox with Space
      await page.keyboard.press('Tab'); // Private checkbox
      await page.keyboard.press('Space');
      await expect(createHivePage.privateCheckbox).toBeChecked();
    });

    test('should have proper ARIA labels and descriptions', async () => {
      // Arrange
      await createHivePage.goto();
      
      // Assert - Form elements should have proper accessibility attributes
      await expect(createHivePage.nameInput).toHaveAttribute('aria-label', 'Hive name');
      await expect(createHivePage.descriptionInput).toHaveAttribute('aria-label', 'Hive description');
      await expect(createHivePage.maxMembersInput).toHaveAttribute('aria-label', 'Maximum number of members');
      await expect(createHivePage.privateCheckbox).toHaveAttribute('aria-label', 'Make this hive private');
      
      // Validation errors should be announced
      await createHivePage.createButton.click(); // Submit empty form
      const errorElement = page.locator('[data-testid="validation-error"]').first();
      await expect(errorElement).toHaveAttribute('role', 'alert');
      await expect(errorElement).toHaveAttribute('aria-live', 'assertive');
    });
  });

  test.describe('Performance', () => {
    test('should load create hive page within acceptable time', async () => {
      // Arrange
      const startTime = Date.now();
      
      // Act
      await createHivePage.goto();
      
      // Assert
      const loadTime = Date.now() - startTime;
      expect(loadTime).toBeLessThan(3000); // Should load within 3 seconds
      
      // Form should be interactive
      await expect(createHivePage.nameInput).toBeEditable();
      await expect(createHivePage.createButton).toBeEnabled();
    });

    test('should handle form submission without performance degradation', async () => {
      // Arrange
      const hiveData = generateUniqueHiveData();
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(hiveData);
      
      // Act
      const startTime = Date.now();
      await createHivePage.createButton.click();
      
      // Wait for creation to complete
      await expect(
        createHivePage.successMessage.or(page.locator('[data-testid="hive-workspace"]'))
      ).toBeVisible();
      
      // Assert
      const creationTime = Date.now() - startTime;
      expect(creationTime).toBeLessThan(5000); // Should complete within 5 seconds
    });
  });
});