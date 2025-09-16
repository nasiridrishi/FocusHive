package com.focushive.identity.config;

import com.focushive.identity.listener.TokenRevocationEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for Redis pub/sub messaging.
 * Enables distributed event propagation for token revocation.
 * Only enabled when Redis repositories are enabled (disabled in test profile).
 */
@Slf4j
@Configuration
@EnableAsync
@ConditionalOnProperty(name = "spring.data.redis.repositories.enabled", havingValue = "true", matchIfMissing = true)
public class RedisMessagingConfig {

    private static final String REVOCATION_TOPIC = "token-revocation-events";

    /**
     * Redis message listener container for pub/sub.
     */
    @Bean
    public RedisMessageListenerContainer messageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter revocationListenerAdapter,
            ChannelTopic revocationTopic) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(revocationListenerAdapter, revocationTopic);

        log.info("Configured Redis message listener for topic: {}", REVOCATION_TOPIC);
        return container;
    }

    /**
     * Message listener adapter for token revocation events.
     */
    @Bean
    public MessageListenerAdapter revocationListenerAdapter(TokenRevocationEventListener listener) {
        return new MessageListenerAdapter(listener);
    }

    /**
     * Channel topic for token revocation events.
     */
    @Bean
    public ChannelTopic revocationTopic() {
        return new ChannelTopic(REVOCATION_TOPIC);
    }
}