package com.focushive.music.repository;

import com.focushive.music.entity.MusicPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for MusicPreference entity.
 */
@Repository
public interface MusicPreferenceRepository extends JpaRepository<MusicPreference, UUID> {

    Optional<MusicPreference> findByUserId(String userId);

    void deleteByUserId(String userId);

    boolean existsByUserId(String userId);
}