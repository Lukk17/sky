package com.lukk.sky.booking.adapters.outbound.rest;

import com.lukk.sky.booking.config.propertyBind.SkyConfigProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RequestUriStrategyPrimary URL building")
class RequestUriStrategyPrimaryTest {

    private static final String HOSTNAME = "http://sky-offer-service";
    private static final String OWNER_ENDPOINT = "api/v1/offers";
    private static final String ENDPOINT = "api/v1/offers/00000000-0000-0000-0000-000000000101/owner";

    @Test
    @DisplayName("createRestUrl leaves out the port segment when no port is configured, which is the deployed default")
    void createRestUrl_whenConfiguredPortIsBlank_thenUrlHasNoPortSegment() {
        RequestUriStrategyPrimary strategy = strategyWithPort("");

        String url = strategy.createRestUrl(ENDPOINT);

        assertThat(url).isEqualTo(HOSTNAME + "/" + ENDPOINT);
    }

    @Test
    @DisplayName("createRestUrl appends the configured port after a colon")
    void createRestUrl_whenPortIsConfigured_thenUrlCarriesHostColonPort() {
        RequestUriStrategyPrimary strategy = strategyWithPort("5552");

        String url = strategy.createRestUrl(ENDPOINT);

        assertThat(url).isEqualTo(HOSTNAME + ":5552/" + ENDPOINT);
    }

    private static RequestUriStrategyPrimary strategyWithPort(String port) {
        return new RequestUriStrategyPrimary(new SkyConfigProperties(HOSTNAME, port, OWNER_ENDPOINT));
    }
}
