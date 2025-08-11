package com.focushive.forum.repository;

import com.focushive.forum.entity.ForumCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumCategoryRepository extends JpaRepository<ForumCategory, Long> {
    
    Optional<ForumCategory> findBySlug(String slug);
    
    List<ForumCategory> findByIsActiveOrderBySortOrderAsc(Boolean isActive);
    
    List<ForumCategory> findByHiveIdOrderBySortOrderAsc(Long hiveId);
    
    @Query("SELECT fc FROM ForumCategory fc " +
           "WHERE fc.hive IS NULL " +
           "AND fc.isActive = true " +
           "ORDER BY fc.sortOrder ASC")
    List<ForumCategory> findGlobalCategories();
    
    @Query("SELECT fc FROM ForumCategory fc " +
           "WHERE fc.parent IS NULL " +
           "AND fc.isActive = true " +
           "ORDER BY fc.sortOrder ASC")
    List<ForumCategory> findRootCategories();
    
    List<ForumCategory> findByParentIdOrderBySortOrderAsc(Long parentId);
    
    @Query("SELECT fc FROM ForumCategory fc " +
           "WHERE (fc.hive.id = :hiveId OR fc.hive IS NULL) " +
           "AND fc.isActive = true " +
           "ORDER BY fc.hive.id DESC, fc.sortOrder ASC")
    List<ForumCategory> findCategoriesForHive(@Param("hiveId") Long hiveId);
    
    @Query("SELECT fc FROM ForumCategory fc " +
           "LEFT JOIN FETCH fc.children " +
           "WHERE fc.parent IS NULL " +
           "AND fc.isActive = true " +
           "ORDER BY fc.sortOrder ASC")
    List<ForumCategory> findRootCategoriesWithChildren();
    
    @Query("SELECT COUNT(fp) FROM ForumPost fp " +
           "WHERE fp.category.id = :categoryId " +
           "AND fp.isDeleted = false")
    Long countPostsInCategory(@Param("categoryId") Long categoryId);
    
    @Query("SELECT fc FROM ForumCategory fc " +
           "WHERE fc.name LIKE %:searchTerm% " +
           "AND fc.isActive = true " +
           "ORDER BY fc.sortOrder ASC")
    List<ForumCategory> searchByName(@Param("searchTerm") String searchTerm);
}