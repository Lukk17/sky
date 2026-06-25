package com.lukk.sky.offer.adapters.api;

import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.exception.OfferNotFoundException;
import com.lukk.sky.offer.domain.ports.service.OfferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.springframework.context.annotation.Import;

import static com.lukk.sky.offer.Assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_OWNER_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("OfferInternalController — MockMvc tests for the internal offer-owner lookup endpoint")
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"offerTopic-1"})
@Import({com.lukk.sky.offer.TestSecurityConfig.class, com.lukk.sky.offer.TestcontainersConfiguration.class})
class OfferInternalControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OfferService offerService;

    @Test
    @DisplayName("GET /api/internal/v1/owner/offer/{id} returns the owner email when the offer exists")
    public void getOfferOwner_whenOfferExists_thenReturnOwnerEmail() throws Exception {
//Given
        when(offerService.findOfferOwner(TEST_DEFAULT_OFFER_ID))
                .thenReturn(TEST_OWNER_EMAIL);
//When
        MvcResult result = mvc.perform(
                        get("/api/internal/v1/owner/offer/{offerId}", TEST_DEFAULT_OFFER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL))))
//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(TEST_OWNER_EMAIL, result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("GET /api/internal/v1/owner/offer/{id} returns 404 with error message when offer does not exist")
    public void getOfferOwner_whenOfferDoesNotExist_thenReturn404WithErrorMessage() throws Exception {
//Given
        String expectedErrorMessage = "Offer is not existing.";
        when(offerService.findOfferOwner(TEST_DEFAULT_OFFER_ID))
                .thenThrow(new OfferNotFoundException(expectedErrorMessage));
//When
        MvcResult result = mvc.perform(
                        get("/api/internal/v1/owner/offer/{offerId}", TEST_DEFAULT_OFFER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL))))
//Then
                .andExpect(status().isNotFound())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains(expectedErrorMessage));
    }

    @Test
    @DisplayName("GET /api/internal/v1/owner/offer/{id} returns 400 when offerId is not a valid number")
    public void getOfferOwner_whenOfferIdIsNotNumeric_thenReturn400() throws Exception {
//When
        mvc.perform(
                        get("/api/internal/v1/owner/offer/{offerId}", "not-a-number")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL))))
//Then
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/internal/v1/owner/offer/{id} returns 401 Unauthorized when no JWT is supplied")
    public void getOfferOwner_whenNoJwt_thenReturn401() throws Exception {
//When
        mvc.perform(
                        get("/api/internal/v1/owner/offer/{offerId}", TEST_DEFAULT_OFFER_ID)
                                .contentType(MediaType.APPLICATION_JSON))
//Then
                .andExpect(status().isUnauthorized());
    }
}
