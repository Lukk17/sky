package com.lukk.sky.booking.adapters.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lukk.sky.booking.Assemblers.BookingAssembler;
import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.ports.notification.BookingNotificationService;
import com.lukk.sky.booking.domain.ports.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import org.springframework.context.annotation.Import;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lukk.sky.booking.Assemblers.BookingAssembler.*;
import static com.lukk.sky.booking.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("BookingController unit tests (MockMvc)")
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"offerTopic-1"})
@Import({com.lukk.sky.booking.TestSecurityConfig.class, com.lukk.sky.booking.TestcontainersConfiguration.class})
public class BookingControllerTest {

    private Gson gson;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private BookingNotificationService bookingNotificationService;

    private final String API_PREFIX;

    public BookingControllerTest(@Value("${sky.apiPrefix}") String apiPrefix) {
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

    @BeforeEach
    public void beforeAll() {
        gson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .create();
        doNothing().when(bookingNotificationService).sendMessage(any());
    }

    @Test
    @DisplayName("getBookings returns the user's bookings as a paged response when a valid JWT is present")
    public void getBookings_whenJwtIsPresent_thenReturnPagedBookingsJson() throws Exception {
//Given
        List<BookingDTO> bookingsDTO = BookingAssembler.getPopulatedBookedDTOList();
        Pageable pageable = PageRequest.of(0, 20);
        when(bookingService.getBookedOffersForUser(eq(TEST_USER_EMAIL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(bookingsDTO, pageable, bookingsDTO.size()));
//When
        mvc.perform(
                        get("/user/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)))
                )
//Then
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].offerId").value(bookingsDTO.get(0).getOfferId()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("getBookings returns 401 Unauthorized when no JWT is supplied")
    public void getBookings_whenNoJwt_thenReturn401() throws Exception {
//When
        mvc.perform(get("/user/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
//Then
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    @DisplayName("bookOffer creates a booking and returns 201 with the booked DTO when the request is valid")
    public void bookOffer_whenRequestIsValid_thenReturn201WithBookedDto() throws Exception {
//Given
        BookingDTO expected = BookingAssembler.getPopulatedBookedDTO();

        Map<String, String> values = new HashMap<>();
        values.put("offerId", TEST_DEFAULT_OFFER_ID);
        values.put("dateToBook", TEST_DATE.toString());

        when(bookingService.bookOffer(TEST_DEFAULT_OFFER_ID, TEST_DATE.toString(), TEST_USER_EMAIL))
                .thenReturn(expected);

        String jsonValues = gson.toJson(values);
//When
        mvc.perform(
                        post("/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)))
                                .content(jsonValues)
                )
//Then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.offerId").value(expected.getOfferId()))
                .andExpect(jsonPath("$.bookingUser").value(expected.getBookingUser()))
                .andExpect(jsonPath("$.ownerEmail").value(expected.getOwnerEmail()))
                .andExpect(jsonPath("$.bookedDate").value(expected.getBookedDate()));
    }

    @Test
    @DisplayName("bookOffer returns 401 Unauthorized when no JWT is supplied")
    public void bookOffer_whenNoJwt_thenReturn401() throws Exception {
//Given
        Map<String, String> values = new HashMap<>();
        values.put("offerId", " ");
        values.put("dateToBook", TEST_DATE.toString());

        String jsonValues = gson.toJson(values);
//When
        mvc.perform(
                        post("/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonValues)
                )
//Then
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    @DisplayName("deleteBooking returns 200 OK when the booking exists and a valid JWT is present")
    public void deleteBooking_whenBookingExistsAndJwtIsPresent_thenReturnOk() throws Exception {
//Given
        when(bookingService.removeBooking(TEST_DEFAULT_BOOKED_ID.toString(), TEST_USER_EMAIL))
                .thenReturn("Booking removed by user");
//When
        mvc.perform(delete(String.format("/bookings/%s", TEST_DEFAULT_BOOKED_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)))
                )
//Then
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("deleteBooking returns 401 Unauthorized when no JWT is supplied")
    public void deleteBooking_whenNoJwt_thenReturn401() throws Exception {
//Given
        when(bookingService.removeBooking(TEST_DEFAULT_BOOKED_ID.toString(), TEST_USER_EMAIL))
                .thenReturn("Booking removed by user");
//When
        mvc.perform(delete(String.format("/bookings/%s", TEST_DEFAULT_BOOKED_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                )
//Then
                .andExpect(status().isUnauthorized());
    }
}
