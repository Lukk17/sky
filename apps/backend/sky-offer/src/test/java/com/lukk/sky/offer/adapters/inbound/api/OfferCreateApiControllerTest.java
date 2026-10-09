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

@DisplayName("OfferApiController: OfferApiController: offer creation endpoints")
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"offerTopic-1"})
@Import({com.lukk.sky.offer.TestSecurityConfig.class, com.lukk.sky.offer.TestcontainersConfiguration.class, com.lukk.sky.offer.TestS3Config.class})
class OfferCreateApiControllerTest {

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

    OfferCreateApiControllerTest(@Value("${sky.apiPrefix}") String apiPrefix) {
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

}
