/**
 * Hive Joining Workflow E2E Tests
 * Tests the complete hive discovery, joining, and leaving workflows
 * Includes multi-user scenarios and membership management
 */

import { test, expect, Page, Browser } from '@playwright/test';
import { HivePage } from './pages/HivePage';
import { CreateHivePage } from './pages/CreateHivePage';
import { DiscoverHivesPage } from './pages/DiscoverHivesPage';
import { EnhancedAuthHelper } from '../../helpers/auth-helpers';
import { HiveWorkflowHelper, MultiUserHiveHelper } from './hive-helpers';
import { 
  HIVE_TEST_USERS, 
  HIVE_TEMPLATES,
  generateUniqueHiveData,
  type TestHive 
} from './hive-fixtures';

test.describe('Hive Joining Workflow', () => {
  let browser: Browser;
  let ownerPage: Page;
  let memberPage: Page;
  let nonMemberPage: Page;
  
  let hivePage: HivePage;
  let createHivePage: CreateHivePage;
  let discoverPage: DiscoverHivesPage;
  
  let ownerAuth: EnhancedAuthHelper;
  let memberAuth: EnhancedAuthHelper;
  let nonMemberAuth: EnhancedAuthHelper;
  
  let testHive: TestHive;

  test.beforeAll(async ({ browser: testBrowser }) => {
    browser = testBrowser;
    
    // Set up multiple user contexts
    const ownerContext = await browser.newContext();
    const memberContext = await browser.newContext();
    const nonMemberContext = await browser.newContext();
    
    ownerPage = await ownerContext.newPage();
    memberPage = await memberContext.newPage();
    nonMemberPage = await nonMemberContext.newPage();
    
    // Initialize page objects
    createHivePage = new CreateHivePage(ownerPage);
    hivePage = new HivePage(memberPage);
    discoverPage = new DiscoverHivesPage(nonMemberPage);
    
    // Initialize auth helpers
    ownerAuth = new EnhancedAuthHelper(ownerPage);
    memberAuth = new EnhancedAuthHelper(memberPage);
    nonMemberAuth = new EnhancedAuthHelper(nonMemberPage);
    
    // Login all users
    await ownerAuth.loginUser(HIVE_TEST_USERS.OWNER.email, HIVE_TEST_USERS.OWNER.password);
    await memberAuth.loginUser(HIVE_TEST_USERS.MEMBER_1.email, HIVE_TEST_USERS.MEMBER_1.password);
    await nonMemberAuth.loginUser(HIVE_TEST_USERS.NON_MEMBER.email, HIVE_TEST_USERS.NON_MEMBER.password);
    
    // Create a test hive for joining scenarios
    const hiveData = generateUniqueHiveData('PUBLIC_STUDY_HIVE');
    await createHivePage.goto();
    await createHivePage.fillBasicInfo(hiveData);
    await createHivePage.addTags(['testing', 'e2e', 'joining']);
    testHive = await createHivePage.createHive();
  });

  test.afterAll(async () => {
    await ownerPage.close();
    await memberPage.close();
    await nonMemberPage.close();
  });

  test.describe('Hive Discovery', () => {
    test('should browse and find public hives', async () => {
      // Act
      await discoverPage.goto();
      
      // Assert - Should see hive discovery page
      await expect(discoverPage.discoveryPage).toBeVisible();
      
      // Should see created test hive
      const hiveCards = await discoverPage.getHiveCards();
      const foundHive = hiveCards.find(hive => hive.name === testHive.name);
      
      expect(foundHive).toBeDefined();
      expect(foundHive?.isPrivate).toBe(false);
      expect(foundHive?.hasAvailableSlots).toBe(true);
    });

    test('should search hives by name', async () => {
      // Act
      await discoverPage.goto();
      await discoverPage.searchHives(testHive.name);
      
      // Assert
      const hiveCards = await discoverPage.getHiveCards();
      expect(hiveCards.length).toBeGreaterThan(0);
      
      // All results should match search query
      const allNamesMatch = hiveCards.every(hive => 
        hive.name.toLowerCase().includes(testHive.name.toLowerCase())
      );
      expect(allNamesMatch).toBe(true);
    });

    test('should filter hives by tags', async () => {
      // Act
      await discoverPage.goto();
      await discoverPage.filterByTags(['testing']);
      
      // Assert
      const resultsMatchFilter = await discoverPage.verifyResultsMatchFilters({
        tags: ['testing']
      });
      expect(resultsMatchFilter).toBe(true);
    });

    test('should filter by available slots', async () => {
      // Act
      await discoverPage.goto();
      await discoverPage.filterByAvailableSlots(true);
      
      // Assert
      const resultsMatchFilter = await discoverPage.verifyResultsMatchFilters({
        hasAvailableSlots: true
      });
      expect(resultsMatchFilter).toBe(true);
    });

    test('should sort hives by member count', async () => {
      // Act
      await discoverPage.goto();
      await discoverPage.sortResults('members', 'desc');
      
      // Assert
      const hiveCards = await discoverPage.getHiveCards();
      
      // Verify descending order by member count
      for (let i = 1; i < hiveCards.length; i++) {
        expect(hiveCards[i - 1].memberCount).toBeGreaterThanOrEqual(hiveCards[i].memberCount);
      }
    });

    test('should paginate through results', async () => {
      // Act
      await discoverPage.goto();
      
      const paginationInfo = await discoverPage.getPaginationInfo();
      
      if (paginationInfo.totalPages > 1) {
        // Go to next page
        await discoverPage.goToNextPage();
        
        const newPaginationInfo = await discoverPage.getPaginationInfo();
        expect(newPaginationInfo.currentPage).toBe(2);
        
        // Go back to first page
        await discoverPage.goToPreviousPage();
        
        const finalPaginationInfo = await discoverPage.getPaginationInfo();
        expect(finalPaginationInfo.currentPage).toBe(1);
      }
    });
  });

  test.describe('Public Hive Joining', () => {
    test('should join public hive directly', async () => {
      // Act
      await hivePage.goto(testHive.id);
      await hivePage.joinHive();
      
      // Assert - Should be joined immediately
      const hiveInfo = await hivePage.getHiveInfo();
      expect(hiveInfo.isJoined).toBe(true);
      expect(hiveInfo.memberCount).toBeGreaterThan(1); // Owner + new member
      
      // Should see member in member list
      const members = await hivePage.getMembers();
      const joinedMember = members.find(member => 
        member.username === HIVE_TEST_USERS.MEMBER_1.username
      );
      expect(joinedMember).toBeDefined();
    });

    test('should join hive from discovery page', async () => {
      // First leave the hive if already joined
      await hivePage.goto(testHive.id);
      const initialInfo = await hivePage.getHiveInfo();
      if (initialInfo.isJoined) {
        await hivePage.leaveHive();
      }
      
      // Act - Join from discovery page
      await discoverPage.goto();
      await discoverPage.searchHives(testHive.name);
      await discoverPage.joinHiveFromCard(testHive.id);
      
      // Assert
      await expect(nonMemberPage.locator('[data-testid="join-success-message"]')).toBeVisible();
      
      // Verify by navigating to hive
      await hivePage.goto(testHive.id);
      const hiveInfo = await hivePage.getHiveInfo();
      expect(hiveInfo.isJoined).toBe(true);
    });

    test('should show hive details before joining', async () => {
      // Act
      await discoverPage.goto();
      await discoverPage.clickHiveCard(testHive.id);
      
      // Assert - Should navigate to hive details page
      await expect(memberPage.locator('[data-testid="hive-workspace"]')).toBeVisible();
      
      // Should see hive information
      const hiveInfo = await hivePage.getHiveInfo();
      expect(hiveInfo.title).toBe(testHive.name);
      
      // Should see join button if not already joined
      if (!hiveInfo.isJoined) {
        await expect(hivePage.joinButton).toBeVisible();
      }
    });
  });

  test.describe('Private Hive Joining', () => {
    let privateHive: TestHive;

    test.beforeAll(async () => {
      // Create a private hive for testing
      const privateHiveData = generateUniqueHiveData('PRIVATE_WORK_HIVE');
      privateHiveData.isPrivate = true;
      
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(privateHiveData);
      await createHivePage.configureSettings({
        requireApproval: true
      });
      privateHive = await createHivePage.createHive();
    });

    test('should not show private hives in public discovery', async () => {
      // Act
      await discoverPage.goto();
      await discoverPage.searchHives(privateHive.name);
      
      // Assert - Private hive should not appear in public search
      const hiveCards = await discoverPage.getHiveCards();
      const foundPrivateHive = hiveCards.find(hive => hive.name === privateHive.name);
      expect(foundPrivateHive).toBeUndefined();
    });

    test('should request approval for private hive', async () => {
      // Act - Navigate directly to private hive (e.g., via invitation link)
      await hivePage.goto(privateHive.id);
      await hivePage.joinHive('I would like to join this private hive for collaboration.');
      
      // Assert - Should show request sent message
      await expect(memberPage.locator('[data-testid="join-request-sent"]')).toBeVisible();
      
      // Should not be immediately joined
      const hiveInfo = await hivePage.getHiveInfo();
      expect(hiveInfo.isJoined).toBe(false);
    });

    test('should allow owner to approve join requests', async () => {
      // Act - Switch to owner and approve request
      await ownerPage.goto(`/hives/${privateHive.id}/manage/requests`);
      
      // Should see pending join request
      await expect(ownerPage.locator('[data-testid="pending-join-request"]')).toBeVisible();
      
      // Approve the request
      await ownerPage.click('[data-testid="approve-join-request"]');
      
      // Assert
      await expect(ownerPage.locator('[data-testid="request-approved"]')).toBeVisible();
      
      // Member should now be able to access hive
      await hivePage.goto(privateHive.id);
      const hiveInfo = await hivePage.getHiveInfo();
      expect(hiveInfo.isJoined).toBe(true);
    });

    test('should allow owner to reject join requests', async () => {
      // Create another join request first
      await nonMemberAuth.loginUser(HIVE_TEST_USERS.MEMBER_2.email, HIVE_TEST_USERS.MEMBER_2.password);
      
      await hivePage.goto(privateHive.id);
      await hivePage.joinHive('Another join request');
      
      // Act - Owner rejects request
      await ownerPage.goto(`/hives/${privateHive.id}/manage/requests`);
      await ownerPage.click('[data-testid="reject-join-request"]');
      await ownerPage.fill('[data-testid="rejection-reason"]', 'Not a good fit for this hive.');
      await ownerPage.click('[data-testid="confirm-reject"]');
      
      // Assert
      await expect(ownerPage.locator('[data-testid="request-rejected"]')).toBeVisible();
      
      // Member should receive rejection notification
      await memberPage.goto('/notifications');
      await expect(memberPage.locator('[data-testid*="join-request-rejected"]')).toBeVisible();
    });
  });

  test.describe('Hive Member Limits', () => {
    let limitedHive: TestHive;

    test.beforeAll(async () => {
      // Create hive with limited capacity
      const limitedHiveData = generateUniqueHiveData('FULL_CAPACITY_HIVE');
      limitedHiveData.maxMembers = 2; // Owner + 1 member
      
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(limitedHiveData);
      limitedHive = await createHivePage.createHive();
      
      // Have first member join
      await hivePage.goto(limitedHive.id);
      await hivePage.joinHive();
    });

    test('should prevent joining when at capacity', async () => {
      // Act - Try to join with another user when at capacity
      await nonMemberPage.goto(`/hives/${limitedHive.id}`);
      
      // Assert - Join button should be disabled or show capacity message
      await expect(
        nonMemberPage.locator('[data-testid="hive-at-capacity"]')
      ).toBeVisible();
      
      const joinButton = nonMemberPage.locator('[data-testid="join-hive-button"]');
      if (await joinButton.isVisible()) {
        await expect(joinButton).toBeDisabled();
      }
    });

    test('should show waiting list option for full hive', async () => {
      // Act
      await nonMemberPage.goto(`/hives/${limitedHive.id}`);
      
      // Assert - Should offer waiting list option
      await expect(
        nonMemberPage.locator('[data-testid="join-waitlist-button"]')
      ).toBeVisible();
      
      // Join waiting list
      await nonMemberPage.click('[data-testid="join-waitlist-button"]');
      await expect(
        nonMemberPage.locator('[data-testid="waitlist-joined"]')
      ).toBeVisible();
    });

    test('should notify waitlist members when slot becomes available', async () => {
      // Act - Have a member leave to create space
      await hivePage.goto(limitedHive.id);
      await hivePage.leaveHive();
      
      // Assert - Waitlist member should be notified
      await nonMemberPage.goto('/notifications');
      await expect(
        nonMemberPage.locator('[data-testid="hive-slot-available"]')
      ).toBeVisible();
      
      // Should be able to join now
      await nonMemberPage.goto(`/hives/${limitedHive.id}`);
      await expect(
        nonMemberPage.locator('[data-testid="join-hive-button"]')
      ).toBeEnabled();
    });
  });

  test.describe('Leaving Hives', () => {
    let leaveTestHive: TestHive;

    test.beforeAll(async () => {
      // Create and join a hive for leave testing
      const hiveData = generateUniqueHiveData('LEAVE_TEST_HIVE');
      
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(hiveData);
      leaveTestHive = await createHivePage.createHive();
      
      // Have member join
      await hivePage.goto(leaveTestHive.id);
      await hivePage.joinHive();
    });

    test('should leave hive successfully', async () => {
      // Act
      await hivePage.goto(leaveTestHive.id);
      await hivePage.leaveHive();
      
      // Assert
      await expect(memberPage.locator('[data-testid="hive-left-message"]')).toBeVisible();
      
      // Should no longer be joined
      await hivePage.goto(leaveTestHive.id);
      const hiveInfo = await hivePage.getHiveInfo();
      expect(hiveInfo.isJoined).toBe(false);
      
      // Member count should decrease
      expect(hiveInfo.memberCount).toBeLessThan(2);
    });

    test('should show confirmation dialog before leaving', async () => {
      // Join again for this test
      await hivePage.goto(leaveTestHive.id);
      await hivePage.joinHive();
      
      // Act
      await hivePage.goto(leaveTestHive.id);
      await memberPage.click('[data-testid="hive-menu-button"]');
      await memberPage.click('[data-testid="leave-hive-option"]');
      
      // Assert - Confirmation dialog should appear
      const confirmDialog = memberPage.locator('[data-testid="leave-confirmation-dialog"]');
      await expect(confirmDialog).toBeVisible();
      
      // Cancel leave
      await memberPage.click('[data-testid="cancel-leave"]');
      await expect(confirmDialog).not.toBeVisible();
      
      // Should still be in hive
      const hiveInfo = await hivePage.getHiveInfo();
      expect(hiveInfo.isJoined).toBe(true);
    });

    test('should handle owner leaving (transfer ownership)', async () => {
      // First add another member to receive ownership
      await hivePage.goto(leaveTestHive.id);
      await hivePage.joinHive();
      
      // Act - Owner tries to leave
      await ownerPage.goto(`/hives/${leaveTestHive.id}`);
      await ownerPage.click('[data-testid="hive-menu-button"]');
      await ownerPage.click('[data-testid="leave-hive-option"]');
      
      // Assert - Should show ownership transfer dialog
      await expect(
        ownerPage.locator('[data-testid="ownership-transfer-dialog"]')
      ).toBeVisible();
      
      // Select new owner
      await ownerPage.click('[data-testid="select-new-owner"]');
      await ownerPage.click(`[data-testid="member-${HIVE_TEST_USERS.MEMBER_1.id}"]`);
      await ownerPage.click('[data-testid="confirm-transfer"]');
      
      // Assert - Ownership should be transferred
      await expect(
        ownerPage.locator('[data-testid="ownership-transferred"]')
      ).toBeVisible();
    });

    test('should prevent leaving if only owner remains', async () => {
      // Create hive with only owner
      const soloHiveData = generateUniqueHiveData('SOLO_HIVE');
      await createHivePage.goto();
      await createHivePage.fillBasicInfo(soloHiveData);
      const soloHive = await createHivePage.createHive();
      
      // Act - Try to leave as sole owner
      await ownerPage.goto(`/hives/${soloHive.id}`);
      await ownerPage.click('[data-testid="hive-menu-button"]');
      await ownerPage.click('[data-testid="leave-hive-option"]');
      
      // Assert - Should show delete hive option instead
      await expect(
        ownerPage.locator('[data-testid="delete-hive-dialog"]')
      ).toBeVisible();
    });
  });

  test.describe('Invitation System', () => {
    test('should send hive invitations', async () => {
      // Act
      await hivePage.goto(testHive.id);
      await hivePage.inviteMembers(['invited@test.com', 'another@test.com']);
      
      // Assert
      await expect(memberPage.locator('[data-testid="invitations-sent"]')).toBeVisible();
    });

    test('should accept hive invitation', async () => {
      // Simulate receiving invitation (this would normally be via email)
      const invitationToken = 'mock-invitation-token';
      
      // Act
      await nonMemberPage.goto(`/hives/invitation/${invitationToken}`);
      await nonMemberPage.click('[data-testid="accept-invitation"]');
      
      // Assert
      await expect(
        nonMemberPage.locator('[data-testid="invitation-accepted"]')
      ).toBeVisible();
      
      // Should be automatically joined to hive
      const currentUrl = nonMemberPage.url();
      expect(currentUrl).toContain(`/hives/${testHive.id}`);
    });

    test('should decline hive invitation', async () => {
      const invitationToken = 'another-mock-invitation-token';
      
      // Act
      await nonMemberPage.goto(`/hives/invitation/${invitationToken}`);
      await nonMemberPage.click('[data-testid="decline-invitation"]');
      await nonMemberPage.fill('[data-testid="decline-reason"]', 'Not interested at this time');
      await nonMemberPage.click('[data-testid="confirm-decline"]');
      
      // Assert
      await expect(
        nonMemberPage.locator('[data-testid="invitation-declined"]')
      ).toBeVisible();
      
      // Should not be joined to hive
      await nonMemberPage.goto(`/hives/${testHive.id}`);
      const hiveInfo = await hivePage.getHiveInfo();
      expect(hiveInfo.isJoined).toBe(false);
    });
  });

  test.describe('Multi-User Concurrent Joining', () => {
    test('should handle multiple users joining simultaneously', async () => {
      const multiUserHelper = new MultiUserHiveHelper();
      
      try {
        // Create multiple user sessions
        const users = [
          HIVE_TEST_USERS.MEMBER_1,
          HIVE_TEST_USERS.MEMBER_2,
          HIVE_TEST_USERS.MODERATOR
        ];
        
        const helpers = [];
        for (const user of users) {
          const helper = await multiUserHelper.createUserSession(browser, user);
          helpers.push(helper);
        }
        
        // Act - All users join the same hive simultaneously
        const joinPromises = helpers.map(helper => 
          helper.joinHive(testHive.id)
        );
        
        await Promise.all(joinPromises);
        
        // Assert - All users should be successfully joined
        const finalMemberCount = await helpers[0].getHiveMembers();
        expect(finalMemberCount.length).toBeGreaterThanOrEqual(users.length + 1); // +1 for owner
        
        // Verify presence is updated for all users
        const presenceVerified = await multiUserHelper.verifyConcurrentPresence(testHive.id);
        expect(presenceVerified).toBe(true);
        
      } finally {
        await multiUserHelper.cleanup();
      }
    });
  });

  test.describe('Member Management', () => {
    test('should display member roles correctly', async () => {
      // Act
      await hivePage.goto(testHive.id);
      const members = await hivePage.getMembers();
      
      // Assert
      const ownerMember = members.find(member => 
        member.username === HIVE_TEST_USERS.OWNER.username
      );
      expect(ownerMember).toBeDefined();
      
      // Owner should have special indicator
      await expect(
        memberPage.locator(`[data-testid="member-${HIVE_TEST_USERS.OWNER.id}"] [data-testid="owner-badge"]`)
      ).toBeVisible();
    });

    test('should show member activity status', async () => {
      // Act
      await hivePage.goto(testHive.id);
      await hivePage.updatePresenceStatus('active');
      
      // Assert - Member should show as active
      const members = await hivePage.getMembers();
      const activeMember = members.find(member => 
        member.username === HIVE_TEST_USERS.MEMBER_1.username
      );
      expect(activeMember?.isActive).toBe(true);
      expect(activeMember?.status).toContain('active');
    });
  });
});