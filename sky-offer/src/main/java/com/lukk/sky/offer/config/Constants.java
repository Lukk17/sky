package com.lukk.sky.offer.config;

import com.lukk.sky.common.kafka.SkyTopics;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Service-local constants for sky-offer. Cross-service Kafka topic names
 * live in {@link SkyTopics}; cross-cutting web constants live in
 * {@code com.lukk.sky.common.web}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

    public static final String KAFKA_TOPIC = SkyTopics.OFFER_TOPIC;
}
