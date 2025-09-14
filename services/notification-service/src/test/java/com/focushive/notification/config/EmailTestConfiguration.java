package com.focushive.notification.config;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for email delivery tests that provides a real JavaMailSender.
 * This configuration provides all the necessary mocked beans except JavaMailSender.
 */
@TestConfiguration
public class EmailTestConfiguration {

    /**
     * Provides a real JavaMailSender implementation that can work with GreenMail
     * This overrides the mock from H2TestConfiguration for email tests
     */
    @Bean("mailSender")
    @Primary
    public JavaMailSender mailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("127.0.0.1");  // Match GreenMail's binding address
        mailSender.setPort(3025);
        // No username/password needed for GreenMail with disabled authentication
        
        // Configure properties for GreenMail - disable authentication to match GreenMail setup
        java.util.Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false");  // Match GreenMail's withDisabledAuthentication()
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.debug", "true");
        
        return mailSender;
    }

    /**
     * Mock RabbitTemplate for tests that don't need actual RabbitMQ
     */
    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return mock(RabbitTemplate.class);
    }

    /**
     * Mock AmqpTemplate for tests that don't need actual messaging
     */
    @Bean
    @Primary
    public AmqpTemplate amqpTemplate() {
        return mock(AmqpTemplate.class);
    }

    /**
     * Mock notification queue for tests
     */
    @Bean("notificationQueue")
    public Queue notificationQueue() {
        return new Queue("test-notifications", false);
    }
}