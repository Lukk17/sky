package com.lukk.sky.message.adapters.inbound.api;

import com.lukk.sky.message.TestSecurityConfig;
import com.lukk.sky.message.TestcontainersConfiguration;
import com.lukk.sky.message.domain.exception.MessageAccessDeniedException;
import com.lukk.sky.message.domain.exception.MessageException;
import com.lukk.sky.message.domain.exception.MessageNotFoundException;
import com.lukk.sky.message.domain.ports.inbound.MessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;
import java.util.stream.Stream;

import static com.lukk.sky.message.assemblers.MessageAssembler.SENDER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins that the shared last-resort 500 never takes a response away from this service's own advice, and that
 * an exception nobody mapped answers as a problem detail instead of Spring Boot's flat error body.
 */
@DisplayName("sky-message exception mapping precedence")
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class, TestcontainersConfiguration.class})
class ExceptionMappingPrecedenceTest {

    private static final UUID MESSAGE_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MessageService messageService;

    private final String apiPrefix;

    ExceptionMappingPrecedenceTest(@Value("${sky.apiPrefix}") String apiPrefix) {
        this.apiPrefix = apiPrefix;
    }

    static Stream<Arguments> mappedFailures() {
        return Stream.of(
                Arguments.of("MessageNotFoundException",
                        new MessageNotFoundException("No message with that id."),
                        404, "No message with that id."),
                Arguments.of("MessageAccessDeniedException",
                        new MessageAccessDeniedException("Not your message."),
                        403, "Not your message."),
                Arguments.of("MessageException",
                        new MessageException("Message text must not be blank."),
                        400, "Message text must not be blank."));
    }

    @ParameterizedTest(name = "{0} still answers {2}")
    @MethodSource("mappedFailures")
    @DisplayName("mappedFailure_keepsItsOwnStatusAndDetail_ratherThanTheLastResort500")
    void mappedFailure_keepsItsOwnStatusAndDetail_ratherThanTheLastResort500(
            String exceptionType,
            RuntimeException failure,
            int expectedStatus,
            String expectedDetail) throws Exception {

        doThrow(failure).when(messageService).remove(eq(MESSAGE_ID), any());

        mvc.perform(delete("/messages/" + MESSAGE_ID).with(senderJwt()))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail").value(expectedDetail));
    }

    @Test
    @DisplayName("noMappingCarriesRetryAfter_becauseThisServiceHasNoDependencyToWaitFor")
    void noMappingCarriesRetryAfter_becauseThisServiceHasNoDependencyToWaitFor() throws Exception {
        doThrow(new MessageNotFoundException("No message with that id."))
                .when(messageService).remove(eq(MESSAGE_ID), any());

        mvc.perform(delete("/messages/" + MESSAGE_ID).with(senderJwt()))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER));
    }

    @Test
    @DisplayName("unmappedFailure_answersProblemDetail500_ratherThanTheFlatDefaultErrorBody")
    void unmappedFailure_answersProblemDetail500_ratherThanTheFlatDefaultErrorBody() throws Exception {
        doThrow(new IllegalStateException("senderEmail was null"))
                .when(messageService).remove(eq(MESSAGE_ID), any());

        mvc.perform(delete("/messages/" + MESSAGE_ID).with(senderJwt()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.instance").value(apiPrefix + "/messages/" + MESSAGE_ID))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    @Test
    @DisplayName("unmappedFailure_namesNeitherTheExceptionTypeNorItsMessage")
    void unmappedFailure_namesNeitherTheExceptionTypeNorItsMessage() throws Exception {
        doThrow(new IllegalStateException("senderEmail was null"))
                .when(messageService).remove(eq(MESSAGE_ID), any());

        String body = mvc.perform(delete("/messages/" + MESSAGE_ID).with(senderJwt()))
                .andExpect(status().isInternalServerError())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .as("an unanticipated failure is where an internal message is most likely to leak")
                .doesNotContain("IllegalStateException")
                .doesNotContain("senderEmail was null")
                .doesNotContain("com.lukk.sky");
    }

    private MockHttpServletRequestBuilder delete(String uri) {
        return MockMvcRequestBuilders.delete(apiPrefix + uri);
    }

    private static RequestPostProcessor senderJwt() {
        return jwt()
                .jwt(builder -> builder.claim("email", SENDER_EMAIL))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
