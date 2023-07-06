package com.lukk.sky.notify.adapters.outbound.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate template;

    public void triggerMessage(String destination, Object message) {
        template.convertAndSend(destination, message);
    }
}
