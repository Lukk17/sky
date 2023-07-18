package com.lukk.sky.notify.adapters.outbound;

import com.lukk.sky.notify.adapters.outbound.service.WebSocketService;
import com.lukk.sky.notify.domain.ports.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import static com.lukk.sky.notify.config.Constants.NOTIFY_DEST;

/**
 * The primary implementation of the {@link NotificationPublisher} interface.
 * This implementation uses a {@link WebSocketService} to publish notifications.
 */
@Component
@RequiredArgsConstructor
@Primary
public class NotificationPublisherPrimary implements NotificationPublisher {

    private final WebSocketService webSocketService;

    /**
     * {@inheritDoc}
     * <p>
     * This implementation triggers a message on a WebSocket endpoint with the given payload.
     */
    @Override
    public void publish(String payload) {
        webSocketService.triggerMessage("/" + NOTIFY_DEST, payload);
    }
}
