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

@DisplayName("OfferApiController: OfferApiController: single-photo upload validation")
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"offerTopic-1"})
@Import({com.lukk.sky.offer.TestSecurityConfig.class, com.lukk.sky.offer.TestcontainersConfiguration.class, com.lukk.sky.offer.TestS3Config.class})
class OfferPhotoUploadApiControllerTest {

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

    OfferPhotoUploadApiControllerTest(@Value("${sky.apiPrefix}") String apiPrefix) {
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

}
