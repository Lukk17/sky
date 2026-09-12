package com.lukk.sky.common.web;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Copies the correlation ID of the current request onto every outbound call made through a
 * {@code RestClient} or {@code RestTemplate} the interceptor is registered on.
 */
public class CorrelationIdClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        CorrelationId.current().ifPresent(correlationId ->
                request.getHeaders().set(CorrelationId.HEADER, correlationId));

        return execution.execute(request, body);
    }
}
