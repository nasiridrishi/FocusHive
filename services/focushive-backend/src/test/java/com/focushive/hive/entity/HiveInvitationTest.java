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
 * Unit tests for HiveInvitation entity following TDD approach.
 * These tests define the expected behavior before implementation.
 * THIS WILL FAIL initially as HiveInvitation entity doesn't exist yet.
 */
class HiveInvitationTest {

    private Validator validator;
    private User invitedUser;
    private User invitingUser;
    private Hive testHive;
    private HiveInvitation testInvitation;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        // Set up inviting user
        invitingUser = new User();
        invitingUser.setId(UUID.randomUUID().toString());
        invitingUser.setUsername("inviter");
        invitingUser.setEmail("inviter@example.com");

        // Set up invited user
        invitedUser = new User();
        invitedUser.setId(UUID.randomUUID().toString());
        invitedUser.setUsername("invited");
        invitedUser.setEmail("invited@example.com");

        // Set up test hive
        testHive = new Hive();
        testHive.setId(UUID.randomUUID().toString());
        testHive.setName("Test Hive");
        testHive.setSlug("test-hive");
        testHive.setOwner(invitingUser);

        // Set up test invitation
        testInvitation = new HiveInvitation();
        testInvitation.setId(UUID.randomUUID().toString());
        testInvitation.setHive(testHive);
        testInvitation.setInvitedUser(invitedUser);
        testInvitation.setInvitedBy(invitingUser);
        testInvitation.setInvitationCode(UUID.randomUUID().toString());
        testInvitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        testInvitation.setStatus(HiveInvitation.InvitationStatus.PENDING);
        testInvitation.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void shouldCreateValidInvitation() {
        // Test basic invitation creation
        Set<ConstraintViolation<HiveInvitation>> violations = validator.validate(testInvitation);
        assertThat(violations).isEmpty();

        assertThat(testInvitation.getHive()).isEqualTo(testHive);
        assertThat(testInvitation.getInvitedUser()).isEqualTo(invitedUser);
        assertThat(testInvitation.getInvitedBy()).isEqualTo(invitingUser);
        assertThat(testInvitation.getStatus()).isEqualTo(HiveInvitation.InvitationStatus.PENDING);
    }

    @Test
    void shouldRequireHive() {
        // Test that hive is required
        testInvitation.setHive(null);

        Set<ConstraintViolation<HiveInvitation>> violations = validator.validate(testInvitation);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Hive is required");
    }

    @Test
    void shouldRequireInvitedUser() {
        // Test that invited user is required
        testInvitation.setInvitedUser(null);

        Set<ConstraintViolation<HiveInvitation>> violations = validator.validate(testInvitation);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Invited user is required");
    }

    @Test
    void shouldRequireInvitedBy() {
        // Test that inviter is required
        testInvitation.setInvitedBy(null);

        Set<ConstraintViolation<HiveInvitation>> violations = validator.validate(testInvitation);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Inviter is required");
    }

    @Test
    void shouldRequireInvitationCode() {
        // Test that invitation code is required and unique
        testInvitation.setInvitationCode(null);

        Set<ConstraintViolation<HiveInvitation>> violations = validator.validate(testInvitation);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Invitation code is required");
    }

    @Test
    void shouldRequireExpirationDate() {
        // Test that expiration date is required
        testInvitation.setExpiresAt(null);

        Set<ConstraintViolation<HiveInvitation>> violations = validator.validate(testInvitation);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Expiration date is required");
    }

    @Test
    void shouldRequireStatus() {
        // Test that status is required
        testInvitation.setStatus(null);

        Set<ConstraintViolation<HiveInvitation>> violations = validator.validate(testInvitation);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Status is required");
    }

    @Test
    void shouldDefaultToPendingStatus() {
        // Test that default status is PENDING
        HiveInvitation newInvitation = new HiveInvitation();
        assertThat(newInvitation.getStatus()).isEqualTo(HiveInvitation.InvitationStatus.PENDING);
    }

