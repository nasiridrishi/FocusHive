package com.focushive.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for notification message processing.
 * 
 * Sets up queues, exchanges, bindings, and message converters for
 * asynchronous notification processing with dead letter queue support.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${notification.queue.name:notifications}")
    private String queueName;

    @Value("${notification.queue.exchange:focushive.notifications}")
    private String exchangeName;

    @Value("${notification.queue.routing-key:notification.created}")
    private String routingKey;

    @Value("${notification.queue.dlq.name:notifications.dlq}")
    private String dlqName;

    @Value("${notification.queue.dlq.exchange:focushive.notifications.dlx}")
    private String dlxExchangeName;

    @Value("${notification.queue.dlq.routing-key:notification.failed}")
    private String dlqRoutingKey;

    @Value("${notification.queue.ttl:3600000}") // 1 hour default
    private Long messageTtl;

    @Value("${notification.queue.max-retries:3}")
    private Integer maxRetries;

    // Main queue configuration
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", dlxExchangeName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .withArgument("x-message-ttl", messageTtl)
                .build();
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(notificationExchange())
                .with(routingKey);
    }

    // Dead Letter Queue configuration
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlqName)
                .withArgument("x-message-ttl", messageTtl * 2) // 2 hours for DLQ
                .build();
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(dlxExchangeName, true, false);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(dlqRoutingKey);
    }
    
    // Additional specialized dead letter queues
    @Bean
    public Queue emailDeadLetterQueue() {
        return QueueBuilder.durable("focushive.notifications.email.dlq")
                .withArgument("x-message-ttl", messageTtl * 2) // 2 hours for DLQ
                .build();
    }
    
    @Bean
    public Binding emailDeadLetterBinding() {
        return BindingBuilder
                .bind(emailDeadLetterQueue())
                .to(deadLetterExchange())
                .with("notification.email.failed");
    }
    
    @Bean
    public Queue priorityDeadLetterQueue() {
        return QueueBuilder.durable("focushive.notifications.priority.dlq")
                .withArgument("x-message-ttl", messageTtl * 2) // 2 hours for DLQ
                .build();
    }
    
    @Bean
    public Binding priorityDeadLetterBinding() {
        return BindingBuilder
                .bind(priorityDeadLetterQueue())
                .to(deadLetterExchange())
                .with("notification.priority.failed");
    }

    // Priority queue for urgent notifications
    @Bean
    public Queue priorityNotificationQueue() {
        return QueueBuilder.durable(queueName + ".priority")
                .withArgument("x-max-priority", 10)
                .withArgument("x-dead-letter-exchange", dlxExchangeName)
                .withArgument("x-dead-letter-routing-key", "notification.priority.failed")
                .build();
    }

    @Bean
    public Binding priorityNotificationBinding() {
        return BindingBuilder
                .bind(priorityNotificationQueue())
                .to(notificationExchange())
                .with("notification.priority.*");
    }

    // Email notification queue
    @Bean
    public Queue emailNotificationQueue() {
        return QueueBuilder.durable("notifications.email")
                .withArgument("x-dead-letter-exchange", dlxExchangeName)
                .withArgument("x-dead-letter-routing-key", "notification.email.failed")
                .build();
    }

    @Bean
    public Binding emailNotificationBinding() {
        return BindingBuilder
                .bind(emailNotificationQueue())
                .to(notificationExchange())
                .with("notification.email.*");
    }

    // Message converter configuration
    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        rabbitTemplate.setExchange(exchangeName);
        
        // Confirm callback for message acknowledgment
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // Log failed message sends
                System.err.println("Message send failed: " + cause);
            }
        });
        
        // Return callback for unroutable messages
        rabbitTemplate.setReturnsCallback(returned -> {
            System.err.println("Message returned: " + returned.getMessage());
        });
        
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(10);
        
        // Configure retry logic
        factory.setDefaultRequeueRejected(false);
        
        // Error handler for failed messages
        factory.setErrorHandler(throwable -> {
            System.err.println("Error in listener: " + throwable.getMessage());
        });
        
        return factory;
    }
}