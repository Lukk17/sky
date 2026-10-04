package com.lukk.sky.booking.adapters.inbound.api;

import com.lukk.sky.booking.TestSecurityConfig;
import com.lukk.sky.booking.TestcontainersConfiguration;
import com.lukk.sky.booking.domain.exception.BookingAccessDeniedException;
import com.lukk.sky.booking.domain.exception.BookingDateAlreadyBookedException;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.exception.BookingNotFoundException;
import com.lukk.sky.booking.domain.exception.EventSequenceConflictException;
import com.lukk.sky.booking.domain.exception.OfferNotFoundException;
import com.lukk.sky.booking.domain.exception.OfferServiceBadResponseException;
import com.lukk.sky.booking.domain.exception.OfferServiceUnavailableException;
import com.lukk.sky.booking.domain.ports.inbound.BookingService;
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

import java.util.UUID;
import java.util.stream.Stream;

import static com.lukk.sky.common.test.TestUsers.TEST_USER_EMAIL;
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
@DisplayName("sky-booking exception mapping precedence")
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"offerTopic-1"})
@Import({TestSecurityConfig.class, TestcontainersConfiguration.class})
class ExceptionMappingPrecedenceTest {

    private static final UUID BOOKING_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BookingService bookingService;

    private final String apiPrefix;

    ExceptionMappingPrecedenceTest(@Value("${sky.apiPrefix}") String apiPrefix) {
        this.apiPrefix = apiPrefix;
    }

    static Stream<Arguments> mappedFailures() {
        return Stream.of(
                Arguments.of("BookingNotFoundException",
                        new BookingNotFoundException("No booking with that id."),
                        404, "Resource not found."),
                Arguments.of("OfferNotFoundException",
                        new OfferNotFoundException("No offer with that id."),
                        404, "Resource not found."),
                Arguments.of("BookingAccessDeniedException",
                        new BookingAccessDeniedException("Not yours to cancel."),
                        403, "Access denied."),
                Arguments.of("OfferServiceUnavailableException",
                        new OfferServiceUnavailableException("sky-offer is unreachable."),
                        503, "Service temporarily unavailable, please retry."),
                Arguments.of("OfferServiceBadResponseException",
                        new OfferServiceBadResponseException("sky-offer answered 401."),
                        502, "Upstream service failed."),
                Arguments.of("BookingDateAlreadyBookedException",
                        new BookingDateAlreadyBookedException("Already booked on that date."),
                        409, "Request conflicts with current state."),
                Arguments.of("EventSequenceConflictException",
                        new EventSequenceConflictException("lost 20 races", new IllegalStateException("duplicate key")),
                        409, "Request conflicts with current state."),
                Arguments.of("BookingException",
                        new BookingException("Date is in the past."),
                        400, "Invalid request."));
    }

    @ParameterizedTest(name = "{0} still answers {2}")
    @MethodSource("mappedFailures")
    @DisplayName("mappedFailure_keepsItsOwnStatusAndDetail_ratherThanTheLastResort500")
    void mappedFailure_keepsItsOwnStatusAndDetail_ratherThanTheLastResort500(
            String exceptionType,
            RuntimeException failure,
            int expectedStatus,
            String expectedDetail) throws Exception {

        doThrow(failure).when(bookingService).removeBooking(eq(BOOKING_ID), any());

        mvc.perform(delete("/bookings/" + BOOKING_ID).with(userJwt()))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail").value(expectedDetail));
    }

    @Test
    @DisplayName("outageMapping_keepsItsRetryAfterHeader")
    void outageMapping_keepsItsRetryAfterHeader() throws Exception {
        doThrow(new OfferServiceUnavailableException("sky-offer is unreachable."))
                .when(bookingService).removeBooking(eq(BOOKING_ID), any());

        mvc.perform(delete("/bookings/" + BOOKING_ID).with(userJwt()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "10"));
    }

    @Test
    @DisplayName("unmappedFailure_answersProblemDetail500_ratherThanTheFlatDefaultErrorBody")
    void unmappedFailure_answersProblemDetail500_ratherThanTheFlatDefaultErrorBody() throws Exception {
        doThrow(new IllegalStateException("offerOwner was null"))
                .when(bookingService).removeBooking(eq(BOOKING_ID), any());

        mvc.perform(delete("/bookings/" + BOOKING_ID).with(userJwt()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.instance").value("/" + apiPrefix + "/bookings/" + BOOKING_ID))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    @Test
    @DisplayName("unmappedFailure_namesNeitherTheExceptionTypeNorItsMessage")
    void unmappedFailure_namesNeitherTheExceptionTypeNorItsMessage() throws Exception {
        doThrow(new IllegalStateException("offerOwner was null"))
                .when(bookingService).removeBooking(eq(BOOKING_ID), any());

        String body = mvc.perform(delete("/bookings/" + BOOKING_ID).with(userJwt()))
                .andExpect(status().isInternalServerError())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .as("an unanticipated failure is where an internal message is most likely to leak")
                .doesNotContain("IllegalStateException")
                .doesNotContain("offerOwner was null")
                .doesNotContain("com.lukk.sky");
    }

    private MockHttpServletRequestBuilder delete(String uri) {
        return MockMvcRequestBuilders.delete("/" + apiPrefix + uri);
    }

    private static RequestPostProcessor userJwt() {
        return jwt()
                .jwt(builder -> builder.claim("email", TEST_USER_EMAIL))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
