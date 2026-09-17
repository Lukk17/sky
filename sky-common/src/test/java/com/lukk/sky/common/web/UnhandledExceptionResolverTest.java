package com.lukk.sky.common.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UnhandledExceptionResolver")
class UnhandledExceptionResolverTest {

    private final MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/bookings/42");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private ListAppender<ILoggingEvent> logAppender;
    private Logger resolverLogger;

    @BeforeEach
    void captureLog() {
        resolverLogger = (Logger) LoggerFactory.getLogger(UnhandledExceptionResolver.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        resolverLogger.addAppender(logAppender);
    }

    @AfterEach
    void releaseLog() {
        resolverLogger.detachAppender(logAppender);
        CorrelationId.clear();
    }

    @Test
    @DisplayName("logsTheFailureAtErrorWithItsStackAndTheCorrelationIdInTheMdc")
    void logsTheFailureAtErrorWithItsStackAndTheCorrelationIdInTheMdc() {
        CorrelationId.set("corr-1234");

        resolverWith(new RecordingProblemDetailConverter())
                .resolveException(request, response, null, new IllegalStateException("ownerEmail was null"));

        ILoggingEvent logged = logAppender.list.getFirst();

        assertThat(logged.getLevel()).isEqualTo(Level.ERROR);
        assertThat(logged.getFormattedMessage())
                .isEqualTo("unhandled_exception method=DELETE path=/api/v1/bookings/42");
        assertThat(logged.getMDCPropertyMap())
                .as("every service renders %X{correlationId} in its logback pattern, so the id reaches the line")
                .containsEntry(CorrelationId.MDC_KEY, "corr-1234");
        assertThat(logged.getThrowableProxy())
                .as("swallowing the stack would trade a bad response for a silent failure")
                .isNotNull();
        assertThat(logged.getThrowableProxy().getMessage()).isEqualTo("ownerEmail was null");
    }

    @Test
    @DisplayName("declinesTheException_whenNoConverterCanWriteAProblemDetail")
    void declinesTheException_whenNoConverterCanWriteAProblemDetail() {
        ModelAndView resolved = resolverWith(new StringHttpMessageConverter())
                .resolveException(request, response, null, new IllegalStateException("boom"));

        assertThat(resolved)
                .as("declining leaves the container error page in charge rather than answering an empty 200")
                .isNull();
        assertThat(logAppender.list)
                .anyMatch(event -> event.getFormattedMessage().contains("reason=no_problem_json_converter"));
    }

    @Test
    @DisplayName("declinesTheException_whenNoHandlerAdapterExists")
    void declinesTheException_whenNoHandlerAdapterExists() {
        ModelAndView resolved = new UnhandledExceptionResolver(StubObjectProvider.empty())
                .resolveException(request, response, null, new IllegalStateException("boom"));

        assertThat(resolved).isNull();
    }

    @Test
    @DisplayName("declinesTheException_whenWritingTheResponseFails")
    void declinesTheException_whenWritingTheResponseFails() {
        ModelAndView resolved = resolverWith(new FailingProblemDetailConverter())
                .resolveException(request, response, null, new IllegalStateException("boom"));

        assertThat(resolved).isNull();
        assertThat(logAppender.list)
                .anyMatch(event -> event.getFormattedMessage().contains("reason=write_failed"));
    }

    @Test
    @DisplayName("writesA500ProblemDetailWhoseInstanceIsTheRequestPath")
    void writesA500ProblemDetailWhoseInstanceIsTheRequestPath() {
        RecordingProblemDetailConverter converter = new RecordingProblemDetailConverter();

        ModelAndView resolved = resolverWith(converter)
                .resolveException(request, response, null, new IllegalStateException("boom"));

        assertThat(resolved)
                .as("an empty ModelAndView tells the DispatcherServlet the response is already written")
                .isNotNull();
        assertThat(resolved.isEmpty()).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(converter.written.getStatus()).isEqualTo(500);
        assertThat(converter.written.getTitle()).isEqualTo("Internal Server Error");
        assertThat(converter.written.getDetail()).isEqualTo(UnhandledExceptionResolver.DETAIL);
        assertThat(converter.written.getInstance()).hasToString("/api/v1/bookings/42");
        assertThat(converter.writtenAs).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    @DisplayName("detailNamesNeitherAnExceptionTypeNorAPackage")
    void detailNamesNeitherAnExceptionTypeNorAPackage() {
        assertThat(UnhandledExceptionResolver.DETAIL)
                .doesNotContain("Exception")
                .doesNotContain("com.lukk")
                .doesNotContain("org.springframework");
    }

    private static UnhandledExceptionResolver resolverWith(HttpMessageConverter<?> converter) {
        RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
        adapter.setMessageConverters(List.of(converter));

        return new UnhandledExceptionResolver(StubObjectProvider.of(adapter));
    }

    private static final class StubObjectProvider implements ObjectProvider<RequestMappingHandlerAdapter> {

        private final List<RequestMappingHandlerAdapter> adapters;

        private StubObjectProvider(List<RequestMappingHandlerAdapter> adapters) {
            this.adapters = adapters;
        }

        private static StubObjectProvider of(RequestMappingHandlerAdapter adapter) {
            return new StubObjectProvider(List.of(adapter));
        }

        private static StubObjectProvider empty() {
            return new StubObjectProvider(List.of());
        }

        @Override
        public Stream<RequestMappingHandlerAdapter> stream() {
            return adapters.stream();
        }

        @Override
        public RequestMappingHandlerAdapter getObject() {
            return adapters.getFirst();
        }
    }

    private static class RecordingProblemDetailConverter extends ProblemDetailConverterStub {

        @Override
        public void write(ProblemDetail body, MediaType contentType, HttpOutputMessage output) {
            written = body;
            writtenAs = contentType;
        }
    }

    private static final class FailingProblemDetailConverter extends ProblemDetailConverterStub {

        @Override
        public void write(ProblemDetail body, MediaType contentType, HttpOutputMessage output) throws IOException {
            throw new IOException("the client went away mid-response");
        }
    }

    private abstract static class ProblemDetailConverterStub implements HttpMessageConverter<ProblemDetail> {

        ProblemDetail written;
        MediaType writtenAs;

        @Override
        public boolean canRead(Class<?> clazz, MediaType mediaType) {
            return false;
        }

        @Override
        public boolean canWrite(Class<?> clazz, MediaType mediaType) {
            return ProblemDetail.class.isAssignableFrom(clazz);
        }

        @Override
        public List<MediaType> getSupportedMediaTypes() {
            return List.of(MediaType.APPLICATION_PROBLEM_JSON);
        }

        @Override
        public ProblemDetail read(Class<? extends ProblemDetail> clazz, HttpInputMessage input) {
            throw new UnsupportedOperationException();
        }
    }
}
