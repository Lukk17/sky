package com.lukk.sky.offer.domain.ports.service;

import com.google.gson.Gson;
import com.lukk.sky.offer.Assemblers.OfferAssembler;
import com.lukk.sky.offer.domain.model.Event;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.repository.EventSourceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.lukk.sky.offer.Assemblers.EventAssembler.*;
import static com.lukk.sky.offer.Assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EventSourceServicePrimary — unit tests for event sourcing persistence logic")
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class EventSourceServicePrimaryTest {

    @Mock
    EventSourceRepository eventSourceRepository;

    @InjectMocks
    EventSourceServicePrimary eventSourceServicePrimary;

    Gson gson = new Gson();

    @Test
    @DisplayName("saveEvent assigns sequence number 1 when no previous event exists for the offer")
    void saveEvent_whenNoExistingEventsForOffer_thenSaveWithSequenceNumber1() {
        try (MockedStatic<Instant> instantMock = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            instantMock.when(Instant::now).thenReturn(TEST_DATE);

            Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
            Event expected = getTestEvent(1, gson.toJson(offer));

            when(eventSourceRepository.findLastSequenceNumberByOfferId(TEST_DEFAULT_OFFER_ID))
                    .thenReturn(Optional.empty());

            eventSourceServicePrimary.saveEvent(offer, TEST_EVENT_TYPE);

            ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            verify(eventSourceRepository, times(1)).save(captor.capture());

            Event saved = captor.getValue();
            assertEquals(expected.getOfferId(), saved.getOfferId());
            assertEquals(expected.getSequenceNumber(), saved.getSequenceNumber());
            assertEquals(expected.getEventType(), saved.getEventType());
            assertEquals(expected.getPayload(), saved.getPayload());
            assertEquals(expected.getTimestamp(), saved.getTimestamp());
        }
    }

    @Test
    @DisplayName("saveEvent assigns last sequence number + 1 when previous events already exist for the offer")
    void saveEvent_whenPreviousEventsExist_thenSaveWithIncrementedSequenceNumber() {
        try (MockedStatic<Instant> instantMock = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            instantMock.when(Instant::now).thenReturn(TEST_DATE);

            Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
            Event expected = getTestEvent(3, gson.toJson(offer));

            when(eventSourceRepository.findLastSequenceNumberByOfferId(TEST_DEFAULT_OFFER_ID))
                    .thenReturn(Optional.of(2));

            eventSourceServicePrimary.saveEvent(offer, TEST_EVENT_TYPE);

            ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            verify(eventSourceRepository, times(1)).save(captor.capture());

            Event saved = captor.getValue();
            assertEquals(expected.getOfferId(), saved.getOfferId());
            assertEquals(expected.getSequenceNumber(), saved.getSequenceNumber());
            assertEquals(expected.getEventType(), saved.getEventType());
            assertEquals(expected.getPayload(), saved.getPayload());
            assertEquals(expected.getTimestamp(), saved.getTimestamp());
        }
    }

    @Test
    @DisplayName("saveEvent assigns sequential sequence numbers across two consecutive events for the same offer")
    void saveEvent_whenTwoEventsForSameOffer_thenAssignSequentialNumbers() {
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);

        when(eventSourceRepository.findLastSequenceNumberByOfferId(TEST_DEFAULT_OFFER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(1));

        eventSourceServicePrimary.saveEvent(offer, TEST_EVENT_TYPE);
        eventSourceServicePrimary.saveEvent(offer, TEST_EVENT_TYPE);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventSourceRepository, times(2)).save(captor.capture());

        List<Event> saved = captor.getAllValues();
        assertEquals(1, saved.get(0).getSequenceNumber());
        assertEquals(2, saved.get(1).getSequenceNumber());
    }

    @Test
    @DisplayName("saveEvent acquires the per-offer advisory lock before reading the sequence and saving")
    void saveEvent_whenSaving_thenLockAcquiredBeforeSequenceReadAndSave() {
        Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);

        when(eventSourceRepository.findLastSequenceNumberByOfferId(TEST_DEFAULT_OFFER_ID))
                .thenReturn(Optional.of(4));

        eventSourceServicePrimary.saveEvent(offer, TEST_EVENT_TYPE);

        InOrder inOrder = inOrder(eventSourceRepository);
        inOrder.verify(eventSourceRepository).lockOfferEventStream(TEST_DEFAULT_OFFER_ID);
        inOrder.verify(eventSourceRepository).findLastSequenceNumberByOfferId(TEST_DEFAULT_OFFER_ID);
        inOrder.verify(eventSourceRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("saveEvent throws IllegalArgumentException and persists nothing when the offer id is null")
    void saveEvent_whenOfferIdIsNull_thenThrowIllegalArgumentException() {
        Offer offer = Offer.builder()
                .hotelName("Hotel")
                .ownerEmail("owner@test.com")
                .city("Warsaw")
                .country("Poland")
                .price(BigDecimal.ONE)
                .roomCapacity(1L)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> eventSourceServicePrimary.saveEvent(offer, TEST_EVENT_TYPE));

        verify(eventSourceRepository, never()).save(any(Event.class));
    }
}
