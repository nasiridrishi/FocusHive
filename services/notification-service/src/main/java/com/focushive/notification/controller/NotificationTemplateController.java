package com.focushive.notification.controller;

import com.focushive.notification.entity.NotificationTemplate;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.*;

/**
 * REST controller for managing notification templates.
 * Provides endpoints for CRUD operations, template processing, and validation.
 */
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Validated
@Slf4j
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;

    /**
     * Create a new notification template.
     *
     * @param template the template to create
     * @return created template
     */
    @PostMapping
    public ResponseEntity<NotificationTemplate> createTemplate(@Valid @RequestBody NotificationTemplate template) {
        log.info("Creating new template for type {} and language {}", 
                template.getNotificationType(), template.getLanguage());
        
        NotificationTemplate createdTemplate = templateService.createTemplate(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTemplate);
    }

    /**
     * Get a template by notification type and language.
     *
     * @param type the notification type
     * @param language the language code
     * @return template if found
     */
    @GetMapping("/{type}/{language}")
    public ResponseEntity<NotificationTemplate> getTemplate(
            @PathVariable NotificationType type,
            @PathVariable String language) {
        
        log.debug("Retrieving template for type {} and language {}", type, language);
        
        Optional<NotificationTemplate> template = templateService.findTemplate(type, language);
        return template.map(t -> ResponseEntity.ok(t))
                      .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Process a template with variables to get the final rendered content.
     *
     * @param type the notification type
     * @param language the language code
     * @param variables the variables to substitute
     * @return processed template
     */
    @PostMapping("/{type}/{language}/process")
    public ResponseEntity<ProcessedTemplate> processTemplate(
            @PathVariable NotificationType type,
            @PathVariable String language,
            @RequestBody Map<String, Object> variables) {
        
        log.debug("Processing template for type {} and language {} with {} variables", 
                type, language, variables.size());
        
        ProcessedTemplate processedTemplate = templateService.processTemplate(type, language, variables);
        return ResponseEntity.ok(processedTemplate);
    }

    /**
     * Update an existing template.
     *
     * @param id the template ID
     * @param template the updated template
     * @return updated template
     */
    @PutMapping("/{id}")
    public ResponseEntity<NotificationTemplate> updateTemplate(
            @PathVariable String id,
            @Valid @RequestBody NotificationTemplate template) {
        
        log.info("Updating template with ID: {}", id);
        
        NotificationTemplate updatedTemplate = templateService.updateTemplate(id, template);
        return ResponseEntity.ok(updatedTemplate);
    }

    /**
     * Delete a template.
     *
     * @param id the template ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String id) {
        log.info("Deleting template with ID: {}", id);
        
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all available languages that have at least one template.
     *
     * @return list of language codes
     */
    @GetMapping("/languages")
    public ResponseEntity<List<String>> getAvailableLanguages() {
        log.debug("Retrieving available languages");
        
        List<String> languages = templateService.getAvailableLanguages();
        return ResponseEntity.ok(languages);
    }

    /**
     * Get template statistics.
     *
     * @return statistics by notification type
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<NotificationType, TemplateStatistics>> getTemplateStatistics() {
        log.debug("Retrieving template statistics");
        
        Map<NotificationType, TemplateStatistics> stats = templateService.getTemplateStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Extract variables from a template.
     *
     * @param template the template to analyze
     * @return set of variable names
     */
    @PostMapping("/extract-variables")
    public ResponseEntity<Set<String>> extractTemplateVariables(@Valid @RequestBody NotificationTemplate template) {
        log.debug("Extracting variables from template");
        
        Set<String> variables = templateService.extractTemplateVariables(template);
        return ResponseEntity.ok(variables);
    }

    /**
     * Validate template variables.
     *
     * @param type the notification type
     * @param language the language code
     * @param variables the variables to validate
     * @return validation result
     */
    @PostMapping("/{type}/{language}/validate")
    public ResponseEntity<ValidationResult> validateTemplateVariables(
            @PathVariable NotificationType type,
            @PathVariable String language,
            @RequestBody Map<String, Object> variables) {
        
        log.debug("Validating template variables for type {} and language {}", type, language);
        
        try {
            // Get the template first
            NotificationTemplate template = templateService.findTemplate(type, language)
                    .orElseThrow(() -> new TemplateNotFoundException(
                            String.format("Template not found for type %s and language %s", type, language)));
            
            templateService.validateTemplateVariables(template, variables);
            
            return ResponseEntity.ok(ValidationResult.builder()
                    .valid(true)
                    .build());
                    
        } catch (TemplateValidationException e) {
            return ResponseEntity.badRequest().body(ValidationResult.builder()
                    .valid(false)
                    .errors(List.of(e.getMessage()))
                    .build());
        }
    }

    /**
     * Bulk create templates.
     *
     * @param templates list of templates to create
     * @return created templates
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<NotificationTemplate>> bulkCreateTemplates(
            @Valid @RequestBody List<NotificationTemplate> templates) {
        
        log.info("Bulk creating {} templates", templates.size());
        
        List<NotificationTemplate> createdTemplates = templateService.bulkCreateTemplates(templates);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTemplates);
    }

    /**
     * Exception handler for TemplateNotFoundException.
     */
    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTemplateNotFound(TemplateNotFoundException e) {
        log.warn("Template not found: {}", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .message(e.getMessage())
                        .status(HttpStatus.NOT_FOUND.value())
                        .build());
    }

    /**
     * Exception handler for TemplateValidationException.
     */
    @ExceptionHandler(TemplateValidationException.class)
    public ResponseEntity<ErrorResponse> handleTemplateValidation(TemplateValidationException e) {
        log.warn("Template validation failed: {}", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .message(e.getMessage())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .build());
    }

    /**
     * Exception handler for general exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        log.error("Unexpected error in template controller", e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .message("Internal server error")
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .build());
    }
}