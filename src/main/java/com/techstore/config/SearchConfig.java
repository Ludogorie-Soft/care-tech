package com.techstore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties
public class SearchConfig {

    /**
     * Seconds a single search query may run before the driver cancels it.
     * <p>
     * A search that hangs otherwise holds a pooled connection for as long as the
     * 30s connection-timeout allows, and with 20 connections a handful of slow
     * searches can starve the rest of the application. Failing one search fast is
     * much better than queueing everything behind it.
     */
    private static final int SEARCH_QUERY_TIMEOUT_SECONDS = 15;

    /**
     * Only {@code ProductSearchRepository} uses this template, so the timeout applies
     * to search alone — the sync jobs run long bulk statements through JPA and must
     * not be capped by it.
     */
    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setQueryTimeout(SEARCH_QUERY_TIMEOUT_SECONDS);
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @ConfigurationProperties(prefix = "app.search.postgresql")
    @Data
    public static class PostgreSQLSearchProperties {
        private boolean enableFullTextSearch = true;
        private int defaultPageSize = 20;
        private int maxPageSize = 100;
        private int maxQueryLength = 200;
        private int suggestionsCacheMinutes = 30;
        private boolean enableQueryLogging = false;
    }

    // SearchIndexManager used to be declared twice — once here as a @Bean and once by
    // its own @Component annotation. The factory method is the redundant one: the class
    // already reads app.search.postgresql.auto-create-indexes itself and returns early
    // when it is false, so the @ConditionalOnProperty added nothing.
}
