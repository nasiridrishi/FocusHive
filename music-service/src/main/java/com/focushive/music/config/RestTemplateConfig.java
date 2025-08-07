package com.focushive.music.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate used in external API calls.
 * 
 * Configures timeouts and connection settings for REST calls
 * to external services like Spotify, Apple Music, etc.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates and configures RestTemplate bean.
     * 
     * @return Configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(clientHttpRequestFactory());
        return restTemplate;
    }

    /**
     * Configures HTTP request factory with timeouts.
     * 
     * @return Configured ClientHttpRequestFactory
     */
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(30000);    // 30 seconds
        return factory;
    }
}