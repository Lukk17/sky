package com.lukk.sky.offer.adapters.inbound.api;

import com.lukk.sky.offer.assemblers.OfferAssembler;
import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.exception.EventSequenceConflictException;
import com.lukk.sky.offer.domain.exception.OfferAccessDeniedException;
import com.lukk.sky.offer.domain.exception.OfferNotFoundException;
import com.lukk.sky.offer.domain.exception.PhotoStorageBadResponseException;
import com.lukk.sky.offer.domain.exception.PhotoStorageUnavailableException;
import com.lukk.sky.offer.domain.ports.inbound.CreateOfferCommand;
import com.lukk.sky.offer.domain.ports.inbound.OfferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_HOTEL_NAME;
import static com.lukk.sky.common.test.TestUsers.TEST_OWNER_EMAIL;
import static com.lukk.sky.common.test.TestUsers.TEST_USER_EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("OfferApiController: MockMvc tests for the public and owner offer API endpoints")
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

    private static final byte[] VALID_WEBP_BYTES = {
            0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00,
            0x57, 0x45, 0x42, 0x50, 0x56, 0x50, 0x38, 0x20
    };

    private static final byte[] RIFF_WAVE_BYTES = {
            0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00,
            0x57, 0x41, 0x56, 0x45, 0x66, 0x6D, 0x74, 0x20
    };

    private static final byte[] INVALID_MAGIC_BYTES = {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0A, 0x0B
    };

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OfferService offerService;

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

    @Test
    @DisplayName("getAllOffers_whenOffersExist_thenReturnPagedOffers")
    void getAllOffers_whenOffersExist_thenReturnPagedOffers() throws Exception {
        // given
        List<OfferDTO> offersDTO = OfferAssembler.getPopulatedOffersDTO();
        Pageable pageable = PageRequest.of(0, 20);
        when(offerService.getAllOffers(any(Pageable.class)))
                .thenReturn(new PageImpl<>(OfferAssembler.toOfferViews(offersDTO), pageable, offersDTO.size()));

        // when / then
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
        // given
        List<OfferDTO> offersDTO = OfferAssembler.getPopulatedOffersDTO();
        Pageable pageable = PageRequest.of(0, 20);
        when(offerService.getOwnedOffers(eq(TEST_USER_EMAIL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(OfferAssembler.toOfferViews(offersDTO), pageable, offersDTO.size()));

        // when / then
        mvc.perform(
                        get("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].hotelName").value(offersDTO.get(0).getHotelName()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("getOwnedOffers_whenJwtHasNoRole_thenReturn403")
    void getOwnedOffers_whenJwtHasNoRole_thenReturn403() throws Exception {
        mvc.perform(
                        get("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)))
                )
                .andExpect(status().isForbidden());
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
        // given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        when(offerService.addOffer(any())).thenReturn(OfferAssembler.toOfferView(offerDTO));
        String expectedJson = objectMapper.writeValueAsString(offerDTO);

        // when
        MvcResult result = mvc.perform(
                        post("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                                .content(expectedJson))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // then
        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("addOffer_whenOwnerEmailIsGarbage_thenIgnoreItBecauseTheServerAssignsItFromTheJwt")
    void addOffer_whenOwnerEmailIsGarbage_thenIgnoreIt() throws Exception {
        // given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        offerDTO.setOwnerEmail("not-an-email");
        when(offerService.addOffer(any())).thenReturn(OfferAssembler.toOfferView(offerDTO));
        String payload = objectMapper.writeValueAsString(offerDTO);

        // when / then
        mvc.perform(
                        post("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                                .content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("addOffer_whenTheEventSequenceConflicts_thenReturn409TellingTheClientToReRead")
    void addOffer_whenTheEventSequenceConflicts_thenReturn409TellingTheClientToReRead() throws Exception {
        // given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        when(offerService.addOffer(any()))
                .thenThrow(new EventSequenceConflictException(
                        "Gave up appending a CREATED event for offer " + TEST_DEFAULT_OFFER_ID + " after 20 attempts",
                        new IllegalStateException("duplicate key")));
        String payload = objectMapper.writeValueAsString(offerDTO);

        // when / then
        mvc.perform(
                        post("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                                .content(payload))
                .andExpect(status().isConflict())
                .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "Request conflicts with current state."));
    }

    @Test
    @DisplayName("addOffer_whenHotelNameExceeds255Chars_thenReturn400WithFieldError")
    void addOffer_whenHotelNameExceeds255Chars_thenReturn400() throws Exception {
        // given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        offerDTO.setHotelName("a".repeat(256));
        String payload = objectMapper.writeValueAsString(offerDTO);

        // when / then
        mvc.perform(
                        post("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.['field-errors'].hotelName").exists());
    }

    @Test
    @DisplayName("editOffer_whenHotelNameIsBlank_thenReturn400WithFieldError")
    void editOffer_whenHotelNameIsBlank_thenReturn400() throws Exception {
        // given
        OfferEditDTO offerEditDTO = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        offerEditDTO.setHotelName("   ");
        String payload = objectMapper.writeValueAsString(offerEditDTO);

        // when / then
        mvc.perform(
                        put("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.['field-errors'].hotelName").exists());
    }

    @Test
    @DisplayName("editOffer_whenHotelNameIsAbsent_thenAcceptItAsAPartialUpdate")
    void editOffer_whenHotelNameIsAbsent_thenAcceptIt() throws Exception {
        // given
        OfferEditDTO offerEditDTO = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        offerEditDTO.setHotelName(null);
        when(offerService.editOffer(any(), eq(TEST_OWNER_EMAIL)))
                .thenReturn(OfferAssembler.toOfferView(OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID)));
        String payload = objectMapper.writeValueAsString(offerEditDTO);

        // when / then
        mvc.perform(
                        put("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                                .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("editOffer_whenCallerIsNotOwner_thenReturn403")
    void editOffer_whenCallerIsNotOwner_thenReturn403() throws Exception {
        // given
        OfferEditDTO offerEditDTO = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        doThrow(new OfferAccessDeniedException("You can only edit offers you own."))
                .when(offerService).editOffer(any(), eq(TEST_USER_EMAIL));
        String payload = objectMapper.writeValueAsString(offerEditDTO);

        // when / then
        mvc.perform(
                        put("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                                .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("deleteOffer_whenCallerIsNotOwner_thenReturn403")
    void deleteOffer_whenCallerIsNotOwner_thenReturn403() throws Exception {
        // given
        doThrow(new OfferAccessDeniedException("You can only delete offers you own."))
                .when(offerService).deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_USER_EMAIL);

        // when / then
        mvc.perform(
                        delete(String.format("/owner/offers/%s", TEST_DEFAULT_OFFER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("editOffer_whenValidEdit_thenReturnUpdatedOfferDto")
    void editOffer_whenValidEdit_thenReturnUpdatedOfferDto() throws Exception {
        // given
        OfferEditDTO offerEditDTO = OfferAssembler.getPopulatedOfferEditDTO(TEST_DEFAULT_OFFER_ID);
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        when(offerService.editOffer(eq(OfferAssembler.toEditCommand(offerEditDTO)), eq(TEST_OWNER_EMAIL)))
                .thenReturn(OfferAssembler.toOfferView(offerDTO));
        String requestJson = objectMapper.writeValueAsString(offerEditDTO);
        String expectedResponseJson = objectMapper.writeValueAsString(offerDTO);

        // when
        MvcResult result = mvc.perform(
                        put("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                                .content(requestJson)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // then
        assertEquals(expectedResponseJson, result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("editOffer_whenNoJwt_thenReturn401")
    void editOffer_whenNoJwt_thenReturn401() throws Exception {
        // given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        String expectedJson = objectMapper.writeValueAsString(offerDTO);

        // when / then
        mvc.perform(put("/owner/offers").contentType(MediaType.APPLICATION_JSON).content(expectedJson))
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    @DisplayName("deleteOffer_whenValidRequest_thenReturn200")
    void deleteOffer_whenValidRequest_thenReturn200() throws Exception {
        // given
        doNothing().when(offerService).deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_USER_EMAIL);

        // when / then
        mvc.perform(
                        delete(String.format("/owner/offers/%s", TEST_DEFAULT_OFFER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("deleteOffer_whenOfferDoesNotExist_thenReturn404WithErrorMessage")
    void deleteOffer_whenOfferDoesNotExist_thenReturn404WithErrorMessage() throws Exception {
        // given
        doThrow(new OfferNotFoundException("Can't remove non-existing offer!"))
                .when(offerService).deleteOffer(TEST_DEFAULT_OFFER_ID, TEST_USER_EMAIL);

        // when
        MvcResult result = mvc.perform(
                        delete(String.format("/owner/offers/%s", TEST_DEFAULT_OFFER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isNotFound())
                .andReturn();

        // then
        assertTrue(result.getResponse().getContentAsString().contains("Resource not found."));
    }

    @Test
    @DisplayName("deleteOffer_whenOfferIdIsNotNumeric_thenReturn400")
    void deleteOffer_whenOfferIdIsNotNumeric_thenReturn400() throws Exception {
        mvc.perform(
                        delete("/owner/offers/not-a-number")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("search_whenTermMatches_thenReturnPagedMatchingOffers")
    void search_whenTermMatches_thenReturnPagedMatchingOffers() throws Exception {
        // given
        List<OfferDTO> offersDTO = OfferAssembler.getPopulatedOffersDTO();
        Pageable pageable = PageRequest.of(0, 20);
        when(offerService.searchOffers(eq(TEST_HOTEL_NAME), any(Pageable.class)))
                .thenReturn(new PageImpl<>(OfferAssembler.toOfferViews(offersDTO), pageable, offersDTO.size()));

        // when / then
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
        // given
        String tooLong = "a".repeat(101);

        // when
        MvcResult result = mvc.perform(
                        post("/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(tooLong)
                )
                .andExpect(status().isBadRequest())
                .andReturn();

        // then
        assertTrue(result.getResponse().getContentAsString().contains("Invalid request."));
    }

    @Test
    @DisplayName("getOfferOwner_whenOfferExists_thenReturnOwnerEmail")
    void getOfferOwner_whenOfferExists_thenReturnOwnerEmail() throws Exception {
        // given
        when(offerService.findOfferOwner(TEST_DEFAULT_OFFER_ID))
                .thenReturn(TEST_OWNER_EMAIL);

        // when
        MvcResult result = mvc.perform(
                        get(String.format("/offers/%s/owner", TEST_DEFAULT_OFFER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // then
        assertEquals(TEST_OWNER_EMAIL, result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("getOfferOwner_whenJwtHasNoUserRole_thenReturn403")
    void getOfferOwner_whenJwtHasNoUserRole_thenReturn403() throws Exception {
        mvc.perform(
                        get(String.format("/offers/%s/owner", TEST_DEFAULT_OFFER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getOfferOwner_whenOfferDoesNotExist_thenReturn404WithErrorMessage")
    void getOfferOwner_whenOfferDoesNotExist_thenReturn404WithErrorMessage() throws Exception {
        // given
        String expectedErrorMessage = "Resource not found.";
        when(offerService.findOfferOwner(TEST_DEFAULT_OFFER_ID))
                .thenThrow(new OfferNotFoundException(expectedErrorMessage));

        // when
        MvcResult result = mvc.perform(
                        get(String.format("/offers/%s/owner", TEST_DEFAULT_OFFER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound())
                .andReturn();

        // then
        assertTrue(result.getResponse().getContentAsString().contains(expectedErrorMessage));
    }

    @Test
    @DisplayName("getOfferOwner_whenNoJwt_thenReturn401")
    void getOfferOwner_whenNoJwt_thenReturn401() throws Exception {
        mvc.perform(
                        get(String.format("/offers/%s/owner", TEST_DEFAULT_OFFER_ID))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("getOfferOwner_whenOfferIdIsNotNumeric_thenReturn400")
    void getOfferOwner_whenOfferIdIsNotNumeric_thenReturn400() throws Exception {
        mvc.perform(
                        get("/offers/not-a-number/owner")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("uploadPhoto_whenValidPng_thenReturnUpdatedOfferDtoWithPhotoUrl")
    void uploadPhoto_whenValidPng_thenReturnUpdatedOfferDtoWithPhotoUrl() throws Exception {
        // given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        offerDTO.setPhotoUrl("http://localhost:9000/sky-offers-test/offers/test-uuid-hotel.png?X-Amz-Signature=sig");
        when(offerService.uploadPhoto(
                eq(TEST_DEFAULT_OFFER_ID),
                eq(TEST_OWNER_EMAIL),
                any(InputStream.class),
                anyLong(),
                eq("image/png"),
                eq("hotel.png")
        )).thenReturn(OfferAssembler.toOfferView(offerDTO));
        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.png", "image/png", VALID_PNG_BYTES
        );

        // when / then
        mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.photoObjectKey").doesNotExist())
                .andExpect(jsonPath("$.photoUrl").value("http://localhost:9000/sky-offers-test/offers/test-uuid-hotel.png?X-Amz-Signature=sig"));
    }

    @Test
    @DisplayName("uploadPhoto_whenValidWebP_thenReturnUpdatedOfferDtoWithPhotoUrl")
    void uploadPhoto_whenValidWebP_thenReturnUpdatedOfferDtoWithPhotoUrl() throws Exception {
        // given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        offerDTO.setPhotoUrl("http://localhost:9000/sky-offers-test/offers/test-uuid-hotel.webp?X-Amz-Signature=sig");
        when(offerService.uploadPhoto(
                eq(TEST_DEFAULT_OFFER_ID),
                eq(TEST_OWNER_EMAIL),
                any(InputStream.class),
                anyLong(),
                eq("image/webp"),
                eq("hotel.webp")
        )).thenReturn(OfferAssembler.toOfferView(offerDTO));
        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.webp", "image/webp", VALID_WEBP_BYTES
        );

        // when / then
        mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.photoUrl")
                        .value("http://localhost:9000/sky-offers-test/offers/test-uuid-hotel.webp?X-Amz-Signature=sig"));
    }

    @Test
    @DisplayName("uploadPhoto_whenFileIsTooSmallToSniff_thenReturn400")
    void uploadPhoto_whenFileIsTooSmallToSniff_thenReturn400() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8}
        );

        // when
        MvcResult result = mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isBadRequest())
                .andReturn();

        // then
        assertTrue(result.getResponse().getContentAsString().contains("Invalid request."));
    }

    @Test
    @DisplayName("uploadPhoto_whenFileExceeds5Mb_thenReturn413")
    void uploadPhoto_whenFileExceeds5Mb_thenReturn413() throws Exception {
        // given
        byte[] header = VALID_PNG_BYTES;
        byte[] big = new byte[(5 * 1024 * 1024) + 1];
        System.arraycopy(header, 0, big, 0, header.length);
        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.png", "image/png", big
        );

        // when
        MvcResult result = mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isPayloadTooLarge())
                .andReturn();

        // then
        assertTrue(result.getResponse().getContentAsString().contains("Request payload too large."));
    }

    @Test
    @DisplayName("uploadGalleryPhoto_whenFileExceeds5Mb_thenReturn413")
    void uploadGalleryPhoto_whenFileExceeds5Mb_thenReturn413() throws Exception {
        // given
        byte[] header = VALID_PNG_BYTES;
        byte[] big = new byte[(5 * 1024 * 1024) + 1];
        System.arraycopy(header, 0, big, 0, header.length);
        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.png", "image/png", big
        );

        // when
        MvcResult result = mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photos")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isPayloadTooLarge())
                .andReturn();

        // then
        assertTrue(result.getResponse().getContentAsString().contains("Request payload too large."));
    }

    @Test
    @DisplayName("uploadPhoto_whenFileIsEmpty_thenReturn400")
    void uploadPhoto_whenFileIsEmpty_thenReturn400() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.jpg", "image/jpeg", new byte[0]
        );

        // when
        MvcResult result = mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isBadRequest())
                .andReturn();

        // then
        assertTrue(result.getResponse().getContentAsString().contains("Invalid request."));
    }

    @Test
    @DisplayName("uploadPhoto_whenMagicBytesDoNotMatchDeclaredType_thenReturn400")
    void uploadPhoto_whenMagicBytesDoNotMatchDeclaredType_thenReturn400() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.jpg", "image/jpeg", INVALID_MAGIC_BYTES
        );

        // when
        MvcResult result = mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isBadRequest())
                .andReturn();

        // then
        assertTrue(result.getResponse().getContentAsString().contains("Invalid request."));
    }

    @Test
    @DisplayName("uploadPhoto_whenDisallowedFileType_thenReturn400")
    void uploadPhoto_whenDisallowedFileType_thenReturn400() throws Exception {
        // given
        byte[] pdfBytes = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34, 0x0A, 0x25, (byte) 0xC3, (byte) 0xA4};
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", pdfBytes
        );

        // when
        MvcResult result = mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isBadRequest())
                .andReturn();

        // then
        assertTrue(result.getResponse().getContentAsString().contains("Invalid request."));
    }

    @Test
    @DisplayName("uploadPhoto_whenFileIsARiffContainerButNotWebP_thenReturn400AndNeverReachTheService")
    void uploadPhoto_whenFileIsARiffContainerButNotWebP_thenReturn400() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.webp", "image/webp", RIFF_WAVE_BYTES
        );

        // when
        MvcResult result = mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isBadRequest())
                .andReturn();

        // then
        assertTrue(result.getResponse().getContentAsString().contains("Invalid request."));
        verifyNoInteractions(offerService);
    }

    @Test
    @DisplayName("uploadPhoto_whenNoJwt_thenReturn401")
    void uploadPhoto_whenNoJwt_thenReturn401() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.jpg", "image/jpeg", VALID_JPEG_BYTES
        );

        // when / then
        mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("uploadPhoto_whenNotOwner_thenReturn403")
    void uploadPhoto_whenNotOwner_thenReturn403() throws Exception {
        // given
        doThrow(new OfferAccessDeniedException("You can only upload photos for your own offers."))
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

        // when
        MvcResult result = mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isForbidden())
                .andReturn();

        // then
        assertTrue(result.getResponse().getContentAsString()
                .contains("Access denied."));
    }

    @Test
    @DisplayName("uploadPhoto_whenTheObjectStoreIsUnavailable_thenReturn503WithRetryAfter")
    void uploadPhoto_whenTheObjectStoreIsUnavailable_thenReturn503WithRetryAfter() throws Exception {
        // given
        doThrow(new PhotoStorageUnavailableException(
                "Photo upload failed. The object store is unavailable.",
                new IllegalStateException("Connection refused")))
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

        // when / then
        mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "10"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.title").value("Service Unavailable"))
                .andExpect(jsonPath("$.detail").value("Service temporarily unavailable, please retry."));
    }

    @Test
    @DisplayName("uploadPhoto_whenTheObjectStoreRefusesTheRequest_thenReturn502WithoutRetryAfter")
    void uploadPhoto_whenTheObjectStoreRefusesTheRequest_thenReturn502WithoutRetryAfter() throws Exception {
        // given
        doThrow(new PhotoStorageBadResponseException(
                "Photo upload failed. The object store refused the request.",
                new IllegalStateException("Access Denied")))
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

        // when / then
        mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photo")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isBadGateway())
                .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.title").value("Bad Gateway"))
                .andExpect(jsonPath("$.detail").value("Upstream service failed."));
    }

    @Test
    @DisplayName("getAllOffers_whenThePhotoAddressCannotBeSigned_thenReturn503WithRetryAfter")
    void getAllOffers_whenThePhotoAddressCannotBeSigned_thenReturn503WithRetryAfter() throws Exception {
        // given
        when(offerService.getAllOffers(any(Pageable.class)))
                .thenThrow(new PhotoStorageUnavailableException(
                        "Photo address could not be signed. The object store is unavailable.",
                        new IllegalStateException("presign failed")));

        // when / then
        mvc.perform(get("/offers"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "10"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value(
                        "Service temporarily unavailable, please retry."));
    }

    @Test
    @DisplayName("deletePhoto_whenCallerIsOwner_thenReturn204")
    void deletePhoto_whenCallerIsOwner_thenReturn204() throws Exception {
        // given
        doNothing().when(offerService).deletePhoto(TEST_DEFAULT_OFFER_ID, TEST_USER_EMAIL);

        // when / then
        mvc.perform(
                        delete(String.format("/owner/offers/%s/photo", TEST_DEFAULT_OFFER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("deletePhoto_whenCallerIsNotOwner_thenReturn403")
    void deletePhoto_whenCallerIsNotOwner_thenReturn403() throws Exception {
        // given
        doThrow(new OfferAccessDeniedException("You can only delete photos of your own offers."))
                .when(offerService).deletePhoto(TEST_DEFAULT_OFFER_ID, TEST_USER_EMAIL);

        // when / then
        mvc.perform(
                        delete(String.format("/owner/offers/%s/photo", TEST_DEFAULT_OFFER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("deletePhoto_whenNoToken_thenReturn401AndTouchNoService")
    void deletePhoto_whenNoToken_thenReturn401() throws Exception {
        // when / then
        mvc.perform(
                        delete(String.format("/owner/offers/%s/photo", TEST_DEFAULT_OFFER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(offerService);
    }

    @Test
    @DisplayName("addOffer_whenBodyCarriesAStorageKeyUnderTheRetiredPhotoPathField_thenItIsIgnored")
    void addOffer_whenBodyCarriesTheRetiredPhotoPathField_thenItIsIgnored() throws Exception {
        // given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        when(offerService.addOffer(any(CreateOfferCommand.class)))
                .thenReturn(OfferAssembler.toOfferView(offerDTO));
        String requestJson = """
                {
                  "hotelName": "testHotelName",
                  "price": 20,
                  "roomCapacity": 5,
                  "city": "testCity",
                  "country": "testCountry",
                  "photoPath": "offers/00000000-0000-0000-0000-000000000002/victim.png"
                }
                """;

        // when / then
        mvc.perform(
                        post("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photoPath").doesNotExist());
    }

    @Test
    @DisplayName("addOffer_whenExternalPhotoUrlIsAStorageKey_thenReturn400")
    void addOffer_whenExternalPhotoUrlIsAStorageKey_thenReturn400() throws Exception {
        // given
        String requestJson = """
                {
                  "hotelName": "testHotelName",
                  "price": 20,
                  "roomCapacity": 5,
                  "city": "testCity",
                  "country": "testCountry",
                  "externalPhotoUrl": "offers/00000000-0000-0000-0000-000000000002/victim.png"
                }
                """;

        // when / then
        mvc.perform(
                        post("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(offerService);
    }

    @Test
    @DisplayName("editOffer_whenExternalPhotoUrlIsARelativePath_thenReturn400")
    void editOffer_whenExternalPhotoUrlIsARelativePath_thenReturn400() throws Exception {
        // given
        String requestJson = String.format("""
                {
                  "id": "%s",
                  "externalPhotoUrl": "/images/grand-hotel-paris-new.jpg"
                }
                """, TEST_DEFAULT_OFFER_ID);

        // when / then
        mvc.perform(
                        put("/owner/offers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(offerService);
    }

    @Test
    @DisplayName("deleteGalleryPhoto_whenCoverDeleted_thenReturn200WithFirstRemainingPromoted")
    void deleteGalleryPhoto_whenCoverDeleted_thenReturn200WithPromotedCover() throws Exception {
        // given
        java.util.UUID survivorId = java.util.UUID.randomUUID();
        OfferDTO response = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        com.lukk.sky.offer.adapters.dto.PhotoDTO survivor =
                com.lukk.sky.offer.adapters.dto.PhotoDTO.builder()
                        .id(survivorId).position(0).url("https://cdn.example/next.png").main(true).build();
        response.setGallery(java.util.List.of(survivor));
        response.setCoverPhotoUrl("https://cdn.example/next.png");
        java.util.UUID photoId = java.util.UUID.randomUUID();
        when(offerService.deleteGalleryPhoto(eq(TEST_DEFAULT_OFFER_ID), eq(photoId), eq(TEST_USER_EMAIL)))
                .thenReturn(OfferAssembler.toOfferView(response));

        // when / then
        mvc.perform(
                        delete(String.format("/owner/offers/%s/photos/%s", TEST_DEFAULT_OFFER_ID, photoId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gallery[0].id").value(survivorId.toString()))
                .andExpect(jsonPath("$.gallery[0].main").value(true))
                .andExpect(jsonPath("$.coverPhotoUrl").value("https://cdn.example/next.png"));
    }

    @Test
    @DisplayName("getOfferById_whenOfferExists_thenReturnItWithoutAuthentication")
    void getOfferById_whenOfferExists_thenReturnIt() throws Exception {
        // given
        OfferDTO offerDTO = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        when(offerService.getOfferById(eq(TEST_DEFAULT_OFFER_ID)))
                .thenReturn(OfferAssembler.toOfferView(offerDTO));

        // when / then
        mvc.perform(
                        get("/offers/" + TEST_DEFAULT_OFFER_ID)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_DEFAULT_OFFER_ID.toString()))
                .andExpect(jsonPath("$.hotelName").value(offerDTO.getHotelName()));
    }

    @Test
    @DisplayName("getOfferById_whenOfferDoesNotExist_thenReturn404")
    void getOfferById_whenOfferDoesNotExist_thenReturn404() throws Exception {
        // given
        doThrow(new OfferNotFoundException("Offer with ID: " + TEST_DEFAULT_OFFER_ID + " not exist."))
                .when(offerService).getOfferById(eq(TEST_DEFAULT_OFFER_ID));

        // when / then
        mvc.perform(
                        get("/offers/" + TEST_DEFAULT_OFFER_ID)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
