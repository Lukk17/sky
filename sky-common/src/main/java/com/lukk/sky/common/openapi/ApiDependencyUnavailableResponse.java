package com.lukk.sky.common.openapi;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ProblemDetail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
        responseCode = "503",
        description = "Service Unavailable: a dependency this operation needs could not be used, so the "
                + "operation did not complete. Covers a refused connection, a connect timeout, a read "
                + "timeout, a 5xx after the retries are exhausted, a 429, and an open circuit breaker. The "
                + "request itself was well formed, so retry it unchanged after the number of seconds in the "
                + "Retry-After header, which is always 10. The detail is one of a fixed set of sentences and "
                + "carries nothing from the dependency's own answer.",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
public @interface ApiDependencyUnavailableResponse {
}
