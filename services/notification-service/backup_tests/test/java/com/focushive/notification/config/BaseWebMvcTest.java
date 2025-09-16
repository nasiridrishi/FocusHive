package com.focushive.notification.config;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Base annotation for Web MVC tests with proper exclusions and configurations.
 * Combines @WebMvcTest with our unified test configuration.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@WebMvcTest(excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration.class
})
@ActiveProfiles("test")
@Import({UnifiedTestConfiguration.class, TestSecurityConfig.class})
public @interface BaseWebMvcTest {
    /**
     * Controllers to test.
     */
    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value() default {};

    /**
     * Controllers to test.
     */
    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] controllers() default {};
}