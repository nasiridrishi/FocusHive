package com.focushive.notification.service;

import com.focushive.notification.entity.NotificationTemplate;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.repository.NotificationTemplateRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for managing notification templates.
 * Provides CRUD operations, caching, template processing, and validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final RedisTemplate<String, NotificationTemplate> notificationTemplateRedisTemplate;

    private static final String CACHE_KEY_PREFIX = "template:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    /**
     * Create a new notification template.
     *
     * @param template the template to create
     * @return the created template
     */
    @Transactional
    public NotificationTemplate createTemplate(NotificationTemplate template) {
        log.info("Creating template for type {} and language {}", 
                template.getNotificationType(), template.getLanguage());
        
        NotificationTemplate savedTemplate = templateRepository.save(template);
        cacheTemplate(savedTemplate);
        
        log.info("Template created with ID: {}", savedTemplate.getId());
        return savedTemplate;
    }

    /**
     * Find a template by notification type and language.
     *
     * @param notificationType the notification type
     * @param language the language code
     * @return optional template
     */
    public Optional<NotificationTemplate> findTemplate(NotificationType notificationType, String language) {
        String cacheKey = getCacheKey(notificationType, language);
        
        // Try cache first
        NotificationTemplate cachedTemplate = notificationTemplateRedisTemplate.opsForValue().get(cacheKey);
        if (cachedTemplate != null) {
            log.debug("Template found in cache for {}/{}", notificationType, language);
            return Optional.of(cachedTemplate);
        }
        
        // Not in cache, query database
        Optional<NotificationTemplate> template = templateRepository
                .findByNotificationTypeAndLanguage(notificationType, language);
        
        if (template.isPresent()) {
            cacheTemplate(template.get());
            log.debug("Template found in database and cached for {}/{}", notificationType, language);
        } else {
            log.debug("No template found for {}/{}", notificationType, language);
        }
        
        return template;
    }

    /**
     * Process a template with variables to produce the final content.
     *
     * @param notificationType the notification type
     * @param language the language code
     * @param variables the variables to substitute
     * @return processed template
     */
    public ProcessedTemplate processTemplate(NotificationType notificationType, String language, 
                                           Map<String, Object> variables) {
        log.debug("Processing template for {}/{} with {} variables", 
                notificationType, language, variables.size());
        
        NotificationTemplate template = findTemplate(notificationType, language)
                .orElseThrow(() -> new TemplateNotFoundException(
                        String.format("No template found for type %s and language %s", 
                                notificationType, language)));
        
        String processedSubject = template.processTemplate(template.getSubject(), variables);
        String processedBodyText = template.processTemplate(template.getBodyText(), variables);
        String processedBodyHtml = template.processTemplate(template.getBodyHtml(), variables);
        
        int variableCount = countVariables(template, variables);
        
        ProcessedTemplate result = ProcessedTemplate.create(
                notificationType, language, template.getId(), 
                processedSubject, processedBodyText, processedBodyHtml, 
                variableCount);
        
        log.debug("Template processed successfully with {} variables substituted", variableCount);
        return result;
    }

    /**
     * Validate that all required template variables are provided.
     *
     * @param template the template to validate
     * @param variables the provided variables
     * @throws TemplateValidationException if validation fails
     */
    public void validateTemplateVariables(NotificationTemplate template, Map<String, Object> variables) {
        Set<String> requiredVariables = extractTemplateVariables(template);
        Set<String> providedVariables = variables.keySet();
        
        Set<String> missingVariables = new HashSet<>(requiredVariables);
        missingVariables.removeAll(providedVariables);
        
        if (!missingVariables.isEmpty()) {
            List<String> sorted = new ArrayList<>(missingVariables);
            Collections.sort(sorted);
            throw new TemplateValidationException(
                    String.format("Missing required variables: %s", sorted));
        }
    }

    /**
     * Update an existing template.
     *
     * @param templateId the template ID
     * @param updatedTemplate the updated template data
     * @return the updated template
     */
    @Transactional
    public NotificationTemplate updateTemplate(String templateId, NotificationTemplate updatedTemplate) {
        log.info("Updating template with ID: {}", templateId);
        
        NotificationTemplate existingTemplate = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found: " + templateId));
        
        // Clear cache for old template
        clearTemplateCache(existingTemplate);
        
        NotificationTemplate savedTemplate = templateRepository.save(updatedTemplate);
        cacheTemplate(savedTemplate);
        
        log.info("Template updated successfully: {}", templateId);
        return savedTemplate;
    }

    /**
     * Delete a template.
     *
     * @param templateId the template ID to delete
     */
    @Transactional
    public void deleteTemplate(String templateId) {
        log.info("Deleting template with ID: {}", templateId);
        
        Optional<NotificationTemplate> template = templateRepository.findById(templateId);
        if (template.isPresent()) {
            clearTemplateCache(template.get());
            templateRepository.deleteById(templateId);
            log.info("Template deleted successfully: {}", templateId);
        } else {
            log.warn("Template not found for deletion: {}", templateId);
        }
    }

    /**
     * Find template with fallback to default language.
     *
     * @param notificationType the notification type
     * @param preferredLanguage the preferred language
     * @return optional template
     */
    public Optional<NotificationTemplate> findTemplateWithFallback(NotificationType notificationType, 
                                                                  String preferredLanguage) {
        // Try cache for preferred language first
        String cacheKey = getCacheKey(notificationType, preferredLanguage);
        NotificationTemplate cachedTemplate = notificationTemplateRedisTemplate.opsForValue().get(cacheKey);
        if (cachedTemplate != null) {
            return Optional.of(cachedTemplate);
        }
        
        // Use repository method with fallback
        List<NotificationTemplate> templates = templateRepository
                .findTemplateWithFallback(notificationType, preferredLanguage);
        
        if (!templates.isEmpty()) {
            NotificationTemplate template = templates.get(0);
            cacheTemplate(template);
            return Optional.of(template);
        }
        
        return Optional.empty();
    }

    /**
     * Get all available languages.
     *
     * @return list of language codes
     */
    public List<String> getAvailableLanguages() {
        return templateRepository.findAvailableLanguages();
    }

    /**
     * Get template statistics.
     *
     * @return map of statistics by notification type
     */
    public Map<NotificationType, TemplateStatistics> getTemplateStatistics() {
        List<Object[]> stats = templateRepository.getTemplateStatistics();
        
        return stats.stream().collect(Collectors.toMap(
                row -> (NotificationType) row[0],
                row -> TemplateStatistics.builder()
                        .languageCount((Long) row[1])
                        .templateCount((Long) row[2])
                        .build()
        ));
    }

    /**
     * Extract all template variables from a template.
     *
     * @param template the template to analyze
     * @return set of variable names
     */
    public Set<String> extractTemplateVariables(NotificationTemplate template) {
        Set<String> variables = new HashSet<>();
        
        extractVariablesFromText(template.getSubject(), variables);
        extractVariablesFromText(template.getBodyText(), variables);
        extractVariablesFromText(template.getBodyHtml(), variables);
        
        return variables;
    }

    /**
     * Bulk create templates.
     *
     * @param templates list of templates to create
     * @return list of created templates
     */
    @Transactional
    public List<NotificationTemplate> bulkCreateTemplates(List<NotificationTemplate> templates) {
        log.info("Bulk creating {} templates", templates.size());
        
        List<NotificationTemplate> savedTemplates = templateRepository.saveAll(templates);
        
        // Cache all templates
        savedTemplates.forEach(this::cacheTemplate);
        
        log.info("Bulk created {} templates successfully", savedTemplates.size());
        return savedTemplates;
    }

    // Private helper methods

    private void cacheTemplate(NotificationTemplate template) {
        String cacheKey = getCacheKey(template.getNotificationType(), template.getLanguage());
        notificationTemplateRedisTemplate.opsForValue().set(cacheKey, template, CACHE_TTL);
        log.debug("Template cached with key: {}", cacheKey);
    }

    private void clearTemplateCache(NotificationTemplate template) {
        String cacheKey = getCacheKey(template.getNotificationType(), template.getLanguage());
        notificationTemplateRedisTemplate.delete(cacheKey);
        log.debug("Template cache cleared for key: {}", cacheKey);
    }

    private String getCacheKey(NotificationType notificationType, String language) {
        return CACHE_KEY_PREFIX + notificationType.name() + "_" + language;
    }

    private void extractVariablesFromText(String text, Set<String> variables) {
        if (text == null || text.isEmpty()) {
            return;
        }
        
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(text);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
    }

    private int countVariables(NotificationTemplate template, Map<String, Object> variables) {
        Set<String> templateVariables = extractTemplateVariables(template);
        return (int) templateVariables.stream()
                .filter(variables::containsKey)
                .count();
    }
}