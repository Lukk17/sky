package com.lukk.sky.offer.domain.ports.service;

import com.google.gson.Gson;
import com.lukk.sky.offer.Assemblers.OfferAssembler;
import com.lukk.sky.offer.domain.model.Event;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.repository.EventSourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class EventSourceServicePrimaryTest {

    @Mock
    EventSourceRepository eventSourceRepository;

    @InjectMocks
    EventSourceServicePrimary eventSourceServicePrimary;

    Gson gson = new Gson();

    @Test
    public void saveFirstEvent() {
        // testDate need to be initialized now, before mocking LocalDateTime class
        LocalDateTime testDate = TEST_DATE;

        try (MockedStatic<LocalDateTime> localDateTimeMock = Mockito.mockStatic(LocalDateTime.class)) {
            localDateTimeMock.when(LocalDateTime::now).thenReturn(testDate);

            Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
            Event expected = getTestEvent(1, gson.toJson(offer));

            when(eventSourceRepository.findLastSequenceNumberByOfferId(TEST_DEFAULT_OFFER_ID.toString()))
                    .thenReturn(Optional.empty());

            eventSourceServicePrimary.saveEvent(offer, TEST_EVENT_TYPE);

            verify(eventSourceRepository, times(1)).save(expected);
        }
    }

    @Test
    public void saveNextEvent() {
        // GIVEN
        LocalDateTime testDate = TEST_DATE;

        try (MockedStatic<LocalDateTime> localDateTimeMock = Mockito.mockStatic(LocalDateTime.class)) {
            localDateTimeMock.when(LocalDateTime::now).thenReturn(testDate);

            Offer offer = OfferAssembler.getPopulatedOffer(TEST_DEFAULT_OFFER_ID);
            Event expected = getTestEvent(3, gson.toJson(offer));

            when(eventSourceRepository.findLastSequenceNumberByOfferId(TEST_DEFAULT_OFFER_ID.toString()))
                    .thenReturn(Optional.of(2));

            // WHEN
            eventSourceServicePrimary.saveEvent(offer, TEST_EVENT_TYPE);

            // THEN
            verify(eventSourceRepository, times(1)).save(expected);
        }
    }

}
