package com.focushive.notification.repository;

import com.focushive.notification.entity.NotificationTemplate;
import com.focushive.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing notification templates.
 * Provides CRUD operations and custom queries for notification templates.
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {

    /**
     * Find a notification template by notification type and language.
     *
     * @param notificationType the notification type
     * @param language the language code
     * @return optional notification template
     */
    Optional<NotificationTemplate> findByNotificationTypeAndLanguage(NotificationType notificationType, String language);

    /**
     * Find all templates for a specific notification type across all languages.
     *
     * @param notificationType the notification type
     * @return list of templates for the notification type
     */
    List<NotificationTemplate> findByNotificationType(NotificationType notificationType);

    /**
     * Find all templates for a specific language across all notification types.
     *
     * @param language the language code
     * @return list of templates for the language
     */
    List<NotificationTemplate> findByLanguage(String language);

    /**
     * Get all available languages that have at least one template.
     *
     * @return list of language codes
     */
    @Query("SELECT DISTINCT nt.language FROM NotificationTemplate nt ORDER BY nt.language")
    List<String> findAvailableLanguages();

    /**
     * Get all notification types that have at least one template.
     *
     * @return list of notification types
     */
    @Query("SELECT DISTINCT nt.notificationType FROM NotificationTemplate nt ORDER BY nt.notificationType")
    List<NotificationType> findAvailableNotificationTypes();

    /**
     * Check if a template exists for a specific notification type and language.
     *
     * @param notificationType the notification type
     * @param language the language code
     * @return true if template exists, false otherwise
     */
    boolean existsByNotificationTypeAndLanguage(NotificationType notificationType, String language);

    /**
     * Delete a template by notification type and language.
     *
     * @param notificationType the notification type
     * @param language the language code
     */
    void deleteByNotificationTypeAndLanguage(NotificationType notificationType, String language);

    /**
     * Delete all templates for a specific notification type.
     *
     * @param notificationType the notification type
     */
    void deleteByNotificationType(NotificationType notificationType);

    /**
     * Delete all templates for a specific language.
     *
     * @param language the language code
     */
    void deleteByLanguage(String language);

    /**
     * Find templates that have both subject and HTML body content.
     * Used for email notifications that need rich content.
     *
     * @return list of templates with complete email content
     */
    @Query("SELECT nt FROM NotificationTemplate nt WHERE nt.subject IS NOT NULL " +
           "AND nt.subject != '' AND nt.bodyHtml IS NOT NULL AND nt.bodyHtml != ''")
    List<NotificationTemplate> findTemplatesWithEmailContent();

    /**
     * Find templates by notification type with fallback to default language.
     * If no template exists for the requested language, returns the English template.
     *
     * @param notificationType the notification type
     * @param preferredLanguage the preferred language
     * @return template in preferred language or English fallback
     */
    @Query("SELECT nt FROM NotificationTemplate nt WHERE nt.notificationType = :notificationType " +
           "AND nt.language = :preferredLanguage " +
           "UNION " +
           "SELECT nt FROM NotificationTemplate nt WHERE nt.notificationType = :notificationType " +
           "AND nt.language = 'en' AND NOT EXISTS (" +
           "    SELECT 1 FROM NotificationTemplate nt2 WHERE nt2.notificationType = :notificationType " +
           "    AND nt2.language = :preferredLanguage" +
           ")")
    List<NotificationTemplate> findTemplateWithFallback(
            @Param("notificationType") NotificationType notificationType,
            @Param("preferredLanguage") String preferredLanguage);

    /**
     * Count templates by language.
     *
     * @param language the language code
     * @return number of templates for the language
     */
    long countByLanguage(String language);

    /**
     * Count templates by notification type.
     *
     * @param notificationType the notification type
     * @return number of templates for the notification type
     */
    long countByNotificationType(NotificationType notificationType);

    /**
     * Find templates that are missing for a specific language.
     * Returns notification types that don't have templates in the specified language.
     *
     * @param language the language code
     * @return list of notification types missing templates for the language
     */
    @Query("SELECT DISTINCT nt.notificationType FROM NotificationTemplate nt " +
           "WHERE nt.notificationType NOT IN (" +
           "    SELECT nt2.notificationType FROM NotificationTemplate nt2 WHERE nt2.language = :language" +
           ")")
    List<NotificationType> findMissingTemplatesForLanguage(@Param("language") String language);

    /**
     * Get template statistics for admin dashboard.
     *
     * @return list of objects containing notification type, language count, and template count
     */
    @Query("SELECT nt.notificationType, COUNT(DISTINCT nt.language), COUNT(nt) " +
           "FROM NotificationTemplate nt GROUP BY nt.notificationType")
    List<Object[]> getTemplateStatistics();
}