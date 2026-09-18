package com.lukk.sky.offer.adapters.inbound.api;

import com.lukk.sky.offer.TestSecurityConfig;
import com.lukk.sky.offer.TestS3Config;
import com.lukk.sky.offer.TestcontainersConfiguration;
import com.lukk.sky.offer.domain.exception.EventSequenceConflictException;
import com.lukk.sky.offer.domain.exception.OfferAccessDeniedException;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.exception.OfferNotFoundException;
import com.lukk.sky.offer.domain.exception.PhotoStorageBadResponseException;
import com.lukk.sky.offer.domain.exception.PhotoStorageUnavailableException;
import com.lukk.sky.offer.domain.ports.inbound.OfferService;
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
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;

import static com.lukk.sky.offer.assemblers.UserAssembler.TEST_OWNER_EMAIL;
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
@DisplayName("sky-offer exception mapping precedence")
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"offerTopic-1"})
@Import({TestSecurityConfig.class, TestcontainersConfiguration.class, TestS3Config.class})
class ExceptionMappingPrecedenceTest {

    private static final UUID OFFER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private static final long FIVE_MEGABYTES = 5L * 1024 * 1024;

    private static final String SEQUENCE_CONFLICT_DETAIL =
            "A concurrent write advanced the offer event sequence. "
                    + "Re-read the offer and retry the change against its current state.";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OfferService offerService;

    private final String apiPrefix;

    ExceptionMappingPrecedenceTest(@Value("${sky.apiPrefix}") String apiPrefix) {
        this.apiPrefix = apiPrefix;
    }

    static Stream<Arguments> mappedFailures() {
        return Stream.of(
                Arguments.of("OfferNotFoundException",
                        new OfferNotFoundException("No offer with that id."),
                        404, "No offer with that id."),
                Arguments.of("OfferAccessDeniedException",
                        new OfferAccessDeniedException("Not your offer."),
                        403, "Not your offer."),
                Arguments.of("EventSequenceConflictException",
                        new EventSequenceConflictException("lost 20 races", new IllegalStateException("duplicate key")),
                        409, SEQUENCE_CONFLICT_DETAIL),
                Arguments.of("PhotoStorageUnavailableException",
                        new PhotoStorageUnavailableException(
                                "Photo delete failed. The object store is unavailable.", new IOException("refused")),
                        503, "Photo delete failed. The object store is unavailable."),
                Arguments.of("PhotoStorageBadResponseException",
                        new PhotoStorageBadResponseException(
                                "Photo delete failed. The object store rejected the request.",
                                new IllegalStateException("403")),
                        502, "Photo delete failed. The object store rejected the request."),
                Arguments.of("OfferException",
                        new OfferException("Uploaded file must not be empty."),
                        400, "Uploaded file must not be empty."));
    }

    @ParameterizedTest(name = "{0} still answers {2}")
    @MethodSource("mappedFailures")
    @DisplayName("mappedFailure_keepsItsOwnStatusAndDetail_ratherThanTheLastResort500")
    void mappedFailure_keepsItsOwnStatusAndDetail_ratherThanTheLastResort500(
            String exceptionType,
            RuntimeException failure,
            int expectedStatus,
            String expectedDetail) throws Exception {

        doThrow(failure).when(offerService).deleteOffer(eq(OFFER_ID), any());

        mvc.perform(delete("/owner/offers/" + OFFER_ID).with(ownerJwt()))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail").value(expectedDetail));
    }

    @Test
    @DisplayName("outageMapping_keepsItsRetryAfterHeader")
    void outageMapping_keepsItsRetryAfterHeader() throws Exception {
        doThrow(new PhotoStorageUnavailableException("store down", new IOException("refused")))
                .when(offerService).deleteOffer(eq(OFFER_ID), any());

        mvc.perform(delete("/owner/offers/" + OFFER_ID).with(ownerJwt()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "10"));
    }

    @Test
    @DisplayName("oversizedUpload_staysWithTheSharedHandlerAnd413_ratherThanTheLastResort500")
    void oversizedUpload_staysWithTheSharedHandlerAnd413_ratherThanTheLastResort500() throws Exception {
        doThrow(new MaxUploadSizeExceededException(FIVE_MEGABYTES))
                .when(offerService).deleteOffer(eq(OFFER_ID), any());

        mvc.perform(delete("/owner/offers/" + OFFER_ID).with(ownerJwt()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(413));
    }

    @Test
    @DisplayName("validationFailure_staysWithTheSharedHandlerAndKeepsItsFieldErrors")
    void validationFailure_staysWithTheSharedHandlerAndKeepsItsFieldErrors() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/" + apiPrefix + "/owner/offers")
                        .with(ownerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.field-errors").exists());
    }

    @Test
    @DisplayName("unmappedFailure_answersProblemDetail500_ratherThanTheFlatDefaultErrorBody")
    void unmappedFailure_answersProblemDetail500_ratherThanTheFlatDefaultErrorBody() throws Exception {
        doThrow(new NullPointerException("photoObjectKey was null"))
                .when(offerService).deleteOffer(eq(OFFER_ID), any());

        mvc.perform(delete("/owner/offers/" + OFFER_ID).with(ownerJwt()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.instance").value("/" + apiPrefix + "/owner/offers/" + OFFER_ID))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    @Test
    @DisplayName("unmappedFailure_namesNeitherTheExceptionTypeNorItsMessage")
    void unmappedFailure_namesNeitherTheExceptionTypeNorItsMessage() throws Exception {
        doThrow(new NullPointerException("photoObjectKey was null"))
                .when(offerService).deleteOffer(eq(OFFER_ID), any());

        String body = mvc.perform(delete("/owner/offers/" + OFFER_ID).with(ownerJwt()))
                .andExpect(status().isInternalServerError())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .as("an unanticipated failure is where an internal message is most likely to leak")
                .doesNotContain("NullPointerException")
                .doesNotContain("photoObjectKey was null")
                .doesNotContain("com.lukk.sky");
    }

    private MockHttpServletRequestBuilder delete(String uri) {
        return MockMvcRequestBuilders.delete("/" + apiPrefix + uri);
    }

    private static RequestPostProcessor ownerJwt() {
        return jwt()
                .jwt(builder -> builder.claim("email", TEST_OWNER_EMAIL))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
