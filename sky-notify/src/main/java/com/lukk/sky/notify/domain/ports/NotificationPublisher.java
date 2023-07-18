package com.lukk.sky.notify.domain.ports;

/**
 * Service for publishing notifications.
 */
public interface NotificationPublisher {
    /**
     * Publish the provided data as a notification.
     *
     * @param data The data to be published. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code data} is {@code null}.
     */
    public void publish(String data);
}
