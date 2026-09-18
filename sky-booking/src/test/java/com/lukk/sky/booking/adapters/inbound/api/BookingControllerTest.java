package com.lukk.sky.booking.adapters.inbound.api;

import com.lukk.sky.booking.assemblers.BookingAssembler;
import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.exception.BookingDateAlreadyBookedException;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.exception.EventSequenceConflictException;
import com.lukk.sky.booking.domain.exception.OfferNotFoundException;
import com.lukk.sky.booking.domain.exception.OfferServiceBadResponseException;
import com.lukk.sky.booking.domain.exception.OfferServiceUnavailableException;
import com.lukk.sky.booking.domain.ports.inbound.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lukk.sky.booking.assemblers.BookingAssembler.TEST_DATE;
import static com.lukk.sky.booking.assemblers.BookingAssembler.TEST_DEFAULT_BOOKED_ID;
import static com.lukk.sky.booking.assemblers.BookingAssembler.TEST_DEFAULT_OFFER_ID;
import static com.lukk.sky.booking.assemblers.UserAssembler.TEST_USER_EMAIL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("BookingController unit tests (MockMvc)")
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"offerTopic-1"})
@Import({com.lukk.sky.booking.TestSecurityConfig.class, com.lukk.sky.booking.TestcontainersConfiguration.class})
class BookingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    private final String API_PREFIX;

    BookingControllerTest(@Value("${sky.apiPrefix}") String apiPrefix) {
        this.API_PREFIX = apiPrefix;
    }

    private MockHttpServletRequestBuilder get(String uri) {
        return MockMvcRequestBuilders.get("/" + API_PREFIX + uri);
    }

    private MockHttpServletRequestBuilder post(String uri) {
        return MockMvcRequestBuilders.post("/" + API_PREFIX + uri);
    }

    private MockHttpServletRequestBuilder delete(String uri) {
        return MockMvcRequestBuilders.delete("/" + API_PREFIX + uri);
    }

    private static RequestPostProcessor userJwt() {
        return jwtWithoutRole().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtWithoutRole() {
        return jwt().jwt(builder -> builder.claim("email", TEST_USER_EMAIL));
    }

    @Test
    @DisplayName("getBookings returns the user's bookings as a paged response when a valid JWT is present")
    void getBookings_whenJwtIsPresent_thenReturnPagedBookingsJson() throws Exception {
        // given
        List<BookingDTO> bookingsDTO = BookingAssembler.getPopulatedBookedDTOList();
        Pageable pageable = PageRequest.of(0, 20);
        when(bookingService.getBookedOffersForUser(eq(TEST_USER_EMAIL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(bookingsDTO, pageable, bookingsDTO.size()));

        // when / then
        mvc.perform(get("/user/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(userJwt()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].offerId").value(bookingsDTO.get(0).getOfferId().toString()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("getBookings returns 403 Forbidden when the JWT carries no user role")
    void getBookings_whenJwtHasNoRole_thenReturn403() throws Exception {
        mvc.perform(get("/user/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwtWithoutRole()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getBookings returns 401 Unauthorized when no JWT is supplied")
    void getBookings_whenNoJwt_thenReturn401() throws Exception {
        mvc.perform(get("/user/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    @DisplayName("bookOffer creates a booking and returns 201 with the booked DTO when the request is valid")
    void bookOffer_whenRequestIsValid_thenReturn201WithBookedDto() throws Exception {
        // given
        BookingDTO expected = BookingAssembler.getPopulatedBookedDTO();
        Map<String, Object> values = new HashMap<>();
        values.put("offerId", TEST_DEFAULT_OFFER_ID.toString());
        values.put("dateToBook", TEST_DATE.toString());
        when(bookingService.bookOffer(TEST_DEFAULT_OFFER_ID, TEST_DATE, TEST_USER_EMAIL))
                .thenReturn(expected);
        String jsonValues = objectMapper.writeValueAsString(values);

        // when / then
        mvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(userJwt())
                        .content(jsonValues))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.offerId").value(expected.getOfferId().toString()))
                .andExpect(jsonPath("$.bookingUser").value(expected.getBookingUser()))
                .andExpect(jsonPath("$.ownerEmail").value(expected.getOwnerEmail()))
                .andExpect(jsonPath("$.bookedDate").value(expected.getBookedDate()));
    }

    @Test
    @DisplayName("bookOffer returns 401 Unauthorized when no JWT is supplied")
    void bookOffer_whenNoJwt_thenReturn401() throws Exception {
        // given
        Map<String, String> values = new HashMap<>();
        values.put("offerId", " ");
        values.put("dateToBook", TEST_DATE.toString());
        String jsonValues = objectMapper.writeValueAsString(values);

        // when / then
        mvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonValues))
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    @DisplayName("deleteBooking returns 200 OK when the booking exists and a valid JWT is present")
    void deleteBooking_whenBookingExistsAndJwtIsPresent_thenReturnOk() throws Exception {
        // when / then
        mvc.perform(delete(String.format("/bookings/%s", TEST_DEFAULT_BOOKED_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(userJwt()))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("bookOffer returns 400 when dateToBook is not a date, because the payload binds a LocalDate")
    void bookOffer_whenDateToBookIsNotADate_thenReturn400() throws Exception {
        // given
        Map<String, Object> values = new HashMap<>();
        values.put("offerId", TEST_DEFAULT_OFFER_ID.toString());
        values.put("dateToBook", "tomorrow");
        String jsonValues = objectMapper.writeValueAsString(values);

        // when / then
        mvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(userJwt())
                        .content(jsonValues))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("bookOffer returns 400 with a field error on dateToBook when the field is absent")
    void bookOffer_whenDateToBookIsMissing_thenReturn400WithFieldError() throws Exception {
        // given
        Map<String, Object> values = new HashMap<>();
        values.put("offerId", TEST_DEFAULT_OFFER_ID.toString());
        String jsonValues = objectMapper.writeValueAsString(values);

        // when / then
        mvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(userJwt())
                        .content(jsonValues))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.['field-errors'].dateToBook").exists());
    }

    @Test
    @DisplayName("deleteBooking returns 401 Unauthorized when no JWT is supplied")
    void deleteBooking_whenNoJwt_thenReturn401() throws Exception {
        // when / then
        mvc.perform(delete(String.format("/bookings/%s", TEST_DEFAULT_BOOKED_ID))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("bookOffer returns 503 with Retry-After 10 when sky-offer is unavailable")
    void bookOffer_whenOfferServiceIsUnavailable_thenReturn503WithRetryAfterAndProblemDetail() throws Exception {
        // given
        String detail = "Offer service unavailable for offerId=" + TEST_DEFAULT_OFFER_ID;
        when(bookingService.bookOffer(TEST_DEFAULT_OFFER_ID, TEST_DATE, TEST_USER_EMAIL))
                .thenThrow(new OfferServiceUnavailableException(detail));

        // when / then
        mvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(userJwt())
                        .content(validBookingJson()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "10"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value(detail));
    }

    @Test
    @DisplayName("bookOffer returns 502 when sky-offer answers a status the booking flow cannot use")
    void bookOffer_whenOfferServiceAnswersUnexpectedStatus_thenReturn502() throws Exception {
        // given
        String detail = "Could not resolve offer owner for offerId=" + TEST_DEFAULT_OFFER_ID;
        when(bookingService.bookOffer(TEST_DEFAULT_OFFER_ID, TEST_DATE, TEST_USER_EMAIL))
                .thenThrow(new OfferServiceBadResponseException(detail));

        // when / then
        mvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(userJwt())
                        .content(validBookingJson()))
                .andExpect(status().isBadGateway())
                .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.detail").value(detail));
    }

    @Test
    @DisplayName("bookOffer returns 404 when sky-offer holds no offer with that id")
    void bookOffer_whenOfferDoesNotExist_thenReturn404AndNoRetryAfter() throws Exception {
        // given
        String detail = "Offer not found for offerId=" + TEST_DEFAULT_OFFER_ID;
        when(bookingService.bookOffer(TEST_DEFAULT_OFFER_ID, TEST_DATE, TEST_USER_EMAIL))
                .thenThrow(new OfferNotFoundException(detail));

        // when / then
        mvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(userJwt())
                        .content(validBookingJson()))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(detail));
    }

    @Test
    @DisplayName("bookOffer still returns 400 when the domain rejects the request itself")
    void bookOffer_whenDomainRejectsTheRequest_thenReturn400() throws Exception {
        // given
        when(bookingService.bookOffer(TEST_DEFAULT_OFFER_ID, TEST_DATE, TEST_USER_EMAIL))
                .thenThrow(new BookingException("You try to book offer with date in the past."));

        // when / then
        mvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(userJwt())
                        .content(validBookingJson()))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("You try to book offer with date in the past."));
    }

    @Test
    @DisplayName("bookOffer returns 409 with no Retry-After when the offer already has a booking on that date")
    void bookOffer_whenTheDateIsAlreadyBooked_thenReturn409WithProblemDetail() throws Exception {
        // given
        String detail = "Offer you try to book was already booked on that date.";
        when(bookingService.bookOffer(TEST_DEFAULT_OFFER_ID, TEST_DATE, TEST_USER_EMAIL))
                .thenThrow(new BookingDateAlreadyBookedException(detail));

        // when / then
        mvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(userJwt())
                        .content(validBookingJson()))
                .andExpect(status().isConflict())
                .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(detail));
    }

    @Test
    @DisplayName("bookOffer returns 409 with a problem detail telling the client to re-read when the event sequence conflicts")
    void bookOffer_whenTheEventSequenceConflicts_thenReturn409TellingTheClientToReRead() throws Exception {
        // given
        when(bookingService.bookOffer(TEST_DEFAULT_OFFER_ID, TEST_DATE, TEST_USER_EMAIL))
                .thenThrow(new EventSequenceConflictException(
                        "Gave up appending a BOOKED event for booking " + TEST_DEFAULT_BOOKED_ID + " after 20 attempts",
                        new IllegalStateException("duplicate key")));

        // when / then
        mvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(userJwt())
                        .content(validBookingJson()))
                .andExpect(status().isConflict())
                .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "A concurrent write advanced the booking event sequence. Re-read the booking and retry the change against its current state."));
    }

    private String validBookingJson() {
        Map<String, Object> values = new HashMap<>();
        values.put("offerId", TEST_DEFAULT_OFFER_ID.toString());
        values.put("dateToBook", TEST_DATE.toString());

        return objectMapper.writeValueAsString(values);
    }
}
