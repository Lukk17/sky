package com.lukk.sky.common.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.ErrorResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProblemDetailFactory")
class ProblemDetailFactoryTest {

    @Test
    @DisplayName("notFound_answers404WithTheSharedDetail")
    void notFound_answers404WithTheSharedDetail() {
        // given
        // when
        // then
        ErrorResponse response = ProblemDetailFactory.notFound(new IllegalStateException("gone"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getDetail()).isEqualTo("Resource not found.");
    }

    @Test
    @DisplayName("forbidden_answers403WithTheSharedDetail")
    void forbidden_answers403WithTheSharedDetail() {
        // given
        // when
        // then
        ErrorResponse response = ProblemDetailFactory.forbidden(new IllegalStateException("denied"));

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().getDetail()).isEqualTo("Access denied.");
    }

    @Test
    @DisplayName("badRequest_answers400WithTheSharedDetail")
    void badRequest_answers400WithTheSharedDetail() {
        // given
        // when
        // then
        ErrorResponse response = ProblemDetailFactory.badRequest(new IllegalStateException("bad"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getDetail()).isEqualTo("Invalid request.");
    }

    @Test
    @DisplayName("badGateway_answers502WithTheSharedDetail")
    void badGateway_answers502WithTheSharedDetail() {
        // given
        // when
        // then
        ErrorResponse response = ProblemDetailFactory.badGateway(new IllegalStateException("upstream"));

        assertThat(response.getStatusCode().value()).isEqualTo(502);
        assertThat(response.getBody().getDetail()).isEqualTo("Upstream service failed.");
    }

    @Test
    @DisplayName("payloadTooLarge_answers413WithTheSharedDetail")
    void payloadTooLarge_answers413WithTheSharedDetail() {
        // given
        // when
        // then
        ErrorResponse response = ProblemDetailFactory.payloadTooLarge(new IllegalStateException("big"));

        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody().getDetail()).isEqualTo("Request payload too large.");
    }

    @Test
    @DisplayName("unavailableWithRetry_answers503WithRetryAfterTen")
    void unavailableWithRetry_answers503WithRetryAfterTen() {
        // given
        // when
        // then
        ErrorResponse response = ProblemDetailFactory.unavailableWithRetry(new IllegalStateException("down"));

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().getDetail()).isEqualTo("Service temporarily unavailable, please retry.");
        assertThat(response.getHeaders().get(HttpHeaders.RETRY_AFTER))
                .containsExactly(ProblemDetailFactory.RETRY_AFTER_SECONDS);
    }

    @Test
    @DisplayName("conflict_answers409WithTheSharedDetail")
    void conflict_answers409WithTheSharedDetail() {
        // given
        // when
        // then
        ErrorResponse response = ProblemDetailFactory.conflict(new IllegalStateException("race"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getDetail()).isEqualTo("Request conflicts with current state.");
    }

    @Test
    @DisplayName("conflictWithDetail_answers409WithTheSharedDetail")
    void conflictWithDetail_answers409WithTheSharedDetail() {
        // given
        // when
        // then
        ErrorResponse response = ProblemDetailFactory.conflictWithDetail(
                new IllegalStateException("race"), "a caller-supplied sentence");

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getDetail()).isEqualTo("Request conflicts with current state.");
    }
}
