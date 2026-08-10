package com.lukk.sky.notify.domain.ports;

/**
 * Port for emitting a serialized notification to a specific user's WebSocket destination.
 */
public interface NotificationPublisher {

    /**
     * Publish the provided JSON payload to the named user's notification queue.
     *
     * @param targetUser principal name (matches the JWT {@code sub} or configured claim)
     *                   the broker uses to route to {@code /user/{targetUser}/queue/notify}.
     *                   Must not be {@code null}.
     * @param data       JSON payload. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code targetUser} or {@code data} is {@code null}.
     */
    void publish(String targetUser, String data);
}
