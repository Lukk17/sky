package com.lukk.sky.offer.adapters.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lukk.sky.offer.Assemblers.OfferAssembler;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.ports.notification.OfferNotificationService;
import com.lukk.sky.offer.domain.ports.service.OfferService;
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
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static com.lukk.sky.offer.Assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static com.lukk.sky.offer.Assemblers.OfferAssembler.TEST_HOTEL_NAME;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_OWNER_EMAIL;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("OfferApiController — MockMvc tests for the public and owner offer API endpoints")
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"offerTopic-1"})
@Import({com.lukk.sky.offer.TestSecurityConfig.class, com.lukk.sky.offer.TestcontainersConfiguration.class, com.lukk.sky.offer.TestS3Config.class})
public class OfferApiControllerTest {

    private Gson gson;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OfferService offerService;

    @MockitoBean
    private OfferNotificationService offerNotificationService;

    private final String API_PREFIX;

    public OfferApiControllerTest(@Value("${sky.apiPrefix}") String apiPrefix) {
        this.API_PREFIX = apiPrefix;
    }

    private MockHttpServletRequestBuilder get(String uri) {
        return MockMvcRequestBuilders.get("/" + API_PREFIX + uri);
    }

    private MockHttpServletRequestBuilder post(String uri) {
        return MockMvcRequestBuilders.post("/" + API_PREFIX + uri);
    }

    private MockHttpServletRequestBuilder put(String uri) {
        return MockMvcRequestBuilders.put("/" + API_PREFIX + uri);
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

        doNothing().when(offerNotificationService).sendMessage(any());
    }

