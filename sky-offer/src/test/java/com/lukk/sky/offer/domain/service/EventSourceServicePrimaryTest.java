package com.lukk.sky.offer.domain.service;

import com.lukk.sky.offer.assemblers.OfferAssembler;
import com.lukk.sky.offer.domain.exception.EventSequenceConflictException;
import com.lukk.sky.offer.domain.model.EventType;
import com.lukk.sky.offer.domain.model.Offer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static com.lukk.sky.offer.assemblers.EventAssembler.TEST_EVENT_TYPE;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static com.lukk.sky.offer.domain.service.EventSourceServicePrimary.MAX_APPEND_ATTEMPTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EventSourceServicePrimary: unit tests for event sourcing persistence logic")
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class EventSourceServicePrimaryTest {

    @Mock
    OfferEventAppender offerEventAppender;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private EventSourceServicePrimary eventSourceServicePrimary;

    @BeforeEach
    void setUp() {
        eventSourceServicePrimary = new EventSourceServicePrimary(offerEventAppender, objectMapper);
    }

    @Test
    @DisplayName("saveEvent appends one event carrying the offer id and the event type")
    void saveEvent_whenCalled_thenAppendOneEventForThatOfferAndEventType() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);

        // when
        eventSourceServicePrimary.saveEvent(offer, TEST_EVENT_TYPE);

        // then
        verify(offerEventAppender, times(1))
                .appendNextEvent(eq(TEST_DEFAULT_OFFER_ID), eq(TEST_EVENT_TYPE), anyString());
    }

    @Test
    @DisplayName("saveEvent serialises the offer with Jackson, keeping null fields and unescaped punctuation")
    void saveEvent_whenOfferHasNullFieldAndPunctuation_thenPayloadKeepsNullsAndRawPunctuation() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        offer.setDescription("Bed & breakfast <near> the old town");
        offer.setPhotoObjectKey(null);

        // when
        eventSourceServicePrimary.saveEvent(offer, TEST_EVENT_TYPE);

        // then
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(offerEventAppender).appendNextEvent(eq(TEST_DEFAULT_OFFER_ID), eq(TEST_EVENT_TYPE), payload.capture());
        assertEquals("{\"id\":\"00000000-0000-0000-0000-000000000001\",\"hotelName\":\"testHotelName\","
                        + "\"description\":\"Bed & breakfast <near> the old town\","
                        + "\"comment\":\"testComment\",\"price\":20,\"ownerEmail\":\"test@owner.com\","
                        + "\"roomCapacity\":5,\"city\":\"testCity\",\"country\":\"testCountry\","
                        + "\"photoObjectKey\":null,"
                        + "\"externalPhotoUrl\":\"https://images.example.com/test-hotel.jpeg\"}",
                payload.getValue());
    }

    @Test
    @DisplayName("saveEvent retries the append when another writer took the sequence number first")
    void saveEvent_whenFirstAttemptsConflict_thenRetryUntilTheAppendSucceeds() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        when(offerEventAppender.appendNextEvent(eq(TEST_DEFAULT_OFFER_ID), eq(TEST_EVENT_TYPE), anyString()))
                .thenThrow(conflict())
                .thenThrow(conflict())
                .thenReturn(null);

        // when
        eventSourceServicePrimary.saveEvent(offer, TEST_EVENT_TYPE);

        // then
        verify(offerEventAppender, times(3))
                .appendNextEvent(eq(TEST_DEFAULT_OFFER_ID), eq(TEST_EVENT_TYPE), anyString());
    }

    @Test
    @DisplayName("saveEvent gives up after the bounded number of attempts, with the last conflict as the cause")
    void saveEvent_whenEveryAttemptConflicts_thenThrowAfterMaxAttemptsWithTheLastConflictAsCause() {
        // given
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
        EventSequenceConflictException everyAttemptConflicts = conflict();
        when(offerEventAppender.appendNextEvent(eq(TEST_DEFAULT_OFFER_ID), eq(TEST_EVENT_TYPE), anyString()))
                .thenThrow(everyAttemptConflicts);

        // when
        EventSequenceConflictException thrown = assertThrows(EventSequenceConflictException.class,
                () -> eventSourceServicePrimary.saveEvent(offer, TEST_EVENT_TYPE));

        // then
        verify(offerEventAppender, times(MAX_APPEND_ATTEMPTS))
                .appendNextEvent(eq(TEST_DEFAULT_OFFER_ID), eq(TEST_EVENT_TYPE), anyString());
        assertSame(everyAttemptConflicts, thrown.getCause());
        assertThat(thrown).hasMessageContaining(String.valueOf(MAX_APPEND_ATTEMPTS));
    }

    @Test
    @DisplayName("saveEvent throws IllegalArgumentException and persists nothing when the offer id is null")
    void saveEvent_whenOfferIdIsNull_thenThrowIllegalArgumentException() {
        // given
        Offer offer = Offer.builder()
                .hotelName("Hotel")
                .ownerEmail("owner@test.com")
                .city("Warsaw")
                .country("Poland")
                .price(BigDecimal.ONE)
                .roomCapacity(1L)
                .build();

        // when / then
        assertThrows(IllegalArgumentException.class,
                () -> eventSourceServicePrimary.saveEvent(offer, TEST_EVENT_TYPE));
        verify(offerEventAppender, never()).appendNextEvent(any(UUID.class), any(EventType.class), anyString());
    }

    private static EventSequenceConflictException conflict() {
        return new EventSequenceConflictException("sequence number already taken",
                new IllegalStateException("duplicate key"));
    }
}
