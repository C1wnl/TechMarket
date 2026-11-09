
package com.marketplace.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient productRestClient(
            @Value("${product.service.url}") String url,
            @Value("${product.service.connect-timeout-ms}") long connectTimeout,
            @Value("${product.service.read-timeout-ms}") long readTimeout
    ) {
        return createRestClient(url, connectTimeout, readTimeout);
    }

    @Bean
    public RestClient userRestClient(
            @Value("${user.service.url}") String url,
            @Value("${user.service.connect-timeout-ms}") long connectTimeout,
            @Value("${user.service.read-timeout-ms}") long readTimeout
    ) {
        return createRestClient(url, connectTimeout, readTimeout);
    }

    private RestClient createRestClient(
            String url,
            long connectTimeout,
            long readTimeout
    ) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .build();

        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
    }
}