    @Test
    @DisplayName("GET /offers returns paged offers with content and totalElements when offers exist (no JWT required)")
    public void getAllOffers_whenOffersExist_thenReturnPagedOffers() throws Exception {
//Given
        List<OfferDTO> offersDTO = OfferAssembler.getPopulatedOffersDTO();
        Pageable pageable = PageRequest.of(0, 20);
        when(offerService.getAllOffers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(offersDTO, pageable, offersDTO.size()));
//When
        mvc.perform(
                        get("/offers")
                                .contentType(MediaType.APPLICATION_JSON))
//Then
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].hotelName").value(offersDTO.get(0).getHotelName()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /owner/offers returns paged offers owned by the authenticated user")
    public void getOwnedOffers_whenUserHasOffers_thenReturnPagedOwnedOffers() throws Exception {
//Given
        List<OfferDTO> offersDTO = OfferAssembler.getPopulatedOffersDTO();
        Pageable pageable = PageRequest.of(0, 20);
        when(offerService.getOwnedOffers(eq(TEST_USER_EMAIL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(offersDTO, pageable, offersDTO.size()));
//When
        mvc.perform(
                        get("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)))
                )
//Then
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].hotelName").value(offersDTO.get(0).getHotelName()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /owner/offers without JWT returns 401 Unauthorized")
    public void getOwnedOffers_whenNoJwt_thenReturn401() throws Exception {
//When
        mvc.perform(get("/owner/offers").contentType(MediaType.APPLICATION_JSON))
//Then
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    @DisplayName("POST /owner/offers with a valid offer body creates the offer and returns it")
    public void addOffer_whenValidOffer_thenReturnCreatedOfferDto() throws Exception {
//Given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);

        when(offerService.addOffer(offerDTO)).thenReturn(offerDTO);

        String expectedJson = gson.toJson(offerDTO);
//When
        MvcResult result = mvc.perform(
                        post("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)))
                                .content(expectedJson))
//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("POST /owner/offers when service throws OfferException returns 400 with error message")
    public void addOffer_whenOfferAlreadyExists_thenReturn400WithErrorMessage() throws Exception {
//Given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        doThrow(new OfferException("Offer with given ID already exist!"))
                .when(offerService).addOffer(offerDTO);

        String expectedJson = gson.toJson(offerDTO);
//When
        MvcResult result = mvc.perform(
                        post("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)))
                                .content(expectedJson))
//Then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("Offer with given ID already exist!"));
    }

    @Test
    @DisplayName("POST /owner/offers with a blank ownerEmail returns 400 with a validation message")
    public void addOffer_whenOwnerEmailInvalid_thenReturn400WithValidationMessage() throws Exception {
//Given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        offerDTO.setOwnerEmail(" ");

        String expectedJson = gson.toJson(offerDTO);
//When
        MvcResult result = mvc.perform(
                        post("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)))
                                .content(expectedJson))
//Then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString()
                .contains("Field 'ownerEmail' must be a well-formed email address"));
    }

    @Test
    @DisplayName("PUT /owner/offers with a valid edit body updates the offer and returns it")
    public void editOffer_whenValidEdit_thenReturnUpdatedOfferDto() throws Exception {
//Given
        OfferEditDTO offerEditDTO = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        when(offerService.editOffer(offerEditDTO)).thenReturn(offerDTO);

        String requestJson = gson.toJson(offerEditDTO);
        String expectedResponseJson = gson.toJson(offerDTO);
//When
        MvcResult result = mvc.perform(
                        put("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)))
                                .content(requestJson)
                )
//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedResponseJson, result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("PUT /owner/offers without JWT returns 401 Unauthorized")
    public void editOffer_whenNoJwt_thenReturn401() throws Exception {
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        String expectedJson = gson.toJson(offerDTO);
//When
        mvc.perform(put("/owner/offers").contentType(MediaType.APPLICATION_JSON).content(expectedJson))
//Then
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    @DisplayName("DELETE /owner/offers/{id} with a valid request returns 200")
    public void deleteOffer_whenValidRequest_thenReturn200() throws Exception {
//Given
        doNothing().when(offerService).deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_USER_EMAIL);
//When
        mvc.perform(
                        delete(String.format("/owner/offers/%s", TEST_DEFAULT_OFFER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)))
                )
//Then
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("DELETE /owner/offers/{id} when service throws OfferException returns 400 with error message")
    public void deleteOffer_whenOfferDoesNotExist_thenReturn400WithErrorMessage() throws Exception {
//Given
        doThrow(new OfferException("Can't remove non-existing offer!"))
                .when(offerService).deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_USER_EMAIL);
//When
        MvcResult result = mvc.perform(
                        delete(String.format("/owner/offers/%s", TEST_DEFAULT_OFFER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)))
                )
//Then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("Can't remove non-existing offer!"));
    }

    @Test
    @DisplayName("POST /search with a matching term returns offers that satisfy the search criteria (no JWT required)")
    public void searchOffers_whenTermMatches_thenReturnMatchingOffers() throws Exception {
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
    @DisplayName("POST /owner/offers/{id}/photo with valid file uploads photo and returns updated OfferDTO with photoUrl")
    public void uploadPhoto_whenValidFile_thenReturnUpdatedOfferDtoWithPhotoUrl() throws Exception {
//Given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        offerDTO.setPhotoPath("offers/test-uuid-hotel.jpg");
        offerDTO.setPhotoUrl("http://localhost:9000/sky-offers-test/offers/test-uuid-hotel.jpg?X-Amz-Signature=sig");

        when(offerService.uploadPhoto(
                eq(TEST_DEFAULT_OFFER_ID),
                eq(TEST_OWNER_EMAIL),
                any(byte[].class),
                eq("image/jpeg"),
                eq("hotel.jpg")
        )).thenReturn(offerDTO);

        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.jpg", "image/jpeg", "fake-image-bytes".getBytes()
        );
//When
        mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)))
                )
//Then
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.photoPath").value("offers/test-uuid-hotel.jpg"))
                .andExpect(jsonPath("$.photoUrl").value("http://localhost:9000/sky-offers-test/offers/test-uuid-hotel.jpg?X-Amz-Signature=sig"));
    }

    @Test
    @DisplayName("POST /owner/offers/{id}/photo without JWT returns 401 Unauthorized")
    public void uploadPhoto_whenNoJwt_thenReturn401() throws Exception {
//Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.jpg", "image/jpeg", "fake-image-bytes".getBytes()
        );
//When
        mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                )
//Then
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /owner/offers/{id}/photo when service throws OfferException returns 400")
    public void uploadPhoto_whenNotOwner_thenReturn400() throws Exception {
//Given
        doThrow(new OfferException("You can only upload photos for your own offers."))
                .when(offerService).uploadPhoto(
                        eq(TEST_DEFAULT_OFFER_ID),
                        eq(TEST_OWNER_EMAIL),
                        any(byte[].class),
                        any(),
                        any()
                );

        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.jpg", "image/jpeg", "fake-image-bytes".getBytes()
        );
//When
        MvcResult result = mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)))
                )
//Then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString()
                .contains("You can only upload photos for your own offers."));
    }
}
