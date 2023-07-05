package com.lukk.sky.offer.adapters.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lukk.sky.offer.Assemblers.OfferAssembler;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.ports.service.BookingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lukk.sky.offer.Assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static com.lukk.sky.offer.config.Constants.DATE_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BookingControllerTest {

    private Gson gson;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BookingService bookingService;

    @Before
    public void beforeAll() {
        gson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .create();
    }

    @Test
    public void whenGetBookedOffers_thenReturnBookedOffers() throws Exception {

//Given
        List<OfferDTO> offersDTO = OfferAssembler.getPopulatedOffersDTO();
        when(bookingService.getBookedOffers(TEST_USER_EMAIL)).thenReturn(offersDTO);

        String expectedJson = gson.toJson(offersDTO);

//When
        MvcResult result = mvc.perform(
                        get("/getBooked")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("username", TEST_USER_EMAIL)
                )

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    public void whenBookOffer_thenBookAndReturnOffer() throws Exception {

//Given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        String dateToBook = LocalDate.now().format(DATE_FORMAT);

        Map<String, String> values = new HashMap<>();
        values.put("offerID", TEST_DEFAULT_OFFER_ID.toString());
        values.put("dateToBook", dateToBook);

        when(bookingService.bookOffer(TEST_DEFAULT_OFFER_ID.toString(), dateToBook, TEST_USER_EMAIL)).thenReturn(offerDTO);

        String expectedJson = gson.toJson(offerDTO);
        String jsonValues = gson.toJson(values);

//When
        MvcResult result = mvc.perform(
                        post("/book")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("username", TEST_USER_EMAIL)
                                .content(jsonValues)
                )

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    public void whenBookNotExistingOffer_thenReturnBadRequest() throws Exception {

//Given
        String dateToBook = LocalDate.now().format(DATE_FORMAT);

        Map<String, String> values = new HashMap<>();
        values.put("offerID", TEST_DEFAULT_OFFER_ID.toString());
        values.put("dateToBook", dateToBook);

        doThrow(new OfferException("Offer could not be found in repository."))
                .when(bookingService).bookOffer(TEST_DEFAULT_OFFER_ID.toString(), dateToBook, TEST_USER_EMAIL);

        String jsonValues = gson.toJson(values);

//When
        MvcResult result = mvc.perform(
                        post("/book")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("username", TEST_USER_EMAIL)
                                .content(jsonValues)
                )

//Then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertEquals("Offer could not be found in repository.", result.getResponse().getContentAsString());
    }
}
