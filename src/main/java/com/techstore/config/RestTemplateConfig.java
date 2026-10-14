package com.techstore.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Value("${asbis.api.timeout:60000}")
    private int asbisTimeout;

    @Value("${asbis.api.connection-timeout:30000}")
    private int connectionTimeout;

    @Value("${most.api.timeout:180000}")
    private int mostTimeout;

    @Value("${most.api.connection-timeout:15000}")
    private int mostConnectionTimeout;

    /**
     * Most previously used the @Primary bare {@code new RestTemplate()}, which has no
     * timeouts at all: a hung supplier would have blocked the nightly cron indefinitely.
     * Sized for the feed it actually pulls — ~17 MB of XML in one GET.
     */
    @Bean("mostRestTemplate")
    public RestTemplate mostRestTemplate() {
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setMaxConnTotal(5)
                        .setMaxConnPerRoute(5)
                        .setDefaultSocketConfig(
                                SocketConfig.custom()
                                        .setSoTimeout(Timeout.ofMilliseconds(mostTimeout))
                                        .build()
                        )
                        .setConnectionTimeToLive(TimeValue.ofSeconds(30))
                        .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(mostConnectionTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(mostTimeout))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();

        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    @Bean("asbisRestTemplate")
    public RestTemplate asbisRestTemplate() {
        // Connection pool configuration using Builder (HttpClient 5.x style)
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setMaxConnTotal(20)
                        .setMaxConnPerRoute(10)
                        .setDefaultSocketConfig(
                                SocketConfig.custom()
                                        .setSoTimeout(Timeout.ofMilliseconds(asbisTimeout))
                                        .build()
                        )
                        .setConnectionTimeToLive(TimeValue.ofSeconds(30))
                        .build();

        // Request configuration with timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionTimeout))
                .setResponseTimeout(Timeout.ofMilliseconds(asbisTimeout))
                .build();

        // Build HTTP Client with connection manager and request config
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();

        // Create HTTP Request Factory
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(factory);
    }
}