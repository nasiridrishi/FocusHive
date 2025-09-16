package com.focushive.hive.entity;

import com.focushive.user.entity.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HiveMember entity following TDD approach.
 * These tests define the expected behavior before implementation.
 */
class HiveMemberTest {

    private Validator validator;
    private User testUser;
    private User invitingUser;
    private Hive testHive;
    private HiveMember testMember;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        // Set up test user
        testUser = new User();
        testUser.setId(UUID.randomUUID().toString());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // Set up inviting user
        invitingUser = new User();
        invitingUser.setId(UUID.randomUUID().toString());
        invitingUser.setUsername("inviter");
        invitingUser.setEmail("inviter@example.com");

        // Set up test hive
        testHive = new Hive();
        testHive.setId(UUID.randomUUID().toString());
        testHive.setName("Test Hive");
        testHive.setSlug("test-hive");
        testHive.setOwner(invitingUser);

        // Set up test member
        testMember = new HiveMember();
        testMember.setId(UUID.randomUUID().toString());
        testMember.setHive(testHive);
        testMember.setUser(testUser);
        testMember.setRole(HiveMember.MemberRole.MEMBER);
        testMember.setJoinedAt(LocalDateTime.now());
    }

    @Test
    void shouldCreateMemberWithRole() {
        // Test that a member can be created with a specific role
        HiveMember member = new HiveMember();
        member.setHive(testHive);
        member.setUser(testUser);
        member.setRole(HiveMember.MemberRole.MODERATOR);
        member.setJoinedAt(LocalDateTime.now());

        Set<ConstraintViolation<HiveMember>> violations = validator.validate(member);
        assertThat(violations).isEmpty();
        assertThat(member.getRole()).isEqualTo(HiveMember.MemberRole.MODERATOR);
        assertThat(member.getUser()).isEqualTo(testUser);
        assertThat(member.getHive()).isEqualTo(testHive);
    }

    @Test
    void shouldTrackJoinDate() {
        // Test that join date is tracked and cannot be null
        LocalDateTime joinTime = LocalDateTime.now().minusDays(5);
        testMember.setJoinedAt(joinTime);

        Set<ConstraintViolation<HiveMember>> violations = validator.validate(testMember);
        assertThat(violations).isEmpty();
        assertThat(testMember.getJoinedAt()).isEqualTo(joinTime);
    }

    @Test
    void shouldUpdateMemberRole() {
        // Test that member role can be updated
        assertThat(testMember.getRole()).isEqualTo(HiveMember.MemberRole.MEMBER);

        testMember.setRole(HiveMember.MemberRole.MODERATOR);
        assertThat(testMember.getRole()).isEqualTo(HiveMember.MemberRole.MODERATOR);

        Set<ConstraintViolation<HiveMember>> violations = validator.validate(testMember);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldPreventDuplicateMembers() {
        // This will be validated at the database level with unique constraint
        // Testing that the entity has the expected fields for uniqueness
        assertThat(testMember.getHive()).isNotNull();
        assertThat(testMember.getUser()).isNotNull();

        // Two members with same hive and user should be prevented at DB level
        HiveMember duplicateMember = new HiveMember();
        duplicateMember.setHive(testHive);
        duplicateMember.setUser(testUser);
        duplicateMember.setRole(HiveMember.MemberRole.MEMBER);
        duplicateMember.setJoinedAt(LocalDateTime.now());

        Set<ConstraintViolation<HiveMember>> violations = validator.validate(duplicateMember);
        assertThat(violations).isEmpty(); // Validation should pass, DB constraint will prevent duplicates
    }

    @Test
    void shouldRequireHive() {
        // Test that hive is required
        testMember.setHive(null);

        Set<ConstraintViolation<HiveMember>> violations = validator.validate(testMember);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Hive is required");
    }

    @Test
    void shouldRequireUser() {
        // Test that user is required
        testMember.setUser(null);

        Set<ConstraintViolation<HiveMember>> violations = validator.validate(testMember);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("User is required");
    }

    @Test
    void shouldRequireRole() {
        // Test that role is required
        testMember.setRole(null);

        Set<ConstraintViolation<HiveMember>> violations = validator.validate(testMember);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Role is required");
    }

    @Test
    void shouldRequireJoinedAt() {
        // Test that joinedAt is required
        testMember.setJoinedAt(null);

        Set<ConstraintViolation<HiveMember>> violations = validator.validate(testMember);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Joined date is required");
    }

    @Test
    void shouldHaveValidTotalMinutes() {
        // Test total minutes validation
        testMember.setTotalMinutes(-5);

        Set<ConstraintViolation<HiveMember>> violations = validator.validate(testMember);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Total minutes cannot be negative");

        // Valid value should pass
        testMember.setTotalMinutes(120);
        violations = validator.validate(testMember);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldHaveValidConsecutiveDays() {
        // Test consecutive days validation
        testMember.setConsecutiveDays(-1);

        Set<ConstraintViolation<HiveMember>> violations = validator.validate(testMember);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Consecutive days cannot be negative");

        // Valid value should pass
        testMember.setConsecutiveDays(7);
        violations = validator.validate(testMember);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldSupportInvitationTracking() {
        // Test that member can track who invited them - THIS WILL FAIL initially
        // We need to add invitedBy field to HiveMember entity
        testMember.setInvitedBy(invitingUser);
        assertThat(testMember.getInvitedBy()).isEqualTo(invitingUser);
    }

    @Test
    void shouldSupportMembershipStatus() {
        // Test that member can have different statuses - THIS WILL FAIL initially
        // We need to add status field to HiveMember entity
        testMember.setStatus(HiveMember.MemberStatus.ACTIVE);
        assertThat(testMember.getStatus()).isEqualTo(HiveMember.MemberStatus.ACTIVE);

        testMember.setStatus(HiveMember.MemberStatus.INVITED);
        assertThat(testMember.getStatus()).isEqualTo(HiveMember.MemberStatus.INVITED);

        testMember.setStatus(HiveMember.MemberStatus.BANNED);
        assertThat(testMember.getStatus()).isEqualTo(HiveMember.MemberStatus.BANNED);
    }

    @Test
    void shouldDefaultToMemberRole() {
        // Test default role is MEMBER
        HiveMember newMember = new HiveMember();
        assertThat(newMember.getRole()).isEqualTo(HiveMember.MemberRole.MEMBER);
    }

    @Test
    void shouldDefaultToZeroMinutes() {
        // Test default total minutes is 0
        HiveMember newMember = new HiveMember();
        assertThat(newMember.getTotalMinutes()).isEqualTo(0);
    }

    @Test
    void shouldDefaultToNotMuted() {
        // Test default muted status is false
        HiveMember newMember = new HiveMember();
        assertThat(newMember.getIsMuted()).isFalse();
    }

    @Test
    void shouldDefaultToActiveStatus() {
        // Test default status is ACTIVE - THIS WILL FAIL initially
        HiveMember newMember = new HiveMember();
        assertThat(newMember.getStatus()).isEqualTo(HiveMember.MemberStatus.ACTIVE);
    }

    @Test
    void shouldSupportRoleHierarchy() {
        // Test that role hierarchy is properly defined
        // OWNER > MODERATOR > MEMBER
        assertThat(HiveMember.MemberRole.OWNER.ordinal()).isLessThan(HiveMember.MemberRole.MODERATOR.ordinal());
        assertThat(HiveMember.MemberRole.MODERATOR.ordinal()).isLessThan(HiveMember.MemberRole.MEMBER.ordinal());
    }
}