package com.focushive.music;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Music Service Application - Handles music recommendations, playlist management, 
 * and streaming integrations for FocusHive.
 * 
 * Features:
 * - Spotify API integration
 * - Music recommendations based on focus session types
 * - Playlist management and sharing
 * - Ambient sound generation
 * - Focus music analytics
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
public class MusicServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MusicServiceApplication.class, args);
    }
}