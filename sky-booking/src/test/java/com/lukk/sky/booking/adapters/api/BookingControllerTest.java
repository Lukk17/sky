package com.lukk.sky.booking.adapters.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lukk.sky.booking.Assemblers.BookingAssembler;
import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.domain.ports.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lukk.sky.booking.Assemblers.BookingAssembler.TEST_DATE;
import static com.lukk.sky.booking.Assemblers.BookingAssembler.TEST_DEFAULT_OFFER_ID;
import static com.lukk.sky.booking.Assemblers.UserAssembler.TEST_OWNER_EMAIL;
import static com.lukk.sky.booking.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static com.lukk.sky.booking.config.Constants.USER_INFO_HEADERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class BookingControllerTest {

    private Gson gson;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BookingService bookingService;

    @BeforeEach
    public void beforeAll() {
        gson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .create();
    }

    @Test
    public void whenGetBooking_thenReturnBookings() throws Exception {

//Given
        List<BookingDTO> bookingsDTO = BookingAssembler.getPopulatedBookedDTOList();

        when(bookingService.getBookedOffersForUser(TEST_USER_EMAIL)).thenReturn(bookingsDTO);

        String expectedJson = gson.toJson(bookingsDTO);

//When
        MvcResult result = mvc.perform(
                        get("/getBooked")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(USER_INFO_HEADERS.iterator().next(), TEST_USER_EMAIL)
                )

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    public void whenBookOffer_thenBookAndReturnOffer() throws Exception {

//Given
        BookingDTO bookingDTO = BookingAssembler.getPopulatedBookedDTO();

        Map<String, String> values = new HashMap<>();
        values.put("offerID", TEST_DEFAULT_OFFER_ID);
        values.put("dateToBook", TEST_DATE.toString());
        values.put("ownerId", TEST_OWNER_EMAIL);

        when(bookingService.bookOffer(TEST_DEFAULT_OFFER_ID, TEST_DATE.toString(), TEST_USER_EMAIL, TEST_OWNER_EMAIL))
                .thenReturn(bookingDTO);

        String expectedJson = gson.toJson(bookingDTO);
        String jsonValues = gson.toJson(values);

//When
        MvcResult result = mvc.perform(
                        post("/book")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(USER_INFO_HEADERS.iterator().next(), TEST_USER_EMAIL)
                                .content(jsonValues)
                )

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    public void whenNoBookingUserInHeader_thenReturnBadRequest() throws Exception {

//Given
        Map<String, String> values = new HashMap<>();
        values.put("offerID", TEST_DEFAULT_OFFER_ID);
        values.put("dateToBook", TEST_DATE.toString());
        values.put("ownerId", TEST_OWNER_EMAIL);

        String jsonValues = gson.toJson(values);

//When
        MvcResult result = mvc.perform(
                        post("/book")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonValues)
                )

//Then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertEquals("No bookingUser for booking", result.getResponse().getContentAsString());
    }
}
