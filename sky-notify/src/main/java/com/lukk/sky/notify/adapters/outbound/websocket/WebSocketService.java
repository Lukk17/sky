package com.lukk.sky.notify.adapters.outbound.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import static com.lukk.sky.notify.config.Constants.NOTIFY_DEST;

/**
 * Emits a payload to a specific user's notification queue. Spring's user-destination
 * machinery rewrites {@code /user/{name}/queue/<dest>} based on the authenticated
 * principal attached to the STOMP session.
 */
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate template;

    public void triggerMessage(String targetUser, String message) {
        template.convertAndSendToUser(targetUser, "/queue/" + NOTIFY_DEST, message);
    }
}
