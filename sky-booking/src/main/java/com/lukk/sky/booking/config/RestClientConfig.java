package com.lukk.sky.booking.config;

import com.lukk.sky.common.web.CorrelationIdClientHttpRequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public CorrelationIdClientHttpRequestInterceptor correlationIdInterceptor() {
        return new CorrelationIdClientHttpRequestInterceptor();
    }

    @Bean
    public RestClient restClient(CorrelationIdClientHttpRequestInterceptor correlationIdInterceptor) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(3));

        return RestClient.builder()
                .requestFactory(factory)
                .requestInterceptor(correlationIdInterceptor)
                .build();
    }
}
