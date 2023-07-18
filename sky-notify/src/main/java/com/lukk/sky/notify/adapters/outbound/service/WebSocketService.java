package com.lukk.sky.notify.adapters.outbound.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for WebSocket communication.
 * It uses {@link SimpMessagingTemplate} to send messages to WebSocket endpoints.
 */
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate template;

    /**
     * Sends the provided message to the specified WebSocket destination.
     *
     * @param destination The WebSocket endpoint to which the message should be sent.
     * @param message     The message to be sent.
     * @throws IllegalArgumentException if either {@code destination} or {@code message} is {@code null}.
     */
    public void triggerMessage(String destination, Object message) {
        template.convertAndSend(destination, message);
    }
}
