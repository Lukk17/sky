package com.lukk.sky.offer.adapters.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lukk.sky.offer.Assemblers.OfferAssembler;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.exception.OfferNotFoundException;
import com.lukk.sky.offer.domain.ports.notification.OfferNotificationService;
import com.lukk.sky.offer.domain.ports.service.OfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.InputStream;
import java.util.List;

import static com.lukk.sky.offer.Assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static com.lukk.sky.offer.Assemblers.OfferAssembler.TEST_HOTEL_NAME;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_OWNER_EMAIL;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
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
class OfferApiControllerTest {

    private static final byte[] VALID_JPEG_BYTES = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
    };

    private static final byte[] VALID_PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D
    };

    private static final byte[] INVALID_MAGIC_BYTES = {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0A, 0x0B
    };

    private Gson gson;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OfferService offerService;

    @MockitoBean
    private OfferNotificationService offerNotificationService;

    private final String API_PREFIX;

    OfferApiControllerTest(@Value("${sky.apiPrefix}") String apiPrefix) {
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
    void beforeAll() {
        gson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .create();

        doNothing().when(offerNotificationService).sendMessage(any());
    }

    @Test
    @DisplayName("getAllOffers_whenOffersExist_thenReturnPagedOffers")
    void getAllOffers_whenOffersExist_thenReturnPagedOffers() throws Exception {
        List<OfferDTO> offersDTO = OfferAssembler.getPopulatedOffersDTO();
        Pageable pageable = PageRequest.of(0, 20);
        when(offerService.getAllOffers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(offersDTO, pageable, offersDTO.size()));

        mvc.perform(
                        get("/offers")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].hotelName").value(offersDTO.get(0).getHotelName()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("getOwnedOffers_whenUserHasOffers_thenReturnPagedOwnedOffers")
    void getOwnedOffers_whenUserHasOffers_thenReturnPagedOwnedOffers() throws Exception {
        List<OfferDTO> offersDTO = OfferAssembler.getPopulatedOffersDTO();
        Pageable pageable = PageRequest.of(0, 20);
        when(offerService.getOwnedOffers(eq(TEST_USER_EMAIL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(offersDTO, pageable, offersDTO.size()));

        mvc.perform(
                        get("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].hotelName").value(offersDTO.get(0).getHotelName()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("getOwnedOffers_whenNoJwt_thenReturn401")
    void getOwnedOffers_whenNoJwt_thenReturn401() throws Exception {
        mvc.perform(get("/owner/offers").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    @DisplayName("addOffer_whenValidOffer_thenReturnCreatedOfferDto")
    void addOffer_whenValidOffer_thenReturnCreatedOfferDto() throws Exception {
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);

        when(offerService.addOffer(offerDTO)).thenReturn(offerDTO);

        String expectedJson = gson.toJson(offerDTO);

        MvcResult result = mvc.perform(
                        post("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)))
                                .content(expectedJson))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("addOffer_whenOfferAlreadyExists_thenReturn400WithErrorMessage")
    void addOffer_whenOfferAlreadyExists_thenReturn400WithErrorMessage() throws Exception {
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        doThrow(new OfferException("Offer with given ID already exist!"))
                .when(offerService).addOffer(offerDTO);

        String expectedJson = gson.toJson(offerDTO);

        MvcResult result = mvc.perform(
                        post("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)))
                                .content(expectedJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("Offer with given ID already exist!"));
    }

    @Test
    @DisplayName("addOffer_whenOwnerEmailInvalid_thenReturn400WithValidationMessage")
    void addOffer_whenOwnerEmailInvalid_thenReturn400WithValidationMessage() throws Exception {
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        offerDTO.setOwnerEmail(" ");

        String expectedJson = gson.toJson(offerDTO);

        MvcResult result = mvc.perform(
                        post("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)))
                                .content(expectedJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("ownerEmail"));
        assertTrue(body.contains("field-errors"));
    }

    @Test
    @DisplayName("editOffer_whenValidEdit_thenReturnUpdatedOfferDto")
    void editOffer_whenValidEdit_thenReturnUpdatedOfferDto() throws Exception {
        OfferEditDTO offerEditDTO = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        when(offerService.editOffer(offerEditDTO)).thenReturn(offerDTO);

        String requestJson = gson.toJson(offerEditDTO);
        String expectedResponseJson = gson.toJson(offerDTO);

        MvcResult result = mvc.perform(
                        put("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)))
                                .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedResponseJson, result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("editOffer_whenNoJwt_thenReturn401")
    void editOffer_whenNoJwt_thenReturn401() throws Exception {
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        String expectedJson = gson.toJson(offerDTO);

        mvc.perform(put("/owner/offers").contentType(MediaType.APPLICATION_JSON).content(expectedJson))
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    @DisplayName("deleteOffer_whenValidRequest_thenReturn200")
    void deleteOffer_whenValidRequest_thenReturn200() throws Exception {
        doNothing().when(offerService).deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_USER_EMAIL);

        mvc.perform(
                        delete(String.format("/owner/offers/%s", TEST_DEFAULT_OFFER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)))
                )
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("deleteOffer_whenOfferDoesNotExist_thenReturn404WithErrorMessage")
    void deleteOffer_whenOfferDoesNotExist_thenReturn404WithErrorMessage() throws Exception {
        doThrow(new OfferNotFoundException("Can't remove non-existing offer!"))
                .when(offerService).deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_USER_EMAIL);

        MvcResult result = mvc.perform(
                        delete(String.format("/owner/offers/%s", TEST_DEFAULT_OFFER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)))
                )
                .andExpect(status().isNotFound())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("Can't remove non-existing offer!"));
    }

    @Test
    @DisplayName("deleteOffer_whenOfferIdIsNotNumeric_thenReturn400")
    void deleteOffer_whenOfferIdIsNotNumeric_thenReturn400() throws Exception {
        mvc.perform(
                        delete("/owner/offers/not-a-number")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("search_whenTermMatches_thenReturnPagedMatchingOffers")
    void search_whenTermMatches_thenReturnPagedMatchingOffers() throws Exception {
        List<OfferDTO> offersDTO = OfferAssembler.getPopulatedOffersDTO();
        Pageable pageable = PageRequest.of(0, 20);
        when(offerService.searchOffers(eq(TEST_HOTEL_NAME), any(Pageable.class)))
                .thenReturn(new PageImpl<>(offersDTO, pageable, offersDTO.size()));

        mvc.perform(
                        post("/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(TEST_HOTEL_NAME)
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("search_whenTermIsBlank_thenReturn400")
    void search_whenTermIsBlank_thenReturn400() throws Exception {
        mvc.perform(
                        post("/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("   ")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("search_whenTermExceeds100Chars_thenReturn400")
    void search_whenTermExceeds100Chars_thenReturn400() throws Exception {
        String tooLong = "a".repeat(101);

        MvcResult result = mvc.perform(
                        post("/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(tooLong)
                )
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("100"));
    }

    @Test
    @DisplayName("uploadPhoto_whenValidPng_thenReturnUpdatedOfferDtoWithPhotoUrl")
    void uploadPhoto_whenValidPng_thenReturnUpdatedOfferDtoWithPhotoUrl() throws Exception {
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        offerDTO.setPhotoPath("offers/test-uuid-hotel.png");
        offerDTO.setPhotoUrl("http://localhost:9000/sky-offers-test/offers/test-uuid-hotel.png?X-Amz-Signature=sig");

        when(offerService.uploadPhoto(
                eq(TEST_DEFAULT_OFFER_ID),
                eq(TEST_OWNER_EMAIL),
                any(InputStream.class),
                anyLong(),
                eq("image/png"),
                eq("hotel.png")
        )).thenReturn(offerDTO);

        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.png", "image/png", VALID_PNG_BYTES
        );

        mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.photoPath").value("offers/test-uuid-hotel.png"))
                .andExpect(jsonPath("$.photoUrl").value("http://localhost:9000/sky-offers-test/offers/test-uuid-hotel.png?X-Amz-Signature=sig"));
    }

    @Test
    @DisplayName("uploadPhoto_whenFileIsEmpty_thenReturn400")
    void uploadPhoto_whenFileIsEmpty_thenReturn400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.jpg", "image/jpeg", new byte[0]
        );

        MvcResult result = mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)))
                )
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("empty"));
    }

    @Test
    @DisplayName("uploadPhoto_whenMagicBytesDoNotMatchDeclaredType_thenReturn400")
    void uploadPhoto_whenMagicBytesDoNotMatchDeclaredType_thenReturn400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.jpg", "image/jpeg", INVALID_MAGIC_BYTES
        );

        MvcResult result = mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)))
                )
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("Unsupported image format"));
    }

    @Test
    @DisplayName("uploadPhoto_whenDisallowedFileType_thenReturn400")
    void uploadPhoto_whenDisallowedFileType_thenReturn400() throws Exception {
        byte[] pdfBytes = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34, 0x0A, 0x25, (byte) 0xC3, (byte) 0xA4};
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", pdfBytes
        );

        MvcResult result = mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)))
                )
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("Unsupported image format"));
    }

    @Test
    @DisplayName("uploadPhoto_whenNoJwt_thenReturn401")
    void uploadPhoto_whenNoJwt_thenReturn401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.jpg", "image/jpeg", VALID_JPEG_BYTES
        );

        mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("uploadPhoto_whenNotOwner_thenReturn400")
    void uploadPhoto_whenNotOwner_thenReturn400() throws Exception {
        doThrow(new OfferException("You can only upload photos for your own offers."))
                .when(offerService).uploadPhoto(
                        eq(TEST_DEFAULT_OFFER_ID),
                        eq(TEST_OWNER_EMAIL),
                        any(InputStream.class),
                        anyLong(),
                        anyString(),
                        any()
                );

        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.jpg", "image/jpeg", VALID_JPEG_BYTES
        );

        MvcResult result = mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)))
                )
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString()
                .contains("You can only upload photos for your own offers."));
    }
}
