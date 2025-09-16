package com.focushive.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Test Email Configuration for running the service with test profile.
 * Provides mock/test email configuration when AWS SES is not available.
 */
@Slf4j
@Configuration
@Profile("test")
public class TestEmailConfig {

    @Value("${spring.mail.host:localhost}")
    private String mailHost;

    @Value("${spring.mail.port:1025}")
    private int mailPort;

    @Value("${spring.mail.from:no-reply@focushive.com}")
    private String fromEmail;

    /**
     * Creates a test JavaMailSender for development/testing.
     * Can be configured to use MailHog or other test SMTP servers.
     *
     * @return configured JavaMailSender for test environment
     */
    @Bean
    public JavaMailSender mailSender() {
        log.info("Configuring Test JavaMailSender for host: {}:{}", mailHost, mailPort);

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);

        // Configure SMTP properties for test environment
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.debug", "true");

        return mailSender;
    }

    /**
     * Provides the from email address for test environment.
     *
     * @return the from email address for notifications
     */
    @Bean("fromEmailAddress")
    @Qualifier("fromEmailAddress")
    public String fromEmailAddress() {
        log.info("Test from email configured as: {}", fromEmail);
        return fromEmail;
    }
}