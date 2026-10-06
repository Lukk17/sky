package com.lukk.sky.common.openapi;

import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ProblemDetail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = "Bad Request: invalid input or a rejected domain rule",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized: an operation that requires a token answers this when the "
                        + "token is missing or invalid, and an operation that requires none answers it "
                        + "only when a token arrived and failed to verify. The body is empty. "
                        + "Spring Security answers with the status and a WWW-Authenticate header only.",
                content = @Content,
                headers = @Header(
                        name = "WWW-Authenticate",
                        description = "Bearer challenge, carrying error and error_description "
                                + "when the token was present but invalid.",
                        schema = @Schema(type = "string"))),
        @ApiResponse(
                responseCode = "500",
                description = "Internal Server Error: unexpected failure. The body is a problem detail "
                        + "like every other error on this API. Its detail is a fixed sentence carrying no "
                        + "exception type, no message and no class name, so treat a 500 as opaque, retry "
                        + "once, and quote the X-Correlation-Id when reporting it.",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))
})
public @interface ApiCommonErrorResponses {
}
