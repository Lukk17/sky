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

@DisplayName("OfferApiController: OfferApiController: offer edit and delete endpoints")
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"offerTopic-1"})
@Import({com.lukk.sky.offer.TestSecurityConfig.class, com.lukk.sky.offer.TestcontainersConfiguration.class, com.lukk.sky.offer.TestS3Config.class})
class OfferEditDeleteApiControllerTest {

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

    OfferEditDeleteApiControllerTest(@Value("${sky.apiPrefix}") String apiPrefix) {
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
        // when / then
        mvc.perform(
                        delete("/owner/offers/not-a-number")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isBadRequest());
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

}
