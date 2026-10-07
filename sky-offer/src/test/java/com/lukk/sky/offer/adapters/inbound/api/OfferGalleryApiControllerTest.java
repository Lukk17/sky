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

@DisplayName("OfferGalleryApiController: MockMvc tests for the gallery endpoints")
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"offerTopic-1"})
@Import({com.lukk.sky.offer.TestSecurityConfig.class, com.lukk.sky.offer.TestcontainersConfiguration.class, com.lukk.sky.offer.TestS3Config.class})
class OfferGalleryApiControllerTest {

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

    OfferGalleryApiControllerTest(@Value("${sky.apiPrefix}") String apiPrefix) {
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
    @DisplayName("uploadGalleryPhoto_whenValidPng_thenReturn200WithAppendedPhoto")
    void uploadGalleryPhoto_whenValidPng_thenReturn200() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.png", "image/png", VALID_PNG_BYTES
        );
        OfferDTO response = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        when(offerService.uploadGalleryPhoto(eq(TEST_DEFAULT_OFFER_ID), eq(TEST_OWNER_EMAIL), any(InputStream.class),
                eq((long) VALID_PNG_BYTES.length), eq("image/png"), eq("hotel.png")))
                .thenReturn(OfferAssembler.toOfferView(response));

        // when / then
        mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photos")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_DEFAULT_OFFER_ID.toString()));
    }
    @Test
    @DisplayName("uploadGalleryPhoto_whenValidWebP_thenReturn200")
    void uploadGalleryPhoto_whenValidWebP_thenReturn200() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.webp", "image/webp", VALID_WEBP_BYTES
        );
        OfferDTO response = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        when(offerService.uploadGalleryPhoto(eq(TEST_DEFAULT_OFFER_ID), eq(TEST_OWNER_EMAIL), any(InputStream.class),
                eq((long) VALID_WEBP_BYTES.length), eq("image/webp"), eq("hotel.webp")))
                .thenReturn(OfferAssembler.toOfferView(response));

        // when / then
        mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photos")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_DEFAULT_OFFER_ID.toString()));
    }

    @Test
    @DisplayName("uploadGalleryPhoto_whenMagicBytesAreUnknown_thenReturn400")
    void uploadGalleryPhoto_whenMagicBytesAreUnknown_thenReturn400() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "hotel.png", "image/png", INVALID_MAGIC_BYTES
        );

        // when / then
        mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photos")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(offerService);
    }

    @Test
    @DisplayName("uploadGalleryPhoto_whenRiffContainerIsNotWebP_thenReturn400")
    void uploadGalleryPhoto_whenRiffContainerIsNotWebP_thenReturn400() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "sound.wav", "audio/wav", RIFF_WAVE_BYTES
        );

        // when / then
        mvc.perform(
                        MockMvcRequestBuilders.multipart("/" + API_PREFIX + "/owner/offers/" + TEST_DEFAULT_OFFER_ID + "/photos")
                                .file(file)
                                .with(jwt().jwt(j -> j.claim("email", TEST_OWNER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(offerService);
    }

    @Test
    @DisplayName("reorderGalleryPhoto_whenValidPosition_thenReturn200")
    void reorderGalleryPhoto_whenValidPosition_thenReturn200() throws Exception {
        // given
        java.util.UUID photoId = java.util.UUID.randomUUID();
        OfferDTO response = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        when(offerService.reorderGalleryPhoto(eq(TEST_DEFAULT_OFFER_ID), eq(photoId), eq(0), eq(TEST_USER_EMAIL)))
                .thenReturn(OfferAssembler.toOfferView(response));

        // when / then
        mvc.perform(
                        put(String.format("/owner/offers/%s/photos/%s/position?position=0", TEST_DEFAULT_OFFER_ID, photoId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_DEFAULT_OFFER_ID.toString()));
    }

    @Test
    @DisplayName("setGalleryCover_whenPhotoExists_thenReturn200")
    void setGalleryCover_whenPhotoExists_thenReturn200() throws Exception {
        // given
        java.util.UUID photoId = java.util.UUID.randomUUID();
        OfferDTO response = OfferAssembler.getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID);
        when(offerService.setGalleryCover(eq(TEST_DEFAULT_OFFER_ID), eq(photoId), eq(TEST_USER_EMAIL)))
                .thenReturn(OfferAssembler.toOfferView(response));

        // when / then
        mvc.perform(
                        put(String.format("/owner/offers/%s/photos/%s/cover", TEST_DEFAULT_OFFER_ID, photoId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", TEST_USER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_DEFAULT_OFFER_ID.toString()));
    }

}
