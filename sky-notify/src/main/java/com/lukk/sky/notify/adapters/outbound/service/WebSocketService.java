package com.lukk.sky.notify.adapters.outbound.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import static com.lukk.sky.notify.config.Constants.NOTIFY_DEST;

/**
 * Service class responsible for WebSocket communication.
 * It uses {@link SimpMessagingTemplate} to send messages to WebSocket endpoints.
 */
@Controller
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate template;

    /**
     * Sends the provided message to the specified WebSocket destination.
     *
     * @param message The message to be sent.
     * @throws IllegalArgumentException if either {@code destination} or {@code message} is {@code null}.
     */
    public void triggerMessage(@Payload String message) {
        template.convertAndSend("/" + NOTIFY_DEST, message);
    }
}
