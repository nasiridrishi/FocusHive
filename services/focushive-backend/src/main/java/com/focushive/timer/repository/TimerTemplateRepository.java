package com.focushive.timer.repository;

import com.focushive.timer.entity.TimerTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TimerTemplate entity.
 */
@Repository
public interface TimerTemplateRepository extends JpaRepository<TimerTemplate, String> {

    /**
     * Find templates by user ID.
     */
    List<TimerTemplate> findByUserIdOrderByUsageCountDesc(String userId);

    /**
     * Find system templates.
     */
    List<TimerTemplate> findByIsSystemTrueOrderByName();

    /**
     * Find user's default template.
     */
    Optional<TimerTemplate> findByUserIdAndIsDefaultTrue(String userId);

    /**
     * Find public templates.
     */
    List<TimerTemplate> findByIsPublicTrueOrderByUsageCountDesc();

    /**
     * Find template by name and user.
     */
    Optional<TimerTemplate> findByNameAndUserId(String name, String userId);

    /**
     * Count templates for a user.
     */
    @Query("SELECT COUNT(t) FROM TimerTemplate t WHERE t.userId = :userId")
    Long countByUserId(@Param("userId") String userId);

    /**
     * Find most used templates.
     */
    @Query("SELECT t FROM TimerTemplate t WHERE t.isPublic = true " +
           "ORDER BY t.usageCount DESC")
    List<TimerTemplate> findMostUsedTemplates();

    /**
     * Increment usage count.
     */
    @Query("UPDATE TimerTemplate t SET t.usageCount = t.usageCount + 1 WHERE t.id = :templateId")
    void incrementUsageCount(@Param("templateId") String templateId);
}