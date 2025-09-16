package com.focushive.identity.repository;

import com.focushive.identity.entity.ConsentRecord;
import com.focushive.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ConsentRecord entity.
 */
@Repository
public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, Long> {

    /**
     * Find all consent records for a user.
     */
    List<ConsentRecord> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find active consent records for a user.
     */
    List<ConsentRecord> findByUserAndActiveTrue(User user);

    /**
     * Find consent record by user and purpose.
     */
    Optional<ConsentRecord> findByUserAndPurpose(User user, String purpose);
}