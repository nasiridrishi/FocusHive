package com.focushive.identity.repository;

import com.focushive.identity.entity.DataExportRequest;
import com.focushive.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for DataExportRequest entity.
 */
@Repository
public interface DataExportRequestRepository extends JpaRepository<DataExportRequest, UUID> {

    /**
     * Find all export requests for a user.
     */
    List<DataExportRequest> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find pending export requests for a user.
     */
    List<DataExportRequest> findByUserAndStatus(User user, String status);

    /**
     * Find the most recent export request for a user.
     */
    Optional<DataExportRequest> findFirstByUserOrderByCreatedAtDesc(User user);

    /**
     * Find export requests by status.
     */
    List<DataExportRequest> findByStatus(String status);
}