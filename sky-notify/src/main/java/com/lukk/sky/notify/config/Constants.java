package com.lukk.sky.notify.config;

import com.lukk.sky.common.kafka.SkyTopics;

/**
 * Service-local constants for sky-notify. Topic names delegate to
 * {@link SkyTopics} so the string values are defined exactly once.
 */
public final class Constants {

    public static final String KAFKA_OFFER_TOPIC = SkyTopics.OFFER_TOPIC;
    public static final String KAFKA_BOOKING_TOPIC = SkyTopics.BOOKING_TOPIC;
    public static final String NOTIFY_DEST = "notify";
    public static final String CONSUMER_GROUP_ID = "skyGroup";

    private Constants() {
    }
}
