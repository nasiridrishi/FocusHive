package com.focushive.notification.repository;

import com.focushive.notification.entity.NotificationTemplate;
import com.focushive.notification.entity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH", 
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaAuditing
class NotificationTemplateRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    private NotificationTemplate testTemplate;

    @BeforeEach
    void setUp() {
        testTemplate = NotificationTemplate.builder()
                .notificationType(NotificationType.HIVE_INVITATION)
                .language("en")
                .subject("You've been invited to join {{hiveName}}")
                .bodyText("Hi {{userName}}, you've been invited to join the hive '{{hiveName}}'.")
                .bodyHtml("<p>Hi {{userName}}, you've been invited to join the hive '<strong>{{hiveName}}</strong>'.</p>")
                .build();
        
        testTemplate = entityManager.persistAndFlush(testTemplate);
    }

    @Test
    void findByNotificationTypeAndLanguage_shouldReturnTemplate() {
        // When
        Optional<NotificationTemplate> template = notificationTemplateRepository
                .findByNotificationTypeAndLanguage(NotificationType.HIVE_INVITATION, "en");

        // Then
        assertThat(template).isPresent();
        assertThat(template.get().getNotificationType()).isEqualTo(NotificationType.HIVE_INVITATION);
        assertThat(template.get().getLanguage()).isEqualTo("en");
        assertThat(template.get().getSubject()).contains("{{hiveName}}");
    }

    @Test
    void findByNotificationTypeAndLanguage_withNonExistentLanguage_shouldReturnEmpty() {
        // When
        Optional<NotificationTemplate> template = notificationTemplateRepository
                .findByNotificationTypeAndLanguage(NotificationType.HIVE_INVITATION, "fr");

        // Then
        assertThat(template).isEmpty();
    }

    @Test
    void findByNotificationType_shouldReturnAllLanguageVariants() {
        // Given
        NotificationTemplate spanishTemplate = NotificationTemplate.builder()
                .notificationType(NotificationType.HIVE_INVITATION)
                .language("es")
                .subject("Has sido invitado a unirte a {{hiveName}}")
                .bodyText("Hola {{userName}}, has sido invitado a unirte al grupo '{{hiveName}}'.")
                .build();
        notificationTemplateRepository.save(spanishTemplate);

        // When
        List<NotificationTemplate> templates = notificationTemplateRepository
                .findByNotificationType(NotificationType.HIVE_INVITATION);

        // Then
        assertThat(templates).hasSize(2);
        assertThat(templates.stream().map(NotificationTemplate::getLanguage))
                .containsExactlyInAnyOrder("en", "es");
    }

    @Test
    void findByLanguage_shouldReturnAllTemplatesForLanguage() {
        // Given
        NotificationTemplate taskTemplate = NotificationTemplate.builder()
                .notificationType(NotificationType.TASK_ASSIGNED)
                .language("en")
                .subject("New task assigned: {{taskName}}")
                .bodyText("You have been assigned a new task: {{taskName}}")
                .build();
        notificationTemplateRepository.save(taskTemplate);

        // When
        List<NotificationTemplate> templates = notificationTemplateRepository.findByLanguage("en");

        // Then
        assertThat(templates).hasSize(2);
        assertThat(templates.stream().map(NotificationTemplate::getNotificationType))
                .containsExactlyInAnyOrder(NotificationType.HIVE_INVITATION, NotificationType.TASK_ASSIGNED);
    }

    @Test
    void findAvailableLanguages_shouldReturnUniqueLanguages() {
        // Given
        NotificationTemplate spanishTemplate = NotificationTemplate.builder()
                .notificationType(NotificationType.TASK_ASSIGNED)
                .language("es")
                .subject("Nueva tarea asignada")
                .bodyText("Se te ha asignado una nueva tarea")
                .build();
        
        NotificationTemplate frenchTemplate = NotificationTemplate.builder()
                .notificationType(NotificationType.HIVE_INVITATION)
                .language("fr")
                .subject("Invitation à rejoindre")
                .bodyText("Vous avez été invité")
                .build();
        
        notificationTemplateRepository.save(spanishTemplate);
        notificationTemplateRepository.save(frenchTemplate);

        // When
        List<String> languages = notificationTemplateRepository.findAvailableLanguages();

        // Then
        assertThat(languages).containsExactlyInAnyOrder("en", "es", "fr");
    }

    @Test
    void findAvailableNotificationTypes_shouldReturnUniqueTypes() {
        // Given
        NotificationTemplate taskTemplate = NotificationTemplate.builder()
                .notificationType(NotificationType.TASK_ASSIGNED)
                .language("en")
                .subject("New task assigned")
                .bodyText("You have a new task")
                .build();
        
        NotificationTemplate achievementTemplate = NotificationTemplate.builder()
                .notificationType(NotificationType.ACHIEVEMENT_UNLOCKED)
                .language("en")
                .subject("Achievement unlocked!")
                .bodyText("Congratulations on your achievement")
                .build();
        
        notificationTemplateRepository.save(taskTemplate);
        notificationTemplateRepository.save(achievementTemplate);

        // When
        List<NotificationType> types = notificationTemplateRepository.findAvailableNotificationTypes();

        // Then
        assertThat(types).containsExactlyInAnyOrder(
                NotificationType.HIVE_INVITATION, 
                NotificationType.TASK_ASSIGNED, 
                NotificationType.ACHIEVEMENT_UNLOCKED
        );
    }

    @Test
    void existsByNotificationTypeAndLanguage_shouldReturnTrueWhenExists() {
        // When
        boolean exists = notificationTemplateRepository
                .existsByNotificationTypeAndLanguage(NotificationType.HIVE_INVITATION, "en");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByNotificationTypeAndLanguage_shouldReturnFalseWhenNotExists() {
        // When
        boolean exists = notificationTemplateRepository
                .existsByNotificationTypeAndLanguage(NotificationType.TASK_ASSIGNED, "fr");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void deleteByNotificationTypeAndLanguage_shouldRemoveTemplate() {
        // Given
        assertThat(notificationTemplateRepository.findByNotificationTypeAndLanguage(
                NotificationType.HIVE_INVITATION, "en")).isPresent();

        // When
        notificationTemplateRepository.deleteByNotificationTypeAndLanguage(
                NotificationType.HIVE_INVITATION, "en");

        // Then
        assertThat(notificationTemplateRepository.findByNotificationTypeAndLanguage(
                NotificationType.HIVE_INVITATION, "en")).isEmpty();
    }

    @Test
    void save_shouldCreateNewTemplate() {
        // Given
        NotificationTemplate newTemplate = NotificationTemplate.builder()
                .notificationType(NotificationType.TASK_COMPLETED)
                .language("en")
                .subject("Task completed: {{taskName}}")
                .bodyText("The task '{{taskName}}' has been completed by {{completedBy}}")
                .bodyHtml("<p>The task '<strong>{{taskName}}</strong>' has been completed by {{completedBy}}</p>")
                .build();

        // When
        NotificationTemplate saved = notificationTemplateRepository.save(newTemplate);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.TASK_COMPLETED);
        assertThat(saved.getLanguage()).isEqualTo("en");
        assertThat(saved.getSubject()).contains("{{taskName}}");
    }

    @Test
    void save_shouldUpdateExistingTemplate() {
        // Given
        testTemplate.setSubject("Updated: You've been invited to {{hiveName}}");
        testTemplate.setBodyText("Updated message about joining {{hiveName}}");

        // When
        NotificationTemplate updated = notificationTemplateRepository.save(testTemplate);

        // Then
        assertThat(updated.getId()).isEqualTo(testTemplate.getId());
        assertThat(updated.getSubject()).startsWith("Updated:");
        assertThat(updated.getBodyText()).startsWith("Updated message");
    }

    @Test
    void template_shouldHaveCorrectTemplateKey() {
        // When
        String templateKey = testTemplate.getTemplateKey();

        // Then
        assertThat(templateKey).isEqualTo("HIVE_INVITATION_en");
    }

    @Test
    void template_shouldProcessVariablesCorrectly() {
        // Given
        java.util.Map<String, Object> variables = java.util.Map.of(
                "userName", "John Doe",
                "hiveName", "Study Group"
        );

        // When
        String processedSubject = testTemplate.getProcessedSubject(variables);
        String processedBodyText = testTemplate.getProcessedBodyText(variables);

        // Then
        assertThat(processedSubject).isEqualTo("You've been invited to join Study Group");
        assertThat(processedBodyText).isEqualTo("Hi John Doe, you've been invited to join the hive 'Study Group'.");
    }
}