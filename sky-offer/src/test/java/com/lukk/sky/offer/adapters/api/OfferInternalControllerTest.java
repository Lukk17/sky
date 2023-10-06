package com.lukk.sky.offer.adapters.api;

import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.ports.service.OfferService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.lukk.sky.offer.Assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_OWNER_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class OfferInternalControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private OfferService offerService;

    @Test
    public void whenGetOfferOwner_thenReturnOwnerEmail() throws Exception {
//Given
        when(offerService.findOfferOwner(TEST_DEFAULT_OFFER_ID.toString()))
                .thenReturn(TEST_OWNER_EMAIL);
//When
        MvcResult result = mvc.perform(
                        get("/owner/offer/{offerId}", TEST_DEFAULT_OFFER_ID)
                                .contentType(MediaType.APPLICATION_JSON))
//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(TEST_OWNER_EMAIL, result.getResponse().getContentAsString());
    }

    @Test
    public void whenGetNonExistingOfferOwner_thenReturnError() throws Exception {
//Given
        String expectedErrorMessage = "Offer is not existing.";
        when(offerService.findOfferOwner(TEST_DEFAULT_OFFER_ID.toString()))
                .thenThrow(new OfferException(expectedErrorMessage));
//When
        MvcResult result = mvc.perform(
                        get("/owner/offer/{offerId}", TEST_DEFAULT_OFFER_ID)
                                .contentType(MediaType.APPLICATION_JSON))
//Then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains(expectedErrorMessage));
    }
}
