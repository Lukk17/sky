package com.lukk.sky.notify.config;

import com.lukk.sky.notify.AbstractIntegrationTest;
import com.lukk.sky.notify.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static com.lukk.sky.common.kafka.SkyTopics.BOOKING_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Gateway session STOMP flow: full-stack integration tests")
@Import(TestSecurityConfig.class)
class GatewaySessionStompIntegrationTest extends AbstractIntegrationTest {

    private static final String GATEWAY_USER = "e2e-user@test.com";
    private static final String PROBE_PAYLOAD = "gateway-e2e-probe-1";
    private static final int TIMEOUT_SECONDS = 30;

    @LocalServerPort
    private int port;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("CONNECT without bearer is accepted on the gateway session and a Kafka event reaches the user queue")
    void gatewaySession_whenConnectWithoutBearer_thenConnectedAndEventReceived() throws Exception {
        // given
        // when
        // then
        StompClient client = connect();
        clientHolder = client;
        try {
            client.send("CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n\0");
            assertThat(client.awaitFrameStartingWith("CONNECTED", TIMEOUT_SECONDS))
                    .contains("user-name:" + GATEWAY_USER);

            client.send("SUBSCRIBE\nid:sub-0\ndestination:/user/queue/notify\n\n\0");

            String event = objectMapper.writeValueAsString(Map.of(
                    "payload", PROBE_PAYLOAD, "accessedAt", "2026-09-29T00:00:00Z", "userInfo", GATEWAY_USER));
            assertThat(publishUntilReceived(event)).contains(PROBE_PAYLOAD);
        } finally {
            client.close();
        }
    }

    private String publishUntilReceived(String event) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (true) {
            kafkaTemplate.send(BOOKING_TOPIC, event).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                throw new AssertionError("no STOMP MESSAGE frame within " + TIMEOUT_SECONDS + "s");
            }
            try {
                return client().awaitFrameContaining(PROBE_PAYLOAD, TimeUnit.NANOSECONDS.toSeconds(remaining) + 1);
            } catch (AssertionError retry) {
                if (System.nanoTime() >= deadline) {
                    throw retry;
                }
            }
        }
    }

    private StompClient clientHolder;

    private StompClient client() {
        return clientHolder;
    }

    private StompClient connect() throws Exception {
        StompClient client = new StompClient();
        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .header(GatewayUserHandshakeInterceptor.GATEWAY_USER_HEADER, GATEWAY_USER)
                .buildAsync(URI.create("ws://localhost:" + port + "/notifyWebsocket"), client)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return client;
    }

    private static final class StompClient implements WebSocket.Listener {

        private final StringBuilder buffer = new StringBuilder();
        private final CopyOnWriteArrayList<String> frames = new CopyOnWriteArrayList<>();
        private volatile WebSocket socket;

        void send(String frame) {
            socket.sendText(frame, true);
        }

        void close() {
            if (socket != null) {
                socket.abort();
            }
        }

        String awaitFrameStartingWith(String prefix, long timeoutSeconds) throws Exception {
            return awaitFrame(prefix, null, timeoutSeconds);
        }

        String awaitFrameContaining(String infix, long timeoutSeconds) throws Exception {
            return awaitFrame(null, infix, timeoutSeconds);
        }

        private String awaitFrame(String prefix, String infix, long timeoutSeconds) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
            while (System.nanoTime() < deadline) {
                for (String frame : frames) {
                    if (prefix != null && frame.startsWith(prefix)) {
                        return frame;
                    }
                    if (infix != null && frame.contains(infix)) {
                        return frame;
                    }
                }
                TimeUnit.MILLISECONDS.sleep(100);
            }
            throw new AssertionError("no STOMP frame "
                    + (prefix != null ? "starting with " + prefix : "containing " + infix)
                    + " within " + timeoutSeconds + "s, got: " + frames);
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            this.socket = webSocket;
            webSocket.request(Long.MAX_VALUE);
        }

        @Override
        public CompletableFuture<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                frames.add(buffer.toString());
                buffer.setLength(0);
            }
            webSocket.request(Long.MAX_VALUE);
            return CompletableFuture.completedFuture(null);
        }
    }
}
