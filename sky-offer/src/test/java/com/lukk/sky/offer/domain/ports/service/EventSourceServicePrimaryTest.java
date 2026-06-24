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
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.lukk.sky.offer.Assemblers.EventAssembler.*;
import static com.lukk.sky.offer.Assemblers.OfferAssembler.TEST_DEFAULT_OFFER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

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
    public void saveEvent_whenNoExistingEventsForOffer_thenSaveWithSequenceNumber1() {
        // testDate need to be initialized now, before mocking LocalDateTime class
        LocalDateTime testDate = TEST_DATE;

        try (MockedStatic<LocalDateTime> localDateTimeMock = Mockito.mockStatic(LocalDateTime.class)) {
            localDateTimeMock.when(LocalDateTime::now).thenReturn(testDate);

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
    public void saveEvent_whenPreviousEventsExist_thenSaveWithIncrementedSequenceNumber() {
        // GIVEN
        LocalDateTime testDate = TEST_DATE;

        try (MockedStatic<LocalDateTime> localDateTimeMock = Mockito.mockStatic(LocalDateTime.class)) {
            localDateTimeMock.when(LocalDateTime::now).thenReturn(testDate);

            Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
            Event expected = getTestEvent(3, gson.toJson(offer));

            when(eventSourceRepository.findLastSequenceNumberByOfferId(TEST_DEFAULT_OFFER_ID))
                    .thenReturn(Optional.of(2));

            // WHEN
            eventSourceServicePrimary.saveEvent(offer, TEST_EVENT_TYPE);

            // THEN
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
}
