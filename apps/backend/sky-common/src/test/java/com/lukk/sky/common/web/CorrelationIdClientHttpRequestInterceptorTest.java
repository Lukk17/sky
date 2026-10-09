package com.lukk.sky.common.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CorrelationIdClientHttpRequestInterceptor")
class CorrelationIdClientHttpRequestInterceptorTest {

    private final CorrelationIdClientHttpRequestInterceptor interceptor =
            new CorrelationIdClientHttpRequestInterceptor();

    private final ClientHttpRequestExecution execution = (request, body) ->
            new MockClientHttpResponse(new byte[0], 200);

    @AfterEach
    void clearMdc() {
        CorrelationId.clear();
    }

    @Test
    @DisplayName("copies the current correlation ID onto the outbound request")
    void setsHeader_whenCorrelationIdInScope() throws IOException {
        // given
        CorrelationId.set("outbound-42");
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://offer/api"));

        // when
        ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

        // then
        assertThat(request.getHeaders().getFirst(CorrelationId.HEADER))
                .as("the outbound call must carry the inbound correlation ID")
                .isEqualTo("outbound-42");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("leaves the outbound request untouched when no correlation ID is in scope")
    void setsNoHeader_whenNoCorrelationIdInScope() throws IOException {
        // given
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://offer/api"));

        // when
        interceptor.intercept(request, new byte[0], execution);

        // then
        assertThat(request.getHeaders().containsHeader(CorrelationId.HEADER))
                .as("no correlation ID in scope must leave the header unset")
                .isFalse();
    }
}
