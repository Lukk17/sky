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
        responseCode = "502",
        description = "Bad Gateway: a dependency this operation needs answered, and answered in a way the "
                + "service cannot act on. The caller's own request was well formed and retrying it unchanged "
                + "will not help, so there is no Retry-After. The cause is between the service and its "
                + "dependency.",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
public @interface ApiDependencyBadGatewayResponse {
}
