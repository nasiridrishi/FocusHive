package com.focushive.music;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the FocusHive Music Service.
 * 
 * This microservice handles:
 * - Music recommendations based on user preferences and hive context
 * - Spotify integration for streaming music
 * - Collaborative playlist management for hives
 * - Music session analytics and insights
 * - Real-time music synchronization across hive members
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class MusicServiceApplication {

    /**
     * Main entry point for the Music Service application.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MusicServiceApplication.class, args);
    }
}