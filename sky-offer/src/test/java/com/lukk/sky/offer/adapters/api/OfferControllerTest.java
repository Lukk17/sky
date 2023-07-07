package com.lukk.sky.offer.adapters.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lukk.sky.offer.Assemblers.OfferAssembler;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.ports.service.OfferService;
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

import java.util.List;

import static com.lukk.sky.offer.Assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static com.lukk.sky.offer.Assemblers.OfferAssembler.TEST_HOTEL_NAME;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static com.lukk.sky.offer.config.Constants.USER_INFO_HEADERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class OfferControllerTest {

    private Gson gson;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private OfferService offerService;

    @BeforeEach
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
                                .header(USER_INFO_HEADERS.iterator().next(), TEST_USER_EMAIL)
                                .contentType(MediaType.APPLICATION_JSON))

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals("<center><h1>Welcome to Offer app.</h1></center>", result.getResponse().getContentAsString());
    }

    @Test
    public void whenGoHomePage_thenReturnWelcomingMessage() throws Exception {

//When
        MvcResult result = mvc.perform(
                        get("/home")
                                .header(USER_INFO_HEADERS.iterator().next(), TEST_USER_EMAIL)
                                .contentType(MediaType.APPLICATION_JSON))

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals("<center><h1>Welcome to Offer app.</h1></center>", result.getResponse().getContentAsString());
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
                                .header(USER_INFO_HEADERS.iterator().next(), TEST_USER_EMAIL)
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
                                .header(USER_INFO_HEADERS.iterator().next(), TEST_USER_EMAIL)
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
                                .header(USER_INFO_HEADERS.iterator().next(), TEST_USER_EMAIL)
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
                                .header(USER_INFO_HEADERS.iterator().next(), TEST_USER_EMAIL)
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
                                .header(USER_INFO_HEADERS.iterator().next(), TEST_USER_EMAIL)
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
                                .header(USER_INFO_HEADERS.iterator().next(), TEST_USER_EMAIL)
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
                                .header(USER_INFO_HEADERS.iterator().next(), TEST_USER_EMAIL)
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
                                .header(USER_INFO_HEADERS.iterator().next(), TEST_USER_EMAIL)
                                .content(expectedJson)
                )

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }
}
