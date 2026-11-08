package com.lukk.sky.notify.config;

/**
 * Service-local constants for sky-notify. Kafka topic names live in
 * {@code com.lukk.sky.common.kafka.SkyTopics} and are used directly.
 */
public final class Constants {

    public static final String NOTIFY_DEST = "notify";
    public static final String CONSUMER_GROUP_ID = "skyGroup";

    private Constants() {
    }
}
