package com.focushive.buddy.repository;

import com.focushive.buddy.AbstractTestContainersTest;
import com.focushive.buddy.entity.BuddyPreferences;
import com.focushive.buddy.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compilation test to verify all repository methods are properly defined.
 * This test verifies method signatures compile without actual database execution.
 */
public class RepositoryMethodsCompilationTest extends AbstractTestContainersTest {

    @Test
    @DisplayName("MatchingPreferencesRepository methods should compile")
    void matchingPreferencesRepositoryMethodsShouldCompile() {
        // This test verifies that all our new repository methods compile correctly
        // We're not testing functionality here, just that the methods exist and have correct signatures

        assertDoesNotThrow(() -> {
            // Verify method signatures exist (compilation check)
            MatchingPreferencesRepository.class.getDeclaredMethod("findByFocusAreasIn", String[].class);
            MatchingPreferencesRepository.class.getDeclaredMethod("findByGoalsIn", String[].class);
            MatchingPreferencesRepository.class.getDeclaredMethod("findByAnyFocusArea", String[].class);
            MatchingPreferencesRepository.class.getDeclaredMethod("findByAnyGoal", String[].class);
            MatchingPreferencesRepository.class.getDeclaredMethod("findPotentialMatches",
                UUID.class, LocalDateTime.class, Integer.class, String.class, Integer.class);
            MatchingPreferencesRepository.class.getDeclaredMethod("findHighCompatibilityMatches",
                UUID.class, String[].class, String[].class, String.class, LocalDateTime.class, Integer.class);
            MatchingPreferencesRepository.class.getDeclaredMethod("countFocusAreaOverlap", UUID.class, UUID.class);
        }, "MatchingPreferencesRepository methods should compile successfully");
    }

    @Test
    @DisplayName("UserRepository methods should compile")
    void userRepositoryMethodsShouldCompile() {
        // This test verifies that all our new UserRepository methods compile correctly

        assertDoesNotThrow(() -> {
            // Verify method signatures exist (compilation check)
            UserRepository.class.getDeclaredMethod("findByInterestsIn", String[].class);
            UserRepository.class.getDeclaredMethod("findByAnyInterest", String[].class);
            UserRepository.class.getDeclaredMethod("findByPreferredFocusTimesIn", String[].class);
            UserRepository.class.getDeclaredMethod("findCompatibleUsers",
                String[].class, String.class, String.class, String.class, LocalDateTime.class, Integer.class);
        }, "UserRepository methods should compile successfully");
    }

    @Test
    @DisplayName("All PostgreSQL array query annotations should be present")
    void postgresqlArrayQueryAnnotationsShouldBePresent() {
        assertDoesNotThrow(() -> {
            // Verify @Query annotations are present on our native query methods
            var findByFocusAreasIn = MatchingPreferencesRepository.class.getDeclaredMethod("findByFocusAreasIn", String[].class);
            assertNotNull(findByFocusAreasIn.getAnnotation(org.springframework.data.jpa.repository.Query.class));

            var findByInterestsIn = UserRepository.class.getDeclaredMethod("findByInterestsIn", String[].class);
            assertNotNull(findByInterestsIn.getAnnotation(org.springframework.data.jpa.repository.Query.class));
        }, "PostgreSQL array query annotations should be present");
    }

    @Test
    @DisplayName("All method parameter annotations should be present")
    void parameterAnnotationsShouldBePresent() {
        assertDoesNotThrow(() -> {
            // Verify @Param annotations are present where needed
            var findPotentialMatches = MatchingPreferencesRepository.class.getDeclaredMethod("findPotentialMatches",
                UUID.class, LocalDateTime.class, Integer.class, String.class, Integer.class);

            // Check that parameters have @Param annotations
            var parameters = findPotentialMatches.getParameters();
            assertTrue(parameters.length > 0, "Method should have parameters");

            // At least some parameters should have @Param annotations
            boolean hasParamAnnotations = false;
            for (var param : parameters) {
                if (param.getAnnotation(org.springframework.data.repository.query.Param.class) != null) {
                    hasParamAnnotations = true;
                    break;
                }
            }
            assertTrue(hasParamAnnotations, "Method parameters should have @Param annotations");
        }, "Parameter annotations should be present");
    }
}