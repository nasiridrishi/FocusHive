package com.focushive.identity.repository;

import com.focushive.identity.entity.DataPermission;
import com.focushive.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DataPermission entity.
 */
@Repository
public interface DataPermissionRepository extends JpaRepository<DataPermission, Long> {

    /**
     * Find all permissions for a user.
     */
    List<DataPermission> findByUser(User user);

    /**
     * Find all active permissions for a user.
     */
    List<DataPermission> findByUserAndActiveTrue(User user);

    /**
     * Find permission by user and data type.
     */
    Optional<DataPermission> findByUserAndDataType(User user, String dataType);

    /**
     * Find all permissions for a specific data type.
     */
    List<DataPermission> findByDataType(String dataType);
}