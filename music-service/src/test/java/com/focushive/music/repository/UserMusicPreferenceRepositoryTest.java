package com.focushive.music.repository;

import com.focushive.music.model.MusicPreference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UserMusicPreferenceRepository.
 * 
 * Tests data access operations for user music preferences including
 * CRUD operations, custom queries, and business logic validations.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("User Music Preference Repository Tests")
class UserMusicPreferenceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserMusicPreferenceRepository repository;

    private MusicPreference testPreference;
    private final String TEST_USER_ID = "test-user-123";

    @BeforeEach
    void setUp() {
        testPreference = MusicPreference.builder()
            .userId(TEST_USER_ID)
            .preferredGenres(Arrays.asList("rock", "jazz", "electronic"))
            .preferredArtists(Arrays.asList("The Beatles", "Miles Davis"))
            .preferredEnergyLevel(7)
            .preferredMood("focus")
            .spotifyConnected(true)
            .spotifyAccessToken("test-access-token")
            .spotifyRefreshToken("test-refresh-token")
            .spotifyExpiresAt(LocalDateTime.now().plusHours(1))
            .lastListeningActivity(LocalDateTime.now().minusHours(2))
            .build();
    }

    @Test
    @DisplayName("Should save and retrieve music preference by user ID")
    void shouldSaveAndRetrieveByUserId() {
        // Given
        MusicPreference savedPreference = repository.save(testPreference);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<MusicPreference> found = repository.findByUserId(TEST_USER_ID);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(found.get().getPreferredGenres()).containsExactly("rock", "jazz", "electronic");
        assertThat(found.get().getPreferredEnergyLevel()).isEqualTo(7);
        assertThat(found.get().getSpotifyConnected()).isTrue();
    }

    @Test
    @DisplayName("Should check existence of preferences by user ID")
    void shouldCheckExistenceByUserId() {
        // Given
        repository.save(testPreference);
        entityManager.flush();

        // When & Then
        assertThat(repository.existsByUserId(TEST_USER_ID)).isTrue();
        assertThat(repository.existsByUserId("non-existent-user")).isFalse();
    }

    @Test
    @DisplayName("Should find users with Spotify connected")
    void shouldFindUsersWithSpotifyConnected() {
        // Given
        MusicPreference connectedUser = MusicPreference.builder()
            .userId("connected-user")
            .spotifyConnected(true)
            .build();
            
        MusicPreference disconnectedUser = MusicPreference.builder()
            .userId("disconnected-user")
            .spotifyConnected(false)
            .build();

        repository.saveAll(Arrays.asList(testPreference, connectedUser, disconnectedUser));
        entityManager.flush();

        // When
        List<MusicPreference> connectedUsers = repository.findAllWithSpotifyConnected();

        // Then
        assertThat(connectedUsers).hasSize(2);
        assertThat(connectedUsers)
            .extracting(MusicPreference::getUserId)
            .containsExactlyInAnyOrder(TEST_USER_ID, "connected-user");
    }

    @Test
    @DisplayName("Should find users with expired Spotify tokens")
    void shouldFindUsersWithExpiredTokens() {
        // Given
        LocalDateTime pastTime = LocalDateTime.now().minusHours(1);
        
        MusicPreference expiredTokenUser = MusicPreference.builder()
            .userId("expired-user")
            .spotifyConnected(true)
            .spotifyExpiresAt(pastTime)
            .build();

        repository.saveAll(Arrays.asList(testPreference, expiredTokenUser));
        entityManager.flush();

        // When
        List<MusicPreference> expiredUsers = repository.findWithExpiredSpotifyTokens(LocalDateTime.now());

        // Then
        assertThat(expiredUsers).hasSize(1);
        assertThat(expiredUsers.get(0).getUserId()).isEqualTo("expired-user");
    }

    @Test
    @DisplayName("Should find users by preferred genre")
    void shouldFindUsersByPreferredGenre() {
        // Given
        MusicPreference jazzLover = MusicPreference.builder()
            .userId("jazz-lover")
            .preferredGenres(Arrays.asList("jazz", "blues"))
            .build();

        MusicPreference rockFan = MusicPreference.builder()
            .userId("rock-fan")
            .preferredGenres(Arrays.asList("rock", "metal"))
            .build();

        repository.saveAll(Arrays.asList(testPreference, jazzLover, rockFan));
        entityManager.flush();

        // When
        List<MusicPreference> jazzUsers = repository.findByPreferredGenresContaining("jazz");

        // Then
        assertThat(jazzUsers).hasSize(2);
        assertThat(jazzUsers)
            .extracting(MusicPreference::getUserId)
            .containsExactlyInAnyOrder(TEST_USER_ID, "jazz-lover");
    }

    @Test
    @DisplayName("Should find users by energy level range")
    void shouldFindUsersByEnergyLevelRange() {
        // Given
        MusicPreference lowEnergyUser = MusicPreference.builder()
            .userId("low-energy")
            .preferredEnergyLevel(3)
            .build();

        MusicPreference highEnergyUser = MusicPreference.builder()
            .userId("high-energy")
            .preferredEnergyLevel(9)
            .build();

        repository.saveAll(Arrays.asList(testPreference, lowEnergyUser, highEnergyUser));
        entityManager.flush();

        // When
        List<MusicPreference> mediumEnergyUsers = repository.findByEnergyLevelRange(5, 8);

        // Then
        assertThat(mediumEnergyUsers).hasSize(1);
        assertThat(mediumEnergyUsers.get(0).getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(mediumEnergyUsers.get(0).getPreferredEnergyLevel()).isEqualTo(7);
    }

    @Test
    @DisplayName("Should find inactive users")
    void shouldFindInactiveUsers() {
        // Given
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        
        MusicPreference inactiveUser = MusicPreference.builder()
            .userId("inactive-user")
            .lastListeningActivity(LocalDateTime.now().minusHours(3))
            .build();

        MusicPreference activeUser = MusicPreference.builder()
            .userId("active-user")
            .lastListeningActivity(LocalDateTime.now().minusMinutes(30))
            .build();

        repository.saveAll(Arrays.asList(testPreference, inactiveUser, activeUser));
        entityManager.flush();

        // When
        List<MusicPreference> inactiveUsers = repository.findInactiveUsers(threshold);

        // Then
        assertThat(inactiveUsers).hasSize(2);
        assertThat(inactiveUsers)
            .extracting(MusicPreference::getUserId)
            .containsExactlyInAnyOrder(TEST_USER_ID, "inactive-user");
    }

    @Test
    @DisplayName("Should update last listening activity")
    void shouldUpdateLastListeningActivity() {
        // Given
        repository.save(testPreference);
        entityManager.flush();
        LocalDateTime newTimestamp = LocalDateTime.now();

        // When
        int updatedCount = repository.updateLastListeningActivity(TEST_USER_ID, newTimestamp);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(updatedCount).isEqualTo(1);
        
        Optional<MusicPreference> updated = repository.findByUserId(TEST_USER_ID);
        assertThat(updated).isPresent();
        assertThat(updated.get().getLastListeningActivity()).isEqualToIgnoringNanos(newTimestamp);
    }

    @Test
    @DisplayName("Should count users with Spotify connected")
    void shouldCountUsersWithSpotifyConnected() {
        // Given
        MusicPreference connectedUser = MusicPreference.builder()
            .userId("connected-user")
            .spotifyConnected(true)
            .build();
            
        MusicPreference disconnectedUser = MusicPreference.builder()
            .userId("disconnected-user")
            .spotifyConnected(false)
            .build();

        repository.saveAll(Arrays.asList(testPreference, connectedUser, disconnectedUser));
        entityManager.flush();

        // When
        long count = repository.countUsersWithSpotifyConnected();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle token expiration check correctly")
    void shouldHandleTokenExpirationCheck() {
        // Given
        repository.save(testPreference);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<MusicPreference> preference = repository.findByUserId(TEST_USER_ID);

        // Then
        assertThat(preference).isPresent();
        assertThat(preference.get().isSpotifyTokenExpired()).isFalse(); // Token expires in 1 hour
        
        // Test with expired token
        preference.get().setSpotifyExpiresAt(LocalDateTime.now().minusHours(1));
        assertThat(preference.get().isSpotifyTokenExpired()).isTrue();
    }
}