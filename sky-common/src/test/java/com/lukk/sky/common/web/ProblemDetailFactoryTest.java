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
        ErrorResponse response = ProblemDetailFactory.notFound(new IllegalStateException("gone"));

        // when / then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getDetail()).isEqualTo("Resource not found.");
    }

    @Test
    @DisplayName("forbidden_answers403WithTheSharedDetail")
    void forbidden_answers403WithTheSharedDetail() {
        // given
        ErrorResponse response = ProblemDetailFactory.forbidden(new IllegalStateException("denied"));

        // when / then
        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().getDetail()).isEqualTo("Access denied.");
    }

    @Test
    @DisplayName("badRequest_answers400WithTheSharedDetail")
    void badRequest_answers400WithTheSharedDetail() {
        // given
        ErrorResponse response = ProblemDetailFactory.badRequest(new IllegalStateException("bad"));

        // when / then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getDetail()).isEqualTo("Invalid request.");
    }

    @Test
    @DisplayName("badGateway_answers502WithTheSharedDetail")
    void badGateway_answers502WithTheSharedDetail() {
        // given
        ErrorResponse response = ProblemDetailFactory.badGateway(new IllegalStateException("upstream"));

        // when / then
        assertThat(response.getStatusCode().value()).isEqualTo(502);
        assertThat(response.getBody().getDetail()).isEqualTo("Upstream service failed.");
    }

    @Test
    @DisplayName("payloadTooLarge_answers413WithTheSharedDetail")
    void payloadTooLarge_answers413WithTheSharedDetail() {
        // given
        ErrorResponse response = ProblemDetailFactory.payloadTooLarge(new IllegalStateException("big"));

        // when / then
        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody().getDetail()).isEqualTo("Request payload too large.");
    }

    @Test
    @DisplayName("unavailableWithRetry_answers503WithRetryAfterTen")
    void unavailableWithRetry_answers503WithRetryAfterTen() {
        // given
        ErrorResponse response = ProblemDetailFactory.unavailableWithRetry(new IllegalStateException("down"));

        // when / then
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().getDetail()).isEqualTo("Service temporarily unavailable, please retry.");
        assertThat(response.getHeaders().get(HttpHeaders.RETRY_AFTER))
                .containsExactly(ProblemDetailFactory.RETRY_AFTER_SECONDS);
    }

    @Test
    @DisplayName("conflict_answers409WithTheSharedDetail")
    void conflict_answers409WithTheSharedDetail() {
        // given
        ErrorResponse response = ProblemDetailFactory.conflict(new IllegalStateException("race"));

        // when / then
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getDetail()).isEqualTo("Request conflicts with current state.");
    }

    @Test
    @DisplayName("conflictWithDetail_answers409WithTheSharedDetail")
    void conflictWithDetail_answers409WithTheSharedDetail() {
        // given
        ErrorResponse response = ProblemDetailFactory.conflictWithDetail(
                new IllegalStateException("race"), "a caller-supplied sentence");

        // when / then
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getDetail()).isEqualTo("Request conflicts with current state.");
    }
}
