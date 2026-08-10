package com.lukk.sky.notify.adapters.outbound;

import com.lukk.sky.notify.adapters.outbound.service.WebSocketService;
import com.lukk.sky.notify.domain.ports.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Primary
public class NotificationPublisherPrimary implements NotificationPublisher {

    private final WebSocketService webSocketService;

    @Override
    public void publish(String targetUser, String payload) {
        webSocketService.triggerMessage(targetUser, payload);
    }
}
