package com.focushive.identity.repository;

import com.focushive.identity.entity.PrivacySetting;
import com.focushive.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for PrivacySetting entity.
 */
@Repository
public interface PrivacySettingRepository extends JpaRepository<PrivacySetting, Long> {

    /**
     * Find privacy settings for a user.
     */
    Optional<PrivacySetting> findByUser(User user);

    /**
     * Check if privacy settings exist for a user.
     */
    boolean existsByUser(User user);
}