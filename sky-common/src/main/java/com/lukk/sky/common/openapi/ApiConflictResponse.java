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
        responseCode = "409",
        description = "Conflict: the request is well formed and lost a race. Either another writer advanced "
                + "the event sequence of the resource, or another caller already claimed the state this "
                + "request asked for. There is no Retry-After, because waiting changes nothing: re-read the "
                + "resource and send the change again against its current state. The detail is a fixed "
                + "sentence rather than the internal message.",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
public @interface ApiConflictResponse {
}
