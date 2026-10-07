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

@DisplayName("OfferApiController: OfferApiController: single-photo upload auth and store failures")
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"offerTopic-1"})
@Import({com.lukk.sky.offer.TestSecurityConfig.class, com.lukk.sky.offer.TestcontainersConfiguration.class, com.lukk.sky.offer.TestS3Config.class})
class OfferPhotoUploadFailureApiControllerTest {

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

    OfferPhotoUploadFailureApiControllerTest(@Value("${sky.apiPrefix}") String apiPrefix) {
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

}
