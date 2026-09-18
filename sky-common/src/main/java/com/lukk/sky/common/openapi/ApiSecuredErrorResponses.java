package com.lukk.sky.common.openapi;

import io.swagger.v3.oas.annotations.headers.Header;
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
        responseCode = "403",
        description = "Forbidden: the caller is authenticated but not allowed. A missing realm role "
                + "is answered by Spring Security with an empty body and a WWW-Authenticate header. "
                + "A domain authorization failure carries a problem detail.",
        content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)),
        headers = @Header(
                name = "WWW-Authenticate",
                description = "Bearer challenge with error=\"insufficient_scope\", "
                        + "present only when the realm role check denied the call.",
                schema = @Schema(type = "string")))
public @interface ApiSecuredErrorResponses {
}
