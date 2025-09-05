package com.focushive.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

/**
 * Production-ready Redis configuration with Lettuce client.
 * Configures connection factory, templates, and client options for optimal performance.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.features.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisConfiguration {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${spring.data.redis.timeout:60s}")
    private Duration commandTimeout;

    @Value("${spring.data.redis.connect-timeout:10s}")
    private Duration connectTimeout;

    @Value("${spring.data.redis.lettuce.shutdown-timeout:100ms}")
    private Duration shutdownTimeout;

    // Connection Pool Settings
    @Value("${spring.data.redis.lettuce.pool.min-idle:8}")
    private int minIdle;

    @Value("${spring.data.redis.lettuce.pool.max-idle:16}")
    private int maxIdle;

    @Value("${spring.data.redis.lettuce.pool.max-active:24}")
    private int maxActive;

    @Value("${spring.data.redis.lettuce.pool.max-wait:1s}")
    private Duration maxWait;

    // Cluster Settings
    @Value("${focushive.redis.cluster.enabled:false}")
    private boolean clusterEnabled;

    @Value("${focushive.redis.cluster.nodes:}")
    private List<String> clusterNodes;

    @Value("${focushive.redis.cluster.max-redirects:5}")
    private int maxRedirects;

    // Sentinel Settings  
    @Value("${spring.data.redis.sentinel.master:}")
    private String sentinelMaster;

    @Value("${spring.data.redis.sentinel.nodes:}")
    private List<String> sentinelNodes;

    /**
     * Creates optimized ClientResources for Lettuce connections.
     * Configures I/O thread pool and computation thread pool for optimal performance.
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(ClientResources.class)
    public ClientResources clientResources() {
        return DefaultClientResources.builder()
                .ioThreadPoolSize(4)
                .computationThreadPoolSize(4)
                .build();
    }

    /**
     * Creates production-ready ClientOptions with optimal settings.
     */
    @Bean
    @ConditionalOnMissingBean(ClientOptions.class)
    public ClientOptions clientOptions() {
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(connectTimeout)
                .keepAlive(true)
                .tcpNoDelay(true)
                .build();

        return ClientOptions.builder()
                .socketOptions(socketOptions)
                .autoReconnect(true)
                .cancelCommandsOnReconnectFailure(false)
                .pingBeforeActivateConnection(true)
                .suspendReconnectOnProtocolFailure(false)
                .build();
    }

    /**
     * Creates ClusterClientOptions for Redis Cluster deployments.
     */
    @Bean
    @ConditionalOnProperty(name = "focushive.redis.cluster.enabled", havingValue = "true")
    public ClusterClientOptions clusterClientOptions(ClientOptions clientOptions) {
        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enableAdaptiveRefreshTrigger()
                .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(30))
                .enablePeriodicRefresh(Duration.ofSeconds(30))
                .build();

        return ClusterClientOptions.builder(clientOptions)
                .topologyRefreshOptions(topologyRefreshOptions)
                .maxRedirects(maxRedirects)
                .validateClusterNodeMembership(false)
                .build();
    }

    /**
     * Creates optimized connection pool configuration.
     */
    @Bean
    public GenericObjectPoolConfig<Object> poolConfig() {
        GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxWait(maxWait);
        
        // Additional pool optimizations
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(false);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        
        return poolConfig;
    }

    /**
     * Creates LettuceClientConfiguration with connection pooling and optimizations.
     */
    @Bean
    public LettuceClientConfiguration lettuceClientConfiguration(
            ClientOptions clientOptions, 
            ClientResources clientResources,
            GenericObjectPoolConfig<Object> poolConfig) {
        
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = 
                LettucePoolingClientConfiguration.builder()
                    .poolConfig(poolConfig)
                    .clientOptions(clientOptions)
                    .clientResources(clientResources)
                    .commandTimeout(commandTimeout)
                    .shutdownTimeout(shutdownTimeout);

        if (sslEnabled) {
            builder.useSsl();
        }

        return builder.build();
    }

    /**
     * Creates primary Redis connection factory based on deployment type.
     */
    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory(
            LettuceClientConfiguration lettuceClientConfiguration) {
        
        // Determine configuration type based on deployment
        if (clusterEnabled && !clusterNodes.isEmpty()) {
            return createClusterConnectionFactory(lettuceClientConfiguration);
        } else if (sentinelMaster != null && !sentinelMaster.isEmpty() && !sentinelNodes.isEmpty()) {
            return createSentinelConnectionFactory(lettuceClientConfiguration);
        } else {
            return createStandaloneConnectionFactory(lettuceClientConfiguration);
        }
    }

    /**
     * Creates Redis Cluster connection factory.
     */
    private LettuceConnectionFactory createClusterConnectionFactory(
            LettuceClientConfiguration lettuceClientConfiguration) {
        
        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration(clusterNodes);
        clusterConfiguration.setMaxRedirects(maxRedirects);
        
        if (!redisPassword.isEmpty()) {
            clusterConfiguration.setPassword(redisPassword);
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(
            clusterConfiguration, lettuceClientConfiguration);
        factory.setValidateConnection(true);
        
        return factory;
    }

    /**
     * Creates Redis Sentinel connection factory for high availability.
     */
    private LettuceConnectionFactory createSentinelConnectionFactory(
            LettuceClientConfiguration lettuceClientConfiguration) {
        
        RedisSentinelConfiguration sentinelConfiguration = new RedisSentinelConfiguration()
            .master(sentinelMaster);
        
        sentinelNodes.forEach(node -> {
            String[] hostPort = node.split(":");
            sentinelConfiguration.sentinel(hostPort[0], Integer.parseInt(hostPort[1]));
        });
        
        sentinelConfiguration.setDatabase(redisDatabase);
        
        if (!redisPassword.isEmpty()) {
            sentinelConfiguration.setPassword(redisPassword);
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(
            sentinelConfiguration, lettuceClientConfiguration);
        factory.setValidateConnection(true);
        
        return factory;
    }

    /**
     * Creates standalone Redis connection factory.
     */
    private LettuceConnectionFactory createStandaloneConnectionFactory(
            LettuceClientConfiguration lettuceClientConfiguration) {
        
        RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration();
        standaloneConfiguration.setHostName(redisHost);
        standaloneConfiguration.setPort(redisPort);
        standaloneConfiguration.setDatabase(redisDatabase);
        
        if (!redisPassword.isEmpty()) {
            standaloneConfiguration.setPassword(redisPassword);
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(
            standaloneConfiguration, lettuceClientConfiguration);
        factory.setValidateConnection(true);
        
        return factory;
    }

    /**
     * Creates optimized ObjectMapper for Redis JSON serialization.
     */
    @Bean("redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    /**
     * Creates JSON serializer for Redis values with custom ObjectMapper.
     */
    @Bean
    public RedisSerializer<Object> redisSerializer(ObjectMapper redisObjectMapper) {
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }

    /**
     * Creates primary RedisTemplate with optimized serializers.
     */
    @Bean("redisTemplate")
    @Primary
    public RedisTemplate<String, Object> redisTemplate(
            LettuceConnectionFactory redisConnectionFactory,
            RedisSerializer<Object> redisSerializer) {
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // Configure serializers
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(redisSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(redisSerializer);
        
        // Enable transaction support
        template.setEnableTransactionSupport(true);
        
        // Set default serializer
        template.setDefaultSerializer(redisSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Creates string RedisTemplate for simple string operations.
     */
    @Bean("stringRedisTemplate")
    public RedisTemplate<String, String> stringRedisTemplate(
            LettuceConnectionFactory redisConnectionFactory) {
        
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        template.setDefaultSerializer(stringSerializer);
        
        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Creates RedisMessageListenerContainer for pub/sub functionality.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            LettuceConnectionFactory redisConnectionFactory) {
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        
        // Configure container for optimal pub/sub performance
        container.setTaskExecutor(null); // Use default executor
        container.setSubscriptionExecutor(null); // Use default executor
        
        // Recovery settings
        container.setRecoveryInterval(5000L); // 5 seconds in milliseconds
        
        return container;
    }

    /**
     * Creates reactive RedisTemplate (if needed for reactive operations).
     */
    /*
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            LettuceConnectionFactory redisConnectionFactory,
            RedisSerializer<Object> redisSerializer) {
        
        RedisSerializationContext<String, Object> serializationContext = 
            RedisSerializationContext.<String, Object>newSerializationContext()
                .key(StringRedisSerializer.UTF_8)
                .value(redisSerializer)
                .hashKey(StringRedisSerializer.UTF_8)
                .hashValue(redisSerializer)
                .build();

        return new ReactiveRedisTemplate<>(redisConnectionFactory, serializationContext);
    }
    */
}