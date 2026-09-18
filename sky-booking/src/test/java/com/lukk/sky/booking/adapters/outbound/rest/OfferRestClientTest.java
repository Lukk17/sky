package com.lukk.sky.booking.adapters.outbound.rest;

import com.lukk.sky.booking.config.propertyBind.SkyConfigProperties;
import com.lukk.sky.booking.domain.exception.OfferServiceUnavailableException;
import com.lukk.sky.booking.domain.ports.outbound.RequestUriStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("OfferRestClient outbound failure translation")
@ExtendWith(MockitoExtension.class)
class OfferRestClientTest {

    private static final UUID OFFER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final String OFFER_URL = "http://sky-offer-service/api/v1/offers/" + OFFER_ID + "/owner";

    @Mock
    private OfferServiceCaller offerServiceCaller;

    @Mock
    private RequestUriStrategy requestUriStrategy;

    private OfferRestClient offerRestClient;

    @BeforeEach
    void setUp() {
        offerRestClient = new OfferRestClient(
                offerServiceCaller,
                new SkyConfigProperties("http://sky-offer-service", "", "api/v1/offers"),
                requestUriStrategy);
    }

    @Test
    @DisplayName("requestOfferOwner reports an unavailable offer service, not a bad request, once the retries are exhausted")
    void requestOfferOwner_whenOfferServiceKeepsAnswering5xx_thenThrowsOfferServiceUnavailable() {
        assertUnavailable(new ResourceAccessException("Offer service 5xx for offerId=" + OFFER_ID));
    }

    @Test
    @DisplayName("requestOfferOwner reports an unavailable offer service when the connection is refused")
    void requestOfferOwner_whenConnectionIsRefused_thenThrowsOfferServiceUnavailable() {
        assertUnavailable(new ResourceAccessException("I/O error", new ConnectException("Connection refused")));
    }

    @Test
    @DisplayName("requestOfferOwner reports an unavailable offer service when the connection attempt times out")
    void requestOfferOwner_whenConnectTimesOut_thenThrowsOfferServiceUnavailable() {
        assertUnavailable(new ResourceAccessException("I/O error", new SocketTimeoutException("connect timed out")));
    }

    @Test
    @DisplayName("requestOfferOwner reports an unavailable offer service when the read times out")
    void requestOfferOwner_whenReadTimesOut_thenThrowsOfferServiceUnavailable() {
        assertUnavailable(new ResourceAccessException("I/O error", new SocketTimeoutException("Read timed out")));
    }

    private void assertUnavailable(ResourceAccessException transportFailure) {
        when(requestUriStrategy.createRestUrl(anyString())).thenReturn(OFFER_URL);
        when(offerServiceCaller.callOfferService(OFFER_URL, OFFER_ID)).thenThrow(transportFailure);

        assertThatThrownBy(() -> offerRestClient.requestOfferOwner(OFFER_ID))
                .isInstanceOf(OfferServiceUnavailableException.class)
                .isNotInstanceOf(ResourceAccessException.class)
                .hasMessage("Offer service unavailable for offerId=" + OFFER_ID)
                .hasCause(transportFailure);
    }
}
