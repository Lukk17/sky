package com.lukk.sky.notify.config.kafka;

import com.lukk.sky.notify.AbstractIntegrationTest;
import com.lukk.sky.notify.TestSecurityConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Pins the five delivery-guarantee properties on the resolved configuration of the dead-letter
 * producer, which this module builds in Java rather than from YAML, and proves the Kafka client
 * accepts them together. The identical assertions live in sky-booking and sky-offer.
 */
@DisplayName("sky-notify dead-letter producer: delivery-guarantee configuration")
@Import(TestSecurityConfig.class)
class DltKafkaProducerDeliveryGuaranteeTest extends AbstractIntegrationTest {

    private static final String EXPECTED_ACKS = "all";
    private static final String EXPECTED_IDEMPOTENCE = "true";
    private static final String EXPECTED_RETRIES = String.valueOf(Integer.MAX_VALUE);
    private static final String EXPECTED_DELIVERY_TIMEOUT_MS = "120000";
    private static final String EXPECTED_MAX_IN_FLIGHT_REQUESTS = "5";

    @Autowired
    private ProducerFactory<String, String> dltProducerFactory;

    @Test
    @DisplayName("all five delivery-guarantee properties are set explicitly, none left to a client default")
    void getConfigurationProperties_whenTheContextHasBooted_thenAllFiveGuaranteePropertiesAreSetExplicitly() {
        // given
        Map<String, Object> resolved = dltProducerFactory.getConfigurationProperties();

        // then
        assertSoftly(softly -> {
            softly.assertThat(stringValueOf(resolved, ProducerConfig.ACKS_CONFIG))
                    .as(ProducerConfig.ACKS_CONFIG)
                    .isEqualTo(EXPECTED_ACKS);

            softly.assertThat(stringValueOf(resolved, ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG))
                    .as(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG)
                    .isEqualTo(EXPECTED_IDEMPOTENCE);

            softly.assertThat(stringValueOf(resolved, ProducerConfig.RETRIES_CONFIG))
                    .as(ProducerConfig.RETRIES_CONFIG)
                    .isEqualTo(EXPECTED_RETRIES);

            softly.assertThat(stringValueOf(resolved, ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG))
                    .as(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG)
                    .isEqualTo(EXPECTED_DELIVERY_TIMEOUT_MS);

            softly.assertThat(stringValueOf(resolved, ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION))
                    .as(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION)
                    .isEqualTo(EXPECTED_MAX_IN_FLIGHT_REQUESTS);
        });
    }

    @Test
    @DisplayName("the client accepts the values together, so no startup validation rejects the producer")
    void getConfigurationProperties_whenAProducerIsBuiltFromThem_thenTheClientValidationsPass() {
        // given
        Map<String, Object> resolved = dltProducerFactory.getConfigurationProperties();

        // then
        assertThatCode(() -> {
            try (Producer<String, String> probe = new KafkaProducer<>(resolved)) {
                assertThat(probe).isNotNull();
            }
        }).doesNotThrowAnyException();
    }

    private static String stringValueOf(Map<String, Object> resolved, String key) {
        Object value = resolved.get(key);

        return value == null ? null : String.valueOf(value);
    }
}
