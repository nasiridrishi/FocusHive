package com.focushive.hive.repository;

import com.focushive.hive.entity.Hive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface HiveRepository extends JpaRepository<Hive, String> {
    
    // Find by unique fields
    Optional<Hive> findBySlug(String slug);
    
    // Find active hives
    @Query("SELECT h FROM Hive h WHERE h.id = :id AND h.deletedAt IS NULL AND h.isActive = true")
    Optional<Hive> findByIdAndActive(@Param("id") String id);
    
    @Query("SELECT h FROM Hive h WHERE h.slug = :slug AND h.deletedAt IS NULL AND h.isActive = true")
    Optional<Hive> findBySlugAndActive(@Param("slug") String slug);
    
    // Find public hives
    @Query("SELECT h FROM Hive h WHERE h.isPublic = true AND h.isActive = true AND h.deletedAt IS NULL")
    Page<Hive> findPublicHives(Pageable pageable);
    
    // Find by owner
    @Query("SELECT h FROM Hive h WHERE h.owner.id = :ownerId AND h.deletedAt IS NULL")
    Page<Hive> findByOwnerId(@Param("ownerId") String ownerId, Pageable pageable);
    
    // Search hives
    @Query("SELECT h FROM Hive h WHERE " +
           "(LOWER(h.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(h.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND h.isPublic = true AND h.isActive = true AND h.deletedAt IS NULL")
    Page<Hive> searchPublicHives(@Param("query") String query, Pageable pageable);
    
    // Find by type
    @Query("SELECT h FROM Hive h WHERE h.type = :type AND h.isPublic = true AND h.isActive = true AND h.deletedAt IS NULL")
    Page<Hive> findByType(@Param("type") Hive.HiveType type, Pageable pageable);
    
    // Find by tags - This requires PostgreSQL-specific array support
    // For H2 tests, this method won't work. Consider using a separate tags table for cross-database compatibility
    // @Query(value = "SELECT h.* FROM hives h WHERE :tag = ANY(h.tags) AND h.is_public = true AND h.is_active = true AND h.deleted_at IS NULL",
    //        nativeQuery = true)
    // Page<Hive> findByTag(@Param("tag") String tag, Pageable pageable);
    
    // Update statistics
    @Modifying
    @Query("UPDATE Hive h SET h.totalFocusMinutes = h.totalFocusMinutes + :minutes WHERE h.id = :hiveId")
    void incrementTotalFocusMinutes(@Param("hiveId") String hiveId, @Param("minutes") Long minutes);
    
    // Check existence
    boolean existsBySlug(String slug);
    
    // Popular hives
    @Query("SELECT h FROM Hive h WHERE h.isPublic = true AND h.isActive = true AND h.deletedAt IS NULL " +
           "ORDER BY h.memberCount DESC")
    Page<Hive> findPopularHives(Pageable pageable);
    
    // Recent hives
    @Query("SELECT h FROM Hive h WHERE h.isPublic = true AND h.isActive = true AND h.deletedAt IS NULL " +
           "ORDER BY h.createdAt DESC")
    Page<Hive> findRecentHives(Pageable pageable);
    
    // Statistics
    @Query("SELECT COUNT(h) FROM Hive h WHERE h.deletedAt IS NULL AND h.isActive = true")
    long countActiveHives();

    @Query("SELECT COUNT(h) FROM Hive h WHERE h.isPublic = true AND h.deletedAt IS NULL AND h.isActive = true")
    long countPublicHives();

    // Additional count methods for enhanced testing
    @Query("SELECT COUNT(h) FROM Hive h WHERE h.isActive = :isActive AND h.deletedAt IS NULL")
    long countByIsActiveAndDeletedAtIsNull(@Param("isActive") boolean isActive);

    // Enhanced validation queries
    @Query("SELECT COUNT(h) FROM Hive h WHERE h.owner.id = :ownerId AND h.deletedAt IS NULL")
    long countByOwnerIdAndDeletedAtIsNull(@Param("ownerId") String ownerId);

    @Query("SELECT COUNT(h) > 0 FROM Hive h WHERE h.name = :name AND h.owner.id = :ownerId AND h.deletedAt IS NULL")
    boolean existsByNameAndOwnerIdAndDeletedAtIsNull(@Param("name") String name, @Param("ownerId") String ownerId);
}