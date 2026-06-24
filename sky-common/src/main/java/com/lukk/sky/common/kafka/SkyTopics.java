package com.lukk.sky.common.kafka;

/**
 * Canonical Kafka topic names shared across sky-booking (producer),
 * sky-offer (producer), and sky-notify (consumer).
 *
 * <p>Each service that uses these constants must declare {@code spring-kafka}
 * as an {@code implementation} dependency in its own build; sky-common keeps
 * it {@code compileOnly} so non-Kafka consumers are not affected.
 */
public final class SkyTopics {

    public static final String BOOKING_TOPIC = "bookingTopic-1";
    public static final String OFFER_TOPIC = "offerTopic-1";

    private SkyTopics() {
    }
}
