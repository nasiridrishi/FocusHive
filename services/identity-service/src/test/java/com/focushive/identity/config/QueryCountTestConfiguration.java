package com.focushive.identity.config;

import net.ttddyy.dsproxy.QueryCountHolder;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Test configuration for query counting in performance tests.
 * This configuration wraps the datasource with a proxy that tracks
 * the number of queries executed, enabling N+1 query detection.
 */
@TestConfiguration
@Profile("test")
public class QueryCountTestConfiguration {

    @Bean
    @Primary
    public DataSource proxyDataSource(DataSource actualDataSource) {
        ChainListener listener = new ChainListener();
        DataSourceQueryCountListener queryCountListener = new DataSourceQueryCountListener();
        listener.addListener(queryCountListener);

        return ProxyDataSourceBuilder
                .create(actualDataSource)
                .name("QueryCountDataSource")
                .listener(listener)
                .countQuery()
                .build();
    }

    /**
     * Helper method to clear query counts
     */
    public static void clearQueryCount() {
        QueryCountHolder.clear();
    }
}