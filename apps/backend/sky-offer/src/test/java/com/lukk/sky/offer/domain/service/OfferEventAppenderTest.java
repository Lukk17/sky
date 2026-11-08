package com.lukk.sky.offer.domain.service;

import com.lukk.sky.offer.domain.exception.EventSequenceConflictException;
import com.lukk.sky.offer.domain.model.Event;
import com.lukk.sky.offer.domain.ports.outbound.OfferEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static com.lukk.sky.offer.assemblers.EventAssembler.TEST_DATE;
import static com.lukk.sky.offer.assemblers.EventAssembler.TEST_EVENT_TYPE;
import static com.lukk.sky.offer.assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OfferEventAppender: assigns the next sequence number for one append attempt")
@ExtendWith(MockitoExtension.class)
class OfferEventAppenderTest {

    private static final String PAYLOAD = "{\"hotelName\":\"testHotelName\"}";

    @Mock
    OfferEventStore offerEventStore;

    private OfferEventAppender offerEventAppender;

    @BeforeEach
    void setUp() {
        offerEventAppender = new OfferEventAppender(offerEventStore);
    }

    @Test
    @DisplayName("appendNextEvent assigns sequence number 1 when no previous event exists for the offer")
    void appendNextEvent_whenNoExistingEventsForOffer_thenAppendWithSequenceNumber1() {
        Instant appendedAt = TEST_DATE;

        try (MockedStatic<Instant> instantMock = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            // given
            instantMock.when(Instant::now).thenReturn(appendedAt);
            when(offerEventStore.findLastSequenceNumber(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.empty());

            // when
            offerEventAppender.appendNextEvent(TEST_DEFAULT_OFFER_ID, TEST_EVENT_TYPE, PAYLOAD);

            // then
            Event appended = capturedEvent();
            assertEquals(TEST_DEFAULT_OFFER_ID, appended.getOfferId());
            assertEquals(1, appended.getSequenceNumber());
            assertEquals(TEST_EVENT_TYPE, appended.getEventType());
            assertEquals(PAYLOAD, appended.getPayload());
            assertEquals(appendedAt, appended.getTimestamp());
        }
    }

    @Test
    @DisplayName("appendNextEvent assigns last sequence number + 1 when previous events already exist")
    void appendNextEvent_whenPreviousEventsExist_thenAppendWithIncrementedSequenceNumber() {
        // given
        when(offerEventStore.findLastSequenceNumber(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(2));

        // when
        offerEventAppender.appendNextEvent(TEST_DEFAULT_OFFER_ID, TEST_EVENT_TYPE, PAYLOAD);

        // then
        assertEquals(3, capturedEvent().getSequenceNumber());
    }

    @Test
    @DisplayName("appendNextEvent re-reads the sequence number per call, so a retry sees the committed maximum")
    void appendNextEvent_whenCalledTwice_thenEachCallReadsTheSequenceNumberAgain() {
        // given
        when(offerEventStore.findLastSequenceNumber(TEST_DEFAULT_OFFER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(1));

        // when
        offerEventAppender.appendNextEvent(TEST_DEFAULT_OFFER_ID, TEST_EVENT_TYPE, PAYLOAD);
        offerEventAppender.appendNextEvent(TEST_DEFAULT_OFFER_ID, TEST_EVENT_TYPE, PAYLOAD);

        // then
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(offerEventStore, times(2)).append(captor.capture());
        assertEquals(1, captor.getAllValues().get(0).getSequenceNumber());
        assertEquals(2, captor.getAllValues().get(1).getSequenceNumber());
    }

    @Test
    @DisplayName("appendNextEvent propagates the conflict when the store rejects the sequence number")
    void appendNextEvent_whenStoreReportsAConflict_thenPropagateIt() {
        // given
        when(offerEventStore.findLastSequenceNumber(TEST_DEFAULT_OFFER_ID)).thenReturn(Optional.of(4));
        when(offerEventStore.append(any(Event.class)))
                .thenThrow(new EventSequenceConflictException("taken", new IllegalStateException("duplicate key")));

        // when / then
        assertThrows(EventSequenceConflictException.class,
                () -> offerEventAppender.appendNextEvent(TEST_DEFAULT_OFFER_ID, TEST_EVENT_TYPE, PAYLOAD));
    }

    private Event capturedEvent() {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(offerEventStore).append(captor.capture());

        return captor.getValue();
    }
}
