package com.lukk.sky.offer.config;

import com.lukk.sky.common.kafka.SkyTopics;

/**
 * Service-local constants for sky-offer. Cross-service Kafka topic names
 * live in {@link SkyTopics}; cross-cutting web constants live in
 * {@code com.lukk.sky.common.web}.
 */
public final class Constants {

    public static final String KAFKA_TOPIC = SkyTopics.OFFER_TOPIC;

    private Constants() {
    }
}
