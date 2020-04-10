package com.lukk.sky.offer.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lukk.sky.offer.Assemblers.OfferAssembler;
import com.lukk.sky.offer.dto.OfferDTO;
import com.lukk.sky.offer.exception.OfferException;
import com.lukk.sky.offer.service.OfferService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lukk.sky.offer.Assemblers.OfferAssembler.*;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static com.lukk.sky.offer.service.OfferServiceImpl.DATE_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class OfferControllerTest {

    private Gson gson;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private OfferService offerService;

    @Before
    public void beforeAll() {
        gson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .create();
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

        assertEquals("Offer service for Sky", result.getResponse().getContentAsString());
    }

    @Test
    public void whenGoHomePage_thenReturnWelcomingMessage() throws Exception {

//When
        MvcResult result = mvc.perform(
                get("/home")
                        .contentType(MediaType.APPLICATION_JSON))

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals("Offer service for Sky", result.getResponse().getContentAsString());
    }

    @Test
    public void whenGetAllOffers_thenReturnOffers() throws Exception {

//Given
        List<OfferDTO> offersDTO = OfferAssembler.getPopulatedOffersDTO();
        when(offerService.getAllOffers()).thenReturn(offersDTO);

        String expectedJson = gson.toJson(offersDTO);

//When
        MvcResult result = mvc.perform(
                get("/getAll")
                        .contentType(MediaType.APPLICATION_JSON))

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    public void whenAddOffer_thenAddAndReturnOffer() throws Exception {

//Given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);

        when(offerService.addOffer(offerDTO)).thenReturn(offerDTO);

        String expectedJson = gson.toJson(offerDTO);

//When
        MvcResult result = mvc.perform(
                post("/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("username", TEST_USER_EMAIL)
                        .content(expectedJson))

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    public void whenAddAlreadyExistingOffer_thenReturnBadRequest() throws Exception {

//Given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        doThrow(new OfferException("Offer with given ID already exist!"))
                .when(offerService).addOffer(offerDTO);

        String expectedJson = gson.toJson(offerDTO);

//When
        MvcResult result = mvc.perform(
                post("/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("username", TEST_USER_EMAIL)
                        .content(expectedJson))

//Then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertEquals("Offer with given ID already exist!", result.getResponse().getContentAsString());
    }

    @Test
    public void whenDeleteOffer_thenStatusOk() throws Exception {

//Given
        doNothing().when(offerService).deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_USER_EMAIL);

        String expectedJson = gson.toJson(TEST_DEFAULT_OFFER_ID);

//When
        mvc.perform(
                delete("/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("username", TEST_USER_EMAIL)
                        .content(expectedJson))

//Then
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void whenDeleteNonExistingOffer_thenReturnBadRequest() throws Exception {

//Given
        doThrow(new OfferException("Can't remove non-existing offer!"))
                .when(offerService).deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_USER_EMAIL);

        String expectedJson = gson.toJson(TEST_DEFAULT_OFFER_ID);

//When
        MvcResult result = mvc.perform(
                delete("/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("username", TEST_USER_EMAIL)
                        .content(expectedJson))

//Then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertEquals("Can't remove non-existing offer!", result.getResponse().getContentAsString());
    }


    @Test
    public void whenGetOwnedOffers_thenReturnOwnedOffers() throws Exception {

//Given
        List<OfferDTO> offersDTO = OfferAssembler.getPopulatedOffersDTO();
        when(offerService.getOwnedOffers(TEST_USER_EMAIL)).thenReturn(offersDTO);

        String expectedJson = gson.toJson(offersDTO);

//When
        MvcResult result = mvc.perform(
                get("/getOwned")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("username", TEST_USER_EMAIL)
        )

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    public void whenGetBookedOffers_thenReturnBookedOffers() throws Exception {

//Given
        List<OfferDTO> offersDTO = OfferAssembler.getPopulatedOffersDTO();
        when(offerService.getBookedOffers(TEST_USER_EMAIL)).thenReturn(offersDTO);

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
    public void whenSearchOffer_thenReturnOffersWithinSearchedCriteria() throws Exception {

//Given
        List<OfferDTO> offersDTO = OfferAssembler.getPopulatedOffersDTO();
        when(offerService.searchOffers(TEST_HOTEL_NAME)).thenReturn(offersDTO);

        String expectedJson = gson.toJson(offersDTO);

//When
        MvcResult result = mvc.perform(
                post("/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TEST_HOTEL_NAME)
        )

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    public void whenEditOffer_thenEditAndReturnOffer() throws Exception {

//Given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        when(offerService.editOffer(offerDTO)).thenReturn(offerDTO);

        String expectedJson = gson.toJson(offerDTO);

//When
        MvcResult result = mvc.perform(
                put("/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("username", TEST_USER_EMAIL)
                        .content(expectedJson)
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

        when(offerService.bookOffer(TEST_DEFAULT_OFFER_ID.toString(), dateToBook, TEST_USER_EMAIL)).thenReturn(offerDTO);

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
                .when(offerService).bookOffer(TEST_DEFAULT_OFFER_ID.toString(), dateToBook, TEST_USER_EMAIL);

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
