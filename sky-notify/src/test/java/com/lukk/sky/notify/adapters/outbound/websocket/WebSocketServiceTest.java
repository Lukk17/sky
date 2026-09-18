package com.lukk.sky.notify.adapters.outbound.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("WebSocketService: per-user queue addressing")
class WebSocketServiceTest {

    private static final String TARGET_USER = "user@test.com";
    private static final String MESSAGE = "{\"payload\":\"offer-data\"}";
    private static final String USER_QUEUE = "/queue/notify";

    private final SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);

    private final WebSocketService webSocketService = new WebSocketService(template);

    @Test
    @DisplayName("sends the message to the target user's own queue rather than to a broadcast topic")
    void triggerMessage_whenCalled_thenSendsToTheUserQueueOfThatUser() {
        // when
        webSocketService.triggerMessage(TARGET_USER, MESSAGE);

        // then
        verify(template).convertAndSendToUser(TARGET_USER, USER_QUEUE, MESSAGE);
    }
}
