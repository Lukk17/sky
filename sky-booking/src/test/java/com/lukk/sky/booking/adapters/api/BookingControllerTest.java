package com.lukk.sky.booking.adapters.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lukk.sky.booking.Assemblers.BookingAssembler;
import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.ports.notification.BookingNotificationService;
import com.lukk.sky.booking.domain.ports.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lukk.sky.booking.Assemblers.BookingAssembler.*;
import static com.lukk.sky.booking.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static com.lukk.sky.booking.config.Constants.USER_INFO_HEADERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.fail;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"offerTopic-1"})
public class BookingControllerTest {

    private Gson gson;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
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
    public void whenGoOnlyDash_thenReturnWelcomingMessage() throws Exception {
//When
        MvcResult result = mvc.perform(
                        get("/")
                                .contentType(MediaType.APPLICATION_JSON))
//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals("<center><h1>Welcome to Booking app.</h1></center>", result.getResponse().getContentAsString());
    }

    @Test
    public void whenGetBooking_thenReturnBookings() throws Exception {
//Given
        List<BookingDTO> bookingsDTO = BookingAssembler.getPopulatedBookedDTOList();

        when(bookingService.getBookedOffersForUser(TEST_USER_EMAIL)).thenReturn(bookingsDTO);

        String expectedJson = gson.toJson(bookingsDTO);
//When
        MvcResult result = mvc.perform(
                        get("/user/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(USER_INFO_HEADERS.iterator().next(), TEST_USER_EMAIL)
                )
//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    public void whenGetBookingsError_thenReturnBadRequest() throws Exception {

//When
        mvc.perform(get("/user/bookings")
                        .contentType(MediaType.APPLICATION_JSON))
//Then
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void whenBookOffer_thenBookAndReturnOffer() throws Exception {
//Given
        BookingDTO expected = BookingAssembler.getPopulatedBookedDTO();

        Map<String, String> values = new HashMap<>();
        values.put("offerId", TEST_DEFAULT_OFFER_ID);
        values.put("dateToBook", TEST_DATE.toString());

        when(bookingService.bookOffer(TEST_DEFAULT_OFFER_ID, TEST_DATE.toString(), TEST_USER_EMAIL))
                .thenReturn(Mono.just(expected));

        String jsonValues = gson.toJson(values);
//When
        MvcResult result = mvc.perform(
                        post("/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(USER_INFO_HEADERS.iterator().next(), TEST_USER_EMAIL)
                                .content(jsonValues)
                )
//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        ResponseEntity<BookingDTO> actual = castAsyncResultToBookingResponse(result);

        assert actual != null;
        assertTrue(actual.getStatusCode().is2xxSuccessful());
        assertEquals(expected, actual.getBody());

    }

    @Test
    public void whenNoBookingUserInHeader_thenReturnBadRequest() throws Exception {
//Given
        Map<String, String> values = new HashMap<>();
        values.put("offerId", " ");
        values.put("dateToBook", TEST_DATE.toString());

        String jsonValues = gson.toJson(values);
//When
        MvcResult result = mvc.perform(
                        post("/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonValues)
                )
//Then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("Field 'offerId' must not be blank"));
    }

    @Test
    public void whenDeleteBooking_thenStatusOk() throws Exception {
//Given
        when(bookingService.removeBooking(TEST_DEFAULT_BOOKED_ID.toString(), TEST_USER_EMAIL))
                .thenReturn("Booking removed by user");
//When
        mvc.perform(delete(String.format("/bookings/%s", TEST_DEFAULT_BOOKED_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(USER_INFO_HEADERS.iterator().next(), TEST_USER_EMAIL)
                )
//Then
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void whenDeleteBookingsError_thenReturnBadRequest() throws Exception {
//Given
        when(bookingService.removeBooking(TEST_DEFAULT_BOOKED_ID.toString(), TEST_USER_EMAIL))
                .thenReturn("Booking removed by user");
//When
        mvc.perform(delete(String.format("/bookings/%s", TEST_DEFAULT_BOOKED_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                )
//Then
                .andExpect(status().isBadRequest());
    }

    private ResponseEntity<BookingDTO> castAsyncResultToBookingResponse(MvcResult result) {
        Object actualObject = result.getAsyncResult(10);

        if (actualObject instanceof ResponseEntity<?> responseEntity) {
            if (responseEntity.getBody() instanceof BookingDTO body) {

                return new ResponseEntity<>(body, responseEntity.getHeaders(), responseEntity.getStatusCode());

            } else {
                fail("Response body is not of type BookingDTO");
            }
        } else {
            fail("Actual object is not of type ResponseEntity");
        }
        return null;
    }
}
