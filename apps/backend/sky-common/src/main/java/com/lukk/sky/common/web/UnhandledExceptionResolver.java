package com.lukk.sky.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Answers an RFC 9457 problem detail for any exception every other resolver declined, so one API never returns
 * two error shapes. Registered at {@link Ordered#LOWEST_PRECEDENCE}, which puts it behind the whole
 * {@code HandlerExceptionResolverComposite}, and therefore behind every {@code @ControllerAdvice} at any order.
 */
@Slf4j
@RequiredArgsConstructor
public class UnhandledExceptionResolver implements HandlerExceptionResolver, Ordered {

    static final String DETAIL =
            "The server failed to complete the request for a reason it did not anticipate. The failure is logged "
                    + "against the X-Correlation-Id this response carries. Nothing in the request needs changing "
                    + "before it is retried.";

    private final ObjectProvider<RequestMappingHandlerAdapter> handlerAdapters;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public @Nullable ModelAndView resolveException(
            HttpServletRequest request,
            HttpServletResponse response,
            @Nullable Object handler,
            Exception ex) {

        log.error("unhandled_exception method={} path={}", request.getMethod(), request.getRequestURI(), ex);

        HttpMessageConverter<ProblemDetail> writer = problemDetailWriter();

        if (writer == null) {
            log.error("unhandled_exception_not_rendered reason=no_problem_json_converter path={}",
                    request.getRequestURI());

            return null;
        }

        try {
            write(writer, request, response);

        } catch (IOException writeFailure) {
            log.error("unhandled_exception_not_rendered reason=write_failed path={}",
                    request.getRequestURI(), writeFailure);

            return null;
        }

        return new ModelAndView();
    }

    private static void write(
            HttpMessageConverter<ProblemDetail> writer,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, DETAIL);
        body.setInstance(URI.create(request.getRequestURI()));

        ServletServerHttpResponse output = new ServletServerHttpResponse(response);
        output.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

        writer.write(body, MediaType.APPLICATION_PROBLEM_JSON, output);
        output.close();
    }

    private @Nullable HttpMessageConverter<ProblemDetail> problemDetailWriter() {
        return handlerAdapters.stream()
                .map(RequestMappingHandlerAdapter::getMessageConverters)
                .flatMap(List::stream)
                .filter(converter -> converter.canWrite(ProblemDetail.class, MediaType.APPLICATION_PROBLEM_JSON))
                .findFirst()
                .map(UnhandledExceptionResolver::asProblemDetailWriter)
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static HttpMessageConverter<ProblemDetail> asProblemDetailWriter(HttpMessageConverter<?> converter) {
        return (HttpMessageConverter<ProblemDetail>) converter;
    }
}
