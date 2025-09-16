package com.focushive.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Single configuration to enable caching for production profiles.
 * This prevents multiple @EnableCaching annotations causing conflicts.
 *
 * Test profile uses TestCacheConfig instead.
 */
@Slf4j
@Configuration
@EnableCaching
@Profile("!test")
public class CacheEnablerConfig {

    public CacheEnablerConfig() {
        log.info("Caching enabled for production profile");
    }
}