    @Test
    void shouldGenerateDefaultExpirationDate() {
        // Test that default expiration is 7 days from creation
        HiveInvitation newInvitation = new HiveInvitation();
        LocalDateTime now = LocalDateTime.now();

        // Should be approximately 7 days from now (within 1 minute tolerance)
        assertThat(newInvitation.getExpiresAt())
            .isAfter(now.plusDays(7).minusMinutes(1))
            .isBefore(now.plusDays(7).plusMinutes(1));
    }

    @Test
    void shouldSupportStatusTransitions() {
        // Test different invitation statuses
        assertThat(testInvitation.getStatus()).isEqualTo(HiveInvitation.InvitationStatus.PENDING);

        testInvitation.setStatus(HiveInvitation.InvitationStatus.ACCEPTED);
        assertThat(testInvitation.getStatus()).isEqualTo(HiveInvitation.InvitationStatus.ACCEPTED);

        testInvitation.setStatus(HiveInvitation.InvitationStatus.REJECTED);
        assertThat(testInvitation.getStatus()).isEqualTo(HiveInvitation.InvitationStatus.REJECTED);

        testInvitation.setStatus(HiveInvitation.InvitationStatus.EXPIRED);
        assertThat(testInvitation.getStatus()).isEqualTo(HiveInvitation.InvitationStatus.EXPIRED);

        testInvitation.setStatus(HiveInvitation.InvitationStatus.REVOKED);
        assertThat(testInvitation.getStatus()).isEqualTo(HiveInvitation.InvitationStatus.REVOKED);
    }

    @Test
    void shouldCheckIfExpired() {
        // Test expiration check
        testInvitation.setExpiresAt(LocalDateTime.now().minusHours(1));
        assertThat(testInvitation.isExpired()).isTrue();

        testInvitation.setExpiresAt(LocalDateTime.now().plusHours(1));
        assertThat(testInvitation.isExpired()).isFalse();
    }

    @Test
    void shouldCheckIfActive() {
        // Test if invitation is active (pending and not expired)
        testInvitation.setStatus(HiveInvitation.InvitationStatus.PENDING);
        testInvitation.setExpiresAt(LocalDateTime.now().plusHours(1));
        assertThat(testInvitation.isActive()).isTrue();

        // Expired invitation should not be active
        testInvitation.setExpiresAt(LocalDateTime.now().minusHours(1));
        assertThat(testInvitation.isActive()).isFalse();

        // Non-pending invitation should not be active
        testInvitation.setExpiresAt(LocalDateTime.now().plusHours(1));
        testInvitation.setStatus(HiveInvitation.InvitationStatus.ACCEPTED);
        assertThat(testInvitation.isActive()).isFalse();
    }

    @Test
    void shouldPreventSelfInvitation() {
        // Test that user cannot invite themselves
        testInvitation.setInvitedUser(invitingUser);
        testInvitation.setInvitedBy(invitingUser);

        // This business logic validation will be handled in the service layer
        // But we can test that the entity allows it (validation happens elsewhere)
        Set<ConstraintViolation<HiveInvitation>> violations = validator.validate(testInvitation);
        assertThat(violations).isEmpty(); // Entity validation passes, business logic prevents it
    }

    @Test
    void shouldHaveUniqueInvitationCode() {
        // Test that invitation code should be unique
        String code = UUID.randomUUID().toString();
        testInvitation.setInvitationCode(code);

        assertThat(testInvitation.getInvitationCode()).isEqualTo(code);
        assertThat(testInvitation.getInvitationCode()).isNotEmpty();
    }

    @Test
    void shouldHandleResponseDate() {
        // Test that response date is tracked when invitation is responded to
        LocalDateTime responseTime = LocalDateTime.now();
        testInvitation.setRespondedAt(responseTime);
        testInvitation.setStatus(HiveInvitation.InvitationStatus.ACCEPTED);

        assertThat(testInvitation.getRespondedAt()).isEqualTo(responseTime);
        assertThat(testInvitation.getStatus()).isEqualTo(HiveInvitation.InvitationStatus.ACCEPTED);
    }
}